# PART 17 · Thread·process·동시성 — 실행 단위와 공유 상태의 비용을 구분하기

동시성 문제를 다룰 때 “여러 개를 동시에 돌린다”는 한 문장으로 thread, process, async task를 묶으면 잘못된 도구를 선택하기 쉽다. 각 모델은 memory 공유 방식, scheduling, failure isolation, communication 비용이 다르다. Python에서는 interpreter implementation과 GIL의 영향도 고려해야 한다. 핵심은 **작업 특성과 state ownership에 맞는 실행 단위를 선택하는 것**이다.

---

## CHAPTER 01 · concurrency와 parallelism은 같은 목표가 아니다

Concurrency는 여러 작업의 진행 시간을 겹치게 구성하는 것이고 parallelism은 실제로 같은 시점에 여러 계산이 실행되는 것을 뜻한다. 한 thread의 event loop도 I/O task를 concurrent하게 다룰 수 있지만 CPU instruction을 여러 core에서 동시에 실행하는 것은 아니다. 반대로 여러 process는 여러 core에서 계산을 parallel하게 수행할 수 있다.

사용자 request 수천 개가 대부분 network response를 기다린다면 필요한 것은 기다림을 효율적으로 겹치는 concurrency일 수 있다. 대형 숫자 계산이나 image transformation처럼 CPU를 계속 사용하는 작업은 parallel execution이 성능에 더 직접적으로 영향을 줄 수 있다. 문제를 분류하지 않고 “thread 수를 늘리자”라고 결정하면 context switching과 contention만 늘 수 있다.

Latency와 throughput도 분리한다. 작업 하나의 완료 시간을 줄이고 싶은지, 단위 시간에 처리하는 작업 수를 늘리고 싶은지에 따라 전략이 달라진다. Parallel worker를 늘려 throughput은 높아져도 queue와 resource contention 때문에 개별 latency가 악화될 수 있다.

실행 모델 선택은 syntax가 아니라 workload의 wait/compute 비율, shared state, isolation 필요성, resource capacity에서 시작한다.

---

## CHAPTER 02 · thread는 한 process의 주소 공간을 공유하기 때문에 통신은 쉽고 경쟁도 쉽다

같은 process의 thread들은 일반적으로 heap object와 file descriptor 같은 process resource를 공유한다. 한 thread가 만든 mutable object를 다른 thread가 reference할 수 있어 data 전달이 빠르지만, 동시에 같은 state를 수정하면 race condition이 생긴다. 공유 편의와 coordination 비용이 한 묶음이다.

Thread마다 독립적인 call stack과 execution state가 있으며 scheduler가 실행 순서를 바꾼다. 한 줄의 Python source가 한 번에 실행되어 보인다고 해서 여러 줄의 read-modify-write sequence가 atomic하다는 보장은 없다. Interpreter version이나 object operation의 현재 구현 세부사항에 correctness를 맡기지 않는다.

Shared object를 줄이고 immutable message를 queue로 전달하면 lock이 필요한 영역을 줄일 수 있다. 모든 state를 하나의 global dict에 두고 lock 하나로 감싸면 correctness는 단순할 수 있지만 contention 때문에 concurrency 이점이 사라질 수 있다. State partition과 ownership이 중요하다.

Thread는 blocking I/O library를 기존 sync API 그대로 사용해야 할 때 유용할 수 있다. 하지만 thread마다 memory stack과 scheduler overhead가 있으므로 수만 개를 만드는 대신 pool과 queue로 worker 수를 제한하는 구조가 일반적이다.

---

## CHAPTER 03 · race condition은 실행 순서에 따라 결과가 달라지는 논리 오류다

두 thread가 counter를 증가시키는 코드를 생각해 보자. 논리적으로 `read → add → write`의 세 단계라면 둘 다 같은 이전 값을 읽고 각각 결과를 쓴 뒤 증가 하나가 사라질 수 있다. Source에서 `counter += 1` 한 줄로 보이는지는 핵심이 아니다. 공유 state의 read-modify-write가 하나의 atomic operation으로 보호되는지가 중요하다.

Race를 찾기 어려운 이유는 특정 scheduling interleaving에서만 나타날 수 있기 때문이다. Debugger나 log를 추가하면 timing이 바뀌어 증상이 사라지는 heisenbug도 생긴다. Stress test와 deterministic synchronization point를 사용해 위험한 순서를 강제로 만들 수 있다.

Race의 해결책이 항상 lock은 아니다. Single writer, immutable state, actor/message passing, database atomic update처럼 공유 자체를 줄이는 구조가 더 단순할 수 있다. Lock은 이미 공유되는 상태를 coordination하는 한 도구다.

Correctness를 설명할 때 “실제로는 거의 동시에 안 들어온다”가 아니라 어떤 interleaving에서도 invariant가 지켜지는지 본다. 낮은 확률의 race도 트래픽이 커지면 반복적으로 발생할 수 있다.

---

## CHAPTER 04 · lock은 critical section을 보호하지만 deadlock과 contention을 만든다

Lock을 획득한 thread만 shared state를 수정하게 하면 특정 operation을 mutual exclusion으로 묶을 수 있다. Critical section은 invariant를 유지하는 데 필요한 최소 범위여야 한다. Lock을 잡은 채 network call이나 긴 계산을 수행하면 다른 thread가 불필요하게 대기한다.

여러 lock을 서로 다른 순서로 획득하면 deadlock이 발생할 수 있다. Thread A가 lock X를 가진 채 Y를 기다리고 Thread B가 Y를 가진 채 X를 기다리면 둘 다 진행하지 못한다. Global lock ordering을 정하거나 필요한 state를 한 lock 아래 모으거나 nested lock 자체를 줄이는 설계가 필요하다.

Timeout lock acquisition은 deadlock을 자동 해결하지 않는다. 실패 후 어떤 상태로 복구할지 추가 정책이 필요하고, 반복 retry가 livelock을 만들 수 있다. Lock contention metric과 wait time을 관찰하면 concurrency가 실제 throughput을 제한하는지 확인할 수 있다.

Context manager로 lock lifetime을 lexical scope에 묶으면 exception 발생 시 release 누락을 줄일 수 있다. 하지만 syntax 안전성과 architecture 안전성은 다르므로 lock hierarchy와 ownership을 별도로 설계한다.

---

## CHAPTER 05 · condition과 queue는 “상태가 될 때까지 기다림”을 busy loop에서 분리한다

Shared flag를 `while not ready: pass`처럼 계속 읽으면 CPU를 낭비하고 memory visibility semantics도 어렵게 만든다. Condition variable이나 queue는 state가 변경될 때 waiter를 깨우는 coordination mechanism을 제공한다. Producer/consumer 관계에서는 thread-safe queue가 데이터와 synchronization을 함께 표현할 수 있다.

Queue에 maximum size를 두면 producer가 consumer보다 빠를 때 memory가 무제한 증가하는 것을 막는다. Queue full에서 block할지 drop할지, priority를 둘지, shutdown sentinel을 사용할지 정책을 정한다. Work item이 성공적으로 완료됐는지 acknowledgement가 필요한 경우도 있다.

Condition을 사용할 때는 wake-up 이후 조건을 다시 검사하는 loop가 일반적이다. 깨어났다는 사실과 원하는 predicate가 여전히 참이라는 사실은 같지 않을 수 있다. 여러 waiter와 spurious wakeup 가능성을 고려해 shared state predicate를 기준으로 진행한다.

Low-level synchronization primitive를 직접 조합하기 전에 더 높은 수준의 queue, executor, concurrent collection이 문제를 표현하는지 확인한다. 직접 lock protocol을 만들수록 검증해야 할 interleaving이 늘어난다.

---

## CHAPTER 06 · GIL은 Python code 실행과 외부 native 작업을 구분해서 이해한다

일반 CPython 실행에서는 global interpreter lock이 여러 thread가 동시에 Python bytecode를 실행하는 방식에 제약을 준다. 따라서 순수 Python CPU-bound loop에 thread를 늘렸다고 core 수만큼 성능이 증가한다고 기대하면 안 된다. 그러나 thread가 I/O를 기다리거나 일부 native extension이 lock을 해제하는 동안 다른 thread가 실행될 수 있다.

GIL의 정확한 동작과 Python version의 변화는 implementation detail이 포함되므로 현재 공식 문서를 확인해야 한다. 중요한 설계 원칙은 특정 interpreter 우연에 correctness를 의존하지 않고, CPU workload의 실제 parallelism은 benchmark로 확인하는 것이다.

NumPy 같은 native library가 내부적으로 parallel computation을 수행한다면 Python thread와 별개의 worker를 사용할 수도 있다. Application thread pool과 library 내부 thread pool이 겹쳐 oversubscription이 생기면 core보다 훨씬 많은 runnable thread가 경쟁할 수 있다. Environment configuration과 profiler로 실제 실행을 확인한다.

CPU-heavy pure Python 작업을 여러 core에 분산하려면 process 기반 parallelism이 더 적합할 수 있지만 serialization과 process startup 비용이 생긴다. 작업 크기가 충분히 커야 이 비용을 상쇄한다.

---

## CHAPTER 07 · process는 memory isolation을 얻는 대신 통신과 startup 비용을 지불한다

각 process는 일반적으로 별도의 주소 공간을 가지므로 한 process의 임의 memory corruption이나 mutable object 변경이 다른 process와 직접 공유되지 않는다. 이 isolation은 안정성과 CPU parallelism에 유리하지만 데이터를 주고받으려면 pipe, queue, shared memory, socket 같은 IPC가 필요하다.

Process pool에 큰 Python object를 자주 보내면 serialization과 memory copy가 실제 계산보다 비쌀 수 있다. Worker가 사용할 read-only data를 startup 때 로드하거나 shared memory representation을 사용해 communication volume을 줄일 수 있다. 하지만 shared memory를 사용하면 다시 synchronization 문제 일부가 돌아온다.

Process startup model은 운영체제와 Python configuration에 따라 fork/spawn 계열 차이가 있을 수 있다. Fork 후 이미 생성된 thread/lock/network connection을 안전하게 재사용할 수 있는지 주의해야 한다. Portable code는 공식 multiprocessing contract를 기준으로 작성한다.

Worker process가 crash했을 때 parent가 작업을 retry할지 실패시킬지, partial output이 남았는지 관리한다. Process isolation은 failure propagation을 없애는 것이 아니라 경계를 명확하게 만드는 것이다.

---

## CHAPTER 08 · executor는 task 제출과 worker 관리 사이에 abstraction을 둔다

Thread pool과 process pool executor는 application이 직접 worker lifecycle을 세밀하게 관리하지 않고 callable task를 제출하고 future result를 받을 수 있게 한다. 이 abstraction을 사용하면 같은 producer logic에서 실행 자원을 바꾸기 쉬울 수 있다.

Future는 완료, 결과, exception을 나타내며 기다리거나 completion callback을 붙일 수 있다. Task exception을 result에서 확인하지 않고 버리면 background failure를 놓칠 수 있다. 모든 제출 task의 ownership과 error observation 경로를 갖는다.

Pool size는 많을수록 좋은 값이 아니다. Thread pool은 downstream connection pool과 blocking ratio를, process pool은 CPU core와 memory를 고려한다. Worker 수를 늘린 benchmark에서 throughput, p95 latency, memory를 함께 본다.

Executor shutdown 시 queued task를 기다릴지 취소할지 결정한다. Application 종료 중에도 신규 task가 들어오지 않도록 submission boundary를 닫고, 진행 중인 작업의 idempotency와 cleanup을 처리한다.

---

## CHAPTER 09 · shared memory와 IPC는 데이터 ownership을 다시 설계하게 한다

Process 사이에서 큰 array를 매 작업마다 serialize하는 비용이 크면 shared memory를 고려할 수 있다. 여러 process가 같은 memory region을 보면 복사 비용은 줄지만 누가 어느 구간을 쓸 수 있는지, update visibility와 synchronization을 어떻게 할지 규칙이 필요하다.

Read-only snapshot은 가장 단순하다. Parent가 데이터를 준비한 뒤 worker들이 읽기만 하면 coordination이 줄어든다. Mutable shared memory는 lock, atomic primitive, partition ownership이 필요하고 process crash 뒤 consistency를 처리해야 한다.

Message passing은 데이터 복사가 있을 수 있지만 ownership transfer를 명시적으로 만들기 쉽다. Worker가 input message를 받고 독립적으로 계산한 result를 반환하면 shared mutable state가 줄어든다. Distributed system으로 확장될 때도 같은 패턴을 사용한다.

성능 때문에 shared memory를 도입하기 전에 profile로 serialization이 실제 bottleneck인지 확인한다. Complexity가 크게 늘어나는 최적화는 측정된 비용이 있을 때만 정당화한다.

---

## CHAPTER 10 · 실행 모델 선택표는 I/O·CPU·state·failure 네 축으로 만든다

Async task는 많은 I/O wait를 적은 thread로 겹치는 데 강하고, thread pool은 blocking API와 비교적 적은 worker로 I/O를 다루기 편하며, process pool은 CPU-bound 작업을 여러 core로 분리하는 데 적합할 수 있다. 하지만 실제 선택은 library와 workload에 따라 달라진다.

Shared mutable state가 많으면 thread concurrency의 synchronization 비용이 커지고, process는 serialization 비용이 커진다. Failure isolation이 중요하면 process boundary가 장점일 수 있고, 매우 낮은 communication latency가 필요하면 shared address space가 유리할 수 있다. 한 기준으로 정답을 고를 수 없다.

혼합 모델도 흔하다. Async web server가 blocking legacy library를 작은 thread pool에 넘기고 CPU-heavy report generation을 process pool이나 별도 worker service로 보낼 수 있다. 이때 각 pool의 queue와 timeout, cancellation, observability를 별도로 관리한다.

동시성 문제를 만났을 때 먼저 묻는다. **작업이 대부분 기다리는가 계산하는가, 어떤 state를 공유하는가, 작업 실패를 어느 수준까지 격리해야 하는가, 데이터 이동 비용이 얼마인가.** 이 질문에 답한 뒤 실행 primitive를 선택하면 syntax 중심의 시행착오를 줄일 수 있다.