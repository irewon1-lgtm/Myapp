# PART 29 · Process and IPC Internals — fork, exec, fd, futex, supervision

process와 IPC는 단순한 API 목록이 아니다. fork/exec가 address space와 descriptor state를 어떻게 바꾸는지, fd가 어떤 kernel object를 가리키는지, pipe·socket·shared memory가 ownership과 backpressure를 어떻게 표현하는지 이해해야 lifecycle과 failure를 정확히 다룰 수 있다.

---

## CHAPTER 01 · fork는 address space를 copy-on-write 관계로 복제한다

fork 뒤 parent와 child는 같은 virtual content에서 시작하지만 write 시점에 private page가 분리될 수 있다. 따라서 fork 순간 전체 heap을 즉시 복사하는 모델로 이해하면 memory와 latency behavior를 잘못 예측한다. page table 복제와 이후 COW fault가 실제 비용을 만든다.

large heap process가 fork 뒤 양쪽에서 많은 page를 수정하면 private RSS가 급격히 증가할 수 있다. snapshot-style fork도 child가 오래 살아 있으면 parent write가 COW overhead를 지속적으로 만든다.

fork 전후 RSS/PSS, COW fault, child lifetime을 측정한다. memory pressure에서 fork failure와 post-fork spike를 별도 scenario로 테스트한다.

---

## CHAPTER 02 · multi-threaded fork는 child에 호출 thread만 남긴다

multi-threaded process에서 fork한 child에는 fork를 호출한 thread만 이어지는 semantics가 일반적이다. 다른 thread가 잡고 있던 user-space mutex나 allocator internal lock state는 memory에 남을 수 있어 child가 같은 lock을 사용하면 deadlock할 수 있다.

fork 후 복잡한 library call을 실행하고 나중에 exec하는 패턴은 async-signal-safe restriction과 연결된다. pre-fork hook과 child-safe path를 구분해야 한다.

thread count, held-lock inventory, atfork handler를 검토한다. stress에서 다른 thread가 allocation/lock을 잡은 시점에 fork를 반복해 child progress를 확인한다.

---

## CHAPTER 03 · exec는 process identity 일부를 유지하며 program image를 교체한다

exec는 새 process를 만드는 호출이라기보다 현재 process의 address space와 program image를 새 executable로 교체하는 operation이다. PID와 일부 kernel state는 유지되고 close-on-exec가 아닌 descriptor는 새 program으로 전달될 수 있다.

환경 변수, credential transition, signal disposition, cwd가 새 program behavior에 영향을 준다. fork→exec child에서 unintended state가 남으면 privilege와 resource leak가 생긴다.

exec 전후 descriptor list와 credential을 비교한다. spawn wrapper는 전달할 state를 allowlist 방식으로 관리하고 error path에서 child가 안전하게 종료되는지 확인한다.

---

## CHAPTER 04 · argv와 environment는 process startup의 외부 입력이다

argument와 environment는 executable이 시작될 때 전달되는 configuration channel이며 shell quoting, locale, loader variable, secret exposure 같은 위험을 가진다. trusted parent가 실행하더라도 environment를 그대로 상속하면 예상하지 않은 동작이 활성화될 수 있다.

privileged execution에서는 loader/debug variable을 특히 조심해야 한다. command string을 shell에 넘기는 방식과 argument vector를 직접 구성하는 방식은 injection surface가 다르다.

실제 argv/env를 secret redaction 후 audit할 수 있게 한다. process launcher는 필요한 key만 전달하고 unknown environment dependency를 test에서 드러내게 한다.

---

## CHAPTER 05 · clone은 execution context가 무엇을 공유할지 세밀하게 선택한다

clone 계열 primitive는 address space, fd table, filesystem state, signal handler, namespace 등 어떤 resource를 child와 공유할지 flag로 결정할 수 있다. thread와 process가 kernel 내부에서 완전히 별개 primitive라는 단순 모델보다 sharing 조합으로 보는 편이 정확하다.

잘못된 flag 조합은 lifetime ownership을 모호하게 만든다. fd table을 공유한 context 하나가 close한 descriptor가 다른 context에도 영향을 줄 수 있다.

생성 wrapper에서 sharing contract를 문서화한다. namespace/container runtime처럼 clone flag가 많은 path는 exact flag set과 kernel version을 trace artifact로 남긴다.

---

## CHAPTER 06 · PID와 task ID는 관찰 context에 따라 의미가 달라질 수 있다

thread group, individual task, PID namespace가 있으면 'PID'라는 숫자 하나가 process identity 전체를 표현하지 못한다. 같은 process의 thread는 다른 task ID를 가질 수 있고 container 안팎에서 다른 PID가 보일 수 있다.

PID reuse 때문에 오래 저장한 numeric PID가 나중에 다른 process를 가리킬 수도 있다. signal이나 monitor가 stale PID를 사용하면 wrong target을 조작할 위험이 있다.

start time, pidfd 같은 stable handle, namespace identity를 함께 사용한다. telemetry correlation에서 PID 숫자만 영구 key로 쓰지 않는다.

---

## CHAPTER 07 · fd는 open file description에 대한 process-local handle이다

file descriptor는 작은 정수지만 kernel의 open file description을 참조하며 그 object가 file offset과 status flag를 보유할 수 있다. dup나 fork로 여러 fd가 같은 open description을 공유하면 한 쪽의 offset 변화가 다른 쪽에 영향을 줄 수 있다.

fd 숫자는 close 후 빠르게 재사용된다. async callback이 old fd 숫자를 보관했다가 새 connection에 operation을 실행할 수 있다.

fd와 함께 generation, inode/socket identity를 기록한다. close/reuse를 빠르게 반복하는 stress test로 stale-handle bug를 찾는다.

---

## CHAPTER 08 · CLOEXEC는 descriptor capability가 새 program으로 새는 것을 막는다

close-on-exec flag는 exec 시 해당 fd를 자동으로 닫아 child program에 secret file, listening socket, IPC handle이 우연히 상속되는 것을 방지한다. open 후 별도 fcntl로 flag를 설정하면 multi-thread race window가 생길 수 있어 atomic CLOEXEC 옵션이 중요하다.

unintended inherited fd는 resource leak뿐 아니라 privilege transfer가 될 수 있다. child가 해당 handle을 이용해 parent authority를 행사할 수 있기 때문이다.

spawn test에서 `/proc` style fd inventory를 검사한다. sensitive resource 생성 API는 기본적으로 CLOEXEC를 켜는 policy를 사용한다.

---

## CHAPTER 09 · pipe는 message가 아니라 bounded byte stream이다

pipe는 producer가 쓰고 consumer가 읽는 kernel buffer 기반 byte stream이다. application record boundary가 자동 보존된다고 가정하면 framing이 깨질 수 있으며 write size에 따라 atomicity 보장 범위가 달라질 수 있다.

reader가 느리면 pipe buffer가 가득 차 writer가 block되거나 nonblocking write가 실패한다. 이는 자연스러운 backpressure이며 무한 user-space queue로 우회하면 memory 문제로 바뀐다.

pipe occupancy, read/write rate, blocked duration을 추적한다. protocol은 partial read/write를 정상 state로 처리한다.

---

## CHAPTER 10 · pipe backpressure는 process graph 전체에 전파될 수 있다

pipeline에서 downstream process 하나가 멈추면 그 앞 pipe가 차고 upstream writer도 block되면서 여러 process가 연쇄적으로 멈출 수 있다. 각 process가 CPU를 거의 쓰지 않아도 system은 progress하지 못할 수 있다.

stderr/stdout을 parent가 drain하지 않아 child가 pipe write에서 멈추는 subprocess deadlock도 같은 구조다.

wait graph를 pipe endpoint까지 포함해 그린다. supervisor는 child output drain과 exit wait 순서를 명확히 하고 bounded capture policy를 사용한다.

---

## CHAPTER 11 · Unix domain socket은 local IPC에 socket semantics와 identity를 제공한다

Unix socket은 byte stream 또는 datagram/seqpacket semantics를 local process 사이에 제공하며 pathname/abstract namespace를 endpoint discovery에 사용할 수 있다. network socket과 비슷한 API지만 filesystem permission과 peer credential 같은 local-specific 보안 도구가 있다.

socket path directory가 writable하면 endpoint replacement가 가능할 수 있다. stale socket file과 server restart도 lifecycle 문제다.

bind path ownership, peer credential, socket type을 기록한다. restart와 concurrent client connect에서 stale endpoint를 안전하게 처리하는지 확인한다.

---

## CHAPTER 12 · SCM_RIGHTS는 fd를 data가 아니라 capability로 전달한다

Unix socket ancillary data를 통해 fd를 다른 process에 전달하면 receiver는 같은 underlying kernel object에 대한 새 descriptor를 얻는다. path를 다시 lookup하지 않고 이미 열린 authority를 전달한다는 점이 중요하다.

파일 path permission이 바뀌어도 전달받은 fd 권한은 유지될 수 있다. 누가 어떤 fd를 누구에게 넘길 수 있는지가 authorization policy가 된다.

sender/receiver credential과 transferred object identity를 audit한다. unexpected descriptor type이나 excessive rights를 거부하고 lifetime ownership을 명시한다.

---

## CHAPTER 13 · fd는 capability-style authority로 취급할 수 있다

이미 열린 fd는 특정 resource와 operation에 대한 authority를 나타내며, descriptor를 가진 process는 pathname을 다시 resolve하지 않고 resource를 사용할 수 있다. 이 성질을 활용하면 broad path access 대신 narrow handle을 위임할 수 있다.

반대로 fd leak는 authority leak다. sandbox process가 privileged parent의 socket이나 directory fd를 상속하면 filesystem permission만으로 isolation이 유지되지 않는다.

권한 review에 fd flow를 포함한다. spawn, IPC, plugin boundary에서 전달 가능한 descriptor를 allowlist하고 revocation/lifetime을 설계한다.

---

## CHAPTER 14 · shared memory는 copy를 줄이는 대신 synchronization을 직접 요구한다

shared memory는 여러 process가 같은 physical page를 mapping해 data를 직접 읽고 쓸 수 있게 한다. serialization copy를 줄일 수 있지만 cache coherence, memory ordering, object layout, lifetime을 application protocol이 책임져야 한다.

한 process가 crash한 뒤 shared structure 중간 mutation이 남을 수 있다. pointer는 process별 virtual address가 다를 수 있어 raw pointer를 shared format에 저장하면 위험하다.

versioned layout, offset-based reference, robust synchronization을 사용한다. crash injection 뒤 shared state recovery 가능성을 확인한다.

---

## CHAPTER 15 · memfd는 anonymous file과 fd capability를 결합한다

memfd는 pathname 없이 memory-backed file object를 만들고 fd로 전달할 수 있어 shared memory, JIT, sealed artifact 전달에 유용하다. filesystem namespace에 이름을 노출하지 않으면서 일반 file/mmap semantics를 사용할 수 있다.

sealing을 사용하면 size 변경이나 write를 제한해 receiver가 immutable content를 신뢰할 수 있게 할 수 있다. seal 적용 전에 writable mapping이 남는지 semantics를 확인해야 한다.

fd transfer와 seal set을 audit한다. producer/consumer가 content hash와 generation을 공유해 stale artifact 사용을 막는다.

---

## CHAPTER 16 · futex는 uncontended fast path를 user space에 남긴다

futex 기반 lock은 lock word를 user-space atomic으로 먼저 처리하고 contention이 있을 때 kernel wait/wake를 사용해 syscall overhead를 줄인다. kernel은 lock 전체 의미를 관리하기보다 wait queue를 제공하는 경우가 많다.

user-space state transition과 kernel wait 등록 사이 ordering이 틀리면 lost wakeup이 생길 수 있다. spurious wake도 고려해 predicate를 반복 확인해야 한다.

lock word, futex wait/wake, owner를 trace한다. contention 없는 path와 heavy contention path를 별도로 benchmark한다.

---

## CHAPTER 17 · lost wakeup은 state change와 wait 등록 사이 race에서 생긴다

waiter가 predicate를 false로 확인한 뒤 실제 sleep 등록 전에 producer가 state를 true로 만들고 wake를 보내면, protocol이 잘못된 경우 wake 신호가 사라지고 waiter가 영원히 잠들 수 있다. 해결은 wake count를 기억하는 것이 아니라 predicate와 wait transition을 원자적으로 연결하는 것이다.

condition variable/futex API가 제공하는 lock release+sleep semantics를 정확히 사용해야 한다.

deterministic barrier로 problematic interleaving을 강제한다. sleep timing에 의존한 stress만으로 regression을 판단하지 않는다.

---

## CHAPTER 18 · spurious wakeup은 깨어났다는 사실과 predicate 만족을 분리하게 한다

일부 wait primitive는 명시적 state change 없이도 waiter가 깨어날 수 있다. 따라서 wake return 뒤 반드시 shared predicate를 다시 검사해야 한다. `if`보다 `while (!condition) wait` pattern이 필요한 이유다.

여러 waiter가 동시에 깨어나 한 thread가 resource를 소비하면 나머지 thread에게 predicate가 다시 false가 될 수도 있다.

wake reason과 predicate transition을 구분한다. test에서 artificial spurious wake를 주입해 consumer logic이 안전한지 확인한다.

---

## CHAPTER 19 · PI futex는 lock owner의 scheduler priority를 일시적으로 조정한다

priority inheritance futex는 high-priority waiter가 low-priority owner를 기다릴 때 owner의 priority를 boost해 inversion을 줄인다. kernel이 owner relation을 알아야 하므로 일반 futex보다 state와 cost가 복잡하다.

nested PI lock과 owner death는 difficult corner case를 만든다. boost가 적절히 복구되지 않으면 scheduler behavior가 왜곡된다.

waiter/owner priority transition과 lock chain을 trace한다. real-time workload에서 worst-case blocking bound를 확인한다.

---

## CHAPTER 20 · robust futex는 owner death를 shared state로 전달한다

process/thread가 mutex를 보유한 채 죽으면 일반 lock은 영원히 locked 상태로 남을 수 있다. robust futex protocol은 kernel이 owner death를 감지해 다음 waiter가 inconsistent state를 인지하고 recovery할 기회를 준다.

lock을 다시 얻었다고 protected data가 자동 복구되는 것은 아니다. 이전 owner가 mutation 중간에 죽었을 수 있어 application-specific consistency repair가 필요하다.

owner-death path를 fault injection으로 실행한다. recovery 후 data invariant와 lock usability를 모두 검증한다.

---

## CHAPTER 21 · eventfd는 counter 기반 wakeup primitive다

eventfd는 64-bit counter를 fd event로 노출해 thread/process 또는 kernel subsystem이 event loop에 notification을 보낼 수 있게 한다. pipe보다 작은 signaling channel로 사용할 수 있지만 payload 자체를 전달하는 protocol은 아니다.

counter coalescing 때문에 write 횟수와 read event 수가 1:1이 아닐 수 있다. notification count를 business event identity로 사용하면 안 된다.

counter semantics와 nonblocking behavior를 이해한다. 실제 work queue state를 source of truth로 두고 eventfd는 wakeup hint로 사용한다.

---

## CHAPTER 22 · signalfd는 asynchronous signal을 fd event loop로 통합한다

signalfd는 선택한 signal을 file descriptor에서 읽게 해 traditional async signal handler의 제한을 피하고 event-loop context에서 처리할 수 있게 한다. 올바르게 사용하려면 target signal을 thread mask에서 block해 일반 handler와 경쟁하지 않게 해야 한다.

여러 thread의 signal mask가 다르면 signal delivery가 예상과 달라질 수 있다. 모든 signal이 queue count를 보존하는 것도 아니다.

mask configuration, signalfd read, process lifecycle을 함께 테스트한다. shutdown signal이 반드시 control loop에 도달하는지 확인한다.

---

## CHAPTER 23 · timerfd는 timer expiration을 fd readiness로 표현한다

timerfd를 사용하면 monotonic/realtime clock 기반 timer를 epoll 같은 event loop와 통합할 수 있다. read value는 expiration count를 나타낼 수 있어 handler가 늦었을 때 여러 tick이 합쳐졌음을 알 수 있다.

periodic timer handler가 느리면 expiration backlog가 쌓이고 event loop fairness를 해칠 수 있다. wall clock correction의 영향을 받는 clock과 duration용 clock을 구분한다.

expected/actual fire time과 overrun count를 기록한다. suspend/resume와 clock step scenario를 포함해 timer semantics를 검증한다.

---

## CHAPTER 24 · process supervision은 child lifecycle을 state machine으로 관리한다

supervisor는 child spawn, readiness, exit, restart, shutdown을 관리하며 단순히 process가 살아 있는지만 보는 것이 아니다. crash-loop에 무제한 restart를 적용하면 CPU/log storm과 dependency overload를 만들 수 있다.

readiness와 liveness를 구분하고 restart backoff, maximum attempts, terminal failure policy를 둔다. durable job이면 process restart 뒤 work state를 복원해야 한다.

child generation, exit reason, restart count를 기록한다. supervisor 자체 restart에서도 orphan process와 duplicate instance가 없는지 확인한다.

---

## CHAPTER 25 · wait status는 exit code와 signal termination을 구분한다

child 종료 상태는 normal exit code, signal termination, stop/continue 같은 여러 정보를 포함할 수 있다. raw integer를 단순 success/failure로 비교하면 crash 원인을 잃는다.

shell wrapper가 signal termination을 다른 exit code로 변환할 수도 있다. core dump 여부와 OOM kill 같은 external cause도 함께 봐야 한다.

supervisor log에 decoded status와 signal을 저장한다. expected shutdown과 unexpected crash를 서로 다른 alert severity로 처리한다.

---

## CHAPTER 26 · pidfd는 PID reuse 없이 process를 stable handle로 참조한다

pidfd는 특정 process instance에 대한 file descriptor handle을 제공해 numeric PID가 재사용되는 race를 줄일 수 있다. signal과 wait operation을 stable identity에 연결하기 좋다.

PID를 lookup한 뒤 signal을 보내는 사이 target이 종료되고 같은 번호가 다른 process에 배정되는 classic race를 피할 수 있다.

process supervisor는 가능하면 stable handle을 사용한다. pidfd readiness와 child exit timeline을 event loop에 통합해 lifecycle을 단순화한다.

---

## CHAPTER 27 · process group과 session은 terminal/job-control ownership을 표현한다

process group은 related process에 signal을 묶어 전달하고 session은 controlling terminal과 job-control boundary를 만든다. shell pipeline과 daemonization에서 parent-child tree만으로 signal scope를 설명할 수 없는 이유다.

잘못된 process group에 kill을 보내면 unrelated child가 남거나 너무 넓은 group을 종료할 수 있다. daemon이 terminal을 계속 붙잡으면 logout behavior도 달라진다.

PGID/SID/TTY를 incident에 포함한다. subprocess tree shutdown test에서 group signal이 intended process만 정리하는지 확인한다.

---

## CHAPTER 28 · peer credential은 local IPC authorization의 중요한 input이다

Unix socket은 kernel이 알고 있는 peer UID/GID/PID credential을 제공할 수 있어 client가 주장하는 user ID보다 강한 local identity signal로 사용할 수 있다. 하지만 namespace mapping과 privilege delegation을 함께 고려해야 한다.

credential 확인 없이 privileged service가 request를 수행하면 confused-deputy 문제가 생길 수 있다. fd passing과 결합되면 authority transfer가 더 강해진다.

accept 시 peer credential을 capture하고 authorization decision과 함께 audit한다. test에서 다른 UID와 user namespace client를 포함한다.

---

## CHAPTER 29 · IPC reference leak는 peer가 사라져도 kernel object를 살아 있게 할 수 있다

socket, pipe, shared memory, transferred fd는 reference count로 lifetime이 유지된다. 한 process가 종료되어도 다른 process가 reference를 갖고 있으면 object와 underlying file/storage가 계속 남을 수 있다.

leak된 write end 하나 때문에 pipe reader가 EOF를 영원히 못 받거나, deleted file fd가 disk space를 유지하는 일이 가능하다.

process별 fd inventory와 object reference를 추적한다. shutdown 후 expected EOF/cleanup이 발생하는지 integration test로 검증한다.

---

## CHAPTER 30 · IPC contract는 framing, identity, ownership, failure를 함께 정의한다

IPC protocol은 bytes format뿐 아니라 누가 endpoint를 인증하는지, fd/resource ownership이 언제 이동하는지, partial message와 backpressure를 어떻게 처리하는지, peer crash 후 어떤 state가 남는지 명시해야 한다. local IPC도 distributed failure의 축소판이다.

pipe, socket, shared memory를 성능 취향으로 고르지 않는다. message size, copy cost, synchronization, security, recovery 요구를 기준으로 선택한다.

CLEAN 검증에서는 short I/O, full buffer, peer death, fd reuse, cancellation을 서로 다른 failure mode로 실행한다. 정상 request 하나가 성공하는 것만으로 IPC contract를 완료로 보지 않는다.
