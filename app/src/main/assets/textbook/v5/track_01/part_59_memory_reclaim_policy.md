# PART 59 · Memory Reclaim Policy — working set, MGLRU, direct reclaim, memcg, PSI

메모리 부족은 free byte가 0이 되는 순간 시작되지 않는다. Kernel은 watermark 아래로 내려가기 전에 page를 age시키고, clean file cache를 버리고, dirty page를 writeback하고, anonymous page를 swap하거나 compact한다. 어느 page를 회수할지와 얼마나 공격적으로 scan할지가 application tail latency를 결정한다. 이 PART는 **watermark와 working-set 추정에서 MGLRU·memcg·PSI·OOM까지** reclaim을 하나의 control loop로 추적한다.

---

## CHAPTER 01 · reclaim policy는 어떤 page를 버릴 때 미래 비용이 가장 작은지 추정하는 문제다

Physical memory가 유한하므로 새 page를 공급하려면 기존 resident page 일부를 회수해야 한다. Kernel은 clean file page처럼 다시 읽을 수 있는 data, dirty page처럼 먼저 writeback이 필요한 data, anonymous page처럼 swap이 필요한 data의 비용을 다르게 본다. 단순히 “가장 오래된 page” 하나의 규칙으로 끝나지 않는다.

좋은 reclaim은 곧 다시 필요한 hot working set을 보존하고 cold page를 제거해야 한다. 잘못된 선택은 refault를 늘려 storage I/O와 CPU를 반복 소비한다. 따라서 reclaim 효율은 freed bytes뿐 아니라 이후 refault 비용으로 평가해야 한다.

Application 입장에서는 RSS 감소가 항상 좋은 신호가 아니다. 동일 data를 계속 다시 fault한다면 memory는 줄어도 latency와 I/O가 악화될 수 있다.

## CHAPTER 02 · watermark는 allocator가 background reclaim을 시작하고 emergency path로 넘어가는 경계다

Memory zone은 free page가 어느 수준 아래로 내려가면 reclaim을 시작해야 하는지 low/high/min 같은 threshold를 가진다. Background daemon은 여유가 완전히 사라지기 전에 free list를 회복하려 하고, allocator는 필요한 watermark를 만족하지 못하면 직접 reclaim에 참여할 수 있다.

Watermark가 너무 낮으면 allocation burst 때 foreground stall이 커질 수 있고, 너무 높으면 사용 가능한 memory를 과도하게 비워둘 수 있다. Zone fragmentation과 high-order allocation도 단순 free bytes와 다른 pressure를 만든다.

진단에서는 total free만 보지 말고 zone별 watermark와 allocation failure를 확인한다. NUMA node 하나만 pressure인 경우 host 전체 free memory가 충분해 보여도 local reclaim이 발생할 수 있다.

## CHAPTER 03 · kswapd는 foreground allocator가 막히기 전에 background에서 free memory를 회복한다

`kswapd` 계열 thread는 zone/node의 watermark가 낮아지면 wake되어 page를 scan하고 reclaim한다. 충분한 free memory를 확보하면 다시 sleep한다. 이상적인 상태에서는 application allocation이 이 background work를 크게 기다리지 않는다.

Memory pressure가 계속되면 kswapd CPU와 I/O가 증가한다. 그것 자체가 문제라기보다 working set이 capacity에 가까워졌다는 신호일 수 있다. Kswapd가 많은 page를 scan하지만 거의 회수하지 못하면 reclaim efficiency가 나쁜 것이다.

Metrics에서 scanned/reclaimed ratio, kswapd CPU, refault를 함께 본다. 단순 daemon CPU 상승을 죽이거나 priority를 낮추는 방식으로 원인을 숨기지 않는다.

## CHAPTER 04 · direct reclaim은 allocation을 요청한 thread가 memory 회수를 직접 수행하는 latency path다

Background reclaim이 따라가지 못하면 allocator가 호출 thread 안에서 page scan, writeback 대기 같은 작업을 수행할 수 있다. Application stack에서 평범한 allocation이 갑자기 수십 ms 이상 느려지는 이유가 된다. Request tail latency에 직접 노출되는 reclaim 형태다.

Direct reclaim을 많이 겪는다면 free memory 숫자보다 pressure와 reclaim throughput의 균형이 깨졌다는 뜻이다. Memory limit, dirty writeback, swap latency를 함께 조사한다.

Profiler가 CPU stack만 보면 reclaim 중 sleep한 시간이 잘 안 보일 수 있다. Off-CPU trace와 PSI를 사용해 allocation stall을 확인한다.

## CHAPTER 05 · GFP context는 allocation이 어떤 reclaim·sleep을 허용하는지 결정한다

Kernel allocation은 호출 context에 따라 sleep 가능 여부, filesystem I/O 재진입 가능 여부, emergency reserve 사용 여부가 다르다. Interrupt/atomic context에서 blocking reclaim을 시도할 수 없으므로 실패 가능성이 더 높고 reserve를 별도로 관리한다.

Filesystem code가 reclaim 중 다시 filesystem allocation을 일으키면 recursion/deadlock이 생길 수 있어 reclaim flag가 이를 제한한다. 단순 `kmalloc failed`만 보고 총 memory 부족으로 해석하면 원인을 놓친다.

Kernel module/driver는 API가 요구하는 allocation context를 지켜야 한다. Fault injection으로 pressure 상황에서 atomic allocation failure path를 실제 검증한다.

## CHAPTER 06 · clean file page는 원본이 storage에 있어 상대적으로 싸게 버릴 수 있다

Page cache의 clean page는 수정되지 않았으므로 memory에서 제거해도 필요할 때 file에서 다시 읽을 수 있다. Reclaim은 이런 page를 우선 후보로 볼 수 있다. 그러나 hot executable/code/data를 지나치게 버리면 refault I/O가 폭증한다.

Large sequential scan이 cache를 채운 뒤 중요한 hot file page를 밀어내면 application latency가 흔들릴 수 있다. Working-set detection과 readahead 정책이 함께 영향을 준다.

Cache hit ratio만 보지 말고 refault distance와 storage latency를 본다. Clean page는 “무료”가 아니라 재읽기 비용을 가진다.

## CHAPTER 07 · dirty page는 reclaim 전에 writeback이라는 추가 진행 조건이 필요하다

수정된 file page는 그냥 버릴 수 없으므로 storage에 기록해 clean 상태로 만든 뒤 회수해야 한다. Device가 느리거나 congested하면 reclaim이 writeback을 기다리며 memory pressure가 I/O latency로 전환된다.

Dirty page가 많으면 allocator와 writer가 동시에 storage에 pressure를 주어 feedback loop가 생길 수 있다. Dirty throttling은 producer 속도를 writeback capacity에 맞추는 역할을 한다.

Memory incident에서는 dirty bytes, writeback bytes, device queue latency를 같이 본다. RAM만 늘려도 storage가 계속 느리면 backlog가 더 크게 쌓일 수 있다.

## CHAPTER 08 · anonymous page는 swap 또는 discard 가능한 backing이 없으면 reclaim하기 어렵다

Heap/stack 같은 anonymous memory는 원본 file이 없으므로 회수하려면 swap에 기록하거나 process 자체가 page를 해제해야 한다. Swap이 없으면 active anonymous working set이 커질수록 file cache를 더 강하게 밀어내거나 OOM에 빨리 접근할 수 있다.

Swap이 있다고 해서 비용이 사라지는 것은 아니다. Cold anonymous page를 밀어내는 데 유용하지만 다시 필요하면 swap-in latency가 발생한다.

Anon/file 비율과 swap-in/out rate를 함께 본다. Total RSS만으로 reclaim 가능성을 판단하지 않는다.

## CHAPTER 09 · refault는 버린 page가 얼마나 빨리 다시 필요해졌는지를 보여주는 reclaim feedback다

Reclaimed file page가 곧 다시 fault되면 그것은 working set 일부였을 가능성이 높다. Kernel은 eviction과 refault 사이의 activity distance를 이용해 page가 실제 hot했는지 추정할 수 있다. 단순 access bit보다 workload 변화에 적응하는 signal이 된다.

High refault rate는 memory가 working set보다 작거나 reclaim policy가 잘못된 page를 선택하고 있음을 뜻할 수 있다. Sequential scan의 one-time fault와 반복 hot-set refault를 분리한다.

Application p99 spike와 refault를 맞춰 보면 memory pressure가 storage latency로 전달되는 순간을 확인할 수 있다.

## CHAPTER 10 · working set은 가까운 미래에 반복적으로 접근될 page 집합으로 이해해야 한다

Process가 address space 100GB를 가진다고 실제 working set이 100GB인 것은 아니다. 반대로 RSS 10GB가 있어도 그중 대부분을 매초 다시 사용한다면 memory capacity 요구가 높다. Reclaim은 현재 resident 여부보다 재사용 가능성을 추정하려 한다.

Working set이 physical/cgroup memory보다 크면 steady-state thrashing이 발생한다. 어떤 eviction algorithm도 근본적으로 부족한 capacity를 해결할 수 없다.

Capacity test는 dataset size를 단계적으로 늘려 refault와 latency가 급격히 증가하는 지점을 찾는다. 평균 RSS만으로 안전 headroom을 정하지 않는다.

## CHAPTER 11 · LRU는 정확한 시간순 목록이라기보다 recency를 근사하는 policy family다

모든 memory access마다 page를 정확한 LRU list 앞으로 옮기면 lock과 cache overhead가 너무 크다. Kernel은 accessed bit, batching, active/inactive list 같은 기법으로 recency를 근사한다. 이름이 LRU여도 strict total order가 아니다.

근사 방식은 scan resistance와 workload에 영향을 준다. One-pass scan이 hot set을 밀어내지 않도록 active promotion 규칙이 중요하다.

Tuning을 논할 때 `LRU라서 오래된 page부터 버린다` 수준에서 멈추지 말고 실제 kernel 세대의 reclaim implementation을 확인한다.

## CHAPTER 12 · MGLRU는 page를 여러 generation으로 나눠 recency 추정을 더 세밀하게 한다

Multi-Gen LRU는 access된 시기를 generation으로 묶어 오래된 generation부터 reclaim하고, refault feedback으로 workload type을 조정한다. Active/inactive 두 단계보다 시간 축을 더 많이 표현해 mixed workload에서 hot/cold 분리를 개선할 수 있다.

Generation aging 자체에도 page table walk와 bookkeeping 비용이 있다. Workload와 kernel version에 따라 개선 폭이 달라질 수 있다.

MGLRU 활성화 전후에는 throughput뿐 아니라 scanned/reclaimed, refault, PSI, p99를 비교한다. 기능 이름만으로 memory pressure가 해결됐다고 판단하지 않는다.

## CHAPTER 13 · aging은 page 접근 정보를 주기적으로 수집해 reclaim priority를 갱신한다

어떤 page가 최근 사용됐는지 계속 갱신하지 않으면 오래전 hot했던 page가 영원히 보호되거나 새 hot page가 cold로 오인될 수 있다. Aging은 access bit 등을 읽고 generation/list 상태를 바꿔 recency 정보를 갱신한다.

Aging 빈도가 너무 낮으면 workload 변화에 늦게 반응하고, 너무 높으면 page table scan CPU가 늘 수 있다. Large address space에서 비용이 특히 중요하다.

관측에서는 reclaim CPU를 scan과 actual eviction으로 나눠 본다. Metadata maintenance가 병목인지 I/O가 병목인지 구분한다.

## CHAPTER 14 · scan cost는 한 page를 회수하기 위해 얼마나 많은 후보를 검사했는지로 평가한다

Reclaim이 수백만 page를 scan해 몇 개만 free한다면 CPU를 많이 쓰면서 progress가 적다. Pinned page, dirty page, hot page 비율이 높을수록 scan efficiency가 떨어진다. `pages scanned / pages reclaimed`가 중요한 이유다.

Pressure가 심할수록 scan을 공격적으로 늘리는 policy는 forward progress에 필요하지만 application CPU를 빼앗을 수 있다.

Kernel upgrade 후 reclaim CPU가 늘었다면 reclaimed volume뿐 아니라 scan ratio를 비교한다. Free memory가 같은 결과라도 비용이 달라질 수 있다.

## CHAPTER 15 · shrinker는 page cache 밖의 reclaimable kernel object를 회수하는 mechanism이다

Dentry, inode cache나 subsystem-specific object는 일반 LRU page와 다른 구조에 있을 수 있다. Shrinker callback은 memory pressure에서 이런 cache가 얼마나 reclaim 가능한지 보고하고 object를 줄인다. 잘못된 shrinker는 reclaim 중 lock contention이나 긴 latency를 만들 수 있다.

Subsystem cache가 커도 shrinkable하면 당장 위험하지 않을 수 있고, non-reclaimable slab이 증가하면 더 문제다. Slab breakdown을 확인한다.

Custom kernel component는 shrinker가 allocation을 재귀적으로 유발하지 않도록 설계한다. Pressure test에서 실제 회수가 되는지 검증한다.

## CHAPTER 16 · reclaim recursion은 memory를 만들려는 코드가 다시 memory를 요구하며 교착되는 문제다

Filesystem writeback이나 shrinker가 reclaim 중 새로운 allocation을 하면 같은 resource 부족 path에 다시 들어갈 수 있다. Lock을 보유한 채 reclaim이 해당 lock을 필요로 하는 I/O를 호출하면 deadlock도 가능하다.

Allocation context flag와 reserve pool은 이런 recursion을 제한한다. Error path가 “로그를 남기기 위해 allocation”하는 것도 pressure에서 실패할 수 있다.

Fault injection으로 low-memory 상태에서 rare cleanup/IO path를 실행한다. 정상 memory에서의 test로 reclaim recursion을 찾기 어렵다.

## CHAPTER 17 · dirty throttling은 write producer 속도를 storage writeback 능력에 맞춘다

Dirty page가 일정 비율을 넘으면 writer thread가 sleep하거나 writeback에 참여해 더 이상 memory를 무한 buffer로 쓰지 못하게 한다. 이때 application write latency가 device 속도를 직접 반영하기 시작한다.

Threshold가 너무 크면 큰 burst 뒤 긴 stall이 오고, 너무 작으면 평소에도 write throughput이 제한될 수 있다. Device와 memory 크기 비율이 중요하다.

Latency spike 때 dirty limit hit와 writeback bandwidth를 확인한다. Application lock 문제로 오인하지 않는다.

## CHAPTER 18 · writeback congestion은 memory reclaim과 block I/O saturation을 서로 증폭시킬 수 있다

Dirty page를 회수하려면 writeback해야 하지만 block device가 이미 포화되면 completion이 늦어지고 dirty memory가 오래 남는다. Free memory가 줄어 더 많은 direct reclaim이 발생하고, foreground latency가 악화된다.

이 feedback loop에서는 CPU, memory, storage를 따로 튜닝하면 해결이 안 될 수 있다. Write rate admission이나 queue control이 필요하다.

PSI memory와 I/O, dirty bytes, device latency를 같은 그래프에 둔다. 어느 resource가 먼저 악화됐는지 시간 순서를 본다.

## CHAPTER 19 · zswap은 swap-out page를 compressed memory cache에 보관해 느린 backing I/O를 줄인다

Anonymous page를 바로 disk swap에 쓰는 대신 RAM 안에서 압축해 보관하면 compression CPU를 쓰는 대신 I/O와 memory footprint를 줄일 수 있다. 압축이 잘 되는 cold page에서 효과가 크고 incompressible data에서는 이득이 작다.

Zswap pool 자체도 memory를 사용하므로 무한히 커질 수 없다. Pool이 차면 entry를 backing swap으로 writeback하거나 새 page를 거부한다.

Tuning은 compression ratio, CPU, swap I/O, refault latency를 함께 본다. “swap을 RAM에 둔다”는 단순 설명보다 pressure 시 실제 eviction path를 이해한다.

## CHAPTER 20 · memcg reclaim은 host 전체 여유와 상관없이 container 내부에서 발생할 수 있다

Memory cgroup이 limit에 가까우면 host에 free memory가 남아 있어도 해당 cgroup의 page를 reclaim한다. 그래서 container application은 host dashboard가 정상인데 direct reclaim과 OOM을 경험할 수 있다.

Shared file page가 여러 cgroup에 어떻게 charge되는지, kernel memory가 포함되는지 version별 accounting semantics도 중요하다.

Incident에서는 cgroup usage/high/max, reclaim, PSI를 먼저 본다. Host total memory만으로 container pressure를 설명하지 않는다.

## CHAPTER 21 · memcg fairness는 여러 tenant가 같은 physical memory와 reclaim CPU를 공유할 때 중요하다

한 cgroup의 scan-heavy workload가 global reclaim 비용을 키워 다른 tenant latency에 간접 영향을 줄 수 있다. `memory.low/min/high` 같은 protection/throttling 정책은 중요 workload의 working set을 어느 정도 보호하는 데 쓰일 수 있다.

보호를 과도하게 주면 남은 cgroup이 더 강하게 reclaim되어 starvation될 수 있다. Reservation 합계가 physical capacity와 맞는지 확인한다.

Per-cgroup refault와 PSI를 비교해 noisy neighbor를 찾는다. Host average만 보면 누가 pressure 비용을 지불하는지 알 수 없다.

## CHAPTER 22 · PSI는 reclaim 때문에 task가 실제로 progress하지 못한 시간을 사용자 영향과 연결한다

Memory PSI는 page fault, reclaim, compaction 등으로 task가 stall된 시간을 집계해 단순 usage보다 실질 pressure를 보여준다. `some` pressure가 높으면 일부 workload가 지연되고, `full`은 모든 non-idle task가 동시에 memory 때문에 멈추는 더 심각한 상태를 나타낸다.

PSI spike를 admission과 autoscaling 신호로 사용할 수 있지만 너무 짧은 burst에 반응하면 oscillation이 생긴다. Window와 hysteresis가 필요하다.

Latency SLO와 PSI correlation을 측정해 threshold를 정한다. 일반적인 10% 숫자를 복사하지 않는다.

## CHAPTER 23 · reclaim tail stall은 평균 memory throughput보다 사용자 p99를 더 크게 해칠 수 있다

대부분 allocation은 빠르다가 특정 request가 direct reclaim, compaction, swap-in을 만나 수백 배 느려질 수 있다. 평균 CPU와 RSS는 정상처럼 보여도 사용자 tail latency가 흔들린다.

Per-request off-CPU trace나 allocation stall을 수집해 느린 request와 reclaim event를 연결한다. GC pause와 kernel reclaim을 구분하는 것도 중요하다.

Memory headroom을 늘리는 목적은 평균 allocation 속도보다 이런 tail event frequency를 줄이는 데 있을 수 있다.

## CHAPTER 24 · compaction은 큰 contiguous physical page를 만들기 위해 movable page를 이동한다

Free memory 총량이 충분해도 high-order allocation에 필요한 연속 block이 없을 수 있다. Compaction은 page를 이동해 큰 free extent를 만들지만 CPU와 memory bandwidth를 사용하고 foreground stall을 일으킬 수 있다.

THP, hugepage, device allocation이 compaction을 유발할 수 있다. Repeated compaction failure는 fragmentation이 심하다는 신호다.

Metrics에서 compact success/fail, stall time을 본다. Memory shortage와 fragmentation을 같은 문제로 취급하지 않는다.

## CHAPTER 25 · THP는 TLB 효율을 높이지만 compaction·reclaim granularity를 크게 만든다

Transparent Huge Pages는 여러 4 KiB page를 큰 page로 매핑해 TLB miss를 줄일 수 있다. 하지만 hugepage allocation/compaction 비용과 copy-on-write amplification이 생길 수 있다. Workload에 따라 latency가 좋아지거나 나빠질 수 있다.

Memory pressure에서 hugepage split과 reclaim behavior도 성능에 영향을 준다. Database가 자체 hugepage policy를 가진 경우 kernel THP와 겹치지 않게 한다.

Benchmark는 throughput뿐 아니라 page fault/compaction p99를 본다. THP on/off를 단순한 전역 성능 스위치로 보지 않는다.

## CHAPTER 26 · OOM은 reclaim이 충분한 progress를 만들 수 없을 때 마지막 단계로 접근한다

Kernel이 page를 계속 scan해도 allocation을 만족할 수 없으면 OOM handling이 필요하다. 모든 free memory가 0이어야만 발생하는 것은 아니며 allocation order, cgroup limit, unreclaimable memory가 영향을 준다.

OOM 직전에는 system이 이미 심한 reclaim stall에 빠질 수 있어 “OOM은 안 났다”가 healthy하다는 뜻이 아니다. Application watchdog가 먼저 timeout될 수도 있다.

OOM event 전후 PSI와 reclaim trace를 보존한다. Kill 순간만 수집하면 preceding pressure 원인을 잃는다.

## CHAPTER 27 · OOM policy는 어떤 process를 희생해 전체 system progress를 복구할지 결정한다

OOM killer는 memory 사용량과 조정 값을 고려해 victim을 선택한다. Application이 “중요 process는 절대 안 죽는다”고 가정하면 안 된다. Critical control plane과 bulk worker를 resource domain으로 분리하는 편이 더 명확하다.

Cgroup OOM에서는 failure blast radius를 tenant/service 안에 가둘 수 있다. Supervisor restart가 다시 같은 memory peak를 만들지 확인한다.

OOM score 조정은 마지막 보호 장치로 사용하고 근본 capacity/admission 문제를 숨기지 않는다.

## CHAPTER 28 · proactive reclaim은 pressure가 심해지기 전에 cold page를 미리 줄여 headroom을 만든다

Idle/background 시간에 cgroup의 cold memory를 reclaim하면 이후 foreground burst가 direct reclaim을 덜 겪을 수 있다. 하지만 잘못하면 곧 필요한 working set을 미리 버려 refault를 증가시킨다.

Proactive policy는 memory usage target과 observed refault/PSI feedback을 사용해 천천히 조정하는 편이 안전하다. 한번에 큰 reclaim을 강제하지 않는다.

실험은 다음 traffic burst의 p99까지 포함해 평가한다. Reclaim 직후 RSS 감소만 보면 성공처럼 보일 수 있다.

## CHAPTER 29 · reclaim tuning은 단일 sysctl보다 workload의 working-set·storage·latency 관계를 측정해야 한다

Swappiness, dirty ratio, THP, cgroup threshold 같은 knob는 서로 영향을 준다. 한 값을 바꾼 뒤 free memory 하나만 보면 secondary effect를 놓친다. Representative memory pressure test에서 refault, swap, writeback, PSI, p99를 함께 수집한다.

Kernel version 변경으로 기본 reclaim algorithm이 달라질 수 있으므로 과거 tuning 값을 맹목적으로 유지하지 않는다. MGLRU 같은 feature 도입 후 old assumption을 재검증한다.

Tuning record에는 목표와 되돌릴 조건을 적는다. “인터넷에서 권장”이라는 근거로 production memory policy를 고정하지 않는다.

## CHAPTER 30 · memory control loop는 working set을 보호하면서 pressure가 SLO를 넘기기 전에 load를 줄여야 한다

Reclaim은 free page를 만드는 내부 mechanism이지만 서비스 관점의 목표는 application progress를 유지하는 것이다. Page aging과 eviction이 working set을 추정하고, PSI와 refault가 잘못된 reclaim을 신호하며, cgroup high/admission control이 load를 조절하는 폐루프가 필요하다.

Capacity가 working set보다 작으면 algorithm만으로 해결할 수 없다. Memory를 늘리거나 dataset/cache를 줄이거나 concurrency를 제한해야 한다.

최종 검증은 pressure를 실제로 만들어 direct reclaim, refault, PSI, p99, OOM까지 단계별 behavior를 관찰하는 것이다. Free-memory 숫자가 일정하다는 이유만으로 reclaim policy를 PASS라고 부를 수 없다.
