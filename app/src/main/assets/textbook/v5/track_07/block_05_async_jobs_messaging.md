# TRACK 07 · 서버와 백엔드

# BLOCK 05 · 큐·작업·웹훅으로 오래 걸리는 일을 안전하게 처리하기

지금까지는 request가 들어오면 그 request 안에서 일을 끝내고 response를 보내는 흐름을 중심으로 봤다. 하지만 이메일, 영상 변환, 대량 보고서, 외부 시스템 동기화처럼 오래 걸리거나 재시도가 필요한 작업은 request와 분리하는 편이 낫다.

이번 BLOCK에서 가장 중요한 문장은 이것이다.

> **비동기화는 일을 없애는 것이 아니라, 누가 언제 끝까지 책임질지를 바꾸는 것이다.**

---

## LESSON 61 · synchronous와 asynchronous 처리를 `사용자가 무엇을 기다려야 하는가`로 구분한다

### 1. 동기 처리

사용자가 주문 버튼을 누르고 결제 승인 결과가 있어야 `성공`을 보여 줄 수 있다면 핵심 결과를 request 안에서 기다릴 수 있다.

```text
request
→ 결제 승인
→ 주문 저장
→ response
```

### 2. 비동기 처리

영수증 이메일은 주문 성공 여부 자체를 결정하지 않을 수 있다.

```text
request
→ 주문 저장
→ 이메일 작업 등록
→ response

나중에 worker
→ 이메일 전송
```

### 3. `빠르게 응답하려고 async`가 전부가 아니다

queue로 넘기면 retry, rate control, peak smoothing, independent scaling 같은 장점이 있지만 delivery delay, duplicate, monitoring, operational complexity가 추가된다.

### 4. 202 Accepted의 의미를 생각한다

server가 작업을 받아들였지만 아직 완료하지 않은 경우 HTTP 202를 사용할 수 있다. 이때 client가 상태를 조회할 URL이나 job id를 받을 수 있다.

### 5. 직접 설계

PDF 보고서 생성 API를 `POST /reports` → `202 + reportId` → `GET /reports/:id` 구조로 설계한다. `PENDING/RUNNING/SUCCEEDED/FAILED` 상태를 정의한다.

### 6. 작은 문제

queue에 넣는 데 성공했으니 사용자에게 `이메일 전송 완료`라고 응답해도 되는가?

아니다. enqueue success와 실제 side effect completion은 다르다. API wording이 실제 보장을 과장하면 안 된다.

---

## LESSON 62 · queue·job·worker는 일을 저장하고 꺼내 실행하는 역할을 나눈다

### 1. 세 단어

```text
job     = 수행해야 할 일과 필요한 data
queue   = 아직 처리되지 않은 job을 보관/전달하는 구조
worker  = job을 가져와 실제로 처리하는 process
```

### 2. job에는 필요한 identity를 명시한다

```json
{
  "jobId": "j-123",
  "type": "SEND_RECEIPT",
  "tenantId": "t-10",
  "orderId": "o-99"
}
```

HTTP request object 전체를 serialize해 queue에 넣지 않는다. request socket, response object 같은 runtime object는 worker가 사용할 수 없다.

### 3. 큰 payload 대신 reference를 보낼 때

대용량 file/content를 message body에 넣기보다 storage reference를 보낼 수 있다. 하지만 worker가 처리할 때 resource가 삭제되거나 권한이 바뀌는 경우를 생각한다.

### 4. schema/version을 둔다

producer가 새 field를 추가하고 worker가 이전 버전인 deploy 순간이 있을 수 있다. message도 API와 마찬가지로 contract다.

### 5. 작은 문제

job payload에 user의 현재 role을 넣고 3일 뒤 그대로 믿어도 되는가?

권한은 그 사이 바뀔 수 있다. 처리 시점에 어떤 정보가 snapshot이고 어떤 정보는 재검증해야 하는지 정한다.

---

## LESSON 63 · at-least-once delivery에서는 같은 job이 다시 올 수 있다고 가정한다

### 1. 왜 duplicate가 생기는가

worker가 email을 보낸 직후 process가 crash했다고 하자. queue에 `처리 완료` acknowledgement를 보내기 전이었다.

queue는 job이 끝나지 않았다고 판단해 다시 전달할 수 있다.

### 2. 전달 보장과 side effect 보장은 다르다

message가 exactly once 전달된다고 주장하는 제품 기능이 있어도 end-to-end side effect가 정확히 한 번이라는 뜻으로 확대하면 안 된다. DB, network, external API 경계를 함께 봐야 한다.

### 3. idempotent consumer

같은 `eventId` 또는 business key를 이미 처리했는지 기록하고 duplicate를 무시하거나 동일 결과를 재사용할 수 있다.

### 4. 자연스럽게 idempotent한 operation

`사용자 상태를 ACTIVE로 설정`은 반복해도 결과가 같을 수 있지만 `잔액에서 1000원 차감`은 반복하면 달라진다.

### 5. 직접 문제

`INSERT processed_events(event_id UNIQUE)`와 business update를 서로 다른 transaction으로 실행하면 어느 순간 crash에서 틈이 생길 수 있는가?

처리 기록만 남고 business update가 안 되거나 반대가 될 수 있다. 같은 DB라면 하나의 transaction으로 묶을 수 있는지 검토한다.

---

## LESSON 64 · acknowledgement·retry·backoff는 실패한 job을 다시 시도하는 규칙을 만든다

### 1. ack

worker가 성공했다고 queue에 알리면 job을 제거하거나 완료 처리할 수 있다.

### 2. 언제 ack할지 중요하다

job을 받자마자 ack하고 그 뒤 process가 crash하면 job을 잃을 수 있다. 실제 side effect와 durable state가 끝난 뒤 ack하는 구조가 일반적이다.

### 3. retry 가능한 실패

```text
일시적 network timeout
provider 503
DB transient connection error
```

### 4. retry해도 안 되는 실패

```text
잘못된 email address 형식
존재하지 않는 required entity
schema validation 실패
permission denied
```

### 5. exponential backoff

```text
1초 → 2초 → 4초 → 8초 ...
```

즉시 반복 요청이 dependency 장애를 더 악화시키는 것을 줄인다.

### 6. jitter

수천 worker가 정확히 같은 시점에 retry하면 다시 burst가 생긴다. random jitter를 섞어 분산한다.

### 7. deadline/max attempts

무한 retry하지 않는다. business deadline이 지나면 더 이상 의미 없는 작업일 수도 있다.

---

## LESSON 65 · dead-letter queue는 자동 처리할 수 없는 job을 격리하는 곳이지 쓰레기통이 아니다

### 1. poison message

항상 같은 code path를 crash시키는 malformed message가 queue 앞에서 반복되면 처리 capacity를 소모한다.

### 2. DLQ로 이동

정해진 attempts 후 별도 dead-letter queue/table로 보내 정상 traffic과 분리한다.

### 3. DLQ에 들어갔다고 해결된 것은 아니다

운영자는 다음을 알아야 한다.

```text
왜 실패했는가
몇 개인가
어떤 tenant/user에 영향이 있는가
고친 뒤 어떻게 replay할 것인가
replay해도 side effect가 중복되지 않는가
```

### 4. 개인정보와 retention

DLQ payload에 민감정보가 포함될 수 있다. 영원히 보관하면 안 된다.

### 5. 작은 문제

DLQ의 모든 message를 fix 후 한 번에 replay하면 어떤 위험이 있는가?

대량 burst, 오래된 business intent 실행, duplicate side effect가 생길 수 있다. 작은 batch와 dry-run/검증이 필요할 수 있다.

---

## LESSON 66 · ordering은 global 순서보다 `어떤 범위의 순서가 필요한가`를 먼저 정한다

### 1. 주문 이벤트 예

```text
OrderCreated
OrderPaid
OrderShipped
```

같은 order 안에서는 이 순서가 중요할 수 있다.

### 2. 모든 주문의 global 순서는 필요하지 않을 수 있다

order A와 B를 한 queue로 완전 직렬화하면 throughput을 희생한다. key/partition별 ordering을 사용할 수 있다.

### 3. retry가 순서를 바꿀 수 있다

Paid 처리가 실패해 retry 중인데 Shipped event가 먼저 처리되면 state machine이 깨질 수 있다.

### 4. sequence/version check

message에 aggregate version을 넣고 expected next version인지 확인할 수 있다. gap이 생기면 보류/retry/reconciliation한다.

### 5. 작은 문제

message timestamp만 비교해 순서를 결정하면 충분한가?

서로 다른 machine clock, 동일 timestamp, network delay 때문에 정확한 causal ordering을 보장하지 못할 수 있다.

---

## LESSON 67 · visibility timeout과 lease는 worker가 job을 잠시 맡고 있다는 계약이다

### 1. worker가 job을 받았다고 바로 queue에서 완전히 삭제하지 않는 방식

queue는 일정 시간 동안 다른 worker에게 안 보이게 하고 그 안에 ack를 기대할 수 있다.

### 2. job이 lease보다 오래 걸리면

아직 처리 중인데 visibility timeout이 끝나 다른 worker에게 같은 job이 전달될 수 있다.

### 3. lease renewal

긴 작업은 heartbeat/renewal로 lease를 연장할 수 있다.

### 4. worker가 hang한 경우

무한 renewal이 되면 job이 영원히 잠길 수 있다. 실제 progress와 heartbeat를 연결하고 max runtime을 둘 수 있다.

### 5. 작은 문제

영상 변환 평균이 2분인데 visibility timeout을 30초로 두면 어떤 현상이 예상되는가?

duplicate processing이 정상적으로 자주 발생할 수 있다. timeout은 workload distribution을 근거로 잡는다.

---

## LESSON 68 · idempotent consumer는 `message id`와 `business operation`을 함께 본다

### 1. 같은 event가 두 번 전달된다

`eventId=e1`을 처리 기록으로 막을 수 있다.

### 2. 다른 event id지만 같은 business intent일 수도 있다

producer bug/retry가 새 event id를 만들어 같은 order charge command를 두 번 보낼 수 있다. external payment idempotency key처럼 business key도 필요할 수 있다.

### 3. side effect provider의 dedupe 기능

payment/email provider가 idempotency key를 지원하면 end-to-end 방어에 활용한다. 우리 DB processed table만으로 provider call 직후 crash gap을 모두 막지는 못한다.

### 4. 결과 저장

같은 key 재처리 시 previous response/reference를 반환할 수 있다.

### 5. 작은 문제

consumer를 idempotent하게 만들었으니 producer 중복은 아무렇게나 생겨도 괜찮은가?

아니다. duplicate는 queue/storage/load/cost를 증가시킨다. producer도 원인을 줄이고 consumer는 방어층으로 둔다.

---

## LESSON 69 · saga와 compensation은 여러 시스템에 걸친 긴 흐름의 실패를 다룬다

### 1. 하나의 DB transaction으로 묶을 수 없는 흐름

```text
주문 생성
→ 결제 승인
→ 재고 예약
→ 배송 접수
```

각 단계가 다른 service/provider라면 하나의 local transaction으로 rollback할 수 없다.

### 2. compensation

재고 예약까지 성공했는데 배송 접수가 실패하면 예약 해제나 결제 취소 같은 보상 action을 실행할 수 있다.

### 3. compensation은 과거를 지우는 rollback과 다르다

고객에게 email이 이미 갔거나 외부 시스템 history가 남는다. `취소 transaction`을 새로 만드는 의미에 가깝다.

### 4. orchestration과 choreography

central coordinator가 다음 step을 명령할 수도 있고 각 event에 반응해 분산 진행할 수도 있다. 둘 모두 state/observability/timeout 문제가 있다.

### 5. 작은 문제

refund API도 실패할 수 있다. compensation이 실패하면?

manual intervention/retry/reconciliation 상태가 필요하다. saga는 실패가 사라지는 패턴이 아니다.

---

## LESSON 70 · webhook receiver는 외부 system이 우리 server를 호출하는 inbound async boundary다

### 1. payment provider가 결과를 알려 준다

```text
POST /webhooks/payments
```

### 2. 서명 검증

internet 누구나 endpoint를 호출할 수 있으므로 provider가 제공하는 signature scheme을 검증한다. secret/header 구조는 provider 공식 문서를 따른다.

### 3. raw body 요구

일부 signature는 원본 body bytes를 대상으로 계산된다. JSON parser가 whitespace나 representation을 바꾸기 전에 raw body를 보존해야 할 수 있다.

### 4. 빠르게 ack하고 내부 처리 분리

provider가 짧은 timeout으로 retry한다면 webhook handler에서 오래 business logic을 하지 않고 검증+durable enqueue 후 2xx를 줄 수 있다.

### 5. duplicate와 out-of-order

provider webhook은 retry될 수 있고 이벤트 순서가 보장되지 않을 수 있다. event id와 resource current state를 함께 사용한다.

### 6. 작은 문제

webhook body의 `status: paid`만 보고 order를 paid로 바꾸면 충분한가?

signature, merchant/account, amount/currency, order reference 등 우리가 기대한 transaction과 일치하는지 검증해야 한다.

---

## LESSON 71 · webhook sender는 상대 server가 느리거나 죽어 있어도 delivery를 관리해야 한다

### 1. 우리 서비스가 partner에게 event를 보내는 경우

request thread에서 바로 partner URL을 호출하면 partner 장애가 우리 사용자 request latency로 전파된다.

### 2. durable event/job

subscription과 event를 저장하고 worker가 delivery한다.

### 3. signature

receiver가 message가 우리에게서 왔고 변조되지 않았음을 확인할 수 있게 shared secret 기반 signature 등을 제공할 수 있다.

### 4. retry 정책

```text
2xx → 성공
일부 4xx → configuration/data 문제, 무한 retry 금지
429/5xx/timeout → backoff retry 후보
```

상대 contract에 따라 세부를 정한다.

### 5. replay 보호

timestamp와 unique event id를 포함해 너무 오래된 signed request를 receiver가 거부할 수 있게 설계할 수 있다.

### 6. 작은 문제

receiver가 500을 24시간 내내 주는데 1초마다 retry하면?

상대와 우리 시스템 모두에 불필요한 load를 만든다. exponential backoff, jitter, max horizon, dashboard가 필요하다.

---

## LESSON 72 · email·SMS·AI API 같은 외부 호출은 실패뿐 아니라 비용 budget도 관리한다

### 1. 호출 1회가 돈일 수 있다

retry를 10번 하면 비용도 10배가 될 수 있다.

### 2. business dedupe

비밀번호 재설정 SMS를 버튼 연타마다 새로 보내지 않고 일정 시간에 한 번으로 제한할 수 있다.

### 3. provider quota

rate limit을 넘으면 전체 message가 지연될 수 있다. global/tenant priority queue를 나눌 수 있다.

### 4. fallback provider

SMS provider A 장애 때 B로 보내는 것은 가용성을 높일 수 있지만 두 provider가 모두 전송해 duplicate가 생기지 않도록 결과 불확실성을 처리해야 한다.

### 5. cost telemetry

`성공률`만 보지 말고 request 수, retry 수, provider cost, tenant별 usage를 본다.

### 6. 작은 문제

AI summary 생성이 timeout이면 무조건 즉시 새 request를 5개 병렬로 보내면?

비용 폭증과 provider load, duplicate result가 생길 수 있다. deadline, budget, dedupe, backoff를 설계한다.

---

## LESSON 73 · scheduler·cron job은 `한 번만 실행된다`고 가정하지 않는다

### 1. 매일 자정 billing job

server instance가 3개인데 각 instance가 자체 cron을 돌리면 세 번 실행될 수 있다.

### 2. leader/lock

분산 lock, scheduler service, DB advisory lock 등으로 한 실행자를 선택할 수 있다.

### 3. 이전 run과 겹침

작업이 70분 걸리는데 매시간 실행되면 overlap한다.

```text
SKIP if running
QUEUE next run
ALLOW overlap with partition
```

정책을 정한다.

### 4. missed run

server가 자정에 down이었다가 01:00에 올라오면 자정 job을 보충할지 건너뛸지 business 의미가 필요하다.

### 5. time zone/DST

`매일 09:00`이 어느 time zone인지 명시한다. daylight saving이 있는 지역은 하루가 23/25시간이 될 수 있다.

### 6. 작은 문제

cron expression이 맞는지만 test하면 충분한가?

중복, overlap, missed execution, idempotency까지 봐야 production behavior를 검증한 것이다.

---

## LESSON 74 · batch와 bulk 처리는 throughput을 높이지만 partial failure 계약이 필요하다

### 1. 한 건씩 1만 번 호출

network/DB round trip overhead가 크다.

### 2. batch write

100개씩 묶어 처리하면 효율을 높일 수 있다.

### 3. 실패 의미

100개 중 1개가 잘못됐을 때:

```text
전체 실패
유효한 99개 성공 + 1개 error
부분 commit 후 retry
```

API contract를 정한다.

### 4. batch 크기

너무 크면 memory, lock time, request timeout이 커진다. 너무 작으면 overhead가 늘어난다. 측정해서 정한다.

### 5. progress checkpoint

백만 건 migration/job은 중간 checkpoint를 저장해 crash 후 처음부터 다시 하지 않게 할 수 있다.

### 6. 작은 문제

batch 전체를 transaction 하나로 3시간 실행하면?

long transaction, lock, log growth, rollback cost가 커질 수 있다. chunk와 checkpoint가 필요할 수 있다.

---

## LESSON 75 · BLOCK 05 실전: 주문 후처리를 queue 기반 pipeline으로 만든다

### 1. 주문 core와 후처리를 구분한다

```text
사용자 request
→ 주문/결제 핵심 결과 확정
→ outbox에 OrderCreated 기록
→ response

publisher
→ broker

workers
├─ receipt email
├─ analytics
└─ fulfillment
```

### 2. message contract

```json
{
  "eventId": "e-100",
  "type": "OrderCreated",
  "schemaVersion": 1,
  "tenantId": "t-1",
  "orderId": "o-9",
  "occurredAt": "..."
}
```

필요 최소 정보만 넣는다.

### 3. 실제로 검증해야 할 failure mode

```text
publish 직전 crash
publish 직후 sent 표시 전 crash
worker side effect 후 ack 전 crash
visibility timeout 중 duplicate worker
provider 429
permanent invalid payload
DLQ replay
out-of-order event
scheduler overlap
```

### 4. 상태를 보이게 만든다

```text
queue depth
oldest message age
processing latency
retry count
DLQ count
success/failure by job type
provider response class
```

### 5. 채점

- enqueue와 completion을 구분한다.
- at-least-once에서 duplicate가 정상 failure mode임을 안다.
- ack 시점을 side effect와 연결한다.
- retryable/permanent failure를 구분한다.
- DLQ를 운영 workflow와 연결한다.
- ordering scope를 business key 기준으로 정한다.
- lease/visibility timeout을 processing time과 맞춘다.
- idempotent consumer가 필요한 이유를 설명한다.
- saga compensation이 rollback과 다름을 안다.
- webhook sender/receiver 양쪽의 retry와 signature를 설명한다.
- scheduler overlap과 missed run을 다룬다.
- batch partial failure contract를 정한다.

### 6. AI에게 맡기지 말아야 할 판단

AI가 queue client code와 worker skeleton을 만들어 주는 것은 좋다. 하지만 다음은 사람이 system contract로 확정한다.

```text
어떤 side effect가 중복되어도 되는가
어떤 key로 dedupe할 것인가
어떤 실패를 retry할 것인가
언제 포기하고 DLQ/manual 처리로 넘길 것인가
순서가 필요한 범위는 어디까지인가
사용자에게 완료라고 말할 수 있는 시점은 언제인가
```

비동기 시스템은 `나중에 알아서 해 줌`이 아니라 책임을 더 명시적으로 설계해야 하는 시스템이다.
