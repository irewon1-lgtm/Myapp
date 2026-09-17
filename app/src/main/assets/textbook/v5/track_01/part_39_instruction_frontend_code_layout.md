# PART 39 · Instruction Frontend and Code Layout — fetch, decode, I-cache, BTB

CPU backend가 아무리 넓어도 frontend가 충분한 instruction을 공급하지 못하면 실행 자원은 빈다. code size, instruction-cache locality, branch prediction, BTB와 code placement가 실제 fetch bandwidth를 결정한다. 이 PART는 **instruction delivery, control prediction, binary layout, JIT patching과 measurement**를 하나의 성능 계약으로 다룬다.

---

## CHAPTER 01 · frontend-bound는 execution unit이 아니라 instruction 공급이 상한인 상태다

CPU pipeline 앞단은 다음 instruction 주소를 예측하고 cache에서 bytes를 가져와 decode·uop 형태로 backend에 공급한다. backend execution unit이 놀고 있는데 fetch/decode가 충분히 못 따라가면 frontend-bound가 된다. source-level arithmetic을 줄여도 병목이 그대로일 수 있다.

large binary, branch-heavy code, I-cache miss, decode-width 제한이 원인이 될 수 있다. IPC가 낮다는 숫자만으로 memory data access를 의심하면 wrong layer를 최적화한다.

PMU frontend stall과 instruction-cache/BTB event를 함께 본다. hotspot의 on-CPU time과 code footprint를 연결해 실제 공급 병목인지 확인한다.

---

## CHAPTER 02 · instruction fetch도 memory hierarchy를 통과한다

code bytes는 memory에 있고 instruction cache와 TLB를 거쳐 frontend에 전달된다. cold code page가 fault나면 storage/page-cache latency까지 startup path에 들어온다. data cache만 관리하고 code locality를 무시하면 large application의 startup과 tail latency를 설명할 수 없다.

code가 여러 shared library와 page에 흩어져 있으면 control flow가 자주 page boundary를 넘고 fetch locality가 나빠진다. hot path를 compact하게 두는 layout optimization이 유효한 이유다.

I-cache miss, ITLB miss, code-page fault를 따로 측정한다. same function도 hot/cold run에서 비용이 달라질 수 있다.

---

## CHAPTER 03 · I-cache capacity는 hot code working set에 대한 상한을 만든다

instruction cache는 제한된 line 수만 유지한다. hot request path가 너무 많은 inlined function과 rare error path를 포함하면 useful code가 서로 eviction할 수 있다. code size 증가는 storage 문제만이 아니라 execution latency 문제다.

aggressive inlining, template expansion, generated code가 I-cache pressure를 높일 수 있다. 반대로 function call을 줄인 이득보다 footprint 손실이 더 클 수 있다.

binary map과 sampled I-cache miss를 함께 본다. hot code를 별도 section에 모은 뒤 miss와 p99가 같이 줄어드는지 검증한다.

---

## CHAPTER 04 · ITLB는 code page translation working set을 제한한다

instruction fetch는 virtual code address를 physical page로 translation해야 하고 ITLB가 최근 translation을 cache한다. hot code가 많은 page에 흩어지면 ITLB miss와 page walk가 frontend를 막을 수 있다. I-cache hit여도 translation miss는 별도 비용이다.

large page mapping은 ITLB reach를 늘릴 수 있지만 code sharing, ASLR, memory waste와 trade-off가 있다. tiny function을 여러 shared object에 흩뜨리는 layout도 page count를 늘린다.

ITLB event와 code page count를 측정한다. page-level layout을 바꾼 뒤 instruction miss와 translation miss를 분리해 비교한다.

---

## CHAPTER 05 · variable-length decode는 instruction boundary 찾기와 decode bandwidth를 복잡하게 만든다

일부 ISA는 instruction 길이가 다양해 fetch bytes에서 instruction boundary를 찾아 여러 decoder에 공급해야 한다. alignment와 instruction mix가 decode efficiency에 영향을 줄 수 있다. source instruction 수와 decoder uop 수가 직접 일치하지 않을 수 있다.

complex instruction이 여러 internal uop으로 분해되면 frontend bandwidth를 더 소비할 수 있다. 반대로 fusion이 가능한 pair는 실제 backend 전달 uop 수를 줄일 수 있다.

hardware counter에서 decoded uop와 retired instruction을 구분한다. assembly를 볼 때 opcode 개수만 세지 않는다.

---

## CHAPTER 06 · decode width는 cycle당 backend에 공급 가능한 work를 제한한다

frontend decoder는 한 cycle에 처리할 수 있는 instruction/uop 수가 제한된다. branch와 boundary가 좋지 않으면 nominal width를 모두 사용하지 못할 수 있다. backend가 넓어도 decoder가 공급하지 못하면 peak issue에 도달하지 않는다.

loop body가 uop cache에 맞는 경우 decode bottleneck을 우회할 수 있지만 large/unpredictable code에서는 다시 decoder path가 중요해진다.

frontend delivery rate와 backend utilization을 함께 본다. 특정 optimization이 instruction 수를 줄였는데 cycles가 그대로면 decode가 이미 bottleneck이 아니었을 수 있다.

---

## CHAPTER 07 · uop cache는 decoded instruction을 재사용해 frontend 비용을 줄인다

일부 CPU는 최근 decoded uop를 cache해 반복 loop가 decoder를 다시 통과하지 않게 한다. hot loop가 cache capacity·alignment에 잘 맞으면 높은 공급률을 얻지만 code size가 커져 footprint를 넘으면 hit benefit이 급락할 수 있다.

작은 source change로 loop가 cache boundary를 넘으면 disproportionate regression이 나타날 수 있다. microarchitecture별 capacity가 다르므로 hard-coded threshold로 일반화하지 않는다.

uop-cache hit/miss counter와 code-size diff를 함께 분석한다. architecture generation별 benchmark를 유지한다.

---

## CHAPTER 08 · branch prediction은 다음 fetch address를 미리 선택한다

conditional branch 결과를 기다린 뒤 fetch를 시작하면 pipeline이 자주 비기 때문에 CPU는 history를 사용해 방향을 예측한다. 예측이 틀리면 wrong-path work를 폐기하고 correct path를 다시 가져와야 한다.

branch count보다 predictability가 중요하다. 거의 항상 같은 방향인 branch는 싸고 data-dependent random branch는 비쌀 수 있다. branchless transformation도 더 많은 arithmetic·memory를 실행하면 손해일 수 있다.

branch miss rate를 input distribution과 연결한다. synthetic 균등 input과 production skew가 다르면 benchmark 결과도 달라진다.

---

## CHAPTER 09 · BTB는 branch target address를 cache해 fetch를 빠르게 전환한다

Branch Target Buffer는 이전 branch의 target을 기억해 instruction bytes가 decode되기 전에 다음 fetch address를 제공할 수 있다. 많은 branch가 같은 BTB set/capacity를 경쟁하면 target miss가 늘어 frontend bubble이 생길 수 있다.

large codebase의 여러 hot function이 unfavorable address에 배치되면 BTB aliasing이 생길 수 있다. code layout 변화만으로 branch behavior가 달라지는 이유 중 하나다.

BTB 관련 counter가 가능한 platform에서는 layout 전후를 비교한다. random address ASLR variance도 여러 run에서 분포로 본다.

---

## CHAPTER 10 · indirect branch는 여러 target 후보 때문에 prediction이 더 어렵다

virtual dispatch, function pointer, switch table은 indirect target을 runtime에 선택한다. call site가 여러 target을 자주 오가면 predictor accuracy가 낮아질 수 있다. type profile로 devirtualization을 하면 direct branch와 inlining 기회가 생긴다.

polymorphic workload를 monomorphic benchmark로 평가하면 실제 branch 비용을 과소평가한다. security mitigation도 indirect predictor behavior와 performance에 영향을 줄 수 있다.

target distribution을 profile하고 hot call site의 entropy를 본다. source abstraction을 없애기 전에 compiler devirtualization 여부를 확인한다.

---

## CHAPTER 11 · return prediction은 call/return nesting을 이용하지만 imbalance에 취약하다

CPU는 return address stack 같은 구조로 function return target을 예측할 수 있다. 비정상 control flow, deep nesting, mismatched call/return, coroutine/JIT trampoline이 predictor capacity와 assumption을 흔들 수 있다.

normal call chain에서는 매우 정확해 return branch cost가 작지만 stack unwinding이나 security mitigation path에서는 behavior가 달라질 수 있다.

return-mispredict counter가 지원되면 exception-heavy workload와 normal workload를 비교한다. function-call overhead를 일률적으로 같은 비용으로 두지 않는다.

---

## CHAPTER 12 · code alignment는 fetch block과 branch target 위치를 바꾼다

hot loop와 branch target을 cache-line/fetch boundary에 맞추면 한 cycle에 더 유리한 fetch·decode가 가능할 수 있다. 그러나 모든 function을 크게 align하면 padding으로 binary size와 I-cache footprint가 증가한다.

alignment option은 target microarchitecture와 hotspot에 선택적으로 적용해야 한다. ASLR이나 linker relaxation이 final address를 바꿀 수 있어 source attribute만 보고 실제 layout을 확정하지 않는다.

final binary address와 performance counter를 같이 본다. alignment 변화가 code size를 얼마나 늘렸는지도 기록한다.

---

## CHAPTER 13 · basic-block layout은 likely control flow를 contiguous code로 만든다

profile을 이용해 자주 이어지는 block을 가까이 배치하면 taken branch와 I-cache line 전환을 줄일 수 있다. rare error block을 hot path 중간에서 분리하면 sequential fetch efficiency가 좋아질 수 있다.

source order가 execution frequency를 반영하지 않을 수 있어 post-link optimizer가 final binary에서 다시 배치하기도 한다. exception table·debug/unwind metadata도 주소 변화에 맞춰 업데이트되어야 한다.

profile hot edge와 final layout을 시각화한다. block move 뒤 correctness뿐 아니라 symbolization도 검증한다.

---

## CHAPTER 14 · hot/cold splitting은 rare code가 hot cache를 오염시키지 않게 한다

error handling, logging, uncommon branch를 cold section으로 이동하면 common path의 code footprint를 줄일 수 있다. 하지만 cold path가 실제 production에서는 자주 실행되면 page fault와 distant branch 비용이 커진다.

profile representativeness가 핵심이다. 정상 트래픽만 학습한 PGO가 incident mode code를 지나치게 멀리 배치할 수도 있다.

hot/cold execution count를 release 후 관찰한다. cold path latency 요구가 있는 control-plane code는 별도 기준을 둔다.

---

## CHAPTER 15 · inlining은 call 제거보다 code footprint 확대 효과를 같이 봐야 한다

inlining은 callee 정보를 caller optimization에 열어 주지만 여러 call site에 body를 복제해 binary를 키운다. hot small function에는 이득이 크지만 large function을 무리하게 inline하면 I-cache·ITLB pressure가 증가한다.

profile-guided inlining은 hotness와 context를 반영한다. manually always-inline을 남발하면 compiler cost model을 무력화한다.

inlining 전후 instruction bytes와 frontend stall을 함께 비교한다. microbenchmark win이 application 전체 miss 증가로 상쇄되는지 확인한다.

---

## CHAPTER 16 · outlining은 rare 또는 반복 code를 별도 function으로 빼 footprint를 줄인다

반복되는 sequence나 cold block을 function으로 추출하면 code duplication이 줄어 I-cache 효율이 좋아질 수 있다. 대신 call/return과 register-save overhead가 생긴다.

size-sensitive mobile app이나 huge template binary에서 outlining은 startup과 storage 양쪽에 도움이 될 수 있다. hot tiny sequence까지 outline하면 branch overhead가 증가한다.

binary size와 runtime profile을 같이 보며 대상 block을 선택한다. release build에서 실제 linker/compiler outlining을 확인한다.

---

## CHAPTER 17 · function order는 inter-function locality와 page working set을 바꾼다

서로 자주 호출되는 function을 같은 code page와 근접한 address에 배치하면 I-cache와 ITLB locality가 좋아질 수 있다. source file order나 link order가 실제 call graph를 반영하지 않을 수 있다.

shared library 경계는 function order 최적화를 제한하고 dynamic symbol interposition도 layout freedom을 줄인다. post-link profile optimizer가 유용한 이유다.

call graph edge weight와 final address를 비교한다. startup critical function이 여러 page에 흩어져 있는지 확인한다.

---

## CHAPTER 18 · code page fault는 cold start에 storage latency를 가져온다

executable mapping이 존재해도 code page가 resident하지 않으면 첫 instruction fetch에서 page fault와 file I/O가 필요할 수 있다. app startup에서 수많은 small function이 여러 page를 건드리면 storage read와 decompression이 critical path가 된다.

warm benchmark는 이 비용을 숨긴다. prefetch를 너무 넓게 하면 필요 없는 code까지 읽어 startup I/O를 늘린다.

major/minor code fault와 first-use function을 trace한다. cold-start experiment를 별도로 유지한다.

---

## CHAPTER 19 · ASLR은 address variance로 microarchitectural layout 결과를 흔들 수 있다

주소 무작위화는 security에 중요하지만 function과 branch가 cache/BTB set에 배치되는 위치를 run마다 바꿀 수 있다. 일부 microbenchmark는 ASLR 배치 차이만으로 성능 분산이 생긴다.

security를 끄고 benchmark를 고정하는 것은 production behavior를 대표하지 않을 수 있다. 여러 randomization run의 distribution을 보는 편이 낫다.

binary layout experiment에서는 base address와 hotspot offsets를 함께 기록한다. single run의 작은 차이를 optimization 효과로 과대해석하지 않는다.

---

## CHAPTER 20 · instruction form은 같은 계산도 frontend와 backend 비용을 바꾼다

compiler는 addressing mode, immediate, vector form 등 여러 instruction sequence 중 target cost가 낮은 것을 선택한다. 긴 encoding은 fetch bytes를 늘리고 complex form은 여러 uop으로 decode될 수 있다.

source expression을 “instruction 하나”로 기대하지 않는다. target CPU와 optimization flag에 따라 lowering이 다르다.

disassembly와 uop counter를 함께 해석한다. code-size와 backend latency를 동시에 비교한다.

---

## CHAPTER 21 · macro/micro fusion은 여러 operation을 frontend에서 하나처럼 처리할 수 있다

일부 CPU는 compare+branch 같은 특정 instruction pair나 addressing form을 내부적으로 fusion해 frontend/uop pressure를 줄인다. alignment나 operand 형태가 조건을 깨면 같은 source도 fusion이 안 될 수 있다.

fusion rule은 microarchitecture specific하므로 generic optimization rule로 hard-code하지 않는다. compiler backend가 이미 이를 고려하는 경우가 많다.

PMU의 fused/unfused behavior가 가능한 환경에서 generated code와 함께 확인한다. manual assembly 변경은 portability cost를 계산한다.

---

## CHAPTER 22 · JIT code는 생성 뒤 instruction-cache visibility를 보장해야 한다

runtime이 memory에 machine code를 쓰고 executable로 전환할 때 CPU instruction cache와 data cache의 coherency rule을 따라야 한다. architecture에 따라 explicit cache maintenance가 필요할 수 있다. W^X permission transition과 code publication ordering도 포함된다.

다른 thread가 patch 완료 전에 code를 실행하면 partial instruction을 볼 수 있다. code cache lifetime과 deoptimization metadata도 함께 관리해야 한다.

JIT crash에서는 code generation ID와 patch timestamp를 보존한다. symbolized source만 보지 말고 fault bytes를 실제 code cache와 대조한다.

---

## CHAPTER 23 · code patching은 concurrent execution과 atomic visibility를 설계해야 한다

JIT inline cache, breakpoint, live patch는 실행 중인 code bytes나 branch target을 바꿀 수 있다. patch size가 architecture atomic write 범위를 넘으면 thread가 중간 상태를 실행할 위험이 있다.

safe point, trap-based patching, versioned code 같은 mechanism으로 publication을 통제한다. permission을 W+X로 오래 열어 두는 방식은 security를 약화한다.

patch event와 executing thread generation을 trace한다. stress test에서 patch와 high-concurrency call을 겹친다.

---

## CHAPTER 24 · unwind metadata도 code layout 변화와 함께 이동해야 한다

function address와 instruction range가 바뀌면 exception unwind와 profiler가 사용하는 metadata가 final code에 맞아야 한다. post-link reordering이나 JIT patch가 metadata update를 놓치면 crash stack이 틀려진다.

symbol table만 맞아도 CFI가 wrong range를 가리킬 수 있다. stripped production binary에서는 build-ID linkage가 더 중요하다.

release artifact에서 unwind coverage를 검증한다. sample stack corruption처럼 보이는 문제를 code bug로 오인하지 않는다.

---

## CHAPTER 25 · shared library layout은 process 전체 frontend working set에 기여한다

application code뿐 아니라 runtime·libc·framework shared library가 동일 I-cache와 ITLB를 사용한다. call path가 여러 library를 왕복하면 code page locality가 분산된다.

library consolidation은 locality를 좋게 할 수 있지만 independent update와 sharing 장점을 줄인다. PLT/loader path와 ASLR도 layout에 영향을 준다.

startup과 hot request에서 실제 module call distribution을 측정한다. own binary만 최적화하고 system library footprint를 무시하지 않는다.

---

## CHAPTER 26 · PGO는 실제 branch와 call hotness를 layout decision에 공급한다

Profile-Guided Optimization은 어떤 function·block·call target이 자주 실행되는지 compiler/linker에 제공해 inlining, block layout, function ordering을 개선한다. profile이 workload를 대표해야 효과가 있다.

old version profile을 크게 바뀐 source에 적용하거나 synthetic test profile만 사용하면 잘못된 hot/cold 판단을 만들 수 있다.

profile version과 collection workload를 artifact로 보존한다. release 후 production counter와 예상 hotness를 비교한다.

---

## CHAPTER 27 · post-link optimization은 final address를 알고 binary layout을 다시 조정한다

link가 끝난 뒤에는 실제 function size, relocation, call edge를 알 수 있어 binary-level reordering과 basic-block optimization을 더 정확히 할 수 있다. source compiler 단계에서 보이지 않던 shared-library와 final alignment도 반영할 수 있다.

post-link rewrite는 relocation·unwind·debug 정보를 모두 정확히 유지해야 한다. signing과 reproducible build pipeline에도 영향을 준다.

optimized binary의 provenance와 original build ID 관계를 명확히 한다. crash symbolization을 자동 검증한다.

---

## CHAPTER 28 · frontend PMU counter는 microarchitecture별 semantics를 확인해야 한다

frontend bound, I-cache miss, branch event 이름이 CPU family마다 동일 의미를 갖지 않을 수 있다. counter multiplexing과 skid도 sample 해석에 영향을 준다.

derived metric 하나를 CPU 세대 사이 직접 비교하면 잘못된 결론을 낼 수 있다. event definition과 sampling mode를 결과와 함께 보존한다.

PMU는 wall-time·profile·binary layout과 상호검증한다. counter가 가설과 같은 방향으로 움직이지 않으면 optimization 설명을 다시 세운다.

---

## CHAPTER 29 · code-layout benchmark는 address variance와 cold/warm state를 통제한다

layout 변경의 효과는 microseconds 수준일 수 있어 CPU frequency, ASLR, page-cache warm state가 noise가 된다. 동일 binary라도 randomized base와 scheduler 상태가 결과를 흔든다.

여러 process launch, cold/warm run을 분리하고 confidence interval을 본다. benchmark harness 자체가 code footprint를 바꾸는 instrumentation도 최소화한다.

layout change가 I-cache/BTB metric과 latency를 동시에 개선하는지 확인한다. wall time만 줄면 causal claim은 약하다.

---

## CHAPTER 30 · frontend contract는 hot code identity와 delivery path를 재현 가능하게 만든다

frontend 최적화를 운영하려면 어느 build의 어떤 function/block이 hot한지, final address와 profile이 무엇인지, cold-start와 steady-state 중 어느 상황을 개선하는지 명시해야 한다. source diff만으로는 binary layout 효과를 재현할 수 없다.

inlining·PGO·post-link rewrite는 code identity와 symbol artifact를 바꾸므로 crash/debug pipeline까지 함께 검증한다. security mitigation과 ASLR을 성능 편의를 위해 임의로 끄지 않는다.

최종 목표는 instruction 수를 줄이는 것이 아니라 **backend가 필요한 순간에 올바른 instruction stream을 안정적으로 공급하도록 code layout과 prediction working set을 관리하는 것**이다.
