# PART 50 · Durable task runner capstone — queue·lease·retry·checkpoint를 하나의 실행기로 연결하기

백그라운드 작업 실행기를 만들면 지금까지 배운 많은 계약이 한곳에서 만난다. 작업은 저장되어야 하고, 여러 worker가 같은 job을 중복 실행하지 않아야 하며, worker가 죽으면 다른 worker가 이어받아야 하고, transient failure는 재시도하되 permanent failure는 끝내야 한다. 여기에 timeout, cancellation, shutdown, observability까지 들어간다. 목표는 단순 worker loop가 아니라 **process crash와 duplicate delivery를 전제로 해도 job lifecycle이 일관되게 남는 작은 durable system**을 설계하는 것이다.

---

## CHAPTER 01 · runner의 public contract를 먼저 고정한다

### 시작 전 용어집

#### 1. runner

- **뜻:** Task runner에 `submit(payload)`와 `status(job_id)`가 있다고 하자.
- **왜 중요한가:** Caller가 알아야 할 것은 함수 이름보다 더 많다.
- **예시:** Task runner에 `submit(payload)`와 `status(job_id)`가 있다고 하자.

#### 2. public contract

- **뜻:** Submit 성공은 “작업이 완료됨”이 아니라 durable queue에 수락되었다는 뜻인지, 같은 idempotency key를 두 번 보내면 기존 job을 돌려주는지, 언제 timeout 상태가 되는지, 결과 retention은 얼마나 되는지를 정해야 한다.
- **왜 중요한가:** 입력 schema와 최대 실행시간, side effect, retry 가능한 exception, cancellation 지원 여부를 등록한다.
- **예시:** Submit 성공은 “작업이 완료됨”이 아니라 durable queue에 수락되었다는 …

#### 3. 함수

- **뜻:** 모든 handler를 `callable` 하나로만 취급하면 runner가 failure policy를 결정할 정보가 부족하다.
- **왜 중요한가:** Handler metadata를 명시적인 definition object로 만들 수 있다.
- **예시:** 모든 handler를 `callable` 하나로만 취급하면 runner가 failure policy를 …

#### 4. queue

- **뜻:** Runner 자체의 guarantee도 과장하지 않는다.
- **왜 중요한가:** Exactly-once execution을 약속하기 어렵다면 at-least-once delivery와 idempotent handler requirement를 명시한다.
- **예시:** Runner 자체의 guarantee도 과장하지 않는다.

Job handler도 contract를 가진다.   

  사용자가 기대해야 할 semantics를 먼저 고정하면 storage와 worker 구현을 그 계약에 맞춰 선택할 수 있다.

---

## CHAPTER 02 · job lifecycle을 상태 머신으로 만든다

### 시작 전 용어집

#### 1. job lifecycle

- **뜻:** Job을 `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`, `RETRY_WAIT`, `CANCELLED` 같은 상태로 모델링하면 허용 전이를 명시할 수 있다.
- **왜 중요한가:** `SUCCEEDED → RUNNING` 같은 전이는 금지하고 `RUNNING → RETRY_WAIT`은 transient failure에서만 허용한다.
- **예시:** Job을 `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`, `RETRY_WAIT`, `CANCELLED` 같은 …

#### 2. 상태

- **뜻:** 상태 문자열을 자유롭게 update하지 않고 transition 함수가 invariant를 소유하게 한다.
- **왜 중요한가:** RUNNING state에는 worker ID, attempt number, lease expiry, started_at이 필요할 수 있다.
- **예시:** 상태 문자열을 자유롭게 update하지 않고 transition 함수가 invariant를 …

#### 3. 함수

- **뜻:** SUCCEEDED에는 result reference와 finished_at이 있고 FAILED에는 terminal error category와 diagnostic ID가 있다.
- **왜 중요한가:** 모든 field를 optional column로 두더라도 status와 field 관계를 object/database constraint로 검증한다.
- **예시:** SUCCEEDED에는 result reference와 finished_at이 있고 FAILED에는 terminal error …

#### 4. 검증

- **뜻:** Cancellation request도 단순 boolean flag보다 상태 전이로 본다.
- **왜 중요한가:** PENDING은 즉시 CANCELLED로 바꿀 수 있지만 RUNNING은 worker에게 cancellation intent를 전달한 뒤 실제 cleanup 완료 시 terminal 상태로 이동할 수 있다.
- **예시:** Cancellation request도 단순 boolean flag보다 상태 전이로 본다.

“취소 버튼을 눌렀다”와 “작업이 멈췄다”는 다른 사건이다.

---

## CHAPTER 03 · durable queue schema는 작업과 attempt history를 분리할 수 있다

### 시작 전 용어집

#### 1. durable queue

- **뜻:** 임의 Python object pickle을 durable queue에 넣으면 code version과 security boundary가 강하게 결합될 수 있다.
- **왜 중요한가:** Public DTO로 serialize하고 worker가 domain input으로 검증해 복원한다.
- **예시:** `status=PENDING AND not_before<=now ORDER BY priority, created_at` 같은 …

#### 2. schema

- **뜻:** Payload를 JSON으로 저장한다면 schema version과 maximum size를 둔다.
- **왜 중요한가:** Index는 dequeue query에 맞춰 설계한다.
- **예시:** `status=PENDING AND not_before<=now ORDER BY priority, created_at` 같은 …

#### 3. attempt history

- **뜻:** Job row 하나에 모든 retry 정보를 덮어쓰면 이전 attempt가 왜 실패했는지 잃을 수 있다.
- **왜 중요한가:** `jobs` table은 logical job의 현재 상태와 identity를 저장하고 `attempts` table은 각 실행 시작/종료, worker, error, duration을 append-only로 남기는 구조를 사용할 수 있다.
- **예시:** `status=PENDING AND not_before<=now ORDER BY priority, created_at` 같은 …

#### 4. 실패

- **뜻:** 이 separation은 operational history와 current state를 동시에 제공한다.
- **왜 중요한가:** `status=PENDING AND not_before<=now ORDER BY priority, created_at` 같은 query가 핵심이면 해당 filter/order를 현실적인 row 수에서 profile한다.
- **예시:** 이 separation은 operational history와 current state를 동시에 제공한다.

Queue table이 커지면 terminal job archival과 retention도 필요하다.

---

## CHAPTER 04 · claim과 lease는 worker crash 뒤 ownership을 회수하게 한다

### 시작 전 용어집

#### 1. claim

- **뜻:** Claim operation은 하나의 transaction/atomic update로 특정 worker가 job을 RUNNING 상태로 소유하게 해야 한다.
- **왜 중요한가:** WHERE status='PENDING'`처럼 이전 상태를 조건에 포함해 한 worker만 성공하도록 만들 수 있다.
- **예시:** Claim operation은 하나의 transaction/atomic update로 특정 worker가 job을 …

#### 2. lease

- **뜻:** Lease expiry를 저장하고 worker가 주기적으로 heartbeat를 갱신하게 하면 일정 시간 이상 갱신되지 않은 job을 다른 worker가 reclaim할 수 있다.
- **왜 중요한가:** Lease duration은 정상 heartbeat jitter보다 충분히 길어야 하지만 crash recovery를 지나치게 늦추지 않아야 한다.
- **예시:** Lease expiry를 저장하고 worker가 주기적으로 heartbeat를 갱신하게 하면 …

#### 3. worker crash

- **뜻:** 여러 worker가 같은 pending job을 조회한 뒤 모두 실행하면 duplicate work가 생긴다.
- **왜 중요한가:** 하지만 RUNNING job을 worker가 영원히 소유하면 process crash 후 작업이 stuck된다.
- **예시:** 여러 worker가 같은 pending job을 조회한 뒤 모두 …

#### 4. ownership

- **뜻:** Lease는 시간만이 아니라 ownership version contract다.
- **왜 중요한가:** Old worker가 잠시 멈췄다가 lease를 잃은 뒤 다시 살아나 결과를 commit하면 split-brain execution이 될 수 있다.
- **예시:** Lease는 시간만이 아니라 ownership version contract다.

`UPDATE ... 

  

 Fencing token이나 attempt/version 번호를 update 조건에 포함해 현재 lease owner만 final state를 기록하게 한다. 

---

## CHAPTER 05 · attempt마다 timeout과 cancellation budget을 별도로 관리한다

### 시작 전 용어집

#### 1. attempt

- **뜻:** Job 전체 deadline과 개별 attempt timeout은 다를 수 있다.
- **왜 중요한가:** 10분 안에 완료해야 하는 job이 최대 3번 retry할 수 있다면 한 attempt가 10분을 모두 써버리지 않도록 budget을 나눈다.
- **예시:** Job 전체 deadline과 개별 attempt timeout은 다를 수 …

#### 2. timeout

- **뜻:** Remaining deadline을 계산해 downstream API timeout에도 전달한다.
- **왜 중요한가:** Timeout이 발생했다고 handler의 외부 side effect가 취소되었다는 보장은 없다.
- **예시:** Remaining deadline을 계산해 downstream API timeout에도 전달한다.

#### 3. cancellation

- **뜻:** Local task cancellation만으로 distributed effect를 되돌릴 수 없다.
- **왜 중요한가:** Cancellation-safe cleanup을 위해 handler는 context manager와 structured concurrency를 사용할 수 있다.
- **예시:** Local task cancellation만으로 distributed effect를 되돌릴 수 없다.

#### 4. stream

- **뜻:** Remote request가 실제로 성공했지만 response만 늦었을 수 있으므로 handler operation에 idempotency key를 전달하고 상태 조회가 가능하게 한다.
- **왜 중요한가:** Child task와 open resource를 정리한 뒤 cancellation을 상위로 전파하고 runner는 attempt result를 기록한다.
- **예시:** Remote request가 실제로 성공했지만 response만 늦었을 수 있으므로 …

Timeout, explicit user cancellation, process shutdown을 서로 다른 reason으로 남기면 운영 분석이 쉬워진다.

---

## CHAPTER 06 · idempotency는 duplicate execution을 correctness 문제에서 운영 가능한 상태로 바꾼다

### 시작 전 용어집

#### 1. idempotency

- **뜻:** Logical job ID 또는 business idempotency key를 downstream operation에도 전달해 동일 effect를 재사용하게 한다.
- **왜 중요한가:** Database write는 unique key와 conditional insert/update로 deduplicate할 수 있다.
- **예시:** Logical job ID 또는 business idempotency key를 downstream …

#### 2. duplicate execution

- **뜻:** Lease expiry 직전 네트워크가 끊기거나 acknowledgement 저장 전에 process가 죽으면 같은 logical job이 두 번 실행될 수 있다.
- **왜 중요한가:** Handler가 `charge_card()`처럼 irreversible effect를 수행한다면 runner infrastructure만으로 중복을 완전히 막기 어렵다.
- **예시:** Lease expiry 직전 네트워크가 끊기거나 acknowledgement 저장 전에 …

#### 3. correctness

- **뜻:** File generation은 deterministic output path와 atomic replace를 사용할 수 있다.
- **왜 중요한가:** Email처럼 완전한 dedup이 어려운 effect는 send record와 provider message ID를 보존해 중복 가능성을 제한한다.
- **예시:** File generation은 deterministic output path와 atomic replace를 사용할 …

#### 4. 상태

- **뜻:** 동일 key가 30일 후 다시 제출되면 같은 결과를 돌려줘야 하는지, retention 이후 새 job으로 볼지 business policy가 필요하다.
- **왜 중요한가:** Key store lifetime이 실제 duplicate window보다 짧으면 contract가 깨진다.
- **예시:** 동일 key가 30일 후 다시 제출되면 같은 결과를 …

Idempotency의 범위도 정한다.  

---

## CHAPTER 07 · 긴 작업은 checkpoint를 저장해 전체 재실행 비용을 줄인다

### 시작 전 용어집

#### 1. checkpoint

- **뜻:** 안전한 중간 지점마다 checkpoint를 durable storage에 기록하면 새로운 attempt가 마지막 완료 지점부터 이어갈 수 있다.
- **왜 중요한가:** Checkpoint에는 input version과 algorithm version도 포함해 이전 code가 만든 state를 새 code가 잘못 해석하지 않게 한다.
- **예시:** 안전한 중간 지점마다 checkpoint를 durable storage에 기록하면 새로운 …

백만 row를 처리하는 job이 90% 진행 후 worker crash로 처음부터 다시 시작되면 비용이 크다.  

Checkpoint write와 side effect 순서가 중요하다. 외부 effect를 수행한 뒤 checkpoint 저장 전에 crash하면 같은 chunk가 재실행될 수 있으므로 chunk operation도 idempotent해야 한다. 반대로 checkpoint를 먼저 올리면 실제 처리하지 않은 item을 건너뛸 수 있다.

Checkpoint granularity는 overhead와 replay cost의 trade-off다. 매 item 저장하면 DB load가 커지고 10만 item마다 저장하면 crash 시 많은 재처리가 생긴다. Workload와 failure 비용을 측정해 선택한다.

---

## CHAPTER 08 · graceful shutdown은 새 claim 중단과 현재 lease 정리를 분리한다

### 시작 전 용어집

#### 1. graceful shutdown

- **뜻:** Worker process가 종료 signal을 받으면 먼저 새 job claim을 중단해야 한다.
- **왜 중요한가:** 이미 실행 중인 job은 grace period 안에 완료하도록 기다리거나 cancellation을 전달하고 checkpoint/cleanup을 수행한다.
- **예시:** Worker process가 종료 signal을 받으면 먼저 새 job …

#### 2. claim

- **뜻:** Shutdown deadline을 넘긴 attempt는 강제 종료될 수 있으므로 runner는 lease를 짧게 release하거나 worker death를 감지해 reclaim할 수 있어야 한다.
- **왜 중요한가:** Current attempt가 final state를 commit하지 못했다면 다음 worker가 안전하게 재실행할 수 있는 idempotency가 필요하다.
- **예시:** Shutdown deadline을 넘긴 attempt는 강제 종료될 수 있으므로 …

#### 3. lease

- **뜻:** 즉시 process를 죽이면 lease expiry까지 다른 worker가 job을 가져갈 수 없어 recovery가 늦어진다.
- **왜 중요한가:** Deployment 때 모든 worker를 동시에 내리면 queue 처리 capacity가 0이 되는 구간이 생길 수 있다.
- **예시:** 즉시 process를 죽이면 lease expiry까지 다른 worker가 job을 …

#### 4. process

- **뜻:** Process lifecycle과 scheduler capacity가 연결되는 지점이다.
- **왜 중요한가:** Rolling shutdown과 readiness를 연결해 새 worker가 준비된 뒤 old worker를 drain한다.
- **예시:** Process lifecycle과 scheduler capacity가 연결되는 지점이다.

---

## CHAPTER 09 · CLEAN PASS는 서로 다른 failure class를 실제로 주입해 검증한다

### 시작 전 용어집

#### 1. CLEAN PASS

- **뜻:** Durable runner는 happy path test 하나로 검증할 수 없다.
- **왜 중요한가:** 첫 번째 pass에서는 state machine과 정상 claim/complete를 확인한다.
- **예시:** Durable runner는 happy path test 하나로 검증할 수 …

#### 2. failure class

- **뜻:** 서로 다른 failure class를 실행하지 않았다면 모두 PASS라고 보고하지 않는다.
- **왜 중요한가:** Attempt가 두 번 실행되었는데 error counter가 하나만 증가하거나 job age가 음수가 되면 운영 중 원인을 잘못 해석할 수 있다.
- **예시:** 서로 다른 failure class를 실행하지 않았다면 모두 PASS라고 …

#### 3. 검증

- **뜻:** 추가로 timeout 중 remote side effect가 이미 성공한 경우 idempotency가 중복을 막는지, checkpoint 직후 crash에서 resume 위치가 맞는지, shutdown signal에서 새 claim이 중단되고 existing job이 정리되는지 검증한다.
- **왜 중요한가:** Correctness와 observability를 함께 검증한다.
- **예시:** 추가로 timeout 중 remote side effect가 이미 성공한 …

#### 4. path

- **뜻:** 두 번째 pass에서는 handler exception과 retry/backoff를 실제로 실행한다.
- **왜 중요한가:** 세 번째 pass에서는 worker를 RUNNING 중 강제로 죽여 lease expiry 후 다른 worker가 reclaim하는지 본다.
- **예시:** 두 번째 pass에서는 handler exception과 retry/backoff를 실제로 실행한다.

Metric/log도 test 대상이다.  

---

## CHAPTER 10 · durable runner architecture는 contract·state·ownership·recovery를 한 그래프로 묶는다

### 시작 전 용어집

#### 1. durable runner

- **뜻:** 최종 구조는 submit adapter가 payload를 검증해 durable queue에 저장하고, scheduler가 ready job을 선택하며, worker가 atomic claim과 lease를 얻고, handler가 idempotency context 안에서 실행되고, attempt/result/checkpoint가 storage에 기록되는 흐름이다.
- **왜 중요한가:** Observability는 job ID와 attempt ID를 연결해 전체 lifecycle을 보여 준다.
- **예시:** 최종 구조는 submit adapter가 payload를 검증해 durable queue에 …

#### 2. architecture

- **뜻:** Queue storage failure, claim conflict, handler timeout, worker crash, shutdown, result serialization failure를 하나의 `FAILED` 문자열로 합치지 않는다.
- **왜 중요한가:** Recovery 가능한 상태와 terminal 상태를 구분한다.
- **예시:** Queue storage failure, claim conflict, handler timeout, worker …

#### 3. contract

- **뜻:** 이 capstone의 핵심은 background loop를 만드는 것이 아니라 **실행이 중복되거나 중간에 끊겨도 durable state가 다음 안전한 행동을 결정할 수 있도록 job lifecycle을 명시적 상태 머신과 ownership protocol로 만드는 것**이다.
- **예시:** 이 capstone의 핵심은 background loop를 만드는 것이 아니라 …

각 경계에는 독립 failure가 있다.
