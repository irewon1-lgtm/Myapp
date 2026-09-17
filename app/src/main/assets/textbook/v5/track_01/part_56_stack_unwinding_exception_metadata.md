# PART 56 · Stack Unwinding and Exception Metadata — frames, CFI, signals, optimized code

Stack trace는 memory를 위로 훑어 return address를 찾는 단순 작업이 아니다. Optimizing compiler는 frame pointer를 생략하고, register를 재배치하고, function을 inline하거나 tail-call로 바꿀 수 있다. Unwinder는 ABI와 CFI metadata를 이용해 각 PC에서 caller state를 복원해야 한다. Signal, exception, JIT, coroutine이 섞이면 하나의 논리 호출 흐름이 여러 종류의 frame으로 이어진다. 이 PART는 **call frame 복원에서 symbolization과 crash-report latency까지** 추적한다.

---

## CHAPTER 01 · call frame은 함수 호출 시점의 복원 가능한 machine state다

함수 호출은 return address, stack pointer 변화, callee-saved register, local storage를 만든다. 하지만 이 정보가 모두 stack에 고정된 형식으로 저장되는 것은 아니다. Compiler와 ABI가 어떤 register를 보존하고 stack을 어떻게 정렬하는지에 따라 frame layout이 달라진다.

Unwinder의 목표는 현재 PC/SP와 metadata를 이용해 caller의 PC/SP와 필요한 register를 재구성하는 것이다. Source-level 함수 경계보다 machine-level calling convention이 먼저다.

Hand-written assembly나 JIT code가 ABI 규칙을 따르지 않거나 unwind metadata를 제공하지 않으면 stack trace가 그 지점에서 끊길 수 있다. Correct execution과 diagnosability가 별도 요구라는 뜻이다.

## CHAPTER 02 · stack pointer는 현재 frame의 경계를 추적하지만 함수 내에서도 계속 움직일 수 있다

Function prologue가 stack space를 확보하고 epilogue가 되돌리는 전형적 pattern이 있지만 optimizer는 필요한 시점에만 조정하거나 dynamic allocation을 사용할 수 있다. Interrupt나 signal이 instruction 중간에 발생하면 SP가 함수 entry 기준과 다른 상태일 수 있다.

Unwind rule은 PC 위치별로 현재 stack layout을 설명해야 한다. 한 function 전체에 고정 offset을 가정하면 prologue/epilogue 중간 sample에서 잘못된 caller를 복원한다.

Crash unwinder는 arbitrary instruction boundary에서 시작할 수 있다는 전제로 구현한다. 정상 call boundary에서만 test하면 부족하다.

## CHAPTER 03 · CFA는 caller frame을 복원하기 위한 기준 주소를 추상화한다

Call Frame Address는 현재 instruction 위치에서 caller의 stack frame을 설명하는 기준점으로 사용된다. CFI는 CFA를 어떤 register+offset으로 계산하고 saved register가 CFA 기준 어디에 있는지 규칙으로 표현한다.

이 abstraction 덕분에 stack pointer가 함수 내부에서 움직여도 unwinder가 일정한 caller view를 재구성할 수 있다. Frame pointer가 있는 경우에는 규칙이 단순해질 수 있지만 필수는 아니다.

CFI parser는 arithmetic overflow와 invalid register 번호를 검증해야 한다. Debug metadata도 crash input과 결합되면 untrusted parser surface가 될 수 있다.

## CHAPTER 04 · callee-saved register는 caller가 기대하는 값을 callee가 복원해야 한다

ABI는 어떤 register를 caller가 보존해야 하고 어떤 register를 callee가 보존해야 하는지 정의한다. Callee가 saved register를 stack에 spill하면 unwind metadata는 그 위치를 알려야 한다. 그렇지 않으면 상위 frame을 복원하는 데 필요한 state가 사라진다.

Compiler는 register allocation에 따라 실제 save set을 최소화한다. 모든 function이 동일 register를 저장하지 않는다.

Unwind correctness test는 다양한 optimization과 register pressure를 가진 code를 포함한다. 단순 leaf function stack만으로 metadata를 검증하지 않는다.

## CHAPTER 05 · return address의 위치는 architecture와 calling convention에 따라 달라진다

어떤 architecture는 call instruction이 return address를 stack에 push하고, 다른 architecture는 link register에 저장한 뒤 필요할 때 spill한다. Tail call은 현재 function의 return address를 별도로 남기지 않을 수 있다.

따라서 “stack word 중 executable address처럼 보이는 값”을 찾는 heuristic은 false positive가 많다. Data pointer나 integer가 우연히 code range에 들어갈 수 있다.

정확한 unwinding은 CFI/FP 같은 구조적 정보를 우선하고 heuristic scan은 최후의 진단 보조로 구분한다.

## CHAPTER 06 · frame pointer는 unwinding을 단순화하지만 optimization 비용과 trade-off가 있다

전통적인 frame-pointer chain은 각 frame이 이전 frame pointer와 return address를 일정 위치에 저장해 빠른 stack walk를 가능하게 한다. Compiler가 frame pointer를 일반 register로 재사용하면 code generation 자유도가 늘지만 metadata 기반 unwinder가 필요해진다.

Modern CPU와 compiler에서는 frame pointer 유지 비용이 workload마다 다르다. Observability 요구가 큰 production에서는 약간의 register cost를 감수하고 frame pointer를 켜는 선택도 가능하다.

결정은 profiling overhead, binary size, architecture register pressure를 실제로 측정해 내린다. “항상 끄는 것이 빠르다”는 오래된 일반화를 그대로 적용하지 않는다.

## CHAPTER 07 · CFI state machine은 PC 구간별 register 복원 규칙을 표현한다

Call Frame Information은 instruction sequence에 따라 CFA와 saved register rule이 어떻게 변하는지 작은 state machine처럼 기술한다. Prologue에서 register를 push하면 rule이 추가되고 epilogue에서 restore되면 다시 바뀐다.

Unwinder는 현재 PC에 해당하는 rule state를 계산해야 한다. 잘못된 PC normalization이나 off-by-one은 특히 return address가 call instruction 다음 위치를 가리킬 때 frame symbol을 어긋나게 만든다.

Metadata 생성과 소비가 compiler/toolchain version에 따라 호환되는지 regression corpus로 확인한다.

## CHAPTER 08 · CIE와 FDE는 공통 규칙과 함수별 범위를 분리해 unwind metadata를 압축한다

DWARF 계열 unwind 정보는 공통 augmentation, code/data alignment, return register 같은 정보를 CIE에 두고, 특정 PC range의 rule sequence를 FDE에 둔다. 여러 function이 공통 정보를 공유해 binary overhead를 줄일 수 있다.

Parser는 FDE가 참조하는 CIE가 valid한지, PC range와 encoding이 address space 안인지 검증해야 한다. Corrupt metadata 때문에 crash reporter가 다시 crash하면 원래 사건 증거를 잃는다.

Build pipeline에서 stripped binary를 만들 때 unwind section을 의도치 않게 제거하지 않는지 확인한다. Debug symbol과 exception unwind 정보는 목적과 retention이 다를 수 있다.

## CHAPTER 09 · virtual unwind는 실제 stack을 수정하지 않고 register context를 한 frame씩 계산한다

Profiler와 crash reporter는 실행을 되돌리는 것이 아니라 현재 context의 copy에 unwind rule을 적용해 caller context를 만든다. 이를 반복하면 call chain을 얻는다. 실제 stack memory나 CPU register를 바꾸지 않는다는 점이 exception unwind와 다르다.

Memory read가 fault할 수 있으므로 remote/core-dump unwinder는 safe read abstraction을 사용해야 한다. Stack bounds 밖을 읽지 않게 thread stack range를 확인한다.

Frame count와 total bytes read에 limit을 둔다. Corrupt chain이 cycle을 만들거나 거대한 loop를 유도하지 않게 한다.

## CHAPTER 10 · tail call은 중간 함수의 physical frame을 없앨 수 있다

Compiler가 `return f(x)`를 jump로 바꾸면 현재 함수 frame을 재사용하고 별도 return address를 남기지 않는다. Stack trace에서 source-level 함수 하나가 사라지는 것은 unwinder bug가 아니라 최적화 결과일 수 있다.

Debug metadata가 tail-call 정보를 제공할 수 있지만 모든 profiler가 logical frame을 복원하는 것은 아니다. Incident에서 missing frame을 곧바로 stack corruption으로 판단하지 않는다.

성능 build와 debug build의 stack shape가 달라질 수 있으므로 production binary로 진단 tool을 검증한다.

## CHAPTER 11 · inlining은 여러 source-level 호출을 하나의 machine frame에 합친다

Inline된 함수는 별도 stack frame을 만들지 않지만 debug info는 instruction range가 어느 inline call chain에 속하는지 기록할 수 있다. Symbolizer가 이를 사용하면 한 physical frame을 여러 logical frame으로 확장해 보여줄 수 있다.

Debug info가 stripped되면 profiler는 outer function 하나만 표시할 수 있다. 이 차이는 CPU attribution에도 영향을 준다.

Build ID와 정확히 일치하는 symbol file이 있어야 inline chain을 복원할 수 있다. 다른 commit의 symbol을 쓰면 그럴듯하지만 틀린 stack이 나올 수 있다.

## CHAPTER 12 · optimized variable은 source 위치와 값이 항상 존재하지 않는다

Optimizer는 local variable을 register에 두거나 constant-fold하거나 완전히 제거할 수 있다. Crash 시 debugger가 `<optimized out>`을 표시하는 것은 정보 부족이지 debugger failure가 아니다. Debug info는 PC range별 variable location expression을 제공할 수 있다.

변수가 여러 register/stack location으로 이동할 수 있어 arbitrary sample 시점에서 복원이 복잡하다. Production crash 분석에서 모든 local 값을 기대하지 않는다.

중요 invariant는 별도 structured logging이나 event trace로 남기는 것이 더 신뢰할 수 있다. Stack dump만으로 business state 전체를 복원하려 하지 않는다.

## CHAPTER 13 · exception unwinding은 frame을 단순 관찰하는 것이 아니라 실제 cleanup을 실행한다

Language exception이 throw되면 runtime은 handler를 찾으며 stack을 unwind하고 scope destructor/finally를 실행할 수 있다. 이 과정은 virtual stack trace와 달리 실제 control flow와 resource lifetime을 변경한다.

Unwind metadata가 잘못되면 cleanup이 누락되거나 잘못된 landing pad로 갈 수 있다. Foreign language/FFI boundary를 exception이 넘어갈 수 있는지도 ABI contract에 달려 있다.

Exception path는 정상 return보다 실행 빈도가 낮으므로 sanitizer와 forced throw test로 깊은 stack을 검증한다.

## CHAPTER 14 · personality routine은 각 frame의 language-specific exception 규칙을 판단한다

Unwinder는 공통 stack traversal을 제공하더라도 C++, Rust, managed runtime은 어떤 exception을 catch하고 어떤 cleanup을 실행할지 서로 다른 규칙을 가진다. Personality routine은 metadata를 해석해 search phase와 cleanup phase에 필요한 결정을 한다.

잘못된 personality pointer나 corrupt LSDA는 security-sensitive parser 문제가 될 수 있다. Toolchain이 생성한 metadata를 그대로 신뢰하더라도 binary corruption을 고려한다.

Cross-language exception은 지원 여부를 명시하고, 지원하지 않으면 boundary에서 catch/translate한다.

## CHAPTER 15 · landing pad는 exception 경로에서 control을 넘겨받는 compiler-generated code다

Try/catch와 cleanup scope는 machine code에서 landing pad와 exception table로 표현될 수 있다. 정상 control flow에서는 거의 실행되지 않아 coverage가 낮기 쉽다. 그런데 resource 해제와 state rollback이 집중되어 있어 correctness 중요도는 높다.

Optimizer가 normal path와 landing pad를 공유/분리할 수 있으므로 source line만 보고 flow를 추정하기 어렵다. Disassembly와 exception metadata가 도움이 된다.

Fault injection으로 constructor 중간 throw, nested cleanup throw 같은 edge case를 검증한다. Double exception policy도 runtime별로 이해한다.

## CHAPTER 16 · signal frame은 kernel이 user stack에 저장한 비동기 context를 unwind chain에 삽입한다

Signal이 도착하면 kernel은 interrupted register state를 signal frame 형태로 저장하고 handler로 control을 넘긴다. Stack trace는 handler frame에서 이 special layout을 인식해 원래 interrupted PC로 돌아가야 한다.

일반 call instruction으로 만들어진 frame이 아니므로 보통 별도 trampoline/sigreturn 규칙이 필요하다. 이를 모르면 stack이 handler에서 끊긴다.

Crash report에서 signal delivery frame을 표시하면 실제 faulting code와 handler code를 구분하기 쉽다.

## CHAPTER 17 · alternate signal stack은 stack overflow 같은 상황에서도 handler가 실행될 공간을 제공한다

기본 user stack이 overflow되거나 guard page에 닿은 상태에서 같은 stack으로 signal handler를 실행하려 하면 handler frame을 만들 공간이 없을 수 있다. Alternate signal stack을 설정하면 별도 메모리에서 crash handler를 실행할 수 있다.

Alt stack 자체도 충분한 크기와 alignment가 필요하고 nested signal을 고려해야 한다. 너무 복잡한 handler는 alt stack을 다시 고갈시킬 수 있다.

Stack-overflow recovery를 시도하기보다 최소 context를 안전하게 기록하고 종료하는 것이 일반적으로 낫다.

## CHAPTER 18 · async unwinding은 임의 instruction 시점의 metadata correctness를 요구한다

Sampling profiler와 signal-based crash collector는 함수 entry/exit가 아닌 어느 instruction에서도 중단될 수 있다. Unwind metadata가 call site만 맞고 prologue 중간을 설명하지 못하면 stack이 깨진다.

Compiler가 asynchronous unwind table을 충분히 생성하는지 build flag를 확인한다. Binary size 절감을 위해 metadata를 줄일 때 observability trade-off가 생긴다.

Random PC sampling corpus로 unwind success rate를 측정할 수 있다. 몇 개 hand-picked stack만으로는 coverage가 부족하다.

## CHAPTER 19 · stack corruption은 unwinder의 입력 자체를 공격한다

Buffer overflow가 saved return address나 frame pointer를 덮으면 정상 CFI가 있어도 복원할 memory가 손상된다. Unwinder가 invalid address를 따라가며 추가 fault를 내지 않도록 stack bounds와 executable mapping을 검증한다.

Canary와 shadow stack 같은 hardening은 corruption 탐지/방지에 도움을 주지만 crash report가 항상 완전해지는 것은 아니다.

Corrupt stack에서는 partial trace라도 보존하고 frame confidence를 표시하는 것이 좋다. Heuristic scan 결과를 확정적 call chain처럼 제시하지 않는다.

## CHAPTER 20 · unwind metadata parser도 malformed binary를 받는 공격 표면이다

Crash dump, plugin, downloaded executable을 분석하는 tool은 공격자가 만든 CFI를 읽을 수 있다. Variable-length encoding, pointer arithmetic, recursive augmentation을 bounded하게 처리해야 한다.

Frame count, expression instruction 수, metadata size에 limit을 둔다. Parser crash가 분석 system 전체를 중단하지 않게 sandbox할 수 있다.

Fuzz corpus에 truncated CIE/FDE, invalid pointer encoding, cyclic reference를 포함한다. Debug tool도 production parser와 같은 보안 기준을 적용한다.

## CHAPTER 21 · symbolization은 numeric PC를 함수·파일·라인 정보로 변환한다

Unwinding이 `0x7f...` 주소 목록을 만들면 symbolizer가 module base와 symbol/debug info를 이용해 사람이 이해할 이름과 source location으로 바꾼다. 이 단계가 틀려도 unwind 자체는 맞을 수 있다.

PIE/shared library에서는 runtime load address를 빼서 object-relative address를 계산해야 한다. Split debug file을 사용할 경우 exact binary identity가 필요하다.

Raw PC와 module offset을 crash report에 함께 보존한다. Symbol server가 unavailable해도 나중에 재처리할 수 있다.

## CHAPTER 22 · build ID는 runtime binary와 symbol artifact를 정확히 연결하는 key다

파일 이름과 version string만으로는 rebuild된 binary를 구분하지 못할 수 있다. Build ID나 content hash를 사용하면 crash의 module과 정확한 symbol file을 매칭할 수 있다.

CI가 strip된 production binary와 debug artifact를 같은 build에서 생성해 ID로 저장한다. Source commit만 같아도 compiler flag가 다르면 address layout이 달라질 수 있다.

Symbol server retention은 crash retention보다 길거나 같아야 과거 incident를 분석할 수 있다.

## CHAPTER 23 · ASLR 환경에서는 runtime address를 module-relative offset으로 정규화해야 한다

같은 binary도 process마다 load base가 달라질 수 있으므로 absolute address는 run 간 비교에 부적합하다. Memory map에서 module base를 찾아 offset을 계산하면 symbolization과 stack aggregation이 가능하다.

JIT, anonymous executable mapping은 일반 ELF module처럼 build ID가 없을 수 있어 별도 metadata가 필요하다.

Crash collector는 `/proc` map 같은 mapping snapshot을 함께 저장한다. 나중에 process가 사라진 뒤에는 base 정보를 복구하기 어렵다.

## CHAPTER 24 · JIT code는 runtime 생성 code와 symbol lifecycle을 함께 관리해야 한다

JIT compiler가 code cache에 machine code를 만들면 static symbol table에는 해당 주소가 없다. Profiler가 의미 있는 이름을 보려면 runtime이 code range, method name, generation 정보를 외부에 제공해야 한다.

Code cache eviction이나 recompilation으로 같은 address가 다른 method에 재사용될 수 있다. Timestamp/generation 없이 나중에 symbolizing하면 잘못된 이름을 붙일 수 있다.

JIT event stream과 sample time을 연결한다. Snapshot 한 번으로 long-running profile 전체를 해석하지 않는다.

## CHAPTER 25 · managed-native mixed stack은 서로 다른 unwinder를 연결해야 한다

Managed runtime frame은 native ABI frame과 다른 metadata와 stack convention을 사용할 수 있다. JNI/FFI boundary에서 native unwinder와 runtime stack walker를 전환해야 전체 call chain을 얻는다.

GC safepoint와 moving stack/object state 때문에 arbitrary-time walking 제약도 runtime별로 다르다. Official profiler interface를 사용하는 것이 안전하다.

Crash report는 managed/native frame 타입을 표시해 symbol source와 신뢰도를 구분한다. 한쪽만 수집하면 원인 call chain이 반으로 잘릴 수 있다.

## CHAPTER 26 · coroutine stack은 논리 call chain이 물리 OS stack 밖에 저장될 수 있다

Stackless coroutine은 suspension 시 local state를 heap frame에 저장하고 OS stack을 반환한다. Resume가 다른 thread에서 일어나면 physical stack trace에는 scheduler만 보이고 이전 await chain이 사라질 수 있다.

Runtime은 async stack metadata를 이용해 logical parent를 연결할 수 있다. 이는 ABI unwinding과 별도의 graph reconstruction이다.

Incident 분석에서 physical stack과 logical async stack을 구분해 제공한다. 둘을 억지로 하나로 합치면 실제 thread ownership을 오해할 수 있다.

## CHAPTER 27 · unwind bounds는 corrupt chain과 pathological depth가 분석기를 고갈시키지 않게 한다

최대 frame 수, stack address range, metadata instruction 수를 제한해 unwinder가 무한 loop나 거대한 memory read를 하지 않게 한다. Frame pointer chain이 자기 자신을 가리키는 corruption도 cycle detection으로 끊는다.

Bound에 도달하면 partial trace와 truncation reason을 남긴다. 아무 결과도 버리는 것보다 incident에 유용하다.

Crash reporter는 원래 application이 memory pressure 상태일 수 있음을 고려해 preallocated/bounded buffer를 사용한다.

## CHAPTER 28 · unwind latency는 sampling profiler의 overhead와 crash handler 안정성에 직접 연결된다

1000Hz profiler가 매 sample마다 깊은 stack을 걷으면 CPU overhead가 커질 수 있다. CFI interpretation과 remote memory read 비용이 frame 수에 비례한다. Frame pointer 기반 walking이 observability 때문에 선택되는 이유가 여기 있다.

Sampling depth를 제한하거나 stack cache를 사용할 수 있지만 attribution 정확도와 trade-off가 있다.

Profiler overhead를 workload CPU와 함께 측정한다. Tool을 켠 상태에서 성능 regression을 측정하면 instrumentation cost가 결과에 섞일 수 있다.

## CHAPTER 29 · frame normalization은 같은 call site를 안정된 key로 집계하게 한다

Return address는 보통 call instruction 다음 PC를 가리키므로 symbolization 전에 architecture 규칙에 따라 call-site PC로 조정할 수 있다. Inline frame와 tail call도 aggregation key에 영향을 준다.

ASLR base를 제거하고 build ID+offset으로 정규화하면 여러 process의 동일 code path를 합칠 수 있다. Source line만 key로 쓰면 optimization/rebuild에서 흔들릴 수 있다.

Profiling backend는 raw address와 normalized key를 모두 보존해 필요할 때 재분석할 수 있게 한다.

## CHAPTER 30 · unwind contract는 machine state·metadata·symbol identity 세 요소를 함께 보존한다

정확한 stack trace에는 fault/sample 시점의 register context, 그 binary에 맞는 unwind metadata, 정확히 일치하는 symbol artifact가 필요하다. 셋 중 하나가 없으면 partial 또는 잘못된 trace가 된다.

Optimized code의 tail call·inline·frame-pointer omission은 정상이며, signal·JIT·coroutine은 별도 frame semantics를 추가한다. Tool은 각 frame의 provenance를 구분해야 한다.

최종 검증은 production build에서 다양한 PC를 실제 unwind하고 known call chain과 비교하는 것이다. Debug build의 깨끗한 stack 몇 개만으로 production observability를 PASS라고 부르지 않는다.
