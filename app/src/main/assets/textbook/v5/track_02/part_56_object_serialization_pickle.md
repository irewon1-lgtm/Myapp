# PART 56 · Object serialization과 pickle — 객체 복원 편의와 code-execution 경계를 분리하기

Python object graph를 저장했다가 다시 복원하고 싶을 때 pickle 계열 protocol은 class와 reference 관계를 비교적 풍부하게 보존할 수 있다. 그러나 이 편리함은 JSON 같은 data-only format과 전혀 다른 신뢰 모델을 가진다. Deserialization 과정에서 callable을 찾아 실행해 object를 재구성할 수 있기 때문에 untrusted bytes를 단순 데이터로 취급하면 위험하다. 핵심은 **객체 복원 protocol과 외부 데이터 교환 format을 같은 문제로 보지 않고, 신뢰 가능한 내부 snapshot에만 강한 object serialization을 제한하는 것**이다.

---

## CHAPTER 01 · object serialization은 field 값뿐 아니라 object graph 관계를 보존하려 한다

### 시작 전 용어집

#### 1. object serialization

- **뜻:** 일반 JSON serialization은 public field를 tree-like data로 바꾸는 데 집중하지만 Python object serialization은 같은 child object를 여러 곳이 공유하는 reference 관계와 class identity까지 보존하려 할 수 있다.
- **왜 중요한가:** right` 같은 aliasing relation이 복원 후에도 유지되어야 하는지에 따라 단순 nested dict encoding과 의미가 달라진다.
- **예시:** 일반 JSON serialization은 public field를 tree-like data로 바꾸는 …

#### 2. field

- **뜻:** Serializable한 field와 transient resource를 분리하고, 복원 후 새 resource를 다시 구성하는 lifecycle이 필요하다.
- **왜 중요한가:** 이 기능은 process 내부 cache snapshot이나 trusted local state를 저장할 때 편리할 수 있다.
- **예시:** Serializable한 field와 transient resource를 분리하고, 복원 후 새 …

#### 3. object graph

- **뜻:** Object graph에 file handle, lock, socket처럼 process 외부 resource를 가리키는 object가 포함되면 의미 있는 복원이 불가능할 수 있다.
- **왜 중요한가:** 그러나 class module path와 reconstruction logic에 강하게 의존하므로 code refactor 후 과거 data를 읽기 어려워질 수 있다.
- **예시:** Object graph에 file handle, lock, socket처럼 process 외부 …

#### 4. class

- **뜻:** 단순히 “Python 객체를 그대로 저장한다”는 표현 뒤에는 source code version과 runtime 환경이라는 큰 dependency가 있다.
- **예시:** 단순히 “Python 객체를 그대로 저장한다”는 표현 뒤에는 source …

`a.left is a.

  

 

---

## CHAPTER 02 · pickle은 object를 재구성하기 위해 callable과 argument 정보를 표현할 수 있다

### 시작 전 용어집

#### 1. pickle

- **뜻:** Pickle stream은 일반 데이터 값만 나열하는 format이 아니라 특정 global/class/function을 참조하고 그것을 이용해 object를 재구성하는 instruction을 포함할 수 있다.
- **왜 중요한가:** 이 때문에 custom class, shared reference, recursive graph를 복원할 수 있다.
- **예시:** Pickle stream은 일반 데이터 값만 나열하는 format이 아니라 …

#### 2. object

- **뜻:** Object reconstruction protocol도 실행 경로다.
- **왜 중요한가:** 동시에 deserializer가 단순 parser보다 훨씬 강한 권한을 가진다는 뜻이다.
- **예시:** Object reconstruction protocol도 실행 경로다.

#### 3. callable

- **뜻:** 복원 과정은 현재 Python environment에서 module과 이름을 찾는다.
- **왜 중요한가:** Class가 다른 module로 이동하거나 이름이 바뀌면 old pickle이 실패할 수 있다.
- **예시:** 복원 과정은 현재 Python environment에서 module과 이름을 찾는다.

#### 4. argument

- **뜻:** Constructor가 어떤 argument로 다시 호출되는지, `__setstate__` 같은 hook가 어떤 state를 적용하는지 이해하면 “저장된 field만 들어간다”는 단순 모델이 틀릴 수 있음을 알 수 있다.
- **왜 중요한가:** Compatibility layer나 custom unpickler를 만들 수 있지만 장기 storage schema로 사용하면 code layout 변경 자유도가 줄어든다.
- **예시:** Constructor가 어떤 argument로 다시 호출되는지, `__setstate__` 같은 hook가 …

---

## CHAPTER 03 · untrusted pickle은 data validation 문제가 아니라 arbitrary code execution 문제다

### 시작 전 용어집

#### 1. untrusted pickle

- **뜻:** 외부 사용자가 업로드한 pickle, 신뢰할 수 없는 cache server의 값, 인터넷에서 받은 file을 `pickle.
- **왜 중요한가:** loads()`에 넘기면 payload가 reconstruction mechanism을 악용해 process 권한으로 의도하지 않은 callable을 실행시킬 수 있다.
- **예시:** 외부 사용자가 업로드한 pickle, 신뢰할 수 없는 cache …

#### 2. data validation

- **뜻:** Schema validation을 나중에 수행해도 이미 늦을 수 있다.
- **왜 중요한가:** 위험은 object가 만들어진 뒤가 아니라 **역직렬화 자체**에서 발생한다.
- **예시:** Schema validation을 나중에 수행해도 이미 늦을 수 있다.

#### 3. arbitrary code

- **뜻:** 따라서 untrusted boundary에서는 JSON, CBOR, Protobuf처럼 data-only semantics를 가진 format을 선택하고 schema/range/resource validation을 적용한다.
- **왜 중요한가:** “허용 class만 나중에 검사”하는 접근보다 deserializer가 실행할 수 있는 capability 자체를 줄이는 것이 안전하다.
- **예시:** 따라서 untrusted boundary에서는 JSON, CBOR, Protobuf처럼 data-only semantics를 …

#### 4. execution

- **뜻:** Signature나 encryption을 붙이면 변조된 payload를 거부할 수 있지만 key를 가진 trusted producer만 생성한다는 전제가 필요하다.
- **왜 중요한가:** 서명이 있다고 pickle semantics가 data-only로 변하는 것은 아니다.
- **예시:** Signature나 encryption을 붙이면 변조된 payload를 거부할 수 있지만 …

Integrity와 safe deserialization은 다른 보안 속성이다.

---

## CHAPTER 04 · protocol version은 encoding 효율과 reader compatibility를 결정한다

### 시작 전 용어집

#### 1. protocol

- **뜻:** Pickle은 여러 protocol version을 가지며 새 version은 더 효율적인 binary representation이나 새로운 object 형태 지원을 추가할 수 있다.
- **왜 중요한가:** Writer가 최신 protocol로 저장했다고 old Python runtime이 반드시 읽을 수 있는 것은 아니다.
- **예시:** Pickle은 여러 protocol version을 가지며 새 version은 더 …

#### 2. encoding

- **뜻:** `schema_version`을 object state에 별도로 저장하고 migration logic을 두어 runtime encoding과 domain evolution을 분리할 수 있다.
- **왜 중요한가:** Producer와 consumer version이 다르다면 지원 matrix를 확인한다.
- **예시:** `schema_version`을 object state에 별도로 저장하고 migration logic을 두어 …

#### 3. reader compatibility

- **뜻:** Persistent cache처럼 언제든 버려도 되는 data라면 application upgrade 때 cache를 flush하고 새 protocol로 다시 만드는 방식이 간단하다.
- **왜 중요한가:** 반면 장기 보관된 user data라면 runtime upgrade 전 old fixture를 새 version에서 실제로 읽고 semantic invariant를 비교해야 한다.
- **예시:** Persistent cache처럼 언제든 버려도 되는 data라면 application upgrade …

#### 4. pickle

- **뜻:** 같은 pickle protocol 안에서도 class field 의미가 바뀔 수 있다.
- **왜 중요한가:** Protocol number와 application schema version은 별개다.
- **예시:** 같은 pickle protocol 안에서도 class field 의미가 바뀔 …

---

## CHAPTER 05 · custom reduction은 복원 recipe를 직접 정의하므로 최소 capability로 제한한다

### 시작 전 용어집

#### 1. custom reduction

- **뜻:** Custom reduction을 작성할 때 현재 object의 모든 private field를 무조건 저장하지 않는다.
- **왜 중요한가:** 재구성에 필요한 stable value만 선택하고 cache와 open resource는 제외한다.
- **예시:** Custom reduction을 작성할 때 현재 object의 모든 private …

#### 2. recipe

- **뜻:** 일부 object는 default state capture가 적절하지 않아 reduction protocol을 custom할 수 있다.
- **왜 중요한가:** 어떤 callable을 어떤 argument로 실행해 object를 복원할지 정의할 수 있어 compact representation과 compatibility adapter를 만들 수 있다.
- **예시:** 일부 object는 default state capture가 적절하지 않아 reduction …

#### 3. capability

- **뜻:** 하지만 이 기능이 바로 untrusted pickle의 공격면이기도 하다.
- **왜 중요한가:** 복원 callable은 validation을 우회해 invalid object를 만들지 않도록 normal constructor/factory contract를 재사용한다.
- **예시:** 하지만 이 기능이 바로 untrusted pickle의 공격면이기도 하다.

#### 4. protocol

- **뜻:** Reduction result가 implementation-specific helper와 module path에 의존하면 refactor가 serialization format을 깨뜨린다.
- **왜 중요한가:** Long-lived data라면 public reconstruction function을 안정된 module에 두거나 object format을 별도 DTO로 분리하는 편이 더 낫다.
- **예시:** Reduction result가 implementation-specific helper와 module path에 의존하면 refactor가 …

---

## CHAPTER 06 · class state migration은 old snapshot을 현재 invariant에 맞게 변환해야 한다

### 시작 전 용어집

#### 1. class

- **뜻:** Old snapshot을 단순히 현재 class dictionary에 채우면 required field가 없거나 잘못된 default가 들어갈 수 있다.
- **왜 중요한가:** State version을 보고 단계별 migration을 적용해 현재 invariant를 만족하는 object로 변환한다.
- **예시:** 여러 version을 한 번에 current로 올리는 migration은 `v1→v2→v3`처럼 …

#### 2. migration

- **뜻:** Migration은 parse 성공보다 의미 보존을 검증한다.
- **왜 중요한가:** Old fixture를 load한 뒤 expected domain behavior, equality, aggregate를 확인한다.
- **예시:** 여러 version을 한 번에 current로 올리는 migration은 `v1→v2→v3`처럼 …

#### 3. old snapshot

- **뜻:** Version 1 object에 `name`만 있었는데 version 2에서 `status`가 필수가 되었다고 하자.
- **왜 중요한가:** Field rename에서 값을 잃지 않았는지, enum 의미가 바뀌지 않았는지 test한다.
- **예시:** 여러 version을 한 번에 current로 올리는 migration은 `v1→v2→v3`처럼 …

#### 4. invariant

- **뜻:** 여러 version을 한 번에 current로 올리는 migration은 `v1→v2→v3`처럼 작은 step을 조합하면 각 rule을 독립적으로 검증하기 쉽다.
- **왜 중요한가:** Direct `v1→v5` branch가 많아지면 상태 조합이 복잡해진다.
- **예시:** 여러 version을 한 번에 current로 올리는 migration은 `v1→v2→v3`처럼 …

---

## CHAPTER 07 · persistent ID는 object graph 밖의 entity를 별도 저장소에 연결할 수 있다

### 시작 전 용어집

#### 1. persistent

- **뜻:** 큰 object graph 안에서 database entity나 shared blob을 매번 inline serialization하지 않고 external persistent ID로 참조하고 싶을 수 있다.
- **왜 중요한가:** Serialization 단계에서 특정 object를 ID로 치환하고 load 단계에서 resolver가 실제 object를 찾는 패턴을 사용할 수 있다.
- **예시:** 큰 object graph 안에서 database entity나 shared blob을 …

#### 2. object graph

- **뜻:** 이때 deserialization은 storage dependency를 갖게 된다.
- **왜 중요한가:** Resolver failure에서 missing entity를 어떻게 처리할지, snapshot 시점의 version을 요구하는지 정한다.
- **예시:** 이때 deserialization은 storage dependency를 갖게 된다.

#### 3. entity

- **뜻:** ID만 저장하고 load 시 current entity를 읽으면 과거 snapshot을 복원했는데 일부 값은 현재 상태가 섞일 수 있다.
- **왜 중요한가:** Historical consistency가 필요하면 entity version도 함께 기록한다.
- **예시:** ID만 저장하고 load 시 current entity를 읽으면 과거 …

#### 4. serialization

- **뜻:** Persistent ID는 graph size를 줄이지만 external store availability와 identity semantics를 추가한다.
- **왜 중요한가:** Snapshot이 self-contained해야 하는지 reference-based여도 되는지 목적을 먼저 정한다.
- **예시:** Persistent ID는 graph size를 줄이지만 external store availability와 …

---

## CHAPTER 08 · format 선택은 object fidelity보다 trust·longevity·interoperability에서 시작한다

### 시작 전 용어집

#### 1. format

- **뜻:** 외부 API, user upload, 장기 archive, 다른 언어와 공유하는 data에는 명시적 schema를 가진 data format이 더 적합한 경우가 많다.
- **왜 중요한가:** 선택 기준은 “무엇이 가장 쉽게 직렬화되는가”가 아니라 누가 payload를 만들 수 있고 몇 년 뒤 누가 읽어야 하는가다.
- **예시:** 외부 API, user upload, 장기 archive, 다른 언어와 …

#### 2. object fidelity

- **뜻:** 같은 Python process의 short-lived trusted cache라면 pickle이 편리할 수 있다.
- **왜 중요한가:** Format boundary test에는 round trip뿐 아니라 old fixture compatibility, malformed input, size limit, unknown field, migration을 포함한다.
- **예시:** 같은 Python process의 short-lived trusted cache라면 pickle이 편리할 …

#### 3. trust

- **뜻:** Pickle을 사용할 때는 load source의 trust를 test/configuration으로 제한하고 file extension만으로 안전하다고 판단하지 않는다.
- **왜 중요한가:** Object serialization의 핵심은 **현재 Python object를 편리하게 복원하는 능력과 안전한 외부 데이터 format을 분리하고, 실행 capability를 가진 deserializer는 trusted boundary 안에만 가두는 것**이다.
- **예시:** Pickle을 사용할 때는 load source의 trust를 test/configuration으로 제한하고 …
