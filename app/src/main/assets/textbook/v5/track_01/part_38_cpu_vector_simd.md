# PART 38 · CPU Vector/SIMD — lanes, predicates, reductions, vector ABI

SIMD 최적화는 “한 instruction으로 여러 값을 계산한다”보다 더 복잡하다. data layout, alignment, alias, dependency, tail 처리와 reduction order가 맞아야 vector width를 실제 throughput으로 바꿀 수 있다. 이 PART는 **lane utilization, memory access, compiler cost model, ABI와 수치 semantics**를 연결한다.

---

## CHAPTER 01 · vector lane은 한 instruction 안의 독립 element operation을 표현한다

SIMD register는 여러 scalar element를 lane으로 담아 동일 계열 operation을 병렬 적용한다. lane 수는 element width와 ISA에 따라 달라지므로 `vector = 4개` 같은 가정을 algorithm에 박아 두면 portability가 깨진다. compiler는 scalar loop를 vector lane에 mapping할 때 dependency와 memory safety를 먼저 증명한다.

모든 lane이 useful work를 하지 않으면 theoretical width만큼 speedup이 나오지 않는다. branch mask, tail element, sparse access가 inactive lane을 늘릴 수 있다.

profile에서는 vector instruction count와 active element 비율을 같이 본다. source loop가 vectorized됐다는 report만으로 end-to-end throughput 향상을 결론내리지 않는다.

---

## CHAPTER 02 · fixed-width와 scalable vector는 code generation 전략이 다르다

일부 ISA는 고정 lane 수의 vector를 제공하고, scalable vector architecture는 runtime hardware width에 맞춰 predicate와 loop를 구성하도록 설계된다. scalable model에서 lane count를 compile-time 상수로 가정하면 코드는 특정 implementation에 묶인다.

vector-length agnostic loop는 현재 active predicate를 기반으로 chunk를 처리하고 remaining element를 반복한다. 이는 tail을 자연스럽게 처리할 수 있지만 control 구조가 scalar intuition과 달라진다.

테스트는 여러 가상 vector length를 사용해 width assumption을 찾는다. ABI boundary에서 scalable vector type을 어떻게 전달하는지도 platform 문서를 기준으로 한다.

---

## CHAPTER 03 · contiguous layout은 vector load/store가 bandwidth를 효율적으로 쓰게 한다

인접 element를 연속 memory에 배치하면 한 vector load가 여러 useful 값을 가져올 수 있고 hardware prefetch와 cache line 활용도 좋아진다. pointer chain이나 object별 heap allocation은 lane마다 다른 address를 만들며 vectorization 이점을 줄인다.

data structure를 SoA로 바꾸면 한 field를 대량 처리하는 kernel에 유리하지만 객체 단위 접근에는 cache locality가 나빠질 수 있다. layout은 workload operation mix와 함께 선택해야 한다.

requested element 수와 실제 cache-line traffic을 비교한다. vector width를 늘렸는데 bandwidth가 더 빨리 포화되면 layout과 reuse를 다시 본다.

---

## CHAPTER 04 · alignment는 vector access의 transaction 수와 legality에 영향을 준다

vector load의 base address가 자연 alignment를 만족하면 hardware가 적은 memory operation으로 처리하기 쉽다. unaligned access를 지원하더라도 cache-line/page 경계를 가로지르면 추가 transaction이나 penalty가 생길 수 있다. 일부 intrinsic은 stricter alignment를 contract로 요구한다.

allocator가 aligned pointer를 줘도 slice offset이 alignment를 깨뜨릴 수 있다. struct field와 interleaved array에서도 stride가 vector boundary와 어긋날 수 있다.

critical path에서는 address alignment를 runtime assertion과 benchmark로 검증한다. platform별 penalty가 다르므로 문법 규칙처럼 일반화하지 않는다.

---

## CHAPTER 05 · gather와 scatter는 비연속 access를 vector form으로 표현하지만 비용이 크다

gather는 lane별 index에서 값을 모으고 scatter는 lane별 address에 쓴다. 이는 scalar loop를 vectorize할 수 있게 하지만 contiguous load보다 address generation과 cache transaction이 훨씬 비쌀 수 있다. index collision이 있으면 scatter ordering semantics도 주의해야 한다.

random index가 넓은 working set을 건드리면 vector width가 늘수록 동시에 더 많은 cache miss를 만들 뿐 useful bandwidth가 증가하지 않을 수 있다.

benchmark에서는 contiguous, strided, random access를 분리한다. gather 사용 자체를 성공으로 보지 않고 scalar 대비 실제 latency·bandwidth를 측정한다.

---

## CHAPTER 06 · predication은 lane별 조건을 mask로 바꿔 control-flow를 단순화한다

predicate mask는 각 lane의 operation 적용 여부를 제어해 branch가 있는 loop를 vectorized form으로 바꿀 수 있다. 하지만 masked-off lane도 일부 instruction issue 자원을 소비할 수 있고, memory operation의 fault-suppression semantics는 ISA마다 확인해야 한다.

조건이 거의 모두 true인 경우 predication이 효율적일 수 있지만 절반 이하 lane만 active라면 wasted work가 커질 수 있다. branch partitioning이 더 나은 경우도 있다.

mask density와 instruction count를 profile한다. input distribution이 달라질 때 vector efficiency가 급변하는지 production workload로 확인한다.

---

## CHAPTER 07 · tail 처리는 vector width에 맞지 않는 남은 element의 correctness 문제다

array length가 vector width의 배수가 아니면 마지막 partial chunk를 처리해야 한다. scalar epilogue, masked vector, padded storage 등 여러 방법이 있다. boundary check를 빼고 full-width load를 하면 object 끝을 넘어 접근할 수 있어 보안 문제로 이어진다.

padding을 사용하면 allocator와 serialization이 실제 extra bytes를 보장해야 한다. masked load도 ISA가 inactive lane의 memory fault를 어떻게 다루는지 확인한다.

length 0, width-1, width, width+1 같은 boundary case를 테스트한다. 성능 benchmark에는 tiny array도 포함해 tail overhead를 본다.

---

## CHAPTER 08 · reduction은 lane 값을 하나로 합치며 dependency tree를 만든다

sum, min, max 같은 reduction은 vector lane을 마지막에 하나의 scalar로 합쳐야 한다. tree reduction은 serial chain보다 dependency depth를 줄일 수 있지만 floating-point에서는 합산 순서를 바꿔 결과가 달라질 수 있다.

parallel reduction을 여러 thread까지 확장하면 partial result merge order가 추가된다. deterministic result가 요구되면 fixed tree나 compensated scheme을 선택해야 할 수 있다.

throughput과 numerical error를 같이 측정한다. integer overflow semantics도 reduction contract에 포함한다.

---

## CHAPTER 09 · horizontal operation은 vector 내부 lane 사이 data 이동을 요구한다

horizontal add/max는 각 lane 독립 operation과 달리 lane 간 shuffle과 combine을 필요로 한다. ISA에 따라 dedicated instruction이 있어도 latency와 throughput이 일반 arithmetic보다 다를 수 있다.

매 iteration마다 horizontal reduction을 하면 vector parallelism을 자주 깨뜨린다. 여러 vector accumulator를 유지한 뒤 loop 끝에서 한 번 합치는 구조가 더 나을 수 있다.

assembly에서 shuffle/horizontal instruction 비중을 확인한다. reduction frequency를 줄였을 때 register pressure가 과도하게 증가하지 않는지도 본다.

---

## CHAPTER 10 · widen과 narrow는 overflow·precision boundary를 명시한다

작은 integer element를 넓은 type으로 확장해 계산하면 intermediate overflow를 피할 수 있고, 결과를 다시 좁힐 때 truncate 또는 saturation semantics를 선택해야 한다. signed/unsigned extension을 잘못 고르면 bit pattern은 같아도 numerical meaning이 바뀐다.

image/audio kernel에서 8-bit input을 16/32-bit accumulator로 처리하는 이유가 여기에 있다. narrow 전에 range proof가 없으면 silent wrap이 발생할 수 있다.

worst-case input으로 accumulator bound를 검증한다. intrinsic 이름보다 실제 signedness와 rounding behavior를 확인한다.

---

## CHAPTER 11 · saturation arithmetic은 overflow를 endpoint에 고정한다

saturating add/sub는 range를 넘는 결과를 wrap시키지 않고 minimum/maximum value에 clamp한다. media signal에는 유용하지만 일반 integer arithmetic과 결과가 달라 algorithm이 이를 전제로 해야 한다.

중간 계산을 너무 일찍 saturate하면 final result가 high-precision computation과 다를 수 있다. 어느 stage에서 clamp할지 numerical contract를 정한다.

boundary ±1 around min/max를 테스트한다. scalar fallback과 SIMD path가 동일 saturation rule을 사용하는지 비교한다.

---

## CHAPTER 12 · shuffle은 lane rearrangement를 통해 data layout을 runtime에 바꾼다

shuffle/permutation instruction은 vector 내부 또는 여러 vector 사이 element 순서를 바꿔 transpose, interleave, reduction을 구현한다. 그러나 복잡한 shuffle network는 arithmetic보다 더 큰 latency와 port pressure를 만들 수 있다.

source layout이 계속 shuffle을 요구한다면 upstream data representation을 바꾸는 편이 더 효율적일 수 있다. compiler가 constant permutation을 최적 instruction으로 낮추는지도 target별로 다르다.

shuffle instruction 수와 dependency를 profile한다. data layout 변경 전후 total memory traffic까지 비교한다.

---

## CHAPTER 13 · vectorization은 register pressure를 늘려 spill을 유발할 수 있다

wide vector register는 한 번에 많은 값을 보관하지만 여러 accumulator와 mask를 동시에 유지하면 physical register가 부족해질 수 있다. spill은 vector register 전체를 memory에 저장해 큰 traffic을 만든다.

unrolling과 vectorization을 함께 강하게 적용하면 throughput이 좋아지기보다 stack/local traffic이 늘 수 있다. compiler cost model이 vector factor를 제한하는 이유다.

register allocation report와 spill load/store를 확인한다. vector width를 줄였을 때 오히려 성능이 좋아지는 경우 pressure가 원인일 수 있다.

---

## CHAPTER 14 · vector instruction도 latency와 throughput을 분리해 봐야 한다

한 vector instruction의 결과가 dependent consumer에 준비되기까지 latency가 있고 independent instruction을 얼마나 자주 issue할 수 있는지 throughput이 따로 존재한다. width가 두 배라고 latency가 절반이 되는 것은 아니다.

critical dependency chain에서는 더 넓은 vector가 clock/frequency나 execution unit occupancy 때문에 이득이 제한될 수 있다. independent work가 많으면 throughput 측면에서 유리하다.

microbenchmark는 dependency chain과 independent stream을 분리해 측정한다. production hotspot의 dependency graph와 연결한다.

---

## CHAPTER 15 · port pressure는 특정 vector operation이 execution resource를 독점할 때 생긴다

CPU backend에는 load/store, integer, FP, shuffle 같은 operation을 처리하는 execution port·unit이 제한되어 있다. 모든 instruction이 서로 다른 자원을 사용하는 것은 아니므로 같은 종류 vector op가 몰리면 해당 port가 bottleneck이 된다.

gather, divide, shuffle처럼 비싼 operation은 instruction count가 적어도 resource occupancy가 길 수 있다. 단순 opcode 수 최적화는 원인을 놓친다.

PMU backend stall과 generated instruction을 함께 본다. 계산과 load를 다른 port에 분산할 여지가 있는지 cost model로 검토한다.

---

## CHAPTER 16 · cache line은 vector width보다 큰 memory transfer 단위일 수 있다

CPU cache는 vector register 크기와 무관하게 cache-line 단위로 memory를 가져온다. vector가 line 경계를 자주 가로지르면 두 line을 접근하고, false sharing이 있으면 다른 core write 때문에 line이 invalidated된다.

vectorizing store가 한 번에 더 넓은 range를 건드려 unrelated field까지 같은 line에서 공유될 수 있다. alignment와 data partition을 동시에 설계해야 한다.

line utilization과 cache miss, cache-to-cache transfer를 본다. vector speedup이 multi-thread에서만 사라지면 coherence 문제를 의심한다.

---

## CHAPTER 17 · AoS와 SoA는 access axis에 따라 vector friendliness가 달라진다

Array of Structures는 object별 field가 가까워 객체 하나를 모두 읽을 때 유리하고, Structure of Arrays는 동일 field가 연속되어 많은 object의 한 속성을 vector 처리하기 좋다. 어느 layout이 좋은지는 operation이 object 중심인지 field 중심인지에 달려 있다.

SoA 변환은 API와 serialization 비용을 바꾸고 여러 field를 동시에 쓰는 code에서는 오히려 cache line 수를 늘릴 수 있다. hybrid AoSoA가 절충안이 되기도 한다.

production access pattern으로 cache miss와 vectorization report를 측정한다. benchmark용 artificial scan만으로 전체 layout을 결정하지 않는다.

---

## CHAPTER 18 · alias uncertainty는 compiler가 vector memory reordering을 막게 한다

두 pointer가 overlap할 수 있으면 compiler는 vector load/store를 재배치했을 때 semantics가 깨질 가능성을 고려해야 한다. NoAlias proof, restrict-like contract, runtime overlap check가 있으면 더 공격적인 vectorization이 가능하다.

잘못된 non-alias assertion은 optimizer에게 실제 dependency를 무시할 권한을 줘 corruption을 만든다. performance hint가 곧 correctness claim이다.

vectorization remark에서 alias 때문에 거부됐는지 확인한다. adversarial overlapping input을 test해 annotation이 진짜 API contract인지 검증한다.

---

## CHAPTER 19 · loop-carried dependency는 iteration 사이 병렬화를 제한한다

현재 iteration 결과를 다음 iteration이 필요로 하면 여러 iteration을 동시에 vector lane에 배치하기 어렵다. prefix sum, recurrence가 대표적이다. dependency distance와 associativity에 따라 scan algorithm이나 block transform으로 구조를 바꿀 수 있다.

floating-point reduction처럼 수학적으로 associative해 보여도 finite precision에서는 operation reorder가 result를 바꾼다. compiler가 어떤 freedom을 갖는지 language semantics와 fast-math flag를 본다.

loop dependence report와 actual output drift를 함께 확인한다. dependency를 무시한 forced vectorization은 금지한다.

---

## CHAPTER 20 · cost model은 vectorization legality와 profitability를 구분한다

compiler는 vectorization이 semantics를 보존해도 실제 target에서 이득이 적으면 scalar path를 선택할 수 있다. gather cost, trip count, runtime check, epilogue, register pressure가 모두 profitability에 들어간다.

“vectorized되지 않았다”가 compiler bug라는 뜻은 아니다. small loop나 cold path에서는 setup overhead가 더 크다. 반대로 profile 정보가 잘못되면 hot loop가 낮은 우선순위를 받을 수 있다.

optimization remark에서 legality failure와 cost rejection을 분리한다. target CPU와 profile을 정확히 설정한 뒤 다시 평가한다.

---

## CHAPTER 21 · runtime versioning은 fast vector path와 safe fallback을 동시에 제공한다

alias나 alignment를 compile time에 확정하지 못해도 runtime check로 조건을 검사한 뒤 vector fast path와 scalar fallback 중 하나를 선택할 수 있다. 이는 API를 넓게 유지하면서 common case를 최적화하는 방법이다.

check 자체와 code duplication이 overhead·I-cache footprint를 늘린다. 조건이 거의 항상 실패하면 vector path가 존재해도 이득이 없다.

production에서 fast-path hit ratio를 측정한다. test는 두 path가 동일 semantics를 갖는지 각각 실행한다.

---

## CHAPTER 22 · if-conversion은 branch를 predicate로 바꿔 vector lane을 유지한다

loop 내부 짧은 branch는 mask select와 conditional operation으로 변환해 lane divergence 없이 실행할 수 있다. 하지만 양쪽 path computation이 비싸거나 fault 가능 memory access가 있으면 predication이 안전하거나 수익성 있는 선택이 아닐 수 있다.

조건 density가 extreme하면 scalar branch가 더 빠를 수도 있다. compiler가 profile을 활용해 선택할 수 있다.

branch miss와 masked instruction 수를 동시에 본다. source에서 `if`를 수동으로 없애는 것보다 generated code를 확인한다.

---

## CHAPTER 23 · scalar epilogue는 tail element를 처리하는 일반적인 fallback이다

vector loop가 full-width chunk를 처리한 뒤 남은 element를 scalar loop가 처리할 수 있다. trip count가 작으면 epilogue 비중이 커져 vectorization setup 이득이 사라진다.

multiple vector width version이 있으면 각 path마다 epilogue가 생겨 code size가 증가한다. masked tail이 가능한 ISA에서는 scalar epilogue를 줄일 수 있다.

length distribution을 benchmark input에 반영한다. 항상 큰 array만 측정하지 않는다.

---

## CHAPTER 24 · vector ABI는 function boundary에서 register와 type 전달 규칙을 정한다

vector type을 함수 argument·return으로 전달할 때 어떤 register를 사용하고 stack alignment가 무엇인지 ABI가 정의한다. compiler·FFI가 다른 convention을 사용하면 lane data가 깨진다.

ISA extension별 vector width가 다르면 public ABI에 concrete vector type을 노출하는 것이 compatibility 문제를 만들 수 있다. scalable vector는 별도 calling rule이 필요할 수 있다.

cross-language boundary에서는 header source보다 actual ABI와 target feature를 검증한다. symbol만 link된다고 correctness가 보장되지 않는다.

---

## CHAPTER 25 · intrinsic은 compiler에게 target operation 의도를 명시하지만 portability를 줄인다

intrinsic은 특정 vector instruction이나 semantic primitive를 source에서 직접 표현할 수 있게 한다. auto-vectorizer가 못 찾는 pattern을 최적화할 수 있지만 ISA별 code path와 feature detection을 관리해야 한다.

intrinsic을 assembly opcode로 단정하면 compiler가 다른 instruction sequence로 lower할 수 있는 가능성을 놓친다. alignment와 lane semantics도 intrinsic contract를 따라야 한다.

scalar reference implementation을 유지하고 differential test를 한다. performance gain이 없는 intrinsic은 유지보수 비용만 늘릴 수 있다.

---

## CHAPTER 26 · runtime dispatch는 CPU feature에 맞는 implementation을 선택한다

한 binary가 여러 CPU 세대를 지원하면 startup 또는 call site에서 available SIMD extension을 탐지해 최적 구현을 선택할 수 있다. dispatch가 없으면 newest ISA를 사용한 binary가 old CPU에서 invalid instruction으로 실패한다.

feature bit만 보고 OS save/restore support나 deployment policy를 무시하면 안 된다. container/VM에서 exposed feature가 host migration compatibility와 연결되기도 한다.

선택된 implementation을 telemetry에 남긴다. CI에서 scalar·각 ISA path를 강제로 실행해 dead code가 되지 않게 한다.

---

## CHAPTER 27 · FP vectorization은 operation reorder로 numerical result를 바꿀 수 있다

vector reduction과 FMA contraction은 scalar source와 다른 floating-point evaluation order를 만들 수 있다. strict semantics에서는 compiler freedom이 제한되고 fast-math에서는 더 넓은 reorder가 허용된다.

performance를 위해 fast mode를 켤 때 bitwise reproducibility와 NaN/signed-zero handling이 어떻게 바뀌는지 명시해야 한다.

vector/scalar output을 domain tolerance로 비교한다. last-bit 차이와 실제 business error를 구분한다.

---

## CHAPTER 28 · roofline 관점은 compute와 memory 중 어느 자원이 상한인지 구분한다

arithmetic intensity가 낮은 loop는 SIMD width를 늘려도 memory bandwidth가 먼저 포화될 수 있다. intensity가 높고 data가 cache에 맞으면 execution throughput이 병목이 된다. vectorization은 machine balance 안에서만 speedup을 낸다.

bytes moved를 계산하지 않고 FLOPS만 최적화하면 memory-bound kernel에서 효과가 작다. data compression이나 blocking이 더 큰 이득을 줄 수 있다.

achieved bandwidth와 vector operation rate를 함께 측정한다. theoretical peak 대비 어느 roof에 가까운지 본다.

---

## CHAPTER 29 · SIMD benchmark는 frequency, alignment, input size를 통제해야 한다

wide vector instruction은 CPU 종류에 따라 frequency/power behavior에 영향을 줄 수 있고 thermal state가 장시간 성능을 바꾼다. small hot-cache benchmark와 large streaming benchmark는 전혀 다른 bottleneck을 측정한다.

compiler가 benchmark 계산을 제거하지 않게 result를 소비하고 warmup·pinning을 관리한다. alignment가 우연히 좋은 한 run만 비교하지 않는다.

latency distribution, cycles/element, bandwidth를 함께 기록한다. target CPU model과 active implementation을 결과에 포함한다.

---

## CHAPTER 30 · vector contract는 layout, alias, numerical semantics, fallback을 명시한다

SIMD path를 안전하게 운영하려면 buffer alignment와 element type, overlap 가능성, tail 처리, supported ISA, floating-point reorder 허용 범위를 문서화해야 한다. fast path가 조건을 만족하지 못하면 정확한 scalar 또는 다른 vector fallback이 있어야 한다.

최적화는 widest instruction을 고르는 작업이 아니다. cache, port, register pressure, memory bandwidth 중 실제 병목을 evidence로 확인한 뒤 vector factor와 layout을 선택한다.

release gate에서는 여러 길이·alignment·CPU feature·alias case를 실행해 **scalar reference와 같은 observable contract를 유지하면서 실제 throughput이 개선되는지** 검증한다.
