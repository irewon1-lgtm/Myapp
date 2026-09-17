# PART 15 · 동시성 심화 — progress, ordering, reclamation, proof

advanced concurrency의 난점은 atomic primitive를 많이 아는 데 있지 않다. operation이 어느 순간 발생한 것으로 간주되는지, 다른 observer가 어떤 순서를 볼 수 있는지, 제거된 memory를 언제 안전하게 회수할 수 있는지, 경쟁 속에서도 어떤 progress가 보장되는지를 하나의 증명으로 연결해야 한다. 이 PART는 그 네 축을 implementation, test, 운영 evidence까지 이어서 다룬다.

---

## CHAPTER 01 · read-modify-write는 경쟁 상태를 하나의 hardware-visible transition으로 묶는다

shared state를 `load → 계산 → store`로 갱신하면 두 thread가 같은 old value를 읽어 update 하나를 잃을 수 있다. atomic RMW는 read와 update를 하나의 indivisible synchronization event로 제공해 특정 state transition을 경쟁자와 직렬화할 수 있게 한다. 다만 원자적이라는 말은 해당 primitive의 대상 값에만 적용된다. 여러 field가 하나의 invariant를 이룬다면 각각 atomic이어도 중간 조합이 노출될 수 있다.

설계 단계에서는 counter 하나가 아니라 **무엇이 한 operation인가**를 먼저 정의한다. 예를 들어 queue의 tail 증가와 slot publication이 분리되어 있다면 tail만 atomic이어도 consumer가 아직 준비되지 않은 slot을 볼 수 있다. 이때 linearization point를 tail CAS로 둘지, slot visibility까지 완료된 시점으로 둘지 명확해야 한다. primitive 선택은 이 정의에서 거꾸로 내려와야 한다.

검증은 최종 값만 비교하지 않는다. competing RMW의 성공 순서, 실패한 CAS 수, retry 횟수, cache-line migration을 기록해 실제 contention을 본다. stress test에서 값이 맞더라도 특정 thread가 끝없이 실패한다면 progress 요구를 만족하지 못할 수 있다. correctness와 scalability를 같은 atomic이라는 단어로 합치지 않는다.

---

## CHAPTER 02 · CAS loop는 optimistic state transition이며 retry policy가 algorithm의 일부다

Compare-And-Swap는 현재 값이 expected와 같을 때만 새 값으로 바꾼다. 일반적인 loop는 state를 읽고 candidate를 계산한 뒤 CAS가 실패하면 최신 state에서 다시 계산한다. 이 구조에서 CAS 성공 순간을 linearization point로 잡을 수 있는 경우가 많지만, candidate가 외부 resource나 별도 memory object를 가리킨다면 성공 이전의 준비 작업과 이후 publication ordering까지 함께 봐야 한다.

contention이 높아지면 여러 thread가 같은 cache line을 읽고 CAS 실패를 반복해 useful work보다 retry와 coherence traffic에 더 많은 CPU를 쓸 수 있다. backoff, combining, sharding은 단순 성능 튜닝이 아니라 경쟁 구조를 바꾸는 방법이다. retry loop 내부에 allocation이나 I/O 같은 side effect를 넣으면 실패할 때마다 중복 실행되므로 candidate 계산이 pure한지 또는 보상 가능한지도 확인해야 한다.

운영 evidence에는 CAS success/failure ratio, retry distribution, line ownership 이동, operation latency를 함께 남긴다. 평균 retry가 낮아도 p99에서 특정 key가 수십 번 재시도하면 hot-key starvation이 숨어 있을 수 있다. algorithm이 lock-free라는 사실과 개별 request가 bounded latency를 갖는다는 사실은 별개다.

---

## CHAPTER 03 · LL/SC는 value equality보다 reservation continuity를 이용한다

Load-Linked/Store-Conditional 계열은 linked load 이후 관련 reservation이 유효할 때만 conditional store를 성공시키는 모델을 제공한다. 값이 A에서 B로 바뀌었다가 다시 A로 돌아와도 reservation이 깨졌다면 store가 실패할 수 있어 단순 value equality 기반 CAS와 다른 관찰점을 제공한다. 하지만 실제 architecture의 reservation granule과 invalidation 조건은 구현마다 다르다.

interrupt, context switch, 같은 granule의 unrelated store가 reservation을 깨뜨릴 수 있으므로 SC 실패를 곧바로 경쟁 thread의 논리적 update로 해석하면 안 된다. algorithm은 spurious failure를 정상 경로로 처리해야 하고, 무한 retry 가능성이 있는지 architecture의 forward-progress 보장을 확인해야 한다. granule이 예상보다 크면 독립 변수의 write가 서로의 reservation을 깨뜨리는 false contention도 생길 수 있다.

성능 분석에서는 SC failure rate만 보지 않고 failure가 어떤 CPU placement와 memory layout에서 증가하는지 측정한다. alignment나 field packing을 바꾼 뒤 실패율이 크게 달라진다면 논리적 경쟁보다 reservation granule sharing이 원인일 수 있다. portable library가 LL/SC와 CAS 차이를 감싸더라도 low-level algorithm의 progress proof는 target ISA 조건을 명시해야 한다.

---

## CHAPTER 04 · lock-free, wait-free, obstruction-free는 서로 다른 progress guarantee다

lock-free는 system 전체 차원에서 계속 어떤 operation이 완료된다는 성질에 가깝고 특정 thread starvation을 허용할 수 있다. wait-free는 각 participant가 bounded step 안에 완료되는 더 강한 보장을 요구한다. obstruction-free는 경쟁 없이 혼자 충분히 실행되면 완료될 수 있다는 더 약한 조건이다. 이 용어들은 benchmark에서 빠르다는 뜻이 아니다.

scheduler가 adversarial하게 특정 thread를 계속 preempt하거나 CAS에서 지게 만드는 상황을 상정해야 progress guarantee의 차이가 드러난다. real-time request가 개별 latency bound를 요구한다면 system-wide lock-free만으로 충분하지 않을 수 있다. 반대로 background maintenance는 starvation 가능성을 허용하면서 전체 throughput을 우선할 수도 있다.

test는 단순 성공률보다 **누가 얼마나 오래 진행하지 못했는가**를 기록해야 한다. operation별 step/retry 수, longest incomplete age, thread별 completion distribution을 관찰하면 평균 throughput 뒤에 숨은 starvation을 찾을 수 있다. progress property는 implementation documentation과 workload SLO가 연결되어야 실용적인 의미를 갖는다.

---

## CHAPTER 05 · lock-free structure는 blocking을 줄이는 대신 coherence와 proof 비용을 늘릴 수 있다

mutex를 제거하면 lock owner preemption에 의한 convoy를 피할 수 있지만 CAS retry, cache-line bouncing, reclamation, weak-memory ordering proof가 추가된다. 특히 read-modify-write가 같은 metadata line에 집중되면 lock이 없어도 사실상 하나의 serialization point가 생긴다. low-contention critical section에서는 잘 구현된 mutex가 더 빠르고 검증도 쉬울 수 있다.

비교는 label이 아니라 workload에서 해야 한다. reader/writer 비율, key skew, CPU oversubscription, preemption, NUMA 배치, object lifetime을 고정한 뒤 mutex와 non-blocking 구조의 throughput과 tail latency를 측정한다. lock-free 구조가 평균은 빠르지만 retire backlog가 폭증하거나 memory footprint가 커질 수도 있다.

운영 난이도도 비용이다. crash dump에서 pointer generation과 reclamation epoch를 복원할 수 없으면 희귀 UAF를 찾기 어렵다. theoretical progress가 강해도 on-call이 incident를 재현하지 못하면 reliability는 낮아질 수 있다. 구조 선택에는 proof complexity와 observability budget까지 포함한다.

---

## CHAPTER 06 · ABA는 동일 value가 동일 history를 의미하지 않는 문제다

Thread A가 pointer A를 읽은 뒤 멈춘 사이 Thread B가 node를 제거하고 같은 address를 다른 node에 재사용하면 값이 다시 A처럼 보일 수 있다. CAS는 expected pointer equality만 확인하므로 중간 topology 변화가 없었다고 오판할 수 있다. 숫자 counter도 wraparound나 generation reuse가 있으면 같은 형태의 문제가 생길 수 있다.

tagged pointer나 version counter는 identity에 generation을 추가해 value equality와 object identity를 분리한다. 하지만 tag bit 수가 제한되면 wraparound가 다시 ABA를 만들 수 있고, pointer packing은 alignment와 architecture 제약을 따른다. hazard pointer나 epoch reclamation처럼 old address가 너무 빨리 재사용되지 않도록 lifetime 자체를 통제하는 방법도 있다.

재현 test는 A→B→A sequence를 의도적으로 만들고 stale observer의 CAS가 거부되는지 확인해야 한다. address reuse가 allocator timing에 의존하면 deterministic allocator나 fake pool을 사용해 같은 address를 재사용시킨다. ABA 방어는 bit trick 하나가 아니라 identity와 reclamation의 공동 계약이다.

---

## CHAPTER 07 · logical removal과 physical reclamation은 분리해야 한다

lock-free list에서 node를 unlink했다고 모든 reader가 그 pointer를 버렸다는 뜻은 아니다. reader는 더 이전 traversal에서 node를 읽고 register나 local variable에 보유하고 있을 수 있다. unlink 직후 free하면 abstract data structure의 논리는 맞더라도 실제 memory에서는 use-after-free가 발생한다.

따라서 algorithm은 **논리적 제거 시점**, **새 reader가 더 이상 획득할 수 없는 시점**, **기존 reader가 모두 끝난 시점**, **allocator가 주소를 재사용해도 되는 시점**을 분리해야 한다. reference count, hazard pointer, epoch, RCU는 이 간격을 관리하는 서로 다른 방식이다. GC가 있는 언어라도 off-heap/native resource에서는 같은 문제가 남는다.

운영에서는 retired object 수와 reclaim delay를 metric으로 둔다. 구조상 안전하지만 stalled reader 때문에 retired list가 계속 커지면 memory leak처럼 보일 수 있다. 반대로 backlog를 줄이려고 grace period를 짧게 잡아 safety 조건을 깨면 더 위험하다. reclamation latency는 correctness와 capacity를 함께 설명하는 핵심 지표다.

---

## CHAPTER 08 · hazard pointer는 dereference 예정 pointer를 명시적으로 publish한다

reader는 candidate pointer를 hazard slot에 저장하고, 그 사이 structure가 바뀌지 않았는지 다시 확인한 뒤 dereference한다. reclaimer는 retired node가 어떤 active hazard에도 없을 때만 free한다. 핵심 순서는 `read candidate → publish hazard → validate candidate → dereference`이며, publication과 validation의 ordering이 틀리면 보호 창이 생기지 않는다.

hazard slot은 thread 또는 execution context와 연결되므로 thread exit, pool reuse, cancellation에서 반드시 정리되어야 한다. stale hazard가 남으면 safety는 유지되지만 reclamation이 영원히 지연될 수 있다. 반대로 coroutine이 thread를 이동하는데 hazard를 physical thread local에만 묶으면 logical reader와 보호 범위가 어긋날 수 있다.

검증에서는 retire 후 즉시 scan하는 경우, hazard publish 직전/직후 writer가 unlink하는 경우, thread 종료 중인 경우를 강제로 만든다. metric으로는 active hazard 수, retire-list 길이, reclaim age를 본다. 안전성 proof와 memory-pressure behavior를 동시에 확인해야 실전 구조가 된다.

---

## CHAPTER 09 · epoch-based reclamation은 active reader generation을 이용한다

epoch reclamation은 reader가 현재 generation 참여를 표시하고 writer가 제거된 node를 특정 epoch의 retire list에 넣는 방식이다. 모든 relevant reader가 이전 epoch를 벗어났다고 확인된 뒤에야 그 node를 free할 수 있다. reader hot path가 짧아질 수 있는 대신 stalled participant 하나가 전체 reclaim frontier를 붙잡을 수 있다.

physical thread와 logical task 중 무엇을 participant로 볼지도 중요하다. thread pool의 worker가 request A의 epoch state를 남긴 채 request B를 수행하면 unrelated work가 reclaim을 막을 수 있다. coroutine처럼 실행 위치가 바뀌는 runtime에서는 enter/exit가 task lifetime과 일치하도록 설계해야 한다.

운영에서는 current epoch 숫자만으로 부족하다. oldest active epoch, participant age, retired bytes, reclaim batch size를 함께 본다. memory usage가 증가할 때 실제 leak인지 한 stalled reader 때문에 reclamation이 멈춘 것인지 구분할 수 있어야 한다. watchdog으로 오래된 participant를 탐지하더라도 safety를 무시하고 강제 제거해서는 안 된다.

---

## CHAPTER 10 · RCU는 read-side cost를 최소화하고 update/reclaim에 복잡성을 집중한다

Read-Copy-Update 계열은 reader가 old version을 계속 읽는 동안 writer가 new version을 만들고 pointer를 publish한 뒤 grace period 이후 old version을 reclaim한다. read-mostly 구조에서는 reader가 heavy lock을 잡지 않아도 되어 강력하지만, publication ordering과 grace-period 의미를 정확히 이해해야 한다.

grace period는 단순 wall-clock delay가 아니다. 해당 RCU flavor에서 기존 reader가 모두 quiescent state를 통과했다는 논리적 조건이다. scheduler/context rule이 reader completion과 연결되므로 일반 user-space callback을 그대로 대입할 수 없다. update가 빈번하면 old version이 여러 세대 누적되어 memory duplication이 커질 수 있다.

진단에서는 update rate, grace-period duration, callback backlog, reader critical-section 길이를 같이 본다. reader latency는 낮은데 reclaim callback이 밀리면 system memory가 압박받을 수 있다. RCU의 이점은 lock이 없다는 사실이 아니라 읽기 비용과 회수 비용을 의도적으로 다른 곳에 배치한다는 점이다.

---

## CHAPTER 11 · seqlock은 reader retry를 허용해 writer serialization을 단순화한다

seqlock 계열에서는 writer가 sequence counter를 변경하며 data를 갱신하고, reader는 시작과 종료 sequence가 같은 stable generation인지 확인한다. writer와 겹친 reader는 snapshot을 버리고 처음부터 다시 읽는다. 그래서 reader operation은 side effect가 없어야 하고 여러 field를 copy한 뒤 검증할 수 있어야 한다.

pointer가 snapshot 안에 포함되어 있고 writer가 pointee lifetime까지 바꾼다면 seqlock만으로 memory safety가 해결되지 않는다. reader가 retry 여부를 판단하기 전에 이미 freed pointer를 dereference할 수 있기 때문이다. 구조 자체의 consistency와 object reclamation을 분리해 proof해야 한다.

writer가 너무 빈번하면 reader가 반복 retry하며 starvation될 수 있다. sequence retry 수와 writer hold duration을 metric으로 두면 평균 read latency만으로 보이지 않는 병목을 찾을 수 있다. seqlock은 `read가 많다`는 이유 하나가 아니라 retry 가능한 immutable-like snapshot에 적합한 도구다.

---

## CHAPTER 12 · read-write lock은 reader concurrency와 metadata contention을 교환한다

RWLock은 동시에 여러 reader를 허용하고 writer에게 exclusive access를 준다. read-heavy workload에서 병렬성을 높일 수 있지만 reader count나 lock state 자체가 shared hot metadata가 되어 cache-line contention을 만들 수 있다. critical section이 매우 짧다면 일반 mutex보다 오버헤드가 더 클 수 있다.

fairness policy도 중요하다. reader preference는 지속적인 read traffic에서 writer starvation을 만들 수 있고, writer preference는 새 reader를 막아 read tail latency를 늘릴 수 있다. lock 구현의 이름만 보고 정책을 추측하지 말고 target runtime의 semantics와 실제 wait distribution을 확인한다.

benchmark는 평균 reader 비율 외에 burst writer, long reader, owner preemption을 포함해야 한다. reader/writer별 p99 wait와 starvation count를 기록하고, immutable snapshot이나 RCU가 더 적합한지 비교한다. RWLock은 읽기 병렬성과 writer progress 사이의 정책 도구다.

---

## CHAPTER 13 · priority inversion은 lock ownership과 scheduler priority가 충돌할 때 발생한다

low-priority task가 lock을 보유한 상태에서 high-priority task가 그 lock을 기다리고, medium-priority task가 CPU를 계속 사용하면 high-priority task가 간접적으로 medium보다 뒤로 밀린다. lock graph만 보면 A가 B를 기다리는 단순 관계지만 scheduler까지 포함하면 실제 지연 원인이 달라진다.

priority inheritance는 owner의 priority를 임시로 높여 critical section을 빨리 끝내게 할 수 있지만 nested lock, 여러 waiter, 다른 scheduling class가 섞이면 분석이 복잡해진다. priority ceiling은 resource가 사용할 수 있는 상한을 미리 정의하는 다른 접근이다. 일반 서버에서도 control-plane thread가 background owner를 기다릴 때 유사한 tail spike가 생길 수 있다.

trace에서는 waiter priority, owner state, runnable-but-not-running 시간, lock hold 구간을 같은 timeline에 놓는다. 단순히 mutex가 오래 잡혔다고 결론내리면 owner가 CPU를 못 받은 원인을 놓친다. priority 문제는 synchronization과 scheduling evidence를 함께 봐야 한다.

---

## CHAPTER 14 · lock convoy는 한 owner 지연이 waiter 전체의 throughput을 끌어내린다

high-contention mutex에서 owner가 preempt되거나 page fault, I/O에 걸리면 많은 waiter가 한 줄로 쌓인다. owner가 lock을 풀어도 wakeup, context switch, cache-line ownership 이동이 연쇄적으로 발생해 recovery가 즉시 끝나지 않을 수 있다. 공정한 FIFO handoff가 오히려 이 줄을 강하게 유지하는 경우도 있다.

critical section 안의 blocking operation을 제거하고 protected state를 partition하는 것이 우선이다. spin count를 늘리는 미세 튜닝은 owner가 실제로 CPU에서 실행 중일 때만 의미가 있다. owner가 sleep 중이라면 spin은 contention collapse를 악화시킨다.

metric으로 hold time만 보지 말고 waiter count, owner on/off-CPU 상태, handoff latency를 함께 본다. 같은 평균 hold time이라도 드물게 긴 owner stall 하나가 p99를 지배할 수 있다. convoy는 lock cost가 아니라 queueing과 scheduler가 결합된 현상이다.

---

## CHAPTER 15 · sharding은 하나의 synchronization domain을 여러 독립 domain으로 분할한다

global map lock 대신 key hash에 따라 shard lock을 사용하면 unrelated key operation이 서로 독립적으로 진행할 수 있다. 그러나 hot key가 한 shard에 몰리면 전체 shard 수가 많아도 그 지점의 contention은 줄지 않는다. shard count 자체가 해결책이 아니라 traffic distribution과 state partition이 핵심이다.

multi-key operation은 여러 shard를 동시에 잠가야 할 수 있어 deadlock ordering과 atomicity가 다시 복잡해진다. global invariant가 있다면 shard-local lock만으로 보호되지 않는다. rebalancing으로 key가 shard를 이동할 때 old/new shard 사이 transfer protocol도 필요하다.

관측은 shard별 request rate, lock wait, hot-key 분포, rebalance 빈도를 포함해야 한다. aggregate p99만 보면 한 shard의 폭발을 놓칠 수 있다. sharding은 synchronization 범위를 줄이는 대신 cross-shard operation이라는 새로운 경계를 만든다.

---

## CHAPTER 16 · optimistic concurrency는 conflict가 드물다는 가정에 의존한다

optimistic scheme은 version을 읽고 lock 없이 work를 수행한 뒤 commit 시 version이 바뀌지 않았는지 검증한다. conflict가 없으면 긴 critical section을 피할 수 있지만 충돌이 있으면 계산한 work를 버리고 retry하거나 abort해야 한다. long transaction은 conflict window를 넓혀 성공률을 낮춘다.

retry가 cheap하다는 가정도 명시해야 한다. 외부 API 호출이나 irreversible side effect를 optimistic section 안에서 먼저 수행하면 commit conflict 후 중복 효과가 남는다. candidate computation과 commit side effect를 분리하거나 idempotency key를 사용해야 한다.

성능은 성공 path latency만 측정하면 안 된다. conflict rate, wasted CPU, retry depth, abort age를 함께 본다. load가 높아질수록 retry traffic이 추가 load를 만들어 positive feedback을 만들 수 있다. 일정 conflict 이상에서는 pessimistic lock이나 queue serialization이 더 안정적일 수 있다.

---

## CHAPTER 17 · linearizability는 operation을 호출-응답 사이 한 순간에 일어난 것처럼 설명할 수 있는가를 묻는다

concurrent history가 sequential specification과 일치하도록 각 operation에 한 순간의 linearization point를 배치할 수 있고 real-time order를 보존한다면 linearizable하다고 본다. 먼저 완료된 operation이 나중에 시작한 operation 뒤로 이동해서는 안 된다. 이는 내부 구현 순서가 아니라 외부 관찰 가능한 history의 조건이다.

linearization point는 CAS 한 줄일 수도 있지만 helping algorithm에서는 다른 thread가 operation을 대신 완료하는 순간일 수도 있다. enqueue가 tail pointer를 움직인 순간인지 slot이 reader에게 보이는 순간인지 specification에 따라 달라질 수 있다. code comment로 point를 표시해도 실제 proof와 맞지 않으면 의미가 없다.

history-based test는 invocation, response, argument, result를 기록해 가능한 sequential ordering을 탐색할 수 있다. timeout으로 결과를 모르는 operation은 특별히 모델링해야 한다. deterministic proof와 randomized history test를 함께 사용하면 구현과 specification의 간극을 줄일 수 있다.

---

## CHAPTER 18 · consistency model은 observer가 허용받는 history 집합을 정의한다

sequential consistency, causal consistency, eventual consistency 같은 용어는 observer가 어떤 순서를 볼 수 있는지 정의한다. 이름을 외우는 것보다 구체적인 anomaly를 history로 쓰는 편이 강하다. 예를 들어 write A가 보인 뒤 write B가 반드시 보여야 하는지, 서로 다른 client가 같은 order를 봐야 하는지로 요구를 표현한다.

더 약한 consistency는 latency, availability, partition tolerance 측면에서 선택지를 늘릴 수 있지만 application invariant가 견딜 수 있어야 한다. balance, inventory, feed ordering은 필요한 보장이 다르다. 한 subsystem의 eventual semantics를 linearizable API처럼 노출하면 상위 계층이 잘못된 전제를 갖게 된다.

test는 정상 결과만 아니라 허용되지 않아야 할 history를 생성해야 한다. fault injection과 delayed message, retry를 섞어 stale read, lost update, reordering이 specification 범위인지 확인한다. consistency는 storage 제품의 마케팅 라벨이 아니라 application-level 계약이다.

---

## CHAPTER 19 · double-checked locking은 publication ordering이 없으면 incomplete object를 노출할 수 있다

`if null → lock → if null → create` 패턴은 object construction write와 reference publication 사이 ordering이 보장되어야 한다. 언어 memory model이 요구하는 volatile 또는 atomic semantics가 없으면 다른 thread가 non-null reference를 본 뒤 일부 field가 아직 보이지 않는 상태를 관찰할 수 있다.

modern runtime이 제공하는 lazy initialization primitive는 이러한 ordering을 검증된 방식으로 캡슐화한다. 직접 구현한다면 constructor side effect, exception, retry, publication edge까지 proof해야 한다. object가 외부 callback에 `this`를 노출하는 경우에는 reference가 더 일찍 escape할 수 있어 별도 위험이 생긴다.

재현이 어렵다고 안전한 것이 아니다. timing delay를 넣어 bug가 사라지는 것은 proof가 아니다. compiler optimization과 다른 ISA에서도 happens-before가 유지되는지 specification으로 확인해야 한다. publication 문제는 cache flush 감각이 아니라 language memory model의 문제다.

---

## CHAPTER 20 · safe publication은 object lifetime의 시작점을 synchronization event에 연결한다

immutable object도 construction 완료 상태가 reader에게 보인다는 publication guarantee가 필요하다. lock release/acquire, atomic store/load, thread start/join 같은 synchronization edge는 writer의 초기화가 reader의 관찰보다 앞선다는 happens-before 관계를 만든다. 이 edge가 없다면 source 순서만으로는 충분하지 않을 수 있다.

publication 이후 mutation이 없다면 reasoning이 크게 단순해진다. 반대로 mutable shared object는 각 mutation path가 새로운 synchronization 계약을 필요로 한다. object ownership을 한 thread에서 다른 thread로 넘길 때 transfer event를 API로 드러내면 accidental sharing을 줄일 수 있다.

검증에서는 object 생성 직후 다른 core가 읽는 경로, pool에 넣고 꺼내는 경로, shutdown 중 재사용 경로를 본다. field 값이 우연히 맞는지보다 publication primitive가 모든 경로에서 동일하게 사용되는지 확인해야 한다. lifetime의 시작점을 명확히 해야 reclamation의 끝점도 정의할 수 있다.

---

## CHAPTER 21 · work stealing은 idle worker가 다른 worker의 deque에서 task를 가져간다

fork-join workload에서 worker는 local deque를 주로 사용해 locality를 유지하고 idle worker가 다른 worker의 deque 반대쪽에서 task를 steal할 수 있다. task granularity가 너무 작으면 deque synchronization과 steal 비용이 커지고, 너무 크면 일부 worker만 오래 바빠 load balance가 나빠진다.

blocking task가 compute pool worker를 점유하면 steal만으로 해결되지 않는 starvation이 생긴다. 모든 worker가 I/O wait에 빠지면 runnable compute task가 queue에 남아도 실행 주체가 없다. blocking workload를 별도 pool로 분리하거나 runtime의 managed-blocking mechanism을 사용해야 한다.

관측에는 local-pop, steal-success/failure, runnable task age, worker idle time을 포함한다. queue length가 짧아도 오래된 task가 남아 있으면 scheduling unfairness가 존재할 수 있다. work stealing의 목적은 worker 수를 늘리는 것이 아니라 dynamic load를 분산하면서 locality를 보존하는 것이다.

---

## CHAPTER 22 · structured concurrency는 child task lifetime을 lexical/request lifetime에 묶는다

structured concurrency는 parent scope가 끝날 때 child task의 completion이나 cancellation이 정리되도록 lifetime을 구조화한다. 이 방식은 orphan work와 leaked callback을 줄이고 error propagation 경계를 명확하게 만든다. 동시에 실행한다는 사실보다 **누가 child의 종료를 책임지는가**가 핵심이다.

모든 child failure가 sibling 전체를 취소해야 하는 것은 아니다. request의 핵심 subtask라면 fail-fast가 맞을 수 있고 독립 telemetry라면 supervisor-style 격리가 더 적합할 수 있다. deadline도 parent에서 child로 전달되어야 이미 만료된 작업이 downstream resource를 계속 쓰지 않는다.

test에서는 parent success, child failure, parent cancellation, timeout 경쟁을 각각 만든다. 종료 후 active child 수가 0인지, resource cleanup이 완료됐는지 확인한다. structured concurrency는 syntax가 아니라 lifetime과 failure tree를 코드 구조에 강제하는 설계 원칙이다.

---

## CHAPTER 23 · cancellation은 partially completed side effect를 처리하는 protocol이다

cooperative task가 cancellation을 관찰하는 순간 file write, DB transaction, remote request가 이미 일부 수행됐을 수 있다. exception을 던진다고 외부 세계의 side effect가 자동으로 rollback되지는 않는다. 그래서 operation을 cancellable phase와 commit phase로 나누거나 idempotent compensation을 준비해야 한다.

cleanup 자체가 cancellation되어 lock이나 descriptor가 남는 문제도 고려한다. critical cleanup은 bounded non-cancellable section으로 보호할 수 있지만 너무 넓게 잡으면 cancellation latency가 길어진다. remote operation은 caller가 포기한 뒤에도 서버에서 계속 실행될 수 있어 request ID와 status query가 필요하다.

metric으로 caller cancellation 시각, underlying work 종료 시각, cleanup 완료 시각을 분리한다. 이 차이가 계속 길어지면 ghost work가 capacity를 잠식한다. cancellation correctness는 빠르게 예외를 반환하는 것이 아니라 invariant를 보존하며 ownership을 회수하는 것이다.

---

## CHAPTER 24 · channel은 memory queue와 backpressure policy를 함께 가진다

message channel은 shared mutable state에 직접 접근하는 대신 message ownership을 이동시키지만 capacity와 ordering semantics가 필요하다. rendezvous channel, bounded buffer, unbounded buffer는 producer와 consumer를 묶는 정도가 다르다. channel을 사용했다고 overload 문제가 사라지는 것은 아니다.

full 상태에서 suspend, drop, fail, overwrite 중 어떤 정책을 택할지가 application correctness를 결정한다. telemetry는 drop이 허용될 수 있지만 transaction command는 손실되면 안 된다. unbounded mailbox는 lock contention을 queue latency와 memory growth로 바꿀 뿐이다.

운영에서는 queue depth뿐 아니라 oldest message age, drop/reject count, producer wait를 본다. ordering이 중요한 경우 retry된 message가 원래 순서를 깨지 않는지도 확인해야 한다. channel은 synchronization primitive이면서 동시에 admission-control 지점이다.

---

## CHAPTER 25 · actor도 cyclic request/reply dependency를 만들면 deadlock할 수 있다

actor는 내부 mutable state를 한 mailbox에서 serialize해 data race를 줄일 수 있지만 logical dependency cycle은 제거하지 않는다. Actor A가 B의 reply를 기다리고 B가 다시 A의 synchronous reply를 기다리면 mailbox thread가 서로 진행하지 못할 수 있다. lock이 없어도 wait-for graph는 생긴다.

actor가 long computation이나 blocking I/O를 mailbox 처리 thread에서 수행하면 뒤 message 전체가 지연된다. offload 후 result correlation을 사용하고, reply가 돌아왔을 때 original request generation이 아직 유효한지 확인해야 한다. timeout과 cancellation도 message protocol에 포함한다.

진단은 mailbox depth만으로 부족하다. actor별 current message, outstanding request/reply edge, oldest wait를 연결해 graph를 만든다. actor model의 장점은 state ownership을 단순화하는 데 있고, progress와 backpressure는 여전히 별도로 설계해야 한다.

---

## CHAPTER 26 · fairness는 throughput과 tail starvation 사이의 정책 선택이다

strict FIFO lock이나 scheduler는 기다린 순서를 보장하지만 cache-local owner가 곧바로 재획득할 기회를 막아 throughput을 낮출 수 있다. unfair policy는 locality와 throughput을 높일 수 있지만 unlucky waiter가 매우 오래 기다릴 수 있다. 어떤 정책이 좋은지는 workload SLO에 달려 있다.

user request는 bounded latency가 중요할 수 있고 background compaction은 느슨한 fairness를 허용할 수 있다. 단일 global policy보다 class별 queue나 quota가 더 적절한 경우가 있다. aging은 오래 기다린 work의 우선순위를 점차 높여 starvation을 완화할 수 있다.

평균 wait time만 보면 starvation을 숨긴다. p99/max wait, longest pending age, waiter별 service count를 측정한다. fairness는 윤리적 표현이 아니라 progress distribution에 대한 구체적인 시스템 정책이다.

---

## CHAPTER 27 · spinlock과 mutex 선택은 expected wait와 preemption 가능성에 달려 있다

spinlock은 lock이 곧 풀릴 것으로 기대하고 CPU를 사용하며 기다리고, sleep mutex는 scheduler 전환 비용을 내고 CPU를 양보한다. owner가 다른 core에서 아주 짧은 critical section을 실행 중이면 spin이 유리할 수 있다. 반대로 owner가 preempt되거나 I/O에 걸렸다면 spin은 순수 낭비다.

single-core 또는 oversubscribed system에서는 spinner가 owner의 실행 기회를 빼앗을 수 있다. adaptive mutex는 짧게 spin한 뒤 sleep하는 방식으로 두 비용을 절충한다. threshold는 magic number가 아니라 hold-time distribution과 scheduler topology를 바탕으로 정해야 한다.

관측에는 spin CPU time, owner running state, sleep/wakeup 횟수, lock wait를 포함한다. 평균 hold time이 짧아도 드문 long hold가 많으면 aggressive spinning은 tail을 악화시킨다. primitive 선택은 critical section 코드만 아니라 scheduler 상태까지 포함한다.

---

## CHAPTER 28 · false sharing은 synchronization primitive 밖의 coherence contention이다

각 thread가 서로 다른 counter를 수정해 logical data race가 없어도 값들이 같은 cache line에 있으면 write ownership이 core 사이를 계속 이동할 수 있다. lock profiler에는 아무 병목도 보이지 않지만 scaling이 나빠질 수 있다. 이 현상은 데이터 구조의 물리적 layout에서 발생한다.

padding만 넣는 것이 항상 답은 아니다. allocator alignment, object header, array stride 때문에 의도한 분리가 실제 binary layout에서 유지되지 않을 수 있다. per-thread aggregation이나 update batching으로 shared write 자체를 줄이는 편이 더 강한 해결책일 수 있다.

PMU의 cache-to-cache transfer, coherence event, thread affinity 변화에 따른 throughput을 함께 본다. source-level field 이름이 다르다는 사실은 증거가 아니다. 실제 address와 cache-line boundary를 확인해야 logical sharing과 physical sharing을 구분할 수 있다.

---

## CHAPTER 29 · contention collapse는 concurrency 증가가 useful throughput을 감소시키는 구간이다

worker 수가 늘면서 lock retry, context switch, cache bouncing, queue bookkeeping이 useful work보다 커지면 throughput이 오히려 감소한다. 이 구간에서는 latency도 급격히 늘어나고 더 많은 client retry가 들어오면 collapse가 가속된다. CPU utilization이 100%가 아니어도 off-CPU wait와 coherence가 병목일 수 있다.

concurrency limit과 admission control은 system을 collapse 이전 operating point에 묶는 도구다. limit은 단순 thread 수가 아니라 downstream DB pool, critical lock, memory bandwidth 같은 실제 service center의 capacity와 맞춰야 한다. overload에서 일부 request를 빠르게 reject하는 것이 전체 성공률을 높일 수 있다.

load test는 throughput curve를 concurrency에 따라 그려 peak 이후 감소 구간까지 확인해야 한다. p99 latency, retries, lock wait, runnable queue를 함께 보면 collapse가 어디서 시작되는지 찾을 수 있다. 최대 동시성보다 안정적인 operating region이 목표다.

---

## CHAPTER 30 · race detector는 runtime access history로 happens-before 위반 후보를 찾는다

dynamic race detector는 memory access instrumentation과 synchronization event를 추적해 conflicting access 사이 happens-before가 없는 경우를 보고한다. 실제로 실행된 interleaving만 관찰하므로 report가 없다고 race가 없다는 proof는 아니다. 반대로 custom synchronization을 detector가 이해하지 못하면 false positive가 생길 수 있다.

instrumentation은 실행 속도와 allocation layout을 바꾸어 원래 timing을 변화시킨다. 따라서 detector run과 production-like stress를 서로 대체하지 않는다. report가 나오면 두 access의 ownership 계약을 확인하고, 단순 suppression보다 왜 synchronization edge가 없었는지 추적해야 한다.

발견한 race는 barrier나 deterministic scheduler를 이용해 재현 가능한 regression test로 고정한다. random stress에서 한 번 잡힌 사건만 남기면 미래에 다시 확인하기 어렵다. detector는 증거 생성 도구이고 최종 수정은 invariant와 happens-before를 명시하는 일이다.

---

## CHAPTER 31 · model checking은 작은 state space의 모든 interleaving을 탐색해 reasoning을 검증한다

concurrency algorithm을 작은 thread 수와 제한된 state로 축소하면 가능한 interleaving을 체계적으로 탐색해 assertion violation, deadlock, livelock을 찾을 수 있다. random stress가 우연한 schedule을 샘플링한다면 model checking은 정의한 state space를 가능한 한 빠짐없이 탐색하는 쪽에 가깝다.

state explosion 때문에 abstraction, symmetry, partial-order reduction이 필요하다. 그러나 abstraction이 핵심 memory-order behavior를 지워 버리면 proof가 실제 implementation을 대표하지 못한다. model의 atomic step과 production code의 instruction/primitive가 어떻게 대응하는지 문서화해야 한다.

실패 trace는 매우 가치가 있다. 최소 counterexample sequence를 실제 test harness의 barrier sequence로 변환하면 model과 code를 연결할 수 있다. 반대로 model은 PASS했지만 mapping이 불명확하다면 안전하다고 선언해서는 안 된다.

---

## CHAPTER 32 · history-based test는 concurrent operation의 invocation/response를 specification과 비교한다

실제 concurrent run에서 operation의 invocation, response, argument, result를 기록하면 가능한 sequential history 중 specification을 만족하는 배치가 있는지 검사할 수 있다. linearizability checker는 내부 구현을 몰라도 외부 observable contract를 기준으로 판단할 수 있다는 장점이 있다.

timeout이나 process crash로 response를 모르는 operation은 history에서 특별히 모델링해야 한다. 무조건 실패로 처리하면 실제로 commit된 operation을 잘못 판단할 수 있고, 성공으로 처리하면 반대 오류가 생긴다. fault injection을 사용해 ambiguous completion을 의도적으로 포함해야 한다.

history logging 자체가 timing을 바꿀 수 있으므로 lightweight event와 다양한 workload를 사용한다. failure가 발견되면 최소 history를 추출해 deterministic test로 바꾼다. specification이 명확하지 않으면 checker도 답을 줄 수 없으므로 API 계약이 먼저다.

---

## CHAPTER 33 · concurrency limit은 synchronization contention을 upstream에서 제한한다

DB pool, lock, CPU가 효율적으로 처리할 수 있는 수보다 많은 request를 내부에 들이면 queue와 context switching만 늘어날 수 있다. semaphore, token, worker limit은 critical resource에 도달하는 in-flight 수를 upstream에서 제한해 overload를 bounded state로 만든다.

limit은 static magic number가 아니다. service time, downstream capacity, target latency를 측정해 정하고 workload class가 다르면 별도 quota를 둘 수 있다. bulk work가 interactive request의 모든 permit을 점유하지 않게 reserved capacity나 partitioned pool을 사용할 수 있다.

운영에서는 permit wait time, rejection rate, downstream utilization을 함께 본다. limit이 너무 낮으면 resource가 놀고, 너무 높으면 내부 queue가 다시 커진다. adaptive limit을 쓰면 feedback delay 때문에 oscillation이 생기지 않는지도 확인해야 한다.

---

## CHAPTER 34 · Android race는 lifecycle과 async completion 순서가 뒤집히며 자주 발생한다

Activity나 Composable이 사라진 뒤 background result가 돌아와 old UI state를 수정하거나, configuration change 뒤 서로 다른 request generation의 결과가 역순으로 도착하면 stale result가 최신 state를 덮을 수 있다. main thread에서 callback이 실행된다는 사실은 이런 logical race를 막지 않는다.

request generation이나 state version을 결과에 포함해 적용 시점에 freshness를 검증한다. lifecycle scope cancellation은 오래된 work를 줄이는 데 도움이 되지만 remote request가 실제로 중단됐다는 보장은 없다. process death까지 고려하면 중요한 transaction state는 callback chain에만 두면 안 된다.

test에서는 navigation, rotation, rapid repeated search, network delay를 deterministic scheduler로 조합해 completion 순서를 바꾼다. 화면 crash 여부뿐 아니라 최종 state가 최신 generation과 일치하는지 확인한다. Android race는 thread safety와 lifecycle correctness가 만나는 문제다.

---

## CHAPTER 35 · synchronization 선택은 invariant, contention, progress, lifetime 네 축으로 결정한다

mutex, RWLock, atomic CAS, channel, actor, RCU, lock-free queue를 기술 선호로 고르면 복잡성만 늘어난다. 먼저 보호할 invariant, contention distribution, 필요한 progress guarantee, object lifetime/reclamation 조건을 적는다. 그 네 질문에 가장 단순하게 답하는 mechanism이 기본 선택이다.

성능이 부족할 때도 primitive부터 교체하지 않는다. trace에서 lock wait인지 cache-line bouncing인지 queue overload인지 확인하고 병목의 원인을 줄이는 방향으로 구조를 바꾼다. 더 약한 memory ordering이나 non-blocking structure로 내려가면 proof와 test burden이 커진다는 비용을 명시해야 한다.

최종 evidence는 code review 설명, deterministic concurrency test, stress/history test, 운영 metric이 서로 같은 invariant를 가리켜야 한다. advanced concurrency의 목표는 lock-free라는 라벨이 아니라 **허용 가능한 history와 progress를 반복해서 검증 가능한 형태로 증명하는 것**이다.
