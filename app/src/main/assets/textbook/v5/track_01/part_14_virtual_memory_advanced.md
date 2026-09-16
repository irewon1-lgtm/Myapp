# PART 14 · virtual memory 심화 — page table, TLB, reclaim, overcommit, NUMA

`가상주소가 물리주소로 변환된다`는 한 문장만으로는 실제 성능과 장애를 설명하기 어렵다. address translation 자체에도 cache가 있고, page table은 메모리를 소비하며, page size 선택은 TLB reach와 fragmentation을 바꾸고, memory pressure에서는 reclaim/swap/kill 정책이 실행된다.

이 PART의 목표는 MMU 용어를 외우는 것이 아니라 **주소 변환 비용, resident set, reclaim, shared page, NUMA locality가 실제 프로그램 성능과 실패로 이어지는 경로**를 이해하는 것이다.

---

## CHAPTER 01 · virtual address를 page number와 offset으로 나눈다

page size가 4KiB라고 가정한 단순 예에서 virtual address의 낮은 12bit는 page 내부 offset으로 사용할 수 있다.

```text
virtual address
[ virtual page number | page offset ]
```

translation은 virtual page number를 physical frame number로 바꾸고 offset은 그대로 유지하는 방식으로 생각할 수 있다.

```text
VPN → PFN
physical address = PFN + same offset
```

실제 architecture는 address width와 page size mode가 여러 개일 수 있다.

---

## CHAPTER 02 · flat page table은 큰 address space에서 너무 비쌀 수 있다

모든 virtual page마다 page table entry 하나를 고정 배열로 두면 사용하지 않는 huge address range까지 table memory를 소비한다.

64-bit address space에서는 비현실적으로 커질 수 있다.

그래서 multi-level page table을 사용해 **실제로 필요한 범위의 하위 table만 할당**한다.

```text
VA bits
↓ level 1 index
page table pointer
↓ level 2 index
...
↓ leaf PTE
physical frame
```

---

## CHAPTER 03 · page table walk 자체가 memory access다

TLB miss가 나면 hardware 또는 software가 page table entry를 여러 단계 읽어 translation을 찾는다.

4-level table에서 최악의 경우 여러 memory access가 필요할 수 있다.

```text
load data
전에
PTE L1 read
PTE L2 read
PTE L3 read
PTE L4 read
```

물론 page table entry 자체가 CPU cache에 있을 수 있어 실제 latency는 상황마다 다르다.

이 때문에 translation cache인 TLB가 중요하다.

---

## CHAPTER 04 · TLB는 주소 변환 결과를 cache한다

Translation Lookaside Buffer는 최근 virtual-page→physical-frame translation을 보관한다.

```text
virtual page
↓ TLB hit
physical frame immediately available
```

TLB miss면 page table walk가 필요하다.

### TLB reach

TLB entry 수 × page size는 한 번에 cover할 수 있는 memory footprint의 거친 upper bound를 준다.

예:

```text
TLB 2048 entries
page 4KiB
→ 약 8MiB mapping reach
```

실제 TLB는 여러 level/page-size set이 있어 더 복잡하다.

---

## CHAPTER 05 · huge page는 TLB reach를 늘린다

2MiB page 하나는 4KiB page 512개 영역을 cover한다.

같은 TLB entry 수로 더 큰 memory를 cover할 수 있다.

장점 후보:

```text
fewer TLB misses
smaller page-table overhead
```

대가:

```text
internal fragmentation
allocation/compaction difficulty
copy-on-write granularity
memory waste for sparse working set
```

`huge page 켜면 DB가 무조건 빨라진다`가 아니다.

---

## CHAPTER 06 · page table도 process memory cost다

수백 GB virtual memory를 촘촘히 mapping하면 page table page 자체가 상당한 physical memory를 소비할 수 있다.

프로세스 RSS만 보지 않고 page table memory가 system-wide pressure에 기여할 수 있음을 이해해야 한다.

특히 작은 page를 매우 넓은 range에 mapping하면 table overhead가 커질 수 있다.

---

## CHAPTER 07 · PTE에는 주소 외의 상태가 있다

architecture에 따라 page table entry에는 다음 성격의 bit가 있을 수 있다.

```text
present/valid
read/write permission
user/supervisor
execute restriction
accessed/reference
modified/dirty
cache/memory attribute
```

OS는 이 metadata를 protection과 reclaim 판단에 활용한다.

정확한 bit layout은 architecture manual을 확인한다.

---

## CHAPTER 08 · page fault의 종류를 원인으로 분해한다

page fault가 발생해도 처리 결과는 다르다.

```text
valid mapping but page not resident
→ demand page-in

copy-on-write write
→ private page allocate/copy

stack growth allowed range
→ new page mapping

invalid address
→ process fault/termination

permission violation
→ protection fault
```

`fault count`만 보고 error count처럼 해석하지 않는다.

---

## CHAPTER 09 · minor와 major fault는 I/O 유무의 성능 차이를 드러낸다

이미 memory/page cache에 있는 page를 page table에 연결하는 fault와 storage I/O가 필요한 fault는 비용이 크게 다를 수 있다.

성능 조사에서:

```text
minor faults 증가
vs
major faults 증가
```

를 분리한다.

major fault 폭증은 working set이 RAM에 못 들어가고 storage를 반복 접근하는 신호일 수 있다.

---

## CHAPTER 10 · copy-on-write는 write 순간에 비용을 지불한다

fork 직후 parent/child가 same physical page를 read-only style로 공유하다가 한쪽이 write하면:

```text
COW fault
↓
new physical page allocate
↓
old content copy
↓
writer PTE remap writable
```

가 일어날 수 있다.

따라서 fork가 싸더라도 child/parent가 huge memory를 곧바로 수정하면 실제 copy cost가 뒤에서 나타난다.

---

## CHAPTER 11 · memory-mapped file은 page cache와 PTE를 연결한다

file offset range를 virtual address에 mapping하면 page fault 시 kernel이 해당 file page를 page cache에 준비하고 PTE를 연결한다.

```text
VA access
↓ fault
file offset derived
↓ page cache lookup/read
physical page
↓ PTE
resume instruction
```

read syscall path와 mmap path가 같은 cached physical page를 공유할 수 있다.

---

## CHAPTER 12 · anonymous memory는 file에서 다시 읽을 원본이 없다

heap/stack 같은 anonymous dirty page는 reclaim할 때 단순히 `버리고 나중에 파일에서 다시 읽기`가 어렵다.

선택:

```text
keep in RAM
swap/compress somewhere
kill process
```

반면 clean file-backed page는 필요하면 file에서 다시 읽을 수 있어 reclaim하기 상대적으로 쉽다.

---

## CHAPTER 13 · reclaim은 어떤 page를 버릴지 결정한다

memory pressure가 생기면 kernel은 inactive/cold page를 찾고 reclaim하려 한다.

완벽한 `가장 오래 안 쓴 page`를 추적하는 것은 비싸므로 reference/access information을 이용한 근사 policy를 사용한다.

구현은 kernel version에 따라 발전한다.

핵심은:

> reclaim algorithm은 미래 working set을 정확히 모른 채 과거 access를 바탕으로 page 가치를 추정한다.

---

## CHAPTER 14 · thrashing은 useful work보다 page 이동에 시간을 쓰는 상태다

working set이 physical memory를 크게 초과하고 반복 access pattern이 넓으면:

```text
page in A
↓ B 필요 → A evict
↓ A 다시 필요 → B evict
↓ repeat
```

storage I/O/page fault에 대부분 시간을 쓸 수 있다.

CPU utilization이 낮고 disk activity/major faults가 높은 조합으로 보일 수 있다.

---

## CHAPTER 15 · swap은 RAM의 느린 확장처럼만 보면 안 된다

anonymous page를 storage-backed swap area로 내보내면 process를 죽이지 않고 RAM을 확보할 수 있다.

하지만 random page-in latency가 매우 커질 수 있다.

mobile에서는 compressed RAM(zRAM) 같은 mechanism을 사용할 수 있다.

### zRAM

page를 storage 대신 RAM 안에서 압축해 더 많은 logical page를 보관한다.

대가:

```text
compression CPU
compressed memory metadata
latency
```

이다.

---

## CHAPTER 16 · overcommit은 virtual allocation과 physical backing을 분리한다

어떤 OS policy에서는 application이 요청한 virtual memory 총량이 현재 physical RAM+swap보다 커도 allocation call이 성공할 수 있다.

실제 page를 touch할 때 backing이 필요하다.

```text
reserve huge virtual range
→ success
later touch every page
→ physical pressure
```

`malloc/new 성공했으니 앞으로 메모리 부족 없음`이 아니다.

---

## CHAPTER 17 · OOM은 allocation 한 줄만의 문제가 아니다

system이 더 이상 필요한 page를 확보할 수 없으면 OOM handling이 발생할 수 있다.

Linux 계열에서는 process kill policy가 실행될 수 있고 container/cgroup 안에서는 group memory limit 기준으로 OOM이 발생할 수 있다.

Android는 process importance/reclaim 정책까지 별도로 연결된다.

`죽은 process가 가장 메모리를 많이 썼다`고 단정하지 않는다.

victim selection policy와 context를 본다.

---

## CHAPTER 18 · memory pressure와 allocation latency

OOM까지 가지 않아도 reclaim/compaction이 allocation path에서 오래 걸릴 수 있다.

```text
allocation request
↓ free page insufficient
reclaim
compaction
writeback
↓
allocation completes
```

사용자에게는 간헐적 latency spike로 보일 수 있다.

CPU profile만 보면 kernel wait가 핵심인 상황을 놓칠 수 있다.

---

## CHAPTER 19 · contiguous physical memory 요구는 fragmentation 문제를 만든다

virtual memory는 연속 virtual range를 비연속 physical page에 mapping할 수 있어 일반 user allocation에 유리하다.

하지만 DMA/huge page/일부 kernel allocation처럼 큰 contiguous physical range가 필요한 경우 physical fragmentation이 문제가 된다.

compaction이 page를 이동해 큰 free range를 만들 수 있다.

---

## CHAPTER 20 · pinned page는 reclaim 자유도를 줄인다

DMA/user I/O 등을 위해 physical page가 일정 기간 이동/회수되지 못하도록 pin될 수 있다.

너무 많은 long-term pinned page는 memory management가 reclaim/compaction할 수 있는 선택지를 줄인다.

zero-copy optimization이 system memory flexibility를 대가로 가질 수 있는 이유다.

---

## CHAPTER 21 · NUMA에서는 memory latency가 CPU 위치에 따라 달라진다

Non-Uniform Memory Access system에서는 여러 memory node/socket가 있고 local node memory가 remote node보다 빠를 수 있다.

```text
CPU socket 0 → local node 0 memory fast
CPU socket 0 → remote node 1 memory slower
```

large server에서 thread placement와 memory allocation locality가 성능에 영향을 준다.

mobile phone에서는 전형적 multi-socket NUMA server와 구조가 다르므로 환경에 맞게 적용한다.

---

## CHAPTER 22 · first-touch policy가 NUMA placement를 결정할 수 있다

어떤 OS policy에서는 virtual page를 실제로 처음 touch한 CPU/node 근처에 physical page를 배치할 수 있다.

```text
main thread allocates 100GB virtual
worker node1 first writes half
worker node2 first writes half
```

initialization thread가 전부 먼저 zero-write하면 모든 page가 한 node에 몰릴 수 있다.

parallel initialization이 locality에 영향을 줄 수 있다.

---

## CHAPTER 23 · migration은 locality를 고치지만 공짜가 아니다

kernel이 hot page를 thread가 실행되는 NUMA node로 이동할 수 있다.

page copy와 page-table update/TLB invalidation 비용이 발생한다.

thread 자체가 계속 node를 옮기면 page도 쫓아다니며 비용이 커질 수 있다.

CPU affinity를 무조건 고정하는 것도 load balancing을 방해할 수 있다.

---

## CHAPTER 24 · TLB shootdown은 다른 core의 translation cache도 무효화해야 한다

page table mapping/permission을 바꾸면 다른 core TLB에 old translation이 남아 있을 수 있다.

OS는 관련 core에 invalidation을 전달해야 한다.

```text
core0 changes PTE
↓
notify cores using address space
↓
remote TLB invalidate
```

많은 core에서 frequent mapping change가 있으면 synchronization cost가 커질 수 있다.

---

## CHAPTER 25 · mmap/munmap을 매우 자주 하면 metadata와 shootdown 비용이 생긴다

memory allocator가 작은 object마다 mmap/munmap을 호출하지 않고 arena를 재사용하는 이유 중 하나다.

virtual memory area 관리, page table, TLB invalidation 비용이 있기 때문이다.

allocation profiler에서 system call rate도 본다.

---

## CHAPTER 26 · address-space fragmentation과 physical fragmentation은 다르다

### virtual fragmentation

연속 virtual range를 찾기 어려운 문제.

### physical fragmentation

큰 contiguous physical page block을 찾기 어려운 문제.

64-bit process는 virtual address space가 매우 넓어 virtual fragmentation 문제가 완화될 수 있지만 physical contiguous allocation 문제는 남을 수 있다.

둘을 `메모리가 조각났다` 한 문장으로 섞지 않는다.

---

## CHAPTER 27 · memory protection은 W^X와 code generation에 영향을 준다

보안 hardening은 같은 page가 동시에 writable+executable이 되지 않도록 W^X 정책을 사용할 수 있다.

JIT compiler는 code를 생성할 때:

```text
writable mapping에서 bytes 생성
↓
permission transition
↓
executable, non-writable
```

같은 안전한 protocol을 따라야 한다.

platform에 따라 dual mapping 등 다른 mechanism이 가능하다.

---

## CHAPTER 28 · guard page는 overflow를 조용한 corruption 대신 fault로 바꾼다

stack/allocator boundary 주변에 intentionally unmapped page를 두면 넘친 access가 즉시 page fault로 드러날 수 있다.

```text
valid stack pages
↓
guard page (no access)
↓
other memory
```

memory waste는 조금 늘지만 debugging/security에 도움을 준다.

---

## CHAPTER 29 · address sanitizer shadow memory도 virtual memory를 적극 사용한다

ASan 같은 memory bug detector는 application memory의 상태를 추적하는 shadow mapping과 redzone을 사용한다.

실제 memory footprint와 address-space use가 크게 증가할 수 있다.

따라서 sanitizer build performance/memory 숫자를 production build와 직접 비교하면 안 된다.

---

## CHAPTER 30 · Android memory metric을 VM 관점으로 해석한다

Android `dumpsys meminfo`/profiling에서 PSS, private dirty, shared dirty, native heap, graphics 등 분류를 본다.

PSS는 shared physical page의 비용을 여러 process에 비례 배분해 system impact를 추정한다.

### process kill 뒤 재시작

system memory pressure에서 background process를 kill해 RAM을 회수할 수 있다.

app은 process memory를 durable source of truth로 가정하면 안 된다.

---

## CHAPTER 31 · memory performance 사건을 단계별로 분석한다

증상:

> 대형 이미지 화면에서 처음 3초 freeze, 이후 빠름.

가능한 path:

```text
file page major faults
image compressed bytes read
decode allocation
anonymous pages commit
graphics buffer allocation
GC/reclaim
GPU upload
```

### 측정

```text
major/minor faults
I/O latency
allocation profile
PSS breakdown
GC events
frame trace
```

`RAM 부족` 하나로 끝내지 않는다.

---

## CHAPTER 32 · VM tuning은 application invariant 안에서 한다

huge page, mmap, buffer pool, swap policy를 튜닝할 때 다음을 같이 본다.

```text
latency distribution
memory footprint
fault rate
TLB/cache metrics
reclaim pressure
startup behavior
failure/recovery behavior
```

한 숫자 최적화로 다른 failure mode를 만들지 않는다.

---

## PART 14 종료 점검

1. multi-level page table이 flat table보다 memory를 절약하는 이유는 무엇인가?
2. TLB miss가 왜 추가 memory access를 만들 수 있는가?
3. huge page가 TLB reach를 늘리면서 fragmentation을 만들 수 있는 이유는 무엇인가?
4. COW write 순간 어떤 page fault/copy가 발생하는가?
5. anonymous page와 clean file-backed page가 reclaim에서 어떻게 다른가?
6. thrashing은 어떤 metric 조합으로 의심할 수 있는가?
7. overcommit 환경에서 allocation 성공이 future physical memory를 보장하지 않는 이유는 무엇인가?
8. memory pressure가 OOM 이전에도 latency spike를 만들 수 있는 이유는 무엇인가?
9. NUMA에서 first-touch가 memory locality를 바꿀 수 있는 이유는 무엇인가?
10. TLB shootdown은 왜 multi-core synchronization cost가 되는가?
11. virtual fragmentation과 physical fragmentation을 어떻게 구분하는가?
12. JIT와 W^X policy가 어떤 permission transition 문제를 만드는가?
13. sanitizer build memory 수치를 production과 직접 비교하면 안 되는 이유는 무엇인가?
14. Android PSS를 단순 RSS와 구분해야 하는 이유는 무엇인가?

이제 virtual memory를 `가상주소를 물리주소로 바꾼다` 한 문장으로 끝내지 않는다. **page table → TLB → fault → resident set → reclaim → swap/kill → NUMA/TLB synchronization**까지 연결할 수 있어야 한다.