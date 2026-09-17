# PART 23 · Scheduler Internals — EEVDF, runqueue, RT, deadline

스케줄러는 CPU 시간을 단순히 나누는 코드가 아니다. runnable state, priority, deadline, locality, thermal capacity, cgroup policy가 동시에 적용되어 어떤 task가 언제 어느 CPU에서 실행될지 결정한다. latency를 설명하려면 on-CPU time과 runnable-but-not-running 시간을 분리해야 한다.

---

## CHAPTER 01 · runnable과 running

runnable task는 실행할 준비가 되었지만 아직 CPU를 받지 못한 상태이고 running task는 실제 core에서 instruction을 실행 중이다. 두 상태 사이의 queue delay가 user-visible latency의 큰 부분이 될 수 있다.

CPU utilization이 낮아 보여도 특정 CPU runqueue가 길거나 affinity가 좁으면 task가 오래 runnable 상태로 남을 수 있다. blocked와 runnable wait를 같은 '대기'로 합치면 scheduler 문제와 I/O 문제를 구분할 수 없다.

wakeup, enqueue, switch timestamp를 연결해 runnable delay를 측정한다. optimization은 total runtime만 줄이는 것이 아니라 deadline 전에 CPU를 얻는 probability를 높이는지 확인한다.

---

## CHAPTER 02 · scheduling class

scheduling class는 normal, real-time, deadline처럼 서로 다른 policy와 priority relation을 정의한다. 같은 numeric priority처럼 보여도 class가 다르면 선점 규칙과 starvation 가능성이 달라진다.

높은 class를 사용하면 latency가 자동 개선되는 것이 아니다. 잘못된 RT task는 normal workload 전체를 굶길 수 있고 deadline reservation이 과도하면 admission이 실패할 수 있다.

incident에는 class, priority, policy, runtime limit을 함께 기록한다. application 요구와 맞지 않는 class를 성능 문제의 임시 우회로로 사용하지 않는다.

---

## CHAPTER 03 · EEVDF

EEVDF는 eligible task와 virtual deadline을 사용해 공정성과 latency를 조절하는 scheduler model이다. 모든 runnable task를 단순 round-robin으로 순환하는 구조가 아니며 weight와 service history가 selection에 영향을 준다.

virtual deadline이 빠른 task라도 eligibility 조건을 만족해야 한다. 따라서 한 시점의 priority만 보고 다음 task를 예측하기 어렵다. workload의 wakeup pattern과 weight가 함께 중요하다.

scheduler trace에서 enqueue, virtual service progression, actual switch를 비교한다. tuning은 특정 task를 항상 먼저 실행시키는 것이 아니라 fairness contract 안에서 latency 목표를 만족시키는 방향이어야 한다.

---

## CHAPTER 04 · virtual runtime

virtual runtime은 실제 실행 시간을 weight와 capacity 관점에서 정규화한 논리적 progress 값이다. 서로 다른 weight의 task가 CPU share를 공정하게 비교할 수 있게 한다.

wall-clock runtime과 virtual runtime을 혼동하면 scheduler behavior를 잘못 해석한다. CPU frequency나 task weight가 달라도 service accounting 목적은 동일하지 않을 수 있다.

trace에서 actual runtime, weight, virtual progress를 분리해 본다. fairness regression은 단일 task throughput보다 competing workload의 service ratio로 검증한다.

---

## CHAPTER 05 · wakeup preemption

wakeup preemption은 새로 runnable이 된 task가 현재 running task를 선점할 가치가 있는지 판단한다. interactive workload의 response time을 줄일 수 있지만 과도한 선점은 context switch와 cache disruption을 늘린다.

짧은 sleeper가 반복 wakeup하면 CPU-bound task의 progress가 불안정해질 수 있다. 반대로 wakeup task를 너무 늦게 실행하면 input latency가 커진다.

wakeup-to-run latency와 context switch rate를 함께 측정한다. latency만 줄이고 throughput이 크게 손상되지 않는지 workload mix로 확인한다.

---

## CHAPTER 06 · context switch

context switch는 register와 stack pointer를 바꾸는 것뿐 아니라 cache, TLB, branch predictor locality에 영향을 줄 수 있다. oversubscription에서 switch frequency가 높아지면 간접 비용이 커진다.

thread 수를 늘렸는데 CPU 사용률은 높고 throughput이 떨어지는 경우 useful work보다 scheduling overhead가 늘었을 수 있다. NUMA migration과 결합되면 memory locality도 깨진다.

switch rate, migration, cache miss, runnable delay를 함께 본다. 단순 ns 단위 switch cost 하나로 application impact를 설명하지 않는다.

---

## CHAPTER 07 · per-CPU runqueue

per-CPU runqueue는 global scheduling lock을 줄이고 cache locality를 높이는 구조다. 하지만 CPU별 load가 달라지면 idle CPU가 있는데 다른 CPU queue는 길어지는 imbalance가 생길 수 있다.

affinity와 cgroup cpuset가 balancing 범위를 제한할 수 있다. task migration은 imbalance를 줄이지만 locality를 희생한다.

CPU별 runnable count와 idle time을 동시에 본다. load balancing이 자주 일어나는지, 특정 CPU에 hot task가 고정되는지 trace로 확인한다.

---

## CHAPTER 08 · load balance

load balance는 overloaded CPU에서 idle 또는 less-loaded CPU로 task를 옮겨 capacity를 활용한다. migration이 항상 좋은 것은 아니며 cache warmth와 NUMA placement 비용을 함께 고려해야 한다.

짧은 task를 계속 옮기면 migration cost가 execution time보다 커질 수 있다. heterogeneous CPU에서는 단순 runnable count보다 capacity 차이가 중요하다.

migration reason, source/destination CPU, task age를 기록한다. throughput과 tail latency가 모두 개선되는지 확인해 balancing aggressiveness를 조정한다.

---

## CHAPTER 09 · PELT

PELT는 최근 utilization/load를 지수적으로 누적해 short burst와 sustained demand를 구분한다. scheduler와 DVFS가 task의 순간 사용량보다 시간에 따른 demand를 볼 수 있게 한다.

새 task나 burst workload는 historical signal이 충분하지 않아 실제 demand와 estimate가 어긋날 수 있다. decay와 update timing이 policy response에 영향을 준다.

PELT signal과 actual runtime, frequency를 함께 추적한다. utilization estimator가 workload phase 변화에 얼마나 빨리 적응하는지 확인한다.

---

## CHAPTER 10 · uclamp

uclamp는 task나 cgroup의 utilization expectation에 최소·최대 범위를 적용해 placement와 frequency policy에 영향을 준다. latency-sensitive task에 minimum capacity를 요구하거나 background task의 maximum을 제한할 수 있다.

clamp를 너무 높게 잡으면 energy와 thermal cost가 커지고, 너무 낮으면 deadline을 놓칠 수 있다. nested cgroup과 task-level setting이 함께 적용되는 effective value를 봐야 한다.

effective clamp, CPU placement, frequency, task latency를 함께 기록한다. magic value보다 workload requirement에서 clamp를 도출한다.

---

## CHAPTER 11 · schedutil

schedutil은 scheduler utilization signal을 사용해 CPU frequency를 선택한다. task placement와 DVFS를 같은 demand estimate에 연결해 response를 빠르게 만들 수 있다.

utilization signal이 burst를 늦게 반영하거나 thermal limit가 걸리면 requested frequency와 actual frequency가 달라진다. scheduler tuning과 power tuning을 분리해서 보면 feedback 문제를 놓칠 수 있다.

request/actual frequency, PELT, runqueue delay를 같은 timeline에 둔다. code regression과 frequency policy 변화를 구분한다.

---

## CHAPTER 12 · affinity

affinity는 task가 실행 가능한 CPU 집합을 제한한다. cache locality와 jitter를 줄일 수 있지만 너무 좁은 mask는 idle CPU가 있어도 task를 한 CPU에 가둔다.

CPU hotplug, heterogeneous topology, SMT sibling을 고려해야 한다. hard pinning이 scheduler의 load balancing과 thermal avoidance를 막을 수 있다.

mask와 actual migration history를 함께 기록한다. affinity 적용 전후 latency뿐 아니라 runqueue pressure와 thermal state도 비교한다.

---

## CHAPTER 13 · SMT

SMT는 한 physical core가 여러 hardware thread를 실행해 execution resource 활용도를 높인다. logical CPU 두 개가 independent physical core 두 개와 같은 capacity를 제공하는 것은 아니다.

두 sibling이 같은 execution port, cache, bandwidth를 경쟁하면 workload pairing에 따라 성능이 크게 달라진다. security threat model에서도 shared microarchitecture가 문제가 될 수 있다.

sibling topology와 co-running workload를 기록한다. benchmark는 SMT on/off 또는 sibling placement를 분리해 비교한다.

---

## CHAPTER 14 · NUMA scheduling

NUMA scheduling은 task가 실행되는 CPU와 memory page가 위치한 node를 가깝게 유지하려 한다. remote memory access가 많으면 CPU가 idle하지 않아도 latency가 증가한다.

thread migration과 page migration이 서로 따라다니면 locality가 안정되지 않을 수 있다. first-touch placement와 application sharding도 scheduler 결과에 영향을 준다.

CPU/node migration, remote access, memory bandwidth를 같이 본다. placement 변경 후 실제 remote traffic이 줄었는지 확인한다.

---

## CHAPTER 15 · RT FIFO

RT FIFO는 같은 priority에서 time slice 없이 실행될 수 있으며 높은 priority task가 block 또는 yield할 때까지 CPU를 점유할 수 있다. 잘못 사용하면 normal task starvation을 만들 수 있다.

long critical section이나 unexpected loop가 RT priority에서 실행되면 system responsiveness가 급격히 나빠진다. watchdog과 runtime limit가 필요할 수 있다.

RT task의 run duration과 preemption history를 기록한다. latency requirement가 실제 RT class를 필요로 하는지 먼저 검토한다.

---

## CHAPTER 16 · RT RR

RT round-robin은 같은 priority task 사이에 time slice를 적용한다. 낮은 priority class에 대한 선점 관계는 유지되므로 normal workload 보호가 자동으로 되지는 않는다.

slice가 너무 길면 peer RT task latency가 커지고 너무 짧으면 switch overhead가 늘어난다. workload 수와 criticality에 따라 설정해야 한다.

slice, runnable RT count, deadline miss를 함께 측정한다. average response보다 worst-case wait를 본다.

---

## CHAPTER 17 · priority inversion

priority inversion은 high-priority task가 low-priority lock owner를 기다리고 medium-priority task가 owner 실행을 방해할 때 발생한다. 결과적으로 high-priority task가 medium task보다 늦게 progress한다.

priority inheritance는 owner를 임시 boost해 문제를 완화할 수 있지만 nested lock과 chain blocking은 복잡하다. lock order와 hold time도 함께 관리해야 한다.

waiter priority, owner state, lock hold duration을 scheduler trace와 연결한다. timeout을 늘려 증상을 숨기지 않는다.

---

## CHAPTER 18 · deadline contract

deadline scheduling은 runtime, period, relative deadline 같은 reservation으로 task의 CPU 요구를 표현한다. priority 숫자보다 시간 제약 자체를 scheduler contract에 넣는다.

실제 workload가 선언 runtime을 초과하면 다른 deadline task 보장을 침범할 수 있다. execution variance와 overrun 정책을 이해해야 한다.

runtime consumption과 deadline miss를 기록한다. reservation 값은 worst-case 또는 measured distribution과 연결한다.

---

## CHAPTER 19 · CBS

Constant Bandwidth Server는 deadline task가 reservation을 넘게 CPU를 쓰지 못하도록 runtime budget과 deadline을 조절한다. 한 task의 burst가 전체 real-time capacity를 무너뜨리는 것을 제한한다.

budget exhaustion 뒤 replenishment behavior를 모르면 periodic latency spike를 application bug로 오해할 수 있다. task execution pattern과 reservation period가 맞아야 한다.

budget remaining, throttling, deadline shift를 trace한다. workload 변화 뒤 reservation을 재평가한다.

---

## CHAPTER 20 · admission control

admission control은 deadline workload의 declared utilization이 available CPU capacity 안에 들어오는지 확인해 과도한 reservation을 사전에 거부한다. 이미 overload된 system에 task를 더 넣고 runtime에서 해결하려는 방식보다 안전하다.

CPU affinity나 cpuset가 capacity를 줄이면 global CPU 수로 admission을 계산하면 틀릴 수 있다. migration 가능한 domain도 고려한다.

reservation 합계와 effective capacity를 비교한다. configuration change 후 admission invariant가 유지되는지 자동 검증한다.

---

## CHAPTER 21 · cgroup bandwidth

cgroup CPU bandwidth는 quota와 period로 group이 일정 시간 동안 사용할 CPU 양을 제한한다. host에 idle CPU가 있어도 quota를 다 쓰면 group은 throttled될 수 있다.

application CPU utilization만 보면 throttling을 CPU shortage와 구분하기 어렵다. 짧은 burst가 period 경계에 걸리면 latency distribution이 계단형으로 나타날 수 있다.

throttled time, quota usage, request latency를 correlation한다. quota 조정은 downstream capacity와 multi-tenant fairness를 함께 본다.

---

## CHAPTER 22 · CPU weight

CPU weight는 competing cgroup 사이 relative share를 조정한다. absolute core 보장을 뜻하지 않고 competition이 없으면 더 많은 CPU를 사용할 수도 있다.

weight ratio를 throughput ratio와 단순 동일시하면 안 된다. task 수, affinity, capacity, scheduler class가 결과에 영향을 준다.

competition workload에서 actual CPU share와 latency를 측정한다. service tier 정책이 intended fairness로 나타나는지 검증한다.

---

## CHAPTER 23 · IRQ and softirq

IRQ와 softirq는 application thread 밖에서 CPU를 소비한다. network나 storage interrupt가 특정 CPU에 몰리면 process CPU metric은 낮은데 task runqueue가 길어질 수 있다.

softirq backlog가 커지면 packet drop이나 latency가 application layer에서만 보일 수 있다. interrupt affinity와 NAPI polling도 scheduler pressure를 바꾼다.

per-CPU IRQ/softirq time과 runqueue delay를 함께 본다. application thread만 profile해서는 원인을 놓칠 수 있다.

---

## CHAPTER 24 · preemption model

kernel preemption model은 kernel execution 중 더 높은 priority work로 전환할 수 있는 지점을 바꾼다. throughput과 worst-case scheduling latency 사이 trade-off가 달라진다.

non-preemptible section이 길면 user task가 runnable이어도 CPU를 늦게 받을 수 있다. 반대로 preemption point가 많으면 overhead와 locking requirement가 달라진다.

latency trace에서 kernel section과 wakeup delay를 연결한다. model 변경은 average throughput뿐 아니라 max latency로 검증한다.

---

## CHAPTER 25 · migration cost

task migration은 다른 CPU capacity를 활용하게 하지만 cache, TLB, NUMA locality를 잃을 수 있다. migration cost는 task working set과 topology에 따라 크게 달라진다.

짧은 task가 자주 이동하면 useful work보다 cache warmup 비용이 커질 수 있다. load balance가 완벽해 보여도 throughput이 떨어지는 이유가 된다.

migration count, cache miss, remote memory를 함께 측정한다. affinity를 강제하기 전에 실제 migration cost를 증명한다.

---

## CHAPTER 26 · wakeup affinity

wakeup affinity는 새로 깨어난 task를 producer와 가까운 CPU나 cache-friendly CPU에 두어 handoff latency를 줄이려는 policy다. 항상 same CPU가 최적은 아니며 load imbalance와 trade-off가 있다.

producer가 overloaded CPU에 있으면 locality를 지키려다 queue delay가 더 커질 수 있다. workload graph와 topology를 함께 봐야 한다.

waker/wakee CPU, migration, wakeup latency를 trace한다. policy 변경 전후의 cache miss와 runqueue wait를 함께 비교한다.

---

## CHAPTER 27 · scheduler trace

scheduler trace는 wakeup, enqueue, switch, migration, throttle event를 시간축으로 보여 준다. stack snapshot 한 장보다 task가 왜 늦게 실행됐는지 설명하기 좋다.

trace clock과 event loss를 확인해야 한다. 높은 event rate에서 buffer가 넘치면 가장 바쁜 구간의 evidence가 사라질 수 있다.

request ID를 task timeline과 연결해 user symptom과 scheduler state를 매칭한다. trace overhead가 workload를 바꾸지 않는지도 확인한다.

---

## CHAPTER 28 · scheduler benchmark

scheduler benchmark는 CPU-bound, sleep/wakeup, mixed I/O, oversubscribed workload를 분리해야 한다. 단일 microbenchmark는 실제 service mix를 대표하지 못한다.

core count, SMT, affinity, governor, background load를 고정한다. warm cache와 thermal state도 결과를 바꿀 수 있다.

throughput, runnable latency, switch, migration distribution을 함께 저장한다. score 하나보다 workload별 trade-off curve로 판단한다.

---

## CHAPTER 29 · application tuning

application thread 수를 늘리면 parallelism이 증가할 수 있지만 CPU capacity, lock, DB pool 같은 downstream bottleneck이 그대로면 queue만 늘어난다. scheduler는 없는 capacity를 만들 수 없다.

CPU-bound와 blocking task를 같은 pool에 섞으면 runnable pressure와 starvation이 생길 수 있다. task class별 concurrency limit가 필요하다.

thread count 변화 전후 runqueue와 service time을 비교한다. CPU가 100%가 아니라는 이유만으로 worker를 추가하지 않는다.

---

## CHAPTER 30 · scheduler contract

scheduler contract는 progress, fairness, priority, deadline, placement, throttling의 의미를 workload와 연결해야 한다. application은 자신이 어떤 scheduling guarantee를 기대하는지 명시해야 한다.

RT, affinity, quota 같은 강한 제약은 다른 workload와 system recovery 능력을 줄일 수 있다. performance win과 operational risk를 함께 평가한다.

release gate는 representative contention과 failure scenario를 포함한다. 최종 목표는 benchmark score가 아니라 overload 속에서도 critical work가 예측 가능한 시간 안에 progress하는 것이다.
