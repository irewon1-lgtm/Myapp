# PART 03 · 메모리는 한 덩어리가 아니고, 저장은 한 번의 동작이 아니다

`RAM은 작업대, SSD는 창고`라는 비유는 처음 역할을 구분하는 데는 유용하지만 실제 성능·메모리 부족·데이터 유실을 설명하기에는 너무 거칠다. 프로그램이 한 주소를 읽을 때 CPU cache와 주소 변환이 관여할 수 있고, 메모리를 할당했다고 해서 즉시 물리 RAM 한 덩어리가 고정되는 것도 아니며, 파일에 write했다고 해서 전원이 꺼져도 남는 단계까지 반드시 끝났다는 뜻도 아니다.

이 PART에서는 **속도 계층, 주소 변환, 메모리 수명, 파일 cache, durability**를 하나의 데이터 이동 경로로 연결한다.

---

## CHAPTER 01 · 메모리 계층은 용량과 지연시간의 trade-off다

### CPU와 main memory 사이에는 속도 간격이 있다

CPU가 arithmetic instruction을 수행하는 속도에 비해 main memory에서 데이터를 가져오는 데는 훨씬 긴 시간이 걸릴 수 있다. 프로세서 설계는 이 간격을 그대로 두지 않고 작은 고속 cache를 여러 단계 둔다.

전형적인 개념 그림은 다음과 같다.

```text
CPU register
↓
L1 cache
↓
L2 cache
↓
last-level cache
↓
DRAM
↓
SSD / persistent storage
```

정확한 크기·지연시간·공유 방식은 CPU와 시스템마다 다르다. 이 그림에서 외워야 할 숫자는 없다. 중요한 것은 **가까운 계층일수록 작고 빠르며, 멀수록 크고 느린 경향이 있기 때문에 프로그램의 접근 패턴이 실제 실행시간을 크게 바꾼다**는 점이다.

### cache는 변수 하나씩 가져오지 않고 보통 block 단위로 움직인다

CPU cache는 일반적으로 memory의 데이터를 cache line이라는 고정 크기 block 단위로 가져오고 관리한다. 프로그램이 배열 원소 하나를 읽었을 때 주변 원소가 같은 line에 함께 들어올 수 있다.

그래서 연속 배열을 순서대로 읽으면 이미 가져온 line 안의 다음 원소를 재사용할 가능성이 높다.

```text
arr[0]
arr[1]
arr[2]
arr[3]
...
```

반대로 큰 메모리 공간을 멀리 뛰어다니며 읽으면 매번 다른 line이 필요해 cache miss가 늘 수 있다.

`array는 linked list보다 빠르다`를 절대 법칙으로 외우는 것이 아니라, **각 연산의 알고리즘 비용과 memory locality를 함께 본다.** linked structure가 필요한 문제도 많고, array가 크게 이동·복사되는 비용도 있다. 어떤 구조가 좋은지는 실제 workload와 operation distribution에 달려 있다.

### spatial locality와 temporal locality를 구분한다

spatial locality는 가까운 주소를 함께 사용할 가능성이다.

temporal locality는 같은 데이터를 짧은 시간 안에 다시 사용할 가능성이다.

예를 들어 행 우선으로 저장된 2차원 배열을 행 방향으로 순회하는 것과 열 방향으로 순회하는 것은 계산식의 Big-O가 같아도 cache behavior가 달라질 수 있다.

```text
for each row:
    for each column:
        use matrix[row][column]
```

과

```text
for each column:
    for each row:
        use matrix[row][column]
```

이 둘의 실제 차이는 layout, element size, cache line, compiler optimization에 영향을 받는다. 핵심은 **메모리 접근 순서가 성능 입력값**이라는 것이다.

### cache miss는 모두 같은 비용이 아니다

L1에서 miss가 나도 L2에서 찾을 수 있고, 거기서도 없으면 더 낮은 계층으로 내려갈 수 있다. 그래서 profiler가 단순히 `cache miss count`만 보여 준다고 해서 원인이 끝나는 것이 아니다.

조사할 때는 가능한 경우 다음을 본다.

```text
어느 cache level인가?
miss rate인가 absolute count인가?
access pattern은 sequential인가 random인가?
working set이 cache capacity보다 큰가?
false sharing 같은 multi-core 효과가 있는가?
```

---

## CHAPTER 02 · virtual address를 physical address로 바꾸는 경로

### 프로그램은 virtual address를 사용한다

프로세스 안의 pointer가 가리키는 주소는 일반적으로 virtual address다. CPU와 operating system은 page 단위 mapping을 사용해 virtual page가 어떤 physical frame 또는 backing에 대응하는지 관리한다.

단순화한 변환은 다음과 같다.

```text
virtual address
= virtual page number + page offset

page table lookup
↓
physical frame number + same page offset
↓
physical address
```

실제 architecture의 page table은 여러 단계 구조일 수 있고 huge page 같은 변형도 있다. 이 단계에서 중요한 것은 `주소 변환 표가 있다`가 아니라 **프로세스가 보는 연속적인 주소 세계와 실제 물리 메모리 배치를 분리한다**는 점이다.

### page table을 매번 main memory에서 찾으면 너무 비싸다

주소를 읽을 때마다 page table을 여러 번 memory에서 읽어야 한다면 memory access 비용이 크게 늘어난다. CPU는 최근 address translation을 TLB, Translation Lookaside Buffer 같은 cache에 보관한다.

```text
virtual address
↓
TLB hit?
├─ yes → translation 빠르게 사용
└─ no  → page-table walk 필요
```

TLB도 크기가 제한된 cache다. 매우 큰 working set을 특정 stride로 순회하면 data cache뿐 아니라 translation cache behavior까지 성능에 영향을 줄 수 있다.

### page table entry에는 주소 외에 보호 정보도 있다

page mapping에는 read/write/execute 권한 같은 protection bit가 포함될 수 있다. 그래서 code page를 read+execute로 두고 write를 막거나, read-only mapping에 쓰려 하면 fault를 발생시키는 식의 격리가 가능하다.

`segmentation fault`를 단순히 `메모리가 부족하다`고 번역하면 틀릴 수 있다. invalid/unmapped address나 허용되지 않은 access 같은 memory protection violation이 원인일 수 있다.

---

## CHAPTER 03 · page fault는 무조건 오류가 아니다

### fault는 CPU가 운영체제 개입이 필요하다고 알리는 사건일 수 있다

virtual address access 중 현재 page table state만으로 처리를 완료할 수 없으면 CPU가 page fault exception을 발생시키고 kernel이 원인을 처리한다.

이것이 항상 crash라는 뜻은 아니다.

예를 들어 process가 아직 physical page를 실제로 할당받지 않은 anonymous virtual memory를 처음 touch했을 때 kernel이 새 page를 준비하고 mapping한 뒤 instruction을 다시 실행할 수 있다.

또 memory-mapped file의 page가 아직 RAM에 없다면 storage에서 읽어 온 뒤 mapping하고 실행을 재개할 수 있다.

반대로 주소 자체가 유효하지 않거나 권한 위반이면 kernel이 process에 signal/exception을 전달해 종료로 이어질 수 있다.

따라서:

```text
page fault 발생
=
프로그램 버그
```

는 아니다.

### minor fault와 major fault의 차이를 이해한다

운영체제 도구에서 minor/major page fault를 구분하는 경우가 있다. 용어의 세부 정의는 플랫폼 문서를 봐야 하지만 일반적으로 major fault는 필요한 page를 storage I/O와 연결해야 하는 더 비싼 경로를 포함할 수 있다.

성능 분석에서는 단순 fault count보다 **fault가 어떤 I/O와 지연을 유발했는지**가 중요하다.

### demand paging은 '예약'과 '실제 사용'을 분리한다

프로세스가 큰 virtual region을 확보했다고 해서 모든 page에 대응하는 물리 RAM이 즉시 채워지는 것은 아니다. OS는 실제 access 시점에 page를 준비하는 demand paging 전략을 사용할 수 있다.

그래서 memory profiler의 숫자에는 여러 개념이 나온다.

- virtual size
- resident set
- private/shared memory
- committed memory
- mapped file

이 숫자를 전부 `RAM 사용량`이라는 한 단어로 부르면 도구 결과를 잘못 읽는다.

---

## CHAPTER 04 · copy-on-write는 복사를 늦추는 대신 write 시점에 비용을 낸다

### 처음부터 모든 page를 복사하지 않는 전략

process 생성이나 snapshot 같은 상황에서 큰 memory를 바로 전부 복사하면 비싸다. copy-on-write, COW는 처음에는 같은 physical page를 read-only처럼 공유하고 실제로 어느 쪽이 write하려 할 때 그 page만 복사하는 전략이다.

```text
처음
process A ─┐
           ├─ shared physical page
process B ─┘

B가 write
↓
page copy
↓
A -> old page
B -> new private page
```

이 전략은 복사 비용을 실제 변경된 page로 제한할 수 있다.

### COW를 알면 '갑자기 memory가 늘었다'는 현상을 설명할 수 있다

처음에는 shared page가 많아 memory 사용량이 작게 보였는데, write가 대량 발생하면서 private copy가 만들어져 resident memory가 증가할 수 있다.

`process를 하나 더 만들었는데 처음에는 괜찮다가 workload가 시작되니 메모리가 확 늘었다`는 현상을 볼 때 COW가 조사 후보가 될 수 있다.

물론 실제 runtime/process 생성 방식이 COW를 쓰는지는 플랫폼에 따라 확인해야 한다.

---

## CHAPTER 05 · mmap은 파일을 주소 공간에 연결한다

### read/write만이 파일 접근 방법은 아니다

memory mapping을 사용하면 file content를 process virtual address space에 mapping하고 memory load/store처럼 접근할 수 있다.

개념적으로:

```text
file offset range
↕ mapping
virtual address range
```

필요한 page를 처음 접근할 때 page fault를 통해 file data가 memory에 들어올 수 있고, 이후 cache된 page를 재사용할 수 있다.

### mmap이 무조건 read보다 빠른 것은 아니다

mmap은 copy path와 API 구조를 단순화할 수 있지만 page fault pattern, random access, file size, OS cache, concurrency, durability 요구에 따라 결과가 달라진다.

`mmap = zero copy = 무조건 최고 성능` 같은 결론은 위험하다.

프로파일링해야 할 항목은 다음과 같다.

```text
access pattern
page fault
working-set size
memory pressure
writeback behavior
file growth/truncation
synchronization
```

### mapping lifetime과 file lifetime도 구분한다

file descriptor를 닫았다고 이미 만들어진 mapping이 즉시 사라지는지 여부 같은 세부 동작은 OS API 계약을 확인해야 한다. `파일 handle`, `virtual mapping`, `cached page`는 서로 다른 resource다.

이런 lifetime을 분리하는 습관이 resource leak과 stale mapping 문제를 찾는 데 도움이 된다.

---

## CHAPTER 06 · malloc/new가 곧바로 kernel에 page 하나를 요청하는 것은 아니다

### application allocator가 중간 계층에 있다

C의 `malloc`, C++의 `new`, 많은 language runtime의 object allocation은 매 객체마다 syscall로 kernel에 새 page를 요청하지 않는다. allocator/runtime이 큰 memory region을 확보한 뒤 작은 block으로 나누어 재사용할 수 있다.

그래서 allocation 경로를 다음처럼 나눠 본다.

```text
프로그램 객체 요청
↓
language/runtime allocator
↓
allocator가 이미 가진 free block?
├─ yes → 재사용
└─ no → OS에 더 큰 region 요청 가능
```

이 구조를 알면 `객체를 100만 개 만들었으니 syscall도 100만 번` 같은 잘못된 추정을 피할 수 있다.

### fragmentation에는 내부와 외부 문제가 있다

allocator가 관리하는 block 크기와 실제 요청 크기가 맞지 않아 block 내부 공간이 남는 것을 internal fragmentation이라고 설명할 수 있다.

free memory 총량은 충분하지만 작은 조각으로 흩어져 큰 연속 block을 만족하기 어려운 상황은 external fragmentation 문제와 연결된다.

allocator는 size class, arena, slab 같은 여러 전략으로 이 문제를 다룬다. 세부 구현은 runtime마다 다르다.

### free했다고 RSS가 즉시 줄지 않을 수 있다

프로그램이 allocator에 memory를 반환해도 allocator가 그 page를 향후 재사용하려고 보관할 수 있다. OS에 즉시 반환되지 않으면 process RSS가 바로 줄지 않을 수 있다.

따라서:

```text
객체 삭제 완료
그런데 OS에서 보이는 memory가 그대로
```

라는 관찰만으로 leak을 확정하지 않는다.

조사하려면:

```text
live object 수
allocator retained memory
resident pages
mapped regions
GC heap committed/used
```

을 구분해야 한다.

---

## CHAPTER 07 · garbage collection도 메모리 비용을 없애지 않는다

### GC의 문제는 'free를 안 써도 된다'로 끝나지 않는다

garbage collector가 있는 runtime은 programmer가 모든 object를 직접 free하지 않아도 된다. 하지만 collector는 어떤 object가 더 이상 도달 가능하지 않은지 판단하고 memory를 회수하는 일을 해야 한다.

이 과정에는 CPU 시간, metadata, stop-the-world pause 또는 concurrent collector overhead 같은 비용이 있을 수 있다.

### reachable과 useful은 다르다

다음 cache가 있다고 하자.

```text
globalCache[key] = hugeObject
```

key를 영원히 제거하지 않으면 hugeObject는 program logic상 다시 쓸 일이 없더라도 globalCache에서 reference가 남아 있다.

GC 입장에서는 reachable이므로 회수하면 안 된다.

이 문제를 이해하려면 memory leak을 다음처럼 넓게 정의한다.

```text
프로그램이 더 이상 필요로 하지 않는 자원이
의도치 않은 reference/lifetime 때문에 계속 유지된다.
```

### allocation rate도 중요하다

live heap 크기가 작아도 초당 매우 많은 temporary object를 만들면 collector가 자주 일해야 할 수 있다. 그래서 memory performance를 볼 때 peak heap만 보지 않는다.

```text
live size
allocation rate
promotion/survivor behavior
collection frequency
pause distribution
```

처럼 여러 지표가 필요할 수 있다.

---

## CHAPTER 08 · memory pressure에서 운영체제는 선택해야 한다

### free memory가 적다고 바로 문제가 되는 것은 아니다

OS는 남는 RAM을 file cache로 적극 활용할 수 있다. `free가 거의 0`이라는 숫자만 보고 memory leak을 단정하면 안 된다. 필요하면 reclaim 가능한 cache가 포함되어 있을 수 있다.

더 중요한 것은 **새 allocation을 만족시키기 위해 reclaim/eviction/swap/kill 같은 비싼 동작이 얼마나 발생하는지**다.

### page cache는 파일 I/O와 RAM을 연결한다

파일을 읽으면 kernel이 그 page를 page cache에 보관할 수 있다. 같은 파일을 다시 읽을 때 storage device에 다시 접근하지 않고 RAM에 있는 cached page를 사용할 수 있다.

따라서 두 번째 파일 읽기가 첫 번째보다 훨씬 빠른 benchmark가 나왔다고 해서 application algorithm이 개선된 것은 아닐 수 있다.

benchmark에서는 warm cache와 cold cache 조건을 구분해야 한다.

### swap은 느린 storage를 RAM처럼 쓰는 단순한 확장판이 아니다

일부 시스템은 덜 사용되는 anonymous page를 storage의 swap 영역으로 내보낼 수 있다. 다시 필요하면 page-in해야 하므로 큰 지연이 생길 수 있다.

memory pressure가 심해 page를 계속 내보내고 다시 가져오는 thrashing 상태가 되면 CPU보다 I/O에 시간을 많이 쓰며 전체 시스템이 매우 느려질 수 있다.

Android처럼 일반 desktop/server와 memory pressure 정책이 다른 플랫폼에서는 low-memory handling과 process killing 정책을 따로 봐야 한다.

---

## CHAPTER 09 · Android에서 process memory를 볼 때도 숫자를 하나로 합치지 않는다

### Java/Kotlin heap만 있는 것이 아니다

Android process에는 managed heap 외에도 native allocation, thread stack, graphics buffer, memory-mapped files, shared libraries 등 여러 memory 영역이 있을 수 있다.

그래서 `heap profiler에 객체가 별로 없는데 process memory가 크다`면 다른 영역을 봐야 한다.

### bitmap과 graphics는 별도 비용 경로를 가질 수 있다

이미지 앱은 압축된 JPEG 파일 크기와 화면에 decode된 pixel buffer 크기가 크게 다를 수 있다.

예를 들어 4000×3000 RGBA 이미지 한 장의 raw pixel data는 단순 계산으로:

```text
4,000 × 3,000 × 4 bytes
= 48,000,000 bytes
≈ 45.8 MiB
```

가 될 수 있다.

디스크 JPEG가 3MB라고 해서 memory에서도 3MB인 것은 아니다. 이미지 처리 앱에서 OOM을 볼 때 file size만 확인하면 원인을 놓칠 수 있다.

### lifecycle과 memory retention을 연결한다

Activity가 화면에서 사라졌는데 singleton이나 long-lived callback이 Activity/Context를 reference하면 큰 view tree와 resource가 함께 남을 수 있다.

```text
long-lived object
→ callback
→ Activity
→ View tree
→ Bitmap / resource
```

heap dump에서 단순히 `Activity가 10개 있다`보다 **GC root에서 왜 reachability path가 남아 있는지**를 따라가야 한다.

---

## CHAPTER 10 · write는 여러 cache를 거쳐 durability에 도달한다

### application write가 성공한 시점을 정확히 묻는다

파일에 데이터를 쓸 때 경로를 단순화하면 다음 계층이 있을 수 있다.

```text
application buffer
↓
library/runtime buffer
↓ system call
kernel page cache
↓ writeback
storage device controller/cache
↓
non-volatile media
```

모든 시스템이 정확히 이 그림과 같지는 않지만, `함수가 반환했다`와 `전원 상실에도 데이터가 남는다` 사이에 계층이 있을 수 있다는 점이 중요하다.

### buffering은 성능과 durability 시점을 분리한다

작은 write마다 storage까지 기다리면 latency가 크다. 여러 데이터를 buffer에 모아 한 번에 쓰면 throughput을 높일 수 있다.

하지만 buffer에만 있고 아직 durable하지 않은 데이터는 crash에서 잃을 수 있다.

따라서 시스템은 다음 trade-off를 결정해야 한다.

```text
얼마나 자주 durable flush할 것인가?
얼마나 많은 데이터 유실을 허용할 수 있는가?
latency와 throughput 목표는 무엇인가?
```

이 질문은 logging system, database, message queue 모두에서 다시 나온다.

### flush라는 이름도 API마다 보장 범위가 다르다

language buffer의 `flush()`가 library buffer를 kernel로 밀어내는 것만 보장할 수 있고, kernel cache를 physical durable media까지 강제하는 것은 `fsync` 계열처럼 별도 API가 필요할 수 있다.

그래서 `flush 호출했으니 안전`이라고 이름만 보고 결론내리지 않는다.

문서에서 **어느 경계까지 완료를 보장하는지** 확인한다.

---

## CHAPTER 11 · fsync만 부르면 crash consistency가 자동으로 완성되는 것은 아니다

### 데이터와 metadata가 함께 일관되어야 한다

새 파일을 만든 뒤 내용을 쓰고 이름을 바꾸는 update pattern을 생각하자.

```text
1. temp file 생성
2. 새 내용 write
3. temp file durable flush
4. rename으로 old file 교체
5. directory metadata durability 확인
```

파일시스템과 OS의 정확한 보장에 따라 필요한 단계가 달라질 수 있다. 핵심은 **데이터 블록뿐 아니라 파일 이름과 directory entry 같은 metadata 변경도 crash 후 일관되게 남아야 한다**는 점이다.

### atomicity와 durability는 같은 속성이 아니다

rename이 관찰자에게 atomic하게 old/new 중 하나로 보이게 해 줄 수 있어도, 전원 장애 후 새 이름이 반드시 durable하게 남는 문제는 별도일 수 있다.

```text
atomic
→ 중간 찢어진 상태를 관찰하지 않도록 하는 성질

durable
→ 성공으로 인정한 결과가 특정 failure 후에도 남는 성질
```

데이터베이스의 ACID에서도 이 둘을 A와 D로 따로 부르는 이유가 있다.

### crash consistency는 순서 문제다

관련 write가 여러 개일 때 crash가 어느 시점에도 발생할 수 있다고 가정해야 한다.

```text
write A 완료
write B 진행 중
--- 전원 장애 ---
```

복구 후 A만 있고 B가 없을 때 데이터 구조가 유효한가?

이 질문 때문에 filesystem journaling, database WAL 같은 구조가 필요하다. 변경 순서를 기록하고 recover 가능한 state transition을 설계하는 것이다.

---

## CHAPTER 12 · 데이터베이스 WAL을 지금은 '순서 보장 장치'로 이해한다

### 본문 page를 먼저 고치는 것보다 log를 먼저 durable하게 만든다

데이터베이스가 table/index page를 제자리에서 수정하는 도중 crash가 나면 복구가 어렵다. Write-Ahead Logging, WAL의 핵심 아이디어 중 하나는 **데이터 page 변경보다 recovery에 필요한 log record를 먼저 durable하게 만든다**는 것이다.

단순화한 흐름:

```text
transaction change
↓
WAL record 생성
↓
commit에 필요한 WAL durable
↓
나중에 dirty data page writeback 가능
```

이 덕분에 data page가 아직 storage에 반영되지 않았어도 WAL을 사용해 redo할 수 있다.

DB마다 구현과 보장은 다르므로 이 그림을 특정 제품의 정확한 commit protocol로 외우면 안 된다. 뒤의 데이터베이스 TRACK에서 PostgreSQL의 WAL/checkpoint/MVCC를 실제 문서와 함께 분리해서 배운다.

### checkpoint는 log를 지우는 버튼이 아니다

log가 무한히 쌓일 수 없으므로 어느 시점의 data page 상태와 recovery 시작점을 관리해야 한다. checkpoint는 recovery time, write I/O, log retention과 연결된다.

`checkpoint를 자주 하면 무조건 안전` 같은 결론은 틀리다. 더 잦은 checkpoint는 write pressure를 높일 수도 있다.

---

## CHAPTER 13 · 메모리와 저장 문제를 증거로 나눈다

### 증상: 앱이 점점 느려진다

가능한 원인은 전혀 다른 계층에 있다.

```text
allocation rate 증가 → GC pressure
working set 증가 → cache miss/TLB miss
memory pressure → reclaim/swap
file cache miss → storage I/O
lock contention → runnable thread가 기다림
```

따라서 먼저 측정한다.

```text
CPU profile
allocation profile
heap/live object
RSS/working set
page fault
I/O latency/throughput
GC pause
lock wait
```

### 증상: 메모리 부족으로 종료된다

조사 순서 예시:

```text
1. managed heap live object가 증가하는가?
2. reference chain이 왜 남는가?
3. native/graphics allocation은 얼마인가?
4. thread 수와 stack은?
5. mmap/file mapping은?
6. 순간 peak인가 지속 growth인가?
```

`객체를 몇 개 줄였다`가 아니라 어떤 영역이 실제 limit을 넘었는지 찾는다.

### 증상: 저장 성공을 띄웠는데 재부팅 후 데이터가 없다

조사 질문:

```text
UI 성공 시점은 어느 API 반환 시점인가?
application buffer만 flush했는가?
OS durability API가 필요한가?
transaction commit은 언제 완료되었는가?
write ordering과 rename 순서는?
crash recovery 경로를 실제 시험했는가?
```

이것이 durability bug를 UI state bug와 분리하는 방법이다.

---

## CHAPTER 14 · 이 PART의 핵심 연결

메모리와 저장을 다음처럼 분리해서 외우지 않는다.

```text
RAM = 빠름
SSD = 느림
```

대신 하나의 access가 여러 계층을 통과할 수 있음을 기억한다.

```text
program virtual address
↓
TLB / page-table translation
↓
CPU cache hierarchy
↓
physical memory
↕
page cache / memory mapping / reclaim
↕
persistent storage
```

그리고 write의 성공을 다음 세 질문으로 분리한다.

```text
프로세스가 값을 만들었는가?
kernel이 write를 받아들였는가?
요구하는 failure model에서 durable한가?
```

마지막 질문에 답하지 못한 시스템은 `저장 완료`라는 말을 아직 정확히 정의하지 못한 것이다.
