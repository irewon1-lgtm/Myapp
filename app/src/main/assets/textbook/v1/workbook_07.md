# 7장 실전 훈련편 — 로그·시간축·stack trace·correlation

이 훈련편은 로그 정의를 반복하지 않는다. 실제 로그 조각에서 **최초 실패, 후속 증상, 섞인 요청, 민감정보**를 찾아낸다.

---

## 훈련 A. 시간순 재구성

로그가 파일에 다음 순서로 섞여 있다.

```text
10:00:05 WARN  Screen showing cached data
10:00:01 INFO  Api request start
10:00:03 ERROR Api timeout
10:00:04 INFO  Cache fallback start
10:00:02 DEBUG DNS resolved
```

시간순으로 다시 적는다.

```text
10:00:01 request start
10:00:02 DNS resolved
10:00:03 timeout
10:00:04 cache fallback
10:00:05 cached data shown
```

사용자 증상은 “옛 데이터가 보임”이지만 최초 실패는 API timeout이다.

---

## 훈련 B. root cause와 symptom 분리

로그:

```text
09:30:01 ERROR Database migration failed
09:30:02 ERROR Repository cannot load user
09:30:03 ERROR HomeScreen user is null
09:30:04 ERROR UI render aborted
```

표를 채운다.

| 줄 | 관찰 | 분류 |
|---|---|---|
| migration failed | DB 구조 변환 실패 | 최초 실패 후보 |
| repository cannot load | DB 실패 영향 | 후속 증상 |
| user is null | 데이터 없음 영향 | 후속 증상 |
| UI render aborted | 최종 사용자 증상 | 후속 증상 |

마지막 ERROR부터 고치면 왜 근본 원인을 놓칠 수 있는지 설명한다.

---

## 훈련 C. timezone 맞추기

서버 로그:

```text
2026-09-16T00:00:01Z job start
```

기기 로그:

```text
2026-09-16 09:00:02 +09:00 notification missing
```

같은 사건 시간대인지 확인한다.

`Z`는 UTC를 뜻한다. 한국시간은 UTC+9이므로 두 기록은 약 1초 차이의 같은 실행 흐름일 수 있다.

분산 장애에서 timezone 통일 전 시간순서를 단정하면 안 된다.

---

## 훈련 D. level만 보고 원인 고르지 않기

로그:

```text
WARN  token expires in 10s
INFO  refreshing token
ERROR network unavailable
ERROR auth refresh failed
ERROR request unauthorized
```

마지막 `unauthorized`만 보면 인증 로직을 의심하기 쉽다.

시간축을 보면 네트워크 unavailable 때문에 refresh가 실패했고 그 결과 unauthorized가 발생했을 수 있다.

level은 원인 우선순위가 아니라 **필터 힌트**다.

---

## 훈련 E. 섞인 요청 분리

```text
req=A1 symbol=AAPL stage=start
req=B2 symbol=MSFT stage=start
req=A1 stage=dns ok
req=B2 stage=http status=200
req=B2 stage=parse ok
req=A1 stage=http timeout
req=B2 stage=ui shown
```

A1만 추린다.

```text
A1 AAPL start
A1 dns ok
A1 http timeout
```

B2만 추린다.

```text
B2 MSFT start
B2 http 200
B2 parse ok
B2 ui shown
```

이제 “API가 전체 장애”라는 결론이 약해진다.

---

## 훈련 F. correlation ID 설계

하나의 주가 수집 작업이 다음 시스템을 지난다.

```text
scheduler → worker → external API → parser → DB → publisher
```

각 시스템이 제각각 ID를 만들면 추적이 어렵다.

공통 `runId=20260916-0900-001`을 전달한다고 가정한다.

각 로그에 최소한 넣을 항목:

```text
runId
component
stage
status
timestamp
safe entity key
```

`safe entity key`는 민감정보를 그대로 넣지 않는 범위에서 작업 대상을 식별한다.

---

## 훈련 G. stack trace에서 내 코드 찾기

예:

```text
java.lang.IllegalStateException: price missing
    at kotlin.collections...
    at retrofit2...
    at com.myapp.data.StockParser.parse(StockParser.kt:42)
    at com.myapp.data.StockRepository.load(StockRepository.kt:88)
    at com.myapp.ui.StockViewModel.refresh(StockViewModel.kt:60)
```

처음 조사할 내 코드 줄:

```text
StockParser.kt:42
```

그 줄의 입력값과 호출자까지 본다.

라이브러리 stack 줄을 전부 이해하려고 시간을 쓰기보다 **내 코드 경계와 입력**부터 확인한다.

---

## 훈련 H. cause chain 읽기

예외:

```text
RuntimeException: failed to load data
Caused by: JsonDataException: expected NUMBER but was STRING at $.price
```

겉 예외는 `failed to load data`지만 더 구체적인 원인은 `price` 타입 불일치다.

`Caused by` 체인을 따라 가장 구체적인 원인을 찾는다.

---

## 훈련 I. 로그에 PII/secret 찾기

다음 로그에서 위험한 것을 표시한다.

```text
userId=1234
email=user@example.com
accessToken=eyJhbGciOi...
password=hello123
query=AAPL
```

위험도가 큰 항목:

- access token
- password
- email도 개인정보 정책에 따라 마스킹 필요

더 안전한 형태:

```text
userIdHash=ab12
emailDomain=example.com
accessTokenPresent=true
password=<never log>
query=AAPL
```

필요한 진단 정보를 유지하면서 민감정보를 줄인다.

---

## 훈련 J. 로그가 너무 많은 것도 문제

모든 루프에서 초당 10,000줄 DEBUG 로그를 남기면:

- 저장공간 사용
- I/O 부하
- 중요한 ERROR 찾기 어려움
- 비용 증가
- 민감정보 노출 면적 증가

로그는 “많을수록 좋다”가 아니다.

질문:

- 어떤 사건을 INFO로 남길까?
- 반복 상세는 DEBUG로 내려야 하나?
- sampling이 필요한가?

제품 운영 목적에 맞춰 결정한다.

---

## 훈련 K. structured log와 자유문장 비교

자유문장:

```text
AAPL 요청이 2초 만에 실패했어요
```

구조화:

```json
{
  "component":"StockApi",
  "symbol":"AAPL",
  "stage":"request",
  "status":"timeout",
  "elapsedMs":2000,
  "requestId":"A1"
}
```

구조화 로그는 필터·집계하기 쉽다.

실제 시스템에서는 JSON log, key=value 등 여러 형태를 쓸 수 있다.

---

## 훈련 L. 최초 실패 자동 후보 추출

작은 Python 연습:

```python
logs = [
    ("10:00:01", "INFO", "request start"),
    ("10:00:02", "ERROR", "timeout"),
    ("10:00:03", "WARN", "fallback"),
    ("10:00:04", "ERROR", "screen empty"),
]

for row in logs:
    if row[1] == "ERROR":
        print("first error candidate:", row)
        break
```

이 코드는 최초 ERROR만 찾는다. 실제 root cause가 WARN/DEBUG일 수도 있으므로 자동 결과를 최종 판정으로 쓰지 않는다.

---

## 훈련 M. 알림 사건 시간축

다음 데이터를 한 표로 합친다.

서버:

```text
09:00:00 schedule trigger
09:00:01 recipient wife selected
09:00:02 provider accepted msg=555
```

기기:

```text
09:00:02 app process background
09:00:03 notification channel disabled
```

결론:

provider까지 정상 전달됐다는 증거가 있고, 기기 채널 비활성화가 표시 실패의 강한 후보가 된다.

---

## 훈련 N. 데이터 수집 사건 시간축

```text
10:00:00 job start
10:00:01 API 200
10:00:02 parse revenue=100
10:00:03 DB save revenue=100
10:00:04 publish snapshot revenue=90
```

값이 처음 달라지는 지점은 publish snapshot 단계다.

원본 API를 다시 바꾸는 것은 우선순위가 아니다.

---

## AI 답 검증 1

AI 답:

> 마지막 ERROR가 root cause입니다.

판정: 틀림.

마지막 ERROR는 앞 실패의 결과일 수 있다.

---

## AI 답 검증 2

AI 답:

> 로그를 AI에 전부 붙이면 가장 정확하게 분석할 수 있습니다.

판정: 위험.

민감정보 마스킹, 시간창/요청 범위 축소, 원본 보존이 먼저다.

---

## 디버깅 미션 1 — 로그 없는 실패

현재 코드:

```text
catch error:
    return emptyList()
```

사용자는 “데이터가 없음”만 본다.

최소한 남길 로그 설계:

```text
component
stage
error class
safe message
requestId
elapsed
```

오류를 사용자에게 그대로 노출할 필요는 없지만 운영 관찰 가능성은 유지한다.

---

## 디버깅 미션 2 — 중복 자동화

로그:

```text
09:00 trigger schedule=S1
09:00 worker run=R1 start
09:02 timeout reported for R1
09:02 retry run=R2 start
09:03 R1 DB save success
09:04 R2 DB save success
```

중복의 핵심 구조를 설명한다.

```text
클라이언트/worker는 R1 실패로 판단했지만 서버 작업은 계속 살아 있었음
→ R2 재시도
→ 둘 다 저장
```

---

## 독립 프로젝트 — 로그 분석 보고서

실제 또는 가상 로그 30~100줄을 준비한다.

### 1단계
민감정보를 제거한다.

### 2단계
시간대를 통일한다.

### 3단계
request/run ID별로 묶는다.

### 4단계
각 작업의 최초 비정상 사건을 찾는다.

### 5단계
다음 표를 작성한다.

| 시각 | component | 관찰 | 원인/후속 | 증거 |
|---|---|---|---|---|

### 6단계
가설 3개와 추가 필요한 로그를 적는다.

---

# 7장 훈련 완료 기준

- 로그를 시간순으로 재구성한다.
- 사용자 증상과 최초 실패를 분리한다.
- timezone을 맞춰 비교한다.
- level만으로 root cause를 결정하지 않는다.
- request/run ID로 섞인 로그를 분리한다.
- stack trace에서 내 코드 경계를 찾는다.
- cause chain을 따라 구체 원인을 찾는다.
- 민감정보를 마스킹한 뒤 AI/도구에 넘긴다.
- 값이 처음 달라지는 로그 지점을 찾아낼 수 있다.
