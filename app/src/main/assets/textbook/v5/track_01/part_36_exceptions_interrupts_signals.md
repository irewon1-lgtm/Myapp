# PART 36 · Exceptions, Interrupts and Signals — fault delivery across privilege boundaries

예외·인터럽트·signal은 모두 “정상 instruction stream이 다른 경로로 전환된다”는 공통점을 가지지만 원인과 실행 context가 다르다. fault는 현재 instruction과 직접 연결될 수 있고 interrupt는 외부 event에서 오며 signal은 kernel이 user process에 전달하는 비동기 control flow다. 이 PART는 **발생 원인, privilege transition, saved state, restart/cleanup, 진단 evidence**를 하나의 전달 chain으로 정리한다.

---

## CHAPTER 01 · synchronous exception과 asynchronous event는 causality가 다르다

동기 예외는 현재 실행한 instruction이나 그 memory access와 직접 연결되어 같은 input과 machine state에서 재현될 가능성이 높다. page fault, invalid opcode처럼 faulting instruction을 특정할 수 있다. 반면 device interrupt는 현재 instruction과 무관한 외부 event가 도착해 instruction boundary에서 처리될 수 있다.

두 범주를 같은 “인터럽트”라는 말로 뭉치면 root cause 추적이 흐려진다. CPU가 handler로 들어갔다는 사실보다 어떤 vector와 reason으로 들어갔는지, return 시 instruction을 재시도하는지 다음 instruction으로 진행하는지가 중요하다.

trace에는 exception vector, fault address, current IP, interrupt source를 구분해 남긴다. latency incident에서 interrupt count가 높다고 현재 request code가 fault를 낸 것으로 해석하지 않는다.

---

## CHAPTER 02 · fault, trap, abort는 복귀 semantics와 recoverability가 다르다

일부 architecture 설명에서는 fault를 instruction 재시도 가능 event, trap을 instruction 완료 뒤 전달되는 event, abort를 정확한 재시도가 어려운 심각한 failure로 구분한다. 명칭은 architecture마다 다를 수 있지만 핵심은 **saved PC가 무엇을 가리키고 handler가 어떤 state에서 복귀 가능한가**다.

page fault는 missing page를 준비한 뒤 같은 instruction을 다시 실행할 수 있지만 machine-check 계열은 hardware state가 신뢰 가능하지 않아 process나 system을 중단해야 할 수 있다. debugger breakpoint는 intentional trap으로 정상 제어 흐름의 일부가 된다.

crash log에서 signal 이름만 보지 말고 architecture syndrome/error code를 함께 해석한다. recoverable fault를 fatal error로 처리하거나 반대로 unrecoverable state에서 계속 실행하는 두 실수를 피한다.

---

## CHAPTER 03 · exception vector는 event class를 정해진 entry point로 연결한다

CPU는 exception/interrupt reason에 따라 미리 등록된 handler entry로 control을 넘긴다. vector table이나 descriptor 구조는 privilege level과 stack transition 규칙을 함께 담을 수 있다. 잘못된 vector setup은 fault 처리 자체에서 다시 fault를 일으켜 system을 더 불안정하게 만든다.

early boot에서 vector table이 완전히 준비되기 전에 exception이 발생하면 진단 정보가 제한될 수 있다. virtualization에서는 guest vector와 host physical interrupt가 서로 다른 mapping layer를 거친다.

저수준 장애에서는 handler address, vector number, setup stage를 보존한다. fault가 반복되면 원래 event와 handler 내부 secondary event를 분리해 timeline을 만든다.

---

## CHAPTER 04 · privilege frame은 user state를 저장해 안전한 kernel entry를 만든다

user mode에서 kernel handler로 들어갈 때 CPU와 entry code는 return에 필요한 PC, flags, stack 관련 state를 보존해야 한다. architecture가 자동 저장하는 부분과 software prologue가 추가로 저장하는 register가 나뉜다. frame layout이 ABI와 unwind logic의 기준이 된다.

공격자가 조작한 user stack을 그대로 privileged stack처럼 신뢰하면 안 된다. kernel은 정의된 entry stack과 validation을 사용해야 한다. nested event가 들어오면 각 frame의 ownership과 depth도 보장해야 한다.

crash dump에서 frame corruption이 의심되면 saved PC뿐 아니라 privilege level, stack pointer, exception metadata를 함께 검사한다. 잘못된 unwind가 그럴듯한 가짜 call stack을 만들 수 있다.

---

## CHAPTER 05 · kernel stack은 privileged execution의 bounded resource다

각 task 또는 CPU가 사용하는 kernel stack은 일반 userspace stack처럼 무한히 커질 수 없다. deep call chain, 큰 local object, nested interrupt가 stack budget을 소모한다. privileged stack overflow는 인접 kernel state를 손상시킬 수 있어 guard와 크기 제한이 중요하다.

interrupt context가 별도 stack을 쓰는지 architecture와 kernel configuration에 따라 다르다. recursion이 허용되지 않는 path에서 callback이 다시 같은 entry를 타면 예상보다 빠르게 stack을 소모한다.

stack usage report와 deepest call path를 측정한다. rare panic이 load spike에서만 나타난다면 nested event와 large frame을 함께 조사한다.

---

## CHAPTER 06 · page fault는 virtual-memory state를 채우기 위한 정상 경로일 수 있다

page fault는 현재 translation이나 permission만으로 access를 완료할 수 없음을 알리는 event다. anonymous first touch, file-backed demand paging, copy-on-write는 handler가 mapping을 준비한 뒤 instruction을 재시도하는 정상 경로가 될 수 있다.

같은 vector라도 unmapped address나 backing I/O failure면 user-visible signal로 끝날 수 있다. fault count만으로 memory bug라고 결론내리지 않고 reason과 handler outcome을 봐야 한다.

fault address, access type, VMA, major/minor path, service latency를 기록한다. performance issue에서는 page-fault handling 자체와 storage/reclaim 대기를 분리한다.

---

## CHAPTER 07 · protection fault는 mapping 존재와 access 권한을 분리한다

address가 valid mapping 안에 있어도 read-only page에 write하거나 execute-never page에서 instruction fetch를 하면 protection fault가 발생할 수 있다. COW처럼 의도적으로 write permission을 제거해 첫 write를 trap하는 경우도 있어 fault가 곧 security violation은 아니다.

mprotect, JIT W^X transition, stack guard가 permission state를 바꾼다. race로 permission 변경과 access가 겹치면 intermittent crash가 발생할 수 있다.

maps와 page permission을 fault timestamp 기준으로 확인한다. crash 후 현재 mapping만 보면 이미 state가 바뀌었을 수 있으므로 transition event를 trace하는 것이 유용하다.

---

## CHAPTER 08 · invalid opcode는 binary identity와 execution mode를 먼저 확인한다

CPU가 현재 mode에서 정의되지 않았거나 허용되지 않은 instruction을 fetch하면 invalid-instruction fault가 발생할 수 있다. 잘못된 function pointer가 data를 code로 실행하거나, binary가 지원하지 않는 ISA extension을 사용하거나, code page가 corruption된 경우가 원인이다.

단순히 “CPU가 구형이다”라고 결론내리지 않는다. fault bytes와 module build ID, feature detection, dispatch path를 비교한다. JIT code라면 generation과 code-cache lifecycle까지 확인한다.

재현 시 exact instruction bytes와 register를 보존한다. symbol name만으로 다른 build의 disassembly를 보면 원인을 잘못 찾을 수 있다.

---

## CHAPTER 09 · debug trap은 의도적으로 execution을 관찰자에게 넘기는 mechanism이다

breakpoint, single-step, hardware watchpoint는 정상 프로그램 state를 debugger나 tracer에 전달하기 위해 trap을 발생시킨다. production에서 우연한 debug instruction이나 stale breakpoint patch가 남으면 일반 crash처럼 보일 수 있다.

trap handling은 code patching과 instruction-cache coherence에도 의존한다. multi-thread process에서 한 thread가 breakpoint를 밟는 동안 다른 thread의 execution을 어떻게 정지할지 debugger 정책이 개입한다.

incident에서는 ptrace/debugger attachment 여부와 breakpoint address를 기록한다. instrumentation이 timing-sensitive race를 가리거나 새 race를 만들 수 있다는 observer effect도 고려한다.

---

## CHAPTER 10 · syscall entry는 controlled privilege transition이다

system call은 user code가 정해진 ABI를 통해 kernel service를 요청하는 경로다. syscall number와 argument register를 전달하고 CPU는 privilege mode를 전환한 뒤 검증된 entry code로 이동한다. arbitrary kernel address로 jump하는 것과 완전히 다른 보호된 contract다.

user pointer와 length는 신뢰할 수 없으므로 kernel이 검증·copy해야 한다. entry/exit에서 speculation mitigation이나 register sanitization이 추가되면 성능 비용도 생긴다.

syscall latency를 분석할 때 transition 자체와 handler 내부 blocking을 분리한다. trace에서 entry→sleep→wakeup→exit sequence를 보면 실제 wait 지점을 알 수 있다.

---

## CHAPTER 11 · syscall restart는 signal delivery와 blocking operation 사이의 계약이다

blocking syscall 도중 signal이 도착하면 호출이 interruption error로 돌아오거나 kernel/libc 정책에 따라 자동 restart될 수 있다. restart 가능 여부는 syscall과 signal action에 따라 다르며 partial progress가 이미 있었는지도 확인해야 한다.

write가 일부 byte를 처리한 뒤 interrupted되었는데 전체 request를 다시 보내면 duplicate data가 생길 수 있다. timeout budget도 restart마다 새로 시작하면 caller deadline을 초과할 수 있다.

return code와 transferred length, signal timestamp를 함께 저장한다. wrapper가 EINTR을 무조건 loop하는 패턴이 해당 API에서 안전한지 검증한다.

---

## CHAPTER 12 · external interrupt는 device completion을 CPU execution에 전달한다

device는 I/O completion, packet arrival 같은 event를 interrupt로 CPU에 알릴 수 있다. interrupt routing과 affinity는 어느 CPU가 handler를 실행할지 결정하고 cache locality와 scheduler load에 영향을 준다.

interrupt rate가 너무 높으면 useful application work가 줄어들 수 있어 moderation·polling으로 trade-off를 조정한다. 반대로 지나친 coalescing은 latency를 증가시킨다.

IRQ count, handler duration, queue completion을 같은 CPU별 timeline에 둔다. 특정 core만 과부하라면 device queue와 affinity를 함께 본다.

---

## CHAPTER 13 · interrupt masking은 critical region을 보호하지만 latency budget을 소비한다

일부 저수준 code는 짧은 구간 동안 interrupt delivery를 제한해 per-CPU state를 안전하게 갱신한다. mask 시간이 길어지면 timer와 device event latency가 그대로 늘어난다. lock을 잡았다는 이유만으로 arbitrary long work를 interrupt-disabled 상태에서 수행하면 안 된다.

mask nesting과 restore rule이 틀리면 interrupt가 영구적으로 비활성화되거나 예상보다 일찍 열릴 수 있다. preemption disable과 interrupt disable도 같은 개념이 아니다.

latency tracer로 longest irq-off section을 측정한다. real-time 요구에서는 평균보다 최대 masking duration이 중요하다.

---

## CHAPTER 14 · IRQ context는 허용되는 operation이 process context와 다르다

interrupt handler는 일반 process가 아니며 sleep 가능한 operation, blocking allocation, user-space wait를 수행할 수 없는 제약이 있을 수 있다. handler에서 오래 걸리는 work를 직접 처리하면 interrupt latency와 nested event를 악화시킨다.

따라서 최소한의 acknowledgment와 state capture만 하고 나머지를 deferred work로 넘기는 구조가 사용된다. 하지만 defer queue가 포화되면 backlog가 쌓여 결국 latency가 올라간다.

handler time과 deferred queue age를 동시에 측정한다. top half가 짧다고 전체 interrupt processing이 건강하다고 결론내리지 않는다.

---

## CHAPTER 15 · deferred work는 urgent event와 expensive processing을 분리한다

softirq, tasklet, workqueue 같은 deferred mechanism은 interrupt context에서 처리하기 어려운 일을 나중 execution context로 옮긴다. 각 mechanism은 scheduling과 concurrency 성질이 다르므로 공유 state 보호 방법도 달라진다.

network burst에서 deferred work가 CPU를 오래 점유하면 user task가 runnable인데도 실행되지 못할 수 있다. workqueue item이 sleep 가능한지 여부도 mechanism에 따라 확인해야 한다.

queue depth, service time, CPU share를 monitor한다. backlog가 증가하면 interrupt moderation과 consumer capacity를 같이 조정한다.

---

## CHAPTER 16 · NMI는 일반 interrupt masking보다 높은 우선도의 특수 event다

Non-Maskable Interrupt 계열은 일반 interrupt가 막혀 있는 상황에서도 watchdog, hardware error 같은 critical event를 전달하는 데 사용될 수 있다. 따라서 handler에서 사용할 수 있는 lock과 function이 더욱 제한된다. 일반 code와 같은 synchronization을 사용하면 deadlock이 생길 수 있다.

NMI context에서 이미 같은 lock을 잡은 code를 interrupted한 뒤 다시 lock을 시도하면 progress가 없다. logging조차 NMI-safe path가 필요하다.

NMI incident에서는 interrupted context와 handler stack을 둘 다 보존한다. watchdog NMI는 root cause가 아니라 progress failure를 관찰한 mechanism일 수 있다.

---

## CHAPTER 17 · machine check는 hardware-detected error의 recoverability를 평가한다

CPU나 memory subsystem이 uncorrectable error를 발견하면 machine-check 계열 event가 전달될 수 있다. corrected ECC처럼 계속 실행 가능한 경우와 execution state 신뢰가 깨진 경우를 구분해야 한다.

단순 process kill로 끝낼 수 있는 poisoned page와 system-wide panic이 필요한 error scope가 다르다. hardware firmware와 OS가 severity를 해석하는 방식도 platform-specific하다.

EDAC/MCE record에서 bank, address, corrected 여부를 확인한다. 같은 location의 corrected error가 증가하면 fatal failure 이전에 proactive maintenance 신호로 사용한다.

---

## CHAPTER 18 · nested exception은 handler 자체의 fault까지 안전하게 처리해야 한다

예외 처리 중 또 다른 예외가 발생할 수 있다. handler가 user pointer를 잘못 접근하거나 stack이 손상된 경우 secondary fault가 원래 정보를 덮어쓸 수 있다. architecture는 nesting 제한이나 double-fault 전용 path를 제공할 수 있다.

reentrant하지 않은 per-CPU state를 handler가 다시 사용하면 corruption이 생긴다. critical entry path는 가능한 작은 trusted code로 유지한다.

crash dump에서 첫 fault와 마지막 panic을 구분한다. earliest exception record가 root cause에 더 가까운 경우가 많다.

---

## CHAPTER 19 · oops와 panic은 failure scope와 계속 실행 가능성의 판단이다

kernel이 invariant violation을 발견했을 때 특정 task만 종료하거나 system 전체를 중단하는 선택이 필요하다. corruption 가능성이 있는 상태에서 계속 실행하면 data integrity와 security가 더 크게 손상될 수 있다. 반대로 모든 recoverable driver error를 panic으로 만들면 availability가 불필요하게 낮아진다.

panic policy는 workload와 integrity requirement에 따라 달라질 수 있다. crash dump가 persistent storage에 남기 전에 reboot하면 evidence를 잃을 수 있다.

운영에서는 panic reason, taint/state, preceding warning을 보존한다. 자동 reboot가 같은 fault를 반복하면 crash counter로 loop를 차단한다.

---

## CHAPTER 20 · user signal은 kernel event를 process-visible asynchronous control flow로 바꾼다

SIGSEGV, SIGALRM, SIGTERM 같은 signal은 서로 다른 원인과 정책을 가진다. hardware fault가 signal로 mapping될 수도 있고 다른 process가 explicit signal을 보낼 수도 있다. signal number만으로 origin을 확정하지 않고 siginfo와 sender를 확인한다.

handler가 설치되어 있으면 normal user code 중간에 실행될 수 있어 reentrancy가 생긴다. default action이 terminate/core인지 ignore인지도 signal별로 다르다.

log에는 signal number, code, fault address 또는 sender pid를 함께 남긴다. service shutdown signal과 genuine crash signal을 monitoring에서 분리한다.

---

## CHAPTER 21 · signal frame은 interrupted user context를 stack에 저장해 복귀를 가능하게 한다

signal delivery 시 kernel은 user handler가 끝난 뒤 원래 execution을 복원할 수 있도록 register와 mask 정보를 user-space frame에 배치할 수 있다. stack corruption이 있으면 signal return 자체가 실패할 수 있다.

handler가 일반 stack을 크게 사용하면 이미 stack overflow로 fault난 상황에서 다시 문제가 생길 수 있다. architecture ABI에 맞지 않는 hand-written trampoline도 frame을 손상시킬 수 있다.

core dump에서 signal frame을 unwind할 때 exact ABI와 symbol을 사용한다. 잘못된 unwinder가 handler 이전 call chain을 잃을 수 있다.

---

## CHAPTER 22 · async-signal-safe 규칙은 handler가 interrupted library state를 재진입하지 않게 한다

signal은 malloc, stdio, lock 같은 library code 실행 중에 도착할 수 있다. handler가 같은 내부 state를 사용하는 non-safe function을 호출하면 deadlock이나 corruption이 생길 수 있다. 따라서 POSIX는 handler에서 안전하게 사용할 수 있는 제한된 operation 집합을 정의한다.

logging을 위해 복잡한 formatter를 호출하는 습관이 대표적인 위험이다. handler에서는 최소 flag나 pipe write만 남기고 실제 처리를 main loop로 넘기는 편이 안전하다.

signal test는 arbitrary timing에 handler를 주입한다. 정상 demo에서 한 번 동작했다는 사실은 reentrancy safety를 증명하지 못한다.

---

## CHAPTER 23 · signal mask는 어느 구간에서 어떤 signal delivery를 허용할지 정의한다

thread마다 signal mask를 관리할 수 있어 critical state update 중 특정 signal delivery를 미룰 수 있다. multi-thread process에서는 process-directed signal이 어느 eligible thread에 전달될지도 고려해야 한다.

mask를 복원하지 않으면 signal이 영구 차단되고, 너무 넓게 막으면 shutdown이나 timer response가 늦어진다. signalfd 같은 mechanism은 signal을 event-loop 모델로 통합하는 선택지가 된다.

thread별 mask와 handler ownership을 문서화한다. signal이 “사라졌다”면 send 실패보다 먼저 blocked/pending state를 확인한다.

---

## CHAPTER 24 · alternate signal stack은 main stack 손상 상황에서 handler 공간을 분리한다

stack overflow나 guard-page fault처럼 normal stack을 사용할 수 없는 사건을 처리하려면 별도의 signal stack이 필요할 수 있다. altstack이 너무 작으면 handler와 unwinder가 다시 overflow할 수 있고, lifetime이 끝난 memory를 등록해 두면 더 위험하다.

모든 signal을 altstack에 올릴 필요는 없지만 fatal diagnostic path는 stack failure를 고려해야 한다. thread별로 설정이 필요한 환경도 있다.

stress test에서 deep recursion과 signal을 겹쳐 core dump가 안정적으로 남는지 확인한다. 단순 crash 여부보다 diagnostic survivability를 본다.

---

## CHAPTER 25 · language exception은 hardware fault와 다른 runtime control-flow mechanism이다

Java/Kotlin/Python exception은 일반적으로 language runtime이 object와 handler table을 사용해 stack을 탐색하거나 frame을 unwind하는 mechanism이다. CPU exception vector와 이름이 같아 보여도 privilege transition이 아니라 user/runtime level control flow다.

native fault가 managed exception으로 변환되는 특수 path도 있어 경계가 섞일 수 있다. exception을 정상 branch처럼 과도하게 사용하면 allocation과 unwind 비용이 hot path에 들어올 수 있다.

profile에서는 thrown/caught count와 stack construction cost를 본다. fatal native crash와 recoverable managed exception을 같은 monitoring event로 합치지 않는다.

---

## CHAPTER 26 · unwind metadata는 optimized frame에서 caller state를 복원한다

frame pointer가 생략되거나 register가 복잡하게 저장되는 optimized code에서 stack을 복원하려면 unwind metadata가 필요하다. exception handling과 profiler, crash symbolization이 모두 이 정보에 의존할 수 있다.

wrong-version symbol/unwind table을 사용하면 stack이 그럴듯하지만 틀리게 보인다. hand-written assembly와 JIT code도 적절한 metadata를 제공하지 않으면 관측 가능성이 떨어진다.

build ID로 executable과 symbol을 정확히 매칭한다. signal frame과 native frame이 섞인 stack은 unwinder capability를 별도 검증한다.

---

## CHAPTER 27 · core dump는 failure 순간의 memory와 register state를 보존하는 artifact다

core dump는 process address space, thread register, mapping 정보를 분석에 사용할 수 있게 남긴다. 하지만 민감한 key, credential, user data도 포함될 수 있어 storage와 access control이 필요하다. dump size 제한 때문에 필요한 segment가 빠질 수도 있다.

container나 crash-loop 환경에서 dump path가 writable하지 않아 evidence가 남지 않는 경우도 있다. symbol artifact와 exact executable이 없으면 dump 가치가 크게 줄어든다.

운영에서는 dump generation 성공 여부와 retention policy를 monitor한다. 개인·보안 data를 무조건 수집하는 diagnostic은 피한다.

---

## CHAPTER 28 · virtual exception은 guest와 host의 event ownership을 구분해야 한다

virtual machine에서 guest exception은 guest CPU state에 주입될 수 있고 physical interrupt나 VM exit는 host hypervisor가 먼저 처리할 수 있다. 같은 timer/device event도 virtual interrupt controller를 거쳐 guest에 전달된다.

host가 처리해야 할 fault와 guest에게 inject해야 할 fault를 잘못 구분하면 isolation이 깨진다. nested virtualization에서는 translation layer가 더 늘어난다.

trace에 guest vCPU, VM exit reason, injected vector를 함께 기록한다. guest crash가 host hardware event에서 시작됐는지 분리할 수 있어야 한다.

---

## CHAPTER 29 · exception latency는 handler 실행시간뿐 아니라 masking과 scheduling을 포함한다

event가 발생한 시각과 handler가 실제 실행된 시각 사이에는 interrupt masking, higher-priority work, CPU power state가 영향을 줄 수 있다. user signal은 kernel handler 뒤 scheduler가 process를 다시 실행할 때까지 추가 delay가 있다.

real-time 요구에서는 average handler cost보다 worst-case delivery latency가 중요하다. tracing을 켠 자체 overhead가 latency를 늘릴 수도 있다.

event source timestamp와 entry/exit timestamp를 separate clock domain에서 정확히 align한다. long-tail sample의 preceding irq-off/preemption state를 같이 수집한다.

---

## CHAPTER 30 · fault contract는 cause, saved state, handler, recovery outcome을 연결한다

예외 시스템을 안정적으로 다루려면 어떤 event가 어떤 context에서 발생하고, 어떤 state가 저장되며, handler가 무엇을 수정한 뒤 어디로 복귀하는지를 명시해야 한다. signal이나 language exception으로 변환되는 경계도 포함한다.

복구 가능성을 과대평가하면 corrupted state로 실행을 계속하고, 모든 event를 fatal하게 처리하면 availability를 잃는다. fault class마다 retry, process kill, system reset, telemetry 정책을 구분한다.

최종 검증은 page fault, invalid access, device interrupt, signal interruption, nested failure를 실제로 주입해 state가 정의된 outcome으로 수렴하는지 확인한다. 목표는 handler가 존재하는 것이 아니라 **failure가 privilege와 process 경계를 넘어 통제된 방식으로 전달되는 것**이다.
