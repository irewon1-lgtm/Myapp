# PART 31 · DRAM Internals — channels, banks, row buffers, refresh

DRAM 성능은 `메모리 접근이 느리다`는 한 문장으로 설명되지 않는다. command sequence, row-buffer state, bank parallelism, controller scheduling, refresh, channel saturation이 동시에 latency를 만든다. CPU cache miss 뒤의 시간을 이해하려면 **address가 어느 channel·rank·bank·row로 매핑되고, controller가 어떤 순서로 command를 발행했는지**까지 내려가야 한다.

---

## CHAPTER 01 · DRAM command sequence는 activate·read/write·precharge 상태 전이다

DRAM access는 임의 byte를 즉시 읽는 동작이 아니라 row를 activate해 row buffer에 올리고 column read/write를 수행한 뒤 필요하면 precharge해 다음 row를 준비하는 command sequence다. 이미 열린 row와 같은 row를 접근하면 일부 단계를 피할 수 있지만 다른 row라면 row conflict가 발생한다.

따라서 동일한 byte 수를 읽어도 address pattern에 따라 service time이 달라진다. random pointer chasing과 sequential scan의 차이는 CPU cache뿐 아니라 DRAM row locality에서도 나타날 수 있다.

memory-controller trace나 uncore counter에서 row hit/miss/conflict와 command occupancy를 본다. bandwidth만 측정하지 말고 동일 load에서 row state가 latency distribution을 어떻게 바꾸는지 확인한다.

---

## CHAPTER 02 · row buffer는 locality를 활용하는 DRAM 내부 cache 역할을 한다

한 row가 activate되면 많은 bit가 sense amplifier/row buffer에 올라오고 이후 같은 row의 다른 column 접근은 더 짧은 command path를 사용할 수 있다. row-buffer hit가 많으면 effective latency와 command 효율이 좋아질 수 있다.

반대로 여러 stream이 같은 bank의 서로 다른 row를 교대로 접근하면 row가 계속 열리고 닫히며 conflict가 증가한다. application thread가 독립적이어도 physical address mapping 때문에 같은 bank를 경쟁할 수 있다.

row hit rate를 workload phase와 연결한다. data layout이나 page placement를 바꾼 뒤 row locality가 실제로 개선됐는지, 단순 cache 효과와 구분해 측정한다.

---

## CHAPTER 03 · channel은 독립 memory traffic의 큰 병렬 단위다

memory channel은 controller와 DIMM 사이 독립 data/command path를 제공해 여러 channel에 traffic을 분산하면 aggregate bandwidth를 높일 수 있다. channel 수가 많아도 address mapping이나 NUMA placement가 한 channel에 hot data를 몰면 전체 capacity를 활용하지 못한다.

CPU utilization이 낮고 total bandwidth도 이론 peak보다 낮지만 특정 channel만 saturation될 수 있다. 평균 system counter는 이런 imbalance를 숨긴다.

channel별 read/write bytes와 queue occupancy를 수집한다. page allocation, thread placement, data sharding 변경이 traffic을 실제로 분산했는지 검증한다.

---

## CHAPTER 04 · rank와 bank는 command-level parallelism을 만든다

DIMM은 rank와 bank 구조를 사용해 여러 memory operation을 겹쳐 처리할 수 있다. 한 bank가 activate/precharge timing을 기다리는 동안 다른 bank의 request를 service할 수 있어 controller가 충분한 independent request를 가지고 있으면 latency를 숨길 수 있다.

하지만 같은 bank에 집중된 workload는 다른 bank가 idle이어도 직렬화된다. bank conflict를 CPU thread contention과 혼동하지 않는다.

bank-level parallelism과 queue depth를 함께 본다. address stride를 바꾼 benchmark로 bank mapping alias가 throughput을 제한하는지 확인한다.

---

## CHAPTER 05 · physical address mapping이 channel·bank·row 선택을 결정한다

memory controller는 physical address bit의 일부를 channel, rank, bank, row, column 선택에 사용하며 실제 mapping은 platform마다 다를 수 있다. 연속 physical page가 단순히 연속 channel에 배치된다고 가정하면 안 된다.

특정 stride가 mapping bit와 맞물리면 모든 access가 같은 bank로 몰리는 pathological pattern이 생길 수 있다. virtual address만 보고는 이 현상을 직접 판단하기 어렵다.

platform mapping 문서와 physical-page information을 가능한 범위에서 활용한다. stride sweep과 uncore counter를 결합해 반복 가능한 conflict pattern을 찾는다.

---

## CHAPTER 06 · open/closed row policy는 future locality 예측을 포함한다

controller가 access 후 row를 계속 열어 둘지 바로 precharge할지 결정하는 정책은 다음 access가 같은 row일 가능성과 다른 row request 대기 여부를 반영한다. open-page policy는 locality가 높을 때 유리하고 random workload에서는 다른 request를 방해할 수 있다.

정책은 workload에 따라 dynamic하게 바뀔 수 있어 application이 고정 latency를 기대하면 안 된다. tail spike가 row-policy 변화와 연결될 수도 있다.

row state와 queue request pattern을 함께 분석한다. synthetic row-hit benchmark와 real mixed workload를 구분해 controller behavior를 이해한다.

---

## CHAPTER 07 · memory controller scheduling은 throughput과 fairness를 절충한다

controller는 여러 bank/channel의 pending request 중 어떤 command를 먼저 보낼지 선택한다. row hit를 우선하면 throughput이 좋아질 수 있지만 계속 hit를 만드는 stream이 다른 row request를 오래 굶길 수 있다. read와 write urgency도 scheduling에 영향을 준다.

따라서 평균 bandwidth가 높아도 일부 core/request의 latency는 크게 악화될 수 있다. multi-tenant memory workload에서 fairness가 중요한 이유다.

request class별 latency와 bank queue age를 본다. controller tuning은 peak bandwidth뿐 아니라 p99 wait와 starvation을 함께 평가한다.

---

## CHAPTER 08 · DRAM timing parameter는 command 사이 최소 간격을 정의한다

tRCD, tRP, tRAS 같은 timing은 activate, read/write, precharge 사이 필요한 최소 delay를 표현한다. 이 값은 clock cycle과 physical cell behavior에 연결되며 여러 timing constraint가 동시에 적용된다.

'CAS latency' 숫자 하나로 실제 memory access latency를 설명할 수 없는 이유가 여기에 있다. queueing과 row miss, refresh가 추가된다.

memory frequency와 timing 값을 실제 ns 단위로 환산해 비교한다. firmware overclock 설정이 안정성과 ECC error에 미치는 영향도 함께 본다.

---

## CHAPTER 09 · CAS latency는 전체 load-to-use latency의 일부다

CAS latency는 column command 후 data가 나오는 특정 구간을 가리키며 CPU load가 DRAM miss로 끝날 때의 전체 latency는 address translation, cache hierarchy, controller queue, activate, bus transfer까지 포함한다.

제품 스펙의 CL 값만 비교해 application 성능을 예측하면 틀릴 수 있다. 높은 data rate는 cycle time이 짧아 CL 숫자가 커도 실제 ns는 비슷할 수 있다.

load latency microbenchmark와 uncore command counter를 결합한다. measured latency가 timing theoretical minimum보다 얼마나 queueing을 포함하는지 분리한다.

---

## CHAPTER 10 · burst transfer는 한 command에서 연속 data beat를 전송한다

DRAM interface는 cache line 같은 연속 data를 burst로 전송해 command/address overhead를 amortize한다. CPU cache-line 크기와 memory bus burst가 잘 맞으면 bandwidth 효율이 높아질 수 있다.

작은 random access라도 실제 bus에서는 더 큰 단위가 이동할 수 있어 useful-byte 대비 traffic이 늘어난다. sparse access가 bandwidth를 예상보다 빨리 포화시키는 이유가 될 수 있다.

requested byte와 memory-controller transferred byte를 비교한다. access layout 변경이 실제 bus traffic 감소로 이어지는지 검증한다.

---

## CHAPTER 11 · read↔write turnaround는 bus direction change 비용을 만든다

DRAM data bus는 read와 write 방향을 전환할 때 electrical/timing gap이 필요할 수 있다. controller는 request를 어느 정도 batch해 direction switch 횟수를 줄이려 한다. mixed 50/50 workload가 pure read/write보다 효율이 낮을 수 있다.

write batch가 길어지면 read latency가 튈 수 있어 throughput과 response time이 충돌한다.

read/write queue와 turnaround event를 측정한다. mixed workload에서 p99 read latency가 write burst와 동기화되는지 확인한다.

---

## CHAPTER 12 · write queue는 write를 모아 bus 효율을 높이지만 read를 지연시킬 수 있다

CPU cache의 dirty eviction과 DMA write가 controller write queue에 쌓이면 controller가 write draining phase를 수행할 수 있다. queue가 임계점을 넘을 때 여러 write를 연속 처리해 turnaround overhead를 줄인다.

대량 writeback이 시작되면 latency-sensitive read가 뒤로 밀릴 수 있다. application에서는 갑작스러운 memory latency spike로 보일 수 있다.

write queue occupancy, read latency, dirty eviction을 같은 시간축에 둔다. batching threshold 변경은 bandwidth와 tail read를 함께 평가한다.

---

## CHAPTER 13 · memory-level parallelism은 여러 miss를 동시에 outstanding하게 만든다

out-of-order CPU와 nonblocking cache는 independent cache miss를 여러 개 동시에 발행해 한 access latency를 다른 access와 overlap할 수 있다. pointer chasing처럼 다음 address가 이전 load 결과에 의존하면 MLP가 낮아지고 DRAM latency가 그대로 critical path에 들어온다.

따라서 동일 miss rate라도 MLP가 높은 scan과 낮은 linked list는 성능이 크게 다르다.

outstanding miss, load stall cycle, bandwidth를 함께 측정한다. data structure 변경이 dependency chain을 줄였는지 generated code와 counter로 검증한다.

---

## CHAPTER 14 · prefetcher는 future access를 예측해 memory latency를 숨긴다

hardware prefetcher는 stride나 stream pattern을 감지해 demand load 전에 cache line을 가져온다. 예측이 맞으면 latency를 숨길 수 있지만 불필요한 line을 가져오면 bandwidth와 cache capacity를 낭비한다.

random access나 여러 interleaved stream은 accuracy를 낮출 수 있다. benchmark가 prefetch-friendly인지 아닌지에 따라 memory latency 결과가 달라진다.

prefetch request, useful hit, bandwidth를 비교한다. data layout optimization이 prefetch accuracy를 높였는지 확인한다.

---

## CHAPTER 15 · refresh는 cell charge를 주기적으로 복원한다

DRAM cell은 charge를 잃기 때문에 일정 시간 안에 row를 refresh해야 data가 유지된다. refresh command 동안 일부 bank/rank access가 제한되어 latency와 bandwidth에 영향을 줄 수 있다.

대용량 memory와 높은 온도에서 refresh overhead가 커질 수 있다. refresh를 단순 background 비용으로 무시하면 periodic latency spike를 설명하지 못한다.

refresh event와 memory latency를 correlation한다. sustained latency test에서 refresh periodicity와 p99 pattern이 맞는지 본다.

---

## CHAPTER 16 · refresh granularity는 pause 길이와 빈도를 바꾼다

all-bank refresh처럼 넓은 범위를 한 번에 처리하는 방식과 더 세분화된 refresh는 한 번의 service interruption과 command frequency 사이 trade-off를 가진다. 세밀한 refresh가 peak pause를 줄일 수 있지만 scheduling complexity가 늘어난다.

controller policy와 DRAM generation에 따라 실제 behavior가 다르므로 generic assumption을 피한다.

refresh mode, command count, latency distribution을 측정한다. 변경 전후 worst-case access delay가 개선되는지 확인한다.

---

## CHAPTER 17 · temperature는 retention과 refresh requirement에 영향을 준다

DRAM retention은 temperature의 영향을 받으며 platform은 높은 온도에서 refresh rate를 조정할 수 있다. CPU/GPU heat가 memory subsystem timing과 reliability에 간접 영향을 줄 수 있다.

thermal benchmark에서 CPU frequency만 보면서 memory refresh 변화는 놓칠 수 있다. 모바일 SoC에서는 enclosure thermal state가 여러 subsystem을 함께 바꾼다.

temperature zone, refresh mode, bandwidth/latency를 같은 timeline에 둔다. hot/cold condition을 분리해 benchmark한다.

---

## CHAPTER 18 · ECC DRAM은 correctable error를 service failure 전에 잡는다

ECC memory는 redundant bit를 사용해 일부 bit error를 검출·수정한다. correction이 성공해도 error rate 증가는 DIMM/channel degradation signal이 될 수 있다. user-visible crash가 없다는 이유로 telemetry를 버리면 안 된다.

uncorrectable error는 page poisoning, process kill, machine check 같은 containment로 이어질 수 있다.

EDAC/controller counter를 DIMM topology와 연결한다. corrected error trend와 workload/temperature 상관관계를 장기적으로 본다.

---

## CHAPTER 19 · memory scrubbing은 reliability와 bandwidth를 교환한다

scrubber는 background에서 memory를 읽어 ECC correction을 수행해 cold page의 latent error를 조기에 발견한다. 너무 느리면 exposure window가 길고 너무 빠르면 application과 memory bandwidth를 경쟁한다.

scrub traffic이 peak workload와 겹치면 latency SLO를 해칠 수 있다. corrected error rate가 높은 system은 더 적극적인 policy가 필요할 수도 있다.

scrub bandwidth, corrected error, application p99를 함께 측정한다. safety를 위해 scrub을 완전히 끄는 최적화는 피한다.

---

## CHAPTER 20 · NUMA controller placement는 CPU와 memory path를 함께 결정한다

multi-socket system에서 memory controller가 socket/node에 연결돼 local access와 remote interconnect access의 비용이 다르다. thread를 한 node에 pin해도 page가 다른 node에 있으면 memory latency는 높게 유지된다.

VM, container, allocator policy가 physical page placement를 바꿀 수 있다. CPU utilization만 보고 locality를 판단하지 않는다.

per-node bandwidth와 remote access counter를 수집한다. thread/page placement 변경이 실제 remote traffic 감소로 이어지는지 확인한다.

---

## CHAPTER 21 · interleaving은 traffic을 여러 channel에 분산시키는 address mapping 전략이다

controller는 address bit를 사용해 cache line이나 page를 여러 channel/bank에 interleave할 수 있다. 균등한 streaming workload에서 bandwidth를 넓게 활용하지만 특정 stride가 mapping과 충돌하면 imbalance가 생길 수 있다.

application의 logical sharding과 hardware interleaving이 우연히 같은 channel을 집중시키기도 한다.

stride sweep과 channel counter로 alias pattern을 찾는다. allocator/page coloring 같은 상위 정책과 mapping interaction을 검토한다.

---

## CHAPTER 22 · bandwidth saturation은 queueing delay를 비선형적으로 키운다

memory bandwidth가 channel capacity에 가까워지면 새 request가 controller queue에서 기다리는 시간이 빠르게 증가한다. CPU core를 더 추가하면 compute throughput 대신 memory queue만 늘어날 수 있다.

memory-bound service는 CPU utilization이 100%가 아니어도 이미 saturation 상태일 수 있다.

bandwidth와 average뿐 아니라 p95/p99 load latency를 같이 본다. concurrency sweep에서 throughput이 평탄해지는 지점을 operational limit로 사용한다.

---

## CHAPTER 23 · memory latency에는 service time과 controller queue wait가 섞여 있다

DRAM timing이 그대로 access latency가 되는 것은 아니다. 앞선 request와 refresh, read/write turnaround 때문에 controller에서 wait한 시간까지 더해진다. 같은 DIMM에서도 load가 높을수록 latency가 커지는 이유다.

평균 ns만 보면 queue burst를 숨긴다. tail request가 어떤 bank와 queue 상태를 만났는지 중요하다.

latency distribution과 queue occupancy를 같은 sampling window에서 본다. hardware timing을 바꾸기 전에 workload-induced queueing을 분리한다.

---

## CHAPTER 24 · memory fairness는 bandwidth-heavy workload가 latency-sensitive workload를 굶기지 않게 한다

controller가 row hit나 throughput만 최우선하면 streaming task가 많은 command를 소비해 random latency-sensitive request를 오래 지연시킬 수 있다. multi-tenant server에서는 memory controller도 shared service center다.

CPU cgroup이 공정해도 memory bandwidth는 별도 contention을 만들 수 있다. task priority와 memory priority가 자동 연결되지 않는 platform도 있다.

workload별 bandwidth와 p99 latency를 측정한다. co-location test로 noisy neighbor의 interference를 정량화한다.

---

## CHAPTER 25 · page coloring은 physical-address index 충돌을 제어하려는 배치 기법이다

cache set이나 memory bank 선택이 physical address bit와 연결될 때 OS/allocator가 page를 특정 color 집합에 배치해 interference를 줄이는 기법을 사용할 수 있다. 실제 mapping을 알아야 효과가 있으며 hardware 세대가 바뀌면 가정도 바뀐다.

과도한 partition은 usable capacity를 줄이고 fragmentation을 만들 수 있다. 일반 workload에서는 scheduler/NUMA placement가 더 큰 효과를 낼 수도 있다.

적용 전 conflict counter와 address mapping evidence를 확보한다. microbenchmark뿐 아니라 end-to-end workload에서 이득을 검증한다.

---

## CHAPTER 26 · huge page는 translation 이득과 DRAM locality를 함께 바꿀 수 있다

huge page는 TLB reach를 늘리지만 physical allocation이 큰 단위로 이루어져 NUMA placement와 address mapping에도 영향을 줄 수 있다. contiguous allocation이 특정 channel/bank distribution을 바꾸는 경우 memory traffic 특성이 달라질 수 있다.

huge page 사용률만 높인다고 성능이 보장되지 않는다. fragmentation과 remote placement cost를 함께 본다.

TLB miss, channel balance, NUMA traffic을 동시에 측정한다. page size 전환 전후 같은 workload를 반복한다.

---

## CHAPTER 27 · uncore counter는 memory controller와 interconnect activity를 직접 관찰한다

CPU core PMU만으로는 DRAM channel bandwidth, controller queue, memory-controller event를 충분히 볼 수 없다. uncore PMU는 core 밖 memory/interconnect subsystem의 traffic을 측정하는 데 사용된다.

counter 의미와 unit은 CPU model별로 다르고 여러 socket의 instance를 합산할 때 topology를 알아야 한다.

CPU model과 raw event definition을 결과에 포함한다. application metric과 같은 timestamp window로 수집해 causal relation을 분석한다.

---

## CHAPTER 28 · memory benchmark는 latency·bandwidth·MLP를 분리해 설계한다

pointer chase는 dependent load latency를, streaming copy는 bandwidth를, multi-stream access는 MLP와 controller parallelism을 더 많이 측정한다. 하나의 memory benchmark 숫자로 subsystem 전체를 평가하면 안 된다.

working set이 cache보다 작은지, NUMA page가 어디에 있는지, prefetcher가 켜졌는지 결과를 크게 바꾼다.

size/stride/concurrency sweep을 사용한다. benchmark 환경과 mapping을 함께 보존해 재현성을 확보한다.

---

## CHAPTER 29 · memory incident는 CPU stall에서 DRAM queue까지 evidence를 연결한다

production에서 latency가 증가했을 때 먼저 on-CPU/off-CPU와 cache miss를 보고, memory-bound 의심이 생기면 bandwidth, NUMA remote access, row conflict, controller saturation까지 내려간다. 한 counter만 높은 것을 root cause로 선언하지 않는다.

traffic 증가나 deploy로 working set이 바뀌었는지도 함께 본다. hardware error와 performance saturation은 다른 incident class다.

request latency와 memory counter를 동일 time window에 정렬한다. 정상 host와 failing host의 topology와 DIMM configuration도 비교한다.

---

## CHAPTER 30 · DRAM contract는 locality, capacity, reliability를 함께 정의한다

memory subsystem을 사용하는 software는 latency가 constant라고 가정하지 않고 cache miss, NUMA, bandwidth saturation, refresh, ECC event가 distribution을 바꾼다는 것을 받아들여야 한다. capacity가 남았다는 사실과 low-latency access가 가능하다는 사실은 다르다.

설계 시 working set, expected bandwidth, NUMA placement, error handling을 명시한다. memory pressure와 hardware fault를 별도 recovery path로 둔다.

CLEAN 검증은 dependent-latency, bandwidth saturation, NUMA imbalance, ECC/scrub을 서로 다른 failure mode로 확인한다. 모든 조건에서 workload invariant와 SLO가 유지되는지를 최종 기준으로 삼는다.
