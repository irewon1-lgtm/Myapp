# TRACK 10 · 오류·테스트·Git·빌드·배포

코드를 고치는 능력과 코드를 **안전하게 변경하는 능력**은 다르다.

이 TRACK에서는 한 변경이 실제 사용자에게 전달되기까지의 전체 흐름을 배운다.

```text
문제 재현
↓
원인 좁히기
↓
수정
↓
테스트
↓
Git 기록
↓
CI
↓
빌드
↓
artifact
↓
배포
↓
운영 검증
↓
문제 시 rollback
```

핵심 규칙은 하나다.

> 실제로 실행하지 않은 검증을 PASS라고 부르지 않는다.

---

## BLOCK 01 · bug

### LESSON 01 · 기대와 실제의 차이

프로그램이 의도한 동작과 다르게 움직이는 결함을 **버그(bug)**라고 부른다.

```text
Expected: 저장 후 다시 열어도 메모가 남는다.
Actual: 앱 재시작 후 메모가 사라진다.
```

### LESSON 02 · 증상과 원인

`메모가 사라짐`은 **증상(symptom)**이다.

원인은 저장 함수 미호출, DB 오류, transaction 실패, 잘못된 조회 등 여러 가지일 수 있다.

보이는 현상을 원인이라고 부르지 않는다.

---

## BLOCK 02 · 재현

### LESSON 01 · 같은 문제를 다시 만든다

같은 조건에서 문제를 다시 일으켜 보는 것을 **재현(reproduction)**이라고 부른다.

좋은 재현 절차:

```text
1. 앱 데이터 초기화
2. 사용자 A 로그인
3. 메모 "hello" 입력
4. 저장 누름
5. 앱 강제 종료
6. 다시 실행
7. 메모가 없는지 확인
```

### LESSON 02 · 빈도

```text
10회 중 10회 실패
100회 중 3회 실패
```

처럼 발생 빈도도 기록한다.

가끔만 생기는 버그는 시간 순서와 race condition을 의심할 수 있다.

---

## BLOCK 03 · 환경 기록

### LESSON 01 · 같은 코드도 환경에 따라 다를 수 있다

버그 보고에 다음을 적는다.

```text
앱 버전
OS 버전
브라우저/기기
서버 버전
DB schema version
네트워크 조건
```

### LESSON 02 · 재현이 안 되면 환경 차이를 찾는다

개발자 PC에서는 정상인데 사용자 Galaxy에서만 실패한다면 코드가 동일해도 OS 권한·화면 크기·백그라운드 정책이 다를 수 있다.

---

## BLOCK 04 · 최소 재현

### LESSON 01 · 문제에 필요 없는 부분을 지운다

큰 앱 전체가 아니라 버그를 만들 수 있는 최소 코드와 조건만 남긴 예를 **최소 재현(minimal reproduction)**이라고 부른다.

### LESSON 02 · 왜 강력한가

관련 없는 기능을 제거하면 원인 후보가 줄어든다.

다른 사람도 빠르게 실행해 같은 문제를 확인할 수 있다.

---

## BLOCK 05 · 사실과 가설

### LESSON 01 · 사실

실제 측정·로그·코드에서 확인한 내용이다.

```text
Network 패널에서 POST /save가 500을 반환했다.
```

### LESSON 02 · 가설

아직 검증되지 않은 원인 설명을 **가설(hypothesis)**이라고 부른다.

```text
DB connection pool이 고갈되었을 수 있다.
```

가설을 사실처럼 쓰지 않는다.

---

## BLOCK 06 · 한 가설에 한 실험

### LESSON 01 · 여러 곳을 동시에 고치지 않는다

가설:

```text
버튼 handler가 호출되지 않는다.
```

실험:

```text
handler 첫 줄에 로그 추가 → 버튼 클릭
```

로그가 찍히면 가설을 기각할 수 있다.

### LESSON 02 · 변경 통제

코드 20곳을 동시에 바꾸고 문제가 사라지면 어떤 변경이 원인이었는지 모른다.

가능하면 변수 하나씩 통제한다.

---

## BLOCK 07 · binary isolation

### LESSON 01 · 범위를 절반으로 나누기

처리 단계가 100개라면 중간 지점의 데이터가 맞는지 본다.

```text
1~50 정상 → 문제는 51~100
50에서 이미 오류 → 문제는 1~50
```

### LESSON 02 · 기능 절반 끄기

A+B+C+D 조합에서 실패한다면:

```text
A+B만 → 정상
C+D만 → 실패
```

로 범위를 줄인다.

---

## BLOCK 08 · 로그 디버깅

### LESSON 01 · 실행 경로 기록

```javascript
console.log("save:start", { id });
```

처럼 필요한 위치에 로그를 넣어 실제 실행 흐름을 확인한다.

### LESSON 02 · 민감정보는 출력하지 않는다

```text
password
access token
private key
card number
```

는 디버깅 로그에서도 남기지 않는다.

---

## BLOCK 09 · debugger

### LESSON 01 · 실행을 멈추고 내부를 본다

**debugger**는 프로그램을 실행하면서 특정 지점에서 멈추고 변수와 호출 경로를 조사하는 도구다.

### LESSON 02 · breakpoint

실행을 멈출 코드 위치를 **breakpoint**라고 부른다.

버그 직전 지점에 걸어 상태를 직접 확인한다.

---

## BLOCK 10 · step over / into / out

### LESSON 01 · step over

현재 줄을 실행하되 호출한 함수 안으로 들어가지 않고 다음 줄로 이동한다.

### LESSON 02 · step into

현재 줄이 호출하는 함수 내부로 들어간다.

### LESSON 03 · step out

현재 함수 실행을 마치고 호출한 함수로 돌아간다.

이 세 기능으로 처음 상태가 틀어지는 함수 위치를 찾는다.

---

## BLOCK 11 · watch

### LESSON 01 · 특정 표현식 계속 보기

변수나 표현식의 현재 값을 디버거에 계속 표시하도록 하는 기능을 **watch**라고 부른다.

```text
cart.total
user.id
request.state
```

상태가 예상과 달라지는 순간을 찾는다.

---

## BLOCK 12 · stack trace

### LESSON 01 · 어떤 함수 경로로 오류까지 왔나

오류가 발생했을 때 호출된 함수 경로를 보여주는 기록을 **stack trace**라고 부른다.

```text
saveOrder
→ chargeCard
→ sendRequest
→ TimeoutError
```

### LESSON 02 · 마지막 줄만 보지 않는다

외부 library 내부에서 오류가 났어도 내가 잘못된 값을 넘긴 것이 원인일 수 있다.

stack trace에서 내 코드가 library를 호출한 첫 지점을 찾는다.

---

## BLOCK 13 · assertion

### LESSON 01 · 내부 가정 확인

반드시 참이어야 하는 개발자 가정을 코드로 검사하는 것을 **assertion**이라고 부른다.

```python
assert total >= 0
```

### LESSON 02 · 사용자 입력 검증과 다르다

사용자가 잘못된 값을 입력하는 것은 예상 가능한 상황이다.

사용자에게 오류 메시지를 보여주고 정상 처리해야 한다.

assertion은 주로 `이 상태는 프로그램 논리상 절대 나오면 안 됨`을 찾는 데 사용한다.

---

## BLOCK 14 · 테스트

### LESSON 01 · 기대 동작을 반복 가능하게 확인

프로그램이 특정 조건에서 기대한 결과를 내는지 확인하는 절차를 **테스트(test)**라고 부른다.

### LESSON 02 · 테스트 성공 = 버그 0개는 아니다

실행한 테스트는 `그 조건에서 기대 결과가 나왔다`는 증거다.

시험하지 않은 조건까지 모두 정상이라는 증명은 아니다.

---

## BLOCK 15 · unit test

### LESSON 01 · 작은 코드 단위

함수·클래스 같은 작은 단위를 다른 구성요소와 최대한 분리해 검사하는 테스트를 **unit test**라고 부른다.

```javascript
expect(add(2, 3)).toBe(5);
```

### LESSON 02 · 빠른 feedback

unit test는 보통 빠르기 때문에 코드 변경마다 자주 실행할 수 있다.

실패하면 원인 범위도 작다.

---

## BLOCK 16 · integration test

### LESSON 01 · 구성요소 연결 검사

API handler, service, DB처럼 여러 부분을 연결한 상태에서 확인하는 테스트를 **integration test**라고 부른다.

```text
POST /orders
→ service
→ test DB
→ response 확인
```

### LESSON 02 · unit만으로 못 잡는 것

SQL schema mismatch, 실제 JSON parsing, transaction 동작 같은 문제는 unit test mock만으로 놓칠 수 있다.

---

## BLOCK 17 · E2E test

### LESSON 01 · 사용자 시작부터 끝까지

**E2E(end-to-end) test**는 실제 사용자 흐름 전체를 넓게 검사한다.

```text
브라우저 열기
→ 로그인
→ 상품 검색
→ 주문
→ 완료 화면
```

### LESSON 02 · 느리고 깨지기 쉽다

E2E는 많은 시스템을 지나므로 실행 시간이 길고 원인 파악이 어렵다.

unit/integration과 적절히 나눠 사용한다.

---

## BLOCK 18 · test double

### LESSON 01 · 실제 구성요소 대신 가짜 사용

테스트에서 실제 결제 서버를 호출하면 돈이 결제될 수 있다.

실제 dependency 대신 테스트용 대체물을 **test double**이라고 부른다.

### LESSON 02 · mock

예상 호출과 결과를 설정해 상호작용을 검사하는 test double을 **mock**이라고 부른다.

### LESSON 03 · fake와 stub

`stub`은 정해진 응답을 돌려주는 간단한 대체물이다.

`fake`는 실제처럼 동작하지만 단순화된 구현이다.

예: 메모리 DB.

---

## BLOCK 19 · mock 남용

### LESSON 01 · 내부 구현만 테스트하게 될 수 있다

함수 내부 호출 하나하나를 mock으로 고정하면 refactoring만 해도 테스트가 깨질 수 있다.

### LESSON 02 · 행동을 테스트한다

가능하면:

```text
입력했을 때 사용자에게 어떤 결과가 보이는가?
DB 최종 상태가 올바른가?
```

같은 외부 행동을 중심으로 테스트한다.

---

## BLOCK 20 · boundary test

### LESSON 01 · 경계에서 오류가 많다

허용 나이가 1~120이라면:

```text
0
1
120
121
```

을 시험한다.

### LESSON 02 · empty / one / many

목록 알고리즘에서는:

```text
0개
1개
2개
매우 많음
```

을 따로 확인한다.

---

## BLOCK 21 · equivalence partition

### LESSON 01 · 입력을 같은 행동 그룹으로 나누기

나이 입력:

```text
0 이하 → 거절
1~120 → 허용
121 이상 → 거절
글자 → 변환 실패
```

같은 행동을 기대하는 그룹으로 입력을 나누는 것을 **동등 분할(equivalence partitioning)**이라고 부른다.

모든 숫자를 하나씩 테스트하지 않아도 대표값을 고를 수 있다.

---

## BLOCK 22 · property-based testing

### LESSON 01 · 예제 몇 개보다 성질을 정의

정렬 함수라면 특정 배열 결과만 검사하지 않고:

```text
결과 길이는 입력과 같다.
결과는 오름차순이다.
입력의 각 값 개수는 결과에도 같다.
```

같은 **성질(property)**을 정의할 수 있다.

### LESSON 02 · 무작위 입력 생성

도구가 다양한 입력을 자동 생성해 property가 깨지는 사례를 찾을 수 있다.

이 방식을 **property-based testing**이라고 부른다.

---

## BLOCK 23 · regression test

### LESSON 01 · 고친 버그가 돌아오지 않게

기존에 정상인 기능이 코드 변경 후 다시 깨지는 것을 **regression(회귀)**이라고 부른다.

### LESSON 02 · 버그 발견 시 테스트 추가

가능하면:

```text
수정 전 → 새 테스트 실패
수정 후 → 새 테스트 성공
```

을 만든다.

같은 버그가 다시 들어오는 것을 자동으로 감지할 수 있다.

---

## BLOCK 24 · flaky test

### LESSON 01 · 코드가 안 바뀌었는데 가끔 실패

같은 commit에서 테스트가 어떤 때는 성공하고 어떤 때는 실패하면 **flaky test**일 수 있다.

원인:

```text
시간 의존
race condition
외부 network
공유 DB
무작위 값
```

### LESSON 02 · 다시 돌려 초록색 만들지 않는다

재실행 성공은 원인 해결이 아니다.

실패 조건을 재현하고 제거한다.

---

## BLOCK 25 · test coverage

### LESSON 01 · 어떤 코드가 테스트 중 실행되었나

테스트 실행 중 코드의 몇 %가 실제 지나갔는지 측정한 값을 **coverage**라고 부른다.

### LESSON 02 · 100% coverage = 완벽한 테스트가 아니다

```javascript
if (age >= 19) return "adult";
return "minor";
```

두 줄을 모두 실행했다고 경계값 19를 올바르게 검증했다는 뜻은 아니다.

coverage는 빈 영역을 찾는 힌트이지 품질 점수 그 자체가 아니다.

---

## BLOCK 26 · test pyramid

### LESSON 01 · 빠른 테스트를 많이, 느린 테스트를 적절히

전통적인 **테스트 피라미드(test pyramid)**는:

```text
많은 unit tests
중간 정도 integration tests
적은 수의 핵심 E2E tests
```

구조를 제안한다.

### LESSON 02 · 절대 법칙은 아니다

앱 구조와 리스크에 따라 적절한 비율은 다르다.

핵심은 빠른 feedback과 실제 통합 검증을 둘 다 가지는 것이다.

---

## BLOCK 27 · Git

### LESSON 01 · 코드 변경 역사를 기록

**Git**은 파일 변경 이력을 commit 단위로 저장하고 여러 작업 흐름을 관리하는 분산 버전 관리 시스템이다.

### LESSON 02 · repository

Git이 변경 이력을 관리하는 프로젝트 저장 공간을 **repository(repo)**라고 부른다.

---

## BLOCK 28 · working tree

### LESSON 01 · 지금 편집 중인 파일 상태

현재 파일 시스템에 보이는 프로젝트 파일 상태를 **working tree**라고 부른다.

아직 commit하지 않은 변경이 있을 수 있다.

### LESSON 02 · git status

```bash
git status
```

으로 어떤 파일이 수정·추가·삭제되었고 staging 상태인지 확인한다.

변경 전에 자주 보는 안전 명령이다.

---

## BLOCK 29 · staging area

### LESSON 01 · 다음 commit에 넣을 변경 선택

working tree 변경 중 다음 commit에 포함할 내용을 준비하는 영역을 **staging area(index)**라고 부른다.

```bash
git add app.js
```

### LESSON 02 · 모든 변경을 한 commit에 넣지 않는다

서로 다른 목적의 수정은 가능한 한 별도 commit으로 나누면 review와 rollback이 쉬워진다.

---

## BLOCK 30 · commit

### LESSON 01 · 프로젝트 상태의 기록점

준비된 변경을 하나의 이력으로 저장하는 것을 **commit**이라고 부른다.

```bash
git commit -m "fix: prevent duplicate payment"
```

### LESSON 02 · commit hash

각 commit에는 내용과 부모 이력 등을 바탕으로 만들어지는 고유한 hash 식별자가 있다.

예:

```text
a1b2c3d...
```

어떤 코드 상태를 정확히 가리키는 데 사용한다.

---

## BLOCK 31 · branch

### LESSON 01 · 독립된 변경 흐름

**branch**는 특정 commit을 가리키며 독립된 개발 흐름을 만들 수 있는 이름 있는 포인터다.

```text
main
feature/login
fix/payment-race
```

### LESSON 02 · branch는 프로젝트 전체 복사본이 아니다

Git 내부에서는 commit 이력과 포인터로 관리한다.

사용자는 독립 작업 공간처럼 사용할 수 있다.

---

## BLOCK 32 · merge

### LESSON 01 · branch 변경 합치기

다른 branch의 변경 이력을 현재 branch에 통합하는 것을 **merge**라고 부른다.

### LESSON 02 · merge commit

두 branch가 갈라졌다가 합쳐질 때 두 부모를 가진 merge commit이 만들어질 수 있다.

프로젝트 정책에 따라 squash merge나 rebase merge를 사용할 수도 있다.

---

## BLOCK 33 · merge conflict

### LESSON 01 · Git이 자동 결정할 수 없는 충돌

두 branch가 같은 줄을 다르게 바꾸면 Git이 어떤 내용을 선택할지 모를 수 있다.

이를 **merge conflict**라고 부른다.

### LESSON 02 · 사람이 의미를 판단한다

충돌 표시:

```text
<<<<<<<
내 branch
=======
다른 branch
>>>>>>>
```

를 보고 단순히 한쪽을 지우는 것이 아니라 최종 코드가 의도대로 동작하는지 판단한다.

---

## BLOCK 34 · rebase

### LESSON 01 · commit 기반을 다시 옮기기

**rebase**는 한 branch의 commit들을 다른 기준 commit 위에 다시 적용해 선형 이력을 만드는 Git 기능이다.

### LESSON 02 · 공유된 이력 주의

이미 다른 사람이 사용하는 public branch를 rebase하면 commit hash가 바뀌어 혼란이 생길 수 있다.

팀 정책을 따른다.

---

## BLOCK 35 · remote

### LESSON 01 · 다른 Git repository 위치

GitHub 같은 서버의 repository를 **remote**로 등록할 수 있다.

기본 이름으로 `origin`을 많이 사용한다.

### LESSON 02 · fetch와 pull

```text
fetch = remote의 새 commit 정보를 가져오되 내 branch에 자동 merge하지 않음
pull = fetch 후 현재 branch에 merge/rebase까지 진행
```

안전하게 상태를 볼 때 fetch를 먼저 사용할 수 있다.

---

## BLOCK 36 · push

### LESSON 01 · local commit을 remote에 보내기

```bash
git push origin feature/login
```

local branch commit을 remote repository에 전송한다.

### LESSON 02 · push 전에 확인

```text
branch 이름
commit 목록
secret 포함 여부
테스트 결과
```

를 확인한다.

---

## BLOCK 37 · pull request

### LESSON 01 · 변경을 검토하고 합치기 위한 요청

GitHub의 **Pull Request(PR)**는 branch 변경을 다른 branch에 합치기 전에 diff·테스트·리뷰를 함께 검토하는 기능이다.

### LESSON 02 · 코드만 보지 않는다

좋은 PR에는:

```text
왜 바꿨는가
무엇이 바뀌었나
어떻게 테스트했나
스크린샷/로그
남은 위험
```

가 포함될 수 있다.

---

## BLOCK 38 · diff

### LESSON 01 · 이전과 무엇이 달라졌나

파일의 변경된 줄을 비교한 결과를 **diff**라고 부른다.

```text
- old code
+ new code
```

### LESSON 02 · diff review

commit 전에 diff를 보면 디버그 코드, secret, 실수로 삭제한 파일을 찾을 수 있다.

---

## BLOCK 39 · .gitignore

### LESSON 01 · Git 추적 대상에서 제외

빌드 결과, local secret file, IDE 임시 파일 등을 Git이 추적하지 않게 규칙을 적는 파일이 `.gitignore`다.

### LESSON 02 · 이미 commit된 파일은 자동 제거되지 않는다

`.gitignore`에 추가해도 이미 Git이 추적 중인 파일은 계속 추적된다.

추적 상태를 따로 제거해야 한다.

---

## BLOCK 40 · tag

### LESSON 01 · 특정 commit에 버전 이름 붙이기

Git의 **tag**는 특정 commit에 `v1.0.0` 같은 이름을 붙이는 기능이다.

release 버전을 표시하는 데 많이 사용한다.

### LESSON 02 · branch와 차이

branch는 개발이 진행되며 가리키는 commit이 이동한다.

tag는 보통 특정 commit을 고정해 가리킨다.

---

## BLOCK 41 · dependency

### LESSON 01 · 내 프로그램이 다른 package에 기대고 있다

외부 library가 없으면 내 프로그램이 실행되지 않을 수 있다.

이 관계를 **dependency**라고 부른다.

### LESSON 02 · version range

```text
^1.2.3
>=1.2 <2
```

처럼 허용 version 범위를 지정할 수 있다.

범위가 넓으면 새 version이 자동 선택될 수 있다.

---

## BLOCK 42 · lock file

### LESSON 01 · 실제 설치 version 고정

package manager가 실제로 선택한 dependency version을 기록한 파일을 **lock file**이라고 부른다.

예:

```text
package-lock.json
pnpm-lock.yaml
poetry.lock
```

### LESSON 02 · 재현성

팀원과 CI가 같은 dependency 조합을 설치할 수 있게 해 빌드 **재현성(reproducibility)**을 높인다.

---

## BLOCK 43 · build

### LESSON 01 · source를 실행·배포 결과물로

소스 코드, 자원, dependency를 검사·변환·묶어 실행 가능한 결과물을 만드는 과정을 **build**라고 부른다.

### LESSON 02 · build 단계

프로젝트에 따라:

```text
compile
lint
type check
test
resource processing
minification
packaging
```

등이 들어간다.

---

## BLOCK 44 · compiler와 bundler

### LESSON 01 · compiler

소스 코드를 다른 실행·처리 가능한 형태로 변환하는 도구를 **compiler**라고 부른다.

### LESSON 02 · bundler

여러 JavaScript/CSS/module 자원을 분석해 배포용 파일 묶음으로 만드는 도구를 **bundler**라고 부른다.

예:

```text
Vite
Webpack
Rollup
```

---

## BLOCK 45 · lint

### LESSON 01 · 실행 전 코드 문제 패턴 검사

코드를 실행하지 않고 문법·스타일·위험 패턴을 검사하는 도구를 **linter**라고 부른다.

예:

```text
ESLint
ktlint
Ruff
```

### LESSON 02 · lint PASS = 기능 정상은 아니다

linter는 규칙 위반을 찾는다.

실제 API 호출·DB transaction·UI 흐름이 정상이라는 증거는 아니다.

---

## BLOCK 46 · artifact

### LESSON 01 · build 결과 파일

빌드가 만든 실제 결과 파일을 **artifact**라고 부른다.

예:

```text
APK
AAB
JAR
Docker image
ZIP
```

### LESSON 02 · source commit과 연결한다

운영에 배포한 artifact가 어느 Git commit에서 만들어졌는지 추적할 수 있어야 한다.

```text
commit SHA → CI run → artifact hash → deployed version
```

연결이 중요하다.

---

## BLOCK 47 · Debug와 Release

### LESSON 01 · Debug build

개발 중 디버깅 편의를 위한 build다.

상세 로그, 디버거 연결, 최적화 차이가 있을 수 있다.

### LESSON 02 · Release build

실제 사용자에게 배포할 build다.

최적화와 서명, 환경 설정이 다를 수 있다.

```text
Debug PASS ≠ Release PASS
```

Release 자체를 검증해야 한다.

---

## BLOCK 48 · signing

### LESSON 01 · artifact 발행자와 무결성 확인

Android APK 등은 디지털 서명을 사용한다.

같은 앱 update는 같은 서명 key 계열로 서명되어야 설치가 이어질 수 있다.

### LESSON 02 · signing key 보호

private signing key가 유출되면 공격자가 정상 발행자인 것처럼 악성 앱을 서명할 위험이 있다.

코드 repository에 넣지 않고 안전하게 관리한다.

---

## BLOCK 49 · version

### LESSON 01 · 어떤 build인지 구분

사용자에게 보이는 version name과 시스템이 update 순서를 판단하는 version code가 따로 있을 수 있다.

### LESSON 02 · semantic versioning

프로젝트가 SemVer를 따르면:

```text
MAJOR.MINOR.PATCH
```

형태로 호환성 의미를 표현한다.

모든 프로젝트가 반드시 SemVer를 쓰는 것은 아니다.

---

## BLOCK 50 · CI

### LESSON 01 · 변경마다 자동 검증

개발자 변경을 자주 통합하면서 자동으로 build·test·lint를 실행하는 방식을 **CI(Continuous Integration)**라고 부른다.

### LESSON 02 · CI pipeline

자동 작업의 순서를 **pipeline**이라고 부른다.

예:

```text
checkout
→ dependency install
→ lint
→ unit test
→ integration test
→ build
→ artifact upload
```

---

## BLOCK 51 · CI 실패 읽기

### LESSON 01 · 빨간 표시만 보지 않는다

어느 step에서 실패했는지 확인한다.

```text
install failure?
compile failure?
test failure?
artifact upload failure?
```

### LESSON 02 · 첫 번째 의미 있는 오류

하나의 compile 오류 때문에 뒤에서 수백 개 오류가 연쇄로 생길 수 있다.

처음 의미 있는 실패를 찾는다.

---

## BLOCK 52 · environment

### LESSON 01 · dev/staging/prod

서비스는 목적에 따라 여러 실행 환경을 둘 수 있다.

```text
development = 개발
staging = 운영과 비슷한 사전 검증
production = 실제 사용자 운영
```

### LESSON 02 · 설정을 섞지 않는다

개발 DB key를 production에서 사용하거나 production payment API를 테스트에서 호출하면 큰 사고가 날 수 있다.

환경별 secret과 URL을 분리한다.

---

## BLOCK 53 · deployment

### LESSON 01 · 실제 실행 환경에 새 version 반영

빌드된 artifact를 서버나 앱 배포 시스템에 반영해 사용자가 사용할 수 있게 만드는 과정을 **배포(deployment)**라고 부른다.

### LESSON 02 · build success ≠ deploy success

artifact 생성은 성공했지만 서버 업로드, migration, health check에서 실패할 수 있다.

단계를 각각 확인한다.

---

## BLOCK 54 · migration in deployment

### LESSON 01 · 코드와 DB schema 순서

새 코드가 새 column을 기대한다면 DB migration이 먼저 필요할 수 있다.

하지만 오래된 code가 새 schema와도 동작해야 무중단 배포가 쉬워진다.

### LESSON 02 · expand-contract pattern

안전한 schema 변경에 다음 패턴을 사용할 수 있다.

```text
1. 새 column 추가(expand)
2. 새/구 코드 모두 동작
3. data backfill
4. 새 코드로 전환
5. 오래된 column 제거(contract)
```

한 번에 breaking schema 변경을 하지 않는다.

---

## BLOCK 55 · rollout

### LESSON 01 · 모든 사용자에게 한 번에 배포하지 않기

새 version을 일부 instance나 사용자에게 먼저 보내 점차 늘리는 것을 **점진적 rollout**이라고 부른다.

### LESSON 02 · canary

아주 작은 비율에 새 version을 먼저 배포해 metric과 error를 관찰하는 방식을 **canary deployment**라고 부른다.

문제가 있으면 전체 사용자에게 퍼지기 전에 중단할 수 있다.

---

## BLOCK 56 · blue-green deployment

### LESSON 01 · old와 new 환경을 동시에 준비

```text
Blue = 현재 운영 version
Green = 새 version
```

두 환경을 준비한 뒤 traffic을 Green으로 바꾸는 방식을 **blue-green deployment**라고 부른다.

### LESSON 02 · rollback

문제가 있으면 traffic을 다시 Blue로 돌려 빠르게 되돌릴 수 있다.

DB schema 호환성이 깨졌다면 단순 traffic 전환만으로 해결되지 않을 수 있다.

---

## BLOCK 57 · feature flag

### LESSON 01 · 배포와 기능 공개를 분리

코드는 production에 배포하되 특정 사용자에게만 기능을 켜는 설정을 **feature flag**라고 부른다.

### LESSON 02 · 장점

```text
내부 사용자만 먼저 사용
10% 사용자 rollout
문제 시 기능만 off
```

가 가능하다.

오래된 flag를 방치하면 코드 복잡도가 늘어난다.

---

## BLOCK 58 · rollback

### LESSON 01 · 이전 정상 상태로 되돌리기

새 deployment가 문제를 만들면 이전 version으로 되돌리는 것을 **rollback**이라고 부른다.

### LESSON 02 · code rollback과 data rollback

새 code가 DB 데이터를 새 형태로 이미 변경했다면 code만 이전으로 돌려도 old code가 데이터를 읽지 못할 수 있다.

rollback 계획에는 data compatibility를 포함한다.

---

## BLOCK 59 · monitoring

### LESSON 01 · 배포 후가 시작

production에 배포했다고 끝이 아니다.

다음을 본다.

```text
error rate
latency
CPU/memory
business metric
로그
사용자 신고
```

### LESSON 02 · baseline과 비교

배포 전 정상 기준값을 **baseline**으로 저장한다.

```text
error rate 0.2%
→ 배포 후 8%
```

이면 즉시 조사해야 한다.

---

## BLOCK 60 · SLI / SLO 맛보기

### LESSON 01 · SLI

서비스 품질을 실제 측정하는 지표를 **SLI(Service Level Indicator)**라고 부른다.

예:

```text
성공 요청 비율
p95 latency
```

### LESSON 02 · SLO

SLI가 어느 수준을 만족해야 하는지 정한 목표를 **SLO(Service Level Objective)**라고 부른다.

예:

```text
30일 동안 성공률 99.9% 이상
```

---

## BLOCK 61 · smoke test

### LESSON 01 · 배포 직후 핵심 기능 빠르게 확인

서비스가 기본적으로 살아 있는지 확인하는 짧은 핵심 테스트를 **smoke test**라고 부른다.

예:

```text
홈 열림
로그인 가능
핵심 API 200
DB read/write 정상
```

### LESSON 02 · smoke test만으로 전체 검증은 아니다

전체 회귀 테스트의 일부 핵심 경로만 빠르게 확인하는 것이다.

---

## BLOCK 62 · CLEAN PASS

### LESSON 01 · 수정 후 처음부터 다시 검증

이 교재에서는 수정이 끝난 뒤 **처음부터 독립된 실패 유형을 실제로 다시 검사하는 절차**를 CLEAN PASS라고 부른다.

### LESSON 02 · 반복 횟수보다 실패 유형

좋지 않은 검증:

```text
같은 정상 버튼 클릭 20회
```

좋은 검증:

```text
정상 입력
빈 입력
최댓값
네트워크 끊김
서버 500
빠른 연속 클릭
앱 재시작
기존 데이터 migration
```

### LESSON 03 · 실행하지 않은 것은 PASS 아님

```text
코드를 읽음 → 정적 검토
머릿속 가정 → 시뮬레이션
실제 테스트 실행 → 실행 증거
운영에서 확인 → 운영 증거
```

서로 구분해 기록한다.

---

## BLOCK 63 · release checklist

### LESSON 01 · 배포 전

```text
요구사항 확인
변경 diff 확인
secret scan
lint/type check
unit test
integration test
E2E 핵심 경로
Release build
artifact hash
migration 검토
rollback plan
```

### LESSON 02 · 배포 후

```text
version 확인
health check
smoke test
error rate
latency
business metric
로그
```

---

## BLOCK 64 · incident

### LESSON 01 · 운영 장애

사용자에게 실제 영향을 주는 서비스 문제를 **incident(장애 사건)**라고 부른다.

### LESSON 02 · 우선 복구

운영 장애 중에는 완벽한 root cause 분석보다 사용자 영향 줄이기와 서비스 복구를 먼저 할 수 있다.

```text
rollback
feature flag off
traffic 차단
fallback 사용
```

등이 있다.

---

## BLOCK 65 · postmortem

### LESSON 01 · 장애 후 구조적으로 배우기

장애가 끝난 뒤 원인, 영향, 대응, 개선점을 정리하는 문서를 **postmortem**이라고 부른다.

### LESSON 02 · 사람 비난보다 시스템 개선

좋은 postmortem은:

```text
무슨 일이 있었나
왜 탐지 못했나
왜 방어가 실패했나
어떤 자동화·테스트를 추가할까
```

를 다룬다.

---

## BLOCK 66 · 핵심 용어 사전

| 용어 | 아주 쉬운 뜻 |
|---|---|
| bug | 기대 동작과 실제 동작이 다른 결함 |
| reproduction | 같은 문제를 다시 일으켜 보는 것 |
| minimal reproduction | 문제를 만드는 최소 코드·조건 |
| hypothesis | 아직 검증되지 않은 원인 후보 |
| debugger | 실행 중 프로그램 내부를 조사하는 도구 |
| breakpoint | debugger가 실행을 멈출 코드 위치 |
| stack trace | 오류 위치까지 함수 호출 경로를 보여주는 기록 |
| assertion | 반드시 참이어야 하는 내부 가정을 검사하는 것 |
| unit test | 작은 코드 단위를 분리해 검사하는 테스트 |
| integration test | 여러 구성요소 연결을 검사하는 테스트 |
| E2E test | 사용자 흐름 전체를 검사하는 테스트 |
| test double | 실제 dependency 대신 쓰는 테스트 대체물 |
| regression | 변경으로 기존 정상 기능이 다시 깨지는 현상 |
| flaky test | 같은 코드에서도 가끔 성공·실패가 바뀌는 테스트 |
| coverage | 테스트 중 실행된 코드 범위를 측정한 값 |
| Git | 파일 변경 이력을 commit 단위로 관리하는 버전 관리 시스템 |
| repository | Git이 관리하는 프로젝트와 이력 저장소 |
| staging area | 다음 commit에 넣을 변경을 준비하는 영역 |
| commit | 프로젝트 변경을 저장한 이력 단위 |
| branch | 독립 개발 흐름을 가리키는 Git 포인터 |
| merge | 다른 branch 변경을 합치는 것 |
| conflict | Git이 자동으로 합칠 수 없는 변경 충돌 |
| rebase | commit들을 다른 기준 commit 위에 다시 적용하는 기능 |
| remote | 다른 위치의 Git repository |
| Pull Request | 변경을 검토하고 합치기 위한 협업 단위 |
| dependency | 프로그램이 필요로 하는 외부 코드 관계 |
| lock file | 실제 dependency version을 고정·기록하는 파일 |
| build | source를 실행·배포 가능한 결과로 만드는 과정 |
| artifact | build가 만든 실제 결과 파일 |
| CI | 변경마다 자동 build·test를 실행하는 통합 방식 |
| deployment | 새 artifact를 실제 실행 환경에 반영하는 과정 |
| canary | 일부 traffic에 새 version을 먼저 배포하는 방식 |
| feature flag | 배포된 기능의 활성 여부를 설정으로 제어하는 장치 |
| rollback | 이전 정상 상태로 되돌리는 것 |
| SLI | 서비스 품질을 실제로 측정하는 지표 |
| SLO | 서비스 품질 지표가 만족해야 할 목표 |
| CLEAN PASS | 수정 후 서로 다른 실패 유형을 처음부터 실제 검증하는 절차 |
| incident | 운영 사용자에게 영향을 주는 장애 사건 |
| postmortem | 장애 후 원인·대응·개선을 정리하는 분석 |

---

## BLOCK 67 · TRACK 10 완료 기준

다음을 실제로 할 수 있어야 한다.

- 버그를 Expected/Actual로 정의한다.
- 재현 절차와 환경을 기록한다.
- 최소 재현을 만든다.
- 사실과 가설을 구분한다.
- debugger, breakpoint, stack trace로 원인을 좁힌다.
- unit/integration/E2E test를 목적에 맞게 나눈다.
- regression test를 추가한다.
- flaky test를 재실행으로 숨기지 않는다.
- Git working tree/staging/commit/branch를 설명한다.
- merge conflict를 의미를 보며 해결한다.
- PR diff와 테스트 증거를 검토한다.
- dependency와 lock file의 역할을 설명한다.
- Release build와 Debug build를 따로 검증한다.
- commit → CI → artifact → deployed version을 추적한다.
- migration이 포함된 배포 순서를 설계한다.
- canary 또는 feature flag로 위험을 줄인다.
- rollback plan을 만든다.
- 배포 후 metric과 smoke test로 실제 운영 상태를 확인한다.
- 미실행 검증을 PASS라고 부르지 않는다.

### TRACK 프로젝트 · 버그 수정에서 운영 배포 직전까지

작은 웹앱의 race condition 버그를 고친다.

필수 절차:

1. 버그를 10회 재현하고 실패 빈도를 기록한다.
2. 최소 재현을 별도 test로 만든다.
3. 수정 전 test가 실제 실패하는 것을 확인한다.
4. 원인 가설을 작성한다.
5. debugger/log로 완료 순서를 기록한다.
6. 코드를 한 가지 방식으로 수정한다.
7. 같은 test를 다시 실행한다.
8. 정상·빈 입력·빠른 연속 요청·network error를 별도로 검사한다.
9. regression test를 추가한다.
10. 새 branch에 commit한다.
11. diff를 검토한다.
12. secret이 포함되지 않았는지 확인한다.
13. local lint/type/unit/integration/E2E를 실행한다.
14. Release build를 만든다.
15. artifact hash와 source commit SHA를 연결해 기록한다.
16. deployment plan, canary 조건, rollback 조건을 문서화한다.
17. **실제 deployment는 별도 승인 없이는 실행하지 않는다.**

완료 판정에는 `어떤 테스트를 실제로 실행했고 어떤 결과가 나왔는지`가 반드시 있어야 한다.
