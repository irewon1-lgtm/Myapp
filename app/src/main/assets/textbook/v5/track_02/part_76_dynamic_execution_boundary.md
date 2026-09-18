# PART 76 · Dynamic execution boundary — `compile`·`eval`·`exec`와 namespace·보안 경계를 분리하기

Python은 source text를 runtime에 compile하고 실행할 수 있다. 이 기능은 REPL, template engine, rule system, debugger를 만들 때 강력하지만 “문자열을 코드로 바꾼다”는 순간 데이터와 프로그램의 경계가 사라질 수 있다. 특히 입력이 신뢰되지 않으면 namespace를 제한하는 정도로 안전한 sandbox가 된다고 생각해서는 안 된다. 이 PART에서는 **compile 단계, 실행 namespace, trust boundary**를 분리한다.

---

## CHAPTER 01 · `compile`은 source text를 실행 가능한 code object로 바꾸는 단계다

### 시작 전 용어집

#### 1. compile

- **뜻:** `compile(source, filename, mode)`은 source를 바로 실행하지 않고 code object로 변환한다.
- **왜 중요한가:** `mode`는 expression, statement block 등 기대 문법 형태를 구분한다.
- **예시:** code = compile("a + b", "<rule>", "eval")

#### 2. source text

- **뜻:** Compile 단계에서 syntax error가 발생할 수 있고, filename metadata는 traceback에 사용된다.
- **왜 중요한가:** Dynamic source가 어디서 왔는지 식별 가능한 synthetic filename을 사용하면 운영 진단에 도움이 된다.
- **예시:** code = compile("a + b", "<rule>", "eval")

#### 3. code object

- **뜻:** 같은 source를 반복 실행한다면 매번 parse/compile하지 않고 code object를 cache할 수 있지만 source version과 cache key를 정확히 관리해야 한다.
- **예시:** code = compile("a + b", "<rule>", "eval")

```python
code = compile("a + b", "<rule>", "eval")
```

 


---

## CHAPTER 02 · `eval`은 하나의 expression 결과를 계산하는 실행 경로다

### 시작 전 용어집

#### 1. eval

- **뜻:** `eval`은 expression을 평가해 결과를 반환한다.
- **왜 중요한가:** 이 예시는 namespace를 작게 보이게 할 수 있지만 이것만으로 untrusted code sandbox가 되는 것은 아니다.
- **예시:** result = eval("price * quantity", {"__builtins__": {}}, {"price": …

#### 2. expression

- **뜻:** 신뢰된 내부 expression을 configuration-like 용도로 쓰더라도 허용 문법이 좁다면 AST를 검사하거나 직접 parser를 만드는 편이 더 안전하고 예측 가능할 수 있다.
- **왜 중요한가:** `eval`을 편의 계산기로 사용하기 전에 정말 Python 전체 expression semantics가 필요한지 묻는다.
- **예시:** result = eval("price * quantity", {"__builtins__": {}}, {"price": …

#### 3. introspection

- **뜻:** Python object model과 introspection을 이용하면 예상보다 넓은 표면에 접근할 수 있다.
- **예시:** result = eval("price * quantity", {"__builtins__": {}}, {"price": …

```python
result = eval("price * quantity", {"__builtins__": {}}, {"price": 10, "quantity": 3})
```

 


---

## CHAPTER 03 · `exec`는 statement block을 지정한 namespace에서 실행한다

### 시작 전 용어집

#### 1. exec

- **뜻:** `exec`는 assignment, function/class definition을 포함한 statement block을 실행할 수 있다.
- **왜 중요한가:** 이 경우 실행 후 namespace가 mutation된다.
- **예시:** ns = {} / exec("x = 40\ny = …

#### 2. statement block

- **뜻:** Global과 local mapping을 별도로 넘기면 name resolution semantics가 일반 module execution과 완전히 같다고 단정할 수 없으므로 language reference 기준으로 이해해야 한다.
- **왜 중요한가:** Plugin code generation, migration script처럼 dynamic definition이 필요할 수 있지만, 생성된 name이 어느 namespace에 남고 누가 lifecycle을 관리하는지 명확히 한다.
- **예시:** ns = {} / exec("x = 40\ny = …

```python
ns = {}
exec("x = 40\ny = x + 2", ns)
print(ns["y"])
```

 


---

## CHAPTER 04 · builtins를 제한하는 것은 공격 표면 축소일 뿐 완전한 격리가 아니다

### 시작 전 용어집

#### 1. builtins

- **뜻:** `{"__builtins__": {}}`를 넘기면 직접적인 built-in name access를 줄일 수 있다.
- **왜 중요한가:** 하지만 이미 전달한 object의 method나 class hierarchy를 통해 다른 capability에 도달할 수 있다.
- **예시:** `{"__builtins__": {}}`를 넘기면 직접적인 built-in name access를 줄일 …

#### 2. class

- **뜻:** 즉 namespace restriction은 **capability exposure를 줄이는 설계**이지 hostile Python code를 안전하게 실행하는 sandbox가 아니다.
- **왜 중요한가:** 같은 process와 interpreter 안에서 공격자가 임의 Python semantics를 사용할 수 있다면 격리는 매우 어렵다.
- **예시:** 즉 namespace restriction은 **capability exposure를 줄이는 설계**이지 hostile …

#### 3. process

- **뜻:** 실제 untrusted code execution은 OS process, container, syscall/resource restriction 같은 별도 isolation layer를 검토해야 한다.
- **왜 중요한가:** Language-level dictionary 하나로 security boundary를 대체하지 않는다.
- **예시:** 실제 untrusted code execution은 OS process, container, syscall/resource …

---

## CHAPTER 05 · dynamic execution과 closure·local namespace는 예상보다 복잡한 경계를 만든다

### 시작 전 용어집

#### 1. dynamic execution

- **뜻:** Function 내부의 local variable과 `exec`/`locals()`를 결합하면 source-level assignment와 동일하게 local binding이 갱신될 것이라고 생각하기 쉽다.
- **왜 중요한가:** 하지만 optimized local storage와 namespace mapping의 관계는 단순 dictionary mutation 모델과 다를 수 있다.
- **예시:** Function 내부의 local variable과 `exec`/`locals()`를 결합하면 source-level assignment와 …

#### 2. closure

- **뜻:** Dynamic code가 enclosing lexical cell을 직접 공유하는지, global mapping만 보는지, 별도 namespace를 받는지에 따라 behavior가 달라진다.
- **왜 중요한가:** 이름이 “보인다”와 “그 binding을 정상 assignment처럼 수정할 수 있다”를 구분한다.
- **예시:** Dynamic code가 enclosing lexical cell을 직접 공유하는지, global …

#### 3. local namespace

- **뜻:** 가능하면 dynamic code에 필요한 값을 explicit mapping으로 전달하고 결과도 mapping이나 return-like protocol로 회수한다.
- **왜 중요한가:** 숨은 local mutation에 의존하지 않는다.
- **예시:** 가능하면 dynamic code에 필요한 값을 explicit mapping으로 전달하고 …

---

## CHAPTER 06 · untrusted input을 Python code로 해석하는 순간 injection boundary가 열린다

### 시작 전 용어집

#### 1. untrusted input

- **뜻:** 사용자가 입력한 계산식, filter, template를 그대로 `eval`하면 데이터가 프로그램 권한을 얻는다.
- **왜 중요한가:** “숫자와 연산자만 입력할 것”이라는 UI 기대는 security control이 아니다.
- **예시:** 사용자가 입력한 계산식, filter, template를 그대로 `eval`하면 데이터가 …

#### 2. Python code

- **뜻:** 허용 문법이 제한적이라면 tokenize/AST validation 후 allowlist evaluator를 만들거나 별도 expression language를 사용한다.
- **왜 중요한가:** File/network/process capability가 필요 없는 rule engine이라면 Python 전체 runtime을 주는 것은 과도하다.
- **예시:** 허용 문법이 제한적이라면 tokenize/AST validation 후 allowlist evaluator를 …

#### 3. injection boundary

- **뜻:** I/O를 막아도 매우 큰 연산이나 deep recursion으로 CPU/memory를 소진할 수 있다.
- **왜 중요한가:** 보안은 syntax 차단만이 아니라 resource budget까지 포함한다.
- **예시:** I/O를 막아도 매우 큰 연산이나 deep recursion으로 CPU/memory를 …

입력 길이와 계산 복잡도도 제한해야 한다.  

---

## CHAPTER 07 · dynamic execution은 audit와 provenance를 남겨야 한다

### 시작 전 용어집

#### 1. dynamic execution

- **뜻:** 운영 환경에서 dynamic code를 실행한다면 누가 어떤 source revision을 실행했는지 추적 가능해야 한다.
- **왜 중요한가:** 전체 source를 무조건 로그에 남기면 secret이 포함될 수 있으므로 hash, rule ID, version, trusted origin을 기록한다.
- **예시:** 운영 환경에서 dynamic code를 실행한다면 누가 어떤 source …

#### 2. audit

- **뜻:** Compile error와 runtime error를 분리하고 synthetic filename에 rule ID를 넣으면 traceback에서 provenance를 찾기 쉽다.
- **왜 중요한가:** 정책 변경이 잦은 system에서는 실행 시점의 rule version을 event와 함께 저장해야 나중에 같은 입력을 재현할 수 있다.
- **예시:** Compile error와 runtime error를 분리하고 synthetic filename에 rule …

#### 3. provenance

- **뜻:** Dynamic behavior는 source provenance가 없으면 재현성이 크게 떨어진다.
- **예시:** Dynamic behavior는 source provenance가 없으면 재현성이 크게 떨어진다.

---

## CHAPTER 08 · dynamic execution contract는 syntax·namespace·capability·resource를 모두 제한한다

### 시작 전 용어집

#### 1. dynamic execution

- **뜻:** 이 PART의 핵심은 **dynamic execution을 편리한 문자열 평가 함수로 보지 않고, code generation과 security boundary를 동시에 여는 실행 인터페이스로 취급하는 것**이다.
- **왜 중요한가:** `eval`/`exec` 사용 여부는 코드 길이의 문제가 아니라 trust model의 문제다.
- **예시:** 이 PART의 핵심은 **dynamic execution을 편리한 문자열 평가 …

#### 2. contract

- **뜻:** 안전한 설계는 어떤 syntax를 허용하는지, 어떤 object/capability를 namespace에 노출하는지, 실행 시간·memory를 어떻게 제한하는지, 오류와 audit 정보를 어떻게 남기는지 정한다.
- **왜 중요한가:** 테스트에서는 정상 expression뿐 아니라 forbidden name, huge expression, syntax error, runtime exception, namespace mutation, 재현 가능한 rule version을 확인한다.
- **예시:** 안전한 설계는 어떤 syntax를 허용하는지, 어떤 object/capability를 namespace에 …

---

## 실전 학습 루프 · dynamic execution boundary

### 1. 쉬운 예

문자열로 받은 식을 `eval`하거나 코드를 `exec`하면 데이터였던 입력이 실행 권한을 갖게 된다. 외부 입력이 섞이면 단순 parser 문제가 아니라 코드 실행 경계가 된다.

### 2. 한 줄 해석

dynamic execution은 문자열을 편하게 처리하는 기능이 아니라 신뢰 수준을 바꾸는 보안 경계다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상한다. 그 다음 아래 최소 예제를 실행하고, 예상이 틀렸다면 **호출 순서와 상태 변화**를 표시한다.

```python
user_text = '2 + 3'
# 신뢰되지 않은 입력에는 eval 대신 제한된 parser를 설계한다.
print(user_text)
```

### 4. 수정 실습

1. 허용 연산이 덧셈뿐이라면 AST를 검사하는 작은 evaluator로 바꾼다.
2. globals/locals를 비웠다고 임의 코드 실행이 완전히 안전해지는지 반례를 찾는다.

수정 후에는 정상 예제만 다시 보지 말고 실패·경계·반복 호출 중 하나를 추가해 계약이 유지되는지 확인한다.

### 5. 확인 문제

입력 형식을 제한할 수 있는데도 `eval`을 쓰는 것이 좋은 기본값일까?

### 6. 정답과 오답 설명

**정답:** 아니다. 필요한 문법만 parser로 허용하는 설계가 공격면과 해석 차이를 줄인다.

**자주 나오는 오답:** “builtins만 지우면 sandbox다”는 답은 위험하다. 동적 실행을 안전한 격리로 만드는 것은 훨씬 더 큰 문제다.

이 PART를 마칠 때는 해당 문법 이름을 외우는 데서 멈추지 말고 **언제 호출되는가 / 무엇을 읽거나 바꾸는가 / 실패하면 어디로 가는가** 세 문장으로 설명한다.

## 현장 디버깅 체크 · dynamic execution boundary

### 증상에서 시작한다

사용자가 준 문자열 하나 때문에 파일 접근·import·명령 실행 같은 예상 밖 동작이 가능해진다. 이때 문법을 먼저 고치면 원인이 가려질 수 있다. 재현 입력과 실제 상태를 보존한 뒤 **어느 경계에서 처음 기대와 달라졌는지**를 찾는다.

### 먼저 볼 증거

실제 허용 문법 목록, 입력 provenance, 실행 namespace, OS 권한을 따로 확인한다. 최종 출력 하나만 보지 말고 호출 전 값, 호출 뒤 값, 예외 또는 resource 상태를 나란히 두면 원인 후보가 급격히 줄어든다.

### 일부러 실패시켜 보기

허용 입력을 산술식으로 제한하고 AST node allowlist 밖의 Call/Attribute를 넣어 거부되는지 시험한다. 정상 예제만 통과시키는 것은 검증이 아니다. 경계 조건을 강제로 만들고 같은 증상이 반복되는지 확인해야 수정 전후를 비교할 수 있다.

### 통과 기준

허용하지 않은 syntax와 side effect는 실행 단계 전에 거부되고, 데이터 입력이 코드 권한으로 승격되지 않아야 한다. 이 기준을 테스트 이름과 assertion으로 옮기면 이후 refactoring에서도 같은 오류가 돌아오는지 자동으로 잡을 수 있다.

