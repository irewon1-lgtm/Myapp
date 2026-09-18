# PART 82 · ABC runtime contracts — abstract method·virtual subclass·collections ABC를 구분하기

Python은 duck typing을 강조하지만 runtime interface를 전혀 표현할 수 없는 언어는 아니다. `abc`와 `collections.abc`는 abstract method, virtual subclass, runtime membership test를 제공한다. 중요한 것은 이 도구를 Java식 nominal hierarchy 복제에 쓰는 것이 아니라 **어떤 operation set을 runtime에서 안정적으로 식별해야 하는지**를 기준으로 선택하는 것이다.

---

## CHAPTER 01 · ABC는 상속 계층보다 runtime interface intent를 명시하는 도구다

### 시작 전 용어집

#### 1. ABC

- **뜻:** 하지만 ABC가 모든 semantic behavior를 자동 검증하는 것은 아니다.
- **왜 중요한가:** `load()`가 존재해도 timeout, consistency, return type 의미가 틀릴 수 있다.
- **예시:** from abc import ABC, abstractmethod / class Storage(ABC):

#### 2. runtime interface

- **뜻:** Abstract Base Class는 공통 operation contract를 이름 붙이고, subclass가 필요한 method를 구현하도록 요구할 수 있다.
- **왜 중요한가:** 이제 concrete subclass는 abstract method를 충족해야 일반 instance construction이 가능하다.
- **예시:** from abc import ABC, abstractmethod / class Storage(ABC):

#### 3. intent

- **뜻:** ABC는 method presence와 일부 default behavior를 구조화하는 도구이고, 실제 contract는 문서·test와 함께 완성된다.
- **예시:** from abc import ABC, abstractmethod / class Storage(ABC):

```python
from abc import ABC, abstractmethod

class Storage(ABC):
    @abstractmethod
    def load(self, key):
        ...
```

  


---

## CHAPTER 02 · `abstractmethod`는 구현 강제와 reusable default body를 함께 가질 수 있다

### 시작 전 용어집

#### 1. abstractmethod

- **뜻:** Abstract method가 반드시 body 없는 placeholder일 필요는 없다.
- **왜 중요한가:** 공통 동작을 제공하면서 subclass가 override하도록 요구하는 pattern도 가능하다.
- **예시:** Abstract method가 반드시 body 없는 placeholder일 필요는 없다.

#### 2. reusable default

- **뜻:** 그러나 abstract default body에 복잡한 workflow를 넣으면 subclass가 `super()` 호출 순서를 알아야 하는 hidden protocol이 생긴다.
- **왜 중요한가:** Template Method pattern이 적합한 문제인지 검토한다.
- **예시:** 그러나 abstract default body에 복잡한 workflow를 넣으면 subclass가 …

#### 3. body

- **뜻:** Abstract property, classmethod 등과 결합할 때 decorator order와 framework semantics를 정확히 따른다.
- **왜 중요한가:** 목적은 “추상화가 많아 보이는 구조”가 아니라 invalid concrete implementation을 일찍 막는 것이다.
- **예시:** Abstract property, classmethod 등과 결합할 때 decorator order와 …

---

## CHAPTER 03 · virtual subclass는 실제 inheritance 없이 runtime membership을 인정할 수 있다

### 시작 전 용어집

#### 1. virtual subclass

- **뜻:** ABC는 특정 외부 class를 virtual subclass로 등록해 `isinstance`/`issubclass` 관점에서 contract에 참여시킬 수 있다.
- **왜 중요한가:** 이 방식은 third-party type을 수정하지 않고 ecosystem interface에 편입할 때 유용하다.
- **예시:** ABC는 특정 외부 class를 virtual subclass로 등록해 `isinstance`/`issubclass` …

#### 2. inheritance

- **뜻:** MRO에도 일반 inheritance와 같은 방식으로 들어가지 않는다.
- **왜 중요한가:** “등록했으니 default method도 생긴다”라고 생각하면 안 된다.
- **예시:** MRO에도 일반 inheritance와 같은 방식으로 들어가지 않는다.

#### 3. runtime membership

- **뜻:** 하지만 virtual subclass 등록은 실제 method implementation을 주입하지 않는다.
- **왜 중요한가:** Runtime classification과 behavior reuse를 분리한다.
- **예시:** 하지만 virtual subclass 등록은 실제 method implementation을 주입하지 …

---

## CHAPTER 04 · `register`는 타입을 인정하지만 그 타입의 의미를 검증하지 않는다

### 시작 전 용어집

#### 1. register

- **뜻:** 따라서 register는 외부 type이 이미 contract를 충족한다는 것을 검증한 뒤 adapter-free integration을 위해 사용한다.
- **왜 중요한가:** 편의를 위해 incompatible type을 억지로 포함시키지 않는다.
- **예시:** from collections.abc import Sequence / Sequence.register(MyForeignSequence)

#### 2. 타입

- **뜻:** 이 등록은 runtime relation을 선언하지만 `MyForeignSequence`가 정말 모든 sequence semantic invariant를 지키는지 자동 검사하지 않는다.
- **왜 중요한가:** 잘못 등록하면 downstream code가 `isinstance(x, Sequence)`를 믿고 호출하다 실패할 수 있다.
- **예시:** from collections.abc import Sequence / Sequence.register(MyForeignSequence)

```python
from collections.abc import Sequence

Sequence.register(MyForeignSequence)
```

 

 

---

**검증 시나리오 P82-C4 — CHAPTER 04 · `register`는 타입을 인정하지만 그 타입의 의미를 검증하지 않는다**
`CHAPTER 04 · `register`는 타입을 인정하지만 그 타입의 의미를 검증하지 않는다` 검증은 성공 사례를 기준선으로 저장하는 데서 시작한다. P82-C4에서는 `CHAPTER 04 · `register`는 타입을 인정하지만 그 타입의 의미를 검증하지 않는다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 04 · `register`는 타입을 인정하지만 그 타입의 의미를 검증하지 않는다`에 대해 실패 조건은 하나만 주입고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 04 · `register`는 타입을 인정하지만 그 타입의 의미를 검증하지 않는다`의 로그 시각과 상태 식별자를 맞춰 본다하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 04 · `register`는 타입을 인정하지만 그 타입의 의미를 검증하지 않는다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P82-C4의 마무리는 성공·실패 모두 결정적으로 끝나는지 확인하는 것이다. 통과 기준은 `CHAPTER 04 · `register`는 타입을 인정하지만 그 타입의 의미를 검증하지 않는다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 05 · `collections.abc`는 container protocol의 operation 조합을 runtime 이름으로 제공한다

### 시작 전 용어집

#### 1. collection

- **뜻:** Iterable, Iterator, Collection, Sequence, Mapping 같은 ABC는 P66에서 본 container operation들을 조합해 runtime category로 표현한다.
- **왜 중요한가:** 예를 들어 iterable이라고 해서 length나 membership이 반드시 있는 것은 아니며, iterator는 single-pass state를 가질 수 있다.
- **예시:** Iterable, Iterator, Collection, Sequence, Mapping 같은 ABC는 P66에서 …

#### 2. ABC

- **뜻:** 더 강한 ABC를 요구할수록 caller가 제공할 수 있는 타입이 줄어든다.
- **왜 중요한가:** Interface는 최소 capability principle로 선택한다.
- **예시:** 더 강한 ABC를 요구할수록 caller가 제공할 수 있는 …

#### 3. container protocol

- **뜻:** `Collection`과 `Sequence`는 더 강한 기대를 추가한다.
- **왜 중요한가:** Function이 정말 필요한 최소 interface가 `Iterable`뿐이라면 `Sequence`를 요구하지 않는다.
- **예시:** `Collection`과 `Sequence`는 더 강한 기대를 추가한다.

---

## CHAPTER 06 · `__subclasshook__`은 structural recognition을 custom rule로 확장할 수 있다

### 시작 전 용어집

#### 1. __subclasshook__

- **뜻:** ABC는 실제 상속이나 explicit register 없이도 특정 method 구조를 보고 subclass relation을 인정하는 hook을 구현할 수 있다.
- **왜 중요한가:** 하지만 이 기능은 강력한 만큼 false positive 위험이 있다.
- **예시:** ABC는 실제 상속이나 explicit register 없이도 특정 method …

#### 2. structural recognition

- **뜻:** 같은 이름의 method가 존재한다고 semantic contract가 같다는 보장은 없다.
- **왜 중요한가:** Signature, return meaning, side effect가 다를 수 있다.
- **예시:** 같은 이름의 method가 존재한다고 semantic contract가 같다는 보장은 …

#### 3. custom rule

- **뜻:** Structural detection을 너무 영리하게 만들기보다 필요한 operation을 실제 호출 가능한 작은 protocol로 설계하는 편이 유지보수에 유리하다.
- **예시:** Structural detection을 너무 영리하게 만들기보다 필요한 operation을 실제 …

---

## CHAPTER 07 · nominal과 structural typing은 runtime과 static analysis에서 역할이 다르다

### 시작 전 용어집

#### 1. nominal

- **뜻:** ABC는 runtime `isinstance` relation에 강하고, typing `Protocol`은 static structural contract를 표현하는 데 강하다.
- **왜 중요한가:** Application core가 static checker를 통해 capability를 확인하고 runtime에서는 실제 method call만 수행할 수 있다.
- **예시:** ABC는 runtime `isinstance` relation에 강하고, typing `Protocol`은 static …

#### 2. structural typing

- **뜻:** 반대로 plugin loader가 runtime에 object category를 구분해야 하면 ABC나 explicit capability metadata가 필요할 수 있다.
- **왜 중요한가:** Type system 도구 선택은 “어느 방식이 더 Pythonic인가”가 아니라 검증 시점과 failure mode에 맞춘다.
- **예시:** 반대로 plugin loader가 runtime에 object category를 구분해야 하면 …

둘을 무조건 하나로 합칠 필요는 없다.

 


---

## CHAPTER 08 · ABC contract는 최소 capability와 runtime classification 필요성을 명확히 한다

### 시작 전 용어집

#### 1. ABC

- **뜻:** ABC를 설계할 때는 어떤 method가 반드시 필요한지, default implementation을 제공할지, 외부 type을 virtual subclass로 인정할지, runtime `isinstance`가 실제로 필요한지 질문한다.
- **왜 중요한가:** Test에서는 abstract class의 직접 생성 실패, concrete implementation, virtual subclass, register된 외부 type의 실제 behavior를 확인한다.
- **예시:** ABC를 설계할 때는 어떤 method가 반드시 필요한지, default …

#### 2. ABC contract

- **뜻:** 등록 relation만 테스트하고 semantics를 생략하지 않는다.
- **왜 중요한가:** 이 PART의 핵심은 **ABC를 상속 체계 장식으로 보지 않고, runtime에서 operation capability를 이름 붙이되 실제 semantic contract는 별도 검증해야 하는 interface 도구로 이해하는 것**이다.
- **예시:** 등록 relation만 테스트하고 semantics를 생략하지 않는다.

---

## 실전 학습 루프 · ABC와 runtime contract

### 1. 쉬운 예

공통 인터페이스를 문서로만 약속하면 누락 구현을 늦게 발견할 수 있다. ABC는 abstract method를 통해 특정 subclass가 제공해야 할 동작을 instance 생성 시점 가까이에서 검증할 수 있다.

### 2. 한 줄 해석

ABC는 “어떤 이름이 존재해야 한다”는 runtime 계약을 표현하지만 모든 의미적 올바름까지 보장하지는 않는다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상한다. 그 다음 아래 최소 예제를 실행하고, 예상이 틀렸다면 **호출 순서와 상태 변화**를 표시한다.

```python
from abc import ABC, abstractmethod

class Store(ABC):
    @abstractmethod
    def save(self, value): ...

class MemoryStore(Store):
    def save(self, value):
        return value
```

### 4. 수정 실습

1. `save`를 제거한 subclass의 instance 생성을 시도한다.
2. ABC와 typing.Protocol이 각각 runtime/정적 검증에서 맡는 역할을 비교한다.

수정 후에는 정상 예제만 다시 보지 말고 실패·경계·반복 호출 중 하나를 추가해 계약이 유지되는지 확인한다.

### 5. 확인 문제

abstract method를 구현하면 그 메서드의 의미까지 자동으로 올바르다고 보장될까?

### 6. 정답과 오답 설명

**정답:** 아니다. 존재와 호출 형태 일부는 강제할 수 있지만 도메인 의미와 불변식은 테스트·문서·타입 규칙이 더 필요하다.

**자주 나오는 오답:** ABC를 완전한 formal specification처럼 보는 것이 오답이다.

이 PART를 마칠 때는 해당 문법 이름을 외우는 데서 멈추지 말고 **언제 호출되는가 / 무엇을 읽거나 바꾸는가 / 실패하면 어디로 가는가** 세 문장으로 설명한다.

## 현장 디버깅 체크 · ABC runtime contract

### 증상에서 시작한다

구현 클래스는 생성되지만 method가 잘못된 의미나 signature로 동작해 runtime에서 늦게 실패한다. 이때 문법을 먼저 고치면 원인이 가려질 수 있다. 재현 입력과 실제 상태를 보존한 뒤 **어느 경계에서 처음 기대와 달라졌는지**를 찾는다.

### 먼저 볼 증거

abstract method 집합, override signature, typing 결과, contract test를 함께 확인한다. 최종 출력 하나만 보지 말고 호출 전 값, 호출 뒤 값, 예외 또는 resource 상태를 나란히 두면 원인 후보가 급격히 줄어든다.

### 일부러 실패시켜 보기

필수 메서드는 이름만 같고 반환 의미가 틀린 fake 구현을 만들어 ABC가 무엇을 잡고 무엇을 못 잡는지 본다. 정상 예제만 통과시키는 것은 검증이 아니다. 경계 조건을 강제로 만들고 같은 증상이 반복되는지 확인해야 수정 전후를 비교할 수 있다.

### 통과 기준

ABC 통과 여부와 별개로 공통 contract test가 모든 구현에서 동일한 불변식과 오류 의미를 검증해야 한다. 이 기준을 테스트 이름과 assertion으로 옮기면 이후 refactoring에서도 같은 오류가 돌아오는지 자동으로 잡을 수 있다.

