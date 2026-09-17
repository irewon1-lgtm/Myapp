# PART 74 · Memory-Level Parallelism and Prefetch — load/store queues, outstanding misses, forwarding, speculation

Memory latency는 한 번의 cache miss가 몇 ns인지로만 결정되지 않는다. Out-of-order core는 **여러 독립 load miss를 동시에 진행하고, store와 load dependency를 추측하며, hardware/software prefetch로 future lines를 미리 요청**해 latency를 겹칠 수 있다. 같은 DRAM latency라도 outstanding miss capacity와 dependency graph에 따라 program throughput이 크게 달라진다. 핵심은 `cache hit rate`가 아니라 **얼마나 많은 유용한 memory work를 동시에 flight에 둘 수 있는가**다.

## CHAPTER 01 · Memory-level parallelism은 여러 cache miss가 동시에 진행되는 정도다

두 loads가 서로 독립이고 cache에서 모두 miss하면 CPU는 첫 miss가 memory에서 돌아올 때까지 반드시 멈출 필요가 없다. Load/store queue, fill buffers/MSHR류 miss-tracking resources, memory controller가 여러 outstanding requests를 지원하면 latency를 겹칠 수 있다. 반대로 pointer-chasing처럼 두 번째 address가 첫 load result에 의존하면 parallelism이 1에 가깝다. 따라서 bandwidth가 충분해도 dependency chain이 긴 workload는 latency bound가 된다.

## CHAPTER 02 · Out-of-order execution은 address/data dependency가 허용하는 범위에서 memory wait를 숨긴다

Scheduler는 older instruction이 cache miss로 기다리는 동안 dependency가 없는 younger work를 실행할 수 있다. 그러나 reorder buffer, load/store queue, physical register와 execution window가 가득 차면 더 이상 future instructions를 끌어올 수 없다. `CPU가 cache miss를 숨긴다`는 능력에는 finite instruction-window limit이 있다. Large dependency distance를 만들 수 없는 code는 높은 MLP에 도달하지 못한다.

## CHAPTER 03 · Load queue는 in-flight loads의 ordering과 replay state를 보존한다

Issued load는 retirement 전까지 exception, memory-order violation, forwarding result 등을 추적해야 한다. Load queue entry가 부족하면 frontend/rename 단계가 새로운 memory operation을 받아들이지 못할 수 있다. Cache bandwidth가 남아 있어도 LSQ capacity가 bottleneck이 될 수 있으므로 load-buffer-full stall과 cache miss stall을 분리해야 한다.

## CHAPTER 04 · Store queue는 store address와 data가 준비되는 시간 차이를 흡수한다

Store instruction은 address 계산과 data 생산이 서로 다른 dependency를 가질 수 있고 architectural memory update는 retirement/order 규칙을 따라야 한다. Store buffer/queue는 pending stores를 보관해 CPU가 바로 다음 instructions를 진행하게 한다. 그러나 buffer가 가득 차면 retirement와 new issue가 막힌다. Write-heavy loop에서 store queue saturation은 DRAM write bandwidth와 다른 core-local bottleneck이다.

## CHAPTER 05 · Memory disambiguation은 younger load가 older store보다 먼저 갈 수 있는지 추측한다

Older store address가 아직 계산되지 않았을 때 conservative processor는 모든 younger loads를 기다릴 수 있지만 성능 손실이 크다. Modern core는 alias 가능성을 예측해 independent load를 먼저 실행할 수 있다. 나중에 실제 same-address conflict가 밝혀지면 load와 dependent instructions를 replay한다. 이 mechanism은 speculation을 통해 memory parallelism을 얻는 대신 mis-speculation penalty를 가진다.

## CHAPTER 06 · 4K alias 같은 partial-address ambiguity는 false dependency를 만들 수 있다

Full physical address가 아직 준비되지 않은 단계에서는 page offset 일부만 비교해 load/store alias를 추정할 수 있다. 서로 다른 pages인데 low address bits가 같으면 processor가 temporary conflict로 보고 load를 지연/replay할 수 있다. `두 arrays가 논리적으로 독립`이어도 virtual-address layout 때문에 memory-ordering machinery가 false dependency를 만들 수 있다. Alignment/padding 변화가 예상 밖 performance swing을 만드는 원인 중 하나다.

## CHAPTER 07 · Store-to-load forwarding은 cache round trip 없이 최근 store 값을 전달한다

Younger load가 같은 address range를 읽고 older store data가 buffer에 있으면 store queue에서 직접 forwarding할 수 있다. 이 path는 L1 cache를 다시 거치지 않아 latency를 줄인다. 하지만 size/alignment가 맞지 않거나 load가 여러 stores를 애매하게 합쳐야 하면 forwarding이 실패하고 pipeline replay가 발생할 수 있다. Source-level assignment가 같아 보여도 access width/layout이 중요하다.

## CHAPTER 08 · Partial overlap은 forwarding failure와 replay를 만든다

Store가 4 bytes를 쓰고 load가 그 경계를 걸쳐 8 bytes를 읽는 식의 overlap은 processor가 store buffer와 cache에서 값을 합쳐야 한다. Architecture/microarchitecture에 따라 slow path나 stall이 발생할 수 있다. Packed structs, unaligned fields, byte-wise serialization code가 예상보다 느린 이유는 cache miss가 아니라 forwarding geometry일 수 있다. PMU의 store-forwarding stall event가 있다면 확인해야 한다.

## CHAPTER 09 · Line-fill buffer/MSHR류 구조는 outstanding cache miss마다 state를 소비한다

L1 miss가 발생하면 lower cache/memory에서 해당 cache line을 가져오는 동안 target line, waiting loads, coherence transaction state를 추적할 entry가 필요하다. 동일 line에 여러 loads가 miss하면 하나의 in-flight fill에 merge할 수 있지만 서로 다른 lines가 많으면 finite entries를 소진한다. Entry가 다 차면 new miss를 launch하지 못해 MLP가 architecture limit보다 먼저 막힌다.

## CHAPTER 10 · 동일 cache line miss merge는 request 수를 줄이지만 dependency latency는 남는다

여러 loads가 아직 도착하지 않은 같은 line을 요구하면 추가 memory request를 만들지 않고 existing fill에 기다릴 수 있다. Bus traffic은 줄지만 모든 consumers가 동일 fill completion을 기다린다. Hot structure의 fields가 한 line에 모여 있는 것은 spatial locality에는 좋지만 그 line 하나가 miss하면 dependent chain 전체가 동시에 멈출 수 있다. Locality와 independent parallelism은 때때로 trade-off다.

## CHAPTER 11 · Independent streams는 MLP를 늘리지만 memory bandwidth를 더 빨리 포화시킨다

여러 arrays를 동시에 순회하면 각 stream의 misses가 독립이라 latency를 겹칠 수 있다. 그러나 outstanding demand가 늘수록 DRAM channels와 interconnect bandwidth를 더 빠르게 사용한다. Single-thread latency가 좋아져도 multi-core total bandwidth headroom이 줄 수 있다. Per-core optimization은 socket-wide contention까지 측정해야 한다.

## CHAPTER 12 · Pointer chasing은 address dependency 때문에 hardware가 future misses를 쉽게 겹치지 못한다

Linked list의 next pointer를 읽어야 다음 node address를 알 수 있으면 next memory request를 현재 miss completion 전 시작하기 어렵다. Large cache miss latency가 거의 그대로 critical path에 들어온다. Data layout을 array/index 기반으로 바꾸거나 multiple independent chains를 interleave하면 MLP를 늘릴 수 있다. Algorithmic data dependency가 microarchitecture보다 먼저 parallelism 상한을 결정한다.

## CHAPTER 13 · Software prefetch는 address가 일찍 계산 가능할 때 dependency gap을 인위적으로 만든다

Future iteration address를 미리 알 수 있다면 explicit prefetch hint를 issue해 실제 load보다 먼저 memory request를 시작할 수 있다. Prefetch distance가 memory latency를 숨길 만큼 멀어야 하지만 너무 멀면 cache에서 evict되거나 useless bandwidth를 소비한다. Loop trip count, cache capacity, iteration compute time에 따라 적정 distance가 달라진다.

## CHAPTER 14 · Prefetch instruction은 보통 correctness-affecting load가 아니라 performance hint다

Arm `PRFM`처럼 architecture가 prefetch를 hint로 정의하면 implementation은 요청을 무시하거나 다른 cache level에 처리할 수 있다. Prefetch target이 invalid/unmapped edge에 있을 때 fault semantics도 ordinary load와 다를 수 있다. Correctness가 prefetch completion에 의존해서는 안 된다. Prefetch를 제거해도 program result가 같아야 한다.

## CHAPTER 15 · Hardware stream/stride prefetcher는 regular miss pattern에서 future lines를 예측한다

Sequential 또는 fixed-stride access가 반복되면 hardware가 future cache lines를 demand 전에 요청할 수 있다. 따라서 benchmark에서 sequential array scan이 DRAM latency보다 훨씬 빠르게 보일 수 있다. Randomizing access order로 prefetch를 끄면 latency가 급증할 수 있으므로 `memory latency benchmark`가 어떤 prefetch behavior를 포함하는지 명시해야 한다.

## CHAPTER 16 · Prefetch accuracy와 coverage는 서로 다른 품질 지표다

Accuracy는 가져온 prefetched lines 중 실제 demand가 사용한 비율이고 coverage는 demand misses 중 prefetch가 미리 막아준 비율이다. 매우 보수적 prefetcher는 accuracy가 높아도 coverage가 낮고, 공격적 prefetcher는 coverage는 높지만 pollution/bandwidth waste가 클 수 있다. Hit-rate 하나로 prefetch quality를 평가하면 policy trade-off를 볼 수 없다.

## CHAPTER 17 · Useless prefetch는 cache pollution과 eviction을 만든다

미래에 사용하지 않을 line이 cache를 차지하면 application hot line을 밀어내 demand miss를 증가시킬 수 있다. 특히 LLC가 여러 cores 사이 shared일 때 한 core의 aggressive prefetch가 다른 workloads의 cache capacity를 소모한다. Prefetch optimization은 local thread hit rate뿐 아니라 socket-wide eviction와 memory traffic을 확인해야 한다.

## CHAPTER 18 · Prefetch는 DRAM bandwidth와 memory-controller queue도 소비한다

Demand와 prefetch request가 같은 lower-level bandwidth를 경쟁하면 bandwidth-bound workload에서는 prefetch가 useful demand의 service time을 오히려 늦출 수 있다. Controller가 prefetch priority를 낮추더라도 queue slots와 row locality에 영향이 있을 수 있다. `cache miss 감소`와 `total memory traffic 증가`를 같이 측정해야 한다.

## CHAPTER 19 · Page boundary와 TLB miss는 prefetch stream의 다음 병목이 된다

Data cache lines을 미리 가져와도 target virtual pages의 TLB translation이 준비되지 않으면 demand load가 page walk를 기다릴 수 있다. Large datasets에서는 DTLB miss와 page-walk MLP를 함께 고려해야 한다. Huge pages는 TLB reach를 늘리지만 fragmentation/migration trade-off를 가진다. Cache prefetch와 address-translation prefetch는 다른 hierarchy다.

## CHAPTER 20 · Page walk 자체도 memory-level parallelism을 소비한다

TLB miss의 page-table walk는 여러 levels의 PTE memory accesses를 발생시키며 page-walk caches가 miss하면 lower cache/DRAM traffic을 추가한다. 여러 TLB misses가 동시에 발생하면 page-walk engine capacity와 fill buffers를 경쟁할 수 있다. Random sparse access의 비용은 data miss + translation miss가 합성된 결과다.

## CHAPTER 21 · Memory dependence predictor가 틀리면 speculative load replay가 bandwidth 없이 cycles를 낭비한다

Load가 실제 memory request를 한 번만 냈더라도 older store와 violation이 나중에 발견되면 dependent instruction graph를 다시 실행할 수 있다. PMU에서 cache miss는 낮은데 backend/replay stalls가 높은 경우 memory ordering speculation을 의심할 수 있다. Alias-friendly layout과 compiler scheduling이 predictor behavior에 영향을 줄 수 있다.

## CHAPTER 22 · Non-temporal access는 cache pollution을 줄이지만 temporal reuse를 버린다

Streaming writes/loads에 non-temporal hint를 사용하면 일부 cache hierarchy를 우회하거나 eviction priority를 낮출 수 있다. Huge one-pass buffer가 hot cache를 밀어내는 것을 줄일 수 있지만 data를 곧 다시 읽으면 normal cached access보다 느릴 수 있다. Access lifetime과 reuse distance를 근거로 선택해야 한다.

## CHAPTER 23 · Write combining은 작은 stores를 burst로 합치지만 ordering semantics를 확인해야 한다

Non-temporal/uncacheable write path는 write-combining buffer에 data를 모아 larger transaction으로 내보낼 수 있다. Buffer boundary·alignment가 나쁘면 partial transactions가 늘고, device/MMIO 영역에서는 ordinary cacheable memory와 다른 ordering/fence가 필요하다. Throughput optimization이 device-visible ordering contract를 깨뜨려서는 안 된다.

## CHAPTER 24 · Memory fence는 MLP를 제한할 수 있는 ordering barrier다

Strong fence가 outstanding memory operations의 ordering/completion을 요구하면 younger loads/stores가 자유롭게 overlap할 수 있는 범위가 줄어든다. Correctness에 필요한 fence를 제거하면 안 되지만 overly conservative fence를 hot loop에 넣으면 CPU가 가진 MLP를 스스로 차단한다. P27의 memory-model proof로 필요한 최소 ordering을 먼저 확정해야 한다.

## CHAPTER 25 · Atomic RMW는 cache-line ownership과 ordering 때문에 independent miss와 다른 비용을 가진다

Atomic increment는 단순 load+store보다 coherence exclusive ownership과 serialization을 요구하며 같은 line에 여러 cores가 경쟁하면 MLP로 latency를 숨기기 어렵다. Per-CPU sharding/rseq처럼 ownership domain을 분할하는 설계가 단순 prefetch보다 큰 효과를 내는 경우가 많다. Memory optimization은 cache line access mode까지 포함해야 한다.

## CHAPTER 26 · Large out-of-order window도 branch misprediction이 잦으면 future memory work를 못 본다

Frontend가 wrong path를 fetch하거나 frequent misprediction으로 speculative window가 반복 flush되면 independent future loads가 충분히 issue되지 못한다. Memory-bound처럼 보이는 workload가 branch structure 개선으로 빨라질 수 있다. MLP는 backend resource만이 아니라 frontend가 얼마나 멀리 유용한 instructions를 공급하는지에 의존한다.

## CHAPTER 27 · Prefetch benchmark는 cold/warm cache와 prefetcher training을 분리해야 한다

첫 iteration은 hardware prefetcher가 pattern을 학습하지 못했지만 later iterations는 이미 trained state와 warm cache를 사용할 수 있다. 두 구간 평균을 내면 startup와 steady-state가 섞인다. Hardware prefetcher reset이 어려우면 randomized runs, buffer rotation, phase-specific measurement로 training effect를 보고해야 한다.

## CHAPTER 28 · PMU로 MLP를 볼 때 miss count만으로 outstanding concurrency를 알 수 없다

LLC miss 1M회가 모두 직렬로 발생했는지 10개씩 parallel했는지는 count가 같다. Memory-stall cycles, load queue occupancy, line-fill-buffer hit/full, outstanding requests, bandwidth, latency samples 같은 여러 events를 조합해야 한다. PMU availability는 architecture마다 다르므로 P73의 event semantics/multiplex error를 함께 적용해야 한다.

## CHAPTER 29 · MLP 최적화는 single-core speedup과 system capacity를 동시에 시험해야 한다

Prefetch distance와 independent streams를 늘려 한 thread latency가 줄어도 memory bandwidth를 많이 사용하면 multi-tenant server 전체 throughput이 감소할 수 있다. 1/2/4/8/… cores scaling curve에서 bandwidth saturation point와 p99를 측정해야 한다. Per-request speedup이 resource-efficiency 개선인지 단순 bandwidth 선점인지 구분해야 한다.

## CHAPTER 30 · Memory performance는 latency를 숨길 수 있는 dependency/resource budget의 문제다

깊은 memory 최적화는 **instruction window, load/store queue, disambiguation, store forwarding, miss-tracking entries, TLB/page walk, prefetch accuracy/coverage, cache pollution, DRAM bandwidth, fence/atomic ordering, frontend supply**를 하나의 dependency graph로 본다. `cache miss를 줄여라`보다 중요한 질문은 어떤 misses가 독립적으로 동시에 진행될 수 있고, 그 parallelism을 막는 dependency나 finite resource가 무엇인가다.
