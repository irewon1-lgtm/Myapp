# PART 94 · Filesystem Event Observation — inotify, fanotify, rename races, overflow recovery, permission mediation

Filesystem event API는 “파일이 바뀌면 callback 하나 받는 기능”이 아니다. Kernel이 관찰한 object 변화가 bounded queue를 거쳐 userspace에 전달되는 동안 pathname과 inode identity가 달라질 수 있고, rename의 두 절반이 시간차를 두고 보이거나 queue overflow로 사건 일부가 사라질 수 있다. Recursive tree 감시는 새 directory가 생기는 순간과 watch가 설치되는 순간 사이에 구멍이 생기며, fanotify permission event는 observer가 느려지면 다른 process의 open/read 자체를 막는 inline decision path가 된다. 따라서 watcher의 진짜 과제는 event를 많이 받는 것이 아니라 **event stream이 불완전할 수 있음을 전제로 현재 filesystem state와 다시 수렴하는 것**이다. P94는 inotify/fanotify를 syscall 목록이 아니라 identity, ordering, loss, backpressure, recovery의 관점에서 다룬다.

## CHAPTER 01 · Filesystem event는 state 그 자체가 아니라 state transition에 대한 관찰 기록이다

`IN_CREATE`를 받았다는 사실은 “지금도 그 파일이 존재한다”는 보장이 아니다. Event가 queue에 들어간 뒤 userspace가 읽기 전에 file이 rename되거나 삭제될 수 있고, 동일 pathname에 다른 inode가 새로 만들어질 수도 있다. 반대로 watcher가 시작되기 전에 이미 존재한 files는 create event 없이 현재 state에 들어와 있다. 그래서 event consumer가 event log만 replay해 완전한 현재 상태를 만들 수 있다고 생각하면 시작점과 loss recovery에서 틀어진다. Robust design은 initial snapshot으로 현재 state를 만들고 event stream을 그 snapshot 이후 변화의 힌트로 적용하며, identity나 sequence에 의심이 생기면 다시 authoritative filesystem scan으로 수렴한다. **Event stream은 truth database가 아니라 truth를 싸게 추적하기 위한 delta channel**이다. 이 관점을 잡으면 overflow가 곧 corruption이 아니라 “incremental knowledge를 잃었으므로 재동기화해야 하는 상태”라는 의미가 된다.

## CHAPTER 02 · inotify instance는 watch 목록과 event queue를 함께 소유하는 kernel object다

`inotify_init1()`은 inotify instance를 가리키는 file descriptor를 만들고, 이후 `inotify_add_watch()`가 그 instance의 watch list를 확장한다. 발생한 events는 같은 instance의 queue에 쌓여 `read()`로 소비된다. 이 fd를 `dup()`하면 별도 queue가 생기는 것이 아니라 같은 underlying instance에 대한 또 하나의 reference가 될 수 있으므로 여러 readers가 같은 queue를 경쟁해서 소비할 수 있다. 모든 references가 닫히면 instance와 watches가 정리된다. 이 구조는 watcher library에서 raw fd 숫자보다 **instance lifetime과 reader ownership**을 명확히 해야 함을 뜻한다. 두 threads가 각각 “내가 모든 events를 받는다”고 믿고 동일 instance를 읽으면 사건이 둘에게 broadcast되는 것이 아니라 한 queue를 나눠 소비할 수 있다. Dispatch는 한 owner가 읽고 내부 subscriber로 분배하는 편이 대개 더 예측 가능하다.

## CHAPTER 03 · Watch descriptor는 pathname handle이 아니라 instance 안에서 filesystem object를 식별하는 작은 정수다

`inotify_add_watch(path)`는 pathname을 resolution한 뒤 해당 filesystem object에 watch를 연결하고 watch descriptor(wd)를 반환한다. 같은 inode를 hard link나 다른 path로 다시 watch하면 기존 watch가 수정되거나 같은 wd가 반환될 수 있다. 따라서 `wd → original string path`를 영구 진실로 저장하면 rename/hard-link 환경에서 의미가 어긋난다. wd는 inotify instance 내부의 watch identity이고 pathname은 그 watch를 설치할 때 사용한 한 이름일 뿐이다. Event processing에서 current pathname이 필요하면 watched directory identity와 event의 `name`을 조합하거나 filesystem을 다시 조회해야 한다. **Object identity와 name identity를 분리**해야 rename 뒤 old path를 새 object에 잘못 붙이는 cache bug를 막을 수 있다. P13의 inode/path 구분이 event subsystem에서도 그대로 반복된다.

## CHAPTER 04 · Inotify의 inode 기반 감시는 hard link가 여러 이름을 가진다는 사실을 그대로 드러낸다

한 inode가 여러 hard links를 가질 때 특정 file object 자체를 watch하면 다른 directory entry를 통해 발생한 activity도 같은 object event로 나타날 수 있다. 반대로 directory watch는 그 directory entry namespace 안에서 일어난 create/delete/move를 name과 함께 알려준다. 따라서 “파일의 내용 변화”와 “이 경로 이름의 변화”는 서로 다른 monitoring 문제다. Content cache invalidation은 inode-oriented watch가 유용할 수 있고, directory listing cache는 parent directory의 namespace events가 더 중요하다. 한쪽만 감시하면 blind spot이 생긴다. 특히 hard link가 존재할 수 있는 storage에서 pathname을 primary key로 둔 indexer는 동일 inode를 중복 object로 만들 수 있다. **Watcher schema는 object identity와 directory-entry identity 중 무엇을 추적하는지 먼저 선언**해야 한다.

## CHAPTER 05 · Directory watch의 name field는 child pathname 전체가 아니라 watched directory 안의 한 component다

Directory를 watch했을 때 child event의 `name`은 해당 directory 내부 entry 이름을 제공한다. 이것을 root부터의 canonical full path라고 착각하면 nested watcher에서 경로 조합이 틀어진다. Application은 `wd → watched-directory identity/current path` mapping과 `name`을 조합해 표시용 path를 만들 수 있지만, rename이 동시에 일어나면 그 mapping도 바뀔 수 있다. Event가 도착한 뒤 `stat(full_path)`를 했는데 ENOENT가 나는 것은 event가 거짓이라서가 아니라 state가 이미 다음 transition으로 진행했을 수 있기 때문이다. Security-sensitive code는 event name을 신뢰된 input으로 취급해서도 안 된다. **Event는 당시 directory entry에 대한 observation이고, 후속 pathname lookup은 새 race를 시작한다.** Lookup 결과가 반드시 event 당시 object와 같아야 한다면 file handle이나 fd 중심 identity가 필요하다.

## CHAPTER 06 · Watch mask 업데이트는 같은 object의 기존 subscription을 덮어쓸 수 있으므로 ownership 병합이 필요하다

한 instance에서 같은 filesystem object를 다시 `inotify_add_watch()`하면 새 mask가 기존 watch에 영향을 줄 수 있다. 여러 library component가 각각 원하는 events를 독립적으로 추가한다고 생각하면 한 component가 다른 component의 mask를 뜻하지 않게 바꿀 수 있다. `IN_MASK_ADD`는 기존 mask에 bits를 더하는 방식에 도움을 주고, `IN_MASK_CREATE`는 이미 watch가 있을 때 조용히 수정하지 않고 실패시키는 데 사용할 수 있다. 하지만 여러 consumers가 subscription lifecycle을 공유한다면 refcount와 desired-mask union을 userspace에서 관리하는 편이 더 명확하다. **Watch descriptor를 global singleton resource처럼 다루면서 누가 어떤 mask를 소유하는지 기록**해야 unsubscribe 한 component가 다른 component의 관심 event까지 제거하지 않는다. Watch registry도 일반 resource manager와 같은 ownership 문제가 있다.

## CHAPTER 07 · Event record는 variable-length framing이므로 read buffer를 구조체 배열처럼 고정 index하면 안 된다

`struct inotify_event` 뒤에는 `len`으로 길이가 지정된 optional name bytes가 붙고 alignment padding도 포함될 수 있다. 한 번의 `read()`에는 여러 records가 연속해서 들어올 수 있으므로 parser는 `sizeof(struct inotify_event) + event->len`만큼 정확히 전진해야 한다. Name이 없는 self event도 있고, directory child event는 null-terminated name을 포함할 수 있다. Fixed-size struct cast만 반복하면 다음 record의 시작점을 잘못 계산해 queue data를 corruption처럼 해석한다. Buffer도 최소 한 event와 최대 name을 담을 만큼 확보하는 것이 안전하다. Fanotify는 더 다양한 variable information records를 사용하므로 같은 교훈이 더 강해진다. **Kernel event API를 읽는 코드는 syscall 성공 여부뿐 아니라 framing parser가 untrusted variable-length stream을 안전하게 순회하는지**를 검증해야 한다.

## CHAPTER 08 · O_NONBLOCK은 queue가 비었을 때 기다리지 않게 할 뿐 event ordering이나 loss를 해결하지 않는다

`inotify_init1(IN_NONBLOCK)` 또는 fd flag를 사용하면 queue가 비어 있을 때 `read()`가 blocking하지 않고 EAGAIN으로 돌아올 수 있다. 이 fd를 poll/epoll event loop에 넣으면 filesystem observation을 socket/timer와 같은 readiness architecture에서 다룰 수 있다. 하지만 readable event 하나가 “queue에 event 하나만 있다”는 뜻은 아니므로 wakeup마다 가능한 records를 충분히 drain해야 한다. Edge-triggered epoll을 쓰면서 한 record만 읽고 돌아가면 queue에 data가 남아도 다음 edge가 오지 않아 watcher가 멈출 수 있다. 반대로 busy loop로 EAGAIN을 반복하면 CPU를 낭비한다. **Readiness는 queue consumption 가능성을 알리는 신호이고, 실제 progress는 read한 byte/record 수로 판단**해야 한다. P25의 event-engine 원칙이 그대로 적용된다.

## CHAPTER 09 · Event coalescing은 syscall 횟수를 줄이지만 “한 event = 한 실제 operation”이라는 회계 가정을 깨뜨린다

Inotify는 아직 읽히지 않은 queue의 연속적이고 동일한 events를 coalesce할 수 있다. 따라서 `IN_MODIFY` event가 한 번 왔다고 write syscall이 한 번 있었다는 뜻이 아니고, event count를 file modification 횟수 통계로 쓰면 실제 activity를 과소계수할 수 있다. Fanotify도 동일 object/process의 일부 consecutive events를 merge할 수 있으며 permission events처럼 merge되지 않는 예외가 있다. Monitoring의 목적이 “무언가 바뀌었으니 cache를 invalidate”라면 coalescing은 유리하지만, audit trail처럼 every operation cardinality가 필요하다면 API semantics와 요구가 맞지 않을 수 있다. **Event notification과 forensic audit log를 같은 것으로 취급하지 않는 것**이 중요하다. 필요한 evidence granularity에 맞는 instrumentation을 선택해야 한다.

## CHAPTER 10 · Queue overflow는 일부 events가 없어진 상태이므로 이후 delta 적용의 신뢰성을 끊어야 한다

Inotify queue는 무한하지 않고 `/proc/sys/fs/inotify/max_queued_events` 같은 limit 아래에서 동작한다. Producer가 consumer보다 빠르면 `IN_Q_OVERFLOW`가 나타나고 events가 유실될 수 있다. 이때 overflow marker 이후 event만 계속 적용하면 어떤 create/delete/rename을 놓쳤는지 알 수 없어 cache가 실제 filesystem과 영구적으로 어긋날 수 있다. 올바른 recovery는 watcher가 관리하는 state를 “dirty/unknown”으로 전환하고 authoritative scan으로 재구축한 뒤 watch set과 snapshot을 다시 정합시키는 것이다. Rebuild 중에도 filesystem은 변하므로 scan과 watcher 재설치 순서를 설계해야 한다. **Overflow는 하나의 이벤트가 아니라 incremental model의 신뢰 경계를 무효화하는 control event**다. SRE 관점에서는 overflow count와 resync duration을 핵심 health metric으로 봐야 한다.

## CHAPTER 11 · Overflow recovery는 queue를 비우는 것보다 cache와 watch generation을 새로 만드는 작업이다

단순히 `IN_Q_OVERFLOW`를 로그로 남기고 queue를 계속 읽으면 누락된 past transitions는 돌아오지 않는다. 더 안전한 패턴은 current inotify generation을 폐기하거나 resync mode로 전환하고, filesystem tree를 다시 scan하여 snapshot generation N+1을 만든 뒤 새 watches와 결합하는 것이다. Rescan 중 발생한 events를 어떻게 다룰지도 정해야 한다. 먼저 watches를 설치한 뒤 scan하면 scan 중 events를 queue에 보존할 수 있지만 새 subdirectory race가 남고, scan 후 watch면 scan과 watch 사이 변화가 빠질 수 있다. Application이 허용하는 consistency 수준에 따라 retry loop나 generation comparison을 사용한다. **Resynchronization 자체도 concurrency algorithm**이며 “다시 훑으면 된다” 한 줄로 끝나지 않는다. 완료 조건은 cache가 filesystem과 다시 수렴했다는 증거다.

## CHAPTER 12 · Inotify는 recursive API가 아니므로 directory tree 감시는 watch graph를 직접 유지해야 한다

Root directory 하나를 watch한다고 모든 descendants가 자동으로 감시되는 것은 아니다. Recursive watcher는 초기 tree를 walk하면서 각 subdirectory에 watch를 설치하고, 이후 새 directory 생성/move-in events가 오면 새 watch를 추가해야 한다. Directory 수가 크면 watch count와 kernel memory 사용이 커지고 `max_user_watches` limit에 닿을 수 있다. Directory가 제거되거나 unmounted되면 registry에서도 watch를 정리해야 한다. Tree가 매우 동적이면 watch graph 유지 자체가 주요 workload가 된다. **Recursive monitoring은 syscall 옵션이 아니라 userspace가 directory topology와 watch lifetime을 동기화하는 distributed-like state problem**이다. 수백만 directories를 감시한다면 fanotify filesystem/mount mark가 더 적합한지 비교해야 한다.

## CHAPTER 13 · 새 subdirectory 생성과 watch 설치 사이에는 내부 파일을 놓칠 수 있는 race window가 있다

Watched parent에서 새 directory create event를 받은 뒤 userspace가 그 child에 watch를 추가하기까지 시간이 걸린다. 그 사이 다른 process가 child 안에 files를 만들고 지우면 해당 events는 새 watch 설치 전이라 관찰되지 않을 수 있다. Inotify man-page가 새 subdirectory에 watch를 추가한 즉시 그 contents를 scan하는 방식을 권하는 이유다. 하지만 scan 중에도 변화는 계속되므로 high-assurance indexer는 child snapshot과 queued events를 generation 방식으로 병합해야 할 수 있다. “create directory event를 받자마자 watch를 붙였으니 완전하다”는 주장은 scheduler latency를 무시한다. **Topology expansion은 항상 observe-parent → install-child-watch 사이의 blind interval을 가진다.** 요구가 strict하면 전체 filesystem mark를 지원하는 fanotify 같은 다른 primitive를 검토한다.

## CHAPTER 14 · Rename은 IN_MOVED_FROM과 IN_MOVED_TO 두 events를 cookie로 연결하지만 transaction record는 아니다

같은 monitored scope 안에서 rename이 일어나면 old parent의 `IN_MOVED_FROM`과 new parent의 `IN_MOVED_TO`가 같은 nonzero cookie로 연결될 수 있다. 이것은 path index가 object movement를 추적하는 데 유용하지만 pair를 하나의 atomic record처럼 가정하면 안 된다. 두 events 사이에 다른 events가 끼어들 수 있고, source 또는 destination directory가 watch scope 밖이면 한쪽만 볼 수 있다. Cross-filesystem move는 rename 하나가 아니라 copy+delete 같은 동작으로 나타날 수도 있다. Consumer는 cookie를 bounded pending table에 보관하고 timeout 시 unpaired move를 “scope 밖으로 나감/들어옴 또는 loss”로 처리해야 한다. **Rename cookie는 correlation key이지 two-phase transaction commit proof가 아니다.** Overflow가 발생하면 pending pairs도 무효화하고 resync해야 한다.

## CHAPTER 15 · Rename pair matching에서 무한 대기는 memory leak과 stale path를 동시에 만든다

`IN_MOVED_FROM`을 받은 뒤 matching `IN_MOVED_TO`가 반드시 올 거라 믿고 forever pending으로 두면 watched tree 밖으로 이동한 object마다 table이 쌓인다. 반대로 너무 짧은 timeout으로 delete 처리하면 scheduler delay나 queue backlog 때문에 늦게 도착한 matching event를 새 create로 오인할 수 있다. Pending rename entry에는 cookie뿐 아니라 source directory generation, old name, enqueue time을 기록하고 bounded expiration policy를 둬야 한다. Pair가 완성되면 inode/path cache를 원자적으로 이동시키고, timeout이면 source disappearance를 확인하는 targeted rescan이 안전하다. **Rename correlation은 queue latency distribution을 고려한 bounded reconciliation problem**이다. 단순 map[cookie] 한 줄은 happy path만 표현한다.

## CHAPTER 16 · IN_MOVE_SELF는 watched object가 움직였다는 사실을 알려도 새 pathname을 직접 제공하지 않는다

File 또는 directory 자체를 watch하는 동안 그 object가 rename되면 `IN_MOVE_SELF`를 받을 수 있지만 event 하나만으로 destination pathname을 알 수 있는 것은 아니다. Object watch는 inode identity에 붙고 이름은 parent directory namespace의 property이기 때문이다. Current path를 유지해야 하는 application은 parent directory watches의 MOVED_FROM/TO correlation과 object watch를 함께 사용하거나, 이후 별도 namespace lookup을 수행해야 한다. Hard links가 있으면 “the pathname” 자체가 하나가 아닐 수도 있다. **Object movement 관찰과 name resolution은 서로 다른 데이터 모델**이다. File watcher UI가 old path를 계속 표시하는 버그는 kernel이 rename destination을 숨긴 게 아니라 application이 identity와 name을 하나로 합쳤기 때문에 생긴다.

## CHAPTER 17 · Delete와 IN_IGNORED는 object disappearance와 watch lifetime 종료를 구분해 보여준다

Watched object가 삭제되거나 filesystem이 unmount되거나 watch가 명시적으로 제거되면 `IN_IGNORED`가 발생해 watch descriptor가 더 이상 active하지 않음을 알릴 수 있다. `IN_DELETE_SELF`는 watched object 자체의 deletion을 의미하고, directory child의 `IN_DELETE`와는 다른 scope다. Consumer는 delete event를 state mutation으로 처리하는 것과 watch registry entry를 제거하는 일을 분리해야 한다. Watch가 자동 제거됐는데 userspace registry에는 남아 있으면 나중에 wd reuse 시 stale metadata를 잘못 연결할 수 있다. **Filesystem object lifetime과 watcher-subscription lifetime은 독립 state machine**이며, 둘 다 terminal transition을 처리해야 한다. Cleanup code도 event handling만큼 중요하다.

## CHAPTER 18 · Watch descriptor 재사용 가능성은 작은 integer를 영구 object ID로 쓰면 안 되는 이유다

Watch descriptor는 instance-local integer이고 제거된 뒤 미래에 다시 할당될 수 있다. Linux man-page는 극단적인 경우 unread old events가 남은 상태에서 동일 wd가 재사용되면 application이 incarnation을 혼동할 수 있는 오래된 corner case까지 설명한다. 현실에서 INT_MAX를 순환해야 해 드물더라도 설계 원칙은 분명하다. Registry key에 wd만 두지 말고 userspace generation을 함께 두고, watch removal 후 pending events를 어떻게 drain할지 정한다. Instance 자체를 재생성하면 generation을 통째로 바꿀 수 있어 overflow recovery에도 유용하다. **Small handle은 object identity가 아니라 현재 reference table의 index**라는 원칙은 fd/PID와 동일하다. Handle reuse를 견디는 코드가 장기 실행 daemon에서 안전하다.

## CHAPTER 19 · Mount로 가려진 subtree는 기존 directory watch의 “경로 아래 모두”라는 직관을 깨뜨린다

Watched directory 아래 특정 path에 다른 filesystem이 mount되면 pathname tree는 같아 보여도 underlying objects와 event visibility는 달라질 수 있다. Inotify가 parent directory를 감시한다고 새 mount 내부의 모든 activity를 자동으로 recursive 관찰하는 것은 아니다. Bind mount와 namespace-specific mount topology까지 포함하면 한 process가 보는 namespace와 watcher process가 보는 namespace가 다를 수도 있다. Container host-side monitor는 container 내부 pathname과 host mount topology를 같은 것으로 가정하면 안 된다. **Filesystem observation scope는 lexical path prefix가 아니라 mount/object topology의 함수**다. Security or backup watcher는 mount attach/detach 자체도 inventory에 포함하고 scope 변화 때 watches를 재평가해야 한다.

## CHAPTER 20 · Namespace가 다르면 같은 pathname 문자열도 다른 mount tree를 가리킬 수 있다

Mount namespace는 process마다 pathname resolution view를 달리할 수 있다. `/data/x`라는 문자열이 watcher와 target process에서 같은 filesystem object를 가리킨다고 보장할 수 없다. Inotify는 watcher가 watch를 설치할 때 자신의 namespace에서 resolution한 object에 연결된다. 다른 namespace에서 동일 이름이 다른 mount에 연결되어 있으면 events가 예상과 다르게 보인다. Fanotify의 최근 mount-namespace monitoring 기능은 mount attach/detach 같은 topology 변화 자체를 관찰할 수 있는 방향을 제공하지만 지원 조건과 flags를 정확히 맞춰야 한다. **Path string 기반 observability에는 namespace identity가 필수 context**다. Incident log에는 mount namespace/container identity, fsid/mount id 같은 정보를 함께 남겨야 object correlation이 가능하다.

## CHAPTER 21 · fanotify는 inode 한두 개가 아니라 mount 또는 filesystem scope를 감시할 수 있어 watch topology 비용을 바꾼다

Fanotify는 mark를 inode, mount, filesystem 등의 scope에 적용할 수 있어 대규모 tree에서 directory마다 inotify watch를 유지하는 비용을 줄일 수 있다. 대신 event format과 privilege, supported event types, identity model이 더 복잡하다. Mount mark는 해당 mount 아래 events를 관찰하지만 nested mount와 filesystem mark의 scope 차이를 이해해야 한다. Filesystem-wide mark는 같은 filesystem object들을 넓게 포괄하지만 deployment storage topology가 바뀌면 의미도 달라진다. **inotify vs fanotify 선택은 기능 수가 아니라 observation scope와 watch-state cardinality의 trade-off**다. 수백만 directories를 다루는 scanner와 desktop UI의 요구가 같은 API를 선택할 이유는 없다.

## CHAPTER 22 · Fanotify event fd 방식과 FID 방식은 object identity와 resource lifetime을 다르게 만든다

전통적인 fanotify notification은 event metadata 안에 대상 object를 가리키는 open file descriptor를 제공할 수 있다. Consumer는 그 fd로 `fstat()`하거나 object에 접근할 수 있지만 반드시 close하여 fd leak을 막아야 한다. `FAN_REPORT_FID`를 사용하면 event fd 대신 filesystem ID와 file handle을 포함하는 information record로 object를 식별할 수 있어 queue가 수많은 live descriptors를 전달하는 모델을 바꾼다. 그러나 file handle을 다시 pathname으로 해석하는 과정은 별도 권한과 filesystem support를 요구할 수 있다. **Identity representation을 fd, inode/path, fsid+handle 중 무엇으로 선택하느냐가 lifetime과 race surface를 결정**한다. High-rate monitor에서 event-fd close 누락은 곧 fd exhaustion incident가 된다.

## CHAPTER 23 · Fanotify permission event는 observer를 inline authorization path에 넣기 때문에 느린 consumer가 system I/O를 막을 수 있다

`FAN_OPEN_PERM`, `FAN_ACCESS_PERM`, `FAN_OPEN_EXEC_PERM` 같은 events는 단순 사후 notification이 아니다. Target process의 operation이 pending된 상태에서 listener가 allow/deny response를 써야 진행 여부가 정해진다. Antivirus나 policy engine이 이를 이용할 수 있지만 listener latency가 file-open latency에 직접 더해진다. Event loop가 deadlock되거나 overloaded되면 unrelated applications까지 기다릴 수 있다. Permission event handler는 bounded queue, deadline, fail-open/fail-closed policy를 명확히 하고 heavy scanning을 어디까지 inline으로 할지 정해야 한다. **Observer가 enforcement point가 되는 순간 monitoring SLO가 application I/O SLO의 일부**가 된다. 사후 notification과 같은 worker pool에 무제한 섞으면 안 된다.

## CHAPTER 24 · Permission listener가 자신이 검사하는 file을 다시 열면 self-induced recursion과 deadlock을 만들 수 있다

Permission event를 받은 scanner가 대상 file을 분석하기 위해 ordinary open/read를 수행하면 그 접근 자체가 같은 fanotify group의 permission event를 다시 발생시킬 수 있는 구조가 생길 수 있다. Mark/ignore policy, group class, scanner identity에 따른 exemption을 설계하지 않으면 listener가 자신의 결정을 기다리는 순환 의존이 생길 수 있다. Helper subprocess를 사용해도 inherited marks나 monitoring scope가 같으면 문제가 사라지지 않는다. Security product는 **누가 observation 대상이고 누가 trusted mediator인지 trust boundary를 명시**해야 한다. Self-events를 무조건 ignore하는 것도 attacker가 mediator identity를 악용할 수 있으므로 credential/process identity를 안정적으로 검증해야 한다.

## CHAPTER 25 · Permission response는 event fd와 정확한 pending request를 연결해야 하며 duplicate/stale response를 피해야 한다

Fanotify permission event에는 response를 작성할 때 어떤 pending event에 대한 결정인지 식별할 수 있는 정보가 필요하다. Event를 여러 worker에 분배한다면 한 worker가 timeout된 request에 늦은 allow를 쓰거나 두 workers가 같은 request에 중복 응답하지 않도록 ownership을 관리해야 한다. Event object fd를 닫는 것과 permission response를 쓰는 것도 별도 lifecycle이다. Response가 이미 처리된 뒤 stale fd를 사용하면 error가 날 수 있다. **Permission mediation은 request/response protocol**이므로 `RECEIVED → OWNED → DECIDED → RESPONDED → RELEASED` 같은 state를 두는 편이 안전하다. Crash recovery 정책도 listener가 죽었을 때 pending opens가 어떻게 풀리는지 실제 kernel behavior와 service restart 설계를 함께 검증해야 한다.

## CHAPTER 26 · Fanotify queue overflow는 notification completeness를 깨뜨리며 permission workload에서는 capacity 설계가 더 중요하다

Fanotify group도 `max_queued_events` 같은 bounded queue를 가지며 limit을 넘으면 events가 drop되고 `FAN_Q_OVERFLOW`가 전달될 수 있다. 일반 notification monitor라면 inotify처럼 state resync가 필요하다. Permission events는 ordinary notification과 다르게 access decision path에 있으므로 queue sizing과 listener throughput이 system liveness에 직접 관계한다. 무조건 unlimited queue를 요청하는 것은 memory exhaustion risk를 다른 곳으로 옮길 뿐이다. Event storm에서 CPU와 memory를 보호하려면 mark scope를 줄이고, fast-path decisions, bounded enrichment, sampling 가능한 사후 telemetry를 분리한다. **Overflow를 없애려 무한 buffer를 만드는 것이 아니라 overflow 후 안전하게 degraded/recovered되는 protocol을 만드는 것**이 핵심이다.

## CHAPTER 27 · FAN_REPORT_PIDFD는 event origin process의 PID reuse race를 줄이는 stable handle을 제공한다

Fanotify metadata의 integer PID만 오래 저장하면 event 처리 전에 process가 종료되고 PID가 재사용될 수 있다. `FAN_REPORT_PIDFD`를 사용하면 origin process에 대한 pidfd information record를 받아 특정 process generation을 더 안정적으로 식별할 수 있다. P92에서 본 것처럼 pidfd는 raw PID lookup race를 줄이는 kernel handle이다. 다만 thread ID reporting과 pidfd reporting 조합에는 API 제한이 있을 수 있고, pidfd 자체도 close해야 하는 resource다. **Filesystem event의 subject identity도 process generation 문제**이며, security decision이나 audit correlation에서 raw PID 하나에 기대면 안 된다. High-rate monitoring에서는 pidfd count와 lifetime도 bounded하게 관리해야 한다.

## CHAPTER 28 · FAN_FS_ERROR는 filesystem health signal이지 개별 I/O 성공 여부를 증명하는 completion channel이 아니다

Fanotify의 `FAN_FS_ERROR`는 filesystem metadata inconsistency 같은 error를 userspace health monitor에 알리는 framework다. Error record에는 errno와 suppressed additional error count가 포함될 수 있고 FID 정보와 결합해 affected object를 식별할 수 있다. Kernel documentation은 이것을 특정 read/write operation이 성공했는지 검증하는 API로 사용하지 말라고 명확히 구분한다. 또한 underlying filesystem support가 필요하며 현재 문서 기준 지원 범위가 제한적일 수 있다. **Health event와 operation completion을 같은 것으로 합치지 않아야 한다.** Storage daemon은 FAN_FS_ERROR를 incident trigger로 사용하되 application durability는 fsync/write error 같은 직접 I/O contract로 별도 검증해야 한다.

## CHAPTER 29 · FAN_PRE_ACCESS 같은 range-aware event는 content 준비를 access critical path 앞에 배치한다

최근 fanotify에는 read/write access 전에 file range 정보를 전달하는 `FAN_PRE_ACCESS` 계열 기능이 추가되어 listener가 해당 range content를 미리 준비하거나 수정할 기회를 줄 수 있다. Event에 offset/count 정보가 추가되므로 whole-file decision보다 세밀하지만, listener latency가 data access critical path에 더 강하게 연결된다. Sparse/remote tiered storage에서 on-demand hydration 같은 use case가 가능하지만 range overlap, concurrent accesses, duplicate preparation을 관리해야 한다. Kernel version과 filesystem/support 조건도 확인해야 한다. **새 event type을 쓴다고 data pipeline이 자동 coherent해지는 것이 아니라 range ownership과 preparation completion protocol이 새로 생긴다.** Version-gated feature detection과 fallback path가 필수다.

## CHAPTER 30 · Ignore marks는 event storm을 줄이는 도구지만 suppression scope를 틀리면 blind spot을 만든다

Fanotify는 mark와 ignore mask를 조합해 특정 subtree/object/events를 제외할 수 있다. Scanner 자신의 cache directory나 trusted artifacts를 제외하면 self-generated noise를 크게 줄일 수 있지만, attacker가 excluded path로 data를 이동할 수 있는 security model에서는 blind spot이 된다. Ignore rule이 modification 뒤에도 유지되는지, inode/mount/filesystem 어느 level에 적용되는지도 semantics를 정확히 확인해야 한다. **Noise reduction rule도 security policy**다. 운영 중 dynamic configuration을 바꿀 때 active mark set과 intended policy를 diff하고, excluded scope에서도 별도 integrity mechanism이 있는지 확인해야 한다.

## CHAPTER 31 · Fanotify privilege와 capability 요구는 monitoring deployment architecture를 결정한다

Fanotify의 일부 class, mount/filesystem marks, permission handling, audit integration은 capability와 kernel configuration 요구가 있다. Root-like privilege를 가진 daemon 하나에 전체 filesystem visibility와 allow/deny 권한을 주는 것은 강력한 attack surface이기도 하다. Principle of least privilege에 따라 observation-only와 permission mediation을 분리하거나, privileged collector가 minimal event metadata만 less-privileged analyzer에 전달하는 구조를 고려할 수 있다. Container 안에서 필요한 capability가 없으면 host agent가 감시해야 할 수도 있고 namespace visibility 차이가 다시 생긴다. **Monitoring 기능을 켜기 위한 privilege는 부수 설정이 아니라 threat model의 일부**다. Compromise 시 어떤 files를 읽거나 access를 막을 수 있는지 평가해야 한다.

## CHAPTER 32 · Watch/mark limits는 단순 튜닝값이 아니라 per-user kernel memory admission control이다

Inotify에는 `max_user_instances`, `max_user_watches`, `max_queued_events`, fanotify에는 group/mark/queue limit들이 있어 한 user가 kernel memory를 무제한 소비하지 못하게 한다. Large repository IDE, sync agent, antivirus가 같은 user 아래 있으면 서로 limit을 경쟁할 수도 있다. 무작정 sysctl을 크게 올리기 전에 실제 watch cardinality, object lifetime, memory footprint, overflow rate를 측정해야 한다. Watch leak이 있는 program에서 limit 확대는 incident 발생 시간을 늦출 뿐이다. **Resource limit에 도달했을 때 fail mode와 fallback scan 경로**를 설계해야 한다. Monitoring system 자체가 resource exhaustion의 원인이 되면 관측 기능이 production availability를 해친다.

## CHAPTER 33 · Cache coherence watcher는 event 적용보다 periodic verification과 rebuild path를 가져야 한다

Filesystem watcher를 search index, config reload, asset cache invalidation에 쓰면 “event를 받았으니 cache가 항상 정확하다”는 믿음이 쉽게 생긴다. Queue overflow 외에도 startup race, recursive watch gap, mount topology 변화, permission error, process restart가 delta stream을 불완전하게 만들 수 있다. 그래서 critical cache는 event-driven fast path와 주기적 checksum/scan verification을 결합하는 편이 강하다. Verification이 mismatch를 찾으면 어떤 subtree/generation부터 재구축할지 정한다. **Event watcher는 coherence accelerator이고 rebuild가 correctness safety net**이다. Rebuild가 너무 비싸서 실행할 수 없다면 그 cache는 결국 복구 불가능한 state machine이 된다.

## CHAPTER 34 · Filesystem-event fault injection은 overflow, rename, unmount, restart를 서로 다른 failure로 실제 흔들어야 한다

테스트는 file 하나를 수정해 MODIFY event가 오는지만 보면 부족하다. Queue를 의도적으로 작게 하거나 consumer를 멈춰 overflow를 만들고, rename pair 중 scope 밖 이동, rapid rename chain, hard links, watched object delete/recreate, subtree 신규 directory 안 즉시 file 생성, mount/unmount, watcher process restart를 주입해야 한다. Fanotify는 permission listener 지연, deny/allow timeout, self-access recursion, event-fd leak, FID parser unknown records를 테스트한다. `FAN_FS_ERROR`는 지원 filesystem 환경에서만 실제 검증 가능하므로 미지원 환경을 PASS로 속이면 안 된다. **각 failure class는 cache가 authoritative scan으로 다시 수렴하고 resource counts가 baseline으로 돌아오는지** 확인해야 한다.

## CHAPTER 35 · inotify, fanotify, periodic scan 선택은 event richness보다 correctness와 scope 비용으로 결정한다

사용자 홈의 몇 directories를 UI 갱신용으로 감시한다면 inotify가 단순하고 충분할 수 있다. 거대한 filesystem 전체를 감시하거나 permission decision/FID가 필요하면 fanotify가 더 적합할 수 있지만 privilege와 parser/latency complexity가 커진다. Remote object store나 semantics가 kernel event API와 맞지 않으면 periodic scan/change feed가 더 정확할 수도 있다. 어떤 선택에서도 overflow/restart recovery가 필요하다. **“event API를 쓰면 scan이 필요 없다”가 아니라 scan 빈도를 줄이고 latency를 낮추는 것**이 현실적 모델이다. Workload cardinality, required latency, loss tolerance, privilege, deployment filesystem을 표로 놓고 선택해야 한다.

## CHAPTER 36 · Filesystem observation correctness는 event를 한 번도 놓치지 않는다는 주장 대신 state convergence를 증명해야 한다

안전한 watcher는 다음을 답할 수 있어야 한다. Watch가 pathname인지 object identity인지 구분하는가. Hard link와 rename 뒤 cache key가 올바른가. Variable event framing을 안전하게 파싱하는가. Queue overflow를 만나면 incremental state를 버리고 resync하는가. Recursive watch 설치 race를 보완하는가. Rename cookie가 unpaired일 수 있음을 처리하는가. Watch descriptor reuse를 generation과 구분하는가. Mount namespace와 filesystem scope를 기록하는가. Fanotify permission handler가 bounded latency와 fail policy를 갖는가. FID/pidfd resources를 회수하는가. Fault injection 후 cache가 authoritative filesystem과 다시 일치하는가. **Watcher의 최종 proof는 event completeness가 아니라 어떤 loss와 race 뒤에도 bounded time 안에 현재 filesystem state로 수렴한다는 것**이다.
