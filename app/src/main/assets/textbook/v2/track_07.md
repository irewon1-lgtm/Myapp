# TRACK 07 · 서버와 백엔드

TRACK 06에서는 클라이언트가 HTTP 요청을 보내고 서버가 응답한다는 구조를 배웠다.

이번 TRACK에서는 그 서버 안으로 들어간다.

```text
요청 도착
↓
경로 선택
↓
인증·권한
↓
입력 검증
↓
비즈니스 로직
↓
DB·외부 API
↓
응답
↓
로그·메트릭
```

목표는 서버 프레임워크 문법을 외우는 것이 아니다.

> 요청 하나가 어디를 지나고, 어디서 실패하며, 재시도·중복·동시 요청이 생기면 어떤 문제가 생기는지 설명할 수 있게 만드는 것

이다.

---

## BLOCK 01 · 백엔드란 무엇인가

### LESSON 01 · 사용자가 직접 보지 않는 서버 쪽 기능

앱과 웹서비스에서 사용자 화면 뒤에서 데이터를 저장하고 규칙을 실행하고 다른 시스템과 통신하는 부분을 **백엔드(backend)**라고 부른다.

예:

```text
로그인 확인
주문 저장
결제 요청
데이터베이스 조회
알림 전송
```

### LESSON 02 · 프론트엔드와 백엔드

```text
프론트엔드 = 사용자가 직접 보는 화면과 상호작용
백엔드 = 서버에서 데이터와 규칙을 처리하는 부분
```

경계는 프로젝트에 따라 다르지만 역할을 나누면 구조를 이해하기 쉽다.

---

## BLOCK 02 · 서버 프로그램

### LESSON 01 · 요청을 기다린다

서버 프로그램은 네트워크 포트에서 요청이 들어오기를 기다린다.

요청이 오면 내용을 읽고 필요한 작업을 수행한 뒤 응답한다.

### LESSON 02 · process

실행 중인 서버 프로그램 하나를 **프로세스(process)**라고 부를 수 있다.

운영체제는 프로세스에 메모리와 CPU 시간을 배분한다.

---

## BLOCK 03 · socket 맛보기

### LESSON 01 · 네트워크 통신 끝점

프로그램이 IP 주소와 port를 사용해 네트워크 데이터를 주고받는 끝점을 **소켓(socket)**이라고 부른다.

서버는 특정 port에 socket을 열어 연결을 기다릴 수 있다.

### LESSON 02 · 프레임워크가 많은 세부를 숨긴다

Express, FastAPI, Spring 같은 서버 프레임워크를 사용하면 개발자가 매번 저수준 socket 코드를 직접 작성하지 않아도 된다.

하지만 아래에서 연결이 존재한다는 사실을 알면 오류를 더 잘 이해할 수 있다.

---

## BLOCK 04 · Node.js로 가장 작은 서버

### LESSON 01 · HTTP server

```javascript
import http from "node:http";

const server = http.createServer((req, res) => {
  res.statusCode = 200;
  res.end("hello");
});

server.listen(3000);
```

사람말:

```text
HTTP 서버를 만든다.
요청이 오면 200 응답과 hello를 보낸다.
3000번 port에서 기다린다.
```

### LESSON 02 · localhost

자기 컴퓨터를 가리키는 호스트 이름으로 `localhost`를 많이 사용한다.

```text
http://localhost:3000
```

은 내 컴퓨터의 3000번 port에 연결하려는 주소다.

---

## BLOCK 05 · framework

### LESSON 01 · 반복되는 서버 작업을 편하게

서버를 만들 때 요청 파싱, routing, middleware 같은 공통 기능을 제공하는 도구를 **프레임워크(framework)**라고 부른다.

예:

```text
Express
FastAPI
Django
Spring Boot
NestJS
```

### LESSON 02 · library와 framework 차이

둘의 경계는 완전히 딱 잘라지지 않지만:

```text
library = 내가 필요할 때 기능을 호출
framework = 전체 프로그램 구조와 실행 흐름을 어느 정도 정함
```

이라고 처음 이해할 수 있다.

---

## BLOCK 06 · route

### LESSON 01 · URL과 method에 따라 코드 선택

서버가:

```text
GET /users
POST /users
GET /orders
```

를 서로 다른 함수로 처리해야 한다.

이 요청 경로 규칙을 **route**라고 부른다.

### LESSON 02 · router

여러 route를 묶어 관리하는 구성 요소를 **router**라고 부른다.

```text
/users 관련 router
/orders 관련 router
/admin 관련 router
```

처럼 나눌 수 있다.

---

## BLOCK 07 · route parameter

### LESSON 01 · URL 안의 값

```text
GET /users/123
```

여기서 `123`을 사용자 ID로 사용할 수 있다.

```text
/users/:id
```

처럼 바뀌는 URL 부분을 **route parameter**라고 부른다.

### LESSON 02 · 문자열 그대로 믿지 않는다

URL에서 온 값도 사용자 입력이다.

```text
id가 숫자인가?
허용 범위인가?
현재 사용자가 볼 수 있는 id인가?
```

를 검사해야 한다.

---

## BLOCK 08 · request body parsing

### LESSON 01 · body는 그냥 객체가 아니다

네트워크로 온 body는 바이트 데이터다.

`Content-Type: application/json`이라면 서버 프레임워크가 JSON을 파싱해 객체로 바꿔 줄 수 있다.

### LESSON 02 · parsing

문자열이나 바이트 데이터를 정해진 문법에 따라 읽어 내부 데이터 구조로 바꾸는 과정을 **파싱(parsing)**이라고 부른다.

잘못된 JSON이면 parsing 단계에서 실패할 수 있다.

---

## BLOCK 09 · middleware

### LESSON 01 · route 앞뒤에서 공통 처리

여러 요청에 공통으로 필요한 처리가 있다.

```text
로그 기록
인증 확인
JSON parsing
CORS header
오류 처리
```

요청과 응답 사이에 이런 공통 처리를 연결하는 함수를 **middleware**라고 부른다.

### LESSON 02 · 순서가 중요하다

```text
로그 middleware
↓
인증 middleware
↓
route handler
```

와:

```text
route handler
↓
인증 middleware
```

는 결과가 다르다.

권한 확인이 route 뒤에 있으면 이미 위험한 작업이 실행됐을 수 있다.

---

## BLOCK 10 · handler

### LESSON 01 · 실제 요청을 처리하는 함수

특정 route 요청을 받아 응답을 만드는 함수를 **handler**라고 부른다.

```javascript
app.get("/hello", (req, res) => {
  res.json({ message: "hello" });
});
```

여기서 callback 함수가 handler다.

### LESSON 02 · handler에 모든 코드를 넣지 않는다

DB 조회, 이메일 전송, 결제 로직을 route handler 하나에 모두 넣으면 테스트와 수정이 어려워진다.

역할별로 나누는 방법을 뒤에서 배운다.

---

## BLOCK 11 · validation

### LESSON 01 · 입력이 규칙에 맞는지 확인

사용자가 보낸 데이터가 허용된 형태인지 검사하는 것을 **검증(validation)**이라고 부른다.

예:

```text
이메일 형식인가?
비밀번호 길이가 8자 이상인가?
수량이 1 이상인가?
날짜가 실제 날짜인가?
```

### LESSON 02 · 프론트엔드 검증만 믿으면 안 된다

공격자는 브라우저 UI를 거치지 않고 서버 API를 직접 호출할 수 있다.

따라서 서버는 입력을 다시 검증해야 한다.

---

## BLOCK 12 · sanitization

### LESSON 01 · 위험하거나 불필요한 입력 정리

입력 문자열에서 허용하지 않는 부분을 제거하거나 표준 형태로 바꾸는 것을 **정제(sanitization)**라고 부른다.

예:

```text
앞뒤 공백 제거
전화번호 기호 정규화
HTML 일부 제거
```

### LESSON 02 · validation과 sanitization은 다르다

```text
validation = 이 입력을 받아도 되는가?
sanitization = 입력을 어떤 안전한/표준 형태로 바꿀 것인가?
```

문맥에 맞게 둘을 나눈다.

---

## BLOCK 13 · business logic

### LESSON 01 · 서비스의 실제 규칙

`회원은 하루에 쿠폰을 한 번만 받을 수 있다`, `재고보다 많이 주문할 수 없다` 같은 서비스 규칙을 **비즈니스 로직(business logic)**이라고 부른다.

### LESSON 02 · HTTP와 business logic을 분리한다

할인 계산 함수는 HTTP 요청이 없어도 테스트할 수 있어야 한다.

```javascript
function calculateDiscount(total, membership) {
  // 비즈니스 규칙
}
```

이렇게 분리하면 route와 무관하게 테스트하기 쉽다.

---

## BLOCK 14 · service layer

### LESSON 01 · 여러 규칙을 묶는 계층

route handler와 데이터 접근 사이에서 비즈니스 로직을 담당하는 코드 묶음을 **service layer**라고 부르는 설계가 많다.

```text
route
↓
service
↓
repository/DB
```

### LESSON 02 · 이름 자체가 법칙은 아니다

프로젝트마다 service라는 단어를 다른 의미로 쓸 수 있다.

핵심은 `HTTP 처리`, `업무 규칙`, `데이터 접근`의 책임을 적절히 나누는 것이다.

---

## BLOCK 15 · repository

### LESSON 01 · 데이터 저장소 접근을 감싼다

DB query를 한 곳에 모아 상위 코드가 데이터베이스 세부 문법을 덜 알게 만드는 패턴을 **repository**라고 부른다.

```text
userRepository.findById(id)
```

### LESSON 02 · repository가 항상 필요한 것은 아니다

작은 프로젝트에서는 별도 repository 계층이 오히려 복잡도를 늘릴 수 있다.

목적은 책임과 변경 영향을 줄이는 것이지 파일 수를 늘리는 것이 아니다.

---

## BLOCK 16 · authentication

### LESSON 01 · 누군지 확인

서버에서 사용자가 누구인지 확인하는 과정을 **인증(authentication)**이라고 부른다.

로그인 password, session, token 등을 사용할 수 있다.

### LESSON 02 · 인증 실패

인증 정보가 없거나 유효하지 않으면 보호된 route에 접근하지 못하게 해야 한다.

인증을 route 함수 뒤에 실행하면 이미 중요한 코드가 실행됐을 수 있다.

---

## BLOCK 17 · authorization

### LESSON 01 · 무엇을 할 수 있는지 확인

사용자의 신원을 확인한 뒤 어떤 작업을 허용할지 판단하는 것을 **인가(authorization)**라고 부른다.

예:

```text
일반 사용자 → 자신의 주문만 보기
관리자 → 모든 주문 보기
```

### LESSON 02 · ID만 받았다고 권한이 생기지 않는다

```text
GET /orders/999
```

사용자가 999번 주문 ID를 안다고 그 주문을 볼 권한이 있는 것은 아니다.

서버는 `이 주문의 소유자가 현재 사용자와 같은가?`를 확인해야 한다.

---

## BLOCK 18 · RBAC

### LESSON 01 · 역할에 권한 부여

사용자를 `admin`, `editor`, `viewer` 같은 역할로 나누고 역할별 권한을 주는 방식을 **RBAC(Role-Based Access Control)**라고 부른다.

### LESSON 02 · role 문자열 하나만 믿지 않는다

클라이언트가 보낸:

```json
{"role":"admin"}
```

을 그대로 믿으면 안 된다.

서버가 신뢰할 수 있는 인증 정보와 DB 상태를 기준으로 권한을 판단한다.

---

## BLOCK 19 · 상태 있는 서버와 상태 없는 서버

### LESSON 01 · stateful

사용자 session 같은 상태를 서버 메모리에 직접 저장하고 이후 요청에서 이어 사용하는 서버를 **stateful**하게 설계할 수 있다.

### LESSON 02 · stateless

각 요청이 처리에 필요한 정보를 충분히 포함해 특정 서버 메모리 상태에 덜 의존하게 만드는 방식을 **stateless**라고 부른다.

HTTP 자체는 요청 간 상태를 자동으로 기억하지 않는 stateless 프로토콜이다.

### LESSON 03 · 어느 것이 무조건 좋다는 뜻은 아니다

상태를 어디에 둘지에 따라 확장성, 복잡도, 성능이 달라진다.

---

## BLOCK 20 · process와 thread

### LESSON 01 · process

실행 중인 프로그램의 독립된 실행 환경을 **프로세스(process)**라고 부른다.

각 process는 자신의 가상 메모리 공간을 가진다.

### LESSON 02 · thread

process 안에서 코드를 실행하는 흐름을 **스레드(thread)**라고 부른다.

한 process가 여러 thread를 가질 수 있다.

---

## BLOCK 21 · concurrency와 parallelism

### LESSON 01 · concurrency

여러 작업을 겹쳐 진행할 수 있도록 구조화하는 것을 **동시성(concurrency)**이라고 부른다.

한 CPU core에서도 작업을 번갈아 처리하며 동시성을 만들 수 있다.

### LESSON 02 · parallelism

여러 작업을 실제 같은 시간에 서로 다른 core 등에서 실행하는 것을 **병렬성(parallelism)**이라고 부른다.

```text
concurrency = 여러 일을 겹쳐 다루기
parallelism = 여러 일을 실제 동시에 계산
```

---

## BLOCK 22 · event-driven server

### LESSON 01 · Node.js가 많은 연결을 다루는 방식

Node.js는 JavaScript event loop와 비동기 I/O를 이용해 많은 네트워크 연결을 효율적으로 다룬다.

### LESSON 02 · CPU 계산이 길면 문제

한 JavaScript thread에서 아주 긴 CPU 계산을 실행하면 event loop가 다른 요청을 처리하지 못한다.

```text
I/O 많은 서버 → 비동기 event loop에 잘 맞을 수 있음
CPU 무거운 계산 → worker/process 분리가 필요할 수 있음
```

---

## BLOCK 23 · worker

### LESSON 01 · 무거운 일을 다른 실행 단위로

메인 요청 처리에서 오래 걸리는 계산을 별도의 process나 thread로 보내 처리할 수 있다.

이렇게 일을 수행하는 별도 실행 단위를 **worker**라고 부른다.

### LESSON 02 · 결과를 기다리는 방법

worker와 main server는 message queue, shared memory, IPC 같은 방식으로 통신할 수 있다.

초보 단계에서는 `무거운 일을 메인 요청 처리에서 분리한다`는 구조를 잡는다.

---

## BLOCK 24 · background job

### LESSON 01 · HTTP 응답 뒤에 처리해도 되는 일

회원가입 후 환영 이메일을 보내는 데 3초가 걸린다고 하자.

사용자가 이메일 전송까지 기다릴 필요가 없다면:

```text
회원 DB 저장
↓
응답 201
↓
이메일 job을 뒤에서 처리
```

할 수 있다.

### LESSON 02 · background job

사용자 요청과 분리해 뒤에서 처리하는 작업을 **background job**이라고 부른다.

---

## BLOCK 25 · queue

### LESSON 01 · 처리할 일을 줄 세운다

아직 처리되지 않은 job을 저장해 worker가 하나씩 가져가게 하는 구조를 **작업 큐(job queue)**라고 부른다.

```text
[이메일1][이메일2][PDF생성][알림]
       ↓ worker
```

### LESSON 02 · queue가 주는 장점

```text
순간적인 요청 폭주 완충
실패 job 재시도
worker 수 조절
HTTP 응답과 긴 작업 분리
```

등이 가능하다.

---

## BLOCK 26 · retry

### LESSON 01 · 일시적 실패 다시 시도

외부 API가 잠깐 503을 반환했다면 잠시 후 성공할 수 있다.

실패한 작업을 다시 실행하는 것을 **retry**라고 부른다.

### LESSON 02 · 모든 오류를 retry하면 안 된다

```text
503 일시 장애 → retry 후보
400 잘못된 입력 → 같은 입력으로 retry해도 계속 실패
```

오류 종류를 구분한다.

---

## BLOCK 27 · exponential backoff

### LESSON 01 · 재시도 간격을 늘린다

```text
1초 후
2초 후
4초 후
8초 후
```

처럼 실패할수록 기다리는 시간을 늘리는 전략을 **exponential backoff**라고 부른다.

### LESSON 02 · jitter

수천 client가 정확히 같은 시각에 재시도하면 다시 서버를 공격할 수 있다.

재시도 시간에 약간의 무작위 차이를 넣는 것을 **jitter**라고 부른다.

---

## BLOCK 28 · idempotency

### LESSON 01 · retry가 결제를 두 번 만들면

결제 요청 후 응답을 받기 전에 네트워크가 끊겼다.

클라이언트가 재시도했다.

첫 결제가 이미 성공했다면 두 번 결제될 수 있다.

### LESSON 02 · idempotency key

클라이언트가 요청마다 고유한 **idempotency key**를 보내고 서버가 이미 처리한 key를 기억하면 중복 실행을 막을 수 있다.

```text
key abc123 처음 → 결제 수행
key abc123 재요청 → 기존 결과 반환
```

---

## BLOCK 29 · timeout

### LESSON 01 · 다른 시스템을 영원히 기다리지 않는다

서버가 외부 결제 API를 호출했는데 아무 응답이 없다고 하자.

무한히 기다리면 thread, connection 같은 자원이 계속 점유될 수 있다.

그래서 **timeout**을 설정한다.

### LESSON 02 · 계층마다 timeout이 있다

```text
client timeout
load balancer timeout
server timeout
DB timeout
외부 API timeout
```

서로 다른 timeout이 존재할 수 있다.

---

## BLOCK 30 · circuit breaker

### LESSON 01 · 계속 실패하는 외부 시스템을 잠시 부르지 않는다

외부 결제 서버가 완전히 장애인데 요청마다 계속 호출하면 내 서버도 자원을 낭비한다.

일정 실패율을 넘으면 잠시 호출을 막는 패턴을 **circuit breaker**라고 부른다.

### LESSON 02 · 상태

대표적으로:

```text
closed = 정상 호출
open = 잠시 호출 차단
half-open = 일부 요청으로 회복 시험
```

상태를 사용한다.

---

## BLOCK 31 · cache

### LESSON 01 · 자주 읽는 값을 빠른 곳에

DB 조회 결과나 외부 API 결과를 임시로 저장해 다음 요청에서 빠르게 돌려주는 것을 **cache**라고 부른다.

### LESSON 02 · cache key

어떤 값을 찾을지 구분하는 이름을 **cache key**라고 부른다.

```text
user:123
product:77
```

### LESSON 03 · stale data

DB는 바뀌었는데 cache가 예전 값을 가지고 있으면 **stale data(오래된 데이터)**를 보여 줄 수 있다.

---

## BLOCK 32 · cache invalidation

### LESSON 01 · 언제 버릴 것인가

cache에 저장된 값이 더 이상 유효하지 않다고 판단해 삭제하거나 갱신하는 것을 **cache invalidation**이라고 부른다.

### LESSON 02 · TTL

일정 시간이 지나면 자동 만료하도록 하는 시간을 **TTL(Time To Live)**이라고 부른다.

```text
user:123 TTL = 60초
```

60초 뒤 cache에서 새로 읽도록 할 수 있다.

---

## BLOCK 33 · rate limiting

### LESSON 01 · 너무 많은 요청 제한

사용자 한 명이 1초에 10만 번 로그인 API를 호출하면 서버에 부담이 된다.

일정 시간 동안 허용할 요청 수를 제한하는 것을 **rate limiting**이라고 부른다.

### LESSON 02 · 왜 필요한가

```text
서버 보호
무차별 password 공격 완화
공정한 자원 사용
API 비용 통제
```

등에 사용한다.

---

## BLOCK 34 · file upload server

### LESSON 01 · 파일은 그냥 믿지 않는다

사용자가 `image/png`라고 보내도 실제 파일이 PNG라는 보장은 없다.

서버는:

```text
파일 크기
허용 확장자
MIME type
실제 파일 signature
저장 경로
파일 이름
```

등을 확인해야 한다.

### LESSON 02 · 원래 파일 이름을 그대로 경로로 쓰지 않는다

사용자가:

```text
../../secret.txt
```

같은 이름을 보내 경로를 조작하려 할 수 있다.

서버는 안전한 자체 파일 이름을 만들고 저장 위치를 통제한다.

---

## BLOCK 35 · configuration

### LESSON 01 · 코드와 환경 설정 분리

DB 주소, API URL, 로그 레벨 같은 값을 코드 밖 설정으로 관리할 수 있다.

이런 실행 조건을 **configuration(설정)**이라고 부른다.

### LESSON 02 · environment variable

운영체제 실행 환경에서 이름과 값으로 제공하는 설정을 **환경변수(environment variable)**라고 부른다.

```text
DATABASE_URL=...
LOG_LEVEL=info
```

---

## BLOCK 36 · secret

### LESSON 01 · 노출되면 안 되는 설정

DB password, API key, private key 같은 값을 **secret**이라고 부른다.

### LESSON 02 · 로그에 남기지 않는다

```text
DATABASE_URL=postgres://user:password@...
```

를 통째로 로그에 출력하면 password가 노출될 수 있다.

민감 부분은 마스킹한다.

---

## BLOCK 37 · logging

### LESSON 01 · 서버 사건 기록

서버가 처리한 요청과 오류를 기록하는 것을 **logging**이라고 부른다.

좋은 로그에는:

```text
timestamp
level
request ID
route
status
latency
error code
```

같은 정보가 도움이 된다.

### LESSON 02 · password는 기록하지 않는다

인증 token, password, 카드 번호 전체 같은 민감정보는 로그에 넣지 않는다.

---

## BLOCK 38 · request ID

### LESSON 01 · 같은 요청 로그 연결하기

동시에 많은 요청이 오면 로그가 섞인다.

요청마다 고유한 **request ID**를 붙이면 같은 요청의 로그를 모을 수 있다.

```text
req=abc start
req=abc db query
req=abc response 200
```

---

## BLOCK 39 · metric

### LESSON 01 · 수치로 시스템 상태 보기

시간에 따라 측정한 시스템 수치를 **메트릭(metric)**이라고 부른다.

예:

```text
초당 요청 수
평균 latency
error rate
CPU 사용률
queue 길이
```

### LESSON 02 · percentile

평균만 보면 느린 일부 사용자를 숨길 수 있다.

`p95 latency`는 요청의 약 95%가 그 시간 이하에 끝났다는 뜻이다.

```text
평균 100ms
p95 900ms
```

이면 일부 요청이 매우 느릴 수 있다.

---

## BLOCK 40 · trace

### LESSON 01 · 요청 하나의 전체 경로

사용자 요청이 여러 서비스와 DB를 지나가는 전체 흐름을 **trace**라고 부를 수 있다.

```text
API server 20ms
→ user DB 30ms
→ payment API 800ms
→ total 870ms
```

### LESSON 02 · span

trace 안의 개별 작업 구간을 **span**이라고 부른다.

어디에서 시간이 많이 걸렸는지 찾을 수 있다.

---

## BLOCK 41 · health check

### LESSON 01 · 서버가 살아 있는지 확인

로드밸런서나 운영 시스템이 서버 상태를 확인할 수 있도록 **health check endpoint**를 만들 수 있다.

```text
GET /health
```

### LESSON 02 · liveness와 readiness

```text
liveness = process가 살아 있는가?
readiness = 지금 실제 요청을 받을 준비가 되었는가?
```

DB 연결 준비 전에는 process가 살아 있어도 readiness는 false일 수 있다.

---

## BLOCK 42 · graceful shutdown

### LESSON 01 · 서버를 바로 죽이면 진행 중 요청이 끊긴다

배포 중 서버 process를 즉시 종료하면 처리 중인 요청이 실패할 수 있다.

### LESSON 02 · graceful shutdown

새 요청을 받지 않고 진행 중 요청을 마친 뒤 종료하는 과정을 **graceful shutdown**이라고 부른다.

queue worker도 현재 job을 안전하게 마치고 종료해야 할 수 있다.

---

## BLOCK 43 · API versioning

### LESSON 01 · API를 바꾸면 기존 앱이 깨질 수 있다

모바일 앱 1.0이:

```json
{"name":"민수"}
```

를 기대하는데 서버가 갑자기:

```json
{"fullName":"민수"}
```

만 보내면 기존 앱이 깨질 수 있다.

### LESSON 02 · versioning

큰 breaking change를 구분하기 위해:

```text
/api/v1/users
/api/v2/users
```

처럼 API version을 운영할 수 있다.

항상 URL versioning만 써야 하는 것은 아니다.

---

## BLOCK 44 · backward compatibility

### LESSON 01 · 오래된 클라이언트도 계속 동작하게

새 서버가 기존 클라이언트와도 동작하는 성질을 **하위 호환성(backward compatibility)**이라고 부른다.

### LESSON 02 · 필드 추가와 삭제

응답에 선택적인 새 필드를 추가하는 것은 기존 client가 무시할 수 있어 비교적 안전할 수 있다.

기존 필드를 갑자기 삭제하거나 타입을 바꾸면 breaking change가 될 수 있다.

---

## BLOCK 45 · cron과 scheduler

### LESSON 01 · 정해진 시간에 작업 실행

매일 새벽 2시에 보고서를 만드는 것처럼 일정에 따라 작업을 시작하는 시스템을 **scheduler**라고 부른다.

Unix 계열에서 시간표 기반 작업에 **cron**이 널리 사용된다.

### LESSON 02 · 중복 실행

서버가 두 대인데 둘 다 같은 cron을 실행하면 같은 보고서가 두 번 생성될 수 있다.

분산 환경에서는 leader election, distributed lock, idempotency 같은 추가 설계가 필요할 수 있다.

---

## BLOCK 46 · transaction boundary 맛보기

### LESSON 01 · 여러 DB 작업을 하나처럼

주문 생성:

```text
재고 감소
주문 저장
결제 기록 저장
```

중간에서 실패하면 일부만 반영되어 데이터가 어긋날 수 있다.

DB의 **transaction**을 사용해 여러 변경을 하나의 작업 단위로 처리할 수 있다.

TRACK 08에서 깊게 배운다.

---

## BLOCK 47 · N+1 문제 맛보기

### LESSON 01 · 목록 하나 때문에 DB를 너무 많이 호출

주문 100개를 조회한 뒤 각 주문마다 사용자 정보를 따로 DB에서 조회한다고 하자.

```text
주문 목록 1번 query
사용자 query 100번
총 101번
```

이런 패턴을 **N+1 query problem**이라고 부른다.

### LESSON 02 · 해결 방향

JOIN, batch query, eager loading 등을 이용해 query 수를 줄일 수 있다.

정확한 DB 원리는 TRACK 08에서 배운다.

---

## BLOCK 48 · serverless 맛보기

### LESSON 01 · 서버를 직접 계속 운영하지 않는 실행 모델

클라우드가 요청이나 이벤트가 있을 때 함수를 실행하고 인프라 관리를 많이 대신해 주는 모델을 **serverless**라고 부른다.

### LESSON 02 · 서버가 없다는 뜻은 아니다

실제로 코드는 서버에서 실행된다.

개발자가 서버 운영을 직접 덜 관리한다는 뜻에 가깝다.

cold start, 실행 시간 제한, 비용 모델 같은 새로운 고려사항이 있다.

---

## BLOCK 49 · monolith와 microservice

### LESSON 01 · monolith

여러 기능을 하나의 배포 가능한 애플리케이션 안에 두는 구조를 **monolith**라고 부른다.

처음에는 개발·테스트·배포가 단순할 수 있다.

### LESSON 02 · microservice

기능을 독립적으로 배포 가능한 여러 서비스로 나누는 구조를 **microservices**라고 부른다.

각 서비스가 독립적으로 확장될 수 있지만 네트워크, 배포, 데이터 일관성 문제가 더 복잡해진다.

### LESSON 03 · 작은 팀이 무조건 microservice를 써야 하는 것은 아니다

시스템 구조는 팀 규모와 변경 필요에 맞춰 선택한다.

유행하는 구조가 항상 정답은 아니다.

---

## BLOCK 50 · 핵심 용어 사전

| 용어 | 아주 쉬운 뜻 |
|---|---|
| backend | 화면 뒤에서 데이터와 규칙을 처리하는 서버 쪽 기능 |
| process | 실행 중인 프로그램의 독립된 실행 환경 |
| socket | 네트워크 통신을 주고받는 프로그램 끝점 |
| framework | 프로그램 구조와 반복 기능을 제공하는 개발 틀 |
| route | method와 path에 따라 요청 처리 코드를 연결하는 규칙 |
| middleware | 요청 처리 전후에 공통 동작을 연결하는 함수 |
| handler | 특정 요청을 실제로 처리하는 함수 |
| validation | 입력이 허용 규칙에 맞는지 검사하는 것 |
| sanitization | 입력을 안전하고 표준적인 형태로 정리하는 것 |
| business logic | 서비스가 지켜야 할 실제 업무 규칙 |
| service layer | 비즈니스 규칙을 담당하도록 나눈 코드 계층 |
| repository | 데이터 저장소 접근을 감싸는 코드 패턴 |
| authentication | 사용자가 누구인지 확인하는 것 |
| authorization | 사용자가 무엇을 할 수 있는지 판단하는 것 |
| concurrency | 여러 작업을 겹쳐 진행할 수 있게 구조화하는 것 |
| parallelism | 여러 작업을 실제 동시에 계산하는 것 |
| background job | 사용자 요청과 분리해 뒤에서 처리하는 작업 |
| queue | 처리할 job을 순서대로 보관하는 대기 구조 |
| retry | 실패한 작업을 다시 시도하는 것 |
| exponential backoff | 재시도할수록 기다리는 시간을 늘리는 전략 |
| idempotency | 같은 요청을 반복해도 최종 효과가 중복되지 않는 성질 |
| circuit breaker | 계속 실패하는 외부 시스템 호출을 잠시 차단하는 패턴 |
| cache | 자주 쓰는 결과를 빠른 곳에 임시 저장하는 것 |
| TTL | cache 항목이 살아 있을 시간 |
| rate limiting | 일정 시간의 요청 수를 제한하는 것 |
| request ID | 요청 하나의 로그를 연결하는 식별값 |
| metric | 시스템 상태를 시간에 따라 측정한 수치 |
| trace | 요청 하나가 시스템을 지나간 전체 경로 기록 |
| graceful shutdown | 진행 중 작업을 안전하게 마치고 서버를 종료하는 과정 |
| backward compatibility | 새 버전이 오래된 사용 방법과도 동작하는 성질 |
| scheduler | 정해진 시간이나 조건에 작업을 실행하는 시스템 |
| N+1 | 목록 N개 때문에 추가 query를 N번 실행하는 성능 문제 |
| serverless | 인프라 운영을 클라우드에 많이 맡기는 이벤트 기반 실행 모델 |
| monolith | 여러 기능을 하나의 배포 단위에 둔 구조 |
| microservice | 기능을 독립 서비스 여러 개로 나눈 구조 |

---

## BLOCK 51 · TRACK 07 완료 기준

다음을 설명하고 직접 구현할 수 있어야 한다.

- 서버가 port에서 요청을 기다리는 구조를 설명한다.
- route와 middleware, handler를 구분한다.
- body parsing과 validation을 구분한다.
- 프론트엔드 validation만 믿으면 안 되는 이유를 설명한다.
- business logic을 HTTP 코드와 분리한다.
- authentication과 authorization을 서버에서 모두 적용한다.
- process/thread/concurrency/parallelism을 구분한다.
- background job과 queue를 사용해야 하는 상황을 찾는다.
- retry 대상 오류와 retry하면 안 되는 오류를 구분한다.
- idempotency key로 중복 결제를 막는 구조를 설명한다.
- timeout, backoff, circuit breaker의 역할을 구분한다.
- cache와 invalidation 문제를 설명한다.
- rate limit을 적용한다.
- request ID, metric, trace로 장애를 추적한다.
- graceful shutdown이 왜 필요한지 설명한다.
- API breaking change와 backward compatibility를 판단한다.

### TRACK 프로젝트 · 주문 API 서버

다음 기능을 가진 서버를 만든다.

```text
POST /orders       주문 생성
GET /orders/:id    주문 조회
POST /orders/:id/pay 결제
POST /orders/:id/cancel 취소
```

필수 조건:

1. request body를 schema로 validation한다.
2. 로그인 사용자의 주문만 조회하도록 authorization한다.
3. 주문 계산 로직을 service 함수로 분리한다.
4. DB 접근을 별도 module로 분리한다.
5. 결제 요청에는 idempotency key를 사용한다.
6. 외부 결제 API에 timeout을 설정한다.
7. 일시적 503에는 backoff retry를 적용한다.
8. 실패가 계속되면 circuit breaker 동작을 실험한다.
9. 주문 완료 후 이메일을 queue background job으로 보낸다.
10. 모든 요청에 request ID를 넣고 latency를 로그로 기록한다.
11. `/health`에서 readiness를 확인한다.
12. 서버 종료 시 새 요청을 막고 진행 중 작업을 마치는 graceful shutdown을 구현한다.

마지막에는 HTTP 한 요청이 **route → auth → validation → service → DB/외부 API → response**를 어떻게 지나가는지 그림으로 설명한다.
