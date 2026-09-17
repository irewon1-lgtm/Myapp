# PART 96 · Identifier design — random ID·name-derived ID·database locality·idempotency key를 분리하기

식별자는 단순히 “겹치지 않는 문자열”이 아니다. 객체를 외부에 노출할지, 같은 입력에서 같은 ID가 필요할지, database index에 어떤 write pattern을 만들지, retry request를 같은 작업으로 인식할지에 따라 요구가 달라진다. UUID 같은 도구도 목적을 먼저 정하지 않으면 security token이나 정렬 key로 잘못 사용하기 쉽다. 이 PART에서는 **identity 목적 → 생성 방식 → storage/노출 특성** 순서로 설계한다.

---

## CHAPTER 01 · identifier의 첫 질문은 무엇을 동일한 것으로 볼 것인가다

Order ID, user ID, upload ID, content ID는 모두 식별자지만 equality 의미가 다르다. 새 order가 만들어질 때마다 새 identity가 필요한지, 동일 content는 같은 ID가 되어야 하는지 먼저 정한다.

Database surrogate key는 row identity를 안정적으로 가리키는 목적이고, content digest는 내용 identity를 표현한다. 두 목적을 하나의 field로 합치면 content 변경 시 row identity까지 바뀌는 문제가 생길 수 있다.

Identifier format을 고르기 전에 lifecycle과 equality를 정의한다.

---

## CHAPTER 02 · random identifier는 중앙 coordination 없이 새 identity를 만들기 쉽다

충분히 큰 random space에서 ID를 만들면 여러 node가 중앙 sequence 없이 독립적으로 생성할 수 있다. UUID 계열의 random identifier가 대표적이다.

```python
import uuid
request_id = uuid.uuid4()
```

충돌 확률이 매우 낮다는 것과 절대 충돌하지 않는다는 것은 다르다. Database unique constraint처럼 마지막 방어를 둘 수 있다.

Random ID는 생성 순서를 직접 드러내지 않아 외부 노출에 장점이 있을 수 있지만, “추측하기 어렵다”를 authorization으로 사용하지 않는다. 접근 권한은 별도 검증한다.

---

## CHAPTER 03 · name-derived identifier는 같은 namespace와 name에서 같은 결과를 만드는 deterministic identity다

같은 logical object를 여러 system에서 독립적으로 같은 ID로 계산해야 한다면 namespace와 name을 기반으로 deterministic identifier를 만들 수 있다.

```text
namespace = customer-system
name = external-customer-42
```

이 방식은 mapping table 없이 reproducible identity를 만들 수 있지만 입력 normalization 규칙이 contract가 된다. 대소문자, whitespace, Unicode normalization이 달라지면 다른 ID가 생성될 수 있다.

Input name이 비밀이 아닐 때 deterministic ID도 secret이라고 가정하지 않는다. Known input에서 ID를 재계산할 수 있다.

---

## CHAPTER 04 · text와 binary representation은 같은 ID를 다른 storage 비용으로 표현한다

UUID를 canonical text로 저장하면 사람이 읽기 쉽고 log/API와 일관되지만 binary form보다 공간을 더 사용할 수 있다. Database와 wire protocol에서는 native UUID/binary type을 지원할 수도 있다.

중요한 것은 representation conversion 중 identity가 바뀌지 않는 것이다. 대소문자나 hyphen 유무를 다른 ID로 비교하지 않도록 parse 후 canonical object/value로 비교한다.

API boundary에서는 허용 text format을 명시하고 invalid length/character를 초기에 거부한다.

---

## CHAPTER 05 · identifier 생성 순서는 database index locality와 write amplification에 영향을 줄 수 있다

완전히 random한 primary key를 clustered B-tree의 leading key로 사용하면 insert가 넓은 page에 분산되어 locality가 낮아질 수 있다. 시간 순서 성분을 가진 ID나 별도 sequential internal key가 workload에 더 적합할 수 있다.

반대로 순차 ID를 외부에 그대로 노출하면 object 수와 생성 속도를 추정하거나 ID enumeration을 쉽게 만들 수 있다. Internal storage key와 public identifier를 분리하는 architecture도 가능하다.

Database 성능은 ID 유행보다 실제 index 구조·write rate·replication 요구를 기준으로 측정한다.

---

## CHAPTER 06 · identifier는 capability나 secret이 아니다

`/users/550e8400-...`처럼 긴 random-looking ID가 있다고 해서 해당 resource를 볼 권한이 생기는 것은 아니다. Server는 항상 authenticated principal의 authorization을 확인해야 한다.

Password reset token처럼 **소유 자체가 권한**인 값은 일반 identifier보다 훨씬 강한 entropy, expiration, one-time semantics가 필요하다. Domain object ID를 그대로 bearer token으로 재사용하지 않는다.

로그에 identifier를 기록할 수 있는지와 개인정보/민감도도 별도 판단한다. ID가 직접 개인정보가 아니어도 다른 dataset과 결합하면 개인을 추적할 수 있다.

---

## CHAPTER 07 · idempotency key는 resource identity가 아니라 요청 중복을 묶는 operation identity다

Client가 timeout 후 같은 payment request를 retry할 때 새로운 random request ID를 매번 만들면 server는 두 요청이 같은 logical operation인지 알기 어렵다. Client가 동일 idempotency key를 재사용하면 중복 side effect를 방지할 수 있다.

Server는 key만 저장하는 것이 아니라 request fingerprint와 result를 함께 묶어야 한다. 같은 key로 다른 payload가 오면 conflict로 처리하는 편이 안전하다.

Key retention 기간이 끝난 뒤 retry가 오면 다시 실행될 수 있으므로 idempotency window를 API contract에 포함한다.

---

## CHAPTER 08 · identifier contract는 uniqueness보다 identity semantics와 노출 정책을 우선한다

식별자 설계에서는 생성 충돌 확률, deterministic 여부, database locality, public exposure, authorization 분리, lifecycle/retention을 함께 본다.

테스트에서는 parse/canonical form, duplicate insert, deterministic input normalization, idempotency key 재사용과 payload mismatch를 확인한다.

이 PART의 핵심은 **ID를 단지 랜덤 문자열로 보지 않고, 무엇을 같은 대상으로 간주할지와 storage·security·retry semantics를 연결하는 장기 계약으로 설계하는 것**이다.
