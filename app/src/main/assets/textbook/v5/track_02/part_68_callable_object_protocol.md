# TRACK 02 · P68 — Callable object protocol: __call__·method binding·argument routing을 추적하기

Python에서 `thing(...)` 문법은 함수에만 허용되지 않는다. 클래스 인스턴스도 `__call__`을 통해 callable이 될 수 있고, 클래스 본문에 정의된 함수는 instance를 통해 읽힐 때 bound method로 변환된다. 그 뒤 positional-only, keyword-only, default, `*args`, `**kwargs` 규칙에 따라 실제 인수가 parameter에 결합된다.

이 PART에서는 호출을 “괄호를 붙이면 함수 실행”으로 보지 않는다. **callable 획득 → method binding → argument binding → callable body 진입 → 상태/실패 계약**의 순서로 분해한다.

---

## 1. Callable object — 함수가 아니어도 호출 문법에 참여할 수 있다

객체가 `__call__`을 구현하면 인스턴스 자체를 호출할 수 있다.

```python
class Clamp:
    def __init__(self, minimum, maximum):
        self.minimum = minimum
        self.maximum = maximum

    def __call__(self, value):
        return min(self.maximum, max(self.minimum, value))

limit_percentage = Clamp(0, 100)
print(limit_percentage(120))  # 100
```

이 패턴은 단순 함수보다 **호출 사이에 설정 상태를 보존해야 할 때** 유용하다. 그러나 callable이라는 이유만으로 객체의 모든 동작을 `__call__`에 넣으면 의미가 흐려진다.

다음 두 API를 비교해 보자.

```python
validator(data)
validator.validate(data)
```

첫 번째는 객체의 주 역할이 “입력을 받아 결과를 만든다”일 때 간결하다. 두 번째는 객체가 여러 operation을 가진 서비스일 때 더 명확할 수 있다.

`callable(obj)`는 객체가 호출 가능한지 확인하는 데 쓸 수 있지만, **어떤 signature와 의미를 가진 호출인지**까지 보장하지 않는다.

---

## 2. Function binding — class attribute인 함수는 그대로 반환되지 않는다

클래스 본문에 정의한 함수는 instance를 통해 조회할 때 descriptor protocol에 의해 bound method가 된다.

```python
class Greeter:
    def hello(self, name):
        return f"hello {name}"

print(Greeter.hello)
g = Greeter()
print(g.hello)
```

`Greeter.hello`와 `g.hello`는 같은 방식으로 보이지 않는다. instance를 통해 접근하면 `g`가 method 호출의 첫 인수에 결합된 형태가 된다.

그래서 다음 두 호출은 개념적으로 연결된다.

```python
g.hello("Lee")
Greeter.hello(g, "Lee")
```

이 연결은 `self`가 예약어이기 때문에 일어나는 것이 아니다. `self`는 관례적인 parameter 이름일 뿐이고, 핵심은 **function object의 descriptor binding**이다.

P58의 descriptor와 P67의 attribute lookup이 여기에서 호출 모델과 이어진다.

---

## 3. Bound method와 `self` — 객체와 함수가 결합된 호출 준비 상태다

Bound method를 변수에 저장해도 receiver 결합은 유지된다.

```python
class Counter:
    def __init__(self):
        self.value = 0

    def increment(self, amount=1):
        self.value += amount
        return self.value

counter = Counter()
step = counter.increment

step()
step(4)
print(counter.value)  # 5
```

`step`에는 호출할 함수뿐 아니라 어느 instance가 receiver인지에 대한 연결이 들어 있다. 그래서 다시 `counter`를 명시적으로 넘기지 않는다.

이 모델은 callback을 분석할 때 중요하다.

```python
callbacks = [counter.increment]
```

이 리스트는 “나중에 Counter.increment를 찾아라”가 아니라 **현재 counter에 bound된 method 객체**를 보관한다. 객체 lifetime과 reference graph에도 영향을 줄 수 있다.

반대로 class function 자체를 저장했다면 receiver를 나중에 명시해야 한다.

```python
callback = Counter.increment
callback(counter, 2)
```

따라서 callback registry, event handler, task queue에서 bound/unbound 형태를 혼동하면 signature 오류뿐 아니라 객체가 예상보다 오래 살아 있는 retention 문제까지 생길 수 있다.

---

## 4. Argument binding — 호출 전에 parameter map이 완성된다

함수 호출은 body 첫 줄부터 시작하는 것이 아니다. 먼저 실제 인수를 parameter에 결합해야 한다.

```python
def request(method, path, timeout=3, *, retry=False):
    ...

request("GET", "/users", 5, retry=True)
```

개념적으로 다음 mapping이 만들어진다.

```text
method  ← "GET"
path    ← "/users"
timeout ← 5
retry   ← True
```

인수 개수가 맞지 않거나 같은 parameter에 positional과 keyword가 중복되면 function body에 들어가기 전에 `TypeError`가 발생한다.

```python
def f(x):
    print("body")

f(1, x=2)  # body 실행 전 TypeError
```

이 구분은 디버깅에 중요하다. stack trace에서 body 내부 오류인지 **call-site binding 오류인지**를 먼저 구별하면 탐색 범위가 좁아진다.

`*args`와 `**kwargs`는 모든 signature 문제를 해결하는 마법이 아니다. 너무 넓게 받으면 오타까지 통과하고 API 계약이 약해진다.

---

## 5. Positional-only / keyword-only — 호출자가 의존할 이름의 범위를 설계한다

Python signature는 `/`와 `*`를 통해 argument 전달 방식을 제한할 수 있다.

```python
def normalize(value, /, *, strict=False):
    ...
```

여기서 `value`는 positional-only이고 `strict`는 keyword-only다.

```python
normalize("ABC", strict=True)   # 정상
normalize(value="ABC")         # 오류
normalize("ABC", True)         # 오류
```

positional-only는 parameter 이름을 공개 API 계약으로 고정하고 싶지 않을 때 유용하다. 반대로 boolean flag나 의미가 중요한 option은 keyword-only로 강제하면 호출 위치에서 의도가 드러난다.

```python
send(payload, True, False)
```

보다

```python
send(payload, compress=True, retry=False)
```

가 읽기 쉽다.

Signature는 단순 문법 취향이 아니라 **호출자의 결합도를 어디에 둘지 결정하는 인터페이스 설계**다.

---

## 6. Signature introspection — “호출 가능”과 “이 인수를 받을 수 있음”은 다르다

`inspect.signature()`를 사용하면 많은 Python callable의 공개 signature를 조사할 수 있다.

```python
import inspect

def export(path, *, format="json"):
    pass

sig = inspect.signature(export)
print(sig)

bound = sig.bind("out.txt", format="csv")
print(bound.arguments)
```

`Signature.bind()`는 실제 body를 실행하지 않고 argument binding 규칙을 검증할 수 있다. plugin adapter, dependency injection, CLI wrapper처럼 다양한 callable을 연결하는 코드에서 유용하다.

하지만 introspection을 runtime dispatch의 전부로 사용하면 복잡해질 수 있다.

- 일부 C extension callable은 정보가 제한될 수 있다.
- decorator가 metadata를 보존하지 않으면 원래 signature가 가려질 수 있다.
- 동적 `__call__`이 넓은 `*args, **kwargs`만 노출할 수 있다.

따라서 framework에서는 introspection 실패 경로와 명시적 adapter 계약을 함께 설계하는 것이 안전하다.

---

## 7. Callable state — 함수 대신 객체를 쓰는 이유는 state lifetime에 있다

Closure와 callable object는 모두 상태를 기억할 수 있다.

```python
class RetryPolicy:
    def __init__(self, retries):
        self.retries = retries
        self.calls = 0

    def __call__(self, operation):
        self.calls += 1
        last_error = None
        for _ in range(self.retries + 1):
            try:
                return operation()
            except Exception as exc:
                last_error = exc
        raise last_error
```

이 구조에서 `calls`는 호출 사이에 유지된다. 장점은 state가 명시적 object field로 보인다는 것이다.

하지만 mutable callable을 여러 thread/task에서 공유하면 경쟁 조건이 생길 수 있다. 또한 같은 instance를 여러 독립 작업이 공유하면 counter, cache, random generator state가 서로 섞일 수 있다.

따라서 stateful callable에는 다음 lifetime 질문이 필요하다.

```text
누가 생성하는가?
몇 번 호출되는가?
여러 요청이 공유하는가?
state reset은 언제인가?
동시 호출을 허용하는가?
```

P62의 closure도 같은 관점으로 비교할 수 있다. 작은 lexical state는 closure가 간결하고, 여러 상태와 invariant·method가 필요하면 callable object가 더 읽기 쉬울 수 있다.

---

## 8. Call contract — 괄호 뒤에 숨는 경계를 최소화한다

호출 가능한 API를 리뷰할 때는 다음을 명시한다.

```text
callable kind: function / bound method / callable object
required args: ...
keyword-only options: ...
mutation: yes/no
I/O: local/remote
reentrant: yes/no
thread/task sharing: allowed/not allowed
exceptions: ...
return contract: ...
```

특히 decorator와 wrapper는 call contract를 훼손하기 쉽다.

```python
from functools import wraps

def logged(fn):
    @wraps(fn)
    def wrapper(*args, **kwargs):
        print(fn.__name__)
        return fn(*args, **kwargs)
    return wrapper
```

`wraps`는 metadata 보존에 도움을 주지만 wrapper가 원래 함수의 동기/비동기 의미, 예외, cancellation, generator 성격까지 자동 보존해 주는 것은 아니다.

호출 프로토콜의 최종 질문은 “실행되나?”가 아니다.

- receiver가 언제 binding되는가?
- argument error는 body 진입 전인지 내부인지?
- signature가 호출자의 의도를 충분히 표현하는가?
- callable object의 mutable state lifetime이 올바른가?
- wrapper가 원래 contract를 실제로 유지하는가?

이 질문이 답이 되면 `thing(...)`이라는 짧은 문법 뒤의 실행 경계를 예측할 수 있다.

---

## 직관 봉인

- callable은 함수만을 뜻하지 않는다. `__call__`을 가진 객체도 호출 문법에 참여한다.
- instance method의 `self` 결합은 이름 규칙이 아니라 function descriptor binding의 결과다.
- bound method는 receiver reference를 이미 가진다.
- argument binding 오류는 함수 body가 실행되기 전에 발생할 수 있다.
- positional-only와 keyword-only는 API 결합도와 가독성을 조절하는 장치다.
- stateful callable은 closure와 마찬가지로 lifetime·sharing·concurrency를 검토해야 한다.

## 다음 연결

다음 PART에서는 호출 전후의 자원 lifetime을 구조화하는 **context manager state machine**을 다룬다. `__enter__`, `__exit__`, 예외 정보 전달, suppress 여부, 여러 자원의 cleanup order를 `with` 실행 순서로 추적한다.
