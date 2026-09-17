# PART 82 · ABC runtime contracts — abstract method·virtual subclass·collections ABC를 구분하기

Python은 duck typing을 강조하지만 runtime interface를 전혀 표현할 수 없는 언어는 아니다. `abc`와 `collections.abc`는 abstract method, virtual subclass, runtime membership test를 제공한다. 중요한 것은 이 도구를 Java식 nominal hierarchy 복제에 쓰는 것이 아니라 **어떤 operation set을 runtime에서 안정적으로 식별해야 하는지**를 기준으로 선택하는 것이다.

---

## CHAPTER 01 · ABC는 상속 계층보다 runtime interface intent를 명시하는 도구다

Abstract Base Class는 공통 operation contract를 이름 붙이고, subclass가 필요한 method를 구현하도록 요구할 수 있다.

```python
from abc import ABC, abstractmethod

class Storage(ABC):
    @abstractmethod
    def load(self, key):
        ...
```

이제 concrete subclass는 abstract method를 충족해야 일반 instance construction이 가능하다. 하지만 ABC가 모든 semantic behavior를 자동 검증하는 것은 아니다. `load()`가 존재해도 timeout, consistency, return type 의미가 틀릴 수 있다.

ABC는 method presence와 일부 default behavior를 구조화하는 도구이고, 실제 contract는 문서·test와 함께 완성된다.

---

## CHAPTER 02 · `abstractmethod`는 구현 강제와 reusable default body를 함께 가질 수 있다

Abstract method가 반드시 body 없는 placeholder일 필요는 없다. 공통 동작을 제공하면서 subclass가 override하도록 요구하는 pattern도 가능하다.

그러나 abstract default body에 복잡한 workflow를 넣으면 subclass가 `super()` 호출 순서를 알아야 하는 hidden protocol이 생긴다. Template Method pattern이 적합한 문제인지 검토한다.

Abstract property, classmethod 등과 결합할 때 decorator order와 framework semantics를 정확히 따른다. 목적은 “추상화가 많아 보이는 구조”가 아니라 invalid concrete implementation을 일찍 막는 것이다.

---

## CHAPTER 03 · virtual subclass는 실제 inheritance 없이 runtime membership을 인정할 수 있다

ABC는 특정 외부 class를 virtual subclass로 등록해 `isinstance`/`issubclass` 관점에서 contract에 참여시킬 수 있다. 이 방식은 third-party type을 수정하지 않고 ecosystem interface에 편입할 때 유용하다.

하지만 virtual subclass 등록은 실제 method implementation을 주입하지 않는다. MRO에도 일반 inheritance와 같은 방식으로 들어가지 않는다. “등록했으니 default method도 생긴다”라고 생각하면 안 된다.

Runtime classification과 behavior reuse를 분리한다.

---

## CHAPTER 04 · `register`는 타입을 인정하지만 그 타입의 의미를 검증하지 않는다

```python
from collections.abc import Sequence

Sequence.register(MyForeignSequence)
```

이 등록은 runtime relation을 선언하지만 `MyForeignSequence`가 정말 모든 sequence semantic invariant를 지키는지 자동 검사하지 않는다. 잘못 등록하면 downstream code가 `isinstance(x, Sequence)`를 믿고 호출하다 실패할 수 있다.

따라서 register는 외부 type이 이미 contract를 충족한다는 것을 검증한 뒤 adapter-free integration을 위해 사용한다. 편의를 위해 incompatible type을 억지로 포함시키지 않는다.

---

## CHAPTER 05 · `collections.abc`는 container protocol의 operation 조합을 runtime 이름으로 제공한다

Iterable, Iterator, Collection, Sequence, Mapping 같은 ABC는 P66에서 본 container operation들을 조합해 runtime category로 표현한다.

예를 들어 iterable이라고 해서 length나 membership이 반드시 있는 것은 아니며, iterator는 single-pass state를 가질 수 있다. `Collection`과 `Sequence`는 더 강한 기대를 추가한다.

Function이 정말 필요한 최소 interface가 `Iterable`뿐이라면 `Sequence`를 요구하지 않는다. 더 강한 ABC를 요구할수록 caller가 제공할 수 있는 타입이 줄어든다.

Interface는 최소 capability principle로 선택한다.

---

## CHAPTER 06 · `__subclasshook__`은 structural recognition을 custom rule로 확장할 수 있다

ABC는 실제 상속이나 explicit register 없이도 특정 method 구조를 보고 subclass relation을 인정하는 hook을 구현할 수 있다. 하지만 이 기능은 강력한 만큼 false positive 위험이 있다.

같은 이름의 method가 존재한다고 semantic contract가 같다는 보장은 없다. Signature, return meaning, side effect가 다를 수 있다.

Structural detection을 너무 영리하게 만들기보다 필요한 operation을 실제 호출 가능한 작은 protocol로 설계하는 편이 유지보수에 유리하다.

---

## CHAPTER 07 · nominal과 structural typing은 runtime과 static analysis에서 역할이 다르다

ABC는 runtime `isinstance` relation에 강하고, typing `Protocol`은 static structural contract를 표현하는 데 강하다. 둘을 무조건 하나로 합칠 필요는 없다.

Application core가 static checker를 통해 capability를 확인하고 runtime에서는 실제 method call만 수행할 수 있다. 반대로 plugin loader가 runtime에 object category를 구분해야 하면 ABC나 explicit capability metadata가 필요할 수 있다.

Type system 도구 선택은 “어느 방식이 더 Pythonic인가”가 아니라 검증 시점과 failure mode에 맞춘다.

---

## CHAPTER 08 · ABC contract는 최소 capability와 runtime classification 필요성을 명확히 한다

ABC를 설계할 때는 어떤 method가 반드시 필요한지, default implementation을 제공할지, 외부 type을 virtual subclass로 인정할지, runtime `isinstance`가 실제로 필요한지 질문한다.

Test에서는 abstract class의 직접 생성 실패, concrete implementation, virtual subclass, register된 외부 type의 실제 behavior를 확인한다. 등록 relation만 테스트하고 semantics를 생략하지 않는다.

이 PART의 핵심은 **ABC를 상속 체계 장식으로 보지 않고, runtime에서 operation capability를 이름 붙이되 실제 semantic contract는 별도 검증해야 하는 interface 도구로 이해하는 것**이다.
