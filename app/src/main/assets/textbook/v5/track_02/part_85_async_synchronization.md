# PART 85 · Async synchronization — Lock·Event·Condition·Semaphore를 상태 의미로 구분하기

Async program도 shared state와 resource capacity를 조정해야 한다. 다만 `asyncio` synchronization primitive는 thread primitive와 같은 이름을 가져도 event loop task 사이에서 cooperative하게 동작하며, cancellation이 waiter queue에 끼어들 수 있다. 중요한 것은 primitive 이름을 외우는 것이 아니라 **어떤 상태를 보호하고 어떤 condition에서 task를 깨울 것인지**를 정확히 표현하는 것이다.

---

## CHAPTER 01 · async Lock은 한 번에 하나의 task만 critical section에 들어가게 한다

```python
lock = asyncio.Lock()

async with lock:
    await update_shared_state()
```

Lock은 mutual exclusion을 제공하지만 critical section 안에 await가 있으면 lock을 잡은 채 다른 I/O를 기다릴 수 있다. 이 동안 같은 lock이 필요한 다른 task는 모두 막힌다.

따라서 lock 범위를 최소화하고, 오래 걸리는 network call까지 lock 안에 넣어야 하는지 검토한다. Shared state snapshot만 보호하고 외부 I/O는 lock 밖에서 하는 구조가 가능한지 본다.

Lock이 있다고 transaction atomicity가 자동 보장되는 것은 아니다. Process 밖의 database나 remote system state는 별도 consistency mechanism이 필요하다.

---

## CHAPTER 02 · Event는 하나의 상태 변화가 여러 waiter를 깨우는 signal이다

Event는 set/clear 상태를 가지며 여러 task가 같은 readiness signal을 기다릴 수 있다.

```python
ready = asyncio.Event()

async def consumer():
    await ready.wait()
    use_service()
```

Event는 data item queue가 아니다. `set()`을 여러 번 호출했다고 signal count가 누적되는 semaphore처럼 동작하지 않는다. “준비됨/아직 아님” 같은 level state를 표현하는 데 적합하다.

Event를 clear하는 owner와 timing이 불명확하면 늦게 도착한 waiter가 signal을 놓치거나 예상보다 오래 기다릴 수 있다. Signal 의미와 lifecycle을 문서화한다.

---

## CHAPTER 03 · Condition은 lock과 predicate를 결합해 상태가 원하는 형태가 될 때까지 기다린다

Condition을 사용할 때 핵심은 notification 자체가 아니라 predicate다.

```python
async with condition:
    await condition.wait_for(lambda: len(buffer) >= 10)
```

Wake-up 후에도 predicate를 다시 확인해야 한다. 다른 task가 먼저 state를 소비했거나 여러 waiter가 동시에 깨어났을 수 있다.

Condition은 producer/consumer buffer처럼 여러 상태 전환을 같은 lock 아래 조정할 때 유용하다. 단순 “작업 끝남” 신호라면 Event가 더 간결할 수 있다.

Primitive를 더 복잡하게 만드는 것이 안전성을 자동으로 높이지 않는다. 문제의 상태 모델과 맞는 최소 도구를 고른다.

---

## CHAPTER 04 · Semaphore는 mutual exclusion이 아니라 동시 사용 capacity를 제한한다

Semaphore count가 10이면 최대 10개 task가 동시에 resource를 사용할 수 있는 식으로 concurrency budget을 표현할 수 있다.

```python
limit = asyncio.Semaphore(10)

async with limit:
    await call_remote_api()
```

HTTP connection, external API rate pressure, memory-heavy job처럼 완전 직렬화는 필요 없지만 무제한 fan-out이 위험한 곳에 적합하다.

Semaphore 숫자는 임의 tuning 값이 아니다. Downstream capacity, latency, timeout, memory 사용을 측정해 정한다. Limit이 너무 작으면 throughput이 떨어지고 너무 크면 remote service와 자신의 queue를 동시에 압박한다.

---

## CHAPTER 05 · fairness는 구현 세부와 cancellation에 영향을 받을 수 있으므로 절대 보장으로 가정하지 않는다

Waiter가 먼저 기다렸다고 항상 strict FIFO 순서로 critical section을 얻는다고 일반화하면 위험하다. Primitive와 Python version의 documented behavior를 확인해야 한다.

Priority가 중요한 scheduler라면 일반 Lock queue에 priority semantics를 기대하지 말고 별도 priority queue와 worker architecture를 만든다.

또 한 task가 lock을 자주 짧게 재획득하면 다른 task가 starvation에 가까운 지연을 겪을 수 있다. 평균 latency만 보지 말고 tail wait time을 측정한다.

---

## CHAPTER 06 · cancellation은 waiter를 queue에서 제거하고 상태 invariant를 유지해야 한다

Lock이나 semaphore를 기다리는 task가 cancellation되면 해당 waiter가 더 이상 resource를 받을 필요가 없다. Runtime primitive가 이를 처리하더라도 application이 acquisition 전/후 상태를 구분해야 한다.

가장 위험한 경우는 resource를 획득한 뒤 cancellation되어 release가 누락되는 것이다. `async with`를 사용하면 lexical cleanup을 연결하기 쉽다.

```python
async with semaphore:
    await work()
```

수동 acquire/release를 쓰면 `try/finally`로 release를 보장한다. Cancellation test를 정상 exception test와 별도로 둔다.

---

## CHAPTER 07 · 여러 lock을 잡는다면 acquisition order가 deadlock 가능성을 결정한다

Task A가 lock X를 잡고 Y를 기다리며, Task B가 Y를 잡고 X를 기다리면 cooperative async code에서도 deadlock이 생긴다. Thread가 아니어도 자원 순환 대기는 가능하다.

해결은 전역 lock ordering, 하나의 higher-level lock, ownership actor처럼 구조적으로 cycle을 제거하는 것이다. Timeout은 deadlock을 감지할 수 있지만 consistency 문제를 자동 해결하지 않는다.

Lock이 늘어날수록 state ownership이 분산됐다는 신호일 수 있다. Single-owner task와 message passing으로 단순화할 수 있는지 검토한다.

---

## CHAPTER 08 · synchronization contract는 보호 대상과 wake-up 조건을 먼저 정의한다

Lock, Event, Condition, Semaphore 선택은 API 이름보다 state model에서 시작한다. 하나의 invariant를 보호하는가, readiness state를 broadcast하는가, predicate 변화에 기다리는가, capacity를 제한하는가를 구분한다.

테스트에서는 contention, cancellation, timeout, double release 방지, shutdown 중 waiter를 포함한다. Deterministic test에서는 fake clock이나 controlled scheduling으로 rare ordering을 재현한다.

이 PART의 핵심은 **async synchronization을 경쟁 상태를 막는 주문처럼 사용하지 않고, shared state ownership과 waiter wake-up 조건을 명시적으로 표현하는 protocol로 설계하는 것**이다.
