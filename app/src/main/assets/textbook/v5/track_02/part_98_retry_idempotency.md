# PART 98 · Retry와 idempotency — transient failure·backoff·중복 side effect를 함께 설계하기

Retry는 실패한 함수를 다시 호출하는 단순 loop가 아니다. 일시적 장애인지 영구 오류인지 분류해야 하고, 같은 operation을 다시 수행해도 side effect가 중복되지 않는지 확인해야 하며, 여러 client가 동시에 retry해 장애를 더 키우지 않도록 backoff와 budget을 둬야 한다. 이 절에서는 **실패 분류 → 재시도 가능성 → idempotency → 시간·횟수 budget → 관찰 가능성**의 순서로 본다.

---

## CHAPTER 01 · retry는 transient failure에만 적용해야 한다

### 시작 전 용어집

#### 1. retry

- **뜻:** `except Exception` 전체를 retry하면 programming bug와 permanent failure까지 반복해 장애를 숨긴다.
- **왜 중요한가:** Retryable exception/type/status를 명시적으로 allowlist한다.
- **예시:** for attempt in range(max_attempts): / try:

#### 2. transient failure

- **뜻:** Timeout, 일시적 connection reset, overload 같은 실패는 시간이 지나면 성공할 가능성이 있다.
- **왜 중요한가:** 반면 invalid input, authentication failure, schema mismatch는 같은 요청을 반복해도 바뀌지 않는 경우가 많다.
- **예시:** for attempt in range(max_attempts): / try:

#### 3. timeout

- **뜻:** Server가 명확한 retry hint를 제공한다면 그 의미를 사용하되 caller의 deadline과 policy를 넘지 않는다.
- **예시:** for attempt in range(max_attempts): / try:

```python
for attempt in range(max_attempts):
    try:
        return call()
    except TransientError:
        ...
```

 


---

## CHAPTER 02 · idempotent operation은 같은 요청을 반복해도 최종 효과가 한 번과 같도록 만든다

### 시작 전 용어집

#### 1. idempotent operation

- **뜻:** Read request나 특정 replace operation은 반복해도 결과가 동일할 수 있다.
- **왜 중요한가:** 하지만 “새 주문 생성”, “결제 승인” 같은 side effect는 두 번 실행하면 중복 결과를 만들 수 있다.
- **예시:** POST /payments / Idempotency-Key: operation-123

#### 2. idempotency

- **뜻:** HTTP method 이름만 보고 idempotency를 자동 판단하지 않는다.
- **왜 중요한가:** 실제 application semantics가 중요하다.
- **예시:** POST /payments / Idempotency-Key: operation-123

#### 3. timeout

- **뜻:** Server가 operation key와 request fingerprint/result를 저장하면 timeout 뒤 같은 요청을 다시 받아도 기존 결과를 반환할 수 있다.
- **왜 중요한가:** Client와 server 양쪽이 같은 logical operation identity를 공유해야 한다.
- **예시:** POST /payments / Idempotency-Key: operation-123

```text
POST /payments
Idempotency-Key: operation-123
```

 

---

## CHAPTER 03 · exponential backoff와 jitter는 동시 retry 폭주를 줄인다

### 시작 전 용어집

#### 1. exponential backoff

- **뜻:** 장애 직후 모든 client가 100ms마다 같은 간격으로 retry하면 recovery 중인 server에 다시 burst를 만들 수 있다.
- **왜 중요한가:** Attempt가 늘수록 delay를 키우고 random jitter를 섞어 retry 시점을 분산한다.
- **예시:** base * 2^attempt + randomized jitter

#### 2. jitter

- **뜻:** Jitter의 목적은 요청을 무작위로 만드는 것이 아니라 synchronized retry herd를 깨는 것이다.
- **왜 중요한가:** Backoff upper bound를 두지 않으면 recovery가 너무 늦어질 수 있고, 너무 작은 cap은 overload를 계속 유지할 수 있다.
- **예시:** base * 2^attempt + randomized jitter

#### 3. retry

- **뜻:** Service 특성과 caller deadline에 맞춰 조정한다.
- **예시:** base * 2^attempt + randomized jitter

```text
base * 2^attempt + randomized jitter
```

 


---

**운영 검증 98-3 — CHAPTER 03 · exponential backoff와 jitter는 동시 retry 폭주를 줄인다**
CHAPTER 03 · exponential backoff와 jitter는 동시 retry 폭주를 줄인다을 검증할 때는 요청과 처리 상태를 연결할 식별자를 정하고 같은 식별자로 다시 전달한다. PART 98 CHAPTER 3의 핵심 관찰값으로 종료 전 남은 작업과 종료 후 상태를 비교한다. P98-C03 복구 검증은 실패 직전과 재시작 직후의 상태를 나란히 놓는 방식으로 진행한다. 같은 요청을 P98-C03에 다시 전달했을 때 결과가 한 번만 반영되고 복구 경로가 결정적으로 끝나는지 본다. 수정 전후를 비교할 때는 성공 여부만 보지 말고 재시도 횟수, 중복 처리, 누락, 남은 작업을 함께 본다. 통과 기준은 반복 실행에서도 결과가 안정적이고, 실패가 생겨도 손실이나 무한 반복 없이 추적 가능한 상태로 종료되는 것이다.
## CHAPTER 04 · retry budget은 attempt 수뿐 아니라 전체 시간과 시스템 부하를 제한한다

### 시작 전 용어집

#### 1. retry

- **뜻:** System-wide retry budget도 중요하다.
- **왜 중요한가:** 원래 요청 1000개가 모두 3회 retry하면 downstream load가 최대 3000개 더 늘 수 있다.
- **예시:** `max_retries=5`만으로는 충분하지 않다.

#### 2. attempt

- **뜻:** 각 attempt가 10초 timeout이라면 최악의 경우 caller가 너무 오래 기다릴 수 있다.
- **왜 중요한가:** P97의 remaining deadline과 결합해 새 attempt를 시작할 시간이 있는지 본다.
- **예시:** `max_retries=5`만으로는 충분하지 않다.

#### 3. timeout

- **뜻:** `max_retries=5`만으로는 충분하지 않다.
- **왜 중요한가:** Retry ratio를 제한하거나 circuit breaker, admission control과 결합해 장애 중 추가 부하를 bounded하게 만든다.
- **예시:** `max_retries=5`만으로는 충분하지 않다.

---

**운영 검증 98-4 — CHAPTER 04 · retry budget은 attempt 수뿐 아니라 전체 시간과 시스템 부하를 제한한다**
CHAPTER 04 · retry budget은 attempt 수뿐 아니라 전체 시간과 시스템 부하를 제한한다을 검증할 때는 성공 기준을 수치나 상태로 정하고 처리 중간에 프로세스를 중단한다. PART 98 CHAPTER 4의 핵심 관찰값으로 대기열 길이와 거부된 요청 수를 같이 본다. P98-C04 실패 후 재처리는 멱등성 확인 단계다. P98-C04의 처리 횟수·최종 상태·재시도 로그를 함께 비교해 같은 입력이 여러 번 와도 데이터 의미가 변하지 않는지 검증한다. 수정 전후를 비교할 때는 성공 여부만 보지 말고 재시도 횟수, 중복 처리, 누락, 남은 작업을 함께 본다. 통과 기준은 반복 실행에서도 결과가 안정적이고, 실패가 생겨도 손실이나 무한 반복 없이 추적 가능한 상태로 종료되는 것이다.
## CHAPTER 05 · partial success는 “실패했으니 아무 일도 안 일어났다”는 가정을 깨뜨린다

### 시작 전 용어집

#### 1. partial success

- **뜻:** Client가 request를 보낸 뒤 response를 받기 전에 connection이 끊겼다면 server가 side effect를 완료했는지 알 수 없을 수 있다.
- **왜 중요한가:** 이 경우 retry는 duplicate side effect 위험을 가진다.
- **예시:** client sends -> server commits -> response lost …

#### 2. 실패

- **뜻:** Idempotency key, operation status 조회 API, transactional outbox 같은 pattern이 필요하다.
- **왜 중요한가:** Failure가 어느 단계에서 발생했는지 모르는 **ambiguous outcome**을 별도 상태로 모델링한다.
- **예시:** client sends -> server commits -> response lost …

```text
client sends -> server commits -> response lost -> client sees timeout
```

 

 단순 성공/실패 boolean로 줄이지 않는다.

---

**운영 검증 98-5 — CHAPTER 05 · partial success는 “실패했으니 아무 일도 안 일어났다”는 가정을 깨뜨린다**
CHAPTER 05 · partial success는 “실패했으니 아무 일도 안 일어났다”는 가정을 깨뜨린다을 검증할 때는 동일 입력을 재현 가능하게 고정하고 제한시간 직전에 실패시킨다. PART 98 CHAPTER 5의 핵심 관찰값으로 차단 상태와 재개 시점을 시간순으로 본다. P98-C05 장애 테스트가 끝나면 초기 상태를 복원하지 않고 바로 재실행해 복구성을 확인한다. P98-C05이 남은 상태를 정확히 읽고 중복·누락 없이 종료되면 재시도 계약이 지켜진 것이다. 수정 전후를 비교할 때는 성공 여부만 보지 말고 재시도 횟수, 중복 처리, 누락, 남은 작업을 함께 본다. 통과 기준은 반복 실행에서도 결과가 안정적이고, 실패가 생겨도 손실이나 무한 반복 없이 추적 가능한 상태로 종료되는 것이다.
## CHAPTER 06 · duplicate side effect는 request identity와 result persistence로 막는다

### 시작 전 용어집

#### 1. duplicate side

- **뜻:** Server가 idempotency key를 받았다면 같은 key의 최초 request payload hash와 result를 저장할 수 있다.
- **왜 중요한가:** 같은 key/같은 payload는 기존 결과를 재사용하고 같은 key/다른 payload는 conflict로 거부한다.
- **예시:** Server가 idempotency key를 받았다면 같은 key의 최초 request …

#### 2. effect

- **뜻:** Side effect와 idempotency record 저장이 서로 다른 transaction이면 crash 사이에 불일치가 생길 수 있다.
- **왜 중요한가:** 가능한 경우 같은 durable transaction 안에서 묶거나 recovery protocol을 둔다.
- **예시:** Side effect와 idempotency record 저장이 서로 다른 transaction이면 …

#### 3. request identity

- **뜻:** Key를 영원히 보관할 수 없다면 retention window를 정한다.
- **왜 중요한가:** Window 밖의 retry는 새 operation으로 처리될 수 있음을 client contract에 포함한다.
- **예시:** Key를 영원히 보관할 수 없다면 retention window를 정한다.

---

## CHAPTER 07 · retry observability는 최종 성공률만 보면 숨겨진 장애를 놓친다

### 시작 전 용어집

#### 1. retry

- **뜻:** Attempt count, first-failure reason, total retry delay, final outcome을 기록한다.
- **왜 중요한가:** Metrics에는 original request 수와 retry request 수를 분리한다.
- **예시:** Attempt count, first-failure reason, total retry delay, final …

#### 2. observability

- **뜻:** 세 번째 attempt에서 성공한 요청을 모두 “성공”으로만 집계하면 downstream이 이미 불안정하다는 신호를 잃는다.
- **왜 중요한가:** Retry amplification ratio가 급증하면 latency보다 먼저 장애 징후가 나타날 수 있다.
- **예시:** 세 번째 attempt에서 성공한 요청을 모두 “성공”으로만 집계하면 …

#### 3. stream

- **뜻:** 로그에 매 attempt stack trace를 남기면 noise가 커질 수 있으므로 반복 실패는 structured count로 모으고 최종 failure에 detail을 남기는 정책도 가능하다.
- **예시:** 로그에 매 attempt stack trace를 남기면 noise가 커질 …

---

## CHAPTER 08 · retry contract는 다시 시도할 조건과 중복 효과 방지를 함께 정의한다

### 시작 전 용어집

#### 1. retry

- **뜻:** Retry policy는 retryable failure 목록, max attempts, backoff/jitter, deadline budget, idempotency requirement, ambiguous outcome 처리, observability를 하나의 계약으로 갖는다.
- **왜 중요한가:** 테스트에서는 첫 attempt timeout 뒤 실제 server commit, 같은 idempotency key 재전송, permanent validation error, budget 소진, 여러 client의 동시 backoff를 검증한다.
- **예시:** Retry policy는 retryable failure 목록, max attempts, backoff/jitter, …

#### 2. deadline

- **뜻:** 이 PART의 핵심은 **retry를 성공률을 높이는 반복문으로 보지 않고, 불확실한 distributed outcome을 다시 실행하면서도 부하와 side effect 중복을 통제하는 복구 protocol로 설계하는 것**이다.
- **예시:** 이 PART의 핵심은 **retry를 성공률을 높이는 반복문으로 보지 …

---

## 실전 학습 루프 · retry와 idempotency

### 1. 쉬운 예

네트워크 timeout 뒤 서버가 실제로 결제를 성공시켰는지 모르는 상태에서 같은 요청을 재전송하면 중복 결제가 생길 수 있다. retry 전에 operation이 반복 실행되어도 안전한지 또는 idempotency key로 한 번의 논리 연산으로 묶을지 정해야 한다.

### 2. 한 줄 해석

retry는 오류 복구 기술이지만 idempotency가 없으면 실패를 중복 부작용으로 바꿀 수 있다.

### 3. 직접 실행

아래 최소 예제를 실행하기 전에 **성공 경로와 실패 경로를 각각 한 줄로 예측**한다.

```python
seen = {}

def charge(key, amount):
    if key in seen:
        return seen[key]
    result = {'charged': amount}
    seen[key] = result
    return result

print(charge('req-1', 1000))
print(charge('req-1', 1000))
```

### 4. 수정 실습

1. 같은 key에 다른 amount가 들어오면 reject하도록 request fingerprint를 추가한다.
2. 영구 validation error에는 retry하지 않고 transient error만 분류한다.

수정 뒤에는 같은 입력을 여러 번 실행하거나 중간 crash를 가정해 결과가 중복·누락·무한 대기로 바뀌지 않는지 확인한다.

### 5. 확인 문제

모든 예외에 3회 retry를 붙이면 성공률이 높아질까?

### 6. 정답과 오답 설명

**정답:** 아니다. 영구 오류에는 효과가 없고, 비멱등 부작용·과부하를 악화시킬 수 있다.

**자주 나오는 오답:** retry 횟수만 정하고 중복 실행 의미와 backoff·deadline을 정의하지 않는 것이 흔한 실수다.

운영형 문제에서는 함수 한 번의 정상 출력보다 **재시도, 중복, timeout, crash, 재시작** 뒤의 상태가 더 중요하다. 마지막으로 이 기능이 어떤 상태를 영구 저장하고 어떤 상태를 다시 계산할 수 있는지 구분해 적는다.

## 현장 디버깅 체크 · retry·idempotency

### 증상에서 시작한다

timeout 뒤 재시도했더니 결제·메일·DB write가 두 번 적용되거나, 같은 idempotency key에 다른 요청이 섞인다. 먼저 재현 가능한 최소 payload와 operation id를 고정한다. 최종 상태만 고치면 중복·정밀도·복구 문제의 실제 발생 지점을 숨길 수 있다.

### 먼저 볼 증거

operation key, request fingerprint, attempt number, remote result, local commit와 dedup record 순서를 기록한다. 가능하면 이 값을 하나의 trace 또는 audit record로 묶어 시간 순서를 복원한다.

### 일부러 실패시켜 보기

첫 attempt가 side effect 직후 응답 전에 끊기도록 실패를 넣고 동일 key 재시도 결과를 확인한다. 이런 반례가 자동 테스트에 들어가야 정상 예제만 통과하는 구현을 걸러낼 수 있다.

### 통과 기준

같은 논리 요청의 반복은 효과를 한 번만 만들고, 같은 key의 다른 payload는 충돌로 명시적으로 거부돼야 한다. 통과 기준은 “에러가 안 난다”가 아니라 **어떤 입력과 실패 순서에서도 허용된 상태 집합을 벗어나지 않는다**로 적는다.

