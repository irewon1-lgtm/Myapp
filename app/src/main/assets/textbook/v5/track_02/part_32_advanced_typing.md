# PART 32 · Advanced typing — TypedDict·Literal·overload·variance로 계약을 더 정밀하게 표현하기

기본 type hint가 `list[int]`, `User | None` 같은 값의 종류를 설명한다면 더 큰 코드에서는 dict의 field shape, literal 상태, callback signature, generic variance처럼 **타입 사이의 관계**를 표현해야 한다. 정적 타입은 runtime validation을 대신하지 않지만 가능한 호출과 상태를 더 좁혀 리팩터링 전에 불일치를 발견하게 한다. 복잡한 typing 문법의 목적은 기교가 아니라 실제 계약을 정확하게 표현하는 데 있다.

---

## CHAPTER 01 · TypedDict는 mapping의 field contract를 정적으로 표현한다

외부 JSON을 `dict[str, Any]`로 application 전체에 전달하면 어떤 key가 필수인지, 각 value가 어떤 타입인지 모든 호출자가 다시 추측해야 한다. TypedDict는 runtime에는 일반 mapping과 유사하게 다루면서 static checker에 field 이름과 value type을 알려 줄 수 있다.

Required와 optional field를 구분하면 partial update payload와 complete object를 서로 다른 shape로 표현할 수 있다. 그러나 key가 type에 있다고 실제 untrusted JSON에 반드시 존재한다는 뜻은 아니다. Parser/schema validation을 통과한 뒤 typed mapping으로 취급해야 한다.

TypedDict는 behavior를 소유하지 않는다. 상태 전이와 invariant가 복잡하면 dataclass나 domain object가 더 적합할 수 있다. Read-only DTO나 API payload처럼 mapping 형태 자체가 public contract일 때 강하다.

Field rename은 type checker가 넓은 사용처를 찾아 주는 장점이 있다. Raw dict 문자열 key를 여기저기 직접 쓰는 것보다 schema change의 영향 범위를 훨씬 명확하게 볼 수 있다.

---

## CHAPTER 02 · Literal은 값의 타입뿐 아니라 허용 값 집합을 좁힌다

`str`은 모든 문자열을 허용하지만 상태가 실제로 `"created"`, `"paid"`, `"cancelled"` 세 개뿐이라면 Literal union으로 가능한 값을 표현할 수 있다. Caller가 `"done"` 같은 임의 문자열을 전달하면 static error 후보가 된다.

Literal은 작은 closed set에 적합하지만 상태마다 다른 data와 behavior가 있으면 Enum이나 variant class가 더 강한 모델이다. 문자열 literal 수십 개를 union으로 늘리면 의미와 namespace가 약해진다.

Boolean flag도 `Literal[True]`와 overload를 결합해 argument에 따라 return type이 달라지는 API를 표현할 수 있다. 하지만 caller가 type system을 이해하기 위해 복잡한 signature를 해석해야 한다면 두 개의 명시적 함수로 나누는 편이 더 나을 수 있다.

외부 protocol에서 unknown future value가 올 수 있다면 closed Literal만 믿고 runtime에서 unreachable이라고 가정하지 않는다. Static closed-world와 wire open-world를 구분한다.

---

## CHAPTER 03 · overload는 argument 형태에 따라 달라지는 return 계약을 설명한다

하나의 함수가 input type에 따라 다른 return type을 보장하는 경우 overload signature를 여러 개 제공할 수 있다. Type checker는 caller의 argument를 보고 더 구체적인 결과 타입을 추론한다. 실제 runtime implementation은 일반적으로 하나이며 overload declaration과 behavior가 일치해야 한다.

Overload는 이미 존재하는 안정된 API semantics를 정적으로 설명할 때 유용하다. Type checker를 만족시키기 위해 input 조합마다 억지로 signature를 추가하면 runtime contract가 더 복잡해질 수 있다. API 자체를 단순화할 수 있는지 먼저 본다.

Overload branch ordering과 overlap도 문제다. 두 signature가 같은 input에 모두 맞는데 return type이 다르면 caller inference가 불안정할 수 있다. 가장 구체적인 case와 fallback을 설계하고 checker의 overlap warning을 확인한다.

Runtime dispatch를 overload declaration이 자동으로 수행하지 않는다. Implementation 내부에서 `isinstance`나 명시적 branch로 실제 behavior를 구현해야 한다.

---

## CHAPTER 04 · TypeVar는 독립된 Any가 아니라 호출 안의 타입 관계를 보존한다

Generic identity 함수가 `Any -> Any`라면 caller가 string을 넣어도 결과가 int일 가능성을 checker가 배제하지 못한다. `T -> T` 관계로 표현하면 input과 output이 같은 type이라는 정보를 보존할 수 있다. TypeVar의 핵심은 구체 type을 모른다는 사실보다 **여러 위치가 같은 type이어야 한다는 관계**다.

Bound를 사용하면 T가 특정 base capability를 가진 subtype이어야 한다고 제한할 수 있다. Constraint는 허용 가능한 몇 개의 타입 집합을 표현하는 다른 도구다. Bound와 constraint는 비슷해 보여도 결과 type inference가 다를 수 있으므로 목적에 맞게 선택한다.

Generic class에서는 instance마다 type parameter가 달라질 수 있다. `Box[int]`와 `Box[str]`가 같은 implementation을 공유하면서 value contract를 다르게 가진다.

Type parameter가 너무 많아 signature를 읽기 어렵다면 abstraction 경계가 과도하게 일반화됐을 수 있다. 실제 변하는 축만 generic으로 만든다.

---

## CHAPTER 05 · variance는 “하위 타입 container를 상위 타입 container로 볼 수 있는가”를 결정한다

`Dog`가 `Animal`의 subtype이라고 해서 `list[Dog]`를 항상 `list[Animal]`로 안전하게 취급할 수 있는 것은 아니다. 후자가 mutable이고 caller가 Cat을 append할 수 있다면 원래 `list[Dog]` invariant가 깨진다. 그래서 mutable generic은 직관과 다른 invariant 관계를 가질 수 있다.

Read-only producer는 covariance가 자연스러울 수 있다. `Sequence[Dog]`를 Animal을 읽기만 하는 함수에 전달해도 Dog는 모두 Animal이므로 안전하다. 반대로 consumer callback처럼 값을 받기만 하는 위치는 contravariance가 의미를 가질 수 있다.

Variance를 외우기보다 “이 interface가 T를 밖으로 내보내는가, 안으로 받는가, 둘 다 하는가”를 보면 방향을 이해하기 쉽다. Read와 write가 동시에 있으면 invariant가 필요한 경우가 많다.

API parameter에서 구체 `list[T]` 대신 필요한 capability만 가진 read-only abstraction을 사용하면 caller가 전달할 수 있는 타입 범위가 넓어진다.

---

## CHAPTER 06 · Protocol generic은 capability와 값 관계를 함께 표현한다

단순 Protocol은 method 존재를 설명하고 generic Protocol은 그 method가 주고받는 type 관계까지 표현할 수 있다. `Repository[T]`가 `get(id) -> T | None`, `save(T)`를 가진다면 User repository와 Order repository가 같은 구조적 contract를 공유할 수 있다.

하지만 generic repository가 모든 domain에 맞는다는 뜻은 아니다. Query semantics, transaction, uniqueness rule이 다르면 이름만 generic으로 통일해 중요한 contract를 잃을 수 있다. 공통 operation이 실제로 같은 의미를 가질 때만 추상화한다.

Protocol inheritance로 capability를 조합할 수도 있다. Read-only와 writable interface를 나누면 consumer가 필요 이상으로 강한 dependency를 요구하지 않는다. Query handler는 Reader만 알고 command handler가 Writer를 요구하는 식이다.

Static structural typing은 runtime object가 의미를 지키는지 증명하지 않는다. Contract test와 domain validation이 필요하다.

---

## CHAPTER 07 · ParamSpec은 decorator가 원래 callable의 parameter contract를 보존하게 한다

Decorator를 `Callable[..., Any] -> Callable[..., Any]`로 typing하면 wrapper를 통과하는 순간 parameter 이름과 타입 정보가 사라진다. ParamSpec을 사용하면 입력 callable의 parameter list를 capture해 반환 callable이 같은 signature를 유지한다고 표현할 수 있다.

Logging이나 timing decorator처럼 arguments를 그대로 forwarding하는 wrapper에서 유용하다. Return TypeVar와 결합하면 원래 return type도 보존한다. 이 정보는 IDE completion과 static check에서 wrapper가 transparent하게 보이게 한다.

Decorator가 실제로 argument를 추가하거나 제거한다면 Concatenate 같은 더 정밀한 typing이 필요할 수 있다. 하지만 복잡한 annotation이 runtime wrapper보다 더 이해하기 어려워지면 API 설계를 재검토한다.

`functools.wraps`가 runtime metadata를 보존하고 ParamSpec이 static signature 관계를 보존한다는 두 층을 구분한다. 하나가 다른 하나를 자동으로 해결하지 않는다.

---

## CHAPTER 08 · TypeGuard와 narrowing은 runtime predicate와 static knowledge를 연결한다

Generic object를 검사하는 함수가 단순 `bool`을 반환하면 type checker가 true branch에서 구체 type을 알지 못할 수 있다. TypeGuard 계열 annotation은 predicate가 참일 때 argument가 특정 타입이라고 정적 분석기에 알려 준다.

이 기능은 매우 강한 약속이다. Predicate implementation이 실제로 충분히 검사하지 않으면 checker가 잘못된 안전성을 믿게 된다. `is_user_dict()`가 id field만 확인하고 나머지 필드를 검증하지 않으면서 완전한 User payload라고 선언하면 runtime error가 뒤로 밀린다.

Built-in `isinstance`, `is None`, literal discriminator도 narrowing을 제공한다. Custom guard는 반복되는 복잡한 검사를 한 곳에 모을 때 사용한다.

Narrowing 이후 mutation으로 object shape가 달라질 수 있는 mutable data에서는 static assumption과 runtime state가 어긋날 가능성을 고려한다. Validated immutable object로 변환하면 계약이 더 안정적이다.

---

## CHAPTER 09 · Never와 exhaustive checking은 도달 불가능해야 하는 상태를 표현한다

모든 variant를 처리한 뒤 남은 branch가 이론상 존재할 수 없다면 Never/`assert_never` 같은 도구로 static checker에 exhaustiveness를 요청할 수 있다. Union에 새 variant가 추가되면 기존 match/if가 미처 처리하지 않은 위치를 발견하기 쉬워진다.

하지만 외부 입력에서 unknown 값이 실제로 들어올 수 있는데 type annotation만 믿고 unreachable로 처리하면 runtime에서 예외가 발생한다. Trust boundary의 validation이 먼저 closed union을 만들어야 한다.

함수가 항상 exception을 발생시키거나 process를 종료해 정상 return이 없다는 사실도 Never 계열 return type으로 표현할 수 있다. Control-flow analysis가 이후 code를 unreachable로 판단하는 데 도움이 된다.

Static exhaustiveness는 state machine 검증의 한 층이다. 허용 transition의 업무 정확성은 별도 test와 invariant가 필요하다.

---

## CHAPTER 10 · typing 강도는 오류 비용이 큰 경계부터 높인다

모든 private 변수에 가장 복잡한 generic을 붙이는 것이 좋은 typing 전략은 아니다. Public API, serialization boundary 이후의 domain model, callback/plugin interface, money/identifier처럼 혼동 비용이 큰 값에 정밀한 타입을 먼저 적용한다.

Type checker 설정은 project가 허용하는 Any, implicit optional, untyped function 범위를 결정한다. 한 번에 strict mode로 전환하기 어렵다면 새로운 module과 핵심 경계부터 강화하고 warning debt를 줄인다.

Runtime schema와 static type이 서로 다른 정의를 가지면 시간이 지나 drift할 수 있다. 가능하면 하나의 schema에서 type/model을 생성하거나 contract test로 두 표현이 일치하는지 확인한다.

Advanced typing의 목적은 annotation을 어려워 보이게 만드는 것이 아니다. **값 하나의 타입을 넘어 input-output 관계, capability, variant exhaustiveness, callback signature를 표현해 잘못된 연결이 실행되기 전에 드러나게 만드는 것**이다.