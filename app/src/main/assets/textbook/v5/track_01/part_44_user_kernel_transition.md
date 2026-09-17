# PART 44 · User↔Kernel Transition — syscall entry, context state, vDSO, restart

System call은 library function보다 `조금 더 느린 함수 호출`이 아니다. CPU privilege state가 바뀌고 architecture가 정한 entry point로 제어가 이동하며, kernel은 user pointer·length·credential을 검증하고 필요하면 scheduler·page fault·I/O subsystem과 상호작용한 뒤 user register state로 돌아간다. 이 경계의 비용은 instruction transition뿐 아니라 cache/TLB state, security mitigation, copy, blocking, wakeup까지 포함한다.

## CHAPTER 01 · syscall ABI는 user register를 kernel argument contract로 해석한다

Application의 high-level `read()` 호출은 libc/runtime wrapper를 거쳐 architecture가 정한 system-call number와 argument register 배치로 변환될 수 있다. Kernel entry code는 ordinary function ABI가 아니라 syscall ABI를 기준으로 user register를 해석한다. Pointer width, sign extension, return/error convention도 architecture와 compatibility mode에 따라 다를 수 있다. Raw syscall wrapper나 FFI를 작성할 때 C function calling convention과 syscall calling convention을 섞으면 register/stack 해석이 깨진다. Kernel API 안정성은 syscall semantic contract에 있고 internal kernel function signature는 public ABI가 아니다.

## CHAPTER 02 · privilege transition은 현재 user execution context를 잃지 않고 kernel code로 이동해야 한다

CPU는 syscall/trap entry에서 현재 instruction location과 flags/state 일부를 보존하고 privileged mode의 entry address로 제어를 넘긴다. Kernel은 return할 때 원래 user state를 복원하거나 signal/deopt-like modification을 반영해야 한다. Architecture별로 dedicated syscall instruction, exception vector, return instruction이 다르다. `mode bit 하나를 바꾼다`는 설명만으로는 stack 전환, interrupt masking state, register save, speculation control을 설명할 수 없다.

## CHAPTER 03 · kernel entry prologue는 user-controlled state를 trusted kernel state로 바꾼다

Entry 직후 kernel은 user register를 정해진 frame에 저장하고 per-thread kernel stack/current-task metadata를 사용할 준비를 해야 한다. User가 제공한 stack pointer를 kernel local stack으로 그대로 사용할 수 없으므로 privilege별 stack 또는 architecture mechanism으로 안전한 stack을 확보한다. Trace/debugger가 보는 pt_regs류 구조는 이 경계 state의 snapshot 역할을 할 수 있다. Entry assembly는 compiler-generated ordinary prologue보다 architecture/security contract가 강하다.

## CHAPTER 04 · syscall은 ordinary call보다 branch prediction과 pipeline state에 더 큰 경계를 만든다

User function call은 같은 privilege와 address-space context 안에서 return address를 관리하지만 syscall은 privilege boundary와 kernel entry path를 지난다. CPU는 pipeline serialization, predictor/security state 처리, return validation 같은 추가 work를 수행할 수 있다. Microarchitecture mitigation과 kernel version에 따라 transition cost는 달라진다. `syscall 몇 ns` 같은 숫자를 다른 CPU/OS에 상수처럼 적용하지 않고 현재 machine에서 empty/minimal syscall benchmark와 PMU로 측정한다.

## CHAPTER 05 · user pointer는 kernel pointer가 아니며 access 전에 검증·fault handling이 필요하다

System call이 user buffer pointer와 length를 받았다고 해서 kernel이 일반 kernel memory처럼 바로 dereference할 수 있는 것은 아니다. Address range가 user space인지, mapping/permission이 유효한지, copy 중 page fault가 발생할 수 있는지 고려해야 한다. Linux의 copy_to_user/copy_from_user류 helper는 architecture별 user-access mechanism과 fault recovery를 감싼다. Kernel이 user pointer를 long-lived raw pointer로 저장하면 mapping 변경과 process lifetime 때문에 위험하다. User/kernel copy boundary는 security validation과 memory-fault boundary다.

## CHAPTER 06 · TOCTOU는 user memory를 여러 번 읽을 때 kernel이 같은 값을 본다고 가정하면 생긴다

Kernel이 user structure의 length를 한 번 검사한 뒤 나중에 같은 user memory에서 다시 읽으면 다른 thread가 중간에 값을 바꿀 수 있다. 검증한 값과 사용하는 값이 달라지는 race가 된다. 중요한 control field는 kernel-owned copy로 snapshot한 뒤 validate/use하는 방식이 필요할 수 있다. Shared user memory는 syscall 실행 중에도 mutable하다는 사실을 API/parser 설계에 반영한다. 이는 filesystem path TOCTOU와 같은 check/use 분리 문제의 memory 형태다.

## CHAPTER 07 · 작은 kernel 정보를 가져오는 모든 API가 syscall일 필요는 없다

현재 시각, CPU/process metadata처럼 kernel이 read-only page에 안전하게 노출할 수 있는 값은 vDSO 같은 user-mapped helper를 통해 privilege transition 없이 얻을 수 있다. Helper는 kernel과 ABI를 공유하지만 ordinary user code로 실행되며 필요하면 real syscall fallback을 사용할 수 있다. 따라서 `clock_gettime 호출 횟수 = syscall 횟수`로 profiler를 해석하면 틀릴 수 있다. Symbol resolution과 actual instruction path를 확인한다.

## CHAPTER 08 · page fault는 user instruction에서 시작해 kernel fault handler를 거쳐 같은 instruction으로 돌아올 수 있다

User load/store가 mapping miss 또는 permission condition을 만나면 CPU exception으로 kernel에 들어간다. Kernel은 VMA/page-table state를 검사해 demand allocation, file page-in, COW를 처리한 뒤 faulting instruction을 재시작할 수 있다. Invalid access면 signal을 전달한다. Fault handler가 storage I/O를 기다리면 transition 자체보다 blocked duration이 훨씬 크다. Page-fault count는 minor/major와 fault reason을 나눠 분석한다.

## CHAPTER 09 · external interrupt는 현재 user/kernel code와 무관하게 비동기 entry를 만든다

Timer, NIC, storage completion interrupt는 현재 실행 중인 instruction stream과 독립적으로 발생할 수 있다. CPU가 interrupt를 수락하면 current context를 저장하고 interrupt handler로 이동한다. Handler는 가능한 짧게 critical work를 수행하고 deferred processing으로 넘길 수 있다. User request latency가 흔들릴 때 application code에 아무 변화가 없어도 interrupt load와 IRQ affinity가 원인일 수 있다. Per-CPU interrupt rate와 handler duration을 함께 본다.

## CHAPTER 10 · NMI류 event는 ordinary interrupt masking 규칙과 다른 emergency path다

Non-maskable interrupt는 severe hardware event, watchdog, profiler 등에 사용될 수 있고 일반 lock/interrupt-disabled assumption이 통하지 않는 context에서 실행될 수 있다. NMI handler가 ordinary code와 같은 lock을 잡으면 deadlock 위험이 생길 수 있다. Logging/trace buffer도 NMI-safe operation을 별도로 요구할 수 있다. 특수 exception context의 programming rule은 normal kernel thread context와 구분한다.

## CHAPTER 11 · kernel stack은 thread의 privileged control flow를 저장하는 별도 resource다

각 task/thread가 kernel mode에서 syscall, fault, interrupt를 처리할 때 사용할 stack이 필요하다. Deep kernel call chain이나 large stack local을 남발하면 제한된 kernel stack을 소진할 수 있다. User stack 크기와 kernel stack 크기는 독립적이다. Stack trace에서 user→kernel boundary를 넘을 때 unwind mechanism도 달라질 수 있다. Kernel stack overflow는 ordinary user stack overflow보다 system stability 영향이 크다.

## CHAPTER 12 · context switch는 register 저장만이 아니라 address-space와 scheduler accounting을 바꾼다

Scheduler가 task A에서 B로 전환하면 callee-saved register, stack pointer, architecture thread state를 저장/복원하고 current task metadata를 바꾼다. Process가 다르면 address-space root/page-table context도 전환될 수 있다. Same-process thread switch는 일부 memory context를 공유하므로 비용 구조가 다를 수 있다. Context-switch count만이 아니라 involuntary/voluntary reason, working-set migration, cache/TLB impact를 함께 본다.

## CHAPTER 13 · FPU/vector register state는 context switch 비용의 큰 상태가 될 수 있다

Modern SIMD/vector extension은 수백~수천 byte의 register state를 가질 수 있다. OS는 task switch 시 이 state를 save/restore하거나 hardware/lazy mechanism을 사용할 수 있다. Wide-vector-heavy workload가 많은 thread를 자주 switch하면 register-state traffic이 증가할 수 있다. Security 문제 때문에 과거 lazy switching strategy가 바뀐 architecture도 있다. Scheduler benchmark는 integer-only task와 vector-heavy task의 switch 비용이 다를 수 있음을 고려한다.

## CHAPTER 14 · address-space identifier는 context switch 때 TLB 전체 flush를 피하는 데 사용될 수 있다

Page-table root가 바뀌어도 TLB entry에 ASID/PCID 같은 address-space tag를 붙이면 여러 process의 translation을 동시에 cache할 수 있다. Tag 재사용 시에는 stale translation을 안전하게 invalidate해야 한다. Address-space switch 비용은 CPU generation과 kernel configuration에 따라 크게 달라진다. Process-per-request와 thread-per-request의 비용을 비교할 때 TLB tagging behavior도 background factor다.

## CHAPTER 15 · kernel/user page-table isolation은 security와 transition cost를 교환할 수 있다

Speculative side-channel mitigation을 위해 user mode에서는 kernel mapping을 최소화하고 syscall/interrupt entry에서 별도 page-table context로 전환하는 정책을 사용할 수 있다. 이런 isolation은 transition마다 address-space switch와 TLB 관련 비용을 추가할 수 있다. 정확한 mitigation은 CPU vulnerability와 OS 설정에 따라 달라진다. Benchmark machine에서 mitigation을 끈 결과를 production 기본 설정 성능으로 보고하지 않는다.

## CHAPTER 16 · speculation barrier와 predictor mitigation은 syscall/VM transition 비용을 바꾼다

Indirect branch predictor state, return stack, speculative data access에 대한 vulnerability 대응으로 barrier, predictor flush/control bit 같은 mitigation이 transition path에 추가될 수 있다. 모든 CPU가 동일 mitigation을 필요로 하지 않고 microcode/kernel update로 정책이 바뀔 수 있다. OS upgrade 후 syscall-heavy workload regression이 생기면 mitigation status와 CPU model을 함께 확인한다. Security control을 성능 때문에 임의 해제하지 않고 risk를 별도 평가한다.

## CHAPTER 17 · blocking syscall은 kernel 안에서 계속 CPU를 쓰는 상태와 다르다

Read가 data를 기다리면 current task가 wait queue에 등록되고 scheduler가 다른 runnable task를 실행할 수 있다. Thread는 syscall frame을 유지하지만 CPU는 점유하지 않는다. Completion event가 task를 wake하면 run queue에서 다시 선택되어 syscall을 마치고 user로 돌아간다. 따라서 syscall wall time = kernel CPU time이 아니다. Off-CPU profile과 scheduler trace로 wait reason을 분리한다.

## CHAPTER 18 · wakeup latency는 event completion과 user continuation 사이 시간이다

I/O가 완료되어 task가 runnable이 되어도 CPU가 즉시 배정된다는 보장은 없다. Run queue length, priority, affinity, CPU idle exit, preemption policy가 wakeup-to-run latency를 결정한다. Device latency가 일정한데 request p99가 늘면 scheduler wakeup delay를 조사한다. Completion timestamp, wakeup timestamp, scheduled-on timestamp를 같은 monotonic clock domain에서 수집한다.

## CHAPTER 19 · preemption은 kernel code가 언제 다른 task에 CPU를 넘길 수 있는지 정의한다

Kernel preemption configuration과 critical section은 scheduler latency에 영향을 준다. Spinlock/interrupt-disabled region에서는 즉시 task switch가 불가능할 수 있다. Low-latency workload는 긴 non-preemptible section을 찾는 trace가 중요하다. Throughput-oriented configuration은 더 큰 batching을 허용할 수 있다. `kernel time`을 하나로 뭉개지 말고 preempt-disabled duration과 scheduling delay를 구분한다.

## CHAPTER 20 · signal delivery는 user return path에 새로운 frame/control flow를 삽입한다

Pending unblocked signal이 있으면 kernel은 user로 돌아가기 전에 signal handler가 실행되도록 user stack/register state를 구성할 수 있다. Handler가 끝나면 sigreturn류 mechanism으로 original context를 복원한다. Signal은 ordinary function call과 달리 비동기 지점에 들어오므로 async-signal-safe operation 제약이 있다. Handler에서 malloc/lock/stdio 같은 non-safe operation을 호출하면 deadlock이나 corruption 위험이 있다.

## CHAPTER 21 · interrupted syscall은 EINTR, partial result, automatic restart 중 하나가 될 수 있다

Signal이 blocking syscall 중 도착하면 operation이 이미 일부 진행됐는지와 restart policy에 따라 결과가 달라진다. read/write가 일부 byte를 처리했다면 positive partial count를 반환할 수 있고, 아직 side effect가 없으면 EINTR 또는 automatic restart가 가능하다. Caller는 `실패면 처음부터 재시도`가 중복 side effect를 만드는지 확인해야 한다. API별 restart semantics를 문서로 확인한다.

## CHAPTER 22 · restartable sequence는 짧은 per-CPU userspace operation의 preemption 문제를 줄인다

일부 runtime은 per-CPU data update처럼 thread가 현재 CPU에 계속 있다는 가정이 필요한 매우 짧은 sequence를 수행한다. Kernel이 preempt/migrate하면 abort handler로 이동시켜 sequence를 재시작할 수 있는 mechanism이 restartable sequence다. 이를 통해 매 operation syscall/atomic cost를 줄일 수 있다. 하지만 signal, migration, registration ABI와 강하게 결합되므로 일반 application optimization으로 남용하지 않는다.

## CHAPTER 23 · seccomp filter는 syscall entry에서 추가 policy evaluation을 수행한다

Sandbox가 syscall number/argument를 BPF-like filter로 검사하면 허용되지 않은 operation을 차단할 수 있다. Filter complexity와 logging/notification mode는 syscall-heavy workload에 overhead를 추가할 수 있다. Security policy를 단순화할 때는 performance보다 attack surface와 least privilege를 우선하고, 실제 overhead는 representative syscall mix로 측정한다. Allowlist 변경은 기능 regression test와 함께 관리한다.

## CHAPTER 24 · ptrace/debugging은 syscall·signal·exception 경계를 의도적으로 멈출 수 있다

Debugger/tracer는 syscall entry/exit, signal delivery, breakpoint에서 traced task를 stop하고 tracer process에 event를 전달할 수 있다. 이 과정은 context switch와 IPC를 추가해 timing-sensitive bug를 숨기거나 만들어 낼 수 있다. `strace를 붙이면 느려진다`는 단순 사실을 넘어 어떤 syscall이 얼마나 자주 stop되는지 이해해야 한다. Production profiling에는 더 낮은 overhead의 tracepoint/eBPF를 선택할 수 있다.

## CHAPTER 25 · probe/instrumentation은 entry path 자체의 비용을 변경한다

Kprobe/ftrace/eBPF가 syscall 또는 scheduler function에 attach되면 handler execution, ring-buffer write, stack collection이 추가된다. 고빈도 syscall에 heavy stack trace를 켜면 관측 비용이 workload보다 커질 수 있다. Lost event가 0인지, sampling/aggregation으로 줄일 수 있는지 확인한다. Instrumented 결과를 baseline과 비교해 probe overhead upper bound를 측정한다.

## CHAPTER 26 · copy 비용은 syscall count와 독립적인 성능 축이다

한 번의 write syscall로 1MB를 복사하는 것과 1,000번의 작은 write는 transition 수와 byte copy가 각각 다르다. Syscall batching은 entry overhead를 줄여도 memory copy bandwidth가 병목이면 speedup이 제한된다. Zero-copy/splice/mmap 같은 mechanism은 copy path를 줄일 수 있지만 lifetime, page pinning, ownership complexity를 추가한다. `syscall 줄임`과 `byte movement 줄임`을 별도 metric으로 본다.

## CHAPTER 27 · io_uring은 많은 I/O 제출·완료에서 transition을 batch하도록 설계될 수 있다

Shared submission/completion ring을 사용하면 application이 여러 operation을 queue하고 kernel과 batch로 동기화할 수 있다. SQ polling 같은 mode는 syscall frequency를 더 줄이는 대신 dedicated CPU cost를 만들 수 있다. Registered buffer/file은 lookup/pinning 비용을 줄이지만 resource lifetime contract를 강화한다. P25 async engine과 user-kernel transition 비용을 연결해 실제 workload에 맞는 mode를 선택한다.

## CHAPTER 28 · shared memory는 data syscall을 줄이지만 synchronization syscall을 없애지는 않는다

두 process가 shared mapping에서 message를 주고받으면 payload copy/transition을 줄일 수 있다. 하지만 producer/consumer coordination에는 atomic, futex, eventfd 같은 mechanism이 필요하고 memory ordering을 직접 설계해야 한다. Large payload에는 유리할 수 있지만 crash isolation과 ownership recovery가 복잡해진다. Copy cost와 synchronization complexity를 함께 비교한다.

## CHAPTER 29 · transition benchmark는 empty syscall과 real workload를 분리한다

Minimal getpid-like path의 ns/op은 architecture transition lower bound에 가깝지만 real read/write/open에는 VFS, permission, copy, cache miss, device wait가 붙는다. Empty benchmark만 보고 storage/network application을 최적화하지 않는다. Perf/ftrace로 entry→handler→block/wakeup→exit 구간을 나누고 cycles, context switches, faults, copied bytes를 기록한다. Security mitigation 상태와 kernel version도 결과 metadata에 포함한다.

## CHAPTER 30 · user/kernel 경계의 최적화는 transition·copy·block·wakeup을 따로 측정한다

System-call-heavy path를 개선할 때 호출 횟수만 세지 말고 per-call byte, user-copy time, kernel CPU, off-CPU wait, wakeup latency, context switch, page fault를 분리한다. VDSO·batching·shared memory·async completion은 서로 다른 비용을 줄이는 도구다. Security policy와 correctness validation을 제거해 빠르게 만들지 않는다. 최종 검증은 실제 production syscall mix에서 latency distribution과 CPU/energy가 개선되는지로 판정한다.