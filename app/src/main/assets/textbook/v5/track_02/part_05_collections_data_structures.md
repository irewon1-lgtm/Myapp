# PART 05 · Collection과 자료구조 — 저장 모양이 연산 비용과 불변조건을 결정한다

여러 값을 다루기 시작하면 “무엇을 저장할까”와 함께 “어떤 연산을 자주 할까”를 선택해야 한다. Python의 list, tuple, dict, set은 단순히 괄호 모양이 다른 container가 아니다. 순서, 중복, key 기반 조회, 변경 가능성, hashability, 메모리 배치와 같은 서로 다른 계약을 제공한다. 자료구조 선택은 코드 스타일이 아니라 프로그램의 정확성과 비용을 동시에 결정한다.

---

## CHAPTER 01 · 자료구조는 값의 모양보다 필요한 연산으로 선택한다

### 시작 전 용어집

#### 1. 입력

- **뜻:** 입력 순서를 유지해야 하는지, ID로 자주 찾아야 하는지, 중복을 허용하는지, 앞뒤에서 삽입·삭제가 많은지, 정렬 결과가 필요한지를 알아야 한다.
- **왜 중요한가:** 같은 데이터라도 우세한 연산이 다르면 적합한 구조가 달라진다.
- **예시:** ID→객체 관계를 두 개의 병렬 list로 관리하는 것보다 …

#### 2. list

- **뜻:** `list`는 순서가 있는 sequence로 index 접근과 iteration에 적합하다.
- **왜 중요한가:** `dict`는 key에서 value로의 mapping을 표현한다.
- **예시:** ID→객체 관계를 두 개의 병렬 list로 관리하는 것보다 …

#### 3. dict

- **뜻:** 항목 10개를 한 번 찾는 코드에서 dict로 바꾸는 것이 의미가 없을 수 있고, 수백만 건을 요청마다 선형 탐색하는 구조에서는 index가 핵심일 수 있다.
- **왜 중요한가:** 자료구조 선택은 실제 workload와 함께 측정해야 한다.
- **예시:** ID→객체 관계를 두 개의 병렬 list로 관리하는 것보다 …

#### 4. value

- **뜻:** 상품 목록을 저장한다는 요구만으로는 어떤 자료구조가 적합한지 결정할 수 없다.
- **왜 중요한가:** `set`은 고유한 element의 membership과 집합 연산을 표현한다.
- **예시:** ID→객체 관계를 두 개의 병렬 list로 관리하는 것보다 …

`tuple`은 순서 있는 값 묶음을 만들면서 일반적으로 list보다 변경 제한이 강하다. 이름만 보고 “빠른 구조”를 고르는 것이 아니라 이 semantic contract가 문제와 맞는지 확인한다.

성능도 한 연산의 Big-O만 보고 결정하지 않는다. 데이터 크기, access pattern, memory locality, hash 계산 비용, 실제 상수 비용이 영향을 준다.  

또한 자료구조는 잘못된 상태를 막는 장치가 될 수 있다. 중복이 금지된 ID 목록을 list로 저장하면 매번 중복 검사를 해야 하지만 set은 uniqueness를 구조 자체가 표현한다. ID→객체 관계를 두 개의 병렬 list로 관리하는 것보다 dict가 관계를 직접 표현한다. 좋은 구조는 필요한 연산뿐 아니라 도메인 invariant를 코드에 드러낸다.

---

## CHAPTER 02 · list는 동적 배열 성격을 가지며 index와 삽입 비용이 다르다

### 시작 전 용어집

#### 1. list

- **뜻:** Python list는 순서 있는 mutable sequence다.
- **왜 중요한가:** 일반적으로 끝에 append하고 index로 원소를 읽는 작업은 효율적으로 지원하지만, 맨 앞이나 중간에 원소를 삽입·삭제하면 뒤의 많은 원소 위치를 옮겨야 할 수 있다.
- **예시:** `items[:100]` 정도는 문제가 없지만 loop 안에서 점점 긴 …

#### 2. index

- **뜻:** `items[i]`는 index를 통해 특정 위치를 찾는 연산이고 `value in items`는 일반적으로 값을 앞에서부터 비교하며 찾는 membership 연산이다.
- **왜 중요한가:** ID 존재 여부를 매우 자주 확인하는 문제라면 list보다 set이나 dict가 맞을 가능성이 높다.
- **예시:** `items[:100]` 정도는 문제가 없지만 loop 안에서 점점 긴 …

#### 3. mutable

- **뜻:** 모든 list 연산이 같은 비용이라고 생각하면 workload가 커졌을 때 병목을 놓친다.
- **왜 중요한가:** list slicing도 새 list를 만드는 경우가 많기 때문에 큰 데이터에서 반복적으로 사용하면 복사 비용이 누적될 수 있다.
- **예시:** `items[:100]` 정도는 문제가 없지만 loop 안에서 점점 긴 …

#### 4. value

- **뜻:** `items[:100]` 정도는 문제가 없지만 loop 안에서 점점 긴 slice를 계속 만들면 전체 비용이 예상보다 커질 수 있다.
- **왜 중요한가:** index range만 전달하거나 iterator를 사용하는 방식이 더 적합한 상황도 있다.
- **예시:** `items[:100]` 정도는 문제가 없지만 loop 안에서 점점 긴 …

둘은 문법적으로 짧아도 비용 구조가 다르다. 

  

append의 내부 구현은 필요할 때마다 정확히 한 칸씩 저장공간을 늘리는 방식보다 여유 capacity를 확보해 여러 append를 amortize하는 전략을 사용할 수 있다. 그래서 한 번의 resize는 비싸도 연속 append의 평균 비용은 낮아질 수 있다. 이 개념은 동적 배열을 사용하는 여러 언어에서 반복해서 등장한다.

---

## CHAPTER 03 · tuple은 단순한 읽기 전용 list가 아니라 고정된 관계를 표현할 수 있다

### 시작 전 용어집

#### 1. tuple

- **뜻:** Tuple은 순서가 있는 sequence이고 생성 후 slot 구성을 변경할 수 없다.
- **왜 중요한가:** 이 성질 때문에 함수의 여러 반환값, 좌표, 복합 key처럼 “몇 개의 값이 하나의 고정된 묶음”이라는 의미를 표현하는 데 사용된다.
- **예시:** Tuple은 순서가 있는 sequence이고 생성 후 slot 구성을 …

#### 2. list

- **뜻:** `(user_id, tags_list)`에서 tuple의 두 번째 slot이 다른 list로 바뀌지는 않지만 `tags_list.
- **왜 중요한가:** 따라서 tuple이라는 형식만 보고 전체 객체 그래프가 변하지 않는다고 결론 내리지 않는다.
- **예시:** `(user_id, tags_list)`에서 tuple의 두 번째 slot이 다른 list로 …

#### 3. 함수

- **뜻:** 하지만 field 의미가 많아지면 `item[2]` 같은 위치 기반 접근이 읽기 어려워지므로 named structure나 dataclass가 더 적합할 수 있다.
- **왜 중요한가:** Tuple 자체가 immutable이어도 내부에 mutable object를 참조할 수 있다.
- **예시:** 하지만 field 의미가 많아지면 `item[2]` 같은 위치 기반 …

#### 4. dataclass

- **뜻:** Hash key로 tuple을 사용할 수 있는 것도 내부 요소가 모두 hashable일 때다.
- **왜 중요한가:** 복합 식별자 `(country_code, local_id)`처럼 구성 요소가 안정적으로 변하지 않는 경우 dict key로 유용하다.
- **예시:** Hash key로 tuple을 사용할 수 있는 것도 내부 …

append()`는 가능하다. 

  반대로 list를 포함한 tuple은 내부 hash 안정성을 보장할 수 없어 key로 사용할 수 없다.

list와 tuple 중 무엇을 쓰는지는 성능 차이만으로 결정하지 않는다. 호출자에게 “이 sequence를 변경해도 되는가”, “원소 수와 위치 의미가 고정됐는가”라는 계약을 표현하는 것이 우선이다. 자료구조의 제한을 활용하면 잘못된 mutation 경로를 줄일 수 있다.

---

## CHAPTER 04 · dict는 key와 value의 관계를 직접 모델링한다

### 시작 전 용어집

#### 1. dict

- **뜻:** `dict`는 key를 통해 value를 찾는 mapping이다.
- **왜 중요한가:** 사용자 ID에서 사용자 객체, 상품 코드에서 가격, 설정 이름에서 설정값처럼 lookup 관계가 중심인 문제에 적합하다.
- **예시:** `dict`는 key를 통해 value를 찾는 mapping이다.

#### 2. value

- **뜻:** 저장된 실제 value가 `None`일 수 있다면 `get()` 결과가 None이라는 사실만으로 “key가 없음”을 판단할 수 없다.
- **왜 중요한가:** absence semantics를 명확하게 설계해야 한다.
- **예시:** 저장된 실제 value가 `None`일 수 있다면 `get()` 결과가 …

#### 3. key

- **뜻:** Hash table 기반 lookup은 평균적으로 매우 효율적이지만 key의 hash와 equality 계약이 올바르다는 전제에 의존한다.
- **왜 중요한가:** collision이 전혀 없다는 뜻도 아니고 모든 상황에서 일정 시간이 보장된다는 뜻도 아니다.
- **예시:** Hash table 기반 lookup은 평균적으로 매우 효율적이지만 key의 …

#### 4. 객체

- **뜻:** key가 복잡한 객체라면 hash 계산 자체에 비용이 있을 수 있고, 매우 큰 dict는 메모리 overhead도 고려해야 한다.
- **왜 중요한가:** Key 존재 여부를 확인할 때 `if key in mapping:`과 `mapping.
- **예시:** key가 복잡한 객체라면 hash 계산 자체에 비용이 있을 …

두 개의 list를 같은 index로 맞춰 관리하는 구조보다 관계를 한 자료구조가 직접 표현하므로 동기화 오류를 줄인다.

  

get(key)`는 의미가 다를 수 있다.  

반복할 때 dict의 key, value, item 중 무엇이 필요한지 선택한다. key와 value를 별도로 찾기 위해 매번 lookup을 반복하기보다 `for key, value in mapping.items()`가 관계를 직접 드러낼 수 있다. dict comprehension으로 변환할 때 duplicate key가 생기면 어떤 값이 남는지도 데이터 손실 계약과 연결된다.

---

## CHAPTER 05 · set은 중복 제거 도구보다 membership과 집합 관계를 표현하는 구조다

### 시작 전 용어집

#### 1. set

- **뜻:** Set은 고유한 hashable element의 집합을 표현한다.
- **왜 중요한가:** membership 검사, union, intersection, difference 같은 연산이 문제의 언어와 맞을 때 강하다.
- **예시:** 예를 들어 사용자에게 필요한 권한 집합이 보유 권한 …

#### 2. membership

- **뜻:** 예를 들어 사용자에게 필요한 권한 집합이 보유 권한 집합의 부분집합인지 검사하거나, 두 데이터 집합에서 공통 ID를 찾는 문제는 set semantics로 직접 표현할 수 있다.
- **왜 중요한가:** 단순히 list의 중복을 제거하려고 set으로 바꾸면 순서 요구사항을 잊을 수 있다.
- **예시:** `missing = required - granted`는 “필요하지만 없는 권한”이라는 …

#### 3. hash

- **뜻:** Set element도 hash 안정성이 필요하므로 list 같은 mutable unhashable object는 일반 element로 사용할 수 없다.
- **왜 중요한가:** 객체를 set에 넣은 뒤 equality에 영향을 주는 상태를 바꾸는 사용자 정의 타입도 위험하다.
- **예시:** `missing = required - granted`는 “필요하지만 없는 권한”이라는 …

#### 4. union

- **뜻:** 현재 Python 구현과 언어 버전의 구체적 iteration behavior에 기대기보다 set의 본질이 순서 기반 sequence가 아니라는 점을 기준으로 설계한다.
- **왜 중요한가:** 출력 순서가 계약이라면 별도 정렬이나 순서 보존 구조가 필요하다.
- **예시:** `missing = required - granted`는 “필요하지만 없는 권한”이라는 …

식별 기준이 변할 수 있다면 변하지 않는 ID를 element로 사용하는 방식이 더 안전할 수 있다.

집합 연산은 nested loop로 같은 기능을 구현하는 것보다 의도를 명확하게 표현할 수 있다. `missing = required - granted`는 “필요하지만 없는 권한”이라는 의미가 그대로 드러난다. 적절한 자료구조는 코드 길이를 줄이는 것보다 문제의 수학적 관계를 실행 가능한 형태로 보존한다.

---

## CHAPTER 06 · hash table은 equality와 hash의 일관성에 의존한다

### 시작 전 용어집

#### 1. hash

- **뜻:** Dict와 set의 빠른 lookup을 이해하려면 hash table의 기본 계약을 알아야 한다.
- **왜 중요한가:** key에서 hash 값을 계산해 후보 위치를 좁히고, 필요하면 equality 비교로 실제 key인지 확인한다.
- **예시:** Dict와 set의 빠른 lookup을 이해하려면 hash table의 기본 …

#### 2. equality

- **뜻:** 가장 중요한 불변조건은 equality가 같은 객체는 호환되는 hash를 가져야 한다는 것이다.
- **왜 중요한가:** 사용자 정의 클래스에서 `__eq__`만 바꾸고 `__hash__`를 부주의하게 정의하면 dict/set의 기본 전제를 깨뜨릴 수 있다.
- **예시:** 가장 중요한 불변조건은 equality가 같은 객체는 호환되는 hash를 …

#### 3. dict

- **뜻:** dict key 배치용 hash를 데이터 무결성이나 password 저장에 사용하면 안 된다.
- **왜 중요한가:** 이름이 같아 보여도 요구되는 충돌 저항성, 공격 모델, 안정성이 전혀 다르다.
- **예시:** dict key 배치용 hash를 데이터 무결성이나 password 저장에 …

#### 4. set

- **뜻:** 서로 다른 key가 같은 hash를 가질 수 있으므로 collision 처리가 필요하며, hash 값만으로 동일 객체를 확정하지 않는다.
- **왜 중요한가:** 저장된 뒤 equality와 hash 결과가 변하는 mutable key도 lookup을 망가뜨릴 수 있다.
- **예시:** 서로 다른 key가 같은 hash를 가질 수 있으므로 …

보안 관점에서도 외부 입력이 hash collision을 악용해 많은 비교를 유발하는 문제를 생각할 수 있다. 일반 애플리케이션이 hash algorithm을 직접 구현할 필요는 없지만, “hash lookup은 무조건 O(1)”이라는 절대 명제로 이해하지 않는 것이 중요하다. 평균적 기대 비용과 최악 상황은 구분한다.

Hash는 암호학적 digest와 목적도 다르다.   자료구조용 hashing은 lookup contract의 일부다.

---

## CHAPTER 07 · nested collection에서는 aliasing 구조를 먼저 그린다

### 시작 전 용어집

#### 1. nested collection

- **뜻:** `matrix = [[0] * 3] * 3` 같은 코드는 겉으로 3×3 리스트를 만든 것처럼 보이지만 바깥 리스트의 세 항목이 같은 내부 리스트 객체를 참조할 수 있다.
- **왜 중요한가:** 한 행을 수정했는데 여러 행이 함께 바뀌는 이유는 multiplication이 내부 객체를 깊게 복제하지 않기 때문이다.
- **예시:** bad = [[0] * 3] * 3 / …

#### 2. aliasing

- **뜻:** 얕은 복사, default argument, class attribute의 mutable object도 같은 종류의 aliasing 문제를 만들 수 있다.
- **왜 중요한가:** 여러 객체가 동일 configuration이나 cache를 공유해야 하는 경우도 있다.
- **예시:** bad = [[0] * 3] * 3 / …

#### 3. 객체

- **뜻:** 중첩 collection을 다룰 때는 괄호 모양보다 객체 그래프를 본다.
- **왜 중요한가:** 각 slot이 독립 객체를 가리키는지, 공유 객체를 가리키는지에 따라 mutation 결과가 달라진다.
- **예시:** bad = [[0] * 3] * 3 / …

#### 4. class

- **뜻:** 문제는 공유가 의도인지 우연인지 구분되지 않을 때다.
- **왜 중요한가:** 공유 객체는 ownership과 update responsibility를 명시하고, 독립 snapshot이 필요한 경계에서는 명시적으로 새 구조를 만든다.
- **예시:** bad = [[0] * 3] * 3 / …

```python
bad = [[0] * 3] * 3
good = [[0] * 3 for _ in range(3)]
```

  

공유 자체가 잘못은 아니다.   

디버깅할 때 “값 출력이 같아 보인다”는 사실만으로 공유 여부를 알 수 없다. mutation 전후의 경로와 identity 관계를 확인한다. 복잡한 nested state를 자주 변경해야 한다면 mutable dict/list의 임의 조합보다 명시적 data model을 도입하는 것이 장기적으로 더 안전할 수 있다.

---

## CHAPTER 08 · iterator와 generator는 collection 전체를 만들지 않고 데이터를 흐르게 한다

### 시작 전 용어집

#### 1. iterator

- **뜻:** Iterable은 iterator를 제공할 수 있는 객체이고 iterator는 다음 값을 순차적으로 생산하는 상태를 가진다.
- **왜 중요한가:** Generator는 이런 iterator를 작성하기 쉽게 만드는 언어 기능이다.
- **예시:** def valid_lines(path): / with open(path, encoding="utf-8") as f:

#### 2. generator

- **뜻:** 파일 열기나 parsing 오류가 generator를 생성할 때가 아니라 실제 iteration 중에 발생할 수 있다.
- **왜 중요한가:** iterator는 한 번 소비하면 같은 상태로 다시 읽을 수 없는 경우가 많다.
- **예시:** def valid_lines(path): / with open(path, encoding="utf-8") as f:

#### 3. collection

- **뜻:** “두 번 순회할 수 있는 collection”과 “한 번 흐르는 stream”은 다른 계약이다.
- **왜 중요한가:** Generator가 resource를 보유하면 소비가 중간에 멈췄을 때 정리 시점도 고려해야 한다.
- **예시:** def valid_lines(path): / with open(path, encoding="utf-8") as f:

#### 4. 객체

- **뜻:** 이 모델을 사용하면 대용량 파일이나 무한 sequence를 전체 list로 만들지 않고 한 항목씩 처리할 수 있다.
- **왜 중요한가:** 이 함수는 모든 줄을 메모리에 저장한 뒤 반환하지 않는다.
- **예시:** def valid_lines(path): / with open(path, encoding="utf-8") as f:

```python
def valid_lines(path):
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                yield line
```

 소비자가 `next`를 요청할 때 실행이 진행되어 값을 하나씩 제공한다. 따라서 memory usage를 줄이고 producer와 consumer를 pipeline으로 연결할 수 있다.

Lazy evaluation에는 trade-off가 있다.   

 lazy 구조는 메모리를 줄이는 대신 lifetime을 길게 만들 수 있다. 그래서 eager list와 lazy iterator 선택은 데이터 규모뿐 아니라 오류 시점, 반복 가능성, resource ownership까지 함께 결정한다.

---

## CHAPTER 09 · 정렬은 비교 기준과 안정성이라는 계약을 가진다

### 시작 전 용어집

#### 1. 계약

- **뜻:** 정렬은 값을 “작은 순서”로 놓는 단일 동작이 아니다.
- **왜 중요한가:** 어떤 key를 기준으로 비교하는지, 동일 key끼리 기존 순서를 유지해야 하는지, 원본을 변경할지 새 결과를 만들지에 따라 의미가 달라진다.
- **예시:** 예를 들어 `(department, age)` tuple을 key로 사용하면 첫 …

#### 2. list

- **뜻:** Python의 `sorted()`는 iterable에서 새 list를 만들고, list의 `.
- **왜 중요한가:** sort()`는 해당 list를 제자리에서 변경한다.
- **예시:** 예를 들어 `(department, age)` tuple을 key로 사용하면 첫 …

#### 3. tuple

- **뜻:** 예를 들어 `(department, age)` tuple을 key로 사용하면 첫 필드가 같을 때 다음 필드로 비교하는 규칙을 만들 수 있다.
- **왜 중요한가:** 문자열 정렬에서는 대소문자, locale, Unicode normalization 등 도메인 정책이 필요할 수 있다.
- **예시:** 예를 들어 `(department, age)` tuple을 key로 사용하면 첫 …

복합 데이터를 정렬할 때 key function을 사용하면 비교 규칙을 명시적으로 표현할 수 있다.  

Stable sort의 의미를 이해하면 여러 단계 정렬을 조합할 수 있다. 동일한 key를 가진 항목들의 기존 상대 순서가 유지되면 앞선 정렬 결과를 보존하면서 다른 기준을 적용하는 설계가 가능하다. 하지만 이런 특성을 사용한다면 코드에서 의도가 드러나야 한다.

정렬 비용도 무시하지 않는다. 화면에 상위 10개만 필요하다고 해서 항상 전체 수백만 항목을 정렬할 필요는 없다. heap이나 database index처럼 문제 규모에 더 적합한 방법이 있을 수 있다. 정렬은 presentation 기능처럼 보여도 데이터 구조와 알고리즘 선택에 직접 연결된다.

---

## CHAPTER 10 · 자료구조 선택은 API와 데이터 모델의 일부다

### 시작 전 용어집

#### 1. API

- **뜻:** `dict`를 반환하면 key가 무엇인지, key가 고유 식별자인지, value가 mutable인지가 API 의미의 일부가 된다.
- **왜 중요한가:** 경계에서 지나치게 구체적인 구조를 노출하면 내부 구현 변경이 어려워질 수도 있다.
- **예시:** 예를 들어 단순 dict로 주문을 표현하면 필드 이름 …

#### 2. 함수

- **뜻:** 함수가 `list`를 반환한다고 선언하면 호출자는 순서와 중복, 반복 가능성을 기대할 수 있다.
- **왜 중요한가:** `set`을 반환하면 uniqueness를 전달하지만 순서에 대한 다른 기대를 가져야 한다.
- **예시:** 예를 들어 단순 dict로 주문을 표현하면 필드 이름 …

#### 3. list

- **뜻:** 이 세 질문을 함께 사용하면 `list가 익숙해서 list` 같은 선택에서 벗어나 문제에 맞는 저장 모델을 만들 수 있다.
- **왜 중요한가:** 호출자가 iteration만 필요하다면 `Iterable` 같은 추상 계약이 더 적합할 수 있고, random access가 필요하다면 sequence 성질을 요구해야 한다.
- **예시:** 예를 들어 단순 dict로 주문을 표현하면 필드 이름 …

#### 4. 반복

- **뜻:** 반대로 지나치게 추상적인 타입은 필요한 성능과 mutation semantics를 숨길 수 있다.
- **왜 중요한가:** 데이터 구조가 커지면 invariant를 한 곳에서 보호하는 객체로 감쌀 가치가 생긴다.
- **예시:** 예를 들어 단순 dict로 주문을 표현하면 필드 이름 …

자료구조 타입은 단순 구현 세부사항이 아니다.

  

 예를 들어 단순 dict로 주문을 표현하면 필드 이름 오타, 누락, 잘못된 상태 조합을 어디서든 만들 수 있다. dataclass나 domain object가 생성 규칙과 상태 전이를 소유하면 잘못된 상태를 줄일 수 있다.

자료구조를 고를 때 최종 질문은 세 가지다. 어떤 관계를 표현해야 하는가, 어떤 연산이 우세한가, 어떤 잘못된 상태를 구조적으로 막고 싶은가.
