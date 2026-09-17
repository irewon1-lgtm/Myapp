# PART 31 · MRO·super·mixin — 상속 계층의 실제 호출 순서를 추적하기

상속을 사용한 코드가 어려워지는 지점은 class 이름이 많아져서가 아니라 **어떤 method가 실제로 선택되고 다음 호출이 어디로 이어지는지**가 눈에 보이지 않을 때다. Python은 method resolution order(MRO)를 통해 attribute 검색 순서를 정하고, `super()`는 단순히 “부모 class”가 아니라 그 MRO의 다음 구현으로 협력적 호출을 이어간다. 이 모델을 이해하면 multiple inheritance와 mixin을 추측 없이 읽을 수 있다.

---

## CHAPTER 01 · method lookup은 현재 class 하나가 아니라 MRO 전체를 따라간다

`obj.run()`을 호출하면 instance에 직접 저장된 attribute와 class hierarchy를 정해진 순서로 검색한다. 같은 이름의 method가 여러 base class에 있어도 임의로 하나가 선택되는 것이 아니라 class의 MRO가 우선순위를 결정한다. Debugging할 때 class 선언의 왼쪽·오른쪽만 눈으로 추측하지 말고 실제 MRO를 확인한다.

MRO는 diamond inheritance처럼 같은 ancestor가 여러 경로로 나타나는 구조에서도 한 번의 일관된 선형 순서를 만들려고 한다. 이 순서가 만들어질 수 없는 모순된 hierarchy는 class 생성 단계에서 거부될 수 있다. 상속 관계 자체가 lookup contract를 만족해야 하는 셈이다.

Attribute lookup과 method call을 분리해서 보면 이해가 쉽다. 먼저 어느 function/descriptor를 찾았는지 결정되고, 그 뒤 instance binding과 호출이 일어난다. MRO는 호출 stack이 아니라 **이름을 찾는 우선순위**다.

---

## CHAPTER 02 · `super()`는 특정 부모 이름이 아니라 MRO의 다음 지점을 가리킨다

`super().save()`를 “내 부모의 save를 호출한다”고만 외우면 multiple inheritance에서 틀린 예측을 한다. `super()`는 현재 class와 instance의 MRO 문맥을 사용해 **현재 구현 다음에 오는 호환 구현**을 찾는다. 그래서 같은 mixin이 다른 hierarchy에 들어가도 다음 대상이 달라질 수 있다.

이 성질은 cooperative multiple inheritance를 가능하게 한다. 각 class가 자신의 작업을 수행하고 `super()`로 다음 구현에 제어를 넘기면 diamond 구조에서도 공통 ancestor가 한 번만 호출되는 chain을 만들 수 있다. 반대로 어떤 class가 base class 이름을 직접 호출하면 MRO chain을 건너뛰어 중복 실행이나 누락을 만들 수 있다.

`super()`를 사용할 때는 다음 method가 존재하고 compatible signature를 가진다는 계약이 필요하다. 상속 계층의 한 class만 독자적인 parameter를 요구하면 chain이 깨질 수 있다.

---

## CHAPTER 03 · cooperative method는 argument 전달 규칙까지 협력해야 한다

여러 class가 동일 method chain에 참여한다면 각 구현이 자신에게 필요한 argument만 소비하고 나머지를 다음 `super()`로 전달하는 규칙을 만들 수 있다. 하지만 무제한 `**kwargs`로 모든 오류를 숨기면 잘못된 이름도 조용히 지나갈 수 있다. 어떤 keyword가 어느 layer의 책임인지 문서화하고 최종 base에서 예상하지 못한 argument를 거부한다.

Constructor chain에서 이 문제가 특히 자주 보인다. `__init__`마다 서로 다른 positional signature를 가지면 mixin 조합 순서를 바꾸는 순간 호출이 깨질 수 있다. Cooperative hierarchy를 의도했다면 keyword 중심의 안정된 protocol을 설계한다.

상속 계층이 실제로 cooperative하지 않다면 억지로 `super()` chain을 만들지 않는다. Composition으로 dependency를 명시하는 편이 단순할 수 있다.

---

## CHAPTER 04 · mixin은 독립 기능 조각이지 작은 base class의 다른 이름이 아니다

Mixin은 완전한 domain object를 직접 생성하기보다 다른 class에 제한된 capability를 더하는 용도로 사용된다. Serialization helper, timestamp formatting, logging hook처럼 좁은 behavior를 제공할 수 있다. Mixin이 database connection과 global configuration까지 소유하기 시작하면 결합이 커진다.

좋은 mixin은 요구하는 host interface가 작고 명확하다. 예를 들어 `AuditMixin`이 `self.id`와 `self.clock`을 요구한다면 그 전제를 타입 Protocol이나 문서로 표현한다. 그렇지 않으면 특정 class에 섞었을 때 runtime에서 늦게 attribute error가 발생한다.

Mixin 순서가 behavior를 바꾼다면 class declaration 순서가 public semantics가 된다. 여러 mixin이 같은 method를 override하는 구조는 review에서 실제 MRO와 각 `super()` 호출 여부를 확인해야 한다.

---

## CHAPTER 05 · multiple inheritance는 데이터 소유권보다 protocol 조합에 더 적합하다

서로 독립된 stateful base class 두 개를 합치면 constructor, lifecycle, equality, serialization 책임이 충돌하기 쉽다. 반면 작은 protocol implementation이나 stateless mixin 조합은 충돌 가능성이 낮다. Multiple inheritance를 코드 재사용 도구보다 **behavior contract 합성 도구**로 보는 편이 안전하다.

두 base가 같은 attribute 이름을 서로 다른 의미로 사용하면 MRO가 하나를 선택해도 domain 의미 충돌은 해결되지 않는다. 이름 충돌을 기술적으로 피하는 것과 모델이 일관적인 것은 다른 문제다.

Stateful component 두 개가 필요하다면 composition으로 각각의 객체를 field에 두면 ownership과 lifetime이 더 분명해질 수 있다. Inheritance를 선택할 때 대체 가능성과 MRO cooperation이 실제로 필요한지 확인한다.

---

## CHAPTER 06 · `classmethod`는 polymorphic constructor와 class-level policy에 사용될 수 있다

Alternative constructor가 subclass에서도 해당 subclass instance를 만들기를 원한다면 `classmethod`가 class object를 첫 argument로 받아 polymorphic construction을 지원할 수 있다. `from_text`, `from_row`처럼 외부 representation을 변환하는 factory에 사용할 수 있다.

하지만 parsing과 I/O가 복잡해지면 classmethod 하나에 file/network 접근까지 넣지 않는다. 외부 adapter가 raw data를 준비하고 class factory는 검증된 값에서 object를 만드는 구조가 더 분명할 수 있다.

Class-level registry를 classmethod로 수정할 때는 공유 mutable state라는 사실을 잊지 않는다. Subclass가 registry를 별도로 가져야 하는지 base와 공유해야 하는지 attribute lookup 규칙을 확인한다.

---

## CHAPTER 07 · method override는 signature보다 behavior contract를 유지해야 한다

Subclass가 같은 method 이름과 parameter를 제공해도 precondition을 더 강하게 만들거나 postcondition을 약하게 만들면 base type을 기대하는 호출자가 깨질 수 있다. Override의 품질은 문법적 일치가 아니라 substitutability로 평가한다.

Base method가 idempotent하다고 약속했는데 subclass가 호출마다 외부 상태를 누적 변경한다면 interface 의미가 달라진다. Exception type과 side effect도 contract 일부다.

Static type checker는 일부 signature mismatch를 찾을 수 있지만 semantic contract 전체를 증명하지는 못한다. Base contract test를 subclass implementation에도 적용하는 방식으로 대체 가능성을 검증할 수 있다.

---

## CHAPTER 08 · MRO 문제는 introspection으로 실제 순서를 증거화한다

복잡한 hierarchy에서 `Class.__mro__` 또는 관련 introspection을 사용해 실제 lookup 순서를 확인할 수 있다. 추측한 diagram과 runtime 결과가 다르면 runtime이 정답이다. Method가 어느 class에서 정의되었는지와 bound method의 owner도 조사한다.

Decorator와 descriptor가 결합되면 단순 source search만으로 실제 callable을 찾기 어려울 수 있다. `type(obj)`, class dictionary, MRO를 단계적으로 확인한다. Dynamic monkey patching까지 사용한다면 실행 시점 state snapshot이 필요하다.

Debug log에 MRO 전체를 상시 출력할 필요는 없지만 plugin conflict나 framework extension 문제를 재현할 때 유용한 diagnostic이 된다.

---

## CHAPTER 09 · hierarchy가 깊어질수록 composition으로 평탄화할 지점을 찾는다

BaseA→BaseB→FeatureMixin→Concrete처럼 여러 층이 생기면 작은 behavior 하나를 찾기 위해 여러 파일을 오가야 한다. 상속 깊이는 그 자체로 오류는 아니지만 변경 이유가 서로 다르다면 composition으로 분리할 후보가 된다.

Inheritance는 “is-a” 관계와 shared contract가 안정적일 때 유지하고, 정책·저장소·formatter처럼 교체 가능한 협력 객체는 composition으로 빼낼 수 있다. 리팩터링할 때 한 번에 hierarchy 전체를 없애기보다 가장 불안정한 override chain부터 explicit dependency로 바꾼다.

테스트가 subclass의 private implementation을 많이 mock한다면 hierarchy가 지나치게 결합됐다는 신호일 수 있다. Public behavior 중심으로 검증할 수 있는 구조가 더 견고하다.

---

## CHAPTER 10 · 상속 코드를 읽는 순서는 `MRO → contract → state ownership → super chain`이다

복잡한 class를 만나면 먼저 실제 MRO를 확인하고, 각 override가 공유해야 하는 method contract를 본다. 다음으로 어느 class가 어떤 state를 소유하는지 확인하고 마지막으로 `super()` chain이 모든 필요한 구현을 한 번씩 통과하는지 추적한다.

이 순서로 보면 “부모가 누구인가”라는 모호한 질문보다 실제 runtime behavior를 설명할 수 있다. MRO와 cooperative call은 Python object model의 기계적 규칙이고, substitutability와 ownership은 설계 규칙이다.

상속을 잘 쓰는 기준은 hierarchy가 영리해 보이는가가 아니라 **호출 순서와 상태 책임을 소스와 runtime evidence로 예측할 수 있는가**다.