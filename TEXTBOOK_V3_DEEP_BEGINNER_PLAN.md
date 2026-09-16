# TEXTBOOK V3 · 완전초급 심화 교재 개편 계약

## 사용자 요구를 숫자로 고정

- 기존 V2 `### LESSON` 총수: **590개**
- V3 목표: **74개**
- 감소율: **87.46%**
- 계층은 유지: `TRACK → BLOCK → LESSON`
- 한 BLOCK에는 원칙적으로 한 개의 큰 LESSON을 둔다.
- 관련 있는 기존 소단원을 한 LESSON 안의 `####` 소제목으로 합친다.
- V2 원본은 보존하고 V3를 별도 asset으로 만든다.
- main / 배포 / Actions는 이 작업에서 건드리지 않는다.

## TRACK별 LESSON 수

| TRACK | V2 | V3 목표 | 감소율 |
|---|---:|---:|---:|
| 01 컴퓨터와 프로그래밍의 언어 | 31 | 5 | 83.9% |
| 02 프로그래밍 사고와 문법 | 70 | 8 | 88.6% |
| 03 자료구조와 알고리즘 | 51 | 7 | 86.3% |
| 04 웹 화면과 브라우저 | 43 | 6 | 86.0% |
| 05 JavaScript와 TypeScript | 66 | 8 | 87.9% |
| 06 인터넷·네트워크·API | 49 | 7 | 85.7% |
| 07 서버와 백엔드 | 48 | 6 | 87.5% |
| 08 데이터베이스 | 63 | 8 | 87.3% |
| 09 보안과 데이터 보호 | 39 | 6 | 84.6% |
| 10 오류·테스트·Git·빌드·배포 | 53 | 6 | 88.7% |
| 11 소프트웨어 설계와 종합 프로젝트 | 77 | 7 | 90.9% |
| **합계** | **590** | **74** | **87.46%** |

## V3 LESSON 편집 계약

각 큰 LESSON은 다음 순서를 기본으로 한다.

1. **왜 배우는가** — 실제 상황부터 시작한다.
2. **한국어로 먼저** — 영어 전문용어를 먼저 던지지 않는다.
3. **눈으로 보는 구조** — 그림·표·순서도로 상태 변화를 보여준다.
4. **한 단계씩** — 한 번에 새 개념을 여러 개 정의하지 않는다.
5. **첫 코드** — 가장 작은 실행 가능한 코드부터 보여준다.
6. **한 줄씩 읽기** — 아직 배우지 않은 문법을 건너뛰지 않는다.
7. **조금씩 확장** — 예제를 한 번에 완성하지 않고 한 기능씩 추가한다.
8. **실수 사례** — 초보자가 실제로 틀리는 코드를 보여준다.
9. **왜 틀렸는지** — 정답만 제시하지 않는다.
10. **실제 앱 연결** — 화면·서버·DB 등 어디에 쓰이는지 연결한다.
11. **무엇과 연결되는가** — 앞 단원과 뒤 단원을 이어준다.
12. **책을 덮고 확인** — 마지막에 회상 질문을 둔다.

## 분량 계약

V2의 짧은 LESSON을 그대로 늘어놓는 방식은 금지한다.

V3에서는 기존 V2를 그대로 보존하므로 자동 테스트에서 V2와 V3를 함께 읽을 수 있다. 최종 품질 게이트는 다음을 검사하도록 만든다.

- `V3 lesson count <= V2 lesson count * 2 / 3` — 사용자 최소 요구(1/3 이상 감소) 하한.
- 실제 목표는 위 표의 74개와 정확히 일치.
- 각 V3 instructional LESSON은 짧은 정의 카드가 아니라 **대단원형 설명**이어야 한다.
- TRACK별 V3 평균 weightedLength가 같은 TRACK V2 평균의 **20배 이상**인지 검사한다.
- V3 각 LESSON에 최소한 `왜`, 단계/순서 설명, 예제, 실수/주의, 회상 확인 중 핵심 교육 요소가 존재하는지 정적 검사한다.
- 전문용어 사전은 본문 대체물이 아니라 복습용으로만 둔다.

## TRACK별 묶음

### TRACK 01 · 5 LESSON
1. 컴퓨터가 실제로 하는 일 — 입력·처리·출력, 하드웨어·소프트웨어, CPU·RAM·저장장치
2. 파일과 데이터의 정체 — 파일·폴더·경로·확장자·형식·bit·byte
3. 글자가 저장되는 과정 — 숫자 표현, encoding, Unicode, UTF-8, 문자 깨짐
4. 프로그램과 코드는 어떻게 실행되는가 — OS·프로그램·코드·프로그래밍 언어·소스·실행
5. 첫 프로그램을 읽고 고치기 — 출력·값·문법 오류·실행 오류·디버깅의 첫 경험

### TRACK 02 · 8 LESSON
1. 문제를 코드로 바꾸는 생각 — 목표·입력·처리·출력·분해
2. 값과 변수 — 숫자·문자열·Boolean·None·리터럴·변수·상수·표현식·연산자
3. 사용자 입력과 조건 판단 — input·변환·if·elif·else·논리 조건
4. 반복 — for·while·range·break·continue·중첩 반복
5. 여러 데이터를 다루기 — list·tuple·dict·set·index·slice·추가·삭제
6. 함수로 일을 나누기 — 함수·parameter·argument·return·scope·재사용
7. 파일·모듈·오류를 다루기 — import·module·exception·try/catch 계열 사고·파일 입출력
8. 작은 프로그램 완성 — 객체 기초와 앞 개념을 묶어 입력→처리→저장→오류처리까지

### TRACK 03 · 7 LESSON
1. 자료구조를 왜 고르는가 — array·dynamic array·linked list·node를 비교
2. 넣고 빼고 찾는 구조 — stack·queue·deque·set·map·hash table·collision
3. 계층 구조 — tree·binary tree·BST·balanced tree·heap·priority queue·trie
4. 연결 관계 — graph·vertex·edge·directed/undirected·adjacency list/matrix
5. 찾고 정렬하기 — linear/binary search, selection/insertion/merge/quick sort
6. 빠르다는 뜻 — 실행 횟수·Big-O·O(1)/O(log n)/O(n)/O(n log n)/O(n²)·공간·재귀
7. 문제 해결 전략 — BFS·DFS·visited·greedy·DP·memoization·정확성·경계값

### TRACK 04 · 6 LESSON
1. 웹과 브라우저, HTML 문서 만들기
2. 링크·이미지·폼·입력과 semantic/accessibility
3. CSS가 화면을 만드는 법 — selector·cascade·box model·display·position
4. 레이아웃 — flex·grid·반응형·viewport
5. DOM과 이벤트 — 요소 찾기·수정·event·state 흐름
6. 브라우저 렌더링과 DevTools로 실제 화면 만들고 고치기

### TRACK 05 · 8 LESSON
1. JavaScript 실행 환경과 값 — let/const/var·primitive·object·reference·equality·coercion
2. 함수와 스코프 — function·arrow·lexical scope·closure·this·prototype·class
3. 데이터를 편하게 다루기 — map/filter/reduce·destructuring·spread/rest·module·error
4. 기다림에서 비동기까지 — blocking·sync·async·callback
5. Promise와 async/await — 상태·then/catch·chain·async·await·실패 처리
6. event loop — call stack·task·event loop·microtask를 실제 출력 순서로 이해
7. 여러 비동기 작업 — Promise.all/allSettled·timeout·cancellation·race·debounce·throttle
8. TypeScript — type·interface·union·narrowing·unknown·never·generic·runtime validation

### TRACK 06 · 7 LESSON
1. 인터넷의 길 찾기 — network·client/server·IP·private/public·NAT
2. 서버까지 도착하기 — domain·DNS·port·protocol·packet·TCP/UDP·latency/bandwidth
3. 안전한 웹 연결 — TLS·certificate·HTTP/HTTPS·URL
4. HTTP 메시지 읽기 — request/response·method·status·header·body·idempotency
5. 데이터 형식과 업로드 — MIME·JSON·Content-Type·Accept·form·multipart·boundary
6. API를 사용하는 법 — API·REST·endpoint·pagination·authentication·cookie/session/token
7. 브라우저 정책과 실전 진단 — origin·CORS·cache·redirect·WebSocket/SSE·fetch·오류 층 진단

### TRACK 07 · 6 LESSON
1. 서버는 요청을 어떻게 받는가 — process·socket·framework·route·handler
2. 요청을 안전하게 통과시키기 — parsing·middleware·validation·sanitization
3. 업무 규칙과 코드 책임 — business logic·service·repository
4. 누가 무엇을 할 수 있는가 — authentication·authorization·RBAC·상태 관리
5. 느린 일을 다루기 — DB·외부 API·cache·queue·background job·retry·idempotency
6. 운영 가능한 서버 — 동시성·rate limit·logging·metric·trace·health check·graceful failure

### TRACK 08 · 8 LESSON
1. DB가 왜 필요한가 — data·file·DB·DBMS·relational·table·row·column·schema·type
2. SQL로 만들고 넣고 찾고 바꾸기 — CREATE/INSERT/SELECT/WHERE/UPDATE/DELETE/ORDER/LIMIT
3. 여러 데이터를 계산하기 — aggregate·GROUP BY·HAVING과 실제 조회 사고
4. 틀린 데이터를 막기 — ID·PRIMARY KEY·UNIQUE·NULL·NOT NULL·CHECK·integrity·FOREIGN KEY
5. 관계를 설계하기 — 1:1·1:N·N:M·junction table·normalization
6. 여러 table을 함께 읽기 — JOIN·INNER/LEFT·subquery와 결과 예측
7. 빠르게 찾기 — index·query plan·EXPLAIN·N+1·읽기/쓰기 비용
8. 여러 변경을 안전하게 — transaction·ACID·lock·deadlock·isolation·rollback·복구

### TRACK 09 · 6 LESSON
1. 무엇을 누구에게서 지키는가 — asset·CIA·threat·vulnerability·risk·trust boundary·threat model
2. 사용자를 확인하고 권한을 제한하기 — authentication·authorization·least privilege
3. encoding과 hash를 구분하기 — Base64·hash·SHA-256·collision·preimage·file integrity
4. 비밀번호를 안전하게 저장하기 — password hashing·salt·bcrypt/Argon2·공격 시나리오
5. 암호화와 신뢰 — symmetric/asymmetric·key·signature·certificate·TLS 관계
6. 웹서비스 공격과 방어 — injection·XSS·CSRF·secret·upload·dependency·logging과 종합 점검

### TRACK 10 · 6 LESSON
1. 버그를 재현하고 원인을 좁히기 — symptom·reproduction·environment·minimal repro·fact/hypothesis
2. 실행 중인 코드를 조사하기 — log·debugger·breakpoint·step·watch·stack trace·assertion
3. 테스트를 설계하기 — unit·integration·E2E·double·boundary·property·regression
4. Git으로 변경을 추적하기 — commit·branch·merge·conflict·review·revert
5. 코드를 실행 파일로 만들기 — dependency·build·CI·artifact·version·signature
6. 안전하게 배포하고 되돌리기 — migration·release·deployment·canary 개념·rollback·운영 검증

### TRACK 11 · 7 LESSON
1. 요구사항을 설계도로 바꾸기 — functional/non-functional·constraint·acceptance·domain·model
2. 코드를 나누는 기준 — abstraction·interface·module·responsibility·cohesion·coupling·dependency·DI
3. 아키텍처를 고르는 법 — layer·Clean/Hexagonal 사고·경계와 의존 방향
4. 데이터와 상태의 흐름 — state management·cache·queue·event·consistency 선택
5. 커졌을 때 버티기 — profiling·bottleneck·scaling·load balance·backpressure·capacity
6. 실패해도 버티기 — timeout·retry·circuit breaker·fault tolerance·observability·technical debt/refactoring
7. 종합 프로젝트 — 요구사항→모델→API→DB→보안→테스트→빌드→배포 증거까지 한 번에 설계
