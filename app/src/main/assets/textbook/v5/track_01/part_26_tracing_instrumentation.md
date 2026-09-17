# PART 26 · Tracing Internals — perf, ftrace, probes, eBPF, ring buffers

관측 도구는 사실을 자동으로 만들어 주지 않는다. tracepoint, probe, perf event, eBPF, ring buffer는 서로 다른 지점에서 서로 다른 비용으로 system state를 샘플링하거나 기록한다. 올바른 tracing은 **무엇을 관찰했는지, 무엇을 놓칠 수 있는지, 관측 자체가 실행을 얼마나 바꾸는지**까지 포함한다.

---

## CHAPTER 01 · instrumentation과 sampling은 다른 질문에 답한다

instrumentation은 특정 event가 일어날 때마다 record를 남겨 sequence와 count를 자세히 볼 수 있고, sampling은 일정 주기나 event count마다 현재 execution state를 관찰해 population을 추정한다. instrumentation은 completeness에 강하지만 overhead와 data volume이 커질 수 있고 sampling은 저비용이지만 희귀 event를 놓칠 수 있다.

CPU hotspot에는 sampling이 적합할 수 있고 정확한 lock acquisition 순서에는 event instrumentation이 필요할 수 있다. 문제에 맞지 않는 방법을 쓰면 많은 data를 모아도 답이 나오지 않는다.

실험 전 expected event rate와 허용 overhead를 정한다. trace on/off 성능 차이와 lost-event counter를 함께 기록해 관측 결과의 신뢰 범위를 명시한다.

---

## CHAPTER 02 · static tracepoint는 안정된 event schema를 제공한다

static tracepoint는 source에 의도적으로 정의된 관측 지점으로 event name과 field가 비교적 안정적이다. scheduler switch, block request 같은 subsystem semantic을 직접 표현할 수 있어 raw function probe보다 해석이 쉽다.

하지만 tracepoint가 존재하지 않는 내부 state는 관찰할 수 없고 kernel version에 따라 field가 바뀔 수 있다. event가 찍혔다는 사실도 subsystem의 모든 internal transition을 의미하지 않는다.

tracepoint schema와 kernel build를 함께 저장한다. consumer parser는 unknown field와 version 차이를 처리하고, release upgrade 뒤 동일 query가 같은 의미인지 contract test로 확인한다.

---

## CHAPTER 03 · dynamic probe는 observability와 safety를 교환한다

kprobe/uprobe 계열 dynamic probe는 source 수정 없이 function이나 instruction 지점에 instrumentation을 붙일 수 있어 incident 조사에 강하다. 대신 optimized/inlined code, symbol 변화, unsupported context 때문에 probe 위치의 semantic이 예상과 달라질 수 있다.

hot path에 무거운 probe를 걸면 latency를 직접 악화시키고 race timing을 바꿀 수 있다. 잘못된 address나 argument 해석은 bogus data를 만든다.

probe target build ID, symbol offset, attach success를 기록한다. probe를 켠 상태와 끈 상태의 reproduction rate를 비교해 observer effect를 확인한다.

---

## CHAPTER 04 · entry와 return probe는 한 call의 두 시점을 연결해야 한다

entry probe는 함수 진입 argument를, return probe는 결과와 duration을 볼 수 있다. 둘을 thread/task와 call instance 기준으로 연결하지 않으면 recursion이나 concurrency에서 다른 invocation을 잘못 짝지을 수 있다.

함수가 tail-call, inline, exception path로 종료되면 예상한 return hook이 보이지 않을 수도 있다. 모든 code path가 동일 instrumentation contract를 따르는지 확인해야 한다.

call ID, task ID, nesting depth를 사용해 pairing한다. unmatched entry/return count를 metric으로 두어 tracing 자체의 손실을 감지한다.

---

## CHAPTER 05 · ftrace는 kernel execution flow를 낮은 비용으로 기록한다

ftrace는 function, scheduler, IRQ 등 kernel 내부 event를 ring buffer에 기록해 timing과 call flow를 분석할 수 있다. function tracer를 전체 kernel에 켜면 data와 overhead가 폭증하므로 filter를 통해 문제 범위를 좁히는 것이 중요하다.

boot나 latency issue에서 trace buffer가 너무 작으면 가장 오래된 원인 event가 overwrite될 수 있다. snapshot trigger와 buffer size를 failure window에 맞춰야 한다.

trace clock, buffer size, enabled event set을 incident artifact에 포함한다. trace가 증상을 재현하지 못하게 만들면 더 낮은 overhead mode로 바꾼다.

---

## CHAPTER 06 · function graph는 call nesting과 duration을 보여 주지만 모든 wait를 설명하지 않는다

function graph tracing은 entry/return을 결합해 nested call tree와 duration을 보여 준다. kernel path에서 어느 function이 긴 시간을 소비했는지 찾기 좋지만 preemption과 interrupt, off-CPU wait가 duration 안에 섞일 수 있다.

한 함수가 10ms 걸렸다고 CPU를 10ms 사용했다는 뜻은 아니다. 중간에 sleep하거나 deschedule되었을 수 있다.

scheduler event와 function graph를 함께 봐 wall time과 on-CPU time을 분리한다. long function을 발견한 뒤 wait reason을 별도 증거로 확인한다.

---

## CHAPTER 07 · trace buffer loss는 가장 중요한 구간을 지울 수 있다

per-CPU ring buffer는 producer가 consumer보다 빠르면 overwrite 또는 lost record를 만들 수 있다. high event rate incident에서 loss가 커지면 trace가 정상처럼 보이는 구간만 남고 실제 burst가 사라질 수 있다.

buffer를 무조건 크게 하면 memory footprint와 copy cost가 늘어난다. event filtering과 trigger-based capture가 더 효율적일 수 있다.

lost-event counter와 per-CPU utilization을 trace 결과 옆에 표시한다. loss가 0이 아니라면 event absence를 사실 부재로 해석하지 않는다.

---

## CHAPTER 08 · per-CPU buffer의 timestamp를 total order로 단순 병합하면 안 된다

per-CPU buffer는 write contention을 줄이지만 event가 CPU별로 독립 기록된다. clock synchronization과 timestamp granularity에 따라 서로 다른 CPU event의 absolute 순서를 완벽히 판단하기 어려울 수 있다.

migration하는 task는 여러 buffer에 record가 나타난다. timestamp만 정렬하면 wakeup과 switch의 causality를 잘못 연결할 수 있다.

task ID, sequence, explicit relation을 사용해 ordering을 보완한다. cross-CPU causal edge와 단순 시간 근접성을 구분한다.

---

## CHAPTER 09 · BPF ring buffer는 kernel event를 user space로 전달하는 bounded channel이다

eBPF program이 만든 event를 ring buffer로 user space에 전달할 때도 producer/consumer rate가 맞아야 한다. consumer가 느리면 reservation 실패나 drop이 발생할 수 있으며, 무한 관측 channel은 존재하지 않는다.

record lifetime과 variable-length event layout을 정확히 처리해야 한다. schema mismatch는 trace corruption처럼 보일 수 있다.

reserve failure, buffer occupancy, consumer lag를 측정한다. event drop이 생기면 sampling/filtering을 조정하고 결과 신뢰 범위를 명시한다.

---

## CHAPTER 10 · BPF verifier는 kernel safety를 위한 static gate다

BPF verifier는 program이 허용되지 않은 memory를 접근하거나 unbounded execution을 만들 가능성을 분석해 load 전에 거부한다. verifier acceptance는 program이 business logic상 옳다는 뜻이 아니라 kernel safety constraint를 만족했다는 뜻이다.

복잡한 control flow나 pointer relation을 verifier가 증명하지 못해 실제로 안전한 program도 reject할 수 있다. verifier를 우회하기보다 state를 단순화해야 한다.

reject log와 program version을 보존한다. verifier-friendly rewrite 뒤 event semantic이 바뀌지 않았는지 unit test와 trace fixture로 확인한다.

---

## CHAPTER 11 · BPF map은 observation state의 lifetime과 concurrency를 만든다

BPF map은 counter, histogram, correlation state를 kernel/user program 사이에 공유한다. per-CPU map은 contention을 줄이지만 aggregation 단계가 필요하고 global hash map은 key cardinality와 synchronization 비용을 가진다.

request ID처럼 unbounded key를 map에 계속 넣으면 memory가 커지고 eviction policy가 없으면 observability가 system reliability를 해칠 수 있다.

map entry count, eviction, update failure를 모니터링한다. map key lifetime을 observed object lifetime과 맞추고 cleanup path를 테스트한다.

---

## CHAPTER 12 · attach point가 측정 semantic을 결정한다

같은 function이라도 entry, tracepoint, syscall, network hook 중 어디에 program을 attach하느냐에 따라 관찰 가능한 state와 timing이 다르다. 너무 이른 지점에서는 outcome을 모르고 너무 늦은 지점에서는 원인 정보가 사라질 수 있다.

attach point가 kernel version에서 바뀌거나 function이 inline되면 probe가 더 이상 동일 의미를 갖지 않을 수 있다.

관측 질문을 먼저 쓰고 그 질문에 필요한 최소 attach point를 선택한다. version upgrade 후 attach success뿐 아니라 event field 의미를 검증한다.

---

## CHAPTER 13 · perf event는 hardware와 software counter를 공통 sampling source로 연결한다

perf subsystem은 cycles, instructions, cache miss 같은 PMU event와 scheduler/software event를 count 또는 sample할 수 있다. event마다 정확도와 privilege scope가 다르며 multiplexing이 발생하면 raw count가 직접 비교되지 않을 수 있다.

frequency-based sampling과 period-based sampling은 workload 변화에 다른 bias를 만든다. short-lived task는 sample이 거의 없을 수 있다.

enabled/running time과 sample count를 결과에 포함한다. derived metric을 계산할 때 scaled count의 오차를 고려한다.

---

## CHAPTER 14 · hardware counter는 microarchitecture별 정의를 확인해야 한다

`cache-misses` 같은 이름이 있어도 실제 PMU event semantics는 CPU model마다 다를 수 있다. speculative event인지 retired event인지, 어느 cache level을 의미하는지 확인해야 한다.

다른 machine의 counter 절대값을 그대로 비교하면 틀릴 수 있다. multiplexing과 skid도 sample attribution을 흔든다.

CPU model, raw event code, kernel perf version을 기록한다. counter 변화는 generated code와 latency 변화가 함께 맞을 때 원인 evidence로 사용한다.

---

## CHAPTER 15 · sampling overhead는 rate와 stack depth에 따라 급격히 커질 수 있다

sample frequency를 높이고 stack unwinding까지 수행하면 interrupt와 memory write가 늘어나 application execution을 방해할 수 있다. 매우 짧은 hotspot을 잡으려다 전체 workload를 다른 상태로 만들 수도 있다.

overhead는 CPU 사용량뿐 아니라 cache pollution과 scheduling jitter로 나타난다. race bug가 sampling 중 사라지는 것도 가능하다.

여러 sampling rate에서 baseline latency를 비교해 overhead curve를 만든다. 가능한 가장 낮은 rate에서 동일 conclusion이 나오는지 확인한다.

---

## CHAPTER 16 · sampling mode는 관측하고 싶은 시간 축에 맞춘다

CPU-cycle sampling은 on-CPU hotspot에 강하고 wall-clock/off-CPU sampling은 sleep과 wait까지 포함할 수 있다. instruction event sampling은 특정 microarchitectural question에 답하지만 application wait를 직접 설명하지 못한다.

한 profile에서 CPU가 적게 보인다고 code가 빠르다는 결론은 틀릴 수 있다. 대부분의 시간이 lock이나 I/O wait라면 CPU sampler는 빈 공간을 보여 준다.

질문별로 CPU, wall, off-CPU profile을 분리한다. 동일 request에 여러 profile을 연결할 수 있으면 병목 분류가 더 정확해진다.

---

## CHAPTER 17 · unwinding은 exact binary metadata에 의존한다

optimized native stack은 frame pointer, DWARF CFI, architecture unwind table을 이용해 복원된다. wrong build symbol이나 손상된 unwind metadata를 사용하면 그럴듯하지만 틀린 stack이 생성될 수 있다.

JIT와 signal trampoline, hand-written assembly는 일반 frame 규칙과 다를 수 있다. stack sample loss가 특정 module에 몰릴 수 있다.

build ID와 symbol artifact를 sample과 함께 보존한다. unwind failure rate를 metric으로 두고 unknown stack을 무시하지 않는다.

---

## CHAPTER 18 · JIT symbolization은 code lifetime과 generation을 추적해야 한다

JIT code는 runtime에 생성·이동·폐기될 수 있어 address만으로 source method를 사후 해석하기 어렵다. code cache generation과 symbol map을 sample timestamp와 맞춰야 한다.

같은 address range가 나중에 다른 compiled method에 재사용될 수도 있다. crash 뒤 현재 code map만 보면 과거 sample을 잘못 symbolicate할 수 있다.

JIT load/unload event와 build/runtime version을 보존한다. profile pipeline이 generation-aware mapping을 사용하는지 검증한다.

---

## CHAPTER 19 · flame graph의 폭은 sample 비율이지 시간 순서가 아니다

flame graph는 stack sample을 집계해 같은 stack prefix의 폭을 넓게 보여 준다. x축 위치는 일반적으로 chronological timeline이 아니며 넓은 frame은 많은 sample에 나타났다는 뜻이다.

부모 frame이 넓어도 실제 CPU 소비는 자식이 할 수 있고, leaf가 넓어도 짧은 call이 매우 자주 실행된 결과일 수 있다.

CPU flame, off-CPU flame, wall flame의 의미를 구분한다. hotspot hypothesis는 raw sample과 request latency로 교차검증한다.

---

## CHAPTER 20 · off-CPU profile은 block site와 wakeup source를 연결한다

thread가 mutex, I/O, scheduler queue에서 기다리는 시간은 CPU profile에 나타나지 않는다. off-CPU analysis는 block 시작 stack과 wakeup/return 시점을 연결해 무엇을 기다렸는지 보여 준다.

wait site가 `read()`라고 해서 storage가 원인인 것은 아니다. socket peer, pipe, page fault 등 semantic이 달라질 수 있다.

block reason, duration, waker task를 기록한다. long wait가 lock owner scheduling 때문인지 downstream service 때문인지 causal chain을 만든다.

---

## CHAPTER 21 · event schema는 observability의 API다

trace event field name, unit, optionality, identifier semantics가 바뀌면 downstream parser와 alert가 조용히 틀릴 수 있다. 관측 data도 versioned contract로 관리해야 한다.

raw pointer나 sensitive payload를 event에 넣으면 보안과 cardinality 문제가 생긴다. 필요한 stable identifier와 bounded field만 선택한다.

schema version과 producer build를 record에 포함한다. parser compatibility test와 retention migration을 release 과정에 둔다.

---

## CHAPTER 22 · trace clock은 duration과 cross-host ordering 요구에 맞춰 선택한다

monotonic clock은 local duration에 적합하고 wall clock은 사람에게 보이는 시각과 cross-system 근사 정렬에 유용하다. clock correction이 가능한 source로 duration을 계산하면 왜곡될 수 있다.

CPU별 clock offset이나 virtualization 환경의 clock behavior도 확인해야 한다. 서로 다른 host timestamp만으로 strict causality를 증명할 수 없다.

clock source와 synchronization 상태를 artifact에 기록한다. request ID와 sequence relation을 timestamp보다 우선해 causality를 구성한다.

---

## CHAPTER 23 · probe context는 실행 가능한 operation을 제한한다

interrupt, NMI, atomic context처럼 probe가 실행되는 환경에 따라 allocation, sleep, lock 사용이 금지되거나 위험할 수 있다. 일반 function처럼 logging이나 heavy parsing을 수행하면 deadlock 또는 recursion을 만들 수 있다.

probe가 target subsystem이 사용하는 lock을 다시 획득하면 instrumentation 자체가 hang 원인이 된다.

probe handler는 최소 data capture에 집중하고 expensive work를 user space로 넘긴다. stress와 fault context에서 probe safety를 테스트한다.

---

## CHAPTER 24 · recursion protection은 tracer가 자기 자신을 다시 trace하지 않게 한다

instrumentation이 사용하는 function 자체에 probe가 걸리면 trace 기록 중 같은 path가 다시 호출되어 recursion이 생길 수 있다. 깊은 recursion은 stack overflow나 endless tracing으로 이어진다.

recursion guard는 data loss를 만들 수 있으므로 suppressed event 수를 이해해야 한다. guard가 있다고 모든 reentrancy 문제가 사라지는 것은 아니다.

tracer internal path와 target path를 분리한다. recursion/drop counter를 함께 보고 event absence를 과신하지 않는다.

---

## CHAPTER 25 · filtering은 data volume보다 질문의 범위를 줄이는 도구다

PID, cgroup, CPU, event field 조건으로 trace를 제한하면 buffer loss와 observer effect를 줄일 수 있다. 하지만 filter가 너무 좁으면 원인 event가 scope 밖에서 발생했을 때 완전히 사라진다.

symptom process만 filter했는데 실제 lock owner나 IRQ가 다른 context에 있으면 causal chain이 끊긴다.

wide low-cost capture로 범위를 확인한 뒤 단계적으로 좁힌다. filter 조건 자체를 incident note에 남겨 재해석 가능하게 한다.

---

## CHAPTER 26 · lost event는 측정 실패 자체를 first-class signal로 취급한다

trace queue overflow, perf sample loss, network exporter drop은 data pipeline capacity가 실제 event rate보다 부족했다는 뜻이다. 가장 바쁜 시점에 loss가 증가하므로 missing data는 random하지 않을 수 있다.

lost count를 숨기면 정상처럼 보이는 graph가 생성된다. 특히 incident peak에서 drop이 많으면 percentile과 causal order를 신뢰하기 어렵다.

producer별 loss metric과 buffer occupancy를 결과와 같이 표시한다. loss가 허용 threshold를 넘으면 재측정하거나 conclusion confidence를 낮춘다.

---

## CHAPTER 27 · correlation은 서로 다른 evidence plane을 하나의 operation으로 묶는다

log, trace, metric, kernel event를 연결하려면 request ID, task ID, connection ID, block request 같은 stable correlation key가 필요하다. timestamp 근접성만으로는 high concurrency에서 잘못 연결하기 쉽다.

logical operation과 retry attempt를 같은 ID로만 표현하면 duplicate execution을 구분하기 어렵다. parent/child relation과 generation을 명시한다.

correlation propagation이 끊기는 boundary를 test한다. queue, IPC, async callback을 건널 때 ID가 유지되는지 확인한다.

---

## CHAPTER 28 · observer effect는 측정이 대상 시스템을 바꾸는 현상이다

logging, probe, sanitizer, debugger는 execution timing, memory layout, scheduler behavior를 바꿀 수 있다. race가 instrumented build에서 사라지는 것은 bug가 고쳐졌다는 증거가 아니다.

high-frequency trace가 CPU와 I/O를 사용해 원래 latency를 악화시키는 반대 효과도 있다.

instrumentation level을 여러 단계로 바꾸며 symptom rate가 어떻게 변하는지 본다. production-safe low-overhead evidence와 lab heavy instrumentation을 구분한다.

---

## CHAPTER 29 · observability data도 security boundary를 가진다

trace에는 memory address, identifier, path, network endpoint, user payload가 포함될 수 있어 민감정보가 새 telemetry channel로 유출될 수 있다. privileged probe는 application보다 더 넓은 system state를 볼 수 있다.

관측 agent compromise는 kernel/process data를 수집하는 강한 공격 surface가 된다. least privilege와 retention policy가 필요하다.

field redaction, access control, audit log를 적용한다. debugging convenience를 이유로 production에 unrestricted probe capability를 남기지 않는다.

---

## CHAPTER 30 · tracing contract는 completeness, overhead, identity를 명시한다

신뢰 가능한 trace는 어떤 event를 기록하고 어떤 경우 drop될 수 있는지, clock이 무엇인지, identifier가 무엇을 가리키는지, instrumentation overhead가 어느 정도인지 설명할 수 있어야 한다. 이 정보가 없으면 상세한 timeline도 사실 증거로 쓰기 어렵다.

도구를 바꿔도 동일한 investigation question이 유지되어야 한다. trace가 원인을 '보여 준다'기보다 특정 hypothesis를 지지하거나 반박하는 evidence라는 원칙을 유지한다.

CLEAN 검증에서는 event schema, loss, clock, symbol identity, overhead를 각각 확인한다. 관측 시스템 자체의 failure mode까지 포함할 때 trace를 operational evidence로 신뢰할 수 있다.
