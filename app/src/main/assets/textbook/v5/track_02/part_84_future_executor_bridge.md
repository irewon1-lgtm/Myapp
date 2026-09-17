# PART 84 · Future와 executor bridge — thread/process 작업을 async 결과 계약으로 연결하기

동기 함수를 비동기 application에 연결할 때 executor를 사용하면 event loop를 막지 않고 별도 thread나 process에서 작업할 수 있다. 하지만 executor는 “아무 함수를 async로 바꾸는 버튼”이 아니다. Future state, thread-safe 여부, process serialization, cancellation 한계를 이해해야 한다. 이 PART에서는 **작업 제출 → Future 상태 → 결과 회수 → 취소 가능성 → executor shutdown**의 lifecycle을 본다.

---

## CHAPTER 01 · Future는 아직 없을 수 있는 결과의 상태를 표현한다

Future는 pending, completed, failed, cancelled 같은 상태를 가지며 결과가 준비되면 consumer에게 전달한다. 직접 값이 아니라 **나중 결과에 대한 handle**이다.

```python
future = executor.submit(load_data, path)
result = future.result()
```

`result()` 호출이 결과 준비 전이면 block할 수 있다. Async event loop 안에서 concurrent future의 blocking `result()`를 직접 호출하면 loop를 멈출 수 있으므로 appropriate bridge를 사용한다.

Future object를 공유하면 여러 consumer가 같은 작업 결과를 기다릴 수 있지만 누가 cancellation과 exception handling을 책임지는지 정해야 한다.

---

## CHAPTER 02 · ThreadPoolExecutor는 같은 process memory를 공유하므로 thread safety가 필요하다

Thread executor의 worker는 같은 address space의 object를 접근할 수 있다. File I/O, blocking network library처럼 GIL을 release하거나 I/O 대기 시간이 큰 작업을 event loop 밖으로 옮길 때 유용하다.

하지만 mutable object를 여러 worker가 수정하면 race가 생길 수 있다. “Python에는 GIL이 있으니 data race가 없다”라고 생각하면 안 된다. 여러 bytecode step과 I/O release 사이에서 interleaving이 발생한다.

Thread-local state, DB connection affinity, GUI main-thread rule도 확인한다. 함수가 동기라는 이유만으로 thread executor에 안전한 것은 아니다.

---

## CHAPTER 03 · ProcessPoolExecutor는 address space를 분리하고 serialization 경계를 만든다

CPU-bound Python 작업을 여러 process로 분리하면 interpreter execution을 병렬화할 수 있는 경우가 있다. 대신 argument와 result가 process boundary를 넘어야 하므로 picklability와 data movement cost가 중요하다.

큰 numpy-like buffer나 수백 MB object graph를 매 task마다 serialize하면 계산보다 전송 비용이 더 커질 수 있다. Process pool은 함수 호출 latency도 thread보다 크다.

Worker가 parent의 open socket, lock, runtime state를 그대로 안전하게 공유한다고 가정하지 않는다. Start method와 platform 차이도 multiprocessing semantics와 연결된다.

---

## CHAPTER 04 · async code에서 executor bridge는 blocking 함수를 event loop 밖으로 격리한다

Legacy 동기 library를 async service에서 호출해야 할 때 executor bridge를 사용할 수 있다.

```python
loop = asyncio.get_running_loop()
result = await loop.run_in_executor(None, blocking_call, arg)
```

이 구조는 current task가 기다리는 동안 event loop가 다른 작업을 진행하게 한다. 하지만 blocking_call 자체가 멈추는 것은 아니고 worker에서 계속 실행된다.

호출 빈도가 높으면 기본 thread pool이 포화될 수 있다. 작업 종류별 pool 분리, concurrency limit, backpressure를 함께 고려한다.

---

## CHAPTER 05 · execution context는 thread/process boundary에서 자동 보존된다고 가정하지 않는다

Request ID, locale, security context가 `contextvars`나 thread-local에 있을 수 있다. Executor worker로 넘어갈 때 어떤 context가 전파되는지는 API와 runtime에 따라 다르므로 명시적으로 확인해야 한다.

Logging correlation ID가 executor 내부에서 사라지면 운영 trace가 끊긴다. 반대로 민감한 request context를 불필요한 background worker에 복사하면 isolation이 약해질 수 있다.

Context propagation은 편의가 아니라 ownership과 security 범위의 문제다.

---

## CHAPTER 06 · Future cancellation은 이미 실행 중인 외부 작업을 강제로 중단하지 못할 수 있다

Pending task는 queue에서 취소할 수 있어도 worker가 이미 function을 실행 중이면 Future cancellation이 underlying blocking call을 즉시 멈출 수 없는 경우가 많다.

```text
async caller timeout -> future cancelled 표시 -> worker thread의 blocking I/O는 계속될 수 있음
```

따라서 실제 작업 함수 자체에도 timeout이나 cooperative stop mechanism이 필요할 수 있다. Database query, HTTP call 같은 외부 operation은 library-level timeout을 설정한다.

Cancellation semantics를 사용자에게 “작업이 취소됨”이라고 표시하기 전에 side effect가 이미 실행됐는지 고려한다.

---

## CHAPTER 07 · executor shutdown은 새 작업 접수와 기존 작업 종료 정책을 결정한다

Application 종료 시 executor를 방치하면 worker가 남거나 process 종료가 늦어질 수 있다. Context manager나 explicit shutdown으로 lifecycle을 owner에 연결한다.

Shutdown에서 pending future를 어떻게 할지, running task 완료를 기다릴지 정책을 정한다. Server graceful shutdown에서는 새 request를 막고 일정 시간 기존 작업을 기다린 뒤 남은 작업을 취소하는 단계가 필요할 수 있다.

Process pool worker crash나 broken pool 상태도 정상 failure mode로 다룬다. 무한히 submit retry하지 않는다.

---

## CHAPTER 08 · executor contract는 workload 유형과 취소 한계를 명확히 해야 한다

Executor를 선택할 때는 I/O-bound인지 CPU-bound인지, argument serialization 비용이 얼마나 되는지, function이 thread/process safe인지, timeout 후 underlying work를 실제로 멈출 수 있는지 확인한다.

테스트에서는 worker exception, queue saturation, timeout, already-running cancellation, shutdown 중 submit을 분리한다. 성능 test는 단일 task가 아니라 realistic concurrency에서 queue delay까지 측정한다.

이 PART의 핵심은 **executor를 async 변환 도구로 보지 않고, 실행 위치를 다른 thread/process로 옮기면서 Future state와 새로운 concurrency·serialization·lifecycle 계약을 만드는 bridge로 이해하는 것**이다.
