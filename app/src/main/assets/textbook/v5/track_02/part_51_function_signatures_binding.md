# PART 51 · Function signature와 argument binding — 호출 가능 범위를 API 계약으로 설계하기

함수의 parameter 목록은 값을 받는 문법이 아니라 호출자가 어떤 형태로 의도를 표현할 수 있는지를 제한하는 public contract다. Positional-only, positional-or-keyword, keyword-only, default, variadic parameter를 어떻게 배치하느냐에 따라 같은 기능도 호환성·가독성·오류 탐지력이 달라진다. Signature를 설계한다는 것은 **어떤 호출을 허용하고 어떤 잘못된 호출을 interpreter 단계에서 즉시 거부할지 결정하는 일**이다.

---

## CHAPTER 01 · Python parameter는 위치에 따라 서로 다른 binding 규칙을 가진다

### 시작 전 용어집

#### 1. Python parameter

- **뜻:** 함수 호출이 시작되면 argument expression을 평가한 뒤 각 결과를 parameter에 binding한다.
- **왜 중요한가:** 모든 parameter가 같은 방식으로 값을 받는 것은 아니다.
- **예시:** 함수 호출이 시작되면 argument expression을 평가한 뒤 각 …

#### 2. binding

- **뜻:** 호출 오류의 상당수는 함수 body에 들어가기 전에 이 binding 단계에서 발생한다.
- **왜 중요한가:** `라는 형태에서 `a`는 위치로만, `b`는 위치 또는 keyword로, `c`는 keyword로만 전달된다.
- **예시:** 호출 오류의 상당수는 함수 body에 들어가기 전에 이 …

#### 3. 함수

- **뜻:** Required argument 누락, 같은 parameter에 positional과 keyword가 중복 전달됨, 허용되지 않은 keyword가 들어온 경우 interpreter가 함수 body 실행 전에 거부한다.
- **왜 중요한가:** 가능한 오류를 이 층에서 잡게 만들수록 내부 validation이 단순해진다.
- **예시:** Required argument 누락, 같은 parameter에 positional과 keyword가 중복 …

#### 4. 오류

- **뜻:** Positional-only, positional-or-keyword, var-positional, keyword-only, var-keyword라는 서로 다른 종류가 있으며 signature가 이 순서를 제한한다.
- **왜 중요한가:** 같은 세 값을 받더라도 호출자가 사용할 수 있는 표현 공간이 다르다.
- **예시:** Positional-only, positional-or-keyword, var-positional, keyword-only, var-keyword라는 서로 다른 종류가 …

`def f(a, /, b, *, c): ...  이 규칙을 이해하면 library가 왜 일부 parameter 이름 변경을 자유롭게 할 수 있고 어떤 이름은 public API가 되는지 설명할 수 있다.

Binding은 단순 편의 문법이 아니라 validation의 첫 층이다.  

---

## CHAPTER 02 · positional-only parameter는 이름을 public contract에서 숨길 수 있다

### 시작 전 용어집

#### 1. positional-only parameter

- **뜻:** Positional-only parameter를 사용하면 caller가 `name=value` 형태로 그 이름에 의존할 수 없기 때문에 내부 parameter rename이 public breaking change가 되지 않는다.
- **왜 중요한가:** Built-in function이 positional-only를 사용하는 경우를 이해하면 documentation의 `/` 표기를 읽을 수 있다.
- **예시:** Positional-only parameter를 사용하면 caller가 `name=value` 형태로 그 이름에 …

#### 2. public contract

- **뜻:** 일부 argument는 이름보다 순서가 본질적이고 implementation이 parameter 이름을 바꿀 자유를 유지하고 싶을 수 있다.
- **왜 중요한가:** Mathematical function의 핵심 operand처럼 이름을 붙여 호출해도 가독성이 크게 늘지 않는 값은 positional-only 후보가 될 수 있다.
- **예시:** 일부 argument는 이름보다 순서가 본질적이고 implementation이 parameter 이름을 …

#### 3. value

- **뜻:** 반대로 boolean option이나 서로 같은 타입의 여러 값은 keyword가 의미를 더 잘 드러낼 수 있다.
- **왜 중요한가:** Positional-only를 과도하게 사용하면 호출 site에서 `f(10, 30, 1, False)`처럼 각 값의 의미를 기억해야 한다.
- **예시:** 반대로 boolean option이나 서로 같은 타입의 여러 값은 …

#### 4. boolean

- **뜻:** 따라서 “이름을 숨길 수 있다”는 기능과 “이름을 숨기는 것이 좋은 API다”는 판단을 구분한다.
- **예시:** 따라서 “이름을 숨길 수 있다”는 기능과 “이름을 숨기는 …

변경 자유도와 caller 가독성을 함께 본다.

---

## CHAPTER 03 · keyword-only parameter는 option 의미를 호출 위치에 드러낸다

### 시작 전 용어집

#### 1. keyword-only parameter

- **뜻:** 여러 optional setting이 모두 정수·boolean처럼 비슷한 타입이라면 positional 호출은 순서를 잘못 바꿔도 실행될 수 있다.
- **왜 중요한가:** `connect(host, 30, 5, True)`보다 `connect(host, timeout=30, retries=5, verify=True)`가 policy를 명확하게 표현한다.
- **예시:** 여러 optional setting이 모두 정수·boolean처럼 비슷한 타입이라면 positional …

#### 2. option

- **뜻:** 기존 positional parameter 뒤에 새 positional option을 끼워 넣으면 오래된 호출과 의미 충돌이 생길 수 있지만 keyword-only option은 명시적 이름으로 추가하기 쉽다.
- **왜 중요한가:** 다만 default behavior 변경은 여전히 compatibility 문제다.
- **예시:** 기존 positional parameter 뒤에 새 positional option을 끼워 …

#### 3. set

- **뜻:** Keyword-only로 강제하면 caller가 이름을 생략한 모호한 호출을 만들 수 없다.
- **왜 중요한가:** 특히 boolean flag는 positional argument로 읽기 어렵다.
- **예시:** Keyword-only로 강제하면 caller가 이름을 생략한 모호한 호출을 만들 …

#### 4. boolean

- **뜻:** `open_report(path, True, False)`에서 어떤 옵션이 켜졌는지 source를 찾아야 하지만 keyword-only는 호출 자체가 설명이 된다.
- **왜 중요한가:** Parameter 이름이 public API가 되므로 나중에 rename할 때 compatibility를 고려해야 한다.
- **예시:** `open_report(path, True, False)`에서 어떤 옵션이 켜졌는지 source를 찾아야 …

Keyword-only는 API evolution에도 유용하다.  

---

## CHAPTER 04 · `*args`와 `**kwargs`는 forwarding에 강하지만 계약을 약하게 만들 수 있다

### 시작 전 용어집

#### 1. args

- **뜻:** `*args`, `**kwargs`를 사용하면 caller의 positional/keyword 집합을 받아 그대로 forwarding할 수 있다.
- **왜 중요한가:** Decorator가 원래 function signature를 보존하거나 proxy가 downstream API를 연결할 때 유용하다.
- **예시:** `*args`, `**kwargs`를 사용하면 caller의 positional/keyword 집합을 받아 그대로 …

#### 2. forwarding

- **뜻:** Variadic parameter는 open-ended forwarding이 실제 requirement일 때 사용하고 stable business input은 명시적으로 적는다.
- **왜 중요한가:** Forwarding wrapper는 자신이 소비하는 option과 downstream으로 보내는 option을 분리해야 한다.
- **예시:** Variadic parameter는 open-ended forwarding이 실제 requirement일 때 사용하고 …

#### 3. 계약

- **뜻:** Wrapper와 adapter는 자신이 모든 argument를 해석하지 않고 다른 callable로 전달해야 할 수 있다.
- **왜 중요한가:** 하지만 application domain 함수가 모든 입력을 `**kwargs` 하나로 받으면 required field와 허용 이름을 signature에서 알 수 없고 오타가 body 깊숙한 곳에서 발견된다.
- **예시:** Wrapper와 adapter는 자신이 모든 argument를 해석하지 않고 다른 …

#### 4. callable

- **뜻:** Type checker와 IDE도 정확한 지원을 하기 어렵다.
- **왜 중요한가:** 같은 `timeout` 이름을 wrapper와 target이 서로 다른 의미로 사용하면 silent conflict가 생길 수 있다.
- **예시:** Type checker와 IDE도 정확한 지원을 하기 어렵다.

Reserved prefix나 explicit wrapper configuration object를 사용해 namespace를 나눌 수 있다.

---

## CHAPTER 05 · signature binding API는 호출 전에 argument 구조를 검사하고 정규화할 수 있다

### 시작 전 용어집

#### 1. signature binding

- **뜻:** Introspection을 사용하면 callable의 signature를 읽고 주어진 argument 집합을 실제 호출 규칙에 따라 binding해 볼 수 있다.
- **왜 중요한가:** Dependency injection, CLI generator, RPC adapter 같은 framework는 이를 이용해 parameter name과 annotation을 분석할 수 있다.
- **예시:** Introspection을 사용하면 callable의 signature를 읽고 주어진 argument 집합을 …

#### 2. API

- **뜻:** 단순 dictionary merge보다 interpreter의 binding semantics를 재사용하므로 실제 호출과 더 일관된 오류를 만들 수 있다.
- **왜 중요한가:** Bound arguments를 mapping 형태로 얻으면 positional과 keyword로 서로 다르게 들어온 호출을 하나의 normalized representation으로 바꿀 수 있다.
- **예시:** 단순 dictionary merge보다 interpreter의 binding semantics를 재사용하므로 실제 …

#### 3. argument

- **뜻:** Logging이나 memoization key를 만들 때 유용할 수 있지만 secret parameter를 무조건 기록하지 않는다.
- **왜 중요한가:** Signature introspection에 runtime behavior를 너무 많이 의존하면 decorator가 metadata를 보존하지 않을 때 framework가 깨질 수 있다.
- **예시:** Logging이나 memoization key를 만들 때 유용할 수 있지만 …

#### 4. callable

- **뜻:** Wrapper는 원본 signature와 `__wrapped__` 관계를 보존하고 framework는 unsupported callable 형태를 명시적으로 거부해야 한다.
- **예시:** Wrapper는 원본 signature와 `__wrapped__` 관계를 보존하고 framework는 unsupported …

---

## CHAPTER 06 · sentinel default는 “생략됨”과 실제 `None`을 구분한다

### 시작 전 용어집

#### 1. sentinel

- **뜻:** 이럴 때 module-private unique object를 sentinel로 만들어 default에 사용하면 caller가 명시적으로 `None`을 전달한 것과 argument를 아예 생략한 것을 다른 branch로 처리할 수 있다.
- **왜 중요한가:** Patch API, configuration overlay, dataclass field처럼 absence semantics가 중요한 곳에서 이 차이가 핵심이다.
- **예시:** _MISSING = object() / def update(value=_MISSING):

#### 2. None

- **뜻:** `None`이 domain에서 유효한 값인데 동시에 “argument가 제공되지 않음”의 default로 사용하면 두 상태를 구분할 수 없다.
- **왜 중요한가:** Sentinel은 equality보다 identity로 검사한다.
- **예시:** _MISSING = object() / def update(value=_MISSING):

#### 3. 상태

- **뜻:** Caller가 같은 값을 만들어낼 수 없는 고유 object라는 특성을 이용하는 것이다.
- **왜 중요한가:** Public API에서 sentinel object 자체를 노출할지 private implementation으로 둘지도 결정한다.
- **예시:** _MISSING = object() / def update(value=_MISSING):

#### 4. module

- **뜻:** Caller가 “default behavior 요청”을 명시적으로 전달해야 한다면 enum이나 public marker type이 더 적합할 수 있다.
- **예시:** _MISSING = object() / def update(value=_MISSING):

```python
_MISSING = object()

def update(value=_MISSING):
    if value is _MISSING:
        ...  # 변경하지 않음
    elif value is None:
        ...  # 값을 명시적으로 제거
```

  

 

---

## CHAPTER 07 · signature 변경은 source compatibility와 behavior compatibility를 따로 본다

### 시작 전 용어집

#### 1. signature

- **뜻:** Signature compatibility와 behavior compatibility를 분리한다.
- **왜 중요한가:** Required parameter를 추가하면 기존 caller가 즉시 binding error를 내므로 명확한 breaking change다.
- **예시:** Signature compatibility와 behavior compatibility를 분리한다.

#### 2. source compatibility

- **뜻:** 새 optional keyword를 추가하는 것은 기존 호출을 깨지 않을 가능성이 높지만 같은 default로도 내부 behavior가 달라지면 semantic breaking change가 될 수 있다.
- **왜 중요한가:** 반대로 positional-only parameter의 내부 이름 변경은 source-level caller에 영향이 없을 수 있다.
- **예시:** 새 optional keyword를 추가하는 것은 기존 호출을 깨지 …

#### 3. behavior compatibility

- **뜻:** 기존 parameter를 keyword-only로 바꾸는 것도 positional caller를 깨뜨린다.
- **왜 중요한가:** Deprecation 기간 동안 두 형태를 모두 받고 warning을 낸 뒤 다음 major version에서 제한을 강화할 수 있다.
- **예시:** 기존 parameter를 keyword-only로 바꾸는 것도 positional caller를 깨뜨린다.

#### 4. parameter

- **뜻:** Public library에서는 representative old call forms를 contract test로 남기면 accidental signature break를 빠르게 발견한다.
- **왜 중요한가:** API surface snapshot이나 type checking fixture도 활용할 수 있다.
- **예시:** Public library에서는 representative old call forms를 contract test로 …

---

## CHAPTER 08 · decorator는 runtime behavior뿐 아니라 signature contract도 보존해야 한다

### 시작 전 용어집

#### 1. decorator

- **뜻:** Decorator wrapper를 `def wrapper(*args, **kwargs)`로만 만들면 실제 호출은 forwarding되더라도 introspection에서 원래 parameter 정보가 사라질 수 있다.
- **왜 중요한가:** wraps` 같은 도구는 metadata와 wrapped relation을 유지해 documentation, debugger, dependency injection tool이 원본 interface를 추적하게 한다.
- **예시:** Decorator wrapper를 `def wrapper(*args, **kwargs)`로만 만들면 실제 호출은 …

#### 2. runtime behavior

- **뜻:** Parameter를 실제로 추가·제거하는 decorator라면 원본 signature를 그대로 보여 주는 것도 거짓이 될 수 있다.
- **왜 중요한가:** Authentication decorator가 `user`를 내부에서 주입하는 경우 외부 caller에게 어떤 signature를 보여야 하는지 framework contract에 맞춰야 한다.
- **예시:** Parameter를 실제로 추가·제거하는 decorator라면 원본 signature를 그대로 보여 …

#### 3. signature contract

- **뜻:** Metadata 보존과 interface transformation을 구분한다.
- **왜 중요한가:** Typed decorator에서는 `ParamSpec` 같은 typing mechanism을 사용해 원본 callable의 parameter shape를 보존할 수 있다.
- **예시:** Metadata 보존과 interface transformation을 구분한다.

#### 4. parameter

- **뜻:** Runtime wrapper와 static signature가 같은 계약을 표현하도록 맞춘다.
- **예시:** Runtime wrapper와 static signature가 같은 계약을 표현하도록 맞춘다.

`functools.

  

 

---

## CHAPTER 09 · signature test는 허용 호출뿐 아니라 거부 호출을 검증한다

### 시작 전 용어집

#### 1. signature test

- **뜻:** 함수 test가 정상 positional 호출 하나만 사용하면 keyword-only 제약이나 duplicate argument 오류를 보호하지 못한다.
- **왜 중요한가:** Public API라면 지원해야 하는 호출 형태와 금지해야 하는 형태를 table로 만들 수 있다.
- **예시:** 함수 test가 정상 positional 호출 하나만 사용하면 keyword-only …

#### 2. 검증

- **뜻:** Required omission, unexpected keyword, positional overflow, duplicate binding을 실제 호출해 expected `TypeError`가 나는지 확인한다.
- **왜 중요한가:** Backward compatibility가 중요한 library에서는 이전 release에서 유효했던 sample call을 fixture로 보존한다.
- **예시:** Required omission, unexpected keyword, positional overflow, duplicate binding을 …

#### 3. 함수

- **뜻:** Decorator를 적용한 함수는 `inspect.
- **왜 중요한가:** signature` 결과와 실제 호출 behavior가 일치하는지 검사한다.
- **예시:** Decorator를 적용한 함수는 `inspect.

#### 4. 오류

- **뜻:** Signature refactor 후 모두 계속 binding되는지 확인하면 문서에 없는 caller도 일부 보호할 수 있다.
- **왜 중요한가:** Framework가 introspection을 사용한다면 metadata correctness도 기능 test의 일부다.
- **예시:** Signature refactor 후 모두 계속 binding되는지 확인하면 문서에 …

---

## CHAPTER 10 · 좋은 call contract는 정보량과 변경 자유도를 균형 있게 배치한다

### 시작 전 용어집

#### 1. call contract

- **뜻:** Parameter가 적다는 이유만으로 좋은 API가 되지 않는다.
- **왜 중요한가:** 서로 다른 의미의 값을 positional로 몰아 넣으면 호출은 짧지만 이해하기 어렵고, 모든 option을 keyword-only로 만들면 명확하지만 자주 쓰는 핵심 operand까지 장황해질 수 있다.
- **예시:** Parameter가 적다는 이유만으로 좋은 API가 되지 않는다.

#### 2. parameter

- **뜻:** Function signature는 type hint, default, parameter kind, naming을 함께 사용해 호출 가능한 상태 공간을 제한한다.
- **왜 중요한가:** 이 층에서 표현할 수 없는 cross-field invariant만 body validation로 넘긴다.
- **예시:** Function signature는 type hint, default, parameter kind, naming을 …

#### 3. API

- **뜻:** 자주 사용되는 필수 입력과 정책 option을 구분해 signature에 구조를 반영한다.
- **왜 중요한가:** Interpreter가 자동으로 잡을 수 있는 호출 오류를 manual dict parsing으로 다시 만들 이유가 없다.
- **예시:** 자주 사용되는 필수 입력과 정책 option을 구분해 signature에 …

#### 4. 입력

- **뜻:** Signature 설계의 핵심은 **값을 함수 안으로 넣는 방법을 정하는 것이 아니라 caller가 올바른 의도를 가장 명확하게 표현하고 잘못된 조합은 가능한 한 호출 경계에서 즉시 실패하도록 만드는 것**이다.
- **예시:** Signature 설계의 핵심은 **값을 함수 안으로 넣는 방법을 …
