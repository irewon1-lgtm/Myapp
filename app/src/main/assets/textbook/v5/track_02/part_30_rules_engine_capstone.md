# PART 30 · Rules engine capstone — tokenizer·AST·type·validation·test를 하나의 작은 언어로 연결하기

프로그래밍 사고를 가장 압축해서 연습하는 방법 중 하나는 작은 규칙 언어를 직접 만드는 것이다. `age >= 19 and country == "KR"` 같은 표현을 문자열로 받아 token으로 나누고, grammar에 따라 AST를 만들고, type과 허용 operation을 검사한 뒤 안전하게 평가하면 지금까지 배운 값·상태·함수·오류·parser·variant·test가 하나의 pipeline으로 연결된다. 핵심은 `eval`을 쓰지 않고 **허용한 언어만 명시적으로 해석하는 것**이다.

---

## CHAPTER 01 · 먼저 언어가 표현할 수 있는 범위를 문법 전에 고정한다

### 시작 전 용어집

#### 1. boolean

- **뜻:** 이 작은 언어는 boolean, integer, string literal, 변수 이름, 비교 연산, `and/or/not`, 괄호만 지원한다고 하자.
- **왜 중요한가:** 함수 호출, attribute access, import, assignment는 지원하지 않는다.
- **예시:** 예를 들어 숫자와 문자열 비교를 허용할지, unknown variable은 …

#### 2. 함수

- **뜻:** 이렇게 capability를 먼저 제한하면 parser와 evaluator가 처리해야 할 상태 공간과 security risk가 동시에 줄어든다.
- **왜 중요한가:** 언어 spec에는 literal 범위, identifier 규칙, operator precedence, type error, unknown variable behavior를 적는다.
- **예시:** 예를 들어 숫자와 문자열 비교를 허용할지, unknown variable은 …

#### 3. import

- **뜻:** 예를 들어 숫자와 문자열 비교를 허용할지, unknown variable은 false인지 오류인지 결정한다.
- **왜 중요한가:** 기능 요청이 생길 때마다 Python 문법을 그대로 추가하지 않는다.
- **예시:** 예를 들어 숫자와 문자열 비교를 허용할지, unknown variable은 …

#### 4. 상태

- **뜻:** 실제 사용자 요구와 security budget을 보고 언어 표면을 확장한다.
- **예시:** 실제 사용자 요구와 security budget을 보고 언어 표면을 …

DSL은 작을수록 검증하기 쉽다.

---

## CHAPTER 02 · tokenizer는 raw text를 의미 단위로 분리하고 source 위치를 보존한다

### 시작 전 용어집

#### 1. tokenizer

- **뜻:** Tokenizer는 현재 위치에서 허용 token 중 하나가 반드시 매칭되어야 한다.
- **왜 중요한가:** 인식되지 않는 문자를 조용히 건너뛰면 공격자나 사용자의 오타가 다른 의미로 해석될 수 있다.
- **예시:** 입력 `age >= 19`를 `IDENT(age)`, `GTE`, `INT(19)` token으로 …

#### 2. raw text

- **뜻:** 입력 `age >= 19`를 `IDENT(age)`, `GTE`, `INT(19)` token으로 바꾼다.
- **왜 중요한가:** Whitespace는 무시할 수 있지만 문자열 literal 내부 공백은 보존해야 한다.
- **예시:** 입력 `age >= 19`를 `IDENT(age)`, `GTE`, `INT(19)` token으로 …

#### 3. source

- **뜻:** Token마다 start/end offset을 저장하면 이후 syntax error가 원본의 어느 위치인지 알려 줄 수 있다.
- **왜 중요한가:** 문자열 escape와 최대 literal 길이도 contract다.
- **예시:** Token마다 start/end offset을 저장하면 이후 syntax error가 원본의 …

#### 4. 입력

- **뜻:** 매우 긴 token 하나로 memory를 과도하게 사용하지 않도록 input size와 token size limit을 둔다.
- **예시:** 매우 긴 token 하나로 memory를 과도하게 사용하지 않도록 …

---

## CHAPTER 03 · parser는 precedence를 AST 구조에 반영한다

### 시작 전 용어집

#### 1. parser

- **뜻:** Parser의 출력은 문자열이 아니라 `Or(And(Not(Name("a")), Name("b")), Name("c"))` 같은 AST다.
- **왜 중요한가:** 괄호는 parse structure를 바꾸지만 AST에 별도 node로 남길 필요가 없을 수 있다.
- **예시:** `age >=`처럼 operand가 없는 경우 evaluator까지 보내지 않고 …

#### 2. precedence

- **뜻:** 일반적으로 `not`이 가장 강하고 `and`, `or` 순서를 정해 recursive descent function을 precedence level별로 나눌 수 있다.
- **왜 중요한가:** Syntax error에는 현재 token과 기대한 token class를 포함한다.
- **예시:** `age >=`처럼 operand가 없는 경우 evaluator까지 보내지 않고 …

#### 3. AST

- **뜻:** `not a and b or c`를 어떤 순서로 묶을지 grammar가 정한다.
- **왜 중요한가:** `age >=`처럼 operand가 없는 경우 evaluator까지 보내지 않고 parser에서 거부한다.
- **예시:** `not a and b or c`를 어떤 순서로 …

---

## CHAPTER 04 · AST variant는 가능한 문법 구조를 타입으로 고정한다

### 시작 전 용어집

#### 1. AST

- **뜻:** AST를 immutable하게 만들면 parse 후 evaluation 중 structure가 바뀌지 않는다는 가정을 할 수 있다.
- **왜 중요한가:** 같은 AST를 여러 input environment에 반복 평가하기도 쉽다.
- **예시:** AST를 immutable하게 만들면 parse 후 evaluation 중 structure가 …

#### 2. AST variant

- **뜻:** Literal, Name, Compare, And, Or, Not node를 별도 dataclass로 만들면 evaluator가 raw dict key를 추측하지 않아도 된다.
- **왜 중요한가:** Compare에는 left, operator, right가 있고 Not에는 operand 하나만 있다.
- **예시:** Literal, Name, Compare, And, Or, Not node를 별도 …

#### 3. 타입

- **뜻:** Pattern matching으로 node variant를 dispatch할 수 있고 static type union으로 exhaustiveness를 확인할 수 있다.
- **왜 중요한가:** 새 node가 추가됐는데 evaluator가 처리하지 않으면 test/type check가 실패하도록 설계한다.
- **예시:** Pattern matching으로 node variant를 dispatch할 수 있고 static …

각 node는 필요한 field만 가진다. 

 

 

---

## CHAPTER 05 · semantic validation은 syntax가 맞는 표현의 의미 오류를 찾는다

### 시작 전 용어집

#### 1. semantic validation

- **뜻:** `19 >= age`는 syntax로는 유효할 수 있지만 policy에서 허용할 수 있고, `age >= "nineteen"`은 type mismatch일 수 있다.
- **왜 중요한가:** Variable schema가 `age:int`, `country:str`를 알고 있다면 evaluator 전에 AST type validation을 수행할 수 있다.
- **예시:** `19 >= age`는 syntax로는 유효할 수 있지만 policy에서 …

#### 2. syntax

- **뜻:** Unknown variable도 이 단계에서 거부하면 runtime environment typo를 빨리 찾는다.
- **왜 중요한가:** 운영 중 동적으로 field가 추가되는 system이라면 unknown policy를 versioned schema로 관리한다.
- **예시:** Unknown variable도 이 단계에서 거부하면 runtime environment typo를 …

#### 3. 오류

- **뜻:** Operator별 허용 type table을 만들면 evaluator가 매번 ad-hoc `isinstance` branch를 반복하지 않아도 된다.
- **왜 중요한가:** Validation 성공 후 AST에 inferred type metadata를 붙이거나 별도 typed AST로 변환할 수 있다.
- **예시:** Operator별 허용 type table을 만들면 evaluator가 매번 ad-hoc …

---

## CHAPTER 06 · evaluator는 AST와 environment를 받아 순수하게 결과를 계산한다

### 시작 전 용어집

#### 1. evaluator

- **뜻:** Evaluator는 `environment: dict[str, domain value]`와 AST를 입력으로 받아 boolean 결과를 만든다.
- **왜 중요한가:** File, network, current time을 직접 읽지 않으면 같은 AST와 environment에서 deterministic하다.
- **예시:** Evaluator는 `environment: dict[str, domain value]`와 AST를 입력으로 받아 …

#### 2. AST

- **뜻:** 매우 깊은 AST나 거대한 expression이 CPU와 call stack을 고갈시키지 않게 parser 단계에서 제한할 수 있다.
- **왜 중요한가:** `and/or`는 short-circuit semantics를 구현한다.
- **예시:** 매우 깊은 AST나 거대한 expression이 CPU와 call stack을 …

#### 3. environment

- **뜻:** Left가 결과를 결정하면 right를 평가하지 않는다.
- **왜 중요한가:** 현재 DSL에 function call이 없으므로 side effect 차이는 없지만 expensive nested rule 평가를 줄일 수 있다.
- **예시:** Left가 결과를 결정하면 right를 평가하지 않는다.

#### 4. dict

- **뜻:** Evaluation depth와 node count에 budget을 둔다.
- **예시:** Evaluation depth와 node count에 budget을 둔다.

---

## CHAPTER 07 · 사용자-facing error와 developer diagnostic을 분리한다

### 시작 전 용어집

#### 1. facing error

- **뜻:** Syntax error에는 line/column, offending token, expected structure를 제공할 수 있다.
- **왜 중요한가:** Type error는 `age(int) cannot be compared with string`처럼 domain language로 설명한다.
- **예시:** Syntax error에는 line/column, offending token, expected structure를 제공할 …

#### 2. developer diagnostic

- **뜻:** Internal exception stack을 그대로 사용자에게 노출하지 않는다.
- **왜 중요한가:** 동시에 developer log에는 rule ID, schema version, parser/evaluator stage, safe input fingerprint를 남겨 incident를 재현할 수 있게 한다.
- **예시:** Internal exception stack을 그대로 사용자에게 노출하지 않는다.

#### 3. exception

- **뜻:** Expression 원문에 개인정보가 포함될 수 있다면 전체를 log하지 않는다.
- **왜 중요한가:** Error code를 안정적으로 정의하면 UI가 특정 오류를 field highlight로 연결할 수 있다.
- **예시:** Expression 원문에 개인정보가 포함될 수 있다면 전체를 log하지 …

#### 4. 오류

- **뜻:** Message text 변경과 machine behavior를 분리한다.
- **예시:** Message text 변경과 machine behavior를 분리한다.

---

## CHAPTER 08 · test는 lexer·parser·type checker·evaluator를 다른 failure class로 나눈다

### 시작 전 용어집

#### 1. test

- **뜻:** Tokenizer test는 escape와 token boundary를, parser test는 precedence와 괄호를, semantic test는 type mismatch와 unknown name을, evaluator test는 truth table과 short-circuit를 검증한다.
- **왜 중요한가:** End-to-end test는 대표 rule 몇 개만 전체 pipeline으로 통과시킨다.
- **예시:** 모든 test를 한 함수에서 문자열→result만 비교하면 실패가 어느 …

#### 2. lexer

- **뜻:** Property test로 `parse(format(ast))` round trip이나 boolean normalization invariant를 검증할 수 있다.
- **왜 중요한가:** Fuzzing으로 random text를 parser에 넣어 crash나 excessive runtime이 없는지 확인할 수 있다.
- **예시:** 모든 test를 한 함수에서 문자열→result만 비교하면 실패가 어느 …

#### 3. parser

- **뜻:** Regression test에는 발견된 실제 bug의 최소 input을 남긴다.
- **왜 중요한가:** 모든 test를 한 함수에서 문자열→result만 비교하면 실패가 어느 stage인지 찾기 어렵다.
- **예시:** Regression test에는 발견된 실제 bug의 최소 input을 남긴다.

---

## CHAPTER 09 · versioning은 저장된 rule과 evaluator semantics를 연결한다

### 시작 전 용어집

#### 1. versioning

- **뜻:** 규칙을 database에 저장한다면 나중에 operator precedence나 type coercion rule을 바꿨을 때 과거 rule의 의미가 달라질 수 있다.
- **왜 중요한가:** Language version을 저장하고 evaluator가 어떤 semantics로 해석했는지 추적한다.
- **예시:** 규칙을 database에 저장한다면 나중에 operator precedence나 type coercion …

#### 2. rule

- **뜻:** 새 version에서 문법을 확장할 때 old parser가 new syntax를 거부하는지, new parser가 old rule을 같은 의미로 평가하는지 compatibility test를 만든다.
- **왜 중요한가:** Rule migration이 필요하면 source text를 단순 문자열 replace하기보다 AST로 parse해 구조적으로 변환하는 편이 안전할 수 있다.
- **예시:** 새 version에서 문법을 확장할 때 old parser가 new …

#### 3. evaluator semantics

- **뜻:** Migration 후 before/after sample environment에서 의미가 유지되는지 비교한다.
- **예시:** Migration 후 before/after sample environment에서 의미가 유지되는지 비교한다.

---

## CHAPTER 10 · 작은 언어를 만들면 프로그래밍의 핵심 경계가 한눈에 보인다

### 시작 전 용어집

#### 1. 경계

- **뜻:** Raw text는 tokenizer를 거쳐 token이 되고 parser에서 AST가 되며 semantic validator가 허용 상태를 좁히고 evaluator가 environment와 결합해 결과를 만든다.
- **왜 중요한가:** 각 단계는 다른 input/output type과 실패를 가진다.
- **예시:** Raw text는 tokenizer를 거쳐 token이 되고 parser에서 AST가 …

#### 2. AST

- **뜻:** 이 pipeline은 일반 application architecture와 같다.
- **왜 중요한가:** 외부 표현을 parse하고 validate해 domain model을 만들고, pure core가 계산하며 adapter가 결과를 다시 외부 형식으로 바꾼다.
- **예시:** 이 pipeline은 일반 application architecture와 같다.

#### 3. 상태

- **뜻:** Security는 허용 language를 제한하고 resource budget을 두는 데서 시작한다.
- **왜 중요한가:** Capstone의 목적은 compiler를 만드는 것이 아니다.
- **예시:** Security는 허용 language를 제한하고 resource budget을 두는 데서 …

**문법, 타입, 상태, 함수, 오류, testing, versioning을 하나의 작은 시스템 안에서 연결해 프로그램을 계약의 연속으로 보는 사고를 굳히는 것**이다.
