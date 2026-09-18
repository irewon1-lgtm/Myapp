# PART 68 · Callable object protocol — function·bound method·`__call__`을 같은 호출 모델로 읽기

Python에서 괄호 호출 문법 `target(...)`은 일반 함수에만 쓰이지 않는다. 함수, bound method, class, `__call__`을 구현한 instance가 모두 callable이 될 수 있다. 호출 가능성 자체와 argument binding, 저장된 state, introspection 가능성을 분리해서 보면 decorator·callback·strategy object를 더 정확하게 설계할 수 있다.

---

## CHAPTER 01 · callable object는 함수가 아니어도 호출 문법에 참여할 수 있다

### 시작 전 용어집

#### 1. callable

- **뜻:** 다만 `callable(x)`가 True라는 사실은 어떤 argument를 받는지, side effect가 있는지, idempotent한지 알려주지 않는다.
- **왜 중요한가:** “호출 가능함”은 가장 얇은 structural property일 뿐이다.
- **예시:** class Scale: / def __init__(self, factor):

#### 2. 함수

- **뜻:** 객체가 `__call__`을 구현하면 instance 자체를 함수처럼 사용할 수 있다.
- **왜 중요한가:** 이 패턴은 configuration과 behavior를 하나의 객체로 묶을 때 유용하다.
- **예시:** class Scale: / def __init__(self, factor):

#### 3. 객체

- **뜻:** Closure와 비슷하지만 state를 attribute로 명시적으로 노출하고 여러 method를 추가하기 쉽다.
- **예시:** class Scale: / def __init__(self, factor):

```python
class Scale:
    def __init__(self, factor):
        self.factor = factor

    def __call__(self, value):
        return value * self.factor

triple = Scale(3)
print(triple(10))
```

 

 

---

## CHAPTER 02 · function binding은 class attribute의 function을 method access로 바꾼다

### 시작 전 용어집

#### 1. function binding

- **뜻:** Class body에 정의한 function을 instance를 통해 읽으면 일반적으로 bound method가 만들어져 instance가 첫 인수에 연결된다.
- **왜 중요한가:** 전자는 `self`가 이미 binding된 method object이고 후자는 function 자체다.
- **예시:** class Greeter: / def hello(self, name):

#### 2. class

- **뜻:** 이 차이는 descriptor protocol과 연결된다.
- **왜 중요한가:** Callback registry에 bound method를 저장하면 method가 instance를 강하게 참조해 lifetime을 늘릴 수 있다는 점도 중요하다.
- **예시:** class Greeter: / def hello(self, name):

#### 3. method access

- **뜻:** 단순 function pointer가 아니라 object relation을 저장하는 셈이다.
- **예시:** class Greeter: / def hello(self, name):

```python
class Greeter:
    def hello(self, name):
        return f"hello {name}"

g = Greeter()
method = g.hello
print(method("Lee"))
```

`g.hello`와 `Greeter.hello`는 같은 방식으로 호출하지 않는다.  

 

---

## CHAPTER 03 · bound method의 `self`는 호출 순간에 임의로 다시 선택되는 값이 아니다

### 시작 전 용어집

#### 1. bound method

- **뜻:** run`으로 얻은 bound method는 특정 `obj`와 function의 조합을 가진다.
- **왜 중요한가:** 나중에 `method()`를 호출해도 다른 object가 자동으로 self가 되지 않는다.
- **예시:** run`으로 얻은 bound method는 특정 `obj`와 function의 조합을 …

#### 2. self

- **뜻:** 이 특성은 event handler나 retry callback을 저장할 때 유용하지만, 오래 보관하면 stale object에 계속 연결될 수 있다.
- **왜 중요한가:** UI screen이나 session object의 bound method가 global registry에 남으면 해당 object graph가 해제되지 않을 수 있다.
- **예시:** 이 특성은 event handler나 retry callback을 저장할 때 …

`method = obj. 

 

Method를 전달할 때는 receiver lifetime을 의도한 것인지 확인한다. 단지 operation 종류만 전달하려는 목적이라면 static function이나 명시적 ID를 전달하는 편이 ownership을 줄일 수 있다.

---

## CHAPTER 04 · argument binding은 function body 실행 전에 호출 계약을 검증한다

### 시작 전 용어집

#### 1. argument binding

- **뜻:** Python call은 positional argument, keyword argument, positional-only, keyword-only, default, `*args`, `**kwargs` 규칙에 따라 parameter와 값을 연결한다.
- **왜 중요한가:** 잘못된 호출은 body에 들어가기 전에 binding 단계에서 실패한다.
- **예시:** def connect(host, /, port=443, *, timeout=5): / ...

#### 2. function body

- **뜻:** 이 signature는 `host`를 위치 인수로, `timeout`을 keyword-only로 제한한다.
- **왜 중요한가:** 이런 제약은 스타일이 아니라 public API evolution과 오사용 방지 장치다.
- **예시:** def connect(host, /, port=443, *, timeout=5): / ...

#### 3. 계약

- **뜻:** Wrapper가 `*args, **kwargs`만 받으면 원래 callable의 contract가 호출자와 도구에 덜 보일 수 있다.
- **왜 중요한가:** 가능하면 signature를 보존하거나 명시적 adapter API를 만든다.
- **예시:** def connect(host, /, port=443, *, timeout=5): / ...

```python
def connect(host, /, port=443, *, timeout=5):
    ...
```

 

 

---

## CHAPTER 05 · positional과 keyword 경계는 API의 변경 가능성을 결정한다

### 시작 전 용어집

#### 1. positional

- **뜻:** 반대로 positional-only parameter는 이름을 내부 구현 세부로 유지할 수 있다.
- **왜 중요한가:** Keyword-only parameter는 boolean flag나 선택적 policy처럼 호출 의미를 명확히 하는 데 유용하다.
- **예시:** fetch(url, timeout=5, verify=True)

#### 2. keyword

- **뜻:** Parameter name을 keyword로 허용하면 그 이름이 public API 일부가 된다.
- **왜 중요한가:** 나중에 내부 변수 이름을 바꾸는 것처럼 쉽게 변경하기 어렵다.
- **예시:** fetch(url, timeout=5, verify=True)

#### 3. 경계

- **뜻:** `fetch(url, 5, True)`보다 intent가 잘 보인다.
- **왜 중요한가:** 특히 같은 primitive type 인수가 연속될 때 keyword-only는 순서 오류를 줄인다.
- **예시:** fetch(url, timeout=5, verify=True)

#### 4. API

- **뜻:** Signature 설계는 오늘 호출이 되는가보다 향후 호환성, readability, wrapper tooling까지 고려한다.
- **예시:** fetch(url, timeout=5, verify=True)

```python
fetch(url, timeout=5, verify=True)
```

 


---

## CHAPTER 06 · signature introspection은 callable을 자동화 도구와 연결한다

### 시작 전 용어집

#### 1. signature introspection

- **뜻:** Framework, dependency injection, CLI generator, test utility는 callable signature를 읽어 parameter 구조를 이해할 수 있다.
- **왜 중요한가:** 하지만 모든 callable이 같은 수준의 introspection metadata를 제공하는 것은 아니다.
- **예시:** Framework, dependency injection, CLI generator, test utility는 callable …

#### 2. callable

- **뜻:** wraps` 같은 도구는 wrapper가 원본 callable 정보를 보존하는 데 도움을 준다.
- **왜 중요한가:** Custom callable object도 자신의 호출 API가 안정적이라면 `__call__` signature를 명확히 정의한다.
- **예시:** wraps` 같은 도구는 wrapper가 원본 callable 정보를 보존하는 …

#### 3. frame

- **뜻:** Decorator가 원래 function metadata를 잃으면 이름, 문서, signature 기반 도구가 잘못 동작할 수 있다.
- **왜 중요한가:** Dynamic dispatch를 이유로 모든 것을 `*args, **kwargs`에 숨기면 자동화 가능성과 정적 이해가 떨어진다.
- **예시:** Decorator가 원래 function metadata를 잃으면 이름, 문서, signature …

#### 4. dependency

- **뜻:** Introspection은 구현 편의가 아니라 ecosystem compatibility의 일부다.
- **예시:** Introspection은 구현 편의가 아니라 ecosystem compatibility의 일부다.

`functools.

 


---

## CHAPTER 07 · callable state는 closure와 object field 중 어떤 표현이 더 명확한지 선택한다

### 시작 전 용어집

#### 1. callable

- **뜻:** Closure도 같은 기능을 만들 수 있지만 callable object는 state inspection, reset method, serialization policy를 더 명시적으로 표현하기 쉽다.
- **왜 중요한가:** 반대로 단일 immutable config와 작은 behavior라면 closure가 더 간결할 수 있다.
- **예시:** class RetryCounter: / def __init__(self, limit):

#### 2. closure

- **뜻:** Retry policy처럼 호출 간 state를 유지해야 할 수 있다.
- **왜 중요한가:** 어떤 표현을 쓰든 shared mutable state면 concurrency 문제가 생길 수 있다.
- **예시:** class RetryCounter: / def __init__(self, limit):

#### 3. object field

- **뜻:** Callable이라고 해서 stateless하거나 thread-safe한 것은 아니다.
- **예시:** class RetryCounter: / def __init__(self, limit):

```python
class RetryCounter:
    def __init__(self, limit):
        self.limit = limit
        self.used = 0

    def __call__(self):
        if self.used >= self.limit:
            return False
        self.used += 1
        return True
```

 

 

---

## CHAPTER 08 · call contract는 callable 여부보다 binding·state·lifetime을 함께 정의한다

### 시작 전 용어집

#### 1. call contract

- **뜻:** Public API에서 callable을 받거나 반환한다면 어떤 signature를 기대하는지, 호출 횟수와 순서가 정해져 있는지, exception을 어떻게 다루는지, callable이 state를 보존하는지 문서화해야 한다.
- **왜 중요한가:** Test에서는 정상 인수뿐 아니라 positional/keyword 오류, wrapper metadata, bound method lifetime, stateful callable의 반복 호출을 확인한다.
- **예시:** Public API에서 callable을 받거나 반환한다면 어떤 signature를 기대하는지, …

#### 2. callable

- **뜻:** 이 PART의 핵심은 **`()`를 함수 호출 기호로만 보지 않고, function·bound method·callable object가 argument binding과 hidden state를 통해 참여하는 하나의 호출 프로토콜로 읽는 것**이다.
- **왜 중요한가:** Callback이 later execution되는 경우에는 capture된 receiver가 유효한지도 본다.
- **예시:** 이 PART의 핵심은 **`()`를 함수 호출 기호로만 보지 …
