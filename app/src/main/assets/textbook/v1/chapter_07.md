## V1-C07. 로그를 읽는 법 — 시간축·stack trace·correlation

**목표:** 오류 메시지 한 줄만 보고 결론내리지 않고, 로그를 시간순으로 재구성해 **최초 비정상 사건 → 후속 오류 → 사용자 증상**을 구분할 수 있게 된다.

---

### 1. 로그는 사건 기록이다

**로그(log)**는 프로그램이 실행 중 남긴 기록이다.

좋은 로그에는 보통 다음 정보가 있다.

```text
시간
심각도
어느 구성요소인지
무슨 일이 있었는지
관련 요청/사용자/작업 식별자
```

예:

```text
10:00:01 INFO  StockApi   request started symbol=AAPL
10:00:02 ERROR StockApi   timeout requestId=abc123
10:00:02 WARN  Screen     showing cached data requestId=abc123
```

사용자는 “가격이 예전 값으로 보여요”라고 말할 수 있다. 로그를 보면 그 전에 네트워크 timeout이 있었고, 화면은 후속 조치로 캐시를 보여준 것을 알 수 있다.

---

### 2. 마지막 ERROR가 원인이라는 보장은 없다

다음 로그를 보자.

```text
10:00:01 ERROR DB      failed to open database
10:00:02 ERROR Repo    user data unavailable
10:00:03 ERROR Screen  cannot render profile
```

가장 마지막 줄은 화면 오류지만 첫 번째 실패는 DB다.

```text
DB 실패
↓
데이터 없음
↓
화면 렌더링 실패
```

마지막 오류만 고치면 원인을 놓칠 수 있다.

그래서 로그를 볼 때는 **사용자 증상 발생 시각보다 조금 앞**에서 시작한다.

---

### 3. timestamp — 시간순으로 다시 세운다

분산된 로그는 파일 순서가 실제 사건 순서와 다를 수 있다.

```text
10:00:03 WARN retry
10:00:01 INFO request
10:00:02 ERROR timeout
```

시간순으로 정렬하면:

```text
10:00:01 request
10:00:02 timeout
10:00:03 retry
```

이제 원인 흐름이 보인다.

시간대(timezone)도 중요하다. 서버 로그는 UTC, 휴대폰 화면은 한국시간일 수 있다.

```text
서버: 00:00 UTC
기기: 09:00 KST
```

같은 사건인데 시각이 다르게 보일 수 있다.

---

### 4. log level — 심각도 힌트일 뿐 정답은 아니다

자주 보는 수준:

```text
DEBUG = 개발/세부 추적 정보
INFO  = 정상 흐름의 중요한 사건
WARN  = 이상 가능성이 있지만 계속 진행
ERROR = 실패 발생
```

하지만 `ERROR`라고 해서 항상 root cause인 것은 아니다.

라이브러리가 같은 실패를 여러 번 ERROR로 기록하거나, 실제 원인이 그보다 앞의 WARN에 있을 수도 있다.

**level은 필터링 힌트이지 원인 판정기가 아니다.**

---

### 5. component를 나누면 계층이 보인다

로그에 구성요소 이름이 있으면 어느 계층이 실패했는지 좁힐 수 있다.

```text
Network
ApiClient
Parser
Database
Repository
Screen
NotificationWorker
```

예:

```text
Network 200 OK
Parser success
Database save success
Screen old value=180
```

네트워크와 데이터 저장은 정상이고 화면 상태 연결 쪽이 우선 후보다.

반대로:

```text
Network timeout
Parser not started
```

파서 코드를 먼저 고치는 것은 우선순위가 아니다.

---

### 6. stack trace — “어디서 터졌는지” 역추적한다

프로그램 예외가 발생하면 호출 경로가 **stack trace**로 남을 수 있다.

단순 예:

```text
NullPointerException
at StockFormatter.format(StockFormatter.kt:42)
at StockScreen.render(StockScreen.kt:88)
at MainActivity.onCreate(MainActivity.kt:30)
```

초보자가 모든 줄을 이해할 필요는 없다.

먼저 본다.

```text
예외 종류
내 코드가 처음 등장하는 줄
파일명과 줄번호
그 직전에 어떤 값이 들어왔는지
```

라이브러리 내부 줄 수십 개보다 **내 코드와 연결되는 첫 지점**이 더 유용할 수 있다.

---

### 7. correlation id — 한 요청의 로그를 묶는다

동시에 여러 사용자가 여러 요청을 보내면 로그가 섞인다.

```text
requestId=A1 start AAPL
requestId=B7 start MSFT
requestId=A1 timeout
requestId=B7 success
```

AAPL 요청과 MSFT 요청을 구분하려면 같은 작업에 공통 식별자가 필요하다.

이런 값을 **correlation id** 또는 request id라고 부른다.

```text
A1 로그만 모음
↓
AAPL 요청 전체 흐름 복원
```

분산 시스템에서는 앱 → API → DB → 외부 서비스가 같은 ID를 전달하면 훨씬 추적하기 쉽다.

---

### 8. 로그에 비밀정보를 남기면 안 된다

디버깅을 위해 모든 값을 출력하는 습관은 위험하다.

나쁜 로그:

```text
Authorization: Bearer REAL_TOKEN
password=MyPassword123
residentNumber=...
```

로그 파일은 개발자, 서버, 모니터링 도구, 백업 시스템 등 여러 곳에 복제될 수 있다.

비밀값은 마스킹한다.

```text
token=***
email=w***@example.com
```

필요한 증거는 남기되 **민감정보 자체는 남기지 않는다.**

---

### 9. 로그가 없으면 관찰 가능성이 떨어진다

문제 발생 시 이런 기록만 있다면:

```text
"실패"
```

원인을 찾기 어렵다.

더 좋은 기록:

```text
2026-09-16T09:00:01+09:00
component=PriceSync
symbol=AAPL
stage=api_request
status=timeout
requestId=ab12
elapsedMs=5000
```

이런 구조는 검색·정렬·집계를 쉽게 한다.

로그 설계의 목적은 문장을 많이 남기는 것이 아니라 **나중에 필요한 질문에 답할 수 있게 하는 것**이다.

---

### 10. 실제 사례 — 알림이 한 기기에서만 안 옴

사용자 증상:

```text
엄마 폰: 알림 옴
배우자 폰: 알림 안 옴
```

로그를 단계별로 본다.

```text
09:00:00 schedule triggered
09:00:01 recipients=3
09:00:01 deviceToken wife=XYZ
09:00:02 provider accepted messageId=123
```

서버 로그만 보면 push provider까지 요청은 갔다.

그렇다고 기기 화면에 알림이 표시됐다는 뜻은 아니다.

다음 단계는:

```text
기기 token 최신 여부
notification permission
notification channel
battery/background restriction
```

이다.

**provider accepted = 사용자에게 표시 완료**가 아니다.

관찰과 추론을 분리한다.

---

### 11. 실제 사례 — 자동화가 두 번 실행됨

로그:

```text
10:00:00 scheduler trigger job=J1
10:00:01 worker start run=R1
10:00:05 retry start run=R2
10:00:08 R1 success
10:00:10 R2 success
```

최종 데이터가 두 번 저장됐다.

중복 결과만 삭제하면 재발한다.

확인할 것:

```text
retry가 왜 시작됐는가?
R1이 살아 있는데 R2를 시작했는가?
중복 방지 키가 있는가?
worker가 idempotent한가?
```

로그가 시간축과 run id를 포함하면 원인을 재구성하기 쉽다.

---

### 12. 로그 분석 순서

```text
1. 사용자 증상 시각 고정
2. 시각 전후 로그 수집
3. timezone 통일
4. request/run/device id로 묶기
5. 최초 비정상 사건 찾기
6. 후속 오류와 분리
7. 가설 작성
8. 추가로 필요한 로그 결정
```

처음부터 전체 로그를 AI에 던져 “원인 찾아줘”라고 하기보다, 비밀값을 제거하고 관련 시간창과 component를 좁히는 편이 안전하고 정확하다.

---

## 실습 1 — root cause 후보 찾기

```text
12:00:00 INFO  API request
12:00:01 ERROR DNS lookup failed
12:00:02 ERROR request failed
12:00:03 WARN  showing empty state
```

첫 번째 조사 후보는?

정답: DNS lookup failure.

---

## 실습 2 — 두 요청 분리

```text
A1 start AAPL
B2 start MSFT
B2 success
A1 timeout
```

AAPL 로그만 추리면:

```text
A1 start AAPL
A1 timeout
```

---

## 실습 3 — 민감정보 제거

나쁜 로그:

```text
authToken=eyJhbGciOi...
```

더 나은 로그:

```text
authTokenPresent=true
```

토큰의 존재 여부만 필요하다면 실제 값은 기록하지 않는다.

---

## 실습 4 — 관찰과 추론 분리

관찰:

```text
provider response=200
```

잘못된 결론:

```text
사용자 폰에 알림이 반드시 표시됐다.
```

200 응답은 provider가 요청을 받아들였다는 증거일 수 있지만 기기 표시까지 증명하지는 않는다.

---

## 장 끝 미니 프로젝트 — 장애 시간축 만들기

가상의 오류 로그 20줄을 만든다고 생각하고, 실제로 필요한 컬럼을 설계한다.

| 필드 | 이유 |
|---|---|
| timestamp | 사건 순서 |
| timezone | 시각 비교 |
| level | 필터링 |
| component | 계층 분리 |
| request/run id | 같은 작업 묶기 |
| stage | 어느 단계인지 |
| result | 성공/실패 |
| elapsedMs | 지연 확인 |
| safe context | 민감정보 제외한 입력 정보 |

그 다음 실제 앱 문제 하나에 이 표를 적용해본다.

---

## 핵심 용어

| 용어 | 뜻 |
|---|---|
| log | 실행 중 발생한 사건 기록 |
| timestamp | 사건 발생 시각 |
| log level | DEBUG/INFO/WARN/ERROR 같은 심각도/용도 분류 |
| component | 로그를 남긴 기능/계층 |
| stack trace | 예외가 어떤 호출 경로를 거쳐 발생했는지 보여주는 기록 |
| correlation id | 같은 요청/작업 로그를 묶는 식별자 |
| root cause | 연쇄 오류를 만든 근본 원인 |
| masking | 민감정보 일부를 숨겨 기록하는 것 |

---

## 이 장을 끝내고 할 수 있어야 하는 것

1. 마지막 ERROR와 최초 실패를 구분한다.
2. 로그를 시간순으로 재구성한다.
3. timezone 차이를 확인한다.
4. component별로 실패 계층을 좁힌다.
5. stack trace에서 내 코드와 연결되는 지점을 찾는다.
6. request/run id로 섞인 로그를 분리한다.
7. 로그에 secret을 그대로 남기지 않는다.
8. 관찰된 사실과 추론을 분리한다.
