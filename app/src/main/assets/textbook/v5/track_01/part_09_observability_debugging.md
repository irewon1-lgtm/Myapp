# PART 09 · 증거 기반 디버깅 — reproduction, timeline, causal proof

디버깅은 원인을 떠올리는 능력이 아니라 **관측 가능한 사실을 만들고 가설을 제거하는 과정**이다. 로그·metric·trace·profile·dump는 각각 다른 질문에 답하므로 한 도구의 결과를 전체 원인으로 확대하지 않는다.

---

## CHAPTER 01 · observation과 hypothesis를 같은 문장에 섞지 않는다

`화면이 4.8초 뒤 나타났다`는 observation이고 `DB가 느리다`는 hypothesis다. 두 문장을 분리해야 새 증거가 나왔을 때 가설을 버릴 수 있다. bug report에는 expected/actual, 시작 시각, input, build, device/environment를 먼저 고정한다.

가설은 반증 가능한 형태로 쓴다. `DB가 느리다`보다 `request latency 4.8초 중 DB span이 3초 이상이며 동일 query의 lock wait가 증가했다`가 강하다. 다음 실험은 이 조건을 측정하도록 설계한다.

---

## CHAPTER 02 · reproduction은 실패 조건을 하나의 실험으로 고정한다

재현 가능한 버그는 input, initial state, build artifact, environment, action sequence를 반복할 수 있어야 한다. `가끔`이라는 표현은 확률이 아니라 통제하지 못한 변수의 존재를 뜻한다.

재현율도 증거다. 100회 중 2회 발생하던 문제가 변경 후 100회 중 0회라고 해서 바로 해결을 증명할 수 없다. confidence를 높이려면 failure probability와 test count를 고려하고 deterministic trigger를 찾는다.

---

## CHAPTER 03 · binary isolation은 원인 공간을 반씩 줄인다

큰 시스템에서 모든 component를 동시에 읽지 않는다. feature, commit, request path, shard, device class를 기준으로 정상군/실패군을 나누면 원인 후보가 급격히 줄어든다.

Git bisect와 같은 원리는 runtime에도 적용된다. middleware 절반을 bypass하거나 data set을 절반으로 줄여 failure가 어느 쪽에 남는지 본다. 단, isolation change가 timing이나 state를 바꾸어 bug를 숨길 수 있으므로 control group을 유지한다.

---

## CHAPTER 04 · log는 문장이 아니라 structured event다

운영 로그는 사람이 읽는 설명문만 남기기보다 timestamp, event name, request ID, principal, resource ID, outcome, duration, error category를 구조화한다. 동일 event schema를 사용하면 query와 aggregation이 가능하다.

민감정보를 그대로 기록하지 않는다. token, password, personal payload는 redaction 정책을 두고 debugging에 필요한 stable identifier만 남긴다. log schema도 외부 데이터처럼 versioning과 compatibility가 필요하다.

---

## CHAPTER 05 · log level은 심각도와 actionability를 표현한다

DEBUG/INFO/WARN/ERROR를 감정적으로 선택하면 운영에서 의미가 무너진다. ERROR는 일반적으로 요청 또는 system invariant가 실제로 실패해 operator/action이 필요한 사건에 사용하고, 예상 가능한 validation reject를 모두 ERROR로 남기면 signal-to-noise가 붕괴한다.

같은 failure를 여러 layer가 중복 ERROR로 기록하면 incident count가 부풀려진다. error ownership layer를 정하고 lower layer는 context를 attach한 뒤 propagate하는 방식으로 중복을 줄인다.

---

## CHAPTER 06 · correlation은 분산된 event를 하나의 request lifetime으로 묶는다

request ID, trace ID, job ID는 여러 process/service의 event를 연결한다. ID가 없으면 timestamp와 payload를 추측해 같은 요청을 찾게 되고 concurrency가 높을수록 잘못 연결할 가능성이 커진다.

correlation ID는 security identity가 아니다. 사용자가 누구인지 증명하거나 authorization을 부여하지 않는다. 동일 request가 retry되어 새 attempt가 생길 때 logical operation ID와 attempt ID를 분리하면 duplicate execution을 분석하기 쉽다.

---

## CHAPTER 07 · stack trace는 실패 시점의 call chain이지 전체 timeline이 아니다

exception stack은 현재 thread가 어떤 call chain을 거쳤는지 보여 준다. optimized code, coroutine, async callback에서는 source-level logical call chain이 physical stack과 다를 수 있다.

stack의 top frame만 고치지 않는다. exception type, root cause chain, input state, thread context를 함께 본다. native crash에서는 symbol file/build ID가 없으면 raw address만 남으므로 release artifact와 symbol을 보존한다.

---

## CHAPTER 08 · thread dump는 wait graph를 만들 때 가치가 커진다

thread dump는 각 thread가 runnable, blocked, waiting, sleeping 중 어떤 상태인지와 stack을 보여 줄 수 있다. 한 장의 dump에서 동일 lock을 기다리는 thread와 owner를 연결하면 contention/deadlock 구조를 찾을 수 있다.

순간 snapshot만으로 starvation을 증명하기 어렵다. 일정 간격의 여러 dump 또는 scheduler trace를 통해 상태가 지속되는지 확인한다. runnable thread가 오래 CPU를 못 받는 문제와 monitor wait를 구분한다.

---

## CHAPTER 09 · crash dump는 process가 사라지기 직전의 machine state를 보존한다

native crash dump/tombstone에는 signal, fault address, register, module mapping, backtrace가 포함될 수 있다. faulting instruction과 memory address relation을 보면 null dereference, use-after-free, stack corruption 후보를 좁힐 수 있다.

ASLR 때문에 address는 실행마다 달라질 수 있으므로 module base와 build ID가 필요하다. crash dump를 source revision, compiler flags, native library version과 연결해야 symbolization 결과를 신뢰할 수 있다.

---

## CHAPTER 10 · metric은 사건이 아니라 population을 본다

counter는 누적 사건 수, gauge는 시점 값, histogram은 분포를 표현한다. request latency를 gauge 평균 하나로 저장하면 tail을 잃는다. retry count를 request count와 분리해야 실제 load amplification을 볼 수 있다.

metric name보다 label cardinality가 운영 비용을 좌우할 수 있다. user ID나 raw URL을 label로 넣으면 time series 수가 폭발한다. bounded dimension만 metric label로 사용하고 high-cardinality detail은 trace/log로 보낸다.

---

## CHAPTER 11 · percentile은 tail latency를 드러내지만 aggregation 방식에 주의한다

p99는 request의 99%가 그 값 이하라는 distribution statistic이다. 평균이 안정적이어도 소수 request가 수초로 늘어나면 사용자 체감과 timeout rate가 악화될 수 있다.

instance별 p99를 다시 평균내면 전체 population p99가 되지 않는다. merge 가능한 histogram/quantile sketch를 사용하거나 raw bucket을 집계한다. sample count가 적은 interval의 high percentile은 불안정할 수 있다.

---

## CHAPTER 12 · RED와 USE는 관측 질문을 구조화한다

request-driven service에서는 Rate, Errors, Duration을 보면 traffic과 failure/latency 변화를 빠르게 파악할 수 있다. resource 관점에서는 Utilization, Saturation, Errors가 CPU, pool, disk, queue 병목을 찾는 출발점이 된다.

framework 이름을 외우는 목적이 아니다. 증상 metric만 보지 말고 demand와 capacity, queue/saturation을 같이 보라는 구조다. latency가 오를 때 request rate, error, queue depth, utilization을 같은 시간축에 놓는다.

---

## CHAPTER 13 · distributed trace는 service boundary의 시간과 causal parent를 연결한다

trace는 request를 span으로 나누어 service, DB, queue, external API의 duration을 연결한다. parent-child 관계가 있으면 단순 timestamp보다 causal path를 이해하기 쉽다.

sampling 때문에 모든 slow request가 trace에 남지 않을 수 있다. head sampling, tail sampling의 trade-off를 이해하고 error/high-latency trace 보존 정책을 설계한다. async queue를 건널 때 trace context propagation을 잃지 않는다.

---

## CHAPTER 14 · profiler는 CPU 시간을 function에 귀속하지만 wait를 설명하지 못할 수 있다

sampling CPU profiler는 주기적으로 instruction pointer/stack을 관찰해 on-CPU time 분포를 추정한다. instrumentation profiler는 함수 진입/종료를 기록해 세밀한 데이터를 얻지만 overhead가 더 클 수 있다.

CPU profile에 hotspot이 없는데 wall latency가 길다면 off-CPU wait를 의심한다. 반대로 profiler overhead가 scheduling을 바꾸어 race를 숨길 수 있으므로 production-safe sampling과 lab instrumentation을 구분한다.

---

## CHAPTER 15 · flame graph는 stack sample의 집계다

flame graph의 width는 일반적으로 sample에 나타난 비중이며 시간 순서가 아니다. 넓은 box가 반드시 느린 단일 call을 뜻하지 않고 매우 자주 실행된 짧은 call일 수도 있다.

top-down으로 request path를, bottom-up으로 CPU-consuming leaf를 본다. recursion과 async stack stitching 여부도 확인한다. wall-clock flame graph와 CPU flame graph는 의미가 다를 수 있다.

---

## CHAPTER 16 · off-CPU 분석은 기다린 이유와 깨운 주체를 찾는다

thread가 sleep, mutex, I/O, scheduler queue에서 시간을 보낸다면 CPU profiler에는 원인이 충분히 보이지 않는다. off-CPU trace는 block 시작, wakeup, 실제 reschedule까지의 구간을 분해한다.

lock owner가 CPU를 못 받아 waiter가 늘어나는 priority/scheduling 문제도 가능하다. wait site와 wakeup source를 연결해야 단순히 `read()`가 느렸다는 오판을 피할 수 있다.

---

## CHAPTER 17 · system trace는 app, scheduler, Binder, frame을 하나의 시간축에 놓는다

Android system trace/Perfetto에서는 main thread slice, Binder transaction, CPU scheduling, frame timeline, I/O event를 함께 볼 수 있다. UI jank가 application code인지 remote Binder service인지 scheduler delay인지 분리할 수 있다.

trace buffer 크기와 category가 너무 많으면 overhead와 data loss가 생긴다. investigation question에 필요한 event를 선택하고 정확한 failure window를 capture한다.

---

## CHAPTER 18 · allocation profile은 누가 memory를 만들었는지 보여 준다

heap size만 보면 allocation churn을 놓친다. allocation profile은 type, allocation site, rate를 보여 주어 short-lived object가 GC pressure를 만드는 path를 찾게 한다.

높은 allocation count가 곧 leak은 아니다. object가 빠르게 수거되면 retention은 낮다. latency issue라면 allocation rate와 GC pause를, memory growth라면 retained object graph를 본다.

---

## CHAPTER 19 · retained size와 dominator는 leak root를 찾는 데 사용한다

object 하나의 shallow size가 작아도 그것이 reference chain의 유일한 root이면 거대한 graph를 살려 둘 수 있다. dominator/retained size는 해당 object가 제거될 때 함께 reclaim 가능한 graph 규모를 추정한다.

leak fix는 큰 object를 직접 찾는 작업이 아니다. GC root에서 왜 reference가 유지되는지 ownership/lifecycle을 추적한다. listener, callback, static cache, native reference가 lifecycle보다 오래 살 수 있다.

---

## CHAPTER 20 · database evidence는 query time을 parse/plan/lock/I/O로 분해한다

slow query에서 SQL text만 수정하기 전에 execution plan, row estimate, actual row count, index usage, buffer/cache hit, lock wait를 확인한다. 같은 query도 data distribution과 parameter에 따라 plan이 달라질 수 있다.

transaction이 오래 열린 경우 query 자체는 짧아도 다른 transaction을 block할 수 있다. DB latency와 application pool wait를 분리한다. connection 획득 시간이 query execution time에 섞이지 않게 span을 나눈다.

---

## CHAPTER 21 · network timing은 DNS, connect, TLS, server, transfer를 분리한다

end-to-end request latency를 하나의 HTTP duration으로만 기록하면 원인을 찾기 어렵다. DNS lookup, TCP/QUIC connection, TLS handshake, request queue, server processing, TTFB, body transfer를 가능한 범위에서 분리한다.

retry가 숨겨져 있으면 사용자는 한 request로 보지만 실제 network attempt는 여러 번일 수 있다. attempt별 status와 timeout 원인을 기록하고 connection reuse 여부도 함께 본다.

---

## CHAPTER 22 · wall clock과 monotonic clock은 용도가 다르다

wall clock은 사람이 보는 날짜/시각과 동기화되며 NTP/manual adjustment로 점프할 수 있다. duration 측정에는 monotonic clock이 적합하다.

분산 시스템의 서로 다른 host timestamp를 exact causal order로 믿으면 clock skew가 문제를 만든다. trace context와 sequence/event relation을 이용하고 wall time은 근사적인 cross-host 정렬에 사용한다.

---

## CHAPTER 23 · Heisenbug는 관측이 timing을 바꾸는 현상까지 포함한다

추가 로그, debugger breakpoint, sanitizer가 thread scheduling과 allocation layout을 바꾸어 race 증상을 사라지게 할 수 있다. instrumentation 후 재현이 안 된다는 사실은 수정 증거가 아니다.

observer effect를 줄이려면 low-overhead trace, sampling, hardware counter를 사용하고 reproduction condition을 유지한다. race detector처럼 의도적으로 execution을 바꾸는 도구는 결과를 보조 증거로 해석한다.

---

## CHAPTER 24 · feature flag와 canary는 변화 범위를 제어하는 실험 도구다

새 code path를 일부 traffic에만 적용하면 control/canary의 metric을 같은 시간대에 비교할 수 있다. 전체 rollback보다 빠르게 hypothesis를 검증하고 blast radius를 제한한다.

flag 자체가 장기간 남으면 두 code path를 유지하는 complexity가 된다. owner, expiration, default state를 관리한다. data migration처럼 되돌릴 수 없는 변화는 flag만으로 rollback되지 않는다.

---

## CHAPTER 25 · Git bisect는 regression boundary를 commit 단위로 찾는다

good와 bad commit 사이에서 test를 자동 실행하면 binary search로 regression introduction point를 찾을 수 있다. test가 deterministic할수록 bisect 결과가 강하다.

build environment와 dependency가 commit 외부에서 변하면 과거 commit을 동일하게 재현하지 못할 수 있다. lockfile, toolchain, artifact source를 고정해 commit 비교가 실제 code difference를 반영하게 한다.

---

## CHAPTER 26 · debugging experiment는 한 번에 한 가설을 바꾼다

여러 설정과 코드를 동시에 바꾸고 증상이 사라지면 어떤 변화가 효과였는지 모른다. experiment는 independent variable, measurement, expected result를 미리 적는다.

negative result도 후보를 제거한 증거다. 실험 결과를 기록하면 같은 추측을 반복하지 않는다. production에서 실험할 때는 safety limit와 rollback 조건을 먼저 정한다.

---

## CHAPTER 27 · observability 자체도 CPU, storage, network budget을 소비한다

모든 request의 full payload와 stack을 기록하면 system을 관찰하기 위해 system을 망가뜨릴 수 있다. sampling, aggregation, retention tier로 비용을 통제한다.

trace span과 log field에 high-cardinality 데이터를 무제한 넣으면 backend 비용이 폭증한다. debugging 가치가 높은 field와 개인정보/비용 risk를 함께 평가한다.

---

## CHAPTER 28 · alert는 원인 추측보다 사용자 영향에 가까워야 한다

CPU 80% 하나로 alert하면 정상 batch workload에서도 noise가 발생할 수 있다. availability, latency, correctness처럼 SLO/user impact에 가까운 signal을 primary alert로 두고 resource metric은 diagnosis에 사용한다.

alert마다 owner, severity, runbook, deduplication, auto-resolve 조건을 둔다. action 없는 alert는 운영자가 무시하게 되고 실제 incident signal도 묻힌다.

---

## CHAPTER 29 · incident timeline은 사실과 결정의 순서를 보존한다

incident 중에는 symptom 시작, alert, deploy, mitigation, recovery를 timestamp와 evidence link로 기록한다. 사후 기억만으로 timeline을 재구성하면 hindsight bias가 생긴다.

postmortem은 사람의 실수를 단일 root cause로 끝내지 않고 detection gap, unsafe default, missing rollback, capacity assumption 같은 system condition을 찾는다. action item은 owner와 검증 방법을 가져야 한다.

---

## CHAPTER 30 · AI debugging은 가설 생성에 쓰고 사실 판정에는 evidence를 요구한다

AI가 stack trace나 code를 보고 가능한 원인을 빠르게 나열할 수 있지만 실행하지 않은 path와 실제 runtime state를 알 수 없는 경우가 많다. 제안마다 `이 가설이 맞다면 어떤 observable이 보여야 하는가`를 요구한다.

AI가 만든 fix는 reproduction test, regression test, performance/security impact로 검증한다. `그럴듯한 원인 설명`과 `실제 failure cause가 입증됨`을 구분한다. 로그에 없는 값을 AI가 추정한 경우 FACT로 기록하지 않는다.

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