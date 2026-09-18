# PART 102 · Delivery와 dedup semantics — at-most-once·at-least-once·exactly-once 주장을 검증하기

메시지 queue나 비동기 worker에서 “한 번 처리한다”는 말은 network failure와 process crash가 들어오면 모호해진다. Consumer가 작업을 수행한 뒤 ack 전에 죽으면 broker는 같은 메시지를 다시 전달할 수 있고, ack를 먼저 보낸 뒤 작업 중 죽으면 메시지는 다시 오지 않을 수 있다. 따라서 delivery semantics는 **언제 ack하는가, side effect를 어떻게 기록하는가, duplicate를 어떻게 식별하는가**의 조합으로 이해해야 한다.

---

## CHAPTER 01 · delivery semantics는 전송 횟수보다 관찰 가능한 효과를 설명해야 한다

### 시작 전 용어집

#### 1. delivery

- **뜻:** 따라서 “exactly once” 같은 문장을 볼 때는 transport delivery인지, handler execution인지, durable business effect인지 범위를 먼저 묻는다.
- **왜 중요한가:** Failure boundary를 그리면 의미가 선명해진다.
- **예시:** receive -> process -> commit -> acknowledge

#### 2. retry

- **뜻:** Broker가 한 번만 deliver해도 consumer 내부 retry가 두 번 write할 수 있고, broker가 여러 번 deliver해도 dedup으로 효과를 한 번만 만들 수 있다.
- **왜 중요한가:** Crash가 어느 화살표 사이에 발생하는지에 따라 손실과 중복 가능성이 달라진다.
- **예시:** receive -> process -> commit -> acknowledge

#### 3. commit

- **뜻:** Message가 wire를 한 번 건넜는지보다 application side effect가 몇 번 발생할 수 있는지가 중요하다.
- **예시:** receive -> process -> commit -> acknowledge

```text
receive -> process -> commit -> acknowledge
```


---

## CHAPTER 02 · at-most-once는 중복보다 손실을 허용하는 선택이다

### 시작 전 용어집

#### 1. at-most-once

- **뜻:** 그러나 결제나 주문 처리에 at-most-once를 쓰면서 “중복이 없으니 안전하다”고 말하면 손실 위험을 숨긴다.
- **왜 중요한가:** Delivery guarantee는 어느 실패를 선택적으로 허용하는지 명시해야 한다.
- **예시:** 그러나 결제나 주문 처리에 at-most-once를 쓰면서 “중복이 없으니 …

#### 2. delivery

- **뜻:** 메시지를 받은 직후 ack하고 그 뒤 처리한다면 같은 message를 다시 받을 가능성은 줄지만 ack 뒤 process가 죽으면 작업이 영구 손실될 수 있다.
- **왜 중요한가:** Metric sample, best-effort telemetry처럼 일부 손실이 중복보다 낫거나 재생성 가능한 데이터에서는 이 trade-off가 합리적일 수 있다.
- **예시:** 메시지를 받은 직후 ack하고 그 뒤 처리한다면 같은 …

---

**현장 점검 102-2 — CHAPTER 02 · at-most-once는 중복보다 손실을 허용하는 선택이다**
재현 가능한 입력과 초기 상태를 먼저 고정한다. CHAPTER 02 · at-most-once는 중복보다 손실을 허용하는 선택이다 검증에서는 각 단계의 입력과 출력에 식별자를 붙여 어떤 요청이 어디서 멈췄는지 추적한다. PART 102의 CHAPTER 2은 중간 중단이나 재전달이 발생해도 상태가 뒤섞이지 않는지 확인하는 것이 핵심이므로, 실패 조건은 한 번에 하나만 주입하고 나머지 조건은 고정한다. 수정 뒤에는 원래 정상 사례, 방금 만든 실패 사례, 같은 요청을 다시 보내는 재실행 사례를 순서대로 반복한다. 통과 기준은 장애 주입 전후 모두 동일한 비즈니스 불변식이 유지되는 것이다.
## CHAPTER 03 · at-least-once는 손실을 줄이는 대신 duplicate processing을 정상 상태로 만든다

### 시작 전 용어집

#### 1. at-least-once

- **뜻:** 처리를 완료하고 durable state를 남긴 뒤 ack하면 ack 전에 crash할 경우 message가 다시 전달될 수 있다.
- **왜 중요한가:** 이 duplicate는 예외적인 버그가 아니라 protocol이 허용하는 정상 failure path다.
- **예시:** 처리를 완료하고 durable state를 남긴 뒤 ack하면 ack …

#### 2. duplicate

- **뜻:** 따라서 handler를 idempotent하게 만들거나 message ID 기반 dedup을 둔다.
- **왜 중요한가:** UNIQUE(message_id)`처럼 durable storage가 동일 operation의 두 번째 적용을 막게 할 수 있다.
- **예시:** 따라서 handler를 idempotent하게 만들거나 message ID 기반 dedup을 …

#### 3. 상태

- **뜻:** Consumer code가 “이 메시지는 한 번만 온다”고 가정하면 production crash에서만 데이터가 중복된다.
- **예시:** Consumer code가 “이 메시지는 한 번만 온다”고 가정하면 …

`INSERT ... 


---

**현장 점검 102-3 — CHAPTER 03 · at-least-once는 손실을 줄이는 대신 duplicate processing을 정상 상태로 만든다**
중복·지연·부분 실패 중 가장 재현이 쉬운 조건부터 선택한다. CHAPTER 03 · at-least-once는 손실을 줄이는 대신 duplicate processing을 정상 상태로 만든다 검증에서는 복구 직후 동일 입력을 다시 넣어 멱등성과 중복 방지 계약을 검증한다. PART 102의 CHAPTER 3은 중간 중단이나 재전달이 발생해도 상태가 뒤섞이지 않는지 확인하는 것이 핵심이므로, 실패 조건은 한 번에 하나만 주입하고 나머지 조건은 고정한다. 수정 뒤에는 원래 정상 사례, 방금 만든 실패 사례, 같은 요청을 다시 보내는 재실행 사례를 순서대로 반복한다. 통과 기준은 성공 항목이 다시 처리되지 않고 실패 항목만 정해진 정책으로 재시도되는 것이다.
## CHAPTER 04 · exactly-once는 어느 경계까지 보장하는지 증명해야 한다

### 시작 전 용어집

#### 1. exactly-once

- **뜻:** Transport가 exactly-once delivery를 주장하더라도 consumer가 외부 payment API와 database 두 곳에 side effect를 만들면 전체 business operation까지 exactly once가 자동 보장되지는 않는다.
- **왜 중요한가:** Exactly-once에 가까운 효과는 idempotent operation, transactional state, dedup, replay-safe output을 조합해 특정 boundary 안에서 만들 수 있다.
- **예시:** broker transaction != arbitrary external-world transaction

#### 2. 경계

- **뜻:** 보장 범위를 system diagram으로 표시한다.
- **왜 중요한가:** “중복이 절대 없다”보다 “동일 operation ID가 이 database effect에 두 번 적용되지 않는다”처럼 검증 가능한 문장으로 쓴다.
- **예시:** broker transaction != arbitrary external-world transaction

```text
broker transaction != arbitrary external-world transaction
```

 


---

## CHAPTER 05 · dedup store는 message ID와 처리 결과를 durable하게 묶는다

### 시작 전 용어집

#### 1. dedup store

- **뜻:** 단순 in-memory `seen_ids` set은 process restart 후 사라지므로 durable delivery dedup에 충분하지 않다.
- **왜 중요한가:** Database table에 message ID와 processing state/result를 기록할 수 있다.
- **예시:** message_id | payload_hash | status | result_ref | …

#### 2. message

- **뜻:** 같은 ID에 다른 payload가 들어오면 collision이나 producer bug일 수 있으므로 무조건 기존 결과를 반환하지 않고 conflict를 감지한다.
- **왜 중요한가:** Dedup record 작성과 business effect가 별도 transaction이면 crash gap이 생긴다.
- **예시:** message_id | payload_hash | status | result_ref | …

#### 3. durable

- **뜻:** 가능한 경우 같은 transaction 안에 두거나 inbox/outbox pattern으로 연결한다.
- **예시:** message_id | payload_hash | status | result_ref | …

```text
message_id | payload_hash | status | result_ref | processed_at
```


 

---

## CHAPTER 06 · inbox/outbox는 database commit과 message publication 사이의 gap을 줄인다

### 시작 전 용어집

#### 1. inbox

- **뜻:** Consumer 쪽 inbox는 받은 message ID를 business update와 함께 기록해 duplicate 적용을 막을 수 있다.
- **왜 중요한가:** 이 pattern도 publisher delivery가 중복될 수 있으므로 downstream idempotency가 필요하다.
- **예시:** Consumer 쪽 inbox는 받은 message ID를 business update와 …

#### 2. outbox

- **뜻:** Transactional outbox는 business change와 “보낼 event” row를 같은 transaction에 저장하고 별도 publisher가 outbox를 전송한다.
- **왜 중요한가:** 목표는 불가능한 전역 atomic transaction을 가장 작은 durable boundary들로 분해하는 것이다.
- **예시:** Transactional outbox는 business change와 “보낼 event” row를 같은 …

#### 3. database commit

- **뜻:** Business row를 commit한 뒤 event publish 전에 process가 죽으면 상태는 바뀌었지만 event가 사라질 수 있다.
- **왜 중요한가:** 반대로 event를 먼저 publish하고 DB commit이 실패하면 존재하지 않는 상태에 대한 event가 나갈 수 있다.
- **예시:** Business row를 commit한 뒤 event publish 전에 process가 …

---

## CHAPTER 07 · dedup retention과 ordering은 오래된 duplicate의 의미를 바꾼다

### 시작 전 용어집

#### 1. dedup retention

- **뜻:** Dedup record를 영원히 보관할 수 없다면 retention 기간이 필요하다.
- **왜 중요한가:** 기간이 지난 후 매우 늦은 duplicate가 오면 새 message로 처리될 수 있다.
- **예시:** Dedup record를 영원히 보관할 수 없다면 retention 기간이 …

#### 2. ordering

- **뜻:** Message ID가 시간 순서와 결합되어 있거나 producer sequence를 가진다면 old replay를 거부할 수 있지만 partition별 ordering과 재처리 요구를 함께 고려한다.
- **왜 중요한가:** Backfill이나 disaster recovery에서는 과거 event를 의도적으로 replay할 수 있다.
- **예시:** Message ID가 시간 순서와 결합되어 있거나 producer sequence를 …

#### 3. duplicate

- **뜻:** “duplicate를 제거한다”와 “replay를 허용한다”는 충돌할 수 있으므로 replay namespace/generation을 분리할 수 있다.
- **예시:** “duplicate를 제거한다”와 “replay를 허용한다”는 충돌할 수 있으므로 replay …

---

## CHAPTER 08 · delivery contract는 crash 위치별 결과를 표로 설명할 수 있어야 한다

### 시작 전 용어집

#### 1. delivery

- **뜻:** 테스트에서는 commit 직후 강제 crash, duplicate redelivery, 같은 ID의 payload mismatch, dedup retention 만료, outbox publisher 재시도를 주입한다.
- **왜 중요한가:** 이 PART의 핵심은 **delivery guarantee를 마케팅 용어로 받아들이지 않고, ack·durable commit·dedup record의 순서에서 손실과 중복 가능성을 실제로 추적해 보장 범위를 정의하는 것**이다.
- **예시:** 테스트에서는 commit 직후 강제 crash, duplicate redelivery, 같은 …

#### 2. crash

- **뜻:** 안정적인 worker는 receive 전, process 중, commit 후 ack 전, ack 후 각 지점에서 crash했을 때 message와 side effect가 어떻게 되는지 설명할 수 있어야 한다.
- **예시:** 안정적인 worker는 receive 전, process 중, commit 후 …

---

## 실전 학습 루프 · delivery dedup semantics

### 1. 쉬운 예

message broker가 at-least-once delivery를 제공하면 같은 event가 다시 올 수 있다. consumer는 event id·operation id를 기록하고 이미 적용한 부작용인지 확인해야 한다. 단, dedup 저장과 실제 부작용의 원자성도 함께 설계해야 한다.

### 2. 한 줄 해석

중복 전달을 막는다는 것은 message를 한 번만 받는 것이 아니라 같은 논리 효과를 한 번만 적용하도록 만드는 문제다.

### 3. 직접 실행

아래 최소 예제를 실행하기 전에 **성공 경로와 실패 경로를 각각 한 줄로 예측**한다.

```python
processed = set()

def handle(event_id, value):
    if event_id in processed:
        return 'duplicate'
    # 실제 side effect와 processed 기록의 원자성 필요
    processed.add(event_id)
    return f'applied:{value}'
```

### 4. 수정 실습

1. 처리 성공 뒤 dedup 기록 전에 crash하는 경우를 재현 순서로 적는다.
2. dedup key 보존 기간이 너무 짧거나 무한히 길 때 각각 생기는 문제를 비교한다.

수정 뒤에는 같은 입력을 여러 번 실행하거나 중간 crash를 가정해 결과가 중복·누락·무한 대기로 바뀌지 않는지 확인한다.

### 5. 확인 문제

broker가 at-least-once라면 consumer에서 exactly-once 효과가 자동으로 생길까?

### 6. 정답과 오답 설명

**정답:** 아니다. consumer의 idempotency/dedup과 transaction 경계를 직접 설계해야 한다.

**자주 나오는 오답:** 전달 횟수와 부작용 적용 횟수를 같은 개념으로 보는 것이 오답이다.

운영형 문제에서는 함수 한 번의 정상 출력보다 **재시도, 중복, timeout, crash, 재시작** 뒤의 상태가 더 중요하다. 마지막으로 이 기능이 어떤 상태를 영구 저장하고 어떤 상태를 다시 계산할 수 있는지 구분해 적는다.

## 현장 디버깅 체크 · delivery dedup

### 증상에서 시작한다

같은 event가 재전달될 때 일부는 중복 적용되고, dedup table에는 처리 완료로 남았는데 실제 side effect는 빠진다. 먼저 재현 가능한 최소 payload와 operation id를 고정한다. 최종 상태만 고치면 중복·정밀도·복구 문제의 실제 발생 지점을 숨길 수 있다.

### 먼저 볼 증거

event/operation id, payload fingerprint, side-effect transaction id, dedup record commit 순서와 broker ack 시각을 연결한다. 가능하면 이 값을 하나의 trace 또는 audit record로 묶어 시간 순서를 복원한다.

### 일부러 실패시켜 보기

side effect 전·후, dedup write 전·후, ack 전·후에 crash를 각각 주입해 재전달 결과를 비교한다. 이런 반례가 자동 테스트에 들어가야 정상 예제만 통과하는 구현을 걸러낼 수 있다.

### 통과 기준

모든 crash point에서 결과가 누락되지 않고 중복 효과도 생기지 않으며 재전달 판단 근거가 durable하게 남아야 한다. 통과 기준은 “에러가 안 난다”가 아니라 **어떤 입력과 실패 순서에서도 허용된 상태 집합을 벗어나지 않는다**로 적는다.

