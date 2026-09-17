# PART 71 · File Descriptor and Open-File-Description Internals — tables, sharing, inheritance, reuse, lifetime

File descriptor는 `파일 번호`가 아니다. Process의 descriptor table에 있는 작은 integer index가 kernel의 **open file description**을 가리키고, 그 description이 file offset·status flags·underlying inode/socket/device state와 연결된다. `dup`, `fork`, `exec`, UNIX-domain FD passing, epoll, async I/O가 모두 이 reference graph를 재사용하므로 **숫자 fd와 실제 kernel object lifetime을 분리**하지 않으면 가장 위험한 race—close 뒤 번호 재사용—를 놓치게 된다.

## CHAPTER 01 · fd는 process descriptor table의 index이고 object identity가 아니다

Userspace가 보는 `3`, `7`, `42` 같은 fd 값은 process별 descriptor table의 slot 번호다. 같은 숫자 `5`라도 다른 process에서는 전혀 다른 open file description을 가리킬 수 있고, 같은 process에서도 close 뒤 재사용되면 과거의 `fd=5`와 현재의 `fd=5`가 다른 object를 뜻한다. 따라서 log·async callback·cache key에 fd 숫자만 저장하면 generation identity가 없다. 올바른 mental model은 `integer → fd table entry → open file description → underlying object`라는 reference chain이며, 각 단계의 lifetime과 flags가 다르다.

## CHAPTER 02 · open은 pathname을 열면서 새 open file description을 만든다

`open()`/`openat()` 계열이 성공하면 kernel은 descriptor slot만 반환하는 것이 아니라 새 open file description을 생성해 file status flags와 current file offset 같은 per-open state를 보관한다. 동일 pathname을 두 번 open하면 같은 inode를 가리킬 수 있어도 서로 다른 open file descriptions이므로 offset과 일부 status state가 독립적이다. 반대로 `dup()`은 새 open을 하지 않고 기존 description을 공유한다. `같은 파일`이라는 말은 inode identity인지 open-instance identity인지 반드시 구분해야 한다.

## CHAPTER 03 · fd allocation은 작은 빈 번호 재사용 때문에 ABA 문제를 만든다

Kernel은 일반적으로 사용 가능한 작은 descriptor number를 다시 쓸 수 있다. Thread A가 fd 17을 close한 직후 thread B가 새 file/socket을 열어 다시 17을 받으면, 늦게 도착한 A의 async completion이나 cleanup code가 숫자만 보고 B의 object를 조작할 수 있다. 이것은 pointer ABA와 유사한 identity-reuse 문제다. 해결은 `close를 늦추자`가 아니라 in-flight operation이 kernel reference를 독립적으로 보유하게 하거나 generation/token으로 userspace identity를 구분하고, fd 숫자를 long-lived object ID로 사용하지 않는 것이다.

## CHAPTER 04 · FD_CLOEXEC는 descriptor entry flag이고 O_NONBLOCK은 open-file-description status다

`FD_CLOEXEC` 같은 descriptor flag는 특정 fd table slot에 속하므로 duplicate descriptor마다 값이 다를 수 있다. 반면 `O_NONBLOCK`, `O_APPEND` 같은 file status flags는 open file description에 속해 `dup()`이나 `fork()`로 같은 description을 공유하는 descriptors에 영향을 줄 수 있다. `fcntl(F_SETFD)`와 `fcntl(F_SETFL)`을 혼동하면 한 descriptor만 바꿨다고 생각했는데 sibling duplicate의 I/O semantics까지 변할 수 있다. Debugging할 때 fd-level state와 OFD-level state를 별도 출력해야 한다.

## CHAPTER 05 · File offset도 open file description에 속하므로 dup/fork에서 공유된다

Regular file의 current offset은 descriptor 번호가 아니라 open file description의 state다. `dup(fd)`로 만든 두 descriptors가 같은 OFD를 가리키면 한 descriptor의 read가 offset을 전진시켜 다른 descriptor의 다음 read 위치도 바꾼다. `fork()` 뒤 parent/child도 동일 OFD를 공유하면 offset interaction이 생길 수 있다. 독립 offset이 필요하면 `pread/pwrite`처럼 explicit offset I/O를 쓰거나 file을 별도로 open해야 한다. Shared offset은 의도하면 serialization primitive가 될 수 있지만 암묵적으로 기대하면 race가 된다.

## CHAPTER 06 · dup는 새 번호를 만들지만 open instance는 새로 만들지 않는다

`dup()`는 기존 descriptor table entry가 참조하던 open file description에 새 fd slot을 추가한다. 따라서 underlying object와 file offset/status flags는 공유하지만 새 fd의 close-on-exec flag는 별도다. 이 차이는 pipe/socket endpoint에도 적용된다. `dup`를 object clone으로 이해하면 close lifetime과 epoll registration을 틀리게 해석한다. Kernel object refcount가 증가해 original fd를 닫아도 duplicate가 남으면 OFD와 underlying endpoint는 살아 있다.

## CHAPTER 07 · dup2의 close+reuse는 atomic이어야 signal/thread race를 피한다

`close(newfd); dup(oldfd)` 두 syscall로 특정 번호에 duplication을 구현하면 그 사이 다른 thread나 signal handler가 fd를 열어 같은 slot을 차지할 수 있다. `dup2/dup3`가 기존 target을 닫고 duplication을 하나의 atomic fd-table operation으로 제공하는 이유다. Shell redirection 같은 code에서 이 race는 드물지만 production에서는 signal·logging·network helper가 비동기로 descriptor를 만들 수 있다. Descriptor-number replacement는 `close 후 open`이 아니라 원자적 table mutation이 필요하다.

## CHAPTER 08 · O_CLOEXEC는 multithreaded fork+exec에서 leak window를 제거한다

`open()` 뒤 `fcntl(F_SETFD, FD_CLOEXEC)`를 따로 호출하면 두 syscall 사이 다른 thread가 `fork()+exec()`할 수 있고, 새 program이 원치 않는 fd를 상속한다. 그래서 fd를 만드는 syscall은 가능하면 creation 시점에 `O_CLOEXEC`, `SOCK_CLOEXEC`, `pipe2(O_CLOEXEC)`, `dup3(O_CLOEXEC)` 같은 atomic option을 사용해야 한다. 이것은 편의 기능이 아니라 privilege boundary다. Secret file, listening socket, namespace handle이 child exec에 새면 authority leakage가 된다.

## CHAPTER 09 · fork는 fd table entry를 복제하지만 open file descriptions은 공유한다

`fork()` 후 child는 parent descriptor table의 copy를 받지만 각 entry는 같은 kernel open file description을 참조한다. 그러므로 offset/status flags/pipe endpoint/socket state는 공유될 수 있다. Parent가 `O_NONBLOCK`을 바꾸거나 file offset을 이동하면 child에도 영향을 줄 수 있다. 반대로 `FD_CLOEXEC`는 각 process의 descriptor table entry flag로 관리된다. Fork 직후 어느 side가 어떤 descriptor를 close할지 명시하지 않으면 pipe EOF가 안 오거나 socket lifetime이 예상보다 길어진다.

## CHAPTER 10 · exec는 process image를 바꾸지만 CLOEXEC가 아닌 fd는 살아남는다

`execve()`는 code/data/stack 등 process image를 교체하지만 descriptor table 전체를 자동 비우지 않는다. Close-on-exec가 설정되지 않은 fd는 새 executable로 전달된다. 이 특성은 stdin/stdout/stderr redirection, socket activation 같은 기능을 가능하게 하지만 accidental authority inheritance도 만든다. Exec 경계에서는 `어떤 fd가 intentionally inherited되는가`를 allowlist로 관리하는 편이 안전하다. Child program이 알지 못하는 fd를 물려받으면 EOF/lifetime/security 버그가 생긴다.

## CHAPTER 11 · SCM_RIGHTS는 fd 숫자를 보내는 것이 아니라 kernel reference를 전달한다

UNIX-domain socket의 `SCM_RIGHTS` ancillary data는 sender process의 integer fd 값을 receiver에게 그대로 복사하지 않는다. Kernel은 underlying open file description에 대한 reference를 receiver의 fd table에 새 번호로 설치한다. 따라서 sender의 fd 12가 receiver에서는 7이 될 수 있지만 같은 OFD를 공유한다. File offset/status flags sharing도 이어질 수 있다. FD passing은 path 없이 capability를 전달하는 강력한 IPC이므로 peer authentication과 allowed object type 검증이 필요하다.

## CHAPTER 12 · FD passing은 권한 전달이므로 namespace보다 강한 authority channel이 될 수 있다

Receiver가 pathname으로 접근 권한이 없는 file/device라도 이미 열린 fd를 전달받으면 그 descriptor가 허용하는 operation을 수행할 수 있다. 이는 deliberate capability design에 유용하지만 sandbox escape boundary가 될 수도 있다. Server는 `peer가 이 path를 열 수 있는가`가 아니라 `이 열린 object reference를 받을 자격이 있는가`를 판단해야 한다. Credentials, object type, open mode, ioctl surface까지 authority model에 포함해야 한다.

## CHAPTER 13 · O_PATH는 data I/O 대신 pathname object reference를 유지하는 descriptor다

`O_PATH`로 연 fd는 일반 read/write data handle과 다르게 path resolution 결과에 대한 reference 역할을 할 수 있다. `fstat`, `fchdir`, `*at` 계열 dirfd, 일부 metadata operation의 anchor로 사용할 수 있고 실제 object를 다시 열기 위한 stable handle이 된다. `fd가 있으면 read 가능`이라는 가정은 깨진다. Descriptor capability를 operation class로 구분해야 하며 O_PATH는 path traversal과 data access authority를 분리하는 도구로 볼 수 있다.

## CHAPTER 14 · dirfd 기반 openat은 pathname resolution root를 명시적으로 고정한다

Global current working directory에 의존한 `chdir()+open(relative)`는 multithread process에서 race와 ambient-state coupling을 만든다. Directory fd를 anchor로 `openat`을 사용하면 path resolution base가 explicit object reference가 된다. Directory가 rename되어도 fd가 underlying directory object를 유지할 수 있어 pathname string보다 stable하다. 이 방식은 TOCTOU를 완전히 제거하지는 않지만 resolution context를 descriptor lifetime에 결합해 race surface를 크게 줄인다.

## CHAPTER 15 · openat2의 resolve policy는 pathname traversal을 security contract로 만든다

Symlink, mount crossing, `..`, magic link 같은 resolution behavior는 sandbox/importer에서 공격 surface가 된다. `openat2`의 resolve flags는 `이 dirfd 아래만`, `symlink 금지`, `mount crossing 금지` 같은 정책을 kernel pathname walker에 함께 전달할 수 있다. Userspace에서 component를 하나씩 `lstat`한 뒤 open하는 방식은 검사와 사용 사이에 namespace가 바뀌는 TOCTOU가 있다. Security-critical path resolution은 검증과 open을 가능한 한 같은 kernel operation에 묶어야 한다.

## CHAPTER 16 · Traditional process-associated lock과 OFD lock은 lifetime semantics가 다르다

POSIX record lock의 일부 semantics는 process와 file association에 묶여 있어 같은 process의 다른 fd close가 lock에 영향을 줄 수 있어 직관적이지 않다. Open-file-description lock은 특정 OFD identity에 lock ownership을 연결해 `dup/fork` sharing model과 더 직접적으로 맞는다. Lock API를 선택할 때 byte-range conflict뿐 아니라 close/dup/fork 후 ownership이 어떻게 지속되는지 확인해야 한다. Lock lifetime을 descriptor number 하나에 연결해 생각하면 오판한다.

## CHAPTER 17 · epoll의 interest key는 fd 숫자 하나가 아니라 fd와 open file description의 결합이다

Epoll은 등록 시 descriptor number와 underlying OFD identity를 사용해 interest entry를 관리한다. 같은 OFD를 가리키는 duplicate fds를 별도로 등록해 서로 다른 event masks/data를 둘 수도 있다. 따라서 `epoll에 fd 9를 넣었다`는 로그만으로 object identity가 충분하지 않다. Registration 시점의 fd number와 OFD relationship을 함께 알아야 duplicate/close behavior를 이해할 수 있다.

## CHAPTER 18 · duplicate fd 하나를 close해도 epoll interest가 바로 사라지지 않을 수 있다

Epoll이 관심을 가진 underlying OFD에 다른 duplicate fd가 여전히 열려 있으면 한 descriptor를 close해도 kernel object와 interest relationship이 지속될 수 있다. Application이 `close(fd)` 후 그 번호가 재사용된 것을 보고 stale epoll event를 새 object event로 오인하면 identity bug가 생긴다. 안전한 event loop는 per-connection generation/token을 epoll user data에 넣고 descriptor reuse와 logical connection identity를 분리한다.

## CHAPTER 19 · close는 새 userspace access를 막지만 이미 시작된 kernel operation을 즉시 취소하지 않는다

한 thread가 blocking I/O syscall 안에서 open file description reference를 이미 획득한 상태에서 다른 thread가 fd를 close할 수 있다. Descriptor slot은 비어 새 object에 재사용될 수 있지만 in-flight syscall은 old OFD reference를 들고 계속 완료될 수 있다. 그래서 close를 cancellation primitive로 일반화하면 안 된다. Async cancellation은 operation-specific mechanism과 lifetime token을 사용해야 하며 fd-number invalidation은 별도다.

## CHAPTER 20 · close 후 fd-number reuse는 async completion routing의 대표적 ABA race다

Request A가 fd 23에서 시작되고 userspace가 close한 뒤 새로운 socket B가 fd 23을 받았다고 하자. 늦게 온 A completion을 `fd=23`으로 lookup하면 B connection 상태에 적용할 수 있다. Kernel async frameworks가 file reference를 별도로 pin하더라도 userspace routing table이 숫자만 키로 쓰면 race가 남는다. Connection/request object ID와 descriptor generation을 유지하고 completion이 old generation인지 검증해야 한다.

## CHAPTER 21 · io_uring registered files는 fd lookup을 stable kernel references로 치환한다

Hot I/O마다 process fd table을 lookup하는 비용과 close/reuse race를 줄이기 위해 io_uring은 files를 registration table에 pin하고 fixed-file index로 참조할 수 있다. 이 index는 ordinary fd number와 다른 namespace이며 update/unregister lifecycle을 가진다. Registered reference가 남는 동안 original fd를 닫아도 underlying file가 살아 있을 수 있으므로 memory/resource accounting과 teardown에서 별도 reference를 추적해야 한다.

## CHAPTER 22 · pidfd도 integer fd지만 대상 process lifetime을 stable reference로 만든다

PID 숫자는 process 종료 후 재사용될 수 있어 `kill(pid)` 같은 long-lived control path에 ABA risk가 있다. pidfd는 특정 process object에 대한 fd reference를 사용해 PID reuse와 identity를 분리한다. 이것은 fd abstraction이 file만 나타내는 것이 아니라 **kernel object capability/lifetime handle**이라는 더 일반적인 모델을 보여준다. 숫자 namespace는 재사용되지만 underlying reference identity가 안전성을 제공한다.

## CHAPTER 23 · Pipe endpoint도 OFD reference count 때문에 EOF semantics가 결정된다

Pipe read가 EOF를 받으려면 모든 write-end references가 사라져야 한다. Parent가 child에게 pipe를 fork로 상속한 뒤 쓰지 않는 write end를 닫지 않으면 reader는 writer가 논리적으로 끝났어도 EOF를 기다리며 block될 수 있다. Descriptor 한 개가 아니라 같은 pipe endpoint를 가리키는 모든 process/fd references를 세야 한다. IPC shutdown bug의 상당수가 숨은 inherited duplicate 때문이다.

## CHAPTER 24 · Socket dup/fork는 하나의 protocol endpoint를 여러 descriptor owner가 공유하게 만든다

Duplicate socket fds는 같은 underlying socket state, receive/send queues, shutdown state를 공유한다. 한 descriptor에서 `shutdown()`을 호출하면 sibling descriptor에도 protocol-level effect가 나타나지만 단순 close는 remaining references 때문에 socket을 유지한다. `fd 하나당 connection 하나`라는 mapping은 dup/fork/passing 뒤 깨진다. Network layer identity와 descriptor owner를 별도 구조로 관리해야 한다.

## CHAPTER 25 · F_SETFL 변경은 shared OFD 상태라 sibling descriptor semantics를 바꿀 수 있다

`O_NONBLOCK` 등을 `fcntl(F_SETFL)`로 바꾸면 같은 open file description을 공유하는 duplicates에도 적용될 수 있다. Library A가 자신의 fd만 nonblocking으로 바꾼다고 생각했는데 Library B가 dup한 handle도 EAGAIN behavior로 바뀔 수 있다. API boundary에서 descriptor ownership이 shared일 가능성이 있으면 mutable OFD status flag를 바꾸기 전에 contract를 확인하거나 별도 open instance를 사용해야 한다.

## CHAPTER 26 · close error는 이미 descriptor number가 해제된 뒤 보고될 수 있어 retry가 위험하다

일부 filesystem/storage error가 delayed writeback 때문에 `close()` 시점에 보고될 수 있다. 그러나 close가 error를 반환했더라도 fd number는 이미 release되어 다른 thread에서 재사용됐을 수 있다. `close가 실패했으니 같은 fd를 다시 close`하면 새 object를 닫을 위험이 있다. Durability-critical code는 fsync류 explicit error boundary를 먼저 사용하고 close failure는 기록하되 descriptor-number retry를 피해야 한다.

## CHAPTER 27 · Descriptor leak은 memory leak보다 권한·EOF·resource exhaustion을 동시에 만든다

닫히지 않은 fd는 kernel object reference와 관련 buffers, mount/device lifetime을 유지한다. Listening socket이나 secret file이 exec child에 새면 security leak이고, pipe write end가 남으면 EOF semantics가 깨지며, 대량 leak은 per-process `RLIMIT_NOFILE` 또는 system-wide file table limit에 도달한다. Leak detector는 fd count뿐 아니라 object type·creation stack·CLOEXEC·age를 추적해야 root cause를 찾을 수 있다.

## CHAPTER 28 · EMFILE과 ENFILE은 서로 다른 capacity boundary다

`EMFILE`은 process가 허용된 open descriptor 수를 소진했음을, `ENFILE`은 system-wide open-file capacity가 부족함을 나타낼 수 있다. 해결책도 다르다. Process leak/limit 문제와 host-wide file table pressure를 같은 `fd 부족`으로 처리하면 잘못된 auto-recovery를 만든다. Admission control은 reserve descriptors와 emergency logging path까지 고려해야 한다.

## CHAPTER 29 · /proc fd/fdinfo와 kcmp류 도구는 숫자와 shared OFD를 구분하는 관측 증거다

`/proc/<pid>/fd`는 descriptor가 어떤 object/path를 참조하는지 보여주고 fdinfo는 position, flags, mount ID 등 OFD 관련 정보를 제공한다. 두 descriptors가 같은 open file description인지 확인하려면 단순 symlink path 비교보다 kernel object relationship을 보는 도구가 필요하다. Incident 분석에서는 `같은 path`와 `same OFD`를 구분해 offset/epoll/lock sharing 원인을 확인해야 한다.

## CHAPTER 30 · Descriptor correctness는 숫자가 아니라 reference graph의 lifetime proof다

안전한 descriptor 설계는 **fd-table slot, per-fd flags, open-file-description offset/status, underlying inode/socket/device, duplicate/fork/exec inheritance, SCM_RIGHTS capability transfer, epoll interest, async operation references, close/reuse generation, resource limits**를 하나의 graph로 본다. Integer fd는 잠깐 존재하는 lookup key일 뿐 long-lived identity가 아니다. 모든 async·IPC·teardown path에서 어떤 kernel reference가 아직 살아 있는지를 증명해야 use-after-close와 descriptor-reuse race를 막을 수 있다.
