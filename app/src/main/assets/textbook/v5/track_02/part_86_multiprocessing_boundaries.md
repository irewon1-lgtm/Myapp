# PART 86 · Multiprocessing boundaries — process memory·start method·pickle·shared memory를 분리하기

`multiprocessing`은 thread와 달리 별도 process와 address space를 사용한다. 함수 하나를 worker로 보냈다고 해서 parent의 모든 object가 같은 방식으로 공유되는 것은 아니며, start method에 따라 초기 상태와 import behavior도 달라질 수 있다. 이 PART에서는 **process isolation → 데이터 전달 → 공유 상태 → lifecycle** 순서로 본다.

---

## CHAPTER 01 · process는 기본적으로 별도 address space를 가지므로 mutable object를 직접 공유하지 않는다

Parent가 list를 worker에 넘겼다고 해서 두 process가 같은 Python list object를 동시에 수정하는 모델은 아니다. 전달 방식과 platform에 따라 state가 복사·직렬화되거나 OS memory mechanism을 통해 이어질 수 있다.

이 차이는 race를 줄이는 대신 communication cost를 만든다. Process boundary를 넘는 argument가 크면 serialization과 copy가 계산 시간보다 더 비쌀 수 있다.

Shared state가 정말 필요한지 먼저 묻고, 가능하면 immutable input과 explicit result message로 owner를 분리한다.

---

## CHAPTER 02 · start method는 child가 어떤 초기 상태에서 시작하는지 결정한다

Process 생성 방식은 platform과 설정에 따라 다를 수 있다. Parent state를 기반으로 시작하는 방식과 fresh interpreter에서 module을 다시 import하는 방식은 global state, open handle, thread 상태에 다른 영향을 준다.

따라서 module top-level에서 process를 무조건 생성하거나 import side effect에 의존하는 code는 portability가 낮다. Entry-point guard와 명시적 initializer를 사용해 worker 시작 절차를 드러낸다.

Start method를 바꾸는 것은 성능 tuning만이 아니라 initialization semantics를 바꾸는 결정이다.

---

## CHAPTER 03 · process argument와 result는 serialization boundary를 통과할 수 있다

많은 multiprocessing API는 Python object를 pickle 계열 직렬화로 worker에 전달한다. Closure, open file handle, local class처럼 pickling이 어려운 object는 실패할 수 있다.

또 pickle은 신뢰되지 않은 bytes를 안전한 data format으로 읽는 용도가 아니다. Process 내부 IPC라 해도 payload source가 공격자에게 열려 있다면 code execution 위험을 고려한다.

Worker message는 작고 명시적인 DTO로 만드는 편이 호환성과 성능에 유리하다. 거대한 service object 전체를 보내지 않는다.

---

## CHAPTER 04 · Queue와 Pipe는 process 사이 ownership을 message로 이동시키는 도구다

Queue는 여러 producer/consumer가 message를 교환하기에 편하고 Pipe는 endpoint 관계가 더 직접적이다. 둘 다 “같은 object를 공유한다”보다 serialized message를 전달한다는 모델로 읽는 편이 안전하다.

Queue가 무제한처럼 보여도 memory와 internal buffer는 유한하다. Producer가 consumer보다 빠르면 backlog가 커진다. Max size와 application-level backpressure를 설계한다.

Process 종료 시 queue flush, sentinel, pending message 처리가 어떻게 되는지도 확인한다. 갑작스러운 terminate는 전달 중인 state를 잃을 수 있다.

---

## CHAPTER 05 · shared memory는 serialization 비용을 줄이지만 synchronization 책임을 다시 가져온다

Large numeric buffer처럼 copy가 비싼 data는 shared memory를 통해 여러 process가 같은 memory region을 볼 수 있다. 하지만 그 순간 ownership과 consistency 문제가 다시 생긴다.

한 process가 쓰는 동안 다른 process가 읽으면 어떤 상태가 보이는지, record boundary가 atomic한지, lock이나 version marker가 필요한지 설계해야 한다.

Shared memory handle의 생성·close·unlink lifecycle도 별도 resource management가 필요하다. Copy를 줄인 대가로 synchronization과 cleanup complexity가 증가한다.

---

## CHAPTER 06 · Manager proxy는 편리하지만 remote-like operation 비용을 숨길 수 있다

Manager가 제공하는 proxy list/dict는 여러 process가 공유 객체처럼 사용할 수 있게 보이지만 실제 operation은 manager process와의 IPC를 포함할 수 있다.

반복문에서 proxy를 수천 번 작은 단위로 읽으면 local dict와 전혀 다른 비용을 낼 수 있다. 가능한 경우 batch operation이나 message passing으로 round trip을 줄인다.

Proxy가 편하다는 이유로 모든 shared state를 manager에 넣지 않는다. 상태 owner를 하나 두고 command/result를 교환하는 architecture가 더 단순할 수 있다.

---

## CHAPTER 07 · process lifecycle은 start·health·shutdown·crash를 모두 관리해야 한다

Worker가 시작됐다는 것과 healthy하게 계속 실행된다는 것은 다르다. Child exit code, timeout, heartbeat가 필요할 수 있다.

Graceful shutdown에서는 새 작업 접수를 막고 queue를 drain한 뒤 worker에 sentinel을 보내고 join한다. 강제 terminate는 마지막 수단으로 두며 shared resource가 일관된 상태인지 확인한다.

Worker crash를 무조건 즉시 재시작하면 poison job이 반복 crash를 만들 수 있다. 실패 input을 격리하고 retry budget을 둔다.

---

## CHAPTER 08 · multiprocessing contract는 isolation 이득과 IPC 비용을 함께 계산한다

Process를 쓰기 전에 CPU-bound 이득이 있는지, data transfer가 얼마나 큰지, worker 함수가 serialization 가능한지, start method 차이를 감당할 수 있는지 확인한다.

테스트에서는 spawn-like fresh start, worker crash, unpicklable input, queue full, shared memory cleanup을 분리한다. Single-process unit test만으로 process lifecycle을 대체하지 않는다.

이 PART의 핵심은 **multiprocessing을 GIL 회피 버튼으로 보지 않고, 별도 address space와 serialization·IPC·resource lifecycle을 새로 도입하는 execution boundary로 이해하는 것**이다.
