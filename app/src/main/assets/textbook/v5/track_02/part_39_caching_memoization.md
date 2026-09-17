# PART 39 · Cache와 memoization — 계산·I/O 결과를 재사용하면서 freshness를 통제하기

Cache는 느린 계산이나 외부 조회 결과를 저장해 같은 작업을 반복하지 않게 한다. 성능은 좋아질 수 있지만 원본과 복사본이 동시에 존재하면서 **언제 값이 유효하고 언제 버려야 하는가**라는 새로운 correctness 문제가 생긴다. Memoization, TTL cache, LRU, distributed cache는 구현은 달라도 key identity, freshness, invalidation, concurrency, memory budget이라는 공통 계약을 가진다.

---

## CHAPTER 01 · cache key는 “같은 요청”의 의미를 정의한다

함수 결과를 재사용하려면 두 호출이 같은 입력인지 판단할 key가 필요하다. `price(user, item, currency)` 결과를 item ID만으로 cache하면 사용자 등급이나 통화가 다른 요청이 같은 entry를 공유해 잘못된 값을 반환할 수 있다. Key는 함수의 실제 결과에 영향을 주는 모든 입력을 포함해야 한다.

Mutable object를 key로 직접 사용하면 호출 사이에 상태가 바뀌어 같은 logical 요청의 identity가 불안정해질 수 있다. Stable ID, normalized argument tuple, immutable configuration version처럼 재현 가능한 representation을 선택한다. 순서가 의미 없는 query parameter를 raw string 순서 그대로 key로 쓰면 semantic하게 같은 요청이 여러 entry로 분리될 수 있어 canonicalization도 필요하다.

Security context도 key 일부일 수 있다. Authorization 결과나 tenant-specific data를 cache하면서 사용자/tenant identity를 빼면 다른 권한 영역의 값을 재사용하는 심각한 isolation bug가 된다. Cache key 설계는 performance detail이 아니라 data partition contract다.

---

## CHAPTER 02 · memoization은 pure function에 가장 자연스럽지만 입력 lifetime을 연장할 수 있다

같은 argument에 같은 result를 반환하는 pure function은 memoization하기 쉽다. 입력 tuple을 key로 저장하고 결과를 재사용하면 계산을 줄일 수 있다. 반대로 현재 시각, random, database state처럼 숨은 입력에 의존하는 함수는 visible argument만 key로 사용하면 stale 또는 잘못된 결과가 생긴다.

Memoization cache가 argument object와 result를 strong reference로 보관하면 원래 호출이 끝난 뒤에도 object lifetime이 길어질 수 있다. 무제한 key space에서 memoization을 적용하면 memory가 계속 증가해 leak처럼 보일 수 있다. 최대 entry 수나 explicit clear 정책이 필요하다.

Function code나 configuration이 바뀌었을 때 이전 result를 계속 사용할 수 있는지도 본다. Process-local memoization은 restart로 자연스럽게 사라지지만 persistent cache는 algorithm version을 key에 포함하거나 migration/flush가 필요할 수 있다.

---

## CHAPTER 03 · TTL은 시간 제한일 뿐 원본 변경을 즉시 감지하지 않는다

TTL 60초 cache는 entry가 생성된 뒤 일정 기간 재사용할 수 있다는 정책이다. 원본 데이터가 1초 뒤 바뀌어도 남은 59초 동안 오래된 값이 제공될 수 있다. 따라서 TTL은 “최대 stale 허용시간”과 연결해 선택해야 한다. 가격, 권한, 정적 catalog는 각각 허용 가능한 staleness가 다르다.

Expiration을 판단할 때 elapsed duration이면 monotonic clock이 적합할 수 있다. Wall clock이 뒤로 조정되면 entry가 예상보다 오래 살아 있는 오류를 피할 수 있다. 반면 원본이 `updated_at` instant를 제공한다면 version timestamp 자체를 freshness 조건으로 사용할 수 있다.

TTL을 너무 짧게 하면 hit rate가 낮아지고 동시에 많은 entry가 만료되어 downstream load spike를 만들 수 있다. Jitter를 섞어 만료 시점을 분산하거나 event-driven invalidation과 결합할 수 있다. Freshness와 origin load 사이의 trade-off를 측정한다.

---

## CHAPTER 04 · LRU는 사용 빈도 추정이 아니라 제한된 memory에서 eviction 순서를 정한다

LRU는 최근에 사용되지 않은 entry부터 제거하는 정책이다. Working set이 최근 사용 패턴과 비슷한 workload에서 효과적일 수 있지만 모든 cache에 최적은 아니다. 한 번만 순차적으로 스캔하는 대규모 dataset은 유용한 hot entry를 밀어낼 수 있다.

Entry count 제한은 모든 object가 같은 크기일 때만 memory 상한과 비슷하다. Result 크기가 크게 다르면 byte-size-aware eviction이 필요할 수 있다. Cache hit ratio만 보고 success를 판단하지 않고 memory footprint와 origin request 감소량, latency를 함께 본다.

Eviction은 데이터 삭제가 아니라 cache copy 제거다. 원본 source는 남아 있어 miss 시 다시 계산/조회할 수 있어야 한다. Cache가 사실상 유일한 저장소가 되면 eviction policy가 data loss policy가 되므로 architecture가 잘못된 것이다.

---

## CHAPTER 05 · invalidation은 원본 변경과 cached copy 사이의 일관성 계약이다

데이터를 update한 뒤 관련 cache entry를 지우거나 새 값으로 갱신해야 한다. Write-through, write-behind, cache-aside 패턴은 update 순서와 failure behavior가 다르다. Database commit 전에 cache를 갱신했다가 transaction이 rollback되면 cache에는 존재하지 않는 미래 상태가 남을 수 있다.

Cache-aside에서는 DB update가 commit된 뒤 cache를 invalidate하는 방식이 흔하지만 commit과 invalidation 사이에 crash하면 stale entry가 남을 수 있다. TTL이 최종 safety net 역할을 하거나 version key를 사용해 old entry를 더 이상 찾지 않게 만들 수 있다.

한 object 변경이 여러 aggregate cache에 영향을 주면 dependency graph가 복잡해진다. Invalidation list를 수동으로 곳곳에 흩뜨리기보다 domain event나 version namespace로 변경 범위를 관리한다. “Cache invalidation이 어렵다”는 말은 결국 copy가 많아질수록 consistency 경로가 늘어난다는 뜻이다.

---

## CHAPTER 06 · cache stampede는 같은 miss가 동시에 origin으로 몰리는 현상이다

인기 key가 만료되는 순간 수백 request가 동시에 miss를 보고 모두 같은 expensive query를 실행하면 cache가 있어도 backend가 순간 과부하될 수 있다. 이를 stampede 또는 thundering herd 문제로 볼 수 있다. 단순 lock 하나로 막을 수도 있지만 요청 latency와 failure policy를 함께 설계해야 한다.

Single-flight 방식은 같은 key에 대해 한 작업만 origin을 조회하고 다른 요청은 그 결과를 기다리게 한다. 기다릴 최대 시간과 leader 실패 시 follower가 어떻게 처리할지 정한다. 오래된 값을 잠시 제공하며 한 worker만 background refresh하는 stale-while-revalidate 전략도 있다.

분산 cache에서는 process-local lock만으로 여러 server의 stampede를 막지 못한다. Distributed lock, lease, probabilistic early refresh 같은 더 높은 수준의 coordination이 필요할 수 있다. 실제 트래픽 규모가 작다면 복잡한 분산 locking보다 TTL jitter만으로 충분할 수도 있다.

---

## CHAPTER 07 · negative caching은 실패와 부재도 재사용하지만 복구를 늦출 수 있다

존재하지 않는 ID를 조회할 때마다 database를 치는 것이 비싸다면 “없음” 결과를 짧게 cache할 수 있다. DNS의 negative response처럼 absence도 결과의 한 종류다. 하지만 방금 생성된 object가 negative cache 때문에 잠시 계속 없다고 보일 수 있어 TTL을 정상 data보다 짧게 둘 수 있다.

외부 서비스 500 error나 timeout을 cache하는 것은 더 위험하다. 일시 장애를 오래 재사용하면 origin이 복구된 뒤에도 application은 계속 실패한다. Circuit breaker와 retry, negative cache를 혼동하지 않는다. 무엇이 deterministic absence이고 무엇이 transient failure인지 구분한다.

Permission denied 결과를 cache할 때는 user identity와 policy version을 key에 포함해야 한다. 권한 변경 후 stale deny 또는 stale allow가 얼마나 오래 허용되는지도 security contract다.

---

## CHAPTER 08 · distributed cache는 serialization·network·partial failure 비용을 추가한다

In-process cache는 매우 빠르지만 process마다 값이 달라질 수 있고 restart하면 사라진다. Redis 같은 remote cache는 여러 instance가 공유할 수 있지만 network latency, serialization, authentication, timeout, cluster failure가 새로운 경계가 된다. 작은 계산을 remote cache에서 가져오는 것이 직접 계산보다 느릴 수도 있다.

Cache value schema도 versioning이 필요하다. Old application이 쓴 serialized value를 new application이 읽을 수 있는지, rolling deploy 중 두 version이 같은 key를 공유해도 되는지 확인한다. Version prefix를 key namespace에 넣으면 incompatible format을 분리할 수 있다.

Remote cache가 unavailable할 때 origin으로 fallback할지 전체 요청을 실패시킬지 결정한다. 모두 fallback하면 cache 장애 순간 backend가 감당하지 못할 load를 받을 수 있으므로 rate limit과 degraded mode가 필요할 수 있다.

---

## CHAPTER 09 · cache observability는 hit ratio만으로 충분하지 않다

높은 hit ratio가 좋은 것처럼 보여도 stale data incident가 있거나 cache lookup latency가 origin보다 비싸면 성공이 아니다. Hit, miss, eviction, expiration, load latency, entry count/bytes, refresh failure를 함께 본다. Key cardinality가 비정상적으로 증가하면 key design bug나 attack 신호일 수 있다.

Per-key metric은 cardinality가 너무 높아질 수 있으므로 key class나 namespace 수준으로 집계한다. Rare hot key 분석은 sampled log나 tracing에서 수행할 수 있다. Cache layer가 request latency에서 차지하는 span을 보면 serialization과 network 비용을 분리할 수 있다.

Correctness incident에서는 어떤 version의 원본과 cached copy가 반환됐는지 evidence가 필요하다. Value 자체를 모두 log하지 않고 cache version, age, source, hit/miss를 안전하게 남긴다.

---

## CHAPTER 10 · cache 도입 기준은 “재사용 가치가 consistency 비용보다 큰가”다

Cache는 느린 것을 빠르게 만드는 만능 layer가 아니다. 계산이 충분히 비싸고 반복될 가능성이 높으며 일정 수준의 stale을 허용할 수 있을 때 가치가 크다. Origin이 이미 빠르고 data가 매 요청 달라지며 strict consistency가 필요하면 cache가 복잡성만 추가할 수 있다.

도입 전에 baseline latency와 origin load를 측정하고 target을 정한다. 이후 key correctness, memory budget, TTL/invalidation, stampede, failure fallback을 각각 검증한다. 단순 benchmark에서 hit case만 빠르다는 이유로 production-safe하다고 판단하지 않는다.

Cache 설계의 핵심은 **값을 저장하는 자료구조가 아니라 같은 계산을 언제 재사용해도 되는지, stale copy를 언제 버리며, 동시에 갱신될 때 누가 책임지는지를 명시하는 consistency contract**다.