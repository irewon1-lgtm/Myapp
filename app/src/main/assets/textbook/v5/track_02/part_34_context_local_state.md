# PART 34 · Context-local state — thread·task마다 실행 문맥을 분리하기

요청 ID, 현재 사용자, trace ID처럼 여러 함수가 함께 알아야 하지만 매 함수 parameter로 계속 전달하기 번거로운 값이 있다. 이를 process global 하나에 넣으면 동시에 실행되는 작업이 서로 값을 덮어쓸 수 있다. Thread-local과 context-local storage는 **같은 코드가 여러 실행 문맥에서 동시에 돌아갈 때 문맥 값을 분리하는 방법**이다. 편리한 만큼 숨은 의존성이 생기므로 scope와 propagation을 계약으로 관리해야 한다.

---

## CHAPTER 01 · global state는 실행 문맥이 하나라는 가정을 숨긴다

`current_user = ...` 같은 module global은 코드 어디서든 읽기 쉽다. 단일 순차 script에서는 문제가 없어 보이지만 server가 여러 request를 동시에 처리하면 요청 A가 저장한 사용자를 요청 B가 덮어쓸 수 있다. 함수 signature에는 user dependency가 보이지 않는데 결과는 global 값에 따라 달라진다. Authorization이나 tenant ID가 섞이면 단순 계산 오류가 아니라 다른 사용자의 데이터가 노출되는 문제가 된다.

Process 전체에서 정말 하나여야 하는 configuration과 request마다 달라지는 context를 구분한다. 전자는 immutable startup configuration으로 관리할 수 있지만 후자를 module global로 표현하면 isolation contract가 없다. 가장 명시적인 해법은 필요한 값을 parameter로 전달하는 것이다. 호출 깊이가 짧고 domain 의미가 강하면 explicit parameter가 가장 읽기 쉽다.

Context-local state는 parameter 전달을 없애는 마법이 아니라 dependency의 위치를 바꾸는 방법이다. Log correlation처럼 깊은 infrastructure stack 전체가 동일 metadata를 필요로 하지만 business function의 입력으로 노출할 이유가 약할 때 유용하다. 값을 넣기 전에 process-wide인지 thread-wide인지 async task 단위인지 먼저 결정한다.

---

## CHAPTER 02 · thread-local은 thread마다 binding을 분리하지만 task lifetime과 같지 않다

Thread-local storage는 같은 process 안에서도 각 thread가 동일 이름에 서로 다른 값을 보게 한다. Thread pool에서 요청을 처리할 때 request ID를 thread-local에 넣으면 다른 thread의 요청과 직접 섞이지 않는다. 하지만 pool의 worker thread는 한 요청이 끝난 뒤 재사용될 수 있으므로 이전 값이 자동으로 업무적으로 초기화된다고 가정하면 안 된다.

작업 시작에서 값을 설정했다면 종료 경계에서 반드시 정리해야 한다. Exception이 발생해도 cleanup이 수행되도록 `try/finally`나 context manager로 lifetime을 묶는다. 한 worker가 A 요청을 끝내고 B 요청을 처리할 때 A의 user ID가 남아 있으면 isolation failure가 된다. 따라서 thread-local의 scope는 thread 자체가 아니라 application이 정의한 request/job scope와 함께 관리해야 한다.

또한 thread-local은 async task isolation을 해결하지 않는다. Event loop thread 하나에서 여러 coroutine이 번갈아 실행되면 같은 thread-local storage를 공유할 수 있다. 작업이 실제로 어느 execution unit에서 interleave되는지 이해하고 mechanism을 선택한다.

---

## CHAPTER 03 · context variable은 logical task 문맥에 값을 바인딩한다

Python의 context variable mechanism은 async task가 suspend/resume되어도 논리적 execution context에 맞는 binding을 유지하도록 설계되어 있다. Event loop thread 하나에서 여러 task가 interleaving되어도 각 task는 자신의 context snapshot을 기반으로 값을 읽을 수 있다. 이것이 thread identity에만 묶인 storage와 다른 핵심이다.

Request handler 시작에서 correlation ID를 context variable에 설정하면 깊은 logging helper가 parameter 없이 해당 ID를 읽을 수 있다. 값을 설정하는 operation과 object 내부 mutation을 구분해야 한다. Context마다 서로 다른 binding을 가져도 그 binding이 동일한 mutable dict를 가리키면 object state 자체는 여전히 공유될 수 있다. 따라서 context에는 작은 immutable ID나 metadata를 두는 편이 안전하다.

Context variable을 사용했다고 dependency가 사라지는 것도 아니다. Domain service가 현재 사용자에 따라 가격이나 권한을 판단한다면 그 user를 explicit parameter로 받는 편이 계약이 명확하다. Context-local은 실행 환경의 metadata와 domain input 사이의 경계를 유지할 때 강하다.

---

## CHAPTER 04 · propagation은 새 task·thread·process로 문맥이 어떻게 이어지는지 정한다

Context의 중요한 계약은 값을 저장하는 방식보다 새로운 실행 단위로 얼마나 전파되는가다. Async child task를 만들 때 부모의 context가 복사될 수 있지만 executor thread나 별도 process, remote worker로 넘어갈 때 같은 규칙이 자동 적용된다고 가정하면 안 된다. 실행 모델이 바뀌는 경계마다 propagation policy를 확인한다.

Background task가 요청과 논리적으로 같은 작업이라면 trace ID를 이어받는 것이 유용할 수 있다. 반대로 요청 종료 후 수시간 살아 있는 job이 user session context까지 계속 보유하면 lifetime과 보안 의미가 달라진다. 필요한 metadata만 message field로 serialize하고 short-lived credential은 별도 authorization protocol로 다룬다.

Propagation은 편의 기능이 아니라 causality model이다. 어느 작업이 어느 request에서 파생됐는지 연결하는 대신 민감한 정보가 불필요하게 downstream으로 번지지 않게 해야 한다. Context field마다 전파 범위를 명시하면 distributed system으로 확장할 때도 같은 원리를 사용할 수 있다.

---

## CHAPTER 05 · context-local은 숨은 의존성을 만들기 때문에 domain 입력을 대체하지 않는다

함수 body에서 `current_tenant()`를 호출하면 signature에는 tenant가 없지만 결과는 tenant에 의존한다. 호출자는 같은 argument로도 context에 따라 다른 결과를 받을 수 있고 unit test는 매번 숨은 environment를 구성해야 한다. 이것은 global variable보다 안전한 isolation을 제공하더라도 dependency visibility 문제는 남는다.

Domain rule에 필요한 값은 boundary에서 context를 읽어 explicit typed input으로 변환한 뒤 service에 전달할 수 있다. 예를 들어 web middleware가 authenticated user ID를 context에 넣고 request handler가 이를 꺼내 `calculate_price(user_id, items)`에 넘긴다. Core function은 context mechanism을 몰라도 되고 다른 환경에서 재사용하기 쉽다.

반면 log formatter나 tracing instrumentation처럼 application 전체의 cross-cutting concern은 context를 직접 읽는 편이 parameter pollution을 줄인다. 기준은 “여러 곳에서 필요하다”가 아니라 **이 값이 business contract인가 execution context의 속성인가**다.

---

## CHAPTER 06 · logging context는 event마다 correlation field를 자동으로 결합할 수 있다

모든 함수에 `request_id`, `trace_id`, `job_id`를 넘기면 핵심 interface가 observability metadata로 가득 찰 수 있다. Request boundary에서 context를 설정하고 structured logger가 각 record를 만들 때 해당 값을 자동 결합하면 로그를 동일 사건으로 묶을 수 있다. Startup이나 scheduler처럼 request context가 없는 실행도 있으므로 field absence는 정상 상태로 처리한다.

Logging context는 mutable dict 하나를 계속 수정하기보다 immutable field set 또는 scope별 binding으로 관리하는 편이 race와 stale field를 줄인다. Nested operation이 `operation_id`를 잠시 추가했다가 끝난 뒤 이전 context를 복원해야 한다면 set/reset token이나 context manager를 사용한다.

민감정보를 context에 넣으면 모든 log record로 복제될 위험이 있다. Access token, password, raw personal data는 넣지 않고 안전한 correlation identifier만 남긴다. Context propagation과 logging policy는 privacy boundary와 함께 검토한다.

---

## CHAPTER 07 · tracing context는 서비스 경계를 건널 때 protocol data가 된다

한 process 안에서는 context variable이 trace/span ID를 보관할 수 있지만 HTTP, message queue, subprocess로 경계를 넘으면 memory binding은 사라진다. Trace context를 header나 message metadata로 encode하고 수신 측이 검증한 뒤 새로운 local context로 복원해야 한다. 이 시점부터 context는 명시적인 wire data다.

모든 내부 metadata를 그대로 전달하지 않는다. Standard trace identifiers처럼 interoperability가 필요한 값과 application-specific business metadata를 분리한다. 외부 trust boundary에서 들어오는 trace header를 무조건 신뢰하면 log injection이나 unbounded baggage 문제가 생길 수 있으므로 size와 format을 제한한다.

Distributed trace에서 parent-child 관계를 유지하면 end-to-end latency와 실패 경로를 연결할 수 있다. 그러나 tracing context가 authorization 근거가 되어서는 안 된다. Observability identity와 security identity는 목적이 다르다.

---

## CHAPTER 08 · executor와 blocking bridge는 context가 끊기는 대표 경계다

Async application이 blocking function을 thread executor에 넘기면 coroutine task와 worker thread 사이에 execution model이 바뀐다. Context variable의 자동 propagation 여부를 library와 Python version contract에서 확인하고, 필요하다면 현재 context를 명시적으로 복사해 worker call에 적용한다. 반대로 독립 background operation이라면 context를 의도적으로 끊을 수 있다.

Thread pool worker는 여러 요청에서 재사용되므로 worker 내부에서 thread-local을 별도로 설정한다면 cleanup까지 책임져야 한다. Async context를 thread-local로 수동 복사한 뒤 reset하지 않으면 다음 job에 stale metadata가 남을 수 있다. Bridge layer가 set→call→reset을 하나의 lexical scope로 관리해야 한다.

Process pool은 memory 자체가 분리되므로 일반 in-memory context가 자동 공유되지 않는다. Worker가 필요한 job ID와 tenant 정보는 serialized argument나 message schema에 포함한다. Execution boundary마다 어떤 context가 자동인지 explicit인지 구분한다.

---

## CHAPTER 09 · isolation test는 두 문맥을 실제로 interleave해 섞이지 않는지 확인한다

단일 request test가 통과해도 context isolation이 올바른지 알 수 없다. 두 async task가 서로 다른 ID를 설정하고 여러 `await` 지점에서 interleave된 뒤에도 각자 자신의 값을 보는지 검증한다. Thread pool에서는 같은 worker가 연속 job을 처리한 뒤 이전 값이 남지 않는지 확인한다.

Exception path도 별도 failure class다. Context를 set한 뒤 handler가 예외를 던져도 `finally`에서 reset되어 다음 request가 오염되지 않아야 한다. Cleanup 줄을 제거하는 mutation이나 강제 exception을 넣었을 때 test가 실패해야 한다. 단순 happy path logging test와 다른 검증이다.

Security-sensitive tenant context라면 A와 B의 값이 섞이지 않는 negative assertion도 둔다. “B가 자신의 값을 보았다”뿐 아니라 “A의 값은 절대 보지 않았다”를 확인한다. Context mechanism을 도입한 만큼 isolation을 실제 실행으로 증명한다.

---

## CHAPTER 10 · dynamic scope contract는 scope·propagation·cleanup 세 축으로 정리한다

Context-local 값을 도입할 때 첫째, 어느 execution scope에서 유효한지 정한다. Request, task, thread, job 중 무엇인가. 둘째, 새로운 task·thread·process·remote call로 얼마나 전파되는지 정한다. 셋째, scope가 끝날 때 누가 이전 binding을 복원하거나 제거하는지 정한다. 이 세 규칙이 없으면 global state보다 세련된 형태의 숨은 coupling이 된다.

Context는 lexical parameter와 달리 호출 stack 바깥에서 동적으로 제공되므로 사용 위치를 제한할 가치가 있다. Infrastructure adapter가 context를 읽고 core domain에는 explicit input을 전달하면 dynamic scope의 편리함과 명시적 계약을 함께 유지할 수 있다.

Context-local programming의 핵심은 **전역 상태를 더 영리하게 숨기는 것이 아니라 논리적 실행 문맥의 lifetime과 전파 범위를 실행 모델에 맞게 고정하는 것**이다. 그 계약이 분명하면 logging·tracing·request metadata를 동시에 처리해도 서로 다른 요청의 state를 안전하게 분리할 수 있다.