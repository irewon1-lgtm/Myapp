# PART 21 · Expression과 함수 합성 — 값을 만드는 흐름을 상태 변경과 분리하기

Python 프로그램은 statement만 순서대로 실행하는 것이 아니라 expression을 평가해 값을 만들고 그 값을 다시 다른 expression의 입력으로 연결한다. 계산 중심 코드를 작은 함수와 transformation으로 구성하면 중간 상태를 줄이고 데이터 흐름을 직접 읽을 수 있다. 다만 한 줄로 압축하는 것이 목표가 아니라 **평가 순서와 부작용을 예측할 수 있는 값의 흐름**을 만드는 것이 목표다.

---

## CHAPTER 01 · expression은 평가되어 하나의 객체를 만든다

### 시작 전 용어집

#### 1. expression

- **뜻:** Literal, 이름 조회, 함수 호출, 연산자, comprehension은 모두 평가 결과를 가진 expression이 될 수 있다.
- **왜 중요한가:** `total = price * quantity`를 읽을 때 오른쪽의 이름을 찾고 곱셈 operation을 수행해 결과 객체를 얻은 뒤 `total`에 바인딩된다는 순서를 추적할 수 있다.
- **예시:** Literal, 이름 조회, 함수 호출, 연산자, comprehension은 모두 …

#### 2. 객체

- **뜻:** 이 모델은 복잡한 한 줄도 작은 평가 단계로 분해하게 한다.
- **왜 중요한가:** Expression 안에 여러 함수 호출이 있다면 어떤 호출이 먼저 평가되는지 언어의 규칙이 중요하다.
- **예시:** 이 모델은 복잡한 한 줄도 작은 평가 단계로 …

#### 3. 함수

- **뜻:** 단순 산술 우선순위와 함수 argument 평가 순서를 혼동하지 않는다.
- **왜 중요한가:** Side effect가 있는 호출을 같은 expression에 여러 개 넣으면 평가 순서가 프로그램 상태를 바꿀 수 있으므로 값 계산과 상태 변경을 분리하는 편이 읽기 쉽다.
- **예시:** 단순 산술 우선순위와 함수 argument 평가 순서를 혼동하지 …

#### 4. 상태

- **뜻:** Pure expression은 입력과 출력만 검증할 수 있지만 effectful call은 외부 상태 변화와 실패 후 상태까지 관찰해야 한다.
- **왜 중요한가:** Expression의 결과를 사용하지 않는 코드도 있을 수 있다.
- **예시:** Pure expression은 입력과 출력만 검증할 수 있지만 effectful …

`items.append(x)`는 호출 자체가 list를 변경하는 effect가 중심이고 반환값은 일반 계산 결과로 쓰지 않는다. 반대로 `sorted(items)`는 새 값을 반환한다. API를 읽을 때 반환값과 effect 중 어느 것이 목적이인지 구분한다.

이 구분은 test에도 영향을 준다. 

---

## CHAPTER 02 · operator도 객체 protocol을 통해 behavior가 결정된다

### 시작 전 용어집

#### 1. operator

- **뜻:** Operator overloading은 domain 개념과 자연스럽게 맞을 때 유용하다.
- **왜 중요한가:** Money 덧셈처럼 의미가 분명할 수 있지만 database query를 `+` 하나에 숨기는 식으로 비용과 effect가 예상 밖이면 explicit method가 더 낫다.
- **예시:** 같은 타입에서도 `+`가 새 객체를 만드는지, `+=`가 in-place …

#### 2. 객체

- **뜻:** 같은 타입에서도 `+`가 새 객체를 만드는지, `+=`가 in-place mutation을 시도하는지 semantics가 다를 수 있다.
- **왜 중요한가:** Immutable integer에서는 새 값에 재바인딩되는 반면 mutable sequence에서는 객체 자체를 변경할 수 있다.
- **예시:** 같은 타입에서도 `+`가 새 객체를 만드는지, `+=`가 in-place …

#### 3. protocol

- **뜻:** 실제 의미는 operand 타입과 해당 protocol이 결정한다.
- **왜 중요한가:** 따라서 연산자 모양만 보고 성능이나 side effect를 추정하지 않는다.
- **예시:** 실제 의미는 operand 타입과 해당 protocol이 결정한다.

#### 4. behavior

- **뜻:** 문자열과 sequence의 결합, 사용자 정의 타입의 operation으로 연결될 수 있다.
- **왜 중요한가:** Aliasing이 있는 코드에서 augmented assignment 결과가 다른 reference에 보이는지 확인한다.
- **예시:** 문자열과 sequence의 결합, 사용자 정의 타입의 operation으로 연결될 …

`+`는 숫자 덧셈만 뜻하지 않는다.   

  

비교 연산도 boolean을 얻는 단순 기호 이상이다. 사용자 타입이 equality와 ordering을 어떤 domain 기준으로 정의하는지 중요하다. NaN 같은 특수 부동소수점 값은 일반 숫자 직관과 다른 비교 결과를 보일 수 있다.

 

---

## CHAPTER 03 · conditional expression은 값 선택에 쓰고 큰 제어 흐름을 숨기지 않는다

### 시작 전 용어집

#### 1. conditional expression

- **뜻:** Conditional expression에서도 선택되지 않은 branch expression은 평가되지 않는다.
- **왜 중요한가:** 따라서 양쪽에 함수 호출이나 expensive computation이 있으면 실제 실행 여부가 조건에 따라 달라진다.
- **예시:** Conditional expression에서도 선택되지 않은 branch expression은 평가되지 않는다.

#### 2. value

- **뜻:** 짧은 default나 display value를 만들 때 유용하지만 중첩되면 branch structure가 빠르게 읽기 어려워진다.
- **왜 중요한가:** 중요한 업무 정책은 명시적 `if/elif`나 named function으로 분리한다.
- **예시:** 짧은 default나 display value를 만들 때 유용하지만 중첩되면 …

#### 3. 함수

- **뜻:** `a if condition else b`는 두 후보 값 중 하나를 선택하는 expression이다.
- **왜 중요한가:** 값 선택이라는 표면 뒤에 effectful operation을 넣지 않는 편이 안전하다.
- **예시:** `a if condition else b`는 두 후보 값 …

#### 4. 조건

- **뜻:** `x or default`도 비슷한 default 패턴처럼 보이지만 모든 falsy value를 default로 바꾼다.
- **왜 중요한가:** 0이나 빈 문자열이 유효한 값이면 의미가 달라진다.
- **예시:** `x or default`도 비슷한 default 패턴처럼 보이지만 모든 …

`None`만 absence라면 explicit `x if x is not None else default`가 더 정확하다.

짧은 표현은 코드 줄 수를 줄이지만 semantic branch 수를 줄이지 않는다. 읽는 사람이 어떤 상태에서 어떤 값이 선택되는지 즉시 설명할 수 있는 범위에서 사용한다.

---

## CHAPTER 04 · unpacking은 구조를 분해하지만 shape contract를 요구한다

### 시작 전 용어집

#### 1. unpacking

- **뜻:** `a, b = pair` 같은 unpacking은 iterable의 요소를 여러 이름에 바인딩한다.
- **왜 중요한가:** 함수의 여러 결과, `(key, value)` pair, coordinate를 분해할 때 구조가 명확하다.
- **예시:** 하지만 element 수가 예상과 다르면 실패하므로 source의 shape가 …

#### 2. shape contract

- **뜻:** 하지만 element 수가 예상과 다르면 실패하므로 source의 shape가 contract다.
- **왜 중요한가:** Starred unpacking은 앞뒤 고정 요소와 중간 가변 요소를 분리할 수 있다.
- **예시:** 하지만 element 수가 예상과 다르면 실패하므로 source의 shape가 …

#### 3. 함수

- **뜻:** 함수 parameter의 `*args`, `**kwargs`도 구조를 모으는 도구지만 모든 API를 가변 argument로 만들면 signature에서 계약이 사라진다.
- **왜 중요한가:** 실제로 확장 가능 key 집합이 필요한 wrapper나 forwarding layer에서 사용하고, 일반 domain function은 명시적 parameter를 선호한다.
- **예시:** 함수 parameter의 `*args`, `**kwargs`도 구조를 모으는 도구지만 모든 …

#### 4. value

- **뜻:** 편리하지만 거대한 iterator를 `head, *rest`로 풀면 나머지를 list로 materialize해 memory를 사용할 수 있다.
- **왜 중요한가:** 문법적 destructuring이 evaluation/memory behavior에 영향을 준다.
- **예시:** 편리하지만 거대한 iterator를 `head, *rest`로 풀면 나머지를 list로 …

Dict merge와 mapping unpacking에서도 같은 key가 중복될 때 어느 값이 남는지 순서 규칙을 확인한다. Configuration layer 합성처럼 override precedence가 핵심인 경우 이 순서를 명시적인 정책으로 문서화한다.

---

## CHAPTER 05 · higher-order function은 behavior를 parameter로 만들어 분기를 줄인다

### 시작 전 용어집

#### 1. higher-order function

- **뜻:** 정렬 기준, validation rule, transformation처럼 algorithm은 같고 일부 behavior만 바뀐다면 함수를 argument로 전달할 수 있다.
- **왜 중요한가:** )`는 정렬 algorithm과 key extraction policy를 분리하는 대표 사례다.
- **예시:** 정렬 기준, validation rule, transformation처럼 algorithm은 같고 일부 …

#### 2. behavior

- **뜻:** 직접 만든 processing pipeline에서도 `transform(items, fn)`처럼 behavior를 주입할 수 있지만 단순한 한 번의 호출을 불필요한 abstraction으로 감싸지 않는다.
- **왜 중요한가:** 동일 구조가 반복되고 policy만 달라질 때 higher-order boundary가 의미 있다.
- **예시:** 직접 만든 processing pipeline에서도 `transform(items, fn)`처럼 behavior를 주입할 …

#### 3. parameter

- **뜻:** Callback contract에는 parameter와 return 외에도 exception, side effect, 호출 횟수, 호출 순서가 포함될 수 있다.
- **왜 중요한가:** 같은 callable type을 만족해도 한 callback이 database write를 수행하면 pure key function과 같은 방식으로 다룰 수 없다.
- **예시:** Callback contract에는 parameter와 return 외에도 exception, side effect, …

#### 4. validation

- **뜻:** Behavior를 값처럼 조합하면 거대한 type switch를 줄일 수 있지만 runtime path가 눈에 덜 보일 수도 있다.
- **왜 중요한가:** Named function과 type hint, 작은 protocol을 사용해 어떤 behavior가 연결됐는지 추적 가능하게 유지한다.
- **예시:** Behavior를 값처럼 조합하면 거대한 type switch를 줄일 수 …

`sorted(rows, key=...

 

 

 

---

## CHAPTER 06 · map/filter/reduce는 데이터 변환의 형태를 드러내지만 무조건적인 정답은 아니다

### 시작 전 용어집

#### 1. map

- **뜻:** Sequence 각 요소를 독립적으로 바꾸는 mapping, 조건으로 일부를 선택하는 filtering, 여러 값을 하나의 누적값으로 합치는 reduction은 반복 처리의 기본 형태다.
- **왜 중요한가:** Python에서는 comprehension, generator expression, built-in function, 명시적 loop 등 여러 표현을 선택할 수 있다.
- **예시:** Sequence 각 요소를 독립적으로 바꾸는 mapping, 조건으로 일부를 …

#### 2. filter

- **뜻:** `[normalize(x) for x in values if valid(x)]`는 filter와 map을 한 문장으로 표현할 수 있다.
- **왜 중요한가:** 단계가 많아지거나 오류 처리가 복잡하면 별도 named pipeline stage로 나누는 편이 낫다.
- **예시:** `[normalize(x) for x in values if valid(x)]`는 filter와 …

#### 3. reduce

- **뜻:** 함수형 표현을 쓰는 목적은 짧은 code가 아니라 transformation 구조를 선명하게 만드는 것이다.
- **왜 중요한가:** Reduction은 초기값과 결합 operation의 성질이 중요하다.
- **예시:** 함수형 표현을 쓰는 목적은 짧은 code가 아니라 transformation …

#### 4. 조건

- **뜻:** 합계처럼 associative한 연산은 parallel aggregation으로 확장하기 쉽지만 order-sensitive operation은 그렇지 않다.
- **왜 중요한가:** Empty input에서 identity value가 무엇인지도 계약이다.
- **예시:** 합계처럼 associative한 연산은 parallel aggregation으로 확장하기 쉽지만 order-sensitive …

Side effect를 `map` 안에 넣어 결과 iterator를 소비해야만 작업이 실행되게 만드는 코드는 lazy semantics를 오해하기 쉽다. Effect 반복이 목적이면 명시적 loop가 실행 의도를 더 잘 보여 준다.

---

## CHAPTER 07 · immutability는 상태 변경 지점을 줄여 reasoning 범위를 좁힌다

### 시작 전 용어집

#### 1. immutability

- **뜻:** 값을 만든 뒤 변경하지 않는 구조에서는 함수가 받은 object가 다른 코드 때문에 갑자기 바뀔 가능성이 줄어든다.
- **왜 중요한가:** 여러 version의 state가 필요하면 기존 object를 수정하는 대신 새로운 value를 만들어 반환할 수 있다.
- **예시:** 값을 만든 뒤 변경하지 않는 구조에서는 함수가 받은 …

#### 2. 상태

- **뜻:** 이는 concurrency와 undo/history에서도 장점이 있다.
- **왜 중요한가:** 하지만 큰 nested structure를 매번 완전히 복사하면 비용이 커질 수 있다.
- **예시:** 이는 concurrency와 undo/history에서도 장점이 있다.

#### 3. reasoning

- **뜻:** Persistent data structure처럼 구조 일부를 공유하는 기법도 있지만 Python 기본 collection에서 자동으로 제공되는 것은 아니다.
- **왜 중요한가:** Cache, connection pool, UI model은 시간에 따라 바뀐다.
- **예시:** Persistent data structure처럼 구조 일부를 공유하는 기법도 있지만 …

#### 4. 함수

- **뜻:** 불변 값을 선호하면 함수 contract도 단순해진다.
- **왜 중요한가:** 입력을 바꾸지 않는다는 가정이 생기고 test가 return value에 집중할 수 있다.
- **예시:** 불변 값을 선호하면 함수 contract도 단순해진다.

규모와 update pattern을 측정한다.

Mutable state가 본질적인 곳도 있다.  목표는 mutation을 없애는 것이 아니라 ownership을 좁히고 외부에 immutable snapshot이나 read-only interface를 제공하는 것이다.

  Mutation이 필요한 함수는 이름과 문서에서 effect를 드러낸다.

---

## CHAPTER 08 · partial application과 closure는 configuration을 behavior에 고정한다

### 시작 전 용어집

#### 1. partial application

- **뜻:** Closure나 partial application은 `tax_rate=0.
- **왜 중요한가:** 1` 같은 configuration을 capture해 `calculate(amount)` 형태의 더 좁은 interface를 만든다.
- **예시:** Closure나 partial application은 `tax_rate=0.

#### 2. closure

- **뜻:** Closure와 callable object 중 선택할 때 state introspection, serialization, 여러 operation 필요성을 본다.
- **왜 중요한가:** 단일 behavior와 작은 immutable configuration은 closure가 간결하고, lifecycle과 여러 method가 필요하면 객체가 선명할 수 있다.
- **예시:** Closure와 callable object 중 선택할 때 state introspection, …

#### 3. configuration

- **뜻:** Snapshot configuration인지 live shared state인지 명확히 한다.
- **왜 중요한가:** Configuration factory가 실패할 수 있다면 startup에 validation해 invalid callable이 만들어지지 않게 한다.
- **예시:** Snapshot configuration인지 live shared state인지 명확히 한다.

#### 4. behavior

- **뜻:** 하지만 capture된 object가 mutable하면 나중의 변경이 모든 호출 behavior에 영향을 줄 수 있다.
- **왜 중요한가:** 실행 중 매 호출마다 같은 설정 오류를 발견하는 것보다 초기 구성 단계에서 막는 편이 좋다.
- **예시:** 하지만 capture된 object가 mutable하면 나중의 변경이 모든 호출 …

같은 함수에 반복해서 동일한 argument를 전달한다면 일부 parameter를 미리 고정한 callable을 만들 수 있다. 

이 방식은 dependency를 global variable에서 제거하면서 호출자에게 필요한 parameter 수를 줄인다.  

 

 

---

## CHAPTER 09 · pipeline 합성에서는 실패 정보가 데이터 흐름을 끊는 방식을 결정한다

### 시작 전 용어집

#### 1. pipeline

- **뜻:** 여러 transformation을 연결하면 한 단계의 실패가 exception으로 전체 pipeline을 중단할지, error value로 다음 단계에 전달될지 정책이 필요하다.
- **왜 중요한가:** Parsing batch에서 한 row 오류를 건너뛸 수 있는지, financial calculation에서 하나라도 실패하면 전체를 거부해야 하는지 domain이 결정한다.
- **예시:** Retry를 transformation 내부에 숨기면 전체 처리 시간이 예상하기 …

#### 2. 실패

- **뜻:** Result object를 사용하면 성공/실패를 값으로 다룰 수 있지만 모든 단계가 result unwrapping을 알아야 해 코드가 장황해질 수 있다.
- **왜 중요한가:** Pipeline이 lazy iterator라면 실패 시점이 최종 consumer가 값을 요청할 때까지 늦춰진다.
- **예시:** Retry를 transformation 내부에 숨기면 전체 처리 시간이 예상하기 …

#### 3. exception

- **뜻:** Exception 기반 pipeline은 happy path가 간결하지만 per-item recovery가 필요하면 각 item boundary에서 catch해야 한다.
- **왜 중요한가:** Error logging context가 source item과 연결되도록 stage metadata를 유지한다.
- **예시:** Retry를 transformation 내부에 숨기면 전체 처리 시간이 예상하기 …

#### 4. value

- **뜻:** Retry를 transformation 내부에 숨기면 전체 처리 시간이 예상하기 어려워질 수 있다.
- **왜 중요한가:** 외부 I/O stage와 pure stage를 구분하고 retry/timeout은 effect boundary에서 명시한다.
- **예시:** Retry를 transformation 내부에 숨기면 전체 처리 시간이 예상하기 …

---

## CHAPTER 10 · 함수 합성의 품질은 중간 state 수와 계약 가시성으로 평가한다

### 시작 전 용어집

#### 1. 함수

- **뜻:** 좋은 functional-style code는 lambda를 많이 쓰는 코드가 아니다.
- **왜 중요한가:** 입력이 여러 작은 변환을 거쳐 결과로 흐르고, 각 단계의 input/output과 failure가 명확하며, mutation이 필요한 지점이 제한된 코드다.
- **예시:** 좋은 functional-style code는 lambda를 많이 쓰는 코드가 아니다.

#### 2. state

- **뜻:** 한 줄짜리 거대한 expression은 오히려 debugging과 naming을 어렵게 할 수 있다.
- **왜 중요한가:** `validated`, `normalized`, `priced` 같은 이름은 data가 어떤 contract를 통과했는지 나타낸다.
- **예시:** 한 줄짜리 거대한 expression은 오히려 debugging과 naming을 어렵게 …

#### 3. 계약

- **뜻:** Temporary variable을 모두 제거한다고 abstraction이 좋아지는 것은 아니다.
- **왜 중요한가:** Intermediate list를 여러 개 만드는 eager pipeline과 generator를 연결한 lazy pipeline의 memory/latency가 다르다.
- **예시:** Temporary variable을 모두 제거한다고 abstraction이 좋아지는 것은 아니다.

#### 4. 입력

- **뜻:** 같은 semantic stages를 유지하면서 evaluation strategy를 바꿀 수 있게 하면 최적화가 쉽다.
- **왜 중요한가:** Expression과 합성을 배우는 목적은 문법 트릭이 아니라 **값이 어디에서 만들어지고 어떤 상태 변경 없이 다음 계산으로 전달되는지 설명할 수 있는 흐름을 만드는 것**이다.
- **예시:** 같은 semantic stages를 유지하면서 evaluation strategy를 바꿀 수 …

중간값에 의미가 있다면 이름을 붙인다.  

Performance도 고려한다.
