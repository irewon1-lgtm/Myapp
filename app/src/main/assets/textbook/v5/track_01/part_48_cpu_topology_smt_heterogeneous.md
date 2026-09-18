# PART 48 · CPU Topology and SMT — sockets, cores, siblings, heterogeneous capacity

CPU 개수는 하나의 숫자가 아니다. 운영체제가 보이는 logical CPU는 socket·core·SMT sibling·NUMA node·cache domain·heterogeneous capacity 위에 놓인다. Scheduler, IRQ, memory allocator, device queue가 이 topology를 어떻게 사용하느냐에 따라 같은 thread 수에서도 latency와 throughput이 크게 달라진다. 이 PART는 “몇 코어인가”라는 질문을 버리고 **실행 자원과 공유 자원의 실제 구조**를 추적한다.

---

## CHAPTER 01 · logical CPU와 physical execution resource는 같은 개념이 아니다

운영체제가 16개의 CPU를 보여준다고 해서 독립적인 physical core가 16개라는 뜻은 아니다. SMT가 켜져 있으면 한 core가 두 개 이상의 logical CPU를 제공할 수 있고, 각 logical CPU는 architectural state는 분리하지만 execution unit·cache 일부를 공유한다. 따라서 thread 수를 `logical CPU 수`와 1:1로 맞추는 규칙은 workload에 따라 틀릴 수 있다.

Topology는 socket → core → thread 수준으로 읽어야 한다. 동일 socket 안에서도 LLC domain이나 chiplet 경계가 나뉠 수 있다. VM에서는 guest에 노출된 topology가 host physical layout과 다를 수 있으므로 “guest가 보는 core”를 곧바로 hardware core로 해석하면 안 된다.

Capacity planning에서는 logical CPU count, physical core count, SMT sibling relation을 별도 필드로 저장한다. CPU 사용률 50%라는 숫자도 어떤 sibling 조합이 사용 중인지에 따라 남은 성능이 다르다.

## CHAPTER 02 · SMT는 idle execution slot을 활용하지만 완전한 2배 자원을 만들지 않는다

SMT는 한 hardware core에서 여러 logical thread의 instruction을 섞어 실행해 pipeline의 빈 자리를 줄인다. Memory stall이 많은 두 thread는 서로 보완될 수 있지만 같은 execution port나 cache를 강하게 쓰는 두 thread는 경쟁할 수 있다. 그래서 SMT 2-way를 “core 수가 정확히 두 배”라고 보는 모델은 과도하게 단순하다.

Throughput workload에서는 SMT가 core utilization을 높여 이득을 줄 수 있다. 반면 single-request tail latency가 중요한 서비스에서는 sibling의 간섭이 jitter를 만들 수 있다. 특히 한 thread가 branch miss나 cache miss를 많이 만들면 sibling의 front-end·cache pressure도 증가한다.

SMT의 가치는 benchmark로 확인한다. 동일 physical core 수에서 SMT on/off를 비교하고, runnable thread 수와 CPU utilization뿐 아니라 p99 latency, IPC, cache miss를 함께 본다.

## CHAPTER 03 · SMT sibling은 cache와 실행 파이프라인의 일부 자원을 공유한다

같은 core의 sibling은 보통 L1/L2 일부, front-end bandwidth, execution unit 같은 자원을 공유한다. 두 thread가 동시에 vector-heavy code를 실행하면 동일한 arithmetic unit을 경쟁할 수 있고, 두 memory-intensive thread가 cache를 압박하면 miss가 늘 수 있다. Scheduler가 단순히 “idle logical CPU”를 찾았다고 해서 완전히 빈 core를 찾은 것은 아니다.

이 sharing은 security에도 영향을 준다. 같은 core에서 서로 다른 trust domain이 동작하면 microarchitectural side channel을 고려해야 할 수 있다. Isolation requirement가 강한 환경에서는 sibling placement까지 정책에 포함한다.

성능 분석에서는 CPU 번호만 보지 말고 sibling 관계를 매핑한다. 특정 pair에서만 latency가 나빠진다면 application lock보다 shared execution resource가 원인일 수 있다.

## CHAPTER 04 · SMT 간섭은 평균보다 tail latency에서 더 크게 보일 수 있다

서비스의 평균 처리 시간은 SMT로 좋아져도 일부 request가 sibling의 burst와 겹치며 크게 느려질 수 있다. CPU-bound background task가 같은 physical core의 sibling에 배치되면 foreground thread가 runnable 상태를 유지하면서도 실행 throughput이 떨어진다. 이 현상은 scheduler wait time만 보면 잘 드러나지 않는다.

Tail을 분석할 때는 느린 request가 실행된 logical CPU와 그 sibling의 workload를 함께 기록한다. `cpu_busy=100%`라는 aggregate 값보다 어떤 process가 같은 core를 공유했는지가 더 직접적인 증거가 된다.

Latency-sensitive pool을 physical core 단위로 격리하고 sibling을 비우는 실험을 통해 원인을 분리할 수 있다. 개선이 확인되면 throughput 손실과 isolation 이득을 비교해 운영 정책을 정한다.

## CHAPTER 05 · cache topology는 thread 간 통신 비용과 data locality를 결정한다

두 thread가 같은 L2를 공유하는지, 같은 LLC만 공유하는지, socket을 넘어가는지에 따라 cache line 전달 비용이 다르다. Producer-consumer가 자주 같은 데이터를 주고받는다면 가까운 cache domain에 배치하는 것이 유리할 수 있다. 반대로 서로 큰 working set을 쓰는 독립 task는 같은 cache를 공유하면 eviction이 늘 수 있다.

Scheduler affinity를 설계할 때 “가까울수록 항상 좋다”는 규칙은 없다. Communication이 많은 task는 locality가 중요하고, cache capacity를 많이 쓰는 task는 분산이 중요하다. Workload 특성에 따라 placement objective가 달라진다.

Hardware topology와 cache miss counter를 함께 본다. 특정 cache domain에서만 miss가 치솟는다면 thread 수보다 placement가 문제일 수 있다.

## CHAPTER 06 · LLC slice와 hash mapping은 하나의 공유 cache도 내부적으로 분산시킨다

Modern CPU의 LLC는 여러 slice로 나뉘고 physical address가 내부 hash에 따라 slice에 배치될 수 있다. Software는 논리적으로 하나의 LLC로 보더라도 access가 interconnect를 지나 다른 slice로 갈 수 있다. 따라서 동일 socket 안에서도 memory latency가 완전히 균일하지 않을 수 있다.

이 구조는 microbenchmark 해석을 어렵게 한다. 작은 배열과 큰 배열, 특정 address pattern이 서로 다른 slice·set 분포를 만들 수 있어 결과가 흔들린다. Randomized allocation과 충분한 sample을 사용하는 이유다.

Application은 내부 hash를 직접 최적화하기보다 locality와 working set을 관리하는 편이 일반적으로 안전하다. 다만 low-level performance debugging에서는 slice contention 가능성을 배제하지 않는다.

## CHAPTER 07 · chiplet 구조에서는 같은 socket 안에도 추가적인 거리 차이가 생긴다

하나의 package가 여러 compute die 또는 chiplet으로 구성되면 core 간 통신이 package 내부 interconnect를 거칠 수 있다. OS가 이를 NUMA node나 cache domain으로 노출하기도 하고 그렇지 않기도 한다. “same socket”만으로 locality를 판단하기 어려운 이유다.

Shared-memory workload에서 cross-chiplet traffic이 많으면 latency와 bandwidth가 달라질 수 있다. Lock-heavy 구조나 shared queue는 이 차이에 민감하다. 반면 embarrassingly parallel task는 각 chiplet에 독립적으로 분배하면 scale이 잘 나올 수 있다.

배포 환경별 topology를 inventory로 남기고 benchmark artifact에 CPU model과 topology를 포함한다. 다른 세대 hardware 결과를 core count만 맞춰 비교하지 않는다.

## CHAPTER 08 · NUMA placement는 CPU와 memory가 가까운 조합을 만드는 문제다

NUMA 시스템에서는 각 CPU가 특정 memory node에 더 가까운 access path를 가진다. Thread가 node 0에서 실행하면서 대부분의 page가 node 1에 있으면 remote traffic이 늘어 latency와 bandwidth가 나빠질 수 있다. CPU affinity만 고정하고 memory placement를 무시하면 절반만 최적화한 것이다.

Large in-memory service는 worker와 data shard를 같은 NUMA node에 배치해 locality를 높일 수 있다. 하지만 workload가 shard 간 데이터를 자주 공유하면 remote access를 완전히 없애기 어렵다. Replication과 partitioning 비용을 함께 본다.

NUMA diagnosis에서는 per-node allocated bytes, remote access counter, thread CPU placement를 시간축으로 맞춘다. 단순 total RSS는 memory가 어디에 있는지 알려주지 않는다.

## CHAPTER 09 · first-touch 정책은 누가 page를 처음 썼는지가 물리 위치를 결정할 수 있다

많은 NUMA 환경에서 anonymous page는 처음 write fault를 일으킨 CPU와 가까운 node에 할당된다. Main thread가 큰 buffer를 단일 thread로 초기화한 뒤 worker에게 나누면 모든 page가 한 node에 몰릴 수 있다. 나중에 worker affinity만 분산해도 memory는 그대로 남는다.

병렬 initialization은 각 worker가 자기 shard를 first-touch하게 만들어 placement를 맞출 수 있다. 다만 zeroing 방법과 allocator behavior가 실제 fault를 언제 발생시키는지 확인해야 한다. Huge page는 더 큰 단위로 placement가 결정될 수 있다.

Initialization benchmark와 steady-state benchmark를 분리해 본다. Startup이 만든 잘못된 placement가 runtime 성능 문제로 보일 수 있다.

## CHAPTER 10 · automatic NUMA balancing은 page와 task를 이동시키지만 비용이 공짜가 아니다

Kernel은 access pattern을 관찰해 task 또는 page를 더 가까운 node로 옮기려 할 수 있다. 이 기능은 수동 배치가 없는 일반 workload에 도움이 되지만 scan fault, migration copy, locality oscillation 같은 비용도 만든다. Working set이 크거나 access pattern이 자주 바뀌면 안정화까지 시간이 걸릴 수 있다.

Benchmark 초반과 장기 steady state 결과가 다른 이유 중 하나가 NUMA balancing이다. 짧은 run에서는 migration 중간 상태만 측정할 수 있다. Tuning 전에 balancing event와 page migration을 관찰한다.

수동 pinning을 도입하면 automatic policy와 충돌하지 않는지 확인한다. 둘 다 locality를 개선하려 해도 서로 다른 objective를 사용하면 불필요한 이동이 생길 수 있다.

## CHAPTER 11 · heterogeneous CPU는 logical CPU마다 처리 capacity가 다를 수 있다

모든 core가 같은 microarchitecture와 최대 frequency를 갖는 symmetric SMP 가정이 깨지는 시스템이 많아졌다. 고성능 core와 효율 core가 섞인 경우 같은 100% utilization이라도 처리량이 다르다. Scheduler는 task 특성과 capacity를 고려해 placement해야 한다.

Latency-sensitive foreground task를 낮은 capacity core에 오래 두면 response time이 늘 수 있고, background task를 고성능 core에 고정하면 energy를 낭비할 수 있다. 단순 CPU 번호 순서에 의미를 부여하지 말고 platform이 제공하는 capacity 정보를 사용한다.

성능 데이터에는 어떤 class의 core에서 실행됐는지 포함한다. 평균 CPU utilization만으로 regression을 설명하려 하면 heterogeneous topology에서 오판하기 쉽다.

## CHAPTER 12 · frequency와 thermal 상태는 동일 core의 순간 capacity를 계속 바꾼다

CPU capacity는 core 종류뿐 아니라 현재 frequency, power budget, thermal throttling에 따라 변한다. 두 benchmark run이 같은 core에서 실행돼도 한쪽이 boost 상태이고 다른 쪽이 thermal limit에 걸리면 결과가 크게 다를 수 있다. Scheduler가 보는 utilization과 실제 처리 능력을 분리해야 한다.

DVFS governor와 thermal control은 workload history에 반응하므로 run 순서도 중요하다. 긴 stress 후 바로 latency test를 하면 낮아진 frequency에서 시작할 수 있다. Warmup은 JIT뿐 아니라 power state에도 적용된다.

Frequency·temperature·throttling event를 benchmark metadata에 포함한다. CPU time만으로 instruction throughput을 추정하지 않는다.

## CHAPTER 13 · uclamp 같은 capacity hint는 scheduler에 최소·최대 성능 요구를 전달한다

Utilization clamping은 task나 cgroup이 scheduler에게 어느 정도의 utilization/capacity 범위를 요구하는지 표현할 수 있다. 최소 clamp는 latency-sensitive task가 너무 낮은 capacity CPU나 frequency에 머무는 것을 줄이는 데 쓰일 수 있고, 최대 clamp는 background workload의 power 사용을 제한하는 데 쓰일 수 있다.

하지만 clamp는 CPU time quota와 다른 개념이다. 실행 시간을 직접 보장하는 것이 아니라 placement·frequency 판단에 영향을 주는 hint다. 잘못된 clamp는 다른 workload와 energy 정책을 방해할 수 있다.

튜닝은 request latency와 energy를 동시에 측정한다. 큰 min clamp를 넣고 성능이 좋아졌다는 이유만으로 항상 유지하면 idle power와 thermal headroom을 잃을 수 있다.

## CHAPTER 14 · migration은 load balance를 돕지만 cache와 NUMA locality를 초기화한다

Scheduler가 thread를 다른 CPU로 이동시키면 runnable load를 고르게 만들 수 있다. 그러나 새 CPU의 private cache에는 working set이 없고, NUMA node까지 바뀌면 memory access 거리도 달라질 수 있다. 짧은 task는 migration cost가 실행 시간보다 클 수 있다.

반대로 과도한 pinning은 한 CPU가 바쁜데 다른 CPU가 놀아도 task를 옮기지 못하게 한다. Throughput workload에서는 locality보다 load balance가 더 중요할 수 있다. Migration rate를 성능 지표와 함께 봐야 한다.

프로파일에서 동일 thread의 CPU history를 추적하면 cache miss spike와 migration의 상관을 확인할 수 있다. Scheduler 문제를 lock contention으로 오해하지 않게 해준다.

## CHAPTER 15 · CPU affinity는 배치 자유도를 줄여 locality를 얻는 도구다

Affinity mask를 설정하면 thread가 실행될 수 있는 CPU 집합을 제한한다. Benchmark isolation, device locality, cache affinity에 유용하지만 mask가 너무 좁으면 burst 처리 능력과 load balancing을 잃는다. 온라인 CPU가 바뀌거나 container cpuset이 변경되면 effective mask도 달라질 수 있다.

Affinity는 logical CPU 단위이므로 SMT sibling relation을 함께 고려한다. “CPU 2,3에 pin”이 두 physical core인지 한 core의 sibling인지 topology에 따라 다르다.

운영 설정은 CPU 번호를 하드코딩하기보다 topology discovery 결과로 생성한다. Hardware SKU가 바뀌면 동일 번호 정책이 전혀 다른 배치를 만들 수 있다.

## CHAPTER 16 · IRQ affinity는 device interrupt 처리와 application CPU locality를 연결한다

NIC·storage interrupt가 특정 CPU에서 처리되면 packet이나 completion 관련 cache line이 그 CPU에 먼저 들어온다. Application worker를 너무 멀리 배치하면 inter-core transfer가 늘 수 있다. 반대로 모든 IRQ를 한 CPU에 몰면 그 CPU가 bottleneck이 된다.

Multiqueue device는 queue별 interrupt vector를 여러 CPU에 분산할 수 있다. RSS, queue steering, application affinity를 함께 설계해야 locality 이득이 생긴다. 한 계층만 조정하면 traffic이 다시 다른 CPU로 이동할 수 있다.

IRQ rate와 softirq time을 CPU별로 본다. 사용자 thread utilization이 낮은데 특정 CPU가 바쁘다면 interrupt handling이 원인일 수 있다.

## CHAPTER 17 · PCIe device locality는 NUMA topology의 또 다른 축이다

NIC, GPU, NVMe는 특정 socket/root complex와 더 가까울 수 있다. Device DMA가 node 0 memory를 주로 사용하고 completion worker가 node 1에서 실행되면 memory와 interrupt traffic이 inter-socket link를 건널 수 있다. CPU만 가까운 곳에 배치해도 buffer가 먼 node에 있으면 이득이 제한된다.

High-throughput I/O에서는 queue, IRQ, worker, buffer allocation을 같은 locality domain에 맞추는 것이 중요하다. Virtualization에서는 vCPU topology와 passthrough device의 host locality도 함께 봐야 한다.

Topology inventory에 device→NUMA node mapping을 포함한다. 서버를 다른 slot에 장착하거나 VM host가 바뀌면 이 관계가 변할 수 있다.

## CHAPTER 18 · memory bandwidth는 core 수 증가보다 먼저 공유 병목이 될 수 있다

Thread를 늘리면 처음에는 처리량이 늘지만 memory-intensive workload에서는 DRAM bandwidth가 포화되어 더 이상 scale하지 않을 수 있다. 이때 CPU utilization은 높아도 IPC는 떨어지고 memory controller queue가 길어진다. Core를 추가해도 같은 shared bandwidth를 경쟁하기 때문이다.

NUMA node별 bandwidth를 분산하면 scale이 개선될 수 있다. 반면 shared read-only data가 한 node에 몰리면 remote traffic이 늘어난다. Workload의 bytes/instruction 특성을 파악한다.

Scaling graph를 1,2,4,8 thread처럼 단계적으로 그려 saturation 지점을 찾는다. 단일 최대 thread 수 결과만 보면 병목이 언제 시작됐는지 알기 어렵다.

## CHAPTER 19 · SMT off 실험은 sibling contention 가설을 직접 검증하는 방법이다

SMT가 문제인지 추측만 하지 말고 동일 hardware에서 physical core당 하나의 logical CPU만 사용하도록 제한해 비교할 수 있다. Throughput 감소와 latency 개선을 동시에 측정하면 workload가 shared core resource에 얼마나 민감한지 보인다.

Firmware에서 SMT 자체를 끄는 것과 scheduler affinity로 sibling을 비우는 것은 security와 power 측면에서 다를 수 있다. 실험 목적에 맞는 방법을 선택한다. VM에서는 host SMT 상태를 guest가 완전히 제어하지 못할 수도 있다.

결과는 CPU model별로 달라질 수 있으므로 일반화하지 않는다. 새로운 세대 CPU나 compiler 변경 후 같은 실험을 반복할 가치가 있다.

## CHAPTER 20 · oversubscription은 runnable thread가 execution slot보다 많은 상태다

Runnable thread 수가 usable CPU보다 많으면 scheduler가 time-slice로 CPU를 공유한다. I/O wait가 많은 workload에서는 oversubscription이 latency를 숨기고 throughput을 높일 수 있지만 CPU-bound workload에서는 context switch와 cache churn이 늘어난다.

Thread pool size를 `CPU 수 × N`으로 정하는 공식은 task의 blocking ratio를 모르면 의미가 없다. Async I/O를 사용하면 필요한 runnable thread 수가 줄어들 수 있다. SMT와 heterogeneous capacity까지 포함하면 단순 count는 더 부정확해진다.

Run queue length, context switch, CPU utilization, task wait time을 함께 본다. 긴 run queue가 steady state라면 thread를 더 만드는 것이 해결책이 아닐 가능성이 높다.

## CHAPTER 21 · CPU isolation은 latency를 위해 shared scheduler activity를 줄이는 선택이다

Realtime 또는 low-latency workload는 일부 CPU를 일반 task와 housekeeping에서 분리해 jitter를 줄이려 할 수 있다. 하지만 timer, IRQ, kernel work가 완전히 사라지는 것은 아니며 설정마다 isolation 범위가 다르다. “isolated CPU”라는 이름만 믿지 말고 실제 activity를 trace한다.

Isolation한 CPU는 다른 workload가 사용할 수 없으므로 평균 utilization이 낮아질 수 있다. 전체 throughput과 tail latency 사이의 명시적 trade-off다.

배포 시 isolated set, IRQ affinity, worker affinity가 서로 겹치지 않는지 자동 검증한다. CPU hotplug나 container cpuset 변경이 정책을 깨뜨리는지도 모니터링한다.

## CHAPTER 22 · cpuset과 CPU quota는 서로 다른 종류의 제한을 만든다

Cpuset은 task가 **어디에서** 실행할 수 있는지 제한하고, CPU quota는 일정 기간 동안 **얼마나 오래** 실행할 수 있는지를 제한한다. 네 개 CPU가 허용돼 있어도 quota가 한 CPU 분량이면 네 CPU를 짧게 사용한 뒤 throttling될 수 있다. 반대로 quota가 충분해도 cpuset이 하나면 parallelism이 제한된다.

Container 안에서 `nproc` 같은 값만 보고 thread pool을 만들면 effective quota를 놓칠 수 있다. Runtime이 cgroup 정보를 읽어 usable capacity를 계산하는 이유다.

관측에서는 throttled time, effective cpuset, scheduler utilization을 같이 본다. CPU가 남아 보이는데 application이 느릴 때 quota throttling을 확인한다.

## CHAPTER 23 · vCPU topology는 guest scheduler가 보는 구조와 host 배치를 연결한다

Hypervisor는 vCPU를 guest에 socket/core/thread topology로 제시할 수 있다. Guest scheduler는 이 정보를 이용해 SMT sibling과 cache sharing을 추정하지만, host가 vCPU를 실제 pCPU에 어떻게 배치하는지가 일치하지 않으면 가정이 틀릴 수 있다.

NUMA-aware VM에서는 guest vNUMA와 host NUMA memory 배치를 맞추는 것이 중요하다. Live migration 후에도 destination host에서 topology와 capacity가 비슷해야 성능 변화가 작다.

VM 성능 incident에서는 guest CPU stats만 보지 말고 steal time, host oversubscription, vCPU→pCPU placement를 함께 조사한다.

## CHAPTER 24 · CPU hotplug는 topology를 runtime 중에 바꿀 수 있다

Power management, maintenance, VM resize로 CPU가 offline/online될 수 있다. Application이 시작 시 읽은 CPU count를 영원히 고정하면 affinity mask가 invalid해지거나 worker 수가 현재 capacity와 맞지 않을 수 있다.

Long-running service는 topology change event에 반응하거나 최소한 periodic refresh를 고려해야 한다. Hotplug 중 per-CPU data와 IRQ affinity도 재배치될 수 있다.

테스트에서는 CPU set 축소 후 pinned worker가 어떻게 되는지 확인한다. 실패를 silently ignore해 일부 thread만 실행 불능이 되는 상태를 피한다.

## CHAPTER 25 · topology discovery는 표준 interface를 통해 계층 관계를 수집해야 한다

CPU model string과 count만으로는 충분하지 않다. Core id, package id, sibling mask, NUMA node, cache sharing 정보를 OS interface에서 읽어 graph로 구성한다. Container에서는 host 전체 topology와 현재 namespace/cpuset에서 사용할 수 있는 부분이 다를 수 있다.

Discovery code는 sparse CPU numbering과 offline CPU를 처리해야 한다. `0..N-1`이 항상 연속 online CPU라는 가정은 위험하다.

수집 결과를 startup log나 metrics metadata로 남기면 incident에서 당시 배치를 재구성할 수 있다. 다만 너무 큰 raw topology를 매 request마다 기록하지 않는다.

## CHAPTER 26 · thermal pressure는 scheduler가 보는 usable capacity를 낮춘다

온도가 올라 power/thermal controller가 frequency를 낮추면 core의 실질 처리 능력이 감소한다. Heterogeneous scheduler는 이런 thermal pressure를 capacity estimation에 반영할 수 있다. Application은 CPU utilization만 보고 “여유가 있다”고 판단하면 실제 처리량 저하를 놓친다.

Mobile·edge device는 특히 workload와 주변 온도에 따라 capacity가 크게 변한다. 짧은 benchmark의 cold result를 장시간 workload에 적용하지 않는다.

Thermal event, frequency, scheduler migration을 같은 timeline에 놓아 본다. 성능 저하가 코드 regression인지 thermal state 변화인지 분리할 수 있다.

## CHAPTER 27 · shared-resource contention은 CPU scheduler가 직접 해결하지 못하는 병목도 포함한다

두 task가 서로 다른 logical CPU에 있어도 LLC, memory bandwidth, interconnect를 공유하면 간섭한다. Scheduler가 CPU time을 공평하게 나눠도 memory-intensive task가 shared bandwidth를 대부분 소비하면 다른 task의 latency가 증가할 수 있다.

이 문제는 CPU quota만으로 격리되지 않는다. Workload placement, memory bandwidth control, cache partitioning 같은 별도 mechanism이 필요할 수 있다. 모든 platform이 같은 control을 제공하는 것은 아니다.

Noisy-neighbor incident에서는 run queue뿐 아니라 LLC miss와 memory bandwidth를 확인한다. CPU time 공정성과 microarchitectural resource 공정성을 구분한다.

## CHAPTER 28 · benchmark pinning은 noise를 줄이지만 production reality를 숨길 수도 있다

Microbenchmark를 한 CPU에 pin하면 migration과 scheduler noise를 줄여 작은 차이를 측정하기 좋다. 그러나 production에서는 task가 여러 CPU를 이동한다면 pinning 결과가 실제 behavior를 대표하지 않을 수 있다. 따라서 controlled microbenchmark와 unconstrained system test를 둘 다 사용한다.

SMT sibling을 비워두었는지, benchmark와 background daemon이 같은 cache domain을 공유하는지도 기록한다. Pin 하나만 설정했다고 환경이 완전히 통제되는 것은 아니다.

결과 artifact에는 CPU affinity mask와 topology snapshot을 포함한다. 동일 binary를 다른 machine에서 재현할 때 필수 정보다.

## CHAPTER 29 · topology observability는 CPU 번호를 의미 있는 계층으로 변환해야 한다

Metrics에 `cpu=17`만 있으면 그 CPU가 어느 socket·core·NUMA node인지 incident 중 다시 조회해야 한다. Topology metadata를 join할 수 있게 해두면 특정 socket의 throttling, 특정 sibling pair의 contention을 빠르게 찾을 수 있다.

Thread migration trace, IRQ distribution, per-node memory, frequency를 같은 CPU identity 체계로 묶는다. Hardware hotplug가 있으면 topology version 또는 timestamp도 필요하다.

Dashboard는 aggregate와 topology별 breakdown을 함께 제공한다. 전체 평균이 정상이어도 한 NUMA node나 한 chiplet만 병목인 상황을 드러낼 수 있다.

## CHAPTER 30 · CPU topology contract는 count가 아니라 placement 가능한 자원 graph다

Scheduler와 application이 알아야 할 것은 “CPU가 몇 개”가 아니라 어떤 logical CPU들이 execution unit, cache, memory node, device path를 공유하는지다. 이 graph 위에서 latency, throughput, power, isolation 목표에 따라 task·IRQ·memory를 배치한다.

운영 설정은 topology discovery에서 생성하고 hard-coded CPU 번호를 최소화한다. SMT, heterogeneous core, quota, hotplug, virtualization 때문에 static count 기반 정책은 쉽게 깨진다.

최종 검증은 placement가 의도한 공유 자원 경계를 실제로 따르는지 trace와 hardware counter로 확인하는 것이다. Topology를 정확히 모델링해야 affinity와 scaling 숫자가 설명 가능한 결과가 된다.
