# PART 16 · Asyncio와 event loop — 기다림을 중첩해 동시성을 만드는 실행 모델

네트워크 요청이나 timer처럼 CPU가 계산하지 않고 결과를 기다리는 시간이 길다면 한 작업이 기다리는 동안 다른 작업을 진행할 수 있다. Python의 `async`/`await`는 이 협력적 동시성을 표현하는 언어 도구다. 핵심은 함수를 빠르게 만드는 마법이 아니라 **task가 어디에서 실행권을 양보하고, event loop가 어떤 준비된 작업을 다시 진행시키는가**를 이해하는 것이다.

---

## CHAPTER 01 · coroutine function 호출은 완료 결과가 아니라 실행 가능한 coroutine을 만든다

`async def`로 정의된 함수를 호출하면 일반 함수처럼 body 전체가 즉시 실행되어 최종 값을 반환하는 것이 아니라 coroutine object를 얻는다. 이 coroutine을 event loop가 실행하거나 다른 coroutine이 `await`해야 실제 코드가 진행된다. 단순히 `async`를 붙였다는 이유로 background thread가 자동 생성되는 것은 아니다.

이 차이는 누락된 `await` 버그를 설명한다. 비동기 함수를 호출해 coroutine object만 만들고 실행 경로에 연결하지 않으면 기대한 network request나 저장 작업이 실제로 수행되지 않을 수 있다. Static analysis와 runtime warning이 이런 실수를 잡는 데 도움이 된다.

Coroutine의 local state는 suspension 지점 사이에서 보존된다. `await`에서 다른 task에게 실행권을 넘기고 나중에 같은 frame 상태로 재개된다. Generator의 suspend/resume 모델과 연결해서 보면 이해하기 쉽지만 coroutine은 값 sequence 생산보다 비동기 operation coordination에 초점을 둔다.

API를 읽을 때 함수가 sync인지 async인지 구분하는 이유는 호출자의 실행 모델이 달라지기 때문이다. Async 함수는 결과를 얻기 위해 await가 필요하고, 호출 stack 전체가 async boundary를 따라 올라갈 수 있다.

---

## CHAPTER 02 · `await`는 “기다리며 멈춤”이 아니라 event loop에 제어권을 양보하는 지점이다

`await operation`에서 operation이 아직 완료되지 않았다면 현재 task는 suspended 상태가 되고 event loop가 다른 runnable task를 실행할 수 있다. Operation이 준비되면 task를 다시 schedule해 다음 코드가 이어진다. 따라서 한 thread에서도 여러 I/O-bound task의 기다림 시간을 겹칠 수 있다.

모든 표현식 앞에 await를 붙일 수 있는 것은 아니다. Awaitable protocol을 제공하는 coroutine, task, future 같은 객체가 대상이다. CPU 계산 loop 안에는 자연스러운 suspension point가 없기 때문에 큰 계산이 event loop thread를 오래 점유하면 다른 task도 진행하지 못한다.

이 때문에 async는 병렬 CPU 실행과 다르다. 네트워크 response를 기다리는 동안 다른 request를 처리하는 데 강하지만 이미지 encoding 같은 CPU-heavy operation을 같은 event loop에서 오래 수행하면 latency가 악화된다. CPU work는 필요에 따라 process/thread executor 같은 다른 실행 자원으로 넘긴다.

Await point는 concurrency interleaving이 발생할 수 있는 경계이기도 하다. Await 이전에 읽은 shared state가 재개 시점에도 같다고 가정하면 다른 task가 그 사이 값을 바꿨을 수 있다. 협력적 동시성에서도 state race를 고려해야 한다.

---

## CHAPTER 03 · task는 coroutine의 진행 상태와 scheduling을 event loop에 등록한다

Coroutine을 순서대로 `await`하면 각 operation이 끝난 뒤 다음 operation을 시작한다. 서로 독립적인 I/O를 겹쳐 실행하려면 여러 coroutine을 task로 schedule해 event loop가 진행 상태를 관리하게 할 수 있다. Task는 coroutine의 result, exception, cancellation 상태를 추적하는 handle 역할을 한다.

```python
first = asyncio.create_task(fetch_a())
second = asyncio.create_task(fetch_b())
a = await first
b = await second
```

두 request가 독립이라면 첫 request를 기다리는 동안 두 번째도 진행될 수 있다. 하지만 무조건 task를 많이 만들면 remote service와 local resource에 과도한 동시 요청을 보낼 수 있다. Concurrency limit과 queue를 사용해 최대 in-flight 수를 통제한다.

Task reference를 버리고 background로만 실행시키면 exception을 누가 관찰할지 문제가 생긴다. Fire-and-forget이 정말 필요한 경우에도 task registry와 error reporting, shutdown cleanup을 갖는다. Request handler가 끝난 뒤 orphan task가 shared state를 수정하는 구조는 lifecycle을 이해하기 어렵게 한다.

Task 생성은 execution ownership 결정이다. 누가 완료를 기다리고, 누가 cancellation하며, 실패를 누가 처리할지 함께 정해야 한다.

---

## CHAPTER 04 · `gather`와 structured concurrency는 자식 task의 lifetime을 부모 범위에 묶는다

여러 비동기 작업을 병렬로 진행한 뒤 모두의 결과가 필요하다면 task group이나 gather 계열 abstraction을 사용할 수 있다. 중요한 질문은 한 child가 실패했을 때 다른 child를 계속 실행할지 취소할지, 여러 exception을 어떻게 전달할지다. 단순히 “동시에 실행”만 정하면 failure semantics가 비어 있다.

Structured concurrency의 핵심 아이디어는 부모 scope가 끝날 때 시작한 child task도 완료·취소·실패 상태가 정리되어 있어야 한다는 것이다. Background task가 scope 밖으로 무제한 새어나가는 것을 줄이고 lifetime을 call tree와 연결한다.

예를 들어 주문 화면을 위해 가격, 재고, 배송 예상치를 동시에 조회하는데 가격 조회가 필수이고 추천 정보는 선택적이라면 모든 child를 같은 실패 정책으로 묶는 것이 맞지 않을 수 있다. Critical dependency와 best-effort dependency를 별도 task group으로 나눌 수 있다.

Concurrency abstraction을 선택할 때 result ordering도 확인한다. 완료 순서와 input order가 다를 수 있으며 API가 어느 순서로 결과를 제공하는지가 downstream semantics에 영향을 준다.

---

## CHAPTER 05 · cancellation은 exception과 비슷하지만 cooperative cleanup protocol이다

Async task를 취소하면 실행이 강제로 어떤 machine instruction에서 즉시 중단되는 것이 아니라 cancellation이 task에 전달되고 적절한 suspension point에서 관찰될 수 있다. 따라서 긴 CPU loop가 await 없이 실행되면 cancellation 반응도 늦어진다.

Cancellation을 일반 오류처럼 넓은 `except`로 삼키면 상위 scope가 작업을 종료하려 해도 계속 실행될 수 있다. Cleanup을 위해 cancellation을 잡더라도 resource를 정리한 뒤 다시 전파해야 하는 경우가 많다. Library의 cancellation exception hierarchy와 권장 처리 방식을 정확히 확인한다.

취소 시 부분 side effect도 중요하다. File write 중인지, transaction이 열려 있는지, remote request가 이미 전송됐는지에 따라 정리와 idempotency가 필요하다. Cancellation-safe 함수는 단순히 빨리 멈추는 것이 아니라 **멈춘 뒤에도 invariant를 유지**해야 한다.

Shutdown에서는 새 task 생성을 중단하고 기존 task에 cancellation을 전달한 뒤 일정 시간 cleanup을 기다리는 순서를 설계한다. 즉시 process를 종료하면 buffered log, transaction, queue acknowledgement가 유실될 수 있다.

---

## CHAPTER 06 · timeout은 local clock 제한이지 remote operation 취소 보장이 아니다

`await`에 timeout을 걸어 local task가 일정 시간 이상 기다리지 않게 할 수 있다. 하지만 local timeout이 발생했다고 remote server가 작업을 중단했다는 보장은 없다. Payment request가 server에서 완료됐지만 response가 늦어 client timeout이 난 경우 재시도하면 중복 side effect가 생길 수 있다.

따라서 timeout은 retry와 함께 idempotency contract를 요구할 수 있다. Request ID나 idempotency key를 보내 remote side가 동일 operation을 중복 실행하지 않게 하고, timeout 후 상태 조회를 통해 실제 결과를 확인한다.

여러 계층의 timeout budget도 조정한다. 외부 API timeout이 5초인데 전체 사용자 request deadline이 2초라면 의미가 없다. Parent deadline에서 남은 시간을 child call에 분배하고 retry가 총 budget을 초과하지 않게 한다.

Timeout 값을 너무 짧게 설정하면 정상적인 tail latency를 실패로 바꾸고 retry storm을 만들 수 있다. 실제 latency distribution과 서비스 목표를 근거로 설정한다.

---

## CHAPTER 07 · semaphore와 queue는 동시성 수와 producer/consumer 속도를 제어한다

Task를 수천 개 생성할 수 있다고 해서 수천 network connection이나 DB query를 동시에 실행하는 것이 안전한 것은 아니다. Semaphore는 critical resource를 동시에 사용하는 task 수를 제한하고, queue는 producer가 만든 작업을 consumer가 일정 속도로 처리하게 한다.

Bounded queue는 backpressure를 제공한다. Queue가 가득 차면 producer가 기다리거나 거부 정책을 적용해 memory가 무한히 증가하는 것을 막는다. Unbounded queue는 traffic spike를 처리한 것처럼 보여도 실제로는 지연된 작업을 memory에 쌓아 장애를 늦출 수 있다.

Concurrency limit은 downstream capacity와 연결한다. DB connection pool이 20개인데 1000 task가 query를 기다리게 만들면 scheduler overhead와 timeout만 늘 수 있다. Resource capacity와 queueing latency를 측정해 적절한 in-flight 수를 선택한다.

Priority가 필요한 queue에서는 starvation과 fairness도 고려한다. 긴 background job 때문에 사용자 request가 대기하지 않도록 workload class를 분리할 수 있다.

---

## CHAPTER 08 · shared mutable state는 await 사이에서 interleaving될 수 있다

한 thread에서 event loop가 돈다고 해서 race condition이 사라지는 것은 아니다. Task A가 balance를 읽고 `await`로 양보한 사이 task B가 balance를 변경하면 A가 재개해 오래된 값으로 overwrite할 수 있다. Race는 동시에 CPU instruction을 실행할 때만 생기는 것이 아니라 operation이 논리적으로 atomic하지 않을 때 생긴다.

```text
A: read balance=100
A: await ...
B: read balance=100
B: write balance=80
A: write balance=70
```

Lock으로 critical section을 보호할 수 있지만 lock 안에서 느린 await를 수행하면 다른 task가 오래 대기할 수 있다. 가능한 한 필요한 상태를 읽고 계산한 뒤 atomic storage operation을 사용하거나 ownership을 한 task에 집중하는 message-passing 구조를 고려한다.

Database state는 process-local asyncio lock으로 충분하지 않을 수 있다. 여러 process나 server instance가 같은 row를 수정한다면 transaction isolation, optimistic version, atomic update 같은 저장소 수준의 coordination이 필요하다.

---

## CHAPTER 09 · blocking call을 event loop에서 직접 실행하면 전체 동시성이 멈춘다

기존 sync library가 network나 disk에서 오래 block하거나 CPU computation을 수행하면 async function 안에서 호출해도 자동으로 non-blocking이 되지 않는다. Event loop thread가 그 호출이 끝날 때까지 다른 task를 schedule하지 못할 수 있다. “async 함수 안에 있으니 괜찮다”는 가정은 틀리다.

Blocking I/O library를 async-compatible library로 바꾸거나 executor/thread로 넘길 수 있다. CPU-heavy work는 GIL과 library implementation에 따라 thread가 실제 parallel speedup을 주지 못할 수 있어 process pool이 적합할 수 있다. 작업 특성을 먼저 분류한다.

Executor로 넘긴 작업도 cancellation semantics가 다르다. Async task가 취소되어도 이미 thread에서 실행 중인 blocking function을 강제로 중지할 수 없을 수 있다. Function 자체가 timeout/cancellation을 지원하는지 확인한다.

Monitoring에서 event loop lag를 측정하면 loop가 오래 block된 시점을 발견할 수 있다. 특정 request latency뿐 아니라 scheduler가 예정된 timer를 제때 실행하지 못하는 현상도 중요한 신호다.

---

## CHAPTER 10 · async 설계는 throughput, latency, resource lifetime을 함께 최적화한다

Asyncio의 목표를 “동시에 많이 실행”으로만 잡으면 task 수가 증가하고 downstream overload와 memory 사용이 커질 수 있다. 실제 목표는 I/O 기다림을 겹쳐 필요한 throughput을 얻으면서 latency budget과 resource capacity를 지키는 것이다.

작업 하나가 빠른지는 await 개수로 판단하지 않는다. Sequential dependency는 그대로 순서대로 실행해야 하고 독립 I/O만 안전하게 overlap할 수 있다. Concurrency를 추가하기 전에 data dependency graph를 그리면 병렬화 가능한 부분과 반드시 순차인 부분이 보인다.

Failure, timeout, cancellation, shutdown, backpressure를 정상 path와 같은 수준으로 설계한다. Happy path만 빠르고 오류 때 orphan task와 connection leak이 남는 async system은 운영에서 불안정하다.

비동기 코드를 읽을 때 최종 질문은 네 가지다. **현재 task가 어디서 양보하는가, 어떤 task들이 동시에 살아 있는가, 어떤 mutable state와 resource를 공유하는가, 실패·취소 시 누가 자식을 정리하는가.** 이 네 축을 추적하면 event loop 기반 프로그램을 예측 가능한 상태 머신으로 볼 수 있다.