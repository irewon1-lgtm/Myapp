# PART 93 · File Locking and Lease Semantics — POSIX ranges, OFD locks, flock, lease breaks, pathname races, remote loss

파일 잠금은 “이 파일은 내 것”이라는 단일한 기능이 아니다. Linux에는 traditional POSIX process-associated byte-range lock, open-file-description(OFD) lock, BSD-style `flock()`, file lease처럼 ownership 단위와 충돌 규칙이 다른 mechanism이 공존한다. Local filesystem에서는 서로 독립인 lock 종류가 NFS나 SMB에서는 protocol emulation 때문에 상호작용할 수도 있고, pathname이 rename되어도 이미 열린 file object의 lock lifetime은 따로 간다. 더 위험한 것은 advisory lock을 걸었다고 협조하지 않는 writer까지 자동 차단된다고 믿거나, fd 하나를 닫았을 뿐인데 같은 process가 그 file에 잡아둔 traditional record locks가 풀리는 semantics를 놓치는 것이다. P93은 lock API 이름을 외우는 대신 **무엇을 잠갔는가, 누가 소유하는가, 어떤 reference가 lifetime을 유지하는가, remote failure 뒤에도 lock이 유효한가**를 하나의 concurrency contract로 정리한다.

## CHAPTER 01 · File lock은 데이터 자체를 얼리는 기능이 아니라 경쟁자 사이의 protocol이다

Advisory locking의 가장 중요한 전제는 참여자가 모두 같은 protocol을 지킨다는 것이다. Process A가 byte-range write lock을 잡았다고 해서 권한이 있는 process B의 ordinary `write()`가 local filesystem에서 자동으로 금지되는 것은 아니다. B가 lock을 조회하거나 같은 locking mechanism으로 충돌을 확인하지 않으면 data를 바꿀 수 있다. 그래서 advisory lock은 memory protection이나 access-control mechanism이 아니다. 이것은 cooperating applications가 “이 range를 수정하기 전에는 먼저 lock을 얻는다”는 규칙을 공유하도록 만드는 synchronization channel이다. Security boundary가 필요하다면 file permissions, capabilities, sandbox policy가 별도로 필요하다. Correctness review에서는 **lock acquisition code뿐 아니라 모든 mutation path가 동일 protocol에 참여하는지**를 찾아야 한다. Backup tool, maintenance script, mmap writer, 다른 language runtime 하나만 protocol 밖에 있어도 lock 기반 invariant는 깨질 수 있다.

## CHAPTER 02 · Traditional POSIX record lock은 전체 파일이 아니라 byte range를 잠그는 interval protocol이다

`F_SETLK`, `F_SETLKW`, `F_GETLK`는 `struct flock`의 type, start, length, whence를 이용해 file의 특정 byte interval에 read/write lock을 설정하거나 충돌을 조회한다. `l_len == 0` 같은 표현은 시작점부터 EOF 방향으로 이어지는 range를 나타낼 수 있어 file growth까지 포함하는 의미가 된다. 이 모델은 database-style fixed record나 append region을 서로 독립적으로 보호할 때 유용하지만, application logical record가 variable length이거나 compaction으로 offset이 바뀌면 byte range와 domain object가 쉽게 어긋난다. Lock table이 보호하는 것은 “사용자 42번 레코드”가 아니라 **그 순간 지정한 file offsets**다. 따라서 on-disk layout이 이동하는 시스템은 logical metadata와 lock range mapping을 transaction처럼 관리해야 한다. Range 계산 overflow나 off-by-one은 경쟁 제어를 조용히 무력화한다.

## CHAPTER 03 · Read lock과 write lock의 충돌은 memory rwlock과 비슷해 보여도 ownership과 range가 다르다

Record locking에서 read lock들은 겹치는 range에 공존할 수 있지만 write lock은 겹치는 read/write lock과 충돌한다. 다만 memory `pthread_rwlock`과 달리 대상은 kernel file object의 byte interval이고, process-associated lock은 thread가 아니라 process 단위 ownership을 가진다. 같은 process의 두 threads가 각자 “서로 다른 owner”라고 생각하고 traditional fcntl locks로 상호배제를 시도하면 기대와 다른 결과가 나온다. Kernel은 process 내부 thread synchronization을 위해 traditional record lock을 제공하는 것이 아니다. Thread 간 file-range coordination이 필요하면 userspace mutex를 함께 쓰거나 각 thread가 독립 open-file description을 갖고 OFD lock을 사용하는 등의 설계가 필요하다. **Shared/read와 exclusive/write라는 이름만 보고 memory lock semantics를 그대로 대입하면 안 된다.**

## CHAPTER 04 · F_SETLK와 F_SETLKW의 차이는 conflict 처리 정책을 caller에 드러낸다

`F_SETLK`는 충돌하는 lock 때문에 즉시 획득할 수 없으면 error로 돌아오는 nonblocking 형태이고, `F_SETLKW`는 lock이 가능해질 때까지 기다리는 blocking 형태다. Blocking이 편하다고 무조건 `F_SETLKW`를 쓰면 service thread가 예상치 못한 시간 동안 멈춰 queue와 shutdown을 막을 수 있다. 반대로 nonblocking loop가 sleep/backoff 없이 반복되면 CPU를 태우는 busy wait가 된다. Production code는 lock wait를 request deadline과 연결하고, cancellation/signal interruption 뒤 retry policy를 명시해야 한다. Lock acquisition이 실패했을 때 operation이 아직 아무 state도 바꾸지 않았는지, 일부 metadata를 먼저 수정했는지도 중요하다. **Lock wait는 단순 syscall latency가 아니라 resource admission queue**이므로 wait time과 holder identity를 관측해야 contention을 진단할 수 있다.

## CHAPTER 05 · F_GETLK는 “잠겨 있나?”라는 boolean보다 첫 conflict의 구조를 돌려주는 진단 도구다

`F_GETLK`는 요청하려는 range와 충돌하는 lock을 조회해 type과 range, traditional process lock의 경우 owner PID 정보를 제공할 수 있다. 하지만 이 결과를 받은 직후 상태가 그대로 유지된다는 보장은 없다. 조회와 실제 lock 획득 사이에 다른 process가 들어올 수 있으므로 `F_GETLK`를 authorization/check-then-act barrier처럼 쓰면 TOCTOU race가 생긴다. 용도는 “지금 관찰한 conflict를 설명”하거나 debugging metadata를 얻는 데 가깝고, 실제 correctness boundary는 `F_SETLK/W`의 atomic conflict check/acquire에 둬야 한다. OFD lock에서는 ownership이 단일 PID에 귀속되지 않아 PID field semantics도 다르다. **Inspection result와 ownership acquisition을 분리**하면 “조회 때 비어 있었는데 왜 acquire가 실패했지?” 같은 정상 race를 버그로 오해하지 않는다.

## CHAPTER 06 · Traditional record lock의 ownership은 fd가 아니라 process와 file의 조합에 묶인다

가장 위험한 함정 중 하나는 “이 fd로 잡았으니 이 fd를 닫을 때만 lock이 풀린다”는 가정이다. Linux `close(2)` 문서는 process가 file에 보유한 process-associated record locks가, 그 lock을 만들 때 사용한 fd와 무관하게 해당 file에 연결된 descriptor를 닫는 사건으로 제거될 수 있음을 경고한다. Library 내부가 같은 file을 잠깐 열었다 닫는 것만으로 application의 traditional locks가 풀릴 수 있다는 뜻이다. Large program에서 이 semantics는 ownership reasoning을 매우 어렵게 만든다. Descriptor alias가 많거나 third-party library가 같은 file을 여는 경우 특히 위험하다. 이 문제 때문에 **open-file-description lock이 더 자연스러운 lifetime을 제공하는 경우가 많다.** Traditional lock을 써야 한다면 같은 process에서 해당 file의 모든 open/close path를 audit해야 한다.

## CHAPTER 07 · fork는 process-associated record lock을 상속하지 않지만 shared file object는 그대로 상속할 수 있다

`fork()` 뒤 child는 parent의 address space와 fd table을 복제된 형태로 이어받지만, traditional process-associated record locks 자체는 child에게 상속되지 않는다. 반면 OFD locks와 `flock()` locks는 open file description과 연결되므로 inherited descriptor가 같은 description을 가리키는 동안 child에서도 그 lock lifetime에 참여한다. 이 차이는 pre-fork server에서 매우 중요하다. Parent가 traditional lock을 잡고 fork한 뒤 child가 lock도 상속했다고 믿으면 보호 없이 file을 수정할 수 있다. 반대로 OFD/flock lock은 child가 inherited fd를 계속 갖는 바람에 parent가 descriptor를 닫아도 lock이 예상보다 오래 유지될 수 있다. **fork boundary에서는 fd inheritance와 lock inheritance를 별도 표로 그려야 한다.**

## CHAPTER 08 · exec는 “process image 교체”이지 open descriptor와 lock lifetime의 자동 초기화가 아니다

`exec`는 executable image를 바꾸지만 close-on-exec가 설정되지 않은 file descriptors는 새 program으로 넘어갈 수 있다. 그 descriptor가 OFD/flock lock lifetime을 유지한다면 완전히 다른 executable이 의도치 않게 lock holder가 될 수 있다. Traditional record lock도 process lifetime과 file association에 따라 exec를 가로질러 유지될 수 있으므로 “새 프로그램이 시작됐으니 잠금이 리셋됐다”는 가정은 위험하다. Helper를 `exec`할 때 lock을 넘길 의도가 없다면 fd를 `O_CLOEXEC`/`FD_CLOEXEC`로 만들고 inheritance policy를 명시해야 한다. 반대로 lock handoff를 의도한다면 어떤 executable generation이 같은 ownership을 이어받는지 기록해야 한다. **Exec boundary의 핵심은 code identity는 바뀌어도 kernel object references는 선택적으로 살아남는다는 것**이다.

## CHAPTER 09 · OFD lock은 process 대신 open file description을 owner로 삼아 close와 thread semantics를 바꾼다

OFD lock은 `F_OFD_SETLK`, `F_OFD_SETLKW`, `F_OFD_GETLK`로 다루며 ownership이 특정 PID가 아니라 open file description에 붙는다. `dup()`이나 `fork()`로 같은 open file description을 공유하는 descriptors는 같은 lock ownership을 공유할 수 있고, 마지막 reference가 닫힐 때 lock lifetime이 끝나는 방향으로 이해할 수 있다. 이것은 traditional record lock의 “같은 file의 다른 fd close가 lock을 예상 밖으로 제거”하는 문제를 피하는 데 도움이 된다. 또한 threads가 각자 `open()`으로 서로 다른 open file descriptions를 만들면 OFD locks를 통해 서로 충돌하도록 구성할 수 있다. **OFD lock의 핵심은 fd number가 아니라 fd 뒤의 open-file-description identity**다. P71의 fd/reference graph를 그대로 lock ownership에 적용해야 한다.

## CHAPTER 10 · 같은 process라도 별도 open()으로 만든 OFD들은 서로 다른 owner이므로 충돌할 수 있다

OFD ownership이 process-wide가 아니라는 점은 장점이면서 함정이다. 같은 process에서 같은 pathname을 두 번 `open()`하면 두 descriptors가 서로 다른 open file descriptions를 가리킬 수 있고, 각각의 OFD lock은 서로 별도 owner로 취급된다. 따라서 thread A와 B가 각각 독립 open을 하고 같은 range에 exclusive OFD lock을 요청하면 실제 conflict가 발생할 수 있다. 반대로 `dup()`한 descriptor 둘은 같은 open file description을 공유하므로 별도 owner라고 생각하면 안 된다. Lock design document에는 raw fd 숫자가 아니라 **which open created this description, which dup/fork aliases share it**를 기록해야 한다. 이 구분이 없으면 self-deadlock처럼 보이는 대기와 의도치 않은 shared ownership을 구분하기 어렵다.

## CHAPTER 11 · OFD GETLK에서 PID가 핵심 owner 정보가 될 수 없는 이유는 ownership 모델 자체가 다르기 때문이다

Traditional lock은 process-associated이므로 conflict report에 PID가 의미 있는 owner hint가 될 수 있다. OFD lock은 여러 processes가 fork/dup을 통해 같은 open file description을 공유할 수 있어 단일 PID가 owner를 대표하지 못한다. Linux interface가 OFD operation에서 `l_pid`를 zero로 요구하고 `/proc/locks`가 OFD lock owner PID를 -1처럼 표시하는 이유가 이 ownership 차이를 반영한다. Monitoring system이 모든 lock에 PID가 있어야 한다고 가정하면 OFD locks를 “unknown owner”로 잘못 분류한다. 관측 모델도 mechanism에 맞춰 **lock type + file identity + range + description/reference lifetime**을 중심으로 바꿔야 한다. PID는 traditional lock에서만 ownership의 일부 단서다.

## CHAPTER 12 · flock()은 whole-file style API지만 Linux에서는 open file description lifetime을 따른다

`flock(fd, LOCK_SH/LOCK_EX/LOCK_UN)`은 사용법이 단순해 lockfile, single-writer tool, whole-file coordination에 많이 쓰인다. Linux의 native `flock()` lock은 open file description과 연결되므로 `dup()`이나 `fork()`로 같은 description을 공유하는 descriptors가 같은 lock을 가리킨다. Lock은 명시적으로 unlock하거나 그 open file description을 가리키는 descriptors가 모두 닫힐 때 release된다. 같은 process가 같은 file을 별도로 여러 번 open하면 각각은 독립적인 flock owner처럼 동작해 서로 충돌할 수도 있다. `flock()`이 whole-file이라는 사실이 ownership을 process-wide로 만든다는 뜻은 아니다. **Range가 단순할 뿐 lifetime은 reference graph 문제**다. Background child가 inherited fd를 잡고 있어 lock이 안 풀리는 사고가 흔한 이유다.

## CHAPTER 13 · flock lock conversion은 항상 원자적인 upgrade/downgrade라고 가정할 수 없다

Shared `LOCK_SH`를 exclusive `LOCK_EX`로 바꾸는 conversion을 하나의 atomic “upgrade”라고 생각하면 경쟁 상황에서 놀랄 수 있다. Linux man-page가 설명하듯 conversion 과정에서 기존 lock이 제거된 뒤 새 lock을 얻는 사이 다른 waiter가 lock을 차지할 수 있는 semantics가 존재한다. Nonblocking conversion이면 실패할 수도 있고 blocking이면 예상치 못한 대기가 생긴다. 따라서 “read phase 동안 shared lock을 유지한 채 writer로 승격”이 반드시 필요하다면 flock conversion에 그 invariant를 맡기기 전에 정확한 platform behavior를 검토해야 한다. Application-level state machine이나 별도 serialization mutex가 필요할 수 있다. **Lock mode change도 acquire와 동일하게 race가 있는 state transition**으로 취급해야 한다.

## CHAPTER 14 · Local Linux에서 flock과 fcntl record locks는 보통 독립이지만 portability가 그 가정을 깨뜨린다

Linux local filesystem semantics에서는 native `flock()`과 `fcntl()` record locks가 서로 독립적으로 동작하는 것이 기본이다. Process A가 flock exclusive를 잡았다고 process B의 fcntl record lock이 반드시 conflict하는 것은 아니다. 하지만 다른 Unix 계열이나 remote filesystem emulation에서는 두 mechanism이 상호작용할 수 있다. 같은 application suite에서 한 component는 flock, 다른 component는 fcntl을 쓰면서 “어쨌든 file lock이니까 서로 막히겠지”라고 생각하는 것은 위험하다. **Coordination domain은 mechanism까지 통일**해야 한다. Cross-platform software라면 Linux local, NFS, SMB, BSD/macOS 등 실제 deployment target별 semantics를 contract test로 확인해야 한다.

## CHAPTER 15 · NFS에서는 flock이 fcntl byte-range lock으로 emulation되어 local과 다른 상호작용이 생길 수 있다

Linux NFS client는 modern kernels에서 `flock()`을 entire-file byte-range lock으로 emulation할 수 있어 NFS 위에서는 flock과 fcntl locks가 서로 interaction할 수 있다. Exclusive flock을 잡기 위해 file을 write-open해야 하는 제약도 local usage와 다르게 드러날 수 있다. Mount option으로 locking을 local-only처럼 취급하는 mode도 있어 같은 source code가 mount configuration에 따라 분산 coordination 여부를 달리할 수 있다. Network partition이나 server restart는 lock loss라는 별도 failure를 만든다. 따라서 NFS에서 file lock을 distributed mutex로 사용할 때는 **protocol version, server support, mount options, reconnect/lost-lock handling**까지 system contract에 포함해야 한다. Local laptop test만 통과했다고 production NFS correctness가 증명되지 않는다.

## CHAPTER 16 · SMB/CIFS에서는 flock emulation이 advisory 가정을 넘어 I/O 자체를 막는 효과를 만들 수 있다

Modern Linux CIFS/SMB 환경에서는 `flock()`이 SMB byte-range locks로 emulation되며 fcntl locks와 상호작용할 수 있다. 더 중요한 차이는 SMB protocol의 lock 특성 때문에 별도 file descriptor에서 수행한 I/O가 `EACCES`로 실패하는 등 local advisory lock보다 mandatory에 가까운 효과가 나타날 수 있다는 점이다. 정확한 behavior는 SMB protocol version, server, mount options에 따라 달라질 수 있다. 같은 binary가 ext4에서는 “협조하는 process만 막는 advisory lock”으로 동작하다가 network share에서는 직접 I/O failure를 만들 수 있다는 뜻이다. **Filesystem type을 lock implementation detail로 치부하면 안 된다.** Error handling과 timeout policy는 deployment storage backend를 포함해 검증해야 한다.

## CHAPTER 17 · Traditional Linux mandatory locking을 새 설계의 기본으로 삼으면 안 된다

과거 Linux에는 file mode bits와 mount option을 결합해 advisory record lock을 mandatory하게 적용하는 mechanism이 있었지만, 이 기능은 portability와 correctness 문제가 많았고 현대 kernel에서 legacy/deprecated 또는 제거된 영역으로 취급된다. Kernel filesystem API에서도 과거 mandatory flock 지원이 제거되었음을 확인할 수 있다. 따라서 “advisory가 귀찮으니 kernel이 모든 read/write를 강제로 막게 하자”는 방향은 현대 application coordination의 기본 해법이 아니다. 권한 통제는 security mechanism으로, 동시성은 cooperating lock/transaction protocol로 분리하는 편이 명확하다. **낡은 mandatory-lock recipe를 검색해서 새 시스템에 적용하지 않는 것**도 source freshness가 필요한 이유다.

## CHAPTER 18 · File lease는 mutex가 아니라 “다른 process가 conflicting open/truncate를 하려 한다”는 break protocol이다

`F_SETLEASE`는 open file description에 read 또는 write lease를 설정해 다른 process가 conflicting `open()`이나 `truncate()`를 시도할 때 holder에게 notification을 주는 Linux mechanism이다. Read lease는 write-open/truncate와 충돌하고, write lease는 read/write open과 truncate에 더 넓게 충돌한다. Lease holder는 signal을 받은 뒤 cached state를 flush하거나 준비 작업을 하고 lease를 downgrade/release해야 한다. 이것은 ordinary mutex처럼 “lease가 있는 동안 영원히 상대를 막는다”는 기능이 아니다. Kernel은 break protocol과 timeout을 통해 conflicting operation이 결국 진행할 수 있도록 한다. **Lease의 목적은 exclusive ownership 선언보다 cache/coherency handoff 시간을 얻는 것**에 가깝다.

## CHAPTER 19 · Lease break notification은 signal delivery와 state transition을 함께 다뤄야 한다

Conflicting opener/truncater가 나타나면 kernel은 기본적으로 SIGIO 계열 notification을 lease holder에게 전달하고 상대 operation을 잠시 block할 수 있다. `F_SETSIG`를 통해 다른 signal을 선택하고 `SA_SIGINFO`로 더 구조화된 정보를 받을 수도 있다. Signal handler 안에서 heavy I/O나 복잡한 lock을 수행하면 async-signal-safety 문제를 만들 수 있으므로 handler는 wakeup만 기록하고 main loop가 실제 lease downgrade/release를 수행하는 구조가 안전할 수 있다. Notification을 놓치거나 event loop가 stall하면 kernel의 lease-break deadline이 만료되어 강제 downgrade/release가 일어날 수 있다. 따라서 **signal 수신 자체가 coherence 완료가 아니며, break requested → cleanup → lease changed**라는 state machine이 필요하다.

## CHAPTER 20 · lease-break-time은 holder에게 무한 독점권이 없음을 명시하는 bounded coordination 정책이다

Lease holder가 break request에 반응하지 않으면 `/proc/sys/fs/lease-break-time`에 의해 제한되는 시간 뒤 kernel이 lease를 강제로 downgrade하거나 제거할 수 있다. 이것은 holder crash나 scheduler stall 때문에 다른 opener가 영원히 막히지 않도록 하는 liveness mechanism이다. 반대로 holder가 “lease를 잡았으니 이 파일은 내가 원하는 시점까지 절대 안 바뀐다”고 가정하면 deadline 이후 invariant가 깨질 수 있다. Lease는 timeout이 있는 협상권이지 영구 fencing token이 아니다. Distributed or cache-coherent design에서 hard ownership이 필요하다면 generation/version check나 transaction protocol을 함께 써야 한다. **Liveness를 위해 강제 break가 존재한다는 사실을 correctness 모델에 포함**해야 한다.

## CHAPTER 21 · Nonblocking open도 lease conflict를 무시하는 것이 아니라 다른 failure 형태를 만든다

Lease와 충돌하는 `open()`이 `O_NONBLOCK`을 사용하면 무조건 기다리지 않고 `EWOULDBLOCK` 계열로 실패할 수 있지만, lease-break notification 절차 자체는 시작될 수 있다. 즉 breaker가 즉시 돌아갔다고 holder 입장에서 아무 사건도 없었던 것이 아니다. Holder는 signal을 받고 lease 상태가 transition 중임을 관찰할 수 있다. Retry하는 opener는 backoff와 deadline을 가져야 하고, holder는 stale break request를 현재 generation과 연결해야 한다. **Nonblocking은 coordination을 제거하지 않고 wait policy만 바꾼다.** Error code를 단순 “file busy”로 버리면 실제로 lease-break pressure가 발생하고 있다는 운영 신호를 잃는다.

## CHAPTER 22 · Rename은 pathname을 바꾸지만 이미 열린 file object와 그 lock reference를 다른 object로 바꾸지 않는다

Kernel lock은 문자열 pathname에 걸리는 자물쇠가 아니다. Process가 `open()`으로 file object를 얻은 뒤 pathname이 rename되어도 그 open reference는 같은 underlying file identity를 계속 가리킬 수 있다. 따라서 `/path/config`라는 이름을 lock했다고 생각하면 배포가 rename으로 새 inode를 같은 pathname에 놓았을 때 old fd의 lock과 new pathname의 file이 서로 다른 objects가 될 수 있다. Lockfile을 실제 data pathname과 동일시하면 generation switch에서 보호 범위가 갈라진다. Immutable generation + atomic rename pattern을 쓰는 시스템은 **어느 inode/generation을 lock하는지**를 명확히 해야 한다. Pathname coordination이 필요하면 별도 stable lock object를 두는 전략도 있지만, stale/lifetime 문제를 별도로 해결해야 한다.

## CHAPTER 23 · unlink된 file도 open references가 남으면 살아 있으므로 lock lifetime과 pathname lifetime이 다르다

Unix filesystem에서 file pathname entry가 unlink되어도 open descriptors가 남아 있으면 underlying file object와 data는 계속 존재할 수 있다. 그 open file description에 연결된 OFD/flock locks 역시 reference lifetime을 따라갈 수 있다. 그래서 lock holder가 보던 pathname이 사라졌다고 lock이 자동으로 의미 없어진다고 단정할 수 없다. 반대로 다른 process가 같은 pathname으로 새 file을 만들면 그것은 다른 inode/generation이라 기존 lock이 보호하지 않을 수 있다. Temp-file/lockfile design에서는 **name existence, file object existence, lock ownership을 세 축으로 분리**해야 한다. `rm lockfile`을 “unlock”으로 사용하는 ad-hoc protocol이 위험한 이유가 여기에 있다.

## CHAPTER 24 · PID를 적은 lockfile은 kernel lock과 다른 실패 모델을 가지며 stale detection이 핵심 문제가 된다

많은 legacy application은 `app.lock` file을 만들고 PID를 기록해 mutual exclusion처럼 사용한다. 이 방식은 atomic create를 잘 사용하면 admission에는 도움을 줄 수 있지만 process crash 뒤 stale file이 남고, PID가 재사용되면 살아 있는 전혀 다른 process를 owner로 오인할 수 있다. Pathname 삭제 권한이 있는 다른 process가 lockfile을 지울 수도 있다. Kernel advisory lock은 descriptor/object lifetime과 process/open-description cleanup semantics를 활용할 수 있지만, lockfile protocol은 application이 crash recovery와 generation identity를 직접 구현해야 한다. Lockfile이 필요한 경우 PID만 쓰지 말고 boot/session/generation metadata와 owner liveness validation을 설계해야 한다. **“파일이 존재함”은 ownership proof가 아니다.**

## CHAPTER 25 · flock command wrapper도 child/background fd inheritance 때문에 lock lifetime이 예상보다 길어질 수 있다

Shell의 `flock` utility는 script 전체를 쉽게 잠그는 데 유용하지만, 실행한 command가 fork해 background process를 만들고 lock fd를 상속하면 foreground command가 끝난 뒤에도 open file description reference가 남아 lock이 유지될 수 있다. Utility options가 별도 fd close behavior를 제공하는 이유도 이런 lifetime 때문이다. Script author가 “명령이 끝났으니 lock도 풀렸다”고만 생각하면 다음 job이 오래 block될 수 있다. Production script는 lock acquisition과 command execution뿐 아니라 descendants가 fd를 상속하는지 확인해야 한다. **OFD/flock의 장점인 reference-based lifetime이 subprocess tree에서는 의도치 않은 retention으로 바뀔 수 있다.**

## CHAPTER 26 · mmap writer는 advisory file lock을 자동으로 존중하지 않으므로 동일 protocol에 명시적으로 참여해야 한다

Process가 file을 memory-map하고 store instruction으로 dirty page를 만든다고 해서 다른 process의 advisory fcntl/flock lock이 CPU memory access를 자동 차단하는 것은 아니다. Advisory lock은 cooperating application protocol이므로 mmap path도 수정 전에 같은 coordination contract를 수행해야 한다. 더 복잡한 점은 mapping을 얻은 뒤 lock을 release하고 계속 memory를 수정하는 코드가 있을 수 있다는 것이다. Lock을 “open 전용 gate”로 쓰고 실제 write lifetime을 보호하지 않으면 data race가 남는다. Page cache/writeback이 뒤늦게 발생하는 것과 logical mutation ownership도 분리해야 한다. **I/O syscall 경로와 mmap store 경로를 모두 mutation inventory에 포함**해야 file lock correctness를 증명할 수 있다.

## CHAPTER 27 · sendfile/splice zero-copy path도 advisory lock을 우회하는 특별한 보호를 제공하지 않는다

P91의 zero-copy 전송은 bytes를 효율적으로 이동하는 mechanism이지 source file의 concurrent mutation을 막는 lock이 아니다. Application이 source generation을 고정하기 위해 advisory lock을 선택했다면 writer와 reader 모두 그 protocol을 따라야 한다. Reader가 lock 없이 `sendfile()`을 호출한다고 kernel이 다른 process의 advisory write lock을 보고 자동으로 기다려주는 일반 규칙은 없다. 반대로 long transfer 전체에 read lock을 잡으면 writer starvation과 deploy latency를 늘릴 수 있다. Immutable inode generation을 사용하면 긴 read lock 없이도 consistency를 얻을 수 있다. **Lock을 넣기 전에 더 좋은 ownership/version architecture로 경쟁 자체를 없앨 수 있는지** 검토해야 한다.

## CHAPTER 28 · Byte-range lock splitting과 merging은 application이 “lock object 하나”로 생각한 상태와 다를 수 있다

같은 owner가 겹치거나 인접한 range에 locks를 설정·해제하면 kernel이 효과적인 locked intervals를 merge하거나 split해 관리할 수 있다. 중간 subrange를 unlock하면 하나의 큰 logical lock이 두 intervals로 나뉜 것처럼 보일 수 있다. 따라서 “lock handle 객체 하나를 destroy하면 처음 range 전체가 풀린다”는 wrapper를 만들 때 실제 현재 range state와 API 호출 history가 일치하는지 조심해야 한다. File size가 변하고 EOF-relative range를 썼다면 해석도 복잡해진다. Debugging에서는 acquisition call 수보다 `/proc/locks`와 requested intervals를 비교해 **현재 effective lock set**을 봐야 한다. Range algebra를 명시적으로 다루지 않으면 partial unlock이 보호 구멍을 만든다.

## CHAPTER 29 · Deadlock detection은 mechanism마다 다르고 모든 cycle을 kernel이 해결해주지는 않는다

Blocking POSIX record locks는 일부 deadlock 상황에서 kernel이 `EDEADLK`를 반환할 수 있지만, 이를 application 전체 deadlock detector로 오해하면 안 된다. `flock()`은 Linux에서 deadlock detection을 제공하지 않는다고 문서화되어 있고, 여러 lock 종류·userspace mutex·network locks가 섞인 cycle은 kernel이 하나의 wait graph로 볼 수 없다. 두 files를 잠그는 순서가 서로 다른 workers는 여전히 영구 대기를 만들 수 있다. 안전한 설계는 global lock ordering, bounded wait timeout, cancellation, diagnostics를 가진다. **Deadlock prevention은 error code에 기대는 기능이 아니라 resource acquisition order의 설계 문제**다. Timeout이 났을 때 어느 locks를 이미 갖고 있는지도 정확히 rollback해야 한다.

## CHAPTER 30 · Network filesystem lock은 “획득 성공” 뒤에도 lost-lock 상태가 생길 수 있다

Local kernel memory에만 있는 mutex와 달리 NFS 같은 networked locking은 server state와 connectivity에 의존한다. Server administrative action이나 오래 지속되는 partition 뒤 server가 client를 dead로 판단하면 advisory lock이 lost될 수 있다. Linux man-page는 lost lock이 이후 I/O에서 error로 드러날 수 있음을 경고한다. 따라서 distributed correctness를 “처음 F_SETLKW가 성공했으니 작업 끝날 때까지 ownership 유지”에만 맡기면 안 된다. Storage protocol이 제공하는 recovery semantics와 application fencing/version을 확인해야 한다. 특히 외부 side effect를 수행하는 leader election 용도로 file lock을 쓴다면 **lock loss 뒤 old owner가 계속 작업하는 split-brain**을 막을 fencing token이 필요할 수 있다.

## CHAPTER 31 · /proc/locks는 lock 종류와 range를 볼 수 있지만 complete distributed truth는 아니다

Linux `/proc/locks`는 현재 kernel이 알고 있는 POSIX, OFD, flock locks와 leases를 관찰하는 데 유용하다. Type, advisory 여부, read/write, owner PID 또는 OFD의 -1, device/inode identity, range 등을 확인해 어떤 waiter가 왜 막히는지 좁힐 수 있다. 하지만 namespace/remote server state와 application logical ownership을 모두 설명하는 database는 아니다. Network filesystem에서 server-side recovery가 진행 중이거나 pathname이 rename된 경우 inode 정보를 path와 다시 연결해야 한다. Container에서는 PID namespace translation도 고려해야 한다. **`/proc/locks`는 kernel lock table의 증거이지 business transaction 상태의 증거가 아니다.** Logs에 inode/generation/range와 request identity를 함께 남기면 correlation이 쉬워진다.

## CHAPTER 32 · Lock wait observability는 holder보다 “왜 이 range를 얼마나 오래 기다리는가”를 기록해야 한다

Contention incident에서 lock count만 보면 원인이 드러나지 않는다. Acquisition start/end time, requested range, read/write mode, mechanism(POSIX/OFD/flock/lease), file generation, timeout, holder hint를 기록해야 한다. Long wait가 hot range 집중 때문인지, inherited fd가 lock을 놓지 않아서인지, remote NFS lock recovery 때문인지 구분할 수 있다. High-cardinality pathname 전체를 metrics label로 쓰면 observability system을 공격할 수 있으므로 aggregate metrics와 sampled trace를 조합한다. 중요한 지표는 wait latency distribution, timeout count, longest-held age, lock-loss events, lease-break latency다. **Lock은 invisible serialization queue**이므로 queueing time을 보지 않으면 파일 I/O latency로만 잘못 분류된다.

## CHAPTER 33 · Lock ordering은 여러 files/ranges를 동시에 다룰 때 application-level invariant가 된다

한 operation이 index file과 data file 두 개를 모두 수정하거나 여러 ranges를 잠가야 하면 각 worker가 acquisition order를 다르게 선택하는 순간 cycle 가능성이 생긴다. File A→B와 B→A가 공존하면 두 workers가 하나씩 잡고 서로를 기다릴 수 있다. Range locks는 더 복잡해 dynamic offset order가 생기므로 canonical ordering key를 정의해야 한다. 예를 들어 `(device,inode,start)` 순서처럼 모든 participants가 동일 기준을 사용하도록 한다. Timeout 후 partial acquisition을 release하고 retry할 때도 동일 order를 유지해야 livelock을 줄일 수 있다. **여러 lock을 하나의 transaction처럼 얻는 atomic syscall이 없는 경우, ordering protocol이 사실상 transaction coordinator 역할**을 한다.

## CHAPTER 34 · File-lock fault injection은 close/fork/exec/rename/network failure를 실제로 흔들어야 한다

Happy-path에서 process A가 lock을 얻고 B가 block되는지만 보면 ownership bugs를 찾지 못한다. Traditional lock을 잡은 뒤 같은 process의 다른 fd를 close해 예상대로 lock이 사라지는지, OFD/flock fd를 dup/fork해 last-close lifetime이 맞는지, exec에서 CLOEXEC 유무가 lock retention을 바꾸는지 테스트해야 한다. Lock을 잡은 inode를 rename/unlink하고 같은 pathname에 새 inode를 만든 뒤 protection scope도 확인한다. NFS/SMB test environment가 있다면 disconnect/server restart/mount option 차이를 넣고 lost-lock behavior를 본다. Lease는 breaker open/truncate, notification 지연, deadline expiry를 주입한다. **각 mechanism의 failure mode를 서로 다른 CLEAN으로 실제 검증**해야 한다.

## CHAPTER 35 · Mechanism 선택은 “가장 강한 lock”이 아니라 ownership unit과 failure domain에 맞춰야 한다

같은 process 내 threads만 조율한다면 userspace mutex가 가장 단순할 수 있다. 여러 local processes가 whole file에 협력한다면 flock이 편할 수 있고, byte ranges와 standard portability가 중요하면 POSIX fcntl locks를 검토한다. Traditional close semantics가 위험하거나 open-description lifetime이 자연스러우면 OFD locks가 더 적합할 수 있다. Cache holder가 conflicting open/truncate 전에 notification과 grace period가 필요하면 lease가 다른 문제를 해결한다. Remote filesystem이면 server/protocol semantics가 핵심이므로 local 선택표를 그대로 쓰면 안 된다. **Lock API의 기능 목록이 아니라 owner identity, range, inheritance, release condition, remote failure를 먼저 적은 뒤 mechanism을 고르는 것**이 맞다.

## CHAPTER 36 · File coordination correctness는 lock을 잡았다는 사실이 아니라 모든 mutation path와 lifetime이 닫히는 proof다

한 file-concurrency 설계를 승인하려면 다음을 답할 수 있어야 한다. Advisory protocol에 모든 writers/readers가 참여하는가. Traditional POSIX lock의 process-associated close semantics를 감당할 수 있는가. Fork/dup/exec 뒤 어떤 owner가 lock을 유지하는가. OFD/flock에서 last-reference release가 명확한가. Path rename/unlink와 inode generation을 혼동하지 않는가. mmap과 zero-copy 경로도 같은 consistency policy를 따르는가. Multiple locks의 ordering이 고정됐는가. Lease break deadline 뒤에도 correctness가 유지되는가. NFS/SMB에서 lock interaction/loss를 검증했는가. Fault injection 후 stale holder와 unreleased locks가 0으로 수렴하는가. **파일 잠금은 syscall 하나가 아니라 file identity, ownership reference, cooperation rule, failure recovery를 묶는 concurrency protocol**이다.
