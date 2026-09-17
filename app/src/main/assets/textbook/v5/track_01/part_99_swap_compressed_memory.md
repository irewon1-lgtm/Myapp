# PART 99 · Swap and Compressed Memory — swap entries, swap cache, zswap, zram, fault-in, thrashing

Anonymous memory가 RAM에서 밀려났다고 해서 곧바로 “disk에 파일처럼 저장된다”는 설명으로 끝나지 않는다. Linux swap path는 **PTE가 physical page 대신 swap entry를 가리키는 전이, swap slot lifetime, swap cache identity, asynchronous writeout, fault-in, zswap compressed cache, zram block device, memcg accounting, swapoff drain**을 함께 관리한다. 같은 4 KiB logical page가 resident RAM, compressed RAM, backing swap device 사이를 이동할 수 있으므로 correctness는 byte 내용뿐 아니라 어느 representation이 authoritative한지와 reference가 언제 해제되는지에 달려 있다.

## CHAPTER 01 · Swap은 RAM의 단순 연장이 아니라 anonymous-page backing contract다

File-backed page는 깨끗한 상태라면 원본 file에서 다시 읽을 수 있어 reclaim이 page cache entry를 버리는 것으로 끝날 수 있다. Anonymous page는 process heap·stack처럼 수정된 runtime state이므로 RAM에서 제거하려면 나중에 복원할 backing representation이 필요하다. Swap은 이 anonymous state에 backing identity를 부여한다. 중요한 차이는 `free memory가 부족하면 아무 page나 swap으로 보낸다`가 아니라 reclaim policy가 page type, access history, memcg pressure, I/O cost를 고려해 victim을 고른다는 점이다. Swap이 존재해도 working set 전체가 resident보다 크면 반복 fault-in/out으로 latency가 폭발할 수 있고, swap이 없으면 같은 pressure가 더 일찍 OOM으로 전환될 수 있다. 따라서 swap capacity는 usable memory를 공짜로 늘리는 값이 아니라 **어떤 cold anonymous state를 느린 tier로 이동시킬지 허용하는 policy budget**이다. 운영에서는 사용량보다 swap-in/out rate와 stall time을 함께 봐야 한다.

## CHAPTER 02 · PTE의 swap entry는 virtual address가 더 이상 resident PFN을 직접 가리키지 않음을 표현한다

Resident anonymous page가 swap-out되면 해당 virtual address의 page-table entry는 present mapping에서 swap entry representation으로 바뀔 수 있다. CPU가 이 주소를 접근하면 present page가 없으므로 page fault가 발생하고, kernel은 swap entry에서 어느 swap type과 offset에 backing data가 있는지 찾아 복원한다. 이 전이는 단순 pointer 교체가 아니다. 기존 TLB mapping을 무효화하고, reverse mapping과 page references를 정리하며, 여러 process가 COW 이전 상태를 공유했다면 같은 swap-backed object에 대한 reference count도 맞춰야 한다. Stale TLB가 old PFN을 계속 사용하거나 swap slot을 너무 일찍 재사용하면 다른 page의 data가 노출될 수 있다. 따라서 swap-out proof는 `data가 storage에 있다`만으로 끝나지 않고 **모든 CPU가 resident mapping을 더 이상 사용할 수 없고, PTE가 정확한 backing identity를 가리킨다**는 조건까지 포함한다.

## CHAPTER 03 · Swap type과 offset은 backing location identity이고 slot reuse는 generation 문제다

여러 swap device나 swapfile이 동시에 활성화될 수 있으므로 swap entry는 단순 disk block number가 아니라 어느 swap area의 어느 slot인지 식별해야 한다. Slot은 page data가 더 이상 필요 없을 때 free list로 돌아가 다시 다른 anonymous page에 배정될 수 있다. 이때 old PTE나 stale metadata가 남아 있으면 같은 numeric slot이 새 data를 가리키는 ABA형 identity 문제가 생길 수 있으므로 swap subsystem은 reference와 invalidation ordering을 엄격히 관리한다. Swap area 제거 역시 slot을 그냥 버리는 작업이 아니다. 모든 active references가 fault-in되거나 다른 backing으로 이동해 해당 area를 더 이상 가리키지 않아야 한다. Debugging에서 `swap offset 1234`만 기록하면 어떤 swap type과 어떤 시점의 allocation인지 빠져 원인 분석이 어렵다. Backing identity는 type, offset, lifetime을 함께 봐야 한다.

## CHAPTER 04 · Swap cache는 resident page와 swap entry 사이의 단일 identity를 유지하는 중간 계층이다

Page가 swap-backed 상태라고 해서 fault마다 storage에서 새 physical page를 무조건 읽는 것은 아니다. Swap cache는 특정 swap entry와 resident page의 관계를 유지해 같은 backing data를 여러 fault나 writeout path가 중복 생성하지 않도록 돕는다. Swap-out I/O가 진행 중인데 page가 다시 접근되거나, 여러 PTE가 같은 swap entry를 참조하는 경우에도 authoritative resident copy와 backing slot의 관계가 필요하다. Cache entry가 너무 일찍 삭제되면 duplicate read/write와 race가 생길 수 있고, 너무 오래 유지되면 reclaim이 막힌다. 중요한 invariant는 **한 swap identity에 대해 어느 resident page가 현재 authoritative한지, I/O가 진행 중인지, slot이 재사용 가능한지**가 모순 없이 연결되는 것이다. Swap cache를 일반 file page cache와 같은 것으로 뭉개면 writeback source와 lifetime 규칙 차이를 놓친다.

## CHAPTER 05 · Swap-out은 victim 선택과 backing write completion을 분리한다

Reclaim scanner가 anonymous page를 cold하다고 판단했다고 해서 그 즉시 physical frame을 재사용할 수 있는 것은 아니다. Page content가 backing swap에 안전하게 저장되거나 zswap 같은 front cache가 ownership을 받아야 하고, dirty data를 잃지 않는다는 조건이 충족돼야 한다. I/O를 submit한 순간과 durable backing representation이 확보된 순간은 다를 수 있다. Storage congestion이나 allocation failure가 있으면 reclaim path 자체가 stall하거나 다른 victim을 찾아야 한다. Page를 free list로 돌려놓는 시점이 write completion보다 앞서면 physical frame이 새 data로 덮여 original anonymous state를 잃는다. 반대로 completion 이후에도 unnecessary reference를 붙잡으면 reclaim 효과가 사라진다. Swap-out latency를 평가할 때 scanner CPU time, compression time, queueing, storage write latency, final page release를 별도로 측정해야 bottleneck이 보인다.

## CHAPTER 06 · Anonymous reclaim과 file reclaim은 복원 비용 모델이 다르다

Clean file page는 원본 file에서 다시 읽을 수 있으므로 eviction 시 별도 write가 필요 없을 수 있다. Anonymous page는 swap backing을 만들거나 유지해야 하므로 write path와 slot accounting이 따라온다. 그래서 reclaim policy는 `anonymous가 더 중요하다`나 `file cache를 먼저 버린다` 같은 고정 우선순위가 아니라 workload가 실제로 치를 refault 비용을 비교한다. File cache를 과도하게 버리면 executable/data file read I/O가 늘고, anonymous를 과도하게 swap하면 interactive task가 swap fault로 멈춘다. `swappiness` 같은 tuning도 이러한 상대 I/O cost 추정에 영향을 준다. 특히 zram/zswap처럼 swap path가 매우 빠른 compressed RAM일 경우 traditional disk-swap 가정보다 높은 anonymous eviction이 합리적일 수 있다. Policy는 page 종류가 아니라 **재접근 확률 × 복원 비용 × pressure goal**의 trade-off다.

## CHAPTER 07 · 여러 swap device의 priority는 capacity뿐 아니라 I/O 분산과 failure domain을 결정한다

Linux는 여러 swap area를 활성화할 수 있고 priority에 따라 어느 backing store를 먼저 사용할지 정책을 구성할 수 있다. 빠른 NVMe swap과 느린 device를 같은 우선순위로 섞는지, compressed RAM device를 앞에 두는지에 따라 fault latency distribution이 달라진다. Capacity만 합산하면 100 GiB swap처럼 보이지만, 실제 working set이 느린 tier까지 넘어가는 순간 tail latency가 급격히 바뀔 수 있다. Device failure나 detach도 고려해야 한다. 특정 swap area에 live entries가 있는 상태에서 storage가 사라지면 anonymous state를 복구할 수 없으므로 일반 file cache loss보다 훨씬 심각하다. 운영 지표는 total swap used 하나가 아니라 area별 used slots, priority, read/write latency, error count, device health를 분리해야 한다. Tiering policy가 capacity graph와 failure graph를 동시에 만든다.

## CHAPTER 08 · Swap I/O는 page 단위 논리 identity와 block-layer queueing이 만나는 지점이다

Swap slot은 VM 관점의 page identity지만 실제 backing device는 block request, queue depth, merge, scheduler, controller latency를 가진다. Sequential file I/O보다 swap access가 random에 가까우면 device throughput이 같아도 tail latency가 크게 나빠질 수 있다. 여러 process가 동시에 fault-in하면 page fault thread들이 storage completion을 기다리면서 runnable progress가 사라질 수 있다. 반대로 과도한 readahead나 clustering은 곧 쓰지 않을 pages까지 읽어 I/O bandwidth와 memory를 낭비할 수 있다. NVMe처럼 parallelism이 큰 device에서는 queue depth가 throughput을 높일 수 있지만, interactive fault가 bulk swap-out 뒤에 줄 서면 latency가 악화된다. Swap performance를 block bandwidth로만 보지 말고 **fault critical I/O와 background reclaim I/O가 같은 queue에서 어떻게 경쟁하는지** 추적해야 한다.

## CHAPTER 09 · Write completion 이전에 page를 재사용하면 silent data loss가 생긴다

Swap-out path가 storage write를 비동기로 제출할 수 있어도 page lifetime은 completion ordering에 묶여 있다. Device가 DMA로 source page를 읽는 동안 allocator가 그 frame을 다른 object에 재사용하면 swap slot에는 새로운 bytes가 기록되거나 mixed data가 생길 수 있다. DMA mapping, bio/request completion, page lock/writeback state가 이 race를 막는 역할을 한다. Error completion도 성공과 다르게 처리해야 한다. Write가 실패했는데 PTE는 이미 swap entry만 남고 resident copy도 해제됐다면 process state를 복구할 수 없다. 따라서 error path는 resident data를 유지하거나 failure를 더 높은 수준으로 승격해야 한다. Fault injection에서 단순 I/O timeout뿐 아니라 short/error completion, device reset, memory pressure가 동시에 일어날 때 page reference가 살아 있는지 확인해야 한다. Persistence path의 핵심은 **free-after-success ordering**이다.

## CHAPTER 10 · Swap-in page fault는 backing identity를 다시 resident mapping으로 승격하는 transaction이다

CPU가 swap entry를 가진 PTE를 접근하면 fault handler는 새 physical page를 확보하고 backing data를 load하거나 decompress한 뒤, content가 준비된 시점에 PTE를 present mapping으로 바꾼다. Allocation이 먼저 실패할 수도 있고, swap I/O가 error를 반환할 수도 있으며, 같은 address에 다른 thread가 동시에 fault를 일으킬 수도 있다. 이 race에서 두 physical copies가 독립적으로 authoritative해지면 안 된다. Swap cache와 PTE lock, page lock이 중복 load와 mapping transition을 조정한다. TLB는 새 mapping을 보도록 갱신돼야 하고, swap slot reference도 모든 PTE가 더 이상 필요로 하지 않을 때만 해제된다. Fault latency는 `disk read time` 하나가 아니라 allocation, lookup, decompression, I/O, page-table update, scheduler wakeup의 합이다. Major stall 분석은 각 단계 시간을 분리해야 한다.

## CHAPTER 11 · Swap fault의 tail latency는 runnable task를 storage latency에 직접 결박시킨다

File cache miss도 major fault를 만들지만 anonymous swap fault는 process의 private execution state가 없어서 해당 page가 돌아오기 전 instruction을 계속 진행할 수 없는 경우가 많다. Hot request path의 stack/heap page가 swap에 있으면 하나의 page fault가 request P99 latency를 크게 늘릴 수 있다. 여러 dependent pages가 연속 fault되면 storage queueing과 scheduler sleep/wakeup이 반복된다. 평균 swap read latency가 낮아도 cold-start나 burst에서 queue가 길어지면 사용자 체감 stall이 커진다. CPU utilization이 낮은데 latency가 높은 상황에서 swap-in, PSI memory stall, major fault rate를 함께 보면 `CPU가 느리다`는 오판을 피할 수 있다. Capacity planning은 swap 사용량 percentage보다 **어떤 latency-sensitive working set이 resident에서 밀려나는가**를 기준으로 해야 한다.

## CHAPTER 12 · Swap readahead는 spatial locality를 추정하지만 잘못된 예측은 pressure를 되돌린다

Fault난 page 하나만 읽는 대신 인접 swap entries를 미리 읽으면 sequential access pattern에서 다음 fault latency를 줄일 수 있다. 그러나 swap slot allocation order가 virtual address locality나 future access order와 항상 같지는 않다. 잘못된 readahead는 storage bandwidth를 쓰고, 막 확보한 RAM을 곧 쓰지 않을 pages로 채워 다시 reclaim pressure를 만든다. 따라서 readahead는 hit rate와 refault pattern을 기준으로 적응해야 한다. THP가 split된 뒤 여러 base pages가 swap에 흩어졌거나 여러 process의 anonymous pages가 interleaved된 경우 단순 adjacency가 의미를 잃을 수 있다. 관측에서는 read pages 수와 실제 fault-consumed pages 수, wasted readahead, latency savings를 비교한다. Prefetch는 I/O를 없애는 것이 아니라 **미래 latency와 현재 bandwidth/memory를 교환하는 speculation**이다.

## CHAPTER 13 · swapoff는 device disable이 아니라 모든 live swap identity를 drain하는 operation이다

`swapoff`는 metadata flag 하나를 끄고 끝나지 않는다. 대상 swap area에 아직 process PTE가 가리키는 entries가 있다면 그 contents를 RAM으로 fault-in하거나 다른 안전한 representation으로 옮겨야 한다. 따라서 충분한 free memory가 없으면 swapoff가 매우 느리거나 실패할 수 있다. Zswap에 저장된 entries도 backing swap identity와 연결돼 있으므로 해당 swap area를 제거할 때 compressed entries까지 정리되어야 한다. Production에서 storage maintenance 전에 `swapoff` 시간을 무시하면 memory pressure가 급격히 올라 OOM이나 latency spike를 만들 수 있다. Drain 동안 application working set과 swap-in bandwidth가 경쟁한다. 안전한 운영 절차는 target area의 used pages, available RAM, other swap capacity, zswap pool, fault rate를 보고 단계적으로 drain하는 것이다. **Disable은 lifetime 종료를 증명한 뒤에만 완료**된다.

## CHAPTER 14 · swapon은 backing store를 VM state machine에 편입시키는 lifecycle boundary다

Swapfile이나 block device를 활성화하면 kernel은 해당 영역을 anonymous backing store로 사용할 수 있게 metadata를 읽고 slot allocator에 편입한다. File-backed swap은 filesystem layout과 hole 특성, underlying storage semantics가 중요하며 arbitrary sparse file이 안전한 swap backing이 되는 것은 아니다. 활성화 이후에는 일반 file처럼 move, truncate, replace한다고 생각하면 안 된다. Live swap slots가 process state의 유일한 backing일 수 있기 때문이다. Boot automation에서 `swapon 성공`만 확인하지 말고 intended device/size/priority가 맞는지 검증해야 한다. 잘못된 느린 device가 높은 priority로 들어오면 기능은 정상인데 latency가 크게 악화될 수 있다. Swap configuration은 storage mount 옵션 정도가 아니라 **VM이 process memory durability를 맡기는 backing topology**를 만드는 작업이다.

## CHAPTER 15 · zswap은 swap device를 대체하지 않고 swap-out 앞에 compressed RAM cache를 둔다

Zswap은 swap-out되는 pages를 압축해 dynamically allocated RAM pool에 보관하고, cache hit이면 backing swap device I/O를 피한다. Pool이 가득 차거나 entry를 유지할 가치가 낮아지면 compressed page를 backing swap으로 writeback할 수 있으므로 일반적으로 persistent swap area와 함께 동작한다. 이 구조는 RAM을 더 쓰면서 storage I/O를 줄이는 trade-off다. Compression ratio가 좋고 CPU가 여유로우면 큰 이득이 있지만, incompressible workload에서는 compression CPU를 쓰고도 backing I/O가 발생한다. Zswap을 `RAM disk swap`으로 이해하면 zram과 구분이 사라진다. Zswap의 identity는 **swap subsystem 앞의 cache**이고 backing slot identity를 그대로 이용한다. 따라서 swapoff, entry invalidation, memcg accounting과 긴밀히 연결된다.

## CHAPTER 16 · zswap은 swap entry를 key로 compressed object handle을 찾아 fault-in한다

Zswap은 logical swap entry와 compressed storage object의 mapping을 유지해야 한다. Kernel 문서의 설계처럼 swap type별 xarray에서 swap offset을 key로 zsmalloc handle을 찾을 수 있다. Fault가 발생하면 backing device를 읽기 전에 zswap entry가 있는지 확인하고, 있으면 compressed bytes를 새 resident page로 decompress한다. 이때 compressed object의 allocator handle은 일반 direct pointer와 같지 않아 mapping/unmapping 규칙을 따른다. Swap slot이 더 이상 참조되지 않으면 zswap entry도 invalidate해 compressed memory를 반환해야 한다. Stale xarray entry가 slot reuse 이후 남으면 완전히 다른 process page를 잘못 복원할 수 있으므로 backing identity lifetime과 compressed-cache lifetime이 일치해야 한다. Debugging에서는 pool bytes만 보지 말고 stored pages, duplicate/same-value pages, invalidation과 rejection counters를 함께 봐야 한다.

## CHAPTER 17 · Compression ratio는 saved I/O와 CPU cycles, metadata overhead를 동시에 바꾼다

4 KiB page가 1 KiB로 압축되면 같은 RAM으로 여러 swapped pages를 보관할 수 있고 backing device write를 피할 확률이 높아진다. 그러나 compressor는 swap-out과 swap-in critical path에서 CPU cycles를 사용하고, compressed object allocator도 metadata와 fragmentation overhead를 가진다. `compressed size / original size`만 보면 실제 pool memory를 과소평가할 수 있다. 같은-value page처럼 거의 storage를 쓰지 않는 특수 case와 incompressible page도 workload mix에 따라 비율이 크게 달라진다. CPU가 포화된 service에서는 I/O 절감보다 compression contention이 latency를 악화시킬 수 있고, 느린 flash를 쓰는 mobile system에서는 반대로 CPU trade가 유리할 수 있다. 선택 기준은 **saved backing I/O latency와 energy가 compression/decompression cost보다 큰가**이며, percentile 단위로 검증해야 한다.

## CHAPTER 18 · Incompressible page는 zswap store rejection과 backing write를 유발할 수 있다

모든 anonymous page가 잘 압축되는 것은 아니다. Encrypted/compressed application buffers, random-like data는 compressor output이 충분히 작아지지 않아 zswap이 entry 저장을 거절할 수 있다. 이 경우 reclaim이 실패한 것이 아니라 backing swap path로 계속 진행할 수 있다. 문제는 workload가 지속적으로 incompressible한데 zswap writeback까지 제한한 구성이다. 같은 pages가 반복해서 compression attempt 후 거절되면 CPU를 낭비하면서 reclaim efficiency가 떨어질 수 있다. 따라서 rejection reason counters와 compression ratio를 관측해야 한다. `zswap enabled=true`는 실제 hit ratio나 benefit을 말해주지 않는다. 운영 정책은 acceptance, rejection, eviction, backing writes를 구분하고, workload 변화로 incompressible 비율이 높아지면 compressor/pool 설정 또는 zswap 사용 자체를 재평가해야 한다.

## CHAPTER 19 · zswap pool limit과 hysteresis는 compressed cache가 RAM pressure를 다시 만드는 것을 막는다

Zswap pool은 on-demand로 커지지만 무제한이면 reclaim이 RAM을 확보하려고 anonymous pages를 압축한 결과 압축 pool이 RAM을 다시 점유하는 자기모순이 생길 수 있다. `max_pool_percent` 같은 limit은 compressed cache가 physical memory의 일정 비율을 넘지 않도록 bound를 둔다. Pool이 limit에 닿았을 때 즉시 조금 비워질 때마다 새 page를 다시 받아들이면 thrash가 생길 수 있어 acceptance threshold hysteresis가 중요하다. High pressure에서 `store→evict→store→evict`가 반복되면 CPU와 I/O를 모두 쓰면서 resident working set은 개선되지 않는다. 관측해야 할 값은 pool size뿐 아니라 pool-limit-hit, reject, eviction, backing write, acceptance recovery다. Cache controller는 단순 최대 크기가 아니라 **진입과 재진입 조건을 다르게 두어 oscillation을 억제**해야 한다.

## CHAPTER 20 · zswap eviction은 compressed RAM에서 backing swap으로 cold entry를 내려보낸다

Compressed pool이 pressure를 받으면 zswap은 cold entry를 선택해 backing swap device로 writeback할 수 있다. 이때 source는 resident process page가 아니라 compressed object이므로 decompress 또는 write path의 변환 비용과 backing I/O가 발생한다. Eviction completion 전 compressed entry를 버리면 backing data가 사라지고, completion 후에도 reference를 남기면 pool이 줄지 않는다. Device congestion이 심하면 zswap shrink가 swap I/O backlog를 늘려 latency를 악화시킬 수 있다. 반대로 backing이 빠르면 cold compressed pages를 proactive하게 내려 RAM을 hot working set에 돌려주는 것이 유리할 수 있다. Zswap은 `RAM에 최대한 오래 보관`이 목적이 아니라 **RAM과 backing swap의 상대 비용을 이용해 cold anonymous state를 어디에 둘지 결정하는 cache**다.

## CHAPTER 21 · Runtime compressor 변경은 기존 entries를 즉시 재압축하지 않고 multiple pools를 만들 수 있다

Zswap compressor parameter를 runtime에 바꿔도 이미 저장된 compressed pages를 새 algorithm으로 즉시 변환하는 것은 아니다. 기존 entries는 original compressor/pool과 함께 남고 새 stores가 새 pool을 사용할 수 있다. 따라서 설정 화면에는 compressor 하나가 보이는데 memory에는 여러 generation의 pools가 공존할 수 있다. Old pool은 그 entries가 fault-in, invalidation, eviction으로 모두 사라진 뒤에야 해제된다. 운영자가 compressor change 직후 memory usage나 ratio가 즉시 새 특성으로 바뀔 것이라 기대하면 잘못된 결론을 낼 수 있다. Generation별 lifetime이 있는 configuration change다. Rollback이나 반복 변경을 자주 하면 transition period가 길어질 수 있으므로 change 이후 old pool drain 상태를 관측해야 한다. **Configuration value와 live object generation은 별개 state**다.

## CHAPTER 22 · memcg의 zswap writeback 정책은 한 tenant의 compressed pages가 host I/O를 만드는 방식을 제한한다

Cgroup은 memory와 swap 사용량뿐 아니라 zswap writeback behavior에도 영향을 줄 수 있다. 특정 cgroup에서 zswap writeback을 막으면 compressed pool entry가 backing swap으로 내려가지 못해 I/O를 줄일 수 있지만, store failure나 incompressible pages가 반복될 때 reclaim이 같은 pages를 계속 시도하며 비효율이 커질 수 있다. 한 tenant의 policy가 global zswap pool pressure와 다른 tenants의 reclaim latency에 영향을 줄 수 있으므로 isolation을 단순 byte quota로만 보기는 어렵다. `memory.swap.max` 같은 swap limit과 zswap policy, host-wide pool limit을 함께 해석해야 한다. Tenant가 swap을 거의 쓰지 못하도록 제한됐는데 anonymous working set이 증가하면 reclaim은 더 빨리 OOM path로 갈 수 있다. Policy의 목적은 **어느 계층에서 latency/I/O/memory debt를 허용할지 명시하는 것**이지 무조건 swap을 없애는 것이 아니다.

## CHAPTER 23 · zswap shrinker는 cold compressed memory를 proactive하게 회수하는 별도 pressure loop다

Compressed pool에 오랫동안 접근되지 않은 cold entries가 많이 남으면 그 RAM을 active working set에 쓰는 편이 유리할 수 있다. Zswap shrinker를 사용하면 global memory pressure와 pool state에 따라 cold compressed pages를 backing swap으로 내려보내 pool을 줄일 수 있다. 이 과정은 reactive fault path와 별개 background control loop이므로 너무 공격적이면 backing I/O를 늘리고, 너무 보수적이면 compressed pool이 hot RAM을 잠식한다. Shrinker scan rate와 device latency, fault hit rate를 함께 관측해야 한다. `pool이 작아졌다`는 사실만으로 성공이 아니다. 이후 동일 pages가 곧 fault-in되면 writeback과 readback을 연달아 수행한 셈이다. **Coldness prediction의 정확도와 reclaim benefit**을 refault 관점에서 평가해야 한다.

## CHAPTER 24 · zram은 compressed cache가 아니라 block device 자체가 RAM-backed compressed storage다

Zram은 `/dev/zramN` block device를 만들고 그 device에 쓰이는 blocks를 compressed RAM에 저장한다. 이를 swap device로 포맷해 사용할 수 있지만 architecture는 zswap과 다르다. Zswap은 일반 swap entry 앞의 cache이며 backing swap으로 eviction할 수 있는 반면, zram은 VM이 보기에 하나의 block device다. 따라서 `zswap + disk`와 `zram swap`은 같은 knob의 다른 이름이 아니다. Zram은 disk I/O 없이 빠른 compressed swap을 제공할 수 있지만 compressed data와 allocator metadata가 physical RAM을 소비하므로 지나친 logical disksize는 실제 capacity를 만들지 않는다. 설정할 때 logical zram size, actual compressed memory, incompressible pages, memory limit을 분리해야 한다. **Logical swap capacity와 physical RAM consumption이 비선형 관계**를 가진다.

## CHAPTER 25 · zram memory limit은 incompressible workload가 physical RAM을 잠식하는 것을 bound한다

Zram의 logical `disksize`는 addressable block capacity이고 실제 physical memory usage는 compression ratio와 allocator overhead에 따라 달라진다. Incompressible pages가 많으면 logical 1 GiB가 거의 1 GiB에 가까운 RAM을 먹을 수 있다. `mem_limit` 같은 bound를 두면 zram 자체가 host OOM을 유발할 정도로 성장하는 것을 막을 수 있지만, limit에 도달한 뒤 swap write가 실패하거나 upstream reclaim이 다른 경로를 찾아야 한다. 따라서 limit은 독립 안전장치이면서 capacity promise가 아니다. `orig_data_size`, `compr_data_size`, `mem_used_total`, huge/incompressible page counters를 함께 봐야 allocator fragmentation과 metadata overhead까지 알 수 있다. Compression ratio만으로 host memory budget을 잡으면 실제 사용량을 과소평가할 수 있다. Zram은 **RAM을 압축해 쓰는 subsystem이지 RAM을 생성하는 subsystem이 아니다**.

## CHAPTER 26 · zram backing writeback은 cold compressed blocks를 더 느린 storage로 내려 hybrid tier를 만들 수 있다

일부 zram 구성은 backing device를 연결해 idle 또는 incompressible blocks를 외부 storage로 writeback할 수 있다. 그러면 zram은 pure-RAM swap에서 compressed RAM + secondary storage의 tiered block device로 변한다. Writeback된 block을 다시 접근하면 backing read latency가 fault path에 들어오므로 `zram이면 항상 RAM latency`라는 전제가 깨진다. Idle marking, writeback policy, backing device endurance와 queue congestion을 함께 고려해야 한다. Backing write를 너무 공격적으로 하면 zswap과 비슷한 I/O trade-off가 생기지만 control surface와 identity는 block-device level에 있다. 관측에서 `bd_reads`, `bd_writes`, resident compressed bytes를 분리하면 어떤 tier가 실제 요청을 서비스하는지 알 수 있다. Hybrid mode는 **capacity와 latency distribution을 동시에 바꾸는 topology change**다.

## CHAPTER 27 · memcg swap accounting은 host free memory와 tenant entitlement를 분리한다

Host에 swap이 남아 있어도 특정 cgroup의 `memory.swap.max`가 낮으면 그 tenant의 anonymous pages는 더 이상 swap backing으로 이동하지 못할 수 있다. 반대로 swap limit이 넓으면 한 cgroup이 많은 cold state를 backing swap에 남겨 host I/O와 fault storm을 만들 수 있다. Memory limit과 swap limit은 함께 봐야 한다. Resident memory를 줄이는 데 성공했어도 swap debt가 커져 latency-sensitive restart에서 대량 fault-in이 생길 수 있다. Container platform이 `memory.max`만 설정하고 swap policy를 명시하지 않으면 node configuration에 따라 behavior가 달라질 수 있다. Isolation proof는 bytes quota뿐 아니라 **pressure가 발생했을 때 resident reclaim, swap-out, OOM 중 어느 transition이 허용되는지**까지 포함해야 한다. Tenant별 PSI와 swap I/O를 상관해야 noisy-neighbor를 찾을 수 있다.

## CHAPTER 28 · swappiness는 percentage가 아니라 file-vs-anonymous reclaim의 상대 I/O cost 모델이다

Linux의 swappiness는 0~200 범위에서 swap I/O와 filesystem paging의 상대 비용을 VM에 전달하는 tuning으로 해석해야 한다. `60이면 RAM 60%에서 swap 시작` 같은 설명은 틀리다. 낮은 값은 swap I/O가 상대적으로 비싸다고 보고 file-backed reclaim을 선호하게 만들고, 높은 값은 swap이 더 싸다고 판단하게 한다. Zram/zswap처럼 in-memory compressed swap이 file storage보다 빠른 경우 100보다 높은 값도 합리적일 수 있다. 그러나 최적값은 workload access pattern과 device latency에 따라 달라진다. 너무 낮추면 hot file cache를 과도하게 버리고 anonymous memory를 끝까지 붙잡을 수 있고, 너무 높이면 latency-sensitive heap pages가 자주 swap될 수 있다. Tuning은 **fault cost와 refault rate의 실측 비교**로 결정해야 한다. 변경 전후에 anonymous/file scan 비율, swap-in latency, cache miss, PSI를 함께 비교하지 않으면 knob 자체의 효과와 workload 변화를 구분할 수 없다.

## CHAPTER 29 · Thrashing은 swap 사용량이 아니라 page 이동률이 useful work를 압도하는 상태다

Swap이 80% 사용돼도 대부분 cold pages가 장기간 움직이지 않으면 시스템은 안정적일 수 있다. 반대로 swap 사용량이 작아도 hot working set이 RAM보다 조금 커서 같은 pages가 swap-out과 fault-in을 반복하면 CPU, compression, storage I/O, scheduler wakeup이 useful application work를 압도한다. Thrashing의 증거는 swap-in/out rate, major fault, refault, PSI memory stall, runqueue idle/blocked pattern, I/O latency가 함께 상승하는 것이다. Zswap에서도 store→evict→fault-in loop가 생기면 disk보다 빠르더라도 CPU와 memory bandwidth를 낭비한다. 해결은 swap size를 단순 추가하는 것이 아니라 working set 축소, memory 증설, admission control, reclaim policy 조정 중 원인에 맞는 것을 선택해야 한다. **Capacity와 churn을 분리해서 보는 것**이 핵심이다.

## CHAPTER 30 · Swap correctness는 representation, backing, progress, pressure 네 ledger를 동시에 닫아야 한다

신뢰할 수 있는 swap subsystem 분석은 네 상태를 따로 유지한다. Representation ledger는 각 virtual page가 resident PTE, swap entry, zswap entry, zram/backing block 중 어디에 authoritative data를 갖는지 기록한다. Backing ledger는 swap type/offset과 compressed object 또는 device block의 lifetime을 연결한다. Progress ledger는 swap-out write, fault-in read/decompress, swapoff drain이 어느 단계까지 끝났는지 나타낸다. Pressure ledger는 reclaim이 실제 free pages를 만들었는지, compressed pool이나 I/O queue에 debt를 옮겼는지 보여준다. OOM, device error, compressor rejection, swapoff, cgroup limit 같은 모든 종료 경로에서 네 ledger가 모순 없이 닫혀야 한다. Swap은 `느린 RAM`이 아니라 **anonymous state를 여러 storage representation 사이에서 이동시키는 transactional memory-tier protocol**이다.
