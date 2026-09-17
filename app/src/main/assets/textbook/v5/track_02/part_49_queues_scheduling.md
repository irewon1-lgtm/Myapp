# PART 49 · Queue와 scheduling — 작업 순서·용량·재시도를 운영 가능한 상태 머신으로 만들기

작업이 도착하는 속도와 처리하는 속도가 항상 같을 수는 없다. Queue는 이 차이를 흡수하고 worker는 일정한 용량으로 작업을 처리한다. 하지만 queue를 추가하는 순간 순서, 우선순위, 재시도, 중복, starvation, backlog라는 새로운 semantics가 생긴다. 핵심은 **작업을 저장하는 자료구조가 아니라 언제 어떤 작업이 실행되고 실패한 작업이 어떤 상태로 이동하는지**를 명확한 상태 머신으로 만드는 것이다.

---

## CHAPTER 01 · work queue는 producer와 worker의 속도를 분리한다

Producer가 작업을 생성하고 worker가 처리하는 구조에서 queue는 두 속도의 차이를 일시적으로 저장한다. Request가 순간적으로 몰려도 worker 수만큼만 실제 작업을 실행하면 downstream database나 API를 보호할 수 있다. 하지만 평균 유입량이 평균 처리량보다 계속 높다면 queue는 해결책이 아니라 backlog를 늦게 보여 주는 저장소가 된다.

Queue depth는 그래서 system health의 핵심 signal이다. 현재 길이만이 아니라 가장 오래 기다린 job의 age, enqueue rate, dequeue/complete rate를 함께 봐야 한다. Queue 1,000개가 문제인지 여부는 job이 1ms 만에 처리되는지 10초 걸리는지에 따라 다르다.

Bounded queue는 capacity가 찼을 때 producer를 block하거나 reject해 backpressure를 전달할 수 있다. Unbounded queue는 overload를 memory/disk에 계속 쌓아 outage를 지연시키므로 maximum backlog와 admission policy를 명시한다.

---

## CHAPTER 02 · FIFO는 단순하지만 fairness와 deadline을 자동으로 보장하지 않는다

First-In-First-Out queue는 먼저 들어온 작업을 먼저 처리해 순서를 이해하기 쉽다. 같은 중요도의 짧은 작업이 대부분인 환경에서는 합리적이다. 그러나 앞에 매우 긴 작업이 몰리면 뒤의 짧고 긴급한 작업도 기다리는 head-of-line blocking이 생길 수 있다.

FIFO가 공정해 보여도 tenant별 workload가 크게 다르면 한 고객이 queue를 대부분 차지해 다른 고객을 사실상 지연시킬 수 있다. Per-tenant queue, weighted round-robin, concurrency quota처럼 fairness를 더 높은 수준에서 정의해야 할 수 있다.

Deadline이 있는 작업은 도착 순서보다 남은 시간이 중요할 수 있다. 이미 deadline이 지난 job을 계속 처리해 결과를 버리는 것보다 dequeue 시점에 expiry를 확인하고 skip하는 정책이 자원을 아낄 수 있다. 순서 규칙은 업무 가치와 연결된다.

---

## CHAPTER 03 · priority queue는 heap으로 효율화할 수 있지만 tie-break 규칙이 필요하다

Priority가 작은 숫자일수록 먼저 실행한다고 하자. 모든 enqueue마다 전체 list를 정렬하는 대신 heap 자료구조를 사용하면 최소 priority item을 효율적으로 꺼낼 수 있다. Python의 heap 도구는 이러한 priority queue 구현의 기반이 될 수 있다.

Priority가 같은 두 job에서 비교 불가능한 payload object까지 tuple 비교가 진행되면 runtime error가 날 수 있다. `(priority, sequence_number, job)`처럼 monotonically increasing sequence를 tie-break로 넣으면 동일 priority에서 안정적인 insertion order를 유지할 수 있다.

Priority 값 자체도 policy다. 사용자 입력이 임의로 최고 priority를 지정하게 두면 모든 요청이 “긴급”이 되어 의미가 사라질 수 있다. Admission layer가 role과 job type에 따라 허용 priority 범위를 결정한다.

---

## CHAPTER 04 · delayed scheduling은 실행 시각과 ready queue를 분리한다

“10분 뒤 재시도”, “내일 09:00 실행” 같은 작업은 즉시 ready queue에 넣고 worker가 기다리게 하면 자원을 낭비한다. `not_before` 또는 deadline 기준으로 future set에 두고 실행 가능 시점이 되었을 때 ready queue로 이동시키는 구조를 사용할 수 있다.

Delay가 elapsed duration인지 wall-clock schedule인지 구분한다. Retry 30초 후는 monotonic duration 성격이 강하고, 매일 local 09:00은 timezone/DST가 포함된 calendar rule이다. 같은 timestamp field 하나에 둘을 섞으면 clock change에서 의미가 깨진다.

Process restart 후에도 future job을 보존해야 한다면 delayed schedule은 durable storage가 필요하다. Memory timer만으로는 restart 시 작업이 사라진다. Persistent scheduler는 next-run state와 recurrence rule을 함께 저장한다.

---

## CHAPTER 05 · worker pool size는 downstream capacity와 작업 성격으로 정한다

Worker를 늘리면 throughput이 높아질 수 있지만 database connection 20개밖에 없는데 worker 200개가 동시에 query를 시작하면 나머지는 connection pool 앞에서 대기한다. Worker 수는 CPU core, I/O wait 비율, downstream rate limit, memory per job을 함께 고려한다.

CPU-bound job을 thread worker로 늘려도 interpreter execution model 때문에 원하는 parallelism을 얻지 못할 수 있다. Process pool이나 별도 compute worker가 적합할 수 있다. I/O-bound job은 더 높은 concurrency를 사용할 수 있지만 remote API quota와 timeout을 지켜야 한다.

Autoscaling도 queue depth 하나만 보면 oscillation이 생길 수 있다. Job age, arrival rate, service time을 함께 사용하고 scale-up/down cooldown을 둔다. Worker 증설이 실제 bottleneck을 해소하는지도 측정한다.

---

## CHAPTER 06 · retry queue와 dead-letter state는 실패 분류를 보존해야 한다

모든 실패를 즉시 재시도하면 validation error와 permanent permission error까지 반복해 queue를 오염시킨다. Transient network error처럼 회복 가능성이 있는 실패만 retry policy에 넣고 deterministic failure는 바로 terminal state나 dead-letter queue로 이동시킨다.

Retry에는 attempt count, next-attempt time, last error category가 필요하다. Exponential backoff와 jitter를 사용하면 outage 중 모든 job이 동시에 재시도해 backend를 더 압박하는 것을 줄일 수 있다. Max attempts를 넘은 job을 버리기보다 조사와 재처리가 가능한 dead-letter 저장소에 보존한다.

Dead-letter는 쓰레기통이 아니다. 원본 payload, safe error context, schema version, 발생 시각을 보존하고 재처리 시 이미 성공한 side effect가 있는지 확인해야 한다.

---

## CHAPTER 07 · job은 중복 전달을 전제로 idempotent하게 설계할 가치가 있다

Worker가 작업을 성공적으로 처리한 뒤 acknowledgement 전에 crash하면 queue는 같은 job을 다시 전달할 수 있다. Exactly-once execution을 인프라 한 옵션으로 보장하려 하기보다 동일 job이 두 번 실행되어도 결과가 중복되지 않는 idempotency contract를 만드는 편이 현실적이다.

Payment처럼 side effect가 있는 operation은 job ID/idempotency key를 durable storage에 기록하고 같은 key의 완료 결과를 재사용할 수 있다. 단순 email 발송은 완전 idempotent하기 어렵기 때문에 send-log와 dedup window를 사용할 수 있다.

Idempotency key의 scope와 lifetime도 중요하다. 영원히 모든 key를 보관할 수 없다면 business retry 가능 기간보다 긴 retention을 정하고 이후 duplicate 위험을 문서화한다.

---

## CHAPTER 08 · priority가 있으면 starvation을 막는 aging이나 quota가 필요할 수 있다

High-priority job이 계속 들어오면 low-priority queue가 영원히 처리되지 않는 starvation이 발생할 수 있다. 오래 기다린 job의 effective priority를 점차 높이는 aging, class별 최소 worker quota, weighted scheduling으로 starvation을 완화할 수 있다.

반대로 너무 강한 aging은 실제 긴급 job을 밀어낼 수 있다. Priority는 절대 서열이 아니라 latency objective를 표현하는 정책으로 설계한다. 각 class의 최대 대기시간 목표를 먼저 정한 뒤 scheduler를 맞춘다.

Fairness와 throughput도 trade-off가 있다. 큰 batch job을 조금씩 잘라 interactive job 사이에 끼우면 latency는 좋아지지만 context/setup overhead가 늘 수 있다. 실제 SLA와 workload distribution을 기준으로 선택한다.

---

## CHAPTER 09 · scheduler observability는 backlog의 원인을 분해해야 한다

Queue가 길어졌다는 alert만으로는 원인을 알기 어렵다. Arrival rate가 급증했는지, worker가 줄었는지, service time이 늘었는지, retry job이 폭증했는지 분해해야 한다. Metric에는 enqueue/completion/retry/failure rate, queue age, running worker, processing duration distribution이 필요하다.

Job trace에는 enqueue time, first start, attempt start/end, final completion을 연결하면 queue wait와 execution latency를 분리할 수 있다. 사용자 입장에서 10초 걸린 작업 중 실제 계산은 100ms이고 queue wait가 9.9초일 수도 있다.

High-cardinality job ID를 metric label로 넣지 않고 log/trace에서 개별 job을 찾는다. Queue name, job class, result category처럼 집계 가능한 차원만 metric에 사용한다.

---

## CHAPTER 10 · scheduling contract는 order·capacity·retry·dedup·deadline을 함께 정의한다

작업 실행기를 API로 제공할 때 “queue에 넣는다”만으로는 부족하다. FIFO인지 priority인지, 최대 backlog는 얼마인지, worker concurrency는 어떻게 제한되는지, 실패는 몇 번 재시도하는지, duplicate delivery가 가능한지, job deadline을 넘으면 어떻게 되는지 함께 정의한다.

이 contract가 있어야 caller가 결과 시점을 예측하고 안전한 job을 설계할 수 있다. Queue implementation을 memory heap에서 remote broker로 바꾸더라도 이 semantics를 유지하면 상위 코드 영향이 줄어든다.

Queue와 scheduler의 핵심은 **비동기 실행을 뒤로 미루는 것이 아니라 제한된 처리 용량 안에서 어떤 작업에 언제 실행권을 주고 실패·중복·과부하를 어떤 상태 전이로 관리할지 결정하는 것**이다.