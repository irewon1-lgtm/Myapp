# PART 46 · Async iterator와 generator — 시간이 걸리는 stream을 backpressure와 cancellation 안에서 다루기

동기 iterator는 다음 값을 요청하면 즉시 값을 주거나 종료한다. 네트워크 socket, message broker, 원격 pagination처럼 다음 값이 준비될 때까지 기다려야 하는 source는 같은 구조를 `await` 가능한 iteration으로 확장해야 한다. Async iterator와 async generator는 값 sequence와 비동기 대기를 하나의 protocol로 묶는다. 핵심은 **값을 언제 생산할지뿐 아니라 consumer가 느릴 때 producer를 어떻게 멈추고, cancellation과 오류에서 resource를 어떻게 정리할지**를 함께 설계하는 것이다.

---

## CHAPTER 01 · async iteration은 `다음 값` 자체가 awaitable인 iteration protocol이다

### 시작 전 용어집

#### 1. async iteration

- **뜻:** 동기 iterator의 `next()`가 즉시 value 또는 종료를 반환한다면 async iterator는 다음 값을 얻는 operation이 기다릴 수 있다.
- **왜 중요한가:** Consumer는 `async for`를 사용하고 producer는 asynchronous iteration protocol을 제공한다.
- **예시:** 동기 iterator의 `next()`가 즉시 value 또는 종료를 반환한다면 …

#### 2. 다음 값

- **뜻:** 네트워크 packet, 원격 API page, database cursor처럼 I/O가 각 item 사이에 존재하는 stream을 자연스럽게 표현할 수 있다.
- **왜 중요한가:** Async iterable과 async iterator도 구분한다.
- **예시:** 네트워크 packet, 원격 API page, database cursor처럼 I/O가 …

#### 3. awaitable

- **뜻:** 매번 새로운 stream state를 만들 수 있는 source와 이미 일부 소비된 iterator object는 같은 contract가 아니다.
- **왜 중요한가:** 함수가 input을 두 번 순회해야 한다면 single-pass async iterator만 받아서는 안 된다.
- **예시:** 매번 새로운 stream state를 만들 수 있는 source와 …

#### 4. iteration protocol

- **뜻:** Protocol을 이해하면 `async for`를 마법으로 보지 않는다.
- **왜 중요한가:** 개념적으로 iterator를 얻고 다음 항목을 await하며 종료 signal을 만날 때까지 반복한다.
- **예시:** Protocol을 이해하면 `async for`를 마법으로 보지 않는다.

Replay 가능한 source인지 명시한다.

  실제 source가 어디서 block되는지, 어떤 exception이 next 단계에서 발생하는지 trace할 수 있다.

---

## CHAPTER 02 · async generator는 local state를 보존하며 `yield`와 `await` 두 suspension 경계를 가진다

### 시작 전 용어집

#### 1. async generator

- **뜻:** Async generator는 `async def` 안에서 `yield`를 사용해 값을 생산하고 필요할 때 `await`로 외부 I/O를 기다릴 수 있다.
- **왜 중요한가:** 일반 generator가 consumer 요청 사이에 local state를 보존하는 것처럼 async generator도 frame state를 유지한다.
- **예시:** 하지만 generator object가 살아 있는 동안 이 state와 …

#### 2. local state

- **뜻:** 차이는 값 생산 사이에 event loop에 제어권을 양보하는 asynchronous wait가 존재할 수 있다는 점이다.
- **왜 중요한가:** 이 구조에서는 한 function 안에 pagination token, connection, partial parser state를 유지할 수 있다.
- **예시:** 하지만 generator object가 살아 있는 동안 이 state와 …

#### 3. yield

- **뜻:** 하지만 generator object가 살아 있는 동안 이 state와 reference도 함께 살아 있으므로 큰 buffer나 connection lifetime이 예상보다 길어질 수 있다.
- **왜 중요한가:** 지연 실행이 memory/resource를 자동 절약하는 것은 아니다.
- **예시:** `stream = fetch_rows()`가 성공했다고 remote source가 정상이라는 뜻이 …

#### 4. await

- **뜻:** Exception도 generator 생성 시점보다 실제 iteration 중에 발생한다.
- **왜 중요한가:** `stream = fetch_rows()`가 성공했다고 remote source가 정상이라는 뜻이 아니다.
- **예시:** Exception도 generator 생성 시점보다 실제 iteration 중에 발생한다.

Consumer가 첫 item을 요청할 때 authentication error가 발생할 수 있고 100번째 item에서 malformed payload를 만날 수도 있다.

---

## CHAPTER 03 · `async for`는 consumer 속도가 producer 진행을 제한하는 자연스러운 backpressure를 만든다

### 시작 전 용어집

#### 1. async for

- **뜻:** Consumer가 한 항목을 처리한 뒤 다음 항목을 요청하는 구조에서는 producer가 무제한 앞서갈 수 없다.
- **왜 중요한가:** Consumer가 느리면 다음 `__anext__` 요청이 늦어지고 producer도 그만큼 진행을 멈춘다.
- **예시:** Consumer가 한 항목을 처리한 뒤 다음 항목을 요청하는 …

#### 2. consumer

- **뜻:** Remote API pagination에서도 consumer가 필요한 만큼만 page를 요청하면 불필요한 network call을 줄일 수 있다.
- **왜 중요한가:** 반대로 latency를 숨기려고 next page를 prefetch하면 buffer와 cancellation policy가 추가된다.
- **예시:** Remote API pagination에서도 consumer가 필요한 만큼만 page를 요청하면 …

#### 3. producer

- **뜻:** 그러나 producer가 내부 background task를 따로 만들어 queue에 계속 값을 쌓는다면 이 자연스러운 backpressure가 사라질 수 있다.
- **왜 중요한가:** Queue maximum size를 정하고 full일 때 producer가 await하도록 만들어 memory가 무한히 증가하지 않게 한다.
- **예시:** 그러나 producer가 내부 background task를 따로 만들어 queue에 …

#### 4. backpressure

- **뜻:** 이것은 bounded pull-based stream의 기본 backpressure 형태다.
- **왜 중요한가:** “async라서 non-blocking”과 “무제한 buffering이 안전함”은 전혀 다른 말이다.
- **예시:** 이것은 bounded pull-based stream의 기본 backpressure 형태다.

Throughput과 memory budget 사이의 trade-off를 명시한다.

---

## CHAPTER 04 · stream이 resource를 열면 iterator lifetime과 resource lifetime이 결합된다

### 시작 전 용어집

#### 1. stream

- **뜻:** Async generator가 database cursor나 HTTP response를 열고 item을 yield한다면 stream이 끝날 때까지 해당 resource가 살아 있을 수 있다.
- **왜 중요한가:** Consumer가 첫 10개만 읽고 iteration을 중단하면 generator cleanup이 제대로 실행되는지 중요하다.
- **예시:** Async generator가 database cursor나 HTTP response를 열고 item을 …

#### 2. resource

- **뜻:** Resource pool slot을 오래 점유하면 다른 요청 throughput이 떨어진다.
- **왜 중요한가:** Generator 내부에서 `async with`를 사용해 resource를 소유할 수 있지만 조기 종료와 exception에서도 exit protocol이 실행되어야 한다.
- **예시:** Resource pool slot을 오래 점유하면 다른 요청 throughput이 …

#### 3. iterator

- **뜻:** Consumer가 generator object를 그냥 버리는 것에만 cleanup timing을 맡기지 않고 close semantics를 명확히 한다.
- **왜 중요한가:** Framework가 `aclose()`를 언제 호출하는지 확인한다.
- **예시:** Consumer가 generator object를 그냥 버리는 것에만 cleanup timing을 …

#### 4. generator

- **뜻:** 때로는 resource를 빨리 닫기 위해 page 하나를 list로 materialize한 뒤 connection을 반환하는 편이 낫다.
- **왜 중요한가:** Lazy stream이 항상 더 효율적이라는 규칙은 없다.
- **예시:** 때로는 resource를 빨리 닫기 위해 page 하나를 list로 …

Memory 절약과 scarce resource occupancy를 함께 측정한다.

---

## CHAPTER 05 · cancellation은 async stream의 producer와 consumer 양쪽에 전달될 수 있다

### 시작 전 용어집

#### 1. cancellation

- **뜻:** Consumer task가 취소되면 현재 기다리던 `__anext__` 또는 producer 내부 await가 cancellation을 받을 수 있다.
- **왜 중요한가:** 이때 partially processed item, open connection, lock, acknowledgement state를 어떻게 정리할지 필요하다.
- **예시:** Consumer task가 취소되면 현재 기다리던 `__anext__` 또는 producer …

#### 2. async stream

- **뜻:** Shutdown 때 여러 async stream을 동시에 닫는다면 cleanup 역시 시간이 걸릴 수 있다.
- **왜 중요한가:** Grace period와 강제 종료 경계를 정하고, 미완료 item을 다시 처리할 durable checkpoint가 있는지 확인한다.
- **예시:** Shutdown 때 여러 async stream을 동시에 닫는다면 cleanup …

#### 3. producer

- **뜻:** Cancellation을 일반 exception handler가 삼키면 상위 scope는 작업이 멈췄다고 생각하지만 producer는 계속 살아 있을 수 있다.
- **왜 중요한가:** Generator의 `finally`와 async context manager를 사용해 cleanup을 수행하고 cancellation을 적절히 전파한다.
- **예시:** Cancellation을 일반 exception handler가 삼키면 상위 scope는 작업이 …

#### 4. consumer

- **뜻:** Message stream에서 item을 읽은 뒤 처리 완료 전에 취소되었다면 acknowledgement를 하지 않아 재전달하게 할지, 이미 side effect가 발생했으므로 idempotency key로 중복을 견딜지 domain policy가 필요하다.
- **예시:** Message stream에서 item을 읽은 뒤 처리 완료 전에 …

---

## CHAPTER 06 · batch와 window는 무한 stream을 유한 처리 단위로 바꾼다

### 시작 전 용어집

#### 1. batch

- **뜻:** Item 하나씩 remote storage에 쓰면 overhead가 크기 때문에 N개씩 batch를 만들어 처리할 수 있다.
- **왜 중요한가:** Batch size가 너무 작으면 call overhead가 커지고 너무 크면 memory와 실패 시 재처리량이 증가한다.
- **예시:** Item 하나씩 remote storage에 쓰면 overhead가 크기 때문에 …

#### 2. window

- **뜻:** 무한 event stream에서는 최근 1분, 100개, session 단위처럼 window를 정의해 aggregate한다.
- **왜 중요한가:** Window boundary가 result semantics를 결정하고 late event를 어떻게 처리할지 정책이 필요하다.
- **예시:** 무한 event stream에서는 최근 1분, 100개, session 단위처럼 …

#### 3. stream

- **뜻:** 단순 async iteration syntax를 넘어 stream processing model로 확장되는 지점이다.
- **왜 중요한가:** Batch operation이 일부 item만 성공할 수 있다면 result도 per-item status를 보존해야 한다.
- **예시:** 단순 async iteration syntax를 넘어 stream processing model로 …

#### 4. 실패

- **뜻:** 첫 실패에서 전체 batch를 retry하면 이미 성공한 item이 중복될 수 있으므로 idempotency와 partial failure model을 함께 설계한다.
- **왜 중요한가:** Count 기준, byte 기준, 시간 기준을 조합해 flush policy를 만들 수 있다.
- **예시:** 첫 실패에서 전체 batch를 retry하면 이미 성공한 item이 …

---

## CHAPTER 07 · 여러 async source를 merge할 때 arrival order와 fairness가 새로운 계약이 된다

### 시작 전 용어집

#### 1. async source

- **뜻:** 두 stream A와 B를 하나로 합치면 어느 source의 item을 먼저 내보낼지 결정해야 한다.
- **왜 중요한가:** 단순히 A를 모두 소비한 뒤 B를 읽으면 B는 오래 기다릴 수 있다.
- **예시:** 두 stream A와 B를 하나로 합치면 어느 source의 …

#### 2. merge

- **뜻:** 두 source의 next item을 concurrent하게 기다려 먼저 준비된 것을 내보내면 arrival order를 보존할 수 있지만 task management와 cancellation이 복잡해진다.
- **왜 중요한가:** 한 source가 매우 빠르면 다른 source가 starvation될 수 있다.
- **예시:** 두 source의 next item을 concurrent하게 기다려 먼저 준비된 …

#### 3. arrival order

- **뜻:** Round-robin fairness, priority, timestamp ordering 중 어떤 정책이 필요한지 domain이 결정한다.
- **왜 중요한가:** Event time ordering을 요구한다면 네트워크 도착 순서만으로 충분하지 않고 buffering watermark 같은 개념이 필요할 수 있다.
- **예시:** Round-robin fairness, priority, timestamp ordering 중 어떤 정책이 …

#### 4. fairness

- **뜻:** Fan-in 구현에서 child task exception을 first-error 하나로 버리지 않는다.
- **왜 중요한가:** 여러 source가 동시에 실패할 수 있으므로 structured failure model을 사용하고 각 source identity를 error context에 포함한다.
- **예시:** Fan-in 구현에서 child task exception을 first-error 하나로 버리지 …

---

## CHAPTER 08 · stream error policy는 `중단`, `건너뜀`, `격리`를 구분한다

### 시작 전 용어집

#### 1. stream

- **뜻:** 하나의 malformed item을 만났을 때 전체 stream을 종료할지 그 item만 버리고 계속할지 정해야 한다.
- **왜 중요한가:** Financial ledger처럼 item 누락이 전체 정확성을 깨면 fail-fast가 적합할 수 있고 telemetry처럼 일부 bad record를 quarantine하고 계속 처리하는 것이 더 실용적일 수 있다.
- **예시:** 하나의 malformed item을 만났을 때 전체 stream을 종료할지 …

#### 2. policy

- **뜻:** “로그 남기고 continue”는 정책이 아니라 데이터 손실일 수 있다.
- **왜 중요한가:** Skip할 경우 원본 item, error category, 위치를 dead-letter/quarantine storage에 보존해 재처리할 수 있게 한다.
- **예시:** “로그 남기고 continue”는 정책이 아니라 데이터 손실일 수 …

#### 3. 중단

- **뜻:** Error rate가 일정 threshold를 넘으면 upstream contract가 깨졌다고 보고 전체 pipeline을 멈출 수도 있다.
- **왜 중요한가:** Retry는 transient I/O failure와 deterministic parse failure를 구분한다.
- **예시:** Error rate가 일정 threshold를 넘으면 upstream contract가 깨졌다고 …

#### 4. 건너뜀

- **뜻:** 같은 malformed bytes를 세 번 parse해도 성공하지 않는다.
- **왜 중요한가:** Failure classification을 stream stage별로 유지한다.
- **예시:** 같은 malformed bytes를 세 번 parse해도 성공하지 않는다.

---

## CHAPTER 09 · async stream test는 값 목록뿐 아니라 timing·cancellation·cleanup을 검증한다

### 시작 전 용어집

#### 1. async stream

- **뜻:** 정상 입력 세 개를 넣어 세 결과가 나오는지만 확인하면 async 특성을 거의 검증하지 못한다.
- **왜 중요한가:** Fake producer를 pause시켜 consumer backpressure가 작동하는지, 중간 cancellation에서 connection이 닫히는지, source error가 올바른 위치에서 전파되는지 확인한다.
- **예시:** 정상 입력 세 개를 넣어 세 결과가 나오는지만 …

#### 2. test

- **뜻:** Merge test에서는 두 source completion 순서를 의도적으로 바꿔 fairness와 ordering policy를 확인한다.
- **왜 중요한가:** Concurrency test가 우연한 scheduler에 의존하지 않게 barrier와 event를 사용해 필요한 interleaving을 만든다.
- **예시:** Merge test에서는 두 source completion 순서를 의도적으로 바꿔 …

#### 3. timing

- **뜻:** Virtual/fake clock을 사용하면 time window와 timeout을 real sleep 없이 빠르게 검증할 수 있다.
- **왜 중요한가:** Queue maximum을 아주 작게 두고 producer가 block되는 지점을 관찰하면 unbounded buffering regression을 찾을 수 있다.
- **예시:** Virtual/fake clock을 사용하면 time window와 timeout을 real sleep …

---

## CHAPTER 10 · async stream contract는 순서·single-pass·backpressure·lifetime·failure 다섯 축으로 정리한다

### 시작 전 용어집

#### 1. async stream

- **뜻:** Async stream programming의 핵심은 **기다리는 sequence를 문법적으로 표현하는 것이 아니라 producer와 consumer 사이의 속도·resource·cancellation·오류 책임을 한 lifecycle로 설계하는 것**이다.
- **왜 중요한가:** Async iterable을 API로 제공할 때 item type만 문서화하면 부족하다.
- **예시:** Async stream programming의 핵심은 **기다리는 sequence를 문법적으로 표현하는 …

#### 2. contract

- **뜻:** 이 contract가 선명하면 implementation을 remote pagination에서 message queue로 바꾸더라도 consumer가 의존하는 semantics를 유지할 수 있다.
- **왜 중요한가:** 반대로 단순 `AsyncIterator[T]` type만 같고 ordering/failure가 바뀌면 실질적으로 다른 API다.
- **예시:** 이 contract가 선명하면 implementation을 remote pagination에서 message queue로 …

#### 3. single-pass

- **뜻:** Input order를 보존하는지, 한 번만 소비 가능한지, consumer 속도가 upstream을 제한하는지, 어떤 resource가 iteration 동안 살아 있는지, item failure가 전체 stream에 어떤 영향을 주는지 함께 명시해야 한다.
- **예시:** Input order를 보존하는지, 한 번만 소비 가능한지, consumer …
