# PART 69 · Context manager state machine — enter·exit·exception suppression·cleanup order를 설계하기

`with`는 파일을 닫는 편의 문법을 넘어 resource acquisition과 release를 하나의 lexical scope로 묶는 프로토콜이다. Lock, transaction, temporary configuration, trace span처럼 반드시 되돌려야 하는 상태를 다룰 때 특히 강하다. 핵심은 **진입 성공 여부, body 실패, 종료 훅 호출, 예외 suppression**을 하나의 state machine으로 보는 것이다.

---

## CHAPTER 01 · context protocol은 scope 진입과 종료를 두 개의 명시적 훅으로 분리한다

### 시작 전 용어집

#### 1. context protocol

- **뜻:** 동기 context manager는 `__enter__`와 `__exit__`를 통해 `with` 문에 참여한다.
- **왜 중요한가:** `__enter__`가 반환한 값이 `as` target에 binding되고, body가 정상 종료되거나 exception으로 빠져나갈 때 `__exit__`가 호출된다.
- **예시:** class Session: / def __enter__(self):

#### 2. scope

- **뜻:** 이 구조는 acquisition과 release를 가까이 배치해 누락 가능성을 줄인다.
- **왜 중요한가:** Context manager가 무엇을 소유하는지 명확해야 한다.
- **예시:** class Session: / def __enter__(self):

#### 3. context manager

- **뜻:** 외부에서 전달받은 resource를 빌려 쓰는 것인지, 직접 만들고 닫는 것인지가 cleanup 책임을 결정한다.
- **예시:** class Session: / def __enter__(self):

```python
class Session:
    def __enter__(self):
        self.open()
        return self

    def __exit__(self, exc_type, exc, tb):
        self.close()
        return False
```

 

 

---

**직접 확인하기 — CHAPTER 01 · context protocol은 scope 진입과 종료를 두 개의 명시적 훅으로 분리한다**
CHAPTER 01 · context protocol은 scope 진입과 종료를 두 개의 명시적 훅으로 분리한다의 규칙은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 확인하는 편이 정확하다. 먼저 입력이나 객체 하나만 두고 결과를 기록한 뒤, 값 하나 또는 호출 순서 하나만 바꿔 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 01 · context protocol은 scope 진입과 종료를 두 개의 명시적 훅으로 분리한다이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 결과가 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 확인하고, 수정 뒤 같은 예제와 반대 조건 예제를 다시 실행한다. 이 과정을 설명할 수 있어야 문법을 외운 것이 아니라 동작 원리를 이해한 것이다.
## CHAPTER 02 · `__enter__`는 resource acquisition 성공 이후에만 사용 가능한 값을 반환해야 한다

### 시작 전 용어집

#### 1. __enter__

- **뜻:** `__enter__` 내부에서 여러 resource를 순차적으로 얻다가 중간에 실패하면 아직 context body가 시작되지 않았으므로 `__exit__`가 일반적인 성공 진입 경로처럼 호출된다고 가정하면 안 된다.
- **왜 중요한가:** 이미 획득한 부분 resource는 `__enter__` 자체가 정리하거나 더 작은 context manager를 조합해야 한다.
- **예시:** class LockedFile: / def __enter__(self):

#### 2. resource

- **뜻:** `__enter__`가 `self`를 반환할지 내부 resource를 반환할지도 API 선택이다.
- **왜 중요한가:** Caller가 어떤 surface를 사용해야 하는지 기준으로 결정한다.
- **예시:** class LockedFile: / def __enter__(self):

#### 3. 실패

- **뜻:** 진입이 절반만 성공하는 실패 path를 반드시 생각한다.
- **왜 중요한가:** 복잡한 acquisition chain에서는 `ExitStack` 같은 도구가 rollback registration을 순차적으로 관리하는 데 유용하다.
- **예시:** class LockedFile: / def __enter__(self):

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

 

 

---

## CHAPTER 03 · `__exit__`는 exception type·instance·traceback을 함께 받는다

### 시작 전 용어집

#### 1. __exit__

- **뜻:** Context body에서 exception이 발생하면 `__exit__`는 관련 정보를 받아 cleanup과 정책 판단을 할 수 있다.
- **왜 중요한가:** 정상 종료에서는 이 값들이 exception이 없음을 나타내는 형태로 전달된다.
- **예시:** def __exit__(self, exc_type, exc, tb): / self.release()

#### 2. exception

- **뜻:** Cleanup은 exception 종류와 무관하게 필요한 경우가 많으므로 `release()`를 특정 branch 안에만 넣지 않는다.
- **왜 중요한가:** Logging이나 metric 기록이 원래 exception을 가리는 두 번째 failure를 만들지 않도록 주의한다.
- **예시:** def __exit__(self, exc_type, exc, tb): / self.release()

#### 3. instance

- **뜻:** Traceback 정보를 사용해 관찰 가능성을 높일 수 있지만 context manager가 모든 exception을 domain error로 다시 포장하면 원인 추적이 어려워질 수 있다.
- **예시:** def __exit__(self, exc_type, exc, tb): / self.release()

```python
def __exit__(self, exc_type, exc, tb):
    self.release()
    if exc_type is TimeoutError:
        self.metrics.timeout += 1
    return False
```

 


---

## CHAPTER 04 · `__exit__`의 truthy 반환은 exception suppression이라는 강한 의미를 가진다

### 시작 전 용어집

#### 1. __exit__

- **뜻:** `__exit__`가 truthy 값을 반환하면 body의 exception이 suppress될 수 있다.
- **왜 중요한가:** 이는 cleanup 성공 여부를 반환하는 것이 아니다.
- **예시:** class IgnoreMissing: / def __exit__(self, exc_type, exc, tb):

#### 2. truthy

- **뜻:** 예상한 좁은 exception만 의도적으로 suppress해야 한다.
- **왜 중요한가:** 무조건 `return True`를 하면 programming error, cancellation, resource corruption까지 조용히 사라질 수 있다.
- **예시:** class IgnoreMissing: / def __exit__(self, exc_type, exc, tb):

#### 3. exception

- **뜻:** Suppress한 뒤 caller가 정상 완료로 해석해도 되는지 도메인 의미를 확인한다.
- **왜 중요한가:** “오류를 로그하고 계속”이 필요한 경우에도 어떤 상태가 보장되는지 명확하지 않으면 suppression보다 explicit result가 낫다.
- **예시:** class IgnoreMissing: / def __exit__(self, exc_type, exc, tb):

```python
class IgnoreMissing:
    def __exit__(self, exc_type, exc, tb):
        return exc_type is FileNotFoundError
```

 

 

---

## CHAPTER 05 · 여러 context manager의 cleanup은 acquisition의 역순으로 생각한다

### 시작 전 용어집

#### 1. context manager

- **뜻:** 동적 개수의 context manager를 다룰 때 수동 `try/finally`를 여러 겹 쌓기보다 `ExitStack`으로 cleanup callback을 등록할 수 있다.
- **왜 중요한가:** 핵심은 “얻은 순서”와 “되돌리는 순서”를 짝지어 보는 것이다.
- **예시:** with open_db() as db: / with db.transaction() as …

#### 2. cleanup

- **뜻:** 안쪽 resource가 바깥 resource를 cleanup 중 필요로 한다면 역순 정리가 자연스럽다.
- **왜 중요한가:** 여러 resource가 중첩되면 나중에 얻은 resource를 먼저 정리하는 LIFO 구조가 일반적이다.
- **예시:** with open_db() as db: / with db.transaction() as …

#### 3. acquisition

- **뜻:** Body를 빠져나올 때 lock, transaction, database scope 순으로 정리된다.
- **예시:** with open_db() as db: / with db.transaction() as …

```python
with open_db() as db:
    with db.transaction() as tx:
        with acquire_lock() as lock:
            run(tx)
```

 이 순서는 dependency와 맞아야 한다. 

 

---

## CHAPTER 06 · generator-based context manager는 하나의 `yield`를 진입/종료 경계로 바꾼다

### 시작 전 용어집

#### 1. generator

- **뜻:** contextmanager`를 사용하면 generator 형태로 context manager를 작성할 수 있다.
- **왜 중요한가:** `yield` 전은 진입, `yield`된 값은 `as` target, 이후 코드는 종료 path가 된다.
- **예시:** from contextlib import contextmanager / @contextmanager

#### 2. context manager

- **뜻:** 하지만 generator control과 exception 전달을 decorator가 context protocol로 변환하므로 `try/finally` 배치를 정확히 해야 한다.
- **왜 중요한가:** 여러 번 yield하거나 cleanup을 yield 이전에 잘못 배치하면 contract가 깨진다.
- **예시:** from contextlib import contextmanager / @contextmanager

#### 3. yield

- **뜻:** 간단한 scope 변환에는 유용하지만 복잡한 state machine은 class 기반 implementation이 더 명시적일 수 있다.
- **예시:** from contextlib import contextmanager / @contextmanager

`contextlib.

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

 

 

---

## CHAPTER 07 · exception safety는 cleanup이 실행됐다는 사실보다 최종 상태 보장이 중요하다

### 시작 전 용어집

#### 1. exception

- **뜻:** 따라서 exception safety 수준을 정의한다.
- **왜 중요한가:** Resource leak만 막는 basic guarantee인지, 실패 후 object invariant를 유지하는 strong guarantee인지, transaction atomicity까지 제공하는지 구분한다.
- **예시:** with transaction() as tx: / tx.update_a()

#### 2. cleanup

- **뜻:** Resource를 닫았더라도 중간 mutation이 절반만 적용됐다면 프로그램 상태는 깨질 수 있다.
- **왜 중요한가:** Transaction context는 실패 시 rollback, 성공 시 commit 같은 더 강한 invariant를 제공해야 한다.
- **예시:** with transaction() as tx: / tx.update_a()

#### 3. 상태

- **뜻:** `update_b`가 실패하면 `update_a`의 효과를 어떻게 되돌릴지 context manager가 책임질 수 있다.
- **왜 중요한가:** 하지만 외부 API 호출처럼 rollback 불가능한 side effect가 섞이면 단순 context manager만으로 atomicity를 보장할 수 없다.
- **예시:** with transaction() as tx: / tx.update_a()

```python
with transaction() as tx:
    tx.update_a()
    tx.update_b()
```

 

 

---

## CHAPTER 08 · context contract는 ownership·suppression·reentrancy를 외부에 드러낸다

### 시작 전 용어집

#### 1. context contract

- **뜻:** Context manager를 API로 제공할 때는 한 번만 사용할 수 있는지 재진입 가능한지, 동일 object를 여러 번 중첩해도 되는지, 어떤 exception을 suppress하는지, 종료 후 object를 다시 사용할 수 있는지 명확해야 한다.
- **왜 중요한가:** Test에서는 정상 종료, body exception, `__enter__` 중간 실패, cleanup 자체 실패, nested cleanup order를 별도 case로 만든다.
- **예시:** Context manager를 API로 제공할 때는 한 번만 사용할 …

#### 2. ownership

- **뜻:** Resource counter나 fake handle을 사용하면 누락된 close를 검증하기 쉽다.
- **왜 중요한가:** 이 PART의 핵심은 **`with`를 자동 close 문법으로 축소하지 않고, acquisition·body·failure·suppression·reverse cleanup을 명시적으로 가진 resource state machine으로 설계하는 것**이다.
- **예시:** Resource counter나 fake handle을 사용하면 누락된 close를 검증하기 …

---

## 실전 학습 루프 · context manager state machine

### 1. 쉬운 예

파일이나 lock은 “열기 성공 → 사용 → 정리”라는 수명을 가진다. 중간에서 예외가 나도 정리가 실행돼야 하므로 `with`는 단순 문법 축약이 아니라 enter/exit 상태 전이를 코드 구조로 고정한다.

### 2. 한 줄 해석

context manager는 자원 획득과 해제를 한 lexical scope에 묶어 정상·예외 경로에서 같은 cleanup 계약을 지키게 한다.

### 3. 직접 실행

아래 코드는 개념을 작게 격리한 예다. 실행 전에 출력이나 상태 변화를 먼저 예상한 뒤 실제 결과와 비교한다.

```python
class Guard:
    def __enter__(self):
        print('enter')
        return self
    def __exit__(self, exc_type, exc, tb):
        print('exit', exc_type)
        return False

with Guard():
    print('work')
```

결과가 예상과 다르면 문법부터 고치지 말고, **어떤 protocol·상태·계약이 호출됐는지**를 한 단계씩 확인한다. 이렇게 해야 “우연히 동작하는 코드”와 “이유를 설명할 수 있는 코드”를 구분할 수 있다.

### 4. 수정 실습

1. 본문에서 예외를 발생시켜 `__exit__`가 받는 값을 관찰한다.
2. `__exit__`가 True를 반환하게 바꾸고 예외 전파가 어떻게 달라지는지 확인한다.

수정 후에는 정상 입력 하나만 보지 말고 빈 값, 경계값, 반복 호출, 예외 경로 중 해당되는 반례를 최소 하나 추가한다.

### 5. 확인 문제

`with` 블록 안에서 예외가 나면 cleanup이 생략될까?

### 6. 정답과 오답 설명

**정답:** 정상적인 context manager라면 `__exit__`가 호출된다. 예외를 삼킬지는 반환값과 구현 정책이 결정한다.

**자주 나오는 오답:** `with`를 단순한 `try` 축약이라고만 외우면 자원 수명과 예외 억제 계약을 놓친다.

마지막으로 코드를 다시 읽으면서 **입력 → 호출되는 규칙 → 상태 변화 → 결과/예외** 네 칸으로 요약한다. 이 네 칸을 설명할 수 있으면 단순 암기가 아니라 실행 모델을 이해한 것이다.

