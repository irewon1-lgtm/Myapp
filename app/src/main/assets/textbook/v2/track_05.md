# TRACK 05 · JavaScript와 TypeScript 깊게

TRACK 04에서 브라우저 화면을 만들었다.

이제 그 화면을 움직이는 JavaScript를 깊게 이해한다.

이 TRACK의 핵심은 문법 목록이 아니다.

> 코드가 **언제**, **어떤 값과 상태로**, **어떤 순서로** 실행되는지 추적하는 능력

이다.

특히 비동기는 어려운 단어를 한꺼번에 던지지 않고 `기다림`부터 시작해 Promise, async/await, event loop, microtask, race condition 순으로 쌓는다.

---

## BLOCK 01 · JavaScript는 무엇인가

### LESSON 01 · 브라우저에서 실행되는 프로그래밍 언어

**JavaScript**는 웹 브라우저에서 사용자 상호작용과 동적인 기능을 구현하는 데 널리 사용하는 프로그래밍 언어다.

오늘날에는 Node.js를 이용해 서버와 도구를 만드는 데도 사용한다.

### LESSON 02 · Java와 다른 언어다

이름이 비슷하지만 JavaScript와 Java는 서로 다른 프로그래밍 언어다.

문법 일부가 비슷해 보일 수 있지만 실행 환경과 언어 설계가 다르다.

---

## BLOCK 02 · JavaScript 실행 환경

### LESSON 01 · 브라우저

브라우저는 JavaScript 엔진과 웹 API를 제공한다.

JavaScript 엔진은 코드를 실행한다.

브라우저의 웹 API는 DOM, timer, network 같은 기능을 제공한다.

### LESSON 02 · Node.js

**Node.js**는 브라우저 밖에서도 JavaScript를 실행할 수 있게 만든 실행 환경이다.

파일, 네트워크 서버, 프로세스 같은 기능을 제공한다.

```text
JavaScript = 언어
브라우저/Node.js = 그 언어를 실행하는 환경
```

---

## BLOCK 03 · let과 const

### LESSON 01 · let

값이 바뀔 수 있는 변수는 `let`으로 선언할 수 있다.

```javascript
let score = 10;
score = 20;
```

### LESSON 02 · const

다시 다른 값을 대입하지 않을 이름은 `const`로 선언한다.

```javascript
const taxRate = 0.1;
```

### LESSON 03 · const 객체 내부는 바뀔 수 있다

```javascript
const user = { name: "민수" };
user.name = "지수";
```

가능하다.

`const`는 변수 이름이 다른 객체를 가리키도록 다시 대입하는 것을 막는 것이지 객체 내부를 자동으로 불변으로 만드는 것은 아니다.

---

## BLOCK 04 · var가 다른 이유

### LESSON 01 · 오래된 변수 선언 방식

JavaScript에는 `var`도 있다.

`var`는 함수 스코프와 hoisting 동작 때문에 `let`, `const`와 다르게 행동한다.

새 코드에서는 보통 `const`와 `let`을 먼저 사용한다.

### LESSON 02 · 함수 스코프

```javascript
if (true) {
  var x = 10;
}
console.log(x);
```

`var`로 만든 `x`는 if 블록 밖에서도 보일 수 있다.

`let`과 `const`는 블록 스코프를 가진다.

---

## BLOCK 05 · primitive 값

### LESSON 01 · 원시 타입

JavaScript의 기본 값 가운데 객체가 아닌 값을 **primitive(원시 값)**라고 부른다.

대표적으로:

```text
string
number
boolean
null
undefined
bigint
symbol
```

이 있다.

### LESSON 02 · null과 undefined

`undefined`는 값이 아직 할당되지 않았거나 존재하지 않는 상황에서 나타난다.

`null`은 개발자가 의도적으로 `값 없음`을 나타낼 때 사용할 수 있다.

둘은 비슷해 보이지만 다른 값이다.

---

## BLOCK 06 · 객체와 참조

### LESSON 01 · 객체

JavaScript 객체는 key와 value를 묶어 데이터를 표현한다.

```javascript
const user = {
  name: "민수",
  age: 20
};
```

### LESSON 02 · 같은 객체를 가리키기

```javascript
const a = { count: 1 };
const b = a;
b.count = 2;
console.log(a.count);
```

결과:

```text
2
```

`a`와 `b`가 같은 객체를 가리키기 때문이다.

---

## BLOCK 07 · 값 비교

### LESSON 01 · ===

JavaScript에서는 값과 타입을 함께 비교하는 **엄격한 동등 비교(strict equality)** `===`를 많이 사용한다.

```javascript
10 === 10       // true
10 === "10"     // false
```

### LESSON 02 · ==

`==`는 비교 전에 타입 변환을 수행할 수 있다.

```javascript
10 == "10"      // true
```

예상하기 어려운 변환을 줄이기 위해 일반적으로 `===` 사용을 선호한다.

---

## BLOCK 08 · type coercion

### LESSON 01 · 자동 타입 변환

JavaScript가 연산 중 타입을 자동으로 바꾸는 것을 **타입 강제 변환(type coercion)**이라고 부른다.

```javascript
console.log("5" + 1);
```

결과:

```text
51
```

문자열 연결이 일어났다.

### LESSON 02 · 명시적 변환

```javascript
Number("5") + 1
```

처럼 개발자가 의도를 분명히 적는 것을 **명시적 변환**이라고 부른다.

자동 변환에 기대기보다 입력 경계에서 타입을 명확히 만드는 습관이 좋다.

---

## BLOCK 09 · NaN

### LESSON 01 · 숫자로 바꾸지 못한 결과

```javascript
Number("hello")
```

결과는 `NaN`이다.

`NaN`은 `Not a Number`라는 이름이지만 JavaScript의 number 타입 안에 있는 특별한 값이다.

### LESSON 02 · NaN 비교

```javascript
NaN === NaN
```

은 false다.

값이 NaN인지 확인할 때는:

```javascript
Number.isNaN(value)
```

를 사용할 수 있다.

---

## BLOCK 10 · 함수 선언

### LESSON 01 · function

```javascript
function add(a, b) {
  return a + b;
}
```

특정 작업을 이름 붙여 묶은 함수다.

### LESSON 02 · 함수도 값이다

JavaScript에서는 함수를 변수에 넣고 다른 함수에 전달할 수 있다.

```javascript
const add = function (a, b) {
  return a + b;
};
```

함수를 값처럼 다룰 수 있는 성질은 callback과 함수형 패턴의 기초가 된다.

---

## BLOCK 11 · arrow function

### LESSON 01 · 짧은 함수 문법

```javascript
const add = (a, b) => {
  return a + b;
};
```

한 줄이면:

```javascript
const add = (a, b) => a + b;
```

### LESSON 02 · 일반 function과 완전히 같지 않다

arrow function은 자신만의 `this`를 만들지 않는다.

`arguments` 객체도 일반 function과 다르게 동작한다.

단순히 `더 짧은 함수`라고만 외우지 않는다.

---

## BLOCK 12 · lexical scope

### LESSON 01 · 코드가 작성된 위치에 따라 변수 범위가 정해진다

JavaScript는 함수가 **어디에서 정의되었는지**에 따라 바깥 변수를 찾는다.

이를 **렉시컬 스코프(lexical scope)**라고 부른다.

```javascript
const name = "밖";

function show() {
  console.log(name);
}
```

`show`는 자신이 정의된 바깥 영역의 `name`을 찾는다.

---

## BLOCK 13 · closure를 배우기 전에

### LESSON 01 · 함수가 만들어진 바깥 환경을 기억한다

```javascript
function makeCounter() {
  let count = 0;

  return function () {
    count += 1;
    return count;
  };
}
```

`makeCounter()`가 끝난 뒤에도 반환된 함수는 `count`를 계속 사용할 수 있다.

### LESSON 02 · closure

함수가 자신이 만들어질 때 접근할 수 있던 바깥 변수 환경을 계속 기억하고 사용할 수 있는 현상을 **클로저(closure)**라고 부른다.

```javascript
const counter = makeCounter();
console.log(counter()); // 1
console.log(counter()); // 2
```

---

## BLOCK 14 · closure가 어디에 쓰이나

### LESSON 01 · private한 상태처럼 사용

```javascript
function createWallet() {
  let money = 0;

  return {
    add(amount) {
      money += amount;
    },
    get() {
      return money;
    }
  };
}
```

밖에서는 `money`를 직접 바꾸지 못하고 제공된 함수로만 접근하게 만들 수 있다.

### LESSON 02 · callback과 closure

이벤트 handler가 바깥 변수에 접근할 때도 closure가 사용된다.

```javascript
let clicks = 0;
button.addEventListener("click", () => {
  clicks += 1;
});
```

---

## BLOCK 15 · this

### LESSON 01 · 호출 방식에 따라 달라지는 값

JavaScript의 `this`는 함수가 **어떻게 호출되었는지**에 따라 가리키는 값이 달라질 수 있다.

```javascript
const user = {
  name: "민수",
  show() {
    console.log(this.name);
  }
};
```

`user.show()`에서 `this`는 `user`를 가리킨다.

### LESSON 02 · 함수를 떼어내면

```javascript
const show = user.show;
show();
```

호출 방식이 바뀌었기 때문에 `this`도 달라질 수 있다.

`this`를 변수처럼 정의 위치만 보고 판단하면 틀릴 수 있다.

---

## BLOCK 16 · prototype

### LESSON 01 · 객체가 다른 객체의 기능을 찾는다

JavaScript 객체는 자신에게 없는 속성을 **프로토타입(prototype)** 연결을 따라 찾아볼 수 있다.

한 줄 뜻:

> prototype = JavaScript 객체가 공통 기능을 물려받아 찾을 수 있도록 연결하는 객체 구조

### LESSON 02 · 배열 메서드는 어디에서 오나

```javascript
const items = [1, 2, 3];
items.map(...)
```

배열 객체 자체에 모든 메서드를 각각 복사해 넣는 것이 아니라 Array prototype에서 공통 메서드를 찾을 수 있다.

---

## BLOCK 17 · class 문법

### LESSON 01 · prototype 구조를 더 익숙한 문법으로

```javascript
class User {
  constructor(name) {
    this.name = name;
  }

  greet() {
    return `안녕 ${this.name}`;
  }
}
```

JavaScript의 class 문법은 내부적으로 prototype 기반 객체 모델 위에 만들어진 문법이다.

### LESSON 02 · constructor

새 인스턴스를 만들 때 초기 값을 설정하는 특별한 메서드를 `constructor`라고 부른다.

```javascript
const user = new User("민수");
```

---

## BLOCK 18 · 배열 고차 함수

### LESSON 01 · map

```javascript
const numbers = [1, 2, 3];
const doubled = numbers.map(n => n * 2);
```

각 요소를 변환해 새 배열을 만든다.

### LESSON 02 · filter

```javascript
const even = numbers.filter(n => n % 2 === 0);
```

조건에 맞는 값만 새 배열에 남긴다.

### LESSON 03 · reduce

```javascript
const total = numbers.reduce((sum, n) => sum + n, 0);
```

여러 값을 하나의 누적 결과로 합친다.

---

## BLOCK 19 · destructuring

### LESSON 01 · 배열 구조 분해

```javascript
const [first, second] = [10, 20];
```

배열 값을 위치에 따라 변수로 꺼낸다.

### LESSON 02 · 객체 구조 분해

```javascript
const user = { name: "민수", age: 20 };
const { name, age } = user;
```

객체 속성을 이름으로 꺼낸다.

---

## BLOCK 20 · spread와 rest

### LESSON 01 · spread

```javascript
const a = [1, 2];
const b = [...a, 3];
```

`...a`는 배열의 요소를 펼친다.

### LESSON 02 · 객체 복사

```javascript
const user2 = { ...user, age: 21 };
```

새 객체를 만들며 속성을 복사한다.

얕은 복사이므로 중첩 객체는 여전히 공유될 수 있다.

### LESSON 03 · rest

함수 parameter에서 남은 값을 모을 수도 있다.

```javascript
function sum(...numbers) {
  // numbers는 배열
}
```

---

## BLOCK 21 · module

### LESSON 01 · 파일을 역할별로 나누기

**모듈(module)**은 관련 코드를 독립된 단위로 나눈 것이다.

JavaScript의 ES module에서는 `export`, `import`를 사용한다.

```javascript
export function add(a, b) {
  return a + b;
}
```

```javascript
import { add } from "./math.js";
```

### LESSON 02 · dependency

다른 모듈의 기능이 필요하면 해당 모듈에 **의존(depend)**한다.

의존 관계가 너무 복잡해지면 코드 변경 영향 범위가 커진다.

---

## BLOCK 22 · error object

### LESSON 01 · 오류 정보를 객체로 다룬다

JavaScript는 오류 상황을 `Error` 객체로 표현할 수 있다.

```javascript
throw new Error("잘못된 상태");
```

### LESSON 02 · throw

`throw`는 정상 실행 흐름을 멈추고 예외를 발생시킨다.

```javascript
if (age < 0) {
  throw new Error("나이는 음수일 수 없습니다");
}
```

---

## BLOCK 23 · try/catch/finally

### LESSON 01 · catch

```javascript
try {
  riskyWork();
} catch (error) {
  console.error(error);
}
```

문제가 생기면 `catch`로 이동한다.

### LESSON 02 · finally

성공·실패와 관계없이 실행할 정리 코드를 `finally`에 둘 수 있다.

```javascript
try {
  open();
} finally {
  close();
}
```

---

## BLOCK 24 · 비동기를 배우기 전에 기다림

### LESSON 01 · 시간이 걸리는 작업

다음 작업은 즉시 끝나지 않을 수 있다.

```text
서버 응답 기다리기
파일 읽기
타이머 기다리기
사용자 클릭 기다리기
```

### LESSON 02 · 기다리는 동안 프로그램 전체를 세워야 할까

사진 다운로드에 3초가 걸린다고 브라우저 버튼과 스크롤까지 3초 동안 모두 멈출 필요는 없다.

이 문제를 해결하는 핵심이 비동기 처리다.

---

## BLOCK 25 · blocking

### LESSON 01 · 다음 진행을 막고 기다리기

어떤 작업이 끝날 때까지 해당 실행 흐름이 다음 코드를 진행하지 못하는 상황을 **블로킹(blocking)**이라고 부른다.

### LESSON 02 · UI thread를 막으면

브라우저의 주 JavaScript 실행이 매우 긴 계산에 붙잡히면 사용자 클릭과 화면 업데이트가 늦어질 수 있다.

```javascript
while (true) {
  // 끝나지 않음
}
```

이런 코드는 화면을 멈춘 것처럼 보이게 할 수 있다.

---

## BLOCK 26 · 동기

### LESSON 01 · 앞 작업이 끝난 뒤 다음 작업

```text
A 시작
A 완료
B 시작
B 완료
```

앞 작업 완료를 기다리며 순서대로 이어가는 흐름을 **동기(synchronous)** 방식이라고 부른다.

### LESSON 02 · 동기가 나쁜 것은 아니다

즉시 끝나는 계산과 순서가 반드시 필요한 작업에서는 동기 흐름이 이해하기 쉽다.

문제는 오래 걸리는 작업이 다른 진행까지 막을 때다.

---

## BLOCK 27 · 비동기

### LESSON 01 · 기다리는 동안 다른 일을 처리한다

```text
A 시작
A는 기다리는 중
B 실행 가능
A 완료 알림
A 결과 처리
```

이처럼 시간이 걸리는 작업의 완료를 그 자리에서 계속 붙잡지 않고, 결과가 준비되면 이어 처리하는 구성을 **비동기(asynchronous)**라고 부른다.

### LESSON 02 · 동시에 CPU 두 개에서 실행된다는 뜻은 아니다

비동기는 `기다림을 처리하는 구조`다.

실제로 여러 CPU 코어에서 같은 시간에 계산하는 **병렬성(parallelism)**과는 다른 개념이다.

---

## BLOCK 28 · callback

### LESSON 01 · 나중에 실행할 함수

```javascript
setTimeout(function () {
  console.log("끝");
}, 1000);
```

`1초 정도 뒤에 이 함수를 실행해`라고 넘긴 함수가 callback이다.

한 줄 뜻:

> callback = 특정 작업이나 사건이 끝났을 때 나중에 실행하도록 넘겨 둔 함수

### LESSON 02 · callback nesting

비동기 단계가 계속 이어지면:

```javascript
first(() => {
  second(() => {
    third(() => {
      // ...
    });
  });
});
```

처럼 깊게 중첩되어 읽기 어려워질 수 있다.

---

## BLOCK 29 · Promise

### LESSON 01 · 미래 결과를 나타낸다

**Promise**는 나중에 성공 결과 또는 실패 이유가 정해질 비동기 작업을 표현하는 JavaScript 객체다.

### LESSON 02 · 세 상태

```text
pending   = 아직 결과 없음
fulfilled = 성공해서 결과 준비됨
rejected  = 실패해서 이유 준비됨
```

Promise는 한 번 fulfilled 또는 rejected가 되면 그 결과 상태가 다시 다른 결과로 바뀌지 않는다.

---

## BLOCK 30 · then과 catch

### LESSON 01 · 성공 처리

```javascript
getData().then(data => {
  console.log(data);
});
```

Promise가 성공했을 때 결과를 받아 다음 작업을 한다.

### LESSON 02 · 실패 처리

```javascript
getData()
  .then(data => console.log(data))
  .catch(error => console.error(error));
```

실패 경로를 명시한다.

---

## BLOCK 31 · Promise chaining

### LESSON 01 · then에서 새 값 반환

```javascript
getUser()
  .then(user => user.id)
  .then(id => getOrders(id))
  .then(orders => console.log(orders));
```

각 `then`이 반환한 값이 다음 `then`으로 전달된다.

### LESSON 02 · Promise를 반환하면 기다린다

`then` 안에서 새 Promise를 반환하면 그 Promise가 완료된 뒤 다음 단계가 이어진다.

이 연결을 이해해야 중첩 callback을 줄일 수 있다.

---

## BLOCK 32 · async

### LESSON 01 · Promise 기반 함수를 선언한다

```javascript
async function load() {
  return 10;
}
```

async 함수는 항상 Promise를 반환한다.

위 함수는 `10`을 직접 반환하는 것처럼 보이지만 실제 호출 결과는 `Promise`다.

### LESSON 02 · async = 병렬 실행이 아니다

함수 앞에 `async`를 붙였다고 자동으로 여러 코어에서 동시에 실행되는 것이 아니다.

Promise와 await를 사용하기 좋은 비동기 함수 형태가 된다.

---

## BLOCK 33 · await

### LESSON 01 · Promise 결과를 기다리는 문법

```javascript
async function load() {
  const data = await getData();
  console.log(data);
}
```

사람말:

```text
getData 작업 시작
결과가 준비될 때까지 load 함수의 다음 부분 대기
결과가 오면 data에 저장
출력
```

### LESSON 02 · 전체 프로그램이 멈추지는 않는다

await는 해당 async 함수의 이어지는 코드를 기다리게 한다.

그동안 실행 환경은 다른 이벤트와 준비된 작업을 처리할 수 있다.

---

## BLOCK 34 · async 오류 처리

### LESSON 01 · rejected Promise

await한 Promise가 rejected되면 예외가 발생한다.

```javascript
async function load() {
  try {
    const data = await getData();
    return data;
  } catch (error) {
    console.error(error);
  }
}
```

### LESSON 02 · 실패를 숨기지 않는다

catch에서 오류를 기록만 하고 정상 값처럼 `undefined`를 반환하면 호출한 쪽이 실패를 모를 수 있다.

오류를 어디에서 처리할지 설계해야 한다.

---

## BLOCK 35 · event loop를 배우기 전에

### LESSON 01 · 지금 실행 중인 JavaScript는 하나의 흐름에서 진행된다

브라우저의 메인 JavaScript 실행은 한 순간에 하나의 함수 호출 흐름을 실행한다.

타이머나 네트워크 작업은 브라우저가 별도 기능으로 처리할 수 있다.

### LESSON 02 · 완료된 작업의 callback은 언제 실행할까

현재 코드가 실행 중이면 타이머 callback을 아무 중간 지점에 끼워 넣지 않는다.

현재 호출 스택이 비면 준비된 다음 작업을 실행할 수 있다.

---

## BLOCK 36 · call stack

### LESSON 01 · 함수 호출이 쌓인다

```javascript
function a() { b(); }
function b() { c(); }
function c() { console.log("끝"); }
a();
```

호출 흐름:

```text
a
→ b
→ c
```

현재 실행 중인 함수 호출을 쌓아 관리하는 구조를 **call stack**이라고 부른다.

### LESSON 02 · 함수가 끝나면

`c`가 끝나면 stack에서 빠지고 `b`로 돌아간다.

`b`가 끝나면 `a`로 돌아간다.

---

## BLOCK 37 · task queue

### LESSON 01 · 나중에 실행할 준비가 된 작업

타이머가 끝나 callback이 실행 준비가 되어도 현재 JavaScript 코드가 실행 중이면 기다려야 한다.

이런 task가 기다리는 대기 구조를 **task queue**라고 부른다.

### LESSON 02 · queue가 하나뿐이라고 단순화하지 않는다

실행 환경에는 종류별로 여러 task source와 microtask queue가 있다.

중요한 것은 `모든 준비된 일이 정확히 한 줄에 같은 우선순위로 선다`는 생각이 틀릴 수 있다는 것이다.

---

## BLOCK 38 · event loop

### LESSON 01 · 실행할 다음 일을 조정한다

**event loop**는 현재 JavaScript 실행 상태와 준비된 작업을 확인해 다음 작업을 실행하도록 계속 조정하는 구조다.

아주 쉽게:

> 지금 코드가 끝났으면 기다리던 다음 일을 실행할 수 있게 관리한다.

### LESSON 02 · event loop가 network를 직접 하는 것은 아니다

네트워크 요청 자체는 브라우저의 네트워크 기능이 수행한다.

event loop는 완료 후 이어질 JavaScript callback을 실행 흐름에 연결하는 데 중요하다.

---

## BLOCK 39 · microtask

### LESSON 01 · Promise 후속 작업

Promise의 `.then()`이나 `await` 이후 이어지는 작업은 **microtask**로 예약된다.

microtask는 현재 실행 중인 코드가 끝난 뒤 일반적인 다음 task보다 먼저 처리된다.

### LESSON 02 · 출력 순서

```javascript
console.log("A");

setTimeout(() => console.log("B"), 0);

Promise.resolve().then(() => console.log("C"));

console.log("D");
```

일반적인 결과:

```text
A
D
C
B
```

순서:

```text
현재 동기 코드 A,D
→ microtask C
→ timer task B
```

---

## BLOCK 40 · 여러 Promise를 동시에 시작하기

### LESSON 01 · 순차 await

```javascript
const a = await getA();
const b = await getB();
```

A가 끝난 뒤 B를 시작한다.

둘이 서로 독립적이라면 불필요하게 오래 걸릴 수 있다.

### LESSON 02 · Promise.all

```javascript
const [a, b] = await Promise.all([
  getA(),
  getB()
]);
```

두 작업을 먼저 시작하고 둘 다 끝날 때까지 기다린다.

한 작업이 실패하면 Promise.all도 실패한다.

---

## BLOCK 41 · Promise.allSettled

### LESSON 01 · 일부 실패도 결과로 보고 싶을 때

```javascript
const results = await Promise.allSettled([
  getA(),
  getB()
]);
```

각 작업의 성공/실패 상태를 모두 결과로 받는다.

### LESSON 02 · all과 allSettled 구분

```text
Promise.all = 하나라도 실패하면 전체 Promise가 reject
Promise.allSettled = 모든 작업이 끝난 뒤 각 결과를 확인
```

목적에 따라 선택한다.

---

## BLOCK 42 · timeout

### LESSON 01 · 영원히 기다리지 않기

작업이 정해진 시간 안에 끝나지 않으면 실패로 처리하는 제한을 **timeout**이라고 부른다.

### LESSON 02 · timeout 값도 설계다

너무 짧으면 정상 요청도 실패한다.

너무 길면 장애 상황에서 사용자가 오래 기다린다.

실제 응답 시간 분포와 사용자 경험을 보고 정한다.

---

## BLOCK 43 · cancellation

### LESSON 01 · 더 이상 필요 없는 작업

사용자가 검색어를 빠르게 바꾸면 이전 검색 결과는 필요 없을 수 있다.

진행 중인 작업을 중단하거나 결과 반영을 막는 것을 **cancellation**이라고 부른다.

### LESSON 02 · AbortController

브라우저 fetch 요청은 `AbortController`를 이용해 취소 신호를 전달할 수 있다.

```javascript
const controller = new AbortController();

fetch("/search", { signal: controller.signal });
controller.abort();
```

---

## BLOCK 44 · race condition

### LESSON 01 · 완료 순서가 뒤집힌다

```text
검색 A 시작 ───────── 완료
검색 B 시작 ── 완료
```

사용자가 B를 마지막으로 검색했는데 늦게 도착한 A 결과가 화면을 덮으면 잘못된 상태가 된다.

### LESSON 02 · 이름 붙이기

여러 작업의 실행·완료 순서에 따라 결과가 달라지는 문제를 **race condition**이라고 부른다.

### LESSON 03 · 해결 후보

```text
이전 요청 취소
요청 번호 비교
최신 검색어와 결과 연결 확인
```

중 시스템에 맞는 방법을 사용한다.

---

## BLOCK 45 · debounce와 throttle

### LESSON 01 · debounce

검색창에서 사용자가 계속 타이핑하는 동안 매 글자마다 요청하지 않고 입력이 잠시 멈춘 뒤 한 번 실행하는 방식을 **debounce**라고 부른다.

### LESSON 02 · throttle

스크롤 이벤트처럼 매우 자주 발생하는 사건을 일정 시간에 한 번 정도만 처리하도록 제한하는 방식을 **throttle**이라고 부른다.

```text
debounce = 마지막 입력 이후 잠잠해지면 실행
throttle = 정해진 간격으로 최대 실행 횟수 제한
```

---

## BLOCK 46 · TypeScript는 왜 필요한가

### LESSON 01 · JavaScript 오류를 실행 전에 더 많이 잡기

**TypeScript**는 JavaScript에 정적 타입 시스템을 추가한 언어다.

코드를 실행하기 전에 많은 타입 오류를 발견할 수 있다.

### LESSON 02 · 정적 타입

프로그램 실행 전에 변수와 함수가 어떤 종류의 값을 다룰지 검사하는 것을 **정적 타입 검사(static type checking)**라고 부른다.

```typescript
let age: number = 20;
age = "스무살"; // 타입 오류
```

---

## BLOCK 47 · Type annotation

### LESSON 01 · 타입을 직접 적기

```typescript
let name: string = "민수";
let age: number = 20;
let active: boolean = true;
```

`: string`, `: number`처럼 타입을 직접 적는 것을 **type annotation**이라고 부른다.

### LESSON 02 · type inference

```typescript
let age = 20;
```

TypeScript는 초기값을 보고 age가 number라고 추론할 수 있다.

이를 **type inference(타입 추론)**라고 부른다.

---

## BLOCK 48 · 함수 타입

### LESSON 01 · parameter 타입

```typescript
function add(a: number, b: number): number {
  return a + b;
}
```

### LESSON 02 · return 타입

마지막 `: number`는 이 함수가 number를 반환한다는 뜻이다.

실수로 문자열을 반환하면 TypeScript가 오류를 알려줄 수 있다.

---

## BLOCK 49 · object type

### LESSON 01 · 객체 모양 정의

```typescript
type User = {
  id: number;
  name: string;
};
```

`User`라는 객체 모양을 정의했다.

### LESSON 02 · optional property

```typescript
type User = {
  id: number;
  nickname?: string;
};
```

`?`는 해당 속성이 없을 수도 있음을 나타낸다.

---

## BLOCK 50 · interface

### LESSON 01 · 객체 계약 표현

```typescript
interface User {
  id: number;
  name: string;
}
```

**interface**는 객체나 클래스가 가져야 할 구조를 표현할 수 있다.

### LESSON 02 · type과 interface

둘은 겹치는 기능이 많지만 완전히 같지는 않다.

interface는 선언 병합과 객체 계약 표현에 강점이 있고 type은 union 등 더 다양한 타입 조합을 표현할 수 있다.

프로젝트 규칙에 맞춰 일관되게 사용한다.

---

## BLOCK 51 · union type

### LESSON 01 · 여러 타입 중 하나

```typescript
let id: string | number;
```

`id`는 string 또는 number일 수 있다.

`|`로 연결한 타입을 **union type**이라고 부른다.

### LESSON 02 · 바로 아무 메서드나 쓸 수 없다

```typescript
id.toUpperCase();
```

number에는 `toUpperCase`가 없기 때문에 TypeScript가 막는다.

먼저 현재 타입을 좁혀야 한다.

---

## BLOCK 52 · narrowing

### LESSON 01 · 실제 타입을 좁혀 확인하기

```typescript
if (typeof id === "string") {
  console.log(id.toUpperCase());
}
```

조건을 이용해 union 타입에서 실제 가능한 타입을 좁히는 것을 **narrowing**이라고 부른다.

### LESSON 02 · discriminated union

```typescript
type Result =
  | { status: "success"; data: string }
  | { status: "error"; message: string };
```

`status` 값을 보고 어떤 객체 형태인지 안전하게 좁힐 수 있다.

---

## BLOCK 53 · any와 unknown

### LESSON 01 · any

`any`는 타입 검사를 대부분 꺼 버린다.

```typescript
let value: any;
value.notExisting().whatever;
```

컴파일 단계에서 막지 않을 수 있다.

### LESSON 02 · unknown

외부에서 어떤 타입이 올지 모를 때 `unknown`을 사용하면 사용 전에 타입 확인을 요구한다.

```typescript
let value: unknown;

if (typeof value === "string") {
  console.log(value.toUpperCase());
}
```

외부 입력에서는 any보다 안전한 선택이 될 수 있다.

---

## BLOCK 54 · never

### LESSON 01 · 발생할 수 없는 타입

정상적으로 값을 반환하지 않는 함수나 모든 경우를 처리했는지 확인할 때 **never** 타입을 사용할 수 있다.

```typescript
function fail(message: string): never {
  throw new Error(message);
}
```

### LESSON 02 · exhaustive check

union의 모든 경우를 처리했는지 컴파일러에게 확인시키는 패턴에 never를 사용할 수 있다.

새 상태를 추가했는데 switch 처리를 빼먹는 오류를 잡는 데 도움이 된다.

---

## BLOCK 55 · generic

### LESSON 01 · 타입을 parameter처럼 받기

```typescript
function first<T>(items: T[]): T | undefined {
  return items[0];
}
```

`T`는 나중에 실제 타입으로 채워지는 **generic type parameter**다.

### LESSON 02 · 왜 필요한가

string 배열, number 배열에 같은 로직을 재사용하면서도 타입 정보를 잃지 않는다.

```typescript
first([1, 2, 3])       // number | undefined
first(["a", "b"])     // string | undefined
```

---

## BLOCK 56 · TypeScript가 못 잡는 것

### LESSON 01 · 타입이 맞아도 로직은 틀릴 수 있다

```typescript
function total(price: number, count: number): number {
  return price + count;
}
```

타입은 모두 맞다.

하지만 총가격이면 곱셈이 맞을 수 있다.

TypeScript는 비즈니스 로직의 의도까지 자동으로 안다.

### LESSON 02 · 외부 데이터는 runtime 검증이 필요하다

서버에서 받는 JSON이 TypeScript 타입과 다를 수 있다.

타입 선언만 했다고 실제 데이터가 자동 검증되는 것은 아니다.

외부 입력은 runtime validation이 필요하다.

---

## BLOCK 57 · 핵심 용어 사전

| 용어 | 아주 쉬운 뜻 |
|---|---|
| JavaScript | 웹 브라우저와 Node.js 등에서 실행되는 프로그래밍 언어 |
| primitive | 객체가 아닌 기본 값 종류 |
| strict equality | 타입 변환 없이 값과 타입을 비교하는 === 연산 |
| type coercion | JavaScript가 자동으로 타입을 바꾸는 동작 |
| lexical scope | 코드가 정의된 위치를 기준으로 변수 범위를 찾는 규칙 |
| closure | 함수가 만들어질 때의 바깥 변수 환경을 계속 사용할 수 있는 현상 |
| this | 함수 호출 방식에 따라 결정되는 특별한 참조 값 |
| prototype | 객체가 공통 속성과 메서드를 찾는 연결 구조 |
| module | 관련 코드를 나눈 독립 단위 |
| blocking | 어떤 작업을 기다리며 다음 진행을 막는 상태 |
| synchronous | 앞 작업 완료를 기다리며 순서대로 진행하는 방식 |
| asynchronous | 기다리는 동안 다른 일을 처리하고 완료 후 이어가는 방식 |
| callback | 나중에 실행하도록 전달하는 함수 |
| Promise | 미래의 성공 결과 또는 실패를 표현하는 객체 |
| async | Promise 기반 비동기 함수를 만드는 문법 |
| await | Promise 결과를 기다리는 async 함수 문법 |
| call stack | 현재 실행 중인 함수 호출을 쌓아 관리하는 구조 |
| task queue | 실행 준비가 된 일반 task가 기다리는 구조 |
| event loop | 현재 실행과 대기 작업을 조정해 다음 JavaScript를 실행하는 구조 |
| microtask | Promise 후속 작업 등이 기다리는 높은 우선순위 대기 종류 |
| race condition | 작업 완료 순서에 따라 결과가 달라지는 문제 |
| debounce | 입력이 잠잠해진 뒤 한 번 실행하는 제한 방식 |
| throttle | 일정 시간에 실행 횟수를 제한하는 방식 |
| TypeScript | JavaScript에 정적 타입 검사를 추가한 언어 |
| type inference | 코드에서 타입을 자동으로 추론하는 기능 |
| union | 여러 타입 중 하나일 수 있음을 표현하는 타입 |
| narrowing | 조건으로 가능한 타입 범위를 좁히는 과정 |
| unknown | 사용 전에 타입 확인이 필요한 미확정 타입 |
| never | 정상적으로 값이 존재할 수 없음을 나타내는 타입 |
| generic | 타입 자체를 parameter처럼 받아 재사용하는 기능 |

---

## BLOCK 58 · TRACK 05 완료 기준

다음을 직접 할 수 있어야 한다.

- let/const/var의 큰 차이를 설명한다.
- primitive와 object reference를 구분한다.
- `===`와 `==` 차이를 설명한다.
- 자동 type coercion 오류를 찾아낸다.
- lexical scope와 closure를 코드로 설명한다.
- `this`가 호출 방식에 따라 달라지는 사례를 재현한다.
- prototype과 class 문법의 관계를 설명한다.
- map/filter/reduce를 직접 사용한다.
- ES module을 나누고 import/export한다.
- Promise의 세 상태를 설명한다.
- Promise chain과 async/await를 서로 변환해 설명한다.
- event loop, call stack, task, microtask 실행 순서를 예측한다.
- timeout과 cancellation을 구현한다.
- race condition을 실제 코드로 재현하고 고친다.
- TypeScript union/narrowing/interface/generic을 사용한다.
- any와 unknown의 안전성 차이를 설명한다.
- TypeScript가 runtime 데이터 정확성을 보장하지 않는 이유를 말한다.

### TRACK 프로젝트 · 검색 앱

브라우저에서 동작하는 검색 앱을 만든다.

필수 기능:

```text
검색어 입력
300ms debounce
이전 요청 AbortController로 취소
loading/error/success 상태 분리
가장 최신 검색 결과만 화면에 반영
검색 결과 배열 렌더링
TypeScript로 응답 타입 정의
외부 응답 runtime 검증
```

실험할 오류:

1. A 요청을 3초 늦게 응답시킨다.
2. B 요청을 0.5초 만에 응답시킨다.
3. 취소와 최신 요청 확인을 제거해 race condition을 재현한다.
4. 다시 고쳐 오래된 A 결과가 B를 덮지 못하게 한다.
5. Network와 Console에서 실제 요청 순서를 기록한다.

비동기를 `async/await 문법`으로 외우는 것이 아니라 **실제 시간 순서를 설명할 수 있어야** 통과다.
