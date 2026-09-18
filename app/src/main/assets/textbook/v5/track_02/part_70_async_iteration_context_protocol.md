# PART 70 · Async iteration과 async context protocol — suspension·종료·cancellation cleanup을 연결하기

`async for`와 `async with`는 동기 protocol에 `await`를 붙인 단순 변형이 아니다. 각 iteration step이나 resource acquisition/release가 suspension point가 될 수 있고, 그 사이 다른 task가 실행되며 cancellation이 들어올 수 있다. 따라서 비동기 protocol에서는 **데이터 순서뿐 아니라 await 경계와 cleanup 보장**을 함께 설계해야 한다.

---

## CHAPTER 01 · async iteration은 `__aiter__`와 `__anext__`로 다음 값을 awaitable하게 만든다

### 시작 전 용어집

#### 1. async iteration

- **뜻:** Async iterable은 `async for`에 참여하며 각 다음 값 요청이 대기할 수 있다.
- **왜 중요한가:** Network stream, queue, paginated API처럼 다음 값이 즉시 준비되지 않는 source에 적합하다.
- **예시:** class AsyncCounter: / def __init__(self, limit):

#### 2. __aiter__

- **뜻:** 일반 iterator와 달리 각 step 사이에 scheduler에게 control이 넘어갈 수 있으므로 shared state가 바뀔 수 있다.
- **왜 중요한가:** Async iterator가 self를 반환하는 single-pass cursor인지, 매번 새 iterator를 만드는 reusable source인지 구분한다.
- **예시:** class AsyncCounter: / def __init__(self, limit):

#### 3. __anext__

- **뜻:** 두 semantics를 섞으면 두 consumer가 같은 cursor를 경쟁할 수 있다.
- **예시:** class AsyncCounter: / def __init__(self, limit):

```python
class AsyncCounter:
    def __init__(self, limit):
        self.limit = limit
        self.current = 0

    def __aiter__(self):
        return self

    async def __anext__(self):
        if self.current >= self.limit:
            raise StopAsyncIteration
        value = self.current
        self.current += 1
        return value
```

 

 

---

## CHAPTER 02 · `StopAsyncIteration`은 async stream의 정상 종료 신호다

### 시작 전 용어집

#### 1. StopAsyncIteration

- **뜻:** 동기 iterator의 `StopIteration`과 마찬가지로 async iterator는 더 이상 값이 없을 때 `StopAsyncIteration`을 사용한다.
- **왜 중요한가:** 이것은 일반 business failure와 구분되는 protocol 종료 신호다.
- **예시:** async def __anext__(self): / item = await self.queue.get()

#### 2. async stream

- **뜻:** 종료 sentinel을 외부 data와 충돌하지 않게 설계하고, 실제 I/O error를 정상 종료로 바꾸지 않는다.
- **왜 중요한가:** Connection reset, authentication failure, parser error까지 `StopAsyncIteration`으로 숨기면 consumer는 stream이 정상 완료됐다고 오해한다.
- **예시:** async def __anext__(self): / item = await self.queue.get()

#### 3. iterator

- **뜻:** 종료 시 내부 resource 정리가 필요하다면 async context manager와 결합하거나 explicit close protocol을 제공한다.
- **예시:** async def __anext__(self): / item = await self.queue.get()

```python
async def __anext__(self):
    item = await self.queue.get()
    if item is END:
        raise StopAsyncIteration
    return item
```

 


---

## CHAPTER 03 · `async for`를 desugar하면 매 반복에 await 경계가 있음을 볼 수 있다

### 시작 전 용어집

#### 1. async for

- **뜻:** 개념적으로 `async for item in source`는 async iterator를 얻고 반복적으로 `__anext__` 결과를 await하는 구조다.
- **왜 중요한가:** 실제 language semantics에는 세부 규칙이 있지만 이 모델만으로도 중요한 사실이 드러난다.
- **예시:** async for item in source: / await store(item)

#### 2. desugar

- **뜻:** 그래서 다음과 같은 코드는 동기 loop보다 더 많은 interleaving 가능성을 가진다.
- **왜 중요한가:** __anext__()` 대기와 `store()` 대기 사이에 shared configuration이나 cancellation state가 바뀔 수 있다.
- **예시:** async for item in source: / await store(item)

#### 3. 반복

- **뜻:** Loop 시작 때 읽은 값을 계속 동일하다고 가정하지 않는다.
- **왜 중요한가:** Batching으로 await 횟수를 줄일 수 있지만 latency와 memory가 달라진다.
- **예시:** async for item in source: / await store(item)

#### 4. await

- **뜻:** Protocol을 이해한 뒤 performance trade-off를 조정한다.
- **예시:** async for item in source: / await store(item)

**반복문 한 바퀴마다 다른 task가 끼어들 수 있다.**


```python
async for item in source:
    await store(item)
```

`source. 

 

---

## CHAPTER 04 · async context protocol은 acquisition과 release 자체가 await될 수 있게 한다

### 시작 전 용어집

#### 1. async context

- **뜻:** 반대로 실제 await가 필요 없는 작은 local lock까지 무조건 async context로 만들 필요는 없다.
- **왜 중요한가:** Acquisition이 절반만 성공한 경우의 rollback도 동기 context와 마찬가지로 `__aenter__` 안에서 처리해야 한다.
- **예시:** class ConnectionLease: / async def __aenter__(self):

#### 2. protocol

- **뜻:** `async with`는 `__aenter__`와 `__aexit__`를 사용한다.
- **왜 중요한가:** Connection pool에서 lease를 얻거나 remote lock을 해제하는 것처럼 진입·종료 자체가 비동기 작업인 resource에 적합하다.
- **예시:** class ConnectionLease: / async def __aenter__(self):

#### 3. acquisition

- **뜻:** 동기 context manager 안에서 blocking I/O를 수행하면 event loop 전체를 막을 수 있다.
- **예시:** class ConnectionLease: / async def __aenter__(self):

```python
class ConnectionLease:
    async def __aenter__(self):
        self.conn = await self.pool.acquire()
        return self.conn

    async def __aexit__(self, exc_type, exc, tb):
        await self.pool.release(self.conn)
        return False
```

 


---

## CHAPTER 05 · `__aenter__`와 `__aexit__` 사이에는 task suspension과 외부 변화가 존재한다

### 시작 전 용어집

#### 1. __aenter__

- **뜻:** Async context는 lexical scope가 명확하지만 그 scope 안의 execution이 연속적이라는 뜻은 아니다.
- **왜 중요한가:** Body가 await할 때마다 다른 task가 동일 service나 shared state를 변경할 수 있다.
- **예시:** 예를 들어 transaction context 안에서 await를 여러 번 …

#### 2. __aexit__

- **뜻:** 예를 들어 transaction context 안에서 await를 여러 번 하면 database transaction 자체는 유지돼도 application-level cache나 in-memory state는 다른 task에 의해 바뀔 수 있다.
- **왜 중요한가:** “context 안이므로 모든 상태가 고정된다”는 가정을 하지 않는다.
- **예시:** 예를 들어 transaction context 안에서 await를 여러 번 …

#### 3. task suspension

- **뜻:** Resource가 concurrency isolation까지 제공하는지 단순 lifetime 관리만 제공하는지 분리한다.
- **왜 중요한가:** Async lock context는 mutual exclusion을 줄 수 있지만 transaction context와 같은 rollback 의미는 자동으로 생기지 않는다.
- **예시:** Resource가 concurrency isolation까지 제공하는지 단순 lifetime 관리만 제공하는지 …

---

## CHAPTER 06 · cancellation은 cleanup 경로를 실제 실패 유형으로 만든다

### 시작 전 용어집

#### 1. cancellation

- **뜻:** Async code에서 task cancellation은 특별히 자주 만나는 종료 경로다.
- **왜 중요한가:** Cancellation이 await 지점에서 전달되면 resource release가 누락되지 않도록 `async with`와 `finally`를 사용해야 한다.
- **예시:** async with lease() as conn: / await do_work(conn)

#### 2. cleanup

- **뜻:** 다만 cleanup 자체가 await를 포함하면 그 cleanup이 다시 cancellation의 영향을 받을 수 있으므로 library contract와 runtime semantics를 이해해야 한다.
- **왜 중요한가:** Cancellation을 일반 exception처럼 무조건 catch하고 계속 실행하면 상위 orchestration이 task를 멈추지 못할 수 있다.
- **예시:** async with lease() as conn: / await do_work(conn)

#### 3. 실패

- **뜻:** `do_work` 중 cancellation이 들어와도 context exit가 실행되어 lease를 반환해야 한다.
- **왜 중요한가:** 필요한 cleanup을 수행한 뒤 취소 의도를 보존하는 것이 기본이다.
- **예시:** async with lease() as conn: / await do_work(conn)

```python
async with lease() as conn:
    await do_work(conn)
```

 

 

---

## CHAPTER 07 · sync/async boundary를 섞으면 blocking과 hidden scheduler dependency가 생긴다

### 시작 전 용어집

#### 1. sync

- **뜻:** 동기 함수에서 async iterator를 억지로 소비하거나 async code에서 blocking file/network API를 직접 호출하면 실행 모델이 충돌한다.
- **왜 중요한가:** Event loop thread에서 긴 blocking call을 수행하면 다른 task가 진행하지 못한다.
- **예시:** 동기 함수에서 async iterator를 억지로 소비하거나 async code에서 …

#### 2. blocking

- **뜻:** Boundary를 정할 때 operation이 실제로 blocking I/O인지, CPU-bound인지, 짧은 local operation인지 구분한다.
- **왜 중요한가:** Library API도 한 계층에서 sync와 async 버전을 무질서하게 섞기보다 명확한 adapter boundary를 둔다.
- **예시:** Boundary를 정할 때 operation이 실제로 blocking I/O인지, CPU-bound인지, …

#### 3. hidden scheduler

- **뜻:** 반대로 작은 CPU 연산을 무조건 thread executor로 보내면 context switching과 error propagation 복잡도가 커질 수 있다.
- **왜 중요한가:** 같은 resource에 두 API가 동시에 접근할 때 thread safety와 event-loop affinity도 검토한다.
- **예시:** 반대로 작은 CPU 연산을 무조건 thread executor로 보내면 …

---

## CHAPTER 08 · async protocol contract는 backpressure·ownership·cancellation을 함께 정의한다

### 시작 전 용어집

#### 1. async protocol

- **뜻:** Async iterable을 public API로 제공할 때 consumer가 천천히 읽으면 producer가 얼마나 buffer하는지, iterator를 중간에 포기하면 어떤 cleanup이 필요한지, 같은 source를 두 consumer가 동시에 읽을 수 있는지 정해야 한다.
- **왜 중요한가:** Async context manager는 어떤 task가 resource를 소유하는지, 중첩 사용이 가능한지, cancellation 중 release를 어떻게 보장하는지 문서화한다.
- **예시:** Async iterable을 public API로 제공할 때 consumer가 천천히 …

#### 2. contract

- **뜻:** Test에서는 정상 종료, source exhaustion, producer error, consumer cancellation, `__aenter__` 실패, `__aexit__` 실패를 서로 다른 case로 확인한다.
- **왜 중요한가:** 이 PART의 핵심은 **async iteration과 async context를 단순히 await가 붙은 문법으로 보지 않고, suspension point 사이의 interleaving과 cancellation까지 포함한 resource·stream protocol로 설계하는 것**이다.
- **예시:** Test에서는 정상 종료, source exhaustion, producer error, consumer …
