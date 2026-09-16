# PART 10 · 성능 공학 — queueing, saturation, tail, capacity

성능 문제는 `느리다`가 아니라 **service time, wait time, concurrency, arrival rate, utilization, tail distribution**의 관계다. 최적화는 code tweak가 아니라 어느 자원이 saturation되어 queue를 만들고 있는지 증명하는 작업이다.

---

## CHAPTER 01 · latency와 throughput은 독립적으로 측정한다

latency는 개별 operation의 완료 시간이고 throughput은 단위 시간당 완료량이다. batching이나 concurrency를 늘리면 throughput이 증가하면서 개별 latency가 악화될 수 있다.

성능 목표는 둘 중 하나만 최대화하지 않는다. interactive system은 tail latency constraint 안에서 필요한 throughput을 유지해야 하고 batch system은 deadline 안에서 총 work를 완료해야 한다.

---

## CHAPTER 02 · service time과 wait time을 분리해야 원인이 보인다

request latency는 resource가 실제로 작업한 service time과 queue/scheduler/lock에서 기다린 wait time의 합으로 볼 수 있다. CPU profile이 짧은데 wall time이 길다면 wait component가 크다는 신호다.

각 stage에 enqueue/dequeue/start/end timestamp를 두면 queue delay와 execution cost를 분리할 수 있다. service time을 줄일지 admission/parallelism을 바꿀지 결정이 달라진다.

---

## CHAPTER 03 · utilization이 100%에 가까워질수록 queueing delay는 비선형적으로 커진다

평균 arrival rate가 평균 service capacity에 근접하면 작은 burst나 service-time 변동도 queue를 만든다. utilization 50%에서 안정적인 system이 90%에서 같은 비율로 느려지는 것이 아니다.

capacity planning은 평균 utilization만 보지 않고 burstiness와 failure headroom을 포함한다. 정상 peak에서도 항상 일부 여유가 있어야 replica loss나 retry surge를 흡수할 수 있다.

---

## CHAPTER 04 · Little’s Law는 in-flight work, throughput, residence time을 연결한다

steady-state에서 평균 system 내 작업 수 `L`, throughput `λ`, 평균 residence time `W`는 `L = λW` 관계를 갖는다. 이는 queue 구현을 몰라도 concurrency와 latency가 일관적인지 검산하는 데 유용하다.

예상 throughput과 latency로 필요한 in-flight 수를 추정하고 실제 connection/thread/queue 수와 비교한다. 측정이 관계와 크게 어긋나면 window 정의나 dropped work, non-steady state를 재검토한다.

---

## CHAPTER 05 · queue는 overload를 저장할 뿐 capacity를 만들지 않는다

queue는 producer와 consumer 속도 차이를 일시적으로 흡수하지만 장기 arrival rate가 service rate보다 크면 길이는 계속 증가한다. unbounded queue는 즉시 실패를 tail latency와 memory consumption으로 바꾼다.

queue metric에는 depth뿐 아니라 oldest-item age를 포함한다. depth가 일정해도 오래된 work가 남아 있으면 starvation/priority 문제가 있을 수 있다.

---

## CHAPTER 06 · bounded queue는 admission control의 일부다

queue capacity를 제한하면 full 시 block, reject, drop, shed 중 정책을 선택해야 한다. 이 결정은 데이터의 손실 허용성과 upstream retry behavior에 따라 달라진다.

reject를 실패로만 보지 않는다. downstream을 보호해 전체 system collapse를 막는 safety mechanism일 수 있다. caller가 retry한다면 retry budget과 backoff를 함께 설계한다.

---

## CHAPTER 07 · fan-out은 tail latency를 증폭한다

한 request가 병렬로 여러 dependency를 호출하고 모두 기다리면 전체 latency는 가장 느린 branch의 영향을 받는다. dependency 하나의 p99가 작아 보여도 fan-out 수가 많으면 최소 한 branch가 느릴 확률이 커진다.

fan-out 최적화는 평균 dependency latency만 줄이는 것이 아니라 request count, hedge policy, quorum, cache 사용을 포함한다. 불필요한 synchronous dependency를 critical path에서 제거한다.

---

## CHAPTER 08 · deadline은 전체 request budget을 downstream에 배분한다

각 layer가 독립적인 5초 timeout을 사용하면 chain 전체는 훨씬 오래 걸릴 수 있다. end-to-end deadline을 전달하고 남은 budget에 맞춰 downstream timeout을 정한다.

timeout은 성공 가능성이 없는 작업을 빨리 중단해 resource를 회수해야 한다. 이미 deadline을 넘긴 request가 DB/worker queue에서 계속 실행되면 useful throughput을 잠식한다.

---

## CHAPTER 09 · retry는 failure traffic을 load amplifier로 바꿀 수 있다

error rate가 오를 때 client가 즉시 retry하면 original traffic 위에 추가 traffic이 생긴다. 여러 layer가 각각 retry하면 attempt 수가 곱셈으로 증가할 수 있다.

retry는 transient failure에만 제한하고 idempotency, maximum attempts, retry budget을 둔다. capacity가 부족한 overload에는 retry보다 load shedding이 맞을 수 있다.

---

## CHAPTER 10 · exponential backoff와 jitter는 synchronized retry wave를 줄인다

모든 client가 동일한 fixed delay로 retry하면 outage 후 같은 시점에 다시 몰리는 thundering herd가 생긴다. exponential backoff는 attempt 간격을 늘리고 jitter는 retry time을 분산한다.

backoff 상한과 total deadline을 함께 둔다. 무한 backoff는 user-visible failure를 늦출 뿐이다. server가 Retry-After 같은 signal을 제공하면 client policy와 결합한다.

---

## CHAPTER 11 · Amdahl’s Law는 최적화 가능한 부분의 상한을 보여 준다

전체 시간 중 fraction `p`만 speedup `s`배 개선하면 전체 speedup은 나머지 serial fraction에 제한된다. 10% 구간을 10배 빠르게 해도 전체 개선은 제한적이다.

profile에서 hotspot 비중을 확인하지 않고 작은 함수 micro-optimization에 투자하면 전체 latency가 거의 변하지 않는다. critical path 비중이 optimization priority를 정한다.

---

## CHAPTER 12 · parallelism은 dependency와 shared bottleneck에 제한된다

work를 여러 thread/core로 나눠도 serial dependency, shared memory bandwidth, lock, storage queue가 병목이면 speedup이 제한된다. worker 수를 늘리는 것과 실제 parallel service capacity는 같지 않다.

parallel overhead에는 task partition, synchronization, cache coherence, merge가 포함된다. 작은 task를 지나치게 분할하면 overhead가 useful work보다 커질 수 있다.

---

## CHAPTER 13 · scale-up과 scale-out은 state와 failure model을 바꾼다

larger instance는 single-node resource를 늘리지만 failure domain도 커질 수 있다. scale-out은 concurrency capacity를 늘리지만 load balancing, distributed state, coordination이 필요하다.

stateless request는 scale-out이 쉬운 반면 shared DB가 bottleneck이면 app replica만 늘려도 전체 capacity가 증가하지 않는다. end-to-end bottleneck을 먼저 찾는다.

---

## CHAPTER 14 · batching은 fixed overhead를 amortize하지만 queue delay를 추가한다

여러 item을 한 request/transaction/I/O에 묶으면 per-operation overhead를 줄일 수 있다. 그러나 batch를 채우기 위해 기다리는 시간이 interactive latency를 늘린다.

batch size는 throughput curve와 latency SLO로 결정한다. 최대 batch를 항상 채우는 대신 size 또는 deadline 중 먼저 도달한 조건으로 flush하는 방식이 흔하다.

---

## CHAPTER 15 · cache는 faster lookup과 invalidation cost를 교환한다

cache hit은 expensive computation/I/O를 피하지만 miss path, fill, eviction, memory footprint 비용이 있다. hit ratio가 높아도 가장 비싼 object가 miss하면 latency benefit이 작을 수 있다.

cache key cardinality, object size, admission policy를 함께 본다. 모든 데이터를 cache하면 working set이 커져 오히려 lower-level cache와 memory pressure를 악화시킬 수 있다.

---

## CHAPTER 16 · cache correctness는 freshness와 source-of-truth contract다

TTL만으로 consistency requirement가 해결되지 않는다. write-through, write-behind, explicit invalidation, version key마다 failure mode가 다르다.

stale data가 허용되는 최대 시간, read-after-write 필요성, invalidate failure 시 행동을 문서화한다. cache outage가 source DB overload로 전파되는 cache stampede도 대비한다.

---

## CHAPTER 17 · GC performance는 allocation rate와 live set 모두의 함수다

heap이 크다고 GC가 항상 느린 것도, allocation이 많다고 항상 문제인 것도 아니다. collector가 scan해야 하는 live object와 allocation pressure가 pause/concurrent CPU cost를 결정한다.

latency-sensitive path에서 large allocation burst를 줄이고 object lifetime을 짧고 예측 가능하게 만든다. GC tuning 전에 leak과 unnecessary allocation을 구분한다.

---

## CHAPTER 18 · memory performance는 capacity뿐 아니라 bandwidth와 locality다

RAM 사용량이 limit 아래여도 random large working set은 cache/TLB miss와 memory bandwidth saturation을 만들 수 있다. memory-bound workload는 CPU clock을 올려도 성능이 거의 늘지 않을 수 있다.

profile과 hardware counter로 bytes moved, cache miss, bandwidth를 본다. data layout과 access order가 algorithmic complexity가 같은 code의 실제 성능을 바꾼다.

---

## CHAPTER 19 · compression은 CPU와 I/O/네트워크 비용을 교환한다

compression은 전송/저장 byte를 줄이는 대신 encode/decode CPU와 latency를 추가한다. network가 병목이면 이득이고 CPU가 포화됐으면 손해일 수 있다.

algorithm/level은 compression ratio 하나로 고르지 않는다. payload size distribution, CPU budget, tail latency, battery, cacheability를 benchmark한다.

---

## CHAPTER 20 · prefetch는 미래 access를 맞히면 latency를 숨기고 틀리면 bandwidth를 낭비한다

software/hardware prefetch는 필요한 data를 사용 전에 lower memory/storage에서 가져온다. predictable sequential pattern에서 유리하지만 inaccurate prefetch는 cache pollution과 I/O amplification을 만든다.

remote API prefetch는 더 큰 correctness 비용이 있다. 사용하지 않을 data를 network·DB에서 미리 읽고 stale cache를 만들 수 있다. access probability와 cost를 근거로 적용한다.

---

## CHAPTER 21 · concurrency limit은 system을 saturation 이전 operating point에 묶는다

unlimited parallel request는 throughput을 늘리지 못한 채 queue와 context switching을 증가시킬 수 있다. semaphore/token bucket/worker limit으로 in-flight work를 제한한다.

limit은 downstream capacity와 latency target에 맞춰 조정한다. adaptive concurrency는 latency/queue signal로 limit을 바꿀 수 있지만 oscillation을 막는 control logic이 필요하다.

---

## CHAPTER 22 · connection pool은 DB capacity의 admission controller다

DB connection 수를 늘리면 app-side wait는 줄어도 DB 내부 concurrency와 memory/lock pressure가 증가할 수 있다. pool size는 application thread 수가 아니라 DB가 효율적으로 처리할 concurrent query 수를 기준으로 본다.

connection acquisition wait와 query execution time을 따로 측정한다. leak이 있으면 pool exhaustion이 query slowdown처럼 보일 수 있다.

---

## CHAPTER 23 · lock queue는 critical section service center다

여러 thread가 같은 mutex를 기다리면 lock은 queueing point가 된다. throughput은 critical section service rate에 제한되고 waiter가 늘수록 tail latency가 증가한다.

lock optimization은 spin count 조정보다 shared state를 partition하거나 critical section 밖으로 work를 이동하는 것이 더 효과적일 수 있다. hold-time distribution과 waiter count를 측정한다.

---

## CHAPTER 24 · coordinated omission은 부하생성기가 멈춘 시간을 측정에서 숨긴다

closed-loop load generator가 response를 받은 뒤 다음 request를 보내면 system이 느려질수록 request 생성 자체가 줄어든다. outage 동안 보내졌어야 할 request가 사라져 latency distribution이 실제 사용자 load보다 좋아 보일 수 있다.

open-loop/constant-arrival model이나 omission correction을 사용해 intended arrival schedule을 보존한다. benchmark tool의 load-generation model을 결과와 함께 기록한다.

---

## CHAPTER 25 · load model은 production traffic의 arrival와 mix를 재현해야 한다

평균 QPS 하나로 workload를 재현할 수 없다. burst, diurnal pattern, request type mix, payload size, hot key, cache hit state가 resource usage를 바꾼다.

synthetic benchmark가 production과 다른 workload mix를 사용하면 optimization priority가 틀어진다. anonymized production distribution이나 representative scenario set을 사용한다.

---

## CHAPTER 26 · steady-state 측정은 warmup, cache, JIT, thermal을 통제한다

startup 직후에는 class loading/JIT/cache cold state가 포함되고 장시간 모바일 실행에서는 thermal throttling이 생길 수 있다. 측정 window를 명시하지 않으면 결과가 서로 다른 system state를 비교하게 된다.

warmup 종료 조건과 steady-state 판단 기준을 정의한다. startup 성능이 목적이면 warmup을 제거하지 않고 별도 workload로 측정한다.

---

## CHAPTER 27 · regression gate는 absolute threshold와 relative change를 함께 본다

performance test는 `100ms 이하` 같은 absolute SLO와 baseline 대비 regression을 동시에 볼 수 있다. noise가 큰 환경에서 1% 변화에 build를 실패시키면 flaky gate가 된다.

sample size와 variance를 고려한 threshold를 정하고 hardware/environment를 고정한다. 기능 correctness test와 performance gate의 실패 의미를 구분한다.

---

## CHAPTER 28 · cost optimization은 resource 단가와 engineering complexity를 같이 계산한다

CPU 20% 절감이 cloud cost를 줄여도 개발·운영 complexity가 크게 늘면 전체 경제성은 나빠질 수 있다. performance engineering은 비용/성능/신뢰성의 trade-off다.

cache/CDN/compression/instance type 변경은 request당 cost와 tail latency를 함께 측정한다. 비용 metric도 capacity planning의 입력이다.

---

## CHAPTER 29 · mobile 성능에는 energy와 thermal budget이 포함된다

background CPU, wakeup, network radio, GPS 사용은 battery와 thermal state를 바꾼다. short benchmark에서 빠른 code가 장시간에는 throttling으로 더 느려질 수 있다.

mobile optimization은 frame latency뿐 아니라 energy per task, wakeup frequency, background execution을 본다. 지속 작업은 batching으로 radio/CPU wakeup을 줄일 수 있다.

---

## CHAPTER 30 · capacity planning은 peak와 failure scenario를 함께 모델링한다

정상 peak QPS만 처리 가능한 system은 replica 하나가 죽거나 dependency latency가 늘면 즉시 saturation된다. N+1 failure, deploy overlap, retry surge를 포함한 headroom을 둔다.

capacity forecast는 traffic growth와 resource-per-request trend를 함께 본다. application feature가 request당 DB query 수를 늘리면 QPS가 같아도 capacity 요구가 증가한다.

---

## CHAPTER 31 · priority는 critical work가 overload 속에서도 진행되게 한다

모든 request가 같은 queue를 공유하면 low-value bulk work가 interactive/control-plane 작업을 막을 수 있다. priority queue, reserved capacity, bulkhead로 critical class를 보호한다.

priority는 starvation 위험을 만든다. aging, quota, separate pool을 사용해 low-priority work도 bounded progress를 갖게 한다. 최종 목표는 최대 throughput이 아니라 **과부하에서도 중요한 invariant와 SLO를 유지하는 것**이다.