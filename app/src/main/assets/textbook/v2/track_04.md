# TRACK 04 · 웹 화면과 브라우저

이 TRACK에서는 사용자가 실제로 보는 웹 화면을 만든다.

화면을 만들 때 세 가지 역할을 먼저 구분한다.

```text
HTML = 무엇이 있는가
CSS = 어떻게 보이는가
JavaScript = 어떻게 움직이는가
```

처음에는 이 구분만 잡고, 뒤에서 브라우저 내부의 DOM·이벤트·레이아웃·렌더링까지 들어간다.

---

## BLOCK 01 · 웹과 인터넷은 같은 말인가

### LESSON 01 · 인터넷

인터넷은 전 세계의 많은 컴퓨터 네트워크를 연결한 거대한 연결망이다.

### LESSON 02 · 웹

**웹(World Wide Web)**은 인터넷을 이용해 문서와 자원을 서로 연결하고 브라우저로 사용하는 서비스 체계다.

```text
인터넷 = 연결망
웹 = 그 연결망 위에서 사용하는 서비스 중 하나
```

이메일이나 게임 통신도 인터넷을 사용하지만 모두 웹인 것은 아니다.

---

## BLOCK 02 · 브라우저

### LESSON 01 · 웹페이지를 읽고 보여주는 프로그램

**브라우저(browser)**는 웹 자원을 받아 해석하고 사용자에게 화면으로 보여주는 프로그램이다.

예:

```text
Chrome
Edge
Safari
Firefox
```

### LESSON 02 · 브라우저는 단순 그림 뷰어가 아니다

브라우저는:

```text
HTML 해석
CSS 계산
JavaScript 실행
네트워크 요청
보안 정책 적용
화면 그리기
사용자 입력 처리
```

같은 많은 일을 한다.

---

## BLOCK 03 · HTML

### LESSON 01 · 웹 문서의 구조

**HTML(HyperText Markup Language)**은 웹 문서의 내용과 구조를 표현하는 마크업 언어다.

한 줄 뜻:

> HTML = 웹페이지에 어떤 내용과 구조가 있는지 표현하는 언어

### LESSON 02 · 첫 HTML

```html
<h1>안녕하세요</h1>
<p>첫 웹페이지입니다.</p>
```

`h1`은 큰 제목 역할을 가진 요소다.

`p`는 문단을 나타내는 요소다.

---

## BLOCK 04 · tag와 element

### LESSON 01 · tag

```html
<p>안녕</p>
```

여기서:

```text
<p>  = 시작 태그
</p> = 종료 태그
```

### LESSON 02 · element

시작 태그, 내용, 종료 태그를 포함한 전체를 **요소(element)**라고 부른다.

```html
<p>안녕</p>
```

전체가 하나의 p element다.

`tag`와 `element`를 완전히 같은 말로 사용하지 않는 이유다.

---

## BLOCK 05 · attribute

### LESSON 01 · 요소에 추가 정보 붙이기

HTML 요소에 추가 정보를 주는 것을 **속성(attribute)**이라고 부른다.

```html
<a href="https://example.com">사이트</a>
```

여기서 `href`가 attribute 이름이다.

`https://example.com`이 attribute 값이다.

### LESSON 02 · 여러 속성

```html
<img src="cat.png" alt="고양이 사진">
```

```text
src = 이미지 파일 위치
alt = 이미지를 설명하는 대체 텍스트
```

---

## BLOCK 06 · 기본 HTML 문서

### LESSON 01 · 문서 뼈대

```html
<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <title>첫 페이지</title>
</head>
<body>
  <h1>안녕하세요</h1>
</body>
</html>
```

### LESSON 02 · head와 body

`head`에는 문서 자체의 정보와 설정이 들어간다.

`body`에는 사용자가 보게 될 주요 내용이 들어간다.

### LESSON 03 · charset

```html
<meta charset="utf-8">
```

은 이 HTML 문서의 문자 인코딩이 UTF-8이라는 정보를 브라우저에 알려준다.

TRACK 01의 인코딩 개념이 여기서 실제로 연결된다.

---

## BLOCK 07 · 제목과 문단

### LESSON 01 · heading

HTML에는 `h1`부터 `h6`까지 제목 요소가 있다.

```html
<h1>가장 큰 문서 제목</h1>
<h2>하위 제목</h2>
```

숫자는 단순 글씨 크기만 뜻하는 것이 아니라 문서 구조의 단계다.

### LESSON 02 · paragraph

문단은 `p`를 사용한다.

```html
<p>이 문장은 하나의 문단입니다.</p>
```

---

## BLOCK 08 · 목록

### LESSON 01 · 순서 없는 목록

```html
<ul>
  <li>사과</li>
  <li>바나나</li>
</ul>
```

`ul`은 순서 없는 목록, `li`는 목록 항목이다.

### LESSON 02 · 순서 있는 목록

```html
<ol>
  <li>회원가입</li>
  <li>로그인</li>
  <li>상품 선택</li>
</ol>
```

순서가 의미 있을 때 `ol`을 사용한다.

---

## BLOCK 09 · link

### LESSON 01 · 다른 자원으로 이동하기

링크는 `a` 요소로 만든다.

```html
<a href="/about">소개</a>
```

클릭하면 `href`가 가리키는 위치로 이동한다.

### LESSON 02 · 상대 URL과 절대 URL

```text
/about
```

은 현재 사이트 기준의 상대 위치다.

```text
https://example.com/about
```

은 전체 주소다.

---

## BLOCK 10 · image

### LESSON 01 · 이미지 표시

```html
<img src="profile.png" alt="민수 프로필 사진">
```

`src`는 이미지 위치다.

### LESSON 02 · alt가 왜 중요한가

이미지가 로드되지 않거나 화면을 보지 못하는 사용자가 **스크린 리더(screen reader)**를 사용할 때 대체 설명으로 사용할 수 있다.

`alt`는 접근성에 중요한 정보다.

---

## BLOCK 11 · semantic HTML

### LESSON 01 · div만 쓰면 안 되나

`div`는 의미가 없는 일반적인 묶음 요소다.

하지만 문서에는 역할이 있다.

```text
header
nav
main
article
section
footer
```

처럼 의미가 있는 요소를 사용할 수 있다.

### LESSON 02 · semantic

내용의 역할과 의미가 드러나는 HTML을 **시맨틱 HTML(semantic HTML)**이라고 부른다.

검색엔진, 접근성 도구, 개발자가 구조를 이해하는 데 도움이 된다.

---

## BLOCK 12 · form

### LESSON 01 · 사용자 입력 받기

로그인, 검색, 회원가입처럼 사용자의 입력을 받는 영역을 `form` 요소로 구성할 수 있다.

```html
<form>
  <input name="email">
  <button>로그인</button>
</form>
```

### LESSON 02 · input

```html
<input type="text" name="nickname">
```

`input`은 사용자가 값을 입력할 수 있는 요소다.

`type`에 따라 이메일, 비밀번호, 숫자 등 다양한 입력 형태를 만들 수 있다.

---

## BLOCK 13 · label

### LESSON 01 · 입력창의 의미를 알려주기

```html
<label for="email">이메일</label>
<input id="email" type="email">
```

`label`은 입력 필드가 무엇을 의미하는지 알려준다.

### LESSON 02 · 접근성

화면을 읽어주는 보조기술도 label을 사용해 입력 필드의 의미를 전달할 수 있다.

placeholder만 넣고 label을 생략하면 접근성과 사용성이 떨어질 수 있다.

---

## BLOCK 14 · button

### LESSON 01 · 사용자가 행동을 요청한다

```html
<button type="button">저장</button>
```

버튼은 사용자가 어떤 행동을 시작하도록 하는 상호작용 요소다.

### LESSON 02 · button과 link 구분

```text
button = 현재 화면에서 행동 수행
link = 다른 위치나 문서로 이동
```

이 역할을 명확히 구분하면 접근성과 사용자 경험이 좋아진다.

---

## BLOCK 15 · CSS

### LESSON 01 · 화면 스타일

**CSS(Cascading Style Sheets)**는 HTML 요소의 모양과 배치를 지정하는 언어다.

```css
p {
  font-size: 18px;
}
```

### LESSON 02 · selector, property, value

```css
p {
  font-size: 18px;
}
```

```text
p          = selector, 어떤 요소를 고를지
font-size  = property, 무엇을 바꿀지
18px       = value, 어떤 값으로 바꿀지
```

---

## BLOCK 16 · class selector

### LESSON 01 · 여러 요소에 같은 스타일

HTML:

```html
<p class="notice">중요</p>
```

CSS:

```css
.notice {
  font-weight: bold;
}
```

class 이름 앞에 `.`을 붙여 선택한다.

### LESSON 02 · id selector

```html
<div id="app"></div>
```

```css
#app {
  max-width: 800px;
}
```

id는 문서 안에서 고유한 식별을 위해 사용한다.

---

## BLOCK 17 · cascade

### LESSON 01 · 여러 CSS 규칙이 동시에 적용되면

같은 요소에 여러 규칙이 적용될 수 있다.

브라우저는 출처, 중요도, selector의 구체성, 작성 순서 등의 규칙을 사용해 최종 스타일을 결정한다.

이 과정이 CSS 이름의 **Cascading**과 연결된다.

### LESSON 02 · specificity

어떤 selector가 더 구체적인지를 나타내는 우선순위 개념을 **specificity**라고 부른다.

`!important`를 무조건 붙여 해결하면 나중에 더 복잡해질 수 있다.

---

## BLOCK 18 · box model

### LESSON 01 · 모든 요소는 상자처럼 배치된다

CSS에서 요소는 사각형 상자로 생각할 수 있다.

안쪽부터:

```text
content
padding
border
margin
```

으로 이루어진다.

### LESSON 02 · padding과 margin

```text
padding = 내용과 테두리 사이 안쪽 여백
margin = 요소 바깥쪽 여백
```

둘을 섞어 사용하면 레이아웃이 예상과 달라질 수 있다.

---

## BLOCK 19 · width와 box-sizing

### LESSON 01 · width 100px이면 실제 전체 크기도 100px인가

기본 box model에서는 width가 content 영역 크기를 나타내고 padding과 border가 추가될 수 있다.

### LESSON 02 · border-box

```css
* {
  box-sizing: border-box;
}
```

`border-box`를 사용하면 지정한 width 안에 padding과 border까지 포함해 계산한다.

레이아웃 계산이 직관적이어서 많이 사용한다.

---

## BLOCK 20 · display

### LESSON 01 · block

block 요소는 보통 한 줄의 가로 공간을 차지하며 다음 요소를 아래로 보낸다.

### LESSON 02 · inline

inline 요소는 글자처럼 같은 줄 안에 이어질 수 있다.

### LESSON 03 · inline-block

같은 줄에 배치되면서 width와 height를 지정할 수 있는 특성을 가진다.

---

## BLOCK 21 · position

### LESSON 01 · static

기본 문서 흐름에 따라 배치된다.

### LESSON 02 · relative

원래 위치를 기준으로 이동하거나 absolute 자식의 기준을 만들 때 사용한다.

### LESSON 03 · absolute

가장 가까운 위치 기준 조상 등을 기준으로 문서 흐름에서 벗어나 배치할 수 있다.

### LESSON 04 · fixed와 sticky

`fixed`는 viewport를 기준으로 고정할 수 있다.

`sticky`는 스크롤하다 특정 지점에서 붙는 동작을 만들 수 있다.

---

## BLOCK 22 · flexbox

### LESSON 01 · 한 방향의 배치

**Flexbox**는 요소들을 행 또는 열 방향으로 배치하고 정렬하는 CSS 레이아웃 시스템이다.

```css
.row {
  display: flex;
}
```

### LESSON 02 · main axis와 cross axis

Flexbox에는 주축과 교차축이 있다.

`flex-direction: row`라면 가로가 주축이다.

### LESSON 03 · 정렬

```text
justify-content = 주축 정렬
align-items = 교차축 정렬
```

방향을 먼저 확인해야 한다.

---

## BLOCK 23 · flex grow와 shrink

### LESSON 01 · 남는 공간 나누기

`flex-grow`는 남는 공간을 얼마나 가져갈지 정한다.

### LESSON 02 · 공간이 부족할 때

`flex-shrink`는 공간이 부족할 때 얼마나 줄어들지 정한다.

레이아웃이 갑자기 찌그러질 때 이 값을 확인한다.

---

## BLOCK 24 · grid

### LESSON 01 · 행과 열을 동시에 설계

**CSS Grid**는 행과 열을 이용해 2차원 레이아웃을 만드는 시스템이다.

```css
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
}
```

### LESSON 02 · fr 단위

`fr`은 사용 가능한 공간의 비율을 나타낸다.

```text
1fr 1fr = 절반씩
2fr 1fr = 2:1 비율
```

---

## BLOCK 25 · responsive design

### LESSON 01 · 화면 크기가 다르다

휴대폰, 태블릿, PC의 화면 폭은 다르다.

하나의 고정 크기 화면만 만들면 작은 화면에서 잘릴 수 있다.

여러 화면 크기에 맞춰 레이아웃을 조절하는 설계를 **반응형 디자인(responsive design)**이라고 부른다.

### LESSON 02 · media query

```css
@media (max-width: 600px) {
  .sidebar {
    display: none;
  }
}
```

특정 화면 조건에서 다른 CSS를 적용하는 기능을 **media query**라고 부른다.

---

## BLOCK 26 · viewport

### LESSON 01 · 브라우저에서 실제로 보이는 영역

**viewport**는 웹페이지가 브라우저에서 실제로 표시되는 화면 영역이다.

모바일에서는 다음 meta 태그를 자주 사용한다.

```html
<meta name="viewport" content="width=device-width, initial-scale=1">
```

브라우저에게 페이지 폭을 기기 화면 폭과 맞춰 해석하도록 알려준다.

---

## BLOCK 27 · 접근성

### LESSON 01 · 누구나 사용할 수 있게

장애 여부, 입력 장치, 시각 상태와 관계없이 더 많은 사용자가 서비스를 이용할 수 있도록 만드는 것을 **접근성(accessibility)**이라고 부른다.

### LESSON 02 · 키보드 사용

마우스 없이 `Tab`, `Enter`, 방향키 등으로 중요한 기능을 사용할 수 있어야 한다.

### LESSON 03 · 색만으로 정보를 전달하지 않는다

```text
빨강 = 오류
초록 = 성공
```

색만 사용하면 색을 구분하기 어려운 사용자가 정보를 놓칠 수 있다.

아이콘, 글자, 상태 설명을 함께 사용한다.

---

## BLOCK 28 · ARIA 맛보기

### LESSON 01 · 기본 HTML로 의미를 표현할 수 있으면 우선 사용

`button`이 필요한 곳에 `div`를 만들고 ARIA로 억지로 버튼처럼 만드는 것보다 실제 `button` 요소를 사용하는 것이 좋다.

### LESSON 02 · ARIA

**ARIA**는 보조기술에 웹 UI의 역할·상태를 추가로 전달하기 위한 속성 규칙이다.

기본 시맨틱 HTML로 표현할 수 없는 복잡한 위젯에서 도움이 된다.

---

## BLOCK 29 · DOM

### LESSON 01 · HTML을 객체 트리로 만든다

브라우저는 HTML 문서를 읽어 요소 관계를 메모리의 트리 구조로 만든다.

이 구조를 **DOM(Document Object Model)**이라고 부른다.

한 줄 뜻:

> DOM = HTML 문서를 JavaScript가 읽고 바꿀 수 있는 객체 트리로 표현한 구조

### LESSON 02 · node

DOM을 이루는 각 항목을 **노드(node)**라고 부른다.

요소 노드, 텍스트 노드 등이 있다.

---

## BLOCK 30 · JavaScript로 DOM 찾기

### LESSON 01 · querySelector

```html
<button id="save">저장</button>
```

```javascript
const button = document.querySelector("#save");
```

CSS selector를 사용해 DOM 요소를 찾는다.

### LESSON 02 · 텍스트 바꾸기

```javascript
button.textContent = "저장 완료";
```

JavaScript로 화면 내용을 바꿀 수 있다.

---

## BLOCK 31 · 이벤트

### LESSON 01 · 사용자 행동이 발생한다

버튼 클릭, 키 입력, 스크롤 같은 사건을 **이벤트(event)**라고 부른다.

### LESSON 02 · event listener

```javascript
button.addEventListener("click", function () {
  console.log("눌림");
});
```

이벤트가 발생했을 때 실행할 함수를 등록한다.

`listener`는 사건을 기다렸다가 반응하는 함수라고 이해하면 된다.

---

## BLOCK 32 · event object

### LESSON 01 · 어떤 사건이 발생했는지 정보 받기

이벤트 처리 함수는 **event object**를 받을 수 있다.

```javascript
button.addEventListener("click", function (event) {
  console.log(event.target);
});
```

`target`은 이벤트가 실제로 발생한 요소를 가리킨다.

---

## BLOCK 33 · bubbling

### LESSON 01 · 이벤트가 부모 방향으로 올라간다

버튼이 div 안에 있을 때 버튼 click 이벤트가 부모 요소 쪽으로 전파될 수 있다.

이를 **이벤트 버블링(event bubbling)**이라고 부른다.

```text
button
↑
div
↑
body
```

### LESSON 02 · event delegation

부모 하나에 이벤트 listener를 등록하고 자식들의 이벤트를 처리하는 방식을 **event delegation**이라고 부른다.

동적으로 추가되는 많은 목록 항목을 다룰 때 유용하다.

---

## BLOCK 34 · form submit

### LESSON 01 · submit 이벤트

사용자가 폼을 제출하면 `submit` 이벤트가 발생한다.

```javascript
form.addEventListener("submit", function (event) {
  event.preventDefault();
});
```

### LESSON 02 · preventDefault

브라우저가 원래 하려던 기본 동작을 막는 메서드다.

위 예에서는 폼 제출 후 기본 페이지 이동을 막고 JavaScript로 직접 처리할 수 있다.

---

## BLOCK 35 · 브라우저 렌더링

### LESSON 01 · HTML을 바로 픽셀로 그리는 것이 아니다

브라우저는 대략 다음 과정을 거친다.

```text
HTML → DOM
CSS → 스타일 계산
↓
레이아웃 계산
↓
그리기(paint)
↓
화면 합성(composite)
```

실제 브라우저 내부는 더 복잡하지만 이 큰 흐름부터 잡는다.

---

## BLOCK 36 · layout

### LESSON 01 · 요소의 크기와 위치 계산

브라우저가 각 요소의 너비, 높이, 위치를 계산하는 단계를 **layout**이라고 부른다.

### LESSON 02 · reflow

이미 계산한 레이아웃을 다시 계산해야 하는 상황을 흔히 **reflow**라고 부른다.

많은 요소의 크기를 반복해서 바꾸면 성능에 영향을 줄 수 있다.

---

## BLOCK 37 · paint와 composite

### LESSON 01 · paint

색, 글자, 그림자 같은 픽셀 내용을 그리는 작업을 **paint**라고 부른다.

### LESSON 02 · composite

여러 그려진 레이어를 최종 화면으로 합치는 작업을 **composite**라고 부른다.

transform이나 opacity 애니메이션은 경우에 따라 레이아웃 재계산 없이 합성 단계에서 효율적으로 처리될 수 있다.

---

## BLOCK 38 · DevTools

### LESSON 01 · 브라우저 개발자 도구

브라우저에는 웹페이지를 조사하는 **개발자 도구(DevTools)**가 있다.

주요 기능:

```text
Elements = DOM과 CSS 확인
Console = JavaScript 실행과 오류 확인
Network = HTTP 요청 확인
Sources = 코드와 breakpoint
Performance = 렌더링·실행 성능 분석
```

### LESSON 02 · Elements 패널

화면 요소를 클릭해 실제 적용된 CSS를 확인한다.

CSS를 임시로 바꾸며 문제 원인을 빠르게 시험할 수 있다.

---

## BLOCK 39 · Console

### LESSON 01 · JavaScript를 즉시 실행

Console에서:

```javascript
document.title
```

같은 코드를 바로 실행할 수 있다.

### LESSON 02 · 오류 읽기

JavaScript 오류가 나면 파일 이름과 줄 번호가 표시될 수 있다.

오류 메시지를 그대로 보존하고 해당 줄의 실제 코드와 비교한다.

---

## BLOCK 40 · Network 패널 맛보기

### LESSON 01 · 페이지가 어떤 파일을 받았는지 본다

Network 패널에서는 HTML, CSS, JavaScript, 이미지, API 요청을 볼 수 있다.

### LESSON 02 · 중요한 정보

```text
URL
method
status code
시간
request headers
response headers
response body
```

TRACK 06에서 이 항목을 훨씬 깊게 배운다.

---

## BLOCK 41 · localStorage와 sessionStorage

### LESSON 01 · 브라우저에 작은 데이터 저장

**localStorage**는 웹사이트가 브라우저에 문자열 데이터를 저장할 수 있는 기능이다.

브라우저를 닫아도 남을 수 있다.

### LESSON 02 · sessionStorage

**sessionStorage**는 보통 현재 탭 세션 동안 데이터를 저장한다.

탭을 닫으면 사라진다.

### LESSON 03 · 비밀값을 마음대로 저장하지 않는다

브라우저 JavaScript가 접근 가능한 저장소에 민감한 토큰을 넣으면 XSS 공격에 노출될 수 있다.

보안은 TRACK 09에서 다룬다.

---

## BLOCK 42 · SPA라는 말

### LESSON 01 · 페이지 전체 새로고침 없이 화면 바꾸기

**SPA(Single Page Application)**는 하나의 HTML 페이지를 중심으로 JavaScript가 필요한 화면을 동적으로 바꾸는 웹앱 구조다.

### LESSON 02 · 장점과 비용

장점:

```text
앱처럼 빠른 화면 전환
상태 유지 용이
```

비용:

```text
초기 JavaScript 용량
복잡한 상태 관리
SEO·접근성 고려
```

모든 사이트가 SPA여야 하는 것은 아니다.

---

## BLOCK 43 · component라는 생각

### LESSON 01 · 화면을 작은 부품으로 나누기

버튼, 카드, 검색창, 종목 행처럼 독립적으로 재사용할 수 있는 UI 단위를 **컴포넌트(component)**라고 부른다.

### LESSON 02 · 상태와 UI

```text
검색어 state 변경
↓
검색 결과 계산
↓
화면 다시 표시
```

현대 프론트엔드 라이브러리는 상태와 UI를 연결해 관리한다.

React, Vue, Svelte 같은 도구가 대표적이다.

---

## BLOCK 44 · 접근성 실전 체크

### LESSON 01 · 키보드

Tab만 사용해 모든 중요한 버튼과 입력창에 접근한다.

### LESSON 02 · focus

현재 키보드 입력 대상이 된 요소를 **focus**되었다고 말한다.

focus 표시를 CSS로 완전히 없애면 키보드 사용자가 현재 위치를 알기 어렵다.

### LESSON 03 · 글자 크기와 대비

작은 글씨와 낮은 색 대비는 읽기 어렵다.

특히 학습앱은 긴 시간 읽기 때문에 가독성이 매우 중요하다.

---

## BLOCK 45 · 반응형 실전 체크

### LESSON 01 · 360px

모바일 웹을 만들 때 360px 정도의 작은 화면에서도 가로 스크롤이 생기지 않는지 확인한다.

### LESSON 02 · 긴 글자

긴 URL이나 코드 문자열이 박스를 밀어내는지 확인한다.

필요하면:

```css
overflow-wrap: anywhere;
```

같은 처리를 고려한다.

### LESSON 03 · 터치 영역

버튼이 너무 작으면 손가락으로 누르기 어렵다.

중요한 터치 요소는 충분한 크기와 간격을 확보한다.

---

## BLOCK 46 · 성능 실전

### LESSON 01 · 너무 많은 DOM

화면에 수만 개 요소를 한 번에 만들면 브라우저의 레이아웃과 paint 비용이 커질 수 있다.

### LESSON 02 · virtualization

화면에 실제로 보이는 항목만 DOM에 렌더링하고 스크롤에 따라 교체하는 방식을 **virtualization**이라고 부른다.

긴 목록 앱에서 중요하다.

---

## BLOCK 47 · 핵심 용어 사전

| 용어 | 아주 쉬운 뜻 |
|---|---|
| web | 인터넷 위에서 문서와 자원을 연결해 사용하는 서비스 체계 |
| browser | 웹 자원을 받아 해석하고 화면에 보여주는 프로그램 |
| HTML | 웹페이지의 내용과 구조를 표현하는 언어 |
| tag | HTML 요소의 시작·끝 등을 표시하는 문법 |
| element | 태그와 내용을 포함한 HTML 구성 단위 |
| attribute | HTML 요소에 붙이는 추가 정보 |
| semantic HTML | 내용의 역할과 의미가 드러나는 HTML |
| CSS | HTML 요소의 모양과 배치를 지정하는 언어 |
| selector | CSS를 적용할 대상을 고르는 표현 |
| property | 바꿀 스타일 항목 |
| box model | content·padding·border·margin으로 요소 크기를 보는 모델 |
| Flexbox | 한 방향 배치에 강한 CSS 레이아웃 시스템 |
| Grid | 행과 열의 2차원 배치를 만드는 CSS 레이아웃 시스템 |
| responsive design | 화면 크기에 따라 레이아웃을 조정하는 설계 |
| viewport | 브라우저에서 실제 페이지가 보이는 화면 영역 |
| accessibility | 더 다양한 사용자가 서비스를 사용할 수 있게 만드는 성질 |
| DOM | HTML 문서를 객체 트리로 표현한 구조 |
| event | 사용자가 클릭하는 등 프로그램이 반응할 사건 |
| event listener | 특정 event가 발생하면 실행할 함수 |
| bubbling | event가 부모 요소 방향으로 전파되는 과정 |
| layout | 요소의 크기와 위치를 계산하는 단계 |
| paint | 화면의 글자·색·그림을 그리는 단계 |
| composite | 여러 레이어를 최종 화면으로 합치는 단계 |
| DevTools | 브라우저 내부 상태를 조사하는 개발자 도구 |
| SPA | 페이지 전체 새로고침 없이 동적으로 화면을 바꾸는 웹앱 구조 |
| component | 재사용 가능한 UI 부품 단위 |
| virtualization | 보이는 목록 항목만 실제로 렌더링하는 방식 |

---

## BLOCK 48 · TRACK 04 완료 기준

다음을 직접 할 수 있어야 한다.

- HTML 문서 구조를 만든다.
- semantic element를 선택한다.
- form, input, label, button을 올바르게 연결한다.
- CSS selector/property/value를 설명한다.
- box model로 크기 문제를 찾는다.
- Flexbox와 Grid 중 어떤 것을 선택할지 설명한다.
- 360px 모바일까지 반응형 UI를 만든다.
- 키보드와 스크린리더를 고려한 기본 접근성을 적용한다.
- DOM을 찾아 JavaScript로 수정한다.
- click/submit 이벤트를 처리한다.
- bubbling과 event delegation의 기본 원리를 설명한다.
- 브라우저의 layout/paint/composite 과정을 설명한다.
- DevTools로 CSS와 JavaScript 오류를 찾는다.
- 긴 목록에서 virtualization이 필요한 이유를 설명한다.

### TRACK 프로젝트 · 전문적인 모바일 학습앱 화면 만들기

다음 화면을 직접 만든다.

```text
상단: TRACK 이름과 진행률
중앙: 학습 본문
본문 안: 용어 카드, 코드 블록, 그림 영역
하단: 이전 LESSON / 다음 LESSON
오른쪽 또는 별도 화면: 전체 목차
```

필수 검증:

1. 배경과 글자 대비가 충분하다.
2. 360px에서 가로 넘침이 없다.
3. 768px 태블릿에서도 본문 폭이 너무 넓어지지 않는다.
4. 키보드만으로 이전/다음 버튼을 사용할 수 있다.
5. 버튼을 `div`가 아니라 실제 `button`으로 만든다.
6. 제목은 h1/h2/h3 구조를 가진다.
7. DevTools에서 적용 CSS와 DOM을 직접 확인한다.
8. JavaScript로 LESSON 이동을 구현한다.
9. 100개 LESSON 목록을 만든 뒤 성능을 관찰한다.

결과 화면만 예쁘게 만드는 것이 아니라 **왜 그 HTML 요소와 CSS 레이아웃을 선택했는지** 설명할 수 있어야 통과다.
