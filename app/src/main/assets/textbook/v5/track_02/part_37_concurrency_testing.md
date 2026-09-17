# PART 37 · Concurrency testing — race·deadlock·cancellation을 우연이 아니라 조건으로 재현하기

동시성 버그는 특정 실행 순서에서만 나타나기 때문에 정상 요청을 많이 반복하는 것만으로 충분히 검증하기 어렵다. Thread scheduling과 async interleaving을 전적으로 운영체제 우연에 맡기면 실패가 재현되지 않고, debugger를 붙이는 순간 timing이 달라져 증상이 사라질 수도 있다. 강한 검증은 **위험한 순서를 의도적으로 만들고, 진행성·원자성·정리 invariant를 각각 확인하는 것**이다.

---

## CHAPTER 01 · concurrent correctness는 최종 값뿐 아니라 가능한 interleaving 전체에 대한 주장이다

Counter가 최종적으로 100이 나왔다는 test 한 번은 특정 scheduling에서 성공했다는 증거다. 두 worker가 read-modify-write를 겹칠 수 있다면 다른 interleaving에서 update가 사라질 수 있다. 동시성 contract는 “어떤 순서로 섞여도 허용 invariant가 유지되는가”를 묻는다.

모든 instruction interleaving을 실제로 열거할 수는 없으므로 shared state를 읽고 쓰는 synchronization point를 찾아 critical schedule을 구성한다. 두 요청이 같은 old version을 읽은 뒤 둘 다 write하는 lost update sequence처럼 위험한 패턴을 직접 만든다.

Final state 외에도 duplicate side effect, exception, lock release, task leak을 본다. 같은 결과가 나왔어도 API가 두 번 호출되었다면 idempotency contract가 깨졌을 수 있다.

Concurrent test의 첫 단계는 thread 수를 늘리는 것이 아니라 state machine과 invariant를 정의하는 것이다.

---

## CHAPTER 02 · barrier와 event를 사용해 원하는 순서까지 두 실행 흐름을 멈춘다

Race를 재현하기 위해 `sleep(0.01)`을 넣는 방식은 machine load에 따라 달라져 flaky하다. Barrier, Event, Condition 같은 synchronization primitive를 test seam으로 사용하면 두 worker가 정확히 같은 지점에 도달한 뒤 다음 단계를 진행시킬 수 있다.

예를 들어 두 worker가 balance를 읽은 직후 barrier에서 기다리게 하고 둘 다 old value를 읽은 것이 확인된 뒤 write를 허용하면 lost update 가능성을 deterministic하게 검사할 수 있다. Production code에 test-only global hook을 무분별하게 넣기보다 repository fake나 injectable coordination point를 사용한다.

Async code에서는 `asyncio.Event`와 fake awaitable로 동일한 방식의 schedule control을 만들 수 있다. Task A가 lock 획득 직전, task B가 mutation 직전처럼 원하는 state를 맞춘다.

Test synchronization 자체가 production lock behavior를 바꾸지 않는지 주의한다. 검증 seam은 최소한의 관찰/대기만 추가한다.

---

## CHAPTER 03 · race test는 read–check–act 사이에 다른 실행을 삽입해 본다

`if key not in cache: cache[key] = compute()`처럼 check와 action이 분리된 코드는 두 실행 흐름이 동시에 missing을 보고 compute를 두 번 수행할 수 있다. 결과 cache는 하나여도 expensive side effect가 중복되거나 compute가 non-idempotent라면 결함이다.

Test에서는 첫 worker가 check를 끝낸 순간 멈추고 두 번째 worker도 같은 check를 통과시킨 뒤 둘을 계속 진행시킨다. 최종 value뿐 아니라 compute 호출 횟수를 검증한다. 원자적 put-if-absent 또는 lock이 필요하다는 증거가 된다.

Authorization에서도 check 후 object state가 바뀌는 TOCTOU race가 가능하다. 권한 확인과 실제 update가 같은 transaction/condition에 묶여야 할 수 있다.

Race condition test는 일반 random stress보다 훨씬 적은 실행으로 특정 취약 window를 직접 공격한다.

---

## CHAPTER 04 · deadlock test는 timeout만 두는 것이 아니라 lock ordering cycle을 구성한다

Deadlock은 테스트가 영원히 멈추기 때문에 suite 자체를 정지시킬 수 있다. 모든 concurrency test에 적절한 global deadline을 두고 timeout이 발생하면 thread/task stack과 lock state를 진단 정보로 남긴다.

두 lock을 A→B, B→A 순서로 잡는 두 worker가 있다면 barrier를 사용해 각각 첫 lock을 보유한 상태를 만든 뒤 두 번째 lock을 요청하게 하면 cycle을 재현할 수 있다. 이 test는 단순 load test보다 lock ordering defect를 직접 보여 준다.

Fix 후에는 global lock order를 contract로 만들고 같은 schedule에서 progress가 일어나는지 확인한다. 한 worker가 완료되어 lock을 놓고 다른 worker가 이어서 완료해야 한다.

Timeout 통과만으로 deadlock free를 증명하지 않는다. Test가 실제 위험한 ordering에 도달했다는 assertion/trace가 있어야 한다.

---

## CHAPTER 05 · async cancellation test는 모든 await boundary에서 cleanup 가능성을 본다

Coroutine은 여러 await 지점에서 cancellation을 받을 수 있다. File/resource 획득 직후, transaction 시작 후, remote request 완료 전후 등 어느 지점에서 취소되어도 lock과 connection이 release되고 invariant가 유지되어야 한다.

Fake awaitable을 사용해 특정 await에서 task를 멈춘 뒤 cancel을 전달하고 finally/context-manager cleanup을 검증한다. Cancellation exception을 handler가 삼켜 task가 계속 실행되는 bug도 찾을 수 있다.

Remote side effect가 이미 실행됐을 가능성이 있는 지점에서는 local cleanup만으로 충분하지 않다. Idempotency key와 reconciliation state가 남았는지 확인한다.

Cancellation test는 정상 exception test와 다른 failure class다. Timeout이나 client disconnect가 production에서 자주 cancellation으로 표현되므로 별도 검증 가치가 있다.

---

## CHAPTER 06 · thread-safe라는 주장은 operation 하나가 아니라 compound invariant를 기준으로 한다

Library의 individual `get`과 `set`이 thread-safe하더라도 `if get() is None: set(x)` 조합 전체가 atomic한 것은 아니다. Test도 method 하나씩만 호출하면 compound race를 놓친다.

Business operation 단위로 invariant를 정의한다. Inventory가 음수가 되지 않아야 한다면 두 concurrent reserve가 각자 availability를 확인하고 차감하는 전체 sequence를 검증해야 한다. Database atomic update/constraint가 필요한 경우 process-local lock test만으로 충분하지 않다.

Immutable snapshot을 읽는 operation은 synchronization 요구가 적을 수 있다. Writer가 새 snapshot을 한 번에 publish하는 구조라면 reader가 partial mutation을 볼 수 없는지 검사한다.

“Thread-safe collection을 썼다”는 구현 속성보다 application-level operation이 atomic한지 묻는다.

---

## CHAPTER 07 · stress test는 rare schedule을 넓게 탐색하지만 실패 재현 정보를 남겨야 한다

수십~수백 worker가 random operation을 반복하면 특정 interleaving을 우연히 발견할 수 있다. State machine invariant를 매 단계 확인하고 실패 시 random seed, operation sequence, worker ID, timing event를 기록한다.

Stress test를 CI의 필수 deterministic gate로 매번 오래 돌리면 runtime과 flakiness가 커질 수 있다. 빠른 deterministic schedule tests를 기본 gate로 두고 stress/fuzz suite를 별도 주기나 bounded run으로 운영할 수 있다.

발견된 stress failure는 가능한 한 작은 deterministic sequence로 축소한다. “10만 번 돌리면 가끔 실패” 상태로만 남기면 regression 검증이 어렵다.

System load와 core count가 실제 race probability에 영향을 줄 수 있으므로 production-like environment test도 가치가 있지만 correctness proof를 대체하지 않는다.

---

## CHAPTER 08 · model-based concurrency test는 구현 결과를 단순한 reference state와 비교한다

Concurrent queue나 account처럼 operation semantics가 명확하면 단순 sequential model을 reference로 만들 수 있다. 여러 operation의 호출/완료 history를 수집하고 어떤 serial order로 설명 가능한지 검사하면 linearizability 같은 강한 correctness property를 분석할 수 있다.

Reference model은 production 최적화를 그대로 복제하지 않고 명확하고 느린 규칙을 사용한다. 그래야 implementation과 같은 bug를 공유할 가능성이 줄어든다.

History가 너무 크면 가능한 serial ordering 탐색 비용이 커질 수 있어 작은 operation set과 bounded concurrency로 exhaustive하게 확인하고 큰 workload는 sampling한다.

모든 application이 formal linearizability checker를 필요로 하는 것은 아니다. 하지만 “동시 실행 결과가 어떤 단일 순서의 정상 실행과 동등해야 한다”는 사고는 atomic API를 설계하는 데 유용하다.

---

## CHAPTER 09 · concurrency test의 observability는 timing을 바꾸지 않는 최소 증거를 남긴다

많은 `print`와 synchronous logging을 critical section에 넣으면 thread scheduling이 달라져 race가 사라질 수 있다. Test instrumentation은 in-memory event record나 lightweight counter처럼 최소 overhead를 사용하고 종료 후 분석한다.

Event에는 logical step과 worker identity, state version을 기록하면 실제 interleaving을 재구성할 수 있다. Wall-clock timestamp만으로는 해상도와 clock scheduling 때문에 순서를 완전히 설명하지 못할 수 있다.

Timeout failure에서 thread dump와 task stack을 캡처하면 어디서 기다리는지 확인할 수 있다. Lock owner 정보가 library에서 제공되는지도 본다.

진단 기능은 test failure에서만 활성화해 정상 suite 속도를 유지할 수 있다. 관측 자체가 defect probability를 바꾸는 heisenbug 가능성을 계속 인식한다.

---

## CHAPTER 10 · concurrency CLEAN PASS는 race·deadlock·cancellation·leak을 서로 다른 검증으로 본다

동시성 코드를 한 번 load test해 성공했다고 모든 failure class를 PASS 처리하지 않는다. 같은 공유 상태에 대한 deterministic race schedule, lock ordering deadlock schedule, cancellation at await boundary, task/thread/resource leak 확인은 서로 다른 검증이다.

각 PASS는 실제로 해당 조건을 만들었다는 evidence가 있어야 한다. Barrier count, forced cancellation point, timeout stack, post-condition resource count처럼 test가 목표 상황에 도달했음을 기록한다.

Fix가 한 failure를 없애면서 다른 문제를 만들 수 있다. Lock을 추가해 race를 막았지만 contention이나 deadlock이 생길 수 있으므로 correctness와 progress를 함께 본다.

동시성 테스트의 핵심은 **운 좋게 문제가 안 보이는 실행을 반복하는 것이 아니라 위험한 interleaving을 직접 구성하고, 상태 불변식과 진행성, cleanup을 각각 독립적으로 증명 가능한 테스트로 만드는 것**이다.