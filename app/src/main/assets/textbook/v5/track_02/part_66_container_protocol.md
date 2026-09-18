# PART 66 · Container protocol — length·truth·index·slice·membership을 하나의 계약으로 읽기

Python container는 `list`나 `dict` 같은 내장 타입만 뜻하지 않는다. 사용자 정의 객체도 `len(x)`, `x[i]`, `x[a:b]`, `item in x`, 반복문 같은 문법에 참여할 수 있다. 이때 편의 메서드를 여러 개 붙이는 것이 아니라 **각 문법이 어떤 프로토콜을 호출하고 서로 어떤 의미를 공유해야 하는지**를 설계해야 한다.

---

## CHAPTER 01 · `__len__`은 단순 숫자 반환이 아니라 container size 계약이다

### 시작 전 용어집

#### 1. __len__

- **뜻:** 그런 타입에 억지로 `__len__`을 제공하면 `len()`이 예상보다 비싸거나 state를 바꾸는 이상한 API가 된다.
- **왜 중요한가:** 길이를 O(1)에 제공할 수 있는지보다 **길이라는 개념이 안정적으로 존재하는지**를 먼저 판단한다.
- **예시:** 그런 타입에 억지로 `__len__`을 제공하면 `len()`이 예상보다 비싸거나 …

#### 2. container size

- **뜻:** `len(obj)`는 객체가 보고하는 논리적 원소 수를 사용한다.
- **왜 중요한가:** 반환값은 음수가 될 수 없고, 호출자가 이 값으로 반복 범위·버퍼 크기·빈 상태를 판단할 수 있으므로 순간적인 내부 구현값과 다르면 안 된다.
- **예시:** `len(obj)`는 객체가 보고하는 논리적 원소 수를 사용한다.

#### 3. 계약

- **뜻:** Lazy container라면 길이를 계산하기 위해 전체 stream을 소비해야 할 수도 있다.
- **왜 중요한가:** Cache를 사용하는 container라면 cached length와 실제 storage가 일치하는지 mutation path마다 검증한다.
- **예시:** Lazy container라면 길이를 계산하기 위해 전체 stream을 소비해야 …

#### 4. 객체

- **뜻:** 길이 정보가 틀리면 truth test와 slicing 최적화까지 연쇄적으로 오염될 수 있다.
- **예시:** 길이 정보가 틀리면 truth test와 slicing 최적화까지 연쇄적으로 …

---

## CHAPTER 02 · truth test는 `__bool__`이 없을 때 length로 fallback할 수 있다

### 시작 전 용어집

#### 1. truth test

- **뜻:** Truth test를 제공할 때는 호출자가 `if obj:`를 보고 어떤 질문에 대한 답이라고 이해할지 정한다.
- **왜 중요한가:** Container에서는 emptiness가 자연스럽지만 resource handle이나 query result에서는 명시적 property가 더 안전할 수 있다.
- **예시:** class QueueView: / def __init__(self, items):

#### 2. __bool__

- **뜻:** 이 객체는 별도 `__bool__` 없이도 비어 있으면 false-like, 원소가 있으면 true-like로 동작할 수 있다.
- **왜 중요한가:** 그러나 “연결됨”, “유효함”, “성공함” 같은 상태를 size와 섞으면 의미가 불분명해진다.
- **예시:** class QueueView: / def __init__(self, items):

#### 3. length

- **뜻:** 타입이 명시적 truth protocol을 제공하지 않으면 길이를 기반으로 빈/비어 있지 않음을 판단하는 fallback이 사용될 수 있다.
- **예시:** class QueueView: / def __init__(self, items):

`if obj:`는 반드시 `obj.__bool__()`만 호출하는 것이 아니다. 

```python
class QueueView:
    def __init__(self, items):
        self.items = items

    def __len__(self):
        return len(self.items)
```

 

 

---

## CHAPTER 03 · `__getitem__`은 integer index와 key access를 같은 문법에 연결한다

### 시작 전 용어집

#### 1. __getitem__

- **뜻:** 사용자 타입이 `__getitem__`을 구현하면 어떤 key 타입을 허용하고, 범위를 벗어난 경우 어떤 예외를 사용할지 계약해야 한다.
- **왜 중요한가:** Sequence-like 타입이라면 음수 index, `IndexError`, slice 처리와의 일관성을 고려한다.
- **예시:** class RecordList: / def __init__(self, rows):

#### 2. integer index

- **뜻:** `obj[key]`는 sequence index일 수도 있고 mapping key lookup일 수도 있다.
- **왜 중요한가:** Mapping-like 타입이라면 존재하지 않는 key에 `KeyError`가 자연스럽다.
- **예시:** class RecordList: / def __init__(self, rows):

#### 3. key access

- **뜻:** 잘못된 key type과 없는 key를 같은 예외로 뭉개면 caller가 실패 원인을 구분하기 어려워진다.
- **왜 중요한가:** Custom key normalization을 한다면 lookup 전에 어느 변환이 일어나는지 명확히 한다.
- **예시:** class RecordList: / def __init__(self, rows):

#### 4. 타입

- **뜻:** 예를 들어 문자열 key를 자동 lower-case하는 것은 편리하지만 원본 key identity를 잃을 수 있다.
- **예시:** class RecordList: / def __init__(self, rows):

```python
class RecordList:
    def __init__(self, rows):
        self._rows = list(rows)

    def __getitem__(self, index):
        return self._rows[index]
```

  

 

---

## CHAPTER 04 · slicing은 하나의 index가 아니라 범위 표현 객체를 전달한다

### 시작 전 용어집

#### 1. slicing

- **뜻:** `obj[1:5:2]`는 특수한 문법이지만 사용자 객체에는 slice 객체로 전달될 수 있다.
- **왜 중요한가:** 따라서 `__getitem__` 안에서 integer와 slice를 구분해 처리한다.
- **예시:** class Window: / def __init__(self, values):

#### 2. index

- **뜻:** 직접 산술로 index를 정규화하기보다 표준 slice semantics를 재사용하면 경계 오류를 줄일 수 있다.
- **왜 중요한가:** Slice 결과를 원본과 같은 타입으로 돌려줄지, 표준 list/tuple을 돌려줄지는 API 의미다.
- **예시:** class Window: / def __init__(self, values):

#### 3. 객체

- **뜻:** View를 반환한다면 원본 mutation이 결과에 반영되는지, snapshot을 반환한다면 비용이 얼마인지도 드러내야 한다.
- **왜 중요한가:** `start`, `stop`, `step`의 `None`과 음수 처리는 내장 sequence 기대와 어긋나지 않게 설계한다.
- **예시:** class Window: / def __init__(self, values):

```python
class Window:
    def __init__(self, values):
        self.values = tuple(values)

    def __getitem__(self, key):
        if isinstance(key, slice):
            return Window(self.values[key])
        return self.values[key]
```

 

 

---

**직접 확인하기 — CHAPTER 04 · slicing은 하나의 index가 아니라 범위 표현 객체를 전달한다**
CHAPTER 04 · slicing은 하나의 index가 아니라 범위 표현 객체를 전달한다의 규칙은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 확인하는 편이 정확하다. 먼저 입력이나 객체 하나만 두고 결과를 기록한 뒤, 값 하나 또는 호출 순서 하나만 바꿔 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 04 · slicing은 하나의 index가 아니라 범위 표현 객체를 전달한다이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 결과가 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 확인하고, 수정 뒤 같은 예제와 반대 조건 예제를 다시 실행한다. 이 과정을 설명할 수 있어야 문법을 외운 것이 아니라 동작 원리를 이해한 것이다.
## CHAPTER 05 · iteration은 `__iter__`가 우선이지만 sequence fallback을 이해해야 한다

### 시작 전 용어집

#### 1. iteration

- **뜻:** `for`, `list()`, unpacking, comprehension이 같은 iteration contract를 사용하므로 한 곳의 state bug가 여러 문법에서 반복된다.
- **왜 중요한가:** 일반적인 iterable은 `__iter__`를 제공하고 iterator object를 반환한다.
- **예시:** class Names: / def __init__(self, values):

#### 2. __iter__

- **뜻:** 그러나 새로운 타입에서는 의도를 명시적으로 드러내는 `__iter__` 구현이 더 읽기 쉽다.
- **왜 중요한가:** Iterator 자신이 iterable이면 `__iter__`가 보통 `self`를 반환한다.
- **예시:** class Names: / def __init__(self, values):

#### 3. sequence fallback

- **뜻:** 일부 sequence-style 객체는 정수 index를 0부터 증가시키는 방식으로 반복 가능한 fallback에 참여할 수 있다.
- **왜 중요한가:** Container와 iterator를 같은 객체로 만들면 한 번 소비한 뒤 재반복이 예상과 다를 수 있으므로 reusable collection인지 single-pass cursor인지 구분한다.
- **예시:** class Names: / def __init__(self, values):

```python
class Names:
    def __init__(self, values):
        self._values = tuple(values)

    def __iter__(self):
        return iter(self._values)
```

 


---

## CHAPTER 06 · membership은 `__contains__`가 있으면 의미와 성능을 직접 제어한다

### 시작 전 용어집

#### 1. membership

- **뜻:** `item in container`는 membership protocol을 사용한다.
- **왜 중요한가:** 타입이 `__contains__`를 제공하면 hash index, database index, interval test처럼 반복보다 더 적합한 알고리즘을 사용할 수 있다.
- **예시:** class RangeSet: / def __init__(self, low, high):

#### 2. __contains__

- **뜻:** Membership이 없을 때 iteration을 통한 fallback이 가능할 수 있지만, 무한 iterator나 expensive remote sequence에서는 위험하다.
- **왜 중요한가:** “포함 여부 확인”이 종료 가능하고 비용 예측 가능한 operation인지 먼저 판단한다.
- **예시:** class RangeSet: / def __init__(self, low, high):

#### 3. protocol

- **뜻:** Equality 기준도 container 의미와 맞아야 한다.
- **왜 중요한가:** Case-insensitive collection처럼 정규화된 membership을 제공한다면 iteration 결과와 `in`의 의미 차이를 문서화한다.
- **예시:** class RangeSet: / def __init__(self, low, high):

```python
class RangeSet:
    def __init__(self, low, high):
        self.low = low
        self.high = high

    def __contains__(self, value):
        return self.low <= value <= self.high
```

 

 

---

## CHAPTER 07 · mutation 중 iteration은 fail-fast·snapshot·live-view 중 하나를 선택해야 한다

### 시작 전 용어집

#### 1. mutation

- **뜻:** 안전한 선택지는 version counter로 mutation을 감지해 실패시키거나, iterator 생성 시 snapshot을 만들거나, immutable container로 제한하는 것이다.
- **왜 중요한가:** 각 선택은 memory, latency, freshness 비용이 다르다.
- **예시:** 안전한 선택지는 version counter로 mutation을 감지해 실패시키거나, iterator …

#### 2. iteration

- **뜻:** Thread-safe container라고 해도 한 번의 iteration 전체가 atomic하다는 뜻은 아니다.
- **왜 중요한가:** Container를 순회하는 동안 같은 container가 수정되면 무엇이 일어나는지 타입마다 다르다.
- **예시:** Thread-safe container라고 해도 한 번의 iteration 전체가 atomic하다는 …

#### 3. fail-fast

- **뜻:** 일부는 오류를 내고, 일부는 snapshot을 순회하며, 일부는 변화가 보이는 live view를 제공한다.
- **왜 중요한가:** Custom container에서는 이 동작을 우연에 맡기지 않는다.
- **예시:** 일부는 오류를 내고, 일부는 snapshot을 순회하며, 일부는 변화가 …

#### 4. snapshot

- **뜻:** Iterator가 내부 배열 index를 직접 들고 있는데 중간 삽입/삭제가 일어나면 원소를 건너뛰거나 중복 방문할 수 있다.
- **왜 중요한가:** Concurrency가 들어오면 문제는 더 커진다.
- **예시:** Iterator가 내부 배열 index를 직접 들고 있는데 중간 …

필요한 consistency 수준을 명시한다.

---

## CHAPTER 08 · container contract는 문법별 메서드가 아니라 관찰 가능한 의미를 통합한다

### 시작 전 용어집

#### 1. container contract

- **뜻:** 좋은 container는 `len`, truth, index, slice, iteration, membership이 서로 모순되지 않는다.
- **왜 중요한가:** 길이가 0인데 iteration에서 원소가 나오거나, `x in c`는 True인데 순회 결과에서는 절대 같은 equality 의미로 찾을 수 없다면 caller의 기본 가정이 깨진다.
- **예시:** 좋은 container는 `len`, truth, index, slice, iteration, membership이 …

#### 2. 상태

- **뜻:** 빈/비어 있지 않은 상태, 음수 index, slice 경계, membership, 반복 재사용, mutation 중 반복을 한 시나리오로 연결해 본다.
- **왜 중요한가:** 이 PART의 핵심은 **container protocol을 여러 magic method의 모음으로 보지 않고, 객체가 크기·접근·반복·포함·변경을 어떤 일관된 의미로 제공하는지 검증하는 통합 계약으로 보는 것**이다.
- **예시:** 빈/비어 있지 않은 상태, 음수 index, slice 경계, …

#### 3. 경계

- **뜻:** 테스트는 각 메서드를 따로 호출하는 수준을 넘는다.
- **예시:** 테스트는 각 메서드를 따로 호출하는 수준을 넘는다.

---

## 실전 학습 루프 · container protocol

### 1. 쉬운 예

상자 안의 물건을 `len(box)`, `x in box`, `box[i]`, `for x in box`처럼 다루려면 각 문법이 기대하는 protocol을 구현해야 한다. 모든 기능을 한 번에 넣기보다 컨테이너가 실제로 약속할 연산만 노출하는 편이 안전하다.

### 2. 한 줄 해석

Python의 컨테이너 문법은 숨은 마법이 아니라 특정 special method를 호출하는 protocol의 조합이다.

### 3. 직접 실행

아래 코드는 개념을 작게 격리한 예다. 실행 전에 출력이나 상태 변화를 먼저 예상한 뒤 실제 결과와 비교한다.

```python
class Bag:
    def __init__(self, items):
        self.items = list(items)
    def __len__(self):
        return len(self.items)
    def __contains__(self, item):
        return item in self.items

bag = Bag([1, 2, 3])
print(len(bag), 2 in bag)
```

결과가 예상과 다르면 문법부터 고치지 말고, **어떤 protocol·상태·계약이 호출됐는지**를 한 단계씩 확인한다. 이렇게 해야 “우연히 동작하는 코드”와 “이유를 설명할 수 있는 코드”를 구분할 수 있다.

### 4. 수정 실습

1. `__iter__`를 추가해 for 문이 어떤 경로로 동작하는지 확인한다.
2. `__getitem__`만 남겨도 일부 반복이 가능한 이유와 한계를 확인한다.

수정 후에는 정상 입력 하나만 보지 말고 빈 값, 경계값, 반복 호출, 예외 경로 중 해당되는 반례를 최소 하나 추가한다.

### 5. 확인 문제

`len(obj)`가 동작한다고 해서 `for x in obj`도 반드시 동작할까?

### 6. 정답과 오답 설명

**정답:** 아니다. 길이 protocol과 iteration protocol은 별개의 계약이다.

**자주 나오는 오답:** “컨테이너면 전부 같은 메서드를 가진다”는 생각이 오답의 출발점이다. 문법마다 필요한 protocol을 따로 본다.

마지막으로 코드를 다시 읽으면서 **입력 → 호출되는 규칙 → 상태 변화 → 결과/예외** 네 칸으로 요약한다. 이 네 칸을 설명할 수 있으면 단순 암기가 아니라 실행 모델을 이해한 것이다.

