# PART 45 · functools·dispatch·memoization — 호출 behavior를 재사용 가능한 정책으로 구성하기

함수는 값이기 때문에 wrapper, partial application, cache, generic dispatch 같은 도구로 호출 behavior를 합성할 수 있다. `functools` 계열 기능은 반복되는 wrapper code를 줄여 주지만 원래 함수의 계약에 새로운 lifetime, cache key, dispatch rule을 추가한다. 핵심은 **호출을 편하게 감싸는 것이 아니라 어떤 입력이 같은 계산으로 취급되고 어떤 구현이 선택되는지 명확히 하는 것**이다.

---

## CHAPTER 01 · `partial`은 argument 일부를 미리 바인딩한 새로운 callable을 만든다

같은 함수에 항상 같은 configuration argument를 반복해 전달한다면 partial application으로 더 좁은 callable을 만들 수 있다. `convert(value, currency, rounding)`에서 currency와 rounding을 고정해 `convert_krw(value)` 같은 behavior를 구성할 수 있다.

이 방식은 global configuration을 읽는 것보다 dependency가 명시적이고 test가 쉽다. 하지만 바인딩한 object가 mutable이면 partial이 그 object reference를 계속 보유하므로 이후 mutation이 호출 behavior를 바꿀 수 있다. Immutable configuration을 선호한다.

Partial callable의 signature와 metadata가 원래 함수와 다르게 보일 수 있으므로 framework introspection에 전달할 때 문서를 확인한다. Callback API가 parameter 수를 엄격히 검사하면 맞지 않을 수 있다.

Partial은 작은 configuration binding에 적합하고 stateful lifecycle이 필요하면 callable object가 더 설명력이 높다.

---

## CHAPTER 02 · cache는 동일 key의 계산을 재사용하면서 결과 lifetime을 늘린다

Memoization decorator는 argument를 key로 사용해 이전 결과를 저장하고 같은 key가 다시 들어오면 계산을 생략한다. Pure하고 expensive한 함수에서 강하지만 side effect가 있거나 외부 state에 따라 결과가 달라지는 함수에는 잘못된 stale result를 만들 수 있다.

Cache key는 hashable argument의 equality에 의존한다. 같은 의미의 input이 서로 다른 representation으로 들어오면 cache miss가 늘고, 서로 다른 의미를 같은 key로 canonicalize하면 잘못된 hit가 생긴다. Key normalization도 contract다.

Unbounded cache는 입력 종류가 계속 늘어날 때 memory retention을 만든다. LRU size, TTL, explicit invalidation 중 workload에 맞는 policy를 선택한다. Memoization은 성능 최적화이면서 stateful component다.

Exception을 cache하는지, `None`과 실패를 구분하는지도 구현마다 다를 수 있다. 외부 service 오류를 성공 결과처럼 오래 cache하지 않게 한다.

---

## CHAPTER 03 · cache invalidation은 source of truth가 바뀌는 순간을 정의하는 문제다

가격 계산 함수가 reference table을 global하게 읽는데 argument에는 table version이 없다면 table이 업데이트되어도 동일 argument cache는 오래된 결과를 반환할 수 있다. Cache key에 모든 의미 있는 dependency를 포함하거나 dependency change 시 전체/부분 invalidation을 수행한다.

Time-based TTL은 correctness rule이 아니라 “최대 얼마나 오래 stale을 허용하는가”라는 정책이다. 5분 TTL이 business freshness와 맞는지 명시한다. 권한과 재고처럼 stale 허용이 작으면 event-driven invalidation이나 source read가 필요할 수 있다.

Manual invalidation은 update path가 여러 개일 때 누락되기 쉽다. Source of truth를 변경하는 service가 invalidation을 함께 소유하도록 경계를 좁힌다.

Cache stampede도 고려한다. 인기 key가 만료된 순간 많은 worker가 동시에 expensive recomputation을 시작하면 downstream이 overload될 수 있다. Single-flight나 jittered expiry 같은 coordination이 필요할 수 있다.

---

## CHAPTER 04 · `singledispatch`는 첫 argument의 runtime type에 따라 구현을 선택한다

하나의 logical operation이 타입별로 다른 구현을 가져야 할 때 single dispatch를 사용할 수 있다. Serializer가 int, datetime, domain object를 서로 다르게 처리하거나 renderer가 node type별 implementation을 선택하는 경우다.

이 dispatch는 arbitrary business condition을 대체하는 switch가 아니다. 선택 기준이 실제 type hierarchy일 때 자연스럽다. 상태 string이나 feature flag까지 fake subclass로 만들면 type model이 왜곡된다.

등록된 구현 중 어떤 것이 선택되는지는 type inheritance와 가장 가까운 matching implementation에 영향을 받는다. Multiple inheritance에서는 MRO와 registry 관계를 확인한다.

새 type support를 extension module에서 등록할 수 있지만 import order와 plugin lifecycle이 registry content에 영향을 줄 수 있다. Startup에서 registry snapshot과 duplicate policy를 관리한다.

---

## CHAPTER 05 · method dispatch와 function dispatch는 behavior owner가 다르다

Object method는 receiver type이 behavior를 소유하고 `obj.render()`처럼 호출한다. Generic function dispatch는 operation namespace가 behavior를 소유하고 `render(obj)`에서 type별 implementation을 선택한다. 어느 쪽이 적합한지는 변화 축에 따라 달라진다.

Domain entity가 자신의 핵심 invariant와 operation을 소유한다면 method가 자연스럽다. 외부 serialization/export처럼 domain object를 여러 format으로 처리하는 operation이 계속 추가된다면 generic function이나 adapter가 core class 오염을 줄일 수 있다.

같은 data type에 서로 다른 third-party operation이 추가될 때 class를 수정할 수 없는 경우 function dispatch가 유용하다. 반대로 subtype마다 behavior가 기본 정체성의 일부라면 polymorphic method가 더 읽기 쉽다.

Dispatch 도구를 고르는 목적은 if 문을 없애는 것이 아니라 behavior와 data의 ownership을 실제 변화 방향에 맞추는 것이다.

---

## CHAPTER 06 · decorator stack은 적용 순서가 실행 semantics를 결정한다

Cache와 retry, authorization, timing decorator를 같은 함수에 붙이면 어느 wrapper가 바깥에 있는지에 따라 결과가 달라진다. Authorization 바깥에 cache가 있으면 권한별 결과가 같은 cache key를 공유해 security bug가 생길 수 있다. Retry 안쪽에 timing을 두면 각 attempt를 측정하고 바깥에 두면 전체 latency를 측정한다.

Decorator 순서를 source 장식으로 보지 않고 nested function call로 풀어 적는다. `A(B(function))`의 runtime entry/exit order를 trace하면 resource lifetime과 error handling이 선명해진다.

Wrapper가 exception을 변환하거나 swallow하면 바깥 decorator가 보는 failure type도 달라진다. Retry가 실제 transient error를 인식할 수 있는 위치에 있어야 한다.

Cross-cutting concern이 너무 많이 stack되면 호출 의미가 보이지 않는다. Critical transaction과 authorization은 explicit service boundary로 옮기는 것이 더 나을 수 있다.

---

## CHAPTER 07 · ordering helper는 rich comparison contract를 생성하지만 equality 의미가 먼저다

사용자 정의 값 객체에서 일부 comparison method를 바탕으로 다른 ordering method를 생성하는 helper를 사용할 수 있다. 하지만 무엇을 기준으로 두 객체가 같은지와 total order가 가능한지 domain에서 먼저 정해야 한다.

Version string, semantic category처럼 partial order만 자연스러운 값에 임의 total ordering을 부여하면 호출자가 정렬 결과에 잘못된 의미를 부여할 수 있다. 모든 object가 `<`를 가져야 하는 것은 아니다.

Equality와 hash 관계도 유지한다. Comparable value가 mutable field에 따라 equality가 바뀌면 set/dict key invariant가 깨질 수 있다.

자동 생성되는 convenience method는 boilerplate를 줄일 뿐 domain order rule을 정의해 주지는 않는다.

---

## CHAPTER 08 · reduce는 accumulator contract가 명확할 때 사용한다

`reduce`는 sequence를 하나의 state로 접는 일반 operation이다. Sum처럼 단순한 결합뿐 아니라 여러 configuration layer merge도 표현할 수 있다. 그러나 accumulator type과 결합 rule이 복잡하면 명시적 loop가 중간 상태를 더 잘 보여 줄 수 있다.

Initial value가 없으면 empty input behavior가 달라질 수 있다. Identity element가 있는 operation이면 explicit initial을 사용하는 편이 contract가 명확하다.

Mutable accumulator를 제자리 수정하면서 reduce를 사용하면 functional-looking code 뒤에 side effect가 숨는다. Immutable transition 또는 명시적 loop를 선택한다.

Parallel reduction 가능성은 operation의 associativity에 달려 있다. Floating sum과 order-sensitive merge는 결합 순서에 따라 결과가 달라질 수 있다.

---

## CHAPTER 09 · wrapper metadata와 signature는 tooling contract의 일부다

Decorator가 원래 함수의 이름과 documentation, annotations를 잃으면 logger와 router, test framework, dependency injection tool이 wrapper만 보게 된다. `wraps` 계열 기능으로 metadata chain을 보존하면 introspection tool이 original callable에 접근할 수 있다.

Runtime metadata와 static typing은 별개다. `wraps`를 썼다고 type checker가 parameter relation을 자동 이해하는 것은 아니며 ParamSpec 같은 typing이 필요할 수 있다.

Signature를 일부러 바꾸는 decorator도 있다. Dependency parameter를 삽입하거나 command-line option을 추가한다면 새 public contract를 명시적으로 제공해야 한다.

Tooling이 `__wrapped__` chain을 따라갈 수 있지만 wrapper가 여러 library를 거치면 compatibility를 실제 integration test로 검증한다.

---

## CHAPTER 10 · 호출 정책을 합성할수록 state·key·order를 눈에 보이게 만든다

Memoization은 cache state, dispatch는 implementation registry, partial은 captured argument, decorator stack은 wrapper order라는 새로운 숨은 구조를 만든다. 한 줄 decorator로 보이더라도 runtime에는 state와 selection rule이 존재한다.

성능이나 재사용을 위해 도구를 적용하기 전에 원래 함수 contract를 고정한다. Pure calculation인지, external effect가 있는지, identity-sensitive result인지에 따라 cache와 wrapper 가능성이 달라진다.

Observability에는 cache hit/miss, selected dispatch implementation, retry attempts처럼 합성된 behavior를 이해할 evidence를 남길 수 있다. 단, hot function에 과도한 logging을 넣어 비용을 바꾸지 않는다.

`functools`를 배우는 목적은 편의 함수를 암기하는 것이 아니다. **함수 호출에 policy를 합성할 때 어떤 state를 추가하고 어떤 input을 동일하게 보며 어떤 구현을 선택하는지 명시해 abstraction이 원래 의미를 흐리지 않게 하는 것**이다.