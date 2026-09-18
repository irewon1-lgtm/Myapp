# PART 73 · Finalization과 resurrection — `__del__`·cycle·weakref cleanup의 한계를 이해하기

객체가 더 이상 필요 없을 때 “파괴된다”는 표현은 Python에서 조심해서 써야 한다. Reference가 사라지는 시점, garbage collection이 cycle을 발견하는 시점, finalizer가 호출되는 시점은 서로 다를 수 있고 interpreter shutdown에서는 module state 자체가 사라질 수 있다. 특히 `__del__`에 file close나 lock release 같은 필수 cleanup을 맡기면 실행 시점의 불확실성이 correctness 문제로 이어질 수 있다.

---

## CHAPTER 01 · object lifetime은 scope 종료와 동일하지 않다

### 시작 전 용어집

#### 1. object lifetime

- **뜻:** 함수가 끝나도 `cache`가 reference를 유지하므로 object lifetime은 계속된다.
- **왜 중요한가:** Memory retention 문제를 찾을 때는 “어느 함수에서 만들었는가”보다 “현재 누가 참조하고 있는가”를 본다.
- **예시:** def build(cache): / item = LargeObject()

#### 2. scope

- **뜻:** 함수 local 이름이 scope를 벗어났다고 해서 object가 반드시 즉시 사라지는 것은 아니다.
- **왜 중요한가:** 다른 container, closure, callback registry, cycle이 같은 object를 참조하고 있을 수 있다.
- **예시:** def build(cache): / item = LargeObject()

#### 3. 함수

- **뜻:** CPython의 reference counting은 많은 객체를 빠르게 회수할 수 있지만 language-level API를 deterministic destruction timing에 의존하도록 설계하면 다른 implementation이나 cycle에서 가정이 깨질 수 있다.
- **예시:** def build(cache): / item = LargeObject()

```python
def build(cache):
    item = LargeObject()
    cache["latest"] = item
```

 


---

## CHAPTER 02 · `__del__`은 편리해 보여도 필수 resource cleanup의 주 경로가 되면 위험하다

### 시작 전 용어집

#### 1. __del__

- **뜻:** `__del__`은 object finalization 시점에 호출될 수 있는 hook이지만 언제 실행될지, 실행 환경이 어떤 상태일지 완전히 단순하지 않다.
- **왜 중요한가:** File descriptor, transaction, lock처럼 release 시점이 중요하면 context manager나 explicit `close()`를 사용한다.
- **예시:** class Temp: / def __del__(self):

#### 2. resource

- **뜻:** `__del__`은 마지막 안전망 이상의 책임을 갖지 않는 편이 좋다.
- **왜 중요한가:** Finalizer 내부 exception은 일반 control flow와 다르게 처리될 수 있고, 프로그램이 종료되는 동안 logging infrastructure 자체가 이미 약화되어 있을 수도 있다.
- **예시:** class Temp: / def __del__(self):

#### 3. finalization

- **뜻:** Error reporting을 finalizer에 의존하지 않는다.
- **예시:** class Temp: / def __del__(self):

```python
class Temp:
    def __del__(self):
        print("finalizing")
```

 

 

---

**검증 시나리오 P73-C2 — CHAPTER 02 · `__del__`은 편리해 보여도 필수 resource cleanup의 주 경로가 되면 위험하다**
`CHAPTER 02 · `__del__`은 편리해 보여도 필수 resource cleanup의 주 경로가 되면 위험하다` 검증은 정상 경로를 먼저 재현하는 데서 시작한다. P73-C2에서는 `CHAPTER 02 · `__del__`은 편리해 보여도 필수 resource cleanup의 주 경로가 되면 위험하다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 02 · `__del__`은 편리해 보여도 필수 resource cleanup의 주 경로가 되면 위험하다`에 대해 오류 경로 하나를 의도적으로 만든다고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 02 · `__del__`은 편리해 보여도 필수 resource cleanup의 주 경로가 되면 위험하다`의 예외 종류와 직전 상태를 함께 남긴다하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 02 · `__del__`은 편리해 보여도 필수 resource cleanup의 주 경로가 되면 위험하다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P73-C2의 마무리는 복구 후 같은 오류가 다시 재현되지 않는지 검사하는 것이다. 통과 기준은 `CHAPTER 02 · `__del__`은 편리해 보여도 필수 resource cleanup의 주 경로가 되면 위험하다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 03 · reference cycle은 단순 reference count만으로 회수되지 않는 구조를 만든다

### 시작 전 용어집

#### 1. reference cycle

- **뜻:** 두 객체가 서로를 참조하면 외부 reference가 사라져도 내부 reference count가 0이 되지 않을 수 있다.
- **왜 중요한가:** Python의 cyclic garbage collector가 이런 cycle을 탐지할 수 있지만, cycle을 만들었다는 사실 자체가 lifetime 추적을 복잡하게 한다.
- **예시:** class Node: / pass

#### 2. reference count

- **뜻:** Large graph, callback relation, parent/child 양방향 pointer에서 흔하다.
- **왜 중요한가:** Cycle이 반드시 leak은 아니지만 객체가 예상보다 오래 살아 memory peak가 커질 수 있다.
- **예시:** class Node: / pass

#### 3. 객체

- **뜻:** Ownership이 한 방향이면 weak reference나 ID relation으로 reverse link를 표현할 수 있는지 검토한다.
- **예시:** class Node: / pass

```python
class Node:
    pass

a = Node()
b = Node()
a.peer = b
b.peer = a
```

 

 

---

**검증 시나리오 P73-C3 — CHAPTER 03 · reference cycle은 단순 reference count만으로 회수되지 않는 구조를 만든다**
`CHAPTER 03 · reference cycle은 단순 reference count만으로 회수되지 않는 구조를 만든다` 검증은 호출 순서를 단순화하는 데서 시작한다. P73-C3에서는 `CHAPTER 03 · reference cycle은 단순 reference count만으로 회수되지 않는 구조를 만든다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 03 · reference cycle은 단순 reference count만으로 회수되지 않는 구조를 만든다`에 대해 순서 하나만 뒤집어 차이를 본다고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 03 · reference cycle은 단순 reference count만으로 회수되지 않는 구조를 만든다`의 호출 전후의 상태 전이를 번호로 남긴다하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 03 · reference cycle은 단순 reference count만으로 회수되지 않는 구조를 만든다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P73-C3의 마무리는 다른 순서에서도 계약이 유지되는지 확인하는 것이다. 통과 기준은 `CHAPTER 03 · reference cycle은 단순 reference count만으로 회수되지 않는 구조를 만든다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 04 · resurrection은 finalizer가 object를 다시 reachable하게 만드는 특수한 경로다

### 시작 전 용어집

#### 1. resurrection

- **뜻:** 이를 resurrection이라고 생각할 수 있다.
- **왜 중요한가:** 이런 코드는 object lifetime을 극도로 예측하기 어렵게 만들고, finalizer가 한 번 실행된 object의 후속 상태를 복잡하게 한다.
- **예시:** saved = [] / class Strange:

#### 2. finalizer

- **뜻:** Finalizer에서 `self`를 global container 등에 저장하면 사라지려던 object가 다시 reachable해질 수 있다.
- **왜 중요한가:** 정상 application architecture에서는 피하는 편이 좋다.
- **예시:** saved = [] / class Strange:

#### 3. object

- **뜻:** Resurrection을 이해하는 이유는 이런 패턴을 사용하기 위해서가 아니라 finalization이 단순 “마지막 메서드 호출 후 메모리 해제” 모델보다 복잡하다는 것을 알기 위해서다.
- **예시:** saved = [] / class Strange:

```python
saved = []

class Strange:
    def __del__(self):
        saved.append(self)
```

 


---

**검증 시나리오 P73-C4 — CHAPTER 04 · resurrection은 finalizer가 object를 다시 reachable하게 만드는 특수한 경로다**
`CHAPTER 04 · resurrection은 finalizer가 object를 다시 reachable하게 만드는 특수한 경로다` 검증은 입력 크기를 고정하는 데서 시작한다. P73-C4에서는 `CHAPTER 04 · resurrection은 finalizer가 object를 다시 reachable하게 만드는 특수한 경로다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 04 · resurrection은 finalizer가 object를 다시 reachable하게 만드는 특수한 경로다`에 대해 변형은 한 요소만 허용고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 04 · resurrection은 finalizer가 object를 다시 reachable하게 만드는 특수한 경로다`의 측정값을 여러 번 모아 분포를 비교하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 04 · resurrection은 finalizer가 object를 다시 reachable하게 만드는 특수한 경로다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P73-C4의 마무리는 워밍업과 측정 자체의 비용을 분리하는 것이다. 통과 기준은 `CHAPTER 04 · resurrection은 finalizer가 object를 다시 reachable하게 만드는 특수한 경로다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 05 · `weakref.finalize`는 cleanup callback을 object method와 분리할 수 있다

### 시작 전 용어집

#### 1. weakref

- **뜻:** Weak reference 기반 finalizer는 object가 사라질 때 별도 callback을 실행하도록 구성할 수 있다.
- **왜 중요한가:** `__del__`보다 cleanup logic을 object 자체와 분리하기 쉬운 장점이 있다.
- **예시:** import weakref / class Handle:

#### 2. finalize

- **뜻:** Finalizer callback이 cleanup 대상 object를 다시 강하게 capture하면 weak relation의 목적을 깨뜨릴 수 있다.
- **왜 중요한가:** Callback closure가 무엇을 참조하는지 확인한다.
- **예시:** import weakref / class Handle:

#### 3. cleanup callback

- **뜻:** 하지만 이것도 필수 deterministic cleanup을 대신하지 않는다.
- **왜 중요한가:** 프로그램이 정상 scope를 벗어날 때 즉시 release해야 하는 resource라면 context manager가 우선이다.
- **예시:** import weakref / class Handle:

```python
import weakref

class Handle:
    def __init__(self, raw):
        self.raw = raw
        self._finalizer = weakref.finalize(self, close_raw, raw)
```

 

 

---

**검증 시나리오 P73-C5 — CHAPTER 05 · `weakref.finalize`는 cleanup callback을 object method와 분리할 수 있다**
`CHAPTER 05 · `weakref.finalize`는 cleanup callback을 object method와 분리할 수 있다` 검증은 성공 사례를 기준선으로 저장하는 데서 시작한다. P73-C5에서는 `CHAPTER 05 · `weakref.finalize`는 cleanup callback을 object method와 분리할 수 있다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 05 · `weakref.finalize`는 cleanup callback을 object method와 분리할 수 있다`에 대해 실패 조건은 하나만 주입고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 05 · `weakref.finalize`는 cleanup callback을 object method와 분리할 수 있다`의 로그 시각과 상태 식별자를 맞춰 본다하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 05 · `weakref.finalize`는 cleanup callback을 object method와 분리할 수 있다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P73-C5의 마무리는 성공·실패 모두 결정적으로 끝나는지 확인하는 것이다. 통과 기준은 `CHAPTER 05 · `weakref.finalize`는 cleanup callback을 object method와 분리할 수 있다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 06 · scarce resource는 finalizer가 아니라 명시적 lifetime protocol로 관리한다

### 시작 전 용어집

#### 1. scarce resource

- **뜻:** Memory는 collector가 다룰 수 있지만 socket, file, DB connection, lock은 외부 system의 제한된 resource다.
- **왜 중요한가:** 회수 시점을 늦추면 descriptor exhaustion이나 connection pool starvation이 생길 수 있다.
- **예시:** with open(path, "rb") as f: / data = …

#### 2. finalizer

- **뜻:** 이 구조는 scope 종료 시 cleanup path가 명확하다.
- **왜 중요한가:** Object finalization timing과 무관하다.
- **예시:** with open(path, "rb") as f: / data = …

#### 3. lifetime protocol

- **뜻:** Explicit `close()`를 제공한다면 여러 번 호출해도 안전한지, close 후 method 호출은 어떤 예외를 내는지 contract를 정한다.
- **왜 중요한가:** Resource ownership을 caller와 object 중 누가 갖는지도 문서화한다.
- **예시:** with open(path, "rb") as f: / data = …

```python
with open(path, "rb") as f:
    data = f.read()
```

 

 

---

## CHAPTER 07 · interpreter shutdown에서는 module global과 logging dependency가 먼저 약해질 수 있다

### 시작 전 용어집

#### 1. interpreter shutdown

- **뜻:** 프로세스 종료 단계에서는 object finalizer가 실행될 때 module-level name이나 imported service가 평소와 같은 상태라는 보장이 약하다.
- **왜 중요한가:** Finalizer가 global logger, network client, registry에 의존하면 shutdown에서 예상하지 못한 오류가 날 수 있다.
- **예시:** 프로세스 종료 단계에서는 object finalizer가 실행될 때 module-level …

#### 2. module

- **뜻:** 따라서 shutdown-critical cleanup은 application lifecycle manager가 명시적으로 수행하는 편이 낫다.
- **왜 중요한가:** Service container가 reverse order로 component를 닫고 마지막에 process가 종료되도록 만든다.
- **예시:** 따라서 shutdown-critical cleanup은 application lifecycle manager가 명시적으로 수행하는 …

#### 3. logging

- **뜻:** `atexit` 같은 mechanism도 ordering과 failure semantics를 이해하고 사용한다.
- **왜 중요한가:** “프로그램 종료 시 언젠가 불릴 것”과 “이 resource가 항상 안전하게 정리된다”는 같은 문장이 아니다.
- **예시:** `atexit` 같은 mechanism도 ordering과 failure semantics를 이해하고 사용한다.

---

## CHAPTER 08 · finalization contract는 optional safety net과 required cleanup을 구분한다

### 시작 전 용어집

#### 1. finalization

- **뜻:** Object lifecycle 설계에서 가장 중요한 구분은 correctness에 필수인 cleanup과 best-effort cleanup이다.
- **왜 중요한가:** 전자는 lexical scope, explicit lifecycle, transaction manager로 보장하고 후자는 finalizer가 보조할 수 있다.
- **예시:** Object lifecycle 설계에서 가장 중요한 구분은 correctness에 필수인 …

#### 2. optional safety

- **뜻:** 테스트에서는 cycle, callback retention, explicit close, double close, shutdown-like dependency loss를 구분한다.
- **왜 중요한가:** Memory issue를 조사할 때는 `__del__` 호출 여부보다 reference path와 ownership graph를 먼저 본다.
- **예시:** 테스트에서는 cycle, callback retention, explicit close, double close, …

#### 3. net

- **뜻:** 이 PART의 핵심은 **객체 소멸을 scope 종료와 동일시하지 않고, reference graph·cycle collection·finalizer·resurrection·external resource lifetime을 서로 다른 메커니즘으로 분리하는 것**이다.
- **예시:** 이 PART의 핵심은 **객체 소멸을 scope 종료와 동일시하지 …

---

## 실전 학습 루프 · finalization과 resurrection

### 1. 쉬운 예

객체가 더 이상 필요 없어졌을 때 정리 코드를 실행하고 싶어도 정확한 시점과 순서를 믿기 어렵다. `__del__`에서 객체를 다시 전역에 저장하면 죽으려던 객체가 다시 reachable해지는 resurrection까지 가능해 수명 추적이 복잡해진다.

### 2. 한 줄 해석

중요한 자원 정리는 GC finalization에 맡기지 말고 명시적 lifetime/context manager로 관리하는 것이 기본이다.

### 3. 직접 실행

아래 코드는 개념을 작게 격리한 예다. 실행 전에 출력이나 상태 변화를 먼저 예상한 뒤 실제 결과와 비교한다.

```python
saved = []
class Strange:
    def __del__(self):
        saved.append(self)  # resurrection 예시: 실제 코드에서는 피한다

x = Strange()
del x
```

결과가 예상과 다르면 문법부터 고치지 말고, **어떤 protocol·상태·계약이 호출됐는지**를 한 단계씩 확인한다. 이렇게 해야 “우연히 동작하는 코드”와 “이유를 설명할 수 있는 코드”를 구분할 수 있다.

### 4. 수정 실습

1. 파일 close 같은 필수 cleanup을 `__del__` 대신 `with`로 옮긴다.
2. cycle이 있는 객체에서 finalization 순서를 가정하면 왜 위험한지 사례를 만든다.

수정 후에는 정상 입력 하나만 보지 말고 빈 값, 경계값, 반복 호출, 예외 경로 중 해당되는 반례를 최소 하나 추가한다.

### 5. 확인 문제

`del x` 직후 `__del__`이 반드시 즉시 실행된다고 가정해도 될까?

### 6. 정답과 오답 설명

**정답:** 안 된다. 이름 삭제와 객체 finalization은 같은 사건이 아니며 runtime/참조 상태에 따라 시점이 달라질 수 있다.

**자주 나오는 오답:** 변수 이름을 지우는 것과 객체가 메모리에서 즉시 사라지는 것을 동일시하면 안 된다.

마지막으로 코드를 다시 읽으면서 **입력 → 호출되는 규칙 → 상태 변화 → 결과/예외** 네 칸으로 요약한다. 이 네 칸을 설명할 수 있으면 단순 암기가 아니라 실행 모델을 이해한 것이다.

## 현장 디버깅 체크 · finalization·resurrection

### 증상에서 시작한다

객체를 지웠는데 file descriptor나 callback이 계속 남거나, 종료 시점에만 이상한 부작용이 발생한다. 이때 문법을 먼저 고치면 원인이 가려질 수 있다. 재현 입력과 실제 상태를 보존한 뒤 **어느 경계에서 처음 기대와 달라졌는지**를 찾는다.

### 먼저 볼 증거

weakref/referrer와 실제 resource open/close 시점을 같이 기록한다. 최종 출력 하나만 보지 말고 호출 전 값, 호출 뒤 값, 예외 또는 resource 상태를 나란히 두면 원인 후보가 급격히 줄어든다.

### 일부러 실패시켜 보기

`__del__` 안에서 전역 목록에 self를 넣어 resurrection을 만들고, 두 번째 수명에서 cleanup이 어떻게 달라지는지 본다. 정상 예제만 통과시키는 것은 검증이 아니다. 경계 조건을 강제로 만들고 같은 증상이 반복되는지 확인해야 수정 전후를 비교할 수 있다.

### 통과 기준

필수 자원 해제는 finalizer 호출 시점과 무관하게 context manager 또는 명시적 close 경로에서 결정적으로 끝나야 한다. 이 기준을 테스트 이름과 assertion으로 옮기면 이후 refactoring에서도 같은 오류가 돌아오는지 자동으로 잡을 수 있다.

