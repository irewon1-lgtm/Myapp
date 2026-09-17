# PART 89 · Memory Compaction and CMA — fragmentation, migration scanners, high-order allocation, contiguous DMA

Free memory가 충분하다는 사실과 큰 physically contiguous block을 즉시 만들 수 있다는 사실은 다르다. Page allocator는 order-N allocation에서 2^N개의 연속 physical pages를 요구할 수 있고, system uptime 동안 서로 다른 lifetime의 movable·unmovable pages가 섞이면 total free pages가 많아도 필요한 order block이 사라진다. Memory compaction은 reclaim처럼 bytes를 없애는 것이 아니라 **살아 있는 movable pages를 다른 곳으로 옮겨 free space의 physical geometry를 재배열**한다. CMA는 이 migration capability를 이용해 특정 영역을 일반 movable allocation에 빌려주면서도 device가 large contiguous DMA buffer를 요구할 때 되찾는다. 이 subsystem의 핵심은 capacity가 아니라 **mobility와 fragmentation topology**다.

## CHAPTER 01 · External fragmentation은 free-byte 부족이 아니라 free-page 배치 실패다

Order-0 page allocation은 free page 하나면 되지만 order-9 allocation은 512개의 연속 pages가 같은 aligned buddy block 안에 있어야 한다. Zone 전체에 512개 이상의 free pages가 흩어져 있어도 중간마다 allocated unmovable page가 끼어 있으면 high-order request는 실패한다. `MemFree=2GB` 같은 aggregate metric만 보고 allocation failure를 이상하게 여기는 이유가 여기에 있다. High-order reliability를 판단하려면 **order별 free-area distribution과 pageblock mobility**를 봐야 하며, capacity와 contiguous extent size를 분리해야 한다.

## CHAPTER 02 · Buddy allocator의 order 구조가 fragmentation의 실질적 단위를 만든다

Buddy allocator는 power-of-two 크기의 free blocks를 order별 list에 관리하고, 큰 block을 쪼개 작은 allocation을 만족시키며 adjacent buddy가 모두 free일 때 다시 merge한다. 한번 큰 block 내부에 long-lived allocation이 남으면 주변 작은 free pages가 아무리 많아도 원래 order로 merge되지 않는다. Compaction은 단순 buddy merge가 불가능한 allocated pages를 실제 이동시켜 merge 조건을 다시 만드는 역할을 한다. **Fragmentation은 allocator metadata 문제가 아니라 살아 있는 object placement history가 남긴 물리적 구조**다.

## CHAPTER 03 · Pageblock migratetype은 비슷한 mobility를 모아 future compaction 비용을 줄인다

Kernel은 MOVABLE, UNMOVABLE, RECLAIMABLE 같은 mobility가 비슷한 allocations를 pageblock 단위로 묶으려 한다. Movable pages끼리 모여 있으면 high-order space가 필요할 때 한 pageblock을 비우기 위해 이동시켜야 할 object 종류가 줄어든다. Unmovable slab/kernel pages가 movable area 중앙에 박히면 compaction이 그 경계를 넘지 못해 extent가 갈라진다. GFP mobility hint는 당장 allocation 위치만 결정하는 hint가 아니라 **미래 physical-layout recoverability를 보존하는 장기 policy**다.

## CHAPTER 04 · Compaction은 migrate scanner와 free scanner가 서로 접근하도록 physical space를 탐색한다

Compaction은 대략 zone lower PFN 쪽에서 이동 가능한 allocated pages를 찾는 migrate scanner와 upper PFN 쪽에서 destination free pages를 찾는 free scanner를 진행시켜 occupied pages와 free pages를 분리한다. 두 scanner가 만날 때까지 성공적으로 migration하면 한쪽에 free extents가 더 크게 모이고 buddy allocator가 high-order block을 형성할 수 있다. Scanner가 page마다 expensive migration을 시도하면 CPU/latency가 폭증하므로 skip hints와 pageblock state를 이용한다. **Compaction은 메모리를 정렬하는 linear copy가 아니라 두 후보 집합을 matching하는 migration algorithm**이다.

## CHAPTER 05 · Page migration은 data copy보다 mapping과 ownership 전환이 핵심이다

Movable page를 destination PFN으로 옮기려면 contents를 복사한 뒤 CPU page tables, page cache/xarray, reverse mappings, LRU/refcount, filesystem-private state가 새 folio를 가리키도록 전환해야 한다. Concurrent reader/writer가 old page를 계속 접근할 수 있으므로 migration entry와 lock protocol을 사용해 access를 일시적으로 막거나 retry시킨다. Copy 자체는 가장 쉬운 단계다. **Old physical identity를 참조하던 모든 software edges를 new page로 atomically 수렴시키는 것**이 migration correctness다.

## CHAPTER 06 · Unmovable page 하나가 큰 compaction target 전체를 막을 수 있다

Page table page, 특정 slab/kernel allocation, long-term pin처럼 migration할 수 없는 page가 pageblock에 있으면 해당 physical position을 비울 수 없다. High-order allocation은 alignment까지 요구하므로 strategic 위치의 unmovable page 하나가 수 MB contiguous range 형성을 막을 수 있다. 이런 page를 `몇 KB밖에 안 먹는다`며 무시하면 fragmentation root cause를 놓친다. **Unmovable bytes의 양보다 그 bytes가 어느 physical extent를 끊고 있는지가 중요**하다.

## CHAPTER 07 · Long-term DMA pin은 VM의 mobility promise를 직접 무효화한다

FOLL_LONGTERM/RDMA/VFIO 같은 pin은 device가 같은 physical storage를 계속 참조하므로 compaction이 page를 다른 PFN으로 옮길 수 없다. Pin된 pages가 CMA나 movable zone에 흩어지면 contiguous allocation이 실패할 수 있고 memory hot-remove도 막힌다. Page pin quota를 RSS처럼 byte count만 관리하면 physical fragmentation impact를 반영하지 못한다. Pin policy는 **duration·placement·pageblock distribution을 포함한 mobility budget**으로 보아야 한다.

## CHAPTER 08 · Reclaim과 compaction은 다른 문제를 풀지만 allocator slow path에서 연속해서 만날 수 있다

Reclaim은 clean cache를 버리거나 anonymous pages를 swap해 free pages의 수를 늘린다. Compaction은 이미 존재하는 free pages와 movable pages의 배치를 바꿔 큰 extent를 만든다. High-order request에서 reclaim만 반복하면 free pages는 늘지만 unmovable islands가 남아 계속 실패할 수 있고, compaction만 해도 destination free pages가 부족하면 migration을 수행할 수 없다. Allocation slow path는 **capacity pressure와 fragmentation pressure를 별도 신호로 판단해 reclaim/compaction을 조합**한다.

## CHAPTER 09 · Direct compaction은 allocation caller의 latency budget을 직접 소비한다

High-order allocation이 fast path에서 실패하면 caller가 synchronous compaction을 실행해 필요한 contiguous block을 만들려고 할 수 있다. 이 동안 task는 page scanning, isolation, migration copy와 TLB/rmap 작업에 stall되어 tail latency가 커진다. THP fault처럼 user request critical path에서 direct compaction이 실행되면 memory allocation이 갑자기 수 ms 이상 걸릴 수 있다. `compact_stall`과 allocation trace를 함께 봐야 **느린 request가 CPU 계산이 아니라 physical layout repair를 수행하고 있었음**을 알 수 있다.

## CHAPTER 10 · kcompactd는 fragmentation debt를 background에서 미리 줄인다

`kcompactd`는 high-order request가 들어온 순간만 compaction하는 대신 zone의 fragmentation state를 보고 background에서 free extents를 모을 수 있다. Proactive compaction은 future allocation latency를 낮출 수 있지만 page copy, cache/TLB disturbance, memory bandwidth를 지속적으로 사용한다. Aggressive setting은 application이 느끼는 direct stalls를 줄이는 대신 항상 약간의 migration cost를 지불하게 한다. **Compaction work를 request critical path와 background maintenance 사이 어디에 둘지 결정하는 latency-shaping 정책**이다.

## CHAPTER 11 · compaction_proactiveness는 fragmentation 목표와 migration 비용을 교환한다

Proactiveness를 높이면 kernel이 fragmentation 증가에 더 민감하게 반응해 더 자주 background compaction을 수행한다. High-order allocation success는 좋아질 수 있지만 unrelated workload의 cache locality와 memory bandwidth를 흔들어 latency spike를 만들 수 있다. 0은 proactive compaction을 끄지만 direct compaction 자체를 없애는 값은 아니다. 튜닝은 `THP/CMA success rate`, `compact_stall`, migration pages, CPU time을 함께 보고 **background cost로 direct latency를 얼마나 사는지** 평가해야 한다.

## CHAPTER 12 · compact_memory는 manual trigger이지 정상 운영에서 root cause를 지우는 해결책이 아니다

`/proc/sys/vm/compact_memory`에 값을 써서 모든 zones에 compaction을 요청하면 큰 contiguous blocks를 일시적으로 늘릴 수 있다. 그러나 long-term pin, unmovable slab, poorly classified allocations가 그대로라면 fragmentation은 다시 쌓인다. Incident 때 manual compaction 후 증상이 사라졌다는 사실은 `메모리 부족`이 아니라 fragmentation이 관련됐다는 진단 evidence로 유용하다. Production cron으로 반복 실행해 문제를 숨기기보다 **왜 mobility가 깨졌는지 owner를 찾는 쪽이 우선**이다.

## CHAPTER 13 · Fragmentation index는 free memory 양보다 high-order failure 가능성을 추정한다

Zone의 free blocks가 어떤 orders에 분포하는지 분석하면 high-order allocation 실패가 `실제로 free pages가 부족해서`인지 `free pages는 있지만 잘게 쪼개져서`인지 구분할 수 있다. External-fragmentation metric이 높다면 reclaim보다 compaction/page-placement가 relevant하고, free page 자체가 적다면 먼저 reclaim/capacity 문제를 봐야 한다. 동일 ENOMEM 결과라도 원인이 다르므로 allocator telemetry는 **capacity exhaustion과 fragmentation exhaustion을 분리**해야 한다.

## CHAPTER 14 · THP allocation은 compaction cost를 user page fault 경로로 끌어올 수 있다

Transparent Huge Page는 PMD-size contiguous physical memory를 요구해 fragmented system에서 direct compaction을 촉발할 수 있다. THP가 TLB miss를 줄여 throughput을 높여도 first-touch latency가 compaction stall 때문에 커질 수 있다. Kernel counters의 `compact_stall/compact_success/compact_fail`와 THP allocation/fallback을 함께 보면 hugepage policy가 실제 workload에 이득인지 판단할 수 있다. **Large-page optimization의 비용은 page-table/TLB savings와 allocation-time migration latency의 교환**이다.

## CHAPTER 15 · THP defrag policy는 hugepage를 위해 request가 얼마나 기다릴지 정한다

Workload가 hugepage를 반드시 필요로 하지 않는다면 order-0 fallback으로 빠르게 진행하고 background collapse를 기다리는 선택이 가능하다. 반대로 hugepage benefit이 매우 크다면 direct compaction에 더 오래 기다릴 수 있다. Defrag mode는 correctness가 아니라 latency/throughput 정책이지만, 극단 설정은 request timeout이나 CPU pressure에 operational 영향을 준다. **High-order allocation의 가치가 stall budget보다 큰 workload에만 aggressive compaction을 허용**해야 한다.

## CHAPTER 16 · CMA는 contiguous memory를 영구적으로 비워두지 않고 movable pages에 임시 사용을 허용한다

Contiguous Memory Allocator는 boot/DT/Kconfig로 큰 physical range를 reserve하지만, device가 사용하지 않는 동안 ordinary movable pages가 그 영역을 사용할 수 있게 해 RAM 낭비를 줄인다. 나중에 CMA allocation이 오면 range 안의 movable pages를 밖으로 migrate해 contiguous span을 회수한다. 이 설계가 성립하려면 CMA region에 unmovable allocation이 들어오지 않는다는 전제가 중요하다. **CMA는 예약 pool이라기보다 future evacuation 가능성을 보장한 physical region**이다.

## CHAPTER 17 · CMA allocation success는 free bytes보다 region evacuability에 달려 있다

CMA region 안에 많은 free pages가 있어도 requested aligned range에 pinned/unmovable page가 끼면 allocation이 실패하거나 다른 range를 찾아야 한다. 반대로 region 대부분이 movable pages로 사용 중이어도 destination memory가 충분하고 migration 가능하면 large contiguous buffer를 만들 수 있다. `CmaFree`가 작다는 사실만으로 CMA exhaustion을 판단하면 틀릴 수 있다. **Used CMA가 movable loan인지 permanent blocker인지 구분**해야 한다.

## CHAPTER 18 · CMA alignment는 device와 pageblock constraint를 동시에 만족해야 한다

Device DMA engine이 power-of-two boundary나 large alignment를 요구하면 allocator는 size뿐 아니라 starting PFN 제약을 만족해야 한다. Candidate region이 충분히 커도 alignment 때문에 usable extents가 줄어들 수 있다. Oversized alignment를 안전하다고 무조건 요청하면 fragmentation과 migration 양이 증가한다. Driver는 hardware가 실제로 요구하는 최소 alignment만 선언하고 **software convenience를 physical-placement requirement로 승격하지 않아야** 한다.

## CHAPTER 19 · default CMA region은 dma-buf heap 같은 userspace allocator의 physical backing이 될 수 있다

Modern Linux의 default CMA region이 존재하면 dma-buf heap interface를 통해 userspace frameworks가 physically contiguous shared buffers를 요청할 수 있다. Camera/video/display pipeline처럼 여러 devices가 같은 buffer를 공유할 때 유용하지만, 대규모 long-lived allocations가 CMA region을 점유하면 다른 drivers의 contiguous request를 굶길 수 있다. Buffer-pool sizing은 application memory budget뿐 아니라 **shared CMA pool의 fragmentation과 concurrent peak demand**를 고려해야 한다.

## CHAPTER 20 · CMA region size는 boot-time capacity reservation과 runtime flexibility를 교환한다

CMA를 크게 예약하면 large DMA request 성공률은 높아지지만 해당 region은 unmovable kernel allocations에 사용할 수 없어 일반 allocator flexibility가 줄 수 있다. 너무 작으면 video/camera/huge DMA burst에서 allocation이 실패한다. Workload의 average 사용량보다 simultaneous largest-buffer set과 migration headroom을 기준으로 sizing해야 한다. Dynamic pressure에서 movable pages가 CMA 영역을 활용할 수 있어도 **CMA가 모든 종류의 RAM과 동일한 capacity는 아니다.**

## CHAPTER 21 · Per-NUMA CMA는 device locality와 contiguous capacity를 함께 설계한다

Multi-socket system에서 하나의 global CMA region이 remote node에만 있으면 local PCIe device DMA가 interconnect를 건너 성능이 나빠질 수 있다. Node별 CMA나 device-local reserved region을 사용하면 locality를 개선할 수 있지만 pool 간 capacity sharing은 어려워진다. 한 node는 idle capacity가 남는데 다른 node CMA가 고갈될 수 있다. **Contiguity와 NUMA locality를 동시에 만족시키는 placement constraint**가 pool partitioning을 결정한다.

## CHAPTER 22 · Gigantic HugeTLB도 CMA를 이용해 boot 이후 large physical blocks를 확보할 수 있다

Gigantic hugepages는 일반 buddy order 범위를 넘는 매우 큰 contiguous memory를 요구해 runtime allocation이 어렵다. `hugetlb_cma` 같은 reserved area를 이용하면 movable pages를 evacuate해 boot 이후 gigantic page를 만들 가능성을 높일 수 있다. 대신 이 CMA capacity는 다른 use와 경쟁할 수 있고 per-node sizing을 잘못하면 hugepage pool 생성 실패가 난다. Large-page capacity도 **physical extent reservation + migration policy** 문제다.

## CHAPTER 23 · Memory compaction은 NUMA locality를 희생해 성공률을 높이면 안 된다

Movable page destination을 아무 node에서나 고르면 contiguous block은 만들 수 있어도 page owner의 memory access가 remote로 바뀌어 장기 성능이 악화된다. Compaction은 기본적으로 zone/node boundaries와 allocation policy를 존중해 migration target을 선택해야 한다. Hugepage 하나를 얻기 위해 수천 hot pages를 remote memory로 보내면 local latency benefit보다 큰 비용을 만든다. **Contiguity repair와 placement policy는 함께 유지**되어야 한다.

## CHAPTER 24 · Unevictable/mLocked pages는 reclaim 불가와 migration 가능성을 따로 판단해야 한다

`mlock()`된 page는 swap/reclaim하지 않는 것이 핵심이지만 page migration까지 반드시 금지한다는 의미는 아니다. 반대로 long-term device pin은 physical identity 안정성이 필요해 migration을 실제로 막는다. `reclaim 불가 = unmovable`로 단순 분류하면 compaction 가능성을 과소평가하고, `userspace page = movable`로 단순화하면 pin blocker를 놓친다. **Reclaimability와 migratability는 서로 다른 page properties**다.

## CHAPTER 25 · Slab page mobility는 object allocator 설계에 따라 compaction 가능성이 달라진다

많은 kernel slab allocations는 object 내부 pointer와 lifetime 때문에 일반 page migration처럼 옮기기 어렵고 pageblock의 unmovable islands가 될 수 있다. Reclaimable slab은 shrinker가 object를 버려 page를 free할 수 있지만 active object를 arbitrary PFN으로 복사하는 것과는 다르다. Long-lived small kernel objects가 넓게 퍼지면 high-order fragmentation을 만든다. Slab growth와 fragmentation을 연결해 보려면 **cache별 active objects와 pageblock distribution**을 함께 분석해야 한다.

## CHAPTER 26 · Compaction failure는 migration 대상 부족과 destination 부족을 구분해야 한다

Scanner가 많은 movable pages를 찾지 못한 경우와, movable pages는 있지만 받아줄 free destination pages가 없는 경우는 대응이 다르다. 전자는 page placement/pins/unmovable allocations 문제이고 후자는 reclaim/capacity pressure가 먼저 해결돼야 한다. `compact_fail` counter 하나만으로는 어느 이유인지 충분하지 않아 tracepoints와 zone state를 같이 봐야 한다. **Compaction은 source 후보와 destination 후보 두 자원을 동시에 필요로 하는 matching problem**이다.

## CHAPTER 27 · Compaction latency는 page count보다 migration cost distribution에 좌우된다

Cold anonymous page와 heavily mapped shared page, page-cache folio, huge folio는 migration 시 reverse-map walk와 TLB/page-table update 비용이 다르다. 같은 1000 pages를 옮겨도 mapping fanout과 cache locality에 따라 CPU time이 크게 다를 수 있다. Tail-sensitive system은 migrated-pages count만 보지 말고 compaction duration과 per-page type를 추적해야 한다. **Migration quantity와 migration complexity를 분리**하면 왜 특정 compaction stall만 유난히 긴지 설명할 수 있다.

## CHAPTER 28 · Fragmentation observability는 buddyinfo·pagetypeinfo·vmstat를 함께 사용해야 한다

`/proc/buddyinfo`는 order별 free blocks, `/proc/pagetypeinfo`는 migratetype/pageblock distribution, vmstat는 compaction stall/success/fail과 migration counters를 제공한다. 한 지표만 보면 free block 부족의 원인을 알기 어렵다. DMA pin accounting과 THP/CMA allocation failure까지 같은 timeline에 묶으면 physical-layout debt가 어느 subsystem에서 시작됐는지 찾을 수 있다. **Fragmentation은 snapshot geometry + migration history + allocation demand를 함께 봐야 하는 현상**이다.

## CHAPTER 29 · Compaction fault test는 의도적으로 unmovable islands와 pins를 만들어야 한다

Order-0 allocation/free만 반복하는 synthetic test는 real-world fragmentation을 충분히 만들지 못할 수 있다. Long-lived unmovable kernel objects, movable anonymous pages, long-term pins를 pageblocks에 섞고 high-order/THP/CMA request를 실행해 compaction behavior를 관찰해야 한다. Pin을 해제했을 때 success rate가 회복되는지, proactive compaction이 direct stall을 얼마나 줄이는지 비교한다. **실제 failure topology를 재현하지 않은 compaction benchmark는 allocator fast path만 측정할 위험**이 있다.

## CHAPTER 30 · Contiguous-allocation correctness는 free capacity가 아니라 mobility ledger로 증명해야 한다

Large physical allocation을 안정적으로 제공하려면 `어떤 order/alignment가 필요한가`, `대상 pageblocks에 어떤 migratetype이 들어갈 수 있는가`, `long-term pins/unmovable pages가 어디에 존재하는가`, `destination free pages는 충분한가`, `direct vs background compaction latency budget은 무엇인가`, `CMA region은 누가 얼마나 오래 점유하는가`, `THP/CMA/high-order 실패와 migration counters가 어떻게 연결되는가`를 답할 수 있어야 한다. **Compaction은 메모리를 더 만드는 기능이 아니라 살아 있는 pages의 이동 가능성을 이용해 physical address space의 shape를 다시 만드는 기능**이다.
