# PART 25 · Async I/O Engines — readiness, completion, epoll, io_uring, zero-copy

비동기 I/O는 `thread를 많이 만들지 않고 동시에 처리한다`는 설명으로 끝나지 않는다. Kernel interface가 readiness를 알려주는지 completion을 알려주는지, user/kernel 사이 request metadata가 어떻게 전달되는지, buffer/file lifetime을 누가 소유하는지, cancellation과 timeout이 어느 state transition에 적용되는지가 correctness와 성능을 결정한다. 같은 network server라도 epoll 기반 state machine과 io_uring completion pipeline은 backpressure·memory ownership·error handling 구조가 다르다.

---

## CHAPTER 01 · Blocking은 thread state이고 asynchronous는 operation lifecycle이다

Blocking system call은 호출 thread가 operation 완료 조건을 기다리는 동안 sleep할 수 있다. Nonblocking file descriptor는 operation이 즉시 진행할 수 없을 때 caller에게 상태를 반환한다. Asynchronous interface는 operation을 제출한 뒤 completion을 나중에 회수하도록 설계할 수 있다. 이 세 개념은 같은 축이 아니다.

`nonblocking = asynchronous`로 동일시하면 readiness API와 completion API의 책임 차이를 놓친다. Readiness에서는 application이 준비된 descriptor에 실제 read/write를 다시 수행하고, completion API에서는 제출한 operation의 result 자체가 completion event로 돌아올 수 있다.

---

## CHAPTER 02 · Readiness는 operation 결과가 아니라 지금 시도할 조건을 알려준다

poll/epoll 계열은 file descriptor가 read/write 같은 operation을 진행할 수 있는 상태인지를 notification한다. `readable`은 application message 하나가 완성됐다는 뜻이 아니다. Socket receive buffer에 최소 일부 data가 있거나 EOF/error 상태가 발생했다는 뜻일 수 있다.

Protocol parser는 readiness event와 message framing을 분리해야 한다. 한 readiness에서 partial frame만 읽힐 수 있고 여러 frame이 한 번에 들어올 수도 있다. Event count와 request count를 동일하게 보면 state machine이 깨진다.

---

## CHAPTER 03 · epoll interest list와 ready list는 서로 다른 state를 가진다

epoll instance는 관찰 대상 descriptor 집합과 현재 event가 준비된 descriptor 집합을 kernel에서 관리한다. Application은 `epoll_ctl`로 interest를 갱신하고 `epoll_wait`로 ready event를 소비한다. Descriptor 등록 상태와 현재 readiness는 분리된다.

File descriptor reuse, close, duplicate descriptor가 얽히면 application의 logical connection identity와 kernel file object identity를 명확히 관리해야 한다. Event payload에 raw fd만 저장해 stale state를 참조하는 bug를 만들 수 있다.

---

## CHAPTER 04 · Level-triggered와 edge-triggered는 event delivery contract가 다르다

Level-triggered mode는 readiness 조건이 유지되는 동안 다시 notification될 수 있다. Edge-triggered mode는 상태 변화 edge를 중심으로 event를 전달하므로 application이 가능한 data를 충분히 drain하지 않으면 추가 edge가 오지 않아 connection이 멈춘 것처럼 보일 수 있다.

Edge-triggered loop는 nonblocking operation을 반복해 `EAGAIN`에 도달할 때까지 현재 readiness를 소비하는 invariant를 갖는다. 이 invariant가 깨지면 lost work처럼 보이는 application bug가 생긴다.

---

## CHAPTER 05 · One-shot registration은 ownership handoff를 명시하는 도구다

여러 worker가 같은 fd event를 동시에 처리하면 state mutation race가 생길 수 있다. EPOLLONESHOT류 mechanism은 event 전달 후 descriptor를 다시 arm하기 전까지 추가 delivery를 막아 한 worker가 connection state를 독점하게 설계할 수 있다.

Rearm 시점은 correctness boundary다. 처리 중 error path에서 rearm을 빠뜨리면 descriptor가 영원히 조용해지고, 너무 일찍 rearm하면 두 worker가 겹쳐 실행될 수 있다.

---

## CHAPTER 06 · Thundering herd는 readiness 하나에 여러 waiter가 깨는 비용이다

여러 thread/process가 같은 event source를 기다릴 때 하나의 connection event가 불필요하게 많은 waiter를 깨우면 context switch와 cache contention이 증가한다. Kernel과 application은 exclusive wakeup, accept distribution, per-worker queue 같은 방식으로 herd를 줄일 수 있다.

Wakeup count가 request count보다 훨씬 높다면 user code lock보다 event distribution policy를 먼저 조사한다.

---

## CHAPTER 07 · accept loop도 backlog·readiness·connection ownership을 함께 다룬다

Listening socket이 readable하다는 것은 accept 가능한 connection이 있음을 의미한다. Edge-triggered server는 accept를 반복해 queue를 drain하고, accepted fd를 어떤 worker/event loop가 소유할지 결정해야 한다. Backlog가 가득 차면 application worker 이전 단계에서 connection이 지연·거부될 수 있다.

Connection latency는 client network RTT뿐 아니라 listen backlog, accept scheduling, worker handoff를 포함한다.

---

## CHAPTER 08 · Completion model은 operation descriptor와 result correlation이 핵심이다

Asynchronous completion API에서는 operation을 제출할 때 application-defined identity를 함께 저장하고 나중에 completion result를 해당 state object와 연결한다. Submission order와 completion order가 같다고 가정하면 안 된다. Device/network operation은 서로 다른 시간에 완료될 수 있다.

Correlation ID가 재사용되거나 state object가 completion 전에 free되면 use-after-free가 발생한다. Operation lifetime은 request object lifetime보다 짧거나 길 수 있으므로 명시적 ownership이 필요하다.

---

## CHAPTER 09 · io_uring SQ/CQ는 user/kernel 사이 shared ring protocol이다

io_uring은 submission queue와 completion queue를 shared memory로 mapping해 request descriptor와 result를 교환한다. User가 SQE를 준비하고 kernel이 operation을 처리한 뒤 CQE에 result를 기록한다. Ring index와 entry publication에는 producer-consumer ordering이 필요하다.

Shared ring은 syscall 수를 줄일 수 있지만 queue overflow, stale index, CQ draining 지연을 application이 새 failure mode로 관리해야 한다. Queue가 shared됐다고 synchronization requirement가 사라지는 것이 아니다.

---

## CHAPTER 10 · SQE는 system call의 argument bundle을 queue entry로 바꾼다

각 SQE는 opcode, fd, buffer address/length, offset, flags, user data 등을 기술한다. Application은 여러 SQE를 준비한 뒤 batch로 submit할 수 있다. 이 구조는 repeated syscall entry 비용을 줄이고 dependency를 표현할 수 있게 한다.

잘못된 pointer/lifetime을 SQE에 넣으면 submission 시점이 아니라 kernel이 실제 operation을 수행할 때 failure가 드러날 수 있다. Deferred execution 때문에 validation과 resource pinning 경계가 중요하다.

---

## CHAPTER 11 · CQ를 늦게 비우면 completion 자체가 backpressure source가 된다

Operation이 완료돼도 application이 CQE를 소비하지 않으면 completion ring이 차고 새 result publication에 제약이 생긴다. Submission capacity와 completion-drain capacity는 따로 관리해야 한다.

Server loop는 `더 많이 submit`하기 전에 CQ occupancy와 in-flight count를 본다. Queue depth를 무제한 늘리는 것은 throughput 최적화가 아니라 memory와 tail latency를 숨기는 방식이 될 수 있다.

---

## CHAPTER 12 · Registered files는 fd lookup 비용과 lifetime contract를 바꾼다

io_uring은 file descriptor table lookup을 반복하지 않도록 file set을 ring에 register할 수 있다. Fixed-file index를 사용하면 hot path overhead를 줄일 수 있지만 registration/update/unregister lifecycle이 추가된다.

Application의 fd number와 registered slot을 같은 identity로 취급하면 안 된다. Connection close 시 in-flight operation과 fixed-file table update 순서를 관리해야 한다.

---

## CHAPTER 13 · Registered buffers는 page pinning과 memory accounting을 요구한다

Buffer를 미리 register하면 매 operation마다 page mapping/pinning overhead를 줄일 수 있지만, 해당 memory를 ring lifetime 동안 사용할 수 있게 유지해야 한다. Large registered pool은 process memory뿐 아니라 kernel pinning/resource limit에 영향을 준다.

Buffer registration은 allocator optimization이 아니라 I/O ownership contract다. Buffer가 어느 operation에 대여됐고 언제 재사용 가능한지 별도 state machine으로 추적해야 한다.

---

## CHAPTER 14 · Provided buffer ring은 receive buffer 선택을 kernel과 협상한다

Network receive처럼 incoming size/connection이 미리 정해지지 않는 operation에서는 application이 reusable buffer pool을 제공하고 kernel이 적절한 buffer를 선택하게 할 수 있다. Completion은 어느 buffer ID가 사용됐는지 알려줘 application이 ownership을 회수한다.

Buffer pool exhaustion은 packet/request loss나 fallback allocation으로 이어질 수 있다. Pool size는 average보다 burst concurrency에 맞춰 설계한다.

---

## CHAPTER 15 · Multishot operation은 submission 하나가 여러 completion을 만든다

accept/recv 같은 event를 매번 다시 submit하지 않고 하나의 request가 반복 completion을 생성하도록 하는 multishot mode가 있다. 이 경우 `SQE 하나 ↔ CQE 하나` invariant가 깨진다. Completion flags를 확인해 request가 계속 active인지 종료됐는지 판단해야 한다.

Cancellation과 resource lifetime도 마지막 completion까지 유지되어야 한다. Reference count를 completion 수가 아닌 request active-state에 연결하는 편이 안전하다.

---

## CHAPTER 16 · Linked request는 execution dependency를 queue level에서 표현한다

여러 SQE를 link해 앞 operation 성공/실패에 따라 뒤 operation 실행을 제어할 수 있다. 예를 들어 timeout과 I/O를 연결하거나 open→read→close chain을 만들 수 있다. Dependency가 kernel queue에 들어가면 application callback round-trip을 줄일 수 있다.

하지만 partial failure semantics를 명확히 알아야 한다. 앞 operation failure가 뒤 link를 cancel하는지, hardlink가 어떤 behavior를 갖는지 문서 contract를 따라야 한다.

---

## CHAPTER 17 · Timeout은 operation과 별도 request일 수 있다

Asynchronous I/O timeout은 blocking call에 `timeout=5s`를 넣는 것보다 복잡하다. Original operation과 timeout request가 race할 수 있고 한쪽 completion이 다른 쪽 cancellation을 trigger할 수 있다. 이미 완료된 operation을 cancel하려 하면 결과 code가 달라진다.

Exactly-one user response invariant를 지키려면 I/O completion과 timeout completion이 동시에 도착해도 application state transition은 한 번만 성공해야 한다.

---

## CHAPTER 18 · Cancellation은 best-effort state transition이지 시간여행이 아니다

Request가 이미 hardware/device에 전달됐거나 completion 직전이면 cancel request가 늦을 수 있다. API는 cancel 성공, target not found, already completing 같은 상태를 구분한다. Cancellation 반환값을 `원래 operation은 아무 영향도 남기지 않았다`로 확대 해석하면 안 된다.

Write cancellation은 특히 side effect가 일부 발생했을 가능성을 고려해야 한다. Idempotency와 application-level reconciliation이 필요하다.

---

## CHAPTER 19 · SQ polling은 syscall을 줄이는 대신 dedicated CPU를 소비한다

SQPOLL mode는 kernel thread가 submission queue를 polling해 user가 매 batch마다 io_uring_enter를 호출하지 않아도 request를 발견할 수 있게 한다. Low latency/high IOPS에서는 syscall transition을 줄일 수 있지만 polling thread가 CPU와 power를 소비한다.

Idle workload에서 SQPOLL은 energy 비용이 과할 수 있다. CPU affinity와 shared-core interference도 평가해야 한다.

---

## CHAPTER 20 · I/O polling은 completion latency와 CPU budget을 교환한다

Device가 fast하고 predictable할 때 completion interrupt 대신 polling하면 interrupt scheduling latency를 줄일 수 있다. 그러나 request가 오래 걸리면 CPU spin이 낭비된다. Polling은 storage/network backend와 CPU allocation을 함께 설계해야 한다.

Latency percentile 개선과 CPU per request 악화를 동시에 측정한다. P99만 낮아졌다고 total system efficiency가 좋아진 것은 아니다.

---

## CHAPTER 21 · Readiness와 completion을 한 event loop에서 섞으면 state ownership이 복잡해진다

기존 epoll socket과 io_uring file I/O를 한 service에서 사용하면 event source마다 의미가 다르다. Epoll event는 `이제 시도 가능`, CQE는 `operation 종료/result`를 의미한다. 하나의 generic event enum으로 뭉개면 retry/cancellation semantics가 사라진다.

Event source별 transition을 명시하고 upper-layer request state에서 합친다.

---

## CHAPTER 22 · Zero-copy는 copy 하나를 없애는 대신 buffer lifetime을 길게 만든다

Network zero-copy send/receive는 kernel↔userspace copy를 줄일 수 있지만 DMA와 protocol stack이 buffer를 사용할 동안 application이 memory를 재사용하면 안 된다. Completion notification이 실제 buffer release point가 된다.

Copy cost가 bottleneck이 아닌 작은 payload에서는 pinning, notification, bookkeeping이 더 비쌀 수 있다. Zero-copy는 payload size와 CPU/memory bandwidth profile을 근거로 선택한다.

---

## CHAPTER 23 · io_uring zero-copy receive는 NIC queue와 userspace memory ownership까지 연결한다

Zero-copy Rx는 packet payload가 kernel buffer를 한 번 더 거치지 않고 userspace memory로 전달되는 경로를 제공할 수 있다. Hardware header/data split, flow steering, compatible queue configuration 같은 NIC capability가 필요하다. 이는 단순 API flag가 아니라 NIC→kernel TCP stack→userspace buffer의 data path 변경이다.

Deployment 전에 NIC/driver capability와 queue isolation을 확인한다. Fallback path의 semantics와 performance도 같이 테스트한다.

---

## CHAPTER 24 · Direct I/O와 async API는 별개의 선택이다

io_uring을 사용한다고 page cache를 우회하는 것은 아니다. Buffered I/O와 direct I/O 모두 asynchronous interface로 제출할 수 있다. Direct I/O는 alignment와 coherency constraint를 가지며 cache benefit을 잃는 대신 double caching/copy를 줄일 수 있다.

Storage access pattern에 따라 buffered+readahead가 direct보다 빠를 수 있다. API modernity가 storage policy를 자동 결정하지 않는다.

---

## CHAPTER 25 · Short I/O와 partial completion은 비동기에서도 존재한다

Read/write request가 요청한 전체 byte를 처리하지 못하고 short result를 반환할 수 있다. Network socket, signal, filesystem condition에 따라 partial progress가 정상 contract일 수 있다. Completion을 `성공/실패 boolean`으로만 모델링하면 남은 offset/length를 잃는다.

State machine은 bytes completed와 retry condition을 저장한다. 특히 vectored I/O에서는 iovec cursor까지 갱신해야 한다.

---

## CHAPTER 26 · Backpressure는 in-flight count를 제한하는 곳에서 시작한다

Asynchronous API는 caller thread를 block하지 않기 때문에 과부하 때 request가 더 빠르게 누적될 수 있다. Ring capacity가 수천이라고 application request도 수천 개를 무조건 submit하면 memory retention과 tail latency가 증가한다.

Admission limit은 downstream service rate와 deadline에 맞춰 둔다. Queue full을 error가 아니라 overload signal로 취급하고 upstream에 pressure를 전달한다.

---

## CHAPTER 27 · Event loop fairness가 없으면 한 hot connection이 다른 connection을 굶길 수 있다

Edge-triggered drain loop가 한 descriptor를 EAGAIN까지 읽는 동안 대량 data가 계속 들어오면 다른 ready connection 처리가 늦어질 수 있다. Completion loop에서도 CQ를 한 request class가 독점할 수 있다.

Per-iteration work budget, connection quota, priority queue를 사용해 fairness를 관리할 수 있다. Throughput 최대화와 per-connection latency isolation은 다른 목표다.

---

## CHAPTER 28 · Async debugging은 request lifecycle trace가 없으면 거의 불가능하다

Stack trace는 현재 call stack을 보여 주지만 submit→kernel wait→completion→callback 사이에 stack가 끊어진다. Operation ID를 submission/completion log와 trace span에 유지해야 causal chain을 복원할 수 있다.

필수 field는 op type, fd/resource identity, submission timestamp, completion timestamp, bytes/result, cancel/timeout state, queue depth다. 이 정보 없이 `callback이 늦었다`는 증상만으로 kernel/device/application delay를 구분할 수 없다.

---

## CHAPTER 29 · Async API benchmark는 syscall 수만 세면 안 된다

io_uring이 syscall transition을 줄였더라도 ring synchronization, worker fallback, pinned memory, polling CPU가 증가할 수 있다. Epoll 기반 implementation은 syscall 수가 많아도 workload가 network wait 중심이면 차이가 작을 수 있다.

Benchmark는 requests/sec, p50/p99, CPU cycles/request, context switches, syscalls, in-flight depth, memory footprint, energy를 같이 비교한다. 동일 semantics와 overload policy를 맞춘 뒤 측정해야 한다.

---

## CHAPTER 30 · Async I/O 설계의 최종 계약은 ownership·ordering·completion·cancellation·backpressure다

비동기 엔진을 검토할 때 API 이름보다 다섯 가지 invariant를 확인한다.

1. **Ownership** — in-flight buffer/file/request state를 누가 언제까지 소유하는가.
2. **Ordering** — operation dependency와 publication order가 필요한가.
3. **Completion** — 하나의 logical request가 몇 completion을 만들며 partial result를 어떻게 처리하는가.
4. **Cancellation** — 이미 진행된 side effect와 race를 어떻게 reconcile하는가.
5. **Backpressure** — downstream capacity를 넘을 때 어디서 submission을 제한하는가.

이 다섯 조건이 명시되지 않은 async code는 callback 문법이 아무리 세련돼도 race, leak, overload에 취약하다.
