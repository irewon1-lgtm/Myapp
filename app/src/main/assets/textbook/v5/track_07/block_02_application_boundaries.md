# TRACK 07 · 서버와 백엔드

# BLOCK 02 · 비즈니스 규칙과 코드의 책임을 분리하기

BLOCK 01에서는 request가 server에 들어와 route·validation·handler·response를 통과하는 길을 배웠다. 이제 handler 안에 모든 코드를 넣으면 왜 금방 복잡해지는지 본다.

이번 BLOCK의 핵심 질문은 하나다.

> **이 코드는 HTTP 때문에 존재하는가, 아니면 우리 서비스의 규칙 때문에 존재하는가?**

둘을 구분할 수 있어야 framework를 바꾸거나 DB를 바꿔도 핵심 규칙을 잃지 않는다.

---

## LESSON 16 · business rule은 HTTP와 상관없이 지켜야 하는 서비스 규칙이다

### 1. 주문 규칙부터 생각한다

쇼핑몰에 다음 규칙이 있다고 하자.

```text
수량은 1개 이상이어야 한다.
판매 중지 상품은 주문할 수 없다.
쿠폰은 사용 기간 안에 있어야 한다.
한 주문의 최종 금액은 0원보다 작을 수 없다.
```

이 규칙은 browser에서 주문하든, mobile app에서 주문하든, 관리자 도구에서 주문하든 같다. 이런 규칙을 business rule 또는 domain rule이라고 부른다.

### 2. handler에 바로 박아 넣으면 경계가 흐려진다

```ts
app.post("/orders", async (req, res) => {
  if (req.body.quantity < 1) return res.status(400).end();
  const product = await db.product.findUnique(...);
  if (!product.active) return res.status(409).end();
  const total = product.price * req.body.quantity;
  await db.order.create(...);
  res.json(...);
});
```

작은 코드에서는 동작한다. 문제는 HTTP parsing, validation, DB access, 주문 규칙, response formatting이 한 함수에 동시에 들어 있다는 것이다.

### 3. 규칙을 함수로 떼어 본다

```ts
function calculateOrderTotal(price: number, quantity: number): number {
  if (!Number.isInteger(quantity) || quantity < 1) {
    throw new InvalidQuantityError();
  }
  return price * quantity;
}
```

이 함수는 `req`, `res`, DB driver를 몰라도 된다. 그래서 unit test도 간단하고 다른 entry point에서도 재사용할 수 있다.

### 4. 직접 수정한다

기존 handler에서 계산 코드를 함수로 빼고 `quantity=0`, `1`, `3`을 테스트한다. HTTP 없이 함수만 호출해도 규칙을 검증할 수 있어야 한다.

### 5. 작은 문제

`res.status(409)`는 business rule인가?

아니다. `판매 중지 상품은 주문할 수 없다`가 business 의미이고, 이를 HTTP에서 어떤 status로 표현할지는 transport adapter의 책임이다.

---

## LESSON 17 · controller·service·repository를 이름이 아니라 책임으로 구분한다

### 1. 세 층을 그림으로 본다

```text
HTTP request
↓
controller/handler
↓
application service
↓
repository
↓
storage
```

이 용어가 모든 프로젝트에서 똑같이 쓰이는 것은 아니다. 중요한 것은 책임이다.

```text
controller : HTTP 입력을 application 입력으로 번역하고 결과를 HTTP response로 번역
service    : 한 use case의 흐름과 business rule을 조정
repository : 저장소에서 domain에 필요한 데이터를 읽고 쓰는 경계
```

### 2. 코드로 나눈다

```ts
async function createOrderHandler(req: Request, res: Response) {
  const input = CreateOrderSchema.parse(req.body);
  const result = await orderService.create(input);
  res.status(201).json(toOrderResponse(result));
}
```

```ts
class OrderService {
  constructor(private orders: OrderRepository, private products: ProductRepository) {}

  async create(input: CreateOrderInput) {
    const product = await this.products.getRequired(input.productId);
    const order = Order.create(product, input.quantity);
    await this.orders.save(order);
    return order;
  }
}
```

### 3. layer 수를 늘리는 것이 목표가 아니다

작은 CRUD 앱에서 controller→service→repository가 모두 한 줄씩 전달만 한다면 오히려 불필요한 간접층일 수 있다. 분리의 이유는 `변경 이유`가 다를 때다.

### 4. 직접 판단한다

다음 코드를 어디에 둘지 생각한다.

```text
req.params.id parsing              → HTTP/controller 쪽
주문 취소 가능 시간 30분 규칙       → domain/application 쪽
SELECT/ORM query                   → repository 쪽
JSON field 이름 변경               → response mapper 쪽
```

### 5. 작은 문제

repository가 `res.status(404)`를 직접 호출하면 어떤 결합이 생기는가?

storage layer가 HTTP를 알아버린다. CLI나 message worker에서 같은 repository를 사용하기 어려워지고 테스트도 transport에 묶인다.

---

## LESSON 18 · domain model은 현실의 모든 것을 복사하는 객체가 아니라 중요한 규칙을 표현하는 모델이다

### 1. model은 현실의 축소판이다

`Order`라는 말은 현실 주문의 모든 속성을 담는 것이 아니다. 프로그램이 해결하려는 문제에 필요한 상태와 규칙을 표현한다.

```ts
class Order {
  private constructor(
    readonly id: string,
    private status: OrderStatus,
    private items: OrderItem[]
  ) {}
}
```

### 2. entity와 value의 차이를 감각으로 잡는다

두 주문의 내용이 완전히 같아도 주문 ID가 다르면 다른 주문이다. 반면 `Money(1000, KRW)`는 식별자보다 값 자체가 중요할 수 있다.

이 구분을 각각 entity, value object라고 부르기도 한다. 단어를 먼저 외우지 않고 **동일성을 무엇으로 판단하는가**를 본다.

### 3. primitive obsession을 피하되 과도하게 객체화하지 않는다

모든 string을 class로 싸면 코드가 무거워질 수 있다. 하지만 이메일, 돈, 기간, 주문 상태처럼 규칙이 반복되는 값은 의미 있는 type으로 묶으면 실수를 줄일 수 있다.

### 4. invalid state를 만들기 어렵게 한다

```ts
class Quantity {
  private constructor(readonly value: number) {}

  static create(raw: number) {
    if (!Number.isInteger(raw) || raw < 1) throw new InvalidQuantityError();
    return new Quantity(raw);
  }
}
```

한 번 생성된 `Quantity`가 항상 1 이상이라는 invariant를 얻는다.

### 5. 작은 문제

DB row를 그대로 domain object라고 불러도 되는가?

가능한 단순 시스템도 있지만 항상 같은 개념은 아니다. storage schema는 저장 효율과 query 요구 때문에, domain model은 규칙 표현 때문에 모양이 달라질 수 있다.

---

## LESSON 19 · command와 query를 구분하면 `상태를 바꾸는가`를 먼저 생각하게 된다

### 1. 두 종류의 요청

```text
주문 생성   → 상태를 바꿈
주문 취소   → 상태를 바꿈
주문 조회   → 상태를 읽음
상품 검색   → 상태를 읽음
```

상태를 바꾸는 의도를 command, 읽는 의도를 query라고 부를 수 있다.

### 2. 거대한 CQRS부터 시작하지 않는다

command/query 구분을 이해한다고 즉시 서로 다른 DB, event bus, 별도 service를 만들 필요는 없다. 초보 단계에서는 함수 이름과 side effect를 분명히 하는 것만으로도 가치가 있다.

```ts
orderService.cancelOrder(...);  // command
orderQuery.getOrder(...);       // query
```

### 3. query에서 몰래 write하지 않는다

`GET /orders/:id`를 호출할 때 조회수나 lastSeen을 매번 write하도록 만들면 caching, retry, read replica 사용이 복잡해질 수 있다. 정말 필요한 side effect인지 별도로 설계한다.

### 4. 작은 문제

`checkCoupon()`이 내부에서 coupon을 사용 처리한다. 이름만 보면 query처럼 보인다. 무엇이 문제인가?

호출자가 side effect를 예상하기 어렵다. 이름·type·API가 실제 의미를 드러내야 한다.

---

## LESSON 20 · dependency injection은 필요한 협력자를 바깥에서 넣어 주는 방식이다

### 1. 함수 안에서 모든 dependency를 직접 만들면 교체가 어렵다

```ts
class OrderService {
  private db = new RealDatabase();
  private payment = new RealPaymentClient();
}
```

이 service를 test하려고 해도 실제 DB와 결제 API가 따라온다.

### 2. 생성자에서 받는다

```ts
class OrderService {
  constructor(
    private readonly orders: OrderRepository,
    private readonly payment: PaymentGateway
  ) {}
}
```

production에서는 real implementation을, test에서는 fake implementation을 넣을 수 있다.

### 3. DI container는 선택 사항이다

큰 framework는 dependency graph를 자동으로 조립하는 container를 제공한다. 하지만 `dependency injection = container library`는 아니다. 함수 argument나 constructor로 직접 넘겨도 DI다.

### 4. dependency 방향을 본다

핵심 rule이 특정 DB SDK type을 직접 사용하면 infrastructure가 domain 안으로 침투한다. interface를 application 쪽에서 정의하고 외부 adapter가 구현하게 만들 수 있다.

### 5. 직접 실습

`PaymentGateway` interface를 만들고 `FakePaymentGateway`가 항상 성공하도록 구현한다. 실제 network 없이 order service를 test한다.

---

## LESSON 21 · pure logic과 side effect를 분리하면 테스트와 실패 처리 기준이 선명해진다

### 1. side effect가 무엇인지 찾는다

```text
DB write
file write
network call
email 전송
현재 시각 읽기
random 값 만들기
전역 상태 변경
```

입력이 같아도 외부 상태에 따라 결과가 달라질 수 있는 일이 많다.

### 2. 계산을 먼저 순수하게 만든다

```ts
function discountPrice(price: number, rate: number): number {
  return Math.floor(price * (1 - rate));
}
```

이 함수는 DB나 clock이 필요 없다. 수많은 boundary case를 빠르게 검증할 수 있다.

### 3. side effect orchestration은 별도 흐름에서 한다

```text
상품 조회
→ 할인 계산
→ 결제 요청
→ 주문 저장
```

각 단계가 실패할 수 있으므로 순서와 retry 가능성을 명시한다.

### 4. 작은 문제

함수 안에서 `Date.now()`를 호출하면 왜 test가 불안정해질 수 있는가?

시간이 implicit dependency이기 때문이다. 다음 LESSON에서 clock을 주입한다.

---

## LESSON 22 · 상태 머신은 `지금 상태에서 가능한 다음 행동`을 명시한다

### 1. 주문 상태를 string 하나로만 두면 규칙이 흩어진다

```text
PENDING → PAID → SHIPPED → DELIVERED
          ↓
       CANCELLED
```

모든 상태에서 모든 전이가 가능한 것은 아니다.

### 2. transition 규칙을 코드로 모은다

```ts
function canCancel(status: OrderStatus): boolean {
  return status === "PENDING" || status === "PAID";
}
```

더 복잡하면 transition table이나 state object로 표현할 수 있다.

### 3. illegal transition을 실제로 막는다

이미 `DELIVERED`인 주문을 `PENDING`으로 되돌리는 request가 들어와도 client 값을 그대로 DB에 update하면 안 된다. 현재 state와 요청 transition을 함께 검사한다.

### 4. concurrency까지 생각한다

두 request가 동시에 같은 order를 보고 둘 다 `PAID`에서 `CANCELLED`로 바꾸려고 할 수 있다. 단순 in-memory check만으로는 DB write race를 막지 못한다. BLOCK 04의 optimistic locking과 transaction으로 연결된다.

### 5. 작은 문제

`status`를 client가 자유롭게 PATCH하게 하는 API는 왜 위험한가?

허용된 business transition을 우회해 임의 상태를 만들 수 있다. `cancelOrder`, `shipOrder`처럼 의도 중심 command가 더 안전할 수 있다.

---

## LESSON 23 · invariant는 시스템이 성공 상태라면 반드시 참이어야 하는 조건이다

### 1. 예를 든다

```text
주문 총액은 0 미만이 아니다.
재고 수량은 정책상 허용하지 않는 음수가 되지 않는다.
결제 완료 주문은 결제 참조 ID를 가진다.
한 이메일은 tenant 안에서 중복될 수 없다.
```

이런 조건이 invariant다.

### 2. validation과 invariant는 범위가 다르다

`quantity`가 정수인지 확인하는 것은 input validation일 수 있다. `주문을 확정한 뒤 모든 item의 재고가 확보되었다`는 것은 여러 데이터와 상태를 포함한 invariant다.

### 3. precondition과 postcondition

```text
precondition  : 함수를 실행하기 전에 필요한 조건
postcondition : 성공적으로 끝났을 때 보장할 조건
```

예: `shipOrder`의 precondition은 결제 완료 상태, postcondition은 shipped state와 shipping reference 존재일 수 있다.

### 4. 작은 문제

`if` 문을 handler에 넣었으니 invariant가 보장된다고 할 수 있는가?

다른 entry point가 같은 data를 바꿀 수 있고 concurrency가 개입할 수 있다. 중요한 invariant는 가능한 한 domain logic과 저장소 constraint 등 여러 경계에서 방어한다.

---

## LESSON 24 · idempotency는 같은 의도를 반복해도 결과를 중복시키지 않는 설계다

### 1. mobile network는 응답을 잃을 수 있다

client가 주문 생성 요청을 보냈고 server는 주문을 만들었다. 그런데 response가 이동 중 끊겼다. client는 `실패했나?`라고 생각하고 다시 보낼 수 있다.

### 2. 재시도와 중복 생성은 다른 문제다

POST가 반복되면 주문이 두 개 생길 수 있다. 그래서 client가 unique idempotency key를 보내고 server가 같은 key의 완료 결과를 재사용하는 설계를 쓸 수 있다.

```text
Idempotency-Key: 6d... unique value
```

### 3. key만 저장하면 끝이 아니다

같은 key로 서로 다른 body를 보내면 어떻게 할지, key 보관 기간은 얼마인지, 처리 중 동시 요청은 어떻게 막을지 정책이 필요하다.

### 4. HTTP method의 idempotent 의미와 application 효과를 구분한다

RFC의 method semantics와 실제 application 구현은 연결되지만 동일하지 않다. `PUT`이라고 자동으로 중복 side effect가 사라지는 것은 아니다.

### 5. 직접 사고 실험

결제 API에 charge 요청을 보낸 뒤 timeout이 났다. `실패했다`고 단정하고 즉시 다시 charge하면 이중 결제가 될 수 있다. 외부 provider의 idempotency/reconciliation contract를 확인해야 한다.

---

## LESSON 25 · 시간과 난수는 숨은 dependency로 만들지 않는다

### 1. 현재 시간에 따라 결과가 달라지는 규칙

```ts
function isCouponValid(coupon: Coupon) {
  return Date.now() < coupon.expiresAt.getTime();
}
```

test 시점에 따라 결과가 달라진다.

### 2. clock을 명시적으로 받는다

```ts
interface Clock {
  now(): Date;
}
```

production은 system clock, test는 fixed clock을 사용한다.

### 3. wall clock과 elapsed time을 구분한다

`몇 초가 지났는지` 재는 것과 `현재 달력 시간이 몇 시인지`는 다른 문제다. system clock은 동기화 조정으로 앞뒤로 변할 수 있다. timeout/elapsed measurement에는 monotonic clock 성격이 필요한 경우가 있다.

### 4. random도 주입할 수 있다

쿠폰 code, test fixture, retry jitter에서 random이 필요할 수 있다. 보안 token에 일반 pseudo-random 함수를 아무렇게나 쓰면 안 되고 secure random source를 사용해야 한다. 보안 심화는 TRACK 09로 넘긴다.

### 5. 작은 문제

테스트에서 `sleep(1000)` 후 coupon이 만료되기를 기다리는 방식은 왜 나쁜가?

느리고 flaky하다. fixed clock을 앞으로 이동시켜 즉시 경계 조건을 검증할 수 있다.

---

## LESSON 26 · transaction boundary는 `어디까지 함께 성공해야 하는가`를 정한다

### 1. DB transaction 문법보다 먼저 의미를 정한다

주문 생성 시:

```text
orders row 생성
order_items 여러 개 생성
재고 예약 기록 생성
```

이 세 write 중 하나만 남으면 invariant가 깨질 수 있다. 같은 database 안에서 atomic transaction으로 묶을 수 있다.

### 2. transaction을 너무 크게 잡지 않는다

DB transaction을 열어 둔 채 느린 외부 결제 API를 호출하면 lock과 connection을 오래 점유할 수 있다. 외부 시스템은 같은 DB transaction에 자동 참여하지 않는다.

### 3. application use case와 DB transaction은 1:1이 아닐 수 있다

하나의 use case가 여러 local transaction과 external step으로 나뉠 수 있다. 그 사이 실패를 어떻게 복구할지는 BLOCK 05의 saga/compensation에서 다룬다.

### 4. 작은 문제

`BEGIN` 안에서 email을 보냈는데 뒤 DB write가 rollback됐다. email도 자동 취소되는가?

아니다. DB transaction이 외부 email service side effect까지 되돌리지 않는다. transaction boundary를 실제 resource 기준으로 생각한다.

---

## LESSON 27 · 외부 API는 우리 코드 밖의 실패와 변경을 가진 dependency다

### 1. payment client를 직접 business code에 퍼뜨리지 않는다

```ts
const response = await fetch("https://pay.example/...", ...);
```

이 호출이 여러 service에 흩어지면 timeout, auth header, error mapping, version 변경을 일관되게 관리하기 어렵다.

### 2. adapter로 감싼다

```ts
interface PaymentGateway {
  authorize(input: PaymentRequest): Promise<PaymentResult>;
}
```

HTTP client adapter가 provider-specific JSON과 status를 application result로 번역한다.

### 3. 외부 200이 business success라는 보장은 없다

provider가 HTTP 200 body 안에 `status: declined`를 줄 수도 있다. 반대로 5xx 후 실제 처리는 완료됐을 수도 있다. provider contract를 확인해야 한다.

### 4. timeout은 결과 미확정일 수 있다

client timeout은 `상대가 처리하지 않았다`가 아니라 `우리가 제한 시간 안에 결과를 받지 못했다`는 뜻일 수 있다. 돈·주문 같은 side effect에서는 reconciliation 조회가 필요할 수 있다.

### 5. 직접 실습

fake payment server가 success, decline, 500, 3초 지연 네 동작을 하게 만들고 adapter가 각각 어떤 application result를 반환할지 정한다.

---

## LESSON 28 · ports and adapters는 핵심 코드와 외부 기술의 경계를 설명하는 방법이다

### 1. port는 핵심 코드가 필요로 하는 능력의 계약이다

```ts
interface OrderRepository { ... }
interface PaymentGateway { ... }
interface Clock { ... }
```

이 interface를 port라고 부를 수 있다.

### 2. adapter는 실제 기술과 연결한다

```text
PostgresOrderRepository
StripePaymentGateway
SystemClock
ExpressController
```

framework/DB/provider detail은 adapter 쪽에 모인다.

### 3. hexagonal architecture 그림을 과도하게 신성시하지 않는다

목표는 폴더 이름을 `ports/`, `adapters/`로 만드는 것이 아니다. 핵심 rule이 외부 SDK에 끌려다니지 않고 dependency direction을 통제하는 것이다.

### 4. 작은 문제

application service가 `PrismaClient` type을 public signature에 노출한다. 어떤 결합인가?

DB implementation detail이 application boundary까지 올라왔다. 교체 비용과 test 범위가 커질 수 있다.

---

## LESSON 29 · error taxonomy는 `누가 무엇을 할 수 있는 실패인가`로 나눈다

### 1. 실패를 층별로 생각한다

```text
ValidationError       → client 입력 수정 가능
NotFoundError         → resource 식별 확인
ConflictError         → 현재 상태와 command 충돌
DependencyTimeout     → 외부 dependency 결과 미확정/지연
InvariantViolation    → 내부 코드/데이터 문제 가능
```

### 2. catch-all로 모두 삼키지 않는다

```ts
try { ... }
catch { return null; }
```

이 코드는 `없음`과 `실패`를 구분하지 못하게 한다.

### 3. 재시도 가능한 오류와 불가능한 오류

잘못된 이메일 형식을 100번 retry해도 성공하지 않는다. 일시적인 network timeout은 retry가 의미 있을 수 있다. error type이 retry policy의 입력이 된다.

### 4. 원인 chain을 보존한다

외부 library error를 application error로 번역하더라도 server 내부 log/exception cause에서 root cause를 잃지 않는 것이 진단에 유리하다. client에게는 내부 detail을 숨긴다.

### 5. 작은 문제

`UserNotFound`를 500으로 보내면 무엇이 잘못되는가?

application에서 예상 가능한 상태와 server의 unexpected failure를 섞어 운영 metric과 client 행동을 왜곡한다.

---

## LESSON 30 · BLOCK 02 실전: 주문 생성 use case를 얇은 HTTP와 두꺼운 규칙으로 재구성한다

### 1. 출발 코드

한 handler에 validation, product 조회, 가격 계산, 결제, 저장, JSON response가 모두 들어 있다고 가정한다.

### 2. 목표 구조

```text
HTTP adapter
  └─ input parse/validate
     └─ CreateOrderService
        ├─ ProductRepository
        ├─ PaymentGateway
        ├─ OrderRepository
        └─ Clock
  └─ response mapper
```

### 3. 구현 순서

1. `CreateOrderInput`을 transport와 분리한다.
2. quantity/value rule을 domain type 또는 pure function으로 만든다.
3. repositories/gateway interface를 만든다.
4. service constructor로 dependency를 받는다.
5. handler는 validation과 HTTP translation만 남긴다.
6. fake dependencies로 service test를 만든다.
7. real adapter와 연결한다.

### 4. 반드시 검증할 failure mode

```text
상품 없음
판매 중지
수량 0
결제 거절
결제 timeout
저장 실패
중복 idempotency key
잘못된 상태 전이
```

### 5. 구조를 평가하는 질문

- HTTP 없이 핵심 주문 규칙을 실행할 수 있는가?
- 실제 payment network 없이 주요 failure를 test할 수 있는가?
- DB SDK type이 domain/public application API에 새어 나오는가?
- handler가 business rule을 중복해서 가지고 있는가?
- 어떤 operation이 상태를 바꾸는지 이름과 type에서 보이는가?
- transaction이 실제로 되돌릴 수 있는 resource 범위 안에 있는가?

### 6. 사람이 반드시 이해해야 하는 것

`controller/service/repository`라는 단어보다 **왜 이 코드가 이 위치에 있어야 하는지**를 설명할 수 있어야 한다. AI는 폴더와 class를 빠르게 만들어 줄 수 있지만, 실제 business invariant와 side effect 순서를 모르고는 올바른 경계를 결정할 수 없다.

### 7. AI 사용 규칙

AI에게 `Clean Architecture로 만들어 줘`라고만 하지 않는다. 다음처럼 concrete contract를 준다.

```text
CreateOrderService는 HTTP type을 import하지 말 것.
PaymentGateway는 authorize 결과를 success/declined/unknown으로 구분할 것.
Clock은 주입 가능해야 할 것.
Domain error와 infrastructure error를 구분할 것.
외부 timeout을 결제 실패로 단정하지 말 것.
```

이렇게 요구하면 AI가 낸 구조를 사람이 검토할 기준이 생긴다.
