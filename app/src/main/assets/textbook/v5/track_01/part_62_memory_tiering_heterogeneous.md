# PART 62 · Memory Tiering and Heterogeneous Memory — placement, promotion, demotion, CXL-class capacity

Memory가 모두 같은 latency·bandwidth·failure 특성을 가진다는 가정은 더 이상 안전하지 않다. 한 system 안에 **local DRAM, remote NUMA DRAM, slower capacity tier, device-attached coherent memory**가 공존하면 page placement는 capacity allocation이 아니라 성능 정책이 된다. 핵심은 `어디에 빈 공간이 있는가`가 아니라 **어떤 page를 어느 tier에 두고 언제 이동시킬 것인가**다.

## CHAPTER 01 · Memory tier는 NUMA node와 같은 개념이 아니다

NUMA node는 locality/topology 단위이고 memory tier는 performance characteristic이 비슷한 nodes의 논리적 계층이다. 같은 tier 안에도 여러 NUMA node가 있을 수 있고, 서로 다른 tier가 모두 CPU addressable System RAM으로 보일 수 있다. Placement policy는 node distance와 tier class를 동시에 고려해야 한다.

## CHAPTER 02 · Capacity expansion은 평균 latency보다 tail access cost를 바꾼다

느린 memory tier를 추가하면 OOM을 피하고 더 큰 working set을 유지할 수 있지만 hot page가 느린 tier에 남으면 request tail latency가 커질 수 있다. Capacity가 늘었다는 사실만으로 performance가 개선됐다고 판단할 수 없다. Tier별 resident bytes와 access latency를 함께 측정해야 한다.

## CHAPTER 03 · HMAT/CDAT 같은 topology data는 performance coordinate를 제공한다

Firmware/device topology는 memory node의 latency·bandwidth 특성을 OS에 전달할 수 있다. Kernel은 이 정보를 tiering/demotion decision에 활용할 수 있다. Metadata가 누락되거나 잘못되면 실제 느린 memory가 fast tier로 분류돼 placement policy가 반대로 작동할 수 있다.

## CHAPTER 04 · Initial placement와 runtime migration은 별도 문제다

Allocation 시 어느 node/tier에서 page를 만들지 결정하는 것과, access pattern 변화 후 page를 다른 tier로 옮기는 것은 다른 비용 모델을 가진다. Initial placement는 future hotness를 예측해야 하고 migration은 이미 관측한 hotness를 반영하지만 copy와 TLB update 비용을 지불한다.

## CHAPTER 05 · First-touch policy는 initialization thread가 placement를 결정할 수 있다

Page가 처음 fault되는 CPU의 local node에 memory가 배치되는 정책은 data consumer와 initializer가 같을 때 locality가 좋다. Parallel application이 single-thread initialization 후 worker를 다른 nodes에 배치하면 page ownership이 잘못된 곳에 고정될 수 있다. Initialization strategy도 NUMA/tier placement contract의 일부다.

## CHAPTER 06 · Interleave는 bandwidth를 넓히지만 hot-page locality를 희석한다

Memory를 여러 nodes에 round-robin/interleave하면 aggregate bandwidth를 활용하고 capacity를 균등하게 쓸 수 있다. 그러나 latency-sensitive data도 remote/slower node에 놓일 수 있다. Interleave는 access pattern이 넓고 균일한 workload와 locality-sensitive workload에서 효과가 다르다.

## CHAPTER 07 · Bind policy는 locality를 강제하지만 capacity failure를 명시해야 한다

특정 node set에만 allocation을 허용하면 predictable locality를 얻을 수 있지만 해당 tier가 고갈되었을 때 fallback이 실패하거나 allocation stall이 커질 수 있다. `fast memory에만 둔다`는 policy는 fast tier capacity budget과 admission control 없이는 안정적이지 않다.

## CHAPTER 08 · Preferred policy는 fast path와 fallback을 함께 정의한다

특정 node/tier를 선호하되 부족하면 다른 memory로 fallback하는 방식은 availability를 높인다. 하지만 fallback page가 계속 slow tier에 남으면 성능 degradation이 영구화될 수 있다. Runtime promotion이 함께 있어야 preferred policy가 burst 이후 recovery할 수 있다.

## CHAPTER 09 · Demotion은 cold page를 느린 tier로 내려 fast capacity를 확보한다

Memory pressure에서 cold page를 swap으로 내보내기 전에 slower memory node로 migration할 수 있다. 이는 storage I/O보다 낮은 cost로 fast tier 공간을 만들 수 있지만 demoted page는 여전히 RAM을 소비한다. Slow tier까지 포화되면 다음 reclaim step이 필요하다.

## CHAPTER 10 · Promotion은 access hotness를 근거로 page를 빠른 tier로 올린다

Slow tier page가 자주 access되면 fast tier로 이동시키는 것이 유리할 수 있다. 그러나 promotion 자체가 memory copy와 bandwidth를 소비하고 fast tier에서 다른 page를 밀어낼 수 있다. Hotness threshold와 rate limit이 필요한 이유다.

## CHAPTER 11 · Promotion/demotion loop는 migration thrashing을 만들 수 있다

Access pattern이 threshold 근처에서 흔들리면 같은 page가 tier 사이를 반복 이동할 수 있다. Migration bytes가 useful application traffic보다 커지면 tiering이 오히려 bandwidth를 소비한다. Hysteresis, cooldown, minimum residency 같은 stabilizing policy가 필요하다.

## CHAPTER 12 · Page migration은 data copy뿐 아니라 mapping update 작업이다

Physical page를 옮기려면 새로운 page allocation, content copy, page-table mapping 변경, reverse mapping, TLB invalidation, pin/reference 상태 처리가 필요하다. Large hot page migration은 memory bandwidth와 shootdown latency를 유발할 수 있다. Migration cost를 단순 bytes/bandwidth로만 계산하면 안 된다.

## CHAPTER 13 · Pinned page는 tier migration이 제한될 수 있다

DMA, long-term pin, device mapping, certain kernel users가 page를 이동 불가능하게 만들 수 있다. Fast tier에 cold pinned page가 많이 쌓이면 hot movable page를 위한 capacity가 줄어든다. Tiering design은 pinned-memory budget을 별도 resource로 추적해야 한다.

## CHAPTER 14 · Huge page는 migration granularity를 키운다

Huge page를 통째로 빠른 tier로 올리면 일부 subpage만 hot해도 큰 copy 비용을 지불한다. Split 후 작은 page 단위로 이동하면 policy precision은 높아지지만 TLB/metadata 이득을 잃는다. Page size와 tiering granularity는 독립 tuning이 아니다.

## CHAPTER 15 · Auto NUMA balancing은 sampled fault를 placement signal로 사용한다

Kernel은 일부 mapping을 sampling 목적에 따라 fault가 나도록 만들어 어떤 CPU가 page를 access하는지 관찰할 수 있다. 이 fault는 application bug가 아니라 placement telemetry다. Sampling 빈도가 너무 높으면 fault overhead가 커지고 너무 낮으면 phase change 대응이 느리다.

## CHAPTER 16 · Memory-tiering balancing은 locality와 tier hotness를 함께 판단한다

같은 NUMA balancing machinery를 사용해 hot page를 fast memory type으로 promotion할 수 있다. 이때 CPU-local node가 항상 가장 빠른 tier라는 가정이 성립하지 않을 수 있다. CPU distance와 memory-class performance를 별도 coordinate로 봐야 한다.

## CHAPTER 17 · Promotion rate limit은 fast-tier bandwidth를 보호한다

Large working set phase change에서 수 GB의 page를 즉시 promotion하면 application memory traffic과 migration copy가 bandwidth를 경쟁한다. Rate limit은 recovery 속도를 늦추는 대신 foreground latency를 보호한다. 적정 값은 fast/slow tier bandwidth와 application headroom을 근거로 정해야 한다.

## CHAPTER 18 · Slow tier write bandwidth는 dirty-data placement에 중요하다

Read-mostly cold data는 느린 tier에서도 acceptable할 수 있지만 write-intensive page는 slow tier bandwidth와 endurance 특성에 큰 영향을 받을 수 있다. Tiering policy가 access count만 보고 read/write type을 무시하면 write-heavy data를 잘못 배치할 수 있다.

## CHAPTER 19 · Memory tier는 swap의 완전한 대체가 아니다

Slower RAM tier도 finite capacity를 가지며 reclaim pressure에서 결국 swap/storage 또는 process termination이 필요할 수 있다. Demotion을 enable했다고 OOM이 사라지는 것이 아니다. Reclaim chain에서 `fast→slow RAM→compressed/swap→OOM` 순서와 각 threshold를 명확히 해야 한다.

## CHAPTER 20 · Zswap과 demotion을 같이 쓰면 cold data path가 중복될 수 있다

Cold page를 slow tier로 내리고 다시 compression/swap하는 policy가 비효율적 순환을 만들 수 있다. Compressed page backing을 어느 node에 둘지도 중요하다. Tier policy와 swap/compression policy를 독립적으로 켜면 data가 불필요하게 promotion/demotion될 수 있다.

## CHAPTER 21 · Memcg/cpuset isolation과 demotion target이 충돌할 수 있다

Container가 허용된 memory nodes를 제한했더라도 demotion mechanism이 shared page나 policy 예외 때문에 다른 node를 사용할 수 있는지 확인해야 한다. Resource isolation은 logical limit뿐 아니라 physical placement까지 포함한다. Multi-tenant system에서 cross-tier placement는 data locality와 accounting 이슈가 된다.

## CHAPTER 22 · CXL-class memory는 system physical address space에 mapping될 수 있다

Coherent device-attached memory는 host physical address range에 region/decoder를 통해 노출될 수 있다. CPU가 ordinary load/store semantics로 접근 가능하더라도 latency, bandwidth, failure management는 local DRAM과 다를 수 있다. `System RAM으로 보인다`와 `DRAM과 동일하다`는 전혀 다른 주장이다.

## CHAPTER 23 · Decoder/interleave topology는 address가 실제 device로 가는 경로를 결정한다

Host bridge와 device decoder가 physical address range를 endpoint/device address로 변환하고 interleave할 수 있다. Region configuration과 target ordering이 잘못되면 capacity가 online되지 않거나 expected bandwidth distribution을 얻지 못한다. Memory topology는 software NUMA node보다 아래 hardware decode layer까지 이어진다.

## CHAPTER 24 · Poison은 page error를 capacity-tier failure로 전파한다

Device memory에서 uncorrectable media error가 특정 address range에 poison으로 기록될 수 있다. OS는 affected page/process를 isolate하거나 recovery해야 한다. Slow tier의 capacity 이득만 보고 poison/event handling을 무시하면 memory expansion device 하나의 fault가 process crash 또는 data corruption으로 전파될 수 있다.

## CHAPTER 25 · Hotplug/offline은 page migration과 allocation quiescence를 요구한다

Memory device/tier를 remove하려면 그 node의 movable pages를 다른 곳으로 옮기고 새 allocation을 막아야 한다. Pinned/unmovable page가 남으면 offline이 실패할 수 있다. Hardware serviceability는 page lifetime과 resource ownership protocol에 의존한다.

## CHAPTER 26 · Bandwidth saturation은 tier latency ranking을 동적으로 바꾼다

Idle 상태에서 fast tier가 low latency여도 traffic이 saturation되면 queueing delay가 커져 remote/slow tier와 차이가 달라질 수 있다. Static benchmark latency 하나로 placement policy를 고정하면 load-dependent reversal을 놓친다. Tier별 latency는 bandwidth utilization과 함께 측정해야 한다.

## CHAPTER 27 · Tier-aware allocator는 object semantic importance를 자동으로 알지 못한다

Kernel은 page access pattern과 policy hint는 볼 수 있어도 어떤 object가 user-visible critical path인지 완전히 알 수 없다. Application/runtime이 hot/cold allocation pool, memory policy, arena separation으로 semantic hint를 제공하면 placement precision을 높일 수 있다. 그러나 hint가 stale하면 policy를 악화시킨다.

## CHAPTER 28 · Observability는 bytes뿐 아니라 migration cause를 기록해야 한다

Fast/slow tier resident bytes, promotion/demotion bytes, fault sampling, refault, migration failure, pinned pages, tier bandwidth, latency를 함께 기록해야 한다. `slow tier 사용량 증가`만으로 문제를 판단할 수 없다. Capacity 활용인지 hot-page misplacement인지 원인이 다르다.

## CHAPTER 29 · Tier benchmark는 steady-state와 phase-change를 모두 시험해야 한다

Warm steady workload에서는 placement가 이미 최적화돼 좋은 결과가 나오지만 production은 deploy, query shift, model load처럼 working-set phase change를 겪는다. Benchmark에는 cold start, sudden hot-set shift, fast-tier pressure, migration recovery time을 포함해야 한다.

## CHAPTER 30 · Memory tiering은 capacity, latency, migration, failure를 함께 제어한다

Heterogeneous memory를 제대로 쓰려면 **initial placement, NUMA locality, tier rank, promotion/demotion, migration bandwidth, pinned pages, reclaim/swap interaction, cgroup isolation, poison/hotplug, observability**를 하나의 policy로 묶어야 한다. 최종 목표는 fast memory 사용률이 아니라 critical working set이 적절한 tier에 안정적으로 머무는 것이다.
