# PART 104 · Checkpoint와 resume — progress를 durable state로 만들고 crash 뒤 안전하게 이어가기

긴 batch나 stream job이 중간에 죽을 때 처음부터 다시 시작하면 비용이 크다. Checkpoint는 “어디까지 처리했는가”를 durable하게 기록해 resume을 가능하게 한다. 하지만 output보다 checkpoint를 먼저 저장하면 처리되지 않은 item을 건너뛸 수 있고, output 뒤 checkpoint 전에 crash하면 같은 item을 다시 처리할 수 있다. 따라서 checkpoint는 **progress representation과 output side effect의 관계**로 설계해야 한다.

---

## CHAPTER 01 · progress model은 처리 순서와 재시작 가능성을 표현해야 한다

### 시작 전 용어집

#### 1. progress model

- **뜻:** Progress가 단순 row number인지, immutable event offset인지, stable item ID 집합인지에 따라 checkpoint semantics가 달라진다.
- **왜 중요한가:** Sequential immutable input에서는 `next_offset=1200` 같은 값이 충분할 수 있다.
- **예시:** Progress가 단순 row number인지, immutable event offset인지, stable …

#### 2. offset

- **뜻:** Input file이 수정되거나 row order가 바뀔 수 있다면 같은 offset이 다른 record를 가리킬 수 있다.
- **왜 중요한가:** Checkpoint를 만들기 전에 input identity와 ordering stability를 정의한다.
- **예시:** Input file이 수정되거나 row order가 바뀔 수 있다면 …

#### 3. checkpoint

- **뜻:** “100번째까지 완료”라는 문장은 input version이 없으면 불완전하다.
- **예시:** “100번째까지 완료”라는 문장은 input version이 없으면 불완전하다.

---

## CHAPTER 02 · offset과 item ID는 서로 다른 recovery 특성을 가진다

### 시작 전 용어집

#### 1. offset

- **뜻:** Offset은 작고 빠르게 저장할 수 있지만 input order가 고정되어야 한다.
- **왜 중요한가:** Stable item ID는 순서가 바뀌어도 dedup에 사용할 수 있지만 processed-ID set이 커질 수 있다.
- **예시:** offset checkpoint: source=v3, next=10001 / id checkpoint: generation=v3, …

#### 2. item

- **뜻:** Partitioned stream에서는 partition별 offset이 필요할 수 있고, batch file에서는 byte offset보다 logical record number가 안전할 수 있다.
- **왜 중요한가:** Text encoding과 quoted CSV 때문에 arbitrary byte offset에서 parser를 재시작하기 어려울 수 있기 때문이다.
- **예시:** offset checkpoint: source=v3, next=10001 / id checkpoint: generation=v3, …

#### 3. recovery

- **뜻:** Resume unit을 parser framing과 맞춘다.
- **예시:** offset checkpoint: source=v3, next=10001 / id checkpoint: generation=v3, …

```text
offset checkpoint: source=v3, next=10001
id checkpoint: generation=v3, processed_until_key=...
```

 


---

## CHAPTER 03 · output commit과 checkpoint update 사이의 atomicity gap을 줄여야 한다

### 시작 전 용어집

#### 1. output commit

- **뜻:** 가장 위험한 순서는 checkpoint를 먼저 저장하는 것이다.
- **왜 중요한가:** 반대로 output을 먼저 commit하면 checkpoint 전 crash에서 item 42가 replay될 수 있다.
- **예시:** checkpoint says item 42 done -> crash -> …

#### 2. checkpoint

- **뜻:** 같은 database에 output과 checkpoint를 저장할 수 있다면 하나의 transaction으로 묶을 수 있다.
- **왜 중요한가:** 서로 다른 systems라면 idempotency/dedup을 사용해 duplicate replay를 허용하면서 손실을 막는다.
- **예시:** checkpoint says item 42 done -> crash -> …

#### 3. atomicity gap

- **뜻:** Replay-safe output이라면 후자가 일반적으로 더 안전하다.
- **예시:** checkpoint says item 42 done -> crash -> …

```text
checkpoint says item 42 done -> crash -> item 42 output never committed
```

 

 

---

**현장 디버깅 점검 — CHAPTER 03 · output commit과 checkpoint update 사이의 atomicity gap을 줄여야 한다**
CHAPTER 03 · output commit과 checkpoint update 사이의 atomicity gap을 줄여야 한다을 운영에서 검증할 때는 재시작 뒤에도 상태가 이어지는지까지 확인해야 한다. 먼저 처리 전 상태와 완료 조건을 기록하고, 정상 입력으로 기준 결과를 만든다. 그다음 처리 중간에 강제 중단, 일부 레코드 실패, 중복 전달, 재시작을 각각 따로 주입한다. 재실행 후에는 완료된 항목이 다시 처리되지 않는지, 실패 항목만 안전하게 재시도되는지, 체크포인트와 실제 데이터 상태가 일치하는지 비교한다. 통과 기준은 모든 성공 항목이 정확히 한 번 반영되고 실패 항목은 추적 가능한 상태로 남으며, 같은 시나리오를 반복해도 결과가 변하지 않는 것이다.
## CHAPTER 04 · replay window는 마지막 checkpoint 이후 얼마만큼 다시 처리할지 결정한다

### 시작 전 용어집

#### 1. replay window

- **뜻:** 매 item마다 checkpoint를 sync하면 recovery work는 작지만 write overhead가 크다.
- **왜 중요한가:** 1000 item마다 checkpoint하면 정상 처리 throughput은 높아지지만 crash 후 최대 999개를 다시 처리할 수 있다.
- **예시:** 매 item마다 checkpoint를 sync하면 recovery work는 작지만 write …

#### 2. checkpoint

- **뜻:** Checkpoint interval은 side effect 비용, idempotency, job length를 기준으로 정한다.
- **왜 중요한가:** Expensive non-idempotent effect가 있다면 더 촘촘한 durable operation log가 필요할 수 있다.
- **예시:** Checkpoint interval은 side effect 비용, idempotency, job length를 …

#### 3. idempotency

- **뜻:** Time-based checkpoint와 count-based checkpoint를 조합할 수도 있다.
- **왜 중요한가:** 중요한 것은 worst-case replay 범위를 계산할 수 있는 것이다.
- **예시:** Time-based checkpoint와 count-based checkpoint를 조합할 수도 있다.

---

## CHAPTER 05 · checkpoint에는 schema와 processing version을 포함해야 한다

### 시작 전 용어집

#### 1. checkpoint

- **뜻:** Application을 업데이트한 뒤 old checkpoint에서 resume할 때 data model이나 transform logic이 달라졌을 수 있다.
- **왜 중요한가:** Version 없이 progress 숫자만 읽으면 old state를 new code가 잘못 해석할 수 있다.
- **예시:** job_id, input_digest, schema_version, processor_version, progress

#### 2. schema

- **뜻:** Version mismatch 시 migration할지 처음부터 재처리할지 명시한다.
- **왜 중요한가:** Output이 versioned라면 두 processing version의 결과가 섞이지 않게 generation을 분리한다.
- **예시:** job_id, input_digest, schema_version, processor_version, progress

#### 3. processing version

- **뜻:** Checkpoint format 자체도 atomic update와 backward compatibility가 필요하다.
- **예시:** job_id, input_digest, schema_version, processor_version, progress

```text
job_id, input_digest, schema_version, processor_version, progress
```

 


---

## CHAPTER 06 · 하나의 partition progress에는 single owner 또는 coordination이 필요하다

### 시작 전 용어집

#### 1. partition progress

- **뜻:** 두 worker가 같은 checkpoint를 동시에 갱신하면 더 느린 worker가 오래된 progress로 덮어쓰거나 서로 같은 item을 처리할 수 있다.
- **왜 중요한가:** Lease, partition ownership, compare-and-swap generation을 사용해 현재 owner를 명확히 한다.
- **예시:** 두 worker가 같은 checkpoint를 동시에 갱신하면 더 느린 …

#### 2. single owner

- **뜻:** Lease가 만료돼 새 worker가 takeover할 때 old worker가 늦게 commit하는 fencing 문제도 고려한다.
- **왜 중요한가:** Simple local batch라면 single process owner로 문제를 제거하는 것이 가장 쉽다.
- **예시:** Lease가 만료돼 새 worker가 takeover할 때 old worker가 …

#### 3. coordination

- **뜻:** Distributed coordination은 필요할 때만 추가한다.
- **예시:** Distributed coordination은 필요할 때만 추가한다.

---

**현장 디버깅 점검 — CHAPTER 06 · 하나의 partition progress에는 single owner 또는 coordination이 필요하다**
CHAPTER 06 · 하나의 partition progress에는 single owner 또는 coordination이 필요하다을 운영에서 검증할 때는 재시작 뒤에도 상태가 이어지는지까지 확인해야 한다. 먼저 처리 전 상태와 완료 조건을 기록하고, 정상 입력으로 기준 결과를 만든다. 그다음 처리 중간에 강제 중단, 일부 레코드 실패, 중복 전달, 재시작을 각각 따로 주입한다. 재실행 후에는 완료된 항목이 다시 처리되지 않는지, 실패 항목만 안전하게 재시도되는지, 체크포인트와 실제 데이터 상태가 일치하는지 비교한다. 통과 기준은 모든 성공 항목이 정확히 한 번 반영되고 실패 항목은 추적 가능한 상태로 남으며, 같은 시나리오를 반복해도 결과가 변하지 않는 것이다.
## CHAPTER 07 · corrupt checkpoint는 입력 손실로 이어지지 않게 검증 후 fallback한다

### 시작 전 용어집

#### 1. corrupt checkpoint

- **뜻:** Corrupt checkpoint를 0으로 간주해 전체 replay하는 것이 안전한지, 마지막 backup generation을 사용할지 workload에 따라 정한다.
- **왜 중요한가:** Non-idempotent output에서는 무조건 처음부터 replay하는 것도 위험할 수 있다.
- **예시:** Corrupt checkpoint를 0으로 간주해 전체 replay하는 것이 안전한지, …

#### 2. 검증

- **뜻:** Recovery 전에 input digest와 output state를 함께 검증한다.
- **왜 중요한가:** Checkpoint 하나만 truth source로 과신하지 않는다.
- **예시:** Recovery 전에 input digest와 output state를 함께 검증한다.

#### 3. fallback

- **뜻:** Checkpoint file이 partial write나 disk corruption으로 읽히지 않을 수 있다.
- **왜 중요한가:** P91의 atomic file update를 적용하고 checksum/version을 포함하면 detection과 recovery가 쉬워진다.
- **예시:** Checkpoint file이 partial write나 disk corruption으로 읽히지 않을 …

---

## CHAPTER 08 · checkpoint contract는 crash 위치마다 loss와 replay 범위를 설명한다

### 시작 전 용어집

#### 1. checkpoint

- **뜻:** 안정적인 resume system은 output 전 crash, output 후 checkpoint 전 crash, checkpoint write 중 crash, version update 뒤 restart에서 어떻게 동작하는지 설명할 수 있어야 한다.
- **왜 중요한가:** 테스트에서는 checkpoint 직전/직후 강제 종료, stale owner, corrupt state, input version 변경, replay duplicate를 주입한다.
- **예시:** 안정적인 resume system은 output 전 crash, output 후 …

#### 2. crash

- **뜻:** 정상 resume 한 번 성공은 충분한 검증이 아니다.
- **왜 중요한가:** 이 PART의 핵심은 **checkpoint를 진행률 숫자 저장으로 보지 않고, output commit과의 순서를 통해 손실을 막고 허용 가능한 replay 범위를 정의하는 durable recovery protocol로 설계하는 것**이다.
- **예시:** 정상 resume 한 번 성공은 충분한 검증이 아니다.

---

## 실전 학습 루프 · checkpoint와 resume

### 1. 쉬운 예

백만 행을 처리하다 90만 행에서 process가 죽었는데 처음부터 다시 시작할 필요는 없다. 다만 checkpoint를 “90만”이라는 숫자 하나로 저장하면 입력 파일이 바뀌었거나 직전 side effect가 commit되지 않은 경우 잘못 재개할 수 있다.

### 2. 한 줄 해석

checkpoint는 위치뿐 아니라 입력 identity·처리 버전·commit 경계를 함께 묶어야 안전한 resume 지점이 된다.

### 3. 직접 실행

아래 최소 예제를 실행하기 전에 **성공 경로와 실패 경로를 각각 한 줄로 예측**한다.

```python
checkpoint = {
    'source_digest': 'abc123',
    'offset': 900000,
    'version': 2,
}
print(checkpoint)
```

### 4. 수정 실습

1. checkpoint 저장 직전/직후 crash를 나눠 마지막 항목이 중복·누락되는지 분석한다.
2. 입력 파일 digest가 달라지면 기존 checkpoint를 거부하도록 만든다.

수정 뒤에는 같은 입력을 여러 번 실행하거나 중간 crash를 가정해 결과가 중복·누락·무한 대기로 바뀌지 않는지 확인한다.

### 5. 확인 문제

마지막으로 읽은 row 번호만 저장하면 정확히 이어서 처리할 수 있을까?

### 6. 정답과 오답 설명

**정답:** 항상 아니다. side effect commit과 checkpoint 저장 순서, 입력 동일성까지 일치해야 한다.

**자주 나오는 오답:** progress 표시 숫자와 복구 가능한 durable checkpoint를 같은 것으로 보면 안 된다.

운영형 문제에서는 함수 한 번의 정상 출력보다 **재시도, 중복, timeout, crash, 재시작** 뒤의 상태가 더 중요하다. 마지막으로 이 기능이 어떤 상태를 영구 저장하고 어떤 상태를 다시 계산할 수 있는지 구분해 적는다.
## 현장 디버깅 체크 · checkpoint와 resume

### 증상에서 시작한다

재시작 후 데이터 한 건이 빠지거나 두 번 처리된다면 checkpoint 숫자 자체보다 side effect와 checkpoint가 어떤 순서로 durable해졌는지 먼저 본다. offset=500이라고 저장돼 있어도 500번째 결과가 실제 DB에 commit됐다는 보장은 별개의 문제다.

### 먼저 볼 증거

checkpoint에는 source identity 또는 digest, schema나 processor version, cursor 또는 offset, last committed item identity, checkpoint generation, written_at을 함께 둔다. 입력 파일이 바뀌었는데 오래된 offset만 재사용하는 상황을 막으려면 source identity가 특히 중요하다.

### 일부러 실패시켜 보기

한 item 처리에 input read, side effect write, side effect commit, checkpoint persist 네 지점을 두고 각 지점 직후 process kill을 넣어 재시작 결과를 본다. commit과 checkpoint 사이에서 죽으면 같은 item을 다시 읽을 수 있으므로 side effect가 idempotent하거나 두 기록을 같은 transaction 경계에 둬야 한다.

### 통과 기준

resume 후 누락 0, 허용되지 않은 중복 효과 0이어야 한다. 단순히 처리 개수만 보면 같은 item이 두 번 적용되고 다른 item이 빠져도 총개수는 같을 수 있다.

### 설계 문제

processor version이 바뀐 새 배포에서 과거 checkpoint를 사용할지 결정한다. 변환 규칙이 달라졌다면 checkpoint invalidation 또는 migration이 필요하다. offset만 맞으니 이어 간다는 방식은 같은 입력을 서로 다른 의미로 처리하게 만들 수 있다.

