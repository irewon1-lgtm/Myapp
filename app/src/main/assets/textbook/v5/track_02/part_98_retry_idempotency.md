# PART 98 · Retry와 idempotency — transient failure·backoff·중복 side effect를 함께 설계하기

Retry는 실패한 함수를 다시 호출하는 단순 loop가 아니다. 일시적 장애인지 영구 오류인지 분류해야 하고, 같은 operation을 다시 수행해도 side effect가 중복되지 않는지 확인해야 하며, 여러 client가 동시에 retry해 장애를 더 키우지 않도록 backoff와 budget을 둬야 한다. 이 PART에서는 **실패 분류 → 재시도 가능성 → idempotency → 시간·횟수 budget → 관찰 가능성**의 순서로 본다.

---

## CHAPTER 01 · retry는 transient failure에만 적용해야 한다

Timeout, 일시적 connection reset, overload 같은 실패는 시간이 지나면 성공할 가능성이 있다. 반면 invalid input, authentication failure, schema mismatch는 같은 요청을 반복해도 바뀌지 않는 경우가 많다.

```python
for attempt in range(max_attempts):
    try:
        return call()
    except TransientError:
        ...
```

`except Exception` 전체를 retry하면 programming bug와 permanent failure까지 반복해 장애를 숨긴다. Retryable exception/type/status를 명시적으로 allowlist한다.

Server가 명확한 retry hint를 제공한다면 그 의미를 사용하되 caller의 deadline과 policy를 넘지 않는다.

---

## CHAPTER 02 · idempotent operation은 같은 요청을 반복해도 최종 효과가 한 번과 같도록 만든다

Read request나 특정 replace operation은 반복해도 결과가 동일할 수 있다. 하지만 “새 주문 생성”, “결제 승인” 같은 side effect는 두 번 실행하면 중복 결과를 만들 수 있다.

HTTP method 이름만 보고 idempotency를 자동 판단하지 않는다. 실제 application semantics가 중요하다.

```text
POST /payments
Idempotency-Key: operation-123
```

Server가 operation key와 request fingerprint/result를 저장하면 timeout 뒤 같은 요청을 다시 받아도 기존 결과를 반환할 수 있다. Client와 server 양쪽이 같은 logical operation identity를 공유해야 한다.

---

## CHAPTER 03 · exponential backoff와 jitter는 동시 retry 폭주를 줄인다

장애 직후 모든 client가 100ms마다 같은 간격으로 retry하면 recovery 중인 server에 다시 burst를 만들 수 있다. Attempt가 늘수록 delay를 키우고 random jitter를 섞어 retry 시점을 분산한다.

```text
base * 2^attempt + randomized jitter
```

Backoff upper bound를 두지 않으면 recovery가 너무 늦어질 수 있고, 너무 작은 cap은 overload를 계속 유지할 수 있다. Service 특성과 caller deadline에 맞춰 조정한다.

Jitter의 목적은 요청을 무작위로 만드는 것이 아니라 synchronized retry herd를 깨는 것이다.

---

## CHAPTER 04 · retry budget은 attempt 수뿐 아니라 전체 시간과 시스템 부하를 제한한다

`max_retries=5`만으로는 충분하지 않다. 각 attempt가 10초 timeout이라면 최악의 경우 caller가 너무 오래 기다릴 수 있다. P97의 remaining deadline과 결합해 새 attempt를 시작할 시간이 있는지 본다.

System-wide retry budget도 중요하다. 원래 요청 1000개가 모두 3회 retry하면 downstream load가 최대 3000개 더 늘 수 있다.

Retry ratio를 제한하거나 circuit breaker, admission control과 결합해 장애 중 추가 부하를 bounded하게 만든다.

---

## CHAPTER 05 · partial success는 “실패했으니 아무 일도 안 일어났다”는 가정을 깨뜨린다

Client가 request를 보낸 뒤 response를 받기 전에 connection이 끊겼다면 server가 side effect를 완료했는지 알 수 없을 수 있다.

```text
client sends -> server commits -> response lost -> client sees timeout
```

이 경우 retry는 duplicate side effect 위험을 가진다. Idempotency key, operation status 조회 API, transactional outbox 같은 pattern이 필요하다.

Failure가 어느 단계에서 발생했는지 모르는 **ambiguous outcome**을 별도 상태로 모델링한다. 단순 성공/실패 boolean로 줄이지 않는다.

---

## CHAPTER 06 · duplicate side effect는 request identity와 result persistence로 막는다

Server가 idempotency key를 받았다면 같은 key의 최초 request payload hash와 result를 저장할 수 있다. 같은 key/같은 payload는 기존 결과를 재사용하고 같은 key/다른 payload는 conflict로 거부한다.

Side effect와 idempotency record 저장이 서로 다른 transaction이면 crash 사이에 불일치가 생길 수 있다. 가능한 경우 같은 durable transaction 안에서 묶거나 recovery protocol을 둔다.

Key를 영원히 보관할 수 없다면 retention window를 정한다. Window 밖의 retry는 새 operation으로 처리될 수 있음을 client contract에 포함한다.

---

## CHAPTER 07 · retry observability는 최종 성공률만 보면 숨겨진 장애를 놓친다

세 번째 attempt에서 성공한 요청을 모두 “성공”으로만 집계하면 downstream이 이미 불안정하다는 신호를 잃는다. Attempt count, first-failure reason, total retry delay, final outcome을 기록한다.

Metrics에는 original request 수와 retry request 수를 분리한다. Retry amplification ratio가 급증하면 latency보다 먼저 장애 징후가 나타날 수 있다.

로그에 매 attempt stack trace를 남기면 noise가 커질 수 있으므로 반복 실패는 structured count로 모으고 최종 failure에 detail을 남기는 정책도 가능하다.

---

## CHAPTER 08 · retry contract는 다시 시도할 조건과 중복 효과 방지를 함께 정의한다

Retry policy는 retryable failure 목록, max attempts, backoff/jitter, deadline budget, idempotency requirement, ambiguous outcome 처리, observability를 하나의 계약으로 갖는다.

테스트에서는 첫 attempt timeout 뒤 실제 server commit, 같은 idempotency key 재전송, permanent validation error, budget 소진, 여러 client의 동시 backoff를 검증한다.

이 PART의 핵심은 **retry를 성공률을 높이는 반복문으로 보지 않고, 불확실한 distributed outcome을 다시 실행하면서도 부하와 side effect 중복을 통제하는 복구 protocol로 설계하는 것**이다.
