# PART 13 · filesystem 내부 — namespace, cache, allocation, crash consistency

파일 API는 pathname을 persistent object로 바꾸는 여러 계층을 숨긴다. filesystem correctness는 **namespace lookup, object identity, cached page, block allocation, write ordering, metadata durability**가 서로 다른 상태라는 사실에서 시작한다.

---

## CHAPTER 01 · path는 object identity가 아니라 namespace resolution recipe다

pathname은 directory entry를 순서대로 lookup하는 문자열 표현이다. lookup 중 mount point, symlink, permission, current working directory, namespace가 결과를 바꿀 수 있다. 같은 문자열도 다른 mount namespace에서는 다른 object를 가리킬 수 있다.

security check에서 path string을 canonicalize한 뒤 나중에 다시 open하면 check/use 사이 namespace가 변할 수 있다. 이미 검증된 directory/file descriptor를 기준으로 operation을 수행하는 방식이 object identity를 더 안정적으로 보존한다. 로그에는 path와 함께 inode/device 또는 stable file identity를 가능한 범위에서 기록한다.

---

## CHAPTER 02 · inode는 filename이 아니라 file metadata와 data mapping의 중심 object다

전형적인 Unix filesystem에서 inode는 type, mode, owner, size, timestamp, link count와 data block mapping 정보를 가진다. filename은 directory entry가 inode를 참조하는 방식으로 관리될 수 있다. 따라서 rename은 file data를 복사하지 않고 namespace entry를 바꾸는 operation일 수 있다.

inode number는 filesystem 범위에서 의미가 있고 reuse될 수 있으므로 영구 global ID로 취급하지 않는다. open handle이 있는 동안 object lifetime과 directory-name lifetime이 분리되는 이유도 inode/open-file reference counting과 연결된다.

---

## CHAPTER 03 · directory는 name→object mapping을 저장하는 특수 file structure다

directory lookup은 entry 수와 filesystem data structure에 따라 hash/tree/index를 사용할 수 있다. 한 directory에 수백만 entry를 두면 metadata cache miss와 lookup/update cost가 커질 수 있다.

filename creation/deletion은 directory metadata update이므로 file contents durability와 별개다. crash-safe file replacement에서 새 file의 contents만 fsync하고 containing directory entry를 durable하게 만들지 않으면 rename/create 결과가 power loss 뒤 사라질 수 있다.

---

## CHAPTER 04 · VFS는 서로 다른 filesystem implementation을 공통 object model로 연결한다

Virtual Filesystem layer는 inode/dentry/file/superblock 같은 generic abstraction을 통해 ext4, f2fs, tmpfs, procfs 등 서로 다른 filesystem에 공통 syscall interface를 제공한다. 같은 `read()`라도 backend semantics와 durability는 filesystem type에 따라 다를 수 있다.

application이 POSIX-like API만 보고 storage behavior를 완전히 추론하면 안 된다. network filesystem, FUSE, pseudo filesystem은 latency와 consistency 특성이 다르다. incident record에는 mount type과 option까지 포함한다.

---

## CHAPTER 05 · mount는 filesystem tree의 namespace 연결점이다

mount operation은 특정 filesystem root를 namespace의 directory 위치에 연결한다. bind mount, overlay, namespace별 mount view 때문에 host의 `/data/x`와 container/process가 보는 `/data/x`가 같은 backing object라는 보장은 없다.

storage bug를 재현할 때 path만 비교하지 말고 mount table과 device/filesystem ID를 확인한다. read-only/remount, noexec/nosuid 같은 option은 동일 file의 allowed operation을 바꿀 수 있다.

---

## CHAPTER 06 · symbolic link는 path resolution을 다른 path로 재귀시킨다

symlink는 target path text를 저장하고 lookup 과정에서 resolution을 이어 간다. attacker가 writable directory에서 symlink를 바꾸면 privileged process의 path-based check가 다른 object로 유도될 수 있다.

safe temporary-file/update code는 symlink follow policy와 directory ownership을 명시한다. kernel의 `openat`/no-follow 계열 primitive처럼 lookup과 open을 가능한 한 하나의 trusted directory context 안에서 수행해 TOCTOU surface를 줄인다.

---

## CHAPTER 07 · hard link와 unlink는 name count와 open reference를 분리한다

hard link는 여러 directory entry가 같은 inode를 참조하게 한다. unlink는 name reference 하나를 제거하지만 open descriptor가 있으면 object가 즉시 제거되지 않을 수 있다. 따라서 `rm` 직후 disk usage가 줄지 않는 현상은 deleted-but-open file로 설명될 수 있다.

log rotation이나 temporary-file pattern에서는 이 lifetime 차이가 중요하다. long-running process가 old log inode를 계속 열고 있으면 새 pathname과 다른 object에 쓰고 있을 수 있다. descriptor target을 확인한다.

---

## CHAPTER 08 · open file description은 descriptor와 underlying I/O state 사이에 있다

process의 fd table entry는 kernel의 open file description을 참조할 수 있고 그 object가 file offset과 status flag를 보유한다. `dup`나 `fork`로 만들어진 descriptor가 같은 open description을 공유하면 offset 변화가 서로 영향을 줄 수 있다.

`두 fd니까 독립적`이라는 가정은 틀릴 수 있다. concurrent sequential read/write가 offset 공유에 의존하면 atomicity 규칙을 확인한다. independent offset이 필요하면 separate open 또는 positional I/O를 사용한다.

---

## CHAPTER 09 · append는 application의 seek+write 조합보다 강한 atomicity를 요구한다

여러 writer가 `seek(end)` 후 write하면 두 process가 같은 end offset을 보고 overwrite할 수 있다. append flag는 filesystem이 각 write의 position selection을 operation과 결합해 처리하도록 한다.

그러나 한 write call 내부 data가 다른 writer와 어느 크기까지 atomic하게 유지되는지는 filesystem/API contract를 확인해야 한다. log record framing을 append 하나에 의존할지, record checksum/sequence를 추가할지 workload 요구로 결정한다.

---

## CHAPTER 10 · block allocation은 logical file offset을 physical storage extent로 바꾼다

filesystem은 file offset range를 device block/extents에 mapping한다. extent 기반 allocation은 연속 block range를 압축해 metadata를 줄일 수 있지만 fragmentation과 free-space layout에 따라 file가 여러 extent로 나뉜다.

write performance는 logical sequential access만으로 결정되지 않는다. allocation locality, filesystem free-space state, device FTL이 영향을 준다. long-running system에서는 fresh filesystem benchmark와 aged/fragmented state를 구분한다.

---

## CHAPTER 11 · sparse file은 logical size와 allocated blocks를 다르게 만든다

seek로 멀리 이동한 뒤 일부 data만 쓰면 중간 hole을 physical block 없이 표현할 수 있는 filesystem이 있다. file size는 크지만 실제 allocated space는 작을 수 있다. hole read는 zero처럼 보일 수 있다.

backup/copy tool이 sparse semantics를 보존하지 않으면 hole을 실제 zero block으로 materialize해 storage 사용량이 폭증한다. disk usage 분석에서 logical size와 allocated block count를 별도 확인한다.

---

## CHAPTER 12 · delayed allocation은 block 선택을 뒤로 미뤄 더 나은 layout을 만들 수 있다

write가 page cache에 들어온 즉시 physical block을 정하지 않고 writeback 시점까지 allocation을 미루면 여러 small write를 더 큰 extent로 결합할 수 있다. 반면 free-space exhaustion 같은 failure가 application write 반환 뒤 늦게 드러날 수 있다.

`write()`가 성공했으니 disk space 확보가 끝났다고 가정하지 않는다. fallocate/preallocation이 필요한 workload와 crash/disk-full behavior를 테스트한다.

---

## CHAPTER 13 · page cache는 file I/O와 virtual memory를 연결하는 shared cache다

normal buffered read/write와 file-backed mmap은 kernel page cache의 같은 page를 공유할 수 있다. 따라서 두 path를 통해 data를 접근할 때 별도 cache가 있다고 가정하면 consistency를 오해한다.

page cache hit는 storage I/O를 피하지만 memory pressure 시 reclaim될 수 있다. benchmark에서 반복 read가 빨라진 이유가 application optimization인지 cache warm state인지 분리한다. drop-cache 실험은 production behavior를 대표하지 않을 수 있으므로 workload-specific hit ratio를 측정한다.

---

## CHAPTER 14 · writeback은 dirty page를 storage로 내보내는 비동기 정책이다

buffered write 후 dirty page는 background writeback에 의해 storage로 내려갈 수 있다. dirty ratio, memory pressure, explicit sync가 writeback 시점을 바꾼다. 많은 dirty data가 한번에 flush되면 latency spike와 I/O queue saturation이 생길 수 있다.

writeback pressure는 writer thread를 throttle할 수 있으므로 application stack에서 `write`가 느려져도 device operation 하나의 latency만이 원인은 아니다. dirty-page metric과 block I/O queue를 함께 본다.

---

## CHAPTER 15 · crash consistency는 data와 metadata write ordering을 함께 설계한다

file contents, inode size, block allocation bitmap, directory entry가 서로 다른 write로 storage에 반영될 수 있다. crash가 중간에 발생하면 일부만 persistent해져 filesystem invariant가 깨질 수 있다.

filesystem은 journaling, copy-on-write, ordered write 같은 전략으로 metadata/data ordering을 제어한다. application transaction은 filesystem이 보장하는 atomicity 범위를 알아야 한다. `rename은 atomic` 같은 문장도 동일 filesystem namespace와 crash durability를 분리해 해석한다.

---

## CHAPTER 16 · journaling은 recovery에 필요한 state transition을 log로 보호한다

metadata journaling에서는 metadata update intent를 journal에 기록하고 commit marker가 durable한 뒤 home location에 checkpoint할 수 있다. crash recovery는 incomplete transaction을 무시하고 committed transaction을 replay한다.

journal이 application data 전체의 transaction을 보장하는 것은 아니다. data=ordered/writeback/journal mode처럼 policy에 따라 data ordering이 다를 수 있다. database WAL과 filesystem journal을 동일한 layer로 생각하지 않는다.

---

## CHAPTER 17 · fsync는 file의 required dirty state를 durability boundary로 밀어낸다

`fsync(fd)`의 정확한 보장은 OS/filesystem/device contract에 따르지만 일반적으로 file data와 필요한 metadata를 stable storage로 flush하기 위한 primitive다. user-space buffer flush나 close만으로 같은 보장을 얻는다고 가정하지 않는다.

fsync latency는 batching과 group commit 대상이 될 수 있다. transaction마다 개별 fsync를 하면 durability는 명확하지만 throughput이 제한될 수 있다. DB engine은 WAL/group commit으로 여러 transaction의 durability cost를 amortize한다.

---

## CHAPTER 18 · 새 pathname durability에는 containing directory sync가 필요할 수 있다

새 file을 만들고 contents를 fsync한 뒤 crash했을 때 directory entry 자체가 durable하지 않으면 pathname이 복구되지 않을 수 있다. crash-safe replace pattern은 temporary file과 containing directory metadata의 durability를 함께 고려한다.

platform/filesystem별 보장을 확인하고 실제 power-failure test 또는 fault-injection을 사용한다. unit test에서 process kill만 하는 것은 storage cache와 power-loss ordering을 충분히 재현하지 못한다.

---

## CHAPTER 19 · rename은 namespace atomicity와 content durability가 다른 성질이다

동일 filesystem 내 rename이 observer에게 old/new name 중 하나로 보이는 atomic namespace update를 제공할 수 있어도 renamed file contents가 stable storage에 도달했다는 의미는 아니다. replace pattern에서 temp contents fsync→rename→directory fsync 순서가 중요한 이유다.

rename 대상이 이미 존재할 때 overwrite semantics, exchange/no-replace option은 API마다 다르다. concurrent updater가 있으면 expected generation과 compare semantics를 추가해 last-writer-wins를 의도적으로 선택한다.

---

## CHAPTER 20 · cross-filesystem move는 rename이 아니라 copy+delete가 될 수 있다

서로 다른 mount/filesystem 사이에서는 inode/object ownership이 다르므로 atomic rename이 불가능할 수 있다. high-level file move API가 copy→fsync?→delete로 fallback하면 중간 failure에서 source와 destination이 모두 존재하거나 partial destination이 남을 수 있다.

operation contract가 atomic move를 요구한다면 동일 filesystem staging area를 사용한다. cross-device backup/migration은 resumable copy와 checksum, commit marker를 별도로 설계한다.

---

## CHAPTER 21 · file lock은 advisory/mandatory와 process/thread semantics를 확인한다

POSIX advisory lock은 cooperating process가 lock protocol을 따를 때만 보호된다. 다른 process가 lock 없이 write하면 kernel이 항상 막아 주는 것은 아니다. `flock`과 record lock의 ownership/dup/fork semantics도 다를 수 있다.

DB/file-based coordination에서 lock file 존재 자체를 lock으로 사용하면 stale file problem이 생긴다. kernel-managed lock 또는 atomic create + owner identity/lease를 사용한다. network filesystem에서는 lock implementation과 failure semantics를 별도 검증한다.

---

## CHAPTER 22 · mmap된 file을 truncate하면 mapping과 file size의 관계가 깨질 수 있다

process가 file page를 mapping한 상태에서 다른 process가 file을 줄이면 기존 mapping의 일부 address가 더 이상 valid backing을 갖지 못할 수 있다. 이후 access가 signal/fault로 이어질 수 있다.

shared mmap protocol은 file resize를 concurrent하게 허용할지 명시해야 한다. generation/version mapping을 사용하거나 writer가 새 file을 만들고 atomic rename으로 교체해 reader mapping lifetime을 분리하는 방법이 있다.

---

## CHAPTER 23 · filesystem benchmark는 page cache와 device cache를 구분해야 한다

첫 read와 두 번째 read latency 차이는 filesystem code보다 page cache warmup 때문일 수 있다. write benchmark도 buffered write 반환만 재면 device durability throughput을 측정하지 못한다.

benchmark 목표가 cached read인지 cold storage read인지, synchronous durability인지 async throughput인지 명시한다. dataset이 RAM보다 큰지, fsync를 포함하는지, device queue depth와 filesystem age를 기록한다.

---

## CHAPTER 24 · dentry/inode cache는 namespace lookup 자체를 cache한다

file data만 cache되는 것이 아니다. recently resolved pathname component와 inode metadata도 memory에 cache될 수 있다. 수백만 tiny file workload에서는 data보다 metadata lookup이 병목이 될 수 있다.

negative dentry처럼 `이 이름이 없음` 결과도 cache될 수 있다. external filesystem change나 network-backed namespace에서는 cache invalidation semantics가 중요하다. lookup benchmark는 warm/cold metadata state를 구분한다.

---

## CHAPTER 25 · free-space allocator는 fragmentation과 allocation latency를 결정한다

filesystem은 bitmap, tree, group/extent allocator를 사용해 free block을 추적한다. free space percentage가 충분해도 큰 contiguous extent가 부족하면 allocation이 fragmented되거나 metadata work가 증가할 수 있다.

write-heavy long-lived volume은 delete/create history 때문에 free-space geometry가 달라진다. capacity alarm을 byte percentage 하나로 두지 않고 inode/metadata reserve와 allocation failure도 모니터링한다.

---

## CHAPTER 26 · inode/resource exhaustion은 byte 공간이 남아도 file creation을 막을 수 있다

일부 filesystem은 inode 수나 metadata capacity가 별도 제약이다. tiny file가 매우 많으면 data blocks는 남아 있어도 새 inode를 만들 수 없을 수 있다.

application cache가 object마다 파일 하나를 생성한다면 object count가 storage model의 주요 capacity metric이다. directory entry/inode overhead와 backup/scan cost까지 포함해 blob store/DB 사용 여부를 선택한다.

---

## CHAPTER 27 · filesystem corruption은 recovery tool 실행 전에 failure evidence를 보존한다

metadata checksum/error, I/O error, journal recovery failure가 보이면 즉시 repair를 반복 실행하기보다 block-device health와 read-only snapshot/image를 가능한 범위에서 보존한다. repair tool이 corrupted metadata를 수정해 원본 증거를 잃게 할 수 있다.

corruption 원인은 filesystem software bug뿐 아니라 storage media, controller, power-loss guarantee violation, memory corruption에서 올 수 있다. kernel log와 device SMART/health, power event를 함께 조사한다.

---

## CHAPTER 28 · Android storage는 app sandbox와 database durability 위에서 해석한다

internal app storage는 UID/SELinux boundary와 연결되고 shared/media storage는 다른 access model을 가진다. pathname permission만으로 Android storage access를 설명할 수 없다.

SQLite database file은 filesystem 위에 존재하지만 transaction atomicity와 WAL/journal protocol을 DB engine이 추가한다. application이 DB file을 직접 copy/modify하면 engine lock/journal invariant를 깨뜨릴 수 있다. backup은 engine-supported snapshot/export semantics를 사용한다.

---

## CHAPTER 29 · storage incident는 namespace→cache→filesystem→block→device 순서로 좁힌다

`파일이 사라졌다`는 증상에서 먼저 pathname/rename/unlink history와 open descriptor를 확인한다. `저장했는데 복구 후 없어졌다`면 fsync/directory durability와 crash timeline을 본다. `write가 느리다`면 dirty throttling과 block-device queue를 본다.

각 layer의 metric과 log를 같은 timestamp로 연결한다. application error만 남기면 ENOSPC, EIO, permission, stale mount를 구분하기 어렵다. raw errno와 target filesystem/device identity를 보존한다.

---

## CHAPTER 30 · file update protocol은 object identity, atomicity, durability를 명시해야 한다

중요 file을 안전하게 갱신하려면 최소한 **어떤 이름이 current version을 가리키는가, partial contents가 노출될 수 있는가, commit point가 어디인가, crash 후 어느 version으로 복구되는가**를 정의한다.

일반적인 temp-write→fsync→atomic rename→directory fsync 패턴도 모든 filesystem/network storage에서 동일하게 보장된다고 가정하지 않는다. target platform의 documented semantics와 fault-injection으로 검증한다. 파일 API의 목적은 bytes를 쓰는 것이 아니라 **crash를 포함한 모든 중간 상태에서 namespace와 data invariant를 유지하는 것**이다.