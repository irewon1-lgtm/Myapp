# PART 09 · 추측하지 않고 증거로 디버깅한다 — 로그, stack, profile, trace, metric

개발자가 가장 많이 낭비하는 시간은 `원인이 아마 이것일 것`이라는 첫 추측을 고치는 데 들어간다. 시스템은 여러 계층이 연결돼 있으므로 같은 증상이 전혀 다른 원인에서 나올 수 있다.

```text
앱이 느림
→ CPU 병목일 수도 있음
→ main thread lock 대기일 수도 있음
→ network tail latency일 수도 있음
→ storage writeback일 수도 있음
→ GC pressure일 수도 있음
```

이 PART의 목표는 디버깅 도구 이름을 외우는 것이 아니라 **증상을 재현 가능한 관측으로 바꾸고, 시간축과 causal chain으로 범위를 줄이는 방법**을 만드는 것이다.

---

## CHAPTER 01 · 증상과 원인을 한 문장에 섞지 않는다

잘못된 bug report:

```text
DB가 느려서 앱이 멈춤
```

여기에는 아직 검증되지 않은 원인이 들어 있다.

더 좋은 기록:

```text
2026-09-17 build X에서
주문 상세 진입 후 3~8초 동안 터치 반응이 없고
main 화면 frame update가 멈춘다.
재현율 7/10.
```

이렇게 쓰면 관찰과 가설이 분리된다.

### 관찰

직접 측정/재현된 사실.

### 가설

관찰을 설명하기 위한 후보.

### 증거

가설을 지지하거나 반박하는 측정값.

세 칸을 섞지 않는다.

---

## CHAPTER 02 · 재현 조건을 입력 변수로 만든다

`가끔 발생`은 테스트 조건이 아니다.

다음 축을 기록한다.

```text
device/model
OS version
app build/commit
account/data size
network type
battery/thermal state
cold/warm start
permissions
locale/timezone
exact steps
frequency
```

### 최소 재현

원래 20단계에서 발생하는 bug라면 step을 하나씩 제거한다.

```text
20 step
→ 12 step
→ 7 step
→ 3 step
```

최소 재현은 단순히 QA 문서를 짧게 하는 작업이 아니다.

**원인에 필요한 조건과 우연히 같이 있던 조건을 분리하는 실험**이다.

---

## CHAPTER 03 · binary search는 code뿐 아니라 원인 공간에도 쓴다

가능한 layer가 많다고 하나씩 순서대로 전부 보지 않는다.

```text
UI
↓
view model
↓
repository
↓
DB/network
```

중간 boundary에서 timestamp와 input/output을 관측한다.

```text
UI→repository: 5ms
repository→HTTP: 20ms setup
HTTP wait: 1800ms
JSON parse: 30ms
UI render: 12ms
```

이제 1.8초의 핵심 구간은 HTTP wait다.

### divide and conquer debugging

시스템 중간에서 `여기까지 정상인가?`를 반복하면 탐색 범위를 절반씩 줄일 수 있다.

---

## CHAPTER 04 · 로그는 사건 기록이지 프로그램 설명문이 아니다

좋지 않은 로그:

```text
here
worked
error
value = 3
```

좋은 event log는 질문에 답한다.

```text
무슨 operation?
어떤 entity/request?
언제 시작/끝?
결과 status?
어떤 failure class?
얼마나 걸림?
```

예:

```text
payment.authorize
requestId=r-123
orderId=o-77
attempt=2
latencyMs=842
result=timeout
```

### 구조화 로그

문자열 문장을 parser로 다시 뜯기보다 key/value field로 남기면 query와 aggregation이 쉽다.

```json
{
  "event": "payment.authorize",
  "requestId": "r-123",
  "latencyMs": 842,
  "result": "timeout"
}
```

### PII/secret를 로그에 넣지 않는다

password, access token, full card data, private health data 같은 민감정보는 debugging 편의보다 노출 위험이 크다.

필요한 correlation ID와 non-sensitive metadata로 추적한다.

---

## CHAPTER 05 · log level은 심각도와 운영 비용을 통제한다

일반적인 분류:

```text
DEBUG
INFO
WARN
ERROR
```

정확한 정책은 조직마다 다르다.

### ERROR 남발

retry로 자동 복구되는 transient failure를 매번 ERROR로 남기면 alert fatigue가 생긴다.

반대로 data corruption 가능성을 DEBUG로 숨기면 중요한 사건을 놓친다.

level은 `개발자가 놀란 정도`가 아니라 **운영자가 행동해야 하는 정도**와 연결한다.

---

## CHAPTER 06 · correlation ID가 분산된 사건을 한 요청으로 묶는다

사용자 한 번의 버튼 클릭이 여러 service를 통과할 수 있다.

```text
mobile app
→ API gateway
→ order service
→ payment service
→ DB
```

각 system log timestamp만 보고 수동으로 맞추면 어렵다.

request/correlation ID를 propagation하면 같은 logical request를 묶을 수 있다.

### ID를 새로 만들지 이어 전달할 때

service마다 완전히 새 ID만 만들면 parent-child 관계를 잃는다.

trace context와 span ID 같은 구조는 하나의 request tree를 표현한다.

---

## CHAPTER 07 · stack trace는 `어디서 죽었나`와 `어떻게 여기 왔나`를 보여 준다

exception stack:

```text
parseAmount()
called by createOrder()
called by onSubmit()
```

현재 failure point뿐 아니라 call chain을 보여 준다.

### 가장 위 한 줄만 읽지 않는다

wrapper exception이 여러 층으로 감싸질 수 있다.

```text
UI exception
caused by repository exception
caused by SQL constraint violation
```

root cause chain을 끝까지 본다.

### stack에 없는 원인

data race, stale remote response, corrupted persistent file처럼 원인이 이전 시점에 발생한 경우 현재 stack은 결과만 보여 줄 수 있다.

stack trace는 강력하지만 완전한 역사 기록은 아니다.

---

## CHAPTER 08 · thread dump는 기다림 관계를 본다

hang/deadlock에서는 한 thread stack만 부족하다.

동시에 여러 thread state를 capture한다.

```text
Main: waiting Future.get
Worker-1: waiting DB pool
Worker-2: holds DB connection, waiting main callback
```

이제 wait cycle 후보가 보인다.

### timestamp가 같은 snapshot

5초 간격으로 각 thread를 따로 수집하면 상태가 바뀔 수 있다.

가능한 한 같은 시점의 thread dump로 wait graph를 만든다.

---

## CHAPTER 09 · crash dump/core/tombstone은 process가 죽는 순간의 낮은 수준 상태를 보존한다

native crash에서 source exception stack이 없을 수 있다.

register, fault address, native backtrace, loaded module, signal 같은 정보가 필요하다.

### symbolization

native address:

```text
0x7a12bc...
```

만 보면 source function을 알기 어렵다.

정확한 build의 symbol/debug information으로 address를 function/file/line에 mapping해야 한다.

### build mismatch

다른 version symbol을 사용하면 그럴듯하지만 틀린 stack이 나올 수 있다.

crash artifact와 exact build ID/version을 보존해야 한다.

---

## CHAPTER 10 · metric은 시간에 따른 시스템 상태를 수량화한다

예:

```text
request count
error rate
CPU utilization
heap usage
queue depth
DB connections
cache hit ratio
latency histogram
```

### gauge/counter/histogram

counter는 누적 사건 수, gauge는 현재 값, histogram/distribution은 값의 분포를 표현한다.

모든 것을 평균 하나로 만들지 않는다.

---

## CHAPTER 11 · 평균 latency는 tail을 숨긴다

요청 100개의 latency:

```text
99개 = 50ms
1개 = 5000ms
```

평균은 약 99.5ms지만 한 사용자는 5초를 경험했다.

p50/p95/p99 같은 percentile을 보면 tail을 드러낼 수 있다.

### percentile aggregation 주의

각 server의 p99를 평균내서 전체 p99라고 부를 수 없다.

원래 distribution/histogram을 적절히 aggregate해야 한다.

---

## CHAPTER 12 · rate, error, duration을 함께 본다

서비스 요청을 본다면 최소한:

```text
rate
= 얼마나 들어오는가

errors
= 얼마나 실패하는가

duration
= 얼마나 오래 걸리는가
```

를 함께 본다.

latency가 좋아졌는데 request가 절반 drop된 것일 수도 있다.

성능 metric 하나의 개선을 성공으로 선언하지 않는다.

---

## CHAPTER 13 · trace는 한 요청의 시간 구간을 연결한다

trace span:

```text
request 1200ms
├─ auth 20ms
├─ DB query 80ms
├─ downstream API 900ms
└─ render response 30ms
```

숫자 합이 정확히 parent duration과 같지 않을 수 있다. parallel span, scheduling gap, instrumentation overhead가 있기 때문이다.

### critical path

전체 latency를 줄이려면 가장 긴 causal path를 찾는다.

parallel work 3개 중 가장 짧은 하나를 50% 줄여도 total latency는 그대로일 수 있다.

---

## CHAPTER 14 · sampling profiler와 instrumentation profiler를 구분한다

### sampling

주기적으로 execution stack을 샘플링해 CPU 시간을 어디서 쓰는지 추정한다.

overhead가 비교적 낮아 production-like workload에 유리할 수 있다.

### instrumentation

function entry/exit 등에 측정 코드를 넣어 호출과 duration을 더 직접 기록할 수 있다.

세밀하지만 overhead와 timing distortion이 커질 수 있다.

### profiler가 프로그램을 바꾼다

관측 자체가 timing/cache/scheduling을 바꿀 수 있다.

특히 concurrency bug에서 profiler 켜면 bug가 사라질 수도 있다.

---

## CHAPTER 15 · flame graph는 stack sample을 넓이로 본다

개념:

```text
width = sample에서 차지한 비율
vertical = call stack depth
```

넓은 block이 CPU sample을 많이 차지한 path 후보다.

### 가장 위 함수만 고치지 않는다

넓은 leaf function이 library memcpy라면 실제 원인은 caller가 너무 큰 data를 너무 자주 복사하는 구조일 수 있다.

call path 전체를 본다.

---

## CHAPTER 16 · CPU profile과 wall-time profile은 다르다

thread가 network를 2초 기다리면 wall time은 2초지만 CPU time은 거의 없을 수 있다.

CPU profiler에는 병목이 안 보인다.

```text
wall latency 2s
CPU 20ms
wait 1980ms
```

이럴 때 scheduler/I/O trace가 필요하다.

### off-CPU analysis

thread가 실행되지 않는 시간을 분류한다.

```text
sleep
lock wait
I/O wait
run queue wait
```

`느리다 = CPU profile` 공식에서 벗어난다.

---

## CHAPTER 17 · system trace는 scheduler와 I/O까지 시간축에 놓는다

Android/Linux system trace에서는 thread running/runnable/sleep state, CPU scheduling, I/O event, frame event 등을 시간축으로 볼 수 있다.

### main thread gap

```text
main runnable but not running
→ CPU contention/scheduling 문제 후보

main sleeping on futex
→ lock/condition wait 후보

main running 200ms continuously
→ CPU-heavy work 후보
```

같은 200ms freeze도 원인이 다르다.

---

## CHAPTER 18 · allocation profile은 object count보다 lifetime을 본다

object 100만 개를 매우 짧게 만들고 즉시 수거하는 workload와 object 1만 개가 계속 retained되는 leak은 다른 문제다.

관측:

```text
allocation rate
live object count
retained size
GC frequency
pause time
```

### dominant type

특정 bitmap/string/list가 memory 대부분을 차지하면 왜 그 object가 살아 있는지 reference path를 추적한다.

---

## CHAPTER 19 · heap dump에서 retained size를 본다

object 자체는 100byte여도 그 object가 root에서 큰 graph를 잡아 두면 retained size가 수백 MB일 수 있다.

```text
Activity
↓ listener
↓ repository
↓ cache
↓ huge bitmap graph
```

작은 reference 하나가 큰 memory를 유지할 수 있다.

### GC root

왜 수거되지 않는지 알려면 root까지 reference chain을 본다.

`object가 있다`보다 `누가 아직 참조하는가`가 핵심이다.

---

## CHAPTER 20 · DB slow query는 query text만 보지 않는다

필요한 evidence:

```text
actual parameters
query plan
rows examined/returned
index used?
lock wait?
cache state?
I/O latency?
transaction context?
```

같은 SQL도 parameter selectivity와 data distribution에 따라 plan이 달라질 수 있다.

DB 파트에서 더 깊게 배우지만 관측 원칙은 동일하다.

---

## CHAPTER 21 · network latency를 DNS/TCP/TLS/server/download로 분해한다

`HTTP 2초`는 하나의 시간값이 아니다.

```text
DNS
connect
TLS handshake
request queue/upload
server processing
TTFB
response download
client parse
```

어느 구간이 2초인지 알아야 최적화 대상이 정해진다.

### retry가 latency를 숨길 수 있다

첫 request가 timeout 후 자동 retry 성공하면 최종 status는 200이어도 user latency가 길어진다.

attempt count를 trace/log에 남긴다.

---

## CHAPTER 22 · clock과 timestamp에도 오류가 있다

wall clock은 NTP/user 설정으로 앞으로/뒤로 조정될 수 있다.

duration 측정에는 monotonic clock이 적합하다.

```text
wall clock
→ 실제 시각 기록

monotonic clock
→ elapsed duration
```

분산 시스템에서는 machine별 clock skew도 고려한다.

trace ordering을 timestamp 하나만으로 절대 진리처럼 사용하지 않는다.

---

## CHAPTER 23 · 로그를 추가해 bug가 사라졌다면 중요한 증거다

로그 I/O는 thread scheduling과 timing을 바꾼다.

race condition이 사라질 수 있다.

이때 `재현 안 됨`으로 닫지 않는다.

오히려 timing-sensitive race 가능성을 높이는 evidence다.

### low-intrusion instrumentation

가능하면 tracing buffer, sampling, counter처럼 timing perturbation이 적은 방법을 사용한다.

---

## CHAPTER 24 · feature flag와 canary는 원인 격리 도구가 될 수 있다

production bug가 새 feature 이후 발생했다고 하자.

전체 rollback 대신 일부 traffic에서 feature를 끄고 metric을 비교할 수 있다.

```text
flag ON cohort
error 5%

flag OFF cohort
error 0.1%
```

강한 correlation evidence가 된다.

하지만 cohort의 user/data distribution이 다르면 confounder가 생길 수 있다.

---

## CHAPTER 25 · bisect는 회귀 commit을 찾는다

좋았던 commit G와 나쁜 commit B 사이가 크다고 하자.

중간 commit을 build/test하며 범위를 절반씩 줄인다.

```text
G -------- M -------- B
            test
```

재현 테스트가 deterministic할수록 강력하다.

flaky test로 bisect하면 잘못된 commit을 지목할 수 있다.

---

## CHAPTER 26 · experiment에는 control이 필요하다

성능 변경 전후 비교에서 동시에 여러 것을 바꾸지 않는다.

```text
before: old cache + old DB + debug build

after: new cache + new DB + release build
```

이 비교로 무엇이 효과였는지 모른다.

한 번에 하나의 주요 variable을 바꾸거나 factorial experiment를 설계한다.

---

## CHAPTER 27 · observability overhead도 budget을 가진다

모든 function에 full log/trace를 남기면:

```text
CPU overhead
allocation
I/O
network cost
storage cost
privacy risk
```

가 커진다.

sampling rate, aggregation, retention, redaction을 설계한다.

### cardinality explosion

metric label에 userId/requestId처럼 거의 무한한 값을 넣으면 time-series 수가 폭발한다.

high-cardinality identity는 trace/log가 더 적합할 수 있다.

---

## CHAPTER 28 · alert는 symptom 중심으로 설계한다

`CPU 80%`가 항상 incident는 아니다.

사용자 영향과 직접 연결된 SLI를 우선한다.

예:

```text
request success rate
p99 latency
checkout completion
freshness
```

resource metric은 원인 진단에 유용하지만 symptom alert와 구분한다.

---

## CHAPTER 29 · incident timeline은 원인보다 먼저 사실을 고정한다

```text
10:02 deploy start
10:05 error rate 0.2%→8%
10:07 oncall alerted
10:10 rollback start
10:14 error back to 0.3%
```

이 timeline은 나중에 `누가 실수했나`보다 causal relation을 분석하는 기반이다.

### postmortem

좋은 postmortem은 개인 비난보다 system condition을 분석한다.

```text
trigger
contributing factors
why detection late
why blast radius large
what safeguard missing
```

---

## CHAPTER 30 · AI가 제시한 원인도 같은 증거 규칙을 적용한다

AI가 log를 보고 `race condition 같습니다`라고 말해도 그것은 hypothesis다.

요구해야 할 것:

```text
어떤 log line이 근거인가?
어떤 alternate hypothesis가 있는가?
무슨 추가 측정으로 둘을 구분하는가?
재현 test는 무엇인가?
수정 후 어떤 regression test가 필요한가?
```

AI가 자신 있게 말하는 정도와 evidence quality는 별개다.

---

## CHAPTER 31 · 종합 장애 추적

증상:

> 앱에서 사진 업로드를 누르면 5% 확률로 10초 멈춘 뒤 실패한다.

### 1. UI trace

main thread는 10초 동안 running이 아니라 lock wait.

### 2. wait graph

main waits `uploadStateMutex`.

worker owns mutex while synchronous network upload 중.

### 3. network trace

packet loss 상황에서 retry/backoff로 10초.

### 4. code review

worker가 mutex 안에서 network call 수행.

### root mechanism

network 자체의 지연이 main freeze가 된 이유는 **긴 external I/O를 잡은 채 UI가 필요한 shared lock을 보유했기 때문**이다.

### 수정

network I/O를 lock 밖에서 수행하고, lock 안에서는 version 확인 + 짧은 state commit만 한다.

### 검증

```text
packet loss injection
50회
main frame responsiveness
upload correctness
cancel behavior
state race
```

하나의 layer만 봤으면 `network 느림`으로 끝났을 문제다.

---

## PART 09 종료 점검

1. observation과 hypothesis를 왜 분리해야 하는가?
2. 최소 재현이 원인 공간을 어떻게 줄이는가?
3. 구조화 로그와 correlation ID가 왜 필요한가?
4. stack trace가 보여 주지 못하는 history 문제에는 무엇이 있는가?
5. thread dump에서 wait graph를 만드는 이유는 무엇인가?
6. native crash symbolization에서 exact build가 왜 필요한가?
7. 평균 latency가 tail latency를 숨기는 예를 설명할 수 있는가?
8. CPU profiler에 network wait가 잘 안 보이는 이유는 무엇인가?
9. sampling과 instrumentation profiler의 trade-off는 무엇인가?
10. heap dump에서 retained size와 GC root를 왜 보는가?
11. system trace에서 running/runnable/sleep state는 어떻게 원인 후보를 나누는가?
12. duration 측정에 monotonic clock을 쓰는 이유는 무엇인가?
13. logging 후 race가 사라졌을 때 왜 bug가 해결됐다고 하면 안 되는가?
14. metric high-cardinality가 왜 운영 비용을 폭발시키는가?
15. AI의 debugging 답을 hypothesis로 다뤄야 하는 이유는 무엇인가?

이제 디버깅을 `코드 보면서 이상한 곳 찾기`로 정의하지 않는다. **재현 → 관측 → 시간축 → 가설 → 분리 실험 → 수정 → 회귀 검증**의 증거 과정으로 정의한다.