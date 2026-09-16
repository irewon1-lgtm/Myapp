# TRACK 02 · 프로그래밍 사고와 문법

TRACK 01에서는 컴퓨터와 프로그램이 무엇인지, 코드가 실행된다는 것이 무엇인지 배웠다.

이제부터는 실제로 작은 프로그램을 만든다.

하지만 이 TRACK의 목표는 Python 문법을 빨리 외우는 것이 아니다.

> 문제를 작은 단계로 나누고, 값을 저장하고, 조건을 판단하고, 반복하고, 기능을 나누고, 오류를 처리해서 내가 원하는 동작을 직접 만들어 보는 것

이 목표다.

예제는 처음에는 Python을 사용한다. 다른 언어를 나중에 배우더라도 여기서 익히는 `값`, `변수`, `조건`, `반복`, `함수`라는 생각은 계속 다시 나온다.

---

## BLOCK 01 · 코드를 쓰기 전에 문제를 작은 단계로 바꾼다

### LESSON 01 · 목표 → 입력 → 처리 → 출력 → 작은 작업 순서로 생각한다

#### 1. 초보자는 왜 코드를 바로 쓰면 막힐까

처음 코딩을 배우면 문제를 보자마자 키보드부터 잡고 싶어진다.

예를 들어:

> 상품 가격과 수량을 입력받아서 총가격을 보여 주는 프로그램을 만들어라.

라는 문제가 나왔다.

바로 Python 문법을 떠올리려고 하면:

```text
input이 뭐였지?
int는 뭐였지?
print를 어디에 쓰지?
변수 이름은?
```

처럼 문법 생각에 막힌다.

그런데 코드 전에 문제 구조부터 적으면 훨씬 단순해진다.

```text
목표
상품 총가격을 계산한다.

입력
상품 가격
상품 수량

처리
가격 × 수량

출력
총가격
```

아직 코드가 한 줄도 없지만 프로그램의 뼈대는 이미 만들어졌다.

#### 2. 목표를 한 문장으로 만든다

프로그램을 만들기 전에 가장 먼저 적을 것은 `무엇을 만들 것인가`다.

나쁜 목표:

```text
계산하는 거 만들기
```

너무 애매하다.

조금 더 좋은 목표:

```text
상품 가격과 수량을 받아 총가격을 계산해 보여 주는 프로그램을 만든다.
```

좋은 목표는 입력과 결과가 어느 정도 보인다.

또 다른 예:

```text
사용자의 나이를 받아 성인인지 알려 주는 프로그램을 만든다.
```

여기서도 자연스럽게:

```text
입력 = 나이
처리 = 19 이상인지 비교
출력 = 성인/미성년
```

이 보인다.

#### 3. 입력과 출력을 먼저 정하면 중간이 보인다

프로그램이 복잡해져도 먼저 다음 두 질문을 한다.

```text
무엇을 받아야 하지?
무엇을 내놓아야 하지?
```

예를 들어 로그인 기능:

```text
입력
이메일
비밀번호

출력
로그인 성공 또는 실패
```

그러면 중간 처리 후보가 나온다.

```text
입력값이 비어 있는지 검사
사용자를 찾기
비밀번호 확인
성공 상태 만들기
```

이것이 문제를 코드로 바꾸는 첫 번째 사고방식이다.

#### 4. 큰 기능을 작은 작업으로 나누는 것이 decomposition이다

회원가입 기능을 한 덩어리로 보면 크다.

```text
회원가입 구현
```

이라고만 적으면 어디부터 시작해야 할지 모른다.

작게 나눈다.

```text
1. 이름을 입력받는다.
2. 이메일을 입력받는다.
3. 비밀번호를 입력받는다.
4. 비어 있는 값이 없는지 확인한다.
5. 이메일 모양이 맞는지 확인한다.
6. 이미 가입된 이메일인지 확인한다.
7. 사용자를 저장한다.
8. 성공 또는 실패를 보여 준다.
```

큰 문제를 작은 문제로 나누는 이런 생각을 **분해(decomposition)**라고 부른다.

용어보다 중요한 것은 습관이다.

> `회원가입`이라고 한 줄로 생각하지 않고, 실제로 컴퓨터가 수행할 수 있는 작은 단계로 쪼갠다.

#### 5. 한 단계는 입력과 출력을 다시 가질 수 있다

예를 들어:

```text
이메일 모양 검사
```

라는 작은 단계도 다시 나눌 수 있다.

```text
입력 = 이메일 문자열
처리 = 최소한 필요한 형식 확인
출력 = 올바름/올바르지 않음
```

그래서 큰 프로그램은 작은 `입력→처리→출력` 묶음이 여러 개 연결된 것으로 볼 수 있다.

#### 6. 사람말로 먼저 의사코드를 적는다

아직 Python 문법을 모른다고 하자.

그래도 다음처럼 적을 수 있다.

```text
가격을 받는다
수량을 받는다
가격과 수량을 곱한다
결과를 보여 준다
```

이처럼 정확한 프로그래밍 언어 문법 전 단계에서 실행 절차를 사람말이나 단순한 형식으로 적는 것을 **의사코드(pseudocode)**라고 부른다.

예:

```text
IF 나이 >= 19
    "성인" 출력
ELSE
    "미성년" 출력
```

이것은 Python도 JavaScript도 아니다.

하지만 프로그램의 생각은 이미 표현돼 있다.

#### 7. 의사코드를 실제 코드로 옮겨 본다

사람말:

```text
가격 1000
수량 3
가격과 수량을 곱한다
결과를 보여 준다
```

Python:

```python
price = 1000
count = 3
total = price * count
print(total)
```

결과:

```text
3000
```

한 줄씩 읽는다.

```text
price = 1000
→ price라는 이름으로 1000을 기억한다.

count = 3
→ count라는 이름으로 3을 기억한다.

total = price * count
→ 두 값을 곱한 결과를 total로 기억한다.

print(total)
→ total 값을 보여 준다.
```

문법을 먼저 외운 것이 아니다.

사람말 절차를 코드로 하나씩 옮겼다.

#### 8. 잘못된 프로그램을 문제 구조로 찾는다

다음 코드를 보자.

```python
price = 1000
count = 3
total = price + count
print(total)
```

코드는 실행된다.

하지만 출력은:

```text
1003
```

이다.

문제 정의로 돌아간다.

```text
처리 = 가격 × 수량
```

인데 코드에는:

```text
가격 + 수량
```

이 들어 있다.

따라서 문법 문제가 아니라 **처리 규칙이 목표와 다르다**.

이 방식으로 디버깅을 시작할 수 있다.

#### 9. AI에게 맡길 때도 문제 분해가 먼저다

AI에게:

> 쇼핑몰 만들어 줘.

라고만 하면 요구가 너무 넓다.

반대로:

```text
목표: 상품 1개의 총가격 계산
입력: price, count
규칙: 둘 다 0 이상
처리: price * count
출력: total
오류: 숫자가 아니면 오류 메시지
```

처럼 주면 AI가 만들어야 할 결과도 명확해진다.

AI 시대에도 문제 분해 능력이 중요한 이유다.

#### 10. 책을 덮고 확인한다

1. 프로그램을 만들기 전에 목표를 한 문장으로 적는 이유는 무엇인가?
2. `입력·처리·출력`으로 회원가입의 일부를 나눠 보라.
3. decomposition을 자기 말로 설명해 보라.
4. 의사코드가 실제 Python 코드와 다른 점은 무엇인가?
5. 실행되는 코드인데 결과가 틀릴 때 무엇부터 다시 확인해야 하는가?

---

## BLOCK 02 · 값과 변수는 프로그램이 기억하고 계산하는 기본 재료다

### LESSON 01 · 숫자·문자·참거짓·값 없음에서 변수와 연산까지 연결한다

#### 1. 프로그램이 실제로 다루는 것은 값이다

프로그램 안에는 수많은 정보가 들어온다.

```text
사용자 이름
나이
상품 가격
로그인 여부
검색어
주문 개수
```

이런 실제 내용을 넓게 **값(value)**이라고 부른다.

Python 코드에서:

```python
10
3.14
"안녕"
True
None
```

모두 값이다.

하지만 종류가 다르다.

#### 2. 정수와 소수 숫자를 구분한다

```python
10
0
-3
```

처럼 소수점이 없는 정수를 Python에서는 보통 `int` 타입으로 다룬다.

```python
3.14
0.5
-2.7
```

같은 소수 값은 `float` 타입으로 다룬다.

여기서 **타입(type)**은 값이 어떤 종류이며 어떤 동작을 할 수 있는지 구분하는 정보라고 이해하면 된다.

```text
10
→ 정수로 계산 가능

"10"
→ 글자 두 개로 된 문자열
```

겉으로 화면에 `10`이라고 보일 수 있어도 프로그램 내부 의미는 다를 수 있다.

#### 3. 소수 계산은 사람의 십진수 감각과 다를 수 있다

```python
print(0.1 + 0.2)
```

실행하면 흔히:

```text
0.30000000000000004
```

처럼 보일 수 있다.

컴퓨터가 많은 소수를 이진 기반의 근삿값으로 표현하기 때문이다.

지금 부동소수점 수학을 깊게 공부할 필요는 없다.

대신 다음 사실을 기억한다.

> 컴퓨터의 `float` 계산은 사람이 종이에 쓰는 정확한 십진수 계산과 항상 똑같지 않을 수 있다.

돈처럼 정확성이 중요한 값은 나중에 정수 단위나 decimal 계열 도구를 선택할 수 있다.

#### 4. 문자열은 글자 데이터다

Python에서는 따옴표 안의 글자를 **문자열(string)**이라고 부른다.

```python
"민수"
"hello"
"010-1234-5678"
```

전화번호에 숫자가 들어 있지만 계산용 숫자라기보다 `표현할 글자`일 수 있다.

```text
01012345678 + 1
```

같은 계산을 하는 것이 목적이 아니기 때문이다.

어떤 정보는 숫자처럼 보여도 문자열로 다루는 것이 자연스럽다.

#### 5. Boolean은 참과 거짓 두 상태를 나타낸다

```python
True
False
```

이 두 값을 **Boolean(불리언)**이라고 부른다.

예:

```python
age = 20
print(age >= 19)
```

결과:

```text
True
```

`age >= 19`라는 비교가 `True` 또는 `False`를 만든다.

이 값이 다음 LESSON의 조건문을 움직인다.

#### 6. None은 의미 있는 값이 없음을 나타낸다

```python
result = None
```

`None`은 `현재 의미 있는 값이 없음`을 나타낼 때 쓰는 Python의 특별한 값이다.

빈 문자열과는 다르다.

```text
""
→ 문자열이 존재하지만 길이가 0

None
→ 의미 있는 값이 없음을 나타내는 특별한 값
```

나중에 DB의 `NULL`, JavaScript의 `null/undefined`를 배울 때 비슷하지만 세부가 다른 개념을 다시 만난다.

#### 7. literal은 코드에 값을 직접 적은 표현이다

```python
10
"hello"
True
```

처럼 코드에 값을 직접 적어 놓은 표현을 **리터럴(literal)**이라고 부른다.

단어가 어렵지만 뜻은 단순하다.

```text
literal = 코드에 값을 직접 적은 모양
```

#### 8. 변수는 값에 붙인 이름이라고 먼저 이해한다

```python
price = 1200
```

사람말:

> 1200이라는 값을 `price`라는 이름으로 앞으로 사용하겠다.

라고 이해한다.

다음 코드:

```python
price = 1200
count = 3
print(price * count)
```

에서는 `1200`, `3`을 계속 다시 적지 않고 이름으로 사용한다.

변수가 필요한 이유는 값을 기억하고 의미 있는 이름으로 다루기 위해서다.

#### 9. 변수 이름이 코드 설명이 된다

```python
x = 1200
y = 3
z = x * y
```

도 실행된다.

하지만:

```python
product_price = 1200
quantity = 3
total_price = product_price * quantity
```

가 의도를 더 쉽게 보여 준다.

좋은 변수 이름은 주석 없이도 코드 일부를 설명한다.

#### 10. `=`를 수학의 등호로만 보면 헷갈린다

Python의:

```python
score = 10
```

에서 `=`는 `score와 10은 영원히 같다`라는 수학 문장이라기보다 `score라는 이름에 10을 연결한다`는 대입 동작으로 이해하는 편이 좋다.

그래서:

```python
score = 10
score = 20
print(score)
```

결과는:

```text
20
```

이다.

두 번째 대입이 현재 score 값을 바꿨기 때문이다.

#### 11. 상수는 바꾸지 않기로 정한 값이다

세율처럼 프로그램에서 고정해서 쓰고 싶은 값이 있다.

```python
TAX_RATE = 0.1
```

Python에서는 일반 변수를 언어 차원에서 완전히 다른 상수로 강제하는 방식이 아니지만, 보통 대문자 이름으로 `이 값은 바꾸지 말자`는 의도를 표현한다.

중요한 것은:

```text
변수 = 바뀔 수 있는 상태
상수라는 생각 = 바꾸지 않기로 한 규칙 값
```

을 구분하는 것이다.

#### 12. 표현식은 값을 만들어 내는 코드 조각이다

```python
10 + 20
price * quantity
age >= 19
```

각 코드는 하나의 결과 값을 만든다.

이런 코드 조각을 **표현식(expression)**이라고 부른다.

예:

```python
total = price * quantity
```

오른쪽의:

```python
price * quantity
```

가 값을 만들고, 그 결과를 `total`이라는 이름에 넣는다.

#### 13. 산술 연산자를 실제로 써 본다

```python
print(10 + 3)
print(10 - 3)
print(10 * 3)
print(10 / 3)
```

또:

```python
print(10 // 3)
print(10 % 3)
```

`//`는 몫을 구할 때 사용할 수 있고 `%`는 나머지를 구한다.

짝수 판정:

```python
number = 8
print(number % 2 == 0)
```

8을 2로 나눈 나머지가 0인지 확인한다.

#### 14. 비교 연산은 Boolean 값을 만든다

```python
10 > 5
10 < 5
10 >= 10
10 <= 9
10 == 10
10 != 20
```

결과는 True 또는 False다.

`=`와 `==`를 구분한다.

```text
=  값을 이름에 대입
== 두 값이 같은지 비교
```

다음은 완전히 다르다.

```python
age = 20
```

```python
age == 20
```

첫 번째는 age 값을 정하고, 두 번째는 age가 20인지 질문한다.

#### 15. 논리 연산자는 여러 조건을 연결한다

성인이면서 티켓이 있어야 입장 가능하다고 하자.

```python
age = 25
has_ticket = True

print(age >= 19 and has_ticket)
```

`and`는 둘 다 참인지 본다.

`or`는 둘 중 하나 이상이 참인지 볼 때 사용할 수 있다.

```python
is_admin = False
is_owner = True
print(is_admin or is_owner)
```

`not`은 참/거짓을 반대로 본다.

```python
is_closed = False
print(not is_closed)
```

#### 16. 타입이 다르면 같은 기호도 다르게 보일 수 있다

```python
print(5 + 1)
```

결과:

```text
6
```

하지만:

```python
print("5" + "1")
```

결과:

```text
51
```

첫 번째는 숫자 덧셈이고 두 번째는 문자열 연결이다.

이래서 프로그램에서는 값의 **타입**을 함께 봐야 한다.

#### 17. 이번 LESSON의 실수 체크

- 숫자 `10`과 문자열 `"10"`을 같은 것으로 생각하지 않는다.
- `=`와 `==`를 구분한다.
- 변수 이름은 의미가 드러나게 만든다.
- float 계산이 모든 십진수에서 정확하다고 단정하지 않는다.
- None과 빈 문자열을 같은 상태로 보지 않는다.

#### 18. 책을 덮고 확인한다

1. 값과 타입의 차이를 설명해 보라.
2. `10`과 `"10"`은 왜 다를 수 있는가?
3. 변수는 왜 필요한가?
4. `=`와 `==`는 각각 무엇인가?
5. `number % 2 == 0`이 짝수를 확인하는 이유를 말해 보라.

---

## BLOCK 03 · 사용자 입력을 받고 조건에 따라 다른 길로 보낸다

### LESSON 01 · input·타입 변환·if·elif·else를 하나의 프로그램으로 배운다

#### 1. 지금까지 값은 코드 안에 직접 넣었다

```python
age = 20
```

이렇게 하면 항상 20으로 실행된다.

하지만 실제 앱은 사용자마다 다른 값을 받는다.

```text
이름
나이
검색어
수량
```

Python의 간단한 콘솔 프로그램에서는 `input()`으로 사용자의 글자 입력을 받을 수 있다.

```python
name = input("이름을 입력하세요: ")
print(name)
```

#### 2. input 결과는 기본적으로 문자열이다

사용자가:

```text
20
```

을 입력해도 `input()`에서 받은 값은 글자 형태다.

```python
age_text = input("나이: ")
print(age_text)
```

숫자 계산을 하고 싶다면 정수로 바꿔야 한다.

```python
age = int(age_text)
```

전체:

```python
age_text = input("나이: ")
age = int(age_text)
print(age + 1)
```

사용자가 20을 입력하면 21을 출력한다.

#### 3. 타입 변환은 아무 값이나 성공하지 않는다

```python
int("20")
```

은 가능하다.

하지만:

```python
int("스무살")
```

은 정수로 바꿀 수 없으므로 오류가 발생한다.

그래서 외부에서 들어오는 입력은 항상 믿으면 안 된다.

나중에 validation, 즉 입력 검증을 반복해서 배우게 된다.

#### 4. 조건문은 프로그램의 길을 나눈다

나이가 19 이상이면 성인이라고 출력하고 싶다.

```python
age = 20

if age >= 19:
    print("성인")
```

사람말:

```text
만약 age가 19 이상이라면
    "성인"을 출력한다.
```

`age >= 19`의 결과는 True 또는 False다.

True일 때 들여쓰기된 코드가 실행된다.

#### 5. Python에서 들여쓰기는 장식이 아니다

```python
if age >= 19:
    print("성인")
```

`print` 앞의 공백은 `이 코드는 if 안에 속한다`는 구조를 나타낸다.

다음처럼 들여쓰기 위치를 잘못 만들면 의미가 달라지거나 문법 오류가 날 수 있다.

```python
if age >= 19:
print("성인")
```

Python에서는 들여쓰기를 코드 구조로 사용한다.

#### 6. 조건이 거짓일 때는 else를 사용한다

```python
if age >= 19:
    print("성인")
else:
    print("미성년")
```

흐름:

```text
age >= 19 ?
├─ True  → 성인
└─ False → 미성년
```

조건이 두 갈래가 되는 것이다.

#### 7. 세 갈래 이상이면 elif를 사용할 수 있다

점수에 따라 등급을 정한다고 하자.

```python
score = 85

if score >= 90:
    grade = "A"
elif score >= 80:
    grade = "B"
else:
    grade = "C"

print(grade)
```

85라면 첫 조건은 거짓이다.

두 번째 `score >= 80`은 참이므로 B가 된다.

#### 8. 조건 순서가 결과를 바꿀 수 있다

잘못된 예:

```python
score = 95

if score >= 80:
    grade = "B"
elif score >= 90:
    grade = "A"
```

95는 `80 이상` 조건을 먼저 만족한다.

따라서 뒤의 `90 이상`까지 가지 않고 B가 될 수 있다.

원하는 규칙이라면 더 좁고 높은 조건을 먼저 둔다.

```python
if score >= 90:
    grade = "A"
elif score >= 80:
    grade = "B"
```

이것은 문법보다 **실행 순서** 문제다.

#### 9. and, or, not으로 조건을 조합한다

성인이면서 티켓이 있어야 한다.

```python
if age >= 19 and has_ticket:
    print("입장")
```

관리자이거나 본인이면 볼 수 있다.

```python
if is_admin or is_owner:
    print("열람 가능")
```

서비스가 닫혀 있지 않아야 한다.

```python
if not is_closed:
    print("사용 가능")
```

#### 10. 너무 깊은 if는 읽기 어려워진다

```text
if 로그인:
    if 성인:
        if 티켓:
            if 좌석:
                ...
```

조건이 계속 안으로 들어가면 흐름을 읽기 어렵다.

처음에는 동작하는 코드를 만드는 것이 먼저지만, 나중에는 함수를 나누거나 조건을 일찍 끝내는 방식으로 구조를 단순하게 만들 수 있다.

#### 11. 입력부터 결과까지 하나로 만든다

```python
age_text = input("나이: ")
age = int(age_text)

if age >= 19:
    print("성인")
else:
    print("미성년")
```

실행 흐름:

```text
사용자 입력
↓
문자열 age_text
↓ int 변환
정수 age
↓ 비교
age >= 19
↓
True/False에 따라 다른 출력
```

이 흐름을 코드만 보지 않고 화살표로 설명할 수 있어야 한다.

#### 12. 실수 사례 — 입력을 바로 믿는다

```python
age = int(input("나이: "))
```

짧고 편하지만 사용자가 `hello`를 입력하면 프로그램이 중단될 수 있다.

지금은 아직 오류 처리 문법을 배우지 않았으므로:

> 외부 입력은 실패할 수 있다.

라는 사실만 기억한다.

뒤에서 try/except로 실제 대응한다.

#### 13. 책을 덮고 확인한다

1. `input()`으로 받은 `20`이 왜 숫자 계산에 바로 적합하지 않을 수 있는가?
2. `int()`는 무엇을 하는가?
3. if 조건식의 결과는 어떤 타입인가?
4. elif의 순서가 중요한 이유를 95점 예제로 설명하라.
5. `and`와 `or`의 차이를 실제 규칙으로 만들어 보라.

---

## BLOCK 04 · 같은 일을 여러 번 해야 할 때 반복문을 사용한다

### LESSON 01 · for·while·range·break·continue를 실행 순서로 이해한다

#### 1. 같은 코드를 열 번 복사할 필요가 있을까

다음 글자를 5번 출력하고 싶다.

나쁜 방법:

```python
print("안녕")
print("안녕")
print("안녕")
print("안녕")
print("안녕")
```

5번은 쓸 수 있지만 10,000번이라면 불가능에 가깝다.

컴퓨터의 강점은 반복되는 일을 빠르게 수행하는 것이다.

이때 **반복(loop)**을 사용한다.

#### 2. for는 여러 항목을 하나씩 처리한다

```python
for number in [1, 2, 3]:
    print(number)
```

결과:

```text
1
2
3
```

사람말로 읽는다.

```text
[1, 2, 3]에서 값을 하나씩 꺼내
그 값을 number라는 이름으로 사용하면서
print를 실행한다.
```

실행 흐름:

```text
첫 번째 반복: number = 1 → 출력 1
두 번째 반복: number = 2 → 출력 2
세 번째 반복: number = 3 → 출력 3
끝
```

#### 3. range로 숫자 범위를 만든다

```python
for number in range(5):
    print(number)
```

결과:

```text
0
1
2
3
4
```

`range(5)`는 0부터 시작해 5 직전까지의 반복 범위를 제공한다.

초보자가 자주 하는 실수:

```text
range(5)니까 1,2,3,4,5겠지
```

라고 생각하는 것이다.

실제로 기본 시작은 0이다.

#### 4. 반복하며 값을 누적한다

1부터 5까지 더해 보자.

```python
total = 0

for number in [1, 2, 3, 4, 5]:
    total = total + number

print(total)
```

한 단계씩 상태를 추적한다.

```text
시작 total = 0
1을 더함 → 1
2를 더함 → 3
3을 더함 → 6
4를 더함 → 10
5를 더함 → 15
```

반복문을 이해하려면 코드 모양보다 **변수 값이 반복마다 어떻게 바뀌는지** 추적하는 습관이 중요하다.

#### 5. while은 조건이 참인 동안 반복한다

```python
count = 1

while count <= 3:
    print(count)
    count = count + 1
```

결과:

```text
1
2
3
```

흐름:

```text
count <= 3 ?
참 → 출력 → count 1 증가 → 다시 조건 확인
거짓 → 반복 종료
```

#### 6. while에서는 상태 변화가 없으면 무한 반복이 생길 수 있다

잘못된 코드:

```python
count = 1

while count <= 3:
    print(count)
```

`count`가 영원히 1이다.

따라서 조건 `count <= 3`도 계속 참이다.

프로그램이 끝없이 반복할 수 있다.

이것이 **무한 반복(infinite loop)**의 간단한 예다.

#### 7. break는 반복 전체를 끝낸다

숫자를 찾으면 더 볼 필요가 없다고 하자.

```python
numbers = [3, 7, 10, 20]

for number in numbers:
    if number == 10:
        print("찾음")
        break
```

10을 찾은 순간 반복을 종료한다.

#### 8. continue는 이번 반복만 건너뛴다

짝수만 출력하고 싶다.

```python
for number in [1, 2, 3, 4, 5]:
    if number % 2 != 0:
        continue
    print(number)
```

결과:

```text
2
4
```

홀수일 때 `continue`를 만나면 그 아래 코드를 건너뛰고 다음 반복으로 간다.

#### 9. 중첩 반복은 작업량이 빨리 늘 수 있다

```python
for a in [1, 2, 3]:
    for b in [1, 2, 3]:
        print(a, b)
```

바깥 반복 3번마다 안쪽 반복 3번이 실행된다.

총 9개의 조합이 나온다.

데이터가 100개씩이면 10,000번이 될 수 있다.

이 개념은 다음 TRACK의 시간복잡도와 연결된다.

#### 10. for와 while 중 무엇을 쓰나

정답 하나가 있는 것은 아니다.

처음에는:

```text
목록의 항목을 하나씩 처리
→ for가 자연스러운 경우가 많음

특정 조건이 유지되는 동안 계속 실행
→ while이 자연스러운 경우가 많음
```

으로 구분해 본다.

#### 11. 작은 프로그램으로 연결한다

1부터 10까지 중 짝수의 합을 구한다.

```python
total = 0

for number in range(1, 11):
    if number % 2 == 0:
        total = total + number

print(total)
```

코드에는 지금까지 배운 것이 함께 있다.

```text
변수
range
for
if
나머지 연산
대입
출력
```

새 문법을 따로 외우는 것이 아니라 여러 작은 개념이 하나의 프로그램을 만든다.

#### 12. 책을 덮고 확인한다

1. for와 while을 각각 어떤 상황에서 쓰기 자연스러운가?
2. while에서 상태를 바꾸지 않으면 어떤 문제가 생길 수 있는가?
3. break와 continue의 차이를 설명하라.
4. 이중 반복에서 항목이 10×10이면 안쪽 코드는 대략 몇 번 실행되는가?
5. 반복문을 읽을 때 변수 상태를 추적해야 하는 이유는 무엇인가?

---

## BLOCK 05 · 여러 값을 한꺼번에 다루는 자료구조의 기초

### LESSON 01 · list·tuple·dict·set을 실제 사용 목적부터 구분한다

#### 1. 변수 하나에 값 하나만 두면 불편하다

학생 점수가 3개 있다고 하자.

```python
score1 = 80
score2 = 90
score3 = 100
```

3개는 괜찮다.

학생이 1,000명이면 `score1`부터 `score1000`까지 만들 수 없다.

여러 값을 하나의 묶음으로 다뤄야 한다.

#### 2. list는 순서가 있는 여러 값을 담는다

```python
scores = [80, 90, 100]
```

이것을 Python의 **list**라고 부른다.

순서가 있다.

각 위치에는 번호가 붙는데 이 번호를 **index(인덱스)**라고 부른다.

Python list의 첫 index는 0이다.

```text
값      80   90   100
index    0    1     2
```

```python
print(scores[0])
```

결과:

```text
80
```

#### 3. index 1이 첫 번째라고 착각하지 않는다

사람은 첫 번째를 1이라고 세기 쉽다.

Python list는 첫 요소 index가 0이다.

```python
scores[0]  # 첫 번째
scores[1]  # 두 번째
scores[2]  # 세 번째
```

범위를 넘으면 오류가 난다.

```python
scores[10]
```

3개밖에 없는 목록에서 10번 위치는 없다.

#### 4. list에 값을 추가하고 삭제한다

```python
scores.append(70)
```

뒤에 70을 추가한다.

```python
print(scores)
```

결과:

```text
[80, 90, 100, 70]
```

특정 값을 제거하는 방법도 있다.

```python
scores.remove(90)
```

여기서 중요한 것은 `append`, `remove`라는 단어를 외우는 것보다 **list가 실행 중에 변할 수 있는 여러 값의 묶음**이라는 점이다.

#### 5. list와 for는 자주 함께 쓴다

```python
scores = [80, 90, 100]

for score in scores:
    print(score)
```

목록에서 값을 하나씩 꺼내 처리한다.

평균을 계산해 보자.

```python
scores = [80, 90, 100]
total = 0

for score in scores:
    total = total + score

average = total / len(scores)
print(average)
```

여기서 `len(scores)`는 목록 항목 개수를 알려 준다.

#### 6. slice는 목록 일부를 잘라 본다

```python
numbers = [10, 20, 30, 40, 50]
print(numbers[1:4])
```

결과:

```text
[20, 30, 40]
```

`1`부터 시작하고 `4` 직전까지 가져온다.

범위의 끝이 포함되지 않는 패턴은 Python에서 자주 나온다.

#### 7. tuple은 바꾸지 않는 묶음으로 사용할 수 있다

```python
point = (10, 20)
```

Python의 **tuple**은 list처럼 여러 값을 순서대로 담지만 일반적인 사용에서 생성 후 항목을 바꿀 수 없는 구조다.

처음에는:

```text
list = 바뀔 수 있는 순서 있는 묶음

tuple = 고정된 묶음으로 다루기 좋은 순서 있는 값
```

정도로 구분한다.

#### 8. dict는 위치 번호보다 이름으로 찾고 싶을 때 편하다

사용자 한 명의 정보를 list로 만들 수 있다.

```python
user = [101, "민수", 37]
```

하지만 `user[1]`이 무엇인지 외워야 한다.

dict를 사용하면:

```python
user = {
    "id": 101,
    "name": "민수",
    "age": 37
}
```

처럼 key와 value로 표현할 수 있다.

```python
print(user["name"])
```

결과:

```text
민수
```

사람이 읽기도 더 쉽다.

#### 9. key와 value를 구분한다

```python
{
    "name": "민수"
}
```

에서:

```text
"name" = key
"민수" = value
```

이다.

key를 기준으로 원하는 값을 찾는다.

이 생각이 다음 TRACK의 hash table과 DB의 key 개념으로 확장된다.

#### 10. set은 중복 없는 값 모음이다

```python
users = {"A", "B", "A"}
print(users)
```

set은 중복 없는 값을 다루는 데 유용하다.

예를 들어 방문한 사용자 ID를 기록할 때:

```python
visited = set()
visited.add(10)
visited.add(20)
visited.add(10)
```

10이 여러 번 추가돼도 하나의 항목으로 관리된다.

다음 TRACK의 graph 탐색에서 `visited set`을 다시 사용한다.

#### 11. 어떤 것을 선택해야 할까

처음에는 질문으로 고른다.

```text
순서대로 여러 값을 다루고 계속 바꾸고 싶다
→ list

고정된 몇 개 값을 순서대로 묶고 싶다
→ tuple

이름(key)으로 값을 찾고 싶다
→ dict

중복을 없애고 포함 여부를 보고 싶다
→ set
```

하나가 무조건 최고가 아니다.

사용 목적에 맞는 구조를 고른다.

#### 12. 중첩 구조를 만든다

여러 사용자를 표현해 보자.

```python
users = [
    {"id": 1, "name": "민수"},
    {"id": 2, "name": "지수"}
]
```

이것은:

```text
list
└─ dict
   ├─ id
   └─ name
```

구조다.

실제 API의 JSON 데이터도 이런 중첩 구조와 매우 비슷하게 보일 수 있다.

#### 13. 책을 덮고 확인한다

1. list의 첫 index는 몇인가?
2. list와 tuple의 큰 차이를 말해 보라.
3. dict가 `user[1]`보다 읽기 쉬울 수 있는 이유는 무엇인가?
4. set은 어떤 상황에서 유용한가?
5. list 안에 dict를 넣은 예를 하나 만들어 보라.

---

## BLOCK 06 · 함수를 사용하면 프로그램을 작은 기능으로 나눌 수 있다

### LESSON 01 · function·parameter·argument·return·scope를 하나의 흐름으로 배운다

#### 1. 같은 계산 코드를 계속 복사하면 무슨 일이 생길까

총가격을 여러 곳에서 계산한다고 하자.

```python
price = 1000
count = 3
total = price * count
```

다른 화면에서도 또 복사한다.

또 다른 기능에도 복사한다.

나중에 할인 규칙이 바뀌면 여러 곳을 전부 찾아 수정해야 한다.

그래서 관련 작업을 이름 붙인 하나의 기능으로 묶는다.

이것이 **함수(function)**다.

#### 2. 가장 작은 함수부터 만든다

```python
def say_hello():
    print("안녕")
```

이 코드는 `say_hello`라는 함수를 **정의**한다.

정의했다고 바로 실행되는 것은 아니다.

사용하려면 호출한다.

```python
say_hello()
```

결과:

```text
안녕
```

```text
def로 함수 정의
↓
함수 이름()으로 호출
↓
함수 안 코드 실행
```

#### 3. 함수에 값을 전달하고 싶으면 parameter를 둔다

```python
def greet(name):
    print("안녕", name)
```

여기서 `name`은 함수가 사용할 입력 자리다.

이런 자리를 **parameter(매개변수)**라고 부른다.

호출:

```python
greet("민수")
```

호출할 때 실제로 전달한 `"민수"`를 **argument(인자)**라고 부른다.

초보용 구분:

```text
parameter = 함수 정의에 적은 입력 자리 이름
argument = 실제 호출하면서 넘긴 값
```

#### 4. 여러 입력을 받을 수 있다

```python
def calculate_total(price, count):
    total = price * count
    print(total)
```

호출:

```python
calculate_total(1000, 3)
```

결과:

```text
3000
```

#### 5. print와 return은 역할이 다르다

함수 안에서 결과를 print만 하면 화면에는 보이지만 다른 코드가 그 결과를 계속 계산에 쓰기 불편하다.

```python
def calculate_total(price, count):
    return price * count
```

호출:

```python
total = calculate_total(1000, 3)
print(total)
```

`return`은 함수의 결과 값을 호출한 쪽으로 돌려준다.

```text
함수 입력
price=1000, count=3
↓
계산
3000
↓ return
호출한 코드가 3000을 받음
```

#### 6. return 이후에는 함수가 끝난다

```python
def check_age(age):
    if age < 0:
        return "잘못된 나이"

    return "정상"
```

age가 -1이면 첫 return에서 함수가 끝난다.

아래 return까지 내려가지 않는다.

이런 흐름은 나중에 복잡한 중첩 조건을 줄이는 데도 사용할 수 있다.

#### 7. 함수는 왜 프로그램을 이해하기 쉽게 만들까

다음 코드를 보자.

```python
email = input("이메일: ")
# 아주 많은 검사 코드...
# 또 저장 코드...
# 또 출력 코드...
```

역할별 함수로 나누면:

```python
email = input_email()
valid = validate_email(email)
user = create_user(email)
save_user(user)
```

아직 각 함수 내부를 보지 않아도 전체 흐름이 읽힌다.

함수는 단순 코드 압축 도구가 아니라 **문제를 역할별로 나누는 도구**다.

#### 8. 함수 안의 변수와 바깥 변수를 구분한다

```python
def work():
    message = "함수 안"
    print(message)

work()
```

`message`는 함수 안에서 만든 변수다.

보통 함수 밖에서 그대로 사용할 수 없다.

이처럼 변수가 보이고 사용할 수 있는 범위를 **scope(스코프)**라고 부른다.

정확한 규칙은 언어마다 다르지만 중요한 생각은:

> 모든 변수가 프로그램 전체 어디서나 자동으로 보이는 것은 아니다.

이다.

#### 9. 전역 변수를 아무 데서나 바꾸면 추적하기 어려워진다

```python
total = 0

def add():
    global total
    total = total + 1
```

작은 예에서는 되지만 여러 함수가 같은 전역 상태를 마음대로 바꾸면:

```text
누가 값을 바꿨지?
언제 바뀌었지?
```

추적이 어려워진다.

가능하면 함수가 입력을 받고 결과를 return하도록 만드는 것이 이해와 테스트에 유리한 경우가 많다.

#### 10. 같은 함수에 여러 예를 넣어 검증한다

```python
def calculate_total(price, count):
    return price * count
```

하나만 실행하지 않는다.

```python
print(calculate_total(1000, 3))
print(calculate_total(500, 0))
print(calculate_total(2000, 1))
```

예상:

```text
3000
0
2000
```

함수 하나를 만들면 여러 입력에서 같은 규칙이 맞는지 확인하기 쉬워진다.

#### 11. 함수 이름은 역할을 설명해야 한다

나쁜 이름:

```python
def do_it(x, y):
    return x * y
```

좋은 방향:

```python
def calculate_total_price(price, quantity):
    return price * quantity
```

길다고 무조건 좋은 것은 아니지만 `무엇을 하는 함수인지` 드러나야 한다.

#### 12. 책을 덮고 확인한다

1. 함수 정의와 함수 호출의 차이는 무엇인가?
2. parameter와 argument를 구분하라.
3. print와 return은 왜 다른가?
4. scope가 필요한 이유를 말해 보라.
5. 큰 프로그램을 함수로 나누면 어떤 장점이 있는가?

---

## BLOCK 07 · 코드를 파일로 나누고 실패를 안전하게 다룬다

### LESSON 01 · module·import·파일 입출력·exception·try/except를 실제 상황으로 연결한다

#### 1. 코드가 10,000줄이면 한 파일에 모두 넣을까

작은 프로그램은 한 파일에 쓸 수 있다.

하지만 기능이 늘면:

```text
사용자 기능
주문 기능
계산 기능
파일 저장 기능
```

을 역할별로 나누는 편이 이해하기 쉽다.

관련 코드를 나눈 단위를 넓게 **module(모듈)**이라고 부른다.

Python에서는 `.py` 파일 하나가 모듈로 사용될 수 있다.

예:

`math_tools.py`

```python
def add(a, b):
    return a + b
```

다른 파일에서:

```python
from math_tools import add

print(add(2, 3))
```

처럼 가져와 사용할 수 있다.

이때 `import`는 다른 모듈의 기능을 현재 코드에서 사용할 수 있게 가져오는 문법이다.

#### 2. 모듈은 파일 수를 늘리는 놀이가 아니다

아무 함수나 파일 하나씩 만들면 오히려 복잡해진다.

목적은:

```text
관련 기능을 함께 두고
서로 다른 책임은 적절히 나누고
필요한 곳에서 재사용하기
```

이다.

나중에 소프트웨어 설계 TRACK에서 책임과 모듈 경계를 깊게 배운다.

#### 3. 프로그램이 데이터를 파일에 저장해 본다

메모를 파일로 저장한다.

```python
with open("memo.txt", "w", encoding="utf-8") as file:
    file.write("안녕하세요")
```

처음 보는 문법이 많다.

한 번에 외우지 않는다.

사람말로 읽는다.

```text
memo.txt 파일을 쓰기 모드로 연다.
문자 encoding은 UTF-8로 사용한다.
열린 파일을 file이라는 이름으로 다룬다.
"안녕하세요"를 쓴다.
작업이 끝나면 파일을 정리한다.
```

TRACK 01의 파일과 UTF-8이 실제 코드와 연결됐다.

#### 4. 파일을 다시 읽는다

```python
with open("memo.txt", "r", encoding="utf-8") as file:
    text = file.read()

print(text)
```

`"r"`은 읽기 모드다.

파일에 저장된 글자를 읽어 `text` 변수에 넣고 출력한다.

#### 5. 파일이 없으면 어떻게 될까

```python
with open("없는파일.txt", "r", encoding="utf-8") as file:
    text = file.read()
```

파일이 없다면 프로그램 실행 중 오류가 발생할 수 있다.

현실 프로그램에서는 실패 가능성이 많다.

```text
파일 없음
숫자 변환 실패
네트워크 실패
권한 없음
DB 오류
```

프로그램이 예상 가능한 실패를 다룰 방법이 필요하다.

#### 6. exception은 실행 중 발생한 비정상 상황을 전달한다

Python에서는 실행 중 문제가 발생했을 때 **exception(예외)**이라는 방식으로 오류 상황을 나타낼 수 있다.

예:

```python
int("hello")
```

정수로 변환할 수 없으므로 예외가 발생한다.

#### 7. try/except로 실패를 처리한다

```python
try:
    age = int(input("나이: "))
    print(age)
except ValueError:
    print("숫자를 입력해 주세요")
```

사람말:

```text
try
→ 이 작업을 시도한다.

정상
→ age 출력

ValueError 발생
→ except로 이동
→ 사용자에게 안내
```

프로그램이 무조건 종료되는 대신 예상 가능한 실패를 다룬다.

#### 8. 아무 오류나 전부 숨기면 안 된다

다음처럼:

```python
try:
    risky_work()
except:
    pass
```

모든 오류를 아무 말 없이 무시하면 실제 버그가 숨어 버릴 수 있다.

가능하면 예상하는 오류 종류를 구분하고 필요한 기록이나 사용자 안내를 한다.

#### 9. 오류 메시지는 사람에게 도움을 줘야 한다

나쁜 메시지:

```text
error
```

조금 더 좋은 메시지:

```text
나이는 숫자로 입력해 주세요.
```

개발자용 로그와 사용자용 메시지는 목적이 다를 수 있다.

사용자에게 내부 stack trace나 비밀번호 같은 민감한 정보를 그대로 보여 주면 안 된다.

보안 TRACK에서 다시 다룬다.

#### 10. 파일 저장 프로그램을 안전하게 만든다

```python
name = input("이름: ")

try:
    with open("user.txt", "w", encoding="utf-8") as file:
        file.write(name)
    print("저장 완료")
except OSError:
    print("파일을 저장하지 못했습니다")
```

흐름:

```text
입력
↓
파일 열기 시도
↓
성공 → 저장 → 완료 출력
실패 → 오류 안내
```

단순한 프로그램이지만 실제 소프트웨어 구조의 작은 버전이다.

#### 11. 책을 덮고 확인한다

1. module을 왜 나누는가?
2. import는 어떤 역할인가?
3. 파일 읽기와 RAM의 변수는 무엇이 다른가?
4. exception은 어떤 상황을 전달하는가?
5. 모든 오류를 `except: pass`로 숨기면 왜 위험한가?

---

## BLOCK 08 · 지금까지 배운 것을 하나의 작은 프로그램으로 묶는다

### LESSON 01 · 입력·조건·반복·자료구조·함수·파일·오류처리를 한 번에 연결한다

#### 1. 최종 프로그램의 목표를 먼저 정한다

`간단한 할 일 목록 프로그램`을 만든다.

기능:

```text
1. 할 일을 추가한다.
2. 현재 할 일을 보여 준다.
3. 완료한 할 일을 지운다.
4. 파일에 저장한다.
5. 다음 실행 때 파일에서 다시 읽는다.
6. 잘못된 입력이 들어와도 가능한 범위에서 안내한다.
```

바로 코드를 쓰지 않는다.

먼저 데이터 모양을 정한다.

```text
할 일 여러 개
→ list
```

예:

```python
todos = ["약 사기", "전화하기"]
```

#### 2. 가장 작은 기능부터 만든다

출력 함수:

```python
def show_todos(todos):
    if len(todos) == 0:
        print("할 일이 없습니다")
        return

    for index, todo in enumerate(todos):
        print(index, todo)
```

처음 보는 `enumerate`는 목록을 돌면서 index와 값을 함께 받게 해 주는 기능이라고 이해한다.

예:

```text
0 약 사기
1 전화하기
```

#### 3. 추가 기능을 만든다

```python
def add_todo(todos, text):
    if text == "":
        return False

    todos.append(text)
    return True
```

입력 문자열이 비어 있으면 추가하지 않는다.

성공 여부를 True/False로 돌려준다.

#### 4. 삭제 기능을 만든다

```python
def remove_todo(todos, index):
    if index < 0 or index >= len(todos):
        return False

    todos.pop(index)
    return True
```

여기에는:

```text
조건
논리 연산
list 길이
index
return
```

이 함께 들어 있다.

#### 5. 파일 저장 함수를 만든다

복잡한 JSON은 다음 TRACK 이후에 더 다룬다.

지금은 한 줄에 할 일 하나를 저장한다.

```python
def save_todos(todos):
    with open("todos.txt", "w", encoding="utf-8") as file:
        for todo in todos:
            file.write(todo + "\n")
```

각 할 일을 파일에 한 줄씩 기록한다.

#### 6. 파일에서 읽는 함수를 만든다

```python
def load_todos():
    todos = []

    try:
        with open("todos.txt", "r", encoding="utf-8") as file:
            for line in file:
                todos.append(line.strip())
    except FileNotFoundError:
        pass

    return todos
```

첫 실행에서는 파일이 없을 수 있다.

그 경우 빈 목록으로 시작한다.

`strip()`은 줄 끝의 줄바꿈과 앞뒤 공백을 정리하는 데 사용할 수 있다.

#### 7. 메뉴 반복을 만든다

```python
todos = load_todos()

while True:
    print("1 추가 / 2 보기 / 3 삭제 / 0 종료")
    command = input("> ")

    if command == "1":
        text = input("할 일: ")
        if add_todo(todos, text):
            save_todos(todos)
            print("추가했습니다")
        else:
            print("빈 할 일은 추가할 수 없습니다")

    elif command == "2":
        show_todos(todos)

    elif command == "3":
        show_todos(todos)
        try:
            index = int(input("삭제할 번호: "))
            if remove_todo(todos, index):
                save_todos(todos)
                print("삭제했습니다")
            else:
                print("없는 번호입니다")
        except ValueError:
            print("숫자를 입력해 주세요")

    elif command == "0":
        break

    else:
        print("알 수 없는 명령입니다")
```

코드가 길어졌다.

하지만 완전히 새로운 마법은 거의 없다.

이전 LESSON에서 배운 작은 요소가 연결됐을 뿐이다.

```text
변수
list
함수
if/elif/else
while
input
int 변환
try/except
파일 읽기/쓰기
break
```

#### 8. 긴 코드를 읽는 방법

위 코드를 첫 줄부터 끝까지 한 번에 이해하려고 하지 않는다.

기능별로 나눈다.

```text
데이터 불러오기
↓
메뉴 반복
↓
명령 입력
├─ 추가
├─ 보기
├─ 삭제
└─ 종료
```

그리고 각 갈래를 다시 읽는다.

이것이 큰 코드를 읽는 기본 방법이다.

#### 9. 객체와 class를 지금 아주 가볍게 맛본다

할 일을 문자열 하나가 아니라 상태와 함께 저장하고 싶다고 하자.

```text
제목 = 약 사기
완료 여부 = False
```

dict로 만들 수 있다.

```python
todo = {
    "text": "약 사기",
    "done": False
}
```

또 Python에서는 관련 데이터와 기능을 **class**라는 구조로 묶을 수도 있다.

```python
class Todo:
    def __init__(self, text):
        self.text = text
        self.done = False
```

지금 class 문법을 외우지 않는다.

다음 정도만 이해한다.

```text
Todo라는 데이터 모양을 만든다.
각 Todo는 text와 done 상태를 가진다.
```

객체지향을 깊게 배우는 것은 이후 설계 단계에서 다시 한다.

#### 10. AI에게 이 프로그램을 고쳐 달라고 할 때 무엇을 확인해야 할까

AI가 `할 일 목록을 더 좋게 만들어 줬습니다`라고 코드를 바꿨다고 하자.

그대로 믿지 않는다.

확인 목록:

```text
기존 파일을 읽을 수 있는가?
빈 목록에서 정상인가?
잘못된 삭제 번호에서 종료되지 않는가?
추가 후 실제 파일에 저장되는가?
재실행 후 데이터가 남는가?
한글 encoding이 깨지지 않는가?
```

이것이 앞으로 계속 사용할 검증 태도다.

#### 11. TRACK 02 완료 기준

책을 보지 않고 다음을 직접 해 본다.

- 문제를 목표·입력·처리·출력으로 나눈다.
- int/float/string/Boolean/None의 큰 차이를 설명한다.
- 변수와 비교 연산을 사용한다.
- 사용자 입력을 숫자로 변환한다.
- if/elif/else로 세 갈래 조건을 만든다.
- for와 while을 사용한다.
- list/dict/set을 상황에 맞게 선택한다.
- 함수를 만들고 parameter와 return을 사용한다.
- 파일을 UTF-8로 읽고 쓴다.
- try/except로 예상 가능한 입력 오류를 처리한다.
- 긴 프로그램을 기능 단위로 나누어 읽는다.

### TRACK 프로젝트 · 할 일 목록 프로그램 개선

위 프로그램에 다음을 한 기능씩 추가한다.

1. 할 일 완료 상태 추가
2. 완료/미완료 표시
3. 전체 삭제 전에 사용자에게 확인
4. 같은 빈 문자열 입력 거부
5. 프로그램 시작 시 저장 파일 불러오기
6. 파일이 없을 때 정상 시작
7. 잘못된 메뉴 번호 처리
8. 숫자가 아닌 삭제 index 처리

한 번에 모두 추가하지 않는다.

```text
기능 하나 추가
→ 실행
→ 정상 예제 확인
→ 잘못된 입력 확인
→ 다음 기능
```

순서로 진행한다.

이 TRACK의 목적은 Python 문법 70개를 외우는 것이 아니다.

**값을 저장하고, 조건을 판단하고, 반복하고, 기능을 나누고, 실패를 처리해서 작은 프로그램 하나를 끝까지 만드는 경험**이 목적이다.
