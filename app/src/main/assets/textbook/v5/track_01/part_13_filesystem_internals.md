# PART 13 · 파일은 이름 붙은 바이트 배열보다 훨씬 복잡하다 — filesystem 내부 구조

애플리케이션은 `open("a.txt")`, `write`, `rename` 같은 API를 사용하지만 kernel은 path 문자열을 namespace object로 해석하고, metadata와 data block을 찾고, cache와 journal을 관리하고, crash 뒤 일관성을 복구해야 한다.

이 PART의 목표는 filesystem command를 외우는 것이 아니라 **path → dentry/inode → page cache → block allocation → journal/writeback → persistent media**의 흐름을 이해하는 것이다.

---

## CHAPTER 01 · path는 파일 object 자체가 아니다

`/home/app/data.db`라는 문자열은 filesystem namespace에서 object를 찾기 위한 경로다.

```text
/
↓ lookup home
/home
↓ lookup app
/home/app
↓ lookup data.db
file object
```

각 component lookup에서 directory permission, mount point, symlink 등이 영향을 준다.

### 같은 file에 여러 path가 있을 수 있다

hard link를 허용하는 filesystem에서는 서로 다른 directory entry가 같은 underlying inode/object를 가리킬 수 있다.

따라서:

```text
path identity
≠ file object identity
```

이다.

---

## CHAPTER 02 · inode는 파일 이름이 아니라 metadata object다

전형적인 Unix filesystem에서 inode는 다음 종류의 정보를 가진다.

```text
file type
permission/mode
owner/group
size
timestamps
link count
data block mapping
```

파일 이름은 directory entry 쪽에 존재한다.

### rename이 file data 전체를 복사할 필요가 없는 이유

같은 filesystem 안에서 rename은 directory namespace entry를 바꾸는 작업으로 구현될 수 있다.

수 GB file을 rename해도 data block 전체를 복사할 필요가 없기 때문에 매우 빠를 수 있다.

---

## CHAPTER 03 · directory는 `폴더 UI`가 아니라 name→inode mapping을 저장하는 filesystem object다

directory entry는 대략:

```text
name → inode number/object
```

mapping을 가진다.

directory가 크면 lookup 성능을 위해 hash/tree 구조를 사용할 수 있다.

구체 구조는 ext4, XFS, F2FS 등 filesystem마다 다르다.

### directory permission

read와 execute/search permission의 의미가 일반 file과 다르다.

path traversal에 search permission이 필요한 이유다.

---

## CHAPTER 04 · VFS는 filesystem별 구현 위에 공통 API를 제공한다

Linux Virtual File System 계층은 ext4, tmpfs, procfs 등 서로 다른 filesystem을 `open/read/write/stat` 같은 공통 interface로 다룰 수 있게 한다.

```text
application syscall
↓
VFS
├─ ext4 implementation
├─ tmpfs implementation
├─ procfs implementation
└─ other filesystem
```

`/proc/cpuinfo`도 path로 읽을 수 있지만 SSD에 저장된 일반 file과 같은 backing을 가진다는 뜻은 아니다.

---

## CHAPTER 05 · mount는 filesystem tree를 하나의 namespace에 연결한다

filesystem instance를 특정 directory path에 mount하면 그 위치 아래 namespace가 새 filesystem root로 연결된다.

```text
root fs /
└─ /data  ← another filesystem mounted
```

### path는 mount namespace에 따라 다르게 보일 수 있다

container/process가 다른 mount namespace에 있으면 같은 `/data` 문자열이 서로 다른 mount를 가리킬 수 있다.

PART 07 namespace와 연결된다.

---

## CHAPTER 06 · symlink는 path resolution에 추가 lookup을 만든다

symbolic link는 다른 path를 가리키는 filesystem object다.

```text
/current → /versions/v5
```

path resolver는 symlink target을 다시 해석한다.

### symlink loop

```text
a → b
b → a
```

같은 loop를 막기 위해 resolver는 traversal limit을 둔다.

### security

privileged program이 attacker-controlled directory에서 symlink를 따라가면 TOCTOU/path traversal 문제가 생길 수 있다.

---

## CHAPTER 07 · hard link와 unlink를 link count로 이해한다

두 이름이 같은 inode를 가리킬 수 있다.

```text
name A ─┐
        ├→ inode 42 → data blocks
name B ─┘
```

`unlink(A)`는 이름 A의 directory entry와 link count를 줄일 뿐 B가 남아 있으면 file data는 계속 존재한다.

### 열린 file을 unlink

process가 file descriptor로 file을 열고 있는 동안 path를 unlink해도 open reference가 남아 있으면 data object가 즉시 사라지지 않을 수 있다.

Unix log rotation에서 이 특성이 중요하다.

---

## CHAPTER 08 · file descriptor와 open file description

`open()`이 반환하는 fd는 process-local 작은 정수다.

kernel 내부에는 current offset, flags 등을 가진 open file state가 따로 있을 수 있다.

```text
fd 3 ─┐
      ├→ open file description → inode
fd 7 ─┘
```

`dup`이나 fork 뒤 descriptor가 같은 offset state를 공유할 수 있는 이유다.

---

## CHAPTER 09 · file offset과 append race

두 writer가:

```text
seek end
write
```

를 각각 수행하면 사이에서 다른 writer가 끼어들 수 있다.

append mode가 제공하는 atomic append semantics를 사용하면 `현재 끝 찾기 + write`를 filesystem/kernel이 하나의 operation처럼 처리할 수 있다.

정확한 atomicity 범위는 API/filesystem을 확인해야 한다.

---

## CHAPTER 10 · data block allocation은 logical file offset을 storage block으로 연결한다

파일 크기가 커질 때 filesystem은 free block을 할당하고 file metadata에 mapping을 기록한다.

단순 direct block list가 아니라 extent 같은 연속 범위 표현을 사용할 수 있다.

```text
file logical 0..1MB
→ physical blocks X..Y
```

### fragmentation

file의 logical sequential data가 physical storage에서 여러 조각으로 흩어질 수 있다.

HDD에서 seek 비용, SSD에서도 mapping/metadata와 write pattern에 영향을 줄 수 있다.

---

## CHAPTER 11 · sparse file은 logical size와 allocated bytes가 다르다

큰 offset으로 seek한 뒤 조금 write하면 중간 hole을 실제 zero block으로 모두 할당하지 않을 수 있다.

```text
logical size: 10GB
allocated:    4KB
```

hole을 read하면 zero처럼 보일 수 있다.

따라서 `ls에서 보이는 size`와 실제 disk usage가 다를 수 있다.

---

## CHAPTER 12 · delayed allocation은 더 좋은 block 배치를 위해 write를 늦출 수 있다

application write 시점에 physical block을 즉시 확정하지 않고 page cache에 dirty data를 모아 나중에 큰 extent로 allocation할 수 있다.

장점:

```text
better contiguous allocation
fewer metadata updates
batching
```

하지만 crash consistency와 free-space pressure를 이해해야 한다.

---

## CHAPTER 13 · page cache는 file I/O와 memory mapping을 연결한다

buffered read/write와 mmap은 종종 같은 page cache의 file-backed page를 공유한다.

```text
read() path ─┐
             ├→ page cache → storage
mmap path ───┘
```

이 때문에 한 path의 write가 다른 mapping에서 보이는 visibility 규칙이 중요하다.

### direct I/O

일부 workload는 page cache를 우회하거나 다르게 사용하는 direct I/O를 선택할 수 있다.

alignment와 application-side caching 책임이 늘 수 있다.

---

## CHAPTER 14 · writeback은 dirty page를 storage로 내린다

application write가 page cache에서 성공한 뒤 kernel writeback thread/policy가 storage I/O를 수행할 수 있다.

```text
clean page
↓ modification
dirty page
↓ writeback
writeback/in-flight
↓ completion
clean page
```

### dirty throttling

dirty data가 너무 많이 쌓이면 writer를 throttle해 memory를 무한히 dirty cache로 쓰지 못하게 할 수 있다.

`갑자기 write latency가 튄다`는 현상이 writeback pressure와 연결될 수 있다.

---

## CHAPTER 15 · metadata와 data ordering이 crash consistency를 결정한다

새 file을 만든다고 하자.

필요한 변화:

```text
allocate inode
allocate data block
write file data
add directory entry
update allocation bitmap
```

crash가 중간에 나면 일부만 반영될 수 있다.

filesystem은 update ordering, journal, copy-on-write tree 등으로 복구 가능한 상태를 만든다.

---

## CHAPTER 16 · journaling은 `모든 데이터 두 번 쓰기` 하나로 설명할 수 없다

filesystem journal은 metadata update 또는 data까지 포함한 transaction record를 사용해 crash 뒤 consistency를 회복할 수 있다.

mode에 따라:

```text
metadata journaled
data ordered before metadata commit
full data journaling
```

등 정책이 다를 수 있다.

성능과 durability trade-off가 있다.

---

## CHAPTER 17 · journal commit과 application durability boundary는 다를 수 있다

filesystem이 자체 consistency를 지킨다는 것과 application이 방금 쓴 특정 file의 최신 bytes가 crash 뒤 반드시 남는다는 것은 다른 계약이다.

application은 필요하면 `fsync`/`fdatasync` 등으로 durability를 요청해야 한다.

### fsync cost

storage queue와 flush command가 완료될 때까지 기다리면 latency가 커질 수 있다.

DB commit latency에서 sync가 큰 비중을 차지할 수 있다.

---

## CHAPTER 18 · directory fsync가 필요한 파일 교체 패턴

새 config를 안전하게 교체한다고 하자.

```text
write temp
fsync(temp)
rename(temp, target)
fsync(parent directory)
```

왜 directory까지 sync하는가?

rename으로 바뀐 namespace metadata가 crash 뒤 durable해야 하기 때문이다.

정확한 filesystem/API contract에 맞춰 패턴을 사용한다.

---

## CHAPTER 19 · rename atomicity와 durability는 별개다

같은 filesystem 안에서 rename이 namespace 관점에서 atomic하더라도 crash 뒤 rename이 유지된다는 durability는 별도 sync ordering이 필요할 수 있다.

```text
atomic visibility
≠ persistent durability
```

이 구분은 PART 03에서 배운 transaction 성질과 같다.

---

## CHAPTER 20 · cross-filesystem rename은 복사+삭제가 될 수 있다

source와 target이 다른 mount/filesystem이면 atomic rename을 제공하지 못하고 `EXDEV` 같은 오류가 날 수 있다.

상위 library가 이를 숨기고 copy+unlink를 수행하면:

```text
큰 파일 → 오래 걸림
중간 failure → partial target 가능
permission/metadata 달라짐
```

이 생길 수 있다.

`move` UI 하나가 항상 O(1) metadata operation은 아니다.

---

## CHAPTER 21 · file locking은 모든 process가 자동으로 지켜 주는 물리적 잠금이 아닐 수 있다

advisory lock model에서는 협력하는 process들이 lock 규칙을 따라야 한다.

lock을 무시하고 직접 write하는 process를 kernel이 반드시 막는 것은 아니다.

### record locking

file 전체가 아니라 byte range를 lock할 수 있는 API도 있다.

하지만 network filesystem과 process/thread semantics가 복잡하므로 portable correctness가 필요하면 DB 같은 더 높은 수준 mechanism을 고려한다.

---

## CHAPTER 22 · mmap과 truncate의 조합은 위험할 수 있다

process A가 file을 mmap 중인데 process B가 file을 더 작은 크기로 truncate하면 A가 기존 mapping의 잘려나간 영역을 접근할 때 fault가 발생할 수 있다.

mapping lifetime과 file size mutation을 coordination해야 한다.

---

## CHAPTER 23 · filesystem cache를 benchmark에서 통제한다

첫 read:

```text
storage I/O 20ms
```

두 번째 read:

```text
page cache hit 0.3ms
```

둘을 섞어 평균내면 무엇을 측정한지 불분명하다.

### cold cache 강제의 위험

production은 보통 완전 cold가 아닐 수 있다.

benchmark 목적에 따라 cold/warm 두 조건을 따로 측정한다.

---

## CHAPTER 24 · inode/dentry cache도 path lookup 성능을 바꾼다

파일 내용만 cache되는 것이 아니다.

directory entry와 inode metadata도 memory cache에 남아 repeated stat/open path lookup을 빠르게 할 수 있다.

따라서 metadata-heavy benchmark에서도 warm/cold 상태를 고려한다.

---

## CHAPTER 25 · filesystem free space가 적으면 성능 특성이 바뀔 수 있다

free block이 적으면 allocator가 적합한 연속 extent를 찾기 어려워지고 fragmentation/GC가 증가할 수 있다.

flash filesystem/SSD까지 포함하면 lower-layer garbage collection과 겹칠 수 있다.

`10GB free니까 충분`보다 percentage, allocation pattern, filesystem recommendation을 본다.

---

## CHAPTER 26 · inode exhaustion은 disk byte가 남아도 파일 생성을 막을 수 있다

inode 수가 고정/제한된 filesystem에서는 작은 file을 매우 많이 만들면 data block free space는 남아도 inode resource가 고갈될 수 있다.

증상:

```text
No space left on device
but df shows bytes free
```

inode usage를 별도로 확인한다.

---

## CHAPTER 27 · filesystem corruption과 application corruption을 구분한다

application file 내용이 잘못됐다고 filesystem 자체가 corruption된 것은 아니다.

```text
application wrote invalid JSON
→ app-level corruption

filesystem metadata checksum/inode tree damage
→ fs-level corruption
```

복구 도구와 원인이 다르다.

### checksum

일부 filesystem은 metadata/data checksum을 사용해 corruption을 검출할 수 있다.

검출과 자동 복구는 같은 기능이 아니다.

---

## CHAPTER 28 · Android 앱 내부 storage도 filesystem 위에 있다

SQLite DB, SharedPreferences/Datastore file, image cache, APK code cache 모두 결국 filesystem/storage stack을 사용한다.

`Room transaction commit`도 더 아래에서는 page cache/filesystem/storage flush semantics와 연결된다.

고수준 API가 low-level complexity를 숨기지만 물리 법칙을 없애지는 않는다.

---

## CHAPTER 29 · file corruption 사고 조사

증상:

> crash 후 settings.json이 0 byte.

가설을 나눈다.

```text
old file truncate 후 write 전에 crash?
temp+rename pattern 사용?
write error 무시?
fsync 없음?
directory rename durability?
disk full?
concurrent writer?
wrong path cleanup?
```

### evidence

```text
file size/timestamps
filesystem free space
application write logs
crash timestamp
strace/system call trace 재현
fault injection
```

단순히 `저장 코드에 try-catch 추가`로 끝내지 않는다.

---

## CHAPTER 30 · filesystem invariant를 문장으로 쓴다

안전한 persistent file format 예:

```text
1. target은 항상 old-valid 또는 new-valid 중 하나다.
2. partial new content가 target name으로 노출되지 않는다.
3. checksum/version이 맞지 않으면 읽지 않는다.
4. crash 어느 지점에서도 다음 실행이 복구할 수 있다.
5. writer는 동시에 하나만 commit한다.
```

이 invariant가 구현을 이끈다.

---

## PART 13 종료 점검

1. path와 inode/file object가 왜 같은 것이 아닌가?
2. hard link가 같은 file에 여러 이름을 만들 수 있는 이유는 무엇인가?
3. VFS가 여러 filesystem을 어떤 방식으로 공통 API에 연결하는가?
4. sparse file에서 logical size와 allocated size가 왜 다른가?
5. delayed allocation이 성능과 crash semantics에 어떤 영향을 주는가?
6. page cache와 mmap이 어떻게 연결될 수 있는가?
7. dirty throttling이 write latency를 갑자기 높일 수 있는 이유는 무엇인가?
8. journaling이 filesystem consistency와 application durability를 자동으로 동일하게 만들지 않는 이유는 무엇인가?
9. rename atomicity와 durability는 무엇이 다른가?
10. directory fsync가 필요한 교체 패턴이 있는 이유는 무엇인가?
11. cross-filesystem move가 큰 copy operation이 될 수 있는 이유는 무엇인가?
12. inode exhaustion이 free bytes와 별개인 이유는 무엇인가?
13. application-level corruption과 filesystem corruption을 어떻게 구분하는가?
14. persistent file update에 invariant를 먼저 써야 하는 이유는 무엇인가?

이제 파일을 `이름+내용`으로만 보지 않는다. **namespace → inode → cache → allocation → journal/writeback → storage → crash recovery**를 한 경로로 추적할 수 있어야 한다.