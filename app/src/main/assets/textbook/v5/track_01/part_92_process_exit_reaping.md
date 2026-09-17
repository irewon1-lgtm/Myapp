# PART 92 · Process Exit and Reaping Internals — exit_group, resource teardown, zombies, subreapers, pidfd lifetime

프로세스 종료는 `return 0`이나 `exit()` 한 줄로 끝나는 사건이 아니다. 실행 중이던 thread group이 더 이상 사용자 코드를 실행하지 않게 되고, 주소공간·파일·동기화 객체·namespace 관계·부모 자식 관계가 서로 다른 lifetime으로 정리되며, 부모가 관찰해야 할 최소한의 사망 정보는 zombie state로 남을 수 있다. 마지막으로 `wait*()`가 그 정보를 회수해야 process identity의 한 generation이 완전히 닫힌다. 이 경로를 이해하지 못하면 “프로세스는 이미 죽었는데 PID가 왜 남아 있지?”, “fd를 닫았는데 write가 왜 늦게 끝나지?”, “child가 끝났는데 epoll의 pidfd는 왜 readable이지?”, “parent가 죽었는데 child는 누가 거두지?” 같은 현상을 서로 다른 버그로 오해한다. P92는 종료를 한 순간이 아니라 **execution stop → resource teardown → death publication → observation → reap**으로 이어지는 lifetime protocol로 다룬다.

## CHAPTER 01 · exit(), _exit(), exit_group은 같은 “종료”라는 단어 아래 서로 다른 층을 가진다

C library의 `exit()`는 사용자 공간 정리 절차를 포함한다. 등록된 `atexit` handler를 실행하고 stdio stream을 flush하는 등 application-level cleanup을 수행한 뒤 최종적으로 process termination을 요청한다. 반면 `_exit()`와 `_Exit()`는 그런 stdio/atexit 절차를 건너뛰고 즉시 kernel termination 경로로 들어간다. Linux의 glibc `_exit()` wrapper는 오래전부터 raw `exit` syscall 대신 `exit_group`을 사용해 호출한 thread만이 아니라 process의 전체 thread group을 종료시키는 의미를 제공한다. 따라서 fork 직후 child가 exec 실패로 빠져나갈 때 `exit()`를 쓰면 parent에서 복제된 stdio buffer를 child가 다시 flush하여 중복 출력이 생길 수 있고, `_exit()`가 필요한 이유가 생긴다. 핵심은 **library cleanup, thread-group termination, kernel resource teardown을 하나의 “exit”로 뭉개지 않는 것**이다. 어느 층까지 실행되는지를 알아야 destructor 누락과 이중 flush를 정확히 구분할 수 있다.

## CHAPTER 02 · Process exit는 thread 하나의 return과 thread group 전체의 death를 구분해야 한다

멀티스레드 process에서 한 worker function이 return하는 것은 보통 그 thread의 종료일 뿐이며 다른 threads와 process address space는 계속 살아 있다. 반대로 `exit_group` 계열의 process termination은 thread group 전체를 더 이상 사용자 코드가 실행되지 않는 방향으로 몰아간다. 이 차이는 “마지막 thread가 끝났을 때 process가 끝난다”는 lifetime 규칙과 연결된다. Monitoring에서 task 수와 process 수를 혼동하면 worker 하나가 사라진 것을 service process death로 오인할 수 있고, 반대로 leader가 종료 절차에 들어간 뒤 sibling threads가 정리되는 짧은 전이를 놓칠 수도 있다. pidfd도 thread-specific flag 사용 여부에 따라 특정 task exit와 전체 thread-group exit를 다른 readiness로 표현할 수 있다. 종료 설계는 **task lifetime과 thread-group lifetime을 별도 상태로 모델링**해야 하며, shutdown barrier가 필요한 application은 어떤 thread가 process-wide termination 권한을 갖는지도 명확히 해야 한다.

## CHAPTER 03 · 정상 shutdown과 강제 termination은 application cleanup 가능성에서 갈린다

정상 종료는 application이 새 요청 수락을 멈추고, in-flight work를 drain하고, transaction을 정리하고, telemetry를 flush한 뒤 process termination으로 넘어갈 시간을 준다. 반면 SIGKILL처럼 handler를 실행할 수 없는 강제 종료는 사용자 공간 cleanup을 건너뛰므로 “finally가 항상 돈다”, “destructor가 반드시 돈다”, “lockfile을 지운 뒤 죽는다” 같은 가정이 깨진다. 그렇다고 kernel resource가 영원히 leak되는 것은 아니다. Kernel이 추적하는 address space, file references, sockets 같은 자원은 task lifetime과 함께 정리되지만 application protocol의 의미까지 복구해주지는 않는다. 예를 들어 database transaction은 server 측 connection close로 rollback될 수 있어도 외부 API에 이미 보낸 요청은 되돌아오지 않는다. **Kernel cleanup 가능성과 business cleanup 가능성은 다른 문제**다. Production shutdown은 graceful deadline과 hard-kill deadline을 분리하고, hard kill 뒤에도 외부 상태가 일관적인지 별도 recovery protocol로 확인해야 한다.

## CHAPTER 04 · Exit status는 process 전체 상태를 설명하는 report가 아니라 작고 제한된 termination metadata다

전통적인 exit status는 child가 부모에게 전달하는 작은 정수 결과이며 shell과 supervisor가 성공/실패를 구분하는 데 유용하다. 그러나 이것만으로 종료 원인을 완전히 표현할 수 없다. 정상 `exit(status)`인지 signal에 의해 죽었는지, core dump가 생성됐는지, stop/continue state 변화가 있었는지는 `wait` status encoding과 `waitid()`의 `siginfo_t` 같은 구조를 통해 구분된다. `_exit()` 문서가 설명하듯 parent에 전달되는 정상 status는 제한된 bit 폭으로 해석되기 때문에 거대한 application error code를 그대로 보존하는 채널로 쓰는 것도 부적절하다. Supervisor는 exit code 하나를 로그 message처럼 과도하게 의미 부여하지 말고 **termination class, signal, core 여부, timestamp, executable generation, restart attempt**를 함께 기록해야 한다. 그래야 동일한 숫자라도 planned restart와 crash loop를 구분할 수 있다.

## CHAPTER 05 · 죽은 child가 zombie가 되는 이유는 “안 죽어서”가 아니라 부모가 아직 death record를 회수하지 않았기 때문이다

Child가 실행을 끝내면 대부분의 실행 자원은 정리되지만 부모가 나중에 `wait*()`로 exit status와 일부 accounting 정보를 얻을 수 있도록 최소한의 process record가 남을 수 있다. 이 상태가 zombie다. Zombie는 CPU를 실행하는 process가 아니고 일반적인 user memory를 계속 들고 있는 것도 아니지만 PID와 termination 정보를 표현하는 kernel process-table slot을 소비한다. 부모가 자식을 계속 만들면서 wait하지 않으면 zombie가 누적되어 결국 새 process 생성 capacity에 영향을 줄 수 있다. 따라서 `ps`에서 `Z`를 보았을 때 “이 프로세스를 kill해서 없애자”는 접근은 틀렸다. 이미 실행은 끝났으므로 핵심 문제는 **어떤 부모가 왜 reap하지 않았는가**다. Incident 조사에서는 zombie PID보다 PPID와 supervisor wait loop, SIGCHLD policy, parent event loop stall을 먼저 확인해야 한다.

## CHAPTER 06 · Reaping은 child memory를 free하는 행위라기보다 남아 있는 termination identity를 소비하는 행위다

`wait()`, `waitpid()`, `waitid()`가 종료된 child를 회수하면 parent가 death information을 관찰하고 zombie record를 제거할 수 있다. 이 시점을 process generation의 마지막 identity-release 단계로 볼 수 있다. Parent가 status를 읽지 않으면서도 child를 reaped state로 만들 수 있는 signal disposition 정책이 있고, 반대로 `waitid(..., WNOWAIT)`처럼 상태를 관찰하되 즉시 소비하지 않는 방식도 있다. 그래서 “exit event를 봤다”와 “reap까지 끝났다”는 동일하지 않다. pidfd polling도 이 차이를 드러낸다. 현재 Linux 동작에서 process가 종료되어 zombie가 되면 pidfd가 readable해질 수 있고, reaped 뒤에는 hangup event가 관찰될 수 있다. **Death publication과 death record consumption을 별도 milestone으로 두면** supervisor race와 PID reuse 문제를 훨씬 명확하게 설명할 수 있다.

## CHAPTER 07 · waitpid()는 PID 선택과 state selection을 함께 수행하는 synchronization primitive다

`waitpid(pid, status, options)`는 단순한 “종료 코드 읽기” 함수가 아니다. 특정 child, 같은 process group의 child, 임의 child 중 누구를 기다릴지 선택하고, `WNOHANG`, stopped/continued state 같은 option에 따라 어떤 state transition을 관찰할지 결정한다. Blocking wait는 해당 조건이 아직 없으면 caller를 재우고, `WNOHANG`은 event loop가 다른 일을 계속할 수 있도록 즉시 돌아온다. Wait 대상은 **caller의 child 관계**에 묶여 있다는 제약이 핵심이다. 시스템의 아무 PID나 숫자만 안다고 wait할 수 있는 것이 아니다. Supervisor architecture는 process creation ownership과 wait ownership을 같이 설계해야 한다. 여러 worker threads가 wait를 경쟁한다면 누가 어느 child event를 소비했는지 추적해야 하며, 한 thread가 이미 reaped한 child를 다른 thread가 다시 기다릴 수 있다고 가정하면 race가 생긴다.

## CHAPTER 08 · waitid()는 “무슨 일이 일어났는가”와 “그 record를 소비할 것인가”를 더 세밀하게 분리한다

`waitid()`는 `P_PID`, `P_PGID`, `P_ALL`, Linux의 `P_PIDFD` 같은 id type을 사용해 대상을 지정하고, `WEXITED`, `WSTOPPED`, `WCONTINUED`로 관심 state를 선택한다. 특히 `WNOWAIT`는 waitable state를 관찰한 뒤 child를 그대로 waitable하게 남겨 이후 다른 단계에서 실제 reap을 수행할 수 있게 한다. 이 기능은 observer와 reaper 역할을 분리하는 설계에 유용하지만, “나중에 누군가 reap하겠지”라고 두면 zombie retention을 늘릴 수 있다. `siginfo_t`는 종료 signal/code와 PID/UID 같은 구조화 정보를 제공하므로 bitmask를 직접 해석하는 것보다 명확한 경우가 있다. 다만 어떤 interface를 써도 **관찰 ownership**은 필요하다. Event를 본 thread, metrics를 기록한 component, 실제 reap을 수행한 component가 서로 다르면 generation ID를 공유해야 중복 처리와 누락을 막을 수 있다.

## CHAPTER 09 · WNOWAIT는 debugging과 multi-stage supervision에 유용하지만 zombie lifetime을 의도적으로 연장한다

`WNOWAIT`를 사용하면 parent는 child의 waitable event를 확인하면서도 그 event를 소비하지 않을 수 있다. 예를 들어 crash collector가 exit metadata를 먼저 읽고, 별도 supervisor가 최종 accounting을 한 뒤 reap하도록 pipeline을 나눌 수 있다. 하지만 이 기능은 reference처럼 생각해야 한다. Observer가 record를 “봤다”는 사실이 kernel에게 “이제 버려도 된다”는 뜻이 아니므로, 마지막 reaper가 확실히 존재해야 한다. Crash storm에서 수천 children을 WNOWAIT 상태로 오래 유지하면 각각은 작은 record라도 process-table pressure를 만들 수 있다. 따라서 multi-stage design은 `OBSERVED → ENRICHED → REAPED` 같은 명시적 state와 timeout을 두는 편이 안전하다. **관찰을 비파괴적으로 만들수록 최종 소비 책임을 더 엄격하게 추적해야 한다**는 것이 WNOWAIT의 핵심 운영 교훈이다.

## CHAPTER 10 · SIGCHLD는 child state change notification이지 그 자체가 reap operation은 아니다

Child가 종료하거나 특정 state change를 겪으면 parent는 SIGCHLD를 받을 수 있다. Signal handler가 실행됐다고 zombie가 자동으로 사라지는 것은 일반 규칙이 아니다. Handler 또는 main event loop가 `wait*()`를 호출해 대상 children을 회수해야 한다. Signal은 여러 사건이 하나의 pending indication으로 합쳐질 수 있으므로 “SIGCHLD 한 번 받았으니 wait 한 번만 호출”하는 구현은 여러 children이 동시에 끝났을 때 zombies를 남길 수 있다. 흔한 패턴은 signal을 wakeup으로만 사용하고 `waitpid(-1, ..., WNOHANG)`를 더 이상 회수할 child가 없을 때까지 반복하는 것이다. signalfd를 사용해 SIGCHLD를 fd event loop에 통합해도 본질은 같다. **Notification cardinality와 child-event cardinality는 같다고 보장되지 않는다.** 따라서 signal count가 아니라 waitable records를 drain해야 한다.

## CHAPTER 11 · SIGCHLD를 SIG_IGN하거나 SA_NOCLDWAIT를 쓰면 zombie policy 자체가 바뀔 수 있다

Linux/POSIX의 child semantics에는 SIGCHLD disposition과 `SA_NOCLDWAIT`가 termination record 보존에 영향을 주는 규칙이 있다. 이를 사용하면 parent가 일반적인 zombie를 나중에 wait할 필요가 없도록 만들 수 있지만, supervisor가 exit status를 수집해야 하는 workload에는 적합하지 않을 수 있다. 더 미묘한 점은 pidfd를 종료 후에 `pidfd_open()`으로 얻으려는 race와도 연결된다는 것이다. 최신 `pidfd_open` 문서는 child가 이미 종료했더라도 zombie가 아직 reaped되지 않았다면 안정적으로 process를 참조할 수 있는 조건을 설명하며, SIGCHLD ignore/NOCLDWAIT/reap 경쟁이 있으면 그 보장이 달라진다. **Zombie를 없애는 편리한 policy와 정확한 postmortem status 수집은 trade-off**다. Service manager는 어떤 정보가 필요한지를 먼저 정하고 signal disposition을 선택해야 한다.

## CHAPTER 12 · PID 숫자는 reap 뒤 재사용될 수 있으므로 process identity로 쓰기에는 generation 정보가 부족하다

PID는 유한한 namespace의 정수이고 process lifetime이 끝나면 나중에 다른 process에 재할당될 수 있다. “PID 1234에게 signal 보내기”를 비동기 queue에 오래 보관했다가 실행하면 원래 대상은 이미 죽고 새 process가 1234를 사용하고 있을 수 있다. `/proc/1234`를 두 번 읽는 사이에도 generation이 바뀔 수 있다. Zombie가 아직 reaped되지 않은 동안에는 그 PID identity가 보존되지만, reap 이후에는 reuse 가능성을 고려해야 한다. 따라서 long-lived supervisor는 PID 숫자만 state key로 삼지 말고 pidfd처럼 kernel object reference를 사용하거나 start-time/generation metadata를 함께 검증해야 한다. **Process identity는 PID value가 아니라 특정 lifetime generation**이다. 이 원칙은 kill, wait, metrics attribution, cgroup cleanup, crash artifact indexing 모두에 적용된다.

## CHAPTER 13 · pidfd는 process identity를 fd lifetime으로 바꿔 PID-reuse race를 줄인다

`pidfd_open()` 또는 `clone`의 PIDFD 기능으로 얻는 pidfd는 특정 process generation을 가리키는 file descriptor다. Integer PID를 나중에 다시 lookup하는 대신 이미 확보한 reference를 poll하거나 signal API에 전달할 수 있어 PID reuse race를 크게 줄인다. pidfd는 poll/select/epoll과 결합할 수 있고, 대상이 종료되어 zombie가 되면 readiness로 death를 관찰할 수 있다. Child를 가리키는 pidfd라면 `waitid(P_PIDFD, ...)`로 wait 대상도 명시할 수 있다. 이 구조는 event-driven supervisor에서 특히 강력하다. Connection fd와 process pidfd를 같은 readiness architecture에 넣을 수 있기 때문이다. 하지만 pidfd도 magical ownership이 아니다. **pidfd가 process를 살아 있게 실행시키는 것이 아니라 그 generation을 안전하게 참조하게 해주는 handle**이며, reap 책임과 child relation 규칙은 여전히 별도로 이해해야 한다.

## CHAPTER 14 · pidfd readable과 EPOLLHUP를 한 단계로 합치면 exit와 reap의 차이를 잃는다

현재 Linux pidfd polling semantics에서는 일반 process pidfd가 thread-group termination 후 readable해져 exit를 알릴 수 있고, zombie가 reaped되면 hangup state가 나타날 수 있다. 이 두 event는 supervisor가 다른 작업을 수행할 수 있는 경계를 제공한다. Readable 시점에는 `waitid(..., WNOWAIT)`로 status를 관찰하거나 crash metadata를 수집하고, 필요한 절차가 끝난 뒤 최종 reap을 할 수 있다. 반대로 EPOLLIN만 보고 즉시 fd를 닫고 status를 버리면 정확한 종료 원인을 잃을 수 있고, HUP만 기다리면 자신이 reap해야 하는 child를 아무도 reap하지 않는 순환 의존을 만들 수 있다. **pidfd event는 “죽음이 관찰 가능함”과 “죽음 record가 제거됨”을 구분하는 상태 machine**으로 사용하는 편이 명확하다. Event loop code에 이 두 상태를 이름으로 드러내면 race 분석이 쉬워진다.

## CHAPTER 15 · pidfd를 exit 이후에 여는 것보다 process creation과 동시에 확보하는 편이 race surface가 작다

`fork()` 뒤 parent가 `pidfd_open(child_pid)`를 호출하는 짧은 구간에는 child가 매우 빨리 종료하고 다른 component가 reap할 가능성이 있다. 조건이 맞으면 zombie가 남아 있어 pidfd_open이 여전히 가능하지만, SIGCHLD ignore, `SA_NOCLDWAIT`, 다른 waiter의 선행 reap 같은 상황에서는 안정성이 달라진다. 그래서 최신 man-page는 이러한 조건이 보장되지 않는 경우 `clone()`의 `CLONE_PIDFD`를 이용해 process 생성과 pidfd 획득을 같은 creation protocol에 묶는 방법을 설명한다. 이것은 일반적인 systems principle과 같다. **identity lookup을 나중에 수행하는 것보다 object creation과 stable handle 획득을 원자적으로 묶는 편이 안전하다.** Supervisor가 child 생성 주체라면 가능하면 처음부터 stable handle을 확보해 registry에 넣고, 이후 signal/wait/monitoring을 그 handle 중심으로 구성하는 것이 낫다.

## CHAPTER 16 · Parent가 먼저 죽으면 child relation은 사라지지 않고 reparenting으로 재구성된다

Unix process tree는 parent가 사라졌다고 orphan child를 방치하지 않는다. Child는 init 역할을 하는 process 또는 가장 가까운 child subreaper로 reparent될 수 있다. 이 mechanism 덕분에 원래 parent가 죽더라도 나중에 child가 종료했을 때 누군가 reap 책임을 맡을 수 있다. 여기서 PPID는 immutable birth certificate가 아니다. `getppid()`가 parent 종료 뒤 다른 value를 반환할 수 있는 이유다. Application이 “부모 PID가 바뀌면 공격” 같은 invariant를 두면 정상 reparenting과 충돌할 수 있다. 반대로 worker hierarchy가 parent death를 감지해 함께 종료해야 한다면 reparenting만 믿어서는 안 되고 별도 parent-death signal이나 supervisor control channel이 필요하다. **Process tree는 lifetime 중 재구성되는 supervision graph**이며, original creator와 current reaper가 같지 않을 수 있다.

## CHAPTER 17 · Subreaper는 daemon/container supervisor가 고아 descendant의 reap 책임을 중간에서 인수하게 한다

`PR_SET_CHILD_SUBREAPER`를 설정한 process는 descendant들의 중간 reaping anchor가 될 수 있다. 중간 parent가 종료되면 orphan descendant가 곧바로 system-wide init까지 올라가지 않고 가장 가까운 subreaper에게 재부모화될 수 있다. Service manager나 container init이 이 기능을 사용하는 이유는 자신이 관리하는 process subtree 안에서 종료 status와 zombie cleanup을 책임지기 위해서다. 단순히 main application을 PID 1로 실행하고 child processes를 만들게 두면 application이 SIGCHLD/wait semantics를 제대로 구현하지 않았을 때 zombies가 누적될 수 있다. Subreaper를 켜는 것 역시 자동 해결은 아니다. **새로 떠맡은 children까지 drain하는 wait loop**가 필요하다. Supervision ownership이 확대되면 metrics와 restart policy도 original child뿐 아니라 adopted descendant를 식별할 수 있어야 한다.

## CHAPTER 18 · PID namespace의 init process는 그 namespace에서 특별한 reaping 책임과 failure 의미를 가진다

Container의 PID namespace 안에서는 namespace init이 내부 process tree의 최상위 reaper 역할을 한다. 일반 application을 아무 준비 없이 PID 1로 놓았을 때 signal handling과 child reaping behavior가 예상과 다르게 느껴지는 이유가 여기에 있다. Namespace init이 종료하는 사건은 단순한 한 process crash가 아니라 namespace 전체 lifetime과 연결되므로 container runtime의 teardown semantics까지 영향을 준다. Host PID와 namespace PID가 다르기 때문에 crash artifact나 supervisor log에는 어느 namespace에서 어떤 PID generation이었는지 같이 기록해야 한다. **Container에서 “PID 1”은 숫자 하나가 아니라 supervision role**이다. Child spawning library를 사용하는 service라면 container 환경에서 누가 wait/reap을 담당하는지 명확히 하지 않으면 host에서는 없던 zombie 문제가 나타날 수 있다.

## CHAPTER 19 · Parent-death signal은 reaping과 다른 문제인 “부모가 사라졌음을 child가 감지하는 방법”이다

`PR_SET_PDEATHSIG` 같은 Linux 기능은 parent가 죽었을 때 child에게 signal을 보내도록 요청할 수 있어 tightly coupled helper를 정리하는 데 유용하다. 그러나 이것은 child reaping mechanism이 아니다. Signal을 받은 child가 종료하더라도 그 child를 최종적으로 누군가는 wait/reap해야 하고, signal을 무시하거나 처리하는 policy에 따라 child가 계속 실행할 수도 있다. 또한 parent identity race를 막기 위해 설정 시점 전후의 parent 상태를 확인하는 패턴이 필요할 수 있다. Supervision에서는 **parent loss detection, child termination request, child reap**을 세 단계로 나눠야 한다. 이 구분 없이 “부모 죽으면 자식도 자동으로 완전히 사라진다”고 생각하면 descendants가 남거나 zombie가 생기는 이유를 설명하지 못한다.

## CHAPTER 20 · File descriptor close는 process exit의 일부지만 close가 즉시 모든 external effect를 완료한다는 뜻은 아니다

Process가 종료되면 열려 있던 file descriptors에 대한 process-side references가 정리된다. 하지만 P71과 P91에서 본 것처럼 open-file description이나 socket/network operation은 다른 references와 in-flight lifetime을 가질 수 있다. `_exit()` manual도 descriptor close가 pending output과 관련해 예측하기 어려운 delay를 만들 수 있음을 경고한다. 즉 “process가 exit syscall을 불렀으니 즉시 사라진다”는 latency model은 틀릴 수 있다. Pipe writer가 마지막 reference였다면 close가 reader에게 EOF를 가능하게 만들고, socket close는 peer에 FIN/RST 같은 transport effects를 촉발할 수 있지만 remote application completion을 보장하지 않는다. **Exit latency에는 resource teardown의 blocking/flush 특성이 섞일 수 있다.** Hard real-time shutdown budget을 설계한다면 application-level flush와 kernel-level close behavior를 따로 측정해야 한다.

## CHAPTER 21 · Address space teardown은 다른 subsystem이 잡은 reference와 결합되어 즉시 physical free와 같지 않을 수 있다

마지막 thread가 process address space를 더 이상 사용하지 않게 되면 user mappings는 teardown 대상이 되지만, physical page lifetime은 다른 reference에 따라 달라질 수 있다. Shared memory, page cache, pinned pages, DMA, zero-copy network path 같은 subsystem은 process virtual mapping과 별도 lifetime을 가질 수 있다. 그래서 process가 죽었다고 모든 관련 physical memory가 같은 순간 RSS 그래프에서 사라지거나 device pin이 즉시 해제된다고 단정할 수 없다. 반대로 private anonymous memory는 process mm lifetime과 밀접하게 정리된다. Memory incident에서 “process kill했는데 system memory가 바로 안 줄었다”면 leak이라고 결론 내리기 전에 **shared/pinned/page-cache/kernel queue ownership**을 확인해야 한다. Process exit는 address-space ownership 종료이지 시스템 전체 reference graph의 강제 소거 명령이 아니다.

## CHAPTER 22 · Shared memory와 memfd는 creator process가 죽어도 다른 holder가 있으면 살아 있을 수 있다

POSIX/shared mappings, memfd, inherited file descriptors처럼 kernel object가 여러 processes에서 참조되는 경우 creator의 종료는 object 전체의 종료와 동일하지 않다. Parent가 만든 memfd를 child에게 전달한 뒤 parent가 죽어도 child가 fd를 들고 있으면 object lifetime은 계속될 수 있다. Shared mapping도 backing object와 다른 process mappings가 남아 있으면 data가 유지될 수 있다. 이 성질은 IPC에 유용하지만 “owner process 죽으면 secret buffer도 자동 삭제” 같은 security assumption을 깨뜨릴 수 있다. Cleanup은 creator identity가 아니라 **마지막 reference와 storage policy**에 의해 결정된다. Incident response에서 process kill을 data erasure로 취급해서는 안 되며, shared objects와 filesystem-backed artifacts를 별도로 inventory해야 한다.

## CHAPTER 23 · Robust futex와 owner-death 처리는 process crash가 synchronization state에 남기는 흔적을 다룬다

Mutex owner가 critical section 중 죽으면 일반 lock word만 남아서는 다른 waiters가 영원히 잠들 수 있다. Linux robust futex mechanism은 thread가 종료할 때 kernel이 userspace에 등록된 robust-list 정보를 이용해 owner death 상태를 표시하고 waiter가 recovery path로 들어갈 수 있도록 돕는다. 하지만 이것은 protected data를 자동 복구하는 기능이 아니다. Lock을 얻은 다음 caller는 “이전 owner가 중간에 죽었으므로 invariant가 깨졌을 수 있음”을 인식하고 state를 검사·복원해야 한다. 복구 불가능하면 object를 permanently unusable하게 처리할 수도 있다. **Owner death notification과 data consistency recovery는 별도 단계**다. Crash-safe shared-memory structure를 만들 때 robust mutex를 켜는 것만으로 atomic transaction이 생긴다고 생각하면 안 된다.

## CHAPTER 24 · clear_child_tid와 futex wake는 thread join 계열 구현에서 exit publication을 memory location과 연결한다

Linux threading runtime은 thread 종료를 userspace synchronization과 연결하기 위해 child-tid address를 사용한다. `CLONE_CHILD_CLEARTID` 계열 semantics에서는 thread exit 시 kernel이 지정된 userspace address를 clear하고 futex wake를 수행하는 방식이 thread join 구현에 활용된다. 핵심은 join이 단순 polling이 아니라 **thread lifetime publication + waiter wakeup**의 protocol이라는 점이다. Address 자체의 lifetime이 thread보다 짧아지면 kernel이 잘못된 userspace memory를 만질 위험이 있으므로 threading library는 해당 storage를 안전하게 관리해야 한다. Process-wide exit와 개별 thread exit를 구분해야 하는 또 하나의 이유다. Thread object를 free하는 시점도 “thread function return”이 아니라 join/detach protocol과 kernel-side exit completion을 반영해야 한다.

## CHAPTER 25 · Stdio flush와 kernel fd close를 혼동하면 fork/exit에서 중복 데이터와 유실을 동시에 만든다

`exit()`는 stdio buffer를 flush하지만 `_exit()`는 그렇지 않다. 반면 둘 다 최종 kernel termination에서 fd references를 닫는 방향으로 간다. 이 차이는 fork와 결합될 때 중요하다. Parent가 flush되지 않은 stdio buffer를 가진 상태에서 fork하면 child도 userspace buffer의 복사본을 갖게 된다. Parent와 child가 둘 다 `exit()`하면 같은 buffered text가 두 번 write될 수 있다. 반대로 crash 경로에서 `_exit()`나 SIGKILL로 끝나면 userspace buffer는 flush되지 않아 마지막 log가 사라질 수 있다. 그래서 crash diagnosis에서 “로그 마지막 줄이 없으니 그 코드까지 못 갔다”고 단정하면 안 된다. **Buffered logging의 visibility와 kernel process lifetime은 별도 증거**다. 중요한 audit log라면 stdio exit flush에만 의존하지 말고 명시적 durability/remote ingestion contract를 가져야 한다.

## CHAPTER 26 · Orphaned process group은 terminal/job-control semantics와 결합되어 단순 reparenting보다 복잡하다

Shell job control에서는 process group과 session이 terminal ownership과 signal delivery에 사용된다. 어떤 process group이 orphaned 상태가 되면서 stopped members를 포함하면 POSIX/Linux semantics에 따라 SIGHUP과 SIGCONT가 전달되는 경우가 있다. 목적은 더 이상 자신을 continue시켜 줄 controlling parent가 없는 stopped jobs가 영원히 매달리는 것을 막는 것이다. 이것은 “부모가 죽으면 자식에게 항상 SIGHUP”이라는 단순 규칙이 아니다. Process group의 orphan 판정과 stopped state가 함께 작용한다. Daemon/service 환경에서 terminal을 detach하고 session을 새로 만드는 전통적 패턴도 이런 job-control 관계와 연결된다. **Parent-child graph, process-group graph, session graph는 서로 다른 topology**이며 종료 시 각각의 signal semantics가 다르다.

## CHAPTER 27 · ptrace/debugger가 끼면 누가 exit event를 먼저 관찰하고 누가 최종 reap하는지가 더 복잡해진다

Tracing 중인 process는 ordinary parent-child wait path 외에 tracer가 관찰하는 stop/exit events를 가진다. Debugger는 signal delivery와 exit 직전 상태를 가로채거나 관찰할 수 있어 parent supervisor가 기대한 timing과 달라질 수 있다. 특히 production crash collector나 sandbox가 ptrace를 사용하는 경우 “child가 종료했는데 parent wait가 아직 안 끝난다”는 현상을 단순 hang으로 보면 안 된다. Tracer가 어떤 event를 소비·재개해야 하는지 확인해야 한다. 반대로 tracer가 죽었을 때 tracee가 어떻게 처리되는지도 정책에 따라 중요하다. **Observation ownership이 parent와 tracer 두 축으로 분리**되므로, debugging tooling을 process supervisor와 함께 쓸 때는 누가 final reap 책임을 갖는지 명시해야 한다.

## CHAPTER 28 · Resource usage accounting은 종료 시점에 supervisor로 전달할 수 있는 중요한 postmortem metadata다

`wait4()`나 관련 interface는 child의 user/system CPU time 같은 resource usage 정보를 함께 제공할 수 있다. 종료된 process의 live metrics endpoint는 더 이상 질의할 수 없으므로, supervisor가 death event와 함께 마지막 accounting snapshot을 수집하면 crash-loop나 runaway workload 분석에 도움이 된다. 하지만 이 값은 전체 distributed request cost를 의미하지 않는다. Child가 사용한 kernel/shared resource, descendants의 비용, remote service 비용은 별도 accounting이 필요하다. Container/cgroup metrics와 process rusage를 결합하면 “개별 worker가 CPU를 태웠는지, subtree 전체가 memory pressure를 일으켰는지”를 구분할 수 있다. **Exit record는 마지막 상태를 남길 수 있는 observability boundary**이므로 exit code만 저장하고 accounting을 버리는 것은 아까운 정보 손실이다.

## CHAPTER 29 · Restart policy는 death detection보다 reap과 generation transition을 먼저 정확히 끝내야 한다

Supervisor가 child death를 감지하자마자 같은 logical service의 새 process를 시작할 수 있지만, old generation의 status/reap/metrics 정리가 끝나지 않았다면 logs와 PID attribution이 섞일 수 있다. 특히 PID reuse가 빠르거나 external registry가 PID만 key로 쓰면 old crash event가 new process에 붙는 심각한 오진이 생긴다. 안전한 restart는 `OLD_EXIT_OBSERVED → OLD_METADATA_CAPTURED → OLD_REAPED → NEW_GENERATION_CREATED` 같은 ordering을 가진다. Availability 때문에 새 generation을 먼저 띄워야 한다면 두 generations를 동시에 식별할 stable ID가 필요하다. Crash loop backoff도 exit code 하나가 아니라 signal, uptime, restart count, config/build generation을 보고 결정해야 한다. **Restart는 process를 다시 만드는 동작이 아니라 generation handoff protocol**이다.

## CHAPTER 30 · Shutdown timeout은 graceful completion과 forced cleanup 사이의 정책 경계다

Service manager는 SIGTERM 같은 graceful request를 보낸 뒤 일정 시간 동안 process가 drain하도록 기다리고, deadline을 넘기면 SIGKILL 같은 강제 수단을 사용할 수 있다. 이 timeout을 너무 짧게 잡으면 정상 cleanup을 매번 끊고, 너무 길게 잡으면 broken process가 deployment나 incident recovery를 오래 막는다. 중요한 것은 timeout 숫자보다 shutdown phases를 관측하는 것이다. `stop accepting`, `drain requests`, `flush state`, `child reap`, `exit requested`를 분리해서 어느 단계가 오래 걸리는지 봐야 한다. Child subprocess를 가진 service라면 parent가 종료하기 전에 descendants에게 termination을 전달하고 reap할 시간을 포함해야 한다. **Graceful shutdown SLO는 process exit latency가 아니라 안전하게 ownership을 반환하는 전체 protocol latency**로 정의하는 편이 맞다.

## CHAPTER 31 · Crash storm에서 zombie와 core dump pipeline은 같은 failure domain을 공유할 수 있다

많은 workers가 동시에 crash하면 core dump collector, parent wait loop, logging, restart logic이 동시에 압력을 받는다. P90에서 core collector가 느리면 dead task resource release가 지연될 수 있음을 봤고, 여기서는 parent가 SIGCHLD/wait events를 제때 drain하지 못하면 zombie record가 쌓일 수 있다. Supervisor가 crash마다 동기식 upload나 heavy symbolization을 wait loop 안에서 수행하면 reaping throughput이 떨어진다. 해결은 event capture와 expensive enrichment를 분리하는 것이다. Exit event에서 최소 metadata를 빠르게 확보하고, reap 정책을 안전하게 수행한 뒤, heavy analysis는 bounded worker queue로 넘긴다. **Death handling path 자체가 overload-safe해야 한다.** Crash storm은 application failure뿐 아니라 evidence/restart/reap subsystem의 capacity test이기도 하다.

## CHAPTER 32 · Zombie 문제를 조사할 때 kill보다 parent의 wait ownership을 추적해야 한다

Zombie troubleshooting의 첫 질문은 “왜 이 PID가 안 죽나?”가 아니라 “누가 이 child를 reap해야 하나?”다. `ps`나 `/proc`에서 state, PID, PPID를 확인하고 parent process가 살아 있는지, SIGCHLD disposition이 무엇인지, wait loop가 event loop에서 block됐는지, adopted child라면 subreaper가 누구인지 확인한다. Parent가 멈춰 있다면 stack/trace로 `wait*()` 호출이 없는 이유를 좁힌다. Container에서는 내부 PID 1이 reaper 역할을 수행하는지도 본다. Zombie에 signal을 보내는 것은 실행할 user code가 없으므로 본질적 해결이 아니다. Parent가 죽으면 reparenting으로 다른 reaper가 맡을 수 있지만 production fix는 wait ownership bug를 고치는 것이다. **Zombie는 child-side 실행 버그보다 parent-side lifecycle bookkeeping bug의 증거인 경우가 많다.**

## CHAPTER 33 · “kill -9면 즉시 사라진다”는 운영 가정은 process state와 I/O teardown을 무시한다

SIGKILL은 user handler를 건너뛰고 process termination을 강제하는 강력한 수단이지만, operator가 신호를 보낸 순간 화면에서 PID가 즉시 없어져야 한다는 뜻은 아니다. Task가 kernel에서 uninterruptible wait 상태에 있거나 failure recovery path를 통과하는 동안 관찰상 지연될 수 있고, 종료 후 zombie가 되어 parent reap을 기다릴 수도 있다. 또한 PID가 사라졌다고 외부 device/network transaction이 원상복구됐다는 뜻도 아니다. 따라서 “kill -9 후 1초 내 ps에서 사라짐”을 correctness 기준으로 잡으면 잘못된 진단을 만든다. **Signal delivery, task termination, resource teardown, reap은 다른 단계**다. 문제가 있는 process가 오래 남으면 state와 kernel wait reason을 확인해 어느 단계에서 멈췄는지 찾아야 한다.

## CHAPTER 34 · Process-exit fault injection은 정상 exit code보다 race와 adoption을 검증해야 한다

테스트에서는 child가 `return 0` 하는 경우만 확인하면 부족하다. Parent보다 먼저 child가 죽기, parent가 child보다 먼저 죽기, 여러 children 동시 exit, SIGCHLD coalescing, WNOWAIT 후 지연 reap, subreaper adoption, pidfd open race, supervisor thread 여러 개의 concurrent wait, stopped/continued child, SIGKILL, core-producing fatal signal을 조합해야 한다. Shared memory와 inherited fds를 가진 child를 종료해 object lifetime이 예상대로 남거나 사라지는지도 본다. Crash storm에서 wait loop throughput과 zombie peak도 측정한다. Invariant는 “각 child generation은 정확히 한 번 최종 처리됨”, “PID reuse가 old event와 섞이지 않음”, “terminal state 후 unreaped record가 bounded time 안에 0으로 수렴함”처럼 잡는다. **Exit path의 CLEAN은 단순 종료 성공이 아니라 lifecycle race에 대한 증명**이어야 한다.

## CHAPTER 35 · Process supervision observability는 state transition을 generation ID와 함께 기록해야 한다

좋은 supervisor telemetry는 `spawned`, `running`, `termination-requested`, `exit-observed`, `metadata-captured`, `reaped`, `restart-created` 같은 transition을 시간순으로 남긴다. 여기에 PID, pidfd-associated generation ID, parent/subreaper identity, exit status/signal, uptime, build version, cgroup/container identity를 연결한다. Raw PID만 기록하면 reuse 뒤 사건이 섞일 수 있고, exit code만 기록하면 forced kill과 crash를 구분하기 어렵다. Metrics에는 live children, zombies, reap latency, exit rate, restart rate, longest-unreaped age를 포함하면 lifecycle leak을 조기에 찾을 수 있다. Crash artifact와 log도 같은 generation key를 사용해야 한다. **Process lifecycle debugging은 개별 syscall trace보다 generation state machine의 timestamped history가 더 강력한 증거**가 된다.

## CHAPTER 36 · Process exit correctness는 “실행 중단”이 아니라 generation을 완전히 닫는 proof다

한 process lifecycle을 안전하다고 승인하려면 다음을 답할 수 있어야 한다. Thread 하나의 종료와 process-wide exit를 구분하는가. Graceful cleanup과 SIGKILL failure path의 차이를 아는가. Child death event를 누가 관찰하고 누가 reap하는가. SIGCHLD가 합쳐져도 wait records를 끝까지 drain하는가. WNOWAIT를 썼다면 최종 reaper가 존재하는가. PID reuse 대신 stable generation identity를 쓰는가. Parent death 후 subreaper/adoption 경로가 명확한가. Shared objects와 pinned/in-flight resources가 process lifetime과 다를 수 있음을 고려하는가. Restart 전에 old generation의 accounting을 닫는가. Fault injection 후 unreaped children과 duplicate handling이 0으로 수렴하는가. **Process가 CPU를 더 이상 실행하지 않는 순간은 종료의 시작일 뿐이고, resource ownership과 death record와 supervision responsibility가 모두 닫혀야 한 generation이 끝난다.**
