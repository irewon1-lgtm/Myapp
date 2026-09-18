# PART 86 · Multiprocessing boundaries — process memory·start method·pickle·shared memory를 분리하기

`multiprocessing`은 thread와 달리 별도 process와 address space를 사용한다. 함수 하나를 worker로 보냈다고 해서 parent의 모든 object가 같은 방식으로 공유되는 것은 아니며, start method에 따라 초기 상태와 import behavior도 달라질 수 있다. 이 절에서는 **process isolation → 데이터 전달 → 공유 상태 → lifecycle** 순서로 본다.

---

## CHAPTER 01 · process는 기본적으로 별도 address space를 가지므로 mutable object를 직접 공유하지 않는다

### 시작 전 용어집

#### 1. process

- **뜻:** Parent가 list를 worker에 넘겼다고 해서 두 process가 같은 Python list object를 동시에 수정하는 모델은 아니다.
- **왜 중요한가:** 전달 방식과 platform에 따라 state가 복사·직렬화되거나 OS memory mechanism을 통해 이어질 수 있다.
- **예시:** Parent가 list를 worker에 넘겼다고 해서 두 process가 같은 …

#### 2. address space

- **뜻:** 이 차이는 race를 줄이는 대신 communication cost를 만든다.
- **왜 중요한가:** Process boundary를 넘는 argument가 크면 serialization과 copy가 계산 시간보다 더 비쌀 수 있다.
- **예시:** 이 차이는 race를 줄이는 대신 communication cost를 만든다.

#### 3. mutable

- **뜻:** Shared state가 정말 필요한지 먼저 묻고, 가능하면 immutable input과 explicit result message로 owner를 분리한다.
- **예시:** Shared state가 정말 필요한지 먼저 묻고, 가능하면 immutable …

---

## CHAPTER 02 · start method는 child가 어떤 초기 상태에서 시작하는지 결정한다

### 시작 전 용어집

#### 1. start method

- **뜻:** Start method를 바꾸는 것은 성능 tuning만이 아니라 initialization semantics를 바꾸는 결정이다.
- **왜 중요한가:** Process 생성 방식은 platform과 설정에 따라 다를 수 있다.
- **예시:** Start method를 바꾸는 것은 성능 tuning만이 아니라 initialization …

#### 2. child

- **뜻:** Parent state를 기반으로 시작하는 방식과 fresh interpreter에서 module을 다시 import하는 방식은 global state, open handle, thread 상태에 다른 영향을 준다.
- **왜 중요한가:** 따라서 module top-level에서 process를 무조건 생성하거나 import side effect에 의존하는 code는 portability가 낮다.
- **예시:** Parent state를 기반으로 시작하는 방식과 fresh interpreter에서 module을 …

#### 3. 상태

- **뜻:** Entry-point guard와 명시적 initializer를 사용해 worker 시작 절차를 드러낸다.
- **예시:** Entry-point guard와 명시적 initializer를 사용해 worker 시작 절차를 …

---

## CHAPTER 03 · process argument와 result는 serialization boundary를 통과할 수 있다

### 시작 전 용어집

#### 1. process

- **뜻:** 많은 multiprocessing API는 Python object를 pickle 계열 직렬화로 worker에 전달한다.
- **왜 중요한가:** Closure, open file handle, local class처럼 pickling이 어려운 object는 실패할 수 있다.
- **예시:** 많은 multiprocessing API는 Python object를 pickle 계열 직렬화로 …

#### 2. result

- **뜻:** 또 pickle은 신뢰되지 않은 bytes를 안전한 data format으로 읽는 용도가 아니다.
- **왜 중요한가:** Process 내부 IPC라 해도 payload source가 공격자에게 열려 있다면 code execution 위험을 고려한다.
- **예시:** 또 pickle은 신뢰되지 않은 bytes를 안전한 data format으로 …

#### 3. serialization

- **뜻:** Worker message는 작고 명시적인 DTO로 만드는 편이 호환성과 성능에 유리하다.
- **왜 중요한가:** 거대한 service object 전체를 보내지 않는다.
- **예시:** Worker message는 작고 명시적인 DTO로 만드는 편이 호환성과 …

---

## CHAPTER 04 · Queue와 Pipe는 process 사이 ownership을 message로 이동시키는 도구다

### 시작 전 용어집

#### 1. queue

- **뜻:** Queue는 여러 producer/consumer가 message를 교환하기에 편하고 Pipe는 endpoint 관계가 더 직접적이다.
- **왜 중요한가:** 둘 다 “같은 object를 공유한다”보다 serialized message를 전달한다는 모델로 읽는 편이 안전하다.
- **예시:** Queue는 여러 producer/consumer가 message를 교환하기에 편하고 Pipe는 endpoint …

#### 2. Pipe

- **뜻:** Queue가 무제한처럼 보여도 memory와 internal buffer는 유한하다.
- **왜 중요한가:** Producer가 consumer보다 빠르면 backlog가 커진다.
- **예시:** Queue가 무제한처럼 보여도 memory와 internal buffer는 유한하다.

#### 3. process

- **뜻:** Process 종료 시 queue flush, sentinel, pending message 처리가 어떻게 되는지도 확인한다.
- **왜 중요한가:** 갑작스러운 terminate는 전달 중인 state를 잃을 수 있다.
- **예시:** Process 종료 시 queue flush, sentinel, pending message …

#### 4. ownership

- **뜻:** Max size와 application-level backpressure를 설계한다.
- **예시:** Max size와 application-level backpressure를 설계한다.

---

## CHAPTER 05 · shared memory는 serialization 비용을 줄이지만 synchronization 책임을 다시 가져온다

### 시작 전 용어집

#### 1. shared memory

- **뜻:** Large numeric buffer처럼 copy가 비싼 data는 shared memory를 통해 여러 process가 같은 memory region을 볼 수 있다.
- **왜 중요한가:** 하지만 그 순간 ownership과 consistency 문제가 다시 생긴다.
- **예시:** Large numeric buffer처럼 copy가 비싼 data는 shared memory를 …

#### 2. serialization

- **뜻:** 한 process가 쓰는 동안 다른 process가 읽으면 어떤 상태가 보이는지, record boundary가 atomic한지, lock이나 version marker가 필요한지 설계해야 한다.
- **왜 중요한가:** Shared memory handle의 생성·close·unlink lifecycle도 별도 resource management가 필요하다.
- **예시:** 한 process가 쓰는 동안 다른 process가 읽으면 어떤 …

#### 3. synchronization

- **뜻:** Copy를 줄인 대가로 synchronization과 cleanup complexity가 증가한다.
- **예시:** Copy를 줄인 대가로 synchronization과 cleanup complexity가 증가한다.

---

## CHAPTER 06 · Manager proxy는 편리하지만 remote-like operation 비용을 숨길 수 있다

### 시작 전 용어집

#### 1. Manager proxy

- **뜻:** Manager가 제공하는 proxy list/dict는 여러 process가 공유 객체처럼 사용할 수 있게 보이지만 실제 operation은 manager process와의 IPC를 포함할 수 있다.
- **왜 중요한가:** 반복문에서 proxy를 수천 번 작은 단위로 읽으면 local dict와 전혀 다른 비용을 낼 수 있다.
- **예시:** Manager가 제공하는 proxy list/dict는 여러 process가 공유 객체처럼 …

#### 2. remote-like operation

- **뜻:** 가능한 경우 batch operation이나 message passing으로 round trip을 줄인다.
- **왜 중요한가:** Proxy가 편하다는 이유로 모든 shared state를 manager에 넣지 않는다.
- **예시:** 가능한 경우 batch operation이나 message passing으로 round trip을 …

#### 3. process

- **뜻:** 상태 owner를 하나 두고 command/result를 교환하는 architecture가 더 단순할 수 있다.
- **예시:** 상태 owner를 하나 두고 command/result를 교환하는 architecture가 더 …

---

## CHAPTER 07 · process lifecycle은 start·health·shutdown·crash를 모두 관리해야 한다

### 시작 전 용어집

#### 1. process

- **뜻:** Worker가 시작됐다는 것과 healthy하게 계속 실행된다는 것은 다르다.
- **왜 중요한가:** Child exit code, timeout, heartbeat가 필요할 수 있다.
- **예시:** Worker가 시작됐다는 것과 healthy하게 계속 실행된다는 것은 다르다.

#### 2. lifecycle

- **뜻:** Graceful shutdown에서는 새 작업 접수를 막고 queue를 drain한 뒤 worker에 sentinel을 보내고 join한다.
- **왜 중요한가:** 강제 terminate는 마지막 수단으로 두며 shared resource가 일관된 상태인지 확인한다.
- **예시:** Graceful shutdown에서는 새 작업 접수를 막고 queue를 drain한 …

#### 3. start

- **뜻:** Worker crash를 무조건 즉시 재시작하면 poison job이 반복 crash를 만들 수 있다.
- **왜 중요한가:** 실패 input을 격리하고 retry budget을 둔다.
- **예시:** Worker crash를 무조건 즉시 재시작하면 poison job이 반복 …

---

**현장 점검 86-7 — CHAPTER 07 · process lifecycle은 start·health·shutdown·crash를 모두 관리해야 한다**
CHAPTER 07 · process lifecycle은 start·health·shutdown·crash를 모두 관리해야 한다을 운영에서 검증할 때는 작업 하나만 남겨 단순화한 뒤 지연 응답을 한 번 발생시킨다. PART 86 CHAPTER 7의 관찰 항목으로 처리 시작 시각과 종료 시각을 같이 남긴다. 실패 주입 뒤에는 같은 입력으로 다시 실행해 CHAPTER 07 · process lifecycle은 start·health·shutdown·crash를 모두 관리해야 한다의 복구 경로가 반복 가능한지 확인한다. 수정 전과 수정 후의 상태를 비교할 때는 성공 여부뿐 아니라 중복, 누락, 대기 중인 작업, 닫히지 않은 자원까지 함께 본다. 통과 기준은 정상 경로가 성공하고 실패 경로도 정해진 오류 또는 복구 상태로 끝나며, 같은 시나리오를 반복해도 데이터 손실이나 무한 재시도가 생기지 않는 것이다.
## CHAPTER 08 · multiprocessing contract는 isolation 이득과 IPC 비용을 함께 계산한다

### 시작 전 용어집

#### 1. multiprocessing

- **뜻:** 이 PART의 핵심은 **multiprocessing을 GIL 회피 버튼으로 보지 않고, 별도 address space와 serialization·IPC·resource lifecycle을 새로 도입하는 execution boundary로 이해하는 것**이다.
- **왜 중요한가:** Process를 쓰기 전에 CPU-bound 이득이 있는지, data transfer가 얼마나 큰지, worker 함수가 serialization 가능한지, start method 차이를 감당할 수 있는지 확인한다.
- **예시:** 이 PART의 핵심은 **multiprocessing을 GIL 회피 버튼으로 보지 …

#### 2. isolation

- **뜻:** 테스트에서는 spawn-like fresh start, worker crash, unpicklable input, queue full, shared memory cleanup을 분리한다.
- **왜 중요한가:** Single-process unit test만으로 process lifecycle을 대체하지 않는다.
- **예시:** 테스트에서는 spawn-like fresh start, worker crash, unpicklable input, …

---

## 실전 학습 루프 · multiprocessing boundary

### 1. 쉬운 예

process는 메모리 공간이 분리되므로 일반 Python 객체를 그냥 공유하는 것이 아니다. 인자·결과는 직렬화되거나 shared memory 같은 별도 메커니즘을 거치며 startup 비용과 종료 정책도 thread와 다르다.

### 2. 한 줄 해석

multiprocessing은 GIL 회피 도구 하나가 아니라 process isolation·IPC·serialization 비용을 함께 가진 실행 모델이다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상하고, 실행 후에는 **어느 경계에서 상태나 의미가 바뀌었는지** 표시한다.

```python
from multiprocessing import Process

def work(x):
    print(x * x)

if __name__ == '__main__':
    p = Process(target=work, args=(4,))
    p.start()
    p.join()
```

### 4. 수정 실습

1. 큰 객체를 자주 넘길 때 serialization 비용이 계산 이득을 넘지 않는지 측정한다.
2. child process가 비정상 종료했을 때 parent가 exit code를 어떻게 처리할지 추가한다.

수정 전후를 비교할 때는 정상 경로만 보지 않고 실패 입력과 자원 한도도 함께 확인한다.

### 5. 확인 문제

process에 list를 넘기면 parent와 child가 같은 list 객체를 수정하는가?

### 6. 정답과 오답 설명

**정답:** 일반적으로 아니다. 주소 공간이 분리되며 전달 방식과 공유 메모리 사용 여부를 별도로 설계해야 한다.

**자주 나오는 오답:** thread의 공유 메모리 모델을 process에 그대로 적용하는 것이 흔한 오답이다.

마지막에는 이 주제를 **입력/신뢰 수준 → 변환 또는 대기 → 검증 → 결과/실패** 순서로 다시 설명한다. 이 순서가 보이면 실제 장애에서도 원인 경계를 빠르게 좁힐 수 있다.

## 현장 디버깅 체크 · multiprocessing boundary

### 증상에서 시작한다

child process는 정상처럼 보이는데 결과가 parent에 반영되지 않거나 시작·직렬화 비용이 계산 시간보다 커진다. 재현 시점의 입력과 작업 식별자를 먼저 고정하고, 결과를 보고 추측하기보다 상태 전이를 시간순으로 적는다.

### 먼저 볼 증거

process start method, pid, exit code, 전달 payload 크기, serialization 시간, IPC queue 상태를 본다. 한 숫자만 보지 말고 **대기/실행/완료/실패**를 분리하면 병목과 논리 오류를 구분하기 쉽다.

### 일부러 실패시켜 보기

pickle하기 어려운 객체와 큰 객체를 각각 전달해 실패 지점과 overhead를 분리한다. 정상 경로는 원래 잘 되는 경우가 많다. 강제 실패에서 cleanup·retry·재시작 의미가 유지되는지가 운영 품질을 결정한다.

### 통과 기준

process 경계를 넘는 데이터는 명시적으로 직렬화 가능한 형태여야 하고 child 실패가 parent에서 관찰·복구돼야 한다. 이 기준을 regression test와 운영 metric 두 곳에 동시에 연결하면 배포 뒤 같은 문제가 돌아왔을 때 빠르게 탐지할 수 있다.

