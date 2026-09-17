# PART 34 · GPU SIMT Execution — workgroups, divergence, occupancy, memory

GPU 성능은 core 수나 FLOPS 숫자 하나로 설명되지 않는다. thread가 어떤 hierarchy로 묶여 실행되는지, subgroup이 같은 instruction을 어떻게 공유하는지, register·shared memory가 occupancy를 얼마나 제한하는지, global-memory transaction이 얼마나 잘 합쳐지는지를 함께 봐야 한다. 이 PART는 **execution width, memory traffic, synchronization, queue submission**을 하나의 모델로 연결한다.

---

## CHAPTER 01 · thread hierarchy는 work를 hardware scheduling 단위로 묶는다

GPU programming model은 많은 logical invocation을 grid·workgroup·subgroup·thread 같은 계층으로 묶는다. 각 계층은 단순 번호 체계가 아니라 scheduling과 synchronization 범위를 결정한다. workgroup 내부 thread는 shared memory와 barrier를 활용할 수 있지만 서로 다른 workgroup 사이에는 일반적으로 같은 방식의 즉시 barrier를 기대할 수 없다.

문제는 logical work 크기와 hardware resident capacity가 다르다는 데서 시작한다. 너무 큰 workgroup은 register와 local/shared memory를 과도하게 소비해 동시에 올라갈 group 수를 줄일 수 있고, 너무 작은 group은 scheduler가 latency를 숨길 충분한 ready work를 확보하지 못할 수 있다.

튜닝에서는 total thread 수만 보지 않고 workgroup size, resident group 수, subgroup utilization을 함께 측정한다. 같은 알고리즘이라도 입력 shape와 GPU 세대가 바뀌면 최적 hierarchy가 달라질 수 있으므로 launch parameter를 immutable 상수처럼 취급하지 않는다.

---

## CHAPTER 02 · SIMT는 여러 thread가 instruction issue를 공유하는 실행 방식이다

SIMT 계열에서는 여러 thread가 같은 instruction stream을 함께 진행하지만 각 thread는 자신만의 register state와 predicate를 가질 수 있다. 따라서 source에서는 독립 thread처럼 보이면서도 hardware issue 자원은 subgroup 단위로 공유한다. SIMD와 유사한 면이 있지만 programming abstraction과 divergence 처리 방식이 같다고 단정하면 안 된다.

같은 subgroup에서 thread별 control path가 갈라지면 모든 path가 동시에 완전히 병렬로 실행되지 못할 수 있다. 반대로 동일 instruction을 동일 시점에 수행하면 fetch·decode·execution 자원을 효율적으로 활용한다. arithmetic instruction 수만 세면 이 실행 효율 차이를 놓친다.

profiling에서는 active lane 비율, executed instruction, subgroup divergence를 연결한다. CPU thread 모델의 직관을 그대로 옮기지 말고 어떤 단위가 실제 issue와 synchronization을 공유하는지 target API와 hardware 기준으로 확인한다.

---

## CHAPTER 03 · subgroup size는 algorithm의 implicit constant가 되어서는 안 된다

subgroup은 함께 실행·통신하는 lane 집합을 제공하지만 size가 모든 device에서 동일하다고 가정하면 portability가 깨질 수 있다. 특정 API나 feature가 size를 노출하거나 제약할 수 있어도 algorithm이 하드코딩한 32·64 같은 숫자에 의미를 묶으면 다른 architecture에서 잘못된 reduction이나 ballot 결과가 생길 수 있다.

subgroup operation을 사용할 때는 active lane mask, partial subgroup, tail invocation을 처리해야 한다. workgroup size가 subgroup size의 정확한 배수가 아닐 수 있고 helper/inactive lane semantics도 operation에 따라 확인해야 한다.

테스트는 여러 subgroup size를 가정한 logical simulation과 실제 device matrix를 함께 사용한다. correctness가 특정 GPU에서만 맞는다면 performance보다 먼저 hidden width assumption을 찾는다.

---

## CHAPTER 04 · divergence는 control-flow 차이를 execution serialization 비용으로 바꾼다

같은 subgroup의 lane들이 서로 다른 branch를 선택하면 hardware는 각 path의 active lane을 바꾸며 실행할 수 있다. branch 자체가 비싼 것이 아니라 **같은 issue slot에서 useful lane 비율이 떨어지는 것**이 핵심 비용이다. branch가 매우 predictable해 모든 lane이 같은 방향을 택하면 divergence cost가 작을 수 있다.

조건별 data가 섞여 있으면 sorting·compaction·work partitioning으로 유사 path를 묶는 방법이 있다. 하지만 재배열 비용과 memory locality 손실이 더 클 수도 있다. branchless 변환도 양쪽 계산을 모두 수행하면 오히려 arithmetic과 bandwidth를 낭비한다.

profile에서 branch count만 보지 말고 divergent branch와 active-lane efficiency를 확인한다. input distribution이 달라질 때 성능이 급변하면 control-flow entropy를 workload 속성으로 기록한다.

---

## CHAPTER 05 · predication은 짧은 분기를 mask된 operation으로 바꿀 수 있다

짧은 conditional path는 branch를 만들기보다 predicate로 lane별 결과 반영 여부를 제어할 수 있다. 이 방식은 control transfer를 줄이지만 inactive lane도 instruction issue에 포함될 수 있어 계산량이 공짜가 아니다. compiler와 backend가 cost model에 따라 branch와 predication 사이를 선택할 수 있다.

복잡한 path를 무조건 predication으로 바꾸면 양쪽 load와 expensive function이 실행돼 memory traffic이 증가할 수 있다. 특히 fault 가능 access나 side effect operation은 단순 mask 처리로 의미가 보존되지 않는다.

IR·shader disassembly와 lane-utilization counter를 함께 봐야 실제 선택을 알 수 있다. source의 `if` 유무만으로 GPU branch cost를 판단하지 않는다.

---

## CHAPTER 06 · occupancy는 resident work의 양이지 performance 점수가 아니다

occupancy는 execution unit에 동시에 resident할 수 있는 active warps/subgroups의 비율로 표현될 수 있다. 높은 occupancy는 memory latency를 다른 ready work로 숨길 기회를 늘리지만, 이미 충분한 latency hiding이 확보됐다면 더 올려도 성능이 증가하지 않을 수 있다.

register 수와 shared memory 사용량, workgroup 크기가 resident block 수를 제한한다. occupancy를 높이려고 register를 강제로 줄이면 spill이 생겨 local/global memory traffic이 늘 수 있다. 최대 occupancy와 최대 throughput은 다른 목표다.

benchmark에서는 occupancy, stall reason, achieved bandwidth를 함께 비교한다. occupancy만 낮다는 이유로 kernel을 실패로 판정하지 않고 실제 bottleneck이 latency hiding인지 확인한다.

---

## CHAPTER 07 · register pressure는 parallel residency와 spill 사이 trade-off를 만든다

각 thread가 많은 register를 요구하면 한 compute unit에 동시에 resident할 수 있는 thread 수가 줄어든다. compiler가 available register를 넘으면 spill이 발생해 빠른 register value가 local memory 계층으로 내려갈 수 있다. source local variable 개수와 실제 register pressure는 inlining·unrolling·liveness에 따라 크게 달라진다.

register limit 옵션을 무리하게 낮추면 occupancy 숫자는 좋아져도 spill load/store 때문에 느려질 수 있다. 반대로 unrolling으로 register를 늘렸지만 memory latency가 줄어 전체 성능이 개선될 수도 있다.

compiler report의 register count와 profiler의 local-memory traffic을 함께 본다. 작은 source change 뒤 kernel 성능이 급락하면 instruction count뿐 아니라 register-allocation 임계점을 확인한다.

---

## CHAPTER 08 · local memory는 이름과 달리 항상 빠른 on-chip storage가 아니다

GPU 문맥에서 thread-local address space가 물리적으로 register가 아니라 backing memory에 놓일 수 있으며 compiler spill이나 큰 automatic array가 이 경로를 사용할 수 있다. `local`이라는 이름을 CPU stack cache처럼 해석하면 latency를 잘못 추정한다.

index가 동적이거나 object 크기가 커 register promotion이 어려우면 local-memory transaction이 증가할 수 있다. cache가 일부 비용을 숨겨도 많은 thread가 동시에 spill하면 bandwidth와 cache pressure가 커진다.

profiling에서는 local load/store와 register count를 연결한다. source에서 array를 작은 scalar로 분해하거나 access pattern을 바꾼 뒤 실제 memory transaction이 줄었는지 확인한다.

---

## CHAPTER 09 · shared memory는 workgroup 내부 협업을 위한 명시적 scratchpad다

shared/workgroup memory는 같은 group의 thread가 낮은 latency로 data를 교환하고 global load를 재사용하게 해준다. 대신 용량이 제한되고 barrier와 bank 구조를 고려해야 한다. cache와 달리 programmer가 placement와 synchronization을 직접 설계한다는 점이 핵심이다.

tile을 너무 크게 잡으면 group당 shared-memory footprint가 커져 occupancy가 낮아진다. producer가 값을 쓰고 consumer가 읽기 전에 적절한 barrier가 없으면 data race가 발생한다. group 밖 thread와의 communication에 같은 memory를 사용할 수도 없다.

shared-memory bytes per group, bank conflict, barrier stall을 함께 측정한다. global traffic 감소가 resident-group 감소보다 큰 이득을 주는지 workload별로 확인한다.

---

## CHAPTER 10 · bank conflict는 shared-memory address pattern을 serialized access로 바꿀 수 있다

shared memory는 여러 bank로 나뉘어 lane들이 서로 다른 bank를 접근하면 병렬 service가 가능하다. 여러 lane이 같은 cycle에 충돌하는 bank pattern을 만들면 access가 여러 transaction으로 나뉠 수 있다. 자료구조가 논리적으로 contiguous하다고 bank 관점에서도 최적인 것은 아니다.

matrix tile의 stride가 bank 수와 나쁜 공약수를 가지면 반복 충돌이 생길 수 있다. padding 한 칸이 layout을 바꿔 큰 차이를 만들기도 하지만 architecture별 bank width와 broadcast rule을 확인해야 한다.

bank-conflict counter와 access stride를 연결한다. padding 전후 shared-memory transaction 수와 occupancy 변화까지 함께 비교해 단순 용량 증가의 부작용을 확인한다.

---

## CHAPTER 11 · coalescing은 여러 lane의 global access를 적은 transaction으로 합친다

global memory bandwidth를 잘 쓰려면 인접 lane이 가능한 한 인접 address를 접근해 memory transaction 수를 줄이는 것이 유리하다. lane별 stride가 크거나 pointer chasing을 하면 같은 useful bytes를 위해 더 많은 cache line과 transaction을 가져올 수 있다.

구조체 배열은 thread가 한 field만 읽는 workload에서 불필요한 field bytes를 함께 가져올 수 있다. SoA 변환은 coalescing을 개선할 수 있지만 CPU-side layout과 serialization 비용이 바뀐다.

requested bytes와 actual global-memory transaction을 비교한다. bandwidth가 낮은데 memory latency stall이 높다면 coalescing과 cache hit를 함께 확인한다.

---

## CHAPTER 12 · alignment는 transaction 경계와 vector load 가능성을 결정한다

address가 요구 alignment를 만족하지 않으면 하나의 logical access가 여러 memory segment에 걸쳐 추가 transaction을 만들 수 있다. 일부 instruction은 alignment 위반을 허용하면서 느려지고, 다른 경우에는 API contract를 위반할 수 있다. host-device buffer offset도 같은 문제를 만든다.

padding과 allocator alignment를 source struct에만 의존하면 sub-buffer offset에서 다시 깨질 수 있다. vectorized load를 사용할 때 element base와 stride 모두 target requirement를 만족해야 한다.

profile에서 misaligned transaction과 memory throughput을 확인하고, runtime assertion으로 critical buffer alignment를 검증한다. correctness와 performance requirement를 문서에서 분리한다.

---

## CHAPTER 13 · GPU cache는 access locality를 돕지만 coalescing 문제를 자동 해결하지 않는다

GPU에도 여러 cache 계층이 존재할 수 있지만 capacity와 policy는 CPU와 동일하지 않다. repeated read가 cache hit를 얻어도 lane access가 흩어져 있으면 많은 line을 가져와야 한다. cache를 믿고 나쁜 global layout을 방치하면 working set이 커지는 순간 성능이 급락한다.

read-only data, texture-like path, shared memory가 각각 다른 locality 이점을 제공할 수 있다. cache hint를 사용하기 전에 reuse distance와 access pattern을 측정한다.

cache hit ratio, requested throughput, DRAM throughput을 같이 본다. 높은 hit ratio가 곧 낮은 latency를 뜻하지 않으며 cache bandwidth 자체도 포화될 수 있다.

---

## CHAPTER 14 · latency hiding은 기다리는 warp 대신 ready warp를 실행하는 전략이다

GPU는 한 warp가 memory result를 기다릴 때 다른 ready warp를 issue해 긴 latency를 숨긴다. 따라서 single access latency가 커도 충분한 independent work와 occupancy가 있으면 throughput은 높을 수 있다. 반대로 dependency chain이 길고 resident work가 적으면 memory latency가 그대로 stall로 드러난다.

latency hiding을 위해 thread 수만 늘리면 register/shared-memory resource가 부족해질 수 있다. independent instruction을 늘려 한 thread 내부 ILP를 높이는 방법도 있다.

stall reason에서 memory dependency와 no-eligible-warp 상태를 구분한다. occupancy와 ILP를 동시에 바꾸지 말고 한 축씩 측정해 원인을 좁힌다.

---

## CHAPTER 15 · ILP는 한 thread 안의 독립 operation으로 scheduler 선택지를 늘린다

instruction-level parallelism이 충분하면 하나의 thread도 여러 independent memory request나 arithmetic을 겹칠 수 있다. dependency chain이 길면 많은 warp가 있어도 각 warp가 자주 stall할 수 있다. loop unrolling이 ILP를 늘릴 수 있지만 register pressure도 동시에 증가한다.

여러 accumulator를 사용한 reduction은 dependency latency를 분산할 수 있다. 하지만 compiler가 이미 scheduling을 최적화했다면 수동 변환이 code size와 register만 늘릴 수 있다.

generated code와 dependency stall을 확인한다. ILP 최적화 후 register count와 occupancy까지 함께 재측정해 trade-off를 수치로 남긴다.

---

## CHAPTER 16 · workgroup barrier는 group 내부 execution rendezvous를 만든다

barrier는 같은 workgroup의 참여 thread가 특정 지점에 도달할 때까지 진행을 맞춘다. 모든 thread가 barrier에 도달하지 않는 divergent control-flow 안에 barrier를 두면 deadlock 또는 undefined behavior 위험이 있다. barrier는 global device synchronization을 의미하지 않는다.

barrier 자체가 memory visibility를 모두 해결하는지도 API semantics에 따라 확인해야 한다. execution synchronization과 memory ordering scope를 구분한다.

검증에서는 barrier 이전 write와 이후 read를 race detector 또는 validation layer와 함께 확인한다. 성능에서는 barrier wait 분포를 보고 workload imbalance가 큰지 측정한다.

---

## CHAPTER 17 · execution barrier와 memory barrier는 서로 다른 질문에 답한다

execution barrier는 어떤 operation이 다른 operation보다 먼저 또는 나중에 진행하도록 제약하고, memory barrier는 write가 특정 scope의 observer에게 어떤 순서로 보이는지 정의한다. API에 따라 두 기능이 한 primitive에 묶이기도 하지만 개념을 분리해야 최소한의 synchronization을 설계할 수 있다.

너무 broad한 stage·scope를 지정하면 unrelated work까지 serialize되어 GPU pipeline concurrency가 줄어든다. 반대로 scope가 좁으면 consumer가 아직 visible하지 않은 data를 읽는다.

validation warning만 없애기 위해 global barrier를 남발하지 않는다. producer resource, consumer stage, access type을 정확히 매칭하고 barrier 전후 queue timestamp를 profile한다.

---

## CHAPTER 18 · GPU atomic은 memory race를 직렬화하지만 contention 비용을 만든다

atomic operation은 동일 location의 update를 잃지 않게 하지만 많은 lane이 한 counter에 집중되면 해당 cache line 또는 memory partition이 hot spot이 된다. correctness를 얻는 대가로 throughput이 급격히 감소할 수 있다.

per-workgroup aggregation 후 global atomic을 한 번 수행하거나 sharded counter를 사용하면 contention을 줄일 수 있다. atomic ordering과 scope도 필요 이상으로 강하게 잡지 않는다.

profile에서는 atomic instruction 수뿐 아니라 serialization stall과 address concentration을 본다. input skew가 심할 때 성능이 무너지는지 별도 workload로 테스트한다.

---

## CHAPTER 19 · subgroup operation은 lane 사이 통신을 shared memory 없이 수행할 수 있다

shuffle, ballot, vote 같은 subgroup primitive는 같은 subgroup lane 사이 data exchange와 collective operation을 낮은 overhead로 제공할 수 있다. 대신 active mask와 subgroup width에 대한 정확한 이해가 필요하다. inactive lane 값을 읽거나 모든 lane이 참여한다고 가정하면 결과가 잘못될 수 있다.

reduction을 subgroup primitive로 바꾸면 barrier와 shared-memory traffic을 줄일 수 있지만 여러 subgroup을 합치는 단계는 여전히 필요하다. portability 요구가 크면 fallback path를 유지한다.

테스트에서는 partial subgroup과 divergent mask를 포함한다. performance는 shared-memory 버전과 동일 input에서 instruction·barrier 수를 비교한다.

---

## CHAPTER 20 · work distribution은 균등한 thread 수보다 균등한 service time이 중요하다

각 thread에 같은 element 수를 배정해도 data-dependent loop나 sparse structure가 있으면 실행 시간이 크게 다를 수 있다. 한 subgroup의 느린 lane이 전체 progression을 늦추고, workgroup별 편차는 compute unit utilization을 떨어뜨린다.

persistent kernel이나 dynamic work queue는 load imbalance를 줄일 수 있지만 global atomic·queue contention을 추가한다. 작은 task를 지나치게 쪼개면 scheduling overhead가 커진다.

thread/workgroup duration histogram과 SM utilization을 함께 본다. 평균 work량보다 tail group이 전체 kernel completion을 지배하는지 확인한다.

---

## CHAPTER 21 · kernel launch overhead는 작은 work에서 계산 자체보다 클 수 있다

host가 command를 준비하고 driver/runtime을 거쳐 GPU queue에 work를 제출하는 데는 고정 비용이 있다. 매우 작은 kernel을 연속 실행하면 useful compute보다 launch와 synchronization이 더 큰 비중을 차지할 수 있다.

kernel fusion은 launch 수와 intermediate memory traffic을 줄일 수 있지만 register pressure와 code complexity를 늘린다. graph/batched submission 같은 API도 선택지가 될 수 있다.

kernel duration과 host launch interval을 분리해 측정한다. GPU timestamp와 CPU wall time의 차이를 보면 queueing·launch overhead를 구분할 수 있다.

---

## CHAPTER 22 · host-device transfer는 bus bandwidth와 synchronization을 소비한다

GPU가 discrete memory를 사용할 때 host와 device 사이 copy가 전체 pipeline의 큰 비용이 될 수 있다. PCIe bandwidth는 device DRAM bandwidth보다 낮을 수 있고 작은 transfer는 per-operation latency가 지배한다. compute 최적화가 transfer 비용에 묻히는 경우가 많다.

batching, overlap, data residency를 통해 transfer 횟수를 줄일 수 있다. 매 iteration마다 동일 read-only data를 다시 보내는 구조는 residency contract를 재설계해야 한다.

bytes transferred, transfer duration, compute overlap을 timeline으로 본다. end-to-end latency에서 copy 비중을 먼저 확인한 뒤 kernel micro-optimization을 진행한다.

---

## CHAPTER 23 · pinned host memory는 faster transfer와 memory-management 제약을 교환한다

page-locked host memory는 DMA setup과 asynchronous transfer에 유리할 수 있지만 OS가 해당 page를 쉽게 reclaim하거나 migrate하지 못한다. 큰 pinned pool은 system memory pressure와 다른 process latency에 영향을 준다.

필요 이상 오래 pin하면 mobile/desktop 환경에서 power와 memory 문제가 생긴다. short-lived transfer마다 pin/unpin을 반복하면 setup overhead가 커질 수 있어 bounded pool이 일반적인 절충안이다.

pinned bytes, transfer throughput, system pressure를 함께 모니터링한다. GPU benchmark만 보고 host 전체의 memory health를 무시하지 않는다.

---

## CHAPTER 24 · unified memory는 address 편의와 migration cost를 함께 가져온다

unified memory 계열은 CPU와 GPU가 하나의 pointer model을 공유하게 해 programming을 단순화하지만 실제 page residency와 migration이 사라지는 것은 아니다. first touch와 access pattern에 따라 page fault·migration이 runtime 중 발생해 latency spike를 만들 수 있다.

CPU와 GPU가 같은 page를 번갈아 쓰면 ownership ping-pong이 생길 수 있다. prefetch·placement hint는 working set이 예측 가능한 경우에 유용하지만 잘못 사용하면 불필요한 migration을 늘린다.

GPU page fault와 migration bytes를 profile한다. warm run만 측정하지 말고 cold placement와 phase transition을 포함해 평가한다.

---

## CHAPTER 25 · queue submission은 command recording과 device execution을 분리한다

Vulkan 같은 explicit API에서는 command를 기록하는 시점과 queue에 제출되어 실제 실행되는 시점이 다르다. resource가 command recording 뒤 수정되거나 파괴되어도 execution이 끝날 때까지 필요한 lifetime을 유지해야 한다.

여러 queue를 사용하면 overlap 가능성이 늘지만 ownership transfer와 semaphore dependency가 복잡해진다. submission order만으로 모든 resource hazard가 해결된다고 가정하면 안 된다.

CPU submission timestamp, GPU begin/end timestamp, semaphore state를 연결한다. queue idle을 자주 호출하면 implicit global serialization이 생기는지 확인한다.

---

## CHAPTER 26 · pipeline barrier는 정확한 producer-consumer dependency만 표현해야 한다

pipeline barrier는 resource access의 실행 순서와 visibility를 stage/access scope로 표현한다. 모든 stage에서 모든 stage로 broad barrier를 걸면 correctness는 얻기 쉬워도 parallelism을 크게 잃는다. 반대로 consumer가 필요한 write를 포함하지 않으면 race가 생긴다.

image layout transition과 ownership transfer가 barrier semantics와 함께 묶이는 경우도 있어 resource state machine을 문서화해야 한다. old/new layout만 맞추고 access mask가 틀린 오류가 흔하다.

validation layer와 GPU timeline을 함께 사용한다. barrier 최적화는 warning 제거가 아니라 실제 dependency graph를 최소화하는 작업이다.

---

## CHAPTER 27 · asynchronous compute는 독립 work가 있을 때만 overlap 이득이 생긴다

graphics와 compute queue를 동시에 사용해도 두 workload가 같은 memory bandwidth나 execution unit을 포화시키면 실제 overlap 이득이 없거나 서로 느려질 수 있다. dependency semaphore가 촘촘하면 queue를 나눈 의미도 사라진다.

async compute에 적합한 work는 resource dependency가 느슨하고 다른 pipeline의 idle resource를 활용할 수 있어야 한다. 작은 kernel을 별도 queue로 옮기는 overhead가 더 클 수도 있다.

queue별 active time과 hardware unit utilization을 profile한다. 단순 wall-time 겹침이 아니라 total frame/request latency가 줄었는지 확인한다.

---

## CHAPTER 28 · GPU profiling은 queue wait와 execution stall을 분리해야 한다

GPU kernel이 늦게 끝났다는 사실만으로 kernel 내부가 느린 것은 아니다. queue에 오래 기다렸거나 semaphore dependency가 늦게 풀렸을 수 있다. device timestamp와 hardware counter를 사용해 queued, executing, stalled 단계를 구분한다.

profiling counter 수집이 kernel scheduling과 clock을 바꿀 수 있어 observer effect도 고려한다. thermal throttling과 GPU frequency state가 run마다 다르면 microbenchmark 비교가 흔들린다.

동일 workload에서 warmup, power state, input을 고정하고 여러 반복의 distribution을 본다. CPU trace와 GPU trace의 clock domain도 정확히 align한다.

---

## CHAPTER 29 · floating-point reproducibility는 GPU parallel reduction 순서에 영향을 받는다

floating-point addition은 일반적으로 결합법칙을 정확히 만족하지 않으므로 reduction tree와 lane scheduling이 달라지면 마지막 bit가 달라질 수 있다. GPU parallelism은 합산 순서를 CPU serial loop와 다르게 만들며 compiler fast-math 옵션도 결과 범위를 바꾼다.

bit-identical 결과가 필요한 workload와 허용 오차 기반 결과가 가능한 workload를 구분해야 한다. deterministic reduction은 성능을 일부 희생할 수 있다.

테스트는 exact equality만 사용하지 말고 domain error budget을 정의한다. device·driver·optimization 변경 후 numerical drift를 distribution으로 비교한다.

---

## CHAPTER 30 · GPU contract는 execution width, memory, synchronization, lifetime을 함께 명시한다

안전하고 빠른 GPU kernel은 thread hierarchy와 subgroup 가정을 밝히고, buffer layout·alignment와 ownership을 정하며, shared/global memory race를 barrier scope로 증명해야 한다. host submission과 device completion 사이 resource lifetime도 contract의 일부다.

최적화는 occupancy 하나를 최대화하는 작업이 아니다. register, shared memory, coalescing, divergence, queue overlap 중 실제 bottleneck을 evidence로 식별하고 한 축씩 변경한다.

release 검증에는 여러 GPU 세대, subgroup size, input skew, memory pressure를 포함한다. 목표는 특정 장치에서 벤치마크 숫자를 만드는 것이 아니라 **허용된 실행·memory ordering에서 같은 결과와 예측 가능한 성능을 유지하는 것**이다.
