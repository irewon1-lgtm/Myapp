# PART 40 · MRO·super·mixin — 상속 계층의 실제 호출 순서를 추적하기

상속 코드는 class 수가 많아서 어려운 것이 아니라 **어떤 method가 선택되고 다음 호출이 어디로 이어지는지**가 눈에 보이지 않을 때 어려워진다. Python은 method resolution order(MRO)로 attribute 검색 순서를 정하고, `super()`는 단순한 “부모 호출”이 아니라 그 MRO에서 다음 구현으로 협력적 호출을 이어간다. 이 실행 모델을 알면 multiple inheritance와 mixin을 암기하지 않고 실제 lookup 순서로 읽을 수 있다.

---

## CHAPTER 01 · method lookup은 class 선언 한 줄이 아니라 MRO 전체를 따른다

`obj.run()`을 실행하면 Python은 instance와 class hierarchy의 정해진 lookup 규칙을 사용해 `run` attribute를 찾는다. 같은 이름의 method가 여러 base class에 있어도 임의로 선택하지 않는다. Class가 가진 MRO가 어느 구현을 먼저 볼지 결정한다. 복잡한 hierarchy를 읽을 때 선언의 왼쪽·오른쪽만 눈으로 추측하지 말고 실제 MRO를 확인하는 이유다.

Diamond inheritance처럼 같은 ancestor가 여러 경로로 나타나는 구조에서도 MRO는 한 번의 일관된 선형 순서를 만든다. 이 순서가 서로 모순되어 계산될 수 없는 hierarchy는 class 생성 단계에서 거부될 수 있다. 즉 상속 graph는 단순히 “부모 목록”이 아니라 Python이 일관된 lookup order를 만들 수 있어야 하는 구조다.

Attribute lookup과 method call도 분리해서 본다. 먼저 어느 descriptor/function을 찾는지가 정해지고, 그다음 instance binding을 거쳐 bound method가 호출된다. MRO는 call stack 전체를 말하는 것이 아니라 **이름을 찾는 우선순위**다. 이 차이를 알면 descriptor, property, override가 섞인 코드도 단계적으로 추적할 수 있다.

---

## CHAPTER 02 · `super()`는 특정 부모 class가 아니라 MRO의 다음 지점을 뜻한다

`super().save()`를 “부모의 save 호출”이라고만 외우면 multiple inheritance에서 잘못된 예측을 한다. `super()`는 현재 class와 instance의 MRO 문맥을 이용해 현재 구현 다음에 오는 compatible implementation을 찾는다. 따라서 같은 mixin code라도 어떤 hierarchy에 들어가느냐에 따라 다음 대상이 달라질 수 있다.

이 성질은 cooperative multiple inheritance를 가능하게 한다. 각 class가 자신의 작은 책임을 수행한 뒤 `super()`로 다음 구현에 제어를 넘기면 diamond 구조에서도 공통 ancestor가 중복 호출되지 않는 chain을 만들 수 있다. 반대로 `BaseClass.method(self)`처럼 특정 base를 직접 호출하면 MRO chain을 건너뛰어 다른 mixin의 behavior가 누락되거나 동일 ancestor가 두 번 실행될 수 있다.

`super()`는 호출 순서를 자동으로 올바르게 만드는 마법이 아니다. Chain에 참여하는 모든 class가 같은 협력 규칙을 지켜야 한다. 다음 구현이 존재하는지, signature가 호환되는지, 각 class가 정확히 한 번 `super()`를 호출하는지까지 hierarchy의 계약으로 관리한다.

---

## CHAPTER 03 · cooperative method는 argument 전달 규칙까지 공유해야 한다

여러 class가 같은 method chain에 참여하면 각 구현이 자신에게 필요한 argument를 소비하고 나머지를 다음 `super()`로 전달해야 할 수 있다. Constructor chain에서 이 문제가 특히 잘 드러난다. Base마다 서로 다른 positional parameter 순서를 요구하면 mixin 조합 순서가 바뀌는 순간 호출이 깨질 수 있다.

Keyword 중심 protocol을 사용하면 각 layer가 자신의 이름을 꺼내고 나머지 keyword를 다음 구현으로 넘기는 구조를 만들 수 있다. 하지만 무제한 `**kwargs`가 모든 오타를 조용히 삼키게 만들면 또 다른 문제가 생긴다. 최종 base에서는 예상하지 못한 argument를 거부하고, 어떤 key가 어느 layer의 책임인지 명확히 한다.

협력적 hierarchy가 실제 요구사항이 아니라면 억지로 이런 protocol을 만들 필요가 없다. 두 component가 서로 다른 configuration과 lifecycle을 가진다면 composition으로 각각의 object를 field에 두는 편이 더 단순하다. `super()` chain은 코드 재사용을 위한 요령이 아니라 **공유된 호출 계약을 여러 class가 함께 구현할 때** 의미가 있다.

---

## CHAPTER 04 · mixin은 완전한 entity보다 좁은 capability를 제공할 때 강하다

Mixin은 독립적으로 생성되는 domain object라기보다 다른 class에 제한된 behavior를 더하는 데 적합하다. Serialization helper, audit formatting, retry metadata처럼 작은 capability가 대표적이다. Mixin이 database connection, global configuration, network client까지 직접 소유하기 시작하면 실제로는 별도 service responsibility를 숨긴 복잡한 base class가 된다.

좋은 mixin은 host class에게 요구하는 interface가 작다. 예를 들어 `AuditMixin`이 `self.id`와 `self.clock`을 필요로 한다면 그 전제를 protocol이나 documentation으로 표현한다. 그렇지 않으면 특정 class와 조합했을 때 runtime 깊숙한 곳에서 attribute error가 발생해 원인 추적이 어려워진다.

여러 mixin이 동일 method를 override하면 선언 순서와 MRO가 최종 behavior의 일부가 된다. Registration order처럼 우연한 순서에 의존하지 않고, 실제 MRO를 test와 introspection으로 확인한다. 서로 순서에 민감한 mixin이 많아지면 composition 또는 explicit pipeline이 더 나은 구조인지 검토한다.

---

## CHAPTER 05 · multiple inheritance는 state 합성보다 protocol 합성에서 덜 위험하다

서로 독립적인 stateful base class 두 개를 상속하면 constructor, resource lifetime, equality, serialization 책임이 충돌하기 쉽다. 두 base가 모두 `close()`, `status`, `config` 같은 이름을 다른 의미로 사용하면 MRO가 기술적으로 하나를 선택해도 domain 의미 충돌은 해결되지 않는다.

반면 작은 stateless mixin이나 protocol implementation을 조합하는 경우에는 shared mutable state가 적어 충돌 가능성이 낮다. Multiple inheritance를 “여러 class의 코드를 공짜로 가져오는 기능”보다 **여러 behavior contract를 한 type이 만족하게 만드는 구조**로 보는 편이 안전하다.

Stateful component 두 개가 필요하다면 composition이 ownership을 더 직접적으로 보여 준다. `self.cache`, `self.transport`처럼 각각의 object를 보유하면 어느 component가 어떤 state를 변경하고 누가 닫아야 하는지 알기 쉽다. 상속을 선택할 때는 실제로 substitutability와 cooperative lookup이 필요한지 먼저 확인한다.

---

## CHAPTER 06 · `classmethod`는 class 자체가 필요한 construction policy를 표현한다

Alternative constructor가 subclass에서도 해당 subclass instance를 만들기를 원한다면 `classmethod`가 class object를 첫 argument로 받아 polymorphic construction을 지원할 수 있다. `from_text`, `from_row` 같은 factory는 raw 표현을 validated value로 바꾼 뒤 `cls(...)`를 호출하는 방식으로 subclass와 협력할 수 있다.

그러나 classmethod 안에 file I/O, network request, database transaction을 모두 넣으면 “객체 하나 만들기”가 느리고 실패가 많은 operation으로 바뀐다. 외부 adapter가 raw data를 준비하고 class factory는 validation과 object construction에 집중하게 하면 lifetime과 오류 경계가 더 선명하다.

Class-level registry를 classmethod로 수정할 때는 공유 mutable state라는 사실도 고려한다. Base class와 subclass가 registry를 공유할지 각자 가져야 하는지 attribute lookup과 assignment 규칙을 확인한다. Constructor convenience와 global state를 같은 abstraction에 섞지 않는다.

---

## CHAPTER 07 · override는 signature뿐 아니라 behavior contract를 유지해야 한다

Subclass가 같은 method 이름과 parameter를 제공한다고 해서 base type과 완전히 호환되는 것은 아니다. Base method가 양수 amount 전체를 허용하는데 subclass가 100 이상만 허용하면 precondition을 강화한 셈이고, caller가 base contract를 믿고 50을 전달했을 때 깨진다. 반대로 성공 후 보장을 약화해도 substitutability가 무너진다.

Exception type, idempotency, side effect도 behavior contract의 일부다. Base interface가 `save()`를 여러 번 호출해도 같은 결과를 약속했는데 subclass가 호출할 때마다 중복 record를 만든다면 signature는 같아도 의미는 다르다. Static type checker는 일부 shape mismatch를 찾지만 이런 semantic contract를 자동으로 증명하지 못한다.

Base contract test를 여러 subclass implementation에 반복 적용하면 대체 가능성을 실제 behavior로 검증할 수 있다. 상속 설계는 code reuse 양보다 “caller가 base에 대해 알고 있던 사실이 subclass에서도 유지되는가”로 평가한다.

---

## CHAPTER 08 · MRO 문제는 runtime introspection으로 추측을 증거로 바꾼다

복잡한 hierarchy에서는 `Class.__mro__`와 관련 introspection을 사용해 실제 lookup 순서를 확인할 수 있다. Source diagram과 runtime 결과가 다르면 runtime이 실제 behavior의 증거다. Method가 어느 class dictionary에 정의되어 있는지와 bound method가 어떤 instance를 보유하는지도 함께 본다.

Decorator와 descriptor가 결합되면 단순 text search만으로 최종 callable을 찾기 어려울 수 있다. 먼저 `type(obj)`를 확인하고 MRO를 본 뒤 각 class namespace에서 이름이 어떻게 정의되어 있는지 추적한다. Dynamic monkey patching을 사용하는 system이라면 실행 시점에 class state가 source와 다를 수도 있다.

Introspection 결과를 상시 log할 필요는 없지만 plugin conflict, framework extension, test double 문제를 재현할 때 강한 diagnostic이 된다. 디버깅의 목표는 hierarchy를 머릿속으로 맞히는 것이 아니라 **lookup 순서를 실제 객체에서 확인하는 것**이다.

---

## CHAPTER 09 · hierarchy가 깊어질수록 변화 이유가 다른 층을 composition으로 평탄화한다

BaseA→BaseB→FeatureMixin→Concrete처럼 층이 길어지면 작은 behavior 하나를 이해하기 위해 여러 file을 오가야 한다. 깊은 상속 자체가 자동으로 잘못은 아니지만 각 layer가 서로 다른 변화 이유를 가진다면 명시적 dependency로 분리할 후보다. 정책, 저장소, formatter, transport는 특히 교체 가능한 협력 object로 두기 쉽다.

리팩터링은 hierarchy 전체를 한 번에 없애기보다 가장 불안정한 override chain부터 시작한다. 특정 base method가 여러 subclass에서 서로 다른 외부 service를 호출한다면 service object를 parameter/field로 주입하고 base contract를 단순화할 수 있다. Behavior test를 먼저 고정하면 구조를 바꿔도 외부 semantics를 유지했는지 확인할 수 있다.

Test가 subclass의 protected/private method를 많이 mock해야 한다면 inheritance coupling이 지나치게 깊다는 신호일 수 있다. Public behavior 중심으로 검증하고 내부 collaboration을 explicit object로 바꾸면 test도 implementation hierarchy에서 자유로워진다.

---

## CHAPTER 10 · 상속 코드를 읽는 순서는 `MRO → contract → state ownership → super chain`이다

복잡한 class를 만나면 먼저 실제 MRO를 확인한다. 그다음 각 override가 공유해야 하는 method contract를 읽고, 어느 class가 어떤 state와 resource를 소유하는지 표시한다. 마지막으로 `super()` chain이 필요한 implementation을 한 번씩 통과하며 cleanup과 error semantics를 유지하는지 추적한다.

이 순서로 보면 “진짜 부모가 누구인가” 같은 모호한 질문보다 runtime behavior를 설명할 수 있다. MRO와 descriptor binding은 언어의 기계적 규칙이고, substitutability와 ownership은 설계 규칙이다. 둘을 섞지 않으면 multiple inheritance도 작은 단계로 분석할 수 있다.

상속을 잘 쓰는 기준은 hierarchy가 영리해 보이는가가 아니다. **호출 순서와 상태 책임을 source contract와 runtime evidence로 예측할 수 있고, 새 subclass나 mixin이 추가돼도 기존 caller의 가정이 유지되는가**가 기준이다.