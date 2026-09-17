# PART 13 · filesystem 내부 — namespace, cache, allocation, crash consistency

파일 API는 pathname을 persistent object로 바꾸는 여러 계층을 숨긴다. filesystem correctness는 **namespace lookup, object identity, cached page, block allocation, write ordering, metadata durability**가 서로 다른 상태라는 사실에서 시작한다.

---

## CHAPTER 01 · path는 object identity가 아니라 namespace resolution recipe다

pathname은 directory entry를 순서대로 lookup하는 문자열 표현이다. lookup 중 mount point, symlink, permission, current working directory, namespace가 결과를 바꿀 수 있다. 같은 문자열도 다른 mount namespace에서는 다른 object를 가리킬 수 있다.

security check에서 path string을 canonicalize한 뒤 나중에 다시 open하면 check/use 사이 namespace가 변할 수 있다. 이미 검증된 directory/file descriptor를 기준으로 operation을 수행하는 방식이 object identity를 더 안정적으로 보존한다. 로그에는 path와 함께 inode/device 또는 stable file identity를 가능한 범위에서 기록한다.

path resolution을 재현하려면 process의 cwd, root/chroot, mount namespace, symlink chain과 각 component의 inode/device를 함께 캡처한다. rename이나 bind mount가 concurrent하게 일어나면 문자열은 같아도 resolution 결과가 바뀔 수 있다. security-sensitive operation은 trusted dirfd에서 relative lookup을 시작하고 no-follow/beneath 같은 platform 제약을 사용해 namespace 변화가 허용 범위를 벗어나지 못하게 한다. 실패 로그에는 최종 path뿐 아니라 어느 component lookup에서 errno가 발생했는지 남기면 permission과 missing-object 문제를 구분하기 쉽다.

---

## CHAPTER 02 · inode는 filename이 아니라 file metadata와 data mapping의 중심 object다

전형적인 Unix filesystem에서 inode는 type, mode, owner, size, timestamp, link count와 data block mapping 정보를 가진다. filename은 directory entry가 inode를 참조하는 방식으로 관리될 수 있다. 따라서 rename은 file data를 복사하지 않고 namespace entry를 바꾸는 operation일 수 있다.

inode number는 filesystem 범위에서 의미가 있고 reuse될 수 있으므로 영구 global ID로 취급하지 않는다. open handle이 있는 동안 object lifetime과 directory-name lifetime이 분리되는 이유도 inode/open-file reference counting과 연결된다.

incident에서 inode number만 장기 식별자로 저장하면 delete/recreate 뒤 같은 번호가 재사용돼 다른 object를 같은 파일로 오인할 수 있다. device/filesystem identity, generation 가능 정보, open handle lifetime을 함께 사용한다. link count가 0인데 open reference가 남은 deleted file은 pathname lookup에는 보이지 않아도 storage와 I/O를 계속 소비할 수 있다. disk usage가 줄지 않을 때 open fd target과 inode metadata를 대조해 name lifetime과 object lifetime을 분리한다.

---

## CHAPTER 03 · directory는 name→object mapping을 저장하는 특수 file structure다

directory lookup은 entry 수와 filesystem data structure에 따라 hash/tree/index를 사용할 수 있다. 한 directory에 수백만 entry를 두면 metadata cache miss와 lookup/update cost가 커질 수 있다.

filename creation/deletion은 directory metadata update이므로 file contents durability와 별개다. crash-safe file replacement에서 새 file의 contents만 fsync하고 containing directory entry를 durable하게 만들지 않으면 rename/create 결과가 power loss 뒤 사라질 수 있다.

metadata-heavy workload는 file data throughput보다 directory update serialization과 cache locality가 병목일 수 있다. create/unlink/rename rate, lookup latency, dentry/inode cache miss를 data I/O와 분리해 측정한다. 동일 directory에 hot key가 몰리면 hash/tree 자체가 O(log n)이어도 lock contention이나 journal metadata work가 tail을 만들 수 있다. directory sharding을 적용할 때는 lookup path 깊이와 backup/scan 비용까지 포함해 전체 namespace cost를 비교한다.

---

## CHAPTER 04 · VFS는 서로 다른 filesystem implementation을 공통 object model로 연결한다

Virtual Filesystem layer는 inode/dentry/file/superblock 같은 generic abstraction을 통해 ext4, f2fs, tmpfs, procfs 등 서로 다른 filesystem에 공통 syscall interface를 제공한다. 같은 `read()`라도 backend semantics와 durability는 filesystem type에 따라 다를 수 있다.

application이 POSIX-like API만 보고 storage behavior를 완전히 추론하면 안 된다. network filesystem, FUSE, pseudo filesystem은 latency와 consistency 특성이 다르다. incident record에는 mount type과 option까지 포함한다.

VFS가 공통 syscall을 제공한다고 backend의 rename atomicity, locking, cache coherency, sync semantics까지 동일해지는 것은 아니다. 같은 binary를 ext4, tmpfs, NFS/FUSE에 올렸을 때 failure mode가 다를 수 있으므로 요구하는 invariant를 filesystem별 문서와 fault test로 확인한다. trace에서는 VFS syscall latency와 filesystem-specific function, block/device I/O를 이어서 보면 공통 layer에서 막혔는지 backend에서 막혔는지 좁힐 수 있다.

---

## CHAPTER 05 · mount는 filesystem tree의 namespace 연결점이다

mount operation은 특정 filesystem root를 namespace의 directory 위치에 연결한다. bind mount, overlay, namespace별 mount view 때문에 host의 `/data/x`와 container/process가 보는 `/data/x`가 같은 backing object라는 보장은 없다.

storage bug를 재현할 때 path만 비교하지 말고 mount table과 device/filesystem ID를 확인한다. read-only/remount, noexec/nosuid 같은 option은 동일 file의 allowed operation을 바꿀 수 있다.

mount propagation과 over-mount도 진단에 중요하다. 기존 directory 위에 다른 filesystem이 mount되면 이전 contents는 삭제되지 않았어도 pathname에서 가려질 수 있어 “파일이 사라졌다”처럼 보인다. process별 `/proc/.../mountinfo` 계열 정보에서 mount ID, parent, root, source, option을 캡처하고 정상/실패 process를 비교한다. remount나 namespace clone 시점과 application open 시점을 연결하면 동일 path가 서로 다른 backing object로 resolve된 원인을 찾을 수 있다.

---

## CHAPTER 06 · symbolic link는 path resolution을 다른 path로 재귀시킨다

symlink는 target path text를 저장하고 lookup 과정에서 resolution을 이어 간다. attacker가 writable directory에서 symlink를 바꾸면 privileged process의 path-based check가 다른 object로 유도될 수 있다.

safe temporary-file/update code는 symlink follow policy와 directory ownership을 명시한다. kernel의 `openat`/no-follow 계열 primitive처럼 lookup과 open을 가능한 한 하나의 trusted directory context 안에서 수행해 TOCTOU surface를 줄인다.

absolute target과 relative target은 symlink가 위치한 directory에 따라 resolution이 달라지므로 canonical string 비교만으로 containment를 증명하기 어렵다. symlink loop와 resolution-depth 제한도 failure mode다. 공격 테스트에서는 check 직후 link target을 반복 교체하고, 실제 open된 inode가 허용 root 안에 있는지 descriptor 기반으로 검증한다. log에는 입력 path, resolution constraint, 최종 object identity를 함께 남겨 string validation과 실제 사용 object가 일치했는지 확인한다.

---

## CHAPTER 07 · hard link와 unlink는 name count와 open reference를 분리한다

hard link는 여러 directory entry가 같은 inode를 참조하게 한다. unlink는 name reference 하나를 제거하지만 open descriptor가 있으면 object가 즉시 제거되지 않을 수 있다. 따라서 `rm` 직후 disk usage가 줄지 않는 현상은 deleted-but-open file로 설명될 수 있다.

log rotation이나 temporary-file pattern에서는 이 lifetime 차이가 중요하다. long-running process가 old log inode를 계속 열고 있으면 새 pathname과 다른 object에 쓰고 있을 수 있다. descriptor target을 확인한다.

hard link 때문에 두 pathname이 독립 파일처럼 보여도 metadata와 content mutation은 같은 inode에 적용된다. security policy가 path prefix만 보고 ownership을 판단하면 다른 link를 통해 같은 object에 접근할 수 있는지 검토해야 한다. unlink 이후 공간 회수 시점은 link count와 open/mmap reference가 모두 사라지는 시점에 달릴 수 있다. rotation test에서는 writer fd의 inode, 새 pathname inode, link count, open process를 함께 기록해 stale writer를 빠르게 찾는다.

---

## CHAPTER 08 · open file description은 descriptor와 underlying I/O state 사이에 있다

process의 fd table entry는 kernel의 open file description을 참조할 수 있고 그 object가 file offset과 status flag를 보유한다. `dup`나 `fork`로 만들어진 descriptor가 같은 open description을 공유하면 offset 변화가 서로 영향을 줄 수 있다.

`두 fd니까 독립적`이라는 가정은 틀릴 수 있다. concurrent sequential read/write가 offset 공유에 의존하면 atomicity 규칙을 확인한다. independent offset이 필요하면 separate open 또는 positional I/O를 사용한다.

fd 번호는 process-local slot일 뿐이고 close 뒤 재사용될 수 있다. 비동기 request가 숫자 fd만 오래 보관하면 late completion이 새 object에 적용되는 ABA 문제가 생길 수 있으므로 request generation과 handle ownership을 함께 관리한다. `fork` 뒤 parent/child가 같은 open description의 offset을 공유하는지, status flag 변경이 어느 descriptor에 보이는지 실제 API semantics를 확인한다. incident에서는 fd 숫자보다 inode/open description 관계를 추적한다.

---

## CHAPTER 09 · append는 application의 seek+write 조합보다 강한 atomicity를 요구한다

여러 writer가 `seek(end)` 후 write하면 두 process가 같은 end offset을 보고 overwrite할 수 있다. append flag는 filesystem이 각 write의 position selection을 operation과 결합해 처리하도록 한다.

그러나 한 write call 내부 data가 다른 writer와 어느 크기까지 atomic하게 유지되는지는 filesystem/API contract를 확인해야 한다. log record framing을 append 하나에 의존할지, record checksum/sequence를 추가할지 workload 요구로 결정한다.

user-space buffered writer가 한 logical log record를 여러 `write()`로 분할하면 append offset 선택은 각각 원자적이어도 다른 writer의 bytes가 중간에 끼어들 수 있다. 따라서 record boundary가 중요하면 한 syscall 크기, framing length, checksum, sequence를 함께 설계한다. network/filesystem 구현에서는 append semantics가 local filesystem과 다를 수 있으므로 concurrency stress test에서 여러 writer의 record corruption과 ordering을 실제로 확인한다.

---

## CHAPTER 10 · block allocation은 logical file offset을 physical storage extent로 바꾼다

filesystem은 file offset range를 device block/extents에 mapping한다. extent 기반 allocation은 연속 block range를 압축해 metadata를 줄일 수 있지만 fragmentation과 free-space layout에 따라 file가 여러 extent로 나뉜다.

write performance는 logical sequential access만으로 결정되지 않는다. allocation locality, filesystem free-space state, device FTL이 영향을 준다. long-running system에서는 fresh filesystem benchmark와 aged/fragmented state를 구분한다.

extent map을 보면 logically sequential file이 실제로 얼마나 분절됐는지 확인할 수 있다. delayed allocation과 preallocation 때문에 write 시점과 block assignment 시점이 다를 수 있으므로 benchmark에서는 allocation 상태를 고정한다. large-file append가 시간이 지나며 느려지면 free-space geometry, extent count, metadata update 비용을 함께 본다. SSD에서는 filesystem extent와 FTL physical placement가 다시 분리되므로 host-level contiguous extent를 곧 NAND sequentiality로 해석하지 않는다.

---

## CHAPTER 11 · sparse file은 logical size와 allocated blocks를 다르게 만든다

seek로 멀리 이동한 뒤 일부 data만 쓰면 중간 hole을 physical block 없이 표현할 수 있는 filesystem이 있다. file size는 크지만 실제 allocated space는 작을 수 있다. hole read는 zero처럼 보일 수 있다.

backup/copy tool이 sparse semantics를 보존하지 않으면 hole을 실제 zero block으로 materialize해 storage 사용량이 폭증한다. disk usage 분석에서 logical size와 allocated block count를 별도 확인한다.

hole punching이나 reflink/copy 방식이 섞이면 logical zero와 실제 allocated zero-filled extent를 구분해야 한다. quota와 backup 비용이 logical size 기준인지 allocated blocks 기준인지 system별 정책도 확인한다. sparse DB/image를 복사한 뒤 destination 사용량이 급증했다면 source/destination의 extent와 `st_blocks`류 값을 비교한다. checksum은 logical byte stream을 검증하고 allocation layout은 별도 evidence로 관리하면 data correctness와 space efficiency를 분리할 수 있다.

---

## CHAPTER 12 · delayed allocation은 block 선택을 뒤로 미뤄 더 나은 layout을 만들 수 있다

write가 page cache에 들어온 즉시 physical block을 정하지 않고 writeback 시점까지 allocation을 미루면 여러 small write를 더 큰 extent로 결합할 수 있다. 반면 free-space exhaustion 같은 failure가 application write 반환 뒤 늦게 드러날 수 있다.

`write()`가 성공했으니 disk space 확보가 끝났다고 가정하지 않는다. fallocate/preallocation이 필요한 workload와 crash/disk-full behavior를 테스트한다.

late ENOSPC가 발생하면 application은 이미 성공 처리한 logical operation을 durable하지 못할 수 있다. 중요 파일은 commit 전에 필요한 capacity를 reserve하거나 preallocation 실패를 즉시 surface하도록 설계한다. writeback trace에서 dirty page가 언제 block allocation을 받고 어느 errno로 실패했는지 확인하면 user write success와 persistence failure 사이의 gap을 증명할 수 있다. quota와 reserved-block 정책도 free-byte metric과 별개로 allocation 가능성을 제한할 수 있다.

---

## CHAPTER 13 · page cache는 file I/O와 virtual memory를 연결하는 shared cache다

normal buffered read/write와 file-backed mmap은 kernel page cache의 같은 page를 공유할 수 있다. 따라서 두 path를 통해 data를 접근할 때 별도 cache가 있다고 가정하면 consistency를 오해한다.

page cache hit는 storage I/O를 피하지만 memory pressure 시 reclaim될 수 있다. benchmark에서 반복 read가 빨라진 이유가 application optimization인지 cache warm state인지 분리한다. drop-cache 실험은 production behavior를 대표하지 않을 수 있으므로 workload-specific hit ratio를 측정한다.

mmap writer와 buffered reader가 같은 page를 공유하더라도 visibility와 durability는 별도 문제다. memory에 수정된 page가 보이는 것과 storage에 writeback되어 power loss를 견디는 것은 다르다. major/minor fault, readahead, page-cache residency와 block I/O를 함께 관찰하면 cache miss가 실제 storage read로 이어졌는지 알 수 있다. performance test는 warm/cold뿐 아니라 realistic working-set pressure에서 hit ratio가 유지되는지도 측정한다.

---

## CHAPTER 14 · writeback은 dirty page를 storage로 내보내는 비동기 정책이다

buffered write 후 dirty page는 background writeback에 의해 storage로 내려갈 수 있다. dirty ratio, memory pressure, explicit sync가 writeback 시점을 바꾼다. 많은 dirty data가 한번에 flush되면 latency spike와 I/O queue saturation이 생길 수 있다.

writeback pressure는 writer thread를 throttle할 수 있으므로 application stack에서 `write`가 느려져도 device operation 하나의 latency만이 원인은 아니다. dirty-page metric과 block I/O queue를 함께 본다.

writeback이 특정 inode에 오래 걸리면 global dirty budget을 소진해 unrelated writer까지 throttle할 수 있다. dirty bytes, writeback bytes, per-device queue, writeback worker activity를 same timeline에 놓고 latency spike가 dirty threshold crossing과 맞물리는지 본다. memory cgroup이나 filesystem별 accounting이 있다면 host average보다 해당 workload scope를 확인한다. flush interval을 조정한 뒤에는 평균 throughput뿐 아니라 p99 write/fsync와 foreground read latency가 어떻게 바뀌는지 검증한다.

---

## CHAPTER 15 · crash consistency는 data와 metadata write ordering을 함께 설계한다

file contents, inode size, block allocation bitmap, directory entry가 서로 다른 write로 storage에 반영될 수 있다. crash가 중간에 발생하면 일부만 persistent해져 filesystem invariant가 깨질 수 있다.

filesystem은 journaling, copy-on-write, ordered write 같은 전략으로 metadata/data ordering을 제어한다. application transaction은 filesystem이 보장하는 atomicity 범위를 알아야 한다. `rename은 atomic` 같은 문장도 동일 filesystem namespace와 crash durability를 분리해 해석한다.

crash matrix는 logical operation의 각 persistence edge 뒤에서 전원을 끊는 방식으로 설계한다. old data, new data, inode size, directory entry가 허용된 조합 중 하나로만 복구되는지 검사하고 torn/zeroed state가 나오면 protocol이 부족한 것이다. process kill은 page cache와 device volatile cache를 남길 수 있어 power-loss semantics와 다르다. filesystem mount mode와 storage flush guarantee를 기록해 recovery result를 해석한다.

---

## CHAPTER 16 · journaling은 recovery에 필요한 state transition을 log로 보호한다

metadata journaling에서는 metadata update intent를 journal에 기록하고 commit marker가 durable한 뒤 home location에 checkpoint할 수 있다. crash recovery는 incomplete transaction을 무시하고 committed transaction을 replay한다.

journal이 application data 전체의 transaction을 보장하는 것은 아니다. data=ordered/writeback/journal mode처럼 policy에 따라 data ordering이 다를 수 있다. database WAL과 filesystem journal을 동일한 layer로 생각하지 않는다.

journal capacity가 압박되면 transaction checkpoint를 기다리며 foreground metadata operation이 stall할 수 있다. recovery time도 pending committed transaction 수와 journal scan에 영향을 받을 수 있다. filesystem error가 journal abort로 이어지면 이후 mount가 read-only로 전환되는 등 failure semantics가 바뀔 수 있으므로 kernel log의 journal state와 mount state를 함께 본다. application DB WAL은 파일 내용의 logical transaction을, filesystem journal은 filesystem metadata invariant를 보호한다는 계층 차이를 유지한다.

---

## CHAPTER 17 · fsync는 file의 required dirty state를 durability boundary로 밀어낸다

`fsync(fd)`의 정확한 보장은 OS/filesystem/device contract에 따르지만 일반적으로 file data와 필요한 metadata를 stable storage로 flush하기 위한 primitive다. user-space buffer flush나 close만으로 같은 보장을 얻는다고 가정하지 않는다.

fsync latency는 batching과 group commit 대상이 될 수 있다. transaction마다 개별 fsync를 하면 durability는 명확하지만 throughput이 제한될 수 있다. DB engine은 WAL/group commit으로 여러 transaction의 durability cost를 amortize한다.

`fdatasync`처럼 metadata 범위를 줄이는 API와 file `fsync`, directory `fsync`는 목적이 다르다. 완료 시각을 syscall latency로 기록하고 block flush/FUA event와 연결하면 storage queue에서 지연됐는지 filesystem writeback을 기다렸는지 좁힐 수 있다. fsync를 제거해 benchmark가 빨라졌다면 durability requirement가 바뀐 것이지 같은 작업이 최적화된 것이 아니다. crash test로 commit 응답 뒤 데이터가 실제로 복구되는지 확인한다.

---

## CHAPTER 18 · 새 pathname durability에는 containing directory sync가 필요할 수 있다

새 file을 만들고 contents를 fsync한 뒤 crash했을 때 directory entry 자체가 durable하지 않으면 pathname이 복구되지 않을 수 있다. crash-safe replace pattern은 temporary file과 containing directory metadata의 durability를 함께 고려한다.

platform/filesystem별 보장을 확인하고 실제 power-failure test 또는 fault-injection을 사용한다. unit test에서 process kill만 하는 것은 storage cache와 power-loss ordering을 충분히 재현하지 못한다.

새 file 생성과 rename은 containing directory의 metadata를 변경하므로 file fd만 sync해도 namespace change가 commit됐다는 보장이 없을 수 있다. temp file과 target이 같은 filesystem에 있는지, directory sync가 지원/의미 있는지 platform contract를 확인한다. fault injection을 create→file sync→rename→directory sync 각 경계에 넣어 crash 후 old 또는 new complete version만 보이는지 검사한다. recovery code가 orphan temp를 어떻게 정리하는지도 protocol의 일부다.

---

## CHAPTER 19 · rename은 namespace atomicity와 content durability가 다른 성질이다

동일 filesystem 내 rename이 observer에게 old/new name 중 하나로 보이는 atomic namespace update를 제공할 수 있어도 renamed file contents가 stable storage에 도달했다는 의미는 아니다. replace pattern에서 temp contents fsync→rename→directory fsync 순서가 중요한 이유다.

rename 대상이 이미 존재할 때 overwrite semantics, exchange/no-replace option은 API마다 다르다. concurrent updater가 있으면 expected generation과 compare semantics를 추가해 last-writer-wins를 의도적으로 선택한다.

동시 reader가 open한 old inode는 rename 뒤에도 계속 읽을 수 있으므로 namespace 전환 시점과 open-handle lifetime을 분리한다. two-writer replace에서는 둘 다 완전한 temp를 만들더라도 마지막 rename이 승리할 수 있으므로 version/expected hash를 비교해 의도하지 않은 overwrite를 막는다. cross-directory rename이면 두 directory metadata durability를 고려해야 하는지 filesystem contract를 확인한다. rename 성공 로그와 file/directory sync 완료를 별도 event로 남긴다.

---

## CHAPTER 20 · cross-filesystem move는 rename이 아니라 copy+delete가 될 수 있다

서로 다른 mount/filesystem 사이에서는 inode/object ownership이 다르므로 atomic rename이 불가능할 수 있다. high-level file move API가 copy→fsync?→delete로 fallback하면 중간 failure에서 source와 destination이 모두 존재하거나 partial destination이 남을 수 있다.

operation contract가 atomic move를 요구한다면 동일 filesystem staging area를 사용한다. cross-device backup/migration은 resumable copy와 checksum, commit marker를 별도로 설계한다.

copy completion은 bytes가 destination page cache에 들어온 것과 durable commit을 분리한다. migration protocol은 destination temp 생성, chunk checksum/length, file sync, destination namespace commit, source delete 순서를 명시하고 crash 후 재시작이 어느 단계부터 안전한지 기록한다. source 삭제 전에 destination identity와 checksum을 검증하면 partial copy로 인한 data loss를 막을 수 있다. high-level move API가 EXDEV에서 어떤 fallback을 하는지 문서와 실제 trace로 확인한다.

---

## CHAPTER 21 · file lock은 advisory/mandatory와 process/thread semantics를 확인한다

POSIX advisory lock은 cooperating process가 lock protocol을 따를 때만 보호된다. 다른 process가 lock 없이 write하면 kernel이 항상 막아 주는 것은 아니다. `flock`과 record lock의 ownership/dup/fork semantics도 다를 수 있다.

DB/file-based coordination에서 lock file 존재 자체를 lock으로 사용하면 stale file problem이 생긴다. kernel-managed lock 또는 atomic create + owner identity/lease를 사용한다. network filesystem에서는 lock implementation과 failure semantics를 별도 검증한다.

lock lifetime이 fd close, process exit, fork/dup 중 어느 사건에 연결되는지 API마다 다르므로 generic “file lock”으로 추상화하지 않는다. record lock은 byte range가 겹치는지까지 고려해야 한다. distributed/network filesystem에서는 server failover나 lease expiration 뒤 두 client가 동시에 lock을 가진 것으로 믿는 split-brain 가능성을 검토한다. contention trace에는 lock owner PID, file identity, range, wait duration을 남겨 stale lock file과 kernel lock을 구분한다.

---

## CHAPTER 22 · mmap된 file을 truncate하면 mapping과 file size의 관계가 깨질 수 있다

process가 file page를 mapping한 상태에서 다른 process가 file을 줄이면 기존 mapping의 일부 address가 더 이상 valid backing을 갖지 못할 수 있다. 이후 access가 signal/fault로 이어질 수 있다.

shared mmap protocol은 file resize를 concurrent하게 허용할지 명시해야 한다. generation/version mapping을 사용하거나 writer가 새 file을 만들고 atomic rename으로 교체해 reader mapping lifetime을 분리하는 방법이 있다.

reader가 open/mmap한 inode는 pathname이 새 version으로 rename돼도 기존 mapping을 계속 볼 수 있으므로 version swap과 mapping lifetime이 자연스럽게 분리된다. 반대로 in-place truncate/extend를 허용하면 reader는 size generation을 동기화해야 한다. fault가 발생했을 때 address, mapping offset, 당시 file size와 inode generation을 기록하면 random SIGBUS류 증상을 resize race와 연결할 수 있다. stress test에서 reader faulting과 writer truncate를 의도적으로 겹친다.

---

## CHAPTER 23 · filesystem benchmark는 page cache와 device cache를 구분해야 한다

첫 read와 두 번째 read latency 차이는 filesystem code보다 page cache warmup 때문일 수 있다. write benchmark도 buffered write 반환만 재면 device durability throughput을 측정하지 못한다.

benchmark 목표가 cached read인지 cold storage read인지, synchronous durability인지 async throughput인지 명시한다. dataset이 RAM보다 큰지, fsync를 포함하는지, device queue depth와 filesystem age를 기록한다.

cold benchmark를 만들기 위해 무조건 global cache를 drop하면 production과 다른 system-wide disturbance가 생길 수 있다. dataset working set, readahead, direct-I/O 사용 여부를 목적에 맞게 고정한다. write throughput은 submitted bytes와 durable bytes/sec를 구분하고 `fsync` cadence를 명시한다. SSD는 fresh/empty 상태와 steady-state GC 상태가 다르므로 preconditioning과 thermal 상태도 기록한다. 결과에는 mount option, filesystem age/fragmentation까지 포함한다.

---

## CHAPTER 24 · dentry/inode cache는 namespace lookup 자체를 cache한다

file data만 cache되는 것이 아니다. recently resolved pathname component와 inode metadata도 memory에 cache될 수 있다. 수백만 tiny file workload에서는 data보다 metadata lookup이 병목이 될 수 있다.

negative dentry처럼 `이 이름이 없음` 결과도 cache될 수 있다. external filesystem change나 network-backed namespace에서는 cache invalidation semantics가 중요하다. lookup benchmark는 warm/cold metadata state를 구분한다.

namespace-heavy application은 data bytes가 거의 없어도 path component 수와 metadata cache pressure 때문에 CPU/reclaim 비용이 커질 수 있다. cache hit/miss와 lookup syscall latency, directory size를 함께 측정한다. remote filesystem에서는 attribute/dentry cache TTL 때문에 다른 client의 create/delete가 즉시 보이지 않을 수 있어 consistency 요구와 cache policy를 맞춰야 한다. negative lookup storm이 workload인지 attack인지도 request key cardinality와 함께 본다.

---

## CHAPTER 25 · free-space allocator는 fragmentation과 allocation latency를 결정한다

filesystem은 bitmap, tree, group/extent allocator를 사용해 free block을 추적한다. free space percentage가 충분해도 큰 contiguous extent가 부족하면 allocation이 fragmented되거나 metadata work가 증가할 수 있다.

write-heavy long-lived volume은 delete/create history 때문에 free-space geometry가 달라진다. capacity alarm을 byte percentage 하나로 두지 않고 inode/metadata reserve와 allocation failure도 모니터링한다.

large preallocation이 실패하거나 느려질 때 total free bytes와 largest/typical free extent는 다른 정보다. aged-volume test에서 extent count, allocation latency, metadata CPU를 fresh volume과 비교한다. filesystem이 locality group이나 block group을 사용한다면 특정 group만 포화되어 global free-space 수치가 여유로워도 hot directory/file에 allocation pressure가 생길 수 있다. cleanup 정책은 단순 percentage보다 allocation failure headroom과 compaction/trim 비용까지 본다.

---

## CHAPTER 26 · inode/resource exhaustion은 byte 공간이 남아도 file creation을 막을 수 있다

일부 filesystem은 inode 수나 metadata capacity가 별도 제약이다. tiny file가 매우 많으면 data blocks는 남아 있어도 새 inode를 만들 수 없을 수 있다.

application cache가 object마다 파일 하나를 생성한다면 object count가 storage model의 주요 capacity metric이다. directory entry/inode overhead와 backup/scan cost까지 포함해 blob store/DB 사용 여부를 선택한다.

`ENOSPC`를 받았을 때 free byte만 확인하고 이상하다고 판단하지 않는다. inode/free object counter, quota, reserved metadata, directory entry limit을 함께 확인한다. tiny-file workload는 fsck/backup/startup scan 시간도 object count에 비례해 운영비용을 늘릴 수 있다. cache eviction 정책은 byte budget과 file-count budget을 동시에 관리하고, creation rate와 deletion rate가 장기적으로 균형을 이루는지 metric으로 감시한다.

---

## CHAPTER 27 · filesystem corruption은 recovery tool 실행 전에 failure evidence를 보존한다

metadata checksum/error, I/O error, journal recovery failure가 보이면 즉시 repair를 반복 실행하기보다 block-device health와 read-only snapshot/image를 가능한 범위에서 보존한다. repair tool이 corrupted metadata를 수정해 원본 증거를 잃게 할 수 있다.

corruption 원인은 filesystem software bug뿐 아니라 storage media, controller, power-loss guarantee violation, memory corruption에서 올 수 있다. kernel log와 device SMART/health, power event를 함께 조사한다.

repair 실행 전 mount 상태, last journal sequence, failing block/LBA, checksum error, device firmware/health를 캡처하면 계층별 원인을 비교할 수 있다. 가능하면 write를 중단하고 image/snapshot에서 분석해 원본을 보존한다. repair tool의 version과 수행한 mutation도 기록해 이후 남은 증상이 원래 corruption인지 repair 결과인지 구분한다. ECC/memory error가 같은 시각에 있었다면 on-disk corruption이 storage device 밖에서 생성됐을 가능성도 열어 둔다.

---

## CHAPTER 28 · Android storage는 app sandbox와 database durability 위에서 해석한다

internal app storage는 UID/SELinux boundary와 연결되고 shared/media storage는 다른 access model을 가진다. pathname permission만으로 Android storage access를 설명할 수 없다.

SQLite database file은 filesystem 위에 존재하지만 transaction atomicity와 WAL/journal protocol을 DB engine이 추가한다. application이 DB file을 직접 copy/modify하면 engine lock/journal invariant를 깨뜨릴 수 있다. backup은 engine-supported snapshot/export semantics를 사용한다.

scoped/shared storage에서는 content URI와 platform permission/AppOps가 실제 access boundary가 될 수 있으므로 raw path 존재 여부만으로 read/write 가능성을 판단하지 않는다. app internal DB backup은 main DB뿐 아니라 WAL/SHM state와 checkpoint timing을 고려해야 한다. process가 열린 DB를 file-copy하는 테스트가 우연히 성공해도 crash-consistent snapshot이 보장되는 것은 아니다. restore 후 schema/version과 transaction checksum을 검증해 filesystem copy와 DB logical consistency를 분리한다.

---

## CHAPTER 29 · storage incident는 namespace→cache→filesystem→block→device 순서로 좁힌다

`파일이 사라졌다`는 증상에서 먼저 pathname/rename/unlink history와 open descriptor를 확인한다. `저장했는데 복구 후 없어졌다`면 fsync/directory durability와 crash timeline을 본다. `write가 느리다`면 dirty throttling과 block-device queue를 본다.

각 layer의 metric과 log를 같은 timestamp로 연결한다. application error만 남기면 ENOSPC, EIO, permission, stale mount를 구분하기 어렵다. raw errno와 target filesystem/device identity를 보존한다.

request ID와 file identity를 syscall trace, filesystem event, block request까지 연결하면 어느 layer에서 residence time이나 오류가 처음 생겼는지 찾을 수 있다. namespace 문제라면 block I/O가 아예 없을 수 있고, page-cache hit라면 device latency와 무관할 수 있다. EIO가 보이면 filesystem가 어떤 logical file offset을 어떤 block에 mapping했는지와 device health를 연결한다. evidence가 없는 layer를 추측으로 채우지 않고 다음 계측을 추가한다.

---

## CHAPTER 30 · file update protocol은 object identity, atomicity, durability를 명시해야 한다

중요 file을 안전하게 갱신하려면 최소한 **어떤 이름이 current version을 가리키는가, partial contents가 노출될 수 있는가, commit point가 어디인가, crash 후 어느 version으로 복구되는가**를 정의한다.

일반적인 temp-write→fsync→atomic rename→directory fsync 패턴도 모든 filesystem/network storage에서 동일하게 보장된다고 가정하지 않는다. target platform의 documented semantics와 fault-injection으로 검증한다. 파일 API의 목적은 bytes를 쓰는 것이 아니라 **crash를 포함한 모든 중간 상태에서 namespace와 data invariant를 유지하는 것**이다.

protocol에는 concurrent writer와 recovery owner도 포함한다. temp filename에 generation/transaction ID를 넣고 expected-current version을 확인하면 두 updater의 silent overwrite를 막을 수 있다. startup recovery가 orphan temp를 삭제하기 전에 어떤 것이 committed인지 directory entry와 durable marker로 판별해야 한다. disk-full, permission change, fsync error, power loss를 각 단계에 주입하고 old complete 또는 new complete 외 상태가 노출되지 않는지 검증하면 API 조합이 아니라 실제 durability contract를 증명할 수 있다.