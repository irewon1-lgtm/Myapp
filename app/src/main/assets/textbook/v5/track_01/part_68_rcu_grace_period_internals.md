# PART 68 · RCU Grace-Period Internals — readers, quiescent states, callbacks, stalls, expedited progress

RCU의 본질은 `lock 없이 읽는다`가 아니다. Reader가 매우 싼 critical section을 갖는 대신 updater는 **old readers가 모두 끝난 시점을 grace period로 판정하고, 그 뒤에만 old object를 reclaim**해야 한다. Correctness와 latency는 quiescent-state detection, callback queue, preemption, CPU hotplug, nohz, memory ordering, forward-progress 정책이 모두 맞아야 성립한다.

## CHAPTER 01 · RCU는 publication과 reclamation을 분리한다

Updater가 pointer를 새 object로 바꾸는 순간 logical update는 완료될 수 있지만 old object를 즉시 free할 수는 없다. 이미 old pointer를 읽은 readers가 아직 사용 중일 수 있기 때문이다. RCU는 `새 readers가 새 object를 봄`과 `old object를 reclaim 가능` 사이에 grace period를 둔다.

## CHAPTER 02 · Read-side critical section은 object lifetime에 대한 임시 claim이다

`rcu_read_lock()`과 unlock 사이 reader는 RCU-protected pointer를 dereference해 사용할 수 있다. Reader가 reference count를 증가시키지 않아도 updater는 pre-existing readers가 모두 section을 빠져나갈 때까지 free하지 않는다. Lifetime guarantee는 per-object refcount가 아니라 global grace-period protocol에서 나온다.

## CHAPTER 03 · Grace period는 특정 object가 아니라 reader generation의 종료다

`synchronize_rcu()`는 특정 pointer를 추적하지 않는다. 호출 시작 전 이미 존재하던 read-side critical sections가 모두 끝났음을 보장한다. 호출 이후 시작한 new readers는 old object를 볼 수 없도록 publication rule이 지켜졌다면 grace period를 막을 필요가 없다.

## CHAPTER 04 · Quiescent state는 그 CPU/task가 pre-existing reader를 더 이상 품지 않음을 뜻한다

Context switch, user mode, idle 등은 RCU flavor와 kernel config에 따라 quiescent state로 해석될 수 있다. 중요한 것은 `CPU가 쉬었다`가 아니라 **grace-period 시작 전에 있던 read-side section이 해당 execution context에 남아 있지 않다**는 증거다.

## CHAPTER 05 · Preemptible RCU는 reader가 task와 함께 잠들 수 있다

Preemption 가능한 kernel에서는 task가 RCU read-side critical section 안에서 preempt될 수 있다. CPU가 다른 task를 실행한다고 그 reader가 끝난 것이 아니다. Grace-period engine은 preempted reader task까지 추적해 section 종료를 기다려야 한다.

## CHAPTER 06 · Non-preemptible region은 quiescent-state detection을 지연시킬 수 있다

Interrupt/preemption disabled region이 길어지거나 kernel loop가 scheduling point를 지나지 않으면 CPU가 quiescent state를 보고하지 못한다. RCU stall은 data-structure bug가 아니라 scheduler/IRQ latency bug에서 시작될 수도 있다.

## CHAPTER 07 · TREE_RCU는 CPU 상태를 tree로 집계해 global bottleneck을 줄인다

수백 CPU가 하나의 global bitmap/lock에 quiescent state를 직접 보고하면 cache contention이 커진다. Tree 구조는 leaf `rcu_node`에서 CPU reports를 모아 상위 node로 aggregation한다. Scalability는 reader fast path뿐 아니라 grace-period bookkeeping topology에서도 필요하다.

## CHAPTER 08 · Grace-period sequence number는 과거와 현재 generation을 구분한다

RCU는 grace period 시작/종료 sequence를 사용해 callback이 어느 grace period를 기다려야 하는지, poll cookie가 완료됐는지 판단한다. Sequence wrap과 concurrent start/end를 안전하게 처리해야 `미래 grace period를 이미 완료`로 오판하지 않는다.

## CHAPTER 09 · call_rcu는 reclaim work를 callback queue에 연기한다

Updater는 old object에 `rcu_head`를 연결해 callback을 등록하고 바로 돌아올 수 있다. Callback은 필요한 grace period가 지난 뒤 실행되어 memory free나 후속 cleanup을 수행한다. Async callback은 updater latency를 줄이지만 queue backlog와 delayed cleanup을 만든다.

## CHAPTER 10 · Callback batching은 grace-period overhead를 여러 updates에 공유한다

각 delete마다 별도 grace period를 시작하면 system-wide coordination 비용이 커진다. 여러 callbacks를 같은 grace period generation에 묶으면 overhead를 amortize할 수 있다. 대신 callback reclamation latency가 늘 수 있어 memory pressure와 batch efficiency를 함께 봐야 한다.

## CHAPTER 11 · Lazy callback은 power와 reclaim latency를 교환한다

Idle/light-load system에서 callback 처리를 일부 지연하면 불필요한 wakeup과 grace-period activity를 줄일 수 있다. 그러나 queued object memory가 더 오래 남는다. `call_rcu_hurry` 같은 path는 더 빠른 callback progress를 요구하는 대신 power/CPU overhead를 받아들인다.

## CHAPTER 12 · Callback backlog는 logical free와 physical free의 차이를 키운다

Application/subsystem이 object를 unlink했어도 callback이 밀리면 memory는 계속 retained된다. Allocation profiler에서 leak처럼 보일 수 있다. RCU callback count, bytes pending, grace-period latency를 함께 관찰해야 실제 leak과 deferred reclamation을 구분할 수 있다.

## CHAPTER 13 · synchronize_rcu는 callback completion과 같은 primitive가 아니다

Grace period가 끝났다고 이미 등록한 모든 callbacks가 CPU에서 실행 완료됐다는 보장은 없다. Callback invocation은 별도 scheduling/batching 단계다. `old readers 없음`과 `deferred destructor 실행 완료`를 구분해야 shutdown ordering을 올바르게 만들 수 있다.

## CHAPTER 14 · rcu_barrier는 이미 등록된 callbacks의 실행 완료를 기다린다

Module unload처럼 callback function code 자체를 제거하려면 outstanding RCU callbacks가 모두 끝났는지 확인해야 한다. `rcu_barrier()`는 pre-existing callbacks를 기다리는 목적이며 새 grace period 한 번을 기다리는 것과 semantic이 다르다. 둘을 혼동하면 unload 후 callback이 freed code를 실행할 수 있다.

## CHAPTER 15 · Expedited grace period는 latency를 줄이기 위해 더 적극적으로 CPUs를 흔든다

`synchronize_rcu_expedited()`는 ordinary grace period보다 빠른 completion을 목표로 inter-processor actions를 더 적극적으로 사용할 수 있다. 이는 update latency를 줄이지만 CPU overhead, realtime jitter, energy를 증가시킨다. Expedited를 default로 쓰는 것은 read-mostly scalability의 장점을 잠식할 수 있다.

## CHAPTER 16 · Force-quiescent-state는 holdout CPU에게 progress pressure를 준다

Grace period가 오래 걸리면 RCU는 holdout CPUs가 quiescent state를 보고하도록 reschedule pressure나 interrupt를 사용할 수 있다. 이는 infinite reader를 magically 끝내는 기능이 아니라 정상적으로 progress 가능한 CPU의 bookkeeping delay를 줄이는 quality-of-implementation mechanism이다.

## CHAPTER 17 · nohz_full CPU는 scheduler tick이 없어서 quiescent detection이 더 어렵다

Userspace/isolated workload를 위해 tick을 줄인 CPU는 periodic tick 기반 progress hint가 부족할 수 있다. RCU는 필요하면 reschedule IPI 등으로 holdout을 자극해야 한다. CPU isolation tuning은 RCU grace-period latency와 독립적이지 않다.

## CHAPTER 18 · RCU stall warning은 원인이지 결과가 아니라 symptom이다

Stall detector는 특정 CPU/task가 grace period를 너무 오래 막았음을 알리지만 root cause는 irq-off loop, preempt-disabled loop, scheduler starvation, interrupt loss, timer 문제 등 다양하다. Warning의 CPU mask와 stack trace, GP sequence, callback backlog를 함께 읽어야 한다.

## CHAPTER 19 · Callback execution storm도 realtime latency를 악화시킬 수 있다

많은 callbacks가 한꺼번에 ready되면 callback processing이 CPU를 오래 사용할 수 있다. Forward progress를 위해 batch limit을 완화하면 memory retention은 줄지만 realtime response는 악화될 수 있다. Grace-period progress와 callback-service progress는 별도 control loop다.

## CHAPTER 20 · rcu_nocbs는 callback execution을 지정 CPUs에서 offload한다

Latency-sensitive CPUs에서 callback processing을 빼면 jitter를 줄일 수 있지만 offload kthreads/CPUs가 backlog를 처리할 capacity가 있어야 한다. Producers가 callbacks를 등록하는 속도보다 consumer CPU가 느리면 memory가 계속 쌓인다. Isolation은 work를 제거하는 것이 아니라 다른 CPU에 이전하는 것이다.

## CHAPTER 21 · CPU hotplug는 grace-period participant set을 동적으로 바꾼다

CPU가 offline되는 동안 그 CPU의 pre-existing read-side section bookkeeping이 grace period에서 빠져야 한다. 반대로 online CPU를 너무 늦게/일찍 participant로 추가하면 reader를 놓칠 수 있다. Sequence start와 hotplug scan ordering이 correctness-critical하다.

## CHAPTER 22 · Ghost CPU bookkeeping은 boot/hotplug race의 예다

CPU가 online 예정이지만 실제로 아직 실행 가능한 상태가 아닐 때 RCU participant bitmap이 잘못 구성되면 영원히 quiescent report를 기다릴 수 있다. Hardware topology state와 scheduler-online state를 구분해야 한다. Hotplug protocol은 simple bit toggle이 아니다.

## CHAPTER 23 · RCU publication에는 pointer memory ordering이 필요하다

Updater가 object fields를 초기화한 뒤 pointer를 publish할 때 readers가 pointer는 새 값인데 fields는 old/uninitialized로 보는 reorder를 막아야 한다. `rcu_assign_pointer`와 `rcu_dereference`류 helper가 compiler/architecture ordering과 dependency semantics를 encapsulate한다.

## CHAPTER 24 · Reader가 protected pointer를 critical section 밖으로 유출하면 grace-period proof가 깨진다

RCU read lock 안에서 pointer를 얻고 unlock 뒤에도 reference를 사용하면 updater가 grace period 후 object를 free할 수 있다. Critical section 밖으로 lifetime을 연장하려면 refcount를 획득하거나 다른 ownership protocol로 전환해야 한다.

## CHAPTER 25 · In-place update는 copy-and-publish보다 더 강한 synchronization을 요구한다

RCU는 pointer replacement와 deferred reclamation에 특히 잘 맞는다. Existing object fields를 여러 writers/readers가 in-place로 수정하면 lock, sequence counter, atomic state 등 별도 synchronization이 필요하다. `RCU로 보호됨`이 object의 모든 field race를 해결하지 않는다.

## CHAPTER 26 · SRCU는 sleep 가능한 reader를 다른 tracking model로 지원한다

Sleepable RCU는 reader가 blocking/sleep할 수 있는 use case에 맞춰 별도 per-instance tracking을 사용한다. Classic RCU primitive를 sleeping reader에 무조건 적용하면 grace-period semantics가 맞지 않는다. RCU flavor 선택은 reader execution context에서 시작한다.

## CHAPTER 27 · Tasks RCU는 task-level quiescent transition을 추적한다

Tracing/function patching처럼 ordinary RCU read section보다 task execution transition 자체를 기다려야 하는 use case가 있다. Tasks RCU flavors는 voluntary context switch, userspace, idle, tracing critical section 등 다른 quiescent definition을 사용한다. Flavor 이름만 바꿔 동일 primitive로 보면 안 된다.

## CHAPTER 28 · RCU torture는 scheduler/hotplug/stall race를 의도적으로 증폭한다

`rcutorture` 같은 stress framework는 read/update patterns, CPU hotplug, stalls, callback load를 조합해 드문 grace-period race를 검증한다. Unit test에서 pointer list 한 번 insert/delete 성공하는 것보다 lifecycle pressure 아래 progress와 memory ordering을 검증해야 한다.

## CHAPTER 29 · RCU observability는 GP latency와 callback latency를 분리해야 한다

Grace period가 20ms인데 callback이 2s 뒤 실행되는 상황과 grace period 자체가 2s stall한 상황은 완전히 다르다. GP sequence age, holdout CPU/task, callback queue depth, callback service rate를 각각 추적해야 한다. `RCU 느림` 하나의 metric으로는 원인을 못 찾는다.

## CHAPTER 30 · RCU는 read scalability를 deferred global coordination으로 교환한다

정확한 RCU 설계는 **publication ordering, read-side lifetime, quiescent-state detection, grace-period sequence, callback batching, hotplug/nohz, expedited progress, callback backpressure, correct flavor selection, teardown barrier**를 함께 다룬다. Reader path가 싸다는 사실은 update/reclamation complexity가 사라졌다는 뜻이 아니라 system-wide coordination이 뒤로 이동했다는 뜻이다.
