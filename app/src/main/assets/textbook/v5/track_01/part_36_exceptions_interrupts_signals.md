# PART 36 · Exceptions, Interrupts and Signals — fault delivery from CPU to user process

정상 instruction stream이 끊기는 사건은 모두 같은 `에러`가 아니다. CPU instruction 자체가 fault를 만들 수도 있고, device가 asynchronous interrupt를 보낼 수도 있으며, kernel이 user process에 signal을 전달할 수도 있고, language runtime이 stack unwinding을 수행할 수도 있다. 각 mechanism은 **발생 원인, 저장되는 machine state, privilege transition, 재시작 가능성, handler context, 복구 범위**가 다르다.

---

## CHAPTER 01 · Synchronous exception과 asynchronous interrupt는 causal relation이 다르다

Synchronous exception은 현재 instruction execution과 직접 연결된다. Page fault, invalid opcode, divide error처럼 동일 state에서 같은 instruction을 다시 실행하면 같은 event가 발생할 수 있다. External interrupt는 timer/device event가 현재 instruction과 독립적으로 도착한다.

Crash timeline에서 `PC가 어디였는가`의 의미도 다르다. Synchronous fault PC는 원인 instruction을 가리킬 수 있지만 interrupt PC는 단지 interrupt가 끼어든 지점이다.

---

## CHAPTER 02 · Fault, trap, abort라는 용어는 architecture semantics를 확인해야 한다

일부 architecture 문서는 exception을 fault/trap/abort로 나눠 saved PC와 restart 가능성을 구분한다. Fault는 원인 수정 후 instruction 재실행이 가능할 수 있고 trap은 instruction 완료 뒤 control transfer가 일어나며 abort는 precise restart가 어려운 심각한 상태를 나타낼 수 있다.

용어를 platform-independent universal taxonomy로 외우지 않는다. 실제 CPU architecture manual과 OS entry code contract를 확인한다.

---

## CHAPTER 03 · Exception vector는 event class를 handler entry point에 연결한다

CPU는 exception/interrupt type에 따라 architecture-defined vector/entry table을 통해 kernel entry code로 control을 넘긴다. Entry table은 handler address뿐 아니라 privilege, gate type, stack switching 같은 metadata와 연결될 수 있다.

Handler table corruption은 arbitrary privileged control flow로 이어질 수 있어 read-only/protected configuration과 initialization ordering이 중요하다.

---

## CHAPTER 04 · Privilege transition은 user register state를 보존해야 한다

User mode에서 kernel mode로 진입할 때 return에 필요한 PC, flags, stack/register state를 trap frame 같은 kernel structure에 저장한다. Handler가 끝나면 이 state를 복원해 user execution을 재개하거나 signal frame으로 바꿀 수 있다.

Crash dump의 register set은 이 saved frame에서 나오므로 architecture ABI와 frame layout을 알아야 정확히 해석할 수 있다.

---

## CHAPTER 05 · Kernel entry stack은 untrusted user stack과 분리돼야 한다

User stack pointer가 invalid하거나 attacker-controlled이어도 exception handling은 시작돼야 한다. Architecture/OS는 privilege entry에서 trusted kernel stack으로 전환하는 mechanism을 사용한다. 일부 critical event는 별도 emergency/NMI stack을 사용할 수 있다.

Stack overflow가 fault handler를 다시 fault시키면 double-fault류 catastrophic path가 생길 수 있다.

---

## CHAPTER 06 · Page fault는 virtual-memory state machine의 요청이다

CPU가 address translation/protection을 완료하지 못하면 fault address와 access type을 kernel에 전달한다. Kernel은 VMA/PTE state를 확인해 demand allocation, COW, file page-in을 수행한 뒤 instruction을 재시작할 수 있다. Invalid address/permission이면 process에 fault signal을 전달한다.

`page fault count`에는 정상 demand-paging event와 fatal memory bug가 함께 존재할 수 있다.

---

## CHAPTER 07 · Protection fault는 mapping 존재 여부와 permission을 분리한다

Address가 mapped돼도 write-protected, NX, user/supervisor permission 때문에 access가 실패할 수 있다. COW write fault는 의도된 read-only mapping 위 write를 private copy로 바꾸는 정상 path지만 code page write나 NX execute는 security violation일 수 있다.

Fault error code/access metadata를 보지 않고 `segfault` 한 단어로 원인을 좁히지 않는다.

---

## CHAPTER 08 · Invalid opcode는 binary/ISA mismatch와 memory corruption 신호가 될 수 있다

CPU가 현재 instruction encoding을 지원하지 않거나 executable bytes가 corruption되면 invalid-instruction exception이 발생할 수 있다. Wrong CPU feature dispatch, jump-to-data, overwritten code pointer, malformed JIT code가 후보가 된다.

Crash PC 주변 machine code와 loaded binary build ID를 확인해 source-level stack trace보다 아래에서 검증한다.

---

## CHAPTER 09 · Breakpoint와 single-step은 의도적으로 exception을 생성한다

Debugger는 software breakpoint instruction이나 hardware breakpoint/debug register를 이용해 execution을 exception handler로 넘긴다. Single-step flag는 instruction마다 debug trap을 만들 수 있다.

Debugger attached 상태가 timing, signal delivery, anti-debug logic을 바꿀 수 있으므로 race bug 재현에 observer effect가 생긴다.

---

## CHAPTER 10 · System call도 controlled exception/entry mechanism이다

User process는 arbitrary kernel function을 호출하지 않고 architecture-defined syscall instruction과 ABI register convention으로 kernel entry를 요청한다. Entry code는 syscall number/argument를 검증하고 kernel stack/context로 전환한다.

Syscall과 page fault는 모두 privilege entry를 만들 수 있지만 하나는 explicit service request, 다른 하나는 instruction execution 중 resolution requirement다.

---

## CHAPTER 11 · Syscall restart는 signal interruption과 side effect를 함께 고려한다

Blocking syscall이 signal 때문에 중단되면 EINTR을 반환하거나 kernel/libc가 특정 조건에서 자동 restart할 수 있다. Partial read/write가 이미 발생한 뒤 interruption될 수도 있으므로 `retry same call`이 항상 idempotent하지 않다.

I/O loop는 bytes transferred와 error/restart policy를 함께 관리한다.

---

## CHAPTER 12 · External interrupt는 device/timer가 CPU attention을 요청하는 path다

Interrupt controller는 device interrupt source를 CPU vector/priority에 routing한다. CPU는 현재 interrupt masking/priority에 따라 handler entry를 수행한다. MSI-X처럼 device가 message transaction으로 interrupt를 생성하는 경우에도 최종적으로 CPU interrupt dispatch path로 연결된다.

Interrupt rate가 너무 높으면 application CPU budget이 줄고 interrupt moderation이 필요할 수 있다.

---

## CHAPTER 13 · Interrupt masking은 critical section과 latency를 교환한다

Kernel이 local interrupt를 잠시 disable하면 해당 CPU에서 특정 interrupt handler가 끼어드는 것을 막아 critical state를 보호할 수 있다. 그러나 disable interval이 길면 device/timer latency가 증가한다.

Realtime analysis에서는 longest IRQ-off interval을 측정한다. Lock hold time과 별개의 latency source다.

---

## CHAPTER 14 · Hard interrupt context에서는 할 수 있는 일이 제한된다

Interrupt handler는 sleeping lock을 기다리거나 arbitrary blocking I/O를 수행할 수 없는 context일 수 있다. 최소한의 acknowledge/state capture만 하고 긴 작업은 softirq/threaded IRQ/workqueue로 defer한다.

Context rule을 위반하면 `scheduling while atomic`류 kernel failure나 latency 폭발을 만들 수 있다.

---

## CHAPTER 15 · Deferred interrupt work는 latency와 throughput을 분리한다

Packet receive처럼 interrupt마다 전체 protocol stack을 처리하면 IRQ latency가 길어진다. Top half가 device event를 acknowledge하고 bottom-half mechanism이 batch processing을 수행하면 interrupt responsiveness와 throughput을 조절할 수 있다.

Defer queue가 과부하되면 application보다 kernel background backlog가 latency source가 된다.

---

## CHAPTER 16 · NMI는 일반 interrupt masking보다 강한 emergency observation path다

Non-maskable interrupt는 일반 local interrupt disable 상태에서도 전달될 수 있어 watchdog, perf sampling, severe hardware event에 사용될 수 있다. Handler는 normal IRQ보다 더 엄격한 reentrancy/NMI-safe 제약을 가진다.

NMI handler에서 normal spinlock이나 allocator를 사용하면 이미 interrupted context가 같은 resource를 보유해 deadlock될 수 있다.

---

## CHAPTER 17 · Machine check는 hardware-reported execution integrity failure다

CPU/core/cache/memory/interconnect가 corrected/uncorrected hardware error를 machine-check architecture로 보고할 수 있다. Corrected event는 execution을 계속할 수 있지만 fatal event는 process kill이나 system panic으로 이어질 수 있다.

PART 22 EDAC와 함께 bank/status/address를 분석해 software crash와 hardware integrity failure를 분리한다.

---

## CHAPTER 18 · Nested exception은 handler 자체가 fault할 때 복구 난도가 급증한다

Page-fault handler가 필요한 kernel memory를 access하다 또 fault하거나 kernel stack이 깨져 exception entry가 실패하면 nested/double-fault path로 들어갈 수 있다. Recovery가 불가능하면 panic/reboot가 필요하다.

Kernel hardening은 exception entry code와 emergency stack을 최소 dependency로 유지한다.

---

## CHAPTER 19 · Kernel oops와 panic은 failure containment 범위가 다르다

Kernel oops는 특정 task/context의 serious bug를 report하고 일부 경우 system이 계속 실행할 수 있지만 internal invariant가 이미 깨졌을 수 있다. Panic은 system 전체를 중단/재부팅하는 fail-stop 결정이다.

Production policy는 corrupted kernel state에서 계속 서비스하는 risk와 availability를 비교한다. Oops 후 결과를 정상 evidence로 취급하지 않는다.

---

## CHAPTER 20 · User fault는 kernel이 process signal로 변환할 수 있다

Invalid memory access, illegal instruction, arithmetic exception은 OS가 SIGSEGV/SIGILL/SIGFPE류 signal로 user process에 전달할 수 있다. Signal number 이름만으로 raw CPU exception details가 모두 보존되는 것은 아니다.

siginfo, fault address, ucontext/register를 crash handler/core dump에 저장해 original machine event를 복원한다.

---

## CHAPTER 21 · Signal delivery는 normal function call이 아니다

Kernel은 target thread의 user stack에 signal frame/context를 구성하고 instruction pointer를 handler로 바꿔 user mode로 돌아갈 수 있다. Handler return은 sigreturn path를 통해 original context를 복원한다.

Corrupted user stack이 signal frame을 받을 수 없으면 alternate signal stack이 필요할 수 있다.

---

## CHAPTER 22 · Async signal handler는 매우 제한된 API만 안전하게 호출할 수 있다

Signal이 malloc/stdio/lock 내부 실행을 중단한 뒤 동일 non-reentrant function을 handler에서 호출하면 internal state를 깨뜨릴 수 있다. POSIX async-signal-safe operation만 사용하고 복잡한 처리는 flag/self-pipe/signalfd로 normal event loop에 넘긴다.

Logging library를 crash handler에서 무조건 호출하는 것도 안전하지 않을 수 있다.

---

## CHAPTER 23 · Signal mask는 thread마다 delivery eligibility를 바꾼다

Multi-thread process에서 signal mask는 thread별이고 process-directed signal은 eligible thread 중 하나가 받을 수 있다. Dedicated signal thread가 모든 worker에서 mask하고 sigwait/signalfd로 central handling하는 pattern이 있다.

`SIGTERM은 main thread로 온다`는 가정은 일반적인 보장이 아니다.

---

## CHAPTER 24 · Alternate signal stack은 stack-overflow crash를 관찰하기 위한 별도 자원이다

Normal stack overflow가 guard page에 닿았을 때 handler도 같은 exhausted stack을 사용하면 실행할 공간이 없다. sigaltstack류 mechanism으로 preallocated alternate stack에서 handler를 실행하면 crash context를 저장할 가능성이 높아진다.

Alternate stack 크기도 nested signal/unwind requirement를 만족해야 한다.

---

## CHAPTER 25 · Language exception은 CPU exception과 다른 runtime control-flow다

Java/Kotlin/Python exception throw는 일반적으로 runtime metadata와 stack unwinding을 사용하는 language mechanism이며 CPU page fault와 동일하지 않다. Null/bounds check가 compiler-generated branch로 language exception을 만들 수 있고 일부 runtime은 hardware trap을 optimization으로 활용할 수 있다.

Source `try/catch`가 SIGSEGV 같은 arbitrary native fault를 안전하게 복구한다고 가정하지 않는다.

---

## CHAPTER 26 · Stack unwinding은 call frame metadata의 정확성에 의존한다

C++ exception, debugger backtrace, crash symbolization은 frame pointer 또는 unwind table/CFI metadata를 사용해 caller state를 복원한다. Corrupted stack이나 hand-written assembly가 unwind metadata와 맞지 않으면 trace가 끊긴다.

Binary size 최적화로 unwind info를 제거할 때 debugging/recovery cost를 명시한다.

---

## CHAPTER 27 · Core dump는 process address space와 machine state의 snapshot이다

Fatal signal 시 core dump는 register, mapping, memory segment, thread state를 보존해 postmortem analysis를 가능하게 한다. Full memory dump는 secret을 포함할 수 있어 size/security policy가 필요하다.

Exact executable/shared-library build ID 없이 core만 있으면 symbolization이 부정확해질 수 있다.

---

## CHAPTER 28 · Virtual machine은 guest exception/interrupt를 injection해야 한다

Guest CPU는 virtual page fault, timer interrupt, device interrupt를 받아야 한다. Hypervisor는 VM exit 원인과 virtual interrupt-controller state를 관리해 guest가 native architecture와 호환되는 exception model을 보게 한다.

Direct-assigned device MSI-X와 interrupt remapping은 host→guest injection path와 연결된다.

---

## CHAPTER 29 · Exception latency는 event 발생→handler start→recovery 전체로 측정한다

Realtime system에서 interrupt latency는 hardware event부터 ISR entry까지, scheduling latency는 wakeup부터 task run까지 별도 측정한다. Handler가 빠르게 시작해도 deferred work가 오래 걸리면 application recovery latency는 길 수 있다.

Trace에는 vector/source, CPU, IRQ-off interval, handler/deferred-work duration을 함께 남긴다.

---

## CHAPTER 30 · Fault-delivery 설계의 최종 계약은 cause·state·context·restart·containment다

1. **Cause** — 현재 instruction, external device, hardware integrity 중 무엇이 event를 만들었는가.
2. **State** — PC/register/fault address/interrupt source를 충분히 보존했는가.
3. **Context** — handler가 sleep/allocation/lock을 사용할 수 있는 실행 context인가.
4. **Restart** — 원인 수정 후 instruction/syscall/task를 안전하게 재개할 수 있는가.
5. **Containment** — process kill로 충분한가, device reset이 필요한가, kernel/system 전체를 중단해야 하는가.

Exception을 `에러 처리`로 뭉개지 않고 이 다섯 축으로 분류하면 fault가 어느 계층에서 생성·전달·복구됐는지 추적할 수 있다.
