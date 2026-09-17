# PART 56 · Stack Unwinding and Exception Metadata — frames, CFA, CFI, signals, optimized code

Stack trace는 `함수 이름 목록`이 아니다. 실행 중 각 activation의 **register state, stack pointer 변화, return address, saved register 위치, compiler optimization, asynchronous interruption**을 역산한 결과다. 이 복원 규칙이 없거나 틀리면 crash dump는 존재해도 호출 경로를 신뢰할 수 없다.

## CHAPTER 01 · Call frame은 함수의 소스 블록이 아니라 실행 activation이다

같은 함수가 재귀·동시 호출되면 서로 다른 activation이 존재한다. 각 activation은 현재 code location, stack storage, caller에게 돌려줄 return state, preserved register 상태를 갖는다. Debugger가 복원해야 하는 대상은 함수 정의가 아니라 **특정 시점의 activation state**다.

## CHAPTER 02 · Stack pointer는 frame identity 그 자체가 아니다

Function prologue가 local storage를 확보하고 alignment를 맞추며 register를 save하면 stack pointer는 함수 실행 중 여러 값으로 변할 수 있다. Dynamic allocation, variable-size object, outgoing argument area가 있으면 더 복잡해진다. 따라서 unwind는 현재 SP 하나를 `이 frame의 시작`이라고 가정하지 않고 별도 canonical rule을 사용해야 한다.

## CHAPTER 03 · Canonical Frame Address는 caller state를 복원하기 위한 기준점이다

DWARF CFI의 CFA는 일반적으로 이전 frame의 call-site stack 상태를 표현하는 architecture-independent 기준이다. 각 code range에서 CFA를 어떤 register+offset으로 계산하는지 기술하면 debugger는 prologue 중간·epilogue 중간에서도 predecessor frame을 복원할 수 있다. CFA는 source-level local variable scope가 아니라 **unwind 계산의 좌표계**다.

## CHAPTER 04 · Callee-saved register는 unwind metadata 없이는 위치를 알 수 없다

ABI가 특정 register를 preserved로 정해도 callee가 실제로 어디에 저장했는지는 code generation에 따라 달라진다. Stack slot에 save할 수도 있고 아직 save하지 않았을 수도 있으며 shrink-wrapping으로 prologue가 분산될 수도 있다. Unwind rule은 각 PC range에서 predecessor register value를 어디서 얻는지 기술해야 한다.

## CHAPTER 05 · Return address도 항상 stack top에 있지 않다

Architecture와 ABI에 따라 return address가 link register에 있거나 stack에 spill될 수 있다. Leaf function은 stack frame을 거의 만들지 않을 수도 있다. 최적화가 진행되면 return-address 저장 위치가 code range에 따라 달라질 수 있으므로 `SP+고정 offset` 규칙만으로 stack walking을 구현하면 깨진다.

## CHAPTER 06 · Frame pointer는 unwind의 한 전략이지 보편적 진실이 아니다

Compiler가 dedicated frame pointer를 유지하면 linked-frame 형태의 walking이 단순해질 수 있다. 그러나 register pressure를 줄이거나 optimization freedom을 얻기 위해 frame pointer를 생략할 수 있고, leaf/optimized frame은 전통적 chain을 갖지 않을 수 있다. Production profiling에서 frame-pointer 정책은 performance, binary size, observability를 함께 바꾸는 build contract다.

## CHAPTER 07 · CFI는 code address range마다 register 복원 규칙을 바꾼다

Prologue 전, 일부 register save 후, stack allocation 후, epilogue 중에는 같은 function 안에서도 unwind 규칙이 다르다. CFI는 instruction address 진행에 따라 CFA rule과 saved-register rule을 업데이트한다. 이 때문에 unwind metadata는 function당 단일 record가 아니라 **code location에 따른 state machine**으로 이해해야 한다.

## CHAPTER 08 · CIE와 FDE는 공통 규칙과 code-range 규칙을 분리한다

DWARF 계열 CFI는 공통 encoding·return-address register 같은 정보를 Common Information Entry에 두고, 특정 PC range의 unwind instruction을 Frame Description Entry에 둘 수 있다. 이 분리는 metadata 중복을 줄이면서 여러 function/range에 서로 다른 unwind state를 기술하게 한다. Parser는 length, augmentation, encoding을 엄격히 검증해야 한다.

## CHAPTER 09 · Virtual unwind는 실제 process state를 바꾸지 않는다

Debugger가 stack을 걷는 과정은 CPU register를 실제로 이전 상태로 되돌리는 것이 아니다. Current frame metadata로 predecessor의 virtual CFA, register value, return location을 계산하고 그 virtual state에서 다음 frame 규칙을 적용한다. 따라서 unwind failure는 program control flow failure가 아니라 **state reconstruction failure**다.

## CHAPTER 10 · Tail call은 logical caller와 physical return chain을 다르게 만든다

Tail-call optimization은 caller frame을 유지하지 않고 callee로 control을 넘길 수 있다. 그러면 hardware return chain만 보면 source-level call path 일부가 사라진다. Debug metadata가 call-site/tail-call 정보를 제공하지 않으면 profiler가 `누가 이 함수를 호출했는가`를 완전히 복원할 수 없다.

## CHAPTER 11 · Inlining은 source function을 physical frame 없이 존재하게 한다

Inline된 function은 별도 stack frame을 만들지 않아도 source-level call hierarchy에는 존재한다. Symbolizer가 inline metadata를 사용하면 한 physical PC에서 여러 logical inline frame을 표현할 수 있다. `stack depth=함수 호출 깊이`라고 가정하면 optimized build의 call graph를 잘못 읽게 된다.

## CHAPTER 12 · Optimized-out variable은 debugger 실패가 아니라 location-lifetime 문제일 수 있다

Compiler는 value를 register에 두었다가 없애거나 상수 전파로 실제 storage를 제거할 수 있다. DWARF location list는 PC range마다 variable이 어느 location/expression으로 표현되는지 기술할 수 있지만 모든 순간에 value가 materialized돼 있다는 보장은 없다. 디버거가 `optimized out`을 표시하는 것은 metadata와 machine state로 값을 재구성할 수 없다는 뜻이다.

## CHAPTER 13 · Exception unwind는 stack trace와 비슷해 보여도 semantic goal이 다르다

Debugger unwind는 관측을 위해 predecessor state를 계산하지만 language exception unwind는 handler를 찾고 stack cleanup을 실제로 진행해야 한다. C++ 계열 zero-cost exception 구현은 정상경로 비용을 줄이는 대신 unwind table과 personality logic을 사용해 예외 시 cleanup/action을 결정할 수 있다. 같은 unwind metadata family를 써도 목적은 다르다.

## CHAPTER 14 · Personality routine은 frame별 exception semantics를 해석한다

Runtime은 각 frame의 language-specific metadata를 읽어 해당 exception을 catch할 handler가 있는지, cleanup destructor가 필요한지 판단한다. Handler search phase와 cleanup phase를 분리하는 ABI도 있다. `throw 하면 stack을 거꾸로 pop한다`는 설명만으로는 destructors, landing pad, foreign-language frame을 설명할 수 없다.

## CHAPTER 15 · Landing pad는 source catch block과 일대일일 필요가 없다

Compiler는 여러 cleanup/catch 경로를 machine-level landing pad와 table entry로 조직할 수 있다. 동일 source construct가 여러 machine block으로 나뉘거나 여러 source handler가 metadata table을 공유할 수 있다. Crash disassembly를 읽을 때 source syntax만으로 exception edge를 추정하면 틀릴 수 있다.

## CHAPTER 16 · Signal frame은 ordinary call frame과 다른 비동기 경계다

Signal은 현재 instruction stream의 임의 지점에서 user handler로 control을 옮길 수 있다. Kernel은 interrupted register state를 user stack의 signal frame 등에 저장하고 handler return 시 복원한다. Unwinder가 signal trampoline/frame을 이해하지 못하면 handler 아래의 원래 call stack을 잃는다.

## CHAPTER 17 · Alternate signal stack은 stack-overflow 진단의 생존 경로가 될 수 있다

원래 thread stack이 guard page를 침범하거나 심하게 손상된 상황에서 같은 stack으로 signal handler를 실행하면 진단 코드조차 실행되지 못할 수 있다. Alternate signal stack은 fatal signal handler가 별도 stack 공간을 사용하게 할 수 있다. 그러나 크기·nested signal·async-signal-safety까지 고려하지 않으면 복구 경로가 새로운 failure point가 된다.

## CHAPTER 18 · Async stack sampling은 instruction boundary에서 frame을 읽는다

Profiler interrupt/signal이 prologue·epilogue 중간에 발생할 수 있으므로 unwind metadata는 non-steady-state PC에서도 유효해야 한다. Compiler가 save/restore를 재배치하면 frame layout이 함수 시작과 끝에서만 일정하다는 가정이 깨진다. Sampling profiler 품질은 unwind table의 PC-granularity 정확도에 크게 의존한다.

## CHAPTER 19 · Stack corruption은 unwind metadata보다 실제 memory가 먼저 깨진 경우다

Buffer overwrite가 saved return address나 frame data를 덮으면 metadata가 정확해도 predecessor state가 거짓이 된다. 이때 unwinder가 invalid address를 따라 연쇄적으로 nonsense frame을 만들 수 있다. Hardened unwinder는 executable range, stack bounds, canonical address, loop detection 같은 sanity check로 손상 확산을 제한해야 한다.

## CHAPTER 20 · Unwind table corruption도 신뢰 경계를 가진다

Binary가 손상되었거나 malicious input이 symbolizer/unwinder에 들어가면 length/offset/expression parser가 공격 surface가 된다. Debug/unwind metadata를 `실행 안 되는 정보`라고 가볍게 취급하면 bounds overflow와 recursive expression 같은 parser bug를 놓친다. Crash-processing infrastructure도 untrusted binary metadata를 방어적으로 파싱해야 한다.

## CHAPTER 21 · Symbolization은 address→function 하나 이상의 변환이다

PC를 symbol name으로 바꾸려면 load address, relocation/ASLR slide, module mapping, symbol table, debug info를 맞춰야 한다. Source file/line과 inline call chain까지 복원하려면 line table과 inline DIE 정보가 추가로 필요하다. 같은 raw PC도 잘못된 binary build ID를 사용하면 그럴듯하지만 틀린 symbol로 변환될 수 있다.

## CHAPTER 22 · Build ID와 symbol artifact 보존은 production crash 분석의 전제다

Release binary와 정확히 대응하는 symbols/unwind metadata가 없으면 crash address는 재현 불가능해질 수 있다. Strip된 production binary를 배포하더라도 별도 symbol artifact를 build ID로 보존하면 offline symbolization이 가능하다. `최신 symbols로 과거 crash 분석`은 주소 배치가 달라져 잘못된 결과를 낼 수 있다.

## CHAPTER 23 · ASLR/PIE 환경에서는 runtime load address를 제거해야 한다

Position-independent executable/shared object는 실행마다 다른 base address에 mapping될 수 있다. Crash PC에서 module load base를 빼거나 relocation context를 반영해 object-relative address를 얻어야 symbol table과 일치한다. Module mapping 정보가 누락된 tombstone은 동일 숫자 주소만으로 source를 찾기 어렵다.

## CHAPTER 24 · JIT code는 static binary symbol table 밖에서 생성된다

Runtime-generated code는 file-backed ELF symbol만으로는 이름과 source를 알 수 없다. JIT runtime은 code cache range, compiled method identity, deoptimization/inline metadata를 profiler나 crash tooling에 노출해야 한다. Code cache가 재사용된 뒤 늦게 symbolization하면 같은 address가 다른 compiled body를 가리킬 수 있어 timestamp/version binding이 필요하다.

## CHAPTER 25 · Mixed managed/native stack은 서로 다른 unwind 체계를 연결한다

JNI/FFI boundary에서는 managed frame metadata와 native ABI unwind metadata가 교차한다. 한쪽 runtime이 safepoint/deopt 상태를 알고 다른 쪽 unwinder가 ELF CFI를 아는 식으로 책임이 나뉜다. Boundary frame을 식별하지 못하면 stack trace가 native에서 끊기거나 managed frame을 잘못 해석한다.

## CHAPTER 26 · Coroutine stack은 physical thread stack과 logical async stack이 다르다

Suspended coroutine은 continuation object에 logical caller state를 저장하고 physical thread stack에서는 사라질 수 있다. 재개 시 다른 worker thread에서 실행될 수도 있다. 따라서 thread stack만 수집하면 async causal chain이 끊긴다. Runtime이 continuation parent/link metadata를 별도로 제공해야 logical async stack을 복원할 수 있다.

## CHAPTER 27 · Stack trace depth 제한은 비용뿐 아니라 정보 손실 정책이다

깊은 recursion·framework stack에서 frame cap을 작게 두면 root cause가 잘릴 수 있고, 무제한 unwind는 corrupt stack에서 CPU 시간을 과도하게 소비할 수 있다. Production unwinder는 max frame count, stack-range bound, cycle detection, per-frame validation을 함께 사용해 정보량과 안전성을 조절해야 한다.

## CHAPTER 28 · Unwind latency는 incident pipeline 용량을 결정한다

대규모 crash storm에서 각 dump의 symbolization/unwind가 비싸면 crash backend 자체가 overload될 수 있다. Inline expansion, remote symbol fetch, demangling, debug-info parsing 비용을 cache하고 build-ID 단위로 공유해야 한다. Observability pipeline도 queueing system이므로 p99 unwind time과 backlog를 운영 지표로 봐야 한다.

## CHAPTER 29 · Stack trace 비교는 address가 아니라 normalized frame identity로 해야 한다

ASLR, inlining, code layout 변화 때문에 raw PC sequence는 build마다 달라진다. Incident deduplication은 module build ID, function/inline identity, normalized source location, top-N semantic frames 등을 사용해야 안정적이다. 너무 공격적인 normalization은 서로 다른 crash를 하나로 합치고, 너무 세밀하면 동일 root cause를 수천 signature로 쪼갠다.

## CHAPTER 30 · 신뢰할 수 있는 stack trace는 compiler·ABI·runtime·artifact 보존의 합성 결과다

완전한 unwind는 **정확한 ABI 규칙, PC별 CFI, intact stack memory, signal/exception boundary 처리, JIT/async metadata, 올바른 build symbols**가 동시에 필요하다. Stack trace를 사실 그 자체로 읽지 말고 어떤 frame이 어떤 metadata와 memory evidence로 복원됐는지 확인해야 한다. 잘못된 unwind 한 frame이 이후 전체 caller chain을 오염시킬 수 있으므로 첫 비정상 frame에서 신뢰도를 분리하는 것이 중요하다.
