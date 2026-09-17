# PART 38 · CPU Vector/SIMD — lane, predicate, reduction, vector ABI

CPU vector execution은 같은 연산을 여러 데이터 요소에 적용해 instruction-level data parallelism을 얻는 방식이다. 실제 성능은 vector width 하나로 결정되지 않는다. 메모리 배치, alias 가능성, loop-carried dependency, mask 비용, register pressure, tail 처리, reduction 순서, 수치 의미 보존 조건이 함께 맞아야 scalar work가 효율적인 vector instruction으로 변환된다.

## CHAPTER 01 · vector instruction은 여러 lane에 하나의 operation semantics를 적용한다

Vector register는 여러 scalar element를 담는 register class로 볼 수 있고, instruction은 element lane에 동일하거나 정의된 형태의 연산을 수행한다. 그러나 vectorization은 단순히 `N개 값을 한 번에 계산한다`가 아니다. Lane 사이 data dependency가 없거나 vector form으로 표현 가능해야 하고, load/store가 요구하는 주소 패턴도 vector hardware가 효율적으로 처리할 수 있어야 한다. 같은 256-bit register라도 8개의 32-bit lane, 4개의 64-bit lane처럼 element type에 따라 병렬 element 수가 달라진다. 따라서 성능 모델은 register bit width보다 element width, instruction throughput, dependency latency, memory traffic을 함께 봐야 한다.

## CHAPTER 02 · fixed-width와 scalable vector는 code-generation 계약이 다르다

일부 ISA는 compile-time에 정해진 vector width를 중심으로 instruction을 제공하고, 다른 설계는 실제 hardware vector length를 runtime에 반영하는 scalable vector model을 지원한다. Fixed-width code는 특정 vector width에 맞춘 unroll과 tail 처리가 명확하지만 여러 microarchitecture에서 width 차이를 흡수하려면 binary multiversioning이나 compiler 선택이 필요할 수 있다. Scalable model은 `한 vector에 정확히 몇 element`라는 가정을 code에서 제거하는 대신 predicate와 vector-length-aware loop 구조를 요구한다. Library API가 vector width를 public data layout에 박아 넣으면 ISA 세대 교체와 portability 비용이 커진다.

## CHAPTER 03 · contiguous memory layout이 vector load/store의 기본 우호 조건이다

연속된 element를 순서대로 처리하면 vector load 한 번으로 여러 lane을 채울 수 있다. 반대로 각 element가 pointer를 따라 흩어져 있거나 구조체 내부 field 간격이 크면 gather가 필요하거나 scalar load 여러 개로 떨어질 수 있다. 따라서 vectorization은 arithmetic optimization이면서 동시에 data-layout optimization이다. Hot loop를 최적화할 때 source expression만 보지 말고 실제 address sequence를 그려야 한다. `연산 횟수가 같다`는 사실은 memory transaction 수, cache-line utilization, TLB pressure가 같다는 뜻이 아니다.

## CHAPTER 04 · alignment는 correctness requirement와 performance hint를 구분해야 한다

ISA와 instruction 종류에 따라 unaligned vector access가 지원될 수 있지만, alignment가 cache-line/page boundary와 겹치면 한 vector load가 여러 memory transaction으로 분리될 수 있다. 어떤 instruction은 stricter alignment contract를 가질 수 있고 compiler intrinsic도 aligned pointer assumption을 요구할 수 있다. 존재하지 않는 alignment를 compiler에 약속하면 optimization hint가 아니라 undefined behavior나 fault의 원인이 될 수 있다. Alignment를 개선할 때는 allocator 반환 alignment, array base, field offset, loop induction offset을 모두 확인해야 한다.

## CHAPTER 05 · gather/scatter는 불규칙 주소를 vector form으로 표현하지만 공짜가 아니다

Gather는 lane별 index를 사용해 여러 주소에서 element를 읽고 scatter는 여러 주소에 쓴다. 이는 pointer/index-heavy workload를 vector instruction으로 표현할 수 있게 하지만 contiguous load/store와 동일한 bandwidth 효율을 보장하지 않는다. Cache miss가 lane별로 분산되면 하나의 instruction이 여러 memory dependency를 기다릴 수 있고, address generation과 fault semantics도 복잡해진다. Gather가 존재한다는 사실만으로 array-of-pointers 구조가 vector-friendly가 되는 것은 아니다. Data layout을 바꿀 수 있다면 contiguous access와 gather 비용을 실제 benchmark로 비교해야 한다.

## CHAPTER 06 · predicate/mask는 branch를 없애지만 inactive lane 비용을 남길 수 있다

조건식이 lane마다 다르면 mask를 계산하고 active lane만 결과를 commit하는 predicated execution을 사용할 수 있다. Branch divergence를 scalar branch 없이 표현할 수 있지만 mask 생성, blend/select, inactive-lane execution, masked memory access 비용이 존재한다. 조건이 거의 항상 한쪽으로 치우치면 scalar branch predictor가 더 유리할 수도 있고, mask density가 낮으면 vector lane 대부분이 유효한 일을 하지 않을 수 있다. 따라서 `branch 제거 = vector 이득`이 아니라 branch predictability와 active-lane ratio를 같이 측정한다.

## CHAPTER 07 · tail 처리는 vector width로 나누어떨어지지 않는 반복의 correctness 문제다

Loop trip count가 vector lane 수의 배수가 아니면 마지막 일부 element를 안전하게 처리해야 한다. 전통적인 방식은 main vector loop 뒤 scalar epilogue를 두고, predicate-capable ISA는 tail mask로 마지막 vector를 처리할 수 있다. 잘못된 tail 구현은 배열 경계를 넘어 read/write하거나 padding byte를 실제 데이터로 오인할 수 있다. Compiler가 자동 vectorization을 적용할 때 생성되는 remainder loop나 masked tail을 assembly/IR에서 확인하면 performance anomaly를 설명하는 데 도움이 된다.

## CHAPTER 08 · reduction은 lane-independent loop보다 dependency가 강하다

합계, 최댓값, dot product 같은 reduction은 여러 iteration의 값을 하나의 accumulator에 결합한다. Scalar loop에서는 accumulator dependency chain이 존재하고, vectorized reduction은 lane별 partial accumulator를 만든 뒤 horizontal combine을 수행한다. 이 구조는 throughput을 높일 수 있지만 floating-point addition처럼 결합 순서가 결과 bit pattern에 영향을 주는 연산에서는 numerical semantics가 달라질 수 있다. Compiler가 reassociation을 허용하는 조건과 application tolerance를 구분해야 한다.

## CHAPTER 09 · horizontal operation은 lane 방향 data movement를 요구한다

Element-wise add/multiply는 lane 사이 교환이 거의 없지만 horizontal sum, min/max, prefix-like operation은 lane 값을 서로 결합해야 한다. Shuffle, permute, pairwise reduction instruction이 사용되며 이들은 execution port와 latency 특성이 일반 arithmetic과 다를 수 있다. `vector ALU throughput`만 보고 reduction 성능을 예측하면 틀릴 수 있다. Dependency depth와 shuffle network 비용을 포함한 critical path를 본다.

## CHAPTER 10 · widening과 narrowing은 overflow와 precision contract를 바꾼다

작은 integer lane을 더 큰 lane으로 확장해 계산하면 intermediate overflow를 줄일 수 있고, 반대로 결과를 좁은 타입으로 줄일 때 truncation·rounding·saturation 규칙이 필요하다. Image/audio/DSP workload는 8/16-bit 입력을 넓혀 누적한 뒤 다시 narrowing하는 패턴을 자주 사용한다. Widening은 lane 수를 줄여 vector parallelism을 낮추므로 precision과 throughput을 교환한다. Narrowing 단계에서 signedness와 rounding mode가 데이터 품질을 결정한다.

## CHAPTER 11 · saturating arithmetic은 wraparound와 다른 도메인 계약이다

일반 fixed-width integer overflow는 modular wraparound 또는 언어별 규칙을 따를 수 있지만 media processing에서는 최솟값/최댓값에 clamp하는 saturating arithmetic이 더 적절한 경우가 있다. 예를 들어 pixel channel에서 250+20을 14로 wrap시키는 것보다 255로 clamp하는 의미가 필요할 수 있다. SIMD ISA는 saturating add/subtract 같은 instruction을 제공할 수 있다. 중요한 것은 instruction 존재가 아니라 application의 수학적 domain이 wrap, checked overflow, saturation 중 무엇을 요구하는지 명시하는 것이다.

## CHAPTER 12 · shuffle/permute는 data layout mismatch를 register 내부에서 보정한다

Vector lane 순서를 바꾸거나 여러 vector에서 lane을 교차 조합하면 AoS 데이터를 SoA-like 계산 형태로 재배열할 수 있다. 그러나 shuffle은 공짜가 아니며 complex permute network는 arithmetic보다 높은 latency나 낮은 throughput을 가질 수 있다. Hot loop에서 load→shuffle 다수→arithmetic→shuffle→store 구조가 보인다면 source data layout이 vector engine과 맞지 않는 신호일 수 있다. Persistent data layout 변경 비용과 per-iteration shuffle 비용을 비교한다.

## CHAPTER 13 · register pressure는 vector width 확대의 숨은 한계다

더 넓은 vector와 aggressive unrolling은 동시에 살아 있는 temporary register 수를 늘린다. Physical/vector register resource를 초과하면 compiler가 값을 stack에 spill하고 reload해야 하며, 이 memory traffic이 vectorization 이득을 상쇄할 수 있다. Register pressure는 source variable 개수만으로 결정되지 않고 live range, inlining, unrolling, instruction scheduling과 연결된다. Optimization report와 assembly에서 spill load/store를 확인하고 vector width를 무조건 최대화하지 않는다.

## CHAPTER 14 · instruction latency와 reciprocal throughput은 다른 성능 값이다

Instruction 하나가 결과를 만들기까지 걸리는 latency와, pipeline이 충분히 채워졌을 때 cycle당 몇 instruction을 시작할 수 있는 throughput은 다르다. Independent vector operations가 많으면 latency를 겹쳐 throughput에 가까운 성능을 얻을 수 있지만, accumulator처럼 dependency chain이 있으면 latency가 critical path를 지배한다. Microbenchmark는 dependency 형태를 실제 workload와 맞춰야 한다. Independent operands만 반복한 benchmark로 dependent loop 성능을 예측하면 잘못된 결론을 낸다.

## CHAPTER 15 · execution port와 functional unit 경쟁이 vector throughput을 제한할 수 있다

Modern superscalar CPU는 여러 execution resource를 가지며 vector add, multiply, load/store, shuffle가 특정 port/resource를 공유할 수 있다. Source code에서 operation count가 적어 보여도 같은 execution port에 몰리면 dispatch/issue bottleneck이 생긴다. 반대로 서로 다른 resource를 사용하는 instruction을 적절히 섞으면 overlap이 가능하다. PMU counter와 static scheduling analysis를 조합해 frontend stall, execution-port pressure, memory stall을 분리한다.

## CHAPTER 16 · vector load가 cache line을 잘 쓰는지 확인해야 한다

Vector width가 커질수록 한 instruction이 더 많은 byte를 요구한다. Access가 cache-line boundary를 반복해서 가로지르거나 working set이 cache capacity를 넘으면 arithmetic throughput보다 memory hierarchy가 병목이 된다. Prefetcher가 sequential stream을 잘 따라오는지, read-for-ownership가 필요한 store인지, write-allocate traffic이 얼마나 생기는지도 영향을 준다. Vectorization으로 instruction 수가 줄어도 memory byte 수가 그대로면 bandwidth-bound workload의 speedup은 제한된다.

## CHAPTER 17 · AoS와 SoA는 vector lane에 들어가는 field 구성을 바꾼다

Array of Structures는 한 객체의 여러 field를 인접하게 두고, Structure of Arrays는 같은 field를 여러 객체에 걸쳐 연속 배치한다. 특정 field 하나만 대량 계산하는 loop에서는 SoA가 contiguous vector load에 유리할 수 있다. 반대로 객체 단위로 모든 field를 함께 사용하는 workload에서는 AoS가 cache locality에 더 적합할 수 있다. Data-oriented design은 vectorization 하나만 위한 규칙이 아니라 access pattern별 byte utilization을 최적화하는 문제다.

## CHAPTER 18 · alias 가능성은 compiler가 memory operation을 재배열할 수 있는지 결정한다

두 pointer가 같은 memory를 가리킬 가능성이 있으면 compiler는 load/store 순서를 자유롭게 바꾸기 어렵다. Alias analysis가 `겹치지 않는다`고 증명하면 vector loop를 더 공격적으로 만들 수 있다. Programmer annotation이나 restrict-like contract를 사용할 때는 실제 non-alias invariant가 반드시 참이어야 한다. 틀린 alias promise는 단순 성능 저하가 아니라 잘못된 code generation을 허용할 수 있다. Optimization remark에서 alias check와 runtime versioning 여부를 확인한다.

## CHAPTER 19 · loop-carried dependency는 iteration을 동시에 실행할 수 있는지 결정한다

현재 iteration의 결과가 다음 iteration 입력이 되면 단순 lane 병렬화가 불가능할 수 있다. Prefix sum, recurrence, state machine 형태가 대표적이다. 일부 dependency는 algorithm transformation으로 병렬 prefix/reduction 형태로 바꿀 수 있지만 수학적 의미와 overhead가 달라진다. Compiler가 vectorization을 포기한 이유가 `unknown dependency`인지 `proven recurrence`인지 구분한다. Source를 미세하게 바꾸기 전에 dependency graph를 그린다.

## CHAPTER 20 · auto-vectorizer는 legality와 profitability를 따로 판단한다

Compiler는 먼저 vector transformation이 program semantics를 보존하는지 판단하고, 그 다음 target cost model에서 이득이 있을지 평가한다. Legality가 통과해도 gather 비용, runtime alias check, small trip count, register pressure 때문에 profitability에서 거부할 수 있다. LLVM vectorizer는 target instruction cost와 loop structure를 이용해 width/interleave를 선택한다. Optimization remark를 읽으면 `왜 vectorize되지 않았는가`를 추측 대신 근거로 좁힐 수 있다.

## CHAPTER 21 · runtime versioning은 fast vector path와 safe scalar path를 함께 만들 수 있다

Compile time에 alias/alignment를 완전히 증명하지 못해도 runtime check를 삽입해 조건이 맞을 때 vector path, 아니면 scalar path를 선택할 수 있다. 이 전략은 applicability를 넓히지만 branch/check overhead와 code size를 늘린다. Hot loop trip count가 짧으면 check 비용이 vector body 이득보다 클 수 있다. Benchmark는 vector path가 실제 입력에서 얼마나 자주 선택되는지까지 측정해야 한다.

## CHAPTER 22 · branch-heavy loop는 if-conversion과 predication 가능성을 본다

작은 조건문은 mask/select로 바꾸어 vector lane을 유지할 수 있지만 branch body가 크거나 side effect가 많으면 predication이 비싸진다. Masked store/load가 fault suppression을 보장하는지, inactive lane에서도 computation이 발생하는지 ISA semantics를 확인해야 한다. Compiler는 branch probability와 cost model을 이용해 if-conversion을 결정한다. Data를 조건별로 partition해 branch 자체를 제거하는 algorithm redesign이 더 나을 수도 있다.

## CHAPTER 23 · scalar epilogue가 전체 성능을 지배하는 작은 배열도 있다

Vector loop startup, alignment/alias check, tail 처리는 고정 비용을 만든다. 배열 길이가 작으면 main vector body보다 prologue/epilogue가 상대적으로 커져 scalar implementation이 더 빠를 수 있다. Library가 다양한 input size를 받는다면 하나의 benchmark 크기로 결론내리지 않는다. Distribution별 latency를 측정하고 threshold 기반 dispatch를 고려한다.

## CHAPTER 24 · vector ABI는 함수 경계에서 register와 data layout을 규정한다

Vector type을 함수 parameter/return으로 노출하면 어떤 register class를 사용하는지, stack alignment가 무엇인지, caller/callee-saved rule이 어떤지 ABI와 연결된다. ISA extension이 다른 binary 사이에서 unsupported instruction이나 calling convention mismatch가 생기지 않도록 build target과 runtime dispatch를 관리해야 한다. Public ABI에 architecture-specific vector type을 박아 넣으면 portability와 binary compatibility 비용이 커질 수 있다.

## CHAPTER 25 · intrinsic은 compiler를 우회하는 assembly가 아니라 typed instruction interface다

Intrinsic은 특정 vector operation을 source에서 직접 표현하지만 register allocation, instruction scheduling, constant folding 같은 많은 일은 여전히 compiler가 수행한다. Intrinsic을 사용한다고 최적 code가 자동 보장되지 않는다. Target-specific code가 늘어나면 portability, testing matrix, future compiler improvement 활용이 어려워질 수 있다. Auto-vectorization이 실패하는 원인을 확인한 뒤 필요한 hotspot에 제한적으로 사용한다.

## CHAPTER 26 · runtime CPU feature dispatch는 binary 하나로 여러 ISA generation을 지원한다

배포 binary가 baseline ISA만 요구하면서도 newer CPU에서 확장 vector instruction을 사용하려면 function multiversioning이나 runtime feature detection을 사용할 수 있다. Dispatch result를 cache하고 hot path에서 feature check를 반복하지 않도록 설계할 수 있다. 그러나 build artifact마다 실제 target feature set과 fallback path가 존재하는지 검증해야 한다. 특정 lab machine에서만 실행되는 binary를 production 호환이라고 착각하면 안 된다.

## CHAPTER 27 · vectorization은 floating-point operation order를 바꿀 수 있다

Reduction tree, FMA 사용, reassociation은 scalar source와 다른 rounding sequence를 만들 수 있다. Strict IEEE semantics가 필요하면 compiler option과 transformation이 제한될 수 있고, fast-math 계열 contract를 허용하면 더 공격적인 vectorization이 가능해진다. 성능 요구와 numerical error budget을 문서화하지 않은 채 compiler flag만 바꾸면 regression 판정 기준이 사라진다. P35의 floating-point error analysis와 연결해 tolerance를 정의한다.

## CHAPTER 28 · bandwidth-bound workload에는 Roofline 관점이 유용하다

Operation 대비 memory byte 비율이 낮으면 vector ALU를 더 넓혀도 memory bandwidth ceiling 때문에 성능이 오르지 않는다. Arithmetic intensity를 계산하고 achieved bandwidth·compute throughput을 측정하면 compute-bound와 bandwidth-bound를 구분할 수 있다. Cache reuse를 늘려 effective byte traffic을 줄이거나 data representation을 압축하는 최적화가 vector width 확대보다 큰 효과를 낼 수 있다.

## CHAPTER 29 · SIMD benchmark는 thermal, frequency, alignment, dispatch를 통제해야 한다

짧은 benchmark는 turbo frequency에서 끝나고 장시간 workload는 thermal/power limit에 걸릴 수 있다. Input alignment, size, cache warmness, branch/mask distribution, CPU affinity, compiler flags, ISA dispatch path가 다르면 비교가 무의미하다. Wall-clock 하나만 기록하지 말고 cycles, instructions, vector instruction mix, cache misses, bandwidth, frequency 상태를 함께 본다. P21의 thermal state와 P26의 PMU/tracing evidence를 연결한다.

## CHAPTER 30 · vector optimization의 계약은 semantics·layout·hardware·compiler 네 층을 동시에 고정한다

검증 가능한 vector 최적화는 먼저 scalar reference semantics와 numerical tolerance를 고정한다. 이어서 non-alias·alignment·bounds 같은 memory invariant, target ISA와 runtime dispatch, compiler version/flags를 기록한다. 마지막으로 representative data distribution에서 correctness differential test와 performance counter를 함께 비교한다. `vector instruction이 생성됐다`는 사실은 성공 조건이 아니다. 결과 정확성, tail safety, portability, sustained throughput이 모두 만족될 때만 최적화가 성립한다.