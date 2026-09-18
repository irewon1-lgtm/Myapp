# PART 96 · Identifier design — random ID·name-derived ID·database locality·idempotency key를 분리하기

식별자는 단순히 “겹치지 않는 문자열”이 아니다. 객체를 외부에 노출할지, 같은 입력에서 같은 ID가 필요할지, database index에 어떤 write pattern을 만들지, retry request를 같은 작업으로 인식할지에 따라 요구가 달라진다. UUID 같은 도구도 목적을 먼저 정하지 않으면 security token이나 정렬 key로 잘못 사용하기 쉽다. 이 절에서는 **identity 목적 → 생성 방식 → storage/노출 특성** 순서로 설계한다.

---

## CHAPTER 01 · identifier의 첫 질문은 무엇을 동일한 것으로 볼 것인가다

### 시작 전 용어집

#### 1. identifier

- **뜻:** Identifier format을 고르기 전에 lifecycle과 equality를 정의한다.
- **왜 중요한가:** Order ID, user ID, upload ID, content ID는 모두 식별자지만 equality 의미가 다르다.
- **예시:** Identifier format을 고르기 전에 lifecycle과 equality를 정의한다.

#### 2. digest

- **뜻:** Database surrogate key는 row identity를 안정적으로 가리키는 목적이고, content digest는 내용 identity를 표현한다.
- **왜 중요한가:** 두 목적을 하나의 field로 합치면 content 변경 시 row identity까지 바뀌는 문제가 생길 수 있다.
- **예시:** Database surrogate key는 row identity를 안정적으로 가리키는 목적이고, …

새 order가 만들어질 때마다 새 identity가 필요한지, 동일 content는 같은 ID가 되어야 하는지 먼저 정한다.

 


---

**운영 검증 96-1 — CHAPTER 01 · identifier의 첫 질문은 무엇을 동일한 것으로 볼 것인가다**
CHAPTER 01 · identifier의 첫 질문은 무엇을 동일한 것으로 볼 것인가다을 검증할 때는 부분 성공을 구분할 상태 값을 정하고 중복 요청을 한 번 넣는다. PART 96 CHAPTER 1의 핵심 관찰값으로 재시도 로그와 실제 반영 건수를 비교한다. P96-C01 복구 검증은 실패 직전과 재시작 직후의 상태를 나란히 놓는 방식으로 진행한다. 같은 요청을 P96-C01에 다시 전달했을 때 결과가 한 번만 반영되고 복구 경로가 결정적으로 끝나는지 본다. P96-C01: 변경 후에는 중복·누락·재시도·미완료 작업을 각각 별도 지표로 센다. 여러 번 실행해도 각 지표가 예상 범위에 머물고 종료 조건이 항상 동일하면 통과다.
## CHAPTER 02 · random identifier는 중앙 coordination 없이 새 identity를 만들기 쉽다

### 시작 전 용어집

#### 1. random identifier

- **뜻:** UUID 계열의 random identifier가 대표적이다.
- **왜 중요한가:** 충돌 확률이 매우 낮다는 것과 절대 충돌하지 않는다는 것은 다르다.
- **예시:** import uuid / request_id = uuid.uuid4()

#### 2. coordination

- **뜻:** 충분히 큰 random space에서 ID를 만들면 여러 node가 중앙 sequence 없이 독립적으로 생성할 수 있다.
- **왜 중요한가:** Database unique constraint처럼 마지막 방어를 둘 수 있다.
- **예시:** import uuid / request_id = uuid.uuid4()

#### 3. identity

- **뜻:** Random ID는 생성 순서를 직접 드러내지 않아 외부 노출에 장점이 있을 수 있지만, “추측하기 어렵다”를 authorization으로 사용하지 않는다.
- **예시:** import uuid / request_id = uuid.uuid4()

```python
import uuid
request_id = uuid.uuid4()
```

 

 접근 권한은 별도 검증한다.

---

**운영 검증 96-2 — CHAPTER 02 · random identifier는 중앙 coordination 없이 새 identity를 만들기 쉽다**
CHAPTER 02 · random identifier는 중앙 coordination 없이 새 identity를 만들기 쉽다을 검증할 때는 제한시간과 재시도 횟수를 명시하고 연속 실패 뒤 정상 요청을 보낸다. PART 96 CHAPTER 2의 핵심 관찰값으로 중복 반영과 누락을 별도 지표로 센다. P96-C02 실패 후 재처리는 멱등성 확인 단계다. P96-C02의 처리 횟수·최종 상태·재시도 로그를 함께 비교해 같은 입력이 여러 번 와도 데이터 의미가 변하지 않는지 검증한다. P96-C02: 수정 효과를 검증할 때는 처리 완료 목록과 남은 목록을 직접 대조한다. 재시도 이후 완료 목록이 불필요하게 늘지 않고 실패 목록만 줄어드는 방향으로 움직여야 한다.
## CHAPTER 03 · name-derived identifier는 같은 namespace와 name에서 같은 결과를 만드는 deterministic identity다

### 시작 전 용어집

#### 1. name-derived identifier

- **뜻:** 같은 logical object를 여러 system에서 독립적으로 같은 ID로 계산해야 한다면 namespace와 name을 기반으로 deterministic identifier를 만들 수 있다.
- **왜 중요한가:** 이 방식은 mapping table 없이 reproducible identity를 만들 수 있지만 입력 normalization 규칙이 contract가 된다.
- **예시:** namespace = customer-system / name = external-customer-42

#### 2. namespace

- **뜻:** 대소문자, whitespace, Unicode normalization이 달라지면 다른 ID가 생성될 수 있다.
- **왜 중요한가:** Input name이 비밀이 아닐 때 deterministic ID도 secret이라고 가정하지 않는다.
- **예시:** namespace = customer-system / name = external-customer-42

#### 3. deterministic identity

- **뜻:** Known input에서 ID를 재계산할 수 있다.
- **예시:** namespace = customer-system / name = external-customer-42

```text
namespace = customer-system
name = external-customer-42
```

 

 

---

## CHAPTER 04 · text와 binary representation은 같은 ID를 다른 storage 비용으로 표현한다

### 시작 전 용어집

#### 1. text

- **뜻:** UUID를 canonical text로 저장하면 사람이 읽기 쉽고 log/API와 일관되지만 binary form보다 공간을 더 사용할 수 있다.
- **왜 중요한가:** Database와 wire protocol에서는 native UUID/binary type을 지원할 수도 있다.
- **예시:** UUID를 canonical text로 저장하면 사람이 읽기 쉽고 log/API와 …

#### 2. binary representation

- **뜻:** 중요한 것은 representation conversion 중 identity가 바뀌지 않는 것이다.
- **왜 중요한가:** 대소문자나 hyphen 유무를 다른 ID로 비교하지 않도록 parse 후 canonical object/value로 비교한다.
- **예시:** 중요한 것은 representation conversion 중 identity가 바뀌지 않는 …

#### 3. storage

- **뜻:** API boundary에서는 허용 text format을 명시하고 invalid length/character를 초기에 거부한다.
- **예시:** API boundary에서는 허용 text format을 명시하고 invalid length/character를 …

---

## CHAPTER 05 · identifier 생성 순서는 database index locality와 write amplification에 영향을 줄 수 있다

### 시작 전 용어집

#### 1. identifier

- **뜻:** Internal storage key와 public identifier를 분리하는 architecture도 가능하다.
- **왜 중요한가:** Database 성능은 ID 유행보다 실제 index 구조·write rate·replication 요구를 기준으로 측정한다.
- **예시:** Internal storage key와 public identifier를 분리하는 architecture도 가능하다.

#### 2. database index

- **뜻:** 완전히 random한 primary key를 clustered B-tree의 leading key로 사용하면 insert가 넓은 page에 분산되어 locality가 낮아질 수 있다.
- **왜 중요한가:** 시간 순서 성분을 가진 ID나 별도 sequential internal key가 workload에 더 적합할 수 있다.
- **예시:** 완전히 random한 primary key를 clustered B-tree의 leading key로 …

#### 3. locality

- **뜻:** 반대로 순차 ID를 외부에 그대로 노출하면 object 수와 생성 속도를 추정하거나 ID enumeration을 쉽게 만들 수 있다.
- **예시:** 반대로 순차 ID를 외부에 그대로 노출하면 object 수와 …

---

## CHAPTER 06 · identifier는 capability나 secret이 아니다

### 시작 전 용어집

#### 1. identifier

- **뜻:** Password reset token처럼 **소유 자체가 권한**인 값은 일반 identifier보다 훨씬 강한 entropy, expiration, one-time semantics가 필요하다.
- **왜 중요한가:** Domain object ID를 그대로 bearer token으로 재사용하지 않는다.
- **예시:** Password reset token처럼 **소유 자체가 권한**인 값은 일반 …

#### 2. capability

- **뜻:** `처럼 긴 random-looking ID가 있다고 해서 해당 resource를 볼 권한이 생기는 것은 아니다.
- **왜 중요한가:** Server는 항상 authenticated principal의 authorization을 확인해야 한다.
- **예시:** `처럼 긴 random-looking ID가 있다고 해서 해당 resource를 …

#### 3. secret

- **뜻:** 로그에 identifier를 기록할 수 있는지와 개인정보/민감도도 별도 판단한다.
- **왜 중요한가:** ID가 직접 개인정보가 아니어도 다른 dataset과 결합하면 개인을 추적할 수 있다.
- **예시:** 로그에 identifier를 기록할 수 있는지와 개인정보/민감도도 별도 판단한다.

`/users/550e8400-... 

 

 

---

## CHAPTER 07 · idempotency key는 resource identity가 아니라 요청 중복을 묶는 operation identity다

### 시작 전 용어집

#### 1. idempotency

- **뜻:** Client가 동일 idempotency key를 재사용하면 중복 side effect를 방지할 수 있다.
- **왜 중요한가:** Server는 key만 저장하는 것이 아니라 request fingerprint와 result를 함께 묶어야 한다.
- **예시:** Client가 동일 idempotency key를 재사용하면 중복 side effect를 …

#### 2. resource identity

- **뜻:** Client가 timeout 후 같은 payment request를 retry할 때 새로운 random request ID를 매번 만들면 server는 두 요청이 같은 logical operation인지 알기 어렵다.
- **왜 중요한가:** 같은 key로 다른 payload가 오면 conflict로 처리하는 편이 안전하다.
- **예시:** Client가 timeout 후 같은 payment request를 retry할 때 …

#### 3. operation identity

- **뜻:** Key retention 기간이 끝난 뒤 retry가 오면 다시 실행될 수 있으므로 idempotency window를 API contract에 포함한다.
- **예시:** Key retention 기간이 끝난 뒤 retry가 오면 다시 …

---

## CHAPTER 08 · identifier contract는 uniqueness보다 identity semantics와 노출 정책을 우선한다

### 시작 전 용어집

#### 1. identifier

- **뜻:** 식별자 설계에서는 생성 충돌 확률, deterministic 여부, database locality, public exposure, authorization 분리, lifecycle/retention을 함께 본다.
- **왜 중요한가:** 테스트에서는 parse/canonical form, duplicate insert, deterministic input normalization, idempotency key 재사용과 payload mismatch를 확인한다.
- **예시:** 식별자 설계에서는 생성 충돌 확률, deterministic 여부, database …

#### 2. uniqueness

- **뜻:** 이 PART의 핵심은 **ID를 단지 랜덤 문자열로 보지 않고, 무엇을 같은 대상으로 간주할지와 storage·security·retry semantics를 연결하는 장기 계약으로 설계하는 것**이다.
- **예시:** 이 PART의 핵심은 **ID를 단지 랜덤 문자열로 보지 …

---

## 실전 학습 루프 · identifier design

### 1. 쉬운 예

사용자 이름, DB primary key, 외부 API id는 모두 식별자지만 요구가 다르다. 사람이 읽기 쉬운 값, 변경 불가능한 내부 identity, 전역 uniqueness, 정렬 가능성, 정보 노출 위험을 분리해 선택해야 한다.

### 2. 한 줄 해석

좋은 identifier는 생성 방식보다 수명·scope·노출·충돌·변경 가능성 계약이 먼저다.

### 3. 직접 실행

아래 최소 예제를 실행하기 전에 **성공 경로와 실패 경로를 각각 한 줄로 예측**한다.

```python
from uuid import uuid4

internal_id = uuid4()
username = 'kim'
print(internal_id, username)
```

### 4. 수정 실습

1. 변경 가능한 username을 foreign key로 사용했을 때 rename 비용을 생각한다.
2. 시간 순서가 드러나는 id가 privacy상 문제가 되는 사례를 만든다.

수정 뒤에는 같은 입력을 여러 번 실행하거나 중간 crash를 가정해 결과가 중복·누락·무한 대기로 바뀌지 않는지 확인한다.

### 5. 확인 문제

사람에게 보이는 이름과 내부 불변 id를 하나로 합치는 것이 항상 단순하고 좋을까?

### 6. 정답과 오답 설명

**정답:** 아니다. 표시 이름은 변경 요구가 많고 내부 참조는 안정성이 중요해 역할을 분리하는 편이 흔히 안전하다.

**자주 나오는 오답:** unique해야 한다는 조건만 만족하면 identifier 설계가 끝났다고 보는 것이 오답이다.

운영형 문제에서는 함수 한 번의 정상 출력보다 **재시도, 중복, timeout, crash, 재시작** 뒤의 상태가 더 중요하다. 마지막으로 이 기능이 어떤 상태를 영구 저장하고 어떤 상태를 다시 계산할 수 있는지 구분해 적는다.

## 현장 디버깅 체크 · identifier design

### 증상에서 시작한다

id가 충돌하지는 않지만 rename·merge·sharding·외부 노출 시 참조가 깨지거나 정보가 과도하게 드러난다. 먼저 재현 가능한 최소 payload와 operation id를 고정한다. 최종 상태만 고치면 중복·정밀도·복구 문제의 실제 발생 지점을 숨길 수 있다.

### 먼저 볼 증거

id 생성 규칙, uniqueness scope, 변경 가능성, 정렬성, 외부 노출 범위, lookup index를 확인한다. 가능하면 이 값을 하나의 trace 또는 audit record로 묶어 시간 순서를 복원한다.

### 일부러 실패시켜 보기

표시 이름을 key로 쓴 데이터에서 rename을 수행하고 불변 surrogate id 설계와 변경 범위를 비교한다. 이런 반례가 자동 테스트에 들어가야 정상 예제만 통과하는 구현을 걸러낼 수 있다.

### 통과 기준

내부 identity와 표시/외부 reference 역할이 분리되고 수명 동안 참조 안정성이 유지돼야 한다. 통과 기준은 “에러가 안 난다”가 아니라 **어떤 입력과 실패 순서에서도 허용된 상태 집합을 벗어나지 않는다**로 적는다.

