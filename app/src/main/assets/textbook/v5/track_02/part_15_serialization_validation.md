# PART 15 · Serialization·schema·validation — 외부 표현을 내부 의미로 안전하게 번역하기

프로그램은 object를 그대로 network나 file에 보낼 수 없다. 외부 경계를 넘을 때는 bytes 또는 text format으로 encode하고, 반대쪽에서 decode해 다시 의미 있는 data model을 만든다. 이 과정에서 format 문법, schema, version, validation, canonicalization을 한 덩어리로 처리하면 실패 위치가 흐려진다. 안전한 경계는 **parse 가능한가, schema에 맞는가, domain에서 유효한가**를 단계별로 나눈다.

---

## CHAPTER 01 · serialization은 object memory를 복사하는 것이 아니라 외부 계약으로 encode한다

### 시작 전 용어집

#### 1. serialization

- **뜻:** Serialization은 공유하기로 정한 field와 의미를 format에 맞는 representation으로 변환한다.
- **왜 중요한가:** 따라서 무엇을 포함하고 제외하는지가 public contract다.
- **예시:** Serialization은 공유하기로 정한 field와 의미를 format에 맞는 representation으로 …

#### 2. object memory

- **뜻:** Python object는 reference, class implementation, runtime state를 포함하지만 다른 process나 언어가 이 내부 구조를 그대로 이해할 수 있는 것은 아니다.
- **왜 중요한가:** User object를 JSON object로 만든다고 할 때 password hash, internal flag, cache metadata까지 `__dict__` 전체를 자동으로 내보내면 정보 노출과 coupling이 생긴다.
- **예시:** Python object는 reference, class implementation, runtime state를 포함하지만 …

#### 3. 계약

- **뜻:** Round-trip requirement가 있는 값은 명시적 encoder/decoder 계약을 갖는다.
- **왜 중요한가:** Security 관점에서도 arbitrary object deserialization은 위험할 수 있다.
- **예시:** Round-trip requirement가 있는 값은 명시적 encoder/decoder 계약을 갖는다.

#### 4. encode

- **뜻:** Output schema에 필요한 field를 명시적으로 선택한다.
- **왜 중요한가:** Domain object와 wire DTO를 분리하면 내부 field 이름을 리팩터링해도 외부 API version을 독립적으로 유지할 수 있다.
- **예시:** Output schema에 필요한 field를 명시적으로 선택한다.

Serialization은 lossy할 수도 있다. Datetime timezone, decimal precision, enum unknown value, binary data를 format이 어떻게 표현하는지 정해야 한다. `str(obj)`를 저장했다가 다시 parse하면 원래 의미를 복구할 수 있다는 보장은 없다. 

 Format에 따라 decode 과정이 object construction이나 code execution과 연결될 수 있으므로 untrusted input에는 data-only format과 검증된 schema를 선호한다. “편하게 객체가 복원된다”는 기능은 신뢰 경계에서는 공격면이 될 수 있다.

---

## CHAPTER 02 · syntax parse와 schema validation은 서로 다른 실패다

### 시작 전 용어집

#### 1. syntax parse

- **뜻:** `{"age":"hello"}`는 JSON 문법으로는 완전히 유효할 수 있지만 `age`가 integer여야 하는 application schema에는 맞지 않는다.
- **왜 중요한가:** Parser 성공을 데이터 유효성으로 착각하면 내부 코드가 잘못된 타입과 누락 field를 처리해야 한다.
- **예시:** `start_at <= end_at`, 주문 상태에 따라 필요한 field가 …

#### 2. schema validation

- **뜻:** Schema validation을 통과한 data를 typed internal model로 변환하면 이후 함수가 같은 type check를 반복하지 않아도 된다.
- **왜 중요한가:** Trust boundary에서 강하게 검증하고 내부에서는 더 좁은 상태 공간을 유지하는 전략이다.
- **예시:** `start_at <= end_at`, 주문 상태에 따라 필요한 field가 …

#### 3. 실패

- **뜻:** 첫 단계에서 format syntax를 읽고 두 번째 단계에서 구조와 타입을 확인한다.
- **왜 중요한가:** Schema는 required field, optional field, numeric range, enum, nested structure, unknown field policy를 표현할 수 있다.
- **예시:** `start_at <= end_at`, 주문 상태에 따라 필요한 field가 …

#### 4. 타입

- **뜻:** 그러나 모든 업무 규칙을 schema 하나에 넣을 수 있는 것은 아니다.
- **왜 중요한가:** `start_at <= end_at`, 주문 상태에 따라 필요한 field가 달라지는 조건, DB에 이미 존재하는 ID인지 여부는 domain validation이나 service validation이 필요할 수 있다.
- **예시:** quantity: must be >= 1`처럼 위치가 있으면 사용자와 …

Validation error는 가능한 한 field path와 reason을 구조화한다. `invalid data` 한 문자열보다 `items[3].quantity: must be >= 1`처럼 위치가 있으면 사용자와 개발자가 수정하기 쉽다. 다만 외부 공격자에게 내부 schema와 민감 state를 과도하게 노출하지 않도록 API error surface를 설계한다.

 

---

## CHAPTER 03 · JSON의 간단한 표면 아래 숫자와 duplicate key 정책이 있다

### 시작 전 용어집

#### 1. JSON

- **뜻:** JSON은 널리 사용되지만 모든 구현이 숫자와 object member를 완전히 같은 방식으로 다루는 것은 아니다.
- **왜 중요한가:** JSON number는 format 수준의 문법이고 각 언어에서 int, float, decimal로 어떤 타입을 선택할지는 decoder policy다.
- **예시:** JSON은 널리 사용되지만 모든 구현이 숫자와 object member를 …

#### 2. duplicate key

- **뜻:** 매우 큰 integer를 JavaScript number와 주고받을 때 precision이 손실될 수 있으므로 ID나 금액 representation을 검토해야 한다.
- **왜 중요한가:** Object에 같은 member name이 반복될 때 parser가 first wins, last wins, error 중 어떤 behavior를 갖는지 차이가 있을 수 있다.
- **예시:** 매우 큰 integer를 JavaScript number와 주고받을 때 precision이 …

#### 3. decimal

- **뜻:** Security filter와 application parser가 서로 다른 duplicate policy를 사용하면 한 payload를 다르게 해석하는 parser differential이 생길 수 있다.
- **왜 중요한가:** 신뢰 경계에서는 ambiguous input을 거부하는 정책이 더 안전할 수 있다.
- **예시:** Security filter와 application parser가 서로 다른 duplicate policy를 …

#### 4. 타입

- **뜻:** `null`, field absence, empty string도 서로 다른 의미일 수 있다.
- **왜 중요한가:** Patch API에서 field가 없다는 것은 “변경하지 않음”, explicit null은 “값 삭제”를 뜻할 수 있다.
- **예시:** `null`, field absence, empty string도 서로 다른 의미일 …

Decoder가 둘을 같은 None으로 합치면 operation semantics를 잃는다.

JSON을 사람이 읽기 쉬운 format이라고 해서 canonical byte representation이 자동으로 정해지는 것도 아니다. Key order와 whitespace, number 표기가 달라도 semantic value는 같을 수 있다. Raw JSON bytes를 signature/hash input으로 사용하려면 별도의 canonicalization 규칙이 필요하다.

---

## CHAPTER 04 · CSV는 단순 `split(',')`로 안전하게 파싱할 수 없는 구조다

### 시작 전 용어집

#### 1. CSV

- **뜻:** CSV처럼 단순해 보이는 format도 quoted field 안에 comma, newline, quote escaping이 들어갈 수 있다.
- **왜 중요한가:** 한 줄을 `split(',')`하면 정상 데이터가 여러 column으로 잘못 분리될 수 있다.
- **예시:** 외부로 export하는 값이 `=`, `+`, `-`, `@` 등으로 …

#### 2. split

- **뜻:** 표준 library parser를 사용하고 dialect, delimiter, quote, encoding을 명시한다.
- **왜 중요한가:** Header가 column identity를 제공한다면 순서만 믿기보다 이름 mapping을 사용할 수 있지만 duplicate header와 누락 header 정책이 필요하다.
- **예시:** 외부로 export하는 값이 `=`, `+`, `-`, `@` 등으로 …

#### 3. API

- **뜻:** Header 없는 legacy file은 schema version이나 source metadata로 column meaning을 고정해야 한다.
- **왜 중요한가:** Spreadsheet에서 생성된 CSV를 다시 spreadsheet가 열 때 formula-like prefix가 실행될 수 있는 CSV injection 문제도 있다.
- **예시:** 외부로 export하는 값이 `=`, `+`, `-`, `@` 등으로 …

#### 4. identity

- **뜻:** 외부로 export하는 값이 `=`, `+`, `-`, `@` 등으로 시작할 때 대상 application의 해석 규칙을 고려한다.
- **왜 중요한가:** Text 파일 자체가 code는 아니어도 consumer가 특별한 의미를 부여할 수 있다.
- **예시:** 외부로 export하는 값이 `=`, `+`, `-`, `@` 등으로 …

대용량 CSV는 row iterator로 streaming parse할 수 있다. 하지만 한 row의 parse error를 전체 실패로 볼지 quarantine하고 계속할지 정책을 정한다. 누락 row를 조용히 버리는 것은 데이터 pipeline에서는 심각한 정확성 문제가 될 수 있다.

---

## CHAPTER 05 · schema evolution은 새 field 추가보다 old/new reader 조합을 검증한다

### 시작 전 용어집

#### 1. schema evolution

- **뜻:** 저장 파일이나 API가 version을 거치면 producer와 consumer가 동시에 업데이트되지 않을 수 있다.
- **왜 중요한가:** New writer가 추가한 field를 old reader가 어떻게 처리하는지, old writer가 보내지 않는 field를 new reader가 어떤 default로 해석하는지 확인해야 한다.
- **예시:** 저장 파일이나 API가 version을 거치면 producer와 consumer가 동시에 …

#### 2. field

- **뜻:** Field를 삭제했다고 identifier나 field number를 새 의미로 재사용하면 과거 데이터가 새로운 의미로 해석될 수 있다.
- **왜 중요한가:** Schema 기반 binary format에서는 field identity를 안정적으로 유지하고 제거된 slot을 reserve하는 규칙이 중요한 이유다.
- **예시:** Field를 삭제했다고 identifier나 field number를 새 의미로 재사용하면 …

#### 3. old

- **뜻:** Compatibility test는 old fixture를 새 decoder로 읽고, 필요한 경우 new payload를 old reader simulator로 읽어 본다.
- **왜 중요한가:** 문서상 “호환됨”보다 실제 version matrix를 자동 검증하는 편이 강하다.
- **예시:** Compatibility test는 old fixture를 새 decoder로 읽고, 필요한 …

#### 4. new reader

- **뜻:** Compatibility는 한 방향이 아니라 배포 순서와 데이터 수명에 따라 여러 조합을 가진다.
- **왜 중요한가:** Text JSON에서도 field name 의미를 갑자기 바꾸면 같은 문제가 생긴다.
- **예시:** Compatibility는 한 방향이 아니라 배포 순서와 데이터 수명에 …

Default value 추가도 의미 변화다. 과거에 field absence가 “알 수 없음”이었는데 새 version에서 false를 default로 정하면 historical data가 새로운 사실을 가진 것처럼 보일 수 있다. Migration에서 unknown과 explicit false를 구분할 필요가 있다.

 

---

## CHAPTER 06 · domain validation은 field 하나보다 값들의 관계를 본다

### 시작 전 용어집

#### 1. domain validation

- **뜻:** Schema가 `start_at`과 `end_at`을 각각 valid datetime으로 확인해도 `start_at > end_at`인 조합은 업무적으로 잘못될 수 있다.
- **왜 중요한가:** `discount`와 `subtotal`이 모두 non-negative라도 discount가 subtotal보다 큰 것이 금지될 수 있다.
- **예시:** 예를 들어 “email 형식이 맞음”은 value object가, “이미 …

#### 2. field

- **뜻:** 이런 규칙은 object-level invariant다.
- **왜 중요한가:** Validation을 여러 계층에서 무작정 반복하기보다 책임을 나눈다.
- **예시:** 예를 들어 “email 형식이 맞음”은 value object가, “이미 …

#### 3. datetime

- **뜻:** Parser는 syntax, schema layer는 구조와 기본 type, domain constructor는 내부 invariant, service layer는 외부 state를 요구하는 rule을 담당할 수 있다.
- **왜 중요한가:** 예를 들어 “email 형식이 맞음”은 value object가, “이미 가입된 email이 아님”은 repository를 조회하는 service가 검사한다.
- **예시:** Parser는 syntax, schema layer는 구조와 기본 type, domain …

#### 4. 반복

- **뜻:** Validation 순서도 user experience와 비용에 영향을 준다.
- **왜 중요한가:** Local format이 명백히 틀린 input에 DB query를 먼저 할 필요는 없다.
- **예시:** Validation 순서도 user experience와 비용에 영향을 준다.

값 자체의 cheap validation을 통과한 뒤 외부 I/O가 필요한 검사를 수행한다. 하지만 security authorization을 validation 뒤로 너무 늦춰 존재 여부를 노출하는 식의 side channel이 생기지 않도록 경계별 threat model을 본다.

정상화와 validation 순서도 고정한다. Phone number에서 공백을 제거하고 비교할지, username의 case policy를 어떻게 할지 producer와 consumer가 같은 canonical form을 사용해야 한다. 서로 다른 정규화가 같은 raw input을 다른 identity로 만들 수 있다.

---

## CHAPTER 07 · canonicalization은 equality, cache key, signature의 기반을 바꾼다

### 시작 전 용어집

#### 1. canonicalization

- **뜻:** Canonicalization을 한 번의 범용 “sanitize” 함수로 해결하지 않는다.
- **왜 중요한가:** Password는 normalization을 함부로 적용하면 기존 credential과 호환성이 깨질 수 있고, search query는 사용자 편의를 위해 case folding과 Unicode normalization을 적용할 수 있다.
- **예시:** Canonicalization을 한 번의 범용 “sanitize” 함수로 해결하지 않는다.

#### 2. equality

- **뜻:** 서로 다른 representation이 같은 semantic value를 뜻할 수 있다.
- **왜 중요한가:** Unicode normalization, URL percent encoding, JSON whitespace, path separator가 대표적이다.
- **예시:** 서로 다른 representation이 같은 semantic value를 뜻할 수 …

#### 3. cache

- **뜻:** 어떤 계층은 raw representation을 비교하고 다른 계층은 canonical form을 비교하면 validation bypass나 cache inconsistency가 발생할 수 있다.
- **왜 중요한가:** Signature나 hash에 들어가는 bytes는 특히 deterministic representation이 필요하다.
- **예시:** 어떤 계층은 raw representation을 비교하고 다른 계층은 canonical …

#### 4. signature

- **뜻:** Sender와 receiver가 같은 semantic object를 서로 다른 byte ordering으로 encode하면 signature verification이 실패한다.
- **왜 중요한가:** Canonical JSON/CBOR 같은 규칙이나 명시적 field ordering이 필요한 이유다.
- **예시:** Sender와 receiver가 같은 semantic object를 서로 다른 byte …

Domain마다 동등성 기준이 다르다. 

  

Cache key도 동일하다. URL query parameter order를 semantic하게 동일하게 볼지에 따라 같은 요청이 여러 cache entry로 분리될 수 있다. Canonicalization은 storage 효율과 security, identity semantics를 동시에 건드리므로 명시적 계약으로 관리한다.

---

## CHAPTER 08 · untrusted deserialization은 resource exhaustion까지 공격면으로 본다

### 시작 전 용어집

#### 1. untrusted deserialization

- **뜻:** 악성 입력은 parser bug뿐 아니라 정상 문법을 이용해 CPU와 memory를 과도하게 사용하게 할 수 있다.
- **왜 중요한가:** 매우 깊은 nesting, 거대한 array, 과도한 string 길이, 압축 폭탄처럼 decoder가 처리해야 할 양을 공격자가 통제하면 서비스 거부가 가능하다.
- **예시:** 악성 입력은 parser bug뿐 아니라 정상 문법을 이용해 …

#### 2. resource

- **뜻:** Resource limit은 schema contract와 함께 설계한다.
- **왜 중요한가:** Recursive decoder는 입력 depth가 call stack limit에 영향을 줄 수 있다.
- **예시:** Resource limit은 schema contract와 함께 설계한다.

#### 3. 입력

- **뜻:** Parser boundary에 최대 body size, nesting depth, element count, string length, allocation budget을 둔다.
- **왜 중요한가:** Streaming parser를 사용해 memory를 줄이더라도 총 처리 시간과 element 수가 무제한이면 CPU exhaustion은 남는다.
- **예시:** Parser boundary에 최대 body size, nesting depth, element …

#### 4. stream

- **뜻:** Framework가 자동 parsing을 제공한다고 해서 안전한 default가 application의 threat model에 충분한지 확인한다.
- **왜 중요한가:** File upload와 network API는 허용 크기와 timeout을 명시한다.
- **예시:** Framework가 자동 parsing을 제공한다고 해서 안전한 default가 application의 …

Error message 생성 자체도 조심한다. 거대한 invalid input 전체를 exception에 포함하거나 log에 기록하면 storage와 privacy 문제가 추가된다. 위치와 요약, 제한된 preview만 남기는 방식이 더 안전하다.

---

## CHAPTER 09 · data migration은 decode 성공이 아니라 의미 보존을 검증한다

### 시작 전 용어집

#### 1. data migration

- **뜻:** Schema version이 바뀌면 과거 데이터를 새 model로 migration해야 할 수 있다.
- **왜 중요한가:** Migration script가 exception 없이 끝났다는 사실만으로 의미가 보존됐다고 할 수 없다.
- **예시:** Schema version이 바뀌면 과거 데이터를 새 model로 migration해야 …

#### 2. decode

- **뜻:** Row count, invariant, aggregate total, sample comparison 같은 reconciliation을 통해 변환 전후의 중요한 속성을 확인한다.
- **왜 중요한가:** 금액 단위를 원에서 cent-like 최소단위로 바꾸거나 timestamp timezone을 명시화하는 migration은 단순 field rename보다 위험하다.
- **예시:** Row count, invariant, aggregate total, sample comparison 같은 …

#### 3. 검증

- **뜻:** Rounding, overflow, ambiguous local time에서 데이터 의미가 달라질 수 있다.
- **왜 중요한가:** 변환 규칙과 irreversible case를 기록한다.
- **예시:** Rounding, overflow, ambiguous local time에서 데이터 의미가 달라질 …

#### 4. exception

- **뜻:** Large migration은 한 transaction으로 끝내기 어려울 수 있어 batch와 resume checkpoint를 사용한다.
- **왜 중요한가:** 재실행해도 중복 변환되지 않도록 idempotency를 고려한다.
- **예시:** Large migration은 한 transaction으로 끝내기 어려울 수 있어 …

중간에 실패했을 때 어느 row까지 완료됐는지 알 수 있어야 한다.

Application deploy와 migration deploy 순서도 compatibility 문제다. Old code와 new schema가 잠시 공존할 수 있다면 expand-and-contract 전략처럼 양쪽이 읽을 수 있는 중간 단계를 둔다. Data evolution은 code refactor보다 더 긴 lifetime을 가진다.

---

## CHAPTER 10 · serialization boundary를 하나의 pipeline으로 고정하면 오류 위치가 선명해진다

### 시작 전 용어집

#### 1. serialization

- **뜻:** Serialization의 최종 목적은 데이터를 “저장할 수 있게 만드는 것”이 아니다.
- **왜 중요한가:** 외부 입력 처리 흐름을 `bytes → decode → syntax parse → schema validation → normalization → domain construction → business validation`처럼 단계화하면 어떤 종류의 실패가 어디서 발생해야 하는지 정할 수 있다.
- **예시:** Serialization의 최종 목적은 데이터를 “저장할 수 있게 만드는 …

#### 2. pipeline

- **뜻:** Output은 역방향으로 domain value를 public DTO로 선택하고 format encoder를 통해 bytes로 만든다.
- **왜 중요한가:** Raw bytes를 내부 깊숙이 전달하거나 unvalidated dict를 domain service에 그대로 넘기면 여러 함수가 parser와 validation 책임을 반복한다.
- **예시:** Output은 역방향으로 domain value를 public DTO로 선택하고 format …

#### 3. 오류

- **뜻:** Trust boundary에서 의미를 정제하고 내부에는 좁은 type을 전달한다.
- **왜 중요한가:** Parser fixture는 format edge case를, schema test는 field contract를, domain test는 invariant를, compatibility test는 version matrix를 검증한다.
- **예시:** Trust boundary에서 의미를 정제하고 내부에는 좁은 type을 전달한다.

#### 4. 입력

- **뜻:** 같은 payload를 수십 번 end-to-end로만 테스트하는 것보다 failure class별로 원인이 빠르게 드러난다.
- **예시:** 같은 payload를 수십 번 end-to-end로만 테스트하는 것보다 failure …

각 단계는 입력과 출력 타입이 다르다.  

Test도 이 경계를 따라 나눈다.  

 **서로 다른 실행 환경과 시간의 버전 사이에서 같은 의미를 유지하면서 잘못된 입력을 경계에서 차단하는 것**이다.
