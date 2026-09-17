# PART 41 · Async Runtime and Coroutines — suspension, executors, wakeups, cancellation

async runtime의 핵심은 “thread를 적게 쓴다”가 아니다. task state를 stack 밖에 보존하고, executor가 언제 다시 실행할지 결정하며, I/O completion·timer·cancel event가 같은 logical operation에 정확히 연결되어야 한다. 이 PART는 **continuation lifetime, wakeup protocol, backpressure, structured cancellation과 observability**를 하나의 실행 계약으로 다룬다.

---

## CHAPTER 01 · blocking과 suspension은 기다림 상태를 저장하는 위치가 다르다

blocking thread는 kernel/runtime이 해당 thread를 잠들게 하고 call stack을 그대로 유지한다. coroutine suspension은 현재 continuation state를 heap/frame 등에 보존한 뒤 physical thread를 다른 task에 돌릴 수 있다. service time은 같아도 대기 resource 모델이 달라진다.

suspend 가능한 함수가 CPU-bound loop를 오래 실행하면 async라고 해도 executor thread를 독점한다. 반대로 짧은 blocking call을 무조건 async로 바꾸면 state machine과 scheduling overhead만 늘 수 있다.

thread utilization, runnable task, suspension duration을 함께 본다. async 전환의 목표를 concurrency limit과 latency SLO로 명시한다.

---

## CHAPTER 02 · continuation은 중단 지점 이후 실행을 재구성할 수 있는 state다

coroutine이 suspend되면 다음 instruction 위치와 필요한 local value, error handler 정보를 continuation 형태로 보존해야 한다. resume 시 physical thread가 달라도 logical control flow는 이어진다. 따라서 thread-local state를 logical request state로 착각하면 context가 누락될 수 있다.

continuation이 owner object를 strong capture하면 task가 끝날 때까지 object lifetime이 연장된다. cancellation 후 continuation이 queue에 남아 있으면 memory leak이 생긴다.

heap profile에서 coroutine frame과 capture graph를 확인한다. request ID는 thread ID가 아니라 explicit context로 전파한다.

---

## CHAPTER 03 · coroutine frame은 suspension 사이에 살아남아야 하는 local state만 보존한다

compiler/runtime은 suspend point를 기준으로 이후 필요한 변수만 coroutine frame에 저장할 수 있다. 큰 object를 suspend 전 local에 잡고 있으면 실제로는 오랫동안 heap에 retention될 수 있다. source scope보다 lifetime이 길어지는 대표 사례다.

frame allocation이 빈번하면 GC pressure가 증가하고 escape analysis가 최적화하지 못할 수 있다. suspend하지 않는 fast path에서는 frame 생성을 피하는 runtime도 있다.

allocation profiler에서 coroutine frame 수와 retained bytes를 본다. large buffer는 suspension 전에 ownership을 정리하거나 별도 pool로 관리한다.

---

## CHAPTER 04 · stackful coroutine은 별도 execution stack과 scheduling 비용을 가진다

stackful coroutine/fiber는 각 task가 독립 stack을 가져 기존 blocking-style code를 쉽게 suspend할 수 있다. 하지만 stack memory reservation과 guard, switching 비용을 관리해야 한다. 수십만 task에서는 per-fiber stack footprint가 capacity를 결정할 수 있다.

native library가 thread identity나 TLS에 의존하면 fiber migration과 충돌할 수 있다. signal/unwind가 alternate stack을 정확히 이해하는지도 확인해야 한다.

stack high-water mark와 fiber count를 측정한다. stackless model과 trade-off를 workload로 비교한다.

---

## CHAPTER 05 · executor는 ready task를 physical thread에 배치하는 scheduler다

executor는 queue에서 runnable task를 선택해 worker에 실행시키며 worker 수, priority, affinity가 latency와 throughput을 결정한다. application-level scheduler이므로 OS scheduler 위에서 또 하나의 queue를 만든다.

unbounded ready queue는 overload를 숨기고, worker 수를 무작정 늘리면 CPU contention과 cache miss가 증가한다. blocking task가 compute executor를 점유하면 unrelated coroutine도 굶는다.

queue wait, execution time, worker utilization을 분리한다. task enqueue timestamp를 trace에 남겨 off-CPU delay를 설명한다.

---

## CHAPTER 06 · event loop는 handler가 짧고 non-blocking이라는 전제에 의존한다

single event-loop thread는 shared state mutation을 직렬화해 lock을 줄일 수 있지만 한 handler가 오래 실행되면 뒤의 timer·I/O callback 모두 지연된다. loop lag는 CPU utilization만으로 보이지 않을 수 있다.

blocking filesystem call, DNS, large JSON parsing을 event loop에서 직접 수행하면 architecture assumption이 깨진다. worker offload 후 result가 돌아올 때 original request가 이미 cancel됐을 수도 있다.

loop iteration time과 longest handler를 측정한다. threshold를 넘는 callback의 stack을 자동 수집한다.

---

## CHAPTER 07 · reactor와 proactor는 readiness와 completion ownership이 다르다

reactor는 resource가 ready라는 event를 받은 뒤 application이 실제 read/write를 수행하는 모델이고, proactor는 I/O operation을 제출한 뒤 completion을 받는 모델이다. 두 방식을 섞으면 buffer ownership과 error handling이 달라진다.

readiness 뒤 short I/O가 정상일 수 있고 completion 기반 request는 out-of-order로 끝날 수 있다. 한 logical request에서 서로 다른 model을 사용할 때 terminal state를 통합해야 한다.

trace에는 event type을 readiness/completion으로 명확히 구분한다. 같은 callback interface로 감춰도 semantics를 잃지 않는다.

---

## CHAPTER 08 · future와 promise는 아직 완료되지 않은 결과의 identity를 제공한다

future는 result/error/cancel state를 나중에 관찰할 수 있게 하고 promise는 completion 권한을 가진 producer side를 나타낼 수 있다. correctness 조건은 terminal state가 한 번만 결정되고 waiter가 그 결과를 일관되게 관찰하는 것이다.

multiple completion race나 exception swallowing이 있으면 state machine이 깨진다. future chain이 긴 경우 hidden queue와 capture가 memory retention을 만들 수 있다.

completion count와 age를 monitor한다. unresolved future가 shutdown 뒤 남지 않는지 확인한다.

---

## CHAPTER 09 · waker는 blocked task를 다시 runnable하게 만드는 notification protocol이다

async runtime에서 task가 resource를 기다릴 때 poll/suspend하고 event가 오면 waker를 통해 ready queue에 다시 들어간다. wakeup이 state 변경보다 먼저·나중에 어떻게 ordering되는지 명확해야 lost wakeup이 없다.

같은 event가 여러 번 wake를 호출해도 task가 중복 queueing되지 않게 idempotency가 필요할 수 있다. wake storm은 scheduler overhead를 크게 만든다.

wakeup count와 successful progress를 비교한다. state predicate를 다시 확인하는 loop를 사용해 spurious wake를 안전하게 처리한다.

---

## CHAPTER 10 · lost wakeup은 wait 등록과 state 변경 사이 race에서 발생한다

consumer가 predicate를 확인한 뒤 실제 wait registration 전에 producer가 state를 true로 만들고 wake를 보냈다면 consumer가 영원히 잠들 수 있다. correct primitive는 state check와 waiter registration 사이의 atomic relation을 제공해야 한다.

sleep을 추가해 timing을 맞추는 수정은 race probability만 바꾼다. lock, atomic state machine, event registration protocol로 happens-before를 만들어야 한다.

deterministic scheduler로 producer/consumer interleaving을 강제해 regression test를 만든다. rare production hang을 stress 확률에만 의존하지 않는다.

---

## CHAPTER 11 · thundering herd는 하나의 event가 너무 많은 task를 runnable하게 만든다

resource 하나가 available해졌는데 수백 waiter를 모두 깨우면 대부분은 다시 조건을 확인하고 sleep한다. useful work보다 queue operation과 cache traffic이 커질 수 있다.

semaphore permit 수만큼 깨우거나 ownership handoff를 사용하면 wake amplification을 줄일 수 있다. fairness 요구와 throughput을 함께 본다.

wake count/useful completion ratio와 ready queue spike를 측정한다. worker 추가로 herd를 해결하려 하지 않는다.

---

## CHAPTER 12 · async mutex는 thread block 대신 task suspension을 사용한다

async mutex는 lock을 얻지 못한 task를 executor thread를 점유한 채 block하지 않고 suspend시킬 수 있다. 하지만 critical section 안에서 suspend를 허용하면 lock hold time이 external I/O에 의존해 매우 길어질 수 있다.

reentrant assumption과 cancellation도 주의해야 한다. waiter가 cancel됐는데 queue에서 제거되지 않으면 fairness와 memory leak 문제가 생긴다.

lock wait·hold time을 task ID로 trace한다. suspension point가 lock 안에 들어오는지 code review로 검사한다.

---

## CHAPTER 13 · semaphore는 in-flight concurrency의 상한을 명시한다

semaphore permit은 DB connection, remote request, CPU-heavy task 같은 scarce resource에 동시에 들어갈 수 있는 task 수를 제한한다. queue를 무한히 키우는 대신 backpressure를 상위에 전달할 수 있다.

permit release가 exception/cancel path에서 누락되면 capacity가 점진적으로 줄어든다. permit을 획득한 뒤 오래 기다리는 work를 넣으면 starvation이 생길 수 있다.

available permit, wait age, cancel count를 metric으로 둔다. RAII/scoped permit로 release를 자동화한다.

---

## CHAPTER 14 · channel은 task 사이 ownership transfer와 queue capacity를 함께 표현한다

channel로 message를 보내면 shared mutable object에 직접 접근하는 대신 producer→consumer ownership transfer를 명시할 수 있다. capacity가 bounded인지 rendezvous인지에 따라 backpressure와 ordering이 달라진다.

unbounded channel은 producer burst를 memory로 흡수해 overload를 늦게 드러낸다. drop 정책은 message semantics에 따라 data loss가 될 수 있다.

queue length보다 oldest-message age를 함께 본다. producer·consumer rate가 장기적으로 균형인지 확인한다.

---

## CHAPTER 15 · backpressure는 async pipeline의 각 stage rate를 맞춘다

upstream이 downstream capacity보다 빠르면 어느 queue에서든 backlog가 증가한다. 비동기 API는 blocking을 없애도 capacity mismatch를 없애지 않는다. bounded queue, semaphore, demand signal로 rate를 제한해야 한다.

retry가 backpressure를 무시하면 overload가 증폭된다. timeout된 request가 queue에서 계속 실행되면 capacity를 zombie work가 차지한다.

in-flight count와 queue age, cancellation success를 stage별로 측정한다. SLO가 깨지는 지점에서 admission을 줄인다.

---

## CHAPTER 16 · structured concurrency는 child task lifetime을 parent scope에 묶는다

parent operation이 끝날 때 child completion이나 cancellation을 수렴시키면 orphan task와 leaked callback을 줄일 수 있다. error propagation과 sibling cancellation policy가 explicit해진다.

모든 child failure가 전체 parent를 취소해야 하는 것은 아니다. supervisor-like isolation이 필요한 background subtask도 있다. 중요한 것은 policy가 implicit fire-and-forget가 아니라 구조에 드러나는 것이다.

request 종료 후 active child count가 zero로 수렴하는지 검사한다. tracing에서 parent-child span relation을 유지한다.

---

## CHAPTER 17 · cancellation은 cooperative protocol이며 즉시 termination 보장이 아니다

cancel 요청은 task가 안전한 지점에서 관찰하고 cleanup하도록 신호를 준다. 이미 remote side effect나 file write가 시작됐으면 caller가 기다림을 멈췄다고 operation이 취소된 것은 아니다.

non-cancellable commit region, idempotent compensation, late result discard가 필요할 수 있다. cleanup 자체가 suspend되거나 실패할 수 있어 resource release를 별도로 보장한다.

cancel request→observed→cleanup complete 시간을 metric으로 둔다. timeout과 cancel을 같은 상태로 뭉치지 않는다.

---

## CHAPTER 18 · timeout은 caller wait budget과 underlying work lifetime을 분리한다

timeout은 deadline까지 결과를 얻지 못했다는 뜻이지 operation이 실패했다고 확정하는 것이 아니다. request가 server에서 성공한 뒤 response만 늦었을 수 있어 non-idempotent retry는 duplicate side effect를 만든다.

nested timeout마다 새 full budget을 쓰면 end-to-end deadline을 초과한다. absolute deadline이나 remaining budget을 downstream으로 전달한다.

timeout event와 actual completion을 correlation한다. ambiguous outcome을 domain protocol에서 처리한다.

---

## CHAPTER 19 · deadline budget은 여러 async dependency에 end-to-end 시간을 배분한다

request SLO가 500ms라면 DB, cache, network가 각각 500ms를 써서는 안 된다. queue wait와 retry, serialization overhead까지 포함해 remaining budget을 전달해야 한다.

고정 timeout만 사용하면 upstream queueing에 따라 downstream에 실제 남은 시간이 달라진다. expired task를 실행 전에 drop하면 capacity를 보호할 수 있다.

span마다 deadline과 remaining budget을 기록한다. 어느 stage가 budget을 소비했는지 incident에서 바로 볼 수 있게 한다.

---

## CHAPTER 20 · context propagation은 thread가 바뀌어도 logical request identity를 유지한다

trace ID, auth context, locale 같은 request-local state를 thread-local에만 두면 coroutine이 다른 worker로 resume될 때 누락되거나 다른 request 값이 섞일 수 있다. runtime-supported async context나 explicit parameter로 전달해야 한다.

context capture가 큰 object를 포함하면 child task lifetime 동안 memory를 유지할 수 있다. security credential은 필요 범위보다 넓게 propagate하지 않는다.

context key별 provenance를 정의하고 test에서 executor migration을 강제한다. 로그 correlation이 thread ID에 의존하지 않게 한다.

---

## CHAPTER 21 · thread affinity가 필요한 code는 coroutine migration과 충돌할 수 있다

UI toolkit, native library, event loop처럼 특정 thread에서만 호출해야 하는 API가 있다. coroutine이 arbitrary executor에서 resume되면 affinity invariant가 깨진다. dispatcher를 명시해 target thread로 전환해야 한다.

반대로 모든 task를 main thread에 고정하면 CPU-heavy work가 responsiveness를 망친다. state mutation과 heavy compute boundary를 분리한다.

thread/dispatcher identity를 debug assertion으로 검사한다. production trace에서 unintended hop을 찾는다.

---

## CHAPTER 22 · work stealing은 idle worker가 다른 queue의 task를 가져와 load를 균형화한다

per-worker deque는 local push/pop으로 contention을 줄이고 idle worker가 steal해 global imbalance를 완화한다. task가 너무 작으면 steal overhead가 커지고 affinity-sensitive data locality가 깨질 수 있다.

blocking task가 worker를 잡고 있으면 ready compute task가 다른 worker로 몰릴 수 있다. NUMA 환경에서는 remote memory access도 증가한다.

steal rate와 task duration, cache locality를 함께 측정한다. scheduler tuning을 application workload distribution과 연결한다.

---

## CHAPTER 23 · task granularity는 scheduling overhead와 load balance의 trade-off다

작은 task는 load balance와 cancellation responsiveness가 좋지만 queue operation, context capture, wakeup 비용이 상대적으로 커진다. 큰 task는 overhead는 적지만 한 worker를 오래 점유하고 tail imbalance를 만든다.

batching은 tiny message 처리에 유용하지만 batch가 너무 커지면 interactive task가 기다린다. dynamic batch size가 필요할 수 있다.

task duration histogram과 scheduler overhead 비율을 본다. 평균만으로 granularity를 정하지 않는다.

---

## CHAPTER 24 · fairness는 throughput 최적화와 starvation 방지 사이 정책이다

cache-hot task를 계속 재실행하면 throughput이 좋지만 오래 기다린 task가 굶을 수 있다. strict FIFO는 fairness를 높여도 locality와 priority를 잃을 수 있다.

interactive, background, maintenance task가 같은 queue를 공유할 때 class별 weight나 deadline을 고려한다. priority inversion도 async queue에서 발생할 수 있다.

max queue age와 class별 p99를 metric으로 둔다. fairness requirement를 SLO로 명시한다.

---

## CHAPTER 25 · blocking pool은 async executor를 외부 blocking work에서 보호한다

legacy filesystem, DNS, native call처럼 thread를 실제 block하는 operation은 별도 bounded pool에 격리해 main async executor worker를 보존할 수 있다. pool이 unbounded면 thread explosion으로 memory와 scheduler를 소모한다.

blocking pool과 downstream connection pool capacity를 맞춰야 한다. 100 thread가 10 DB connection을 기다리면 queue 위치만 이동한다.

active thread, queue wait, downstream utilization을 함께 본다. library가 내부적으로 blocking하는지도 profile로 확인한다.

---

## CHAPTER 26 · async stack은 logical call chain을 physical stack 밖에서 복원해야 한다

suspension 뒤에는 caller frame이 physical stack에 남지 않을 수 있어 일반 stack trace만 보면 causal chain이 끊긴다. runtime은 continuation metadata나 async stack trace를 통해 logical parent를 연결할 수 있다.

error wrapping이 context를 반복 복사하면 stack이 지나치게 커질 수 있고 performance overhead가 생긴다. release build symbol과 runtime metadata가 정확히 매칭되어야 한다.

exception report에 coroutine/task ID와 await chain을 포함한다. timeout incident에서 어디에서 suspend됐는지 확인할 수 있게 한다.

---

## CHAPTER 27 · deterministic async test는 scheduler와 clock을 통제해 race를 재현한다

sleep 기반 test는 machine speed에 따라 flaky하다. fake clock, test dispatcher, controlled completion을 사용해 event order를 정확히 구성하면 timeout, cancellation, lost wakeup을 반복 재현할 수 있다.

production runtime behavior와 test scheduler semantics가 너무 다르면 false confidence가 생긴다. 동일 state machine을 사용하고 scheduling policy만 통제하는 편이 낫다.

발견된 race의 최소 interleaving을 regression corpus로 남긴다. random stress와 deterministic proof를 역할별로 사용한다.

---

## CHAPTER 28 · FFI callback은 native thread와 runtime scheduler 경계를 넘는다

native library가 임의 thread에서 callback을 호출하면 managed runtime attachment, thread affinity, object lifetime을 맞춰야 한다. callback이 반환된 뒤 native side가 pointer를 계속 보관하는지도 ownership contract다.

runtime shutdown 중 callback이 도착하거나 library unload 뒤 trampoline을 호출하면 crash가 난다. callback registration/unregistration과 in-flight count를 관리한다.

FFI bridge에서 callback generation과 native thread ID를 trace한다. unload/cancel race를 stress test한다.

---

## CHAPTER 29 · queue observability는 length보다 age와 lifecycle state가 중요하다

queue length 100이 healthy인지 overload인지 service time에 따라 다르다. oldest task age, enqueue rate, dequeue rate, cancellation ratio를 함께 봐야 한다. cancelled task가 queue에 남아 있으면 apparent length와 useful work가 다르다.

여러 executor가 이어진 pipeline에서는 한 queue만 보는 것으로 root bottleneck을 찾기 어렵다. logical request ID로 stage를 연결한다.

capacity alert는 age와 deadline violation을 기준으로 설정한다. queue가 늘기 전에 downstream latency가 먼저 악화되는 패턴도 추적한다.

---

## CHAPTER 30 · async contract는 lifetime, scheduling, cancellation, backpressure를 명시한다

async API는 어느 executor에서 callback이 실행되는지, task가 owner보다 오래 살 수 있는지, timeout/cancel 이후 late completion을 어떻게 처리하는지, queue full 시 어떤 정책을 쓰는지 정의해야 한다. `async`라는 keyword만으로는 이 정보가 없다.

structured scope와 bounded queue, explicit context를 사용해 hidden state를 줄인다. FFI나 legacy blocking 경계에는 별도 adapter와 lifecycle을 둔다.

최종 CLEAN 검증은 event order를 뒤집고 owner destruction·timeout·overload를 주입해 **모든 task가 정의된 terminal state로 수렴하며 resource와 queue가 baseline으로 돌아오는지** 확인해야 한다.
