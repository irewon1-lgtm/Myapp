# PART 19 · Compiler Optimization — SSA, dataflow, alias, loop, vectorization

최적화 compiler는 source 문장을 “더 빠른 문장”으로 바꾸는 도구가 아니다. 프로그램이 허용하는 observable behavior를 보존하면서 **필요한 계산만 남기고, dependency를 드러내고, memory uncertainty를 줄이며, target machine에서 더 싼 실행 형태를 선택**한다. 성능과 correctness를 동시에 이해하려면 IR의 구조와 analysis가 transformation을 어떻게 정당화하는지 알아야 한다.

---

## CHAPTER 01 · IR은 source syntax와 machine instruction 사이의 reasoning substrate다

좋은 IR은 compiler가 분석하기 어려운 source-level sugar를 걷어내면서도 optimization에 필요한 type·control-flow·memory semantics를 유지한다. LLVM IR은 SSA 기반 typed representation으로 function, basic block, instruction, explicit control-flow를 표현한다. human-readable assembly, in-memory IR, bitcode가 같은 semantic model을 공유한다.

IR을 보는 목적은 syntax 암기가 아니다. source가 어떤 operation graph로 lowering되었는지, high-level abstraction이 실제 load/store/call/branch로 어떻게 드러났는지 확인하는 것이다. performance regression이 source diff보다 lower-level IR shape 변화에서 더 명확하게 보이는 경우가 있다.

---

## CHAPTER 02 · well-formedness는 parser acceptance보다 강한 compiler invariant다

IR text가 문법적으로 parse된다고 valid program representation은 아니다. SSA use가 definition에 dominance되어야 하고 type/CFG/phi relation이 맞아야 한다. LLVM LangRef도 parser가 받아들이는 것과 well-formed IR를 구분한다. verifier는 optimization 전에 이러한 structural invariant를 확인한다.

custom compiler pass나 code generator가 malformed IR를 만들면 뒤 pass에서 obscure crash가 생길 수 있다. transformation 직후 verifier를 실행해 corruption point를 좁힌다. `backend가 crash했다`는 증상보다 처음 invariant를 깨뜨린 pass가 원인이다.

---

## CHAPTER 03 · CFG는 branch 문법을 basic-block graph로 바꾼다

basic block은 중간 branch 없이 순차 실행되는 instruction sequence이고 terminator가 successor를 결정한다. `if`, loop, switch, exception path는 control-flow graph edge로 표현된다. optimization은 source indentation보다 CFG를 기준으로 reachability와 path relation을 계산한다.

unreachable block, critical edge, irreducible loop는 transformation 가능성을 바꾼다. code coverage와 CFG coverage도 구분한다. source line 하나가 여러 block으로 lowering될 수 있고 exception/cleanup edge가 source에 명시적으로 보이지 않을 수 있다.

---

## CHAPTER 04 · dominator는 value와 control condition이 모든 path에서 선행하는지를 표현한다

block A가 block B를 dominate한다는 것은 entry에서 B로 가는 모든 path가 A를 지난다는 뜻이다. SSA definition이 use를 dominate해야 value가 모든 실행 path에서 존재한다는 것이 보장된다. loop header와 branch reasoning에도 dominator tree가 핵심이다.

post-dominator는 반대로 어떤 block 이후 모든 exit path가 특정 block을 지나가는지 표현한다. control dependence, code motion, cleanup placement를 분석할 때 사용된다. dominator tree를 계산한 뒤 CFG를 바꾸는 pass는 analysis를 invalidate하거나 정확히 update해야 한다.

---

## CHAPTER 05 · SSA는 각 logical value에 하나의 definition을 부여한다

Static Single Assignment form에서는 register-like value가 한 번만 정의된다. source variable이 여러 번 대입되어도 compiler는 versioned value로 나눈다. 이 구조는 def-use chain을 명확히 하고 constant propagation, dead-code elimination, value numbering을 쉽게 만든다.

SSA가 모든 memory location을 한 번만 쓰게 한다는 뜻은 아니다. heap/stack memory는 load/store와 alias analysis가 필요하다. `register SSA`와 `memory state reasoning`을 구분해야 optimizer가 local scalar에는 강하지만 pointer-heavy code에서 보수적인 이유를 이해할 수 있다.

---

## CHAPTER 06 · phi node는 여러 predecessor의 value를 control-flow merge에서 선택한다

branch마다 다른 value가 정의되고 merge block에서 하나의 SSA value가 필요하면 phi가 predecessor edge에 따라 입력을 선택한다. phi는 runtime function call이 아니라 CFG edge와 value definition을 표현하는 IR construct다.

loop induction variable도 header phi로 표현할 수 있다. initial value와 backedge update가 하나의 recurrence를 만든다. loop optimization은 이 recurrence를 분석해 trip count, bounds, stride를 추론한다.

---

## CHAPTER 07 · mem2reg는 stack slot을 SSA value로 승격해 memory dependency를 제거한다

frontend가 local scalar를 alloca/load/store 형태로 생성해도 address가 escape하지 않으면 optimizer가 이를 SSA register value로 promote할 수 있다. load/store가 사라지면 alias uncertainty와 memory traffic이 줄고 scalar optimization이 쉬워진다.

address가 다른 function에 전달되거나 volatile/escaping semantics가 있으면 promotion이 제한된다. `local variable이 stack에 있다`는 source-level 추정은 optimized IR에서 틀릴 수 있다. debug build와 release build의 stack frame이 다른 이유다.

---

## CHAPTER 08 · dataflow analysis는 program point마다 사실의 집합을 계산한다

reaching definitions, liveness, available expressions 같은 analysis는 CFG edge를 따라 fact를 전달하고 fixed point까지 반복한다. forward/backward, may/must 성질에 따라 meet operator와 lattice가 달라진다.

compiler pass를 이해할 때 개별 algorithm보다 **어떤 fact를 어느 program point에서 보수적으로 알고 싶은가**를 먼저 묻는다. analysis가 conservative하다는 것은 최적화 기회를 놓칠 수 있어도 semantics를 깨는 잘못된 확신을 피한다는 뜻이다.

---

## CHAPTER 09 · liveness는 register allocation과 dead-code 판단의 입력이다

value가 현재 지점 이후 어떤 path에서 사용될 가능성이 있으면 live하다. live range가 겹치는 value는 같은 physical register를 동시에 공유할 수 없으므로 interference relation이 생긴다. 높은 register pressure는 spill/reload로 이어진다.

source variable scope와 live range는 다르다. lexical scope가 길어도 마지막 use 이후 value는 dead일 수 있고 compiler가 register를 재사용한다. debugging에서 variable이 optimized out되는 이유도 source 이름의 lifetime과 machine value liveness가 다르기 때문이다.

---

## CHAPTER 10 · constant propagation은 value lattice로 compile-time known state를 전파한다

constant value가 알려지면 arithmetic을 compile time에 계산하고 branch condition을 확정해 unreachable path를 제거할 수 있다. conditional constant propagation은 CFG reachability와 value information을 결합해 단순 local folding보다 강한 결과를 만든다.

잘못된 undefined behavior assumption이나 language overflow rule을 적용하면 constant folding이 source expectation과 달라 보일 수 있다. compiler는 language semantics를 기준으로 최적화한다. test가 undefined behavior에 의존하면 optimization level에서 결과가 바뀌는 것이 허용될 수 있다.

---

## CHAPTER 11 · value numbering은 동일 계산을 semantic equivalence로 찾아낸다

같은 operands와 operation이 동일한 결과를 낸다는 사실을 증명하면 중복 계산을 하나로 합칠 수 있다. Global Value Numbering은 CFG를 넘어 value equivalence를 추적한다. common-subexpression elimination의 더 일반적인 형태로 볼 수 있다.

memory load는 pointer alias와 intervening store 때문에 단순 expression equality로 합칠 수 없다. memory dependency와 alias analysis가 `같은 address의 값이 바뀌지 않았다`는 사실을 제공해야 한다.

---

## CHAPTER 12 · dead-code elimination은 side effect 없는 불필요한 계산만 제거한다

result가 사용되지 않고 observable side effect도 없는 instruction은 제거 가능하다. pure arithmetic과 unused temporary는 사라질 수 있지만 volatile access, atomic, I/O call처럼 관찰 가능한 effect는 보존해야 한다.

function call이 제거 가능한지 판단하려면 memory/side-effect attribute와 interprocedural analysis가 필요하다. 잘못된 purity annotation은 optimizer에게 실제 effect를 삭제할 권한을 주어 correctness를 깨뜨린다. optimization hint는 correctness contract다.

---

## CHAPTER 13 · alias analysis는 두 memory reference가 같은 object를 가리킬 수 있는지 분류한다

pointer A와 B가 절대 겹치지 않는다고 증명하면 compiler는 load/store 순서를 더 자유롭게 바꾸고 vectorization·LICM을 적용할 수 있다. LLVM alias infrastructure는 Must/May/NoAlias 같은 결과와 Mod/Ref 정보를 analysis consumer에 제공한다.

MayAlias는 optimizer가 모른다는 뜻이지 실제로 반드시 겹친다는 뜻이 아니다. pointer-heavy API는 alias uncertainty 때문에 optimization을 잃을 수 있다. restrict/nonalias 같은 annotation을 잘못 적용하면 실제 overlap case가 undefined behavior가 될 수 있다.

---

## CHAPTER 14 · escape analysis는 object가 현재 scope 밖에서 관찰되는지 추론한다

allocation된 object reference가 function/thread/global 영역으로 escape하지 않는다고 증명하면 stack allocation, scalar replacement, synchronization elimination 같은 최적화가 가능하다. JIT runtime은 call target과 dynamic type profile까지 활용해 더 공격적으로 분석할 수 있다.

escape 판정은 language reflection, native call, unknown function 때문에 보수적으로 실패할 수 있다. `객체를 만들었으니 heap allocation이 발생한다`는 source-level 판단은 최적화 후 사실이 아닐 수 있다. allocation profiler로 확인한다.

---

## CHAPTER 15 · scalar replacement는 aggregate object를 독립 SSA value로 분해한다

struct/object가 memory identity를 실제로 필요로 하지 않으면 field를 각각 scalar value로 분리해 allocation과 load/store를 제거할 수 있다. object abstraction은 source readability를 유지하면서 machine-level materialization을 피할 수 있다.

reflection, address identity, alias escape는 replacement를 막는다. benchmark에서 object count가 source 코드와 일치하지 않을 수 있으므로 runtime/compiler optimization state를 포함해 분석한다.

---

## CHAPTER 16 · inlining은 call overhead보다 optimization context 확대가 더 큰 목적일 수 있다

callee body가 caller 안으로 들어오면 constant argument, exact type, alias relation이 보이면서 추가 constant folding과 dead-code elimination이 가능해진다. call/return instruction 제거는 일부 이득일 뿐이다.

지나친 inlining은 code size, instruction-cache footprint, register pressure를 키운다. optimizer는 cost model로 hotness와 function size를 고려한다. `inline keyword`와 실제 machine-level inline 여부도 언어마다 다르다.

---

## CHAPTER 17 · interprocedural analysis는 function 경계를 넘어 side effect와 call graph를 본다

whole-program/LTO 환경에서는 어떤 function이 실제로 호출되는지, global이 외부에서 수정되는지 더 많이 알 수 있다. unused exported symbol 제거, devirtualization, cross-module inlining 기회가 생긴다.

반대로 plugin/dynamic loading/interposition 가능성이 있으면 optimizer가 모든 call target을 고정할 수 없다. binary visibility와 link model이 middle-end optimization 품질에 영향을 준다.

---

## CHAPTER 18 · devirtualization은 dynamic dispatch target을 하나 또는 작은 집합으로 줄인다

receiver의 concrete type을 증명하면 virtual/interface call을 direct call로 바꿀 수 있고 이후 inlining이 가능해진다. static class hierarchy analysis, profile-guided type feedback가 사용될 수 있다.

class loading이나 dynamic subclass 가능성이 있으면 JIT는 speculative devirtualization 후 assumption invalidation 시 deopt할 수 있다. AOT compiler는 더 보수적인 visibility rule을 따른다. runtime flexibility와 optimization freedom이 trade-off다.

---

## CHAPTER 19 · loop canonicalization은 후속 analysis가 이해하기 쉬운 CFG 형태를 만든다

loop preheader, dedicated exit, latch처럼 정규화된 구조는 invariant hoisting과 induction-variable analysis를 단순화한다. canonicalization 자체는 큰 speedup을 만들지 않아도 다음 pass의 전제조건을 제공한다.

pass ordering이 중요한 이유가 여기 있다. 한 pass가 IR를 정리해 다음 pass의 analysis precision을 높이고, transformation 뒤 다시 simplification 기회가 생긴다. optimization pipeline은 independent trick 목록이 아니다.

---

## CHAPTER 20 · LICM은 loop invariant work를 반복 영역 밖으로 이동한다

Loop-Invariant Code Motion은 매 iteration 결과가 변하지 않고 이동해도 side-effect/order semantics를 깨지 않는 계산을 preheader로 hoist할 수 있다. memory load는 loop 안 store와 alias할 가능성이 없음을 증명해야 한다.

hoisting이 항상 빠른 것은 아니다. loop가 실행되지 않는 path에서도 expensive computation을 수행하게 만들 수 있고 register lifetime을 늘릴 수 있다. profitability와 safety를 분리한다.

---

## CHAPTER 21 · induction-variable analysis는 반복의 수학적 recurrence를 추론한다

loop counter가 `i = i + stride` 형태로 변하면 Scalar Evolution 계열 analysis가 iteration마다 value를 수식으로 표현할 수 있다. trip count와 array index 범위를 알면 bounds-check elimination, dependence analysis, vectorization이 가능하다.

integer overflow semantics가 recurrence proof에 영향을 준다. no-wrap attribute가 거짓인데 있다고 주장하면 optimizer가 실제 wrap path를 제거할 수 있다. range information도 correctness assertion이다.

---

## CHAPTER 22 · loop unrolling은 branch 수를 줄이고 ILP를 늘리지만 code size를 키운다

iteration body를 여러 번 복제하면 branch/loop-control overhead가 줄고 independent operation이 동시에 보이기 쉬워진다. vectorization과 scheduling 기회를 늘릴 수도 있다.

unroll factor가 크면 instruction cache와 register pressure가 악화되고 remainder handling이 필요하다. tiny hot loop와 large cold loop의 최적 factor가 다르다. target cost model과 profile을 이용한다.

---

## CHAPTER 23 · loop vectorizer는 iteration 간 dependence를 증명한 뒤 여러 iteration을 SIMD로 묶는다

consecutive iteration이 independent하거나 안전한 reduction pattern이면 scalar operation을 vector lane으로 widen할 수 있다. pointer alias가 불확실하면 runtime overlap check를 삽입하고 fast vector path와 scalar fallback을 만들 수도 있다.

vectorization factor는 register width만으로 결정되지 않는다. trip count, memory stride, gather/scatter cost, register pressure, target instruction cost를 포함한 cost model이 필요하다. LLVM은 Loop Vectorizer와 SLP Vectorizer를 별도 경로로 제공한다. citeturn930511search2

---

## CHAPTER 24 · SLP vectorization은 인접 scalar statement의 isomorphic operation을 묶는다

loop가 없어도 여러 독립 scalar add/multiply/load가 유사한 형태이면 Superword-Level Parallelism vectorizer가 vector operation으로 pack할 수 있다. source에서 수동 SIMD 코드를 쓰지 않아도 basic-block pattern에서 기회를 찾는다.

data layout과 instruction ordering이 SLP pattern 인식을 돕거나 방해할 수 있다. 수동 micro-optimization이 오히려 compiler pattern을 깨뜨리는 경우가 있으므로 vectorization remark와 final assembly를 확인한다.

---

## CHAPTER 25 · strength reduction은 비싼 연산을 recurrence나 더 싼 연산으로 바꾼다

loop에서 매번 `base + i*stride`를 계산하는 대신 이전 address에 stride를 더하는 induction update로 바꿀 수 있다. constant multiply/divide도 target에 따라 shift/add 또는 reciprocal 기반 sequence로 lowering될 수 있다.

`multiply가 항상 느리다` 같은 수동 규칙은 modern CPU에서 틀릴 수 있다. optimizer가 target instruction latency/throughput을 알고 있으므로 source를 난독화하기보다 semantics를 명확히 제공하고 assembly를 측정한다.

---

## CHAPTER 26 · branch simplification과 if-conversion은 control dependency를 data dependency로 바꿀 수 있다

작은 branch를 conditional select/predication으로 바꾸면 misprediction 비용을 피할 수 있지만 양쪽 계산을 모두 수행할 수 있다. branch predictability와 side-effect cost가 결정 요소다.

secret-dependent branch를 제거하는 constant-time security 코드에서는 성능보다 observable timing leakage가 중요하다. compiler가 다시 branch를 생성하지 않는지 final assembly와 toolchain guarantee를 확인한다.

---

## CHAPTER 27 · pass manager는 analysis cache와 invalidation contract를 관리한다

optimization pass는 module/function/loop 같은 IR unit에서 analysis 결과를 조회하고 transformation 후 어떤 analysis가 여전히 유효한지 알려야 한다. dominance/loop info 같은 analysis 계산은 비싸므로 pass manager가 캐시하고 필요한 경우에만 재계산한다.

잘못된 invalidation은 stale analysis를 다음 pass가 사용하게 해 miscompile을 만들 수 있다. LLVM new pass manager는 analysis manager와 preserved analyses를 명시적으로 다룬다. pass ordering과 analysis lifetime도 compiler correctness의 일부다. citeturn930511search0turn930511search6

---

## CHAPTER 28 · profile-guided optimization은 실제 hot path를 cost model에 입력한다

static heuristic만으로 branch probability와 function hotness를 정확히 알기 어렵다. instrumentation 또는 sampling profile을 수집해 inlining, layout, branch placement를 workload에 맞출 수 있다.

profile이 production workload를 대표하지 않으면 hot/cold 판단이 잘못되어 code layout이 악화될 수 있다. profile version과 binary build를 매칭하고 stale profile 사용을 탐지한다. rare error path가 cold라고 삭제되는 것은 아니며 semantics는 유지되어야 한다.

---

## CHAPTER 29 · JIT speculation은 guard와 deoptimization으로 공격적인 optimization을 가능하게 한다

runtime이 `이 call site는 지금까지 한 type만 보였다`는 profile을 이용해 direct call/inlining을 수행할 수 있다. assumption을 guard하고 다른 type이 나타나면 optimized frame을 interpreter/baseline state로 deopt한다.

deoptimization metadata는 optimized register/stack state에서 logical source frame을 재구성할 수 있어야 한다. aggressive optimization은 fast path만의 문제가 아니라 assumption invalidation과 state reconstruction correctness까지 포함한다.

---

## CHAPTER 30 · miscompile을 의심할 때는 source를 바꾸기 전에 optimization differential을 만든다

`-O0`에서는 정상이고 `-O2`에서만 실패한다고 compiler bug를 즉시 결론내리지 않는다. undefined behavior, data race, uninitialized read, alias contract violation이 optimization에서 드러나는 경우가 훨씬 흔하다.

최소 reproducer를 만들고 optimization pass를 단계적으로 줄여 처음 결과가 달라지는 pass를 찾는다. IR verifier, sanitizer, different compiler/version을 교차검증한다. compiler optimization을 이해하는 목적은 hand-optimization보다 **어떤 transformation이 정당한지, source의 어느 contract가 optimizer에게 그 권한을 주었는지 증명하는 것**이다.