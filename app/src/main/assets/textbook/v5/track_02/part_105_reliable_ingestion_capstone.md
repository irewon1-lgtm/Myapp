# PART 105 · Reliable ingestion capstone — untrusted input에서 atomic publish까지 전체 failure path를 연결하기

TRACK 02의 마지막 PART는 새 문법 하나를 더 배우는 대신 앞에서 분리해 배운 경계를 실제 ingestion service 하나에 연결한다. 외부 archive/CSV/JSON을 받아 검증하고, 중복을 식별하며, bounded concurrency로 처리하고, 외부 dependency에는 deadline·retry·circuit breaker를 적용하고, checkpoint와 atomic publish를 거쳐 결과를 공개한다. 목표는 happy path가 아니라 **어느 단계에서 실패해도 무엇이 보존되고 무엇을 다시 실행해야 하는지 설명 가능한 구조**를 만드는 것이다.

---

## CHAPTER 01 · untrusted input gate는 parse 전에 크기·형식·저장 위치를 제한한다

### 시작 전 용어집

#### 1. untrusted input

- **뜻:** Upload를 받자마자 parser에 넘기지 않는다.
- **왜 중요한가:** Request body 최대 크기, 허용 content type, 임시 저장 budget, 파일 개수를 먼저 제한한다.
- **예시:** network input / -> admission/size limit

#### 2. gate

- **뜻:** Archive라면 P92의 entry/path/symlink/expansion budget을 적용한다.
- **왜 중요한가:** Staging directory는 최종 publish location과 분리한다.
- **예시:** network input / -> admission/size limit

#### 3. parse

- **뜻:** 입력 reject는 내부 parser crash와 다른 failure class다.
- **왜 중요한가:** 사용자에게는 수정 가능한 validation error를 주고, security/policy rejection은 운영 metric으로 별도 집계한다.
- **예시:** network input / -> admission/size limit

#### 4. 검증

- **뜻:** 검증되지 않은 파일이 serving path에 노출되지 않게 한다.
- **예시:** network input / -> admission/size limit

```text
network input
  -> admission/size limit
  -> private staging
  -> archive inventory
  -> extraction policy
```

 

 

---

## CHAPTER 02 · parse와 schema validation은 transport 형식과 domain 의미를 두 단계로 분리한다

### 시작 전 용어집

#### 1. parse

- **뜻:** CSV parser는 record를 만들고 JSON parser는 value tree를 만든다.
- **왜 중요한가:** 그 다음 domain schema가 required field, identifier, number precision, enum, cross-field invariant를 검증한다.
- **예시:** bytes -> text/codec -> syntax parser -> structural …

#### 2. schema validation

- **뜻:** 각 단계의 error vocabulary를 분리하면 사용자는 “UTF-8 decode 실패”, “CSV column 누락”, “amount가 음수”를 구체적으로 고칠 수 있다.
- **왜 중요한가:** Parser가 성공했다고 object를 바로 database에 넣지 않는다.
- **예시:** bytes -> text/codec -> syntax parser -> structural …

#### 3. transport

- **뜻:** Unknown field 정책과 schema version을 확인하고 valid-by-construction domain object를 만든다.
- **예시:** bytes -> text/codec -> syntax parser -> structural …

```text
bytes -> text/codec -> syntax parser -> structural schema -> domain object
```


 

---

## CHAPTER 03 · content identity와 operation identity를 분리해 duplicate를 안전하게 처리한다

### 시작 전 용어집

#### 1. content identity

- **뜻:** 같은 파일을 두 번 업로드할 수 있고 서로 다른 파일 이름이 같은 bytes를 담을 수도 있다.
- **왜 중요한가:** Content digest는 bytes identity를 제공하고 upload/request ID는 operation identity를 제공한다.
- **예시:** content_digest -> 동일 입력 탐지 / job_id -> …

#### 2. operation identity

- **뜻:** Digest 하나를 authorization token처럼 사용하지 않는다.
- **왜 중요한가:** 같은 content라도 다른 tenant의 권한과 lifecycle은 독립적일 수 있다.
- **예시:** content_digest -> 동일 입력 탐지 / job_id -> …

#### 3. duplicate

- **뜻:** Dedup 정책이 “같은 content는 결과 재사용”이라면 processing/schema version까지 cache key에 포함한다.
- **왜 중요한가:** Code가 바뀌었는데 old result를 무조건 반환하지 않는다.
- **예시:** content_digest -> 동일 입력 탐지 / job_id -> …

```text
content_digest -> 동일 입력 탐지
job_id         -> 처리 lifecycle 추적
idempotency_key-> 동일 요청 retry 묶기
```

 

 

---

## CHAPTER 04 · bounded concurrency는 CPU·memory·downstream capacity를 동시에 넘지 않게 한다

### 시작 전 용어집

#### 1. bounded concurrency

- **뜻:** Valid record 100만 개를 한 번에 task 100만 개로 만들면 event loop가 있어도 memory와 queue가 폭증한다.
- **왜 중요한가:** Bounded queue와 worker pool, semaphore를 사용해 in-flight work를 제한한다.
- **예시:** parser -> bounded queue -> N workers -> …

#### 2. CPU

- **뜻:** Worker count는 CPU core 수만 보고 정하지 않는다.
- **왜 중요한가:** External API concurrency limit, DB pool, record별 memory를 함께 측정한다.
- **예시:** parser -> bounded queue -> N workers -> …

#### 3. memory

- **뜻:** Queue를 무제한으로 두면 overload가 memory로 이동할 뿐이다.
- **왜 중요한가:** Parser 속도가 worker보다 빠르면 queue가 차고 upstream이 기다리면서 backpressure가 전달되어야 한다.
- **예시:** parser -> bounded queue -> N workers -> …

```text
parser -> bounded queue -> N workers -> bounded downstream client
```

 

 

---

## CHAPTER 05 · external dependency에는 하나의 deadline 안에서 retry와 circuit breaker를 조합한다

### 시작 전 용어집

#### 1. external dependency

- **뜻:** 각 job/record에는 remaining deadline이 있고 attempt timeout과 backoff는 그 안에서만 동작한다.
- **왜 중요한가:** Transient failure만 retry하고, dependency가 지속 실패하면 breaker가 OPEN되어 새 호출을 빠르게 차단한다.
- **예시:** deadline / -> attempt timeout

#### 2. deadline

- **뜻:** Breaker rejection, deadline exhaustion, validation failure는 서로 다른 final status로 기록해 재처리 정책을 구분한다.
- **왜 중요한가:** Retryable operation이 side effect를 만든다면 idempotency key가 있어야 한다.
- **예시:** deadline / -> attempt timeout

#### 3. retry

- **뜻:** Timeout 후 결과를 모르는 ambiguous state에서는 무작정 새 operation을 만들지 않는다.
- **예시:** deadline / -> attempt timeout

```text
deadline
  -> attempt timeout
  -> retry classification/backoff
  -> breaker admission
```

 


---

## CHAPTER 06 · checkpoint와 atomic publish는 처리 progress와 외부 가시성을 분리한다

### 시작 전 용어집

#### 1. checkpoint

- **뜻:** Long-running job은 chunk 단위로 output을 durable staging에 commit하고 checkpoint를 갱신할 수 있다.
- **왜 중요한가:** Crash 후 마지막 checkpoint부터 replay하되 output operation은 duplicate-safe해야 한다.
- **예시:** working generation -> validate -> fsync/commit -> atomic …

#### 2. atomic publish

- **뜻:** 최종 결과가 모두 준비되기 전에는 public manifest가 old generation을 가리키게 유지한다.
- **왜 중요한가:** 검증 완료 후 P91의 replace model로 새 manifest/generation을 publish하면 reader는 partial output 대신 이전 또는 새 완성본을 보게 할 수 있다.
- **예시:** working generation -> validate -> fsync/commit -> atomic …

#### 3. progress

- **뜻:** Output file 여러 개를 하나의 rename으로 완전히 atomic하게 바꿀 수 없는 환경에서는 generation directory와 작은 pointer/manifest를 commit point로 사용한다.
- **예시:** working generation -> validate -> fsync/commit -> atomic …

```text
working generation -> validate -> fsync/commit -> atomic publish pointer
```


---

## CHAPTER 07 · graceful shutdown과 recovery drill은 같은 상태 기계를 반대 방향에서 검증한다

### 시작 전 용어집

#### 1. graceful shutdown

- **뜻:** Shutdown 요청이 오면 새 upload/job admission을 멈추고 parser와 worker queue를 drain한다.
- **왜 중요한가:** 전체 shutdown deadline 안에 완료되지 않은 task는 cancellation하고, durable checkpoint를 남긴 뒤 resource를 역순으로 닫는다.
- **예시:** Shutdown 요청이 오면 새 upload/job admission을 멈추고 parser와 …

#### 2. recovery drill

- **뜻:** Recovery drill에서는 의도적으로 parse 중, chunk commit 후, checkpoint 전, publish 직전에 process를 죽여본다.
- **왜 중요한가:** 각 crash point에서 duplicate는 허용 범위 내인지, loss는 없는지 실제 state를 확인한다.
- **예시:** Recovery drill에서는 의도적으로 parse 중, chunk commit 후, …

#### 3. 상태

- **뜻:** 다음 startup에서는 incomplete job을 검색해 staging/checkpoint/input digest를 검증한 뒤 resume 또는 quarantine한다.
- **왜 중요한가:** “process가 정상 종료되면 cleanup된다”는 가정만으로 correctness를 만들지 않는다.
- **예시:** 다음 startup에서는 incomplete job을 검색해 staging/checkpoint/input digest를 검증한 …

---

## CHAPTER 08 · reliable ingestion contract는 각 경계의 보장을 조합해 end-to-end로 설명한다

### 시작 전 용어집

#### 1. reliable ingestion

- **뜻:** 최종 설계는 한 문장으로 “안전하다”고 말하지 않는다.
- **왜 중요한가:** Input은 size/path budget으로 제한되고, parser와 schema가 분리되며, content/job identity가 구분되고, concurrency가 bounded되고, external effect는 deadline·retry·idempotency·breaker를 거치며, progress는 checkpoint되고 결과는 atomic publish된다고 단계별로 설명한다.
- **예시:** 최종 설계는 한 문장으로 “안전하다”고 말하지 않는다.

#### 2. contract

- **뜻:** 최종 검증 matrix에는 malformed input, archive traversal, duplicate upload, downstream timeout, retry 후 성공, breaker open, worker crash, checkpoint corruption, shutdown 중 in-flight work, publish 직전 crash가 포함된다.
- **왜 중요한가:** 이 PART의 핵심은 **신뢰성 기능을 개별 유틸리티로 나열하지 않고, 입력이 시스템에 들어와 durable 결과로 공개될 때까지 각 failure boundary의 ownership·budget·replay semantics를 연결해 전체 프로그램 계약으로 만드는 것**이다.
- **예시:** 최종 검증 matrix에는 malformed input, archive traversal, duplicate …
