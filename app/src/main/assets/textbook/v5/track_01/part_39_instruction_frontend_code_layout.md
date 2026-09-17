# PART 39 · Instruction Frontend and Code Layout — fetch, decode, I-cache, BTB

CPU가 arithmetic unit을 충분히 가지고 있어도 instruction을 제때 공급하지 못하면 backend execution resource는 놀게 된다. Instruction frontend는 code address를 예측하고, instruction byte를 I-cache/ITLB에서 가져오고, instruction boundary를 찾고 decode하며, 일부 microarchitecture에서는 decoded operation을 별도 cache에 보관한다. 따라서 code size, branch topology, function placement, alignment는 source-level operation count와 독립적인 성능 축이다.

## CHAPTER 01 · frontend bottleneck은 execution unit이 비어 있는데 instruction 공급이 늦은 상태다

Performance counter에서 ALU utilization이 낮다고 해서 계산량이 적다는 뜻은 아니다. Fetch stall, I-cache miss, ITLB miss, branch redirect, decoder throughput limit가 backend에 충분한 operation을 공급하지 못할 수 있다. 이런 경우 arithmetic instruction을 줄이는 optimization보다 code footprint와 control-flow locality를 고치는 편이 효과적이다. Frontend-bound와 backend-bound를 분리하지 않으면 profiler에서 `CPU가 덜 바쁘다`는 현상을 잘못 해석한다.

## CHAPTER 02 · instruction fetch는 program counter 주변 byte를 계속 공급하는 memory workload다

Instruction도 memory hierarchy를 통과한다. Code page는 virtual address translation을 거쳐 instruction cache에 들어오고 fetch unit은 architecture-defined instruction stream을 읽는다. Data cache hit rate가 높아도 I-cache miss가 많을 수 있고, code와 data의 working set은 별도 pressure를 만든다. Large generated code, heavy template expansion, aggressive inlining은 data locality를 개선하면서 동시에 instruction footprint를 악화시킬 수 있다.

## CHAPTER 03 · I-cache capacity는 hot code working set과 비교해야 한다

Hot path가 I-cache에 안정적으로 머무르면 반복 실행 시 fetch latency를 줄일 수 있다. 반대로 여러 큰 function과 library stub를 오가면 hot instruction line이 계속 eviction될 수 있다. Code size를 줄이는 optimization이 실행 instruction 수를 조금 늘리더라도 I-cache residency를 크게 개선하면 전체 latency가 줄 수 있다. Binary size와 hot-code size는 같은 지표가 아니므로 profile-guided hot section을 별도로 본다.

## CHAPTER 04 · ITLB는 instruction page translation을 cache한다

Code가 많은 virtual page에 흩어져 있으면 instruction fetch마다 translation pressure가 커지고 ITLB miss가 page-table walk로 이어질 수 있다. Huge-page code mapping이 ITLB reach를 높일 가능성이 있지만 fragmentation, deployment, ASLR, permission 관리와 trade-off가 있다. Function layout이 같은 byte 수라도 page boundary를 자주 넘는지에 따라 ITLB behavior가 달라진다. I-cache miss와 ITLB miss를 구분해 수집해야 원인 위치를 알 수 있다.

## CHAPTER 05 · variable-length instruction ISA에서는 decode boundary 자체가 frontend work다

고정 길이 instruction과 variable-length instruction은 fetch/decode 구조가 다르다. Variable-length ISA에서는 byte stream에서 instruction boundary를 찾고 여러 instruction을 병렬 decode하는 과정이 복잡해질 수 있다. Instruction density가 좋아 code footprint를 줄이는 장점과 decode complexity가 공존한다. `한 instruction이 짧다`와 `frontend throughput이 높다`는 같은 말이 아니다.

## CHAPTER 06 · decode width는 cycle당 backend로 보낼 수 있는 operation 상한 중 하나다

Frontend가 cycle당 decode 가능한 instruction 또는 micro-operation 수에는 microarchitecture별 한계가 있다. Branch·complex instruction·multi-uop expansion 때문에 source instruction 수와 decoded uop 수가 다를 수 있다. Loop가 L1I에 완전히 들어와도 decoder가 병목이면 backend port는 충분히 활용되지 않는다. PMU에서 frontend delivery와 uop count를 함께 보면 decode throughput 문제를 확인할 수 있다.

## CHAPTER 07 · micro-op cache는 반복 decode 비용을 줄이는 별도 계층이 될 수 있다

일부 CPU는 이미 decode한 instruction을 micro-op 형태로 cache해 hot loop가 decoder를 반복 사용하지 않도록 한다. 이 cache의 capacity와 indexing 특성 때문에 code alignment와 branch topology가 hit rate에 영향을 줄 수 있다. Micro-op cache에서 잘 공급되던 loop가 작은 code layout 변화 후 decode path로 떨어지면 source operation은 같아도 성능이 변한다. 특정 CPU에만 존재하는 구조를 universal rule로 일반화하지 않고 target별 counter를 본다.

## CHAPTER 08 · branch prediction은 다음 fetch address를 미리 정하는 frontend mechanism이다

Conditional branch의 taken/not-taken 예측뿐 아니라 direct/indirect branch target 예측이 fetch stream을 유지한다. Prediction이 맞으면 speculative fetch/decode를 계속할 수 있고, 틀리면 pipeline에서 잘못 가져온 work를 버리고 correct target에서 다시 시작한다. Branch misprediction penalty는 단순 branch instruction 한 개 비용이 아니라 frontend/backend pipeline depth와 speculation window에 연결된다.

## CHAPTER 09 · BTB는 branch target을 빠르게 제공하지만 capacity와 aliasing이 있다

Branch Target Buffer류 structure는 최근 branch의 target 정보를 보관해 fetch address를 빠르게 예측한다. Hot code에 branch site가 매우 많거나 서로 다른 address가 같은 predictor resource를 경쟁하면 capacity/conflict 문제가 생길 수 있다. Large switch, virtual dispatch, generated state machine은 branch count와 target diversity를 늘린다. Branch predictor accuracy만이 아니라 BTB miss·indirect target behavior를 함께 분석한다.

## CHAPTER 10 · indirect branch는 target set 크기가 커질수록 예측이 어려워질 수 있다

Function pointer, virtual method, interpreter dispatch처럼 하나의 branch site가 여러 target을 가질 수 있다. Input에 따라 target distribution이 안정적이면 predictor가 잘 학습할 수 있지만 megamorphic call site처럼 target이 넓게 분산되면 miss가 증가할 수 있다. Devirtualization과 inline cache는 단순 call overhead 제거뿐 아니라 indirect prediction entropy를 줄이는 최적화이기도 하다.

## CHAPTER 11 · return prediction은 일반 indirect branch와 별도 mechanism을 사용할 수 있다

Call/return pair는 stack 구조를 가지므로 CPU가 return-address stack류 predictor를 사용해 return target을 예측할 수 있다. Deep recursion, unmatched control transfer, coroutine/context-switch 구현은 predictor 가정과 충돌할 수 있다. Return misprediction이 나타난다면 단순 function-call count보다 call/return structure를 본다. Security mitigation이 return prediction behavior를 바꾸는 경우도 있으므로 build/configuration과 함께 분석한다.

## CHAPTER 12 · function alignment는 fetch boundary와 code size를 동시에 바꾼다

Function 또는 loop entry를 특정 boundary에 맞추면 fetch/decode block crossing을 줄일 수 있지만 padding byte가 늘어 instruction footprint를 키운다. Alignment를 과하게 적용하면 전체 text section이 커져 I-cache/ITLB pressure가 악화될 수 있다. 최적 alignment는 target microarchitecture와 hotness에 따라 다르다. 모든 function을 동일 boundary에 정렬하는 규칙보다 profile-guided selective alignment가 합리적이다.

## CHAPTER 13 · basic-block layout은 fall-through path와 branch 수를 바꾼다

조건문의 hot path가 다음 byte sequence로 이어지도록 block을 배치하면 taken branch를 줄이고 instruction fetch locality를 높일 수 있다. Cold error path를 중간에 배치하면 hot instruction line에 거의 실행되지 않는 code가 섞여 I-cache 공간을 소비한다. Compiler profile data는 block frequency를 사용해 hot fall-through와 branch target layout을 개선할 수 있다. Source code의 if/else 순서가 최종 binary layout과 항상 같지는 않다.

## CHAPTER 14 · hot/cold splitting은 드물게 실행되는 code를 별도 영역으로 이동한다

Error handling, logging, exceptional path가 large code body를 차지해도 normal path에서 거의 실행되지 않을 수 있다. Hot/cold splitting은 이런 block을 hot function body에서 분리해 I-cache density를 높인다. 대신 cold path로 들어갈 때 longer branch, page miss가 발생할 수 있다. 목표는 모든 path를 동일하게 빠르게 만드는 것이 아니라 production frequency에 맞춰 instruction working set을 구성하는 것이다.

## CHAPTER 15 · aggressive inlining은 call overhead를 줄이지만 instruction footprint를 복제한다

Inlining은 call/return overhead 제거, constant propagation, devirtualization 기회를 만들지만 callee code가 여러 call site에 복제되어 text size를 늘린다. Small hot function은 유리할 수 있지만 large callee를 여러 곳에 inlining하면 I-cache와 uop-cache pressure가 커진다. Compiler inlining threshold를 무작정 올리지 말고 profile-guided hotness와 code-size report를 함께 본다.

## CHAPTER 16 · outlining은 반복 code를 function으로 추출해 footprint를 줄일 수 있다

Inlining의 반대 방향으로 여러 곳에 중복된 sequence를 shared function으로 만들면 code size를 줄인다. Branch/call overhead가 추가되지만 cold 또는 large repeated sequence에서는 I-cache 이득이 더 클 수 있다. Size-optimized build가 throughput-optimized build보다 특정 workload에서 빨라지는 이유 중 하나가 instruction footprint 감소다. Binary diff에서 text size와 hot-path call structure를 함께 본다.

## CHAPTER 17 · link-time function ordering은 page와 cache-line locality를 바꾼다

서로 자주 호출되는 function을 binary에서 가깝게 배치하면 instruction page와 cache-line 재사용이 좋아질 수 있다. 반대로 alphabetic/object-file order는 runtime call graph와 무관하다. Profile-guided function reordering은 call edge frequency를 이용해 cluster를 만들 수 있다. Shared library가 여러 개면 library boundary와 relocation/PLT path까지 포함한 실제 code locality를 평가한다.

## CHAPTER 18 · code page fault도 startup latency의 일부다

Process startup이나 cold function 진입 시 executable page가 아직 resident하지 않으면 page fault를 통해 storage/page cache에서 code를 가져와야 할 수 있다. Cold start benchmark에서 CPU decode보다 storage/mmap/page-fault 비용이 지배할 수 있다. Warm benchmark만으로 startup optimization을 판단하면 이 비용을 놓친다. P13 page cache, P14 fault, P24 storage와 instruction fetch를 같은 timeline에 연결한다.

## CHAPTER 19 · ASLR은 security를 높이는 대신 absolute placement를 run마다 바꾼다

Address-space randomization은 code/library 위치를 변화시켜 exploit reliability를 낮추는 protection이다. Performance measurement에서 absolute address가 달라지면 cache-set conflict나 branch predictor aliasing 같은 미세한 영향이 변할 수 있다. 재현성이 매우 중요한 microbenchmark에서는 multiple-run distribution을 사용하고 한 실행의 outlier를 구조적 성능으로 해석하지 않는다. Security protection을 끄고 얻은 benchmark를 production 성능으로 직접 대입하지 않는다.

## CHAPTER 20 · code generation은 instruction count보다 instruction form을 바꿀 수 있다

같은 high-level operation도 compiler가 target feature와 cost model에 따라 다른 instruction sequence로 만든다. Macro-fusion 가능한 compare+branch, addressing mode 사용, load-op fusion, immediate encoding 차이가 decoded uop 수와 frontend bandwidth를 바꿀 수 있다. Source line count나 assembly instruction count만으로 성능을 예측하지 말고 target에서 실제 decoded work를 측정한다.

## CHAPTER 21 · macro-fusion과 micro-fusion은 source instruction 수와 uop 수의 관계를 바꾼다

일부 architecture/microarchitecture는 특정 instruction pair를 frontend/backend에서 하나에 가까운 internal operation으로 처리하거나 memory operand를 결합할 수 있다. 이런 세부는 CPU generation별로 달라질 수 있으므로 portable correctness contract가 되어서는 안 된다. 다만 hotspot tuning에서는 특정 sequence가 fusion을 깨뜨리는 code layout/flag 변화로 regression을 만들 수 있음을 알아야 한다.

## CHAPTER 22 · self-modifying/JIT code는 instruction/data coherence 계약이 필요하다

JIT compiler는 writable memory에 machine code를 생성한 뒤 executable state로 전환한다. CPU architecture에 따라 data cache에 쓴 byte와 instruction fetch가 보는 상태를 동기화하기 위한 cache maintenance 또는 barrier가 필요할 수 있다. W^X policy는 동시에 writable+executable mapping을 제한한다. JIT correctness는 code byte 생성뿐 아니라 permission transition과 instruction synchronization까지 포함한다.

## CHAPTER 23 · code patching은 실행 중 thread와의 동시성 문제다

Runtime이 inline cache, probe, breakpoint, hot patch를 위해 instruction을 바꾸면 다른 CPU/thread가 그 위치를 fetch 중일 수 있다. Patch 단위 atomicity, stop-the-world, breakpoint-assisted patching 등 안전한 protocol이 필요하다. Partial instruction이 관찰되면 illegal instruction이나 잘못된 control flow가 발생할 수 있다. Dynamic instrumentation은 text byte 변경 비용과 synchronization 비용을 모두 가진다.

## CHAPTER 24 · exception/unwind metadata도 binary instruction footprint와 별도 memory footprint를 만든다

Stack unwinding, exception handling, symbolization을 위한 metadata는 실행 instruction과 다른 section에 저장될 수 있다. 일반 path에서는 거의 읽지 않지만 crash/unwind 시 memory access가 발생한다. Binary-size optimization에서 text만 줄이고 unwind/debug metadata를 무시하면 package/startup/memory footprint가 예상과 다를 수 있다. Strip policy는 production diagnosability와 artifact size를 함께 결정한다.

## CHAPTER 25 · shared-library boundary는 code locality와 symbol indirection을 함께 만든다

모듈화된 shared library는 update와 memory sharing 장점이 있지만 hot call graph가 library를 자주 넘으면 PLT/GOT, separate mapping, page locality가 영향을 줄 수 있다. Loader가 symbol을 이미 resolve했더라도 code page는 다른 mapping에 있다. Microservice처럼 software boundary가 performance domain을 만드는 것과 유사하게 binary module boundary도 instruction-locality domain이 될 수 있다.

## CHAPTER 26 · PGO는 branch frequency와 call graph를 실제 workload에서 가져온다

Profile-Guided Optimization은 representative run에서 branch/call frequency를 수집해 inlining, block layout, function ordering 같은 결정을 개선한다. Profile workload가 production과 다르면 optimizer가 잘못된 hot path를 강화할 수 있다. PGO artifact에는 source/build revision과 profile dataset identity를 묶어야 한다. Profile freshness와 workload drift가 optimization validity의 일부다.

## CHAPTER 27 · BOLT류 post-link optimization은 final binary layout을 profile로 다시 배열할 수 있다

Link 후 binary를 실제 execution profile 기반으로 재배치하는 접근은 compiler IR 단계에서 알기 어려운 final address/layout을 직접 최적화한다. Function/block reordering, alignment 조정으로 I-cache/ITLB/branch locality를 개선할 수 있다. 하지만 debug info, unwind, relocation, symbol mapping을 보존해야 한다. Post-link transformation도 reproducible artifact chain에 포함돼야 한다.

## CHAPTER 28 · frontend PMU counter는 이름보다 event semantics를 확인해야 한다

CPU마다 `frontend bound`, `icache miss`, `itlb miss`, `branch miss`, `uops delivered` 등 event 정의가 다르고 counter multiplexing이 발생할 수 있다. Tool이 계산한 high-level 비율은 raw counter와 model assumption에서 나온다. Counter availability와 skid, sampling period를 기록하지 않으면 두 machine 결과를 직접 비교하기 어렵다. Performance diagnosis는 event 이름이 비슷하다는 이유로 architecture를 섞지 않는다.

## CHAPTER 29 · code-layout benchmark는 address randomization과 warm state를 통제한다

Function order 최적화 전후를 비교할 때 동일 input, build flags, CPU frequency, ASLR policy, cache warmness, page residency를 가능한 한 맞춘다. Cold-start와 steady-state를 별도로 측정하고, multiple process launch로 distribution을 본다. I-cache/ITLB/branch counter가 예상 방향으로 변했는지 확인해야 wall-clock improvement의 causal explanation이 생긴다.

## CHAPTER 30 · instruction frontend 최적화의 최종 계약은 hot code를 적은 page와 predictable control flow에 배치하는 것이다

검증 가능한 optimization은 hot instruction working set, branch target distribution, I-cache/ITLB miss, uop delivery, code size를 함께 기록한다. Inlining·alignment·function ordering을 한꺼번에 바꾸지 말고 profile과 binary diff로 변화 원인을 분리한다. Speedup이 특정 CPU의 undocumented quirk에만 의존하면 portability risk를 명시한다. Frontend optimization은 source readability보다 binary layout과 runtime evidence를 기준으로 판정한다.