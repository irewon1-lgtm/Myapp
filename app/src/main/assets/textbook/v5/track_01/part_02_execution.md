# PART 02 · 실행 경로 — source에서 process state까지

소스 파일은 실행이 아니라 **프로그램을 생성하기 위한 입력 표현**이다. 실제 실행에는 parsing·semantic analysis·IR·optimization·code generation 또는 bytecode/runtime·link/load·process construction·CPU state transition이 연속으로 관여한다. 실패를 빠르게 좁히려면 이 계층을 분리해야 한다.

---

## CHAPTER 01 · parser가 만든 구조와 의미 검증은 다른 단계다

compiler/runtime frontend는 source bytes를 문자로 decode한 뒤 lexical structure와 grammar를 해석한다. parser가 AST를 만들었다고 프로그램이 의미적으로 유효한 것은 아니다. 이름 resolution, type checking, definite assignment, control-flow 제약처럼 **문법 이후의 semantic rule**이 별도로 존재한다.

```text
source bytes
→ decoding
→ tokens
→ syntax tree / AST
→ name & type resolution
→ semantic checks
```

따라서 오류를 `compile error` 하나로 묶지 않는다. syntax failure, unresolved symbol, type mismatch, illegal control flow는 서로 다른 invariant를 깨뜨린다. compiler diagnostic을 읽을 때는 **어느 representation까지 생성됐고 어느 검증에서 중단됐는지** 확인한다.

source-level macro, annotation processing, code generation이 있다면 사용자가 쓴 text와 compiler가 실제로 처리한 source가 다를 수도 있다. generated source와 build artifact를 증거로 남겨야 하는 이유다.

---

## CHAPTER 02 · IR은 최적화 가능한 계약이고 verifier가 필요하다

현대 compiler는 high-level source를 곧바로 final instruction으로 바꾸지 않고 intermediate representation을 사용할 수 있다. LLVM IR처럼 typed SSA 기반 IR은 source language보다 낮은 수준이지만 machine instruction보다 높은 추상화를 제공한다.

중요한 구분은 **parseable IR과 valid IR가 같지 않다**는 점이다. 문법상 읽을 수 있어도 dominance, type, control-flow 같은 IR invariant를 깨뜨릴 수 있으며 verifier가 이를 검출한다.

optimization은 “코드를 더 짧게 만드는 단계”가 아니다. compiler는 language semantics가 허용하는 범위에서 constant propagation, dead-code elimination, inlining, vectorization, loop transformation 등을 수행한다. 따라서 undefined behavior가 있는 source는 programmer가 기대한 실행 경로 자체를 compiler가 보존할 의무가 없을 수 있다.

실전 디버깅에서는 optimization level에 따라 증상이 사라지거나 나타나는 경우 다음을 구분한다.

```text
source UB / race
compiler bug
optimizer-sensitive timing
uninitialized state
debug/release build configuration difference
```

---

## CHAPTER 03 · AOT, interpreter, bytecode, JIT는 조합될 수 있다

`compiled language`와 `interpreted language`의 이분법은 실제 runtime을 설명하기에 부족하다. 실행 전략은 서로 조합될 수 있다.

```text
source → native code ahead of time
source → bytecode → interpreter
source/bytecode → runtime profiling → hot path JIT
source → IR → target-specific code
```

JIT runtime은 실행 중 profile을 수집해 hot code를 최적화할 수 있고 assumption이 깨지면 deoptimization을 수행할 수 있다. startup latency, steady-state throughput, code-cache memory, battery/thermal cost는 서로 다른 최적화 목표다.

Android ART 역시 interpretation, JIT, AOT compilation을 조합한다. 따라서 “APK 안의 Kotlin이 그대로 CPU에서 실행된다”는 설명은 틀리다. managed code는 DEX/runtime 계층을 거치고 native library는 ABI에 직접 묶인다.

실행 중 생성된 native code는 source artifact와 동일한 lifetime을 갖지 않는다. profile 상태가 달라지면 같은 method도 interpreter frame, baseline code, optimized code 사이를 이동할 수 있고 deoptimization 시점에는 optimized frame의 값을 source-level state로 다시 복원해야 한다. 그래서 JIT 문제를 재현할 때는 source revision만이 아니라 runtime version, profile 상태, compilation tier, code-cache 상태를 함께 기록해야 한다.

---

## CHAPTER 04 · code generation은 ISA와 ABI 두 계약을 동시에 맞춘다

ISA는 CPU가 이해하는 instruction·register·memory operation의 architecture-level 계약이다. ABI는 함수 호출, register 사용, stack alignment, object layout, symbol naming 같은 **binary component 간 상호운용 규칙**을 추가한다.

같은 algorithm도 target ISA와 ABI가 달라지면 instruction sequence와 call boundary가 달라질 수 있다. `arm64-v8a` native library와 x86-64 library가 서로 교환되지 않는 이유는 source language가 아니라 target binary contract가 다르기 때문이다.

code generator는 register allocation, instruction selection, scheduling을 수행한다. source variable 하나가 runtime 내내 register 하나에 고정된다고 가정하면 debugger와 optimized assembly를 잘못 읽게 된다.

---

## CHAPTER 05 · object file은 미완성 주소와 symbol 관계를 가진다

separate compilation에서는 translation unit이 object file로 만들어질 때 final virtual address가 아직 확정되지 않을 수 있다. object file은 code/data와 함께 symbol·relocation 정보를 갖고 linker가 component 관계를 해결할 수 있게 한다.

link failure는 source syntax failure와 별개다. declaration은 보였지만 definition이 link input에 없거나, ABI가 다른 library를 연결하거나, visibility 규칙으로 symbol이 노출되지 않으면 compile 이후에 실패할 수 있다.

```text
compile success
≠ link success
≠ load success
≠ runtime correctness
```

각 단계가 남기는 artifact와 diagnostic을 따로 보존해야 한다.

relocation은 “나중에 주소를 채운다”보다 더 구체적인 계약이다. relocation record는 어느 위치에 어떤 symbol의 어떤 계산 규칙을 적용할지를 나타내며, target ISA와 object format에 따라 허용되는 relocation 종류가 다르다. symbol binding도 local/global/weak, visibility, versioning에 따라 결과가 바뀔 수 있다. 동일 이름의 함수가 존재해도 linker가 어떤 definition을 선택했는지 확인하려면 symbol table과 linker map을 봐야 하며, library 순서나 dead stripping이 결과를 바꾸는 경우도 있다.

---

## CHAPTER 06 · loader는 파일을 process image로 변환한다

executable/shared object는 code bytes만의 집합이 아니다. loader는 segment mapping, permissions, relocation, dynamic dependency, entry point 같은 정보를 사용해 process image를 만든다.

일반적인 process address space에는 다음 범주가 존재할 수 있다.

```text
executable mappings
read-only data
writable data
heap / allocator-managed regions
thread stacks
shared libraries
memory-mapped files
runtime/JIT code regions
```

모든 page가 시작 순간 physical RAM에 resident해야 하는 것은 아니다. virtual mapping과 실제 resident page를 구분해야 startup memory를 정확히 해석할 수 있다.

ASLR 같은 기법으로 실행할 때마다 address가 달라질 수 있으므로 crash address만 저장하고 symbol/module mapping을 잃으면 사후 분석이 어려워진다.

loader 단계에는 권한도 포함된다. code segment가 실행 가능하면서 writable한지, relocation 이후 read-only로 잠기는 영역이 있는지, shared object의 load bias가 얼마인지가 runtime attack surface와 crash 해석에 직접 연결된다. dynamic dependency가 없거나 architecture가 맞지 않는 경우, 혹은 필요한 symbol version을 찾지 못한 경우는 entry point에 도달하기도 전에 실패한다. 따라서 “프로세스가 시작됐다”는 판단은 파일 존재가 아니라 실제 mapping과 dependency resolution이 끝났는지로 확인해야 한다.

---

## CHAPTER 07 · stack frame은 함수 호출의 논리 상태를 담지만 형태는 최적화에 따라 달라진다

함수 호출은 return address, arguments, local state, saved register 등 호출 규약에 필요한 정보를 관리한다. 전통적인 설명에서는 stack frame을 사용하지만 optimized code에서는 일부 local이 register에만 존재하고 frame pointer가 생략되거나 함수가 inline될 수 있다.

따라서 source stack trace와 machine stack unwind 결과가 항상 1:1로 대응한다고 가정하지 않는다. 정확한 symbolication에는 build ID, binary, debug information, unwind metadata가 필요하다.

heap은 “큰 데이터가 가는 곳”이 아니다. lifetime이 lexical call frame과 독립적인 object를 allocator/runtime가 관리하는 영역으로 보는 편이 정확하다. managed runtime에서는 object lifetime과 physical free 시점 사이에 GC policy가 개입한다.

---

## CHAPTER 08 · CPU는 instruction stream과 architectural state를 전이시킨다

machine-level 실행의 최소 모델은 다음 상태다.

```text
instruction pointer / program counter
register file
condition/status state
memory-visible state
```

instruction은 이 상태를 다음 상태로 바꾼다. branch는 program counter 흐름을 바꾸고 load/store는 memory hierarchy를 통해 데이터를 이동시키며 arithmetic instruction은 register/flags를 갱신한다.

실제 microarchitecture는 pipeline, out-of-order execution, speculation을 사용해 instruction을 내부적으로 겹쳐 처리할 수 있다. 그러나 software가 관찰해야 하는 architecture-level semantics는 ISA와 memory model이 규정한다. PART 05에서 이 차이를 더 깊게 다룬다.

이 구분은 crash 분석에서도 중요하다. register dump와 program counter는 architectural state의 관측치지만, 그 직전에 microarchitecture 내부에서 어떤 speculative work가 진행됐는지를 그대로 보여주지는 않는다. exception이 precise하게 정의된 ISA에서는 software가 복구 가능한 경계의 상태가 제공되지만, cache miss나 branch misprediction 같은 내부 사건은 일반 instruction semantics와 다른 관측 경로를 가진다. 따라서 성능 counter와 crash state를 같은 종류의 증거로 취급하지 않는다.

---

## CHAPTER 09 · syscall은 user code가 privileged resource를 요청하는 경계다

application code가 임의의 physical memory, device register, process table을 직접 조작하게 두면 격리가 무너진다. 운영체제는 user mode와 kernel mode의 권한 경계를 두고 system call을 통해 file, memory mapping, process/thread, network, timer 같은 자원을 요청하게 한다.

syscall 비용은 단순 함수 호출과 같지 않지만 모든 I/O가 매 byte마다 syscall을 발생시키는 것도 아니다. libc/runtime buffering, page cache, async I/O, batching이 중간 계층에 존재할 수 있다.

오류 조사에서는 language exception만 보지 말고 최종 OS error code와 syscall context까지 내려갈 필요가 있다.

system call은 함수 이름만으로 결과가 결정되지 않는다. 전달된 descriptor, pointer 범위, flags, 현재 credential·namespace, signal 상태와 kernel resource 상태가 함께 결과를 만든다. 일부 호출은 signal로 중단될 수 있고, I/O 계열은 요청한 길이보다 짧게 성공할 수 있으므로 `return >= 0`과 “요청 전체 완료”를 구분해야 한다. wrapper가 errno를 exception으로 바꾸거나 자동 retry를 수행하면 raw 경계가 가려질 수 있어, 재현 로그에는 syscall 이름·인자 범주·return value·errno를 같은 사건으로 묶어 남기는 편이 안전하다.

---

## CHAPTER 10 · file descriptor는 resource identity와 lifetime을 분리해 봐야 한다

Unix 계열에서 file descriptor는 process-local integer handle이고 kernel의 open-file state를 가리킨다. pathname과 open file description은 같은 개념이 아니다. 파일이 rename/unlink된 뒤에도 열린 descriptor가 유효할 수 있는 이유는 namespace lookup과 열린 resource lifetime이 분리되기 때문이다.

buffered stream, descriptor, kernel page cache, storage durability 역시 서로 다른 상태다. application buffer의 `flush`가 kernel buffer로 전달하는 의미와 storage까지 durable하게 만드는 의미를 혼동하면 crash-consistency 버그가 생긴다.

`dup` 계열 호출이나 `fork` 이후에는 서로 다른 descriptor 번호가 같은 open-file state를 공유할 수 있다. 이때 file offset이나 status flag의 일부는 공유되고 descriptor-local flag는 별도일 수 있다. 반대로 descriptor 번호는 close 후 재사용될 수 있으므로 숫자만 오래 보관하면 전혀 다른 resource를 같은 대상으로 오인하는 ABA형 문제가 생긴다. lifetime을 추적할 때는 “fd=7” 자체보다 언제 열렸고 어떤 kernel object를 가리켰으며 어느 시점에 close됐는지를 사건 순서와 함께 기록해야 한다.

---

## CHAPTER 11 · thread는 shared address space 안에서 독립 execution context를 가진다

같은 process의 thread는 code·heap·open resource를 공유하면서 각자 stack과 register/scheduling state를 가진다. 이 구조 때문에 communication은 빠르지만 shared mutable state에는 race가 생긴다.

scheduler는 runnable task 중 CPU를 받을 대상을 선택한다. runnable, sleeping, blocked, I/O wait 상태를 구분하지 않으면 `CPU가 느리다`는 잘못된 결론에 도달한다. thread dump와 scheduler trace는 **실행 중인지, lock을 기다리는지, I/O completion을 기다리는지**를 분리하는 증거다.

스케줄링 순서는 memory visibility 규칙을 대신하지 않는다. 한 thread가 먼저 실행됐다는 관찰만으로 다른 thread가 그 write를 반드시 같은 순서로 본다고 결론낼 수 없으며, 언어 memory model이 정의한 synchronization edge가 필요하다. lock, atomic, condition variable은 단순 대기 도구가 아니라 shared state publication의 ordering 계약을 함께 제공한다. 따라서 race를 고칠 때 sleep이나 우연한 실행 순서를 추가하는 방식은 증상을 숨길 수 있어도 happens-before 관계를 만들지 못한다.

---

## CHAPTER 12 · Android process는 ART, Zygote, Binder, main thread 계약을 가진다

Android application은 Linux process 위에서 동작하지만 managed runtime과 framework lifecycle이 추가된다. Zygote 계열 process는 공통 runtime state를 미리 준비해 application process 생성 비용을 줄이는 구조를 사용한다. ART는 DEX를 실행하고 device/runtime 조건에 따라 interpretation·JIT·AOT 전략을 조합한다.

Binder는 process 경계를 넘는 IPC의 핵심 메커니즘이다. method call처럼 보이는 framework API가 실제로는 다른 process와 transaction을 수행할 수 있으므로 latency·failure·permission boundary를 local function과 동일하게 다루면 안 된다.

UI main thread는 input dispatch, framework callback, rendering orchestration과 연결된다. long blocking work를 main thread에 두면 CPU 전체가 느린 것이 아니라 **UI event processing deadline을 놓치는 구조적 문제**가 된다.

---

## CHAPTER 13 · 실패 위치는 artifact와 증거로 좁힌다

실행 파이프라인별 대표 증거는 다르다.

```text
frontend        → compiler diagnostic / generated source / AST
IR/optimizer    → IR dump / verifier / optimization diff
codegen         → assembly / object / target flags
link            → symbol table / relocation / linker map
load            → dependency list / loader error / module mapping
runtime         → exception / crash dump / stack / heap state
kernel boundary → syscall trace / errno / scheduler / I/O trace
Android         → logcat / tombstone / ART/Binder/Perfetto evidence
```

증상만 보고 계층을 건너뛰어 수정하면 우연히 증상이 사라져도 원인을 증명하지 못한다. 같은 input과 build artifact에서 failure를 재현하고, 가장 이른 깨진 invariant를 찾는 것이 디버깅의 목표다.

실전에서는 각 단계의 입력과 출력을 고정한 뒤 **처음 달라지는 artifact**를 찾는다. source hash와 compiler flags가 같지만 IR이 다르면 frontend·toolchain state를 의심하고, IR은 같지만 object가 다르면 codegen·target configuration을 좁힌다. object hash까지 같은데 실행만 다르면 loader mapping, runtime state, kernel·device 환경이 후보가 된다. build ID, toolchain version, target triple, dependency digest를 사건과 함께 보존하면 “내 컴퓨터에서는 된다”를 비교 가능한 상태 차이로 바꿀 수 있다.

수정 검증도 같은 경계를 역으로 사용한다. 원인으로 지목한 invariant를 직접 실패시키는 최소 입력을 만들고, 수정 전에는 깨지고 수정 후에는 유지되는지 확인한다. 최종 UI 증상만 사라졌다는 사실보다 중간 artifact와 boundary evidence가 예상대로 바뀌었는지가 더 강한 증거다. 이 방식은 compiler 문제와 runtime 문제를 섞지 않고, 재현성 없는 우연한 성공을 회귀 테스트로 오인하는 위험을 줄인다.

---

## CHAPTER 14 · 실행 성능은 `CPU time + wait + contention + runtime overhead`로 분해한다

wall-clock latency가 길다고 CPU instruction이 많다는 뜻은 아니다. task는 CPU에서 실행하는 시간 외에 I/O, lock, scheduler queue, page fault, GC, runtime compilation을 기다릴 수 있다.

성능 분석의 첫 분해는 다음처럼 한다.

```text
wall time
├─ on-CPU
├─ runnable but unscheduled
├─ I/O wait
├─ lock / condition wait
├─ page fault / memory reclaim
├─ GC / runtime pause
└─ external dependency wait
```

profile은 on-CPU hotspot을 찾는 데 강하고 trace는 시간 순서와 wait relation을 찾는 데 강하다. metric은 전체 population과 장기 추세를 본다. 도구 하나의 결과를 전체 원인으로 일반화하지 않는다.

CPU utilization이 높다는 사실만으로 CPU가 병목이라고 확정할 수도 없다. runnable queue가 길어지는지, core별 imbalance가 있는지, lock owner가 CPU를 독점하는지, I/O completion을 처리할 thread가 지연되는지까지 함께 봐야 한다. 반대로 utilization이 낮아도 serial dependency나 single-thread bottleneck 때문에 latency가 제한될 수 있다. throughput·latency·queue depth·on-CPU time을 같은 시간축으로 맞춰야 원인과 결과의 순서를 검증할 수 있다.

source에서 결과까지의 경로는 하나의 거대한 검은 상자가 아니다. **각 단계는 입력·출력 artifact, 불변조건, 실패 증거를 가진 독립 경계**다. 이 경계를 유지하면 AI가 만든 코드든 사람이 만든 코드든 어디서 의미가 바뀌었는지 추적할 수 있다.