# PART 65 · Rich comparison과 hash contract — equality·ordering·dict/set 불변식을 연결하기

비교 연산은 화면에 `==`, `<`, `>=` 같은 기호로 보이지만 실제 계약은 더 넓다. 사용자 정의 객체가 `dict`와 `set`의 key가 되기 시작하면 equality와 hash가 서로 맞물리고, mutable 상태가 hash에 들어가거나 이질 타입 비교를 조기에 확정하면 컬렉션의 기본 가정이 깨질 수 있다. 이 PART에서는 비교를 **후보 선택 → 지원 여부 → 의미 판단 → hash table lookup**의 순서로 읽는다.

---

## CHAPTER 01 · comparison dispatch는 여섯 비교 연산의 후보 선택 규칙이다

Python rich comparison에는 `__lt__`, `__le__`, `__eq__`, `__ne__`, `__gt__`, `__ge__`가 있다. 핵심은 메서드 이름을 외우는 것이 아니라 서로 다른 타입이 만났을 때 어느 구현이 의미를 결정하는지 추적하는 것이다.

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
```

`v1 < v2`를 볼 때는 좌우 타입, 왼쪽 후보의 지원 여부, 반사 관계의 비교 후보, 최종 실패 형태를 확인한다. 상속 관계가 있으면 더 구체적인 타입이 자신의 의미를 제공할 기회도 고려된다.

비교 버그는 코드 한 줄보다 타입 관계에서 발생한다. “항상 왼쪽 메서드가 결정한다”는 단순 모델을 버리고 실제 디스패치 경로를 본다.

---

## CHAPTER 02 · equality는 어떤 필드가 객체의 의미를 구성하는지 결정한다

`__eq__` 구현에서 가장 어려운 부분은 True/False를 반환하는 코드가 아니라 “무엇을 같다고 볼 것인가”를 정의하는 일이다.

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

여기서는 `label`을 표시용 메타데이터로 보고 equality에서 제외했다. 이 결정은 hash, cache key, deduplication에도 그대로 영향을 준다.

`is`는 identity이고 `==`는 타입이 정의한 value equality다. 서로 다른 객체가 같은 값을 나타낼 수 있고, 같은 객체라도 도메인 규칙상 특수한 비교 의미를 가질 수 있다. 일반 값 객체는 대칭성·추이성 같은 성질을 지키는 편이 좋지만 NaN처럼 예외적인 모델도 있으므로 자신의 타입 의미를 먼저 정의한다.

---

## CHAPTER 03 · ordering은 반드시 total order일 필요가 없다

모든 값이 자연스럽게 한 줄로 정렬되는 것은 아니다. 권한 집합, 버전 범위, 그래프 관계처럼 일부 쌍만 비교 가능한 도메인에서는 부분 순서가 더 정확할 수 있다.

```python
class CapabilitySet:
    def __init__(self, names):
        self.names = frozenset(names)

    def __le__(self, other):
        if not isinstance(other, CapabilitySet):
            return NotImplemented
        return self.names <= other.names
```

여기서 `a <= b`는 포함 관계다. 서로 포함되지 않는 두 집합에 임의로 `<`나 `>`를 부여하면 UI 정렬 편의 때문에 도메인 의미를 왜곡한다.

표시 순서가 필요하면 `sorted(..., key=...)`로 별도 기준을 둔다. `functools.total_ordering`도 일부 비교를 합성하는 도구일 뿐 잘못된 수학적 의미를 교정해 주지는 않는다.

---

## CHAPTER 04 · comparison에서 `NotImplemented`는 False와 다르다

이질 타입을 만났을 때 무조건 `False`를 반환하면 다른 타입이 비교 의미를 제공할 기회를 막을 수 있다.

```python
class UserId:
    def __init__(self, value):
        self.value = value

    def __eq__(self, other):
        if not isinstance(other, UserId):
            return NotImplemented
        return self.value == other.value
```

`NotImplemented`는 “둘이 다르다”가 아니라 “이 구현은 이 타입 조합을 판단하지 않는다”는 신호다. Python은 반대쪽 후보 등 프로토콜이 허용하는 경로를 더 볼 수 있다.

특히 extension type이나 adapter type과 상호운용할 때 중요하다. 먼저 만들어진 타입이 모르는 미래 타입까지 `False`로 확정하면 확장 가능성이 떨어진다. 지원하지 않는 비교와 실제로 서로 다른 값을 구분한다.

---

## CHAPTER 05 · hash와 equality는 같은 의미 필드를 공유해야 한다

Hashable 객체에서 가장 중요한 불변식은 다음이다.

```text
if a == b:
    hash(a) == hash(b)
```

역은 필요 없다. hash collision은 허용된다. Hash table은 hash로 후보 위치를 좁힌 뒤 equality를 다시 확인한다.

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

Equality가 `(x, y)`를 의미로 삼았으므로 hash도 같은 필드를 사용한다. Equality에서 제외한 field를 hash에 넣으면 같은 객체라고 판단하면서 서로 다른 bucket 후보를 만들 수 있어 계약이 깨진다.

사용자 클래스가 equality를 새로 정의했을 때 Python이 hashability를 보수적으로 다루는 이유도 이 불변식을 보호하기 위해서다.

---

## CHAPTER 06 · mutable hash는 key를 논리적으로 잃어버리게 만들 수 있다

Hash 계산에 참여하는 상태를 `dict`나 `set` 삽입 뒤 바꾸면 탐색 경로가 달라질 수 있다.

```python
class MutableKey:
    def __init__(self, code):
        self.code = code

    def __eq__(self, other):
        return isinstance(other, MutableKey) and self.code == other.code

    def __hash__(self):
        return hash(self.code)

key = MutableKey("A")
data = {key: "saved"}
key.code = "B"
```

삽입할 때와 조회할 때 hash가 달라질 수 있으므로 컬렉션이 기대한 위치와 현재 객체 의미가 어긋난다. 같은 Python object를 들고 있어도 key lookup이 실패할 수 있는 위험한 상태다.

해결책은 hash 의미 상태를 immutable로 만들거나, mutable 객체를 unhashable로 두거나, 별도의 immutable key object를 사용하는 것이다. `dataclass(frozen=True)` 같은 도구는 이 의도를 표현하는 데 도움이 되지만 field 자체의 깊은 mutability까지 자동 해결하지는 않는다.

---

## CHAPTER 07 · dict/set lookup은 hash 한 번으로 끝나지 않는다

`dict`와 `set`은 hash value를 이용해 후보 영역을 찾지만 collision 때문에 equality 확인도 필요하다. 따라서 느린 `__eq__`나 불안정한 `__hash__`는 데이터 구조 전체의 성능과 correctness에 영향을 준다.

Key가 복잡한 network object graph를 따라가며 equality를 계산하거나 hash를 매번 비싼 방식으로 만들면 lookup 비용이 커진다. Immutable value object라면 필요한 경우 hash 결과를 캐시할 수 있지만, 캐시가 의미 상태와 불일치하지 않는지 보장해야 한다.

또 악의적이거나 편향된 입력에서 많은 collision이 발생하면 평균적인 lookup 기대와 다른 비용이 생길 수 있다. 일반 애플리케이션에서는 내장 hash 구현을 활용하고 임의의 약한 hash 함수를 직접 만들지 않는 편이 안전하다.

성능 분석에서는 `dict`가 평균 O(1)이라는 문장만 보지 말고 key 생성 비용, hash 비용, equality 비용, collision 분포를 함께 본다.

---

## CHAPTER 08 · comparison contract는 equality·ordering·hash·mutation을 하나로 묶는다

사용자 정의 값 타입을 설계할 때는 비교 메서드를 개별적으로 추가하지 않는다. 먼저 어떤 field가 identity가 아니라 value meaning을 구성하는지, 그 값이 immutable인지, ordering이 total인지 partial인지, 이질 타입을 어떻게 다룰지 표로 정리한다.

테스트도 예제 두 개보다 불변식 중심으로 만든다. 동일 의미 객체는 같은 hash를 갖는지, dict/set 삽입 뒤 key 의미가 변하지 않는지, unsupported type에는 `NotImplemented` 경로가 열리는지, 정렬이 필요한 곳에서 도메인 ordering과 display key가 섞이지 않는지 확인한다.

이 PART의 핵심은 **비교를 True/False 함수로 보지 않고, 타입 협력·값 의미·hash table 탐색·mutability까지 연결된 하나의 데이터 모델 계약으로 보는 것**이다.
