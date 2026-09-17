# PART 18 · Garbage Collection — reachability, barriers, concurrent reclamation

자동 메모리 관리는 `안 쓰는 객체를 지운다`는 기능이 아니다. collector는 **root에서 도달 가능한 객체를 정확히 보존하고, mutator가 동시에 reference graph를 바꾸는 동안 살아 있는 객체를 놓치지 않으며, pause·throughput·memory overhead를 제한**해야 한다. 알고리즘마다 이 세 비용의 배치가 다르다.

---

## CHAPTER 01 · reachability는 future use를 직접 예측하지 않고 안전한 liveness 근사를 만든다

collector는 프로그램이 미래에 어떤 객체를 다시 사용할지 일반적으로 알 수 없으므로 stack, register, global, runtime handle 같은 root에서 reference graph를 따라 도달 가능한 객체를 live로 간주한다. 도달 가능하다는 사실은 업무적으로 필요하다는 뜻이 아니라 **합법적인 reference를 통해 다시 접근할 가능성이 남아 있다**는 뜻이다. 따라서 cache가 실질적으로 쓸모없어도 global map이 reference를 유지하면 GC는 회수할 수 없다.

이 모델은 memory leak의 정의를 바꾼다. native allocator leak처럼 release를 잊은 경우도 있지만 managed heap에서는 ownership graph가 객체를 reachable 상태로 유지하는 경우가 흔하다. listener, static registry, long-lived coroutine scope가 short-lived 화면이나 request 객체를 붙잡으면 collector는 정상 동작하면서도 heap이 증가한다.

조사에서는 큰 객체 자체보다 GC root로 이어지는 retained path를 본다. dominator와 retained size, root category, object age를 연결해야 `왜 아직 reachable한가`를 설명할 수 있다. 단순 heap size 증가는 reachability 원인을 말해 주지 않는다.

---

## CHAPTER 02 · root enumeration은 collection 시점에 runtime이 반드시 놓치지 말아야 할 reference 집합을 찾는다

stack slot과 register의 모든 bit pattern을 pointer로 볼 수는 없다. precise GC는 stack map, type metadata, runtime frame 정보를 사용해 어느 위치가 reference인지 식별한다. native frame, JIT frame, interpreter frame, JNI handle이 함께 존재하면 root enumeration은 runtime 전체의 calling convention과 safepoint metadata에 의존한다.

root 하나를 놓치면 실제 live object가 unreachable하다고 오판되어 use-after-free에 해당하는 심각한 corruption이 생길 수 있다. 반대로 integer를 pointer로 잘못 해석하는 conservative scheme은 garbage를 live로 남겨 memory retention을 만든다. correctness와 precision은 서로 다른 문제다.

GC pause가 길 때 heap scan만 원인으로 보지 않는다. thread가 safepoint에 도달하고 stack을 안정화하는 시간, native transition, root 수가 비용에 포함된다. root enumeration latency를 독립적으로 측정해야 한다.

---

## CHAPTER 03 · tri-color abstraction은 concurrent marking에서 놓치면 안 되는 invariant를 표현한다

tri-color marking은 object를 아직 보지 않은 white, 발견했지만 outgoing edge를 모두 처리하지 않은 gray, 처리 완료한 black으로 설명한다. 핵심 안전 조건은 collector가 종료할 때 **black object에서 white live object로 가는 edge가 몰래 생겨 있지 않아야 한다**는 것이다. mutator가 reference graph를 바꾸면 이 조건이 깨질 수 있다.

그래서 concurrent collector는 write barrier, SATB, incremental-update 같은 mechanism으로 mutator modification을 기록한다. 알고리즘 이름보다 어떤 invariant를 유지하는지 이해하면 barrier 동작을 해석하기 쉽다. barrier가 모든 store를 느리게 만드는 이유가 아니라 특정 marking 정보를 보존하기 위한 비용이라는 뜻이다.

디버깅에서는 marking bug를 random GC crash로만 보지 않는다. barrier가 빠지는 write path, native field update, JIT optimization이 invariant를 위반하는지 확인한다. collector correctness는 graph invariant와 implementation path가 1:1로 대응해야 한다.

---

## CHAPTER 04 · mark-sweep는 object 이동 없이 live를 표시하고 unreachable block을 free로 돌린다

mark phase가 root에서 reachable object를 표시한 뒤 sweep phase가 heap을 순회하며 표시되지 않은 block을 free list로 반환한다. object 주소가 유지되므로 native pointer나 pinning과 호환하기 쉽지만, 살아 있는 object 사이에 hole이 남아 external fragmentation이 커질 수 있다. allocation은 충분한 총 free bytes가 있어도 요청 크기에 맞는 block을 못 찾을 수 있다.

sweep cost는 heap size와 metadata organization에 영향을 받는다. 모든 block을 매 collection마다 훑으면 large heap에서 시간이 커지므로 lazy sweep이나 region-based sweep을 사용할 수 있다. mark와 sweep을 병렬화해도 memory bandwidth와 cache pollution이 병목이 될 수 있다.

운영 metric은 pause 하나가 아니라 mark duration, sweep rate, free-block distribution, allocation failure를 함께 본다. fragmentation 때문에 compaction이 별도로 필요한지 판단해야 한다.

---

## CHAPTER 05 · mark bitmap은 object header 변경 없이 liveness 상태를 별도 metadata에 저장한다

collector는 object마다 mark bit를 header에 넣거나 별도 bitmap을 둘 수 있다. bitmap은 heap address range를 작은 metadata로 압축해 표시하고 parallel marker가 atomic bit update를 수행할 수 있게 한다. object header를 건드리지 않아 runtime layout을 단순하게 유지할 수 있지만 bitmap 자체도 cache와 memory bandwidth를 사용한다.

large sparse heap에서는 bitmap scanning이 실제 live object보다 넓은 address range를 만질 수 있다. region summary나 card metadata를 추가해 비어 있는 범위를 건너뛸 수 있다. marking throughput이 느릴 때 object graph뿐 아니라 metadata access locality를 확인해야 한다.

corruption 조사에서는 mark bit가 틀렸다는 증상만 보지 않는다. object allocation/free와 bitmap initialization 순서, region reuse generation이 일치하는지 확인한다. stale mark state가 새 object에 남으면 collector 판단이 깨질 수 있다.

---

## CHAPTER 06 · lazy sweep은 free memory 발견을 allocation 시점으로 미루어 pause를 줄인다

mark가 끝난 뒤 heap 전체를 즉시 sweep하면 collection pause가 길어질 수 있다. lazy sweep은 region이나 page를 `unswept` 상태로 남기고 allocator가 새 memory를 필요로 할 때 해당 범위를 sweep한다. pause를 줄이는 대신 이후 allocation path가 sweep 비용을 갑자기 떠안을 수 있다.

latency-sensitive request가 처음 unswept region을 만나면 allocation p99가 튈 수 있다. background sweeper를 두면 이 비용을 분산할 수 있지만 CPU와 memory bandwidth를 지속적으로 소비한다. collector는 pause와 mutator allocation latency 사이에서 비용을 옮긴다.

운영에서는 GC pause만 보고 성공이라고 결론내리지 않는다. post-GC allocation latency, background sweep CPU, unswept backlog를 같이 봐야 한다. pause 감소가 user-visible tail 감소로 이어졌는지 확인해야 한다.

---

## CHAPTER 07 · copying collector는 live object를 새 공간으로 옮기며 fragmentation을 제거한다

copying scheme은 from-space에서 reachable object만 to-space로 복사하고 나머지 전체 영역을 한 번에 free로 간주할 수 있다. live object가 compact하게 모이므로 allocation은 bump pointer로 매우 빨라지고 fragmentation이 작다. 대신 collection 동안 live bytes를 copy해야 하고 동시에 두 공간에 대한 capacity가 필요할 수 있다.

copy cost는 heap size가 아니라 live set size에 더 가깝다. short-lived object가 많고 survival rate가 낮은 young generation에서 copying이 잘 맞는 이유다. 반대로 대부분 객체가 오래 살아남으면 매 collection마다 많은 bytes를 옮겨 throughput이 나빠진다.

성능 분석에서는 allocation rate와 survival rate, copied bytes, promotion bytes를 본다. `GC 횟수` 하나로 copying cost를 판단하면 안 된다. 같은 횟수라도 survival ratio가 다르면 비용이 크게 달라진다.

---

## CHAPTER 08 · forwarding pointer는 이동 전 주소를 새 주소로 연결해 reference graph를 복구한다

object를 이동시키면 기존 reference가 old address를 가리키므로 collector는 새 위치를 기록하고 모든 reference를 업데이트해야 한다. forwarding pointer는 old object 위치나 side metadata에 new address를 남겨 중복 copy를 막고 pointer rewrite를 가능하게 한다. graph traversal 중 같은 object를 여러 경로에서 만나도 하나의 identity를 유지하는 핵심 장치다.

concurrent compaction에서는 mutator가 old pointer를 읽을 수 있어 read barrier나 indirection이 필요할 수 있다. forwarding state publication과 object copy 완료 순서가 잘못되면 reader가 partially copied object를 볼 수 있다. 단순 memcpy보다 memory-ordering proof가 중요하다.

버그 분석에서는 stale reference가 어느 generation 주소를 가리키는지 확인한다. forwarding metadata가 언제 제거되고 old region이 언제 재사용되는지도 lifetime contract에 포함된다.

---

## CHAPTER 09 · mark-compact는 live object를 한쪽으로 모아 free space를 연속화한다

mark-compact collector는 먼저 live object를 식별한 뒤 relocation plan을 만들고 object를 이동하며 reference를 갱신한다. external fragmentation을 크게 줄일 수 있어 long-lived heap에 유용하지만 relocation 계산과 pointer update 비용이 크다. pinning이 많은 heap에서는 이동 가능한 공간이 제한되어 compaction 효과가 떨어진다.

compaction은 pause를 길게 만들 수 있어 region 단위나 incremental 방식으로 나누기도 한다. 하지만 일부 region만 정리하면 global fragmentation이 완전히 사라지지 않는다. 어떤 region을 compact할지 live density와 evacuation cost로 선택해야 한다.

운영 metric은 compaction pause, moved bytes, reclaimed contiguous space, pinned bytes를 본다. `compaction 발생`을 무조건 문제로 보지 않고 이후 allocation 성공과 footprint 개선까지 평가한다.

---

## CHAPTER 10 · generational GC는 object lifetime 분포의 편향을 이용한다

많은 managed workload에서 대부분 객체는 매우 짧게 살고 일부만 오래 살아남는다. generational collector는 young 영역을 자주 수집하고 survivor를 old generation으로 promotion해 전체 heap scanning 빈도를 줄인다. 이 가정이 맞을수록 allocation throughput과 pause에 유리하다.

promotion threshold와 young size가 너무 작으면 살아남는 object가 일찍 old로 이동해 promotion traffic과 old pressure가 커질 수 있다. 너무 크면 young collection pause와 footprint가 증가한다. workload의 survival curve가 tuning의 근거다.

진단에서는 allocation rate, age histogram, promotion rate, old occupancy를 함께 본다. 특정 feature가 medium-lived object를 대량 생성하면 generational 가정의 효율이 떨어질 수 있다. GC 설정만이 아니라 object lifetime 구조를 바꾸는 것이 더 큰 개선이 될 수 있다.

---

## CHAPTER 11 · remembered set은 old→young reference를 기록해 young collection 범위를 제한한다

young generation만 수집할 때 root와 young object만 보면 old object가 young object를 가리키는 edge를 놓칠 수 있다. remembered set은 이런 cross-generation reference를 추적해 old heap 전체를 scan하지 않고도 young liveness를 정확히 찾게 한다. write barrier가 이 정보를 유지한다.

remembered set이 너무 coarse하면 실제 edge보다 넓은 영역을 scan해 collection cost가 커지고, 너무 fine하면 update metadata가 무거워진다. card table, object set 등 구현마다 trade-off가 다르다. mutation-heavy workload는 remembered-set maintenance CPU가 커질 수 있다.

GC log에서 young pause가 증가할 때 survival만 보지 않고 scanned cards와 remembered-set size를 본다. old object가 young pointer를 많이 갱신하는 data structure가 원인일 수 있다.

---

## CHAPTER 12 · card table은 heap을 coarse region으로 나눠 dirty reference 영역을 표시한다

card table은 heap을 수백 byte 또는 그 이상의 card로 나누고 reference write가 발생한 card를 dirty로 표시한다. young collection에서는 dirty card만 다시 scan해 old→young edge를 찾는다. write barrier가 매우 가볍게 동작할 수 있지만 card 하나에 실제 relevant pointer가 하나뿐이어도 전체 card를 검사해야 한다.

false sharing처럼 여러 thread가 인접 object를 수정하면 card metadata의 동일 cache line을 경쟁할 수 있다. card 크기와 table layout이 mutator overhead에 영향을 준다. collector는 clearing과 scanning 순서도 정확히 관리해야 한다.

성능 증거에는 dirty-card rate, scanned bytes, useful discovered edge ratio를 포함한다. card table은 metadata optimization이지만 application write pattern과 직접 연결된다.

---

## CHAPTER 13 · write barrier는 pointer store에 GC bookkeeping을 삽입하는 mutator-side 계약이다

collector가 concurrent 또는 generational 상태를 유지하려면 object reference write를 관찰해야 할 수 있다. compiler와 runtime은 pointer store 주변에 barrier를 삽입해 card를 dirty로 만들거나 old value/new value를 queue에 기록한다. 이 비용은 allocation이 아니라 평상시 application mutation path에 분산된다.

barrier omission은 성능 문제가 아니라 correctness bug다. JIT optimization이 store를 제거·합칠 때도 GC semantics를 보존해야 하고 JNI/native write가 barrier를 우회하면 live object를 놓칠 수 있다. runtime 전체의 write path가 barrier contract를 공유해야 한다.

최적화는 barrier 자체를 무조건 제거하는 것이 아니라 redundant barrier를 proven-safe하게 elide하는 방식이어야 한다. workload별 barrier frequency와 cache effect를 측정해 cost를 이해한다.

---

## CHAPTER 14 · SATB는 marking 시작 시점의 reachable graph를 보존하는 방식으로 concurrent mutation을 추적한다

Snapshot-At-The-Beginning 계열은 collection 시작 시점에 reachable했던 object가 marking 중에 reference overwrite로 사라지더라도 놓치지 않게 old reference를 기록한다. mutator가 pointer를 바꾸기 전에 이전 값을 barrier buffer에 넣으면 collector가 나중에 그 object를 방문할 수 있다.

이 방식은 collection 중 새로 생성된 garbage를 일부 다음 cycle까지 남길 수 있다. safety를 위해 더 많이 live로 보는 것은 허용되지만 실제 live를 놓치는 것은 허용되지 않는다. barrier buffer overflow와 drain scheduling도 throughput과 pause에 영향을 준다.

진단에서는 SATB queue 크기와 mutator assist, remark pause를 본다. write-heavy workload에서 buffer production이 collector 처리보다 빠르면 final remark가 길어질 수 있다. mutation pattern이 concurrent marking cost를 좌우한다.

---

## CHAPTER 15 · incremental-update barrier는 black→white edge가 생기지 않도록 새 reference를 추적한다

incremental-update 방식은 이미 scan한 black object가 아직 scan하지 않은 white object를 새로 가리키는 경우 그 edge를 기록하거나 target을 gray로 만들어 tri-color invariant를 유지한다. SATB가 old reference에 관심을 두는 것과 달리 new reference mutation을 중심으로 생각할 수 있다.

어떤 barrier가 적합한지는 collector의 marking semantics와 runtime workload에 달려 있다. write frequency, allocation during marking, remark strategy가 비용을 바꾼다. 두 방식의 이름만 외우기보다 어떤 graph snapshot을 보존하는지 이해해야 한다.

compiler optimization은 barrier reordering에도 주의해야 한다. object publication과 barrier가 다른 thread/collector에게 어떤 순서로 보이는지 memory model과 함께 증명해야 한다.

---

## CHAPTER 16 · concurrent marking은 mutator와 collector가 같은 heap graph를 동시에 다루는 protocol이다

stop-the-world mark는 reasoning이 단순하지만 heap과 root가 크면 pause가 길어진다. concurrent mark는 application thread가 계속 실행되는 동안 collector thread가 graph를 탐색해 pause를 줄이지만 write barrier와 synchronization, final remark가 필요하다. 총 CPU work가 늘어날 수 있다는 점도 중요하다.

collector thread를 많이 늘리면 marking은 빨라질 수 있지만 application과 memory bandwidth를 경쟁한다. CPU-limited service에서는 concurrent GC가 throughput을 떨어뜨려 request latency를 오히려 높일 수 있다. pause와 application capacity를 함께 평가해야 한다.

GC log에는 concurrent phase duration, CPU time, mutator assist, remark pause를 분리한다. `STW가 짧다`만으로 성공을 판단하면 background CPU cost를 놓친다.

---

## CHAPTER 17 · safepoint는 runtime이 thread state를 정확히 관찰할 수 있는 협력 지점이다

collector가 root를 enumerate하거나 object를 이동하려면 각 thread의 register와 stack이 runtime metadata와 일치하는 안정된 상태가 필요하다. compiler는 특정 instruction 위치를 safepoint로 만들고 stack map을 제공한다. GC 요청 뒤 모든 thread가 safepoint에 도달할 때까지 기다리는 시간이 pause에 포함될 수 있다.

long-running native call, tight loop, non-interruptible runtime section은 safepoint latency를 늘릴 수 있다. GC 자체는 짧아도 한 thread가 늦게 멈춰 전체 pause가 길어지는 현상이 생긴다. `time to safepoint`를 별도 metric으로 보는 이유다.

성능 regression에서는 GC phase 시작 시각과 last-thread-arrival을 비교한다. compiler가 polling point를 어디에 두는지와 native transition rule이 tail pause를 결정할 수 있다.

---

## CHAPTER 18 · read barrier는 reference를 읽는 순간 relocation·marking 상태를 해석한다

일부 concurrent moving collector는 mutator가 pointer를 load할 때 barrier를 실행해 old location의 reference를 forwarding address로 바꾸거나 marking metadata를 갱신한다. write-heavy workload 대신 read path에 비용을 분산하는 설계가 될 수 있다. barrier가 fast path에서 거의 no-op이 되도록 최적화하는 것이 중요하다.

read barrier correctness는 object movement와 밀접하다. mutator가 이동 중인 object를 읽을 때 완성된 새 object를 보거나 안전한 old copy를 보도록 해야 한다. forwarding state와 memory ordering이 어긋나면 partially initialized view가 생긴다.

profile에서 load-heavy code가 barrier stub에 시간을 쓰는지 확인할 수 있다. 하지만 barrier를 우회하는 unsafe/native access는 collector invariant를 깨뜨릴 수 있으므로 성능을 이유로 제거하면 안 된다.

---

## CHAPTER 19 · parallel GC는 stop-the-world work를 여러 CPU에 나눠 pause를 줄인다

mark, copy, compact 같은 phase를 여러 worker가 병렬 수행하면 wall pause를 줄일 수 있다. 하지만 work partition이 불균형하거나 shared queue contention이 크면 worker 수를 늘려도 scaling이 제한된다. memory bandwidth와 cache coherence도 공통 병목이 된다.

small heap이나 작은 live set에서는 thread startup과 synchronization overhead가 실제 GC work보다 커질 수 있다. collector는 heap size와 CPU availability에 따라 worker 수를 조정할 수 있다. application이 CPU를 강하게 요구하는 순간에는 GC worker 경쟁도 고려해야 한다.

GC trace에서 worker별 active time과 idle/barrier wait를 보면 imbalance를 찾을 수 있다. 평균 pause만 줄이는 것보다 CPU cost와 thermal 영향까지 포함한 end-to-end 평가가 필요하다.

---

## CHAPTER 20 · weak reference는 reachability 강도를 낮춰 cache와 lifecycle policy를 표현한다

weak reference는 대상이 다른 strong path로 reachable하지 않다면 collector가 회수할 수 있게 한다. 이것은 `언젠가 지워지는 cache`를 만들 수 있지만 collection timing은 memory pressure와 GC policy에 따라 달라지므로 expiry나 resource close 같은 정확한 lifecycle을 맡길 수 없다.

weak map을 사용해도 value가 key를 다시 strong하게 참조하면 cycle 때문에 기대한 회수가 안 될 수 있다. runtime별 reference processing order와 semantics를 확인해야 한다. cache correctness를 GC timing에 의존시키면 재현성이 낮아진다.

진단에서는 weak reference count보다 target의 다른 root path를 찾는다. 회수되지 않는 이유가 collector가 아니라 hidden strong owner일 수 있다. explicit bounded cache와 TTL이 더 적합한 경우도 많다.

---

## CHAPTER 21 · finalizer는 reclamation 시점과 외부 resource release를 불확실하게 만든다

finalizer는 object가 unreachable해진 뒤 runtime이 별도 queue와 thread에서 cleanup callback을 실행하는 방식이지만 실행 시점이 deterministic하지 않다. finalizer가 느리거나 blocked되면 finalized 대기 객체가 쌓여 heap과 file descriptor 같은 외부 resource가 동시에 고갈될 수 있다.

object가 finalizer에서 자기 자신을 다시 reachable하게 만드는 resurrection도 reasoning을 복잡하게 한다. modern design은 explicit close, RAII/context manager를 사용해 resource lifetime을 lexical 또는 request lifetime에 연결하는 편이 안전하다.

운영에서 finalizer queue length, processing latency를 관찰한다. GC가 자주 돌아도 finalizer thread backlog 때문에 native resource가 남을 수 있다. heap 문제와 cleanup queue 문제를 구분해야 한다.

---

## CHAPTER 22 · ephemeron은 key reachability에 따라 value가 key를 살리는 cycle을 안전하게 다룬다

일반 weak-key map에서 value가 key를 참조하면 단순 weak semantics로는 원하는 collection behavior를 표현하기 어렵다. ephemeron은 key가 다른 경로에서 reachable할 때만 value를 live로 간주하는 고정점 계산을 통해 이 문제를 해결한다. runtime metadata table이나 weak map 구현에서 중요하다.

collector는 한 번의 mark pass로 끝나지 않고 새로 reachable해진 ephemeron value가 또 다른 key를 살릴 수 있으므로 반복 처리가 필요할 수 있다. ephemeron 수가 많으면 remark와 reference-processing 비용이 커질 수 있다.

진단에서 weak structure가 예상보다 memory를 오래 잡고 있다면 key-value graph와 ephemeron processing을 본다. 일반 strong/weak edge만으로 retained path를 해석하면 원인을 놓칠 수 있다.

---

## CHAPTER 23 · pinning은 object 이동을 금지해 native interoperability를 얻는 대신 compaction 자유도를 줄인다

native code나 device가 object address를 일정 시간 직접 사용해야 하면 collector가 그 object를 이동시키지 못하도록 pin할 수 있다. 짧은 pin은 필요하지만 long-term pin이 많아지면 moving collector가 region을 비우지 못하고 fragmentation과 evacuation failure가 늘 수 있다.

pinning API는 scope와 duration을 엄격히 제한해야 한다. JNI critical section 안에서 blocking I/O를 수행하면 GC가 필요한 relocation을 오래 기다릴 수 있다. pointer를 native global에 저장하는 패턴은 explicit handle이나 copied buffer로 바꾸는 것이 안전할 수 있다.

GC log에서 pinned bytes, failed evacuation, compacted region density를 확인한다. memory pressure 때만 pause가 길어진다면 pinning과 fragmentation의 결합을 의심한다.

---

## CHAPTER 24 · large object는 일반 young-copy path와 다른 allocation·collection 정책이 필요할 수 있다

수 MB 크기의 object를 young space에서 반복 copy하면 memory bandwidth와 pause 비용이 매우 크다. runtime은 large-object space나 humongous region에 직접 배치해 이동을 줄일 수 있다. 대신 큰 연속 영역을 요구해 fragmentation과 region waste가 문제가 될 수 있다.

large object threshold 근처의 size distribution이 중요하다. 몇 byte 차이로 일반 path와 large path가 갈리면 latency와 lifetime이 크게 바뀔 수 있다. image, byte buffer, temporary serialization 결과처럼 큰 ephemeral object가 반복되면 pooling이나 streaming 구조를 검토해야 한다.

profile은 large allocation count, lifetime, region occupancy를 분리한다. 객체 수가 적어도 총 bytes와 GC pressure를 지배할 수 있다. count 기반 top list만 보면 놓치기 쉽다.

---

## CHAPTER 25 · region-based collector는 heap을 독립 관리 단위로 나눠 선택적 collection을 가능하게 한다

heap을 고정 크기 region으로 나누면 collector는 live density, age, remembered-set cost를 기준으로 어떤 region을 수집할지 선택할 수 있다. 전체 heap을 매번 compact하지 않고 garbage가 많은 region을 우선 evacuation해 pause 목표를 맞추는 전략이 가능하다.

region 크기가 너무 작으면 metadata와 remembered-set overhead가 커지고, 너무 크면 large object waste와 selection granularity가 나빠진다. evacuation에는 destination free region이 필요하므로 heap occupancy가 너무 높으면 collector가 움직일 여유가 없어 failure mode가 바뀐다.

운영에서는 region occupancy distribution, evacuation failure, reserve percentage를 본다. 평균 heap 사용률이 같아도 garbage가 어떻게 분포하는지에 따라 collection 비용이 달라진다.

---

## CHAPTER 26 · fragmentation은 free bytes 총량보다 allocator와 collector가 실제로 사용할 수 있는 형태가 중요하다

moving collector는 compaction으로 fragmentation을 줄일 수 있지만 pinning, large-object region, native allocation은 그대로 남을 수 있다. non-moving space에서는 free block이 많아도 large request를 만족할 contiguous range가 부족할 수 있다. `free heap` 한 숫자로 allocation 가능성을 판단하면 안 된다.

fragmentation은 internal waste, sparse region, unmovable hole을 나눠 본다. heap dump의 logical live bytes가 낮은데 committed heap이 높은 경우 region occupancy와 allocator metadata를 확인한다. GC frequency를 늘려도 movable garbage가 없으면 개선되지 않는다.

장기 soak test에서 peak 후 footprint 회복 속도를 측정하면 fragmentation과 cache retention을 구분하는 데 도움이 된다. restart만으로 해결되는 증상은 allocator geometry가 누적되는지 확인해야 한다.

---

## CHAPTER 27 · GC scheduling은 언제 collection을 시작할지 결정하는 control problem이다

heap이 완전히 찬 뒤 collection을 시작하면 concurrent collector도 finish 전에 allocation이 공간을 소진할 수 있다. 너무 일찍 시작하면 background GC가 불필요하게 자주 돌아 CPU와 battery를 낭비한다. allocation rate, expected marking speed, free headroom을 이용해 시작 시점을 예측해야 한다.

mutator utilization 목표와 pause target이 scheduling에 영향을 준다. sudden burst에서 collector가 뒤처지면 application thread가 marking이나 allocation slow path를 도와야 할 수 있다. 이런 mutator assist는 user latency로 직접 나타난다.

운영에서는 trigger occupancy, allocation rate, concurrent-cycle duration, assist time을 같은 그래프에 놓는다. tuning은 heap size 하나가 아니라 feedback loop의 안정성을 조정하는 일이다.

---

## CHAPTER 28 · GC log는 phase 이름보다 allocation·survival·pause·CPU의 관계를 읽어야 한다

좋은 GC log에는 collection reason, heap before/after, young/old occupancy, promoted bytes, pause duration, concurrent phase, worker CPU가 포함된다. pause 한 줄만 보면 allocation burst, promotion pressure, safepoint delay를 구분할 수 없다. runtime version에 따라 field 의미가 달라질 수 있어 parser도 version-aware해야 한다.

incident timeline에 request latency와 GC event를 겹친다고 곧바로 causality가 증명되는 것은 아니다. 같은 memory pressure가 GC와 latency를 동시에 유발했을 수 있다. pause interval 동안 thread가 실제 멈췄는지, CPU contention이 증가했는지 trace로 교차검증한다.

baseline workload의 allocation rate와 GC distribution을 저장해 regression을 비교한다. `GC가 있었다`가 아니라 이전보다 어떤 phase가 왜 커졌는지를 설명해야 한다.

---

## CHAPTER 29 · managed-memory leak는 retained root path를 ownership bug로 해석해야 한다

collector가 unreachable object를 정상적으로 회수해도 application root가 계속 reference를 잡으면 heap은 증가한다. Activity context를 static singleton이 보유하거나 callback registry에서 unregister하지 않는 패턴이 대표적이다. object가 크지 않아도 dominator가 거대한 graph를 붙잡을 수 있다.

heap snapshot 비교는 class count 증가만 보지 않는다. retained size, dominator tree, GC root path, object age를 연결해 누가 lifetime을 연장하는지 찾는다. cache처럼 의도적 retention인지 leak인지 제품 contract와 대조해야 한다.

수정 후에는 root edge가 사라졌는지와 반복 scenario에서 retained graph가 안정화되는지 확인한다. 한 번 GC 후 heap이 내려갔다는 사실만으로 lifecycle leak가 해결됐다고 결론내리지 않는다.

---

## CHAPTER 30 · collector 선택은 pause·throughput·footprint·barrier cost의 우선순위를 명시하는 일이다

어떤 collector도 모든 workload에서 동시에 최소 pause, 최대 throughput, 최소 memory를 제공하지 않는다. server batch는 throughput을 우선할 수 있고 interactive mobile app은 frame deadline과 battery를 더 중요하게 볼 수 있다. heap size, core 수, allocation rate, object lifetime이 선택 결과를 바꾼다.

collector 이름을 바꾸기 전에 현재 병목을 증명한다. safepoint가 문제인지, concurrent CPU가 문제인지, fragmentation인지, promotion pressure인지에 따라 필요한 mechanism이 다르다. collector 변경은 object layout과 timing을 바꿔 latent race나 JNI bug를 드러낼 수도 있다.

평가는 동일 workload에서 p50/p99 pause, mutator utilization, total CPU, peak RSS, missed deadline을 함께 비교한다. 운영 evidence와 crash diagnostics가 충분한지도 선택 기준에 포함한다. GC tuning의 목적은 숫자 하나를 줄이는 것이 아니라 application의 latency와 capacity invariant를 안정적으로 유지하는 것이다.