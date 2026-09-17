# TRACK 07 · 서버와 백엔드

# BLOCK 04 · 데이터 접근·일관성·캐시를 서비스 흐름에 연결하기

TRACK 08은 database 자체를 깊게 배운다. 이번 BLOCK의 범위는 다르다. backend use case가 저장소를 사용할 때 **동시 요청, 중복, transaction 경계, cache 때문에 application 의미가 깨지는 지점**을 배운다.

---

## LESSON 46 · repository는 query를 숨기는 상자가 아니라 application이 저장소에 요구하는 계약이다

### 1. repository가 필요한 이유를 먼저 본다

OrderService가 ORM 문법을 직접 쓰면 use case와 storage detail이 섞인다.

```ts
const row = await prisma.order.findUnique({ where: { id } });
```

repository boundary를 두면 application은 `어떤 data가 필요한가`에 집중할 수 있다.

```ts
interface OrderRepository {
  getRequired(id: OrderId): Promise<Order>;
  save(order: Order): Promise<void>;
}
```

### 2. CRUD method 이름만 복사하지 않는다

repository를 `create/read/update/delete` 네 함수로 기계적으로 만들 필요는 없다. application use case가 `findOpenOrdersForUser`, `reserveInventory`처럼 더 의미 있는 operation을 필요로 할 수 있다.

### 3. 너무 추상화하면 query 능력을 잃는다

모든 repository가 generic `find(criteria: any)` 하나만 제공하면 호출자가 다시 storage query language를 만들어 버린다. abstraction은 domain 질문을 더 명확하게 해야 한다.

### 4. 직접 실습

OrderService에서 ORM import를 제거하고 in-memory repository와 real repository 두 구현을 만들어 같은 service test를 돌린다.

### 5. 작은 문제

repository가 항상 모든 column과 모든 relation을 load하면 어떤 비용이 생기는가?

불필요한 I/O와 memory, serialization이 늘고 조회 목적에 맞는 query 최적화를 어렵게 한다. read model을 별도로 둘 수도 있다.

---

## LESSON 47 · N+1은 코드 한 줄의 문제가 아니라 request 한 건이 DB round trip을 얼마나 만드는지의 문제다

### 1. 주문 목록 예

```ts
const orders = await ordersRepo.list();
for (const order of orders) {
  order.user = await usersRepo.get(order.userId);
}
```

주문 100개면 처음 목록 query 1회 + 사용자 query 최대 100회가 될 수 있다.

### 2. 로컬 개발에서는 안 느릴 수 있다

DB가 같은 machine이고 data가 5개면 문제를 못 느낀다. production에서는 network latency와 concurrent load가 곱해진다.

### 3. 해결 방법은 하나가 아니다

```text
join/fetch join
batch query with IN
DataLoader 같은 batching
read model/precomputed projection
```

어떤 방법이 좋은지는 access pattern과 DB 특성에 따라 다르다.

### 4. query count를 evidence로 남긴다

`느린 것 같다`가 아니라 request당 query 수와 total DB time을 측정한다.

### 5. 작은 문제

N+1을 없애려고 모든 relation을 거대한 join으로 항상 가져오면 완벽한가?

아니다. row explosion, 중복 data transfer, 불필요한 field, pagination 왜곡이 생길 수 있다. query shape를 목적에 맞춘다.

---

## LESSON 48 · pagination은 data가 늘어나도 response 크기와 query 비용을 제한하는 계약이다

### 1. `GET /orders`가 모든 주문을 반환하면 처음에는 편하다

10개일 때는 괜찮지만 100만 개가 되면 memory, network, DB 모두 문제가 된다.

### 2. offset pagination

```text
?page=3&pageSize=20
```

구현이 단순하지만 큰 offset 비용과 중간 삽입/삭제에 따른 중복·누락을 고려해야 한다.

### 3. cursor pagination

```text
?after=opaqueCursor&limit=20
```

stable ordering key를 기준으로 다음 위치를 표현한다. cursor를 client가 내부 DB query로 해석할 수 있는 단순 문자열로 노출할 필요는 없다.

### 4. 정렬 기준이 안정적이어야 한다

`createdAt`이 같은 row가 여러 개면 tie-breaker로 id를 함께 쓸 수 있다.

```text
ORDER BY created_at DESC, id DESC
```

### 5. page size 상한

client가 `limit=1000000`을 보내도 server가 정한 최대값을 넘지 않게 한다.

### 6. 작은 문제

cursor에 `page=3`만 base64로 인코딩하면 cursor pagination이 되는가?

아니다. encoding 형식보다 **어떤 ordering position을 안정적으로 이어 가는가**가 본질이다.

---

## LESSON 49 · transaction boundary는 동시에 바뀌어야 하는 local state를 묶는다

### 1. bank transfer 예보다 작은 주문 예로 본다

```text
order 생성
order item 생성
coupon 사용 처리
```

중간 실패 후 일부만 남으면 application invariant가 깨질 수 있다.

### 2. repository 여러 개가 같은 transaction context를 공유해야 할 수 있다

각 repository가 제멋대로 connection/transaction을 만들면 service에서 atomic use case를 구성하기 어렵다.

```ts
await unitOfWork.run(async (tx) => {
  await orders.save(order, tx);
  await coupons.markUsed(couponId, tx);
});
```

### 3. transaction 밖에서 발생하는 side effect를 구분한다

email, HTTP payment call, message broker publish는 DB rollback으로 자동 복구되지 않는다.

### 4. transaction isolation detail은 TRACK 08과 연결한다

여기서는 `transaction을 썼다 = 모든 concurrency 문제 해결`이라고 생각하지 않는 것까지 배운다. isolation level과 DB constraint가 실제 보장을 결정한다.

### 5. 작은 문제

transaction 안에서 30초짜리 외부 API를 기다리면 어떤 운영 문제가 생길 수 있는가?

DB connection과 lock을 오래 잡고 throughput을 떨어뜨릴 수 있다.

---

## LESSON 50 · optimistic locking은 `내가 읽은 뒤 누가 바꿨는지`를 version으로 감지한다

### 1. lost update 상황

두 사용자가 stock=10을 동시에 읽는다.

```text
A: 10 읽음 → 9 저장
B: 10 읽음 → 9 저장
```

두 번 감소했지만 최종 9라면 하나의 update가 사라졌다.

### 2. version field

```text
stock=10, version=5
```

update할 때:

```sql
UPDATE products
SET stock = 9, version = 6
WHERE id = ? AND version = 5;
```

영향 받은 row가 0이면 누군가 먼저 변경했다는 신호다.

### 3. conflict 후 행동

무조건 자동 retry할지, client에게 conflict를 알릴지 use case에 따라 다르다. 사용자 편집 내용은 자동 덮어쓰면 안 될 수 있다.

### 4. ETag/If-Match 같은 HTTP precondition과 연결할 수도 있다

API representation version을 이용해 client가 stale update를 보내는 것을 막을 수 있다.

### 5. 작은 문제

version check 없이 `last write wins`가 항상 잘못인가?

아니다. 일부 setting처럼 마지막 값이면 충분한 domain도 있다. invariant와 사용자 기대가 기준이다.

---

## LESSON 51 · uniqueness는 `먼저 조회했으니 중복이 없다`로 보장되지 않는다

### 1. 잘못된 흐름

```text
A: email 존재 확인 → 없음
B: email 존재 확인 → 없음
A: insert
B: insert
```

application check만으로 concurrency race를 막지 못한다.

### 2. DB unique constraint를 최종 방어선으로 사용한다

application에서 친절한 사전 검사도 할 수 있지만 invariant는 storage constraint로 보강한다.

### 3. constraint error를 application error로 번역한다

DB error code/message를 client에 그대로 내보내지 않는다.

```text
unique constraint violation
→ EmailAlreadyUsed
→ API 409 등 contract에 맞는 response
```

### 4. scope가 있는 uniqueness

이메일이 global unique인지 tenant 안에서만 unique인지 business rule을 먼저 정의한다.

### 5. 작은 문제

`SELECT 후 INSERT`를 transaction 안에 넣기만 하면 모든 DB에서 안전한가?

isolation과 locking 방식에 따라 다르다. unique constraint가 명시적 invariant를 더 직접 표현한다.

---

## LESSON 52 · deadlock과 serialization conflict는 `재시도할 수 있는 transaction 실패`일 수 있다

### 1. deadlock을 간단히 그린다

```text
Transaction A: row 1 lock → row 2 기다림
Transaction B: row 2 lock → row 1 기다림
```

DB는 둘 중 하나를 abort해 cycle을 끊을 수 있다.

### 2. application은 error class를 알아야 한다

모든 DB error를 retry하면 안 된다. syntax error나 constraint violation은 반복해도 해결되지 않는다. deadlock/serialization failure처럼 transient class만 제한적으로 retry한다.

### 3. retry는 전체 transaction을 다시 실행한다

중간 statement 하나만 재실행하면 이전 read 가정이 더 이상 맞지 않을 수 있다.

### 4. side effect를 transaction body 안에서 함부로 실행하지 않는다

transaction retry가 일어나면 email/payment가 두 번 호출될 수 있다.

### 5. 작은 문제

retry 횟수를 무한으로 두면 안전한가?

아니다. 지속적인 contention에서 request가 끝나지 않고 load를 더 키울 수 있다. backoff와 max attempts/deadline이 필요하다.

---

## LESSON 53 · idempotency key를 저장소와 연결해 동시 중복 요청까지 처리한다

### 1. 단순 cache만으로 부족할 수 있다

두 동일 request가 거의 동시에 들어오면 둘 다 `key 없음`을 보고 처리할 수 있다.

### 2. 상태를 모델링한다

```text
key = abc
status = PROCESSING | COMPLETED | FAILED_RETRYABLE
request fingerprint
response snapshot/reference
expiresAt
```

### 3. 같은 key + 다른 payload

request fingerprint가 다르면 conflict로 처리하는 정책을 둘 수 있다. 같은 key가 완전히 다른 주문에 재사용되는 것을 막는다.

### 4. atomic claim

unique constraint나 conditional insert로 key 처리권을 한 request만 획득하게 한다.

### 5. 처리 중 crash

PROCESSING이 영원히 남지 않도록 lease/timeout/recovery 정책이 필요하다.

### 6. 작은 문제

성공 response만 저장하고 실패는 모두 key를 지우면 어떤 위험이 있는가?

실패가 실제로 side effect를 만들었는지 불확실한 경우 재처리가 중복 효과를 만들 수 있다. failure class를 구분한다.

---

## LESSON 54 · transactional outbox는 DB 변경과 `보낼 메시지 기록`을 같은 transaction에 넣는다

### 1. dual write 문제

```text
DB order 저장 성공
↓
message broker publish 실패
```

또는 반대 순서에서 message는 갔지만 DB가 rollback될 수 있다.

### 2. outbox idea

같은 DB transaction에서:

```text
orders row 저장
outbox row 저장
COMMIT
```

그 뒤 별도 publisher가 outbox row를 읽어 broker로 전송한다.

### 3. exactly-once 마법이 아니다

publisher가 message를 보낸 뒤 `sent` 표시 전에 crash하면 같은 message를 다시 보낼 수 있다. consumer idempotency가 여전히 필요하다.

### 4. ordering과 cleanup

aggregate별 ordering, retry, poison message, outbox table growth를 운영해야 한다.

### 5. 작은 문제

outbox를 쓰면 broker와 DB가 완전한 한 transaction처럼 atomic해진다고 말할 수 있는가?

아니다. `DB state + publish intent`를 local atomic하게 기록하고 eventual delivery를 별도 처리하는 패턴이다.

---

## LESSON 55 · cache-aside는 `먼저 cache, 없으면 원본`이라는 읽기 패턴이다

### 1. 기본 흐름

```text
GET product 10
→ cache lookup
→ hit면 반환
→ miss면 DB 조회
→ cache에 저장
→ 반환
```

### 2. cache는 source of truth가 아닐 수 있다

원본 DB가 authoritative state이고 cache는 재생성 가능한 derived copy로 둔다.

### 3. stale data를 받아들일 수 있는지 먼저 묻는다

상품 설명은 몇 초 늦어도 괜찮을 수 있지만 계좌 잔액이나 권한은 훨씬 엄격할 수 있다.

### 4. cache key 설계

```text
product:v3:10
user-summary:v2:tenantA:user7
```

version/schema/tenant scope가 key에 필요할 수 있다.

### 5. 작은 문제

`user:7`만 key로 쓰는데 여러 tenant에 같은 user id가 존재한다면?

cross-tenant data leak이 날 수 있다. cache key도 authorization/data partition과 같은 scope를 보존해야 한다.

---

## LESSON 56 · TTL은 freshness와 load를 교환하는 정책이다

### 1. TTL이 짧으면

stale 시간은 줄지만 cache miss와 DB load가 늘 수 있다.

### 2. TTL이 길면

hit rate는 좋아질 수 있지만 변경 반영이 늦어진다.

### 3. 모든 key가 같은 순간 만료되면 burst가 생긴다

배포 직후 10만 key를 동일 TTL로 채웠다면 한 시간 뒤 동시에 만료될 수 있다.

### 4. jitter

TTL에 작은 random 범위를 더해 만료 시점을 분산할 수 있다.

```text
base 300s + random 0~60s
```

### 5. negative caching

존재하지 않는 resource도 짧게 cache해 반복 miss load를 줄일 수 있다. 하지만 새로 생성됐을 때 보이지 않는 기간과 trade-off가 생긴다.

### 6. 작은 문제

TTL=무한이면 가장 빠르지 않은가?

invalidated되지 않는 stale data가 영구 유지될 수 있다. freshness contract가 없어진다.

---

## LESSON 57 · cache stampede는 인기 key가 만료될 때 많은 request가 동시에 원본으로 몰리는 현상이다

### 1. 상황

1초에 1만 번 읽는 key가 만료된다.

```text
request 1 → miss → DB
request 2 → miss → DB
...
request 10000 → miss → DB
```

cache가 보호막이 아니라 순간적으로 load amplifier가 된다.

### 2. single-flight/request coalescing

한 key의 refresh를 한 worker/request만 수행하고 나머지는 결과를 기다리게 할 수 있다.

### 3. stale-while-revalidate

약간 오래된 값을 잠깐 제공하면서 background에서 새 값을 가져오는 전략도 있다. freshness 요구가 허용할 때만 사용한다.

### 4. distributed lock의 비용

여러 server instance라면 local mutex만으로 전체 stampede를 막지 못할 수 있다. distributed coordination은 failure mode가 추가되므로 무조건 넣지 않는다.

### 5. 작은 문제

single-flight 중 refresh가 10초 hang하면 기다리는 request도 모두 10초 묶일 수 있다. 무엇이 필요한가?

refresh timeout/deadline, stale fallback, waiter limit 같은 정책을 함께 둔다.

---

## LESSON 58 · cache invalidation은 `언제 무엇을 지울지`를 data dependency로 추적하는 문제다

### 1. write 후 cache 삭제

```text
DB update 성공
→ product:10 cache delete
```

간단하지만 delete 실패나 concurrent reader race가 있다.

### 2. write-through/update cache

DB와 cache를 함께 갱신하면 순서/partial failure 문제가 생긴다.

### 3. event 기반 invalidation

data change event를 받아 여러 derived cache를 갱신할 수 있다. delivery delay와 duplicate를 처리해야 한다.

### 4. key dependency

product 10을 수정하면 `product:10`뿐 아니라 category list, search result, recommendation cache도 영향을 받을 수 있다.

### 5. versioned key

data version을 key에 포함해 old cache를 자연스럽게 버리는 전략도 있다. old key cleanup과 key discovery 비용을 생각한다.

### 6. 작은 문제

DB commit 전에 cache부터 지우면 안전한가?

concurrent reader가 cache miss 후 아직 old DB value를 읽어 다시 cache에 넣을 수 있다. 순서와 race를 실제 timeline으로 그려야 한다.

---

## LESSON 59 · 파일과 object storage는 DB row와 다른 lifecycle을 가진다

### 1. upload를 DB blob 하나로만 생각하지 않는다

큰 image/video는 object storage를 쓰고 DB에는 metadata/reference를 저장하는 경우가 많다.

### 2. 두 resource의 atomicity가 다르다

```text
object upload 성공
DB metadata insert 실패
```

orphan object가 남을 수 있다.

반대로 DB row는 생겼는데 upload가 실패할 수도 있다.

### 3. staged upload

```text
TEMP 상태 object
→ 검증/scan
→ DB commit
→ ACTIVE 표시
```

background cleanup으로 오래된 TEMP를 지울 수 있다.

### 4. client direct upload

server가 signed upload URL 등을 발급하고 client가 object storage로 직접 보내면 backend bandwidth를 줄일 수 있다. 권한·size·content type·expiry를 제한한다.

### 5. file name을 신뢰하지 않는다

client file name은 display metadata일 뿐 storage path나 실행 명령으로 직접 사용하지 않는다.

### 6. 작은 문제

DB transaction 안에서 object storage upload를 호출하면 둘이 atomic해지는가?

아니다. 외부 storage는 local DB transaction에 자동 참여하지 않는다.

---

## LESSON 60 · BLOCK 04 실전: 주문 목록·생성·이벤트·cache를 하나의 consistency 지도에 놓는다

### 1. 시스템

```text
POST /orders
GET /orders?after=...
GET /products/:id
```

order 생성은 DB에 저장하고 `OrderCreated` message가 필요하다. product 조회는 cache를 사용한다.

### 2. 먼저 invariant를 적는다

```text
같은 idempotency key로 주문이 두 개 만들어지지 않는다.
order와 item은 함께 저장된다.
OrderCreated는 eventual하게 적어도 한 번 전달된다.
consumer는 duplicate message를 견딘다.
product cache가 다른 tenant data를 섞지 않는다.
```

### 3. failure timeline을 직접 그린다

```text
DB commit 직후 process crash
message publish 직후 crash
cache delete 실패
두 update가 동시에 같은 version 수정
같은 idempotency key 동시 요청
```

### 4. 적용할 도구

```text
transaction
unique constraint
optimistic version
idempotency record
outbox
cache-aside
TTL + jitter
single-flight
cursor pagination
```

도구 이름을 외우는 대신 어떤 failure를 막는지 한 줄씩 연결한다.

### 5. 채점

- N+1을 request/query-count 관점으로 설명한다.
- offset과 cursor pagination trade-off를 설명한다.
- transaction이 외부 API를 rollback하지 못함을 설명한다.
- lost update와 optimistic locking을 timeline으로 설명한다.
- pre-check가 uniqueness를 보장하지 못하는 이유를 말한다.
- transient DB conflict만 제한적으로 retry해야 함을 안다.
- idempotency key concurrent claim 문제를 안다.
- outbox가 duplicate 가능성을 없애지 않음을 안다.
- cache TTL/stampede/invalidation을 서로 다른 실패로 구분한다.
- object storage와 DB의 dual-resource consistency를 설명한다.

### 6. AI 활용

AI에게 ORM query나 pagination boilerplate는 맡길 수 있다. 하지만 다음은 직접 검토한다.

```text
unique constraint가 실제 invariant scope와 일치하는가
retry가 side effect를 중복시키지 않는가
cursor ordering이 stable한가
cache key에 tenant/version이 필요한가
outbox consumer가 idempotent한가
```

이 판단이 없는 자동 생성 코드는 정상 상황에서는 돌아가도 concurrency와 장애에서 깨질 수 있다.
