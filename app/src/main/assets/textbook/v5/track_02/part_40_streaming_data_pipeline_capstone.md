# PART 40 · Streaming data pipeline capstone — ingest·parse·validate·transform·checkpoint를 하나의 흐름으로 연결하기

대용량 데이터를 처리할 때 모든 입력을 메모리에 올리고 마지막에 한 번 저장하는 구조는 단순하지만 데이터가 커지면 memory peak, 재시작 비용, 부분 실패 처리 문제가 커진다. Streaming pipeline은 입력을 작은 단위로 읽고, 검증하고, 변환하고, 저장하면서 진행 상태를 남긴다. 이 구조에는 iterator, parser, schema validation, backpressure, batching, idempotency, observability가 한꺼번에 연결된다. 핵심은 **값이 흐르는 각 단계의 계약과 실패 뒤 재개 위치를 명시하는 것**이다.

---

## CHAPTER 01 · pipeline은 stage 목록이 아니라 입력·출력·실패 계약의 연결이다

### 시작 전 용어집

#### 1. pipeline

- **뜻:** 같은 이름의 pipeline이라도 이 계약에 따라 완전히 다른 시스템이 된다.
- **왜 중요한가:** Raw input stage는 bytes나 text line을 생산하고 parser는 구조화된 DTO를 만든다.
- **예시:** `읽기 → 파싱 → 검증 → 변환 → …

#### 2. stage

- **뜻:** 각 stage가 어떤 타입을 받고 무엇을 반환하는지, 한 항목 실패가 전체 작업을 중단하는지, 순서를 보존해야 하는지, side effect가 있는지를 정해야 한다.
- **왜 중요한가:** Validation을 통과한 뒤에야 domain value가 되고 pure transform이 계산을 수행한다.
- **예시:** `읽기 → 파싱 → 검증 → 변환 → …

#### 3. 입력

- **뜻:** `읽기 → 파싱 → 검증 → 변환 → 저장`이라는 다섯 단계를 적는 것만으로는 충분하지 않다.
- **왜 중요한가:** Sink는 durable storage에 결과를 반영한다.
- **예시:** `읽기 → 파싱 → 검증 → 변환 → …

#### 4. 출력

- **뜻:** 이 경계를 지키면 malformed input과 business rule 실패, storage outage를 서로 다른 failure class로 구분할 수 있다.
- **왜 중요한가:** Stage 사이의 타입이 명확하면 잘못된 data가 어디까지 침투했는지 추적하기 쉽다.
- **예시:** 이 경계를 지키면 malformed input과 business rule 실패, …

`dict[str, Any]` 하나를 끝까지 전달하면 모든 stage가 같은 validation을 다시 해야 하고 key typo가 늦게 발견된다. Boundary마다 representation을 좁힌다.

Pipeline 설계는 function composition과 비슷하지만 sink와 checkpoint 같은 side effect가 있기 때문에 실패 원자성과 replay를 반드시 함께 다룬다.

---

## CHAPTER 02 · ingest는 source를 전체 materialize하지 않고 bounded stream으로 제공한다

### 시작 전 용어집

#### 1. ingest

- **뜻:** File, HTTP response, message queue, database cursor는 모두 입력 source가 될 수 있다.
- **왜 중요한가:** Source를 list로 한 번에 읽는 대신 iterator나 async iterator로 항목을 하나씩 제공하면 memory usage가 source 크기와 직접 비례하지 않게 만들 수 있다.
- **예시:** File, HTTP response, message queue, database cursor는 모두 …

#### 2. source

- **뜻:** 하지만 streaming source는 single-pass일 수 있다.
- **왜 중요한가:** Validation을 위해 한 번 모두 순회한 뒤 transform 단계에서 다시 사용하려 하면 이미 소비되어 있을 수 있다.
- **예시:** 하지만 streaming source는 single-pass일 수 있다.

#### 3. materialize

- **뜻:** 여러 번 읽어야 한다면 replay 가능한 source인지, snapshot을 저장할지 결정한다.
- **왜 중요한가:** Input 자체가 무한하거나 끝을 알 수 없는 경우에는 count, time window, cancellation 같은 budget을 둔다.
- **예시:** 여러 번 읽어야 한다면 replay 가능한 source인지, snapshot을 …

#### 4. bounded stream

- **뜻:** Pagination API가 같은 next token을 반복하는 bug도 무한 stream으로 바뀔 수 있으므로 token cycle과 maximum page count를 방어한다.
- **왜 중요한가:** Source resource lifetime을 pipeline 전체보다 길게 잡지 않는다.
- **예시:** Pagination API가 같은 next token을 반복하는 bug도 무한 …

File handle과 DB cursor가 generator lifetime에 묶여 있다면 consumer가 멈췄을 때 close가 보장되는지 context manager와 cancellation path를 검증한다.

---

## CHAPTER 03 · parser는 incomplete input·invalid syntax·valid structure를 구분한다

### 시작 전 용어집

#### 1. parser

- **뜻:** Parser state는 `need more data`, `invalid`, `complete`를 구분해야 한다.
- **왜 중요한가:** Syntax가 valid해도 schema가 맞는 것은 아니다.
- **예시:** Parser state는 `need more data`, `invalid`, `complete`를 구분해야 …

#### 2. incomplete input

- **뜻:** Line-delimited JSON처럼 record boundary가 명확한 source는 한 줄씩 parse할 수 있지만 binary frame이나 multiline CSV는 여러 chunk를 모아야 하나의 record가 완성될 수 있다.
- **왜 중요한가:** JSON object가 parse됐지만 required field가 없거나 type이 잘못될 수 있다.
- **예시:** Line-delimited JSON처럼 record boundary가 명확한 source는 한 줄씩 …

#### 3. invalid syntax

- **뜻:** Parser와 schema validator를 분리하면 오류 위치와 recovery 정책을 다르게 할 수 있다.
- **왜 중요한가:** Record에 source offset, line number, message ID 같은 provenance를 붙이면 downstream에서 오류가 발생했을 때 원본 위치를 찾을 수 있다.
- **예시:** Parser와 schema validator를 분리하면 오류 위치와 recovery 정책을 …

#### 4. valid structure

- **뜻:** Transformation 과정에서 이 metadata를 잃으면 quarantine된 record를 재현하기 어렵다.
- **왜 중요한가:** Untrusted input에는 record size, nesting depth, field count 같은 resource limit을 적용한다.
- **예시:** Transformation 과정에서 이 metadata를 잃으면 quarantine된 record를 재현하기 …

Streaming이라고 해서 하나의 record가 무한히 커지는 문제까지 자동으로 해결되는 것은 아니다.

---

## CHAPTER 04 · validation을 통과한 뒤 pure transform으로 business 계산을 격리한다

### 시작 전 용어집

#### 1. validation

- **뜻:** Parser output을 바로 database sink에 쓰지 않고 domain validation을 통과시킨 뒤 계산 stage로 보낸다.
- **왜 중요한가:** Transform이 pure function에 가까우면 같은 validated input으로 동일 결과를 재현할 수 있어 retry와 test가 단순해진다.
- **예시:** Parser output을 바로 database sink에 쓰지 않고 domain …

#### 2. pure transform

- **뜻:** 외부 API enrichment처럼 I/O가 필요한 작업은 pure transform과 다른 stage로 분리해 timeout/retry/concurrency policy를 독립적으로 둔다.
- **왜 중요한가:** 여러 transform이 순서를 요구하는지 확인한다.
- **예시:** 외부 API enrichment처럼 I/O가 필요한 작업은 pure transform과 …

#### 3. business

- **뜻:** Transformation에는 normalization, enrichment, aggregation이 포함될 수 있다.
- **왜 중요한가:** Currency conversion 전에 unit normalization이 필요하다면 stage 순서가 contract다.
- **예시:** Transformation에는 normalization, enrichment, aggregation이 포함될 수 있다.

#### 4. API

- **뜻:** 서로 독립적인 transform은 parallelize할 수 있지만 공유 state가 있으면 순서와 atomicity를 다시 검토한다.
- **왜 중요한가:** 하나의 input이 수천 output을 만드는 flat-map stage는 downstream queue를 급격히 채울 수 있으므로 expansion ratio도 capacity planning에 포함한다.
- **예시:** 서로 독립적인 transform은 parallelize할 수 있지만 공유 state가 …

변환 결과가 원본보다 커질 수 있다. 

---

## CHAPTER 05 · batching은 per-item overhead를 줄이는 대신 failure granularity를 크게 만든다

### 시작 전 용어집

#### 1. batching

- **뜻:** Database insert나 network API를 항목 하나씩 호출하면 round-trip과 transaction overhead가 커질 수 있다.
- **왜 중요한가:** 여러 항목을 batch로 묶으면 throughput이 좋아질 수 있지만 batch 중 한 record가 실패할 때 전체 batch가 실패하는지 일부 성공인지 semantics를 확인해야 한다.
- **예시:** Database insert나 network API를 항목 하나씩 호출하면 round-trip과 …

#### 2. per-item overhead

- **뜻:** Batch size가 너무 크면 memory와 latency가 증가하고 retry 시 같은 큰 작업을 다시 수행해야 한다.
- **왜 중요한가:** 실제 payload 크기와 sink capacity를 기준으로 측정한다.
- **예시:** Batch size가 너무 크면 memory와 latency가 증가하고 retry …

#### 3. failure granularity

- **뜻:** Time-based flush와 count-based flush를 함께 사용할 수 있다.
- **왜 중요한가:** Traffic이 적을 때 batch가 꽉 찰 때까지 영원히 기다리지 않도록 maximum wait를 둔다.
- **예시:** Time-based flush와 count-based flush를 함께 사용할 수 있다.

#### 4. API

- **뜻:** Shutdown 시 남은 partial batch를 flush할지 discard할지도 정책이다.
- **왜 중요한가:** Batch ID와 item ID를 함께 기록하면 partial retry와 reconciliation을 할 수 있다.
- **예시:** Shutdown 시 남은 partial batch를 flush할지 discard할지도 정책이다.

너무 작으면 overhead 절감 효과가 없다. 

  

 Batch abstraction이 individual record identity를 지우지 않게 한다.

---

## CHAPTER 06 · backpressure는 downstream이 느릴 때 upstream이 무한히 쌓이지 않게 한다

### 시작 전 용어집

#### 1. backpressure

- **뜻:** Backpressure가 source까지 전파될 수 없는 경우가 있다.
- **왜 중요한가:** External message broker가 계속 데이터를 보내거나 webhook traffic을 거부할 수 없다면 durable buffer와 admission control이 필요하다.
- **예시:** Backpressure가 source까지 전파될 수 없는 경우가 있다.

#### 2. stream

- **뜻:** Worker를 늘리는 것만으로 downstream database capacity가 커지지 않는다.
- **왜 중요한가:** Memory queue로 absorb할 수 있는 범위를 계산한다.
- **예시:** Worker를 늘리는 것만으로 downstream database capacity가 커지지 않는다.

#### 3. queue

- **뜻:** Producer가 초당 10,000개를 만들고 sink가 1,000개만 처리한다면 중간 queue가 계속 커진다.
- **왜 중요한가:** Unbounded queue는 잠시 성공처럼 보이다가 memory exhaustion과 수십 분의 latency를 만든다.
- **예시:** Producer가 초당 10,000개를 만들고 sink가 1,000개만 처리한다면 중간 …

#### 4. thread

- **뜻:** Thread/process pool도 worker 수와 submission queue를 함께 봐야 한다.
- **왜 중요한가:** Queue depth, oldest item age, processing rate를 metric으로 관측하면 saturation이 발생하기 전에 알 수 있다.
- **예시:** Thread/process pool도 worker 수와 submission queue를 함께 봐야 …

Bounded queue가 가득 차면 producer를 기다리게 하거나 load-shedding 정책을 적용해야 한다.

Async pipeline에서는 semaphore와 bounded queue로 in-flight task를 제한할 수 있다.  

  

 단순 CPU usage보다 pipeline health를 더 직접적으로 보여 주는 지표다.

---

## CHAPTER 07 · quarantine은 잘못된 record를 조용히 버리지 않고 별도 상태로 보존한다

### 시작 전 용어집

#### 1. quarantine

- **뜻:** Log ingestion처럼 한 malformed record 때문에 전체 stream을 멈추는 것이 부적절한 경우에는 실패 record를 quarantine에 저장하고 계속 처리할 수 있다.
- **왜 중요한가:** 이때 원본 data, source 위치, error category, parser/schema version을 함께 남겨 나중에 수정 후 replay할 수 있게 한다.
- **예시:** Log ingestion처럼 한 malformed record 때문에 전체 stream을 …

#### 2. record

- **뜻:** Schema registry가 잘못되어 100% record가 실패하는데 pipeline이 성공 상태로 끝나면 대규모 데이터 손실을 숨긴다.
- **왜 중요한가:** Error rate threshold를 넘으면 전체 job을 중단하거나 alert를 발생시킨다.
- **예시:** Schema registry가 잘못되어 100% record가 실패하는데 pipeline이 성공 …

#### 3. 상태

- **뜻:** 모든 오류를 quarantine해서 계속 가는 것도 위험하다.
- **왜 중요한가:** PII와 secret이 포함된 원본 record를 quarantine storage에 그대로 보관하면 새로운 보안 surface가 생긴다.
- **예시:** 모든 오류를 quarantine해서 계속 가는 것도 위험하다.

#### 4. stream

- **뜻:** 필요한 field만 보존하거나 encryption/access-control을 적용한다.
- **왜 중요한가:** Quarantine replay는 같은 sink에 duplicate write를 만들 수 있으므로 idempotency key와 version을 사용한다.
- **예시:** 필요한 field만 보존하거나 encryption/access-control을 적용한다.

“건너뛰었다”와 “나중에 복구 가능하게 분리했다”는 전혀 다른 품질 수준이다.

---

## CHAPTER 08 · checkpoint는 처리 위치와 durable effect의 관계를 고정한다

### 시작 전 용어집

#### 1. checkpoint

- **뜻:** Checkpoint에는 source offset/token과 그 시점까지 sink effect가 durable하게 반영됐다는 관계가 필요하다.
- **왜 중요한가:** Checkpoint를 sink commit 전에 먼저 기록하면 crash 후 이미 처리했다고 믿고 실제 저장되지 않은 record를 건너뛸 수 있다.
- **예시:** Checkpoint에는 source offset/token과 그 시점까지 sink effect가 durable하게 …

#### 2. durable effect

- **뜻:** 백만 record 중 80만 개를 처리한 뒤 process가 죽었을 때 처음부터 다시 시작할지 마지막 안전 지점부터 재개할지 결정해야 한다.
- **왜 중요한가:** Sink commit 후 checkpoint를 늦게 기록하면 재시작 때 일부 record를 다시 처리할 수 있다.
- **예시:** 백만 record 중 80만 개를 처리한 뒤 process가 …

#### 3. process

- **뜻:** Source와 sink가 같은 transaction을 공유하지 않는 distributed pipeline에서는 보통 at-least-once delivery와 idempotent processing으로 결과를 수렴시킨다.
- **왜 중요한가:** Checkpoint version에는 parser/schema/transform version을 연결할 수 있다.
- **예시:** Source와 sink가 같은 transaction을 공유하지 않는 distributed pipeline에서는 …

#### 4. set

- **뜻:** 후자의 경우 idempotent sink로 duplicate를 견디는 설계가 더 안전할 수 있다.
- **왜 중요한가:** Exactly-once라는 표현을 쉽게 사용하지 않는다.
- **예시:** 후자의 경우 idempotent sink로 duplicate를 견디는 설계가 더 …

코드가 바뀐 뒤 과거 checkpoint를 이어서 처리해도 semantic consistency가 유지되는지 검토한다.

---

## CHAPTER 09 · reconciliation은 처리 완료 수가 아니라 source와 sink의 의미적 일치를 검증한다

### 시작 전 용어집

#### 1. reconciliation

- **뜻:** Job이 exception 없이 끝났고 `processed=1,000,000`이라고 출력해도 모든 데이터가 올바르게 저장됐다는 보장은 없다.
- **왜 중요한가:** Source count, accepted, rejected, quarantined, written, deduplicated를 conservation equation처럼 비교할 수 있다.
- **예시:** Job이 exception 없이 끝났고 `processed=1,000,000`이라고 출력해도 모든 데이터가 …

#### 2. source

- **뜻:** 금액이나 수량처럼 aggregate invariant가 있다면 source 총합과 sink 총합을 비교한다.
- **왜 중요한가:** Record 수가 같아도 transformation에서 값이 잘못 계산되었으면 aggregate mismatch가 드러날 수 있다.
- **예시:** 금액이나 수량처럼 aggregate invariant가 있다면 source 총합과 sink …

#### 3. sink

- **뜻:** Sampled record의 end-to-end lineage를 추적해 source raw input에서 final sink row까지 변환 결과를 검증한다.
- **왜 중요한가:** High-risk migration에서는 full checksum이나 partition-level aggregate를 사용할 수 있다.
- **예시:** Sampled record의 end-to-end lineage를 추적해 source raw input에서 …

#### 4. 검증

- **뜻:** Observability는 progress bar가 아니라 correctness evidence다.
- **왜 중요한가:** 재처리와 duplicate가 있는 시스템에서는 processed count와 unique committed count를 구분한다.
- **예시:** Observability는 progress bar가 아니라 correctness evidence다.

---

## CHAPTER 10 · streaming pipeline의 architecture는 재시작 가능한 상태 머신으로 설명할 수 있어야 한다

### 시작 전 용어집

#### 1. stream

- **뜻:** Streaming capstone의 핵심은 **iterator를 쓰는 기술이 아니라 data가 이동하는 모든 경계에서 타입·실패·resource·durability를 정의해 큰 입력도 bounded resource로 처리하고 중간 실패 뒤 정확히 복구할 수 있는 구조를 만드는 것**이다.
- **왜 중요한가:** 완성된 pipeline은 `SOURCE → PARSED → VALIDATED → TRANSFORMED → BATCHED → COMMITTED → CHECKPOINTED`라는 상태 흐름으로 설명할 수 있다.
- **예시:** Streaming capstone의 핵심은 **iterator를 쓰는 기술이 아니라 data가 …

#### 2. architecture

- **뜻:** 각 transition이 실패하면 어떤 상태가 남고 어떤 operation을 재시도할 수 있는지 정의한다.
- **왜 중요한가:** Pure stage는 자유롭게 재실행할 수 있지만 external effect stage는 idempotency가 필요하다.
- **예시:** 각 transition이 실패하면 어떤 상태가 남고 어떤 operation을 …

#### 3. 상태

- **뜻:** Bounded queue와 concurrency limit은 resource budget을 지키고, quarantine은 data quality failure를 격리하며 checkpoint는 crash recovery를 가능하게 한다.
- **왜 중요한가:** Test는 malformed input, sink partial failure, duplicate replay, cancellation during batch, checkpoint crash window, queue saturation을 각각 다른 failure class로 구성한다.
- **예시:** Bounded queue와 concurrency limit은 resource budget을 지키고, quarantine은 …

#### 4. checkpoint

- **뜻:** 정상 파일 한 번 처리 성공만으로 pipeline을 검증했다고 말하지 않는다.
- **예시:** 정상 파일 한 번 처리 성공만으로 pipeline을 검증했다고 …
