# PART 73 · Finalization과 resurrection — `__del__`·cycle·weakref cleanup의 한계를 이해하기

객체가 더 이상 필요 없을 때 “파괴된다”는 표현은 Python에서 조심해서 써야 한다. Reference가 사라지는 시점, garbage collection이 cycle을 발견하는 시점, finalizer가 호출되는 시점은 서로 다를 수 있고 interpreter shutdown에서는 module state 자체가 사라질 수 있다. 특히 `__del__`에 file close나 lock release 같은 필수 cleanup을 맡기면 실행 시점의 불확실성이 correctness 문제로 이어질 수 있다.

---

## CHAPTER 01 · object lifetime은 scope 종료와 동일하지 않다

함수 local 이름이 scope를 벗어났다고 해서 object가 반드시 즉시 사라지는 것은 아니다. 다른 container, closure, callback registry, cycle이 같은 object를 참조하고 있을 수 있다.

```python
def build(cache):
    item = LargeObject()
    cache["latest"] = item
```

함수가 끝나도 `cache`가 reference를 유지하므로 object lifetime은 계속된다. Memory retention 문제를 찾을 때는 “어느 함수에서 만들었는가”보다 “현재 누가 참조하고 있는가”를 본다.

CPython의 reference counting은 많은 객체를 빠르게 회수할 수 있지만 language-level API를 deterministic destruction timing에 의존하도록 설계하면 다른 implementation이나 cycle에서 가정이 깨질 수 있다.

---

## CHAPTER 02 · `__del__`은 편리해 보여도 필수 resource cleanup의 주 경로가 되면 위험하다

`__del__`은 object finalization 시점에 호출될 수 있는 hook이지만 언제 실행될지, 실행 환경이 어떤 상태일지 완전히 단순하지 않다.

```python
class Temp:
    def __del__(self):
        print("finalizing")
```

File descriptor, transaction, lock처럼 release 시점이 중요하면 context manager나 explicit `close()`를 사용한다. `__del__`은 마지막 안전망 이상의 책임을 갖지 않는 편이 좋다.

Finalizer 내부 exception은 일반 control flow와 다르게 처리될 수 있고, 프로그램이 종료되는 동안 logging infrastructure 자체가 이미 약화되어 있을 수도 있다. Error reporting을 finalizer에 의존하지 않는다.

---

## CHAPTER 03 · reference cycle은 단순 reference count만으로 회수되지 않는 구조를 만든다

두 객체가 서로를 참조하면 외부 reference가 사라져도 내부 reference count가 0이 되지 않을 수 있다.

```python
class Node:
    pass

a = Node()
b = Node()
a.peer = b
b.peer = a
```

Python의 cyclic garbage collector가 이런 cycle을 탐지할 수 있지만, cycle을 만들었다는 사실 자체가 lifetime 추적을 복잡하게 한다. Large graph, callback relation, parent/child 양방향 pointer에서 흔하다.

Cycle이 반드시 leak은 아니지만 객체가 예상보다 오래 살아 memory peak가 커질 수 있다. Ownership이 한 방향이면 weak reference나 ID relation으로 reverse link를 표현할 수 있는지 검토한다.

---

## CHAPTER 04 · resurrection은 finalizer가 object를 다시 reachable하게 만드는 특수한 경로다

Finalizer에서 `self`를 global container 등에 저장하면 사라지려던 object가 다시 reachable해질 수 있다. 이를 resurrection이라고 생각할 수 있다.

```python
saved = []

class Strange:
    def __del__(self):
        saved.append(self)
```

이런 코드는 object lifetime을 극도로 예측하기 어렵게 만들고, finalizer가 한 번 실행된 object의 후속 상태를 복잡하게 한다. 정상 application architecture에서는 피하는 편이 좋다.

Resurrection을 이해하는 이유는 이런 패턴을 사용하기 위해서가 아니라 finalization이 단순 “마지막 메서드 호출 후 메모리 해제” 모델보다 복잡하다는 것을 알기 위해서다.

---

## CHAPTER 05 · `weakref.finalize`는 cleanup callback을 object method와 분리할 수 있다

Weak reference 기반 finalizer는 object가 사라질 때 별도 callback을 실행하도록 구성할 수 있다. `__del__`보다 cleanup logic을 object 자체와 분리하기 쉬운 장점이 있다.

```python
import weakref

class Handle:
    def __init__(self, raw):
        self.raw = raw
        self._finalizer = weakref.finalize(self, close_raw, raw)
```

하지만 이것도 필수 deterministic cleanup을 대신하지 않는다. 프로그램이 정상 scope를 벗어날 때 즉시 release해야 하는 resource라면 context manager가 우선이다.

Finalizer callback이 cleanup 대상 object를 다시 강하게 capture하면 weak relation의 목적을 깨뜨릴 수 있다. Callback closure가 무엇을 참조하는지 확인한다.

---

## CHAPTER 06 · scarce resource는 finalizer가 아니라 명시적 lifetime protocol로 관리한다

Memory는 collector가 다룰 수 있지만 socket, file, DB connection, lock은 외부 system의 제한된 resource다. 회수 시점을 늦추면 descriptor exhaustion이나 connection pool starvation이 생길 수 있다.

```python
with open(path, "rb") as f:
    data = f.read()
```

이 구조는 scope 종료 시 cleanup path가 명확하다. Object finalization timing과 무관하다.

Explicit `close()`를 제공한다면 여러 번 호출해도 안전한지, close 후 method 호출은 어떤 예외를 내는지 contract를 정한다. Resource ownership을 caller와 object 중 누가 갖는지도 문서화한다.

---

## CHAPTER 07 · interpreter shutdown에서는 module global과 logging dependency가 먼저 약해질 수 있다

프로세스 종료 단계에서는 object finalizer가 실행될 때 module-level name이나 imported service가 평소와 같은 상태라는 보장이 약하다. Finalizer가 global logger, network client, registry에 의존하면 shutdown에서 예상하지 못한 오류가 날 수 있다.

따라서 shutdown-critical cleanup은 application lifecycle manager가 명시적으로 수행하는 편이 낫다. Service container가 reverse order로 component를 닫고 마지막에 process가 종료되도록 만든다.

`atexit` 같은 mechanism도 ordering과 failure semantics를 이해하고 사용한다. “프로그램 종료 시 언젠가 불릴 것”과 “이 resource가 항상 안전하게 정리된다”는 같은 문장이 아니다.

---

## CHAPTER 08 · finalization contract는 optional safety net과 required cleanup을 구분한다

Object lifecycle 설계에서 가장 중요한 구분은 correctness에 필수인 cleanup과 best-effort cleanup이다. 전자는 lexical scope, explicit lifecycle, transaction manager로 보장하고 후자는 finalizer가 보조할 수 있다.

테스트에서는 cycle, callback retention, explicit close, double close, shutdown-like dependency loss를 구분한다. Memory issue를 조사할 때는 `__del__` 호출 여부보다 reference path와 ownership graph를 먼저 본다.

이 PART의 핵심은 **객체 소멸을 scope 종료와 동일시하지 않고, reference graph·cycle collection·finalizer·resurrection·external resource lifetime을 서로 다른 메커니즘으로 분리하는 것**이다.
