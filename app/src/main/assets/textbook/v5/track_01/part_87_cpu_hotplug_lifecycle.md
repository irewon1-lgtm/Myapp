# PART 87 · CPU Hotplug Lifecycle — cpuhp states, scheduler evacuation, IRQ/timer migration, RCU quiescence

CPU online/offline은 core 하나의 전원을 켜고 끄는 동작이 아니다. Scheduler runqueue, per-CPU threads, timers, interrupt affinity, workqueues, RCU grace-period accounting, PMU, cpuidle/cpufreq, cpuset과 subsystem-local per-CPU state가 모두 해당 CPU의 존재 여부를 전제로 한다. CPU를 offline하려면 **새 work가 그 CPU로 들어오지 않게 막고, 이미 존재하는 state를 다른 CPU나 global owner로 이관하고, target CPU에서만 수행 가능한 teardown을 정해진 순서로 실행한 뒤 online mask에서 제거**해야 한다. Online은 이 역과정이지만 실패 시 이미 올라온 states를 정확히 rollback해야 한다.

## CHAPTER 01 · CPU hotplug는 하나의 boolean이 아니라 ordered state machine이다

Kernel은 CPU가 `offline/online` 두 상태만 가진다고 보지 않고 여러 subsystem이 준비되는 순서를 `cpuhp_state` 단계로 표현한다. Architecture bring-up, scheduler, RCU, perf, cpuidle, driver-local callbacks는 서로 다른 시점에 target CPU가 어느 기능을 사용할 수 있는지 전제로 한다. 특정 callback이 너무 일찍 실행되면 scheduler나 interrupts가 아직 준비되지 않았고, 너무 늦게 teardown되면 이미 의존 subsystem이 사라진 뒤 resource를 건드릴 수 있다. **Hotplug ordering은 per-CPU dependency graph를 linearized state sequence로 표현한 것**이다.

## CHAPTER 02 · possible, present, online, active CPU 집합은 서로 다른 의미를 가진다

Hardware/firmware가 시스템 lifetime 동안 존재 가능하다고 알려준 CPU, 현재 물리적으로 present한 CPU, kernel scheduling 대상으로 online된 CPU, scheduler/load balancing에 active한 CPU는 같은 집합이 아닐 수 있다. Subsystem이 `nr_cpu_ids`만 보고 per-CPU work를 보내면 offline CPU에 callback을 queue하거나 unavailable hardware를 접근할 수 있다. 반대로 possible CPU 전체에 metadata를 미리 할당하는 것은 future online을 준비하기 위한 정당한 설계일 수 있다. Code review에서는 **어떤 CPU mask가 해당 operation의 actual eligibility를 표현하는지** 명확히 해야 한다.

## CHAPTER 03 · cpus_read_lock은 CPU population이 중간에 바뀌지 않는다는 snapshot contract를 만든다

여러 online CPUs를 순회하며 per-CPU state를 읽거나 작업을 설치하는 동안 CPU가 동시에 offline되면 iterator가 사라진 target에 접근할 수 있다. CPU hotplug read-side locking은 해당 critical section 동안 online/offline transition이 완전히 진행되지 못하게 해 stable population을 제공한다. 이 lock을 오래 잡은 채 blocking I/O를 하면 administrative hotplug latency를 크게 늘릴 수 있으므로 scope를 작게 유지해야 한다. **CPU set 안정성도 ordinary object lifetime처럼 explicit reference/lock이 필요한 resource**다.

## CHAPTER 04 · PREPARE phase는 target CPU가 완전히 실행되기 전 global context에서 resource를 준비한다

CPU bring-up 초기에는 target CPU가 아직 callbacks를 실행할 수 없거나 scheduler/interrupt infrastructure가 준비되지 않았다. PREPARE states는 다른 online CPU의 normal context에서 allocation, global bookkeeping, dependency preparation을 수행할 수 있다. Allocation failure처럼 recoverable error는 이 단계에서 online을 중단해 partial target execution을 피하는 편이 유리하다. Offline에서는 대응 teardown이 late target shutdown보다 뒤쪽 global context에서 실행될 수 있어 **resource create/destroy 위치와 target CPU execution capability를 분리**한다.

## CHAPTER 05 · STARTING phase는 target CPU에서 실행되지만 interrupts/scheduler 가용성이 제한될 수 있다

CPU가 architecture-level bring-up을 마치고 target CPU에서 code를 실행할 수 있어도 ordinary process context와 같은 freedom이 즉시 생기는 것은 아니다. STARTING 구간 callbacks는 명시적 ordering이 필요한 scheduler/RCU/architecture state를 local CPU에서 세팅한다. Sleep이나 arbitrary allocation을 요구하는 초기화는 이 단계에 부적절할 수 있다. `callback이 target CPU에서 돈다`는 사실보다 **그 cpuhp phase가 허용하는 execution context와 dependencies**를 봐야 한다.

## CHAPTER 06 · ONLINE phase는 scheduler가 동작하는 CPU에서 subsystem service를 최종 publish한다

CPU가 normal scheduling과 interrupts를 사용할 수 있게 된 뒤 ONLINE callbacks는 cpuidle/perf/driver instances처럼 CPU-local service를 등록한다. Dynamic state allocation이 주로 이 구간에 제공되는 이유는 ordering constraint가 없는 많은 subsystem이 normal context에서 independent setup을 수행할 수 있기 때문이다. Startup callback이 external registry에 CPU를 publish하기 전에 local state를 완전히 초기화해야 한다. **Online bit가 보이기 시작하는 시점과 subsystem-specific usability가 같은 순간인지 API contract로 확인**해야 한다.

## CHAPTER 07 · Static hotplug state는 subsystem 사이 ordering dependency를 코드 구조에 고정한다

Perf core가 driver보다 먼저 올라와야 하고 offline에서는 driver가 core보다 먼저 내려가야 하는 것처럼 명확한 dependency가 있으면 static state order가 필요하다. 임의 dynamic callback 순서에 기대면 module load timing에 따라 bring-up 순서가 달라질 수 있다. Static state는 enum ordering 변경 자체가 global kernel dependency modification이므로 다른 subsystem assumptions를 검토해야 한다. **Hotplug state 번호는 priority가 아니라 initialization DAG의 topological order 일부**다.

## CHAPTER 08 · Dynamic state는 ordering requirement가 없는 extension에 적합하다

Driver나 subsystem이 `ONLINE_DYN` 같은 range에서 state를 할당받으면 core가 available slot을 선택하고 callback을 관리한다. 이 방식은 unrelated drivers끼리 fixed enum entry를 소비하지 않게 하지만, 다른 subsystem보다 반드시 앞/뒤여야 하는 dependency를 표현하지 못한다. Dynamic registration을 선택했다면 callback은 **이미 보장된 generic hotplug invariants만 의존**해야 한다. Hidden ordering requirement가 생겼다면 static state 또는 explicit dependency mechanism으로 올려야 한다.

## CHAPTER 09 · cpuhp_setup_state는 future hotplug뿐 아니라 현재 online CPUs에도 startup을 적용한다

State registration 시점에 여러 CPU가 이미 online이라면 `cpuhp_setup_state()`는 새 callback을 그 CPU들에도 호출해 현재 world state와 future hotplug behavior를 맞춘다. 등록 함수가 return했을 때 caller는 supported online CPUs에 state가 설치됐다고 기대할 수 있다. 하지만 CPU N에서 startup이 실패하면 앞서 성공한 CPUs에 teardown을 실행해 partial registration을 rollback해야 한다. **Callback 등록 자체도 multi-CPU transaction**이며 failure symmetry가 필요하다.

## CHAPTER 10 · _nocalls variant는 registration과 population synchronization 책임을 caller에게 돌린다

`cpuhp_setup_state_nocalls()`는 callback table만 설치하고 현재 online CPUs에 startup을 자동 실행하지 않는다. Caller가 이미 별도 방식으로 per-CPU state를 구성했거나 exact timing을 통제해야 할 때 유용하지만, 현재 population과 callback registration 사이 race를 직접 막아야 한다. `nocalls`를 단순 fast version으로 쓰면 기존 CPUs에는 state가 없고 future CPUs에만 생기는 split-brain이 된다. **자동 replay를 끄는 API는 bootstrap responsibility를 명시적으로 인수하는 것**이다.

## CHAPTER 11 · Multi-instance state는 같은 hotplug callback을 여러 device/object instance에 직렬화한다

Subsystem에 동일 type의 device instance가 여러 개 있으면 각 instance가 자체 notifier list를 만들기보다 cpuhp multi-instance mechanism을 사용할 수 있다. Instance add/remove와 CPU online/offline가 core에 의해 serialization돼 `CPU는 올라오는데 새 instance callback은 빠짐` 같은 race를 줄인다. Instance removal도 online CPUs에 teardown callback을 적용한 뒤 list에서 빼야 object lifetime이 안전하다. **CPU dimension과 object-instance dimension을 한 registration protocol로 교차시키는 구조**다.

## CHAPTER 12 · CPU online failure는 이미 통과한 hotplug states를 역순으로 rollback한다

Bring-up 중 어느 callback이 error를 반환하면 CPU를 half-online 상태로 남길 수 없으므로 성공했던 states의 teardown을 reverse order로 실행한다. Callback은 normal offline뿐 아니라 `startup 실패 뒤 rollback`에서도 안전해야 하며 hardware가 완전히 정상 동작했다고 가정하면 안 된다. Resource acquisition을 단계별 ownership token으로 기록하면 partial initialization cleanup이 쉬워진다. **Online path의 각 state는 다음 state가 실패해도 되돌릴 수 있는 checkpoint**여야 한다.

## CHAPTER 13 · CPU offline은 online sequence의 reverse dependency order를 따른다

CPU를 내릴 때 consumer-level subsystem을 먼저 떼고 core infrastructure를 나중에 제거해야 한다. Scheduler service가 사라진 뒤 per-CPU worker를 flush하려 하거나 interrupt routing을 제거한 뒤 timer callback drain을 기다리면 progress source를 잃는다. Reverse ordering은 단순 convention이 아니라 initialization dependency를 역전한 teardown proof다. Hotplug callback pair를 작성할 때 startup에서 획득한 각 resource가 **어느 later state에 의해 사용될 수 있는지**까지 추적해야 한다.

## CHAPTER 14 · Scheduler는 runnable tasks를 outgoing CPU에서 이주시켜야 한다

Offline 대상 CPU의 runqueue에 ordinary runnable task가 남아 있으면 CPU stop 뒤 execution이 영원히 사라진다. Scheduler는 affinity와 cpuset constraints를 고려해 tasks를 surviving CPUs로 migrate하고 target CPU를 load balancing 대상에서 제거한다. Task affinity가 해당 CPU 하나만 허용하도록 고정돼 있다면 offline policy가 affinity를 강제로 조정하거나 hotplug를 거부해야 할 수 있다. **CPU removal은 task placement constraint를 다시 satisfiable하게 만드는 문제**다.

## CHAPTER 15 · Migration-disabled/pinned execution은 CPU offline의 progress blocker가 될 수 있다

Kernel code가 `migration_disable()` 영역에 오래 머물거나 특정 per-CPU thread가 target CPU를 떠날 수 없으면 offline은 해당 execution이 safe point에 도달할 때까지 기다려야 한다. User affinity와 달리 low-level CPU-local invariant 때문에 이동할 수 없는 구간을 억지로 migrate하면 per-CPU pointer correctness가 깨진다. Hotplug latency tail은 average task load보다 **longest non-migratable critical section**에 지배될 수 있다. Context trace로 blocker stack을 찾아야 한다.

## CHAPTER 16 · Per-CPU kthreads는 stop, park, migrate 중 어떤 lifecycle을 갖는지 subsystem별로 다르다

Timer/RCU/network/thermal 등 subsystem은 CPU마다 kernel thread를 둘 수 있다. CPU offline 시 thread를 완전히 destroy할 수도 있고, 다른 CPU로 migrate/park했다가 online 시 target으로 되돌릴 수도 있다. Thread가 보유한 per-CPU queues와 timers를 먼저 이관하지 않고 task만 옮기면 logical ownership이 남는다. **Thread identity, CPU affinity, queue ownership, recreation policy를 하나의 hotplug state로 관리**해야 한다.

## CHAPTER 17 · Timers는 offline CPU의 local base에서 살아남는 CPU로 migration되어야 한다

CPU-local timer wheel/hrtimer queue에 expiry가 남아 있는데 CPU를 끄면 callback이 실행되지 않는다. Hotplug core와 timer subsystem은 migratable timers를 housekeeping/other online CPU로 옮기고 pinned/non-migratable timer semantics를 처리한다. Isolation workload에서는 CPU offline/online이 recurring timer placement를 바꾸는 효과도 있어 jitter tuning에 사용되기도 한다. Timer migration은 단순 data move가 아니라 **time deadline ownership을 새로운 execution CPU에 이전하는 작업**이다.

## CHAPTER 18 · Interrupt affinity는 target CPU가 offline되기 전에 valid online mask로 재배치되어야 한다

MSI/MSI-X/IOAPIC routing이 offline CPU를 계속 가리키면 device completion interrupt가 delivery되지 않거나 platform-specific fallback이 발생할 수 있다. IRQ core는 affinity mask를 online CPUs에 맞게 조정하고 managed interrupt는 device queue와 CPU topology를 함께 재구성할 수 있다. Driver가 hardware queue를 CPU 번호와 1:1로 고정했다면 hotplug callback에서 queue mapping을 다시 만들어야 한다. **Interrupt destination과 software queue owner는 동시에 이동해야** late completion을 잃지 않는다.

## CHAPTER 19 · RCU는 offline CPU가 future grace period를 영원히 막지 않도록 quiescent state를 확정한다

RCU는 모든 relevant CPUs가 old read-side critical section을 지났다는 사실을 기다리므로 offline CPU를 online set에서 빼기만 하고 quiescent bookkeeping을 놓치면 grace period가 끝나지 않을 수 있다. CPU dying path는 RCU에 dead/quiescent state를 보고하고 callbacks를 surviving CPUs로 넘길 수 있다. 반대로 stop-machine-like hotplug state에서 `rcu_barrier()`처럼 hotplug completion을 필요로 하는 operation을 호출하면 deadlock 가능성이 있다. **RCU progress와 CPU population change는 서로의 wait graph를 명시적으로 끊어야 한다.**

## CHAPTER 20 · Workqueue는 worker pool과 queued work의 CPU locality를 재조정한다

Bound workqueue는 특정 CPU/pool에 work item이 연결될 수 있어 target CPU offline 시 work가 다른 online pool에서 실행 가능하도록 migration/rebind가 필요하다. Unbound workqueue도 cpumask와 NUMA affinity policy가 hotplug 이후 달라질 수 있다. In-flight work가 CPU-local state를 직접 참조하면 단순 worker migration만으로 안전하지 않다. Hotplug-safe work item은 **execution CPU가 바뀌어도 object ownership invariant가 유지되거나 explicit per-CPU handoff**를 해야 한다.

## CHAPTER 21 · PMU/perf state는 CPU-local hardware counter lifetime과 함께 올라오고 내려간다

Performance monitoring register와 event scheduler는 CPU마다 독립 hardware context를 가지므로 offline CPU의 active events를 stop하고 overflow interrupt를 차단한 뒤 state를 정리해야 한다. Online에서는 core PMU support가 driver-specific PMU callback보다 먼저 준비돼야 해 cpuhp static ordering이 중요할 수 있다. Benchmark/monitoring daemon은 CPU가 사라졌을 때 fd/read semantics와 group membership 변화를 처리해야 한다. **PMU counter identity는 CPU generation에 종속**된다.

## CHAPTER 22 · cpuidle/cpufreq는 CPU online state를 power-management device registration과 연결한다

CPU idle driver는 online CPU에 cpuidle device를 등록하고 offline에서 제거할 수 있으며 frequency policy도 policy-domain CPU membership 변화에 반응해야 한다. Heterogeneous system에서 core 하나가 빠지면 shared policy capacity와 energy-aware scheduler inputs가 바뀔 수 있다. Power subsystem이 stale online mask를 사용하면 nonexistent CPU에 IPI/register access를 시도하거나 capacity accounting이 틀어진다. Hotplug는 **compute capacity와 power-control topology를 동시에 변경**한다.

## CHAPTER 23 · Cpuset/cgroup effective CPU mask는 hotplug 이후 실제 실행 가능한 CPU를 반영해야 한다

Container/task가 requested cpuset에 CPU 4만 허용했는데 CPU 4가 offline되면 effective CPU set이 비어 scheduler placement가 불가능해질 수 있다. Cgroup v2 partition validity와 effective masks는 hotplug 변화에 따라 재계산되어 userspace orchestration이 topology loss를 관찰할 수 있어야 한다. Configured mask와 effective mask를 동일하게 보면 offline CPU를 계속 capacity로 계산하게 된다. **Policy intent와 currently realizable resources를 분리**해야 한다.

## CHAPTER 24 · NO_HZ/CPU isolation 환경에서는 hotplug가 housekeeping work placement를 재분배한다

Tickless isolated CPU에서 timer/RCU/IRQ noise를 줄여둔 시스템도 다른 CPU를 offline하면 housekeeping workload가 isolation CPU로 밀려올 수 있다. 반대로 isolated CPU를 한번 offline/online해 recurring timers를 다른 CPU로 이동시키는 tuning 패턴도 있다. 따라서 hotplug는 capacity change뿐 아니라 OS jitter distribution을 바꾸는 operation이다. Low-latency workload는 **hotplug event 전후 IRQ/timer/kthread affinity를 다시 검증**해야 한다.

## CHAPTER 25 · Stop-machine 구간은 arbitrary blocking이 금지된 hotplug의 하드 경계다

CPU state 전환의 일부 critical section은 system-wide coordination 또는 target CPU stop을 요구해 ordinary scheduler progress를 사용할 수 없는 context에서 실행될 수 있다. 이런 cpuhp state callback에서 synchronous RCU barrier나 blocking dependency를 만들면 hotplug operation 자체가 끝나야 상대가 진행하는 cycle이 생긴다. Callback author는 자신이 PREPARE/STARTING/ONLINE 중 어디서 실행되는지 확인해야 한다. **Hotplug callback은 동일 signature여도 execution-context legality가 state마다 다르다.**

## CHAPTER 26 · NUMA locality는 CPU offline 뒤 memory placement와 access cost를 다시 평가해야 한다

CPU 하나 또는 socket의 cores가 offline되면 해당 CPU를 기준으로 배치한 NUMA-local memory가 surviving task에게 remote가 될 수 있다. Scheduler task migration만 끝내고 memory는 그대로 두면 execution은 계속되지만 latency/bandwidth가 급격히 악화될 수 있다. Long-lived workloads는 automatic NUMA balancing 또는 explicit migration policy가 새 topology에 수렴하는지 관찰해야 한다. Hotplug recovery의 완료 기준은 **task가 실행 가능함 + resource locality가 acceptable함**으로 확장될 수 있다.

## CHAPTER 27 · CPU hotplug와 system suspend는 동일 CPU state를 만지는 transition끼리 serialization돼야 한다

Deep system suspend가 nonboot CPUs를 offline하는 동안 administrator나 thermal policy가 별도 CPU hotplug를 시작하면 cpuhp states와 PM phase가 서로 침범할 수 있다. Device/memory hotplug도 shared hotplug lock order와 firmware notifications를 통해 serialize할 필요가 있다. Transition owner가 둘이면 online mask, IRQ routing, per-CPU state가 어느 generation에 속하는지 모호해진다. **CPU population을 변경하는 global transaction은 한 번에 하나의 owner만 갖도록 해야 한다.**

## CHAPTER 28 · Hotplug observability는 final online mask보다 state transition latency와 blocker를 보여줘야 한다

`/sys/devices/system/cpu/cpuN/online` 결과만 보면 offline이 20ms 걸렸는지 10초 동안 특정 callback을 기다렸는지 알 수 없다. CPUHP state trace, callback name/duration, task migration, IRQ/timer movement, RCU wait를 동일 hotplug generation으로 기록해야 한다. 실패했다면 어느 state startup/teardown이 error를 반환했고 rollback이 어디까지 실행됐는지 남겨야 한다. **Transition latency와 rollback state가 operational reliability 지표**다.

## CHAPTER 29 · Hotplug fault test는 각 cpuhp state failure와 concurrent workload를 조합해야 한다

Startup callback N에서 의도적 실패를 주고 1..N-1 states가 정확히 reverse teardown되는지 검증해야 한다. Offline 동안 high-rate interrupts, per-CPU timers, RCU readers, pinned tasks, PMU events, cpuset-restricted workloads를 활성화해 migration/drain race를 흔든다. CPU를 반복 online/offline해 refcount/per-CPU allocation이 baseline으로 돌아오는지도 확인한다. `한 번 offline 성공`은 state-space의 극히 작은 부분만 검증한 것이다.

## CHAPTER 30 · CPU hotplug correctness는 per-CPU state를 새 population으로 완전히 수렴시키는 proof다

CPU N을 offline했다고 선언하려면 `새 tasks/IRQs/timers/work가 언제 차단됐는가`, `runnable/pinned execution이 어디로 이동했는가`, `RCU와 callback queues가 어떤 quiescent state를 기록했는가`, `PMU/power/per-CPU data가 어떤 order로 teardown됐는가`, `cpuset와 scheduler capacity가 새 mask를 반영했는가`, `실패 시 startup states가 정확히 rollback되는가`를 답할 수 있어야 한다. Online은 동일 ledger를 반대로 채워야 한다. **CPU hotplug는 CPU 하나를 끄는 기능이 아니라 kernel 전체의 per-CPU ownership graph를 새 topology로 재작성하는 transaction**이다.
