# PART 63 · Generator control protocol — `send`·`throw`·`close`로 suspension point를 양방향으로 제어하기

일반 generator를 `for`로만 사용하면 producer가 값을 바깥으로 내보내는 단방향 sequence처럼 보인다. Python generator protocol은 더 강하다. Consumer는 다음 값을 요청하는 것뿐 아니라 suspended `yield` expression 안으로 값을 보내고, exception을 주입하고, 종료를 요청할 수 있다. 이 기능은 현대 `async/await` 이전의 coroutine-style abstraction과 연결되며, 지금도 `yield from`과 cleanup semantics를 이해하는 데 중요하다. 핵심은 **generator가 단순 iterator가 아니라 중단된 frame과 양방향 control channel을 함께 가진 state machine**이라는 점이다.

---

## CHAPTER 01 · generator는 `yield` 지점마다 frame을 보존한 실행 상태다

Generator function을 호출하면 body 전체가 즉시 실행되지 않고 generator object가 만들어진다. 처음 `next()`를 호출하면 첫 `yield`까지 진행하고 값을 내보낸 뒤 local variable, instruction position, exception state를 보존한 채 멈춘다. 다음 resume에서는 그 지점 뒤에서 계속 실행한다.

이 suspend/resume 모델은 iterator가 단순히 “다음 값 계산 함수”라는 설명보다 강하다. Generator 안에는 실제 call frame과 control state가 살아 있으며 consumer가 언제 resume할지 결정한다. 따라서 generator object를 장기간 보관하면 local variable이 참조하는 큰 object와 resource도 함께 살아 있을 수 있다.

Suspension point 사이에서 외부 state가 바뀔 수 있다는 점도 중요하다. Generator가 yield 직전에 읽은 mutable object를 다음 resume에서도 같다고 가정하면 caller가 그 사이 object를 수정해 결과가 달라질 수 있다. Snapshot semantics가 필요하면 yield 전에 필요한 값을 복사한다.

---

## CHAPTER 02 · `send(value)`는 suspended `yield` expression의 결과로 값을 주입한다

`next(gen)`은 개념적으로 `gen.send(None)`과 비슷한 resume를 수행한다. Generator가 `received = yield output`에서 멈춰 있다면 다음 `send(x)`가 resume될 때 `yield output` expression의 결과가 `x`가 되어 `received`에 binding될 수 있다. 즉 generator가 값을 밖으로 보내면서 이후 consumer input도 받을 수 있다.

이 기능으로 running average, stateful parser, pipeline stage처럼 내부 state를 유지하는 receiver를 만들 수 있다. 하지만 처음 시작되지 않은 generator에 non-None 값을 바로 send할 수 없는 등 initialization protocol이 존재한다. Caller가 반드시 priming을 알아야 하는 API는 사용성이 복잡해질 수 있어 wrapper/factory로 시작 절차를 감출 수 있다.

현대 code에서는 async coroutine과 explicit object method가 더 읽기 쉬운 경우가 많다. `send`를 사용할 이유는 state machine을 generator suspension과 자연스럽게 표현할 때다. 단순 callback 전달을 위해 양방향 generator를 선택하면 control flow가 오히려 숨는다.

---

## CHAPTER 03 · `throw(exc)`는 다음 resume 지점에서 generator 내부에 exception을 발생시킨다

Consumer가 generator에 exception을 주입하면 suspended `yield`가 그 exception을 발생시킨 것처럼 처리할 수 있다. Generator 내부의 `try/except`가 이를 잡아 recovery value를 yield하거나 cleanup 후 다시 전파할 수 있다. 이는 일반 function call에서는 보기 어려운 역방향 error channel이다.

Exception을 잡고 계속 yield하려면 어떤 failure가 recoverable한지 명확해야 한다. 모든 exception을 catch해 stream을 계속 진행하면 cancellation이나 programming error까지 삼킬 수 있다. 예상된 protocol exception만 처리하고 나머지는 caller에 전파한다.

`throw`는 test와 orchestration에서도 generator가 실패 신호를 어떻게 처리하는지 검증하는 도구가 될 수 있다. 다만 external caller가 generator internals에 너무 많은 exception type을 알아야 한다면 abstraction boundary가 약한 것이다. Public protocol에는 안정된 failure vocabulary를 둔다.

---

## CHAPTER 04 · `close()`는 종료 요청을 generator 내부 cleanup path로 전달한다

Generator 소비를 중간에 그만둘 때 내부 file, lock, transaction이 남아 있다면 cleanup이 필요하다. `close()`는 generator에 종료 signal을 전달해 `finally` block이 실행될 수 있게 한다. Generator가 종료 요청을 무시하고 계속 값을 yield하려 하면 protocol 오류가 발생할 수 있다.

Consumer가 모든 generator를 명시적으로 close해야 하는 API는 사용하기 어렵다. Generator가 resource를 소유한다면 context manager와 결합하거나 higher-level function이 consumption lifecycle을 끝까지 책임지게 만들 수 있다. Resource cleanup을 garbage collection timing에만 맡기지 않는다.

`for` loop가 정상 종료되는 경우와 `break`로 조기 중단하는 경우도 구분한다. 조기 중단 뒤 generator object가 아직 다른 reference에 남아 있다면 자동으로 즉시 close되지 않을 수 있다. Long-lived generator와 scarce resource를 함께 사용한다면 explicit scope가 중요하다.

---

## CHAPTER 05 · `yield from`은 값 전달뿐 아니라 send·throw·close protocol도 위임한다

단순 nested iteration에서 `yield from child`는 child의 값을 바깥으로 그대로 전달하는 문법처럼 보인다. 더 깊게는 outer generator가 받은 `send`, `throw`, `close` control을 delegate generator에 연결하는 protocol을 구현한다. 그래서 수동 loop보다 양방향 generator composition을 정확하게 처리할 수 있다.

Delegation 중 child가 return하면 그 return value를 outer generator가 받을 수 있다. 값 stream과 final result가 서로 다른 channel로 존재하는 셈이다. Parser combinator나 state machine에서 child computation의 결과를 상위 단계가 이용할 수 있다.

이 protocol은 강력하지만 modern async code에서는 `await`와 async iterator가 더 명시적이다. `yield from` 내부 semantics를 이해하는 목적은 generator composition과 legacy coroutine code, Python 실행 모델을 정확히 읽기 위함이지 모든 control flow를 generator로 바꾸기 위한 것이 아니다.

---

## CHAPTER 06 · generator `return value`는 StopIteration의 종료 payload와 연결된다

일반 function의 return과 generator의 return은 의미가 다르다. Generator에서 `return value`는 더 이상 yield할 값이 없음을 나타내면서 종료 결과를 전달할 수 있고, iteration protocol에서는 StopIteration과 연결된다. 일반 `for` loop는 이 종료 payload를 사용하지 않지만 `yield from` delegation은 이를 받을 수 있다.

StopIteration을 generator body에서 직접 다루는 semantics는 Python version과 language rule의 영향을 받으므로 iterator protocol 종료를 임의 exception control로 남용하지 않는다. Custom iterator에서 정상 종료와 실제 error를 구분하고 generator 내부 programming mistake가 조용한 종료로 바뀌지 않게 language contract를 따른다.

Stream items와 final aggregate result가 모두 필요한 API라면 generator return payload보다 명시적 result object나 별도 method가 더 읽기 쉬울 수도 있다. Caller가 어떤 consumption mechanism을 쓰는지에 따라 interface를 선택한다.

---

## CHAPTER 07 · generator-based coroutine은 async/await의 역사적 기반이지만 실행 모델을 혼합하지 않는다

`send`와 `yield from`을 이용하면 cooperative coroutine을 구성할 수 있었고 Python의 native coroutine/`async` 문법은 이런 아이디어를 더 명시적인 타입과 syntax로 발전시켰다. 둘은 suspend/resume와 delegation이라는 공통 개념을 가지지만 일반 generator와 native coroutine은 같은 object type이나 동일 API가 아니다.

Legacy library에서 generator-based coroutine을 만나면 yield된 값이 data item인지 scheduler에게 넘기는 awaitable-like token인지 구분해야 한다. 현대 application에서 새 비동기 control flow를 작성할 때는 `async def`/`await`를 우선해 intent를 드러낸다.

Generator protocol을 이해하면 async task가 await에서 frame을 보존하고 다시 resume되는 구조를 비교해 볼 수 있다. 하지만 thread safety, cancellation, task scheduling은 별도 async runtime contract이므로 generator 지식만으로 일반화하지 않는다.

---

## CHAPTER 08 · generator control contract는 ownership·입력 channel·종료 path를 함께 정의한다

양방향 generator를 public API로 쓸 때는 caller가 언제 first `next`를 해야 하는지, 어떤 값을 `send`할 수 있는지, 어떤 exception을 `throw`할 수 있는지, 조기 종료 시 `close`가 필요한지 문서화해야 한다. 단순 `Iterator[T]`보다 훨씬 큰 protocol이다.

복잡한 protocol이라면 class의 `send_event`, `finish`, `cancel` method가 더 명시적일 수 있다. Generator를 선택하는 이유는 suspension state와 sequential protocol이 문제 구조와 자연스럽게 맞기 때문이어야 한다.

Generator control의 핵심은 **`yield`를 값 반환의 특수 문법으로 보는 데서 벗어나 보존된 frame을 consumer가 resume·입력·예외·종료 신호로 제어하는 작은 state machine으로 이해하는 것**이다.