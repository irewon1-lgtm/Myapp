# PART 07 · BLOCK 01 · LESSON 07 · runtime validation으로 외부 값을 신뢰 가능한 application input으로 좁히기

TypeScript type이나 IDE 자동완성은 개발 중 실수를 줄이지만 network에서 들어온 값의 진실을 확인해 주지는 않는다. 외부 client는 compiler를 통과하지 않고 JSON·query·path를 직접 만들 수 있다. backend는 이 값을 처음에는 `unknown`으로 보고, 구조·타입·범위·길이·상호 관계를 검사한 뒤에만 좁은 application type으로 넘겨야 한다. 이 PART는 validation을 `if문 몇 개`가 아니라 **trust boundary에서 가능한 상태 공간을 줄이는 과정**으로 다룬다.

---

## CHAPTER 01 · 외부 입력은 type annotation보다 먼저 unknown으로 취급한다

`interface CreateUserBody { email: string; age: number }`를 선언해도 runtime에서 `req.body`가 정말 그 형태라는 증거는 생기지 않는다. `const body = req.body as CreateUserBody`는 compiler에게 믿으라고 지시할 뿐 실제 값을 검사하지 않는다. 공격자나 오래된 client는 예상하지 않은 field와 type을 자유롭게 보낼 수 있다.

그래서 boundary에서는 raw input을 `unknown`으로 생각하고 parser/schema가 성공한 값만 trusted application input으로 만든다. 이 사고방식을 쓰면 `any`와 무분별한 type assertion이 줄어든다. 내부 함수는 이미 검증된 type을 받아 더 단순한 invariant를 가질 수 있다.

실습에서 `age:number` interface만 선언한 endpoint에 `{"age":"twenty"}`를 보내 runtime behavior를 본다. 그 다음 validator를 추가하고 service spy가 호출되지 않는지 확인한다. compile success와 runtime trust를 별개 증거로 기록한다.

---

## CHAPTER 02 · schema는 허용되는 데이터 형태를 실행 가능한 계약으로 표현한다

validation library는 object field, primitive type, required 여부, length, enum, nested structure 같은 규칙을 선언한다. 도구는 Zod, JSON Schema validator, framework schema 등 여러 가지가 있지만 목적은 같다. 외부 value가 contract를 만족하는지 runtime에서 검사하고 성공하면 좁은 representation을 얻는다.

schema를 route 근처에 두면 OpenAPI 생성이나 client type 생성과 연결할 수 있지만 code-first 자동생성이 실제 runtime rule과 정확히 같은지 확인해야 한다. schema가 documentation용으로만 있고 request pipeline에서 실행되지 않으면 보안 경계가 되지 않는다.

실습에서는 `CreateProduct` schema에 name, price, stock을 선언하고 valid/invalid table을 먼저 작성한다. schema code를 만든 뒤 table-driven test로 모든 case를 실행해 문서와 runtime behavior가 일치하는지 확인한다.

---

## CHAPTER 03 · required·optional·nullable은 서로 다른 상태 공간을 만든다

required field는 key가 존재해야 하고, optional은 key가 없어도 되며, nullable은 값으로 null을 허용한다. 세 개념을 섞으면 update semantics가 흔들린다. `nickname?: string | null`은 key 없음, string, null 세 상태를 가질 수 있으며 각각 `변경 없음`, `새 값`, `삭제` 의미를 줄 수 있다.

create request와 patch request에 같은 schema를 재사용하면 필요한 field 조건이 다를 수 있다. create에서는 name이 required지만 patch에서는 보내지 않은 field를 유지해야 하므로 optional이다. `Partial<CreateUser>` 같은 compile-time type만으로 runtime unknown-field와 constraint가 유지되는지도 확인해야 한다.

실습에서는 nickname을 omitted, null, empty string, normal string으로 보내고 API contract를 표로 만든다. validator 결과와 update command 생성 결과가 같은 의미를 유지하는지 test한다.

---

## CHAPTER 04 · number validation은 type 확인 뒤 finite·integer·range를 추가로 본다

JavaScript number에는 `NaN`, `Infinity`, 소수, 매우 큰 값이 있다. `typeof value === "number"`만으로 quantity나 age 같은 domain rule을 충족하지 않는다. quantity가 positive integer라면 `Number.isInteger`와 min/max를 함께 적용한다. monetary amount는 안전한 integer 범위나 decimal representation을 따로 설계한다.

범위는 business rule이면서 resource budget일 수도 있다. `pageSize` 최대 100은 DB와 response 비용을 제한하고, `retryCount` 최대값은 폭주를 막는다. arbitrary precision이 필요한 값에 JavaScript number를 사용하면 validation이 통과해도 정밀도 손실이 생길 수 있다.

실습에서 0, 1, 1.5, -1, NaN을 직접 함수에 넣고 JSON을 통해 전달 가능한 값과 아닌 값을 구분한다. `Number.isFinite`, `Number.isSafeInteger`가 어떤 문제를 막는지 작은 assertion으로 확인한다.

---

## CHAPTER 05 · string validation은 길이·형식·문자 체계를 분리해서 설계한다

문자열이라는 사실만 확인하면 빈 문자열, 공백뿐인 값, 수백 MB 문자열, control character가 모두 통과할 수 있다. field 의미에 맞춰 min/max length, trim policy, 허용 character class를 정한다. 하지만 Unicode에서 JavaScript `length`는 사용자가 보는 글자 수와 정확히 같지 않을 수 있다. surrogate pair와 combining sequence가 존재하기 때문이다.

username처럼 엄격한 ASCII subset을 허용할 수 있는 값과 display name처럼 다양한 Unicode를 허용해야 하는 값을 구분한다. 모든 문자열에 동일 regex를 적용하지 않는다. 정규식이 지나치게 복잡하면 pathological input에서 CPU를 많이 쓰는 ReDoS 위험도 생길 수 있다.

실습에서 ASCII, 한글, emoji, combining character를 사용해 `length`와 실제 화면 인지 차이를 관찰한다. 정책이 byte, code unit, grapheme 중 무엇을 제한하려는지 명시한다.

---

## CHAPTER 06 · format validation은 `문법적으로 그럴듯함`과 `실제로 존재함`을 구분한다

email, URL, UUID, timestamp 같은 값은 format rule을 가질 수 있다. email regex가 문법을 대략 검사해도 mailbox가 실제 존재하는지는 알 수 없고, URL parser가 성공해도 destination이 안전하다는 뜻은 아니다. UUID format이 맞아도 그 resource에 접근할 권한은 별도다.

validation 단계에서 할 수 있는 것은 application이 받아들일 syntactic contract를 좁히는 일이다. 존재 확인, ownership, external verification은 다른 layer로 넘긴다. 모든 것을 하나의 schema refinement에 넣으면 network I/O와 side effect가 parser 단계에 숨어 testing과 retry가 복잡해진다.

실습으로 UUID parser와 repository existence check를 분리한다. malformed UUID는 repository call이 0회여야 하고, valid-but-missing UUID는 repository 조회 후 NotFound가 되어야 한다. failure 위치를 metric에서도 분리한다.

---

## CHAPTER 07 · unknown field 정책은 silent ignore와 strict reject 사이의 호환성 선택이다

client가 schema에 없는 `role` field를 보냈을 때 validator가 제거하고 계속할 수도 있고 error로 거부할 수도 있다. strict reject는 typo와 mass-assignment 시도를 빨리 드러내지만 additive client evolution과 일부 proxy metadata에는 불편할 수 있다. public API가 어떤 방향으로 evolve할지 고려해 정책을 정한다.

write command에서는 허용 field allowlist가 중요하다. ORM entity 전체와 request body를 그대로 merge하면 `isAdmin`, `ownerId`, internal status 같은 field가 새로 추가될 때 공격 surface가 생긴다. strict schema와 explicit mapping을 함께 사용하면 위험을 줄인다.

실습에서 `displayName`만 허용하는 profile update에 `role`, `createdAt`, typo `displayNmae`를 보내 strict/strip 두 모드를 비교한다. 어떤 mode를 선택했는지 API documentation과 error response가 일치하게 한다.

---

## CHAPTER 08 · coercion은 편리하지만 client contract 오류를 조용히 숨길 수 있다

query에서 모든 값이 문자열로 시작하므로 `limit="20"`을 number 20으로 변환하는 것은 자연스러울 수 있다. 반면 JSON body에서 number를 요구했는데 string `"20"`까지 자동 허용하면 client가 잘못된 type을 보내도 오래 발견되지 않는다. coercion 범위를 channel별로 다르게 정할 수 있다.

JavaScript의 일반 coercion에는 예상하기 어려운 값이 많다. `Number("")`가 0이 되고, Boolean("false")가 true가 되는 식이다. schema library의 명시적 parser를 사용하고 허용 syntax를 test한다. 날짜 parsing도 runtime마다 permissive behavior가 있을 수 있어 ISO format을 좁게 정의하는 편이 낫다.

실습에서는 query boolean parser를 직접 만들어 `true|false`만 허용하고 `1`, `yes`, 빈 문자열을 거부한다. generic `Boolean(raw)`와 결과를 비교해 implicit coercion이 contract를 어떻게 깨뜨리는지 본다.

---

## CHAPTER 09 · array와 nested object에는 원소 수·깊이·각 원소 규칙이 모두 필요하다

`items`가 array라는 사실만 확인하면 백만 개 원소를 보낼 수 있다. 주문 item은 최소 1개, 최대 100개처럼 collection size를 제한하고 각 item의 productId, quantity를 검증한다. nested comment tree처럼 depth가 늘어나는 구조는 최대 깊이를 두지 않으면 recursive processing 비용이 커질 수 있다.

duplicate element가 허용되는지도 domain rule이다. 같은 productId가 여러 번 나타나면 합칠지, 오류로 거부할지 정한다. 순서가 의미 있는 list인지 set 의미인지도 serialization contract에 반영한다. collection validation은 `array` 한 단어보다 많은 상태를 줄인다.

실습에서 0, 1, 100, 101개 item을 생성하고 validation time을 측정한다. 같은 product ID 중복 case도 넣어 schema-level uniqueness와 service-level stock rule을 분리한다.

---

## CHAPTER 10 · union은 가능한 variant를 discriminator로 명확히 구분할 수 있다

결제 method가 card와 bank transfer 두 종류라면 모든 field를 optional로 한 거대한 object보다 `type` discriminator를 사용한 union이 invalid combination을 줄인다.

```text
{ type:"card", cardToken:"..." }
{ type:"bank", bankAccountId:"..." }
```

card variant에 bank-only field가 들어오거나 required cardToken이 빠지면 validation에서 거부한다. TypeScript discriminated union과 runtime schema를 같은 구조로 맞추면 application code에서 exhaustive switch를 사용하기 쉽다.

새 variant를 추가할 때 오래된 consumer가 unknown type을 어떻게 처리할지도 evolution issue다. server input에서는 unknown variant를 4xx로 거부할 수 있고, server output consumer는 fallback을 준비할 수 있다.

실습으로 payment union을 만들고 valid card, valid bank, missing discriminator, mixed fields, unknown type을 test한다. handler 안의 switch가 default에서 silent success하지 않도록 exhaustive assertion을 둔다.

---

## CHAPTER 11 · cross-field rule은 한 field만 봐서는 판단할 수 없는 invariant를 검사한다

예약 request에서 `endAt`은 `startAt`보다 뒤여야 하고, discount의 `min <= max`가 필요할 수 있다. shipping address가 특정 국가일 때만 province field가 required일 수도 있다. 이런 규칙은 field-level type 검사 뒤 object-level refinement에서 다룬다.

그러나 DB 현재 상태나 외부 API 결과가 필요한 rule까지 schema validator에 넣으면 boundary가 흐려진다. `startAt < endAt`은 pure input relation이지만 `coupon이 아직 사용 가능한가`는 clock과 database state가 필요한 application rule이다. validator와 service invariant를 분리한다.

실습에서 date range schema에 object-level rule을 추가하고 equal/start-after-end case를 거부한다. 이어 fake DB를 호출해야 하는 coupon availability는 service test로 따로 둔다. test 실행 속도와 failure taxonomy가 어떻게 선명해지는지 비교한다.

---

## CHAPTER 12 · uniqueness와 existence는 request schema가 아니라 shared state와의 관계다

회원가입 email이 형식상 valid해도 DB에 이미 존재할 수 있다. 이를 `schema validation`이라고 한 덩어리로 부르면 concurrency race를 놓치기 쉽다. 사전 existence check는 친절한 error를 줄 수 있지만 두 request가 동시에 check한 뒤 insert하면 중복될 수 있다. 최종 uniqueness는 database constraint 같은 atomic mechanism으로 보강한다.

resource existence도 같은 맥락이다. valid orderId 문자열을 받은 뒤 repository에서 row가 없으면 NotFound이고, row는 있지만 다른 user 소유면 authorization failure다. input schema는 이 상태를 알 수 없다. boundary별 failure를 분리해야 올바른 status와 retry policy를 선택할 수 있다.

실습에서 email format validation, `findExisting`, unique constraint fake를 단계별로 두고 concurrent create 시나리오를 작성한다. `먼저 조회했으니 안전`이라는 가정이 깨지는 timeline을 직접 그린다.

---

## CHAPTER 13 · validation schema도 API version과 함께 진화한다

오늘 optional인 field를 내일 required로 바꾸면 오래된 client가 깨질 수 있다. enum 값 제거, max length 축소, number range 축소도 breaking change가 될 수 있다. schema diff는 type 이름이 같아도 consumer compatibility를 분석해야 한다.

request에서는 server가 더 넓은 값을 받아들이다가 내부에서 normalize할 수 있지만 business rule이 바뀌면 거부가 필요할 수도 있다. deprecation 기간에 old/new field를 둘 다 받아 한 내부 representation으로 매핑하고 usage metric을 수집한 뒤 old field를 제거하는 방식이 있다.

실습에서 `fullName`을 `givenName/familyName`으로 migration하는 두 schema 버전을 설계한다. old client fixture와 new client fixture가 같은 internal command로 변환되는지 test하고, ambiguous하게 둘 다 보냈을 때 precedence를 정한다.

---

## CHAPTER 14 · validation error는 사람이 고칠 field path와 machine-readable code를 함께 제공할 수 있다

`invalid input` 한 문장만 반환하면 client UI가 어느 field를 강조할지 알기 어렵다. `path`, `code`, `message` 같은 구조화 error를 제공할 수 있다. 그러나 internal schema library 이름과 stack trace를 그대로 노출할 필요는 없다. public error code는 API contract로 관리한다.

배열에서는 `items[3].quantity`처럼 정확한 path가 유용하다. localization이 필요한 message와 program logic에 쓰는 stable code를 분리하면 client가 한국어 문구를 parsing하지 않아도 된다. 너무 세밀한 internal rule detail이 보안 정보를 노출하는 경우도 있으므로 authentication 관련 error는 일반화할 수 있다.

실습으로 여러 field가 동시에 잘못된 request를 보내 `errors[]`를 반환한다. error ordering을 stable하게 할지, 첫 오류만 반환할지 contract를 정하고 UI test fixture와 맞춘다.

---

## CHAPTER 15 · validation 실습은 type보다 상태 공간 축소를 증명한다

`POST /orders` input에 customer reference, items, delivery option, optional coupon을 정의한다. raw body는 unknown으로 받고 strict schema가 성공한 결과만 `CreateOrderInput`으로 변환한다. items는 1~100개, quantity는 positive integer, delivery variant는 discriminator union, coupon은 정해진 pattern을 갖게 한다. cross-field rule로 pickup이면 address를 금지하고 delivery면 address를 요구한다.

테스트에는 wrong primitive type, unknown field, empty array, 101 items, duplicate product, mixed union fields, malformed coupon, omitted/null 차이, query coercion case를 넣는다. service spy를 사용해 invalid input에서 service가 한 번도 호출되지 않았음을 검증한다. valid input은 validated type만 service에 전달되는지 확인한다.

AI는 schema library 문법과 test case 초안을 빠르게 만들 수 있다. 사람은 어느 상태를 허용하는지, coercion을 어디까지 허용할지, shared-state rule을 schema에 숨기지 않을지 결정한다. validation의 성과는 코드 줄 수가 아니라 **뒤 layer가 불가능한 입력 상태를 걱정하지 않아도 되는 정도**로 판단한다.
