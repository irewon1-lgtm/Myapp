# TRACK 07 · 서버와 백엔드

# BLOCK 07 · API를 오래 바꾸고 AI와 함께 검증 가능한 백엔드를 만들기

마지막 BLOCK은 새 framework를 하나 더 배우는 과정이 아니다. 지금까지 만든 backend를 **다른 client가 오래 사용할 수 있는 계약**으로 만들고, 변경·테스트·AI 생성 코드 검토까지 연결한다.

핵심 질문은 다음과 같다.

```text
외부 사용자가 무엇을 계약으로 보고 있는가?
어떤 변경이 기존 client를 깨뜨리는가?
문서와 실제 server가 같은가?
테스트가 어떤 failure를 실제로 증명하는가?
AI가 만든 코드의 어느 부분을 사람이 반드시 검토해야 하는가?
```

---

## LESSON 91 · API는 endpoint 목록이 아니라 다른 프로그램이 의존하는 계약이다

### 1. contract에 포함되는 것

```text
method와 path
request parameter/body shape
required/optional field
response shape
status/error semantics
authorization rule
pagination/order
idempotency behavior
rate/resource limits
```

### 2. 내부 구현은 숨긴다

DB table 이름이 `tbl_users_v2`로 바뀌어도 public API가 변할 필요는 없다. API가 implementation detail을 그대로 노출하면 내부 변경이 client breaking change가 된다.

### 3. resource와 action

REST 스타일에서는 resource 중심으로 표현할 수 있다.

```text
GET /orders/10
POST /orders
```

하지만 모든 operation을 CRUD 단어에 억지로 맞추지 않는다.

```text
POST /orders/10/cancel
```

처럼 domain action을 명시하는 편이 contract가 선명할 때도 있다.

### 4. 이름의 일관성

`userId`, `user_id`, `memberNo`가 같은 API 안에서 이유 없이 섞이면 client 인지 비용이 커진다.

### 5. 작은 문제

DB primary key 이름이 `ord_idx`이므로 API도 `/orders?ord_idx=...`로 만들어야 하는가?

아니다. API는 consumer가 이해하는 domain contract를 우선하고 내부 schema를 숨길 수 있다.

---

## LESSON 92 · OpenAPI와 JSON Schema는 API 계약을 machine-readable하게 표현한다

### 1. 사람 문서만으로는 drift를 잡기 어렵다

README에는 `age`가 number라고 쓰였는데 server는 string도 허용할 수 있다.

### 2. OpenAPI document

operation, parameter, request body, response, security 등을 구조화해 표현할 수 있다.

```yaml
paths:
  /users/{id}:
    get:
      responses:
        '200':
          description: user
```

### 3. schema 재사용

JSON Schema 계열 표현으로 object field/type/range 등을 정의할 수 있다.

### 4. schema-first vs code-first

spec을 먼저 만들고 code를 생성/구현할 수도 있고 code annotation에서 spec을 만들 수도 있다. 어느 방식이든 **실제 runtime behavior와 spec이 같은지**가 중요하다.

### 5. lint

organization rule을 자동 검사할 수 있다.

```text
operationId 존재
error schema 일관성
pagination naming
security requirement
```

### 6. 작은 문제

OpenAPI 문서가 생성됐으니 server가 그 계약을 지킨다는 것이 증명되는가?

아니다. runtime request/response validation, contract test 등으로 drift를 검증해야 한다.

---

## LESSON 93 · backward compatibility는 기존 client가 업데이트되지 않아도 계속 동작하는가를 본다

### 1. additive change

optional response field 추가는 대개 호환되기 쉽지만 client가 unknown field를 거부하는 특수한 경우도 있다.

### 2. breaking change 예

```text
field 삭제/이름 변경
string → number type 변경
optional → required
허용 enum 값 제거
status code 의미 변경
pagination order 변경
```

### 3. enum 값 추가도 위험할 수 있다

client가 exhaustive switch에서 unknown value를 처리하지 못하면 새 enum 추가가 깨질 수 있다. consumer behavior를 생각한다.

### 4. tolerant reader

client는 모르는 response field를 무시하고 unknown enum fallback을 두는 것이 evolution에 유리할 수 있다.

### 5. 작은 문제

`createdAt` format을 `2026-01-01`에서 Unix integer로 바꾸면 field 이름이 같으니 compatible한가?

아니다. representation contract가 바뀌었다.

---

## LESSON 94 · versioning과 deprecation은 breaking change를 관리하는 수단이지 첫 해결책이 아니다

### 1. version 방법

```text
/v1/users
Accept header/media type
query/header version
```

각 방식의 tooling/cache/discovery trade-off가 있다.

### 2. version을 늘리기 전에 additive evolution을 검토한다

매 작은 변경마다 v2/v3를 만들면 여러 version을 동시에 유지해야 한다.

### 3. deprecation lifecycle

```text
새 방식 제공
→ 문서에 deprecated 표시
→ usage 측정
→ migration guide
→ 충분한 기간
→ 제거
```

### 4. 사용량을 모르면 제거하기 어렵다

어떤 client가 old endpoint를 쓰는지 observability가 필요하다.

### 5. sunset date

public/partner API라면 구체적 날짜와 migration path를 전달한다.

### 6. 작은 문제

`v2 만들었으니 내일부터 v1 삭제`는 왜 위험한가?

client release cycle과 migration 시간, 계약 약속을 무시한다.

---

## LESSON 95 · contract test는 provider와 consumer가 기대하는 interface가 맞는지 확인한다

### 1. unit test만으로 부족한 지점

service 함수가 정상이어도 route path, JSON field, status code가 바뀌어 client가 깨질 수 있다.

### 2. provider contract test

실제 HTTP server를 띄워 request를 보내 response shape와 semantics를 검사한다.

### 3. consumer-driven contract

consumer가 실제 의존하는 interaction을 contract로 만들고 provider change가 이를 깨는지 CI에서 확인하는 방식도 있다.

### 4. schema validation만으로 의미가 전부 검증되지는 않는다

`status: "PAID"`라는 string shape가 맞아도 권한 없는 order가 반환되면 business contract는 실패다.

### 5. 작은 문제

mock server가 항상 spec대로 답하니 frontend test가 모두 통과한다. 실제 backend도 안전한가?

mock과 real provider 사이 drift가 있을 수 있다. 실제 provider contract verification이 필요하다.

---

## LESSON 96 · test double은 외부 dependency를 통제하지만 현실과 달라질 위험도 있다

### 1. 용어보다 목적

fake, stub, mock, spy 같은 용어가 있지만 초보자는 먼저 `무엇을 대신하고 무엇을 검증하는가`를 본다.

### 2. fake repository

in-memory Map으로 OrderRepository contract를 구현할 수 있다.

### 3. fake의 함정

real DB는 unique constraint, transaction isolation, case collation이 있는데 Map fake에는 없다. fake test가 통과해도 production behavior는 다를 수 있다.

### 4. mock over-specification

`함수 A 다음 B를 정확히 한 번 호출` 같은 내부 순서를 너무 많이 고정하면 harmless refactor에도 test가 깨진다.

### 5. 중요한 boundary는 integration test

DB, message broker, HTTP adapter 등 contract가 중요한 지점은 실제 또는 production과 충분히 유사한 component를 사용해 검증한다.

### 6. 작은 문제

모든 test를 real payment API로 실행하면 더 정확하지 않은가?

비용, 속도, rate limit, 외부 장애, 실제 side effect 때문에 위험하다. sandbox/fake/contract test를 목적별로 나눈다.

---

## LESSON 97 · edge case test는 happy path보다 invariant 경계를 공격한다

### 1. 경계값

```text
quantity: 0, 1, max, max+1
string: empty, 1 char, max, max+1
pageSize: 0, 1, 100, 101
```

### 2. malformed input

JSON syntax, wrong type, unknown field, duplicate logical value를 보낸다.

### 3. concurrency case

같은 idempotency key 두 request, same version update 두 request를 동시에 실행한다.

### 4. time boundary

coupon expiry 직전/정확히 expiry/직후를 fixed clock으로 test한다.

### 5. property-based testing 개념

개별 예만 쓰지 않고 많은 generated input에 대해 invariant가 항상 유지되는지 검사할 수 있다.

예: 어떤 valid item list에서도 final total이 음수가 되지 않는다.

### 6. 작은 문제

랜덤 test가 한 번 통과했다. 재현 가능한가?

seed와 failing input을 보존해야 실패를 다시 실행할 수 있다.

---

## LESSON 98 · load test는 서버를 괴롭히는 행사가 아니라 capacity curve를 찾는 실험이다

### 1. 질문을 먼저 정한다

```text
어느 concurrency까지 p99<500ms인가?
초당 1000 request에서 error rate는?
DB pool이 언제 saturate되는가?
```

### 2. warm-up

JIT, cache, connection pool이 안정화되기 전 수치를 steady state와 섞지 않는다.

### 3. arrival rate와 closed-loop user model

부하 도구 설정이 실제 traffic pattern과 다르면 결과 해석이 왜곡된다.

### 4. coordinated omission

client가 느린 response를 기다리는 동안 새 request를 만들지 않아 실제 overload latency를 과소평가하는 측정 함정이 있다. 사용하는 load tool의 모델을 이해한다.

### 5. production과 다른 dependency

mock DB가 너무 빠르면 application bottleneck만 측정하게 된다. 반대로 실 production 외부 API를 load test하면 피해와 비용을 만들 수 있다.

### 6. 작은 문제

10초 동안 1만 req/s가 성공했으니 장시간 production도 안전한가?

memory leak, connection leak, cache eviction, thermal/resource drift 같은 장기 문제가 나타나지 않았을 수 있다.

---

## LESSON 99 · modular monolith는 한 배포 단위 안에서도 책임 경계를 만들 수 있다

### 1. microservice가 아니어도 module을 나눌 수 있다

```text
orders
payments
catalog
users
```

한 process/DB deployment 안에 있어도 public module API와 내부 implementation을 분리한다.

### 2. 장점

network/distributed transaction 복잡도 없이 코드 ownership과 dependency direction을 관리할 수 있다.

### 3. module 간 DB table 직접 접근

orders module이 payments 내부 table을 직접 update하면 code module은 나뉘어도 data boundary가 깨진다.

### 4. 내부 event

module coupling을 낮추기 위해 in-process event를 쓸 수 있지만 flow가 너무 숨겨지면 디버깅이 어려워질 수 있다.

### 5. 작은 문제

폴더만 `orders/`, `payments/`로 나누면 modular architecture가 된 것인가?

import dependency와 data ownership까지 실제 경계를 검사해야 한다.

---

## LESSON 100 · background worker와 API server를 같은 codebase에서 운영해도 runtime 책임은 분리할 수 있다

### 1. 하나의 repository, 여러 entry point

```text
api.ts     → HTTP server process
worker.ts  → queue consumer process
cron.ts    → scheduled batch process
```

공통 domain/application code를 공유할 수 있다.

### 2. scaling 기준이 다르다

API는 request latency/concurrency, worker는 queue depth/age를 기준으로 scale할 수 있다.

### 3. shutdown도 다르다

API는 새 connection을 끊고 in-flight request를 drain한다. worker는 새 message fetch를 멈추고 current lease/job을 완료/반납한다.

### 4. config 분리

worker에 HTTP port가 필요 없고 API server에 consumer concurrency setting이 필요 없을 수 있다.

### 5. 작은 문제

worker crash가 API process까지 함께 죽게 만든 single process 구조의 trade-off는?

단순함은 있지만 failure isolation과 scaling 독립성이 낮다. 시스템 크기에 맞춰 결정한다.

---

## LESSON 101 · microservice는 `큰 시스템이면 무조건 써야 하는 정답`이 아니다

### 1. 서비스 분리 시 얻을 수 있는 것

```text
독립 deploy
팀 ownership
서로 다른 scaling
failure/resource isolation
technology 선택 독립성
```

### 2. 새로 생기는 비용

```text
network latency/failure
service discovery
observability
schema/message evolution
partial failure
security between services
distributed data consistency
```

### 3. 잘못된 경계는 distributed monolith가 된다

항상 같이 deploy해야 하고 서로 sync call을 여러 번 해야 한다면 network 비용만 추가될 수 있다.

### 4. team/organization도 architecture에 영향을 준다

독립 ownership과 operational maturity가 없으면 많은 service를 운영하는 비용이 과도할 수 있다.

### 5. 작은 문제

`코드가 10만 줄을 넘었으니 20 microservice로 쪼갠다`는 기준이 충분한가?

business capability, coupling, deployment need, team ownership, data boundary를 함께 본다.

---

## LESSON 102 · distributed system에서는 `한 번에 완벽히 최신인 하나의 상태`라는 직관이 깨질 수 있다

### 1. replication delay

write 직후 다른 replica에서 read하면 이전 값이 보일 수 있다.

### 2. network partition

두 node가 서로 통신하지 못하는 동안 모든 곳에서 같은 결정을 내리기 어렵다.

### 3. eventual consistency

모든 use case가 즉시 같은 값을 볼 필요는 없다. 검색 index나 analytics는 조금 늦어도 될 수 있다.

### 4. strong invariant가 필요한 부분

중복 결제 방지, unique username처럼 더 강한 coordination이 필요한 rule도 있다.

### 5. consistency를 기능별로 선택한다

`우리 시스템은 eventual consistency`라고 전체에 하나의 label을 붙이기보다 각 operation의 read-after-write, ordering, uniqueness, staleness tolerance를 명시한다.

### 6. 작은 문제

cache가 30초 늦을 수 있는데 사용자에게 `저장 성공` 직후 다시 조회하면 old value가 보인다. bug인가?

product contract가 read-your-write를 약속했다면 bug다. 약속하지 않았다면 UX에서 pending/refresh 전략이 필요할 수 있다. consistency requirement가 먼저다.

---

## LESSON 103 · AI가 만든 backend 코드는 `작동하나`보다 `어떤 계약을 빠뜨렸나`를 검토한다

### 1. AI가 잘하는 일

```text
boilerplate route/controller
schema syntax
ORM query 초안
OpenAPI 초안
unit test case 후보
refactor 반복 작업
```

### 2. 자주 빠뜨릴 수 있는 판단

```text
object-level authorization
transaction boundary
idempotency
retry duplicate side effect
secret/log leakage
cache tenant scope
concurrency race
timeout/reconciliation
```

### 3. prompt에 invariant를 넣는다

나쁜 요청:

```text
주문 API 만들어 줘
```

더 나은 요청:

```text
같은 idempotency key로 주문이 두 개 생기면 안 된다.
order와 items는 local transaction으로 함께 저장한다.
결제 timeout은 declined로 단정하지 않는다.
response에는 payment secret/internal note를 노출하지 않는다.
다른 user의 order id를 넣어도 조회할 수 없어야 한다.
```

### 4. generated code를 diff로 검토한다

파일 전체를 새로 받아 blindly replace하지 않고 변경 이유와 boundary별로 검토한다.

### 5. AI 설명도 evidence가 아니다

`이 코드는 thread-safe합니다`라는 설명을 그대로 믿지 않는다. race test, constraint, atomic operation을 실제 확인한다.

### 6. 작은 문제

AI가 `Promise.all`로 결제와 재고예약을 동시에 해 성능을 높였다. 둘 중 하나만 성공했을 때 복구는?

성능 최적화가 consistency contract를 깨뜨릴 수 있다. side effect independence를 먼저 확인한다.

---

## LESSON 104 · 최종 프로젝트 1: secure-by-contract 주문 API를 구현한다

### 1. 기능

```text
POST /orders
GET /orders/:id
POST /orders/:id/cancel
GET /orders?after=...&limit=...
```

### 2. contract

```text
runtime validation
principal/tenant scope
object authorization
response field allowlist
idempotency key
transaction boundary
outbox
cursor pagination
problem-details style error
request/trace correlation
```

### 3. failure injection

정상 테스트 후 다음 실패를 실제로 만든다.

```text
DB unique conflict
DB transient error
payment timeout
payment decline
outbox publish duplicate
cache stale
permission mismatch
SIGTERM during request
```

### 4. observability

모든 request에서 최소한 다음을 연결한다.

```text
route template
status
latency
request/trace id
error class
selected dependency spans
```

### 5. 실행 결과를 하드코딩하지 않는다

실제 handler와 fake/real adapter가 코드를 실행한 결과를 test가 확인해야 한다. `성공 화면`만 만들어 놓고 backend 기능이 된 것처럼 표시하지 않는다.

### 6. 채점

correctness뿐 아니라 `왜 이 boundary가 필요한가`를 설명한다. 코드를 AI가 작성했더라도 본인이 request→service→repository/adapter→response 흐름을 추적할 수 있어야 한다.

---

## LESSON 105 · 최종 프로젝트 2: failure-first CLEAN PASS로 TRACK 07을 닫는다

### 1. CLEAN PASS는 같은 test를 여러 번 돌린 횟수가 아니다

서로 다른 failure class를 실제로 검증해야 한다.

### 2. PASS A · 입력/계약 실패

검증 대상:

```text
malformed JSON
wrong type
boundary length
unknown field
unsupported method
response schema
```

### 3. PASS B · 권한 실패

```text
credential 없음
expired/invalid credential
다른 user object id
다른 tenant id
admin function 직접 호출
mass-assignment field
```

### 4. PASS C · consistency/concurrency 실패

```text
same idempotency key concurrent request
lost-update race
unique race
transaction rollback
outbox duplicate
cache invalidation race
```

### 5. PASS D · async delivery 실패

```text
worker side effect 후 crash
ack 전 crash
retry backoff
DLQ
out-of-order message
lease expiry
scheduler overlap
```

### 6. PASS E · reliability 실패

```text
slow dependency
timeout
retry amplification
pool exhaustion
CPU event-loop blocking
load shedding
SIGTERM drain
```

### 7. PASS F · observability 실패

`실패가 발생했다`만 확인하지 않는다. request id/trace/log/metric으로 어느 단계에서 실패했는지 재구성 가능한지 본다.

### 8. PASS G · API evolution 실패

old client fixture로 새 server를 호출해 backward compatibility가 실제로 유지되는지 검증한다. OpenAPI diff만으로 끝내지 않는다.

### 9. 실패하면 PASS가 아니다

검증 중 문제가 나오면 해당 class를 수정하고 다시 실행한다. 실행하지 않은 test를 `구조상 문제없음`으로 PASS 처리하지 않는다.

### 10. 이번 TRACK에서 암기할 최소 핵심

```text
Authentication ≠ Authorization
Parsing ≠ Validation
TypeScript type ≠ runtime trust
Timeout ≠ operation definitely failed
Retry ≠ harmless
Queue delivery ≠ exactly-once side effect
Cache ≠ source of truth
HTTP success ≠ business success
Mock success ≠ production contract success
```

### 11. 직접 설명할 수 있어야 하는 전체 흐름

```text
client request
→ network/HTTP
→ server process
→ middleware/parsing
→ authentication
→ validation
→ authorization
→ controller
→ application service
→ domain rule
→ repository/cache/external adapter
→ transaction/outbox
→ response mapping
→ logs/metrics/traces
→ async workers/webhooks
```

### 12. AI 시대의 사람 역할

코드 타이핑 자체는 AI가 많이 대신할 수 있다. 하지만 사람이 직접 알아야 하는 부분은 줄어들지 않는다.

```text
어떤 입력을 믿을지
무엇이 invariant인지
어디까지 함께 성공해야 하는지
중복 실행이 어떤 피해를 만드는지
어떤 failure를 retry할지
누가 어떤 data를 볼 수 있는지
어떤 숫자와 trace가 실제 증거인지
변경이 기존 client를 깨는지
```

AI에게 맡길 수 있는 것은 구현 반복, 문법 검색, test 후보 생성, documentation 초안이다. 최종 계약과 evidence 판정은 사람이 해야 한다.

### 13. 최종 프로젝트 완료 조건

읽기만 하고 끝나지 않는다.

```text
코드를 직접 실행한다.
의도적으로 실패를 만든다.
일부 코드를 수정한다.
작은 문제를 푼다.
왜 맞고 틀렸는지 설명한다.
동시성/권한/timeout failure를 실제 test한다.
로그·metric·trace evidence를 연결한다.
```

이 조건을 통과해야 TRACK 07의 학습 목표를 달성한 것이다.
