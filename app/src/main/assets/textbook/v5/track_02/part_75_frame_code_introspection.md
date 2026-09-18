# PART 75 · Frame·code object introspection — 실행 중인 Python을 구조적으로 관찰하기

Python function은 단순한 source text가 아니다. Compile된 code object와 runtime frame이 분리되어 있고, traceback·debugger·profiler는 이 구조를 이용한다. Introspection은 강력하지만 frame이 local object graph를 붙잡거나 implementation detail에 의존하게 만들 수 있다. 이 절에서는 **code는 실행 설계도, frame은 특정 호출의 실행 상태**라는 모델로 접근한다.

---

## CHAPTER 01 · code object는 실행 가능한 구조와 이름·상수·위치 정보를 담는다

### 시작 전 용어집

#### 1. code object

- **뜻:** 같은 code object가 여러 function object나 여러 호출에서 사용될 수 있다.
- **왜 중요한가:** Code object는 한 번의 호출 상태가 아니다.
- **예시:** def add(a, b): / return a + b

#### 2. parameter

- **뜻:** Function의 `__code__`는 parameter 수, local name, constant, instruction-related metadata 같은 compile 결과를 가진다.
- **왜 중요한가:** `a=1, b=2` 같은 실제 argument value는 frame에 존재하고 code에는 변수 slot과 instruction 구조가 있다.
- **예시:** def add(a, b): / return a + b

#### 3. compile

- **뜻:** 이 차이를 이해하면 profiler가 function definition과 call instance를 어떻게 구분하는지, closure가 code와 environment를 별도로 갖는 이유를 설명할 수 있다.
- **예시:** def add(a, b): / return a + b

```python
def add(a, b):
    return a + b

code = add.__code__
print(code.co_name)
print(code.co_varnames)
```

 


---

**직접 확인하기 — CHAPTER 01 · code object는 실행 가능한 구조와 이름·상수·위치 정보를 담는다**
CHAPTER 01 · code object는 실행 가능한 구조와 이름·상수·위치 정보를 담는다은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 규칙을 확인해야 오래 남는다. 먼저 입력이나 객체 하나만 두고 기대 결과를 적은 뒤 실행한다. 다음에는 값 하나, 호출 순서 하나, 경계 조건 하나만 바꿔 실제 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 01 · code object는 실행 가능한 구조와 이름·상수·위치 정보를 담는다의 규칙이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 좁히고, 수정 뒤 원래 예제와 반대 조건 예제를 모두 다시 실행한다. 마지막에는 왜 그런 결과가 나왔는지 자기 문장으로 설명해 본다.
## CHAPTER 02 · frame object는 특정 호출의 instruction position과 namespace를 가진다

### 시작 전 용어집

#### 1. frame

- **뜻:** Function이 실행될 때 frame은 local, global reference, 현재 code object, 실행 위치 같은 상태를 가진다.
- **왜 중요한가:** Recursive call은 같은 code를 사용해도 서로 다른 frame을 만든다.
- **예시:** Function이 실행될 때 frame은 local, global reference, 현재 …

#### 2. instruction position

- **뜻:** Debugger가 stack을 보여줄 때 각 줄은 이런 frame chain에 해당한다.
- **왜 중요한가:** 현재 frame에서 caller frame으로 연결해 call path를 따라갈 수 있지만 application code가 이 구조에 직접 의존하면 implementation coupling이 커질 수 있다.
- **예시:** Debugger가 stack을 보여줄 때 각 줄은 이런 frame …

#### 3. namespace

- **뜻:** Frame은 강한 reference를 통해 local object를 유지할 수 있으므로 장기 cache에 저장하지 않는다.
- **예시:** Frame은 강한 reference를 통해 local object를 유지할 수 …

관찰 후 필요한 요약만 남기는 편이 안전하다.

---

## CHAPTER 03 · locals·globals·builtins는 이름 해석의 서로 다른 namespace 층이다

### 시작 전 용어집

#### 1. locals

- **뜻:** 하지만 `locals()` 반환 mapping을 수정하면 항상 실제 fast local slot이 즉시 바뀐다고 생각하면 안 된다.
- **왜 중요한가:** 이름 해석을 디버깅할 때는 같은 spelling의 이름이 어느 namespace에서 왔는지 확인한다.
- **예시:** 하지만 `locals()` 반환 mapping을 수정하면 항상 실제 fast …

#### 2. globals

- **뜻:** Frame을 보면 현재 local namespace와 global namespace를 구분할 수 있다.
- **왜 중요한가:** Builtins도 별도 lookup source로 연결된다.
- **예시:** Frame을 보면 현재 local namespace와 global namespace를 구분할 …

#### 3. builtins

- **뜻:** Dynamic execution이나 debugger가 local mapping을 보여준다고 해서 source-level assignment와 동일한 mutation semantics를 가진다고 단정하지 않는다.
- **왜 중요한가:** Global dict를 직접 변경하는 metaprogramming은 가능하지만 hidden coupling이 커진다.
- **예시:** Dynamic execution이나 debugger가 local mapping을 보여준다고 해서 source-level …

#### 4. namespace

- **뜻:** Introspection은 관찰과 진단에 우선 사용하고 runtime state mutation은 명시적 API로 한다.
- **예시:** Introspection은 관찰과 진단에 우선 사용하고 runtime state mutation은 …

---

## CHAPTER 04 · instruction position은 source line 하나보다 더 세밀한 실행 위치를 가질 수 있다

### 시작 전 용어집

#### 1. instruction position

- **뜻:** 한 source line에 여러 expression이 있거나 multiline expression이 있으면 “몇 번째 줄에서 실패했는가”만으로 충분하지 않을 수 있다.
- **왜 중요한가:** Modern Python metadata는 instruction과 source position을 더 세밀하게 연결할 수 있다.
- **예시:** 한 source line에 여러 expression이 있거나 multiline expression이 …

#### 2. source line

- **뜻:** 이 정보는 traceback rendering, debugger highlight, coverage에 중요하다.
- **왜 중요한가:** 하지만 source file이 배포 후 달라졌다면 code object와 현재 file 내용이 어긋날 수 있다.
- **예시:** 이 정보는 traceback rendering, debugger highlight, coverage에 중요하다.

#### 3. 실패

- **뜻:** 따라서 production artifact와 source revision을 함께 식별할 수 있어야 한다.
- **왜 중요한가:** Stack trace line number만 보고 다른 commit의 source를 열면 잘못된 원인을 찾게 된다.
- **예시:** 따라서 production artifact와 source revision을 함께 식별할 수 …

---

## CHAPTER 05 · bytecode view는 source와 interpreter execution 사이의 중간 관찰 창이다

### 시작 전 용어집

#### 1. bytecode view

- **뜻:** `dis` 같은 도구로 bytecode를 보면 name lookup, call, branch가 compile 결과에서 어떻게 나타나는지 비교할 수 있다.
- **왜 중요한가:** 이는 performance와 language semantics를 이해하는 데 유용하지만 bytecode 형식은 Python version에 따라 변할 수 있다.
- **예시:** import dis / def choose(x):

#### 2. source

- **뜻:** 학습과 진단에서는 source → AST/code → bytecode → frame이라는 층을 연결하는 목적으로 사용한다.
- **왜 중요한가:** Bytecode를 business logic의 stable API로 취급하지 않는다.
- **예시:** import dis / def choose(x):

#### 3. interpreter execution

- **뜻:** 특정 opcode 이름과 sequence에 강하게 결합한 tool은 version migration에서 깨질 수 있다.
- **예시:** import dis / def choose(x):

```python
import dis

def choose(x):
    return x + 1 if x > 0 else 0

dis.dis(choose)
```

 


---

**직접 확인하기 — CHAPTER 05 · bytecode view는 source와 interpreter execution 사이의 중간 관찰 창이다**
CHAPTER 05 · bytecode view는 source와 interpreter execution 사이의 중간 관찰 창이다은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 규칙을 확인해야 오래 남는다. 먼저 입력이나 객체 하나만 두고 기대 결과를 적은 뒤 실행한다. 다음에는 값 하나, 호출 순서 하나, 경계 조건 하나만 바꿔 실제 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 05 · bytecode view는 source와 interpreter execution 사이의 중간 관찰 창이다의 규칙이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 좁히고, 수정 뒤 원래 예제와 반대 조건 예제를 모두 다시 실행한다. 마지막에는 왜 그런 결과가 나왔는지 자기 문장으로 설명해 본다.
## CHAPTER 06 · frame retention은 traceback·debugger가 예상보다 많은 memory를 붙잡게 할 수 있다

### 시작 전 용어집

#### 1. frame

- **뜻:** Exception을 장기 보관하면 traceback이 frame을 가리키고 frame이 local variable을 통해 대형 object graph를 붙잡을 수 있다.
- **왜 중요한가:** 특히 batch system이 실패 exception 전체를 in-memory history에 쌓으면 leak처럼 보이는 retention이 생길 수 있다.
- **예시:** Exception을 장기 보관하면 traceback이 frame을 가리키고 frame이 local …

#### 2. traceback

- **뜻:** 필요한 경우 traceback summary나 string을 추출하고 원본 exception/frame reference를 버린다.
- **왜 중요한가:** Debugger session이 끝난 뒤 frame reference를 계속 global에 보관하지 않는다.
- **예시:** 필요한 경우 traceback summary나 string을 추출하고 원본 exception/frame …

#### 3. debugger

- **뜻:** Memory investigation에서 “object가 왜 살아 있나”를 볼 때 traceback list, exception cache, coroutine frame도 root 후보로 본다.
- **예시:** Memory investigation에서 “object가 왜 살아 있나”를 볼 때 …

---

## CHAPTER 07 · introspection은 관찰 비용과 최적화 방해 가능성을 가진다

### 시작 전 용어집

#### 1. introspection

- **뜻:** Introspection API는 debugging에는 강력하지만 core business path의 정상 동작이 frame layout이나 function name 문자열에 의존하지 않게 한다.
- **왜 중요한가:** 모든 요청에서 frame을 걷고 local 전체를 serialize하면 observability 자체가 성능 문제를 만든다.
- **예시:** Introspection API는 debugging에는 강력하지만 core business path의 정상 …

#### 2. frame

- **뜻:** Sensitive input도 함께 수집될 수 있다.
- **왜 중요한가:** Instrumentation은 sampling, field allowlist, depth limit를 사용한다.
- **예시:** Sensitive input도 함께 수집될 수 있다.

#### 3. observability

- **뜻:** Error path에서만 상세 정보를 수집하거나 trace ID와 핵심 state만 남기는 방식으로 비용을 제한한다.
- **예시:** Error path에서만 상세 정보를 수집하거나 trace ID와 핵심 …

Refactoring 내성을 유지해야 한다.

---

## CHAPTER 08 · frame/code contract는 진단 데이터와 프로그램 의미를 구분한다

### 시작 전 용어집

#### 1. frame

- **뜻:** Code object와 frame을 이해하면 traceback, profiler, debugger를 더 정확히 해석할 수 있다.
- **왜 중요한가:** 그러나 이 정보는 대부분 실행을 관찰하기 위한 metadata이지 domain model 자체가 아니다.
- **예시:** Code object와 frame을 이해하면 traceback, profiler, debugger를 더 …

#### 2. code contract

- **뜻:** 테스트에서는 introspection tool이 Python minor version 변화에 얼마나 민감한지, frame을 저장했을 때 reference가 남지 않는지, redaction 정책이 local capture에도 적용되는지 확인한다.
- **왜 중요한가:** 이 PART의 핵심은 **함수 실행을 source line 하나로 보지 않고, 재사용되는 code object와 호출마다 만들어지는 frame의 결합으로 이해하면서 introspection의 lifetime·비용·version coupling까지 관리하는 것**이다.
- **예시:** 테스트에서는 introspection tool이 Python minor version 변화에 얼마나 …

---

## 실전 학습 루프 · frame·code introspection

### 1. 쉬운 예

함수 안에서 현재 실행 프레임을 들여다보면 local 변수, 호출 위치, code object 같은 실행 증거를 확인할 수 있다. 하지만 introspection은 구현 세부와 결합되기 쉬우므로 일반 업무 로직이 아니라 디버깅·도구 계층에 제한하는 편이 낫다.

### 2. 한 줄 해석

frame은 “지금 실행 중인 한 함수 호출의 상태”, code object는 “그 호출이 따르는 실행 코드의 메타데이터”다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상한다. 그 다음 아래 최소 예제를 실행하고, 예상이 틀렸다면 **호출 순서와 상태 변화**를 표시한다.

```python
import inspect

def demo(x):
    f = inspect.currentframe()
    print(f.f_code.co_name, f.f_locals['x'])

demo(7)
```

### 4. 수정 실습

1. `inspect.stack()`을 무분별하게 반복했을 때 비용을 생각한다.
2. 민감한 local 값이 로그에 노출되지 않도록 수집 범위를 줄인다.

수정 후에는 정상 예제만 다시 보지 말고 실패·경계·반복 호출 중 하나를 추가해 계약이 유지되는지 확인한다.

### 5. 확인 문제

frame을 읽을 수 있다는 이유로 production 로직이 frame 구조에 의존해도 될까?

### 6. 정답과 오답 설명

**정답:** 권장하지 않는다. 진단 도구에는 유용하지만 정상 기능의 계약을 실행 프레임 내부 구조에 묶으면 유지보수성이 떨어진다.

**자주 나오는 오답:** “볼 수 있는 정보는 모두 써도 된다”는 생각이 오답이다. introspection은 강력할수록 경계를 좁혀야 한다.

이 PART를 마칠 때는 해당 문법 이름을 외우는 데서 멈추지 말고 **언제 호출되는가 / 무엇을 읽거나 바꾸는가 / 실패하면 어디로 가는가** 세 문장으로 설명한다.

## 현장 디버깅 체크 · frame·code introspection

### 증상에서 시작한다

디버깅 도구를 켜면 latency가 급증하거나 로그에 예상하지 못한 지역 변수·secret이 노출된다. 이때 문법을 먼저 고치면 원인이 가려질 수 있다. 재현 입력과 실제 상태를 보존한 뒤 **어느 경계에서 처음 기대와 달라졌는지**를 찾는다.

### 먼저 볼 증거

stack depth, frame 수집 빈도, 수집 필드, 로그 payload 크기를 측정한다. 최종 출력 하나만 보지 말고 호출 전 값, 호출 뒤 값, 예외 또는 resource 상태를 나란히 두면 원인 후보가 급격히 줄어든다.

### 일부러 실패시켜 보기

hot loop에서 매번 stack 전체를 수집하는 버전과 필요한 frame 하나만 읽는 버전을 비교한다. 정상 예제만 통과시키는 것은 검증이 아니다. 경계 조건을 강제로 만들고 같은 증상이 반복되는지 확인해야 수정 전후를 비교할 수 있다.

### 통과 기준

진단 정보가 기능 동작을 바꾸지 않고, 필요한 최소 메타데이터만 수집하며 민감값은 노출되지 않아야 한다. 이 기준을 테스트 이름과 assertion으로 옮기면 이후 refactoring에서도 같은 오류가 돌아오는지 자동으로 잡을 수 있다.

