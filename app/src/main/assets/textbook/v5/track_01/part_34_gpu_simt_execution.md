# PART 34 · GPU and SIMT — warps, divergence, occupancy, memory coalescing, queues

GPU는 CPU core를 수천 개 복제한 장치가 아니다. 많은 execution lane이 같은 instruction stream을 공유하고, thread group/subgroup 단위로 scheduling되며, 높은 memory latency를 **많은 resident work를 빠르게 교체**하는 방식으로 숨긴다. GPU 성능은 FLOPS 숫자보다 **warp/wave occupancy, divergence, register/shared-memory resource, memory coalescing, synchronization, host-device transfer**의 조합으로 결정된다.

---

## CHAPTER 01 · GPU workload는 thread hierarchy로 표현된다

CUDA에서는 grid→block→thread, Vulkan compute에서는 dispatch→workgroup→invocation 같은 hierarchy를 사용한다. 같은 workgroup의 invocation은 shared/workgroup memory와 barrier를 사용할 수 있지만 서로 다른 workgroup 사이에는 일반적으로 같은 dispatch 안에서 global barrier를 가정할 수 없다.

Algorithm을 GPU에 mapping할 때 dependency가 workgroup boundary를 넘는지 먼저 확인한다. Global synchronization이 필요하면 multiple dispatch/pass로 분리해야 할 수 있다.

---

## CHAPTER 02 · SIMT는 scalar thread abstraction과 grouped execution을 결합한다

Programmer는 여러 thread/invocation이 각자 scalar control flow를 가진 것처럼 작성하지만 hardware는 warp/subgroup 단위 lane을 함께 issue할 수 있다. 동일 instruction을 active lane들이 실행하고 lane별 register/data가 다르다.

Thread abstraction이 독립적이라고 해서 hardware execution cost도 독립적이라는 뜻은 아니다. 같은 subgroup 안 lane의 control-flow와 memory pattern이 서로의 효율에 영향을 준다.

---

## CHAPTER 03 · Warp/subgroup size는 algorithm correctness에 암묵적으로 박으면 안 된다

NVIDIA CUDA에서 warp는 특정 lane group을 뜻하지만 portable graphics/compute API에서는 subgroup size가 implementation마다 달라질 수 있다. Subgroup operation을 사용할 때 actual supported size와 capability를 query하거나 API guarantee를 따라야 한다.

`32 thread면 warp 하나` 같은 assumption을 cross-vendor algorithm의 correctness 조건으로 만들지 않는다.

---

## CHAPTER 04 · Divergence는 branch가 아니라 lane mask serialization 문제다

같은 subgroup lane이 branch condition에서 서로 다른 path를 선택하면 hardware가 active mask를 바꿔 path를 순차적으로 실행할 수 있다. 두 path를 모두 수행한 뒤 reconverge하면 instruction throughput이 낮아진다.

Branch 자체가 나쁜 것은 아니다. 모든 lane이 같은 branch를 선택하면 divergence cost가 거의 없을 수 있다. Performance 질문은 **subgroup 안 조건 분포가 얼마나 갈리는가**다.

---

## CHAPTER 05 · Predication은 짧은 branch를 mask operation으로 바꿀 수 있다

Compiler는 작은 conditional body를 control-flow branch 대신 predicated instruction으로 바꿔 divergence/reconvergence overhead를 줄일 수 있다. 그러나 두 path의 instruction을 모두 issue하고 mask만 적용하면 계산량이 늘 수 있다.

Branch 제거가 자동 최적화가 아니다. Path length와 lane distribution을 보고 compiler output/profile로 판단한다.

---

## CHAPTER 06 · Occupancy는 resident warp 비율이지 utilization 그 자체가 아니다

Streaming multiprocessor가 동시에 유지할 수 있는 active warp 수는 register file, shared memory, block/thread limit에 제약된다. Occupancy는 theoretical maximum 대비 resident warp 비율을 나타낼 수 있지만 high occupancy가 항상 high performance를 뜻하지 않는다.

Arithmetic-intense kernel은 적은 warp만으로 execution unit을 채울 수 있고, register spilling을 줄이기 위해 occupancy를 일부 희생하는 편이 빠를 수 있다.

---

## CHAPTER 07 · Register pressure가 occupancy를 낮춘다

각 thread가 많은 register를 사용하면 block 하나가 요구하는 register 수가 커져 같은 multiprocessor에 동시에 resident할 block/warp 수가 줄어든다. Compiler optimization과 loop unroll이 register 사용을 늘릴 수 있다.

Register count를 억지로 제한하면 local-memory spill이 발생해 훨씬 느린 memory traffic을 만들 수 있다. Register pressure와 occupancy를 함께 튜닝한다.

---

## CHAPTER 08 · Spill된 local memory는 이름과 달리 on-chip register가 아니다

GPU programming model의 local memory는 thread-private address space지만 implementation에서 device memory-backed storage로 내려갈 수 있다. Register allocation 실패나 large local array가 local memory traffic을 만들면 latency/bandwidth가 크게 증가한다.

Source variable scope가 local이라는 사실과 physical storage location을 구분한다.

---

## CHAPTER 09 · Shared/workgroup memory는 software-managed on-chip locality다

같은 block/workgroup의 thread가 빠른 shared memory에 tile/data를 올려 global memory traffic을 줄일 수 있다. 하지만 capacity가 제한되고 block당 allocation이 커지면 occupancy가 낮아진다.

Tiling size는 reuse 증가와 resident block 감소를 교환한다. Matrix multiplication 최적화가 tile 하나만 크게 만들면 안 되는 이유다.

---

## CHAPTER 10 · Bank conflict는 shared memory의 parallel access를 직렬화할 수 있다

Shared memory는 여러 bank로 나뉘고 lane address가 같은 bank에 충돌하면 access가 여러 transaction으로 나뉠 수 있다. Layout padding이나 index transformation으로 bank distribution을 바꿀 수 있다.

Global-memory coalescing과 shared-memory bank conflict는 서로 다른 memory layer 문제다. 둘을 `memory access 최적화` 한 문장으로 묶지 않는다.

---

## CHAPTER 11 · Global memory coalescing은 lane 주소를 memory transaction으로 묶는 효율이다

Subgroup lane이 연속/정렬된 address를 access하면 hardware가 적은 memory transaction으로 요청을 합칠 수 있다. Lane이 넓은 stride나 scattered address를 읽으면 더 많은 cache line/segment를 가져와 useful byte 비율이 낮아진다.

Array-of-Struct와 Struct-of-Arrays layout 선택은 CPU cache뿐 아니라 GPU lane access pattern에 따라 달라진다.

---

## CHAPTER 12 · Misalignment도 transaction 수를 늘릴 수 있다

연속 address라도 transaction boundary와 어긋나면 동일 data를 가져오기 위해 추가 memory segment가 필요할 수 있다. Data alignment와 padding은 vector load와 coalescing efficiency에 영향을 준다.

Allocator base alignment뿐 아니라 field/row pitch까지 확인한다.

---

## CHAPTER 13 · GPU cache hierarchy도 workload별 hit behavior가 다르다

GPU에는 instruction/cache, L1/texture/read-only cache, L2 등 architecture-specific hierarchy가 있을 수 있다. Read-only spatial locality, texture sampling, general global load가 서로 다른 path/caching policy를 사용할 수 있다.

`global memory = DRAM`으로 바로 등치하지 않는다. L2 hit와 DRAM transaction을 별도 counter로 본다.

---

## CHAPTER 14 · Latency hiding은 independent warp가 있어야 작동한다

한 warp가 global memory를 기다리는 동안 scheduler가 다른 ready warp를 issue해 execution units를 활용할 수 있다. Resident warp가 있어도 모두 같은 dependency/memory bottleneck으로 stall되면 latency를 숨기지 못한다.

Occupancy와 eligible-warps-per-cycle, stall reason을 같이 봐야 한다.

---

## CHAPTER 15 · Instruction-level parallelism도 GPU에서 중요하다

한 thread가 서로 독립적인 arithmetic/memory operation을 갖고 있으면 warp 내부에서 pipeline utilization을 높일 수 있다. 반대로 long dependency chain은 warp가 다음 instruction을 기다리게 한다.

More threads만 늘리는 것과 per-thread dependency를 줄이는 것은 서로 다른 latency-hiding 전략이다.

---

## CHAPTER 16 · Barrier는 workgroup 전체의 rendezvous이며 divergence와 결합하면 deadlock 위험이 있다

Workgroup barrier는 참여해야 할 invocation이 모두 해당 synchronization point에 도달해야 한다. 일부 lane이 conditional path 때문에 barrier를 건너뛰고 다른 lane만 기다리면 undefined/invalid behavior가 될 수 있다.

Barrier placement는 control-flow uniformity와 memory visibility scope를 함께 증명해야 한다.

---

## CHAPTER 17 · Execution barrier와 memory barrier는 구분한다

모든 invocation이 같은 point에 도달했다는 execution synchronization과, 이전 memory write가 이후 read에 visible하도록 하는 memory ordering은 다른 조건이다. API primitive는 둘을 함께 제공하거나 stage/access scope를 따로 지정할 수 있다.

Vulkan synchronization에서 pipeline stage와 access mask를 정확히 지정해야 하는 이유다.

---

## CHAPTER 18 · GPU atomic은 global serialization hot spot이 될 수 있다

여러 lane/block이 같은 global atomic counter를 갱신하면 memory subsystem이 해당 location의 atomic serialization을 처리해야 한다. Warp-level reduction→block local aggregation→global atomic처럼 hierarchy를 이용해 contention을 줄일 수 있다.

Atomic correctness와 throughput scalability를 분리한다.

---

## CHAPTER 19 · Subgroup operation은 shared memory 없이 lane 간 data exchange를 가능하게 한다

Shuffle, ballot, vote, subgroup reduction 같은 primitive는 같은 subgroup lane 사이 register-like exchange/collective를 제공할 수 있다. Shared memory와 barrier를 줄여 latency를 낮출 수 있지만 subgroup-size/control-flow assumption이 생긴다.

Portable code는 subgroup capability와 active mask semantics를 명확히 다룬다.

---

## CHAPTER 20 · Work distribution은 load balance와 locality를 동시에 결정한다

Block/workgroup마다 작업량이 크게 다르면 일부 SM은 일찍 idle해지고 long-tail block이 kernel completion을 지연시킨다. 너무 작은 work unit은 scheduling overhead와 poor locality를 만들 수 있다.

Persistent-thread/work-stealing류 design은 dynamic load balancing을 제공하지만 atomic queue contention과 fairness 비용이 추가된다.

---

## CHAPTER 21 · Kernel launch도 고정 overhead를 가진다

매우 작은 kernel을 수천 번 launch하면 실제 arithmetic보다 launch/synchronization overhead가 커질 수 있다. Kernel fusion은 intermediate global-memory traffic과 launch 수를 줄일 수 있지만 register pressure와 code complexity를 늘린다.

Fusion 여부는 end-to-end timeline과 occupancy를 같이 측정한다.

---

## CHAPTER 22 · Host-device transfer는 PCIe/UMA topology에 따라 비용 모델이 다르다

Discrete GPU는 CPU DRAM과 device VRAM이 분리되어 PCIe copy가 필요할 수 있고 integrated/UMA system은 physical memory를 공유하면서 cache/coherency/ownership cost가 다르다. `GPU memory copy`라는 API 이름만으로 physical transfer를 단정하지 않는다.

Topology에 따라 pinned host memory, staging buffer, unified memory의 trade-off가 달라진다.

---

## CHAPTER 23 · Pinned host memory는 DMA에 유리하지만 OS memory flexibility를 줄인다

Pageable user memory는 DMA 전에 pin/copy가 필요할 수 있다. Page-locked memory를 미리 확보하면 transfer setup을 줄일 수 있지만 reclaim/migration 불가능 memory가 늘어나 system pressure를 만들 수 있다.

Pinned pool은 bounded resource로 관리하고 transfer completion 뒤 ownership을 정확히 반환한다.

---

## CHAPTER 24 · Unified/managed memory는 pointer 편의와 page migration fault를 교환한다

Unified virtual memory는 CPU/GPU가 같은 pointer/address space를 사용하게 할 수 있지만 page residency가 CPU↔GPU 사이 이동하거나 page fault로 on-demand migration될 수 있다. Irregular ping-pong access는 transfer thrashing을 만들 수 있다.

Prefetch/advice와 access phase separation으로 residency를 안정화한다.

---

## CHAPTER 25 · Graphics/compute queue는 command submission과 execution dependency를 분리한다

Vulkan 같은 explicit API에서 command buffer를 queue에 submit하고 semaphore/fence/event로 queue 간/host 간 dependency를 표현한다. CPU가 submit call을 반환했다고 GPU work가 완료된 것은 아니다.

Resource destroy/reuse는 fence/timeline semaphore가 완료를 증명한 뒤 수행한다. GPU use-after-free는 CPU pointer lifetime만 봐서는 찾기 어렵다.

---

## CHAPTER 26 · Pipeline barrier를 과도하게 쓰면 GPU parallelism을 스스로 직렬화한다

모든 stage를 wait하고 모든 memory를 synchronize하는 broad barrier는 correctness에는 쉬워도 graphics/compute overlap과 cache efficiency를 해친다. Producer stage/access와 consumer stage/access를 정확히 좁히면 필요한 dependency만 강제할 수 있다.

Synchronization 최적화는 PART 27처럼 ordering edge를 제거하기 전에 hazard가 없는지 증명해야 한다.

---

## CHAPTER 27 · Async compute는 compute와 graphics가 실제 resource를 공유한다

별도 queue가 있어도 compute shader와 graphics pipeline이 동일 shader core, cache, memory bandwidth를 경쟁할 수 있다. Overlap이 항상 total time을 줄이는 것은 아니다. 두 workload가 같은 bottleneck을 쓰면 concurrency가 서로를 느리게 만든다.

Queue overlap은 GPU utilization trace와 memory/SM counter로 검증한다.

---

## CHAPTER 28 · GPU profiling은 kernel time뿐 아니라 stall reason과 transfer timeline을 봐야 한다

Kernel duration이 길어도 compute-bound인지 memory-bound인지 divergence/occupancy 문제인지 모른다. Warp stall, branch efficiency, memory transaction, cache hit, occupancy, launch/transfer timeline을 함께 본다.

Single metric `GPU utilization 99%`는 bottleneck을 설명하지 않는다. 어떤 engine이 어떤 이유로 busy한지 분해한다.

---

## CHAPTER 29 · Floating-point reproducibility는 parallel reduction order에 따라 달라질 수 있다

부동소수점 덧셈은 일반적으로 결합법칙이 정확히 성립하지 않으므로 parallel reduction tree가 바뀌면 마지막 bit 결과가 달라질 수 있다. Atomic/reduction scheduling이 nondeterministic하면 같은 input에서 small numerical difference가 발생할 수 있다.

정확 재현이 필요한 workload는 deterministic reduction order와 numerical tolerance policy를 명시한다.

---

## CHAPTER 30 · GPU 설계의 최종 계약은 mapping·locality·occupancy·synchronization·transfer다

1. **Mapping** — logical data/task를 grid/workgroup/subgroup/lane에 어떻게 배치하는가.
2. **Locality** — global/shared/register/cache에서 reuse와 coalescing이 어떻게 일어나는가.
3. **Occupancy** — register/shared-memory/resource 사용이 resident work를 얼마나 제한하는가.
4. **Synchronization** — barrier/atomic/queue dependency가 필요한 ordering만 강제하는가.
5. **Transfer** — host/device/queue 사이 data와 resource ownership이 언제 이동하는가.

GPU 최적화는 thread 수를 늘리는 것이 아니라 이 다섯 축에서 hardware execution structure와 algorithm structure를 맞추는 작업이다.
