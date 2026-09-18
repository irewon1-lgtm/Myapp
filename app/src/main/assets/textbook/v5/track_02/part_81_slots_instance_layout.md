# PART 81 · `__slots__`와 instance layout — attribute storage·weakref·상속 비용을 구조적으로 이해하기

일반 Python instance는 흔히 per-instance dictionary를 통해 동적 attribute를 저장한다. `__slots__`는 이 기본 모델을 바꾸어 특정 attribute storage를 class layout에 고정할 수 있다. 메모리를 줄이는 용도로 유명하지만 실제 의미는 더 넓다. 동적 attribute 허용 범위, weak reference 가능성, inheritance layout, descriptor behavior가 함께 바뀔 수 있다. 이 절에서는 **메모리 절약 팁이 아니라 object layout 계약**으로 `__slots__`를 읽는다.

---

## CHAPTER 01 · slot storage는 instance dictionary 대신 고정된 attribute 위치를 만든다

### 시작 전 용어집

#### 1. slot storage

- **뜻:** 이 class의 instance는 일반적인 임의 attribute dictionary 대신 선언된 slot을 통해 `x`, `y`를 저장한다.
- **왜 중요한가:** Slot name은 class 수준 descriptor와 연결되어 attribute protocol 안에서 동작한다.
- **예시:** class Point: / __slots__ = ("x", "y")

#### 2. instance dictionary

- **뜻:** 즉 `__slots__`는 “dict를 조금 더 빠르게 만든다”가 아니다.
- **왜 중요한가:** Instance state representation 자체를 바꾸는 선택이다.
- **예시:** class Point: / __slots__ = ("x", "y")

#### 3. attribute

- **뜻:** 따라서 serialization, reflection, subclassing tool이 `__dict__` 존재를 가정한다면 영향을 받을 수 있다.
- **예시:** class Point: / __slots__ = ("x", "y")

```python
class Point:
    __slots__ = ("x", "y")

    def __init__(self, x, y):
        self.x = x
        self.y = y
```

 

  

---

**직접 확인하기 — CHAPTER 01 · slot storage는 instance dictionary 대신 고정된 attribute 위치를 만든다**
CHAPTER 01 · slot storage는 instance dictionary 대신 고정된 attribute 위치를 만든다의 동작은 가장 작은 실행 예제로 규칙을 확인하면 훨씬 명확해진다. 먼저 입력이나 객체 하나만 두고 기대 결과를 적은 다음 실행한다. 그다음 값 하나 또는 호출 순서 하나만 바꿔 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 01 · slot storage는 instance dictionary 대신 고정된 attribute 위치를 만든다의 규칙이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 좁히고, 수정 뒤 원래 예제와 반대 조건 예제를 모두 다시 실행한다. 결과를 자기 문장으로 설명할 수 있어야 문법을 복사한 것이 아니라 동작 원리를 이해한 것이다.
## CHAPTER 02 · `__dict__` 부재는 임의 attribute 추가를 제한하지만 완전한 immutable을 만들지는 않는다

### 시작 전 용어집

#### 1. __dict__

- **뜻:** Slot class에 `__dict__`를 별도로 허용하지 않으면 선언되지 않은 attribute assignment가 실패할 수 있다.
- **왜 중요한가:** 이는 typo를 빨리 잡는 데 도움이 될 수 있지만 object 자체가 immutable이라는 뜻은 아니다.
- **예시:** p = Point(1, 2) / # p.label = …

#### 2. attribute

- **뜻:** Attribute surface를 좁히는 효과가 애플리케이션에 맞는지 확인한다.
- **왜 중요한가:** x = 10`처럼 기존 slot은 여전히 변경 가능하다.
- **예시:** p = Point(1, 2) / # p.label = …

#### 3. immutable

- **뜻:** Dynamic plugin metadata를 instance에 임의로 붙이는 framework와 함께 쓴다면 compatibility 문제가 생길 수 있다.
- **예시:** p = Point(1, 2) / # p.label = …

```python
p = Point(1, 2)
# p.label = "A"  -> 선언되지 않은 slot이면 실패할 수 있다.
```

 `p.

 

---

**직접 확인하기 — CHAPTER 02 · `__dict__` 부재는 임의 attribute 추가를 제한하지만 완전한 immutable을 만들지는 않는다**
CHAPTER 02 · `__dict__` 부재는 임의 attribute 추가를 제한하지만 완전한 immutable을 만들지는 않는다의 동작은 가장 작은 실행 예제로 규칙을 확인하면 훨씬 명확해진다. 먼저 입력이나 객체 하나만 두고 기대 결과를 적은 다음 실행한다. 그다음 값 하나 또는 호출 순서 하나만 바꿔 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 02 · `__dict__` 부재는 임의 attribute 추가를 제한하지만 완전한 immutable을 만들지는 않는다의 규칙이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 좁히고, 수정 뒤 원래 예제와 반대 조건 예제를 모두 다시 실행한다. 결과를 자기 문장으로 설명할 수 있어야 문법을 복사한 것이 아니라 동작 원리를 이해한 것이다.
## CHAPTER 03 · inheritance에서는 base와 subclass의 slot 구성을 함께 봐야 한다

### 시작 전 용어집

#### 1. inheritance

- **뜻:** Multiple inheritance와 layout compatibility는 더 복잡해질 수 있다.
- **왜 중요한가:** `__slots__`를 hierarchy-wide design으로 보지 않고 한 class의 micro-optimization으로 넣으면 나중에 상속 확장이 어려워질 수 있다.
- **예시:** Slot name을 상속 계층에서 중복 정의하면 예상하기 어려운 …

#### 2. base

- **뜻:** Base class가 slots를 사용한다고 해서 모든 subclass가 자동으로 dictionary 없는 layout을 유지하는 것은 아니다.
- **왜 중요한가:** Subclass가 일반 class처럼 정의되면 instance dictionary가 다시 생길 수 있는 등 hierarchy 전체 구성이 중요하다.
- **예시:** Slot name을 상속 계층에서 중복 정의하면 예상하기 어려운 …

#### 3. class

- **뜻:** Base field를 확장하려면 subclass는 자신이 새로 추가하는 slot만 선언하는 식으로 구조를 명확히 한다.
- **왜 중요한가:** Slot name을 상속 계층에서 중복 정의하면 예상하기 어려운 shadowing과 descriptor relation을 만들 수 있으므로 피한다.
- **예시:** Base field를 확장하려면 subclass는 자신이 새로 추가하는 slot만 …

---

## CHAPTER 04 · weak reference가 필요하면 slot layout에서 명시적으로 고려해야 한다

### 시작 전 용어집

#### 1. weak reference

- **뜻:** 일반 instance는 weak reference 대상이 될 수 있는 경우가 많지만 slots를 사용하는 class는 weak reference 지원을 별도로 고려해야 한다.
- **왜 중요한가:** Observer registry, cache, finalizer가 weakref를 사용한다면 class layout 변경 뒤 동작을 검증한다.
- **예시:** 일반 instance는 weak reference 대상이 될 수 있는 …

#### 2. slot layout

- **뜻:** Object를 weak key/value cache에 넣던 코드가 slots 적용 뒤 갑자기 실패한다면 memory optimization이 lifecycle architecture를 깨뜨린 것이다.
- **왜 중요한가:** 따라서 slots migration 전에는 해당 type이 weakref, debugging tool, serializer에 사용되는지 검색한다.
- **예시:** Object를 weak key/value cache에 넣던 코드가 slots 적용 …

#### 3. slots

- **뜻:** Memory 절약 수치만 비교해서 결정하지 않는다.
- **예시:** Memory 절약 수치만 비교해서 결정하지 않는다.

---

## CHAPTER 05 · slot은 attribute lookup protocol 안에서 descriptor 형태로 참여한다

### 시작 전 용어집

#### 1. slot

- **뜻:** Slot field는 raw array index처럼 language 바깥에서 직접 접근되는 것이 아니라 Python attribute model에 통합된다.
- **왜 중요한가:** Class dictionary에서 slot-related descriptor를 관찰할 수 있고 `obj.
- **예시:** Slot field는 raw array index처럼 language 바깥에서 직접 …

#### 2. attribute lookup

- **뜻:** x`는 정상 attribute lookup 규칙을 따른다.
- **왜 중요한가:** 이 점은 P67의 descriptor precedence와 연결된다.
- **예시:** x`는 정상 attribute lookup 규칙을 따른다.

#### 3. protocol

- **뜻:** Slot과 property를 같은 이름으로 무리하게 조합하거나 custom `__getattribute__`가 slot access를 가로챌 때 전체 lookup route를 이해해야 한다.
- **왜 중요한가:** 저장 방식 최적화와 public attribute semantics는 분리한다.
- **예시:** Slot과 property를 같은 이름으로 무리하게 조합하거나 custom `__getattribute__`가 …

#### 4. descriptor

- **뜻:** Caller는 implementation이 dict인지 slot인지 몰라도 같은 contract를 사용할 수 있어야 한다.
- **예시:** Caller는 implementation이 dict인지 slot인지 몰라도 같은 contract를 사용할 …

---

## CHAPTER 06 · memory trade-off는 instance 수와 field 수를 실제 측정해 판단한다

### 시작 전 용어집

#### 1. memory trade-off

- **뜻:** 수백만 개의 작은 객체에서는 per-instance dictionary overhead 절감이 의미 있을 수 있다.
- **왜 중요한가:** 반대로 수십 개의 service object에 slots를 적용해도 전체 application memory에는 영향이 거의 없을 수 있다.
- **예시:** 수백만 개의 작은 객체에서는 per-instance dictionary overhead 절감이 …

#### 2. instance

- **뜻:** Instance count, retained size, allocation rate를 함께 보고 readability와 extension cost보다 이득이 큰지 판단한다.
- **왜 중요한가:** 또 slots를 사용한다고 모든 memory가 사라지는 것은 아니다.
- **예시:** Instance count, retained size, allocation rate를 함께 보고 …

#### 3. field

- **뜻:** Field가 가리키는 문자열, list, child object의 memory는 그대로 존재한다.
- **왜 중요한가:** Object header와 storage representation 일부가 달라지는 것이다.
- **예시:** Field가 가리키는 문자열, list, child object의 memory는 그대로 …

#### 4. 객체

- **뜻:** Memory optimization은 representative workload에서 측정한다.
- **예시:** Memory optimization은 representative workload에서 측정한다.

---

## CHAPTER 07 · dynamic attribute가 필요한 타입과 fixed schema 타입을 구분한다

### 시작 전 용어집

#### 1. dynamic attribute

- **뜻:** Domain value object처럼 field schema가 안정된 타입은 slots와 잘 맞을 수 있다.
- **왜 중요한가:** 반면 plugin이 arbitrary metadata를 추가하거나 interactive exploration에서 임시 attribute를 자주 붙이는 타입에는 제약이 될 수 있다.
- **예시:** Domain value object처럼 field schema가 안정된 타입은 slots와 …

#### 2. 타입

- **뜻:** , "__dict__")`로 동적 attribute를 다시 허용할 수도 있지만 그러면 메모리 절감과 schema 제한 목적 일부가 약해진다.
- **왜 중요한가:** 왜 slots를 도입했는지 목적을 먼저 명확히 한다.
- **예시:** , "__dict__")`로 동적 attribute를 다시 허용할 수도 있지만 …

#### 3. fixed schema

- **뜻:** API compatibility 관점에서도 사용자가 subclass를 만들 수 있는 public class라면 slots 변경이 downstream subclass layout과 behavior를 바꿀 수 있다.
- **예시:** API compatibility 관점에서도 사용자가 subclass를 만들 수 있는 …

`__slots__ = (... 


---

## CHAPTER 08 · slots contract는 layout 최적화와 확장성 비용을 함께 기록한다

### 시작 전 용어집

#### 1. slots

- **뜻:** `__slots__` 도입 전에는 세 가지를 확인한다.
- **왜 중요한가:** Instance가 매우 많은가, dynamic attribute가 필요한가, weakref·serializer·subclassing ecosystem과 충돌하지 않는가.
- **예시:** `__slots__` 도입 전에는 세 가지를 확인한다.

#### 2. layout

- **뜻:** 이 PART의 핵심은 **`__slots__`를 메모리 절약 문법으로만 보지 않고, instance storage와 attribute extensibility를 함께 바꾸는 object layout 계약으로 이해하는 것**이다.
- **왜 중요한가:** Test에서는 선언된 field 접근, unknown attribute assignment, subclass instance, weak reference 사용, serialization path를 확인한다.
- **예시:** 이 PART의 핵심은 **`__slots__`를 메모리 절약 문법으로만 보지 …

#### 3. class

- **뜻:** getsizeof` 하나보다 실제 retained memory와 workload를 측정한다.
- **예시:** getsizeof` 하나보다 실제 retained memory와 workload를 측정한다.

Benchmark는 synthetic `sys.

---

## 실전 학습 루프 · slots와 instance layout

### 1. 쉬운 예

일반 instance는 흔히 속성 저장을 위한 dict를 갖지만 `__slots__`는 허용 속성과 저장 layout을 바꿀 수 있다. 메모리 절감 가능성은 있지만 상속·weakref·동적 속성 요구와 함께 검토해야 한다.

### 2. 한 줄 해석

slots는 성능 스위치가 아니라 객체 layout과 허용되는 속성 계약을 바꾸는 기능이다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상한다. 그 다음 아래 최소 예제를 실행하고, 예상이 틀렸다면 **호출 순서와 상태 변화**를 표시한다.

```python
class Pixel:
    __slots__ = ('x', 'y')
    def __init__(self, x, y):
        self.x, self.y = x, y

p = Pixel(1, 2)
print(p.x, p.y)
```

### 4. 수정 실습

1. 정의되지 않은 `p.z`를 대입해 동적 속성 제한을 확인한다.
2. slots class가 필요한 weak reference를 지원하려면 무엇을 추가해야 하는지 확인한다.

수정 후에는 정상 예제만 다시 보지 말고 실패·경계·반복 호출 중 하나를 추가해 계약이 유지되는지 확인한다.

### 5. 확인 문제

`__slots__`를 넣으면 모든 경우에 객체가 더 빠르고 작아질까?

### 6. 정답과 오답 설명

**정답:** 항상 그렇지 않다. 실제 workload와 상속 구조, runtime 구현을 측정해야 한다.

**자주 나오는 오답:** 메모리 절약 가능성을 곧바로 전체 애플리케이션 성능 향상으로 확대 해석하면 안 된다.

이 PART를 마칠 때는 해당 문법 이름을 외우는 데서 멈추지 말고 **언제 호출되는가 / 무엇을 읽거나 바꾸는가 / 실패하면 어디로 가는가** 세 문장으로 설명한다.

## 현장 디버깅 체크 · slots·instance layout

### 증상에서 시작한다

slots 적용 뒤 serialization·weakref·상속 코드가 깨지거나 기대한 만큼 memory가 줄지 않는다. 이때 문법을 먼저 고치면 원인이 가려질 수 있다. 재현 입력과 실제 상태를 보존한 뒤 **어느 경계에서 처음 기대와 달라졌는지**를 찾는다.

### 먼저 볼 증거

instance `__dict__` 존재 여부, slot 목록, base class layout, weakref 요구와 실제 메모리 측정을 함께 본다. 최종 출력 하나만 보지 말고 호출 전 값, 호출 뒤 값, 예외 또는 resource 상태를 나란히 두면 원인 후보가 급격히 줄어든다.

### 일부러 실패시켜 보기

slots subclass와 non-slots base를 조합해 동적 속성이 다시 생기는 경우를 확인한다. 정상 예제만 통과시키는 것은 검증이 아니다. 경계 조건을 강제로 만들고 같은 증상이 반복되는지 확인해야 수정 전후를 비교할 수 있다.

### 통과 기준

layout 최적화가 기능 계약을 깨지 않고 실제 측정에서도 의미 있는 이득을 만들 때만 유지한다. 이 기준을 테스트 이름과 assertion으로 옮기면 이후 refactoring에서도 같은 오류가 돌아오는지 자동으로 잡을 수 있다.

