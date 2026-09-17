# PART 10 · BLOCK 01 · LESSON 10 · response DTO와 serialization으로 내부 상태와 외부 계약을 분리하기

application이 가진 object를 그대로 `res.json()`에 넣으면 처음에는 빠르게 API가 만들어진다. 하지만 내부 field가 늘어날 때 자동으로 외부에 노출되고, database schema 변경이 client breaking change가 되며, Date·BigInt·decimal 같은 값이 wire representation에서 다르게 바뀔 수 있다. response는 내부 객체를 보여 주는 창이 아니라 **consumer에게 약속한 별도 데이터 계약**이다. 이 PART는 response DTO, field allowlist, serialization, pagination envelope, 권한별 projection, payload budget을 한 흐름으로 연결한다.

---

## CHAPTER 01 · 내부 model과 public response는 변경 이유가 다르므로 같은 형태일 필요가 없다

내부 User object에는 id, email, passwordHash, riskScore, supportNote, createdAt 같은 값이 있을 수 있다. public profile API는 displayName과 avatar만 필요할 수 있고, account API는 email을 추가로 보여 줄 수 있다. 내부 object를 통째로 serialize하면 새 internal field가 추가되는 순간 API surface도 몰래 변한다.

response DTO는 endpoint가 외부에 공개할 모양을 명시한다. DTO라는 이름을 반드시 쓸 필요는 없지만 `internal representation → public representation` 변환 경계를 갖는 것이 중요하다. database row와 domain object, API response가 각각 다른 목적을 가질 수 있다.

실습에서는 User 내부 object에 `passwordHash`와 `internalFlags`를 추가한 뒤 response mapper가 기존 JSON을 그대로 유지하는지 snapshot test한다. 내부 field 추가가 public response diff를 만들지 않는 것을 확인한다.

---

## CHAPTER 02 · allowlist mapping은 공개할 field를 선택하고 blacklist 누락 위험을 줄인다

`const { passwordHash, ...rest } = user`처럼 몇 개 민감 field를 빼는 blacklist 방식은 새 secret field가 추가될 때 자동으로 외부에 포함될 수 있다. 반대로 mapper에서 id, displayName, createdAt처럼 필요한 field만 작성하면 새 내부 field는 기본적으로 숨겨진다. public exposure는 명시적 opt-in이 된다.

```ts
function toUserResponse(user: User): UserResponse {
  return {
    id: user.id.value,
    displayName: user.displayName,
    createdAt: user.createdAt.toISOString()
  };
}
```

mapper도 test 대상이다. 특히 nested object를 그대로 넣지 않고 nested DTO를 별도로 만든다. `order.user = user`처럼 entity 전체를 연결하면 새 field가 깊은 위치에서 새어 나갈 수 있다.

실습에서는 internal object에 테스트용 secret을 계속 추가하는 mutation test를 하고 public JSON에 나타나지 않는지 확인한다.

---

## CHAPTER 03 · Date와 time-zone 정보는 wire에서 명확한 문자열 contract로 바꾼다

JavaScript Date는 JSON serialization에서 ISO 형태 문자열로 표현될 수 있지만 application이 그 default에만 의존하면 date-only와 instant 의미가 섞인다. `birthDate`는 `YYYY-MM-DD` 같은 calendar date contract가 적합할 수 있고 `createdAt`은 UTC instant 문자열이 적합하다. client는 둘을 다른 type으로 해석해야 한다.

server local timezone에 따라 문자열 format이 달라지지 않도록 explicit formatting을 사용한다. offset을 포함한 local time을 반환할 때는 어느 zone의 값인지 contract를 문서화한다. 같은 timestamp field가 endpoint마다 `Z`, epoch milliseconds, locale 문자열로 섞이지 않게 한다.

실습에서는 Date object, date-only value, schedule local time을 각각 response type으로 나누고 한국과 다른 timezone 환경에서도 JSON fixture가 안정적인지 test한다.

---

## CHAPTER 04 · BigInt·decimal·money는 JSON number의 정밀도 한계를 고려해 표현한다

JavaScript BigInt는 기본 JSON stringify에서 그대로 직렬화되지 않는다. int64 identifier나 매우 큰 counter를 number로 보내면 JavaScript client가 safe integer 범위를 넘어서 정밀도를 잃을 수 있다. string representation을 사용하거나 API schema에서 format과 client generator behavior를 확인한다.

화폐와 decimal은 binary floating-point representation에 의존하면 `0.1 + 0.2` 같은 오차가 나타날 수 있다. minor-unit integer와 currency code 또는 decimal string을 public contract로 선택할 수 있다. 중요한 것은 server와 모든 client가 동일한 단위와 rounding rule을 공유하는 것이다.

실습에서 BigInt stringify failure를 재현하고 string mapper를 적용한다. 금액은 `{"currency":"KRW","minor":1000}`처럼 보내 client가 단위를 추측하지 않게 한다.

---

## CHAPTER 05 · undefined·null·field omission은 client가 보는 상태를 다르게 만든다

JavaScript object의 `undefined` property는 JSON serialization에서 생략될 수 있고 array 안의 undefined는 다른 형태로 나타날 수 있다. `null`은 명시적 JSON value다. API에서 field가 없음과 null이 같은 의미인지 별도인지 결정해야 한다. schema의 required/nullable 정의와 실제 serializer 결과가 일치해야 한다.

PATCH response나 partial view에서 field omission을 `권한 없음`, `not loaded`, `값 없음` 세 의미로 혼용하면 client가 상태를 구분할 수 없다. 필요하다면 explicit status field 또는 별도 endpoint를 사용한다. authorization 때문에 숨긴 field가 null로 표시되면 존재 자체를 노출할 수도 있다.

실습에서는 undefined, null, empty string을 가진 object를 serialize하고 JSON 결과를 기록한다. OpenAPI schema와 fixture가 같은 required/nullable contract를 표현하는지 확인한다.

---

## CHAPTER 06 · nested relation을 그대로 serialize하면 over-fetch와 data leak이 함께 생길 수 있다

ORM이 user와 organization, orders, paymentMethod를 eager load한 object를 반환할 수 있다. response에 entity를 그대로 넣으면 의도하지 않은 relation과 내부 field까지 JSON으로 따라올 수 있다. 또한 circular relation이 있으면 serializer가 실패할 수 있다. response mapper는 필요한 nested projection만 명시한다.

목록 endpoint에서는 summary representation을, 상세 endpoint에서는 detail representation을 둘 수 있다. 모든 endpoint가 가장 큰 UserDetail을 반환하면 network와 serialization CPU가 늘고 mobile client parsing 비용도 커진다. consumer가 실제로 필요한 data shape를 기준으로 projection을 만든다.

실습으로 User→Organization→Users 순환 object를 stringify해 failure를 확인하고, `UserSummary`와 `OrganizationSummary` mapper로 cycle을 끊는다. query에서도 필요한 column만 읽도록 repository projection과 연결한다.

---

## CHAPTER 07 · 권한에 따라 field가 달라질 때 entity 하나의 toJSON에 정책을 숨기지 않는다

일반 사용자는 email을 볼 수 있지만 support agent는 일부 masked data, admin은 더 많은 audit field를 볼 수 있다고 하자. entity의 generic `toJSON()` 하나가 caller 권한을 모르면 정확한 response를 만들기 어렵다. endpoint/use-case context에 맞는 projection을 선택한다.

permission별 field filtering을 response 후반에 blacklist로 제거하면 이미 log/cache에 full object가 들어갔을 수 있다. 가능한 한 query/projection 단계에서 필요한 field만 가져오고 public mapper에서 다시 allowlist한다. 데이터 최소화는 storage read부터 response까지 여러 방어층을 가질 수 있다.

실습에서 `toSelfProfile`, `toPublicProfile`, `toSupportProfile` 세 mapper를 만들고 같은 User domain object가 다른 JSON을 만드는지 test한다. forbidden field matrix를 작성해 permission별 leak regression을 잡는다.

---

## CHAPTER 08 · collection response는 items 외에도 pagination과 ordering contract를 전달한다

단순 array만 반환하면 다음 page cursor, total count, applied sort를 어디에 넣을지 애매해진다. envelope를 사용할 수 있다.

```json
{
  "items": [ ... ],
  "nextCursor": "...",
  "hasMore": true
}
```

`totalCount`는 expensive query일 수 있고 데이터가 계속 바뀌면 정확한 의미를 정의해야 한다. cursor는 opaque하게 취급하고 client가 내용을 parsing하도록 문서화하지 않는다. ordering이 stable해야 nextCursor가 의미 있다.

실습에서는 page size보다 하나 더 조회해 hasMore와 cursor를 계산하고 마지막 page에서 null cursor를 반환한다. 동일 timestamp tie-breaker를 포함한 fixture로 중복/누락이 없는지 test한다.

---

## CHAPTER 09 · field naming은 내부 언어 관례와 public API 관례를 독립적으로 선택할 수 있다

TypeScript 내부에서는 camelCase를 쓰고 DB는 snake_case일 수 있다. API를 camelCase로 정했다면 mapper가 변환을 담당한다. ORM naming을 public JSON에 그대로 노출하지 않으면 storage schema를 바꿔도 API를 유지할 수 있다. 자동 snake/camel conversion middleware를 쓰더라도 acronym, nested map key, external payload가 의도대로 변환되는지 확인한다.

`userID`, `userId`, `user_id`가 endpoint마다 섞이면 client code와 documentation이 복잡해진다. naming convention을 lint하거나 OpenAPI style rule로 검사할 수 있다. 단 external provider raw payload를 passthrough해야 하는 endpoint라면 provider contract를 보존한다.

실습에서 DB row mapper와 response mapper를 분리하고 DB column rename이 public fixture를 바꾸지 않는지 test한다.

---

## CHAPTER 10 · content negotiation이 있으면 같은 resource도 serializer가 달라질 수 있다

JSON만 지원한다면 response path가 단순하지만 CSV export, protobuf, file download를 지원하면 `Accept`나 별도 endpoint에 따라 serializer가 달라진다. representation마다 field ordering, escaping, streaming, compression 특성이 다르다. business object를 format-specific writer에 직접 섞지 않고 projection과 encoding을 분리할 수 있다.

CSV는 spreadsheet formula injection 같은 별도 안전 문제가 있고, binary protocol은 schema evolution rule이 있다. 필요 없는 format을 단지 framework가 지원한다고 켜지 않는다. supported representation을 명시하고 406/415 semantics를 이해한다.

실습에서는 동일 `OrderSummary` projection을 JSON과 CSV writer에 넣고 값 의미가 유지되는지 비교한다. CSV escaping과 newline field를 test한다.

---

## CHAPTER 11 · compression은 payload 크기를 줄이지만 CPU와 latency budget을 사용한다

gzip/brotli response compression은 text payload의 network byte를 줄일 수 있지만 server CPU를 사용하고 작은 response에서는 header와 compression overhead가 이득보다 클 수 있다. reverse proxy가 compression을 담당할 수도 있다. application과 proxy가 중복 압축하지 않도록 실제 topology를 확인한다.

secret과 attacker-controlled input이 같은 compressed response에 섞이는 경우 compression side-channel 같은 보안 고려도 존재한다. 모든 endpoint에 동일 setting을 무조건 적용하지 않는다. image/video처럼 이미 압축된 format은 추가 compression 이득이 적다.

실습에서 작은 JSON, 큰 반복 JSON, random binary의 compressed/uncompressed size와 CPU 시간을 비교한다. threshold를 설정할 근거를 만든다.

---

## CHAPTER 12 · response schema는 OpenAPI 문서와 runtime 결과가 함께 검증되어야 한다

OpenAPI에 UserResponse를 정의해도 실제 mapper가 field를 누락하거나 잘못된 type을 보내면 문서만 맞고 runtime은 틀린다. integration test에서 실제 HTTP response를 schema validator로 검사하거나 generated client fixture를 사용해 drift를 잡을 수 있다.

반대로 runtime object에서 자동 OpenAPI를 생성해도 모든 business 의미가 문서화되는 것은 아니다. enum description, permission, conditional field, pagination semantics는 사람이 추가해야 한다. schema generation은 duplication을 줄이는 도구이지 계약 판단을 대신하지 않는다.

실습에서는 실제 route를 호출해 response를 OpenAPI/JSON Schema against validation하고, mapper에서 일부러 `id`를 number로 바꿔 test가 실패하는지 확인한다.

---

## CHAPTER 13 · serialization 자체가 CPU·memory hotspot이 될 수 있다

DB query는 20ms인데 50MB JSON을 stringify하는 데 수백 ms가 걸리고 event loop를 막을 수 있다. response가 커지면 temporary string allocation과 garbage collection pressure도 증가한다. `DB가 빠르니 API도 빠르다`가 아니다. payload size와 serialization duration을 separately 측정한다.

대용량 export는 streaming writer나 async job으로 분리할 수 있다. 일반 list endpoint는 pagination과 field projection으로 payload를 제한한다. compression까지 더해지면 CPU 비용이 추가된다. 성능 최적화는 실제 profile과 payload distribution을 근거로 한다.

실습에서 100, 10k, 100k object 배열을 serialize하고 duration과 output byte를 측정한다. 일반 endpoint의 max page size가 serialization budget 안에 들어오는지 확인한다.

---

## CHAPTER 14 · response logging은 body 전체보다 contract와 outcome을 기록한다

debugging 편의를 위해 response body 전체를 log하면 개인정보와 secret이 log storage로 복제된다. payload가 크면 logging 자체가 latency와 비용을 늘린다. route, status, response size, error code, request ID 같은 metadata로 대부분의 운영 질문을 해결할 수 있다.

특정 incident에서 body sample이 필요하면 redaction과 sampling, 접근 통제를 가진 별도 capture를 설계한다. 성공 response와 error response의 retention도 다르게 둘 수 있다. log는 API가 공개한 정보보다 더 민감한 internal detail을 포함할 수 있음을 잊지 않는다.

실습에서는 logger가 response byte 수만 기록하고 body를 저장하지 않게 한다. test secret을 response mapper에서 제거한 뒤 log에도 없는지 확인한다.

---

## CHAPTER 15 · response 실습은 data leak·schema drift·payload 폭증을 각각 검증한다

`GET /users/:id`와 `GET /users`를 구현한다. 내부 User에는 passwordHash, tenantId, riskScore가 있지만 public response는 명시적 DTO만 반환한다. 목록은 summary projection과 cursor envelope를 사용하고, Date는 ISO instant로, large identifier는 string으로 serialize한다. response schema를 OpenAPI에 등록한다.

검증은 internal secret field 추가, nested relation 추가, undefined/null 차이, large integer, schema type 변경, 1000-item oversized page를 각각 주입한다. privacy leak은 field assertion으로, schema drift는 validator로, payload budget은 byte/serialization duration으로 확인한다. 같은 200 response를 반복하는 것으로 품질을 증명하지 않는다.

AI는 mapper boilerplate와 OpenAPI schema 초안을 생성할 수 있다. 사람은 어떤 field가 public contract인지, caller 권한에 따라 representation이 어떻게 달라지는지, 숫자·시간·money를 어떤 wire type으로 보낼지 결정한다. response 경계가 명시되면 내부 refactor가 client 계약을 우연히 깨뜨리는 위험이 크게 줄어든다.
