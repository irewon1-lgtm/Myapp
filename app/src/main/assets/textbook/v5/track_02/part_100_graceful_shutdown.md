# PART 100 · Graceful shutdown — admission 중단·in-flight drain·resource close를 순서화하기

서비스 종료는 `sys.exit()` 한 줄이나 signal handler 하나로 끝나지 않는다. 종료 요청이 들어온 순간부터 새 작업을 받지 않고, 이미 시작된 작업을 제한 시간 안에 마무리하며, queue와 connection을 올바른 순서로 닫고, 마지막에는 강제 종료로 넘어갈 수 있어야 한다. 이 PART에서는 **종료 신호 → admission stop → drain → resource close → force-stop**을 하나의 state machine으로 설계한다.

---

## CHAPTER 01 · shutdown의 첫 단계는 새 작업 admission을 멈추는 것이다

### 시작 전 용어집

#### 1. shutdown

- **뜻:** 종료 중에도 새 request나 job을 계속 받으면 in-flight count가 줄지 않아 drain이 끝나지 않는다.
- **왜 중요한가:** 먼저 listener, scheduler, queue consumer의 admission을 닫는다.
- **예시:** RUNNING -> DRAINING / DRAINING 상태에서는 new work …

#### 2. admission

- **뜻:** Load balancer가 있는 서비스라면 readiness를 먼저 내리고 traffic이 다른 instance로 이동할 시간을 준다.
- **왜 중요한가:** Local worker라면 queue에서 새 item을 가져오는 것을 멈춘다.
- **예시:** RUNNING -> DRAINING / DRAINING 상태에서는 new work …

#### 3. queue

- **뜻:** HTTP에서는 명확한 unavailable response를 주고, batch scheduler에서는 job을 queue에 남겨 다른 worker가 가져가게 할 수 있다.
- **예시:** RUNNING -> DRAINING / DRAINING 상태에서는 new work …

```text
RUNNING -> DRAINING
DRAINING 상태에서는 new work = reject/redirect
```

 

새 작업 거절 방식도 contract다. 

---

## CHAPTER 02 · signal handler는 복잡한 cleanup을 직접 수행하기보다 종료 state를 시작한다

### 시작 전 용어집

#### 1. signal

- **뜻:** 운영체제 signal이나 application stop event가 들어오면 handler는 shutdown coordinator에 상태 변경을 알리고 실제 async cleanup은 정상 execution context에서 수행하는 편이 안전하다.
- **왜 중요한가:** Signal context에서 blocking I/O, 긴 logging, lock acquisition을 직접 수행하면 deadlock이나 reentrancy 문제가 생길 수 있다.
- **예시:** stop_event.set()

#### 2. cleanup

- **뜻:** Platform마다 signal delivery semantics도 다르므로 portable layer를 둔다.
- **왜 중요한가:** P18에서 signal 자체를 다뤘다면 여기서는 signal을 **전체 service lifecycle 전환의 trigger**로만 사용한다.
- **예시:** stop_event.set()

```python
stop_event.set()
```

 


---

## CHAPTER 03 · in-flight 작업은 끝낼 것과 취소할 것을 분류해 drain한다

### 시작 전 용어집

#### 1. in-flight

- **뜻:** 이미 commit 직전인 payment와 오래 걸리는 best-effort report를 같은 정책으로 종료할 필요는 없다.
- **왜 중요한가:** 작업 class마다 graceful completion 가치가 다르다.
- **예시:** stop admission -> wait in-flight -> close dependencies

#### 2. drain

- **뜻:** Task registry나 request counter를 통해 현재 진행 중인 작업을 추적하고, admission stop 뒤 count가 0이 될 때까지 기다릴 수 있다.
- **왜 중요한가:** Background fire-and-forget task가 registry 밖에 있으면 shutdown coordinator가 존재를 알 수 없다.
- **예시:** stop admission -> wait in-flight -> close dependencies

#### 3. shutdown

- **뜻:** P83의 task ownership을 shutdown lifecycle과 연결한다.
- **예시:** stop admission -> wait in-flight -> close dependencies

```text
stop admission -> wait in-flight -> close dependencies
```

 

---

## CHAPTER 04 · shutdown에도 deadline이 있어야 무한 drain을 막을 수 있다

### 시작 전 용어집

#### 1. shutdown

- **뜻:** Graceful shutdown이 무한히 기다리면 orchestration platform의 kill timeout을 넘겨 결국 갑작스럽게 종료될 수 있다.
- **왜 중요한가:** 전체 shutdown budget을 정하고 각 단계가 남은 시간을 소비하게 한다.
- **예시:** 예를 들어 30초 budget에서 5초는 traffic drain, 20초는 …

#### 2. deadline

- **뜻:** 한 task가 deadline을 넘기면 cooperative cancellation을 시도하고, 그래도 멈추지 않으면 process-level termination이 마지막 경계가 된다.
- **왜 중요한가:** “graceful”은 무제한 대기와 동의어가 아니다.
- **예시:** 예를 들어 30초 budget에서 5초는 traffic drain, 20초는 …

#### 3. drain

- **뜻:** 예를 들어 30초 budget에서 5초는 traffic drain, 20초는 in-flight completion, 마지막 5초는 cancellation과 resource close에 배정할 수 있다.
- **예시:** 예를 들어 30초 budget에서 5초는 traffic drain, 20초는 …

---

## CHAPTER 05 · resource close order는 dependency graph의 역순으로 정한다

### 시작 전 용어집

#### 1. resource close

- **뜻:** Worker가 DB pool을 사용한다면 worker task가 끝나기 전에 DB pool을 닫아서는 안 된다.
- **왜 중요한가:** 일반적으로 acquisition/initialization dependency의 역순으로 종료한다.
- **예시:** HTTP listener -> request tasks -> job workers …

#### 2. order

- **뜻:** Logging exporter를 너무 일찍 닫으면 뒤 단계 cleanup 오류를 기록하지 못한다.
- **왜 중요한가:** 반대로 process 종료 직전까지 비동기 logger를 남기면 flush 시간도 shutdown budget에 포함해야 한다.
- **예시:** HTTP listener -> request tasks -> job workers …

#### 3. dependency graph

- **뜻:** Resource manager가 dependency graph를 알고 있으면 종료 순서를 중앙에서 관리하기 쉽다.
- **예시:** HTTP listener -> request tasks -> job workers …

```text
HTTP listener -> request tasks -> job workers -> DB/client pools -> logging exporter
```

 


---

## CHAPTER 06 · queue는 drain·requeue·discard 중 어떤 정책을 쓸지 명시한다

### 시작 전 용어집

#### 1. queue

- **뜻:** In-memory queue의 item은 process가 종료되면 사라질 수 있다.
- **왜 중요한가:** Durable queue에서는 ack하기 전 item을 다른 consumer가 다시 가져갈 수 있다.
- **예시:** In-memory queue의 item은 process가 종료되면 사라질 수 있다.

#### 2. drain

- **뜻:** Queue가 비워질 때까지 기다리는 drain과 새 worker에게 넘기는 requeue는 workload 특성에 따라 선택한다.
- **왜 중요한가:** 따라서 shutdown 시 현재 처리 중 item과 아직 시작하지 않은 item을 구분한다.
- **예시:** Queue가 비워질 때까지 기다리는 drain과 새 worker에게 넘기는 …

#### 3. discard

- **뜻:** 작업 시작 전에 dequeue를 확정하는 구조라면 종료 중 unprocessed item을 durable store에 되돌려야 할 수 있다.
- **왜 중요한가:** Poison job과 retry count도 유지해야 한다.
- **예시:** 작업 시작 전에 dequeue를 확정하는 구조라면 종료 중 …

---

## CHAPTER 07 · force stop은 실패가 아니라 bounded shutdown protocol의 마지막 단계다

### 시작 전 용어집

#### 1. force stop

- **뜻:** 따라서 durable state는 force stop을 견디도록 P91의 atomic update, P98의 idempotency 같은 원칙과 결합한다.
- **왜 중요한가:** Shutdown correctness를 “항상 cleanup callback이 실행된다”는 가정에만 의존시키지 않는다.
- **예시:** 따라서 durable state는 force stop을 견디도록 P91의 atomic …

#### 2. 실패

- **뜻:** 외부 library가 block되어 cancellation에 응답하지 않거나 process가 deadlock 상태라면 graceful path만으로 종료할 수 없다.
- **왜 중요한가:** Supervisor가 최종 kill을 수행할 수 있어야 한다.
- **예시:** 외부 library가 block되어 cancellation에 응답하지 않거나 process가 deadlock …

#### 3. bounded shutdown

- **뜻:** Force-stop 발생 횟수는 운영 metric으로 남긴다.
- **왜 중요한가:** 자주 발생한다면 특정 dependency의 timeout/cancellation이 잘못 설계된 신호다.
- **예시:** Force-stop 발생 횟수는 운영 metric으로 남긴다.

---

## CHAPTER 08 · shutdown contract는 종료 중 각 component의 책임을 상태 전이로 고정한다

### 시작 전 용어집

#### 1. shutdown

- **뜻:** 테스트에서는 종료와 동시에 새 request가 오는 경우, 오래 걸리는 task, queue item, cleanup exception, 두 번의 종료 신호, shutdown deadline 초과를 각각 재현한다.
- **왜 중요한가:** 단순 정상 exit test만으로는 운영 종료 경로를 검증할 수 없다.
- **예시:** 테스트에서는 종료와 동시에 새 request가 오는 경우, 오래 …

#### 2. component

- **뜻:** Service는 RUNNING, DRAINING, STOPPING, STOPPED 같은 상태를 가질 수 있고 각 상태에서 admission, task 생성, resource access 허용 범위를 정의한다.
- **왜 중요한가:** 이 PART의 핵심은 **shutdown을 프로그램 마지막 한 줄로 보지 않고, 새 작업 차단부터 기존 작업 정리와 강제 종료까지 순서를 가진 bounded lifecycle protocol로 설계하는 것**이다.
- **예시:** Service는 RUNNING, DRAINING, STOPPING, STOPPED 같은 상태를 가질 …

---

## 실전 학습 루프 · graceful shutdown

### 1. 쉬운 예

서버가 종료 신호를 받자마자 process를 끝내면 처리 중 요청과 buffer 데이터가 사라질 수 있다. 새 작업 수락을 중단하고, 진행 중 작업에 deadline을 주고, flush·checkpoint·자원 close를 순서대로 수행해야 한다.

### 2. 한 줄 해석

graceful shutdown은 “기다린다”가 아니라 admission stop → drain → persist/cleanup → exit의 상태 머신이다.

### 3. 직접 실행

아래 최소 예제를 실행하기 전에 **성공 경로와 실패 경로를 각각 한 줄로 예측**한다.

```python
import asyncio

accepting = True
inflight = set()

async def shutdown():
    global accepting
    accepting = False
    if inflight:
        await asyncio.wait(inflight, timeout=5)
```

### 4. 수정 실습

1. drain이 영원히 끝나지 않도록 shutdown deadline을 둔다.
2. 재시작 후 미완료 작업을 복구할 수 있게 checkpoint/queue semantics를 연결한다.

수정 뒤에는 같은 입력을 여러 번 실행하거나 중간 crash를 가정해 결과가 중복·누락·무한 대기로 바뀌지 않는지 확인한다.

### 5. 확인 문제

graceful shutdown이면 모든 작업이 끝날 때까지 무한히 기다려야 할까?

### 6. 정답과 오답 설명

**정답:** 아니다. 운영 환경에는 종료 deadline이 필요하며 이후 강제 종료·재처리 정책을 정의해야 한다.

**자주 나오는 오답:** “SIGTERM을 catch했다”만으로 graceful shutdown이 완성됐다고 보면 안 된다.

운영형 문제에서는 함수 한 번의 정상 출력보다 **재시도, 중복, timeout, crash, 재시작** 뒤의 상태가 더 중요하다. 마지막으로 이 기능이 어떤 상태를 영구 저장하고 어떤 상태를 다시 계산할 수 있는지 구분해 적는다.

## 현장 디버깅 체크 · graceful shutdown

### 증상에서 시작한다

배포 때 일부 요청이 끊기거나 worker가 종료되지 않아 orchestrator가 강제 kill하고 재시작 후 중복 작업이 생긴다. 먼저 재현 가능한 최소 payload와 operation id를 고정한다. 최종 상태만 고치면 중복·정밀도·복구 문제의 실제 발생 지점을 숨길 수 있다.

### 먼저 볼 증거

signal 수신 시각, admission stop, inflight count, drain deadline, checkpoint/flush/close 완료 시각을 기록한다. 가능하면 이 값을 하나의 trace 또는 audit record로 묶어 시간 순서를 복원한다.

### 일부러 실패시켜 보기

처리 중 요청·긴 작업·멈춘 dependency가 각각 있을 때 SIGTERM을 보내 deadline 내 종료와 재처리를 확인한다. 이런 반례가 자동 테스트에 들어가야 정상 예제만 통과하는 구현을 걸러낼 수 있다.

### 통과 기준

새 작업 수락이 먼저 멈추고 기존 작업은 정책대로 drain/cancel되며 durable state가 남은 뒤 정해진 시간 안에 process가 끝나야 한다. 통과 기준은 “에러가 안 난다”가 아니라 **어떤 입력과 실패 순서에서도 허용된 상태 집합을 벗어나지 않는다**로 적는다.

