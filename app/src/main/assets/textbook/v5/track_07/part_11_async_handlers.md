# PART 11 · BLOCK 01 · LESSON 11 · async handler에서 대기·병렬성·실패·취소를 분리하기

backend는 DB와 다른 API를 기다리는 시간이 많다. `async/await`는 이런 대기를 읽기 쉬운 control flow로 표현하지만, `await`를 썼다고 race·transaction·timeout·cancellation 문제가 해결되는 것은 아니다. 병렬로 실행할 수 있는 작업과 순서가 필요한 side effect를 구분하고, Promise rejection이 request error pipeline에 연결되는지 확인해야 한다. 이 PART는 async 코드를 **시간과 ownership이 있는 실행 흐름**으로 읽는 능력을 만든다.

---

## CHAPTER 01 · await는 process 전체가 아니라 현재 async continuation을 기다리게 한다

`await repository.findById(id)`를 만나면 현재 async function의 나머지 부분은 Promise가 settle될 때까지 중단된다. Node.js process 전체가 sleep하는 것은 아니며 event loop는 다른 request의 callback을 처리할 수 있다. 따라서 I/O wait가 많은 service는 한 thread에서도 여러 in-flight operation을 겹쳐 처리할 수 있다.

그러나 await 대상 전에 CPU-heavy loop를 실행하면 event loop가 막힌다. 같은 1초라는 시간이라도 nonblocking I/O wait와 synchronous CPU work는 다른 system behavior를 만든다. `async function`으로 선언했다고 내부 CPU 코드가 자동으로 background thread로 이동하지 않는다.

실습에서는 1초 `setTimeout` 기반 await route와 1초 busy loop route를 만들고 동시에 `/fast` 요청을 보낸다. fast latency가 어떻게 달라지는지 측정한다.

---

## CHAPTER 02 · sequential await는 dependency가 있을 때 필요하지만 독립 작업까지 직렬화할 수 있다

```ts
const profile = await loadProfile();
const settings = await loadSettings();
```

두 operation이 서로 독립이라면 총 latency가 합에 가까워질 수 있다. 동시에 시작해 `Promise.all`로 기다리면 최대 latency에 가까워질 수 있다. 반면 두 번째가 첫 결과를 사용한다면 병렬화할 수 없다. 먼저 data dependency를 그린다.

무조건 parallelize하면 downstream concurrency가 폭증할 수 있다. 한 request가 50개 query를 동시에 날리면 latency는 줄어도 DB pool을 독점한다. independent 여부와 system capacity를 함께 본다.

실습에서는 200ms fake call 두 개를 순차/병렬로 실행해 duration을 비교하고, 100개 병렬 call에서 pool semaphore가 필요한 상황을 관찰한다.

---

## CHAPTER 03 · Promise.all은 하나가 reject해도 이미 시작된 다른 side effect를 자동 취소하지 않는다

결제 승인과 재고 예약을 동시에 시작하고 `Promise.all`을 사용했다고 하자. 재고 예약이 실패해 Promise.all이 reject되어도 결제 request는 이미 provider로 전송됐을 수 있다. JavaScript Promise 조합은 distributed transaction rollback 기능이 아니다.

read-only independent query에서는 Promise.all이 적합할 수 있지만 irreversible side effect를 병렬화하기 전에 partial-success state와 compensation을 설계한다. cancellation signal을 지원해도 상대가 이미 처리한 side effect는 되돌아가지 않을 수 있다.

실습에서 A는 100ms 후 counter 증가, B는 50ms 후 reject하게 만든다. Promise.all catch가 실행된 뒤에도 A counter가 증가하는지 확인한다. control flow rejection과 side effect completion을 분리한다.

---

## CHAPTER 04 · Promise.allSettled는 모든 결과를 모으지만 business 성공 기준은 직접 정의해야 한다

notification을 email, SMS, push 세 channel로 보내고 일부 실패해도 나머지 결과를 보고 싶을 수 있다. `Promise.allSettled`는 각 Promise의 fulfilled/rejected 결과를 모은다. 하지만 `2/3 성공이면 전체 성공인가`, 실패한 channel을 retry할 것인가는 application policy다.

allSettled는 실패를 자동 처리하지 않는다. 결과를 무시하면 error가 조용히 사라진다. 각 rejection을 classification하고 telemetry에 남긴다. critical step이 하나라도 실패하면 use case가 실패해야 하는 경우 allSettled가 오히려 복잡할 수 있다.

실습에서 세 provider fake 결과를 조합하고 `REQUIRED`, `BEST_EFFORT` channel policy를 달리 적용한다. 같은 allSettled 결과에서 overall outcome이 달라지는 것을 test한다.

---

## CHAPTER 05 · async rejection이 framework error handler로 전달되는지 실제 version에서 확인한다

async handler가 `throw`하거나 awaited Promise가 reject할 때 framework가 자동으로 error middleware에 전달하는지 버전마다 다를 수 있다. wrapper가 필요한 환경에서 그냥 async callback을 등록하면 unhandled rejection 또는 request hang가 발생할 수 있다. 공식 문서와 실제 test를 사용한다.

try/catch로 모든 handler를 감싸 복붙하면 누락이 생기므로 framework-native mechanism이나 공통 adapter를 사용할 수 있다. 중요한 것은 error가 request ID와 함께 central error contract로 들어가는 것이다.

실습에서 awaited reject와 synchronous throw를 각각 발생시키고 error handler call count, final status, process-level unhandledRejection event를 관찰한다.

---

## CHAPTER 06 · fire-and-forget Promise는 request와 별도의 owner가 없으면 실패를 잃는다

```ts
sendReceiptEmail(order);
res.status(201).json(...);
```

이 코드는 response latency를 줄이는 것처럼 보이지만 email Promise가 reject했을 때 누가 retry하고 기록할지 없다. process가 response 직후 종료되면 작업이 사라질 수도 있다. `await하지 않음`은 background job system이 아니다.

작업이 중요한데 response와 분리하고 싶다면 durable queue/outbox에 등록해 worker가 ownership을 가져가게 한다. 정말 best-effort telemetry처럼 잃어도 되는 작업이라면 명시적으로 catch하고 failure budget을 정의한다.

실습에서 await하지 않은 Promise가 1초 뒤 reject하도록 만들고 request는 201을 반환하게 한다. process event와 log를 관찰한 뒤 durable job placeholder를 사용한 구조와 비교한다.

---

## CHAPTER 07 · deadline은 하위 async call이 사용할 남은 시간을 전달한다

전체 request deadline이 2초인데 첫 DB call에 1.4초를 썼다면 external API가 2초 timeout을 새로 갖게 하면 사용자 deadline을 초과한다. context에 absolute deadline 또는 remaining budget을 전달하고 하위 adapter가 더 짧은 timeout을 설정하게 할 수 있다.

각 layer가 독립적으로 긴 timeout을 가지면 nested call graph의 worst-case latency가 커진다. 반대로 너무 짧게 나누면 정상 dependency가 자주 실패한다. historical latency와 SLO를 근거로 budget을 배분한다.

실습에서 2초 deadline context를 만들고 500ms delay 후 remaining milliseconds가 줄어드는지 확인한다. outbound fake가 remaining budget보다 긴 delay면 AbortSignal로 취소되게 한다.

---

## CHAPTER 08 · cancellation은 요청 중단 신호이며 이미 완료된 side effect의 rollback이 아니다

AbortController 같은 mechanism으로 fetch나 일부 DB call을 중단할 수 있다. client disconnect나 deadline expiry에서 signal을 전달하면 useless work를 줄일 수 있다. 그러나 payment provider가 이미 charge를 처리했다면 local Promise를 abort해도 remote side effect는 사라지지 않는다.

operation을 cancellation-safe하게 만들려면 `아직 시작 전`, `진행 중 취소 가능`, `side effect committed`, `outcome unknown` 같은 state를 구분할 수 있다. cleanup action도 cancellation signal을 그대로 받아 즉시 중단하면 resource leak이 생길 수 있어 별도 cleanup budget이 필요하다.

실습에서 read-only fake fetch는 abort 즉시 중단하고, side-effect fake는 commit 시점 이후 abort해도 result reconciliation record를 남기게 한다.

---

## CHAPTER 09 · concurrency limit은 한 request가 downstream을 독점하지 못하게 한다

1000개 URL metadata를 가져오며 `Promise.all(urls.map(fetch))`를 실행하면 request 하나가 1000 connection을 동시에 열 수 있다. process file descriptor와 remote rate limit, memory를 압박한다. semaphore 또는 worker pool로 동시 작업 수를 제한하고 queue wait를 측정한다.

optimal concurrency는 task latency, downstream capacity, pool size에 따라 달라진다. 너무 낮으면 throughput이 떨어지고 너무 높으면 queue와 retry가 폭증한다. benchmark와 production telemetry를 사용한다.

실습에서 fake provider max concurrent counter를 기록하며 limit 1, 5, 50을 비교한다. 총 duration뿐 아니라 peak concurrency와 failure rate를 같이 본다.

---

## CHAPTER 10 · async iterator는 큰 sequence를 한 항목씩 소비해 memory를 제한할 수 있다

DB cursor나 paginated API를 async iterator로 감싸면 전체 100만 record를 memory에 올리지 않고 순차 처리할 수 있다. `for await` loop에서 each item을 처리하고 checkpoint를 남길 수 있다. 그러나 processing이 느리면 source가 backpressure를 지원하는지 확인한다.

iterator 중간에 error가 나면 이미 처리한 항목을 되돌릴 수 있는지, resume cursor가 있는지 설계한다. batch job에서는 exactly-once fantasy보다 idempotent item processing과 checkpoint가 현실적이다.

실습으로 10만 synthetic item generator를 array collect 방식과 async generator 방식으로 처리해 peak memory를 비교한다. 중간 50001번째에서 failure를 넣고 resume 전략을 작성한다.

---

## CHAPTER 11 · race condition은 single-threaded JavaScript에서도 await 사이에 발생할 수 있다

두 request가 같은 balance를 읽고 await 후 각각 update하면 event loop가 thread 하나여도 interleaving이 발생한다. `읽기 → await → 쓰기` 사이에 다른 request가 같은 state를 변경할 수 있다. single-threaded라는 사실은 application-level race를 없애지 않는다.

shared memory object에서도 await 사이의 stale read가 문제이고, database에서는 lost update와 transaction isolation이 필요하다. mutex를 process-local로 걸어도 여러 instance에서는 전체 system lock이 아니다.

실습으로 in-memory counter를 `const old=value; await delay(); value=old+1` 방식으로 100 concurrent request에 실행해 final count가 100보다 작아질 수 있는지 확인한다. atomic update/queue 방식과 비교한다.

---

## CHAPTER 12 · transaction callback 안의 await는 어떤 resource를 얼마나 오래 잡는지 확인한다

DB transaction callback에서 여러 await를 실행하면 transaction connection과 lock이 그동안 유지될 수 있다. 외부 HTTP를 10초 기다리는 동안 row lock을 잡으면 다른 request가 block된다. `async/await가 읽기 좋다`와 `transaction을 오래 유지해도 된다`는 별개다.

transaction 안에는 함께 atomic해야 하는 local DB 작업을 넣고 외부 side effect는 outbox/saga 같은 패턴으로 분리할 수 있다. isolation과 lock behavior는 TRACK 08에서 더 깊게 다룬다.

실습에서 transaction fake가 acquire/release 시각을 기록하도록 하고 내부에 2초 외부 delay를 넣는다. connection occupancy가 얼마나 늘어나는지 본 뒤 구조를 분리한다.

---

## CHAPTER 13 · async context는 Promise chain을 따라가지만 모든 실행 경계를 자동 통과하지 않는다

AsyncLocalStorage 같은 도구는 같은 async execution chain에서 request ID를 전달하는 데 유용하다. 하지만 worker thread, child process, message queue consumer는 새 execution context다. 필요한 trace/request/tenant metadata를 explicit message/header로 전파해야 한다.

library가 custom callback mechanism을 사용하거나 context가 끊기는 경우도 있을 수 있으므로 framework instrumentation을 검증한다. context가 없을 때 silent `undefined`로 log를 남길지 error를 내는지도 중요하다.

실습에서 nested Promise와 timer에서는 ID가 유지되는지, 새 worker/message simulation에서는 자동 유지되지 않는지 확인한다. boundary adapter가 context를 serialize/restore하게 한다.

---

## CHAPTER 14 · async debugging은 stack trace 하나보다 timeline과 span을 함께 본다

await 경계를 지나면 synchronous stack만으로 전체 latency를 이해하기 어렵다. request trace에서 DB span, external HTTP span, queue wait를 시간축으로 보면 어느 await가 느렸는지 알 수 있다. Promise가 pending인 이유가 CPU blocking인지 network wait인지 구분한다.

unhandled rejection과 timeout, abort는 서로 다른 event다. error log에 operation name, dependency, elapsed, remaining deadline을 넣으면 원인을 좁힐 수 있다. async profiler와 event-loop lag metric도 CPU blocking을 찾는 데 도움된다.

실습에서는 3단계 async flow에 시작/종료 timestamp와 trace span을 넣고 두 번째 dependency만 느리게 만든다. total duration에서 어느 단계가 기여했는지 계산한다.

---

## CHAPTER 15 · async 실습은 속도 최적화보다 partial failure와 ownership을 먼저 검증한다

주문 상세 API에서 profile, inventory status, recommendation을 가져온다. profile과 inventory는 required, recommendation은 best-effort라고 정의한다. 독립 required read는 제한된 병렬 실행을 하고, recommendation failure는 fallback으로 처리한다. 전체 request deadline과 각 dependency timeout을 설정한다.

검증에는 sequential vs parallel duration, one required reject, best-effort reject, client abort, deadline expiry, concurrency limit, unhandled fire-and-forget 금지, concurrent stale update를 포함한다. 각 case에서 response뿐 아니라 outstanding task가 남는지와 metric을 확인한다.

AI는 Promise combinator와 concurrency helper 문법을 만들어 줄 수 있다. 사람은 어떤 작업이 서로 독립인지, partial success를 허용할지, side effect를 병렬화해도 되는지, 누가 실패한 background work를 책임질지 결정한다. async code의 품질은 `await` 수가 아니라 **시간과 실패가 어느 owner에게 귀속되는지 설명할 수 있는가**로 판단한다.
