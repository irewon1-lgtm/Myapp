# PART 83 · Asyncio Task lifecycle — coroutine을 schedule하고 cancellation까지 책임지기

`async def`를 호출해 얻은 coroutine object와 event loop에 schedule된 Task는 같은 것이 아니다. Task는 coroutine 실행을 runtime에 등록하고, 결과·exception·cancellation 상태를 보존한다. Background task를 만들기만 하고 reference를 잃거나 exception을 회수하지 않으면 운영 중 누락된 실패와 lifetime 문제가 생길 수 있다. 이 PART에서는 **coroutine → Task → result/exception → cancellation → cleanup**의 전체 lifecycle을 본다.

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
