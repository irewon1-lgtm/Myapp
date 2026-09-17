# PART 15 · 동시성 심화 — progress, ordering, reclamation, proof

advanced concurrency의 난점은 atomic primitive 사용법이 아니라 **operation이 어느 순간 발생한 것으로 간주되는가, 다른 observer가 어떤 순서를 볼 수 있는가, 제거된 memory를 언제 안전하게 회수할 수 있는가, 경쟁 속에서도 누가 진행을 보장받는가**를 동시에 증명하는 데 있다.

---

## CHAPTER 01 · read-modify-write는 경쟁 상태를 하나의 hardware-visible transition으로 묶는다

shared state를 `load → 계산 → store`로 갱신하면 두 thread가 동일 old value를 읽어 update를 잃을 수 있다. atomic RMW는 read와 conditional/update를 하나의 indivisible synchronization event로 제공해 특정 state transition을 경쟁자와 직렬화할 수 있게 한다.

그러나 RMW 하나가 복합 invariant 전체를 보호하지는 않는다. 두 atomic variable의 개별 update가 모두 원자적이어도 둘 사이 consistency가 필요하면 더 큰 state representation 또는 transaction/lock이 필요하다. atomic primitive 선택 전에 **linearization point와 invariant 범위**를 먼저 정의한다.

---

## CHAPTER 02 · CAS loop는 optimistic state transition이며 retry policy가 algorithm의 일부다

Compare-And-Swap는 현재 값이 expected와 같을 때만 새 값으로 바꾼다. algorithm은 old state를 읽고 candidate를 계산한 뒤 CAS에 실패하면 최신 state에서 다시 계산한다. correctness는 CAS 성공 순간을 operation의 linearization point로 잡아 증명하는 경우가 많다.

contention이 높으면 여러 thread가 동일 cache line을 읽고 CAS 실패를 반복하며 coherence traffic을 폭증시킬 수 있다. backoff, combining, sharding이 필요할 수 있다. CAS retry가 finite progress를 보장하는지, starvation 가능한지까지 progress property에 포함한다.

---

## CHAPTER 03 · LL/SC는 value equality보다 reservation continuity를 이용한다

Load-Linked/Store-Conditional 계열은 linked load 이후 관련 memory state가 변경되지 않았을 때 conditional store가 성공하는 모델을 제공한다. 이는 중간에 값이 A→B→A로 돌아오는 ABA 상황을 CAS와 다르게 감지할 수 있는 architecture semantics를 제공할 수 있다.

실제 LL/SC는 reservation granule, interrupt/context switch, unrelated store에 의해 spurious failure가 발생할 수 있다. algorithm은 conditional store 실패를 정상 retry로 처리해야 하며 forward-progress guarantee는 architecture specification을 확인한다. high-level atomic library가 architecture 차이를 감싸는 이유다.

---

## CHAPTER 04 · lock-free, wait-free, obstruction-free는 서로 다른 progress guarantee다

lock-free는 system 전체가 계속 progress한다는 성질에 가깝고 특정 thread starvation을 허용할 수 있다. wait-free는 각 operation이 자신의 bounded step 안에 완료되는 더 강한 guarantee를 요구한다. obstruction-free는 경쟁 없이 충분히 실행되면 완료될 수 있는 더 약한 성질이다.

progress guarantee는 throughput benchmark 결과가 아니다. scheduler가 adversarial하게 thread를 배치해도 어떤 completion이 보장되는지에 대한 algorithm property다. real-time requirement가 있으면 average throughput보다 per-operation bound와 priority interaction이 중요하다.

---

## CHAPTER 05 · lock-free structure는 blocking을 줄이는 대신 coherence와 proof 비용을 늘릴 수 있다

mutex가 없으면 lock owner preemption에 의한 convoy를 피할 수 있지만 CAS retry, cache-line bouncing, complex reclamation, weak-memory ordering proof가 추가된다. low-contention critical section에서는 optimized mutex가 더 빠르고 훨씬 검증하기 쉬울 수 있다.

선택 기준은 label이 아니라 workload와 failure requirement다. preemption tolerance, contention distribution, operation mix, reader/writer ratio, memory footprint, debugging capability를 비교한다. algorithm complexity가 운영팀의 incident-resolution 능력을 초과하면 theoretical progress가 실제 reliability를 해칠 수 있다.

---

## CHAPTER 06 · ABA는 동일 value가 동일 history를 의미하지 않는 문제다

Thread A가 pointer A를 읽은 뒤 Thread B가 node를 제거하고 다른 node를 같은 address에 재사용하면 value가 다시 A처럼 보일 수 있다. CAS는 expected pointer equality만 확인하므로 중간 topology 변화가 없었다고 오판할 수 있다.

version/tagged pointer는 identity에 generation을 추가하고, reclamation scheme은 old node address가 너무 빨리 재사용되지 않게 한다. ABA mitigation은 pointer bit trick 하나가 아니라 **object lifetime과 identity reuse policy**를 함께 설계하는 문제다.

---

## CHAPTER 07 · logical removal과 physical reclamation은 분리해야 한다

lock-free list에서 node를 unlink했다고 다른 reader가 그 node pointer를 더 이상 갖지 않는다는 뜻은 아니다. reader가 earlier traversal에서 pointer를 보유할 수 있으므로 즉시 free하면 use-after-free가 된다.

algorithm은 node가 abstract data structure에서 제거된 시점과 memory를 재사용할 수 있는 시점을 별도로 정의해야 한다. reclamation proof가 없는 lock-free structure는 memory safety proof가 완료되지 않은 것이다. GC language에서도 native/off-heap node는 같은 문제가 생길 수 있다.

---

## CHAPTER 08 · hazard pointer는 dereference 예정 pointer를 명시적으로 publish한다

reader는 candidate pointer를 hazard slot에 저장하고 pointer가 여전히 structure에 유효한지 재검증한 뒤 dereference한다. reclaimer는 retired node가 어떤 active hazard에도 없을 때만 free한다.

hazard publication은 atomic ordering과 per-thread slot 관리 비용을 가진다. retire list scan이 커지면 reclamation latency가 늘 수 있다. thread exit 시 hazard cleanup과 dynamic thread count를 관리해야 한다. correctness는 `publish→validate→dereference` 순서를 memory model 위에서 증명한다.

---

## CHAPTER 09 · epoch-based reclamation은 active reader generation을 이용한다

reader가 현재 epoch participation을 표시하고 writer가 removed node를 retire list에 넣은 뒤, 모든 relevant reader가 이전 epoch를 벗어나면 해당 node를 reclaim한다. reader hot path가 단순해질 수 있지만 stalled reader 하나가 대량 reclamation을 막을 수 있다.

latency-sensitive system에서는 reclamation backlog와 stalled participant를 모니터링한다. thread pool 재사용이나 coroutine migration과 epoch participation을 혼동하지 않는다. logical task lifetime과 physical thread lifetime 중 어느 것이 safety boundary인지 algorithm에 맞춰 결정한다.

---

## CHAPTER 10 · RCU는 read-side cost를 최소화하고 update/reclaim에 복잡성을 집중한다

Read-Copy-Update는 reader가 old version을 lock 없이 읽는 동안 writer가 new version을 만들고 pointer를 publish한 뒤 grace period 이후 old version을 reclaim하는 계열의 technique다. read-mostly kernel data structure에서 유용하다.

RCU correctness는 publication ordering과 grace-period definition에 달려 있다. `reader가 끝났다`는 의미가 scheduler/context rule과 연결될 수 있다. RCU를 general-purpose mutable container로 쓰지 않는다. update frequency, memory duplication, grace-period latency를 workload와 비교한다.

---

## CHAPTER 11 · seqlock은 reader retry를 허용해 writer serialization을 단순화한다

writer는 sequence counter를 odd/even transition으로 변경하고 data를 갱신한다. reader는 시작/종료 sequence가 동일한 stable value인지 확인해 writer와 겹쳤다면 전체 read를 재시도한다.

reader가 retry 가능하고 data copy가 side effect가 없을 때 적합하다. writer가 빈번하면 reader starvation이 생길 수 있고 pointer lifetime이 바뀌는 structure는 seqlock만으로 memory safety가 해결되지 않는다. snapshot consistency와 reclamation을 별도 증명한다.

---

## CHAPTER 12 · read-write lock은 reader concurrency와 metadata contention을 교환한다

RWLock은 동시에 여러 reader를 허용하고 writer에 exclusive access를 준다. read-heavy workload에서 이득일 수 있지만 reader count 업데이트와 fairness policy 자체가 shared hot state가 된다.

critical section이 매우 짧으면 mutex보다 overhead가 클 수 있고 writer-preference/read-preference에 따라 starvation 특성이 달라진다. benchmark는 평균 reader 비율만이 아니라 burst writer와 tail wait를 포함한다. immutable snapshot/RCU가 더 적합한지 비교한다.

---

## CHAPTER 13 · priority inversion은 lock ownership과 scheduler priority가 충돌할 때 발생한다

low-priority task가 lock을 보유한 상태에서 high-priority task가 기다리고, medium-priority task가 CPU를 계속 사용하면 high-priority task는 간접적으로 medium task보다 뒤로 밀린다. 단순 priority boost만으로 모든 nested-lock case가 해결되지는 않는다.

priority inheritance/ceiling protocol은 real-time system에서 이 문제를 완화한다. 일반 server에서도 critical control thread가 background owner를 기다리는 구조가 latency spike를 만들 수 있다. lock wait와 owner scheduling state를 같은 trace에서 본다.

---

## CHAPTER 14 · lock convoy는 한 owner 지연이 waiter 전체의 throughput을 끌어내린다

high-contention mutex에서 lock owner가 preempt되거나 page fault/I/O에 걸리면 많은 waiter가 줄지어 대기한다. owner가 깨어난 뒤에도 cache line ownership과 scheduler wakeup이 연쇄 비용을 만든다.

critical section 내부 blocking operation을 제거하고 state를 partition한다. fair lock은 starvation을 줄이지만 convoy를 더 강하게 유지할 수 있다. unfair lock이 항상 빠른 것도 아니므로 workload의 fairness SLO와 throughput을 함께 평가한다.

---

## CHAPTER 15 · sharding은 하나의 synchronization domain을 여러 독립 domain으로 분할한다

global map lock 대신 key hash에 따라 shard lock을 사용하면 unrelated key operation이 병렬 진행할 수 있다. 그러나 multi-key operation은 여러 shard를 동시에 잠가야 해 deadlock ordering과 atomicity가 복잡해진다.

shard count가 너무 많으면 metadata와 cache footprint가 커지고 skewed hot key는 여전히 한 shard를 포화시킨다. contention metric을 shard별로 측정하고 dynamic rebalance가 필요한지 판단한다.

---

## CHAPTER 16 · optimistic concurrency는 conflict가 드물다는 가정에 의존한다

reader는 version을 읽고 lock 없이 work를 수행한 뒤 commit 시 version이 바뀌지 않았는지 검증한다. conflict가 있으면 retry/abort한다. MVCC와 compare-version update도 이 사고의 변형이다.

conflict rate가 높으면 wasted work와 retry load가 증가한다. long transaction은 conflict window를 넓힌다. optimistic scheme의 성능은 성공 path cost뿐 아니라 abort probability×retry cost까지 포함해 계산한다.

---

## CHAPTER 17 · linearizability는 operation을 호출-응답 사이 한 순간에 일어난 것처럼 설명할 수 있는가를 묻는다

concurrent history가 sequential specification과 일치하도록 각 operation에 linearization point를 배치할 수 있다면 linearizable하다고 본다. real-time order도 보존해야 하므로 먼저 완료된 operation이 이후 시작된 operation 뒤로 이동할 수 없다.

linearization point는 구현에서 실제 atomic instruction 하나일 수도 있고 복잡한 help mechanism에 의해 다른 thread가 operation을 완료하는 순간일 수도 있다. code review에서 `lock-free니까 linearizable`이라고 가정하지 않고 history를 specification에 mapping해 증명한다.

---

## CHAPTER 18 · consistency model은 observer가 허용받는 history 집합을 정의한다

sequential consistency, causal consistency, eventual consistency는 서로 다른 guarantee를 제공한다. concurrent memory algorithm과 distributed storage 모두 `어떤 관찰 순서가 허용되는가`라는 질문으로 연결된다.

더 약한 consistency는 performance/availability 선택지를 늘릴 수 있지만 application invariant가 견딜 수 있어야 한다. user-facing balance, inventory, feed ordering은 필요한 consistency가 다르다. model 이름보다 실제 anomaly를 example history로 정의한다.

---

## CHAPTER 19 · double-checked locking은 publication ordering이 없으면 incomplete object를 노출할 수 있다

`if null → lock → if null → create` 패턴은 object construction write와 reference publication 사이 ordering이 보장되어야 한다. 언어 memory model이 요구하는 volatile/atomic semantics 없이 구현하면 reader가 non-null reference와 partially initialized fields를 볼 가능성이 있다.

modern language/runtime가 제공하는 lazy initialization primitive를 우선 사용한다. 직접 구현한다면 happens-before를 specification으로 증명한다. timing test 수천 번이 memory-model proof를 대신하지 않는다.

---

## CHAPTER 20 · safe publication은 object lifetime의 시작점을 synchronization event에 연결한다

immutable object도 construction 완료가 reader에게 visible하다는 보장이 필요하다. lock release/acquire, atomic store/load, thread start/join 같은 synchronization edge가 initialized state를 전달한다.

publication 이후 object가 mutate되지 않는다면 reasoning이 크게 단순해진다. mutable shared object는 매 mutation path에 synchronization을 요구한다. architecture는 object ownership transfer를 명시해 accidental shared mutation을 줄인다.

---

## CHAPTER 21 · work stealing은 idle worker가 다른 worker의 deque에서 task를 가져간다

fork-join style workload에서 worker는 local deque를 주로 사용해 locality를 유지하고 idle worker가 다른 deque의 반대쪽에서 steal할 수 있다. task granularity가 너무 작으면 scheduling overhead와 contention이 커지고 너무 크면 load balance가 나빠진다.

blocking task가 compute pool worker를 점유하면 steal로 해결되지 않는 starvation이 발생할 수 있다. CPU-bound와 blocking workload를 분리하거나 managed blocking mechanism을 사용한다. queue depth만이 아니라 runnable task age를 측정한다.

---

## CHAPTER 22 · structured concurrency는 child task lifetime을 lexical/request lifetime에 묶는다

parent scope가 끝날 때 child task의 completion/cancellation이 정리되도록 구조화하면 orphan work와 leaked callback을 줄일 수 있다. error propagation과 cancellation tree가 명시적이 된다.

동시에 실행한다고 모두 같은 cancellation semantics를 가져야 하는 것은 아니다. critical child failure가 sibling 전체를 취소할지 supervisor-style로 격리할지 요구사항에 따라 선택한다. request deadline을 child operation까지 전달한다.

---

## CHAPTER 23 · cancellation은 partially completed side effect를 처리하는 protocol이다

cooperative task가 cancellation을 관찰하는 시점에 file write, DB transaction, remote request가 이미 일부 수행됐을 수 있다. 단순 exception throw로 atomic rollback이 되지 않는다.

operation을 cancellable phase와 non-cancellable commit phase로 나누거나 idempotent compensation을 설계한다. resource cleanup은 cancellation exception에도 실행되어야 하고 cleanup 자체가 cancellation되어 lock/descriptor가 남지 않게 보호한다.

---

## CHAPTER 24 · channel은 memory queue와 backpressure policy를 함께 가진다

message channel은 shared mutable state를 direct access 대신 message ownership으로 바꾸지만 capacity와 ordering semantics가 필요하다. rendezvous, bounded buffer, unbounded buffer는 producer/consumer coupling이 다르다.

channel full 시 suspend/drop/fail 정책이 application correctness를 결정한다. actor mailbox가 무한이면 lock contention 대신 queue latency/memory explosion으로 overload 형태만 바뀐다. message age와 backlog를 운영 metric으로 둔다.

---

## CHAPTER 25 · actor도 cyclic request/reply dependency를 만들면 deadlock할 수 있다

actor가 내부 state를 한 mailbox에서 serialize해 data race를 줄여도, Actor A가 B의 reply를 기다리고 B가 A의 reply를 기다리면 logical deadlock이 생긴다. mailbox processing thread를 synchronous call로 block하면 progress가 멈춘다.

request/reply graph에 deadline과 failure handling을 넣고 cyclic dependency를 피한다. long computation은 actor mailbox를 점유하지 않게 offload하고 result correlation을 사용한다. actor model도 progress proof가 필요하다.

---

## CHAPTER 26 · fairness는 throughput과 tail starvation 사이의 정책 선택이다

strict FIFO lock/scheduler는 기다린 순서를 보장하지만 cache-local owner 재획득 기회를 막아 throughput을 낮출 수 있다. unfair policy는 throughput을 높일 수 있지만 일부 waiter가 매우 오래 기다릴 수 있다.

fairness requirement는 workload class마다 다르다. user request는 bounded latency가 중요하고 background compaction은 더 느슨할 수 있다. 평균 wait만 보지 않고 max/p99 wait와 starvation count를 측정한다.

---

## CHAPTER 27 · spinlock과 mutex 선택은 expected wait와 preemption 가능성에 달려 있다

spinlock은 lock이 곧 풀릴 것으로 기대하고 CPU를 소비하며 기다린다. sleep mutex는 scheduler 전환 비용을 내고 CPU를 양보한다. critical section이 매우 짧고 owner가 현재 다른 core에서 실행 중이면 spin이 유리할 수 있다.

single core나 oversubscribed system에서 owner가 preempt된 상태로 spin하면 CPU를 낭비한다. adaptive mutex는 일정 시간 spin 후 sleep할 수 있다. 선택은 lock hold distribution과 scheduler topology를 기반으로 한다.

---

## CHAPTER 28 · false sharing은 synchronization primitive 밖의 coherence contention이다

각 thread가 별도 counter를 수정해 logical data race가 없어도 같은 cache line에 배치되면 line ownership이 core 사이를 이동한다. lock profiler에는 병목이 보이지 않을 수 있다.

PMU/coherence counter와 scaling curve로 진단하고 per-thread counter, padding/alignment, batch aggregation으로 shared write 빈도를 줄인다. cache line size를 hard-code한 padding은 platform portability를 고려해야 한다.

---

## CHAPTER 29 · contention collapse는 concurrency 증가가 useful throughput을 감소시키는 구간이다

lock retry, context switch, cache bouncing, queue management가 useful work보다 커지면 worker를 더 추가할수록 throughput이 떨어질 수 있다. 이 지점 이후 latency는 급격히 증가한다.

adaptive concurrency limit과 admission control로 operating point를 collapse 이전에 유지한다. `CPU가 100%가 아니니 worker를 더 늘린다`는 결정은 잘못될 수 있다. off-CPU contention과 coherence cost를 함께 본다.

---

## CHAPTER 30 · race detector는 runtime access history로 happens-before 위반 후보를 찾는다

dynamic race detector는 memory access instrumentation과 synchronization event를 추적해 conflicting access 사이 happens-before가 없는 경우를 보고할 수 있다. 실제 실행된 interleaving만 관찰하므로 report 없음이 race 부재 증명은 아니다.

instrumentation overhead가 timing을 바꾸고 supported primitive 밖 custom synchronization은 false positive/negative를 만들 수 있다. report를 source-level ownership model과 대조한다. discovered race는 deterministic regression test로 보강한다.

---

## CHAPTER 31 · model checking은 작은 state space의 모든 interleaving을 탐색해 reasoning을 검증한다

concurrency algorithm을 작은 thread 수와 state로 축소하면 가능한 scheduling/interleaving을 체계적으로 탐색할 수 있다. assertion/invariant violation, deadlock, livelock을 random stress보다 결정적으로 찾을 수 있다.

state explosion을 줄이기 위해 abstraction, symmetry, partial-order reduction을 사용한다. model이 implementation의 memory-order semantics를 충분히 반영하는지 확인한다. model proof와 actual code mapping이 끊기면 false confidence가 생긴다.

---

## CHAPTER 32 · history-based test는 concurrent operation의 invocation/response를 specification과 비교한다

실제 concurrent run에서 operation 시작·종료·argument·result를 기록하고 가능한 sequential history 중 specification을 만족하는 배치가 있는지 검사할 수 있다. linearizability checker가 이 접근을 사용한다.

history logging 자체가 timing을 바꿀 수 있으므로 여러 workload와 fault injection을 사용한다. timeout/indeterminate operation은 history에서 별도로 모델링한다. test는 implementation state가 아니라 externally observable contract를 검증한다.

---

## CHAPTER 33 · concurrency limit은 synchronization contention을 upstream에서 제한한다

DB pool, lock, CPU가 처리할 수 있는 in-flight 수보다 많은 request를 내부에 들이면 queue와 context switching만 증가한다. semaphore/admission controller로 critical section에 도달하는 concurrency 자체를 제한할 수 있다.

limit은 static magic number가 아니라 measured service capacity와 latency target에서 정한다. load class별 separate limit를 두면 low-priority bulk가 interactive path를 포화시키는 것을 막는다. rejection도 overload protocol의 일부다.

---

## CHAPTER 34 · Android race는 lifecycle과 async completion 순서가 뒤집히며 자주 발생한다

Activity/Composable이 사라진 뒤 background result가 돌아와 old UI state를 수정하거나, configuration change 뒤 두 request가 서로 다른 generation에 속하면서 stale result가 최신 state를 덮을 수 있다.

request generation/version을 state에 포함하고 lifecycle scope cancellation과 result freshness를 검증한다. main thread에서 실행된다는 사실만으로 logical race가 사라지지 않는다. event ordering과 user navigation을 deterministic test로 재현한다.

---

## CHAPTER 35 · synchronization 선택은 invariant, contention, progress, lifetime 네 축으로 결정한다

mutex, RWLock, atomic CAS, channel, actor, RCU, lock-free queue를 기술 선호로 고르지 않는다. 먼저 **보호할 invariant, 경쟁 빈도, 필요한 progress guarantee, object reclamation/lifetime**을 적는다.

가장 단순하게 증명 가능한 mechanism이 요구 성능을 만족하면 그것을 선택한다. 부족할 때만 더 약한 ordering이나 non-blocking structure로 내려간다. advanced concurrency의 목표는 primitive를 많이 아는 것이 아니라 **허용 가능한 history와 progress를 코드·테스트·운영 evidence로 증명하는 것**이다.