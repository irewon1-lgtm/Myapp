# PART 04 · 프로세스, 스레드, 동시성 — 한 CPU에서 여러 일이 동시에 보이는 이유

프로그램이 두 개 떠 있고 다운로드와 화면 그리기가 동시에 진행되는 것처럼 보여도, `동시에`라는 말 안에는 여러 경우가 섞여 있다. 여러 CPU core가 실제로 병렬 실행할 수도 있고, 하나의 core가 여러 runnable task를 매우 빠르게 번갈아 실행할 수도 있으며, 어떤 task는 CPU를 쓰지 않고 I/O가 끝나기를 기다릴 수도 있다.

이 PART의 목표는 API 이름을 외우는 것이 아니라 **실행 주체, 공유 상태, 대기 이유, 깨울 조건, 순서 보장**을 분리해 사고하는 것이다.

---

## CHAPTER 01 · process는 실행 중인 프로그램의 격리 단위다

### 같은 실행 파일도 여러 process가 될 수 있다

디스크의 executable file 하나를 두 번 실행하면 보통 서로 다른 process가 만들어질 수 있다.

```text
program image on storage
       ↓ start
process A
       ↓ another start
process B
```

A와 B는 같은 code를 기반으로 해도 서로 다른 process identity와 address space를 가진다. 따라서 `프로그램 파일`과 `현재 실행 중인 instance`를 구분해야 한다.

### process가 가진 것은 code만이 아니다

process를 이해할 때 다음 상태를 함께 본다.

```text
virtual address space
open file descriptors / handles
threads
credentials / security context
signal or exception state
current working directory 같은 process context
resource accounting
```

운영체제마다 세부 구조는 다르지만 핵심은 process가 **실행에 필요한 여러 자원을 묶는 context**라는 점이다.

### PID는 identity의 한 표현이지 영원한 이름이 아니다

운영체제가 process에 ID를 부여하지만 process 종료 후 ID 공간이 재사용될 수 있다. 로그에서 PID만 보고 시간 구간을 무시하면 서로 다른 process instance를 같은 것으로 착각할 수 있다.

운영 로그에서는 보통 다음을 함께 남기는 편이 안전하다.

```text
timestamp
process id
thread id 또는 execution context
app version/build
request/job correlation id
```

---

## CHAPTER 02 · 새 process를 만드는 것과 새 program을 실행하는 것은 다른 단계다

### Unix 계열의 fork/exec 모델을 개념으로 이해한다

Unix 전통에서 process 생성과 새 program image 실행은 분리된 연산으로 설명되는 경우가 많다.

단순화하면:

```text
parent process
↓ fork-like creation
child process
↓ exec-like replacement
child가 새로운 program image 실행
```

중요한 점은 `fork가 새 프로그램을 실행한다`로 뭉개지 않는 것이다.

process context를 복제/공유하는 단계와 현재 process image를 다른 executable로 대체하는 단계는 다른 문제다.

### copy-on-write가 process 생성 비용을 줄일 수 있다

parent의 모든 memory page를 fork 순간 실제로 전부 복사하면 비싸다. 그래서 copy-on-write를 이용해 처음에는 page를 공유하고 수정되는 page만 나중에 복사하는 전략이 사용될 수 있다.

PART 03에서 배운 COW가 process 생성과 연결된다.

```text
parent pages
    ↑
child initially shares

child writes page X
↓
page X private copy
```

### multithread process에서 fork가 까다로운 이유

여러 thread가 있는 process에서 한 thread가 process creation을 수행하면 `다른 thread가 잡고 있던 lock 상태`, `library internal state`, `buffer` 같은 문제가 생길 수 있다.

예를 들어 다른 thread가 mutex를 잡은 순간 child에 현재 memory state가 보이지만 그 lock을 잡았던 thread 자체는 child에 존재하지 않는 구조라면 이후 코드가 deadlock에 빠질 수 있다.

그래서 low-level process creation API를 사용할 때는 platform의 multithread 규칙을 정확히 확인해야 한다.

---

## CHAPTER 03 · thread는 같은 process 자원을 공유하면서 독립 실행 흐름을 가진다

### thread마다 따로 필요한 상태와 공유하는 상태를 구분한다

같은 process의 thread는 일반적으로 같은 virtual address space와 많은 process resource를 공유한다.

하지만 각 thread에는 독립적인 실행 상태가 필요하다.

```text
shared in process
- heap
- global data
- code mapping
- many open resources

per thread
- instruction position / registers
- stack
- scheduling state
- thread-local state
```

이 구조 때문에 thread 사이 통신은 빠를 수 있지만 동시에 위험하다. 같은 memory를 직접 볼 수 있기 때문이다.

### `변수 하나 공유`가 동시성 문제의 시작이다

다음 값을 두 thread가 동시에 증가시킨다고 하자.

```text
counter = 0

Thread A: counter += 1
Thread B: counter += 1
```

초보자는 결과가 당연히 2라고 생각한다. 하지만 `counter += 1`이 반드시 하나의 indivisible machine operation인 것은 아니다.

개념적으로는:

```text
load counter
add 1
store counter
```

일 수 있다.

가능한 interleaving:

```text
A load 0
B load 0
A store 1
B store 1
```

최종 결과는 1이다.

이것이 race condition을 이해하는 첫 모델이다.

---

## CHAPTER 04 · scheduler는 runnable task에게 CPU 시간을 배분한다

### running과 runnable과 waiting을 구분한다

모든 thread가 항상 CPU 위에서 실행되는 것은 아니다.

상태를 단순화하면:

```text
running
= 지금 CPU에서 instruction을 실행 중

runnable
= 실행할 수 있지만 CPU 차례를 기다림

blocked/waiting
= 어떤 사건이 일어나기 전까지 실행할 이유가 없음
```

예를 들어 network read 결과를 기다리는 thread는 CPU를 계속 태우며 기다릴 필요가 없다. kernel에 대기 상태로 들어갔다가 data가 준비되면 runnable로 돌아갈 수 있다.

### time slice라는 비유만으로 scheduler를 고정하지 않는다

운영체제 scheduler 구현은 시대와 kernel 버전에 따라 달라질 수 있다. 모든 scheduler를 `각 process에 10ms씩 정확히 나눠 준다`로 외우면 틀린다.

안전한 모델은 다음이다.

> scheduler는 runnable task, priority/weight, latency 목표, CPU topology와 정책을 고려해 다음 실행 대상을 결정한다.

실제 Linux scheduler의 내부 algorithm은 버전에 따라 바뀔 수 있으므로 성능 문제에서 오래된 블로그의 특정 공식 하나를 진리처럼 쓰지 않는다.

### context switch에는 공짜가 아닌 비용이 있다

CPU가 A thread에서 B thread로 바뀌면 실행 state를 보존하고 복원해야 하며 cache/TLB locality도 영향을 받을 수 있다.

그래서 `thread를 더 만들면 항상 더 빠르다`는 규칙은 틀리다.

너무 많은 runnable thread가 있으면:

```text
scheduler competition 증가
context switch 증가
cache working set 충돌
memory stack 비용 증가
lock contention 증가
```

가 생길 수 있다.

### CPU-bound와 I/O-bound를 구분한다

CPU-bound workload는 실제 계산이 CPU 시간을 많이 사용한다.

I/O-bound workload는 storage/network/다른 service 응답을 기다리는 시간이 크다.

thread/concurrency 수를 정할 때 이 차이가 중요하다.

CPU-bound task를 core 수보다 훨씬 많이 동시에 runnable로 만들면 계산 자체가 빨라지지 않고 scheduling 비용만 늘 수 있다.

반면 I/O-bound task는 일부가 기다리는 동안 다른 task가 진행해 throughput을 높일 여지가 있다.

---

## CHAPTER 05 · atomicity는 `한 줄`이 아니라 관찰 가능한 중간 상태의 문제다

### source 한 줄과 machine atomic operation은 다르다

```kotlin
balance -= amount
```

이 한 줄이 다른 thread 관점에서 절대 쪼개지지 않는다고 가정하면 안 된다.

고수준 표현은 여러 load/compare/store 동작과 runtime check로 내려갈 수 있다.

동시성에서는 다음을 묻는다.

```text
어떤 상태 변화가 indivisible해야 하는가?
다른 thread가 중간 상태를 볼 수 있는가?
operation 순서가 어떤 memory ordering을 요구하는가?
실패 시 retry해도 안전한가?
```

### atomic type이 모든 복합 invariant를 자동 보호하지 않는다

두 개의 atomic variable이 있다고 하자.

```text
availableSeats = 1
paidOrders = 0
```

각 variable의 단일 read/write가 atomic해도 `좌석 감소와 결제 주문 증가가 함께 맞아야 한다`는 복합 invariant를 자동 보장하지 않는다.

여러 상태의 관계가 하나의 transaction/critical section으로 묶여야 할 수 있다.

---

## CHAPTER 06 · mutex는 공유 상태에 들어가는 규칙을 만든다

### lock의 목적은 느리게 만드는 것이 아니라 invariant를 보호하는 것이다

critical section을 다음처럼 정의하자.

```text
lock acquire
↓
shared state read/modify/write
↓
lock release
```

같은 lock 규칙을 지키는 thread는 동시에 critical section을 실행하지 않도록 조정할 수 있다.

핵심은 `mutex를 썼다`가 아니다.

**어떤 state를 어떤 lock이 보호하는지**가 명확해야 한다.

### lock ownership이 문서화되지 않으면 유지보수가 무너진다

예를 들어:

```text
userMap  -> userLock
cache    -> cacheLock
metrics  -> 별도 atomic counter
```

처럼 ownership을 정해야 한다.

함수 안에서 임의로 여러 lock을 잡기 시작하면 deadlock과 hidden dependency가 커진다.

### uncontended fast path와 contended slow path

현대 synchronization primitive는 lock이 비어 있을 때 매번 kernel로 들어가지 않도록 user-space atomic operation으로 빠르게 획득하고, 실제 경쟁이 생겨 기다려야 할 때 kernel blocking primitive를 사용하는 식으로 구현될 수 있다.

Linux의 futex 계열은 이런 고수준 mutex/condition primitive를 만들기 위한 저수준 building block의 대표적인 예다.

중요한 사고는 다음이다.

```text
경쟁 없음
→ user-space에서 빠르게 완료 가능

경쟁 있음
→ 기다리는 thread를 CPU에서 내려 blocking
→ unlock 시 필요한 waiter를 깨움
```

spin으로 계속 확인하는 것과 sleep/blocking하는 것은 CPU 사용 특성이 다르다.

---

## CHAPTER 07 · condition은 `상태가 바뀌었을 가능성`을 알려 준다

### signal을 받았다고 조건이 반드시 참이라는 보장을 만들지 않는다

producer-consumer queue를 생각하자.

consumer는 queue가 비었으면 기다려야 한다.

```text
lock
while queue is empty:
    wait(condition)
item = queue.remove()
unlock
```

여기서 `if`보다 `while` 패턴이 중요한 이유는 wake 뒤에 조건을 다시 검사해야 하기 때문이다.

다른 consumer가 먼저 item을 가져갈 수 있고, spurious wakeup을 허용하는 API도 있을 수 있다.

condition notification은 보통 `이제 조건을 다시 검사할 이유가 생겼다`는 신호로 이해하는 편이 안전하다.

### lost wakeup을 막으려면 상태와 wait 등록의 순서를 설계해야 한다

잘못된 구조:

```text
consumer: queue empty 확인
--- 사이에 producer가 item 추가 + signal ---
consumer: 이제 wait 진입
```

consumer가 signal을 놓치고 계속 잘 수 있다.

mutex와 condition variable API는 상태 검사와 wait transition을 올바르게 묶기 위한 계약을 제공한다.

---

## CHAPTER 08 · race condition은 테스트에서 잘 안 재현될수록 더 위험할 수 있다

### timing이 바뀌면 사라지는 bug

로그를 추가했더니 race가 사라지는 현상이 있다.

왜냐하면 로그 I/O와 synchronization이 thread timing을 바꾸기 때문이다.

이런 bug를 흔히 timing-sensitive 또는 Heisenbug라고 표현하기도 한다.

따라서 `로그 넣으니 안 생기네 → 해결`이라고 하면 안 된다.

### race를 재현할 때 scheduling 우연에만 기대지 않는다

가능하면 barrier/latch를 사용해 두 thread를 특정 지점까지 정확히 이동시킨 뒤 동시에 release한다.

```text
Thread A ── reach barrier ┐
                          ├─ release together
Thread B ── reach barrier ┘
```

이렇게 하면 production의 희귀 timing을 deterministic test에 가깝게 만들 수 있다.

### race 탐지기는 source review와 다른 증거를 준다

지원되는 언어/runtime에서는 ThreadSanitizer 같은 dynamic race detector가 memory access와 synchronization 관계를 추적해 data race를 찾을 수 있다.

하지만 도구가 모든 logical race를 찾아 주는 것은 아니다.

예를 들어 두 요청이 각각 thread-safe API를 사용해도 `마지막 요청만 화면에 보여야 한다`는 product-level invariant가 깨질 수 있다.

```text
search("A") starts
search("AB") starts
AB result returns
A result returns late
UI overwritten with stale A result
```

이것은 단순 memory race가 아니라 **logical race**다.

---

## CHAPTER 09 · deadlock은 서로가 가진 것을 기다리는 cycle이다

### 두 lock으로 가장 작은 deadlock을 만든다

```text
Thread A:
lock(L1)
lock(L2)

Thread B:
lock(L2)
lock(L1)
```

가능한 실행:

```text
A owns L1
B owns L2
A waits L2
B waits L1
```

둘 다 영원히 진행하지 못한다.

### lock order를 전체 시스템 규칙으로 만든다

예:

```text
L1 before L2 before L3
```

모든 코드가 같은 순서를 지키면 circular wait 가능성을 줄일 수 있다.

문제는 함수가 내부에서 어떤 lock을 잡는지 숨겨져 있을 때다.

```text
caller owns L2
↓
helper() internally acquires L1
```

겉에서는 순서를 지킨 것처럼 보여도 실제 call graph에서는 역순이 된다.

그래서 synchronization은 local code 문제가 아니라 API contract다.

### deadlock과 starvation과 livelock을 구분한다

```text
deadlock
→ 서로 기다려 아무도 진행 못함

starvation
→ 시스템 전체는 진행하지만 특정 task가 계속 기회를 못 얻음

livelock
→ 서로 반응하며 상태를 계속 바꾸지만 유용한 진행이 없음
```

`멈췄다`는 증상만으로 셋을 구분할 수 없다.

---

## CHAPTER 10 · thread-safe와 reentrant와 immutable은 다른 속성이다

### thread-safe는 동시 호출에서 contract를 지키는가의 문제다

함수가 thread-safe하다고 해서 항상 lock을 사용한다는 뜻은 아니다.

immutable state만 읽거나, thread-local data만 사용하거나, lock-free algorithm을 사용해도 thread-safe할 수 있다.

### immutable data는 공유를 단순하게 만든다

한 번 만들어진 뒤 바뀌지 않는 object는 여러 thread가 같은 값을 읽을 때 synchronization 요구를 크게 줄일 수 있다.

하지만 reference 자체를 새 object로 교체하는 publish 과정에는 visibility 규칙이 필요할 수 있다.

즉 `immutable이니 memory model을 전혀 몰라도 된다`는 뜻은 아니다.

### reentrant는 같은 execution이 다시 들어올 수 있는가의 문제다

signal handler, callback, recursive path 등에서 같은 함수가 중첩 호출될 수 있다.

함수가 global mutable buffer를 임시 작업공간으로 사용하면 reentrant하지 않을 수 있다.

thread-safe와 reentrant는 겹치는 부분이 있지만 같은 정의가 아니다.

---

## CHAPTER 11 · memory visibility는 `내 thread에서 썼다`와 `남도 본다` 사이의 규칙이다

### compiler와 CPU는 무조건 source 순서 그대로 memory operation을 노출하지 않는다

성능을 위해 compiler와 processor가 instruction을 재배치하거나 cache/coherence mechanism을 사용할 수 있다.

그래서 두 thread가 synchronization 없이 shared flag만 사용하면 source code에서 보이는 순서와 다른 관찰이 가능할 수 있다.

고수준 언어는 memory model을 정의해 `어떤 synchronization operation 사이에서 어떤 write가 visible해야 하는가`를 규정한다.

### happens-before를 사고 도구로 사용한다

정확한 용어 정의는 언어 memory model마다 봐야 하지만, 실무에서는 `A의 write가 B의 read에 보인다는 근거가 무엇인가`를 묻는 습관이 중요하다.

근거가 될 수 있는 것은 예를 들어:

```text
mutex unlock -> later lock
thread start/join 계약
channel/queue send-receive 계약
atomic variable의 정해진 ordering
runtime-provided synchronization
```

등이다.

`대충 먼저 실행했으니까 보이겠지`는 근거가 아니다.

---

## CHAPTER 12 · IPC는 process 격리를 넘어서 데이터를 전달하는 방법이다

### process가 분리돼 있으면 pointer를 그대로 넘길 수 없다

process A의 virtual address `0x1234`는 process B에서 같은 object를 뜻하지 않는다.

따라서 process 사이 통신에는 별도의 IPC mechanism이 필요하다.

### pipe는 byte stream을 연결한다

pipe는 한쪽이 쓰고 다른 쪽이 읽는 byte stream channel로 사용할 수 있다.

```text
producer process
   ↓ write
 [ pipe buffer ]
   ↓ read
consumer process
```

여기서도 message boundary가 자동으로 보존되는지, buffer가 가득 차면 write가 어떻게 되는지, reader가 닫히면 writer가 어떤 오류를 받는지 API 계약을 봐야 한다.

### socket은 local/remote communication을 비슷한 I/O 모델로 다룰 수 있다

socket은 network뿐 아니라 local IPC에도 사용될 수 있다.

stream socket에서는 application-level message framing을 직접 정의해야 할 수 있다.

예를 들어 JSON 두 개를 그냥 붙이면 경계가 모호하다.

```text
{"a":1}{"b":2}
```

그래서 length prefix, delimiter, fixed header 같은 framing protocol이 필요하다.

### shared memory는 빠르지만 synchronization을 별도로 요구한다

두 process가 같은 physical memory를 mapping하면 큰 data를 복사하지 않고 공유할 수 있다.

하지만 누가 언제 읽고 쓰는지 조정하는 문제는 남는다.

```text
shared memory
+ mutex/futex/semaphore 같은 coordination
+ ownership protocol
```

빠른 data path와 올바른 synchronization protocol을 함께 설계해야 한다.

---

## CHAPTER 13 · blocking I/O와 non-blocking I/O를 실행 상태로 이해한다

### blocking call은 thread 진행을 멈출 수 있다

socket read에서 아직 data가 없다면 blocking mode에서는 thread가 wait state로 들어갈 수 있다.

이때 CPU를 계속 소비하는 busy loop와 다르다.

```text
read request
↓ data not ready
thread blocks
↓
network event arrives
↓
thread becomes runnable
```

### non-blocking은 `기다림이 없다`가 아니라 `지금 가능한 만큼만 처리한다`에 가깝다

non-blocking descriptor에서 data가 준비되지 않았으면 API가 즉시 `지금은 처리할 수 없음`을 나타내는 결과를 줄 수 있다.

그렇다고 application이 무한 loop로 계속 read를 재시도하면 CPU를 낭비한다.

그래서 readiness notification mechanism과 함께 사용한다.

### event multiplexing은 많은 descriptor를 소수 thread로 감시할 수 있게 한다

Linux의 epoll 같은 mechanism은 여러 file descriptor 중 어떤 것이 I/O 가능한 상태인지 효율적으로 기다리기 위해 사용된다.

개념적으로:

```text
register interests
fd A: read
fd B: read/write
fd C: read

wait for readiness
↓
ready set returned
↓
process only ready descriptors
```

이 구조는 `connection 하나당 thread 하나`만이 유일한 서버 모델이 아님을 보여 준다.

---

## CHAPTER 14 · event loop는 callback 목록이 아니라 실행 정책이다

### 하나의 loop가 event를 순서대로 처리할 수 있다

UI framework나 network runtime에서 event loop는 queue에서 event/task를 꺼내 실행한다.

```text
event arrives
↓
queue
↓
loop picks task
↓
handler runs
↓
next task
```

한 loop thread에서 handler가 오래 걸리면 뒤 task가 모두 기다린다.

그래서 event-driven architecture에서도 CPU-heavy/blocking work를 어디서 실행할지 분리해야 한다.

### async가 CPU를 자동 병렬화하는 것은 아니다

`async/await`는 asynchronous operation의 제어 흐름을 표현하는 도구다.

I/O를 기다리는 동안 thread를 효율적으로 다른 일에 쓸 수 있지만, CPU-bound loop가 같은 execution thread에서 오래 돌면 여전히 다른 task를 막을 수 있다.

```text
async network wait
→ 다른 task에게 실행 기회 제공 가능

CPU infinite loop inside async function
→ event loop를 계속 점유할 수 있음
```

---

## CHAPTER 15 · cancellation은 bool 하나가 아니라 협력 protocol이다

### task를 중간에 멈추면 resource와 invariant가 남는다

파일 변환 작업을 취소한다고 하자.

```text
read source
→ create temp file
→ write half
→ cancel
```

그냥 thread를 강제로 없애면 temp file, lock, transaction, progress state가 남을 수 있다.

안전한 cancellation은 cleanup point와 ownership을 설계해야 한다.

### cooperative cancellation

많은 고수준 concurrency framework는 task가 cancellation state를 확인하거나 suspension point에서 cancel을 전달받도록 한다.

이 접근에서는 code가 cleanup을 실행할 기회를 가진다.

```text
try:
    work
finally:
    close resource
    rollback partial state
```

### cancellation과 timeout을 구분한다

```text
timeout
→ 정해진 시간 안에 완료되지 않았다는 정책 판단

cancellation
→ 더 이상 결과가 필요 없으므로 작업 중단 요청
```

상위 request가 취소됐는데 하위 DB/network 작업이 계속 돌면 system load와 stale update가 남을 수 있다.

그래서 cancellation propagation이 중요하다.

---

## CHAPTER 16 · Android main thread를 막으면 화면 전체가 멈춘다

### Android app process의 기본 main thread

Android application component가 기본 process에서 실행될 때 framework callback과 UI 작업의 많은 부분은 main thread를 통해 처리된다.

사용자 touch 처리, view update, lifecycle callback 등이 같은 main execution context에 몰릴 수 있다.

따라서 main thread에서 오래 걸리는 disk/network/CPU 작업을 하면 단순히 그 함수만 늦어지는 것이 아니다.

```text
long task occupies main thread
↓
input dispatch delayed
↓
drawing delayed
↓
lifecycle/callback processing delayed
↓
user sees frozen app
```

### ANR은 `앱이 crash했다`와 다른 실패다

Application Not Responding은 process가 반드시 exception으로 죽었다는 뜻이 아니다. 중요한 event를 정해진 시간 안에 처리하지 못해 시스템이 responsiveness failure로 판단하는 경우다.

따라서 crash log만 뒤져서는 원인을 못 찾을 수 있다.

thread dump, trace, blocked main thread의 stack, lock owner, I/O wait 같은 증거를 봐야 한다.

### background로 보냈다고 자동 안전하지 않다

worker thread가 UI object를 직접 수정하면 thread-safety 규칙을 어길 수 있다.

올바른 구조는 대개:

```text
main thread
→ launch/request background work

worker/background
→ blocking/CPU work
→ result produced

main thread
→ apply result to UI state
```

처럼 ownership 경계를 분명히 한다.

---

## CHAPTER 17 · thread pool은 task와 worker 수를 분리한다

### task마다 새 thread를 만들지 않는다

짧은 task가 매우 많이 들어오는 서버에서 매 request마다 thread를 만들고 버리면 creation/destruction과 memory stack 비용이 커질 수 있다.

thread pool은 일정 수의 worker가 queue의 task를 반복 처리하게 한다.

```text
requests/jobs
      ↓
    queue
      ↓
worker1 worker2 worker3 ...
```

### queue가 무한이면 overload를 숨긴다

worker보다 task arrival이 계속 빠르면 queue가 자란다.

처음에는 request를 모두 받아 주므로 정상처럼 보이지만 latency와 memory가 계속 증가할 수 있다.

```text
arrival rate > service rate
for long enough
→ queue growth
→ latency growth
→ memory pressure
→ timeout/retry
→ 더 큰 load
```

이것이 overload feedback loop로 번질 수 있다.

### bounded queue와 backpressure

queue에 상한을 두고 넘으면 caller를 block하거나 reject하거나 load shedding할 수 있다.

중요한 것은 `무조건 다 받기`가 안전한 정책이 아니라는 점이다.

system capacity보다 많은 일을 받으면 결국 더 나쁜 방식으로 실패한다.

---

## CHAPTER 18 · lock contention은 correctness 문제와 performance 문제를 함께 만든다

### lock이 맞아도 너무 넓게 잡으면 throughput이 무너진다

하나의 global lock 아래 모든 작업을 직렬화하면 race는 막을 수 있어도 parallelism이 사라진다.

```text
global lock
  ├─ read user
  ├─ slow network call
  ├─ update cache
  └─ write DB
```

특히 lock을 잡은 채 network I/O를 기다리면 다른 thread가 긴 시간 critical section에 진입하지 못한다.

### critical section을 줄일 때 invariant를 깨면 안 된다

성능 때문에 lock 범위를 줄이는 수정은 race를 만들기 쉽다.

안전한 접근:

```text
1. lock이 보호하는 invariant를 문장으로 쓴다.
2. lock 안에서 반드시 함께 봐야 하는 state를 찾는다.
3. 오래 걸리는 외부 I/O를 lock 밖으로 옮길 수 있는 protocol을 설계한다.
4. 중간에 state가 바뀌었는지 version/token으로 재검증한다.
```

단순히 중괄호를 줄이는 것이 아니다.

---

## CHAPTER 19 · concurrency bug를 조사하는 증거 세트

`가끔 멈춤`을 받았을 때 다음 순서로 좁힌다.

### 1. 모든 thread의 stack을 같은 시점에 본다

한 thread만 보면 `lock을 기다린다`는 사실은 알 수 있어도 lock owner가 왜 안 풀어 주는지 모른다.

thread dump 전체에서:

```text
Thread A waiting lock X
Thread B owns X, waiting lock Y
Thread C owns Y, waiting network
```

처럼 wait graph를 만든다.

### 2. CPU 사용률을 본다

```text
0%에 가까움 + stuck
→ blocking/deadlock/wait 가능성

100% core 사용 + stuck
→ busy loop/livelock/CPU-bound 가능성
```

물론 절대 규칙은 아니지만 조사 방향을 나눈다.

### 3. queue depth와 latency를 본다

worker pool 문제라면 worker CPU뿐 아니라 pending queue와 처리시간 분포를 함께 본다.

### 4. lock hold time을 본다

contention이 심하면 누가 lock을 얼마나 오래 잡는지 측정한다.

### 5. cancellation과 timeout propagation을 확인한다

상위 request가 끝났는데 child work가 계속 남는지 본다.

---

## CHAPTER 20 · 이 PART의 통합 불변조건

동시성 설계에서 API 이름보다 먼저 다음 다섯 문장을 완성한다.

```text
1. 이 mutable state의 owner는 ______ 이다.
2. 이 state를 동시에 접근할 수 있는 execution context는 ______ 이다.
3. 동시에 접근하면 ______ synchronization rule을 따른다.
4. 기다리는 task는 ______ 조건이 참이 될 때 다시 실행된다.
5. cancellation/failure 시 ______ cleanup으로 invariant를 복구한다.
```

### 예제: 사진 업로드

잘못된 설계:

```text
button click마다 새 thread
shared mutable progressMap 직접 수정
network timeout 없음
화면 닫혀도 업로드 계속
UI를 worker thread에서 갱신
```

더 구조적인 설계:

```text
UI/main
→ upload job 생성
→ job id와 immutable request를 queue에 전달

worker
→ bounded concurrency
→ network timeout
→ progress event 발행
→ cancellation 확인

state owner
→ job id 기준 최신 progress 반영

UI/main
→ state observe 후 화면 갱신
```

여기서 사용한 핵심은 특정 library가 아니다.

```text
ownership
bounded concurrency
message passing
cancellation
main-thread confinement
```

이다.

---

## PART 04 종료 점검

다음 질문에 설명할 수 있어야 한다.

1. program file과 process는 무엇이 다른가?
2. process와 thread가 공유하는 state와 공유하지 않는 state는 무엇인가?
3. runnable과 blocked는 무엇이 다른가?
4. thread를 늘리면 항상 빨라지지 않는 이유는 무엇인가?
5. `counter += 1`이 race가 될 수 있는 이유는 무엇인가?
6. mutex가 어떤 invariant를 보호하는지 문서화해야 하는 이유는 무엇인가?
7. condition wait 뒤 조건을 다시 검사해야 하는 이유는 무엇인가?
8. deadlock, starvation, livelock을 어떻게 구분하는가?
9. non-blocking I/O가 busy loop를 정당화하지 않는 이유는 무엇인가?
10. event loop에서 CPU-heavy 작업이 왜 전체 responsiveness를 망칠 수 있는가?
11. Android main thread를 막았을 때 ANR로 이어질 수 있는 경로는 무엇인가?
12. bounded queue와 backpressure가 overload를 왜 더 안전하게 다루는가?
13. cancellation이 cleanup protocol이어야 하는 이유는 무엇인가?
14. concurrency bug를 재현할 때 timing 우연 대신 어떤 증거를 만들 수 있는가?

이 질문의 답이 `멀티스레드는 동시에 돌아간다` 수준이면 아직 부족하다. **실행 상태, scheduler, shared state, synchronization, visibility, wait graph, cancellation**까지 연결해야 한다.