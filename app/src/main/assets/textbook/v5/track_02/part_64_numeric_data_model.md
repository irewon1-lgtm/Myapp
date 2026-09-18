# PART 64 · Numeric data model — 연산자·NotImplemented·정수 프로토콜을 실행 순서로 읽기

Python의 `+`, `*`, `//`, `%`, `int(x)`는 단순 기호가 아니다. 객체가 어떤 연산에 참여할 수 있는지, 서로 다른 타입이 만났을 때 어느 쪽 구현을 먼저 시도하는지, 변환이 손실 없는 정수를 요구하는지까지 데이터 모델의 규칙으로 결정된다. 숫자 타입을 제대로 설계하려면 메서드 이름을 외우기보다 **표현식 → 후보 선택 → 반환값 → fallback 또는 실패**의 순서를 추적해야 한다.

---

## CHAPTER 01 · numeric protocol은 연산자의 의미를 타입 계약으로 바꾼다

### 시작 전 용어집

#### 1. numeric protocol

- **뜻:** __add__(b)`의 축약으로 보면 이질 타입 연산에서 틀린 결론을 내리기 쉽다.
- **왜 중요한가:** Python은 피연산자의 타입 관계, 정방향/역방향 특수 메서드, `NotImplemented` 반환 여부를 함께 보고 최종 연산 경로를 정한다.
- **예시:** class Meter: / def __init__(self, value):

#### 2. 타입

- **뜻:** 돈, 거리, 각도처럼 도메인 의미가 있는 숫자 타입이라면 “연산 가능”과 “연산 의미가 타당함”을 분리해야 한다.
- **왜 중요한가:** 첫째 사용자가 쓴 표현식, 둘째 참여 가능한 특수 메서드, 셋째 실제 디스패치 순서, 넷째 결과가 지켜야 할 도메인 불변식이다.
- **예시:** class Meter: / def __init__(self, value):

#### 3. 계약

- **뜻:** 핵심은 `__add__`가 모든 입력을 처리해야 하는 함수가 아니라는 점이다.
- **왜 중요한가:** 자신이 의미를 정의할 수 있는 조합만 처리하고 나머지는 프로토콜에 돌려준다.
- **예시:** class Meter: / def __init__(self, value):

#### 4. class

- **뜻:** 이 네 층을 분리하면 연산자가 실행됐지만 의미는 틀린 버그를 잡기 쉬워진다.
- **예시:** class Meter: / def __init__(self, value):

`a + b`를 단순히 `a. 

```python
class Meter:
    def __init__(self, value):
        self.value = float(value)

    def __add__(self, other):
        if isinstance(other, Meter):
            return Meter(self.value + other.value)
        return NotImplemented
```

  

실전에서는 네 층으로 읽는다.  

---

## CHAPTER 02 · `__index__`는 `__int__`보다 강한 정수 계약이다

### 시작 전 용어집

#### 1. __index__

- **뜻:** 이 경계를 표현하는 프로토콜이 `__index__`다.
- **왜 중요한가:** 반대로 온도나 비율처럼 정수화 과정에서 절삭·반올림이 발생할 수 있는 값은 `__int__`는 의미가 있어도 `__index__`는 과도한 약속일 수 있다.
- **예시:** class Page: / def __init__(self, number):

#### 2. __int__

- **뜻:** `int(x)`는 값을 정수로 변환하라는 명시적 요청이다.
- **왜 중요한가:** 반면 리스트 인덱스, 슬라이스, 일부 비트·크기 문맥은 손실 없이 정확한 정수로 해석 가능한 객체를 요구한다.
- **예시:** class Page: / def __init__(self, number):

#### 3. 계약

- **뜻:** `__index__`를 구현한다는 것은 “이 객체는 정확한 정수 위치·개수·비트값으로 사용해도 의미가 보존된다”는 강한 계약이다.
- **왜 중요한가:** 따라서 편의를 위해 두 메서드를 동시에 구현하지 않는다.
- **예시:** class Page: / def __init__(self, number):

#### 4. 객체

- **뜻:** 타입의 본질이 exact integer인지, 단지 정수로 투영할 수 있는 값인지 먼저 판단한다.
- **예시:** class Page: / def __init__(self, number):

```python
class Page:
    def __init__(self, number):
        self.number = number

    def __index__(self):
        return self.number

pages = ["intro", "model", "protocol"]
print(pages[Page(1)])
```

 

 

---

## CHAPTER 03 · forward와 reverse operator는 서로 다른 타입이 협력하는 두 번째 기회를 만든다

### 시작 전 용어집

#### 1. forward

- **뜻:** 사용자 정의 타입과 내장 숫자를 섞으면 정방향 메서드만으로는 표현식 양쪽 순서를 모두 처리하기 어렵다.
- **왜 중요한가:** `v * 3`과 `3 * v`는 같은 수학적 결과를 원할 수 있지만 메서드 후보는 다르다.
- **예시:** class Vector: / def __init__(self, x, y):

#### 2. reverse operator

- **뜻:** `__radd__`, `__rmul__`의 `r`은 결과를 뒤집는다는 뜻이 아니라 반대편 타입이 같은 표현식에 참여할 수 있는 reverse candidate라는 뜻이다.
- **왜 중요한가:** 또한 하위 타입이 더 구체적인 의미를 제공할 때는 단순한 “항상 왼쪽 먼저” 규칙만으로 설명되지 않는 우선순위가 생길 수 있다.
- **예시:** class Vector: / def __init__(self, x, y):

#### 3. 타입

- **뜻:** 디버깅할 때는 표현식, 좌우 타입, 각 후보 반환값, 최종 결과 타입을 기록한다.
- **왜 중요한가:** 실제 호출 경로를 보면 상속·확장 타입이 끼어든 경우도 명확해진다.
- **예시:** class Vector: / def __init__(self, x, y):

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

 

 

---

**직접 확인하기 — CHAPTER 03 · forward와 reverse operator는 서로 다른 타입이 협력하는 두 번째 기회를 만든다**
CHAPTER 03 · forward와 reverse operator는 서로 다른 타입이 협력하는 두 번째 기회를 만든다의 규칙은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 확인하는 편이 정확하다. 먼저 입력이나 객체 하나만 두고 결과를 기록한 뒤, 값 하나 또는 호출 순서 하나만 바꿔 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 03 · forward와 reverse operator는 서로 다른 타입이 협력하는 두 번째 기회를 만든다이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 결과가 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 확인하고, 수정 뒤 같은 예제와 반대 조건 예제를 다시 실행한다. 이 과정을 설명할 수 있어야 문법을 외운 것이 아니라 동작 원리를 이해한 것이다.
## CHAPTER 04 · `NotImplemented`는 오류가 아니라 디스패치 계속 신호다

### 시작 전 용어집

#### 1. NotImplemented

- **뜻:** `NotImplemented`는 예외가 아니라 특별한 값이다.
- **왜 중요한가:** 연산 메서드가 이 값을 반환하면 “이 타입 조합의 의미를 내가 결정하지 않겠다”는 뜻이고 Python은 다른 합법적인 후보를 시도할 수 있다.
- **예시:** class Money: / def __init__(self, won):

#### 2. 오류

- **뜻:** `return NotImplemented`, `raise NotImplementedError`, `raise TypeError`는 전혀 다르다.
- **왜 중요한가:** 첫 번째는 fallback을 허용하고, 두 번째는 일반적으로 기능이 구현되지 않았음을 알리는 예외이며, 세 번째는 현재 지점에서 실패를 확정한다.
- **예시:** class Money: / def __init__(self, won):

#### 3. 타입

- **뜻:** 미래 타입이 역연산으로 협력할 수 있으려면 앞선 타입이 모르는 조합을 조기에 `TypeError`로 닫지 않아야 한다.
- **왜 중요한가:** 잘못된 값과 단지 지원하지 않는 타입 조합도 구분해야 한다.
- **예시:** class Money: / def __init__(self, won):

```python
class Money:
    def __init__(self, won):
        self.won = won

    def __add__(self, other):
        if isinstance(other, Money):
            return Money(self.won + other.won)
        return NotImplemented
```

 

이 구분은 확장 가능성과 직접 연결된다.  

---

## CHAPTER 05 · in-place operator는 identity 유지 여부까지 포함한 계약이다

### 시작 전 용어집

#### 1. in-place operator

- **뜻:** `a += b`를 무조건 “a 내부를 수정한다”로 이해하면 안 된다.
- **왜 중요한가:** 타입은 `__iadd__` 같은 in-place 후보를 제공할 수 있고, 그렇지 않으면 일반 이항 연산으로 fallback할 수 있다.
- **예시:** items = [1, 2] / alias = items

#### 2. identity

- **뜻:** 결과 객체의 identity가 유지되는지도 타입에 따라 다르다.
- **왜 중요한가:** Mutable 타입의 `__iadd__`가 `self`를 수정하고 반환한다면 모든 alias에서 변화가 관찰된다.
- **예시:** items = [1, 2] / alias = items

#### 3. 계약

- **뜻:** 반대로 immutable value object라면 새 객체를 반환하는 일반 연산이 의미에 더 맞을 수 있다.
- **예시:** items = [1, 2] / alias = items

```python
items = [1, 2]
alias = items
items += [3]
print(alias)  # 같은 list가 수정되므로 [1, 2, 3]

text = "ab"
old = text
text += "c"
print(old)    # str은 immutable이므로 old는 "ab"
```

 

따라서 제자리 연산은 단순 성능 최적화가 아니다. **연산 전후에 동일 객체를 유지할 것인가, 다른 참조가 그 변화를 보게 할 것인가**를 결정하는 API 의미다.

---

**직접 확인하기 — CHAPTER 05 · in-place operator는 identity 유지 여부까지 포함한 계약이다**
CHAPTER 05 · in-place operator는 identity 유지 여부까지 포함한 계약이다의 규칙은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 확인하는 편이 정확하다. 먼저 입력이나 객체 하나만 두고 결과를 기록한 뒤, 값 하나 또는 호출 순서 하나만 바꿔 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 05 · in-place operator는 identity 유지 여부까지 포함한 계약이다이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 결과가 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 확인하고, 수정 뒤 같은 예제와 반대 조건 예제를 다시 실행한다. 이 과정을 설명할 수 있어야 문법을 외운 것이 아니라 동작 원리를 이해한 것이다.
## CHAPTER 06 · conversion protocol은 값 표현의 손실 경계를 드러내야 한다

### 시작 전 용어집

#### 1. conversion protocol

- **뜻:** `int(x)`, `float(x)`, `complex(x)` 같은 변환은 내부 필드를 꺼내는 편의 기능이 아니라 다른 표현 체계로 값을 투영하는 경계다.
- **왜 중요한가:** 변환 과정에서 정밀도, 단위, 범위 정보가 사라질 수 있다.
- **예시:** class Ratio: / def __init__(self, numerator, denominator):

#### 2. 경계

- **뜻:** `Ratio(1, 3)`을 float로 바꾸면 근삿값이 되고, `Ratio(5, 2)`를 int로 바꾸면 소수 정보가 사라진다.
- **왜 중요한가:** 암묵적 변환을 과도하게 허용하면 호출자가 정보 손실 시점을 보기 어렵다.
- **예시:** class Ratio: / def __init__(self, numerator, denominator):

#### 3. class

- **뜻:** 좋은 변환 계약은 손실 가능성, 허용 범위, 오류 조건을 명시한다.
- **왜 중요한가:** 특히 돈·측정값·고정소수점 타입은 단순 내장 숫자로의 변환이 불변식을 무너뜨리지 않는지 먼저 본다.
- **예시:** class Ratio: / def __init__(self, numerator, denominator):

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

 이런 손실이 도메인상 허용되는지 문서화해야 한다. 

 

---

## CHAPTER 07 · floor division과 modulo는 음수에서도 하나의 불변식으로 읽는다

### 시작 전 용어집

#### 1. floor division

- **뜻:** 하지만 floor division은 아래쪽 정수로 내려가고 modulo는 위의 관계를 만족하도록 결정된다.
- **왜 중요한가:** 나머지의 부호 규칙도 divisor와의 관계 안에서 읽어야 한다.
- **예시:** x == (x // y) * y + …

#### 2. modulo

- **뜻:** `//`와 `%`를 각각 암기하면 음수가 들어왔을 때 실수하기 쉽다.
- **왜 중요한가:** Python 정수 연산에서는 다음 관계를 함께 생각하는 편이 안전하다.
- **예시:** x == (x // y) * y + …

#### 3. 불변식

- **뜻:** 0 방향 절삭만 생각하면 `-7 // 3`을 `-2`라고 예상하기 쉽다.
- **왜 중요한가:** 사용자 정의 숫자 타입에서 `__floordiv__`와 `__mod__`를 따로 구현할 때도 둘의 결과가 상호 모순되지 않아야 한다.
- **예시:** x == (x // y) * y + …

#### 4. 타입

- **뜻:** `divmod()`와 관련 메서드를 지원한다면 동일한 몫·나머지 의미를 공유해야 한다.
- **왜 중요한가:** 경계값 테스트에는 양/음 피제수, 양/음 제수, 정확히 나누어지는 경우, 0 근처 값을 포함한다.
- **예시:** x == (x // y) * y + …

```text
x == (x // y) * y + (x % y)
```

예를 들어:

```python
print(-7 // 3)  # -3
print(-7 % 3)   # 2
```

  

 

 숫자 프로토콜은 예제 한두 개보다 algebraic invariant 검사가 더 강하다.

---

## CHAPTER 08 · numeric type contract는 연산 가능 범위와 불변식을 동시에 고정한다

### 시작 전 용어집

#### 1. numeric type

- **뜻:** 사용자 정의 숫자 타입이 안정적이려면 연산자 목록보다 먼저 의미 표를 만든다.
- **왜 중요한가:** 어떤 타입끼리 더할 수 있는지, reverse operation을 허용하는지, in-place 연산이 identity를 유지하는지, 어떤 변환에서 정보가 손실되는지, equality와 hash는 무엇을 의미하는지까지 한 계약으로 본다.
- **예시:** 예를 들어 `Money` 타입이라면 같은 통화끼리의 덧셈은 허용하되 …

#### 2. contract

- **뜻:** 예를 들어 `Money` 타입이라면 같은 통화끼리의 덧셈은 허용하되 통화가 다른 경우 자동 환산을 숨길지 명시적 conversion을 요구할지 정해야 한다.
- **왜 중요한가:** `Duration`과 `Timestamp`라면 `Timestamp + Duration`은 의미가 있지만 `Timestamp + Timestamp`는 거부하는 편이 자연스럽다.
- **예시:** 예를 들어 `Money` 타입이라면 같은 통화끼리의 덧셈은 허용하되 …

#### 3. 불변식

- **뜻:** 이 PART의 핵심은 **숫자 연산자를 문법 기호가 아니라 여러 타입이 협상하는 프로토콜로 보고, `NotImplemented`·reverse dispatch·identity·정수 정확성·몫/나머지 불변식을 하나의 실행 계약으로 연결하는 것**이다.
- **왜 중요한가:** 지원 조합은 정확한 결과 타입과 값을 확인하고, 미지원 조합은 다른 피연산자의 reverse candidate가 참여할 수 있는지 확인하며, 모든 후보가 거부했을 때 최종 실패를 확인한다.
- **예시:** 이 PART의 핵심은 **숫자 연산자를 문법 기호가 아니라 …

테스트도 메서드별이 아니라 표현식별로 만든다.
