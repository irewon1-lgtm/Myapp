# PART 65 · Rich comparison과 hash contract — equality·ordering·dict/set 불변식을 연결하기

비교 연산은 화면에 `==`, `<`, `>=` 같은 기호로 보이지만 실제 계약은 더 넓다. 사용자 정의 객체가 `dict`와 `set`의 key가 되기 시작하면 equality와 hash가 서로 맞물리고, mutable 상태가 hash에 들어가거나 이질 타입 비교를 조기에 확정하면 컬렉션의 기본 가정이 깨질 수 있다. 이 절에서는 비교를 **후보 선택 → 지원 여부 → 의미 판단 → hash table lookup**의 순서로 읽는다.

---

## CHAPTER 01 · comparison dispatch는 여섯 비교 연산의 후보 선택 규칙이다

### 시작 전 용어집

#### 1. comparison

- **뜻:** Python rich comparison에는 `__lt__`, `__le__`, `__eq__`, `__ne__`, `__gt__`, `__ge__`가 있다.
- **왜 중요한가:** 핵심은 메서드 이름을 외우는 것이 아니라 서로 다른 타입이 만났을 때 어느 구현이 의미를 결정하는지 추적하는 것이다.
- **예시:** class Version: / def __init__(self, major, minor):

#### 2. 타입

- **뜻:** `v1 < v2`를 볼 때는 좌우 타입, 왼쪽 후보의 지원 여부, 반사 관계의 비교 후보, 최종 실패 형태를 확인한다.
- **왜 중요한가:** 상속 관계가 있으면 더 구체적인 타입이 자신의 의미를 제공할 기회도 고려된다.
- **예시:** class Version: / def __init__(self, major, minor):

#### 3. class

- **뜻:** 비교 버그는 코드 한 줄보다 타입 관계에서 발생한다.
- **왜 중요한가:** “항상 왼쪽 메서드가 결정한다”는 단순 모델을 버리고 실제 디스패치 경로를 본다.
- **예시:** class Version: / def __init__(self, major, minor):

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

 

 

---

**검증 시나리오 P65-C01 — CHAPTER 01 · comparison dispatch는 여섯 비교 연산의 후보 선택 규칙이다**
`CHAPTER 01 · comparison dispatch는 여섯 비교 연산의 후보 선택 규칙이다` 검증은 경계값을 먼저 지정하는 데서 시작한다. P65-C1에서는 `CHAPTER 01 · comparison dispatch는 여섯 비교 연산의 후보 선택 규칙이다` 실행 직전 상태를 먼저 적고, 실행 뒤 얻은 값과 비교해 어떤 규칙이 실제로 적용됐는지 확인한다. 두 번째 단계에서는 `CHAPTER 01 · comparison dispatch는 여섯 비교 연산의 후보 선택 규칙이다`에 대해 정상값과 경계값을 연속 실행고, 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 01 · comparison dispatch는 여섯 비교 연산의 후보 선택 규칙이다`의 타입·정체성·수명 변화를 분리 기록하여 결과만 맞는 우연한 통과를 배제한다. 예상이 빗나가면 `CHAPTER 01 · comparison dispatch는 여섯 비교 연산의 후보 선택 규칙이다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P65-C1의 마무리는 결과를 설명할 수 있을 때 종료하는 것이다. 통과 기준은 `CHAPTER 01 · comparison dispatch는 여섯 비교 연산의 후보 선택 규칙이다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고, 같은 절차를 반복했을 때 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 02 · equality는 어떤 필드가 객체의 의미를 구성하는지 결정한다

### 시작 전 용어집

#### 1. equality

- **뜻:** 여기서는 `label`을 표시용 메타데이터로 보고 equality에서 제외했다.
- **왜 중요한가:** 이 결정은 hash, cache key, deduplication에도 그대로 영향을 준다.
- **예시:** class Coordinate: / def __init__(self, x, y, label=None):

#### 2. 객체

- **뜻:** 서로 다른 객체가 같은 값을 나타낼 수 있고, 같은 객체라도 도메인 규칙상 특수한 비교 의미를 가질 수 있다.
- **왜 중요한가:** 일반 값 객체는 대칭성·추이성 같은 성질을 지키는 편이 좋지만 NaN처럼 예외적인 모델도 있으므로 자신의 타입 의미를 먼저 정의한다.
- **예시:** class Coordinate: / def __init__(self, x, y, label=None):

#### 3. class

- **뜻:** `__eq__` 구현에서 가장 어려운 부분은 True/False를 반환하는 코드가 아니라 “무엇을 같다고 볼 것인가”를 정의하는 일이다.
- **왜 중요한가:** `is`는 identity이고 `==`는 타입이 정의한 value equality다.
- **예시:** class Coordinate: / def __init__(self, x, y, label=None):

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

 

  

---

## CHAPTER 03 · ordering은 반드시 total order일 필요가 없다

### 시작 전 용어집

#### 1. ordering

- **뜻:** total_ordering`도 일부 비교를 합성하는 도구일 뿐 잘못된 수학적 의미를 교정해 주지는 않는다.
- **왜 중요한가:** 모든 값이 자연스럽게 한 줄로 정렬되는 것은 아니다.
- **예시:** class CapabilitySet: / def __init__(self, names):

#### 2. total order

- **뜻:** 권한 집합, 버전 범위, 그래프 관계처럼 일부 쌍만 비교 가능한 도메인에서는 부분 순서가 더 정확할 수 있다.
- **왜 중요한가:** 서로 포함되지 않는 두 집합에 임의로 `<`나 `>`를 부여하면 UI 정렬 편의 때문에 도메인 의미를 왜곡한다.
- **예시:** class CapabilitySet: / def __init__(self, names):

```python
class CapabilitySet:
    def __init__(self, names):
        self.names = frozenset(names)

    def __le__(self, other):
        if not isinstance(other, CapabilitySet):
            return NotImplemented
        return self.names <= other.names
```

여기서 `a <= b`는 포함 관계다. 

표시 순서가 필요하면 `sorted(..., key=...)`로 별도 기준을 둔다. `functools.

---

**검증 시나리오 P65-C03 — CHAPTER 03 · ordering은 반드시 total order일 필요가 없다**
`CHAPTER 03 · ordering은 반드시 total order일 필요가 없다` 검증은 호출 순서를 단순화하는 데서 시작한다. P65-C3에서는 `CHAPTER 03 · ordering은 반드시 total order일 필요가 없다` 실행 직전 상태를 먼저 적고, 실행 뒤 얻은 값과 비교해 어떤 규칙이 실제로 적용됐는지 확인한다. 두 번째 단계에서는 `CHAPTER 03 · ordering은 반드시 total order일 필요가 없다`에 대해 순서 하나만 뒤집어 차이를 본다고, 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 03 · ordering은 반드시 total order일 필요가 없다`의 호출 전후의 상태 전이를 번호로 남긴다하여 결과만 맞는 우연한 통과를 배제한다. 예상이 빗나가면 `CHAPTER 03 · ordering은 반드시 total order일 필요가 없다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P65-C3의 마무리는 다른 순서에서도 계약이 유지되는지 확인하는 것이다. 통과 기준은 `CHAPTER 03 · ordering은 반드시 total order일 필요가 없다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고, 같은 절차를 반복했을 때 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 04 · comparison에서 `NotImplemented`는 False와 다르다

### 시작 전 용어집

#### 1. comparison

- **뜻:** 이질 타입을 만났을 때 무조건 `False`를 반환하면 다른 타입이 비교 의미를 제공할 기회를 막을 수 있다.
- **왜 중요한가:** `NotImplemented`는 “둘이 다르다”가 아니라 “이 구현은 이 타입 조합을 판단하지 않는다”는 신호다.
- **예시:** class UserId: / def __init__(self, value):

#### 2. NotImplemented

- **뜻:** Python은 반대쪽 후보 등 프로토콜이 허용하는 경로를 더 볼 수 있다.
- **왜 중요한가:** 특히 extension type이나 adapter type과 상호운용할 때 중요하다.
- **예시:** class UserId: / def __init__(self, value):

#### 3. False

- **뜻:** 먼저 만들어진 타입이 모르는 미래 타입까지 `False`로 확정하면 확장 가능성이 떨어진다.
- **왜 중요한가:** 지원하지 않는 비교와 실제로 서로 다른 값을 구분한다.
- **예시:** class UserId: / def __init__(self, value):

```python
class UserId:
    def __init__(self, value):
        self.value = value

    def __eq__(self, other):
        if not isinstance(other, UserId):
            return NotImplemented
        return self.value == other.value
```

 

  

---

**검증 시나리오 P65-C04 — CHAPTER 04 · comparison에서 `NotImplemented`는 False와 다르다**
`CHAPTER 04 · comparison에서 `NotImplemented`는 False와 다르다` 검증은 입력 크기를 고정하는 데서 시작한다. P65-C4에서는 `CHAPTER 04 · comparison에서 `NotImplemented`는 False와 다르다` 실행 직전 상태를 먼저 적고, 실행 뒤 얻은 값과 비교해 어떤 규칙이 실제로 적용됐는지 확인한다. 두 번째 단계에서는 `CHAPTER 04 · comparison에서 `NotImplemented`는 False와 다르다`에 대해 변형은 한 요소만 허용고, 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 04 · comparison에서 `NotImplemented`는 False와 다르다`의 측정값을 여러 번 모아 분포를 비교하여 결과만 맞는 우연한 통과를 배제한다. 예상이 빗나가면 `CHAPTER 04 · comparison에서 `NotImplemented`는 False와 다르다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P65-C4의 마무리는 워밍업과 측정 자체의 비용을 분리하는 것이다. 통과 기준은 `CHAPTER 04 · comparison에서 `NotImplemented`는 False와 다르다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고, 같은 절차를 반복했을 때 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 05 · hash와 equality는 같은 의미 필드를 공유해야 한다

### 시작 전 용어집

#### 1. hash

- **뜻:** Hashable 객체에서 가장 중요한 불변식은 다음이다.
- **왜 중요한가:** Hash table은 hash로 후보 위치를 좁힌 뒤 equality를 다시 확인한다.
- **예시:** if a == b: / hash(a) == hash(b)

#### 2. equality

- **뜻:** Equality가 `(x, y)`를 의미로 삼았으므로 hash도 같은 필드를 사용한다.
- **왜 중요한가:** Equality에서 제외한 field를 hash에 넣으면 같은 객체라고 판단하면서 서로 다른 bucket 후보를 만들 수 있어 계약이 깨진다.
- **예시:** if a == b: / hash(a) == hash(b)

#### 3. 객체

- **뜻:** 사용자 클래스가 equality를 새로 정의했을 때 Python이 hashability를 보수적으로 다루는 이유도 이 불변식을 보호하기 위해서다.
- **예시:** if a == b: / hash(a) == hash(b)

```text
if a == b:
    hash(a) == hash(b)
```

역은 필요 없다. hash collision은 허용된다. 

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

 


---

**검증 시나리오 P65-C05 — CHAPTER 05 · hash와 equality는 같은 의미 필드를 공유해야 한다**
`CHAPTER 05 · hash와 equality는 같은 의미 필드를 공유해야 한다` 검증은 성공 사례를 기준선으로 저장하는 데서 시작한다. P65-C5에서는 `CHAPTER 05 · hash와 equality는 같은 의미 필드를 공유해야 한다` 실행 직전 상태를 먼저 적고, 실행 뒤 얻은 값과 비교해 어떤 규칙이 실제로 적용됐는지 확인한다. 두 번째 단계에서는 `CHAPTER 05 · hash와 equality는 같은 의미 필드를 공유해야 한다`에 대해 실패 조건은 하나만 주입고, 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 05 · hash와 equality는 같은 의미 필드를 공유해야 한다`의 로그 시각과 상태 식별자를 맞춰 본다하여 결과만 맞는 우연한 통과를 배제한다. 예상이 빗나가면 `CHAPTER 05 · hash와 equality는 같은 의미 필드를 공유해야 한다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P65-C5의 마무리는 성공·실패 모두 결정적으로 끝나는지 확인하는 것이다. 통과 기준은 `CHAPTER 05 · hash와 equality는 같은 의미 필드를 공유해야 한다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고, 같은 절차를 반복했을 때 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 06 · mutable hash는 key를 논리적으로 잃어버리게 만들 수 있다

### 시작 전 용어집

#### 1. mutable

- **뜻:** 해결책은 hash 의미 상태를 immutable로 만들거나, mutable 객체를 unhashable로 두거나, 별도의 immutable key object를 사용하는 것이다.
- **왜 중요한가:** `dataclass(frozen=True)` 같은 도구는 이 의도를 표현하는 데 도움이 되지만 field 자체의 깊은 mutability까지 자동 해결하지는 않는다.
- **예시:** class MutableKey: / def __init__(self, code):

#### 2. hash

- **뜻:** Hash 계산에 참여하는 상태를 `dict`나 `set` 삽입 뒤 바꾸면 탐색 경로가 달라질 수 있다.
- **왜 중요한가:** 삽입할 때와 조회할 때 hash가 달라질 수 있으므로 컬렉션이 기대한 위치와 현재 객체 의미가 어긋난다.
- **예시:** class MutableKey: / def __init__(self, code):

#### 3. key

- **뜻:** 같은 Python object를 들고 있어도 key lookup이 실패할 수 있는 위험한 상태다.
- **예시:** class MutableKey: / def __init__(self, code):

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

 

 

---

## CHAPTER 07 · dict/set lookup은 hash 한 번으로 끝나지 않는다

### 시작 전 용어집

#### 1. dict

- **뜻:** `dict`와 `set`은 hash value를 이용해 후보 영역을 찾지만 collision 때문에 equality 확인도 필요하다.
- **왜 중요한가:** 따라서 느린 `__eq__`나 불안정한 `__hash__`는 데이터 구조 전체의 성능과 correctness에 영향을 준다.
- **예시:** `dict`와 `set`은 hash value를 이용해 후보 영역을 찾지만 …

#### 2. set

- **뜻:** Key가 복잡한 network object graph를 따라가며 equality를 계산하거나 hash를 매번 비싼 방식으로 만들면 lookup 비용이 커진다.
- **왜 중요한가:** Immutable value object라면 필요한 경우 hash 결과를 캐시할 수 있지만, 캐시가 의미 상태와 불일치하지 않는지 보장해야 한다.
- **예시:** Key가 복잡한 network object graph를 따라가며 equality를 계산하거나 …

#### 3. set lookup

- **뜻:** 또 악의적이거나 편향된 입력에서 많은 collision이 발생하면 평균적인 lookup 기대와 다른 비용이 생길 수 있다.
- **왜 중요한가:** 일반 애플리케이션에서는 내장 hash 구현을 활용하고 임의의 약한 hash 함수를 직접 만들지 않는 편이 안전하다.
- **예시:** 또 악의적이거나 편향된 입력에서 많은 collision이 발생하면 평균적인 …

#### 4. hash

- **뜻:** 성능 분석에서는 `dict`가 평균 O(1)이라는 문장만 보지 말고 key 생성 비용, hash 비용, equality 비용, collision 분포를 함께 본다.
- **예시:** 성능 분석에서는 `dict`가 평균 O(1)이라는 문장만 보지 말고 …

---

## CHAPTER 08 · comparison contract는 equality·ordering·hash·mutation을 하나로 묶는다

### 시작 전 용어집

#### 1. comparison

- **뜻:** 사용자 정의 값 타입을 설계할 때는 비교 메서드를 개별적으로 추가하지 않는다.
- **왜 중요한가:** 먼저 어떤 field가 identity가 아니라 value meaning을 구성하는지, 그 값이 immutable인지, ordering이 total인지 partial인지, 이질 타입을 어떻게 다룰지 표로 정리한다.
- **예시:** 사용자 정의 값 타입을 설계할 때는 비교 메서드를 …

#### 2. equality

- **뜻:** 동일 의미 객체는 같은 hash를 갖는지, dict/set 삽입 뒤 key 의미가 변하지 않는지, unsupported type에는 `NotImplemented` 경로가 열리는지, 정렬이 필요한 곳에서 도메인 ordering과 display key가 섞이지 않는지 확인한다.
- **왜 중요한가:** 이 PART의 핵심은 **비교를 True/False 함수로 보지 않고, 타입 협력·값 의미·hash table 탐색·mutability까지 연결된 하나의 데이터 모델 계약으로 보는 것**이다.
- **예시:** 동일 의미 객체는 같은 hash를 갖는지, dict/set 삽입 …

테스트도 예제 두 개보다 불변식 중심으로 만든다.

---

## 실전 학습 루프 · 비교와 hash 계약

### 1. 쉬운 예

회원 객체 두 개가 같은 회원번호를 갖는다고 하자. 화면에서는 같은 회원으로 취급하면서 set에서는 서로 다른 원소로 남는다면 equality와 hash 규칙이 서로 어긋난 것이다. 비교 결과는 단순한 편의 기능이 아니라 dict·set 같은 자료구조의 동작을 결정한다.

### 2. 한 줄 해석

`a == b`가 참인 객체는 hash 기반 컨테이너에서도 같은 논리적 키로 취급되도록 `__eq__`와 `__hash__`의 계약을 함께 설계한다.

### 3. 직접 실행

아래 코드는 개념을 작게 격리한 예다. 실행 전에 출력이나 상태 변화를 먼저 예상한 뒤 실제 결과와 비교한다.

```python
class User:
    def __init__(self, user_id):
        self.user_id = user_id
    def __eq__(self, other):
        return isinstance(other, User) and self.user_id == other.user_id
    def __hash__(self):
        return hash(self.user_id)

print(len({User(7), User(7)}))
```

결과가 예상과 다르면 문법부터 고치지 말고, **어떤 protocol·상태·계약이 호출됐는지**를 한 단계씩 확인한다. 이렇게 해야 “우연히 동작하는 코드”와 “이유를 설명할 수 있는 코드”를 구분할 수 있다.

### 4. 수정 실습

1. `user_id`를 생성 뒤 바꿀 수 있게 만든 다음 set 조회가 왜 위험해지는지 확인한다.
2. `__eq__`만 구현하고 `__hash__` 정책을 정하지 않았을 때 Python이 어떤 선택을 하는지 확인한다.

수정 후에는 정상 입력 하나만 보지 말고 빈 값, 경계값, 반복 호출, 예외 경로 중 해당되는 반례를 최소 하나 추가한다.

### 5. 확인 문제

논리적으로 같은 객체의 hash 값이 서로 달라도 괜찮을까?

### 6. 정답과 오답 설명

**정답:** 안 된다. 같은 객체라고 비교되는 값은 같은 hash를 가져야 hash table이 같은 후보 위치에서 찾을 수 있다.

**자주 나오는 오답:** “충돌만 없으면 된다”는 답은 틀리다. hash 충돌은 허용되지만 equality와 hash의 방향 계약은 깨지면 안 된다.

마지막으로 코드를 다시 읽으면서 **입력 → 호출되는 규칙 → 상태 변화 → 결과/예외** 네 칸으로 요약한다. 이 네 칸을 설명할 수 있으면 단순 암기가 아니라 실행 모델을 이해한 것이다.

