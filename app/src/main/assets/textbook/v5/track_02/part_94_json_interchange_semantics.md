# PART 94 · JSON interchange semantics — number·object key·canonicalization 경계를 정확히 다루기

JSON은 간단한 데이터 교환 형식이지만 Python object를 그대로 직렬화하면 모든 의미가 보존되는 것은 아니다. 정수와 부동소수점 범위, object key의 문자열 제약, key order, whitespace, 사용자 정의 type 변환이 서로 다른 층에 있다. 특히 JSON text를 hash·signature·cache key에 쓰려면 “의미가 같은 JSON”과 “byte가 같은 JSON”을 구분해야 한다. 이 PART에서는 **JSON value model → Python 변환 → 안정된 representation 요구** 순서로 본다.

---

## CHAPTER 01 · JSON value model은 Python object model보다 훨씬 좁다

JSON의 기본 값은 object, array, string, number, boolean, null로 제한된다. Python의 tuple, set, Decimal, datetime, bytes, custom class는 직접적인 JSON primitive가 아니다.

```python
import json

payload = {"name": "lee", "active": True, "count": 3}
text = json.dumps(payload)
```

Serialization이 성공한다는 것은 데이터가 JSON value model 안으로 표현됐다는 뜻이지 원래 Python type identity가 그대로 보존된다는 뜻은 아니다. Tuple이 array로 나가면 decode 후 list가 될 수 있다.

Round-trip에서 어떤 의미를 유지해야 하는지 schema 수준에서 정한다.

---

## CHAPTER 02 · JSON number와 Python numeric type의 범위는 동일하지 않다

JSON number grammar는 하나지만 consumer 구현은 정수 범위와 floating-point precision이 다를 수 있다. Python의 arbitrary precision int를 JavaScript 계열 consumer에 보내면 큰 정수가 정확히 표현되지 않을 수 있다.

금액이나 ID를 number로 보낼지 string으로 보낼지는 interoperability contract다. Decimal을 float로 바꿔 dump하면 rounding이 들어갈 수 있다.

```python
from decimal import Decimal
value = Decimal("0.1")
```

이 값을 어떤 JSON representation으로 내보낼지 라이브러리가 임의로 결정하게 두지 않는다. Consumer가 요구하는 precision과 schema를 먼저 정한다.

---

## CHAPTER 03 · JSON object key는 문자열이므로 Python dict key 의미가 좁아질 수 있다

Python dict는 여러 hashable type을 key로 사용할 수 있지만 JSON object member name은 문자열이다. `1`과 `"1"`을 서로 다른 Python key로 가진 dict를 JSON으로 옮기면 원래 key identity를 유지하기 어렵다.

따라서 public JSON model에서는 처음부터 string key를 사용하거나 key/value record array로 표현한다.

```json
[
  {"key_type":"int","key":1,"value":"a"},
  {"key_type":"str","key":"1","value":"b"}
]
```

Serialization layer가 암묵적으로 key를 문자열화하는 것보다 schema가 의미 변환을 명시하는 편이 안전하다.

---

## CHAPTER 04 · object member order와 whitespace는 의미와 text identity를 분리해서 본다

많은 application에서는 JSON object key order가 business meaning이 아니다. 그러나 serializer는 insertion order를 보존하거나 pretty-print whitespace를 추가할 수 있다.

다음 두 text는 사람이 보기에는 같은 data를 표현할 수 있다.

```text
{"a":1,"b":2}
{"b":2, "a":1}
```

Cache key나 test snapshot이 raw text에 의존하면 serializer option 하나만 바뀌어도 miss나 diff가 생긴다. 의미 비교는 parsed structure로 하고, byte identity가 필요한 곳만 별도의 canonical representation을 정의한다.

---

## CHAPTER 05 · canonicalization은 `sort_keys=True` 하나보다 더 큰 계약이다

Key sort와 whitespace 제거는 representation을 안정화하는 데 도움을 줄 수 있지만 numeric spelling, Unicode representation, duplicate key 처리까지 포함한 전체 canonicalization 표준과 동일하다고 단정하면 안 된다.

Hash나 digital signature의 input으로 JSON을 사용한다면 양쪽 구현이 동일한 canonicalization 규칙을 공유해야 한다. 단순 application cache라면 자신이 통제하는 serializer option을 versioned contract로 고정할 수 있다.

Canonical text를 만든 뒤 schema 변경으로 field가 추가되면 digest도 바뀐다. Representation version을 hash input과 함께 관리한다.

---

## CHAPTER 06 · custom encoding은 domain object를 JSON schema로 투영하는 명시적 변환이다

Datetime이나 Decimal을 자동으로 `str(obj)`로 보내면 format이 불명확해진다. Encoder function에서 type별 representation을 명시한다.

```python
def encode(value):
    if isinstance(value, Decimal):
        return {"type": "decimal", "value": str(value)}
    raise TypeError
```

Decode할 때도 arbitrary `type` field를 보고 임의 class를 생성하는 방식보다 allowlist된 schema만 복원한다. Serialization은 객체 그래프 복제가 아니라 interchange model로의 투영이라고 생각하면 attack surface가 줄어든다.

---

## CHAPTER 07 · JSON parser에도 입력 크기와 nesting depth 같은 resource limit이 필요하다

JSON은 text format이지만 매우 큰 array, 긴 string, 깊은 nesting은 memory와 CPU를 소모할 수 있다. HTTP body size를 parser 호출 전에 제한하고, domain layer에서도 item count와 field length를 검증한다.

Duplicate object key를 parser가 마지막 값으로 덮는다면 security-sensitive configuration에서 ambiguity가 생길 수 있다. 필요하면 duplicate detection 가능한 parse path를 사용한다.

Syntax valid와 policy valid를 구분한다. JSON parser 성공 뒤 schema와 resource policy를 적용한다.

---

## CHAPTER 08 · JSON contract는 data model과 representation identity를 별도로 정의한다

안정적인 JSON API는 허용 field/type, number precision, unknown field policy, duplicate key policy, size limit을 정한다. Raw JSON text를 digest나 cache key로 쓴다면 serializer/canonicalization version도 추가한다.

테스트에는 매우 큰 integer, Decimal-like precision, non-string Python key, reordered object, duplicate key, deep nesting, custom type을 포함한다.

이 PART의 핵심은 **JSON을 Python object를 자동 저장하는 형식으로 보지 않고, 더 좁은 interchange value model로 투영하면서 의미 identity와 byte representation을 분리하는 것**이다.
