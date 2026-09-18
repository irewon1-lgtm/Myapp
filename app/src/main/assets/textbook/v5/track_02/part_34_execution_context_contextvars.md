# PART 34 · Execution context와 ContextVar — 전역 상태 없이 request-local 정보를 전달하기

로그 correlation ID, 현재 사용자, tracing span처럼 함수 호출마다 parameter로 넘기기 번거롭지만 실행 흐름마다 달라야 하는 정보가 있다. 단순 global variable은 동시에 처리되는 요청이 서로 값을 덮어쓰고, thread-local은 async task가 한 thread에서 interleave될 때 충분하지 않을 수 있다. Python의 execution context와 `ContextVar`는 **현재 논리적 실행 흐름에 귀속된 동적 context**를 표현하는 도구다.

---

## CHAPTER 01 · global state는 모든 실행 흐름이 같은 하나의 값을 공유한다

### 시작 전 용어집

#### 1. global state

- **뜻:** ` 같은 module global에 요청 ID를 저장하면 단일 요청에서는 동작해 보일 수 있다.
- **왜 중요한가:** 하지만 두 요청이 겹치면 두 번째 요청이 값을 바꾼 뒤 첫 번째 요청의 log도 두 번째 ID를 사용할 수 있다.
- **예시:** ` 같은 module global에 요청 ID를 저장하면 단일 …

#### 2. module

- **뜻:** Global configuration처럼 application 전체에서 동일하고 startup 이후 변경되지 않는 값은 module-level immutable value가 적합할 수 있다.
- **왜 중요한가:** 반면 현재 요청과 사용자처럼 invocation마다 다른 값은 global에 두지 않는다.
- **예시:** Global configuration처럼 application 전체에서 동일하고 startup 이후 변경되지 …

#### 3. thread

- **뜻:** 문제는 thread 수가 아니라 state ownership이 process global이라는 데 있다.
- **왜 중요한가:** 값의 lifetime과 scope가 변수 저장 위치와 맞아야 한다.
- **예시:** 문제는 thread 수가 아니라 state ownership이 process global이라는 …

#### 4. process

- **뜻:** Global mutable state는 test 격리도 어렵게 한다.
- **왜 중요한가:** 한 test가 값을 바꾸고 복구하지 않으면 다음 test가 영향을 받는다.
- **예시:** Global mutable state는 test 격리도 어렵게 한다.

`current_request_id = ...  

  

  Parallel test에서는 같은 global을 동시에 변경할 수 있다.

가장 명시적인 해법은 필요한 context를 parameter로 전달하는 것이다. 하지만 logging처럼 application 깊은 모든 함수에 반복 parameter를 추가하는 비용이 커질 때 execution-local context가 대안이 된다.

---

## CHAPTER 02 · thread-local은 operating thread에 state를 붙이지만 task와 thread가 항상 같지는 않다

### 시작 전 용어집

#### 1. thread

- **뜻:** Thread-local storage는 각 thread가 같은 이름에 서로 다른 값을 가질 수 있게 한다.
- **왜 중요한가:** Traditional synchronous server에서 요청 하나가 한 thread에서 끝까지 처리된다면 current request context를 분리하는 데 사용할 수 있다.
- **예시:** Thread-local storage는 각 thread가 같은 이름에 서로 다른 …

#### 2. state

- **뜻:** Asyncio에서는 여러 task가 같은 event-loop thread를 번갈아 사용한다.
- **왜 중요한가:** Thread-local만 사용하면 task A가 값을 설정한 뒤 await하고 task B가 다른 값을 설정했을 때 A가 재개해서 B의 값을 볼 수 있다.
- **예시:** Asyncio에서는 여러 task가 같은 event-loop thread를 번갈아 사용한다.

#### 3. task

- **뜻:** 반대로 worker thread pool에 작업을 넘기면 async task context가 새 thread로 자동 전파되는지 API contract를 확인해야 한다.
- **왜 중요한가:** Execution context propagation은 scheduler boundary마다 다를 수 있다.
- **예시:** 반대로 worker thread pool에 작업을 넘기면 async task …

#### 4. asyncio

- **뜻:** 논리적 request와 OS thread identity가 일치하지 않는 것이다.
- **왜 중요한가:** Thread-local을 사용할 때도 cleanup이 필요하다.
- **예시:** 논리적 request와 OS thread identity가 일치하지 않는 것이다.

Thread pool worker가 다음 요청에 재사용되는데 이전 request value가 남아 있으면 data leakage가 생길 수 있다. Set/reset lifetime을 request boundary에 묶는다.

---

## CHAPTER 03 · ContextVar는 현재 logical context에서 값을 조회하게 한다

### 시작 전 용어집

#### 1. ContextVar

- **뜻:** `ContextVar`는 같은 variable object에 대해 execution context마다 다른 binding을 가질 수 있게 한다.
- **왜 중요한가:** Async task가 생성될 때 context가 어떤 방식으로 copy되고 child task가 값을 바꿨을 때 parent에 어떻게 보이는지 공식 semantics를 이해해야 한다.
- **예시:** request_id = ContextVar("request_id", default=None) / token = request_id.set("req-123")

#### 2. logical context

- **뜻:** Set은 단순 대입보다 이전 값을 복구할 token을 반환한다.
- **왜 중요한가:** Nested context에서 library가 잠시 값을 바꾸더라도 자신의 scope가 끝나면 정확한 이전 binding으로 되돌릴 수 있다.
- **예시:** request_id = ContextVar("request_id", default=None) / token = request_id.set("req-123")

#### 3. binding

- **뜻:** 이 stack-like restoration이 global assignment와 다른 중요한 성질이다.
- **왜 중요한가:** Default value를 둘 수 있지만 “context를 설정하지 않은 버그”를 조용히 숨기지 않는지 고려한다.
- **예시:** request_id = ContextVar("request_id", default=None) / token = request_id.set("req-123")

#### 4. set

- **뜻:** 반드시 존재해야 하는 request ID라면 missing 상태를 명시적으로 실패시키거나 log field에서 absent로 처리한다.
- **예시:** request_id = ContextVar("request_id", default=None) / token = request_id.set("req-123")

```python
request_id = ContextVar("request_id", default=None)

token = request_id.set("req-123")
try:
    handle_request()
finally:
    request_id.reset(token)
```

  

 

---

## CHAPTER 04 · context propagation은 task 생성 시점과 실행 boundary에 따라 결정된다

### 시작 전 용어집

#### 1. context propagation

- **뜻:** Parent coroutine에서 ContextVar를 설정한 뒤 새 task를 만들면 일반적으로 task가 생성 시점의 context를 상속하는 semantics를 제공할 수 있다.
- **왜 중요한가:** 이후 parent와 child가 각각 값을 바꿔도 독립 context로 진행될 수 있다.
- **예시:** Parent coroutine에서 ContextVar를 설정한 뒤 새 task를 만들면 …

#### 2. task

- **뜻:** Task를 먼저 생성한 뒤 parent context 값을 바꾸면 child가 어느 값을 보는지 생성 시점 규칙에 따라 달라진다.
- **왜 중요한가:** “현재 global을 언제든 읽는다”는 모델보다 task가 가진 execution context snapshot으로 생각하는 편이 정확하다.
- **예시:** Task를 먼저 생성한 뒤 parent context 값을 바꾸면 …

#### 3. boundary

- **뜻:** Framework가 tracing context를 자동 propagation해도 custom background task와 executor boundary가 같은 정책을 따르는지 검증한다.
- **왜 중요한가:** 자동 전파는 보이지 않는 dependency이므로 observability test가 필요하다.
- **예시:** Framework가 tracing context를 자동 propagation해도 custom background task와 …

#### 4. API

- **뜻:** 정확한 behavior는 사용하는 Python version과 API를 확인한다.
- **왜 중요한가:** Queue worker처럼 long-lived task가 여러 job을 순차 처리하면 task 생성 시점의 context가 각 job에 맞지 않을 수 있다.
- **예시:** 정확한 behavior는 사용하는 Python version과 API를 확인한다.

Message metadata에서 context를 읽어 job 처리 scope마다 set/reset해야 한다.

 

---

## CHAPTER 05 · context는 parameter 전달을 줄이지만 숨은 dependency가 될 수 있다

### 시작 전 용어집

#### 1. context

- **뜻:** 함수 signature에 `request_id`가 없는데 내부에서 ContextVar를 읽으면 caller는 그 dependency를 쉽게 알지 못한다.
- **왜 중요한가:** Logging, tracing처럼 cross-cutting concern에는 이 trade-off가 받아들여질 수 있지만 domain 계산이 현재 사용자나 locale을 몰래 읽으면 test와 재사용이 어려워진다.
- **예시:** 함수 signature에 `request_id`가 없는데 내부에서 ContextVar를 읽으면 caller는 …

#### 2. parameter

- **뜻:** Business rule에 필요한 값은 일반 parameter나 explicit context object로 전달하고, diagnostics metadata처럼 핵심 결과를 바꾸지 않는 정보만 execution context에 두는 원칙을 사용할 수 있다.
- **왜 중요한가:** Authorization처럼 결과를 바꾸는 중요한 state를 숨은 context에 넣는 경우에는 architecture에서 명확히 문서화한다.
- **예시:** Business rule에 필요한 값은 일반 parameter나 explicit context …

#### 3. dependency

- **뜻:** ContextVar를 dependency injection 대체제로 사용하지 않는다.
- **왜 중요한가:** Database connection과 repository까지 current context에서 꺼내기 시작하면 service locator와 비슷한 hidden coupling이 생긴다.
- **예시:** ContextVar를 dependency injection 대체제로 사용하지 않는다.

#### 4. 함수

- **뜻:** 좋은 기준은 함수의 return과 side effect가 context 값에 따라 달라지는가다.
- **왜 중요한가:** 그렇다면 caller contract에 해당 dependency가 보이는 편이 더 안전할 가능성이 높다.
- **예시:** 좋은 기준은 함수의 return과 side effect가 context 값에 …

---

## CHAPTER 06 · logging context는 ContextVar가 강한 사용 사례다

### 시작 전 용어집

#### 1. logging

- **뜻:** Request entry에서 correlation ID, tenant ID, trace ID를 context에 설정하면 깊은 library code가 logger에 별도 parameter를 계속 넘기지 않아도 structured log field를 추가할 수 있다.
- **왜 중요한가:** 각 concurrent task가 독립 context를 가지면 log가 요청 사이에 섞이는 것을 줄인다.
- **예시:** `request_id=None`과 아예 context outside request를 구분해도 된다.

#### 2. ContextVar

- **뜻:** Logger adapter나 filter가 ContextVar를 읽어 record에 field를 추가할 수 있다.
- **왜 중요한가:** 하지만 secret과 개인정보를 context에 넣고 모든 log에 자동 첨부하면 정보 노출이 확대된다.
- **예시:** `request_id=None`과 아예 context outside request를 구분해도 된다.

#### 3. parameter

- **뜻:** Observability metadata는 안전한 식별자 중심으로 제한한다.
- **왜 중요한가:** Context가 없는 startup/background log도 존재할 수 있으므로 formatter가 missing field에서 crash하지 않게 한다.
- **예시:** `request_id=None`과 아예 context outside request를 구분해도 된다.

#### 4. observability

- **뜻:** `request_id=None`과 아예 context outside request를 구분해도 된다.
- **왜 중요한가:** Log test에서는 두 async task가 서로 다른 ID를 설정하고 interleave될 때 각각의 record가 올바른 ID를 가지는지 실제로 검증할 수 있다.
- **예시:** `request_id=None`과 아예 context outside request를 구분해도 된다.

Execution context isolation을 단위 기능이 아니라 concurrency property로 확인한다.

---

## CHAPTER 07 · tracing span은 parent-child context를 동적으로 연결한다

### 시작 전 용어집

#### 1. tracing span

- **뜻:** Distributed tracing에서는 현재 span을 context에 보관하고 하위 operation이 자동으로 child span을 만들 수 있다.
- **왜 중요한가:** Function parameter마다 span을 전달하지 않아도 call tree와 trace tree를 비슷하게 유지할 수 있다.
- **예시:** Distributed tracing에서는 현재 span을 context에 보관하고 하위 operation이 …

#### 2. parent-child context

- **뜻:** Nested span을 시작하면 current span을 child로 바꾸고 scope가 끝날 때 parent로 복구한다.
- **왜 중요한가:** Token-based reset과 잘 맞는 구조다.
- **예시:** Nested span을 시작하면 current span을 child로 바꾸고 scope가 …

#### 3. parameter

- **뜻:** Exception이 발생해도 `finally`나 context manager로 복구하지 않으면 이후 log와 span이 잘못된 parent에 연결될 수 있다.
- **왜 중요한가:** Remote request를 보낼 때 local ContextVar 자체가 network를 건너가지는 않는다.
- **예시:** Exception이 발생해도 `finally`나 context manager로 복구하지 않으면 이후 …

#### 4. scope

- **뜻:** Trace ID를 protocol header로 serialize하고 remote service가 새 local context를 구성해야 한다.
- **왜 중요한가:** Process-local execution context와 distributed propagation을 구분한다.
- **예시:** Trace ID를 protocol header로 serialize하고 remote service가 새 …

Untrusted inbound trace header를 그대로 권한 정보로 사용하지 않는다. Correlation metadata와 authentication identity는 다른 security level을 가진다.

---

## CHAPTER 08 · executor와 native thread boundary에서는 context 전파를 명시적으로 확인한다

### 시작 전 용어집

#### 1. executor

- **뜻:** Async coroutine이 blocking function을 thread executor에 넘기면 새 worker thread에서 현재 ContextVar가 자동으로 보존되는지 사용하는 helper API에 따라 다를 수 있다.
- **왜 중요한가:** 일부 high-level API는 context copy를 지원하고 low-level executor submission은 그렇지 않을 수 있다.
- **예시:** 하지만 모든 context를 무조건 전파하면 parent request secret이나 …

#### 2. native thread

- **뜻:** Context가 필요하다면 `copy_context()` 같은 mechanism으로 현재 context를 capture해 worker에서 실행할 수 있다.
- **왜 중요한가:** 하지만 모든 context를 무조건 전파하면 parent request secret이나 transaction-local state가 예상 밖의 background worker에 전달될 수 있다.
- **예시:** Context가 필요하다면 `copy_context()` 같은 mechanism으로 현재 context를 capture해 …

#### 3. boundary

- **뜻:** Process boundary가 hidden context를 explicit data로 바꾸는 지점이다.
- **왜 중요한가:** Execution boundary별 propagation table을 만들면 async task, thread, process, network 중 어디까지 자동인지 알 수 있다.
- **예시:** Process boundary가 hidden context를 explicit data로 바꾸는 지점이다.

#### 4. context

- **뜻:** Process pool에서는 memory context를 공유하지 않으므로 필요한 metadata를 serialization 가능한 argument/message로 전달해야 한다.
- **왜 중요한가:** Framework upgrade 시 이 contract가 달라지는지도 integration test로 보호한다.
- **예시:** Process pool에서는 memory context를 공유하지 않으므로 필요한 metadata를 …

---

## CHAPTER 09 · context leak은 값이 틀리는 것뿐 아니라 tenant data 노출로 이어질 수 있다

### 시작 전 용어집

#### 1. context leak

- **뜻:** Multi-tenant server에서 현재 tenant ID가 이전 요청에서 남아 다음 요청에 재사용되면 잘못된 database filter, cache key, log metadata가 만들어질 수 있다.
- **왜 중요한가:** Context cleanup은 단순 hygiene가 아니라 isolation 경계다.
- **예시:** Multi-tenant server에서 현재 tenant ID가 이전 요청에서 남아 …

#### 2. tenant data

- **뜻:** Entry middleware가 set하고 `finally`에서 reset하는 구조로 lifetime을 강제한다.
- **왜 중요한가:** Early return과 exception에도 cleanup이 실행되어야 한다.
- **예시:** Entry middleware가 set하고 `finally`에서 reset하는 구조로 lifetime을 강제한다.

#### 3. cache

- **뜻:** Manual `set` 후 여러 exit path가 있는 코드는 context manager로 감쌀 수 있다.
- **왜 중요한가:** Background task가 request context를 capture한 채 request 종료 후 오래 살아 있으면 user object와 large state의 lifetime도 연장할 수 있다.
- **예시:** Manual `set` 후 여러 exit path가 있는 코드는 …

#### 4. 경계

- **뜻:** Background job에 필요한 최소 immutable metadata만 복사하고 전체 request object를 capture하지 않는다.
- **왜 중요한가:** Security test에서는 request A와 B를 interleave해 tenant/context가 섞이지 않는지 확인한다.
- **예시:** Background job에 필요한 최소 immutable metadata만 복사하고 전체 …

Sequential happy path만으로는 leak을 발견하기 어렵다.

---

## CHAPTER 10 · execution context는 동적 scope이며 사용 범위를 좁게 유지한다

### 시작 전 용어집

#### 1. execution context

- **뜻:** 첫 번째는 explicit parameter/type으로, 두 번째는 dependency/config object로, 세 번째는 ContextVar 같은 execution context로 두는 방식이 일반적으로 추론하기 쉽다.
- **왜 중요한가:** Async/thread/process boundary에서 propagation을 자동으로 가정하지 않고 실제 API semantics를 검증한다.
- **예시:** 첫 번째는 explicit parameter/type으로, 두 번째는 dependency/config object로, …

#### 2. scope

- **뜻:** ContextVar는 lexical parameter와 달리 현재 execution path에서 암묵적으로 접근할 수 있는 동적 scope를 만든다.
- **왜 중요한가:** Logging과 tracing에는 강력하지만 모든 dependency를 넣으면 program data flow가 보이지 않게 된다.
- **예시:** ContextVar는 lexical parameter와 달리 현재 execution path에서 암묵적으로 …

#### 3. parameter

- **뜻:** Domain result에 직접 영향을 주는 핵심 input, infrastructure configuration, diagnostics context다.
- **왜 중요한가:** Set/reset은 resource acquire/release처럼 scope를 가져야 한다.
- **예시:** Domain result에 직접 영향을 주는 핵심 input, infrastructure …

#### 4. path

- **뜻:** Execution context를 배우는 목적은 global variable을 더 세련되게 숨기는 것이 아니다.
- **예시:** Execution context를 배우는 목적은 global variable을 더 세련되게 …

설계할 때 값의 의미를 세 종류로 나눌 수 있다.  

 

 **동시에 진행되는 논리적 작업마다 다른 metadata를 안전하게 유지하면서도 핵심 business dependency는 명시적으로 보존하는 것**이다.
