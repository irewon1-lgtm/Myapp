# PART 45 · Dynamic resource management — ExitStack으로 가변 lifetime을 안전하게 합성하기

정해진 파일 하나를 `with`로 여는 문제는 단순하다. 실제 프로그램에서는 설정에 따라 파일이 0개일 수도 20개일 수도 있고, 일부 resource를 얻은 뒤 다음 단계에서 실패할 수도 있으며, sync와 async cleanup을 한 scope에서 조합해야 할 수 있다. 이때 중첩 `with`를 미리 고정할 수 없거나 획득 순서가 runtime에 결정된다. 핵심은 **resource 획득이 늘어날수록 cleanup 책임도 stack처럼 함께 등록되어야 한다**는 것이다.

---

## CHAPTER 01 · 가변 resource 집합은 고정된 `with A, B`만으로 표현하기 어렵다

Configuration에 적힌 여러 파일을 열거나 plugin이 요구하는 connection을 순차적으로 획득한다고 하자. resource 개수가 runtime에서 결정되면 source에 `with a, b, c`를 미리 적을 수 없다. 단순 loop로 `open()`한 뒤 list에 모으면 중간 세 번째 open에서 exception이 발생했을 때 앞의 두 파일을 누가 닫는지 별도 코드가 필요하다.

이 문제의 핵심은 resource 수가 아니라 **부분 성공**이다. 첫 단계는 성공했고 두 번째는 성공했으며 세 번째에서 실패할 수 있다. 따라서 각 성공 직후 해당 resource의 cleanup action을 등록해 두면 이후 어디서 실패해도 지금까지 획득한 것만 역순으로 정리할 수 있다. Transaction rollback log와 비슷한 사고다.

가변 lifetime을 직접 `try/finally` 여러 층으로 만들 수도 있지만 분기와 cleanup 순서가 복잡해진다. Python의 context management 도구는 이 패턴을 standard protocol로 제공해 acquisition과 release를 구조적으로 연결한다.

---

## CHAPTER 02 · ExitStack은 runtime에 context manager를 하나씩 쌓는다

`ExitStack`은 `enter_context()`를 호출할 때마다 해당 context manager의 exit 동작을 내부 stack에 등록한다. Scope가 끝나면 등록된 cleanup이 LIFO 순서로 실행된다. 따라서 runtime loop 안에서 파일이나 lock 수가 달라져도 하나의 lexical scope에서 lifetime을 관리할 수 있다.

```python
with ExitStack() as stack:
    files = [stack.enter_context(open(path)) for path in paths]
    process(files)
```

두 번째 file open 이후 세 번째에서 실패해도 앞에서 성공한 file의 exit callback이 이미 등록되어 있다. “모든 resource를 다 얻은 뒤 cleanup 준비”가 아니라 **각 acquisition 직후 cleanup 준비**가 핵심이다.

ExitStack은 resource collection 자체를 소유하기보다 종료 protocol을 조합한다. File뿐 아니라 lock, temporary directory, database transaction처럼 context manager contract를 제공하는 object를 같은 구조에서 관리할 수 있다.

---

## CHAPTER 03 · callback 등록으로 context manager가 아닌 cleanup도 같은 stack에 넣을 수 있다

모든 resource가 `__enter__/__exit__`를 제공하는 것은 아니다. 어떤 library는 `create()`와 `destroy(handle)` pair를 제공하거나 temporary global setting을 바꾼 뒤 원래 값으로 되돌리는 함수를 요구할 수 있다. ExitStack에 cleanup callback을 등록하면 이런 release action도 같은 LIFO stack에 포함할 수 있다.

Callback을 등록할 때 argument snapshot과 mutable state를 구분한다. Loop variable을 closure로 늦게 capture하면 모든 callback이 마지막 값만 참조할 수 있다. 등록 시점의 handle을 명시적 argument로 전달해 각 cleanup이 정확한 resource를 가리키게 한다.

Cleanup callback은 exception suppression protocol을 context manager와 동일하게 제공하지 않을 수 있다. 단순 release가 목적이면 충분하지만 exception 처리 semantics가 필요하다면 작은 context manager adapter를 만드는 편이 더 명확할 수 있다.

---

## CHAPTER 04 · acquisition 중 실패와 body 실행 중 실패를 같은 cleanup stack이 다룬다

Resource management 코드에서는 두 종류의 실패를 구분해야 한다. 아직 모든 resource를 얻기 전 acquisition 자체가 실패할 수 있고, 전부 얻은 뒤 업무 body에서 exception이 날 수도 있다. ExitStack은 성공적으로 등록된 resource에 대해 두 경로 모두에서 cleanup을 실행한다.

그러나 cleanup 자체도 실패할 수 있다. File close, transaction rollback, remote lease release가 exception을 만들 수 있다. 원래 업무 exception과 cleanup exception 중 어떤 정보를 보존할지 중요하다. Python의 exception chaining과 multiple-failure 모델을 활용해 원인을 잃지 않게 한다.

Cleanup이 실패했다고 다음 cleanup을 무조건 건너뛰면 나머지 resource leak이 생길 수 있다. Context management protocol과 ExitStack의 실제 exception propagation semantics를 이해하고 critical resource에는 cleanup failure logging과 recovery policy를 추가한다.

---

## CHAPTER 05 · `pop_all()`은 cleanup 책임을 다른 owner에게 이전하는 도구다

Resource를 임시 scope에서 구성한 뒤 성공 시 더 긴 lifetime의 객체에게 넘기고 싶을 수 있다. 예를 들어 connection 여러 개를 모두 성공적으로 연 뒤 service object가 이후 close 책임을 맡는 구조다. 이때 scope 종료 시 바로 cleanup하면 안 된다.

`pop_all()` 같은 operation은 현재 stack에 등록된 cleanup 책임을 새 stack으로 이전해 원래 scope가 종료되어도 resource를 유지하게 할 수 있다. 이것은 단순히 “close하지 않음”이 아니라 **ownership transfer**다. 새로운 owner가 반드시 나중에 close해야 한다.

Ownership transfer는 실패 직전까지는 임시 builder가 책임지고, construction이 완전히 성공한 뒤만 완성된 object가 책임지는 two-phase construction에 유용하다. 성공 여부와 상관없이 resource를 global list에 넣는 것보다 lifetime contract가 명확하다.

---

## CHAPTER 06 · generator 기반 context manager는 entry와 exit 코드를 한 함수에 붙인다

`contextmanager` 계열 decorator를 사용하면 `yield` 앞을 entry, 뒤를 exit/cleanup으로 구성한 generator를 context manager처럼 사용할 수 있다. 짧은 setup/teardown pair를 class 두 method보다 가까운 위치에 표현할 수 있다는 장점이 있다.

```python
@contextmanager
def temporary_mode(target, value):
    old = target.mode
    target.mode = value
    try:
        yield target
    finally:
        target.mode = old
```

핵심은 `yield` 문법 자체가 아니라 exception이 body에서 generator로 다시 전달되고 `finally` cleanup이 실행되는 protocol이다. Cleanup을 `yield` 뒤 일반 문장으로만 두고 `finally`를 사용하지 않으면 body exception handling을 잘못 구성할 수 있다.

Generator context manager가 값을 반환하는 함수와 다르게 한 번의 entry/exit lifecycle을 가진다는 점도 이해한다. 재사용 가능한 manager object가 필요한 경우 class implementation이 더 적합할 수 있다.

---

## CHAPTER 07 · AsyncExitStack은 sync와 async lifetime을 비동기 scope에 조합한다

Async application에서는 connection pool lease나 async lock처럼 `async with`가 필요한 resource가 있다. Resource 개수가 runtime에서 달라진다면 `AsyncExitStack`이 async context manager와 async callback의 종료 동작을 모을 수 있다. `await`를 포함한 cleanup도 parent scope의 종료 과정에서 순서대로 처리된다.

비동기 cleanup은 cancellation과 충돌한다. Parent task가 취소되는 동안 connection close와 transaction rollback까지 취소되면 resource가 남을 수 있다. Library의 cancellation semantics를 확인하고 정말 반드시 완료해야 하는 cleanup은 별도 protected scope가 필요한지 판단한다.

Sync resource와 async resource를 한 orchestration 함수에서 함께 다룰 때 어떤 stack이 어느 cleanup을 소유하는지 명확히 한다. Async라고 모든 sync close가 자동 awaitable이 되는 것은 아니다.

---

## CHAPTER 08 · 부분 생성 실패를 고려하면 constructor 안의 resource 획득을 줄일 이유가 보인다

객체 생성자에서 파일 세 개와 socket 두 개를 순차적으로 열다가 네 번째에서 실패하면 이미 생성된 resource를 정리할 책임이 constructor 내부에 생긴다. 완성되지 않은 object의 destructor에 cleanup을 기대하기보다 factory나 context builder가 ExitStack으로 acquisition을 관리한 뒤 완성된 object에 ownership을 이전하는 구조가 안전하다.

이 패턴은 exception-safe construction을 만든다. 모든 prerequisite를 성공적으로 얻기 전에는 외부에 object가 노출되지 않고, 실패하면 지금까지의 resource가 자동 정리된다. 성공 후에는 resource owner가 명확하다.

객체가 context manager 자체가 되어 자신의 lifetime을 소유할 수도 있다. 중요한 것은 생성·사용·종료 상태 전이가 하나의 contract로 표현되고 partially initialized state가 application 곳곳에 퍼지지 않는 것이다.

---

## CHAPTER 09 · resource stack test는 cleanup 순서와 실패 경로를 실제로 검증한다

정상 path에서 모든 resource가 닫혔다는 test만으로는 충분하지 않다. N번째 acquisition에서 exception을 강제로 발생시켜 앞에서 획득한 N-1개가 역순으로 정리되는지 확인한다. Body exception에서도 동일하게 cleanup되는지, cleanup 하나가 실패해도 필요한 나머지 정리가 어떤 순서로 실행되는지 검증한다.

Fake context manager에 enter/exit event를 기록하면 실제 sequence를 assertion할 수 있다. Resource 종류가 많아질수록 “close가 불렸음”보다 ownership과 ordering이 중요하다. Database transaction과 lock처럼 release 순서가 invariant인 경우 특히 그렇다.

Async test에서는 cancellation을 cleanup 중간에 주입해 leak 여부를 본다. Test가 끝난 뒤 열린 task, file descriptor, connection count가 baseline으로 돌아왔는지도 관측할 수 있다.

---

## CHAPTER 10 · lifetime 합성의 기준은 acquire 직후 cleanup 등록과 단일 ownership이다

동적 resource 관리가 복잡해지는 이유는 resource 종류가 많기 때문이 아니라 성공한 일부와 실패한 이후를 함께 처리해야 하기 때문이다. 강한 패턴은 resource를 얻는 즉시 cleanup을 등록하고, lifetime을 연장할 때는 명시적으로 ownership을 이전하며, scope 종료 시 LIFO 순서로 정리하는 것이다.

이 원칙은 file과 lock을 넘어 transaction, temporary configuration, plugin handle, native resource에도 적용된다. ExitStack은 그 원칙을 Python context protocol 위에서 구현한 도구다.

Dynamic lifetime 설계의 핵심은 **cleanup을 마지막 단계로 기억해 두는 것이 아니라 acquisition 순간부터 실패 경로에 포함시키고, 언제나 누가 현재 owner인지 하나로 결정하는 것**이다.