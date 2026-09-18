# PART 71 · Representation과 formatting protocol — `repr`·`str`·`format`을 목적별로 분리하기

객체를 문자열로 보이는 일은 단순 출력 문제가 아니다. 로그·디버거·에러 메시지·사용자 화면은 서로 다른 독자를 가지며, Python은 이를 `__repr__`, `__str__`, `__format__` 같은 protocol로 나눈다. 표현을 잘못 설계하면 비밀값이 로그에 노출되거나, 디버깅에 필요한 identity가 사라지거나, f-string 포맷 규칙이 다른 숫자 타입과 어긋날 수 있다. 이 절에서는 **누가 읽는 표현인지, 어떤 정보가 안정적으로 포함되어야 하는지**를 기준으로 본다.

---

## CHAPTER 01 · `repr`은 개발자가 객체 상태를 식별할 수 있는 표현을 제공한다

### 시작 전 용어집

#### 1. repr

- **뜻:** `repr(obj)`는 일반적으로 개발자와 디버거를 위한 표현이다.
- **왜 중요한가:** 가능한 경우 객체를 명확히 식별하고 중요한 constructor-like state를 보여주는 편이 좋다.
- **예시:** class Point: / def __init__(self, x, y):

#### 2. 객체

- **뜻:** r`을 사용하면 문자열 안의 escape나 빈 문자열 같은 경계가 더 분명하게 보인다.
- **왜 중요한가:** `repr`을 반드시 `eval()` 가능한 문자열로 만들 필요는 없지만, 최소한 어떤 타입의 어떤 상태인지 식별 가능해야 한다.
- **예시:** class Point: / def __init__(self, x, y):

#### 3. 상태

- **뜻:** 반대로 access token, password, 주민번호 같은 민감값을 그대로 포함하면 로그·traceback·REPL에서 노출될 수 있다.
- **예시:** class Point: / def __init__(self, x, y):

```python
class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    def __repr__(self):
        return f"Point(x={self.x!r}, y={self.y!r})"
```

여기서 내부 값에 `! 

 디버깅 유용성과 정보 노출을 함께 검토한다.

---

**직접 확인하기 — CHAPTER 01 · `repr`은 개발자가 객체 상태를 식별할 수 있는 표현을 제공한다**
CHAPTER 01 · `repr`은 개발자가 객체 상태를 식별할 수 있는 표현을 제공한다은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 규칙을 확인해야 오래 남는다. 먼저 입력이나 객체 하나만 두고 기대 결과를 적은 뒤 실행한다. 다음에는 값 하나, 호출 순서 하나, 경계 조건 하나만 바꿔 실제 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 01 · `repr`은 개발자가 객체 상태를 식별할 수 있는 표현을 제공한다의 규칙이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 좁히고, 수정 뒤 원래 예제와 반대 조건 예제를 모두 다시 실행한다. 마지막에는 왜 그런 결과가 나왔는지 자기 문장으로 설명해 본다.
## CHAPTER 02 · `str`은 사용자가 읽을 목적의 표현으로 좁힐 수 있다

### 시작 전 용어집

#### 1. str

- **뜻:** `str(obj)`는 최종 사용자나 일반 메시지에 더 적합한 표현을 제공할 수 있다.
- **왜 중요한가:** 타입이 별도 `__str__`을 정의하지 않으면 `__repr__`과 연결되는 fallback이 사용될 수 있다.
- **예시:** class Money: / def __init__(self, won):

#### 2. 타입

- **뜻:** 로그에서는 `Money(won=12000)`이 상태 파악에 유용하고, 화면에서는 `₩12,000`이 더 적절할 수 있다.
- **왜 중요한가:** `__str__`에 locale, 번역, timezone 정책을 숨기면 같은 객체가 실행 환경에 따라 다르게 보일 수 있다.
- **예시:** class Money: / def __init__(self, won):

#### 3. class

- **뜻:** 도메인 object 자체와 presentation layer의 책임을 어디까지 나눌지 결정한다.
- **예시:** class Money: / def __init__(self, won):

```python
class Money:
    def __init__(self, won):
        self.won = won

    def __repr__(self):
        return f"Money(won={self.won!r})"

    def __str__(self):
        return f"₩{self.won:,}"
```

 두 목적을 하나의 문자열에 억지로 맞추지 않는다.

 

---

**직접 확인하기 — CHAPTER 02 · `str`은 사용자가 읽을 목적의 표현으로 좁힐 수 있다**
CHAPTER 02 · `str`은 사용자가 읽을 목적의 표현으로 좁힐 수 있다은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 규칙을 확인해야 오래 남는다. 먼저 입력이나 객체 하나만 두고 기대 결과를 적은 뒤 실행한다. 다음에는 값 하나, 호출 순서 하나, 경계 조건 하나만 바꿔 실제 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 02 · `str`은 사용자가 읽을 목적의 표현으로 좁힐 수 있다의 규칙이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 좁히고, 수정 뒤 원래 예제와 반대 조건 예제를 모두 다시 실행한다. 마지막에는 왜 그런 결과가 나왔는지 자기 문장으로 설명해 본다.
## CHAPTER 03 · `__format__`은 format spec을 타입의 표현 정책으로 해석한다

### 시작 전 용어집

#### 1. __format__

- **뜻:** `format(value, spec)`과 f-string의 `f"{value:spec}"`는 타입의 formatting protocol과 연결된다.
- **왜 중요한가:** 숫자나 날짜 타입은 width, precision, alignment 같은 spec을 해석할 수 있다.
- **예시:** class Percent: / def __init__(self, ratio):

#### 2. format spec

- **뜻:** 1f}"` 같은 문법으로 표현 정책을 재사용할 수 있다.
- **왜 중요한가:** 중요한 것은 임의 문자열 spec을 무질서하게 늘리지 않는 것이다.
- **예시:** class Percent: / def __init__(self, ratio):

#### 3. 타입

- **뜻:** 표준 format mini-language와 최대한 일관되게 맞추면 caller의 학습 비용이 줄어든다.
- **왜 중요한가:** 지원하지 않는 spec은 조용히 무시하기보다 명확히 실패시키는 편이 오타를 잡기 쉽다.
- **예시:** class Percent: / def __init__(self, ratio):

```python
class Percent:
    def __init__(self, ratio):
        self.ratio = ratio

    def __format__(self, spec):
        return format(self.ratio * 100, spec) + "%"
```

이제 `f"{Percent(0.1234):.  


---

**직접 확인하기 — CHAPTER 03 · `__format__`은 format spec을 타입의 표현 정책으로 해석한다**
CHAPTER 03 · `__format__`은 format spec을 타입의 표현 정책으로 해석한다은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 규칙을 확인해야 오래 남는다. 먼저 입력이나 객체 하나만 두고 기대 결과를 적은 뒤 실행한다. 다음에는 값 하나, 호출 순서 하나, 경계 조건 하나만 바꿔 실제 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 03 · `__format__`은 format spec을 타입의 표현 정책으로 해석한다의 규칙이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 좁히고, 수정 뒤 원래 예제와 반대 조건 예제를 모두 다시 실행한다. 마지막에는 왜 그런 결과가 나왔는지 자기 문장으로 설명해 본다.
## CHAPTER 04 · f-string conversion은 `!r`·`!s`·format spec의 적용 순서를 구분한다

### 시작 전 용어집

#### 1. f-string conversion

- **뜻:** F-string은 expression을 평가한 뒤 conversion과 formatting을 적용한다.
- **왜 중요한가:** s`는 `str` 경로를 강제할 수 있고, 그 뒤 format spec이 적용되는 형태를 이해해야 한다.
- **예시:** name = "a\nb" / print(f"{name}")

#### 2. format spec

- **뜻:** 첫 번째는 실제 줄바꿈이 보일 수 있지만 두 번째는 escape가 포함된 개발자 표현으로 경계를 확인하기 쉽다.
- **왜 중요한가:** r`은 empty string, whitespace, control character를 찾는 데 유용하다.
- **예시:** name = "a\nb" / print(f"{name}")

#### 3. 경계

- **뜻:** r`을 쓰면 따옴표와 escape가 노출되어 읽기 어려워진다.
- **예시:** name = "a\nb" / print(f"{name}")

`!r`은 `repr`, `!

```python
name = "a\nb"
print(f"{name}")
print(f"{name!r}")
```


디버깅 로그에서 `! 반면 사용자 메시지에서 무조건 `!

표현 문법의 선택도 대상 독자와 목적에 맞춘다.

---

**직접 확인하기 — CHAPTER 04 · f-string conversion은 `!r`·`!s`·format spec의 적용 순서를 구분한다**
CHAPTER 04 · f-string conversion은 `!r`·`!s`·format spec의 적용 순서를 구분한다은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 규칙을 확인해야 오래 남는다. 먼저 입력이나 객체 하나만 두고 기대 결과를 적은 뒤 실행한다. 다음에는 값 하나, 호출 순서 하나, 경계 조건 하나만 바꿔 실제 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 04 · f-string conversion은 `!r`·`!s`·format spec의 적용 순서를 구분한다의 규칙이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 좁히고, 수정 뒤 원래 예제와 반대 조건 예제를 모두 다시 실행한다. 마지막에는 왜 그런 결과가 나왔는지 자기 문장으로 설명해 본다.
## CHAPTER 05 · bytes와 text representation은 encoding 경계를 대신 해결해 주지 않는다

### 시작 전 용어집

#### 1. bytes

- **뜻:** `bytes`의 `repr`이 사람이 읽을 수 있는 ASCII 일부를 보여준다고 해서 그것이 올바르게 decode된 text라는 뜻은 아니다.
- **왜 중요한가:** 표현은 byte sequence를 진단하기 위한 것이고, 실제 text 의미를 얻으려면 올바른 encoding과 error policy로 decode해야 한다.
- **예시:** raw = b"hello\xff" / print(repr(raw))

#### 2. text representation

- **뜻:** 로그에서 `repr(bytes)`를 본 뒤 문자열이라고 착각해 다시 encode/decode를 반복하면 경계가 더 혼란스러워진다.
- **왜 중요한가:** Binary protocol payload는 길이·hex prefix·checksum처럼 진단에 필요한 제한된 정보를 표현하고 전체 secret payload를 출력하지 않는 정책도 고려한다.
- **예시:** raw = b"hello\xff" / print(repr(raw))

#### 3. encoding

- **뜻:** Representation은 데이터 변환이 아니라 관찰 창이다.
- **예시:** raw = b"hello\xff" / print(repr(raw))

```python
raw = b"hello\xff"
print(repr(raw))
```

 


---

## CHAPTER 06 · debug representation은 운영 로그의 크기와 민감정보 정책을 고려해야 한다

### 시작 전 용어집

#### 1. debug representation

- **뜻:** 거대한 collection 전체를 `repr`에 포함하면 한 번의 예외가 수 MB 로그를 만들 수 있다.
- **왜 중요한가:** 반대로 핵심 ID 없이 `<Order object at .
- **예시:** Batch(id='B-42', items=1842, state='retrying')

#### 2. collection

- **뜻:** 실무 표현은 보통 안정적인 식별자, 중요한 상태 몇 개, 크기 요약을 사용한다.
- **왜 중요한가:** 민감 필드는 redaction하고, 대형 payload는 길이와 digest만 남길 수 있다.
- **예시:** Batch(id='B-42', items=1842, state='retrying')

#### 3. 객체

- **뜻:** Representation이 호출되는 위치도 고려한다.
- **왜 중요한가:** 에러 처리 중 `repr` 자체가 예외를 던지면 원래 오류를 가릴 수 있으므로 가능하면 단순하고 안정적으로 만든다.
- **예시:** Batch(id='B-42', items=1842, state='retrying')

#### 4. 상태

- **뜻:** 운영 로그에서 representation은 관찰 가능성 계약의 일부다.
- **예시:** Batch(id='B-42', items=1842, state='retrying')

..>`만 남기면 운영 장애에서 어떤 객체였는지 알기 어렵다.


```text
Batch(id='B-42', items=1842, state='retrying')
```

  


---

## CHAPTER 07 · recursive object graph의 `repr`은 무한 재귀와 폭발적 출력을 피해야 한다

### 시작 전 용어집

#### 1. recursive object

- **뜻:** Container가 자기 자신을 참조하거나 graph에 cycle이 있으면 순진한 recursive `__repr__`은 끝나지 않을 수 있다.
- **왜 중요한가:** 내장 container는 이런 cycle을 감지해 제한된 표현을 사용한다.
- **예시:** items = [] / items.append(items)

#### 2. graph

- **뜻:** Custom graph type도 child 전체를 무조건 재귀 출력하기보다 ID, depth limit, visited set을 사용할 수 있다.
- **왜 중요한가:** Tree라고 믿었던 구조가 실제로 DAG나 cycle을 가질 수 있는지 domain invariant를 확인한다.
- **예시:** items = [] / items.append(items)

#### 3. repr

- **뜻:** Representation은 객체의 모든 정보를 직렬화하는 기능이 아니다.
- **왜 중요한가:** Debug 표현이 graph traversal algorithm이 되어 버리면 성능과 안전성 문제가 생긴다.
- **예시:** items = [] / items.append(items)

```python
items = []
items.append(items)
print(items)
```

 

 


---

**직접 확인하기 — CHAPTER 07 · recursive object graph의 `repr`은 무한 재귀와 폭발적 출력을 피해야 한다**
CHAPTER 07 · recursive object graph의 `repr`은 무한 재귀와 폭발적 출력을 피해야 한다은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 규칙을 확인해야 오래 남는다. 먼저 입력이나 객체 하나만 두고 기대 결과를 적은 뒤 실행한다. 다음에는 값 하나, 호출 순서 하나, 경계 조건 하나만 바꿔 실제 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 07 · recursive object graph의 `repr`은 무한 재귀와 폭발적 출력을 피해야 한다의 규칙이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 좁히고, 수정 뒤 원래 예제와 반대 조건 예제를 모두 다시 실행한다. 마지막에는 왜 그런 결과가 나왔는지 자기 문장으로 설명해 본다.
## CHAPTER 08 · representation contract는 독자·안정성·정보 노출 범위를 명시한다

### 시작 전 용어집

#### 1. representation contract

- **뜻:** 좋은 표현 정책은 `repr`, `str`, formatting의 책임을 나눈다.
- **왜 중요한가:** 개발자용 `repr`에는 타입과 핵심 상태를, 사용자용 `str`에는 이해하기 쉬운 의미를, formatting에는 명시적 spec에 따른 변형을 둔다.
- **예시:** 좋은 표현 정책은 `repr`, `str`, formatting의 책임을 나눈다.

#### 2. 타입

- **뜻:** 테스트에서는 escape가 필요한 문자열, 민감값, 매우 큰 collection, recursive graph, unsupported format spec을 확인한다.
- **왜 중요한가:** Snapshot test만 믿으면 필드 추가가 로그 계약을 조용히 바꿀 수 있으므로 어떤 정보가 반드시 포함/제외되어야 하는지 invariant로 본다.
- **예시:** 테스트에서는 escape가 필요한 문자열, 민감값, 매우 큰 collection, …

#### 3. 상태

- **뜻:** 이 PART의 핵심은 **객체를 문자열로 바꾸는 일을 하나의 출력 함수로 보지 않고, 디버깅·사용자 표시·포맷 지정이라는 서로 다른 목적을 가진 protocol로 분리하는 것**이다.
- **예시:** 이 PART의 핵심은 **객체를 문자열로 바꾸는 일을 하나의 …

---

## 실전 학습 루프 · representation과 formatting protocol

### 1. 쉬운 예

디버거에서 보는 표현, 사용자에게 보여 줄 문자열, format spec을 적용한 결과는 목적이 다르다. `repr(x)`는 진단 가능한 표현을, `str(x)`는 사용자 친화적 표현을 목표로 할 수 있고 `format(x, spec)`은 출력 정책을 더 세밀하게 받는다.

### 2. 한 줄 해석

객체의 문자열 표현은 하나가 아니라 사용 목적에 따라 서로 다른 protocol로 분리된다.

### 3. 직접 실행

아래 코드는 개념을 작게 격리한 예다. 실행 전에 출력이나 상태 변화를 먼저 예상한 뒤 실제 결과와 비교한다.

```python
class Money:
    def __init__(self, won): self.won = won
    def __repr__(self): return f'Money(won={self.won})'
    def __str__(self): return f'{self.won:,}원'

m = Money(12000)
print(repr(m))
print(str(m))
```

결과가 예상과 다르면 문법부터 고치지 말고, **어떤 protocol·상태·계약이 호출됐는지**를 한 단계씩 확인한다. 이렇게 해야 “우연히 동작하는 코드”와 “이유를 설명할 수 있는 코드”를 구분할 수 있다.

### 4. 수정 실습

1. `__format__`을 추가해 `format(m, 'plain')` 같은 정책을 정의한다.
2. 민감 정보가 있는 객체의 `repr`에 secret이 노출되면 왜 위험한지 점검한다.

수정 후에는 정상 입력 하나만 보지 말고 빈 값, 경계값, 반복 호출, 예외 경로 중 해당되는 반례를 최소 하나 추가한다.

### 5. 확인 문제

`repr()`과 `str()`은 항상 같은 문자열을 내야 할까?

### 6. 정답과 오답 설명

**정답:** 아니다. 진단·재현 목적과 사용자 표시 목적이 다를 수 있다.

**자주 나오는 오답:** 예쁘게 보이는 문자열만 만들면 된다는 생각은 로그·디버깅·보안 요구를 놓친다.

마지막으로 코드를 다시 읽으면서 **입력 → 호출되는 규칙 → 상태 변화 → 결과/예외** 네 칸으로 요약한다. 이 네 칸을 설명할 수 있으면 단순 암기가 아니라 실행 모델을 이해한 것이다.

