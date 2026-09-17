# PART 03 · BLOCK 01 · LESSON 03 · route와 handler를 요청 분배 계약으로 설계하기

요청이 server process에 도착해도 모든 request를 같은 코드가 처리하지는 않는다. method와 path를 기준으로 알맞은 operation을 선택하고, 선택된 handler가 application 기능을 호출한다. routing은 `URL 문자열을 if문으로 비교하는 기술`보다 넓다. **어떤 외부 요청이 어떤 코드 책임으로 연결되는지 공개 계약을 만드는 일**이며, 잘못된 ordering·decoding·fallback은 기능 오류와 보안 문제를 동시에 만들 수 있다.

---

## CHAPTER 01 · route는 method와 path pattern을 처리 코드에 연결한다

가장 작은 server는 `if (method === "GET" && url === "/users")`처럼 직접 분기할 수 있다. route가 몇 개일 때는 충분하지만 수십 개가 되면 path parameter, query, middleware, authorization, documentation을 일관되게 관리하기 어렵다. router는 이런 매칭 규칙을 한 구조에 모으고 요청마다 적합한 handler를 선택한다.

```text
GET    /users        → listUsers
POST   /users        → createUser
GET    /users/:id    → getUser
DELETE /users/:id    → deleteUser
```

같은 path라도 method가 다르면 다른 operation이다. 반대로 query가 다르다고 반드시 다른 route가 되는 것은 아니다. `GET /users?active=true`는 보통 같은 `/users` route가 query 조건을 읽는다. 이 구분은 OpenAPI operation과 access control을 설계할 때 그대로 쓰인다.

작은 router를 직접 배열로 만들어 method와 pattern을 등록하고, 세 요청을 어떤 handler에 보낼지 출력해 본다. framework가 해 주는 일을 한 번 손으로 축소 구현하면 `route registration`과 `handler execution`을 분리해서 읽을 수 있다.

---

## CHAPTER 02 · handler는 transport 입력을 application 호출로 번역하는 가장자리 코드다

handler가 request를 받으면 path/query/body를 읽고 validation을 통과한 값을 application service에 넘긴 뒤 결과를 HTTP response로 변환한다. 모든 business rule과 DB query를 handler에 넣어도 처음에는 동작하지만 transport detail과 핵심 규칙이 한 함수에 섞인다. 이후 CLI, queue worker, 다른 protocol에서 같은 기능을 사용하기 어려워진다.

```ts
async function getUserHandler(req: Request, res: Response) {
  const id = parseUserId(req.params.id);
  const user = await getUser.execute({ id });
  res.json(toUserResponse(user));
}
```

이 구조에서 handler는 `req.params`, HTTP status, JSON serialization을 알고, application service는 HTTP object를 몰라도 된다. 에러 translation도 edge에서 담당할 수 있다. 뒤 BLOCK에서 controller/service/repository 경계를 더 깊게 다루지만 routing 단계부터 **handler가 transport adapter라는 방향**을 잡아 두면 구조가 흔들리지 않는다.

실습에서는 handler에서 직접 배열을 검색하던 코드를 `getUser(id)` 함수로 빼고, 함수는 HTTP 없이 test한다. 그다음 response mapping만 handler에 남았는지 확인한다.

---

## CHAPTER 03 · path parameter는 route를 동적으로 만들지만 여전히 외부 입력이다

`/users/:id` 같은 pattern은 `/users/10`, `/users/20`을 하나의 route로 묶는다. framework가 `req.params.id`를 꺼내 주더라도 값이 안전해진 것은 아니다. client는 `/users/abc`, `/users/-1`, 매우 긴 문자열, percent-encoded 값을 보낼 수 있다. parameter는 routing을 통과했다는 사실만 증명하고 domain type을 만족한다는 사실은 증명하지 않는다.

숫자 ID를 기대한다면 문자열을 integer로 parse한 뒤 범위와 format을 검사한다. `Number("1e3")`처럼 언어 변환 함수가 예상보다 넓은 syntax를 허용할 수 있으므로 identifier grammar가 엄격하다면 정규식 또는 schema parser를 사용한다. UUID도 형식 검증과 authorization이 별개다.

실습에서는 `/users/:id`에 `10`, `0010`, `1e2`, `-1`, `abc`, 1만 자 문자열을 보내고 parser 정책을 결정한다. 허용값·거부값을 표로 고정하고, route match 성공 여부와 application ID validation 성공 여부를 따로 기록한다.

---

## CHAPTER 04 · route ordering은 넓은 pattern이 구체적 route를 가로채는 문제를 만든다

router implementation에 따라 등록 순서가 match 우선순위에 영향을 줄 수 있다. `/users/:id`가 `/users/me`보다 먼저 등록되어 `me`를 id로 취급하는 경우가 대표적이다. 일부 router는 specificity를 계산해 해결하지만 framework마다 규칙이 다르므로 문서를 확인한다.

catch-all route `/*`나 optional segment는 더 큰 범위를 잡는다. static file fallback, SPA fallback, API 404 handler의 위치가 잘못되면 실제 API route가 실행되지 않을 수 있다. middleware ordering과 routing ordering은 함께 request pipeline을 만든다.

실습에서 `/files/:name`, `/files/latest`, `/files/*` 세 route를 서로 다른 순서로 등록하고 어떤 handler가 선택되는지 확인한다. framework 결과를 기록한 뒤 unit test로 `latest`가 항상 intended handler를 선택하는지 고정한다. route table 자체가 regression test 대상이 될 수 있다.

---

## CHAPTER 05 · 404와 405는 path 존재 여부와 method 지원 여부를 다르게 표현한다

`GET /users/10` route는 있지만 `PATCH /users/10`을 지원하지 않는 경우와 `/unknown` path 자체가 없는 경우는 의미가 다르다. HTTP 404는 target resource를 찾을 수 없음을 표현하고, 405는 해당 resource가 request method를 지원하지 않음을 표현하는 데 사용할 수 있다. framework default가 항상 원하는 수준으로 구분해 주지는 않는다.

API client는 404에서 ID를 확인하고, 405에서 method나 client version을 확인할 수 있다. monitoring에서도 갑자기 405가 늘면 오래된 client가 제거된 operation을 호출하거나 proxy가 method를 바꾸는 문제를 의심할 수 있다. 다만 security 정책상 resource 존재를 숨기기 위해 권한 없는 경우 404를 선택하는 등 세부 contract는 별도 판단이 필요하다.

실습에서는 route registry에서 path pattern match와 method match를 두 단계로 분리해 `NOT_FOUND`, `METHOD_NOT_ALLOWED`, `MATCHED` 세 결과를 만든다. client가 각각 다른 message를 보여 주게 하고, status 의미가 debugging에 어떤 정보를 주는지 확인한다.

---

## CHAPTER 06 · query는 route 선택보다 조회 조건과 표현 선택에 쓰이는 경우가 많다

`GET /products?category=book&sort=price&limit=20`은 `/products` resource collection을 조회하되 filter·sort·pagination을 요청한다. query 조합마다 route를 새로 만들면 route table이 폭발한다. handler는 허용된 query parameter를 schema로 검증하고 application query object로 변환한다.

unknown query를 조용히 무시할지 400으로 거부할지는 contract다. 조용히 무시하면 typo `limt=20`이 default limit으로 실행되어 client bug가 숨어 있을 수 있다. 반대로 future compatibility를 위해 unknown parameter를 허용해야 하는 상황도 있다. API 소비자와 evolution 전략을 보고 결정한다.

정렬 field를 client 문자열 그대로 SQL에 연결하면 injection이나 허용되지 않은 expensive sort가 생길 수 있다. `createdAt`, `price`처럼 allowlist를 만들고 내부 column으로 매핑한다. routing 이후에도 **외부 문자열을 내부 실행 언어로 직접 연결하지 않는 원칙**이 계속 적용된다.

---

## CHAPTER 07 · URL decoding과 canonicalization은 routing 전후 표현을 일치시켜야 한다

path에는 percent-encoding이 들어갈 수 있다. `%2F`, `%2E`, Unicode encoding처럼 raw bytes와 decoded path가 달라질 때 proxy, router, application이 서로 다른 횟수로 decode하면 동일 request를 다르게 해석할 수 있다. security rule이 raw path를 검사하고 router는 decoded path를 사용하면 우회 가능성이 생길 수 있다.

safe design은 어느 component가 decoding과 normalization을 책임지는지 명확히 하고 같은 canonical representation을 authorization과 routing에서 사용하도록 한다. reverse proxy와 framework의 documented behavior를 확인한다. `decodeURIComponent`를 application 곳곳에서 반복 호출하는 방식은 double decoding 위험을 키운다.

실습에서는 `/files/a%20b`, encoded slash, encoded dot segment를 테스트 환경에서 보내 proxy 없이 runtime이 `req.url`과 route parameter를 어떻게 보여 주는지 기록한다. 공격 payload를 만드는 것이 목표가 아니라 **representation 단계가 하나 이상일 수 있다는 사실을 관찰**하고 validation 위치를 정하는 것이다.

---

## CHAPTER 08 · trailing slash와 case policy는 작은 차이처럼 보여도 cache·client 계약에 영향을 준다

`/users`와 `/users/`, `/Users`를 같은 route로 볼지 다른 route로 볼지는 framework 설정과 API policy에 따라 달라진다. 자동 redirect를 사용할 수도 있지만 POST body가 있는 request에서 redirect method semantics가 바뀌는 오래된 client나 intermediary behavior를 고려해야 할 수 있다. canonical URL을 정해 일관되게 문서화하는 편이 좋다.

case-sensitive path를 쓰면서 client 팀이 case-insensitive라고 가정하면 production에서만 404가 발생할 수 있다. cache key나 signature가 raw URL을 포함하는 경우 canonicalization 차이가 더 큰 문제가 된다. public API는 path naming 규칙을 정하고 lint/test로 지키는 것이 변경 비용을 줄인다.

실습에서 router 설정을 확인하고 `/users`, `/users/`, `/Users` 세 요청 결과를 표로 만든다. 의도한 policy와 실제 framework default가 다르면 명시적 설정 또는 redirect를 적용하고 regression test를 추가한다.

---

## CHAPTER 09 · nested resource path는 관계를 표현하지만 깊이가 곧 좋은 설계는 아니다

`/users/7/orders/10`은 user와 order 관계를 직관적으로 보여 줄 수 있다. 그러나 order ID가 전역적으로 유일하다면 `/orders/10`으로도 충분할 수 있고, 깊은 nesting은 client path 구성과 authorization logic을 복잡하게 만든다. path 구조는 DB foreign key를 그대로 복사하는 것이 아니라 consumer가 resource를 식별하는 방식과 scope를 반영한다.

nested path에서 두 ID의 관계를 반드시 검증해야 한다. `/users/7/orders/10`을 받았는데 order 10의 owner가 user 8이라면 단순 `orderId=10` 조회만 하고 반환해서는 안 된다. parent ID가 authorization scope 또는 resource identity의 일부인지 contract로 정한다.

실습에서는 `/users/:userId/orders/:orderId` handler를 만들고 두 ID가 실제 관계를 이루는지 fake repository로 검증한다. `userId`를 URL 장식으로만 받고 사용하지 않는 anti-pattern을 일부러 만든 뒤 test가 잡도록 한다.

---

## CHAPTER 10 · route metadata는 authorization·documentation·observability를 같은 operation에 묶을 수 있다

규모가 커지면 route마다 required permission, request schema, response schema, operation name, rate limit class 같은 metadata가 필요해진다. 이것이 코드·문서·middleware에 따로 흩어지면 한쪽만 바뀌는 drift가 생긴다. framework가 route schema를 중심으로 validation과 OpenAPI를 생성하도록 지원하는 이유다.

예를 들어 `operationId=createOrder`, `permission=order.create`, `bodySchema=CreateOrder`를 하나의 route declaration 근처에 둔다. observability에서는 raw path `/orders/123` 대신 route template `/orders/:id`를 metric dimension으로 사용한다. route table이 application의 public surface를 설명하는 inventory가 된다.

실습으로 세 route의 method, template, operationId, auth requirement를 배열로 만들고 startup 시 duplicate operationId와 duplicate method+path를 검사한다. metadata가 단지 문서 장식이 아니라 **startup validation과 운영 관측의 입력**이 되는 구조를 확인한다.

---

## CHAPTER 11 · route collision은 새 기능 추가가 기존 handler 선택을 바꾸는 회귀를 만든다

새 route `/reports/latest`를 추가했는데 기존 `/reports/:id`가 먼저 잡아 버리면 code review에서 두 파일만 봐서는 놓치기 쉽다. optional parameter나 wildcard가 많을수록 collision 가능성이 커진다. route framework가 startup에서 ambiguous pattern을 거부하는지 확인하고, 그렇지 않다면 route inventory test를 만들 수 있다.

public API에서 collision은 단순 404보다 위험할 수 있다. 잘못된 handler가 실행되어 다른 authorization policy나 side effect가 적용될 수 있기 때문이다. route 선택 후 permission 검사만 안전하다고 가정하지 말고 **요청이 어느 operation으로 분류되는지 자체**를 검증한다.

실습에서는 기존 route set에 새 candidate를 추가할 때 representative path들을 생성해 예상 operation과 실제 match를 비교한다. 특히 static segment와 parameter, wildcard 경계를 포함한다. 실패한 case를 regression fixture로 남긴다.

---

## CHAPTER 12 · route handler의 입력 type은 runtime validation 이후에 좁혀져야 한다

TypeScript에서 handler signature를 `req: Request<Params, ResBody, CreateUserBody>`처럼 적을 수 있어도 internet payload가 compiler를 통과하는 것은 아니다. generic type은 개발 도구와 compile-time contract를 돕지만 runtime body·params·query를 검증하지 않는다. handler 안에서 `as CreateUserBody`로 단언하면 잘못된 값을 trusted type으로 위장한다.

더 안전한 흐름은 raw input을 `unknown` 취급하고 schema parser가 성공한 결과만 application type으로 전달하는 것이다. route metadata에 schema를 붙이면 handler에 들어오기 전에 validated value를 제공하는 framework도 있다. 이 경우에도 coercion·unknown field·default policy가 기대와 같은지 확인한다.

실습에서 `age:number` type을 선언하고 실제 JSON으로 `{"age":"not-a-number"}`를 보낸다. type annotation만 있을 때 runtime에서 통과하는지 확인한 뒤 schema validation을 추가한다. compiler success와 runtime trust가 다른 층이라는 사실을 직접 증명한다.

---

## CHAPTER 13 · 404 fallback은 모든 정상 route가 끝난 뒤 마지막 분기여야 한다

Express 계열처럼 middleware가 순서대로 실행되는 환경에서는 일반 404 handler를 너무 앞에 등록하면 뒤 route가 절대 도달하지 않는다. static file fallback이나 SPA index fallback도 API prefix보다 먼저 잡으면 `/api/orders`가 HTML 200을 반환하는 기묘한 문제가 생길 수 있다.

fallback response는 content type과 error contract도 맞춰야 한다. API client가 JSON Problem Details를 기대하는데 catch-all이 HTML error page를 반환하면 client parser가 두 번째 오류를 만든다. proxy layer의 404와 application router의 404가 서로 다른 body를 만들 수도 있다.

실습에서 404 middleware를 앞뒤로 이동해 `/users` response가 어떻게 달라지는지 확인한다. 그다음 `/api/*`와 web page fallback을 분리해 API는 JSON, page는 HTML을 반환하게 한다. response content type을 test assertion에 포함한다.

---

## CHAPTER 14 · route table은 보안과 운영을 위한 inventory로도 사용된다

운영 중인 endpoint를 모르면 deprecated API를 제거하기 어렵고 authorization 누락을 찾기도 어렵다. route inventory에서 method, template, owner, auth policy, operationId를 추출하면 `public인데 의도했는가`, `관리 기능인데 permission이 없는가`, `deprecated인데 traffic이 남았는가`를 검사할 수 있다.

OWASP API 보안 관점에서도 old/unmanaged endpoint는 위험한 surface가 될 수 있다. `/v1/admin`을 UI에서 더 이상 쓰지 않아도 server에 남아 있으면 공격자는 직접 호출할 수 있다. API inventory와 traffic observability를 연결하면 실제 사용 여부를 근거로 deprecation을 진행할 수 있다.

작은 script로 route registry를 JSON으로 출력하고 permission 없는 state-changing route를 탐지한다. 단순히 route 개수를 세는 것이 아니라 **외부에서 호출 가능한 모든 operation에 소유자와 정책이 있는지** 확인한다.

---

## CHAPTER 15 · routing 실습은 정상 path보다 충돌·오류·경계값을 더 많이 보낸다

`users` API에 `GET /users`, `POST /users`, `GET /users/me`, `GET /users/:id`, `DELETE /users/:id`를 만든다. 각 route는 고유 operationId를 갖고, unknown path에는 JSON 404, path는 맞지만 지원하지 않는 method에는 정책에 맞는 405를 반환한다. `:id`는 positive integer만 허용하고 `/users/me`와 충돌하지 않아야 한다.

검증 요청에는 `/users/10`, `/users/me`, `/users/-1`, `/users/abc`, `/Users/10`, `/users/`, encoded path, PATCH method, 완전히 없는 path를 포함한다. 각 case에 예상 operation과 status를 먼저 적은 뒤 실제 server를 호출한다. 예상과 실제가 다르면 framework routing rule을 문서에서 확인하고 code 또는 contract를 수정한다.

AI는 route skeleton과 OpenAPI operation 초안을 빠르게 만들 수 있다. 사람이 직접 확인할 부분은 wildcard/static route collision, ID validation, parent-child scope, method semantics, authorization metadata다. routing이 올바르면 이후 middleware와 service가 **정확히 의도한 operation**에 적용될 기반이 생긴다.
