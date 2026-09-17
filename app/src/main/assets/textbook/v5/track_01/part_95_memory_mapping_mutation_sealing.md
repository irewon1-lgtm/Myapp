# PART 95 · Memory Mapping Mutation and Sealing — mmap lifetime, mprotect, mremap, MAP_FIXED, memfd seals, SIGBUS races

`mmap()`은 file bytes를 pointer로 바꾸는 편의 함수가 아니라 process virtual address space에 새로운 lifetime object를 만드는 operation이다. Mapping은 생성 뒤 원래 fd를 닫아도 살아 있을 수 있고, `mprotect()`로 권한이 쪼개지며, `munmap()`으로 VMA가 분할되고, `mremap()`으로 주소가 이동하며, backing file의 truncate와 경쟁하면 정상 pointer dereference가 SIGBUS로 바뀔 수 있다. `memfd_create()`와 seals를 결합하면 pathname 없이 공유 가능한 immutable generation을 만들 수 있지만 기존 writable mapping과 future-write seal의 관계를 이해하지 못하면 “읽기 전용 공유”가 실제로는 수정 가능한 상태로 남는다. P95는 memory mapping을 단순 I/O 방식이 아니라 **virtual-range identity, backing-object generation, protection state, reference lifetime을 함께 관리하는 address-space transaction**으로 다룬다.

## CHAPTER 01 · Mapping은 pointer가 아니라 `[virtual range → backing object + permissions + flags]` 계약이다

`mmap()` 성공 결과를 단순히 “메모리 주소 하나를 받았다”고 생각하면 이후 mutation을 설명하기 어렵다. Kernel은 page-aligned virtual range에 anonymous memory나 file-backed object를 연결하고 read/write/execute protection, private/shared behavior, offset을 VMA metadata로 관리한다. 실제 physical pages는 즉시 모두 존재하지 않을 수 있고 access 시 page fault로 채워질 수 있다. 같은 backing file도 서로 다른 offsets, protections, processes에서 여러 mappings를 가질 수 있다. 따라서 mapping identity는 시작 pointer 하나가 아니라 address interval과 backing generation의 조합이다. Allocator나 cache가 mapping base만 key로 저장하면 resize/mremap 후 stale length를 사용해 다른 VMA까지 접근할 수 있다. **Mapping object를 base, length, protection, flags, backing identity, generation으로 명시적으로 추적**해야 mutation과 cleanup을 안전하게 만들 수 있다.

## CHAPTER 02 · mmap의 addr는 보통 “희망 위치”이지 ownership이 확정된 주소 예약 선언이 아니다

`MAP_FIXED` 없이 `mmap(addr, ...)`에 non-NULL addr를 주면 kernel은 그 값을 hint로 취급하며 alignment와 기존 mappings를 고려해 다른 주소를 선택할 수 있다. ASLR과 allocator state에 따라 같은 실행에서도 결과가 달라질 수 있으므로 return address를 확인하지 않고 requested addr를 계속 쓰면 즉시 memory corruption으로 이어진다. 주소 hint는 locality나 layout preference에 사용할 수 있지만 correctness는 returned pointer에 기반해야 한다. 특정 virtual range를 다른 subsystem과 공유해야 한다면 단순 hint보다 reservation protocol과 `MAP_FIXED_NOREPLACE` 같은 충돌 감지 mechanism을 고려해야 한다. **Virtual address는 process-local resource이고 “원하는 숫자를 요청했다”는 사실만으로 소유권이 생기지 않는다.** Mapping layout을 serialization하지 않으면 concurrent mapper가 먼저 range를 차지할 수 있다.

## CHAPTER 03 · File offset alignment는 mapping의 page-granular translation 구조에서 나온다

File-backed mmap의 offset은 page size의 배수여야 하는 조건이 일반적이다. Application이 임의 byte offset 123부터 구조체를 보고 싶다면 mapping은 그보다 앞의 page-aligned offset에서 시작하고, userspace pointer에 delta를 더해 logical object 위치를 계산하는 방식이 필요하다. 이때 실제 mapped length는 requested logical length보다 커질 수 있으므로 overflow와 cleanup length를 정확히 관리해야 한다. Huge-page mapping은 더 큰 alignment 제약을 가질 수 있다. Alignment를 맞추기 위해 offset을 내림하면서 length 확장을 빼먹으면 마지막 bytes가 mapping 밖으로 나간다. **Logical byte range와 physical/page mapping range를 별도 변수로 유지**해야 한다. Unmap도 mmap에 사용한 page-aligned mapping base/extent를 기준으로 해야 logical pointer만 들고 cleanup하는 버그를 피할 수 있다.

## CHAPTER 04 · MAP_SHARED와 MAP_PRIVATE의 핵심 차이는 “처음 읽는 값”보다 write가 backing object에 어떻게 반영되는가다

둘 다 file의 같은 initial bytes를 볼 수 있지만 write fault 이후 semantics가 달라진다. `MAP_SHARED` writable mapping의 변경은 같은 shared mapping을 보는 다른 processes와 page cache에 반영될 수 있고 적절한 writeback/durability 절차를 거쳐 file에 저장될 수 있다. `MAP_PRIVATE` write는 copy-on-write private page를 만들어 process-local 변경으로 분기하므로 file 자체를 수정하는 transaction이 아니다. 하지만 private mapping이라고 file truncate race나 file-backed page fault semantics에서 완전히 독립 snapshot이 되는 것은 아니다. **Sharing policy와 version snapshot guarantee를 같은 것으로 착각하면 안 된다.** Immutable snapshot이 필요하면 writer exclusion, immutable inode generation, explicit copy 같은 별도 protocol을 사용해야 한다.

## CHAPTER 05 · MAP_SHARED 변경의 visibility와 durability는 같은 사건이 아니다

Process A가 shared mapping에 store한 뒤 process B가 같은 page의 변경을 볼 수 있는 문제와, crash/power-loss 뒤 storage에서 그 값이 살아남는 문제는 별개다. Cache coherence와 page cache는 visibility를 제공할 수 있지만 durable persistence는 `msync`, `fsync`, filesystem/device ordering 같은 별도 contract를 요구한다. CPU store가 성공한 순간 disk transaction이 완료됐다고 생각하면 crash consistency가 무너진다. 또한 mapped data와 file metadata의 durability ordering도 filesystem semantics에 따라 다를 수 있다. **Memory visibility, kernel dirty accounting, storage writeback, durable commit을 단계별로 분리**해야 한다. P54의 crash atomicity 원칙은 mmap writer에도 그대로 적용되며, pointer store syntax가 storage transaction을 단순하게 만들지 않는다.

## CHAPTER 06 · mmap 뒤 원래 fd를 닫아도 mapping은 독립 reference를 통해 계속 유효할 수 있다

성공한 file-backed mapping은 backing object에 필요한 reference를 확보하므로 원래 `mmap()`에 사용한 fd를 이후 `close()`해도 mapping이 즉시 사라지는 것은 아니다. 이 특성은 fd 수를 줄이는 데 유용하지만, programmer가 “fd가 닫혔으니 file lifetime도 끝났다”고 가정하면 unlink/rename/truncate와 결합해 혼란이 생긴다. 반대로 mapping을 `munmap()`했다고 다른 fd나 다른 process mapping까지 없어지는 것도 아니다. **fd lifetime, open-file description, inode/backing object, VMA lifetime은 서로 다른 reference graph**다. Resource accounting에서 mapping은 `/proc/<pid>/maps`에는 남아 있는데 application fd table에는 아무 entry도 없을 수 있다는 점을 이해해야 한다.

## CHAPTER 07 · File size보다 긴 mapping을 만들 수 있다는 사실과 끝까지 안전하게 읽을 수 있다는 사실은 다르다

Mapping length가 current file size보다 길다고 해서 그 전체 range가 valid file data로 존재하는 것은 아니다. File의 마지막 partial page에는 EOF 뒤의 zero-filled portion처럼 특별한 semantics가 있을 수 있지만, 완전히 EOF 바깥의 page에 접근하면 SIGBUS가 발생할 수 있다. Application이 header에 기록된 length를 검증하지 않고 거대한 file mapping을 만든 뒤 pointer walk를 하면 malformed/truncated input이 process signal crash로 바뀔 수 있다. Parser는 mapping address가 process에 존재한다는 것만 보지 말고 **logical file size와 every dereference range를 검증**해야 한다. mmap parser도 ordinary `read()` parser와 같은 bounds discipline이 필요하다.

## CHAPTER 08 · Concurrent truncate는 이미 얻은 pointer를 비동기적으로 위험한 주소로 바꿀 수 있다

Thread A가 file을 mmap해 pointer를 보유한 동안 thread B나 다른 process가 file을 더 작게 truncate하면 mapping object 자체가 즉시 사라지지 않아도 backing 범위 일부가 더 이상 valid file contents가 아닐 수 있다. 이후 A가 잘려 나간 page를 touch하면 SIGBUS가 발생할 수 있다. Lock-free reader가 “pointer를 이미 얻었으니 안전하다”고 믿는 cache에서 자주 놓치는 race다. File size를 access 직전에 `fstat()`해도 check 뒤 truncate가 가능하므로 TOCTOU가 남는다. **Reader lifetime 동안 file generation/size를 고정하는 protocol**이 필요하다. Immutable file을 새 inode로 publish하고 old mappings가 drain될 때까지 old inode를 보존하는 방식이 단순한 이유다.

## CHAPTER 09 · File growth는 mapping virtual length를 자동으로 늘려 주지 않는다

Writer가 file을 크게 `ftruncate()`했다고 기존 mapping의 VMA length가 저절로 확장되는 것은 아니다. Original mmap length 밖의 주소를 접근하는 것은 adjacent VMA나 unmapped range를 건드리는 별도 memory bug다. Growth를 반영하려면 새로운 mapping을 만들거나 `mremap()` 같은 explicit virtual-range mutation이 필요하다. 반대로 file이 mapping보다 작아졌을 때는 앞 장의 SIGBUS risk가 생긴다. **Backing object size와 virtual mapping extent는 독립 state variables**다. Shared file format이 dynamic growth를 허용한다면 readers는 generation, logical size, mapping capacity를 따로 관리하고 resize synchronization을 가져야 한다.

## CHAPTER 10 · munmap은 VMA를 통째로 지우는 것뿐 아니라 중간 range를 잘라 두 개로 분할할 수 있다

`munmap(addr, len)`은 page-aligned range를 address space에서 제거하며 기존 mapping의 일부만 제거하면 앞/뒤가 별도 VMAs처럼 남을 수 있다. Application의 mapping registry가 원래 one-record `[base,len]`만 보존하고 partial unmap을 허용하면 metadata와 kernel map이 달라진다. 이후 cleanup에서 이미 unmapped hole을 포함한 range를 다시 다루거나, stale pointer가 hole을 건너며 SIGSEGV를 만들 수 있다. Concurrent readers가 있는 상태에서 partial unmap은 memory reclamation problem이므로 hazard/epoch/stop-the-world 같은 synchronization이 필요하다. **Virtual address range 제거도 lifetime reclamation operation**이며 pointer를 다른 threads가 보유할 수 있으면 단순 syscall 한 번으로 안전성이 끝나지 않는다.

## CHAPTER 11 · mprotect는 permission bit 변경이면서 VMA topology를 더 잘게 쪼갤 수 있는 mutation이다

큰 read-write mapping의 중간 한 page만 `PROT_NONE`으로 바꾸면 kernel은 protection이 다른 구간을 표현하기 위해 VMA를 여러 pieces로 나눌 수 있다. 다시 permissions를 합치면 조건에 따라 VMAs가 merge될 수 있지만 application은 그 최적화를 전제로 하면 안 된다. 과도하게 작은 protection ranges를 반복 생성하면 VMA metadata 수와 page-table/TLB invalidation 비용이 커질 수 있고 system limit에 닿을 수도 있다. Guard pages, JIT code state, GC barriers에 `mprotect()`가 유용하지만 **permission change의 비용은 PTE bit 하나뿐이라는 모델은 틀리다.** Address-space metadata mutation과 TLB shootdown까지 포함해 측정해야 한다.

## CHAPTER 12 · Protection 위반의 signal은 mapping 존재 여부와 access 권한 failure를 구분해서 해석해야 한다

`PROT_NONE` 또는 read-only page에 잘못된 access를 하면 SIGSEGV가 발생할 수 있다. Debugger에서는 fault address가 mapped range 안에 있는지, VMA protection이 무엇인지, instruction이 read/write/execute 중 무엇을 시도했는지 확인해야 한다. Unmapped hole access와 permission fault는 둘 다 SIGSEGV로 보일 수 있지만 root cause는 다르다. File truncate에서 발생하는 SIGBUS와도 구분해야 한다. Signal handler로 모든 memory fault를 recover하려는 설계는 async context와 instruction restart 문제를 복잡하게 만든다. **Fault signal은 최종 symptom이고 VMA/backing/protection state를 함께 봐야 원인을 분류**할 수 있다.

## CHAPTER 13 · W^X는 writable과 executable mapping transition을 JIT lifecycle과 연결한다

Security hardening에서는 같은 memory가 동시에 writable+executable인 시간을 줄이는 W^X 정책이 중요하다. JIT runtime은 code bytes를 생성할 때 write permission이 필요하고 execution 단계에서는 execute permission이 필요하므로 `mprotect()` 전환이나 dual-mapping strategy를 사용한다. 단순히 RWX 한 번으로 열어두면 arbitrary write primitive가 code injection으로 이어질 위험을 키운다. 하지만 permission transition마다 TLB shootdown과 synchronization 비용이 생길 수 있어 code cache를 batch 단위로 publish하는 설계가 필요하다. **JIT code generation은 byte emission → instruction/cache coherence → permission publication → execution이라는 lifecycle**이며, protection flag는 그 state machine의 일부다.

## CHAPTER 14 · Protection keys는 같은 page-table permission 위에 thread-local access policy를 추가할 수 있다

지원 architecture에서 pkey 기반 memory protection은 page mapping에 protection key를 부여하고 thread-local register 상태로 access를 빠르게 제한하는 mechanism을 제공한다. `pkey_mprotect()`로 mapping을 분류하고 thread가 key permissions를 바꾸면 ordinary `mprotect()`보다 낮은 overhead로 domain switching을 구현할 수 있는 경우가 있다. 그러나 이것을 process isolation과 같은 security boundary로 과장하면 안 된다. 같은 process의 malicious code가 relevant instructions를 실행할 수 있는 threat model, signal/context switch에서 register state, library compatibility를 검토해야 한다. **Hardware feature가 빠른 access-control primitive를 제공해도 threat boundary와 lifecycle discipline은 application이 정의**해야 한다.

## CHAPTER 15 · MAP_FIXED는 원하는 주소를 얻는 대신 이미 있던 mapping을 파괴할 수 있는 강한 operation이다

`MAP_FIXED`는 caller가 지정한 주소 range를 강제로 사용하며 그 range와 겹치는 기존 mappings를 제거할 수 있다. Multi-threaded process에서 다른 thread가 방금 mapping한 object, thread stack, library mapping과 경쟁하면 catastrophic corruption을 일으킬 수 있다. `/proc/maps`를 읽어 비어 있음을 확인한 뒤 MAP_FIXED를 호출하는 것도 check와 map 사이 race가 있다. 그래서 일반 allocator가 단순 address preference를 위해 MAP_FIXED를 쓰는 것은 위험하다. **MAP_FIXED는 reservation ownership이 이미 증명된 range에만 사용하는 address-space surgery**로 취급해야 한다. Legacy code의 fixed-address assumptions는 ASLR과 modern runtime layout에서도 충돌할 수 있다.

## CHAPTER 16 · MAP_FIXED_NOREPLACE는 fixed-address reservation에서 silent clobber 대신 conflict detection을 제공한다

`MAP_FIXED_NOREPLACE`는 지정 range에 기존 mapping이 있으면 덮어쓰지 않고 실패하도록 하여 multi-threaded address reservation을 더 안전하게 만든다. Shared-memory runtime, emulator, GC가 특정 virtual layout을 필요로 할 때 “이미 누가 차지했는가”를 atomic mapping operation으로 확인할 수 있다. 하지만 old kernels에서 flag behavior/compatibility를 feature-detect해야 하고, 성공 뒤에도 그 range의 lifetime ownership을 중앙 registry에서 관리해야 한다. **NOREPLACE는 TOCTOU를 줄이는 admission primitive이지 이후 unmap/mremap race까지 자동 해결하는 lock이 아니다.** Address-space owner token을 mapping record와 함께 유지해야 한다.

## CHAPTER 17 · ASLR 환경에서 주소를 persistence key나 cross-process identity로 쓰면 안 된다

같은 executable과 file mapping도 process마다, 실행마다 다른 virtual address에 배치될 수 있다. Pointer value를 file에 저장하거나 IPC로 보내 다른 process에서 그대로 dereference하려는 설계는 fixed mapping을 강요하고 collision/security 문제를 만든다. Persistent shared structure는 pointer 대신 object-relative offset, index, handle을 저장하는 편이 relocatable하다. `mremap()`으로 mapping이 이동할 수도 있으므로 같은 process 안에서도 base-relative representation이 강하다. **Virtual address는 temporary view coordinate이지 object identity가 아니다.** Pointer persistence를 피하면 ASLR, remapping, crash recovery가 훨씬 단순해진다.

## CHAPTER 18 · mremap은 resize와 relocation을 하나의 operation으로 다루지만 pointer invalidation을 application 전체에 전파한다

`mremap()`은 existing mapping을 늘리거나 줄이고, 필요하면 `MREMAP_MAYMOVE`로 다른 virtual address로 이동시킬 수 있다. Return address가 old address와 다르면 mapping 내부를 가리키던 모든 raw pointers가 stale해질 수 있다. Relative offsets를 사용하면 base만 갱신하면 되지만 absolute pointer graph는 전체 fix-up이나 stop-the-world가 필요하다. 다른 threads가 old pointer를 읽는 동안 mapping을 move하면 use-after-unmap과 같은 race가 생긴다. **mremap은 allocator convenience가 아니라 global pointer topology mutation**이다. Resize coordination 없이 concurrent access와 섞으면 높은 확률로 드문 corruption이 된다.

## CHAPTER 19 · MREMAP_FIXED는 destination ownership과 source mutation을 동시에 다루므로 rollback 가정을 조심해야 한다

Fixed relocation은 mapping을 특정 destination에 옮길 수 있지만 destination collision과 source range state를 정확히 확인해야 한다. 최신 Linux에서는 특정 조건에서 여러 mappings/gaps를 포함하는 range를 move하는 기능까지 확장되었고, error가 발생할 경우 부분 완료 가능성이 문서화된 경우도 있다. 따라서 modern feature를 사용하는 code는 “mremap이 실패하면 address space가 완전히 원상복구돼 있다”는 단순 transaction 가정을 문서 기준으로 검증해야 한다. Version별 semantics도 달라질 수 있다. **Address-space mutation syscall의 failure atomicity를 임의로 추측하지 말고 사용한 flags와 kernel contract에 맞춰 recovery state를 검사**해야 한다.

## CHAPTER 20 · MREMAP_DONTUNMAP은 old range를 남긴 채 pages를 새 위치로 옮기는 특수한 fault-driven design을 가능하게 한다

`MREMAP_DONTUNMAP`은 새 address로 mapping을 이동하면서 old range를 즉시 unmap하지 않는 mode를 제공한다. 이후 old range 접근은 page fault를 만들고 userfaultfd handler가 이를 처리하도록 결합할 수 있어 non-cooperative migration, garbage collection 같은 고급 runtime design에 쓰일 수 있다. 하지만 “old와 new가 같은 data를 동시에 alias한다”는 단순 copy semantics가 아니다. Old range fault behavior와 registered userfaultfd range, zero-page fallback 등을 정확히 이해해야 한다. **이 기능은 pointer migration problem을 page-fault protocol로 바꾸는 mechanism**이며, handler lifetime과 race가 새 correctness boundary가 된다.

## CHAPTER 21 · MAP_POPULATE와 prefault는 latency 위치를 바꿀 뿐 physical memory를 공짜로 만들지 않는다

Lazy mapping은 실제 page access 때 fault가 발생해 latency가 분산된다. `MAP_POPULATE`나 madvise/prefault 전략은 일부 page-table/page-cache 작업을 mapping 시점에 앞당겨 request-time fault를 줄일 수 있지만 startup latency와 memory/I/O burst를 증가시킨다. Large mapping을 모두 prefault하면 사용하지 않을 pages까지 읽어 cache pollution과 memory pressure를 만들 수 있다. `MAP_NONBLOCK` 같은 historical/implementation-specific flag 조합도 실제 effect를 최신 documentation으로 확인해야 한다. **Fault timing은 workload scheduling 선택**이며 tail-latency와 warmup budget을 측정해 결정해야 한다.

## CHAPTER 22 · mlock과 mapped range의 resize는 locked-memory accounting을 같이 움직인다

`mlock()`으로 resident 보장을 요청한 mapping을 `mremap()`으로 확장하거나 이동하면 locked-memory amount와 resource limit interaction이 달라질 수 있다. Real-time or secret-handling code가 page fault를 피하기 위해 locking을 사용해도 RLIMIT_MEMLOCK과 system pressure를 무시할 수 없다. Mapping을 줄이거나 unmap할 때 lock state가 어떻게 해제되는지도 lifecycle에 포함해야 한다. “pointer가 mlocked니까 swap되지 않는다”는 목표와 “프로세스 종료 전 secret이 안전하게 zeroized된다”는 목표도 별개다. **Residency guarantee, address mapping, data lifetime을 서로 다른 properties로 관리**해야 한다.

## CHAPTER 23 · Huge-page mapping은 page size와 alignment, fragmentation failure mode를 바꾼다

HugeTLB나 transparent huge page와 관련된 mapping은 ordinary 4 KiB page assumptions를 그대로 적용하기 어렵다. Explicit huge-page mmap은 offset/length alignment와 pool availability 제약을 가지며 allocation failure가 훨씬 coarse하게 나타날 수 있다. Large TLB coverage는 throughput에 유리하지만 sparse touch, fragmentation, NUMA placement, mprotect granularity 비용을 키울 수 있다. Mapping registry가 system page size 하나만 전역 constant로 사용하면 huge mappings의 range arithmetic이 틀어진다. **페이지 크기는 mapping property로 다루고 allocation/permission/fault behavior를 실제 backing mode에 맞춰 검증**해야 한다.

## CHAPTER 24 · Anonymous mmap은 file이 없어도 VMA와 fault/reclaim lifetime을 동일하게 가진다

`MAP_ANONYMOUS`는 fd-backed file 없이 zero-initialized virtual memory를 만들지만 “그냥 malloc보다 큰 byte array”로만 보면 안 된다. Physical page는 demand fault로 생길 수 있고 fork 시 COW 대상이 되며 reclaim/swap, NUMA, mprotect, munmap rules를 그대로 받는다. Large allocator와 runtime은 brk보다 mmap을 사용해 independently releasable regions를 만들기도 한다. Anonymous mapping도 shared mode를 사용하면 processes 사이 IPC primitive가 될 수 있다. **Backing file 부재는 mapping lifecycle 부재를 뜻하지 않는다.** VMA count, page faults, reclaim cost를 heap allocator 분석에 포함해야 한다.

## CHAPTER 25 · memfd_create는 pathname 없는 RAM-backed file descriptor를 만들어 mapping과 fd-passing을 결합한다

`memfd_create()`는 anonymous file-like object를 만들고 ordinary file APIs와 mmap을 사용할 수 있게 한다. Temporary pathname race를 피하면서 shared memory, JIT/code artifact, immutable payload를 processes 사이에 전달하는 데 유용하다. Object는 fd/reference lifetime에 따라 존재하므로 creator가 fd를 넘긴 뒤 닫아도 receiver reference가 남아 있으면 계속 살아 있을 수 있다. `MFD_ALLOW_SEALING`을 사용하면 이후 mutation 권한을 단계적으로 닫는 seals를 추가할 수 있다. **memfd는 memory buffer가 아니라 file semantics를 가진 reference-counted kernel object**이므로 size, truncate, mapping, seals를 함께 설계해야 한다.

## CHAPTER 26 · F_SEAL_GROW와 F_SEAL_SHRINK는 file-size mutation을 막아 mapping range assumptions을 안정화한다

Shared memfd consumer가 header에 기록된 size와 mapping extent를 신뢰하려면 producer나 다른 holder가 뒤에서 `ftruncate()`로 크기를 바꾸지 못하게 하는 것이 유리하다. `F_SEAL_GROW`와 `F_SEAL_SHRINK`는 각각 file 확장/축소를 제한해 size generation을 고정하는 데 사용된다. 특히 shrink를 막으면 mapped reader의 SIGBUS risk를 줄이는 중요한 invariant가 된다. 하지만 size seal만으로 existing bytes write를 막지는 않는다. **Object shape immutability와 content immutability는 다른 seals**로 다뤄야 한다. Consumer는 필요한 seal set을 `F_GET_SEALS`로 확인하고 “memfd이므로 안전”이라고 추정하지 않아야 한다.

## CHAPTER 27 · F_SEAL_WRITE는 content mutation을 강하게 닫지만 existing writable shared mapping과 충돌한다

`F_SEAL_WRITE`는 write 계열 operations와 새로운 writable shared mappings를 제한해 content를 immutable하게 만들 수 있다. 그러나 이미 writable shared mapping이 존재하는 상태에서 이 seal을 추가하려 하면 EBUSY가 날 수 있다. 즉 producer가 writable mapping으로 data를 채운 뒤 immutable publish하려면 writable shared mapping을 먼저 제거하거나 권한/lifecycle을 적절히 정리해야 한다. Seal 추가 성공 여부를 무시하고 receiver에게 “immutable”이라고 광고하면 race가 그대로 남는다. **Seal publication은 initialization phase와 immutable phase 사이의 commit point**로 취급해야 한다. Success를 확인한 뒤에만 untrusted consumers에게 object를 전달하는 편이 안전하다.

## CHAPTER 28 · F_SEAL_FUTURE_WRITE는 기존 writable mappings는 유지하면서 future mutation channels를 닫는 다른 contract다

`F_SEAL_FUTURE_WRITE`는 이후 `write()`나 새로운 writable mapping을 막으면서 seal 설정 전에 존재한 writable shared mapping은 계속 수정 가능하게 둘 수 있다. Producer가 한 mapping으로 업데이트를 계속하면서 새 recipients에게는 writable mapping creation을 허용하지 않는 use case에 적합할 수 있다. 하지만 receiver가 “FUTURE_WRITE seal이 있으니 contents가 완전히 immutable하다”고 믿으면 틀린다. 기존 writer가 여전히 값을 바꿀 수 있기 때문이다. **Seal 이름이 아니라 기존 capabilities까지 포함한 mutation graph를 검사**해야 한다. Hard immutability가 필요하면 existing writers를 drain한 뒤 더 강한 seal state로 전환해야 한다.

## CHAPTER 29 · F_SEAL_EXEC는 executable memfd의 write/size mutation을 닫아 code object publication을 더 강하게 만든다

Modern Linux의 `F_SEAL_EXEC`는 execute mode 변화 제한과 함께 grow/shrink/write/future-write 관련 seals를 묶어 executable object를 더 불변에 가깝게 만들 수 있다. JIT 또는 dynamic code artifact를 여러 processes와 공유하는 설계에서 code bytes가 실행 승인 뒤 바뀌지 않아야 한다는 W^X/provenance invariant와 연결할 수 있다. 다만 kernel version/filesystem support를 feature-detect해야 하고 seal 적용 전에 필요한 preconditions를 만족해야 한다. **Executable permission과 code immutability를 함께 관리**해야 TOCTOU code replacement risk를 줄일 수 있다. Seal success를 build/content hash와 함께 audit metadata로 남기면 provenance가 더 명확하다.

## CHAPTER 30 · memfd를 SCM_RIGHTS로 넘기면 copy 대신 object capability가 전달된다

UNIX-domain socket의 SCM_RIGHTS를 통해 memfd descriptor를 다른 process에 보내면 payload bytes를 message로 복사하기보다 같은 kernel file object에 대한 새로운 fd reference를 전달할 수 있다. Receiver는 자신의 address space에 mmap하여 읽을 수 있다. 이때 transfer되는 것은 pathname이 아니라 capability이므로 sender는 전달 전 object size, seals, content generation을 확정해야 한다. Receiver도 sender가 주장한 properties를 그대로 믿지 말고 fd에서 직접 metadata/seals를 검증할 수 있다. **Shared-memory IPC의 trust boundary는 bytes parser뿐 아니라 전달된 object capability의 mutation 권한까지 포함**한다. Last-reference lifetime과 fd leak도 accounting해야 한다.

## CHAPTER 31 · fork는 mappings를 child address space에 복제하지만 shared/private semantics에 따라 이후 data lineage가 갈린다

`fork()` 후 child는 parent virtual layout을 기반으로 mappings를 물려받는다. Private writable mappings는 COW를 통해 이후 writes가 분리되고 shared mappings는 같은 backing object changes를 공유할 수 있다. `MADV_DONTFORK` 같은 advice로 특정 ranges를 inheritance에서 제외할 수도 있다. Multi-threaded parent에서 fork 후 child는 한 thread만 남으므로 mapping을 보호하던 userspace lock state와 JIT/allocator metadata가 일관된지 주의해야 한다. **Mapping inheritance는 pointer 값 복제뿐 아니라 backing-sharing policy와 runtime synchronization state를 함께 상속하는 사건**이다. Fork+exec helper라면 필요 없는 sensitive mappings를 최소화하는 정책도 중요하다.

## CHAPTER 32 · MADV_DONTFORK와 MADV_WIPEONFORK는 child에게 memory를 어떻게 넘길지 선택하는 security/lifetime 도구다

일부 mappings는 device DMA, thread stack helper, secret state처럼 fork child가 그대로 사용하면 위험할 수 있다. `MADV_DONTFORK`는 range를 child inheritance에서 제외하는 데 사용할 수 있고 `MADV_WIPEONFORK`는 child 쪽 내용을 zeroed state로 만들 수 있는 semantics를 제공한다. 이 flags는 application object model과 맞춰 사용해야 한다. Child에서 pointer는 남아 있는데 backing range가 없거나 contents가 zero가 된 사실을 모르면 crash/logic corruption이 생긴다. **Fork inheritance policy를 mapping metadata의 일부로 기록**하고 child initialization이 해당 state를 재구축하도록 해야 한다. Security hardening은 semantic compatibility와 같이 검증해야 한다.

## CHAPTER 33 · /proc/<pid>/maps와 smaps는 mapping topology와 residency를 다른 층으로 보여준다

`maps`는 virtual ranges, protection, offsets, device/inode, pathname-like labels을 통해 현재 address-space topology를 보여주고 `smaps`는 RSS/PSS, dirty, anonymous, huge-page 등 더 많은 accounting을 제공한다. Crash나 memory leak 조사에서 application allocator metric만 보는 것보다 VMA 수와 mapped file identities를 같이 보면 hidden mappings를 찾을 수 있다. 하지만 `/proc` snapshot을 읽는 동안 mappings가 변할 수 있으므로 여러 lines를 하나의 atomic transaction처럼 해석하면 안 된다. **Observability output도 시점 snapshot**이며 high-churn process에서는 tracing과 generation-aware application logs를 함께 써야 한다.

## CHAPTER 34 · Mapping leak은 byte capacity뿐 아니라 VMA count와 fd-independent backing references를 고갈시킬 수 있다

Application이 `munmap()`을 빼먹으면 virtual address space와 resident/cache references가 남을 수 있다. Mapping마다 VMA metadata가 필요하고 protection split이 많으면 작은 ranges라도 VMA count가 증가한다. 64-bit address space가 넓다고 metadata/resource limit이 무한한 것은 아니다. Memfd mapping은 original fd를 닫았어도 mapping reference 때문에 backing object가 남을 수 있어 fd leak detector만으로 찾지 못한다. **Memory leak detection은 heap bytes, RSS, virtual size, VMA count, mapped object generations를 함께 봐야 한다.** Long-running process에서 maps diff가 강력한 진단 도구가 된다.

## CHAPTER 35 · Mapping fault injection은 truncate, unmap, protect, move를 concurrent access와 실제로 충돌시켜야 한다

Happy-path mmap read/write만 확인하면 lifetime bugs를 찾을 수 없다. Reader가 tight loop로 pointer를 쓰는 동안 다른 thread가 `mprotect(PROT_NONE)`, partial `munmap`, `mremap(MAYMOVE)`, backing file truncate를 수행해 expected SIGSEGV/SIGBUS 또는 synchronization guard가 발생하는지 본다. MAP_FIXED collision test는 isolated child process에서 수행해 test runner 자체 mappings를 파괴하지 않도록 한다. Memfd는 existing writable mapping 상태에서 F_SEAL_WRITE가 실패하는 case와 FUTURE_WRITE 뒤 old writer가 여전히 수정 가능한 case를 검증한다. **각 mutation failure class를 별도 CLEAN으로 실행해야 mapping ownership protocol을 실제 증명**할 수 있다. 위험한 syscall test는 disposable process boundary가 필수다.

## CHAPTER 36 · Mapping correctness는 pointer validity가 아니라 address-range generation을 끝까지 추적하는 proof다

안전한 mapping subsystem은 다음을 답할 수 있어야 한다. Base와 length를 함께 추적하는가. File size와 VMA extent를 혼동하지 않는가. Truncate 동안 readers를 보호하는가. Partial munmap/mprotect가 registry에 반영되는가. Fixed mappings가 다른 owners를 clobber하지 않는가. mremap move 뒤 stale pointers가 남지 않는가. Shared visibility와 durability를 구분하는가. Fork inheritance policy가 명확한가. Memfd receiver가 seals를 직접 확인하는가. FUTURE_WRITE와 full immutability의 차이를 아는가. Fault injection 후 stale mappings와 unreleased references가 0으로 수렴하는가. **mmap의 최종 안전성은 pointer가 한 번 유효했다는 사실이 아니라, 그 pointer가 속한 mapping generation의 생성·변형·폐기가 모두 동기화됐다는 증거**다.
