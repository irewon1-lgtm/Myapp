# PART 69 · Context manager state machine — enter·exit·exception suppression·cleanup order를 설계하기

`with`는 파일을 닫는 편의 문법을 넘어 resource acquisition과 release를 하나의 lexical scope로 묶는 프로토콜이다. Lock, transaction, temporary configuration, trace span처럼 반드시 되돌려야 하는 상태를 다룰 때 특히 강하다. 핵심은 **진입 성공 여부, body 실패, 종료 훅 호출, 예외 suppression**을 하나의 state machine으로 보는 것이다.

---

## CHAPTER 01 · context protocol은 scope 진입과 종료를 두 개의 명시적 훅으로 분리한다

동기 context manager는 `__enter__`와 `__exit__`를 통해 `with` 문에 참여한다.

```python
class Session:
    def __enter__(self):
        self.open()
        return self

    def __exit__(self, exc_type, exc, tb):
        self.close()
        return False
```

`__enter__`가 반환한 값이 `as` target에 binding되고, body가 정상 종료되거나 exception으로 빠져나갈 때 `__exit__`가 호출된다. 이 구조는 acquisition과 release를 가까이 배치해 누락 가능성을 줄인다.

Context manager가 무엇을 소유하는지 명확해야 한다. 외부에서 전달받은 resource를 빌려 쓰는 것인지, 직접 만들고 닫는 것인지가 cleanup 책임을 결정한다.

---

## CHAPTER 02 · `__enter__`는 resource acquisition 성공 이후에만 사용 가능한 값을 반환해야 한다

`__enter__` 내부에서 여러 resource를 순차적으로 얻다가 중간에 실패하면 아직 context body가 시작되지 않았으므로 `__exit__`가 일반적인 성공 진입 경로처럼 호출된다고 가정하면 안 된다. 이미 획득한 부분 resource는 `__enter__` 자체가 정리하거나 더 작은 context manager를 조합해야 한다.

```python
class LockedFile:
    def __enter__(self):
        self.file = open(self.path)
        try:
            self.lock()
        except Exception:
            self.file.close()
            raise
        return self.file
```

복잡한 acquisition chain에서는 `ExitStack` 같은 도구가 rollback registration을 순차적으로 관리하는 데 유용하다. 진입이 절반만 성공하는 실패 path를 반드시 생각한다.

`__enter__`가 `self`를 반환할지 내부 resource를 반환할지도 API 선택이다. Caller가 어떤 surface를 사용해야 하는지 기준으로 결정한다.

---

## CHAPTER 03 · `__exit__`는 exception type·instance·traceback을 함께 받는다

Context body에서 exception이 발생하면 `__exit__`는 관련 정보를 받아 cleanup과 정책 판단을 할 수 있다. 정상 종료에서는 이 값들이 exception이 없음을 나타내는 형태로 전달된다.

```python
def __exit__(self, exc_type, exc, tb):
    self.release()
    if exc_type is TimeoutError:
        self.metrics.timeout += 1
    return False
```

Cleanup은 exception 종류와 무관하게 필요한 경우가 많으므로 `release()`를 특정 branch 안에만 넣지 않는다. Logging이나 metric 기록이 원래 exception을 가리는 두 번째 failure를 만들지 않도록 주의한다.

Traceback 정보를 사용해 관찰 가능성을 높일 수 있지만 context manager가 모든 exception을 domain error로 다시 포장하면 원인 추적이 어려워질 수 있다.

---

## CHAPTER 04 · `__exit__`의 truthy 반환은 exception suppression이라는 강한 의미를 가진다

`__exit__`가 truthy 값을 반환하면 body의 exception이 suppress될 수 있다. 이는 cleanup 성공 여부를 반환하는 것이 아니다.

```python
class IgnoreMissing:
    def __exit__(self, exc_type, exc, tb):
        return exc_type is FileNotFoundError
```

예상한 좁은 exception만 의도적으로 suppress해야 한다. 무조건 `return True`를 하면 programming error, cancellation, resource corruption까지 조용히 사라질 수 있다.

Suppress한 뒤 caller가 정상 완료로 해석해도 되는지 도메인 의미를 확인한다. “오류를 로그하고 계속”이 필요한 경우에도 어떤 상태가 보장되는지 명확하지 않으면 suppression보다 explicit result가 낫다.

---

## CHAPTER 05 · 여러 context manager의 cleanup은 acquisition의 역순으로 생각한다

여러 resource가 중첩되면 나중에 얻은 resource를 먼저 정리하는 LIFO 구조가 일반적이다.

```python
with open_db() as db:
    with db.transaction() as tx:
        with acquire_lock() as lock:
            run(tx)
```

Body를 빠져나올 때 lock, transaction, database scope 순으로 정리된다. 이 순서는 dependency와 맞아야 한다. 안쪽 resource가 바깥 resource를 cleanup 중 필요로 한다면 역순 정리가 자연스럽다.

동적 개수의 context manager를 다룰 때 수동 `try/finally`를 여러 겹 쌓기보다 `ExitStack`으로 cleanup callback을 등록할 수 있다. 핵심은 “얻은 순서”와 “되돌리는 순서”를 짝지어 보는 것이다.

---

## CHAPTER 06 · generator-based context manager는 하나의 `yield`를 진입/종료 경계로 바꾼다

`contextlib.contextmanager`를 사용하면 generator 형태로 context manager를 작성할 수 있다.

```python
from contextlib import contextmanager

@contextmanager
def temporary_mode(settings, value):
    old = settings.mode
    settings.mode = value
    try:
        yield
    finally:
        settings.mode = old
```

`yield` 전은 진입, `yield`된 값은 `as` target, 이후 코드는 종료 path가 된다. 하지만 generator control과 exception 전달을 decorator가 context protocol로 변환하므로 `try/finally` 배치를 정확히 해야 한다.

여러 번 yield하거나 cleanup을 yield 이전에 잘못 배치하면 contract가 깨진다. 간단한 scope 변환에는 유용하지만 복잡한 state machine은 class 기반 implementation이 더 명시적일 수 있다.

---

## CHAPTER 07 · exception safety는 cleanup이 실행됐다는 사실보다 최종 상태 보장이 중요하다

Resource를 닫았더라도 중간 mutation이 절반만 적용됐다면 프로그램 상태는 깨질 수 있다. Transaction context는 실패 시 rollback, 성공 시 commit 같은 더 강한 invariant를 제공해야 한다.

```python
with transaction() as tx:
    tx.update_a()
    tx.update_b()
```

`update_b`가 실패하면 `update_a`의 효과를 어떻게 되돌릴지 context manager가 책임질 수 있다. 하지만 외부 API 호출처럼 rollback 불가능한 side effect가 섞이면 단순 context manager만으로 atomicity를 보장할 수 없다.

따라서 exception safety 수준을 정의한다. Resource leak만 막는 basic guarantee인지, 실패 후 object invariant를 유지하는 strong guarantee인지, transaction atomicity까지 제공하는지 구분한다.

---

## CHAPTER 08 · context contract는 ownership·suppression·reentrancy를 외부에 드러낸다

Context manager를 API로 제공할 때는 한 번만 사용할 수 있는지 재진입 가능한지, 동일 object를 여러 번 중첩해도 되는지, 어떤 exception을 suppress하는지, 종료 후 object를 다시 사용할 수 있는지 명확해야 한다.

Test에서는 정상 종료, body exception, `__enter__` 중간 실패, cleanup 자체 실패, nested cleanup order를 별도 case로 만든다. Resource counter나 fake handle을 사용하면 누락된 close를 검증하기 쉽다.

이 PART의 핵심은 **`with`를 자동 close 문법으로 축소하지 않고, acquisition·body·failure·suppression·reverse cleanup을 명시적으로 가진 resource state machine으로 설계하는 것**이다.
