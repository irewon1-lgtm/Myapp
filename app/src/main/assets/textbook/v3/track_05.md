# TRACK 05 · JavaScript와 TypeScript 깊게

TRACK 04에서는 HTML로 구조를 만들고 CSS로 모양을 만들고 JavaScript로 DOM을 바꿔 화면을 움직여 봤다.

이번 TRACK에서는 JavaScript 자체를 더 깊게 배운다.

하지만 처음부터 `closure`, `Promise`, `event loop`, `microtask` 같은 단어를 외우지 않는다. 먼저 실제 코드에서 문제가 생기는 상황을 본다.

이 TRACK의 핵심 질문은 계속 같다.

```text
지금 어떤 값이 있는가?
어느 함수가 실행 중인가?
다음에 어떤 코드가 실행되는가?
시간이 걸리는 작업은 언제 끝나는가?
실패하면 어디로 가는가?
여러 작업이 섞이면 결과 순서가 어떻게 되는가?
```

이 질문을 따라가면 비동기와 TypeScript도 문법 암기가 아니라 실행 흐름으로 이해할 수 있다.

---

## BLOCK 01 · JavaScript에서 값과 변수가 실제로 어떻게 움직이는가

### LESSON 01 · 실행 환경·let·const·var·primitive·object·reference·비교·자동변환을 하나의 이야기로 배운다

#### 1. JavaScript는 언어이고 브라우저는 실행 환경이다

JavaScript를 처음 배우면 `JavaScript = 브라우저`처럼 느껴질 수 있다.

하지만 둘은 구분해야 한다.

```text
JavaScript
→ 프로그래밍 언어

브라우저
→ JavaScript 엔진과 DOM, timer, network 같은 웹 기능을 제공하는 실행 환경

Node.js
→ 브라우저 밖에서 JavaScript를 실행하면서 파일·서버·프로세스 같은 기능을 제공하는 실행 환경
```

같은 JavaScript 코드라도 실행 환경에 따라 사용할 수 있는 기능이 다르다.

예를 들어 브라우저에는:

```javascript
document.querySelector("button")
```

처럼 DOM을 다루는 기능이 있다.

Node.js 서버 코드에서는 일반적으로 브라우저 DOM이 없으므로 `document`가 존재하지 않는다.

반대로 Node.js에는 파일 시스템을 다루는 기능이 있다.

이 차이를 모르면 `왜 브라우저에서는 되는데 Node에서 안 되지?` 같은 오류를 만나게 된다.

#### 2. Java와 JavaScript는 이름만 비슷하다

JavaScript와 Java는 서로 다른 프로그래밍 언어다.

```text
JavaScript
→ 웹 브라우저에서 널리 사용, Node.js에서도 실행

Java
→ JVM 생태계에서 널리 사용하는 별도 언어
```

이름이 비슷하다고 `Java의 작은 버전`이라고 생각하면 안 된다.

#### 3. let은 나중에 다른 값을 넣을 수 있는 변수다

```javascript
let score = 10;
console.log(score);
```

결과:

```text
10
```

값을 바꾼다.

```javascript
score = 20;
console.log(score);
```

결과:

```text
20
```

`let`은 **다시 다른 값을 대입할 수 있는 변수**를 선언할 때 사용한다.

#### 4. const는 이름을 다른 값에 다시 연결하지 않겠다는 뜻이다

```javascript
const taxRate = 0.1;
```

다시:

```javascript
taxRate = 0.2;
```

라고 하면 오류가 발생한다.

그래서 기본적으로 값의 연결이 바뀔 필요가 없다면 `const`, 실제로 재대입이 필요하다면 `let`을 사용하는 습관이 흔하다.

#### 5. const 객체의 내부는 바뀔 수 있다

초보자가 자주 오해한다.

```javascript
const user = {
  name: "민수",
  age: 20
};

user.age = 21;
```

이 코드는 가능하다.

왜냐하면 `const`가 막는 것은 `user`라는 변수 이름을 **다른 객체로 다시 대입하는 것**이기 때문이다.

```javascript
user = { name: "지수" }; // 오류
```

하지만 현재 user가 가리키는 객체 내부 속성은 별도의 불변 처리 없이는 바뀔 수 있다.

따라서:

```text
const = 객체 전체가 영원히 불변
```

이라고 외우면 틀린다.

#### 6. var는 왜 따로 존재할까

오래된 JavaScript 코드에서는 `var`를 많이 본다.

```javascript
var count = 0;
```

`var`는 함수 스코프와 hoisting 관련 동작이 `let`, `const`와 다르다.

예:

```javascript
if (true) {
  var x = 10;
}

console.log(x);
```

블록 밖에서도 x가 보일 수 있다.

`let`은:

```javascript
if (true) {
  let y = 10;
}

console.log(y);
```

처럼 블록 밖에서 사용할 수 없다.

처음 새 코드를 작성할 때는 `const`와 `let`을 우선 사용하고, 기존 코드에서 var를 만나면 차이를 이해하는 방향이 좋다.

#### 7. primitive는 객체가 아닌 기본 값 종류다

JavaScript에는 다음과 같은 원시 값이 있다.

```text
string
number
boolean
null
undefined
bigint
symbol
```

처음에는 자주 쓰는 것부터 본다.

```javascript
const name = "민수";   // string
const age = 20;        // number
const active = true;   // boolean
const selected = null;
let result;             // undefined
```

`undefined`와 `null`은 둘 다 `값 없음`처럼 느껴지지만 서로 다른 값이다.

```text
undefined
→ 아직 값이 할당되지 않았거나 존재하지 않는 상황에서 자주 등장

null
→ 개발자가 의도적으로 "현재 값 없음"을 표시할 때 사용할 수 있음
```

#### 8. JavaScript의 number에는 정수와 일반 소수 계산이 함께 들어간다

```javascript
const a = 10;
const b = 3.14;
```

둘 다 일반적으로 `number` 타입이다.

Python의 int/float처럼 코드상 타입 이름이 따로 보이지 않는다.

그리고 JavaScript도 부동소수점 계산 특성이 있다.

```javascript
console.log(0.1 + 0.2);
```

일반적인 결과:

```text
0.30000000000000004
```

돈 계산에서 단순 number를 어떻게 사용할지는 도메인 정확도 요구를 보고 결정해야 한다.

#### 9. 객체는 여러 값을 key와 value로 묶는다

```javascript
const user = {
  id: 1,
  name: "민수",
  age: 20
};
```

속성 접근:

```javascript
console.log(user.name);
```

또는:

```javascript
console.log(user["name"]);
```

객체는 실제 앱 상태, API 응답, 설정 등에서 매우 자주 사용한다.

#### 10. 객체를 변수에 넣을 때 reference가 중요하다

```javascript
const a = { count: 1 };
const b = a;

b.count = 2;

console.log(a.count);
```

결과는:

```text
2
```

왜 a도 바뀌었을까?

`a`와 `b`가 서로 완전히 독립된 객체 복사본을 가진 것이 아니라 **같은 객체를 가리키고 있기 때문**이다.

그림:

```text
a ─┐
   ├──→ { count: 2 }
b ─┘
```

이런 연결을 **reference(참조)** 관점으로 이해할 수 있다.

#### 11. primitive 값을 복사할 때와 비교한다

```javascript
let a = 10;
let b = a;
b = 20;

console.log(a);
```

결과:

```text
10
```

숫자 값에서 b를 바꾼다고 a가 같이 바뀌지 않는다.

객체 reference와 primitive 값의 동작 차이는 state 버그에서 매우 중요하다.

#### 12. 얕은 복사를 해도 안쪽 객체는 공유될 수 있다

```javascript
const original = {
  name: "민수",
  address: {
    city: "서울"
  }
};

const copied = { ...original };
copied.address.city = "부산";

console.log(original.address.city);
```

원본도 부산으로 바뀔 수 있다.

`{ ...original }`은 최상위 속성을 새 객체에 복사하지만 중첩 객체의 reference는 공유될 수 있기 때문이다.

```text
original.address ─┐
                 ├──→ 같은 address 객체
copied.address ───┘
```

#### 13. `===`와 `==`는 다르다

엄격한 동등 비교:

```javascript
10 === 10       // true
10 === "10"     // false
```

`===`는 타입을 강제로 맞춰 비교하지 않는다.

반면:

```javascript
10 == "10"      // true
```

`==`는 비교 과정에서 타입 변환을 할 수 있다.

초보 코드에서는 예상치 못한 자동 변환을 줄이기 위해 `===`를 기본으로 사용하는 것이 이해하기 쉽다.

#### 14. 객체 비교는 내용이 같아 보여도 reference가 다르면 false일 수 있다

```javascript
const a = { id: 1 };
const b = { id: 1 };

console.log(a === b);
```

결과:

```text
false
```

모양은 같지만 서로 다른 객체다.

```text
a → 객체 1
b → 객체 2
```

반대로:

```javascript
const a = { id: 1 };
const b = a;
console.log(a === b);
```

는 true다.

같은 객체를 가리키기 때문이다.

#### 15. type coercion은 JavaScript가 자동으로 타입을 바꾸는 동작이다

```javascript
console.log("5" + 1);
```

결과:

```text
51
```

문자열 연결이 일어났다.

```javascript
console.log("5" - 1);
```

은 또 다르게 숫자 계산이 될 수 있다.

이런 자동 타입 변환을 **type coercion**이라고 부른다.

규칙을 전부 외우기보다 입력 경계에서 의도를 명시하는 습관을 만든다.

```javascript
const count = Number(inputValue);
```

#### 16. NaN은 숫자 변환 실패에서 자주 만난다

```javascript
const value = Number("hello");
console.log(value);
```

결과:

```text
NaN
```

NaN은 `Not a Number`라는 이름이지만 JavaScript의 number 타입 안에 있는 특별한 값이다.

```javascript
console.log(Number.isNaN(value));
```

로 확인할 수 있다.

특이하게:

```javascript
NaN === NaN
```

은 false다.

그래서 NaN 검사는 전용 방법을 사용한다.

#### 17. 입력 값을 바로 믿지 않는 작은 예

```javascript
function calculateTotal(priceText, countText) {
  const price = Number(priceText);
  const count = Number(countText);

  if (Number.isNaN(price) || Number.isNaN(count)) {
    return null;
  }

  return price * count;
}
```

흐름:

```text
문자 입력
↓ Number 변환
숫자인지 확인
↓
정상이면 계산
실패면 null
```

#### 18. 책을 덮고 확인한다

1. JavaScript와 브라우저의 관계를 설명하라.
2. let과 const는 무엇이 다른가?
3. const 객체 내부 속성이 바뀔 수 있는 이유는 무엇인가?
4. `const b = a`로 객체를 대입했을 때 왜 둘이 같이 바뀔 수 있는가?
5. `===`와 `==` 차이를 예로 설명하라.
6. type coercion이 버그를 만들 수 있는 예를 하나 만들어라.
7. NaN을 확인할 때 `NaN === NaN`을 쓰면 안 되는 이유는 무엇인가?

---

## BLOCK 02 · 함수는 실행 코드이면서 동시에 값이다

### LESSON 01 · function·arrow·lexical scope·closure·this·prototype·class를 실행 흐름으로 연결한다

#### 1. JavaScript에서는 함수도 값처럼 다룬다

기본 함수:

```javascript
function add(a, b) {
  return a + b;
}
```

호출:

```javascript
console.log(add(2, 3));
```

결과 5다.

그런데 JavaScript에서는 함수를 변수에 넣을 수도 있다.

```javascript
const add = function (a, b) {
  return a + b;
};
```

함수를 다른 함수에 argument로 넘길 수도 있다.

이 성질이 event handler와 callback의 기초가 된다.

#### 2. arrow function은 짧은 문법이지만 일반 function과 완전히 같지 않다

```javascript
const add = (a, b) => {
  return a + b;
};
```

한 줄 표현:

```javascript
const add = (a, b) => a + b;
```

하지만 arrow function은 자신만의 `this`를 만드는 방식이 일반 function과 다르다.

따라서 `그냥 function을 짧게 적는 문법`이라고만 생각하면 나중에 this 버그를 이해하기 어렵다.

#### 3. scope는 변수가 보이는 범위다

```javascript
function work() {
  const message = "안쪽";
  console.log(message);
}
```

`message`는 함수 안에서 만든 변수다.

밖에서:

```javascript
console.log(message);
```

하려 하면 사용할 수 없다.

어떤 이름을 어느 범위에서 사용할 수 있는지를 **scope**라고 부른다.

#### 4. block scope도 있다

```javascript
if (true) {
  const secret = 10;
}

console.log(secret);
```

const/let은 블록 `{ ... }` 범위를 가진다.

이 때문에 같은 이름을 서로 다른 작은 범위에서 안전하게 사용할 수 있다.

#### 5. lexical scope는 함수가 어디에서 만들어졌는지가 중요하다

```javascript
const name = "바깥";

function show() {
  console.log(name);
}

show();
```

show 함수는 자신이 정의된 바깥 환경에서 `name`을 찾을 수 있다.

JavaScript는 함수가 **어디에서 정의되었는지**를 기준으로 바깥 변수 범위를 연결한다.

이를 **lexical scope(렉시컬 스코프)**라고 부른다.

#### 6. closure를 배우기 전에 카운터 문제를 본다

다음 요구가 있다.

```text
함수를 호출할 때마다 1씩 증가하는 카운터를 만들고 싶다.
하지만 count 변수를 프로그램 아무 곳에서나 직접 바꾸게 하고 싶지는 않다.
```

코드:

```javascript
function makeCounter() {
  let count = 0;

  return function () {
    count = count + 1;
    return count;
  };
}
```

사용:

```javascript
const counter = makeCounter();

console.log(counter()); // 1
console.log(counter()); // 2
console.log(counter()); // 3
```

#### 7. makeCounter가 끝났는데 count가 왜 살아 있을까

`makeCounter()` 호출은 이미 끝났다.

그런데 반환된 함수는 계속 `count`를 사용한다.

반환된 함수가 만들어질 당시 접근할 수 있던 바깥 변수 환경을 계속 사용할 수 있기 때문이다.

이 현상을 **closure(클로저)**라고 부른다.

그림:

```text
makeCounter 실행
└─ count = 0
   └─ 내부 함수가 count를 사용하는 상태로 만들어짐

makeCounter 종료
↓
내부 함수가 반환됨
↓
내부 함수는 count 환경을 계속 참조
```

#### 8. closure는 이벤트에서도 자연스럽게 나온다

```javascript
let clicks = 0;

button.addEventListener("click", () => {
  clicks = clicks + 1;
  console.log(clicks);
});
```

click handler는 나중에 실행되지만 바깥 `clicks` 변수에 접근한다.

closure는 특별한 마법 함수를 부르는 기능이 아니라 JavaScript의 함수와 lexical scope가 만드는 자연스러운 동작이다.

#### 9. closure를 잘못 사용하면 오래 살아 있는 상태가 생길 수 있다

큰 객체를 closure가 계속 참조하면 그 데이터가 필요 이상 오래 메모리에 남을 수 있다.

또 loop 안에서 callback을 만들 때 어떤 값을 캡처하는지 몰라 예상치 못한 결과가 나올 수 있다.

따라서 closure는 `무조건 좋은 private 기능`으로 외우지 않고 **어떤 바깥 상태를 잡고 있는지** 본다.

#### 10. this는 일반 변수와 다르게 호출 방식이 중요하다

객체:

```javascript
const user = {
  name: "민수",
  show() {
    console.log(this.name);
  }
};

user.show();
```

이 호출에서 this는 user를 가리킨다.

결과:

```text
민수
```

그런데 함수를 떼어낸다.

```javascript
const show = user.show;
show();
```

호출 방식이 달라졌기 때문에 this도 달라질 수 있다.

strict mode나 실행 환경에 따라 undefined 관련 오류가 날 수 있다.

#### 11. arrow function의 this가 중요한 이유

arrow function은 자신만의 this binding을 만들지 않고 바깥 lexical 환경의 this를 사용한다.

그래서 method를 무조건 arrow로 바꾸거나, 반대로 callback에서 무조건 일반 function을 쓰면 this가 달라질 수 있다.

`어떤 문법이 더 최신인가`보다 **어떤 this를 원하는가**가 중요하다.

#### 12. prototype은 객체가 공통 기능을 찾는 연결 구조다

```javascript
const numbers = [1, 2, 3];
numbers.map(...);
```

배열 객체마다 `map` 함수 전체가 새로 복사되어 들어 있는 것이 아니다.

JavaScript 객체는 자신에게 없는 속성을 **prototype chain**을 따라 찾을 수 있다.

초급용 그림:

```text
numbers 배열 객체
↓ 없는 map을 찾음
Array.prototype
↓
map 발견
```

이런 prototype 기반 위에서 class 문법도 동작한다.

#### 13. class는 익숙한 객체 생성 문법을 제공한다

```javascript
class User {
  constructor(name) {
    this.name = name;
  }

  greet() {
    return `안녕 ${this.name}`;
  }
}

const user = new User("민수");
console.log(user.greet());
```

사람말:

```text
User라는 객체 모양을 정의
↓
new User("민수")로 인스턴스 생성
↓
constructor가 초기 name 설정
↓
greet method 사용
```

#### 14. JavaScript class는 prototype 모델 위의 문법이다

Java나 Kotlin의 class와 비슷하게 보이지만 JavaScript 객체 시스템은 prototype 기반이다.

class 문법은 이 모델을 더 익숙하게 사용할 수 있게 해 주는 문법이라고 이해한다.

#### 15. 상속을 무조건 깊게 만들지 않는다

```javascript
class AdminUser extends User {
  ...
}
```

처럼 상속을 만들 수 있다.

하지만 class 계층이 너무 깊어지면 동작을 추적하기 어려워질 수 있다.

실제 설계에서는 composition, 작은 객체 협력 등 다른 방법도 고려한다.

설계 TRACK에서 다시 다룬다.

#### 16. 함수 실행을 손으로 추적한다

```javascript
function outer(x) {
  const y = x + 1;

  function inner(z) {
    return y + z;
  }

  return inner;
}

const fn = outer(10);
console.log(fn(5));
```

순서:

```text
outer(10)
→ x=10
→ y=11
→ inner 함수 생성
→ inner 반환

fn(5)
→ z=5
→ closure로 y=11 사용
→ 16 반환
```

코드가 어렵게 보일 때 변수와 호출 단계를 종이에 적는다.

#### 17. 책을 덮고 확인한다

1. JavaScript 함수가 값이라는 말은 어떤 의미인가?
2. scope와 lexical scope를 구분해 설명하라.
3. closure가 makeCounter의 count를 계속 사용할 수 있는 이유는 무엇인가?
4. `this`를 함수 정의 위치만 보고 판단하면 안 되는 이유는 무엇인가?
5. prototype을 배열의 map 예로 설명하라.
6. JavaScript class와 prototype은 어떤 관계인가?

---

## BLOCK 03 · 배열과 객체를 변환하고 파일 단위로 코드를 나눈다

### LESSON 01 · map·filter·reduce·destructuring·spread/rest·module·Error를 실제 데이터 처리로 연결한다

#### 1. API에서 사용자 목록을 받았다고 하자

```javascript
const users = [
  { id: 1, name: "민수", age: 20, active: true },
  { id: 2, name: "지수", age: 17, active: false },
  { id: 3, name: "현우", age: 31, active: true }
];
```

이 데이터를 화면에 보여 주려면:

```text
이름만 뽑기
활성 사용자만 고르기
평균 나이 계산
```

같은 작업을 한다.

#### 2. map은 각 항목을 다른 값으로 변환한다

```javascript
const names = users.map(user => user.name);
console.log(names);
```

결과:

```text
["민수", "지수", "현우"]
```

흐름:

```text
user 1 → "민수"
user 2 → "지수"
user 3 → "현우"
↓
새 배열
```

원본 배열을 같은 위치에서 바꾸는 것이 아니라 변환 결과로 새 배열을 만든다는 생각이 중요하다.

#### 3. filter는 조건에 맞는 항목만 남긴다

```javascript
const activeUsers = users.filter(user => user.active);
```

결과에는 active가 true인 사용자만 남는다.

```text
민수 → 통과
지수 → 제외
현우 → 통과
```

#### 4. reduce는 여러 값을 하나의 누적 결과로 합친다

전체 나이 합계:

```javascript
const totalAge = users.reduce((sum, user) => {
  return sum + user.age;
}, 0);
```

실행 추적:

```text
초기 sum = 0
+20 → 20
+17 → 37
+31 → 68
```

평균:

```javascript
const averageAge = totalAge / users.length;
```

#### 5. reduce를 모든 곳에 쓰지 않는다

reduce로 map/filter 역할까지 복잡하게 표현할 수 있지만 읽기 어려워질 수 있다.

```text
변환 → map
조건 선별 → filter
여러 값을 하나로 누적 → reduce
```

처럼 의도가 잘 드러나는 도구를 고른다.

#### 6. destructuring으로 필요한 값을 꺼낸다

객체:

```javascript
const user = { id: 1, name: "민수", age: 20 };
```

기존:

```javascript
const name = user.name;
const age = user.age;
```

구조 분해:

```javascript
const { name, age } = user;
```

배열:

```javascript
const [first, second] = [10, 20];
```

#### 7. destructuring에서 기본값과 이름 변경도 가능하다

```javascript
const { nickname = "없음" } = user;
```

속성이 없으면 기본값을 사용할 수 있다.

```javascript
const { name: userName } = user;
```

name 속성을 userName 변수로 받을 수도 있다.

#### 8. spread로 배열을 펼쳐 새 배열을 만든다

```javascript
const oldItems = [1, 2];
const newItems = [...oldItems, 3];
```

결과:

```text
[1, 2, 3]
```

객체:

```javascript
const updatedUser = {
  ...user,
  age: 21
};
```

기존 user 속성을 펼친 뒤 age를 새 값으로 덮는다.

#### 9. spread는 deep clone이 아니다

앞 LESSON에서 본 것처럼 중첩 객체 reference는 공유될 수 있다.

```javascript
const a = {
  profile: { city: "서울" }
};

const b = { ...a };
b.profile.city = "부산";
```

원본 a도 영향을 받을 수 있다.

#### 10. rest는 남은 값을 모은다

함수:

```javascript
function sum(...numbers) {
  return numbers.reduce((total, n) => total + n, 0);
}
```

호출:

```javascript
sum(1, 2, 3, 4);
```

`numbers`는 `[1,2,3,4]`가 된다.

#### 11. 코드가 커지면 module로 나눈다

`math.js`:

```javascript
export function add(a, b) {
  return a + b;
}
```

`app.js`:

```javascript
import { add } from "./math.js";

console.log(add(2, 3));
```

module은 관련 코드를 독립 단위로 나누고 필요한 기능을 명시적으로 내보내고 가져오게 한다.

#### 12. module dependency를 그림으로 본다

```text
app.js
├─ users.js
└─ api.js
   └─ http.js
```

app이 users와 api에 의존하고 api가 http에 의존한다.

의존 관계가 지나치게 복잡하거나 서로 순환하면 수정하기 어려워질 수 있다.

#### 13. 오류도 객체로 표현할 수 있다

```javascript
throw new Error("나이는 음수일 수 없습니다");
```

`Error` 객체에는 message와 stack 같은 오류 정보가 들어갈 수 있다.

`throw`는 정상 실행 흐름을 중단하고 오류를 위쪽으로 전달한다.

#### 14. try/catch로 예외를 처리한다

```javascript
try {
  riskyWork();
} catch (error) {
  console.error(error);
}
```

성공:

```text
try 코드 계속 실행
```

실패:

```text
throw/error
↓
catch로 이동
```

#### 15. finally는 성공/실패와 관계없이 정리 작업을 할 수 있다

```javascript
try {
  openResource();
} finally {
  closeResource();
}
```

실제로 자원 정리 방식은 API마다 다르지만 `성공해도 실패해도 실행해야 하는 정리`라는 생각을 이해한다.

#### 16. catch에서 오류를 숨기지 않는다

```javascript
try {
  await save();
} catch (error) {
  // 아무것도 안 함
}
```

이러면 저장이 실패해도 호출한 쪽은 성공처럼 진행할 수 있다.

오류를:

```text
여기서 사용자에게 처리할 것인가?
로그만 남기고 다시 throw할 것인가?
기본값으로 복구 가능한가?
```

의도적으로 결정한다.

#### 17. 실제 데이터 파이프라인으로 연결한다

```javascript
const activeNames = users
  .filter(user => user.active)
  .map(user => user.name);
```

사람말:

```text
사용자 중 active만 남긴다.
↓
남은 사용자에서 name만 뽑는다.
```

코드 체인을 읽을 때도 왼쪽→오른쪽이 아니라 **데이터가 어떤 단계로 변하는지** 설명한다.

#### 18. 책을 덮고 확인한다

1. map/filter/reduce는 각각 어떤 질문에 답하는가?
2. destructuring은 어떤 코드를 줄여 주는가?
3. spread로 만든 객체가 deep clone이 아닌 이유는 무엇인가?
4. module의 export/import는 무엇을 표현하는가?
5. throw가 실행 흐름을 어떻게 바꾸는가?
6. catch에서 오류를 무조건 무시하면 왜 위험한가?

---

## BLOCK 04 · 시간이 걸리는 일을 기다리는 방법부터 비동기를 시작한다

### LESSON 01 · blocking·동기·비동기·callback을 사용자가 기다리는 상황으로 이해한다

#### 1. 비동기를 영어 단어부터 배우면 어려워진다

먼저 상황을 본다.

사용자가 앱에서 사진 100장을 업로드한다.

업로드가 20초 걸린다고 하자.

그 20초 동안 앱이:

```text
스크롤 안 됨
버튼 안 눌림
화면 갱신 안 됨
취소 안 됨
```

이라면 매우 불편하다.

시간이 걸리는 작업을 기다리는 동안 다른 일을 처리할 구조가 필요하다.

#### 2. 동기 흐름은 앞 작업이 끝난 뒤 다음으로 진행한다

가장 단순한 실행:

```javascript
console.log("A");
console.log("B");
console.log("C");
```

결과:

```text
A
B
C
```

앞 줄이 끝나고 다음 줄로 간다.

이런 순차적인 흐름을 **synchronous(동기)**라고 부른다.

동기가 나쁜 것은 아니다.

짧은 계산과 순서가 필요한 코드에서는 매우 이해하기 쉽다.

#### 3. blocking은 다음 진행을 막고 기다리는 상태다

아주 긴 계산을 JavaScript 메인 실행 흐름에서 한다고 하자.

```javascript
while (true) {
  // 끝나지 않음
}
```

이 코드가 메인 thread를 계속 차지하면 UI event 처리와 화면 업데이트가 지연될 수 있다.

이런 식으로 어떤 작업이 실행 흐름을 붙잡아 다음 진행을 막는 것을 **blocking**이라고 부른다.

#### 4. 모든 기다림이 같은 종류는 아니다

```text
CPU가 실제 계산을 오래 함
네트워크 응답을 기다림
타이머 시간을 기다림
사용자 클릭을 기다림
파일 읽기 완료를 기다림
```

CPU 계산과 외부 I/O 대기는 성격이 다르다.

비동기 API를 쓴다고 CPU가 많이 필요한 계산이 자동으로 빨라지는 것은 아니다.

#### 5. setTimeout으로 "나중에 실행"을 처음 본다

```javascript
console.log("1. 시작");

setTimeout(() => {
  console.log("3. 타이머 완료");
}, 1000);

console.log("2. 다른 코드");
```

실행 전 예상한다.

코드 가운데에 timer가 있으니 1→3→2라고 생각할 수 있다.

일반적인 결과는:

```text
1. 시작
2. 다른 코드
3. 타이머 완료
```

이다.

#### 6. 왜 타이머를 기다리며 그 자리에서 멈추지 않을까

`setTimeout`은 실행 환경에:

```text
약 1초가 지난 뒤 이 함수를 실행할 수 있게 준비해 줘.
```

라고 요청한다.

현재 JavaScript 코드는 다음 줄로 계속 진행한다.

타이머 시간이 지난 뒤 callback은 **실행 준비 상태**가 된다.

정확히 언제 실행되는지는 뒤의 event loop에서 배운다.

#### 7. callback은 나중에 실행하도록 넘겨 둔 함수다

```javascript
setTimeout(() => {
  console.log("완료");
}, 1000);
```

여기서:

```javascript
() => {
  console.log("완료");
}
```

가 callback이다.

한 줄 뜻:

> callback = 어떤 작업이나 사건이 끝났을 때 나중에 실행할 수 있도록 다른 코드에 전달한 함수

#### 8. click event handler도 callback이다

```javascript
button.addEventListener("click", () => {
  console.log("클릭됨");
});
```

우리는 함수를 지금 바로 실행하지 않는다.

브라우저에게 넘겨 둔다.

```text
나중에 click이 발생하면 이 함수를 실행해 줘.
```

이것도 callback 구조다.

#### 9. 네트워크도 기다림이 있다

서버 요청:

```text
요청 전송
↓
인터넷 이동
↓
서버 처리
↓
응답 도착
```

몇 ms에서 몇 초 이상 걸릴 수 있다.

브라우저가 이 시간 동안 JavaScript 전체를 멈춰 둘 필요는 없다.

네트워크 작업은 브라우저의 별도 기능이 처리하고, 완료 뒤 JavaScript가 이어서 결과를 처리하도록 만들 수 있다.

이런 `완료를 그 자리에서 붙잡아 기다리지 않고 나중에 이어 처리하는 구성`을 **asynchronous(비동기)**라고 부른다.

#### 10. 비동기와 병렬은 같은 말이 아니다

```text
비동기
→ 기다리는 작업의 완료를 나중에 이어 처리하는 실행 구조

병렬
→ 여러 계산을 실제 같은 시간에 여러 실행 자원에서 수행
```

비동기 프로그램이 내부적으로 여러 thread/프로세스/장치를 활용할 수는 있지만 개념 자체는 다르다.

`async를 붙이면 CPU 두 개에서 동시에 계산한다`고 생각하면 안 된다.

#### 11. callback이 여러 단계로 이어지면 코드가 깊어질 수 있다

```javascript
loadUser(user => {
  loadOrders(user.id, orders => {
    loadDetails(orders, details => {
      console.log(details);
    });
  });
});
```

오른쪽으로 계속 들어간다.

실패 처리까지 추가하면 더 복잡해진다.

이 문제를 다루기 위해 Promise라는 구조가 등장한다.

#### 12. 비동기 코드를 읽을 때 "코드 위치"와 "실행 시간"을 구분한다

```javascript
console.log("A");

setTimeout(() => {
  console.log("B");
}, 0);

console.log("C");
```

B 코드가 C보다 위에 적혀 있어도 실제 B callback은 나중에 실행된다.

따라서 비동기에서는:

```text
코드 파일에서 위에 있다
=
무조건 먼저 실행된다
```

가 아니다.

#### 13. 실제 검색 앱 흐름으로 본다

```text
사용자 검색 클릭
↓
loading=true
↓
서버 요청 시작
↓
기다리는 동안 화면은 로딩 표시 가능
↓
응답 완료
↓
result state 변경
↓
loading=false
↓
결과 화면 표시
```

이것이 비동기 UI의 기본적인 흐름이다.

#### 14. 책을 덮고 확인한다

1. synchronous와 blocking은 완전히 같은 말인가? 각각 어떤 관점인가?
2. setTimeout callback이 코드 가운데 있다고 즉시 실행되지 않는 이유는 무엇인가?
3. callback을 자기 말로 설명하라.
4. 비동기와 병렬의 차이는 무엇인가?
5. 여러 callback을 깊게 중첩하면 어떤 문제가 생길 수 있는가?
6. 비동기 코드를 읽을 때 코드 위치와 실행 시간을 왜 구분해야 하는가?

---

## BLOCK 05 · Promise와 async/await는 미래의 성공과 실패를 다루는 방법이다

### LESSON 01 · Promise 상태부터 then/catch·chain·async·await·오류 전파까지 한 단계씩 배운다

#### 1. callback 대신 "미래 결과" 자체를 값처럼 다루면 어떨까

서버 요청은 지금 바로 결과가 없다.

```text
지금
→ 요청만 시작됨

나중
→ 성공 데이터 또는 실패 이유가 생김
```

JavaScript의 **Promise**는 이런 `미래에 완료될 비동기 작업의 결과`를 표현하는 객체다.

#### 2. Promise에는 세 상태가 있다

```text
pending
→ 아직 결과가 정해지지 않음

fulfilled
→ 성공 결과가 준비됨

rejected
→ 실패 이유가 준비됨
```

Promise가 fulfilled 또는 rejected로 확정되면 다시 pending으로 돌아가거나 반대 결과로 바뀌지 않는다.

#### 3. 직접 Promise를 만들어 상태를 본다

```javascript
const promise = new Promise((resolve, reject) => {
  setTimeout(() => {
    resolve("성공");
  }, 1000);
});
```

흐름:

```text
Promise 생성
↓ pending
1초 대기
↓
resolve("성공")
↓ fulfilled
```

실패:

```javascript
reject(new Error("실패"));
```

하면 rejected 상태가 된다.

실전에서는 fetch 같은 API가 Promise를 만들어 주므로 매번 `new Promise`를 직접 쓸 필요는 없다.

#### 4. then으로 성공 결과를 이어서 처리한다

```javascript
getData().then(data => {
  console.log(data);
});
```

`getData()`가 Promise를 반환한다고 하자.

Promise가 성공하면 `then`에 넘긴 함수가 결과 data를 받는다.

#### 5. catch로 실패 경로를 처리한다

```javascript
getData()
  .then(data => {
    console.log(data);
  })
  .catch(error => {
    console.error(error);
  });
```

성공과 실패 흐름을 코드에서 분리해 볼 수 있다.

#### 6. then에서 반환한 값은 다음 then으로 간다

```javascript
getUser()
  .then(user => {
    return user.id;
  })
  .then(id => {
    console.log(id);
  });
```

첫 then의 return 값이 다음 then의 입력이 된다.

#### 7. then에서 Promise를 반환하면 그 완료를 기다린다

```javascript
getUser()
  .then(user => {
    return getOrders(user.id);
  })
  .then(orders => {
    console.log(orders);
  });
```

`getOrders`가 Promise를 반환하면 다음 then은 그 Promise가 fulfilled될 때까지 이어지지 않는다.

이렇게 Promise를 연결하는 것을 **Promise chaining**이라고 부른다.

#### 8. chain 중간에서 throw하면 catch로 이어질 수 있다

```javascript
getUser()
  .then(user => {
    if (!user.active) {
      throw new Error("비활성 사용자");
    }
    return getOrders(user.id);
  })
  .catch(error => {
    console.error(error);
  });
```

동기 코드에서 throw한 오류도 Promise chain의 실패 경로로 연결될 수 있다.

#### 9. async 함수는 Promise를 반환한다

```javascript
async function load() {
  return 10;
}
```

겉으로는 10을 return하지만 호출 결과는 Promise다.

```javascript
const result = load();
```

result는 `10` 자체가 아니라 10으로 fulfilled되는 Promise다.

#### 10. await는 Promise 결과가 준비될 때까지 현재 async 함수의 다음 부분을 잠시 멈춘다

```javascript
async function load() {
  const data = await getData();
  console.log(data);
}
```

사람말:

```text
getData 시작
↓
Promise 결과가 올 때까지 load 함수의 이 다음 부분은 대기
↓
결과가 오면 data에 받음
↓
console.log 실행
```

#### 11. await 때문에 전체 JavaScript 프로그램이 정지하는 것은 아니다

중요하다.

`await`는 해당 async 함수의 이어지는 부분을 기다리게 한다.

그동안 실행 환경은 준비된 다른 event와 작업을 처리할 수 있다.

```text
load 함수 기다림
≠
브라우저 전체 정지
```

#### 12. Promise chain을 async/await로 다시 쓴다

Promise:

```javascript
getUser()
  .then(user => getOrders(user.id))
  .then(orders => render(orders))
  .catch(error => showError(error));
```

async/await:

```javascript
async function load() {
  try {
    const user = await getUser();
    const orders = await getOrders(user.id);
    render(orders);
  } catch (error) {
    showError(error);
  }
}
```

동일한 비동기 관계를 더 동기 코드처럼 읽을 수 있다.

#### 13. await한 Promise가 reject되면 예외처럼 다룬다

```javascript
try {
  const data = await getData();
} catch (error) {
  console.error(error);
}
```

getData Promise가 rejected되면 await 지점에서 오류가 발생한 것처럼 catch로 이동한다.

#### 14. catch에서 로그만 찍고 실패를 성공처럼 바꿀 수 있다

```javascript
async function load() {
  try {
    return await getData();
  } catch (error) {
    console.error(error);
  }
}
```

실패하면 이 함수가 `undefined`로 fulfilled되는 Promise를 반환할 수 있다.

호출한 쪽은 실패가 사라졌다고 오해할 수 있다.

필요하다면 다시 throw한다.

```javascript
catch (error) {
  console.error(error);
  throw error;
}
```

오류를 어디에서 책임질지 설계해야 한다.

#### 15. 순차 await는 작업을 하나씩 시작한다

```javascript
const a = await getA();
const b = await getB();
```

흐름:

```text
A 시작
↓ A 완료
B 시작
↓ B 완료
```

B가 A 결과를 필요로 한다면 맞다.

하지만 서로 완전히 독립적이라면 불필요하게 오래 걸릴 수 있다.

#### 16. Promise 코드를 읽는 체크리스트

```text
이 함수는 Promise를 반환하는가?
성공 값은 무엇인가?
실패는 어떤 방식으로 전달되는가?
await 때문에 어떤 코드가 기다리는가?
서로 의존하지 않는 작업을 순서대로 기다리고 있지 않은가?
catch가 오류를 숨기고 있지 않은가?
```

#### 17. 책을 덮고 확인한다

1. Promise의 세 상태를 설명하라.
2. then에서 일반 값을 return하면 다음 then에 어떻게 전달되는가?
3. Promise를 return하면 다음 then은 어떻게 되는가?
4. async 함수는 무엇을 반환하는가?
5. await가 브라우저 전체를 멈추는 것이 아닌 이유를 설명하라.
6. catch가 오류를 숨겨 버리는 사례를 설명하라.

---

## BLOCK 06 · "나중"은 정확히 언제 실행되는가

### LESSON 01 · call stack·task·event loop·microtask를 출력 순서로 직접 추적한다

#### 1. event loop를 정의부터 외우지 않는다

다음 코드의 출력 순서를 먼저 맞혀 본다.

```javascript
console.log("A");

setTimeout(() => {
  console.log("B");
}, 0);

console.log("C");
```

예상 결과를 적는다.

일반적인 결과:

```text
A
C
B
```

`0ms`인데도 B가 C보다 뒤다.

왜인지 설명하려면 실행 중인 함수와 대기 중인 작업을 구분해야 한다.

#### 2. call stack은 현재 실행 중인 함수 호출을 쌓아 관리한다

```javascript
function a() {
  b();
}

function b() {
  c();
}

function c() {
  console.log("끝");
}

a();
```

호출 흐름:

```text
a 시작
↓
b 호출
↓
c 호출
↓
console.log
↓
c 종료
↓
b 종료
↓
a 종료
```

현재 실행해야 할 함수 호출이 stack 구조로 관리된다.

이를 **call stack**이라고 부른다.

#### 3. stack은 마지막에 들어온 호출이 먼저 끝난다

TRACK 03의 stack을 떠올린다.

```text
a
└─ b
   └─ c
```

c가 먼저 끝나고 b, a 순으로 돌아온다.

자료구조가 실제 실행 모델에 연결된다.

#### 4. 타이머는 JavaScript 엔진이 그 자리에서 시간을 재며 멈추는 것이 아니다

브라우저에는 timer, network 같은 웹 API 기능이 있다.

`setTimeout`을 호출하면 브라우저 환경이 타이머를 관리한다.

시간이 지나면 callback은 **실행할 준비가 된 task**가 된다.

하지만 현재 call stack에 실행 중인 JavaScript가 있다면 그 중간에 갑자기 끼어들지 않는다.

#### 5. task queue는 실행 준비가 된 일반 task가 기다리는 곳으로 생각한다

타이머 callback 같은 task가 준비돼도 JavaScript가 바쁘면 기다린다.

```text
현재 call stack
[긴 함수 실행 중]

대기
[타이머 callback]
```

현재 실행이 끝나야 다음 task를 가져올 수 있다.

실제 브라우저에는 여러 task source가 있고 단순히 queue 하나만 있다고 이해하면 세부적으로 틀릴 수 있지만 초급에서는 이 모델로 큰 흐름을 익힌다.

#### 6. event loop는 다음 실행 기회를 조정한다

**event loop**는 현재 JavaScript 실행과 준비된 대기 작업을 계속 확인해 다음 JavaScript를 실행할 수 있게 조정하는 구조다.

아주 쉽게:

```text
지금 실행 중인 코드가 끝났나?
↓
끝났다면 준비된 다음 일을 실행할 수 있게 넘긴다.
```

네트워크 요청 자체를 event loop가 직접 다운로드하는 것은 아니다.

브라우저의 네트워크 기능이 작업하고, 완료 이후 JavaScript 실행이 다시 연결되는 과정에서 event loop가 중요하다.

#### 7. 이제 첫 출력 순서를 다시 설명한다

```javascript
console.log("A");
setTimeout(() => console.log("B"), 0);
console.log("C");
```

순서:

```text
1. console.log("A") 실행 → A
2. setTimeout 등록 → callback은 나중 task
3. console.log("C") 실행 → C
4. 현재 script 실행 종료
5. 준비된 timer task 실행 → B
```

그래서 A C B다.

#### 8. Promise가 나오면 microtask가 등장한다

다음 코드를 본다.

```javascript
console.log("A");

setTimeout(() => console.log("B"), 0);

Promise.resolve().then(() => console.log("C"));

console.log("D");
```

예상한다.

일반적인 결과:

```text
A
D
C
B
```

#### 9. Promise 후속 작업은 microtask로 예약된다

Promise `.then()` callback이나 `await` 뒤의 이어지는 작업은 **microtask**와 연결된다.

현재 실행 중인 JavaScript가 끝난 뒤, 다음 일반 task로 넘어가기 전에 microtask들을 먼저 처리한다.

초급 실행 모델:

```text
현재 call stack 실행
↓
microtask 처리
↓
다음 task 처리
```

따라서 Promise callback C가 timer task B보다 먼저 실행될 수 있다.

#### 10. 전체 출력 순서를 손으로 그린다

```text
동기 script
A 출력
setTimeout 등록
Promise.then 등록
D 출력
↓ script 종료

microtask
C 출력
↓

timer task
B 출력
```

결과:

```text
A D C B
```

#### 11. `await`도 중간에서 함수를 두 조각처럼 볼 수 있다

```javascript
async function run() {
  console.log("1");
  await Promise.resolve();
  console.log("2");
}

console.log("A");
run();
console.log("B");
```

흐름:

```text
A 출력
run 시작
1 출력
await에서 run의 나머지 부분은 나중 microtask로 이어짐
run 호출이 일단 제어 반환
B 출력
현재 script 종료
microtask에서 run 이어짐
2 출력
```

일반적인 결과:

```text
A
1
B
2
```

#### 12. microtask를 무한히 계속 추가하면 화면이 늦어질 수도 있다

microtask는 다음 task보다 우선 처리되지만, `우선이면 무조건 좋은 것`이 아니다.

microtask가 계속 새 microtask를 만들어 queue가 비지 않으면 브라우저가 rendering이나 다른 task로 넘어가는 시간이 지연될 수 있다.

실행 모델은 성능에도 영향을 준다.

#### 13. event loop 문제를 풀 때 표를 만든다

코드:

```javascript
console.log("start");
setTimeout(() => console.log("timer"), 0);
Promise.resolve().then(() => console.log("promise"));
console.log("end");
```

표:

```text
현재 stack에서 즉시 실행
start
end

microtask
promise

일반 task
timer
```

이 표를 만들면 암기보다 안정적으로 문제를 풀 수 있다.

#### 14. 브라우저와 Node.js의 event loop 세부는 완전히 같다고 단정하지 않는다

Node.js에는 phases와 별도 queue 규칙 등이 있고 브라우저와 세부 실행 모델이 다를 수 있다.

초급에서 공통 큰 개념을 먼저 배우고, 특정 환경의 정확한 순서가 중요할 때 해당 runtime 문서를 확인한다.

#### 15. 책을 덮고 확인한다

1. call stack은 무엇을 관리하는가?
2. setTimeout 0이 즉시 callback을 실행한다는 뜻이 아닌 이유는 무엇인가?
3. task와 microtask의 큰 차이를 설명하라.
4. Promise.then이 timer보다 먼저 실행될 수 있는 이유는 무엇인가?
5. await 앞과 뒤 코드를 실행 시간 관점으로 나누어 설명하라.
6. event loop가 네트워크 다운로드 자체를 직접 한다고 보면 왜 틀린가?

---

## BLOCK 07 · 여러 비동기 작업이 동시에 존재할 때 생기는 문제를 다룬다

### LESSON 01 · Promise.all·allSettled·timeout·cancellation·race condition·debounce·throttle을 검색 앱으로 이해한다

#### 1. 서로 독립적인 요청 두 개를 순서대로 기다리고 있다

```javascript
const user = await getUser();
const notices = await getNotices();
```

두 요청이 서로 관계없고 각각 2초 걸린다고 하자.

순차 실행이라면 약 4초 가까이 걸릴 수 있다.

```text
getUser 시작
2초
완료
getNotices 시작
2초
완료
```

둘을 먼저 시작할 수 있다.

#### 2. Promise.all은 여러 Promise를 함께 기다린다

```javascript
const [user, notices] = await Promise.all([
  getUser(),
  getNotices()
]);
```

호출 표현식을 만들 때 두 작업이 시작되고, 둘 다 성공할 때까지 결과를 기다린다.

```text
getUser    ─────── 완료
getNotices ───── 완료
            ↓ 둘 다 완료 후 계속
```

#### 3. Promise.all은 하나가 reject되면 전체도 reject된다

```javascript
await Promise.all([
  getA(),
  getB()
]);
```

B가 실패하면 Promise.all 결과도 rejected된다.

`A가 실제로 취소된다`는 뜻은 아니다. 이미 시작된 다른 작업은 계속 진행될 수 있다.

#### 4. 일부 실패도 모두 결과로 보고 싶다면 allSettled를 고려한다

```javascript
const results = await Promise.allSettled([
  getA(),
  getB(),
  getC()
]);
```

각 결과에:

```text
fulfilled + value
또는
rejected + reason
```

같은 상태가 들어간다.

대시보드 여러 카드처럼 일부 데이터 실패도 나머지를 보여 줘야 하는 상황에서 유용할 수 있다.

#### 5. timeout은 무한히 기다리지 않게 하는 정책이다

서버가 아무 응답도 주지 않으면 사용자가 영원히 로딩을 볼 수 없다.

`정해진 시간 안에 끝나지 않으면 실패로 처리`하는 제한을 **timeout**이라고 부른다.

하지만:

```text
timeout 0.1초
```

처럼 너무 짧으면 정상 요청도 실패한다.

```text
timeout 5분
```

처럼 너무 길면 장애 상황에서 사용자가 너무 오래 기다릴 수 있다.

실제 응답 시간과 사용자 경험을 보고 정한다.

#### 6. fetch timeout을 AbortController와 연결할 수 있다

```javascript
async function fetchWithTimeout(url, ms) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), ms);

  try {
    return await fetch(url, { signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}
```

한 단계씩:

```text
AbortController 생성
↓
ms 후 abort하도록 timer 설정
↓
fetch에 signal 전달
↓
먼저 완료하면 response 반환
↓
finally에서 timer 정리
```

#### 7. cancellation은 더 이상 필요 없는 작업을 중단하거나 결과를 무시하는 것이다

검색창에서 사용자가:

```text
삼
↓
삼성
↓
삼성전자
```

를 빠르게 입력한다.

세 요청을 전부 끝까지 처리할 필요가 없을 수 있다.

이전 요청을 취소하거나, 취소가 불가능하면 오래된 결과를 화면에 반영하지 않는 정책이 필요하다.

#### 8. race condition은 완료 순서에 따라 결과가 달라지는 문제다

검색 A:

```text
"삼" 요청
3초 걸림
```

검색 B:

```text
"삼성" 요청
0.5초 걸림
```

시간:

```text
A 시작 ───────────────── 완료
    B 시작 ── 완료
```

B 결과가 먼저 화면에 나온다.

그 뒤 오래된 A가 도착해 화면을 덮으면 사용자가 마지막으로 입력한 `삼성`이 아니라 `삼` 결과를 보게 된다.

여러 작업의 실행·완료 순서에 따라 결과가 달라지는 문제를 **race condition**이라고 부른다.

#### 9. 해결 1 — 이전 요청 취소

```javascript
let controller = null;

async function search(query) {
  controller?.abort();
  controller = new AbortController();

  const response = await fetch(`/search?q=${encodeURIComponent(query)}`, {
    signal: controller.signal
  });

  return response.json();
}
```

새 요청 전에 이전 controller를 abort한다.

#### 10. 해결 2 — 요청 번호를 확인한다

모든 API가 실제 취소를 지원하지 않을 수도 있다.

```javascript
let latestRequestId = 0;

async function search(query) {
  const requestId = ++latestRequestId;
  const result = await requestSearch(query);

  if (requestId !== latestRequestId) {
    return;
  }

  render(result);
}
```

마지막 요청만 화면에 반영한다.

#### 11. debounce는 입력이 잠잠해진 뒤 실행한다

사용자가 타이핑할 때 매 글자마다 서버 요청을 보내지 않고 잠시 멈춘 뒤 검색한다.

```text
ㅅ       timer 시작
사      이전 timer 취소, 새 timer
삼      이전 timer 취소, 새 timer
삼성    이전 timer 취소, 새 timer
300ms 입력 없음
→ 검색 1회
```

이 패턴이 **debounce**다.

#### 12. 가장 작은 debounce를 만들어 본다

```javascript
function debounce(fn, delay) {
  let timerId;

  return (...args) => {
    clearTimeout(timerId);
    timerId = setTimeout(() => {
      fn(...args);
    }, delay);
  };
}
```

핵심:

```text
새 호출이 오면 이전 timer 취소
↓
새 timer 생성
↓
일정 시간 추가 호출이 없으면 실제 함수 실행
```

closure도 사용되고 있다. `timerId`가 반환된 함수에 의해 유지된다.

#### 13. throttle은 일정 시간에 실행 횟수를 제한한다

scroll event는 매우 자주 발생한다.

매 event마다 무거운 계산을 하면 느려질 수 있다.

**throttle**은 일정 간격 동안 최대 실행 횟수를 제한하는 방식이다.

```text
debounce
→ 마지막 입력이 잠잠해진 뒤 실행

throttle
→ 계속 이벤트가 와도 정해진 간격으로 제한해 실행
```

검색 입력에는 debounce, 스크롤 위치 추적에는 throttle이 자연스러운 경우가 많지만 실제 요구에 따라 다르다.

#### 14. loading/error/success state를 분리한다

비동기 UI에서는 최소한 다음 상태를 생각한다.

```text
idle
loading
success
error
```

`result === null` 하나로 모든 상태를 표현하려 하면:

```text
아직 검색 안 함
검색 중
검색 실패
결과가 실제로 없음
```

을 구분하기 어려울 수 있다.

#### 15. 검색 앱의 전체 시간 순서를 그린다

```text
사용자 입력
↓ debounce 300ms
최신 검색 시작
↓ 이전 요청 취소
loading=true
↓
응답 대기
├─ 실패 → error state
└─ 성공 → 최신 request인지 확인
             ↓
           result state
↓
loading=false
↓
render
```

이 그림을 설명할 수 있으면 race condition 코드도 훨씬 이해하기 쉽다.

#### 16. 실패 상황을 일부러 만든다

테스트:

```text
A 요청 3초 지연
B 요청 0.5초 지연
A 뒤에 B 시작
```

취소/요청번호 검사를 제거해 오래된 A가 B를 덮는 것을 재현한다.

그 다음 한 방법씩 다시 넣어 고친다.

실제 실패를 보고 고치는 경험이 중요하다.

#### 17. 책을 덮고 확인한다

1. 순차 await와 Promise.all의 시작 시점 차이를 설명하라.
2. Promise.all에서 하나가 실패하면 어떤 일이 생기는가?
3. allSettled는 언제 유용한가?
4. timeout과 cancellation은 무엇이 다른가?
5. 검색 A/B 예로 race condition을 설명하라.
6. debounce와 throttle의 차이를 말하라.
7. 비동기 UI에서 loading/error/success를 따로 두는 이유는 무엇인가?

---

## BLOCK 08 · TypeScript는 JavaScript의 모든 버그를 없애는 마법이 아니다

### LESSON 01 · type·interface·union·narrowing·unknown·never·generic·runtime validation을 외부 API 데이터로 배운다

#### 1. JavaScript에서 다음 오류는 실행 전 알아낼 수 있을까

```javascript
function add(a, b) {
  return a + b;
}

add(10, "20");
```

JavaScript는 실행하면서:

```text
1020
```

같은 결과를 만들 수 있다.

개발자는 두 parameter가 숫자라고 의도했을 수 있다.

코드를 실행하기 전에 이런 타입 불일치를 더 많이 잡고 싶다.

#### 2. TypeScript는 JavaScript에 정적 타입 검사를 추가한다

```typescript
function add(a: number, b: number): number {
  return a + b;
}
```

이제:

```typescript
add(10, "20");
```

는 TypeScript 검사 단계에서 오류로 잡힐 수 있다.

**TypeScript**는 JavaScript에 정적 타입 시스템을 추가한 언어다.

일반적으로 TypeScript 코드는 JavaScript로 변환되어 실제 JavaScript runtime에서 실행된다.

#### 3. type annotation은 개발자가 타입을 직접 적는 것이다

```typescript
let name: string = "민수";
let age: number = 20;
let active: boolean = true;
```

`: string`, `: number` 등이 **type annotation**이다.

#### 4. TypeScript는 모든 타입을 직접 적지 않아도 추론할 수 있다

```typescript
let age = 20;
```

초기값을 보고 age를 number로 추론할 수 있다.

이것을 **type inference(타입 추론)**라고 부른다.

불필요하게 모든 변수에 타입을 반복해서 적기보다 추론이 잘 되는 곳은 맡길 수 있다.

#### 5. 객체 모양을 type으로 정의한다

```typescript
type User = {
  id: number;
  name: string;
  age: number;
};
```

사용:

```typescript
const user: User = {
  id: 1,
  name: "민수",
  age: 20
};
```

필수 속성이 빠지거나 타입이 다르면 검사 단계에서 알려 줄 수 있다.

#### 6. optional property는 없을 수도 있음을 표현한다

```typescript
type User = {
  id: number;
  nickname?: string;
};
```

`nickname?`은 속성이 없을 수도 있다는 뜻이다.

사용할 때 그 가능성을 생각해야 한다.

#### 7. interface도 객체 계약을 표현한다

```typescript
interface User {
  id: number;
  name: string;
}
```

`type`과 `interface`는 객체 구조 표현에서 기능이 많이 겹친다.

세부 차이를 처음부터 전부 외우지 않는다.

프로젝트 규칙에 맞춰 일관되게 사용하고, union 조합 등 필요한 기능이 생길 때 차이를 배운다.

#### 8. union type은 여러 타입 중 하나일 수 있음을 표현한다

```typescript
let id: string | number;
```

id는 string 또는 number다.

그런데 바로:

```typescript
id.toUpperCase();
```

라고 할 수 없다.

number에는 toUpperCase가 없기 때문이다.

#### 9. narrowing으로 현재 가능한 타입을 좁힌다

```typescript
if (typeof id === "string") {
  console.log(id.toUpperCase());
}
```

조건 안에서 TypeScript는 id가 string이라는 것을 알 수 있다.

이렇게 가능한 타입 범위를 좁혀 안전하게 사용하는 과정을 **narrowing**이라고 부른다.

#### 10. 상태를 discriminated union으로 표현할 수 있다

비동기 상태:

```typescript
type LoadState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; data: User[] }
  | { status: "error"; message: string };
```

이제 status가 success일 때만 data가 존재한다.

```typescript
function render(state: LoadState) {
  if (state.status === "success") {
    console.log(state.data);
  }
}
```

상태별로 필요한 데이터가 다르다는 사실을 타입에 담았다.

#### 11. any는 타입 검사를 거의 꺼 버린다

```typescript
let value: any;
value.notExisting().whatever();
```

컴파일 단계에서 많은 오류를 놓칠 수 있다.

급하다고 외부 API 응답을 전부 any로 두면 TypeScript의 장점을 잃는다.

#### 12. unknown은 "모르지만 확인하고 써라"에 가깝다

```typescript
let value: unknown;
```

바로:

```typescript
value.toUpperCase();
```

할 수 없다.

먼저 확인한다.

```typescript
if (typeof value === "string") {
  console.log(value.toUpperCase());
}
```

외부에서 타입을 확신할 수 없는 데이터의 경계에서 `unknown`은 유용한 선택이 될 수 있다.

#### 13. never는 정상적인 값이 존재할 수 없는 경우를 표현한다

```typescript
function fail(message: string): never {
  throw new Error(message);
}
```

또 union의 모든 경우를 처리했는지 확인하는 패턴에 사용할 수 있다.

```typescript
function assertNever(value: never): never {
  throw new Error("Unexpected value");
}
```

상태 하나를 새로 추가했는데 switch 처리를 빼먹는 오류를 잡는 데 활용할 수 있다.

#### 14. generic은 타입도 parameter처럼 받는다

```typescript
function first<T>(items: T[]): T | undefined {
  return items[0];
}
```

사용:

```typescript
first([1, 2, 3]);       // number | undefined
first(["a", "b"]);     // string | undefined
```

같은 함수 로직을 여러 타입에 재사용하면서 실제 타입 정보를 유지한다.

`T`를 무슨 특별한 예약어로 외우기보다:

```text
나중에 실제 타입이 들어올 자리
```

라고 이해한다.

#### 15. 가장 중요한 함정 — TypeScript 타입은 실제 서버 JSON을 자동 검증하지 않는다

코드:

```typescript
type User = {
  id: number;
  name: string;
};

const response = await fetch("/api/user");
const data = await response.json() as User;
```

`as User`라고 썼다고 서버가 실제로:

```json
{
  "id": "잘못된문자",
  "name": null
}
```

을 보내는 것을 막아 주지 않는다.

TypeScript 타입은 컴파일/개발 단계의 정보다. 네트워크에서 실제로 들어온 byte의 내용은 runtime에서 검사해야 한다.

#### 16. runtime validation을 별도로 한다

아주 단순하게 직접 확인할 수 있다.

```typescript
function isUser(value: unknown): value is User {
  if (typeof value !== "object" || value === null) return false;

  const candidate = value as Record<string, unknown>;

  return (
    typeof candidate.id === "number" &&
    typeof candidate.name === "string"
  );
}
```

실전에서는 Zod 같은 검증 라이브러리를 사용할 수도 있다.

핵심은:

```text
TypeScript 타입 선언
≠
외부 데이터 실제 검증
```

이다.

#### 17. 타입이 맞아도 논리는 틀릴 수 있다

```typescript
function total(price: number, count: number): number {
  return price + count;
}
```

모든 타입이 맞다.

하지만 총가격 규칙이 `price * count`라면 로직은 틀렸다.

TypeScript는 비즈니스 의도를 자동으로 알지 못한다.

그래서 테스트와 실행 검증이 여전히 필요하다.

#### 18. TRACK 프로젝트 · 안전한 검색 앱

다음 기능을 하나씩 만든다.

```text
1. 검색 input
2. 300ms debounce
3. fetch 요청
4. loading/error/success 상태 분리
5. 이전 요청 AbortController 취소
6. 최신 요청 ID 검증
7. TypeScript로 API 응답 타입 정의
8. 외부 응답 runtime validation
9. 화면 render
10. 실패 메시지
```

일부러 race를 재현한다.

```text
A 요청 = 3초
B 요청 = 0.5초
A 먼저 시작
B 나중 시작
```

취소와 최신 요청 검사를 제거해 오래된 A가 B를 덮는 것을 확인한다.

그 다음 고친다.

#### 19. TRACK 05 완료 기준

책을 보지 않고 다음을 설명할 수 있어야 한다.

- 브라우저/Node.js와 JavaScript 언어의 관계
- let/const/var의 큰 차이
- primitive와 object reference 차이
- 얕은 복사와 중첩 객체 공유 문제
- `===`와 자동 type coercion 차이
- lexical scope와 closure
- this가 호출 방식에 영향을 받는 이유
- prototype과 class 관계
- map/filter/reduce 용도 차이
- callback이 왜 비동기 흐름에서 사용되는지
- Promise의 세 상태와 then/catch chain
- async 함수와 await의 의미
- call stack/task/microtask/event loop 실행 순서
- Promise.all과 순차 await의 차이
- timeout/cancellation/race condition 차이
- debounce와 throttle 차이
- TypeScript union/narrowing/unknown/generic의 목적
- TypeScript가 runtime JSON을 자동 검증하지 않는 이유

비동기 코드를 보고 `async/await 문법을 안다`에서 끝나면 통과가 아니다.

**요청이 언제 시작되고, 어디서 기다리고, 무엇이 먼저 완료되고, 실패와 오래된 결과를 어떻게 처리하는지 시간 순서로 설명할 수 있어야 한다.**
