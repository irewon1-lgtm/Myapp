# PART 54 · Cache와 memoization — 재사용 속도와 stale state의 비용을 함께 관리하기

Cache는 이미 계산했거나 외부에서 읽어 온 값을 다시 사용해 latency와 load를 줄인다. 하지만 원본과 cached copy가 동시에 존재하는 순간 freshness, invalidation, memory budget, duplicate refresh라는 새로운 correctness 문제가 생긴다. Memoization과 LRU cache를 문법적 최적화로만 보면 hit case는 빨라져도 데이터가 오래되거나 memory가 무한히 늘 수 있다. 핵심은 **어떤 입력이 같은 계산을 뜻하고, 얼마 동안 결과를 다시 써도 되며, 원본이 바뀌었을 때 copy를 어떻게 무효화할지**를 계약으로 만드는 것이다.

---

## CHAPTER 01 · cache key는 같은 요청의 identity를 정의한다

`price(user, item, currency, now)`의 결과를 item ID 하나만으로 cache하면 사용자 등급과 통화, 시점이 다른 호출이 같은 entry를 공유할 수 있다. Key에는 결과에 영향을 주는 모든 의미 있는 입력이 포함되어야 한다. 반대로 logging용 request ID처럼 결과에 영향이 없는 값까지 key에 넣으면 같은 계산이 매번 다른 entry가 되어 hit rate가 사라진다.

Key normalization도 중요하다. Query parameter order가 결과 의미와 무관하다면 `a=1&b=2`와 `b=2&a=1`을 같은 key로 canonicalize할 수 있다. 문자열의 Unicode normalization, case policy, default parameter 적용 여부도 identity에 영향을 준다. Cache key를 단순 argument `repr`로 만들면 runtime version과 object representation 변화에 취약할 수 있다.

Security context가 결과를 바꾸는 경우 tenant/user ID를 반드시 key에 포함한다. Authorization 결과를 global key로 cache하면 다른 사용자에게 이전 사용자의 허용 결과를 재사용할 수 있다. Cache partitioning은 성능 설정이 아니라 데이터 격리 contract다.

---

## CHAPTER 02 · memoization은 hidden input이 없는 계산에서 가장 안전하다

같은 explicit input에서 같은 output을 만드는 pure function은 memoization하기 쉽다. Argument tuple을 key로 result를 저장하면 correctness를 바꾸지 않고 계산만 줄일 수 있다. 반대로 함수가 현재 시각, environment variable, database state, random generator를 몰래 읽으면 visible argument가 같아도 실제 의미가 달라질 수 있다.

Global memoization decorator를 붙이기 전에 hidden dependency를 찾는다. Tax rule version이 global configuration에 있고 function argument에는 없다면 configuration update 뒤에도 old result가 계속 반환될 수 있다. Rule version을 key에 포함하거나 config를 explicit input으로 바꾸면 invalidation 기준이 선명해진다.

Memoization은 object lifetime도 늘린다. Key와 result를 cache가 strong reference로 보관하면 caller가 더 이상 사용하지 않아도 object graph가 남는다. Unbounded input space에 무제한 memoization을 적용하면 성능 최적화가 memory leak처럼 변할 수 있다.

---

## CHAPTER 03 · TTL은 정확한 freshness가 아니라 최대 stale 허용시간을 정한다

TTL 60초는 원본이 60초 동안 변하지 않는다는 뜻이 아니다. Entry 생성 1초 뒤 원본이 바뀌어도 남은 59초 동안 stale result가 제공될 수 있다. 따라서 TTL은 business가 허용할 수 있는 stale window와 origin load를 함께 보고 정한다. 권한, 잔액, static catalog의 허용 범위는 서로 다르다.

Elapsed TTL 계산에는 monotonic clock이 wall-clock jump에 더 안전할 수 있다. 반면 upstream이 version 또는 `updated_at`을 제공한다면 그 version을 cache validation에 활용할 수 있다. Time-based expiry와 version-based freshness는 목적이 다르다.

모든 entry가 정확히 같은 TTL로 만료되면 deployment나 batch load 후 동시에 cache miss가 몰릴 수 있다. Jitter를 추가해 expiry를 분산하거나 인기 key는 만료 전에 refresh할 수 있다. Freshness policy는 traffic shape에도 영향을 준다.

---

## CHAPTER 04 · LRU는 memory budget을 제한하지만 entry 크기 차이를 숨길 수 있다

Least Recently Used eviction은 maximum entry 수를 넘으면 오래 사용되지 않은 값을 제거한다. Working set이 최근 사용 패턴과 비슷할 때 유용하고 Python의 cache 도구에서도 흔히 사용된다. 그러나 `maxsize=1000`은 memory를 정확히 1000개의 동일 크기 object만큼 사용한다는 뜻이 아니다.

한 entry가 10KB이고 다른 entry가 50MB라면 count limit만으로 peak memory를 통제하기 어렵다. Large result cache는 byte-size budget과 serialization size를 함께 고려하거나 너무 큰 result는 애초에 cache하지 않는 admission policy를 둘 수 있다.

LRU hit ratio만 최적화하면 scan workload에서 hot entry가 밀려나는 문제가 생길 수 있다. Cache가 실제 latency와 origin load를 줄였는지 benchmark하고, memory pressure와 eviction rate를 함께 관찰한다. Policy 이름이 workload 적합성을 보장하지 않는다.

---

## CHAPTER 05 · invalidation 순서는 원본 write와 cache write 사이의 실패를 다룬다

Database를 update하고 cache를 삭제하는 전형적인 cache-aside 구조에서도 두 operation은 하나의 transaction이 아닐 수 있다. DB commit 후 cache invalidation 전에 process가 죽으면 stale entry가 TTL까지 남는다. 반대로 commit 전에 cache를 먼저 지우면 다른 request가 old DB 값을 다시 cache에 넣을 수 있다.

정확한 일관성이 필요한 경우 versioned key, change event, write-through store 같은 다른 전략을 고려한다. Versioned key에서는 data version이 바뀌면 새로운 key를 사용하므로 old entry가 남아 있어도 더 이상 조회되지 않는다. 대신 old key cleanup과 key cardinality가 새로운 문제다.

Aggregate cache는 한 row 변경이 여러 cached report에 영향을 줄 수 있어 invalidation graph가 빠르게 복잡해진다. 모든 dependency를 manual delete call로 흩뜨리기보다 cache scope를 줄이거나 source version을 key에 포함해 일관성 모델을 단순화한다.

---

## CHAPTER 06 · stampede는 인기 key의 동시 miss가 origin을 과부하시킬 때 발생한다

인기 cache entry가 만료되는 순간 수백 request가 동시에 miss를 보고 같은 database query나 remote API를 실행하면 cache가 있음에도 backend가 순간 과부하될 수 있다. 이를 stampede 또는 thundering herd로 볼 수 있다. 단순 hit ratio metric으로는 이 순간 위험을 잘 설명하지 못한다.

Single-flight 전략은 같은 key의 refresh를 한 실행만 수행하고 다른 요청은 그 future/result를 기다리게 한다. Leader가 timeout되거나 실패했을 때 follower가 모두 같은 failure를 받을지 새 leader를 선출할지 정책이 필요하다. 기다림에 deadline도 둔다.

Stale-while-revalidate는 만료된 값을 일정 시간 더 제공하면서 한 worker만 background refresh해 user latency와 origin spike를 줄일 수 있다. 그러나 stale을 허용할 수 없는 권한·금액 data에는 적합하지 않을 수 있다. Availability와 consistency trade-off를 domain별로 정한다.

---

## CHAPTER 07 · negative cache는 “없음”을 재사용하지만 회복 속도를 늦출 수 있다

존재하지 않는 product ID나 DNS name처럼 deterministic absence를 반복 조회한다면 “없음” 결과도 짧게 cache할 수 있다. Miss traffic을 줄이는 데 유용하지만 바로 뒤에 object가 생성되면 negative entry가 남아 있는 동안 계속 없다고 보일 수 있다. Normal data보다 더 짧은 TTL을 사용할 이유가 있다.

Timeout, 500 error, authentication failure를 absence와 같은 방식으로 cache하면 transient outage가 복구된 뒤에도 failure를 재사용할 수 있다. Failure classification 없이 모든 exception을 negative cache에 넣지 않는다. Deterministic `not found`와 retryable failure를 구분한다.

Permission result를 cache할 때는 policy version과 subject identity가 key에 들어가야 한다. User role이 바뀐 뒤 stale allow가 남는 것은 security incident가 될 수 있다. Negative/positive permission cache 모두 fail-open/fail-closed 정책을 명시한다.

---

## CHAPTER 08 · cache contract는 key·freshness·budget·failure를 함께 정의해야 한다

Cache를 도입할 때 “이 함수 결과를 cache한다”는 설명만으로는 부족하다. 어떤 input이 같은 key인지, 최대 stale 시간이 얼마인지, memory/entry budget이 무엇인지, origin failure에서 stale을 제공할지 실패할지, concurrent refresh를 어떻게 합칠지 문서화한다.

Observability에는 hit/miss ratio뿐 아니라 load latency, eviction, entry age, refresh failure, stampede wait를 포함한다. Cache 때문에 user latency가 줄었는지와 origin load가 실제로 감소했는지 둘 다 측정한다. Correctness incident에서는 반환 entry의 version/age를 안전하게 기록하면 stale 원인을 추적하기 쉽다.

Cache 설계의 핵심은 **값을 빨리 찾는 자료구조를 추가하는 것이 아니라 원본보다 오래 살아 있는 copy의 identity와 freshness를 명시하고, 그 copy가 잘못된 상태가 되는 경로를 제한하는 것**이다.