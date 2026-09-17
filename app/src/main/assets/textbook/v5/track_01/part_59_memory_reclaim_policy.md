# PART 59 · Memory Reclaim Policy — working set, MGLRU, direct reclaim, writeback, memcg, PSI

Memory pressure에서 핵심 질문은 `RAM이 부족하다`가 아니다. **어떤 page를 cold로 판단해 버릴 것인지, dirty state를 어떻게 처리할지, 누가 reclaim 비용을 지불할지, 어느 cgroup에 pressure를 귀속할지, progress가 없을 때 언제 OOM으로 전환할지**가 실제 정책이다.

## CHAPTER 01 · Reclaim은 free memory 확보가 아니라 cache policy 실행이다

Reclaim은 physical page를 재사용 가능하게 만드는 과정이지만, 어떤 page를 버리는지는 system cache policy와 같다. File-backed clean page는 재읽을 수 있고 anonymous page는 swap이나 process state와 연결된다. 잘못된 victim selection은 free page는 만들면서도 곧 다시 fault되는 thrashing을 만든다.

## CHAPTER 02 · Watermark는 background reclaim과 allocation stall 경계를 만든다

Free page가 특정 watermark 아래로 내려가면 background reclaim thread가 깨어나고, 더 낮은 수준에서는 allocation caller가 direct reclaim에 들어갈 수 있다. 이 차이는 performance에 중요하다. Background reclaim CPU는 별도 context가 지불하지만 direct reclaim은 request thread latency에 그대로 붙는다.

## CHAPTER 03 · kswapd는 burst 전에 free-page reserve를 회복하려 한다

kswapd는 zone/node free memory가 낮아졌을 때 비동기로 reclaim을 진행해 향후 allocation이 direct reclaim에 빠지지 않도록 reserve를 회복한다. 너무 늦게 깨어나거나 너무 빨리 잠들면 burst allocation 때 allocstall이 증가한다. `kswapd CPU가 낮다`는 사실이 항상 좋은 것은 아니다.

## CHAPTER 04 · Direct reclaim은 allocator latency path 안에서 실행된다

Allocation context가 reclaim을 허용하면 caller thread가 page scan, writeback dependency, slab shrink 같은 work를 직접 수행할 수 있다. 이때 application profile에서는 `malloc/new가 갑자기 느림`으로 보일 수 있지만 실제 원인은 allocator가 아니라 global memory pressure다. Allocation latency를 memory-pressure metric과 함께 봐야 하는 이유다.

## CHAPTER 05 · GFP flags는 reclaim에서 허용되는 부작용 범위를 제한한다

Kernel allocation은 sleep, I/O, filesystem recursion, direct reclaim을 허용할지 flag로 표현한다. `GFP_NOIO`, `GFP_NOFS`, NOWAIT 계열은 단순 성능 option이 아니라 **현재 lock/context에서 어떤 reclaim recursion이 안전한가**를 나타낸다. 잘못된 flag는 deadlock이나 allocation failure를 만든다.

## CHAPTER 06 · Clean file page는 재생성 비용을 알고 버려야 한다

Page cache의 clean page는 backing file에서 다시 읽을 수 있어 reclaim하기 쉽지만 working-set hot page를 버리면 곧 storage read와 page fault가 발생한다. 따라서 `clean이면 싸다`만으로 victim을 고르면 sequential scan이나 large file workload가 application hot cache를 밀어낼 수 있다.

## CHAPTER 07 · Dirty page는 reclaim 전에 writeback dependency를 가진다

Dirty file page는 최신 data가 RAM에만 있으므로 그냥 버릴 수 없다. Writeback을 시작하고 persistence path가 진행돼야 reclaim 가능해진다. Storage latency가 높거나 device queue가 포화되면 memory pressure가 I/O pressure로 변환돼 reclaim stall이 길어진다.

## CHAPTER 08 · Anonymous page reclaim은 swap policy와 연결된다

Anonymous memory는 backing file에서 재읽을 수 없으므로 swap backing이 있거나 process state를 종료하지 않는 한 단순 drop이 불가능하다. Swap-out은 RAM을 확보하지만 future access에서 swap-in latency를 만든다. Reclaim policy는 anonymous/file page의 재생성 비용과 access probability를 비교해야 한다.

## CHAPTER 09 · Refault는 방금 버린 page가 실제 working set이었는지 알려준다

Reclaimed page가 짧은 시간 안에 다시 fault되면 victim selection이 잘못되었다는 강한 signal이다. Workingset/refault tracking은 단순 access bit보다 **eviction 후 다시 필요한 정도**를 관찰한다. Refault rate가 높으면 free memory가 유지돼도 workload는 reclaim-thrash 상태일 수 있다.

## CHAPTER 10 · Working set은 allocated set과 다르다

Process가 20GB virtual/committed memory를 가지고 있어도 active working set은 훨씬 작을 수 있고, 반대로 작은 resident set도 latency-critical hot pages로 구성될 수 있다. Reclaim은 allocation size가 아니라 access recency/frequency와 refault cost를 근거로 해야 한다.

## CHAPTER 11 · Traditional LRU approximation은 exact access history를 유지하지 않는다

모든 memory access에 global timestamp를 기록하는 것은 지나치게 비싸다. Kernel은 page state와 sampled access information을 사용해 recency를 근사한다. 근사 policy가 workload pattern과 맞지 않으면 scan overhead와 victim error가 증가한다. Reclaim algorithm은 accuracy와 tracking overhead의 trade-off다.

## CHAPTER 12 · MGLRU는 recency를 여러 generation으로 표현한다

Multi-Gen LRU는 page access recency를 여러 generation으로 나눠 aging과 eviction을 구성한다. 목표는 hot/cold 분류 정확도를 높이고 page reclaim CPU와 thrashing을 줄이는 것이다. 핵심은 `LRU list가 여러 개`라는 구조보다 **access aging과 eviction generation을 분리해 working-set estimation을 개선**하는 데 있다.

## CHAPTER 13 · Aging과 eviction 속도가 workload 변화보다 느리면 stale hotness가 남는다

Page가 과거에 hot했다는 정보가 너무 오래 유지되면 현재 cold page를 보호해 새로운 working set이 들어올 공간을 막을 수 있다. 반대로 aging이 너무 공격적이면 재사용 interval이 긴 page를 반복 eviction한다. Reclaim policy는 workload phase 변화 속도와 memory pressure 정도에 적응해야 한다.

## CHAPTER 14 · Reclaim scan 자체가 CPU와 cache bandwidth를 소비한다

Large memory에서 page metadata를 과도하게 scan하면 reclaim할 page를 찾는 비용 자체가 병목이 된다. Scan efficiency는 scanned pages 대비 reclaimed pages, refault, CPU time으로 봐야 한다. `reclaimed MB/s`만 높아도 scan amplification이 크면 application CPU를 빼앗는다.

## CHAPTER 15 · Slab/cache reclaim은 user page reclaim과 다른 shrinker protocol을 가진다

Dentry/inode 등 kernel cache도 pressure에서 줄일 수 있지만 각 subsystem이 어떤 object를 reclaim 가능하다고 판단하는지 다르다. Shrinker가 global lock이나 slow I/O에 의존하면 memory pressure 시 kernel-wide latency가 증가할 수 있다. Kernel memory도 `freeable/nonfreeable` 이분법이 아니다.

## CHAPTER 16 · Reclaim recursion은 filesystem와 I/O lock graph를 건드릴 수 있다

Allocation을 하던 코드가 direct reclaim에 들어가 filesystem callback이나 writeback을 호출하면 현재 보유 lock과 reclaim path lock이 cycle을 만들 수 있다. NOFS/NOIO scope가 필요한 이유다. Memory pressure가 평소엔 보이지 않던 lock-order bug를 드러내는 경우가 많다.

## CHAPTER 17 · Dirty throttling은 write producer가 reclaim을 압도하지 못하게 한다

Application이 storage writeback 속도보다 빠르게 dirty page를 생성하면 memory가 dirty cache로 채워진다. Dirty limit과 throttling은 producer를 늦춰 background writeback이 따라오게 한다. 이때 write syscall latency가 증가하는 것은 storage뿐 아니라 dirty-memory control loop의 결과일 수 있다.

## CHAPTER 18 · Writeback congestion은 memory와 block-I/O queue를 결합한다

Dirty page를 reclaim하려면 block layer와 device가 write를 소화해야 한다. Device tail latency가 증가하면 dirty lifetime이 길어지고 reclaimable clean page 비율이 줄어든다. 결과적으로 memory pressure가 device queue saturation을 키우고 다시 reclaim latency를 늘리는 feedback loop가 생길 수 있다.

## CHAPTER 19 · Swap cache와 zswap은 eviction cost를 다른 자원으로 이동한다

Compressed swap cache는 storage I/O를 줄이는 대신 CPU와 RAM을 사용한다. 압축률이 낮거나 CPU가 포화되면 이득이 사라질 수 있다. `swap 사용량` 하나가 아니라 compressed size, original size, compression CPU, refault latency를 함께 측정해야 한다.

## CHAPTER 20 · Memory cgroup은 pressure와 reclaim을 tenant boundary로 나눈다

cgroup memory limit은 host 전체 free memory가 남아 있어도 특정 workload에 reclaim/throttling/OOM을 유발할 수 있다. 따라서 container 안의 OOM을 host-wide shortage로 해석하면 틀린다. Allocation이 어느 memcg에 charge되었는지와 hierarchy limit을 확인해야 한다.

## CHAPTER 21 · Memcg reclaim은 global optimum과 tenant fairness가 충돌할 수 있다

특정 cgroup이 limit을 넘으면 그 cgroup 내부 page를 reclaim하는 것이 isolation에는 맞지만 host 전체 기준 더 cold한 page가 다른 cgroup에 있을 수 있다. Resource isolation은 cache efficiency를 희생할 수 있다. Multi-tenant system에서는 fairness와 global hit rate를 별도 목표로 둬야 한다.

## CHAPTER 22 · PSI는 pressure를 사용량이 아니라 stall 시간으로 본다

Memory usage 95%라도 reclaim 없이 안정적으로 동작할 수 있고, usage 70%에서도 direct reclaim 때문에 task가 자주 stall할 수 있다. Pressure Stall Information은 runnable/workload가 resource shortage 때문에 얼마나 멈췄는지를 관찰한다. Capacity alert는 utilization과 stall을 함께 봐야 한다.

## CHAPTER 23 · Direct-reclaim latency는 tail request와 강하게 결합될 수 있다

평균 allocation은 빠르지만 일부 request가 watermark 하락 순간 direct reclaim을 떠맡으면 p99/p999가 급증한다. 이 패턴은 GC pause나 storage hiccup처럼 보일 수 있다. Request trace에 allocstall/reclaim event를 시간축으로 합쳐야 원인을 구분할 수 있다.

## CHAPTER 24 · Compaction과 reclaim은 high-order allocation에서 함께 나타날 수 있다

큰 contiguous physical allocation은 free page 총량이 충분해도 fragmentation 때문에 실패할 수 있다. Reclaim으로 page를 비우고 compaction으로 movable page를 옮기는 추가 비용이 발생한다. High-order allocation latency를 단순 `free memory 부족`으로 설명하면 fragmentation pressure를 놓친다.

## CHAPTER 25 · THP allocation policy는 page fault latency와 TLB efficiency를 교환한다

Transparent huge page를 얻기 위해 aggressive reclaim/compaction을 수행하면 fault path가 길어질 수 있다. 반대로 fallback을 쉽게 허용하면 huge-page coverage와 TLB reach가 낮아질 수 있다. Latency-sensitive workload는 allocation stall과 steady-state memory performance를 함께 측정해야 한다.

## CHAPTER 26 · Reclaim이 progress하지 못하면 OOM decision으로 넘어간다

반복 scan과 writeback 후에도 allocation을 만족시킬 수 없으면 system은 더 이상 reclaim만으로 progress를 보장할 수 없다. OOM handling은 victim을 종료해 memory를 회수하는 극단적 progress mechanism이다. OOM은 `RAM 100%` 이벤트가 아니라 reclaim progress failure의 결과다.

## CHAPTER 27 · OOM victim 선택은 memory size만으로 결정되지 않는다

Victim policy는 process/cgroup score와 보호 설정, shared memory, recoverability 등 여러 요소와 결합될 수 있다. 가장 큰 process를 죽이면 된다는 운영 규칙은 service criticality와 restart storm을 무시한다. OOM 이후 system이 안정화되는지까지 failure model에 포함해야 한다.

## CHAPTER 28 · Proactive reclaim은 direct-reclaim spike를 앞당겨 분산하는 전략이다

Access monitoring 등을 사용해 pressure가 심해지기 전에 cold memory를 조금씩 회수하면 request thread가 직접 reclaim을 떠맡는 burst를 줄일 수 있다. 대신 잘못된 cold prediction은 unnecessary refault를 만든다. Proactive policy는 tail latency 감소와 cache hit loss를 같이 평가해야 한다.

## CHAPTER 29 · Reclaim tuning은 free memory 목표가 아니라 stall·refault·scan cost를 최적화해야 한다

Watermark나 swappiness류 knob를 조정한 뒤 free memory가 늘었다는 이유만으로 성공이라 할 수 없다. Direct reclaim count, kswapd CPU, scan/reclaim ratio, refault, PSI, swap I/O, application p99가 함께 좋아졌는지 확인해야 한다. 하나의 metric을 최적화하면 pressure가 다른 계층으로 이동할 수 있다.

## CHAPTER 30 · Memory pressure는 allocator가 아니라 system-wide control loop다

안정적인 memory policy는 **allocation rate, working-set aging, page-cache value, dirty writeback, swap/compression, cgroup isolation, PSI stall, compaction, OOM recovery**를 하나의 control loop로 본다. 핵심 목표는 free page 숫자를 크게 만드는 것이 아니라 workload가 필요한 working set을 유지하면서 allocation path가 예측 가능한 latency로 progress하도록 하는 것이다.
