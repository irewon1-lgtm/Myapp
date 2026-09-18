# PART 88 · Readiness와 selector loop — nonblocking I/O를 event source로 바꾸기

Blocking socket은 한 operation이 끝날 때까지 현재 thread를 멈춘다. Nonblocking mode와 readiness selector를 사용하면 하나의 thread가 여러 file descriptor의 “지금 진행 가능한 상태”를 관찰하고 준비된 대상만 처리할 수 있다. Event loop의 밑바닥을 이해하려면 **readiness notification이 data 자체가 아니라 다시 시도할 기회라는 점**을 명확히 해야 한다.

---

## CHAPTER 01 · blocking과 nonblocking은 같은 I/O operation의 대기 방식을 바꾼다

### 시작 전 용어집

#### 1. lock

- **뜻:** Blocking `recv`는 data가 준비될 때까지 기다릴 수 있다.
- **왜 중요한가:** Nonblocking socket은 즉시 진행할 수 없으면 caller에게 지금은 불가능하다는 상태를 돌려주고 control을 반환한다.
- **예시:** Blocking `recv`는 data가 준비될 때까지 기다릴 수 있다.

#### 2. operation

- **뜻:** Nonblocking은 operation을 더 빠르게 만드는 기술이 아니라 **대기를 중앙 scheduler로 이동시키는 구조**다.
- **왜 중요한가:** 이 모델에서는 application이 busy loop로 계속 재시도하면 CPU를 낭비한다.
- **예시:** Nonblocking은 operation을 더 빠르게 만드는 기술이 아니라 **대기를 …

#### 3. socket

- **뜻:** Selector는 여러 descriptor 중 어떤 것이 준비되었는지 효율적으로 기다리는 계층을 제공한다.
- **예시:** Selector는 여러 descriptor 중 어떤 것이 준비되었는지 효율적으로 …

---

## CHAPTER 02 · readiness는 read/write가 반드시 끝까지 성공한다는 보장이 아니다

### 시작 전 용어집

#### 1. readiness

- **뜻:** 따라서 selector event를 받으면 가능한 만큼 진행하고, 남은 state를 connection object에 보관한 뒤 다음 readiness를 기다린다.
- **왜 중요한가:** P87의 partial I/O model이 여기서 그대로 이어진다.
- **예시:** 따라서 selector event를 받으면 가능한 만큼 진행하고, 남은 …

#### 2. write

- **뜻:** 일부 write 후 다시 would-block 상태가 될 수 있다.
- **왜 중요한가:** Readable notification은 보통 read를 시도할 이유가 생겼다는 뜻이지 원하는 application frame 전체가 도착했다는 뜻이 아니다.
- **예시:** 일부 write 후 다시 would-block 상태가 될 수 …

#### 3. frame

- **뜻:** 한 번 읽고도 partial payload일 수 있다.
- **왜 중요한가:** Writable notification도 application buffer 전체를 무제한 전송할 수 있다는 의미가 아니다.
- **예시:** 한 번 읽고도 partial payload일 수 있다.

---

## CHAPTER 03 · selector registration은 file descriptor와 관심 event를 event loop에 등록한다

### 시작 전 용어집

#### 1. selector

- **뜻:** 한 connection의 state를 global dict와 selector data에 중복 저장하면 lifecycle mismatch가 생기기 쉽다.
- **왜 중요한가:** 닫힌 fd 번호가 나중에 다른 socket에 재사용될 수 있으므로 stale registration을 남기지 않는다.
- **예시:** import selectors / sel = selectors.DefaultSelector()

#### 2. file descriptor

- **뜻:** Registration에는 대상 descriptor, read/write interest, application state를 연결할 수 있다.
- **왜 중요한가:** Event가 오면 key를 통해 해당 connection의 parser/buffer를 찾는다.
- **예시:** import selectors / sel = selectors.DefaultSelector()

```python
import selectors

sel = selectors.DefaultSelector()
sel.register(sock, selectors.EVENT_READ, data=connection_state)
```

 

 Owner와 close path를 하나로 정한다.

Descriptor reuse도 주의한다. 

---

## CHAPTER 04 · interest set은 현재 필요한 event만 구독하도록 동적으로 바뀔 수 있다

### 시작 전 용어집

#### 1. interest set

- **뜻:** Interest set 자체가 connection state machine의 일부다.
- **왜 중요한가:** Buffer를 다 비웠는데 write interest를 제거하지 않으면 busy loop와 CPU 사용량 증가가 생길 수 있다.
- **예시:** outgoing empty -> READ / outgoing queued -> …

#### 2. event

- **뜻:** 항상 write-ready event를 구독하면 대부분의 socket이 계속 writable이라 event loop가 불필요하게 깨어날 수 있다.
- **왜 중요한가:** Outgoing buffer가 비어 있을 때는 read만 보고, data가 생기면 write interest를 추가하는 방식이 일반적이다.
- **예시:** outgoing empty -> READ / outgoing queued -> …

#### 3. socket

- **뜻:** Event subscription을 현재 필요한 작업과 동기화한다.
- **예시:** outgoing empty -> READ / outgoing queued -> …

```text
outgoing empty  -> READ
outgoing queued -> READ | WRITE
flushed         -> READ
```

 


---

## CHAPTER 05 · wakeup mechanism은 다른 thread나 signal이 event loop를 깨우게 한다

### 시작 전 용어집

#### 1. wakeup mechanism

- **뜻:** Self-pipe, socketpair 같은 wakeup mechanism을 사용해 selector가 관찰하는 fd에 event를 만들 수 있다.
- **왜 중요한가:** Wakeup byte 자체는 application data가 아니라 “queue를 다시 확인하라”는 scheduler signal이다.
- **예시:** Self-pipe, socketpair 같은 wakeup mechanism을 사용해 selector가 관찰하는 …

#### 2. thread

- **뜻:** Selector가 긴 timeout으로 잠들어 있을 때 다른 thread가 새 작업을 enqueue하면 loop를 즉시 깨울 방법이 필요할 수 있다.
- **왜 중요한가:** 여러 signal을 하나로 coalesce할 수 있는지, wakeup buffer가 가득 찰 가능성이 있는지 고려한다.
- **예시:** Selector가 긴 timeout으로 잠들어 있을 때 다른 thread가 …

#### 3. signal

- **뜻:** 이 구조를 이해하면 high-level event loop의 `call_soon_threadsafe` 같은 API가 왜 별도 wakeup path를 필요로 하는지 설명할 수 있다.
- **예시:** 이 구조를 이해하면 high-level event loop의 `call_soon_threadsafe` 같은 …

---

## CHAPTER 06 · 한 connection을 너무 오래 처리하면 ready한 다른 connection이 starvation될 수 있다

### 시작 전 용어집

#### 1. connection

- **뜻:** Selector가 100개 ready event를 반환했는데 첫 connection의 parser가 CPU를 오래 쓰면 나머지 99개는 기다린다.
- **왜 중요한가:** Single-thread event loop에서는 handler가 짧게 끝나야 fairness가 유지된다.
- **예시:** Selector가 100개 ready event를 반환했는데 첫 connection의 parser가 …

#### 2. ready

- **뜻:** 한 event에서 처리할 byte/message budget을 제한하거나 CPU-heavy 작업을 executor로 넘길 수 있다.
- **왜 중요한가:** Throughput만 보지 말고 per-connection tail latency를 측정한다.
- **예시:** 한 event에서 처리할 byte/message budget을 제한하거나 CPU-heavy 작업을 …

#### 3. starvation

- **뜻:** 악의적인 peer가 매우 많은 parse work를 유발할 수 있으므로 request/frame당 CPU budget도 resource safety의 일부다.
- **예시:** 악의적인 peer가 매우 많은 parse work를 유발할 수 …

---

## CHAPTER 07 · close와 unregister 순서가 어긋나면 stale event와 descriptor reuse 문제가 생긴다

### 시작 전 용어집

#### 1. close

- **뜻:** Connection을 닫을 때 selector registration을 제거하고 socket을 close하며 application state를 폐기하는 순서를 일관되게 관리한다.
- **왜 중요한가:** Event batch를 이미 가져온 뒤 handler 실행 전에 다른 path가 socket을 닫는 race도 고려한다.
- **예시:** Connection을 닫을 때 selector registration을 제거하고 socket을 close하며 …

#### 2. unregister

- **뜻:** Exception path에서도 unregister를 빼먹지 않도록 connection close를 한 함수에 모은다.
- **왜 중요한가:** Event handler는 connection state가 이미 closing/closed인지 확인할 수 있어야 한다.
- **예시:** Exception path에서도 unregister를 빼먹지 않도록 connection close를 한 …

#### 3. stale event

- **뜻:** 같은 connection에 두 번 close가 들어와도 안전한 idempotent cleanup이 유용하다.
- **예시:** 같은 connection에 두 번 close가 들어와도 안전한 idempotent …

---

## CHAPTER 08 · readiness contract는 I/O 진행 가능성과 application completion을 구분한다

### 시작 전 용어집

#### 1. readiness

- **뜻:** Readiness를 받은 뒤 얼마나 처리할지, would-block에서 어떻게 state를 보존할지, close를 누가 책임질지 정해야 한다.
- **왜 중요한가:** 테스트에서는 partial read/write, 항상 writable idle socket, peer EOF, handler exception, close 후 stale event, 많은 ready connection에서 fairness를 확인한다.
- **예시:** Readiness를 받은 뒤 얼마나 처리할지, would-block에서 어떻게 state를 …

#### 2. application completion

- **뜻:** Selector 기반 loop는 준비된 fd를 알려줄 뿐 message framing, timeout, backpressure, connection state를 대신 설계하지 않는다.
- **왜 중요한가:** 이 PART의 핵심은 **readiness를 data arrival 완료 신호로 오해하지 않고, nonblocking operation을 다시 시도할 수 있는 scheduler event로 해석해 connection state machine과 결합하는 것**이다.
- **예시:** Selector 기반 loop는 준비된 fd를 알려줄 뿐 message …
