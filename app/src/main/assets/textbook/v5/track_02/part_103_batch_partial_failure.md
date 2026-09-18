# PART 103 · Batch partial failure — 검증·commit·오류 격리를 batch 단위와 item 단위로 나누기

Batch 처리에서는 1000개 중 999개가 정상이고 1개가 잘못됐을 때 전체를 실패시킬지, 999개만 반영할지 결정해야 한다. 이 선택은 단순 UX가 아니라 transaction boundary, replay 비용, downstream consistency를 바꾼다. 또한 batch가 커질수록 한 transaction의 lock·memory·rollback 비용도 커진다. 이 절에서는 **batch 전체 의미와 item별 실패 의미를 분리하고 commit 단위를 명시적으로 선택**한다.

---

## CHAPTER 01 · batch unit은 단순히 여러 item을 한 번에 전달한다는 뜻이 아니다

### 시작 전 용어집

#### 1. batch

- **뜻:** 예를 들어 은행 이체 묶음이 “전부 성공해야 하는 payroll”이라면 batch atomicity가 의미 있을 수 있다.
- **왜 중요한가:** 반면 analytics event 1000개는 일부 malformed record를 격리하고 나머지를 수용하는 편이 낫다.
- **예시:** 예를 들어 은행 이체 묶음이 “전부 성공해야 하는 …

#### 2. item

- **뜻:** 100개 record가 하나의 business transaction을 구성하는지, 단지 network 효율을 위해 묶인 독립 operation 100개인지 먼저 구분한다.
- **왜 중요한가:** Batch size와 transaction meaning을 같은 것으로 두지 않는다.
- **예시:** 100개 record가 하나의 business transaction을 구성하는지, 단지 network …

#### 3. retry

- **뜻:** Transport batch와 domain batch를 별도 개념으로 모델링하면 retry와 error reporting이 명확해진다.
- **예시:** Transport batch와 domain batch를 별도 개념으로 모델링하면 retry와 …

---

## CHAPTER 02 · 가능한 검증은 side effect 전에 수행해 실패 범위를 줄인다

### 시작 전 용어집

#### 1. 검증

- **뜻:** Schema, required field, local range check처럼 외부 state를 바꾸지 않고 확인할 수 있는 검증은 먼저 수행한다.
- **왜 중요한가:** 1000개를 반영하다 999번째 invalid field에서 멈추는 것보다 사전 validation으로 invalid record를 먼저 찾는 편이 rollback 비용을 줄인다.
- **예시:** parse -> structural validation -> domain validation -> …

#### 2. side effect

- **뜻:** 하지만 모든 검증을 사전에 할 수 있는 것은 아니다.
- **왜 중요한가:** Unique constraint나 account balance처럼 commit 시점의 current state가 필요한 검증은 transaction 안에서 다시 확인해야 한다.
- **예시:** parse -> structural validation -> domain validation -> …

#### 3. 실패

- **뜻:** Pre-validation 성공을 final commit 보장으로 오해하지 않는다.
- **예시:** parse -> structural validation -> domain validation -> …

```text
parse -> structural validation -> domain validation -> side effect
```


  

---

## CHAPTER 03 · transactional batch는 all-or-nothing invariant가 실제 요구일 때 사용한다

### 시작 전 용어집

#### 1. transactional batch

- **뜻:** Database transaction 안에서 batch 전체를 적용하면 중간 item 실패 시 rollback할 수 있다.
- **왜 중요한가:** 이 방식은 결과가 명확하지만 batch가 매우 크면 lock duration, WAL/undo, memory 사용이 커질 수 있다.
- **예시:** Database transaction 안에서 batch 전체를 적용하면 중간 item …

#### 2. all-or-nothing invariant

- **뜻:** 외부 HTTP API나 email 전송처럼 database transaction으로 rollback할 수 없는 side effect가 섞이면 “batch transaction”이 전체 세계에 atomic한 것은 아니다.
- **왜 중요한가:** 외부 effect는 outbox나 compensation pattern으로 분리한다.
- **예시:** 외부 HTTP API나 email 전송처럼 database transaction으로 rollback할 …

#### 3. 실패

- **뜻:** Atomicity 범위를 database transaction과 business workflow 전체에서 구분한다.
- **예시:** Atomicity 범위를 database transaction과 business workflow 전체에서 구분한다.

---

## CHAPTER 04 · chunk commit은 장애 반경을 줄이는 대신 checkpoint와 replay semantics를 요구한다

### 시작 전 용어집

#### 1. chunk commit

- **뜻:** 100만 record를 한 transaction으로 처리하는 대신 1000개씩 commit하면 lock과 rollback 크기를 줄일 수 있다.
- **왜 중요한가:** 이제 job 전체는 partial success 상태가 될 수 있다.
- **예시:** chunk 1 commit / chunk 2 commit

#### 2. checkpoint

- **뜻:** 재시작할 때 chunk 1·2를 다시 처리할지, checkpoint에서 chunk 3부터 이어갈지 정책이 필요하다.
- **왜 중요한가:** Chunk size는 throughput과 recovery cost의 trade-off다.
- **예시:** chunk 1 commit / chunk 2 commit

#### 3. replay semantics

- **뜻:** 너무 작으면 commit overhead가 커지고 너무 크면 실패 시 반복 작업량이 커진다.
- **예시:** chunk 1 commit / chunk 2 commit

```text
chunk 1 commit
chunk 2 commit
chunk 3 fails
```

 

 

---

**검증 시나리오 P103-C4 — CHAPTER 04 · chunk commit은 장애 반경을 줄이는 대신 checkpoint와 replay semantics를 요구한다**
`CHAPTER 04 · chunk commit은 장애 반경을 줄이는 대신 checkpoint와 replay semantics를 요구한다` 검증은 정상 경로를 먼저 재현하는 데서 시작한다. P103-C4에서는 `CHAPTER 04 · chunk commit은 장애 반경을 줄이는 대신 checkpoint와 replay semantics를 요구한다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 04 · chunk commit은 장애 반경을 줄이는 대신 checkpoint와 replay semantics를 요구한다`에 대해 오류 경로 하나를 의도적으로 만든다고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 04 · chunk commit은 장애 반경을 줄이는 대신 checkpoint와 replay semantics를 요구한다`의 예외 종류와 직전 상태를 함께 남긴다하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 04 · chunk commit은 장애 반경을 줄이는 대신 checkpoint와 replay semantics를 요구한다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P103-C4의 마무리는 복구 후 같은 오류가 다시 재현되지 않는지 검사하는 것이다. 통과 기준은 `CHAPTER 04 · chunk commit은 장애 반경을 줄이는 대신 checkpoint와 replay semantics를 요구한다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 05 · partial result는 성공 count 하나보다 item별 outcome을 구조화한다

### 시작 전 용어집

#### 1. partial result

- **뜻:** Batch API가 `True/False`만 반환하면 caller는 어느 item이 성공했는지 알 수 없다.
- **왜 중요한가:** Partial acceptance가 허용된다면 각 item의 status와 stable error code를 반환하거나 별도 result artifact를 제공한다.
- **예시:** row 1 -> accepted / row 2 -> …

#### 2. count

- **뜻:** 원본 row number, logical ID, error category를 함께 보존하면 사용자가 수정 후 실패 항목만 재제출할 수 있다.
- **왜 중요한가:** Error message 문자열만 machine contract로 사용하지 않는다.
- **예시:** row 1 -> accepted / row 2 -> …

```text
row 1 -> accepted
row 2 -> invalid_email
row 3 -> accepted
row 4 -> duplicate
```


 Code와 human message를 분리한다.

---

**검증 시나리오 P103-C5 — CHAPTER 05 · partial result는 성공 count 하나보다 item별 outcome을 구조화한다**
`CHAPTER 05 · partial result는 성공 count 하나보다 item별 outcome을 구조화한다` 검증은 호출 순서를 단순화하는 데서 시작한다. P103-C5에서는 `CHAPTER 05 · partial result는 성공 count 하나보다 item별 outcome을 구조화한다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 05 · partial result는 성공 count 하나보다 item별 outcome을 구조화한다`에 대해 순서 하나만 뒤집어 차이를 본다고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 05 · partial result는 성공 count 하나보다 item별 outcome을 구조화한다`의 호출 전후의 상태 전이를 번호로 남긴다하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 05 · partial result는 성공 count 하나보다 item별 outcome을 구조화한다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P103-C5의 마무리는 다른 순서에서도 계약이 유지되는지 확인하는 것이다. 통과 기준은 `CHAPTER 05 · partial result는 성공 count 하나보다 item별 outcome을 구조화한다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 06 · poison record는 무한 retry를 막기 위해 격리한다

### 시작 전 용어집

#### 1. poison record

- **뜻:** 항상 같은 parser crash나 permanent constraint failure를 만드는 item이 queue head에 있으면 worker가 반복 실패하며 전체 progress를 막을 수 있다.
- **왜 중요한가:** Retry 횟수나 failure classification을 기준으로 dead-letter/quarantine 영역으로 옮기고 나머지 item을 계속 처리할 수 있다.
- **예시:** 항상 같은 parser crash나 permanent constraint failure를 만드는 …

#### 2. retry

- **뜻:** 하지만 자동 격리가 business data loss로 이어지지 않도록 alert와 manual review path를 둔다.
- **왜 중요한가:** Programming bug 때문에 정상 record가 모두 poison처럼 보일 수 있으므로 갑작스러운 quarantine 급증을 모니터링한다.
- **예시:** 하지만 자동 격리가 business data loss로 이어지지 않도록 …

---

**검증 시나리오 P103-C6 — CHAPTER 06 · poison record는 무한 retry를 막기 위해 격리한다**
`CHAPTER 06 · poison record는 무한 retry를 막기 위해 격리한다` 검증은 입력 크기를 고정하는 데서 시작한다. P103-C6에서는 `CHAPTER 06 · poison record는 무한 retry를 막기 위해 격리한다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 06 · poison record는 무한 retry를 막기 위해 격리한다`에 대해 변형은 한 요소만 허용고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 06 · poison record는 무한 retry를 막기 위해 격리한다`의 측정값을 여러 번 모아 분포를 비교하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 06 · poison record는 무한 retry를 막기 위해 격리한다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P103-C6의 마무리는 워밍업과 측정 자체의 비용을 분리하는 것이다. 통과 기준은 `CHAPTER 06 · poison record는 무한 retry를 막기 위해 격리한다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 07 · replay는 이미 성공한 effect를 중복시키지 않는 경계가 필요하다

### 시작 전 용어집

#### 1. replay

- **뜻:** Stable item ID와 upsert/dedup rule을 사용하면 replay-safe하게 만들 수 있다.
- **왜 중요한가:** 반대로 increment, email send, external charge 같은 non-idempotent effect는 별도 operation ID와 result log가 필요하다.
- **예시:** Stable item ID와 upsert/dedup rule을 사용하면 replay-safe하게 만들 …

#### 2. effect

- **뜻:** Batch job을 처음부터 재실행하는 것이 가장 단순한 복구 방법일 수 있지만 output operation이 idempotent해야 한다.
- **왜 중요한가:** P102의 delivery semantics와 연결된다.
- **예시:** Batch job을 처음부터 재실행하는 것이 가장 단순한 복구 …

#### 3. 경계

- **뜻:** Replay mode에서 현재 schema/version이 과거와 달라졌다면 같은 input이 다른 output을 만들 수 있다.
- **왜 중요한가:** Processing version도 checkpoint와 함께 기록한다.
- **예시:** Replay mode에서 현재 schema/version이 과거와 달라졌다면 같은 input이 …

---

## CHAPTER 08 · batch contract는 atomicity·partial acceptance·replay 단위를 명시한다

### 시작 전 용어집

#### 1. batch

- **뜻:** Batch API를 설계할 때는 전체 성공이 필요한지, invalid item만 제외할 수 있는지, commit chunk 크기와 checkpoint가 무엇인지, poison record와 replay를 어떻게 다룰지 정한다.
- **왜 중요한가:** 테스트에서는 첫 item 실패, 중간 item 실패, commit 직후 crash, chunk 경계 duplicate, permanent poison item, result reporting 누락을 각각 검증한다.
- **예시:** Batch API를 설계할 때는 전체 성공이 필요한지, invalid …

#### 2. atomicity

- **뜻:** 이 PART의 핵심은 **batch를 단순 반복문 최적화로 보지 않고, 어떤 단위로 검증·commit·실패·재시작할지를 결정하는 독립된 consistency protocol로 설계하는 것**이다.
- **예시:** 이 PART의 핵심은 **batch를 단순 반복문 최적화로 보지 …

---

## 실전 학습 루프 · batch partial failure

### 1. 쉬운 예

100개 항목을 묶어 처리할 때 57번째가 실패했다고 전체 성공 또는 전체 실패 두 값만 반환하면 이미 처리된 56개의 상태가 불명확해질 수 있다. batch는 항목별 결과와 commit 단위를 명시해야 한다.

### 2. 한 줄 해석

batch API는 입력 묶음보다 실패 단위·재시도 단위·원자성 범위를 먼저 정해야 한다.

### 3. 직접 실행

아래 최소 예제를 실행하기 전에 **성공 경로와 실패 경로를 각각 한 줄로 예측**한다.

```python
def process_batch(items):
    results = []
    for item in items:
        try:
            results.append(('ok', item * 2))
        except Exception as exc:
            results.append(('error', str(exc)))
    return results
```

### 4. 수정 실습

1. all-or-nothing transaction과 per-item commit의 복구 전략을 비교한다.
2. 재시도할 항목만 추출할 수 있도록 stable item id를 결과에 포함한다.

수정 뒤에는 같은 입력을 여러 번 실행하거나 중간 crash를 가정해 결과가 중복·누락·무한 대기로 바뀌지 않는지 확인한다.

### 5. 확인 문제

batch 안에서 하나가 실패하면 나머지도 무조건 같은 상태로 처리해야 할까?

### 6. 정답과 오답 설명

**정답:** 요구사항에 따라 다르다. 원자적 batch인지 부분 성공 허용인지 계약으로 먼저 정해야 한다.

**자주 나오는 오답:** HTTP status 하나만으로 각 item 결과를 표현하려는 설계는 복구 정보를 잃기 쉽다.

운영형 문제에서는 함수 한 번의 정상 출력보다 **재시도, 중복, timeout, crash, 재시작** 뒤의 상태가 더 중요하다. 마지막으로 이 기능이 어떤 상태를 영구 저장하고 어떤 상태를 다시 계산할 수 있는지 구분해 적는다.
## 현장 디버깅 체크 · batch partial failure

### 증상에서 시작한다

대량 batch 1건은 성공으로 끝났는데 실제로는 일부 항목만 저장됐거나, 재시도 후 이미 성공한 항목이 다시 적용된다. 이런 문제는 batch 성공/실패 두 상태만 기록해서는 원인을 찾기 어렵다.

### 먼저 볼 증거

각 item에 stable id를 붙이고 batch_id, item_id, attempt, validation_result, side_effect_result, commit_result, retryable, error_type을 남긴다. batch 전체 status는 이 item 결과의 요약이어야 하며 원본 item 결과를 대신해서는 안 된다.

### 일부러 실패시켜 보기

100개 항목 중 1, 50, 100번째에서 각각 validation error와 transient DB error를 강제로 만든다. batch 전체를 다시 보내는 방식과 실패 item만 재전송하는 방식을 비교하고 이미 성공한 item에 부작용이 두 번 적용되지 않는지 확인한다.

### 통과 기준

부분 성공을 허용한다면 성공한 항목은 보존되고 실패 항목만 정확히 식별·재시도할 수 있어야 한다. all-or-nothing이 요구사항이면 commit 경계가 batch 전체를 포함해야 한다. 두 모델을 섞으면 복구 규칙이 모호해진다.

### 설계 문제

A1 성공, A2 validation error, A3 timeout 후 결과 불명, A4 성공, A5 downstream 503 상황에서 단일 boolean 대신 item별 status, retryable, operation_id, error_code를 포함한다. A3처럼 결과가 불명한 상태를 단순 실패와 구분해야 client가 안전한 다음 행동을 선택할 수 있다.

