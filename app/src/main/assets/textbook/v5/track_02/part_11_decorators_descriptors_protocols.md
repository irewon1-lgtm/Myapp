# PART 11 · Decorator·descriptor·호출 protocol — Python이 동작을 합성하는 방식

Python의 함수와 객체를 깊게 읽기 시작하면 `@decorator`, property, bound method, callable object처럼 겉보기에는 서로 다른 기능이 같은 원리로 연결된다는 사실을 만나게 된다. 핵심은 이름에 객체를 바인딩하고, attribute 접근과 함수 호출이 protocol을 통해 다른 객체의 동작으로 위임된다는 점이다. 이 층을 이해하면 framework가 만드는 “마법 같은 코드”를 문법 암기가 아니라 실행 순서로 추적할 수 있다.

---

## CHAPTER 01 · decorator는 정의된 객체를 다른 객체로 변환해 다시 바인딩한다

### 시작 전 용어집

#### 1. decorator

- **뜻:** 함수 decorator를 가장 단순하게 풀면 `@decorate` 아래의 함수 정의가 끝난 뒤 `name = decorate(name)`과 같은 형태의 변환이 일어난다고 볼 수 있다.
- **왜 중요한가:** 따라서 decorator는 함수 body 안에서 실행되는 특별 문법이 아니라 **정의 결과 객체를 받아 다른 callable 또는 wrapper로 바꾸는 구성 단계**다.
- **예시:** 함수 decorator를 가장 단순하게 풀면 `@decorate` 아래의 함수 …

#### 2. 객체

- **뜻:** 이 관점이면 여러 decorator가 겹칠 때 적용 순서를 실제 함수 호출 순서로 풀어낼 수 있다.
- **왜 중요한가:** Decorator가 원래 함수의 전후에 로깅, 권한 검사, retry, cache 같은 공통 behavior를 붙일 수 있는 이유도 wrapper가 같은 호출 계약을 유지하면서 추가 동작을 수행하기 때문이다.
- **예시:** 이 관점이면 여러 decorator가 겹칠 때 적용 순서를 …

#### 3. 함수

- **뜻:** “decorated function도 원래 함수처럼 사용할 수 있는가”가 가장 중요한 계약이다.
- **왜 중요한가:** Wrapper를 만들면 `__name__`, docstring, annotation처럼 원래 callable을 설명하던 정보가 wrapper 정보로 바뀔 수 있다.
- **예시:** “decorated function도 원래 함수처럼 사용할 수 있는가”가 가장 …

#### 4. callable

- **뜻:** 문제는 wrapper가 원래 parameter나 return semantics를 무시하거나 모든 exception을 삼킬 때 발생한다.
- **왜 중요한가:** wraps` 계열 도구는 introspection과 debugging에 필요한 metadata를 보존하도록 돕는다.
- **예시:** 문제는 wrapper가 원래 parameter나 return semantics를 무시하거나 모든 …

Metadata도 고려해야 한다.  `functools. Framework가 endpoint 이름이나 signature를 reflection하는 경우 metadata 손실은 단순 표시 문제가 아니라 실제 등록 오류가 될 수 있다.

Decorator를 붙였다는 사실이 호출 비용과 side effect를 숨기지 않도록 한다. 단순 함수처럼 보이지만 매 호출마다 network authentication이나 disk cache를 수행한다면 사용자는 비용을 알아야 한다. Decorator는 cross-cutting concern을 모으는 도구이지 중요한 실행 의미를 보이지 않게 감추는 면허가 아니다.

---

## CHAPTER 02 · parameterized decorator는 두 단계 이상의 호출을 가진다

### 시작 전 용어집

#### 1. parameter

- **뜻:** Parameterized decorator를 여러 개 겹치면 바깥 decorator가 안쪽 decorator의 결과를 감싼다.
- **왜 중요한가:** 권한 검사와 retry, transaction을 어떤 순서로 배치하느냐에 따라 권한 검사를 매 retry마다 수행할지, transaction 전체를 한 번 감쌀지 semantics가 달라진다.
- **예시:** configuration stage: retry(max_attempts=3) -> decorator / binding stage: …

#### 2. decorator

- **뜻:** `@retry(max_attempts=3)`처럼 argument를 받는 decorator는 겉보기보다 호출 층이 하나 더 있다.
- **왜 중요한가:** 먼저 `retry(max_attempts=3)`가 실제 decorator function을 만들고, 그 결과가 정의된 함수에 적용된다.
- **예시:** configuration stage: retry(max_attempts=3) -> decorator / binding stage: …

#### 3. 함수

- **뜻:** 이 단계를 구분하지 않으면 어느 시점에 configuration이 계산되고 어느 시점에 실제 업무 함수가 실행되는지 혼동하기 쉽다.
- **왜 중요한가:** Configuration stage에서 mutable object를 capture하면 여러 호출이 같은 상태를 공유할 수 있다.
- **예시:** configuration stage: retry(max_attempts=3) -> decorator / binding stage: …

#### 4. configuration

- **뜻:** Retry attempt count처럼 호출별로 독립적이어야 하는 값은 wrapper 호출 안에서 생성해야 하고, 누적 통계처럼 의도적으로 공유할 값만 closure state에 둔다.
- **왜 중요한가:** Closure lifetime과 decorator lifetime이 만나는 지점이다.
- **예시:** configuration stage: retry(max_attempts=3) -> decorator / binding stage: …

실행 시에는 다시 wrapper가 호출된다. 

개념적으로는 다음 흐름이다.

```text
configuration stage: retry(max_attempts=3) -> decorator
binding stage:       decorator(original) -> wrapped
runtime stage:       wrapped(args...) -> original(args...)
```

  

  문법 순서를 스타일로 보지 말고 resource lifetime과 failure boundary 순서로 해석한다.

---

## CHAPTER 03 · method binding은 function과 instance를 결합해 bound method를 만든다

### 시작 전 용어집

#### 1. method binding

- **뜻:** Class body에 정의된 일반 function을 instance를 통해 접근하면 Python의 descriptor protocol에 의해 instance가 첫 argument에 연결된 bound method 형태를 얻을 수 있다.
- **왜 중요한가:** method(x)`가 내부적으로는 class에 저장된 function과 `obj` reference를 결합해 호출되는 구조를 가진다.
- **예시:** 이 객체는 instance를 reference하므로 예상보다 instance lifetime을 늘릴 …

#### 2. function

- **뜻:** 또한 function object가 class attribute일 때 왜 특별히 method처럼 동작하는지, instance attribute에 일반 callable을 저장했을 때는 왜 같은 방식으로 binding되지 않을 수 있는지를 이해하게 한다.
- **왜 중요한가:** Method를 다른 변수에 저장하면 이미 instance와 결합된 bound method를 보관할 수 있다.
- **예시:** 이 객체는 instance를 reference하므로 예상보다 instance lifetime을 늘릴 …

#### 3. instance

- **뜻:** method(obj, x)`처럼 호출했을 때와 instance를 통해 호출했을 때의 차이를 설명한다.
- **왜 중요한가:** 이 객체는 instance를 reference하므로 예상보다 instance lifetime을 늘릴 수도 있다.
- **예시:** method(obj, x)`처럼 호출했을 때와 instance를 통해 호출했을 때의 …

#### 4. bound method

- **뜻:** Callback registry에 bound method를 장기간 저장할 때 memory retention이나 unregister 정책을 고려해야 하는 이유다.
- **왜 중요한가:** Class method와 static method도 같은 맥락에서 본다.
- **예시:** Callback registry에 bound method를 장기간 저장할 때 memory …

그래서 `obj. `self`가 자동으로 생성되는 마법이라기보다 attribute access protocol의 결과다.

이 모델은 class에서 직접 `Class. 

  

 Class method는 class object를 첫 argument에 연결하는 protocol을 제공하고, static method는 자동 instance/class binding 없이 function을 namespace 안에 둔다. 어느 것을 쓸지는 “class 안에 넣고 싶다”가 아니라 호출에 어떤 context가 필요한지로 결정한다.

---

## CHAPTER 04 · descriptor는 attribute 읽기·쓰기·삭제를 객체가 가로채게 한다

### 시작 전 용어집

#### 1. descriptor

- **뜻:** Descriptor는 특정 protocol method를 제공하는 객체를 class attribute로 두어 다른 instance의 attribute 접근을 제어할 수 있게 한다.
- **왜 중요한가:** field`라는 단순 syntax 뒤에 계산, validation, lazy loading, method binding 같은 behavior가 연결될 수 있는 이유가 descriptor mechanism에 있다.
- **예시:** Descriptor는 특정 protocol method를 제공하는 객체를 class attribute로 …

#### 2. attribute

- **뜻:** Descriptor를 이해할 때는 attribute lookup precedence가 중요하다.
- **왜 중요한가:** Data descriptor인지, instance dictionary에 같은 이름이 있는지, class hierarchy에서 무엇을 찾는지에 따라 실제 반환되는 객체가 달라질 수 있다.
- **예시:** Descriptor를 이해할 때는 attribute lookup precedence가 중요하다.

#### 3. 객체

- **뜻:** Framework ORM field나 validation library가 class 선언을 읽어 runtime behavior를 만드는 방식도 이런 protocol을 활용할 수 있다.
- **왜 중요한가:** 강력한 만큼 남용하면 코드 표면과 실제 비용 사이의 거리가 커진다.
- **예시:** Framework ORM field나 validation library가 class 선언을 읽어 …

#### 4. protocol

- **뜻:** orders`를 읽는 순간 database query가 실행된다면 호출자는 일반 field 접근으로 생각하고 loop 안에서 수백 번 query를 만들 수 있다.
- **왜 중요한가:** Lazy loading descriptor는 편리하지만 query boundary와 caching policy가 관측 가능해야 한다.
- **예시:** orders`를 읽는 순간 database query가 실행된다면 호출자는 일반 …

Property가 대표적인 예다. `obj.

  

 `user. 

직접 descriptor를 구현해야 하는 경우는 많지 않다. Property와 dataclass, 기존 library가 대부분의 일반 요구를 처리한다. 하지만 framework code를 읽을 때 descriptor model을 알면 “attribute인데 왜 function이 실행되지?” 같은 현상을 구체적인 lookup 순서로 추적할 수 있다.

---

## CHAPTER 05 · `__getattribute__`와 `__getattr__`는 attribute 실패 경로까지 바꿀 수 있다

### 시작 전 용어집

#### 1. attribute

- **뜻:** 일반 attribute 접근은 객체의 기본 lookup machinery를 거친다.
- **왜 중요한가:** `__getattribute__`는 광범위한 접근을 가로챌 수 있고, `__getattr__`는 일반 lookup으로 이름을 찾지 못했을 때 fallback 동작을 제공하는 데 사용된다.
- **예시:** 예를 들어 어떤 이름이든 None을 반환하는 fallback은 `user.

#### 2. 실패

- **뜻:** Dynamic proxy나 lazy object가 이런 hook을 활용하지만, 내부 동작을 잘못 구현하면 단순 attribute read 하나가 복잡한 실패 경로를 만든다.
- **왜 중요한가:** `__getattr__`는 실제로 없는 attribute를 계산해 제공할 수 있지만 오타까지 정상값으로 받아들이면 오류를 숨길 수 있다.
- **예시:** 예를 들어 어떤 이름이든 None을 반환하는 fallback은 `user.

#### 3. 객체

- **뜻:** 둘을 같은 hook으로 생각하면 recursion과 debugging 문제가 생기기 쉽다.
- **왜 중요한가:** 모든 접근을 가로채는 `__getattribute__` 구현에서 다시 `self.
- **예시:** 예를 들어 어떤 이름이든 None을 반환하는 fallback은 `user.

#### 4. recursion

- **뜻:** some_name`을 읽으면 자신의 hook을 재호출해 무한 recursion이 생길 수 있다.
- **왜 중요한가:** Base implementation을 명시적으로 사용해야 하는 이유다.
- **예시:** 예를 들어 어떤 이름이든 None을 반환하는 fallback은 `user.

예를 들어 어떤 이름이든 None을 반환하는 fallback은 `user.nmae` 같은 오타를 즉시 실패시키지 않고 나중의 전혀 다른 위치에서 문제를 만든다. Dynamic behavior는 허용 이름 범위를 명확하게 제한하는 편이 좋다.

Introspection 도구와 serializer가 attribute를 어떻게 탐색하는지도 고려해야 한다. Dynamic lookup이 side effect를 가진다면 단순 debug 출력이나 자동 문서화 과정에서 예기치 않은 동작이 실행될 수 있다. Attribute protocol은 object API의 기본 경계이므로 예측 가능성이 중요하다.

---

## CHAPTER 06 · callable object는 상태를 가진 함수처럼 동작할 수 있다

### 시작 전 용어집

#### 1. callable

- **뜻:** Stateful validator, configurable strategy, counter, model inference wrapper처럼 behavior와 설정을 하나의 객체로 묶을 때 callable object가 적합할 수 있다.
- **왜 중요한가:** 이 객체는 callback을 요구하는 API에 전달할 수 있으면서 `minimum`이라는 configuration을 명시적으로 가진다.
- **예시:** class Threshold: / def __init__(self, minimum):

#### 2. 상태

- **뜻:** 함수와 다른 점은 instance field에 configuration과 상태를 명시적으로 보관할 수 있다는 것이다.
- **왜 중요한가:** Closure로도 같은 기능을 만들 수 있지만 object form은 repr, type, multiple methods, explicit state가 필요한 경우 더 읽기 쉬울 수 있다.
- **예시:** class Threshold: / def __init__(self, minimum):

#### 3. 함수

- **뜻:** 객체가 호출 protocol을 제공하면 `obj(.
- **왜 중요한가:** Stateful callable은 재사용 횟수에 따라 결과가 달라질 수 있으므로 thread safety와 lifecycle을 고려한다.
- **예시:** class Threshold: / def __init__(self, minimum):

#### 4. 객체

- **뜻:** Invocation count를 내부에 누적하거나 cache를 보유한다면 여러 요청이 같은 instance를 공유하는 구조에서 경쟁 조건이 생길 수 있다.
- **왜 중요한가:** Stateless callable과 stateful service를 구분한다.
- **예시:** class Threshold: / def __init__(self, minimum):

..)` 형태로 사용할 수 있다.  

```python
class Threshold:
    def __init__(self, minimum):
        self.minimum = minimum

    def __call__(self, value):
        return value >= self.minimum
```

 

  

Function, closure, callable object 중 선택은 문법 취향이 아니라 필요한 상태의 가시성, interface 크기, 테스트 방법으로 결정한다. Behavior를 전달한다는 공통점 아래 서로 다른 state ownership 모델을 가진다.

---

## CHAPTER 07 · context decorator와 context manager는 lifetime 합성의 두 표현이다

### 시작 전 용어집

#### 1. context decorator

- **뜻:** 어떤 동작이 함수 전체의 진입과 종료를 감싸야 한다면 decorator와 context manager가 모두 후보가 될 수 있다.
- **왜 중요한가:** Transaction, tracing span, lock, temporary configuration처럼 범위를 갖는 behavior는 **언제 시작하고 언제 반드시 끝나는가**가 핵심이다.
- **예시:** 어떤 동작이 함수 전체의 진입과 종료를 감싸야 한다면 …

#### 2. context manager

- **뜻:** Context manager를 좁은 구간에 두면 database resource lifetime을 줄일 수 있다.
- **왜 중요한가:** 반대로 모든 endpoint가 같은 tracing 규칙을 가져야 한다면 decorator가 반복을 줄이고 정책을 한 곳에 모을 수 있다.
- **예시:** Context manager를 좁은 구간에 두면 database resource lifetime을 …

#### 3. lifetime

- **뜻:** Cleanup이 보장된다는 사실만으로 deadlock이나 너무 긴 lifetime이 해결되는 것은 아니다.
- **왜 중요한가:** Lifetime abstraction을 평가할 때는 syntax보다 scope를 본다.
- **예시:** Cleanup이 보장된다는 사실만으로 deadlock이나 너무 긴 lifetime이 해결되는 …

#### 4. 함수

- **뜻:** Function 전체가 정확한 범위라면 decorator가 자연스럽고, 함수 내부 일부 구간만 감싸야 한다면 `with` block이 더 명시적이다.
- **왜 중요한가:** Decorator로 transaction을 붙였는데 함수 안에서 network 호출까지 포함되면 transaction이 불필요하게 오래 열린다.
- **예시:** Function 전체가 정확한 범위라면 decorator가 자연스럽고, 함수 내부 …

두 도구를 중첩할 때 종료 순서는 stack처럼 바깥 wrapper와 안쪽 context가 역순으로 정리되는 구조를 가진다. Lock, transaction, tracing을 어떤 순서로 획득·해제하는지 실제 failure path까지 추적해야 한다. 

 Resource가 필요 이상으로 넓은 범위에서 살아 있으면 성능과 contention이 악화되고, 너무 좁으면 필요한 atomicity를 잃을 수 있다.

---

## CHAPTER 08 · metaprogramming은 코드 생성보다 프로그램 정의 단계의 behavior를 바꾼다

### 시작 전 용어집

#### 1. metaprogramming

- **뜻:** Framework를 사용할 때는 metaprogramming의 내부 구현을 모두 암기할 필요는 없지만 언제 선언이 평가되고 어떤 object가 registry에 들어가는지 추적할 수 있어야 한다.
- **왜 중요한가:** 그러면 annotation 하나가 왜 startup error를 만들거나 import되지 않은 module의 handler가 등록되지 않는지 설명할 수 있다.
- **예시:** Framework를 사용할 때는 metaprogramming의 내부 구현을 모두 외울 …

#### 2. behavior

- **뜻:** Class를 import하는 순간 registry가 바뀌거나 plugin이 등록되면 import order와 environment가 behavior에 영향을 줄 수 있다.
- **왜 중요한가:** 정의 시점 side effect를 명시적으로 관리해야 한다.
- **예시:** Class를 import하는 순간 registry가 바뀌거나 plugin이 등록되면 import …

#### 3. decorator

- **뜻:** Decorator와 descriptor를 더 밀어 올리면 class creation hook, metaclass처럼 프로그램의 정의 단계 자체를 가로채는 기능을 만난다.
- **왜 중요한가:** ORM이 class body의 field 선언을 읽어 schema metadata를 만들고 framework가 decorator로 handler registry를 구성하는 식으로 runtime 전에 구조를 수집할 수 있다.
- **예시:** Decorator와 descriptor를 더 밀어 올리면 class creation hook, …

#### 4. descriptor

- **뜻:** 이런 기법은 반복되는 선언을 줄이고 domain-specific interface를 만들 수 있지만 control flow가 소스의 위아래 순서만으로 보이지 않게 한다.
- **왜 중요한가:** Metaclass는 대부분의 application code에서 첫 선택이 아니다.
- **예시:** 이런 기법은 반복되는 선언을 줄이고 domain-specific interface를 만들 …

Class decorator, `__init_subclass__`, factory function, explicit registry가 더 단순한 경우가 많다. 강력한 mechanism을 선택하기 전에 같은 계약을 더 작은 abstraction으로 표현할 수 있는지 확인한다.

 

---

## CHAPTER 09 · introspection은 객체의 구조를 읽지만 안정된 public API와 구분한다

### 시작 전 용어집

#### 1. introspection

- **뜻:** 하지만 내부 attribute나 구현 세부사항에 강하게 의존하는 introspection은 library upgrade에 취약하다.
- **왜 중요한가:** `hasattr`로 capability를 추측하는 것과 명시적 Protocol을 사용하는 것은 서로 다른 장단점을 가진다.
- **예시:** 하지만 내부 attribute나 구현 세부사항에 강하게 의존하는 introspection은 …

#### 2. 객체

- **뜻:** Python은 `type`, `isinstance`, `getattr`, `hasattr`, signature inspection 등 runtime 객체를 조사하는 도구를 제공한다.
- **왜 중요한가:** Test framework, serializer, dependency injection, CLI generator가 이런 정보를 활용해 반복 코드를 줄일 수 있다.
- **예시:** Python은 `type`, `isinstance`, `getattr`, `hasattr`, signature inspection 등 …

#### 3. public API

- **뜻:** Runtime plugin system에서는 동적 검사가 필요할 수 있고, application 내부에서는 static interface가 더 일찍 오류를 잡는다.
- **왜 중요한가:** Introspection은 타입 시스템을 대체하는 것이 아니라 실행 시 구조가 실제로 무엇인지 확인하는 한 도구다.
- **예시:** Runtime plugin system에서는 동적 검사가 필요할 수 있고, …

#### 4. frame

- **뜻:** Reflection 기반 serializer가 모든 field를 자동으로 내보내면 secret이나 내부 metadata까지 외부로 노출할 수 있다.
- **왜 중요한가:** 공개 schema는 명시적으로 선택하는 편이 안전하다.
- **예시:** Reflection 기반 serializer가 모든 field를 자동으로 내보내면 secret이나 …

Debug 표현과 wire representation, persistence representation을 같은 introspection 결과로 만들지 않는다.

도구가 객체 구조를 읽는다는 사실은 metadata compatibility도 API 일부가 될 수 있음을 뜻한다. Decorator가 signature를 보존해야 하고 dataclass field 이름 변경이 serializer와 migration에 영향을 주는 이유다.

---

## CHAPTER 10 · protocol 기반 설계는 “마법”을 호출 순서로 환원한다

### 시작 전 용어집

#### 1. protocol

- **뜻:** 하지만 많은 기능은 attribute lookup, iteration, context management, calling, numeric operation 같은 protocol의 조합이다.
- **왜 중요한가:** `for`, `with`, `obj()`, `obj.
- **예시:** Python의 고급 기능을 접하면 언어가 특별한 예외 규칙을 …

#### 2. attribute lookup

- **뜻:** Python의 고급 기능을 접하면 언어가 특별한 예외 규칙을 무한히 가진 것처럼 느껴질 수 있다.
- **왜 중요한가:** attr`, `len(obj)`를 각각 어떤 protocol이 연결하는지 알면 실행 경로를 작은 단계로 풀 수 있다.
- **예시:** Python의 고급 기능을 접하면 언어가 특별한 예외 규칙을 …

#### 3. decorator

- **뜻:** 디버깅할 때도 decorator가 몇 겹인지, descriptor가 access를 가로채는지, bound method가 어떤 instance를 보유하는지, lazy property가 언제 계산되는지 순서대로 본다.
- **왜 중요한가:** Framework 이름을 먼저 외우는 것보다 object model을 기준으로 추적하면 library가 바뀌어도 같은 사고법을 사용할 수 있다.
- **예시:** 디버깅할 때도 decorator가 몇 겹인지, descriptor가 access를 가로채는지, …

#### 4. descriptor

- **뜻:** Protocol을 직접 구현할 때는 기존 Python 사용자가 기대하는 의미를 존중한다.
- **왜 중요한가:** Iterator는 종료 규칙을 지키고, context manager는 cleanup 의미를 명확히 하며, equality와 hash는 일관성을 유지하고, callable은 이름과 signature가 약속하는 behavior를 제공한다.
- **예시:** Protocol을 직접 구현할 때는 기존 Python 사용자가 기대하는 …

언어에 자연스럽게 통합된다는 것은 문법이 짧아진다는 뜻보다 기존 계약을 따른다는 뜻이다.

이 층의 목적은 모든 dunder method를 외우는 것이 아니다. 코드 표면과 실제 객체 상호작용 사이를 연결해 **어디서 binding이 바뀌고, 어디서 behavior가 위임되며, 어떤 state가 보존되는지** 설명할 수 있는 실행 모델을 갖는 것이다.
