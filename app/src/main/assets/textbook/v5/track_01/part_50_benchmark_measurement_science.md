# PART 50 · Benchmark and Measurement Science — clocks, warmup, distributions, counter bias

Benchmark는 코드를 한 번 재고 빠르다/느리다를 판정하는 절차가 아니다. 어떤 질문을 답하려는지 정의하고, timer와 workload를 통제하고, JIT·cache·DVFS·thermal·allocator state를 구분하며, latency distribution과 measurement overhead를 해석해야 한다. 잘못된 benchmark는 정확한 숫자를 주면서도 틀린 결론을 만들 수 있다. 이 PART는 microbenchmark부터 load test, PMU, regression threshold까지 **측정 설계 자체를 하나의 engineering artifact**로 다룬다.

---

## CHAPTER 01 · benchmark는 먼저 질문과 비교 대상을 고정해야 한다

측정 전에 “무엇을 더 빠르게 만들려는가”를 한 문장으로 정의한다. Function call latency, end-to-end request latency, sustained throughput, memory footprint는 서로 다른 질문이며 같은 harness로 답하기 어렵다. 질문이 불명확하면 가장 쉽게 측정되는 숫자를 목표로 삼게 된다.

비교 대상도 고정한다. Baseline commit, compiler flags, dataset, hardware, OS 설정 중 하나라도 달라지면 code change만의 효과라고 말하기 어렵다. 여러 요인이 바뀌었다면 factorial experiment처럼 따로 분리하거나 최소한 confounder를 기록한다.

결과에는 hypothesis와 metric을 함께 적는다. “새 parser가 10% 빠르다”보다 “1 KiB JSON decode CPU ns/op가 baseline보다 감소하는지”가 검증 가능하다.

## CHAPTER 02 · microbenchmark와 macrobenchmark는 서로 다른 계층의 병목을 본다

Microbenchmark는 작은 operation을 반복해 instruction·allocation·cache 차이를 정밀하게 보기 좋다. 대신 production의 queueing, network, scheduler, storage를 제외하므로 end-to-end 효과를 직접 보장하지 않는다. Macrobenchmark는 실제 workflow에 가깝지만 noise와 원인 분리가 어렵다.

좋은 성능 작업은 두 수준을 연결한다. Micro에서 원인을 확인하고 macro에서 사용자 영향이 실제로 나타나는지 검증한다. Micro가 30% 개선돼도 전체 request에서 그 operation이 2%라면 end-to-end gain은 작다.

반대로 macro regression이 보이면 profile로 hotspot을 찾은 뒤 작은 reproducer를 만든다. 두 종류의 benchmark를 경쟁 관계가 아니라 진단 단계로 사용한다.

## CHAPTER 03 · timer는 측정하려는 duration보다 충분히 높은 해상도와 안정성을 가져야 한다

아주 짧은 operation을 coarse timer로 재면 많은 sample이 0 또는 같은 값이 되어 quantization error가 커진다. Monotonic clock을 사용하고 timer call 자체의 overhead를 측정한다. Wall clock은 시간 동기화로 움직일 수 있어 duration 측정에 부적합할 수 있다.

CPU cycle counter를 쓰면 더 세밀할 수 있지만 frequency 변화, core migration, serialization semantics를 이해해야 한다. Platform이 제공하는 steady/monotonic source가 어떤 guarantee를 갖는지 확인한다.

한 operation이 timer overhead와 비슷하면 여러 번 묶어 batch duration을 재고 반복 수로 나눈다. 단, batching이 cache·branch state를 바꾸므로 실제 workload와의 차이를 기록한다.

## CHAPTER 04 · JIT warmup은 code quality와 runtime state가 시간에 따라 변하는 현상이다

JIT runtime은 처음에는 interpreter 또는 baseline code로 실행하다 hot method를 최적화할 수 있다. 그래서 초반 iteration과 steady-state iteration의 성능이 다르다. 단순히 첫 10회를 버리는 고정 규칙보다 compilation event와 throughput 안정화를 관찰하는 편이 낫다.

Profile-guided optimization과 deoptimization도 결과를 흔들 수 있다. 입력 분포가 warmup과 measurement 단계에서 다르면 JIT가 잘못된 specialization을 만들거나 measurement 중 재컴파일할 수 있다.

Benchmark artifact에 runtime version, tiering 설정, warmup length를 기록한다. Cold-start가 제품 요구라면 warmup을 제거하지 말고 별도 metric으로 측정한다.

## CHAPTER 05 · cache state는 같은 코드의 memory latency를 크게 바꾼다

L1/L2/LLC에 데이터가 있는 hot-cache run과 DRAM에서 가져오는 cold-cache run은 전혀 다른 질문이다. 작은 dataset을 수백만 번 반복하면 처음 몇 회 이후 거의 모두 cache hit가 되어 production의 큰 working set을 대표하지 못할 수 있다.

Cache를 의도적으로 flush하는 것도 완벽한 해결책은 아니다. Flush 자체가 측정 환경을 바꾸고 hardware prefetcher state를 남길 수 있다. 대신 dataset 크기와 access pattern을 실제 workload에 가깝게 설계한다.

결과를 hot/cold 두 조건으로 나누면 algorithm의 compute cost와 memory sensitivity를 이해하기 쉽다. 어떤 조건을 production에 적용하는지 명시한다.

## CHAPTER 06 · branch predictor state도 반복 benchmark를 지나치게 쉽게 만들 수 있다

같은 input과 branch outcome을 계속 반복하면 predictor가 완벽히 학습해 misprediction이 거의 사라질 수 있다. 실제 workload에서 분기가 데이터에 따라 바뀐다면 benchmark가 지나치게 좋은 결과를 낸다.

Input order를 randomize하거나 production 분포를 재현해 branch entropy를 유지한다. 다만 random generator 비용이 측정 대상에 섞이지 않도록 input을 미리 준비할 수 있다.

PMU branch-miss counter를 함께 보면 code change가 instruction 수를 줄였는지 predictor friendliness를 개선했는지 구분할 수 있다. 평균 ns/op 하나만으로는 원인을 알기 어렵다.

## CHAPTER 07 · DVFS는 workload 시작 후 CPU frequency를 바꾸므로 초반과 후반을 다르게 만든다

Idle 상태의 CPU는 낮은 frequency에서 시작했다가 부하를 감지하면 boost할 수 있다. 반대로 지속 부하에서 power limit에 도달하면 frequency가 낮아질 수 있다. 짧은 benchmark는 boost 구간만 재고 장시간 서비스는 sustained frequency에서 동작할 수 있다.

성능 비교는 governor와 power policy를 고정하거나 최소한 frequency trace를 기록한다. 두 코드가 서로 다른 CPU utilization을 만들면 동일 governor에서도 frequency response가 달라질 수 있다.

“CPU cycles는 줄었는데 wall time이 비슷하다” 같은 결과가 frequency 변화 때문인지 확인한다. Energy도 목표라면 joules/op까지 측정한다.

## CHAPTER 08 · thermal throttling은 benchmark 순서와 지속 시간을 confounder로 만든다

CPU·GPU 온도가 오르면 thermal controller가 frequency나 core availability를 제한할 수 있다. 첫 번째 run이 빠르고 뒤 run이 계속 느려지는 현상을 code cache나 GC 문제로 오해하기 쉽다. 특히 mobile 장치에서는 ambient temperature와 casing 상태도 영향을 준다.

Run 사이 cooldown을 두거나 temperature range를 통제하고, 장시간 steady-state 성능이 목표라면 의도적으로 thermal equilibrium까지 부하를 유지한다.

결과에 peak/average temperature와 throttling event를 남긴다. 동일 binary 비교에서도 열 상태가 다르면 작은 regression 판단을 보류해야 한다.

## CHAPTER 09 · CPU affinity는 migration noise를 줄이지만 topology 조건을 함께 명시해야 한다

Benchmark thread를 특정 CPU에 pin하면 scheduler migration과 cold private cache 영향을 줄일 수 있다. 하지만 그 CPU의 SMT sibling에 다른 task가 실행되면 간섭이 남는다. Pinning이 곧 isolated core를 의미하지 않는다.

NUMA system에서는 benchmark thread와 memory가 같은 node에 있는지도 확인한다. First-touch가 다른 node에서 발생하면 remote memory access를 측정할 수 있다.

Artifact에는 affinity mask, sibling 상태, NUMA placement를 기록한다. 다른 machine에서 CPU 번호만 동일하게 맞추는 것은 재현성이 없다.

## CHAPTER 10 · dead-code elimination은 benchmark가 측정 대상을 통째로 지울 수 있게 한다

Compiler가 계산 결과가 사용되지 않는다고 판단하면 대상 operation을 제거하거나 loop 전체를 상수화할 수 있다. Source에 함수 호출이 있어도 final assembly에는 거의 아무 것도 없을 수 있다. Microbenchmark에서 특히 흔한 오류다.

결과를 observable sink에 연결하고 benchmark framework의 blackhole mechanism을 사용한다. 다만 sink 자체 비용이 대상보다 크지 않은지 확인한다.

최적화된 assembly나 IR을 최소 한 번 검토해 실제 operation이 남아 있는지 확인한다. 지나치게 놀라운 속도 향상은 먼저 benchmark elimination을 의심한다.

## CHAPTER 11 · constant folding은 고정 input을 compile time 결과로 바꿀 수 있다

Benchmark input이 literal이고 함수가 pure하다고 compiler가 알면 계산을 미리 수행해 runtime cost를 제거할 수 있다. Hash, parsing, arithmetic microbenchmark에서 동일 상수를 반복할 때 발생할 수 있다.

Input을 runtime에 준비하거나 여러 value를 사용해 compile-time evaluation을 막는다. 그렇다고 매 iteration마다 random input을 만들면 random generation 비용이 섞이므로 preparation과 measurement를 분리한다.

Assembly diff로 baseline과 candidate가 실제 같은 양의 일을 하는지 확인한다. 서로 다른 workload를 재고 숫자만 비교하지 않는다.

## CHAPTER 12 · harness overhead는 작은 operation에서 전체 측정값의 대부분이 될 수 있다

Timer call, loop increment, virtual dispatch, parameter generation이 대상 function보다 비싸면 measured ns/op는 harness 성능을 반영한다. Empty benchmark를 재서 baseline overhead를 파악하고 batching으로 비율을 낮출 수 있다.

단순히 empty cost를 빼는 방식은 두 경로가 독립적이지 않으면 정확하지 않다. Branch predictor와 pipeline이 달라질 수 있기 때문이다. 따라서 대상 duration을 충분히 키우는 것이 더 안전하다.

Harness version도 artifact에 기록한다. Benchmark framework 업데이트가 measurement protocol을 바꿀 수 있다.

## CHAPTER 13 · allocator state는 latency와 GC pressure를 반복마다 다르게 만든다

Memory allocation benchmark는 free list, arena, thread cache가 warm인지에 따라 결과가 달라진다. Managed runtime에서는 heap occupancy와 GC phase도 큰 영향을 준다. 단순히 같은 byte 수를 할당한다고 동일 조건이 아니다.

Steady-state allocation 성능을 보려면 heap size와 live set을 통제하고, GC event를 기록한다. Cold allocator cost가 필요한 경우 process restart나 cache reset을 별도 시나리오로 만든다.

Candidate가 allocation 수를 줄였는지, allocator 자체가 빨라졌는지 분리한다. bytes allocated/op와 GC pause를 함께 보면 원인이 선명해진다.

## CHAPTER 14 · initialization cache를 measurement에 포함할지 명시해야 한다

Regex compile, class loading, connection setup, dictionary initialization 같은 one-time cost가 첫 호출에 몰릴 수 있다. 서비스 startup을 측정한다면 포함해야 하지만 steady-state request latency에서는 분리해야 한다.

Benchmark setup 단계에서 initialization을 미리 수행하면 steady-state만 잴 수 있다. 반대로 production에서 매 request마다 잘못 초기화한다면 setup으로 빼면 bug를 숨긴다. 측정 경계는 실제 lifecycle과 맞아야 한다.

Cold, warm 두 metric을 제공하면 trade-off를 더 정확히 볼 수 있다. “첫 화면”과 “반복 사용” 요구가 다른 모바일 앱에서 특히 중요하다.

## CHAPTER 15 · coordinated omission은 느린 구간에서 요청 자체를 덜 생성해 tail을 숨긴다

Load generator가 이전 요청이 끝난 뒤 다음 요청을 보내면 server가 느려진 동안 arrival rate도 자동으로 줄어든다. 실제 사용자는 일정한 schedule로 요청했을 수 있는데 test가 backlog를 만들지 않아 latency tail을 과소평가한다.

Open-loop 또는 예정 arrival time을 유지하는 generator를 사용하면 queueing을 포함한 latency를 볼 수 있다. Closed-loop test가 틀린 것은 아니지만 측정하는 시스템 모델이 다르다.

Report에는 offered load와 achieved throughput을 분리한다. 서버가 느려져 throughput이 떨어졌는데 latency가 안정적으로 보인다면 omission 가능성을 확인한다.

## CHAPTER 16 · latency는 평균 하나가 아니라 distribution으로 봐야 한다

Request latency는 queueing, GC, I/O, scheduler 때문에 heavy tail을 가질 수 있다. 평균이 10ms라도 일부 요청이 500ms일 수 있다. p50, p90, p99 같은 percentile과 histogram을 함께 저장한다.

Percentile은 sample population을 명확히 해야 한다. 성공 요청만 포함했는지, timeout은 어떤 값으로 처리했는지에 따라 tail이 달라진다. 실패 요청을 버리면 overload 상황이 지나치게 좋아 보일 수 있다.

Distribution을 버리고 평균만 저장하면 나중에 다른 percentile을 계산할 수 없다. Histogram bucket 또는 raw sample 전략을 workload scale에 맞춰 선택한다.

## CHAPTER 17 · 높은 percentile을 안정적으로 추정하려면 충분한 sample이 필요하다

p99.9를 1,000개 sample로 계산하면 사실상 가장 느린 한두 개 값에 의존한다. 반복 run마다 크게 흔들릴 수 있다. 원하는 percentile이 높을수록 sample 수와 run duration을 늘려야 한다.

독립 sample 가정도 주의한다. Burst나 periodic GC로 latency가 시간적으로 correlated될 수 있다. 단순 sample count보다 여러 주기와 충분한 시간 범위를 포함하는 것이 중요하다.

Report에 sample count를 percentile 옆에 표시한다. 숫자만 소수점까지 정밀하게 쓰면 통계적 신뢰가 과장된다.

## CHAPTER 18 · outlier는 무조건 버릴 noise가 아니라 시스템 behavior일 수 있다

Context switch, page fault, GC, thermal event가 만든 느린 sample은 production에서도 실제로 발생한다. 통계 편의를 위해 상하위 1%를 제거하면 tail latency 문제를 숨길 수 있다. Outlier 제거는 측정 오류가 명확한 경우에만 근거를 남긴다.

대신 outlier를 분류한다. Scheduler delay, I/O stall, fault, background daemon과 timestamp를 맞추면 rare event의 원인을 찾을 수 있다.

회귀 판단에는 robust statistic을 사용할 수 있지만 사용자 경험 metric에서는 tail 자체를 보존한다. 목적에 따라 처리 방식을 구분한다.

## CHAPTER 19 · confidence는 반복 측정의 변동성과 effect size를 함께 본다

한 번의 run에서 3% 빨라졌다고 regression/improvement를 확정하면 noise를 성능 변화로 오인할 수 있다. 여러 independent run을 수행해 분포와 confidence interval을 계산하고 effect size가 noise보다 큰지 본다.

Sample이 skewed하면 단순 normal assumption이 맞지 않을 수 있다. Bootstrap이나 nonparametric 접근을 고려할 수 있다. 중요한 것은 복잡한 통계 이름보다 반복 변동을 숨기지 않는 것이다.

CI gate의 threshold는 historical noise를 기반으로 정한다. 매번 흔들리는 1% 차이를 fail시키면 개발자가 gate를 무시하게 된다.

## CHAPTER 20 · background noise는 benchmark process 밖의 CPU·I/O 활동에서 들어온다

OS update, antivirus, logging daemon, 다른 container가 CPU와 storage를 사용하면 결과가 흔들린다. Shared CI runner에서는 neighbor workload가 특히 큰 confounder가 될 수 있다. 동일 machine이라고 동일 load 상태는 아니다.

Run 전 system load를 기록하고 필요하면 dedicated runner를 사용한다. 단, production이 noisy multi-tenant라면 완전히 isolated benchmark만으로 capacity를 예측해서도 안 된다.

Controlled lab result와 noisy environment result를 분리해 저장한다. 두 숫자를 섞으면 regression threshold가 불필요하게 넓어진다.

## CHAPTER 21 · virtualization noise는 guest가 보지 못하는 host scheduling에서 발생한다

VM의 vCPU가 runnable이어도 host pCPU를 바로 받지 못하면 guest 내부에서는 unexplained stall처럼 보일 수 있다. Steal time과 host oversubscription이 benchmark latency를 흔든다. Cloud instance migration이나 noisy neighbor도 영향을 준다.

성능 gate에 VM을 쓴다면 instance type과 host variation을 고려해 반복한다. Bare metal baseline이 있으면 virtualization overhead를 분리하는 데 도움이 된다.

Guest CPU utilization이 낮다고 host contention이 없다는 뜻은 아니다. Hypervisor가 제공하는 steal/ready metric을 함께 본다.

## CHAPTER 22 · PMU multiplexing은 동시에 너무 많은 counter를 재면 각 counter의 관측 시간을 줄인다

Hardware performance counter 개수는 제한되어 있다. 요청한 event가 slot보다 많으면 kernel이 시간 분할로 multiplex하고 값을 scaling해 추정할 수 있다. 짧은 benchmark에서는 active time이 적어 error가 커질 수 있다.

Counter report에서 enabled time과 running time을 확인한다. 중요한 event는 여러 pass로 나눠 직접 측정하고, 동일 workload가 재현 가능한지 보장한다.

PMU event 의미도 CPU model마다 다를 수 있다. event name이 같아도 정확한 semantic이 다른 경우가 있어 cross-machine 비교에 주의한다.

## CHAPTER 23 · profiler sampling은 짧거나 드문 code path를 편향되게 볼 수 있다

Sampling profiler는 일정 주기마다 stack을 관찰하므로 아주 짧은 function이나 burst 사이의 idle을 놓칠 수 있다. CPU time 비율을 추정하는 데 유용하지만 정확한 call count를 주는 것은 아니다.

Sampling frequency를 높이면 overhead와 perturbation이 증가한다. Wall-clock, CPU, hardware event sampling은 서로 다른 population을 본다. 목표 질문에 맞는 trigger를 선택한다.

Profile 결과를 benchmark timer와 교차 검증한다. “profile에서 안 보인다”가 cost가 0이라는 뜻은 아니다.

## CHAPTER 24 · dataset은 benchmark의 사실상 specification이다

Parser, database, compression 성능은 input distribution에 크게 의존한다. 작은 synthetic data만 쓰면 cache fit과 branch predictability가 production보다 지나치게 좋을 수 있다. Representative dataset을 versioned artifact로 관리한다.

Privacy 때문에 production raw data를 쓸 수 없다면 size, key cardinality, skew, field distribution을 보존한 synthetic generator를 만든다. Generator version과 seed를 기록한다.

Candidate가 특정 dataset에 과적합되지 않도록 여러 workload class를 둔다. 평균 하나로 합치기보다 class별 regression을 본다.

## CHAPTER 25 · throughput test는 offered load와 concurrency를 명시해야 해석할 수 있다

“초당 10만 처리”라는 숫자는 client가 얼마나 요청했는지, 동시성은 얼마인지, latency는 어떤지 없으면 의미가 약하다. Throughput은 load가 증가하다 saturation에서 평평해지고 latency가 급증하는 curve로 보는 것이 좋다.

Concurrency를 무한히 늘리면 queue만 길어져 throughput은 그대로인데 latency가 악화될 수 있다. 시스템이 안정적으로 유지할 수 있는 operating point를 찾는다.

CPU, I/O, queue depth를 함께 기록해 saturation resource를 확인한다. Throughput 숫자만으로 bottleneck을 추측하지 않는다.

## CHAPTER 26 · client가 bottleneck이면 server의 실제 capacity보다 낮은 숫자를 측정한다

Load generator가 request serialization, TLS, response validation에 CPU를 많이 쓰면 server보다 먼저 포화될 수 있다. 한 machine에서 client와 server를 같이 실행하면 CPU·network도 경쟁한다.

Client utilization과 event loop lag를 관찰하고 여러 generator로 분산할 수 있다. 요청 생성률이 목표보다 낮아지는지 확인한다.

Server latency가 낮고 CPU도 여유로운데 throughput이 멈춘다면 client bottleneck을 의심한다. Measurement system도 측정 대상과 같은 rigor로 profile한다.

## CHAPTER 27 · overload benchmark는 정상 throughput보다 degradation behavior를 본다

Capacity를 넘긴 load에서 시스템이 queue를 무한히 키우는지, 빠르게 reject하는지, timeout storm이 생기는지 관찰한다. 목표는 peak 숫자를 높이는 것만이 아니라 overload 후 회복 가능한 behavior를 확인하는 것이다.

Load를 단계적으로 올렸다 내리며 hysteresis와 recovery time을 본다. Overload 종료 후 queue와 memory가 baseline으로 돌아오지 않으면 hidden backlog가 남아 있을 수 있다.

Error rate, shed rate, p99 latency를 throughput과 같은 그래프에 둔다. 성공 throughput만 표시하면 실패를 비용 없이 버린 것처럼 보일 수 있다.

## CHAPTER 28 · benchmark artifact는 결과를 재현할 metadata를 포함해야 한다

Commit SHA, compiler/runtime version, flags, hardware model, kernel, topology, dataset hash, benchmark config를 결과와 함께 저장한다. 숫자 CSV만 남기면 몇 달 뒤 같은 조건을 복원할 수 없다.

Container image나 lockfile hash도 도움이 된다. 환경이 바뀌면 baseline을 새로 만드는 이유를 기록한다.

Artifact naming은 timestamp만 쓰지 말고 비교 가능한 key를 포함한다. 자동 regression system이 잘못된 baseline을 고르는 일을 줄일 수 있다.

## CHAPTER 29 · regression threshold는 business impact와 measurement noise 사이에 둔다

너무 좁은 threshold는 false alarm을 만들고, 너무 넓은 threshold는 실제 성능 악화를 놓친다. Historical run의 variance와 사용자 영향이 시작되는 effect size를 함께 고려해 metric별 threshold를 정한다.

Latency와 throughput에 동일한 퍼센트를 쓰지 않는다. p99는 noise가 크고 memory bytes는 비교적 안정적일 수 있다. 각 metric의 특성에 맞는 gate가 필요하다.

Gate가 fail하면 자동으로 rerun만 반복하기보다 raw distribution과 environment drift를 확인한다. 반복해서 통과할 때까지 돌리는 것은 검증이 아니라 선택 편향이 될 수 있다.

## CHAPTER 30 · measurement contract는 질문·환경·표본·판정 기준을 고정하는 것이다

신뢰할 benchmark는 네 요소를 명시한다. 어떤 질문을 답하는지, 어떤 환경과 dataset에서 재는지, sample과 duration을 어떻게 모으는지, 어느 차이를 regression으로 판정하는지다. 숫자 자체보다 이 protocol이 더 중요한 자산이다.

Microbenchmark는 compiler elimination과 harness overhead를 막고, system benchmark는 queueing·coordinated omission·client bottleneck을 통제한다. JIT, cache, DVFS, thermal, virtualization은 결과를 해석할 metadata로 남긴다.

마지막으로 한 번의 좋은 숫자를 찾지 않는다. 재현 가능한 distribution과 비교 가능한 artifact를 남겨야 다음 code change가 실제 개선인지 증명할 수 있다.
