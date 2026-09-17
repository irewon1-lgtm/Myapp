# PART 79 · Shared DMA Buffers and Fences — dma-buf, reservation objects, device ordering, CPU coherency

여러 device와 process가 같은 buffer를 공유하면 pointer lifetime만 맞추는 것으로 부족하다. GPU가 쓰는 동안 display engine이 읽거나, camera가 frame을 채우는 동안 encoder가 같은 storage를 참조하거나, CPU가 mmap한 buffer를 device가 동시에 수정하면 **storage lifetime·execution completion·cache visibility**가 서로 다른 시간축으로 움직인다. Linux dma-buf subsystem은 shared storage를 file-descriptor 기반 object로 전달하고, dma_fence/dma_resv로 asynchronous device ordering을 표현한다. Correctness는 `buffer가 존재한다`가 아니라 **각 participant가 언제 읽고 쓸 수 있으며, 그 시점의 data가 실제로 관측 가능한가**를 증명하는 문제다.

## CHAPTER 01 · dma-buf는 bytes를 복사하지 않고 shared storage의 ownership handle을 전달한다

Dma-buf file descriptor를 process나 subsystem 사이에 넘기는 것은 buffer contents를 복사하는 동작이 아니다. Exporter가 실제 backing storage의 allocation/migration/cache rules를 소유하고, fd와 kernel object reference가 그 storage lifetime을 연장한다. Importer는 storage implementation을 직접 가정하지 않고 attachment/API contract를 통해 접근한다. 이 separation이 깨져 importer가 exporter-private layout이나 physical contiguity를 가정하면 다른 allocator/device 조합에서 즉시 호환성이 무너진다.

## CHAPTER 02 · File descriptor 전달은 buffer authority와 lifetime reference를 동시에 이동시킨다

Userspace가 dma-buf fd를 SCM_RIGHTS나 API return으로 다른 process에 전달하면 recipient는 별도 virtual address가 없어도 같은 kernel buffer object에 reference를 가진다. Sender가 fd를 close해도 recipient reference가 남아 있으면 storage는 살아 있어야 한다. 반대로 fd table entry만 보고 `owner process가 종료됐으니 buffer가 free됐다`고 판단하면 cross-process use-after-free를 만든다. Shared buffer lifetime은 process lifetime이 아니라 **모든 fd/kernel attachment/reference가 사라지는 시점**까지 이어진다.

## CHAPTER 03 · Exporter는 backing storage의 mobility와 coherency policy를 소유한다

Exporter driver는 page/VRAM/system memory 등 실제 storage를 어떻게 배치하고, 언제 migration할 수 있으며, CPU/device cache sync를 어떻게 수행할지 알고 있다. Importer가 필요할 때 exporter callback을 호출하는 이유는 이 policy를 storage owner에게 남기기 위해서다. Exporter가 move 가능한 buffer를 제공한다면 importer는 attachment/fence contract 없이 physical address를 장기 보관해서는 안 된다. `dma-buf fd가 같다`는 사실은 physical location이 영원히 같다는 뜻이 아니다.

## CHAPTER 04 · Attachment는 특정 device가 shared buffer에 참여한다는 explicit relationship이다

`dma_buf_attach()` 계열은 importer device와 dma-buf 사이 relation을 만들고 exporter가 해당 device의 DMA constraints를 고려할 기회를 준다. 같은 buffer라도 device마다 DMA address width, segment limit, IOMMU domain, placement requirement가 다를 수 있다. 따라서 한 device에서 얻은 scatter-gather mapping을 다른 device에 재사용하면 안 된다. Attachment object는 **device-specific view와 lifetime**을 표현하며 detach 전에 해당 device access가 완전히 끝나야 한다.

## CHAPTER 05 · Dynamic importer는 backing이 움직일 수 있다는 사실을 synchronization protocol로 받아들인다

Dynamic buffer management에서는 exporter가 memory pressure나 placement optimization 때문에 backing을 이동할 수 있다. Importer는 `한 번 map하면 영구 address`를 가정하지 않고 fence/reservation lock과 pin/map lifetime을 통해 movement와 충돌하지 않게 해야 한다. Dynamic importer가 buffer storage를 실제로 접근하기 전에는 관련 write dependency를 기다리고, 필요하면 pin으로 movement를 제한한다. Mobility를 허용하는 대신 **mapping stability를 짧은 critical lifetime으로 축소**하는 구조다.

## CHAPTER 06 · Scatter-gather table은 logical buffer와 device-visible DMA segmentation을 연결한다

Logical buffer가 하나여도 physical backing은 여러 page/segment로 흩어질 수 있다. Importer가 map을 요청하면 exporter/IOMMU/DMA layer가 device가 사용할 scatter-gather table을 제공한다. Segment merging과 DMA address translation 때문에 CPU physical layout과 device-visible list가 동일하지 않을 수 있다. Driver는 sg table을 buffer contents가 아니라 **현재 device mapping의 transient translation result**로 취급하고 unmap 이후에는 참조하지 않아야 한다.

## CHAPTER 07 · map_dma_buf와 unmap_dma_buf는 device access lifetime을 bracket한다

DMA mapping을 만들었다는 것은 device가 지금부터 해당 storage에 접근 가능한 address translation을 가질 수 있다는 뜻이다. Unmap은 address를 잊는 함수가 아니라 **device access가 끝났고 mapping resource를 회수해도 된다는 declaration**이다. In-flight command가 남아 있는데 먼저 unmap하면 stale DMA address를 사용하고, completion 이후 unmap을 잊으면 IOMMU/sg resource가 누적된다. Mapping lifetime owner는 command/fence lifetime과 명시적으로 연결해야 한다.

## CHAPTER 08 · dma_resv는 shared buffer에 걸린 asynchronous dependency set을 관리한다

하나의 buffer에는 동시에 여러 read operation과 write operation이 outstanding일 수 있다. `dma_resv`는 이 access history를 dma_fence 집합으로 관리해 다음 submission이 무엇을 기다려야 하는지 계산할 수 있게 한다. Reservation object는 buffer lock 이상의 의미를 가진다. **과거 asynchronous work와 미래 work 사이 ordering graph**를 저장하는 metadata다. Fence를 등록하지 않은 device access는 다른 driver에게 존재하지 않는 작업처럼 보이므로 cross-driver race가 발생한다.

## CHAPTER 09 · READ/WRITE usage는 dependency 방향을 결정한다

새로운 read는 이전 write completion을 기다려야 하지만 서로 독립적인 read들은 동시에 진행할 수 있다. 새로운 write는 이전 read와 write가 모두 끝나야 old readers가 중간 상태를 보지 않는다. `DMA_RESV_USAGE_READ/WRITE`는 이 dependency 관계를 표현한다. 모든 작업을 write로 등록하면 correctness는 보수적으로 유지될 수 있지만 concurrency를 잃고, write를 read로 잘못 등록하면 stale/partial data race가 생긴다. Access mode는 성능 hint가 아니라 ordering semantics다.

## CHAPTER 10 · Implicit synchronization은 buffer handle 안에 dependency 전달을 숨긴다

Implicit sync model에서는 userspace가 fence fd를 명시적으로 전달하지 않아도 kernel driver들이 shared dma_resv의 fences를 읽고 기다린다. API는 단순하지만 hidden dependency 때문에 submission latency 원인을 userspace에서 보기 어렵고 unrelated pipeline이 같은 buffer reservation에 직렬화될 수 있다. Driver가 implicit sync를 지원한다고 선언했으면 모든 relevant access를 reservation object에 정확히 등록해야 한다. 일부 path만 빠지면 API가 평소에는 작동하다 race window에서만 corruption을 만든다.

## CHAPTER 11 · Explicit synchronization은 dependency ownership을 userspace/API boundary로 올린다

Explicit sync에서는 producer가 completion fence를 내보내고 consumer가 그 fence를 input dependency로 전달한다. Dependency graph가 명시적이라 compositor/media pipeline처럼 복잡한 graph를 더 정확히 구성할 수 있지만, userspace가 fence 전달을 누락하면 kernel이 자동으로 복구해주지 않을 수 있다. Explicit model은 **buffer handle과 synchronization handle을 독립 resource로 관리**해야 한다. Buffer lifetime과 fence lifetime을 같은 fd 하나로 착각하면 early reuse 또는 fd leak가 발생한다.

## CHAPTER 12 · sync_file은 fence를 userspace file descriptor로 전달하는 transport다

Kernel dma_fence를 process 경계 밖으로 전달하려면 sync_file 같은 fd wrapper를 사용할 수 있다. 이 fd는 buffer storage가 아니라 특정 asynchronous completion point를 나타낸다. Dup/pass/close semantics는 ordinary fd처럼 reference-counted이지만, underlying fence가 signal되는 시점과 fd close 시점은 독립적이다. `fence fd를 닫았다 = hardware work를 cancel했다`가 아니며 cancellation은 별도 device/job protocol이 필요하다.

## CHAPTER 13 · Fence context와 seqno는 timeline 안의 ordering을 표현한다

Dma_fence는 context와 sequence number를 통해 같은 execution timeline의 work 순서를 표현할 수 있다. 같은 context의 fence끼리는 어느 작업이 먼저 완료되어야 하는지 비교 가능하지만, 서로 다른 engine/context의 seqno 숫자를 직접 비교하면 의미가 없다. Timeline identity를 무시한 global integer comparison은 cross-engine ordering bug를 만든다. Dependency graph는 **fence identity + context + explicit edges**로 구성해야 한다.

## CHAPTER 14 · Fence signal은 hardware access completion이지 CPU coherency 완료와 동일하지 않다

Fence가 signal됐다는 것은 해당 asynchronous DMA operation이 완료됐음을 뜻하지만 CPU가 바로 최신 bytes를 볼 수 있다는 의미까지 자동으로 포함하지 않는다. Non-coherent architecture나 exporter-specific cache policy에서는 CPU access 전에 cache maintenance가 필요하다. 따라서 `wait fence → CPU dereference` 사이에 begin_cpu_access 같은 coherency transition이 필요할 수 있다. **Ordering과 visibility는 서로 다른 proof**다.

## CHAPTER 15 · begin_cpu_access/end_cpu_access는 CPU ownership window를 명시한다

CPU가 shared buffer를 읽거나 쓰기 전에 `dma_buf_begin_cpu_access()` 계열을 호출하면 exporter가 pending implicit DMA를 기다리고 필요한 cache maintenance를 수행할 수 있다. CPU access가 끝난 뒤 end call이 device가 다음 access에서 CPU writes를 볼 수 있게 준비한다. 이 pair를 생략하면 같은 bytes를 CPU와 device가 다른 cache state로 볼 수 있다. CPU mapping이 존재한다는 사실만으로 coherent ownership이 자동 획득되는 것은 아니다.

## CHAPTER 16 · Cache maintenance direction은 read/write intent와 연결된다

CPU가 buffer를 읽기만 하는지, 수정하는지에 따라 invalidate/clean 동작과 비용이 달라질 수 있다. DMA direction이나 CPU access direction을 항상 bidirectional로 처리하면 correctness는 유지될 수 있어도 cache traffic가 커지고 latency가 늘어난다. 반대로 실제 write를 read-only로 선언하면 device가 stale cache line을 볼 수 있다. Access intent metadata는 optimizer hint가 아니라 **visibility protocol의 입력값**이다.

## CHAPTER 17 · vmap lifetime은 kernel virtual mapping reference와 buffer mobility를 묶는다

Kernel이 dma-buf를 contiguous virtual address로 vmap하면 편리하지만 mapping이 살아 있는 동안 backing 이동이나 remap policy에 제약이 생길 수 있다. Dma-buf core가 vmap reference count를 관리하는 이유는 여러 caller의 mapping lifetime을 합쳐 exporter가 언제 mapping을 철회할 수 있는지 알기 위해서다. vmap pointer를 cache해 reference를 놓친 뒤 계속 사용하면 stale mapping이 된다. Pointer 저장보다 lifetime reference가 먼저다.

## CHAPTER 18 · Cross-device sharing은 topology와 DMA constraints가 다르다는 사실을 숨기지 못한다

GPU에서 빠른 local memory가 display/video device에는 직접 접근 불가능할 수 있고, P2P path가 IOMMU/PCIe topology 때문에 제한될 수 있다. Shared buffer API는 logical interoperability를 제공하지만 physical placement cost를 없애지 않는다. Exporter는 importer set에 따라 system memory로 migrate하거나 bounce/copy를 선택할 수 있다. Zero-copy라는 이름만 보고 topology cost가 0이라고 가정하면 bandwidth와 latency가 예상과 달라진다.

## CHAPTER 19 · Reservation lock은 여러 shared object를 동시에 갱신할 때 deadlock-safe ordering을 요구한다

Command submission이 여러 buffers의 dma_resv를 동시에 lock해야 하면 arbitrary lock order가 ABBA deadlock을 만들 수 있다. Reservation infrastructure는 wound/wait 계열 locking 등 multiple-object acquisition protocol을 사용한다. Driver가 임의 mutex와 dma_resv를 섞어 새로운 cycle을 만들면 cross-driver workload에서만 deadlock이 드러난다. Lockdep annotation과 canonical acquisition protocol을 지켜야 shared-buffer graph가 composable하다.

## CHAPTER 20 · Fence는 반드시 reasonable time 안에 completion 또는 error로 종결되어야 한다

Cross-driver dependency로 사용되는 fence가 영원히 signal되지 않으면 다른 device pipeline, memory reclaim, display commit까지 연쇄 정지할 수 있다. Hardware hang이나 malicious long-running job에 대비해 timeout/reset/hang recovery가 fence를 error completion으로라도 끝내야 한다. `hardware가 언젠가 끝날 것`이라는 가정은 shared kernel resource contract로는 부족하다. Fence producer는 **forward-progress responsibility**를 가진다.

## CHAPTER 21 · Reset path는 성공 fence와 실패 fence를 모두 종결해야 한다

GPU/video engine reset으로 outstanding job이 사라지면 normal interrupt completion이 오지 않을 수 있다. Driver는 request table을 walk해 affected fences에 error를 기록하고 signal해 waiter가 영원히 block되지 않게 해야 한다. Buffer는 failed job이 더 이상 access하지 않는다는 hardware quiescence가 확인된 뒤에만 reuse할 수 있다. Error signal은 `작업 성공`이 아니라 **이 dependency가 더 이상 진행 중이 아님**을 전달한다.

## CHAPTER 22 · Fence completion path가 기다리는 lock을 waiter가 들고 있으면 cross-driver deadlock이 된다

`dma_fence_wait()`가 특정 lock을 잡은 상태에서 허용된다면 fence를 signal하기 위해 필요한 모든 path는 그 lock을 다시 필요로 해서는 안 된다. Shrinker, mmu_notifier, reservation lock 같은 context가 결합되면 평범한 lock order review만으로 cycle을 찾기 어렵다. Signalling critical section annotation과 lockdep model이 필요한 이유다. Fence는 asynchronous primitive지만 completion code도 결국 synchronous locks 위에서 실행된다.

## CHAPTER 23 · Signal 이후 driver-private fence data lifetime에는 RCU/refcount 규칙이 필요하다

Fence가 signaled된 순간 waiter는 깨울 수 있지만 다른 CPU가 아직 fence metadata를 조회 중일 수 있다. Driver가 signal 직후 private lock/name/context storage를 free하면 RCU reader가 freed memory를 dereference할 수 있다. 따라서 fence object와 driver-provided metadata의 lifetime을 구분하고 필요한 grace period/reference를 유지해야 한다. Completion과 destruction은 동일 event가 아니다.

## CHAPTER 24 · Dynamic buffer migration은 old fence와 new placement를 하나의 transaction으로 묶어야 한다

Buffer를 VRAM에서 system memory로 옮기거나 다른 node로 migration할 때 기존 device access가 끝나기 전에 backing을 바꾸면 stale mapping DMA가 발생한다. Migration copy 자체도 새로운 asynchronous work라 fence로 표현될 수 있다. Old users의 completion을 기다리고, migration fence를 reservation에 publish하고, new mapping이 준비된 뒤 importer가 접근하게 해야 한다. Placement update는 **storage state와 synchronization graph를 함께 commit**해야 한다.

## CHAPTER 25 · Userspace mmap은 CPU virtual lifetime을 하나 더 추가한다

Dma-buf를 userspace에 mmap하면 fd reference, VMA lifetime, device attachments, CPU coherency window가 동시에 존재한다. Process가 fd를 close해도 VMA가 mapping reference를 유지할 수 있고, VMA close 뒤 device job이 남아 있으면 storage는 여전히 살아야 한다. Mapping fault가 exporter callback을 호출할 수 있으므로 teardown과 migration 중에는 fault path도 고려해야 한다. Buffer object lifetime을 한 종류 reference count로 단순화하면 숨은 participant를 놓친다.

## CHAPTER 26 · poll의 readable/writable 의미는 data bytes가 아니라 fence state일 수 있다

Dma-buf implicit fence poll에서 EPOLLIN은 일반 file의 `읽을 data가 있다`와 같은 뜻이 아니다. Pending write fence가 끝나 read access가 가능해졌는지, 또는 모든 relevant fences가 끝나 write access가 가능한지를 나타낼 수 있다. Generic event loop에서 fd 종류별 readiness semantics를 구분하지 않으면 wrong assumption으로 busy-loop하거나 premature access한다. Readiness bit는 항상 object-specific state machine으로 해석해야 한다.

## CHAPTER 27 · Cross-process buffer ownership은 producer/consumer lifecycle을 명시해야 한다

Camera→codec→GPU→display pipeline에서 여러 process가 같은 dma-buf fd를 이어받으면 누가 최종 close owner인지 불명확해지기 쉽다. 각 stage는 input reference를 보유하는 기간과 output fence를 넘기는 시점을 명시하고, process crash에서도 orphaned references가 정리되어야 한다. Buffer pool은 모든 consumers가 끝났다는 fence/reference 증거 없이 slot을 재사용해서는 안 된다. Shared memory 최적화는 ownership protocol 없이는 corruption 최적화가 된다.

## CHAPTER 28 · Teardown은 new submissions 차단→fence drain/error→detach→unmap→free 순서를 요구한다

Device removal/process death/module unload에서 shared buffer를 없앨 때 새 job이 계속 등록되면 drain이 끝나지 않는다. 먼저 submission entry를 닫고, outstanding work를 completion 또는 error로 종결하고, reservation dependencies가 더 이상 추가되지 않는 상태를 만든다. 이후 device attachment/DMA mapping을 해제하고 마지막 reference에서 storage를 free한다. Teardown 순서가 뒤집히면 fence callback과 device completion이 해제된 exporter state를 건드린다.

## CHAPTER 29 · Shared-buffer observability는 bytes보다 dependency graph와 lifetime을 보여줘야 한다

Memory usage만 보면 buffer가 왜 재사용되지 않는지 알기 어렵다. Exporter/importer 이름, attachment 수, reservation fence contexts/seqnos, fence age, signal/error state, vmap/mmap reference, CPU-access window를 함께 보면 어느 participant가 ownership을 놓지 않았는지 찾을 수 있다. 특히 오래된 unsignaled fence는 buffer leak처럼 보이는 현상의 root cause일 수 있다. Debug UI는 object 크기보다 **누가 무엇을 기다리는지**를 우선 보여줘야 한다.

## CHAPTER 30 · Shared-buffer correctness는 lifetime·ordering·visibility 세 증명을 동시에 요구한다

하나의 buffer에 대해 `storage는 언제까지 살아 있는가`, `각 reader/writer 사이 ordering edge는 무엇인가`, `completion 후 CPU/device가 최신 data를 어떻게 관측하는가`를 각각 답해야 한다. Lifetime만 맞고 fence가 빠지면 concurrent corruption, fence만 맞고 cache sync가 빠지면 stale data, coherency만 맞고 reference가 빠지면 use-after-free가 된다. **dma-buf 시스템의 본질은 zero-copy가 아니라 서로 다른 execution domain 사이 ownership을 보존하는 protocol**이다.
