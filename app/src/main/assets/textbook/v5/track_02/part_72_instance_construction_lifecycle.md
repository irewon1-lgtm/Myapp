# PART 72 · Instance construction lifecycle — `__new__`·`__init__`·immutable subclass를 분리해서 이해하기

Python에서 `ClassName(...)`을 호출하면 단순히 `__init__` 함수가 실행되는 것으로 끝나지 않는다. 새 객체를 실제로 만들고 반환하는 단계와 이미 만들어진 객체의 상태를 초기화하는 단계가 분리되어 있다. 대부분의 클래스에서는 이 차이를 의식하지 않아도 되지만 immutable built-in을 상속하거나 instance caching, validation, factory-like construction을 구현할 때는 `__new__`와 `__init__`의 역할을 정확히 알아야 한다.

---

## CHAPTER 01 · `__new__`는 instance를 만들고 반환하는 allocation 단계다

### 시작 전 용어집

#### 1. __new__

- **뜻:** `__new__`는 class를 첫 인수로 받고 실제 instance를 생성해 반환하는 단계다.
- **왜 중요한가:** 일반 mutable user class에서는 `object.
- **예시:** class Token: / def __new__(cls, value):

#### 2. instance

- **뜻:** 이 둘을 “constructor 한 함수”로 합쳐 생각하면 immutable subclass와 instance reuse 패턴을 이해하기 어렵다.
- **왜 중요한가:** `__new__`에서 다른 타입의 object를 반환할 수 있다는 점도 중요하다.
- **예시:** class Token: / def __new__(cls, value):

#### 3. allocation

- **뜻:** __new__(cls)`가 충분하고 직접 override할 일이 많지 않다.
- **왜 중요한가:** 여기서 `__new__`가 객체 identity를 결정하고 `__init__`은 그 객체 상태를 채운다.
- **예시:** class Token: / def __new__(cls, value):

#### 4. class

- **뜻:** 이 경우 일반적인 `__init__` 흐름이 달라질 수 있으므로 factory semantics를 숨길 때는 매우 신중해야 한다.
- **예시:** class Token: / def __new__(cls, value):

```python
class Token:
    def __new__(cls, value):
        obj = super().__new__(cls)
        return obj

    def __init__(self, value):
        self.value = value
```

 

 

---

## CHAPTER 02 · `__init__`은 이미 존재하는 instance를 초기화하며 새 값을 반환하지 않는다

### 시작 전 용어집

#### 1. __init__

- **뜻:** `__init__`의 역할은 생성된 instance가 유효한 상태가 되도록 초기화하는 것이다.
- **왜 중요한가:** 일반 함수처럼 의미 있는 값을 반환하는 곳이 아니다.
- **예시:** class Range: / def __init__(self, start, end):

#### 2. instance

- **뜻:** 일반적으로 새 instance마다 한 번 초기화되는 계약을 명확히 유지하는 편이 좋다.
- **왜 중요한가:** Validation이 실패하면 partially initialized object가 외부로 노출되지 않도록 해야 한다.
- **예시:** class Range: / def __init__(self, start, end):

#### 3. 상태

- **뜻:** 외부 resource를 획득한 뒤 뒤쪽 validation이 실패하는 구조라면 cleanup도 고려한다.
- **왜 중요한가:** `__init__`이 여러 번 호출될 가능성이 있는 unusual code나 framework가 있다면 idempotency를 가정하지 않는다.
- **예시:** class Range: / def __init__(self, start, end):

```python
class Range:
    def __init__(self, start, end):
        if start > end:
            raise ValueError("start must be <= end")
        self.start = start
        self.end = end
```

 

 

---

**실행 점검 72-2 — CHAPTER 02 · `__init__`은 이미 존재하는 instance를 초기화하며 새 값을 반환하지 않는다**
CHAPTER 02 · `__init__`은 이미 존재하는 instance를 초기화하며 새 값을 반환하지 않는다을 이해할 때는 경계값 한 개를 골라 변경 전후의 반환값과 부수효과를 따로 확인한다. 그다음 값 하나, 호출 순서 하나, 경계 조건 하나만 바꿔 CHAPTER 02 · `__init__`은 이미 존재하는 instance를 초기화하며 새 값을 반환하지 않는다의 규칙이 실제 상태에 어떤 변화를 만드는지 확인한다. PART 72 CHAPTER 2에서는 결과를 맞히는 것보다 예상과 실제의 차이를 설명하는 과정이 중요하다. 우연히 통과한 한 번의 실행보다 반복 가능한 관찰을 통과 기준으로 삼는다. 마지막에는 타입·정체성·수명·예외 경계 중 어느 요소가 결과를 결정했는지 자기 문장으로 정리해, 같은 규칙을 다른 코드에서도 알아볼 수 있는지 확인한다.
## CHAPTER 03 · immutable built-in subclass는 값 자체를 `__new__` 단계에서 결정해야 한다

### 시작 전 용어집

#### 1. immutable

- **뜻:** `int`, `str`, `tuple` 같은 immutable 타입의 핵심 값은 instance가 생성된 뒤 `__init__`에서 바꾸기 어렵다.
- **왜 중요한가:** 따라서 subtype의 실제 값을 결정하려면 `__new__`가 필요할 수 있다.
- **예시:** class UserId(str): / def __new__(cls, value):

#### 2. class

- **뜻:** Immutable subclass에 별도 metadata attribute를 추가할 수 있는 경우도 있지만 핵심 값과 부가 상태를 구분한다.
- **왜 중요한가:** Equality/hash가 base immutable value를 따르는지, metadata까지 포함해야 하는지 별도로 결정한다.
- **예시:** class UserId(str): / def __new__(cls, value):

#### 3. __new__

- **뜻:** 이후 `__init__`에서 원본 문자열 내용을 다시 쓸 수 없다.
- **예시:** class UserId(str): / def __new__(cls, value):

```python
class UserId(str):
    def __new__(cls, value):
        normalized = value.strip().lower()
        if not normalized:
            raise ValueError("empty user id")
        return super().__new__(cls, normalized)
```

이 객체의 문자열 값은 만들어지는 순간 확정된다. 

 

---

**실행 점검 72-3 — CHAPTER 03 · immutable built-in subclass는 값 자체를 `__new__` 단계에서 결정해야 한다**
CHAPTER 03 · immutable built-in subclass는 값 자체를 `__new__` 단계에서 결정해야 한다을 이해할 때는 관찰할 변수를 두세 개로 줄이고 한 번에 한 조건만 바꾸며 결과 차이를 비교한다. 그다음 값 하나, 호출 순서 하나, 경계 조건 하나만 바꿔 CHAPTER 03 · immutable built-in subclass는 값 자체를 `__new__` 단계에서 결정해야 한다의 규칙이 실제 상태에 어떤 변화를 만드는지 확인한다. PART 72 CHAPTER 3에서는 결과를 맞히는 것보다 예상과 실제의 차이를 설명하는 과정이 중요하다. 결과만 맞추지 말고 왜 그 결과가 나왔는지 한 문장으로 설명한다. 마지막에는 타입·정체성·수명·예외 경계 중 어느 요소가 결과를 결정했는지 자기 문장으로 정리해, 같은 규칙을 다른 코드에서도 알아볼 수 있는지 확인한다.
## CHAPTER 04 · `__new__`가 반환한 객체 타입은 이후 initialization 경로를 바꾼다

### 시작 전 용어집

#### 1. __new__

- **뜻:** `__new__`가 요청한 class의 instance가 아닌 다른 object를 반환하면 일반적인 initialization assumptions가 깨질 수 있다.
- **왜 중요한가:** 이런 패턴은 기술적으로 가능하지만 caller는 `MaybeNumber(.
- **예시:** class MaybeNumber: / def __new__(cls, value):

#### 2. 객체

- **뜻:** )`가 항상 `MaybeNumber`를 반환한다고 예상하기 쉽다.
- **왜 중요한가:** 타입 identity가 달라지면 isinstance, method availability, serialization, static typing이 모두 복잡해진다.
- **예시:** class MaybeNumber: / def __new__(cls, value):

#### 3. 타입

- **뜻:** Factory가 필요하면 classmethod나 별도 named constructor가 더 명확한 경우가 많다.
- **왜 중요한가:** `__new__`를 override하는 이유가 object creation semantics 자체와 직접 연결되는지 확인한다.
- **예시:** class MaybeNumber: / def __new__(cls, value):

```python
class MaybeNumber:
    def __new__(cls, value):
        if isinstance(value, int):
            return value
        return super().__new__(cls)
```

.. 

 

---

## CHAPTER 05 · class call path는 metaclass의 `__call__`을 거쳐 instance construction으로 내려간다

### 시작 전 용어집

#### 1. class

- **뜻:** 일반적으로 class를 호출하면 metaclass 수준의 call machinery가 `__new__`와 `__init__`을 조정한다.
- **왜 중요한가:** 따라서 instance construction을 정확히 보려면 “class object를 호출한다”는 한 단계 위 모델이 필요하다.
- **예시:** 일반적으로 class를 호출하면 metaclass 수준의 call machinery가 `__new__`와 …

#### 2. path

- **뜻:** 이 사실은 singleton cache나 dependency injection을 metaclass `__call__`에서 가로채는 패턴을 설명하지만, 너무 강한 hook은 모든 subclass construction을 숨은 방식으로 바꿀 수 있다.
- **왜 중요한가:** 대부분의 application code에서는 metaclass를 건드리지 않고 explicit factory를 사용해도 충분하다.
- **예시:** 이 사실은 singleton cache나 dependency injection을 metaclass `__call__`에서 …

#### 3. __call__

- **뜻:** 실행 경로를 이해하는 것과 모든 확장점을 사용하는 것은 다른 문제다.
- **예시:** 실행 경로를 이해하는 것과 모든 확장점을 사용하는 것은 …

Class도 callable object다.  


 

---

## CHAPTER 06 · initialization 중 exception은 부분 상태와 외부 side effect를 남길 수 있다

### 시작 전 용어집

#### 1. initialization

- **뜻:** `__init__`에서 field를 몇 개 설정한 뒤 exception이 발생하면 정상적으로 사용할 수 없는 instance가 생겼다가 곧 참조를 잃을 수 있다.
- **왜 중요한가:** Python memory는 회수되더라도 외부 file, socket, registration 같은 side effect는 자동 rollback되지 않는다.
- **예시:** class Client: / def __init__(self, config):

#### 2. exception

- **뜻:** 가능하면 validation을 먼저 하고 resource acquisition을 뒤로 미룬다.
- **왜 중요한가:** 여러 단계가 필요하면 classmethod factory와 context manager를 조합해 성공한 object만 반환하는 구조가 더 안전하다.
- **예시:** class Client: / def __init__(self, config):

#### 3. 상태

- **뜻:** Constructor는 실패가 드문 곳이 아니라 실패 시 누가 cleanup을 책임지는지 명확해야 하는 곳이다.
- **예시:** class Client: / def __init__(self, config):

```python
class Client:
    def __init__(self, config):
        self.handle = open_handle(config)
        try:
            self.validate(config)
        except Exception:
            self.handle.close()
            raise
```

 


---

## CHAPTER 07 · constructor invariant는 invalid object가 살아남지 않게 만드는 경계다

### 시작 전 용어집

#### 1. constructor invariant

- **뜻:** Domain object가 생성되었다면 최소 불변식이 이미 만족된 상태가 이상적이다.
- **왜 중요한가:** 예를 들어 음수 금액이 절대 허용되지 않는 Money 타입이라면 생성 뒤 별도 `validate()`를 호출해야만 유효해지는 구조보다 constructor 단계에서 거부하는 편이 안전하다.
- **예시:** class Money: / def __init__(self, won):

#### 2. invalid object

- **뜻:** 하지만 expensive remote validation까지 constructor에 넣으면 객체 생성이 숨은 I/O가 될 수 있다.
- **왜 중요한가:** Local structural invariant와 external policy validation을 분리한다.
- **예시:** class Money: / def __init__(self, won):

#### 3. 경계

- **뜻:** Valid-by-construction은 객체 내부 consistency를 보호하는 원칙이지 모든 business workflow를 constructor로 몰아넣으라는 뜻이 아니다.
- **예시:** class Money: / def __init__(self, won):

```python
class Money:
    def __init__(self, won):
        if won < 0:
            raise ValueError("won must be non-negative")
        self.won = won
```

 


---

## CHAPTER 08 · construction contract는 identity·initialization·failure를 하나로 설명해야 한다

### 시작 전 용어집

#### 1. construction

- **뜻:** 실제 instance identity는 어디서 결정되는가, 언제부터 object invariant가 만족되는가, 중간 실패 시 어떤 side effect가 남는가.
- **왜 중요한가:** Test에서는 invalid argument, immutable subclass normalization, `__new__`가 다른 object를 반환하는 unusual path, resource acquisition 실패를 확인한다.
- **예시:** 실제 instance identity는 어디서 결정되는가, 언제부터 object invariant가 …

#### 2. identity

- **뜻:** Named constructor를 제공한다면 기본 constructor와 invariant 차이도 명확히 한다.
- **왜 중요한가:** 이 PART의 핵심은 **class 호출을 `__init__` 하나로 축소하지 않고, instance를 만드는 `__new__`, 상태를 유효하게 만드는 `__init__`, 그 둘을 조정하는 call path와 실패 cleanup까지 하나의 lifecycle로 보는 것**이다.
- **예시:** Named constructor를 제공한다면 기본 constructor와 invariant 차이도 명확히 …

객체 생성 API를 리뷰할 때는 세 질문을 한다.

---

## 실전 학습 루프 · instance construction lifecycle

### 1. 쉬운 예

`C(...)`를 호출하면 단순히 `__init__` 하나만 실행되는 것이 아니다. 먼저 객체를 만들 책임이 있는 `__new__` 단계가 있고, 그 결과가 적절한 instance일 때 초기화가 이어진다. immutable subclass나 instance caching에서는 이 구분이 중요하다.

### 2. 한 줄 해석

construction은 “객체 생성”과 “생성된 객체 초기화”라는 두 책임을 분리해 본다.

### 3. 직접 실행

아래 코드는 개념을 작게 격리한 예다. 실행 전에 출력이나 상태 변화를 먼저 예상한 뒤 실제 결과와 비교한다.

```python
class Ticket:
    def __new__(cls, code):
        obj = super().__new__(cls)
        print('new', code)
        return obj
    def __init__(self, code):
        print('init', code)
        self.code = code

Ticket('A-1')
```

결과가 예상과 다르면 문법부터 고치지 말고, **어떤 protocol·상태·계약이 호출됐는지**를 한 단계씩 확인한다. 이렇게 해야 “우연히 동작하는 코드”와 “이유를 설명할 수 있는 코드”를 구분할 수 있다.

### 4. 수정 실습

1. `__new__`에서 다른 타입의 객체를 반환했을 때 `__init__` 호출을 관찰한다.
2. 같은 key에 같은 instance를 반환하는 cache를 넣을 때 초기화가 반복되지 않게 설계한다.

수정 후에는 정상 입력 하나만 보지 말고 빈 값, 경계값, 반복 호출, 예외 경로 중 해당되는 반례를 최소 하나 추가한다.

### 5. 확인 문제

`__init__`이 객체 자체를 새로 만들어 반환하는 메서드일까?

### 6. 정답과 오답 설명

**정답:** 아니다. 일반적으로 instance 생성은 `__new__`, 초기 상태 설정은 `__init__`의 책임이다.

**자주 나오는 오답:** constructor라는 한 단어로 두 단계를 합치면 immutable 타입과 metaclass 동작을 이해하기 어려워진다.

마지막으로 코드를 다시 읽으면서 **입력 → 호출되는 규칙 → 상태 변화 → 결과/예외** 네 칸으로 요약한다. 이 네 칸을 설명할 수 있으면 단순 암기가 아니라 실행 모델을 이해한 것이다.

