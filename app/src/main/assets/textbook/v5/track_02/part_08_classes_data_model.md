# PART 08 · Class와 데이터 모델 — 상태·행동·불변조건을 하나의 경계로 묶기

Class는 변수와 함수를 한 파일에 모으는 문법이 아니다. 서로 관련된 상태와 그 상태를 변경하는 규칙을 하나의 abstraction으로 묶고, 잘못된 상태가 만들어지는 경로를 제한하는 도구다. Python의 object model을 이해하려면 instance와 class attribute, method binding, initialization, inheritance, composition, special method를 단편적으로 외우기보다 **객체가 어떤 계약을 소유하는가**를 중심으로 봐야 한다.

---

## CHAPTER 01 · 객체는 데이터 묶음이 아니라 불변조건의 소유자가 될 수 있다

### 시작 전 용어집

#### 1. 객체

- **뜻:** 데이터와 behavior를 묶는 목적은 객체지향 형식을 따르기 위해서가 아니라 **규칙의 주인을 명확히 하기 위해서**다.
- **왜 중요한가:** 주문을 dict 하나로 표현하면 어디서든 `order["status"] = "SHIPPED"`처럼 값을 바꿀 수 있다.
- **예시:** class Order: / def ship(self):

#### 2. 조건

- **뜻:** 이 방식은 작고 단순한 프로그램에서는 충분하지만 상태 전이 규칙이 늘어나면 잘못된 조합을 만들기 쉬워진다.
- **왜 중요한가:** Class를 사용하면 상태와 상태 변경 함수를 같은 경계에 두어 허용되지 않은 전이를 차단할 수 있다.
- **예시:** class Order: / def ship(self):

#### 3. dict

- **뜻:** status`라는 필드가 생겼다는 사실이 아니라 상태 변경 권한을 `ship()`이라는 operation으로 좁혔다는 점이다.
- **왜 중요한가:** 생성 시점부터 유효한 상태만 만들고 이후 변경도 method를 통해 수행하면 invariant가 한곳에 모인다.
- **예시:** class Order: / def ship(self):

#### 4. 상태 전이

- **뜻:** 외부 코드가 내부 attribute를 임의로 수정할 수 있다면 이 보호 경계가 약해진다.
- **왜 중요한가:** 단순히 값을 전달하는 짧은 구조라면 tuple이나 dataclass만으로 충분할 수 있다.
- **예시:** class Order: / def ship(self):

```python
class Order:
    def ship(self):
        if self.status != "PAID":
            raise InvalidTransition(self.status, "SHIPPED")
        self.status = "SHIPPED"
```

핵심은 `self.  

모든 데이터에 class가 필요한 것은 아니다.  반대로 여러 필드가 함께 만족해야 하는 규칙, 상태 변화, identity, lifecycle이 있다면 class가 의미 있는 boundary가 된다. 

---

## CHAPTER 02 · instance와 class는 서로 다른 namespace를 가진다

### 시작 전 용어집

#### 1. instance

- **뜻:** Class를 정의하면 class object가 만들어지고, 이를 호출해 instance를 생성할 수 있다.
- **왜 중요한가:** Class attribute와 instance attribute는 같은 이름처럼 보여도 저장 위치와 공유 범위가 다르다.
- **예시:** class Bag: / items = [] # 모든 …

#### 2. class

- **뜻:** 이 차이를 모르면 mutable class attribute 때문에 여러 instance가 예상하지 않게 같은 상태를 공유할 수 있다.
- **왜 중요한가:** 여러 `Bag()` instance가 `items`를 통해 같은 list를 수정하면 한 객체의 추가가 다른 객체에서도 보일 수 있다.
- **예시:** class Bag: / items = [] # 모든 …

#### 3. namespace

- **뜻:** 각 instance가 독립적인 collection을 가져야 한다면 initialization 과정에서 `self.
- **왜 중요한가:** items = []`처럼 instance attribute를 만들어야 한다.
- **예시:** class Bag: / items = [] # 모든 …

#### 4. mutable

- **뜻:** 그러나 mutable shared state라면 ownership과 동시성 문제가 생긴다.
- **왜 중요한가:** “class 안에 있으니 객체별 값”이라는 가정 대신 실제 attribute lookup과 객체 공유 관계를 확인한다.
- **예시:** class Bag: / items = [] # 모든 …

```python
class Bag:
    items = []  # 모든 instance가 공유할 수 있는 class attribute
```

 

Attribute lookup은 단순히 instance dict 한 곳만 보는 것이 아니라 class와 상속 계층까지 포함하는 규칙을 따른다. Method 역시 class에 저장된 function이 instance를 통해 접근될 때 binding되어 instance가 첫 argument로 전달되는 형태로 이해할 수 있다. `self`는 예약어가 아니라 관례적 parameter 이름이지만, instance method의 의미를 드러내기 때문에 표준 관례를 따른다.

Class attribute는 상수처럼 모든 instance가 공유해야 하는 policy나 metadata에 적합할 수 있다.  

---

## CHAPTER 03 · 초기화는 필드 대입보다 유효한 객체를 만드는 계약이다

### 시작 전 용어집

#### 1. 객체

- **뜻:** 모든 필드에 argument를 그대로 대입하는 것만으로 끝내기보다 객체가 생성 직후부터 invariant를 만족하도록 해야 한다.
- **왜 중요한가:** `quantity > 0`, `end >= start`, `currency in allowed_set` 같은 규칙이 있다면 생성 경계에서 검증하는 것이 후속 method를 단순하게 만든다.
- **예시:** 모든 필드에 argument를 그대로 대입하는 것만으로 끝내기보다 객체가 …

#### 2. 계약

- **뜻:** `__init__`은 instance가 준비되는 과정에서 필요한 초기 상태를 설정하는 method다.
- **왜 중요한가:** 객체가 한동안 “반쯤 유효한 상태”로 존재하고 setter를 여러 번 호출해야 완성되는 설계는 호출 순서 오류를 만든다.
- **예시:** `__init__`은 instance가 준비되는 과정에서 필요한 초기 상태를 설정하는 …

#### 3. 상태

- **뜻:** Immutable value object를 만들고 싶다면 생성 후 상태 변경 경로를 제한한다.
- **왜 중요한가:** Python은 언어 차원에서 모든 형태의 mutation을 자동 금지해 주는 단순한 모드만 있는 것은 아니므로 dataclass의 frozen 옵션이나 property, convention 등을 목적에 맞게 사용한다.
- **예시:** Immutable value object를 만들고 싶다면 생성 후 상태 …

#### 4. set

- **뜻:** 필수 값은 생성 시점에 받고, 비동기 조회나 외부 I/O 때문에 즉시 만들 수 없다면 factory function이나 builder처럼 생성 절차를 별도 abstraction으로 만들 수 있다.
- **왜 중요한가:** 잘못된 값에서 `ValueError`를 던질지 domain-specific exception을 사용할지, parser 단계에서 걸러낼지 결정한다.
- **예시:** 필수 값은 생성 시점에 받고, 비동기 조회나 외부 …

Construction failure도 계약이다.  생성자가 network 요청까지 수행하면 객체 하나를 만드는 행위가 느리고 실패 가능한 외부 operation이 되어 테스트와 lifetime 관리가 복잡해질 수 있다. 가능한 경우 객체 생성과 외부 resource 획득을 분리한다.

  핵심은 호출자가 어떤 상태 변화를 기대해도 되는지 API가 드러내는 것이다.

---

## CHAPTER 04 · dataclass는 보일러플레이트를 줄이지만 도메인 규칙을 대신하지 않는다

### 시작 전 용어집

#### 1. dataclass

- **뜻:** `dataclass`는 이런 구조적 코드를 생성해 값 중심 객체를 간결하게 만들 수 있다.
- **왜 중요한가:** 주문 line, 좌표, 설정 snapshot처럼 필드 구성이 중심인 모델에서 유용하다.
- **예시:** from dataclasses import dataclass / @dataclass(frozen=True)

#### 2. 객체

- **뜻:** 여러 field를 가진 데이터 객체에는 초기화, 표현, equality 같은 반복 코드가 필요하다.
- **왜 중요한가:** 이 선언만으로 `amount >= 0`이나 지원 통화 목록 같은 domain invariant가 자동으로 생기는 것은 아니다.
- **예시:** from dataclasses import dataclass / @dataclass(frozen=True)

#### 3. 반복

- **뜻:** `__post_init__` 또는 별도 factory에서 검증해야 한다.
- **왜 중요한가:** `frozen=True` 역시 모든 nested object가 deep immutable이 된다는 뜻은 아니다.
- **예시:** from dataclasses import dataclass / @dataclass(frozen=True)

#### 4. import

- **뜻:** Field가 mutable list를 참조한다면 그 list 자체는 여전히 바뀔 수 있다.
- **왜 중요한가:** Equality가 field 전체 비교로 생성되는 것이 실제 업무 identity와 맞는지도 확인한다.
- **예시:** from dataclasses import dataclass / @dataclass(frozen=True)

```python
from dataclasses import dataclass

@dataclass(frozen=True)
class Money:
    amount: int
    currency: str
```

   

 Entity는 database ID가 같으면 같은 객체로 볼 수 있지만, value object는 모든 값이 같아야 같은 것으로 보는 경우가 많다. 자동 생성 기능이 domain semantics와 맞을 때 사용하고 그렇지 않으면 명시적으로 정의한다.

Dataclass는 “class를 쓰는 쉬운 방법”이라기보다 데이터 중심 abstraction의 의도를 표현한다. Behavior와 lifecycle이 복잡해지면 일반 class와 method가 더 자연스러울 수 있고, 단순 mapping 전달이면 dict가 충분할 수도 있다. 도구 선택은 생성되는 코드량보다 모델의 의미를 기준으로 한다.

---

## CHAPTER 05 · property는 method 호출을 attribute interface로 표현하지만 비용과 실패를 숨길 수 있다

### 시작 전 용어집

#### 1. property

- **뜻:** 그러나 property가 단순 field 접근처럼 보여도 내부에서 비싼 I/O를 수행하거나 예측하기 어려운 side effect를 가지면 호출자가 비용을 오해할 수 있다.
- **왜 중요한가:** Setter는 invariant를 한곳에 모을 수 있지만 객체 상태를 어디서든 변경 가능하게 만드는 많은 setter는 encapsulation을 약화시킬 수 있다.
- **예시:** class Temperature: / @property

#### 2. method

- **뜻:** value` 같은 attribute syntax 뒤에 계산이나 validation method를 연결할 수 있다.
- **왜 중요한가:** 기존 public attribute를 validation이 있는 interface로 바꿀 때도 호출 형태를 유지할 수 있다.
- **예시:** class Temperature: / @property

#### 3. attribute interface

- **뜻:** `set_status("SHIPPED")`보다 `ship()`처럼 업무 의미가 있는 operation이 허용 전이를 더 잘 표현한다.
- **왜 중요한가:** 계산 property는 객체의 현재 상태에서 빠르게 도출되는 값에 적합하다.
- **예시:** class Temperature: / @property

#### 4. 실패

- **뜻:** Network fetch, database query, long computation처럼 latency와 failure가 중요한 operation은 명시적 method 이름이 비용을 더 잘 드러낼 수 있다.
- **왜 중요한가:** API surface는 문법적 편리함뿐 아니라 호출자가 형성할 기대를 고려해야 한다.
- **예시:** class Temperature: / @property

Property를 사용하면 `obj.  

```python
class Temperature:
    @property
    def celsius(self):
        return self._celsius

    @celsius.setter
    def celsius(self, value):
        if value < -273.15:
            raise ValueError("below absolute zero")
        self._celsius = value
```

 

  

---

## CHAPTER 06 · inheritance는 코드 재사용보다 substitutability 계약으로 평가한다

### 시작 전 용어집

#### 1. inheritance

- **뜻:** Inheritance를 사용하면 subclass가 base class의 interface와 일부 implementation을 물려받는다.
- **왜 중요한가:** 가장 중요한 질문은 “코드를 얼마나 줄일 수 있는가”보다 **subclass instance를 base class가 요구되는 곳에 넣어도 계약이 유지되는가**다.
- **예시:** 예를 들어 base class가 `withdraw(amount)`에서 양수 amount를 허용하고 …

#### 2. substitutability

- **뜻:** 이 대체 가능성이 깨지면 상속 계층은 호출자의 가정을 무너뜨린다.
- **왜 중요한가:** 예를 들어 base class가 `withdraw(amount)`에서 양수 amount를 허용하고 성공 시 balance를 정확히 줄인다고 약속한다면 subclass가 더 강한 precondition을 요구하거나 전혀 다른 의미로 결과를 바꾸면 base interface를 신뢰하기 어렵다.
- **예시:** 이 대체 가능성이 깨지면 상속 계층은 호출자의 가정을 …

#### 3. 계약

- **뜻:** Type hierarchy는 분류 체계가 아니라 behavior contract다.
- **왜 중요한가:** 상속은 parent implementation의 내부 세부사항에 subclass가 강하게 결합될 수 있다.
- **예시:** Type hierarchy는 분류 체계가 아니라 behavior contract다.

#### 4. class

- **뜻:** Protected hook이나 method override 순서를 잘못 이해하면 base class 변경이 subclass를 깨뜨릴 수 있다.
- **왜 중요한가:** 깊은 hierarchy는 실제 실행 method를 찾기 어렵게 하고 여러 mixin이 결합되면 method resolution order까지 고려해야 한다.
- **예시:** Protected hook이나 method override 순서를 잘못 이해하면 base …

따라서 “A는 B의 한 종류다”라는 문장만으로 inheritance를 결정하지 않는다. 공유 behavior가 안정적이고 대체 가능성이 명확한지 본다. 단지 코드를 재사용하고 싶다면 composition이나 독립 helper가 더 느슨한 결합을 만들 수 있다.

---

## CHAPTER 07 · composition은 필요한 behavior를 객체 관계로 조합한다

### 시작 전 용어집

#### 1. composition

- **뜻:** Composition은 객체가 다른 객체를 내부에 가지고 그 기능을 위임하는 방식이다.
- **왜 중요한가:** `OrderService`가 `PaymentGateway`, `InventoryRepository`, `Clock`을 받아 사용하는 구조에서는 각 dependency의 역할이 명시적이다.
- **예시:** class OrderService: / def __init__(self, inventory, payment, clock):

#### 2. behavior

- **뜻:** 외부 behavior를 교체하거나 test double로 바꿀 필요가 있고 “has-a” 관계가 자연스럽다면 composition이 강하다.
- **왜 중요한가:** 공통 interface와 대체 가능성을 표현해야 한다면 inheritance 또는 protocol이 적합할 수 있다.
- **예시:** class OrderService: / def __init__(self, inventory, payment, clock):

#### 3. 객체

- **뜻:** Dependency를 constructor에 명시하면 객체 생성 시 필요한 협력 관계가 드러난다.
- **왜 중요한가:** Composition이 무조건 단순한 것은 아니다.
- **예시:** class OrderService: / def __init__(self, inventory, payment, clock):

#### 4. clock

- **뜻:** 상속처럼 “하나의 종류” 관계를 만들지 않고 “이 기능을 사용한다”는 관계를 표현한다.
- **왜 중요한가:** 이 구조는 test에서 실제 network gateway 대신 fake implementation을 넣기 쉽다.
- **예시:** class OrderService: / def __init__(self, inventory, payment, clock):

```python
class OrderService:
    def __init__(self, inventory, payment, clock):
        self.inventory = inventory
        self.payment = payment
        self.clock = clock
```

 또한 payment provider를 바꿔도 service의 업무 계약이 유지될 수 있다. 

 작은 operation 하나를 수행하기 위해 object graph가 지나치게 커지면 wiring 비용과 간접 호출이 늘어난다. Dependency injection은 필요한 결합을 명시하는 도구이지 모든 함수 호출을 interface로 감싸는 규칙이 아니다.

Inheritance와 composition 사이의 선택은 reuse 양이 아니라 coupling 방향으로 본다.  

---

## CHAPTER 08 · duck typing과 protocol은 구체 class보다 필요한 capability에 초점을 맞춘다

### 시작 전 용어집

#### 1. duck typing

- **뜻:** 이를 흔히 duck typing 관점으로 설명한다.
- **왜 중요한가:** 이 스타일의 장점은 불필요한 inheritance hierarchy 없이 서로 다른 구현을 같은 코드가 사용할 수 있다는 것이다.
- **예시:** 이를 흔히 duck typing 관점으로 설명한다.

#### 2. protocol

- **뜻:** Type hint의 Protocol 같은 구조를 사용하면 “이 consumer가 요구하는 capability”를 정적으로 설명할 수 있다.
- **왜 중요한가:** Protocol 설계에서는 method 존재만이 아니라 semantics가 중요하다.
- **예시:** Type hint의 Protocol 같은 구조를 사용하면 “이 consumer가 …

#### 3. class

- **뜻:** Python에서는 객체의 구체 class 이름보다 필요한 operation을 제공하는지가 중요한 경우가 많다.
- **왜 중요한가:** read()`를 제공하면 실제 파일, memory buffer, test double이 같은 소비자와 동작할 수 있다.
- **예시:** Python에서는 객체의 구체 class 이름보다 필요한 operation을 제공하는지가 …

#### 4. capability

- **뜻:** Interface를 작게 유지하면 구현자가 불필요한 method를 제공하지 않아도 되고 consumer도 자신에게 필요한 capability만 의존한다.
- **왜 중요한가:** Read-only consumer는 `get()`과 iteration만 요구하고 mutation API를 몰라도 된다.
- **예시:** Interface를 작게 유지하면 구현자가 불필요한 method를 제공하지 않아도 …

File-like object가 `. 

 그러나 필요한 method와 반환 의미가 문서화되지 않으면 runtime에서 늦게 오류가 발생할 수 있다. 

 `save(item)`이라는 method가 있어도 overwrite 규칙, failure, transaction behavior가 다르면 완전한 대체가 아닐 수 있다. Structural typing은 형태를 검사하는 도구이고 업무 계약은 추가 문서와 test로 보호해야 한다.

  좁은 protocol은 coupling을 줄이고 test double 작성도 간단하게 만든다.

---

## CHAPTER 09 · special method는 Python 문법과 사용자 타입을 연결한다

### 시작 전 용어집

#### 1. special method

- **뜻:** `len(obj)`, `obj[key]`, `a + b`, `for x in obj` 같은 Python 문법은 사용자 정의 타입의 special method와 연결될 수 있다.
- **왜 중요한가:** `__len__`, `__getitem__`, `__iter__`, `__eq__` 같은 protocol을 구현하면 객체가 언어의 기존 operation과 자연스럽게 통합된다.
- **예시:** `len(obj)`, `obj[key]`, `a + b`, `for x in …

#### 2. Python

- **뜻:** 이 기능은 operator를 마음대로 재정의하는 장식이 아니다.
- **왜 중요한가:** `len(obj)`가 호출할 때마다 network에 접속하거나 값이 음수가 되는 식의 behavior는 호출자의 상식을 깨뜨린다.
- **예시:** 이 기능은 operator를 마음대로 재정의하는 장식이 아니다.

#### 3. 타입

- **뜻:** `__eq__`와 `__hash__` 관계, iterator의 종료 protocol처럼 언어가 요구하는 계약도 지켜야 한다.
- **왜 중요한가:** `__repr__`은 디버깅 표현에 중요하지만 secret을 포함하지 않도록 주의한다.
- **예시:** `__eq__`와 `__hash__` 관계, iterator의 종료 protocol처럼 언어가 요구하는 …

#### 4. protocol

- **뜻:** Special method를 구현할 때는 해당 protocol을 완전히 이해한 뒤 최소한으로 사용한다.
- **왜 중요한가:** 도메인 개념이 이미 연산자 의미와 자연스럽게 맞는 `Money + Money` 같은 경우는 읽기 좋을 수 있지만, 전혀 다른 operation을 `+`에 숨기면 코드가 짧아도 의미가 불투명해진다.
- **예시:** Special method를 구현할 때는 해당 protocol을 완전히 이해한 …

기존 문법이 가진 기대를 지켜야 한다.  

 객체 전체 field를 자동으로 출력하는 dataclass representation이 credential이나 개인 정보를 로그에 남길 수 있다. 개발자 편의와 정보 노출 정책을 함께 본다.

 

---

## CHAPTER 10 · object boundary는 concurrency와 persistence까지 확장된다

### 시작 전 용어집

#### 1. object boundary

- **뜻:** 메모리 안에서 잘 설계된 객체도 database에 저장되거나 여러 thread/task에서 공유되면 새로운 문제가 생긴다.
- **왜 중요한가:** 객체의 method가 invariant를 지켜도 두 실행 흐름이 동시에 오래된 상태를 읽고 각각 update하면 lost update가 발생할 수 있다.
- **예시:** 메모리 안에서 잘 설계된 객체도 database에 저장되거나 여러 …

#### 2. concurrency

- **뜻:** In-memory encapsulation만으로 distributed state consistency를 보장할 수는 없다.
- **왜 중요한가:** Persistence를 위해 object를 직렬화할 때도 class 내부 구조를 그대로 DB schema나 wire format에 노출하면 version 변경이 어려워진다.
- **예시:** In-memory encapsulation만으로 distributed state consistency를 보장할 수는 없다.

#### 3. persistence

- **뜻:** Domain object와 persistence representation 사이에 mapping 경계를 두면 내부 model을 바꾸면서 migration을 관리할 수 있다.
- **왜 중요한가:** Identity와 equality 규칙도 저장된 ID와 연결되어야 한다.
- **예시:** Domain object와 persistence representation 사이에 mapping 경계를 두면 …

#### 4. 객체

- **뜻:** 객체지향 설계의 범위는 class syntax에서 끝나지 않는다.
- **왜 중요한가:** 어떤 상태를 객체가 소유하고, 누가 변경할 수 있으며, 외부 저장소와 어떻게 동기화하고, 어떤 실패 뒤에 invariant를 유지하는지가 실제 설계다.
- **예시:** 객체지향 설계의 범위는 class syntax에서 끝나지 않는다.

Mutable object를 여러 실행 단위가 공유한다면 lock, message passing, immutable snapshot 같은 synchronization 전략이 필요하다. Object method가 thread-safe인지 여부는 method 이름만으로 알 수 없으므로 library나 service boundary에서 contract를 명시한다. 공유를 피할 수 있다면 ownership을 한 실행 단위에 두는 편이 단순하다.

  Class는 이 규칙을 표현하는 하나의 언어 도구이고, 필요하면 함수·immutable data·protocol과 함께 조합한다.
