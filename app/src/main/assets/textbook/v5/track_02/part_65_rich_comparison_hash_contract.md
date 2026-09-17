# TRACK 02 · P65 — Rich comparison과 hash contract: equality·ordering·dict/set 불변식을 추적하기

비교 연산은 화면에 `==`, `<`, `>=` 같은 기호로 보이지만 실제 계약은 훨씬 크다. 특히 `dict`와 `set`은 비교와 hash가 서로 맞물린다는 전제 위에서 동작한다. `__eq__` 하나만 대충 구현해도 예제는 통과할 수 있지만, mutable 상태를 hash에 섞거나 서로 다른 타입의 equality 규칙이 비대칭이면 컬렉션의 기본 가정이 깨진다.

이 PART에서는 비교를 “True/False를 만드는 연산”으로 보지 않는다. **비교 후보 선택 → 지원 여부 → equality/ordering 의미 → hash bucket과 equality 재확인**이라는 실행 흐름으로 추적한다.

---

## 1. Comparison dispatch — 여섯 비교는 각각 프로토콜 후보가 있다

Python의 rich comparison에는 `__lt__`, `__le__`, `__eq__`, `__ne__`, `__gt__`, `__ge__`가 있다. 중요한 것은 이들이 산술 연산처럼 단순히 한쪽 메서드를 호출하는 문법 설탕이 아니라는 점이다.

```python
class Version:
    def __init__(self, major, minor):
        self.major = major
        self.minor = minor

    def _key(self):
        return (self.major, self.minor)

    def __lt__(self, other):
        if not isinstance(other, Version):
            return NotImplemented
        return self._key() < other._key()

    def __eq__(self, other):
        if not isinstance(other, Version):
            return NotImplemented
        return self._key() == other._key()
```

`v1 < v2`를 읽을 때는 다음을 구분한다.

1. 두 객체 타입 사이에 어떤 비교가 의미 있는가?
2. 왼쪽 구현이 그 조합을 지원하는가?
3. 지원하지 않으면 반사된 비교 후보가 참여할 수 있는가?
4. 끝까지 지원되지 않을 때 equality와 ordering이 어떻게 실패하는가?

ordering에서는 `<`의 반사 관계가 `>`이고 `<=`의 반사 관계가 `>=`다. 하위 타입이 더 구체적인 비교 의미를 제공하는 경우 디스패치 우선순위도 고려된다.

따라서 비교 구현을 리뷰할 때는 “메서드가 존재한다”보다 **이질 타입 조합에서 어느 쪽이 최종 의미를 결정하는가**를 확인해야 한다.

---

## 2. Equality와 inequality — 같다는 뜻을 먼저 정의한다

`__eq__`의 가장 어려운 부분은 코드가 아니라 **동일성의 기준**이다. 값 객체라면 어떤 필드가 의미를 구성하는지 먼저 정해야 한다.

```python
class Coordinate:
    def __init__(self, x, y, label=None):
        self.x = x
        self.y = y
        self.label = label

    def __eq__(self, other):
        if not isinstance(other, Coordinate):
            return NotImplemented
        return (self.x, self.y) == (other.x, other.y)
```

여기서는 `label`이 표시용 메타데이터이고 위치의 의미에는 들어가지 않는다고 결정했다. 이 결정이 뒤의 hash에도 그대로 이어져야 한다.

`is`와 `==`도 분리해야 한다.

```python
a = Coordinate(1, 2)
b = Coordinate(1, 2)

print(a is b)   # False
print(a == b)   # True
```

`is`는 객체 identity, `==`는 타입이 정의한 value equality다.

`__ne__`를 굳이 따로 구현할 필요가 없는 경우가 많지만, 핵심은 “`!=`는 무조건 `not (a == b)`”라는 식으로 사용자 타입의 모든 세부를 단순화하지 않는 것이다. Python 데이터 모델의 비교 fallback과 반환값을 존중해야 한다.

좋은 equality는 보통 다음 성질을 의도한다.

- reflexive: 정상 값 `x`에 대해 `x == x`
- symmetric: `x == y`와 `y == x`가 일관됨
- transitive: `x == y`, `y == z`이면 `x == z`

부동소수점 NaN처럼 언어와 수학 모델상 예외가 존재하므로 이것을 무조건 법칙처럼 강제하는 대신 **자신의 타입이 어떤 equality 모델을 채택하는지** 명시해야 한다.

---

## 3. Ordering은 total order가 아닐 수 있다

모든 객체가 자연스럽게 일렬로 정렬되는 것은 아니다. 집합 포함 관계, 버전의 일부 상태, 서로 다른 단위의 측정값처럼 비교가 부분적으로만 정의되는 도메인이 있다.

```python
class CapabilitySet:
    def __init__(self, names):
        self.names = frozenset(names)

    def __le__(self, other):
        if not isinstance(other, CapabilitySet):
            return NotImplemented
        return self.names <= other.names
```

이 타입에서 `a <= b`는 “권한 집합이 포함된다”는 의미가 될 수 있다. 그런데 서로 포함되지 않는 두 집합을 억지로 `<`/`>` 중 하나로 정하면 도메인 의미를 왜곡한다.

정렬이 필요하다고 해서 타입의 본질적 ordering을 거짓으로 만들 필요는 없다. UI 표시를 위한 정렬은 별도 key로 제공할 수 있다.

```python
items = sorted(capabilities, key=lambda x: (len(x.names), sorted(x.names)))
```

이 접근은 **도메인 비교**와 **표시 순서**를 분리한다.

`functools.total_ordering` 같은 도구는 일부 비교 메서드로 나머지를 합성하는 데 유용하지만, 잘못된 의미 모델을 고쳐 주지는 않는다. 먼저 equality와 order의 수학적 관계가 맞아야 한다.

---

## 4. Comparison에서 `NotImplemented` — 거짓과 지원 불가를 구분한다

다른 타입을 만났을 때 곧바로 `False`를 반환하면 미묘한 문제가 생긴다.

```python
class UserId:
    def __init__(self, value):
        self.value = value

    def __eq__(self, other):
        if not isinstance(other, UserId):
            return NotImplemented
        return self.value == other.value
```

`NotImplemented`는 “둘이 같지 않다”가 아니다. **내 구현이 이 타입 조합의 비교 의미를 모른다**는 뜻이다. 그러면 Python은 반대쪽 비교 후보 등 프로토콜이 정한 다음 경로를 고려할 수 있다.

잘못된 구현은 다음과 같다.

```python
    def __eq__(self, other):
        if not isinstance(other, UserId):
            return False
        return self.value == other.value
```

이 코드는 미래에 `UserId`와 의미 있게 비교될 수 있는 다른 타입이 생겨도 왼쪽 구현이 결과를 `False`로 확정해 버릴 수 있다.

ordering에서 지원되지 않는 조합은 최종적으로 `TypeError`로 이어지는 것이 자연스럽다. `1 < "2"`를 억지로 임의의 순서로 정하지 않는 것과 같은 이유다.

비교 구현에서 `NotImplemented`는 확장 가능한 타입 협력의 핵심 신호다.

---

## 5. Hash와 equality — 같은 객체로 판단하면 같은 hash여야 한다

hashable 객체에서 가장 중요한 계약은 다음이다.

```text
if a == b:
    hash(a) == hash(b)
```

역은 필요 없다. hash collision은 허용된다. 서로 다른 객체가 같은 hash를 가질 수 있고, 그래서 hash table은 후보 bucket을 좁힌 뒤 equality를 다시 확인한다.

```python
class Coordinate:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    def __eq__(self, other):
        if not isinstance(other, Coordinate):
            return NotImplemented
        return (self.x, self.y) == (other.x, other.y)

    def __hash__(self):
        return hash((self.x, self.y))
```

`__eq__`가 `(x, y)`를 의미 기준으로 삼았으므로 `__hash__`도 같은 의미 필드를 사용한다.

반대로 equality는 `id`만 보고 hash는 `(id, mutable_name)`을 본다면 같은 것으로 판단되는 두 값이 다른 hash를 가질 수 있어 계약이 깨진다.

또 Python은 사용자 클래스가 equality를 새로 정의했을 때 hashability를 보수적으로 다룬다. 이는 “값 equality를 재정의했는데 identity 기반 hash를 그대로 쓰는” 위험을 막는 방향이다.

hash 구현은 성능용 부가 기능이 아니라 equality 의미의 일부다.

---

## 6. Mutable hash trap — key가 bucket을 떠나 버리는 문제

가장 위험한 실수는 hash 계산에 들어가는 상태를 객체가 `dict`/`set`에 들어간 뒤 바꾸는 것이다.

```python
class MutableKey:
    def __init__(self, code):
        self.code = code

    def __eq__(self, other):
        return isinstance(other, MutableKey) and self.code == other.code

    def __hash__(self):
        return hash(self.code)

key = MutableKey("A")
items = {key: "saved"}
key.code = "B"
```

삽입 당시에는 `hash("A")`에 대응하는 위치를 사용했다. 상태를 바꾼 뒤 탐색은 `hash("B")`에서 시작한다. 같은 객체를 들고 있어도 자료구조가 기대한 탐색 경로와 현재 hash가 어긋난다.

해결은 보통 셋 중 하나다.

1. hash에 참여하는 의미 상태를 immutable로 만든다.
2. mutable 객체는 unhashable로 둔다.
3. 별도의 immutable key/value object를 만들어 dict/set key로 사용한다.

```python
from dataclasses import dataclass

@dataclass(frozen=True)
class UserKey:
    tenant: str
    user_id: int
```

`frozen=True`가 모든 논리적 불변성을 자동 증명하는 것은 아니지만, hash key의 의미 필드를 변경하지 않는 구조를 만드는 데 도움이 된다.

---

## 7. Dict/set lookup — hash는 주소가 아니라 후보를 줄이는 단계다

`dict[key]` 탐색을 “hash값으로 바로 값 찾기”라고 이해하면 collision과 equality의 역할을 놓친다. 개념적으로는 다음 흐름으로 보는 편이 정확하다.

```text
key object
  ↓ hash(key)
후보 위치/bucket 탐색
  ↓
저장된 key 후보와 identity/equality 확인
  ↓
일치하면 value 반환
```

실제 CPython 구현 세부는 최적화되어 있지만, 사용자 타입이 지켜야 하는 계약을 이해하는 데 이 모델이 충분히 중요하다.

따라서 다음 두 종류의 버그가 구분된다.

- **poor hash distribution** — 충돌이 많아져 성능이 나빠질 수 있음
- **hash/equality inconsistency** — 논리적으로 같은 key를 안정적으로 찾는 전제가 깨짐

첫 번째는 주로 성능 문제이고 두 번째는 correctness 문제다.

비교 연산이 비싸다면 hash table에서도 비용이 커질 수 있다. hash가 충돌할수록 equality 비교가 더 필요하기 때문이다. 따라서 큰 객체 전체를 매번 직렬화해 hash하는 식의 구현은 의미가 맞더라도 비용 모델을 검토해야 한다.

그리고 보안 경계에서는 공격자가 의도적으로 충돌을 유발할 수 있는지 같은 별도 고려도 생긴다. 핵심은 `dict`/`set`이 마법이 아니라 **hash와 equality 계약 위에 세워진 자료구조**라는 점이다.

---

## 8. Comparison contract — 타입의 값 의미를 하나의 규칙으로 묶는다

비교와 hash를 구현하기 전에 다음 표를 먼저 작성하면 설계가 안정된다.

```text
의미 identity 필드: tenant_id + user_id
표시용 필드: display_name
mutable 상태: last_seen_at
== 참여 필드: tenant_id + user_id
hash 참여 필드: tenant_id + user_id
ordering: 정의하지 않음
다른 타입과 비교: NotImplemented
```

이렇게 하면 `display_name`이 바뀌어도 key identity가 바뀌지 않아야 한다는 점이 명확하다.

최종 리뷰에서는 예제 한두 개보다 성질을 검증한다.

```python
# equality와 hash consistency
assert a == b
assert hash(a) == hash(b)

# 대칭성
assert (a == b) == (b == a)

# dict round-trip
mapping = {a: "value"}
assert mapping[b] == "value"
```

ordering을 제공한다면 경계값과 이질 타입도 따로 본다. `NotImplemented`, `TypeError`, partial order의 의도를 테스트 이름에 드러내는 것이 좋다.

비교 계약을 압축하면 다음과 같다.

- equality의 의미 필드를 먼저 정한다.
- 이질 타입을 모르면 `NotImplemented`로 협력 기회를 남긴다.
- ordering은 정말 도메인에 존재할 때만 제공한다.
- equal 객체는 같은 hash를 가져야 한다.
- hash에 참여하는 상태는 key로 사용되는 동안 바뀌면 안 된다.
- dict/set 테스트는 단순 hash 값이 아니라 실제 lookup까지 검증한다.

---

## 직관 봉인

- `==`는 identity 비교가 아니라 타입이 정의한 value contract다.
- “다른 타입이면 False”와 `NotImplemented`는 의미가 다르다.
- 모든 타입에 total ordering을 억지로 만들 필요는 없다.
- equal → same hash는 필수지만 same hash → equal은 아니다.
- mutable 상태를 hash에 넣으면 dict/set 탐색 전제를 깨뜨릴 수 있다.
- hash table은 hash만 보지 않고 후보를 좁힌 뒤 equality를 확인한다.

## 다음 연결

다음 PART에서는 개별 값의 비교를 넘어 **container protocol**을 추적한다. `len`, truthiness, `__getitem__`, slice, membership, iteration fallback이 각각 어떤 경로로 연결되며, 하나의 특수 메서드 구현이 예상 밖의 프로토콜 참여를 어떻게 열 수 있는지 살펴본다.
