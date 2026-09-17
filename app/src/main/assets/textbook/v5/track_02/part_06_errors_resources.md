# PART 06 · 오류와 resource lifetime — 실패를 숨기지 않고 상태를 안전하게 정리하기

프로그램이 정상 입력만 처리한다면 구조는 단순해 보인다. 실제 환경에서는 파일이 없고, 입력 형식이 틀리고, 네트워크가 끊기고, disk가 가득 차고, 호출자가 취소하며, 내부 invariant가 깨질 수 있다. 오류 처리는 이런 사건 뒤에 메시지를 출력하는 단계가 아니라 **어떤 실패를 어느 경계가 책임지고, 이미 획득한 resource와 변경된 상태를 어떻게 정리할지 정의하는 설계**다.

---

## CHAPTER 01 · 오류를 원인과 복구 가능성으로 분류한다

모든 실패를 하나의 `error`로 뭉치면 호출자는 다음 행동을 결정할 수 없다. 사용자가 잘못된 값을 입력한 경우, 파일이 일시적으로 잠긴 경우, 인증 권한이 없는 경우, 프로그램 내부 invariant가 깨진 경우는 서로 다른 대응이 필요하다. 실패 분류는 exception class 이름을 정하기 전에 의미부터 나눈다.

입력 validation 실패는 같은 입력으로 재시도해도 보통 결과가 바뀌지 않으므로 사용자나 호출자가 값을 수정해야 한다. 일시적인 network timeout은 재시도가 가능할 수 있지만 원격 작업이 이미 성공했는지 불확실할 수 있다. permission error는 retry가 아니라 권한 변경이 필요하다. 내부 invariant 위반은 정상 업무 실패로 숨기면 결함이 오래 남을 수 있다.

실패를 설계할 때는 최소한 `원인 범주`, `호출자가 취할 행동`, `상태 변경 여부`, `노출 가능한 메시지`, `관측에 필요한 진단 정보`를 구분한다. 사용자에게 보여 줄 문구와 개발자가 분석할 stack trace를 같은 문자열 하나에 넣지 않는다. 보안상 내부 경로나 secret이 외부 메시지에 노출되지 않도록 경계를 둔다.

이 분류가 선명하면 함수 signature와 exception hierarchy도 자연스러워진다. “무엇이든 실패하면 None”보다 존재하지 않음, 형식 오류, 외부 장애를 구별할 수 있는 인터페이스가 강하다. 오류 모델은 정상 데이터 모델과 같은 수준의 API 설계다.

---

## CHAPTER 02 · exception은 비정상 제어 흐름을 호출 스택 위로 전달한다

Python에서 예외가 발생하면 현재 문장의 정상 실행은 중단되고 적절한 handler를 찾으며 호출 stack을 거슬러 올라간다. 이 과정에서 중간 함수가 오류를 직접 처리하지 않아도 상위 경계가 처리할 수 있다. 반환값마다 오류 코드를 확인하는 방식과 다른 제어 흐름을 제공하는 것이다.

`raise`는 단순히 프로그램을 멈추는 명령이 아니다. 현재 함수가 계약을 정상 완료할 수 없다는 사실을 호출자에게 전달한다. Handler가 없다면 최상위까지 전파되어 traceback과 함께 종료될 수 있다. 따라서 exception path도 함수의 가능한 출력 경로 중 하나로 봐야 한다.

예외를 잡는 위치는 복구 책임이 있는 위치여야 한다. 낮은 함수가 모든 예외를 잡고 빈 값으로 바꾸면 상위 코드는 실제 실패를 모른 채 잘못된 데이터로 계속 실행할 수 있다. 반대로 최상위 경계까지 전부 전파하면 사용자에게 의미 있는 복구를 제공하지 못할 수 있다. 오류의 의미를 이해하고 행동을 결정할 수 있는 가장 가까운 경계에서 처리한다.

Exception이 stack을 건너뛰기 때문에 중간에 획득한 resource 정리가 중요해진다. 함수 마지막 줄에 `close()`를 적어 둔 것만으로는 그 이전에 예외가 발생했을 때 실행되지 않을 수 있다. 그래서 `finally`와 context manager가 resource safety의 핵심 도구가 된다.

---

## CHAPTER 03 · `try/except`는 잡을 수 있는 범위를 좁게 유지한다

너무 큰 코드 블록을 `try`로 감싸고 넓은 exception을 잡으면 실제로 어느 연산이 실패했는지 구분하기 어렵다. parsing 오류를 기대해 `ValueError`를 처리하려 했는데 같은 블록의 다른 함수가 우연히 `ValueError`를 던질 수도 있다. 예상하는 실패가 발생할 수 있는 최소 연산 주위에 handler를 두면 원인 해석이 정확해진다.

```python
try:
    quantity = int(text)
except ValueError as exc:
    raise InvalidQuantity(text) from exc
```

이 코드는 문자열→정수 변환 실패를 도메인 오류로 번역한다. `from exc`를 통해 원래 cause 관계를 보존하면 상위에서는 의미 있는 타입을 다루면서 디버깅 시 원인 stack도 추적할 수 있다. 단순히 새 메시지만 만들고 원래 예외를 버리면 진단 정보가 약해질 수 있다.

`except Exception:`처럼 넓게 잡는 패턴이 필요한 경계도 있다. 요청 하나의 실패가 서버 전체를 죽이지 않게 하는 top-level request boundary, worker loop의 task boundary 같은 곳이다. 그러나 이 경우에도 기록 후 무조건 정상값을 반환하는 것이 아니라 요청 실패를 명확히 표시하고 상태를 안전하게 정리해야 한다.

프로그램 내부 깊은 함수에서는 예상 가능한 구체적 exception을 처리하고 나머지는 전파시키는 편이 결함을 숨기지 않는다. Catch 범위는 “무엇을 잡을 수 있는가”가 아니라 “무엇을 여기서 의미 있게 처리할 수 있는가”로 결정한다.

---

## CHAPTER 04 · `else`와 `finally`는 성공 후 처리와 무조건 정리를 분리한다

`try/except`만 알면 정상 경로 후 수행할 코드와 오류 여부와 관계없이 반드시 수행할 코드를 모두 같은 블록에 넣기 쉽다. Python의 `else`는 try body가 exception 없이 끝났을 때 실행할 수 있고, `finally`는 정상 종료·예외·return 등 다양한 경로 뒤에도 정리 작업을 수행할 수 있게 한다.

`else`를 사용하면 exception을 잡아야 하는 범위를 줄일 수 있다. 예를 들어 파일을 여는 단계만 `try`에 두고, 성공 후 처리 로직을 `else`로 이동하면 처리 로직에서 발생한 다른 exception을 실수로 파일-open 오류 handler가 잡는 일을 피할 수 있다. 구조가 장황해지는 경우도 있으므로 의미 분리가 실제로 도움이 될 때 사용한다.

`finally`에서는 resource release, lock 해제, temporary state 복구처럼 반드시 실행되어야 하는 정리를 수행한다. 하지만 `finally` 자체에서 새로운 exception을 발생시키거나 return으로 기존 exception을 덮어쓰는 코드는 진단을 매우 어렵게 만든다. 정리 코드는 가능한 한 단순하고 실패 semantics가 분명해야 한다.

정리 순서도 중요하다. 여러 resource를 획득했다면 일반적으로 의존성의 역순으로 해제해야 할 수 있다. A를 연 뒤 B가 A에 의존한다면 B를 먼저 닫고 A를 닫는 식이다. 이 패턴은 context manager와 `ExitStack` 같은 도구로 체계화할 수 있다.

---

## CHAPTER 05 · context manager는 획득과 해제를 하나의 lexical scope에 묶는다

파일, lock, database transaction, temporary directory 같은 resource는 사용이 끝나면 정리해야 한다. `with` 문은 resource 진입과 종료 protocol을 하나의 블록에 묶어 정상 경로와 exception 경로 모두에서 cleanup이 일어나도록 설계할 수 있게 한다.

```python
with open(path, "r", encoding="utf-8") as f:
    data = f.read()
```

이 구조의 장점은 `f.close()`를 기억해야 하는 줄 수가 줄어드는 것보다 강하다. resource lifetime이 block 범위로 드러나므로 소유권이 명확해진다. 호출자가 file object를 오래 보관하는지, 함수 안에서만 사용하는지 API를 읽는 순간 판단할 수 있다.

사용자 정의 context manager는 transaction boundary에도 활용할 수 있다. `with transaction():` 안에서 모든 작업이 성공하면 commit하고 예외가 나면 rollback하는 규칙을 캡슐화할 수 있다. 이때 rollback 자체가 실패했을 때 원래 exception을 어떻게 보존할지, nested transaction을 지원할지 같은 계약이 필요하다.

Resource는 memory만을 뜻하지 않는다. socket, process handle, DB connection, thread pool, temporary credential도 수명 관리가 필요하다. garbage collection에 최종 정리를 맡기면 종료 시점이 계약에서 멀어질 수 있다. 외부 시스템에 영향을 주는 resource는 명시적 lifetime 관리가 더 안전하다.

---

## CHAPTER 06 · EAFP와 LBYL은 race condition 여부까지 보고 선택한다

Python에서는 흔히 EAFP(Easier to Ask Forgiveness than Permission)와 LBYL(Look Before You Leap)이라는 두 스타일이 비교된다. LBYL은 실행 전에 조건을 확인하고, EAFP는 정상 동작을 시도한 뒤 특정 실패를 처리한다. 둘 중 하나가 모든 상황의 정답은 아니다.

파일 존재 여부를 먼저 `exists()`로 확인한 뒤 `open()`하는 사이에 다른 process가 파일을 지울 수 있다. 이런 check-then-act 사이의 시간 차이 때문에 상태가 바뀌는 race가 생긴다. 실제 `open()`을 수행하고 `FileNotFoundError`를 처리하면 operation과 실패 판단을 같은 경계에서 다룰 수 있다.

반대로 외부 API 호출이 비싸고 입력 validation으로 명백한 잘못을 미리 걸러낼 수 있다면 사전 검사가 의미 있다. 사용자가 입력한 문자열의 형식을 확인하지 않고 매번 원격 요청을 보내 exception을 받는 것은 자원 낭비일 수 있다. EAFP는 validation을 버리라는 규칙이 아니다.

판단 기준은 검사와 실제 동작 사이에서 상태가 변할 수 있는지, 실패가 정상적으로 예상되는지, operation 비용이 얼마나 큰지, 어떤 exception을 정확히 구분할 수 있는지다. 특히 공유 resource에서는 “확인했으니 안전하다”는 가정이 동시성 때문에 깨질 수 있음을 인식한다.

---

## CHAPTER 07 · retry는 오류 처리라기보다 새로운 상태 머신이다

외부 서비스가 일시적으로 실패할 때 재시도는 유효한 전략이지만 모든 exception에 같은 방식으로 적용하면 위험하다. 잘못된 인증 정보, validation 실패, 존재하지 않는 resource는 반복해도 성공하지 않는다. Timeout이나 일시적 overload처럼 회복 가능성이 있는 실패만 정책에 따라 재시도한다.

Retry에는 최대 횟수나 deadline, backoff, jitter, cancellation 같은 상태가 생긴다. 따라서 단순 `while True: try...except...`가 아니라 시도 횟수와 남은 시간, 마지막 오류를 가진 상태 머신으로 보는 편이 정확하다. 여러 client가 동시에 재시도하면 장애 중인 서버에 더 큰 부하를 줄 수 있으므로 지연을 늘리고 timing을 분산하는 전략이 사용된다.

더 큰 문제는 요청이 실패했다는 응답을 받지 못했을 때 실제 작업이 수행되었는지 모를 수 있다는 점이다. 결제 요청을 보낸 뒤 network가 끊겼다면 서버가 결제를 완료했지만 응답만 사라졌을 수 있다. 같은 요청을 다시 보내면 중복 결제가 발생할 수 있다. 이런 operation에는 idempotency key나 조회를 통한 상태 확인 같은 protocol이 필요하다.

Retry 정책은 함수 내부 편의 코드로 숨기지 않고 호출자와 운영 정책이 이해할 수 있게 만든다. 총 timeout budget 안에서 개별 시도가 얼마를 사용할지, 마지막 실패를 어떤 exception으로 전달할지도 계약의 일부다.

---

## CHAPTER 08 · 로그는 오류를 처리하지 않으며 traceback과 context를 보존해야 한다

Exception을 잡아 `print("error")`하고 계속 실행하는 코드는 실패를 처리한 것이 아니라 증거를 줄인 것일 수 있다. 로그는 관측 수단이고 복구 정책과 별개다. 무엇을 로깅할지 결정할 때는 사건의 의미, request나 job을 식별할 correlation 정보, 원래 exception traceback을 보존한다.

같은 exception을 여러 계층에서 반복 로깅하면 한 사건이 수십 개의 error처럼 보일 수 있다. 보통 실제로 요청을 종료하거나 재시도 정책을 결정하는 boundary에서 한 번 충분한 context와 함께 기록하고, 낮은 계층은 exception에 의미 있는 정보를 추가해 전파하는 방식이 관리하기 쉽다.

민감정보도 고려한다. Password, access token, 주민번호, 전체 결제 정보를 exception message나 로그에 포함하면 디버깅 편의를 위해 보안 사고를 만들 수 있다. 로그용 context는 필요한 식별자와 상태만 남기고 secret이나 개인 데이터는 정책에 맞게 제거한다.

Traceback은 “어디서 실패했는가”뿐 아니라 어떤 호출 경로로 도달했는지 보여 준다. Exception을 새로운 타입으로 번역할 때 cause chain을 보존하면 업무 의미와 기술 원인을 모두 볼 수 있다. 디버깅 가능한 오류 모델은 메시지가 길어서가 아니라 **원인 연결과 실행 context가 유지되기 때문에** 강하다.

---

## CHAPTER 09 · 실패 원자성은 오류 뒤에 남는 상태를 정의한다

함수가 여러 상태를 순차적으로 변경하다가 중간에 실패하면 시스템이 절반만 변경된 상태에 남을 수 있다. 계좌 A에서 돈을 차감한 뒤 B에 입금하기 전에 오류가 난다면 총액 invariant가 깨진다. 오류 처리는 exception을 잡는 것만으로 이 문제를 해결하지 못한다. 상태 변경 자체의 atomicity 설계가 필요하다.

메모리 안에서는 새 값을 먼저 계산하고 validation을 마친 뒤 한 번에 참조를 교체하는 방식으로 부분 변경을 줄일 수 있다. 데이터베이스에서는 transaction을 사용해 여러 변경을 commit/rollback 단위로 묶을 수 있다. 외부 서비스 여러 개를 동시에 다루면 단일 transaction이 없을 수 있어 compensating action이나 saga 같은 더 복잡한 상태 모델이 필요하다.

모든 작업을 완전 원자적으로 만들 수는 없다. 이메일을 이미 발송한 뒤 DB commit이 실패했다고 해서 받은 사람의 메일함에서 메시지를 되돌릴 수는 없다. 이런 irreversible effect는 가능한 한 durable state가 확정된 뒤 수행하거나 outbox처럼 별도 전달 상태를 관리하는 설계를 고려한다.

함수 계약에는 실패 시 “아무 것도 변하지 않는다”, “일부 상태가 남을 수 있고 recovery token을 반환한다”, “재시도하면 동일 결과로 수렴한다” 같은 보장을 명시할 수 있다. 이 정보가 있어야 상위 계층이 안전한 복구를 설계한다.

---

## CHAPTER 10 · 오류 경계는 프로그램 계층마다 다른 언어로 번역된다

낮은 계층의 `FileNotFoundError`를 최종 사용자에게 그대로 보여 주는 것은 기술 정보를 업무 의미로 번역하지 않은 상태다. 반대로 모든 오류를 “처리 중 문제가 발생했습니다” 하나로 바꾸면 개발자는 원인을 찾기 어렵다. 각 계층은 자신이 이해하는 실패를 상위 계층의 의미로 번역하되 원인 chain을 유지해야 한다.

Repository 계층은 database constraint 오류를 `DuplicateUser` 같은 domain exception으로 바꿀 수 있고, service 계층은 이를 회원가입 실패 결과로 해석할 수 있다. HTTP boundary는 domain 결과를 4xx/5xx status와 안전한 response body로 변환한다. UI는 사용자가 취할 행동이 있는 메시지로 표현한다. 같은 실패 사건이 계층을 지나며 표현만 달라지는 것이다.

번역은 무조건 새로운 exception을 만드는 것이 아니다. 하위 exception이 이미 상위 계약에 적합하면 그대로 전파할 수 있다. 반대로 상위 계층이 하위 구현 세부사항에 직접 의존하면 storage 기술을 바꿀 때 오류 처리 코드까지 넓게 바뀔 수 있다. 추상화 경계는 정상 결과뿐 아니라 실패 vocabulary도 소유한다.

최상위 boundary에서는 예상된 업무 실패와 예상하지 못한 결함을 구분한다. 예상된 실패는 안정된 오류 코드와 사용자 메시지로 처리하고, unexpected exception은 correlation ID와 diagnostic context를 기록한 뒤 안전하게 실패시킨다. 오류를 “없애는” 설계가 아니라 **실패가 발생해도 의미와 상태를 잃지 않는 설계**가 목표다.