# TRACK 10 · 오류·테스트·Git·빌드·배포

코드를 고칠 줄 아는 것과 **안전하게 고쳐서 실제 사용자에게 전달할 줄 아는 것**은 다르다.

초보자는 종종 이렇게 생각한다.

```text
코드 수정
↓
실행됨
↓
끝
```

하지만 실제 개발은 더 길다.

```text
문제 재현
↓
원인 좁히기
↓
수정
↓
테스트
↓
변경 기록
↓
빌드
↓
배포
↓
운영에서 다시 확인
↓
문제면 되돌리기
```

이 TRACK의 가장 중요한 규칙은 하나다.

> **실제로 실행해서 확인하지 않은 것은 PASS라고 부르지 않는다.**

AI가 코드를 대신 써 주는 시대에는 오히려 이 규칙이 더 중요하다.

---

## BLOCK 01 · 버그를 고치는 첫 단계는 코드를 바꾸는 것이 아니라 사실을 모으는 것이다

### LESSON 01 · bug·symptom·재현·환경·가설·로그·debugger·stack trace를 하나의 조사 절차로 배운다

#### 1. 버그는 `이상하다`가 아니라 기대와 실제의 차이다

메모 앱이 있다고 하자.

우리가 기대하는 동작:

```text
메모를 입력한다.
저장 버튼을 누른다.
앱을 종료한다.
다시 실행한다.
메모가 남아 있다.
```

실제 동작:

```text
다시 실행하면 메모가 사라진다.
```

이처럼 프로그램이 기대한 동작과 다르게 움직이는 결함을 **버그(bug)**라고 부른다.

버그를 설명할 때 가장 먼저 두 줄을 적는다.

```text
Expected: 저장한 메모가 다시 실행해도 남아 있어야 한다.
Actual: 앱을 다시 실행하면 메모가 사라진다.
```

이 두 줄만 있어도 문제 범위가 훨씬 명확해진다.

#### 2. 증상과 원인을 섞지 않는다

`메모가 사라진다`는 **증상(symptom)**이다.

원인은 여러 개일 수 있다.

```text
저장 함수가 호출되지 않음
DB insert 실패
잘못된 사용자 ID로 저장
저장은 됐지만 조회가 잘못됨
transaction rollback
앱 종료 전에 비동기 저장이 끝나지 않음
```

따라서:

```text
증상 = 화면에서 보이는 현상
원인 = 그 현상을 만든 실제 이유
```

로 나눈다.

`메모가 사라지니까 DB가 문제다`라고 바로 결론 내리면 조사 범위를 잘못 잡을 수 있다.

#### 3. 먼저 같은 문제를 다시 만들 수 있어야 한다

버그를 같은 조건에서 다시 일으켜 보는 것을 **재현(reproduction)**이라고 부른다.

좋은 재현 절차는 다른 사람도 그대로 따라 할 수 있어야 한다.

예:

```text
1. 앱 데이터 초기화
2. 사용자 A로 로그인
3. 새 메모 작성
4. 내용에 "hello" 입력
5. 저장 버튼 누름
6. 앱 강제 종료
7. 다시 실행
8. 메모 목록 확인
9. "hello"가 없는지 확인
```

나쁜 재현:

```text
앱 쓰다 보면 가끔 사라짐
```

이 문장만으로는 어떤 조건이 필요한지 알 수 없다.

#### 4. 발생 빈도도 데이터다

버그가:

```text
10번 중 10번
```

발생하는지:

```text
100번 중 2번
```

발생하는지도 중요하다.

항상 발생하면 특정 코드 경로를 의심하기 쉽다.

가끔 발생하면:

```text
race condition
네트워크 timing
동시성
특정 기기
특정 데이터
```

같은 조건을 더 의심하게 된다.

#### 5. 환경을 기록한다

같은 코드라도 환경이 다르면 결과가 달라질 수 있다.

버그 보고에는 가능한 범위에서 다음을 적는다.

```text
앱 버전
commit/version
OS 버전
기기 모델
브라우저 버전
서버 버전
DB schema version
네트워크 조건
로그인 계정 종류
```

예를 들어 개발자 PC에서는 정상인데 특정 Android 기기에서만 실패할 수 있다.

이 경우 코드를 무작정 바꾸기보다:

```text
권한 차이
백그라운드 제한
파일 경로
화면 크기
OS API 차이
```

를 확인해야 할 수 있다.

#### 6. 사실과 가설을 분리한다

실제 확인한 것:

```text
Network 패널에서 POST /save가 500을 반환했다.
```

이것은 **사실**이다.

아직 확인하지 않은 설명:

```text
DB connection이 끊겨서 500이 난 것 같다.
```

이것은 **가설(hypothesis)**이다.

가설을 사실처럼 말하면 디버깅이 꼬인다.

좋은 기록:

```text
FACT
POST /save → HTTP 500 확인

HYPOTHESIS
DB write 단계에서 실패할 가능성

NEXT TEST
서버 로그에서 request ID 기준 오류 위치 확인
```

#### 7. 한 가설에 한 실험을 한다

가설:

```text
버튼 click handler가 호출되지 않는다.
```

검증:

```javascript
function save() {
  console.log("save handler entered");
  // ...
}
```

버튼을 누른다.

로그가 찍힌다.

그러면:

```text
handler 미호출 가설
→ 기각
```

할 수 있다.

코드 20군데를 동시에 바꾸면 문제가 사라져도 무엇이 원인이었는지 모른다.

가능하면 한 번에 하나의 가설을 검증한다.

#### 8. 범위를 절반씩 줄이는 방법

처리 흐름이 길다고 하자.

```text
입력
→ validation
→ service
→ DB
→ cache
→ response
```

DB 직전 값이 정상인지 본다.

정상이라면 문제는 뒤쪽일 가능성이 크다.

잘못됐다면 앞쪽으로 돌아간다.

이처럼 조사 범위를 계속 좁히는 사고를 사용할 수 있다.

```text
1~100 중 문제
↓ 중간 확인
1~50 정상
↓
51~100 조사
```

이런 접근을 binary isolation처럼 생각할 수 있다.

#### 9. 로그는 `실제로 어떤 길을 지나갔는가`를 보여 준다

예:

```javascript
console.log("save:start", { id });
console.log("save:validated", { id });
console.log("save:db-before", { id });
console.log("save:db-after", { id });
```

실제 로그:

```text
save:start
save:validated
save:db-before
```

까지만 있고 `db-after`가 없다.

그러면 DB 작업 주변에서 실패했을 가능성을 좁힐 수 있다.

하지만 로그에 secret을 찍으면 안 된다.

```text
password
access token
private key
card number
```

는 진단 로그에도 남기지 않는다.

#### 10. debugger는 실행을 멈추고 상태를 직접 본다

**debugger**는 프로그램을 실행하면서 특정 줄에서 멈추고 내부 상태를 조사하는 도구다.

멈출 위치를 **breakpoint**라고 부른다.

예를 들어:

```javascript
const total = calculateTotal(items);
const discount = calculateDiscount(total, user);
const finalPrice = total - discount;
```

`finalPrice`가 이상하면 `calculateDiscount` 앞뒤에 breakpoint를 걸어 값을 확인할 수 있다.

#### 11. step over / into / out은 실행 경로를 따라가는 방법이다

`step over`:

```text
현재 줄을 실행하고 다음 줄로 간다.
함수 내부에는 들어가지 않는다.
```

`step into`:

```text
현재 줄에서 호출하는 함수 내부로 들어간다.
```

`step out`:

```text
현재 함수의 나머지를 진행해 호출한 쪽으로 돌아간다.
```

예:

```text
saveOrder()
  ↓ step into
validateOrder()
  ↓ step out
saveOrder()
```

이런 식으로 최초로 값이 잘못되는 지점을 찾는다.

#### 12. watch로 중요한 값을 계속 본다

디버거에서 특정 변수나 표현식을 계속 표시하도록 만들 수 있다.

예:

```text
cart.total
user.id
order.status
request.retryCount
```

정상 흐름에서는:

```text
order.status
DRAFT → PAID → COMPLETED
```

이어야 하는데:

```text
DRAFT → COMPLETED
```

로 건너뛰면 상태 변경 위치를 찾을 수 있다.

#### 13. stack trace는 오류까지 어떤 함수들이 이어졌는지 보여 준다

오류:

```text
TimeoutError
```

만 보면 정보가 적다.

stack trace:

```text
saveOrder
→ chargeCard
→ sendPaymentRequest
→ TimeoutError
```

를 보면 어떤 호출 경로를 통해 오류까지 왔는지 알 수 있다.

외부 library에서 마지막 오류가 났더라도 내 코드가 잘못된 값을 넘겼을 수 있다.

그래서 stack trace에서:

```text
내 코드가 library를 호출한 지점
```

도 확인한다.

#### 14. 최소 재현은 큰 앱을 작은 실험으로 줄인다

앱 전체에서만 발생하는 문제를 작은 코드로 줄여 본다.

예를 들어 복잡한 업로드 화면에서 crash가 나는데 원인이 이미지 크기 처리인지 확인하고 싶다.

앱 전체를 테스트하지 않고:

```text
이미지 한 장 선택
↓
resize 함수만 호출
↓
같은 crash 발생 여부 확인
```

하는 작은 예를 만든다.

이런 **minimal reproduction**은 원인 후보를 줄이고 다른 사람도 빠르게 확인하게 만든다.

#### 15. assertion은 `절대 나오면 안 되는 내부 상태`를 찾는 도구다

예:

```python
assert total >= 0
```

이 코드는 개발자 관점에서:

```text
이 시점의 total은 논리상 음수일 수 없다.
```

라는 가정을 검사한다.

사용자가 잘못된 값을 입력하는 정상적인 오류 처리와는 다르다.

사용자 입력은 친절하게 validation 오류로 처리해야 한다.

#### 16. 책을 덮고 확인한다

1. 증상과 원인은 무엇이 다른가?
2. 재현 절차에는 왜 구체적인 단계가 필요한가?
3. 사실과 가설을 나눠 기록하는 이유는?
4. 로그가 찍히지 않는 마지막 지점으로 무엇을 추론할 수 있는가?
5. debugger의 breakpoint는 무엇인가?
6. stack trace의 마지막 줄만 보면 안 되는 이유는?
7. 최소 재현이 원인 분석에 도움이 되는 이유는?

---

## BLOCK 02 · 테스트는 `몇 개 눌러 봤는데 됨`을 반복 가능한 증거로 바꾸는 일이다

### LESSON 01 · unit·integration·E2E·boundary·regression·test double·property testing을 실제 예제로 연결한다

#### 1. 테스트는 왜 필요한가

계산 함수를 고쳤다고 하자.

```javascript
function total(price, count) {
  return price * count;
}
```

직접 한 번 실행했다.

```text
price=1000
count=3
→ 3000
```

잘 된다.

그런데 이것만으로 모든 입력이 정상이라고 말할 수 있을까?

아니다.

```text
count=0
price=0
음수 입력
아주 큰 값
숫자가 아닌 값
```

은 아직 확인하지 않았다.

**테스트(test)**는 특정 조건에서 프로그램이 기대한 결과를 내는지 반복 가능하게 확인하는 절차다.

중요한 문장:

> 테스트 PASS는 `실행한 조건에서 기대 결과가 나왔다`는 증거이지, 세상 모든 조건에서 버그가 없다는 증명은 아니다.

#### 2. 가장 작은 단위를 빠르게 확인하는 unit test

함수 하나를 검사한다.

```javascript
function add(a, b) {
  return a + b;
}
```

테스트:

```javascript
expect(add(2, 3)).toBe(5);
```

이처럼 함수나 클래스 같은 비교적 작은 단위를 다른 시스템과 최대한 분리해 검사하는 것을 **unit test**라고 부른다.

장점:

```text
빠르다
실패 원인 범위가 작다
많이 실행하기 쉽다
```

#### 3. 실제 연결을 확인하는 integration test

주문 API가 있다고 하자.

```text
HTTP request
→ handler
→ service
→ DB
→ response
```

각 함수의 unit test는 모두 통과했다.

그런데 실제 연결에서는:

```text
DB column 이름 불일치
JSON parsing 오류
transaction 설정 오류
```

가 있을 수 있다.

여러 구성요소를 연결한 상태로 검사하는 것을 **integration test**라고 부른다.

예:

```text
POST /orders
실제 test DB 저장
응답 201 확인
DB row 확인
```

#### 4. 사용자 흐름 전체를 보는 E2E test

실제 사용자의 시작부터 끝까지 확인한다.

```text
브라우저 열기
↓
로그인
↓
상품 검색
↓
장바구니 추가
↓
주문 버튼
↓
완료 화면
```

이런 넓은 흐름을 **E2E(end-to-end) test**라고 부른다.

장점:

```text
사용자 관점의 큰 흐름을 확인
```

단점:

```text
느림
실패 원인 범위가 큼
UI 변경에 깨지기 쉬움
환경 의존성 큼
```

그래서 unit, integration, E2E를 역할에 맞게 섞는다.

#### 5. test double은 실제 위험한 dependency를 대신한다

결제 API를 테스트한다고 하자.

실제 결제 서버를 호출하면 진짜 돈이 결제될 수 있다.

테스트에서는 대체물을 사용한다.

이런 대체물을 넓게 **test double**이라고 부른다.

대표적인 말:

```text
stub
mock
fake
```

초급에서는 역할만 구분한다.

`stub`:

```text
정해진 입력에 정해진 응답을 돌려주는 단순 대체물
```

`mock`:

```text
어떤 호출이 몇 번 일어났는지 같은 상호작용을 검사할 수 있는 대체물
```

`fake`:

```text
실제처럼 동작하지만 단순화된 구현
예: 메모리 DB
```

#### 6. mock을 너무 많이 사용하면 테스트가 구현에 묶인다

예를 들어 주문 기능의 목적은:

```text
주문을 만들면 최종 금액과 상태가 올바르다.
```

인데 테스트가:

```text
A 함수 정확히 1회 호출
B 함수 정확히 2회 호출
C 함수 앞에 D 함수 호출
```

처럼 내부 호출 순서에 지나치게 묶이면 코드를 더 좋은 구조로 refactoring만 해도 테스트가 깨질 수 있다.

가능하면 외부 행동과 중요한 계약을 확인한다.

#### 7. 경계값에서 버그가 많이 난다

허용 나이가 1~120이라고 하자.

중간값 50만 테스트하면 부족하다.

경계를 본다.

```text
0  → 바로 아래
1  → 최소 허용
120 → 최대 허용
121 → 바로 위
```

목록이라면:

```text
0개
1개
2개
매우 많음
```

을 본다.

이런 검사를 **boundary test** 관점으로 생각할 수 있다.

#### 8. 입력을 같은 행동 그룹으로 나눌 수 있다

나이 입력:

```text
음수 → 거절
1~120 → 허용
121 이상 → 거절
글자 → parsing 실패
```

같은 결과를 기대하는 입력 그룹으로 나누고 대표값을 고르는 사고를 **equivalence partitioning**이라고 부른다.

모든 숫자를 하나씩 테스트하지 않아도 중요한 그룹을 체계적으로 고를 수 있다.

#### 9. regression test는 고친 버그가 다시 돌아오지 못하게 한다

버그:

```text
할인율 100%일 때 가격이 음수가 됨
```

수정했다.

그런데 나중에 다른 개발자가 계산 코드를 다시 바꾸면서 같은 버그가 돌아올 수 있다.

그래서 이 버그를 재현하는 테스트를 남긴다.

```text
입력
price=10000
discount=100%

기대
final=0
```

이런 테스트를 **regression test**로 남기면 다음 변경에서 같은 문제가 돌아오면 자동으로 잡을 수 있다.

#### 10. property-based testing은 몇 개 예보다 `항상 지켜야 할 성질`을 본다

정렬 함수가 있다고 하자.

특정 예만 테스트한다.

```text
[3,1,2] → [1,2,3]
```

좋지만 입력은 무한히 많다.

정렬 결과에는 더 일반적인 성질이 있다.

```text
결과 길이는 입력 길이와 같다.
결과는 오름차순이다.
입력에 있던 값의 개수는 결과에도 같다.
```

도구가 다양한 입력을 생성해 이런 성질을 확인하는 방식을 **property-based testing**이라고 부른다.

#### 11. 테스트가 flaky하면 신뢰가 무너진다

같은 코드인데 테스트가:

```text
이번엔 PASS
다음엔 FAIL
또 PASS
```

한다면 **flaky test**일 수 있다.

원인:

```text
시간 의존
공유 데이터
네트워크
비동기 timing
테스트 순서 의존
```

등이 있을 수 있다.

사람들이 `저 테스트는 원래 가끔 깨져`라고 생각하기 시작하면 진짜 오류도 무시하게 된다.

그래서 flaky test는 방치하지 않는다.

#### 12. 테스트 데이터를 독립적으로 만든다

나쁜 테스트:

```text
테스트 A가 user 1 생성
테스트 B가 user 1이 있다고 가정
```

A 없이 B를 실행하면 실패한다.

가능하면 각 테스트가 필요한 상태를 직접 준비하고 끝나면 정리한다.

테스트 순서에 의존하지 않게 만든다.

#### 13. 테스트에서 외부 시스템을 실제로 변경하지 않는다

테스트가 실제 운영 DB 데이터를 삭제하거나 실제 이메일을 수천 통 보내면 안 된다.

환경을 분리한다.

```text
production
staging/test
local
```

실제 결제·문자·이메일 같은 외부 행동은 mock/fake/test account 등 안전한 수단을 사용한다.

#### 14. 테스트를 고칠 때 코드를 맞추려고 기대값을 함부로 바꾸지 않는다

코드 수정 후 테스트가 실패했다.

초보자가 하기 쉬운 실수:

```text
테스트가 실패하네
→ 테스트 기대값을 새 결과로 바꿈
→ PASS
```

하지만 새 결과가 틀린 것일 수 있다.

먼저 질문한다.

```text
요구사항이 바뀌었나?
코드가 틀렸나?
테스트가 틀렸나?
```

근거를 확인하고 수정한다.

#### 15. 좋은 테스트 이름은 조건과 기대를 설명한다

나쁜 이름:

```text
test1
works
orderTest
```

좋은 방향:

```text
rejectsNegativeQuantity
returnsZeroWhenCartIsEmpty
preventsUserFromReadingAnotherUsersOrder
```

실패 메시지만 보고도 무엇이 깨졌는지 이해하기 쉬워진다.

#### 16. 책을 덮고 확인한다

1. unit/integration/E2E의 범위 차이를 설명해 보라.
2. test double은 왜 필요한가?
3. boundary test에서 1~120 나이를 어떤 값으로 시험할 수 있는가?
4. regression test가 필요한 이유는?
5. flaky test를 방치하면 어떤 문제가 생기는가?
6. 테스트 PASS가 버그 0개를 의미하지 않는 이유는?

---

## BLOCK 03 · Git은 코드를 저장하는 폴더가 아니라 변경의 역사를 관리하는 도구다

### LESSON 01 · repository·working tree·commit·branch·merge·conflict·pull request를 작은 변경 하나로 배운다

#### 1. 파일 복사로 버전을 관리하면 금방 무너진다

초보 프로젝트에서 이런 파일을 본 적이 있을 수 있다.

```text
app_final.zip
app_final2.zip
app_real_final.zip
app_real_final_last.zip
```

어느 것이 진짜 최신인지 알기 어렵다.

누가 무엇을 바꿨는지도 알기 어렵다.

Git은 파일의 변경 이력을 체계적으로 기록하고 여러 변경 흐름을 관리하는 **버전 관리 시스템(version control system)**이다.

#### 2. repository는 Git이 관리하는 프로젝트 공간이다

Git으로 관리되는 프로젝트를 보통 **repository(repo)**라고 부른다.

repo에는 현재 파일뿐 아니라 commit으로 기록된 변경 역사도 있다.

GitHub는 Git repository를 원격 서버에 보관하고 협업 기능을 제공하는 서비스다.

```text
Git
→ 버전 관리 도구

GitHub
→ Git repo hosting + 협업 서비스
```

같은 말이 아니다.

#### 3. working tree는 지금 내가 수정 중인 파일 상태다

파일을 연다.

```text
README.md 수정
app.js 수정
```

아직 commit하지 않았다.

이 상태는 작업 중인 working tree의 변경이다.

Git은:

```text
어떤 파일이 바뀌었는가?
새 파일은 무엇인가?
삭제된 파일은 무엇인가?
```

를 추적할 수 있다.

#### 4. commit은 의미 있는 변경 단위를 기록한다

예를 들어 버튼 버그를 고쳤다.

좋은 commit은:

```text
Fix save button double-submit
```

처럼 하나의 의미 있는 변경을 담는다.

commit에는 변경 내용과 부모 commit 정보 등이 연결되어 역사가 만들어진다.

`commit = 백업 버튼` 정도로만 이해하지 않는다.

> **특정 시점의 프로젝트 상태와 그 변화의 의미를 기록하는 단위**

이라고 생각한다.

#### 5. commit을 너무 크게 만들면 검토하기 어렵다

하나의 commit에:

```text
로그인 수정
UI 전면 변경
DB schema 수정
라이브러리 업데이트
파일 이름 정리
```

를 모두 넣으면 어떤 변경이 어떤 문제를 만들었는지 찾기 어렵다.

가능하면 논리적으로 관련된 변경을 묶는다.

#### 6. branch는 다른 변경 흐름을 분리한다

현재 main이 안정적인 상태다.

새 기능을 만들고 싶다.

바로 main에서 모든 것을 바꾸기보다:

```text
main
  └─ feature/search
```

처럼 별도 branch에서 작업할 수 있다.

branch는 프로젝트 전체를 복사한 새 폴더라기보다 Git history에서 별도의 변경 흐름을 가리키는 이름이라고 생각하면 된다.

#### 7. branch에서 작업한 뒤 merge한다

```text
main: A ─ B ─ C
             \
feature:      D ─ E
```

feature의 변경을 main에 합치는 작업이 **merge**다.

서로 다른 부분을 바꿨다면 Git이 자동으로 합칠 수 있다.

같은 줄을 서로 다르게 바꾸면 사람이 결정해야 할 수 있다.

#### 8. merge conflict는 `Git이 망가졌다`가 아니다

main:

```javascript
const title = "상품";
```

feature:

```javascript
const title = "상품 목록";
```

다른 branch에서도 같은 줄을:

```javascript
const title = "제품";
```

으로 바꿨다면 Git은 어떤 것이 맞는지 알 수 없다.

이것이 **merge conflict**다.

사람이 요구사항을 보고 최종 코드를 결정한다.

#### 9. conflict 해결 후에는 반드시 실행한다

충돌 표식을 없앴다고 완료가 아니다.

두 변경을 합치면서 논리는 깨질 수 있다.

따라서:

```text
conflict 해결
↓
compile/build
↓
test
↓
실제 중요한 흐름 확인
```

까지 한다.

#### 10. pull request는 변경을 main에 합치기 전에 검토하는 공간이다

GitHub에서 branch의 변경을 다른 branch에 합치자고 요청하는 기능을 **pull request(PR)**라고 부른다.

PR에서는:

```text
무엇을 바꿨는가?
왜 바꿨는가?
어떤 테스트를 했는가?
위험한 부분은 무엇인가?
```

를 검토할 수 있다.

#### 11. diff를 읽는 습관이 중요하다

AI가 20개 파일을 수정했다.

`완료했습니다`라는 말을 믿고 바로 merge하면 안 된다.

**diff**를 본다.

```text
추가된 줄
삭제된 줄
바뀐 파일
예상하지 못한 변경
```

을 확인한다.

특히:

```text
secret 추가
테스트 삭제
권한 완화
데이터 삭제 코드
설정 변경
```

이 숨어 있지 않은지 본다.

#### 12. commit hash는 특정 commit을 식별한다

Git commit에는 고유한 hash가 있다.

예:

```text
41885ce...
```

`어떤 코드가 배포됐는가?`를 추적할 때:

```text
branch 이름만
```

보다:

```text
정확한 commit SHA
```

가 더 정확한 기준이 된다.

branch는 앞으로 움직일 수 있기 때문이다.

#### 13. revert와 reset은 같은 것이 아니다

이미 공유된 history에서 특정 변경을 되돌릴 때는 새로운 commit으로 반대 변경을 만드는 **revert**를 사용할 수 있다.

`reset`은 branch가 가리키는 history 위치를 바꾸는 동작이라 협업 중에는 더 주의가 필요하다.

초급에서는:

```text
공유 history를 함부로 다시 쓰지 않는다.
```

는 원칙을 먼저 잡는다.

#### 14. Git에 secret을 올렸다가 삭제해도 끝이 아닐 수 있다

API key를 commit했다.

다음 commit에서 줄을 삭제했다.

현재 파일에서는 안 보인다.

하지만 과거 commit history에 남아 있을 수 있다.

따라서 secret이 commit되었다면:

```text
노출된 key 폐기/회전
필요 시 history 정리
재발 방지
```

가 필요하다.

#### 15. Git 작업의 안전한 기본 흐름

```text
1. 최신 기준 확인
2. 별도 branch 생성
3. 작은 변경
4. diff 확인
5. local test
6. commit
7. PR
8. CI 확인
9. review
10. merge
```

프로젝트 규모에 따라 달라질 수 있지만 큰 흐름은 유용하다.

#### 16. 책을 덮고 확인한다

1. Git과 GitHub는 무엇이 다른가?
2. commit은 왜 작은 의미 단위가 좋은가?
3. branch를 사용하는 이유는?
4. merge conflict가 발생하는 이유는?
5. conflict 문구를 없앤 뒤 테스트해야 하는 이유는?
6. diff에서 어떤 위험 변경을 확인해야 하는가?
7. commit SHA가 배포 추적에 유용한 이유는?

---

## BLOCK 04 · 코드가 사용자 앱이 되기까지는 build·artifact·CI·release·deployment·migration·rollback이 이어진다

### LESSON 01 · 소스 코드에서 실제 배포본까지의 전 과정을 한 번에 추적한다

#### 1. 소스 코드와 사용자가 설치하는 파일은 같은 것이 아니다

Android 앱 소스가 있다.

```text
.kt
.xml
이미지
설정 파일
```

사용자는 이 소스 폴더를 그대로 설치하지 않는다.

빌드 도구가 소스와 리소스를 처리해서 설치 가능한 결과물을 만든다.

이 과정을 **build**라고 부른다.

결과물 예:

```text
APK
AAB
JAR
웹 bundle
Docker image
```

#### 2. artifact는 빌드가 만들어 낸 전달 가능한 결과물이다

CI에서 앱을 빌드했다.

결과:

```text
app-release.apk
```

이처럼 build 결과로 만들어져 보관·테스트·배포할 수 있는 파일을 **artifact**라고 부른다.

중요한 질문:

```text
이 artifact는 어떤 commit에서 만들어졌나?
어떤 설정으로 만들어졌나?
어떤 테스트를 통과했나?
```

#### 3. 같은 branch 이름이라도 artifact가 다를 수 있다

오전 10시 main:

```text
commit A
```

오후 3시 main:

```text
commit B
```

둘 다 `main`이라고 부를 수 있다.

그래서 운영 배포를 추적할 때:

```text
main 배포됨
```

보다:

```text
commit B에서 만든 artifact XYZ 배포됨
```

이 더 정확하다.

#### 4. CI는 변경마다 반복 검증을 자동화한다

**CI(Continuous Integration)**는 코드 변경이 합쳐질 때 자동으로:

```text
compile
lint
unit test
integration test
build
```

같은 검사를 실행하도록 만들 수 있는 개발 방식과 시스템이다.

목적은 `버튼 한 번 안 눌러도 된다`가 아니다.

> **사람마다 다르게 검사하지 않고, 같은 검증을 반복 가능하게 실행한다.**

가 핵심이다.

#### 5. CI가 초록색이라고 무조건 안전한 것은 아니다

CI가 PASS했다.

하지만 test suite에 중요한 테스트가 없을 수 있다.

잘못된 환경을 검사했을 수도 있다.

그래서:

```text
CI PASS
=
정의된 CI 검사가 성공
```

이지:

```text
세상 모든 것이 완벽
```

은 아니다.

#### 6. build failure와 test failure를 구분한다

build failure:

```text
문법 오류
컴파일 오류
dependency 해결 실패
리소스 오류
```

일 수 있다.

테스트 failure:

```text
빌드는 됐지만 기대 동작과 다름
```

일 수 있다.

어느 단계에서 실패했는지 구분해야 원인을 빨리 좁힌다.

#### 7. dependency는 내 코드가 사용하는 외부 코드다

프로젝트는 library를 사용한다.

```text
HTTP client
JSON parser
UI framework
DB driver
```

이런 외부 구성요소를 **dependency**라고 부른다.

버전을 올리면 API나 동작이 바뀔 수 있다.

그래서:

```text
dependency version 고정
lock file
release note 확인
테스트
```

가 중요하다.

#### 8. semantic version을 절대 법칙처럼 믿지 않는다

많은 프로젝트에서:

```text
MAJOR.MINOR.PATCH
```

형태의 semantic versioning을 사용한다.

대략:

```text
MAJOR = 큰 호환성 변화
MINOR = 호환되는 기능 추가
PATCH = 호환되는 수정
```

의 의미를 기대하지만 모든 package가 완벽하게 지킨다는 보장은 없다.

업데이트 후 실제 테스트한다.

#### 9. release는 배포할 버전을 정리하는 단계다

특정 commit과 artifact를 사용자에게 전달할 공식 버전으로 정한다.

예:

```text
version 1.4.0
commit abc123
artifact SHA-256 xyz...
```

release note에는:

```text
무엇이 바뀌었는가
알려진 문제
migration 필요 여부
```

등을 기록할 수 있다.

#### 10. deployment는 artifact를 실행 환경에 실제로 반영하는 일이다

서버 앱이라면:

```text
새 Docker image
↓
production server에 반영
```

Android라면 배포 채널에 새 앱 패키지를 올리는 과정이 있을 수 있다.

중요한 점:

```text
코드 merge
≠
production 배포
```

이다.

main에 코드가 있어도 실제 사용자가 보는 서버는 이전 artifact일 수 있다.

#### 11. migration은 데이터 구조도 함께 바꿀 때 필요하다

코드에서 새로운 column을 사용한다.

```text
users.nickname
```

그런데 production DB에는 column이 없다.

코드만 배포하면 실패한다.

DB schema를 변경하는 절차를 **migration**으로 관리할 수 있다.

예:

```sql
ALTER TABLE users ADD COLUMN nickname TEXT;
```

migration은 데이터 손실 위험이 있기 때문에 더 신중해야 한다.

#### 12. `코드 rollback`만 하면 DB도 자동 복구되는 것은 아니다

새 버전이 DB schema를 바꿨다.

문제가 생겨 코드만 이전 버전으로 되돌렸다.

이전 코드가 새 schema와 호환되지 않을 수 있다.

따라서 배포 설계에서는:

```text
코드 호환성
DB migration 방향
backward compatibility
rollback 가능성
```

을 함께 생각한다.

#### 13. rollback은 실패를 인정하는 기능이 아니라 안전장치다

새 배포에서 심각한 문제가 발생했다.

빠르게 이전 안정 버전으로 돌아가는 절차를 **rollback**이라고 부른다.

좋은 운영은:

```text
문제가 절대 안 생기게 만들기
```

만 목표로 하지 않는다.

```text
문제가 생겨도 빨리 감지하고 피해를 줄이며 복구하기
```

도 중요하다.

#### 14. 배포 직후 smoke test를 한다

production 배포 성공 메시지가 나왔다.

그것만 믿지 않는다.

핵심 기능을 짧게 확인한다.

```text
홈페이지 응답
로그인
핵심 조회 API
DB write 하나
주요 화면 로딩
```

이런 빠른 확인을 **smoke test**라고 부를 수 있다.

#### 15. 운영 검증에는 모니터링도 필요하다

새 버전이 배포된 뒤:

```text
error rate
response latency
CPU/memory
DB error
로그인 실패율
```

이 갑자기 바뀌는지 본다.

테스트 환경에서는 나타나지 않은 문제를 production traffic에서 발견할 수 있다.

#### 16. 실제 배포본이 무엇인지 증거로 남긴다

좋은 배포 기록:

```text
version: 1.4.0
commit: abc1234
artifact: app-release.apk
artifact sha256: ...
deploy id: ...
deployed at: ...
smoke test: PASS
```

이런 정보가 있으면 `내 코드가 진짜 운영에 들어갔나?`를 확인할 수 있다.

#### 17. PASS라는 말에는 범위를 붙인다

나쁜 보고:

```text
전부 PASS
```

좋은 보고:

```text
unit test 120/120 PASS
integration test 18/18 PASS
Android 14 emulator smoke 10/10 PASS
production deployment는 아직 실행하지 않음
```

무엇을 실제로 검증했고 무엇은 아직인지 분리한다.

#### 18. TRACK 10 완료 기준

다음을 할 수 있어야 한다.

- 증상과 원인을 구분한다.
- 재현 절차와 환경 정보를 기록한다.
- 사실과 가설을 분리한다.
- 로그와 debugger로 실패 지점을 좁힌다.
- unit/integration/E2E 역할을 구분한다.
- boundary/regression/flaky test를 설명한다.
- Git repo, commit, branch, merge, conflict를 설명한다.
- diff를 보고 위험 변경을 찾는다.
- commit과 artifact, deployment가 다른 단계임을 설명한다.
- CI PASS의 의미와 한계를 설명한다.
- DB migration과 rollback 위험을 설명한다.
- 배포 후 smoke test와 운영 모니터링을 한다.
- 실제 실행하지 않은 것을 PASS라고 보고하지 않는다.

### TRACK 프로젝트 · 버그 수정부터 안전 배포 계획까지

문제:

```text
앱에서 저장 버튼을 빠르게 두 번 누르면 주문이 두 번 생성된다.
```

진행:

```text
1. 재현 절차 작성
2. 20회 반복해 발생 빈도 기록
3. Network/서버 로그로 중복 POST 확인
4. 사실과 가설 분리
5. 최소 재현 작성
6. 수정 전 실패 regression test 작성
7. 중복 방지 로직 수정
8. unit/integration test 실행
9. 별도 branch에서 diff 검토
10. commit 생성
11. CI에서 동일 테스트 실행
12. build artifact와 commit SHA 연결
13. 배포 전 rollback 계획 작성
14. 배포 후 smoke test 항목 작성
15. 실제 배포는 별도 승인 전 실행하지 않음
```

이 프로젝트의 핵심은 버그를 `고쳤다`고 말하는 것이 아니다.

**재현 → 수정 → 테스트 → 변경기록 → artifact → 배포 검증을 증거로 연결할 수 있어야 통과다.**
