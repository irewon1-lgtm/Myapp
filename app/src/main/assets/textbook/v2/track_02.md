# TRACK 02 · 프로그래밍 사고와 문법

TRACK 01에서 컴퓨터와 코드의 가장 기본적인 말을 배웠다.

이제부터는 실제로 프로그램을 만들기 위한 생각을 배운다.

이 TRACK의 목표는 문법을 외우는 것이 아니다.

> 문제를 작은 단계로 나누고, 값을 저장하고, 조건을 판단하고, 반복하고, 함수를 만들고, 오류를 처리해서 **직접 동작하는 프로그램을 만드는 것**

이다.

예제는 처음에는 Python을 사용한다. Python 자체를 외우기보다 다른 언어에도 반복해서 등장하는 공통 개념을 익힌다.

---

## BLOCK 01 · 문제를 코드로 바꾸기 전에

### LESSON 01 · 문제를 한 문장으로 정의하기

코드를 바로 쓰지 않는다.

먼저 무엇을 만들 것인지 한 문장으로 적는다.

예:

```text
상품 가격과 수량을 받아 총가격을 계산하는 프로그램을 만든다.
```

이 한 문장이 **목표**다.

### LESSON 02 · 입력과 출력부터 정한다

목표가 정해졌다면 입력과 출력을 나눈다.

```text
입력: 상품 가격, 수량
출력: 총가격
```

그 다음에야 처리를 정한다.

```text
처리: 가격 × 수량
```

이렇게 하면 코드를 쓰기 전에 프로그램 구조가 보인다.

---

## BLOCK 02 · 큰 문제를 작은 문제로 나누기

### LESSON 01 · 분해

큰 문제를 작은 작업으로 나누는 것을 **분해(decomposition)**라고 부른다.

예를 들어 회원가입 기능은:

```text
이름 입력 받기
이메일 입력 받기
비밀번호 입력 받기
입력값 검사하기
서버에 보내기
성공/실패 보여주기
```

처럼 나눌 수 있다.

### LESSON 02 · 왜 나누는가

한 번에 큰 문제 전체를 생각하면 오류 위치도 찾기 어렵다.

작은 단계로 나누면:

```text
각 단계가 무엇을 받는지
무엇을 하는지
무엇을 내놓는지
```

확인하기 쉬워진다.

---

## BLOCK 03 · 값과 리터럴

### LESSON 01 · 값 다시 보기

프로그램이 다루는 실제 내용을 **값(value)**이라고 했다.

```text
10
3.14
"안녕"
True
```

### LESSON 02 · 리터럴

코드에 값을 직접 적어 넣은 표현을 **리터럴(literal)**이라고 부른다.

예:

```python
10
"hello"
True
```

여기서 `10`, `"hello"`, `True`는 각각 값을 직접 나타낸 리터럴이다.

한 줄 뜻:

> literal = 코드 안에 값을 직접 적어 놓은 표현

---

## BLOCK 04 · 정수와 실수

### LESSON 01 · 정수

소수점이 없는 숫자 값을 **정수(integer)**라고 부른다.

```python
10
0
-3
```

Python에서는 정수를 `int` 타입으로 다룬다.

### LESSON 02 · 실수

소수점이 있는 숫자를 다룰 때 **부동소수점 수(floating-point number)**를 사용한다.

```python
3.14
0.5
-2.7
```

Python에서는 보통 `float` 타입이다.

### LESSON 03 · 컴퓨터의 소수 계산은 완벽한 십진수 계산과 다를 수 있다

다음을 실행해 보자.

```python
print(0.1 + 0.2)
```

환경에 따라 다음처럼 보일 수 있다.

```text
0.30000000000000004
```

컴퓨터가 많은 실수를 이진수 기반 근삿값으로 저장하기 때문이다.

돈 계산처럼 정확한 소수 계산이 중요한 곳에서는 전용 숫자 타입이나 정수 단위를 사용할 수 있다.

---

## BLOCK 05 · 문자열

### LESSON 01 · 글자 묶음

글자와 문장을 다루는 값을 **문자열(string)**이라고 부른다.

Python에서는 따옴표로 감쌀 수 있다.

```python
name = "민수"
message = "안녕하세요"
```

### LESSON 02 · 문자열 길이

문자열에 글자가 몇 개 있는지 `len()`으로 확인할 수 있다.

```python
print(len("hello"))
```

결과:

```text
5
```

### LESSON 03 · 문자열도 데이터다

사용자 이름, 이메일, 검색어, JSON의 일부 등 프로그램에서 문자열은 매우 자주 사용된다.

문자열은 화면 장식이 아니라 프로그램이 처리하는 데이터다.

---

## BLOCK 06 · Boolean

### LESSON 01 · 참과 거짓

참 또는 거짓 두 상태를 나타내는 값을 **Boolean**이라고 부른다.

Python에서는:

```python
True
False
```

를 사용한다.

### LESSON 02 · 비교 결과는 Boolean

```python
print(10 > 5)
```

결과는:

```text
True
```

`10 > 5` 자체가 Boolean 값을 만드는 표현식이다.

---

## BLOCK 07 · None

### LESSON 01 · 값이 없음을 나타내기

프로그램에서는 `아직 값이 없음`을 표현해야 할 때가 있다.

Python에는 **None**이라는 특별한 값이 있다.

```python
result = None
```

한 줄 뜻:

> None = 현재 의미 있는 값이 없다는 것을 나타내는 특별한 값

### LESSON 02 · 빈 문자열과 None은 다르다

```text
""   = 길이가 0인 문자열 값
None = 값이 없음을 나타내는 특별한 값
```

둘은 다른 상태다.

---

## BLOCK 08 · 변수

### LESSON 01 · 값에 이름 붙이기

```python
price = 1200
```

이 코드는 `price`라는 이름으로 `1200` 값을 다룰 수 있게 한다.

### LESSON 02 · 좋은 변수 이름

```python
x = 1200
```

보다:

```python
product_price = 1200
```

가 의미를 이해하기 쉽다.

좋은 이름은 코드 자체가 설명이 되게 만든다.

---

## BLOCK 09 · 상수라는 생각

### LESSON 01 · 바뀌지 않아야 하는 값

프로그램 실행 중 바꾸지 않기로 약속한 값을 **상수(constant)**라고 부른다.

Python은 언어 차원에서 일반 변수와 완전히 다른 상수를 강제하지는 않지만 관례적으로 대문자 이름을 사용한다.

```python
TAX_RATE = 0.1
```

### LESSON 02 · 왜 구분하나

세율이나 최대 시도 횟수처럼 의도적으로 고정한 값이 중간에 바뀌면 버그가 생길 수 있다.

이름과 코드 구조로 `바꾸지 말아야 하는 값`이라는 의도를 표현한다.

---

## BLOCK 10 · 표현식

### LESSON 01 · 값을 만들어 내는 코드 조각

계산하거나 비교해서 하나의 값을 만들어 내는 코드 조각을 **표현식(expression)**이라고 부른다.

```python
10 + 20
price * count
age >= 19
```

각 표현식은 결과값을 만든다.

### LESSON 02 · 문장과 표현식

프로그래밍 언어에는 값을 만드는 표현식과 어떤 동작을 지시하는 문장이 있다.

언어마다 정확한 분류 규칙은 다르다.

지금은 `이 부분이 어떤 값을 만들어 내는가?`를 질문하는 습관을 만든다.

---

## BLOCK 11 · 산술 연산자

### LESSON 01 · 덧셈·뺄셈·곱셈·나눗셈

```python
print(10 + 3)
print(10 - 3)
print(10 * 3)
print(10 / 3)
```

### LESSON 02 · 몫과 나머지

```python
print(10 // 3)
print(10 % 3)
```

`//`는 정수 나눗셈의 몫을 구하는 데 사용할 수 있다.

`%`는 나머지를 구한다.

짝수 확인에도 사용할 수 있다.

```python
number = 8
print(number % 2 == 0)
```

---

## BLOCK 12 · 비교 연산자

### LESSON 01 · 크기 비교

```python
10 > 5
10 < 5
10 >= 10
10 <= 9
```

결과는 True 또는 False다.

### LESSON 02 · 같음과 다름

```python
10 == 10
10 != 20
```

`==`는 같은지 비교한다.

`!=`는 다른지 비교한다.

`=`와 `==`를 구분한다.

```text
=  값을 이름에 연결
== 두 값이 같은지 비교
```

---

## BLOCK 13 · 논리 연산자

### LESSON 01 · and

두 조건이 모두 참이어야 할 때 `and`를 사용한다.

```python
age = 25
has_ticket = True

print(age >= 19 and has_ticket)
```

### LESSON 02 · or

둘 중 하나 이상이 참이면 되는 경우 `or`를 사용한다.

```python
is_admin = False
is_owner = True
print(is_admin or is_owner)
```

### LESSON 03 · not

참과 거짓을 반대로 바꿀 때 `not`을 사용한다.

```python
is_closed = False
print(not is_closed)
```

---

## BLOCK 14 · 입력 받기

### LESSON 01 · input

Python에서 간단한 사용자 입력을 받는 데 `input()`을 사용할 수 있다.

```python
name = input("이름: ")
print(name)
```

### LESSON 02 · input 결과는 문자열

사용자가 `10`을 입력해도 `input()`은 문자열을 돌려준다.

```python
age = input("나이: ")
```

숫자 계산을 하려면 타입 변환이 필요하다.

---

## BLOCK 15 · 타입 변환

### LESSON 01 · 문자열을 정수로

```python
age_text = "20"
age = int(age_text)
```

`int()`는 정수로 바꿀 수 있는 값을 정수 타입으로 변환한다.

### LESSON 02 · 변환 실패

```python
int("안녕")
```

은 정수로 바꿀 수 없어 오류가 발생한다.

사용자 입력은 항상 올바르다고 가정하면 안 된다.

---

## BLOCK 16 · if

### LESSON 01 · 조건이 참일 때만 실행

```python
age = 20

if age >= 19:
    print("성인")
```

사람말:

```text
age가 19 이상이면 "성인"을 출력한다.
```

### LESSON 02 · 들여쓰기

Python에서는 들여쓰기가 코드 구조를 나타낸다.

```python
if age >= 19:
    print("성인")
```

`print` 앞의 공백은 장식이 아니라 `if` 안에 속한다는 뜻이다.

---

## BLOCK 17 · else와 elif

### LESSON 01 · else

조건이 거짓일 때 실행할 코드를 `else`에 적는다.

```python
if age >= 19:
    print("성인")
else:
    print("미성년")
```

### LESSON 02 · elif

여러 조건을 차례로 검사할 때 `elif`를 사용할 수 있다.

```python
score = 85

if score >= 90:
    grade = "A"
elif score >= 80:
    grade = "B"
else:
    grade = "C"
```

조건의 순서가 결과에 영향을 준다.

---

## BLOCK 18 · 중첩 조건

### LESSON 01 · if 안의 if

```python
if is_logged_in:
    if is_admin:
        print("관리자")
```

조건 안에 또 조건을 넣을 수 있다.

### LESSON 02 · 너무 깊은 중첩

조건이 여러 겹 들어가면 읽기 어려워진다.

```text
if
  if
    if
      if
```

가능하면 조건을 함수로 분리하거나 빠른 반환 등을 사용해 구조를 단순하게 만들 수 있다.

---

## BLOCK 19 · for 반복문

### LESSON 01 · 정해진 개수 반복

```python
for number in range(3):
    print(number)
```

결과:

```text
0
1
2
```

### LESSON 02 · 리스트 순회

```python
names = ["민수", "지수", "철수"]

for name in names:
    print(name)
```

리스트 안의 값을 하나씩 꺼내 처리한다.

---

## BLOCK 20 · while 반복문

### LESSON 01 · 조건이 참인 동안 반복

```python
count = 0

while count < 3:
    print(count)
    count = count + 1
```

### LESSON 02 · 무한 반복

조건이 영원히 참이면 반복이 끝나지 않는다.

```python
while True:
    print("계속")
```

서버처럼 의도적으로 무한 루프가 필요한 경우도 있지만 종료 조건과 대기 구조를 잘못 만들면 CPU를 계속 사용할 수 있다.

---

## BLOCK 21 · break와 continue

### LESSON 01 · break

반복을 즉시 끝내고 싶을 때 `break`를 사용할 수 있다.

```python
for number in range(10):
    if number == 5:
        break
    print(number)
```

### LESSON 02 · continue

현재 반복만 건너뛰고 다음 반복으로 가려면 `continue`를 사용한다.

```python
for number in range(5):
    if number == 2:
        continue
    print(number)
```

---

## BLOCK 22 · 리스트

### LESSON 01 · 여러 값을 순서대로 저장

Python의 **list**는 여러 값을 순서대로 담는 자료구조다.

```python
scores = [80, 90, 100]
```

### LESSON 02 · 인덱스

리스트의 위치 번호를 **인덱스(index)**라고 부른다.

Python에서는 0부터 시작한다.

```python
print(scores[0])
```

결과:

```text
80
```

### LESSON 03 · 범위를 벗어나면

```python
scores[10]
```

처럼 존재하지 않는 위치를 읽으면 오류가 발생한다.

경계값 오류의 대표적인 예다.

---

## BLOCK 23 · 리스트 수정

### LESSON 01 · append

리스트 뒤에 값을 추가한다.

```python
scores.append(70)
```

### LESSON 02 · remove

특정 값을 삭제할 수 있다.

```python
scores.remove(90)
```

### LESSON 03 · 값 변경

```python
scores[0] = 85
```

리스트처럼 내부 값이 바뀔 수 있는 객체를 **mutable(변경 가능)**하다고 부른다.

---

## BLOCK 24 · tuple

### LESSON 01 · 변경하지 않는 순서 묶음

Python의 **tuple**은 여러 값을 순서대로 담지만 일반적으로 생성 후 요소를 바꿀 수 없다.

```python
point = (10, 20)
```

### LESSON 02 · list와 tuple

```text
list  = 내부 요소 변경 가능
 tuple = 생성 후 요소 변경 불가
```

의도를 표현할 때 구분할 수 있다.

---

## BLOCK 25 · dictionary

### LESSON 01 · 이름과 값으로 찾기

Python의 **dictionary(dict)**는 키와 값을 연결해 저장한다.

```python
user = {
    "name": "민수",
    "age": 20
}
```

### LESSON 02 · key와 value

```text
"name" → key
"민수" → value
```

키를 사용해 값을 찾는다.

```python
print(user["name"])
```

---

## BLOCK 26 · set

### LESSON 01 · 중복 없는 값 모음

**set**은 같은 값을 여러 번 저장하지 않는 집합 자료구조다.

```python
numbers = {1, 2, 2, 3}
print(numbers)
```

중복 `2`는 하나만 남는다.

### LESSON 02 · 포함 여부 검사

```python
print(2 in numbers)
```

값이 있는지 빠르게 확인할 때 유용하다.

---

## BLOCK 27 · 함수가 필요한 이유

### LESSON 01 · 같은 로직을 여러 번 쓰지 않기

```python
def greet(name):
    print("안녕", name)
```

특정 일을 이름 붙여 묶은 코드를 **함수(function)**라고 부른다.

### LESSON 02 · 호출

함수를 실행시키는 것을 **호출(call)**이라고 부른다.

```python
greet("민수")
```

---

## BLOCK 28 · parameter와 argument

### LESSON 01 · parameter

함수를 정의할 때 들어올 값을 받을 이름을 **parameter(매개변수)**라고 부른다.

```python
def greet(name):
```

여기서 `name`이 parameter다.

### LESSON 02 · argument

함수를 실제 호출할 때 넘기는 값을 **argument(인자)**라고 부른다.

```python
greet("민수")
```

`"민수"`가 argument다.

```text
parameter = 함수 정의의 입력 자리 이름
argument  = 호출할 때 실제로 넘긴 값
```

---

## BLOCK 29 · return

### LESSON 01 · 함수 결과 돌려주기

```python
def add(a, b):
    return a + b
```

`return`은 함수의 결과를 호출한 곳으로 돌려준다.

```python
result = add(2, 3)
print(result)
```

결과:

```text
5
```

### LESSON 02 · print와 return은 다르다

`print`는 화면에 값을 보여 준다.

`return`은 함수 결과를 다른 코드가 사용할 수 있게 돌려준다.

둘을 같은 것으로 생각하면 안 된다.

---

## BLOCK 30 · scope

### LESSON 01 · 변수가 보이는 범위

변수가 코드 어디에서 사용할 수 있는지를 **스코프(scope)**라고 부른다.

```python
def test():
    inside = 10
    print(inside)
```

`inside`는 함수 내부에서 만들어졌다.

함수 밖에서는 바로 사용할 수 없다.

### LESSON 02 · 지역 변수와 전역 변수

함수 안에서 만든 변수를 **지역 변수(local variable)**라고 부른다.

함수 밖의 넓은 영역에 만든 변수를 **전역 변수(global variable)**라고 부를 수 있다.

전역 변수를 많이 사용하면 여러 함수가 같은 값을 바꾸어 상태 추적이 어려워질 수 있다.

---

## BLOCK 31 · 함수는 작은 프로그램처럼 생각한다

### LESSON 01 · 입력 → 처리 → 출력

함수도 다음 구조로 볼 수 있다.

```text
입력: parameter
처리: 함수 내부 코드
출력: return 값
```

이 구조로 함수 하나를 이해하면 큰 프로그램도 작은 함수들로 나눌 수 있다.

### LESSON 02 · 한 함수에 한 역할

함수 하나가:

```text
파일 읽기
DB 저장
이메일 보내기
화면 그리기
```

를 모두 한다면 수정하기 어려워진다.

가능하면 한 함수가 한 가지 분명한 책임을 가지게 한다.

---

## BLOCK 32 · 재귀

### LESSON 01 · 함수가 자기 자신을 호출한다

함수가 자기 자신을 다시 호출하는 방식을 **재귀(recursion)**라고 부른다.

```python
def countdown(n):
    if n == 0:
        return
    print(n)
    countdown(n - 1)
```

### LESSON 02 · 종료 조건

재귀에는 반드시 멈출 조건이 필요하다.

위 예제에서는 `n == 0`이 종료 조건이다.

종료 조건이 없으면 호출이 계속 쌓여 오류가 날 수 있다.

---

## BLOCK 33 · 예외 처리

### LESSON 01 · exception

실행 중 정상 흐름을 이어가기 어려운 문제가 발생했음을 나타내는 구조를 **예외(exception)**라고 부른다.

### LESSON 02 · try/except

Python에서는:

```python
try:
    age = int(input("나이: "))
except ValueError:
    print("숫자를 입력하세요")
```

처럼 오류를 처리할 수 있다.

### LESSON 03 · 모든 예외를 무시하지 않는다

```python
try:
    dangerous_work()
except:
    pass
```

처럼 모든 오류를 아무 기록 없이 무시하면 실제 문제를 숨길 수 있다.

처리할 수 있는 오류만 구체적으로 처리한다.

---

## BLOCK 34 · 모듈

### LESSON 01 · 코드를 파일로 나누기

프로그램이 커지면 한 파일에 모든 코드를 넣기 어렵다.

관련된 코드를 파일이나 묶음으로 나눈 것을 **모듈(module)**이라고 부른다.

### LESSON 02 · import

Python에서는 다른 모듈의 기능을 가져오기 위해 `import`를 사용한다.

```python
import math
print(math.sqrt(9))
```

---

## BLOCK 35 · 객체

### LESSON 01 · 값과 기능을 함께 묶기

프로그램에서 관련된 데이터와 동작을 하나의 단위로 다루는 것을 **객체(object)**라고 부른다.

예를 들어 사용자 객체는:

```text
name
age
email
```

같은 데이터를 가질 수 있고:

```text
login()
logout()
```

같은 동작을 가질 수도 있다.

### LESSON 02 · 객체와 dictionary는 같은 말이 아니다

dictionary는 key-value 자료구조다.

객체는 프로그래밍 언어에서 데이터와 동작을 묶는 더 넓은 개념으로 사용된다.

---

## BLOCK 36 · class

### LESSON 01 · 객체를 만들기 위한 설계

비슷한 종류의 객체를 만들기 위한 구조를 **클래스(class)**라고 부른다.

```python
class User:
    def __init__(self, name):
        self.name = name
```

처음에는 문법보다:

```text
User라는 종류의 객체를 만들기 위한 설계
```

라고 이해한다.

### LESSON 02 · instance

클래스를 이용해 실제로 만든 개별 객체를 **인스턴스(instance)**라고 부른다.

```python
user1 = User("민수")
user2 = User("지수")
```

`user1`, `user2`는 서로 다른 인스턴스다.

---

## BLOCK 37 · reference와 mutation

### LESSON 01 · 같은 객체를 가리킬 수 있다

```python
a = [1, 2]
b = a
b.append(3)
print(a)
```

결과:

```text
[1, 2, 3]
```

`a`와 `b`가 같은 리스트 객체를 가리키기 때문이다.

### LESSON 02 · mutation

객체 자체의 내부 상태를 바꾸는 것을 **mutation(변경)**이라고 부른다.

참조를 공유하는 객체를 수정하면 예상하지 못한 곳까지 값이 바뀔 수 있다.

---

## BLOCK 38 · copy

### LESSON 01 · 얕은 복사

리스트 같은 중첩 구조를 복사할 때 바깥 구조만 복사하고 안쪽 객체는 공유할 수 있다.

이를 **얕은 복사(shallow copy)**라고 부른다.

### LESSON 02 · 깊은 복사

중첩된 내부 객체까지 새로 복사하는 것을 **깊은 복사(deep copy)**라고 부른다.

복사 비용이 더 크고 항상 필요한 것은 아니다.

어떤 부분을 공유해도 되는지 이해해야 한다.

---

## BLOCK 39 · map, filter의 생각

### LESSON 01 · map

여러 값 각각에 같은 변환을 적용해 새 값을 만들 수 있다.

```python
numbers = [1, 2, 3]
squares = list(map(lambda x: x * x, numbers))
```

`lambda`는 이름 없는 작은 함수다. 지금은 결과만 본다.

### LESSON 02 · filter

조건에 맞는 값만 고를 수 있다.

```python
numbers = [1, 2, 3, 4]
evens = list(filter(lambda x: x % 2 == 0, numbers))
```

반복문으로도 만들 수 있다. 목적과 가독성에 따라 선택한다.

---

## BLOCK 40 · pure function 맛보기

### LESSON 01 · 같은 입력이면 같은 출력

외부 상태를 바꾸지 않고 같은 입력에 항상 같은 결과를 돌려주는 함수를 **순수 함수(pure function)**라고 부른다.

```python
def add(a, b):
    return a + b
```

### LESSON 02 · 왜 유용한가

순수 함수는 다른 숨은 상태에 덜 의존해서 테스트와 이해가 쉽다.

모든 함수를 순수 함수로 만들 수는 없지만 핵심 계산 로직을 순수하게 분리하면 좋다.

---

## BLOCK 41 · 프로그램 상태 추적

### LESSON 01 · 한 줄씩 상태표 만들기

```python
count = 1
count = count + 2
count = count * 3
```

상태표:

```text
1줄 후 count = 1
2줄 후 count = 3
3줄 후 count = 9
```

이 습관은 복잡한 코드 디버깅의 기초다.

### LESSON 02 · 함수 호출도 적는다

```python
def double(x):
    return x * 2

result = double(4)
```

```text
x = 4
return 8
result = 8
```

처럼 추적한다.

---

## BLOCK 42 · 코드 스타일

### LESSON 01 · 기계만 읽는 코드가 아니다

코드는 사람도 읽는다.

좋은 코드는:

```text
의도가 드러나는 이름
짧고 분명한 함수
일관된 들여쓰기
중복 감소
```

를 가진다.

### LESSON 02 · formatter

코드의 들여쓰기와 줄바꿈을 자동으로 맞추는 도구를 **포매터(formatter)**라고 부른다.

예:

```text
Black
Prettier
ktfmt
```

스타일 논쟁보다 자동 도구로 일관성을 유지할 수 있다.

---

## BLOCK 43 · 작은 프로젝트 1 · 총가격 계산기

### LESSON 01 · 요구사항

```text
상품 가격 입력
수량 입력
총가격 계산
10,000원 이상이면 "무료배송"
아니면 "배송비 3,000원"
```

### LESSON 02 · 먼저 의사코드

프로그래밍 언어 문법이 아니라 사람말과 비슷하게 절차를 적는 것을 **의사코드(pseudocode)**라고 부른다.

```text
가격 입력
수량 입력
둘을 곱해 총가격 계산
총가격이 10000 이상인지 비교
조건에 따라 배송 메시지 출력
```

### LESSON 03 · 코드

```python
price = int(input("가격: "))
count = int(input("수량: "))
total = price * count

print("총가격:", total)

if total >= 10000:
    print("무료배송")
else:
    print("배송비 3000원")
```

---

## BLOCK 44 · 작은 프로젝트 2 · 성적 관리

### LESSON 01 · 요구사항

여러 점수를 리스트에 저장한다.

평균을 구한다.

90 이상은 A, 80 이상은 B, 나머지는 C로 분류한다.

### LESSON 02 · 직접 구현

```python
scores = [95, 82, 73, 88]

total = 0
for score in scores:
    total = total + score

average = total / len(scores)
print("평균:", average)
```

그 다음 각 점수를 등급으로 바꾸는 함수를 직접 작성한다.

---

## BLOCK 45 · 핵심 용어 사전

| 용어 | 아주 쉬운 뜻 |
|---|---|
| decomposition | 큰 문제를 작은 문제로 나누는 것 |
| literal | 코드에 값을 직접 적은 표현 |
| integer | 소수점 없는 정수 값 |
| float | 소수 값을 표현하는 부동소수점 타입 |
| string | 글자와 문장의 값 |
| Boolean | True/False 두 값을 가지는 타입 |
| None | 의미 있는 값이 없음을 나타내는 특별한 값 |
| expression | 계산·비교해 하나의 값을 만드는 코드 조각 |
| operator | 값에 계산·비교를 적용하는 기호 |
| if | 조건이 참일 때 코드를 실행하는 문법 |
| loop | 같은 종류의 작업을 반복하는 구조 |
| list | 여러 값을 순서대로 담는 자료구조 |
| index | 순서 자료구조에서 위치를 나타내는 번호 |
| dictionary | key와 value를 연결해 저장하는 자료구조 |
| set | 중복 없는 값을 저장하는 집합 자료구조 |
| function | 특정 일을 이름 붙여 묶은 코드 |
| parameter | 함수 정의에서 입력을 받을 이름 |
| argument | 함수 호출 때 실제로 넘기는 값 |
| return | 함수 결과를 호출한 곳으로 돌려주는 문법 |
| scope | 변수를 사용할 수 있는 코드 범위 |
| recursion | 함수가 자기 자신을 호출하는 방식 |
| exception | 실행 중 정상 흐름을 이어가기 어려운 문제를 나타내는 구조 |
| module | 관련 코드를 나눈 파일 또는 묶음 |
| object | 관련 데이터와 동작을 하나로 묶어 다루는 단위 |
| class | 비슷한 객체를 만들기 위한 설계 |
| instance | 클래스로 만든 실제 개별 객체 |
| mutation | 객체 내부 상태를 바꾸는 것 |
| shallow copy | 바깥 구조만 복사하고 내부 객체는 공유할 수 있는 복사 |
| deep copy | 중첩 내부까지 새 객체로 복사하는 방식 |
| pure function | 같은 입력에 같은 출력을 내고 외부 상태를 바꾸지 않는 함수 |
| pseudocode | 실제 언어 문법 대신 절차를 사람말에 가깝게 적은 표현 |

---

## BLOCK 46 · TRACK 02 완료 기준

다음을 직접 할 수 있어야 한다.

- 문제를 입력·처리·출력으로 나눈다.
- 큰 기능을 작은 단계로 분해한다.
- 정수·실수·문자열·Boolean·None을 구분한다.
- 변수와 상수의 의도를 설명한다.
- 산술·비교·논리 연산자를 사용한다.
- 사용자 입력을 받고 타입을 변환한다.
- if/elif/else로 조건을 만든다.
- for/while 반복을 작성한다.
- list/dict/set의 기본 사용 목적을 구분한다.
- 함수를 정의하고 parameter, argument, return을 설명한다.
- scope 문제를 찾는다.
- 재귀의 종료 조건을 설명한다.
- try/except로 예상 가능한 오류를 처리한다.
- 모듈을 나누고 import한다.
- 객체·class·instance의 기본 차이를 설명한다.
- 참조를 공유한 mutable 객체에서 생길 수 있는 문제를 설명한다.
- 작은 프로그램을 의사코드부터 직접 만든다.

### TRACK 프로젝트 · 콘솔 가계부

다음 기능을 가진 프로그램을 만든다.

```text
수입 추가
지출 추가
전체 기록 보기
현재 잔액 계산
카테고리별 지출 합계
잘못된 숫자 입력 처리
프로그램 종료
```

필수 조건:

1. 기록 하나를 dictionary로 표현한다.
2. 여러 기록을 list에 저장한다.
3. 기능을 함수로 나눈다.
4. 메뉴를 반복문으로 계속 보여 준다.
5. 사용자 입력 오류를 예외 처리한다.
6. 최소 10개의 서로 다른 입력으로 직접 실행한다.
7. 정상 입력뿐 아니라 빈 입력, 음수, 글자 입력도 시험한다.

완성 후 `왜 이렇게 나눴는지`를 설명할 수 있어야 통과다.
