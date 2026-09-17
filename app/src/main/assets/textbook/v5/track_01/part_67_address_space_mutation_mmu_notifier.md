# PART 67 · Address-Space Mutation — VMA locks, page tables, invalidation, secondary MMUs

Virtual address space는 process 시작 때 한 번 만들어져 고정되는 table이 아니다. `mmap`, `munmap`, `mprotect`, stack expansion, file truncate, NUMA policy, userfaultfd registration이 **VMA metadata와 page tables를 실행 중에 변경**한다. 동시에 다른 threads는 page fault를 처리하고, KVM/GPU 같은 secondary MMU는 같은 memory를 별도 translation에 cache할 수 있다. Correctness는 address range mutation을 모든 readers와 translation consumers에게 어떤 순서로 publish/invalidate하는가에 달려 있다.

## CHAPTER 01 · mm_struct는 process address-space identity다

같은 process의 threads는 하나의 `mm_struct`를 공유하며 그 안에서 VMAs와 page tables를 통해 virtual address meaning을 정의한다. Thread-local stack도 같은 address-space namespace의 한 VMA다. `thread마다 메모리가 따로 있다`가 아니라 mappings는 process-wide shared state라는 점에서 mutation locking이 필요하다.

## CHAPTER 02 · VMA는 같은 attributes를 가진 contiguous virtual range다

VMA는 start/end, protection, file/offset, policy 같은 mapping metadata를 표현한다. Page가 실제 physical memory에 resident하지 않아도 VMA는 `이 virtual range에서 어떤 fault가 합법적인가`를 정의한다. VMA mutation과 PTE mutation은 같은 것이 아니다.

## CHAPTER 03 · Maple tree는 VMA lookup namespace를 관리한다

Modern Linux mm은 address→VMA lookup을 maple tree로 관리한다. Page fault와 `/proc` traversal처럼 많은 readers가 존재하므로 lookup scalability가 중요하다. Tree node lifetime과 VMA lifetime을 lock/RCU 없이 임의 pointer로 보존하면 concurrent unmap에서 stale reference가 된다.

## CHAPTER 04 · mmap_lock은 address-space-wide stability boundary다

`mmap_lock` read side는 전체 mapping layout을 stable하게 읽게 하고 write side는 mapping structure modification을 serialize한다. 그러나 모든 page fault가 global read lock에 의존하면 large multithread workload에서 contention이 생길 수 있다. 그래서 finer VMA read locking이 존재한다.

## CHAPTER 05 · Per-VMA read lock은 page fault scalability를 높인다

`lock_vma_under_rcu()` 같은 path는 RCU lookup 후 VMA read lock을 optimistic하게 얻어 unrelated VMA mutations와 page faults가 병렬 진행할 수 있게 한다. Lock acquisition이 실패하면 global mmap read lock 같은 fallback이 필요하다. Optimistic path는 failure-free guarantee가 아니다.

## CHAPTER 06 · VMA write는 mmap write lock보다 더 좁은 exclusion을 제공한다

VMA metadata를 수정하는 writer는 먼저 address-space-wide write authority를 얻고 대상 VMA를 write-lock한다. 이렇게 하면 다른 VMA reader가 동시에 page fault를 처리하는 동안 unrelated metadata를 건드릴 수 있다. Lock hierarchy를 거꾸로 잡으면 writer-reader deadlock이 생긴다.

## CHAPTER 07 · VMA stability와 page-table stability는 별도다

VMA read lock을 잡았다고 PTE contents가 자동으로 immutable해지는 것은 아니다. Page-table entry는 별도 PTE/PMD/page-table locks와 atomic access 규칙을 가진다. Mapping metadata와 translation state를 같은 lock 하나가 보호한다고 가정하면 race를 놓친다.

## CHAPTER 08 · Page fault는 VMA permission과 PTE state를 함께 검증한다

Fault handler는 address에 해당하는 VMA가 존재하고 requested access가 protection을 만족하는지 확인한 뒤 PTE를 populate/update한다. Concurrent `mprotect`/`munmap` writer가 시작되면 fault가 stale VMA permission으로 page를 설치하지 못하도록 lock ordering이 필요하다.

## CHAPTER 09 · mprotect는 permission bit만 바꾸는 syscall이 아니다

Protection range가 기존 VMA 일부만 덮으면 VMA split/merge가 발생할 수 있고 page-table permission도 갱신돼야 한다. CPU TLB에 old permission translation이 남아 있으면 access restriction이 늦게 적용될 수 있다. Metadata change→PTE update→TLB invalidation ordering이 하나의 security boundary다.

## CHAPTER 10 · munmap은 VMA removal과 PTE teardown, resource lifetime을 연결한다

Range를 unmap하면 future lookup을 막고 existing page mappings를 제거하며 file/anon reverse mapping과 page reference를 정리해야 한다. 다른 CPU가 해당 PTE를 walk하거나 device가 page를 pin한 상태라면 physical page를 즉시 free할 수 없다. Address removal과 backing lifetime은 별도 단계다.

## CHAPTER 11 · VMA split은 하나의 policy domain을 둘 이상으로 나눈다

Range 일부의 protection/policy를 바꾸면 하나의 VMA를 앞/중간/뒤로 split할 수 있다. Split 후 file offset, anon_vma, userfaultfd context, NUMA policy 같은 metadata가 정확히 상속돼야 한다. Metadata 하나를 빼먹으면 fault semantics가 range 경계에서 달라진다.

## CHAPTER 12 · VMA merge는 metadata equality뿐 아니라 lifetime 조건을 확인한다

인접 VMAs가 같은 flags/file/policy를 가진다고 무조건 합칠 수 있는 것은 아니다. Reverse mappings, anon lineage, special callbacks 등 merge compatibility를 확인해야 한다. Aggressive merge는 metadata overhead를 줄이지만 observer가 기대하는 boundary를 바꿀 수 있다.

## CHAPTER 13 · Reverse mapping은 physical page에서 mapping VMAs를 찾는 반대 방향 index다

Reclaim, migration, truncate는 physical folio가 어느 processes/VMAs에 mapping되어 있는지 찾아야 한다. Anonymous/file-backed reverse mapping structures가 이 관계를 제공한다. VMA layout을 바꾸는 writer는 reverse-map index와 address tree를 일관된 순서로 수정해야 한다.

## CHAPTER 14 · rmap lock order를 어기면 memory pressure에서만 deadlock이 나타날 수 있다

Filesystem write, page fault, reclaim, truncate는 inode/mmap/VMA/rmap/page-table/LRU locks를 서로 다른 경로에서 만난다. 정상 workload에서는 race가 없어 보이다가 direct reclaim이나 concurrent truncate가 섞이면 lock cycle이 드러난다. MM lock order는 error/reclaim path까지 포함해 지켜야 한다.

## CHAPTER 15 · PTE update는 architecture-defined atomicity와 memory ordering을 따른다

PTE를 읽고 쓰는 operation은 ordinary struct field access가 아니다. Hardware page walker가 동시에 볼 수 있고 access/dirty bit가 hardware에서 갱신될 수 있다. Architecture helper를 우회한 raw write는 permission/accessed state를 잃거나 torn translation을 만들 수 있다.

## CHAPTER 16 · TLB shootdown은 old translation을 다른 CPU에서 제거한다

PTE를 바꾼 CPU만 local TLB를 invalidate해도 다른 CPUs가 same mm의 old mapping을 계속 cache할 수 있다. Cross-CPU invalidation과 acknowledgement가 필요하다. 많은 CPUs가 같은 address space를 실행할수록 mapping mutation cost가 증가하는 이유다.

## CHAPTER 17 · Batched invalidation은 shootdown 횟수를 줄이지만 stale-window contract를 요구한다

여러 PTE를 바꿀 때 매 entry마다 IPI를 보내는 대신 range를 batch로 invalidation하면 비용을 줄일 수 있다. 그러나 physical page reuse나 permission tightening 전에 old translations가 모두 사라졌음을 보장해야 한다. Invalidation delay와 memory reuse ordering을 분리하면 security bug가 된다.

## CHAPTER 18 · Huge-page split은 page-table level 자체를 변경한다

PMD-level huge mapping을 작은 PTE mappings로 split하면 translation granularity와 locks가 바뀐다. Concurrent page walker와 secondary MMU가 old huge mapping을 cache할 수 있어 invalidation ordering이 복잡하다. THP split은 단순 metadata flag change가 아니다.

## CHAPTER 19 · File truncate는 mapping 뒤에서 backing object 크기를 줄인다

Mapped file이 truncate되면 해당 offset을 가리키는 page cache와 VMAs/PTEs를 invalidation해야 한다. Process가 stale mapped page를 계속 읽으면 file size contract와 data lifetime이 깨진다. Filesystem invalidate lock과 MM locks가 만나는 이유다.

## CHAPTER 20 · mremap은 virtual identity를 이동시키면서 backing identity를 유지할 수 있다

Mapping을 다른 virtual range로 옮길 때 page contents를 반드시 copy하는 것은 아니다. Page-table entries와 VMA metadata를 재배치해 backing pages를 유지할 수 있다. Pointer를 external device/runtime가 long-term virtual address로 캐시했다면 remap과 충돌한다.

## CHAPTER 21 · userfaultfd registration도 VMA metadata mutation이다

특정 range가 userspace fault handler에 의해 관리됨을 VMA context에 기록해야 한다. Concurrent split/merge/unmap에서 registration metadata가 올바른 subrange에 따라가야 한다. Pager teardown과 address-space writer가 same range를 수정하는 race를 명시해야 한다.

## CHAPTER 22 · mlock/pinning은 mapping removal과 physical-page lifetime을 분리한다

Page가 pinned되어 있으면 VMA를 unmap해도 DMA/other holder 때문에 physical page가 즉시 movable/freeable하지 않을 수 있다. Long-term pin은 migration, compaction, tiering을 방해한다. Mapping lifetime보다 reference lifetime이 길 수 있다는 점을 MM mutation protocol에 포함해야 한다.

## CHAPTER 23 · Secondary MMU는 CPU page table과 별도 translation cache를 가진다

KVM stage-2/EPT, IOMMU, device address-space가 process/user memory를 secondary mapping으로 사용할 수 있다. CPU PTE를 unmap하거나 permission을 줄여도 secondary translation이 old mapping을 유지하면 device/guest가 freed page에 접근할 수 있다. MMU notifier가 필요한 이유다.

## CHAPTER 24 · invalidate_range_start는 secondary user가 old mapping을 쓰는 것을 먼저 막는다

Primary MM이 page를 free/reuse하기 전에 secondary MMU consumer에게 range invalidation 시작을 알려 old translation을 제거하거나 access를 차단하게 해야 한다. Callback ordering이 잘못되면 use-after-free가 hardware translation cache를 통해 발생한다.

## CHAPTER 25 · invalidate_range_end는 mutation completion 이후 새 state를 재구성할 경계다

Start/end pair는 invalidation window 동안 secondary MMU가 mapping을 재설치하지 못하게 coordination할 수 있다. KVM은 active invalidate count 등으로 memslot/update와 notifier concurrency를 조정한다. Start/end callbacks가 같은 mapping generation을 가리켜야 한다.

## CHAPTER 26 · MMU notifier callback은 forbidden lock acquisition을 지켜야 한다

Primary mm lock을 보유한 상태에서 notifier가 호출될 수 있으므로 secondary MMU lock order가 primary path와 cycle을 만들지 않아야 한다. KVM처럼 notifier 안에서 특정 slots lock을 잡지 않는 명시적 규칙이 필요하다. Cross-subsystem callback은 lock inversion의 전형적 source다.

## CHAPTER 27 · Page-table mutation observability는 syscall 이름보다 range/generation을 기록해야 한다

`mprotect` 하나가 VMA split과 수천 PTE invalidation, cross-CPU shootdown을 만들 수 있다. Trace에는 mm identity, virtual range, old/new flags, pages affected, shootdown CPUs, secondary notifier latency를 남겨야 cost와 race를 분석할 수 있다.

## CHAPTER 28 · Address-space churn은 allocator/GC와 다른 성능 병목이 된다

JIT code permission flip, mmap arena churn, frequent small mappings가 많으면 actual data access보다 VMA tree mutation과 TLB shootdown이 CPU를 소비할 수 있다. Allocation benchmark만 보면 kernel MM metadata cost를 놓친다. Mapping operation rate 자체를 지표로 봐야 한다.

## CHAPTER 29 · Mapping mutation fault injection은 concurrent readers와 devices를 포함해야 한다

Stress test는 threads가 page fault를 일으키는 동안 mprotect/munmap/mremap을 반복하고, secondary MMU/device mappings가 있는 조건도 포함해야 한다. Race detector만으로 hardware translation lifetime bug를 잡기 어렵다. Invalid access, stale mapping, notifier delay를 별도 oracle로 둬야 한다.

## CHAPTER 30 · Address-space mutation은 translation consumers 전체에 대한 consistency protocol이다

Robust MM 설계는 **VMA tree stability, mmap/VMA/rmap/page-table lock ordering, PTE atomicity, TLB shootdown, backing-file invalidation, pin lifetime, userfaultfd metadata, secondary-MMU notifier ordering**을 하나로 묶어야 한다. Virtual address의 의미를 바꾸는 순간 CPU뿐 아니라 그 address를 cache한 모든 consumer에게 old meaning이 끝났음을 증명해야 한다.
