# PART 48 · CPU Topology and SMT — sockets, cores, siblings, heterogeneous capacity

`CPU 16개`라는 숫자는 실행 자원의 구조를 설명하지 못한다. Logical CPU는 같은 physical core의 SMT sibling일 수 있고, 여러 core가 LLC·memory controller를 공유하며, multi-socket/NUMA machine은 remote memory latency가 다르다. Mobile/modern SoC는 성능·전력 특성이 다른 core cluster를 함께 사용한다. Scheduler와 application affinity는 이 topology 위에서 cache locality, throughput, latency, energy, I/O locality를 동시에 결정한다.

## CHAPTER 01 · logical CPU 수와 physical core 수를 분리한다

OS scheduler가 task를 배치하는 CPU ID는 hardware thread/logical processor 단위일 수 있다. SMT가 켜진 8-core CPU가 16 logical CPU를 보일 수 있지만 두 sibling은 execution resource를 완전히 독립적으로 갖지 않는다. Capacity planning에서 logical CPU 수를 physical independent core 수처럼 곱하면 peak throughput을 과대평가할 수 있다. Topology API에서 package/core/thread sibling relation을 읽고 benchmark result에 SMT configuration을 기록한다.

## CHAPTER 02 · SMT는 한 core의 pipeline 빈틈을 다른 thread가 채우게 한다

한 thread가 cache miss나 dependency로 execution unit을 충분히 쓰지 못할 때 sibling thread의 instruction을 issue해 core utilization을 높일 수 있다. Memory-latency-heavy workload에서는 throughput 이득이 클 수 있지만 이미 execution port를 꽉 쓰는 vector/compute workload는 sibling이 resource를 경쟁해 각 thread latency가 나빠질 수 있다. SMT 성능은 `두 thread면 2배`가 아니라 stall pattern과 shared resource utilization에 의존한다.

## CHAPTER 03 · SMT sibling은 frontend·execution port·cache 일부를 공유한다

Microarchitecture에 따라 fetch/decode bandwidth, reorder/resource entries, execution unit, L1/L2 cache가 sibling 사이 공유되거나 partition된다. 한 sibling의 branch-heavy workload가 frontend를, 다른 sibling의 vector workload가 execution port를 경쟁할 수 있다. CPU utilization 200%처럼 보여도 physical core resource는 이미 포화일 수 있다. Per-thread IPC와 core-level PMU resource pressure를 함께 보면 sibling contention을 분리할 수 있다.

## CHAPTER 04 · sibling 배치는 throughput과 tail latency 사이 정책 문제다

Batch workload는 한 core의 두 sibling을 모두 채워 aggregate throughput을 높이는 것이 유리할 수 있다. Latency-critical service는 sibling에 noisy workload가 들어오면 p99가 흔들려 physical core 하나당 한 critical thread만 배치하는 정책이 나을 수 있다. CPU isolation은 idle resource를 남기는 비용이 있다. Service class별 latency SLO와 utilization target을 사용해 SMT sharing 여부를 결정한다.

## CHAPTER 05 · private cache와 shared LLC의 경계를 topology에 맞춰 읽는다

L1/L2가 core-private이고 LLC가 여러 core에 공유되는 설계에서는 thread migration 범위에 따라 cache reuse cost가 다르다. 같은 core sibling으로 이동하는 것과 다른 core, 다른 LLC cluster로 이동하는 것은 working-set coldness가 다를 수 있다. Scheduler가 cache topology를 고려하는 이유다. Affinity를 설정할 때 CPU ID가 연속이라는 이유로 같은 cache domain이라고 가정하지 않는다.

## CHAPTER 06 · cache slice와 interconnect는 하나의 LLC도 물리적으로 분산시킬 수 있다

Large last-level cache가 여러 slice/bank로 나뉘고 address hash로 home slice가 결정되면 core에서 slice까지 interconnect hop이 발생한다. Capacity는 공유되지만 access latency와 bandwidth contention이 uniform하지 않을 수 있다. Multi-die/chiplet CPU에서는 LLC와 memory path가 더 복잡해진다. `L3 hit` 하나의 counter만으로 topology latency를 모두 설명하지 않고 interconnect/uncore metric을 함께 본다.

## CHAPTER 07 · package·die·chiplet 경계는 core 사이 communication cost를 바꾼다

두 core가 같은 silicon die에 있는지 다른 compute die/chiplet에 있는지에 따라 cache-coherence message와 memory access 경로가 달라질 수 있다. Lock-heavy shared state가 die 경계를 자주 오가면 cache-line transfer latency가 증가할 수 있다. Worker sharding을 topology cluster와 맞추면 cross-die coherence를 줄일 수 있다. 그러나 workload imbalance가 심하면 locality보다 load balance가 중요해질 수 있어 adaptive policy가 필요하다.

## CHAPTER 08 · multi-socket NUMA에서는 CPU placement와 memory placement를 함께 결정한다

Thread를 socket 0에 pinning했는데 working set page가 socket 1 memory에 있으면 remote interconnect를 통해 접근한다. CPU affinity만 고정하고 memory policy를 무시하면 예상보다 낮은 bandwidth와 높은 latency가 발생한다. First-touch allocation, mbind/NUMA policy, allocator arena가 placement에 영향을 준다. P14 NUMA와 scheduler affinity를 하나의 experiment로 측정한다.

## CHAPTER 09 · first-touch는 초기화 thread가 장기 memory locality를 결정할 수 있다

Large array를 한 initialization thread가 모두 zero/fill하면 page가 그 thread의 NUMA node에 배치될 수 있다. 이후 여러 socket worker가 분할 처리하면 절반 이상의 access가 remote가 될 수 있다. Parallel first-touch로 각 worker가 자기 partition page를 초기화하면 locality를 개선할 수 있다. Benchmark setup의 initialization phase가 production allocation pattern과 다르면 NUMA 결과가 왜곡된다.

## CHAPTER 10 · automatic NUMA balancing은 page와 task를 움직여 locality를 개선하려 한다

Kernel은 access pattern을 관찰해 task 또는 page를 더 적합한 node로 migration할 수 있다. Migration은 copy/translation invalidation 비용이 있고 workload phase가 짧으면 이득 전에 phase가 끝날 수 있다. Manual pinning과 automatic balancing을 동시에 사용하면 policy가 충돌할 수 있다. Long-running service는 migration count와 remote access rate를 관찰해 static/automatic policy를 선택한다.

## CHAPTER 11 · heterogeneous core는 logical CPU마다 동일 capacity라는 가정을 깨뜨린다

Mobile/energy-efficient system은 높은 peak performance core와 낮은 power core를 섞을 수 있다. 같은 frequency 숫자라도 microarchitecture가 달라 instruction throughput이 다를 수 있다. Scheduler는 CPU capacity와 utilization estimate를 사용해 task를 적합한 core로 배치할 수 있다. `CPU 사용률 50%`만으로 headroom을 계산하지 않고 어느 capacity class에서 실행되는지 확인한다.

## CHAPTER 12 · frequency와 core capacity는 서로 다른 변수다

한 big core가 1.5GHz로 throttled된 상태와 little core가 1.5GHz로 실행되는 상태의 처리량은 같지 않을 수 있다. IPC, cache, execution width가 다르기 때문이다. DVFS는 같은 core 안에서 frequency를 바꾸고 heterogeneous topology는 core architecture 차이를 추가한다. Scheduler/benchmark는 cycles, instructions, frequency, CPU ID/capacity를 함께 기록한다.

## CHAPTER 13 · util clamp는 task placement와 DVFS request에 동시에 영향을 줄 수 있다

Linux utilization clamping은 task/group의 effective utilization lower/upper bound를 지정해 frequency selection과 capacity-fit 판단에 영향을 줄 수 있다. Latency-sensitive task에 minimum utilization hint를 주면 faster core/frequency를 얻을 수 있지만 energy cost와 thermal pressure가 증가한다. Uclamp를 magic priority로 사용하지 않고 workload service time과 SLO를 근거로 조정한다.

## CHAPTER 14 · task migration은 load balance를 얻는 대신 cache/TLB locality를 잃는다

Busy CPU에서 idle CPU로 runnable task를 이동하면 queue delay를 줄일 수 있지만 private cache working set과 branch predictor state가 cold해질 수 있다. Short task는 migration cost가 service time보다 클 수 있고 long task는 balance 이득이 더 크다. Scheduler는 wake-affine, cache topology, load metric을 이용해 trade-off를 조정한다. Application이 과도하게 pinning하면 scheduler의 balance 기회를 없앤다.

## CHAPTER 15 · affinity mask는 허용 CPU 집합이지 항상 즉시 고정되는 성능 보장이 아니다

Thread affinity를 하나의 CPU로 제한하면 migration을 막을 수 있지만 그 CPU가 interrupt, other task, thermal throttle을 받으면 escape할 수 없다. 여러 CPU mask를 주면 scheduler가 그 범위에서 이동할 수 있다. CPU hotplug나 cpuset/cgroup policy가 affinity와 교차할 수 있다. Affinity 설정 성공만 확인하지 말고 실제 run CPU distribution을 trace한다.

## CHAPTER 16 · interrupt affinity는 application CPU locality에도 영향을 준다

NIC/storage completion interrupt가 특정 CPU에서 처리되고 application thread가 다른 NUMA node에서 실행되면 packet/buffer cache line이 node 사이를 이동할 수 있다. IRQ affinity와 receive-side scaling queue mapping을 worker placement와 맞추면 locality를 높일 수 있다. 반대로 모든 interrupt를 latency-critical core에 몰면 application execution을 방해한다. Device queue, IRQ, worker CPU를 하나의 topology map에 놓는다.

## CHAPTER 17 · PCIe device locality는 root complex와 NUMA node까지 포함한다

Multi-socket server에서 NIC/GPU/NVMe가 특정 socket의 PCIe root에 연결되면 다른 socket CPU가 device DMA buffer를 처리할 때 inter-socket traffic이 발생할 수 있다. `가장 idle한 CPU`가 I/O locality에 최적인 CPU가 아닐 수 있다. VFIO/passthrough VM도 vCPU·guest memory·device를 같은 NUMA node에 정렬하면 remote DMA/memory cost를 줄일 수 있다.

## CHAPTER 18 · memory controller/channel bandwidth는 core 수보다 먼저 포화될 수 있다

Core를 두 배로 늘려도 DRAM channel bandwidth가 이미 최대라면 memory-bound workload throughput은 거의 늘지 않는다. 추가 core는 queueing과 interference만 증가시킬 수 있다. P31 memory controller counter와 per-socket bandwidth를 보고 scaling curve를 그린다. CPU utilization이 높다는 이유로 더 많은 worker를 추가하지 않는다.

## CHAPTER 19 · SMT를 끄면 logical CPU는 줄지만 per-thread resource가 늘어날 수 있다

HPC/vector/latency workload에서 SMT off가 더 높은 deterministic performance를 제공할 수 있지만 I/O-bound/mixed workload는 aggregate throughput을 잃을 수 있다. BIOS/kernel global setting을 바꾸는 대신 application affinity로 한 sibling만 사용하는 실험도 가능하다. 동일 power/thermal condition에서 throughput, p99, energy per request를 비교한다.

## CHAPTER 20 · oversubscription은 runnable thread가 logical CPU보다 많다는 사실보다 workload 특성이 중요하다

Blocking I/O가 많은 thread는 sleep 시간이 길어 oversubscription이 CPU throughput에 큰 문제가 아닐 수 있다. CPU-bound thread를 수백 개 만들면 context switch와 cache thrash가 증가한다. Executor size를 logical CPU count 그대로 정하기보다 blocking ratio, SMT effectiveness, heterogeneous capacity를 반영한다. Async runtime P41의 worker pool과 연결한다.

## CHAPTER 21 · isolated CPU는 housekeeping work까지 분리해야 의미가 있다

Latency-sensitive thread를 한 CPU에 pin해도 timer tick, RCU callback, kernel worker, interrupt가 같은 CPU에서 실행되면 jitter가 남는다. Kernel isolation/nohz/rcu/irq policy를 함께 구성해야 더 강한 isolation이 가능하다. Isolation은 system throughput과 operational complexity를 희생하므로 필요한 SLO에만 적용한다. `isolcpus`류 설정은 kernel version별 guidance를 확인한다.

## CHAPTER 22 · cgroup/cpuset은 process affinity보다 상위 resource boundary를 만들 수 있다

Container/orchestrator가 cpuset으로 workload CPU 범위를 제한하면 application이 더 넓은 affinity를 요청해도 effective set은 상위 policy와 교집합이 된다. CPU quota가 있으면 cpuset 안에 idle core가 보여도 period budget 소진으로 throttling될 수 있다. Container 안에서 host CPU count만 읽어 worker 수를 정하면 quota를 초과할 수 있다. Effective cpuset과 quota를 함께 읽는다.

## CHAPTER 23 · virtualization에서는 vCPU topology가 host physical topology와 매핑된다

Guest가 sockets/cores/threads topology를 보더라도 host scheduler가 vCPU를 arbitrary physical CPU에 배치하면 guest의 cache/NUMA assumption이 깨질 수 있다. vNUMA memory와 host NUMA backing, vCPU pinning을 일치시키지 않으면 remote access가 증가한다. SMT sibling을 vCPU topology에 어떻게 노출할지도 licensing/performance와 연결된다. P20 KVM과 topology 정보를 함께 검증한다.

## CHAPTER 24 · CPU hotplug는 topology와 affinity가 runtime에 변할 수 있음을 뜻한다

Power management, VM orchestration, hardware operation으로 CPU가 online/offline되면 fixed CPU ID set assumption이 깨질 수 있다. Thread가 offline CPU에만 affinity되어 있으면 scheduler가 policy를 조정하거나 execution이 막힐 수 있다. Runtime worker pool이 online CPU count 변화를 지원하는지 확인한다. Long-lived service는 startup topology snapshot을 영구 truth로 사용하지 않는다.

## CHAPTER 25 · topology discovery는 sysfs/proc/runtime API의 의미를 이해해야 한다

OS는 core_id, physical_package_id, thread_siblings, cache shared_cpu_list, NUMA node 정보를 제공할 수 있다. Container namespace/cpuset 안에서는 host 전체 topology와 usable topology가 다를 수 있다. Library가 `hardware_concurrency` 숫자만 제공한다면 detailed placement에는 부족하다. Benchmark tool은 CPU model과 topology를 artifact metadata로 저장한다.

## CHAPTER 26 · thermal throttling은 특정 cluster/core의 effective capacity를 시간에 따라 바꾼다

Sustained workload에서 high-performance core가 열 한계에 도달하면 frequency/capacity가 낮아지고 scheduler가 다른 cluster로 task를 옮길 수 있다. 초기 10초 benchmark와 10분 production load의 CPU topology utilization이 다를 수 있다. Per-core frequency, thermal zone, migration trace를 수집해 steady-state capacity를 측정한다. P21 power/thermal model과 연결한다.

## CHAPTER 27 · noisy neighbor는 같은 core뿐 아니라 LLC·memory controller·interconnect를 공유한다

서로 다른 process/container가 CPU core를 분리해서 사용해도 shared LLC와 DRAM bandwidth를 경쟁할 수 있다. Core-level utilization만 isolation된다고 성능 isolation이 보장되지 않는다. Cache allocation/memory bandwidth control 같은 hardware/QoS 기능이 있는 플랫폼도 있지만 portability가 제한된다. Tenant SLO는 shared resource pressure를 포함해 측정한다.

## CHAPTER 28 · topology-aware benchmark는 pinning과 memory placement를 명시한다

Run마다 scheduler가 다른 core/socket을 선택하면 variance가 커져 code change 효과를 구분하기 어렵다. Microbenchmark는 CPU affinity, sibling usage, NUMA memory policy, frequency state를 통제하고 production-like benchmark는 오히려 natural scheduler behavior를 포함한다. 두 종류 결과를 혼합하지 않는다. Pinning 자체가 production policy와 다른 경우 결론을 제한해서 표현한다.

## CHAPTER 29 · production observability는 CPU ID·migration·runqueue·frequency를 request latency와 연결한다

p99 request가 느린 시점에 어떤 CPU에서 실행됐는지, 직전 migration이 있었는지, sibling/IRQ contention, runqueue delay, frequency/thermal state가 어땠는지 추적하면 topology 문제를 code issue와 구분할 수 있다. 모든 request에 고비용 trace를 붙이지 않고 sampling/triggered trace를 사용한다. Topology metadata가 없는 CPU profile은 heterogeneous system에서 해석력이 떨어진다.

## CHAPTER 30 · topology 계약은 logical count보다 공유 resource와 placement domain을 기록한다

Capacity model은 package/die/core/SMT sibling, cache sharing, NUMA node, memory controller, PCIe locality, heterogeneous capacity를 포함한다. Worker 수와 affinity는 CPU-bound/blocking ratio와 SLO에 맞춰 결정하고 cgroup/quota/thermal policy를 반영한다. Benchmark는 SMT·NUMA·frequency 조건을 기록한다. 성능 최적화의 단위는 `CPU 개수`가 아니라 실제로 독립·공유되는 hardware resource와 runtime placement다.