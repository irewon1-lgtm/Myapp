# PART 78 · Page Pinning and DMA Lifetime — FOLL_PIN, long-term pins, COW, migration, device ownership

Userspace virtual address를 device DMA에 넘긴다고 해서 device가 그 주소를 직접 이해하는 것은 아니다. Driver는 page fault·COW·migration·reclaim으로 physical backing이 바뀔 수 있는 userspace mapping과, device가 일정 시간 동일 physical storage를 참조해야 하는 DMA lifetime 사이를 연결해야 한다. 이 연결이 **page pinning**이다. Pin은 `메모리를 잠깐 잡아둔다`가 아니라 VM·filesystem·IOMMU·device teardown에 영향을 주는 **cross-subsystem lifetime claim**이다. 잘못된 pin은 data corruption보다 먼저 reclaim failure, memory hot-remove 실패, writeback incoherence, process exit hang 같은 형태로 나타날 수 있다.

## CHAPTER 01 · Virtual address lifetime과 physical page lifetime은 같은 계약이 아니다

Process가 가진 virtual address는 page table entry가 다른 physical page를 가리키도록 바뀔 수 있다. COW, migration, compaction, swap, NUMA balancing이 모두 backing을 바꿀 수 있다. CPU load/store는 page table과 fault mechanism을 따라 새 backing을 다시 찾지만 DMA engine은 이미 programming된 DMA address를 계속 사용할 수 있다. 따라서 device가 page contents를 비동기적으로 참조하는 동안 kernel은 **그 storage가 재배치·회수·재사용되지 않는다는 별도 lifetime guarantee**를 만들어야 한다.

## CHAPTER 02 · get_user_pages reference와 DMA pin은 목적이 다르다

Ordinary `get_user_pages*()` reference는 `struct page`를 일시적으로 참조하는 코드에 적합하지만, DMA가 page data를 CPU와 독립적으로 읽고 쓰는 lifetime은 더 강한 제약을 요구한다. Linux는 DMA pin을 ordinary page reference와 구분하기 위해 `pin_user_pages*()` 계열과 FOLL_PIN semantics를 제공한다. 두 API를 같은 refcount 증가로만 이해하면 filesystem writeback과 page migration이 `이 page를 device가 쓰고 있다`는 사실을 알 수 없다. **Reference existence와 mobility prohibition은 서로 다른 의미**다.

## CHAPTER 03 · FOLL_PIN은 call site가 직접 켜는 flag보다 wrapper contract다

FOLL_PIN은 일반적으로 caller가 raw flag를 넣는 대신 `pin_user_pages*()` wrapper가 내부적으로 설정한다. 목적은 DMA pinning call site를 코드 구조에서 분명하게 만들고, unpin path도 ordinary `put_page()`와 구분하는 것이다. 같은 `struct page`에 ordinary reference와 pin reference가 동시에 존재할 수 있으므로 cleanup은 acquisition 종류와 정확히 짝을 맞춰야 한다. Wrong release primitive는 refcount leak 또는 pin accounting 불일치로 이어진다.

## CHAPTER 04 · FOLL_LONGTERM은 “오래 잡는다” 이상의 더 강한 배치 제한을 의미한다

RDMA registration처럼 page를 장시간 device에 제공하는 경우 VM은 그 page를 migration 가능한 ordinary memory처럼 취급하기 어렵다. FOLL_LONGTERM은 FOLL_PIN보다 제한적인 contract이며, 특정 memory type이나 DAX mapping처럼 long-term pin과 안전하게 공존하지 못하는 대상을 거부할 수 있다. `몇 초 이상이면 long-term` 같은 고정 시간 기준보다 **filesystem·MM이 정상 maintenance 동안 page 이동을 필요로 하는가**가 더 본질적인 판단이다.

## CHAPTER 05 · Pin accounting은 ordinary refcount와 구별 가능한 신호가 필요하다

Small page에서 DMA pin은 page refcount에 큰 bias를 더하는 방식으로 추적될 수 있고, large folio는 별도 pincount field를 사용할 수 있다. 설계 목표는 `pinned인데 아니라고 판단하는 false negative`를 피하는 것이다. False positive는 일부 경로에서 보수적으로 page를 movable하지 않다고 판단하게 만들 뿐이지만, false negative는 device가 접근 중인 page를 migration/reclaim 대상으로 오판하게 만들 수 있다. 따라서 pin tracking은 performance counter가 아니라 correctness metadata다.

## CHAPTER 06 · Large folio와 huge page는 pin count overflow 문제를 별도로 만든다

Huge mapping은 하나의 head structure가 많은 tail page를 대표할 수 있어 ordinary refcount trick을 그대로 적용하면 count range와 false-positive 문제가 커진다. Large folio에 dedicated pincount를 두는 이유는 **page 수가 커질수록 pin accounting이 왜곡되지 않도록 하기 위해서**다. Driver가 huge page를 pin한 뒤 PAGE_SIZE 단위로 unpin하는 경우에도 최종 accounting이 균형을 이뤄야 한다. Pin leak 검사는 byte 수보다 logical pin acquisition/release symmetry를 보는 편이 안전하다.

## CHAPTER 07 · Partial pin success는 반드시 prefix cleanup protocol을 요구한다

GUP/PUP 계열은 요청한 page 수보다 적은 수만 성공하고 반환할 수 있다. Caller가 `nr_pages 전부 아니면 실패`라고 가정해 전체 배열을 unpin하면 초기화되지 않은 entry를 만질 수 있고, 반대로 partial result를 error로 처리하면서 이미 pin된 prefix를 풀지 않으면 leak가 된다. Acquisition 결과는 **0..n의 성공 범위**를 명시적으로 나타내며, error path는 실제 획득된 count만 reverse order로 release해야 한다. Batch API에서 가장 흔한 lifetime bug가 이 partial-success cleanup이다.

## CHAPTER 08 · Write-capable pin은 COW semantics를 실제로 깨울 수 있다

Private mapping의 page를 device가 write할 예정이라면 readonly snapshot page를 그대로 pin해서는 안 된다. CPU write처럼 COW를 resolve해 process가 소유한 writable backing을 확보해야 다른 process와 shared source page를 device가 몰래 변경하지 않는다. Pin request의 write intent가 정확하지 않으면 COW isolation이 깨지거나, 반대로 불필요한 COW로 memory footprint가 폭증한다. **DMA direction과 VM write intent는 서로 다른 layer지만 일관되게 연결**되어야 한다.

## CHAPTER 09 · fork 이후 parent와 child가 공유하는 COW page는 DMA lifetime을 복잡하게 만든다

Process가 buffer를 pin한 뒤 fork하면 page table semantics는 ordinary anonymous memory와 달라질 수 있다. Device가 parent registration을 통해 old physical page를 계속 접근하는 동안 child 또는 parent가 COW fault로 새 page를 받을 수 있다. Application이 `같은 virtual address니까 같은 DMA buffer`라고 가정하면 실제 physical ownership과 어긋난다. Fork-safe API는 registration inheritance 여부, child usage 금지, 재등록 필요성을 명시해야 한다.

## CHAPTER 10 · File-backed writable pin은 dirtying과 filesystem ownership을 건드린다

Device가 file-backed page를 변경하면 CPU store처럼 page dirty state와 filesystem writeback semantics에 반영되어야 한다. Pin만 해두고 unpin 시 dirty propagation을 누락하면 memory의 새 data가 storage에 기록되지 않거나 filesystem이 clean page라고 오판할 수 있다. 반대로 read-only DMA인데 무조건 dirty 처리하면 불필요한 writeback을 만든다. **누가 page contents를 수정했는지**는 pin lifetime 끝에서 filesystem contract로 전달되어야 한다.

## CHAPTER 11 · folio_mkclean과 writeback은 DMA pin 존재를 알아야 한다

Filesystem은 writeback이나 mapping cleanup 과정에서 PTE dirty state를 정리하고 page를 write-protect/unmap하려 할 수 있다. 하지만 device가 CPU page table 밖에서 같은 memory를 수정할 수 있다면 PTE만 관찰해서 dirty state를 완전히 판단할 수 없다. `folio_maybe_dma_pinned()` 같은 정보가 필요한 이유는 writeback subsystem이 **CPU-visible mapping 외부의 writer 존재 가능성**을 보수적으로 고려하기 위해서다. DMA와 filesystem은 physical page를 통해 간접적으로 연결된다.

## CHAPTER 12 · DAX와 long-term pin은 storage block lifetime 때문에 충돌한다

DAX는 page cache를 거치지 않고 persistent storage를 memory mapping에 직접 연결할 수 있다. Long-term pin이 DAX backing을 붙잡으면 filesystem이 storage block layout을 변경하거나 revoke해야 할 때 일반 page-cache page처럼 쉽게 복사·이동할 수 없다. 그래서 long-term pin은 일부 DAX mapping에서 제한될 수 있다. 이 문제는 `RAM page를 못 옮긴다`를 넘어 **filesystem extent ownership까지 외부 device lifetime이 묶어버리는 문제**다.

## CHAPTER 13 · Pin된 page는 migration과 compaction의 후보 집합을 줄인다

Memory compaction은 movable page를 옮겨 큰 contiguous range를 만들고, NUMA balancing이나 tiering도 page migration을 사용한다. Long-term pin이 많으면 물리 메모리에 unmovable island가 생겨 free memory가 충분해도 huge-page allocation이나 memory offlining이 실패할 수 있다. Pin budget은 process RSS와 별개의 **physical placement fragmentation budget**으로 관리해야 한다. DMA registration이 많아질수록 system-wide allocator behavior가 바뀔 수 있다.

## CHAPTER 14 · Memory hot-remove는 pin lifetime이 hardware lifecycle과 충돌하는 대표 사례다

Memory block을 offline하려면 해당 range의 movable pages를 다른 곳으로 migration해야 한다. Long-term pin이 남아 있으면 그 page를 안전하게 옮길 수 없어 hot-remove가 중단된다. 특히 VFIO/RDMA처럼 user process가 오래 등록한 buffer가 infrastructure maintenance를 막을 수 있다. Hotplug-capable system에서는 registration API에 revoke/timeout/quiesce mechanism이 없으면 **application lifetime이 hardware service lifetime보다 강해지는 구조**가 된다.

## CHAPTER 15 · NUMA placement와 memory tiering은 pin 이후에는 선택 자유도가 크게 줄어든다

처음 page fault가 일어난 node가 최적이 아니어도 kernel은 access pattern을 보고 page를 다른 NUMA node나 faster memory tier로 옮길 수 있다. 하지만 device가 physical page를 pin하면 migration이 제한되어 잘못된 초기 placement가 장시간 고정된다. NIC/GPU와 가까운 node에 buffer를 배치하지 못하면 PCIe/NUMA interconnect traffic과 latency가 늘어난다. 따라서 long-term registration은 **먼저 placement를 결정하고 나중에 pin**하는 순서를 가져야 한다.

## CHAPTER 16 · MMU notifier는 pin을 줄이고 device page table을 invalidation에 참여시킨다

Device가 자체 page table을 갖거나 IOMMU/SVA를 통해 process address space를 mirror하는 경우, 모든 page를 영구 pin하는 대신 MMU notifier로 CPU mapping 변화에 참여할 수 있다. Kernel이 range를 invalidate하기 전에 driver가 device access를 중지하고 device PTE를 제거하면 VM은 page를 migration/unmap할 수 있다. 이 모델은 pinning보다 복잡하지만 **address-space mutation protocol에 device를 participant로 포함**시켜 mobility를 회복한다.

## CHAPTER 17 · Replayable device fault는 eager pin 대신 demand paging을 가능하게 한다

일부 accelerator는 invalid device mapping 접근에서 fault를 발생시키고 요청을 나중에 replay할 수 있다. 그러면 CPU처럼 page fault 시 backing을 확보하고 device page table을 갱신한 뒤 operation을 재개할 수 있어 모든 working set을 사전에 pin할 필요가 없다. 하지만 fault handler latency, recursive memory pressure, device queue backpressure가 새 failure mode가 된다. `pin을 없앴다`가 아니라 **lifetime complexity를 fault/replay protocol로 이동**시킨 것이다.

## CHAPTER 18 · RDMA memory registration은 remote peer가 local page lifetime에 영향을 주는 구조다

RDMA는 local CPU가 system call을 수행하지 않아도 remote operation이 registered memory를 직접 읽고 쓸 수 있다. Registration key가 유효한 동안 page와 translation이 안정적이어야 하므로 long-term pin의 대표 사례다. Process가 buffer를 free하거나 mapping을 바꿨더라도 device/remote access가 quiesce되지 않았다면 physical page를 재사용할 수 없다. Deregistration은 **remote access 차단→device queue drain→translation revoke→unpin** 순서를 가져야 한다.

## CHAPTER 19 · VFIO passthrough는 untrusted userspace device control과 pin budget을 동시에 관리해야 한다

Userspace VM/process가 physical device를 직접 제어하면 guest/userspace memory를 IOMMU에 map하기 위해 pages를 pin할 수 있다. 여기서 IOMMU isolation이 DMA target 범위를 제한하고 pin lifetime이 physical backing을 안정화한다. 둘 중 하나라도 깨지면 device가 해제된 page나 다른 process memory를 DMA할 수 있다. VFIO teardown은 file descriptor close만으로 끝나는 게 아니라 **device stop, DMA mapping removal, IOMMU invalidation, unpin completion**을 보장해야 한다.

## CHAPTER 20 · GPU/accelerator buffer는 CPU mapping과 device execution lifetime이 비대칭이다

CPU가 command submission을 끝냈다고 GPU가 buffer 사용을 끝낸 것은 아니다. Queue에 제출된 work가 fence를 signal하기 전까지 device가 page를 계속 참조할 수 있다. Userspace가 mapping을 munmap하거나 object를 free해도 kernel driver가 fence/reference를 통해 storage lifetime을 유지해야 한다. Buffer ownership은 `submission 함수 return`이 아니라 **device completion event**를 기준으로 이전된다.

## CHAPTER 21 · Direct I/O의 pin은 짧아도 completion 전에는 절대 풀 수 없다

Direct I/O는 page cache copy를 피하기 위해 userspace buffer page를 block I/O 동안 pin할 수 있다. Pin duration이 RDMA보다 짧아도 async I/O에서는 syscall return과 device completion이 다르므로 lifetime proof는 동일하게 필요하다. Early unpin 뒤 process가 page를 unmap/reuse하면 storage device가 다른 data를 덮어쓸 수 있다. I/O request object가 pin ownership을 갖고 completion path가 유일한 unpin owner가 되는 구조가 안전하다.

## CHAPTER 22 · IOMMU mapping lifetime과 page pin lifetime은 별도 counter지만 함께 종료돼야 한다

Physical page를 pin했다고 device가 자동으로 접근 가능한 것은 아니고, IOMMU에 IOVA→physical mapping을 설치해야 할 수 있다. 반대로 IOMMU mapping만 남아 있고 page를 먼저 unpin하면 VM이 physical page를 다른 용도로 재사용한 뒤 device가 stale mapping으로 접근할 수 있다. Teardown의 안전한 방향은 **새 DMA 차단→in-flight completion→IOMMU unmap/invalidate→page unpin**이다. Hardware IOTLB invalidation completion까지 포함해야 한다.

## CHAPTER 23 · mm teardown은 process exit와 device lifetime을 rendezvous시켜야 한다

Process가 exit하면 userspace page table은 사라질 수 있지만 device work가 자동으로 즉시 멈추는 것은 아니다. Driver가 `mm_struct`나 notifier registration을 참조한다면 exit path와 device callback 사이 reference lifetime이 필요하다. Exit가 무한히 device drain을 기다리면 unkillable process가 되고, drain 없이 memory를 free하면 DMA-after-free가 된다. Timeout/reset/escalation policy를 포함해 **process death가 device ownership을 어떻게 회수하는지**를 설계해야 한다.

## CHAPTER 24 · Device reset은 completion event를 잃어버릴 수 있어 pin cleanup을 더 어렵게 만든다

Normal path에서는 request completion이 unpin owner지만 device reset, PCIe error, firmware crash가 발생하면 completion이 오지 않을 수 있다. Reset handler가 outstanding request table을 walk해 DMA를 확실히 중지한 뒤 mapping과 pin을 회수해야 한다. Hardware가 실제로 bus mastering을 멈췄다는 보장 없이 software request만 free하면 stale DMA가 새 allocator owner의 memory를 손상시킬 수 있다. Fatal reset path가 normal completion과 동일 lifetime ledger를 정리해야 한다.

## CHAPTER 25 · Cancellation은 “사용자가 더 이상 관심 없다”와 “device가 더 이상 접근하지 않는다”를 구분해야 한다

Async API의 cancel 성공이 logical result delivery만 취소하는지, hardware operation까지 abort하고 DMA가 끝났음을 보장하는지 명확해야 한다. Soft cancellation 후 device가 계속 buffer를 사용한다면 application-visible request는 사라져도 pin ownership은 completion까지 유지해야 한다. Cancellation token lifetime과 DMA buffer lifetime을 하나의 boolean로 합치면 early free가 발생한다. API contract에 **cancel acknowledgement와 resource-quiesced state를 분리**해야 한다.

## CHAPTER 26 · Pinning은 security boundary가 아니라 lifetime primitive다

Page를 pin했다고 caller가 그 memory를 읽거나 쓸 권한을 자동으로 얻는 것은 아니다. Pin acquisition 시 userspace access permission과 write intent를 검증해야 하고, device DMA는 IOMMU/driver authorization으로 별도 제한해야 한다. Shared process나 inherited descriptor가 등록 handle을 넘길 수 있다면 registration object 자체가 capability가 된다. Lifetime correctness와 authorization을 섞지 말고 두 proof를 모두 요구해야 한다.

## CHAPTER 27 · Pin leak은 RSS보다 system mobility 지표에서 먼저 드러날 수 있다

Pin된 page가 정상적으로 unpin되지 않으면 process RSS만 봐서는 원인을 놓칠 수 있다. `/proc/vmstat`의 pin acquire/release accounting, page/folio dump, driver request table, IOMMU mapping count를 함께 비교하면 lifetime ledger 불일치를 찾을 수 있다. 특히 `acquired - released`가 steady state에서 지속 증가하면 request completion/error cleanup 누락을 의심해야 한다. Leak 검사는 allocator leak detector와 별도로 **DMA pin accounting**을 관측해야 한다.

## CHAPTER 28 · Long-term pin은 memory pressure를 free-page 부족 이상으로 악화시킨다

Pinned memory는 swap/reclaim/migration 대상에서 빠질 수 있어 같은 사용량의 ordinary anonymous memory보다 system flexibility를 더 크게 줄인다. Reclaim이 movable page만 반복해서 scan하고 compaction이 실패하면 latency와 CPU cost가 증가한다. Container/cgroup memory limit 안에 pin byte가 들어간다고 해도 host 전체의 contiguous-memory/hotplug/tiering 능력을 소모할 수 있다. Admission control은 byte quota뿐 아니라 pin duration과 mobility impact를 고려해야 한다.

## CHAPTER 29 · 좋은 설계는 가능한 한 permanent pin을 protocol로 대체한다

Short-lived operation은 completion-scoped pin, long-lived shared address space는 MMU notifier/SVA, fault-capable accelerator는 replayable page fault처럼 **필요한 안정성 범위만 보장하는 mechanism**을 선택한다. 모든 buffer를 startup에서 영구 pin하면 구현은 단순해 보이지만 VM의 reclaim/migration/hotplug 기능을 대가로 지불한다. Device가 정말 physical identity를 필요로 하는 기간을 줄이는 것이 pin optimization의 핵심이다.

## CHAPTER 30 · Page pin correctness는 ownership ledger로 증명해야 한다

한 DMA buffer를 검토할 때 `누가 pin했는가`, `write intent는 무엇인가`, `FOLL_LONGTERM인가`, `IOMMU mapping owner는 누구인가`, `in-flight device work는 어떻게 세는가`, `cancel/reset/exit에서 누가 drain하는가`, `dirty state는 누가 반영하는가`, `unpin의 유일 owner는 누구인가`를 모두 답할 수 있어야 한다. 어느 한 경로에서 ownership이 사라지면 pin leak이고, 두 경로가 동시에 free하면 use-after-unpin이다. **Page pinning은 pointer API가 아니라 VM과 device 사이의 lifetime transaction**이다.
