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

---

## CHAPTER 09 · syscall은 user code가 privileged resource를 요청하는 경계다

application code가 임의의 physical memory, device register, process table을 직접 조작하게 두면 격리가 무너진다. 운영체제는 user mode와 kernel mode의 권한 경계를 두고 system call을 통해 file, memory mapping, process/thread, network, timer 같은 자원을 요청하게 한다.

syscall 비용은 단순 함수 호출과 같지 않지만 모든 I/O가 매 byte마다 syscall을 발생시키는 것도 아니다. libc/runtime buffering, page cache, async I/O, batching이 중간 계층에 존재할 수 있다.

오류 조사에서는 language exception만 보지 말고 최종 OS error code와 syscall context까지 내려갈 필요가 있다.

---

## CHAPTER 10 · file descriptor는 resource identity와 lifetime을 분리해 봐야 한다

Unix 계열에서 file descriptor는 process-local integer handle이고 kernel의 open-file state를 가리킨다. pathname과 open file description은 같은 개념이 아니다. 파일이 rename/unlink된 뒤에도 열린 descriptor가 유효할 수 있는 이유는 namespace lookup과 열린 resource lifetime이 분리되기 때문이다.

buffered stream, descriptor, kernel page cache, storage durability 역시 서로 다른 상태다. application buffer의 `flush`가 kernel buffer로 전달하는 의미와 storage까지 durable하게 만드는 의미를 혼동하면 crash-consistency 버그가 생긴다.

---

## CHAPTER 11 · thread는 shared address space 안에서 독립 execution context를 가진다

같은 process의 thread는 code·heap·open resource를 공유하면서 각자 stack과 register/scheduling state를 가진다. 이 구조 때문에 communication은 빠르지만 shared mutable state에는 race가 생긴다.

scheduler는 runnable task 중 CPU를 받을 대상을 선택한다. runnable, sleeping, blocked, I/O wait 상태를 구분하지 않으면 `CPU가 느리다`는 잘못된 결론에 도달한다. thread dump와 scheduler trace는 **실행 중인지, lock을 기다리는지, I/O completion을 기다리는지**를 분리하는 증거다.

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

source에서 결과까지의 경로는 하나의 거대한 검은 상자가 아니다. **각 단계는 입력·출력 artifact, 불변조건, 실패 증거를 가진 독립 경계**다. 이 경계를 유지하면 AI가 만든 코드든 사람이 만든 코드든 어디서 의미가 바뀌었는지 추적할 수 있다.