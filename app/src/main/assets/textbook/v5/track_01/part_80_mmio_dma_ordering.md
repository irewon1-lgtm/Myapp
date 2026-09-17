# PART 80 · MMIO and DMA Ordering — device registers, posted writes, coherent memory, visibility barriers

Device driver는 CPU memory model 하나만 이해해서는 안전하지 않다. Compiler가 load/store를 재배열하는 규칙, CPU가 normal memory를 관측하는 순서, MMIO accessor가 제공하는 ordering, PCIe 같은 bus가 write를 posted transaction으로 처리하는 방식, device가 DMA descriptor를 fetch하는 시점이 서로 다르다. 특히 `coherent DMA`는 CPU와 device cache visibility 문제를 줄이지만 **descriptor publication 순서, MMIO doorbell ordering, posted-write completion**까지 자동 해결하지 않는다. Correctness는 `코드에 writel이 있다`가 아니라 **device가 어떤 state를 어떤 순서로 관측해야 하는가**를 증명하는 문제다.

## CHAPTER 01 · Device ordering에는 최소 네 개의 reorder domain이 있다

Compiler는 language semantics를 보존하는 범위에서 instruction 순서를 바꿀 수 있고, CPU는 memory model에 따라 normal-memory operation을 reorder할 수 있다. MMIO accessor는 architecture별 device access ordering을 추가로 제공하며, bus fabric은 posted write처럼 CPU retirement와 실제 device arrival을 분리할 수 있다. 따라서 `소스코드에서 A 다음 B`만으로 device가 A→B 순서로 관측했다고 결론 내릴 수 없다. Driver proof는 **compiler→CPU→bus→device** 네 단계의 ordering guarantee를 각각 확인해야 한다.

## CHAPTER 02 · `__iomem`은 ordinary pointer가 아니라 device-access capability를 표시한다

MMIO mapping은 많은 architecture에서 virtual address처럼 보이지만 portable kernel code는 ordinary C dereference를 사용하지 않고 `readl/writel` 계열 accessor를 통해 접근해야 한다. `__iomem` annotation은 sparse 같은 checker가 ordinary memory와 device memory를 혼동하는 코드를 잡을 수 있게 한다. Device register를 `volatile *`로 cast해 직접 읽고 쓰면 endian·alignment·ordering·special instruction requirement를 우회한다. Type annotation은 장식이 아니라 **address-space semantics를 코드에 남기는 contract**다.

## CHAPTER 03 · ioremap은 device register에 맞는 memory attributes를 만든다

Typical `ioremap()` mapping은 CPU cache를 우회하고 speculative access·write combining 같은 normal-memory optimization을 제한하도록 architecture-specific page attributes를 설정한다. 목적은 register read/write가 program이 의도하지 않은 시점이나 횟수로 변형되지 않게 하는 것이다. 그러나 이 mapping이 bus-level posted write를 없애는 것은 아니다. CPU-side memory type과 interconnect completion semantics를 하나로 합쳐 생각하면 `write 함수가 return했으니 device가 받았다`는 잘못된 가정이 생긴다.

## CHAPTER 04 · write-combining mapping은 throughput을 위해 register-like ordering을 의도적으로 약화한다

`ioremap_wc()` 같은 mode는 framebuffer나 device memory처럼 여러 write를 합쳐도 되는 영역에서 성능을 높일 수 있다. CPU가 write를 combine/reorder할 수 있으므로 control register처럼 각 store의 순서와 횟수가 의미 있는 영역에는 부적합하다. Device BAR 안에서도 control register와 bulk memory window는 요구 semantics가 다를 수 있다. Mapping type은 `빠른 버전/느린 버전`이 아니라 **device region의 side-effect model과 일치해야 하는 memory type**이다.

## CHAPTER 05 · readl/writel은 portable MMIO ordering의 기본 단위다

`readl/writel` 계열은 device register access에 필요한 endian 변환과 architecture-specific ordering을 제공하도록 설계됐다. Normal memory load/store와 동일한 machine instruction으로 구현되는 architecture도 있지만 portable driver는 그 사실에 의존해서는 안 된다. Ordered accessor가 어떤 prior DMA/normal-memory operation과 관계를 보장하는지도 API contract로 판단해야 한다. `x86에서 됐다`는 evidence는 weak-order architecture에서 correctness proof가 아니다.

## CHAPTER 06 · relaxed accessor는 barrier 비용을 줄이는 대신 caller가 dependency를 증명해야 한다

`readl_relaxed/writel_relaxed`는 일부 architecture에서 DMA/normal-memory와의 ordering barrier를 줄여 fast path 비용을 낮춘다. 서로 독립적인 status register polling처럼 additional order가 필요 없는 경우에는 유효하지만, descriptor publication 뒤 doorbell이나 DMA completion 확인과 연결된 read에 쓰면 stale/reordered state를 관측할 수 있다. Relaxed accessor를 사용한 line에는 **어떤 prior/future memory operation과 independent한지**가 설명되어야 한다. 성능 때문에 `_relaxed`를 일괄 치환하는 최적화는 위험하다.

## CHAPTER 07 · PCIe posted write는 CPU가 write completion을 기다리지 않고 진행하게 한다

PCI memory write는 bus에 queue된 뒤 CPU side accessor가 return할 수 있다. CPU가 이어서 device reset, interrupt mask 변경, resource free를 진행하면 이전 write가 실제 device에 도착하기 전에 다음 software state가 바뀔 수 있다. Posted write는 `순서가 없다`기보다 **issuer completion과 target acceptance가 분리된 transaction**이다. 특정 단계에서 device acceptance를 확실히 해야 하면 explicit flush mechanism이 필요하다.

## CHAPTER 08 · 같은 device에 대한 read는 posted write flush로 사용할 수 있다

많은 PCIe device에서 previous MMIO write가 target에 도달했음을 보장하려면 같은 device의 안전한 register를 read해 read response를 기다리는 패턴을 사용한다. Read response가 돌아오려면 bus ordering상 선행 posted write가 처리되어야 하기 때문이다. Device reset 중 register read가 실패할 수 있다면 config-space read처럼 soft-fail 가능한 path를 선택해야 한다. Dummy read는 쓸모없는 I/O가 아니라 **write completion barrier**다.

## CHAPTER 09 · spinlock unlock은 posted MMIO write가 device에 도착했음을 자동 보장하지 않는다

Driver가 lock 안에서 register write를 한 뒤 lock을 풀면 다른 CPU와 software critical section ordering은 생기지만 bus transaction이 target device에 도달했는지까지 보장되지는 않을 수 있다. 다음 CPU가 lock을 얻고 hardware state가 이미 바뀌었다고 가정하면 software lock order와 device state order가 어긋난다. 필요한 경우 lock release 전에 MMIO flush/read 또는 documented ordering primitive를 사용해야 한다. **CPU mutual exclusion과 device transaction completion은 별도 layer**다.

## CHAPTER 10 · Descriptor memory와 doorbell은 producer publication protocol을 이룬다

Queue-based device에서는 CPU가 normal/coherent memory에 descriptor fields를 채우고 마지막에 MMIO doorbell을 써서 `새 work가 있다`고 알린다. Device가 doorbell을 먼저 보고 descriptor의 일부 old value를 읽으면 malformed command가 된다. 따라서 descriptor stores가 device-visible해진 뒤 doorbell write가 관측되도록 DMA/device barrier가 필요하다. 이 패턴은 network TX ring, NVMe SQ, GPU command queue 등에서 반복되는 **publish-data-then-notify** protocol이다.

## CHAPTER 11 · DMA coherent memory도 store ordering을 자동 직렬화하지 않는다

Coherent allocation은 CPU write가 device에 eventually visible하고 device write도 CPU에 cache maintenance 없이 보이도록 hardware/platform이 coherency를 제공한다. 하지만 CPU가 `descriptor->addr`, `descriptor->len`, `descriptor->valid=1`을 쓴 순서를 device가 그대로 보도록 하는 것은 별도 memory-order problem이다. CPU write buffer와 weak ordering 때문에 valid bit가 먼저 보일 수 있다. Coherency는 **같은 bytes의 일관성**, barrier는 **여러 bytes/update 사이의 순서**를 다룬다.

## CHAPTER 12 · Streaming DMA mapping은 CPU와 device ownership phase를 명시한다

Streaming DMA buffer는 CPU와 device가 동시에 자유롭게 접근하는 shared-memory abstraction이 아니다. `dma_map_*`/sync API는 특정 direction과 phase에서 누가 buffer를 소유하는지 표현한다. Device ownership 중 CPU가 buffer를 수정하거나, CPU sync 없이 device-written buffer를 읽으면 non-coherent platform에서 stale/corrupt data가 발생한다. Ownership handoff를 명시하면 cache maintenance와 ordering을 architecture-specific DMA layer가 수행할 수 있다.

## CHAPTER 13 · sync_for_device와 sync_for_cpu는 cache visibility transition이다

Persistent streaming mapping을 재사용할 때 매번 unmap/remap하지 않고 `dma_sync_*_for_device/cpu`로 ownership을 전환할 수 있다. CPU가 마지막으로 쓴 뒤 device에 넘길 때는 dirty cache line을 device-visible state로 만들고, device가 쓴 뒤 CPU가 읽기 전에는 stale CPU line을 invalidate해야 할 수 있다. 호출 위치가 너무 빠르면 상대가 아직 접근 중이고, 너무 늦으면 stale data를 사용한다. Sync API는 **lifetime phase boundary**에 있어야 한다.

## CHAPTER 14 · DMA direction은 cache maintenance와 permission의 semantic input이다

`DMA_TO_DEVICE`, `DMA_FROM_DEVICE`, `DMA_BIDIRECTIONAL`은 이름뿐인 optimization hint가 아니다. Direction에 따라 어느 side의 writes를 보존하고 어떤 cache operation이 필요한지 달라진다. 실제 device가 write하는데 TO_DEVICE로 map하면 CPU가 device update를 놓칠 수 있고, 모든 것을 BIDIRECTIONAL로 쓰면 불필요한 maintenance와 IOMMU permission widening이 생긴다. Dataflow analysis와 DMA direction이 일치해야 한다.

## CHAPTER 15 · Cache-line granularity는 partial-buffer sharing을 위험하게 만든다

Cache maintenance는 byte가 아니라 cache line 단위로 일어날 수 있다. 서로 다른 ownership의 두 object가 같은 cache line을 공유하면 한 object의 invalidate/clean operation이 다른 object의 dirty CPU data를 날리거나 device data와 충돌시킬 수 있다. DMA-mapped range를 cache-line boundary에 맞추라는 권고는 단순 alignment performance가 아니라 **ownership domain이 같은 cache line을 공유하지 않게 하기 위한 correctness requirement**다.

## CHAPTER 16 · Descriptor publication은 data fields→barrier→ownership flag 순서를 가져야 한다

Device ring descriptor에서 마지막 `OWN/VALID` bit는 device에게 structure 전체가 준비됐다는 commit record와 같다. CPU는 address/length/flags를 먼저 기록하고 appropriate write barrier 뒤 ownership bit를 publish해야 한다. Device completion에서도 device가 result/status data를 쓴 뒤 done bit를 갱신한다는 contract가 필요하다. Commit bit를 ordinary field처럼 다루면 producer/consumer가 partially initialized descriptor를 읽을 수 있다.

## CHAPTER 17 · Completion flag를 읽은 뒤 result data를 읽는 순서에도 acquire-like guarantee가 필요하다

Device가 DMA로 payload/status를 쓰고 completion bit 또는 queue entry를 갱신했다면 CPU는 completion을 확인한 뒤 선행 device writes가 관측 가능하다는 ordering을 확보해야 한다. 단순 polling load만으로 모든 architecture에서 이 관계가 성립하지 않을 수 있다. DMA read barrier나 accessor semantics를 통해 **done observation이 result visibility보다 앞서지 않도록** 해야 한다. Producer release와 consumer acquire가 device boundary에도 필요하다.

## CHAPTER 18 · PIO read completion과 DMA write completion 사이 bus ordering도 protocol 일부다

일부 bus ordering 규칙에서는 device register read response가 돌아왔다는 사실이 같은 device의 선행 DMA writes가 host memory에 도달했음을 의미할 수 있다. Driver는 이 관계를 이용해 status read 뒤 buffer를 안전하게 읽을 수 있다. 반대로 relaxed accessor로 바꾸면 해당 ordering guarantee가 약해질 수 있다. Register read가 단순 status value 조회인지 **DMA completion fence 역할까지 하는지**를 구분해야 한다.

## CHAPTER 19 · Non-coherent platform은 x86에서 숨겨진 bug를 드러낸다

Cache-coherent x86 시스템에서 `memcpy→doorbell→read buffer` 코드가 우연히 동작해도, CPU/device cache가 자동 snoop하지 않는 architecture에서는 sync/barrier 누락이 즉시 corruption으로 나타날 수 있다. Portable driver test는 weak-order/non-coherent configuration을 포함하거나 DMA API debug와 explicit ownership model로 architecture assumption을 제거해야 한다. Fast platform에서 문제없다는 결과는 portable DMA correctness의 증거가 아니다.

## CHAPTER 20 · smp_mb와 mb는 목적 domain이 다를 수 있다

SMP memory barrier는 CPU 간 shared normal-memory ordering을 위해 최적화되며 UP build에서는 code generation이 약해지거나 사라질 수 있다. Physical device와의 ordering은 CPU 개수와 무관하므로 driver는 device-visible order가 필요할 때 appropriate generic/device barrier를 사용해야 한다. `smp_*` primitive를 device register sequencing에 쓰면 UP/architecture variation에서 bug가 생길 수 있다. Barrier 이름보다 **어떤 observer의 순서를 제어하는가**를 봐야 한다.

## CHAPTER 21 · dma_wmb/dma_rmb는 coherent DMA memory producer-consumer에 맞춘 barrier다

CPU가 coherent DMA descriptor를 publish하거나 device-written coherent memory를 consume할 때 DMA-specific barriers는 device observer와의 order를 표현한다. Normal CPU-to-CPU lock ordering과 구별해 사용하면 code review에서 intent가 명확해진다. Descriptor body writes 뒤 dma_wmb, completion/ownership flag 확인 뒤 dma_rmb 같은 pattern은 **data-before-flag / flag-before-data-consume** invariant를 직접 나타낸다. Primitive 선택이 protocol documentation 역할을 한다.

## CHAPTER 22 · Compiler barrier는 hardware order를 만들지 않는다

`barrier()` 같은 compiler barrier는 compiler가 memory operation을 넘겨 재배열하지 못하게 하지만 CPU store buffer나 bus transaction에는 명령을 내리지 않는다. 반대로 hardware barrier가 compiler reordering까지 포함하는지는 primitive contract를 확인해야 한다. Device bug를 고치며 compiler barrier 하나를 넣고 `memory barrier를 추가했다`고 표현하면 실제 hardware order가 그대로일 수 있다. **Compiler order와 execution order를 항상 분리해서 이름 붙여야 한다.**

## CHAPTER 23 · Endianness는 ordering과 별개지만 accessor를 우회할 때 함께 깨진다

PCI register가 little-endian이고 CPU가 big-endian이라면 readl/writel accessor가 byte order contract를 처리할 수 있다. Raw pointer dereference나 memcpy로 register block을 다루면 ordering뿐 아니라 endian semantics도 동시에 잃는다. Device memory bulk copy와 side-effect register access를 분리하고, register spec이 정의한 field width/access size를 사용해야 한다. Wrong endian value가 timing bug처럼 보이는 경우도 있어 register trace는 raw bus value와 decoded field를 함께 남기는 편이 좋다.

## CHAPTER 24 · 64-bit register는 architecture/device에 따라 atomic single access가 아닐 수 있다

32-bit CPU나 bus에서 64-bit device register를 두 번의 32-bit access로 읽거나 써야 할 수 있고, high/low order가 device spec에 따라 다르다. Counter가 중간에 증가하면 torn read가 생길 수 있어 high-low-high retry 같은 protocol이 필요할 수 있다. Generic `readq` 지원 여부와 split helper semantics를 확인해야 한다. Register width는 C type 선택이 아니라 **device transaction atomicity contract**다.

## CHAPTER 25 · memcpy/memset을 MMIO register에 쓰면 access 횟수와 순서를 통제할 수 없다

Compiler/library implementation은 ordinary memory copy를 word/vector 단위로 합치거나 접근 크기를 바꿀 수 있다. Side-effect register에서는 한 번의 32-bit write와 네 번의 byte write가 전혀 다른 동작일 수 있다. Device I/O 영역에는 `memcpy_toio/fromio`처럼 I/O semantics를 보존하는 helper를 사용해야 한다. Bulk register initialization도 device가 허용한 access pattern인지 spec으로 확인해야 한다.

## CHAPTER 26 · Speculative access가 금지된 register와 prefetch 가능한 device memory를 구분해야 한다

Status clear-on-read, FIFO pop, interrupt acknowledge 같은 register는 프로그램이 실제 해당 operation을 수행할 때만 정확히 한 번 읽혀야 한다. Normal cached mapping처럼 CPU가 미리 읽거나 반복 접근하면 device state가 변한다. 반면 framebuffer memory는 write combining/prefetch가 성능에 유리할 수 있다. 같은 BAR라는 이유로 하나의 mapping attribute를 전체 region에 적용하기보다 **side-effect model이 다른 subregion을 분리**해야 한다.

## CHAPTER 27 · Device reset/hot-unplug에서는 MMIO pointer lifetime 자체가 끝날 수 있다

PCI hot-remove나 fatal reset 뒤 기존 `__iomem` mapping을 다른 worker/IRQ가 계속 접근하면 completion timeout이 아니라 machine check/unsupported bus access로 이어질 수 있다. Teardown은 먼저 new work와 interrupt를 차단하고 in-flight handler를 synchronize한 뒤 MMIO mapping을 해제해야 한다. Posted write가 남아 있다면 reset 전 flush/ordering도 고려해야 한다. Pointer가 C scope에 남아 있다는 사실은 hardware resource가 살아 있다는 뜻이 아니다.

## CHAPTER 28 · MMIO ordering bug는 register trace와 descriptor snapshot을 같은 timeline으로 봐야 한다

Device가 malformed descriptor를 보고 멈췄을 때 CPU register write log만 보거나 memory dump만 보면 publication race를 놓친다. Doorbell timestamp, barrier 전후 descriptor contents, DMA map generation, completion IRQ, reset event를 같은 trace timeline에 묶어야 한다. Weak-order bug는 log를 추가하면 timing이 바뀌어 사라질 수 있으므로 low-overhead tracepoint/PMU와 deterministic fault injection을 함께 사용한다. Evidence도 ordering domain을 보존해야 한다.

## CHAPTER 29 · Portable driver는 strong-order architecture의 우연을 specification으로 착각하지 않는다

x86 TSO, coherent PCIe system, single-socket test machine에서는 많은 missing barrier가 우연히 드러나지 않는다. ARM/RISC-V, non-coherent SoC, IOMMU, multiple root complex에서 같은 source가 다른 observable order를 만들 수 있다. Portable code는 architecture folklore 대신 Linux accessor/DMA API가 약속한 semantics에만 의존해야 한다. Optimization은 contract보다 강한 platform property를 명시적으로 제한할 때만 사용한다.

## CHAPTER 30 · Device-order correctness는 observer별 happens-before graph로 증명해야 한다

Descriptor publication을 예로 들면 `CPU data stores → DMA write barrier → MMIO doorbell → device fetch`, completion은 `device result DMA → device completion publish → CPU completion observation → DMA read barrier → CPU result read`의 graph가 필요하다. Posted-write flush, cache ownership transition, reset teardown도 각각 edge로 추가한다. 어느 edge가 accessor·barrier·bus rule·device spec 중 무엇으로 보장되는지 설명할 수 없으면 race 가능성이 남아 있다. **Device driver ordering은 barrier 암기가 아니라 observer 사이 happens-before 설계**다.
