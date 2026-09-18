# PART 87 · Socket stream framing — partial send/recv·message boundary·backpressure를 직접 다루기

TCP socket을 처음 쓰면 `send(message)` 한 번과 `recv()` 한 번이 메시지 하나에 대응한다고 생각하기 쉽다. 하지만 byte stream은 application message boundary를 보존하지 않는다. 한 번 보낸 data가 여러 recv로 나뉘거나 여러 send가 한 recv에 합쳐질 수 있다. 따라서 network protocol은 **stream 위에 framing 규칙을 별도로 만들어야** 한다.

---

## CHAPTER 01 · stream transport는 message가 아니라 순서 있는 byte sequence를 제공한다

### 시작 전 용어집

#### 1. stream

- **뜻:** Stream과 message를 혼동하면 개발 환경의 작은 payload에서는 통과하다 실제 네트워크 지연·buffering에서 깨지는 전형적인 버그가 생긴다.
- **왜 중요한가:** 송신자가 `b"ABC"`, `b"DEF"`를 두 번 보냈다고 수신자가 정확히 두 번 `recv()`해서 같은 경계를 얻는 것은 아니다.
- **예시:** Stream과 message를 혼동하면 개발 환경의 작은 payload에서는 통과하다 …

#### 2. message

- **뜻:** 수신자는 `b"ABCDEF"`를 한 번에 받거나 `b"A"`, `b"BCDE"`, `b"F"`처럼 나눠 받을 수 있다.
- **왜 중요한가:** Application protocol이 record 단위를 필요로 한다면 length prefix, delimiter, fixed size, higher-level framing을 사용한다.
- **예시:** 수신자는 `b"ABCDEF"`를 한 번에 받거나 `b"A"`, `b"BCDE"`, `b"F"`처럼 …

---

**현장 디버깅 점검 — CHAPTER 01 · stream transport는 message가 아니라 순서 있는 byte sequence를 제공한다**
CHAPTER 01 · stream transport는 message가 아니라 순서 있는 byte sequence를 제공한다 문제를 실제 환경에서 확인할 때는 정상 경로와 실패 경로를 분리해 같은 입력으로 재현 가능하게 만든다. 실행 전에는 기대 상태를 적고, 실행 중에는 입력값·중간 상태·반환값·로그 시각을 같은 순서로 수집한다. 실패 주입은 지연, 부분 데이터, 잘못된 형식, 취소, 재시도처럼 한 조건만 선택해 넣고 다른 조건은 고정한다. 수정 뒤에는 정상 입력과 실패 입력을 모두 다시 실행한다. 통과 기준은 예외가 단순히 사라지는 것이 아니라 데이터 손실·중복·자원 누수 없이 기대 상태로 끝나고, 실패 시에도 정해진 복구 또는 오류 경로가 관찰되는 것이다.
## CHAPTER 02 · `send`는 요청한 모든 bytes를 한 번에 전송했다고 보장하지 않을 수 있다

### 시작 전 용어집

#### 1. send

- **뜻:** Low-level `send`는 일부 bytes만 받아들이고 실제 전송된 길이를 반환할 수 있다.
- **왜 중요한가:** 남은 bytes는 caller가 다시 보내야 한다.
- **예시:** view = memoryview(payload) / while view:

#### 2. bytes

- **뜻:** 고수준 `sendall`은 이 반복을 대신할 수 있지만 timeout과 failure가 발생했을 때 어느 정도까지 전달됐는지 application message 관점에서 항상 알 수 있는 것은 아니다.
- **왜 중요한가:** 따라서 idempotency가 필요한 protocol은 application-level request ID와 acknowledgement를 사용한다.
- **예시:** view = memoryview(payload) / while view:

#### 3. memoryview

- **뜻:** Socket write 성공을 business transaction 성공으로 동일시하지 않는다.
- **예시:** view = memoryview(payload) / while view:

```python
view = memoryview(payload)
while view:
    sent = sock.send(view)
    view = view[sent:]
```


 

---

**현장 디버깅 점검 — CHAPTER 02 · `send`는 요청한 모든 bytes를 한 번에 전송했다고 보장하지 않을 수 있다**
CHAPTER 02 · `send`는 요청한 모든 bytes를 한 번에 전송했다고 보장하지 않을 수 있다 문제를 실제 환경에서 확인할 때는 정상 경로와 실패 경로를 분리해 같은 입력으로 재현 가능하게 만든다. 실행 전에는 기대 상태를 적고, 실행 중에는 입력값·중간 상태·반환값·로그 시각을 같은 순서로 수집한다. 실패 주입은 지연, 부분 데이터, 잘못된 형식, 취소, 재시도처럼 한 조건만 선택해 넣고 다른 조건은 고정한다. 수정 뒤에는 정상 입력과 실패 입력을 모두 다시 실행한다. 통과 기준은 예외가 단순히 사라지는 것이 아니라 데이터 손실·중복·자원 누수 없이 기대 상태로 끝나고, 실패 시에도 정해진 복구 또는 오류 경로가 관찰되는 것이다.
## CHAPTER 03 · `recv`는 원하는 message 길이보다 적게 반환하는 것이 정상이다

### 시작 전 용어집

#### 1. recv

- **뜻:** `recv(4096)`은 정확히 4096 bytes를 반환하라는 요청이 아니다.
- **왜 중요한가:** 현재 도착해 읽을 수 있는 최대 범위에서 더 적은 data를 반환할 수 있다.
- **예시:** async def read_exactly(reader, n): / return await reader.readexactly(n)

#### 2. message

- **뜻:** Length-prefixed frame이라면 먼저 header 크기만큼 정확히 누적해서 읽고, header가 말하는 payload length만큼 다시 누적한다.
- **왜 중요한가:** 고수준 API가 exact-read를 제공하더라도 EOF와 timeout failure를 구분한다.
- **예시:** async def read_exactly(reader, n): / return await reader.readexactly(n)

#### 3. bytes

- **뜻:** Partial frame을 정상 메시지로 넘기지 않는다.
- **예시:** async def read_exactly(reader, n): / return await reader.readexactly(n)

```python
async def read_exactly(reader, n):
    return await reader.readexactly(n)
```

 

---

## CHAPTER 04 · framing은 길이 prefix·delimiter·fixed record 중 protocol 의미에 맞게 선택한다

### 시작 전 용어집

#### 1. framing

- **뜻:** Framing은 parser 편의뿐 아니라 resource safety boundary다.
- **왜 중요한가:** Length prefix는 binary payload와 embedded delimiter를 안전하게 다룰 수 있지만 header parsing과 최대 길이 제한이 필요하다.
- **예시:** [4-byte length][payload]

#### 2. prefix

- **뜻:** Delimiter 방식은 line protocol에 간단하지만 delimiter escaping 또는 payload 제한이 필요할 수 있다.
- **왜 중요한가:** Length를 신뢰해서 수 GB buffer를 바로 할당하면 malicious peer가 memory exhaustion을 만들 수 있다.
- **예시:** [4-byte length][payload]

#### 3. delimiter

- **뜻:** Header를 parse한 뒤 protocol maximum과 비교하고 초과하면 connection을 종료한다.
- **예시:** [4-byte length][payload]

```text
[4-byte length][payload]
```

 


---

**현장 디버깅 점검 — CHAPTER 04 · framing은 길이 prefix·delimiter·fixed record 중 protocol 의미에 맞게 선택한다**
CHAPTER 04 · framing은 길이 prefix·delimiter·fixed record 중 protocol 의미에 맞게 선택한다 문제를 실제 환경에서 확인할 때는 정상 경로와 실패 경로를 분리해 같은 입력으로 재현 가능하게 만든다. 실행 전에는 기대 상태를 적고, 실행 중에는 입력값·중간 상태·반환값·로그 시각을 같은 순서로 수집한다. 실패 주입은 지연, 부분 데이터, 잘못된 형식, 취소, 재시도처럼 한 조건만 선택해 넣고 다른 조건은 고정한다. 수정 뒤에는 정상 입력과 실패 입력을 모두 다시 실행한다. 통과 기준은 예외가 단순히 사라지는 것이 아니라 데이터 손실·중복·자원 누수 없이 기대 상태로 끝나고, 실패 시에도 정해진 복구 또는 오류 경로가 관찰되는 것이다.
## CHAPTER 05 · timeout과 half-close는 연결 상태를 단순 open/closed 두 값으로 만들지 않는다

### 시작 전 용어집

#### 1. timeout

- **뜻:** `recv()`가 empty bytes를 반환하는 의미와 timeout exception을 구분해야 한다.
- **왜 중요한가:** Timeout은 “peer가 죽었다”의 직접 증거가 아니다.
- **예시:** `recv()`가 empty bytes를 반환하는 의미와 timeout exception을 구분해야 …

#### 2. half-close

- **뜻:** Peer가 write side를 닫아 EOF를 보내도 반대 방향으로는 data를 더 보낼 수 있는 half-close가 가능하다.
- **왜 중요한가:** 단지 지정 시간 안에 operation이 완료되지 않았다는 뜻이다.
- **예시:** Peer가 write side를 닫아 EOF를 보내도 반대 방향으로는 …

#### 3. 상태

- **뜻:** Connection state machine에는 connecting, open, read EOF, write shutdown, closed, failed 같은 더 세밀한 상태가 필요할 수 있다.
- **왜 중요한가:** Retry할지 connection을 폐기할지는 protocol과 idempotency에 따라 정한다.
- **예시:** Connection state machine에는 connecting, open, read EOF, write …

---

## CHAPTER 06 · backpressure는 sender가 receiver보다 빠를 때 buffer가 무한히 커지지 않게 한다

### 시작 전 용어집

#### 1. backpressure

- **뜻:** 이것이 자연스러운 backpressure 신호가 될 수 있다.
- **왜 중요한가:** Application이 write queue를 별도로 무제한 축적하면 OS backpressure를 우회해 memory가 커질 수 있다.
- **예시:** 이것이 자연스러운 backpressure 신호가 될 수 있다.

#### 2. send

- **뜻:** 하지만 receiver가 지속적으로 느리면 sender의 write도 결국 block되거나 await하게 된다.
- **왜 중요한가:** Queue max size, per-connection budget, slow consumer disconnect 정책을 둔다.
- **예시:** 하지만 receiver가 지속적으로 느리면 sender의 write도 결국 block되거나 …

#### 3. receiver

- **뜻:** OS socket buffer가 있으므로 잠깐의 속도 차이는 흡수할 수 있다.
- **왜 중요한가:** Throughput만 높이려 buffer를 키우면 latency와 memory가 함께 증가할 수 있다.
- **예시:** OS socket buffer가 있으므로 잠깐의 속도 차이는 흡수할 …

#### 4. buffer

- **뜻:** Slow-client 시나리오를 load test에 포함한다.
- **예시:** Slow-client 시나리오를 load test에 포함한다.

---

## CHAPTER 07 · protocol error는 malformed frame과 transport failure를 구분한다

### 시작 전 용어집

#### 1. protocol

- **뜻:** Connection reset, timeout, EOF는 transport failure이고 invalid length, unknown message type, bad checksum은 application protocol failure다.
- **왜 중요한가:** 두 종류를 하나의 `ConnectionError`로 뭉개면 retry와 alert 정책이 불명확해진다.
- **예시:** Connection reset, timeout, EOF는 transport failure이고 invalid length, …

#### 2. malformed frame

- **뜻:** Malformed frame을 받았을 때 parser state를 복구할 수 있는지, connection 전체를 닫아야 하는지 protocol이 결정해야 한다.
- **왜 중요한가:** Binary length가 손상되면 다음 frame boundary도 잃기 쉬워 connection 종료가 더 안전할 수 있다.
- **예시:** Malformed frame을 받았을 때 parser state를 복구할 수 …

#### 3. transport failure

- **뜻:** Error log에는 remote endpoint와 frame metadata를 남기되 payload secret을 그대로 기록하지 않는다.
- **예시:** Error log에는 remote endpoint와 frame metadata를 남기되 payload …

---

## CHAPTER 08 · socket contract는 byte stream 위에 application message와 resource limit을 정의한다

### 시작 전 용어집

#### 1. socket

- **뜻:** 안정적인 socket protocol은 frame format, max frame size, timeout, retry, EOF, half-close, backpressure, malformed input 처리까지 정의한다.
- **왜 중요한가:** `send`와 `recv` 호출 자체는 이 계약의 일부일 뿐이다.
- **예시:** 안정적인 socket protocol은 frame format, max frame size, …

#### 2. byte stream

- **뜻:** 이 PART의 핵심은 **socket을 메시지 API로 오해하지 않고, 순서 있는 byte stream 위에 framing과 flow control을 application이 직접 구성해야 한다는 사실을 실행 경로로 이해하는 것**이다.
- **왜 중요한가:** 테스트에서는 한 frame을 1-byte 단위로 쪼개기, 여러 frame을 한 번에 합치기, partial send, oversized length, mid-frame EOF, slow receiver를 재현한다.
- **예시:** 이 PART의 핵심은 **socket을 메시지 API로 오해하지 않고, …

---

## 실전 학습 루프 · socket stream framing

### 1. 쉬운 예

TCP는 message 경계를 보존하지 않는 byte stream이다. 한 번 `send()`한 데이터가 한 번 `recv()`에 그대로 대응한다고 가정하면 packet 분할·병합에서 parser가 깨진다.

### 2. 한 줄 해석

stream protocol에는 길이 prefix, delimiter, fixed header 같은 명시적 framing 규칙이 필요하다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상하고, 실행 후에는 **어느 경계에서 상태나 의미가 바뀌었는지** 표시한다.

```python
import struct

payload = b'hello'
frame = struct.pack('!I', len(payload)) + payload
size = struct.unpack('!I', frame[:4])[0]
print(frame[4:4+size])
```

### 4. 수정 실습

1. header가 2byte만 도착한 상황을 상태 머신으로 처리한다.
2. declared length가 최대 허용치를 넘으면 payload allocation 전에 거부한다.

수정 전후를 비교할 때는 정상 경로만 보지 않고 실패 입력과 자원 한도도 함께 확인한다.

### 5. 확인 문제

`sendall(b'abc')`를 호출했으면 peer의 첫 `recv()`가 반드시 `b'abc'`일까?

### 6. 정답과 오답 설명

**정답:** 아니다. TCP는 byte 순서를 보장하지만 application message 경계는 보장하지 않는다.

**자주 나오는 오답:** socket 호출 횟수를 message 횟수와 동일시하면 framing 버그가 생긴다.

마지막에는 이 주제를 **입력/신뢰 수준 → 변환 또는 대기 → 검증 → 결과/실패** 순서로 다시 설명한다. 이 순서가 보이면 실제 장애에서도 원인 경계를 빠르게 좁힐 수 있다.

## 현장 디버깅 체크 · socket stream framing

### 증상에서 시작한다

트래픽이 적을 때는 되지만 실제 네트워크에서는 두 message가 붙거나 하나가 잘려 parser가 깨진다. 재현 시점의 입력과 작업 식별자를 먼저 고정하고, 결과를 보고 추측하기보다 상태 전이를 시간순으로 적는다.

### 먼저 볼 증거

recv byte 수, buffer 누적 길이, 현재 parser state, declared frame length, 최대 frame budget을 기록한다. 한 숫자만 보지 말고 **대기/실행/완료/실패**를 분리하면 병목과 논리 오류를 구분하기 쉽다.

### 일부러 실패시켜 보기

header와 payload를 1byte 단위로 쪼개 전달하고 여러 frame을 한 번에 붙여도 동일한 message sequence가 복원되는지 본다. 정상 경로는 원래 잘 되는 경우가 많다. 강제 실패에서 cleanup·retry·재시작 의미가 유지되는지가 운영 품질을 결정한다.

### 통과 기준

임의 chunking에서도 frame 경계가 정확히 복원되고 oversized/truncated frame은 allocation 전에 거부돼야 한다. 이 기준을 regression test와 운영 metric 두 곳에 동시에 연결하면 배포 뒤 같은 문제가 돌아왔을 때 빠르게 탐지할 수 있다.
## 판단 규칙 · stream에서 message를 복원할 때

socket 코드를 읽을 때는 recv 호출 횟수가 아니라 application buffer의 현재 길이와 parser state를 기준으로 판단한다. header가 4byte인데 지금 2byte만 들어왔다면 오류가 아니라 더 필요함 상태다. 반대로 header가 선언한 payload 길이가 protocol 최대치를 넘으면 더 기다리지 말고 즉시 invalid로 종료한다.

실전에서는 NEED_HEADER, NEED_PAYLOAD(n), COMPLETE 같은 상태를 명시적으로 기록한다. connection 종료가 끼었을 때 NEED_PAYLOAD(20) 상태에서 EOF가 오면 정상 완료가 아니라 truncated frame이다. 즉 부분 입력과 잘못된 입력을 구분하는 것이 framing의 핵심이다.

## 개념 연결 · framing에서 parser 상태 머신으로

framing 문제를 제대로 이해하려면 “socket에서 몇 번 읽었는가” 대신 **현재까지 몇 byte를 확보했고 다음 상태로 가려면 몇 byte가 더 필요한가**를 기록해야 한다. 예를 들어 NEED_HEADER(4) → NEED_PAYLOAD(n) → COMPLETE처럼 상태를 나누면 1byte씩 도착하든 여러 frame이 한 번에 도착하든 같은 parser가 동작한다.

이 구조는 뒤의 readiness loop와 직접 연결된다. readiness는 “읽을 수 있음”만 알려 주고, message가 완성됐는지는 framing state가 판단한다.
