# PART 18 · Garbage Collection — reachability, barriers, concurrent reclamation

자동 메모리 관리는 `안 쓰는 객체를 지운다`는 기능이 아니다. collector는 **root에서 도달 가능한 객체를 정확히 보존하고, mutator가 동시에 reference graph를 바꾸는 동안 살아 있는 객체를 놓치지 않으며, pause·throughput·memory overhead를 제한**해야 한다. 알고리즘마다 이 세 비용의 배치가 다르다.

---

## CHAPTER 01 · reachability는 language-level liveness를 근사하는 안전 기준이다

collector는 프로그램이 미래에 객체를 다시 사용할지를 일반적으로 완벽히 예측할 수 없다. 대신 stack/register/global/JNI handle 같은 root에서 reference graph를 따라 도달 가능한 객체를 live로 간주한다. 도달 불가능한 객체는 프로그램이 합법적인 reference를 통해 다시 접근할 수 없다는 전제에서 reclaim한다.

reachability는 business usefulness와 다르다. cache가 더 이상 필요 없어도 global map이 reference를 유지하면 collector는 살아 있다고 판단한다. GC leak는 collector failure가 아니라 ownership graph가 객체를 계속 reachable하게 만드는 경우가 많다.

---

## CHAPTER 02 · precise root enumeration은 pointer와 non-pointer 값을 구분해야 한다

moving collector는 객체를 이동한 뒤 모든 live reference를 새 주소로 갱신해야 하므로 어느 register/stack slot이 object reference인지 정확히 알아야 한다. compiler/runtime는 stack map, metadata, safepoint를 이용해 precise roots를 제공할 수 있다.

conservative collector는 bit pattern이 pointer처럼 보이면 object를 보존할 수 있어 false retention과 이동 제약이 생긴다. managed runtime의 type metadata와 compiler cooperation이 더 강한 collector 알고리즘을 가능하게 한다.

---

## CHAPTER 03 · tri-color abstraction은 tracing collector의 핵심 invariant를 표현한다

white는 아직 발견되지 않은 객체, gray는 발견됐지만 outgoing edge scan이 끝나지 않은 객체, black은 scan이 끝난 객체로 모델링할 수 있다. tracing은 root를 gray로 만들고 gray object의 child를 발견하며 black으로 전환한다.

정확성의 핵심은 collection 종료 시 live white object가 남지 않는 것이다. concurrent mutator가 black→white edge를 새로 만들면 collector가 그 white object를 놓칠 수 있으므로 write/read barrier가 tri-color invariant를 보존하도록 설계된다.

---

## CHAPTER 04 · mark-sweep은 object 위치를 유지하고 free-space 관리 비용을 낸다

mark phase가 live graph를 표시한 뒤 sweep가 unmarked object 영역을 free list로 반환한다. object를 이동하지 않으므로 raw pointer/pinning과 호환이 쉽지만 heap이 시간이 지나면서 fragmented될 수 있다.

sweep를 heap 전체 선형 scan으로 수행하면 pause와 memory bandwidth 비용이 크다. bitmap marking, segregated region, lazy sweeping으로 비용을 줄일 수 있다. allocator와 collector의 free-list 정책이 함께 fragmentation을 결정한다.

---

## CHAPTER 05 · mark bitmap은 object header와 marking metadata를 분리한다

mark bit를 object header 대신 별도 bitmap에 저장하면 compact metadata access와 page-level locality를 얻을 수 있다. bit address는 object address에서 계산할 수 있어 mark state lookup이 빠르다.

bitmap 자체도 memory와 cache bandwidth를 소비한다. sparse large heap에서는 bitmap scan 전략이 중요하고, concurrent marker가 여러 thread에서 bit를 업데이트하면 atomic word contention을 고려해야 한다.

---

## CHAPTER 06 · lazy sweeping은 reclaim cost를 allocation path로 분산한다

mark가 끝난 뒤 heap 전체를 즉시 sweep하지 않고 allocation이 필요한 region부터 sweep하면 stop-the-world pause를 줄일 수 있다. 대신 future allocation latency가 sweep work에 영향을 받고 free-space accounting이 복잡해진다.

real-time sensitive runtime은 large one-shot pause 대신 bounded incremental work를 선호할 수 있다. GC phase time을 평균 하나로 보지 않고 mark/sweep/assist/allocation stall 분포를 분리한다.

---

## CHAPTER 07 · copying collector는 live object만 복사해 allocation과 compaction을 결합한다

semispace copying은 from-space에서 reachable object를 to-space로 복사하고 forwarding pointer로 중복 copy를 방지한다. collection이 끝나면 to-space가 compact live heap이고 나머지 space를 한 번에 free로 사용할 수 있다.

cost는 heap 전체가 아니라 live bytes에 더 비례하지만 두 semispace 때문에 peak memory overhead가 크다. object 이동 때문에 raw pointer/reference update가 필요하다. young generation처럼 live ratio가 낮은 영역에 특히 적합하다.

---

## CHAPTER 08 · forwarding pointer는 이동 중 identity를 보존한다

object를 처음 복사할 때 old location에 forwarding information을 남기면 다른 reference가 같은 old object를 만났을 때 이미 생성한 new copy를 찾을 수 있다. 이 mechanism이 object identity와 cycle graph를 안전하게 처리한다.

header를 forwarding state에 사용하는 runtime은 hash/lock/header metadata와 충돌을 해결해야 한다. side table이나 tagged header state가 필요할 수 있다. moving GC 설계는 object model과 독립적이지 않다.

---

## CHAPTER 09 · mark-compact는 fragmentation을 제거하면서 live object 이동 cost를 낸다

mark 후 live object를 한쪽으로 밀어 contiguous free region을 만들 수 있다. compaction algorithm은 destination 계산, reference update, object move 순서를 조정해야 한다.

large heap에서 모든 live byte를 이동하면 memory bandwidth와 pause가 커진다. region-selective compaction이나 mostly-concurrent approach는 fragmentation이 심한 영역만 이동해 비용을 제한할 수 있다.

---

## CHAPTER 10 · generational hypothesis는 대부분의 객체가 짧게 산다는 workload 관찰이다

많은 managed workload에서 새 object 상당수가 짧은 시간 안에 unreachable된다. heap을 young/old generation으로 나누고 young을 자주 collect하면 전체 old heap scan을 피할 수 있다.

가설이 workload에 맞지 않으면 promotion과 repeated scan 비용이 커진다. large long-lived graph를 빠르게 만드는 workload, cache warmup은 generational collector의 예상과 다를 수 있다. survivor age distribution을 측정한다.

---

## CHAPTER 11 · minor collection은 old→young reference를 별도 추적해야 한다

young generation만 tracing할 때 old object가 young object를 가리키는 edge를 무시하면 live young object를 잘못 reclaim할 수 있다. remembered set은 이러한 cross-generation reference source를 추적한다.

전체 old heap을 매 minor GC마다 scan하지 않기 위해 write barrier가 old object field에 young reference가 저장될 때 card/remembered metadata를 갱신한다. allocation fast path의 이득은 mutator write barrier cost와 교환된다.

---

## CHAPTER 12 · card table은 address range를 coarse dirty unit으로 추적한다

heap을 고정 크기 card로 나누고 reference write가 발생한 card를 dirty 표시하면 GC는 dirty card만 scan해 old→young edge를 찾을 수 있다. card가 너무 크면 불필요한 scan이 늘고 너무 작으면 metadata와 barrier traffic이 증가한다.

false sharing과 유사하게 unrelated object write가 동일 card를 계속 dirty하게 만들 수 있다. object layout과 mutation pattern이 remembered-set 비용에 영향을 준다.

---

## CHAPTER 13 · write barrier는 collector invariant를 mutator operation에 삽입한다

compiler/runtime는 reference field store 주변에 작은 barrier code를 추가할 수 있다. barrier는 generational remembered set, concurrent marking color invariant, snapshot consistency를 유지한다.

barrier는 모든 pointer write hot path에 들어갈 수 있어 instruction count와 cache cost가 중요하다. collector의 pause를 줄였지만 application throughput이 barrier 때문에 낮아질 수 있다. GC 비교는 pause만이 아니라 mutator tax를 포함한다.

---

## CHAPTER 14 · SATB는 marking 시작 시점의 graph snapshot을 보존한다

Snapshot-At-The-Beginning 계열 concurrent marking은 marking 시작 순간 live였던 object가 collection 중 edge 삭제로 사라지지 않게 old reference를 barrier를 통해 기록할 수 있다. mutator가 field를 overwrite할 때 previous value를 marking worklist에 보존한다.

SATB는 collection 동안 새로 allocation된 object 처리와 remark phase가 필요하다. snapshot semantics 덕분에 floating garbage, 즉 collection 중 죽었지만 이번 cycle에는 살아남는 객체가 있을 수 있다. correctness와 prompt reclamation은 다른 목표다.

---

## CHAPTER 15 · incremental-update barrier는 black→white edge 생성 자체를 추적한다

다른 concurrent marking strategy는 black object가 white object를 새로 가리킬 때 target을 gray로 만드는 방식으로 tri-color invariant를 유지한다. insertion barrier 중심의 접근이다.

SATB와 incremental-update는 barrier timing과 floating garbage 특성이 다르다. runtime이 어떤 barrier를 사용하는지 알아야 profiling에서 reference-update cost를 해석할 수 있다. collector 명칭만으로 내부 barrier를 추정하지 않는다.

---

## CHAPTER 16 · concurrent marking은 pause를 줄이지만 CPU와 memory-bandwidth 경쟁을 만든다

collector thread가 application과 동시에 heap graph를 scan하면 stop-the-world 시간을 줄일 수 있지만 동일 CPU core/cache/memory bandwidth를 경쟁한다. CPU가 이미 포화된 server에서는 concurrent GC가 request throughput과 tail latency를 악화시킬 수 있다.

collector thread priority와 CPU quota, cgroup limit을 포함해 측정한다. `pause가 짧다`와 `user request가 빠르다`는 동일 지표가 아니다. concurrent phase duration과 mutator utilization을 함께 본다.

---

## CHAPTER 17 · safepoint는 runtime이 thread state를 정확히 관찰할 수 있는 협력 지점이다

moving/root-scanning GC는 thread register/stack reference 위치를 정확히 알아야 한다. compiler가 stack map을 제공하는 safepoint에서 thread를 정지시키면 precise root scan과 object move가 가능하다.

thread가 safepoint에 도달하기 어려운 long native loop나 uninterruptible region은 time-to-safepoint를 늘려 GC pause보다 더 큰 stop latency를 만들 수 있다. pause metric을 `request stop → all threads stopped → GC work → resume`로 분해한다.

---

## CHAPTER 18 · read barrier는 object load 시 forwarding/mark state를 확인하게 할 수 있다

concurrent moving collector는 mutator가 old location reference를 읽을 수 있으므로 read barrier가 forwarding pointer를 따라가거나 object color/state를 조정할 수 있다. read barrier는 pointer read hot path에 들어가므로 매우 낮은 overhead가 요구된다.

hardware feature나 pointer coloring/tagging을 활용해 barrier를 최적화할 수 있다. collector의 낮은 pause가 runtime 전체 load 비용으로 전가되는지 benchmark한다.

---

## CHAPTER 19 · parallel GC는 collection phase 자체를 여러 worker로 나눈다

stop-the-world 상태에서도 mark/copy/sweep work를 여러 GC worker가 병렬 수행하면 pause duration을 줄일 수 있다. work queue와 stealing이 object graph imbalance를 분산한다.

worker 수를 core 수까지 늘린다고 선형 speedup이 나오지 않는다. shared mark bitmap, memory bandwidth, worklist contention이 limit가 된다. foreground application이 멈춘 STW phase와 concurrent parallel phase의 CPU policy를 구분한다.

---

## CHAPTER 20 · weak reference는 ordinary reachability 외의 liveness policy를 추가한다

weak reference는 object가 strong root graph에서 더 이상 도달되지 않을 때 collector가 reference를 clear하고 object를 reclaim할 수 있게 한다. cache와 metadata에 유용하지만 semantics가 collector cycle timing에 의존할 수 있다.

weak reference를 critical resource lifecycle로 사용하지 않는다. object가 언제 collect될지 예측할 수 없으므로 file/socket close는 explicit ownership으로 처리한다. weak map도 key/value reference 관계를 정확히 이해해야 leak를 피할 수 있다.

---

## CHAPTER 21 · finalizer는 reclamation을 비결정적으로 지연시키고 resurrection을 허용할 수 있다

finalization queue가 object를 추가 cycle 동안 reachable하게 만들거나 finalizer가 object를 다시 global reference에 저장해 resurrection을 만들 수 있다. collector와 object lifecycle reasoning이 복잡해진다.

modern runtime은 finalizer 사용을 제한/비추천하는 방향이 많다. external resource는 explicit close와 structured ownership을 사용하고 cleanup fallback은 leak safety net 정도로만 본다. finalizer backlog를 monitoring한다.

---

## CHAPTER 22 · ephemeron은 key reachability가 value의 liveness를 조건부로 결정한다

weak-key map에서 value가 key를 강하게 가리키면 단순 weak reference 규칙으로는 key가 영원히 살아남을 수 있다. ephemeron semantics는 key가 다른 경로로 live일 때만 value edge를 tracing에 반영한다.

collector는 ephemeron을 fixpoint까지 반복 처리해야 할 수 있다. 언어 runtime의 weak map semantics를 모르면 cache leak를 잘못 해석할 수 있다.

---

## CHAPTER 23 · object pinning은 moving collector의 compaction 자유를 제한한다

native code/DMA가 object address를 일정 기간 고정해야 하면 collector가 해당 object를 이동하지 못한다. pinned object가 많으면 compaction이 불완전해지고 fragmentation이 남을 수 있다.

JNI critical section과 pinning API의 lifetime을 짧게 유지한다. long pin이 GC pause와 heap growth에 미치는 영향을 profile한다. native interaction이 managed heap layout까지 영향을 준다.

---

## CHAPTER 24 · large object space는 큰 객체의 이동 비용을 별도 처리한다

수 MB array/image를 young copying collector에서 매번 옮기면 bandwidth와 pause가 크다. runtime은 일정 threshold 이상 object를 별도 large-object space에 allocate해 non-moving 또는 region 단위로 관리할 수 있다.

large object churn은 일반 allocation count가 적어도 heap fragmentation과 GC pressure를 만들 수 있다. object size histogram과 lifetime을 함께 본다. buffer pooling은 peak memory와 stale retention trade-off가 있다.

---

## CHAPTER 25 · region-based collector는 heap을 독립 회수 단위로 나눈다

heap을 fixed-size region으로 나누고 live density, remembered set, age 같은 정보를 region별로 관리하면 garbage 비율이 높은 region부터 evacuation할 수 있다. 전체 heap compaction보다 pause budget을 제어하기 쉽다.

region selection은 copy cost와 reclaimed bytes를 최적화한다. remembered-set metadata가 커지거나 cross-region pointer가 많으면 overhead가 증가한다. object graph topology가 collector efficiency에 직접 영향을 준다.

---

## CHAPTER 26 · fragmentation은 collector와 allocator의 공동 결과다

non-moving old generation은 free hole을 남기고 moving collector는 compaction으로 이를 줄일 수 있다. 하지만 pinned/large object와 region constraint가 있으면 완전 compaction이 불가능하다.

heap free bytes만 보지 않고 largest contiguous free region과 region live density를 본다. allocation failure 때문에 heap을 확장하기 전에 compaction 가능성과 pinned object를 진단한다.

---

## CHAPTER 27 · GC scheduling은 heap occupancy와 allocation rate를 기반으로 앞당겨져야 한다

heap이 거의 가득 찬 뒤 collection을 시작하면 concurrent marker가 끝나기 전에 allocation headroom이 사라져 emergency STW collection이 필요할 수 있다. runtime은 allocation rate와 estimated GC throughput을 이용해 cycle 시작 시점을 조절할 수 있다.

burst allocation은 predictor를 깨뜨릴 수 있다. GC start threshold, growth limit, concurrent worker를 tuning할 때 tail allocation pattern을 포함한다. 단순 heap size 증가는 collection 빈도를 줄이지만 scan/live-set 비용과 RSS를 늘린다.

---

## CHAPTER 28 · GC log는 pause reason, freed bytes, live set, phase time을 연결해야 한다

`GC 50ms` 하나로는 원인이 부족하다. young/full/concurrent/compaction 여부, before/after heap, freed bytes, allocated since previous cycle, time-to-safepoint를 기록한다.

request latency spike와 GC event를 시간축에 겹쳐 correlation을 확인하되 동시 발생을 인과로 단정하지 않는다. GC가 CPU pressure의 결과인지 원인인지 allocation profile과 collector CPU를 함께 본다.

---

## CHAPTER 29 · GC leak 진단은 root path와 dominator를 찾는 작업이다

heap이 cycle마다 감소하지 않는다면 live object가 누적되는지 확인한다. dominator tree와 retained size를 사용해 큰 subgraph를 붙잡는 root를 찾고 reference chain이 business ownership과 일치하는지 검토한다.

cache capacity 없음, listener unregister 누락, coroutine/task reference, classloader static이 흔한 retention source다. object가 GC 알고리즘 때문에 못 지워진다고 가정하기 전에 reachability path를 증명한다.

---

## CHAPTER 30 · collector 선택은 pause, throughput, footprint, barrier cost의 다목적 최적화다

batch workload는 throughput을 위해 longer pause를 허용할 수 있고 interactive mobile app은 frame deadline 때문에 짧은 pause를 우선할 수 있다. large heap server는 concurrent CPU tax를 감수해 tail latency를 낮출 수 있다.

collector tuning은 benchmark 한 개로 끝내지 않는다. production allocation/lifetime graph, CPU quota, heap headroom을 재현하고 **mutator throughput, p99 pause, RSS, CPU/energy**를 동시에 측정한다. GC engineering의 목표는 memory를 많이 회수하는 것이 아니라 application의 lifetime·latency contract를 예측 가능하게 유지하는 것이다.