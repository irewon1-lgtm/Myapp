# PART 88 · NO_HZ and Tickless Execution — scheduler tick suppression, dynticks, housekeeping, isolation

Periodic scheduler tick은 time sharing, accounting, timer progress, RCU observation 같은 여러 kernel 기능에 편리한 heartbeat를 제공하지만, idle CPU나 single-task low-latency CPU를 몇 ms마다 강제로 깨우는 비용도 만든다. NO_HZ는 `tick을 끈다`는 단일 optimization이 아니라 **periodic heartbeat가 사라져도 각 subsystem이 progress와 time/accounting correctness를 유지하도록 work를 one-shot event, kernel boundary, remote housekeeping CPU로 재배치하는 구조**다. Tick suppression이 깊어질수록 local jitter와 power는 줄지만 RCU, scheduler, watchdog, workqueue, IRQ placement의 hidden dependency가 더 중요해진다.

## CHAPTER 01 · Scheduler tick은 preemption trigger이면서 여러 subsystem의 periodic observation point다

여러 runnable tasks가 한 CPU를 공유할 때 current task가 자발적으로 CPU를 놓지 않으면 scheduler는 periodic tick을 이용해 runtime을 account하고 reschedule 필요를 판단할 수 있다. Tick은 동시에 timer/RCU/load-balancing/accounting 관련 work의 convenient entry point가 되기 때문에 없애면 scheduler만 수정해서 끝나지 않는다. Tickless 설계는 각 consumer에게 `다음에 언제 반드시 관찰되어야 하는가`라는 deadline을 다시 부여해야 한다. **Periodic polling을 event-driven progress로 바꾸는 system-wide refactoring**으로 보는 편이 정확하다.

## CHAPTER 02 · NO_HZ_IDLE은 idle CPU에서 periodic tick의 주요 이유가 사라진다는 사실을 이용한다

Idle loop만 실행 중인 CPU에는 CPU time을 여러 runnable tasks 사이에 나눌 필요가 없으므로 scheduler tick을 계속 발생시킬 이유가 줄어든다. Kernel은 가장 가까운 timer/event를 계산해 tick을 멈추고 CPU를 deep idle state에 보낸 뒤 실제 event가 올 때 깨울 수 있다. Tick period가 1~10ms인데 target idle residency가 수십 ms라면 tick을 유지할 경우 deep C-state의 energy benefit을 계속 깨뜨린다. **Tick stop은 cpuidle state selection과 expected sleep length를 함께 고려하는 결정**이다.

## CHAPTER 03 · Tick을 멈추려면 다음 mandatory event까지의 deadline을 one-shot clockevent에 program해야 한다

Periodic tick을 제거해도 timer deadline을 놓칠 수는 없다. NO_HZ idle path는 pending timers/hrtimers와 scheduler requirement를 조사해 CPU를 다시 깨워야 하는 earliest time을 계산하고 clock-event device를 one-shot mode로 설정한다. 새 earlier timer가 remote CPU에서 enqueue되면 target을 IPI로 깨워 deadline을 reprogram해야 할 수 있다. 따라서 tickless는 timer를 없애는 것이 아니라 **고정 주기 interrupt를 dynamic earliest-deadline interrupt로 교체하는 것**이다.

## CHAPTER 04 · Clocksource와 clockevent는 tickless correctness에서 서로 다른 역할을 맡는다

Clocksource는 `지금 시간이 얼마인가`를 읽는 연속 counter이고 clockevent device는 `특정 미래 시점에 interrupt를 만들어라`는 programmable alarm 역할을 한다. Tick을 멈춘 뒤에도 elapsed time을 정확히 계산하려면 suspend/idle 동안 안정적인 clocksource가 필요하고, 다음 timer를 정확히 깨우려면 충분한 clockevent가 필요하다. 두 기능을 같은 hardware timer 개념으로 뭉개면 one-shot 지원 여부와 timekeeping 안정성을 구분하지 못한다. **시간 측정과 wakeup delivery는 별도 hardware contracts**다.

## CHAPTER 05 · Deep idle에서 local timer가 멈추면 timer broadcast가 wakeup 책임을 다른 device/CPU로 옮긴다

일부 CPU idle state는 local APIC timer/clockevent까지 멈추게 하므로 target CPU가 자기 timer deadline에 스스로 깨어날 수 없다. Broadcast-capable clockevent나 다른 always-on timer가 여러 sleeping CPUs의 next event를 대신 추적하고 해당 CPU를 깨워야 한다. Broadcast registration과 idle enter/exit ordering이 틀리면 timer deadline을 영구히 잃거나 unnecessary wakeup을 만들 수 있다. **Deep power state는 time-event ownership을 local CPU에서 broadcast source로 handoff하는 transition**이다.

## CHAPTER 06 · Tick stop은 단순 idle flag가 아니라 여러 “tick dependency”가 모두 해소됐는지 확인해야 한다

Pending timer, scheduler reschedule need, RCU work, perf event, POSIX CPU timer 등 subsystem이 periodic tick을 요구할 수 있다. 하나라도 local periodic observation이 필요하면 CPU가 idle-like 상태여도 tick을 유지하거나 조기에 재개해야 한다. Tick-stop failure를 `kernel bug`라고 바로 판단하기보다 어떤 dependency가 active한지 추적해야 한다. Debugging은 **tick을 끄지 못한 이유를 subsystem별 dependency bit/event로 설명할 수 있어야** 한다.

## CHAPTER 07 · NO_HZ_FULL은 idle뿐 아니라 single runnable userspace task에서도 scheduler tick을 억제한다

Low-latency/HPC workload가 한 CPU를 독점하고 userspace에서 오랫동안 계산한다면 periodic tick은 task switching에 실질적으로 필요하지 않다. Full dynticks는 조건이 맞을 때 user execution 중 tick을 멈춰 interrupt jitter를 줄인다. 하지만 두 개 이상의 runnable task가 같은 CPU를 공유하면 time sharing과 preemption을 위해 tick이 다시 필요해질 수 있다. 따라서 nohz_full CPU는 **single-task ownership을 유지하는 placement discipline**과 함께 사용해야 효과가 있다.

## CHAPTER 08 · Full-dynticks CPU에서 kernel entry는 hidden bookkeeping debt를 다시 지불한다

Userspace에서 tickless 상태로 오래 머물다가 syscall, exception, IRQ로 kernel에 들어오면 CPU time accounting, RCU dynticks state, scheduler requirements를 boundary에서 갱신해야 한다. Periodic tick이 하던 작은 비용을 없애는 대신 kernel entry/exit가 더 비싸질 수 있다. 따라서 syscall-heavy workload는 nohz_full을 켜도 jitter가 크게 줄지 않거나 boundary cost가 상대적으로 커질 수 있다. **Full dynticks 최적화는 long userspace compute interval을 가진 workload에 가장 잘 맞는다.**

## CHAPTER 09 · RCU는 userspace와 idle을 extended quiescent state로 해석해 tick 없이 progress한다

RCU grace period는 각 CPU가 old read-side critical section을 벗어났음을 알아야 한다. NO_HZ_FULL userspace execution에서는 kernel RCU read-side code를 실행하지 않는다고 보고 dynticks state를 통해 extended quiescent state로 표시할 수 있다. CPU가 kernel로 다시 들어올 때 state transition을 정확히 기록하지 않으면 RCU가 이미 quiescent한 CPU를 영원히 기다리거나 아직 reader인 CPU를 끝났다고 오판할 수 있다. **Dynticks counter는 tickless CPU의 RCU participation state를 압축한 correctness metadata**다.

## CHAPTER 10 · RCU callback execution은 nohz_full CPU 밖으로 offload해야 jitter 이득을 유지할 수 있다

CPU가 userspace에서 tick을 받지 않더라도 RCU callbacks가 local softirq/kthread로 실행되면 unpredictable kernel noise가 남는다. `rcu_nocbs`/nohz_full 관련 offload는 callback processing을 housekeeping CPU로 옮겨 isolated CPU의 jitter를 줄인다. Offload CPU가 overload되면 callback backlog와 grace-period pressure가 커질 수 있으므로 housekeeping capacity가 필요하다. **Noise 제거는 work 제거가 아니라 다른 CPU로 ownership을 이전하는 것**이다.

## CHAPTER 11 · Residual 1Hz scheduler work도 housekeeping CPU로 보내야 full isolation에 가깝다

Long-running tickless CPU도 scheduler statistics/load/accounting을 영원히 갱신하지 않을 수는 없다. Modern full-dynticks setup은 residual periodic work를 unbound workqueue를 통해 housekeeping CPU에서 대신 처리하도록 설계할 수 있다. Housekeeping mask가 너무 작거나 busy하면 remote work가 지연되고 global scheduler view가 늦게 갱신될 수 있다. `tick=0`이라는 local 목표 뒤에는 **remote periodic maintenance capacity**라는 system-wide 비용이 존재한다.

## CHAPTER 12 · Housekeeping CPU는 isolated CPUs가 버린 kernel noise를 흡수하는 shared service pool이다

Timer, RCU callback, unbound workqueue, managed IRQ 일부, scheduler maintenance를 isolated CPU에서 빼면 그 work는 housekeeping CPUs에 집중된다. Housekeeping set을 최소 하나만 남기고 system load가 커지면 그 CPU가 100% kernel work에 포화돼 network/storage latency와 RCU progress가 나빠질 수 있다. Low-jitter cores 개수만 극대화하기보다 **offloaded work arrival rate와 housekeeping service capacity를 계산**해야 한다. Isolation은 전체 CPU 시간을 늘리는 기능이 아니다.

## CHAPTER 13 · Unbound workqueue affinity는 housekeeping policy와 일치해야 한다

Kernel subsystem이 unbound workqueue에 maintenance work를 queue하면 scheduler가 allowed CPU mask 안에서 worker를 배치한다. NO_HZ/isolation 설정 후 effective unbound cpumask가 housekeeping set과 교차되도록 갱신하지 않으면 isolated CPU가 random kworker를 실행해 jitter가 다시 생긴다. 반대로 mask가 지나치게 좁으면 workqueue congestion이 system progress를 막는다. Workqueue placement는 **task affinity가 아니라 kernel background-work routing policy**다.

## CHAPTER 14 · IRQ affinity를 그대로 두면 tick을 없애도 hardware interrupt jitter가 지배한다

Network/storage/device IRQ가 nohz_full CPU에 계속 들어오면 scheduler tick 몇 개를 제거한 이득보다 훨씬 큰 latency disturbance가 생길 수 있다. Static IRQ affinity, managed IRQ policy, RSS/MSI-X queue mapping을 housekeeping CPUs에 맞춰야 한다. 일부 managed interrupt는 device queue와 isolated CPU만이 allowed target인 경우 완전히 빼기 어려울 수 있다. **Tick isolation과 interrupt isolation은 독립 설정이며 둘 다 확인**해야 한다.

## CHAPTER 15 · POSIX CPU timers는 task CPU-time progress를 주기적으로 관찰해야 해 full dynticks와 충돌한다

CPU-time 기반 timer는 wall-clock deadline이 아니라 특정 task/process가 얼마나 CPU를 소비했는지를 기준으로 signal을 발생시킨다. Periodic scheduler tick이 없으면 expiration 시점을 cheap하게 감지하기 어렵기 때문에 nohz_full CPU에서 이런 timer 사용은 tick dependency를 다시 만들 수 있다. Low-jitter workload가 profiling/runtime feature를 무심코 켜 nohz 효과를 잃는 경우가 있다. **Feature 하나가 tick suppression precondition을 깨는지 dependency 관점으로 검토**해야 한다.

## CHAPTER 16 · Clocksource watchdog가 필요한 불안정 counter는 isolated CPU에 extra disturbance를 만들 수 있다

Full dynticks는 userspace 실행 중 시간을 boundary에서 정확히 account해야 하므로 stable clocksource가 중요하다. Clocksource가 unreliable해 periodic watchdog 비교가 필요하면 kernel이 timer/interrupt work를 수행해야 하고 isolation 목표와 충돌할 수 있다. Hardware/firmware clock stability는 scheduler tuning 바깥 문제처럼 보여도 nohz_full feasibility의 전제다. **시간 source 품질이 낮으면 software가 periodic validation work로 비용을 되돌려 받는다.**

## CHAPTER 17 · CPU accounting은 periodic sampling 대신 transition-based accounting 의존도가 높아진다

Tick 기반 system은 일정 간격으로 current task의 user/system time과 scheduler statistics를 누적할 수 있다. Full dynticks에서는 user↔kernel, context-switch 등 event boundary에서 elapsed counter를 읽어 accounting을 갱신해야 한다. Boundary 누락이나 clock inconsistency는 long userspace run의 CPU time을 잘못 계산해 quota/profiling 결과에 영향을 줄 수 있다. Tickless correctness는 latency만이 아니라 **accounting conservation law—elapsed CPU time이 owner들에게 정확히 배분되는가**를 포함한다.

## CHAPTER 18 · Multiple runnable tasks가 생기면 scheduler tick을 다시 켜야 fairness가 보장된다

NoHz-full CPU에 두 번째 runnable task가 wakeup되면 현재 task가 kernel에 들어오지 않는 한 자발적 yield가 없을 수 있다. Scheduler는 time sharing을 위해 tick/reschedule mechanism을 재활성화해 다른 task에게 CPU를 줄 기회를 만들어야 한다. 따라서 helper kthread 하나가 affinity mistake로 isolated CPU에 runnable해지는 것만으로 periodic jitter가 되살아날 수 있다. **Single runnable invariant는 configuration이 아니라 runtime condition**이라 지속 관측이 필요하다.

## CHAPTER 19 · Remote wakeup/IPI는 tickless CPU를 즉시 kernel로 끌어들이는 비주기적 disturbance다

다른 CPU가 isolated task를 wakeup하거나 TLB shootdown, reschedule, call-function IPI를 보내면 periodic tick이 없어도 target CPU는 userspace를 중단하고 kernel handler를 실행한다. Shared address-space mutation, global locks, frequent cross-CPU messaging이 많으면 nohz_full의 jitter benefit이 줄어든다. Low-latency design은 IRQ뿐 아니라 **IPI source와 cross-CPU synchronization graph**를 분석해 isolated CPU가 global kernel maintenance의 target이 되지 않게 해야 한다.

## CHAPTER 20 · Hrtimer/softirq work는 tick과 독립적으로 CPU를 깨울 수 있다

High-resolution timer는 정확한 deadline을 위해 one-shot clockevent를 설정하므로 periodic tick을 껐어도 local hrtimer가 만료되면 CPU가 깨어난다. Timer callback이 softirq 또는 thread context에서 work를 발생시켜 jitter를 만든다. Application/runtime이 local CPU에 recurring hrtimer를 설치했다면 nohz 설정만으로 제거되지 않는다. Trace에서는 `scheduler tick interrupt`와 **application/kernel hrtimer wakeup을 별도 원인으로 분류**해야 한다.

## CHAPTER 21 · Timer broadcast도 deadline을 보존하지만 shared wakeup source contention을 만든다

Deep idle에서 local timer가 멈추는 CPUs가 많으면 broadcast device가 여러 deadline을 관리한다. 가장 이른 deadline이 전체 broadcast programming을 지배하고 CPU마다 wake delivery가 필요할 수 있어 timer density가 높은 system에서는 broadcast overhead가 커진다. Housekeeping/idle topology 변화가 broadcast owner를 바꿀 수도 있다. Power tuning은 deepest C-state residency만 보지 말고 **broadcast wakeup count와 wake latency**를 함께 측정해야 한다.

## CHAPTER 22 · Watchdog를 nohz_full CPU에서 빼면 jitter는 줄지만 lockup observability가 낮아진다

Soft/hard lockup detector가 hrtimer/NMI events를 발생시키면 isolated CPU의 deterministic execution을 깨뜨릴 수 있어 default로 nohz_full CPUs에서 제외될 수 있다. 그 대신 해당 CPU가 kernel에서 lockup됐을 때 local detector evidence가 약해진다. 문제 재현 중에는 watchdog cpumask를 임시로 넓혀 diagnostic quality를 높일 수 있지만 performance result와 같은 조건이 아니게 된다. **Observability와 isolation purity 사이 trade-off를 명시**해야 한다.

## CHAPTER 23 · CPU hotplug는 recurring timer와 housekeeping placement를 재배치하는 강한 topology event다

CPU를 offline했다가 online하면 per-CPU timer/kthread가 다른 CPU로 이동하고 돌아오지 않을 수 있어 jitter tuning 기법으로 사용되기도 한다. 하지만 다른 housekeeping CPU를 이후 offline하면 work가 다시 isolated CPU로 밀려올 수 있다. Hotplug와 nohz policy는 독립 기능이 아니라 **background-work placement를 재계산하는 topology transitions**다. Low-latency configuration은 boot 직후뿐 아니라 hotplug 이후 effective placement를 재검증해야 한다.

## CHAPTER 24 · S2idle/system suspend와 NO_HZ는 둘 다 tick을 멈추지만 transition owner가 다르다

Idle/full-dynticks는 running system에서 CPU-local periodic work를 줄이는 반면 system suspend는 scheduler/timekeeping/device state 전체를 sleep generation으로 이동한다. S2idle 진입 시 tick/timekeeping suspension이 system PM protocol에 포함되므로 local nohz state와 겹칠 수 있다. Resume path는 timekeeping과 timer event를 정상 working state로 재program해야 한다. `이미 tickless였으니 suspend에서 할 일 없다`는 가정은 **local optimization state와 global sleep state를 혼동**한다.

## CHAPTER 25 · Tick dependency 하나가 생기면 전체 nohz_full 실패가 아니라 해당 기간에만 adaptive tick이 돌아올 수 있다

Full dynticks CPU가 kernel work, second runnable task, timer/accounting requirement를 만나면 조건이 해소될 때까지 tick을 재활성화할 수 있다. 따라서 trace에서 tick 하나를 발견했다고 configuration 전체가 실패했다고 결론내리면 안 된다. 문제는 **왜, 얼마나 오래, 얼마나 자주** tick dependency가 생겼는지다. Low-jitter 목표는 zero interrupt가 아니라 workload SLO 안에서 uncontrolled kernel disturbances를 bounded하게 만드는 것이다.

## CHAPTER 26 · Tick-stop reason observability는 configuration debugging의 핵심이다

NoHz가 기대대로 동작하지 않을 때 boot parameter만 확인해서는 원인을 찾기 어렵다. Runnable task count, pending timers, RCU state, perf/CPU timers, IRQ/softirq, workqueue placement를 trace해 tick 재시작 이유를 timeline으로 만들어야 한다. `timer interrupt가 많다`는 결과보다 어떤 subsystem이 local deadline을 생성했는지를 찾아야 한다. **Tick은 symptom이고 dependency owner가 root cause**다.

## CHAPTER 27 · Tickless design은 power 절감과 low-jitter라는 서로 다른 목표를 가질 수 있다

NO_HZ_IDLE은 idle residency를 늘려 energy를 줄이는 데 효과적이고, NO_HZ_FULL은 active single-task CPU의 interrupt jitter를 줄이는 데 사용된다. Power-oriented system은 deep idle entry/exit cost와 wakeup rate를 최적화하고, low-latency system은 user execution 중 worst-case kernel interruption을 줄인다. 같은 옵션이라도 성공 metric이 다르므로 package power만 보고 RT/HPC configuration을 평가하거나 jitter만 보고 mobile energy 효과를 평가하면 안 된다. **목표 function을 먼저 정의**해야 한다.

## CHAPTER 28 · Timer density가 높으면 tick을 없애도 event-driven wakeup이 periodic tick보다 더 자주 발생할 수 있다

수천 connections의 timeout, hrtimer, telemetry timer가 서로 다른 deadline으로 촘촘하게 설정되면 one-shot clockevent가 계속 재program되고 CPU가 자주 깨어난다. Fixed tick을 제거했지만 effective wakeup frequency가 더 높다면 power/jitter 개선이 없다. Timer coalescing, batching, deadline slack으로 application/subsystem timer population을 줄이는 것이 필요하다. Tickless kernel은 **불필요한 periodic wakeup을 제거할 뿐 실제 deadline demand를 없애지 않는다.**

## CHAPTER 29 · NO_HZ 검증은 isolated workload와 housekeeping saturation을 동시에 stress해야 한다

Test는 nohz_full CPU에 single compute task를 고정하고 timer/IRQ/kthread noise를 측정하는 것뿐 아니라 housekeeping CPUs에 network/storage/RCU workload를 증가시켜 offload capacity가 부족할 때 어떤 일이 생기는지 확인해야 한다. Hotplug, syscall burst, second runnable task, POSIX CPU timer를 의도적으로 넣어 tick 재activation과 recovery도 본다. `평온한 benchmark에서 tick 0`보다 **precondition이 깨졌다가 다시 tickless state로 수렴하는지**가 system robustness다.

## CHAPTER 30 · Tickless correctness는 periodic heartbeat 없이도 모든 progress invariant가 유지된다는 proof다

NO_HZ configuration을 승인하려면 `next mandatory event를 누가 program하는가`, `RCU quiescence를 tick 없이 어떻게 아는가`, `CPU accounting이 boundary에서 보존되는가`, `second runnable task가 생기면 fairness tick이 돌아오는가`, `RCU/workqueue/IRQ/timer maintenance가 어느 housekeeping CPU로 갔는가`, `housekeeping saturation이 isolated progress를 막지 않는가`, `hotplug/suspend 뒤 placement가 다시 맞는가`를 답할 수 있어야 한다. **Tickless kernel은 heartbeat를 삭제한 kernel이 아니라 heartbeat가 담당하던 책임을 더 정밀한 event와 remote owner에게 분해한 kernel**이다.
