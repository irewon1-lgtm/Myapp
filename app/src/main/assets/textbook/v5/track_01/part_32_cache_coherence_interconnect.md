# PART 32 · Cache Coherence and Interconnect — ownership, invalidation, topology

멀티코어 성능은 cache miss 수만으로 설명되지 않는다. 여러 core가 같은 cache line을 읽고 쓸 때 coherence protocol이 ownership을 이동시키고 invalidation을 전파하며 interconnect bandwidth를 소비한다. **logical sharing과 physical cache-line sharing을 분리하고, line ownership의 이동 자체를 비용으로 관찰**해야 scalability collapse를 이해할 수 있다.

---

## CHAPTER 01 · coherence와 consistency는 서로 다른 보장이다

cache coherence는 같은 physical memory location에 대한 core별 cached copy가 모순된 값으로 영구 분기하지 않게 write ownership과 invalidation을 조정한다. memory consistency model은 서로 다른 location의 operation을 observer가 어떤 순서로 볼 수 있는지 정의한다. coherence가 있다고 happens-before가 자동 생기지는 않는다.

lock 없는 shared state가 'cache는 일관되니까 안전하다'는 주장은 틀릴 수 있다. compiler와 CPU ordering을 language memory model로 별도 증명해야 한다.

incident에서는 coherence traffic과 synchronization correctness를 다른 축으로 본다. value corruption은 memory-order proof로, scalability 문제는 line ownership과 interconnect counter로 분석한다.

---

## CHAPTER 02 · cache line이 coherence의 실제 전송 단위가 된다

CPU cache는 개별 변수보다 더 큰 cache line 단위로 data를 저장하고 coherence protocol도 보통 line 단위 ownership을 다룬다. 서로 다른 field가 같은 line에 있으면 한 field write가 다른 field reader의 cache state까지 invalidation시킬 수 있다.

source object가 작다고 traffic도 작다는 뜻은 아니다. array stride와 allocator placement가 line sharing을 결정한다.

binary/object layout과 cache-line 크기를 확인한다. address sampling과 cache-to-cache counter를 이용해 hot line을 찾고 source field와 매핑한다.

---

## CHAPTER 03 · shared state는 read-only 공유와 writable 공유를 구분해야 한다

여러 core가 같은 line을 읽기만 하면 cached copy를 동시에 유지할 수 있어 비교적 싸다. 누군가 write를 시작하면 exclusive/modified ownership을 얻기 위해 다른 copy를 invalidate해야 하며 traffic이 급증할 수 있다.

configuration snapshot과 global counter는 둘 다 shared지만 비용 구조가 완전히 다르다. read-mostly data를 불필요하게 갱신하는 timestamp/counter가 scalability를 망칠 수 있다.

shared field별 read/write frequency를 조사한다. mutable metadata를 별도 line이나 per-CPU state로 분리해 ownership movement를 줄일 수 있는지 본다.

---

## CHAPTER 04 · modified state는 dirty data의 최신 copy ownership을 의미한다

coherence protocol의 modified-like state에서는 해당 cache가 memory보다 최신 값을 가진 유일한 owner일 수 있다. 다른 core가 같은 line을 요청하면 owner가 data를 공급하거나 writeback/transfer가 필요하다.

hot write line이 core 사이를 왕복하면 memory DRAM까지 가지 않아도 interconnect와 cache controller가 병목이 될 수 있다. CPU utilization은 높지만 useful work가 낮아지는 패턴이다.

cache-to-cache transfer와 HITM류 event를 platform에 맞게 측정한다. ownership이 어느 CPU pair 사이를 이동하는지 topology와 함께 본다.

---

## CHAPTER 05 · exclusive state는 private line의 future write를 싸게 만든다

한 core만 clean copy를 가진 exclusive-like state에서는 다른 core invalidation 없이 local write로 modified 상태로 전환할 수 있다. data가 thread-local하게 유지될 때 write cost가 낮은 이유다.

thread migration이 일어나면 이전 CPU의 private line이 새 CPU로 이동해 locality 이점을 잃을 수 있다. per-thread data를 여러 worker가 번갈아 처리하면 사실상 shared traffic이 된다.

CPU affinity와 line transfer를 함께 추적한다. ownership을 한 execution context에 유지하는 설계가 lock 최적화보다 효과적인 경우가 많다.

---

## CHAPTER 06 · invalidation은 writer가 다른 cached copy를 무효화하는 과정이다

shared line을 write하려면 다른 core의 read copy를 invalidate해 exclusive ownership을 확보해야 한다. reader 수가 많을수록 write 하나가 더 넓은 interconnect traffic을 만들 수 있다. write-heavy global counter가 core 수 증가에 따라 나빠지는 이유다.

polling reader가 같은 line을 계속 읽으면 writer와 reader가 ownership/validation traffic을 반복할 수 있다.

invalidation-related event와 write frequency를 측정한다. read-mostly snapshot, batching, per-CPU aggregation으로 write 횟수를 줄이는 실험을 한다.

---

## CHAPTER 07 · ownership request는 write permission을 얻기 위한 coherence transaction이다

core가 shared line을 수정하려면 request-for-ownership 류 transaction을 보내 다른 copy를 invalidate하고 write 권한을 얻는다. 실제 protocol 이름은 CPU마다 달라도 핵심은 exclusive write authority의 이동이다.

여러 core가 같은 line에 atomic increment를 반복하면 매 operation마다 ownership이 ping-pong할 수 있다. atomic instruction 자체 latency보다 line migration이 지배적이 된다.

atomic throughput을 core 수별로 측정한다. line ownership counter와 scaling curve가 함께 악화되는지 확인해 contention 원인을 증명한다.

---

## CHAPTER 08 · cache-to-cache transfer는 DRAM을 거치지 않는 data 이동도 비용이 있음을 보여 준다

한 core가 가진 최신 cache line을 다른 core가 요구하면 cache-to-cache transfer로 data가 전달될 수 있다. DRAM miss보다 빠를 수 있지만 socket/NUMA topology를 넘으면 상당한 latency와 interconnect bandwidth를 소비한다.

'LLC hit'처럼 보이는 event라도 어느 cache가 data를 공급했는지에 따라 비용이 다를 수 있다.

source/destination CPU와 remote/local transfer를 구분한다. lock owner migration이나 work stealing이 cache-to-cache traffic을 늘리는지 trace와 연결한다.

---

## CHAPTER 09 · snooping은 participant가 coherence request를 관찰하는 방식이다

snoop 기반 protocol은 coherence request를 relevant cache에 전달해 해당 line state를 확인·변경한다. core 수가 커질수록 broadcast-like 방식의 비용이 커질 수 있어 hierarchy와 filtering이 중요하다.

snoop traffic이 많아지면 interconnect가 application data보다 coherence message로 포화될 수 있다. 정확한 구현은 architecture별로 다르므로 generic MESI 그림만으로 실제 topology를 단정하지 않는다.

uncore/interconnect counter와 CPU topology를 함께 본다. scaling regression이 core 수 특정 임계점에서 발생하는지 확인한다.

---

## CHAPTER 10 · directory protocol은 sharer 정보를 추적해 coherence traffic을 좁힌다

directory-based coherence는 특정 line을 어느 node/cache가 보유하는지 metadata로 추적해 필요한 participant에 request를 보낼 수 있다. large multi-socket system에서 무조건 전체 broadcast하는 비용을 줄이는 데 유리하다.

하지만 directory lookup과 home location이 새로운 latency와 hotspot이 될 수 있다. sharer set가 커지면 metadata와 invalidation fan-out도 증가한다.

NUMA socket별 access와 directory/home traffic을 관찰한다. data placement와 thread placement가 coherence home까지 일관되는지 평가한다.

---

## CHAPTER 11 · LLC policy는 sharing과 eviction behavior에 영향을 준다

last-level cache가 inclusive, exclusive, non-inclusive 성격을 어떻게 갖는지에 따라 lower-level cache tracking과 eviction effect가 달라질 수 있다. 한 core의 LLC pressure가 다른 core private cache에 간접 영향을 줄 수도 있다.

CPU 세대별 policy가 다르므로 오래된 architecture 설명을 그대로 적용하면 counter 해석이 틀릴 수 있다.

실제 CPU documentation과 cache topology를 기록한다. co-tenant workload로 LLC capacity pressure와 coherence traffic을 분리해 측정한다.

---

## CHAPTER 12 · false sharing은 서로 다른 변수의 write도 같은 line이면 경쟁하게 만든다

thread A와 B가 서로 다른 counter만 수정해 logical data sharing이 없어도 두 counter가 같은 cache line에 있으면 write ownership이 core 사이를 이동한다. data race detector는 문제를 찾지 못할 수 있지만 throughput은 급격히 떨어진다.

allocator alignment와 array layout 때문에 source에서 떨어진 field도 runtime에 같은 line에 배치될 수 있다.

actual address와 line boundary를 확인한다. padding/per-thread aggregation 전후 cache-to-cache transfer와 scaling을 비교한다.

---

## CHAPTER 13 · true sharing은 동일 mutable state에 대한 실제 coordination 비용이다

여러 thread가 같은 queue head, lock word, reference count를 수정해야 한다면 coherence traffic은 logical synchronization requirement의 물리적 비용이다. padding만으로 없앨 수 없고 algorithm의 sharing 구조를 바꿔야 한다.

global state가 workload에 꼭 필요한지, sharding·batching·ownership transfer로 mutation 빈도를 줄일 수 있는지 먼저 검토한다.

hot line의 operation semantics를 분류한다. throughput과 fairness를 유지하며 shared mutation 횟수를 줄이는 redesign을 benchmark한다.

---

## CHAPTER 14 · atomic RMW는 cache-line ownership과 serialization을 동시에 요구한다

atomic fetch-add/CAS는 해당 memory location을 원자적으로 갱신하기 위해 line의 exclusive ownership과 hardware serialization을 필요로 한다. core가 늘어날수록 하나의 global atomic counter는 single service center처럼 동작할 수 있다.

relaxed memory order로 바꿔도 ownership ping-pong 자체는 사라지지 않는다. ordering 최적화와 coherence 최적화를 분리해야 한다.

CAS failure, atomic throughput, cache-line transfer를 같이 본다. per-CPU counter 후 aggregation이 더 적합한지 비교한다.

---

## CHAPTER 15 · spinlock은 wait 자체가 coherence traffic을 만들 수 있다

여러 waiter가 같은 lock word를 반복 read하거나 RMW하면 owner가 unlock할 때 line이 여러 cache 사이를 이동한다. test-and-set loop는 특히 write-like traffic을 반복해 contention을 악화시킬 수 있다.

test-and-test-and-set처럼 local cached read로 대기하다 변화 시 RMW를 시도하는 구조는 traffic을 줄일 수 있지만 owner preemption 문제는 남는다.

lock wait, RMW count, line transfer를 측정한다. high contention에서는 parking mutex나 queue lock과 비교한다.

---

## CHAPTER 16 · ticket lock은 fairness를 얻지만 shared counter hotspot을 만들 수 있다

ticket lock은 각 waiter가 순번을 받고 현재 serving 값을 기다려 FIFO fairness를 제공한다. 하지만 모든 waiter가 같은 serving line을 관찰하므로 unlock 때 invalidation fan-out이 커질 수 있다.

많은 core에서 fairness는 좋지만 coherence scalability가 나빠질 수 있다. NUMA socket을 넘는 waiter가 많으면 비용이 더 커진다.

waiter 수별 throughput과 remote cache transfer를 측정한다. fairness SLO가 실제로 필요한지, queue-based lock이 더 적합한지 비교한다.

---

## CHAPTER 17 · queue lock은 waiter별 local state로 공유 line 경쟁을 분산한다

MCS류 queue lock은 각 waiter가 자신의 node/flag를 주로 spin하도록 만들어 global lock line에 대한 반복 traffic을 줄인다. predecessor→successor handoff로 ownership transfer를 구조화한다.

wait node lifetime과 cancellation, thread preemption을 올바르게 처리해야 한다. 짧은 low-contention lock에서는 metadata 비용이 더 클 수 있다.

core 수와 contention sweep을 수행한다. global line transfer 감소와 p99 acquisition latency를 함께 본다.

---

## CHAPTER 18 · lock-free algorithm도 coherence-free는 아니다

lock을 제거하고 CAS loop를 사용해도 hot pointer나 queue head를 여러 core가 갱신하면 동일 cache line ownership 경쟁이 남는다. CAS failure와 retry가 오히려 coherence traffic을 늘릴 수 있다.

lock-free label만 보고 scalability를 기대하면 안 된다. reclamation metadata와 hazard pointer update도 추가 shared write가 될 수 있다.

operation별 shared line을 inventory한다. lock-based reference 구현과 throughput·tail·CPU cost를 비교해 실제 workload에서 선택한다.

---

## CHAPTER 19 · read-mostly 구조는 mutation을 드물게 만들어 coherence 비용을 줄인다

immutable snapshot, RCU, copy-on-write config는 reader가 shared clean line을 읽고 writer가 새 version을 만들어 한 번 publish하게 해 continuous write sharing을 줄일 수 있다. read-heavy workload에서 특히 유리하다.

update마다 큰 object를 copy하면 memory와 publication cost가 늘어나므로 write frequency와 object size를 함께 본다.

reader cache miss, writer publication, old-version reclamation을 측정한다. synchronization correctness와 coherence scalability를 동시에 검증한다.

---

## CHAPTER 20 · NUMA를 넘는 coherence는 remote interconnect 비용을 추가한다

multi-socket system에서 한 socket의 cache line을 다른 socket core가 write하면 coherence message와 data가 socket interconnect를 지나야 한다. 같은 socket 내부 ping-pong보다 latency와 bandwidth 비용이 커질 수 있다.

thread migration이나 cross-socket work stealing이 global lock의 cost를 갑자기 키울 수 있다.

socket별 thread placement와 remote HITM/cache-to-cache event를 본다. state sharding을 NUMA domain과 맞추는 실험을 수행한다.

---

## CHAPTER 21 · home agent는 address의 coherence coordination 지점을 제공한다

많은 architecture에서 physical address는 특정 home agent/directory slice와 연결되어 coherence request routing과 ownership tracking에 사용된다. data가 어느 core에서 사용되는지뿐 아니라 어느 home path를 거치는지도 traffic 분포에 영향을 줄 수 있다.

address hashing과 topology 때문에 특정 allocation pattern이 일부 slice에 hot spot을 만들 수 있다.

uncore slice/home traffic을 가능한 범위에서 측정한다. allocator/address pattern 변경이 imbalance를 줄이는지 확인한다.

---

## CHAPTER 22 · interconnect saturation은 cache-coherent system의 숨은 shared bottleneck이다

core, LLC slice, memory controller를 연결하는 interconnect는 data와 coherence message를 운반한다. DRAM bandwidth가 남아 있어도 cache-to-cache와 invalidation traffic이 많으면 interconnect가 먼저 포화될 수 있다.

CPU를 더 추가했는데 throughput이 떨어지고 remote/coherence event가 증가하면 이 병목을 의심한다.

uncore bandwidth와 core scaling curve를 함께 본다. local sharding과 batching이 실제 interconnect traffic을 줄이는지 검증한다.

---

## CHAPTER 23 · coherence counter는 logical event와 CPU model을 함께 해석해야 한다

PMU는 snoop, HITM, cache-to-cache transfer 같은 coherence-related event를 제공할 수 있지만 event 이름과 정확한 의미는 microarchitecture마다 다르다. 단일 generic counter를 모든 machine에 그대로 적용하면 오진할 수 있다.

multiplexing과 speculative counting도 고려해야 한다.

CPU model, raw event, sampling period를 artifact에 저장한다. counter가 latency regression과 같은 workload phase에서 변화하는지 교차검증한다.

---

## CHAPTER 24 · address sampling은 hot coherence line을 source object에 연결한다

메모리 access sampling이나 load/store address sampling을 사용하면 어느 address/line에서 remote access와 cache-to-cache transfer가 반복되는지 찾을 수 있다. line address를 allocation/object layout과 연결해야 source-level 원인이 드러난다.

ASLR과 allocator reuse 때문에 raw address는 run마다 바뀐다. symbol과 object generation이 필요하다.

sampled address를 cache-line 단위로 aggregate한다. top line의 writer/reader CPU와 field를 확인해 false/true sharing을 구분한다.

---

## CHAPTER 25 · padding은 line 분리를 만들지만 무조건 정답은 아니다

hot writable field 사이에 padding/alignment를 넣으면 false sharing을 줄일 수 있다. 그러나 object size와 cache footprint를 늘리고 array density를 낮춰 다른 cache miss를 증가시킬 수 있다.

platform cache-line size와 allocator alignment를 고려해야 한다. compile-time padding이 actual runtime placement를 보장하는지 확인한다.

layout 전후 object size, cache miss, cache-to-cache traffic을 모두 측정한다. logical sharing이 있다면 padding으로 해결하려 하지 않는다.

---

## CHAPTER 26 · per-CPU sharding은 global write를 local write와 느린 aggregation으로 바꾼다

counter, statistics, freelist를 CPU별로 나누면 hot path에서 local cache line만 수정하고 필요할 때 aggregate할 수 있어 coherence traffic을 크게 줄인다. 대신 migration과 aggregation consistency가 새로운 문제다.

정확한 global instant value가 필요하면 per-CPU 구조의 eventual aggregate가 요구사항을 만족하지 못할 수 있다.

local update latency와 aggregation cost를 분리해 측정한다. CPU hotplug/migration에서 shard ownership이 안전한지 테스트한다.

---

## CHAPTER 27 · task migration은 private cache line의 ownership locality를 바꾼다

thread가 CPU를 옮기면 그 thread의 hot stack/heap line이 이전 cache에 남아 새 CPU가 다시 가져와야 한다. mutable thread-local data도 physical 관점에서는 cache-to-cache transfer가 된다.

work stealing이 load balance를 개선하면서 cache locality를 해칠 수 있다. 특히 large working set thread의 migration cost가 크다.

migration event와 cache miss/transfer burst를 연결한다. scheduler affinity 변경 전후 end-to-end throughput을 본다.

---

## CHAPTER 28 · coherence benchmark는 read-sharing과 write-sharing을 분리해야 한다

shared read benchmark, ping-pong write, atomic counter, false-sharing array는 서로 다른 protocol path를 측정한다. 하나의 `cache latency` benchmark로 coherence 전체를 설명할 수 없다.

core distance, socket, SMT sibling을 바꾸면 결과가 달라진다. compiler가 loop를 제거하지 않게 benchmark harness도 통제해야 한다.

participant 수와 topology sweep을 수행한다. line ownership traffic과 operation latency를 함께 저장한다.

---

## CHAPTER 29 · coherence incident는 lock profile 밖의 traffic까지 본다

production scalability regression에서 lock wait가 낮아도 global counter, allocator metadata, queue index가 false/true sharing을 일으킬 수 있다. CPU usage가 늘지만 throughput이 줄어드는 현상은 coherence collapse의 신호일 수 있다.

software deploy로 field layout이나 thread placement가 바뀌었는지 확인한다.

PMU, address sampling, scheduler migration을 같은 incident timeline에 연결한다. hot line을 찾은 뒤 source invariant와 ownership 구조를 재설계한다.

---

## CHAPTER 30 · coherence contract는 mutable ownership과 topology를 명시한다

scalable shared-memory design은 어떤 data가 read-only인지, 어떤 core/thread가 mutate하는지, synchronization point가 어디인지, line sharing이 unavoidable한지 설명할 수 있어야 한다. correctness memory model과 physical coherence cost를 동시에 고려한다.

lock을 제거하거나 padding을 넣는 단편 최적화보다 ownership domain을 줄이는 구조가 강하다. NUMA와 scheduler placement도 contract에 들어간다.

CLEAN 검증은 false sharing, atomic hotspot, cross-socket transfer, migration을 서로 다른 failure mode로 benchmark한다. core 수가 늘어도 throughput과 tail latency가 요구 범위에서 유지되는지 최종 확인한다.
