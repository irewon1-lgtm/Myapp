# PART 59 · Enum·sentinel·result model — 가능한 상태를 원시값보다 좁게 표현하기

문자열 `"PAID"`, 숫자 `2`, `None`, `False`만으로 상태를 표현하면 값 자체만 보고 의미를 알기 어렵고 잘못된 조합도 쉽게 만들어진다. Enum은 허용 값 집합을 닫고, sentinel은 일반 값과 “제공되지 않음”을 분리하며, result variant는 성공·부재·실패의 서로 다른 data를 한 contract 안에 표현한다. 핵심은 **가능한 상태를 데이터 모델에서 줄여 호출자가 해석해야 할 암묵적 규칙을 없애는 것**이다.

---

## CHAPTER 01 · enum은 문자열 상수 모음보다 닫힌 상태 공간을 만든다

### 시작 전 용어집

#### 1. enum

- **뜻:** Enum을 사용하면 application 내부에서 허용 상태를 명시적인 member 집합으로 제한하고 type checker와 pattern matching이 그 집합을 이해하게 할 수 있다.
- **왜 중요한가:** Enum member의 이름과 외부 wire value는 같은 것으로 사용할 수도 있지만 분리할 가치가 있다.
- **예시:** Enum을 사용하면 application 내부에서 허용 상태를 명시적인 member …

#### 2. 상태

- **뜻:** 주문 상태를 raw string으로 받으면 오타 `"PAIDD"`도 문자열 타입에는 유효하고 어느 함수에서든 새로운 임의 값이 들어올 수 있다.
- **왜 중요한가:** 내부 member `PAID`가 외부 API에서 `"paid"`로 encode될 수 있고 나중에 display label이 바뀌어도 wire contract를 유지할 수 있다.
- **예시:** 주문 상태를 raw string으로 받으면 오타 `"PAIDD"`도 문자열 …

#### 3. 타입

- **뜻:** UI 표시 문자열을 enum identity로 사용하면 번역이나 문구 변경이 persistence schema까지 흔들 수 있다.
- **왜 중요한가:** 새 member 추가는 단순 constant 추가가 아니다.
- **예시:** UI 표시 문자열을 enum identity로 사용하면 번역이나 문구 …

#### 4. 함수

- **뜻:** Exhaustive match, database constraint, serializer, permission policy가 새 상태를 처리하는지 확인해야 한다.
- **왜 중요한가:** Closed set의 장점은 이런 누락을 검증 대상으로 만들 수 있다는 점이다.
- **예시:** Exhaustive match, database constraint, serializer, permission policy가 새 …

---

## CHAPTER 02 · enum member의 identity와 underlying value를 구분한다

### 시작 전 용어집

#### 1. enum

- **뜻:** Enum member는 단순 primitive alias가 아니라 고유 member object semantics를 가질 수 있다.
- **왜 중요한가:** value`가 `"paid"`라는 사실을 구분하면 내부 code가 raw string comparison에 다시 빠지는 것을 막을 수 있다.
- **예시:** Enum member는 단순 primitive alias가 아니라 고유 member …

#### 2. identity

- **뜻:** Application 내부에서는 member를 전달하고 I/O boundary에서 primitive value로 변환하는 구조가 명확하다.
- **왜 중요한가:** Underlying value가 numeric code라면 DB나 protocol compatibility 때문에 안정적으로 유지해야 할 수 있다.
- **예시:** Application 내부에서는 member를 전달하고 I/O boundary에서 primitive value로 …

#### 3. underlying value

- **뜻:** Member 순서를 바꿨다고 자동 번호가 바뀌는 방식은 장기 storage에 위험하다.
- **왜 중요한가:** Persistent/wire code는 명시적으로 고정한다.
- **예시:** Member 순서를 바꿨다고 자동 번호가 바뀌는 방식은 장기 …

#### 4. protocol

- **뜻:** Unknown external value를 decoder가 만났을 때 default member로 조용히 바꿀지 실패할지도 정책이다.
- **왜 중요한가:** Payment status처럼 모르는 상태를 `FAILED`로 추정하면 실제 새 상태 의미를 잃을 수 있다.
- **예시:** Unknown external value를 decoder가 만났을 때 default member로 …

`Status.PAID` 자체와 그 안의 `. 

  

  Forward compatibility가 필요하면 `Unknown(raw)` 같은 별도 variant로 보존하는 방법도 있다.

---

## CHAPTER 03 · Flag는 여러 독립 capability를 bitset으로 조합하지만 모순을 자동 제거하지 않는다

### 시작 전 용어집

#### 1. Flag

- **뜻:** 서로 독립적으로 동시에 켜질 수 있는 option은 `Flag`/bit mask로 표현할 수 있다.
- **왜 중요한가:** READ, WRITE, EXECUTE처럼 여러 capability를 OR로 조합하고 membership을 검사할 수 있다.
- **예시:** 서로 독립적으로 동시에 켜질 수 있는 option은 `Flag`/bit …

#### 2. capability

- **뜻:** Bitset은 독립 capability 집합에 강하다.
- **왜 중요한가:** 외부 numeric flag에서 모르는 bit가 들어왔을 때 버릴지 보존할지도 protocol evolution과 관련된다.
- **예시:** Bitset은 독립 capability 집합에 강하다.

#### 3. set

- **뜻:** `COMPRESS`와 `NO_COMPRESSION`처럼 상호 배타적인 flag를 같은 bitset에 허용하면 모순 state가 생긴다.
- **왜 중요한가:** 이 경우 enum variant나 validation이 더 적합하다.
- **예시:** `COMPRESS`와 `NO_COMPRESSION`처럼 상호 배타적인 flag를 같은 bitset에 허용하면 …

#### 4. bitset

- **뜻:** Binary representation과 bitwise operation을 이해하면 permission mask와 OS flag를 읽기 쉬워진다.
- **왜 중요한가:** 하지만 모든 boolean 조합이 업무적으로 유효한 것은 아니다.
- **예시:** Binary representation과 bitwise operation을 이해하면 permission mask와 OS …

Old reader가 future flag를 읽고 다시 쓸 때 unknown bit를 제거하면 data loss가 생길 수 있다. Low-level API를 감쌀 때 unknown bit policy를 확인한다.

---

## CHAPTER 04 · sentinel은 `None`과 실제 값 사이에 제3의 상태를 만든다

### 시작 전 용어집

#### 1. sentinel

- **뜻:** Default를 None으로 두면 두 상태를 구분할 수 없으므로 고유 sentinel object를 사용한다.
- **왜 중요한가:** Sentinel은 caller가 정상 domain value로 만들 수 없는 marker여야 한다.
- **예시:** MISSING = object() / def patch(name=MISSING):

#### 2. None

- **뜻:** Configuration patch에서 `None`이 “값 삭제”를 의미하고 argument 생략이 “변경하지 않음”을 뜻할 수 있다.
- **왜 중요한가:** Sentinel은 equality보다 identity로 검사한다.
- **예시:** MISSING = object() / def patch(name=MISSING):

#### 3. 상태

- **뜻:** Custom `__eq__`를 가진 object가 우연히 sentinel과 같다고 주장하는 것을 피하고 고유 object 자체를 확인한다.
- **왜 중요한가:** Public API에서 sentinel을 노출할 필요가 없다면 private constant로 유지한다.
- **예시:** MISSING = object() / def patch(name=MISSING):

#### 4. configuration

- **뜻:** 여러 종류의 absence가 필요해지면 sentinel 개수를 계속 늘리기보다 explicit result/state variant가 더 읽기 쉬울 수 있다.
- **왜 중요한가:** `MISSING`, `UNSET`, `DEFAULT`, `DELETE`가 난립하면 caller가 다시 숨은 vocabulary를 외워야 한다.
- **예시:** MISSING = object() / def patch(name=MISSING):

```python
MISSING = object()

def patch(name=MISSING):
    if name is MISSING:
        return KEEP
    if name is None:
        return CLEAR
```

  

 

---

## CHAPTER 05 · result variant는 성공·부재·실패가 서로 다른 data를 갖게 한다

### 시작 전 용어집

#### 1. result variant

- **뜻:** `Found(user)`, `NotFound`, `LookupFailed(error)`처럼 result variant를 두면 호출자가 세 상태를 명시적으로 처리할 수 있다.
- **왜 중요한가:** Exception과 result 중 무엇을 사용할지는 실패가 control flow에서 얼마나 정상적으로 예상되는지에 따라 달라진다.
- **예시:** 각 branch가 필요한 field만 보유하므로 `success=False인데 value가 채워져 …

#### 2. 실패

- **뜻:** 함수가 `User | None`을 반환하면 None은 “찾지 못함”을 표현할 수 있지만 database timeout까지 None으로 바꾸면 원인을 잃는다.
- **왜 중요한가:** Dictionary lookup의 missing처럼 빈번한 branch는 result/optional이 자연스러울 수 있고, contract 자체를 수행하지 못하는 infrastructure failure는 exception이 간결할 수 있다.
- **예시:** 각 branch가 필요한 field만 보유하므로 `success=False인데 value가 채워져 …

#### 3. data

- **뜻:** 모든 오류를 result로 바꾸거나 모든 상태를 exception으로 던지는 극단을 피한다.
- **왜 중요한가:** Result variant는 pattern matching과 잘 맞는다.
- **예시:** 각 branch가 필요한 field만 보유하므로 `success=False인데 value가 채워져 …

#### 4. 함수

- **뜻:** 각 branch가 필요한 field만 보유하므로 `success=False인데 value가 채워져 있음` 같은 모순을 줄인다.
- **왜 중요한가:** 가능한 상태 공간을 type 구조로 좁히는 효과다.
- **예시:** 각 branch가 필요한 field만 보유하므로 `success=False인데 value가 채워져 …

---

## CHAPTER 06 · error code는 외부 안정 contract와 내부 exception을 연결할 수 있다

### 시작 전 용어집

#### 1. error code

- **뜻:** HTTP API나 CLI는 내부 exception class 이름을 외부에 그대로 노출하지 않고 안정된 error code를 제공할 수 있다.
- **왜 중요한가:** `USER_NOT_FOUND`, `RATE_LIMITED`, `INVALID_STATE` 같은 code는 client가 machine-readable하게 대응할 수 있고 human message는 번역/개선 가능하다.
- **예시:** HTTP API나 CLI는 내부 exception class 이름을 외부에 …

#### 2. contract

- **뜻:** 내부 exception hierarchy와 외부 code는 일대일일 필요가 없다.
- **왜 중요한가:** 여러 low-level timeout을 하나의 `UPSTREAM_UNAVAILABLE`로 번역할 수 있고 동일 exception도 operation context에 따라 다른 public result가 될 수 있다.
- **예시:** 내부 exception hierarchy와 외부 code는 일대일일 필요가 없다.

#### 3. exception

- **뜻:** Boundary adapter가 mapping을 소유한다.
- **왜 중요한가:** Error code를 enum으로 관리하면 오타와 duplicate를 줄일 수 있지만 wire value 변경은 breaking change가 될 수 있다.
- **예시:** Boundary adapter가 mapping을 소유한다.

#### 4. HTTP

- **뜻:** 코드 member 이름을 refactor해도 외부 value는 안정적으로 유지한다.
- **예시:** 코드 member 이름을 refactor해도 외부 value는 안정적으로 유지한다.

---

## CHAPTER 07 · enum serialization은 member 이름·value·version 중 무엇을 저장할지 정한다

### 시작 전 용어집

#### 1. enum

- **뜻:** Unknown enum을 읽을 때 decoder failure가 전체 record를 막는지, raw unknown을 보존하는 forward-compatible model을 사용하는지 선택한다.
- **왜 중요한가:** Closed internal state와 open external protocol의 요구가 다르다.
- **예시:** Unknown enum을 읽을 때 decoder failure가 전체 record를 …

#### 2. serialization

- **뜻:** value`를 `"paid"`로 저장할지 선택해야 한다.
- **왜 중요한가:** 이름은 source refactor에 취약할 수 있고 value는 public schema로 고정될 수 있다.
- **예시:** value`를 `"paid"`로 저장할지 선택해야 한다.

#### 3. member

- **뜻:** 이미 저장된 code `2`가 다음 release에서 다른 member를 뜻하면 silent data corruption이 된다.
- **왜 중요한가:** Stable explicit value를 사용하거나 schema migration을 수행한다.
- **예시:** 이미 저장된 code `2`가 다음 release에서 다른 member를 …

#### 4. value

- **뜻:** Numeric auto-value는 source 순서 변화로 값이 달라질 수 있으므로 long-lived storage에는 신중해야 한다.
- **왜 중요한가:** Persistence가 장기적이라면 version과 migration policy를 함께 고려한다.
- **예시:** Numeric auto-value는 source 순서 변화로 값이 달라질 수 …

JSON에 `Status.PAID.name`을 `"PAID"`로 저장할지 `.  

  

 

---

## CHAPTER 08 · 상태 모델의 품질은 `불가능한 조합을 얼마나 만들기 어렵게 했는가`로 평가한다

### 시작 전 용어집

#### 1. 상태

- **뜻:** Boolean 세 개와 optional field 다섯 개로 모든 상태를 표현할 수 있어도 가능한 조합이 수십 개로 늘고 그중 대부분이 무효라면 약한 모델이다.
- **왜 중요한가:** Enum, variant, sentinel을 사용해 각 상태가 필요한 data만 가지게 하면 validation code와 branch 수가 줄어든다.
- **예시:** Boolean 세 개와 optional field 다섯 개로 모든 …

#### 2. 불가능한 조합을 얼마나 만들기 어렵게 했는가

- **뜻:** 모델을 바꿀 때는 serialization, database, UI, type checker, test가 같은 state vocabulary를 공유하는지 확인한다.
- **왜 중요한가:** 한 layer만 enum이고 다른 layer는 자유 문자열이라면 경계에서 다시 invalid state가 들어올 수 있다.
- **예시:** 모델을 바꿀 때는 serialization, database, UI, type checker, …

#### 3. boolean

- **뜻:** Enum·sentinel·result model의 핵심은 **새 타입을 많이 만드는 것이 아니라 의미가 다른 상태를 같은 primitive에 겹치지 않고, 호출자가 처리해야 할 경우의 수를 코드 구조에 그대로 드러내는 것**이다.
- **예시:** Enum·sentinel·result model의 핵심은 **새 타입을 많이 만드는 것이 …
