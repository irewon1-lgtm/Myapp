# PART 80 · Dataclass generated semantics — field·default·equality·frozen·slots 계약을 읽기

`@dataclass`는 boilerplate를 줄여 주지만 단순 코드 생성 장식자가 아니다. Field 정의를 바탕으로 `__init__`, `__repr__`, equality, ordering, hash, pattern matching 관련 동작을 만들 수 있다. 옵션 조합을 이해하지 못하면 mutable default 공유, frozen 객체의 잘못된 hash 기대, inheritance field order 문제를 만들 수 있다. 이 절에서는 **무엇이 자동 생성되고 그 생성이 어떤 의미를 약속하는지**를 본다.

---

## CHAPTER 01 · field model은 annotation과 default를 instance state schema로 해석한다

### 시작 전 용어집

#### 1. field model

- **뜻:** Dataclass는 type annotation이 붙은 class attribute를 field 정의로 사용한다.
- **왜 중요한가:** 이 정의에서 field order는 생성된 initializer와 representation에 영향을 줄 수 있다.
- **예시:** from dataclasses import dataclass / @dataclass

#### 2. annotation

- **뜻:** Annotation은 runtime 강제 type validation이 아니므로 `name=123`을 자동 거부한다고 생각하면 안 된다.
- **왜 중요한가:** Field metadata와 `init=False`, `repr=False`, `compare=False` 같은 옵션은 해당 field가 어떤 자동 생성 동작에 참여할지 바꾼다.
- **예시:** from dataclasses import dataclass / @dataclass

```python
from dataclasses import dataclass

@dataclass
class User:
    id: int
    name: str
```

 

 Domain 의미와 생성 옵션을 맞춘다.

---

**실행 점검 80-1 — CHAPTER 01 · field model은 annotation과 default를 instance state schema로 해석한다**
CHAPTER 01 · field model은 annotation과 default를 instance state schema로 해석한다의 규칙은 작은 예제로 값을 움직여 보면 분명해진다. PART 80 CHAPTER 1에서는 작업 하나만 남겨 단순화한 뒤 실행 전 예상값을 적고, 값이나 호출 순서 하나만 바꿔 실제 결과를 비교한다. 중복 처리 여부와 누락 여부를 동시에 점검한다. 예상과 다르면 타입·객체 정체성·수명·예외 경계를 한 항목씩 좁혀 원인을 찾는다. 수정한 뒤에는 원래 사례와 반대 조건 사례를 모두 실행하고, CHAPTER 01 · field model은 annotation과 default를 instance state schema로 해석한다이 어떤 상태 변화를 만들었는지 한 문장으로 설명한다. 다른 코드에서도 같은 규칙을 알아볼 수 있으면 이해가 실제로 연결된 것이다.
## CHAPTER 02 · mutable default는 `default_factory`로 instance별 새 객체를 만든다

### 시작 전 용어집

#### 1. mutable

- **뜻:** List나 dict처럼 mutable object를 class 정의 시 하나 만들어 default로 공유하면 instance 간 state가 섞일 수 있다.
- **왜 중요한가:** Dataclass는 이런 위험을 줄이기 위해 `default_factory` 패턴을 제공한다.
- **예시:** from dataclasses import dataclass, field / @dataclass

#### 2. default_factory

- **뜻:** 각 instance마다 factory가 호출되어 독립 list가 생긴다.
- **왜 중요한가:** Factory 자체가 global mutable object를 반환하면 다시 공유되므로 “factory 사용”보다 **새 ownership을 만든다**는 의미가 중요하다.
- **예시:** from dataclasses import dataclass, field / @dataclass

#### 3. instance

- **뜻:** 시간, random ID 같은 값도 definition time에 계산할지 instance construction time에 계산할지 구분한다.
- **예시:** from dataclasses import dataclass, field / @dataclass

```python
from dataclasses import dataclass, field

@dataclass
class Basket:
    items: list[str] = field(default_factory=list)
```

 


---

## CHAPTER 03 · generated equality와 ordering은 field order와 compare 옵션에 의존한다

### 시작 전 용어집

#### 1. generated equality

- **뜻:** 기본 equality가 활성화되면 dataclass field 값들의 조합으로 같은 class instance를 비교하는 형태가 생성될 수 있다.
- **왜 중요한가:** Ordering 옵션을 켜면 field tuple 순서가 ordering 의미로 이어질 수 있다.
- **예시:** @dataclass(order=True) / class Version:

#### 2. ordering

- **뜻:** Metadata field가 ordering에 들어가면 예상치 못한 정렬이 생길 수 있어 `compare=False`가 필요할 수 있다.
- **왜 중요한가:** 자동 생성이 편하다는 이유로 total ordering을 켜지 않는다.
- **예시:** @dataclass(order=True) / class Version:

#### 3. field order

- **뜻:** 이 경우 `(major, minor)`의 lexicographic order가 도메인 version order와 맞는지 확인한다.
- **왜 중요한가:** 도메인에 자연스러운 ordering이 없으면 equality만 제공하는 편이 낫다.
- **예시:** @dataclass(order=True) / class Version:

```python
@dataclass(order=True)
class Version:
    major: int
    minor: int
```

 

 

---

## CHAPTER 04 · `frozen=True`는 assignment를 막지만 깊은 immutable을 자동 보장하지 않는다

### 시작 전 용어집

#### 1. frozen

- **뜻:** Frozen dataclass는 field rebinding을 제한해 value object 의도를 표현한다.
- **왜 중요한가:** 하지만 field 안에 mutable list나 dict가 들어 있다면 그 내부 object까지 자동으로 immutable이 되는 것은 아니다.
- **예시:** @dataclass(frozen=True) / class Config:

#### 2. True

- **뜻:** Frozen은 object graph 전체가 아니라 dataclass attribute assignment boundary에 가깝다.
- **왜 중요한가:** Hash key로 쓰려면 nested field의 hashability와 실제 mutability도 확인한다.
- **예시:** @dataclass(frozen=True) / class Config:

#### 3. assignment

- **뜻:** “frozen이니 완전히 안전한 immutable”이라고 단정하지 않는다.
- **예시:** @dataclass(frozen=True) / class Config:

```python
@dataclass(frozen=True)
class Config:
    name: str
    tags: tuple[str, ...]
```

 

 

---

**실행 점검 80-4 — CHAPTER 04 · `frozen=True`는 assignment를 막지만 깊은 immutable을 자동 보장하지 않는다**
CHAPTER 04 · `frozen=True`는 assignment를 막지만 깊은 immutable을 자동 보장하지 않는다의 규칙은 작은 예제로 값을 움직여 보면 분명해진다. PART 80 CHAPTER 4에서는 로그에 남길 관찰값을 고르고 실행 전 예상값을 적고, 값이나 호출 순서 하나만 바꿔 실제 결과를 비교한다. 복구 지점과 실제 데이터 위치가 맞는지 비교한다. 예상과 다르면 타입·객체 정체성·수명·예외 경계를 한 항목씩 좁혀 원인을 찾는다. 수정한 뒤에는 원래 사례와 반대 조건 사례를 모두 실행하고, CHAPTER 04 · `frozen=True`는 assignment를 막지만 깊은 immutable을 자동 보장하지 않는다이 어떤 상태 변화를 만들었는지 한 문장으로 설명한다. 다른 코드에서도 같은 규칙을 알아볼 수 있으면 이해가 실제로 연결된 것이다.
## CHAPTER 05 · hash policy는 equality와 frozen 상태의 조합을 반영한다

### 시작 전 용어집

#### 1. hash

- **뜻:** Dataclass의 hash 생성 정책은 `eq`, `frozen`, `unsafe_hash` 같은 옵션과 연결된다.
- **왜 중요한가:** 이는 mutable object가 value hash를 갖는 위험을 줄이기 위한 보수적 설계다.
- **예시:** `unsafe_hash=True`를 “hash가 필요하니 켜는 옵션”으로 사용하면 안 된다.

#### 2. equality

- **뜻:** Equality에 참여하는 field가 나중에 바뀔 수 있다면 dict/set key 불변식이 깨진다.
- **왜 중요한가:** 정말 immutable value object인지, identity-based object인지 먼저 정하고 hash policy를 선택한다.
- **예시:** `unsafe_hash=True`를 “hash가 필요하니 켜는 옵션”으로 사용하면 안 된다.

#### 3. frozen

- **뜻:** `unsafe_hash=True`를 “hash가 필요하니 켜는 옵션”으로 사용하면 안 된다.
- **왜 중요한가:** Cache key가 필요하다는 이유만으로 domain object 전체를 hashable하게 만드는 대신 별도 immutable key를 만들 수도 있다.
- **예시:** `unsafe_hash=True`를 “hash가 필요하니 켜는 옵션”으로 사용하면 안 된다.

---

## CHAPTER 06 · `__post_init__`은 생성된 initializer 뒤에서 invariant를 완성한다

### 시작 전 용어집

#### 1. __post_init__

- **뜻:** Dataclass가 자동 생성한 `__init__` 뒤에 추가 validation이나 derived field 계산이 필요하면 `__post_init__`을 사용할 수 있다.
- **왜 중요한가:** Derived field를 계산할 때 source field와 consistency를 유지해야 한다.
- **예시:** @dataclass / class Range:

#### 2. initializer

- **뜻:** Mutable object에서 source field를 나중에 바꿀 수 있다면 derived cache가 stale해질 수 있다.
- **왜 중요한가:** Complex I/O나 remote validation을 `__post_init__`에 숨기지 않는다.
- **예시:** @dataclass / class Range:

#### 3. invariant

- **뜻:** Local invariant와 workflow-level validation을 구분한다.
- **예시:** @dataclass / class Range:

```python
@dataclass
class Range:
    start: int
    end: int

    def __post_init__(self):
        if self.start > self.end:
            raise ValueError("invalid range")
```

 

 

---

**실행 점검 80-6 — CHAPTER 06 · `__post_init__`은 생성된 initializer 뒤에서 invariant를 완성한다**
CHAPTER 06 · `__post_init__`은 생성된 initializer 뒤에서 invariant를 완성한다의 규칙은 작은 예제로 값을 움직여 보면 분명해진다. PART 80 CHAPTER 6에서는 재현 가능한 최소 사례를 만들고 실행 전 예상값을 적고, 값이나 호출 순서 하나만 바꿔 실제 결과를 비교한다. 처리 시작 시각과 종료 시각을 같이 남긴다. 예상과 다르면 타입·객체 정체성·수명·예외 경계를 한 항목씩 좁혀 원인을 찾는다. 수정한 뒤에는 원래 사례와 반대 조건 사례를 모두 실행하고, CHAPTER 06 · `__post_init__`은 생성된 initializer 뒤에서 invariant를 완성한다이 어떤 상태 변화를 만들었는지 한 문장으로 설명한다. 다른 코드에서도 같은 규칙을 알아볼 수 있으면 이해가 실제로 연결된 것이다.
## CHAPTER 07 · slots와 match args 옵션은 memory layout과 pattern surface를 바꿀 수 있다

### 시작 전 용어집

#### 1. slots

- **뜻:** Dataclass가 slots를 생성하도록 하면 instance dictionary를 줄이고 허용 attribute set을 좁힐 수 있다.
- **왜 중요한가:** 하지만 inheritance와 weak reference compatibility를 함께 검토해야 한다.
- **예시:** Dataclass가 slots를 생성하도록 하면 instance dictionary를 줄이고 허용 …

#### 2. match args

- **뜻:** Pattern matching에 사용되는 positional class pattern surface도 generated metadata의 영향을 받을 수 있다.
- **왜 중요한가:** Field order를 바꾸는 refactoring이 match behavior에 영향을 줄 수 있다는 뜻이다.
- **예시:** Pattern matching에 사용되는 positional class pattern surface도 generated …

#### 3. memory layout

- **뜻:** Public API에서 positional pattern에 강하게 의존하게 할지 named attribute pattern을 권장할지 결정한다.
- **왜 중요한가:** 자동 생성된 convenience가 장기 compatibility surface가 될 수 있다.
- **예시:** Public API에서 positional pattern에 강하게 의존하게 할지 named …

---

## CHAPTER 08 · dataclass contract는 boilerplate 절감보다 생성된 의미의 적합성을 검증한다

### 시작 전 용어집

#### 1. dataclass

- **뜻:** Dataclass를 쓸 때는 “코드가 짧아졌다”가 끝이 아니다.
- **왜 중요한가:** 어떤 field가 init, repr, compare, hash에 참여하는지 표로 확인하고, frozen/slots/order 옵션이 domain object의 identity와 lifecycle에 맞는지 검토한다.
- **예시:** 테스트에서는 mutable default isolation, equality/hash 불변식, post-init failure, …

#### 2. boilerplate

- **뜻:** 테스트에서는 mutable default isolation, equality/hash 불변식, post-init failure, inheritance field 구성, slots에서 예상하지 못한 attribute assignment를 확인한다.
- **왜 중요한가:** 이 PART의 핵심은 **dataclass를 단순 코드 생성기로 보지 않고, field schema에서 constructor·representation·comparison·hash·layout 의미를 자동 파생하는 계약 생성 도구로 이해하는 것**이다.
- **예시:** 테스트에서는 mutable default isolation, equality/hash 불변식, post-init failure, …

---

## 실전 학습 루프 · dataclass generated semantics

### 1. 쉬운 예

`@dataclass`는 단순히 타이핑을 줄이는 장식이 아니다. field 정의를 바탕으로 init, repr, equality, ordering, hash 같은 동작을 생성하며 옵션 조합에 따라 객체 의미가 달라진다.

### 2. 한 줄 해석

dataclass를 쓸 때는 생성되는 메서드 목록보다 equality·mutability·hash 계약을 먼저 결정한다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상한다. 그 다음 아래 최소 예제를 실행하고, 예상이 틀렸다면 **호출 순서와 상태 변화**를 표시한다.

```python
from dataclasses import dataclass

@dataclass(frozen=True)
class Point:
    x: int
    y: int

print(Point(1, 2) == Point(1, 2))
```

### 4. 수정 실습

1. `frozen=True`를 제거하고 hash 가능성 변화를 확인한다.
2. `compare=False` field를 추가해 equality 의미가 어떻게 달라지는지 본다.

수정 후에는 정상 예제만 다시 보지 말고 실패·경계·반복 호출 중 하나를 추가해 계약이 유지되는지 확인한다.

### 5. 확인 문제

dataclass면 자동으로 immutable하고 hashable할까?

### 6. 정답과 오답 설명

**정답:** 아니다. frozen, eq, unsafe_hash 등 옵션과 field 타입에 따라 계약이 달라진다.

**자주 나오는 오답:** “boilerplate를 줄인다”만 기억하면 생성된 equality/hash가 도메인 의미와 맞는지 놓치게 된다.

이 PART를 마칠 때는 해당 문법 이름을 외우는 데서 멈추지 말고 **언제 호출되는가 / 무엇을 읽거나 바꾸는가 / 실패하면 어디로 가는가** 세 문장으로 설명한다.

## 현장 디버깅 체크 · dataclass generated semantics

### 증상에서 시작한다

field 하나를 추가한 뒤 equality·ordering·hash 결과가 달라져 dict/set key 동작이 깨진다. 이때 문법을 먼저 고치면 원인이 가려질 수 있다. 재현 입력과 실제 상태를 보존한 뒤 **어느 경계에서 처음 기대와 달라졌는지**를 찾는다.

### 먼저 볼 증거

dataclass 옵션과 각 field의 compare/hash/init 설정, 생성된 special method를 확인한다. 최종 출력 하나만 보지 말고 호출 전 값, 호출 뒤 값, 예외 또는 resource 상태를 나란히 두면 원인 후보가 급격히 줄어든다.

### 일부러 실패시켜 보기

mutable field를 equality/hash에 포함하거나 제외한 두 버전을 만들어 container 동작을 비교한다. 정상 예제만 통과시키는 것은 검증이 아니다. 경계 조건을 강제로 만들고 같은 증상이 반복되는지 확인해야 수정 전후를 비교할 수 있다.

### 통과 기준

도메인에서 같은 객체라고 보는 기준과 dataclass가 생성한 equality/hash 계약이 정확히 일치해야 한다. 이 기준을 테스트 이름과 assertion으로 옮기면 이후 refactoring에서도 같은 오류가 돌아오는지 자동으로 잡을 수 있다.

