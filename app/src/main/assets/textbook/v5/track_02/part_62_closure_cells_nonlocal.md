# PART 62 · Closure cell과 nonlocal — 함수가 바깥 binding을 기억하는 실제 구조

Closure를 “함수 안의 함수”로만 이해하면 loop 안 lambda가 모두 같은 값을 보는 문제, outer function이 끝났는데 local 값이 살아 있는 이유, `nonlocal`이 필요한 시점을 정확히 설명하기 어렵다. Closure의 핵심은 inner function code object가 **현재 값 하나를 복사하는 것이 아니라 enclosing scope의 특정 binding을 나중에도 참조할 수 있게 보존하는 것**이다. 이 모델을 이해하면 callback factory와 decorator state를 더 안전하게 설계할 수 있다.

---

## CHAPTER 01 · closure cell은 enclosing binding을 함수 lifetime 밖까지 보존한다

### 시작 전 용어집

#### 1. closure cell

- **뜻:** Outer function이 local variable `factor`를 만들고 inner function이 이를 참조한 뒤 inner function을 반환하면 outer call은 끝나도 `factor`에 필요한 storage가 사라지지 않는다.
- **왜 중요한가:** Inner function의 closure가 그 binding을 보존하기 때문이다.
- **예시:** def multiplier(factor): / def apply(value):

#### 2. enclosing binding

- **뜻:** 이때 inner function은 단순 code pointer가 아니라 code와 lexical environment의 일부를 함께 가진 callable이 된다.
- **왜 중요한가:** `multiplier(3)`과 `multiplier(5)`는 같은 inner code를 사용할 수 있지만 서로 다른 closure environment를 가진다.
- **예시:** def multiplier(factor): / def apply(value):

#### 3. 함수

- **뜻:** 각각의 `factor` binding이 독립적이므로 behavior가 다르다.
- **왜 중요한가:** Class instance 없이도 configuration과 behavior를 묶을 수 있는 이유다.
- **예시:** def multiplier(factor): / def apply(value):

#### 4. lifetime

- **뜻:** 실행 모델을 이해하면 closure가 어떤 object를 붙잡아 lifetime을 늘리는지 추적할 수 있다.
- **왜 중요한가:** Outer local이 모두 closure에 보존되는 것은 아니다.
- **예시:** def multiplier(factor): / def apply(value):

```python
def multiplier(factor):
    def apply(value):
        return value * factor
    return apply
```

  

 Inner function이 실제로 참조하는 free variable만 cell-like storage가 필요하다. 

---

## CHAPTER 02 · free variable은 현재 local도 global도 아닌 lexical binding에서 해결된다

### 시작 전 용어집

#### 1. free variable

- **뜻:** Function body에서 이름을 읽을 때 local binding이 없고 enclosing function scope에서 찾는 이름을 free variable 관점으로 볼 수 있다.
- **왜 중요한가:** Python compiler는 scope 분석 단계에서 local, cell, free, global 관계를 정하고 runtime frame/closure가 이 정보를 사용한다.
- **예시:** Function body에서 이름을 읽을 때 local binding이 없고 …

#### 2. local

- **뜻:** Inner function 안에서 outer 이름을 읽기만 하는 것은 자연스럽지만 같은 이름에 assignment를 하면 기본적으로 inner local binding으로 판단될 수 있다.
- **왜 중요한가:** 이 때문에 assignment 이전 read가 `UnboundLocalError`를 만들기도 한다.
- **예시:** Inner function 안에서 outer 이름을 읽기만 하는 것은 …

#### 3. global

- **뜻:** Scope bug를 디버깅할 때 값만 보지 말고 그 이름이 local인지 free variable인지 global인지 확인한다.
- **왜 중요한가:** 동일한 spelling이라도 서로 다른 namespace binding일 수 있다.
- **예시:** Scope bug를 디버깅할 때 값만 보지 말고 그 …

#### 4. lexical binding

- **뜻:** 그래서 이름 lookup을 매번 dictionary 전체에서 무작정 검색하는 것으로 이해하면 정확하지 않다.
- **왜 중요한가:** 어떤 scope의 이름을 대상으로 하는지 compile-time scope rule이 먼저 결정되기 때문이다.
- **예시:** 그래서 이름 lookup을 매번 dictionary 전체에서 무작정 검색하는 …

---

## CHAPTER 03 · late binding은 closure가 loop 시점의 값이 아니라 binding을 나중에 읽기 때문에 생긴다

### 시작 전 용어집

#### 1. late binding

- **뜻:** Loop 안에서 여러 lambda를 만들며 `i`를 참조하면 각 lambda가 생성 순간의 숫자를 자동 복사한다고 생각하기 쉽다.
- **왜 중요한가:** 실제로는 여러 closure가 동일한 `i` binding을 참조할 수 있고 호출 시점에 그 binding의 현재 값을 읽는다.
- **예시:** funcs = [] / for i in range(3):

#### 2. closure

- **뜻:** Loop가 끝난 뒤 모두 호출하면 마지막 값이 반복되는 전형적인 현상이 생긴다.
- **왜 중요한가:** 호출별 값 snapshot이 필요하다면 default argument `lambda i=i: i`처럼 생성 시점 값을 새로운 parameter default에 고정하거나 factory function을 한 번 더 호출해 독립 scope를 만든다.
- **예시:** funcs = [] / for i in range(3):

#### 3. loop

- **뜻:** 해결법을 외우기보다 “binding 공유를 끊고 값 snapshot을 새 storage에 만든다”는 원리로 이해한다.
- **왜 중요한가:** Callback이 최신 configuration 값을 읽어야 한다면 같은 binding을 나중에 조회하는 성질이 의도일 수 있다.
- **예시:** funcs = [] / for i in range(3):

#### 4. 반복

- **뜻:** Snapshot과 live reference 중 어떤 semantics가 필요한지 결정한다.
- **예시:** funcs = [] / for i in range(3):

```python
funcs = []
for i in range(3):
    funcs.append(lambda: i)
```

 

Late binding이 항상 문제는 아니다.  

---

## CHAPTER 04 · `nonlocal`은 enclosing binding을 재바인딩하겠다는 선언이다

### 시작 전 용어집

#### 1. nonlocal

- **뜻:** Counter closure처럼 inner function이 outer state를 읽고 수정해야 한다면 `nonlocal`을 사용할 수 있다.
- **왜 중요한가:** 이것은 outer object를 mutate하는 권한이 아니라 **해당 이름 assignment가 새 inner local을 만들지 않고 enclosing scope binding을 바꾸도록 하는 선언**이다.
- **예시:** def make_counter(): / count = 0

#### 2. enclosing binding

- **뜻:** 여러 반환 function이 같은 `count` cell을 공유하면 하나가 바꾼 state를 다른 함수도 볼 수 있다.
- **왜 중요한가:** 작은 private state machine을 closure로 만들 수 있지만 concurrent call이 있다면 synchronization과 reentrancy를 고려해야 한다.
- **예시:** def make_counter(): / count = 0

#### 3. closure

- **뜻:** Mutable list를 outer scope에서 `append`하는 경우에는 name rebinding이 아니라 object mutation이므로 nonlocal이 필요하지 않을 수 있다.
- **왜 중요한가:** 다시 한 번 binding과 mutation을 분리하면 규칙이 정리된다.
- **예시:** def make_counter(): / count = 0

```python
def make_counter():
    count = 0
    def next_value():
        nonlocal count
        count += 1
        return count
    return next_value
```

 

 

---

## CHAPTER 05 · closure factory는 configuration을 behavior에 고정하는 lightweight object가 될 수 있다

### 시작 전 용어집

#### 1. closure

- **뜻:** Tax rate, validation pattern, retry predicate처럼 configuration 한두 개와 callable behavior를 묶고 싶을 때 closure factory가 class보다 간결할 수 있다.
- **왜 중요한가:** Factory에서 validation을 한 번 수행하고 유효한 config만 capture하면 이후 호출은 이미 검증된 state를 사용한다.
- **예시:** Tax rate, validation pattern, retry predicate처럼 configuration 한두 …

#### 2. configuration

- **뜻:** Configuration snapshot이 목적이라면 immutable value로 변환하거나 필요한 scalar를 복사한다.
- **왜 중요한가:** Live configuration이 목적이면 그 변화 가능성을 contract에 적는다.
- **예시:** Configuration snapshot이 목적이라면 immutable value로 변환하거나 필요한 scalar를 …

#### 3. behavior

- **뜻:** Factory가 mutable config object를 그대로 capture하면 caller가 나중에 그 object를 수정해 behavior가 변할 수 있다.
- **왜 중요한가:** 하지만 여러 operation, rich introspection, lifecycle method가 필요해지면 callable object/class가 더 명확할 수 있다.
- **예시:** Factory가 mutable config object를 그대로 capture하면 caller가 나중에 …

#### 4. lightweight object

- **뜻:** Closure state는 debugger에서 보이지만 일반 attribute interface보다 덜 직접적이고 serialization도 제한적일 수 있다.
- **왜 중요한가:** “함수형이 더 좋다”가 아니라 state surface와 확장 요구를 비교한다.
- **예시:** Closure state는 debugger에서 보이지만 일반 attribute interface보다 덜 …

---

## CHAPTER 06 · closure는 captured object의 lifetime을 예상보다 길게 만들 수 있다

### 시작 전 용어집

#### 1. closure

- **뜻:** GUI callback, event handler, async task에 closure를 등록하면 registry가 function을 보관하고 function이 큰 object graph를 capture해 memory retention을 만들 수 있다.
- **왜 중요한가:** 화면 object 전체를 `self`로 capture한 callback 하나가 unregister되지 않아 화면 tree가 계속 살아 있는 경우를 생각할 수 있다.
- **예시:** Memory leak을 조사할 때 callback registry→function→closure cell→captured object라는 …

#### 2. captured object

- **뜻:** Memory leak을 조사할 때 callback registry→function→closure cell→captured object라는 reference path를 확인한다.
- **왜 중요한가:** 필요한 ID나 small value만 capture하고 service/object 전체가 필요하지 않다면 lifetime coupling을 줄일 수 있다.
- **예시:** Memory leak을 조사할 때 callback registry→function→closure cell→captured object라는 …

#### 3. lifetime

- **뜻:** Closure lifetime은 outer function call lifetime과 다르다.
- **왜 중요한가:** Outer가 반환됐다는 사실은 captured object가 해제되었다는 증거가 아니다.
- **예시:** Closure lifetime은 outer function call lifetime과 다르다.

#### 4. cell

- **뜻:** Weak reference가 적합한 observer relation인지도 검토한다.
- **왜 중요한가:** 어떤 long-lived owner가 closure를 저장하는지가 실제 lifetime을 결정한다.
- **예시:** Weak reference가 적합한 observer relation인지도 검토한다.

---

## CHAPTER 07 · callback capture는 async/concurrency에서 stale state와 race를 만들 수 있다

### 시작 전 용어집

#### 1. callback capture

- **뜻:** Callback을 schedule할 때 current user ID와 mutable request object를 모두 capture했다고 하자.
- **왜 중요한가:** Callback이 몇 초 뒤 실행될 때 request object가 이미 다른 상태로 변했거나 scope가 논리적으로 끝났을 수 있다.
- **예시:** Callback을 schedule할 때 current user ID와 mutable request …

#### 2. async

- **뜻:** 실행 시점 state를 읽어야 하는지 schedule 시점 snapshot을 사용해야 하는지 선택한다.
- **왜 중요한가:** Loop에서 task callback을 만들 때 late binding까지 겹치면 모든 callback이 마지막 job ID를 처리하는 오류가 생길 수 있다.
- **예시:** 실행 시점 state를 읽어야 하는지 schedule 시점 snapshot을 …

#### 3. concurrency

- **뜻:** Factory/default snapshot으로 ID를 고정하고 mutable payload는 immutable DTO로 변환하는 편이 안전하다.
- **왜 중요한가:** 여러 thread가 같은 closure state를 수정하면 일반 shared mutable state와 동일하게 race가 가능하다.
- **예시:** Factory/default snapshot으로 ID를 고정하고 mutable payload는 immutable DTO로 …

#### 4. stale state

- **뜻:** Closure가 private하다는 사실은 thread-safe하다는 뜻이 아니다.
- **왜 중요한가:** Lock이나 single-owner execution model이 필요하다.
- **예시:** Closure가 private하다는 사실은 thread-safe하다는 뜻이 아니다.

---

## CHAPTER 08 · closure contract는 captured state의 identity·lifetime·mutation을 명시해야 한다

### 시작 전 용어집

#### 1. closure

- **뜻:** Closure를 API로 반환할 때 caller는 parameter와 return만 볼 수 있지만 실제 behavior는 captured environment에도 의존한다.
- **왜 중요한가:** 어떤 값이 snapshot인지 live reference인지, callable 여러 개가 같은 state를 공유하는지, 호출이 state를 변경하는지 설명할 수 있어야 한다.
- **예시:** Closure를 API로 반환할 때 caller는 parameter와 return만 볼 …

#### 2. captured state

- **뜻:** Test에서는 factory를 두 번 호출해 state가 독립적인지, 같은 factory call에서 나온 두 function이 state를 공유해야 하는지 검증한다.
- **왜 중요한가:** Late binding과 outer mutation boundary도 explicit case로 만든다.
- **예시:** Test에서는 factory를 두 번 호출해 state가 독립적인지, 같은 …

#### 3. identity

- **뜻:** Closure의 핵심은 **함수가 code만 가진다는 모델을 넘어 lexical binding을 함께 보존하는 객체라는 사실을 이해하고, 그 숨은 state의 lifetime과 공유 범위를 class field와 같은 수준으로 설계하는 것**이다.
- **예시:** Closure의 핵심은 **함수가 code만 가진다는 모델을 넘어 lexical …
