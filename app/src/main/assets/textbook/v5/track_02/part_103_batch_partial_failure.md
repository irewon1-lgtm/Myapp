# PART 103 · Batch partial failure — 검증·commit·오류 격리를 batch 단위와 item 단위로 나누기

Batch 처리에서는 1000개 중 999개가 정상이고 1개가 잘못됐을 때 전체를 실패시킬지, 999개만 반영할지 결정해야 한다. 이 선택은 단순 UX가 아니라 transaction boundary, replay 비용, downstream consistency를 바꾼다. 또한 batch가 커질수록 한 transaction의 lock·memory·rollback 비용도 커진다. 이 PART에서는 **batch 전체 의미와 item별 실패 의미를 분리하고 commit 단위를 명시적으로 선택**한다.

---

## CHAPTER 01 · batch unit은 단순히 여러 item을 한 번에 전달한다는 뜻이 아니다

100개 record가 하나의 business transaction을 구성하는지, 단지 network 효율을 위해 묶인 독립 operation 100개인지 먼저 구분한다.

예를 들어 은행 이체 묶음이 “전부 성공해야 하는 payroll”이라면 batch atomicity가 의미 있을 수 있다. 반면 analytics event 1000개는 일부 malformed record를 격리하고 나머지를 수용하는 편이 낫다.

Batch size와 transaction meaning을 같은 것으로 두지 않는다. Transport batch와 domain batch를 별도 개념으로 모델링하면 retry와 error reporting이 명확해진다.

---

## CHAPTER 02 · 가능한 검증은 side effect 전에 수행해 실패 범위를 줄인다

Schema, required field, local range check처럼 외부 state를 바꾸지 않고 확인할 수 있는 검증은 먼저 수행한다.

```text
parse -> structural validation -> domain validation -> side effect
```

1000개를 반영하다 999번째 invalid field에서 멈추는 것보다 사전 validation으로 invalid record를 먼저 찾는 편이 rollback 비용을 줄인다.

하지만 모든 검증을 사전에 할 수 있는 것은 아니다. Unique constraint나 account balance처럼 commit 시점의 current state가 필요한 검증은 transaction 안에서 다시 확인해야 한다. Pre-validation 성공을 final commit 보장으로 오해하지 않는다.

---

## CHAPTER 03 · transactional batch는 all-or-nothing invariant가 실제 요구일 때 사용한다

Database transaction 안에서 batch 전체를 적용하면 중간 item 실패 시 rollback할 수 있다. 이 방식은 결과가 명확하지만 batch가 매우 크면 lock duration, WAL/undo, memory 사용이 커질 수 있다.

외부 HTTP API나 email 전송처럼 database transaction으로 rollback할 수 없는 side effect가 섞이면 “batch transaction”이 전체 세계에 atomic한 것은 아니다. 외부 effect는 outbox나 compensation pattern으로 분리한다.

Atomicity 범위를 database transaction과 business workflow 전체에서 구분한다.

---

## CHAPTER 04 · chunk commit은 장애 반경을 줄이는 대신 checkpoint와 replay semantics를 요구한다

100만 record를 한 transaction으로 처리하는 대신 1000개씩 commit하면 lock과 rollback 크기를 줄일 수 있다.

```text
chunk 1 commit
chunk 2 commit
chunk 3 fails
```

이제 job 전체는 partial success 상태가 될 수 있다. 재시작할 때 chunk 1·2를 다시 처리할지, checkpoint에서 chunk 3부터 이어갈지 정책이 필요하다.

Chunk size는 throughput과 recovery cost의 trade-off다. 너무 작으면 commit overhead가 커지고 너무 크면 실패 시 반복 작업량이 커진다.

---

## CHAPTER 05 · partial result는 성공 count 하나보다 item별 outcome을 구조화한다

Batch API가 `True/False`만 반환하면 caller는 어느 item이 성공했는지 알 수 없다. Partial acceptance가 허용된다면 각 item의 status와 stable error code를 반환하거나 별도 result artifact를 제공한다.

```text
row 1 -> accepted
row 2 -> invalid_email
row 3 -> accepted
row 4 -> duplicate
```

원본 row number, logical ID, error category를 함께 보존하면 사용자가 수정 후 실패 항목만 재제출할 수 있다.

Error message 문자열만 machine contract로 사용하지 않는다. Code와 human message를 분리한다.

---

## CHAPTER 06 · poison record는 무한 retry를 막기 위해 격리한다

항상 같은 parser crash나 permanent constraint failure를 만드는 item이 queue head에 있으면 worker가 반복 실패하며 전체 progress를 막을 수 있다.

Retry 횟수나 failure classification을 기준으로 dead-letter/quarantine 영역으로 옮기고 나머지 item을 계속 처리할 수 있다. 하지만 자동 격리가 business data loss로 이어지지 않도록 alert와 manual review path를 둔다.

Programming bug 때문에 정상 record가 모두 poison처럼 보일 수 있으므로 갑작스러운 quarantine 급증을 모니터링한다.

---

## CHAPTER 07 · replay는 이미 성공한 effect를 중복시키지 않는 경계가 필요하다

Batch job을 처음부터 재실행하는 것이 가장 단순한 복구 방법일 수 있지만 output operation이 idempotent해야 한다. Stable item ID와 upsert/dedup rule을 사용하면 replay-safe하게 만들 수 있다.

반대로 increment, email send, external charge 같은 non-idempotent effect는 별도 operation ID와 result log가 필요하다. P102의 delivery semantics와 연결된다.

Replay mode에서 현재 schema/version이 과거와 달라졌다면 같은 input이 다른 output을 만들 수 있다. Processing version도 checkpoint와 함께 기록한다.

---

## CHAPTER 08 · batch contract는 atomicity·partial acceptance·replay 단위를 명시한다

Batch API를 설계할 때는 전체 성공이 필요한지, invalid item만 제외할 수 있는지, commit chunk 크기와 checkpoint가 무엇인지, poison record와 replay를 어떻게 다룰지 정한다.

테스트에서는 첫 item 실패, 중간 item 실패, commit 직후 crash, chunk 경계 duplicate, permanent poison item, result reporting 누락을 각각 검증한다.

이 PART의 핵심은 **batch를 단순 반복문 최적화로 보지 않고, 어떤 단위로 검증·commit·실패·재시작할지를 결정하는 독립된 consistency protocol로 설계하는 것**이다.
