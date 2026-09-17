# PART 23 · Scheduler Internals — EEVDF, runqueue, utilization, RT, deadline

Scheduler는 `여러 thread를 번갈아 실행한다`는 설명으로는 부족하다. 실제 kernel은 runnable task의 fairness, latency, deadline, affinity, CPU capacity, thermal·power state, cgroup budget을 동시에 고려해 **누가 언제 어느 CPU에서 얼마나 실행될지** 결정한다. Scheduling bug는 CPU가 100%일 때만 나타나는 것이 아니라 wakeup delay, priority inversion, migration churn, bandwidth throttling, heterogeneous placement처럼 latency tail을 통해 드러난다.

---

## CHAPTER 01 · Runnable과 running은 다른 상태다

Task가 runnable이라는 것은 CPU만 받으면 실행할 준비가 됐다는 뜻이고, running은 실제 CPU에서 instruction을 수행 중이라는 뜻이다. Runnable task가 많아지면 runqueue에서 대기하는 시간이 생긴다. CPU utilization이 높다는 사실만으로 queue delay를 알 수 없다. 한 CPU에서 10개의 짧은 runnable task가 경쟁하는 상태와 1개의 compute-bound task가 혼자 100%를 사용하는 상태는 latency 특성이 다르다.

Scheduler 분석의 기본 단위는 `실행시간`뿐 아니라 **runnable wait time**이다. 응답 지연을 CPU service time과 scheduler wait time으로 분리해야 한다.

---

## CHAPTER 02 · Scheduling class는 서로 다른 policy를 한 queue에 섞지 않기 위한 구조다

Linux는 normal/fair task, real-time task, deadline task처럼 서로 다른 scheduling class를 가진다. 각 class는 자신의 ordering과 preemption rule을 사용하고 class 사이에도 priority relationship이 존재한다. SCHED_NORMAL workload를 분석한 결론을 SCHED_FIFO task에 그대로 적용하면 틀린다.

Scheduler policy 변경은 단순 tuning knob가 아니라 starvation·system liveness에 영향을 줄 수 있다. RT priority를 잘못 사용하면 일반 task가 CPU를 거의 못 받을 수 있으므로 privilege와 runtime limit이 중요하다.

---

## CHAPTER 03 · EEVDF는 fairness를 lag와 virtual deadline으로 표현한다

EEVDF는 task가 공정한 몫보다 덜 실행됐는지 더 실행됐는지를 lag로 추적한다. Positive lag는 task가 CPU time을 받을 채무가 남아 있음을, negative lag는 이미 상대적으로 많이 실행됐음을 나타낸다. Eligible task 중 virtual deadline이 가장 이른 task를 우선 선택하는 방식으로 fairness와 latency를 결합한다.

이 모델은 `round-robin으로 같은 시간씩`이라는 설명보다 강하다. Task weight와 requested slice가 다르면 virtual time 진행과 deadline이 달라진다. Scheduler trace에서 단순 switch 순서만 보지 말고 task weight·lag·slice 정책을 이해해야 한다.

---

## CHAPTER 04 · Virtual runtime은 physical elapsed time과 같은 단위가 아니다

Fair scheduler는 task weight를 반영해 execution을 virtual-time domain에 투영한다. 높은 weight task는 같은 physical CPU time을 사용해도 virtual runtime이 다르게 증가할 수 있다. 이 때문에 wall-clock 5ms를 실행했다는 사실만으로 scheduler fairness를 판단할 수 없다.

Virtual runtime은 idealized fair CPU model을 근사하기 위한 accounting state다. Profiling에서 application elapsed time, on-CPU time, scheduler virtual accounting을 같은 값으로 섞지 않는다.

---

## CHAPTER 05 · Wakeup preemption은 interactive latency를 크게 좌우한다

잠들어 있던 task가 event를 받고 runnable이 됐을 때 현재 실행 중인 task를 즉시 preempt할지, 기존 slice가 더 진행되도록 둘지는 responsiveness와 context-switch overhead를 바꾼다. Latency-sensitive task가 짧은 slice/early virtual deadline을 가지면 빠른 service를 받을 수 있다.

Wakeup-heavy workload에서는 CPU utilization보다 **wakeup-to-run latency**가 핵심 metric이다. UI main thread, audio thread, request worker가 event 이후 언제 실제 CPU를 받았는지 측정해야 한다.

---

## CHAPTER 06 · Context switch는 register 저장 이상의 비용을 가진다

Task 전환에는 architectural state save/restore 외에도 cache/TLB locality 상실, branch predictor state interference, kernel bookkeeping이 따른다. Context-switch count가 늘었다고 무조건 문제가 되는 것은 아니지만 work quantum이 너무 짧으면 useful computation 대비 overhead 비율이 커질 수 있다.

Thread 수를 늘려 throughput이 떨어질 때 lock contention뿐 아니라 scheduler churn과 cache footprint를 같이 봐야 한다.

---

## CHAPTER 07 · Per-CPU runqueue는 scalability를 높이는 대신 load balancing을 필요로 한다

모든 CPU가 하나의 global queue를 강하게 lock하면 multi-core scalability가 나빠진다. Per-CPU runqueue 구조는 local scheduling을 빠르게 만들지만 CPU별 load가 불균형해질 수 있다. Scheduler는 idle CPU pull, periodic balancing, wakeup placement 같은 mechanism으로 work를 이동시킨다.

Migration은 idle core를 활용하게 하지만 cache/NUMA locality를 잃을 수 있다. `항상 가장 빈 CPU로 이동`이 최적이 아닌 이유다.

---

## CHAPTER 08 · Load balance의 입력은 task count보다 utilization과 capacity다

Task 두 개가 있다고 해서 load가 두 배라는 뜻은 아니다. 하나는 1% CPU를 쓰고 하나는 90%를 쓸 수 있다. Heterogeneous CPU에서는 동일 utilization도 CPU capacity에 따라 의미가 달라진다. Scheduler는 runnable demand와 CPU capacity를 normalization해 placement 판단에 사용한다.

Load balancing bug를 찾을 때 runqueue length, util_avg, CPU capacity, affinity를 같이 봐야 한다.

---

## CHAPTER 09 · PELT는 utilization history를 지수적으로 누적하는 signal이다

Per-Entity Load Tracking은 task와 runqueue의 utilization/load history를 시간에 따라 decay시키며 추적한다. 최근 activity가 더 큰 영향을 갖고 오래된 activity는 점차 줄어든다. 이 signal은 scheduler placement와 schedutil frequency selection에 연결된다.

짧은 burst task는 실제로 즉시 높은 CPU demand가 생겨도 utilization signal이 ramp-up되는 데 시간이 걸릴 수 있다. 그래서 latency-critical burst에 uclamp 같은 hint가 필요한 경우가 있다.

---

## CHAPTER 10 · Utilization clamp는 실제 사용량을 바꾸지 않고 decision input을 제한한다

UCLAMP_MIN과 UCLAMP_MAX는 task의 measured utilization 자체를 조작하는 것이 아니라 scheduler가 placement/frequency 결정을 할 때 사용할 effective bound를 제공한다. UCLAMP_MIN은 최소 performance expectation을, UCLAMP_MAX는 energy/thermal reason으로 최대 performance requirement를 제한하는 데 사용할 수 있다.

Static clamp는 device별 capacity가 달라 portable하지 않을 수 있다. Feedback loop와 workload SLO를 기준으로 조절해야 한다.

---

## CHAPTER 11 · schedutil은 scheduler utilization과 CPUFreq를 연결한다

Schedutil governor는 scheduler가 계산한 utilization signal을 바탕으로 CPU performance request를 만든다. Scheduler placement와 frequency scaling이 독립 subsystem처럼 보여도 실제로는 같은 utilization signal을 공유한다. Task migration이나 uclamp 변경이 frequency behavior까지 바꿀 수 있다.

Frequency transition latency와 rate limit 때문에 task가 깨어난 즉시 원하는 performance state에 도달하지 못할 수 있다. Scheduler trace와 cpufreq trace를 함께 봐야 한다.

---

## CHAPTER 12 · CPU affinity는 scheduling search space를 줄인다

Affinity mask는 task가 실행될 수 있는 CPU set을 제한한다. 이는 cache locality와 isolation에 도움이 될 수 있지만 load balancer가 선택할 수 있는 CPU를 줄인다. 한 core에 여러 latency-sensitive task를 pinning하면 다른 CPU가 idle이어도 queue delay가 생긴다.

Affinity는 topology-aware하게 설정해야 한다. SMT sibling, NUMA node, big/little capacity를 무시하면 성능이 악화된다.

---

## CHAPTER 13 · SMT sibling은 logical CPU 두 개가 완전한 core 두 개라는 뜻이 아니다

Simultaneous Multithreading은 일부 execution resource를 공유하면서 여러 hardware thread의 instruction을 같은 physical core에서 실행한다. 두 runnable task가 SMT sibling에 배치되면 execution unit, cache, frontend resource를 경쟁할 수 있다.

CPU count만 보고 capacity를 계산하면 oversubscription을 과소평가할 수 있다. Workload가 서로 보완적인 resource를 사용할 때는 이득이 있지만 같은 execution unit을 강하게 쓰면 interference가 커진다.

---

## CHAPTER 14 · NUMA-aware scheduling은 CPU와 memory locality를 함께 본다

Task가 다른 NUMA node의 memory를 지속적으로 읽으면 scheduler가 CPU를 memory 쪽으로 옮기거나 memory migration을 고려할 수 있다. 하지만 task migration 자체가 cache locality를 깨뜨린다. Scheduler는 runnable load만이 아니라 memory placement와 locality trade-off를 고려한다.

NUMA incident는 CPU placement trace와 page migration/NUMA fault metric을 함께 봐야 한다.

---

## CHAPTER 15 · Real-time FIFO는 fairness보다 priority를 우선한다

SCHED_FIFO class에서 높은-priority runnable task는 낮은 priority task보다 우선하며 같은 priority의 task는 특정 event까지 계속 실행할 수 있다. 일반 fair scheduler의 virtual deadline 개념으로 동작하지 않는다. 잘못된 RT loop는 CPU를 장시간 점유해 system responsiveness를 무너뜨릴 수 있다.

RT policy는 worst-case execution time과 blocking section을 분석하고 runtime limit을 두어야 한다.

---

## CHAPTER 16 · Round-robin RT는 동일 priority task 사이 quantum을 도입한다

SCHED_RR은 FIFO 계열 priority ordering을 유지하면서 동일 priority runnable task 사이에 time quantum을 사용한다. Quantum이 너무 작으면 context-switch overhead가 늘고 너무 크면 peer latency가 커진다.

RT scheduling parameter는 평균 workload가 아니라 worst-case timing requirement와 interference를 기준으로 선택한다.

---

## CHAPTER 17 · Priority inversion은 scheduler priority만으로 해결되지 않는다

High-priority task가 mutex를 기다리고 mutex owner가 low-priority인데 medium-priority task들이 CPU를 계속 사용하면 high-priority task가 간접적으로 오래 막힐 수 있다. Priority inheritance는 lock owner의 effective priority를 일시적으로 올려 inversion을 줄이는 mechanism이다.

RT correctness는 scheduler class와 synchronization primitive를 함께 봐야 한다. Lock chain이 여러 단계면 blocking bound 계산도 복잡해진다.

---

## CHAPTER 18 · SCHED_DEADLINE은 runtime·period·deadline을 계약으로 사용한다

Deadline scheduler는 task가 period마다 runtime budget을 deadline 안에 받도록 EDF와 Constant Bandwidth Server 원리를 사용한다. `priority 숫자가 높다`가 아니라 temporal reservation을 명시한다.

예를 들어 runtime 2ms, period 10ms, deadline 10ms는 CPU utilization reservation 20%와 연결된다. 여러 task의 admission 가능성은 total utilization과 multiprocessor constraints에 영향을 받는다.

---

## CHAPTER 19 · CBS는 deadline task가 budget을 초과해 다른 task를 파괴하지 않도록 한다

Constant Bandwidth Server는 task가 runtime budget을 소진하면 replenishment/deadline rule을 적용해 bandwidth를 제한한다. Without enforcement, 한 deadline task의 overrun이 다른 reservation을 침범할 수 있다.

Deadline miss 분석에서는 task own execution overrun, blocking, migration, interrupt interference를 구분해야 한다.

---

## CHAPTER 20 · Admission control은 impossible guarantee를 미리 거부한다

모든 real-time reservation을 받아들이면 total requested CPU가 physical capacity를 초과할 수 있다. Scheduler는 일부 policy에서 admission control을 통해 feasibility를 검사한다. Guarantee를 제공하려면 resource budget을 먼저 예약해야 한다.

`실행해보고 늦으면 scale up`은 hard timing requirement에 충분하지 않다. Worst-case demand와 available capacity의 관계를 사전에 검증해야 한다.

---

## CHAPTER 21 · cgroup CPU bandwidth는 group 단위로 CPU time을 제한한다

Container/service group이 일정 period 동안 사용할 수 있는 CPU quota를 제한하면 process 내부 task가 runnable이어도 quota를 소진한 뒤 throttled될 수 있다. Application profiler는 thread가 runnable인 것을 보지만 실제로는 cgroup bandwidth가 CPU 공급을 막는 상황이다.

Kubernetes/container latency 문제에서 host CPU utilization이 낮아도 cgroup throttle counter가 높으면 quota가 원인일 수 있다. Scheduler wait와 policy throttle을 분리한다.

---

## CHAPTER 22 · CPU weight는 quota와 다른 control이다

Weight/share는 CPU가 경쟁할 때 상대적인 몫을 정하지만 idle CPU가 있을 때 hard cap처럼 막지 않을 수 있다. Quota는 absolute bandwidth ceiling을 만든다. 둘을 같은 `CPU limit`으로 부르면 behavior를 잘못 예측한다.

Multi-tenant service는 weight로 fairness를 조절하고 quota로 runaway usage를 containment할 수 있다. Latency SLO와 background throughput에 서로 다른 policy가 필요하다.

---

## CHAPTER 23 · IRQ와 softirq도 CPU time을 소비한다

Application task가 실행하지 않는 동안 network/storage interrupt handling과 softirq가 CPU를 사용할 수 있다. Host CPU 100%에서 process CPU만 60%라면 나머지가 kernel/IRQ work일 수 있다. Scheduler 관점에서 application에 사용할 수 있는 effective capacity가 줄어든다.

Packet storm에서 user thread를 더 늘려도 IRQ load가 bottleneck이면 throughput이 늘지 않는다. IRQ affinity와 RPS/RFS 같은 network placement까지 같이 볼 수 있다.

---

## CHAPTER 24 · Preemption model은 kernel code가 얼마나 빨리 task 전환을 허용하는지 바꾼다

Kernel preemption configuration과 critical section은 high-priority task가 ready가 되어도 실제 전환 가능한 시점을 제한할 수 있다. Long non-preemptible section은 scheduler policy와 무관하게 latency floor를 만든다.

Realtime latency 분석은 scheduler decision 이후 `왜 즉시 실행되지 않았는가`를 kernel preemption-disabled interval, IRQ-off section, lock contention까지 내려가야 한다.

---

## CHAPTER 25 · Migration cost는 task마다 다르다

작은 stateless worker는 CPU migration 비용이 낮을 수 있지만 large cache footprint나 NUMA-local memory를 가진 task는 migration 후 cold-cache penalty가 크다. Scheduler가 load balance를 위해 자주 migration하면 fairness는 좋아져도 throughput이 떨어질 수 있다.

Migration rate와 LLC miss/NUMA remote access 증가를 correlation하면 placement churn을 찾을 수 있다.

---

## CHAPTER 26 · Wakeup affinity는 producer-consumer locality를 활용할 수 있다

한 task가 다른 task를 깨울 때 같은 CPU나 가까운 CPU에 배치하면 shared data cache locality가 좋아질 수 있다. 반대로 해당 CPU가 이미 과부하라면 locality보다 queue delay가 더 큰 문제가 된다. Scheduler는 wakeup placement에서 locality와 load를 함께 고려한다.

Lock handoff와 network packet processing처럼 producer-consumer chain이 긴 workload는 wakeup topology가 tail latency에 큰 영향을 줄 수 있다.

---

## CHAPTER 27 · Scheduler trace는 switch와 wakeup을 같이 봐야 한다

`sched_switch`만 보면 누가 실행됐는지는 알 수 있지만 언제 runnable이 됐는지 모른다. `sched_wakeup`, migrate event, throttling, CPU frequency/idle event를 함께 보면 ready→run delay와 실행 후 sleep reason을 복원할 수 있다.

Latency incident에서 최소 timeline은 `event arrival → wakeup → runnable queue → switch-in → execution → blocking`이다. Application log timestamp만으로는 queue 구간을 볼 수 없다.

---

## CHAPTER 28 · Scheduler benchmark는 steady load와 burst load를 분리해야 한다

Fair throughput benchmark는 장시간 CPU-bound task에 적합하지만 interactive latency는 short burst, wakeup, idle transition에서 결정된다. 동일 scheduler change가 throughput은 개선하고 wake latency는 악화시킬 수 있다.

Benchmark suite는 CPU-bound fairness, wakeup latency, migration cost, RT deadline, cgroup throttle scenario를 분리해야 한다. 하나의 score로 scheduler quality를 요약하면 trade-off를 숨긴다.

---

## CHAPTER 29 · Scheduling bug를 application code만 수정해 해결하려 하지 않는다

Thread pool size를 바꿔 latency가 좋아졌다면 원인이 `thread가 너무 많았다`에서 끝나지 않는다. Runqueue delay, lock contention, cache migration, quota throttle 중 어떤 mechanism이 줄었는지 증명해야 다른 machine/workload에서도 적용할 수 있다.

Scheduler evidence 없이 concurrency setting을 바꾸면 accidental tuning이 된다. 변경 전후 task-state distribution과 scheduler event를 기록한다.

---

## CHAPTER 30 · Scheduler 설계의 최종 계약은 fairness·latency·isolation·capacity-awareness다

Scheduler는 서로 충돌할 수 있는 네 목표를 조정한다.

1. **Fairness** — 장기적으로 task가 weight에 맞는 CPU share를 받는가.
2. **Latency** — event 뒤 필요한 task가 충분히 빨리 실행되는가.
3. **Isolation** — 한 workload가 다른 workload의 CPU budget을 무제한 침범하지 않는가.
4. **Capacity-awareness** — heterogeneous/thermal/frequency 상태를 고려해 실제 처리능력에 맞게 배치하는가.

EEVDF, RT/deadline class, uclamp, cgroup bandwidth, affinity는 이 목표의 서로 다른 부분을 제어한다. 수석 개발자는 `thread priority를 올린다`가 아니라 **어떤 scheduler contract를 바꾸며 그 결과 어떤 workload가 이득·손해를 보는지**까지 설명해야 한다.
