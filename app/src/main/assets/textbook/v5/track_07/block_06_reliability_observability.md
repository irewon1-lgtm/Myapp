# TRACK 07 · 서버와 백엔드

# BLOCK 06 · 느림·과부하·장애를 숫자와 증거로 다루기

서버가 `정상 실행된다`와 `사용자가 안정적으로 쓸 수 있다`는 다르다. request가 50ms에 끝나다가 load가 조금 늘었을 때 10초로 튈 수도 있고, dependency 하나의 장애가 retry 폭주로 전체 시스템을 무너뜨릴 수도 있다.

이번 BLOCK에서는 운영 도구 이름을 외우기보다 다음 질문을 반복한다.

```text
얼마나 오래 걸렸는가?
얼마나 많이 처리하는가?
어디서 기다리는가?
실패가 어느 dependency에서 시작됐는가?
지금 더 받아도 되는가?
증거를 같은 시간축에 연결할 수 있는가?
```

---

## LESSON 76 · latency·throughput·concurrency는 서로 다른 숫자다

### 1. latency

request 한 건이 시작해서 끝날 때까지 걸린 시간이다.

### 2. throughput

일정 시간 동안 처리한 작업 수다.

```text
requests/second
jobs/minute
MB/second
```

### 3. concurrency

동시에 진행 중인 작업 수다.

한 request가 평균 1초 걸리고 초당 100개가 들어오면 단순한 직관으로도 약 100개의 작업이 겹칠 수 있다. 실제 system에는 queue와 burst가 있어 더 복잡하다.

### 4. 평균만 보면 tail이 사라진다

100개 request 중 99개가 10ms, 한 개가 10초면 평균만으로 사용자 경험을 설명하기 어렵다. p50/p95/p99 같은 percentile을 함께 본다.

### 5. 직접 실험

handler에 random delay를 넣고 100회 호출한다. 평균, p50, p95, p99를 계산한다. 한두 개 slow request가 percentile에 어떤 영향을 주는지 본다.

### 6. 작은 문제

평균 latency가 줄었는데 p99가 크게 늘었다. `성능이 좋아졌다`고 하나의 문장으로 결론 내릴 수 있는가?

아니다. 어떤 사용자/operation이 tail에 몰리는지 추가 분석이 필요하다.

---

## LESSON 77 · Node.js event loop를 막는 CPU 작업은 다른 request까지 지연시킬 수 있다

### 1. async I/O와 CPU 계산을 구분한다

DB/network를 기다리는 동안 Node.js는 다른 callback을 처리할 수 있다. 하지만 JavaScript thread에서 긴 CPU loop를 돌리면 event loop가 다른 request handler를 실행하지 못한다.

```ts
app.get("/slow", (_req, res) => {
  let total = 0;
  for (let i = 0; i < 5_000_000_000; i++) total += i;
  res.json({ total });
});
```

### 2. 한 route의 문제가 전체 latency가 된다

`/slow` 하나를 호출하는 동안 가벼운 `/health`도 늦어질 수 있다.

### 3. 해결 선택지

```text
algorithm 개선
작업 chunking
worker thread/process
별도 compute service
native/optimized library
queue 기반 background work
```

무조건 worker thread를 넣기 전에 CPU profile로 실제 bottleneck인지 확인한다.

### 4. JSON parse/serialize도 CPU다

아주 큰 payload는 parsing과 serialization 자체가 event loop를 오래 점유할 수 있다. body size limit이 reliability와도 연결된다.

### 5. 직접 실험

CPU-heavy route와 fast route를 동시에 호출하고 fast route latency가 어떻게 변하는지 측정한다.

---

## LESSON 78 · connection pool은 연결 재사용과 동시 사용량 제한을 동시에 한다

### 1. DB connection은 공짜가 아니다

매 request마다 새 TCP/TLS/DB session을 만들면 overhead가 크다. pool은 미리 만든 연결을 재사용한다.

### 2. pool size가 클수록 무조건 빠른가?

아니다. DB가 감당할 수 있는 concurrent query보다 많은 connection이 몰리면 lock/contention/context switching이 늘 수 있다.

### 3. pool wait time

request가 query 실행 전 pool connection을 기다릴 수 있다.

```text
request latency
= app queue
+ pool wait
+ DB query
+ network
+ serialization ...
```

### 4. 여러 instance가 pool을 합산한다

instance 20개 × pool 50이면 최대 1000 connection이 될 수 있다. autoscaling과 DB connection capacity를 함께 계산한다.

### 5. leak

connection을 반환하지 않는 code path가 있으면 pool이 서서히 고갈된다.

### 6. 작은 문제

DB CPU가 낮으니 pool을 10배로 키우면 안전한가?

query lock, I/O, max connections, memory, downstream dependency를 같이 봐야 한다.

---

## LESSON 79 · timeout은 실패를 만드는 기능이 아니라 기다림의 최대 budget을 명시한다

### 1. timeout이 없으면

외부 dependency가 hang할 때 request가 connection, memory, worker slot을 오래 점유할 수 있다.

### 2. 여러 timeout을 구분한다

```text
connect timeout
request/read timeout
DB statement timeout
queue lease timeout
overall request deadline
```

### 3. 내부 timeout은 바깥 deadline보다 짧아야 한다

사용자 request budget이 2초인데 downstream call 하나를 10초 기다리도록 두면 이미 사용자 deadline을 넘긴다.

```text
전체 2s
→ auth 100ms
→ DB 500ms
→ external API 800ms
→ response budget ...
```

### 4. timeout은 결과 미확정일 수 있다

특히 side effect API에서 timeout은 상대가 실행하지 않았다는 뜻이 아니다. BLOCK 02/05의 idempotency와 reconciliation이 연결된다.

### 5. 직접 실습

fake dependency를 100ms, 1s, never response 세 모드로 만들고 client timeout을 바꾸며 resource가 얼마나 오래 잡히는지 본다.

---

## LESSON 80 · retry는 가용성을 높일 수도 있고 장애를 증폭할 수도 있다

### 1. 일시 실패는 retry로 회복할 수 있다

짧은 network glitch나 503이 한 번 발생했을 수 있다.

### 2. 모든 layer가 retry하면 곱셈된다

```text
client 3회
API gateway 3회
service A 3회
service B 3회
```

최악에는 원래 한 의도가 매우 많은 downstream call로 늘 수 있다.

### 3. retry budget

attempt 수뿐 아니라 전체 deadline과 추가 load 비율을 제한한다.

### 4. backoff + jitter

장애 직후 모든 request가 같은 간격으로 재시도하면 recovering dependency를 다시 압박한다.

### 5. idempotency 확인

state-changing operation을 retry하기 전에 duplicate side effect 가능성을 검토한다.

### 6. 작은 문제

503이면 무조건 10회 retry하는 정책이 왜 위험한가?

지속 장애에서 10배 traffic을 만들고 사용자 latency도 길어진다.

---

## LESSON 81 · circuit breaker와 bulkhead는 서로 다른 failure propagation을 제한한다

### 1. circuit breaker

계속 실패하는 dependency를 일정 기간 빠르게 거절해 불필요한 호출과 resource 점유를 줄이는 pattern이다.

```text
CLOSED → 실패율 증가 → OPEN
OPEN → 호출 차단
시간 후 HALF_OPEN → 제한 시험
성공 시 CLOSED
```

### 2. breaker threshold가 어려운 이유

traffic이 매우 적을 때 2번 실패를 100% 실패율로 볼지, 특정 error class만 셀지 정책이 필요하다.

### 3. bulkhead

하나의 dependency/tenant/workload가 모든 thread/connection/queue capacity를 먹지 않도록 resource pool을 나누거나 concurrency limit을 둔다.

### 4. breaker가 root cause를 고치는 것은 아니다

dependency outage를 감추는 안전장치다. alert와 recovery evidence가 필요하다.

### 5. 작은 문제

payment와 avatar resize가 같은 worker pool을 쓰고 resize가 폭주해 payment도 처리 못 한다. 어떤 pattern이 연관되는가?

workload isolation/bulkhead가 연관된다.

---

## LESSON 82 · load shedding과 backpressure는 감당 못 할 일을 빨리 줄이는 방법이다

### 1. queue가 무한히 늘어나면

메모리와 latency가 계속 증가하고 오래된 request는 성공해도 이미 가치가 없을 수 있다.

### 2. backpressure

생산자에게 소비 속도를 맞추라고 신호를 보내거나 concurrency를 제한한다.

### 3. load shedding

capacity를 넘은 요청 일부를 명확히 거절해 핵심 traffic을 보호한다.

```text
429 Too Many Requests
503 Service Unavailable
queue full rejection
```

### 4. priority

로그인, 결제 같은 핵심 요청과 저우선순위 report를 다른 budget으로 둘 수 있다.

### 5. admission control

작업을 시작한 뒤 중간에 죽는 것보다 시작 전에 capacity를 확인해 거절하는 편이 낫기도 하다.

### 6. 작은 문제

`queue가 있으니 burst는 무제한 받아도 된다`는 왜 틀렸는가?

queue storage와 waiting deadline도 유한하다. backlog age가 사용자 SLO를 넘으면 이미 실질 실패다.

---

## LESSON 83 · liveness·readiness·startup health는 질문이 다르다

### 1. liveness

process가 교착/치명 상태라 재시작이 필요한지 묻는다.

### 2. readiness

지금 새 traffic을 받아 제대로 처리할 준비가 됐는지 묻는다.

DB migration, cache warmup, shutdown drain 중에는 process가 살아 있어도 not ready일 수 있다.

### 3. startup

초기화가 오래 걸리는 application에서 liveness가 너무 빨리 실패해 무한 restart되지 않도록 별도 startup probe 개념을 사용할 수 있다.

### 4. 모든 dependency를 liveness에 넣는 함정

DB가 잠깐 죽었다고 모든 app instance를 동시에 재시작하면 outage를 악화시킬 수 있다.

### 5. health endpoint 자체의 비용

매초 DB full query를 수행하는 health check 수천 개가 실제 load가 될 수 있다.

### 6. 작은 문제

`/health`가 200이면 service가 정상이라고 단정할 수 있는가?

health endpoint가 무엇을 검사하는지에 따라 다르다. 실제 user path synthetic check와 application metrics가 필요할 수 있다.

---

## LESSON 84 · structured log는 문장을 읽는 것이 아니라 field로 검색할 수 있는 사건 기록이다

### 1. 단순 log

```text
order failed
```

어떤 request, order, error인지 알기 어렵다.

### 2. structured log

```json
{
  "level": "error",
  "event": "order.create.failed",
  "requestId": "r1",
  "orderId": "o9",
  "errorType": "DependencyTimeout",
  "durationMs": 812
}
```

### 3. log level과 event를 구분한다

ERROR/WARN/INFO만으로 의미를 표현하지 않는다. stable event name과 fields를 둔다.

### 4. secret/PII redaction

Authorization, cookie, password, token, 카드정보를 무심코 log에 남기지 않는다.

### 5. sampling

고traffic endpoint의 모든 success log를 영원히 저장하면 비용이 커질 수 있다. 오류는 보존하고 성공은 sampling하는 정책 등을 사용할 수 있다.

### 6. 작은 문제

error object를 JSON stringify하면 모든 useful field와 stack이 자동 보존되는가?

library에 따라 Error property가 enumerable이 아니어서 비어 보일 수 있다. logger의 error serialization contract를 확인한다.

---

## LESSON 85 · metrics는 시간에 따라 시스템 행동을 숫자로 본다

### 1. request 서비스의 기본 관점

RED 방법을 예로 들 수 있다.

```text
Rate      → 요청량
Errors    → 실패량/비율
Duration  → latency distribution
```

### 2. resource 관점

CPU, memory, disk, network, pool usage, queue depth 같은 saturation을 함께 본다.

### 3. counter·gauge·histogram 감각

```text
counter   → 누적 증가량
 gauge    → 현재값
histogram → 값 분포
```

### 4. 성공률 denominator

`errors / requests`에서 어떤 status/error를 실패로 셀지 service objective와 맞춘다. user validation 400을 server reliability failure에 포함할지 별도로 볼 수 있다.

### 5. rate를 직접 누적값으로 보지 않는다

process restart로 counter가 reset될 수 있다. monitoring system의 rate function이 이를 어떻게 처리하는지 이해한다.

### 6. 작은 문제

CPU 30%인데 service가 느리다. CPU 여유가 있으니 saturation이 아니라고 할 수 있는가?

DB pool, lock, network, external dependency, event loop lag 등 다른 resource가 포화일 수 있다.

---

## LESSON 86 · distributed trace는 request가 여러 component를 지날 때 시간을 span으로 연결한다

### 1. trace와 span

```text
Trace: checkout request
├─ Span: API server
├─ Span: DB query
├─ Span: payment HTTP
└─ Span: cache lookup
```

### 2. parent-child 관계

어떤 작업이 어떤 호출에서 파생됐는지 context를 전파한다.

### 3. OpenTelemetry HTTP semantic convention

HTTP server span은 method, route template, status 등 표준화된 attribute를 사용한다. route는 `/users/12345` 같은 고유 path보다 `/users/:id`처럼 low-cardinality template을 쓰는 것이 중요하다.

### 4. 모든 함수에 span을 만들 필요는 없다

의미 있는 latency/failure boundary를 고른다. 지나치게 세밀하면 비용과 noise가 커진다.

### 5. async message context

queue를 건너면 producer/consumer span 관계와 message context propagation 방식이 HTTP와 다를 수 있다.

### 6. 직접 실습

API→fake DB→fake payment 세 단계에 span을 만들고 payment에 delay를 넣어 waterfall에서 병목이 보이는지 확인한다.

---

## LESSON 87 · request ID와 trace ID는 목적이 겹치지만 같은 개념으로 고정할 필요는 없다

### 1. request ID

application이 요청 한 건을 log에서 묶기 위해 자체 생성할 수 있다.

### 2. trace ID

observability system에서 distributed trace 전체를 묶는 식별자다. 하나의 trace 안에 여러 server/client span이 있을 수 있다.

### 3. 외부에서 받은 ID를 무조건 신뢰하지 않는다

client가 `X-Request-Id`를 마음대로 보낼 수 있다면 log injection/cardinality 문제가 될 수 있다. format/length를 제한하거나 내부 ID를 별도로 생성한다.

### 4. response에 correlation ID 제공

support가 사용자에게 받은 ID로 server log를 찾는 데 유용할 수 있다.

### 5. 작은 문제

trace sampling에서 trace가 저장되지 않았으면 request log도 반드시 없어야 하는가?

아니다. log와 trace는 별도 signal이며 retention/sampling 정책이 다를 수 있다. correlation field로 연결한다.

---

## LESSON 88 · high-cardinality label은 metric 시스템을 망가뜨릴 수 있다

### 1. 좋은 metric label 예

```text
route=/users/:id
method=GET
status_class=2xx
```

### 2. 위험한 label

```text
user_id=각 사용자
request_id=매 요청
raw_url=/users/123?...unique...
```

unique value가 폭발해 time series 수가 매우 커질 수 있다.

### 3. log/trace와 metric의 역할 분리

개별 request ID는 log/trace에 두고 metric label은 낮은 cardinality dimension으로 유지한다.

### 4. tenant label도 주의

tenant가 몇 개인지, 동적 증가하는지에 따라 metric label로 넣는 것이 위험할 수 있다. top-N이나 별도 usage pipeline을 고려한다.

### 5. 작은 문제

HTTP span name에 raw path `/orders/123456789`를 사용하면 어떤 문제가 있는가?

관측 도구에서 고유 span name/cardinality가 폭발할 수 있다. route template을 사용한다.

---

## LESSON 89 · SLI·SLO·error budget은 `얼마나 좋은 서비스가 필요한가`를 수치로 합의하는 방법이다

### 1. SLI

실제 측정 지표다.

```text
successful request ratio
p99 latency
freshness
job completion delay
```

### 2. SLO

목표다.

```text
28일 동안 valid checkout request의 99.9%가 성공
95%가 500ms 안에 완료
```

### 3. 사용자 관점 denominator

health check와 bot traffic을 user request SLI에 섞지 않는 등 population을 명확히 한다.

### 4. error budget

100%와 SLO 사이 허용 실패량을 운영/변경 속도 의사결정에 사용할 수 있다.

### 5. SLO를 모든 internal metric에 만들지 않는다

사용자에게 중요한 service behavior에 집중한다.

### 6. 작은 문제

`99.9% uptime`이라고만 쓰면 충분한가?

어떤 시간창, 어떤 request population, 어떤 status를 success로 보는지 정의가 필요하다.

---

## LESSON 90 · BLOCK 06 실전: `느려졌다`를 evidence chain으로 디버깅한다

### 1. 상황

어제까지 p99 400ms였던 주문 API가 오늘 p99 4초가 됐다. 오류율은 1%에서 5%로 증가했다.

### 2. 바로 코드부터 고치지 않는다

시간축을 맞춘다.

```text
언제 시작했는가
traffic 변화가 있었나
deploy/config change가 있었나
어느 route/tenant/region인가
```

### 3. top-down evidence

```text
request rate/error/duration
↓
server saturation(event loop/CPU/memory/pool)
↓
dependency latency/error
↓
DB query/pool wait
↓
queue backlog
↓
trace slow span
↓
log error class
```

### 4. 가설 A: DB pool 고갈

증거:

```text
DB query 자체 20ms
pool wait p99 3s
active connection=max
```

이 경우 query optimization만 하는 것은 핵심 원인을 못 맞춘다.

### 5. 가설 B: retry storm

payment 503가 시작된 뒤 service A의 outbound call rate가 평소의 5배가 됐다. retry가 장애 load를 증폭했을 수 있다.

### 6. 가설 C: event loop blocking

CPU-heavy JSON transform deploy 이후 event loop lag와 모든 route latency가 동시에 상승했다.

### 7. CLEAN 검증 failure type

이번 BLOCK에서 서로 다른 failure를 최소한 다음처럼 구분해 실제 test한다.

```text
CPU blocking
pool exhaustion
slow dependency timeout
retry amplification
queue backlog
high-cardinality telemetry misconfiguration
```

같은 endpoint를 여러 번 정상 호출하는 것은 서로 다른 CLEAN PASS가 아니다.

### 8. 채점

- latency/throughput/concurrency를 구분한다.
- 평균과 tail percentile을 함께 본다.
- Node event loop blocking을 재현한다.
- pool size를 전체 instance capacity와 계산한다.
- timeout을 deadline budget으로 본다.
- retry multiplication을 설명한다.
- circuit breaker와 bulkhead 차이를 안다.
- load shedding/backpressure가 필요한 포화 상황을 설명한다.
- liveness와 readiness를 구분한다.
- log/metric/trace가 각각 무엇을 잘하는지 안다.
- high-cardinality label을 피한다.
- SLI/SLO population을 명시한다.

### 9. AI 활용

AI에게 log 검색 query, dashboard 초안, trace 요약, error grouping을 맡길 수 있다. 그러나 `이 metric 하나가 높으니 root cause다`라는 결론은 증거 chain 없이 받아들이지 않는다.

사람이 직접 해야 하는 일은 **변경 시점·사용자 증상·resource 상태·dependency failure를 같은 시간축에 놓고 반증 가능한 가설을 만드는 것**이다.
