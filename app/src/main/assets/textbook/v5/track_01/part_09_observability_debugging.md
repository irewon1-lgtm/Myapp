# PART 09 · 증거 기반 디버깅 — reproduction, timeline, causal proof

디버깅은 원인을 떠올리는 능력이 아니라 **관측 가능한 사실을 만들고 가설을 제거하는 과정**이다. 로그·metric·trace·profile·dump는 각각 다른 질문에 답하므로 한 도구의 결과를 전체 원인으로 확대하지 않는다.

---

## CHAPTER 01 · observation과 hypothesis를 같은 문장에 섞지 않는다

`화면이 4.8초 뒤 나타났다`는 observation이고 `DB가 느리다`는 hypothesis다. 두 문장을 분리해야 새 증거가 나왔을 때 가설을 버릴 수 있다. bug report에는 expected/actual, 시작 시각, input, build, device/environment를 먼저 고정한다.

가설은 반증 가능한 형태로 쓴다. `DB가 느리다`보다 `request latency 4.8초 중 DB span이 3초 이상이며 동일 query의 lock wait가 증가했다`가 강하다. 다음 실험은 이 조건을 측정하도록 설계한다.

증거 항목에는 출처와 관측 시점을 같이 남긴다. 사용자가 말한 체감, application log, kernel trace, database metric은 신뢰 범위가 서로 다르며 서로 다른 실행에서 나온 자료를 한 timeline으로 합치면 거짓 causality를 만들 수 있다. incident note에서 FACT/HYPOTHESIS/TEST/RESULT를 구분하고, 각 FACT에 build ID·request ID·timestamp 같은 재식별 키를 붙이면 새로운 자료가 들어왔을 때 기존 결론을 수정하기 쉽다. 가설이 예측한 observable과 반대 observable도 미리 적어 confirmation bias를 줄인다.

---

## CHAPTER 02 · reproduction은 실패 조건을 하나의 실험으로 고정한다

재현 가능한 버그는 input, initial state, build artifact, environment, action sequence를 반복할 수 있어야 한다. `가끔`이라는 표현은 확률이 아니라 통제하지 못한 변수의 존재를 뜻한다.

재현율도 증거다. 100회 중 2회 발생하던 문제가 변경 후 100회 중 0회라고 해서 바로 해결을 증명할 수 없다. confidence를 높이려면 failure probability와 test count를 고려하고 deterministic trigger를 찾는다.

재현 recipe에는 random seed, account/data snapshot, device thermal/network state, concurrency level처럼 흔히 빠지는 조건도 포함한다. 실패가 2%라면 단 한 번의 성공 실행은 거의 정보가 없고, 수정 전후 같은 횟수와 같은 조건으로 비교해야 한다. race라면 sleep을 추가해 우연히 window를 바꾸는 대신 barrier·fault injection·scheduler control로 interleaving을 확대한다. reproduction harness 자체가 state를 초기화하지 못하면 이전 iteration의 cache나 DB row가 다음 결과에 영향을 주므로 각 run의 precondition checksum과 cleanup 성공 여부도 기록한다.

---

## CHAPTER 03 · binary isolation은 원인 공간을 반씩 줄인다

큰 시스템에서 모든 component를 동시에 읽지 않는다. feature, commit, request path, shard, device class를 기준으로 정상군/실패군을 나누면 원인 후보가 급격히 줄어든다.

Git bisect와 같은 원리는 runtime에도 적용된다. middleware 절반을 bypass하거나 data set을 절반으로 줄여 failure가 어느 쪽에 남는지 본다. 단, isolation change가 timing이나 state를 바꾸어 bug를 숨길 수 있으므로 control group을 유지한다.

좋은 분할 기준은 한 번에 하나의 causal boundary만 바꾼다. 특정 region traffic만 실패한다면 binary·configuration·data·dependency version 중 무엇이 region과 함께 바뀌는지 matrix로 만든다. component를 우회했더니 문제가 사라져도 우회가 load와 timing을 함께 줄였다면 해당 component가 원인이라고 확정할 수 없다. shadow request나 replay를 사용해 input과 load를 유지한 채 boundary만 교체하면 confounder를 줄일 수 있다. 각 isolation 실험에서 변경점, 기대 결과, 실제 결과를 남겨 제거된 후보와 아직 남은 후보를 명시한다.

---

## CHAPTER 04 · log는 문장이 아니라 structured event다

운영 로그는 사람이 읽는 설명문만 남기기보다 timestamp, event name, request ID, principal, resource ID, outcome, duration, error category를 구조화한다. 동일 event schema를 사용하면 query와 aggregation이 가능하다.

민감정보를 그대로 기록하지 않는다. token, password, personal payload는 redaction 정책을 두고 debugging에 필요한 stable identifier만 남긴다. log schema도 외부 데이터처럼 versioning과 compatibility가 필요하다.

structured event에는 event version과 producer build를 포함해 field 의미가 바뀐 시점을 추적한다. timestamp는 wall clock만 두지 않고 가능하면 process-relative monotonic duration이나 sequence를 함께 남겨 clock jump와 buffering reorder를 구분한다. 비동기 logger가 queue overflow로 event를 버릴 수 있으므로 dropped-log counter도 관측 대상이다. 동일 request에서 START와 END를 남긴다면 event ID 또는 span ID로 pair를 연결하고, exception text를 자유형 key로 사용해 cardinality가 폭발하지 않게 error category와 상세 message를 분리한다.

---

## CHAPTER 05 · log level은 심각도와 actionability를 표현한다

DEBUG/INFO/WARN/ERROR를 감정적으로 선택하면 운영에서 의미가 무너진다. ERROR는 일반적으로 요청 또는 system invariant가 실제로 실패해 operator/action이 필요한 사건에 사용하고, 예상 가능한 validation reject를 모두 ERROR로 남기면 signal-to-noise가 붕괴한다.

같은 failure를 여러 layer가 중복 ERROR로 기록하면 incident count가 부풀려진다. error ownership layer를 정하고 lower layer는 context를 attach한 뒤 propagate하는 방식으로 중복을 줄인다.

level 정책은 sampling과도 결합된다. 정상 INFO를 과도하게 줄이다가 rare transition의 유일한 evidence까지 잃지 않도록 state change, retry exhaustion, circuit transition처럼 진단 가치가 큰 사건은 별도 보존한다. ERROR가 실제 user failure를 뜻한다면 request outcome metric과 수가 대략 일치하는지 정기적으로 비교해 logging contract가 무너졌는지 확인할 수 있다. 동일 exception이 retry마다 찍히는 구조에서는 attempt-level DEBUG/WARN과 final operation ERROR를 분리해 한 logical failure가 여러 incident로 집계되는 문제를 막는다.

---

## CHAPTER 06 · correlation은 분산된 event를 하나의 request lifetime으로 묶는다

request ID, trace ID, job ID는 여러 process/service의 event를 연결한다. ID가 없으면 timestamp와 payload를 추측해 같은 요청을 찾게 되고 concurrency가 높을수록 잘못 연결할 가능성이 커진다.

correlation ID는 security identity가 아니다. 사용자가 누구인지 증명하거나 authorization을 부여하지 않는다. 동일 request가 retry되어 새 attempt가 생길 때 logical operation ID와 attempt ID를 분리하면 duplicate execution을 분석하기 쉽다.

fan-out에서는 하나의 operation이 여러 child request를 만들기 때문에 parent operation ID, child span/request ID, attempt ID를 계층적으로 유지한다. queue에 저장됐다가 수분 뒤 실행되는 job은 원래 HTTP request lifetime과 분리되므로 enqueue event와 durable job ID를 link로 남긴다. ID가 외부에서 들어온다면 길이와 형식을 제한하고 로그 injection이나 cardinality abuse를 막는다. correlation propagation 실패율 자체를 metric으로 두면 trace가 끊기는 service boundary를 찾을 수 있다. 동일 ID 재사용이 발생하지 않도록 생성 scope와 lifetime도 정의한다.

---

## CHAPTER 07 · stack trace는 실패 시점의 call chain이지 전체 timeline이 아니다

exception stack은 현재 thread가 어떤 call chain을 거쳤는지 보여 준다. optimized code, coroutine, async callback에서는 source-level logical call chain이 physical stack과 다를 수 있다.

stack의 top frame만 고치지 않는다. exception type, root cause chain, input state, thread context를 함께 본다. native crash에서는 symbol file/build ID가 없으면 raw address만 남으므로 release artifact와 symbol을 보존한다.

async 경계에서는 submit site와 execute site가 physical stack에서 끊어질 수 있으므로 task/request ID로 causal link를 보완한다. exception chaining이 여러 번 wrapping되면 최상위 message보다 가장 안쪽 cause와 각 wrapper가 추가한 context를 함께 본다. optimized native frame은 inlining 때문에 하나의 instruction address가 여러 source frame으로 symbolization될 수 있어 exact build와 debug info가 필요하다. stack fingerprint를 incident grouping에 쓰더라도 line number 변화나 obfuscation 때문에 같은 root cause가 여러 group으로 갈라지지 않는지 검증한다.

---

## CHAPTER 08 · thread dump는 wait graph를 만들 때 가치가 커진다

thread dump는 각 thread가 runnable, blocked, waiting, sleeping 중 어떤 상태인지와 stack을 보여 줄 수 있다. 한 장의 dump에서 동일 lock을 기다리는 thread와 owner를 연결하면 contention/deadlock 구조를 찾을 수 있다.

순간 snapshot만으로 starvation을 증명하기 어렵다. 일정 간격의 여러 dump 또는 scheduler trace를 통해 상태가 지속되는지 확인한다. runnable thread가 오래 CPU를 못 받는 문제와 monitor wait를 구분한다.

반복 dump를 비교할 때 thread name보다 stable thread ID와 lock identity를 사용한다. 같은 stack이 여러 snapshot에서 유지되는지, owner가 바뀌는지, runnable queue에서 진전이 있는지를 보면 deadlock과 단순 장기 I/O를 구분하기 쉽다. pool starvation에서는 모든 worker가 downstream future를 기다리고 그 future를 실행할 task가 같은 pool queue에 갇힌 구조가 나타날 수 있다. wait-for graph에 thread뿐 아니라 executor queue, connection pool, Binder thread 같은 bounded resource를 node로 추가하면 단순 monitor deadlock보다 넓은 progress failure를 표현할 수 있다.

---

## CHAPTER 09 · crash dump는 process가 사라지기 직전의 machine state를 보존한다

native crash dump/tombstone에는 signal, fault address, register, module mapping, backtrace가 포함될 수 있다. faulting instruction과 memory address relation을 보면 null dereference, use-after-free, stack corruption 후보를 좁힐 수 있다.

ASLR 때문에 address는 실행마다 달라질 수 있으므로 module base와 build ID가 필요하다. crash dump를 source revision, compiler flags, native library version과 연결해야 symbolization 결과를 신뢰할 수 있다.

fault address만 보고 null dereference라고 단정하지 않는다. small non-zero offset는 null base+field일 수 있지만 corrupted pointer나 guard page 접근도 가능하다. register dump에서 faulting instruction의 base/index register를 복원하고 memory map에서 해당 address가 어떤 mapping에 속했는지 확인한다. use-after-free가 의심되면 allocator poisoning/sanitizer 재현과 연결하고, stack corruption이면 unwind가 깨진 이후 frame을 무조건 신뢰하지 않는다. minidump 수집 파이프라인은 truncation 여부와 symbol server에 정확한 build ID가 존재하는지도 검증해야 한다.

---

## CHAPTER 10 · metric은 사건이 아니라 population을 본다

counter는 누적 사건 수, gauge는 시점 값, histogram은 분포를 표현한다. request latency를 gauge 평균 하나로 저장하면 tail을 잃는다. retry count를 request count와 분리해야 실제 load amplification을 볼 수 있다.

metric name보다 label cardinality가 운영 비용을 좌우할 수 있다. user ID나 raw URL을 label로 넣으면 time series 수가 폭발한다. bounded dimension만 metric label로 사용하고 high-cardinality detail은 trace/log로 보낸다.

counter는 process restart에서 reset될 수 있으므로 rate 계산기가 reset을 음수 traffic으로 해석하지 않는지 확인한다. gauge는 sample 시점 사이의 peak를 놓칠 수 있어 queue depth처럼 burst가 중요한 값에는 high-water mark나 histogram이 유용하다. histogram bucket 경계가 SLO 주변을 충분히 세밀하게 표현하지 못하면 p99 추정이 거칠어진다. metric을 추가할 때 unit, monotonic 여부, aggregation rule, label domain을 schema처럼 문서화하고 producer version이 바뀌어 의미가 달라지면 같은 name을 재사용하지 않는 편이 안전하다.

---

## CHAPTER 11 · percentile은 tail latency를 드러내지만 aggregation 방식에 주의한다

p99는 request의 99%가 그 값 이하라는 distribution statistic이다. 평균이 안정적이어도 소수 request가 수초로 늘어나면 사용자 체감과 timeout rate가 악화될 수 있다.

instance별 p99를 다시 평균내면 전체 population p99가 되지 않는다. merge 가능한 histogram/quantile sketch를 사용하거나 raw bucket을 집계한다. sample count가 적은 interval의 high percentile은 불안정할 수 있다.

percentile 비교에서는 window와 population을 고정한다. cache-hit 요청과 cache-miss 요청을 합치면 mix 변화만으로 전체 p99가 바뀔 수 있으므로 중요 class를 분리한다. histogram 최상위 bucket보다 큰 값이 모두 한 bucket에 들어가면 극단 tail을 구분할 수 없다. SLO가 500ms라면 500ms 주변 bucket과 timeout 경계가 충분한 해상도를 가져야 한다. 변화가 작은 구간에서는 bootstrap 같은 통계기법보다 먼저 sample count와 measurement noise를 확인하고, p99 한 점 대신 시간에 따른 distribution과 timeout/error rate를 함께 본다.

---

## CHAPTER 12 · RED와 USE는 관측 질문을 구조화한다

request-driven service에서는 Rate, Errors, Duration을 보면 traffic과 failure/latency 변화를 빠르게 파악할 수 있다. resource 관점에서는 Utilization, Saturation, Errors가 CPU, pool, disk, queue 병목을 찾는 출발점이 된다.

framework 이름을 외우는 목적이 아니다. 증상 metric만 보지 말고 demand와 capacity, queue/saturation을 같이 보라는 구조다. latency가 오를 때 request rate, error, queue depth, utilization을 같은 시간축에 놓는다.

100% utilization이 항상 문제인 것도 아니고 50%가 항상 여유인 것도 아니다. single-thread bottleneck은 host CPU 평균이 낮아도 포화될 수 있고, bursty queue는 interval 평균 utilization에 숨는다. saturation은 runnable queue, connection wait, disk queue, pool pending count처럼 resource별 다른 형태로 나타난다. RED에서 user-impact 변화를 확인한 뒤 USE로 어느 resource가 demand를 못 따라가는지 좁히고, dependency RED를 다시 연결하면 “CPU가 높다” 같은 상관관계보다 causal 후보가 강해진다. capacity 변경 후에는 queue와 tail latency가 함께 개선되는지 확인한다.

---

## CHAPTER 13 · distributed trace는 service boundary의 시간과 causal parent를 연결한다

trace는 request를 span으로 나누어 service, DB, queue, external API의 duration을 연결한다. parent-child 관계가 있으면 단순 timestamp보다 causal path를 이해하기 쉽다.

sampling 때문에 모든 slow request가 trace에 남지 않을 수 있다. head sampling, tail sampling의 trade-off를 이해하고 error/high-latency trace 보존 정책을 설계한다. async queue를 건널 때 trace context propagation을 잃지 않는다.

sampling은 population을 편향시킬 수 있으므로 trace count를 request metric의 정확한 분모로 사용하지 않는다. head sampling은 시작 시점에는 future latency를 모르고, tail sampling은 완료 전 span을 buffer해야 하므로 resource cost가 생긴다. retry와 fan-out에서 child span이 폭증하면 trace 자체가 backend 부하를 만들 수 있어 span limit가 필요하다. queue 경계에서는 producer span과 consumer span을 link로 연결해 긴 대기시간을 service execution과 분리한다. trace에서 보인 critical path가 실제 전체 traffic에서도 흔한지 metric과 교차 검증한다.

---

## CHAPTER 14 · profiler는 CPU 시간을 function에 귀속하지만 wait를 설명하지 못할 수 있다

sampling CPU profiler는 주기적으로 instruction pointer/stack을 관찰해 on-CPU time 분포를 추정한다. instrumentation profiler는 함수 진입/종료를 기록해 세밀한 데이터를 얻지만 overhead가 더 클 수 있다.

CPU profile에 hotspot이 없는데 wall latency가 길다면 off-CPU wait를 의심한다. 반대로 profiler overhead가 scheduling을 바꾸어 race를 숨길 수 있으므로 production-safe sampling과 lab instrumentation을 구분한다.

sampling frequency가 낮으면 짧고 자주 반복되는 function을 놓칠 수 있고 너무 높으면 overhead와 sample bias가 커진다. native PMU sample은 skid로 event가 실제 발생한 instruction과 약간 다른 위치에 귀속될 수 있다. JIT/inlining이 있는 runtime에서는 symbolization 시점과 code version도 맞아야 한다. profile 비교는 absolute sample 수보다 같은 workload와 duration에서의 비율을 보고, process가 throttled되거나 sleep한 시간이 많다면 CPU profile이 wall-clock 병목을 대표하지 않는다는 사실을 명시한다. off-CPU trace와 함께 해석한다.

---

## CHAPTER 15 · flame graph는 stack sample의 집계다

flame graph의 width는 일반적으로 sample에 나타난 비중이며 시간 순서가 아니다. 넓은 box가 반드시 느린 단일 call을 뜻하지 않고 매우 자주 실행된 짧은 call일 수도 있다.

top-down으로 request path를, bottom-up으로 CPU-consuming leaf를 본다. recursion과 async stack stitching 여부도 확인한다. wall-clock flame graph와 CPU flame graph는 의미가 다를 수 있다.

전후 flame graph를 비교할 때 denominator가 달라지면 비율 변화가 오해를 만든다. total CPU가 절반으로 줄어든 뒤 한 함수의 비율이 10%에서 15%로 늘어도 absolute CPU는 줄었을 수 있다. differential flame graph를 사용하거나 sample duration·total cycles를 같이 기록한다. `[unknown]` frame이나 collapsed stack이 많으면 symbolization 품질부터 해결해야 한다. 넓은 parent box가 실제 optimization target인지 판단하려면 leaf self time, 호출 빈도, request criticality를 분리하고 generated/JIT/native frame이 같은 stack에 올바르게 연결됐는지 확인한다.

---

## CHAPTER 16 · off-CPU 분석은 기다린 이유와 깨운 주체를 찾는다

thread가 sleep, mutex, I/O, scheduler queue에서 시간을 보낸다면 CPU profiler에는 원인이 충분히 보이지 않는다. off-CPU trace는 block 시작, wakeup, 실제 reschedule까지의 구간을 분해한다.

lock owner가 CPU를 못 받아 waiter가 늘어나는 priority/scheduling 문제도 가능하다. wait site와 wakeup source를 연결해야 단순히 `read()`가 느렸다는 오판을 피할 수 있다.

wait duration을 blocked→wakeup과 wakeup→on-CPU로 나누면 dependency latency와 scheduler latency를 구분할 수 있다. mutex wait라면 owner thread의 on/off-CPU history를 연결하고, socket read라면 packet arrival과 protocol processing을, future wait라면 producer task의 queue state를 따라간다. timeout으로 깨어난 경우 정상 completion wakeup과 별도 category로 기록한다. 많은 waiter가 동시에 깨워져 thundering herd를 만들면 wakeup 자체가 CPU burst를 만들 수 있으므로 wake count와 실제 progress thread 수를 비교한다. wait graph와 scheduler trace를 함께 본다.

---

## CHAPTER 17 · system trace는 app, scheduler, Binder, frame을 하나의 시간축에 놓는다

Android system trace/Perfetto에서는 main thread slice, Binder transaction, CPU scheduling, frame timeline, I/O event를 함께 볼 수 있다. UI jank가 application code인지 remote Binder service인지 scheduler delay인지 분리할 수 있다.

trace buffer 크기와 category가 너무 많으면 overhead와 data loss가 생긴다. investigation question에 필요한 event를 선택하고 정확한 failure window를 capture한다.

trace capture가 circular buffer라면 failure 직후 stop하지 못해 원인 구간이 덮어써질 수 있다. trigger 기반 capture나 충분한 pre/post window를 설계하고 dropped event counter를 확인한다. 서로 다른 data source가 다른 clock을 사용하면 importer의 clock snapshot/translation이 제대로 적용됐는지 본다. jank frame 하나를 기준으로 main thread runnable delay, Binder callee, RenderThread, GPU fence까지 이어지는 dependency를 따라가고, aggregate CPU chart는 보조로 사용한다. trace 설정 자체를 version control하면 재현 간 category 차이를 줄일 수 있다.

---

## CHAPTER 18 · allocation profile은 누가 memory를 만들었는지 보여 준다

heap size만 보면 allocation churn을 놓친다. allocation profile은 type, allocation site, rate를 보여 주어 short-lived object가 GC pressure를 만드는 path를 찾게 한다.

높은 allocation count가 곧 leak은 아니다. object가 빠르게 수거되면 retention은 낮다. latency issue라면 allocation rate와 GC pause를, memory growth라면 retained object graph를 본다.

sampling allocation profiler는 작은 allocation을 일부 놓치거나 size-biased sample을 사용할 수 있어 exact object count와 동일하지 않을 수 있다. instrumentation mode는 정확도가 높아도 실행 특성을 크게 바꿀 수 있다. allocation site를 최적화할 때는 object 수뿐 아니라 bytes/sec와 lifetime distribution을 보고, escape analysis나 pooling 도입이 오히려 long-lived retention을 늘리지 않는지 확인한다. native/graphics allocation은 managed profiler 밖에 있을 수 있으므로 process RSS 변화와 heap growth가 맞지 않으면 다른 allocator evidence를 추가한다.

---

## CHAPTER 19 · retained size와 dominator는 leak root를 찾는 데 사용한다

object 하나의 shallow size가 작아도 그것이 reference chain의 유일한 root이면 거대한 graph를 살려 둘 수 있다. dominator/retained size는 해당 object가 제거될 때 함께 reclaim 가능한 graph 규모를 추정한다.

leak fix는 큰 object를 직접 찾는 작업이 아니다. GC root에서 왜 reference가 유지되는지 ownership/lifecycle을 추적한다. listener, callback, static cache, native reference가 lifecycle보다 오래 살 수 있다.

heap snapshot은 capture 시점의 graph이므로 정상 cache가 일시적으로 크게 보일 수도 있다. 동일 workload의 여러 시점 snapshot을 비교해 특정 owner 아래 retained set이 지속적으로 증가하는지 본다. weak reference와 finalizer/native global reference는 일반 strong edge와 다른 semantics를 가지므로 도구 표시를 확인한다. Android에서는 destroyed Activity가 singleton/listener를 통해 root에 연결되는 경로처럼 lifecycle과 reference graph를 대조한다. fix 후에는 해당 root path가 사라지는지와 GC 이후 RSS/heap이 실제 안정화되는지를 함께 검증한다.

---

## CHAPTER 20 · database evidence는 query time을 parse/plan/lock/I/O로 분해한다

slow query에서 SQL text만 수정하기 전에 execution plan, row estimate, actual row count, index usage, buffer/cache hit, lock wait를 확인한다. 같은 query도 data distribution과 parameter에 따라 plan이 달라질 수 있다.

transaction이 오래 열린 경우 query 자체는 짧아도 다른 transaction을 block할 수 있다. DB latency와 application pool wait를 분리한다. connection 획득 시간이 query execution time에 섞이지 않게 span을 나눈다.

plan 분석에서는 estimated row와 actual row의 큰 차이가 optimizer 선택을 왜곡했는지 보고 통계 freshness와 parameter sensitivity를 확인한다. lock wait가 길다면 blocker transaction의 시작 시각, held lock, application request를 역추적한다. storage I/O가 원인이라면 buffer hit와 physical read latency를 연결한다. connection pool pending time이 높지만 query는 빠르면 DB engine보다 application concurrency budget이 병목일 수 있다. query fingerprint, plan hash, rows scanned/returned, lock wait, pool wait를 같은 operation ID에 붙이면 SQL text 변경 전에 병목 계층을 분리할 수 있다.

---

## CHAPTER 21 · network timing은 DNS, connect, TLS, server, transfer를 분리한다

end-to-end request latency를 하나의 HTTP duration으로만 기록하면 원인을 찾기 어렵다. DNS lookup, TCP/QUIC connection, TLS handshake, request queue, server processing, TTFB, body transfer를 가능한 범위에서 분리한다.

retry가 숨겨져 있으면 사용자는 한 request로 보지만 실제 network attempt는 여러 번일 수 있다. attempt별 status와 timeout 원인을 기록하고 connection reuse 여부도 함께 본다.

DNS도 cache hit/miss와 resolver attempt가 다르고, connect는 IPv4/IPv6 후보 경쟁이나 proxy 경로에 따라 여러 socket 시도가 생길 수 있다. TLS resumption 여부와 certificate validation 시간, HTTP connection reuse/multiplexing 여부를 기록하면 handshake 비용과 server 비용을 나눌 수 있다. TTFB가 길어도 client upload가 아직 끝나지 않았거나 server queue에서 기다린 경우가 있으므로 request body send 완료 시각을 포함한다. packet capture가 필요할 때는 application trace ID와 5-tuple/time window를 연결하고 개인정보 payload는 최소화한다.

---

## CHAPTER 22 · wall clock과 monotonic clock은 용도가 다르다

wall clock은 사람이 보는 날짜/시각과 동기화되며 NTP/manual adjustment로 점프할 수 있다. duration 측정에는 monotonic clock이 적합하다.

분산 시스템의 서로 다른 host timestamp를 exact causal order로 믿으면 clock skew가 문제를 만든다. trace context와 sequence/event relation을 이용하고 wall time은 근사적인 cross-host 정렬에 사용한다.

process 간 duration을 비교하려면 각 timestamp가 어떤 clock domain에서 왔는지 메타데이터가 필요하다. device/GPU/NIC hardware timestamp는 host monotonic clock과 offset·frequency가 다를 수 있어 clock snapshot이나 calibration이 필요하다. wall clock이 뒤로 이동하면 `end-start`가 음수가 될 수도 있으므로 timeout/deadline은 monotonic source를 사용한다. cross-host causality는 request sequence와 send→receive relation으로 보강하고, incident timeline에는 NTP offset/clock uncertainty를 같이 기록해 수 ms 차이를 과도하게 해석하지 않는다.

---

## CHAPTER 23 · Heisenbug는 관측이 timing을 바꾸는 현상까지 포함한다

추가 로그, debugger breakpoint, sanitizer가 thread scheduling과 allocation layout을 바꾸어 race 증상을 사라지게 할 수 있다. instrumentation 후 재현이 안 된다는 사실은 수정 증거가 아니다.

observer effect를 줄이려면 low-overhead trace, sampling, hardware counter를 사용하고 reproduction condition을 유지한다. race detector처럼 의도적으로 execution을 바꾸는 도구는 결과를 보조 증거로 해석한다.

instrumentation on/off 두 cohort의 failure rate와 timing distribution을 비교하면 관측 자체의 영향 크기를 추정할 수 있다. 로그 한 줄이 lock을 추가하거나 allocation을 유발하는지, debugger가 모든 thread를 stop하는지, sanitizer가 memory layout을 바꾸는지 도구의 mechanism을 이해해야 한다. 희귀 race에서는 hardware trace나 ring buffer처럼 overwrite 방식의 저비용 event를 사용하고 failure 후 필요한 구간만 보존한다. 관측을 추가한 뒤 증상이 사라지면 “고쳐졌다”가 아니라 timing-sensitive라는 새로운 evidence로 기록한다.

---

## CHAPTER 24 · feature flag와 canary는 변화 범위를 제어하는 실험 도구다

새 code path를 일부 traffic에만 적용하면 control/canary의 metric을 같은 시간대에 비교할 수 있다. 전체 rollback보다 빠르게 hypothesis를 검증하고 blast radius를 제한한다.

flag 자체가 장기간 남으면 두 code path를 유지하는 complexity가 된다. owner, expiration, default state를 관리한다. data migration처럼 되돌릴 수 없는 변화는 flag만으로 rollback되지 않는다.

control과 canary는 user/device/region mix가 비슷해야 비교가 가능하다. hash-based stable assignment를 사용하지 않으면 같은 사용자가 두 path를 오가며 stateful effect가 섞일 수 있다. canary가 작을 때 rare error p99는 sample 부족으로 보이지 않을 수 있으므로 절대 error count와 confidence를 함께 본다. flag가 DB schema write처럼 persistent side effect를 바꾸면 off로 돌린 뒤 old path가 new state를 읽을 수 있는지 확인한다. rollout 단계마다 promotion/rollback threshold를 사전에 정해 이상을 본 뒤 기준을 바꾸지 않는다.

---

## CHAPTER 25 · Git bisect는 regression boundary를 commit 단위로 찾는다

good와 bad commit 사이에서 test를 자동 실행하면 binary search로 regression introduction point를 찾을 수 있다. test가 deterministic할수록 bisect 결과가 강하다.

build environment와 dependency가 commit 외부에서 변하면 과거 commit을 동일하게 재현하지 못할 수 있다. lockfile, toolchain, artifact source를 고정해 commit 비교가 실제 code difference를 반영하게 한다.

flaky reproduction을 bisect에 넣으면 한 번의 우연한 PASS가 search path를 잘못된 절반으로 보낼 수 있다. 각 commit에서 여러 번 실행하거나 known failure probability에 맞춘 판정 규칙을 둔다. build가 깨지는 commit은 skip할 수 있지만 skip이 많아지면 culprit 범위가 넓어진다는 점을 기록한다. feature flag나 remote config가 commit과 독립적으로 바뀌면 historical binary만으로는 재현되지 않으므로 당시 config/data snapshot을 함께 복원한다. culprit commit을 찾은 뒤에는 그 diff 안의 어떤 invariant가 깨졌는지 별도 실험으로 확인한다.

---

## CHAPTER 26 · debugging experiment는 한 번에 한 가설을 바꾼다

여러 설정과 코드를 동시에 바꾸고 증상이 사라지면 어떤 변화가 효과였는지 모른다. experiment는 independent variable, measurement, expected result를 미리 적는다.

negative result도 후보를 제거한 증거다. 실험 결과를 기록하면 같은 추측을 반복하지 않는다. production에서 실험할 때는 safety limit와 rollback 조건을 먼저 정한다.

실험 카드에는 baseline, 변경 변수, 고정 변수, 예상 observable, 반증 조건, 실행 횟수, 종료 기준을 적는다. cache clear와 code change를 동시에 하면 어느 것이 결과를 만들었는지 모르는 식의 confounding을 피한다. production shadow/canary 실험은 user impact budget과 자동 rollback threshold를 포함한다. 결과가 기대와 다르면 “실험 실패”가 아니라 해당 가설의 posterior가 낮아졌다는 뜻으로 기록하고 다음 후보로 이동한다. 동일 실험을 다른 사람이 재실행할 수 있도록 command, data version, artifact SHA까지 남긴다.

---

## CHAPTER 27 · observability 자체도 CPU, storage, network budget을 소비한다

모든 request의 full payload와 stack을 기록하면 system을 관찰하기 위해 system을 망가뜨릴 수 있다. sampling, aggregation, retention tier로 비용을 통제한다.

trace span과 log field에 high-cardinality 데이터를 무제한 넣으면 backend 비용이 폭증한다. debugging 가치가 높은 field와 개인정보/비용 risk를 함께 평가한다.

관측 파이프라인이 overload될 때 application request까지 block하면 incident 중 가장 필요한 순간에 서비스가 더 느려질 수 있다. logger/telemetry exporter에는 bounded queue와 drop/backpressure 정책을 두고 dropped event metric을 별도 경로로 보낸다. sampling rate를 동적으로 올릴 때 CPU와 egress budget 상한을 지킨다. retention은 raw trace, aggregated metric, audit log의 규제/진단 가치에 따라 다르게 설정하고, 개인정보 필드는 source에서 redaction해 downstream 복제본 전체에 퍼지지 않게 한다. observability 비용도 release 성능 테스트에서 측정한다.

---

## CHAPTER 28 · alert는 원인 추측보다 사용자 영향에 가까워야 한다

CPU 80% 하나로 alert하면 정상 batch workload에서도 noise가 발생할 수 있다. availability, latency, correctness처럼 SLO/user impact에 가까운 signal을 primary alert로 두고 resource metric은 diagnosis에 사용한다.

alert마다 owner, severity, runbook, deduplication, auto-resolve 조건을 둔다. action 없는 alert는 운영자가 무시하게 되고 실제 incident signal도 묻힌다.

짧은 spike와 지속적인 budget 소진을 구분하려면 여러 window의 error-budget burn rate처럼 시간 축을 포함한 조건이 유용하다. dependency 하나의 CPU alarm보다 실제 request SLO가 먼저 깨졌는지를 기준으로 page severity를 정하면 noise를 줄일 수 있다. alert test는 synthetic failure를 주입해 실제 notification, routing, dedup, resolve가 작동하는지 확인한다. threshold 변경 후에는 alert count뿐 아니라 missed incident와 operator response time을 같이 검토해 조용해졌다는 이유만으로 품질이 좋아졌다고 판단하지 않는다.

---

## CHAPTER 29 · incident timeline은 사실과 결정의 순서를 보존한다

incident 중에는 symptom 시작, alert, deploy, mitigation, recovery를 timestamp와 evidence link로 기록한다. 사후 기억만으로 timeline을 재구성하면 hindsight bias가 생긴다.

postmortem은 사람의 실수를 단일 root cause로 끝내지 않고 detection gap, unsafe default, missing rollback, capacity assumption 같은 system condition을 찾는다. action item은 owner와 검증 방법을 가져야 한다.

서로 다른 host의 timestamp는 clock skew가 있을 수 있으므로 deploy ID, request ID, sequence와 함께 정렬한다. 각 timeline item에 FACT, DECISION, ACTION을 구분하면 “원인을 알았다”는 시점과 실제 mitigation 시점을 혼동하지 않는다. 변경을 실행한 사람이 당시 어떤 evidence를 보고 결정했는지 링크를 남겨 사후 지식으로 판단을 왜곡하지 않는다. action item은 문서 작성 같은 산출물보다 재발 방지 invariant와 이를 검증할 test/alert를 정의한다. 다음 drill에서 해당 control이 실제로 failure를 막거나 더 빨리 탐지하는지 확인해야 닫을 수 있다.

---

## CHAPTER 30 · AI debugging은 가설 생성에 쓰고 사실 판정에는 evidence를 요구한다

AI가 stack trace나 code를 보고 가능한 원인을 빠르게 나열할 수 있지만 실행하지 않은 path와 실제 runtime state를 알 수 없는 경우가 많다. 제안마다 `이 가설이 맞다면 어떤 observable이 보여야 하는가`를 요구한다.

AI가 만든 fix는 reproduction test, regression test, performance/security impact로 검증한다. `그럴듯한 원인 설명`과 `실제 failure cause가 입증됨`을 구분한다. 로그에 없는 값을 AI가 추정한 경우 FACT로 기록하지 않는다.

AI에게 제공한 context가 production build와 다르거나 일부 로그가 누락되면 논리적으로 일관된 오답도 만들 수 있다. 제안한 line change마다 어떤 invariant를 복구하는지, 어떤 test가 그 invariant를 깨뜨릴 수 있는지 명시하게 한다. 여러 가설을 ranking하더라도 실제 우선순위는 비용이 낮고 정보 이득이 큰 실험부터 정한다. tool 실행 결과와 AI 해석을 기록상 분리해 command output을 다시 검토할 수 있게 하고, AI가 생성한 synthetic stack·수치를 실제 telemetry처럼 incident note에 복사하지 않는다.

---

## CHAPTER 31 · 통합 디버깅은 가장 이른 깨진 invariant를 찾는 과정이다

실전 순서는 다음 원칙으로 고정할 수 있다.

```text
증상을 수치화한다
→ failing build/input/environment를 고정한다
→ 정상군과 실패군을 나눈다
→ request timeline을 만든다
→ CPU/wait/memory/I/O 중 병목 범주를 고른다
→ 해당 계층 evidence를 추가한다
→ 가장 이른 invariant violation을 찾는다
→ 최소 수정한다
→ 동일 reproduction과 회귀 범위를 재실행한다
```

수정 후 증상이 사라졌다는 사실만으로 원인을 증명하지 않는다. **원인 가설이 예측한 evidence가 사라지고, 재현 test가 통과하며, 인접 invariant가 유지되는 것**까지 확인해야 디버깅이 끝난다.

가장 이른 invariant는 사용자에게 보인 마지막 오류보다 훨씬 앞에 있을 수 있다. 예를 들어 timeout은 최종 증상이고 실제 첫 위반은 connection pool admission이 bounded하지 않았던 시점일 수 있다. timeline의 각 transition에 precondition/postcondition을 적고 처음으로 false가 된 지점을 찾으면 후속 cascade를 원인과 분리할 수 있다. 수정은 그 지점의 contract를 복구하고, regression suite는 원래 reproduction뿐 아니라 인접 정상 case와 overload/fault case를 포함한다. 마지막으로 기존 가설과 evidence ledger를 정리해 어떤 관측이 원인을 입증했고 어떤 후보가 반증됐는지 남긴다.