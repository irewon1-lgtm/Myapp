# TRACK 07 · 서버와 백엔드

# BLOCK 03 · 인증·권한·신뢰 경계를 서버에서 지키기

이번 BLOCK은 암호학을 깊게 파는 과정이 아니다. TRACK 09에서 보안을 더 깊게 배운다. 여기서는 backend가 request를 처리할 때 반드시 지켜야 하는 **신뢰 경계**를 배운다.

핵심은 세 문장이다.

```text
client가 보낸 값은 증거가 아니다.
로그인했다는 사실과 이 데이터를 볼 권한은 별개다.
server가 공개할 field와 변경을 허용할 field를 직접 선택한다.
```

---

## LESSON 31 · authentication과 authorization은 질문 자체가 다르다

### 1. 두 질문

```text
authentication  → 너는 누구인가?
authorization   → 그 사람이 이 행동을 해도 되는가?
```

로그인에 성공했다고 모든 주문을 볼 수 있는 것은 아니다.

### 2. 주문 조회 예

```text
GET /orders/100
```

server가 token을 확인해 user 7이라는 것을 알아냈다고 하자. 여기서 끝이 아니다.

```text
order 100의 owner가 user 7인가?
관리자처럼 별도 권한이 있는가?
이 tenant에서 볼 수 있는가?
```

까지 확인해야 한다.

### 3. handler 흐름

```text
request
→ credential 읽기
→ authentication
→ principal 생성
→ resource 찾기
→ authorization
→ response
```

resource를 찾기 전에 권한을 계산할 수 있는 경우도 있고, resource 속성이 있어야 판단할 수 있는 경우도 있다.

### 4. 작은 문제

`req.user`가 존재하면 `GET /orders/:id`를 허용하는 코드는 왜 불충분한가?

로그인 여부만 검사했지 특정 order에 대한 권한을 검사하지 않았기 때문이다.

---

## LESSON 32 · session과 token은 `인증 상태를 전달하는 방법`이지 권한 규칙 그 자체가 아니다

### 1. session 방식

server가 session id와 로그인 상태를 저장하고 client가 cookie 등으로 session id를 보낼 수 있다.

```text
client cookie: session=abc
↓
server session store
↓
abc → user 7
```

### 2. token 방식

client가 credential/token을 보내고 server가 이를 검증해 subject/claims를 얻을 수 있다.

### 3. JWT라는 형식 자체가 권한을 보장하지 않는다

signature가 유효하다는 사실은 token이 지정한 issuer/key 체계에서 변조되지 않았다는 의미와 연결될 수 있지만, `이 사용자가 order 100을 읽어도 된다`는 business authorization은 여전히 별도 검사다.

### 4. 만료·폐기·rotation 문제가 있다

session/token 선택에는 logout 즉시 반영, token theft, refresh, key rotation, distributed storage 같은 trade-off가 생긴다. 이번 LESSON에서는 직접 구현하지 않고 검증된 library와 표준을 사용한다는 원칙만 잡는다.

### 5. 작은 문제

JWT payload의 `role: admin` 문자열을 decode만 하고 signature/issuer/audience를 검증하지 않은 채 믿어도 되는가?

안 된다. decode는 검증이 아니다. token validation은 사용하는 표준/library의 security contract를 따라야 한다.

---

## LESSON 33 · auth middleware는 credential을 principal로 바꾸고 뒤 단계에 전달한다

### 1. principal은 `현재 호출자에 대해 검증된 identity 정보`를 담는다

예:

```ts
type Principal = {
  userId: string;
  tenantId: string;
  roles: string[];
};
```

### 2. middleware 책임을 제한한다

```text
credential 위치 확인
→ 검증
→ principal 생성
→ request context에 연결
```

주문 취소 가능 여부까지 auth middleware에 넣으면 모든 route의 domain rule이 한곳에 몰린다.

### 3. optional auth와 required auth를 구분한다

public endpoint는 principal이 없어도 동작할 수 있다. private endpoint는 반드시 요구한다.

```ts
const principal = requirePrincipal(req);
```

`req.user!` 같은 non-null assertion만 남발하지 말고 runtime guard를 둔다.

### 4. 직접 실습

credential 없음, malformed credential, expired credential, valid credential 네 경우를 fake verifier로 만들어 middleware 결과를 비교한다.

### 5. 작은 문제

auth middleware가 실패했는데 `next()`를 호출하면 어떻게 될 수 있는가?

뒤 handler가 principal 없는 상태에서 실행될 수 있다. response를 끝냈다면 흐름도 끝내야 한다.

---

## LESSON 34 · object-level authorization은 `이 특정 객체`에 접근 가능한지 확인한다

### 1. BOLA 문제를 아주 단순하게 본다

```ts
app.get("/orders/:id", async (req, res) => {
  const order = await orders.findById(req.params.id);
  res.json(order);
});
```

로그인 middleware가 앞에 있어도 `id`를 100에서 101로 바꾸어 다른 사람 주문을 볼 수 있다면 broken object-level authorization 문제가 된다.

### 2. UUID를 쓰면 해결되는가?

아니다. 예측하기 어려운 ID는 공격 난이도에 영향을 줄 수 있지만 authorization check를 대신하지 않는다. OWASP도 object identifier의 형태와 무관하게 object-level authorization이 필요하다고 강조한다.

### 3. query 자체에 scope를 넣을 수 있다

```ts
const order = await orders.findByIdForUser(orderId, principal.userId);
```

또는 조회 후 policy를 검사할 수 있다. 중요한 것은 **모든 object access path에서 같은 규칙이 빠지지 않는 것**이다.

### 4. 존재 여부 노출까지 고려한다

권한 없는 object에 403을 줄지 404처럼 숨길지는 API threat model과 UX에 따라 결정한다. 정답 하나를 외우지 않는다.

### 5. 직접 문제

관리자 endpoint만 검사하고 mobile endpoint의 같은 repository access는 검사하지 않았다. 왜 중앙 policy나 scoped repository가 도움이 될 수 있는가?

여러 entry point에서 authorization 누락을 줄일 수 있기 때문이다.

---

## LESSON 35 · function-level authorization은 `어떤 기능`을 실행할 수 있는지 확인한다

### 1. 관리자 기능 예

```text
GET /admin/users
POST /admin/refunds
DELETE /admin/products/:id
```

일반 user가 URL을 모르리라고 기대하면 안 된다.

### 2. UI에서 버튼을 숨기는 것은 server authorization이 아니다

client UI는 공격자가 직접 API를 호출하는 것을 막지 못한다.

### 3. role만으로 충분하지 않을 수 있다

`ADMIN` 하나로 모든 권한을 묶으면 너무 넓은 privilege가 생길 수 있다. permission/capability 단위로 나눌 수도 있다.

```text
order.read
order.refund
product.write
user.suspend
```

### 4. deny by default

새 endpoint가 생겼을 때 명시적으로 permission을 지정하지 않으면 자동 허용되는 구조보다 기본 거부가 안전할 수 있다.

### 5. 작은 문제

`if (user.role === "admin")`를 60개 handler에 복붙하면 어떤 위험이 생기는가?

role 이름 변경, 예외 규칙, 누락이 여러 곳에 흩어진다. policy function이나 middleware로 공통화할 필요가 있다.

---

## LESSON 36 · property-level authorization은 object 안의 field마다 읽기·쓰기 권한이 다를 수 있음을 다룬다

### 1. mass assignment 예

client가 profile update를 보낸다.

```json
{
  "displayName": "kim",
  "role": "admin",
  "isBlocked": false
}
```

server가 이 object를 ORM update에 통째로 넘기면 client가 바꾸면 안 되는 field까지 변경할 수 있다.

### 2. allowlist로 직접 고른다

```ts
const update = {
  displayName: input.displayName
};
```

변경을 허용할 field를 endpoint contract에서 명확히 선택한다.

### 3. response도 property authorization이 필요하다

내부 user object의 `riskScore`, `passwordHash`, `supportNotes`를 통째로 내보내면 과도한 정보 노출이 된다.

### 4. request schema와 response schema를 별도로 둔다

같은 `User` type 하나로 create/update/response/admin response를 모두 표현하려 하지 않는다.

### 5. 작은 문제

TypeScript에서 `Pick<User, "displayName">`을 썼다. runtime body에 `role`이 추가돼 와도 자동 제거되는가?

아니다. TypeScript type은 runtime sanitizer가 아니다. validator가 unknown property를 어떻게 처리하는지 확인해야 한다.

---

## LESSON 37 · tenant boundary는 다중 고객 시스템에서 데이터 경계를 추가한다

### 1. user ownership과 tenant ownership은 다를 수 있다

회사 A의 직원 user 7과 회사 B의 직원 user 8이 있다.

```text
order.tenantId = A
principal.tenantId = B
```

user 권한만 보고 tenant scope를 빠뜨리면 회사 간 데이터가 섞일 수 있다.

### 2. tenant id를 client body에서 믿지 않는다

```json
{ "tenantId": "A", "name": "..." }
```

client가 임의로 tenant를 바꿀 수 있다면 위험하다. authenticated principal 또는 trusted routing context에서 tenant scope를 얻고 body와 분리하는 방식이 필요하다.

### 3. query에 tenant condition을 포함한다

```text
WHERE tenant_id = currentTenant AND id = requestedId
```

repository level에서 scope를 강제하면 실수 방지에 도움이 된다.

### 4. background job도 tenant context를 잃으면 안 된다

HTTP request에서 queue로 넘어갈 때 tenant id가 message에 명시되어야 할 수 있다. worker가 global query를 하면 cross-tenant 사고가 날 수 있다.

### 5. 작은 문제

admin은 모든 tenant를 볼 수 있으니 tenant filter를 완전히 제거해도 되는가?

권한 모델에 따라 다르지만 broad bypass는 audit와 실수 위험을 키운다. explicit privileged path와 logging이 필요하다.

---

## LESSON 38 · trust boundary는 `외부에서 들어온 것`만이 아니라 서로 다른 시스템 사이에도 존재한다

### 1. 우리 frontend가 보낸 값도 untrusted input이다

사용자가 개발자 도구, script, 다른 client로 API를 직접 호출할 수 있다.

### 2. 내부 service도 무조건 신뢰하지 않는다

microservice A가 B를 호출할 때 A가 버그나 침해 상태일 수 있다. 중요한 invariant는 B에서도 검증해야 한다.

### 3. allowlist와 denylist 차이

허용하면 안 되는 문자열을 하나씩 막는 denylist는 우회가 생기기 쉽다. 가능한 값이 좁은 domain에서는 허용 목록을 정의하는 편이 명확하다.

```ts
const SortFields = ["createdAt", "price"] as const;
```

client가 임의 SQL column/expression을 sort key로 전달하게 두지 않는다.

### 4. parser와 interpreter 경계를 조심한다

입력 문자열이 SQL, shell, template, URL, regex 등 다른 언어/해석기로 들어갈 때 injection 위험이 생긴다. 각각의 안전한 API와 parameterization을 사용한다.

### 5. 작은 문제

`internal=true` header가 있으면 관리자 기능을 허용한다. client가 그 header를 직접 추가할 수 있다면?

header 이름만으로 trust가 생기지 않는다. 누가 설정했고 network boundary에서 어떻게 보호되는지 증거가 필요하다.

---

## LESSON 39 · output minimization은 필요한 데이터만 반환하는 원칙이다

### 1. `나중에 쓸지도 모르니까 다 보내자`는 비용이 있다

```text
개인정보 노출 범위 증가
client-server 결합 증가
payload 증가
권한 검사 복잡도 증가
field 제거가 breaking change가 될 가능성 증가
```

### 2. endpoint 목적에 맞는 representation을 만든다

목록에서는 summary, 상세에서는 detail을 줄 수 있다.

```ts
type UserSummary = { id: string; displayName: string };
type UserDetail = { id: string; displayName: string; bio: string };
```

### 3. nested object도 검토한다

`order.user`를 통째로 serialize하면 user 내부 field가 새로 추가될 때 API에 자동 노출될 수 있다.

### 4. log output도 minimization 대상이다

request body 전체를 debug log에 남기면 password, token, 주민번호 같은 값이 들어갈 수 있다. observability와 data minimization을 함께 본다.

### 5. 작은 문제

response에서 passwordHash는 뺐지만 `resetToken`을 남겼다. `민감한 field 몇 개를 blacklist` 방식이 왜 취약한가?

새로운 secret field가 추가될 때 누락될 수 있다. 필요한 field만 allowlist하는 response mapper가 더 명확하다.

---

## LESSON 40 · rate limit과 resource budget은 서버 자원이 무한하지 않음을 코드에 반영한다

### 1. 요청 1회 비용이 모두 같지 않다

```text
간단한 profile 조회 → 저비용
복잡한 검색 → CPU/DB 비용 큼
파일 변환 → 매우 큼
SMS 전송 → 외부 비용까지 발생
```

### 2. rate limit은 횟수만 세는 것이 아니다

user/IP/API key/tenant/business operation별로 기준이 다를 수 있다. distributed server에서 counter consistency도 고려한다.

### 3. payload·page size도 budget이다

`limit=1000000`을 허용하면 한 request가 memory와 DB를 과도하게 사용한다. body size, page size, query complexity, upload size에 상한을 둔다.

### 4. 비용이 발생하는 side effect는 더 강하게 제한한다

email, SMS, AI API, 결제 verification처럼 호출당 돈이 들 수 있는 기능은 abuse가 곧 비용 폭증으로 이어진다.

### 5. 작은 문제

모든 요청을 `100 req/min`으로 동일하게 제한하면 충분한가?

아니다. operation 비용과 business abuse 형태가 다르다. endpoint별 budget과 global protection을 조합할 수 있다.

---

## LESSON 41 · SSRF는 server가 대신 URL을 호출해 주는 기능에서 trust boundary가 뒤집히는 문제다

### 1. 예

```json
POST /preview
{ "url": "https://example.com/image.png" }
```

server가 client가 준 URL을 그대로 fetch한다.

공격자는 localhost, cloud metadata endpoint, internal admin host 같은 외부에서 직접 접근할 수 없는 주소를 넣을 수 있다.

### 2. URL validation은 문자열 prefix 하나로 끝나지 않는다

DNS resolution, redirect, IPv6/IPv4 표현, userinfo, encoded address 등 우회가 있을 수 있다. 보안 library와 OWASP guidance를 따른다.

### 3. 가능하면 destination을 좁힌다

완전 임의 URL fetch가 필요한지 먼저 묻는다. known provider allowlist, object storage ID처럼 더 좁은 입력으로 설계할 수 있다.

### 4. network egress control도 방어층이다

application validation 하나에만 기대지 않고 server가 internal sensitive network로 나갈 수 있는 범위를 줄이는 infrastructure control도 사용할 수 있다.

### 5. 작은 문제

`url.startsWith("https://")`면 안전하다고 할 수 있는가?

아니다. HTTPS scheme은 destination이 안전한지 말해 주지 않는다.

---

## LESSON 42 · CORS와 CSRF는 둘 다 browser와 관련 있지만 해결하려는 문제가 다르다

### 1. CORS

browser가 다른 origin의 response를 script가 읽을 수 있는지 제어하는 메커니즘과 관련된다. server가 `Access-Control-Allow-Origin` 등을 설정한다.

### 2. CSRF

browser가 cookie 같은 credential을 자동 첨부하는 상황에서 공격자 site가 사용자의 권한으로 state-changing request를 보내게 만드는 문제가 핵심이다.

### 3. `CORS 막았으니 CSRF 끝`이 아니다

browser가 response를 읽지 못해도 request 자체가 전송되고 side effect가 발생할 수 있는 경우를 고려해야 한다.

### 4. SameSite·CSRF token·origin checking 등

인증 방식과 application 구조에 따라 방어를 조합한다. 깊은 설정은 TRACK 09에서 다룬다.

### 5. 작은 문제

mobile native app API라서 browser CORS가 핵심이 아닐 수 있다. 그렇다고 authorization을 빼도 되는가?

아니다. CORS는 server authorization의 대체물이 아니다.

---

## LESSON 43 · password는 직접 암호화/해시 알고리즘을 발명하지 않는다

### 1. password 저장의 목표

DB가 유출돼도 원래 password를 쉽게 복원·대입하지 못하게 하는 것이 중요하다.

### 2. 일반 hash 한 번으로 끝내지 않는다

빠른 general-purpose hash는 대량 추측 공격에 유리하다. password storage용으로 설계된 느리고 memory-hard한 algorithm과 적절한 parameter를 사용한다.

### 3. library가 제공하는 검증 함수를 사용한다

```text
hashPassword(password)
verifyPassword(password, storedHash)
```

salt 생성/format/parameter를 검증된 library에 맡긴다.

### 4. password를 log에 남기지 않는다

validation error debug를 위해 request body를 통째로 찍다가 password가 log에 남는 실수가 흔하다.

### 5. 작은 문제

`SHA-256(password)`를 저장하면 해시니까 안전하다고 말할 수 있는가?

password storage 전용 설계가 아니며 너무 빠른 hash는 brute-force에 불리할 수 있다. 최신 OWASP password storage guidance를 따른다.

---

## LESSON 44 · secret은 source code·client·log로 흘러가지 않게 lifecycle을 관리한다

### 1. 대표 secret

```text
DB credential
payment API secret
JWT signing key
private key
webhook signing secret
```

### 2. frontend bundle에 넣는 순간 secret이 아니다

browser/mobile client에 포함된 값은 사용자 기기에서 추출될 수 있다고 가정한다. server-side secret을 client app에 심지 않는다.

### 3. log redaction

Authorization header, cookie, API key, password field 등을 structured logger에서 마스킹/제외한다.

### 4. rotation 가능성을 설계한다

key를 바꾸려면 service를 모두 동시에 깨뜨려야 하는 구조보다 old/new key overlap 또는 versioning이 가능한 구조가 운영에 유리할 수 있다.

### 5. 작은 문제

secret을 Git에서 지웠다. 과거 commit에도 없어졌는가?

일반 삭제 commit만으로 history의 값이 사라지지 않는다. 노출된 secret은 rotation/revocation 관점으로 처리한다.

---

## LESSON 45 · BLOCK 03 실전: `내 주문 보기/취소` API의 권한 경계를 끝까지 검증한다

### 1. API

```text
GET    /orders/:id
POST   /orders/:id/cancel
PATCH  /profile
POST   /admin/refunds/:id
```

### 2. principal

```ts
type Principal = {
  userId: string;
  tenantId: string;
  permissions: string[];
};
```

### 3. 검증할 경계

```text
credential 검증
object ownership
Tenant scope
function permission
property write allowlist
response field allowlist
request/resource size limit
outbound URL 제한
secret redaction
```

### 4. 공격자 관점 실습

정상 UI를 쓰지 않고 직접 request를 만든다.

```text
다른 order id로 변경
body에 role/admin field 추가
tenantId 변경
page size 극단값
admin URL 직접 호출
Authorization header 누락/변조
```

각 요청이 server 어느 단계에서 차단되는지 기록한다.

### 5. 틀린 답 예

`UUID라서 order id를 못 맞추므로 안전하다.`

틀렸다. ID 추측 난이도와 authorization은 별개다.

`관리자 버튼을 UI에서 숨겼으므로 일반 user는 호출 못 한다.`

틀렸다. API는 직접 호출할 수 있다.

`TypeScript request type에 role field가 없으니 role은 못 바꾼다.`

틀렸다. runtime network payload는 TypeScript compiler를 거치지 않는다.

### 6. 사람이 배워야 할 핵심

보안 library 사용법은 AI가 찾아 줄 수 있다. 그러나 아래 질문은 사람이 이해해야 한다.

```text
누구의 데이터인가?
누가 어떤 action을 할 수 있는가?
어느 field를 읽거나 바꿀 수 있는가?
어느 입력 경계부터 믿을 수 있는가?
실패했을 때 민감정보가 밖으로 새지 않는가?
```

이 다섯 질문은 backend 설계의 기본 계약이다.
