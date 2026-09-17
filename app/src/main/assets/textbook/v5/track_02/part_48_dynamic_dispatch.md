# PART 48 · Dynamic dispatch — 타입과 capability에 따라 behavior를 확장하기

프로그램이 다루는 데이터 variant가 늘어나면 `if isinstance(...)` 분기가 여러 파일에 퍼질 수 있다. 객체 method, single dispatch, registry, visitor, pattern matching은 모두 **현재 값에 맞는 behavior를 선택하는 문제**를 서로 다른 위치에서 해결한다. 중요한 것은 가장 화려한 mechanism을 고르는 것이 아니라 새 타입과 새 operation 중 무엇이 더 자주 추가되는지, 선택 규칙이 어디에 있어야 변경이 국소적인지를 판단하는 것이다.

---

## CHAPTER 01 · dispatch는 입력 상태에서 실행할 behavior를 선택하는 규칙이다

`render(value)`, `serialize(value)`, `handle(event)` 같은 함수가 여러 타입을 받으면 내부에서 어떤 구현을 사용할지 결정해야 한다. 가장 직접적인 방식은 `if/elif`로 타입을 검사하는 것이지만 variant가 늘어날수록 중앙 함수가 모든 타입을 알아야 한다. 반대로 각 object가 `render()` method를 가지면 선택 규칙은 method lookup으로 이동한다.

Dispatch의 위치는 architecture에 영향을 준다. Domain object가 스스로 자연스럽게 소유하는 behavior라면 method가 적합할 수 있고, 외부 format 변환이나 diagnostic 출력처럼 object core에 넣고 싶지 않은 operation은 외부 dispatcher가 적합할 수 있다. “객체지향 vs 함수형”의 취향 문제가 아니라 change axis를 기준으로 정한다.

Dynamic dispatch를 설계할 때는 default behavior가 있는지, unknown type을 어떻게 처리하는지, subclass가 base implementation을 자동 상속해도 되는지를 함께 정한다. 선택 실패도 public contract의 일부다.

---

## CHAPTER 02 · single dispatch는 첫 argument 타입에 따라 함수 구현을 선택한다

Python의 single-dispatch abstraction을 사용하면 하나의 generic function 이름 아래에 타입별 implementation을 등록할 수 있다. Caller는 `serialize(value)`처럼 동일 API를 사용하고 dispatcher가 value의 runtime type과 등록 정보를 보고 적절한 implementation을 고른다. 중앙의 긴 type switch를 registry 기반 selection으로 바꾸는 셈이다.

```python
@singledispatch
def encode(value):
    raise TypeError(type(value))

@encode.register
def _(value: Order):
    ...
```

이 구조의 장점은 새 타입 지원을 별도 module에서 등록할 수 있다는 것이다. Generic function 자체를 수정하지 않아도 extension이 가능하다. 하지만 등록이 import side effect로 이루어지면 해당 module이 실제로 import되었는지에 따라 behavior가 달라질 수 있으므로 plugin discovery와 startup validation이 중요해진다.

Single dispatch는 이름 그대로 일반적으로 첫 dispatch argument의 type을 본다. 두 argument 조합에 따라 behavior가 달라지는 문제를 억지로 nested dispatcher로 만들면 복잡해질 수 있다. Dispatch dimension이 무엇인지 먼저 모델링한다.

---

## CHAPTER 03 · 등록된 타입이 여러 후보와 맞으면 가장 구체적인 관계를 따라 선택된다

Subclass instance는 base class에 등록된 implementation과도 호환될 수 있다. Dispatcher는 type hierarchy에서 더 구체적인 등록을 찾는 방식으로 동작할 수 있다. 따라서 `Animal`용 handler가 있고 `Dog` 전용 handler가 추가되면 Dog는 더 구체적인 구현을 사용해야 한다.

Multiple inheritance에서는 어떤 등록이 더 구체적인지 MRO와 dispatch algorithm을 함께 봐야 한다. 서로 독립된 두 protocol-like base가 모두 후보가 되는 구조는 ambiguity가 생길 수 있다. 실제 registry와 dispatch result를 introspection으로 확인해 추측하지 않는다.

Default implementation은 unknown subtype을 안전하게 처리할 수 있어야 한다. Serialization처럼 모르는 타입을 조용히 문자열로 바꾸면 data loss가 생길 수 있으므로 명시적 failure가 더 적합할 수 있다. Logging formatter처럼 fallback `repr`이 합리적인 경우도 있다. Domain risk에 맞춰 default를 정한다.

---

## CHAPTER 04 · method dispatch는 behavior를 object 쪽에 두고 encapsulation을 강화한다

`payment.authorize()`처럼 호출하면 actual payment subtype의 method implementation이 선택된다. Caller는 CardPayment와 BankTransfer를 구체적으로 분기하지 않아도 된다. Variant마다 필요한 state와 behavior가 밀접하다면 method dispatch가 자연스럽다.

Method의 장점은 object invariant와 behavior가 가까이 있다는 것이다. 반면 외부 operation이 늘어날 때 각 class에 method를 계속 추가해야 할 수 있다. Domain model이 persistence serializer, HTML renderer, debug formatter까지 모두 가지면 infrastructure concern이 core object에 침투한다.

Base method contract를 subclass가 유지해야 substitutability가 성립한다. Signature만 같고 exception, side effect, postcondition이 다르면 dynamic dispatch가 호출자에게 예측 불가능한 behavior를 준다. Polymorphism은 선택 mechanism이면서 공통 semantic contract다.

---

## CHAPTER 05 · registry는 extension point를 제공하지만 등록 lifetime과 충돌 정책이 필요하다

명령 이름→handler, format 이름→parser, type→serializer 같은 mapping registry를 사용하면 core가 extension 구현을 직접 import하지 않고도 behavior를 찾을 수 있다. Plugin architecture에서 흔한 패턴이다. 하지만 registry 역시 mutable global state가 될 수 있으므로 누가 언제 등록하고 중복 key를 어떻게 처리하는지 규칙이 필요하다.

같은 key에 두 handler가 등록되면 last-wins로 조용히 덮을지, startup error로 거부할지 선택한다. Security-sensitive command나 serializer에서는 duplicate registration을 실패시키는 편이 안전할 수 있다. Test isolation을 위해 registry reset 또는 explicit instance를 사용할 수도 있다.

Import 시 decorator가 자동 등록하는 방식은 간결하지만 import order가 behavior를 결정할 수 있다. Explicit startup registration은 장황하지만 실제 extension set을 한곳에서 볼 수 있다. Application 규모와 plugin 개방성에 따라 trade-off를 선택한다.

---

## CHAPTER 06 · visitor는 variant 집합은 안정적이고 operation이 자주 늘어날 때 유용할 수 있다

AST node 종류가 비교적 안정적이고 formatter, evaluator, type checker, optimizer처럼 operation이 계속 추가된다면 각 node에 모든 method를 넣는 것보다 visitor가 operation별 코드를 모을 수 있다. 반대로 node type이 자주 추가되면 모든 visitor가 새 case를 추가해야 해 비용이 커진다.

Visitor는 double-dispatch 성격을 구현하기 위해 object type과 visitor operation을 함께 사용한다. Python에서는 pattern matching이나 single dispatch가 더 간결한 경우도 있으므로 전통적인 패턴을 기계적으로 적용하지 않는다.

선택 기준은 change matrix다. “새 타입 추가”와 “새 operation 추가” 중 어느 축이 더 자주 바뀌는지를 보면 data-centered method와 operation-centered visitor 사이의 비용이 보인다. Architecture pattern은 변화 방향을 최적화하는 도구다.

---

## CHAPTER 07 · pattern matching과 dispatch는 상태를 누가 아는지에서 차이가 난다

Pattern matching은 하나의 function이 가능한 variant 집합을 알고 각 branch에서 처리한다. Data transformation, compiler pass처럼 모든 variant를 한곳에서 exhaustively 처리해야 할 때 읽기 좋다. Dynamic dispatch는 각 type 또는 registry가 behavior 선택에 참여해 중앙 function이 구체 타입을 몰라도 된다.

Closed-world variant는 matching에 잘 맞고 open extension ecosystem은 registry/dispatch가 더 자연스러울 수 있다. 하지만 open-world에서는 exhaustive handling이 어렵고 plugin conflict 가능성이 생긴다. Closed-world에서는 새 variant 추가 때 compiler/type checker가 누락 branch를 알려 줄 수 있다.

둘을 섞을 수도 있다. External plugin type을 registry로 domain adapter에 연결한 뒤 내부 closed union으로 변환하고 그 이후는 pattern matching으로 처리할 수 있다. Boundary마다 적합한 extension model을 선택한다.

---

## CHAPTER 08 · dispatch failure는 unknown type과 broken implementation을 구분한다

Handler를 찾지 못한 실패와 찾은 handler가 실행 중 exception을 낸 실패는 의미가 다르다. 전자는 registry/configuration 문제거나 unsupported type이고, 후자는 implementation 또는 input data 문제다. 둘을 같은 `TypeError` 한 줄로 합치면 운영에서 원인 분류가 어렵다.

Plugin handler가 failure를 던졌을 때 core가 다른 handler로 fallback할지 즉시 실패할지도 policy다. Serializer에서 실패한 구현 뒤 default 문자열 변환으로 계속하면 data corruption을 숨길 수 있다. Best-effort analytics plugin은 실패를 격리하고 나머지를 계속할 수 있다.

Dispatch error에는 selected handler identity와 input type, registry version을 안전하게 남기면 dynamic system을 조사하기 쉽다. Handler argument 전체를 log해 secret을 노출하지 않도록 한다.

---

## CHAPTER 09 · dispatch test는 등록 여부뿐 아니라 specificity와 extension isolation을 검증한다

Generic function에 Dog handler가 등록됐다는 test 하나로는 충분하지 않다. Base Animal, Dog subclass, unknown type을 각각 넣어 어떤 implementation이 선택되는지 확인한다. Multiple inheritance가 있다면 실제 MRO와 registry precedence를 test fixture로 고정한다.

Plugin test에서는 module load 전후 registry 변화와 duplicate registration policy를 검증한다. Test suite 순서에 따라 registry state가 누적되지 않도록 explicit fixture lifecycle을 사용한다. Global mutable registry가 test isolation을 깨뜨리는지 별도 failure class로 본다.

Contract test를 여러 handler implementation에 적용해 return type, exception semantics, side effect invariant를 공유하게 할 수 있다. “등록됨”과 “대체 가능함”은 다른 품질 조건이다.

---

## CHAPTER 10 · extension contract는 dispatch mechanism보다 public capability를 먼저 고정한다

확장 가능한 system을 설계할 때 먼저 plugin/handler가 받아야 할 input, 반환해야 할 result, 허용 side effect, timeout/cancellation, error vocabulary를 정의한다. 그 뒤 method, single dispatch, registry, entry point 중 적합한 연결 mechanism을 선택한다. Mechanism이 contract를 대신하지 않는다.

Core는 extension이 어떤 내부 global을 수정해야 동작하는 구조보다 작은 capability interface를 제공하는 편이 안전하다. Extension version과 compatibility를 startup에서 확인하고 unsupported plugin을 명시적으로 거부한다.

Dynamic dispatch의 핵심은 **조건문을 없애는 것이 아니라 새로운 behavior가 추가될 때 수정 범위를 제한하면서도 어떤 구현이 왜 선택됐는지 추적 가능한 extension boundary를 만드는 것**이다.