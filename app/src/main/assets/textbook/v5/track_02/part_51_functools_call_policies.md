# PART 51 · functools와 call policy — partial·cache·dispatch로 호출 계약을 합성하기

함수는 값이기 때문에 argument 일부를 미리 고정하거나, 이전 결과를 재사용하거나, runtime type에 따라 구현을 선택하는 새로운 callable을 만들 수 있다. 이런 합성은 반복 코드를 줄이지만 원래 함수 위에 **capture된 상태, cache key, dispatch rule, wrapper order**라는 새 계약을 추가한다. 짧은 decorator 한 줄이 실제 실행 의미를 감추지 않도록 무엇이 저장되고 언제 선택되는지를 추적해야 한다.

---

## CHAPTER 01 · partial application은 일부 argument를 정의 시점의 정책으로 고정한다

같은 함수에 매번 동일한 설정을 넘긴다면 `partial` 계열 도구로 더 좁은 callable을 만들 수 있다. `convert(amount, currency, rounding)`에서 currency와 rounding을 미리 묶어 `convert_krw(amount)`처럼 사용하는 식이다. Global configuration을 함수 안에서 읽는 것보다 어떤 값이 behavior를 구성했는지 객체에 드러난다.

고정한 argument가 immutable value라면 호출마다 같은 정책을 재사용하기 쉽다. Mutable dict나 list를 capture하면 이후 다른 코드의 mutation이 partial callable behavior를 바꿀 수 있다. Configuration snapshot이 필요한 경우 validated immutable object를 넘긴다.

Partial은 함수 argument contract를 줄이는 도구이지 business state를 감추는 저장소가 아니다. 사용자 ID나 현재 transaction처럼 호출마다 달라야 하는 값을 미리 고정하면 lifetime이 예상보다 길어질 수 있다.

Callback API에 partial을 전달할 때 resulting signature와 introspection behavior도 확인한다. Framework가 parameter 이름과 annotation을 읽는다면 원래 function과 동일하게 보이지 않을 수 있다.

---

## CHAPTER 02 · memoization은 계산 결과와 argument identity를 함께 저장한다

Memoization cache는 동일한 key가 다시 들어오면 이전 return을 재사용한다. Pure하고 expensive한 계산에서는 강하지만 외부 상태를 읽거나 side effect를 수행하는 함수에 적용하면 stale result와 누락된 effect를 만들 수 있다. Cache 대상 함수의 의미가 `input → output`으로 안정적인지 먼저 확인한다.

Key는 argument의 hash/equality와 normalization에 의존한다. 같은 의미의 path가 `"./a"`와 `"/x/a"`처럼 여러 representation을 가질 수 있다면 cache miss가 늘 수 있고, 반대로 서로 다른 의미를 과도하게 canonicalize하면 잘못된 hit가 생긴다.

Unbounded memoization은 새로운 key가 계속 들어오면 memory leak처럼 증가한다. Maximum size와 eviction, cache clear lifecycle을 workload에 맞춰 둔다. Function object가 살아 있는 동안 cache도 살아 있을 수 있다는 lifetime 관계를 고려한다.

Result가 mutable object라면 cache hit마다 같은 object reference를 여러 caller가 공유할 수 있다. Caller mutation을 허용할지 immutable result를 반환할지 contract를 정한다.

---

## CHAPTER 03 · cache invalidation은 source of truth가 바뀐 시점을 key에 반영하는 문제다

함수가 parameter 외에 price table version을 읽는데 cache key에는 version이 없다면 table이 갱신되어도 과거 결과를 계속 반환할 수 있다. 의미 있는 dependency를 key에 포함하거나 source update가 cache invalidation을 책임져야 한다.

TTL은 correctness 증명이 아니라 stale data를 최대 얼마 동안 허용하는지 정하는 정책이다. 권한이나 재고처럼 오래된 값이 위험한 domain에는 짧은 TTL조차 부적합할 수 있고, expensive reference data에는 합리적일 수 있다.

인기 key가 동시에 만료되면 많은 worker가 같은 재계산을 시작하는 cache stampede가 생길 수 있다. Single-flight, lock, jittered expiration을 사용해 recomputation을 합칠 수 있다. Lock 안에서 계산이 너무 길면 다른 요청 latency가 커지는 trade-off도 있다.

Invalidation metric과 cache hit ratio를 관찰하되 높은 hit ratio 자체가 목표가 아니다. 오래된 잘못된 값을 많이 반환하는 cache도 hit ratio는 높을 수 있다.

---

## CHAPTER 04 · single dispatch는 첫 argument type을 operation 선택 기준으로 만든다

하나의 logical operation이 input type마다 다른 구현을 가져야 할 때 single-dispatch generic function을 사용할 수 있다. Serializer가 datetime, decimal, domain object를 각기 다르게 encode하거나 renderer가 node type별 구현을 선택하는 경우가 자연스럽다.

선택 기준이 실제 type hierarchy가 아니라 status 문자열이나 arbitrary flag라면 dispatch를 위해 가짜 subclass를 만들지 않는다. 그 경우 state machine이나 table-based policy가 더 적합하다.

Registered implementation 중 어떤 것이 선택되는지는 runtime type과 inheritance relationship에 달려 있다. Multiple inheritance에서는 MRO와 가장 적합한 registration을 실제로 확인한다. Fallback implementation은 unsupported type을 조용히 이상한 값으로 바꾸기보다 명시적 오류를 내는 편이 안전할 수 있다.

Plugin이 registration을 추가할 수 있다면 registry content가 import order에 따라 달라지지 않도록 startup 단계에서 명시적으로 load한다.

---

## CHAPTER 05 · method dispatch와 generic function dispatch는 behavior owner가 다르다

`obj.render()`는 object type이 behavior를 소유하고, `render(obj)` generic function은 operation namespace가 type별 implementation을 소유한다. 어느 쪽이 더 좋은지는 data variant와 operation 중 무엇이 더 자주 늘어나는지에 따라 달라진다.

Domain entity의 핵심 invariant와 operation은 method에 두는 편이 응집력이 높다. 외부 export format처럼 object class를 수정하지 않고 operation을 계속 추가해야 한다면 adapter나 generic function이 class 오염을 줄일 수 있다.

Third-party type에 behavior를 붙여야 하는 경우에도 generic function은 subclass를 강제하지 않고 extension을 등록할 수 있다. 반대로 operation이 object identity의 본질이라면 method가 caller에게 더 자연스럽다.

Dispatch 도구를 고르는 목적은 `if`를 없애는 것이 아니라 **behavior와 data의 변경 축을 맞추는 것**이다.

---

## CHAPTER 06 · decorator stack은 겹친 순서가 authorization·retry·cache 의미를 바꾼다

`@cache`, `@retry`, `@authorize`가 한 함수에 붙으면 실제로는 wrapper가 중첩된다. Cache가 authorization 밖에 있으면 다른 사용자에게 같은 cached result가 노출될 수 있고, retry가 transaction 밖에 있는지 안에 있는지에 따라 attempt마다 새 transaction을 여는지 달라진다.

Decorator를 source 장식 순서로만 보지 말고 `A(B(C(function)))` 형태로 풀어 entry와 exit 순서를 추적한다. Exception type을 안쪽 wrapper가 변환하면 바깥 retry가 원래 transient error를 인식하지 못할 수도 있다.

Timing decorator가 안쪽에 있으면 각 attempt duration을 재고 바깥에 있으면 retry 전체 latency를 잰다. 어느 metric이 필요한지 contract가 decorator order를 결정한다.

Cross-cutting concern이 너무 많이 stack되어 실제 호출 의미가 숨으면 explicit service orchestration으로 펼치는 것이 더 안전하다.

---

## CHAPTER 07 · ordering helper는 비교 method를 생성하지만 domain order를 정의하지 않는다

일부 comparison method를 바탕으로 나머지 ordering method를 생성하는 편의 기능은 boilerplate를 줄인다. 그러나 두 value가 언제 같은지, 모든 두 값 사이에 total order가 존재하는지 domain에서 먼저 정해야 한다.

Version과 상태처럼 partial order만 자연스러운 값을 억지로 `<` 가능한 대상으로 만들면 sorting 결과에 존재하지 않는 의미를 부여한다. 필요한 operation이 equality뿐이라면 ordering을 구현하지 않는다.

Equality에 mutable field가 관여하면 object를 set/dict key로 사용한 뒤 hash contract가 깨질 수 있다. Comparable value object는 가능하면 immutable state로 만든다.

자동 생성 기능은 문법량만 줄이고 semantic responsibility는 그대로 남는다.

---

## CHAPTER 08 · reduce는 accumulator와 element 사이의 상태 전이를 압축한다

Sequence를 하나의 결과로 접는 reduce는 `(state, item) → new_state` 구조가 명확할 때 유용하다. Sum과 product뿐 아니라 validated configuration layer 합성, syntax tree fold를 표현할 수 있다.

Initial value가 없는 경우 empty input에서 결과를 만들 수 없거나 첫 element가 accumulator가 될 수 있다. Identity element가 존재한다면 explicit initial을 두어 empty behavior를 고정한다.

Mutable accumulator를 제자리에서 계속 수정하면 함수형 표면 뒤에 side effect가 숨는다. State transition 자체가 중요한 경우 explicit loop가 중간 invariant와 error handling을 더 잘 보여 줄 수 있다.

Parallel reduction이 가능한지는 combine operation의 associativity와 ordering requirement에 달려 있다. Floating-point sum과 last-wins config merge는 순서에 따라 결과가 바뀔 수 있다.

---

## CHAPTER 09 · wrapper metadata는 router·logger·type tooling의 입력이 된다

Decorator가 원래 함수의 이름, docstring, annotation, wrapped chain을 보존하지 않으면 router와 dependency injection, documentation tool이 wrapper만 보게 된다. Metadata-preserving helper를 사용하면 원래 callable의 public identity를 최대한 유지할 수 있다.

Runtime metadata와 static typing은 별도다. Metadata를 보존해도 type checker가 parameter relationship을 이해하려면 ParamSpec 같은 static annotation이 필요할 수 있다.

Decorator가 실제 signature를 변경하는 경우에는 원래 함수처럼 가장하지 않는다. 추가 parameter나 반환 type 변경을 새로운 API contract로 문서화하고 integration test에서 framework와 맞는지 확인한다.

Introspection은 convenience 기능이 아니라 framework boundary에서 실제 실행을 결정하는 input이 될 수 있다.

---

## CHAPTER 10 · call policy를 합성할수록 state·key·selection rule을 관측 가능하게 만든다

Partial은 captured argument, memoization은 cache state와 key, single dispatch는 implementation registry, decorator stack은 wrapper order를 추가한다. 모두 함수 호출 한 줄 뒤에 새로운 state machine을 만든다.

적용 전 원래 함수가 pure인지 effectful인지, return identity가 중요한지, argument가 stable key인지 확인한다. Cache와 retry를 붙인 뒤 correctness를 다시 정의하지 않으면 optimization과 resilience code가 원래 contract를 바꿀 수 있다.

관측에는 cache hit/miss, selected dispatch implementation, retry count처럼 실제 policy가 무엇을 했는지 필요한 만큼 남긴다. Hot path에 과도한 log를 추가해 성능 특성을 바꾸지는 않는다.

`functools`를 깊게 이해한다는 것은 helper 이름을 암기하는 것이 아니라 **callable 위에 새로운 정책을 합성했을 때 어떤 상태가 생기고 어떤 입력을 동일하게 보며 어떤 구현이 선택되는지 설명하는 것**이다.