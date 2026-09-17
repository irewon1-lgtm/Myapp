# TRACK 02 · P69 — Context manager state machine: enter·exit·exception suppression·cleanup을 추적하기

`with`는 들여쓰기를 예쁘게 만드는 문법이 아니다. 자원 획득과 해제, 예외 전달, suppress 여부를 하나의 구조화된 상태 전이로 묶는다. 파일, lock, transaction, temporary configuration처럼 “들어갈 때 시작하고 나올 때 반드시 정리해야 하는 것”에 특히 잘 맞는다.

이 PART에서는 `with manager as value:`를 **enter 성공 → body 실행 → exit 호출 → 예외 전파 또는 suppress** 순서로 추적한다. 자원 획득이 중간에 실패하거나 cleanup 자체가 실패하는 경우도 별도로 본다.

---

## 1. Context protocol — `with`는 진입과 종료의 쌍이다

기본적인 context manager는 `__enter__`와 `__exit__`을 구현한다.

```python
class Timer:
    def __enter__(self):
        print("start")
        return self

    def __exit__(self, exc_type, exc, tb):
        print("stop")
        return False

with Timer() as timer:
    print("work")
```

개념적 실행은 다음과 같다.

```text
manager 생성
→ __enter__ 호출
→ 반환값을 as target에 binding
→ body 실행
→ 정상/예외 여부와 함께 __exit__ 호출
→ suppress 여부 판단
```

중요한 점은 `as timer`에 들어가는 값이 반드시 manager 자신일 필요는 없다는 것이다. `__enter__`가 다른 resource handle을 반환할 수 있다.

```python
class ConnectionContext:
    def __enter__(self):
        self.conn = open_connection()
        return self.conn
```

따라서 context manager 객체와 body에서 사용하는 resource 객체를 구분해야 한다.

---

## 2. Enter resource — 획득이 성공한 자원만 exit 책임으로 넘긴다

`__enter__` 안에서 자원을 여러 개 얻으면 중간 실패가 어려워진다.

```python
class Pair:
    def __enter__(self):
        self.a = acquire_a()
        self.b = acquire_b()  # 여기서 실패하면 a는 누가 정리할까?
        return self.a, self.b
```

`__enter__` 자체가 예외로 끝나면 일반적인 `with` 흐름의 `__exit__`가 호출된다고 기대해서는 안 된다. 따라서 enter 내부에서 이미 획득한 자원은 enter 실패 경로가 직접 정리해야 한다.

```python
class Pair:
    def __enter__(self):
        self.a = acquire_a()
        try:
            self.b = acquire_b()
        except Exception:
            release_a(self.a)
            raise
        return self.a, self.b
```

자원이 많아질수록 수동 rollback은 복잡해진다. `contextlib.ExitStack`은 성공적으로 획득한 cleanup을 stack에 등록해 부분 실패를 구조적으로 처리하는 데 유용하다.

핵심 invariant는 다음이다.

```text
획득 성공 횟수 == 종료 시 해제 책임 수
```

단, ownership을 다른 객체로 명시적으로 이전한 경우는 별도다.

---

## 3. Exit exception triple — body의 실패 정보가 종료 경계로 전달된다

body에서 예외가 발생하면 `__exit__(exc_type, exc, tb)`는 예외 타입, 예외 객체, traceback 정보를 받는다.

```python
class Reporter:
    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        if exc is not None:
            print("failed:", type(exc).__name__, exc)
        return False
```

정상 종료라면 이 인수들은 `None` 계열로 들어온다.

이 구조 덕분에 context manager는 body 결과를 모른 채도 성공/실패에 따라 다른 cleanup을 할 수 있다. transaction이 대표적이다.

```text
enter  → transaction begin
body 성공 → commit
body 실패 → rollback
exit  → connection state 정리
```

그러나 `__exit__`에서 예외를 로깅한다고 해서 자동으로 원래 예외가 사라지는 것은 아니다. **반환값이 suppress를 결정**한다.

또 traceback 객체를 장기간 저장하면 reference cycle이나 큰 object graph retention으로 이어질 수 있으므로 관찰 목적으로 영구 보관하는 설계는 신중해야 한다.

---

## 4. Suppression return — truthy 반환은 예외를 삼킬 수 있다

`__exit__`가 truthy를 반환하면 body에서 발생한 예외를 suppress하는 의미가 된다.

```python
class IgnoreValueError:
    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return exc_type is ValueError

with IgnoreValueError():
    raise ValueError("ignored")
```

이 기능은 강력해서 범위를 좁혀야 한다. 다음처럼 무조건 `True`를 반환하면 예상치 못한 bug까지 사라진다.

```python
    def __exit__(self, exc_type, exc, tb):
        log(exc)
        return True
```

`NameError`, `TypeError`, assertion failure까지 모두 정상 흐름으로 바뀔 수 있다.

Suppress가 필요한 경우에는 허용할 예외와 의미를 명시한다.

```text
suppressed: FileNotFoundError when removing optional temp file
propagated: PermissionError, OSError, programming errors
```

대부분의 resource cleanup context manager는 `False` 또는 `None`을 반환해 원래 예외를 유지하는 쪽이 기본이다.

---

## 5. Cleanup order — 여러 context는 역순으로 빠져나온다

중첩 context에서는 나중에 들어간 자원부터 먼저 빠져나오는 stack 구조가 자연스럽다.

```python
with open_a() as a:
    with open_b() as b:
        use(a, b)
```

개념적으로:

```text
enter A
enter B
body
exit B
exit A
```

이 LIFO 순서는 의존 자원 정리에 중요하다. 예를 들어 B가 A를 이용해 만들어진 resource라면 B를 먼저 닫고 A를 닫아야 한다.

여러 context를 한 줄에 써도 ownership 순서를 의식해야 한다.

```python
with open_a() as a, open_b(a) as b:
    ...
```

Cleanup 자체가 실패하면 더 복잡하다. body의 원래 예외가 있었는데 `__exit__`에서 새 예외가 발생하면 exception chaining을 통해 두 실패 관계를 추적해야 한다. cleanup 코드에서 불필요한 실패를 만들지 않도록 idempotent close, 존재 여부 검사, 좁은 예외 처리 같은 설계가 필요하다.

---

## 6. Generator context manager — 한 번의 yield를 enter/exit 경계로 변환한다

`contextlib.contextmanager`는 generator 형태로 context manager를 작성하게 해 준다.

```python
from contextlib import contextmanager

@contextmanager
def opened(path):
    f = open(path, "w")
    try:
        yield f
    finally:
        f.close()
```

`yield` 전은 enter 단계, `yield`된 값은 `as` target, `yield` 뒤는 exit/cleanup 단계로 생각할 수 있다.

하지만 일반 generator와 동일하게 여러 번 yield하는 용도로 쓰는 것이 아니다. contextmanager contract는 한 번의 진입과 한 번의 종료를 구조화한다.

예외가 body에서 발생하면 generator 쪽에는 그 실패가 전달되어 `try/finally` 또는 `except`로 처리할 수 있다. 이때 예외를 실수로 소비해 버리지 않도록 주의해야 한다.

Class 기반 context manager가 더 나은 경우도 있다.

- 상태가 여러 개이고 method가 필요함
- 재사용/재진입 정책을 명확히 해야 함
- 타입으로 contract를 드러내고 싶음

Generator 기반은 작은 획득/정리 쌍에서 특히 간결하다.

---

## 7. Exception safety — cleanup이 correctness의 마지막 방어선이다

Context manager의 목적은 단지 `close()` 호출 누락을 줄이는 것이 아니다. body가 어디에서 실패해도 자원 invariant를 복원하는 것이다.

파일을 쓰는 예:

```python
with open(temp_path, "w") as f:
    write_payload(f)
    validate(f)
```

파일 close는 보장할 수 있지만 “최종 파일이 항상 완전하다”는 것은 별도 문제다. 안전한 publish가 필요하면 temporary file에 쓴 뒤 validate하고 atomic rename하는 경계를 추가해야 한다.

Database context도 마찬가지다.

```text
connection close 보장 ≠ transaction correctness 보장
```

`with`는 lifetime을 구조화하지만 domain rollback 전략까지 자동으로 만들어 주지는 않는다.

따라서 exception safety를 세 층으로 본다.

1. **resource safety** — handle/lock/file을 놓치지 않는다.
2. **state safety** — 실패 시 in-memory/transaction 상태가 유효하다.
3. **publication safety** — 외부에 partial result를 노출하지 않는다.

Context manager는 첫 번째를 강하게 돕고, 나머지는 별도 invariant 설계가 필요하다.

---

## 8. Context contract — ownership과 suppress 정책을 명시한다

Context manager를 만들기 전에 다음 표를 적는다.

```text
acquire: database transaction
enter return: transaction handle
owner: context manager
normal exit: commit
exception exit: rollback
suppressed exceptions: none
cleanup failure: propagate with chaining
reentrant: no
reusable instance: no
```

Lock context라면 `enter = acquire`, `exit = release`처럼 더 단순할 수 있다. Temporary configuration context라면 이전 값을 저장했다가 exit에서 복원한다.

테스트는 최소 네 경로를 분리해야 한다.

```text
A. enter 성공 + body 성공 + exit 성공
B. enter 성공 + body 실패 + exit 성공
C. enter 부분 실패
D. cleanup 실패
```

Suppress 기능이 있다면 E로 “허용 예외만 suppress, 다른 예외는 전파”를 추가한다.

이렇게 해야 happy path에서 `with`가 실행됐다는 사실과 **failure path에서도 ownership이 닫힌다**는 사실을 구분해서 검증할 수 있다.

---

## 직관 봉인

- `with`는 enter/body/exit의 구조화된 state machine이다.
- `__enter__`가 실패하면 이미 얻은 자원을 스스로 rollback해야 할 수 있다.
- `__exit__`는 body 예외 정보를 받지만, 예외 suppress 여부는 반환값이 결정한다.
- truthy `__exit__`를 남용하면 programming error까지 숨길 수 있다.
- 여러 context의 cleanup은 의존 관계를 고려한 LIFO 순서로 이해한다.
- resource cleanup과 transaction/domain correctness는 같은 문제가 아니다.

## 다음 연결

다음 PART에서는 같은 lifetime 구조가 `await` suspension을 포함할 때 어떻게 달라지는지 본다. `async for`의 `__aiter__`·`__anext__`, `async with`의 `__aenter__`·`__aexit__`, cancellation 중 cleanup을 **비동기 프로토콜 경계**로 추적한다.
