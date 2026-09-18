# PART 94 · JSON interchange semantics — number·object key·canonicalization 경계를 정확히 다루기

JSON은 간단한 데이터 교환 형식이지만 Python object를 그대로 직렬화하면 모든 의미가 보존되는 것은 아니다. 정수와 부동소수점 범위, object key의 문자열 제약, key order, whitespace, 사용자 정의 type 변환이 서로 다른 층에 있다. 특히 JSON text를 hash·signature·cache key에 쓰려면 “의미가 같은 JSON”과 “byte가 같은 JSON”을 구분해야 한다. 이 절에서는 **JSON value model → Python 변환 → 안정된 representation 요구** 순서로 본다.

---

## CHAPTER 01 · JSON value model은 Python object model보다 훨씬 좁다

### 시작 전 용어집

#### 1. JSON

- **뜻:** JSON의 기본 값은 object, array, string, number, boolean, null로 제한된다.
- **왜 중요한가:** Python의 tuple, set, Decimal, datetime, bytes, custom class는 직접적인 JSON primitive가 아니다.
- **예시:** import json / payload = {"name": "lee", "active": …

#### 2. model

- **뜻:** Serialization이 성공한다는 것은 데이터가 JSON value model 안으로 표현됐다는 뜻이지 원래 Python type identity가 그대로 보존된다는 뜻은 아니다.
- **왜 중요한가:** Tuple이 array로 나가면 decode 후 list가 될 수 있다.
- **예시:** import json / payload = {"name": "lee", "active": …

#### 3. Python object

- **뜻:** Round-trip에서 어떤 의미를 유지해야 하는지 schema 수준에서 정한다.
- **예시:** import json / payload = {"name": "lee", "active": …

```python
import json

payload = {"name": "lee", "active": True, "count": 3}
text = json.dumps(payload)
```

 


---

## CHAPTER 02 · JSON number와 Python numeric type의 범위는 동일하지 않다

### 시작 전 용어집

#### 1. JSON

- **뜻:** JSON number grammar는 하나지만 consumer 구현은 정수 범위와 floating-point precision이 다를 수 있다.
- **왜 중요한가:** Python의 arbitrary precision int를 JavaScript 계열 consumer에 보내면 큰 정수가 정확히 표현되지 않을 수 있다.
- **예시:** from decimal import Decimal / value = Decimal("0.1")

#### 2. Python numeric

- **뜻:** 금액이나 ID를 number로 보낼지 string으로 보낼지는 interoperability contract다.
- **왜 중요한가:** Decimal을 float로 바꿔 dump하면 rounding이 들어갈 수 있다.
- **예시:** from decimal import Decimal / value = Decimal("0.1")

#### 3. type

- **뜻:** 이 값을 어떤 JSON representation으로 내보낼지 라이브러리가 임의로 결정하게 두지 않는다.
- **왜 중요한가:** Consumer가 요구하는 precision과 schema를 먼저 정한다.
- **예시:** from decimal import Decimal / value = Decimal("0.1")

```python
from decimal import Decimal
value = Decimal("0.1")
```

 

---

## CHAPTER 03 · JSON object key는 문자열이므로 Python dict key 의미가 좁아질 수 있다

### 시작 전 용어집

#### 1. JSON

- **뜻:** Python dict는 여러 hashable type을 key로 사용할 수 있지만 JSON object member name은 문자열이다.
- **왜 중요한가:** `1`과 `"1"`을 서로 다른 Python key로 가진 dict를 JSON으로 옮기면 원래 key identity를 유지하기 어렵다.
- **예시:** [ / {"key_type":"int","key":1,"value":"a"},

#### 2. key

- **뜻:** 따라서 public JSON model에서는 처음부터 string key를 사용하거나 key/value record array로 표현한다.
- **왜 중요한가:** Serialization layer가 암묵적으로 key를 문자열화하는 것보다 schema가 의미 변환을 명시하는 편이 안전하다.
- **예시:** [ / {"key_type":"int","key":1,"value":"a"},

```json
[
  {"key_type":"int","key":1,"value":"a"},
  {"key_type":"str","key":"1","value":"b"}
]
```


---

**검증 시나리오 P94-C3 — CHAPTER 03 · JSON object key는 문자열이므로 Python dict key 의미가 좁아질 수 있다**
`CHAPTER 03 · JSON object key는 문자열이므로 Python dict key 의미가 좁아질 수 있다` 검증은 가장 작은 객체 상태로 시작하는 데서 시작한다. P94-C3에서는 `CHAPTER 03 · JSON object key는 문자열이므로 Python dict key 의미가 좁아질 수 있다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 03 · JSON object key는 문자열이므로 Python dict key 의미가 좁아질 수 있다`에 대해 속성 하나만 바꿔 재실행고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 03 · JSON object key는 문자열이므로 Python dict key 의미가 좁아질 수 있다`의 반환값과 부수효과를 따로 기록하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 03 · JSON object key는 문자열이므로 Python dict key 의미가 좁아질 수 있다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P94-C3의 마무리는 수정 뒤 원래 조건을 다시 회귀 확인하는 것이다. 통과 기준은 `CHAPTER 03 · JSON object key는 문자열이므로 Python dict key 의미가 좁아질 수 있다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 04 · object member order와 whitespace는 의미와 text identity를 분리해서 본다

### 시작 전 용어집

#### 1. object member

- **뜻:** 많은 application에서는 JSON object key order가 business meaning이 아니다.
- **왜 중요한가:** 그러나 serializer는 insertion order를 보존하거나 pretty-print whitespace를 추가할 수 있다.
- **예시:** {"a":1,"b":2} / {"b":2, "a":1}

#### 2. order

- **뜻:** 다음 두 text는 사람이 보기에는 같은 data를 표현할 수 있다.
- **왜 중요한가:** Cache key나 test snapshot이 raw text에 의존하면 serializer option 하나만 바뀌어도 miss나 diff가 생긴다.
- **예시:** {"a":1,"b":2} / {"b":2, "a":1}

#### 3. whitespace

- **뜻:** 의미 비교는 parsed structure로 하고, byte identity가 필요한 곳만 별도의 canonical representation을 정의한다.
- **예시:** {"a":1,"b":2} / {"b":2, "a":1}

```text
{"a":1,"b":2}
{"b":2, "a":1}
```

 

---

**검증 시나리오 P94-C4 — CHAPTER 04 · object member order와 whitespace는 의미와 text identity를 분리해서 본다**
`CHAPTER 04 · object member order와 whitespace는 의미와 text identity를 분리해서 본다` 검증은 경계값을 먼저 지정하는 데서 시작한다. P94-C4에서는 `CHAPTER 04 · object member order와 whitespace는 의미와 text identity를 분리해서 본다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 04 · object member order와 whitespace는 의미와 text identity를 분리해서 본다`에 대해 정상값과 경계값을 연속 실행고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 04 · object member order와 whitespace는 의미와 text identity를 분리해서 본다`의 타입·정체성·수명 변화를 분리 기록하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 04 · object member order와 whitespace는 의미와 text identity를 분리해서 본다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P94-C4의 마무리는 결과를 설명할 수 있을 때 종료하는 것이다. 통과 기준은 `CHAPTER 04 · object member order와 whitespace는 의미와 text identity를 분리해서 본다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 05 · canonicalization은 `sort_keys=True` 하나보다 더 큰 계약이다

### 시작 전 용어집

#### 1. canonicalization

- **뜻:** Key sort와 whitespace 제거는 representation을 안정화하는 데 도움을 줄 수 있지만 numeric spelling, Unicode representation, duplicate key 처리까지 포함한 전체 canonicalization 표준과 동일하다고 단정하면 안 된다.
- **왜 중요한가:** Hash나 digital signature의 input으로 JSON을 사용한다면 양쪽 구현이 동일한 canonicalization 규칙을 공유해야 한다.
- **예시:** Key sort와 whitespace 제거는 representation을 안정화하는 데 도움을 …

#### 2. sort_keys

- **뜻:** 단순 application cache라면 자신이 통제하는 serializer option을 versioned contract로 고정할 수 있다.
- **왜 중요한가:** Canonical text를 만든 뒤 schema 변경으로 field가 추가되면 digest도 바뀐다.
- **예시:** 단순 application cache라면 자신이 통제하는 serializer option을 versioned …

#### 3. True

- **뜻:** Representation version을 hash input과 함께 관리한다.
- **예시:** Representation version을 hash input과 함께 관리한다.

---

## CHAPTER 06 · custom encoding은 domain object를 JSON schema로 투영하는 명시적 변환이다

### 시작 전 용어집

#### 1. custom encoding

- **뜻:** Datetime이나 Decimal을 자동으로 `str(obj)`로 보내면 format이 불명확해진다.
- **왜 중요한가:** Encoder function에서 type별 representation을 명시한다.
- **예시:** def encode(value): / if isinstance(value, Decimal):

#### 2. domain object

- **뜻:** Decode할 때도 arbitrary `type` field를 보고 임의 class를 생성하는 방식보다 allowlist된 schema만 복원한다.
- **왜 중요한가:** Serialization은 객체 그래프 복제가 아니라 interchange model로의 투영이라고 생각하면 attack surface가 줄어든다.
- **예시:** def encode(value): / if isinstance(value, Decimal):

```python
def encode(value):
    if isinstance(value, Decimal):
        return {"type": "decimal", "value": str(value)}
    raise TypeError
```

 

---

**검증 시나리오 P94-C6 — CHAPTER 06 · custom encoding은 domain object를 JSON schema로 투영하는 명시적 변환이다**
`CHAPTER 06 · custom encoding은 domain object를 JSON schema로 투영하는 명시적 변환이다` 검증은 호출 순서를 단순화하는 데서 시작한다. P94-C6에서는 `CHAPTER 06 · custom encoding은 domain object를 JSON schema로 투영하는 명시적 변환이다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 06 · custom encoding은 domain object를 JSON schema로 투영하는 명시적 변환이다`에 대해 순서 하나만 뒤집어 차이를 본다고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 06 · custom encoding은 domain object를 JSON schema로 투영하는 명시적 변환이다`의 호출 전후의 상태 전이를 번호로 남긴다하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 06 · custom encoding은 domain object를 JSON schema로 투영하는 명시적 변환이다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P94-C6의 마무리는 다른 순서에서도 계약이 유지되는지 확인하는 것이다. 통과 기준은 `CHAPTER 06 · custom encoding은 domain object를 JSON schema로 투영하는 명시적 변환이다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 07 · JSON parser에도 입력 크기와 nesting depth 같은 resource limit이 필요하다

### 시작 전 용어집

#### 1. JSON

- **뜻:** JSON은 text format이지만 매우 큰 array, 긴 string, 깊은 nesting은 memory와 CPU를 소모할 수 있다.
- **왜 중요한가:** HTTP body size를 parser 호출 전에 제한하고, domain layer에서도 item count와 field length를 검증한다.
- **예시:** JSON은 text format이지만 매우 큰 array, 긴 string, …

#### 2. nesting depth

- **뜻:** Duplicate object key를 parser가 마지막 값으로 덮는다면 security-sensitive configuration에서 ambiguity가 생길 수 있다.
- **왜 중요한가:** 필요하면 duplicate detection 가능한 parse path를 사용한다.
- **예시:** Duplicate object key를 parser가 마지막 값으로 덮는다면 security-sensitive …

#### 3. resource limit

- **뜻:** Syntax valid와 policy valid를 구분한다.
- **왜 중요한가:** JSON parser 성공 뒤 schema와 resource policy를 적용한다.
- **예시:** Syntax valid와 policy valid를 구분한다.

---

## CHAPTER 08 · JSON contract는 data model과 representation identity를 별도로 정의한다

### 시작 전 용어집

#### 1. JSON

- **뜻:** 안정적인 JSON API는 허용 field/type, number precision, unknown field policy, duplicate key policy, size limit을 정한다.
- **왜 중요한가:** Raw JSON text를 digest나 cache key로 쓴다면 serializer/canonicalization version도 추가한다.
- **예시:** 안정적인 JSON API는 허용 field/type, number precision, unknown …

#### 2. data model

- **뜻:** 테스트에는 매우 큰 integer, Decimal-like precision, non-string Python key, reordered object, duplicate key, deep nesting, custom type을 포함한다.
- **왜 중요한가:** 이 PART의 핵심은 **JSON을 Python object를 자동 저장하는 형식으로 보지 않고, 더 좁은 interchange value model로 투영하면서 의미 identity와 byte representation을 분리하는 것**이다.
- **예시:** 테스트에는 매우 큰 integer, Decimal-like precision, non-string Python …

---

## 실전 학습 루프 · JSON interchange semantics

### 1. 쉬운 예

JSON은 object·array·string·number 등의 문법을 정의하지만 큰 정수 정밀도, duplicate member 처리, schema, version compatibility는 각 시스템 계약에 남는다. 언어가 다르면 같은 JSON number를 다르게 표현할 수 있다.

### 2. 한 줄 해석

JSON은 wire syntax이지 도메인 schema와 숫자 정책까지 자동으로 보장하는 형식은 아니다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상하고, 실행 후에는 **어느 경계에서 상태나 의미가 바뀌었는지** 표시한다.

```python
import json

payload = '{"id":"A1","amount":12.50}'
data = json.loads(payload)
print(type(data['amount']), data['amount'])
```

### 4. 수정 실습

1. 큰 integer를 JavaScript client와 Python server 사이에서 안전하게 전달하는 방법을 설계한다.
2. unknown field와 missing field를 각각 어떻게 처리할지 version 정책을 적는다.

수정 전후를 비교할 때는 정상 경로만 보지 않고 실패 입력과 자원 한도도 함께 확인한다.

### 5. 확인 문제

JSON parser가 성공하면 서로 다른 언어에서도 모든 값의 의미가 동일할까?

### 6. 정답과 오답 설명

**정답:** 아니다. numeric model, duplicate key, schema/default/unknown-field 정책을 별도 계약으로 맞춰야 한다.

**자주 나오는 오답:** 텍스트 형식이라는 이유로 interoperability가 자동 보장된다고 보면 안 된다.

마지막에는 이 주제를 **입력/신뢰 수준 → 변환 또는 대기 → 검증 → 결과/실패** 순서로 다시 설명한다. 이 순서가 보이면 실제 장애에서도 원인 경계를 빠르게 좁힐 수 있다.

## 현장 디버깅 체크 · JSON interchange semantics

### 증상에서 시작한다

Python에서는 정상인데 다른 언어 client에서 큰 정수·소수·duplicate key가 다른 값으로 해석된다. 먼저 재현 가능한 최소 payload와 operation id를 고정한다. 최종 상태만 고치면 중복·정밀도·복구 문제의 실제 발생 지점을 숨길 수 있다.

### 먼저 볼 증거

원본 JSON text, parser/library 버전, target numeric type, duplicate-member/unknown-field 정책과 schema version을 기록한다. 가능하면 이 값을 하나의 trace 또는 audit record로 묶어 시간 순서를 복원한다.

### 일부러 실패시켜 보기

2^53보다 큰 integer, 0.1 계열 소수, 같은 key 두 번, missing/null/unknown field payload를 교차 언어로 비교한다. 이런 반례가 자동 테스트에 들어가야 정상 예제만 통과하는 구현을 걸러낼 수 있다.

### 통과 기준

지원하는 모든 reader/writer가 허용 payload의 의미를 동일하게 해석하고 비호환 입력은 명시적으로 거부돼야 한다. 통과 기준은 “에러가 안 난다”가 아니라 **어떤 입력과 실패 순서에서도 허용된 상태 집합을 벗어나지 않는다**로 적는다.
## 판단 규칙 · JSON의 세 층을 분리한다

JSON을 다룰 때는 문법, schema, domain 의미를 분리한다. {"amount": -1}은 JSON 문법상 유효할 수 있지만 업무 규칙에는 위배될 수 있다. {"amount": "100"}도 parser는 성공하지만 schema가 숫자를 요구한다면 거부해야 한다.

또한 null, missing field, unknown field는 서로 다른 상태다. version upgrade를 안전하게 하려면 old reader/new writer와 new reader/old writer 조합에서 이 세 상태를 어떻게 해석하는지 테스트로 고정한다.

## 개념 연결 · JSON 문법과 업무 의미를 분리한다

json.loads()가 성공한 뒤에도 데이터 계약은 끝나지 않는다. null과 field 누락의 차이, integer 범위, decimal 정밀도, unknown field 허용 여부, schema version은 application이 정의해야 한다.

특히 여러 언어가 같은 API를 쓸 때는 **source lexical value → parser 결과 → domain type** 세 단계를 비교해야 숫자 정밀도나 기본값 차이를 놓치지 않는다.
