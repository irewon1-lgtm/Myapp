# PART 67 · Attribute lookup routing — `__getattribute__`·`__getattr__`·descriptor 우선순위를 추적하기

`obj.name`은 단순히 instance dictionary에서 `name`을 찾는 문법이 아니다. Python object model에서는 `__getattribute__`, descriptor, instance state, class attribute, `__getattr__` fallback이 서로 다른 시점에 개입한다. Attribute access를 확장할 때 이 순서를 잘못 이해하면 무한 recursion, cache 우회, 보안 경계 누락이 생길 수 있다. 이 절에서는 **읽기 요청이 어떤 경로를 따라 해결되는지, 쓰기·삭제가 어떤 hook을 거치는지**를 중심으로 본다.

---

## CHAPTER 01 · attribute read는 단일 dictionary lookup이 아니라 우선순위 pipeline이다

### 시작 전 용어집

#### 1. attribute read

- **뜻:** __dict__["x"]`를 찾는다”는 설명은 너무 단순하다.
- **왜 중요한가:** Class에 data descriptor가 있는지, instance dictionary에 값이 있는지, class hierarchy에 일반 attribute가 있는지에 따라 결과가 달라질 수 있다.
- **예시:** __dict__["x"]`를 찾는다”는 설명은 너무 단순하다.

#### 2. dict

- **뜻:** Property가 instance dictionary보다 먼저 개입할 수 있는 이유도 descriptor precedence 때문이다.
- **왜 중요한가:** 반대로 non-data descriptor는 instance attribute에 가려질 수 있다.
- **예시:** Property가 instance dictionary보다 먼저 개입할 수 있는 이유도 …

#### 3. pipeline

- **뜻:** 즉 동일한 이름이 여러 층에 존재할 수 있으며 **어느 층이 이기는가**가 데이터 모델의 일부다.
- **왜 중요한가:** 디버깅할 때는 현재 값만 보지 말고 class MRO, descriptor 여부, instance `__dict__`, custom lookup hook을 함께 본다.
- **예시:** 즉 동일한 이름이 여러 층에 존재할 수 있으며 …

`obj.x`를 읽을 때 “`obj. 

  


---

## CHAPTER 02 · `__getattribute__`는 정상 attribute read마다 먼저 들어오는 강력한 hook이다

### 시작 전 용어집

#### 1. __getattribute__

- **뜻:** `__getattribute__`는 특정 이름이 없을 때만 호출되는 fallback이 아니다.
- **왜 중요한가:** 일반적인 attribute read 자체를 가로챌 수 있다.
- **예시:** class Logged: / def __getattribute__(self, name):

#### 2. attribute read

- **뜻:** 그래서 logging, proxy, lazy loading을 만들 수 있지만 recursion 위험도 가장 크다.
- **왜 중요한가:** __dict__`를 다시 평범하게 읽으면 그 read도 `__getattribute__`를 호출해 recursion이 생길 수 있다.
- **예시:** class Logged: / def __getattribute__(self, name):

#### 3. hook

- **뜻:** Base implementation을 직접 사용하거나 필요한 low-level access 경로를 명확히 해야 한다.
- **왜 중요한가:** 특정 missing attribute만 처리하려는 목적이라면 `__getattr__`가 더 좁고 안전한 선택일 수 있다.
- **예시:** class Logged: / def __getattribute__(self, name):

```python
class Logged:
    def __getattribute__(self, name):
        print("read", name)
        return object.__getattribute__(self, name)
```

여기서 `self. 

강력한 hook일수록 최소한으로 사용한다. 

---

## CHAPTER 03 · `__getattr__`는 정상 lookup이 실패한 뒤에만 동작하는 fallback이다

### 시작 전 용어집

#### 1. __getattr__

- **뜻:** `__getattr__`는 attribute가 정상 경로에서 발견되지 않았을 때 호출된다.
- **왜 중요한가:** 따라서 dynamic field, compatibility alias, lazy proxy처럼 “없는 이름을 어떻게 해석할까”에 적합하다.
- **예시:** class Settings: / def __init__(self, data):

#### 2. lookup

- **뜻:** 없는 key를 `AttributeError`로 바꾸는 이유는 Python attribute protocol의 기대와 맞추기 위해서다.
- **왜 중요한가:** `hasattr`, introspection, framework code가 이 예외 의미를 사용한다.
- **예시:** class Settings: / def __init__(self, data):

#### 3. 실패

- **뜻:** Dynamic access가 편리하더라도 실제로 존재하지 않는 이름은 명확히 실패시키는 편이 디버깅에 유리하다.
- **왜 중요한가:** 모든 unknown name에 `None`을 반환하면 오타가 조용히 통과할 수 있다.
- **예시:** class Settings: / def __init__(self, data):

```python
class Settings:
    def __init__(self, data):
        self._data = data

    def __getattr__(self, name):
        try:
            return self._data[name]
        except KeyError as exc:
            raise AttributeError(name) from exc
```

 

 

---

## CHAPTER 04 · custom attribute hook의 가장 흔한 실패는 자기 자신을 다시 호출하는 recursion이다

### 시작 전 용어집

#### 1. custom attribute

- **뜻:** _cache`를 읽는 순간 다시 `__getattribute__`가 호출될 수 있다.
- **왜 중요한가:** __getattribute__(self, "_cache")`처럼 base path를 이용해 hook을 우회하는 것이다.
- **예시:** class Broken: / def __getattribute__(self, name):

#### 2. hook

- **뜻:** Hook 내부에서 평범한 문법으로 다시 같은 operation을 수행하면 재진입한다.
- **왜 중요한가:** Custom routing 코드는 “어떤 access가 hook을 다시 통과하는가”를 명시적으로 추적해야 한다.
- **예시:** class Broken: / def __getattribute__(self, name):

#### 3. 실패

- **뜻:** 같은 문제가 `__setattr__`, `__delattr__`에서도 생긴다.
- **왜 중요한가:** Recursion limit 오류가 보인다고 단순 재귀 함수만 찾지 않는다.
- **예시:** class Broken: / def __getattribute__(self, name):

#### 4. recursion

- **뜻:** Attribute hook이 자기 내부 상태를 읽는 경로도 확인한다.
- **예시:** class Broken: / def __getattribute__(self, name):

다음과 같은 코드는 위험하다.

```python
class Broken:
    def __getattribute__(self, name):
        if name in self._cache:
            return self._cache[name]
        return object.__getattribute__(self, name)
```

`self. 해결은 `object.

  

 

---

## CHAPTER 05 · `__setattr__`는 쓰기 routing과 invariant 보호에 사용된다

### 시작 전 용어집

#### 1. __setattr__

- **뜻:** x = value`는 custom `__setattr__`가 있으면 그 hook을 통과한다.
- **왜 중요한가:** Field validation, immutable-after-init, proxy forwarding을 구현할 수 있다.
- **예시:** class PositiveBalance: / def __setattr__(self, name, value):

#### 2. routing

- **뜻:** Attribute routing의 한 층만 보고 중복 validation을 넣지 않는다.
- **왜 중요한가:** 하지만 모든 assignment에 비싼 validation이나 remote I/O를 숨기면 평범한 field write의 비용 모델이 무너진다.
- **예시:** class PositiveBalance: / def __setattr__(self, name, value):

#### 3. invariant

- **뜻:** Domain transition이 복잡하면 명시적 method가 더 나을 수 있다.
- **왜 중요한가:** Descriptor의 `__set__`과 `__setattr__`가 함께 있는 경우 실제 호출 순서를 이해해야 한다.
- **예시:** class PositiveBalance: / def __setattr__(self, name, value):

`obj. 

```python
class PositiveBalance:
    def __setattr__(self, name, value):
        if name == "balance" and value < 0:
            raise ValueError("negative balance")
        object.__setattr__(self, name, value)
```

 

 

---

## CHAPTER 06 · `__delattr__`는 이름 삭제를 domain transition으로 바꿀 수 있다

### 시작 전 용어집

#### 1. __delattr__

- **뜻:** Custom `__delattr__`를 통해 required field 삭제를 거부하거나 cleanup을 수행할 수 있다.
- **왜 중요한가:** 삭제를 지원한다면 “없는 attribute 삭제”, descriptor deletion, internal cache invalidation을 어떻게 처리할지 정한다.
- **예시:** class Account: / def __delattr__(self, name):

#### 2. domain transition

- **뜻:** x`도 단순 dictionary key 삭제가 아니다.
- **왜 중요한가:** 삭제 후 fallback `__getattr__`가 같은 이름을 다시 만들어내면 caller가 실제 삭제 여부를 오해할 수 있다.
- **예시:** class Account: / def __delattr__(self, name):

#### 3. dict

- **뜻:** 따라서 read/write/delete hook은 개별 기능이 아니라 같은 namespace 의미를 공유해야 한다.
- **예시:** class Account: / def __delattr__(self, name):

`del obj. 

```python
class Account:
    def __delattr__(self, name):
        if name == "id":
            raise AttributeError("id cannot be deleted")
        object.__delattr__(self, name)
```

 


---

**실행 점검 67-6 — CHAPTER 06 · `__delattr__`는 이름 삭제를 domain transition으로 바꿀 수 있다**
CHAPTER 06 · `__delattr__`는 이름 삭제를 domain transition으로 바꿀 수 있다을 이해할 때는 정상 사례 하나를 정한 뒤 변경 전후의 반환값과 부수효과를 따로 확인한다. 이어서 값 하나나 호출 순서 하나만 바꿔 결과 차이를 확인하면 CHAPTER 06 · `__delattr__`는 이름 삭제를 domain transition으로 바꿀 수 있다이 실제 프로그램 상태에 어떤 제약을 주는지 보인다. PART 67 CHAPTER 6의 한 줄 해석은 “규칙을 값과 상태 변화로 확인한다”이다. 우연히 통과한 한 번의 실행보다 반복 가능한 관찰을 통과 기준으로 삼는다. 마지막에는 타입·정체성·수명·예외 경계 중 실제 원인이 어디였는지 자기 문장으로 설명해, 단순한 문법 복사가 아니라 동작 원리까지 이해했는지 확인한다.
## CHAPTER 07 · descriptor precedence는 attribute lookup 전체 pipeline 안에서 이해해야 한다

### 시작 전 용어집

#### 1. descriptor

- **뜻:** Descriptor 자체의 `__get__`, `__set__`, `__delete__`는 앞선 PART에서 배웠더라도, 실제 attribute lookup에서는 instance dictionary와 class attribute 사이의 우선순위가 핵심이다.
- **왜 중요한가:** Data descriptor는 instance attribute보다 우선할 수 있고, non-data descriptor는 instance attribute에 가려질 수 있다.
- **예시:** Descriptor 자체의 `__get__`, `__set__`, `__delete__`는 앞선 PART에서 배웠더라도, …

#### 2. attribute lookup

- **뜻:** Function object가 class attribute에서 bound method처럼 보이는 현상도 descriptor binding과 연결된다.
- **왜 중요한가:** 이 구조를 이용해 `property`, ORM field, validation field가 동작한다.
- **예시:** Function object가 class attribute에서 bound method처럼 보이는 현상도 …

#### 3. pipeline

- **뜻:** 하지만 동일 이름을 instance state와 descriptor 양쪽에서 관리하면 숨은 precedence bug가 생긴다.
- **왜 중요한가:** Storage name을 분리하거나 descriptor가 직접 private storage를 소유하는 방식으로 충돌을 줄인다.
- **예시:** 하지만 동일 이름을 instance state와 descriptor 양쪽에서 관리하면 …

#### 4. set

- **뜻:** 중요한 것은 descriptor를 별도 마법으로 외우지 않고 전체 lookup route의 한 단계로 배치하는 것이다.
- **예시:** 중요한 것은 descriptor를 별도 마법으로 외우지 않고 전체 …

---

## CHAPTER 08 · attribute contract는 lookup 우선순위와 실패 의미를 외부 API로 만든다

### 시작 전 용어집

#### 1. attribute contract

- **뜻:** 사용자 타입이 attribute hook을 제공하면 caller에게는 평범한 `obj.
- **왜 중요한가:** name`으로 보이지만 내부에서는 cache, proxy, validation, fallback이 실행될 수 있다.
- **예시:** 따라서 존재성, mutation 가능성, 비용, 실패 예외가 예측 …

#### 2. lookup

- **뜻:** 이 PART의 핵심은 **attribute access를 dictionary lookup으로 축소하지 않고, `__getattribute__`에서 시작해 descriptor·instance state·class hierarchy·`__getattr__`로 이어지는 routing pipeline으로 추적하는 것**이다.
- **왜 중요한가:** 따라서 존재성, mutation 가능성, 비용, 실패 예외가 예측 가능해야 한다.
- **예시:** 이 PART의 핵심은 **attribute access를 dictionary lookup으로 축소하지 …

#### 3. 실패

- **뜻:** 테스트에서는 실제 field, class attribute, descriptor, missing name, shadowing, deletion을 각각 확인한다.
- **왜 중요한가:** 특히 `hasattr`, `getattr(default)`, introspection 도구와 custom hook이 함께 동작하는지 본다.
- **예시:** 테스트에서는 실제 field, class attribute, descriptor, missing name, …

---

## 실전 학습 루프 · attribute lookup routing

### 1. 쉬운 예

`obj.name` 한 줄은 단순 dict 조회가 아니다. instance, class, descriptor, `__getattribute__`, 필요하면 `__getattr__`까지 정해진 탐색 규칙이 개입한다. 그래서 같은 이름이 여러 위치에 있을 때 우선순위를 모르면 예상과 다른 값이 나온다.

### 2. 한 줄 해석

속성 접근 버그는 값을 어디에 저장했는지보다 “어떤 lookup 경로가 먼저 선택됐는지”를 추적해야 해결된다.

### 3. 직접 실행

아래 코드는 개념을 작게 격리한 예다. 실행 전에 출력이나 상태 변화를 먼저 예상한 뒤 실제 결과와 비교한다.

```python
class Demo:
    kind = 'class'
    def __init__(self):
        self.name = 'instance'
    def __getattr__(self, key):
        return f'missing:{key}'

x = Demo()
print(x.name, x.kind, x.unknown)
```

결과가 예상과 다르면 문법부터 고치지 말고, **어떤 protocol·상태·계약이 호출됐는지**를 한 단계씩 확인한다. 이렇게 해야 “우연히 동작하는 코드”와 “이유를 설명할 수 있는 코드”를 구분할 수 있다.

### 4. 수정 실습

1. instance의 `kind`를 추가해 class attribute와의 우선순위를 확인한다.
2. `__getattribute__`를 무심코 재귀 호출하도록 작성했을 때 왜 무한 재귀가 생기는지 살펴본다.

수정 후에는 정상 입력 하나만 보지 말고 빈 값, 경계값, 반복 호출, 예외 경로 중 해당되는 반례를 최소 하나 추가한다.

### 5. 확인 문제

`__getattr__`은 모든 속성 접근 때 항상 호출될까?

### 6. 정답과 오답 설명

**정답:** 아니다. 일반 lookup이 속성을 찾지 못했을 때 fallback으로 호출된다.

**자주 나오는 오답:** `__getattr__`과 `__getattribute__`를 같은 hook이라고 보면 호출 시점과 위험도가 섞인다.

마지막으로 코드를 다시 읽으면서 **입력 → 호출되는 규칙 → 상태 변화 → 결과/예외** 네 칸으로 요약한다. 이 네 칸을 설명할 수 있으면 단순 암기가 아니라 실행 모델을 이해한 것이다.

