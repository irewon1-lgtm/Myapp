# PART 77 · AST transformation — source를 구조로 읽고 의미 보존 변환을 검증하기

Python source를 문자열 검색과 치환으로 다루면 주석·문자열 literal·중첩 scope를 쉽게 오해한다. AST(Abstract Syntax Tree)는 parser가 source의 문법 구조를 node tree로 표현한 결과라서 static analysis, code generation, refactoring tool에 더 적합하다. 하지만 AST를 바꿨다는 사실만으로 의미가 보존되지는 않는다. 이 절에서는 **parse → inspect → transform → compile → semantic verification** 순서를 잡는다.

---

## CHAPTER 01 · `ast.parse`는 source text를 문법 구조로 바꾼다

### 시작 전 용어집

#### 1. AST

- **뜻:** 결과에는 module, assignment, name, binary operation 같은 node가 나타난다.
- **왜 중요한가:** 문자열에서 `*` 문자를 찾는 것보다 “multiplication expression”이라는 구조를 직접 볼 수 있다.
- **예시:** import ast / tree = ast.parse("total = price …

#### 2. parse

- **뜻:** `price`가 int인지 custom object인지, multiplication이 어떤 side effect를 갖는지는 execution과 type 정보 없이는 확정할 수 없다.
- **왜 중요한가:** Syntax structure와 runtime semantics를 구분한다.
- **예시:** import ast / tree = ast.parse("total = price …

```python
import ast

tree = ast.parse("total = price * quantity")
print(ast.dump(tree, indent=2))
```

 

AST는 runtime value를 알지 못한다.  

---

**검증 시나리오 P77-C1 — CHAPTER 01 · `ast.parse`는 source text를 문법 구조로 바꾼다**
`CHAPTER 01 · `ast.parse`는 source text를 문법 구조로 바꾼다` 검증은 성공 사례를 기준선으로 저장하는 데서 시작한다. P77-C1에서는 `CHAPTER 01 · `ast.parse`는 source text를 문법 구조로 바꾼다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 01 · `ast.parse`는 source text를 문법 구조로 바꾼다`에 대해 실패 조건은 하나만 주입고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 01 · `ast.parse`는 source text를 문법 구조로 바꾼다`의 로그 시각과 상태 식별자를 맞춰 본다하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 01 · `ast.parse`는 source text를 문법 구조로 바꾼다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P77-C1의 마무리는 성공·실패 모두 결정적으로 끝나는지 확인하는 것이다. 통과 기준은 `CHAPTER 01 · `ast.parse`는 source text를 문법 구조로 바꾼다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 02 · node location은 분석 결과를 다시 source 위치와 연결한다

### 시작 전 용어집

#### 1. node location

- **뜻:** Static analyzer가 “문제가 있다”고만 말하면 사용자는 고치기 어렵다.
- **왜 중요한가:** AST node의 line/column 관련 metadata를 이용하면 어떤 expression에서 규칙이 위반됐는지 source 위치를 제시할 수 있다.
- **예시:** Static analyzer가 “문제가 있다”고만 말하면 사용자는 고치기 어렵다.

#### 2. source

- **뜻:** Source map이 필요한 tool에서는 file revision과 encoding도 함께 관리한다.
- **왜 중요한가:** Line number만 저장해 두고 source가 바뀌면 진단 위치가 다른 코드로 이동할 수 있다.
- **예시:** Source map이 필요한 tool에서는 file revision과 encoding도 함께 …

#### 3. AST

- **뜻:** Code transformation 후 location metadata가 누락되면 traceback이나 compile error 위치가 부정확해질 수 있다.
- **왜 중요한가:** 새 node에 위치 정보를 복사하거나 필요한 fix-up을 수행한다.
- **예시:** Code transformation 후 location metadata가 누락되면 traceback이나 compile …

---

## CHAPTER 03 · NodeVisitor와 NodeTransformer는 읽기와 변경의 책임을 분리한다

### 시작 전 용어집

#### 1. NodeVisitor

- **뜻:** 분석만 필요하면 visitor로 tree를 순회하며 원하는 node를 찾고, 구조를 바꿔야 하면 transformer가 적합하다.
- **왜 중요한가:** Visitor에서 `generic_visit` 호출을 빠뜨리면 child node가 분석되지 않을 수 있다.
- **예시:** class CallCounter(ast.NodeVisitor): / def __init__(self):

#### 2. NodeTransformer

- **뜻:** Transformer는 node를 교체하거나 제거할 수 있으므로 더 큰 책임이 있다.
- **왜 중요한가:** 한 tool에 분석과 변경을 섞기보다 먼저 facts를 수집하고, 명시적 transform phase에서 변경하며, 마지막에 검증하는 구조가 디버깅하기 쉽다.
- **예시:** class CallCounter(ast.NodeVisitor): / def __init__(self):

```python
class CallCounter(ast.NodeVisitor):
    def __init__(self):
        self.count = 0

    def visit_Call(self, node):
        self.count += 1
        self.generic_visit(node)
```

 


---

**검증 시나리오 P77-C3 — CHAPTER 03 · NodeVisitor와 NodeTransformer는 읽기와 변경의 책임을 분리한다**
`CHAPTER 03 · NodeVisitor와 NodeTransformer는 읽기와 변경의 책임을 분리한다` 검증은 기준 입력을 한 개 고정하는 데서 시작한다. P77-C3에서는 `CHAPTER 03 · NodeVisitor와 NodeTransformer는 읽기와 변경의 책임을 분리한다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 03 · NodeVisitor와 NodeTransformer는 읽기와 변경의 책임을 분리한다`에 대해 반대 조건 입력을 한 개 추가고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 03 · NodeVisitor와 NodeTransformer는 읽기와 변경의 책임을 분리한다`의 실행 전후 값을 표로 대조하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 03 · NodeVisitor와 NodeTransformer는 읽기와 변경의 책임을 분리한다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P77-C3의 마무리는 예상과 다르면 최초 차이 지점을 추적하는 것이다. 통과 기준은 `CHAPTER 03 · NodeVisitor와 NodeTransformer는 읽기와 변경의 책임을 분리한다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 04 · 수정한 AST도 다시 compile 가능한 구조와 metadata를 만족해야 한다

### 시작 전 용어집

#### 1. AST

- **뜻:** AST node를 임의로 조립하면 required field나 context가 빠질 수 있다.
- **왜 중요한가:** 예를 들어 `Name` node가 load인지 store인지에 따라 의미가 다르다.
- **예시:** expr = ast.Expression( / body=ast.BinOp(

#### 2. compile

- **뜻:** Compile 성공은 최소 structural validity를 확인하지만 business semantics까지 보증하지 않는다.
- **왜 중요한가:** Generated code가 의도한 namespace와 side effect를 갖는지 별도 test가 필요하다.
- **예시:** expr = ast.Expression( / body=ast.BinOp(

#### 3. metadata

- **뜻:** AST version은 Python grammar 변화의 영향을 받을 수 있으므로 tool이 지원하는 interpreter version 범위를 명시한다.
- **예시:** expr = ast.Expression( / body=ast.BinOp(

```python
expr = ast.Expression(
    body=ast.BinOp(
        left=ast.Constant(2),
        op=ast.Add(),
        right=ast.Constant(3),
    )
)
ast.fix_missing_locations(expr)
code = compile(expr, "<generated>", "eval")
```

 


---

**검증 시나리오 P77-C4 — CHAPTER 04 · 수정한 AST도 다시 compile 가능한 구조와 metadata를 만족해야 한다**
`CHAPTER 04 · 수정한 AST도 다시 compile 가능한 구조와 metadata를 만족해야 한다` 검증은 가장 작은 객체 상태로 시작하는 데서 시작한다. P77-C4에서는 `CHAPTER 04 · 수정한 AST도 다시 compile 가능한 구조와 metadata를 만족해야 한다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 04 · 수정한 AST도 다시 compile 가능한 구조와 metadata를 만족해야 한다`에 대해 속성 하나만 바꿔 재실행고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 04 · 수정한 AST도 다시 compile 가능한 구조와 metadata를 만족해야 한다`의 반환값과 부수효과를 따로 기록하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 04 · 수정한 AST도 다시 compile 가능한 구조와 metadata를 만족해야 한다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P77-C4의 마무리는 수정 뒤 원래 조건을 다시 회귀 확인하는 것이다. 통과 기준은 `CHAPTER 04 · 수정한 AST도 다시 compile 가능한 구조와 metadata를 만족해야 한다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 05 · semantic preservation은 모양이 비슷한 AST보다 관찰 가능한 behavior로 검증한다

### 시작 전 용어집

#### 1. semantic preservation

- **뜻:** Refactoring tool이 `x + 0`을 `x`로 바꾸면 숫자에서는 같아 보이지만 custom object의 `__add__` side effect나 return type에서는 다를 수 있다.
- **왜 중요한가:** Python은 operator overloading과 dynamic lookup이 있으므로 algebraic rewrite를 무조건 적용하면 위험하다.
- **예시:** Refactoring tool이 `x + 0`을 `x`로 바꾸면 숫자에서는 …

#### 2. AST

- **뜻:** 같은 이유로 function call 순서, short-circuit boolean expression, comprehension evaluation order를 바꾸면 결과가 달라질 수 있다.
- **왜 중요한가:** Safe transformation은 적용 조건을 보수적으로 제한한다.
- **예시:** 같은 이유로 function call 순서, short-circuit boolean expression, …

#### 3. behavior

- **뜻:** Type 정보가 확실하지 않다면 syntax simplification을 포기하는 것이 잘못된 optimization보다 낫다.
- **왜 중요한가:** 변환 전후 테스트는 return value뿐 아니라 exception, side effect, call order까지 포함한다.
- **예시:** Type 정보가 확실하지 않다면 syntax simplification을 포기하는 것이 …

---

## CHAPTER 06 · AST는 source의 모든 표면 정보를 보존하는 round-trip 형식이 아니다

### 시작 전 용어집

#### 1. AST

- **뜻:** Comments, original whitespace, quote style 같은 정보는 일반 AST에서 그대로 유지되지 않을 수 있다.
- **왜 중요한가:** 따라서 formatter나 codemod가 source를 다시 생성할 때 원본 formatting을 보존해야 한다면 concrete syntax tree 계열 도구가 더 적합할 수 있다.
- **예시:** Comments, original whitespace, quote style 같은 정보는 일반 …

#### 2. source

- **뜻:** Generated source를 version control에 쓰는 codemod라면 불필요한 전체 formatting change를 피해야 review가 가능하다.
- **왜 중요한가:** AST는 의미 구조 분석에 강하지만 exact text editing에는 정보가 부족할 수 있다는 뜻이다.
- **예시:** Generated source를 version control에 쓰는 codemod라면 불필요한 전체 …

#### 3. round-trip

- **뜻:** Tool 목적에 맞는 representation을 선택한다.
- **예시:** Tool 목적에 맞는 representation을 선택한다.

의미 변경과 format churn을 분리한다.

---

## CHAPTER 07 · AST allowlist만으로 완전한 security sandbox가 되지는 않는다

### 시작 전 용어집

#### 1. AST

- **뜻:** `eval` 전에 AST를 검사해 허용 node만 통과시키면 attack surface를 줄일 수 있다.
- **왜 중요한가:** 하지만 허용된 attribute access나 call을 통해 위험한 capability에 도달할 수 있고, 매우 큰 expression으로 resource를 소진할 수도 있다.
- **예시:** 또 grammar 변화로 새 node가 생기면 validator가 예상하지 …

#### 2. AST allowlist

- **뜻:** 따라서 AST validation은 한 층의 policy enforcement다.
- **왜 중요한가:** Namespace capability 제한, process isolation, execution timeout 같은 별도 방어가 필요할 수 있다.
- **예시:** 또 grammar 변화로 새 node가 생기면 validator가 예상하지 …

#### 3. security sandbox

- **뜻:** 또 grammar 변화로 새 node가 생기면 validator가 예상하지 못한 default behavior를 보일 수 있다.
- **왜 중요한가:** Unknown node는 기본 거부하는 fail-closed 설계를 고려한다.
- **예시:** 또 grammar 변화로 새 node가 생기면 validator가 예상하지 …

---

## CHAPTER 08 · AST contract는 분석 정확도·version 범위·변환 검증을 함께 정의한다

### 시작 전 용어집

#### 1. AST

- **뜻:** AST 기반 tool을 만들 때는 어떤 Python version을 지원하는지, 어떤 node를 이해하는지, 분석 결과가 runtime fact인지 syntax-level hint인지 구분한다.
- **왜 중요한가:** Transformer라면 어떤 조건에서만 rewrite하는지 명시한다.
- **예시:** AST 기반 tool을 만들 때는 어떤 Python version을 …

#### 2. AST contract

- **뜻:** 테스트 corpus에는 nested scope, comprehension, pattern matching, decorator, async syntax, syntax error를 포함한다.
- **왜 중요한가:** 변환 tool은 compile 성공뿐 아니라 golden behavior test로 전후 의미를 확인한다.
- **예시:** 테스트 corpus에는 nested scope, comprehension, pattern matching, decorator, …

#### 3. version

- **뜻:** 이 PART의 핵심은 **source를 문자열이 아니라 syntax tree로 다루되, AST가 runtime semantics와 원본 formatting을 모두 보존하는 만능 표현은 아니라는 경계를 이해하는 것**이다.
- **예시:** 이 PART의 핵심은 **source를 문자열이 아니라 syntax tree로 …

---

## 실전 학습 루프 · AST transformation

### 1. 쉬운 예

소스 문자열을 단순 치환하면 주석·문자열·scope까지 잘못 바꿀 수 있다. AST를 사용하면 문법 구조를 기준으로 특정 node만 변환할 수 있지만, 변환 후에도 위치 정보와 의미 보존을 검증해야 한다.

### 2. 한 줄 해석

AST 변환은 텍스트 편집이 아니라 프로그램 구조를 다른 구조로 바꾸는 compiler 작업에 가깝다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상한다. 그 다음 아래 최소 예제를 실행하고, 예상이 틀렸다면 **호출 순서와 상태 변화**를 표시한다.

```python
import ast

tree = ast.parse('x = a + 1')
print(ast.dump(tree, indent=2))
```

### 4. 수정 실습

1. 모든 숫자 literal을 바꾸는 NodeTransformer를 만들고 예상치 못한 위치까지 바뀌지 않는지 본다.
2. 변환 뒤 `ast.fix_missing_locations()`가 필요한 상황을 확인한다.

수정 후에는 정상 예제만 다시 보지 말고 실패·경계·반복 호출 중 하나를 추가해 계약이 유지되는지 확인한다.

### 5. 확인 문제

정규식으로 Python 소스를 바꾸는 것과 AST 변환은 같은 수준의 작업일까?

### 6. 정답과 오답 설명

**정답:** 아니다. AST는 syntax structure를 보존하며 node 종류와 위치를 기준으로 변경 범위를 제한할 수 있다.

**자주 나오는 오답:** “텍스트가 같으면 의미도 같다”거나 “AST만 만들면 의미 보존이 자동 보장된다”는 둘 다 오답이다.

이 PART를 마칠 때는 해당 문법 이름을 외우는 데서 멈추지 말고 **언제 호출되는가 / 무엇을 읽거나 바꾸는가 / 실패하면 어디로 가는가** 세 문장으로 설명한다.

## 현장 디버깅 체크 · AST transformation

### 증상에서 시작한다

자동 리팩터링 뒤 코드는 parse되지만 변수 scope나 평가 순서가 바뀌어 결과가 달라진다. 이때 문법을 먼저 고치면 원인이 가려질 수 있다. 재현 입력과 실제 상태를 보존한 뒤 **어느 경계에서 처음 기대와 달라졌는지**를 찾는다.

### 먼저 볼 증거

변환 전후 AST, source location, symbol scope, 실행 결과와 regression test를 함께 비교한다. 최종 출력 하나만 보지 말고 호출 전 값, 호출 뒤 값, 예외 또는 resource 상태를 나란히 두면 원인 후보가 급격히 줄어든다.

### 일부러 실패시켜 보기

같은 이름이 local/global/comprehension scope에 각각 있는 예제를 만들어 target node만 바뀌는지 본다. 정상 예제만 통과시키는 것은 검증이 아니다. 경계 조건을 강제로 만들고 같은 증상이 반복되는지 확인해야 수정 전후를 비교할 수 있다.

### 통과 기준

변환 대상 node만 변경되고 나머지 의미·위치 정보·테스트 결과는 보존돼야 한다. 이 기준을 테스트 이름과 assertion으로 옮기면 이후 refactoring에서도 같은 오류가 돌아오는지 자동으로 잡을 수 있다.

