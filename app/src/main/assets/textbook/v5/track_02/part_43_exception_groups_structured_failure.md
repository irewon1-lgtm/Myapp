# PART 43 · ExceptionGroup과 structured failure — 여러 실패를 손실 없이 묶고 분리해 처리하기

동시에 여러 child task를 실행하면 한 번의 상위 operation에서 실패가 하나만 발생한다는 가정이 깨진다. 파일 세 개를 병렬 처리하다 두 개가 실패하거나 task group 종료 중 여러 cancellation/cleanup error가 생길 수 있다. 첫 exception만 남기고 나머지를 버리면 원인 정보가 손실되고, 모든 실패를 문자열 하나로 합치면 타입별 복구가 어려워진다. Structured failure의 핵심은 **여러 독립 실패를 계층 구조로 보존하면서 처리 가능한 일부만 분리하는 것**이다.

---

## CHAPTER 01 · 여러 child가 실패할 수 있는 operation은 결과도 하나가 아닐 수 있다

### 시작 전 용어집

#### 1. child

- **뜻:** Concurrent task는 이미 여러 개가 진행 중이므로 한 child가 실패한 시점에 다른 child도 별도의 실패를 만들 수 있다.
- **왜 중요한가:** Parent가 first error만 선택하면 다른 failure evidence가 사라진다.
- **예시:** Multiple failure는 예외적인 corner case가 아니라 structured concurrency와 …

#### 2. 실패

- **뜻:** Parent는 전체 operation을 실패시키더라도 개별 원인 타입과 context를 보존해야 한다.
- **왜 중요한가:** Batch processing에서도 여러 record 오류를 한꺼번에 보고 싶을 수 있다.
- **예시:** Multiple failure는 예외적인 corner case가 아니라 structured concurrency와 …

#### 3. operation

- **뜻:** Sequential code에서는 첫 exception이 정상 흐름을 중단하므로 뒤 operation은 실행되지 않는 경우가 많다.
- **왜 중요한가:** 모든 child failure가 같은 의미를 가지는 것도 아니다.
- **예시:** Multiple failure는 예외적인 corner case가 아니라 structured concurrency와 …

#### 4. exception

- **뜻:** 다만 error count가 매우 커지면 exception object 자체가 과도한 memory/log를 만들 수 있으므로 maximum detail과 summary 정책이 필요하다.
- **왜 중요한가:** Multiple failure는 예외적인 corner case가 아니라 structured concurrency와 병렬 작업의 정상적인 failure model이다.
- **예시:** 다만 error count가 매우 커지면 exception object 자체가 …

한 task의 validation error는 입력 문제이고 다른 task의 timeout은 external outage일 수 있다. 

 


---

## CHAPTER 02 · ExceptionGroup은 여러 exception을 하나의 계층적 예외로 운반한다

### 시작 전 용어집

#### 1. exception

- **뜻:** Python의 exception group은 여러 exception을 한 객체에 포함하면서 nested group 구조를 유지할 수 있다.
- **왜 중요한가:** Parent task가 child failure를 하나의 exception channel로 전달하되 각 원인의 type과 traceback을 버리지 않게 한다.
- **예시:** Python의 exception group은 여러 exception을 한 객체에 포함하면서 …

#### 2. 객체

- **뜻:** Group title/message는 aggregate operation의 context를 설명하고 내부 exception은 개별 작업의 원인을 가진다.
- **왜 중요한가:** File batch라면 어떤 file/task가 실패했는지 exception note나 typed wrapper에 identity를 포함할 수 있다.
- **예시:** Group title/message는 aggregate operation의 context를 설명하고 내부 exception은 …

#### 3. traceback

- **뜻:** Debug output은 tree 구조를 보여 줄 수 있으므로 단순 traceback list보다 parent-child relation을 보존한다.
- **왜 중요한가:** ExceptionGroup을 application 어디서나 무조건 사용하기보다 실제로 여러 failure를 동시에 보존해야 하는 boundary에서 사용한다.
- **예시:** Debug output은 tree 구조를 보여 줄 수 있으므로 …

#### 4. 실패

- **뜻:** Nested group은 task group 안에 또 다른 task group이 있을 때 자연스럽게 생길 수 있다.
- **왜 중요한가:** Single-error domain API를 필요 이상으로 복잡하게 만들지 않는다.
- **예시:** Nested group은 task group 안에 또 다른 task …

---

## CHAPTER 03 · `except*`는 group 안에서 처리 가능한 타입 부분만 분리한다

### 시작 전 용어집

#### 1. except

- **뜻:** 일반 exception handler가 group 전체를 하나의 객체로 받는 것과 달리 `except*` 계열 handling은 group tree에서 특정 exception type에 해당하는 부분을 선택해 처리할 수 있다.
- **왜 중요한가:** Timeout만 retry 대상으로 기록하고 validation error는 별도 report로 변환하는 식의 partial handling이 가능하다.
- **예시:** 일반 exception handler가 group 전체를 하나의 객체로 받는 …

#### 2. group

- **뜻:** 처리되지 않은 exception 부분은 다시 group 형태로 바깥에 전파될 수 있다.
- **왜 중요한가:** 따라서 하나의 handler가 group을 통째로 “성공 처리”했다고 착각하지 않는다.
- **예시:** 처리되지 않은 exception 부분은 다시 group 형태로 바깥에 …

#### 3. 타입

- **뜻:** 어떤 subtype이 소비되었고 무엇이 남았는지 확인한다.
- **왜 중요한가:** `except*` block 내부에서 받은 group은 원래 group의 subset 구조를 보존할 수 있다.
- **예시:** 어떤 subtype이 소비되었고 무엇이 남았는지 확인한다.

#### 4. 객체

- **뜻:** 내부 exception을 mutation해 다른 handler와 공유 state를 만든다고 가정하지 않고 exception metadata 추가 API를 사용한다.
- **왜 중요한가:** 복구 정책이 type 하나만으로 결정되지 않는다면 task identity와 operation state를 함께 본다.
- **예시:** 내부 exception을 mutation해 다른 handler와 공유 state를 만든다고 …

동일 timeout이어도 idempotent read와 payment write는 retry 정책이 다르다.

---

## CHAPTER 04 · TaskGroup은 child lifetime과 failure aggregation을 parent scope에 묶는다

### 시작 전 용어집

#### 1. TaskGroup

- **뜻:** Structured concurrency 도구는 scope 안에서 만든 child task가 scope 종료 전에 완료·취소·실패 상태로 정리되게 한다.
- **왜 중요한가:** 한 child failure가 sibling cancellation을 유발하고 종료 시 여러 failure가 group으로 전달될 수 있다.
- **예시:** Structured concurrency 도구는 scope 안에서 만든 child task가 …

#### 2. child lifetime

- **뜻:** 이 모델은 임의의 fire-and-forget task보다 lifetime이 명확하다.
- **왜 중요한가:** Parent function이 return했는데 child가 계속 shared state를 수정하는 상황을 줄인다.
- **예시:** 이 모델은 임의의 fire-and-forget task보다 lifetime이 명확하다.

#### 3. failure aggregation

- **뜻:** 어떤 child가 필수이고 어떤 child가 best-effort인지에 따라 task group을 분리할 수 있다.
- **왜 중요한가:** Sibling cancellation도 failure semantics의 일부다.
- **예시:** 어떤 child가 필수이고 어떤 child가 best-effort인지에 따라 task …

#### 4. parent scope

- **뜻:** Child A가 실패해 B가 취소되었다면 B의 cancellation을 원인 error와 같은 수준으로 사용자에게 보여 줄지, 내부 shutdown 결과로만 볼지 결정한다.
- **왜 중요한가:** Task group을 사용했다고 remote effect가 자동 rollback되는 것은 아니다.
- **예시:** Child A가 실패해 B가 취소되었다면 B의 cancellation을 원인 …

각 child가 이미 보낸 external write에 대해 idempotency와 compensation이 여전히 필요하다.

---

## CHAPTER 05 · aggregate validation과 concurrent failure는 같은 group이라도 복구 의미가 다르다

### 시작 전 용어집

#### 1. aggregate validation

- **뜻:** Form validation에서 field 오류 열 개를 한 번에 모으는 것은 사용자에게 모든 수정점을 알려 주기 위한 aggregate error다.
- **왜 중요한가:** Concurrent task failure group은 서로 독립적으로 진행된 operation 결과를 보존하려는 목적이 강하다.
- **예시:** Form validation에서 field 오류 열 개를 한 번에 …

#### 2. concurrent failure

- **뜻:** 같은 “여러 오류”라도 lifecycle과 retry 의미가 다르다.
- **왜 중요한가:** Validation error는 deterministic input에서 반복될 가능성이 높고 전체 set을 domain result object로 표현하는 편이 exception보다 자연스러울 수도 있다.
- **예시:** 같은 “여러 오류”라도 lifecycle과 retry 의미가 다르다.

#### 3. group

- **뜻:** 반면 unexpected concurrent exception은 stack unwinding과 resource cleanup이 필요해 exception group이 적합하다.
- **왜 중요한가:** Error representation을 선택할 때 실패가 정상 업무 결과인지 programming/infrastructure failure인지 본다.
- **예시:** 반면 unexpected concurrent exception은 stack unwinding과 resource cleanup이 …

#### 4. 오류

- **뜻:** ExceptionGroup을 validation 결과 자료구조의 대체재로 사용하지 않는다.
- **왜 중요한가:** Upper layer에서 여러 error source를 하나의 public error response로 번역할 때 내부 traceback을 외부에 노출하지 않고 safe code와 field message만 추출한다.
- **예시:** ExceptionGroup을 validation 결과 자료구조의 대체재로 사용하지 않는다.

---

## CHAPTER 06 · cleanup failure는 원래 operation failure를 덮어쓰지 않게 한다

### 시작 전 용어집

#### 1. cleanup failure

- **뜻:** 반대로 cleanup failure를 무조건 무시하면 connection pool corruption을 놓칠 수 있다.
- **왜 중요한가:** Primary failure와 cleanup failure의 severity와 recovery action을 구분해 log/metric에 남긴다.
- **예시:** Cleanup code를 단순하게 유지하는 것이 최선의 예방이다.

#### 2. operation failure

- **뜻:** Resource 사용 중 operation error가 발생한 뒤 cleanup에서도 error가 날 수 있다.
- **왜 중요한가:** 단일 exception 모델에서는 cleanup error가 original failure를 가려 root cause를 잃기 쉽다.
- **예시:** Cleanup code를 단순하게 유지하는 것이 최선의 예방이다.

#### 3. resource

- **뜻:** Context manager와 exception chaining/grouping을 사용해 둘 다 보존할 수 있는 정책을 설계한다.
- **왜 중요한가:** Database query가 실패한 뒤 connection close도 실패했다면 사용자에게 close failure만 보여 주면 실제 operation 원인이 사라진다.
- **예시:** Cleanup code를 단순하게 유지하는 것이 최선의 예방이다.

#### 4. exception

- **뜻:** Aggregate incident context에는 두 사건이 같은 operation에서 발생했다는 relation이 필요하다.
- **왜 중요한가:** Cleanup code를 단순하게 유지하는 것이 최선의 예방이다.
- **예시:** Aggregate incident context에는 두 사건이 같은 operation에서 발생했다는 …

정리 중 복잡한 network operation과 새 dependency를 만들수록 secondary failure space가 커진다.

---

## CHAPTER 07 · retry는 group 전체가 아니라 실패 단위의 idempotency를 본다

### 시작 전 용어집

#### 1. retry

- **뜻:** Retry 가능 unit을 record/task ID로 식별하고 성공한 작업과 실패한 작업 상태를 분리한다.
- **왜 중요한가:** Read-only query group은 전체 retry가 안전할 수 있지만 write group은 idempotency key와 commit status가 필요하다.
- **예시:** Retry 가능 unit을 record/task ID로 식별하고 성공한 작업과 …

#### 2. group

- **뜻:** 열 개 child 중 두 개가 timeout났다고 전체 batch 열 개를 다시 실행하면 이미 성공한 여덟 개 side effect가 중복될 수 있다.
- **왜 중요한가:** 어떤 exception type이 transient인지뿐 아니라 operation이 반복 가능한지도 함께 판단한다.
- **예시:** 열 개 child 중 두 개가 timeout났다고 전체 …

#### 3. 실패

- **뜻:** Partial retry 후에도 이전 failure와 새 failure를 어떻게 report할지 정한다.
- **왜 중요한가:** Attempt history를 무제한 exception nesting으로 보관하지 않고 bounded diagnostic metadata로 유지할 수 있다.
- **예시:** Partial retry 후에도 이전 failure와 새 failure를 어떻게 …

#### 4. idempotency

- **뜻:** Retry budget은 전체 parent deadline 안에 있어야 한다.
- **왜 중요한가:** 각 child가 독립적으로 세 번 retry하면 fan-out이 큰 operation에서 총 request 수가 폭증할 수 있다.
- **예시:** Retry budget은 전체 parent deadline 안에 있어야 한다.

---

## CHAPTER 08 · error aggregation은 cardinality와 개인정보 budget을 가진다

### 시작 전 용어집

#### 1. error aggregation

- **뜻:** 대규모 batch가 백만 건 실패했을 때 exception object에 백만 payload와 traceback을 모두 넣는 것은 현실적이지 않다.
- **왜 중요한가:** 대표 sample, error code별 count, first/last source position, quarantine reference를 사용해 bounded report를 만든다.
- **예시:** 대규모 batch가 백만 건 실패했을 때 exception object에 …

#### 2. cardinality

- **뜻:** Metric에는 exception type과 stage처럼 low-cardinality label을 사용하고 individual record ID는 log/trace로 보낸다.
- **왜 중요한가:** Error count가 많을수록 관측 시스템 자체가 overload되지 않게 sampling과 rate limit을 둔다.
- **예시:** Metric에는 exception type과 stage처럼 low-cardinality label을 사용하고 individual …

#### 3. budget

- **뜻:** Diagnostic detail budget도 failure handling의 resource contract다.
- **왜 중요한가:** User data 전체를 exception message에 포함하지 않는다.
- **예시:** Diagnostic detail budget도 failure handling의 resource contract다.

#### 4. 실패

- **뜻:** Error group이 여러 계층에서 log되면 민감정보 노출이 배가될 수 있다.
- **왜 중요한가:** Safe identifier와 redacted field를 사용한다.
- **예시:** Error group이 여러 계층에서 log되면 민감정보 노출이 배가될 …

---

## CHAPTER 09 · test는 multi-failure가 실제로 동시에 존재하는 조건을 만들어야 한다

### 시작 전 용어집

#### 1. test

- **뜻:** Exception group test에서 child 하나만 실패시키고 type만 확인하면 aggregation behavior를 검증한 것이 아니다.
- **왜 중요한가:** Barrier를 사용해 두 child가 모두 failure 직전까지 진행한 뒤 각각 다른 exception을 발생시키면 group이 두 원인을 보존하는지 확인할 수 있다.
- **예시:** Nested group에서도 구조가 예상대로 split되는지 확인한다.

#### 2. multi-failure

- **뜻:** `except*` handler test에서는 특정 subtype이 처리되고 다른 subtype이 재전파되는지 검사한다.
- **왜 중요한가:** Nested group에서도 구조가 예상대로 split되는지 확인한다.
- **예시:** `except*` handler test에서는 특정 subtype이 처리되고 다른 subtype이 …

#### 3. 조건

- **뜻:** Sibling cancellation cleanup도 별도 test한다.
- **왜 중요한가:** 한 child가 실패했을 때 다른 child의 context manager가 resource를 release하는지, background task가 남지 않는지 본다.
- **예시:** Sibling cancellation cleanup도 별도 test한다.

#### 4. exception group

- **뜻:** Failure order가 scheduling에 따라 달라질 수 있다면 exception list 순서를 public contract로 test하지 않고 set/identity와 tree semantics를 검증한다.
- **예시:** Failure order가 scheduling에 따라 달라질 수 있다면 exception …

---

## CHAPTER 10 · structured failure는 성공 구조만큼 실패 구조를 설계하는 습관이다

### 시작 전 용어집

#### 1. structured failure

- **뜻:** Structured failure의 핵심은 **동시에 여러 일이 진행되는 프로그램에서 첫 실패 하나만 선택하지 않고 각 failure의 타입·context·lifetime을 보존하며 처리 가능한 부분과 남은 부분을 명확히 나누는 것**이다.
- **왜 중요한가:** Fan-out operation을 설계할 때 성공 결과 배열만 생각하지 않고 “여러 child 중 일부가 실패하면 parent는 어떤 정보를 반환·발생시키는가, sibling은 취소되는가, 성공 side effect는 남는가”를 먼저 정한다.
- **예시:** Structured failure의 핵심은 **동시에 여러 일이 진행되는 프로그램에서 …

#### 2. 실패

- **뜻:** 모든 실패를 exception tree 하나로 해결하려 하지 않는다.
- **왜 중요한가:** Observability에는 parent operation ID와 child identity를 연결해 group traceback과 distributed trace를 함께 볼 수 있게 한다.
- **예시:** 모든 실패를 exception tree 하나로 해결하려 하지 않는다.

#### 3. exception

- **뜻:** ExceptionGroup은 이 정책을 표현하는 language mechanism 하나다.
- **왜 중요한가:** Domain batch result, quarantine, retry ledger와 함께 사용할 수 있다.
- **예시:** ExceptionGroup은 이 정책을 표현하는 language mechanism 하나다.

#### 4. observability

- **뜻:** Aggregate error를 한 줄 message로 축약하면 구조를 잃는다.
- **예시:** Aggregate error를 한 줄 message로 축약하면 구조를 잃는다.
