# PART 75 · Frame·code object introspection — 실행 중인 Python을 구조적으로 관찰하기

Python function은 단순한 source text가 아니다. Compile된 code object와 runtime frame이 분리되어 있고, traceback·debugger·profiler는 이 구조를 이용한다. Introspection은 강력하지만 frame이 local object graph를 붙잡거나 implementation detail에 의존하게 만들 수 있다. 이 PART에서는 **code는 실행 설계도, frame은 특정 호출의 실행 상태**라는 모델로 접근한다.

---

## CHAPTER 01 · code object는 실행 가능한 구조와 이름·상수·위치 정보를 담는다

Function의 `__code__`는 parameter 수, local name, constant, instruction-related metadata 같은 compile 결과를 가진다. 같은 code object가 여러 function object나 여러 호출에서 사용될 수 있다.

```python
def add(a, b):
    return a + b

code = add.__code__
print(code.co_name)
print(code.co_varnames)
```

Code object는 한 번의 호출 상태가 아니다. `a=1, b=2` 같은 실제 argument value는 frame에 존재하고 code에는 변수 slot과 instruction 구조가 있다.

이 차이를 이해하면 profiler가 function definition과 call instance를 어떻게 구분하는지, closure가 code와 environment를 별도로 갖는 이유를 설명할 수 있다.

---

## CHAPTER 02 · frame object는 특정 호출의 instruction position과 namespace를 가진다

Function이 실행될 때 frame은 local, global reference, 현재 code object, 실행 위치 같은 상태를 가진다. Recursive call은 같은 code를 사용해도 서로 다른 frame을 만든다.

Debugger가 stack을 보여줄 때 각 줄은 이런 frame chain에 해당한다. 현재 frame에서 caller frame으로 연결해 call path를 따라갈 수 있지만 application code가 이 구조에 직접 의존하면 implementation coupling이 커질 수 있다.

Frame은 강한 reference를 통해 local object를 유지할 수 있으므로 장기 cache에 저장하지 않는다. 관찰 후 필요한 요약만 남기는 편이 안전하다.

---

## CHAPTER 03 · locals·globals·builtins는 이름 해석의 서로 다른 namespace 층이다

Frame을 보면 현재 local namespace와 global namespace를 구분할 수 있다. Builtins도 별도 lookup source로 연결된다. 하지만 `locals()` 반환 mapping을 수정하면 항상 실제 fast local slot이 즉시 바뀐다고 생각하면 안 된다.

이름 해석을 디버깅할 때는 같은 spelling의 이름이 어느 namespace에서 왔는지 확인한다. Dynamic execution이나 debugger가 local mapping을 보여준다고 해서 source-level assignment와 동일한 mutation semantics를 가진다고 단정하지 않는다.

Global dict를 직접 변경하는 metaprogramming은 가능하지만 hidden coupling이 커진다. Introspection은 관찰과 진단에 우선 사용하고 runtime state mutation은 명시적 API로 한다.

---

## CHAPTER 04 · instruction position은 source line 하나보다 더 세밀한 실행 위치를 가질 수 있다

한 source line에 여러 expression이 있거나 multiline expression이 있으면 “몇 번째 줄에서 실패했는가”만으로 충분하지 않을 수 있다. Modern Python metadata는 instruction과 source position을 더 세밀하게 연결할 수 있다.

이 정보는 traceback rendering, debugger highlight, coverage에 중요하다. 하지만 source file이 배포 후 달라졌다면 code object와 현재 file 내용이 어긋날 수 있다.

따라서 production artifact와 source revision을 함께 식별할 수 있어야 한다. Stack trace line number만 보고 다른 commit의 source를 열면 잘못된 원인을 찾게 된다.

---

## CHAPTER 05 · bytecode view는 source와 interpreter execution 사이의 중간 관찰 창이다

`dis` 같은 도구로 bytecode를 보면 name lookup, call, branch가 compile 결과에서 어떻게 나타나는지 비교할 수 있다. 이는 performance와 language semantics를 이해하는 데 유용하지만 bytecode 형식은 Python version에 따라 변할 수 있다.

```python
import dis

def choose(x):
    return x + 1 if x > 0 else 0

dis.dis(choose)
```

Bytecode를 business logic의 stable API로 취급하지 않는다. 특정 opcode 이름과 sequence에 강하게 결합한 tool은 version migration에서 깨질 수 있다.

학습과 진단에서는 source → AST/code → bytecode → frame이라는 층을 연결하는 목적으로 사용한다.

---

## CHAPTER 06 · frame retention은 traceback·debugger가 예상보다 많은 memory를 붙잡게 할 수 있다

Exception을 장기 보관하면 traceback이 frame을 가리키고 frame이 local variable을 통해 대형 object graph를 붙잡을 수 있다. 특히 batch system이 실패 exception 전체를 in-memory history에 쌓으면 leak처럼 보이는 retention이 생길 수 있다.

필요한 경우 traceback summary나 string을 추출하고 원본 exception/frame reference를 버린다. Debugger session이 끝난 뒤 frame reference를 계속 global에 보관하지 않는다.

Memory investigation에서 “object가 왜 살아 있나”를 볼 때 traceback list, exception cache, coroutine frame도 root 후보로 본다.

---

## CHAPTER 07 · introspection은 관찰 비용과 최적화 방해 가능성을 가진다

모든 요청에서 frame을 걷고 local 전체를 serialize하면 observability 자체가 성능 문제를 만든다. Sensitive input도 함께 수집될 수 있다.

Instrumentation은 sampling, field allowlist, depth limit를 사용한다. Error path에서만 상세 정보를 수집하거나 trace ID와 핵심 state만 남기는 방식으로 비용을 제한한다.

Introspection API는 debugging에는 강력하지만 core business path의 정상 동작이 frame layout이나 function name 문자열에 의존하지 않게 한다. Refactoring 내성을 유지해야 한다.

---

## CHAPTER 08 · frame/code contract는 진단 데이터와 프로그램 의미를 구분한다

Code object와 frame을 이해하면 traceback, profiler, debugger를 더 정확히 해석할 수 있다. 그러나 이 정보는 대부분 실행을 관찰하기 위한 metadata이지 domain model 자체가 아니다.

테스트에서는 introspection tool이 Python minor version 변화에 얼마나 민감한지, frame을 저장했을 때 reference가 남지 않는지, redaction 정책이 local capture에도 적용되는지 확인한다.

이 PART의 핵심은 **함수 실행을 source line 하나로 보지 않고, 재사용되는 code object와 호출마다 만들어지는 frame의 결합으로 이해하면서 introspection의 lifetime·비용·version coupling까지 관리하는 것**이다.
