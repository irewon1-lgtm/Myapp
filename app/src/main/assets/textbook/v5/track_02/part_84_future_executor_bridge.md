# PART 84 · Future와 executor bridge — thread/process 작업을 async 결과 계약으로 연결하기

동기 함수를 비동기 application에 연결할 때 executor를 사용하면 event loop를 막지 않고 별도 thread나 process에서 작업할 수 있다. 하지만 executor는 “아무 함수를 async로 바꾸는 버튼”이 아니다. Future state, thread-safe 여부, process serialization, cancellation 한계를 이해해야 한다. 이 PART에서는 **작업 제출 → Future 상태 → 결과 회수 → 취소 가능성 → executor shutdown**의 lifecycle을 본다.

---

## CHAPTER 01 · Future는 아직 없을 수 있는 결과의 상태를 표현한다

### 시작 전 용어집

#### 1. Future

- **뜻:** Future는 pending, completed, failed, cancelled 같은 상태를 가지며 결과가 준비되면 consumer에게 전달한다.
- **왜 중요한가:** 직접 값이 아니라 **나중 결과에 대한 handle**이다.
- **예시:** future = executor.submit(load_data, path) / result = future.result()

#### 2. 상태

- **뜻:** `result()` 호출이 결과 준비 전이면 block할 수 있다.
- **왜 중요한가:** Async event loop 안에서 concurrent future의 blocking `result()`를 직접 호출하면 loop를 멈출 수 있으므로 appropriate bridge를 사용한다.
- **예시:** future = executor.submit(load_data, path) / result = future.result()

#### 3. cell

- **뜻:** Future object를 공유하면 여러 consumer가 같은 작업 결과를 기다릴 수 있지만 누가 cancellation과 exception handling을 책임지는지 정해야 한다.
- **예시:** future = executor.submit(load_data, path) / result = future.result()

```python
future = executor.submit(load_data, path)
result = future.result()
```

 


---

## CHAPTER 02 · ThreadPoolExecutor는 같은 process memory를 공유하므로 thread safety가 필요하다

### 시작 전 용어집

#### 1. thread

- **뜻:** Thread executor의 worker는 같은 address space의 object를 접근할 수 있다.
- **왜 중요한가:** File I/O, blocking network library처럼 GIL을 release하거나 I/O 대기 시간이 큰 작업을 event loop 밖으로 옮길 때 유용하다.
- **예시:** Thread executor의 worker는 같은 address space의 object를 접근할 …

#### 2. executor

- **뜻:** 함수가 동기라는 이유만으로 thread executor에 안전한 것은 아니다.
- **왜 중요한가:** 하지만 mutable object를 여러 worker가 수정하면 race가 생길 수 있다.
- **예시:** 함수가 동기라는 이유만으로 thread executor에 안전한 것은 아니다.

#### 3. process

- **뜻:** “Python에는 GIL이 있으니 data race가 없다”라고 생각하면 안 된다.
- **왜 중요한가:** 여러 bytecode step과 I/O release 사이에서 interleaving이 발생한다.
- **예시:** “Python에는 GIL이 있으니 data race가 없다”라고 생각하면 안 …

#### 4. lock

- **뜻:** Thread-local state, DB connection affinity, GUI main-thread rule도 확인한다.
- **예시:** Thread-local state, DB connection affinity, GUI main-thread rule도 …

---

## CHAPTER 03 · ProcessPoolExecutor는 address space를 분리하고 serialization 경계를 만든다

### 시작 전 용어집

#### 1. process

- **뜻:** CPU-bound Python 작업을 여러 process로 분리하면 interpreter execution을 병렬화할 수 있는 경우가 있다.
- **왜 중요한가:** 대신 argument와 result가 process boundary를 넘어야 하므로 picklability와 data movement cost가 중요하다.
- **예시:** CPU-bound Python 작업을 여러 process로 분리하면 interpreter execution을 …

#### 2. executor

- **뜻:** 큰 numpy-like buffer나 수백 MB object graph를 매 task마다 serialize하면 계산보다 전송 비용이 더 커질 수 있다.
- **왜 중요한가:** Process pool은 함수 호출 latency도 thread보다 크다.
- **예시:** 큰 numpy-like buffer나 수백 MB object graph를 매 …

#### 3. address space

- **뜻:** Worker가 parent의 open socket, lock, runtime state를 그대로 안전하게 공유한다고 가정하지 않는다.
- **왜 중요한가:** Start method와 platform 차이도 multiprocessing semantics와 연결된다.
- **예시:** Worker가 parent의 open socket, lock, runtime state를 그대로 …

---

## CHAPTER 04 · async code에서 executor bridge는 blocking 함수를 event loop 밖으로 격리한다

### 시작 전 용어집

#### 1. async code

- **뜻:** Legacy 동기 library를 async service에서 호출해야 할 때 executor bridge를 사용할 수 있다.
- **왜 중요한가:** 이 구조는 current task가 기다리는 동안 event loop가 다른 작업을 진행하게 한다.
- **예시:** loop = asyncio.get_running_loop() / result = await loop.run_in_executor(None, …

#### 2. executor

- **뜻:** 하지만 blocking_call 자체가 멈추는 것은 아니고 worker에서 계속 실행된다.
- **왜 중요한가:** 호출 빈도가 높으면 기본 thread pool이 포화될 수 있다.
- **예시:** loop = asyncio.get_running_loop() / result = await loop.run_in_executor(None, …

#### 3. lock

- **뜻:** 작업 종류별 pool 분리, concurrency limit, backpressure를 함께 고려한다.
- **예시:** loop = asyncio.get_running_loop() / result = await loop.run_in_executor(None, …

```python
loop = asyncio.get_running_loop()
result = await loop.run_in_executor(None, blocking_call, arg)
```

 

 

---

## CHAPTER 05 · execution context는 thread/process boundary에서 자동 보존된다고 가정하지 않는다

### 시작 전 용어집

#### 1. exec

- **뜻:** Executor worker로 넘어갈 때 어떤 context가 전파되는지는 API와 runtime에 따라 다르므로 명시적으로 확인해야 한다.
- **왜 중요한가:** Logging correlation ID가 executor 내부에서 사라지면 운영 trace가 끊긴다.
- **예시:** Executor worker로 넘어갈 때 어떤 context가 전파되는지는 API와 …

#### 2. thread

- **뜻:** Request ID, locale, security context가 `contextvars`나 thread-local에 있을 수 있다.
- **왜 중요한가:** 반대로 민감한 request context를 불필요한 background worker에 복사하면 isolation이 약해질 수 있다.
- **예시:** Request ID, locale, security context가 `contextvars`나 thread-local에 있을 …

#### 3. process

- **뜻:** Context propagation은 편의가 아니라 ownership과 security 범위의 문제다.
- **예시:** Context propagation은 편의가 아니라 ownership과 security 범위의 문제다.

---

## CHAPTER 06 · Future cancellation은 이미 실행 중인 외부 작업을 강제로 중단하지 못할 수 있다

### 시작 전 용어집

#### 1. Future

- **뜻:** Pending task는 queue에서 취소할 수 있어도 worker가 이미 function을 실행 중이면 Future cancellation이 underlying blocking call을 즉시 멈출 수 없는 경우가 많다.
- **왜 중요한가:** 따라서 실제 작업 함수 자체에도 timeout이나 cooperative stop mechanism이 필요할 수 있다.
- **예시:** async caller timeout -> future cancelled 표시 -> …

#### 2. cancellation

- **뜻:** Cancellation semantics를 사용자에게 “작업이 취소됨”이라고 표시하기 전에 side effect가 이미 실행됐는지 고려한다.
- **왜 중요한가:** Database query, HTTP call 같은 외부 operation은 library-level timeout을 설정한다.
- **예시:** async caller timeout -> future cancelled 표시 -> …

```text
async caller timeout -> future cancelled 표시 -> worker thread의 blocking I/O는 계속될 수 있음
```

 


---

## CHAPTER 07 · executor shutdown은 새 작업 접수와 기존 작업 종료 정책을 결정한다

### 시작 전 용어집

#### 1. executor

- **뜻:** Application 종료 시 executor를 방치하면 worker가 남거나 process 종료가 늦어질 수 있다.
- **왜 중요한가:** Context manager나 explicit shutdown으로 lifecycle을 owner에 연결한다.
- **예시:** Application 종료 시 executor를 방치하면 worker가 남거나 process …

#### 2. process

- **뜻:** Process pool worker crash나 broken pool 상태도 정상 failure mode로 다룬다.
- **왜 중요한가:** Shutdown에서 pending future를 어떻게 할지, running task 완료를 기다릴지 정책을 정한다.
- **예시:** Process pool worker crash나 broken pool 상태도 정상 …

#### 3. context manager

- **뜻:** Server graceful shutdown에서는 새 request를 막고 일정 시간 기존 작업을 기다린 뒤 남은 작업을 취소하는 단계가 필요할 수 있다.
- **예시:** Server graceful shutdown에서는 새 request를 막고 일정 시간 …

무한히 submit retry하지 않는다.

---

## CHAPTER 08 · executor contract는 workload 유형과 취소 한계를 명확히 해야 한다

### 시작 전 용어집

#### 1. executor

- **뜻:** Executor를 선택할 때는 I/O-bound인지 CPU-bound인지, argument serialization 비용이 얼마나 되는지, function이 thread/process safe인지, timeout 후 underlying work를 실제로 멈출 수 있는지 확인한다.
- **왜 중요한가:** 테스트에서는 worker exception, queue saturation, timeout, already-running cancellation, shutdown 중 submit을 분리한다.
- **예시:** Executor를 선택할 때는 I/O-bound인지 CPU-bound인지, argument serialization 비용이 …

#### 2. workload

- **뜻:** 성능 test는 단일 task가 아니라 realistic concurrency에서 queue delay까지 측정한다.
- **왜 중요한가:** 이 PART의 핵심은 **executor를 async 변환 도구로 보지 않고, 실행 위치를 다른 thread/process로 옮기면서 Future state와 새로운 concurrency·serialization·lifecycle 계약을 만드는 bridge로 이해하는 것**이다.
- **예시:** 성능 test는 단일 task가 아니라 realistic concurrency에서 queue …

---

## 실전 학습 루프 · Future·executor bridge

### 1. 쉬운 예

blocking 함수나 CPU 작업을 event loop thread에서 직접 실행하면 다른 coroutine 진행까지 막을 수 있다. executor bridge는 해당 작업을 다른 실행 자원에 넘기고 Future로 결과를 다시 연결한다.

### 2. 한 줄 해석

executor 사용은 blocking을 없애는 것이 아니라 blocking이 일어나는 위치와 자원 pool을 분리하는 것이다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상한다. 그 다음 아래 최소 예제를 실행하고, 예상이 틀렸다면 **호출 순서와 상태 변화**를 표시한다.

```python
import asyncio, time

def blocking():
    time.sleep(0.05)
    return 7

async def main():
    loop = asyncio.get_running_loop()
    result = await loop.run_in_executor(None, blocking)
    print(result)

asyncio.run(main())
```

### 4. 수정 실습

1. executor worker 수보다 많은 blocking 작업을 넣고 queueing을 생각한다.
2. 취소된 Future가 이미 실행 중인 thread 작업을 즉시 중단시키는지 확인한다.

수정 후에는 정상 예제만 다시 보지 말고 실패·경계·반복 호출 중 하나를 추가해 계약이 유지되는지 확인한다.

### 5. 확인 문제

executor로 넘기면 blocking 작업 자체가 non-blocking 코드로 바뀌는가?

### 6. 정답과 오답 설명

**정답:** 아니다. blocking은 worker에서 계속 일어나며 event loop가 직접 기다리지 않을 뿐이다.

**자주 나오는 오답:** thread pool을 무한 자원처럼 보면 latency와 memory가 오히려 악화될 수 있다.

이 PART를 마칠 때는 해당 문법 이름을 외우는 데서 멈추지 말고 **언제 호출되는가 / 무엇을 읽거나 바꾸는가 / 실패하면 어디로 가는가** 세 문장으로 설명한다.

## 현장 디버깅 체크 · Future·executor bridge

### 증상에서 시작한다

event loop는 멈추지 않지만 executor queue가 길어져 응답 시간이 계속 늘거나 shutdown이 늦어진다. 재현 시점의 입력과 작업 식별자를 먼저 고정하고, 결과를 보고 추측하기보다 상태 전이를 시간순으로 적는다.

### 먼저 볼 증거

worker 수, queue 대기 시간, 실행 시간, Future 상태, cancellation 시 실제 worker 동작을 따로 본다. 한 숫자만 보지 말고 **대기/실행/완료/실패**를 분리하면 병목과 논리 오류를 구분하기 쉽다.

### 일부러 실패시켜 보기

worker 수보다 많은 blocking 작업을 넣고 일부 Future를 cancel해 queue와 이미 실행 중 작업이 어떻게 달라지는지 측정한다. 정상 경로는 원래 잘 되는 경우가 많다. 강제 실패에서 cleanup·retry·재시작 의미가 유지되는지가 운영 품질을 결정한다.

### 통과 기준

executor로 넘긴 작업도 bounded concurrency와 deadline 안에서 관리되고, cancellation 의미가 코드와 운영 지표에 일치해야 한다. 이 기준을 regression test와 운영 metric 두 곳에 동시에 연결하면 배포 뒤 같은 문제가 돌아왔을 때 빠르게 탐지할 수 있다.

