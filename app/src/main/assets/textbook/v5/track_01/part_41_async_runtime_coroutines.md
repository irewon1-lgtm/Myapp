# PART 41 · Async Runtime and Coroutines — suspension, executors, wakeups, cancellation

비동기 프로그램의 핵심은 `thread를 많이 만든다`가 아니라 **기다리는 작업을 실행 자원에서 분리하고, 완료 사건이 왔을 때 정확한 continuation을 다시 runnable하게 만드는 것**이다. Coroutine·future·promise·executor·event loop는 이 상태 전이를 서로 다른 API로 표현한다. 정확성과 성능은 task state, wakeup ownership, cancellation, backpressure, fairness, context propagation을 얼마나 명시적으로 관리하는지에 달려 있다.

## CHAPTER 01 · blocking과 suspension은 실행 자원을 점유하는 방식이 다르다

Blocking call은 현재 thread가 completion을 기다리는 동안 scheduler 관점에서 sleep 상태로 들어갈 수 있지만 thread stack과 runtime metadata는 유지된다. Coroutine suspension은 logical task state를 continuation/frame에 저장하고 실행 thread를 다른 task에 돌려줄 수 있다. 따라서 `기다림이 있다`는 점은 같아도 thread-per-request와 coroutine-per-request의 resource model은 다르다. 비교할 때 task 수가 아니라 stack memory, runnable thread 수, context switch, wakeup queue, scheduler contention을 본다.

## CHAPTER 02 · continuation은 중단된 계산을 다시 시작하기 위한 최소 상태다

Suspension point 뒤의 실행을 재개하려면 instruction 위치뿐 아니라 살아 있는 local value, exception/cancellation state, context를 보존해야 한다. Stackless coroutine implementation은 compiler가 함수의 control flow를 state machine으로 바꾸고 필요한 local을 heap/managed frame에 저장할 수 있다. Resume은 새 함수를 처음부터 실행하는 것이 아니라 저장된 state label에서 continuation을 이어 간다. 이 모델을 이해하면 `await가 thread를 멈춘다`는 오해를 피할 수 있다.

## CHAPTER 03 · stackless coroutine frame은 필요한 local lifetime을 await 경계 너머로 확장한다

Await 이전에 생성된 local이 이후에도 필요하면 compiler는 그 값을 coroutine frame에 저장해야 한다. 이로 인해 원래 stack allocation 가능해 보이던 값이 longer-lived allocation이 되거나 object retention을 만들 수 있다. Large buffer나 UI object를 coroutine local로 잡고 장시간 suspend하면 memory pressure가 커질 수 있다. Async memory profile은 allocation count뿐 아니라 suspended frame이 무엇을 retain하는지 확인해야 한다.

## CHAPTER 04 · stackful coroutine은 독립 stack을 유지하는 대신 switch 비용과 memory policy가 다르다

Fiber/green-thread 계열 stackful coroutine은 ordinary call stack을 그대로 보존할 수 있어 synchronous-looking code를 쉽게 suspend한다. 대신 각 logical task에 stack storage가 필요하고 stack growth/shrink, guard page, switch mechanism이 runtime design에 포함된다. Stackless와 stackful은 API syntax보다 runtime state representation이 다르다. Workload의 task 수, call depth, FFI 요구, debugging tooling이 선택에 영향을 준다.

## CHAPTER 05 · executor는 runnable task를 실제 thread에 배치하는 scheduler다

Coroutine이 많아도 CPU에서 동시에 실행되는 수는 executor가 가진 worker thread와 CPU capacity에 제한된다. Executor queue가 길어지면 task가 ready 상태인데도 실행을 못 하는 scheduling latency가 생긴다. CPU-bound task를 I/O-oriented executor에 오래 실행시키면 다른 completion handler를 막을 수 있다. `async 함수`라는 표면 문법보다 어떤 executor에서 어느 정도의 work를 수행하는지가 운영 성능을 결정한다.

## CHAPTER 06 · event loop는 readiness/completion event와 runnable callback을 직렬화하는 정책이다

Event loop는 file descriptor readiness, timer expiry, posted task 같은 event를 받아 callback/continuation을 실행한다. Single-thread loop에서는 하나의 callback이 오래 CPU를 사용하면 다른 event 처리도 늦어진다. 따라서 nonblocking I/O를 사용해도 callback body가 blocking/CPU-heavy하면 tail latency가 폭발할 수 있다. Event loop lag를 별도 metric으로 측정하면 network latency와 user-space scheduling delay를 분리할 수 있다.

## CHAPTER 07 · reactor와 proactor는 event가 의미하는 완료 단계가 다르다

Reactor model은 `이 descriptor에서 read할 수 있음` 같은 readiness를 알려 주고 application이 실제 read/write를 수행한다. Proactor/completion model은 제출한 I/O operation의 완료를 알려 준다. Epoll과 io_uring 같은 mechanism 차이가 여기 연결된다. Runtime abstraction이 둘을 같은 `await read()`로 감싸도 cancellation, partial completion, buffer ownership, retry semantics는 backend마다 다를 수 있다.

## CHAPTER 08 · future/promise는 아직 없는 결과와 completion authority를 분리한다

Future는 결과를 관찰하거나 await하는 handle이고 promise/completer는 결과를 한 번 결정하는 authority를 가진다. 이 둘을 분리하면 consumer가 임의로 completion을 위조하지 못하게 할 수 있다. State는 pending→success/failure/cancelled 같은 terminal transition을 가지며 terminal state가 여러 번 결정되지 않도록 atomicity가 필요하다. Multiple waiter가 있을 때 wakeup fan-out 비용도 고려한다.

## CHAPTER 09 · waker는 sleeping task를 runnable queue로 다시 넣는 권한이다

Low-level async runtime에서는 pending operation이 현재 task를 나중에 깨울 수 있도록 waker/callback token을 등록한다. Wakeup은 task를 즉시 실행한다는 뜻이 아니라 executor queue에 runnable 상태로 올리는 사건일 수 있다. Duplicate wakeup, wake-after-cancel, lost wakeup을 막으려면 task state와 queue insertion을 원자적으로 조정해야 한다. Wakeup protocol은 lock-free queue와 memory ordering 문제로 이어진다.

## CHAPTER 10 · lost wakeup은 상태 확인과 waiter 등록 사이 race에서 생긴다

Task가 `조건이 아직 false`라고 확인한 뒤 waiter 등록 전에 producer가 조건을 true로 만들고 notification을 보내면, consumer는 notification을 놓친 채 영원히 sleep할 수 있다. Condition variable과 async waker 모두 state check와 registration protocol을 설계해야 한다. 일반적인 해결은 lock/atomic state machine 아래에서 check-register-sleep 순서를 연결하고, wakeup 후 조건을 다시 확인하는 것이다.

## CHAPTER 11 · thundering herd는 하나의 event가 너무 많은 waiter를 깨우는 문제다

같은 socket, lock, queue condition에 수백 task가 기다리다가 하나의 resource availability로 모두 wake되면 대부분은 다시 실패하고 sleep한다. Scheduler queue와 cache를 불필요하게 흔든다. Exclusive waiter, semaphore permit, work stealing, targeted wakeup 같은 policy로 실제 처리 가능한 수만 깨우는 것이 중요하다. Wakeup 수는 throughput metric과 별도로 수집할 가치가 있다.

## CHAPTER 12 · async mutex는 thread를 block하지 않아도 contention을 제거하지 않는다

Async mutex는 lock을 얻지 못한 coroutine을 suspend시켜 worker thread를 다른 task에 사용할 수 있게 한다. 그러나 critical section 직렬화 자체는 그대로이므로 high contention에서는 waiter queue와 handoff latency가 늘어난다. Lock을 await 경계 너머로 유지하면 외부 I/O 때문에 critical section이 길어질 수 있다. Shared state partitioning이나 immutable message passing이 더 나은 경우가 있다.

## CHAPTER 13 · semaphore는 concurrency budget을 resource로 표현한다

Semaphore permit 수를 DB connection, network request, CPU-heavy job처럼 동시에 허용할 work 수와 맞출 수 있다. Permit acquisition을 awaitable하게 만들면 queue가 자연스럽게 backpressure 역할을 한다. 하지만 queue가 무한하면 overload를 숨길 뿐이므로 maximum waiters, timeout, rejection policy를 함께 둔다. Permit leak은 capacity가 점점 줄어드는 resource leak과 같다.

## CHAPTER 14 · channel은 data transfer와 synchronization을 하나의 protocol로 묶는다

Bounded channel은 producer가 capacity를 초과하면 suspend/block되어 consumer 처리율에 맞춰 속도를 제한한다. Unbounded channel은 producer burst를 memory queue로 흡수하지만 overload에서 OOM과 stale work를 만들 수 있다. Close semantics, multiple producer/consumer, message ordering, cancellation 후 ownership을 명시해야 한다. Channel은 단순 thread-safe queue가 아니라 lifecycle과 backpressure contract다.

## CHAPTER 15 · backpressure는 downstream capacity를 upstream admission에 전달한다

Async pipeline에서 각 stage가 독립적으로 task를 생성하면 느린 stage 앞에 queue가 누적된다. Queue length가 늘어나는 동안 request latency와 memory가 함께 증가한다. Bounded queue, semaphore, demand signal, load shedding으로 downstream saturation을 upstream에 전달해야 한다. P10 queueing 모델과 결합해 arrival rate가 service capacity를 넘는 시점의 behavior를 설계한다.

## CHAPTER 16 · structured concurrency는 task tree로 lifetime과 failure propagation을 제한한다

Parent operation이 child task를 생성했는데 child가 parent보다 임의로 오래 살아남으면 resource lifetime과 error ownership이 비국소적으로 변한다. Structured concurrency는 child를 lexical/logical scope 아래 두고 parent completion 전에 child completion/cancellation을 정리하는 모델이다. 이 구조는 task leak, orphan error, cancellation 누락을 줄인다. Background daemon처럼 실제로 더 긴 lifetime이 필요한 task는 별도 supervisor owner 아래 둔다.

## CHAPTER 17 · cancellation은 cooperative state transition일 수 있다

많은 runtime에서 cancel 요청이 task를 즉시 강제 종료하지 않는다. Task가 cancellation point를 만나거나 token을 확인하고 cleanup 후 terminal state로 전환한다. CPU-bound loop가 check하지 않으면 cancel latency가 길어질 수 있다. 반대로 임의 instruction에서 강제 중단하면 lock/resource invariant를 깨뜨릴 수 있다. Cancellation latency와 cleanup guarantee를 함께 정의한다.

## CHAPTER 18 · timeout과 cancellation은 같은 사건이 아니다

Timeout은 caller가 정한 deadline을 넘겼다는 정책이고 cancellation은 더 이상 결과가 필요하지 않다는 control signal이다. Timeout 후 underlying I/O가 실제로 취소되지 않으면 resource와 remote side effect는 계속 진행될 수 있다. Retry가 동시에 시작되면 duplicate work가 발생한다. Timeout handler는 child cancellation, idempotency, late completion disposal을 함께 설계해야 한다.

## CHAPTER 19 · deadline은 여러 nested call에 남은 시간 budget을 전달한다

각 계층이 독립적으로 5초 timeout을 잡으면 세 단계 chain이 15초 가까이 걸릴 수 있다. Absolute deadline이나 remaining budget을 하위 operation에 전파하면 전체 latency 목표를 지킬 수 있다. Queue에서 소비한 시간도 budget에서 빼야 한다. Monotonic clock을 사용해 wall-clock 조정에 영향받지 않도록 한다. P16 timekeeping과 P10 deadline 모델이 async runtime에서 만난다.

## CHAPTER 20 · context propagation은 request identity를 task migration과 분리한다

Coroutine은 한 thread에서 suspend하고 다른 worker에서 resume할 수 있으므로 thread-local만으로 trace id, auth context, locale를 전달하면 깨질 수 있다. Runtime context/coroutine-local storage는 logical task에 metadata를 묶는다. Context object를 무제한 복사하면 allocation과 retention 비용이 생긴다. Security-sensitive identity는 mutable global보다 explicit propagation과 immutable snapshot이 안전하다.

## CHAPTER 21 · thread affinity가 필요한 resource는 arbitrary executor migration과 충돌한다

UI toolkit, GPU context, 특정 native library는 특정 thread에서만 접근하도록 요구할 수 있다. Coroutine이 arbitrary worker에서 resume하면 이런 contract를 위반한다. Dispatcher/executor를 명시적으로 전환하고, affinity-bound object의 lifetime과 callback thread를 문서화한다. `async라서 어느 thread든 상관없다`는 가정은 틀릴 수 있다.

## CHAPTER 22 · work stealing은 idle worker가 다른 queue의 task를 가져오는 load-balancing 방식이다

Worker마다 local deque를 두면 자기 task를 cache-local하게 처리하면서 idle worker가 다른 worker의 task를 steal할 수 있다. Steal frequency가 높으면 contention과 cache migration이 늘고, 너무 낮으면 load imbalance가 커진다. Long-running task와 many-small-task workload는 적합한 grain size가 다르다. NUMA-aware scheduler에서는 cross-node stealing 비용도 고려한다.

## CHAPTER 23 · task granularity가 너무 작으면 scheduling overhead가 실제 work보다 커진다

수 microsecond 계산을 매번 별도 async task로 만들면 allocation, queue push/pop, wakeup, context capture 비용이 지배할 수 있다. Batching과 inline execution threshold로 grain size를 키울 수 있다. 반대로 너무 큰 task는 fairness와 cancellation latency를 악화시킨다. Task당 useful work와 scheduler overhead를 측정해 적정 크기를 찾는다.

## CHAPTER 24 · fairness는 runnable task가 무기한 굶지 않도록 하는 policy다

LIFO queue는 cache locality와 throughput에 유리할 수 있지만 오래 기다린 task가 계속 뒤로 밀릴 수 있다. FIFO는 fairness를 개선하지만 locality가 나빠질 수 있다. Priority task를 도입하면 priority inversion과 starvation 방지 정책이 필요하다. Runtime scheduler 선택은 평균 throughput뿐 아니라 p99 scheduling latency와 starvation bound를 봐야 한다.

## CHAPTER 25 · blocking escape hatch는 worker pool starvation을 만들 수 있다

Async code 안에서 legacy blocking API를 호출해야 할 때 dedicated blocking pool로 보내는 방식이 흔하다. 그러나 blocking pool이 무제한 thread를 만들면 overload에서 thread explosion이 발생하고, 너무 작으면 queue가 쌓인다. Main async worker에서 직접 block하면 completion processing 전체를 막을 수 있다. Blocking section의 count, duration, pool saturation을 metric으로 둔다.

## CHAPTER 26 · async stack trace는 physical call stack과 logical await chain이 다를 수 있다

Suspension 시 physical stack frame이 풀리기 때문에 crash 시 native/thread stack만 보면 원래 caller chain이 사라질 수 있다. Runtime은 continuation metadata를 이용해 logical async stack을 재구성할 수 있다. Debugger와 production error reporter가 어떤 수준까지 async causal chain을 보존하는지 확인한다. Trace/span id와 결합하면 task 간 causal relation을 더 정확히 복원할 수 있다.

## CHAPTER 27 · deterministic async test는 virtual time과 controlled executor가 필요하다

실제 sleep과 wall-clock에 의존한 test는 느리고 flaky하다. Timer source와 executor를 injection해 virtual clock을 전진시키고 runnable task 순서를 제어하면 timeout/cancellation race를 재현하기 쉬워진다. 모든 concurrency interleaving을 커버할 수는 없지만 known race window를 deterministic하게 regression test로 남길 수 있다. Randomized scheduler와 stress test를 별도로 사용한다.

## CHAPTER 28 · async FFI callback은 foreign runtime에서 completion이 돌아오는 thread를 통제해야 한다

Native library가 callback을 arbitrary thread에서 호출하면 managed runtime object를 바로 만질 수 없는 경우가 있다. Callback은 data를 안전한 queue에 전달하고 correct executor로 marshal해야 한다. User-data pointer lifetime, callback unregister, native operation cancellation을 함께 설계한다. FFI ownership P40과 async task lifetime이 이 경계에서 결합된다.

## CHAPTER 29 · observability는 queue time과 execution time을 분리해야 한다

Task가 생성된 시점부터 완료까지 100ms여도 실제 CPU execution은 2ms이고 executor queue에서 80ms 기다렸을 수 있다. Span을 enqueue→dequeue→start→suspend→resume→complete 단계로 나누면 scheduler delay와 external wait를 구분할 수 있다. Queue depth, runnable count, wakeups, worker utilization, event-loop lag를 함께 기록한다. `async가 느리다`를 CPU issue로 오진하지 않는다.

## CHAPTER 30 · async runtime 계약은 task state·owner·executor·deadline·cancellation을 모두 명시한다

Production async API는 어떤 executor에서 실행되는지, await가 무엇을 suspend하는지, task를 누가 소유하고 join하는지, cancellation이 어디까지 전파되는지, timeout 뒤 underlying work가 남는지, context가 어떻게 전달되는지를 문서화한다. Queue와 concurrency budget을 bounded하게 만들고 overload behavior를 테스트한다. Correctness test는 late completion·double wakeup·cancel race·executor shutdown을 포함하고, performance test는 scheduler overhead와 queue latency를 별도로 측정한다.