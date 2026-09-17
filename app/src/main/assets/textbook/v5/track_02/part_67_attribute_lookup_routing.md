# TRACK 02 · P67 — Attribute lookup routing: __getattribute__·__getattr__·setattr·delattr 경계를 추적하기

`obj.name`은 Python에서 가장 평범해 보이는 표현 중 하나다. 그러나 실제로는 타입의 descriptor, 인스턴스 namespace, `__getattribute__`, `__getattr__`가 서로 다른 우선순위로 참여한다. 쓰기와 삭제도 `__setattr__`, `__delattr__`를 통해 별도 경로를 가진다.

P58에서는 descriptor 자체의 계약을 집중적으로 봤다. 이번 PART에서는 descriptor를 다시 설명하는 대신 **attribute read/write/delete 전체 routing 속에서 descriptor가 어느 위치에 들어가는지**를 연결한다.

---

## 1. Attribute read pipeline — `obj.name`은 단일 dict lookup이 아니다

초보 단계에서는 `obj.name`을 `obj.__dict__["name"]`처럼 이해하기 쉽다. 실제 lookup은 더 복합적이다. 사용자 코드 관점에서 중요한 개념적 순서는 다음과 같다.

```text
obj.name
  ↓
type(obj)의 attribute access machinery
  ↓
data descriptor 후보
  ↓
instance namespace
  ↓
non-data descriptor / class attribute
  ↓
찾지 못하면 __getattr__ fallback 가능
```

실제 구현 세부를 모두 외울 필요는 없지만, **인스턴스 dict가 항상 최우선은 아니다**라는 점은 중요하다.

```python
class Account:
    kind = "user"

    def __init__(self, name):
        self.name = name

account = Account("Kim")
print(account.name)
print(account.kind)
```

`name`은 인스턴스 상태이고 `kind`는 class attribute지만 둘 다 같은 점 문법으로 읽힌다. 호출자는 문법만 보고 저장 위치를 알 수 없다.

따라서 동적 attribute 기능을 디버깅할 때는 “`__dict__`에 값이 있나?”만 보지 말고 **어떤 lookup 단계가 그보다 먼저 값을 가로채는가**를 확인해야 한다.

---

## 2. `__getattribute__` — 정상 조회까지 항상 통과하는 문이다

`__getattribute__`는 해당 클래스 인스턴스의 일반 attribute 읽기에 광범위하게 관여한다.

```python
class Logged:
    def __getattribute__(self, name):
        print("read:", name)
        return object.__getattribute__(self, name)
```

핵심은 `__getattr__`와 달리 **attribute가 존재하는 정상 경로에도 호출된다**는 점이다. 그래서 powerful하지만 위험하다.

다음과 같은 용도가 가능하다.

- 접근 로깅
- proxy forwarding
- 보안/정책 검사
- lazy namespace routing

하지만 모든 읽기 경로에 들어가므로 작은 실수도 객체 전체를 사용할 수 없게 만들 수 있다.

```python
class Broken:
    def __getattribute__(self, name):
        return self.__dict__[name]  # 이 self.__dict__ 조회도 다시 __getattribute__를 탄다
```

이런 구현은 재귀에 빠진다. 내부에서 기본 lookup을 사용하려면 보통 `object.__getattribute__(self, name)`처럼 명시적으로 위임해야 한다.

`__getattribute__`는 “없는 속성을 만드는 hook”이 아니라 **모든 정상 속성 접근 경로를 감싸는 저수준 경계**다.

---

## 3. `__getattr__` — lookup 실패 뒤의 fallback이다

`__getattr__`는 이름이 비슷하지만 역할이 다르다. 일반 lookup이 attribute를 찾지 못했을 때만 fallback으로 참여한다.

```python
class Settings:
    def __init__(self, values):
        self._values = values

    def __getattr__(self, name):
        try:
            return self._values[name]
        except KeyError as exc:
            raise AttributeError(name) from exc
```

이 구조는 실제 attribute와 동적 설정 namespace를 분리하는 데 사용할 수 있다.

중요한 계약은 정말 없는 이름이면 `AttributeError`를 유지하는 것이다. `hasattr`, introspection, 여러 framework가 이 신호를 사용한다.

잘못된 예:

```python
    def __getattr__(self, name):
        return None
```

이 구현은 오타까지 `None`으로 바꾼다.

```python
config.databse_url   # database_url 오타인데 조용히 None
```

동적 fallback을 제공할수록 **실패 신호를 더 정확하게 보존해야 한다**. 편의를 위해 모든 이름을 받아 주면 디버깅 가능한 오류가 사라진다.

---

## 4. Recursion trap — hook 내부의 attribute access를 분리한다

`__getattribute__`, `__setattr__`, `__getattr__`를 커스터마이즈할 때 가장 흔한 실패 유형은 자기 자신을 다시 호출하는 것이다.

```python
class Guarded:
    def __init__(self):
        object.__setattr__(self, "_reads", 0)

    def __getattribute__(self, name):
        if name != "_reads":
            count = object.__getattribute__(self, "_reads")
            object.__setattr__(self, "_reads", count + 1)
        return object.__getattribute__(self, name)
```

여기서는 hook 내부의 내부 상태 접근을 기본 구현으로 우회한다.

재귀 문제를 추적할 때는 코드를 줄 단위로 보지 말고 **각 점 접근이 어떤 hook으로 다시 들어가는지**를 표시하면 된다.

```text
self.cache
→ self.__getattribute__("cache")
→ hook 내부 self.cache
→ self.__getattribute__("cache")
→ ...
```

또 logging 자체가 객체의 `__repr__`나 다른 attribute를 읽어 재진입할 수도 있다. 그래서 저수준 hook 안에서는 부가 동작을 최소화해야 한다.

이 원칙은 attribute hook뿐 아니라 `__repr__`, exception formatting, tracing 같은 low-level 경계에도 일반화된다. **관찰 코드가 다시 관찰 대상 프로토콜을 호출하는지** 확인해야 한다.

---

## 5. `__setattr__` routing — 쓰기도 정책 경계가 된다

`obj.name = value`는 `__setattr__` 경로를 통한다. 이 hook으로 validation이나 immutability 정책을 구현할 수 있다.

```python
class PositiveLimit:
    def __setattr__(self, name, value):
        if name == "limit" and value <= 0:
            raise ValueError("limit must be positive")
        object.__setattr__(self, name, value)

    def __init__(self, limit):
        self.limit = limit
```

여기서 `__init__`의 대입도 같은 hook을 탄다. 따라서 초기화 단계만 예외로 둘 것인지, 모든 lifetime에서 같은 규칙을 적용할 것인지 결정해야 한다.

또 P58의 data descriptor가 `__set__`을 제공한다면 attribute assignment machinery 안에서 descriptor 규칙이 함께 작동한다. `__setattr__`를 직접 구현하면서 무조건 `self.__dict__[name] = value`로 넣어 버리면 descriptor가 기대한 validation/storage 정책을 우회할 수 있다.

그래서 custom `__setattr__`는 가능하면 최종 저장을 `object.__setattr__`에 위임해 기본 데이터 모델을 보존하는 편이 안전하다.

쓰기 hook을 설계할 때 질문은 다음과 같다.

- 초기화와 운영 중 대입의 규칙이 같은가?
- descriptor/property validation을 우회하지 않는가?
- 내부 bookkeeping attribute도 같은 정책을 타야 하는가?
- 실패 시 부분적으로 다른 attribute가 이미 변경되는가?

---

## 6. `__delattr__` — 삭제 가능성도 객체 invariant의 일부다

`del obj.name`은 단순히 dict key를 지우는 작업으로 끝나지 않는다. 어떤 attribute는 객체가 살아 있는 동안 반드시 존재해야 할 수 있다.

```python
class Session:
    def __init__(self, token):
        self.token = token

    def __delattr__(self, name):
        if name == "token":
            raise AttributeError("token cannot be deleted")
        object.__delattr__(self, name)
```

삭제를 허용하면 이후 메서드들이 “항상 존재한다”고 믿던 invariant가 깨질 수 있다.

반대로 cache, optional metadata처럼 삭제가 상태 reset을 의미하는 attribute도 있다.

```python
class CachedValue:
    def __init__(self):
        self._cache = {}

    def invalidate(self, key):
        self._cache.pop(key, None)
```

이 경우 `del obj.cache_entry`처럼 attribute deletion으로 표현하는 것보다 명시적인 domain method가 더 읽기 좋을 수도 있다.

데이터 모델 hook을 쓸 수 있다는 것과 그것이 가장 좋은 API라는 것은 별개다. 삭제 semantics가 도메인 동작이라면 메서드 이름으로 의미를 드러내는 편이 안전하다.

---

## 7. Descriptor precedence bridge — P58을 전체 lookup 안에 배치한다

P58에서 배운 descriptor를 지금 lookup pipeline과 연결해 보자.

```python
class Field:
    def __set_name__(self, owner, name):
        self.private_name = "_" + name

    def __get__(self, instance, owner):
        if instance is None:
            return self
        return getattr(instance, self.private_name)

    def __set__(self, instance, value):
        if not value:
            raise ValueError("empty value")
        setattr(instance, self.private_name, value)

class User:
    name = Field()
```

`Field`처럼 `__set__`을 제공하는 data descriptor는 단순한 class attribute보다 강한 precedence를 가진다. 그래서 `instance.__dict__["name"]`에 값을 억지로 넣더라도 정상 `instance.name` 읽기가 그것을 그대로 반환한다고 가정하면 안 된다.

반대로 method처럼 non-data descriptor는 인스턴스 attribute가 shadow할 수 있는 경로가 존재한다.

여기서 핵심은 descriptor 세부를 다시 외우는 것이 아니다.

```text
attribute syntax
→ __getattribute__ machinery
→ descriptor precedence 판단
→ instance namespace
→ class/MRO lookup
→ 실패 시 __getattr__
```

이 전체 그림 안에서 P58 지식을 배치하는 것이다.

Attribute bug를 분석할 때 “descriptor 버그인지”, “custom `__getattribute__` 버그인지”, “instance shadowing인지”를 분리할 수 있게 된다.

---

## 8. Attribute contract — magic namespace를 만들기 전에 경계를 적는다

동적 attribute는 간결하지만 정적 가독성을 빠르게 낮출 수 있다. 따라서 다음 계약을 먼저 적는다.

```text
normal fields: id, name
computed descriptor: age
fallback namespace: metadata keys only
missing metadata: AttributeError
writes: known fields only
internal fields: _cache, _values
unknown assignment: reject
attribute access I/O: none
```

특히 `obj.customer.profile.name` 같은 표현이 뒤에서 네트워크 호출을 일으킨다면 latency와 failure가 문법에서 보이지 않는다. 일반 attribute 조회는 대체로 local access라는 기대가 강하기 때문이다.

Proxy 객체도 마찬가지다. 모든 이름을 remote object에 전달하는 구조는 편하지만 다음을 별도로 설계해야 한다.

- local management attribute와 remote attribute 충돌
- `__repr__`, `__class__`, introspection 같은 특별 조회
- remote failure를 `AttributeError`로 바꿀지 별도 예외로 보존할지
- typo가 remote request까지 가는 문제

최종 체크리스트:

```text
[ ] __getattribute__와 __getattr__ 역할을 구분했다.
[ ] hook 내부 재귀 경로를 기본 구현 위임으로 차단했다.
[ ] 없는 이름은 정확한 AttributeError를 보존한다.
[ ] __setattr__/__delattr__가 객체 invariant를 깨지 않는다.
[ ] descriptor precedence를 우회하지 않는다.
[ ] attribute syntax 뒤에 예상 밖의 고비용 I/O를 숨기지 않는다.
[ ] 동적 namespace 범위가 제한되어 오타를 삼키지 않는다.
```

---

## 직관 봉인

- `obj.name`은 단순 `__dict__` 조회가 아니다.
- `__getattribute__`는 정상 attribute도 통과하지만 `__getattr__`는 실패 후 fallback이다.
- attribute hook 내부에서 `self.x`를 무심코 읽으면 같은 hook에 재진입할 수 있다.
- `__setattr__`를 직접 구현해도 descriptor와 기본 데이터 모델을 함부로 우회하면 안 된다.
- 삭제 가능성은 객체 invariant의 일부다.
- P58 descriptor는 독립 기술이 아니라 전체 attribute lookup precedence 안에서 동작한다.

## 다음 연결

다음 PART에서는 attribute로 얻은 함수나 객체가 실제로 호출될 때의 **callable protocol**을 다룬다. `__call__`, function binding, bound method의 `self`, positional/keyword argument binding, signature introspection을 한 실행 흐름으로 연결한다.
