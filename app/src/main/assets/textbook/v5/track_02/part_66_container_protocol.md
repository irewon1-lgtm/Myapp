# TRACK 02 · P66 — Container protocol: len·truth·getitem·contains·iteration fallback을 추적하기

Python의 container는 특정 base class를 상속해야만 되는 개념이 아니다. `len(x)`, `x[i]`, `value in x`, `for value in x` 같은 문법이 서로 다른 특수 메서드와 fallback 규칙을 통해 객체에 능력을 부여한다. 그래서 `__getitem__` 하나를 구현했을 뿐인데 iteration까지 되는 것처럼 보이거나, `__len__` 때문에 객체의 bool 값이 결정되는 일이 생긴다.

이 PART의 목표는 list처럼 보이는 클래스를 만드는 것이 아니다. **각 문법이 어느 프로토콜을 먼저 보고, 어떤 fallback이 열리며, 종료 신호가 무엇인지**를 분리해서 추적하는 것이다.

---

## 1. Length contract — `len()`은 음수가 아닌 정수라는 강한 계약이다

`len(obj)`는 `obj.__len__()`의 의미와 연결된다. 하지만 아무 숫자나 반환하면 되는 것이 아니다. 길이는 음수가 될 수 없고 Python이 요구하는 정수 범위와 계약을 따라야 한다.

```python
class Batch:
    def __init__(self, rows):
        self.rows = list(rows)

    def __len__(self):
        return len(self.rows)

batch = Batch([10, 20, 30])
print(len(batch))  # 3
```

길이를 “추정치”나 “전체 서버 레코드 수일 수도 있음” 같은 모호한 값으로 사용하면 호출자가 잘못된 가정을 하게 된다. `len()`은 보통 **현재 객체가 표현하는 collection의 cardinality**로 읽힌다.

또 `__len__` 계산이 매우 비싸다면 API 의미와 비용 모델도 어긋날 수 있다. 호출자는 `len(x)`를 비교적 기본적인 조회로 생각하는 경우가 많다.

따라서 lazy remote collection에서 매번 네트워크 count 쿼리를 실행하도록 `__len__`를 구현하는 대신 명시적 `count_remote()` 같은 API가 더 나을 수 있다.

Container 프로토콜은 단순히 “가능한가?”가 아니라 **문법이 암시하는 비용과 의미까지 맞는가?**를 봐야 한다.

---

## 2. Truth fallback — `__bool__`이 없으면 길이가 진실값에 참여할 수 있다

`if obj:`는 반드시 `obj.__bool__()`만 보는 것이 아니다. 타입이 `__bool__`을 제공하지 않으면 `__len__` 결과가 truthiness 결정에 사용될 수 있다.

```python
class Inbox:
    def __init__(self, messages):
        self.messages = list(messages)

    def __len__(self):
        return len(self.messages)

empty = Inbox([])
full = Inbox(["hello"])

print(bool(empty))  # False
print(bool(full))   # True
```

이 fallback은 편리하지만 의미를 의식해야 한다. “연결 객체가 살아 있는가?”와 “현재 읽을 메시지가 있는가?”는 다른 질문이다.

```python
if connection:
    ...
```

이 표현이 연결 생존 여부를 뜻한다고 기대했는데 `__len__`가 큐 길이를 반환하면 빈 큐 상태에서 `False`가 된다. 그래서 객체 truthiness가 모호하면 `is_connected`, `has_items` 같은 명시적 상태가 더 안전하다.

`__bool__`을 직접 제공할 때도 반환 의미를 하나로 고정해야 한다. 사용 시점마다 “유효함”, “비어 있지 않음”, “성공함”을 섞으면 읽는 사람이 조건문의 의미를 추적할 수 없다.

---

## 3. `__getitem__` — index 접근은 key 해석 규칙을 공개한다

`obj[key]`는 `__getitem__` 프로토콜과 연결된다.

```python
class Ledger:
    def __init__(self, rows):
        self._rows = list(rows)

    def __getitem__(self, index):
        return self._rows[index]
```

이 짧은 구현은 정수 index뿐 아니라 내부 list가 지원하는 slice까지 우연히 전달한다. 하지만 사용자 타입이 어떤 key 종류를 지원하는지는 의도적으로 정해야 한다.

```python
ledger[0]
ledger[-1]
ledger[1:4]
```

각 표현이 모두 의미 있는지 검토한다. 예를 들어 시간순 로그에서 음수 index가 “뒤에서부터”라는 관례와 잘 맞을 수 있지만, pagination cursor 객체에 음수 index를 허용하는 것은 이상할 수 있다.

또 sequence 스타일의 `__getitem__`에서 범위를 벗어난 정수 index는 `IndexError`로 끝나는 것이 중요하다. 이 종료 신호는 뒤의 iteration fallback과도 연결된다.

key가 문자열인 mapping형 객체라면 `KeyError`가 더 자연스럽다. 즉 예외 타입도 container 종류의 의미를 전달한다.

---

## 4. Slicing protocol — slice 객체의 start·stop·step을 해석한다

`obj[2:10:2]`가 호출되면 key는 마법의 세 인수가 아니라 `slice` 객체다.

```python
class Window:
    def __init__(self, values):
        self.values = list(values)

    def __getitem__(self, key):
        if isinstance(key, slice):
            return Window(self.values[key])
        return self.values[key]
```

사용자 타입은 slice 결과 타입도 결정해야 한다. 원래 타입을 유지할지, 내장 list를 반환할지, view를 반환할지에 따라 identity와 비용이 달라진다.

큰 배열이나 파일-backed 데이터에서는 slice가 복사인지 view인지가 특히 중요하다.

```text
copy slice:
- 독립된 새 데이터
- 메모리 비용 증가
- 원본 변경 영향 없음

view slice:
- 원본 저장소 공유
- 낮은 복사 비용
- 원본 lifetime/mutation과 결합
```

`slice.indices(length)`를 이용하면 `None`, 음수, 범위를 벗어난 값을 구체적 인덱스로 정규화할 수 있다. 하지만 정규화 전에 자신의 컨테이너가 Python sequence와 같은 음수/step semantics를 정말 제공할 것인지 먼저 결정해야 한다.

문법을 지원하는 순간 사용자는 내장 sequence와 비슷한 기대를 갖기 때문이다.

---

## 5. Iteration fallback — `__iter__`가 없어도 sequence 방식으로 반복될 수 있다

가장 자주 놓치는 부분 중 하나다. 객체가 명시적 `__iter__`를 제공하지 않아도 오래된 sequence protocol을 만족하면 iteration이 가능할 수 있다. 개념적으로 Python은 `__getitem__(0)`, `__getitem__(1)`처럼 증가시키다가 `IndexError`를 만나 종료하는 경로를 사용할 수 있다.

```python
class LegacySequence:
    def __init__(self, values):
        self.values = list(values)

    def __getitem__(self, index):
        if index >= len(self.values):
            raise IndexError
        return self.values[index]

for item in LegacySequence(["a", "b", "c"]):
    print(item)
```

여기서 종료 시 `None`을 반환하거나 `KeyError`를 던지면 기대한 sequence iteration 계약과 맞지 않는다.

현대 코드에서는 반복 의미가 있다면 대개 `__iter__`를 명시적으로 구현하는 편이 읽기 쉽다.

```python
    def __iter__(self):
        return iter(self.values)
```

중요한 교훈은 **특수 메서드 하나가 다른 문법에 간접 참여할 수 있다**는 점이다. 그래서 프로토콜 구현 전에는 그 메서드의 fallback 경로까지 확인해야 한다.

---

## 6. Membership — `in`은 전용 프로토콜과 fallback을 가진다

`x in container`는 가능하면 `__contains__`를 이용한다.

```python
class RoleSet:
    def __init__(self, roles):
        self.roles = set(roles)

    def __contains__(self, role):
        return role in self.roles
```

전용 membership 구현이 없으면 iteration이나 sequence 접근을 통해 확인될 수 있다. 기능상 같은 결과가 나와도 비용은 크게 달라질 수 있다.

예를 들어 내부에 set index가 있는데도 `__contains__` 없이 모든 요소를 순회한다면 membership이 O(n)으로 동작할 수 있다.

반대로 membership이 원격 API 조회를 의미한다면 `if user in directory:`라는 문법이 네트워크 호출을 숨길 수 있다. 이는 기능적으로 가능하지만 비용과 실패 경계를 숨긴다는 문제가 있다.

프로토콜 문법을 채택할 때는 다음을 확인한다.

- lookup이 local인지 remote인지
- 평균 비용이 호출자 기대와 맞는지
- 실패가 단순 False인지 예외인지
- equality 기준이 container의 의미와 맞는지

“Python답다”는 이유만으로 모든 기능을 연산자 문법에 숨기는 것은 좋은 API가 아니다.

---

## 7. Mutation during iteration — 반복 중 구조 변경의 의미를 정한다

Container가 mutable이면 iteration 중 변경이 새로운 문제를 만든다.

```python
values = [1, 2, 3, 4]
for value in values:
    if value % 2 == 0:
        values.remove(value)
```

이런 코드는 index 이동 때문에 요소를 건너뛰는 등 예상하기 어려운 결과를 만들 수 있다. 어떤 내장 컨테이너는 구조 변경을 감지해 오류를 내고, 어떤 경우에는 변경된 상태를 그대로 관찰한다.

사용자 정의 container/iterator에서도 정책이 필요하다.

1. **live iterator** — 원본 변경을 이후 반복에서 관찰
2. **snapshot iterator** — 시작 시점 데이터를 복사해 안정적 반복
3. **fail-fast** — structural version을 기록하고 변경 시 예외

```python
class SnapshotBag:
    def __iter__(self):
        return iter(tuple(self._items))
```

snapshot은 semantics가 명확하지만 복사 비용이 있다. live는 비용이 낮아도 mutation 의미를 문서화해야 한다.

동시성까지 들어오면 thread/task synchronization 문제가 추가된다. 따라서 반복 안정성을 “iterator 메서드 한 줄”의 문제로 보지 말고 **container state lifetime**의 문제로 봐야 한다.

---

## 8. Container contract — 문법 묶음이 서로 모순되지 않아야 한다

Container를 설계할 때는 지원할 프로토콜을 표로 먼저 고정하는 것이 좋다.

```text
len(x)            지원 / 현재 로컬 요소 수
bool(x)           len 기반 / 비어 있으면 False
x[i]              정수 index + slice
negative index    지원
iteration         명시적 __iter__
value in x        __contains__ / 내부 set index 사용
mutation          append/remove 허용
iteration policy  snapshot
remote I/O        없음
```

이 표를 만들면 `__getitem__`를 추가하면서 뜻하지 않게 iteration까지 열거나, `len`이 remote I/O를 숨기는 문제를 미리 발견할 수 있다.

테스트도 문법별 예시가 아니라 프로토콜 사이의 관계를 본다.

```python
assert len(box) == sum(1 for _ in box)
assert bool(box) == (len(box) != 0)
for value in box:
    assert value in box
```

물론 모든 타입이 이 성질을 반드시 만족해야 하는 것은 아니다. 핵심은 자신의 타입이 어떤 관계를 약속하는지 의도적으로 결정하는 것이다.

Container 프로토콜의 리뷰 질문은 다음으로 압축된다.

- length와 truthiness의 의미가 일관적인가?
- index/slice 예외가 sequence 또는 mapping 의미와 맞는가?
- `__getitem__` fallback으로 원치 않는 iteration을 열지 않았는가?
- membership 비용과 실패가 문법에 숨겨져 있지 않은가?
- mutation과 iterator lifetime 정책이 명확한가?

---

## 직관 봉인

- `len()`은 단순 숫자 반환이 아니라 cardinality 계약이다.
- `__bool__`이 없으면 `__len__`가 truthiness를 결정할 수 있다.
- `__getitem__`는 index뿐 아니라 slice와 과거 sequence iteration 경로에도 영향을 줄 수 있다.
- `in`은 `__contains__`만의 문법이 아니며 fallback 경로가 있다.
- 반복 중 mutation은 iterator 구현이 아니라 state lifetime 정책 문제다.
- 프로토콜 문법은 기능뿐 아니라 비용과 실패를 숨길 수 있으므로 의미가 맞을 때만 열어야 한다.

## 다음 연결

다음 PART에서는 `obj.name`이라는 짧은 표현 뒤의 **attribute lookup routing**을 추적한다. `__getattribute__`와 `__getattr__`의 역할 차이, 재귀 함정, 쓰기·삭제 경로, 그리고 P58에서 배운 descriptor precedence가 전체 lookup pipeline 안에서 어디에 놓이는지 연결한다.
