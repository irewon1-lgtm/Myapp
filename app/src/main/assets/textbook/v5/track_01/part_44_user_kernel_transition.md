# PART 44 · User↔Kernel Transition — syscall entry, context state, vDSO, restart

user code와 kernel은 같은 CPU를 사용하지만 권한과 address space contract가 다르다. syscall, fault, interrupt는 통제된 entry path를 통해 privileged state로 전환하고, user pointer와 register를 검증하며, scheduler·signal과 상호작용한 뒤 다시 돌아온다. 이 PART는 **transition cost, copy boundary, context switch, mitigation, batching과 observability**를 연결한다.

---

## CHAPTER 01 · syscall ABI는 user register state를 kernel request로 해석하는 규칙이다

system call은 syscall number와 argument를 architecture ABI가 정한 register/stack 위치에 놓고 kernel entry instruction을 실행한다. kernel은 이를 신뢰 가능한 function call이 아니라 untrusted request로 받아 type·range·pointer를 검증해야 한다.

32/64-bit compatibility mode나 seccomp wrapper가 있으면 같은 logical API도 다른 syscall ABI를 사용할 수 있다. userspace library wrapper와 raw syscall semantics도 다를 수 있다.

trace에는 syscall number, raw errno, duration을 보존한다. high-level function 이름만으로 kernel path를 추정하지 않는다.

---

## CHAPTER 02 · privilege transition은 CPU execution mode와 access 권한을 바꾼다

user mode에서 kernel mode로 들어갈 때 CPU는 privileged instruction과 kernel memory에 접근 가능한 context로 전환한다. transition은 isolation을 유지하기 위한 필수 경계이며 단순 function call보다 state save/validation이 많다.

speculative execution mitigation이나 address-space isolation이 추가되면 cost가 커질 수 있지만 security requirement와 함께 평가해야 한다. syscall cost를 줄이려고 boundary를 우회하는 것은 해결책이 아니다.

microbenchmark에서는 empty syscall과 real workload를 분리한다. 실제 latency 대부분이 I/O wait일 수 있다.

---

## CHAPTER 03 · entry prologue는 user state를 저장하고 safe kernel state를 만든다

kernel entry code는 return에 필요한 register/flags를 보존하고 kernel stack, segment/address state를 준비한다. user-controlled register를 kernel pointer로 사용하기 전에 sanitization이 필요하다.

entry code는 모든 process가 자주 통과해 작고 검증된 path여야 한다. 한 bug가 system-wide privilege boundary에 영향을 준다.

low-level crash에서는 saved frame과 entry path version을 확인한다. profiler가 frame transition을 올바르게 unwind하는지도 중요하다.

---

## CHAPTER 04 · transition cost는 fixed overhead와 handler work를 분리해야 한다

syscall entry/exit에는 일정한 overhead가 있지만 실제 call latency는 lock, page fault, scheduler, device I/O가 훨씬 크게 차지할 수 있다. “syscall이 느리다”는 말은 어느 구간이 느린지 분해해야 의미가 있다.

많은 tiny syscall을 batching하면 fixed overhead와 cache transition을 줄일 수 있다. 하지만 batch size가 커지면 latency와 cancellation granularity가 나빠진다.

entry→handler→sleep→wakeup→exit를 trace한다. CPU cycles와 wall time을 따로 본다.

---

## CHAPTER 05 · user copy는 untrusted pointer에서 kernel-owned buffer로 data를 이동한다

kernel은 user pointer를 직접 신뢰할 수 없고 access 가능 범위를 검증하면서 copy해야 한다. copy 도중 page fault가 발생할 수 있고 user process가 concurrent하게 memory를 바꿀 가능성도 고려해야 한다.

length overflow나 nested pointer를 검증하지 않으면 kernel memory corruption으로 이어진다. large copy는 cache와 memory bandwidth를 소비한다.

syscall input size와 copy fault를 metric으로 둔다. zero-copy API로 바꿀 때 ownership과 pinning 비용까지 비교한다.

---

## CHAPTER 06 · user-memory TOCTOU는 validation과 use 사이 mutation을 악용할 수 있다

kernel이 user struct를 한 번 검증한 뒤 나중에 같은 user address를 다시 읽으면 다른 thread가 그 사이 내용을 바꿀 수 있다. pointer, length, flag가 변하면 check한 object와 실제 사용 object가 달라진다.

필요한 metadata를 kernel buffer에 한 번 copy해 snapshot으로 사용하거나 atomic access protocol을 설계한다. path-based filesystem check도 유사한 identity race를 가진다.

fuzzing에서 shared user memory를 concurrent mutation시킨다. validation 성공을 lifetime-long permission으로 해석하지 않는다.

---

## CHAPTER 07 · vDSO는 일부 kernel 정보를 user mode에서 syscall 없이 읽게 한다

clock get 같은 operation은 kernel이 read-only/shared data와 user-space helper를 제공해 privilege transition을 피할 수 있다. vDSO fast path가 실패하거나 지원되지 않으면 syscall fallback을 사용할 수 있다.

user-space code가 kernel data update와 일관된 snapshot을 읽도록 sequence protocol이 필요하다. 단순 shared variable보다 정교한 consistency가 포함된다.

benchmark에서 vDSO 사용 여부를 확인한다. container/architecture별 fallback 차이가 latency에 영향을 줄 수 있다.

---

## CHAPTER 08 · page fault는 user instruction에서 kernel memory-management path로 전환한다

user load/store가 missing/protected page를 만나면 kernel fault handler가 mapping을 해결하거나 signal을 전달한다. application에는 ordinary memory access 한 줄로 보이지만 major fault면 storage I/O까지 포함될 수 있다.

fault handling 중 scheduler가 다른 task를 실행할 수 있어 return latency가 길어진다. COW fault와 invalid pointer crash를 같은 category로 보지 않는다.

fault address와 VMA, service time을 trace한다. startup에서 first-touch fault가 집중되는지 확인한다.

---

## CHAPTER 09 · interrupt entry는 현재 task와 무관한 external event를 kernel에 전달한다

device interrupt는 user process가 실행 중이어도 CPU를 kernel handler로 전환할 수 있다. interrupted task의 context는 나중에 이어서 실행할 수 있게 보존된다.

high interrupt rate는 application instruction budget을 줄이고 cache locality를 깨뜨릴 수 있다. affinity가 한 CPU에 몰리면 특정 thread latency가 나빠진다.

IRQ source, handler duration, interrupted workload를 연결한다. application regression이 code change 없이 device traffic 증가에서 올 수 있다.

---

## CHAPTER 10 · NMI path는 일반 kernel lock assumption보다 강한 제약을 가진다

NMI는 일반 interrupt mask 상태에서도 들어올 수 있어 이미 lock을 보유한 code를 interrupt할 수 있다. handler는 NMI-safe data structure와 logging만 사용해야 deadlock을 피한다.

watchdog NMI가 stack을 수집할 때 profiler/debugger와 경쟁할 수 있다. nested NMI 가능성도 platform-specific하게 관리한다.

NMI record는 root cause보다 관찰 mechanism일 수 있다. interrupted instruction과 prior stall을 분석한다.

---

## CHAPTER 11 · kernel stack은 user stack과 분리된 privileged execution resource다

syscall/interrupt handling은 trusted kernel stack에서 실행해 user가 stack content를 직접 조작하지 못하게 한다. per-task stack 크기는 제한되어 deep recursion과 large local buffer가 위험하다.

context switch 시 kernel stack identity도 task와 함께 바뀐다. hardirq가 별도 stack을 사용할 수 있어 unwind가 이를 이해해야 한다.

stack overflow guard와 usage high-water mark를 monitor한다. driver code의 large stack allocation을 review한다.

---

## CHAPTER 12 · context switch는 scheduler decision을 register와 address-space state로 실현한다

현재 task의 register와 scheduling state를 저장하고 next task state를 복원한다. address space가 바뀌면 TLB/cache 영향이 추가될 수 있다. switch count가 많다는 사실보다 왜 task가 sleep/wakeup하는지가 중요하다.

oversubscription과 lock contention이 context switch를 늘리며, affinity migration은 cache locality를 악화시킨다.

voluntary/involuntary switch와 runnable wait를 분리한다. CPU utilization만으로 scheduling health를 판단하지 않는다.

---

## CHAPTER 13 · vector/FPU state는 context switch에서 lazy/eager save 정책의 대상이 된다

wide SIMD register는 많은 state를 가지므로 task 전환 때 저장·복원 비용이 존재한다. architecture/runtime은 사용 여부를 추적하거나 optimized mechanism을 적용할 수 있다.

signal handler와 context API가 vector state를 제대로 보존하지 않으면 rare numerical corruption이 생길 수 있다. 새로운 ISA extension은 context structure 크기를 바꾼다.

low-level coroutine/FFI가 custom context switch를 구현한다면 supported register set을 검증한다.

---

## CHAPTER 14 · address tag와 pointer metadata는 user/kernel boundary에서 해석 규칙이 필요하다

일부 architecture는 pointer upper bit에 tag를 허용할 수 있다. kernel syscall이 user pointer tag를 그대로 허용하는지 제거하는지는 ABI에 따라 다르다. 잘못된 sanitization은 valid pointer를 거부하거나 security check를 우회할 수 있다.

memory tagging과 debug allocator가 pointer representation을 바꿀 수 있어 integer cast와 FFI가 민감하다.

syscall boundary test에서 tagged pointer behavior를 명시적으로 검증한다. architecture-specific assumption을 portable library에 숨기지 않는다.

---

## CHAPTER 15 · page-table isolation은 user와 kernel mapping visibility를 더 강하게 분리한다

speculative side-channel mitigation을 위해 user mode에서 kernel mapping을 최소화하고 entry 시 page-table context를 전환하는 전략이 사용될 수 있다. 이는 TLB와 transition overhead를 늘릴 수 있다.

security mitigation을 끄면 microbenchmark는 빨라질 수 있지만 threat model이 바뀐다. production config와 같은 조건에서 측정해야 한다.

mitigation status와 syscall-heavy workload를 함께 benchmark한다. CPU generation별 비용 차이를 일반화하지 않는다.

---

## CHAPTER 16 · speculation mitigation은 privilege transition path에 additional serialization을 넣을 수 있다

branch predictor state, return prediction, store bypass 같은 speculative mechanism이 privilege boundary 정보를 leak하지 않게 barrier·flush가 추가될 수 있다. mitigation 조합은 CPU와 vulnerability에 따라 다르다.

모든 workload가 같은 overhead를 받는 것은 아니다. syscall/VM-exit 빈도가 높은 service가 더 민감할 수 있다.

active mitigation과 microcode를 결과에 기록한다. security update 뒤 performance regression을 source code와 분리해 분석한다.

---

## CHAPTER 17 · blocking syscall은 kernel 안에서 task를 sleep state로 전환할 수 있다

read, poll, futex 같은 syscall은 조건이 만족되지 않으면 current task를 wait queue에 등록하고 scheduler에 CPU를 양보한다. 함수가 kernel mode라고 계속 CPU를 사용하는 것은 아니다.

wakeup condition과 timeout/cancel이 race할 수 있어 wait queue protocol이 정확해야 한다. signal이 interruption을 만들 수도 있다.

syscall latency를 on-CPU와 off-CPU wait로 나눈다. longest wait의 wakeup source를 찾는다.

---

## CHAPTER 18 · wakeup latency는 event 발생부터 task가 실제 CPU를 얻기까지의 지연이다

I/O가 완료되어 task가 runnable이 돼도 높은 priority work와 runqueue 때문에 바로 실행되지 않을 수 있다. latency-sensitive service는 device latency가 짧아도 scheduler delay로 p99가 나빠질 수 있다.

CPU affinity와 cgroup quota가 runnable delay를 늘릴 수 있다. thread를 더 늘리면 오히려 queue가 커진다.

wakeup timestamp와 scheduled-in timestamp를 trace한다. off-CPU profile에서 wait reason을 분리한다.

---

## CHAPTER 19 · kernel preemption model은 long kernel work가 task latency에 미치는 방식을 바꾼다

kernel code가 어느 지점에서 higher-priority task에 CPU를 양보할 수 있는지 preemption configuration과 context가 결정한다. preempt-disabled critical section이 길면 wakeup된 task가 기다린다.

throughput과 real-time latency가 서로 다른 trade-off를 가질 수 있다. driver가 불필요하게 preemption을 오래 막지 않게 한다.

preempt-off latency tracer를 사용한다. configuration 차이를 benchmark 결과에 포함한다.

---

## CHAPTER 20 · signal delivery는 syscall return과 user context를 수정할 수 있다

pending signal은 kernel이 user mode로 돌아가기 전에 handler frame을 준비하며 전달될 수 있다. blocking syscall 중 signal이 왔다면 interruption/restart semantics가 추가된다.

handler가 runtime/library state를 건드리면 async-signal-safety 문제가 생긴다. multi-thread에서는 signal target selection도 중요하다.

syscall, signal, handler entry를 같은 trace에 연결한다. random EINTR을 network bug로 오인하지 않는다.

---

## CHAPTER 21 · syscall restart는 interrupted operation의 partial progress를 고려해야 한다

kernel/libc가 syscall을 자동 재시작할 수 있지만 모든 operation이 동일하지 않다. partial read/write가 이미 수행되면 전체 요청을 처음부터 반복해서는 안 된다.

timeout은 restart 후 remaining time을 사용해야 end-to-end deadline을 지킬 수 있다. absolute deadline API가 유리한 이유다.

fault test로 signal을 I/O 중간에 주입한다. byte count와 side effect를 검증한다.

---

## CHAPTER 22 · rseq는 짧은 per-CPU sequence를 migration 없이 실행하도록 돕는다

restartable sequence 계열은 userspace가 current CPU와 per-CPU data를 빠르게 다루되 중간에 preemption/migration이 발생하면 sequence를 abort/restart하도록 지원한다. syscall 없이 fast path를 만들 수 있지만 exact ABI와 critical section 규칙이 필요하다.

signal과 preemption이 sequence를 끊을 수 있어 state commit 위치를 명확히 해야 한다. misuse는 per-CPU data corruption을 만든다.

runtime library 구현을 직접 복제하지 않는다. CPU migration stress로 semantics를 검증한다.

---

## CHAPTER 23 · seccomp는 syscall surface를 policy boundary로 제한한다

seccomp filter는 process가 허용된 syscall과 argument pattern만 kernel에 요청하도록 제한해 sandbox attack surface를 줄인다. filter가 user input을 완전히 검증하는 것은 아니며 허용 syscall 내부 보안은 여전히 필요하다.

필요한 syscall을 빠뜨리면 특정 rare feature에서만 process가 죽는다. broad allow를 추가해 문제를 덮으면 sandbox가 약해진다.

violation syscall과 code path를 telemetry로 수집한다. policy 변경은 least-privilege 기준으로 review한다.

---

## CHAPTER 24 · ptrace는 다른 process state를 관찰·수정하는 강력한 debug boundary다

ptrace 계열은 register, memory, syscall event를 관찰해 debugger와 tracer를 구현할 수 있다. 이 권한은 target process confidentiality와 integrity에 직접 영향을 줘 permission과 namespace policy가 중요하다.

single-step/breakpoint가 target timing을 크게 바꾸고 signal delivery semantics에도 개입한다. ptrace로만 재현되는 race는 observer effect를 의심한다.

production access를 제한하고 attachment audit를 남긴다. crash debugging용 권한을 상시 broad capability로 주지 않는다.

---

## CHAPTER 25 · probe overhead는 transition path를 측정하면서 그 path를 바꿀 수 있다

syscall tracepoint, kprobe, eBPF를 모든 event에 걸면 high-frequency path에서 buffer와 execution overhead가 커진다. 문제를 보기 위해 켠 instrumentation이 latency를 악화시킬 수 있다.

sampling, filtering, per-CPU buffer로 overhead를 제한한다. dropped event가 생기면 trace가 완전한 timeline이 아님을 표시해야 한다.

instrumentation on/off performance를 비교한다. 관측 overhead budget을 SLO에 포함한다.

---

## CHAPTER 26 · copy cost는 syscall count보다 bytes와 memory hierarchy에 좌우된다

user↔kernel data copy는 fixed transition 외에 실제 memory bandwidth와 cache pollution을 소비한다. large network/file I/O에서는 copy가 주요 CPU cost가 될 수 있다.

batching, mmap, zero-copy는 copy를 줄이지만 pinning, lifetime, page fault 같은 다른 비용을 만든다. 작은 payload에서는 setup overhead가 더 크다.

bytes copied per request와 cycles/byte를 측정한다. zero-copy를 이름만으로 선택하지 않는다.

---

## CHAPTER 27 · io_uring batching은 여러 I/O submission/completion의 transition 수를 줄인다

shared ring과 batch submit을 사용하면 각 operation마다 syscall을 반복하는 fixed overhead를 줄일 수 있다. 그러나 queue depth와 completion handling이 새 backpressure point가 된다.

ring에 너무 많은 request를 밀면 downstream device queue와 memory lifetime이 커진다. correctness는 descriptor/buffer ownership을 completion까지 유지해야 한다.

syscall rate와 I/O latency를 함께 비교한다. batching으로 p99가 나빠지지 않는지 확인한다.

---

## CHAPTER 28 · shared memory는 user/kernel copy를 줄여도 synchronization 책임을 application에 넘긴다

producer/consumer가 같은 page를 mapping하면 copy 없이 data를 교환할 수 있지만 ownership flag, memory ordering, lifetime을 직접 설계해야 한다. stale reader와 overwrite race가 생기기 쉽다.

untrusted process 사이 shared memory는 data validation과 permission이 추가로 필요하다. huge shared region은 memory pressure를 만든다.

sequence number와 generation으로 slot state를 검증한다. copy 절감과 synchronization CPU를 모두 측정한다.

---

## CHAPTER 29 · transition benchmark는 empty path와 realistic handler를 분리해야 한다

empty syscall nanosecond 수치는 hardware/mitigation overhead를 보여 주지만 database·network request latency를 직접 예측하지 못한다. realistic benchmark는 pointer copy, scheduler wait, cache state를 포함한다.

frequency scaling, ASLR, tracing 설정도 결과를 흔든다. 여러 반복과 CPU pinning을 사용한다.

benchmark 목적을 fixed-overhead 측정인지 end-to-end 개선인지 명시한다. counter와 wall time을 함께 제시한다.

---

## CHAPTER 30 · user/kernel contract는 권한, memory, blocking, restart semantics를 명시한다

system-call API는 argument ABI뿐 아니라 user pointer validation, partial result, signal interruption, blocking과 cancellation, security filter를 포함해야 한다. 호출자는 `return 0/-1`만 보고 side-effect boundary를 추측하면 안 된다.

성능 최적화는 transition 자체, copy, queue wait 중 실제 병목을 측정한 뒤 batching·vDSO·shared-memory 같은 방법을 선택한다. security mitigation을 숨은 변수로 남기지 않는다.

최종 검증은 signal, page fault, timeout, concurrent memory mutation을 주입해 **privilege boundary를 넘는 모든 state가 검증되고 복귀·실패 결과가 문서된 contract로 수렴하는지** 확인한다.
