# PART 61 · Binding과 assignment semantics — 이름·target·mutation을 같은 대입 기호로 오해하지 않기

Python에서 `=`는 흔히 “변수 상자에 값을 넣는다”로 설명되지만 실제 코드를 깊게 읽으려면 오른쪽 expression 평가, 왼쪽 assignment target 해석, 이름 binding, attribute/item mutation을 분리해야 한다. `x = value`, `obj.attr = value`, `items[i] = value`, `a += b`는 모두 비슷한 기호를 사용하지만 객체 graph에 미치는 영향이 다르다. 핵심은 **무엇이 새 이름에 바인딩되고, 무엇이 기존 객체를 변경하며, 어느 scope의 binding이 바뀌는지**를 추적하는 것이다.

---

## CHAPTER 01 · assignment는 오른쪽을 먼저 평가하고 target에 결과를 배치한다

### 시작 전 용어집

#### 1. assignment

- **뜻:** attr = value`는 attribute assignment protocol을 사용하고 `items[index] = value`는 container item assignment를 호출한다.
- **왜 중요한가:** 이 경우 기존 object 내부 상태가 바뀔 수 있다.
- **예시:** `x = f()`를 실행하면 먼저 `f()`가 평가되어 결과 …

#### 2. target

- **뜻:** 왼쪽 target이 항상 단순 이름인 것은 아니다.
- **왜 중요한가:** 동일한 `=`가 보이지만 **name rebinding과 object mutation은 다른 operation**이다.
- **예시:** `x = f()`를 실행하면 먼저 `f()`가 평가되어 결과 …

#### 3. 객체

- **뜻:** `x = f()`를 실행하면 먼저 `f()`가 평가되어 결과 객체를 얻고 그 다음 현재 namespace의 `x` 이름이 그 객체에 바인딩된다.
- **왜 중요한가:** `x`가 이전에 다른 객체를 가리켰다면 binding이 교체될 뿐 이전 객체 자체를 수정하는 것은 아니다.
- **예시:** Assignment chain `a = b = make()`에서는 오른쪽 …

#### 4. binding

- **뜻:** 이 기본 순서를 알면 함수 호출이 실패했을 때 왼쪽 이름이 변경되지 않는 이유도 설명할 수 있다.
- **왜 중요한가:** Assignment chain `a = b = make()`에서는 오른쪽 expression이 한 번 평가되고 같은 결과 object가 여러 target에 연결될 수 있다.
- **예시:** 이 기본 순서를 알면 함수 호출이 실패했을 때 …

`obj.  

 Mutable object를 이렇게 공유하면 한 이름을 통해 수정한 변화가 다른 이름에서도 보인다. “같은 값으로 초기화했다”와 “같은 객체를 공유한다”를 구분한다.

---

## CHAPTER 02 · unpacking assignment는 iterable shape를 target 구조와 맞춘다

### 시작 전 용어집

#### 1. unpacking assignment

- **뜻:** `a, b = source`는 source에서 값을 꺼내 두 target에 각각 binding한다.
- **왜 중요한가:** Source element 수가 너무 적거나 많으면 assignment가 실패하므로 target 구조 자체가 shape validation 역할을 한다.
- **예시:** `head, *middle, tail = values`는 편리하지만 source가 iterator라면 …

#### 2. iterable shape

- **뜻:** 함수가 정확히 두 결과를 반환해야 한다는 contract를 unpacking으로 표현할 수 있다.
- **왜 중요한가:** Starred target을 사용하면 앞뒤 고정 요소와 나머지를 분리할 수 있다.
- **예시:** `head, *middle, tail = values`는 편리하지만 source가 iterator라면 …

#### 3. target

- **뜻:** `head, *middle, tail = values`는 편리하지만 source가 iterator라면 나머지를 수집하기 위해 상당량을 소비하고 list를 만들 수 있다.
- **왜 중요한가:** 구조 분해 문법이 lazy stream을 자동 유지하지는 않는다.
- **예시:** `head, *middle, tail = values`는 편리하지만 source가 iterator라면 …

#### 4. binding

- **뜻:** Nested unpacking은 tuple/tree 모양을 코드에 직접 표현할 수 있지만 input schema가 바뀔 때 assignment line 전체가 깨진다.
- **왜 중요한가:** Long-lived public data structure에서는 named field나 typed model이 위치 기반 unpacking보다 compatibility에 강할 수 있다.
- **예시:** Nested unpacking은 tuple/tree 모양을 코드에 직접 표현할 수 …

---

## CHAPTER 03 · augmented assignment는 read·operation·write와 in-place protocol 사이에 놓인다

### 시작 전 용어집

#### 1. augmented assignment

- **뜻:** Attribute/item target과 augmented assignment가 결합되면 getter와 setter protocol이 모두 실행될 수 있다.
- **왜 중요한가:** Property나 proxy object에서 `obj.
- **예시:** `x += y`를 무조건 `x = x + …

#### 2. read

- **뜻:** `x += y`를 무조건 `x = x + y`의 축약으로 보면 mutable object에서 차이를 놓친다.
- **왜 중요한가:** Python은 in-place operation protocol을 먼저 시도할 수 있고 object가 이를 지원하면 identity를 유지한 채 내부 state를 변경할 수 있다.
- **예시:** List를 두 이름이 공유할 때 `a += [1]`이 …

#### 3. operation

- **뜻:** value += 1`은 단순 numeric operation보다 더 많은 method call과 side effect를 포함할 수 있으므로 performance와 correctness를 타입 contract에서 확인한다.
- **왜 중요한가:** Immutable object는 새로운 result를 만들고 target이 다시 binding될 가능성이 높다.
- **예시:** List를 두 이름이 공유할 때 `a += [1]`이 …

#### 4. write

- **뜻:** List를 두 이름이 공유할 때 `a += [1]`이 기존 list를 수정하면 `b`에서도 변화가 보인다.
- **왜 중요한가:** 반면 tuple 같은 immutable sequence의 `+=`는 새 tuple을 만들고 `a`만 rebinding될 수 있다.
- **예시:** List를 두 이름이 공유할 때 `a += [1]`이 …

동일 연산자라도 type의 data model이 mutation semantics를 결정한다.

 

---

## CHAPTER 04 · assignment expression은 값을 만들면서 이름도 bind하므로 scope를 명확히 읽어야 한다

### 시작 전 용어집

#### 1. assignment expression

- **뜻:** 반대로 복잡한 boolean 식 안에 여러 assignment expression을 넣으면 short-circuit 때문에 어떤 이름이 실제로 bind되었는지 branch별로 달라질 수 있다.
- **왜 중요한가:** 값 생성과 제어 흐름이 동시에 일어나는 지점을 최소화한다.
- **예시:** `:=`는 expression 안에서 계산 결과를 이름에 binding해 같은 …

#### 2. bind

- **뜻:** `:=`는 expression 안에서 계산 결과를 이름에 binding해 같은 값을 다시 사용할 수 있게 한다.
- **왜 중요한가:** Regex match나 stream read처럼 expensive expression을 조건 검사와 이후 처리에서 두 번 호출하지 않게 만들 수 있다.
- **예시:** `while chunk := read_chunk():` 같은 구조는 “읽기 → …

#### 3. scope

- **뜻:** Comprehension 안에서 assignment expression을 사용할 때 binding scope는 일반 assignment와 직관적으로 다를 수 있으므로 language reference를 기준으로 확인한다.
- **왜 중요한가:** 편의 문법을 사용하기 전에 이름이 어느 scope에 남는지 정확히 알아야 한다.
- **예시:** `while chunk := read_chunk():` 같은 구조는 “읽기 → …

#### 4. regex

- **뜻:** 하지만 한 expression 안에서 stateful binding이 추가되므로 과도하게 중첩하면 읽기가 어려워진다.
- **왜 중요한가:** `while chunk := read_chunk():` 같은 구조는 “읽기 → 종료 검사 → body에서 같은 chunk 사용” 흐름을 직접 표현한다.
- **예시:** 하지만 한 expression 안에서 stateful binding이 추가되므로 과도하게 …

---

## CHAPTER 05 · `del`은 object를 지우는 명령보다 target relation을 제거하는 operation이다

### 시작 전 용어집

#### 1. del

- **뜻:** `del name`은 namespace에서 이름 binding을 제거하고, `del obj.
- **왜 중요한가:** attr`은 attribute deletion protocol을 호출하며, `del items[i]`는 container에서 item을 제거한다.
- **예시:** `del name`은 namespace에서 이름 binding을 제거하고, `del obj.

#### 2. object

- **뜻:** 같은 keyword라도 object destruction 자체를 직접 명령하는 것과는 다르다.
- **왜 중요한가:** 다른 strong reference가 남아 있다면 object는 계속 살아 있을 수 있다.
- **예시:** 같은 keyword라도 object destruction 자체를 직접 명령하는 것과는 …

#### 3. target relation

- **뜻:** `del` 이후 이름을 다시 읽으면 binding이 없기 때문에 오류가 발생할 수 있지만 object가 다른 alias를 통해 접근 가능한지 여부는 별개다.
- **왜 중요한가:** Memory 문제를 해결하려고 무작정 `del`을 추가해도 cache/global/container가 같은 object를 붙잡고 있으면 retained memory는 줄지 않는다.
- **예시:** `del` 이후 이름을 다시 읽으면 binding이 없기 때문에 …

#### 4. operation

- **뜻:** Custom object는 attribute deletion을 가로채 invariant를 보호할 수 있다.
- **왜 중요한가:** Required field를 삭제하지 못하게 하거나 deletion을 domain state transition으로 해석할 수 있다.
- **예시:** Custom object는 attribute deletion을 가로채 invariant를 보호할 수 …

따라서 `del obj.attr`도 단순 namespace 조작으로만 보지 않는다.

---

## CHAPTER 06 · `global`과 `nonlocal`은 이름 조회가 아니라 rebinding target scope를 바꾼다

### 시작 전 용어집

#### 1. global

- **뜻:** 바깥 module binding을 다시 bind하려면 `global`, enclosing function scope의 binding을 다시 bind하려면 `nonlocal` 선언이 필요할 수 있다.
- **왜 중요한가:** 이 선언은 object mutation 허가가 아니라 **어느 namespace의 이름을 assignment target으로 삼을 것인지**를 결정한다.
- **예시:** 그러나 `items = new_list`처럼 outer 이름 자체를 교체하려면 …

#### 2. nonlocal

- **뜻:** Outer scope의 list에 `append()`하는 것은 이름을 다시 bind하지 않으므로 `nonlocal`이 필요하지 않을 수 있다.
- **왜 중요한가:** 그러나 `items = new_list`처럼 outer 이름 자체를 교체하려면 nonlocal이 필요하다.
- **예시:** Outer scope의 list에 `append()`하는 것은 이름을 다시 bind하지 …

#### 3. binding

- **뜻:** 함수 안에서 단순 assignment를 하면 보통 local binding이 생긴다.
- **왜 중요한가:** Mutation과 rebinding의 차이가 다시 나타난다.
- **예시:** 함수 안에서 단순 assignment를 하면 보통 local binding이 …

#### 4. scope

- **뜻:** Global rebinding은 숨은 shared state를 만들기 쉬워 사용 범위를 좁힌다.
- **왜 중요한가:** Module-level immutable configuration을 읽는 것과 runtime counter를 함수마다 수정하는 것은 복잡도가 다르다.
- **예시:** Global rebinding은 숨은 shared state를 만들기 쉬워 사용 …

필요한 상태를 객체나 explicit parameter로 소유하게 만들 수 있는지 먼저 본다.

---

## CHAPTER 07 · comprehension은 자체 실행 scope를 가지므로 loop 변수 누출을 일반 loop와 같게 보지 않는다

### 시작 전 용어집

#### 1. comprehension

- **뜻:** Python의 comprehension은 구현과 언어 버전에 따라 별도 scope semantics를 가지며 loop target이 surrounding local scope에 남지 않는 형태를 제공한다.
- **왜 중요한가:** `for` statement와 comprehension을 같은 이름 binding 규칙으로 단순화하면 closure와 assignment expression에서 혼동이 생긴다.
- **예시:** Python의 comprehension은 구현과 언어 버전에 따라 별도 scope …

#### 2. scope

- **뜻:** 한 줄 길이보다 scope와 evaluation order가 명확한지가 중요하다.
- **왜 중요한가:** Comprehension 내부의 lambda가 loop variable을 capture하면 각 lambda가 당시 값을 복사한다고 생각하기 쉽지만 closure는 binding을 capture하기 때문에 나중에 동일 최종값을 볼 수 있다.
- **예시:** 한 줄 길이보다 scope와 evaluation order가 명확한지가 중요하다.

#### 3. loop

- **뜻:** Nested comprehension에서는 각 `for`와 `if`가 어떤 순서로 평가되고 어느 이름이 어느 단계에서 유효한지 일반 nested loop로 풀어 쓸 수 있어야 한다.
- **왜 중요한가:** 호출별 snapshot이 필요하면 default argument나 별도 factory로 값을 고정한다.
- **예시:** Nested comprehension에서는 각 `for`와 `if`가 어떤 순서로 평가되고 …

---

## CHAPTER 08 · binding contract를 읽으면 aliasing·closure·mutation 오류를 같은 모델로 설명할 수 있다

### 시작 전 용어집

#### 1. binding

- **뜻:** Assignment는 binding을 만들거나 target protocol을 호출하고, augmented assignment는 type에 따라 in-place mutation을 할 수 있으며, closure는 enclosing binding을 보존하고, `del`은 relation을 제거한다.
- **왜 중요한가:** 이 공통 모델을 사용하면 서로 다른 문법을 하나의 object graph 변화로 추적할 수 있다.
- **예시:** Assignment는 binding을 만들거나 target protocol을 호출하고, augmented assignment는 …

#### 2. aliasing

- **뜻:** 이 세 질문이면 mutable default, aliasing, closure late binding, unexpected global state를 동일한 언어로 조사할 수 있다.
- **왜 중요한가:** Binding semantics의 핵심은 **변수를 상자로 외우는 데서 벗어나 namespace의 이름과 object graph의 reference가 언제 연결·교체·삭제되는지 실행 순서로 보는 것**이다.
- **예시:** 이 세 질문이면 mutable default, aliasing, closure late …

#### 3. closure

- **뜻:** Python의 여러 “변수 문제”는 사실 이름과 object 관계를 혼동할 때 생긴다.
- **왜 중요한가:** 어떤 expression이 어떤 object를 만들거나 찾았는가, 어느 이름/attribute/item이 그 object와 연결되는가, 기존 object 자체가 수정되는가.
- **예시:** Python의 여러 “변수 문제”는 사실 이름과 object 관계를 …

디버깅할 때 각 줄에서 세 가지를 적는다.
