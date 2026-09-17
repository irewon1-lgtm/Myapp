# PART 84 · Kernel Virtual Address Space — direct map, vmalloc, vmap, ioremap, page-table lifetime

Kernel virtual address는 하나의 homogeneous pointer space가 아니다. Physical RAM을 일정 offset으로 연결한 direct map, 흩어진 pages를 contiguous virtual range로 재매핑하는 vmalloc/vmap, device register를 special memory type으로 연결하는 ioremap, per-CPU·module·vmemmap·fixmap 같은 reserved region이 서로 다른 translation·cache·lifetime semantics를 가진다. 두 pointer가 모두 `void *`처럼 보인다고 해서 physical contiguity, DMA 가능성, cache attribute, `virt_to_phys()` 사용 가능성이 같지 않다. Kernel VA correctness는 **주소가 어느 region에서 왔고 어떤 page-table owner가 언제 mapping을 만들고 제거하는가**를 추적하는 문제다.

## CHAPTER 01 · Kernel virtual address space는 기능별 region으로 partition된다

64-bit kernel은 큰 virtual space를 direct mapping, vmalloc/ioremap, vmemmap, modules, fixmap, architecture-specific holes 같은 영역으로 나눈다. 각 region은 address translation을 만드는 방법과 허용 memory type이 다르다. User-space address처럼 per-process마다 완전히 다른 layout이 아니라 많은 kernel mappings가 모든 process의 kernel half에서 공통으로 보일 수 있어 stale mapping bug의 영향 범위도 크다. Pointer를 debugging할 때 numeric address만 보지 말고 **그 주소가 어느 kernel VA region에 속하는지** 먼저 판정해야 translation과 lifetime을 올바르게 해석할 수 있다.

## CHAPTER 02 · Direct map은 physical RAM을 predictable kernel virtual address로 alias한다

많은 architecture에서 ordinary RAM은 kernel이 항상 접근할 수 있는 direct-mapped region에 연결된다. Page allocator나 kmalloc이 반환한 low-level memory는 이 direct map을 통해 CPU가 접근할 수 있어 별도 page-table mapping을 매 allocation마다 만들 필요가 없다. 이 특성 때문에 direct-map pointer는 physical page identity와 강하게 연결되지만, address randomization과 architecture layout을 무시해 numeric formula를 직접 코딩해서는 안 된다. **Direct map은 allocation API가 아니라 boot-time/global mapping policy**이며 모든 physical address나 device address가 자동 포함되는 것도 아니다.

## CHAPTER 03 · virt_to_phys는 generic pointer 변환기가 아니라 direct-map 계열의 제한된 helper다

`virt_to_phys()`는 direct mapping 또는 kmalloc 계열처럼 physical backing이 해당 address translation과 직접 대응하는 경우에만 의미가 있다. Vmalloc pointer는 virtual range 아래에 여러 unrelated physical pages가 매핑돼 있으므로 단일 linear physical address로 바꿀 수 없다. Ioremap pointer 역시 bus/device memory semantics를 가지며 DMA address와도 다르다. `pointer가 kernel address니까 virt_to_phys`라는 패턴은 **CPU virtual→CPU physical→device DMA라는 서로 다른 address domain**을 하나로 합치는 오류다.

## CHAPTER 04 · kmalloc은 virtual뿐 아니라 physical contiguity 특성을 일부 제공한다

Kmalloc은 slab/page allocator 위에서 small/medium object를 배치해 반환 pointer가 ordinary direct-map kernel memory가 되도록 한다. 일정 크기 이상의 physically contiguous allocation은 high-order page availability에 의존하므로 system uptime과 fragmentation이 커질수록 실패 가능성이 높아진다. Device가 DMA를 위해 contiguous memory를 필요로 한다고 해서 kmalloc pointer의 physical address를 직접 programming해서는 안 되고 DMA API를 거쳐야 한다. Kmalloc을 선택하는 이유는 **작은 object locality와 direct-map accessibility**, not generic large-buffer convenience다.

## CHAPTER 05 · vmalloc은 physical contiguity requirement를 virtual contiguity로 교환한다

`vmalloc(size)`은 필요한 수의 physical pages를 개별적으로 확보한 뒤 vmalloc VA range에 새 page-table entries를 만들어 하나의 contiguous virtual buffer처럼 제공한다. 그래서 큰 allocation이 high-order contiguous physical block을 요구하지 않아 fragmentation에 더 강하지만, page table memory·mapping setup·TLB shootdown·VA management 비용이 추가된다. Device DMA나 physical layout을 요구하는 code에는 부적합하다. Vmalloc은 **physical allocator problem을 없애는 게 아니라 virtual mapping layer를 추가해 다른 cost profile로 바꾸는 것**이다.

## CHAPTER 06 · vmalloc allocation은 두 자원—physical pages와 virtual range—모두 확보해야 한다

Physical pages가 충분해도 vmalloc virtual area에서 필요한 contiguous VA hole을 찾지 못하면 allocation이 실패할 수 있고, 반대로 VA는 넉넉해도 page allocator가 필요한 backing pages를 확보하지 못할 수 있다. Metadata와 page-table allocation도 별도 failure point다. 32-bit kernel처럼 VA가 좁은 환경에서는 virtual fragmentation이 특히 치명적이며, 64-bit에서도 장시간 다양한 크기의 mapping churn이 allocator behavior를 바꿀 수 있다. Failure telemetry는 `free memory` 하나가 아니라 **VA hole size + backing page availability + page-table allocation**을 분리해야 한다.

## CHAPTER 07 · vmap은 이미 존재하는 pages 배열에 새 contiguous virtual view를 만든다

Vmalloc이 backing pages까지 allocation하는 API라면 `vmap()`은 caller가 가진 `struct page` 집합을 특정 kernel VA range에 mapping해 contiguous pointer view를 만든다. Pages가 file cache, device allocation, non-contiguous pool에서 왔더라도 CPU는 하나의 virtual sequence로 접근할 수 있다. Mapping을 제거해도 backing page lifetime은 별도 owner에게 남을 수 있으므로 `vunmap`과 page free를 같은 operation으로 생각하면 double free/leak가 생긴다. **Virtual mapping ownership과 physical page ownership을 분리**해야 한다.

## CHAPTER 08 · vm_map_ram 계열은 short-lived mapping throughput과 VA fragmentation을 교환한다

작은 page 배열을 빠르게 temporary kernel VA로 연결하기 위한 specialized mapping API는 generic vmap보다 낮은 overhead를 목표로 할 수 있다. 하지만 short-lived mapping용 allocator와 long-lived mapping을 섞으면 virtual address fragmentation이 심해질 수 있다. API가 요구하는 matching unmap과 count를 정확히 지켜야 하며, mapping pointer를 장기간 object field에 저장할 경우 그 API의 intended lifetime과 어긋나는지 확인해야 한다. Fast temporary map은 **장기 stable address allocator가 아니다.**

## CHAPTER 09 · kmap_local_page는 temporary per-thread/local mapping lifetime을 매우 짧게 제한한다

Highmem이나 특정 architecture에서 모든 physical page가 영구 direct map에 들어오지 않을 수 있다. `kmap_local_page()` 같은 API는 page를 현재 execution context에서 짧게 접근할 수 있게 mapping하고 대응 unmap을 요구한다. Mapping pointer를 callback이나 asynchronous work에 넘기면 원래 context가 끝난 뒤 address 의미가 사라질 수 있다. 64-bit 환경에서 우연히 direct-map처럼 동작해도 portable code는 **temporary mapping lifetime을 lexical scope 수준으로 제한**해야 한다.

## CHAPTER 10 · Vmalloc mapping은 page-table population 자체가 measurable cost다

Large vmalloc area를 만들 때 kernel은 VA reservation뿐 아니라 각 backing page를 연결할 page-table hierarchy를 populate해야 한다. 수 MB/GB range를 자주 alloc/free하면 page-table update와 TLB invalidation이 CPU cost와 global synchronization을 유발할 수 있다. Allocation latency가 physical page allocation보다 page-table setup에 지배될 수도 있다. Large transient buffer를 hot path에서 매번 vmalloc하는 대신 pool/reuse를 검토해야 하며, **allocation count × mapped pages**가 중요한 cost metric이다.

## CHAPTER 11 · Vfree는 mapping removal과 TLB visibility 종료를 포함한다

Vmalloc object를 free할 때 VA metadata만 반환해서는 안 된다. Page-table entries를 제거하고 해당 mapping을 캐시한 CPU TLB가 더 이상 old translation을 사용하지 않도록 invalidation을 완료한 뒤 backing pages를 재사용해야 한다. 다른 CPU가 pointer를 보유한 채 vfree하면 page table race가 아니라 direct use-after-free다. Teardown은 먼저 external references/callback을 drain하고 **mapping unpublish→TLB invalidation→physical page release** 순서를 유지해야 한다.

## CHAPTER 12 · TLB shootdown 비용 때문에 mapping churn은 CPU 수가 많을수록 비싸질 수 있다

Kernel mapping이 여러 CPU에서 사용됐을 가능성이 있으면 removal/protection change 후 remote CPUs가 stale TLB entry를 버리도록 coordination이 필요하다. CPU 수와 NUMA topology가 커질수록 IPI와 synchronization cost가 늘어 mapping operation의 tail latency가 커질 수 있다. 같은 total bytes를 한 번 크게 mapping하는 것과 수천 개 작은 vmap/vunmap으로 churn하는 것은 비용 구조가 다르다. VA allocator performance는 **bytes보다 mapping generation count와 shootdown fanout**을 함께 봐야 한다.

## CHAPTER 13 · Huge-page vmalloc은 page-table/TLB 비용을 줄일 수 있지만 allocation constraint를 다시 강화한다

Large vmalloc range에서 PMD-size huge backing을 허용하면 page-table entry 수와 TLB pressure를 줄일 수 있다. 하지만 huge page를 실제로 확보할 수 있어야 하며 alignment·protection·architecture support가 맞아야 한다. Fallback이 ordinary pages로 내려갈 수 있는지 API semantics를 확인해야 한다. Huge mapping optimization은 physical contiguity constraint를 일부 되살리므로 **TLB efficiency와 allocation reliability 사이의 선택**이다.

## CHAPTER 14 · Guard page는 VA 낭비를 감수해 linear overflow를 즉시 fault로 바꾼다

Kernel stack이나 sensitive mapping 주변에 unmapped guard page를 두면 sequential overflow가 다른 object를 조용히 덮는 대신 page fault로 드러날 수 있다. 이는 physical memory를 반드시 소비하는 것이 아니라 virtual address hole을 protection boundary로 사용하는 기법이다. VA가 넓은 64-bit system에서는 매우 유리하지만 mapping density가 높은 환경에서는 region 설계에 포함해야 한다. Guard page는 bounds check의 대체가 아니라 **memory corruption의 blast radius를 줄이고 evidence를 앞당기는 방어층**이다.

## CHAPTER 15 · Module text/data는 executable permission과 lifetime 때문에 ordinary vmalloc data와 다르다

Loadable module은 relocatable text/data를 kernel VA에 배치하지만 code page는 executable permission, read-only-after-init 같은 protection transition을 가져야 한다. Writable+executable 상태를 오래 유지하면 memory corruption이 code injection으로 확대될 수 있다. Relocation이 끝난 뒤 permission을 강화하고 unload 전에는 모든 execution reference를 drain해야 한다. Module VA는 단순 buffer가 아니라 **runtime code generation과 W^X security policy가 적용되는 mapping**이다.

## CHAPTER 16 · Executable mapping 변경은 I-cache coherence까지 고려해야 한다

CPU가 code bytes를 수정하거나 새 executable page를 publish할 때 data cache write와 instruction fetch path가 architecture에 따라 자동 coherent하지 않을 수 있다. Text patching/livepatch/JIT-like kernel mechanism은 required cache flush와 synchronization 뒤에 새 code entry를 실행 가능하게 해야 한다. Page-table execute permission만 켜는 것으로 모든 CPU가 new instruction bytes를 즉시 보는 것은 아니다. **Code publication에는 memory visibility + instruction-cache visibility + execution lifetime** 세 단계가 있다.

## CHAPTER 17 · Per-CPU area는 동일 symbol에 CPU별 다른 physical storage를 연결하는 특수 mapping이다

Per-CPU allocator는 counter/queue처럼 CPU-local state를 sharing 없이 접근하기 위해 CPU마다 별도 backing을 제공한다. Address 계산은 ordinary global pointer와 다르고 CPU migration 중 raw per-CPU pointer를 오래 들고 있으면 wrong shard를 사용할 수 있다. Boot-time first chunk와 dynamic per-CPU allocation의 layout도 architecture/allocator policy에 따라 달라질 수 있다. Per-CPU VA는 **주소 모양보다 current CPU identity와 mapping ownership이 핵심**이다.

## CHAPTER 18 · vmemmap은 각 physical page를 설명하는 metadata 자체를 virtual map으로 만든다

Kernel은 physical pages를 관리하기 위해 `struct page`/folio metadata에 접근해야 하고, vmemmap region은 physical PFN→metadata address 변환을 효율적으로 제공한다. HugeTLB/large mapping optimization은 동일 metadata representation의 memory overhead를 줄이는 특수 기법을 사용할 수 있다. `page metadata도 memory를 먹는다`는 사실은 TB-scale RAM에서 큰 비용이 된다. Physical-memory capacity planning은 user data뿐 아니라 **page descriptors와 page tables 같은 management memory**를 포함해야 한다.

## CHAPTER 19 · ioremap은 vmalloc-like VA를 사용해 device physical/bus resource에 special mapping을 만든다

Device BAR/register를 CPU가 접근하려면 ordinary RAM direct map과 다른 cache/order attribute로 kernel VA에 연결해야 한다. `ioremap()` family는 vmalloc/ioremap region에서 VA를 확보하고 architecture-specific device memory attributes를 적용한다. 반환 `__iomem` pointer를 ordinary RAM처럼 `memcpy`, `virt_to_phys`, cacheable dereference로 처리하면 안 된다. 같은 virtual region allocator를 공유하더라도 **backing type과 accessor semantics가 완전히 다르다.**

## CHAPTER 20 · 동일 physical range를 서로 다른 cache attribute로 alias하면 coherence가 깨질 수 있다

한 physical page가 direct-map에서 WB cacheable인데 다른 VA에서는 uncached/write-combining으로 동시에 mapping되면 architecture에 따라 cache alias와 undefined behavior가 생길 수 있다. Device/persistent-memory mapping은 existing direct mapping과 attribute compatibility를 고려해야 한다. `VA가 다르니 독립 메모리`가 아니라 동일 physical storage의 multiple alias임을 추적해야 한다. Mapping API가 cache type을 제한하는 이유는 **CPU cache coherence contract를 global physical-page 단위로 보존**하기 위해서다.

## CHAPTER 21 · vmalloc_user는 userspace 노출 전에 uninitialized kernel data leakage를 막기 위해 zeroing한다

Kernel vmalloc memory를 userspace VMA에 remap할 수 있는 API는 backing contents가 이전 kernel data를 포함할 수 있다는 보안 문제를 해결해야 한다. `vmalloc_user()`가 zeroed memory를 제공하는 이유는 mapping 순간 stale heap/page contents가 user process로 노출되는 것을 방지하기 위해서다. 직접 `vmalloc()` 후 partial initialization만 하고 userspace에 map하면 unwritten padding/range가 정보 leak이 될 수 있다. User mapping contract는 **lifetime뿐 아니라 initialization completeness**를 요구한다.

## CHAPTER 22 · remap_vmalloc_range는 vmalloc backing pages를 userspace page table에 별도 매핑한다

Kernel에 contiguous하게 보이는 vmalloc VA를 userspace에 공개할 때는 해당 VMA에 실제 backing pages를 연결해야 한다. Userspace pointer와 kernel vmalloc pointer는 서로 다른 VA이며 같은 pages를 공유한다. VMA close 전 kernel이 vmalloc area를 free하면 userspace가 stale PTE를 통해 freed page에 접근할 수 있으므로 reference lifetime이 필요하다. Cross-address-space sharing은 **두 page-table mapping의 teardown order를 하나의 backing-page lifetime에 묶어야 한다.**

## CHAPTER 23 · kvmalloc은 small contiguous fast path와 vmalloc fallback을 추상화한다

많은 caller는 작은 경우 kmalloc의 locality/low mapping cost를 원하지만 큰 경우 physical contiguity failure를 피하기 위해 vmalloc fallback이 필요하다. `kvmalloc` family는 이 선택을 abstraction으로 제공할 수 있지만 caller가 반환 pointer를 physical contiguous라고 가정하면 fallback 순간 bug가 난다. 그래서 paired free도 `kvfree`처럼 provenance를 처리할 수 있는 API를 사용한다. Convenience allocator를 선택했다면 **lowest common denominator semantics**, 즉 ordinary CPU-accessible virtual memory만 의존해야 한다.

## CHAPTER 24 · NUMA-aware vmalloc은 virtual contiguity와 physical locality를 별개로 다룬다

`vmalloc_node`처럼 backing pages를 특정 NUMA node에서 선호하도록 할 수 있어 CPU access locality를 개선할 수 있다. 하지만 VA는 하나의 contiguous range이므로 pointer arithmetic만으로 어느 node의 page인지 알 수 없고 allocation fallback이 다른 node를 사용할 수도 있다. Large buffer가 여러 CPUs/devices에서 공유되면 한 node 고정이 오히려 remote traffic을 늘릴 수 있다. Placement tuning은 **virtual shape가 아니라 actual backing-page node distribution과 access topology**를 측정해 결정해야 한다.

## CHAPTER 25 · Vmalloc VA fragmentation은 free bytes보다 largest hole이 중요할 수 있다

VA allocator에 작은 long-lived mapping이 흩어져 있으면 전체 free virtual space 합계가 충분해도 큰 contiguous VA range를 예약하지 못할 수 있다. 32-bit kernel과 constrained architectures에서 특히 치명적이지만 long-running kernel에서도 mapping churn을 진단할 가치가 있다. `/proc/meminfo`의 VmallocTotal/Used/Chunk와 `/proc/vmallocinfo`를 함께 보면 allocator pressure와 owner를 추적할 수 있다. Capacity metric은 **total free와 largest allocatable extent를 분리**해야 한다.

## CHAPTER 26 · /proc/vmallocinfo는 VA range를 caller와 mapping type까지 연결하는 forensic ledger다

Vmallocinfo는 각 virtual range의 start/end, size, creator, pages, NUMA distribution, ioremap/vmalloc/vmap 같은 type을 보여줄 수 있다. Large VA leak를 찾을 때 allocation count만 보는 것보다 어떤 call site가 long-lived range를 계속 생성하는지 확인할 수 있다. Incident snapshot에는 VmallocUsed뿐 아니라 largest mappings와 caller를 보존해야 fragmentation root cause를 찾을 수 있다. **VA leak는 physical byte leak와 다른 ownership dimension**이다.

## CHAPTER 27 · KASLR과 memory-layout randomization은 region base를 고정 상수로 가정하지 못하게 한다

Kernel address randomization이 활성화되면 direct map, vmalloc/ioremap, vmemmap 등의 base가 boot마다 달라질 수 있다. Driver/debugger가 hard-coded address range로 pointer type을 판정하거나 log parser가 static base를 가정하면 보안 기능이 켜진 production에서 깨진다. Runtime helper와 crash metadata를 사용해 layout을 해석해야 한다. Randomization은 exploit mitigation뿐 아니라 **address identity를 symbolic metadata로 다뤄야 한다는 engineering constraint**를 만든다.

## CHAPTER 28 · Kernel VA exhaustion과 physical-memory exhaustion은 서로 다른 failure signature를 가진다

Vmalloc allocation 실패 시 free RAM이 많다면 VA fragmentation, page-table allocation, mapping limit를 의심해야 하고, 반대로 VA hole은 넉넉한데 backing-page allocation이 실패하면 physical pressure를 봐야 한다. Error path가 둘을 모두 `ENOMEM` 하나로 받더라도 telemetry는 원인을 분리해야 한다. 재시도 정책도 다르다. Physical reclaim은 RAM을 만들 수 있지만 VA fragmentation은 unrelated object를 reclaim해도 큰 hole이 만들어지지 않을 수 있다.

## CHAPTER 29 · Mapping churn fault test는 stale pointer와 TLB lifetime을 의도적으로 흔들어야 한다

Vmap/vunmap을 빠르게 반복하고 여러 CPUs가 mapping을 읽는 workload, module load/unload, userspace remap, hotplug와 concurrent teardown을 조합하면 stale translation/reference bug를 드러낼 수 있다. VA가 재사용돼 old pointer가 우연히 valid한 new object를 가리키면 crash보다 silent corruption이 생긴다. Debug mode에서는 guard gap, poisoning, generation tag, delayed reuse를 활용해 reuse window를 더 잘 드러내는 것이 유리하다. Test는 allocation success만 아니라 **unmap 뒤 old reference가 절대 생존하지 않는지**를 검증해야 한다.

## CHAPTER 30 · Kernel VA correctness는 address provenance와 mapping generation을 함께 증명해야 한다

한 kernel pointer를 검토할 때 `direct map/kmalloc/vmalloc/vmap/ioremap/per-CPU/module 중 어디서 왔는가`, `physical backing owner는 누구인가`, `cache/protection attributes는 무엇인가`, `mapping page table을 누가 만들고 지우는가`, `remote TLB invalidation은 언제 끝나는가`, `pointer를 보유한 callbacks/userspace mappings가 언제 drain되는가`를 답할 수 있어야 한다. Pointer type 하나로 이 정보를 잃으면 API 오용이 시작된다. **Kernel virtual memory는 주소 공간이 아니라 서로 다른 provenance를 가진 mapping lifetime들의 집합**이다.
