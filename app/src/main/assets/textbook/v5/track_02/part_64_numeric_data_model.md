# TRACK 02 · P64 — Numeric data model: 연산자·NotImplemented·정수 프로토콜을 추적하기

Python에서 `+`, `*`, `//`, `%`, `int(x)` 같은 표현은 단순한 문법 기호가 아니다. 객체가 어떤 연산에 참여할 수 있는지, 두 피연산자의 타입이 다를 때 누구에게 먼저 기회를 줄지, 변환이 정확한 정수를 요구하는지 손실을 허용하는지까지 데이터 모델의 계약으로 결정된다.

이 PART의 목표는 특수 메서드 이름을 외우는 것이 아니다. **표현식 → 디스패치 후보 → 반환값 → fallback 또는 실패**의 순서를 따라가면서 사용자 정의 숫자 타입이 내장 숫자와 섞였을 때도 예측 가능한 의미를 갖게 만드는 것이다.

---

## 1. Numeric protocol — 연산자는 메서드 호출 이상의 디스패치 규칙이다

`a + b`를 보면 흔히 `a.__add__(b)`라고만 생각한다. 출발점으로는 맞지만 완전한 모델은 아니다. Python은 두 타입의 관계와 특수 메서드 구현 여부, 반환된 값이 `NotImplemented`인지까지 보고 다음 후보를 선택한다.

```python
class Meter:
    def __init__(self, value):
        self.value = float(value)

    def __add__(self, other):
        if isinstance(other, Meter):
            return Meter(self.value + other.value)
        return NotImplemented
```

여기서 중요한 것은 `__add__`가 **모든 입력을 처리해야 하는 함수**가 아니라는 점이다. 자신이 의미를 정의할 수 있는 조합만 처리하고, 모르는 조합에는 `NotImplemented`를 돌려줘야 한다.

숫자 프로토콜을 읽을 때는 연산을 네 층으로 분리하면 좋다.

1. **표현식 층** — `a + b`, `a * b`, `a // b`처럼 사용자가 쓴 문법
2. **프로토콜 층** — `__add__`, `__radd__`, `__mul__`, `__floordiv__` 같은 후보
3. **디스패치 층** — 어느 타입의 구현을 먼저 시도하고 언제 반대편으로 넘길지
4. **의미 층** — 단위, 범위, 정밀도, 불변식이 무엇인지

이 네 층을 섞으면 “메서드는 호출됐는데 결과 의미가 틀린” 타입을 만들기 쉽다. 예를 들어 돈 타입이 `float`와 무조건 더해지도록 허용하면 프로토콜은 작동해도 통화와 반올림 규칙이라는 도메인 의미가 무너질 수 있다.

숫자 타입 설계의 첫 질문은 therefore “어떤 메서드를 구현할까?”가 아니라 **“어떤 타입 조합의 연산을 의미 있다고 인정할까?”**다.

---

## 2. `__index__`와 `__int__` — 정수처럼 보이는 것과 정확한 정수인 것은 다르다

`int(x)`는 값을 정수로 변환하려는 요청이다. 반면 인덱스, 슬라이스 길이, 일부 저수준 정수 문맥은 “손실 없이 정확한 정수로 해석 가능한 객체”를 요구한다. 이 경계를 표현하는 프로토콜이 `__index__`다.

```python
class Page:
    def __init__(self, number):
        self.number = number

    def __index__(self):
        return self.number

pages = ["intro", "model", "protocol"]
print(pages[Page(1)])       # model
print(hex(Page(15)))        # 0xf
```

`__index__`는 결과로 실제 `int`를 반환해야 한다. “대충 정수로 바꿀 수 있다”는 의미가 아니다.

반대로 다음과 같은 타입은 정수 변환은 허용하되 인덱스로 쓰이면 위험할 수 있다.

```python
class Temperature:
    def __init__(self, celsius):
        self.celsius = celsius

    def __int__(self):
        return int(self.celsius)
```

`int(Temperature(21.8))`은 도메인에서 명시적으로 허용한 절삭 변환일 수 있다. 그러나 이 객체가 리스트 인덱스로도 자연스럽다는 뜻은 아니다. 그래서 무심코 `__index__`까지 추가하면 타입의 의미 범위를 넓혀 버린다.

판별 기준은 간단하다.

- 값이 본질적으로 **정확한 정수 개수/위치/비트값**이라면 `__index__` 후보
- 값이 변환 과정에서 절삭·반올림·도메인 해석을 거친다면 대개 `__int__`만 고려
- `__index__` 구현은 편의 기능이 아니라 “이 객체는 정수 문맥에 손실 없이 들어갈 수 있다”는 강한 계약

`bool`이 `int`의 하위 타입이라는 언어 특성처럼 Python 숫자 계층에는 역사적·구현적 세부도 있다. 따라서 “동작한다”와 “내 타입의 의미에 맞다”를 분리해서 판단해야 한다.

---

## 3. Forward / reverse operator — 왼쪽 실패 뒤 오른쪽에게 기회가 간다

서로 다른 타입의 연산에서 핵심은 정방향과 역방향 메서드의 협력이다.

```python
class Vector:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    def __mul__(self, other):
        if isinstance(other, (int, float)):
            return Vector(self.x * other, self.y * other)
        return NotImplemented

    def __rmul__(self, other):
        if isinstance(other, (int, float)):
            return Vector(other * self.x, other * self.y)
        return NotImplemented
```

이제 `v * 3`은 `Vector.__mul__`이 처리하고, `3 * v`는 왼쪽 타입이 처리하지 못한 뒤 `Vector.__rmul__`이 참여할 수 있다.

여기서 `__radd__`의 `r`은 “결과를 뒤집는다”는 뜻이 아니다. **반대편 피연산자 관점에서 같은 표현식에 참여하는 reverse candidate**라는 뜻이다.

또 하나 중요한 점은 하위 타입 관계다. 오른쪽 피연산자 타입이 왼쪽 타입의 적절한 하위 타입이고 역연산을 별도로 제공한다면, 더 구체적인 타입의 의미를 존중하기 위해 역방향 구현이 우선될 수 있다. 즉 단순히 “항상 왼쪽 먼저, 실패하면 오른쪽”이라고 암기하면 일부 상속 관계에서 틀린다.

연산자 디스패치를 디버깅할 때는 로그를 이런 형태로 남기면 좋다.

```text
expression: a + b
left type: A
right type: B
A.__add__(B): NotImplemented
B.__radd__(A): value
result type: B
```

중요한 것은 어느 메서드가 존재하느냐보다 **실제로 어느 후보가 선택되어 어떤 값을 반환했느냐**다.

---

## 4. `NotImplemented` dispatch — “지원하지 않음”과 “오류”를 구분한다

`NotImplemented`는 예외가 아니다. 특별한 싱글턴 값이다. 이 값을 반환하면 인터프리터에게 “이 타입 조합은 내가 처리하지 않으니 다른 합법적인 디스패치 경로를 시도하라”고 알린다.

```python
class Money:
    def __init__(self, won):
        self.won = won

    def __add__(self, other):
        if isinstance(other, Money):
            return Money(self.won + other.won)
        return NotImplemented
```

다음 세 개를 혼동하면 안 된다.

- `return NotImplemented` — 이 연산 조합을 처리하지 않음, 프로토콜 fallback 허용
- `raise NotImplementedError` — 호출된 기능 자체가 아직 구현되지 않았다는 일반 예외
- `raise TypeError(...)` — 현재 지점에서 연산을 오류로 종료

이 차이는 확장성에 직접 영향을 준다. 왼쪽 타입이 알지 못하는 미래의 타입이 오른쪽 역연산으로 협력하려면 왼쪽은 조기에 `TypeError`를 던지지 말고 `NotImplemented`를 반환해야 한다.

```python
class Discount:
    def __radd__(self, money):
        if isinstance(money, Money):
            return Money(money.won - 1000)
        return NotImplemented
```

프로토콜이 허용하는 경로를 끝까지 시도했는데도 어느 쪽도 처리하지 못하면 그때 Python이 적절한 `TypeError`를 만든다. 즉 사용자 타입이 불필요하게 최종 오류 메시지의 책임까지 선점하지 않는 편이 조합 가능성이 높다.

리뷰할 때는 다음 질문을 던져야 한다.

- “모르는 타입”에 예외를 던지고 있지 않은가?
- 역연산 후보가 참여할 기회를 막고 있지 않은가?
- 정말 잘못된 **값**과 단지 지원하지 않는 **타입 조합**을 구분했는가?

---

## 5. In-place operator — `+=`는 반드시 제자리 mutation을 뜻하지 않는다

`a += b`를 보면 “a 내부가 수정된다”고 생각하기 쉽다. 하지만 프로토콜 관점에서는 먼저 `__iadd__` 같은 in-place 후보가 있고, 지원하지 않으면 일반 이항 연산으로 fallback할 수 있다. 결과 객체가 원래 객체와 동일한지도 타입 설계에 따라 다르다.

```python
items = [1, 2]
before = id(items)
items += [3]
print(id(items) == before)   # 보통 True: list는 제자리 확장

text = "ab"
before = id(text)
text += "c"
print(id(text) == before)    # 보통 False: str은 새 객체
```

따라서 `+=`의 의미를 이해하려면 **문법이 아니라 타입의 mutability와 in-place 구현**을 봐야 한다.

사용자 정의 mutable 타입에서 `__iadd__`를 구현한다면 보통 자신을 수정한 뒤 `self`를 반환한다.

```python
class Bag:
    def __init__(self, values=()):
        self.values = list(values)

    def __iadd__(self, other):
        self.values.extend(other)
        return self
```

하지만 여기에는 alias 문제가 생긴다.

```python
a = Bag([1])
b = a
a += [2]
# b도 같은 Bag을 가리키므로 변화가 보인다.
```

반대로 immutable value object라면 `__iadd__`를 굳이 제공하지 않고 일반 `__add__`가 새 객체를 반환하게 두는 편이 더 명확할 수 있다.

즉 제자리 연산 설계는 성능 최적화 문제가 아니라 **identity와 alias를 관찰 가능한 계약으로 만들 것인가**의 문제다.

---

## 6. Conversion protocol — 변환은 “값을 꺼내기”가 아니라 의미를 바꾸는 경계다

숫자 객체는 여러 변환 문맥에 참여할 수 있다. 대표적으로 `int(x)`, `float(x)`, `complex(x)`와 정확한 정수 문맥의 `__index__`가 있다.

```python
class Ratio:
    def __init__(self, numerator, denominator):
        if denominator == 0:
            raise ValueError("denominator must not be zero")
        self.n = numerator
        self.d = denominator

    def __float__(self):
        return self.n / self.d

    def __int__(self):
        return int(self.n / self.d)
```

여기서 `float(Ratio(1, 3))`은 표현 가능한 부동소수점 근삿값으로 투영된다. `int(Ratio(5, 2))`는 정수로 좁히면서 정보가 사라진다. 이 손실은 호출자가 명시적으로 `int(...)`를 썼기 때문에 비교적 드러나지만, 암묵 변환을 넓게 허용하면 손실 경계가 숨는다.

좋은 변환 계약은 다음 세 가지를 명확히 한다.

1. **손실 가능성** — 소수부, 단위, 정밀도가 사라지는가?
2. **예외 조건** — 범위 초과나 정의 불가능한 변환은 무엇인가?
3. **왕복 성질** — `T(convert(x))`가 원래 의미를 보존해야 하는가?

특히 금액, 시간, 측정 단위처럼 정밀도가 중요한 타입은 편의를 위해 `__float__`를 추가하기 전에 오차가 도메인 계약을 깨지 않는지 검토해야 한다.

변환 특수 메서드는 “print를 편하게” 만드는 장치가 아니다. **타입 간 의미 경계를 공개 API로 여는 행위**다.

---

## 7. Floor division과 modulo — 음수에서 불변식을 확인해야 한다

Python의 `//`는 단순히 0 방향으로 버리는 정수 나눗셈이 아니다. floor, 즉 수학적 바닥 방향으로 내려간다.

```python
print(7 // 3)    # 2
print(-7 // 3)   # -3
print(7 // -3)   # -3
```

`%` 역시 이 의미와 맞물린다. 핵심 불변식은 일반적인 유한 수 문맥에서 다음 관계를 유지하는 것이다.

```text
a == (a // b) * b + (a % b)
```

직접 확인해 보자.

```python
a, b = -7, 3
q = a // b       # -3
r = a % b        # 2
assert a == q * b + r
```

C 계열 언어의 정수 나눗셈 습관이나 “부호만 대충 맞추면 된다”는 직관을 그대로 가져오면 음수 경계에서 오류가 생긴다. 사용자 정의 숫자 타입이 `__floordiv__`, `__mod__`, `__divmod__`를 제공한다면 세 연산의 관계를 함께 테스트해야 한다.

예를 들어 기간 구간, 페이지 묶음, 좌표 격자처럼 음수가 실제로 등장하는 도메인에서는 양수 예제만 통과한 구현이 운영에서 깨질 수 있다.

검증은 예시 몇 개보다 성질을 기준으로 하는 편이 강하다.

```python
for a in range(-20, 21):
    for b in range(-5, 6):
        if b == 0:
            continue
        q, r = divmod(a, b)
        assert a == q * b + r
```

숫자 프로토콜에서는 이런 **대수적 불변식**이 단위 테스트의 좋은 축이 된다.

---

## 8. Numeric type contract — 메서드 목록보다 닫힌 의미 체계를 설계한다

사용자 정의 숫자 타입이 안정적이려면 개별 특수 메서드가 아니라 전체 계약을 함께 봐야 한다.

예를 들어 `Money` 타입을 만든다고 하자. 다음 질문에 답하지 못한 채 `__add__`부터 구현하면 나중에 타입 의미가 흔들린다.

- 같은 통화끼리만 더할 수 있는가?
- `Money + int`의 `int`는 원 단위 금액인가, 금지할 것인가?
- 곱셈은 `Money * scalar`만 허용할 것인가?
- 나눗셈 결과는 `Money`인가 비율인가?
- equality와 hash는 통화까지 포함하는가?
- `int(money)`가 허용된다면 소수 통화 단위는 어떻게 처리하는가?
- `__index__`까지 제공할 이유가 실제로 있는가?
- `+=`가 객체 identity를 유지해야 하는가?

이 질문은 모두 같은 문제를 가리킨다. **타입이 허용하는 연산 집합이 도메인의 의미와 닫혀 있는가?**

구현 리뷰용 체크리스트를 압축하면 다음과 같다.

```text
[ ] 지원 타입 조합과 거부 타입 조합이 명시돼 있다.
[ ] 모르는 조합은 필요하면 NotImplemented로 넘긴다.
[ ] forward/reverse 연산이 비대칭 의미를 정확히 표현한다.
[ ] in-place 연산의 identity/alias 효과가 의도적이다.
[ ] __int__와 __index__를 편의상 같이 구현하지 않는다.
[ ] 변환의 정밀도 손실과 예외 조건이 문서화돼 있다.
[ ] //, %, divmod 사이의 불변식을 음수까지 검증한다.
[ ] 결과 타입이 연산의 도메인 의미와 일치한다.
```

숫자 프로토콜의 품질은 “`1 + x`가 실행된다”로 판단할 수 없다. 섞이는 타입, 실패하는 타입, 음수, alias, 정밀도 손실까지 포함해 **표현식 전체의 의미가 예측 가능해야 한다.**

---

## 직관 봉인

- 연산자 오버로딩은 문법 장식이 아니라 **다중 후보 디스패치 계약**이다.
- `__index__`는 `__int__`의 편의 버전이 아니라 **손실 없는 정수 자격**이다.
- `return NotImplemented`와 `raise NotImplementedError`는 전혀 다른 의미다.
- `+=`는 항상 mutation이 아니다. identity가 유지되는지는 타입 계약을 봐야 한다.
- 변환 메서드는 값 노출이 아니라 타입 의미의 경계를 연다.
- `//`와 `%`는 음수까지 포함한 대수적 관계로 검증해야 한다.

## 다음 연결

다음 PART에서는 숫자 연산에서 한 단계 확장해 **비교와 hash contract**를 다룬다. `==`가 어떤 타입과 협력하는지, ordering이 왜 부분적일 수 있는지, mutable 상태를 hash에 넣으면 왜 set/dict의 탐색 전제가 무너지는지를 데이터 모델 수준에서 추적한다.
