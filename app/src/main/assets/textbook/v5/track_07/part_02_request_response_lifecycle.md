# PART 02 · BLOCK 01 · LESSON 02 · request와 response의 실제 수명주기 읽기

server process가 port에서 요청을 받을 준비를 했다면 다음 질문은 `요청 한 건이 코드 안에서 무엇으로 보이는가`다. HTTP message는 method·target·headers·body를 갖지만, framework는 이를 request object와 helper API로 바꾸어 보여 준다. response도 단순 문자열 하나가 아니라 status·headers·body·완료 시점을 가진다. 이 PART는 `req`, `res` 문법보다 **어느 시점에 무엇을 읽거나 쓸 수 있고, 이미 끝난 응답을 다시 건드리면 왜 문제가 되는지**를 다룬다.

---

## CHAPTER 01 · request object는 wire message를 runtime이 해석한 결과다

네트워크에서 JavaScript object가 그대로 날아오는 것은 아니다. HTTP parser가 start line과 header를 읽고, connection에서 body bytes를 전달하며, Node.js와 framework가 그 정보를 API 형태로 노출한다. `req.method`, `req.url`, `req.headers`는 이 해석 결과를 보는 창이다. 따라서 request object의 property가 편리하다고 해서 protocol 경계가 사라진 것은 아니다. parser가 허용하는 문법, header normalization, body streaming 방식은 runtime의 계약에 포함된다.

같은 HTTP 정보를 framework마다 다른 이름으로 제공할 수도 있다. Express의 `req.path`, Fastify의 request object, Spring의 request abstraction이 달라도 underlying 의미를 method·target·header·body로 다시 환원하면 새 framework를 읽기 쉬워진다. 반대로 framework helper 이름만 암기하면 raw URL과 decoded parameter가 다를 때, proxy가 header를 추가했을 때, body parser가 실행되기 전 값을 볼 때 혼란이 커진다.

실습에서는 `req.method`, `req.url`, `req.headers.host`, `req.headers["user-agent"]`만 출력하는 작은 server를 만들고 browser와 curl에서 같은 path를 호출한다. 두 client가 만들어 내는 header 차이를 관찰하고, application code가 직접 만들지 않은 정보도 request object에 포함된다는 사실을 확인한다.

---

## CHAPTER 02 · method는 handler가 기대하는 의미와 side effect를 드러낸다

HTTP method는 문자열 한 조각이 아니라 request semantics의 일부다. GET은 안전한 조회 의미를 갖도록 설계되고, PUT·DELETE 같은 method에는 idempotency와 관련한 표준 의미가 있다. 그러나 application이 표준을 어기는 side effect를 몰래 넣는다면 method 이름이 자동으로 안전성을 만들어 주지 않는다. `GET /users/10`이 호출될 때마다 결제를 수행한다면 client와 cache가 기대하는 모델과 충돌한다.

server에서는 method를 routing과 authorization, logging, metrics의 dimension으로 사용한다. 같은 `/orders/10`이라도 GET과 DELETE는 전혀 다른 operation이다. method가 없거나 지원하지 않는 경우를 path missing과 구분하면 API 진단이 쉬워진다. proxy나 browser가 HEAD, OPTIONS 같은 method를 보낼 수도 있으므로 `GET 아니면 모두 POST`처럼 거칠게 분류하면 뜻하지 않은 동작을 만들 수 있다.

직접 GET과 POST를 같은 path에 보내 `req.method`가 달라지는 것을 확인한 뒤, method별 counter를 남긴다. 그다음 GET handler에 상태 변경을 임시로 넣고 두 번 호출했을 때 결과가 달라지는 것을 관찰한다. 이 실험은 protocol label과 실제 application behavior를 일치시키는 책임이 server code에 있음을 보여 준다.

---

## CHAPTER 03 · request target은 raw path·query·decoded 값으로 여러 단계에서 보일 수 있다

`/products/10?sort=price%20desc`처럼 보이는 target은 path와 query를 포함한다. framework가 parsing하면 path parameter, query map, decoded 문자열처럼 더 편한 표현을 제공한다. 이때 raw representation과 decoded representation을 섞으면 같은 입력을 두 번 decode하거나 서로 다른 canonical form으로 검사하는 문제가 생길 수 있다. routing은 어느 representation을 기준으로 매칭하는지 framework 문서를 확인한다.

query string은 단순 object가 아니다. 동일 key가 여러 번 나타날 수 있고, 빈 값과 key 없음이 다를 수 있으며, percent-encoding과 Unicode 해석이 개입한다. `?tag=a&tag=b`를 framework가 string array로 만들지 마지막 값 하나로 만들지는 API에 따라 다르다. 이런 차이는 validation 이전의 parsing 계약이다.

실습에서 `req.url` 원문과 `new URL(req.url, base)`로 얻은 pathname/searchParams를 동시에 출력한다. `?q=a%20b`, `?x=1&x=2`, `?empty=`를 각각 보내 representation 차이를 기록한다. 후속 코드는 raw 문자열을 임의로 split하기보다 검증된 URL parser를 사용하고, validation은 parser가 만든 값의 의미를 검사하도록 경계를 둔다.

---

## CHAPTER 04 · headers는 부가정보지만 신뢰 수준은 header마다 다르다

HTTP header에는 media type, authorization credential, conditional request 정보, cache 지시, proxy가 추가한 forwarding 정보 등 여러 종류가 들어간다. 모두 `req.headers`에 보인다고 같은 신뢰 수준을 갖는 것은 아니다. internet client가 직접 임의 header를 추가할 수 있고, trusted reverse proxy가 덮어쓰는 header도 있다. `X-Forwarded-For`를 그대로 client IP라고 믿으려면 어떤 proxy chain이 그 값을 관리하는지 설정이 필요하다.

header 이름은 HTTP에서 case-insensitive하게 다뤄지지만 runtime은 lower-case key로 normalize하는 등 표현을 바꿀 수 있다. 동일 의미 header의 중복 처리도 종류마다 규칙이 다르다. application은 protocol library가 제공하는 API를 사용하고, security decision에 쓰는 header는 trusted hop과 validation을 명시한다.

실습에서 client가 임의의 `X-Debug-User: admin`을 보내고 server가 쉽게 읽을 수 있음을 확인한다. 그 값을 인증 증거로 사용하면 안 되는 이유를 설명한다. 이어 `Content-Type`, `Accept`, `Authorization`이 각각 payload 해석, 원하는 representation, credential 전달이라는 서로 다른 역할을 가진다는 표를 만든다.

---

## CHAPTER 05 · body는 한 번에 준비된 object가 아니라 byte stream에서 시작한다

Node.js의 낮은 수준 HTTP request body는 stream으로 읽힌다. packet 하나가 JSON object 하나와 일치하지 않고 body가 여러 chunk로 나뉘어 도착할 수 있다. framework의 JSON middleware는 chunk를 모아 size limit을 적용하고 encoding을 해석한 뒤 JSON parser를 호출해 `req.body` 같은 편한 값을 만든다. 그래서 body parser가 없다면 object가 존재하지 않을 수 있고, 너무 큰 payload는 handler 전에 거부될 수 있다.

stream을 직접 다룰 때는 chunk를 받는 속도와 소비 속도, 최대 크기, client abort를 고려해야 한다. 모든 byte를 제한 없이 memory에 모으면 작은 수의 request가 process memory를 소진시킬 수 있다. 반대로 streaming upload는 전체 내용을 memory에 올리지 않고 순차 처리할 수 있지만 partial failure와 cleanup이 복잡해진다.

작은 실습에서는 `data` event에서 누적 byte 수만 세고 `end`에서 결과를 반환한다. 10바이트와 1MB payload를 보내 memory와 시간 차이를 본다. 그 뒤 100KB 상한을 직접 적용해 초과 시 request를 중단한다. application-level validation보다 먼저 **수신할 자원 자체의 budget**이 필요하다는 점을 확인한다.

---

## CHAPTER 06 · response status는 처리 결과의 protocol 표현이지 내부 예외 이름이 아니다

response를 만들 때 server는 status code와 headers, body를 결정한다. 200, 201, 204, 400, 404, 409, 500 같은 숫자는 client가 후속 행동을 결정하는 protocol signal이다. 내부에서 `UserNotFoundError`가 발생했다고 status가 자동 결정되는 것은 아니다. HTTP edge가 application result를 외부 의미로 번역한다.

status code와 body 의미가 모순되면 client가 혼란스러워진다. 200 body에 `{error:"failed"}`를 넣으면서 monitoring은 2xx를 성공으로 세는 구조가 대표적이다. 반대로 client validation 오류를 전부 500으로 보내면 server reliability 지표가 오염되고 client가 retry해야 할지 판단하기 어려워진다. status 선택은 문서·client·observability가 함께 의존하는 API contract다.

실습에서는 동일한 `찾을 수 없음` 결과를 200+error body와 404 response 두 방식으로 만들고 client code가 어떻게 달라지는지 비교한다. 후속 PART에서 error taxonomy와 RFC Problem Details를 배우기 전에, status가 단순 UI message가 아니라 machine-readable control information이라는 감각을 만든다.

---

## CHAPTER 07 · response header는 body를 해석하고 캐시하고 추적하는 규칙을 전달한다

`Content-Type`은 body representation을 알려 주고, `Content-Length`나 transfer framing은 message 경계를 전달하며, cache 관련 header는 재사용 조건을 표현한다. `Location`은 resource 생성이나 redirect와 연결될 수 있고, request correlation ID를 response header로 돌려 support에 활용할 수도 있다. header는 body 바깥의 계약이므로 JSON field와 목적이 다르다.

server가 JSON을 보내면서 `Content-Type: text/plain`을 사용하면 어떤 client는 여전히 우연히 parsing할 수 있지만 표준 contract는 틀어진다. charset이 필요한 text type에서는 encoding도 명시해야 한다. compression을 적용하면 body byte와 length가 바뀌므로 middleware 순서가 header 계산에 영향을 줄 수 있다.

직접 `Content-Type`, custom request ID, `Cache-Control: no-store`를 설정한 response를 만들고 curl의 `-i` 옵션으로 header와 body를 함께 본다. 그다음 `Content-Type`을 고의로 잘못 바꾸어 browser/client library가 어떻게 처리하는지 관찰한다. 정상 동작처럼 보여도 계약이 어긋난 상태를 구분한다.

---

## CHAPTER 08 · response body serialization은 runtime object를 wire representation으로 바꾼다

`res.json(value)` 같은 helper는 JavaScript object를 JSON text/bytes로 serialize한다. 이 변환에서 Date가 문자열로 바뀌고 `undefined` property가 사라질 수 있으며 BigInt처럼 기본 JSON serialization이 실패하는 값도 있다. 따라서 내부 object와 wire response가 동일하다고 생각하면 타입과 정밀도, 공개 field에서 실수가 생긴다.

순환 참조 object는 일반 JSON stringify가 실패한다. class getter나 custom `toJSON`이 예상치 못한 값을 포함할 수도 있다. 대용량 object serialization은 CPU와 memory를 사용하고 Node.js event loop를 오래 점유할 수 있다. response shape는 data exposure와 성능 양쪽에 영향을 준다.

실습에서는 Date, undefined, BigInt를 포함한 object를 각각 serialize해 실제 결과와 error를 기록한다. `passwordHash`가 들어 있는 user object를 그대로 보내는 경우와 명시적 response mapper로 필요한 field만 고르는 경우를 비교한다. serialization은 편의 함수가 아니라 **외부 계약을 만드는 경계**로 취급한다.

---

## CHAPTER 09 · response는 한 번 완료되면 다시 쓸 수 없는 수명주기를 가진다

`res.end()` 또는 framework의 `res.json()`이 response를 완료한 뒤 다른 branch에서 다시 header나 body를 쓰려 하면 `headers already sent` 계열 오류가 발생할 수 있다. 흔한 원인은 validation 실패 response를 보낸 뒤 `return`하지 않아 아래 business code가 계속 실행되는 것이다. response 완료와 JavaScript 함수 종료는 자동으로 같은 사건이 아니다.

```ts
if (!valid) {
  res.statusCode = 400;
  res.end("invalid");
  return;
}
```

이 `return`은 style 취향이 아니라 이후 side effect를 막는 control-flow다. async callback이 늦게 도착해 두 번째 response를 시도하는 race도 있을 수 있다. response ownership을 한 handler path에서 한 번만 완료하도록 구조를 단순하게 만드는 편이 좋다.

실습에서는 고의로 `res.end("first")` 뒤 다시 `res.end("second")`를 호출해 error를 재현한다. 다음에는 validation branch에서 response를 보냈지만 DB fake 함수가 여전히 호출되는 버그를 만든다. `return`을 추가한 뒤 call count가 0이 되는지 검증한다.

---

## CHAPTER 10 · client가 연결을 끊어도 server 작업이 자동 취소되는 것은 아니다

mobile network가 바뀌거나 사용자가 화면을 떠나 client connection이 끊길 수 있다. server는 response를 전달할 수 없게 되었지만 이미 시작한 DB query나 external API call이 계속 실행될 수 있다. 비싼 작업을 무조건 끝까지 수행하면 자원을 낭비하고, 반대로 중간 취소가 business invariant를 깨뜨릴 수도 있다.

취소 가능한 read 작업은 AbortSignal 같은 cancellation mechanism을 dependency까지 전달할 수 있다. 그러나 결제처럼 side effect가 시작된 operation은 client disconnect만 보고 `없던 일`로 취급할 수 없다. 결과를 durable state로 기록하거나 reconciliation해야 한다. **network connection lifecycle과 business transaction lifecycle을 분리**하는 이유다.

실습에서는 5초 delay handler를 만들고 curl을 중간에 종료한다. server log에서 handler가 계속 끝나는지 확인한다. 다음에는 abort event를 감지해 단순 계산 작업은 중단하되, `이미 저장 시작` flag 이후에는 cleanup path를 실행하도록 분기한다. cancellation이 단순 `throw` 하나가 아님을 경험한다.

---

## CHAPTER 11 · keep-alive는 여러 request가 하나의 connection을 재사용하게 한다

HTTP connection을 매 request마다 새로 만들면 TCP와 TLS setup 비용이 반복된다. keep-alive를 사용하면 하나의 connection에서 여러 request/response를 교환할 수 있어 latency와 CPU 비용을 줄일 수 있다. 그러나 오래 살아 있는 connection은 server의 file descriptor와 memory를 점유하므로 idle timeout과 최대 요청 수 같은 정책이 필요할 수 있다.

connection 수와 request 수는 같은 숫자가 아니다. 한 connection이 여러 request를 순차 또는 protocol에 따라 multiplex할 수 있고, 여러 client가 각각 connection을 유지할 수도 있다. observability에서 `connections=100`을 `requests=100`으로 해석하면 load를 잘못 판단한다.

실습에서는 Node client 또는 curl의 connection reuse를 관찰하고 server socket connection event count와 request count를 별도로 기록한다. 같은 client가 여러 번 요청할 때 connection 생성 횟수가 어떻게 달라지는지 본다. 이 구분은 later connection pool과 load balancer timeout을 이해하는 토대가 된다.

---

## CHAPTER 12 · streaming response는 전체 결과를 만들기 전에 일부를 보낼 수 있지만 실패 계약이 달라진다

큰 파일이나 긴 결과를 한 번에 memory에 만든 뒤 보내지 않고 chunk 단위로 response할 수 있다. streaming은 first-byte latency와 memory 사용을 줄일 수 있지만, header와 status가 이미 전송된 뒤 중간 오류가 나면 일반 JSON error response로 되돌리기 어렵다. client도 partial body를 어떻게 처리할지 알아야 한다.

예를 들어 CSV export에서 10만 행 중 5만 행을 보낸 뒤 DB cursor가 실패했다면 이미 200 status와 절반의 파일이 전달됐을 수 있다. transport success와 complete semantic result가 갈라진다. checksum, content framing, resumable download, job-based generation처럼 더 강한 완료 contract가 필요할 수 있다.

작은 실습으로 1초마다 세 chunk를 보내고 두 번째 뒤 고의 오류를 발생시킨다. client가 받은 byte와 server error log를 비교한다. 다음으로 파일을 먼저 완성한 뒤 atomic하게 제공하는 job 방식과 streaming 방식의 latency·memory·partial failure trade-off를 표로 정리한다.

---

## CHAPTER 13 · backpressure는 쓰는 속도가 network가 보내는 속도보다 빠를 때 필요하다

server가 response stream에 데이터를 매우 빠르게 write해도 network와 client가 같은 속도로 소비한다는 보장은 없다. writable stream buffer가 가득 차면 `write()`가 더 이상 무제한으로 밀어 넣지 말라는 신호를 줄 수 있다. 이 신호를 무시하고 데이터를 계속 생성하면 process memory가 증가한다.

backpressure는 network streaming뿐 아니라 queue와 database cursor에서도 같은 구조로 나타난다. producer가 consumer capacity를 모르면 중간 buffer가 무한히 자라거나 latency가 누적된다. 따라서 `빠르게 생성할 수 있음`과 `빠르게 전달할 수 있음`을 구분한다.

실습에서는 많은 chunk를 write하고 return value와 `drain` event를 기록한다. client를 의도적으로 느리게 읽게 만들어 buffer 변화가 보이는지 확인한다. 정확한 threshold는 runtime에 따라 달라지므로 숫자를 암기하지 않고 **producer가 consumer의 수용 가능 신호에 반응한다**는 메커니즘을 이해한다.

---

## CHAPTER 14 · lifecycle instrumentation은 시작·완료·abort를 모두 관측해야 한다

request 시작 시각만 기록하면 끝나지 않은 request를 찾기 어렵고, `finish` event만 세면 client abort가 빠질 수 있다. server는 정상 완료, error, connection close를 구분해 관측할 필요가 있다. duration metric은 어느 event를 종료로 선택했는지에 따라 의미가 달라진다.

structured log에는 route template, method, final status, duration, request ID를 넣을 수 있다. raw URL query에는 개인정보가 포함될 수 있으므로 그대로 metric label로 쓰지 않는다. response가 끝난 뒤 status를 기록해야 실제 결과와 맞고, error handler가 status를 바꾸기 전에 미리 log하면 잘못된 값을 남길 수 있다.

실습에서는 `finish`, `close`, handler catch path에 서로 다른 event를 기록하고 정상 요청, client 중단, handler throw 세 상황을 만든다. 각각 어떤 event 조합이 발생하는지 표로 만든다. 뒤 observability BLOCK에서 metric과 trace를 배우기 전에 한 request의 수명주기를 정확히 측정하는 기반을 만든다.

---

## CHAPTER 15 · 요청 한 건을 bytes에서 완료 event까지 추적하는 실습으로 닫는다

작은 server에 `/echo` route를 만들고 method, raw URL, selected headers, body byte count를 읽은 뒤 명시적 JSON response를 보낸다. body는 64KB 상한을 두고, 초과하면 handler business logic을 실행하지 않는다. response에는 request ID와 처리 duration을 넣고, 정상 완료와 connection close를 구조화해서 기록한다. 테스트용으로 `?fail=1`이면 handler 내부에서 error를 발생시키고 공통 error path에서 500을 보낸다.

검증 케이스는 정상 GET, JSON POST, body limit 초과, malformed body, client 중단, handler throw, response 이중 종료 시도다. 각 케이스에서 `request object에 무엇이 존재했는가`, `어느 시점에 response header가 확정됐는가`, `business code가 실행됐는가`, `finish/close 중 무엇이 발생했는가`를 기록한다. 단순히 화면에 문자열이 보였는지만 확인하지 않는다.

사람이 직접 유지할 판단은 wire message와 runtime object, connection lifecycle과 business lifecycle, response 완료와 함수 종료를 구분하는 일이다. AI는 stream boilerplate나 curl 명령을 만들어 줄 수 있지만, partial response와 abort가 어떤 데이터 invariant를 깨뜨릴 수 있는지는 system contract를 아는 사람이 결정한다. 이 구분이 잡히면 route·middleware·validation을 추가해도 request 흐름을 잃지 않는다.
