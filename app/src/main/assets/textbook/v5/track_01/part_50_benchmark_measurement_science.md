# PART 50 · Benchmark and Measurement Science — clocks, warmup, distributions, counter bias

성능 수치는 코드의 고유 속성이 아니라 **특정 hardware·OS·runtime·input·load·measurement procedure에서 관측된 결과**다. Benchmark가 틀리면 optimizer는 잘못된 bottleneck을 고치고 regression gate는 noise를 실패로 판단한다. 신뢰할 수 있는 측정은 질문을 명시하고, 환경을 통제하고, 숨은 state를 기록하며, 분포와 counter의 한계를 이해하고, practical effect가 재현되는지 검증해야 한다.

## CHAPTER 01 · benchmark는 먼저 어떤 의사결정을 위한 측정인지 정의한다

`A가 B보다 빠른가`는 input size, concurrency, latency metric, hardware를 정하지 않으면 답이 없다. Microbenchmark는 instruction/algorithm 변화 원인을 분리하기 좋고 macrobenchmark는 full-system interaction을 보지만 원인 추적이 어렵다. 목표가 cold start인지 steady-state throughput인지 p99 request latency인지 먼저 고정한다. 측정 항목이 의사결정과 연결되지 않으면 숫자를 더 많이 모아도 유용성이 늘지 않는다.

## CHAPTER 02 · microbenchmark는 작은 code를 격리하는 대신 production context를 제거한다

Function 하나를 반복 호출하면 network/I/O/scheduler noise를 줄여 CPU cost를 볼 수 있지만 production에서는 cache state, caller inlining, allocator, contention이 달라질 수 있다. Microbenchmark 개선이 application speedup으로 이어지는지 end-to-end benchmark에서 다시 검증한다. 반대로 macrobenchmark가 느려졌을 때 microbenchmark로 subsystem을 분해한다. 두 종류는 경쟁 관계가 아니라 서로 다른 inference 단계다.

## CHAPTER 03 · timer source의 monotonicity와 resolution이 측정 하한을 결정한다

Wall clock은 NTP/manual adjustment로 움직일 수 있어 duration 측정에 부적합할 수 있다. Monotonic high-resolution clock을 사용하고 API call 자체 overhead와 effective resolution을 확인한다. 측정 대상이 timer overhead와 비슷하게 짧으면 여러 operation을 batch하고 total time에서 평균을 구하되 loop overhead를 통제한다. `nanosecond 단위 값을 반환한다`가 실제 hardware resolution이 1ns라는 뜻은 아니다.

## CHAPTER 04 · JIT runtime에서는 warmup이 machine code state를 바꾼다

초기 iteration은 interpreter/baseline execution, profile collection, JIT compilation, deoptimization을 포함할 수 있다. 단순히 앞의 N회를 버린다고 optimized tier가 안정됐다고 보장되지 않는다. Compile/deopt event와 throughput trend를 함께 보고 steady state를 판정한다. Cold-start가 제품 요구라면 warmup을 제거하는 것이 오히려 잘못이다. P43 tier state를 benchmark artifact에 기록한다.

## CHAPTER 05 · cache warm/cold state는 같은 code의 memory cost를 크게 바꾼다

같은 dataset을 반복하면 L1/L2/LLC와 page cache에 올라가 production first-touch보다 훨씬 빠를 수 있다. 반대로 매 iteration cache를 인위적으로 flush하면 실제 steady workload보다 비현실적으로 느리다. 제품 access pattern이 reuse-heavy인지 streaming인지에 맞춰 warm/cold scenario를 분리한다. Cache hit/miss counter를 수집해 benchmark state가 의도와 일치하는지 확인한다.

## CHAPTER 06 · branch predictor도 반복 benchmark에서 input pattern을 학습한다

같은 조건 분기를 동일 sequence로 수백만 번 실행하면 predictor accuracy가 production의 diverse input보다 높아질 수 있다. Random input을 사용해도 generator overhead와 data dependency가 결과에 섞일 수 있다. 실제 branch outcome distribution과 target diversity를 trace/profile에서 추출해 benchmark input을 만든다. Branch-miss counter로 synthetic pattern이 production과 유사한지 확인한다.

## CHAPTER 07 · turbo/DVFS는 benchmark 초반과 후반의 CPU frequency를 다르게 만든다

짧은 run은 high boost frequency에서 끝나고 sustained run은 power/thermal budget에 따라 frequency가 낮아질 수 있다. 두 implementation의 실행 시간이 다르면 더 느린 쪽이 더 오래 throttled되는 비선형 효과도 있다. Per-core frequency/power state를 기록하고 warmup 뒤 steady thermal condition에서 반복한다. Frequency governor를 고정하는 실험은 production policy와 다르다는 한계를 명시한다.

## CHAPTER 08 · thermal state는 재실행 간 독립성을 깨뜨린다

Benchmark A를 먼저 돌려 chip을 가열한 뒤 B를 돌리면 B가 불리할 수 있다. A/B 순서를 randomize하거나 충분한 cool-down/steady-state 조건을 사용한다. Mobile device에서는 battery temperature와 charging state도 power policy를 바꿀 수 있다. P21 thermal metric을 benchmark metadata에 포함한다. 한 번의 A→B run으로 2% 차이를 결론내리지 않는다.

## CHAPTER 09 · CPU affinity/topology를 기록하지 않으면 run 간 hardware resource가 달라질 수 있다

Scheduler가 iteration마다 다른 core/SMT sibling/NUMA node로 이동시키면 cache와 capacity가 변한다. Microbenchmark는 affinity를 고정해 noise를 줄일 수 있고 production benchmark는 natural placement를 측정할 수 있다. 두 모드를 구분한다. Heterogeneous core system에서는 `same CPU utilization`이어도 다른 capacity에서 실행될 수 있으므로 CPU ID와 frequency를 저장한다.

## CHAPTER 10 · compiler가 계산 결과를 사용하지 않으면 benchmark body를 제거할 수 있다

Pure calculation 결과가 externally observable하지 않으면 optimizer는 dead code로 삭제하거나 loop 전체를 constant-fold할 수 있다. Source에 loop가 남아 있다고 실행된다는 보장은 없다. Result를 benchmark harness의 blackhole/observable sink에 전달하고 generated assembly/IR를 검사한다. Volatile 남용은 실제 production memory semantics와 다른 code를 만들어 또 다른 왜곡을 만든다.

## CHAPTER 11 · constant input은 constant folding과 specialization을 과도하게 유도할 수 있다

컴파일러가 argument를 compile-time constant로 알면 production runtime input에서는 불가능한 계산 제거가 일어날 수 있다. Benchmark parameter를 runtime에서 제공하되 measurement loop 안에서 parsing/random generation을 하지 않는다. 여러 representative value를 사용해 input-dependent branch와 algorithm path를 커버한다. `최선의 한 값`이 아니라 distribution을 측정한다.

## CHAPTER 12 · loop harness overhead는 작은 operation measurement에 섞인다

Operation이 몇 cycle이면 loop counter, branch, timer, function-call harness 비용이 결과의 큰 비율을 차지한다. Empty-loop baseline을 단순 subtraction하면 CPU pipeline interaction 때문에 정확하지 않을 수 있다. 여러 operation을 unroll/batch하고 per-operation ratio가 batch size 증가 후 안정되는지 확인한다. Microarchitectural counter와 generated code로 harness가 hotspot을 지배하지 않는지 본다.

## CHAPTER 13 · allocation benchmark는 allocator/GC state를 통제해야 한다

Object allocation을 반복하면 TLAB refill, GC cycle, heap expansion이 주기적으로 발생한다. 짧은 run은 fast bump-pointer만 측정하고 장시간 cost를 숨길 수 있다. Allocation rate, promoted bytes, GC pause/concurrent CPU를 함께 기록한다. Preallocated object를 재사용하는 implementation과 fresh allocation implementation은 semantic ownership이 같은지 먼저 확인한다.

## CHAPTER 14 · resource cache는 first-call과 steady-call을 완전히 다르게 만든다

DNS/TLS/file metadata/JIT/class loading/connection pool 같은 cache가 first operation에만 큰 cost를 만들 수 있다. Benchmark가 initialization을 setup으로 빼면 cold-start requirement를 놓치고, 매 iteration init을 포함하면 steady-state service를 왜곡한다. Product lifecycle에서 initialization frequency를 기준으로 scenario를 분리한다. Setup time을 숨기지 않고 별도 metric으로 보고한다.

## CHAPTER 15 · coordinated omission은 load generator가 느린 시스템에 요청을 덜 보내며 latency를 과소평가하는 오류다

Closed-loop client가 이전 response가 올 때까지 다음 request를 보내지 않으면 server가 멈춘 동안 arrival도 멈춘다. 실제 open-loop traffic에서는 그 시간에 요청이 계속 도착해 queue가 쌓인다. 측정 histogram이 `보낸 요청`만 기록하면 outage 동안 들어왔어야 할 대기 시간을 놓친다. P10 load model에 맞춰 arrival process를 선택하고 coordinated omission 보정 여부를 명시한다.

## CHAPTER 16 · latency는 평균 하나보다 분포가 더 많은 정보를 준다

평균 20ms가 p50 5ms+p99 500ms를 숨길 수 있다. User/SLO가 tail에 민감하면 percentile을 직접 측정하고 충분한 sample 수를 확보한다. Percentile estimator/histogram bucket 해상도가 target threshold를 표현할 수 있는지 확인한다. 서로 다른 request class를 한 histogram에 섞으면 multimodal distribution 원인을 잃을 수 있어 operation type/status별로 분리한다.

## CHAPTER 17 · percentile은 sample 수와 rank uncertainty를 함께 가진다

100개 sample에서 p99는 사실상 가장 느린 몇 개 값에 의해 결정되며 안정적인 tail estimate가 아니다. p99.9를 말하려면 훨씬 많은 independent/relevant observation이 필요하다. Benchmark duration을 fixed iteration보다 minimum sample/time와 convergence 기준으로 정할 수 있다. Rare pause를 보고 싶다면 GC/compaction cycle을 여러 번 포함할 정도로 길게 실행한다.

## CHAPTER 18 · outlier를 무조건 제거하면 실제 failure mode를 삭제할 수 있다

Background interrupt 하나 때문에 생긴 measurement glitch와 real GC pause/lock convoy는 모두 큰 값으로 보인다. Statistical rule로 상위 1%를 버리기 전에 cause를 조사한다. Outlier를 포함한 production distribution이 user experience라면 tail에서 제거하면 안 된다. Measurement error로 판정한 sample을 제외할 경우 exclusion rule과 count를 보고서에 남긴다.

## CHAPTER 19 · confidence interval은 run-to-run noise 속 effect uncertainty를 표현한다

두 평균이 1% 차이 나도 variance가 5%면 개선이라고 말하기 어렵다. 여러 independent run을 사용하고 bootstrap/appropriate statistical method로 estimate uncertainty를 구할 수 있다. 하지만 statistical significance가 product significance를 보장하지 않는다. 0.1% 확실한 speedup이 code complexity를 정당화하는지 별도 판단한다. Effect size와 confidence를 함께 보고한다.

## CHAPTER 20 · benchmark process 밖 background activity도 shared resource를 소비한다

OS update, antivirus/indexer, 다른 container, thermal daemon, browser가 CPU/cache/I/O를 경쟁할 수 있다. Dedicated host로 noise를 줄이거나 system metric을 함께 수집해 contaminated run을 식별한다. Cloud VM은 noisy neighbor와 vCPU steal time도 있다. `machine은 idle했다`는 관찰을 load average 하나로 증명하지 않는다.

## CHAPTER 21 · virtualization에서는 host scheduling과 migration이 guest benchmark에 섞인다

Guest 안에서 CPU pinning을 해도 host vCPU thread가 physical CPU에서 deschedule될 수 있다. Steal time, host overcommit, live migration, host NUMA를 기록하지 않으면 variance 원인을 guest code로 오진한다. Bare-metal result와 VM result를 직접 비교할 때 topology와 frequency exposure 차이를 명시한다. Performance CI의 VM class를 고정한다.

## CHAPTER 22 · hardware counter는 event 의미와 multiplexing을 이해해야 한다

동시에 사용할 수 있는 PMU counter 수보다 많은 event를 요청하면 tool이 time-multiplex하고 값을 scaling할 수 있다. Short benchmark에서는 scaling error가 커질 수 있다. CPU model마다 같은 이름 event의 의미가 다를 수 있다. Raw event와 derived metric formula, enabled/running time을 기록한다. Counter 하나를 absolute truth로 사용하지 않고 wall-clock/trace와 교차검증한다.

## CHAPTER 23 · sampling profiler는 skid와 sample-frequency bias를 가진다

Interrupt-based sample은 event 발생 instruction과 실제 recorded PC가 몇 instruction 어긋날 수 있고 high-frequency sample은 overhead를 늘린다. Very short function은 inclusive stack에서 과소/과대대표될 수 있다. Call-graph unwind 방식(frame pointer/DWARF/LBR)이 stack completeness에 영향을 준다. `top 1 function`을 정확한 cycle accounting으로 오해하지 않는다.

## CHAPTER 24 · input dataset은 size뿐 아니라 distribution과 correlation을 보존해야 한다

Hash table benchmark에서 random uniform key만 쓰면 production hot-key skew를 놓치고 compression benchmark에서 반복 string만 쓰면 과대 ratio가 나온다. Real traffic의 size, cardinality, locality, branch outcome, compressibility를 익명화/합성해 대표 dataset을 만든다. Edge-case worst-case dataset도 별도로 유지한다. 평균 case와 adversarial case를 같은 결과로 섞지 않는다.

## CHAPTER 25 · throughput benchmark는 concurrency와 offered load를 명시한다

`초당 10만 req`는 client concurrency, connection 수, batching, payload, response size가 없으면 재현 불가능하다. Low offered load에서는 모든 implementation이 충분히 빠르게 보이고 saturation 근처에서는 queueing이 급격히 증가한다. Throughput curve를 여러 load point에서 측정하고 p99 latency와 error rate를 함께 그린다. 최고 throughput 하나만 최적점으로 선택하지 않는다.

## CHAPTER 26 · benchmark client 자체가 bottleneck인지 확인한다

Server CPU가 50%인데 throughput이 더 안 오르면 load generator CPU/network, single connection, client lock이 limit일 수 있다. Client/server를 별도 host/core에 배치하고 generator event-loop lag와 send rate를 측정한다. Loopback benchmark는 network stack path와 NIC cost가 production과 다르다. Generator capacity가 target보다 충분히 높은지 사전 검증한다.

## CHAPTER 27 · saturation 이후의 behavior도 제품 성능의 일부다

Normal load에서 빠른 system이 overload 시 unbounded queue로 latency가 수십 초까지 늘어날 수 있다. Benchmark는 capacity point를 넘겨 load shedding, timeout, recovery hysteresis를 관찰한다. Load를 다시 낮췄을 때 queue가 얼마나 빨리 회복되는지도 본다. P10 overload contract가 실제 implementation에서 작동하는지 검증한다.

## CHAPTER 28 · artifact metadata 없이는 benchmark를 나중에 재현할 수 없다

Commit SHA, compiler/runtime version, build flags, OS/kernel, CPU model/topology, memory, governor/thermal state, dependency version, dataset hash를 결과와 묶는다. `지난주보다 8% 느림`이라는 그래프만 남기면 어느 환경 변화인지 분석할 수 없다. Benchmark harness 자체도 version control하고 result schema를 안정화한다. Production release artifact와 동일 build인지 검증한다.

## CHAPTER 29 · regression threshold는 noise floor와 product budget을 모두 반영한다

매 run 1% 흔들리는 benchmark에서 0.2% slowdown을 CI failure로 잡으면 flaky gate가 된다. 반대로 20%만 실패시키면 작은 regression이 누적된다. Historical variance로 noise floor를 측정하고 repeated confirmation, moving baseline을 설계한다. Critical latency budget은 statistical threshold보다 절대 SLO를 우선할 수 있다. Gate가 실패했을 때 raw samples와 environment metadata를 보존한다.

## CHAPTER 30 · 성능 PASS는 동일 질문을 반복 측정해 effect와 원인을 함께 증명한다

좋은 benchmark 보고서는 무엇을 측정했는지, 어떤 state를 통제/포함했는지, sample distribution과 uncertainty가 무엇인지, PMU/trace가 어떤 mechanism 변화를 뒷받침하는지 기록한다. 한 번의 wall-clock best score를 PASS로 쓰지 않는다. Micro improvement는 end-to-end에서 재검증하고, cold/warm/sustained/overload scenario를 분리한다. 재현 가능한 artifact와 measurement protocol 자체가 성능 증거의 일부다.