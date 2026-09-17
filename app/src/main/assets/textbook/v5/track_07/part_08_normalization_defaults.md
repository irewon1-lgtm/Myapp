# PART 08 · BLOCK 01 · LESSON 08 · normalization과 default로 의미를 바꾸지 않고 표현을 정리하기

validation이 `허용 가능한 값인가`를 판단한다면 normalization은 여러 표현을 하나의 내부 표현으로 맞추는 일이다. 두 작업을 섞으면 server가 잘못된 입력을 조용히 고치거나, 원래 의미가 다른 값을 같은 것으로 취급할 수 있다. 이메일 공백 제거, 전화번호 표기, Unicode, 날짜·시간, 화폐 단위처럼 정규화 규칙은 domain과 외부 표준에 따라 달라진다. 이 PART는 **표현을 정리하는 것과 의미를 변경하는 것을 구분하고 raw value를 보존해야 할 때를 판단**한다.

---

## CHAPTER 01 · normalization은 같은 의미라고 정의한 여러 표현을 canonical form으로 바꾼다

사용자가 `"  Kim  "`처럼 앞뒤 공백을 넣었을 때 display name policy가 공백을 의미 없는 것으로 본다면 trim해서 저장할 수 있다. 전화번호 `010-1234-5678`과 `01012345678`을 국제 E.164 representation으로 바꾸는 것도 정규화 예다. 그러나 이 변환이 안전하려면 domain에서 두 표현을 실제로 같은 값으로 정의해야 한다.

모든 문자열에 trim·lowercase를 자동 적용하면 code, password, case-sensitive identifier 같은 값의 의미를 바꿀 수 있다. normalization rule은 field별로 명시하고 generic middleware가 임의로 변형하지 않는다. raw input과 canonical value가 다른 경우 audit나 display를 위해 둘 다 보존할 수도 있다.

실습으로 `displayName`, `couponCode`, `password` 세 field에 서로 다른 normalization policy를 설계한다. 같은 helper를 무조건 재사용하지 않고 왜 어떤 field에는 trim만, 어떤 field에는 아무 변환도 하지 않는지 test 이름에 이유를 남긴다.

---

## CHAPTER 02 · trim은 사소해 보여도 empty validation과 순서가 연결된다

`"   "`은 raw length가 3이지만 trim 후 empty다. `minLength(1)`을 trim 전에 적용하면 공백뿐인 이름이 통과할 수 있다. 반대로 원문 공백이 의미 있는 field에서는 trim이 데이터 손실이다. validation과 normalization 순서를 contract로 고정해야 한다.

schema library가 `.trim().min(1)` 같은 transform chain을 제공할 수 있지만 transform된 값을 실제 service에 전달하는지, error message가 raw/normalized 어느 값 기준인지 확인한다. 같은 string을 여러 번 trim하는 비용은 작지만 어디서 정규화됐는지 추적이 흐려질 수 있다.

실습에서는 `""`, `" "`, `" kim "`을 대상으로 `validate→trim`과 `trim→validate` 결과를 비교한다. display name 정책에 맞는 순서를 선택하고 하나의 boundary에서만 transform하도록 한다.

---

## CHAPTER 03 · case normalization은 identifier 규칙을 확인한 뒤 적용한다

이메일 주소의 domain 부분은 case-insensitive하게 다뤄지지만 local part의 case semantics는 provider와 표준 규칙이 복잡하다. 현실의 많은 서비스가 전체 email을 lowercase로 canonicalize하지만 모든 환경에서 이론적으로 동일하다고 가정하기보다 서비스 account model과 provider behavior를 확인한다. username도 service가 case-insensitive login을 약속한다면 canonical key를 별도로 만들 수 있다.

display text까지 lowercase로 저장하면 사용자가 의도한 표기를 잃는다. 원본 display value와 lookup key를 분리해 `displayEmail`과 `normalizedEmail`처럼 관리할 수 있다. uniqueness constraint는 normalized key에 적용한다.

실습에서는 case-insensitive username을 설계하고 `Kim`, `kim`, `KIM`이 같은 account로 충돌하도록 normalized key를 만든다. 화면에는 최초 입력 표기를 유지할지 별도 정책을 정한다.

---

## CHAPTER 04 · Unicode normalization은 눈에 비슷한 문자열의 code point 표현을 다룬다

Unicode에는 화면에서 같거나 매우 비슷하게 보이지만 code point sequence가 다른 문자열이 있다. 예를 들어 일부 문자는 precomposed form과 combining sequence로 표현될 수 있다. binary equality만 사용하면 사용자가 같은 이름이라고 생각한 값이 다른 key로 저장될 수 있다.

NFC/NFKC 같은 Unicode normalization form은 목적과 영향이 다르다. compatibility normalization은 일부 문자의 구분을 합칠 수 있어 identifier에 적용할 때 security와 사용자 의미를 검토해야 한다. 국제화된 domain이나 username에는 별도 spoofing 위험도 있다.

실습에서 같은 화면 문자로 보이는 두 Unicode sequence를 만들어 `===` 비교와 `normalize("NFC")` 후 비교를 본다. 모든 field에 자동 적용하지 않고 search key, username, display text 각각의 policy를 분리한다.

---

## CHAPTER 05 · 전화번호는 punctuation 제거보다 국가·번호 체계 해석이 핵심이다

`010-1234-5678`에서 dash를 제거하는 것만으로 국제적으로 고유한 번호가 되지 않는다. country code와 national prefix, mobile/landline 규칙을 알아야 E.164 같은 canonical representation을 만들 수 있다. 사용자 locale을 추측해서 잘못된 country를 붙이면 다른 번호가 된다.

검증된 phone number library와 explicit country context를 사용하고, SMS 전송 가능 여부는 실제 provider verification과 별개로 본다. extension number나 short code처럼 일반 mobile number와 다른 category도 있다. 저장 key와 display formatting을 분리한다.

실습에서는 한국 번호와 국제 형식 입력을 동일 canonical value로 만들 수 있는 library behavior를 조사하고, country가 없는 ambiguous 입력을 어떻게 처리할지 contract를 정한다.

---

## CHAPTER 06 · 날짜 문자열은 calendar date와 instant를 구분해야 한다

`2026-09-18`은 달력 날짜이고 `2026-09-18T01:00:00Z`는 UTC instant를 표현한다. 생일처럼 time zone과 무관한 date를 instant로 바꾸면 지역에 따라 전날/다음날로 표시될 수 있다. 반대로 결제 발생 시각은 absolute instant가 필요하다.

parser가 permissive한 날짜 문자열을 허용하면 `09/10/26` 같은 값이 locale에 따라 다르게 해석될 수 있다. public contract에서 ISO-8601의 필요한 subset을 명시하고 strict parser를 사용한다. 내부 representation도 date-only와 instant type을 구분한다.

실습으로 생일 field와 createdAt field를 서로 다른 type/schema로 만든다. 한국/미국 time zone으로 표시했을 때 date-only가 변하지 않고 instant의 local display만 달라지는지 확인한다.

---

## CHAPTER 07 · time zone은 `지역 시간`을 future schedule로 저장할 때 별도 정보가 필요하다

`매일 서울 오전 9시`라는 schedule은 UTC instant 하나가 아니라 local time과 time-zone rule을 포함한다. daylight saving이 있는 지역에서는 future UTC offset이 계절에 따라 바뀔 수 있다. 단순히 현재 offset `+09:00` 또는 `-05:00`만 저장하면 미래 지역 규칙을 정확히 표현하지 못할 수 있다.

반면 이미 발생한 event timestamp는 UTC instant로 저장하고 표시할 때 zone을 적용하는 것이 일반적이다. schedule과 historical timestamp의 요구가 다르다. `server local timezone`에 암묵적으로 의존하면 environment 이동 시 결과가 바뀐다.

실습에서는 IANA zone ID를 사용해 future local time을 instant로 변환하는 예를 만들고 DST 전후를 테스트한다. server process timezone을 바꿔도 결과가 유지되도록 explicit zone을 사용한다.

---

## CHAPTER 08 · 금액 normalization은 currency와 minor unit을 잃지 않아야 한다

`10.50`이라는 숫자만 저장하면 USD 10.50인지 KRW 10.50인지 알 수 없다. currency code와 amount를 함께 다루고 floating-point 오차를 피하기 위해 minor unit integer 또는 decimal representation을 사용할 수 있다. 단 모든 currency가 소수 둘째 자리 규칙을 갖는 것은 아니므로 ISO currency metadata를 확인한다.

client가 `10.5`, `10.50`, `010.500`을 보내더라도 canonical amount가 같을 수 있다. 그러나 rounding policy는 세금·결제와 연결되므로 server가 임의 반올림하지 않는다. external payment provider의 amount unit과 정확히 맞춰 adapter에서 변환한다.

실습에서는 `Money { currency:"KRW", minor:1000 }` 같은 내부 representation을 만들고 다른 input string을 parser에서 동일 value로 바꾸는 test를 작성한다. invalid precision을 silent round하지 않고 reject하는 policy도 비교한다.

---

## CHAPTER 09 · identifier normalization은 보안과 uniqueness를 함께 고려한다

username의 공백·case·Unicode를 normalize하면 uniqueness가 단순해질 수 있지만 서로 다른 표시가 같은 account key가 되는 collision을 만든다. 사용자가 account를 만들기 전에 normalized key를 보여 주거나 clear rule을 제공해야 한다. confusable character를 어떻게 처리할지도 서비스 threat model에 따라 달라진다.

외부 provider ID는 우리가 규칙을 정할 수 없는 경우가 많다. provider가 case-sensitive라고 문서화한 ID를 lowercase하면 다른 resource를 하나로 합칠 수 있다. 내부 ID와 external ID의 normalization policy를 분리한다.

실습에서 local username과 providerCustomerId 두 field에 다른 policy를 적용하고, 같은 helper 사용을 막는 type wrapper를 만든다. test는 provider ID의 case가 보존되는지 확인한다.

---

## CHAPTER 10 · default는 값이 없을 때 선택하는 정책이며 falsy coercion과 다르다

`const limit = raw || 20`은 0, empty string, false까지 default로 바꾼다. `raw ?? 20`은 null/undefined에만 적용한다. 어떤 값이 valid한지에 따라 두 표현의 의미가 크게 달라진다. default 적용 전에 validation을 할지, missing일 때만 default를 만들지 명시한다.

API default는 public contract가 된다. default page size를 20에서 100으로 바꾸면 response 크기와 client behavior가 변할 수 있다. server 내부 constant 변경이라고만 생각하면 안 된다. OpenAPI schema에도 default 의미를 정확히 표현한다.

실습에서 `limit` query에 missing, `0`, `1`, `""`를 보내 parser/validation/default 단계 결과를 표로 만든다. `0`을 특별 의미로 사용할지 invalid로 거부할지도 contract에 명시한다.

---

## CHAPTER 11 · derived default는 다른 field와 runtime context에 의존할 수 있다

배송 국가가 없을 때 authenticated user profile의 country를 default로 사용할 수 있지만, user가 request를 재시도하는 사이 profile이 바뀌면 같은 raw request가 다른 결과를 낼 수 있다. idempotent command를 만들 때 derived default의 시점이 중요하다. business-critical 값은 client가 명시하도록 요구하는 편이 안전할 수 있다.

현재 시간 `now`를 default로 넣는 것도 hidden dependency다. event occurrence time과 server receipt time이 다른 의미일 수 있다. 테스트에서는 clock을 주입해 deterministic result를 만든다.

실습으로 `currency`를 tenant setting에서 default하는 주문 command를 설계하고, idempotency record에 normalized command snapshot을 저장한다. retry 시 setting이 바뀌어도 원래 command 의미가 유지되는지 확인한다.

---

## CHAPTER 12 · 중복 제거는 collection이 set 의미일 때만 정규화로 안전하다

검색 tag filter에서 `["red","red","blue"]`를 `["red","blue"]`로 바꿔도 의미가 같을 수 있다. 반면 주문 item list에서 같은 product가 두 번 등장하는 것이 수량을 합치는 의미인지 별도 line item 의미인지 domain에 따라 다르다. generic `uniq()`를 모든 array에 적용하면 business 의미를 변경한다.

순서도 마찬가지다. permission set은 order가 중요하지 않을 수 있지만 playlist는 순서가 핵심이다. canonical serialization이나 signature를 위해 sorting하려면 order-insensitive field에만 적용한다.

실습에서는 tag set과 cart item list를 비교해 dedupe policy를 다르게 만든다. property test로 tag permutation은 같은 normalized value를 만들지만 cart permutation은 순서를 보존하도록 검증한다.

---

## CHAPTER 13 · canonicalization은 signature·cache key·idempotency fingerprint에도 영향을 준다

같은 의미 JSON이라도 object key order와 whitespace가 다르면 raw bytes hash가 달라진다. idempotency key와 request fingerprint를 비교할 때 raw body hash만 사용하면 semantic-equivalent request를 다르게 볼 수 있다. 반대로 임의 canonicalization은 duplicate key와 number representation 같은 edge case에서 위험할 수 있다.

cryptographic signature protocol은 provider가 지정한 canonicalization 규칙을 정확히 따라야 한다. 우리가 임의로 parse 후 reserialize하면 signature verification이 깨질 수 있다. webhook 서명이 raw body를 대상으로 한다면 parser 전 bytes를 보존해야 한다.

실습에서 동일 object를 다른 key order로 stringify해 hash가 달라지는 것을 본다. application-level idempotency fingerprint는 validated normalized command의 stable serialization을 사용하고, webhook signature는 provider raw-body rule을 따르는 식으로 목적을 분리한다.

---

## CHAPTER 14 · raw value 보존은 audit·support·migration에 유용하지만 개인정보 비용을 만든다

normalization 후 원문을 버리면 사용자가 입력한 정확한 spelling을 복원하기 어렵다. display purpose나 migration 분석을 위해 raw와 normalized를 둘 다 저장할 수 있다. 하지만 password, token, 민감한 식별정보까지 raw로 보존하면 breach impact가 커진다. 보존 필요성과 privacy를 field별로 판단한다.

log에도 raw input을 무조건 남기지 않는다. normalization bug를 조사하기 위해 sample이 필요하면 production data를 광범위하게 기록하기보다 synthetic fixture, redacted capture, explicit debug mode를 사용한다. data minimization은 observability와 함께 설계한다.

연습으로 email display/or normalized lookup, phone display/canonical, password raw/no-storage, coupon exact 값을 표로 만들고 각 raw 보존 여부와 retention을 결정한다.

---

## CHAPTER 15 · normalization 실습은 `변환 전후 의미가 같은가`를 각 field마다 증명한다

회원가입과 주문 입력에 displayName, username, phone, birthDate, deliveryAt, money, couponCode를 넣고 field별 normalization function을 작성한다. validation을 통과한 raw value가 canonical form으로 바뀌며, transformation이 없는 field도 명시한다. 결과 object에는 어떤 값이 user-visible original이고 어떤 값이 lookup/storage key인지 type으로 구분한다.

테스트는 whitespace, Unicode composed/decomposed, case variation, international phone, date-only/instant, invalid currency precision, missing default, duplicate tag를 포함한다. expected normalized value뿐 아니라 `변환하면 안 되는 값이 그대로 유지되는지`도 검증한다. password와 external provider ID가 accidental lowercase/trim되지 않는 test를 둔다.

AI는 locale/library API와 Unicode 예제 조사에 유용하지만, 어떤 표현을 같은 의미로 간주할지는 제품의 identity·search·billing 계약을 아는 사람이 결정한다. normalization을 잘못하면 invalid data를 거부하는 validation보다 더 조용하게 데이터 의미를 바꾸므로 **변환하지 않는 선택도 의도적인 설계**다.
