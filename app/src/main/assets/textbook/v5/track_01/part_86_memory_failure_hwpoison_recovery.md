# PART 86 · Memory Failure Recovery — hwpoison, page isolation, SIGBUS, soft-offline, unrecoverable kernel state

ECC/RAS hardware가 memory corruption을 감지했다고 해서 OS의 문제가 끝나는 것은 아니다. Correctable error는 hardware가 data를 복구할 수 있지만 반복 발생은 fragile page의 신호가 될 수 있고, uncorrectable error는 이미 어떤 physical page의 contents를 신뢰할 수 없다는 뜻이다. Linux memory-failure path는 해당 PFN이 어떤 page type과 owner에 속하는지 판정하고, reload/migration 가능한 data는 우회하며, user process가 corrupted contents를 소비하지 못하게 mapping을 끊고, kernel-owned state처럼 복구 불가능한 경우에는 panic policy까지 고려한다. 핵심은 **corruption detection을 physical-page lifetime과 software ownership graph에 연결해 더 넓은 silent corruption으로 번지는 것을 막는 것**이다.

## CHAPTER 01 · Corrected error와 uncorrectable corruption은 software response가 다르다

Single-bit 수준처럼 ECC가 data를 복원한 correctable event는 immediate data loss를 만들지 않을 수 있지만 같은 page/DIMM에서 rate가 올라가면 future uncorrectable failure 확률을 높이는 health signal이 된다. Uncorrectable error는 해당 cache line/page contents의 정확성을 보장할 수 없으므로 software가 그냥 다시 읽어 계속 사용해서는 안 된다. RAS policy는 `고쳐졌으니 무시`와 `오류니까 즉시 node kill` 사이에서 error class와 page ownership을 구분해야 한다. **현재 data integrity와 미래 hardware reliability를 별도 축으로 관측**해야 한다.

## CHAPTER 02 · Hwpoison bit는 physical page를 allocator/reuse 경로에서 격리하는 software marker다

Hardware가 특정 PFN을 corrupted로 보고하면 kernel은 page에 poison state를 남겨 정상 allocator나 page-cache reuse가 그 storage를 다시 authoritative data로 사용하지 않게 한다. Poison marking은 corrupted byte를 복구하는 것이 아니라 **이 physical storage를 신뢰 대상에서 제외하는 ownership transition**이다. 이미 mapping된 users와 in-flight references가 있을 수 있어 bit 하나만 세팅한다고 격리가 끝나지 않는다. 이후 handler는 page type과 mapping을 찾아 consumer들을 끊고 reference가 정리되는지 확인해야 한다.

## CHAPTER 03 · memory_failure handler는 asynchronous physical event를 VM object model로 변환한다

Machine check나 firmware RAS notification은 physical address/PFN 중심으로 오류를 전달하지만 kernel recovery는 anonymous memory, page cache, HugeTLB, swap, kernel page 같은 software object 의미를 알아야 한다. `memory_failure()` 계열은 page flags, mapping, LRU/refcount state를 조사해 어느 recovery action이 가능한지 선택한다. Error가 allocation/migration과 동시에 발생할 수 있어 정상 VM locking assumptions가 흔들리는 rare path다. **Physical fault identity→page state→software owner→recovery action**의 변환이 이 subsystem의 핵심이다.

## CHAPTER 04 · Page isolation은 corrupted page를 새 allocation과 migration target에서 떼어낸다

Recovery 중 page가 allocator free list나 migration target으로 다시 들어가면 corruption이 전혀 다른 process/object로 전파된다. Handler는 page state를 안정화하고 가능한 경우 LRU/allocator paths에서 분리해 ownership을 고정한 뒤 recovery를 진행한다. 동시에 다른 CPU가 page를 free/migrate하는 race가 있으므로 refcount와 page lock 상태를 보수적으로 다뤄야 한다. Isolation failure는 단순 `복구 못 함`보다 위험하다. **Corrupted storage가 owner를 바꾸는 것을 먼저 막아야** 후속 kill/reload 결정이 의미가 있다.

## CHAPTER 05 · Anonymous private page는 reload source가 없으면 process state 일부를 잃은 것이다

Heap/stack anonymous page가 uncorrectable corruption을 맞으면 backing file에서 pristine copy를 다시 읽을 수 없다. Swap에 최신 valid copy가 없고 application-level redundancy도 없다면 해당 logical state는 이미 손실됐다. Kernel은 mapping을 unmap/poison하고 affected process가 page에 접근할 때 SIGBUS를 주거나 policy에 따라 early notification을 할 수 있다. Process가 signal을 잡는다고 lost bytes가 자동 복원되는 것은 아니며 **application이 object를 재생성할 별도 recovery source가 있는 경우에만 gentle recovery**가 가능하다.

## CHAPTER 06 · Clean file-cache page는 backing store에서 다시 읽을 수 있어 transparent recovery 가능성이 높다

Page cache의 clean folio가 corrupted됐고 storage copy가 정상이라면 bad physical page를 버리고 future fault/read에서 backing file을 새 page로 다시 읽을 수 있다. 이 경우 application data semantics를 잃지 않고 physical page만 교체할 수 있다. 하지만 file 자체가 memory-mapped device/DAX이거나 storage data도 동일 hardware fault domain에 있으면 이 가정이 성립하지 않는다. Page type을 보는 이유는 **authoritative copy가 다른 곳에 존재하는지**를 판단하기 위해서다.

## CHAPTER 07 · Dirty file-cache page는 memory가 유일한 최신 copy일 수 있어 훨씬 어렵다

Dirty page는 storage보다 memory contents가 최신이므로 corruption을 발견했을 때 disk에서 다시 읽으면 application의 최신 update를 되돌리게 된다. Writeback으로 corrupted bytes를 storage에 내보내면 persistent corruption을 전파한다. 따라서 handler/filesystem은 page를 정상 writeback 대상으로 계속 취급하지 않고 error를 기록해 application에 persistence failure를 전달해야 한다. **Dirty state + hwpoison은 volatile corruption이 durable store로 번지지 않게 막아야 하는 cross-subsystem failure**다.

## CHAPTER 08 · Mapping revoke는 CPU가 corrupted page를 더 이상 정상 load/store하지 못하게 만든다

Process page table에 poisoned physical page가 계속 present하면 application은 signal 없이 bad data를 소비하고 그 값을 network/storage에 전파할 수 있다. Recovery는 가능한 mappings를 찾아 unmap하거나 poisoned PTE semantics를 남겨 다음 access에서 fault/SIGBUS가 발생하도록 한다. 동일 page를 여러 processes가 shared mapping할 수 있어 owner 하나만 끊으면 충분하지 않다. Reverse mapping traversal은 **physical page→모든 virtual consumers**를 찾아 corruption consumption을 차단하는 과정이다.

## CHAPTER 09 · Early kill과 late kill은 notification timing policy다

Early-kill mode는 corruption이 감지된 시점에 affected process/thread에 SIGBUS를 보내 application이 object를 버리거나 failover할 시간을 준다. Late-kill mode는 mapping을 poison한 뒤 process가 실제 corrupted page를 다시 access할 때 signal을 발생시켜 불필요한 process termination을 줄일 수 있다. 어떤 방식이 맞는지는 workload가 memory-error-aware한지, state isolation을 할 수 있는지에 달렸다. **오류 존재와 오류 소비 시점을 분리**해 policy를 선택해야 한다.

## CHAPTER 10 · SIGBUS code는 address error와 machine-check origin을 구분하는 recovery metadata다

Memory failure signal은 ordinary invalid mapping의 SIGBUS와 같은 signal number를 사용하더라도 `BUS_MCEERR_AO/AR` 같은 code와 fault address로 asynchronous/synchronous machine-check context를 전달할 수 있다. Application이 이를 처리하려면 signal-safe path에서 affected object 범위를 식별하고 더 이상 해당 data를 사용하지 않게 해야 한다. Signal handler가 allocator나 poisoned memory 자체를 참조하면 second fault로 무너질 수 있다. Error-aware process는 **signal metadata→logical object ownership→application failover** chain을 미리 설계해야 한다.

## CHAPTER 11 · PR_MCE_KILL은 process/thread별 notification strategy를 선택하게 한다

System global early/late policy가 모든 workload에 적합하지 않기 때문에 process는 `PR_MCE_KILL` 계열로 early/late/default mode를 선택할 수 있다. Dedicated recovery thread가 SIGBUS를 처리하게 구성할 수도 있지만 thread-local setting과 main thread semantics를 정확히 이해해야 한다. Error-aware database/VM monitor와 ordinary CLI process는 필요한 recovery behavior가 다르다. RAS policy를 application에 노출할 때는 **signal 받을 thread와 corrupted object ownership이 실제로 연결되는지** 확인해야 한다.

## CHAPTER 12 · KVM은 host physical memory error를 guest-visible machine check로 번역할 수 있다

Guest memory backing page가 poisoned되면 host QEMU/KVM process만 죽이는 대신 guest physical address와 error type을 이용해 guest에 machine check를 주입하는 경로가 가능하다. 그러면 guest OS나 application이 자신의 redundancy 정책으로 대응할 수 있다. 하지만 host page ownership, guest mapping, memslot translation이 정확해야 wrong guest address에 error를 주지 않는다. Virtualization은 hwpoison을 숨기는 것이 아니라 **host PFN fault를 guest memory-failure semantics로 다시 표현**한다.

## CHAPTER 13 · Transparent Huge Page 안의 single bad page는 split/migration cost를 만든다

큰 folio/THP 일부에서 correctable error가 반복되어 soft-offline하려면 전체 huge mapping을 그대로 유지한 채 작은 physical subpage만 교체하기 어려울 수 있다. Kernel은 huge page를 split하고 문제 page만 migration해 reliability를 높일 수 있지만 TLB reach와 memory locality 이득을 잃는다. 따라서 RAS event 하나가 application-visible latency/throughput degradation으로 이어질 수 있다. **Reliability recovery가 memory-layout optimization을 해체할 수 있다는 비용**을 capacity planning에 포함해야 한다.

## CHAPTER 14 · HugeTLB soft-offline은 reserved hugepage pool capacity까지 줄일 수 있다

HugeTLB page의 한 raw page가 suspect하면 전체 hugepage를 다른 hugepage로 migration한 뒤 original hugepage를 dissolve해야 할 수 있다. Migration target으로 free hugepage 하나가 필요하고 original pool capacity가 줄어 application이 reservation을 만족하지 못할 수 있다. Low-latency/database workload는 memory error가 process kill 없이 복구됐더라도 hugepage shortage로 후속 장애를 겪을 수 있다. RAS telemetry는 **poisoned bytes보다 hugepage pool 감소량과 migration 성공률**도 추적해야 한다.

## CHAPTER 15 · Soft-offline은 아직 data loss가 없는 suspect page를 예방적으로 퇴역시킨다

Correctable ECC가 과도하게 반복되는 page는 아직 contents가 정상이라면 새 physical page로 migration해 application을 중단하지 않고 위험한 storage를 사용 대상에서 제거할 수 있다. 이 예방적 회수는 uncorrectable failure 전에 reliability margin을 만드는 방법이다. 하지만 migration bandwidth, hugepage split, free target requirement가 비용이므로 무제한 적용은 performance/capacity를 해칠 수 있다. Soft-offline은 **corrected-error rate를 preventive migration decision으로 변환하는 control policy**다.

## CHAPTER 16 · enable_soft_offline은 reliability와 capacity/performance trade-off를 명시적 정책으로 만든다

Soft-offline을 활성화하면 RAS collector/platform driver가 suspect pages를 이동시키려 하지만 THP/HugeTLB처럼 page type별 비용이 다르다. 비활성화하면 corrected-error가 많은 page도 그대로 사용해 capacity를 보존하지만 future uncorrectable risk를 감수한다. Fleet 정책은 단순 on/off보다 workload의 hugepage dependency, spare capacity, DIMM replacement automation을 함께 고려해야 한다. **하드웨어 오류를 숨기는 옵션이 아니라 예방 조치의 비용을 어디까지 지불할지 결정하는 정책**이다.

## CHAPTER 17 · Kernel-owned page corruption은 user page보다 복구 범위가 훨씬 제한적이다

Page table, slab object, kernel text/data, large kmalloc 같은 kernel-owned state는 특정 process mapping을 끊고 다시 fault시키는 방식으로 복구하기 어렵다. Corrupted lock pointer나 page table entry를 계속 사용하면 이후 임의 memory corruption으로 번지고 원래 RAS evidence와 crash가 멀어질 수 있다. 그래서 일부 policy는 recovery 불가능 kernel page fault에서 즉시 panic해 kdump를 남기는 쪽을 선택한다. **Availability보다 integrity와 diagnosability가 중요하면 early node failure가 더 안전**할 수 있다.

## CHAPTER 18 · panic_on_unrecoverable_memory_failure는 delayed corruption 대신 deterministic failure를 선택한다

Kernel이 복구 불가능한 ownership type을 식별했을 때 계속 실행하면 corrupted data가 언제 소비될지 알 수 없고 crash stack도 원인과 무관해질 수 있다. Immediate panic policy는 service availability를 잃지만 fault PFN/page state/MCE context가 남아 있는 시점에 vmcore를 생성할 수 있다. HA cluster에서는 node failover가 silent corruption propagation보다 나을 수 있다. 이 설정은 `panic을 좋아한다`가 아니라 **failure containment boundary를 node 수준으로 명시**하는 운영 선택이다.

## CHAPTER 19 · PageSlab·PageTable 같은 type 판정은 recovery capability의 핵심 입력이다

Memory failure handler는 page flags와 allocator ownership을 보고 anonymous/file/LRU/kernel-owned 여부를 분류한다. Page가 migration/compaction 중 LRU에서 잠시 빠져 있는 transient state와 진짜 kernel unmovable object를 혼동하면 정상 user page 때문에 panic하거나, 반대로 corrupted kernel object를 recoverable로 오판할 수 있다. 그래서 aggressive panic policy도 classification certainty가 있는 population에만 적용하는 편이 안전하다. **Recovery action의 강도는 page-type evidence confidence와 연결**되어야 한다.

## CHAPTER 20 · Swap-cache와 transient VM state는 authoritative copy 판단을 복잡하게 만든다

Anonymous page가 swap cache에 있으면서 dirty/writeback/migration 상태일 수 있고, swap slot이 최신 valid data인지 memory page가 최신인지 시점에 따라 달라진다. Memory error가 이런 transition 중 발생하면 단순 `swap 있으니 reload` 규칙으로는 update loss를 막을 수 없다. Handler는 normal VM locks/state를 최대한 따르며 concurrent transition이 끝나기를 기다리거나 recovery를 포기할 수 있다. Rare error path일수록 **normal ownership protocol을 우회하지 않는 보수적 synchronization**이 중요하다.

## CHAPTER 21 · DAX/persistent-memory corruption은 page cache reload 모델과 다르다

DAX mapping은 storage media를 page cache 없이 직접 CPU address space에 연결할 수 있어 `clean page를 disk에서 다시 읽는다`는 복구 모델이 적용되지 않는다. Poisoned persistent range는 filesystem extent와 application mapping에 직접 대응하므로 storage-level bad block management, filesystem recovery, SIGBUS semantics가 함께 필요하다. Persistent memory error를 ordinary DRAM poison처럼만 처리하면 authoritative copy가 어디 있는지 잘못 판단한다. **Storage identity와 memory identity가 같은 physical media에 겹치는 failure domain**이다.

## CHAPTER 22 · Memory hot-remove와 hwpoison은 모두 page를 사용 대상에서 빼지만 목적과 granularity가 다르다

Memory block offlining은 maintenance/hardware removal을 위해 큰 range의 movable pages를 migration하고 block 전체를 offline한다. Hwpoison은 특정 corrupted raw page의 재사용을 금지하는 error isolation이다. Suspect DIMM에서 poison page가 계속 늘면 individual recovery보다 memory block/node offline과 hardware replacement가 적합할 수 있다. 두 mechanism을 구분하면서 **page-level symptom을 hardware FRU-level action으로 aggregation**해야 fleet reliability policy가 완성된다.

## CHAPTER 23 · Delayed/failed/ignored/recovered counters는 handler outcome을 분리해 보여준다

NUMA node memory-failure statistics는 raw poisoned pages가 총 몇 개였는지뿐 아니라 recovery가 delayed, failed, ignored, recovered됐는지를 구분할 수 있다. `poison count 100`만 보면 모두 안전하게 격리된 것인지 50개가 recovery 실패한 것인지 알 수 없다. Outcome ratio와 page type, DIMM/physical location을 함께 봐야 handler capability gap과 hardware degradation을 분리할 수 있다. **Error detection count와 successful containment count는 동일 metric이 아니다.**

## CHAPTER 24 · 같은 physical error의 중복 report는 recovery를 idempotent하게 처리해야 한다

Firmware, machine-check bank, retry path가 동일 PFN error를 여러 번 보고할 수 있고 handler가 이미 PG_hwpoison으로 격리한 page를 다시 처리할 수 있다. 중복 event가 refcount를 두 번 감소시키거나 signal을 무한 발생시키면 recovery 자체가 corruption 원인이 된다. Physical page generation/state를 확인해 already-poisoned event를 idempotent하게 처리하고 telemetry에서는 occurrence count와 unique PFN count를 분리한다. **Error handler도 at-least-once event delivery를 견디는 state machine**이어야 한다.

## CHAPTER 25 · Migration/compaction 중 error는 page ownership 전환과 충돌한다

Page contents가 source에서 destination으로 복사되고 page-table mapping이 전환되는 순간 hardware error가 source/destination 어느 PFN에 발생했는지에 따라 recovery 의미가 달라진다. Handler가 stale mapping/refcount를 따라 wrong page를 poison하면 정상 destination을 격리하거나 bad source를 재사용할 수 있다. Normal migration locks/refcounts와 hwpoison serialization을 최대한 공유하는 이유가 여기에 있다. **Physical fault arrival은 VM transaction 경계를 존중하지 않으므로 handler가 transaction state를 해석해야 한다.**

## CHAPTER 26 · Pinned/DMA-owned page corruption은 device lifetime과 recovery 가능성을 제한한다

Long-term pin된 page는 CPU mapping만 끊어도 device가 IOMMU/DMA address로 계속 접근할 수 있다. Hwpoison handler가 page를 migration하고 싶어도 pin 때문에 physical identity를 바꿀 수 없고, device reset/deregistration이 먼저 필요할 수 있다. RDMA/VFIO/GPU buffer의 corruption은 process SIGBUS만으로 끝나지 않는다. **Page pin owner→device queue→IOMMU mapping을 quiesce한 뒤 physical page를 retire**해야 완전한 containment가 된다.

## CHAPTER 27 · EDAC/MCE와 VM hwpoison telemetry를 같은 incident로 연결해야 한다

EDAC는 controller/DIMM/channel 수준의 correctable/uncorrectable event를 제공하고 machine-check record는 physical address와 syndrome 정보를 줄 수 있다. VM은 그 PFN이 어느 page type/process/file에 속했는지 recovery 결과를 안다. 두 로그를 분리하면 hardware owner는 application impact를 모르고 application team은 어떤 DIMM을 교체할지 모른다. Incident pipeline은 **hardware location→PFN→page owner→process/guest impact→recovery outcome**을 하나의 correlation ID로 연결해야 한다.

## CHAPTER 28 · Fault injection은 실제 ECC 고장 없이 rare recovery path를 검증하는 수단이다

`MADV_HWPOISON`, debugfs hwpoison injector, architecture-specific MCE injector를 이용해 anonymous/file/hugepage/memcg 등 특정 page type에 controlled fault를 넣을 수 있다. Test는 signal이 왔는지만 보지 말고 mapping revoke, page reuse 금지, application recovery, node statistics, repeated injection idempotency를 확인해야 한다. Injection filter가 잘못 설정되면 unrelated critical page를 poison할 수 있으므로 test environment와 target PFN ownership을 엄격히 제한한다. **Rare-path testing도 blast radius control이 필요**하다.

## CHAPTER 29 · Recovery policy는 workload redundancy와 failover topology에 맞춰야 한다

Memory-aware database가 corrupted cache object를 버리고 replica에서 재구성할 수 있다면 early SIGBUS recovery가 node uptime을 지킬 수 있다. 반대로 kernel page corruption이나 stateful process가 lost anonymous page를 복구할 수 없다면 빠른 process/node failover가 더 안전하다. Global sysctl 하나로 모든 workload의 최적 policy를 정하기 어렵다. **Physical memory error를 application-level durability/redundancy 모델과 연결해 kill/process-recover/panic 선택**을 해야 한다.

## CHAPTER 30 · Memory-failure correctness는 corruption consumption을 끊고 ownership을 새 storage로 수렴시키는 proof다

한 hwpoison incident를 승인하려면 `어느 hardware event가 어느 PFN을 가리키는가`, `그 page type과 authoritative copy는 무엇인가`, `모든 CPU/process/device mappings가 언제 차단됐는가`, `reload/migration 가능한 경우 새 page가 언제 authoritative해졌는가`, `불가능하면 어떤 process/guest/node가 실패로 전환됐는가`, `old PFN이 allocator에 다시 들어오지 않는가`, `recovery outcome과 hardware FRU가 telemetry로 연결되는가`를 답해야 한다. **Memory RAS의 목표는 error를 숨기는 것이 아니라 corrupted physical storage가 software state에 더 이상 영향 주지 못하도록 ownership graph를 끊는 것**이다.
