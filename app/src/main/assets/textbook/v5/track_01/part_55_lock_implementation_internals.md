# PART 55 · Lock Implementation Internals — spin, park, futex, handoff, priority

Lock의 성능과 정확성은 `잠겼다/풀렸다` 두 상태만으로 설명되지 않는다. 실제 구현은 **소유권 표현, fast path 원자 연산, 경합 감지, waiter 등록, sleep/wakeup, handoff 정책, scheduler priority, owner failure**를 하나의 protocol로 연결한다. 이 protocol이 잘못되면 mutual exclusion은 지켜져도 tail latency, starvation, priority inversion, convoy, crash recovery가 무너질 수 있다.

## CHAPTER 01 · Lock word는 단순 boolean이 아니라 protocol state다

고성능 mutex의 사용자 공간 상태는 unlocked/locked 외에 waiter 존재, owner TID, recursion count, handoff flag 같은 정보를 bit field나 별도 metadata에 담을 수 있다. 중요한 invariant는 **각 bit를 누가 어떤 memory ordering으로 바꾸며 kernel state와 어떻게 대응시키는가**다. 사용자 공간 값과 kernel wait queue가 어긋나면 lost wakeup이나 영구 sleep이 생긴다. 따라서 lock word는 data가 아니라 userspace와 kernel이 공유하는 synchronization protocol의 state encoding이다.

## CHAPTER 02 · Uncontended fast path는 syscall을 피하는 데 목적이 있다

경합이 없는 mutex는 atomic compare-and-swap 같은 read-modify-write로 unlocked→owned 전이를 끝낼 수 있다. 이 경로는 kernel 진입, scheduler queue 조작, context switch를 피한다. 그러나 fast path가 싸다는 이유로 atomic instruction 자체가 공짜가 되는 것은 아니다. Cache line ownership 획득과 coherence traffic이 발생하며 여러 core가 같은 lock word를 반복 갱신하면 lock body보다 coherence 비용이 지배할 수 있다.

## CHAPTER 03 · Acquire와 release ordering이 critical section 경계를 만든다

Lock 획득은 이후 load/store가 critical section 밖으로 앞당겨져 보이지 않게 하는 acquire ordering을, unlock은 이전 critical-section write가 unlock 뒤로 밀려 보이지 않게 하는 release ordering을 요구한다. Mutual exclusion bit만 atomic하게 바꿔도 memory-order contract가 틀리면 보호 데이터의 publication이 깨질 수 있다. 따라서 lock 구현의 correctness proof에는 ownership state와 protected-data visibility가 함께 들어가야 한다.

## CHAPTER 04 · 경합 여부를 모르면 unlock도 불필요한 syscall을 만들 수 있다

Owner가 unlock할 때 waiter가 없는 경우 kernel을 깨울 이유가 없다. 그래서 lock word에 waiter bit나 state를 남겨 fast unlock과 contended unlock을 구분한다. 반대로 waiter가 있는데도 waiter bit를 잃으면 wake가 생략되어 sleeper가 영구히 남을 수 있다. 이 때문에 waiter 표시 전이와 sleep 진입 사이에는 race-free protocol이 필요하다.

## CHAPTER 05 · Futex는 userspace state와 kernel wait queue를 연결한다

Linux futex의 핵심은 lock state 자체를 kernel object로 항상 유지하지 않는 데 있다. 비경합 경로는 userspace atomic operation만으로 끝나고, contention이 생겼을 때 특정 userspace address를 key로 kernel wait queue를 사용한다. 따라서 futex를 `kernel mutex`라고 부르면 중요한 비용 구조를 놓친다. **state ownership은 userspace에 있고 kernel은 wait/wake arbitration을 필요할 때만 제공**한다.

## CHAPTER 06 · FUTEX_WAIT는 값 검사를 포함해야 lost wakeup을 피할 수 있다

Waiter는 lock이 여전히 기대한 contended state인지 확인한 뒤 sleep해야 한다. 값 확인 없이 단순히 queue에 들어가면 owner가 이미 unlock+wake를 끝낸 뒤 waiter가 늦게 잠드는 race가 생길 수 있다. Futex wait가 userspace value와 expected value를 비교하는 이유는 sleep transition을 lock state와 결합해 stale wait를 차단하기 위해서다.

## CHAPTER 07 · Spurious wakeup을 허용하면 wait loop는 predicate 중심으로 설계된다

Wakeup은 곧 ownership 획득을 뜻하지 않는다. Signal, implementation choice, competing waiter 때문에 깨어난 thread가 lock을 얻지 못할 수 있다. 따라서 waiter는 `깨어남`을 success condition으로 쓰지 않고 **protected predicate 또는 lock acquisition을 다시 검사**해야 한다. 이 원칙은 mutex뿐 아니라 condition variable, event wait, channel receive 같은 blocking primitive에 공통이다.

## CHAPTER 08 · Wake-one과 wake-many는 fairness와 herd 비용을 바꾼다

Unlock 때 모든 waiter를 깨우면 여러 CPU가 동시에 runnable이 되어 같은 lock word를 두고 다시 경쟁한다. 이는 thundering herd, cache-line ping-pong, scheduler overhead를 만든다. 한 명만 깨우면 herd를 줄일 수 있지만 잘못된 waiter selection이나 handoff 정책은 fairness를 악화시킬 수 있다. Wake policy는 단순 optimization이 아니라 runnable population과 ownership transfer를 제어하는 scheduler-facing 정책이다.

## CHAPTER 09 · Spin은 sleep/wakeup 비용보다 남은 hold time이 짧을 때만 유리하다

Spin lock 또는 mutex의 adaptive spin은 owner가 곧 unlock할 가능성이 있을 때 context switch를 피한다. 하지만 owner가 descheduled되었거나 critical section이 길면 spinning CPU는 진전 없이 execution slot과 전력을 소비한다. 따라서 spin count를 고정 상수로만 결정하면 workload와 topology 변화에 취약하다. Owner-running 여부, past hold time, CPU count 같은 signal을 활용하는 adaptive 전략이 필요한 이유다.

## CHAPTER 10 · SMT sibling에서 spin은 같은 core의 owner를 방해할 수 있다

SMT 환경에서는 waiter와 owner가 같은 physical core의 sibling hardware thread에 배치될 수 있다. Waiter가 공격적으로 spin하면 execution port, cache bandwidth, frontend 자원을 owner와 경쟁해 오히려 unlock을 늦출 수 있다. `spin은 context switch보다 싸다`는 판단은 core topology를 무시하면 틀린다. Lock tuning은 logical CPU 수가 아니라 physical-core sharing까지 봐야 한다.

## CHAPTER 11 · Park는 waiter를 runnable set에서 제거한다

Parking은 waiter를 scheduler가 실행 후보로 계속 다루지 않도록 sleep state로 옮긴다. 이 선택은 CPU 낭비를 줄이지만 kernel entry, wait-queue 관리, wakeup, runqueue insertion, scheduling delay를 추가한다. 즉 contended mutex latency는 `unlock 시점`이 아니라 **unlock→wake→runnable→scheduled→reacquire** 전체 경로로 측정해야 한다.

## CHAPTER 12 · Adaptive mutex는 spin과 park 사이의 정책 엔진이다

Adaptive mutex는 짧은 hold에 대한 spin 이득과 긴 hold에서의 CPU 낭비를 절충한다. 중요한 것은 `N번 돌고 잠든다`라는 숫자가 아니라 어떤 관측값으로 N을 조정하는가다. Owner가 현재 CPU에서 실행 중인지, 최근 hold-time distribution이 어떤지, contention depth가 얼마나 되는지에 따라 최적 지점이 달라진다. 정책이 workload phase 변화를 따라가지 못하면 평균 latency가 좋아도 p99가 악화될 수 있다.

## CHAPTER 13 · Ticket lock은 순서를 주지만 cache traffic을 없애지 않는다

Ticket lock은 각 waiter가 번호를 받고 serving counter가 자신의 번호가 될 때까지 기다리는 방식으로 FIFO 성격을 만든다. Fairness는 좋아질 수 있지만 모든 waiter가 같은 serving cache line을 계속 읽으므로 많은 core에서 broadcast invalidation/traffic이 커질 수 있다. 공정성과 scalability는 동일한 목표가 아니다.

## CHAPTER 14 · Queue lock은 waiter별 local spinning으로 coherence pressure를 줄인다

MCS류 queue lock은 waiter를 linked queue로 연결하고 각 waiter가 자신의 local node 상태를 spin하도록 설계할 수 있다. 이 구조는 많은 waiter가 하나의 lock word를 두드리는 global spinning을 줄인다. 대신 per-waiter node lifetime, enqueue/dequeue atomicity, predecessor failure 같은 추가 invariant가 생긴다. Lock choice는 critical-section 길이뿐 아니라 contention width와 coherence topology에 따라 달라진다.

## CHAPTER 15 · Direct handoff와 competitive reacquire는 다른 latency profile을 만든다

Unlock owner가 특정 waiter에게 ownership을 직접 넘기면 새 entrant가 앞질러 lock을 훔치는 barging을 줄이고 fairness를 높일 수 있다. 반대로 ownership을 free 상태로 만들고 모든 runnable contender가 경쟁하게 두면 throughput은 좋아질 수 있지만 unlucky waiter가 반복해서 밀릴 수 있다. Handoff 정책은 **fairness, cache locality, wakeup latency, convoy risk**를 동시에 바꾼다.

## CHAPTER 16 · Barging은 throughput을 높여도 starvation 가능성을 만든다

방금 CPU에서 실행 중인 thread가 깨어나지 않은 waiter보다 먼저 lock을 다시 획득하면 scheduler wakeup 비용을 숨기고 cache locality를 얻을 수 있다. 그러나 새 entrant가 계속 유입되면 기존 waiter가 장기간 ownership을 못 받을 수 있다. 따라서 barging을 허용하는 구현은 starvation bound가 있는지, priority policy와 충돌하지 않는지 별도로 검증해야 한다.

## CHAPTER 17 · Convoy는 하나의 느린 owner가 waiter 전체를 직렬화하는 현상이다

Preempt된 owner, page fault, blocking I/O, GC safepoint 같은 사건이 critical section 안에서 발생하면 뒤 waiter가 모두 같은 lock에 묶인다. Owner가 다시 실행된 뒤에도 waiter wakeup과 handoff가 연쇄되어 throughput이 회복되는 데 시간이 걸릴 수 있다. Lock convoy를 줄이려면 lock algorithm보다 먼저 **critical section 안에서 blocking·faulting 가능한 작업을 제거**해야 하는 경우가 많다.

## CHAPTER 18 · Priority inversion은 lock dependency와 scheduler priority가 만나는 문제다

High-priority task가 low-priority owner의 lock을 기다리는 동안 medium-priority task들이 low-priority owner를 계속 preempt하면 high-priority task는 간접적으로 medium task보다 낮은 service를 받는다. Priority inversion은 mutex 내부 현상만이 아니라 scheduler ordering과 ownership dependency의 결합이다. Real-time path에서는 평균 hold time보다 최악 block time과 dependency chain이 중요하다.

## CHAPTER 19 · Priority inheritance는 owner priority를 dependency를 따라 임시 승격한다

PI lock에서는 높은 priority waiter가 낮은 priority owner에 막히면 owner priority를 임시로 높여 unlock까지 실행 기회를 확보한다. Owner가 또 다른 lock에 막혀 있으면 inheritance가 chain을 따라 전파되어야 inversion을 끊을 수 있다. Linux PI futex가 kernel RT-mutex와 결합되는 이유도 이 dependency graph를 scheduler가 볼 수 있게 하기 위해서다.

## CHAPTER 20 · Priority inheritance는 deadlock을 해결하지 않는다

PI는 runnable order를 조정하지만 cyclic lock dependency를 제거하지 않는다. A가 L1을 잡고 L2를 기다리며 B가 L2를 잡고 L1을 기다리면 priority를 아무리 올려도 progress가 없다. 따라서 real-time mutex 설계에서도 **lock ordering, bounded nesting, deadlock detection**은 별도 correctness condition이다.

## CHAPTER 21 · Priority ceiling은 inheritance와 다른 예방 전략이다

Priority-ceiling protocol은 lock마다 해당 resource를 사용할 task 집합에 맞는 ceiling을 두고 acquisition 시 priority를 규칙에 따라 제한/상향한다. 이 방식은 특정 inversion과 deadlock pattern을 예방할 수 있지만 configuration correctness에 의존한다. Runtime observed waiter를 따라 반응하는 inheritance와 달리, ceiling은 사전에 resource-sharing graph를 알아야 하는 설계다.

## CHAPTER 22 · Robust mutex는 owner death를 state-recovery protocol로 노출한다

Owner가 mutex를 잡은 채 종료하면 단순 unlock으로 간주할 수 없다. Protected data가 update 중간 상태일 수 있기 때문이다. Robust mutex는 다음 acquirer에게 owner death를 알리고, caller가 data를 검사·복구한 뒤 consistent 상태를 명시하도록 한다. 즉 robust mutex는 dead process cleanup 기능이 아니라 **lock ownership failure를 application recovery responsibility로 전달하는 protocol**이다.

## CHAPTER 23 · EOWNERDEAD는 lock acquisition 성공과 data consistency 실패를 동시에 뜻한다

Robust mutex에서 다음 thread는 lock ownership을 얻으면서도 `EOWNERDEAD`를 받을 수 있다. 따라서 `nonzero return = lock을 못 얻음`이라는 일반화는 틀린다. Caller는 protected structure의 invariant를 복구하고 consistency를 선언해야 한다. 복구 불가능한 상태를 정상 데이터처럼 계속 쓰면 synchronization primitive는 살아 있어도 application state가 손상된다.

## CHAPTER 24 · Robust futex list는 kernel이 task exit 때 owner-held lock을 찾게 한다

Fast futex의 장점은 uncontended 상태를 kernel이 추적하지 않는 데 있지만, 이것은 owner crash 시 kernel이 어떤 lock을 들고 있었는지 모른다는 뜻이기도 하다. Robust futex ABI는 thread가 보유 lock 목록의 위치를 kernel에 알려 task exit 때 owner-death 표시와 wake 처리를 가능하게 한다. Performance와 crash recoverability 사이에 추가 metadata protocol이 필요한 이유다.

## CHAPTER 25 · Recursive mutex는 lock-order bug를 숨길 수 있다

Recursive mutex는 동일 thread의 재획득을 count로 허용한다. Reentrant API에는 필요할 수 있지만, 호출 계층이 lock ownership을 명확히 드러내지 않아 예상보다 긴 hold time과 lock-order cycle을 숨길 수 있다. Recursive depth가 증가하는 path는 critical-section boundary가 API abstraction 아래로 새는 신호일 수 있다.

## CHAPTER 26 · Reader-writer lock은 read-heavy라는 이유만으로 빠르지 않다

Read lock을 동시에 허용해도 reader count 갱신 자체가 shared cache line을 오염시킬 수 있고 writer arrival 시 reader drain/handoff가 필요하다. Writer preference는 reader starvation을, reader preference는 writer starvation을 만들 수 있다. 짧은 read section과 많은 core에서는 plain mutex가 오히려 더 낮은 coherence cost를 가질 수 있으므로 workload measurement가 필요하다.

## CHAPTER 27 · Lock striping은 하나의 contention domain을 여러 ownership domain으로 쪼갠다

전체 structure에 하나의 global lock을 두는 대신 key/hash/range 기준으로 여러 lock에 state를 분할하면 병렬성을 높일 수 있다. 그러나 multi-key operation은 여러 stripe를 동시에 잡아야 하므로 global lock order와 rebalance protocol이 필요하다. Striping은 contention을 없애는 것이 아니라 **dependency graph를 더 작은 domain으로 재구성**하는 설계다.

## CHAPTER 28 · Lock hold time과 wait time을 분리해 관측해야 한다

Lock이 병목인지 판단하려면 acquisition count만으로 부족하다. Hold duration, waiter count, blocked duration, owner deschedule, contention site, handoff latency를 구분해야 한다. Wait time이 길어도 hold time이 짧다면 wake/scheduler 경로가 원인일 수 있고, hold time 자체가 길면 critical section 구조가 원인이다. Profiling은 source line뿐 아니라 ownership timeline을 복원해야 한다.

## CHAPTER 29 · Lock profiling 자체가 synchronization timing을 바꿀 수 있다

모든 acquisition에 timestamp·stack capture를 넣으면 lock path가 길어지고 contention pattern이 달라진다. Sampling, threshold-based slow-lock capture, aggregate counters를 조합해 observer effect를 제한해야 한다. 특히 microsecond급 critical section에서는 profiler overhead가 measured hold time과 같은 크기가 될 수 있으므로 instrumentation cost를 별도로 측정해야 한다.

## CHAPTER 30 · Lock 선택은 progress·fairness·priority·failure recovery 계약의 선택이다

Mutex, spin lock, queue lock, reader-writer lock, PI mutex, robust mutex는 서로의 단순 상위호환이 아니다. 선택 전에 **critical-section 최대 길이, blocking 가능성, contention width, scheduler priority, owner crash 가능성, fairness bound, memory-order requirement, profiling evidence**를 명시해야 한다. 최고의 lock algorithm을 고르는 것이 아니라 해당 resource의 ownership protocol이 시스템 invariant와 failure model에 맞는지 증명하는 것이 최종 기준이다.
