# PART 27 · Context-local state — 전역값 없이 실행 문맥을 전달하기

요청 ID, 현재 사용자, trace ID처럼 여러 함수가 함께 알아야 하지만 매 함수 parameter로 계속 전달하기 번거로운 값이 있다. 이를 global variable 하나에 넣으면 동시에 실행되는 요청이 서로 값을 덮어쓸 수 있다. Thread-local과 context-local storage는 **같은 코드가 여러 실행 문맥에서 동시에 돌아갈 때 각 문맥의 값을 분리하는 방법**이다. 편리한 만큼 숨은 의존성이 생기므로 어디까지 사용할지 경계를 정해야 한다.

---

## CHAPTER 01 · global state는 실행 문맥이 하나라는 가정을 숨긴다

`current_user = ...` 같은 module global은 코드 어디서든 읽기 쉽다. 단일 순차 script에서는 문제가 없어 보이지만 서버가 여러 요청을 동시에 처리하면 요청 A가 저장한 사용자를 요청 B가 덮어쓸 수 있다. 함수 signature에는 사용자 dependency가 보이지 않는데 결과는 global 값에 따라 달라진다.

Global configuration처럼 process 전체에서 정말 하나여야 하는 값과 request context처럼 실행마다 다른 값을 구분한다. 후자를 global로 표현하면 데이터 격리 오류와 보안 문제가 생길 수 있다. 특히 authorization이나 tenant ID처럼 다른 사용자 context가 섞이면 단순 bug가 아니라 데이터 노출이 된다.

가장 명시적인 해법은 필요한 값을 parameter로 전달하는 것이다. 호출 경로가 짧다면 이것이 가장 읽기 쉽다. Context-local은 깊은 logging/tracing stack처럼 같은 context를 많은 하위 함수가 참조하지만 핵심 업무 parameter로 매번 노출할 필요가 없을 때 후보가 된다.

숨은 의존성을 도입하기 전에 값의 lifetime과 ownership을 먼저 정의한다. Process-wide인지 thread-wide인지 async task 단위인지가 mechanism 선택을 결정한다.

---

## CHAPTER 02 · thread-local은 같은 process 안에서도 thread별 값을 분리한다

Thread-local storage는 각 thread가 같은 이름에 서로 다른 값을 보게 한다. Thread pool에서 요청을 처리할 때 current request ID를 thread-local에 넣으면 다른 thread의 요청과 직접 섞이지 않는다. 하지만 thread 자체가 재사용되므로 작업 종료 후 값을 정리하지 않으면 다음 작업이 이전 context를 볼 수 있다.

Worker thread가 하나의 task를 끝낸 뒤 pool로 돌아간다고 thread-local이 자동으로 업무적으로 초기화된다는 보장은 없다. Request boundary에서 set과 cleanup을 짝지어 관리한다. Exception path에서도 cleanup이 실행되도록 context manager나 `try/finally`를 사용한다.

Thread-local은 async task 분리에는 충분하지 않다. 한 thread의 event loop에서 여러 coroutine이 번갈아 실행되면 모두 같은 thread-local 값을 보게 된다. 요청 A가 await로 양보한 뒤 요청 B가 값을 바꾸면 A가 재개했을 때 B의 context를 읽을 수 있다.

따라서 thread-local은 실행 모델이 실제 thread ownership과 일치할 때 사용한다. Framework가 thread와 task를 어떻게 schedule하는지 모르고 선택하면 격리가 깨진다.

---

## CHAPTER 03 · context variable은 async task 문맥을 따라 값을 전달한다

Python의 context variable mechanism은 async task가 suspend/resume되어도 논리적 execution context에 맞는 값을 유지하도록 설계되어 있다. Event loop thread 하나에서 여러 task가 interleaving되어도 각 task가 자신이 상속받은 context를 볼 수 있다. 이것이 단순 thread-local과 다른 핵심이다.

Request handler 시작에서 correlation ID를 context variable에 설정하면 깊은 logging helper가 parameter 없이 해당 ID를 읽을 수 있다. 새로운 task를 만들 때 context가 어떻게 복사/전파되는지는 공식 semantics를 확인해야 하며, executor thread나 별도 process로 넘어갈 때 자동 전파를 가정하지 않는다.

Context variable은 mutable object를 담을 수도 있지만 값 자체를 변경하는 것과 object 내부 mutation을 구분한다. 여러 context가 같은 mutable object reference를 공유하면 variable binding은 분리되어도 객체 상태는 공유될 수 있다. Immutable ID나 작은 metadata를 담는 편이 안전하다.

Context-local storage가 dependency를 제거하는 것은 아니다. 단지 parameter list에서 실행 문맥으로 이동시킨다. 핵심 domain rule이 현재 사용자에 의존한다면 명시적 parameter가 더 나을 수 있다.

---

## CHAPTER 04 · token과 reset은 중첩된 문맥을 원래 값으로 복원하게 한다

Context 값을 임시로 바꿀 때 단순히 새 값을 set하고 마지막에 None을 넣는 방식은 바깥 scope가 원래 다른 값을 가지고 있었을 때 정보를 잃는다. Set operation이 반환하는 token을 사용해 이전 binding으로 reset하면 중첩된 scope를 stack처럼 안전하게 복원할 수 있다.

```text
outer request_id = A
→ inner operation sets request_id = B
→ inner finishes and resets token
→ outer request_id = A restored
```

이 패턴은 impersonation, nested trace span, temporary locale처럼 안쪽 scope가 context를 잠시 덮어쓸 때 중요하다. Exception이 발생해도 reset이 실행되도록 `try/finally`나 context manager로 묶는다.

Reset 누락은 값 leak을 만들고 잘못된 token 사용은 다른 context의 상태를 복원하려는 오류가 될 수 있다. Context lifecycle을 helper에 캡슐화하면 반복 실수를 줄일 수 있다.

Scope entry/exit가 명확한 값은 context manager interface와 잘 맞는다. 호출자는 내부 storage mechanism을 몰라도 lexical block 안에서만 context가 유효하다는 계약을 볼 수 있다.

---

## CHAPTER 05 · background task는 부모 요청의 context를 얼마나 상속할지 결정해야 한다

HTTP 요청 안에서 background task를 만들면 task가 부모 context의 request ID와 user metadata를 상속할 수 있다. 요청과 논리적으로 같은 작업이라면 유용하지만, 요청이 끝난 뒤 오래 살아 있는 task가 사용자 context를 계속 보유하면 lifetime과 보안 의미가 달라진다.

Background queue에 넘기는 작업은 필요한 context를 명시적인 message field로 serialize하는 편이 좋다. Process를 넘어가면 in-memory context variable은 자동 전달되지 않으며, trace ID와 tenant ID 중 무엇을 이어갈지 protocol로 정해야 한다.

새 task를 생성하는 helper가 context propagation을 자동으로 수행한다면 어디서 끊기는지 문서화한다. Executor thread로 blocking function을 넘길 때 context를 복사해 줄지, 독립된 system operation으로 볼지 결정한다.

Context propagation은 편의 기능이 아니라 causality model이다. 어떤 작업이 어느 요청의 일부인지 추적 가능하게 만드는 대신 불필요한 민감 정보까지 퍼뜨리지 않게 최소 context만 전달한다.

---

## CHAPTER 06 · logging context는 domain parameter와 분리할 가치가 있다

모든 함수에 `request_id`, `trace_id`, `log_fields`를 parameter로 넘기면 핵심 업무 interface가 관측성 metadata로 오염될 수 있다. 이런 값은 context-local storage에 두고 logging adapter가 자동으로 record에 붙이는 구조가 적합할 수 있다.

반대로 `customer_id`가 실제 가격 계산과 authorization에 필요하다면 logging context에서 꺼내 쓰게 만들지 않는다. 핵심 입력은 signature에 명시한다. **업무 의미에 필요한 값과 관측을 위한 문맥 값**을 구분하는 것이 context-local 남용을 막는다.

Structured logging formatter가 context variable을 읽어 모든 event에 correlation field를 붙일 수 있다. Context가 없는 startup/background event에서는 field absence를 정상 상태로 처리한다.

Test에서는 context를 명시적으로 설정하고 종료 후 reset해 다른 test로 leak되지 않게 한다. Test runner가 병렬 실행될 수 있으므로 global logger field를 수정하는 방식보다 context-local이 안정적일 수 있다.

---

## CHAPTER 07 · context-local state도 mutable singleton처럼 남용될 수 있다

Context마다 분리된다고 해서 모든 state를 넣어도 되는 것은 아니다. Shopping cart나 transaction object 전체를 context variable에 저장하면 함수들이 숨은 state에 의존하고 어느 코드가 수정했는지 추적하기 어려워진다. Context-local은 scope 문제를 해결하지만 encapsulation 문제를 해결하지 않는다.

작고 immutable한 identifier, tracing metadata, locale 같은 cross-cutting context에 제한하는 편이 일반적으로 관리하기 쉽다. 큰 mutable domain state는 object와 parameter를 통해 ownership을 명시한다.

Context variable lookup이 편리하다는 이유로 repository나 service가 직접 `current_tenant()`를 호출하면 unit test마다 실행 문맥을 만들게 되고 동일 service를 다른 환경에서 재사용하기 어려워질 수 있다. Boundary에서 context를 읽어 explicit dependency로 변환하는 방식도 가능하다.

사용 기준은 “여러 곳에서 필요하다”가 아니라 “이 값이 논리적 execution context의 속성인가”다.

---

## CHAPTER 08 · process 경계를 넘으면 context는 protocol data가 된다

Thread와 async task는 같은 process memory 안에서 context mechanism을 사용할 수 있지만 subprocess, worker service, remote API로 넘어가면 context는 자동으로 존재하지 않는다. Trace header, message metadata, command argument처럼 명시적 serialization이 필요하다.

모든 context field를 그대로 복사하지 않는다. Authentication credential은 downstream service에 필요한 최소 token으로 변환하고, 내부 debug field는 외부 trust boundary에 보내지 않는다. Context propagation은 security boundary를 통과하는 data transfer다.

Queue retry가 원래 request가 끝난 뒤 수시간 후 실행될 수도 있다. 사용자 session 같은 short-lived context를 그대로 재사용할 수 있는지, job identity와 submitter identity를 분리해야 하는지 본다.

Distributed tracing에서는 trace/span ID propagation standard를 따르는 편이 각 서비스가 독자적인 header를 만드는 것보다 interoperability가 좋다. Application-specific business context는 별도 schema로 관리한다.

---

## CHAPTER 09 · 테스트는 context leak과 isolation을 별도 failure class로 검증한다

단일 요청 test가 통과해도 context 격리가 올바른지 알 수 없다. 두 async task가 서로 다른 ID를 설정하고 await로 여러 번 interleave된 뒤에도 각자 자신의 값을 보는지 검증한다. Thread pool에서는 같은 worker가 연속 작업을 처리해도 이전 값이 남지 않는지 확인한다.

Exception path에서 cleanup이 되는지도 중요하다. Context를 set한 뒤 함수가 예외를 던졌을 때 다음 operation이 오염되지 않아야 한다. `finally`를 제거하는 mutation이 test에서 잡혀야 한다.

Background task가 parent context를 의도대로 상속하거나 끊는지 test한다. Framework upgrade로 propagation semantics가 바뀌면 observability와 security behavior가 달라질 수 있다.

이 테스트들은 일반 domain test와 다른 failure class다. Context mechanism을 도입했다면 격리와 lifetime을 검증층에 추가한다.

---

## CHAPTER 10 · context 설계는 scope·propagation·cleanup 세 계약으로 정리한다

Context-local 값을 도입할 때 첫째, 어느 execution scope에서 유효한지 정한다. Request, task, thread, process 중 무엇인가. 둘째, 새로운 task/thread/process로 얼마나 전파되는지 정한다. 셋째, scope가 끝날 때 누가 원래 상태를 복원하거나 제거하는지 정한다.

이 세 규칙이 모호하면 global state와 다른 형태의 숨은 coupling이 된다. 반대로 작은 cross-cutting metadata에 명확한 scope를 주면 parameter pollution을 줄이고 trace/log correlation을 안정적으로 만들 수 있다.

Core domain function은 가능한 한 explicit input으로 유지하고 infrastructure boundary가 context를 읽어 필요한 typed value로 넘기는 구조가 재사용성과 테스트에 유리하다. Context가 정말 필요한 logging/tracing layer만 직접 읽게 할 수 있다.

Context-local programming의 핵심은 **전역 상태를 더 영리하게 숨기는 것이 아니라 논리적 실행 문맥의 lifetime과 전파 범위를 코드 구조에 맞추는 것**이다.