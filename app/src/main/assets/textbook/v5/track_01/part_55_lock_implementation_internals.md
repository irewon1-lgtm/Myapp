# PART 55 · Lock Implementation Internals — spin, park, futex, handoff, priority

Mutex는 `locked/unlocked` boolean 하나가 아니다. Fast path의 atomic state, waiter registration, futex sleep/wake, spinning, handoff policy, priority inheritance, robust owner-death 처리까지 여러 protocol이 겹친다. Contention이 없을 때 몇 ns 빠른 lock도 경쟁이 시작되면 scheduler와 cache coherence에 의해 전혀 다른 behavior를 보인다. 이 PART는 lock word에서 시작해 fairness·PI·observability까지 **대기와 ownership transfer의 실제 상태 기계**를 추적한다.

---

## CHAPTER 01 · lock word는 ownership과 waiter 상태를 압축한 동기화 protocol의 중심이다

가장 단순한 mutex도 memory의 작은 word에 unlocked/locked 상태를 저장하고 atomic compare-and-swap으로 소유권을 얻는다. 실제 구현은 contention bit, owner id, waiter hint 같은 추가 정보를 같은 word에 encoding할 수 있다. 이 값은 단순 flag가 아니라 userspace와 kernel이 공유하는 protocol state다.

Bit layout을 잘못 해석하면 debugger나 custom wrapper가 상태를 망가뜨릴 수 있다. Library 내부 lock representation에 직접 의존하지 않는 것이 중요하다.

Correctness는 “한 번에 한 owner”뿐 아니라 owner release와 waiter acquisition 사이 memory ordering을 포함한다. Lock word 변경이 protected data의 visibility를 전달해야 한다.

## CHAPTER 02 · uncontended fast path는 kernel 진입 없이 atomic operation 몇 개로 끝내는 것이 목표다

대부분의 mutex는 경쟁이 없을 때 userspace CAS로 lock을 획득하고 release store로 푼다. 매 lock마다 syscall을 하면 짧은 critical section에서 비용이 너무 크기 때문이다. Kernel은 실제로 기다려야 할 때만 slow path에 참여한다.

Fast path 최적화는 branch prediction과 cache-line ownership에 민감하다. Lock variable이 여러 core 사이를 이동하면 CAS 자체보다 coherence round-trip이 더 비쌀 수 있다.

Benchmark는 single-thread uncontended 결과와 multi-thread contended 결과를 분리한다. Fast path 10% 개선이 실제 서비스에서 lock queue가 길다면 큰 의미가 없을 수 있다.

## CHAPTER 03 · mutex acquire/release는 protected memory의 happens-before 경계를 만든다

Lock은 mutual exclusion만 제공하는 것이 아니다. Thread A가 critical section에서 쓴 data는 unlock의 release semantics를 통해, 이후 같은 lock을 acquire한 thread B에게 보이도록 ordering을 제공한다. Atomic flag만 바꾸고 proper barrier가 없으면 보호된 data가 stale하게 보일 수 있다.

CPU architecture마다 weak ordering 정도가 다르므로 source-level memory model과 ABI/library guarantee를 따른다. Hand-written spinlock은 특히 acquire/release primitive를 정확히 사용해야 한다.

Race detector가 lock을 synchronization edge로 인식하는 이유도 이 semantics 때문이다. Custom lock을 만들면 tooling annotation이 필요할 수 있다.

## CHAPTER 04 · contended mutex는 owner뿐 아니라 waiter 상태를 잃지 않아야 한다

Thread가 lock 획득에 실패하면 자신이 기다린다는 사실을 등록하고 sleep으로 전환할 수 있다. Unlocker는 waiter가 있음을 알아야 적절히 wake한다. 이 handshake가 잘못되면 unlock과 sleep 사이 race로 wakeup을 놓쳐 thread가 영원히 잠들 수 있다.

Waiter count를 정확히 저장하지 않고 hint만 쓰는 구현도 있지만 false positive wake는 허용하면서 lost wake는 금지하는 식의 invariant가 필요하다.

Stress test는 lock/unlock과 waiter sleep transition을 매우 자주 교차시켜야 한다. 평온한 workload로는 이 좁은 race window를 찾기 어렵다.

## CHAPTER 05 · futex는 userspace state가 기대값일 때만 kernel에서 sleep하는 primitive다

Futex wait는 memory word가 특정 값인지 kernel 진입 시 다시 확인한 뒤 조건이 맞을 때 sleep한다. Userspace CAS와 kernel wait를 연결해 uncontended case에는 syscall을 피하고 contended case에는 CPU를 낭비하지 않게 한다.

Kernel은 mutex의 전체 의미를 모를 수 있다. Futex word와 wake protocol을 library가 정의하고 kernel은 wait queue를 제공한다. 그래서 futex를 직접 쓰는 code는 lock state machine 전체를 스스로 설계해야 한다.

Spurious wake와 signal interruption을 처리하고 condition을 loop에서 재확인한다. `wake`가 곧 lock ownership transfer를 보장하는 것은 아니다.

## CHAPTER 06 · futex wait race는 condition check와 sleep 등록 사이의 lost-wakeup을 막아야 한다

Thread가 lock이 busy임을 확인한 직후 owner가 unlock하고 wake했는데, waiter가 아직 kernel queue에 들어가지 않았다면 단순 sleep API는 wake를 잃는다. Futex wait가 userspace word를 kernel에서 다시 비교하는 이유가 이 gap을 닫기 위해서다.

Expected value가 이미 바뀌었다면 sleep하지 않고 즉시 돌아가 userspace에서 retry한다. 이 atomic check-and-sleep 성질이 protocol의 핵심이다.

Custom synchronization을 설계할 때 condition variable과 event에도 같은 lost-wakeup 문제를 적용해 생각한다. “check 후 sleep” 두 동작 사이를 어떻게 연결하는지가 중요하다.

## CHAPTER 07 · spurious wakeup을 허용하면 구현은 단순해지지만 caller가 predicate loop를 가져야 한다

Kernel wake, signal, implementation policy 때문에 wait가 condition 충족 없이 반환할 수 있다. 따라서 waiter는 깨어난 뒤 protected predicate를 다시 검사해야 한다. `if (!ready) wait()`보다 `while (!ready) wait()`가 기본 패턴인 이유다.

Spurious wake를 버그로 간주해 모든 원인을 제거하려 하면 protocol이 복잡해질 수 있다. 대신 correctness가 wake count와 무관하도록 설계한다.

Test에서는 의도적으로 불필요한 wake를 많이 발생시켜 predicate recheck가 안전한지 본다. Wake storm이 성능에 미치는 영향도 별도다.

## CHAPTER 08 · wake-one과 wake-all은 latency·fairness·thundering herd를 교환한다

하나의 waiter만 깨우면 불필요한 context switch를 줄일 수 있지만 어떤 waiter를 선택할지 fairness 문제가 생긴다. 모두 깨우면 빠르게 progress 가능한 thread를 찾을 수 있지만 한 lock을 두고 많은 thread가 동시에 경쟁하는 thundering herd가 생긴다.

Condition variable처럼 여러 predicate가 섞이면 wake-one이 적절한 waiter를 깨우지 못할 수 있다. Mutex unlock에서는 보통 하나의 successor면 충분하다.

Wait queue length와 wake-to-acquire 성공률을 측정하면 policy 비용을 볼 수 있다. 단순 wake syscall 수보다 실제 progress를 본다.

## CHAPTER 09 · spin과 park의 선택은 예상 critical-section 길이에 달려 있다

Owner가 곧 lock을 풀 것이라면 waiter가 잠깐 spin하는 편이 syscall과 context switch보다 싸다. 반대로 owner가 deschedule됐거나 critical section이 길면 spin은 CPU를 낭비한다. Adaptive mutex는 runtime 상태를 이용해 둘 사이를 선택한다.

Spin budget은 CPU topology와 load에 따라 달라진다. Idle core가 많은 system과 oversubscribed VM에서 같은 횟수가 적합하지 않다.

Metrics에 spin cycles와 park count를 둔다. 높은 spin 비율이 throughput 개선인지 단순 power 낭비인지 확인한다.

## CHAPTER 10 · SMT sibling 위의 spin은 owner가 쓸 execution resource를 빼앗을 수 있다

Waiter와 lock owner가 같은 physical core의 SMT sibling에 있으면 waiter의 aggressive spin이 owner와 execution resources를 경쟁할 수 있다. Lock을 빨리 풀어야 할 owner를 방해해 오히려 wait time을 늘리는 역설이 생긴다.

CPU pause/yield instruction과 scheduler hint는 이런 contention을 완화할 수 있다. 하지만 architecture별 의미가 다르다.

SMT on/off 또는 sibling placement를 바꾼 benchmark로 spin policy를 검증한다. Logical CPU 수만 보고 spin을 설계하지 않는다.

## CHAPTER 11 · park path는 lock 대기를 scheduler wait로 변환해 CPU를 다른 task에 양보한다

Waiter가 futex sleep에 들어가면 runnable queue에서 빠져 CPU를 소비하지 않는다. Unlocker가 wake하면 다시 runnable이 되고 scheduler가 CPU를 배정한다. 이 과정에는 syscall, wait-queue, scheduler latency가 포함된다.

따라서 매우 짧은 contention에는 비쌀 수 있지만 긴 wait에서는 효율적이다. Tail latency는 wake 후 runnable-to-running delay의 영향을 받는다.

Lock profile에서 blocked time과 scheduler delay를 분리하면 owner critical section이 긴지, 시스템 CPU contention이 큰지 구분할 수 있다.

## CHAPTER 12 · adaptive mutex는 최근 owner 상태와 contention을 이용해 spin budget을 바꾼다

Owner가 현재 다른 CPU에서 실행 중이라면 곧 unlock할 가능성이 있어 잠깐 spin할 가치가 있다. Owner가 sleep/descheduled 상태라면 바로 park하는 편이 낫다. Adaptive policy는 이런 signal을 이용한다.

하지만 owner tracking 자체의 비용과 stale 정보가 있다. Virtualization에서는 vCPU가 실행 중처럼 보여도 host에서 deschedule될 수 있다.

Policy를 튜닝할 때 synthetic uncontended benchmark만 보지 않는다. 실제 critical-section duration distribution과 CPU oversubscription 조건을 포함한다.

## CHAPTER 13 · ticket lock은 획득 순서를 명확히 해 fairness를 얻지만 공유 cache line을 계속 본다

Ticket lock은 각 waiter가 번호를 받고 현재 serve 번호가 자기 차례가 될 때까지 기다린다. FIFO fairness가 단순하지만 모든 waiter가 같은 serve counter를 반복 읽어 contention이 커질 수 있다.

짧은 kernel-style critical section에는 유용할 수 있지만 많은 socket/core가 경쟁하면 cache coherence traffic이 증가한다. Waiting thread 수가 늘어도 한 cache line이 hotspot이다.

Fairness가 중요한지 throughput이 중요한지에 따라 lock 선택이 달라진다. FIFO가 항상 가장 빠른 것은 아니다.

## CHAPTER 14 · queue lock은 각 waiter가 자기 node를 기다리게 해 cache-line contention을 분산한다

MCS 같은 queue lock은 waiter가 linked node를 만들고 predecessor와 successor 관계로 ownership을 전달한다. 각 thread가 local flag를 spin해 global lock word polling을 줄일 수 있어 large SMP에서 scale이 좋다.

대신 per-waiter node lifetime과 queue manipulation이 복잡하다. Cancellation이나 timeout을 지원하려면 중간 node 제거 protocol이 어려워진다.

Lock algorithm은 uncontended cost와 high-contention scalability를 함께 평가한다. Small core system에서 queue bookkeeping이 더 비쌀 수도 있다.

## CHAPTER 15 · direct handoff는 unlocker가 특정 waiter에게 ownership을 넘겨 barging을 막는다

Unlock 시 lock을 완전히 free 상태로 만들지 않고 queue의 다음 waiter를 owner로 지정하면 새로 도착한 thread가 앞질러 가는 것을 막을 수 있다. Fairness와 starvation 방지에 유리하지만 새 waiter가 이미 running 상태라면 throughput 기회를 놓칠 수 있다.

Handoff 받은 waiter가 scheduler에서 늦게 실행되면 lock이 사실상 idle인데 다른 thread가 못 들어가는 convoy가 생길 수 있다.

Scheduler latency와 fairness 목표를 함께 봐야 한다. Lock semantics만으로 최적 policy를 정할 수 없다.

## CHAPTER 16 · barging은 새로 도착한 running thread가 잠든 waiter보다 먼저 lock을 잡게 허용한다

Unlock 후 lock을 free로 만들면 CPU에서 이미 실행 중인 thread가 깨워진 waiter보다 먼저 acquire할 수 있다. Context switch를 줄여 throughput을 높일 수 있지만 같은 thread가 반복적으로 이겨 오래 기다리는 waiter가 starvation될 수 있다.

Work-conserving throughput과 strict fairness 사이의 trade-off다. Interactive latency와 batch throughput이 다른 선택을 요구할 수 있다.

Wait time distribution과 max starvation time을 측정한다. 평균 acquisition latency만 보면 일부 waiter의 긴 tail을 놓친다.

## CHAPTER 17 · lock convoy는 느린 owner 하나가 waiter 줄 전체의 latency를 끌어올리는 현상이다

많은 thread가 한 mutex 뒤에 줄을 서면 owner가 deschedule되거나 page fault를 만났을 때 모두 함께 기다린다. Lock 자체의 CPU cost보다 critical section 안에서 blocking operation을 하는 것이 더 큰 문제다.

Critical section에서 I/O, allocation, logging을 제거하거나 lock scope를 줄이면 convoy를 완화할 수 있다. Lock algorithm 변경만으로 owner stall을 해결할 수는 없다.

Trace에서 owner가 lock을 가진 시간과 off-CPU reason을 기록한다. Waiter count만 보고 contention 원인을 판단하지 않는다.

## CHAPTER 18 · priority inversion은 낮은 우선순위 owner가 높은 우선순위 waiter를 간접적으로 막는 문제다

High-priority task가 low-priority task가 가진 lock을 기다리는 동안 medium-priority task가 CPU를 계속 사용하면 owner가 실행되지 못해 high task도 지연된다. 단순 mutex는 scheduler priority 관계를 모르므로 이 inversion이 길어질 수 있다.

Realtime workload에서는 bounded blocking이 필요해 priority inheritance나 ceiling protocol을 사용한다. 일반 server에서도 latency-sensitive thread와 background thread가 lock을 공유하면 비슷한 tail 문제가 생길 수 있다.

Thread priority 조정만으로 해결하지 말고 lock dependency graph를 본다.

## CHAPTER 19 · priority inheritance는 waiter의 높은 priority를 owner에게 임시로 전달한다

PI mutex에서 high-priority waiter가 blocked되면 owner가 그 priority를 상속받아 빨리 실행되고 lock을 release하도록 한다. Nested lock chain에서는 inheritance가 여러 owner를 따라 전파될 수 있다.

PI에는 kernel scheduler와 lock implementation의 협력이 필요하며 bookkeeping cost가 있다. 모든 mutex에 무조건 적용하기보다 realtime requirement가 있는 경계에 사용한다.

테스트는 low owner, medium CPU hog, high waiter 시나리오를 만들어 inversion bound가 실제로 줄어드는지 확인한다.

## CHAPTER 20 · priority inheritance는 deadlock을 해결하지 않는다

PI는 owner가 CPU를 못 받아 생기는 priority inversion을 완화하지만 A→B와 B→A 순서로 서로 lock을 기다리는 cycle은 그대로다. Priority를 올려도 누구도 release할 수 없다.

Deadlock은 lock ordering, try-lock/backoff, cycle detection 같은 별도 설계가 필요하다. PI가 있는 시스템에서 “priority 문제겠지”라고 deadlock diagnosis를 늦추지 않는다.

Lock graph와 blocked stack을 수집해 wait cycle을 직접 확인한다.

## CHAPTER 21 · priority ceiling은 lock이 보호하는 resource의 최대 priority를 미리 반영한다

Ceiling protocol은 lock을 획득한 task의 effective priority를 정해진 ceiling까지 높이거나, 특정 순서 규칙으로 nested lock blocking을 제한한다. Realtime system에서 worst-case blocking 분석에 유용하다.

올바른 ceiling 값을 정하려면 어떤 task가 해당 resource를 사용할 수 있는지 알아야 한다. Task set이 변하면 configuration도 갱신해야 한다.

일반-purpose application에는 복잡도가 크므로 필요성이 명확한 경우 사용한다. Semantics를 이해하지 못한 채 priority 값만 높이면 scheduler fairness를 해칠 수 있다.

## CHAPTER 22 · robust mutex는 owner process/thread가 죽은 상태를 다음 acquirer에게 노출한다

Shared-memory mutex의 owner가 critical section 중 죽으면 lock bit만 남아 영원히 잠길 수 있다. Robust mutex는 kernel이 owner death를 추적해 다음 waiter에게 special 상태를 알려 recovery 기회를 준다.

Lock을 얻었다고 protected data가 자동으로 일관된 것은 아니다. Owner가 중간에 죽었으므로 application이 data invariant를 복구한 뒤 mutex를 consistent 상태로 표시해야 한다.

Recovery가 불가능하면 resource를 unusable로 표시하고 상위 계층에서 재생성한다. Owner-dead 신호를 정상 acquisition으로 무시하지 않는다.

## CHAPTER 23 · owner-dead 상태는 lock recovery와 data recovery를 연결하는 명시적 경계다

다음 thread가 `owner died`를 받으면 어떤 update가 partial인지 판단해야 한다. Transactional metadata, generation, journal이 없다면 일반 memory structure는 복구할 방법이 없을 수 있다.

Robust mutex는 crash를 감지할 뿐 business invariant를 알려주지 않는다. Protected state가 recovery 가능한 형태로 설계돼 있어야 한다.

Fault test에서 owner를 critical section 여러 위치에서 강제 종료하고 successor가 state를 일관되게 복구하거나 안전하게 폐기하는지 확인한다.

## CHAPTER 24 · robust futex list는 kernel이 죽은 task가 소유한 futex를 찾도록 돕는다

Userspace mutex가 owner list에 자신을 등록하면 kernel은 thread exit 시 해당 list를 따라가 owner-death bit를 표시하고 waiter를 깨울 수 있다. 이 protocol도 userspace memory가 손상되었을 가능성을 고려해 bounded traversal이 필요하다.

Library가 제공하는 robust mutex를 쓰면 세부 구현을 직접 다루지 않아도 되지만 semantics는 이해해야 한다. Fork, shared memory, process-shared option과 결합 시 behavior를 확인한다.

Custom futex protocol로 robust behavior를 재구현하는 것은 매우 높은 검증 비용을 요구한다.

## CHAPTER 25 · recursive mutex는 ownership count를 추가해 편리함과 구조적 위험을 동시에 만든다

같은 thread가 mutex를 다시 acquire할 수 있게 recursion count를 두면 callback나 layered API에서 self-deadlock을 피할 수 있다. 하지만 lock scope와 reentrancy 문제가 숨겨져 설계 오류를 가릴 수 있다.

Unlock 횟수가 acquire와 정확히 맞아야 실제 release된다. Error path에서 한 번 빠뜨리면 lock이 영원히 남는다.

가능하면 dependency를 재구성해 non-recursive lock을 유지하고, recursive가 필요하다면 이유와 maximum nesting을 문서화한다.

## CHAPTER 26 · rwlock은 reader가 많다는 이유만으로 mutex보다 항상 빠르지 않다

Read-write lock은 여러 reader를 동시에 허용하지만 state 관리와 fairness가 더 복잡하다. Critical section이 짧거나 write가 자주 발생하면 reader bookkeeping과 cache-line contention 때문에 mutex보다 느릴 수 있다.

Reader preference는 writer starvation을, writer preference는 reader latency를 만들 수 있다. Upgrade/downgrade semantics도 deadlock 위험이 있다.

Read/write 비율뿐 아니라 critical section cost와 core count를 포함한 benchmark로 선택한다.

## CHAPTER 27 · lock striping은 하나의 global lock을 여러 shard lock으로 분할한다

Hash bucket이나 object shard별 lock을 두면 독립 operation이 다른 lock을 사용해 contention을 줄일 수 있다. 하지만 multi-key operation은 여러 lock을 잡아야 해 ordering과 deadlock 문제가 생긴다.

Stripe 수를 늘리면 parallelism은 높아지지만 memory와 lock-management cost가 증가한다. Hot key가 하나의 stripe에 몰리면 전체 평균은 낮아도 hotspot이 남는다.

Metrics를 lock instance별로 수집해 skew를 확인한다. Global average wait만으로는 hot stripe를 찾기 어렵다.

## CHAPTER 28 · lock observability는 wait time뿐 아니라 owner와 critical-section 시간을 연결해야 한다

어떤 mutex에서 시간이 많이 막히는지 알기 위해 contention count, total wait, max wait를 기록할 수 있다. 더 중요한 것은 그 시점 owner가 무엇을 하고 있었는지다. Owner stack과 hold duration이 있어야 개선 방향을 찾는다.

모든 lock event를 고비용 tracing하면 perturbation이 크므로 sampling이나 threshold 기반 기록을 사용한다. Lock class/id가 stable해야 build 간 비교가 가능하다.

Dashboard에는 contention rate와 request latency를 같이 둔다. Lock wait이 늘어도 사용자 영향이 없는 background path일 수 있다.

## CHAPTER 29 · lock profiling 자체가 timing과 contention을 바꿀 수 있다

Timestamp와 stack capture를 매 acquire마다 수행하면 critical section 주변에 새로운 shared state와 syscall을 추가해 측정 대상 behavior를 바꿀 수 있다. 특히 짧은 uncontended lock에서는 profiler overhead가 원래 cost보다 클 수 있다.

Sampling rate를 낮추고 hardware/perf event와 결합해 perturbation을 줄인다. Profiler on/off 결과 차이도 측정한다.

“프로파일하면 문제가 사라진다”는 현상은 race/timing-sensitive bug의 단서다. Instrumentation을 제거하기보다 영향 자체를 증거로 사용한다.

## CHAPTER 30 · lock 선택은 uncontended cost보다 ownership transfer와 failure model로 결정한다

일반 mutex, adaptive spin, queue lock, PI mutex, robust mutex는 서로 다른 문제를 해결한다. Critical section 길이, contention, scheduler priority, owner crash 가능성, process-shared 여부를 기준으로 선택해야 한다.

Lock 성능은 fast path ns만 재지 않는다. Wait distribution, spin CPU, park/wake, owner hold time, starvation을 함께 본다. Correctness 측면에서는 memory ordering과 lost-wakeup 방지가 우선이다.

최종 contract는 “누가 owner이며, 경쟁 시 다음 owner는 어떻게 정해지고, owner가 사라지면 protected state를 어떻게 처리하는가”로 요약된다. 이 세 질문에 답할 수 있어야 lock implementation을 신뢰할 수 있다.
