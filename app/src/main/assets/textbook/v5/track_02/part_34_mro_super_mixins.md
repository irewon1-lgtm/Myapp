# PART 34 · MRO·super·mixin — 상속 계층의 실제 호출 순서를 추적하기

상속을 사용한 코드가 어려워지는 지점은 class 이름이 많아서가 아니라 **어떤 method가 실제로 선택되고 다음 호출이 어디로 이어지는지**가 눈에 보이지 않을 때다. Python은 method resolution order(MRO)를 통해 attribute 검색 순서를 정하고, `super()`는 단순히 “부모 class”가 아니라 그 MRO의 다음 구현으로 협력적 호출을 이어간다. 이 모델을 이해하면 multiple inheritance와 mixin을 추측 없이 읽을 수 있다.

---

## CHAPTER 01 · method lookup은 현재 class 하나가 아니라 MRO 전체를 따라간다

`obj.run()`을 호출하면 먼저 instance와 class hierarchy에서 `run`이라는 attribute를 정해진 순서로 찾는다. 같은 이름의 method가 여러 base class에 존재해도 임의로 하나가 선택되는 것이 아니라 class의 MRO가 우선순위를 결정한다. 복잡한 hierarchy를 디버깅할 때 class 선언의 왼쪽·오른쪽만 보고 추측하지 말고 실제 MRO를 확인한다.

Diamond inheritance처럼 동일 ancestor가 여러 경로로 나타나는 구조에서도 Python은 일관된 선형 순서를 만든다. 이 순서는 각 class의 local precedence를 보존하면서 ancestor가 중복 실행되지 않도록 구성된다. 서로 모순되는 상속 순서를 요구하면 class 생성 단계에서 hierarchy 자체가 거부될 수 있다. 즉 상속 선언도 lookup contract를 만족해야 한다.

Attribute lookup과 method call을 분리해서 보면 동작이 선명해진다. 먼저 어느 function 또는 descriptor를 찾았는지가 결정되고, 그 다음 instance binding과 실제 호출이 일어난다. MRO는 call stack이 아니라 **이름을 찾는 우선순위**다. 어떤 method가 호출됐는지 이해하려면 source 위치와 MRO를 함께 본다.

---

## CHAPTER 02 · `super()`는 특정 부모 이름이 아니라 MRO의 다음 지점을 가리킨다

`super().save()`를 “내 부모의 save를 호출한다”고만 외우면 multiple inheritance에서 잘못된 예측을 한다. `super()`는 현재 class와 instance의 MRO 문맥을 사용해 **현재 구현 다음에 오는 호환 구현**을 찾는다. 그래서 동일 mixin이 다른 hierarchy에 들어가면 다음 대상도 달라질 수 있다.

이 성질은 cooperative multiple inheritance를 가능하게 한다. 각 class가 자신의 작업을 수행하고 `super()`로 다음 구현에 제어를 넘기면 diamond 구조에서도 공통 ancestor가 한 번만 지나가는 chain을 만들 수 있다. 반대로 `BaseClass.save(self)`처럼 특정 base를 직접 호출하면 MRO chain을 건너뛰어 다른 mixin의 동작을 누락하거나 공통 base를 두 번 실행할 수 있다.

따라서 `super()`는 코드 중복을 줄이는 문법이 아니라 hierarchy가 합의한 호출 protocol의 일부다. 다음 구현이 실제로 존재하고 signature와 semantics가 호환된다는 전제가 있어야 한다. 단일 상속에서도 이 모델을 사용하면 나중에 hierarchy가 확장될 때 직접 base 호출보다 유연하다.

---

## CHAPTER 03 · cooperative method는 argument 전달 규칙까지 협력해야 한다

여러 class가 동일 method chain에 참여한다면 각 구현이 자신에게 필요한 argument만 소비하고 나머지를 다음 `super()`로 전달하는 방식이 필요할 수 있다. Constructor chain에서 특히 자주 보이는 문제다. Base A는 `name`, Mixin B는 `audit_id`, Base C는 `timeout`을 기대하는데 positional argument 순서에 강하게 의존하면 class 조합을 바꾸는 순간 호출이 깨진다.

Keyword 기반 protocol과 `**kwargs` 전달은 이런 결합을 줄일 수 있지만 무제한 forwarding이 모든 문제를 해결하지는 않는다. 오타 난 keyword가 여러 layer를 통과한 뒤 전혀 다른 곳에서 실패하면 원인을 찾기 어렵다. 각 layer가 어떤 key를 소비하는지 명시하고 최종 base가 예상하지 못한 argument를 거부하도록 만들어 계약 오류를 일찍 드러낼 수 있다.

Cooperative hierarchy를 설계할 때는 method signature뿐 아니라 호출 횟수와 side effect도 맞춰야 한다. 한 class가 `super()`를 호출하지 않으면 뒤의 layer가 사라지고, 두 번 호출하면 후속 effect가 중복된다. 상속 chain 전체가 하나의 pipeline이라는 관점이 필요하다.

---

## CHAPTER 04 · mixin은 독립 capability 조각이지 작은 base class의 다른 이름이 아니다

Mixin은 완전한 domain object를 직접 생성하기보다 다른 class에 제한된 capability를 더하는 용도로 적합하다. Serialization helper, audit formatting, comparison behavior처럼 좁은 기능을 제공하고 자체 lifecycle과 business identity를 크게 가지지 않는 편이 조합하기 쉽다. Mixin이 database connection과 network client, global configuration까지 소유하기 시작하면 실제로는 독립 service를 상속으로 숨긴 구조가 된다.

좋은 mixin은 host class에 요구하는 interface가 작고 분명하다. 예를 들어 `AuditMixin`이 `self.id`와 `self.clock`을 사용한다면 그 전제를 Protocol이나 문서, type hint로 표현한다. 그렇지 않으면 특정 class와 조합했을 때 runtime에서 늦게 attribute error가 발생한다.

Mixin 두 개가 같은 method를 override하면 declaration 순서가 behavior를 바꿀 수 있다. 이때 단순히 “왼쪽이 먼저”라고 외우지 말고 실제 MRO와 각 구현의 `super()` 호출 여부를 확인한다. 조합 순서가 중요한 순간 class declaration 자체가 public semantics가 된다.

---

## CHAPTER 05 · multiple inheritance는 데이터 소유권보다 protocol 조합에 더 적합하다

서로 독립된 stateful base class 두 개를 합치면 constructor, lifecycle, equality, serialization 책임이 충돌하기 쉽다. 두 base가 모두 `status`, `close`, `save` 같은 이름을 각자 다른 의미로 사용하면 MRO가 하나를 선택한다고 domain 충돌이 해결되는 것이 아니다. 이름 lookup 문제와 모델의 의미 문제는 별개다.

반면 작은 protocol implementation이나 stateless mixin의 조합은 충돌 가능성이 낮다. Iterator capability, ordering helper, validation hook처럼 동일 object identity에 자연스럽게 붙는 behavior는 multiple inheritance로 표현할 수 있다. 그래도 각 capability가 공유하는 state와 호출 순서를 점검해야 한다.

독립 component 두 개가 필요하다면 composition으로 각각의 객체를 field에 두는 방식이 ownership과 lifetime을 더 명확하게 만들 수 있다. Inheritance를 선택할 때는 “코드를 재사용할 수 있는가”보다 base interface의 대체 가능성과 cooperative MRO가 실제로 필요한지 묻는다.

---

## CHAPTER 06 · `classmethod`와 alternative constructor도 상속 문맥을 가진다

Alternative constructor가 subclass에서도 해당 subclass instance를 만들기를 원한다면 `classmethod`가 class object를 첫 argument로 받아 polymorphic construction을 지원할 수 있다. `from_text`, `from_row`, `from_config` 같은 factory가 hard-coded base class 이름 대신 `cls(...)`를 사용하면 subclass에서도 같은 생성 contract를 확장할 수 있다.

하지만 parsing과 I/O를 constructor 계층에 과도하게 넣으면 class hierarchy와 외부 dependency가 결합된다. File을 열고 JSON을 읽고 network까지 호출하는 classmethod보다 외부 adapter가 raw data를 준비하고 class factory는 검증된 값에서 object를 만드는 구조가 더 명확할 수 있다.

Class-level registry를 수정하는 classmethod도 shared mutable state다. Subclass가 registry를 독립적으로 가져야 하는지 base와 공유해야 하는지 attribute lookup 규칙에 따라 결과가 달라진다. Class attribute inheritance를 instance field처럼 생각하지 않는다.

---

## CHAPTER 07 · method override는 signature보다 behavior contract를 유지해야 한다

Subclass가 같은 method 이름과 parameter를 제공해도 precondition을 더 강하게 만들거나 postcondition을 약하게 만들면 base type을 기대하는 호출자가 깨질 수 있다. Base `save()`가 같은 입력에서 idempotent하다는 계약인데 subclass가 호출마다 외부 side effect를 추가하면 interface 의미가 달라진다. Exception 종류와 resource lifetime도 계약 일부다.

Static type checker는 일부 signature mismatch를 발견할 수 있지만 semantic substitutability 전체를 증명하지는 못한다. Base contract test를 여러 concrete subclass에 공통 적용하면 같은 입력·출력·실패 규칙을 지키는지 검증할 수 있다.

Override가 base 내부 구현 순서에 의존하면 base refactor가 subclass를 조용히 깨뜨릴 수 있다. Extension point가 필요한 method와 private implementation detail을 구분하고, hook의 호출 시점과 허용 effect를 명시한다.

---

## CHAPTER 08 · MRO 문제는 introspection으로 실제 순서를 증거화한다

복잡한 hierarchy에서 `Class.__mro__`나 관련 introspection을 사용하면 실제 lookup 순서를 바로 확인할 수 있다. 손으로 그린 diagram과 runtime 결과가 다르면 runtime이 정답이다. Method가 어느 class dictionary에서 정의되었는지와 bound method가 어느 instance를 잡고 있는지도 함께 본다.

Decorator와 descriptor가 결합되면 source search만으로 실제 callable을 찾기 어려울 수 있다. `type(obj)`, instance dictionary, class dictionary, MRO 순으로 범위를 넓혀 조사하면 lookup 경로를 재구성할 수 있다. Framework가 dynamic class generation이나 plugin을 사용한다면 실행 시점의 class 자체가 source에 보이는 class와 다를 수도 있다.

MRO 전체를 상시 log할 필요는 없지만 plugin conflict나 framework extension 문제의 diagnostic evidence로는 강하다. 추측 대신 실행 구조를 출력해 원인을 좁힌다.

---

## CHAPTER 09 · hierarchy가 깊어질수록 composition으로 평탄화할 지점을 찾는다

BaseA→BaseB→FeatureMixin→Concrete처럼 여러 층이 생기면 작은 behavior 하나의 실제 구현을 찾기 위해 여러 파일을 오가야 한다. 상속 깊이는 그 자체로 오류는 아니지만 변경 이유가 서로 다르다면 composition으로 분리할 후보가 된다. Storage policy, formatter, clock처럼 independently replaceable한 기능은 별도 collaborator로 빼기 쉽다.

Inheritance는 “is-a” 관계와 shared contract가 안정적일 때 유지하고, 정책과 infrastructure처럼 변동성이 큰 기능은 composition으로 이동할 수 있다. 리팩터링할 때 hierarchy 전체를 한 번에 없애기보다 가장 잦게 override되고 충돌하는 chain부터 explicit dependency로 바꾸는 편이 위험이 낮다.

테스트가 subclass의 private method를 많이 patch해야만 동작한다면 hierarchy 내부 결합이 강하다는 신호다. Public behavior 중심으로 검증할 수 있도록 책임을 재배치하면 구현 변경에 강해진다.

---

## CHAPTER 10 · 상속 코드를 읽는 순서는 `MRO → contract → state ownership → super chain`이다

복잡한 class를 만나면 먼저 실제 MRO를 확인하고 각 override가 공유해야 하는 method contract를 본다. 다음으로 어느 class가 어떤 state를 소유하는지 확인하고 마지막으로 `super()` chain이 필요한 구현을 정확히 한 번씩 통과하는지 추적한다. 이 순서는 “부모가 누구인가”라는 모호한 질문보다 실제 runtime behavior를 설명한다.

MRO와 cooperative call은 Python object model의 기계적 규칙이고, substitutability와 ownership은 software design 규칙이다. 둘을 함께 보지 않으면 lookup은 맞지만 의미가 깨진 hierarchy가 생길 수 있다.

상속을 잘 쓰는 기준은 hierarchy가 영리해 보이는가가 아니라 **호출 순서와 상태 책임을 소스와 runtime evidence로 예측할 수 있는가**다. 그 기준을 만족하지 못하면 composition이나 작은 protocol로 구조를 단순화한다.