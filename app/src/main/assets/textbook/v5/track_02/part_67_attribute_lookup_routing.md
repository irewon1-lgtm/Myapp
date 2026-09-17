# PART 67 · Attribute lookup routing — `__getattribute__`·`__getattr__`·descriptor 우선순위를 추적하기

`obj.name`은 단순히 instance dictionary에서 `name`을 찾는 문법이 아니다. Python object model에서는 `__getattribute__`, descriptor, instance state, class attribute, `__getattr__` fallback이 서로 다른 시점에 개입한다. Attribute access를 확장할 때 이 순서를 잘못 이해하면 무한 recursion, cache 우회, 보안 경계 누락이 생길 수 있다. 이 PART에서는 **읽기 요청이 어떤 경로를 따라 해결되는지, 쓰기·삭제가 어떤 hook을 거치는지**를 중심으로 본다.

---

## CHAPTER 01 · attribute read는 단일 dictionary lookup이 아니라 우선순위 pipeline이다

`obj.x`를 읽을 때 “`obj.__dict__["x"]`를 찾는다”는 설명은 너무 단순하다. Class에 data descriptor가 있는지, instance dictionary에 값이 있는지, class hierarchy에 일반 attribute가 있는지에 따라 결과가 달라질 수 있다.

Property가 instance dictionary보다 먼저 개입할 수 있는 이유도 descriptor precedence 때문이다. 반대로 non-data descriptor는 instance attribute에 가려질 수 있다. 즉 동일한 이름이 여러 층에 존재할 수 있으며 **어느 층이 이기는가**가 데이터 모델의 일부다.

디버깅할 때는 현재 값만 보지 말고 class MRO, descriptor 여부, instance `__dict__`, custom lookup hook을 함께 본다.

---

## CHAPTER 02 · `__getattribute__`는 정상 attribute read마다 먼저 들어오는 강력한 hook이다

`__getattribute__`는 특정 이름이 없을 때만 호출되는 fallback이 아니다. 일반적인 attribute read 자체를 가로챌 수 있다. 그래서 logging, proxy, lazy loading을 만들 수 있지만 recursion 위험도 가장 크다.

```python
class Logged:
    def __getattribute__(self, name):
        print("read", name)
        return object.__getattribute__(self, name)
```

여기서 `self.__dict__`를 다시 평범하게 읽으면 그 read도 `__getattribute__`를 호출해 recursion이 생길 수 있다. Base implementation을 직접 사용하거나 필요한 low-level access 경로를 명확히 해야 한다.

강력한 hook일수록 최소한으로 사용한다. 특정 missing attribute만 처리하려는 목적이라면 `__getattr__`가 더 좁고 안전한 선택일 수 있다.

---

## CHAPTER 03 · `__getattr__`는 정상 lookup이 실패한 뒤에만 동작하는 fallback이다

`__getattr__`는 attribute가 정상 경로에서 발견되지 않았을 때 호출된다. 따라서 dynamic field, compatibility alias, lazy proxy처럼 “없는 이름을 어떻게 해석할까”에 적합하다.

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

없는 key를 `AttributeError`로 바꾸는 이유는 Python attribute protocol의 기대와 맞추기 위해서다. `hasattr`, introspection, framework code가 이 예외 의미를 사용한다.

모든 unknown name에 `None`을 반환하면 오타가 조용히 통과할 수 있다. Dynamic access가 편리하더라도 실제로 존재하지 않는 이름은 명확히 실패시키는 편이 디버깅에 유리하다.

---

## CHAPTER 04 · custom attribute hook의 가장 흔한 실패는 자기 자신을 다시 호출하는 recursion이다

다음과 같은 코드는 위험하다.

```python
class Broken:
    def __getattribute__(self, name):
        if name in self._cache:
            return self._cache[name]
        return object.__getattribute__(self, name)
```

`self._cache`를 읽는 순간 다시 `__getattribute__`가 호출될 수 있다. 해결은 `object.__getattribute__(self, "_cache")`처럼 base path를 이용해 hook을 우회하는 것이다.

같은 문제가 `__setattr__`, `__delattr__`에서도 생긴다. Hook 내부에서 평범한 문법으로 다시 같은 operation을 수행하면 재진입한다. Custom routing 코드는 “어떤 access가 hook을 다시 통과하는가”를 명시적으로 추적해야 한다.

Recursion limit 오류가 보인다고 단순 재귀 함수만 찾지 않는다. Attribute hook이 자기 내부 상태를 읽는 경로도 확인한다.

---

## CHAPTER 05 · `__setattr__`는 쓰기 routing과 invariant 보호에 사용된다

`obj.x = value`는 custom `__setattr__`가 있으면 그 hook을 통과한다. Field validation, immutable-after-init, proxy forwarding을 구현할 수 있다.

```python
class PositiveBalance:
    def __setattr__(self, name, value):
        if name == "balance" and value < 0:
            raise ValueError("negative balance")
        object.__setattr__(self, name, value)
```

하지만 모든 assignment에 비싼 validation이나 remote I/O를 숨기면 평범한 field write의 비용 모델이 무너진다. Domain transition이 복잡하면 명시적 method가 더 나을 수 있다.

Descriptor의 `__set__`과 `__setattr__`가 함께 있는 경우 실제 호출 순서를 이해해야 한다. Attribute routing의 한 층만 보고 중복 validation을 넣지 않는다.

---

## CHAPTER 06 · `__delattr__`는 이름 삭제를 domain transition으로 바꿀 수 있다

`del obj.x`도 단순 dictionary key 삭제가 아니다. Custom `__delattr__`를 통해 required field 삭제를 거부하거나 cleanup을 수행할 수 있다.

```python
class Account:
    def __delattr__(self, name):
        if name == "id":
            raise AttributeError("id cannot be deleted")
        object.__delattr__(self, name)
```

삭제를 지원한다면 “없는 attribute 삭제”, descriptor deletion, internal cache invalidation을 어떻게 처리할지 정한다. 삭제 후 fallback `__getattr__`가 같은 이름을 다시 만들어내면 caller가 실제 삭제 여부를 오해할 수 있다.

따라서 read/write/delete hook은 개별 기능이 아니라 같은 namespace 의미를 공유해야 한다.

---

## CHAPTER 07 · descriptor precedence는 attribute lookup 전체 pipeline 안에서 이해해야 한다

Descriptor 자체의 `__get__`, `__set__`, `__delete__`는 앞선 PART에서 배웠더라도, 실제 attribute lookup에서는 instance dictionary와 class attribute 사이의 우선순위가 핵심이다.

Data descriptor는 instance attribute보다 우선할 수 있고, non-data descriptor는 instance attribute에 가려질 수 있다. Function object가 class attribute에서 bound method처럼 보이는 현상도 descriptor binding과 연결된다.

이 구조를 이용해 `property`, ORM field, validation field가 동작한다. 하지만 동일 이름을 instance state와 descriptor 양쪽에서 관리하면 숨은 precedence bug가 생긴다. Storage name을 분리하거나 descriptor가 직접 private storage를 소유하는 방식으로 충돌을 줄인다.

중요한 것은 descriptor를 별도 마법으로 외우지 않고 전체 lookup route의 한 단계로 배치하는 것이다.

---

## CHAPTER 08 · attribute contract는 lookup 우선순위와 실패 의미를 외부 API로 만든다

사용자 타입이 attribute hook을 제공하면 caller에게는 평범한 `obj.name`으로 보이지만 내부에서는 cache, proxy, validation, fallback이 실행될 수 있다. 따라서 존재성, mutation 가능성, 비용, 실패 예외가 예측 가능해야 한다.

테스트에서는 실제 field, class attribute, descriptor, missing name, shadowing, deletion을 각각 확인한다. 특히 `hasattr`, `getattr(default)`, introspection 도구와 custom hook이 함께 동작하는지 본다.

이 PART의 핵심은 **attribute access를 dictionary lookup으로 축소하지 않고, `__getattribute__`에서 시작해 descriptor·instance state·class hierarchy·`__getattr__`로 이어지는 routing pipeline으로 추적하는 것**이다.
