# PART 32 · Cache Coherence and Interconnect — ownership, invalidation, directory, cache-to-cache traffic

멀티코어 cache는 각 core를 빠르게 하지만 동일 physical cache line이 여러 private cache에 존재할 수 있다는 새 문제를 만든다. Coherence protocol은 **한 memory location에 대한 write ownership과 read visibility를 cache line 단위로 조정**한다. Lock contention, atomic counter, false sharing이 느린 이유는 source code의 lock 문법보다 cache line ownership이 core 사이를 왕복하기 때문이다.

---

## CHAPTER 01 · Coherence와 consistency는 다른 질문이다

Cache coherence는 같은 memory location에 대한 write/read가 core 사이에서 일관된 value history를 갖도록 하는 문제다. Memory consistency는 서로 다른 location의 operation 순서를 program이 어떻게 관찰할 수 있는지 정의한다. PART 27의 weak-memory model은 consistency, 이 PART는 cache line ownership/propagation mechanism에 집중한다.

Coherent cache가 있다고 sequentially consistent execution이 자동 보장되는 것은 아니다.

---

## CHAPTER 02 · Cache line이 coherence의 기본 관리 단위다

CPU가 4-byte integer 하나를 수정해도 coherence protocol은 그 integer가 속한 전체 cache line의 state를 관리할 수 있다. 그래서 서로 다른 variable이 같은 line에 있으면 논리적으로 독립해도 write ownership을 경쟁한다.

Data layout은 semantic field boundary가 아니라 cache-line physical boundary까지 성능에 영향을 준다.

---

## CHAPTER 03 · Shared state는 여러 cache가 read-only copy를 가질 수 있게 한다

Line이 여러 core cache에 read-only 상태로 존재하면 load는 local cache에서 처리될 수 있다. Write가 없을 때 replication은 memory bandwidth와 latency를 줄인다. Problem은 한 core가 write하려 할 때 시작된다.

Writer는 다른 shared copy를 invalidate하거나 protocol이 요구하는 exclusive ownership을 얻어야 한다.

---

## CHAPTER 04 · Modified state는 memory보다 cache가 더 최신 value를 가질 수 있음을 뜻한다

Write-back cache에서 core가 line을 수정하면 backing memory는 아직 old data를 가질 수 있다. Protocol state는 어느 cache가 dirty 최신 copy를 소유하는지 추적해 다른 core request에 올바른 data를 제공해야 한다.

Memory dump/physical DRAM value를 `항상 가장 최신 값`으로 단정하지 않는 이유다.

---

## CHAPTER 05 · Exclusive state는 write 전 upgrade traffic을 피할 수 있다

어떤 line을 한 core만 clean copy로 가지고 있고 다른 sharer가 없다면 protocol은 exclusive state로 표시해 이후 write가 별도 invalidation 없이 modified로 전환되게 할 수 있다. Read-mostly private data는 이 path에서 유리하다.

Allocation first-touch와 ownership history가 write latency를 바꿀 수 있다.

---

## CHAPTER 06 · Invalidation protocol은 write 권한을 하나의 owner로 모은다

Shared line을 수정하려는 core는 다른 cache copy를 invalid state로 만들고 write ownership을 확보한다. 다른 core가 이후 read하면 다시 owner/memory에서 line을 가져와야 한다. Repeated multi-writer pattern은 invalidation traffic을 계속 만든다.

Lock variable와 shared counter가 hot line이 되는 근본 비용이다.

---

## CHAPTER 07 · Upgrade request와 read-for-ownership은 data transfer requirement가 다르다

Writer가 이미 clean shared copy를 갖고 있다면 data를 다시 받을 필요 없이 ownership upgrade만 필요할 수 있다. Line이 cache에 없으면 read-for-ownership처럼 data fetch와 invalidation을 함께 수행할 수 있다.

PMU/interconnect counter가 ownership request 종류를 구분한다면 contention diagnosis에 활용할 수 있다.

---

## CHAPTER 08 · Cache-to-cache transfer는 DRAM을 거치지 않고 최신 line을 전달할 수 있다

한 core가 modified line을 보유할 때 다른 core가 read를 요청하면 owner cache가 data를 직접 전달하거나 shared cache/interconnect를 통해 공급할 수 있다. 이 path는 DRAM access보다 빠를 수 있지만 interconnect bandwidth를 사용한다.

Remote-cache hit latency도 local cache hit보다 훨씬 클 수 있다.

---

## CHAPTER 09 · Snooping은 request를 broadcast하고 sharer가 반응하게 한다

작은 shared bus/limited core system에서는 coherence request를 여러 cache에 broadcast해 해당 line 보유 여부를 확인하는 snoop 방식이 가능하다. Core 수가 커지면 broadcast traffic이 scalability bottleneck이 될 수 있다.

Interconnect design은 core count와 topology에 따라 snoop filter/directory를 사용해 traffic을 줄인다.

---

## CHAPTER 10 · Directory protocol은 sharer 정보를 metadata로 추적한다

Directory는 cache line을 어떤 node/core가 보유하는지 기록해 invalidation/request를 필요한 sharer에게만 보낼 수 있다. Broadcast를 줄이는 대신 directory storage와 lookup/consistency 관리가 필요하다.

NUMA/many-core system에서는 directory location과 home node가 remote access latency에 영향을 준다.

---

## CHAPTER 11 · Inclusive/exclusive/non-inclusive LLC policy가 snoop behavior를 바꾼다

Last-level cache가 private cache line을 반드시 포함하는 inclusive policy라면 LLC tag를 snoop filter처럼 사용할 수 있지만 LLC eviction이 private cache invalidation을 유발할 수 있다. Exclusive/non-inclusive policy는 capacity와 coherence metadata trade-off가 다르다.

`LLC miss = DRAM access` 같은 단순 해석은 architecture policy를 확인해야 한다.

---

## CHAPTER 12 · False sharing은 data race 없이도 line ownership ping-pong을 만든다

Thread A와 B가 서로 다른 atomic/counter를 수정하지만 두 variable이 같은 cache line에 있으면 각각 write할 때 line ownership이 core 사이를 왕복한다. Logical synchronization은 독립적이어도 physical coherence는 공유된다.

Padding/alignment로 hot writer field를 다른 line에 분리하면 성능이 크게 개선될 수 있다. 그러나 memory footprint와 prefetch locality 비용이 생긴다.

---

## CHAPTER 13 · True sharing과 false sharing은 해결책이 다르다

Same variable을 여러 core가 write하는 true sharing은 data layout으로 없앨 수 없다. Algorithm을 sharding/per-core aggregation으로 바꿔 write frequency를 줄여야 한다. False sharing은 unrelated variables placement를 바꿔 해결 가능하다.

Cache-line bounce counter만 보고 padding부터 넣지 말고 access semantic을 확인한다.

---

## CHAPTER 14 · Atomic RMW는 exclusive ownership과 serialization point를 요구한다

Fetch-add/CAS 같은 RMW는 read와 write를 indivisible하게 수행해야 해 해당 line에 exclusive ownership을 필요로 한다. 여러 core가 같은 atomic에 RMW하면 coherence ownership이 연속 이동하며 throughput이 core 수에 비례하지 않는다.

Global reference counter·metrics counter는 per-core local counter 후 aggregation으로 contention을 줄일 수 있다.

---

## CHAPTER 15 · Spinlock은 wait time을 coherence traffic으로 바꿀 수 있다

여러 waiter가 같은 lock word를 반복 load/CAS하면 lock holder가 release할 때 많은 cache request가 동시에 발생할 수 있다. Test-and-test-and-set, queue lock은 shared polling/ownership handoff pattern을 바꿔 traffic을 줄인다.

Short critical section이어도 core 수가 많으면 coherence cost가 lock body보다 클 수 있다.

---

## CHAPTER 16 · Ticket lock은 fairness를 얻지만 shared counters에 pressure를 만든다

Ticket lock은 next ticket과 owner ticket을 사용해 FIFO fairness를 제공할 수 있다. 모든 waiter가 owner field를 polling하면 read sharing은 가능하지만 release 때 invalidation/update가 전체 waiter cache에 전달된다.

MCS류 queue lock은 각 waiter가 local node를 polling하도록 만들어 large-core scalability를 높일 수 있다.

---

## CHAPTER 17 · Producer-consumer queue도 head/tail placement에 따라 coherence 비용이 달라진다

Single producer가 tail을 쓰고 single consumer가 head를 쓰는 ring buffer에서 head/tail이 같은 cache line에 있으면 불필요한 ownership bounce가 발생할 수 있다. Data slot과 index publication의 memory-ordering edge도 필요하다.

Queue algorithm 검토는 correctness proof와 cache-line topology를 함께 본다.

---

## CHAPTER 18 · Lock-free algorithm은 lock을 없애도 coherence를 없애지 않는다

CAS loop가 반복 실패하면 lock convoy 대신 RMW retry traffic이 interconnect를 포화시킬 수 있다. Lock-free progress guarantee와 scalability는 별개다. Contention이 높은 shared stack/queue는 elimination/sharding 같은 algorithmic change가 필요할 수 있다.

Atomic retry count와 cache-to-cache transfer를 함께 측정한다.

---

## CHAPTER 19 · Read-mostly data는 immutable snapshot과 RCU가 coherence에 유리할 수 있다

Reader가 shared line을 write하지 않으면 여러 core가 local shared copy를 유지할 수 있다. Writer가 copy-on-update 후 pointer 하나를 publish하면 large data structure의 write invalidation을 줄일 수 있다.

RCU가 read-heavy workload에서 빠른 이유는 lock absence뿐 아니라 read-side shared state를 거의 수정하지 않는 access pattern에도 있다.

---

## CHAPTER 20 · NUMA coherence는 socket 간 interconnect hop을 추가한다

다른 socket의 cache/memory node에 있는 line을 request하면 socket interconnect를 통과해야 한다. Remote modified line transfer는 local LLC miss보다 더 긴 latency와 inter-socket bandwidth를 소비할 수 있다.

Thread migration이 data home/owner와 어긋나면 coherence traffic이 socket을 왕복한다. NUMA placement와 lock owner locality를 같이 본다.

---

## CHAPTER 21 · Home agent/directory placement가 line path를 결정할 수 있다

Many-core/NUMA coherence는 physical address의 home node/directory가 request routing과 sharer tracking을 담당할 수 있다. Requester와 current owner가 가까워도 home lookup이 다른 hop을 요구할 수 있다.

Interconnect topology를 모르면 remote-cache event를 단순 DRAM latency로 해석하게 된다.

---

## CHAPTER 22 · Interconnect bandwidth도 saturation과 queueing을 가진다

Cache-coherence request, DRAM traffic, DMA가 shared fabric bandwidth를 사용하면 core utilization이 낮아도 interconnect가 병목이 될 수 있다. More cores가 shared line을 ping-pong하면 useful application traffic보다 coherence metadata/data transfer가 fabric을 채울 수 있다.

Socket/link bandwidth counter와 queue delay를 performance incident에 포함한다.

---

## CHAPTER 23 · Coherence state transition은 performance counter로 간접 관찰할 수 있다

일부 PMU는 HITM(cache-to-cache modified line hit), snoop response, remote/local cache hit 같은 event를 제공한다. Event semantics는 microarchitecture-specific이므로 exact CPU documentation을 확인해야 한다.

False sharing diagnosis에서는 source-level memory access와 HITM address sampling을 연결하는 tool이 유용하다.

---

## CHAPTER 24 · Address sampling은 hot cache line을 source object로 연결한다

Hardware load/store sampling이 memory address와 latency를 기록하면 어떤 cache line에서 remote/hit-modified latency가 높은지 찾을 수 있다. Symbol/type information을 이용해 line이 어느 struct field에 해당하는지 mapping한다.

Allocator reuse 때문에 같은 address가 시간에 따라 다른 object일 수 있으므로 capture window와 allocation lifetime을 맞춘다.

---

## CHAPTER 25 · Padding은 false sharing을 고치지만 cache footprint를 키운다

Each counter를 cache-line aligned로 만들면 bounce를 줄이지만 thousands of counters가 있을 때 memory footprint와 cache capacity miss가 증가한다. Read-side scanning도 더 많은 line을 가져와야 한다.

Padding은 measurement로 hot writer field에만 적용하고 cold data는 packed layout을 유지할 수 있다.

---

## CHAPTER 26 · Per-core sharding은 write contention을 aggregation cost로 교환한다

Global counter 대신 CPU별 local counter를 update하면 hot-line ownership을 제거할 수 있다. Total을 읽을 때 모든 shard를 aggregate해야 하므로 read cost와 exactness delay가 증가한다.

Metrics counter처럼 frequent write/rare read에는 적합하고 exact transaction balance처럼 immediate global invariant가 필요한 state에는 부적합할 수 있다.

---

## CHAPTER 27 · Core migration은 private-cache locality뿐 아니라 ownership history를 바꾼다

Writer thread가 CPU를 자주 옮기면 자신이 최근 수정한 line ownership도 새 core로 이동해야 할 수 있다. Scheduler load balancing이 CPU fairness를 높이면서 coherence traffic을 늘릴 수 있다.

Hot lock owner/allocator arena thread는 affinity experiment로 migration cost를 검증할 수 있지만 hard pinning의 imbalance 위험도 본다.

---

## CHAPTER 28 · Coherence benchmark는 logical operation당 line transfer를 측정해야 한다

Raw operations/sec만 비교하면 faster algorithm이 더 많은 interconnect traffic을 생성해 다른 tenant를 방해하는 효과를 놓친다. Atomic increment 1회당 HITM/remote transfer, queue operation당 cache-line invalidation을 측정하면 scalability ceiling을 예측할 수 있다.

Single-thread benchmark는 coherence protocol의 핵심 cost를 거의 드러내지 않는다.

---

## CHAPTER 29 · Coherence incident는 state ownership path로 복원한다

Shared data가 느릴 때 `(누가 읽음 → 누가 write ownership 획득 → 어떤 sharer가 invalidated → 다음 requester가 어디서 data를 받음)`을 line 단위 timeline으로 생각한다. Lock profile, scheduler CPU, address sample, PMU event를 결합한다.

`cache miss 많음`보다 **왜 line이 local cache에 머물지 못했는가**가 root cause 질문이다.

---

## CHAPTER 30 · Coherence 설계의 최종 계약은 sharing pattern·ownership transfer·topology·fairness다

1. **Sharing pattern** — read-only, single-writer, multi-writer 중 무엇인가.
2. **Ownership transfer** — logical operation마다 cache line이 몇 core를 이동하는가.
3. **Topology** — same core/cluster/socket/NUMA node 중 어느 interconnect를 건너는가.
4. **Fairness** — hot sharer가 fabric/lock을 독점해 다른 workload를 굶기지 않는가.

멀티코어 scalability는 thread 수를 늘리는 문제가 아니라 **shared-state ownership 이동을 줄이는 data/algorithm design 문제**다.
