# PART 80 · Dataclass generated semantics — field·default·equality·frozen·slots 계약을 읽기

`@dataclass`는 boilerplate를 줄여 주지만 단순 코드 생성 장식자가 아니다. Field 정의를 바탕으로 `__init__`, `__repr__`, equality, ordering, hash, pattern matching 관련 동작을 만들 수 있다. 옵션 조합을 이해하지 못하면 mutable default 공유, frozen 객체의 잘못된 hash 기대, inheritance field order 문제를 만들 수 있다. 이 PART에서는 **무엇이 자동 생성되고 그 생성이 어떤 의미를 약속하는지**를 본다.

---

## CHAPTER 01 · field model은 annotation과 default를 instance state schema로 해석한다

Dataclass는 type annotation이 붙은 class attribute를 field 정의로 사용한다.

```python
from dataclasses import dataclass

@dataclass
class User:
    id: int
    name: str
```

이 정의에서 field order는 생성된 initializer와 representation에 영향을 줄 수 있다. Annotation은 runtime 강제 type validation이 아니므로 `name=123`을 자동 거부한다고 생각하면 안 된다.

Field metadata와 `init=False`, `repr=False`, `compare=False` 같은 옵션은 해당 field가 어떤 자동 생성 동작에 참여할지 바꾼다. Domain 의미와 생성 옵션을 맞춘다.

---

## CHAPTER 02 · mutable default는 `default_factory`로 instance별 새 객체를 만든다

List나 dict처럼 mutable object를 class 정의 시 하나 만들어 default로 공유하면 instance 간 state가 섞일 수 있다. Dataclass는 이런 위험을 줄이기 위해 `default_factory` 패턴을 제공한다.

```python
from dataclasses import dataclass, field

@dataclass
class Basket:
    items: list[str] = field(default_factory=list)
```

각 instance마다 factory가 호출되어 독립 list가 생긴다. Factory 자체가 global mutable object를 반환하면 다시 공유되므로 “factory 사용”보다 **새 ownership을 만든다**는 의미가 중요하다.

시간, random ID 같은 값도 definition time에 계산할지 instance construction time에 계산할지 구분한다.

---

## CHAPTER 03 · generated equality와 ordering은 field order와 compare 옵션에 의존한다

기본 equality가 활성화되면 dataclass field 값들의 조합으로 같은 class instance를 비교하는 형태가 생성될 수 있다. Ordering 옵션을 켜면 field tuple 순서가 ordering 의미로 이어질 수 있다.

```python
@dataclass(order=True)
class Version:
    major: int
    minor: int
```

이 경우 `(major, minor)`의 lexicographic order가 도메인 version order와 맞는지 확인한다. Metadata field가 ordering에 들어가면 예상치 못한 정렬이 생길 수 있어 `compare=False`가 필요할 수 있다.

자동 생성이 편하다는 이유로 total ordering을 켜지 않는다. 도메인에 자연스러운 ordering이 없으면 equality만 제공하는 편이 낫다.

---

## CHAPTER 04 · `frozen=True`는 assignment를 막지만 깊은 immutable을 자동 보장하지 않는다

Frozen dataclass는 field rebinding을 제한해 value object 의도를 표현한다.

```python
@dataclass(frozen=True)
class Config:
    name: str
    tags: tuple[str, ...]
```

하지만 field 안에 mutable list나 dict가 들어 있다면 그 내부 object까지 자동으로 immutable이 되는 것은 아니다. Frozen은 object graph 전체가 아니라 dataclass attribute assignment boundary에 가깝다.

Hash key로 쓰려면 nested field의 hashability와 실제 mutability도 확인한다. “frozen이니 완전히 안전한 immutable”이라고 단정하지 않는다.

---

## CHAPTER 05 · hash policy는 equality와 frozen 상태의 조합을 반영한다

Dataclass의 hash 생성 정책은 `eq`, `frozen`, `unsafe_hash` 같은 옵션과 연결된다. 이는 mutable object가 value hash를 갖는 위험을 줄이기 위한 보수적 설계다.

`unsafe_hash=True`를 “hash가 필요하니 켜는 옵션”으로 사용하면 안 된다. Equality에 참여하는 field가 나중에 바뀔 수 있다면 dict/set key 불변식이 깨진다.

정말 immutable value object인지, identity-based object인지 먼저 정하고 hash policy를 선택한다. Cache key가 필요하다는 이유만으로 domain object 전체를 hashable하게 만드는 대신 별도 immutable key를 만들 수도 있다.

---

## CHAPTER 06 · `__post_init__`은 생성된 initializer 뒤에서 invariant를 완성한다

Dataclass가 자동 생성한 `__init__` 뒤에 추가 validation이나 derived field 계산이 필요하면 `__post_init__`을 사용할 수 있다.

```python
@dataclass
class Range:
    start: int
    end: int

    def __post_init__(self):
        if self.start > self.end:
            raise ValueError("invalid range")
```

Derived field를 계산할 때 source field와 consistency를 유지해야 한다. Mutable object에서 source field를 나중에 바꿀 수 있다면 derived cache가 stale해질 수 있다.

Complex I/O나 remote validation을 `__post_init__`에 숨기지 않는다. Local invariant와 workflow-level validation을 구분한다.

---

## CHAPTER 07 · slots와 match args 옵션은 memory layout과 pattern surface를 바꿀 수 있다

Dataclass가 slots를 생성하도록 하면 instance dictionary를 줄이고 허용 attribute set을 좁힐 수 있다. 하지만 inheritance와 weak reference compatibility를 함께 검토해야 한다.

Pattern matching에 사용되는 positional class pattern surface도 generated metadata의 영향을 받을 수 있다. Field order를 바꾸는 refactoring이 match behavior에 영향을 줄 수 있다는 뜻이다.

Public API에서 positional pattern에 강하게 의존하게 할지 named attribute pattern을 권장할지 결정한다. 자동 생성된 convenience가 장기 compatibility surface가 될 수 있다.

---

## CHAPTER 08 · dataclass contract는 boilerplate 절감보다 생성된 의미의 적합성을 검증한다

Dataclass를 쓸 때는 “코드가 짧아졌다”가 끝이 아니다. 어떤 field가 init, repr, compare, hash에 참여하는지 표로 확인하고, frozen/slots/order 옵션이 domain object의 identity와 lifecycle에 맞는지 검토한다.

테스트에서는 mutable default isolation, equality/hash 불변식, post-init failure, inheritance field 구성, slots에서 예상하지 못한 attribute assignment를 확인한다.

이 PART의 핵심은 **dataclass를 단순 코드 생성기로 보지 않고, field schema에서 constructor·representation·comparison·hash·layout 의미를 자동 파생하는 계약 생성 도구로 이해하는 것**이다.
