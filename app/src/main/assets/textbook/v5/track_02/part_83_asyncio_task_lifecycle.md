# PART 83 · Asyncio Task lifecycle — coroutine을 schedule하고 cancellation까지 책임지기

`async def`를 호출해 얻은 coroutine object와 event loop에 schedule된 Task는 같은 것이 아니다. Task는 coroutine 실행을 runtime에 등록하고, 결과·exception·cancellation 상태를 보존한다. Background task를 만들기만 하고 reference를 잃거나 exception을 회수하지 않으면 운영 중 누락된 실패와 lifetime 문제가 생길 수 있다. 이 절에서는 **coroutine → Task → result/exception → cancellation → cleanup**의 전체 lifecycle을 본다.

---

## CHAPTER 01 · coroutine object와 Task는 실행 상태의 소유권이 다르다

### 시작 전 용어집

#### 1. coroutine object

- **뜻:** 이 시점의 `coro`는 await 가능한 coroutine object지만 아직 독립적으로 schedule되어 계속 실행되는 Task라고 단정할 수 없다.
- **왜 중요한가:** `await coro`로 현재 task가 직접 실행을 이어가거나 `asyncio.
- **예시:** async def fetch(): / ...

#### 2. Task

- **뜻:** create_task(coro)`로 별도 Task를 만들 수 있다.
- **왜 중요한가:** Task를 만든다는 것은 실행 ownership을 분리하는 결정이다.
- **예시:** async def fetch(): / ...

#### 3. 상태

- **뜻:** Caller가 바로 await하지 않고 background로 넘길 수 있으므로 누가 결과를 회수하고 실패를 처리할지 명확해야 한다.
- **예시:** async def fetch(): / ...

```python
async def fetch():
    ...

coro = fetch()
```

 

 

---

**현장 점검 83-1 — CHAPTER 01 · coroutine object와 Task는 실행 상태의 소유권이 다르다**
CHAPTER 01 · coroutine object와 Task는 실행 상태의 소유권이 다르다을 운영에서 검증할 때는 한 번의 실행 경로를 순서대로 나누고 자원 정리를 늦춰 누수가 생기는지 본다. PART 83 CHAPTER 1의 관찰 항목으로 요청 식별자와 상태 전이를 함께 남긴다. 실패 주입 뒤에는 같은 입력으로 다시 실행해 CHAPTER 01 · coroutine object와 Task는 실행 상태의 소유권이 다르다의 복구 경로가 반복 가능한지 확인한다. 수정 전과 수정 후의 상태를 비교할 때는 성공 여부뿐 아니라 중복, 누락, 대기 중인 작업, 닫히지 않은 자원까지 함께 본다. 통과 기준은 정상 경로가 성공하고 실패 경로도 정해진 오류 또는 복구 상태로 끝나며, 같은 시나리오를 반복해도 데이터 손실이나 무한 재시도가 생기지 않는 것이다.
## CHAPTER 02 · task scheduling은 즉시 완료가 아니라 event loop에 실행 기회를 등록하는 일이다

### 시작 전 용어집

#### 1. task scheduling

- **뜻:** `create_task` 뒤 바로 다음 줄에서 task가 완료됐다고 가정하면 안 된다.
- **왜 중요한가:** Task는 event loop가 control을 받을 때 coroutine을 진행한다.
- **예시:** task = asyncio.create_task(fetch()) / # 여기서 task가 반드시 …

#### 2. event loop

- **뜻:** Current coroutine이 CPU-bound loop를 오래 돌며 await하지 않으면 새 task가 실행 기회를 얻지 못할 수 있다.
- **왜 중요한가:** Cooperative scheduling에서 fairness는 await point와 runtime 구조에 의존한다.
- **예시:** task = asyncio.create_task(fetch()) / # 여기서 task가 반드시 …

#### 3. asyncio

- **뜻:** “Task를 만들었으니 병렬 실행된다”가 아니라 “독립 schedule unit으로 등록되었다”라고 이해한다.
- **예시:** task = asyncio.create_task(fetch()) / # 여기서 task가 반드시 …

```python
task = asyncio.create_task(fetch())
# 여기서 task가 반드시 완료된 것은 아니다.
```

 


---

**현장 점검 83-2 — CHAPTER 02 · task scheduling은 즉시 완료가 아니라 event loop에 실행 기회를 등록하는 일이다**
CHAPTER 02 · task scheduling은 즉시 완료가 아니라 event loop에 실행 기회를 등록하는 일이다을 운영에서 검증할 때는 의도한 상태를 먼저 적고 중복 요청을 한 번 만들어 본다. PART 83 CHAPTER 2의 관찰 항목으로 열린 자원과 종료된 자원을 따로 확인한다. 실패 주입 뒤에는 같은 입력으로 다시 실행해 CHAPTER 02 · task scheduling은 즉시 완료가 아니라 event loop에 실행 기회를 등록하는 일이다의 복구 경로가 반복 가능한지 확인한다. 수정 전과 수정 후의 상태를 비교할 때는 성공 여부뿐 아니라 중복, 누락, 대기 중인 작업, 닫히지 않은 자원까지 함께 본다. 통과 기준은 정상 경로가 성공하고 실패 경로도 정해진 오류 또는 복구 상태로 끝나며, 같은 시나리오를 반복해도 데이터 손실이나 무한 재시도가 생기지 않는 것이다.
## CHAPTER 03 · Task는 result와 exception을 완료 상태와 함께 보존한다

### 시작 전 용어집

#### 1. Task

- **뜻:** Task가 정상 완료되면 result를, 실패하면 exception을 보존한다.
- **왜 중요한가:** `await task`는 그 결과를 받거나 exception을 현재 coroutine으로 전파한다.
- **예시:** background = set() / task = asyncio.create_task(work())

#### 2. result

- **뜻:** Background task를 await하지 않고 버리면 exception이 적절한 boundary에서 처리되지 않을 수 있다.
- **왜 중요한가:** Fire-and-forget이 필요하다면 task registry나 callback을 두고 완료·실패를 관찰한다.
- **예시:** background = set() / task = asyncio.create_task(work())

#### 3. exception

- **뜻:** 이 pattern도 exception logging policy를 별도로 고려해야 한다.
- **왜 중요한가:** Reference 보존과 failure consumption은 다른 책임이다.
- **예시:** background = set() / task = asyncio.create_task(work())

```python
background = set()
task = asyncio.create_task(work())
background.add(task)
task.add_done_callback(background.discard)
```

 

---

## CHAPTER 04 · cancellation은 즉시 강제 종료가 아니라 취소 요청을 전달하는 protocol이다

### 시작 전 용어집

#### 1. cancellation

- **뜻:** cancel()`을 호출하면 task에 cancellation 요청이 들어가고 coroutine이 적절한 suspension point에서 이를 관찰할 수 있다.
- **왜 중요한가:** 외부 thread를 강제 kill하는 것과 다르다.
- **예시:** cancel()`을 호출하면 task에 cancellation 요청이 들어가고 coroutine이 적절한 …

#### 2. protocol

- **뜻:** Task가 cancellation을 catch하고 무시하면 계속 실행될 수 있다.
- **왜 중요한가:** 따라서 caller는 cancel 요청을 보낸 뒤 실제 completion을 await해 종료를 확인해야 할 수 있다.
- **예시:** Task가 cancellation을 catch하고 무시하면 계속 실행될 수 있다.

#### 3. thread

- **뜻:** Timeout 구현도 종종 내부 task cancellation과 연결되므로 timeout exception만 보고 underlying work가 완전히 멈췄다고 가정하지 않는다.
- **예시:** Timeout 구현도 종종 내부 task cancellation과 연결되므로 timeout …

`task. 

 


---

## CHAPTER 05 · cancellation 처리 중에도 cleanup을 수행하고 취소 의미는 보존해야 한다

### 시작 전 용어집

#### 1. cancellation

- **뜻:** Cancellation이 `use` 중 들어와도 `finally` cleanup은 실행되어야 한다.
- **왜 중요한가:** 하지만 cleanup 자체가 await를 포함하면 추가 cancellation interaction이 생길 수 있으므로 resource library의 contract를 확인한다.
- **예시:** async def worker(): / resource = await acquire()

#### 2. cleanup

- **뜻:** Cleanup 후 cancellation을 다시 전파하는 것이 일반적인 기본값이다.
- **왜 중요한가:** Cancellation exception을 넓은 `except`에서 잡고 정상값을 반환하면 상위 orchestration이 task가 취소되었다는 사실을 잃을 수 있다.
- **예시:** async def worker(): / resource = await acquire()

```python
async def worker():
    resource = await acquire()
    try:
        await use(resource)
    finally:
        await release(resource)
```

 

 

---

**현장 점검 83-5 — CHAPTER 05 · cancellation 처리 중에도 cleanup을 수행하고 취소 의미는 보존해야 한다**
CHAPTER 05 · cancellation 처리 중에도 cleanup을 수행하고 취소 의미는 보존해야 한다을 운영에서 검증할 때는 재현 가능한 최소 사례를 만들고 부분 실패를 한 지점에만 만든다. PART 83 CHAPTER 5의 관찰 항목으로 재시도 횟수와 최종 결과를 한 줄에 묶어 기록한다. 실패 주입 뒤에는 같은 입력으로 다시 실행해 CHAPTER 05 · cancellation 처리 중에도 cleanup을 수행하고 취소 의미는 보존해야 한다의 복구 경로가 반복 가능한지 확인한다. 수정 전과 수정 후의 상태를 비교할 때는 성공 여부뿐 아니라 중복, 누락, 대기 중인 작업, 닫히지 않은 자원까지 함께 본다. 통과 기준은 정상 경로가 성공하고 실패 경로도 정해진 오류 또는 복구 상태로 끝나며, 같은 시나리오를 반복해도 데이터 손실이나 무한 재시도가 생기지 않는 것이다.
## CHAPTER 06 · shield와 timeout은 cancellation ownership을 바꾸므로 좁게 사용한다

### 시작 전 용어집

#### 1. shield

- **뜻:** Shielding은 외부 cancellation과 내부 awaitable의 취소 전파 관계를 바꾸는 도구지만 무분별하게 사용하면 종료가 늦어지고 orphan work가 남는다.
- **왜 중요한가:** Timeout도 scope를 어디에 두느냐에 따라 acquisition, body, cleanup 중 어떤 단계가 제한되는지 달라진다.
- **예시:** Shielding은 외부 cancellation과 내부 awaitable의 취소 전파 관계를 …

#### 2. timeout

- **뜻:** 일부 작업은 caller가 취소되어도 내부 commit/cleanup을 끝까지 진행해야 할 수 있다.
- **왜 중요한가:** “전체 요청 5초”와 “각 network read 5초”는 다른 정책이다.
- **예시:** 일부 작업은 caller가 취소되어도 내부 commit/cleanup을 끝까지 진행해야 …

#### 3. cancellation

- **뜻:** Cancellation control은 local syntax trick이 아니라 workload ownership policy다.
- **예시:** Cancellation control은 local syntax trick이 아니라 workload ownership …

---

## CHAPTER 07 · Task reference와 scope를 잃으면 background work의 lifetime이 불명확해진다

### 시작 전 용어집

#### 1. Task reference

- **뜻:** 반대로 task reference를 너무 오래 보관하면 completed task와 traceback이 memory를 붙잡을 수 있다.
- **왜 중요한가:** Structured concurrency의 핵심 아이디어는 child task lifetime을 부모 scope와 연결하는 것이다.
- **예시:** 반대로 task reference를 너무 오래 보관하면 completed task와 …

#### 2. scope

- **뜻:** Task group 같은 construct를 사용하면 scope 종료 전에 child completion/exception을 모으는 구조를 만들 수 있다.
- **왜 중요한가:** Manual registry를 사용하더라도 owner가 shutdown 시 남은 task를 cancel하고 await하는 규칙을 둔다.
- **예시:** Task group 같은 construct를 사용하면 scope 종료 전에 …

#### 3. background work

- **뜻:** Task를 생성한 component가 종료됐는데 child task가 계속 실행되면 stale session, closed resource, 이미 무효한 UI state를 접근할 수 있다.
- **예시:** Task를 생성한 component가 종료됐는데 child task가 계속 실행되면 …

---

## CHAPTER 08 · task contract는 생성·관찰·취소·종료 책임을 모두 지정한다

### 시작 전 용어집

#### 1. task contract

- **뜻:** Task를 만드는 함수는 누가 await할지, timeout/cancellation policy가 무엇인지, exception을 누가 log할지, owner shutdown에서 어떻게 종료할지 정해야 한다.
- **왜 중요한가:** 테스트에서는 정상 결과, child exception, cancel 요청, cleanup 중 cancellation, owner shutdown, background exception을 별도 case로 다룬다.
- **예시:** Task를 만드는 함수는 누가 await할지, timeout/cancellation policy가 무엇인지, …

#### 2. 함수

- **뜻:** 이 PART의 핵심은 **Task를 coroutine을 빨리 실행하는 편의 함수로 보지 않고, 실행·결과·실패·cancellation·lifetime을 보존하는 독립 runtime object로 관리하는 것**이다.
- **왜 중요한가:** 단순히 `create_task`가 호출됐는지만 검사하지 않는다.
- **예시:** 이 PART의 핵심은 **Task를 coroutine을 빨리 실행하는 편의 …

---

## 실전 학습 루프 · asyncio Task lifecycle

### 1. 쉬운 예

coroutine을 Task로 만들면 event loop가 독립적으로 진행 상태를 관리한다. 생성만 하고 reference·await·cancel 정책을 잃어버리면 실패가 관찰되지 않거나 shutdown 시 작업이 남을 수 있다.

### 2. 한 줄 해석

Task는 “백그라운드 함수”가 아니라 완료·실패·취소 상태를 가진 비동기 작업 객체다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상한다. 그 다음 아래 최소 예제를 실행하고, 예상이 틀렸다면 **호출 순서와 상태 변화**를 표시한다.

```python
import asyncio

async def work():
    await asyncio.sleep(0.01)
    return 3

async def main():
    task = asyncio.create_task(work())
    print(await task)

asyncio.run(main())
```

### 4. 수정 실습

1. Task를 cancel하고 `CancelledError` 전파를 관찰한다.
2. 여러 Task를 만들 때 하나가 실패하면 나머지를 어떻게 정리할지 정책을 적는다.

수정 후에는 정상 예제만 다시 보지 말고 실패·경계·반복 호출 중 하나를 추가해 계약이 유지되는지 확인한다.

### 5. 확인 문제

`create_task()`를 호출한 뒤 reference를 버려도 실패 처리를 신경 쓰지 않아도 될까?

### 6. 정답과 오답 설명

**정답:** 아니다. 작업의 소유자, await 지점, 취소·실패 관찰 정책을 명확히 해야 한다.

**자주 나오는 오답:** “비동기니까 알아서 끝난다”는 생각은 resource leak과 조용한 실패를 만든다.

이 PART를 마칠 때는 해당 문법 이름을 외우는 데서 멈추지 말고 **언제 호출되는가 / 무엇을 읽거나 바꾸는가 / 실패하면 어디로 가는가** 세 문장으로 설명한다.

## 현장 디버깅 체크 · asyncio Task lifecycle

### 증상에서 시작한다

Task를 만들었는데 종료 시 “pending task”가 남거나 실패 예외가 뒤늦게 경고로 나타난다. 재현 시점의 입력과 작업 식별자를 먼저 고정하고, 결과를 보고 추측하기보다 상태 전이를 시간순으로 적는다.

### 먼저 볼 증거

Task 생성 지점, owner, done/cancelled/exception 상태, await/cancel 호출 위치를 같은 request id로 연결한다. 한 숫자만 보지 말고 **대기/실행/완료/실패**를 분리하면 병목과 논리 오류를 구분하기 쉽다.

### 일부러 실패시켜 보기

부모 coroutine이 먼저 취소되는 상황을 만들고 child Task가 살아남는지, 예외가 어디에서 관찰되는지 확인한다. 정상 경로는 원래 잘 되는 경우가 많다. 강제 실패에서 cleanup·retry·재시작 의미가 유지되는지가 운영 품질을 결정한다.

### 통과 기준

모든 Task는 누가 소유하고 누가 완료·실패·취소를 관찰하는지 명확해야 하며 shutdown 뒤 orphan Task가 남지 않아야 한다. 이 기준을 regression test와 운영 metric 두 곳에 동시에 연결하면 배포 뒤 같은 문제가 돌아왔을 때 빠르게 탐지할 수 있다.

