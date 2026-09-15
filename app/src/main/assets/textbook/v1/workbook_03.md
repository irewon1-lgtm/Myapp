# 3장 실전 훈련편 — 프로세스·스레드·비동기·경쟁 상태

이 훈련편은 프로세스와 스레드 정의를 다시 설명하지 않는다. 실제로 **순서가 꼬이는 문제, UI가 멈추는 문제, 중복 요청, 오래된 응답**을 추적한다.

---

## 훈련 A. 시작 순서와 완료 순서

작업:

```text
A 시작 10:00:00, 소요 3초
B 시작 10:00:01, 소요 1초
C 시작 10:00:01.5, 소요 5초
```

완료 순서를 적는다.

```text
A: 10:00:03
B: 10:00:02
C: 10:00:06.5
```

시작 순서 `A → B → C`와 완료 순서가 다를 수 있음을 확인한다.

---

## 훈련 B. 검색 stale response 버그

로그:

```text
11:00:00 query=A requestId=101 start
11:00:01 query=AA requestId=102 start
11:00:02 query=AAPL requestId=103 start
11:00:03 requestId=103 success items=20
11:00:04 requestId=101 success items=7
11:00:05 requestId=102 success items=12
11:00:05 screen shows query=AA
```

현재 검색창에는 `AAPL`이 입력되어 있다.

질문:

1. 서버 응답 세 개는 모두 성공했는가?
2. 최종 화면이 잘못된 이유는 무엇인가?
3. 어떤 기준으로 101/102 응답을 버릴 수 있는가?

가능한 답:

```text
현재 requestId와 응답 requestId 비교
현재 query와 응답 query 비교
이전 요청 취소
```

---

## 훈련 C. 로딩 상태가 풀리지 않는 실패 경로

상태 전이:

```text
Idle
↓ 검색
Loading
├─ success → Success
└─ error   → Error
```

코드 의사형식:

```text
loading = true
try:
    data = request()
    loading = false
except:
    error = "failed"
```

문제점을 찾는다.

오류 경로에서 `loading=false`가 없다.

수정 후 상태가 다음처럼 되어야 한다.

```text
실패 후 loading=false, error="failed"
```

---

## 훈련 D. 버튼 두 번 클릭

사용자가 저장 버튼을 0.2초 간격으로 두 번 눌렀다.

```text
save A start
save B start
save A success id=500
save B success id=501
```

같은 일정이 두 개 생겼다.

가능한 방어 전략을 **클라이언트 / 서버**로 나눈다.

클라이언트:

- 요청 중 버튼 비활성화
- 같은 화면에서 반복 탭 debounce/throttle

서버:

- idempotency key
- unique constraint
- 동일 요청 중복 감지

한쪽만 있으면 어떤 실패가 남을 수 있는지 생각한다.

---

## 훈련 E. 공유 카운터 경쟁 상태

의사코드:

```text
count = 10

Task A:
  x = count
  x = x + 1
  count = x

Task B:
  y = count
  y = y + 1
  count = y
```

다음 실행 순서에서 최종 count를 계산한다.

```text
A reads 10
B reads 10
A writes 11
B writes 11
```

결과: 11.

기대: 12.

이 문제가 왜 단순 산수 오류가 아닌지 설명한다.

---

## 훈련 F. 읽기와 쓰기 충돌

앱이 다음 데이터를 공유한다.

```text
portfolio = [AAPL, MSFT]
```

작업 A가 전체 목록을 서버에서 새로 받아 교체하고, 작업 B가 사용자의 종목 추가를 저장한다.

나쁜 순서:

```text
B: TSLA 추가 → [AAPL, MSFT, TSLA]
A: 이전 요청 결과 [AAPL, MSFT] 도착 → 전체 교체
```

TSLA가 사라진다.

질문:

- 어떤 작업이 최신 사용자 의도를 가지고 있는가?
- 전체 교체 방식이 왜 위험한가?
- 서버를 single source of truth로 둘 경우 어떤 순서/버전 정책이 필요한가?

---

## 훈련 G. UI thread 차단 판정

로그:

```text
12:00:00 button clicked
12:00:00 parseLargeFile() on main
12:00:08 parseLargeFile() complete
12:00:08 first frame after click
```

8초 동안 터치가 먹지 않았다.

가설:

```text
메인/UI 스레드에서 큰 파일 파싱이 실행됨
```

확인할 증거:

- thread dump
- profiler
- 호출 스택
- 해당 함수 실행 위치

해결 방향:

- 긴 작업을 UI thread 밖으로 이동
- 결과만 UI state에 반영

---

## 훈련 H. “async면 자동으로 병렬”이라는 오해

다음 명제를 판정한다.

1. `async`라는 단어가 있으면 CPU 작업이 자동으로 여러 코어에서 병렬 실행된다. → 항상 그렇지 않다.
2. 비동기 코드는 완료를 기다리는 흐름을 막지 않게 구성할 수 있다. → 맞다.
3. 비동기와 스레드는 완전히 같은 개념이다. → 틀림.
4. 비동기 코드는 순서 문제가 절대 없다. → 틀림.

이 네 문장을 본인 말로 다시 쓴다.

---

## 훈련 I. timeout 후 늦은 성공

상황:

```text
클라이언트 timeout = 3초
서버 작업 = 5초 후 성공
```

흐름:

```text
0초 요청 시작
3초 클라이언트 timeout → 실패로 인식
4초 사용자가 재시도
5초 첫 요청 서버에서 실제 저장 완료
9초 두 번째 요청도 저장 완료
```

결과: 중복 저장 가능.

질문:

- timeout이 “서버 작업이 취소됐다”는 뜻인가?
- 왜 idempotency가 필요한가?
- 서버에서 작업 상태를 조회할 수 있다면 어떻게 도움이 되는가?

---

## 훈련 J. 취소된 화면에 뒤늦게 결과 도착

흐름:

```text
화면 A 진입
요청 시작
화면 B로 이동
요청 완료
응답 콜백이 화면 A 상태 수정 시도
```

가능한 문제:

- 이미 사라진 화면 참조
- 메모리 누수
- 잘못된 상태 업데이트
- 사용자에게 보이지 않는 작업 낭비

해결 전략은 프레임워크에 따라 다르지만, **작업 생명주기와 화면 생명주기를 연결**해야 한다.

---

## 훈련 K. 동시 요청 개수 제한

이미지 1,000개를 한꺼번에 다운로드한다고 하자.

나쁜 전략:

```text
1,000개 요청 동시에 시작
```

가능한 문제:

- 메모리 증가
- 소켓/연결 제한
- 서버 rate limit
- 배터리/네트워크 부담

대안:

```text
동시 작업 개수를 제한
작업 큐 사용
실패 재시도 정책 분리
```

구체적 숫자는 환경에 따라 측정해서 정한다.

---

## 훈련 L. “마지막 write wins” 정책의 함정

두 기기에서 같은 메모를 수정한다.

```text
폰 A 10:00:10 내용="회의 3시"
폰 B 10:00:11 내용="회의 4시"
```

서버가 단순히 나중 write를 무조건 채택하면 B가 남는다.

그런데 폰 A의 시계가 2분 빠르다면?

단순 기기 timestamp를 믿는 방식은 위험할 수 있다.

질문:

- 서버 버전 번호를 쓸 수 있는가?
- 충돌을 사용자에게 보여줄 필요가 있는가?
- CRDT/OT 같은 고급 기술이 필요한 경우와 그렇지 않은 경우를 어떻게 구분할까?

이 장에서는 고급 알고리즘 구현이 목표가 아니다. **동기화 충돌에도 명시적인 정책이 필요하다**는 점을 확인한다.

---

## AI 답 검증 1

AI 답:

> 요청을 먼저 보냈으면 응답도 먼저 도착하므로 requestId 비교는 필요 없습니다.

판정: 틀림.

네트워크 지연, 서버 처리시간, 재시도 때문에 완료 순서는 달라질 수 있다.

---

## AI 답 검증 2

AI 답:

> UI가 멈추면 스레드를 무조건 많이 만들면 해결됩니다.

판정: 위험.

이유:

- 실제 병목이 네트워크/DB일 수 있음
- 스레드 증가 자체가 오버헤드
- 공유 상태 race 증가
- 적절한 coroutine/executor/framework 사용이 필요

---

## 디버깅 미션 1 — 오래된 결과 차단

의사코드:

```python
current_request = 0

async def search(query):
    global current_request
    current_request += 1
    my_id = current_request
    result = await fetch(query)
    show(result)
```

문제: 오래된 요청도 `show`한다.

수정 아이디어:

```python
if my_id == current_request:
    show(result)
```

이 코드는 개념 예시다. 실제 앱에서는 취소·예외·수명주기까지 함께 고려해야 한다.

---

## 디버깅 미션 2 — 중복 클릭

의사코드:

```text
onSaveClick:
    sendSaveRequest()
```

버튼을 여러 번 눌러도 계속 요청된다.

클라이언트 상태를 추가한다.

```text
if saving: return
saving = true
try save
finally saving = false
```

이것만으로 서버 중복을 100% 막을 수 없는 이유도 적는다.

---

## 독립 프로젝트 — 검색 화면 비동기 상태 머신

검색 화면을 다음 상태로 설계한다.

```text
Idle
Loading(query, requestId)
Success(query, requestId, items)
Empty(query, requestId)
Error(query, requestId, message)
```

### 1단계
각 상태에서 화면에 무엇을 보여줄지 적는다.

### 2단계
이벤트를 정한다.

```text
QueryChanged
SearchPressed
ResponseSuccess
ResponseEmpty
ResponseError
RetryPressed
ScreenClosed
```

### 3단계
오래된 응답이 왔을 때 규칙을 작성한다.

```text
response.requestId != current.requestId → ignore
```

### 4단계
다음 순서를 시뮬레이션한다.

```text
A 검색 → AA 검색 → A 응답 → AA 응답
```

각 순간 상태를 표로 적는다.

### 5단계
실패 경로에서 loading이 남지 않는지 검토한다.

---

# 3장 훈련 완료 기준

- 시작 순서와 완료 순서를 구분한다.
- stale response를 로그만 보고 판정할 수 있다.
- 실패/취소 경로에서 상태 정리가 필요한 이유를 안다.
- 중복 클릭을 클라이언트와 서버 양쪽에서 생각한다.
- 공유 상태의 race를 실행 순서로 설명한다.
- timeout이 서버 작업 취소를 보장하지 않는다는 점을 안다.
- 화면 생명주기와 비동기 작업의 관계를 검토한다.
- 검색 화면의 최소 상태 머신을 직접 설계할 수 있다.
