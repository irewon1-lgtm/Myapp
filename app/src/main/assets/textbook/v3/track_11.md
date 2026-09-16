# TRACK 11 · 소프트웨어 설계와 종합 프로젝트

앞의 TRACK들에서 코드, 자료구조, 웹, JavaScript, 네트워크, 서버, DB, 보안, 테스트와 배포를 배웠다.

마지막 TRACK의 목표는 새로운 어려운 단어를 잔뜩 외우는 것이 아니다.

지금까지 배운 조각을 **하나의 시스템으로 묶는 법**을 배우는 것이다.

좋은 개발자는 코드를 빨리 쓰는 사람만을 뜻하지 않는다.

```text
무엇을 만들어야 하는지 정의한다.
데이터와 책임의 경계를 나눈다.
실패를 예상한다.
성능과 보안을 고려한다.
변경하기 쉬운 구조를 만든다.
실제 증거로 검증한다.
```

AI가 코드를 많이 작성하는 시대에는 이 능력이 더 중요하다.

AI에게 `앱 만들어 줘`라고 말하는 것보다:

```text
무슨 기능이 필요한지
데이터가 어떻게 흐르는지
누가 무엇을 책임지는지
어떤 상태는 절대 깨지면 안 되는지
어떻게 테스트할지
실패하면 어떻게 복구할지
```

를 정할 수 있어야 결과를 검토할 수 있다.

---

## BLOCK 01 · 설계는 코드를 쓰기 전에 `무엇을 만들어야 하는가`를 정확히 정하는 일부터 시작한다

### LESSON 01 · requirement·functional/non-functional·constraint·acceptance criteria·domain·model을 하나의 요구사항 문서로 연결한다

#### 1. `좋은 앱 만들어 줘`는 요구사항이 아니다

다음 요청을 보자.

```text
좋은 주식 앱 만들어 줘.
```

사람은 대충 뜻을 짐작할 수 있지만 프로그램을 만들기에는 너무 모호하다.

무엇이 `좋은` 것인지 알 수 없다.

```text
빠른 앱?
예쁜 앱?
정확한 앱?
알림이 있는 앱?
1,000종목을 다루는 앱?
오프라인에서도 되는 앱?
```

시스템이 해야 하는 기능과 조건을 **요구사항(requirement)**이라고 부른다.

좋은 요구사항은 가능한 한 관찰하거나 확인할 수 있어야 한다.

예:

```text
사용자는 이메일과 비밀번호로 로그인할 수 있다.
사용자는 관심 종목을 최대 100개 저장할 수 있다.
권한 없는 사용자는 다른 사용자의 관심 종목을 볼 수 없다.
검색 결과는 일반적인 상황에서 2초 이내 표시되어야 한다.
```

#### 2. 기능 요구사항은 `무엇을 할 수 있는가`를 말한다

**functional requirement**는 시스템이 제공해야 하는 기능을 정의한다.

예:

```text
회원가입
로그인
종목 검색
즐겨찾기 저장
알림 설정
보고서 보기
```

조금 더 구체적으로 쓰면:

```text
사용자는 종목명을 입력해 검색할 수 있다.
검색 결과에서 종목 하나를 선택할 수 있다.
선택한 종목을 관심목록에 저장할 수 있다.
관심목록에서 종목을 제거할 수 있다.
```

이렇게 실제 행동 단위로 나누면 화면과 API, DB 설계가 보이기 시작한다.

#### 3. 비기능 요구사항은 `기능이 어떤 품질로 동작해야 하는가`를 말한다

검색 기능이 있다고 하자.

정확한 결과가 나오는데 3분 걸린다.

기능 자체는 있지만 실제 사용하기 어렵다.

그래서 다음 조건도 필요하다.

```text
성능
가용성
보안
접근성
확장성
운영성
데이터 보존
```

이런 품질 조건을 **non-functional requirement**라고 부른다.

예:

```text
검색 API p95 응답시간 500ms 이하를 목표로 한다.
관리자 기능은 인증과 관리자 권한 확인이 모두 필요하다.
중요 데이터는 앱 재시작 후에도 보존되어야 한다.
장애 시 핵심 조회 기능을 30분 안에 복구할 수 있는 절차가 있어야 한다.
```

#### 4. `빠르게`를 숫자로 바꿔야 확인할 수 있다

나쁜 요구사항:

```text
검색이 빨라야 한다.
```

좋은 방향:

```text
일반적인 검색 요청의 p95 응답시간을 500ms 이하로 유지한다.
```

`p95`가 아직 익숙하지 않다면 이렇게 이해한다.

100번 요청했을 때 대부분의 요청이 어느 정도 시간 안에 들어오는지 보는 성능 지표 중 하나다.

지금 핵심은 숫자 자체가 아니라:

> **완료 여부를 확인할 수 있는 말로 바꾼다.**

이다.

#### 5. constraint는 선택을 제한하는 조건이다

프로젝트에는 자유롭게 선택할 수 없는 조건이 있다.

예:

```text
Android 10 이상 지원
무료 API만 사용
월 운영비 10만원 이하
개인정보는 특정 지역에 저장
외부 시스템 쓰기는 승인 후 실행
```

이런 조건을 **constraint(제약)**라고 부른다.

이 제약을 뒤늦게 알면 큰 재설계가 생긴다.

예를 들어 유료 API로 전체 시스템을 만든 뒤 `유료 서비스 금지`를 알게 되면 수집 구조를 다시 만들어야 한다.

그래서 설계 초기에 제약을 수집한다.

#### 6. acceptance criteria는 `언제 완료라고 말할 수 있는가`를 정한다

요구사항:

```text
사용자는 로그인할 수 있다.
```

만 있으면 완료 판정이 모호하다.

인수 기준을 붙인다.

```text
정상 email/password → 로그인 성공
틀린 password → 401
존재하지 않는 email → 정책에 맞는 실패 응답
5회 연속 실패 → rate limit 적용
로그인 성공 후 자신의 프로필 조회 가능
다른 사용자 프로필 수정 불가
```

이런 실제 확인 조건을 **acceptance criteria**라고 부른다.

완료를 느낌으로 판단하지 않고 테스트 가능한 조건으로 만든다.

#### 7. user story는 사용자 관점에서 기능 이유를 붙여 준다

예:

```text
사용자로서
종목명을 검색해
원하는 회사를 빠르게 찾고 싶다.
```

이런 형식을 **user story**라고 부른다.

하지만 user story 한 줄만으로 구현하기에는 부족할 수 있다.

그래서 acceptance criteria와 세부 제약이 함께 필요하다.

#### 8. domain은 프로그램이 해결하려는 실제 문제 영역이다

병원 CRM과 쇼핑몰은 같은 프로그램 문법을 쓸 수 있다.

하지만 업무 개념은 다르다.

병원 CRM:

```text
병원
의사
방문
처방
후속 일정
```

쇼핑몰:

```text
상품
주문
결제
배송
환불
```

이처럼 프로그램이 해결하려는 실제 업무와 지식 영역을 **domain**이라고 부른다.

도메인의 실제 용어를 코드에 정확히 반영하면 이해가 쉬워진다.

```text
HospitalVisit
Prescription
Order
Refund
Portfolio
```

모호한:

```text
DataManager2
ThingProcessor
```

보다 역할이 분명하다.

#### 9. model은 현실 전체가 아니라 프로그램에 필요한 부분만 표현한다

현실의 주문에는 많은 정보가 있다.

하지만 프로그램에는 필요한 일부만 모델링한다.

```text
Order
- id
- userId
- items
- status
- totalAmount
- createdAt
```

이런 표현을 **model**이라고 생각할 수 있다.

현실의 사람 한 명을 프로그램에 표현한다고 모든 신체 정보와 인생 기록을 저장할 필요는 없다.

현재 문제에 필요한 정보만 선택한다.

#### 10. 요구사항에서 데이터 모델이 나온다

요구사항:

```text
사용자는 여러 개의 관심 종목을 저장할 수 있다.
```

여기서 질문한다.

```text
누가 저장했는가?
어떤 종목인가?
언제 저장했는가?
같은 종목 중복 저장을 허용할까?
```

모델 후보:

```text
WatchlistItem
- userId
- stockId
- createdAt
```

DB 규칙 후보:

```text
(userId, stockId) UNIQUE
```

요구사항이 DB 설계와 연결된다.

#### 11. 상태를 먼저 적으면 누락을 찾기 쉽다

주문을 예로 든다.

```text
DRAFT
PAID
SHIPPED
CANCELLED
```

이렇게 상태를 정한다.

그 다음 허용되는 이동을 생각한다.

```text
DRAFT → PAID
DRAFT → CANCELLED
PAID → SHIPPED
PAID → CANCELLED 정책에 따라 가능
```

허용되지 않는 이동:

```text
CANCELLED → SHIPPED
```

이런 규칙은 나중에 validation과 테스트에 직접 사용된다.

#### 12. 불변조건을 요구사항에서 뽑는다

절대로 깨지면 안 되는 규칙을 적는다.

```text
주문 totalAmount는 음수가 아니다.
모든 주문은 존재하는 사용자에게 속한다.
결제 완료 주문은 결제 기록을 가진다.
삭제된 사용자의 private data는 정책대로 처리된다.
```

이런 규칙은 DB constraint, transaction, 테스트, 모니터링 기준이 된다.

#### 13. AI에게 일을 맡기기 전 spec을 만든다

나쁜 요청:

```text
로그인 만들어 줘.
```

좋은 방향:

```text
목표: email/password 로그인
입력: email, password
성공: session 생성 후 200
실패: 잘못된 인증은 401
보안: password는 Argon2 계열 hashing 사용
권한: 로그인 후 자기 profile만 수정 가능
rate limit: 연속 실패 보호
테스트: 정상/오류/경계 케이스 포함
금지: production secret 코드 하드코딩 금지
```

AI가 코드를 많이 만들어도 검토 기준은 사람이 먼저 정해야 한다.

#### 14. 책을 덮고 확인한다

1. functional과 non-functional requirement의 차이를 예로 설명하라.
2. `빠른 검색`을 측정 가능한 조건으로 바꿔 보라.
3. constraint를 초기에 확인해야 하는 이유는?
4. acceptance criteria가 필요한 이유는?
5. domain과 model은 무엇이 다른가?
6. 주문 시스템의 불변조건을 세 개 적어 보라.

---

## BLOCK 02 · 좋은 구조는 코드를 많이 나누는 것이 아니라 `누가 무엇을 책임지는가`를 분명하게 만든다

### LESSON 01 · abstraction·interface·module·responsibility·cohesion·coupling·dependency·DI·layer를 주문 기능 하나로 연결한다

#### 1. 큰 함수 하나에 모든 것을 넣으면 처음에는 편하다

주문 생성 API를 만든다고 하자.

초보 버전:

```javascript
app.post("/orders", async (req, res) => {
  // 로그인 확인
  // 입력 검사
  // 가격 계산
  // 재고 확인
  // DB 저장
  // 결제 API 호출
  // 이메일 전송
  // 로그 기록
  // 응답
});
```

처음에는 한 파일이라 편해 보인다.

하지만 코드가 커지면:

```text
가격 계산만 테스트하기 어려움
결제사를 바꾸기 어려움
DB 코드를 수정하면 handler도 건드림
이메일 오류가 주문 생성과 섞임
```

이런 문제가 생긴다.

#### 2. responsibility는 `이 코드는 무엇을 맡는가`라는 질문이다

역할을 나눠 본다.

```text
OrderController
→ HTTP request/response 처리

OrderService
→ 주문 생성 업무 규칙

OrderCalculator
→ 가격 계산

OrderRepository
→ 주문 저장/조회

PaymentGateway
→ 외부 결제 기능 사용

NotificationService
→ 알림 전송
```

이처럼 구성요소가 맡는 역할을 **responsibility(책임)**라고 부른다.

좋은 이름은 책임을 설명한다.

```text
CommonManager
Utils2
HelperEverything
```

처럼 아무것이나 들어가는 이름은 책임이 모호해지기 쉽다.

#### 3. module은 관련 책임을 하나의 경계로 묶는다

예:

```text
order module
- OrderService
- OrderRepository
- OrderValidator
- OrderCalculator
```

관련 코드와 데이터를 묶고 외부와의 접점을 제한한 논리 단위를 **module**이라고 부를 수 있다.

파일 하나가 항상 모듈 하나인 것은 아니다.

여러 파일이 하나의 논리 모듈을 이룰 수 있다.

#### 4. abstraction은 복잡한 세부를 숨기고 필요한 동작만 보여 준다

자동차 운전자는 엔진 내부 폭발 과정을 매번 직접 조절하지 않는다.

가속 페달이라는 단순한 사용 방법을 쓴다.

코드도 비슷하다.

```javascript
paymentGateway.charge(order.totalAmount)
```

호출자는:

```text
HTTP URL
signature 생성
retry
provider-specific JSON
```

세부를 매번 알 필요가 없다.

복잡한 세부를 숨기고 필요한 핵심 동작을 제공하는 생각을 **abstraction**이라고 부른다.

#### 5. interface는 `어떻게 사용할 수 있는가`라는 계약이다

결제 기능에 필요한 계약:

```text
charge(amount)
refund(paymentId)
```

이것을 **interface**라고 생각할 수 있다.

```text
interface = 무엇을 제공하는가
implementation = 실제로 어떻게 하는가
```

를 나눈다.

예:

```text
PaymentGateway interface
├─ TossPaymentGateway
├─ StripePaymentGateway
└─ FakePaymentGateway
```

상위 주문 로직은 `결제한다`는 계약만 알면 된다.

#### 6. cohesion은 한 모듈 안의 코드가 얼마나 같은 목적에 모여 있는가를 본다

`EmailValidator` 안에:

```text
email 형식 검사
email 길이 검사
email domain 정책 검사
```

가 있다면 같은 목적에 잘 모여 있다.

그런데:

```text
email 검사
이미지 압축
결제 계산
로그 파일 삭제
```

가 같이 있으면 관련성이 낮다.

이 관점을 **cohesion(응집도)**라고 부른다.

보통 관련 책임은 함께 모으는 높은 응집도를 선호한다.

#### 7. coupling은 다른 모듈과 얼마나 강하게 묶였는가를 본다

OrderService가:

```text
PostgreSQL table 이름
Express request 객체
특정 결제사 SDK 내부 class
Android UI 객체
```

를 모두 직접 알아야 한다면 결합이 강하다.

한 기술이 바뀔 때 여러 곳을 같이 고쳐야 한다.

이 관계를 **coupling(결합도)** 관점으로 본다.

자주 쓰는 방향:

```text
높은 cohesion
낮은 불필요한 coupling
```

#### 8. 무조건 파일을 많이 쪼개면 좋은 설계인가

아니다.

함수 하나마다:

```text
interface
factory
adapter
wrapper
manager
```

를 만들면 작은 프로젝트가 오히려 이해하기 어려워질 수 있다.

설계의 목적은 파일 수가 아니다.

> **변경 이유와 책임을 분리해서 이해·테스트·수정하기 쉽게 만드는 것**

이다.

#### 9. dependency는 한 구성요소가 다른 것을 필요로 하는 관계다

OrderService가 PaymentGateway를 호출한다.

```text
OrderService
↓ depends on
PaymentGateway
```

이것이 dependency다.

전체 시스템을 그림으로 그릴 수 있다.

```text
UI
↓
API Controller
↓
OrderService
↓
OrderRepository
↓
DB
```

의존 방향이 복잡하게 서로 순환하면 수정하기 어려워질 수 있다.

#### 10. dependency injection은 필요한 것을 밖에서 전달한다

나쁜 결합 예:

```javascript
class OrderService {
  constructor() {
    this.payment = new RealPaymentGateway();
  }
}
```

OrderService가 구체적인 결제사를 직접 만든다.

테스트에서도 실제 결제 객체가 생기기 쉽다.

밖에서 전달한다.

```javascript
class OrderService {
  constructor(paymentGateway) {
    this.paymentGateway = paymentGateway;
  }
}
```

production:

```text
RealPaymentGateway 주입
```

unit test:

```text
FakePaymentGateway 주입
```

이런 방식을 **dependency injection(DI)**이라고 부른다.

#### 11. dependency inversion은 핵심 규칙이 구체적인 도구에 묶이지 않게 한다

주문 업무 규칙은 특정 결제사보다 오래 살아남을 수 있다.

```text
OrderService
↓
PaymentGateway 계약
↑
TossPaymentGateway 구현
```

상위 업무 규칙이 특정 SDK에 직접 의존하지 않고 추상 계약에 의존하도록 만드는 생각이 **Dependency Inversion**과 연결된다.

#### 12. layer는 역할별로 시스템을 큰 층으로 나누는 방식이다

예:

```text
Presentation
↓
Application
↓
Domain
↓
Infrastructure
```

프로젝트마다 이름은 다르다.

중요한 것은 계층 수가 아니라:

```text
UI 처리
업무 규칙
데이터 접근
외부 기술
```

을 어떤 경계로 나눌지 생각하는 것이다.

#### 13. Clean Architecture라는 이름보다 핵심 방향을 이해한다

Clean Architecture 같은 접근에서는 핵심 business rule을 UI, DB, framework 같은 바깥 기술에서 최대한 보호하려고 한다.

큰 생각:

```text
핵심 규칙
↑
use case
↑
adapter
↑
DB/framework/UI
```

하지만 작은 앱에 그대로 복사해 거대한 구조를 만들 필요는 없다.

목적은 기술 이름을 따르는 것이 아니라 **변경 경계를 명확하게 하는 것**이다.

#### 14. 같은 기능을 구조 전후로 비교한다

한 파일 버전:

```text
POST /orders
→ validation
→ total 계산
→ DB SQL
→ 결제 SDK
→ 이메일
```

역할 분리 버전:

```text
OrderController
→ 입력/HTTP

OrderService
→ 업무 흐름

OrderCalculator
→ 금액 규칙

OrderRepository
→ DB

PaymentGateway
→ 결제

NotificationService
→ 알림
```

이제 가격 규칙만 바꾸면 OrderCalculator 주변을 집중해서 볼 수 있다.

DB를 PostgreSQL에서 다른 저장소로 바꾸면 Repository 경계가 중요해진다.

#### 15. 좋은 경계인지 확인하는 질문

```text
이 구성요소의 책임을 한 문장으로 말할 수 있는가?
이 기술을 바꾸면 몇 군데가 같이 바뀌는가?
이 기능을 단독으로 테스트할 수 있는가?
외부 시스템 없이 핵심 규칙을 테스트할 수 있는가?
데이터가 어디에서 어디로 흐르는지 보이는가?
```

이 질문에 답하기 어려우면 경계를 다시 본다.

#### 16. 책을 덮고 확인한다

1. responsibility와 module은 어떻게 연결되는가?
2. abstraction과 interface는 무엇이 다른가?
3. cohesion이 높은 모듈의 예를 들어 보라.
4. coupling이 너무 강하면 어떤 문제가 생기는가?
5. DI가 테스트에 도움이 되는 이유는?
6. layer 수가 많다고 좋은 설계가 아닌 이유는?

---

## BLOCK 03 · 상태·캐시·큐·동시성은 `시간 순서가 달라질 때 시스템이 어떻게 버티는가`를 다룬다

### LESSON 01 · state management·cache·queue·idempotency·concurrency·race condition을 주문 처리 흐름으로 연결한다

#### 1. state는 시스템의 현재 상태다

로그인 화면을 생각하자.

```text
로그인 전
로그인 중
로그인 성공
로그인 실패
```

이런 현재 상황을 **state**라고 부를 수 있다.

주문도 상태가 있다.

```text
DRAFT
PAYMENT_PENDING
PAID
SHIPPED
CANCELLED
```

state management는:

```text
현재 상태가 무엇인지
어떤 사건이 상태를 바꾸는지
어떤 이동을 허용하는지
```

관리하는 일이다.

#### 2. 상태 전이를 명확히 적는다

```text
DRAFT
  ├─ pay → PAYMENT_PENDING
  └─ cancel → CANCELLED

PAYMENT_PENDING
  ├─ success → PAID
  └─ fail → DRAFT/FAILED 정책 결정

PAID
  └─ ship → SHIPPED
```

이렇게 그리면 이상한 상태를 찾기 쉽다.

```text
CANCELLED → SHIPPED
```

같은 전이는 막아야 한다.

#### 3. cache는 결과를 다시 계산하거나 다시 가져오는 비용을 줄인다

주가 종목 기본정보가 DB에 있다.

매 요청마다 복잡한 query를 실행하면 비용이 클 수 있다.

자주 쓰는 결과를 빠른 저장소에 잠시 보관한다.

```text
첫 요청
DB 조회
↓
cache 저장

다음 요청
cache에서 바로 반환
```

이것이 **cache**의 기본 생각이다.

#### 4. cache의 어려운 점은 저장보다 `언제 버릴까`다

DB의 회사명이 바뀌었다.

cache에는 예전 이름이 남아 있다.

```text
DB = 새 이름
cache = 옛 이름
```

사용자는 오래된 데이터를 보게 된다.

그래서 cache 설계에는:

```text
TTL
invalidation
version
source of truth
```

같은 문제가 생긴다.

초급에서 중요한 질문:

> **원본이 바뀌었을 때 cache가 언제 새 값을 보게 되는가?**

#### 5. queue는 오래 걸리는 작업을 뒤로 보낼 때 유용하다

회원가입 요청에서 이메일 전송이 5초 걸린다고 하자.

사용자가 5초 동안 응답을 기다릴 필요가 없을 수 있다.

```text
회원 저장
↓
이메일 작업을 queue에 넣음
↓
사용자에게 성공 응답

worker
↓
queue에서 이메일 작업 꺼냄
↓
전송
```

작업 대기열을 **queue**라고 부른다.

#### 6. queue를 쓰면 실패와 재시도를 설계해야 한다

이메일 서버가 잠시 장애다.

worker가 실패했다.

다음 질문이 생긴다.

```text
다시 시도할까?
몇 번 시도할까?
몇 초 간격으로 시도할까?
영원히 실패하면 어디에 남길까?
같은 이메일을 두 번 보내도 괜찮나?
```

queue는 마법처럼 문제를 없애지 않는다.

시간을 분리한 만큼 실패 처리 규칙이 필요하다.

#### 7. retry는 항상 안전하지 않다

결제 요청을 보냈다.

서버 응답이 timeout됐다.

클라이언트 입장에서는 성공했는지 모른다.

무조건 다시 요청한다.

첫 번째 결제가 실제 성공했다면 두 번 결제될 수 있다.

그래서 retry 가능한 작업에는 **idempotency**가 중요하다.

#### 8. idempotency는 같은 요청을 반복해도 최종 효과가 중복되지 않게 하는 성질이다

결제 요청에 고유한 key를 준다고 하자.

```text
idempotency-key = order-100-payment-1
```

서버가 같은 key의 성공 결과를 기억한다.

재시도:

```text
같은 key
↓
새 결제 생성하지 않음
↓
기존 결과 반환
```

이런 설계를 사용하면 네트워크 retry에서 중복 효과를 줄일 수 있다.

#### 9. 동시성은 여러 작업이 겹쳐 진행될 때 생긴다

사용자 두 명이 마지막 재고 1개를 동시에 주문한다.

둘 다 거의 같은 순간에 확인한다.

```text
A: 재고 1 확인
B: 재고 1 확인
A: 주문 성공
B: 주문 성공
```

결과:

```text
재고 1개인데 주문 2개
```

이런 시간 순서 문제를 **race condition** 관점에서 볼 수 있다.

#### 10. `확인 후 수정` 사이가 벌어져 있으면 경쟁 조건이 생길 수 있다

나쁜 흐름:

```text
SELECT stock
if stock > 0
    UPDATE stock - 1
```

두 요청이 SELECT를 동시에 통과할 수 있다.

DB transaction, atomic update, lock, optimistic concurrency 같은 방법을 상황에 맞게 사용한다.

TRACK 08의 transaction이 여기서 실제 시스템 설계와 연결된다.

#### 11. lock은 동시에 바꾸지 못하게 막는 방법 중 하나다

특정 row를 한 작업이 수정하는 동안 다른 작업이 기다리게 할 수 있다.

장점:

```text
충돌 방지
```

단점:

```text
기다림 증가
deadlock 가능
처리량 감소
```

그래서 무조건 많이 거는 것이 좋은 것은 아니다.

#### 12. optimistic concurrency는 `충돌이 자주 없을 것`을 가정하고 검증한다

row에 version이 있다고 하자.

```text
stock=10
version=5
```

업데이트:

```text
version=5일 때만 stock 수정
성공하면 version=6
```

다른 작업이 먼저 수정해 version이 6이면 내 update가 실패한다.

그때 다시 읽고 처리한다.

이런 접근은 모든 경우에 정답이 아니지만 동시 수정 문제를 푸는 대표적 생각이다.

#### 13. state는 여러 곳에 중복되면 맞추기 어려워진다

예:

```text
DB order.status = PAID
cache = PENDING
message queue = PAYMENT_PENDING event 대기
UI local state = FAILED
```

어떤 것이 진짜 상태인가?

그래서 **source of truth**를 명확히 정해야 한다.

```text
이 데이터의 최종 기준은 어디인가?
```

를 설계한다.

#### 14. event-driven 구조에서는 `언젠가 맞아지는 상태`가 있을 수 있다

주문 저장 후 별도 worker가 검색 index를 갱신한다고 하자.

짧은 순간:

```text
DB = 최신 주문
검색 index = 이전 상태
```

일 수 있다.

모든 시스템이 언제나 모든 저장소에서 즉시 같은 상태를 보장하는 것은 아니다.

이런 eventual consistency 상황에서는 사용자에게 어떤 지연이 허용되는지 요구사항으로 정한다.

#### 15. 책을 덮고 확인한다

1. state와 state transition을 주문 예로 설명하라.
2. cache의 가장 어려운 문제 중 하나가 invalidation인 이유는?
3. queue에 작업을 넣으면 새로 어떤 실패 문제를 고려해야 하는가?
4. retry가 결제 중복을 만들 수 있는 이유는?
5. idempotency key는 어떤 문제를 줄이는가?
6. 재고 1개 동시 주문에서 race condition이 생기는 과정을 설명하라.

---

## BLOCK 04 · 확장성과 안정성은 `더 큰 서버` 하나가 아니라 병목·실패·관측을 구조적으로 다루는 일이다

### LESSON 01 · performance·bottleneck·scale up/out·load balancing·fault tolerance·timeout·retry·circuit breaker·observability를 실제 서비스 흐름으로 배운다

#### 1. 느린 시스템을 보면 먼저 어디가 느린지 측정한다

검색이 5초 걸린다.

바로 서버를 두 배로 늘리면 안 된다.

시간을 나눈다.

```text
DNS/TLS 100ms
API handler 50ms
DB query 4,500ms
JSON 50ms
network 300ms
```

가장 큰 병목은 DB query다.

전체 성능을 제한하는 부분을 **bottleneck**이라고 부른다.

병목을 찾지 않고 하드웨어만 늘리면 돈만 쓰고 해결이 안 될 수 있다.

#### 2. latency와 throughput을 구분한다

**latency**:

```text
요청 하나가 완료되는 데 걸리는 시간
```

**throughput**:

```text
일정 시간 동안 처리할 수 있는 요청 수
```

둘은 관련 있지만 같은 값은 아니다.

한 요청이 100ms여도 동시에 2개밖에 처리하지 못하면 많은 사용자가 몰릴 때 대기열이 길어질 수 있다.

#### 3. scale up은 한 서버를 더 강하게 만든다

```text
CPU 2개 → 8개
RAM 8GB → 64GB
더 빠른 disk
```

처럼 한 장비의 성능을 높이는 것을 **vertical scaling / scale up**이라고 부른다.

장점:

```text
구조가 단순할 수 있음
```

한계:

```text
장비 한계 존재
비용 증가
한 대 장애 영향
```

#### 4. scale out은 서버 수를 늘린다

```text
server 1대
↓
server 3대
```

처럼 여러 서버로 나누는 것을 **horizontal scaling / scale out**이라고 부른다.

그럼 새 문제가 생긴다.

```text
어느 서버로 요청을 보낼까?
세션 상태는 어디에 둘까?
모든 서버 버전은 같은가?
DB는 더 많은 요청을 버틸까?
```

#### 5. load balancer는 요청을 여러 서버로 분산한다

```text
사용자
↓
load balancer
├─ app 1
├─ app 2
└─ app 3
```

요청을 여러 backend로 나눠 보내는 역할을 **load balancer**가 할 수 있다.

하지만 app 서버만 늘렸는데 DB가 하나라면 DB가 새 병목이 될 수 있다.

시스템 전체를 본다.

#### 6. stateless 서버가 scale out에 유리한 이유

로그인 session을 app 1의 메모리에만 저장했다고 하자.

다음 요청이 app 2로 가면 session을 모른다.

그래서 여러 서버 구조에서는 상태를 공유 저장소에 두거나 token 기반 구조 등 시스템에 맞는 방법을 선택할 수 있다.

`서버 메모리에 사용자별 필수 상태가 없음`에 가까운 **stateless** 구조는 scale out을 단순하게 만들 수 있다.

#### 7. fault tolerance는 일부가 실패해도 전체가 바로 무너지지 않게 한다

외부 추천 API가 장애다.

우리 쇼핑몰의 결제와 주문까지 모두 멈출 필요가 있을까?

추천은 숨기고 핵심 구매 기능은 계속 제공할 수도 있다.

이처럼 일부 실패가 전체 서비스 실패로 번지지 않게 만드는 생각을 **fault tolerance**와 연결할 수 있다.

#### 8. timeout 없이 외부 호출을 영원히 기다리면 자원이 묶인다

외부 API가 응답하지 않는다.

우리 서버 요청이 계속 기다린다.

동시에 수천 요청이 들어오면 기다리는 연결과 작업이 쌓일 수 있다.

그래서 외부 호출에는 적절한 **timeout**을 둔다.

너무 짧으면 정상 요청도 실패한다.

너무 길면 장애 상황에서 자원이 오래 묶인다.

실제 응답 분포를 보고 정한다.

#### 9. retry는 backoff와 한도를 함께 본다

외부 API가 1초 장애다.

즉시 재시도는 도움이 될 수 있다.

하지만 10,000개 클라이언트가 동시에 무한 retry하면 장애 서버를 더 압박한다.

그래서:

```text
최대 횟수
지연
exponential backoff
jitter
idempotency
```

같은 요소를 고려한다.

#### 10. circuit breaker는 계속 실패하는 dependency를 잠시 호출하지 않는다

외부 결제 조회 API가 계속 실패한다.

우리 서버가 요청마다 계속 호출하면:

```text
느린 timeout 반복
자원 소비
장애 전파
```

가 생긴다.

**circuit breaker** 패턴은 실패가 일정 수준을 넘으면 잠시 호출을 빠르게 차단하고 이후 회복 여부를 확인하는 방식이다.

전기 차단기 비유처럼 생각할 수 있다.

#### 11. bulkhead는 한 기능의 과부하가 전체 자원을 먹지 않게 나눈다

배의 칸막이처럼 자원을 분리하는 생각이다.

예:

```text
보고서 생성 worker pool
알림 전송 worker pool
핵심 API worker pool
```

보고서 생성이 폭주해도 핵심 로그인 요청의 모든 자원을 빼앗지 않게 제한할 수 있다.

#### 12. observability는 시스템 내부를 추측이 아니라 증거로 볼 수 있게 한다

운영에서 문제가 생겼다.

```text
느리다
```

만으로는 부족하다.

보통 다음 세 종류를 많이 본다.

```text
logs
metrics
traces
```

**logs**:

```text
개별 사건과 오류 기록
```

**metrics**:

```text
요청 수
오류율
latency
CPU
memory
```

**traces**:

```text
요청 하나가 여러 서비스와 DB를 지나간 경로와 시간
```

#### 13. request ID로 한 요청을 끝까지 따라간다

사용자 요청 하나가:

```text
API gateway
→ order service
→ payment service
→ DB
```

를 지난다.

각 로그에 같은 request/trace ID를 남기면 여러 시스템의 기록을 연결하기 쉬워진다.

#### 14. SLI/SLO는 `잘 운영된다`를 숫자로 바꾸는 방법이다

예:

```text
성공 요청 비율
p95 latency
서비스 가용률
```

같이 실제 측정하는 지표를 SLI 관점으로 볼 수 있다.

목표:

```text
월간 성공률 99.9% 이상
```

같은 서비스 목표를 SLO로 정할 수 있다.

지금 약어 암기보다:

> **운영 품질도 측정 가능한 목표로 만든다.**

는 생각이 중요하다.

#### 15. 성능 최적화는 측정 전후를 비교해야 한다

`cache를 넣었으니 빨라졌을 것`이라고 말하지 않는다.

수정 전:

```text
p95 = 1200ms
DB query = 900ms
```

수정 후:

```text
p95 = 350ms
DB query = 100ms
```

처럼 실제 측정한다.

최적화가 다른 문제를 만들지 않았는지도 본다.

#### 16. 책을 덮고 확인한다

1. latency와 throughput은 무엇이 다른가?
2. bottleneck을 찾기 전에 서버를 늘리면 안 되는 이유는?
3. scale up과 scale out 차이를 설명하라.
4. app 서버를 늘려도 DB가 병목일 수 있는 이유는?
5. timeout과 retry를 같이 설계해야 하는 이유는?
6. circuit breaker가 장애 전파를 줄이는 원리를 설명하라.
7. logs/metrics/traces를 각각 어떤 질문에 사용할 수 있는가?

---

## BLOCK 05 · 종합 프로젝트는 `코드 많이 쓰기`가 아니라 요구사항에서 운영 검증까지 한 줄로 연결하는 훈련이다

### LESSON 01 · 작은 실제 서비스를 설계하고 AI에게 맡길 부분과 사람이 검증할 부분을 분리한다

#### 1. 프로젝트 목표

다음 서비스를 만든다고 가정한다.

> **관심 종목을 저장하고 가격·뉴스·간단한 메모를 확인할 수 있는 개인용 주식 워치리스트 앱**

기능을 무작정 코딩하지 않는다.

처음부터 지금까지 배운 순서대로 설계한다.

#### 2. 요구사항을 쓴다

기능 요구사항:

```text
사용자는 로그인할 수 있다.
종목을 검색할 수 있다.
관심 종목을 저장/삭제할 수 있다.
관심 종목에 개인 메모를 작성할 수 있다.
각 종목의 최신 가격을 확인할 수 있다.
```

비기능 요구사항:

```text
다른 사용자의 관심목록과 메모를 볼 수 없다.
핵심 목록 화면은 일반 상황에서 2초 안에 표시된다.
가격 수집 실패가 메모 조회까지 막지 않아야 한다.
중요 오류는 로그와 metric으로 확인할 수 있어야 한다.
```

제약:

```text
외부 유료 시스템 사용은 승인 전 실행하지 않는다.
production 배포는 별도 승인 후 진행한다.
secret은 source code에 넣지 않는다.
```

#### 3. acceptance criteria를 만든다

로그인:

```text
정상 계정 → 성공
틀린 password → 실패
로그인 안 한 상태 → 관심목록 API 401
```

관심목록:

```text
종목 저장 → 목록에 표시
같은 종목 중복 저장 → 중복 방지
삭제 → 자기 항목만 삭제
다른 userId 항목 접근 → 403/404 정책에 맞게 차단
```

메모:

```text
저장 후 앱 재시작 → 유지
다른 사용자 메모 → 접근 불가
```

#### 4. 데이터 모델을 만든다

```text
User
- id
- email
- passwordHash

Stock
- id
- ticker
- name

WatchlistItem
- id
- userId
- stockId
- createdAt

StockMemo
- id
- userId
- stockId
- body
- updatedAt

PriceSnapshot
- stockId
- price
- capturedAt
```

불변조건:

```text
User.email은 중복되지 않는다.
WatchlistItem의 (userId, stockId)는 중복되지 않는다.
StockMemo는 실제 User와 Stock을 가리킨다.
다른 사용자 메모를 수정할 수 없다.
```

#### 5. 큰 구조를 그린다

```text
Android/Web UI
↓
API
↓
Auth / Watchlist / Memo service
↓
Repository
↓
DB

Price collector
↓
외부 market API
↓
Price storage/cache
```

가격 외부 API가 실패해도 메모 조회는 DB에서 가능하게 경계를 나눈다.

#### 6. responsibility를 나눈다

```text
AuthService
→ 로그인과 현재 사용자 확인

WatchlistService
→ 관심종목 추가/삭제 규칙

MemoService
→ 개인 메모 규칙

PriceService
→ 가격 조회와 freshness 처리

StockRepository
→ stock DB 접근

WatchlistRepository
→ watchlist DB 접근
```

각 책임을 한 문장으로 말할 수 있게 한다.

#### 7. API 계약을 만든다

```text
POST /login
GET /stocks?q=...
GET /watchlist
POST /watchlist
DELETE /watchlist/:stockId
GET /stocks/:stockId/memo
PUT /stocks/:stockId/memo
```

각 endpoint에:

```text
input
output
status code
auth requirement
validation
error
```

를 적는다.

#### 8. 보안 경계를 표시한다

외부 입력:

```text
search query
stockId
memo body
login credential
```

검증:

```text
length
format
existence
current user ownership
```

DB query는 parameterized query나 ORM의 안전한 binding을 사용한다.

secret:

```text
DB password
market API token
session signing key
```

는 source에 넣지 않는다.

#### 9. 비동기와 race condition을 설계한다

검색창에서 사용자가:

```text
A 검색
↓ 바로
AB 검색
```

한다.

이전 A 응답이 나중에 와서 AB 결과를 덮지 않게 한다.

```text
이전 요청 취소
또는 request version 비교
```

를 사용한다.

가격 수집은 background job/queue로 분리할 수 있다.

중복 job에는 idempotency를 고려한다.

#### 10. cache 정책을 적는다

가격 데이터:

```text
source of truth/freshness 기준
TTL
실패 시 마지막 정상값 사용 여부
```

를 정한다.

예:

```text
가격이 5분 이내면 fresh
5분 넘으면 stale 표시
새 수집 실패 시 마지막 가격은 보여 주되 "업데이트 지연" 표시
```

이런 정책이 없으면 cache가 맞는지 틀린지 판단하기 어렵다.

#### 11. 테스트 계획을 만든다

unit:

```text
watchlist 중복 방지
memo ownership 검사
price freshness 계산
```

integration:

```text
API → service → test DB
transaction/constraint 확인
```

E2E:

```text
로그인
→ 종목 검색
→ 관심 추가
→ 메모 작성
→ 앱 재실행
→ 메모 유지 확인
```

regression:

```text
발견된 버그마다 재현 테스트 추가
```

#### 12. failure simulation을 한다

다음 실패를 일부러 만든다.

```text
DB 연결 실패
가격 API timeout
잘못된 JSON
중복 요청
네트워크 끊김
외부 API 429
메모 저장 중 서버 오류
```

각 경우에:

```text
사용자에게 무엇이 보이는가?
데이터는 올바른 상태인가?
재시도 가능한가?
로그가 남는가?
```

를 확인한다.

#### 13. AI에게 맡길 수 있는 부분

AI가 잘 도울 수 있는 작업:

```text
DTO/schema 초안
반복적인 CRUD 코드
unit test 초안
UI component 초안
API client 코드
문서 초안
refactoring 후보 제안
```

하지만 결과를 그대로 믿지 않는다.

#### 14. 사람이 반드시 확인해야 하는 부분

```text
요구사항이 맞는가?
권한 경계가 맞는가?
데이터 불변조건이 지켜지는가?
transaction 경계가 맞는가?
race condition이 없는가?
secret이 노출되지 않는가?
실제 테스트가 실행됐는가?
배포된 artifact가 어떤 commit인가?
rollback 가능한가?
```

AI는 구현 속도를 높일 수 있지만 최종 시스템 책임을 대신 가져가지는 않는다.

#### 15. 코드 리뷰 체크리스트

```text
[ ] 함수/모듈 책임이 한 문장으로 설명되는가?
[ ] 외부 입력 validation이 있는가?
[ ] 인증과 권한이 분리되어 확인되는가?
[ ] DB constraint와 transaction이 필요한 곳에 있는가?
[ ] 사용자 값이 SQL/HTML/shell 명령에 직접 섞이지 않는가?
[ ] 비동기 실패와 race condition을 고려했는가?
[ ] timeout/retry/idempotency가 필요한가?
[ ] secret이 source/log에 없는가?
[ ] 테스트가 정상/실패/경계 케이스를 포함하는가?
[ ] 실제 실행하지 않은 검증을 PASS라고 하지 않았는가?
```

#### 16. release 전 체크리스트

```text
[ ] 요구사항과 acceptance criteria 재확인
[ ] unit test 실제 실행
[ ] integration test 실제 실행
[ ] build 실제 성공
[ ] artifact와 commit SHA 연결
[ ] migration 검토
[ ] rollback 절차 확인
[ ] secret/config 확인
[ ] staging smoke test
[ ] 운영 배포는 승인 확인
```

#### 17. 운영 이후 체크리스트

```text
error rate
latency
DB error
외부 API error
queue backlog
CPU/memory
중요 사용자 흐름 성공률
```

배포 성공 메시지 하나로 끝내지 않는다.

#### 18. TRACK 11 완료 기준

다음을 자기 말로 설명하고 작은 프로젝트에 적용할 수 있어야 한다.

- functional/non-functional requirement와 constraint를 구분한다.
- acceptance criteria를 테스트 가능한 문장으로 만든다.
- domain과 model을 구분한다.
- 책임과 모듈 경계를 설계한다.
- abstraction/interface/implementation 차이를 설명한다.
- cohesion과 coupling을 실제 코드 구조로 설명한다.
- dependency injection을 사용해 외부 시스템을 fake로 바꿀 수 있다.
- state transition과 invariant를 정의한다.
- cache의 source of truth와 invalidation 정책을 설명한다.
- queue/retry/idempotency를 같이 설계한다.
- race condition을 발견하고 transaction/atomic operation/lock 등 해결 방향을 고른다.
- bottleneck을 측정한 뒤 최적화한다.
- scale up/out, load balancing의 큰 차이를 설명한다.
- timeout/retry/circuit breaker의 목적을 구분한다.
- logs/metrics/traces로 장애를 추적한다.
- AI가 만든 코드를 요구사항·데이터 흐름·오류 경계·테스트 증거로 검증한다.

### 최종 통과 기준

이 과정을 마치고 나면 모든 문법을 외운 개발자가 될 필요는 없다.

대신 처음 보는 코드와 시스템을 보고 다음 질문을 할 수 있어야 한다.

```text
이 시스템의 목표는 무엇인가?
입력은 어디에서 들어오는가?
데이터는 어디에 저장되는가?
누가 이 데이터를 바꿀 수 있는가?
어떤 상태는 절대 깨지면 안 되는가?
어떤 외부 시스템에 의존하는가?
어디서 실패할 수 있는가?
중복 실행되면 안전한가?
동시에 실행되면 안전한가?
무슨 테스트가 실제로 실행되었는가?
운영에 어떤 commit/artifact가 들어갔는가?
문제가 생기면 어떻게 되돌리는가?
```

이 질문을 할 수 있다면 AI에게 구현을 맡기더라도 결과를 검토하고 방향을 통제할 수 있다.
