# TRACK 04 · 웹 화면과 브라우저

TRACK 01~03에서는 컴퓨터와 코드의 기본, 프로그램을 만드는 생각, 자료구조와 알고리즘을 배웠다.

이번 TRACK에서는 처음으로 **사용자가 실제로 보는 화면**을 만든다.

웹을 처음 배우면 HTML, CSS, JavaScript, DOM, event, responsive 같은 단어가 한꺼번에 튀어나온다. 여기서는 그 순서를 반대로 간다.

먼저 화면 하나를 보고 세 가지 질문을 한다.

```text
무엇이 화면에 있는가?
어떻게 보이는가?
사용자가 눌렀을 때 어떻게 움직이는가?
```

이 세 질문이 각각 HTML, CSS, JavaScript와 연결된다.

```text
HTML       = 화면에 무엇이 있고 어떤 구조인가
CSS        = 그것을 어떻게 보이게 할 것인가
JavaScript = 사용자의 행동에 어떻게 반응하고 상태를 바꿀 것인가
```

이 세 역할을 구분할 수 있으면 웹 화면의 절반은 이미 이해한 것이다.

---

## BLOCK 01 · 웹페이지는 브라우저가 읽어서 만드는 문서다

### LESSON 01 · 인터넷·웹·브라우저·HTML을 한 장의 화면에서 시작한다

#### 1. 웹과 인터넷은 같은 말이 아니다

휴대폰으로 웹사이트를 연다고 하자.

우리는 흔히 `인터넷 한다`와 `웹을 본다`를 같은 말처럼 사용한다. 일상에서는 큰 문제가 없지만 개발할 때는 구분하면 오류를 이해하기 쉬워진다.

**인터넷(Internet)**은 전 세계의 많은 컴퓨터 네트워크를 연결한 큰 연결망이다.

그 연결망 위에서는 여러 서비스가 동작할 수 있다.

```text
웹사이트
이메일
온라인 게임 통신
파일 전송
메신저 통신
```

**웹(World Wide Web)**은 그 인터넷을 이용해서 문서와 여러 자원을 주소로 연결하고 브라우저로 사용하는 서비스 체계다.

아주 쉽게:

```text
인터넷 = 도로망
웹 = 그 도로망을 이용하는 서비스 중 하나
```

비유가 완벽한 기술 설명은 아니지만 역할을 나누는 데 도움이 된다.

#### 2. 브라우저는 단순히 그림을 보여 주는 앱이 아니다

Chrome, Edge, Safari, Firefox 같은 프로그램을 **브라우저(browser)**라고 부른다.

브라우저가 하는 일은 생각보다 많다.

```text
서버에 웹 문서를 요청한다.
↓
HTML을 읽는다.
↓
CSS를 읽는다.
↓
JavaScript를 실행한다.
↓
화면에 요소를 배치하고 그린다.
↓
마우스·터치·키보드 입력을 받는다.
↓
필요하면 다시 서버와 통신한다.
```

따라서 브라우저는 `인터넷 그림 뷰어`가 아니라 작은 실행 환경에 가깝다.

#### 3. 가장 작은 HTML부터 본다

다음 파일을 `index.html`이라는 이름으로 저장했다고 하자.

```html
<h1>안녕하세요</h1>
<p>첫 웹페이지입니다.</p>
```

브라우저에서 열면 큰 제목과 문단이 보인다.

아직 문법 이름을 외우지 않는다.

그냥 다음처럼 읽는다.

```text
<h1>안녕하세요</h1>
→ "안녕하세요"를 중요한 큰 제목으로 표시한다.

<p>첫 웹페이지입니다.</p>
→ "첫 웹페이지입니다."를 하나의 문단으로 표시한다.
```

HTML은 **HyperText Markup Language**의 약자다.

초급에서는:

> HTML = 웹페이지에 어떤 내용과 구조가 있는지 표현하는 언어

라고 먼저 이해한다.

#### 4. tag와 element를 실제 코드에서 구분한다

```html
<p>안녕</p>
```

여기에는 세 부분이 있다.

```text
<p>   시작 tag
안녕  내용
</p>  종료 tag
```

`<p>`와 `</p>`처럼 꺾쇠로 감싼 표기를 **tag(태그)**라고 부른다.

시작 tag + 내용 + 종료 tag 전체는 하나의 **element(요소)**라고 부른다.

```text
태그 = 문법의 표지
요소 = 브라우저 문서 구조 안의 실제 한 덩어리
```

처음에는 둘을 같은 뜻처럼 써도 화면은 만들 수 있지만, 뒤에서 DOM을 배울 때 element라는 말이 중요해진다.

#### 5. HTML 요소는 상자 안에 다른 요소를 넣을 수 있다

```html
<div>
  <h1>상품</h1>
  <p>가격: 10,000원</p>
</div>
```

`div` 안에 `h1`, `p`가 들어 있다.

구조로 보면:

```text
div
├─ h1
└─ p
```

이런 부모-자식 구조가 만들어진다.

TRACK 03에서 배운 tree와 node 생각이 여기서 다시 나온다.

브라우저는 HTML을 읽어 문서 구조를 만들고, 이 구조가 뒤의 **DOM**과 연결된다.

#### 6. attribute는 요소에 추가 정보를 준다

링크를 만든다.

```html
<a href="https://example.com">사이트 열기</a>
```

여기서:

```text
href = 추가 정보의 이름
https://example.com = 그 값
```

이런 추가 정보를 **attribute(속성)**라고 부른다.

이미지:

```html
<img src="cat.png" alt="의자에 앉아 있는 고양이">
```

에서는:

```text
src = 어떤 이미지 파일을 사용할지
alt = 이미지를 볼 수 없을 때도 전달할 설명
```

이다.

#### 7. 기본 HTML 문서는 왜 더 길어질까

실제 HTML 파일에는 이런 뼈대를 많이 사용한다.

```html
<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <title>첫 페이지</title>
</head>
<body>
  <h1>안녕하세요</h1>
  <p>첫 웹페이지입니다.</p>
</body>
</html>
```

한 줄씩 사람말로 본다.

```text
<!doctype html>
→ 현대 HTML 문서라는 것을 브라우저에 알린다.

<html lang="ko">
→ HTML 문서의 가장 바깥 요소이며 주 언어가 한국어임을 알린다.

<head>
→ 문서 설정과 메타정보가 들어가는 영역

<meta charset="utf-8">
→ 문자 encoding으로 UTF-8을 사용한다.

<title>
→ 브라우저 탭 등에 사용할 문서 제목

<body>
→ 사용자가 주로 보게 되는 문서 내용
```

TRACK 01에서 배운 `UTF-8`이 실제 웹 문서에 다시 등장했다.

#### 8. 브라우저는 파일을 위에서 아래로 사진처럼 복사하지 않는다

HTML은 화면 좌표를 하나씩 적는 그림 파일이 아니다.

```html
<h1>제목</h1>
<p>설명</p>
<button>저장</button>
```

을 읽고:

```text
이것은 제목 역할
이것은 문단 역할
이것은 버튼 역할
```

처럼 의미 있는 요소 구조를 만든다.

그 다음 CSS를 적용해 크기와 배치를 정하고 실제 픽셀로 화면을 그린다.

그래서 HTML을 `글씨를 크게 만드는 언어`라고만 이해하면 안 된다.

HTML의 첫 역할은 **문서 구조와 의미**다.

#### 9. heading은 글씨 크기만을 위한 것이 아니다

```html
<h1>코딩 강의</h1>
<h2>HTML</h2>
<h3>태그</h3>
```

이 구조는:

```text
코딩 강의
└─ HTML
   └─ 태그
```

라는 문서의 제목 계층을 표현한다.

`h1`을 단지 `글자를 크게 해 주는 태그`라고 외우면 CSS와 HTML의 역할이 섞인다.

크기는 CSS로 바꿀 수 있다.

HTML heading은 **내용의 구조적 역할**을 나타내는 것이 먼저다.

#### 10. 문단과 목록으로 실제 문서를 만든다

```html
<h1>오늘 할 일</h1>
<p>퇴근 전 세 가지를 처리합니다.</p>

<ul>
  <li>전화하기</li>
  <li>메일 보내기</li>
  <li>보고서 저장하기</li>
</ul>
```

`ul`은 순서 없는 목록, `li`는 목록 항목이다.

순서가 중요하면:

```html
<ol>
  <li>회원가입</li>
  <li>로그인</li>
  <li>상품 주문</li>
</ol>
```

처럼 `ol`을 사용할 수 있다.

#### 11. HTML이 틀려도 브라우저가 어느 정도 복구할 수 있어 더 헷갈릴 수 있다

브라우저는 잘못된 HTML을 어느 정도 보정해서 화면을 보여 줄 때가 있다.

그래서 화면이 나온다고 HTML이 완벽하다는 뜻은 아니다.

예를 들어 tag를 잘못 닫아도 브라우저가 예상해서 구조를 만들어 버릴 수 있다.

이때 DevTools의 Elements 패널을 보면 실제 브라우저가 만든 구조를 확인할 수 있다.

#### 12. 초보자가 자주 하는 실수

**실수 1 — 인터넷과 웹을 완전히 같은 개념이라고 생각한다.**

웹은 인터넷을 이용하는 서비스 중 하나다.

**실수 2 — HTML을 디자인 언어라고 생각한다.**

HTML은 먼저 내용과 구조를 표현한다. 디자인은 주로 CSS 역할이다.

**실수 3 — h1을 글씨 크게 만드는 용도로만 사용한다.**

제목의 의미와 계층이 먼저다.

**실수 4 — 파일이 브라우저에 보이면 HTML 구조도 항상 올바르다고 생각한다.**

브라우저의 오류 복구 때문에 잘못된 구조가 숨을 수 있다.

#### 13. 직접 만들어 본다

`index.html`에 다음 구조를 직접 적는다.

```html
<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <title>내 첫 페이지</title>
</head>
<body>
  <h1>내 첫 페이지</h1>
  <p>웹을 배우고 있습니다.</p>

  <h2>오늘 할 일</h2>
  <ul>
    <li>HTML 읽기</li>
    <li>코드 바꾸기</li>
    <li>브라우저에서 확인하기</li>
  </ul>
</body>
</html>
```

다음 순서로 바꾼다.

```text
1. 제목 글자 바꾸기
2. 문단 하나 추가
3. 목록 항목 하나 추가
4. 일부러 종료 tag 하나를 망가뜨려 보기
5. DevTools Elements에서 실제 구조 확인
```

#### 14. 책을 덮고 확인한다

1. 인터넷과 웹의 차이를 설명하라.
2. 브라우저가 HTML을 받은 뒤 하는 일을 큰 순서로 말해 보라.
3. tag와 element는 어떻게 다른가?
4. attribute는 무엇이며 `href` 예를 들어라.
5. head와 body는 각각 어떤 역할인가?
6. heading을 글자 크기로만 생각하면 안 되는 이유는 무엇인가?

---

## BLOCK 02 · 사용자가 보고 누르고 입력할 수 있는 의미 있는 HTML을 만든다

### LESSON 01 · link·image·form·input·label·button·semantic HTML·접근성을 하나의 실제 화면으로 배운다

#### 1. 이제 단순 문서에서 실제 앱 화면으로 간다

회원가입 화면을 만든다고 하자.

필요한 것은:

```text
서비스 제목
이메일 입력창
비밀번호 입력창
가입 버튼
이용약관 링크
프로필 이미지
```

이 요소들을 아무 `div`와 글자로만 만들 수도 있다.

하지만 HTML에는 각 역할을 표현하는 요소가 있다.

역할이 맞는 요소를 사용하면 브라우저, 키보드 사용자, 화면 읽기 도구, 검색엔진, 개발자가 구조를 더 잘 이해할 수 있다.

#### 2. link는 다른 위치로 이동한다

```html
<a href="/terms">이용약관 보기</a>
```

`a` element는 링크를 표현한다.

`href`는 이동할 위치다.

```text
/terms
→ 현재 사이트 기준의 경로

https://example.com/terms
→ 전체 주소
```

링크의 핵심 동작은 **어딘가로 이동하는 것**이다.

#### 3. button은 행동을 실행한다

```html
<button type="button">저장</button>
```

버튼은 현재 화면에서 어떤 **행동을 시작**할 때 사용한다.

초보자가 흔히 모든 클릭 가능한 것을 `div`로 만들고 JavaScript를 붙인다.

하지만 역할에 맞는 기본 요소를 쓰면 키보드 조작, 접근성, 기본 동작을 더 쉽게 얻을 수 있다.

처음 기준:

```text
다른 위치로 이동
→ link

현재 기능 실행
→ button
```

물론 실제 앱에서는 디자인과 동작에 따라 세부 판단이 필요하다.

#### 4. image는 그림만 보여 주는 것이 끝이 아니다

```html
<img src="profile.png" alt="민수의 프로필 사진">
```

`src`는 이미지 파일 위치다.

`alt`는 이미지의 의미를 설명하는 대체 텍스트다.

왜 필요할까?

```text
이미지 다운로드 실패
시각적으로 이미지를 보기 어려운 사용자
화면 읽기 도구 사용
```

같은 상황에서도 의미를 전달하기 위해서다.

장식용 이미지처럼 의미가 없는 경우에는 alt 사용 방식이 달라질 수 있다. 중요한 것은 `alt에 파일 이름을 아무렇게나 넣는 것`이 아니라 **이미지가 전달하는 의미가 무엇인지 생각하는 것**이다.

#### 5. form은 여러 입력을 하나의 제출 흐름으로 묶는다

로그인 화면:

```html
<form>
  <label for="email">이메일</label>
  <input id="email" name="email" type="email">

  <label for="password">비밀번호</label>
  <input id="password" name="password" type="password">

  <button type="submit">로그인</button>
</form>
```

`form`은 사용자 입력을 묶어 제출하는 문맥을 표현한다.

#### 6. input의 type은 단순 디자인 옵션이 아니다

```html
<input type="email">
<input type="password">
<input type="number">
```

브라우저는 type을 참고해 입력 방식, 기본 검증, 모바일 키보드 등을 다르게 제공할 수 있다.

예를 들어 모바일에서 email input은 `@`를 입력하기 편한 키보드를 보여 줄 수 있다.

하지만 `type="email"`만 있다고 서버가 이메일을 완벽히 검증해 주는 것은 아니다.

프론트엔드 입력 도움과 서버의 신뢰할 수 있는 validation은 역할이 다르다.

#### 7. label은 입력창이 무엇인지 알려 준다

```html
<label for="email">이메일</label>
<input id="email" type="email">
```

`for="email"`과 `id="email"`이 연결된다.

사용자가 label을 눌러 input에 focus가 이동할 수 있고, 보조기술도 입력 의미를 이해하기 쉬워진다.

placeholder만 사용하는 것과는 다르다.

```html
<input placeholder="이메일">
```

placeholder는 입력을 시작하면 사라질 수 있고, 항상 label을 대체하기 적합한 것은 아니다.

#### 8. name은 form 데이터에서 중요한 이름이 된다

```html
<input name="email">
```

form이 데이터를 전송할 때 `email`이라는 이름으로 값을 보낼 수 있다.

나중에 HTTP form data에서:

```text
email=user@example.com
```

같은 데이터가 만들어질 수 있다.

HTML이 네트워크 TRACK과 연결된다.

#### 9. checkbox와 radio도 의미가 다르다

여러 개를 동시에 선택 가능:

```html
<label>
  <input type="checkbox" name="agree">
  약관에 동의합니다
</label>
```

여러 선택지 중 하나를 고르는 상황:

```html
<label><input type="radio" name="plan" value="basic"> 기본</label>
<label><input type="radio" name="plan" value="pro"> 프로</label>
```

radio는 같은 `name`을 가진 항목을 하나의 선택 그룹으로 만들 수 있다.

#### 10. semantic HTML은 역할을 코드에 드러낸다

다음 화면을 생각한다.

```text
상단 로고와 메뉴
본문
상품 기사
하단 정보
```

전부 div로 만들 수 있다.

```html
<div>...</div>
<div>...</div>
<div>...</div>
```

하지만 HTML에는 의미 있는 구조 요소가 있다.

```html
<header>...</header>
<nav>...</nav>
<main>...</main>
<article>...</article>
<footer>...</footer>
```

이렇게 **내용의 역할이 드러나는 HTML**을 semantic HTML이라고 부른다.

중요한 것은 `semantic tag를 많이 쓰면 무조건 좋은 코드`가 아니라 **실제 내용 역할에 맞는 요소를 고르는 것**이다.

#### 11. 접근성은 나중에 추가하는 장식이 아니다

**접근성(accessibility)**은 다양한 능력과 환경의 사용자가 제품을 사용할 수 있도록 만드는 것이다.

예:

```text
키보드만 사용하는 사용자
스크린 리더를 사용하는 사용자
색을 구분하기 어려운 사용자
큰 글씨가 필요한 사용자
일시적으로 한 손만 사용할 수 있는 사용자
```

웹 요소의 의미, label, focus 순서, 색 대비, 텍스트 크기 등이 모두 영향을 준다.

#### 12. 키보드만으로 회원가입을 해 본다

마우스를 쓰지 않고 Tab 키만 사용해 본다.

```text
이메일 입력
↓ Tab
비밀번호 입력
↓ Tab
가입 버튼
```

focus가 보이지 않거나 순서가 이상하면 사용하기 어렵다.

CSS로 `outline: none`을 무조건 제거하면 focus 표시가 사라질 수 있다.

디자인 때문에 접근성을 망가뜨리는 대표적인 실수다.

#### 13. button type을 모르면 예상치 못한 form 제출이 생길 수 있다

form 안의 button은 기본 동작이 submit이 될 수 있다.

단순히 팝업만 열려는 버튼이라면:

```html
<button type="button">도움말</button>
```

처럼 목적을 명확히 한다.

제출 버튼:

```html
<button type="submit">가입</button>
```

#### 14. form 제출 흐름을 아직 JavaScript 없이도 이해한다

```text
사용자가 값을 입력
↓
submit 버튼
↓
form 제출
↓
브라우저가 데이터를 정해진 방식으로 보냄
↓
서버 응답
```

뒤에서 JavaScript를 사용하면 기본 제출을 가로채 API를 호출하는 SPA 방식도 볼 수 있다.

하지만 먼저 HTML 자체의 역할을 알아야 JavaScript가 무엇을 바꾸고 있는지 이해할 수 있다.

#### 15. 실제 회원가입 HTML을 만든다

```html
<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <title>회원가입</title>
</head>
<body>
  <header>
    <h1>회원가입</h1>
  </header>

  <main>
    <form>
      <p>
        <label for="email">이메일</label>
        <input id="email" name="email" type="email" required>
      </p>

      <p>
        <label for="password">비밀번호</label>
        <input id="password" name="password" type="password" required>
      </p>

      <label>
        <input type="checkbox" name="agree" required>
        이용약관에 동의합니다
      </label>

      <button type="submit">가입하기</button>
    </form>

    <p><a href="/terms">이용약관 읽기</a></p>
  </main>
</body>
</html>
```

#### 16. 일부러 잘못 만들어 비교한다

나쁜 방향:

```html
<div onclick="save()">가입하기</div>
<input placeholder="이메일">
<img src="logo.png">
```

질문한다.

```text
왜 button이 아닌 div를 썼지?
입력의 label은 어디 있지?
이미지에 의미 있는 대체 설명이 필요한가?
키보드로 조작 가능한가?
```

이렇게 코드가 실행되는지만 보지 않고 **사용 의미**를 검토한다.

#### 17. 책을 덮고 확인한다

1. link와 button의 기본 역할 차이는 무엇인가?
2. img의 alt는 왜 필요한가?
3. label과 input을 연결하는 이유는 무엇인가?
4. input type은 어떤 도움을 줄 수 있는가?
5. semantic HTML은 무엇을 표현하려는가?
6. 접근성 검사를 마우스 없이 해 볼 수 있는 간단한 방법은 무엇인가?

---

## BLOCK 03 · CSS는 HTML 요소가 어떻게 보이고 배치될지 계산한다

### LESSON 01 · selector·cascade·box model·display·position을 한 카드 디자인으로 배운다

#### 1. HTML만으로도 내용은 보이지만 원하는 디자인은 아니다

HTML:

```html
<article class="card">
  <h2 class="card-title">프라임내과</h2>
  <p class="card-note">9월 18일 점심 약속</p>
  <button class="card-button">상세 보기</button>
</article>
```

구조와 의미는 있다.

하지만 크기, 여백, 배경, 배치는 기본값이다.

이때 **CSS(Cascading Style Sheets)**로 스타일을 지정한다.

#### 2. 가장 작은 CSS 규칙부터 읽는다

```css
.card-title {
  font-size: 24px;
}
```

세 부분으로 나눈다.

```text
.card-title
→ 어떤 요소를 고를지: selector

font-size
→ 무엇을 바꿀지: property

24px
→ 어떤 값으로 할지: value
```

사람말:

> class가 card-title인 요소의 글자 크기를 24px로 만들어라.

#### 3. class는 여러 요소에 재사용할 수 있다

HTML:

```html
<p class="muted">보조 설명 1</p>
<p class="muted">보조 설명 2</p>
```

CSS:

```css
.muted {
  opacity: 0.7;
}
```

두 요소 모두 같은 스타일을 받을 수 있다.

`class`는 단순히 CSS를 위한 것만은 아니지만 스타일링에 매우 많이 사용한다.

#### 4. id selector는 `#`을 사용한다

```html
<div id="app"></div>
```

```css
#app {
  max-width: 800px;
}
```

id는 문서 내에서 특정 요소를 고유하게 식별하는 용도로 사용할 수 있다.

스타일을 전부 id로만 작성하면 재사용이 어려워질 수 있어 class 중심 설계가 흔하다.

#### 5. 여러 CSS가 같은 요소를 바꾸려고 하면 어떻게 될까

```css
p {
  color: black;
}

.note {
  color: blue;
}
```

```html
<p class="note">메모</p>
```

두 규칙이 모두 같은 요소에 해당한다.

브라우저는 어떤 규칙을 최종 적용할지 결정해야 한다.

CSS의 `Cascading`이 이 충돌 해결 규칙과 연결된다.

#### 6. specificity는 selector의 구체성에 관한 규칙이다

```css
p { color: black; }
.note { color: blue; }
#important { color: red; }
```

selector 종류에 따라 우선순위 계산이 달라진다.

처음에는 숫자 공식을 외우기보다:

> CSS는 단순히 마지막 줄만 무조건 적용되는 것이 아니라 출처·중요도·selector 구체성·순서 등 규칙으로 최종값을 계산한다.

라고 이해한다.

#### 7. `!important`로 모든 충돌을 덮는 습관을 피한다

```css
color: red !important;
```

은 일반적인 우선순위보다 강하게 적용할 수 있다.

문제가 생길 때마다 `!important`를 붙이면 나중에 더 강한 규칙을 계속 만들어야 할 수 있다.

먼저:

```text
어떤 selector가 적용되고 있나?
규칙이 어디서 왔나?
왜 덮였나?
```

를 DevTools에서 확인한다.

#### 8. 모든 요소를 상자처럼 생각한다 — box model

CSS 레이아웃에서 요소를 상자로 생각한다.

```text
┌────────────────────────────┐
│          margin            │
│  ┌──────────────────────┐  │
│  │        border        │  │
│  │  ┌────────────────┐  │  │
│  │  │    padding     │  │  │
│  │  │  ┌──────────┐  │  │  │
│  │  │  │ content  │  │  │  │
│  │  │  └──────────┘  │  │  │
│  │  └────────────────┘  │  │
│  └──────────────────────┘  │
└────────────────────────────┘
```

안쪽부터:

```text
content = 실제 내용 영역
padding = 내용과 테두리 사이의 안쪽 여백
border = 테두리
margin = 다른 요소와 떨어지는 바깥 여백
```

#### 9. padding과 margin을 직접 비교한다

```css
.card {
  padding: 20px;
  margin: 20px;
  border: 1px solid #999;
}
```

padding을 늘리면 카드 내부 글자와 테두리 사이가 넓어진다.

margin을 늘리면 카드 자체와 주변 요소 사이가 멀어진다.

UI가 답답한데 무조건 `margin`만 늘리면 내부 밀도는 그대로일 수 있다.

#### 10. width가 생각보다 커지는 이유 — box-sizing

```css
.card {
  width: 300px;
  padding: 20px;
  border: 2px solid;
}
```

기본 box model에서는 width가 content 영역을 뜻해 padding과 border가 더해질 수 있다.

많은 프로젝트에서:

```css
* {
  box-sizing: border-box;
}
```

를 사용한다.

이렇게 하면 지정한 width 안에 padding과 border를 포함해 계산해 레이아웃을 이해하기 쉬워진다.

#### 11. display는 요소가 배치 흐름에 참여하는 방식을 바꾼다

대표적으로:

```text
block
inline
inline-block
flex
grid
none
```

등이 있다.

초급에서는 먼저 block과 inline을 구분한다.

`block` 요소는 보통 가로 줄을 크게 차지하며 다음 요소를 아래로 보낸다.

`inline` 요소는 글자처럼 같은 줄 안에서 이어질 수 있다.

#### 12. `display: none`은 화면에서 숨기고 레이아웃에서도 뺀다

```css
.hidden {
  display: none;
}
```

요소를 보이지 않게 한다.

하지만 `안 보이게 한다 = 보안상 접근할 수 없다`는 뜻은 아니다.

관리자 버튼을 CSS로 숨겼다고 권한 보호가 되는 것이 아니다.

보안은 서버에서 검사해야 한다.

#### 13. position은 기본 흐름 밖의 배치를 다룬다

기본 `static`은 문서 흐름대로 배치된다.

`relative`는 자신의 원래 위치와 관련된 이동이나 absolute 자식의 기준을 만드는 데 사용할 수 있다.

`absolute`는 기준이 되는 조상을 바탕으로 특정 위치에 배치할 수 있다.

`fixed`는 viewport에 고정되는 UI에 사용될 수 있다.

`sticky`는 스크롤하다 특정 지점에서 붙는 헤더 등에 사용될 수 있다.

#### 14. absolute를 모든 레이아웃에 쓰면 왜 깨질까

모든 요소를:

```css
position: absolute;
left: 120px;
top: 300px;
```

처럼 좌표로 박아 놓으면 화면 크기가 바뀌거나 글자가 길어질 때 서로 겹칠 수 있다.

일반적인 문서/앱 레이아웃은 먼저 flex나 grid 같은 흐름 기반 레이아웃으로 만들고, absolute는 정말 필요한 겹침/고정 위치에 제한적으로 사용하는 것이 좋다.

#### 15. 카드 스타일을 완성한다

```css
* {
  box-sizing: border-box;
}

.card {
  width: min(100%, 560px);
  padding: 20px;
  margin: 16px auto;
  border: 1px solid #2a2f38;
  border-radius: 16px;
}

.card-title {
  margin: 0 0 8px;
  font-size: 24px;
}

.card-note {
  margin: 0 0 16px;
  line-height: 1.6;
}

.card-button {
  min-height: 44px;
  padding: 0 16px;
}
```

코드를 덩어리로 읽는다.

```text
전체 box 크기 계산 방식을 통일
↓
카드 너비와 내부 여백
↓
제목 간격/크기
↓
본문 읽기 간격
↓
버튼 터치 크기
```

#### 16. 초보자가 자주 하는 실수

- CSS가 안 먹으면 바로 `!important`부터 붙인다.
- margin과 padding을 구분하지 않는다.
- width만 보고 실제 padding/border 크기를 잊는다.
- 모든 배치를 absolute 좌표로 해결한다.
- 화면에서 숨겼으니 권한도 막혔다고 생각한다.
- selector가 어떤 요소를 실제로 선택하는지 확인하지 않는다.

#### 17. DevTools에서 계산된 스타일을 확인한다

Elements에서 요소를 선택하고 Styles/Computed 영역을 본다.

```text
어떤 CSS 규칙이 적용됐나?
어떤 규칙이 줄이 그어져 덮였나?
최종 width/padding/margin은 얼마인가?
```

CSS를 감으로 고치는 대신 브라우저가 실제로 계산한 결과를 확인한다.

#### 18. 책을 덮고 확인한다

1. selector/property/value를 한 CSS 규칙으로 설명하라.
2. class와 id의 큰 차이를 말해 보라.
3. cascade가 필요한 이유는 무엇인가?
4. padding과 margin을 카드 예로 설명하라.
5. box-sizing: border-box는 왜 편한가?
6. absolute를 전체 레이아웃에 남용하면 어떤 문제가 생길 수 있는가?

---

## BLOCK 04 · 화면 크기가 달라도 무너지지 않는 레이아웃을 만든다

### LESSON 01 · flex·grid·viewport·media query·반응형을 실제 모바일 화면으로 배운다

#### 1. 개발자 PC에서 예쁘다고 끝이 아니다

웹 화면은 다양한 크기에서 열린다.

```text
360px 폭의 작은 휴대폰
태블릿
노트북
큰 모니터
브라우저 창을 반으로 줄인 상태
```

한 화면 크기에서만 맞으면 실제 사용에서는 깨질 수 있다.

**반응형 디자인(responsive design)**은 화면과 사용 환경에 따라 레이아웃이 자연스럽게 적응하도록 만드는 접근이다.

#### 2. 고정 width만 사용하면 작은 화면에서 넘친다

```css
.card {
  width: 900px;
}
```

360px 화면에서는 카드가 화면 밖으로 나간다.

대신:

```css
.card {
  width: 100%;
  max-width: 900px;
}
```

처럼 작은 화면에서는 줄어들고 큰 화면에서는 최대 폭을 제한할 수 있다.

더 단순히:

```css
width: min(100%, 900px);
```

같은 표현도 사용할 수 있다.

#### 3. viewport는 현재 보이는 브라우저 영역이다

반응형 CSS에서 **viewport**는 사용자가 현재 보고 있는 브라우저의 표시 영역을 말한다.

모바일 HTML에서는 다음 설정을 자주 본다.

```html
<meta name="viewport" content="width=device-width, initial-scale=1">
```

초급에서는:

> 모바일 브라우저가 페이지의 CSS 폭을 기기 화면 폭에 맞춰 자연스럽게 해석하도록 돕는 기본 설정

이라고 이해한다.

#### 4. flex는 한 방향 배치를 쉽게 만든다

버튼 세 개를 가로로 배치한다.

```html
<div class="actions">
  <button>이전</button>
  <button>저장</button>
  <button>다음</button>
</div>
```

```css
.actions {
  display: flex;
  gap: 8px;
}
```

기본적으로 가로 방향으로 놓인다.

`gap`은 항목 사이 간격이다.

#### 5. main axis와 cross axis를 감으로 이해한다

```css
.actions {
  display: flex;
  flex-direction: row;
}
```

row이면 주 방향(main axis)은 가로다.

```css
justify-content: space-between;
```

은 주 방향 배치를 조정한다.

```css
align-items: center;
```

는 반대 축의 정렬을 조정한다.

영어를 통째로 외우지 말고:

```text
flex-direction으로 주 방향 결정
justify-content로 주 방향 배치
align-items로 반대 방향 정렬
```

을 그림과 함께 기억한다.

#### 6. column으로 세로 흐름을 만든다

```css
.form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
```

입력창들을 세로로 쌓는다.

```text
이메일
↓ 12px
비밀번호
↓ 12px
버튼
```

#### 7. `flex: 1`은 남는 공간을 나눠 가질 수 있다

버튼 둘이 같은 폭을 가지게 한다.

```css
.actions button {
  flex: 1;
}
```

가용 공간을 두 버튼이 나눠 가진다.

하지만 아주 좁은 화면에서 글자가 너무 길면 여전히 문제가 생길 수 있다. 실제 콘텐츠 길이로 테스트한다.

#### 8. flex-wrap은 공간이 부족하면 다음 줄로 보낼 수 있다

태그 목록:

```css
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
```

화면이 좁아지면 한 줄에 다 넣으려 하지 않고 다음 줄로 넘어갈 수 있다.

#### 9. grid는 행과 열을 함께 설계하기 좋다

상품 카드 목록:

```css
.products {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
```

세 열로 만든다.

```text
[카드][카드][카드]
[카드][카드][카드]
```

#### 10. 자동으로 가능한 열 수를 계산할 수도 있다

```css
.products {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}
```

각 카드가 최소 220px 정도는 필요하고, 공간이 되면 여러 열로 늘어난다.

큰 화면:

```text
[카드][카드][카드][카드]
```

좁은 화면:

```text
[카드]
[카드]
```

으로 자연스럽게 변할 수 있다.

#### 11. media query로 특정 폭에서 규칙을 바꾼다

```css
.page {
  padding: 24px;
}

@media (max-width: 600px) {
  .page {
    padding: 12px;
  }
}
```

600px 이하에서는 좌우 여백을 줄인다.

media query를 `휴대폰이면 무조건 600px` 같은 절대 진리로 외우지 않는다.

콘텐츠가 실제로 깨지는 지점을 보고 breakpoint를 정하는 것이 좋다.

#### 12. responsive는 단순히 폭만 줄이는 것이 아니다

큰 화면에서는:

```text
왼쪽 목록 | 오른쪽 상세
```

두 칸이 편할 수 있다.

작은 화면에서는:

```text
목록
↓ 클릭
상세 전체 화면
```

한 칸 흐름이 더 좋을 수 있다.

반응형은 단순 축소가 아니라 **사용 가능한 공간에 맞춰 구조를 재배치**하는 것이다.

#### 13. 텍스트 크기와 줄 길이도 반응형의 일부다

너무 긴 한 줄은 읽기 어렵다.

```css
.article {
  max-width: 70ch;
  line-height: 1.7;
}
```

처럼 본문 폭을 제한할 수 있다.

사용자가 시스템 글자 크기를 키웠을 때도 버튼과 카드가 잘리는지 확인해야 한다.

#### 14. `overflow-x`를 숨겨 문제를 감추지 않는다

화면이 옆으로 삐져나온다고:

```css
body {
  overflow-x: hidden;
}
```

만 붙이면 원인이 숨을 수 있다.

먼저 어떤 요소가 화면보다 넓은지 찾는다.

```text
고정 width?
긴 URL/코드?
absolute 위치?
min-width?
padding + width?
```

원인을 수정한 뒤 정말 필요한 overflow 정책을 정한다.

#### 15. 360px 모바일 검사를 실제로 한다

브라우저 DevTools의 device toolbar에서 폭을 360px 정도로 줄인다.

확인:

```text
가로 스크롤이 생기지 않는가?
버튼이 화면 밖으로 나가지 않는가?
글자가 잘리지 않는가?
입력창이 충분히 큰가?
터치 대상 간격이 너무 좁지 않은가?
```

그리고 태블릿·데스크톱도 본다.

#### 16. 카드 목록을 반응형으로 완성한다

```css
.page {
  width: min(100%, 1200px);
  margin: 0 auto;
  padding: 24px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.card {
  min-width: 0;
  padding: 18px;
}

@media (max-width: 600px) {
  .page {
    padding: 12px;
  }
}
```

#### 17. 책을 덮고 확인한다

1. 반응형 디자인이 필요한 이유를 두 기기 예로 설명하라.
2. width: 900px 고정이 모바일에서 위험한 이유는 무엇인가?
3. flex와 grid를 각각 어떤 배치에 쓰기 자연스러운가?
4. justify-content와 align-items는 어느 방향을 기준으로 생각하는가?
5. media query의 breakpoint를 단순 기기 이름으로만 정하면 안 되는 이유는 무엇인가?
6. overflow-x: hidden으로 바로 숨기기 전에 무엇을 해야 하는가?

---

## BLOCK 05 · JavaScript는 화면 구조를 찾아 상태를 바꾸고 사용자 행동에 반응한다

### LESSON 01 · DOM·querySelector·event·state를 카운터 앱으로 한 단계씩 배운다

#### 1. HTML과 CSS만으로는 버튼 숫자가 자동으로 바뀌지 않는다

다음 화면을 만든다.

```html
<h1>카운터</h1>
<p id="count">0</p>
<button id="plus">+1</button>
```

HTML은 구조를 만들었다.

CSS는 모양을 만들 수 있다.

하지만 사용자가 버튼을 눌렀을 때 숫자를 1씩 올리는 **동작**이 아직 없다.

이런 동적 행동을 JavaScript로 구현할 수 있다.

#### 2. 브라우저는 HTML을 DOM이라는 객체 구조로 만든다

**DOM(Document Object Model)**은 브라우저가 HTML 문서를 프로그램에서 다룰 수 있도록 만든 객체 기반의 tree 구조다.

HTML:

```html
<body>
  <h1>카운터</h1>
  <p id="count">0</p>
  <button id="plus">+1</button>
</body>
```

DOM을 단순 그림으로 보면:

```text
document
└─ html
   └─ body
      ├─ h1
      ├─ p#count
      └─ button#plus
```

TRACK 03의 tree와 node가 다시 나타난다.

#### 3. JavaScript에서 원하는 요소를 찾는다

```javascript
const countElement = document.querySelector("#count");
```

사람말:

```text
document에서
CSS selector #count에 맞는 요소를 찾고
countElement라는 이름으로 기억한다.
```

버튼도 찾는다.

```javascript
const plusButton = document.querySelector("#plus");
```

#### 4. 요소를 찾지 못할 수도 있다

HTML에 `id="count"`가 없으면:

```javascript
document.querySelector("#count")
```

결과가 `null`일 수 있다.

따라서 `DOM 요소는 항상 존재한다`고 무조건 가정하면 오류가 생길 수 있다.

#### 5. 화면 글자를 JavaScript로 바꾼다

```javascript
countElement.textContent = "1";
```

브라우저 화면에서 0이 1로 바뀐다.

흐름:

```text
JavaScript가 DOM node를 찾음
↓
그 node의 textContent 변경
↓
브라우저가 변경된 화면을 다시 보여 줌
```

#### 6. event는 어떤 일이 일어났다는 신호다

사용자가 버튼을 누르는 사건을 **event(이벤트)**라고 부른다.

대표적으로:

```text
click
input
change
submit
keydown
```

등이 있다.

특정 event가 생겼을 때 실행할 코드를 등록할 수 있다.

#### 7. click event에 함수를 연결한다

```javascript
plusButton.addEventListener("click", () => {
  console.log("버튼 클릭");
});
```

사람말:

```text
plusButton에서 click event가 생기면
이 함수를 실행해 줘.
```

함수를 나중에 실행하도록 전달하는 구조이므로 뒤의 callback 개념과도 연결된다.

#### 8. 화면에 보이는 숫자와 프로그램의 상태를 구분한다

숫자를 계속 올리려면 현재 값이 필요하다.

```javascript
let count = 0;
```

여기서 `count`는 프로그램이 기억하는 **state(상태)**다.

버튼 클릭:

```javascript
plusButton.addEventListener("click", () => {
  count = count + 1;
  countElement.textContent = String(count);
});
```

흐름:

```text
클릭
↓
state count 증가
↓
DOM 화면을 새 state에 맞게 갱신
```

#### 9. state와 화면이 서로 다르면 버그가 된다

실수:

```javascript
let count = 0;

plusButton.addEventListener("click", () => {
  count = count + 1;
  // 화면 갱신을 빼먹음
});
```

내부 count는 1,2,3으로 바뀌지만 화면은 계속 0일 수 있다.

반대도 가능하다.

화면 글자만 999로 바꾸고 실제 state는 0이면 다음 계산이 이상해질 수 있다.

그래서 UI 프로그램에서는:

> 어떤 값이 진짜 상태의 기준인가?

를 명확하게 정하는 것이 중요하다.

#### 10. input event로 사용자가 입력하는 순간을 받는다

```html
<label for="name">이름</label>
<input id="name">
<p id="preview"></p>
```

JavaScript:

```javascript
const input = document.querySelector("#name");
const preview = document.querySelector("#preview");

input.addEventListener("input", (event) => {
  preview.textContent = event.target.value;
});
```

사용자가 글자를 입력할 때마다 현재 값을 미리 보여 준다.

#### 11. event object에는 사건 정보가 들어 있다

위 코드의 `event`에는 어떤 element에서 event가 생겼는지, 키보드 정보 등 사건에 관련된 데이터가 들어갈 수 있다.

```javascript
event.target.value
```

는 지금 input의 값을 읽는 예다.

#### 12. form submit의 기본 동작을 이해한다

```html
<form id="login-form">
  ...
  <button type="submit">로그인</button>
</form>
```

form은 기본적으로 제출 동작을 가진다.

JavaScript로 API 호출을 직접 처리하고 싶다면:

```javascript
form.addEventListener("submit", (event) => {
  event.preventDefault();
  // 직접 처리
});
```

처럼 기본 제출을 막고 커스텀 로직을 실행할 수 있다.

`preventDefault`를 무조건 외우지 말고 `브라우저의 기본 동작을 막는다`는 의미를 이해한다.

#### 13. event bubbling을 아주 쉽게 본다

```html
<div id="outer">
  <button id="inner">눌러요</button>
</div>
```

button을 클릭하면 event가 부모 방향으로 전달되는 **bubbling**이 일어날 수 있다.

그래서 바깥 div에도 click handler가 있으면 둘 다 실행될 수 있다.

이 특성을 이용해 많은 자식 대신 부모 하나에서 event를 처리하는 **event delegation** 패턴도 만들 수 있다.

#### 14. event listener를 계속 중복 등록하면 문제가 생길 수 있다

화면을 다시 그릴 때마다 같은 버튼에 listener를 또 등록하면 한 번 클릭했는데 함수가 여러 번 실행될 수 있다.

```text
한 번 클릭
→ handler 3번 실행
```

UI 버그에서 흔히 보는 문제다.

#### 15. DOM을 직접 조작할 때 HTML 문자열을 무심코 넣지 않는다

사용자 입력을:

```javascript
element.innerHTML = userInput;
```

처럼 그대로 넣으면 보안 문제로 이어질 수 있다.

단순 텍스트라면:

```javascript
element.textContent = userInput;
```

처럼 의도에 맞는 API를 사용한다.

XSS는 보안 TRACK에서 더 자세히 배운다.

#### 16. 전체 카운터 앱을 완성한다

HTML:

```html
<main class="counter">
  <h1>카운터</h1>
  <p id="count">0</p>
  <div class="actions">
    <button id="minus" type="button">-1</button>
    <button id="plus" type="button">+1</button>
  </div>
</main>
```

JavaScript:

```javascript
const countElement = document.querySelector("#count");
const plusButton = document.querySelector("#plus");
const minusButton = document.querySelector("#minus");

let count = 0;

function render() {
  countElement.textContent = String(count);
}

plusButton.addEventListener("click", () => {
  count = count + 1;
  render();
});

minusButton.addEventListener("click", () => {
  count = count - 1;
  render();
});

render();
```

여기서 중요한 새 생각은 `render()`다.

```text
state를 바꾼다
↓
render로 화면을 state와 맞춘다
```

React 같은 프레임워크를 나중에 만나도 이 상태→화면 개념은 계속 중요하다.

#### 17. 책을 덮고 확인한다

1. DOM은 무엇이고 왜 tree와 연결되는가?
2. querySelector는 어떤 일을 하는가?
3. event와 event listener를 설명하라.
4. state와 DOM 화면이 서로 달라지면 어떤 문제가 생길 수 있는가?
5. preventDefault는 어떤 기본 동작을 막을 때 쓰는가?
6. innerHTML에 사용자 입력을 무심코 넣으면 왜 위험할 수 있는가?

---

## BLOCK 06 · 브라우저가 화면을 그리는 과정을 알고 DevTools로 문제를 직접 찾는다

### LESSON 01 · 렌더링·Network·Elements·Console·반응형 검사를 하나의 작은 앱 프로젝트로 연결한다

#### 1. 코드 파일을 저장했다고 바로 픽셀이 되는 것은 아니다

브라우저는 대략 다음과 같은 일을 한다.

```text
HTML을 읽어 DOM 구조 생성
CSS를 읽어 스타일 규칙 생성
DOM과 스타일을 조합해 배치 계산
화면에 그리기
JavaScript가 변경하면 필요한 부분 다시 계산/그리기
```

실제 브라우저 렌더링 엔진은 훨씬 복잡하지만 초급에서는 이 큰 흐름을 잡는다.

#### 2. parsing은 글자 코드를 구조로 읽는 과정이다

브라우저가 HTML 문자열을 읽고 의미 있는 구조로 바꾸는 것을 **parsing(파싱)**이라고 한다.

```text
"<h1>안녕</h1>"
↓ HTML parser
DOM의 h1 node
```

CSS도 규칙을 읽어 내부 구조로 만든다.

서버에서도 JSON parsing을 다시 만나게 된다.

#### 3. layout은 각 요소가 어디에 얼마나 큰지 계산한다

예를 들어:

```css
.card {
  width: 300px;
  padding: 20px;
}
```

브라우저는 부모 크기, box model, flex/grid 규칙 등을 바탕으로 실제 위치와 크기를 계산한다.

이런 배치 계산 과정을 넓게 **layout**이라고 부른다.

#### 4. paint는 계산된 것을 실제 화면 픽셀로 그리는 단계다

색, 글자, 테두리, 그림 등을 실제 화면에 그리는 과정이 필요하다.

이런 단계가 **paint**와 연결된다.

스크롤이나 animation이 있을 때 브라우저는 성능을 위해 여러 최적화 단계를 사용한다.

지금 세부 엔진 구현을 외우지 않는다.

#### 5. 왜 width를 바꾸면 더 비싼 변경이 될 수 있을까

어떤 CSS 변경은 요소의 위치와 크기를 다시 계산하게 만들 수 있다.

다른 변경은 주로 그리기나 합성 단계에 영향을 줄 수 있다.

이 때문에 animation을 만들 때 `top/left`보다 transform을 사용하는 것이 더 유리한 경우가 있다는 성능 조언이 나온다.

하지만 무조건 규칙으로 외우지 않고 DevTools Performance로 실제 측정하는 습관을 만든다.

#### 6. DevTools는 브라우저가 실제로 본 것을 확인하는 도구다

Chrome 기준으로 대표 패널을 본다.

```text
Elements
→ 실제 DOM과 적용 CSS

Console
→ JavaScript 로그와 오류

Network
→ 서버 요청과 응답

Sources
→ 로드된 코드와 debugger

Application
→ 저장소, service worker 등의 정보

Performance
→ 실행/렌더링 성능 분석
```

처음에는 Elements, Console, Network 세 개만 익혀도 큰 도움이 된다.

#### 7. Elements에서 실제 DOM을 확인한다

소스 HTML에:

```html
<p id="message">안녕</p>
```

가 있다.

JavaScript가:

```javascript
document.querySelector("#message").textContent = "변경됨";
```

을 실행했다.

파일 자체는 `안녕`일 수 있지만 현재 DOM은 `변경됨`이다.

Elements는 **지금 브라우저에 존재하는 구조**를 보는 데 도움이 된다.

#### 8. Console에서 오류의 첫 위치를 찾는다

JavaScript:

```javascript
const button = document.querySelector("#save");
button.addEventListener("click", save);
```

그런데 HTML에 `#save`가 없다.

Console에 `null`과 관련된 오류가 나올 수 있다.

오류 메시지를 보자마자 코드 전체를 다시 쓰지 않는다.

```text
어느 파일?
몇 번째 줄?
어떤 값이 null/undefined였나?
왜 그 값이 없었나?
```

순서로 좁힌다.

#### 9. Network에서 화면 문제가 서버 문제인지 구분한다

화면에 데이터가 안 나온다.

원인 후보:

```text
요청 자체를 안 보냄
요청 URL이 틀림
서버 500
서버 200인데 JSON이 이상함
응답은 정상인데 JavaScript 렌더링 실패
CSS로 숨김
```

Network 패널에서 요청이 실제로 갔는지 먼저 본다.

```text
Request URL
Method
Status
Response headers
Response body
```

를 확인한다.

네트워크 TRACK에서 더 깊게 다룬다.

#### 10. Console log는 추측을 사실로 바꾸는 가장 작은 도구다

```javascript
console.log("save clicked");
```

버튼이 눌렸는지 확인할 수 있다.

```javascript
console.log({ count });
```

현재 state를 확인한다.

하지만 운영 앱에 개인정보, token, password를 로그로 남기면 안 된다.

#### 11. 반응형 문제는 Device Toolbar로 재현한다

폭을 360px, 768px, 1280px 등으로 바꾸며 실제 깨지는 지점을 본다.

단순히 `Galaxy처럼 보이는 프리셋`만 확인하지 않고 폭을 연속으로 줄여 본다.

어느 순간:

```text
버튼 두 개가 겹침
검색창이 잘림
텍스트가 카드 밖으로 나감
```

같은 문제가 생기는 정확한 지점을 찾는다.

#### 12. 접근성 기본 검사를 함께 한다

프로젝트 마지막에 다음을 한다.

```text
Tab 키만으로 주요 기능 이동
focus 표시 확인
label이 input과 연결됐는지 확인
이미지 alt 점검
색상만으로 상태를 전달하지 않는지 확인
200% 확대에서 내용 손실 없는지 확인
```

모든 접근성 표준을 이 한 TRACK에서 끝내는 것은 아니지만 기본 습관을 만든다.

#### 13. 최종 프로젝트 — 메모 카드 앱

기능:

```text
제목 입력
메모 입력
추가 버튼
카드 목록 표시
카드 삭제
현재 카드 개수 표시
모바일에서 1열
넓은 화면에서 여러 열
```

HTML 뼈대:

```html
<main class="page">
  <h1>메모</h1>

  <form id="memo-form">
    <label for="title">제목</label>
    <input id="title" name="title" required>

    <label for="body">내용</label>
    <textarea id="body" name="body" required></textarea>

    <button type="submit">추가</button>
  </form>

  <p>메모 <strong id="count">0</strong>개</p>
  <section id="memo-list" class="memo-grid"></section>
</main>
```

#### 14. state를 배열로 둔다

```javascript
const memos = [];
```

메모 하나:

```javascript
{
  title: "병원 방문",
  body: "다음 주 점심 약속"
}
```

form submit에서 값을 읽어 배열에 추가한다.

#### 15. render 함수로 화면을 state와 맞춘다

```javascript
function render() {
  const list = document.querySelector("#memo-list");
  const count = document.querySelector("#count");

  list.replaceChildren();
  count.textContent = String(memos.length);

  memos.forEach((memo, index) => {
    const article = document.createElement("article");
    const title = document.createElement("h2");
    const body = document.createElement("p");
    const button = document.createElement("button");

    title.textContent = memo.title;
    body.textContent = memo.body;
    button.textContent = "삭제";
    button.type = "button";

    button.addEventListener("click", () => {
      memos.splice(index, 1);
      render();
    });

    article.append(title, body, button);
    list.append(article);
  });
}
```

여기에는 지금까지 배운 것이 연결된다.

```text
DOM 요소 생성
textContent
array state
forEach
click event
state 변경
다시 render
```

#### 16. CSS grid로 반응형 카드 목록을 만든다

```css
.memo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}
```

모바일에서는 한 열, 넓은 화면에서는 여러 열이 자연스럽게 나올 수 있다.

#### 17. 일부러 다섯 가지 버그를 만든다

```text
1. #memo-list id를 일부러 틀리게 바꾼다.
2. render() 호출을 하나 지운다.
3. CSS width: 900px을 카드에 넣어 모바일 overflow를 만든다.
4. button type을 제거해 form 안에서 예상치 않은 submit을 관찰한다.
5. label의 for와 input id를 다르게 만든다.
```

각 문제를 DevTools로 진단한다.

```text
증상
→ Console/Elements/Network 중 어디를 먼저 볼지
→ 실제 원인
→ 최소 수정
→ 다시 검증
```

#### 18. TRACK 04 완료 기준

책을 보지 않고 다음을 할 수 있어야 한다.

- 인터넷과 웹을 구분한다.
- HTML 문서의 head/body와 element/attribute를 설명한다.
- link/button/form/input/label 역할을 구분한다.
- semantic HTML과 접근성의 기본 이유를 설명한다.
- CSS selector/property/value와 cascade를 설명한다.
- box model에서 content/padding/border/margin을 구분한다.
- flex와 grid로 기본 레이아웃을 만든다.
- 360px 화면에서 가로 overflow를 찾아 고친다.
- DOM이 tree라는 것을 설명하고 querySelector로 요소를 찾는다.
- event listener로 버튼 동작을 연결한다.
- state를 바꾸고 render로 화면을 갱신한다.
- Elements/Console/Network를 이용해 문제를 좁힌다.

화면이 `그럴듯하게 보이는 것`만이 통과 기준이 아니다.

**왜 이 HTML 요소를 골랐는지, 왜 이 CSS가 적용됐는지, 클릭 후 어떤 state가 바뀌고 어떤 DOM이 갱신되는지 설명할 수 있어야 한다.**
