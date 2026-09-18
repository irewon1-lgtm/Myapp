# PART 77 · AST transformation — source를 구조로 읽고 의미 보존 변환을 검증하기

Python source를 문자열 검색과 치환으로 다루면 주석·문자열 literal·중첩 scope를 쉽게 오해한다. AST(Abstract Syntax Tree)는 parser가 source의 문법 구조를 node tree로 표현한 결과라서 static analysis, code generation, refactoring tool에 더 적합하다. 하지만 AST를 바꿨다는 사실만으로 의미가 보존되지는 않는다. 이 PART에서는 **parse → inspect → transform → compile → semantic verification** 순서를 잡는다.

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
