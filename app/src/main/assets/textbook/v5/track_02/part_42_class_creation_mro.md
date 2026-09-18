# PART 42 · Class creation과 MRO — namespace에서 class object와 method resolution까지

Class 문법을 instance를 만드는 틀 정도로만 이해하면 inheritance, descriptor, class decorator, metaclass가 겹칠 때 실행 순서를 설명하기 어렵다. Class body도 code로 실행되어 namespace를 만들고, 그 결과가 class creation mechanism에 전달된다. 이후 attribute lookup은 method resolution order에 따라 base class를 탐색한다. 핵심은 **class 정의 시점과 instance 사용 시점을 분리하고, 상속 관계를 이름 검색 순서로 읽는 것**이다.

---

## CHAPTER 01 · class body는 정의 시점에 실행되어 namespace를 만든다

### 시작 전 용어집

#### 1. class

- **뜻:** `class A:` 아래의 assignment와 function definition은 instance 생성 시마다 실행되는 code가 아니다.
- **왜 중요한가:** Class statement가 평가될 때 body가 실행되어 이름→객체 mapping을 만들고, 그 namespace를 사용해 class object가 생성된다.
- **예시:** `class A:` 아래의 assignment와 function definition은 instance 생성 …

#### 2. namespace

- **뜻:** Method로 보이는 function도 처음에는 class namespace에 저장되는 function object다.
- **왜 중요한가:** Instance attribute로 접근할 때 descriptor binding을 거쳐 bound method가 된다.
- **예시:** Method로 보이는 function도 처음에는 class namespace에 저장되는 function …

#### 3. 객체

- **뜻:** 따라서 class body에서 함수 호출을 하면 import/정의 시점에 side effect가 발생할 수 있다.
- **왜 중요한가:** Class-level registry를 body에서 수정하거나 decorator를 붙이는 framework는 이 정의 시점 semantics를 활용한다.
- **예시:** 따라서 class body에서 함수 호출을 하면 import/정의 시점에 …

#### 4. 함수

- **뜻:** Module import가 class creation을 일으키므로 registration 순서가 import order와 연결될 수 있다.
- **왜 중요한가:** Class body에는 일반 local scope와 비슷하지만 완전히 같은 것은 아닌 namespace 규칙이 적용된다.
- **예시:** Module import가 class creation을 일으키므로 registration 순서가 import …

정의와 호출을 같은 순간으로 생각하지 않는다.

 

 Nested function이 class local name을 lexical closure처럼 자동 capture한다고 가정하지 않는다.

---

## CHAPTER 02 · class object도 callable이며 instance construction protocol을 가진다

### 시작 전 용어집

#### 1. class

- **뜻:** Class 호출 protocol은 instance allocation 단계와 initialization 단계를 연결할 수 있다.
- **왜 중요한가:** `__new__`가 새 instance를 만들고 적절한 object를 반환한 뒤 `__init__`이 상태를 초기화하는 구조를 가진다.
- **예시:** Class 호출 protocol은 instance allocation 단계와 initialization 단계를 …

#### 2. callable

- **뜻:** )`를 호출하면 단순히 `__init__`만 실행되는 것이 아니다.
- **왜 중요한가:** 대부분의 application class는 `__init__`만 구현하면 충분하지만 immutable subclass나 singleton-like construction을 읽을 때 `__new__`를 이해해야 한다.
- **예시:** )`를 호출하면 단순히 `__init__`만 실행되는 것이 아니다.

#### 3. instance construction

- **뜻:** `__new__`가 다른 type object를 반환하면 `__init__` 호출 여부도 일반 경로와 달라질 수 있다.
- **왜 중요한가:** Constructor에서 외부 I/O와 registration을 많이 수행하면 object creation이 느리고 실패 가능하며 test하기 어려워진다.
- **예시:** `__new__`가 다른 type object를 반환하면 `__init__` 호출 여부도 …

#### 4. protocol

- **뜻:** Construction protocol은 “객체가 하나 만들어졌다”는 결과보다 어떤 단계가 ownership과 invariant를 책임지는지로 본다.
- **왜 중요한가:** Factory가 resource를 준비한 뒤 순수한 object construction을 호출하는 구조가 더 명시적일 수 있다.
- **예시:** Construction protocol은 “객체가 하나 만들어졌다”는 결과보다 어떤 단계가 …

`User(...  

 

 


---

## CHAPTER 03 · inheritance는 base class의 namespace를 복사하지 않고 lookup 관계를 만든다

### 시작 전 용어집

#### 1. inheritance

- **뜻:** Inheritance hierarchy가 깊어질수록 이름의 실제 owner를 찾기 어려워진다.
- **왜 중요한가:** Stable interface와 얕은 hierarchy를 선호하는 실용적 이유다.
- **예시:** Inheritance hierarchy가 깊어질수록 이름의 실제 owner를 찾기 어려워진다.

#### 2. base class

- **뜻:** Attribute가 instance와 subclass에 없을 때 MRO를 따라 base class namespace를 탐색한다.
- **왜 중요한가:** 그래서 base class method를 변경하면 아직 override하지 않은 subclass의 lookup 결과도 달라질 수 있다.
- **예시:** Attribute가 instance와 subclass에 없을 때 MRO를 따라 base …

#### 3. namespace

- **뜻:** Override는 같은 이름을 더 가까운 class namespace에 정의해 뒤쪽 base lookup을 가리는 방식으로 이해할 수 있다.
- **왜 중요한가:** Method 안에서 특정 base implementation을 재사용해야 할 때 `super()`가 cooperative lookup을 이어 가는 역할을 한다.
- **예시:** Override는 같은 이름을 더 가까운 class namespace에 정의해 …

#### 4. lookup

- **뜻:** Instance attribute가 method와 같은 이름으로 만들어지면 descriptor 종류에 따라 lookup precedence가 영향을 받을 수 있다.
- **왜 중요한가:** “Subclass가 항상 이긴다”처럼 단순화하지 않고 실제 attribute protocol을 본다.
- **예시:** Instance attribute가 method와 같은 이름으로 만들어지면 descriptor 종류에 …

Subclass가 base method를 “복사해서 가진다”고 생각하면 runtime monkey patch와 descriptor behavior를 설명하기 어렵다.  

 

 

 

---

## CHAPTER 04 · MRO는 다중 상속에서 하나의 일관된 검색 순서를 계산한다

### 시작 전 용어집

#### 1. MRO

- **뜻:** Diamond 구조에서 공통 base가 여러 경로로 나타나도 cooperative `super()` chain을 사용하면 같은 base initialization을 한 번의 MRO 흐름 안에서 호출할 수 있다.
- **왜 중요한가:** 특정 parent 이름을 직접 호출하면 다른 mixin과 결합했을 때 중복 호출이나 누락이 생길 수 있다.
- **예시:** Diamond 구조에서 공통 base가 여러 경로로 나타나도 cooperative …

#### 2. class

- **뜻:** Python은 class hierarchy의 local precedence와 monotonicity를 유지하는 method resolution order를 계산하고 `Class.
- **왜 중요한가:** MRO conflict가 있는 class graph는 class 생성 자체가 실패할 수 있다.
- **예시:** Python은 class hierarchy의 local precedence와 monotonicity를 유지하는 method …

#### 3. 실패

- **뜻:** 다중 상속에서는 어떤 base를 먼저 찾을지 단순 왼쪽 우선만으로 해결되지 않는다.
- **왜 중요한가:** 이는 모호한 hierarchy를 runtime에 임의로 선택하지 않고 거부하는 안전 장치다.
- **예시:** 다중 상속에서는 어떤 base를 먼저 찾을지 단순 왼쪽 …

__mro__`로 확인할 수 있다.

 

 

다중 상속을 사용할 때는 graph를 실제 MRO sequence로 펼쳐 각 method의 다음 대상이 어디인지 확인한다. 자연어 “A와 B를 상속한다”만으로는 실행 순서를 충분히 설명하지 못한다.

---

## CHAPTER 05 · cooperative `super()`는 부모 하나를 지칭하는 keyword가 아니다

### 시작 전 용어집

#### 1. cooperative

- **뜻:** Cooperative initialization을 하려면 각 class가 자신이 소비할 argument를 받고 나머지를 다음 `super()`에 전달하는 등 공통 convention을 지켜야 한다.
- **왜 중요한가:** 한 class가 chain을 끊고 특정 base를 직접 호출하면 다른 mixin initialization이 건너뛰어질 수 있다.
- **예시:** Cooperative initialization을 하려면 각 class가 자신이 소비할 argument를 …

#### 2. super

- **뜻:** `super()`를 “내 부모 class”라고만 이해하면 mixin 조합에서 오류가 난다.
- **왜 중요한가:** 실제로는 현재 class와 instance를 기준으로 MRO에서 다음 구현을 찾는 proxy다.
- **예시:** `super()`를 “내 부모 class”라고만 이해하면 mixin 조합에서 오류가 …

#### 3. keyword

- **뜻:** 따라서 어느 class의 method에서 호출했는지와 현재 MRO가 무엇인지가 결과를 결정한다.
- **왜 중요한가:** 모든 hierarchy를 cooperative하게 만들 필요는 없다.
- **예시:** 따라서 어느 class의 method에서 호출했는지와 현재 MRO가 무엇인지가 …

#### 4. class

- **뜻:** 단일 상속이나 명시적 composition이 더 단순하다면 그쪽이 낫다.
- **왜 중요한가:** Mixin을 사용할 때만 MRO contract를 엄격하게 지킨다.
- **예시:** 단일 상속이나 명시적 composition이 더 단순하다면 그쪽이 낫다.

`super()` 호출 순서를 test할 때 method 호출 횟수와 state invariant를 검증한다. 단순히 exception이 없었다는 사실만으로 각 base가 정확히 한 번 초기화됐다고 할 수 없다.

---

## CHAPTER 06 · mixin은 작은 capability를 제공하고 독립 state를 최소화한다

### 시작 전 용어집

#### 1. mixin

- **뜻:** Mixin은 standalone entity hierarchy보다 여러 class에 조합할 작은 behavior를 제공하는 데 적합하다.
- **왜 중요한가:** Serialization, logging helper처럼 narrow capability를 추가할 수 있다.
- **예시:** Mixin은 standalone entity hierarchy보다 여러 class에 조합할 작은 …

#### 2. capability

- **뜻:** Protocol이나 abstract method로 필요한 capability를 명시하면 잘못된 조합을 static/runtime에서 더 일찍 발견할 수 있다.
- **왜 중요한가:** 두 mixin이 같은 helper method를 정의하면 MRO에 따라 한쪽이 가려질 수 있다.
- **예시:** Protocol이나 abstract method로 필요한 capability를 명시하면 잘못된 조합을 …

#### 3. state

- **뜻:** 하지만 mixin이 많은 hidden field와 initialization 순서를 요구하면 실제로는 복잡한 base class가 된다.
- **왜 중요한가:** Mixin method가 `self`에 어떤 attribute가 존재한다고 가정한다면 그 requirement가 contract다.
- **예시:** 하지만 mixin이 많은 hidden field와 initialization 순서를 요구하면 …

#### 4. class

- **뜻:** Internal helper 이름과 public extension method namespace를 관리한다.
- **왜 중요한가:** Composition으로 같은 기능을 명시적 delegate object에 둘 수 있다면 dependency가 더 선명할 수 있다.
- **예시:** Internal helper 이름과 public extension method namespace를 관리한다.

이름 충돌도 고려한다.  

 Mixin은 inheritance의 편리함보다 capability 조합의 단순성이 실제로 있을 때 사용한다.

---

## CHAPTER 07 · abstract base class와 Protocol은 nominal과 structural contract의 차이다

### 시작 전 용어집

#### 1. abstract base

- **뜻:** ABC를 상속하고 abstract method를 구현하는 방식은 explicit nominal relationship을 만든다.
- **왜 중요한가:** Protocol은 상속 선언 없이 required shape를 만족하는 object도 같은 static contract로 취급할 수 있다.
- **예시:** ABC를 상속하고 abstract method를 구현하는 방식은 explicit nominal …

#### 2. class

- **뜻:** Framework가 plugin registration 단계에서 특정 base subclass인지 확인해야 한다면 ABC가 유용할 수 있다.
- **왜 중요한가:** read()` capability만 요구하고 다양한 외부 object를 받아들이고 싶다면 Protocol이 더 느슨하다.
- **예시:** Framework가 plugin registration 단계에서 특정 base subclass인지 확인해야 …

#### 3. protocol

- **뜻:** 두 방식은 확장성과 runtime enforcement의 trade-off가 다르다.
- **왜 중요한가:** Abstract method 존재가 semantic correctness를 보장하지는 않는다.
- **예시:** 두 방식은 확장성과 runtime enforcement의 trade-off가 다르다.

#### 4. nominal

- **뜻:** Nominal hierarchy를 domain classification으로 남용하지 않는다.
- **왜 중요한가:** 재사용하고 싶은 code가 있다고 inheritance를 만드는 대신 실제 “is-a” behavior contract가 있는지 확인한다.
- **예시:** Nominal hierarchy를 domain classification으로 남용하지 않는다.

Application core가 `.

 `save()` method를 구현했지만 transaction contract가 다르면 substitutability가 깨진다. Behavior test가 필요하다.

 

---

## CHAPTER 08 · `__init_subclass__`는 subclass 생성 시 정책을 검사하거나 등록할 수 있다

### 시작 전 용어집

#### 1. class

- **뜻:** Base class는 새 subclass가 만들어질 때 hook을 받아 naming rule, required class attribute, registry registration을 수행할 수 있다.
- **왜 중요한가:** Full metaclass보다 가벼운 extension point다.
- **예시:** Base class는 새 subclass가 만들어질 때 hook을 받아 …

#### 2. plugin

- **뜻:** Plugin base가 `plugin_name`을 반드시 정의하도록 검사하거나 subclass를 registry에 넣을 수 있다.
- **왜 중요한가:** 하지만 import만으로 global registry가 변하는 side effect가 생기므로 discovery/lifecycle 정책과 함께 사용한다.
- **예시:** Plugin base가 `plugin_name`을 반드시 정의하도록 검사하거나 subclass를 registry에 …

#### 3. import

- **뜻:** 여러 base가 `__init_subclass__`를 구현하는 다중 상속에서는 cooperative `super()`가 중요하다.
- **왜 중요한가:** Keyword argument를 각 hook이 소비하고 다음 hook에 전달하는 convention을 지킨다.
- **예시:** 여러 base가 `__init_subclass__`를 구현하는 다중 상속에서는 cooperative `super()`가 …

#### 4. validation

- **뜻:** Validation을 class creation에서 너무 많이 수행하면 test fixture와 dynamic class generation이 어려워질 수 있다.
- **왜 중요한가:** 실제 class-level invariant만 검사한다.
- **예시:** Validation을 class creation에서 너무 많이 수행하면 test fixture와 …

---

## CHAPTER 09 · metaclass는 class object 생성 자체를 customize하는 더 높은 경계다

### 시작 전 용어집

#### 1. metaclass

- **뜻:** Class의 class를 metaclass라고 부를 수 있고, metaclass는 class namespace를 받아 최종 class object를 만드는 과정에 개입할 수 있다.
- **왜 중요한가:** ORM model field 수집, interface enforcement 같은 framework에서 사용된다.
- **예시:** Class의 class를 metaclass라고 부를 수 있고, metaclass는 class …

#### 2. class object

- **뜻:** Metaclass conflict는 여러 base가 호환되지 않는 metaclass를 사용할 때 발생할 수 있다.
- **왜 중요한가:** Application 개발자가 서로 다른 framework base를 결합하려다 만나는 대표적인 고급 상속 문제다.
- **예시:** Metaclass conflict는 여러 base가 호환되지 않는 metaclass를 사용할 …

#### 3. customize

- **뜻:** Metaclass로 해결할 수 있다고 해서 첫 선택은 아니다.
- **왜 중요한가:** Class decorator, factory, `__init_subclass__`, explicit registry가 더 단순하면 그쪽을 선호한다.
- **예시:** Metaclass로 해결할 수 있다고 해서 첫 선택은 아니다.

#### 4. 경계

- **뜻:** Metaclass는 class creation semantics 전체를 바꾸므로 이해 비용이 높다.
- **왜 중요한가:** Framework를 사용할 때는 metaclass 내부 구현을 모두 알 필요는 없지만 class 정의 순간 field가 수집되고 descriptor로 교체되는 등 public behavior를 추적할 수 있어야 한다.
- **예시:** Metaclass는 class creation semantics 전체를 바꾸므로 이해 비용이 …

---

## CHAPTER 10 · class hierarchy는 이름 검색 graph와 state ownership graph를 함께 검토한다

### 시작 전 용어집

#### 1. class

- **뜻:** 상속 구조를 설계할 때 diagram에 class 이름만 그리지 말고 어떤 method가 어느 namespace에 있고 어떤 state field를 누가 초기화·변경하는지 표시한다.
- **왜 중요한가:** Method lookup은 MRO가 결정하고 state ownership은 별도 design이므로 둘이 어긋나면 hidden coupling이 생긴다.
- **예시:** 상속 구조를 설계할 때 diagram에 class 이름만 그리지 …

#### 2. graph

- **뜻:** Mixin 세 개가 같은 object state를 서로 수정한다면 method 재사용은 편해도 invariant owner가 사라진다.
- **왜 중요한가:** Composition으로 stateful collaborator를 분리하는 편이 낫다.
- **예시:** Mixin 세 개가 같은 object state를 서로 수정한다면 …

#### 3. state ownership

- **뜻:** Hierarchy 변경은 public `isinstance` relationship과 serialization, plugin registry에 영향을 줄 수 있다.
- **왜 중요한가:** Internal refactor라고 단정하지 않는다.
- **예시:** Hierarchy 변경은 public `isinstance` relationship과 serialization, plugin registry에 …

#### 4. MRO

- **뜻:** Class creation을 배우는 목적은 metaclass 기교가 아니다.
- **예시:** Class creation을 배우는 목적은 metaclass 기교가 아니다.

**class body 실행→class object 생성→MRO 계산→attribute lookup→bound method 호출의 흐름을 이해해 inheritance가 실제로 어떤 검색·state 계약을 만드는지 판단하는 것**이다.
