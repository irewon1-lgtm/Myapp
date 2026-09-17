# PART 04 · BLOCK 01 · LESSON 04 · path·query·header·body를 역할별 입력 계약으로 나누기

하나의 HTTP request에는 값을 넣을 장소가 여러 곳 있다. path parameter, query parameter, header, body는 모두 client가 server에 보내는 값이지만 목적과 수명, caching·logging·authorization에 미치는 영향이 다르다. `아무 데나 넣어도 서버에서 읽히면 된다`는 방식은 작은 예제에서는 통과해도 API가 커질수록 모호한 계약과 보안 실수를 만든다. 이 PART는 각 입력 위치를 **resource identity, 조회 조건, protocol metadata, payload**라는 역할로 구분하고, 같은 값이 여러 위치에 나타났을 때 무엇을 신뢰해야 하는지까지 다룬다.

---

## CHAPTER 01 · path parameter는 어느 resource를 다루는지 식별하는 데 사용한다

`GET /users/42`에서 `42`는 URL 경로의 일부이며 보통 어느 user resource를 조회할지 식별한다. route pattern이 `/users/:id`라면 router가 raw path를 매칭한 뒤 `id` 값을 handler에 제공한다. 이 값은 UI가 만든 것처럼 보여도 internet client가 임의로 바꿀 수 있으므로 항상 외부 입력으로 취급한다. 숫자 identifier라면 parse와 range 검증이 필요하고, UUID처럼 형식이 있는 값도 syntax validation이 필요하다.

path는 resource identity를 표현하는 데 강하지만 모든 필터를 path segment로 만들면 계층이 과도하게 깊어진다. `/stores/1/categories/2/products/3/reviews/4`처럼 nested path가 실제 ownership과 scope를 명확히 한다면 가치가 있지만 DB foreign-key 구조를 그대로 복사하기 위해 깊게 만들 필요는 없다. consumer가 어떤 resource를 고유하게 식별하는지와 authorization boundary가 기준이다.

실습으로 `/orders/:id`에 `1`, `001`, `-1`, `abc`, 지나치게 긴 문자열을 보낸다. route가 match됐는지와 `OrderId` parser가 통과했는지를 따로 기록한다. routing 성공이 domain identifier 유효성을 보장하지 않는다는 점을 실제 결과로 확인한다.

---

## CHAPTER 02 · query parameter는 조회·정렬·pagination 같은 선택 조건에 적합하다

`GET /products?category=book&sort=price&limit=20`에서 query는 collection을 어떤 조건으로 볼지 지정한다. resource identity 자체보다 filter, sort, page size, search text처럼 선택적인 조회 조건에 자주 쓰인다. 같은 `/products` route 안에서 query 조합이 달라지므로 route 수를 늘리지 않고 다양한 representation을 요청할 수 있다.

query 값은 URL에 포함되기 때문에 browser history, access log, proxy log, analytics에 남을 수 있다. password나 access token처럼 민감한 값을 query에 넣으면 의도치 않은 저장 지점이 늘어난다. cache가 URL 전체를 key로 사용할 수 있어 query ordering과 canonicalization도 cache behavior에 영향을 줄 수 있다. 따라서 입력 위치 선택은 단지 handler에서 읽기 편한지의 문제가 아니다.

실습에서는 `limit`을 query에서 읽어 1~100 정수만 허용한다. `limit=10`, `limit=0`, `limit=101`, `limit=abc`, query 없음을 보내고 default 적용과 validation 실패를 구분한다. `Number()`의 coercion 범위까지 확인해 API contract가 원하는 syntax보다 넓게 허용하지 않는지 검토한다.

---

## CHAPTER 03 · header는 request 자체의 protocol metadata와 cross-cutting 정보를 전달한다

header는 body payload와 별개로 media type, authorization credential, conditional request, tracing context, content negotiation 같은 요청 특성을 전달한다. `Content-Type`은 body representation을 설명하고 `Accept`는 client가 원하는 response representation을 알리며 `Authorization`은 credential을 전달한다. 같은 header 영역에 있다고 해서 application 의미가 같지는 않다.

custom header를 사용하면 proxy와 CORS preflight, cache key, observability에 영향을 줄 수 있다. browser 환경에서는 임의 header가 cross-origin request를 단순 request가 아닌 preflight 대상으로 바꿀 수도 있다. API gateway가 특정 header를 제거하거나 추가할 수도 있다. 중요한 security decision에 쓰는 header는 누가 설정할 수 있는지 trust boundary를 명확히 해야 한다.

직접 `X-Tenant-Id`, `X-Request-Id`, `Authorization`을 client에서 임의로 바꿔 보낸다. server가 읽을 수 있다는 사실만으로 값이 검증된 identity가 되지 않는다는 것을 확인한다. 이후 authenticated principal과 client-declared metadata를 분리하는 설계로 연결한다.

---

## CHAPTER 04 · body는 생성·변경할 구조화 payload를 전달하는 데 적합하다

사용자 생성이나 주문 생성처럼 여러 field를 한 번에 보내야 할 때 body가 자연스럽다. JSON, form data, binary stream 등 representation은 `Content-Type`과 함께 해석된다. body는 URL 길이 제한과 query logging 노출을 피할 수 있지만 secret 저장소가 되는 것은 아니다. reverse proxy나 application debug log가 body를 기록하면 민감정보가 그대로 남을 수 있다.

HTTP method와 body 사용 규칙은 protocol semantics와 framework 지원을 확인한다. GET body처럼 표준과 intermediary support가 불명확한 패턴을 사용하면 cache와 proxy가 기대와 다르게 동작할 수 있다. public API에서는 널리 상호운용되는 관례를 따르는 편이 유지보수에 유리하다.

실습에서는 `POST /users`에 JSON body를 보내고 `Content-Type`을 정확히 설정한 경우, 누락한 경우, 다른 media type을 선언한 경우를 비교한다. framework가 어떤 parser를 선택하고 handler의 body 값이 어떻게 달라지는지 기록한다.

---

## CHAPTER 05 · 같은 의미의 값이 여러 입력 위치에 존재하면 precedence를 명시해야 한다

`/users/10` path에 user ID가 있는데 body에도 `{ "id": 20 }`이 들어올 수 있다. handler가 어느 값을 사용할지 암묵적으로 섞으면 update target이 흔들린다. resource identity는 path에서만 받고 body의 ID는 금지하거나, 둘이 반드시 같아야 한다는 rule을 둘 수 있다. 중요한 것은 한 위치를 authoritative source로 정하는 것이다.

특히 ownership·tenant scope를 client body에서 받으면 위험하다. `/tenants/A/orders/10`인데 body에 `tenantId=B`가 들어온 경우 server가 body 값을 우선하면 다른 scope로 write할 수 있다. authenticated principal이나 route scope에서 이미 확정한 identity를 body가 덮어쓰지 못하게 해야 한다.

실습에서 path ID와 body ID를 일부러 다르게 보내고 세 정책을 비교한다. `body 무시`, `불일치 400`, `body 우선` 중 어떤 정책이 resource update에 적합한지 이유를 적는다. 테스트에는 반드시 불일치 case를 남겨 regression을 막는다.

---

## CHAPTER 06 · optional과 missing, null, 빈 문자열은 서로 다른 상태일 수 있다

request schema에서 field가 `optional`이라는 말은 단순히 falsy 값을 허용한다는 뜻이 아니다. field 자체가 없음, `null`, 빈 문자열, 0, false는 각각 다른 representation이다. `value || default`를 사용하면 0이나 false까지 누락처럼 처리할 수 있고, `value ?? default`는 null/undefined만 default로 바꾼다. business 의미에 따라 구분해야 한다.

PATCH style update에서는 이 차이가 더 중요하다. field가 없으면 `변경하지 않음`, `null`이면 `기존 값을 지움`으로 정의할 수 있다. 이를 구분하지 않으면 user가 값을 삭제하려는데 server가 default를 넣거나, 보내지 않은 field를 null로 덮어쓸 수 있다. OpenAPI/JSON Schema의 required와 nullable 표현도 정확히 맞춰야 한다.

실습으로 profile update에 `nickname`을 omitted, null, `""`, `"kim"` 네 형태로 보내고 desired behavior를 먼저 표로 만든다. handler 구현이 그 표와 일치하는지 test한다. compile-time optional type만 보고 runtime 의미를 추측하지 않는다.

---

## CHAPTER 07 · repeated query key와 array 표현은 parser default를 확인해야 한다

client가 `?tag=a&tag=b`를 보내면 query parser가 string array로 만들 수도 있고 마지막 값 하나만 남길 수도 있다. `?tag[]=a&tag[]=b`, comma-separated `?tag=a,b`처럼 여러 관례가 존재한다. public API는 한 representation을 문서화하고 parser가 예상과 같은지 확인해야 한다.

filter expression이 복잡해질수록 query language가 사실상 작은 언어가 된다. 임의 operator, nested expression, field name을 허용하면 parser complexity와 injection, expensive query 가능성이 증가한다. 필요한 기능을 명시적 parameter로 제한하고 max list length를 두는 것이 resource budget 관리에도 도움이 된다.

실습에서는 `tag`의 최대 10개 값을 허용하고 중복 tag를 제거할지 유지할지 policy를 정한다. 0개, 1개, 10개, 11개, 동일 tag 반복을 테스트한다. parser output과 application filter object를 분리해 기록한다.

---

## CHAPTER 08 · sort field와 direction은 allowlist로 내부 query 표현에 매핑한다

`sort=price&direction=desc` 같은 query를 받았을 때 client 문자열을 그대로 SQL `ORDER BY`에 이어 붙이면 injection 가능성과 schema coupling이 생긴다. application은 public sort key를 제한된 enum으로 검증하고 내부 column/expression에 명시적으로 매핑한다.

```text
createdAt → orders.created_at
price     → orders.total_price
```

이렇게 하면 DB column 이름을 public contract에서 숨기고, expensive하거나 권한상 노출하면 안 되는 sort를 차단할 수 있다. direction도 `asc|desc` 두 값만 허용하는 식으로 좁힌다. locale/collation에 따라 문자열 sort 결과가 달라질 수 있으므로 사용자에게 보이는 순서 contract가 중요하면 tie-breaker와 collation을 함께 정의한다.

실습에서 허용 key 3개만 가진 map을 만들고 `sort=unknown`, `direction=DROP...` 같은 임의 문자열이 query builder에 도달하지 않도록 한다. test는 SQL 문자열 자체보다 validated internal sort descriptor가 예상대로 생성되는지를 우선 검증한다.

---

## CHAPTER 09 · pagination 입력은 최대값과 stable ordering을 함께 계약한다

`limit`과 `cursor`도 외부 입력이다. limit 상한이 없으면 한 request가 수십만 row를 요청해 memory·DB·network를 과도하게 사용할 수 있다. cursor는 client가 임의로 조작할 수 없게 opaque representation을 쓰더라도 decode와 signature/version 검증이 필요할 수 있다.

pagination은 입력 validation만으로 끝나지 않는다. stable ordering이 없으면 다음 page에서 중복과 누락이 생길 수 있다. `createdAt DESC, id DESC`처럼 tie-breaker를 포함하고 cursor에 필요한 ordering position을 담는다. offset 방식과 cursor 방식의 trade-off는 data 변경 빈도와 random page access 요구를 기준으로 선택한다.

실습으로 `limit` default 20, max 100을 구현하고 `after` cursor가 malformed일 때 400을 반환한다. 동일 createdAt을 가진 row 여러 개를 준비해 tie-breaker가 없을 때 page 경계가 흔들리는지 test한다.

---

## CHAPTER 10 · content negotiation은 request와 response representation을 분리한다

`Content-Type`은 client가 보낸 body의 형식을 설명하고, `Accept`는 client가 어떤 response media type을 받을 수 있는지 표현한다. 둘을 같은 의미로 생각하면 body parser와 response serializer를 잘못 선택할 수 있다. 서버가 JSON만 지원한다면 unsupported request media type과 acceptable response type failure를 구분할 수 있다.

API가 JSON과 CSV를 모두 지원한다면 같은 resource라도 representation에 따라 field와 streaming 방식이 달라질 수 있다. cache가 `Accept`를 key에 포함해야 할 수도 있고 `Vary` header가 필요할 수 있다. 처음부터 여러 format을 지원할 필요는 없지만, 하나만 지원한다면 `application/json` contract를 명확히 하는 편이 좋다.

실습에서는 POST body의 `Content-Type`을 확인해 JSON 외 형식을 거부하고, GET response에서 `Accept: application/json`만 허용하는 작은 middleware를 만든다. request body format과 response preference가 별도 판단이라는 것을 test case 이름에서도 분리한다.

---

## CHAPTER 11 · forwarding header는 proxy topology를 모르면 client identity 증거가 아니다

reverse proxy나 load balancer 뒤에서 application은 socket peer로 proxy의 IP를 보게 된다. proxy는 원래 client address를 `Forwarded` 또는 `X-Forwarded-For`류 header로 전달할 수 있다. 문제는 internet client도 같은 header를 직접 보낼 수 있다는 점이다. application이 trusted proxy 수를 설정하지 않고 첫 값을 무조건 client IP로 믿으면 rate limit이나 audit이 우회될 수 있다.

안전한 해석은 network topology와 proxy의 overwrite/append policy를 함께 알아야 한다. 가장 가까운 trusted proxy부터 chain을 검증하고, 직접 internet에 노출되는 경우 client-provided forwarding header를 제거하거나 무시할 수 있다. cloud provider마다 공식 권고가 있으므로 배포 환경 문서를 따른다.

실습에서는 proxy가 없는 개발 server에 `X-Forwarded-For: 1.2.3.4`를 직접 보내 server가 쉽게 읽는 것을 본다. 그 값을 `remoteAddress`와 비교한다. 이 차이로 `header가 존재함`과 `trusted infrastructure가 증명함`을 분리한다.

---

## CHAPTER 12 · authentication credential의 위치는 logging·browser behavior와 함께 설계한다

Bearer token을 query에 넣으면 URL log와 history, referrer에 노출될 가능성이 커진다. HTTP authorization scheme에 맞춰 `Authorization` header를 사용하는 것이 일반적이다. browser session에서는 HttpOnly cookie가 JavaScript 접근을 줄일 수 있지만 browser가 자동 첨부하므로 CSRF threat와 SameSite policy를 함께 본다. credential transport 방식마다 다른 위험이 있다.

credential을 body에 보내는 login request와 이후 authenticated request에서 token/session을 전달하는 방식도 구분한다. password는 초기 credential이고 access token은 별도 수명주기와 scope를 가진다. server log middleware는 authorization header와 cookie를 redaction해야 한다.

실습으로 request logger가 모든 header를 dump하는 위험한 버전을 만들고 test token이 log에 나타나는지 확인한다. 이후 allowlist 기반 logging 또는 sensitive header redaction을 적용해 같은 request에서 token 문자열이 사라지는지 검증한다.

---

## CHAPTER 13 · tenant와 actor identity는 client가 선언한 field보다 검증된 context에서 가져온다

multi-tenant API에서 body에 `tenantId`가 있다고 그 값을 곧바로 scope로 사용하면 다른 tenant ID로 바꾸는 공격이 가능하다. route가 `/tenants/:tenantId/...` 구조여도 authenticated principal이 해당 tenant에 속하는지 확인해야 한다. identity와 scope는 authentication·authorization 결과에서 생성된 trusted context와 연결한다.

client가 `createdBy`, `ownerId`, `role`을 보내더라도 server가 직접 principal 값으로 덮어쓰는 것이 안전한 경우가 많다. 물론 관리자 위임 기능처럼 다른 owner를 지정하는 legitimate use case도 있으므로 permission이 있는 별도 command로 모델링한다. 일반 update body에서 implicit privilege escalation을 허용하지 않는다.

실습에서는 authenticated user ID가 7인데 body에 `ownerId=8`을 넣는 create request를 보낸다. server가 owner를 7로 강제하거나 불필요한 field를 거부하도록 하고, 관리자 전용 endpoint에서만 explicit owner selection을 허용하는 test를 만든다.

---

## CHAPTER 14 · 입력 위치 선택은 cache·observability·privacy에도 영향을 준다

URL path와 query는 access log와 tracing attribute에 자주 기록되고 CDN/cache key에도 사용된다. body는 일반 cache key에 포함되지 않는 경우가 많고 observability에서 자동 수집되지 않을 수도 있다. header는 일부 proxy가 제거·normalize하며 cache vary 조건이 될 수 있다. 같은 값이라도 어디에 넣느냐에 따라 infrastructure 동작이 달라진다.

예를 들어 검색어를 query에 두면 shareable URL이 되지만 민감한 의료 검색어라면 access log privacy를 검토해야 한다. large filter object를 POST body에 넣는 search endpoint는 URL 길이 문제를 피하지만 일반 HTTP cache 이용이 어려워질 수 있다. API 설계는 REST 형태의 미학보다 **consumer behavior와 infrastructure contract**를 함께 본다.

연습으로 세 기능을 설계한다: public product search, password reset, admin bulk export. 각 값의 위치를 path/query/header/body 중 선택하고 caching, logging, sensitivity, size를 기준으로 이유를 적는다. 하나의 규칙으로 모든 endpoint를 만들지 않는다.

---

## CHAPTER 15 · 입력 채널 실습은 서로 충돌하는 값을 일부러 보내며 끝낸다

`PATCH /users/:id` endpoint를 만들고 path에는 target user ID, query에는 `dryRun=true|false`, header에는 correlation ID와 credential, body에는 변경 가능한 `displayName`만 받는다. body가 `id`, `role`, `tenantId`를 포함하면 schema가 거부하도록 한다. authenticated principal과 path target이 다르면 authorization 단계에서 차단한다.

테스트는 정상 update보다 충돌 case를 더 많이 포함한다. path ID 10/body ID 11, duplicated query key, invalid boolean, oversized displayName, unknown body field, missing Content-Type, client가 만든 fake forwarding header, credential query parameter를 보낸다. 각 실패가 parsing·validation·authorization 중 어느 경계에서 발생해야 하는지 먼저 적은 뒤 실행 결과와 비교한다.

AI는 OpenAPI parameter 선언이나 schema 문법을 생성하는 데 활용할 수 있다. 사람은 authoritative source를 정하고, 같은 identity가 여러 위치에서 충돌할 때 precedence를 결정하며, 민감정보가 URL/log에 남지 않는지 검토해야 한다. 이 판단이 명확하면 handler는 `어디서 값을 읽을지`보다 `검증된 입력을 어떤 use case에 전달할지`에 집중할 수 있다.
