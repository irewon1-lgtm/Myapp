# PART 10 · 성능 공학 — queueing, saturation, tail, capacity

성능 문제는 `느리다`가 아니라 **service time, wait time, concurrency, arrival rate, utilization, tail distribution**의 관계다. 최적화는 code tweak가 아니라 어느 자원이 saturation되어 queue를 만들고 있는지 증명하는 작업이다.

---

## CHAPTER 01 · latency와 throughput은 독립적으로 측정한다

latency는 개별 operation의 완료 시간이고 throughput은 단위 시간당 완료량이다. batching이나 concurrency를 늘리면 throughput이 증가하면서 개별 latency가 악화될 수 있다.

성능 목표는 둘 중 하나만 최대화하지 않는다. interactive system은 tail latency constraint 안에서 필요한 throughput을 유지해야 하고 batch system은 deadline 안에서 총 work를 완료해야 한다.

load test에서는 concurrency를 한 값에 고정하지 말고 단계적으로 올려 throughput-latency curve를 그린다. 낮은 부하에서 latency가 거의 일정하다가 특정 지점 이후 queueing으로 p99가 급증하고 throughput 증가가 둔화되는 saturation knee를 찾을 수 있다. client-side queue와 server-side queue를 합치면 apparent latency가 왜 늘었는지 알기 어려우므로 send 예정 시각, actual send, server start, completion을 분리한다. 결과에는 offered load와 achieved throughput을 모두 기록해 overload에서 client가 요청을 덜 보낸 현상을 capacity 향상으로 오해하지 않는다.

---

## CHAPTER 02 · service time과 wait time을 분리해야 원인이 보인다

request latency는 resource가 실제로 작업한 service time과 queue/scheduler/lock에서 기다린 wait time의 합으로 볼 수 있다. CPU profile이 짧은데 wall time이 길다면 wait component가 크다는 신호다.

각 stage에 enqueue/dequeue/start/end timestamp를 두면 queue delay와 execution cost를 분리할 수 있다. service time을 줄일지 admission/parallelism을 바꿀지 결정이 달라진다.

stage decomposition은 중첩 시간을 단순 합산하지 않도록 주의한다. parallel fan-out 두 branch의 service time을 더하면 wall critical path보다 커질 수 있고, async operation은 caller가 다른 work를 수행하는 동안 진행될 수 있다. request timeline에서 각 span의 queue wait, active service, blocked dependency를 분류하고 parent-child relation으로 critical path를 계산한다. regression 전후 service time은 그대로인데 wait만 늘었다면 code micro-optimization보다 capacity/admission/lock 문제를 먼저 본다. 반대로 queue가 비어 있는데 service 자체가 느려졌다면 resource-level profile로 내려간다.

---

## CHAPTER 03 · utilization이 100%에 가까워질수록 queueing delay는 비선형적으로 커진다

평균 arrival rate가 평균 service capacity에 근접하면 작은 burst나 service-time 변동도 queue를 만든다. utilization 50%에서 안정적인 system이 90%에서 같은 비율로 느려지는 것이 아니다.

capacity planning은 평균 utilization만 보지 않고 burstiness와 failure headroom을 포함한다. 정상 peak에서도 항상 일부 여유가 있어야 replica loss나 retry surge를 흡수할 수 있다.

utilization은 resource별로 정의가 다르다. host CPU 60%여도 single event-loop thread가 100%일 수 있고, DB connection pool은 active connection 수가 상한에 붙어 saturation될 수 있다. storage는 device busy와 queue depth를 같이 봐야 한다. interval 평균이 70%여도 1초 burst가 매번 queue를 만들면 user tail은 나빠질 수 있으므로 짧은 window와 high-water mark를 확인한다. failure scenario에서는 replica 하나를 제거한 뒤 남은 capacity의 utilization과 p99가 어떻게 변하는지 측정해 운영 headroom을 수치화한다.

---

## CHAPTER 04 · Little’s Law는 in-flight work, throughput, residence time을 연결한다

steady-state에서 평균 system 내 작업 수 `L`, throughput `λ`, 평균 residence time `W`는 `L = λW` 관계를 갖는다. 이는 queue 구현을 몰라도 concurrency와 latency가 일관적인지 검산하는 데 유용하다.

예상 throughput과 latency로 필요한 in-flight 수를 추정하고 실제 connection/thread/queue 수와 비교한다. 측정이 관계와 크게 어긋나면 window 정의나 dropped work, non-steady state를 재검토한다.

이 관계는 steady-state average에 대한 것이므로 startup burst나 queue가 계속 증가하는 overload 구간에 기계적으로 적용하지 않는다. system boundary도 명확히 정해야 한다. client queue를 포함한 L/W와 server 내부만 포함한 L/W는 값이 다르다. completed throughput 대신 offered rate를 넣으면 rejected/dropped request 때문에 식이 어긋날 수 있다. 관측한 in-flight count와 완료율, residence time을 동일 시간 window에서 계산해 cross-check하면 instrumentation bug나 누락된 queue를 찾을 수 있다. 특히 예상보다 L이 크면 hidden retry나 background work가 포함됐는지 본다.

---

## CHAPTER 05 · queue는 overload를 저장할 뿐 capacity를 만들지 않는다

queue는 producer와 consumer 속도 차이를 일시적으로 흡수하지만 장기 arrival rate가 service rate보다 크면 길이는 계속 증가한다. unbounded queue는 즉시 실패를 tail latency와 memory consumption으로 바꾼다.

queue metric에는 depth뿐 아니라 oldest-item age를 포함한다. depth가 일정해도 오래된 work가 남아 있으면 starvation/priority 문제가 있을 수 있다.

queue를 여러 단계에 두면 total backlog가 분산돼 각 queue가 작아 보여도 end-to-end wait가 커질 수 있다. client retry queue, load balancer, app executor, DB pool을 합쳐 operation이 머무는 모든 waiting point를 inventory한다. depth와 age를 priority/class별로 분리하면 신규 high-priority work가 계속 앞서가 old low-priority item이 굶는 상황을 찾을 수 있다. queue capacity를 늘린 뒤 throughput이 그대로인데 p99와 memory만 증가한다면 overload 저장 시간이 늘어난 것뿐이다. overload test에서 arrival을 capacity 위로 올려 backlog slope와 recovery drain time을 측정한다.

---

## CHAPTER 06 · bounded queue는 admission control의 일부다

queue capacity를 제한하면 full 시 block, reject, drop, shed 중 정책을 선택해야 한다. 이 결정은 데이터의 손실 허용성과 upstream retry behavior에 따라 달라진다.

reject를 실패로만 보지 않는다. downstream을 보호해 전체 system collapse를 막는 safety mechanism일 수 있다. caller가 retry한다면 retry budget과 backoff를 함께 설계한다.

admission 결과는 caller가 구분할 수 있어야 한다. invalid request와 overload reject가 같은 error로 보이면 client가 영구 오류를 retry하거나 transient overload를 즉시 반복할 수 있다. queue slot을 tenant별로 분리하거나 reserved capacity를 두면 한 noisy producer가 전체 capacity를 소진하는 것을 막을 수 있다. block policy는 upstream thread/socket을 묶어 다른 request까지 전파되는 backpressure를 만들므로 bounded wait deadline이 필요하다. load test에서는 reject가 시작되는 utilization과 downstream p99가 안정적으로 유지되는지 확인해 queue limit가 실제 보호장치인지 검증한다.

---

## CHAPTER 07 · fan-out은 tail latency를 증폭한다

한 request가 병렬로 여러 dependency를 호출하고 모두 기다리면 전체 latency는 가장 느린 branch의 영향을 받는다. dependency 하나의 p99가 작아 보여도 fan-out 수가 많으면 최소 한 branch가 느릴 확률이 커진다.

fan-out 최적화는 평균 dependency latency만 줄이는 것이 아니라 request count, hedge policy, quorum, cache 사용을 포함한다. 불필요한 synchronous dependency를 critical path에서 제거한다.

independent branch가 각각 1% 확률로 느리다면 많은 branch를 모두 기다리는 request의 slow 확률은 단일 branch보다 훨씬 커진다. 실제로는 branch latency가 공통 backend나 network congestion 때문에 상관될 수 있어 단순 독립 확률보다 더 나쁠 수 있다. trace에서 fan-out width와 slowest-child contribution을 기록하고, partial result/quorum이 허용되는지 business contract를 검토한다. hedge request는 tail을 줄일 수 있지만 duplicate load를 늘리므로 trigger percentile, cancel-on-first-success, retry budget을 함께 설계하고 capacity failure 때 amplification이 커지지 않는지 실험한다.

---

## CHAPTER 08 · deadline은 전체 request budget을 downstream에 배분한다

각 layer가 독립적인 5초 timeout을 사용하면 chain 전체는 훨씬 오래 걸릴 수 있다. end-to-end deadline을 전달하고 남은 budget에 맞춰 downstream timeout을 정한다.

timeout은 성공 가능성이 없는 작업을 빨리 중단해 resource를 회수해야 한다. 이미 deadline을 넘긴 request가 DB/worker queue에서 계속 실행되면 useful throughput을 잠식한다.

deadline에는 예상 network/serialization overhead와 caller가 reply를 처리할 시간까지 남겨야 한다. downstream timeout을 남은 budget과 똑같이 잡으면 reply가 돌아왔을 때 이미 caller deadline이 끝날 수 있다. queue에 들어가기 전에 남은 budget이 최소 service time보다 작으면 실행하지 않고 즉시 reject하는 admission도 가능하다. deadline propagation이 끊기는 async queue에서는 enqueue 시 absolute expiry를 durable metadata로 저장한다. trace에는 initial deadline, 각 hop의 remaining budget, timeout cause를 남겨 어느 계층이 budget을 과도하게 소비했는지 확인한다.

---

## CHAPTER 09 · retry는 failure traffic을 load amplifier로 바꿀 수 있다

error rate가 오를 때 client가 즉시 retry하면 original traffic 위에 추가 traffic이 생긴다. 여러 layer가 각각 retry하면 attempt 수가 곱셈으로 증가할 수 있다.

retry는 transient failure에만 제한하고 idempotency, maximum attempts, retry budget을 둔다. capacity가 부족한 overload에는 retry보다 load shedding이 맞을 수 있다.

operation-level 성공률과 attempt-level error rate를 분리하면 retry가 문제를 가리고 있는지 알 수 있다. user request 100개가 100개 성공해도 내부 attempt가 180개라면 dependency에는 1.8배 load가 간다. retry budget은 일정 window에서 original traffic 대비 추가 attempt 비율을 제한하고, circuit/open overload signal에서는 retry를 줄인다. write operation은 idempotency key나 status lookup 없이는 timeout 후 재시도가 duplicate side effect를 만들 수 있다. chaos test에서 dependency failure율을 단계적으로 올리며 attempt amplification과 queue depth가 runaway하지 않는지 확인한다.

---

## CHAPTER 10 · exponential backoff와 jitter는 synchronized retry wave를 줄인다

모든 client가 동일한 fixed delay로 retry하면 outage 후 같은 시점에 다시 몰리는 thundering herd가 생긴다. exponential backoff는 attempt 간격을 늘리고 jitter는 retry time을 분산한다.

backoff 상한과 total deadline을 함께 둔다. 무한 backoff는 user-visible failure를 늦출 뿐이다. server가 Retry-After 같은 signal을 제공하면 client policy와 결합한다.

jitter도 구현 방식에 따라 분포가 다르다. 모든 client가 동일 seed나 deterministic schedule을 쓰면 randomization 효과가 사라질 수 있다. server hint가 있더라도 client별 작은 분산을 더해 경계 시각 집중을 피할 수 있다. backoff가 길어질수록 남은 end-to-end deadline보다 다음 attempt가 늦어질 수 있으므로 retry scheduler가 expiry 전에 중단해야 한다. recovery experiment에서는 outage 종료 직후 incoming QPS가 정상 capacity를 얼마나 초과하는지 측정하고, jitter 정책 변경 전후 herd peak와 recovery time을 비교한다.

---

## CHAPTER 11 · Amdahl’s Law는 최적화 가능한 부분의 상한을 보여 준다

전체 시간 중 fraction `p`만 speedup `s`배 개선하면 전체 speedup은 나머지 serial fraction에 제한된다. 10% 구간을 10배 빠르게 해도 전체 개선은 제한적이다.

profile에서 hotspot 비중을 확인하지 않고 작은 함수 micro-optimization에 투자하면 전체 latency가 거의 변하지 않는다. critical path 비중이 optimization priority를 정한다.

fraction은 CPU profile 비율이 아니라 목표 metric의 critical-path 비중으로 정의해야 한다. request가 대부분 DB wait인데 CPU 함수가 CPU sample의 40%를 차지해도 그 함수를 2배 빠르게 한 end-to-end 효과는 작을 수 있다. 반대로 startup critical path의 serial initializer는 전체 CPU 비중이 낮아도 first-frame latency에 직접 영향을 준다. 예상 speedup을 변경 전에 계산하고 측정 결과가 크게 다르면 parallel overlap, queueing, bottleneck migration 같은 가정을 재검토한다. 한 구간을 최적화한 뒤 새 bottleneck이 생기는 것도 정상적인 결과다.

---

## CHAPTER 12 · parallelism은 dependency와 shared bottleneck에 제한된다

work를 여러 thread/core로 나눠도 serial dependency, shared memory bandwidth, lock, storage queue가 병목이면 speedup이 제한된다. worker 수를 늘리는 것과 실제 parallel service capacity는 같지 않다.

parallel overhead에는 task partition, synchronization, cache coherence, merge가 포함된다. 작은 task를 지나치게 분할하면 overhead가 useful work보다 커질 수 있다.

worker count sweep을 수행해 throughput, per-task latency, context switch, bandwidth, lock wait를 함께 기록하면 scaling ceiling을 찾을 수 있다. 4→8 worker에서 CPU utilization은 늘지만 throughput이 그대로라면 shared bottleneck을 의심한다. memory-bound loop는 bandwidth roof에 닿고, DB workload는 connection/lock capacity에 닿을 수 있다. partition key가 skewed하면 일부 worker만 과부하돼 전체 completion이 느려질 수 있으므로 load balance도 측정한다. parallel reduction/merge 단계가 serial tail을 만드는지 trace로 확인하고 task grain size를 조절한다.

---

## CHAPTER 13 · scale-up과 scale-out은 state와 failure model을 바꾼다

larger instance는 single-node resource를 늘리지만 failure domain도 커질 수 있다. scale-out은 concurrency capacity를 늘리지만 load balancing, distributed state, coordination이 필요하다.

stateless request는 scale-out이 쉬운 반면 shared DB가 bottleneck이면 app replica만 늘려도 전체 capacity가 증가하지 않는다. end-to-end bottleneck을 먼저 찾는다.

scale-out 후에는 connection 수, cache miss, leader/shard coordination 같은 downstream demand가 replica 수와 함께 늘 수 있다. load balancer가 sticky session이나 uneven key distribution으로 traffic을 균등하게 보내지 못하면 nominal replica 수보다 usable capacity가 낮다. scale-up은 NUMA topology나 GC heap size처럼 성능 특성을 바꿀 수 있어 단순 core 비례를 가정하지 않는다. 실험에서는 replica/instance size별 end-to-end throughput curve와 dependency utilization을 같이 측정하고 한 replica failure 시 재분배 후 p99가 SLO를 지키는지 확인한다.

---

## CHAPTER 14 · batching은 fixed overhead를 amortize하지만 queue delay를 추가한다

여러 item을 한 request/transaction/I/O에 묶으면 per-operation overhead를 줄일 수 있다. 그러나 batch를 채우기 위해 기다리는 시간이 interactive latency를 늘린다.

batch size는 throughput curve와 latency SLO로 결정한다. 최대 batch를 항상 채우는 대신 size 또는 deadline 중 먼저 도달한 조건으로 flush하는 방식이 흔하다.

batch 효율은 item size와 service cost가 균일하지 않을 때 달라진다. 큰 item 하나가 batch processing을 지연시키거나 payload limit을 넘길 수 있으므로 byte limit와 count limit을 함께 둘 수 있다. partial failure가 있을 때 batch 전체 retry가 중복 side effect를 만들지 않도록 item-level result와 idempotency를 설계한다. low traffic에서는 size threshold만 쓰면 무한 대기가 가능하므로 max wait timer가 필요하다. load test에서 batch fill ratio, wait-to-fill, service time, payload bytes, p99를 함께 측정해 throughput 이득이 어디서 생기는지 확인한다.

---

## CHAPTER 15 · cache는 faster lookup과 invalidation cost를 교환한다

cache hit은 expensive computation/I/O를 피하지만 miss path, fill, eviction, memory footprint 비용이 있다. hit ratio가 높아도 가장 비싼 object가 miss하면 latency benefit이 작을 수 있다.

cache key cardinality, object size, admission policy를 함께 본다. 모든 데이터를 cache하면 working set이 커져 오히려 lower-level cache와 memory pressure를 악화시킬 수 있다.

단순 hit ratio 대신 saved-cost weighted hit ratio를 보면 어떤 key가 실제 backend work를 줄였는지 알 수 있다. 작은 값 99%를 hit하고 아주 비싼 query 1%가 계속 miss하면 backend는 여전히 포화될 수 있다. cache fill이 concurrent miss마다 중복 실행되면 stampede가 생기므로 single-flight/lease 같은 coordination을 고려한다. eviction 이유와 entry age를 기록해 capacity miss와 TTL expiry를 구분하고, cache memory가 GC/reclaim을 압박해 전체 latency를 악화시키지 않는지 RSS와 lower-level miss를 같이 본다.

---

## CHAPTER 16 · cache correctness는 freshness와 source-of-truth contract다

TTL만으로 consistency requirement가 해결되지 않는다. write-through, write-behind, explicit invalidation, version key마다 failure mode가 다르다.

stale data가 허용되는 최대 시간, read-after-write 필요성, invalidate failure 시 행동을 문서화한다. cache outage가 source DB overload로 전파되는 cache stampede도 대비한다.

versioned key를 사용하면 old entry를 지우지 않아도 새 version을 읽게 만들 수 있지만 version source가 원자적으로 갱신돼야 한다. invalidate message가 유실될 수 있는 구조에서는 TTL이나 version check 같은 secondary convergence mechanism이 필요하다. write-behind는 cache 성공 후 source write 실패 시 durable truth가 갈라질 수 있으므로 queue durability와 replay semantics를 정의한다. correctness test는 write 직후 read, concurrent writer, invalidation loss, cache restart, source outage를 조합해 허용된 stale window와 금지된 state가 실제로 지켜지는지 검사한다.

---

## CHAPTER 17 · GC performance는 allocation rate와 live set 모두의 함수다

heap이 크다고 GC가 항상 느린 것도, allocation이 많다고 항상 문제인 것도 아니다. collector가 scan해야 하는 live object와 allocation pressure가 pause/concurrent CPU cost를 결정한다.

latency-sensitive path에서 large allocation burst를 줄이고 object lifetime을 짧고 예측 가능하게 만든다. GC tuning 전에 leak과 unnecessary allocation을 구분한다.

collector metric에서는 allocation bytes/sec, live-after-GC, pause phase, concurrent CPU, promotion/compaction을 같이 본다. heap을 크게 늘리면 collection 빈도는 줄 수 있지만 live graph scan과 memory footprint가 커질 수 있다. object pooling은 allocation을 줄이는 대신 retained set을 늘려 반대 효과를 낼 수도 있다. request p99와 GC event를 correlation해 실제 critical request가 pause를 겪었는지 확인하고, tuning 전후에는 latency뿐 아니라 total CPU와 RSS를 비교한다. native memory pressure가 GC trigger와 상호작용하는 runtime도 있어 process-level memory도 함께 본다.

---

## CHAPTER 18 · memory performance는 capacity뿐 아니라 bandwidth와 locality다

RAM 사용량이 limit 아래여도 random large working set은 cache/TLB miss와 memory bandwidth saturation을 만들 수 있다. memory-bound workload는 CPU clock을 올려도 성능이 거의 늘지 않을 수 있다.

profile과 hardware counter로 bytes moved, cache miss, bandwidth를 본다. data layout과 access order가 algorithmic complexity가 같은 code의 실제 성능을 바꾼다.

NUMA system에서는 memory가 어느 node에 배치됐는지와 thread가 어느 CPU에서 실행되는지가 latency/bandwidth에 영향을 준다. remote access 비율과 page migration을 관찰하고 thread affinity 변경이 성능을 바꾸는지 본다. structure-of-arrays와 array-of-structures는 필요한 field density와 vectorization에 따라 cache line 효율이 달라질 수 있다. bandwidth counter가 최대 근처인데 IPC가 낮다면 더 많은 core를 추가해도 throughput이 늘지 않을 가능성이 크다. working-set sweep으로 cache capacity 경계를 찾아 알고리즘과 data layout 최적화의 효과를 검증한다.

---

## CHAPTER 19 · compression은 CPU와 I/O/네트워크 비용을 교환한다

compression은 전송/저장 byte를 줄이는 대신 encode/decode CPU와 latency를 추가한다. network가 병목이면 이득이고 CPU가 포화됐으면 손해일 수 있다.

algorithm/level은 compression ratio 하나로 고르지 않는다. payload size distribution, CPU budget, tail latency, battery, cacheability를 benchmark한다.

break-even은 `절약된 전송시간 > 압축+해제 시간` 같은 단순 비교에서 시작할 수 있지만 queueing과 concurrency도 포함해야 한다. CPU가 saturation 근처면 작은 encode cost가 큰 wait 증가를 만들 수 있고, network가 느린 mobile 환경에서는 높은 ratio가 유리할 수 있다. 이미 압축된 image/video 같은 payload는 ratio 이득이 거의 없어 CPU만 낭비한다. size threshold와 content type별 policy를 두고 end-to-end p50/p99, bytes sent, CPU-ms, energy를 함께 측정한다. server/client algorithm compatibility와 dictionary/version rollout도 운영 contract에 포함한다.

---

## CHAPTER 20 · prefetch는 미래 access를 맞히면 latency를 숨기고 틀리면 bandwidth를 낭비한다

software/hardware prefetch는 필요한 data를 사용 전에 lower memory/storage에서 가져온다. predictable sequential pattern에서 유리하지만 inaccurate prefetch는 cache pollution과 I/O amplification을 만든다.

remote API prefetch는 더 큰 correctness 비용이 있다. 사용하지 않을 data를 network·DB에서 미리 읽고 stale cache를 만들 수 있다. access probability와 cost를 근거로 적용한다.

prefetch 품질은 issued, useful, late, unused byte/request로 측정할 수 있다. 너무 가까우면 data가 필요해진 뒤 도착해 latency를 못 숨기고, 너무 멀면 eviction되거나 사용되지 않은 채 bandwidth를 점유한다. remote prefetch는 user가 다음 화면으로 가지 않아도 개인정보나 quota를 소비할 수 있으므로 permission과 cancellation도 고려한다. bandwidth-limited workload에서는 prefetch가 demand request를 밀어내지 않도록 priority를 낮춘다. feature flag로 prefetch on/off cohort를 비교할 때 cache warmness와 traffic amplification을 함께 기록한다.

---

## CHAPTER 21 · concurrency limit은 system을 saturation 이전 operating point에 묶는다

unlimited parallel request는 throughput을 늘리지 못한 채 queue와 context switching을 증가시킬 수 있다. semaphore/token bucket/worker limit으로 in-flight work를 제한한다.

limit은 downstream capacity와 latency target에 맞춰 조정한다. adaptive concurrency는 latency/queue signal로 limit을 바꿀 수 있지만 oscillation을 막는 control logic이 필요하다.

fixed limit를 찾을 때 concurrency sweep으로 throughput knee와 p99를 측정하고 safety margin을 둔다. adaptive controller는 짧은 latency spike에 과도하게 limit를 낮추거나 recovery에서 너무 빨리 올려 oscillation하지 않도록 smoothing/hysteresis가 필요하다. request class별 service cost가 다르면 단순 count limit보다 weighted token이 적합할 수 있다. limit에 막혀 기다린 admission wait도 end-to-end latency에 포함하고, deadline이 임박한 request는 queue에 넣지 않는다. controller 변경 후 useful throughput과 reject rate가 모두 안정적인지 failure load에서 검증한다.

---

## CHAPTER 22 · connection pool은 DB capacity의 admission controller다

DB connection 수를 늘리면 app-side wait는 줄어도 DB 내부 concurrency와 memory/lock pressure가 증가할 수 있다. pool size는 application thread 수가 아니라 DB가 효율적으로 처리할 concurrent query 수를 기준으로 본다.

connection acquisition wait와 query execution time을 따로 측정한다. leak이 있으면 pool exhaustion이 query slowdown처럼 보일 수 있다.

pool size sweep에서는 acquire p99, active/idle count, DB CPU, lock wait, query p99를 함께 본다. pool을 늘려 acquire wait가 사라졌지만 DB query가 느려지면 bottleneck을 아래로 이동시킨 것이다. transaction 동안 connection을 오래 보유하거나 remote call을 끼우면 useful query time보다 lease time이 길어져 effective capacity가 줄어든다. connection leak은 oldest lease와 checkout stack을 추적하고, request cancellation에서 반환이 보장되는지 fault injection으로 확인한다. replica별 pool 합이 DB max connection/headroom을 넘지 않도록 배포 단위 계산도 필요하다.

---

## CHAPTER 23 · lock queue는 critical section service center다

여러 thread가 같은 mutex를 기다리면 lock은 queueing point가 된다. throughput은 critical section service rate에 제한되고 waiter가 늘수록 tail latency가 증가한다.

lock optimization은 spin count 조정보다 shared state를 partition하거나 critical section 밖으로 work를 이동하는 것이 더 효과적일 수 있다. hold-time distribution과 waiter count를 측정한다.

lock holder가 preempt되거나 page fault를 겪으면 critical section code가 짧아도 waiter tail이 길어지는 convoy가 생길 수 있다. acquisition trace에 owner CPU state와 hold duration을 연결한다. fairness 정책은 starvation을 줄이지만 throughput/cache locality와 trade-off가 있을 수 있다. shared counter를 sharding한 뒤 merge하거나 immutable snapshot을 publish해 write ownership 자체를 줄이는 설계를 우선 검토한다. 변경 전후 lock wait p50/p99, acquisitions/sec, cache-to-cache traffic, overall throughput을 비교해 lock primitive 교체가 아니라 contention 원인이 실제로 줄었는지 확인한다.

---

## CHAPTER 24 · coordinated omission은 부하생성기가 멈춘 시간을 측정에서 숨긴다

closed-loop load generator가 response를 받은 뒤 다음 request를 보내면 system이 느려질수록 request 생성 자체가 줄어든다. outage 동안 보내졌어야 할 request가 사라져 latency distribution이 실제 사용자 load보다 좋아 보일 수 있다.

open-loop/constant-arrival model이나 omission correction을 사용해 intended arrival schedule을 보존한다. benchmark tool의 load-generation model을 결과와 함께 기록한다.

open-loop generator도 자체 CPU/network가 포화되면 예정 시각에 요청을 보내지 못할 수 있으므로 scheduled-send와 actual-send lag를 계측한다. backlog가 client generator 내부에 쌓였다면 server latency만 보고 capacity를 평가하면 안 된다. production이 실제로 user think-time을 가진 closed-loop workload인지 machine-generated fixed-arrival인지 모델을 맞춘다. pause injection으로 server를 잠시 멈춘 뒤 load tool이 outage 동안 expected requests를 어떻게 처리하는지 확인하면 omission 여부를 검증할 수 있다. percentile report에는 offered load와 generator lag를 같이 표시한다.

---

## CHAPTER 25 · load model은 production traffic의 arrival와 mix를 재현해야 한다

평균 QPS 하나로 workload를 재현할 수 없다. burst, diurnal pattern, request type mix, payload size, hot key, cache hit state가 resource usage를 바꾼다.

synthetic benchmark가 production과 다른 workload mix를 사용하면 optimization priority가 틀어진다. anonymized production distribution이나 representative scenario set을 사용한다.

workload model에는 key popularity/skew, read-write ratio, fan-out width, authentication/cache state, payload distribution을 포함한다. 평균 payload 10KB라도 일부 5MB request가 memory/GC/network tail을 지배할 수 있다. hot key가 실제로 lock/partition contention을 만든다면 uniform random key benchmark는 병목을 숨긴다. production trace에서 개인정보를 제거한 distribution을 재생하거나 scenario weight를 명시하고, release 비교에서는 같은 seed/dataset을 사용한다. traffic growth뿐 아니라 mix 변화가 resource-per-request를 바꾸는지 분기별 capacity model에 반영한다.

---

## CHAPTER 26 · steady-state 측정은 warmup, cache, JIT, thermal을 통제한다

startup 직후에는 class loading/JIT/cache cold state가 포함되고 장시간 모바일 실행에서는 thermal throttling이 생길 수 있다. 측정 window를 명시하지 않으면 결과가 서로 다른 system state를 비교하게 된다.

warmup 종료 조건과 steady-state 판단 기준을 정의한다. startup 성능이 목적이면 warmup을 제거하지 않고 별도 workload로 측정한다.

steady-state 판단은 임의의 30초 대기보다 throughput, latency, compilation/GC, temperature 같은 signal이 일정 범위에서 안정되는 조건으로 정의하는 편이 낫다. cache warmup이 실제 production cold-miss 비율보다 과도해지지 않도록 workload 목적을 분리한다. long benchmark에서는 thermal/frequency residency가 변하면 구간을 나눠 보고하고, DB/storage는 background compaction/GC가 주기적으로 나타날 수 있어 충분한 duration을 잡는다. 전후 실험은 동일 warmup recipe와 measurement window를 사용하고 warmup 자체의 비용도 user-facing startup이라면 별도 metric으로 보존한다.

---

## CHAPTER 27 · regression gate는 absolute threshold와 relative change를 함께 본다

performance test는 `100ms 이하` 같은 absolute SLO와 baseline 대비 regression을 동시에 볼 수 있다. noise가 큰 환경에서 1% 변화에 build를 실패시키면 flaky gate가 된다.

sample size와 variance를 고려한 threshold를 정하고 hardware/environment를 고정한다. 기능 correctness test와 performance gate의 실패 의미를 구분한다.

baseline은 하나의 과거 숫자가 아니라 동일 환경에서 얻은 distribution과 metadata로 관리한다. hardware frequency, background load, OS update가 바뀌면 code와 무관한 drift가 생길 수 있다. gate는 큰 absolute SLO 위반에는 즉시 실패하고 작은 relative change는 반복 sample이나 confidence interval을 요구하는 식으로 위험에 맞출 수 있다. benchmark 자체의 coefficient of variation과 historical false-fail rate를 추적해 threshold가 신호보다 noise를 재는지 확인한다. 실패 시 raw sample과 environment fingerprint를 보존해 재실행 가능한 증거를 남긴다.

---

## CHAPTER 28 · cost optimization은 resource 단가와 engineering complexity를 같이 계산한다

CPU 20% 절감이 cloud cost를 줄여도 개발·운영 complexity가 크게 늘면 전체 경제성은 나빠질 수 있다. performance engineering은 비용/성능/신뢰성의 trade-off다.

cache/CDN/compression/instance type 변경은 request당 cost와 tail latency를 함께 측정한다. 비용 metric도 capacity planning의 입력이다.

TCO에는 compute/storage/egress 같은 직접 비용뿐 아니라 운영 on-call, migration, vendor lock-in, failure recovery complexity를 포함한다. instance를 더 작게 만들어 utilization을 올리면 단가는 줄어도 failure headroom이 사라져 SLO 비용이 커질 수 있다. optimization 전후 cost per successful request, p99, error rate, engineering maintenance를 같은 horizon에서 비교한다. reserved/spot 같은 가격 모델을 사용할 때도 interruption/rebalance가 capacity와 retry에 미치는 영향을 반영한다. 단가 절감이 user latency나 recovery objective를 훼손하면 별도 trade-off로 명시한다.

---

## CHAPTER 29 · mobile 성능에는 energy와 thermal budget이 포함된다

background CPU, wakeup, network radio, GPS 사용은 battery와 thermal state를 바꾼다. short benchmark에서 빠른 code가 장시간에는 throttling으로 더 느려질 수 있다.

mobile optimization은 frame latency뿐 아니라 energy per task, wakeup frequency, background execution을 본다. 지속 작업은 batching으로 radio/CPU wakeup을 줄일 수 있다.

energy는 CPU time만으로 추정하지 않고 radio active tail, screen/GPU, sensor, wake lock처럼 component별 cost를 고려한다. network request를 조금 줄여도 radio wakeup을 batch하면 energy가 크게 줄 수 있고, 반대로 compression CPU가 늘어 total energy가 악화될 수 있다. 실험에서는 battery level/charging, thermal state, network type, brightness 같은 조건을 고정하고 장시간 반복에서 frequency throttling과 frame p99를 함께 측정한다. background task가 OS scheduler에 의해 지연되는 것을 성능 실패로 볼지 battery 정책으로 볼지도 product deadline과 함께 정의한다.

---

## CHAPTER 30 · capacity planning은 peak와 failure scenario를 함께 모델링한다

정상 peak QPS만 처리 가능한 system은 replica 하나가 죽거나 dependency latency가 늘면 즉시 saturation된다. N+1 failure, deploy overlap, retry surge를 포함한 headroom을 둔다.

capacity forecast는 traffic growth와 resource-per-request trend를 함께 본다. application feature가 request당 DB query 수를 늘리면 QPS가 같아도 capacity 요구가 증가한다.

model은 `capacity = replica 수 × per-replica sustainable throughput` 같은 출발점에 failover efficiency, load imbalance, shared dependency ceiling을 반영한다. deploy 중 old/new replica가 동시에 떠 memory/connection이 늘거나 cache가 cold해지는 조건도 peak와 겹칠 수 있다. fault load test에서 replica를 제거하고 dependency latency를 인위적으로 늘려 headroom 가정을 검증한다. forecast에는 traffic growth뿐 아니라 feature mix와 retry/cache hit 변화로 인한 CPU-ms, DB queries, bytes/request 추세를 넣고, expansion lead time보다 먼저 threshold에 도달하는 resource를 찾는다.

---

## CHAPTER 31 · priority는 critical work가 overload 속에서도 진행되게 한다

모든 request가 같은 queue를 공유하면 low-value bulk work가 interactive/control-plane 작업을 막을 수 있다. priority queue, reserved capacity, bulkhead로 critical class를 보호한다.

priority는 starvation 위험을 만든다. aging, quota, separate pool을 사용해 low-priority work도 bounded progress를 갖게 한다. 최종 목표는 최대 throughput이 아니라 **과부하에서도 중요한 invariant와 SLO를 유지하는 것**이다.

priority를 값 하나로만 두면 high class가 지속되는 동안 low class가 영구 정지할 수 있다. class별 minimum share와 maximum burst, aging rule을 정의하고 control-plane request에는 별도 reserved capacity를 둘 수 있다. priority inversion이 생기면 high-priority task가 low-priority owner의 lock/resource를 기다리므로 queue 순서만 바꿔서는 해결되지 않는다. overload experiment에서 class별 admitted rate, queue age, p99, starvation maximum을 측정하고, critical traffic을 보호하면서 low-priority도 정해진 bounded progress를 유지하는지 검증한다.