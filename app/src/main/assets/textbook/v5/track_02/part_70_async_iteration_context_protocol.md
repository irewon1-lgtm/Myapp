# TRACK 02 · P70 — Async iteration과 async context: __aiter__·__anext__·async with의 suspension 경계를 추적하기

비동기 프로토콜은 동기 프로토콜에 `async`라는 글자를 붙인 버전이 아니다. 각 단계 사이에 `await` suspension이 들어갈 수 있고, 그 사이에 다른 task가 실행되거나 cancellation이 전달될 수 있다. 그래서 iteration 종료, 자원 획득, cleanup을 모두 **시간적으로 분리된 상태 전이**로 이해해야 한다.

P55에서는 task orchestration과 cancellation을 넓게 다뤘다. 이번 PART에서는 그 내용을 반복하지 않고, `async for`와 `async with`가 요구하는 **객체 프로토콜 경계**에 집중한다.

---

## 1. Async iteration protocol — 다음 값이 즉시 준비되지 않아도 된다

비동기 반복 가능한 객체는 `__aiter__`를 통해 async iterator를 제공하고, iterator는 `__anext__`로 다음 값을 awaitable 형태로 만든다.

```python
class Ticker:
    def __init__(self, values):
        self.values = list(values)
        self.index = 0

    def __aiter__(self):
        return self

    async def __anext__(self):
        if self.index >= len(self.values):
            raise StopAsyncIteration
        value = self.values[self.index]
        self.index += 1
        return value
```

사용자는 다음처럼 읽는다.

```python
async for value in Ticker([10, 20, 30]):
    print(value)
```

동기 iterator와 차이는 “다음 값 요청이 I/O나 대기를 포함할 수 있다”는 점이다. 예를 들어 database cursor, websocket message stream, paginated API stream은 다음 항목이 즉시 메모리에 없을 수 있다.

그렇다고 모든 collection을 async iterator로 만들 필요는 없다. 메모리 안의 list를 단순히 순회하는데 async protocol을 쓰면 suspension 의미가 없는 복잡성만 추가된다.

---

## 2. `StopAsyncIteration` — 비동기 반복의 정상 종료 신호다

동기 iterator가 `StopIteration`으로 종료를 알리듯 async iterator는 `StopAsyncIteration`을 사용한다.

```python
async def __anext__(self):
    item = await read_next()
    if item is END:
        raise StopAsyncIteration
    return item
```

이 예외는 “실패”와 다르다. stream이 정상적으로 끝났다는 프로토콜 제어 신호다.

다음은 구분해야 한다.

```text
StopAsyncIteration  → 정상 iteration 종료
TimeoutError        → 다음 값을 기다리다 시간 초과
ConnectionError     → stream transport 실패
CancelledError      → 소비 task 취소 경로
```

종료와 실패를 같은 sentinel `None`으로 합치면 실제 `None` 데이터와 구분이 안 될 수 있고, 실패 원인도 사라진다.

프로토콜이 이미 종료 신호를 제공한다면 그 신호를 정확히 사용해야 소비 측 `async for`가 정상적으로 멈춘다.

---

## 3. `async for` desugaring — 매 항목마다 suspension 지점이 생긴다

개념적으로 다음 코드를 보자.

```python
async for item in stream:
    process(item)
```

대략적인 사고 모델은 다음과 같다.

```text
iterator = stream.__aiter__()
loop:
    item = await iterator.__anext__()
    body(item)
```

실제 언어 semantics의 세부를 이 의사 코드 하나로 완전히 대체할 수는 없지만, 성능과 cancellation을 이해하기에는 중요한 모델이다.

매 `__anext__` 호출 사이에 event loop가 다른 task를 실행할 수 있다. 따라서 iterator 내부 mutable state는 “한 줄 다음에 바로 이어서 실행된다”는 동기적 직관으로 설계하면 안 된다.

또 body가 느리면 producer가 자연스럽게 다음 값을 요청받지 못하는 pull-based backpressure가 생길 수 있다. 반대로 내부 producer가 background task로 계속 데이터를 쌓는 구조라면 별도의 queue capacity와 overflow 정책이 필요하다.

`async for` 문법만 보고 전체 backpressure 모델을 단정하면 안 된다.

---

## 4. Async context protocol — 진입과 종료 자체가 awaitable일 수 있다

원격 connection pool에서 connection을 얻거나 transaction 종료 시 network round-trip이 필요한 경우, 동기 `__enter__`/`__exit__`로는 자연스럽게 표현하기 어렵다.

비동기 context manager는 `__aenter__`와 `__aexit__`을 제공한다.

```python
class AsyncConnection:
    async def __aenter__(self):
        self.conn = await acquire_connection()
        return self.conn

    async def __aexit__(self, exc_type, exc, tb):
        await release_connection(self.conn)
        return False
```

사용:

```python
async with AsyncConnection() as conn:
    await conn.execute("SELECT 1")
```

여기서는 **진입 전에도 suspension, body 안에도 suspension, 종료 중에도 suspension**이 가능하다. 즉 resource lifetime 사이사이에 scheduler 개입 지점이 존재한다.

그래서 공유 state나 timeout을 설계할 때 “with 블록은 하나의 연속된 atomic 구간”처럼 생각하면 안 된다.

---

## 5. `__aenter__` / `__aexit__` — ownership은 await 경계를 넘어 유지된다

Async resource 획득이 여러 단계라면 P69의 부분 획득 문제에 suspension이 추가된다.

```python
class RemoteSession:
    async def __aenter__(self):
        self.transport = await open_transport()
        try:
            self.auth = await authenticate(self.transport)
        except BaseException:
            await close_transport(self.transport)
            raise
        return self
```

인증 단계에서 실패하거나 취소되어도 이미 연 transport는 정리돼야 한다.

여기서 `except Exception`과 `except BaseException` 범위를 무심코 선택하면 cancellation 처리와도 연결될 수 있으므로 현재 Python 버전의 cancellation 예외 계층과 framework contract를 확인해야 한다. 더 중요한 일반 원칙은 **취소도 enter 실패 경로의 하나로 포함해 ownership 누수를 막는 것**이다.

`__aexit__`에서도 정상 종료와 body 실패를 구분해 commit/rollback 같은 작업을 await할 수 있다.

```text
body 성공 → await commit()
body 실패 → await rollback()
always      → await release()
```

하지만 commit 자체가 실패했을 때 connection을 어떤 상태로 pool에 돌려보낼지도 별도 정책이다.

---

## 6. Cancellation cleanup — 취소는 cleanup을 생략하라는 뜻이 아니다

비동기 코드에서 cancellation은 정상적인 제어 흐름의 일부다. 문제는 task가 취소될 때 resource cleanup도 await를 필요로 할 수 있다는 점이다.

```python
async with lease() as resource:
    await use(resource)
```

`use()` 도중 cancellation이 들어오면 `__aexit__`가 resource를 반납할 기회를 가져야 한다. 그러나 cleanup 안의 await도 취소와 상호작용할 수 있다.

무조건 cancellation을 삼키면 상위 orchestration이 task 종료를 알 수 없고, 반대로 cleanup을 전혀 보호하지 않으면 lock/connection이 누수될 수 있다.

따라서 cancellation-safe resource manager는 다음을 구분한다.

1. 취소 요청을 관찰한다.
2. 반드시 필요한 최소 cleanup을 수행한다.
3. 원래 cancellation 의미를 가능한 한 보존한다.
4. cleanup 실패가 추가되면 원인 관계를 추적 가능하게 만든다.

`asyncio.shield` 같은 도구가 존재하지만 “cleanup이면 전부 shield”가 정답은 아니다. shield는 cancellation propagation과 lifetime을 바꾸므로 제한적으로 사용해야 한다.

---

## 7. Sync/async boundary — 같은 기능이라도 프로토콜을 섞어 쓰지 않는다

동기 iterator에서 비동기 I/O를 몰래 실행하려 하거나, async context를 동기 `with`에 끼워 넣으면 경계가 불명확해진다.

나쁜 방향의 예:

```python
class RemoteRows:
    def __iter__(self):
        # 내부에서 event loop를 새로 돌려 remote I/O를 숨김
        ...
```

호출자는 평범한 `for`라고 생각하지만 실제로 blocking I/O와 event loop 문제가 숨는다.

더 명확한 API는 실행 모델을 문법에 드러낸다.

```python
async for row in remote_rows:
    ...
```

반대로 이미 메모리에 모두 있는 결과는 동기 collection으로 materialize해서 반환할 수 있다.

Library boundary에서는 다음을 명시한다.

```text
returns: list / Iterator / AsyncIterator
acquire: with / async with
callback: sync callable / async callable
blocking I/O: yes/no
```

동기와 비동기의 차이는 속도 차이가 아니라 **scheduler와 suspension이 contract에 노출되는가**의 차이다.

---

## 8. Async protocol contract — 종료·실패·취소·backpressure를 따로 적는다

비동기 stream/context를 설계할 때는 다음 표가 유용하다.

```text
producer model: pull on __anext__
end signal: StopAsyncIteration
transient failure: ConnectionError
idle timeout: TimeoutError
consumer cancellation: propagate after cleanup
buffering: max 100 items
resource acquire: async with
release: await pool.release
reusable iterator: no
multiple consumers: no
```

테스트 역시 서로 다른 실패 유형을 분리해야 한다.

```text
A. 정상 항목 N개 후 정상 종료
B. 중간 transport 실패
C. 다음 항목 대기 중 cancellation
D. __aenter__ 부분 획득 뒤 실패
E. body 실패 후 __aexit__ cleanup
F. cleanup 자체 실패
```

이 구분 없이는 happy path 테스트만 통과한 async abstraction이 운영에서 connection leak이나 hanging task를 만들 수 있다.

Async protocol의 핵심은 “await를 어디에 붙이나?”가 아니다. **suspension 사이에도 resource ownership과 종료 의미가 보존되는가?**다.

---

## 직관 봉인

- async iteration은 단순 반복이 아니라 매 항목 획득이 await될 수 있는 프로토콜이다.
- `StopAsyncIteration`은 정상 종료 신호이며 transport 실패와 다르다.
- `async for`의 각 next 사이에는 다른 task가 실행될 수 있다.
- `async with`는 enter와 exit 자체도 suspension될 수 있다.
- cancellation은 cleanup 면제 사유가 아니지만 무조건 suppress해서도 안 된다.
- sync/async 경계는 기능 차이가 아니라 scheduler contract 차이다.

## 다음 연결

다음 구간에서는 이 비동기 프로토콜을 더 낮은 실행 모델로 내려가 **coroutine object, awaitable, async generator, context propagation, synchronization primitive**를 추적한다. P55의 orchestration을 반복하지 않고 “task 안에서 실제로 무엇이 suspend되고 다시 이어지는가”를 중심으로 확장한다.
