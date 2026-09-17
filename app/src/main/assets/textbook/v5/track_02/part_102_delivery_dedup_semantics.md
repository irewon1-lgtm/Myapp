# PART 102 · Delivery와 dedup semantics — at-most-once·at-least-once·exactly-once 주장을 검증하기

메시지 queue나 비동기 worker에서 “한 번 처리한다”는 말은 network failure와 process crash가 들어오면 모호해진다. Consumer가 작업을 수행한 뒤 ack 전에 죽으면 broker는 같은 메시지를 다시 전달할 수 있고, ack를 먼저 보낸 뒤 작업 중 죽으면 메시지는 다시 오지 않을 수 있다. 따라서 delivery semantics는 **언제 ack하는가, side effect를 어떻게 기록하는가, duplicate를 어떻게 식별하는가**의 조합으로 이해해야 한다.

---

## CHAPTER 01 · delivery semantics는 전송 횟수보다 관찰 가능한 효과를 설명해야 한다

Message가 wire를 한 번 건넜는지보다 application side effect가 몇 번 발생할 수 있는지가 중요하다. Broker가 한 번만 deliver해도 consumer 내부 retry가 두 번 write할 수 있고, broker가 여러 번 deliver해도 dedup으로 효과를 한 번만 만들 수 있다.

따라서 “exactly once” 같은 문장을 볼 때는 transport delivery인지, handler execution인지, durable business effect인지 범위를 먼저 묻는다.

Failure boundary를 그리면 의미가 선명해진다.

```text
receive -> process -> commit -> acknowledge
```

Crash가 어느 화살표 사이에 발생하는지에 따라 손실과 중복 가능성이 달라진다.

---

## CHAPTER 02 · at-most-once는 중복보다 손실을 허용하는 선택이다

메시지를 받은 직후 ack하고 그 뒤 처리한다면 같은 message를 다시 받을 가능성은 줄지만 ack 뒤 process가 죽으면 작업이 영구 손실될 수 있다.

Metric sample, best-effort telemetry처럼 일부 손실이 중복보다 낫거나 재생성 가능한 데이터에서는 이 trade-off가 합리적일 수 있다.

그러나 결제나 주문 처리에 at-most-once를 쓰면서 “중복이 없으니 안전하다”고 말하면 손실 위험을 숨긴다. Delivery guarantee는 어느 실패를 선택적으로 허용하는지 명시해야 한다.

---

## CHAPTER 03 · at-least-once는 손실을 줄이는 대신 duplicate processing을 정상 상태로 만든다

처리를 완료하고 durable state를 남긴 뒤 ack하면 ack 전에 crash할 경우 message가 다시 전달될 수 있다. 이 duplicate는 예외적인 버그가 아니라 protocol이 허용하는 정상 failure path다.

따라서 handler를 idempotent하게 만들거나 message ID 기반 dedup을 둔다. `INSERT ... UNIQUE(message_id)`처럼 durable storage가 동일 operation의 두 번째 적용을 막게 할 수 있다.

Consumer code가 “이 메시지는 한 번만 온다”고 가정하면 production crash에서만 데이터가 중복된다.

---

## CHAPTER 04 · exactly-once는 어느 경계까지 보장하는지 증명해야 한다

Transport가 exactly-once delivery를 주장하더라도 consumer가 외부 payment API와 database 두 곳에 side effect를 만들면 전체 business operation까지 exactly once가 자동 보장되지는 않는다.

```text
broker transaction != arbitrary external-world transaction
```

Exactly-once에 가까운 효과는 idempotent operation, transactional state, dedup, replay-safe output을 조합해 특정 boundary 안에서 만들 수 있다. 보장 범위를 system diagram으로 표시한다.

“중복이 절대 없다”보다 “동일 operation ID가 이 database effect에 두 번 적용되지 않는다”처럼 검증 가능한 문장으로 쓴다.

---

## CHAPTER 05 · dedup store는 message ID와 처리 결과를 durable하게 묶는다

단순 in-memory `seen_ids` set은 process restart 후 사라지므로 durable delivery dedup에 충분하지 않다. Database table에 message ID와 processing state/result를 기록할 수 있다.

```text
message_id | payload_hash | status | result_ref | processed_at
```

같은 ID에 다른 payload가 들어오면 collision이나 producer bug일 수 있으므로 무조건 기존 결과를 반환하지 않고 conflict를 감지한다.

Dedup record 작성과 business effect가 별도 transaction이면 crash gap이 생긴다. 가능한 경우 같은 transaction 안에 두거나 inbox/outbox pattern으로 연결한다.

---

## CHAPTER 06 · inbox/outbox는 database commit과 message publication 사이의 gap을 줄인다

Business row를 commit한 뒤 event publish 전에 process가 죽으면 상태는 바뀌었지만 event가 사라질 수 있다. 반대로 event를 먼저 publish하고 DB commit이 실패하면 존재하지 않는 상태에 대한 event가 나갈 수 있다.

Transactional outbox는 business change와 “보낼 event” row를 같은 transaction에 저장하고 별도 publisher가 outbox를 전송한다. Consumer 쪽 inbox는 받은 message ID를 business update와 함께 기록해 duplicate 적용을 막을 수 있다.

이 pattern도 publisher delivery가 중복될 수 있으므로 downstream idempotency가 필요하다. 목표는 불가능한 전역 atomic transaction을 가장 작은 durable boundary들로 분해하는 것이다.

---

## CHAPTER 07 · dedup retention과 ordering은 오래된 duplicate의 의미를 바꾼다

Dedup record를 영원히 보관할 수 없다면 retention 기간이 필요하다. 기간이 지난 후 매우 늦은 duplicate가 오면 새 message로 처리될 수 있다.

Message ID가 시간 순서와 결합되어 있거나 producer sequence를 가진다면 old replay를 거부할 수 있지만 partition별 ordering과 재처리 요구를 함께 고려한다.

Backfill이나 disaster recovery에서는 과거 event를 의도적으로 replay할 수 있다. “duplicate를 제거한다”와 “replay를 허용한다”는 충돌할 수 있으므로 replay namespace/generation을 분리할 수 있다.

---

## CHAPTER 08 · delivery contract는 crash 위치별 결과를 표로 설명할 수 있어야 한다

안정적인 worker는 receive 전, process 중, commit 후 ack 전, ack 후 각 지점에서 crash했을 때 message와 side effect가 어떻게 되는지 설명할 수 있어야 한다.

테스트에서는 commit 직후 강제 crash, duplicate redelivery, 같은 ID의 payload mismatch, dedup retention 만료, outbox publisher 재시도를 주입한다.

이 PART의 핵심은 **delivery guarantee를 마케팅 용어로 받아들이지 않고, ack·durable commit·dedup record의 순서에서 손실과 중복 가능성을 실제로 추적해 보장 범위를 정의하는 것**이다.
