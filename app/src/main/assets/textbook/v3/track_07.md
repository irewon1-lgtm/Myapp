# TRACK 07 · 서버와 백엔드

TRACK 06에서는 browser와 server가 network를 통해 HTTP request와 response를 주고받는 과정을 배웠다.

이번 TRACK에서는 **request가 server 안으로 들어온 뒤 어떤 길을 지나가는지** 본다.

초보자는 backend를 배우면서 route, middleware, controller, service, repository, cache, queue 같은 이름을 한꺼번에 외우기 쉽다.

이번에는 반대로 간다.

하나의 요청을 계속 따라간다.

```text
POST /orders
↓
server가 request를 받음
↓
route 찾기
↓
body 읽기
↓
입력 검사
↓
로그인 사용자 확인
↓
권한 확인
↓
주문 규칙 실행
↓
DB 저장
↓
필요하면 결제/알림 같은 외부 작업
↓
response 반환
```

각 용어는 이 흐름에서 실제 역할이 생길 때만 이름을 붙인다.

---

## BLOCK 01 · 서버는 요청을 기다리고 알맞은 코드로 보낸다

### LESSON 01 · backend·process·socket·framework·route·handler를 가장 작은 HTTP server에서 배운다

#### 1. backend는 사용자가 직접 보는 화면 뒤에서 일하는 부분이다

쇼핑 앱을 연다.

사용자가 보는 것:

```text
상품 카드
검색창
주문 버튼
결제 화면
```

이것이 주로 frontend 영역이다.

그 뒤에는:

```text
사용자 확인
상품 데이터 조회
주문 금액 계산
재고 확인
DB 저장
결제 API 호출
알림 전송
```

같은 일이 있다.

이런 server 쪽 기능과 시스템을 넓게 **backend**라고 부른다.

#### 2. server라는 말은 물리 컴퓨터와 프로그램을 모두 가리킬 수 있다

일상에서는 server라고 하면 데이터센터 컴퓨터 한 대를 떠올린다.

개발에서는:

```text
물리/가상 machine
그 machine에서 실행되는 server program
여러 server program이 모인 system
```

등 문맥에 따라 폭넓게 사용한다.

그래서 `server가 죽었다`라는 말도:

```text
machine 전원 문제
process 종료
application hang
network 차단
```

중 어느 것인지 구체적으로 확인해야 한다.

#### 3. 실행 중인 server program은 process다

저장장치에 코드가 있다고 server가 동작하는 것은 아니다.

실제로 실행돼 있어야 한다.

실행 중인 프로그램의 한 단위를 **process**라고 부른다.

```text
server.js 파일
↓ node server.js 실행
Node.js process가 생김
↓
request 대기
```

process가 종료되면 그 server도 request를 처리하지 못한다.

#### 4. server는 network 통신 끝점에서 request를 기다린다

TRACK 06에서 IP와 port를 배웠다.

server program은 특정 network endpoint에서 연결을 기다린다.

이때 저수준 통신 끝점 개념으로 **socket**이 있다.

아주 쉽게:

> socket = 프로그램이 network 데이터를 주고받는 통신 끝점

server는 특정 port에 socket을 열어 connection/request를 받을 수 있다.

#### 5. 가장 작은 Node.js HTTP server를 본다

```javascript
import http from "node:http";

const server = http.createServer((req, res) => {
  res.statusCode = 200;
  res.setHeader("Content-Type", "text/plain; charset=utf-8");
  res.end("안녕하세요");
});

server.listen(3000);
```

한 줄씩 본다.

```text
http module 가져오기
↓
HTTP server 만들기
↓
request가 올 때 실행할 callback 등록
↓
200 status 설정
↓
text response라는 Content-Type 설정
↓
"안녕하세요" body 보내고 response 종료
↓
3000 port에서 대기
```

#### 6. localhost는 내 컴퓨터 자신을 가리키는 이름으로 자주 쓴다

server를 실행하고 browser에서:

```text
http://localhost:3000
```

을 연다.

`localhost`는 현재 machine 자신을 가리키는 host name으로 자주 사용한다.

3000은 server가 listen하는 port다.

다른 device에서 `localhost:3000`을 열면 그 다른 device 자신을 가리킨다. 내 PC server를 가리키는 것이 아니다.

이 실수는 모바일 테스트에서 자주 나온다.

#### 7. server가 request를 받을 때 method와 path가 다를 수 있다

```text
GET /users
POST /users
GET /orders
DELETE /orders/10
```

모든 request를 한 함수에 if 수십 개로 처리할 수도 있다.

하지만 route별로 나누면 읽기 쉽다.

#### 8. route는 어떤 request를 어떤 코드가 처리할지 정하는 규칙이다

예:

```text
GET /users
→ 사용자 목록 함수

GET /users/:id
→ 한 사용자 함수

POST /users
→ 사용자 생성 함수
```

이런 method/path와 처리 코드의 연결을 **route**라고 부른다.

여러 route를 관리하는 구성요소를 **router**라고 부르기도 한다.

#### 9. framework는 반복되는 server 일을 줄여 준다

저수준 HTTP parsing과 routing을 매번 직접 구현할 필요는 없다.

Express, Fastify, FastAPI, Spring Boot, NestJS 같은 framework가:

```text
routing
body parsing
middleware
error handling
request/response helper
```

등을 제공한다.

Express 예:

```javascript
import express from "express";

const app = express();

app.get("/users", (req, res) => {
  res.json([{ id: 1, name: "민수" }]);
});

app.listen(3000);
```

#### 10. handler는 request를 실제로 처리하는 함수다

```javascript
(req, res) => {
  res.json(...);
}
```

이 함수처럼 특정 route request를 처리하는 코드를 **handler**라고 부른다.

framework에 따라 controller method라고 부르기도 한다.

이름보다 역할을 본다.

```text
request 입력
↓
처리
↓
response 출력
```

#### 11. route parameter는 URL 경로 안의 값이다

```text
GET /users/123
```

`123`은 사용자마다 달라진다.

Express:

```javascript
app.get("/users/:id", (req, res) => {
  console.log(req.params.id);
});
```

`:id`가 route parameter 자리다.

#### 12. URL에서 왔다고 안전한 값이 아니다

```text
/users/123
/users/abc
/users/-100
/users/999999999999
```

모두 client가 만들 수 있다.

route param도 사용자 입력이다.

숫자 ID를 기대한다면:

```javascript
const id = Number(req.params.id);

if (!Number.isInteger(id) || id <= 0) {
  return res.status(400).json({ error: "invalid id" });
}
```

처럼 validation이 필요하다.

#### 13. query와 body도 모두 외부 입력이다

```text
GET /users?active=true
```

query string도 입력이다.

```json
{
  "name": "민수",
  "age": 20
}
```

request body도 입력이다.

`frontend가 만든 request니까 안전하다`고 생각하면 안 된다. 누구나 API를 직접 호출할 수 있다.

#### 14. server request의 첫 흐름을 그림으로 만든다

```text
network에서 request 도착
↓
server process
↓
HTTP/framework가 message parsing
↓
router가 method+path 확인
↓
알맞은 handler 선택
↓
handler가 request data 사용
↓
response 생성
```

뒤 LESSON에서 이 사이에 middleware, validation, auth, service 등이 추가된다.

#### 15. 초보자가 자주 하는 실수

- `localhost`가 모든 device에서 내 개발 PC를 뜻한다고 생각한다.
- route parameter와 query/body를 안전한 내부 값으로 생각한다.
- port connection 실패와 HTTP 404를 같은 문제로 본다.
- framework가 모든 보안과 validation을 자동으로 해 준다고 생각한다.
- handler 하나에 모든 business logic을 넣는다.

#### 16. 책을 덮고 확인한다

1. backend를 frontend와 비교해 설명하라.
2. 저장된 server 코드와 실행 중 process는 무엇이 다른가?
3. socket과 port는 어떤 관계인가?
4. route와 handler를 `GET /users/:id` 예로 설명하라.
5. localhost를 다른 휴대폰에서 열 때 왜 개발 PC가 아닐 수 있는가?
6. route parameter도 validation해야 하는 이유는 무엇인가?

---

## BLOCK 02 · 요청이 business logic에 도착하기 전에 안전하게 정리한다

### LESSON 01 · parsing·middleware·validation·normalization·sanitization·error handler를 회원가입 request로 배운다

#### 1. request body는 처음부터 JavaScript object가 아니다

network로 들어오는 것은 byte data다.

HTTP header에:

```http
Content-Type: application/json
```

이 있고 body가 JSON text라면 framework가 body를 읽고 JSON parser를 사용해 object로 만들어 줄 수 있다.

```text
HTTP body bytes
↓ charset/format 해석
JSON text
↓ JSON parse
JavaScript object
```

이 과정을 몰라서 body parser가 빠졌는데 `req.body가 undefined`라는 문제를 만날 수 있다.

#### 2. parsing과 validation은 다른 일이다

body:

```json
{"age":-900}
```

JSON 문법은 완벽하다.

parser는 성공한다.

하지만 나이로서 올바르지 않다.

```text
parsing
→ 데이터 형식이 문법적으로 읽히는가?

validation
→ 우리 시스템 규칙에 맞는 값인가?
```

이다.

#### 3. middleware는 handler 전후 공통 처리를 연결한다

여러 route에서 반복되는 일이 있다.

```text
request log
JSON body parsing
인증 token 확인
rate limit
CORS header
공통 error 처리
```

이런 공통 단계를 request 흐름 사이에 끼워 넣는 구조를 **middleware**라고 부른다.

Express 그림:

```text
request
↓
logging middleware
↓
JSON parser
↓
auth middleware
↓
route handler
↓
error handler
↓
response
```

#### 4. middleware 순서는 중요하다

예:

```javascript
app.use(express.json());
app.use(authMiddleware);
app.post("/orders", orderHandler);
```

body parser가 먼저 있어야 auth가 JSON body를 사용해야 하는 경우 정상적으로 읽을 수 있다.

error middleware도 framework 규칙에 맞는 위치가 필요할 수 있다.

`middleware는 목록만 있으면 순서가 아무 상관없다`고 생각하면 안 된다.

#### 5. validation은 입력이 요구조건에 맞는지 확인한다

회원가입 request:

```json
{
  "email": "user@example.com",
  "password": "...",
  "age": 20
}
```

검사:

```text
email이 문자열인가?
비어 있지 않은가?
길이가 허용 범위인가?
password가 문자열인가?
age가 정수인가?
age 범위가 현실적인가?
필요 없는 필드가 들어왔는가?
```

validation은 단순 `필수값 있나`보다 넓다.

#### 6. 타입 하나만 확인해도 충분하지 않다

```javascript
if (typeof age === "number") { ... }
```

만 보면:

```text
NaN
Infinity
-1000000
3.14159
```

같은 값이 통과할 수 있다.

우리 domain이 `사람 나이의 정수`를 원하면:

```javascript
Number.isInteger(age) && age >= 0 && age <= 150
```

같은 규칙이 필요할 수 있다.

#### 7. validation schema를 사용하면 규칙을 한곳에 둘 수 있다

예를 들어 Zod 같은 도구를 사용한다고 하자.

```javascript
const UserSchema = z.object({
  email: z.string().email(),
  age: z.number().int().min(0).max(150)
});
```

도구 이름이 중요한 것이 아니다.

```text
입력 규칙을 선언
↓
실제 외부 값을 검사
↓
성공한 값만 business logic으로 보냄
```

이라는 구조가 중요하다.

#### 8. normalization은 같은 의미의 표현을 일관되게 바꾼다

이메일:

```text
 User@Example.COM 
```

을 처리할 때:

```text
앞뒤 공백 제거
정책에 맞는 case normalization
```

을 할 수 있다.

전화번호:

```text
010-1234-5678
01012345678
```

를 내부 표준 형식으로 바꾸는 것도 normalization의 예다.

다만 의미가 바뀌는 데이터를 무심코 변환하면 안 된다.

#### 9. sanitization은 위험한 내용을 무조건 지우는 만능 보안이 아니다

사용자 HTML 입력에서 script 관련 위험을 제거하는 작업을 sanitization이라고 부를 수 있다.

하지만 SQL injection은 `문자 몇 개 지우기`보다 parameterized query를 쓰는 것이 핵심이다.

XSS도 출력 context에 맞는 escaping/안전한 DOM API가 중요하다.

`sanitize 함수를 한 번 돌리면 모든 공격이 사라진다`고 생각하면 위험하다.

#### 10. validation 오류는 400 계열 response로 구조화할 수 있다

```json
{
  "code": "INVALID_INPUT",
  "message": "입력값을 확인해 주세요",
  "fields": {
    "age": "0~150 사이 정수를 입력하세요"
  }
}
```

client가 field별 오류를 표시하기 쉽다.

#### 11. business rule validation과 형식 validation을 구분한다

형식:

```text
email이 올바른 문자열 모양인가?
```

business rule:

```text
이미 가입된 email인가?
이 사용자는 이 쿠폰을 사용할 수 있는가?
주문 가능한 시간인가?
```

둘 다 validation이지만 뒤의 규칙은 DB/현재 상태를 조회해야 할 수 있다.

#### 12. error handling을 route마다 복사하지 않는다

나쁜 방향:

```javascript
app.get("/a", async (...) => {
  try { ... } catch { ... }
});

app.get("/b", async (...) => {
  try { ... } catch { ... }
});
```

framework에서 공통 error middleware/helper를 만들어 error를 일관된 response로 바꿀 수 있다.

#### 13. 내부 오류와 외부 오류를 구분한다

DB 연결 실패 stack trace:

```text
DatabaseError at /home/server/src/...
password=...
```

를 사용자에게 그대로 보내면 내부 정보가 노출된다.

외부 response:

```json
{
  "code":"INTERNAL_ERROR",
  "message":"잠시 후 다시 시도해 주세요"
}
```

내부 log:

```text
request id
stack trace
DB error code
관련 context
```

처럼 역할을 나눈다.

#### 14. 회원가입 흐름을 처음부터 다시 연결한다

```text
POST /users
↓
Content-Type 확인/JSON parsing
↓
형식 validation
↓
normalization
↓
인증이 필요한 route면 auth
↓
중복 email business rule 검사
↓
password 안전 처리
↓
DB 저장
↓
201 response
```

#### 15. 일부러 실패시킨다

테스트 입력:

```json
{}
```

```json
{"email":123}
```

```json
{"email":"bad","age":-10}
```

```json
{"email":"existing@example.com"}
```

각각 어느 단계에서 막혀야 하는지 정한다.

#### 16. 책을 덮고 확인한다

1. parsing과 validation은 무엇이 다른가?
2. middleware는 request 흐름 어디에 들어갈 수 있는가?
3. middleware 순서가 중요한 이유는 무엇인가?
4. type 검사만으로 domain validation이 충분하지 않은 이유는 무엇인가?
5. normalization과 sanitization은 어떻게 다른가?
6. 내부 stack trace를 client에게 그대로 보내면 왜 위험한가?

---

## BLOCK 03 · route handler에서 업무 규칙을 떼어내 구조를 만든다

### LESSON 01 · business logic·service·repository·dependency를 주문 기능으로 배운다

#### 1. handler 하나가 500줄이 되는 문제

처음에는 이렇게 만들기 쉽다.

```javascript
app.post("/orders", async (req, res) => {
  // 입력 검사
  // 사용자 조회
  // 상품 조회
  // 재고 확인
  // 할인 계산
  // 배송비 계산
  // DB insert
  // 결제 API
  // 알림 전송
  // response
});
```

동작은 할 수 있다.

하지만 수정하기 어려워진다.

```text
할인 규칙만 테스트하려면 HTTP server가 필요함
DB를 바꾸면 handler 수정
결제를 바꾸면 handler 수정
500줄에서 오류 위치 찾기 어려움
```

역할을 나눈다.

#### 2. business logic은 우리 서비스의 업무 규칙이다

쇼핑몰 예:

```text
상품 수량 × 가격
회원 등급 할인
쿠폰 중복 규칙
무료배송 조건
재고 0이면 주문 금지
```

이런 규칙이 **business logic**이다.

HTTP가 없어도 개념적으로 존재한다.

#### 3. service는 use case 실행을 맡는 층으로 사용할 수 있다

프로젝트마다 이름과 구조는 다르지만 예:

```javascript
class OrderService {
  async createOrder(command) {
    // 주문 규칙 실행
  }
}
```

handler:

```javascript
app.post("/orders", async (req, res) => {
  const order = await orderService.createOrder(req.body);
  res.status(201).json(order);
});
```

handler는 HTTP request/response에 집중하고 service는 주문 use case에 집중한다.

#### 4. repository는 저장소 접근을 감추는 경계로 사용할 수 있다

service가 SQL 문장을 직접 수십 개 알고 있으면 DB 구조와 강하게 묶인다.

```javascript
class OrderRepository {
  async findById(id) { ... }
  async save(order) { ... }
}
```

service:

```javascript
const order = await orderRepository.save(newOrder);
```

service는 `어떤 SQL로 저장하는지`보다 `주문을 저장한다`는 의미에 집중한다.

#### 5. repository가 반드시 필요한 것은 아니다

작은 CRUD 앱에서 모든 table마다 repository/interface/service를 강제로 만들면 오히려 코드가 늘어난다.

구조는 규모와 변경 요구를 보고 결정한다.

목표는 layer 수를 늘리는 것이 아니라 **책임과 변경 이유를 분리하는 것**이다.

#### 6. 책임을 문장으로 설명할 수 있어야 한다

```text
OrderController/handler
→ HTTP 입력/출력 변환

OrderService
→ 주문 use case와 business rule

OrderRepository
→ 주문 persistence 접근
```

한 class가:

```text
HTTP parsing
DB query
결제 SDK
메일 HTML
파일 압축
```

을 모두 책임진다면 수정 이유가 너무 많을 수 있다.

#### 7. dependency는 다른 구성요소가 필요하다는 관계다

OrderService가 OrderRepository와 PaymentGateway를 사용한다.

```text
OrderService
├─ OrderRepository
└─ PaymentGateway
```

이때 OrderService는 두 구성요소에 **dependency(의존)**가 있다.

#### 8. dependency를 내부에서 직접 만들면 테스트가 어려울 수 있다

```javascript
class OrderService {
  constructor() {
    this.repository = new PostgresOrderRepository();
    this.payment = new RealPaymentGateway();
  }
}
```

unit test를 하는데 실제 DB와 실제 결제가 필요해질 수 있다.

밖에서 전달받게 만든다.

```javascript
class OrderService {
  constructor(repository, payment) {
    this.repository = repository;
    this.payment = payment;
  }
}
```

이것이 dependency injection과 연결된다.

#### 9. test에서는 fake를 넣을 수 있다

```javascript
const fakeRepository = {
  async save(order) {
    return { ...order, id: 1 };
  }
};

const fakePayment = {
  async charge() {
    return { success: true };
  }
};

const service = new OrderService(fakeRepository, fakePayment);
```

실제 돈을 결제하지 않고 business logic을 시험할 수 있다.

#### 10. data model과 domain model이 항상 같은 모양일 필요는 없다

DB row:

```text
order_id
user_id
status_code
created_at
```

domain object:

```text
id
customerId
status
createdAt
```

HTTP response:

```json
{
  "id": 10,
  "status": "paid"
}
```

각 경계의 목적에 맞게 변환할 수 있다.

DB schema를 그대로 client에 노출하면 내부 변경이 API까지 강하게 묶일 수 있다.

#### 11. transaction boundary는 business use case와 함께 생각한다

주문 생성:

```text
주문 row 생성
재고 차감
결제 기록
```

일부 DB 변경은 하나의 transaction으로 묶어야 할 수 있다.

service/use case 층이 어느 변경들을 하나의 업무 단위로 볼지 결정하는 데 적합한 경우가 많다.

DB transaction은 TRACK 08에서 깊게 배운다.

#### 12. 외부 결제를 DB transaction 안에서 오래 기다리면 문제가 될 수 있다

DB lock을 잡은 채 외부 결제 API를 10초 기다리면 다른 transaction을 오래 막을 수 있다.

그래서:

```text
DB transaction 범위
외부 side effect
재시도
보상 처리
```

를 함께 설계해야 한다.

단순히 모든 일을 하나의 transaction 안에 넣는다고 안전한 것이 아니다.

#### 13. handler→service→repository 흐름을 그린다

```text
HTTP request
↓
handler
입력 DTO 생성
↓
OrderService
business rule
↓
repository
DB
↓
service result
↓
handler
HTTP response 변환
```

이 그림을 설명할 수 있으면 class 이름을 외우지 않아도 구조를 이해할 수 있다.

#### 14. 책을 덮고 확인한다

1. business logic을 HTTP handler와 분리하는 이유는 무엇인가?
2. service와 repository를 각각 한 문장으로 설명하라.
3. repository가 모든 앱에 무조건 필요한 것은 아닌 이유는 무엇인가?
4. dependency injection이 test에 어떤 도움을 주는가?
5. DB model과 API response model이 달라도 되는 이유는 무엇인가?
6. 외부 API 호출을 DB transaction 안에서 오래 기다리는 것이 왜 문제일 수 있는가?

---

## BLOCK 04 · 로그인한 사용자가 어떤 작업을 할 수 있는지 서버에서 결정한다

### LESSON 01 · authentication·authorization·RBAC·ownership·server state를 실제 주문 API로 배운다

#### 1. 로그인과 권한은 다른 질문이다

```text
authentication
→ 누구인가?

authorization
→ 무엇을 할 수 있는가?
```

일반 사용자 민수와 관리자 지수가 둘 다 로그인했다.

둘 다 authentication은 성공했다.

하지만:

```text
민수 → 자기 주문 조회
지수 → 모든 사용자 관리
```

처럼 authorization은 다르다.

#### 2. client가 보내는 userId를 그대로 믿지 않는다

위험한 API:

```http
GET /orders?userId=123
Authorization: Bearer ...
```

server가 query의 userId만 보고 데이터를 반환한다면 공격자가:

```text
userId=124
```

로 바꿔 다른 사람 주문을 볼 수 있다.

인증 정보에서 확인된 user identity와 requested resource의 소유권을 비교해야 한다.

#### 3. ownership check를 한다

```text
request user = 123
order.ownerId = 123
→ 허용

request user = 123
order.ownerId = 999
→ 거부
```

이런 object-level authorization을 빼먹으면 IDOR/BOLA 같은 취약점으로 이어질 수 있다.

#### 4. UI에서 버튼을 숨기는 것은 authorization이 아니다

```javascript
if (!isAdmin) {
  hideAdminButton();
}
```

화면에서 버튼이 안 보인다.

하지만 공격자는 API URL을 직접 호출할 수 있다.

server에서:

```text
현재 인증 사용자 role 확인
↓
admin인지 확인
↓
아니면 403
```

를 해야 한다.

#### 5. RBAC는 role에 권한을 묶는 방식이다

```text
user
→ 자기 profile 조회/수정

manager
→ 팀 데이터 조회

admin
→ 사용자 관리
```

이처럼 **role**에 permission을 묶는 접근을 **RBAC(Role-Based Access Control)**라고 부른다.

규모가 커지면 role만으로 세밀한 정책을 표현하기 어려울 수 있어 attribute/policy 기반 방식도 사용한다.

#### 6. 최소 권한 원칙을 적용한다

server DB 계정이 모든 database를 drop할 권한까지 가질 필요가 없다면 주지 않는다.

```text
필요한 table
필요한 read/write 작업
```

만 허용한다.

이것이 **least privilege(최소 권한)** 원칙이다.

하나의 계정이 탈취돼도 피해 범위를 줄인다.

#### 7. server는 사용자 state를 어디에 둘까

session 기반 로그인에서는:

```text
session id
↓
server session store
↓
userId, login state
```

같은 구조가 있다.

여러 server instance로 scale out하면 session을 각 server memory에만 두었을 때 문제가 생길 수 있다.

```text
request 1 → server A, session 있음
request 2 → server B, session 없음
```

shared session store나 stateless token 전략 등을 고려한다.

#### 8. stateless라는 말은 "server가 아무 data도 저장하지 않는다"가 아니다

JWT access token을 사용해 인증 server state를 줄일 수 있다.

하지만:

```text
user DB
revocation 정책
refresh token
permission data
audit log
```

등 다른 state는 존재한다.

`JWT = 완전 무상태 = DB 필요 없음`은 틀린 단순화다.

#### 9. 인증 token 만료와 갱신을 설계한다

access token을 영원히 유효하게 두면 탈취됐을 때 오래 악용될 수 있다.

짧은 access token + 별도 refresh token 같은 전략을 사용할 수 있다.

하지만 refresh token도 안전한 저장, rotation, revoke 정책이 필요하다.

#### 10. password 변경/로그아웃 시 기존 session을 어떻게 할지 결정한다

비밀번호를 바꿨는데 탈취된 기존 session이 계속 유효하면 위험할 수 있다.

정책 예:

```text
현재 device만 유지
모든 session revoke
관리자 강제 logout
```

business/security 요구에 따라 결정한다.

#### 11. authorization check 위치를 일관되게 한다

route마다 개발자가 직접 if문을 빼먹기 쉽다.

공통 middleware/policy layer를 사용해:

```text
authenticate
↓
permission policy
↓
handler/service
```

로 만들 수 있다.

하지만 business object 소유권처럼 DB 데이터를 봐야 하는 검사는 service 안에서 수행될 수도 있다.

한곳에 모든 authorization이 들어가야 한다는 뜻은 아니고 **누락되지 않는 일관된 구조**가 중요하다.

#### 12. 401/403/404를 정보 노출 관점에서도 생각한다

다른 사람 private resource의 존재 자체를 숨기고 싶다면 authorization 실패에 404를 사용하는 API도 있을 수 있다.

HTTP 의미와 security 정보 노출 정책을 함께 본다.

#### 13. audit log가 필요한 민감 작업이 있다

관리자가 사용자를 삭제했다.

나중에:

```text
누가?
언제?
어떤 대상에게?
무슨 작업을?
성공/실패?
```

를 확인해야 할 수 있다.

이런 중요한 행위를 **audit log**로 남길 수 있다.

단 password/token 같은 secret을 log에 넣지 않는다.

#### 14. 주문 조회 API의 권한 흐름

```text
GET /orders/100
↓
access token/session 검사
↓
currentUser = 123
↓
order 100 조회
↓
order.userId == 123 ?
├─ yes → 반환
└─ no  → 권한 거부
```

admin 정책이 있다면 별도 조건을 명시한다.

#### 15. 책을 덮고 확인한다

1. authentication과 authorization을 구분하라.
2. query의 userId를 그대로 믿으면 어떤 공격이 가능한가?
3. ownership check는 무엇인가?
4. RBAC의 role과 permission 관계를 설명하라.
5. UI 버튼 숨김이 authorization이 아닌 이유는 무엇인가?
6. 여러 server instance에서 memory session만 쓰면 어떤 문제가 생길 수 있는가?
7. audit log에는 어떤 정보를 남기고 어떤 secret을 피해야 하는가?

---

## BLOCK 05 · 느리거나 실패할 수 있는 일을 server가 어떻게 다루는가

### LESSON 01 · DB·외부 API·cache·queue·background job·retry·idempotency를 주문/알림 시스템으로 배운다

#### 1. 모든 일을 request 안에서 끝낼 필요는 없다

주문 생성 request가 들어왔다.

할 일:

```text
주문 검증
DB 저장
결제 처리
영수증 email
추천 시스템 event
분석 log
배송 시스템 전달
```

모든 일이 끝날 때까지 HTTP response를 30초 기다리게 하면 사용자 경험이 나빠진다.

무엇이 request 성공에 반드시 필요한지 나눈다.

#### 2. synchronous path와 asynchronous follow-up을 구분한다

예:

```text
반드시 성공해야 주문 성공
- 재고 확인
- 주문 저장
- 결제 승인

나중에 처리 가능
- 확인 email
- 분석 event
- 추천 모델 갱신
```

요구사항에 따라 다르다.

#### 3. cache는 자주 읽는 데이터를 더 빠르게 제공한다

상품 정보 조회가 매번 DB를 읽는다고 하자.

변경이 드문 데이터 일부를 memory/Redis 같은 cache에 둘 수 있다.

```text
request
↓
cache에 있나?
├─ yes → 바로 반환
└─ no  → DB 조회 → cache 저장 → 반환
```

이 패턴을 cache-aside라고 부르기도 한다.

#### 4. cache는 빠르지만 오래된 데이터 문제를 만든다

DB 가격:

```text
10,000 → 9,000 변경
```

cache:

```text
아직 10,000
```

client가 오래된 가격을 볼 수 있다.

그래서 cache에는 **invalidation(언제 지울지/갱신할지)** 전략이 필요하다.

> 컴퓨터 과학에서 cache invalidation이 어려운 문제라고 자주 말하는 이유다.

#### 5. cache miss와 stampede를 생각한다

인기 상품 cache가 만료되는 순간 사용자 10,000명이 동시에 들어온다.

모두 cache miss가 되어 DB를 동시에 조회하면 부하가 폭증할 수 있다.

이런 cache stampede를 막기 위해:

```text
lock/single flight
stale data 일부 허용
TTL jitter
pre-warming
```

같은 전략을 사용할 수 있다.

#### 6. queue는 처리할 작업을 줄 세운다

email 전송 작업을 바로 하지 않고 message queue에 넣는다.

```text
web server
↓ "주문확인 email 보내기" message
queue
↓
worker
↓ email provider
```

request는 queue에 안전하게 작업을 넣고 빨리 response할 수 있다.

#### 7. background job/worker가 queue 작업을 처리한다

웹 request를 받는 process와 별도로 worker process가 작업을 소비한다.

```text
HTTP server
→ 빠른 request/response

worker
→ 시간이 걸리는 background 작업
```

#### 8. queue에 넣었다고 100% 정확히 한 번만 실행되는 것은 아니다

network와 process는 실패할 수 있다.

worker가 작업을 처리하고 성공했지만 ack 전에 죽었다.

queue가 같은 message를 다시 전달할 수 있다.

따라서 많은 queue 시스템에서는 **at-least-once delivery** 가능성을 생각해야 한다.

같은 작업이 두 번 실행돼도 안전한 설계가 중요하다.

#### 9. email은 두 번 보내도 불편하지만 결제는 두 번 하면 큰 문제다

작업마다 중복 영향이 다르다.

결제:

```text
job 1001
→ payment charge
```

worker가 재시도됐다고 돈을 다시 빼면 안 된다.

operation ID/idempotency key를 저장해서 같은 논리 작업을 한 번만 효과 있게 처리하도록 설계할 수 있다.

#### 10. retry는 실패했다고 무조건 반복하는 것이 아니다

일시적인 network timeout은 retry로 회복될 수 있다.

하지만:

```text
잘못된 password
잘못된 request format
권한 없음
```

같은 영구 오류는 반복해도 해결되지 않는다.

retryable error와 non-retryable error를 구분한다.

#### 11. exponential backoff로 재시도 간격을 늘린다

server가 장애인데 client 100만 개가 즉시 계속 재시도하면 더 큰 부하가 생긴다.

예:

```text
1차 실패 → 1초 후
2차 실패 → 2초 후
3차 실패 → 4초 후
4차 실패 → 8초 후
```

처럼 간격을 늘릴 수 있다.

여러 client가 정확히 동시에 재시도하지 않도록 **jitter**를 더하기도 한다.

#### 12. dead-letter queue는 계속 실패하는 작업을 분리한다

정해진 횟수 이상 계속 실패하는 message를 별도 queue에 보내 조사할 수 있다.

```text
normal queue
↓ 5회 실패
DLQ
↓
운영자 조사/수동 복구
```

계속 무한 retry하며 system 자원을 쓰는 것을 줄인다.

#### 13. 외부 API는 우리 transaction 안에 있지 않다

우리 DB update는 rollback할 수 있다.

하지만 이미 외부 SMS를 보냈거나 결제를 승인했다면 DB rollback만으로 외부 세계를 되돌릴 수 없다.

그래서 distributed workflow에서는:

```text
idempotency
outbox
saga/compensation
상태 machine
```

같은 더 복잡한 패턴이 필요할 수 있다.

초급에서는:

> DB transaction 하나가 인터넷의 모든 side effect까지 원자적으로 묶어 주지는 않는다.

를 이해한다.

#### 14. transactional outbox의 큰 아이디어를 맛본다

DB에 주문을 저장하고 event도 보내야 한다.

나쁜 상황:

```text
주문 DB 저장 성공
↓
process 죽음
↓
event queue 전송 실패
```

주문은 있는데 후속 작업이 없다.

outbox pattern에서는 같은 DB transaction에:

```text
주문 row
+
"order created" outbox row
```

를 함께 저장한다.

별도 worker가 outbox를 queue로 보낸다.

정확한 구현은 나중에 시스템 설계에서 더 공부한다.

#### 15. 주문 처리 전체를 나눈다

```text
HTTP request
↓ validation/auth
주문 생성 service
↓
DB transaction
- order 저장
- inventory 변경
- outbox event 저장
↓ commit
HTTP 201 response

background worker
↓ outbox/queue
email, analytics, shipping
```

#### 16. 책을 덮고 확인한다

1. 모든 후속 작업을 HTTP request 안에서 끝낼 필요가 없는 이유는 무엇인가?
2. cache가 빠르지만 어려운 점은 무엇인가?
3. queue와 worker는 어떤 역할을 나누는가?
4. message가 중복 전달될 수 있을 때 idempotency가 왜 중요한가?
5. retry하면 안 되는 오류 예를 들어라.
6. exponential backoff와 jitter의 목적은 무엇인가?
7. DB rollback만으로 이미 보낸 외부 email/결제를 되돌릴 수 없는 이유는 무엇인가?

---

## BLOCK 06 · 운영 server는 동시에 많은 요청을 받고 문제를 관찰할 수 있어야 한다

### LESSON 01 · concurrency·rate limit·logging·metric·trace·health check·graceful shutdown을 실제 장애 대응으로 배운다

#### 1. 개발 PC에서 한 명이 쓰는 것과 운영은 다르다

개발 중:

```text
사용자 1명
request 1개
```

운영:

```text
동시 사용자 10,000명
여러 request
DB connection 제한
외부 API rate limit
CPU/memory 제한
```

동시에 여러 request가 들어오는 상황을 **concurrency(동시성)** 관점으로 본다.

#### 2. JavaScript server가 한 process라고 request 하나만 처리하는 것은 아니다

Node.js는 event-driven I/O를 이용해 한 thread의 JavaScript 실행 흐름에서도 여러 network I/O 작업을 겹쳐 진행할 수 있다.

```text
request A → DB 기다림
그동안
request B → 다른 코드 실행 가능
```

하지만 CPU를 오래 잡는 JavaScript 계산은 event loop를 막을 수 있다.

CPU-heavy 작업은 worker thread/process/별도 service로 분리할 수 있다.

#### 3. shared state를 동시에 바꾸면 race condition이 생길 수 있다

server memory:

```javascript
let balance = 100;
```

두 request가 동시에:

```text
A: balance 읽음 100
B: balance 읽음 100
A: -80 → 20 저장
B: -50 → 50 저장
```

실제 합산 결과가 잘못될 수 있다.

DB transaction/lock/atomic operation 같은 동시성 제어가 필요하다.

#### 4. DB connection pool도 무한하지 않다

server request마다 새 DB connection을 무한히 만들 수 없다.

connection pool을 사용해 제한된 connection을 재사용한다.

```text
pool size 20
동시 DB 작업 100
→ 일부는 connection 반환을 기다림
```

pool을 너무 작게 하면 대기, 너무 크게 하면 DB가 과부하될 수 있다.

#### 5. rate limit은 한 client가 과도한 요청을 보내는 것을 제한한다

예:

```text
로그인 시도
분당 10회
```

초과:

```text
429 Too Many Requests
```

를 보낼 수 있다.

목적:

```text
brute force 완화
service 자원 보호
공정한 사용
비용 폭증 방지
```

#### 6. rate limit을 server 한 대 memory에만 두면 scale-out에서 어긋날 수 있다

server A와 B가 각각 10회 limit을 따로 세면 client가 총 20회 이상 시도할 수 있다.

여러 instance에서 일관된 limit이 필요하면 shared store/gateway 기반 rate limiter를 고려한다.

#### 7. 로그는 "무슨 일이 있었는지"를 기록한다

좋은 구조화 log 예:

```json
{
  "level":"error",
  "requestId":"req-123",
  "route":"POST /orders",
  "userId":10,
  "errorCode":"PAYMENT_TIMEOUT"
}
```

단순:

```text
ERROR!!!
```

보다 조사하기 좋다.

#### 8. secret과 개인정보를 log에 남기지 않는다

금지 후보:

```text
password
access token
private key
카드 번호 전체
민감한 개인정보
```

log도 별도의 sensitive data 저장소가 될 수 있다.

보존 기간과 접근 권한이 필요하다.

#### 9. request ID로 한 요청의 여러 log를 연결한다

```text
requestId=req-123
```

을 request 시작부터 DB/외부 API log까지 전파하면 장애 발생 시 한 요청의 흐름을 검색하기 쉽다.

여러 service를 거치면 trace context와 연결된다.

#### 10. metric은 시스템 상태를 숫자로 계속 본다

예:

```text
request per second
error rate
p50/p95/p99 latency
CPU
memory
DB connection usage
queue length
cache hit rate
```

한 건의 상세 log보다 전체 추세를 보는 데 좋다.

#### 11. 평균 latency만 보면 느린 사용자 일부를 놓칠 수 있다

평균 100ms인데:

```text
90% request = 50ms
10% request = 550ms
```

일 수 있다.

그래서 p95/p99 percentile을 함께 본다.

```text
p95 = request의 95%가 이 시간 이하
```

정도로 이해한다.

#### 12. trace는 한 request가 여러 service를 통과한 시간을 연결해 본다

```text
API Gateway 20ms
↓
Order Service 50ms
↓
DB 15ms
↓
Payment Service 800ms
```

어디서 오래 걸렸는지 찾는다.

distributed system에서 매우 유용하다.

#### 13. logs, metrics, traces를 함께 관측하는 것이 observability와 연결된다

```text
metric
→ error rate가 갑자기 증가함을 발견

trace
→ payment service에서 시간이 오래 걸림 발견

log
→ PAYMENT_TIMEOUT 상세 원인 확인
```

한 도구만으로 모든 문제를 해결하지 않는다.

#### 14. health check는 server가 traffic을 받을 상태인지 알려 준다

```text
GET /health
```

단순 process 생존(liveness)과 실제 dependency까지 준비됐는지(readiness)를 구분할 수 있다.

예:

```text
process는 살아 있음
하지만 DB migration 중
→ 아직 traffic 받으면 안 됨
```

#### 15. deploy 중 request를 갑자기 끊지 않는다 — graceful shutdown

새 version 배포로 process를 종료해야 한다.

진행 중 request를 즉시 끊으면 일부 작업이 실패할 수 있다.

**graceful shutdown** 흐름:

```text
새 request 받기 중지
↓
진행 중 request 완료 기다림
↓
DB/queue connection 정리
↓
process 종료
```

timeout을 두고 너무 오래 걸리면 강제 종료할 수도 있다.

#### 16. overload에서는 빨리 실패하는 것도 시스템 보호다

DB queue가 무한히 쌓이면 memory가 터질 수 있다.

일정 수준을 넘으면:

```text
503 반환
queue 길이 제한
backpressure
rate limit
```

등으로 새 일을 줄이는 것이 전체 service를 살릴 수 있다.

#### 17. 장애 대응 시나리오

상황:

```text
사용자: "주문 버튼이 계속 빙글빙글 돌아요"
```

조사:

```text
1. metric: p95 latency 상승 확인
2. trace: DB query가 5초 걸림
3. DB metric: connection pool 100% 사용
4. log: timeout 증가
5. 최근 deploy/traffic 변화 확인
6. 원인 수정 또는 traffic 완화
7. 같은 metric이 정상으로 돌아왔는지 확인
```

`코드 한 줄 보고 추측`이 아니라 증거를 연결한다.

#### 18. TRACK 07 완료 기준

책을 보지 않고 다음을 설명할 수 있어야 한다.

- backend/server/process/socket/port 관계
- route와 handler 흐름
- parsing과 validation 차이
- middleware의 역할과 순서
- business logic/service/repository 책임 분리
- dependency injection이 test에 주는 이점
- authentication/authorization/ownership check 차이
- cache의 hit/miss/invalidation 문제
- queue/background worker와 중복 message 문제
- retry/backoff/jitter/idempotency
- concurrency와 shared state race
- connection pool과 rate limit
- log/metric/trace의 역할 차이
- health check와 graceful shutdown

### TRACK 프로젝트 · 주문 API 한 요청의 생애

다음 request를 기준으로 설계를 그린다.

```http
POST /orders
Authorization: Bearer ...
Content-Type: application/json

{
  "productId": 100,
  "quantity": 2
}
```

반드시 다음 단계를 포함한다.

```text
1. route
2. body parsing
3. validation
4. authentication
5. authorization
6. product/stock business rule
7. price calculation
8. transaction boundary
9. repository 저장
10. idempotency
11. outbox/queue 후속 event
12. 201 response
13. structured log
14. metric
15. failure/retry policy
```

그리고 일부러 다음 실패를 넣는다.

```text
잘못된 quantity
권한 없는 사용자
DB timeout
결제 timeout
worker 중복 실행
server shutdown
```

각 실패가 어느 layer에서 발견되고 어떤 status/error/log를 남겨야 하는지 설명한다.

서버 코드를 많이 외우는 것이 목표가 아니다.

**request 하나가 어디로 들어와 어떤 검증과 규칙을 지나고, 어디서 저장되고, 실패하면 어떻게 관찰하고 복구할지를 끝까지 설명할 수 있어야 한다.**
