# TRACK 07 · 서버와 백엔드

# BLOCK 01 · 요청 한 건이 서버 안에서 지나가는 길

이 BLOCK은 서버를 처음 보는 사람을 기준으로 시작한다. `middleware`, `DTO`, `AsyncLocalStorage` 같은 단어부터 외우지 않는다. 휴대폰에서 **주문 버튼을 한 번 눌렀을 때 서버 안에서 무슨 일이 벌어지는지** 한 단계씩 따라간다.

학습 루프는 모든 LESSON에서 같다.

```text
개념 설명
→ 아주 쉬운 예
→ 코드 한 줄씩 해석
→ 직접 실행
→ 일부 수정
→ 작은 문제
→ 왜 맞고 틀렸는지 설명
```

이번 BLOCK에서 사용하는 예제는 Node.js + TypeScript다. 중요한 것은 특정 framework 암기가 아니라 request가 들어와 response가 나갈 때까지의 책임 경계를 읽는 능력이다.

---

## LESSON 01 · backend·server·process·port를 하나의 실행 그림으로 이해한다

### 1. 화면 뒤에서 일하는 프로그램이 필요하다

메모 앱에서 `저장` 버튼을 눌렀다고 하자. 화면은 버튼을 보여 줄 뿐이다. 다른 기기에서도 같은 메모를 보려면 누군가가 요청을 받아 저장하고 다시 꺼내 줘야 한다. 이 역할을 하는 프로그램을 넓게 backend라고 부른다.

처음에는 네 단어만 구분한다.

```text
backend  = 화면 뒤의 서버 측 기능 전체를 부르는 넓은 말
server   = 요청을 받아 응답하는 프로그램 또는 그 프로그램이 동작하는 시스템
process  = 지금 실제로 실행 중인 프로그램 한 인스턴스
port     = 한 컴퓨터 안에서 어느 네트워크 프로그램과 통신할지 구분하는 번호
```

`server.js` 파일이 디스크에 존재하는 것과 서버가 실행 중인 것은 다르다. `node server.js`를 실행해 process가 생겨야 요청을 받을 수 있다.

### 2. 가장 작은 HTTP server를 직접 실행한다

```ts
import http from "node:http";

const server = http.createServer((req, res) => {
  res.statusCode = 200;
  res.setHeader("Content-Type", "text/plain; charset=utf-8");
  res.end("hello backend");
});

server.listen(3000, () => {
  console.log("http://localhost:3000");
});
```

한 줄씩 읽는다.

- `import http ...` : Node.js가 제공하는 HTTP 기능을 가져온다.
- `createServer(...)` : 요청이 올 때 실행할 함수를 가진 server object를 만든다.
- `req` : 들어온 request를 읽는 객체다.
- `res` : 보낼 response를 만드는 객체다.
- `statusCode = 200` : 요청을 정상 처리했다는 HTTP 상태를 지정한다.
- `setHeader(...)` : body를 어떤 형식으로 해석할지 알려 준다.
- `end(...)` : body를 보내고 이 response를 끝낸다.
- `listen(3000)` : 3000번 port에서 연결을 받기 시작한다.

### 3. 직접 실행한다

터미널에서 server를 실행한 뒤 browser에서 `http://localhost:3000`을 연다. `hello backend`가 보이면 최소 경로가 실제로 연결된 것이다.

그다음 `3000`을 `4000`으로 바꾸고 **주소는 그대로 3000으로 접속**해 본다. 연결 자체가 실패한다. 다시 주소를 4000으로 바꾸면 응답한다.

여기서 배우는 핵심은 `404`와 `connection refused`가 다르다는 것이다. 전자는 대개 server까지 도달했지만 알맞은 resource를 못 찾은 경우이고, 후자는 그 endpoint에 연결 자체가 성립하지 않은 경우다.

### 4. localhost 함정을 실험한다

PC에서 server를 실행하고 휴대폰 browser에 `http://localhost:3000`을 입력하면 PC가 아니라 **휴대폰 자기 자신**을 가리킨다. 다른 device에서 접속하려면 개발 PC의 실제 network address와 방화벽/바인딩 조건을 확인해야 한다.

### 5. 작은 문제

서버 파일은 존재하고 `node server.js`도 실행했다. 그런데 3000 port 대신 4000 port에 listen한다. client가 3000으로 접속한다. 문제를 `route bug`라고 부르면 왜 틀렸는가?

정답의 핵심은 route를 검사하기 전에 transport endpoint 연결부터 실패할 수 있다는 것이다. 디버깅은 `파일 존재 → process 실행 → listen endpoint → HTTP route`처럼 바깥에서 안쪽으로 좁힌다.

### 6. 사람이 배울 것 / AI에게 맡길 것

**직접 이해해야 한다:** server file과 process의 차이, port의 역할, localhost의 의미, 연결 실패와 HTTP 오류의 차이.

**AI에게 맡겨도 된다:** 특정 OS에서 port를 확인하는 명령어, boilerplate server 코드 생성. 단, AI가 제시한 port와 실제 실행 port가 맞는지는 직접 확인한다.

---

## LESSON 02 · request와 response object에서 실제 입력과 출력을 찾는다

### 1. request는 단순히 URL 한 줄이 아니다

서버에 들어오는 HTTP request에는 method, target/path, headers, body 등 여러 정보가 있다. TRACK 06에서 HTTP message 자체를 배웠다면 여기서는 **서버 코드가 그 정보를 어디서 읽는지** 본다.

```ts
const server = http.createServer((req, res) => {
  console.log(req.method);
  console.log(req.url);
  console.log(req.headers["user-agent"]);
  res.end("ok");
});
```

request를 두 번 보내면서 출력이 어떻게 달라지는지 본다.

### 2. 서버가 보는 값과 사용자가 보는 화면은 다르다

browser 주소창에 `/products?limit=10`을 입력하면 server 쪽에서는 URL 문자열이 들어온다. framework는 이것을 더 편한 형태로 분해해 줄 수 있지만 원래 network에서 `JavaScript object`가 날아오는 것은 아니다.

이 구분이 중요한 이유는 parsing 오류와 business 오류를 섞지 않기 위해서다.

### 3. response는 한 번만 완료해야 한다

```ts
res.end("first");
res.end("second");
```

처럼 이미 끝난 response에 다시 쓰려 하면 오류가 난다. 여러 `if` branch에서 response를 보내고도 함수가 계속 진행되면 흔히 발생한다.

```ts
if (!req.url) {
  res.statusCode = 400;
  res.end("bad request");
  return;
}
```

`return`은 단지 보기 좋은 문법이 아니라 이미 response를 끝낸 뒤 아래 코드가 실행되지 않게 만드는 control-flow 장치다.

### 4. 직접 수정한다

`req.method === "GET"`이면 `read`, 그 외에는 `other`를 응답하도록 바꿔 본다. 그다음 `curl -X POST` 같은 도구로 POST를 보내 결과를 확인한다.

### 5. 실패를 일부러 만든다

response를 두 번 종료하는 코드를 넣고 어떤 오류가 생기는지 본다. 오류 message를 통째로 외우지 않는다. `response lifecycle`이 이미 끝났다는 사실을 stack trace와 함께 확인한다.

### 6. 작은 문제

handler 안에서 `res.statusCode = 404; res.end("not found")`를 실행한 뒤 아래에서 DB 조회가 계속된다. 사용자에게 응답은 갔으니 괜찮다고 볼 수 있는가?

아니다. 불필요한 DB 작업이 발생하고, 아래 코드가 또 response를 쓰거나 side effect를 만들 수 있다. **응답 완료와 함수 흐름 종료는 별도 문제**다.

---

## LESSON 03 · route와 handler는 요청을 알맞은 코드로 보내는 규칙이다

### 1. if문이 늘어나면 경로 찾기가 어려워진다

```ts
if (req.method === "GET" && req.url === "/users") { ... }
else if (req.method === "POST" && req.url === "/users") { ... }
else if (...) { ... }
```

작은 실험에는 충분하지만 API가 커지면 method와 path를 조합한 규칙을 한곳에서 관리하는 편이 낫다.

**route**는 `어떤 HTTP 요청을 어떤 처리 코드로 보낼지` 정하는 규칙이고, **handler**는 선택된 요청을 실제로 처리하는 함수다.

```text
GET /users      → listUsersHandler
GET /users/:id  → getUserHandler
POST /users     → createUserHandler
```

### 2. Express 예제를 읽는다

```ts
import express from "express";

const app = express();

app.get("/users/:id", (req, res) => {
  res.json({ id: req.params.id });
});

app.listen(3000);
```

`app.get`의 첫 번째 인자는 route pattern, 두 번째 인자는 handler다. `/users/10`과 `/users/20`은 같은 route pattern에 매칭되지만 `id` 값은 다르다.

### 3. route parameter는 신뢰할 수 없는 외부 입력이다

```ts
const id = Number(req.params.id);

if (!Number.isInteger(id) || id <= 0) {
  return res.status(400).json({ error: "invalid id" });
}
```

`/users/abc`, `/users/-1`, `/users/1.2`도 client가 만들 수 있다. UI에서 숫자만 보내도록 만들었다는 사실은 server validation을 대신하지 못한다.

### 4. 404와 405를 구분해 본다

resource path는 있는데 method를 지원하지 않는 경우와 path 자체를 못 찾은 경우는 의미가 다르다. framework의 default 처리에 맡길 수도 있지만 API contract를 설계할 때는 이 차이를 이해해야 한다.

### 5. 직접 수정한다

`GET /users/:id`에 이어 `DELETE /users/:id` route를 추가한다. 아직 실제 삭제는 하지 말고 `{ deletedId: ... }`만 반환한다. 같은 path라도 method가 다르면 다른 operation이라는 점을 눈으로 확인한다.

### 6. 작은 문제

`GET /users/10`에서 handler가 실행되지 않는다. 어디부터 확인할까?

1. process가 살아 있는가?
2. client가 맞는 host/port로 보냈는가?
3. method가 GET인가?
4. router에 `/users/:id`가 등록됐는가?
5. 더 앞의 middleware가 response를 끝내지 않았는가?

원인을 한 번에 추측하지 말고 요청이 흐르는 순서대로 확인한다.

---

## LESSON 04 · path parameter·query·header·body는 목적이 다르다

### 1. 모든 입력을 body에 넣지 않는다

예를 들어 상품 API를 만든다.

```text
GET /products/42
GET /products?category=book&limit=20
POST /products
Authorization: Bearer ...
```

- path parameter `42` : 어느 resource인지 식별한다.
- query `category`, `limit` : 조회 조건이나 표현 방식을 조절한다.
- header : 요청 자체의 부가 정보나 protocol metadata를 전달한다.
- body : 생성·변경할 데이터처럼 request payload를 전달한다.

이 구분은 절대 법칙이 아니라 API contract 설계의 기본 도구다.

### 2. 같은 이름이라도 출처가 다르면 별개 값이다

```text
/users/10?id=20
body: { "id": 30 }
```

server가 세 값을 섞어서 사용하면 버그가 생긴다. 특히 ownership이나 update target을 client body의 `id`로 믿으면 권한 문제가 커질 수 있다.

### 3. query는 문자열에서 시작한다

`?limit=10`의 `10`은 처음에는 문자열 표현이다. TypeScript type annotation을 붙였다고 runtime 값이 자동으로 integer가 되지 않는다.

```ts
const rawLimit = req.query.limit;
```

framework에 따라 타입이 `string | string[] | ...`처럼 넓을 수 있다. 실제 runtime validation과 변환이 필요하다.

### 4. 직접 실행한다

query `limit`을 읽고 1~100 범위 정수로 변환한다. `limit=abc`, `limit=0`, `limit=1000`, query 없음 네 경우를 각각 보내 본다.

정상값만 확인하면 validation을 배운 것이 아니다. **경계값과 잘못된 값**을 실제로 보내야 한다.

### 5. 작은 문제

client가 `Content-Type: application/json` 없이 JSON처럼 보이는 body를 보냈다. server가 무조건 JSON으로 해석해야 하는가?

아니다. body의 의미는 media type 계약과 연결된다. framework가 어떤 조건에서 parser를 선택하는지 알아야 한다.

---

## LESSON 05 · body parsing과 body size limit은 validation보다 앞선 문제다

### 1. byte를 object로 바꾸는 일이 먼저다

network로 들어오는 body는 JavaScript object 자체가 아니다. 대략 이런 단계가 있다.

```text
bytes 수신
→ HTTP framing 처리
→ Content-Type 확인
→ body 읽기
→ charset/format 해석
→ JSON parse
→ JavaScript value
```

`{"age": -900}`은 JSON 문법상 정상일 수 있다. 따라서 parsing 성공과 의미상 validation 성공은 다르다.

### 2. Express의 JSON parser를 사용한다

```ts
app.use(express.json({ limit: "100kb" }));
```

이 한 줄은 편하지만 두 가지를 구분해 읽어야 한다.

- JSON parsing을 middleware로 적용한다.
- 너무 큰 body가 memory와 CPU를 과도하게 쓰지 않도록 상한을 둔다.

size limit은 단지 성능 최적화가 아니라 resource abuse 방어이기도 하다.

### 3. malformed JSON을 직접 보낸다

```text
{"name":"kim"
```

닫는 중괄호가 없다. business handler에 도착하기 전에 parser 단계에서 실패할 수 있다. 이때 모든 parsing error를 500으로 보내면 `server 내부 버그`와 `client가 잘못 보낸 데이터`를 구분하기 어렵다.

### 4. 대용량 body를 일부러 보낸다

개발 환경에서 작은 limit을 걸고 그보다 큰 payload를 보내 본다. handler 시작 log가 찍히는지 확인한다. 찍히지 않는다면 실패가 handler 이전에 발생한 것이다.

### 5. 작은 문제

`req.body.email`이 undefined다. 가능한 원인을 세 가지 이상 나눈다.

- body parser가 등록되지 않았다.
- Content-Type이 parser 조건과 맞지 않는다.
- body 자체에 `email` field가 없다.
- middleware 순서 때문에 route보다 뒤에 parser를 등록했다.

오류를 `email 버그`라고 뭉뚱그리지 않고 request pipeline의 어느 단계인지 좁힌다.

---

## LESSON 06 · middleware는 요청 흐름 중간의 공통 단계를 만든다

### 1. 여러 route에서 반복되는 일을 찾는다

모든 handler에 아래 코드를 복붙한다고 하자.

```text
request log 남기기
사용자 확인하기
JSON body 검사하기
request id 만들기
```

중복이 커지고 한 route만 빠뜨릴 가능성도 생긴다. 이런 공통 작업을 handler 앞뒤 pipeline에 넣는 구조를 middleware라고 부른다.

### 2. 순서가 동작을 바꾼다

```ts
app.use(requestIdMiddleware);
app.use(express.json());
app.use(authMiddleware);
app.post("/orders", orderHandler);
```

각 middleware는 앞 단계가 만든 정보를 다음 단계가 사용할 수 있다. 따라서 단순한 목록이 아니라 **순서가 있는 실행 흐름**이다.

### 3. next의 의미를 읽는다

```ts
function timing(req, res, next) {
  const started = Date.now();
  res.on("finish", () => {
    console.log(Date.now() - started);
  });
  next();
}
```

`next()`를 호출하지 않고 response도 끝내지 않으면 request가 멈춰 보일 수 있다. 반대로 response를 끝낸 뒤 `next()`를 호출하면 뒤 middleware가 이미 끝난 response를 다루는 이상한 상태가 생길 수 있다.

### 4. 직접 실패시킨다

`next()`를 일부러 지운 middleware를 route 앞에 둔다. client가 계속 기다리는지 본다. 그다음 timeout이 어디서 발생하는지 확인한다.

### 5. middleware에 모든 business logic을 넣지 않는다

middleware는 공통 transport concern에 유용하지만 주문 가격 계산, 재고 정책, 환불 규칙처럼 domain별 핵심 규칙까지 전부 middleware chain으로 만들면 흐름을 추적하기 어려워진다. 책임을 어디에 둘지는 BLOCK 02에서 다룬다.

### 6. 작은 문제

`authMiddleware`가 `req.user`를 설정한다. route handler에서 `req.user!`처럼 무조건 있다고 단언해도 되는가?

TypeScript compiler를 만족시키는 것과 runtime pipeline이 실제로 보장하는 것은 다르다. 등록 순서, public route 예외, 테스트 환경까지 포함해 계약을 확인해야 한다.

---

## LESSON 07 · validation은 외부 값을 우리 프로그램이 다룰 수 있는 값으로 좁힌다

### 1. TypeScript type은 network 입력을 자동 검증하지 않는다

```ts
interface CreateUserBody {
  email: string;
  age: number;
}
```

이 interface는 compile time에서 개발자를 돕지만 internet에서 들어온 JSON이 정말 이 모양인지 runtime에 확인하지 않는다.

아래처럼 강제 단언하면 위험하다.

```ts
const body = req.body as CreateUserBody;
```

`as`는 검증이 아니라 compiler에게 `내 말을 믿어`라고 하는 문법에 가깝다.

### 2. schema로 runtime 검사를 한다

도구는 Zod, TypeBox, JSON Schema validator 등 무엇이든 될 수 있다. 핵심 구조는 같다.

```ts
const CreateUser = z.object({
  email: z.string().email(),
  age: z.number().int().min(0).max(150)
});

const result = CreateUser.safeParse(req.body);
```

### 3. validation은 타입보다 넓다

`age`가 number라는 것만 확인하면 `-100`, `3.14`, 지나치게 큰 값이 들어올 수 있다. domain이 사람 나이를 원한다면 range와 integer 조건이 필요하다.

문자열도 마찬가지다.

```text
""             → 비어 있음
"   "          → 공백뿐
10만 글자 문자열 → 메모리/저장 비용 문제
```

### 4. unknown에서 시작하는 습관

외부 input을 처음부터 trusted domain type으로 생각하지 않는다.

```ts
const raw: unknown = req.body;
```

검증을 통과한 뒤에만 좁은 타입을 사용한다. 이 사고방식은 TypeScript 문법보다 중요하다.

### 5. 직접 실습

`name`, `price`, `quantity`를 받는 주문 item schema를 만든다. 다음 값을 모두 보내 본다.

```text
price: -1
price: "1000"
quantity: 0
quantity: 1.5
name: ""
```

어떤 값은 변환할지, 어떤 값은 거부할지 정책을 직접 정한다.

### 6. 작은 문제

`"1000"`을 자동으로 숫자 1000으로 바꾸면 친절해 보인다. 항상 좋은가?

아니다. coercion은 편리하지만 잘못된 client contract를 조용히 숨길 수 있다. API가 number를 요구한다면 문자열을 거부하는 편이 오류를 빨리 발견하게 할 수 있다. 변환은 의도적으로 선택한다.

---

## LESSON 08 · normalization과 default는 validation과 같은 것이 아니다

### 1. 같은 의미를 하나의 내부 표현으로 맞춘다

사용자가 이메일에 앞뒤 공백을 넣을 수 있다.

```text
"  user@example.com  "
```

공백 제거 같은 처리를 normalization이라고 부를 수 있다. 전화번호 표기 통일, enum 대소문자 정책 등도 예가 된다.

하지만 모든 문자열을 무조건 lowercase로 만들면 안 된다. 값의 의미와 외부 규칙을 먼저 확인해야 한다.

### 2. default는 `누락`과 `명시된 값`을 구분한다

```ts
const pageSize = input.pageSize ?? 20;
```

`??`는 null/undefined일 때 default를 적용한다. `||`를 쓰면 `0`, 빈 문자열 같은 falsy 값까지 default로 덮어 버릴 수 있다.

### 3. validation → normalization 순서를 생각한다

어떤 시스템에서는 trim한 뒤 빈 문자열인지 검사하고, 어떤 시스템에서는 원문이 규칙에 맞는지 먼저 본다. 정답 하나가 있는 것이 아니라 **계약을 정하고 일관되게 지키는 것**이 중요하다.

### 4. 저장 원본과 검색용 정규화 값을 분리할 수도 있다

사용자 display name은 원래 표기를 보존해야 하지만 검색은 case-insensitive로 하고 싶을 수 있다. 하나의 field를 무조건 변형하기보다 원본과 비교용 표현의 역할을 나누는 설계가 필요하다.

### 5. 작은 문제

coupon code가 대소문자를 구분하는 외부 시스템과 연동된다. server가 편의상 `toUpperCase()`한 뒤 전송한다. 어떤 문제가 생길 수 있는가?

normalization이 의미를 바꿨다. `같아 보이는 표현을 통일`하는 것은 그 domain에서 실제로 같은 값일 때만 안전하다.

---

## LESSON 09 · 오류는 throw 한 번이 아니라 실패를 API 계약으로 번역하는 문제다

### 1. 실패 종류를 먼저 구분한다

```text
JSON 문법 오류
입력 validation 실패
로그인 필요
권한 없음
resource 없음
현재 상태에서 수행 불가
외부 결제 API timeout
프로그래머 버그
```

모두 `error`지만 client가 취할 행동은 다르다. 모든 실패를 500으로 보내면 client도 사람도 문제를 구분하기 어렵다.

### 2. domain error와 HTTP response를 분리한다

```ts
class OutOfStockError extends Error {}
```

service는 `OutOfStockError`를 만들 수 있고 HTTP edge에서 이를 `409 Conflict` 같은 표현으로 번역할 수 있다. business layer가 `res.status(409)`까지 직접 알게 만들 필요는 없다.

### 3. machine-readable error shape를 만든다

RFC 9457의 Problem Details처럼 공통 구조를 참고할 수 있다.

```json
{
  "type": "https://example.com/problems/out-of-stock",
  "title": "재고 부족",
  "status": 409,
  "detail": "요청 수량을 처리할 재고가 없습니다"
}
```

실제 API에서는 민감한 내부 stack trace나 SQL 문을 client에게 노출하지 않는다.

### 4. 중앙 error handler의 역할

각 route가 제각각 error JSON을 만들면 형식이 흔들린다. 중앙 error handler에서 `known application error`와 `unexpected error`를 분리하고 공통 response 계약을 적용할 수 있다.

### 5. 직접 실패시킨다

handler에서 일반 `Error`를 throw하고 error middleware가 500으로 변환하는지 본다. 그다음 validation error를 400 계열로 처리해 두 failure path가 다른지 확인한다.

### 6. 작은 문제

DB에서 `unique constraint`가 깨졌다. client에게 DB driver message를 그대로 보낼 것인가?

대개 아니다. 내부 구현 detail을 숨기고 `이미 사용 중인 이메일` 같은 application-level 의미로 번역한다. 동시에 server log에는 원인 진단에 필요한 evidence를 남긴다.

---

## LESSON 10 · response DTO와 serialization은 내부 객체를 그대로 밖으로 내보내지 않게 한다

### 1. DB row와 API response는 같은 객체일 필요가 없다

내부 사용자 객체에 이런 값이 있다고 하자.

```ts
{
  id: 10,
  email: "a@example.com",
  passwordHash: "...",
  internalRiskScore: 73,
  createdAt: ...
}
```

`res.json(user)`로 통째로 보내면 공개하면 안 되는 field가 섞일 수 있다.

### 2. 필요한 field만 선택한다

```ts
function toUserResponse(user: User) {
  return {
    id: user.id,
    email: user.email,
    createdAt: user.createdAt.toISOString()
  };
}
```

이처럼 **외부로 보낼 데이터 모양**을 별도로 정의한 것을 response DTO라고 부르기도 한다. 이름보다 `내부 representation과 외부 contract를 분리한다`는 목적이 중요하다.

### 3. serialization에서 타입이 달라질 수 있다

JavaScript `Date` object는 JSON에서 문자열로 표현된다. `BigInt`는 기본 JSON stringify에서 그대로 처리되지 않는다. `undefined` property가 사라지는 등 runtime object와 JSON representation은 완전히 같지 않다.

### 4. 직접 실습

user 객체에 `passwordHash`를 추가하고, response mapper를 거치면 빠지는지 확인한다. 테스트에서도 `passwordHash`가 response에 존재하지 않는 것을 검증한다.

### 5. 작은 문제

`toJSON()`을 generic entity에 달아 자동으로 모든 endpoint에서 쓰면 편하다. 위험은 무엇인가?

endpoint마다 공개 가능한 field가 다를 수 있다. 관리자 response와 일반 사용자 response가 같지 않을 수 있다. serialization 정책을 domain object 하나에 과도하게 숨기면 권한별 노출을 놓칠 수 있다.

---

## LESSON 11 · async handler에서는 실패가 Promise 경로를 따라 이동한다

### 1. 서버는 기다리는 동안 다른 일을 할 수 있다

DB나 다른 API를 기다리는 작업은 시간이 걸린다.

```ts
app.get("/users/:id", async (req, res) => {
  const user = await userRepository.findById(req.params.id);
  res.json(user);
});
```

`await`는 process 전체를 멈춘다는 뜻이 아니다. 현재 async function의 이어지는 실행을 결과가 준비될 때까지 미룬다. Node.js event loop의 세부는 TRACK 05와 연결된다.

### 2. reject된 Promise가 어디로 가는지 확인한다

framework 버전에 따라 async handler rejection을 error pipeline으로 전달하는 방식이 다를 수 있다. 특정 framework 문서를 확인해야 한다. `async면 자동으로 다 처리된다`고 일반화하지 않는다.

### 3. fire-and-forget은 실패를 잃기 쉽다

```ts
sendEmail();
res.json({ ok: true });
```

`sendEmail()` Promise를 await하지도, queue에 넣지도, 실패를 기록하지도 않으면 rejection이 유실되거나 예상치 못한 process 문제로 이어질 수 있다.

`사용자 응답과 분리하고 싶다`는 요구와 `아무도 책임지지 않는 Promise`는 다르다. 오래 걸리는 일은 BLOCK 05의 job/queue에서 다룬다.

### 4. 직접 실습

repository 함수가 50% 확률로 reject하도록 임시 구현한다. request를 여러 번 보내 error handler까지 가는지 확인한다. 그다음 `await`를 제거하고 관찰되는 차이를 기록한다.

### 5. 작은 문제

`Promise.all([a(), b(), c()])` 중 b가 실패했다. a와 c가 이미 외부 side effect를 시작했다면 자동 rollback되는가?

아니다. Promise 조합의 실패와 transaction/compensation은 별도 문제다. async 제어 흐름이 business 원자성을 자동으로 제공하지 않는다.

---

## LESSON 12 · request context는 로그와 하위 호출을 한 요청으로 묶는다

### 1. 동시 요청이 섞이면 console log만으로 추적하기 어렵다

두 사용자가 동시에 요청한다.

```text
start order
start order
load user
load user
finish order
finish order
```

어느 줄이 어느 request인지 모르면 incident 분석이 어려워진다.

### 2. request id를 만든다

request가 들어올 때 unique id를 만들고 response header와 log에 포함한다.

```text
requestId=abc123 route=/orders
requestId=abc123 step=load-user
requestId=abc123 step=save-order
```

이제 같은 request를 묶어 볼 수 있다.

### 3. 함수 인자로 계속 전달하는 방식

가장 명시적인 방법은 context를 함수 인자로 넘기는 것이다.

```ts
service.createOrder({ requestId, userId, input });
```

흐름이 분명하지만 함수마다 plumbing이 많아질 수 있다.

### 4. AsyncLocalStorage 같은 async context 도구

Node.js의 AsyncLocalStorage를 사용하면 하나의 async execution chain에 context를 연결할 수 있다. logger가 매번 requestId를 인자로 받지 않아도 현재 context에서 읽게 만들 수 있다.

하지만 context가 자동으로 모든 boundary를 안전하게 넘는다고 가정하면 안 된다. callback library, worker thread, message queue처럼 실행 경계가 달라지면 propagation 방법을 확인해야 한다.

### 5. trace id와 request id는 같을 수도 다를 수도 있다

분산 tracing에서는 trace/span context가 별도로 존재한다. local request id를 임의로 trace id와 동일시하지 않는다. BLOCK 06에서 둘을 연결한다.

### 6. 작은 문제

로그에 `userEmail`을 context로 항상 넣으면 추적하기 편하다. 좋은가?

관측 가능성과 개인정보 최소화가 충돌할 수 있다. 필요한 식별자는 가능한 한 민감정보를 피하고, retention/접근 정책과 함께 설계한다.

---

## LESSON 13 · config와 secret은 코드에서 분리하되 `환경변수면 안전하다`고 착각하지 않는다

### 1. 환경마다 달라지는 값이 있다

```text
PORT
DB_HOST
LOG_LEVEL
PAYMENT_API_BASE_URL
```

이런 값을 source code에 박아 두면 환경을 바꿀 때 코드 자체를 수정해야 한다. config를 외부에서 주입하면 같은 artifact를 여러 환경에서 쓸 수 있다.

### 2. secret은 일반 config보다 더 강하게 다룬다

```text
DB_PASSWORD
API_TOKEN
PRIVATE_KEY
```

이 값들은 유출되면 권한을 얻는 데 쓰일 수 있다. environment variable은 전달 방식 중 하나일 뿐 **자동 암호화 금고**가 아니다. process dump, debug log, CI 출력 등에 노출될 수 있다.

### 3. 시작 시 config를 검증한다

```ts
const port = Number(process.env.PORT ?? "3000");
if (!Number.isInteger(port) || port <= 0) {
  throw new Error("invalid PORT");
}
```

잘못된 config를 request를 받은 뒤 늦게 발견하기보다 startup에서 실패시키는 편이 안전한 경우가 많다.

### 4. config snapshot을 만든다

코드 곳곳에서 `process.env.X`를 직접 읽지 않고 startup에서 한 번 검증해 typed config object를 만드는 방식이 추적하기 쉽다.

```ts
const config = loadConfig(process.env);
```

### 5. 직접 실습

PORT를 `abc`로 넣고 server가 조용히 NaN을 사용하려 하지 않고 startup에서 명확히 실패하게 만든다. 그다음 `LOG_LEVEL`은 default를 허용하도록 구분한다.

### 6. 작은 문제

secret을 `.env` 파일에 넣고 `.gitignore`에 추가했다. 이제 완전히 안전한가?

아니다. 이미 commit된 적이 있는지, backup/chat/log에 복사됐는지, 접근권한과 rotation이 있는지까지 본다. secret 관리 심화는 TRACK 09에서 다룬다.

---

## LESSON 14 · graceful shutdown은 새 요청을 끊고 진행 중인 일을 정리하는 절차다

### 1. process 종료도 request lifecycle의 일부다

배포, 재시작, machine shutdown 때 process는 종료된다. 그냥 즉시 끊으면 처리 중이던 request나 DB write가 중간에 사라질 수 있다.

### 2. 종료 순서를 그린다

```text
종료 signal 수신
→ 새 request 받지 않기
→ 진행 중 request가 끝날 시간을 주기
→ DB/queue/client connection 정리
→ 제한 시간 안에 종료
```

무한정 기다리면 deployment가 멈출 수 있으므로 deadline도 필요하다.

### 3. Node 예시를 읽는다

```ts
process.on("SIGTERM", () => {
  server.close((err) => {
    if (err) {
      process.exitCode = 1;
      return;
    }
    process.exitCode = 0;
  });
});
```

이 코드는 출발점일 뿐이다. keep-alive connection, open WebSocket, DB pool, background job 등을 사용하는 실제 앱은 각 resource의 shutdown contract를 확인해야 한다.

### 4. readiness와 연결한다

load balancer가 계속 traffic을 보내는 동안 server부터 닫으면 사용자 오류가 늘 수 있다. production에서는 traffic routing과 shutdown 순서가 연결된다. 자세한 health/readiness는 BLOCK 06에서 다룬다.

### 5. 직접 실습

3초 걸리는 test route를 만들고 request 처리 중 SIGTERM을 보냈을 때 실제로 응답이 끝나는지 관찰한다. framework/server 설정에 따라 결과가 다를 수 있으므로 문서와 실제 실행을 함께 본다.

### 6. 작은 문제

`process.exit(0)`을 signal handler 첫 줄에서 호출하는 코드는 왜 graceful하지 않은가?

process가 즉시 종료되면 pending request와 buffered log, connection cleanup이 수행될 기회를 잃을 수 있다.

---

## LESSON 15 · BLOCK 01 실전: 메모 API 한 개를 request pipeline으로 완성한다

### 1. 목표

DB 없이 memory array를 사용해 다음 API를 만든다.

```text
POST /notes
GET /notes/:id
```

아직 저장 기술을 배우려는 것이 아니다. request가 들어와 response까지 가는 흐름을 정확히 만드는 것이 목표다.

### 2. 요구사항

`POST /notes` body:

```json
{
  "title": "오늘 할 일",
  "content": "백엔드 공부"
}
```

규칙:

```text
title: 1~100자
content: 0~5000자
알 수 없는 field는 정책에 따라 거부하거나 무시하되 한 방식으로 고정
response에는 내부 field를 그대로 내보내지 않음
request id를 log와 response header에 넣음
validation 실패는 4xx
예상 못한 오류는 공통 error handler가 500으로 처리
```

### 3. 흐름을 코드보다 먼저 쓴다

```text
request 도착
→ request id 생성
→ JSON parsing + size limit
→ route 선택
→ body validation
→ input normalization
→ handler
→ application 함수
→ memory 저장
→ response DTO 변환
→ JSON serialization
→ response 완료
```

이 그림을 먼저 만들고 코드를 그 뒤에 맞춘다.

### 4. 일부러 실패 케이스를 만든다

최소 다음을 실제로 보낸다.

```text
잘못된 JSON
body 없음
제목 빈 문자열
제목 101자
존재하지 않는 note id
숫자가 아닌 id
handler 내부 강제 throw
```

각 경우에 **어느 단계에서 실패하는지** 표로 기록한다.

### 5. 채점 기준

- request/response 기본 흐름을 설명할 수 있다.
- route와 handler를 구분한다.
- path/query/header/body의 역할을 구분한다.
- parsing과 validation을 구분한다.
- middleware 순서가 결과를 바꿀 수 있음을 설명한다.
- TypeScript type assertion이 runtime validation이 아님을 설명한다.
- 내부 object와 response DTO를 분리한다.
- async 실패가 어디로 전달되는지 확인한다.
- request context가 필요한 이유를 설명한다.
- config와 secret을 구분한다.
- graceful shutdown을 단순 `process.exit`와 구분한다.

### 6. 왜 틀렸는지 설명하는 방식

정답만 `400`이라고 외우지 않는다. 예를 들어 제목이 101자인 경우:

```text
JSON parsing은 성공
→ route도 찾음
→ schema validation에서 길이 규칙 실패
→ business logic은 실행하지 않음
→ client가 고칠 수 있는 입력 문제이므로 4xx 계열로 표현
```

처럼 **실패 위치와 다음 행동**까지 말할 수 있어야 한다.

### 7. AI에게 맡길 것과 사람이 확인할 것

AI에게 요청해도 되는 일:

```text
Express boilerplate 만들기
schema library 문법 작성
테스트 request 예시 생성
반복적인 DTO type 선언
```

사람이 직접 확인해야 하는 일:

```text
어떤 값을 신뢰해도 되는가
어디서 validation할 것인가
어떤 field를 밖으로 내보낼 것인가
오류가 어느 계층의 실패인가
middleware 순서가 왜 이 순서인가
종료 중 어떤 작업을 보존해야 하는가
```

BLOCK 01의 최종 목적은 framework 문법을 외우는 것이 아니다. **요청 한 건의 실제 경로를 머릿속에서 추적하고, 문제가 생기면 그 경로를 따라 원인을 좁히는 능력**이다.
