# PART 31 · DRAM Internals — channels, banks, row buffers, refresh, memory-controller scheduling

메모리 latency를 `RAM이 느리다`로 요약하면 cache miss 이후 실제로 어떤 일이 일어나는지 설명할 수 없다. DRAM은 channel·rank·bank·row·column 구조를 가지고, bank 안에서 row를 activate해 row buffer에 올린 뒤 column access를 수행한다. Memory controller는 여러 core의 request를 queue에 모아 row locality·fairness·timing constraint를 고려해 command를 scheduling한다. 같은 cache-miss 수라도 address mapping과 access order에 따라 latency·bandwidth가 달라질 수 있다.

---

## CHAPTER 01 · DRAM access는 단일 read command가 아니라 command sequence다

Bank에 원하는 row가 이미 active한지에 따라 access path가 달라진다. 다른 row가 열려 있다면 precharge로 기존 row를 닫고 새 row를 activate한 뒤 column read/write를 수행해야 한다. Row hit는 이미 active row에서 column command만 필요해 더 빠를 수 있다.

Memory latency 분석은 byte address만 보지 않고 bank/row state까지 고려한다.

---

## CHAPTER 02 · Row buffer는 cache와 비슷해 보여도 replacement semantics가 다르다

Activated row의 data는 sense amplifier/row buffer에 놓이고 이후 같은 row access가 빠르게 처리될 수 있다. 하지만 CPU cache처럼 arbitrary line set을 장기간 저장하는 structure가 아니다. Bank마다 active row가 제한되고 다른 row activate가 이전 row state를 바꾼다.

Row locality를 CPU cache hit와 같은 metric으로 합치지 않는다. 두 계층은 independent locality를 가진다.

---

## CHAPTER 03 · Channel은 data-transfer bandwidth domain이다

여러 memory channel을 사용하면 controller가 독립적인 command/data path를 병렬화할 수 있다. Physical address mapping이 channel 사이에 traffic을 균등하게 분산하지 못하면 한 channel이 포화되고 다른 channel은 idle할 수 있다.

Bandwidth benchmark는 total GB/s뿐 아니라 channel distribution과 controller queue occupancy를 확인해야 한다.

---

## CHAPTER 04 · Rank와 bank parallelism은 같은 channel 안에서도 concurrency를 만든다

Rank는 여러 DRAM device가 함께 data width를 구성하는 단위가 될 수 있고 bank는 독립 row state를 가진다. Different bank access는 timing constraint 범위에서 overlap될 수 있어 bank-level parallelism을 제공한다.

Random access workload가 항상 serial이라는 결론은 틀리다. Address distribution이 bank parallelism을 얼마나 활용하는지가 중요하다.

---

## CHAPTER 05 · Physical address bit mapping이 channel/bank/row locality를 결정한다

Memory controller는 physical address bit 일부를 channel, bank, row, column selection에 mapping한다. 특정 stride가 같은 bank의 다른 row만 반복해서 건드리면 row conflict가 많아질 수 있고 다른 mapping에서는 channel parallelism을 만들 수 있다.

Application virtual address stride와 DRAM physical mapping 사이에는 page translation도 있으므로 단순 pointer arithmetic만으로 bank를 단정하지 않는다.

---

## CHAPTER 06 · Row hit, row miss, row conflict는 service time distribution을 다르게 만든다

Row hit는 active row를 재사용하고 row miss는 idle bank에 row activate가 필요하며 row conflict는 기존 row precharge 후 새 row activate가 필요할 수 있다. Controller scheduling policy는 row hit를 우선해 throughput을 높이면서 오래 기다리는 request starvation을 막아야 한다.

Average memory latency보다 row-hit ratio와 tail queueing을 함께 본다.

---

## CHAPTER 07 · FR-FCFS류 scheduling은 locality와 fairness를 교환한다

Ready command를 먼저, row-hit처럼 빠르게 처리 가능한 request를 우선하면 throughput이 좋아질 수 있다. 그러나 한 row를 계속 hit하는 stream이 다른 row request를 오래 굶길 수 있다. Controller는 age/fairness policy를 추가할 수 있다.

Memory controller도 scheduler이므로 PART 23과 같은 fairness-vs-throughput 문제가 반복된다.

---

## CHAPTER 08 · DRAM timing parameter는 command 사이 최소 간격을 정의한다

Activate-to-read, precharge, row-cycle, column-to-column 같은 timing constraint는 cell circuit이 안정적으로 동작하기 위한 minimum interval을 정의한다. Memory frequency가 높아져 cycle time이 짧아지면 timing value의 cycle count와 real nanosecond를 구분해야 한다.

Marketing CAS latency 숫자 하나만으로 end-to-end memory latency를 계산할 수 없다.

---

## CHAPTER 09 · CAS latency는 cache miss 전체 latency가 아니다

CPU load miss는 address translation, LLC lookup, memory-controller queue, activate/precharge, data transfer, cache fill을 거친다. CAS latency는 이 path의 한 일부다. Queueing과 row conflict가 크면 nominal timing보다 훨씬 긴 latency가 발생한다.

System benchmark는 memory-level parallelism과 outstanding miss를 포함한다.

---

## CHAPTER 10 · Burst transfer는 한 command가 여러 data beat를 전달한다

DRAM interface는 wide burst transfer를 사용해 command overhead를 amortize한다. CPU cache line fill은 여러 beat에 걸쳐 전달될 수 있다. Small byte load도 memory system에서는 cache-line granularity traffic을 만들 수 있다.

Sparse random byte access가 useful bytes 대비 많은 DRAM bandwidth를 소비하는 이유다.

---

## CHAPTER 11 · Read/write bus turnaround가 mixed workload throughput을 낮출 수 있다

Shared data bus에서 read와 write 방향을 전환할 때 timing gap이 필요할 수 있다. Controller는 write request를 batch해 bus turnaround 횟수를 줄일 수 있다. 이때 read latency와 write draining 사이 trade-off가 생긴다.

Write-heavy phase가 시작되면 read p99가 악화되는 현상을 memory-controller queue에서 찾을 수 있다.

---

## CHAPTER 12 · Write queue는 CPU store retirement와 DRAM persistence를 분리한다

CPU store는 cache/store buffer에 들어간 뒤 application instruction은 계속 진행할 수 있고 dirty cache line이 나중에 memory controller write queue로 내려간다. DRAM write completion은 일반 volatile memory correctness에서 application-visible durability 개념과 다르다.

Store-heavy workload는 writeback traffic으로 read bandwidth를 침범할 수 있다.

---

## CHAPTER 13 · Memory-level parallelism이 latency를 throughput으로 숨길 수 있다

Out-of-order CPU가 독립 cache miss 여러 개를 동시에 issue하면 각 request latency가 높아도 overlap을 통해 instruction throughput을 유지할 수 있다. Pointer chasing은 다음 address가 이전 load result에 의존해 MLP를 만들기 어렵다.

같은 DRAM latency에서도 linked traversal과 streaming array throughput이 크게 다른 이유다.

---

## CHAPTER 14 · Hardware prefetcher는 DRAM queue pressure를 늘릴 수도 있다

Sequential/stride pattern을 예측해 미리 cache line을 요청하면 demand load latency를 숨길 수 있다. Prediction이 틀리면 unused line이 cache와 memory bandwidth를 소비한다. Multiple cores의 prefetch stream이 DRAM queue를 채우면 latency-sensitive random request가 밀릴 수 있다.

Prefetch 효과는 useful hit와 bandwidth amplification을 같이 측정한다.

---

## CHAPTER 15 · Refresh는 data retention을 위해 bank access를 주기적으로 방해한다

DRAM cell charge는 시간이 지나며 약해지므로 row를 주기적으로 refresh해야 한다. Refresh command 동안 일부 bank/rank access가 지연될 수 있다. Capacity가 커지고 temperature가 높아지면 refresh overhead와 policy가 성능에 영향을 줄 수 있다.

Memory latency tail에 periodic pattern이 있으면 refresh와 thermal condition을 조사한다.

---

## CHAPTER 16 · Fine-grained refresh와 per-bank refresh는 pause distribution을 바꾼다

Refresh work를 더 작은 단위로 나누거나 bank별로 수행하면 긴 global pause를 줄이는 대신 command overhead와 scheduling complexity가 달라질 수 있다. 실제 지원 mode는 DRAM generation/controller에 따라 다르다.

Latency-sensitive system은 total refresh bandwidth보다 longest blocked interval이 중요할 수 있다.

---

## CHAPTER 17 · Temperature와 retention은 refresh requirement를 연결한다

DRAM cell retention은 temperature에 영향을 받기 때문에 controller/platform은 thermal state에 따라 refresh rate를 조절할 수 있다. Higher refresh는 reliability를 높이지만 available bandwidth와 power를 소비한다.

PART 21의 thermal policy가 memory subsystem에도 존재한다는 뜻이다.

---

## CHAPTER 18 · ECC DIMM은 data bits 외 redundancy와 controller logic을 추가한다

ECC memory는 PART 22의 error-correcting code를 DRAM path에 적용한다. Memory controller는 data write 시 ECC를 생성하고 read 시 syndrome을 계산해 correctable/uncorrectable error를 처리한다.

ECC check latency와 bandwidth overhead는 system design에 포함되지만 reliability gain과 비교해야 한다. Consumer/enterprise platform의 실제 ECC scope가 다를 수 있다.

---

## CHAPTER 19 · Scrubbing은 DRAM bandwidth의 background consumer다

Patrol scrub가 memory를 주기적으로 읽고 corrected data를 다시 쓰면 error accumulation을 줄일 수 있지만 DRAM command bandwidth를 사용한다. High-load workload와 scrub가 겹치면 tail latency가 변할 수 있다.

Scrub rate와 performance counter를 incident timeline에 포함한다.

---

## CHAPTER 20 · NUMA는 memory-controller locality를 machine topology로 확장한다

Multi-socket system에서는 각 socket가 local memory controller/channel을 갖고 다른 node memory는 interconnect를 통해 접근한다. Remote access는 additional hop·bandwidth contention을 만든다. PART 14의 NUMA page placement가 결국 DRAM controller topology와 연결된다.

CPU affinity와 memory binding을 동시에 관리해야 한다.

---

## CHAPTER 21 · Interleaving은 capacity/bandwidth를 균등화하지만 locality를 희석한다

Physical pages를 여러 NUMA node/channel에 interleave하면 aggregate bandwidth를 높이고 hot spot을 줄일 수 있지만 thread가 한 node에 고정된 경우 remote access 비율이 증가할 수 있다.

Bandwidth-bound shared workload와 latency-bound local workload의 최적 placement가 다르다.

---

## CHAPTER 22 · Memory bandwidth saturation은 CPU utilization 100% 없이 발생할 수 있다

Core가 cache miss를 기다리며 stalled되면 execution unit utilization은 낮아도 DRAM channel은 최대 bandwidth에 도달할 수 있다. Core를 더 추가하면 throughput이 늘지 않고 각 core latency만 악화될 수 있다.

Roofline류 reasoning에서 arithmetic intensity와 memory bandwidth ceiling을 사용해 compute-bound/memory-bound를 구분한다.

---

## CHAPTER 23 · Bandwidth와 latency는 queueing 때문에 비선형 관계를 갖는다

Low load에서는 request가 바로 service되어 latency가 낮지만 bandwidth limit에 가까워질수록 controller queue가 길어져 latency가 급격히 증가한다. Peak bandwidth 90%를 지속하는 것이 latency-sensitive workload에 안전하지 않을 수 있다.

Headroom은 CPU뿐 아니라 memory channel에도 필요하다.

---

## CHAPTER 24 · Fairness가 없으면 noisy memory tenant가 다른 core를 방해한다

한 process가 streaming memory traffic으로 DRAM queue와 shared LLC를 채우면 다른 process가 CPU time을 충분히 받아도 memory service latency가 악화될 수 있다. CPU cgroup quota는 memory-bandwidth interference를 직접 해결하지 않는다.

Memory bandwidth allocation/monitoring hardware가 있는 platform은 tenant isolation에 활용할 수 있다.

---

## CHAPTER 25 · Page coloring과 address placement는 shared cache/channel mapping에 영향을 줄 수 있다

Physical address bit가 cache set과 memory channel/bank를 결정하기 때문에 allocator가 page를 어디에 배치하는지에 따라 conflict pattern이 바뀔 수 있다. General-purpose OS는 대부분 hardware-specific placement를 완전히 control하지 않지만 real-time/HPC system에서는 page coloring류 technique을 사용할 수 있다.

Optimization은 architecture mapping을 확인하고 benchmark로 증명한다.

---

## CHAPTER 26 · Huge page는 TLB 이득과 DRAM locality를 동시에 바꿀 수 있다

Huge page는 translation overhead를 줄이지만 physical allocation이 더 큰 contiguous range를 요구하고 page placement flexibility를 낮춘다. NUMA migration과 memory interleaving granularity도 달라질 수 있다.

THP enable만으로 performance improvement를 가정하지 않고 TLB miss, NUMA locality, fragmentation을 함께 본다.

---

## CHAPTER 27 · DRAM performance counter는 controller-specific semantics를 확인해야 한다

Modern CPU/SoC는 memory read/write bandwidth, CAS command, row-hit/miss, queue occupancy 같은 uncore counter를 제공할 수 있다. Event 이름과 availability는 platform마다 다르다.

CPU core PMU와 memory-controller PMU를 같은 namespace로 섞지 말고 counter source와 scale factor를 기록한다.

---

## CHAPTER 28 · Microbenchmark는 access dependency와 working set을 통제해야 한다

Memory latency를 측정하려면 hardware prefetch와 memory-level parallelism을 막기 위해 randomized pointer chase를 사용하기도 하고 bandwidth를 측정하려면 independent streaming access를 사용한다. 두 benchmark는 서로 다른 property를 측정한다.

Working set이 LLC보다 작은지 DRAM까지 넘어가는지 확인하지 않으면 `RAM latency`라고 보고한 숫자가 cache latency일 수 있다.

---

## CHAPTER 29 · Memory incident는 CPU→cache→controller→DRAM→NUMA path로 분해한다

High load에서 latency가 증가했을 때 LLC miss rate, outstanding miss/MLP, memory bandwidth, controller queue, row hit, remote NUMA traffic을 단계적으로 본다. `RAM 부족`과 `memory bandwidth saturation`은 완전히 다른 문제다.

Capacity metric과 bandwidth/latency metric을 같은 대시보드에서 구분한다.

---

## CHAPTER 30 · DRAM 분석의 최종 계약은 locality·parallelism·timing·refresh·fairness다

1. **Locality** — access가 cache line뿐 아니라 DRAM row/channel mapping에서 어떤 reuse를 만드는가.
2. **Parallelism** — independent miss가 bank/channel을 얼마나 동시에 활용하는가.
3. **Timing** — activate/precharge/transfer constraint와 queue wait가 latency를 어떻게 구성하는가.
4. **Refresh/RAS** — data retention·ECC·scrub가 background work와 reliability를 어떻게 바꾸는가.
5. **Fairness** — 한 core/tenant의 bandwidth가 다른 workload latency를 얼마나 침범하는가.

Memory optimization은 `RAM을 더 빠른 것으로 바꾼다`가 아니라 이 다섯 축에서 bottleneck을 증명하고 address/layout/scheduling을 조정하는 작업이다.
