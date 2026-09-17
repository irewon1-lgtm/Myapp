# PART 29 · Process and IPC Internals — clone, exec, fd inheritance, futex, Unix sockets

Process abstraction은 `프로그램 실행 인스턴스`라는 정의보다 훨씬 많은 lifetime과 resource relation을 가진다. Process 생성은 address space·file table·signal disposition·credential을 어떤 단위로 복제/공유할지 결정하고, IPC는 byte 전달뿐 아니라 file descriptor·memory mapping·wake event·credential을 process boundary 너머로 이동시킬 수 있다. Correctness는 **누가 resource를 상속·공유·close하며 peer가 죽었을 때 무엇이 남는가**에 달려 있다.

---

## CHAPTER 01 · fork는 process 전체를 즉시 물리 복사하지 않는다

fork 계열은 parent의 process state를 기반으로 child execution context를 만들지만 modern OS는 address space page를 copy-on-write로 공유해 즉시 전체 memory를 복사하지 않는다. Page table과 metadata는 새 process context를 만들고 write가 발생한 page만 private copy가 생길 수 있다.

Large process에서 fork latency와 post-fork memory spike는 서로 다른 비용이다. Parent/child가 곧 exec할 경우 COW sharing이 특히 중요하다.

---

## CHAPTER 02 · fork 후에는 두 execution path가 같은 point에서 갈라진다

fork 반환 뒤 parent와 child는 각각 execution을 계속하며 return value로 역할을 구분한다. 이 순간부터 mutex/runtime state가 두 process에 복제된다는 점이 중요하다. Multi-threaded process에서 fork한 child에는 호출 thread만 남기 때문에 다른 thread가 잡고 있던 lock state가 복제되면 deadlock 위험이 있다.

Fork-after-threads는 async-signal-safe operation과 exec까지의 제한을 고려해야 한다.

---

## CHAPTER 03 · exec는 PID를 바꾸는 새 process 생성이 아니라 process image 교체다

exec 계열은 현재 process의 address space/code/data/stack를 새 executable image로 교체하지만 process identity의 일부—PID 등—는 유지될 수 있다. File descriptor 중 close-on-exec가 아닌 항목도 살아남는다.

`fork → exec` 조합은 child process를 만들고 그 execution image를 target program으로 교체하는 일반 pattern이다. Debugging에서 fork failure와 exec failure를 분리한다.

---

## CHAPTER 04 · argv와 environment도 untrusted serialized process input이다

exec 시 argument vector와 environment는 새 process startup state로 전달된다. Environment variable은 config·library search path·locale·credential hint에 영향을 줄 수 있어 privilege boundary에서 검증해야 한다.

Secret을 environment에 넣으면 process inspection/core dump/child inheritance로 노출될 수 있다. Config transport와 secret lifetime을 구분한다.

---

## CHAPTER 05 · clone은 어떤 resource를 공유할지 flag로 선택한다

Linux clone/clone3 계열은 address space, file table, filesystem info, signal handlers, namespace 등을 parent와 공유할지 선택해 thread/container primitive의 기반이 된다. Thread는 별도 task identity를 가지면서 address space와 많은 resource를 공유하도록 구성할 수 있다.

Process와 thread를 완전히 다른 kernel object로 생각하기보다 **task가 무엇을 공유하는가**로 이해하면 namespace/container/thread behavior가 연결된다.

---

## CHAPTER 06 · PID와 task ID는 namespace에 따라 다르게 보일 수 있다

Host와 container 내부에서 같은 task가 다른 PID를 가질 수 있고 thread group leader와 individual thread ID도 구분된다. Signal, ptrace, procfs path를 다룰 때 어떤 ID namespace와 scope를 사용하는지 확인해야 한다.

Log correlation은 숫자 PID만 저장하지 말고 namespace/container identity를 함께 남긴다.

---

## CHAPTER 07 · File descriptor table은 process-local handle table이고 open file description과 다르다

File descriptor 번호는 process file table의 index이며 여러 fd가 같은 underlying open file description을 가리킬 수 있다. dup/fork로 descriptor가 복제돼도 file offset/status flag를 공유하는 경우가 있다.

`fd 번호가 다르니 독립 file`이라는 가정은 틀릴 수 있다. Reference 관계를 이해해야 close/offset race를 설명할 수 있다.

---

## CHAPTER 08 · CLOEXEC는 child process로 resource가 새는 것을 막는 기본 lifetime control이다

Server가 socket/file fd를 열어 둔 상태에서 unrelated program을 exec하면 fd가 상속돼 peer가 예상보다 오래 살아 있을 수 있다. Close-on-exec flag는 exec 시 descriptor를 자동 close해 accidental inheritance를 막는다.

Race-free하게 fd creation과 CLOEXEC 설정을 한 operation으로 수행하는 API를 선호한다. Open 후 별도 fcntl 사이에 다른 thread가 fork할 수 있기 때문이다.

---

## CHAPTER 09 · Pipe는 byte stream이며 message boundary를 일반적으로 보장하지 않는다

Pipe는 writer와 reader 사이 kernel buffer를 제공한다. Read는 writer의 write call 경계를 그대로 반환한다고 가정하면 안 된다. 특정 크기 이하 write의 atomicity guarantee는 존재할 수 있지만 protocol framing은 별도로 설계해야 한다.

Reader가 모두 close되면 writer는 broken-pipe condition을 관찰할 수 있고 writer가 모두 close되면 reader는 EOF를 본다. Endpoint lifetime 자체가 protocol signal이다.

---

## CHAPTER 10 · Pipe capacity가 backpressure를 만든다

Writer가 reader보다 빠르면 pipe buffer가 차고 blocking write는 sleep하거나 nonblocking write는 EAGAIN을 반환한다. Pipe는 단순 transport가 아니라 bounded queue다.

Producer/consumer throughput mismatch를 pipe size 증가로 숨기기보다 queue occupancy와 service rate를 측정한다.

---

## CHAPTER 11 · Unix domain socket은 local IPC에 socket semantics를 제공한다

AF_UNIX socket은 stream/datagram/seqpacket 형태로 local process 통신을 제공하며 filesystem path 또는 abstract namespace로 address를 표현할 수 있다. Network stack 전체를 거치지 않더라도 socket buffer, credential, ancillary data semantics를 가진다.

Peer crash, half-close, backlog, send buffer pressure를 network socket과 유사한 state machine으로 다룬다.

---

## CHAPTER 12 · SCM_RIGHTS는 fd 번호가 아니라 underlying open file reference를 전달한다

Unix domain socket ancillary message로 file descriptor를 다른 process에 전달할 수 있다. Receiver는 자신의 fd table에 새 번호를 얻지만 underlying open file object는 shared reference가 될 수 있다.

이 기능은 privilege separation에 강력하다. Privileged broker가 file/socket을 열고 capability 역할의 fd만 unprivileged worker에 전달할 수 있다. 전달 가능한 권한을 path string보다 좁게 만들 수 있다.

---

## CHAPTER 13 · Descriptor passing은 capability security model로 사용할 수 있다

Path 기반 권한은 receiver가 다시 namespace lookup을 수행하지만 이미 열린 fd를 전달하면 broker가 선택한 object에 대한 authority를 직접 넘긴다. 이후 filesystem path가 rename/unlink돼도 open fd가 object를 유지할 수 있다.

권한 검토는 `어떤 fd가 누구에게 전달되는가`를 graph로 본다. SCM_RIGHTS 수신 후 필요 없는 capability는 즉시 close한다.

---

## CHAPTER 14 · Shared memory는 copy를 줄이지만 synchronization 책임을 application에 넘긴다

shm/memfd/mmap으로 여러 process가 같은 physical page-backed region을 mapping하면 IPC copy를 줄일 수 있다. 하지만 data structure consistency, ownership, wakeup은 자동으로 제공되지 않는다.

Shared memory payload와 control channel을 분리해 sequence number, producer/consumer index, futex/eventfd를 결합할 수 있다. Weak-memory ordering도 process 경계를 넘어서 동일하게 적용된다.

---

## CHAPTER 15 · memfd는 anonymous file object를 fd capability로 다루게 한다

memfd는 filesystem pathname 없이 file-like object를 만들어 mmap, sealing, fd passing에 사용할 수 있다. Shared blob이나 executable image transport에서 path race를 줄일 수 있다.

Sealing은 이후 grow/shrink/write 같은 mutation을 제한해 receiver가 immutable assumption을 가질 수 있게 한다. Seal을 언제 적용했는지가 publication contract다.

---

## CHAPTER 16 · Futex는 uncontended path를 userspace에 두고 contention 때 kernel 도움을 받는다

Fast userspace mutex는 atomic state를 userspace memory에서 변경하고 경쟁이 없으면 syscall 없이 lock/unlock할 수 있다. Waiter가 생길 때 futex syscall로 sleep/wakeup queue를 kernel에 맡긴다.

Futex word 자체가 mutex algorithm 전체가 아니다. Owner state, waiter bit, memory ordering, priority inheritance를 runtime이 설계한다.

---

## CHAPTER 17 · Lost wakeup을 피하려면 value check와 sleep transition이 atomic하게 연결돼야 한다

Waiter가 condition을 확인한 직후 notifier가 signal하고, waiter가 그 뒤 sleep하면 wakeup을 놓칠 수 있다. Futex wait는 expected value와 실제 word를 kernel에서 다시 비교해 state가 이미 바뀌었으면 sleep하지 않게 한다.

Condition variable도 predicate를 lock 아래 재확인하는 이유가 같은 race를 막기 위해서다.

---

## CHAPTER 18 · Spurious wakeup을 protocol error로 취급하지 않는다

Wait primitive는 signal 외 이유로 wake할 수 있으므로 waiter는 깨어난 뒤 predicate를 다시 검사해야 한다. `wait returns = condition true`가 아니라 `wait returns = 다시 검사할 기회`다.

Loop-based predicate check는 condition variable/futex protocol의 핵심 invariant다.

---

## CHAPTER 19 · Priority-inheritance futex는 RT lock inversion을 kernel과 협력해 줄인다

High-priority waiter가 low-priority owner를 기다릴 때 PI futex는 owner priority를 일시적으로 boost해 lock release를 앞당길 수 있다. Scheduler class와 lock wait graph가 결합되는 mechanism이다.

PI를 켠다고 deadlock이 사라지는 것은 아니다. Lock ordering과 bounded critical section은 여전히 필요하다.

---

## CHAPTER 20 · Robust futex는 owner death를 lock state에 반영한다

Process/thread가 mutex를 보유한 채 죽으면 ordinary lock state는 영원히 locked로 남을 수 있다. Robust-list mechanism은 kernel이 task exit 시 owned futex를 표시해 다음 waiter가 owner-dead state를 감지하게 한다.

Recovery code는 protected data가 중간 mutation 상태일 수 있음을 인식하고 invariant를 복구한 뒤 lock를 consistent로 전환해야 한다.

---

## CHAPTER 21 · eventfd는 counter 기반 wakeup channel이다

Eventfd는 64-bit counter를 fd로 노출해 producer가 value를 쓰고 consumer가 read/poll로 event를 받을 수 있다. Shared memory queue에 data를 넣고 eventfd로 wakeup만 전달하는 pattern에 적합하다.

Counter saturation, semaphore mode, edge/readiness semantics를 이해해야 coalesced wakeup을 올바르게 처리할 수 있다.

---

## CHAPTER 22 · signalfd는 asynchronous signal delivery를 fd event loop로 통합한다

Signal을 traditional async handler에서 처리하는 대신 signal mask와 signalfd를 사용해 fd read로 consume할 수 있다. 이를 통해 handler context의 async-signal-safe 제약을 줄이고 epoll/event loop에 signal을 통합할 수 있다.

Process 전체 signal mask/thread routing을 올바르게 설정하지 않으면 일부 signal이 traditional handler로 빠질 수 있다.

---

## CHAPTER 23 · timerfd는 timer expiration을 fd readiness/counter로 표현한다

Timerfd는 monotonic/realtime clock 기반 timer를 fd로 제공해 event loop와 통합한다. Read value는 expiration count를 알려줘 consumer가 늦어서 여러 timer period를 놓쳤는지 알 수 있다.

Periodic timer에서 read 한 번을 tick 한 번으로 가정하지 말고 overrun count를 처리한다.

---

## CHAPTER 24 · Process supervision은 child exit와 restart policy를 분리한다

Parent/supervisor는 wait 계열로 child exit status를 수집해 zombie를 제거하고 crash/normal exit를 구분한다. Restart 여부는 별도 policy다. 무한 즉시 restart는 crash loop로 CPU/log를 소모할 수 있다.

Backoff, restart budget, permanent failure classification을 둬야 한다.

---

## CHAPTER 25 · wait status는 exit code 외 signal/core-dump state를 포함한다

Child가 `_exit(3)`으로 종료한 것과 SIGSEGV로 죽은 것은 같은 code path가 아니다. Supervisor는 정상 exit status, terminating signal, stopped/continued state를 구분해야 한다.

Crash telemetry는 binary build ID와 signal/fault address를 함께 남긴다.

---

## CHAPTER 26 · pidfd는 PID reuse race를 줄이는 process handle이다

PID 숫자는 process exit 후 재사용될 수 있어 `PID 1234를 나중에 kill`하는 code가 다른 process를 겨냥할 위험이 있다. pidfd는 특정 process identity를 file descriptor로 참조해 poll/signal/wait와 결합할 수 있다.

숫자 identifier보다 lifetime-bound handle이 race-free resource management에 유리하다.

---

## CHAPTER 27 · Process group/session은 terminal·signal broadcast semantics를 만든다

Shell/job control은 process group과 session을 사용해 pipeline의 여러 process에 signal을 전달하고 foreground terminal ownership을 관리한다. Daemonization에서 session detach와 controlling terminal 관계가 중요하다.

SIGTERM을 한 PID에만 보내 child tree가 남는 문제는 process group/cgroup supervision을 검토해야 한다.

---

## CHAPTER 28 · IPC protocol은 peer credential을 transport identity와 연결해야 한다

Unix socket은 peer credential을 kernel에서 조회할 수 있어 pathname claim보다 실제 UID/GID/PID identity를 검증하는 데 사용할 수 있다. Privileged daemon은 client가 보내는 `userId` field를 신뢰하기보다 transport credential과 authorization policy를 연결한다.

IPC security도 authentication과 authorization을 분리한다.

---

## CHAPTER 29 · IPC leak은 fd와 shared-memory object의 reference graph로 찾는다

Pipe end 하나가 예상치 못한 child에 상속되면 EOF가 오지 않고, memfd mapping/reference가 남으면 storage/memory가 해제되지 않을 수 있다. Resource leak은 allocation site보다 **누가 마지막 reference를 보유하는가**가 중요하다.

/proc fd map, process tree, namespace/cgroup identity를 이용해 reference owner를 찾는다.

---

## CHAPTER 30 · Process/IPC 설계의 최종 계약은 identity·inheritance·ownership·wakeup·failure다

Process boundary를 검토할 때 다섯 질문을 사용한다.

1. **Identity** — PID/fd/credential이 어느 namespace와 lifetime에 속하는가.
2. **Inheritance** — fork/exec 시 어떤 state와 descriptor가 복제되거나 제거되는가.
3. **Ownership** — pipe/socket/shared-memory object의 마지막 reference를 누가 close하는가.
4. **Wakeup** — condition change와 sleep transition 사이 lost-wakeup race가 없는가.
5. **Failure** — peer death/owner death/partial protocol state에서 invariant를 어떻게 복구하는가.

IPC API를 선택하는 것보다 이 다섯 invariant를 만족하는 protocol을 만드는 것이 더 중요하다.
