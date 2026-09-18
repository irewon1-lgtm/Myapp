# PART 09 · Type hint와 interface contract — 동적 실행 위에 정적 검증층을 세우기

Python은 실행 시 객체가 타입을 가지는 동적 타입 언어다. Type hint는 이 실행 모델을 다른 언어로 바꾸지 않지만 함수·객체·collection 사이의 기대 관계를 소스에 표현하고, 정적 분석기가 실행 전에 많은 불일치를 찾게 한다. 타입 표기는 장식이 아니라 **가능한 값의 집합과 호출 관계를 좁히는 설계 언어**로 사용할 때 가치가 커진다.

---

## CHAPTER 01 · runtime type과 static type hint는 서로 다른 층이다

### 시작 전 용어집

#### 1. runtime type

- **뜻:** Python 객체는 실행 중 실제 타입을 가지며 operation은 그 객체의 runtime behavior에 따라 수행된다.
- **왜 중요한가:** Annotation을 붙였다고 해서 Python interpreter가 일반적으로 모든 함수 호출에서 자동 type validation을 수행하는 것은 아니다.
- **예시:** `x: int = "hello"` 같은 코드도 정적 검사 …

#### 2. static type

- **뜻:** `x: int = "hello"` 같은 코드도 정적 검사 없이 실행 경로에 따라 존재할 수 있다.
- **왜 중요한가:** 따라서 annotation과 runtime validation을 같은 것으로 이해하면 경계 검증을 빠뜨릴 수 있다.
- **예시:** `x: int = "hello"` 같은 코드도 정적 검사 …

#### 3. type hint

- **뜻:** 정적 타입 도구는 소스를 실행하기 전에 선언된 관계를 분석한다.
- **왜 중요한가:** 함수가 `int`를 받는다고 했는데 명백히 `str`을 전달하면 오류 후보를 제시할 수 있다.
- **예시:** 정적 타입 도구는 소스를 실행하기 전에 선언된 관계를 …

#### 4. 객체

- **뜻:** 하지만 network JSON, 사용자 입력, database row처럼 외부에서 들어온 데이터가 실제 계약을 만족하는지는 runtime에서 검증해야 한다.
- **왜 중요한가:** Type checker가 외부 세계를 직접 보증해 주지는 않는다.
- **예시:** 하지만 network JSON, 사용자 입력, database row처럼 외부에서 …

이 두 층을 결합하면 강한 경계를 만들 수 있다. 외부 입력을 parser와 validator로 domain type으로 바꾸고, 내부 함수는 이미 검증된 type만 받도록 설계한다. 그러면 정적 분석이 내부 데이터 흐름을 넓게 보호하고 runtime validation은 신뢰 경계에 집중된다.

Annotation을 넣는 목적도 명확히 해야 한다. 모든 지역 변수에 타입을 적어 코드량을 늘리는 것이 아니라 public function, complex data shape, callback, generic collection처럼 사람이 쉽게 오해할 수 있는 경계를 우선한다. 타입 정보가 실제 설계와 어긋나면 잘못된 문서가 되므로 변경 시 함께 유지해야 한다.

---

## CHAPTER 02 · `Optional`은 “선택적 parameter”와 “None 가능한 값”을 구분해야 한다

### 시작 전 용어집

#### 1. Optional

- **뜻:** Optional field가 많은 object는 가능한 상태 조합을 폭발시킬 수 있다.
- **왜 중요한가:** `paid_at`, `shipped_at`, `cancelled_at`이 모두 optional인데 status와의 관계를 별도 invariant로 관리해야 한다면 상태별 타입이나 명시적 state object가 더 강한 모델이 될 수 있다.
- **예시:** def find_user(user_id: int) -> User | None: / …

#### 2. parameter

- **뜻:** 이것은 호출할 때 parameter를 생략할 수 있다는 뜻과 별개다.
- **왜 중요한가:** Default value가 있어야 argument 생략이 가능하고, default가 None이더라도 domain에서 None의 의미가 무엇인지 설계해야 한다.
- **예시:** def find_user(user_id: int) -> User | None: / …

#### 3. None

- **뜻:** 타입에서 `T | None`은 값이 `T`일 수도 있고 `None`일 수도 있음을 표현한다.
- **왜 중요한가:** 이 signature는 사용자를 찾지 못할 수 있음을 반환 타입에 드러낸다.
- **예시:** def find_user(user_id: int) -> User | None: / …

#### 4. 타입

- **뜻:** 정적 분석기는 `if user is None:` 같은 narrowing 이후 branch에서 타입을 더 구체적으로 추론할 수 있다.
- **왜 중요한가:** 하지만 모든 실패를 `None`으로 나타내면 오류 원인을 잃는다.
- **예시:** def find_user(user_id: int) -> User | None: / …

```python
def find_user(user_id: int) -> User | None:
    ...
```

 호출자는 `None`을 처리한 뒤에야 User operation을 안전하게 사용할 수 있다. 

 “존재하지 않음”이 정상적인 조회 결과라면 적합할 수 있지만 DB 연결 실패까지 None으로 바꾸면 호출자는 실제 장애를 데이터 부재로 오해한다. 타입은 실패 semantics를 압축하므로 반환 형태를 선택할 때 정보 손실을 본다.

 

---

## CHAPTER 03 · Union은 가능한 값의 집합을 넓히므로 narrowing 전략이 필요하다

### 시작 전 용어집

#### 1. union

- **뜻:** `str | int` 같은 union type은 값이 여러 형태 중 하나일 수 있음을 표현한다.
- **왜 중요한가:** 편리하게 보이지만 호출자가 매번 어느 variant인지 분기해야 한다.
- **예시:** 예를 들어 결제 결과를 `PaymentSuccess | PaymentDeclined | …

#### 2. narrowing

- **뜻:** Narrowing은 runtime 조건을 통해 더 구체적인 타입을 확정하는 과정이다.
- **왜 중요한가:** `isinstance`, `is None`, discriminant field, type guard 등이 사용될 수 있다.
- **예시:** 예를 들어 결제 결과를 `PaymentSuccess | PaymentDeclined | …

#### 3. 함수

- **뜻:** Union이 커질수록 함수 내부의 상태 공간도 커진다.
- **왜 중요한가:** “아무거나 받기 위해” union을 계속 추가하면 타입 힌트가 실제로는 계약을 약하게 만들 수 있다.
- **예시:** 예를 들어 결제 결과를 `PaymentSuccess | PaymentDeclined | …

#### 4. 상태

- **뜻:** 예를 들어 결제 결과를 `PaymentSuccess | PaymentDeclined | PaymentPending`처럼 명시하면 각 상태가 필요한 필드를 독립적으로 가질 수 있다.
- **왜 중요한가:** 호출자는 type check나 pattern matching을 통해 상태별 처리를 분리한다.
- **예시:** 예를 들어 결제 결과를 `PaymentSuccess | PaymentDeclined | …

Union이 도메인 variant를 정확히 표현한다면 강력하다.  

  조건이 실제 runtime semantics와 일치해야 정적 분석과 실행 결과가 같은 모델을 공유한다.

Union을 사용할 때는 공통 interface가 있는지 먼저 본다. 여러 타입이 모두 `.read()`를 제공하고 호출자가 read만 필요하다면 union보다 protocol 하나가 더 적합할 수 있다. Union은 “여러 구체 variant 중 하나”를, protocol은 “이 capability를 제공하는 무엇이든”을 표현하는 차이가 있다.

---

## CHAPTER 04 · collection type은 element contract까지 표현해야 한다

### 시작 전 용어집

#### 1. collection

- **뜻:** 하지만 지나치게 구체적인 mutable collection 타입은 API 결합을 높일 수 있다.
- **왜 중요한가:** 함수가 단지 반복만 한다면 `list[T]`를 요구할 이유가 없고 iterable capability만 요구할 수 있다.
- **예시:** 하지만 지나치게 구체적인 mutable collection 타입은 API 결합을 …

#### 2. element contract

- **뜻:** `list`라고만 적으면 container 종류는 알 수 있지만 안에 어떤 값이 들어가는지 정보가 부족하다.
- **왜 중요한가:** `list[Order]`, `dict[UserId, User]`, `set[str]`처럼 element와 key/value 타입을 함께 표현하면 data flow를 더 정확하게 추적할 수 있다.
- **예시:** `list`라고만 적으면 container 종류는 알 수 있지만 안에 …

#### 3. list

- **뜻:** `Sequence[T]`나 concrete list 반환은 더 강한 보장을 준다.
- **왜 중요한가:** 추상 타입을 쓰는 것이 항상 좋은 것이 아니라 필요한 semantics를 정확히 표현해야 한다.
- **예시:** `Sequence[T]`나 concrete list 반환은 더 강한 보장을 준다.

#### 4. dict

- **뜻:** Nested structure에서는 이 정보가 특히 중요하다.
- **왜 중요한가:** 반대로 index 접근과 길이가 필요하다면 sequence 성질이 필요하다.
- **예시:** Nested structure에서는 이 정보가 특히 중요하다.

Parameter type은 구현이 실제로 요구하는 최소 capability를 표현하는 편이 재사용성이 좋다.

반환 타입에서는 호출자에게 어떤 성질을 보장할지 생각한다. `Iterable[T]`를 반환하면 lazy single-pass iterator일 가능성까지 포함하므로 두 번 순회해야 하는 호출자에게 충분하지 않을 수 있다.  

Mutable collection의 variance 문제처럼 정적 타입 시스템에는 직관과 다른 제한도 있다. `list[Dog]`를 `list[Animal]`이 필요한 곳에 무조건 넘길 수 있다면 호출자가 Cat을 append해 원래 list의 element invariant를 깨뜨릴 수 있다. 타입 제약은 이런 mutation 가능성까지 반영한다.

---

## CHAPTER 05 · generic은 여러 타입에서 같은 구조적 관계를 보존한다

### 시작 전 용어집

#### 1. generic

- **뜻:** Generic type parameter는 “어떤 타입이든 가능”이라고만 말하는 것이 아니라 입력과 출력 사이의 **동일 타입 관계**를 보존한다.
- **왜 중요한가:** 여기서 `T`는 호출할 때 정해지고 반환 타입도 같은 T와 연결된다.
- **예시:** def first_or_none(items: Sequence[T]) -> T | None: / …

#### 2. 타입

- **뜻:** 함수가 입력값을 그대로 포장해 반환한다면 구체 타입마다 함수를 복제할 필요가 없다.
- **왜 중요한가:** `Sequence[User]`를 넣으면 `User | None`, `Sequence[int]`를 넣으면 `int | None`이라는 관계를 type checker가 추적할 수 있다.
- **예시:** def first_or_none(items: Sequence[T]) -> T | None: / …

#### 3. 함수

- **뜻:** 두 개의 concrete 함수가 더 명확한 작은 코드에서 generic hierarchy를 만드는 것은 과도할 수 있다.
- **왜 중요한가:** 반복되는 구조적 관계가 실제로 존재하고 여러 호출자가 이점을 얻을 때 도입한다.
- **예시:** def first_or_none(items: Sequence[T]) -> T | None: / …

#### 4. 입력

- **뜻:** Generic container를 직접 만들 때는 read와 write operation이 타입 안전성에 어떤 제약을 주는지 고려한다.
- **왜 중요한가:** Type parameter bound나 protocol constraint를 사용해 특정 capability를 요구할 수 있다.
- **예시:** def first_or_none(items: Sequence[T]) -> T | None: / …

```python
def first_or_none(items: Sequence[T]) -> T | None:
    ...
```

  단순 `Any`를 사용하면 이 정보가 사라진다.

  예를 들어 비교 가능한 값만 받는 구조라면 그 operation을 타입 수준에 표현할 수 있다.

Generic abstraction이 너무 복잡해지면 실제 업무 규칙보다 타입 기교를 이해하는 비용이 커질 수 있다.  

---

## CHAPTER 06 · Protocol은 상속 없이 필요한 behavior shape를 정의한다

### 시작 전 용어집

#### 1. protocol

- **뜻:** Consumer가 객체의 concrete class보다 특정 method 집합만 필요하다면 Protocol로 structural interface를 표현할 수 있다.
- **왜 중요한가:** 예를 들어 `Clock` consumer가 `now()`만 필요하다면 system clock, fake clock, fixed clock이 서로 상속 관계가 없어도 같은 protocol을 만족할 수 있다.
- **예시:** class Clock(Protocol): / def now(self) -> datetime: ...

#### 2. behavior shape

- **뜻:** 이 방식은 core logic이 특정 infrastructure class에 직접 의존하는 범위를 줄인다.
- **왜 중요한가:** Test에서는 deterministic fake를 넣고 production에서는 실제 구현을 주입할 수 있다.
- **예시:** class Clock(Protocol): / def now(self) -> datetime: ...

#### 3. 객체

- **뜻:** Protocol이 작을수록 구현자가 제공해야 할 behavior가 줄고 consumer dependency도 선명해진다.
- **왜 중요한가:** Structural compatibility는 method signature가 맞는지를 표현하지만 모든 semantic contract를 증명하지는 않는다.
- **예시:** class Clock(Protocol): / def now(self) -> datetime: ...

#### 4. class

- **뜻:** Protocol을 모든 class 앞에 자동으로 추가하면 abstraction이 늘어나기만 한다.
- **왜 중요한가:** 구현이 하나뿐이고 교체 가능성이나 독립 test boundary가 필요하지 않다면 concrete dependency가 더 명확할 수 있다.
- **예시:** class Clock(Protocol): / def now(self) -> datetime: ...

```python
class Clock(Protocol):
    def now(self) -> datetime: ...
```

  

 `now()`가 실제로 단조 증가해야 하는지, timezone-aware datetime을 반환해야 하는지 같은 의미는 별도 contract와 test가 필요하다. 정적 타입은 API shape의 강력한 일부이지 프로그램 correctness 전체를 대신하지 않는다.

  실제 변화 축이 생기는 경계에서 protocol을 도입한다.

---

## CHAPTER 07 · `Any`는 유연한 타입이 아니라 정적 검사의 탈출구다

### 시작 전 용어집

#### 1. Any

- **뜻:** `Any`는 대부분의 operation을 허용해 type checker가 검사를 계속할 수 있게 하지만 그 지점에서 타입 정보가 크게 약해진다.
- **왜 중요한가:** 외부 dynamic library와의 경계나 점진적 typing 도입 과정에서 필요할 수 있지만, 편의를 위해 넓게 퍼뜨리면 annotation을 작성한 효과가 사라진다.
- **예시:** `Any`는 대부분의 operation을 허용해 type checker가 검사를 계속할 …

#### 2. 타입

- **뜻:** 어느 field가 존재하는지, 어떤 타입인지, 누락 가능성이 있는지 모든 호출자가 다시 추측해야 한다.
- **왜 중요한가:** 외부 경계에서 schema를 검증하고 typed domain object로 바꾸면 Any가 신뢰 경계 안쪽으로 퍼지는 것을 막을 수 있다.
- **예시:** 어느 field가 존재하는지, 어떤 타입인지, 누락 가능성이 있는지 …

#### 3. 경계

- **뜻:** JSON을 `dict[str, Any]`로 파싱한 뒤 application 전체에 그대로 전달하는 패턴을 생각해 보자.
- **왜 중요한가:** `object`는 어떤 Python 객체든 받을 수 있지만 안전하게 사용할 수 있는 operation은 제한된다.
- **예시:** JSON을 `dict[str, Any]`로 파싱한 뒤 application 전체에 그대로 …

#### 4. typing

- **뜻:** `Any`는 checker가 많은 검사를 포기한다.
- **왜 중요한가:** “아무 값이나 받는다”는 요구에서도 어떤 형태가 더 안전한지 구분한다.
- **예시:** `Any`는 checker가 많은 검사를 포기한다.

`object`와 `Any`도 의미가 다르다.  구체 type으로 narrowing해야 한다.  

Type checker 오류를 해결하기 위해 무조건 cast나 ignore를 추가하는 습관도 같은 문제를 만든다. 실제로 checker가 코드의 모순을 발견한 것인지, library stub이 부족한 것인지 원인을 먼저 본다. Escape hatch는 필요한 곳에 국소적으로 사용하고 이유를 남긴다.

---

## CHAPTER 08 · Callable type은 callback의 입력과 결과를 계약으로 만든다

### 시작 전 용어집

#### 1. callable

- **뜻:** 단순 callable signature가 길어지거나 여러 method가 필요한 behavior라면 Protocol을 사용하는 편이 읽기 쉬울 수 있다.
- **왜 중요한가:** Callback이 async인지 sync인지, exception을 던질 수 있는지, 몇 번 호출되는지도 실제 계약에서 중요하다.
- **예시:** 단순 callable signature가 길어지거나 여러 method가 필요한 behavior라면 …

#### 2. callback

- **뜻:** 정렬 key, validator, event handler, retry predicate처럼 callback이 받는 parameter와 반환 타입이 명확하면 호출자가 잘못된 함수를 연결하는 오류를 일찍 찾을 수 있다.
- **왜 중요한가:** Type signature는 그중 shape를 표현한다.
- **예시:** 정렬 key, validator, event handler, retry predicate처럼 callback이 …

#### 3. 입력

- **뜻:** 함수를 값으로 전달할 때도 타입 관계를 표현할 수 있다.
- **왜 중요한가:** Callback이 mutable state를 closure로 캡처하면 타입은 맞아도 실행 순서에 따라 behavior가 달라질 수 있다.
- **예시:** 함수를 값으로 전달할 때도 타입 관계를 표현할 수 …

#### 4. 계약

- **뜻:** 정적 타입이 side effect나 thread safety를 자동으로 보증하지 않는다.
- **왜 중요한가:** 따라서 callback contract에는 호출 횟수, reentrancy, lifetime 같은 semantics가 필요할 수 있다.
- **예시:** 정적 타입이 side effect나 thread safety를 자동으로 보증하지 …

Event system에서는 callback 반환값을 무시하는지, false가 propagation을 중단하는지 같은 규칙도 분명해야 한다. Callable typing을 도입하는 목적은 단순 autocomplete가 아니라 producer와 consumer가 같은 호출 규약을 공유하게 만드는 것이다.

---

## CHAPTER 09 · 타입 오류를 줄이려면 domain type이 primitive보다 강할 때가 있다

### 시작 전 용어집

#### 1. 타입

- **뜻:** 모든 ID를 무거운 class로 만들 필요는 없지만 금액, 단위, 식별자처럼 혼동 비용이 큰 값은 의미를 타입에 담을 가치가 있다.
- **왜 중요한가:** 초와 millisecond를 모두 float로 쓰면 `timeout_seconds=5000` 같은 실수가 실행 가능하다.
- **예시:** 모든 ID를 무거운 class로 만들 필요는 없지만 금액, …

#### 2. 오류

- **뜻:** `user_id`, `product_id`, `quantity`, `price`가 모두 int라면 type checker는 서로 잘못 전달해도 구분하지 못할 수 있다.
- **왜 중요한가:** Domain 의미가 다른 값을 별도 type으로 표현하면 같은 primitive representation을 사용해도 잘못된 조합을 일찍 잡을 수 있다.
- **예시:** `user_id`, `product_id`, `quantity`, `price`가 모두 int라면 type checker는 …

#### 3. domain type

- **뜻:** Duration object나 의미 있는 parameter name, domain type을 사용하면 표현 공간을 좁힌다.
- **왜 중요한가:** Currency가 다른 Money 덧셈도 같은 문제다.
- **예시:** Duration object나 의미 있는 parameter name, domain type을 …

#### 4. primitive

- **뜻:** Value object class, dataclass, `NewType` 같은 여러 선택지가 있으며 각각 runtime behavior와 비용이 다르다.
- **왜 중요한가:** 강한 domain type은 parsing/validation 경계와 결합할 때 효과가 크다.
- **예시:** Value object class, dataclass, `NewType` 같은 여러 선택지가 …

특히 단위 오류는 타입으로 줄일 수 있다.   

 외부 raw string에서 `UserId`를 만드는 constructor가 validation을 책임지고, 내부 코드는 이미 유효한 ID만 받는다. 반복 validation이 줄고 함수 계약이 선명해진다.

---

## CHAPTER 10 · 타입 검사는 테스트와 경쟁하지 않고 서로 다른 오류 공간을 담당한다

### 시작 전 용어집

#### 1. 타입

- **뜻:** 정적 타입 검사는 실행하지 않고도 잘못된 인자 타입, 누락된 return, 불가능한 attribute access 같은 구조적 오류를 넓게 찾을 수 있다.
- **왜 중요한가:** 테스트는 실제 behavior, 경계값, 외부 시스템 통합, 업무 규칙을 실행해 확인한다.
- **예시:** 함수 signature가 `calculate_total(lines: Sequence[Line]) -> Money`라고 적혀 있어도 …

#### 2. 오류

- **뜻:** Public boundary와 새 코드에서 시작해 Any 영역을 줄이고, 실제 오류가 자주 나는 데이터 흐름을 우선하는 점진적 전략이 현실적이다.
- **왜 중요한가:** Checker 설정도 너무 느슨하면 의미가 없고 지나치게 엄격하면 도입 비용이 커지므로 팀의 위험 수준에 맞춰 강화한다.
- **예시:** 함수 signature가 `calculate_total(lines: Sequence[Line]) -> Money`라고 적혀 있어도 …

#### 3. 경계

- **뜻:** 둘은 대체 관계가 아니라 서로 다른 failure class를 잡는다.
- **왜 중요한가:** 함수 signature가 `calculate_total(lines: Sequence[Line]) -> Money`라고 적혀 있어도 할인 규칙이 10%가 아니라 20%로 잘못 구현된 것은 type checker가 잡지 못한다.
- **예시:** 둘은 대체 관계가 아니라 서로 다른 failure class를 …

#### 4. class

- **뜻:** 반대로 정상 test 세 개가 통과해도 호출 경로 중 하나가 `None`을 넘길 가능성을 정적 분석이 찾아낼 수 있다.
- **왜 중요한가:** 타입 도입은 한 번에 전체 codebase를 완벽하게 만드는 프로젝트가 아닐 수 있다.
- **예시:** 반대로 정상 test 세 개가 통과해도 호출 경로 …

검증층을 겹치면 오류 공간을 더 넓게 줄인다.

  

최종적으로 type hint의 가치는 annotation 수가 아니라 **프로그램에서 허용되는 상태와 호출 관계를 얼마나 정확하게 설명하고 실제 변경 시 그 계약 위반을 얼마나 빨리 발견하는가**로 평가한다.
