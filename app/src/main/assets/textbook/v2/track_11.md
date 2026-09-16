# TRACK 11 · 소프트웨어 설계와 종합 프로젝트

앞의 TRACK들에서 코드, 자료구조, 웹, JavaScript, 네트워크, 서버, DB, 보안, 테스트와 배포를 배웠다.

마지막 TRACK에서는 이것들을 **하나의 시스템으로 묶는 법**을 배운다.

좋은 개발자는 코드를 빨리 쓰는 사람만을 뜻하지 않는다.

```text
무엇을 만들어야 하는지 정확히 정의하고
데이터와 책임의 경계를 나누고
실패를 예상하고
성능과 보안을 고려하고
변경 가능한 구조를 만들고
실제 증거로 검증하는 사람
```

이어야 한다.

AI가 코드를 많이 작성하는 시대에는 이 능력이 더 중요해진다.

---

## BLOCK 01 · 요구사항

### LESSON 01 · 무엇을 만들어야 하는가

사용자나 사업이 원하는 기능과 조건을 **요구사항(requirement)**이라고 부른다.

나쁜 요구사항:

```text
좋은 앱 만들어줘.
```

좋은 방향:

```text
사용자는 이메일로 로그인할 수 있다.
검색 결과는 2초 안에 표시되어야 한다.
권한 없는 사용자는 다른 사용자의 주문을 볼 수 없다.
```

### LESSON 02 · 모호한 말을 측정 가능한 말로

```text
빠르게 → p95 응답 500ms 이하
안전하게 → 관리자 API는 인증+admin 권한 확인
많이 → 동시 사용자 10,000명 목표
```

측정 가능한 조건으로 바꾼다.

---

## BLOCK 02 · functional requirement

### LESSON 01 · 시스템이 무엇을 해야 하는가

**기능 요구사항(functional requirement)**은 사용자가 수행할 수 있는 기능을 정의한다.

예:

```text
회원가입
로그인
상품 검색
주문 생성
결제 취소
```

### LESSON 02 · user story

사용자 관점에서:

```text
사용자로서
상품명을 검색해
원하는 상품을 찾고 싶다.
```

처럼 요구를 표현하는 방식을 **user story**라고 부른다.

---

## BLOCK 03 · non-functional requirement

### LESSON 01 · 기능 외 품질 조건

**비기능 요구사항(non-functional requirement)**은 기능이 아니라 시스템의 품질과 제약을 정의한다.

```text
성능
가용성
보안
접근성
확장성
운영성
데이터 보존
```

### LESSON 02 · 기능은 되는데 서비스는 실패할 수 있다

검색 기능이 정확하지만 2분 걸리면 실제 서비스로 쓰기 어렵다.

기능 요구만 맞고 비기능 요구를 놓치면 시스템 전체가 실패할 수 있다.

---

## BLOCK 04 · constraint

### LESSON 01 · 선택을 제한하는 조건

반드시 지켜야 하는 기술·비용·시간·법적 조건을 **제약(constraint)**이라고 부른다.

예:

```text
Android 10 이상 지원
무료 API만 사용
개인정보 한국 리전에 저장
한 달 운영비 10만원 이하
```

### LESSON 02 · 제약을 늦게 알면 재설계가 생긴다

배포 직전에 `외부 유료 API 사용 금지`를 알게 되면 구조를 크게 바꿔야 할 수 있다.

설계 시작 전에 제약을 수집한다.

---

## BLOCK 05 · acceptance criteria

### LESSON 01 · 언제 완료라고 할 것인가

요구사항이 충족되었는지 판단하는 구체적인 기준을 **acceptance criteria(인수 기준)**라고 부른다.

예:

```text
정상 email/password → 로그인 성공
틀린 password → 401
5회 연속 실패 → rate limit 적용
```

### LESSON 02 · 완료를 느낌으로 판단하지 않는다

`대충 잘 됨` 대신 실제 테스트 가능한 조건을 미리 만든다.

---

## BLOCK 06 · domain

### LESSON 01 · 프로그램이 해결하려는 실제 세계의 문제 영역

병원 CRM, 쇼핑몰, 주식 분석처럼 프로그램이 다루는 업무와 지식 영역을 **도메인(domain)**이라고 부른다.

### LESSON 02 · domain language

업무에서 실제 사용하는 용어를 코드에도 정확히 반영하면 의사소통이 쉬워진다.

```text
Order
Prescription
HospitalVisit
Portfolio
```

모호한 `DataManager2`보다 의미가 분명하다.

---

## BLOCK 07 · model

### LESSON 01 · 실제 세계를 프로그램 안에 표현

현실의 개념 중 프로그램에 필요한 부분만 선택해 표현한 것을 **모델(model)**이라고 부른다.

주문 모델:

```text
id
userId
items
status
totalAmount
```

### LESSON 02 · 현실 전체를 복사하지 않는다

사용자 사람 한 명의 모든 정보를 저장할 필요는 없다.

현재 문제를 해결하는 데 필요한 정보만 모델링한다.

---

## BLOCK 08 · abstraction

### LESSON 01 · 중요하지 않은 세부를 숨긴다

자동차를 운전할 때 엔진 내부 폭발 과정을 매번 생각하지 않고 `가속 페달`을 사용한다.

복잡한 세부를 숨기고 필요한 핵심 동작만 드러내는 것을 **추상화(abstraction)**라고 부른다.

### LESSON 02 · 코드 추상화

```javascript
payment.charge(order)
```

호출자는 카드 API URL, retry, signature 생성 같은 세부를 모두 알 필요가 없다.

`결제한다`는 높은 수준의 기능을 사용한다.

---

## BLOCK 09 · interface

### LESSON 01 · 어떻게 사용할 수 있는지 약속

구성요소가 외부에 제공하는 사용 방법을 **인터페이스(interface)**라고 부른다.

예:

```text
PaymentGateway.charge(amount)
PaymentGateway.refund(paymentId)
```

### LESSON 02 · 구현과 분리

인터페이스는 `무엇을 할 수 있는가`를 말한다.

구현은 `어떻게 하는가`를 말한다.

```text
interface = 계약
implementation = 실제 방법
```

---

## BLOCK 10 · module

### LESSON 01 · 관련 책임을 하나의 경계로

관련 코드와 데이터를 묶어 외부와의 접점을 제한한 단위를 **모듈(module)**이라고 부른다.

예:

```text
auth module
order module
notification module
```

### LESSON 02 · 파일 하나 = 모듈 하나는 아니다

프로젝트 언어와 구조에 따라 여러 파일이 하나의 논리 모듈을 이룰 수 있다.

핵심은 책임과 경계다.

---

## BLOCK 11 · responsibility

### LESSON 01 · 이 코드는 무엇을 책임지는가

구성요소가 맡아야 하는 역할을 **책임(responsibility)**이라고 부른다.

예:

```text
OrderCalculator → 주문 금액 계산
OrderRepository → 주문 저장/조회
OrderController → HTTP 요청/응답 처리
```

### LESSON 02 · 책임이 모호하면

`CommonManager`, `Utils`, `Helper`에 아무 코드나 계속 넣으면 수정 영향 범위가 커진다.

무엇을 책임지는지 이름으로 설명할 수 있어야 한다.

---

## BLOCK 12 · cohesion

### LESSON 01 · 한 모듈 안의 기능이 얼마나 관련 있는가

한 모듈 안의 기능들이 같은 목적에 얼마나 잘 모여 있는지를 **응집도(cohesion)**라고 부른다.

### LESSON 02 · 높은 응집도

`EmailValidator` 안에 이메일 검증 관련 코드만 있다면 응집도가 높다.

거기에 결제 계산과 이미지 압축까지 들어가면 관련성이 낮아진다.

일반적으로 높은 응집도를 목표로 한다.

---

## BLOCK 13 · coupling

### LESSON 01 · 모듈들이 서로 얼마나 강하게 의존하는가

한 구성요소 변경이 다른 구성요소에 얼마나 영향을 주는지를 **결합도(coupling)** 관점에서 본다.

### LESSON 02 · 강한 결합

OrderService가 DB schema, HTTP framework, payment SDK 내부 구조를 모두 직접 알면 하나만 바뀌어도 함께 수정해야 한다.

가능하면 필요한 interface에만 의존해 결합을 낮춘다.

---

## BLOCK 14 · high cohesion, low coupling

### LESSON 01 · 자주 쓰는 설계 방향

```text
관련 기능은 한 곳에 모으고(high cohesion)
서로 불필요하게 강하게 묶지 않는다(low coupling)
```

### LESSON 02 · 무조건 잘게 쪼개면 되는가

아니다.

함수 하나마다 interface와 파일을 만들면 오히려 이해하기 어려워진다.

변경 이유와 책임을 기준으로 적절히 나눈다.

---

## BLOCK 15 · dependency

### LESSON 01 · 한 코드가 다른 코드를 필요로 한다

OrderService가 PaymentGateway를 호출해야 한다면 OrderService는 PaymentGateway에 **의존(depend)**한다.

### LESSON 02 · dependency graph

시스템 구성요소의 의존 방향을 그림으로 표현할 수 있다.

```text
UI
↓
OrderService
↓
OrderRepository
↓
DB
```

순환 의존이 생기면 구조를 이해하고 테스트하기 어려워질 수 있다.

---

## BLOCK 16 · dependency inversion

### LESSON 01 · 높은 수준 규칙이 구체적인 도구에 묶이지 않게

주문 계산 같은 핵심 business logic이 특정 Stripe SDK나 PostgreSQL API에 직접 묶이지 않도록 추상 interface에 의존하게 만들 수 있다.

### LESSON 02 · 예시

```text
OrderService
↓
PaymentGateway interface
↑
StripePaymentGateway implementation
```

OrderService는 Stripe 세부를 모른다.

이런 방향이 **의존성 역전(Dependency Inversion)** 원칙과 연결된다.

---

## BLOCK 17 · dependency injection

### LESSON 01 · 필요한 dependency를 밖에서 전달

```javascript
class OrderService {
  constructor(paymentGateway) {
    this.paymentGateway = paymentGateway;
  }
}
```

OrderService가 내부에서 직접 gateway를 만들지 않고 밖에서 전달받는다.

이 방식을 **dependency injection(DI)**이라고 부른다.

### LESSON 02 · 테스트가 쉬워진다

실제 결제 gateway 대신 fake를 주입할 수 있다.

```text
production → RealPaymentGateway
unit test → FakePaymentGateway
```

---

## BLOCK 18 · layer

### LESSON 01 · 역할을 층으로 나누기

시스템을 역할별 층으로 나누는 설계를 **layered architecture**라고 부른다.

예:

```text
Presentation
Application
Domain
Infrastructure
```

### LESSON 02 · 계층 수 자체가 목표는 아니다

작은 앱에 8개 layer를 만들 필요는 없다.

변경 이유를 분리하고 의존 방향을 명확하게 만드는 것이 목적이다.

---

## BLOCK 19 · Clean Architecture

### LESSON 01 · business rule을 바깥 기술에서 보호

**Clean Architecture**는 핵심 business rule이 UI, DB, framework 같은 바깥 기술에 직접 의존하지 않게 만드는 아키텍처 접근 중 하나다.

### LESSON 02 · 안쪽이 바깥쪽을 모르게

큰 생각:

```text
핵심 business rule
↑
application use case
↑
adapter
↑
DB/framework/UI
```

의존 방향을 안쪽 핵심으로 향하게 만든다.

---

## BLOCK 20 · hexagonal architecture

### LESSON 01 · port와 adapter

**Hexagonal Architecture**에서는 핵심 application이 외부 세계와 통신할 인터페이스를 **port**, 실제 DB/API/UI 연결 구현을 **adapter**라고 부른다.

### LESSON 02 · 목적

DB를 바꾸거나 외부 API가 바뀌어도 핵심 business rule 변화가 작도록 경계를 만든다.

Clean Architecture와 비슷한 목표를 다른 용어로 표현한다.

---

## BLOCK 21 · SOLID를 배우기 전에

### LESSON 01 · 다섯 글자를 외우지 않는다

SOLID는 객체지향 설계에서 자주 언급되는 다섯 원칙의 앞글자다.

원칙 이름보다 `어떤 변경 문제를 줄이려는가`를 하나씩 본다.

---

## BLOCK 22 · Single Responsibility Principle

### LESSON 01 · 변경 이유를 하나로

**SRP**는 클래스나 모듈이 하나의 주요 변경 이유를 갖도록 책임을 모으자는 원칙이다.

```text
InvoiceCalculator = 계산
InvoicePrinter = 출력
```

한 클래스가 계산·출력·DB·이메일을 모두 책임지면 변경 이유가 너무 많다.

---

## BLOCK 23 · Open/Closed Principle

### LESSON 01 · 기존 핵심 코드를 계속 뜯지 않고 확장

새 기능을 추가할 때 기존 안정된 코드를 매번 크게 수정하지 않고 확장할 수 있게 설계하자는 원칙이 **OCP**다.

### LESSON 02 · 무조건 interface를 만들라는 뜻은 아니다

미래 변화가 실제로 예상되는 경계에 추상화를 둔다.

쓸모 없는 추상화는 복잡도만 만든다.

---

## BLOCK 24 · Liskov Substitution Principle

### LESSON 01 · 대체해도 약속이 깨지지 않아야

상위 타입을 기대하는 곳에 하위 구현을 넣어도 동작 약속이 깨지지 않아야 한다는 원칙이 **LSP**다.

예:

```text
PaymentGateway.charge()
```

를 구현한 모든 gateway가 성공·실패 계약을 같은 의미로 지켜야 한다.

---

## BLOCK 25 · Interface Segregation Principle

### LESSON 01 · 쓰지 않는 거대한 interface 강요하지 않기

읽기만 필요한 기능에 `create/delete/admin/resetAll`까지 포함된 거대한 interface를 구현하게 만들지 말자는 원칙이 **ISP**다.

필요한 기능 중심의 작은 interface로 나눌 수 있다.

---

## BLOCK 26 · Dependency Inversion Principle

### LESSON 01 · 다시 연결

높은 수준 policy와 낮은 수준 구현이 모두 적절한 abstraction에 의존하도록 만들자는 원칙이 **DIP**다.

`DB가 바뀌면 business logic까지 바뀌는 구조`를 줄이는 데 목적이 있다.

---

## BLOCK 27 · design pattern

### LESSON 01 · 반복해서 나타나는 설계 문제의 이름 있는 해결 형태

**디자인 패턴(design pattern)**은 자주 반복되는 설계 문제에 대한 검증된 구조적 해결 아이디어다.

### LESSON 02 · 패턴부터 끼워 맞추지 않는다

문제가 없는데 `Factory`, `Strategy`, `Observer`를 억지로 쓰면 코드가 더 복잡해진다.

문제를 먼저 이해하고 필요한 경우 패턴을 선택한다.

---

## BLOCK 28 · Strategy pattern

### LESSON 01 · 여러 알고리즘을 교체 가능하게

할인 방법이 여러 개 있다고 하자.

```text
일반 할인
VIP 할인
쿠폰 할인
```

각 계산 방법을 같은 interface 뒤에 분리하는 것을 **Strategy pattern**으로 설계할 수 있다.

### LESSON 02 · 조건문 폭발 줄이기

```text
if type == ...
elif type == ...
elif type == ...
```

가 계속 늘어날 때 전략 객체로 분리하면 변경 영향이 줄어들 수 있다.

---

## BLOCK 29 · Observer pattern

### LESSON 01 · 상태 변화 알리기

어떤 객체의 상태가 바뀌었을 때 여러 구독자에게 알리는 구조를 **Observer pattern**이라고 부른다.

```text
주문 완료
→ 이메일 알림
→ 재고 시스템
→ 분석 이벤트
```

### LESSON 02 · 이벤트 시스템과 연결

브라우저 event listener, message broker의 publish/subscribe 구조에도 비슷한 생각이 나타난다.

구현 방식은 다를 수 있다.

---

## BLOCK 30 · Repository pattern

### LESSON 01 · 데이터 접근 경계

application code가 SQL 세부에 직접 의존하지 않도록 데이터 저장·조회 기능을 interface로 감싸는 패턴이다.

```text
OrderRepository.findById()
OrderRepository.save()
```

### LESSON 02 · 필요할 때만

ORM 자체가 충분한 abstraction을 제공하는 작은 앱에서는 추가 repository layer가 중복일 수 있다.

패턴은 목적이 있을 때 사용한다.

---

## BLOCK 31 · state management

### LESSON 01 · 상태가 어디에 있는지 정한다

UI 상태, 서버 상태, 사용자 session, cache 등 앱에는 많은 상태가 있다.

상태가 여기저기 중복되면 어느 값이 진짜인지 모르게 된다.

### LESSON 02 · source of truth

어떤 데이터의 최종 기준이 되는 저장 위치를 **single source of truth**라고 부른다.

예:

```text
주문 최종 상태 = 서버 DB
화면 선택 탭 = UI local state
```

---

## BLOCK 32 · derived state

### LESSON 01 · 다른 상태로 계산할 수 있는 값

```text
items = [가격들]
total = items 합계
```

`total`을 별도로 저장하지 않고 필요할 때 items에서 계산할 수 있다.

이런 값을 **derived state**라고 부른다.

### LESSON 02 · 중복 상태 위험

items를 수정하고 total 업데이트를 빼먹으면 서로 모순된다.

가능하면 원본 상태 하나에서 계산한다.

---

## BLOCK 33 · finite state machine

### LESSON 01 · 가능한 상태와 전이를 명시

주문 상태:

```text
CREATED
PAID
SHIPPED
CANCELLED
```

어떤 상태에서 어떤 상태로 이동할 수 있는지 규칙을 명시한 모델을 **상태 머신(state machine)**이라고 부른다.

### LESSON 02 · 불가능한 전이 막기

```text
SHIPPED → CREATED
```

같은 잘못된 전이를 코드에서 막을 수 있다.

복잡한 UI와 workflow에서 강력하다.

---

## BLOCK 34 · refactoring

### LESSON 01 · 외부 동작을 유지하며 내부 구조 개선

기능 결과를 바꾸지 않으면서 코드 내부 구조를 더 이해·수정하기 좋게 바꾸는 작업을 **리팩터링(refactoring)**이라고 부른다.

### LESSON 02 · 테스트 없이 큰 refactoring은 위험

기능을 유지했다는 증거가 필요하다.

회귀 테스트가 있어야 구조를 바꾼 뒤 기존 동작을 확인할 수 있다.

---

## BLOCK 35 · code smell

### LESSON 01 · 버그는 아니지만 문제 신호

코드에서 유지보수 문제 가능성을 나타내는 특징을 **code smell**이라고 부른다.

예:

```text
아주 긴 함수
중복 코드
너무 많은 parameter
거대한 class
boolean flag 여러 개
```

### LESSON 02 · smell 발견 = 무조건 고쳐야 함은 아니다

변경 빈도와 위험, 테스트 상태를 보고 우선순위를 정한다.

---

## BLOCK 36 · premature optimization

### LESSON 01 · 측정 전에 복잡한 최적화

실제로 느리지 않은 코드를 추측만으로 복잡하게 바꾸는 것을 **성급한 최적화(premature optimization)**라고 부른다.

### LESSON 02 · 먼저 측정

```text
실제 latency 측정
profile 확인
병목 위치 확인
최적화
다시 측정
```

순서를 따른다.

---

## BLOCK 37 · profiling

### LESSON 01 · 어디에 시간이 쓰이는지 측정

CPU 시간, memory allocation, network, DB query 등 프로그램 자원 사용을 측정하는 것을 **profiling**이라고 부른다.

### LESSON 02 · profiler

프로파일링을 수행하는 도구를 **profiler**라고 부른다.

느린 함수나 과도한 memory allocation을 찾는다.

---

## BLOCK 38 · memory leak

### LESSON 01 · 필요 없는 메모리를 계속 붙잡음

더 이상 사용하지 않는 객체가 참조 때문에 계속 메모리에 남아 회수되지 않는 문제를 **memory leak**이라고 부른다.

### LESSON 02 · 증상

```text
시간이 지날수록 RAM 사용 증가
GC 반복
앱 느려짐
process 종료
```

같은 현상이 나타날 수 있다.

---

## BLOCK 39 · scalability

### LESSON 01 · 사용자가 늘어도 감당할 수 있는가

요청량, 데이터 크기, 사용자 수가 커져도 시스템이 요구 성능을 유지하도록 확장할 수 있는 성질을 **확장성(scalability)**이라고 부른다.

### LESSON 02 · scale up

한 서버의 CPU/RAM을 더 크게 만드는 것을 **수직 확장(vertical scaling, scale up)**이라고 부른다.

### LESSON 03 · scale out

서버 수를 늘려 부하를 나누는 것을 **수평 확장(horizontal scaling, scale out)**이라고 부른다.

---

## BLOCK 40 · load balancer

### LESSON 01 · 여러 서버에 요청 나누기

여러 서버 앞에서 client 요청을 적절한 서버로 분산하는 구성요소를 **로드 밸런서(load balancer)**라고 부른다.

```text
client
↓
load balancer
↙   ↓   ↘
A    B    C
```

### LESSON 02 · health check와 연결

문제가 있는 서버에는 traffic을 보내지 않도록 health check 결과를 사용할 수 있다.

---

## BLOCK 41 · stateless service

### LESSON 01 · 특정 서버 메모리에 사용자 상태를 묶지 않기

사용자 session을 서버 A memory에만 저장하면 다음 요청이 서버 B로 갔을 때 상태를 모른다.

### LESSON 02 · shared store

session을 Redis 같은 공유 저장소에 두거나 token 기반 설계를 사용하면 어떤 server instance가 요청을 받아도 처리하기 쉬워진다.

무조건 stateless가 정답은 아니지만 수평 확장에 유리한 경우가 많다.

---

## BLOCK 42 · replication

### LESSON 01 · 같은 데이터를 여러 노드에 복사

DB나 서비스 데이터를 여러 서버에 복제하는 것을 **replication**이라고 부른다.

### LESSON 02 · 장점

```text
읽기 부하 분산
장애 시 대체
지역별 가까운 데이터
```

에 도움이 될 수 있다.

### LESSON 03 · consistency 문제

복제는 즉시 완료되지 않을 수 있다.

노드마다 잠시 다른 값을 볼 수 있다.

---

## BLOCK 43 · consistency in distributed systems

### LESSON 01 · 여러 복사본이 얼마나 같은 상태인가

분산 시스템에서 **consistency**는 여러 노드가 데이터의 같은 최신 상태를 얼마나 일관되게 보장하는지를 뜻할 때 사용한다.

TRACK 08 ACID의 C와 같은 단어지만 문맥이 다르다.

### LESSON 02 · strong consistency

쓰기 완료 후 모든 이후 읽기가 최신 값을 보도록 강하게 보장하는 모델을 **strong consistency**라고 부를 수 있다.

### LESSON 03 · eventual consistency

복제 지연 때문에 잠시 다를 수 있지만 업데이트가 없으면 결국 같은 상태로 수렴하는 모델을 **eventual consistency**라고 부른다.

---

## BLOCK 44 · CAP를 배우기 전에

### LESSON 01 · 네트워크가 끊길 수 있다

분산 DB 노드 A와 B 사이 통신이 끊겼다고 하자.

```text
사용자는 A에 주문 저장
다른 사용자는 B에서 주문 조회
```

서로 통신할 수 없을 때 `항상 같은 값`과 `항상 응답`을 동시에 어떻게 할지 선택이 생긴다.

---

## BLOCK 45 · CAP theorem

### LESSON 01 · 세 단어

분산 데이터 시스템에서 자주 다루는 CAP의 의미:

```text
C = Consistency
A = Availability
P = Partition tolerance
```

### LESSON 02 · Partition

노드 사이 네트워크가 끊겨 서로 통신하지 못하는 상황을 **network partition**이라고 부른다.

### LESSON 03 · 핵심

partition이 실제로 발생했을 때 시스템은 최신 일관성을 지키기 위해 일부 요청을 거절하거나, 계속 응답하면서 잠시 서로 다른 데이터를 허용하는 선택을 해야 할 수 있다.

`CAP에서 셋 중 아무 두 개를 고르면 끝`이라고 단순 암기하지 않는다.

---

## BLOCK 46 · cache architecture

### LESSON 01 · cache-aside

application이 먼저 cache를 보고 없으면 DB에서 읽은 뒤 cache에 저장하는 패턴을 **cache-aside**라고 부른다.

```text
cache hit → 바로 반환
cache miss → DB → cache 저장 → 반환
```

### LESSON 02 · invalidation

DB가 바뀔 때 cache를 삭제하거나 갱신해야 한다.

stale data 허용 시간과 consistency 요구를 설계한다.

---

## BLOCK 47 · queue architecture

### LESSON 01 · 시간적으로 분리

요청을 바로 처리하지 않고 queue에 저장하면 producer와 consumer가 같은 속도로 움직일 필요가 없다.

```text
Producer → Queue → Consumer
```

### LESSON 02 · producer와 consumer

작업 메시지를 넣는 쪽을 **producer**, 꺼내 처리하는 쪽을 **consumer**라고 부른다.

### LESSON 03 · at-least-once delivery

메시지가 유실되지 않도록 재전송하면서 같은 메시지가 두 번 전달될 수 있는 보장을 **at-least-once delivery**라고 부른다.

consumer가 idempotent해야 중복 처리 문제가 줄어든다.

---

## BLOCK 48 · exactly-once 오해

### LESSON 01 · 분산 시스템에서 매우 어렵다

`메시지를 정확히 한 번만 처리한다`는 요구는 네트워크 장애와 재시도 때문에 매우 어렵다.

### LESSON 02 · 실무 접근

```text
중복 전달 허용
+ idempotency key
+ transaction/outbox
```

등으로 최종 효과가 한 번만 생기게 설계하는 경우가 많다.

---

## BLOCK 49 · outbox pattern

### LESSON 01 · DB 저장은 성공했는데 event 전송은 실패

주문을 DB에 저장한 뒤 message broker에 `OrderCreated` 이벤트를 보내려 한다.

```text
DB commit 성공
↓
broker 전송 실패
```

하면 데이터와 이벤트가 어긋난다.

### LESSON 02 · outbox

같은 DB transaction 안에서 `orders`와 `outbox_events`를 함께 저장한다.

별도 worker가 outbox를 읽어 broker로 보낸다.

이 패턴을 **Transactional Outbox**라고 부른다.

---

## BLOCK 50 · saga 맛보기

### LESSON 01 · 여러 서비스 transaction

주문, 결제, 재고가 각각 다른 DB를 가진 microservice라고 하자.

하나의 DB transaction으로 모두 묶기 어렵다.

### LESSON 02 · saga

각 서비스의 local transaction을 이어 실행하고 실패하면 앞 단계의 **보상 작업(compensating action)**을 실행하는 접근을 **Saga pattern**이라고 부른다.

예:

```text
재고 예약 성공
결제 실패
→ 재고 예약 취소
```

---

## BLOCK 51 · fault tolerance

### LESSON 01 · 일부가 고장나도 전체가 버티기

component 일부가 실패해도 서비스 전체가 가능한 범위에서 계속 동작하도록 설계하는 성질을 **fault tolerance(내결함성)**라고 부른다.

### LESSON 02 · 실패를 정상 상황처럼 설계한다

```text
network timeout
process crash
DB failover
queue duplicate
external API 503
```

는 언젠가 발생한다고 가정한다.

---

## BLOCK 52 · retry storm

### LESSON 01 · 모두가 동시에 재시도

서버가 느려져 timeout이 발생했다.

수천 client가 즉시 재시도하면 요청이 더 늘어나 장애가 심해질 수 있다.

이를 **retry storm**이라고 부른다.

### LESSON 02 · 방어

```text
exponential backoff
jitter
retry limit
circuit breaker
load shedding
```

등을 조합한다.

---

## BLOCK 53 · load shedding

### LESSON 01 · 감당 못할 때 일부 요청 거절

서버가 이미 한계인데 모든 요청을 받으면 전체가 쓰러질 수 있다.

중요하지 않은 요청 일부를 빠르게 거절해 핵심 기능을 보호하는 것을 **load shedding**이라고 부른다.

### LESSON 02 · 우선순위

```text
결제 처리 > 추천 갱신
로그인 > 분석 이벤트
```

처럼 business 중요도에 따라 자원을 보호할 수 있다.

---

## BLOCK 54 · graceful degradation

### LESSON 01 · 일부 기능이 없어도 핵심은 제공

추천 시스템이 장애여도 상품 목록과 결제는 계속 제공한다.

이처럼 일부 기능을 줄여 전체 서비스는 계속 사용할 수 있게 하는 것을 **graceful degradation**이라고 부른다.

---

## BLOCK 55 · observability architecture

### LESSON 01 · 로그만 많이 남기면 충분하지 않다

시스템 상태를 이해할 수 있게:

```text
logs
metrics
traces
```

를 연결한다.

### LESSON 02 · request ID와 trace ID

사용자 오류 신고 하나에서:

```text
request ID
→ trace
→ 느린 span
→ 관련 DB query
→ deployment version
```

까지 추적할 수 있게 만든다.

---

## BLOCK 56 · security by design

### LESSON 01 · 마지막에 보안 붙이지 않기

설계 시작부터:

```text
trust boundary
least privilege
data classification
secret management
audit log
abuse prevention
```

을 포함한다.

### LESSON 02 · 데이터 최소화

필요하지 않은 개인정보는 처음부터 수집하지 않는다.

저장하지 않은 데이터는 유출될 수도 없다.

---

## BLOCK 57 · privacy by design

### LESSON 01 · 개인정보 보호도 설계 요소

필요한 개인정보만 수집하고, 목적·보존 기간·접근 권한을 명확히 하는 것을 **privacy by design** 관점에서 생각한다.

### LESSON 02 · 데이터 lifecycle

```text
수집
사용
공유
보관
삭제
```

전체 생명주기를 설계한다.

---

## BLOCK 58 · API contract

### LESSON 01 · client와 server의 약속

request/response schema, status code, error format을 문서화한 것을 **API contract**라고 부른다.

### LESSON 02 · schema evolution

새 field를 추가하거나 타입을 바꿀 때 기존 client가 깨지지 않는지 확인한다.

모바일 앱은 사용자가 즉시 update하지 않을 수 있으므로 여러 version이 동시에 존재할 수 있다.

---

## BLOCK 59 · backward compatibility

### LESSON 01 · 새 server가 오래된 client도 지원

서버 update 후에도 이전 앱 version이 동작하는 성질을 **backward compatibility**라고 부른다.

### LESSON 02 · deprecation

오래된 API를 바로 삭제하지 않고 `앞으로 제거 예정`이라고 알리고 전환 기간을 주는 것을 **deprecation**이라고 부른다.

---

## BLOCK 60 · ADR

### LESSON 01 · 왜 이 설계를 골랐는지 기록

중요한 architecture 결정을 짧은 문서로 남기는 형식을 **ADR(Architecture Decision Record)**이라고 부른다.

예:

```text
결정: PostgreSQL 사용
상황: 강한 transaction과 관계 query 필요
대안: MongoDB, SQLite
이유: ...
결과/단점: ...
```

### LESSON 02 · 나중의 나에게 근거를 남긴다

몇 달 뒤 `왜 이 구조를 했지?`라는 질문에 답할 수 있다.

---

## BLOCK 61 · trade-off

### LESSON 01 · 하나를 얻으면 다른 비용이 생긴다

설계에서는 모든 장점을 동시에 최대화하기 어렵다.

```text
강한 consistency ↔ latency/availability 비용
cache ↔ stale data 위험
microservice ↔ 운영 복잡도
엄격한 보안 ↔ 사용자 편의 일부 감소
```

이런 선택 관계를 **trade-off**라고 부른다.

### LESSON 02 · 좋은 설계는 근거 있는 선택

`이게 최고 아키텍처`가 아니라:

```text
우리 요구사항과 제약에서는 이 trade-off를 선택했다.
```

라고 설명할 수 있어야 한다.

---

## BLOCK 62 · YAGNI

### LESSON 01 · 필요하지 않은 미래 기능 미리 만들지 않기

`언젠가 1억 사용자가 올 수 있으니 지금 50 microservice`처럼 아직 필요 없는 복잡도를 미리 만드는 것을 피하자는 원칙을 **YAGNI(You Aren't Gonna Need It)**라고 부른다.

### LESSON 02 · 확장 가능성과 과설계 사이

변경이 어려운 핵심 경계는 잘 설계하되 실제 필요가 없는 기능까지 구현하지 않는다.

---

## BLOCK 63 · KISS

### LESSON 01 · 가능한 한 단순하게

**KISS(Keep It Simple)**는 요구사항을 만족하는 범위에서 불필요한 복잡도를 줄이자는 원칙이다.

### LESSON 02 · 단순 = 대충이 아니다

안전성과 요구사항을 빼먹는 것은 단순화가 아니다.

같은 요구를 더 적은 개념과 경계로 명확하게 해결하는 것이 좋은 단순화다.

---

## BLOCK 64 · DRY

### LESSON 01 · 지식의 중복 줄이기

같은 business rule이 여러 곳에 복사되면 한 곳만 수정해 불일치가 생길 수 있다.

중복을 줄이자는 원칙을 **DRY(Don't Repeat Yourself)**라고 부른다.

### LESSON 02 · 비슷해 보인다고 너무 일찍 합치지 않는다

두 코드가 지금 우연히 비슷하지만 앞으로 다른 이유로 변경된다면 억지로 공통화하면 결합이 커질 수 있다.

`같은 지식인가?`를 확인한다.

---

## BLOCK 65 · AI에게 요구사항 주기

### LESSON 01 · "앱 만들어줘"는 부족하다

AI에게도 정확한 specification이 필요하다.

```text
목표
사용자
기능 요구
비기능 요구
데이터 모델
API contract
실패 처리
보안 경계
테스트 기준
배포 제약
```

을 제공한다.

### LESSON 02 · 불변조건을 명시한다

예:

```text
기존 정상 데이터는 절대 덮어쓰지 않는다.
중복 결제는 발생하면 안 된다.
권한 없는 사용자는 다른 사용자 row를 읽을 수 없다.
```

이런 **불변조건(invariant)**을 AI에게 분명히 준다.

---

## BLOCK 66 · AI 코드 검수 1 · race condition

### LESSON 01 · 겉보기에는 정상

```javascript
let latest;

async function search(q) {
  latest = await api.search(q);
  render(latest);
}
```

빠른 연속 검색에서 이전 요청이 늦게 끝나면 최신 결과를 덮을 수 있다.

### LESSON 02 · 검수 질문

```text
여러 번 동시에 호출되면?
완료 순서가 바뀌면?
취소되면?
component가 사라진 뒤 응답하면?
```

을 확인한다.

---

## BLOCK 67 · AI 코드 검수 2 · validation

### LESSON 01 · type만 맞는다고 안전하지 않다

```typescript
function createOrder(input: OrderInput) {
  db.insert(input);
}
```

TypeScript 타입은 외부 HTTP JSON을 실제로 검증하지 않는다.

### LESSON 02 · 경계 검증

서버 request boundary에서 runtime schema validation을 수행한다.

```text
quantity > 0?
price client 값을 믿어도 되는가?
productId 실제 존재?
필드 길이 제한?
```

을 확인한다.

---

## BLOCK 68 · AI 코드 검수 3 · transaction

### LESSON 01 · 여러 DB write가 따로 실행

```text
재고 감소 성공
주문 저장 실패
```

가 가능한 코드라면 데이터가 깨진다.

### LESSON 02 · 검수 질문

```text
어디부터 어디까지 하나의 transaction인가?
중간 실패 시 rollback 되는가?
외부 API를 transaction 안에서 오래 기다리는가?
retry하면 중복 write가 생기는가?
```

를 확인한다.

---

## BLOCK 69 · AI 코드 검수 4 · N+1

### LESSON 01 · 반복문 안 query

```javascript
for (const order of orders) {
  order.user = await userRepo.findById(order.userId);
}
```

orders가 1000개면 사용자 query도 1000번 실행될 수 있다.

### LESSON 02 · 해결 후보

```text
JOIN
batch IN query
preload
DataLoader
```

실제 query 수와 latency를 측정한다.

---

## BLOCK 70 · AI 코드 검수 5 · 잘못된 password hash

### LESSON 01 · SHA-256 한 번

```javascript
hash = sha256(password)
```

은 일반 파일 hash에는 강하지만 password storage에는 너무 빠르다.

### LESSON 02 · 수정

Argon2id, bcrypt 같은 password hashing algorithm과 random salt, 적절한 cost를 사용한다.

`hash 썼으니 안전`이라는 문장을 믿지 않는다.

---

## BLOCK 71 · AI 코드 검수 6 · 파일 upload

### LESSON 01 · Content-Type만 믿음

```javascript
if (file.mimetype === "image/png") save(file);
```

client가 MIME을 거짓으로 보낼 수 있다.

### LESSON 02 · 검수

```text
size limit
허용 MIME
실제 file signature
안전한 decoder
server-generated filename
실행 불가능한 저장소
virus/malware scanning 필요 여부
```

를 확인한다.

---

## BLOCK 72 · AI 코드 검수 7 · retry

### LESSON 01 · 모든 오류 무한 retry

```javascript
while (true) {
  try { await pay(); break; }
  catch { }
}
```

은 서버 장애를 더 악화시키고 결제를 중복시킬 수 있다.

### LESSON 02 · 검수

```text
retry 가능한 오류인가?
최대 횟수?
backoff+jitter?
idempotency key?
timeout?
circuit breaker?
```

를 본다.

---

## BLOCK 73 · AI 코드 검수 8 · 권한

### LESSON 01 · userId를 request에서 그대로 믿음

```http
GET /users/999/orders
```

현재 사용자가 999의 주문을 볼 권한이 있는지 확인해야 한다.

### LESSON 02 · BOLA/IDOR

객체 ID를 바꿔 다른 사용자의 데이터에 접근하는 취약점을 API 보안에서 **BOLA(Broken Object Level Authorization)** 또는 IDOR 문제와 연결해 부른다.

서버가 object 수준 authorization을 해야 한다.

---

## BLOCK 74 · system design interview 방식

### LESSON 01 · 바로 database부터 고르지 않는다

시스템 설계 순서:

```text
1. 요구사항 질문
2. 규모 추정
3. 핵심 데이터 모델
4. API
5. 높은 수준 component
6. 핵심 데이터 흐름
7. 병목과 failure mode
8. 보안
9. 관측성
10. trade-off
```

### LESSON 02 · 숫자를 대략 계산한다

사용자 100만 명이 하루 20번 요청한다면:

```text
20,000,000 requests/day
```

평균 QPS와 peak를 대략 계산해 서버와 DB 선택 근거를 만든다.

---

## BLOCK 75 · capacity estimation

### LESSON 01 · QPS

초당 처리 요청 수를 **QPS(Queries/Requests Per Second)**라고 부른다.

### LESSON 02 · storage

사용자 한 명당 10KB를 저장하고 1,000만 명이라면 원본 데이터만 약 100GB 규모다.

index, replication, backup은 추가 공간이 필요하다.

### LESSON 03 · bandwidth

응답 하나가 50KB이고 peak 1000 req/s면 약 50MB/s의 응답 데이터가 나갈 수 있다.

정확한 설계 전 대략 규모를 계산한다.

---

## BLOCK 76 · bottleneck analysis

### LESSON 01 · 가장 먼저 한계에 닿는 부분

시스템 전체 성능을 제한하는 부분을 **병목(bottleneck)**이라고 부른다.

후보:

```text
CPU
DB query
lock
network
external API
queue consumer
```

### LESSON 02 · 추측보다 측정

load test와 profiler, metric으로 실제 병목을 찾는다.

---

## BLOCK 77 · load test

### LESSON 01 · 많은 사용자 부하를 만들어 보기

실제 또는 가상의 많은 요청을 보내 시스템 성능과 한계를 측정하는 테스트를 **load test**라고 부른다.

### LESSON 02 · 볼 지표

```text
throughput
p50/p95/p99 latency
error rate
CPU
memory
DB connections
queue depth
```

부하를 늘릴 때 어느 지표가 먼저 무너지는지 본다.

---

## BLOCK 78 · stress test

### LESSON 01 · 정상 목표보다 더 밀어붙이기

시스템이 어느 지점에서 실패하고 어떻게 회복하는지 알아보기 위해 한계를 넘어 부하를 주는 테스트를 **stress test**라고 부른다.

### LESSON 02 · 안전한 환경

production에서 무단으로 stress test를 하면 실제 사용자 장애를 만들 수 있다.

별도 허용된 환경에서 실행한다.

---

## BLOCK 79 · chaos engineering 맛보기

### LESSON 01 · 일부러 실패를 만든다

서버 종료, network 지연, dependency 장애를 통제된 환경에서 일부러 발생시켜 시스템 복구 능력을 검증하는 접근을 **chaos engineering**이라고 부른다.

### LESSON 02 · 무작정 망가뜨리는 것이 아니다

```text
가설
영향 범위
중단 조건
복구 방법
모니터링
```

을 먼저 정한다.

---

## BLOCK 80 · 핵심 용어 사전

| 용어 | 아주 쉬운 뜻 |
|---|---|
| requirement | 시스템이 해야 하는 기능과 조건 |
| functional requirement | 시스템이 무엇을 해야 하는지 정한 기능 요구 |
| non-functional requirement | 성능·보안·가용성 같은 품질 요구 |
| constraint | 설계 선택을 제한하는 반드시 지킬 조건 |
| acceptance criteria | 요구사항 완료 여부를 판단하는 구체적 기준 |
| domain | 프로그램이 해결하려는 실제 업무·문제 영역 |
| model | 현실 개념을 프로그램에 필요한 형태로 표현한 것 |
| abstraction | 불필요한 세부를 숨기고 핵심 기능만 드러내는 것 |
| interface | 구성요소를 사용하는 방법에 대한 계약 |
| responsibility | 구성요소가 맡은 역할 |
| cohesion | 한 모듈 내부 기능들이 같은 목적과 얼마나 관련 있는지 |
| coupling | 모듈들이 서로 얼마나 강하게 의존하는지 |
| dependency injection | 필요한 dependency를 외부에서 전달하는 설계 |
| layer | 역할별로 나눈 시스템 층 |
| Clean Architecture | 핵심 business rule을 외부 기술에서 분리하는 설계 접근 |
| design pattern | 반복되는 설계 문제의 이름 있는 해결 구조 |
| state machine | 가능한 상태와 상태 전이 규칙을 명시한 모델 |
| refactoring | 외부 동작을 유지하며 내부 구조를 개선하는 작업 |
| code smell | 유지보수 문제 가능성을 알려주는 코드 특징 |
| profiling | 실제 자원 사용과 성능 병목을 측정하는 작업 |
| memory leak | 필요 없는 객체가 계속 메모리에 남는 문제 |
| scalability | 사용량 증가에 맞춰 시스템을 확장할 수 있는 성질 |
| load balancer | 여러 서버에 요청을 분산하는 구성요소 |
| replication | 같은 데이터를 여러 노드에 복제하는 것 |
| eventual consistency | 시간이 지나면 여러 복사본이 결국 같은 상태로 수렴하는 모델 |
| CAP | network partition 상황에서 consistency와 availability trade-off를 설명하는 정리 |
| producer | queue에 메시지를 넣는 쪽 |
| consumer | queue에서 메시지를 꺼내 처리하는 쪽 |
| outbox | DB 변경과 event 기록을 같은 transaction에 저장하는 패턴 |
| saga | 여러 서비스 작업을 local transaction과 보상 작업으로 연결하는 패턴 |
| fault tolerance | 일부 실패에도 시스템 전체 기능을 유지하는 성질 |
| graceful degradation | 일부 기능을 줄여 핵심 서비스는 계속 제공하는 방식 |
| ADR | 중요한 architecture 결정과 이유를 기록한 문서 |
| trade-off | 한 장점을 얻는 대신 다른 비용을 감수하는 선택 관계 |
| invariant | 시스템에서 항상 지켜야 하는 조건 |
| QPS | 초당 처리하는 요청 수 |
| load test | 실제 목표 부하에서 성능을 측정하는 테스트 |
| stress test | 한계를 넘어 부하를 줘 실패 지점을 보는 테스트 |

---

## BLOCK 81 · 최종 프로젝트 · 실제 서비스 설계

### LESSON 01 · 프로젝트 주제

다음 기능을 가진 `가족 일정 + 알림 + 공유` 서비스를 설계하고 구현한다.

```text
가족 그룹 생성
초대 링크
일정 CRUD
여러 기기 동기화
작성자 제외 가족 알림
오프라인 수정
충돌 해결
사진 첨부
검색
감사 로그
```

### LESSON 02 · 요구사항

기능 요구:

```text
일정 추가/수정/삭제
월 달력
가족별 표시
공유 초대
push notification
```

비기능 요구:

```text
p95 API 500ms 이하 목표
일정 데이터 유실 금지
권한 없는 그룹 접근 금지
Android 앱 재시작 후 데이터 유지
notification 실패 재시도
```

### LESSON 03 · 데이터 모델

최소 table:

```text
users
families
family_members
invitations
events
event_versions
devices
notification_jobs
audit_logs
```

각 PRIMARY KEY, FOREIGN KEY, UNIQUE, NOT NULL, CHECK를 직접 설계한다.

### LESSON 04 · API

```text
POST /families
POST /families/:id/invitations
POST /events
PATCH /events/:id
DELETE /events/:id
GET /events?from=&to=
POST /devices/push-token
```

각 request/response schema와 status code를 정의한다.

### LESSON 05 · 권한

```text
가족 member만 가족 일정 조회
작성자/권한자만 수정
초대 token 만료
다른 family event ID를 바꿔 접근 못함
```

object 수준 authorization을 테스트한다.

### LESSON 06 · 동기화

기기 A와 B가 offline에서 같은 event를 수정하는 상황을 만든다.

```text
version number
updatedAt
field-level merge
manual conflict resolution
```

중 어떤 정책을 쓸지 근거를 적는다.

### LESSON 07 · notification

```text
DB transaction에서 event 저장
↓
outbox 기록
↓
worker가 notification job 생성
↓
FCM 전송
↓
retry/backoff
↓
실패 기록
```

중복 push가 발생해도 안전한지 설계한다.

### LESSON 08 · observability

모든 API에:

```text
request ID
trace ID
user/family internal ID
status
latency
error code
```

를 남기되 민감정보는 redaction한다.

### LESSON 09 · 테스트

필수:

```text
unit
DB integration
API integration
Android/UI 핵심 E2E
권한 공격 test
race/conflict test
notification retry test
migration test
```

### LESSON 10 · 운영 준비

```text
CI
Release build
artifact SHA
schema migration
backup/restore
canary
SLO
alerts
rollback
incident runbook
```

을 문서화한다.

실제 production deployment는 별도 승인과 운영 절차가 있을 때만 실행한다.

---

## BLOCK 82 · 최종 AI 검수 시험

### LESSON 01 · AI에게 일부 기능 구현을 맡긴다

AI에게 `일정 생성 API`를 구현하게 한다.

단, 바로 채택하지 않는다.

### LESSON 02 · 검수 체크

다음을 직접 찾는다.

```text
입력 validation?
authentication?
authorization?
family membership 확인?
transaction boundary?
중복 event 생성 가능?
N+1 query?
secret logging?
error leakage?
outbox와 event 저장 원자성?
retry 시 idempotency?
```

### LESSON 03 · AI 답을 반박할 수 있어야 한다

AI가 `문제없습니다`라고 말해도 실제 코드와 test가 다르면 AI 설명을 버린다.

최종 판단 기준은 **코드 구조 + 실행 증거 + 요구사항**이다.

---

## BLOCK 83 · 최종 완료 기준

이 전체 과정의 마지막 목표는 `혼자 모든 코드를 외워 작성하는 사람`이 아니다.

다음을 할 수 있는 사람이다.

- 요구사항을 기능/비기능/제약/인수 기준으로 나눈다.
- domain model과 데이터 모델을 설계한다.
- module의 책임과 dependency 방향을 설명한다.
- cohesion/coupling 관점으로 구조를 개선한다.
- interface와 dependency injection을 사용해 외부 기술을 분리한다.
- state와 source of truth를 명확하게 설계한다.
- refactoring 전후 회귀 테스트를 유지한다.
- 실제 profiling으로 병목을 찾는다.
- scale up/out, load balancer, replication의 필요 조건을 설명한다.
- consistency trade-off를 설명한다.
- cache/queue/idempotency/outbox/saga를 적절한 문제에 적용한다.
- timeout/retry/backoff/circuit breaker로 failure를 설계한다.
- logs/metrics/traces로 운영 상태를 추적한다.
- security/privacy를 처음부터 설계에 넣는다.
- AI가 만든 코드에서 race, validation, transaction, N+1, 보안 문제를 잡는다.
- 테스트하지 않은 것을 PASS라고 말하지 않는다.
- 실제 evidence가 없는 완료 보고를 거부한다.

### 최종 졸업 조건

위 가족 일정 서비스를 실제 개발 환경에서 구현하고 다음 증거를 제출한다.

```text
요구사항 문서
ADR
ERD
API contract
권한 matrix
threat model
source commit SHA
automated test 결과
Release build 결과
artifact hash
DB migration test
backup/restore test
load test 결과
security test 결과
CLEAN PASS 결과
남은 위험 목록
```

이 모든 것을 자기 말로 설명할 수 있으면 단순히 `AI에게 코딩을 시키는 사람`이 아니라 **AI의 결과를 설계하고 검증할 수 있는 개발자**가 된 것이다.
