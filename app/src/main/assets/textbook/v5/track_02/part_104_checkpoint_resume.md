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
