# PART 31 · Python execution pipeline — source에서 AST·code object·frame까지 실행 경로 읽기

Python source는 문장을 위에서 아래로 “그대로 실행”하는 검은 상자가 아니다. 구현은 source text를 문법 구조로 해석하고 executable representation을 만든 뒤 frame과 namespace를 사용해 operation을 평가한다. 이 경로를 이해하면 syntax error, scope 오류, traceback, decorator/import 시점, profiler 결과를 서로 연결할 수 있다. 핵심은 **source code의 표면과 runtime state 사이에 어떤 변환 단계가 존재하는지**를 구분하는 것이다.

---

## CHAPTER 01 · source text는 먼저 grammar에 따라 구조로 해석된다

### 시작 전 용어집

#### 1. source text

- **뜻:** Python file을 실행하려면 text가 language grammar를 만족하는지 먼저 판단해야 한다.
- **왜 중요한가:** 들여쓰기와 token sequence가 문법적으로 잘못되면 함수 body가 실제로 호출되기 전에도 syntax error가 발생할 수 있다.
- **예시:** 검증 단계가 syntax → name/runtime semantics로 이어진다는 뜻이다.

#### 2. grammar

- **뜻:** Runtime exception과 syntax error가 다른 단계에서 생기는 이유다.
- **왜 중요한가:** Parser가 만들어 내는 구조는 단순 문자열 줄 목록이 아니라 expression, assignment, function definition, branch처럼 언어 construct의 계층을 반영한다.
- **예시:** 검증 단계가 syntax → name/runtime semantics로 이어진다는 뜻이다.

#### 3. 함수

- **뜻:** `1 + 2 * 3`이 multiplication을 더 안쪽 node로 가지는 식으로 precedence가 tree 구조에 들어간다.
- **왜 중요한가:** 따라서 source formatting과 execution grouping을 같은 것으로 보지 않는다.
- **예시:** 검증 단계가 syntax → name/runtime semantics로 이어진다는 뜻이다.

#### 4. exception

- **뜻:** 문법적으로 valid하다는 사실은 이름이 존재하거나 type이 맞다는 뜻이 아니다.
- **왜 중요한가:** `unknown + 1`은 parse할 수 있어도 실행 시 이름을 찾지 못할 수 있다.
- **예시:** 검증 단계가 syntax → name/runtime semantics로 이어진다는 뜻이다.

검증 단계가 syntax → name/runtime semantics로 이어진다는 뜻이다.

Tooling도 이 구조를 이용한다. Formatter, linter, static analyzer가 raw text만 정규식으로 보는 것이 아니라 parser가 만든 syntax structure를 활용하면 multiline expression과 nested scope를 더 정확하게 이해할 수 있다.

---

## CHAPTER 02 · AST는 source의 의미 구조를 programmatic data로 다룰 수 있게 한다

### 시작 전 용어집

#### 1. AST

- **뜻:** 같은 의미를 다른 formatting으로 쓴 source도 유사한 AST 구조를 가질 수 있다.
- **왜 중요한가:** Python의 AST API를 사용하면 source를 안전하게 분석하는 도구를 만들 수 있지만 AST를 수정해 다시 compile하는 metaprogramming은 semantics를 정확히 이해해야 한다.
- **예시:** 같은 의미를 다른 formatting으로 쓴 source도 유사한 AST …

#### 2. source

- **뜻:** Source location metadata를 잃으면 traceback과 error message 품질이 떨어질 수 있다.
- **왜 중요한가:** Security scanner가 위험 함수 호출을 찾거나 migration tool이 deprecated API 사용을 바꾸는 데 AST가 유용하다.
- **예시:** Source location metadata를 잃으면 traceback과 error message 품질이 …

#### 3. programmatic data

- **뜻:** Abstract Syntax Tree는 괄호와 일부 표면 문법 세부사항을 넘어 program의 구조적 의미를 node로 표현한다.
- **왜 중요한가:** Function definition에는 parameter와 body가 있고 binary operation에는 left/operator/right가 있다.
- **예시:** Abstract Syntax Tree는 괄호와 일부 표면 문법 세부사항을 …

#### 4. parameter

- **뜻:** 단순 text replacement는 comment와 string literal까지 바꾸거나 alias/import를 놓칠 수 있다.
- **왜 중요한가:** 하지만 AST 분석도 dynamic attribute와 runtime import를 모두 완벽히 추론하지는 못한다.
- **예시:** 단순 text replacement는 comment와 string literal까지 바꾸거나 alias/import를 …

AST는 “실행 전 구조”다. 실제 object value와 branch 결과는 아직 존재하지 않는다. Static information과 runtime information을 구분하면 analyzer가 무엇을 알 수 있고 무엇을 실행해야만 알 수 있는지 선명해진다.

---

## CHAPTER 03 · compile 단계는 executable code object를 만들고 일부 scope 결정을 미리 한다

### 시작 전 용어집

#### 1. compile

- **뜻:** Function body는 호출될 때마다 source를 처음부터 parse하는 것이 아니라 compile된 code representation을 실행하는 구조를 가진다.
- **왜 중요한가:** Code object에는 instruction, constant, name reference, local variable metadata처럼 실행에 필요한 정보가 포함될 수 있다.
- **예시:** Function body는 호출될 때마다 source를 처음부터 parse하는 것이 …

#### 2. executable code

- **뜻:** 함수 객체는 이런 code object와 global namespace, default, closure 같은 context를 결합한다.
- **왜 중요한가:** 어떤 이름이 local로 취급되는지는 단순히 해당 줄 실행 시점에 결정되지 않는다.
- **예시:** 함수 객체는 이런 code object와 global namespace, default, …

#### 3. object

- **뜻:** Code object 존재가 모든 이름과 type correctness를 보장하지 않는다.
- **왜 중요한가:** Function body 안에 assignment가 존재하면 그 이름의 scope 판단이 전체 function compile 구조에 영향을 줄 수 있다.
- **예시:** Code object 존재가 모든 이름과 type correctness를 보장하지 …

#### 4. scope

- **뜻:** 그래서 global 이름을 먼저 읽은 뒤 같은 function 뒤쪽에서 대입하려는 코드가 `UnboundLocalError`를 만들 수 있다.
- **왜 중요한가:** `global`과 `nonlocal` 선언은 이런 binding 분류를 명시적으로 바꾼다.
- **예시:** 그래서 global 이름을 먼저 읽은 뒤 같은 function …

이 keyword를 단순 “바깥 변수 수정 허용”으로 외우기보다 compiler가 어느 namespace binding을 target으로 만들지 지정한다고 이해한다.

Compile 단계에서 잡을 수 있는 문제와 실행해야 발견되는 문제를 구분하면 static checker와 test가 맡는 역할도 정리된다. 

---

## CHAPTER 04 · frame은 한 번의 실행 호출에 필요한 지역 상태를 담는다

### 시작 전 용어집

#### 1. frame

- **뜻:** Function이 호출되면 parameter binding과 local variable, instruction position 같은 현재 실행 상태를 담는 frame이 만들어진다.
- **왜 중요한가:** 같은 function을 재귀적으로 여러 번 호출하면 같은 code object를 사용해도 서로 다른 frame이 쌓인다.
- **예시:** Function이 호출되면 parameter binding과 local variable, instruction position …

#### 2. 상태

- **뜻:** Debugger가 frame을 선택해 local variable을 보여 줄 수 있는 것도 동일한 실행 상태를 관찰하기 때문이다.
- **왜 중요한가:** Generator와 coroutine은 일반 function과 달리 frame state를 suspension 사이에 보존할 수 있다.
- **예시:** Debugger가 frame을 선택해 local variable을 보여 줄 수 …

#### 3. parameter

- **뜻:** 따라서 local variable은 function 정의가 아니라 각 호출 instance에 속한다.
- **왜 중요한가:** Traceback에 여러 frame이 나타나는 이유도 이 call chain 때문이다.
- **예시:** 따라서 local variable은 function 정의가 아니라 각 호출 …

#### 4. binding

- **뜻:** 실패한 instruction에서 상위 caller로 올라가며 어느 function의 어떤 호출 context였는지 보여 준다.
- **왜 중요한가:** `yield`나 `await` 이후 다시 시작할 수 있는 이유가 local state와 instruction position이 살아 있기 때문이다.
- **예시:** 실패한 instruction에서 상위 caller로 올라가며 어느 function의 어떤 …

실행 model이 lifetime과 memory retention에 연결된다.

Frame을 직접 introspection하는 기능은 debugging과 tooling에 유용하지만 application logic이 caller frame의 local 이름에 의존하면 매우 강한 coupling이 생긴다. Runtime 내부 구조를 public data API처럼 사용하지 않는다.

---

## CHAPTER 05 · name lookup은 source 이름을 현재 namespace chain의 객체 binding으로 해석한다

### 시작 전 용어집

#### 1. name lookup

- **뜻:** Attribute lookup은 단순 name lookup과 다른 protocol이다.
- **왜 중요한가:** value`는 객체/class/descriptor 체계를 거치고 module.
- **예시:** `list = []` 같은 코드가 문법 오류는 아니지만 …

#### 2. source

- **뜻:** Expression에서 `price`라는 이름을 보면 runtime은 해당 이름에 연결된 object를 scope 규칙에 따라 찾는다.
- **왜 중요한가:** Local, enclosing, global, built-in 영역이 구분되며 같은 spelling이 여러 namespace에 존재할 수 있다.
- **예시:** `list = []` 같은 코드가 문법 오류는 아니지만 …

#### 3. namespace chain

- **뜻:** Built-in 이름을 local variable로 덮어쓰면 그 scope 안에서는 원래 built-in에 쉽게 접근할 수 없게 된다.
- **왜 중요한가:** `list = []` 같은 코드가 문법 오류는 아니지만 나중에 `list(.
- **예시:** Built-in 이름을 local variable로 덮어쓰면 그 scope 안에서는 …

#### 4. 객체

- **뜻:** )` constructor를 호출하려 할 때 문제를 만든다.
- **왜 중요한가:** Naming rule은 style보다 name-resolution 충돌을 줄이는 역할도 한다.
- **예시:** )` constructor를 호출하려 할 때 문제를 만든다.

Name shadowing이 가능한 이유다.

 .. 

 `obj.name도 module object의 attribute 접근이다. `value`와 `obj.value`를 같은 lookup mechanism으로 생각하지 않는다.

Dynamic namespace modification과 `globals()` 같은 도구는 가능하지만 static reasoning을 어렵게 한다. 일반 application은 명시적 binding과 import를 선호하고 metaprogramming boundary에서만 dynamic mutation을 좁게 사용한다.

---

## CHAPTER 06 · bytecode는 interpreter가 실행하는 한 표현이지만 안정된 public machine language가 아니다

### 시작 전 용어집

#### 1. bytecode

- **뜻:** CPython 같은 구현은 code object 안의 bytecode instruction을 interpreter가 처리한다.
- **왜 중요한가:** Disassembler를 보면 name load, constant load, call, branch 같은 source operation이 여러 instruction으로 분해되는 모습을 볼 수 있다.
- **예시:** CPython 같은 구현은 code object 안의 bytecode instruction을 …

#### 2. interpreter

- **뜻:** 이를 통해 한 줄 source가 항상 atomic한 operation이 아니라는 점도 확인할 수 있다.
- **왜 중요한가:** 하지만 bytecode instruction set은 Python language specification과 동일한 안정성 계약이 아니다.
- **예시:** 이를 통해 한 줄 source가 항상 atomic한 operation이 …

#### 3. public machine

- **뜻:** Version이 바뀌면서 instruction이 추가·통합·변경될 수 있다.
- **왜 중요한가:** Application correctness를 특정 opcode 배열에 의존시키면 runtime upgrade에 취약하다.
- **예시:** Version이 바뀌면서 instruction이 추가·통합·변경될 수 있다.

#### 4. language

- **뜻:** Portable code는 language-level contract를 기준으로 작성하고 implementation-specific 최적화는 측정된 환경에서만 가정한다.
- **왜 중요한가:** Bytecode를 읽는 목적은 언어 semantics를 보조적으로 이해하거나 performance/debugging evidence를 얻는 데 있다.
- **예시:** Portable code는 language-level contract를 기준으로 작성하고 implementation-specific 최적화는 …

“이 version에서는 이렇게 compile됐다”는 관찰을 “Python은 반드시 영원히 이렇게 실행한다”는 규칙으로 확대하지 않는다.

Alternative Python implementation은 다른 execution representation을 사용할 수 있다. 

---

## CHAPTER 07 · function call 비용은 argument binding·frame·dispatch를 포함한 runtime operation이다

### 시작 전 용어집

#### 1. function call

- **뜻:** Function call micro-cost는 대개 더 큰 구조적 비용보다 작다.
- **왜 중요한가:** Profiler에서 실제 hot path가 작은 callback 호출에 집중되어 있을 때만 batch 처리, built-in/native operation 활용 같은 변경을 검토한다.
- **예시:** Function call micro-cost는 대개 더 큰 구조적 비용보다 …

#### 2. argument binding

- **뜻:** 작은 Python function도 호출하려면 callable을 찾고 argument를 평가하며 parameter를 bind하고 execution frame을 준비해 body를 실행한 뒤 return을 전달한다.
- **왜 중요한가:** 이 비용이 매우 작은 연산을 수백만 번 수행하는 hot loop에서는 눈에 띌 수 있다.
- **예시:** 작은 Python function도 호출하려면 callable을 찾고 argument를 평가하며 …

#### 3. frame

- **뜻:** 그렇다고 모든 helper를 inline으로 합치는 것이 좋은 설계는 아니다.
- **왜 중요한가:** 성능 문제가 확인되면 algorithm과 I/O를 먼저 본다.
- **예시:** 그렇다고 모든 helper를 inline으로 합치는 것이 좋은 설계는 …

#### 4. dispatch

- **뜻:** Keyword argument와 dynamic dispatch, decorator wrapper가 호출 경로를 더 길게 만들 수 있지만 가독성과 계약 분리의 가치도 있다.
- **왜 중요한가:** 성능 최적화는 abstraction을 무조건 제거하는 작업이 아니다.
- **예시:** Keyword argument와 dynamic dispatch, decorator wrapper가 호출 경로를 …

Benchmark는 realistic data와 충분한 반복으로 수행하고 interpreter warm-up, system noise, version을 기록한다. Microbenchmark 결과를 전체 request latency로 직접 일반화하지 않는다.

---

## CHAPTER 08 · exception은 현재 frame의 정상 instruction 흐름을 끊고 handler를 탐색한다

### 시작 전 용어집

#### 1. exception

- **뜻:** Exception이 발생하면 현재 expression의 정상 결과가 만들어지지 않고 적절한 handler를 찾는 제어 경로로 이동한다.
- **왜 중요한가:** 현재 frame에서 처리되지 않으면 caller frame으로 propagation되며 stack이 unwinding된다.
- **예시:** Exception이 발생하면 현재 expression의 정상 결과가 만들어지지 않고 …

#### 2. frame

- **뜻:** Exception traceback은 frame chain을 보존하지만 `except`에서 새 exception을 잘못 다시 만들면 원래 cause가 끊길 수 있다.
- **왜 중요한가:** Chaining을 유지하면 abstraction boundary와 기술 원인을 동시에 볼 수 있다.
- **예시:** Exception traceback은 frame chain을 보존하지만 `except`에서 새 exception을 …

#### 3. instruction

- **뜻:** `finally`와 context-manager cleanup이 이 unwinding 경로에서 실행될 수 있다.
- **왜 중요한가:** 이 모델 때문에 exception은 단순 return value와 다른 control flow 비용과 semantics를 가진다.
- **예시:** `finally`와 context-manager cleanup이 이 unwinding 경로에서 실행될 수 …

#### 4. handler

- **뜻:** 정상적으로 자주 발생하는 branch를 exception으로 표현할지 여부는 Python idiom과 workload를 함께 본다.
- **왜 중요한가:** 존재하지 않는 dict key처럼 exception-based API가 자연스러운 경우도 있다.
- **예시:** 정상적으로 자주 발생하는 branch를 exception으로 표현할지 여부는 Python …

Generator/coroutine의 exception propagation은 suspension boundary와 task scheduler까지 연결될 수 있다. Task result를 관찰하지 않으면 exception이 늦게 보고되거나 경고만 남을 수 있으므로 ownership이 필요하다.

---

## CHAPTER 09 · `eval`과 `exec`는 data를 language code로 승격시키는 강한 경계다

### 시작 전 용어집

#### 1. eval

- **뜻:** 산술식이나 규칙을 받는 기능이 필요하다고 곧바로 `eval(user_text)`로 구현하지 않는다.
- **왜 중요한가:** 허용 grammar를 tokenizer/parser로 만들고 AST variant를 제한해 evaluator가 승인된 operation만 수행하게 할 수 있다.
- **예시:** Python object model을 통해 예상하지 못한 capability에 접근할 …

#### 2. exec

- **뜻:** 문자열을 compile해 실행할 수 있는 기능은 metaprogramming과 interactive tool에서 유용하지만 untrusted input에 사용하면 입력자가 프로그램 권한으로 code를 실행할 수 있는 위험이 생긴다.
- **왜 중요한가:** 이미 P30의 rules engine처럼 code와 data의 경계를 명시하면 보안과 error reporting이 함께 좋아진다.
- **예시:** Python object model을 통해 예상하지 못한 capability에 접근할 …

#### 3. data

- **뜻:** Global/local dictionary를 제한해 eval을 호출한다고 완전한 sandbox가 되는 것도 아니다.
- **왜 중요한가:** Python object model을 통해 예상하지 못한 capability에 접근할 수 있는 경로가 있을 수 있으므로 hostile code sandbox는 별도 process/isolation 수준에서 다뤄야 한다.
- **예시:** Global/local dictionary를 제한해 eval을 호출한다고 완전한 sandbox가 되는 …

#### 4. language code

- **뜻:** Dynamic code generation이 필요한 framework 내부에서도 source provenance, cache, traceback line mapping을 관리한다.
- **왜 중요한가:** 실행 가능 text는 일반 configuration보다 훨씬 높은 trust level을 요구한다.
- **예시:** Dynamic code generation이 필요한 framework 내부에서도 source provenance, …

---

## CHAPTER 10 · 실행 경로를 이해하면 source·static analysis·runtime evidence를 같은 지도에 놓을 수 있다

### 시작 전 용어집

#### 1. source

- **뜻:** 버그를 조사할 때 source 한 줄만 보고 추측하지 않고 “이 이름은 어느 binding인가, 이 object는 언제 만들어졌나, 이 frame은 어떤 호출에서 왔나, exception이 어느 경계를 통과했나”를 질문할 수 있다.
- **왜 중요한가:** 복잡한 decorator나 async framework도 결국 object binding과 frame 진행의 조합으로 내려갈 수 있다.
- **예시:** 버그를 조사할 때 source 한 줄만 보고 추측하지 …

#### 2. static analysis

- **뜻:** Syntax error는 parser 단계, scope classification은 compile 구조, NameError와 type error는 runtime lookup/evaluation, traceback은 frame chain, profiler는 frame/instruction 수행 시간의 관측으로 연결된다.
- **왜 중요한가:** 서로 다른 도구가 사실 같은 execution pipeline의 다른 지점을 본다.
- **예시:** Syntax error는 parser 단계, scope classification은 compile 구조, …

#### 3. runtime evidence

- **뜻:** 성능에서도 source 줄 수보다 실행 operation 수와 object allocation, native boundary, I/O wait를 본다.
- **왜 중요한가:** 한 줄 comprehension이 긴 loop보다 항상 빠르거나 느린 것이 아니라 실제 실행 representation과 library implementation을 측정한다.
- **예시:** 성능에서도 source 줄 수보다 실행 operation 수와 object …

#### 4. scope

- **뜻:** Python 실행 모델을 배우는 목적은 opcode를 암기하는 것이 아니다.
- **예시:** Python 실행 모델을 배우는 목적은 opcode를 암기하는 것이 …

**source의 구조가 executable representation과 frame state로 어떻게 변환되는지 이해해 문법·scope·debugging·performance를 하나의 인과관계로 설명하는 것**이다.
