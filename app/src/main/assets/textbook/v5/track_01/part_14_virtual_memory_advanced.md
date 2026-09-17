# PART 14 · virtual memory 심화 — translation, reclaim, NUMA, OOM

가상 메모리 성능은 `주소를 물리주소로 바꾼다`는 설명보다 훨씬 넓다. page-table footprint, TLB reach, page-fault path, reclaim pressure, NUMA placement, shootdown cost가 실제 latency와 capacity를 결정한다. PART 03의 기본 모델 위에서 운영 메커니즘만 더 깊게 다룬다.

---

## CHAPTER 01 · virtual address는 page offset을 보존하며 translation domain을 나눈다

가상주소는 page size에 따라 page number와 offset으로 해석된다. translation은 virtual page number를 physical frame 또는 특수 mapping state로 바꾸고 page 내부 offset은 유지한다. 이 구조 때문에 protection, sharing, COW, file backing을 page 단위로 관리할 수 있다.

page size는 단순 구현 숫자가 아니다. page가 작으면 mapping granularity가 세밀하지만 page-table entry와 TLB pressure가 커지고, page가 크면 translation overhead를 줄일 수 있지만 fragmentation과 copy/reclaim 단위가 커진다. workload의 working-set geometry가 page-size 효과를 결정한다.

---

## CHAPTER 02 · multi-level page table은 sparse address space의 metadata 비용을 줄인다

64-bit virtual address space 전체에 flat page table을 미리 두는 것은 비효율적이다. multi-level table은 상위 index가 존재하는 하위 region에만 table page를 할당해 sparse mapping을 지원한다.

대신 TLB miss 시 page-table walk가 여러 memory read를 필요로 한다. 일부 architecture는 hardware walker와 page-walk cache로 비용을 줄인다. page-table page 자체도 memory를 소비하고 cache를 사용하므로 huge process 수나 fragmented mapping은 metadata footprint를 키울 수 있다.

---

## CHAPTER 03 · page walk도 cache hierarchy와 memory latency의 영향을 받는다

TLB miss가 발생하면 page-table hierarchy를 따라 leaf PTE를 찾는다. 각 level access가 cache hit인지 DRAM miss인지에 따라 translation latency가 크게 달라진다. page-table entry access가 data cache를 오염시킬 수도 있다.

성능 counter에서 TLB miss 수만 보지 않고 page-walk duration/cycle과 walk completion을 본다. working set이 TLB capacity를 넘더라도 page-table entry가 cache에 잘 남아 있으면 비용이 상대적으로 작을 수 있고, 반대의 경우 translation이 주요 stall이 된다.

---

## CHAPTER 04 · TLB reach는 entry 수 × page size로 결정되는 practical working-set bound다

TLB는 최근 virtual→physical translation을 저장한다. 동일 entry 수에서 larger page를 사용하면 한 번에 cover하는 memory 범위가 커져 TLB reach가 늘어난다. 대규모 sequential scan이나 random large working set에서는 TLB miss가 data-cache miss와 별개 bottleneck이 될 수 있다.

TLB는 instruction/data, page-size class, privilege level별로 구조가 다를 수 있으므로 단일 `TLB size` 숫자로 판단하지 않는다. benchmark는 target CPU의 actual counter와 page size를 확인한다. context switch와 address-space identifier도 effective hit rate에 영향을 준다.

---

## CHAPTER 05 · huge page는 TLB pressure를 줄이는 대신 allocation·fragmentation 비용을 바꾼다

huge page는 더 큰 page size로 적은 TLB entry가 넓은 address range를 cover하게 한다. large in-memory DB, JVM heap, HPC workload에서 translation overhead를 줄일 수 있다.

그러나 큰 contiguous physical region이 필요하고 작은 object가 sparse하게 사용되면 internal fragmentation이 커질 수 있다. transparent huge page는 promotion/compaction latency를 만들 수 있으므로 tail-sensitive service에서 항상 이득이 아니다. allocation stall과 TLB miss를 함께 비교한다.

---

## CHAPTER 06 · page-table memory도 process memory budget에 포함된다

수백 GB virtual mapping을 4KiB page 단위로 촘촘히 사용하면 page-table entry와 intermediate table page가 상당한 memory를 소비한다. RSS만 보고 application data 크기만 추정하면 kernel-side page-table overhead를 놓칠 수 있다.

많은 process가 동일 shared library를 mapping해도 각 process는 자신의 translation metadata를 필요로 할 수 있다. container/process density planning에서 page table, kernel object, stack 같은 per-process overhead를 capacity model에 포함한다.

---

## CHAPTER 07 · PTE state는 present/nonpresent 이상의 정보를 담는다

page-table entry는 frame address뿐 아니라 read/write/execute permission, dirty/accessed 상태, user/kernel access, architecture-specific attribute를 포함할 수 있다. OS는 이 bit를 page replacement, COW, write tracking, protection에 활용한다.

`page가 mapped되었다`와 `write 가능한 page다`는 다른 조건이다. mprotect 같은 operation은 PTE protection을 바꾸고 여러 core의 TLB entry를 invalidate해야 할 수 있다. permission 변경도 global synchronization cost를 가질 수 있다.

---

## CHAPTER 08 · page fault reason을 demand allocation과 protection violation으로 분리한다

not-present fault는 anonymous first touch, swapped page, file-backed page miss처럼 정상 lazy population일 수 있다. protection fault는 write-protected COW, read-only violation, execute-never violation처럼 access type과 permission이 맞지 않을 때 발생한다.

fault handler가 해결 가능한 경우 PTE를 갱신하고 instruction을 재시도한다. 해결 불가능하면 signal/exception으로 process에 전달한다. crash report에서는 fault address, access type, mapping table, signal code를 함께 봐야 null/unmapped/protection 문제를 구분할 수 있다.

---

## CHAPTER 09 · major/minor fault는 storage I/O 포함 여부와 비용 차이를 드러낸다

minor fault는 page가 memory 어딘가에 이미 존재하거나 zero-fill/COW처럼 storage read 없이 처리 가능한 경우가 많고, major fault는 backing storage I/O가 필요할 수 있다. 정확한 accounting 정의는 OS를 따른다.

latency-sensitive application에서 major fault가 startup/first-use critical path에 집중되면 preload/read-ahead가 도움이 될 수 있다. 그러나 fault count만 줄이려고 모든 data를 prefault하면 startup memory와 I/O가 커진다. fault 위치와 reuse probability를 함께 본다.

---

## CHAPTER 10 · COW cost는 page copy뿐 아니라 accounting와 TLB invalidation을 포함한다

write-protected shared page에 첫 write가 발생하면 kernel은 private page를 할당하고 content를 copy하며 PTE를 갱신한다. 이때 memory allocation, copy bandwidth, page-table update가 모두 비용이다.

fork 후 많은 page를 수정하거나 Zygote child가 preload page를 광범위하게 dirty하면 COW burst가 발생한다. `shared RSS`가 높은 startup 상태가 steady state에서도 유지된다고 가정하지 않는다. private dirty growth를 time-series로 본다.

---

## CHAPTER 11 · mmap은 virtual address reservation과 backing policy를 결합한다

anonymous/file-backed, shared/private mapping은 fault와 writeback semantics가 다르다. MAP_PRIVATE file mapping의 write는 COW private page를 만들 수 있고 file에 반영되지 않는다. shared mapping은 dirty page가 backing file과 연결될 수 있다.

mapping size와 address-space reservation이 크다고 physical memory가 즉시 소비되는 것은 아니다. 실제 resident growth는 fault/access pattern에 달려 있다. huge mapping을 memory leak으로 오판하지 않도록 VSS/RSS/PSS를 구분한다.

---

## CHAPTER 12 · anonymous memory는 file backing이 없어 reclaim 선택지가 다르다

heap/stack과 같은 anonymous page는 clean file page처럼 원본 file에서 다시 읽어올 수 없다. reclaim하려면 swap/compressed memory에 쓰거나 process를 kill해야 할 수 있다.

따라서 동일 RSS라도 file-backed cache와 anonymous dirty page는 system pressure에 미치는 영향이 다르다. memory capacity planning에서 reclaimability를 포함한다. cache memory는 높아 보여도 빠르게 회수 가능한 반면 pinned anonymous page는 실제 hard pressure를 만든다.

---

## CHAPTER 13 · reclaim은 cold page를 찾고 memory를 재사용 가능한 상태로 만든다

kernel은 accessed/working-set 정보를 이용해 reclaim candidate를 선택한다. clean file-backed page는 버리고 필요 시 다시 읽을 수 있지만 dirty page는 writeback이 필요하다. anonymous page는 swap policy의 영향을 받는다.

reclaim algorithm의 목표는 단순 free-memory 최대화가 아니다. 최근/자주 쓰는 working set을 유지해 fault 재발을 줄여야 한다. 잘못된 eviction은 page를 버린 직후 다시 fault하는 churn을 만든다.

---

## CHAPTER 14 · thrashing은 useful work보다 page movement에 시간을 쓰는 상태다

working set이 available memory보다 크거나 여러 workload가 서로 hot page를 밀어내면 fault→reclaim→refault cycle이 반복된다. CPU utilization은 낮을 수 있는데 storage/compression/reclaim CPU가 높아지고 latency가 폭증한다.

해결은 swap 크기만 늘리는 것이 아니다. workload working set 축소, concurrency/admission 제한, memory 증설, locality 개선이 필요하다. refault rate와 reclaim scan, major fault, swap traffic을 함께 본다.

---

## CHAPTER 15 · swap과 zRAM은 memory pressure를 다른 자원으로 이동시킨다

swap은 anonymous page를 storage로 이동해 DRAM을 확보하고 zRAM은 compressed RAM에 page를 저장해 capacity를 늘리는 대신 CPU와 compressed-pool memory를 사용한다. mobile platform에서는 storage latency와 flash wear 때문에 zRAM이 중요한 역할을 할 수 있다.

압축 ratio가 낮거나 CPU가 포화되면 zRAM benefit이 줄 수 있다. swap-in latency는 user interaction에 직접 영향을 줄 수 있다. memory pressure policy는 capacity, CPU, I/O, energy를 동시에 본다.

---

## CHAPTER 16 · overcommit은 virtual allocation promise와 실제 physical backing을 분리한다

OS는 process가 요청한 virtual allocation 총합이 즉시 physical memory+swap보다 크더라도 허용할 수 있다. 많은 allocation이 실제 touch되지 않는 workload에서 효율적이지만 peak usage가 겹치면 backing 부족이 뒤늦게 드러난다.

`malloc 성공`을 memory availability 보장으로 해석하면 안 된다. critical workload는 preallocation/touch, cgroup reservation, bounded queue로 peak demand를 통제한다. overcommit policy와 runtime allocator behavior를 같이 이해한다.

---

## CHAPTER 17 · OOM은 allocation failure와 victim selection policy의 결과다

system/cgroup이 더 이상 필요한 page를 확보하지 못하면 allocation 실패 또는 OOM victim termination이 발생할 수 있다. victim 선택은 memory usage와 protection/importance policy의 영향을 받을 수 있다.

Android에서는 process importance와 low-memory management가 application lifetime에 영향을 준다. OOM incident에서 마지막 allocation stack만 보지 않고 전체 system memory pressure, cgroup limit, reclaim failure, victim score를 본다. leak가 아닌 load spike도 원인이 될 수 있다.

---

## CHAPTER 18 · memory pressure는 latency를 OOM 훨씬 전에 악화시킨다

free memory가 줄어들수록 reclaim, compaction, writeback, swap activity가 늘어 application allocation과 page fault latency가 길어질 수 있다. OOM은 마지막 단계일 뿐 user-visible slowdown은 훨씬 먼저 시작된다.

capacity alert를 OOM count에만 두지 않는다. reclaim scan, allocation stall, PSI-like pressure signal, major fault, swap-in/out, GC pressure를 사용해 headroom이 줄어드는 시점을 감지한다.

---

## CHAPTER 19 · physical fragmentation은 충분한 free bytes와 contiguous allocation 가능성을 분리한다

free page가 많아도 고-order contiguous block이 부족할 수 있다. huge page, DMA buffer처럼 연속 물리 메모리가 필요한 allocation은 total free memory가 남아 있어도 실패하거나 compaction을 유발할 수 있다.

buddy allocator order distribution과 compaction event를 보면 fragmentation을 진단할 수 있다. 장시간 uptime/driver pinning이 fragmentation을 악화시킬 수 있다. 단순 reboot가 증상을 없애도 root cause를 설명하지 못한다.

---

## CHAPTER 20 · pinned page는 reclaim과 migration을 막는다

DMA, direct I/O, user-page pinning 같은 mechanism은 page를 특정 physical frame에 고정해 reclaim/migration을 제한할 수 있다. long-term pin이 많으면 memory management flexibility가 줄고 NUMA balancing/compaction이 어려워질 수 있다.

buffer lifecycle을 operation lifetime보다 길게 pin하지 않는다. device driver와 native library에서 pin count/leak를 관찰한다. `memory free가 충분한데 huge-page allocation이 계속 실패`하는 경우 pinned page 분포를 조사한다.

---

## CHAPTER 21 · NUMA에서는 physical memory의 위치가 CPU access latency를 바꾼다

multi-socket/server architecture에서는 memory가 NUMA node별로 연결되어 local node access가 remote node보다 빠를 수 있다. thread가 어느 CPU에서 실행되는지와 page가 어느 node에 배치됐는지가 함께 성능을 결정한다.

전체 memory bandwidth가 충분해도 특정 node가 포화될 수 있다. NUMA-aware allocator, thread affinity, interleave policy를 workload에 맞춰 사용한다. application-level sharding과 hardware locality를 맞추면 remote traffic을 줄일 수 있다.

---

## CHAPTER 22 · first-touch policy는 초기화 thread가 page placement를 결정하게 할 수 있다

일부 NUMA OS policy에서는 virtual allocation만으로 physical node가 결정되지 않고 첫 실제 write가 발생한 CPU node에 page가 배치될 수 있다. single thread가 전체 large array를 초기화하면 이후 worker가 여러 node에 퍼져도 memory는 한 node에 몰릴 수 있다.

parallel initialization으로 worker별 local page를 touch하거나 explicit NUMA policy를 사용한다. benchmark에서는 allocation code보다 initialization pattern이 locality를 결정할 수 있다는 점을 기록한다.

---

## CHAPTER 23 · automatic page migration은 locality를 개선하지만 자체 비용을 가진다

OS는 thread access pattern을 관찰해 hot page를 가까운 NUMA node로 migrate할 수 있다. migration에는 page copy와 PTE/TLB update가 필요하므로 workload가 빠르게 이동하면 이득보다 overhead가 커질 수 있다.

thread migration과 page migration이 서로 쫓아다니는 oscillation을 피한다. NUMA balancing metric과 scheduler trace를 함께 보고 stable workload에는 explicit placement가 더 예측 가능한지 평가한다.

---

## CHAPTER 24 · TLB shootdown은 mapping change를 다른 core에 동기화한다

한 core가 page table entry를 변경했을 때 다른 core의 TLB에 stale translation이 남아 있으면 protection/physical mapping이 불일치한다. OS는 inter-processor interrupt 등으로 관련 core의 TLB entry를 invalidate해야 한다.

mprotect, munmap, frequent page migration, allocator의 aggressive mapping churn은 shootdown traffic을 늘릴 수 있다. multi-core scaling이 이상하게 떨어질 때 syscall count와 TLB invalidation event를 본다. memory mapping도 global coordination point가 될 수 있다.

---

## CHAPTER 25 · 많은 작은 mmap/munmap은 VMA와 page-table metadata를 늘린다

mapping 하나마다 virtual-memory area metadata와 tree/list 관리 비용이 생긴다. JIT, allocator, database가 작은 mapping을 매우 많이 만들면 lookup, lock, page-table update overhead가 커질 수 있다.

mapping count limit과 `/proc` style map inspection으로 fragmentation을 본다. large arena를 내부 allocator로 나누는 방식과 per-object mmap 방식의 trade-off를 workload에서 측정한다.

---

## CHAPTER 26 · internal/external fragmentation을 virtual/physical 관점에서 분리한다

page 내부에 사용되지 않는 공간은 internal fragmentation이고, free physical page가 작은 조각으로 흩어져 large contiguous allocation을 못 만족하는 것은 external fragmentation 성격을 가진다. allocator-level size-class waste와 physical-page fragmentation은 다른 계층이다.

`메모리 낭비` 한 metric으로 합치지 않는다. object requested/allocated bytes, page resident bytes, buddy order availability를 각각 측정한다. 해결책도 object layout, page size, compaction으로 달라진다.

---

## CHAPTER 27 · W^X는 writable과 executable permission을 동시에 주지 않는 원칙이다

JIT runtime은 code를 생성해야 하므로 memory를 writable하게 만들었다가 executable로 전환하는 단계가 필요하다. 동일 page가 장기간 write+execute 가능하면 memory corruption이 arbitrary code execution으로 이어지기 쉬워진다.

permission 전환에는 mprotect와 TLB synchronization 비용이 생길 수 있다. JIT code cache는 batch compilation과 dual mapping 같은 platform mechanism을 사용할 수 있다. security policy를 이유로 W^X를 해제하지 않는다.

---

## CHAPTER 28 · guard page는 접근 불가능한 page로 overflow boundary를 즉시 fault하게 한다

thread stack이나 critical buffer 주변에 unmapped/protected guard page를 두면 overflow가 인접 valid memory를 조용히 덮기 전에 fault를 발생시킬 수 있다. stack growth policy와 guard size는 runtime/OS에 따라 다르다.

large stack local과 deep recursion은 guard fault로 드러날 수 있다. crash address가 stack mapping 경계 근처인지 maps와 함께 확인한다. guard page는 bounds checking을 대체하지 않지만 corruption detectability를 높인다.

---

## CHAPTER 29 · sanitizer는 shadow metadata로 illegal memory access를 탐지한다

AddressSanitizer 계열은 application memory 일부에 대응하는 shadow metadata와 redzone을 이용해 out-of-bounds/use-after-free를 detect할 수 있다. instrumentation은 memory/time overhead를 추가하고 allocation layout을 바꾼다.

sanitizer에서 bug가 재현되지 않는다고 안전하다고 결론내리지 않는다. 여러 sanitizer와 fuzzing을 조합하고 production-like optimized build에서도 regression test를 유지한다. report는 exact binary/symbol과 연결한다.

---

## CHAPTER 30 · Android memory metric은 RSS, PSS, managed/native heap을 구분한다

shared page가 많은 Android process는 RSS 합계가 system physical consumption을 과대평가할 수 있다. PSS는 shared page cost를 process들에 비례 배분해 비교에 유용하지만 순간 sampling이며 accounting rule을 이해해야 한다.

ART heap, native heap, graphics, code, stack, mapped file을 분리해 증가 원인을 본다. process total 하나만으로 `Java leak`을 결론내리지 않는다. system trace와 heap dump, meminfo를 같은 시점에 수집한다.

---

## CHAPTER 31 · memory incident는 capacity와 latency timeline을 함께 만든다

incident 전후 RSS/PSS, allocation rate, GC, reclaim, swap/zRAM, major fault, cgroup limit, OOM event를 같은 시간축에 놓는다. traffic/concurrency 변화도 함께 기록한다.

OOM victim이 마지막으로 memory를 늘린 process라는 보장은 없다. 다른 workload가 reclaimable memory를 소모해 critical process를 희생시킬 수 있다. system-wide pressure source와 victim을 구분한다.

---

## CHAPTER 32 · virtual-memory tuning은 metric 하나가 아니라 workload invariant를 기준으로 한다

huge page, swappiness, heap size, allocator arena 수를 임의로 조정하지 않는다. 어떤 bottleneck을 줄이려는지 먼저 정의하고 translation, reclaim, RSS, tail latency 중 기대 변화와 부작용을 적는다.

변경 후 동일 workload에서 TLB miss, fault/refault, reclaim stall, memory footprint, p99 latency를 함께 재측정한다. virtual memory tuning의 목적은 free-memory 숫자를 예쁘게 만드는 것이 아니라 **working set을 필요한 시간 안에 안정적으로 접근 가능하게 유지하는 것**이다.