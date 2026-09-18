# PART 26 · Memory lifetime·GC·weak reference — 객체가 언제까지 살아 있는지 추적하기

Python은 일반 application code에서 메모리를 직접 `free`하지 않게 해 주지만 객체 lifetime이 사라지는 것은 아니다. 이름, container, closure, cache, callback registry가 reference를 유지하면 더 이상 화면에 보이지 않는 객체도 계속 살아 있을 수 있다. 메모리 문제를 이해하려면 **어떤 코드가 객체를 만들었는가보다 누가 아직 그 객체를 가리키고 있는가**를 추적해야 한다.

---

## CHAPTER 01 · 이름의 lifetime과 객체의 lifetime은 다르다

### 시작 전 용어집

#### 1. lifetime

- **뜻:** Lifetime을 분석할 때 object graph를 그린다.
- **왜 중요한가:** Root 역할을 하는 module/global/thread/task가 어떤 container를 참조하고 그 container가 어떤 object를 붙잡는지 본다.
- **예시:** 두 개념을 분리하면 closure, iterator, cache의 예상치 못한 …

#### 2. 객체

- **뜻:** 함수 local 이름은 함수가 끝나면 scope에서 사라지지만 그 이름이 가리키던 객체가 다른 곳에서도 reference되고 있다면 객체는 계속 살아 있을 수 있다.
- **왜 중요한가:** 반환된 object, global list에 추가된 callback, closure가 capture한 state가 대표 사례다.
- **예시:** 두 개념을 분리하면 closure, iterator, cache의 예상치 못한 …

#### 3. 함수

- **뜻:** 반대로 같은 이름에 새 객체를 대입하면 이전 객체가 즉시 “삭제”된다고 단정할 수 없다.
- **왜 중요한가:** 다른 alias가 남아 있을 수 있고 runtime의 garbage collection 시점과 resource finalization도 별개다.
- **예시:** 두 개념을 분리하면 closure, iterator, cache의 예상치 못한 …

#### 4. scope

- **뜻:** Scope는 이름 검색 규칙이고 lifetime은 reference graph 규칙이다.
- **왜 중요한가:** 두 개념을 분리하면 closure, iterator, cache의 예상치 못한 memory retention을 설명할 수 있다.
- **예시:** Scope는 이름 검색 규칙이고 lifetime은 reference graph 규칙이다.

`del name`은 binding을 제거하는 operation이지 특정 object를 강제로 파괴하는 명령으로 이해하지 않는다.

  Memory leak처럼 보이는 현상은 대개 필요 없어야 할 object로 이어지는 reference path가 남아 있는 문제다.

 

---

## CHAPTER 02 · reference counting은 lifetime의 한 메커니즘이지만 언어 전체 계약으로 과신하지 않는다

### 시작 전 용어집

#### 1. reference counting

- **뜻:** CPython 구현은 reference counting을 중요한 memory management 기법으로 사용한다.
- **왜 중요한가:** Reference 수가 0이 되는 객체는 빠르게 정리될 수 있지만 다른 Python implementation은 내부 전략이 다를 수 있고 cycle은 reference count만으로 해결되지 않는다.
- **예시:** CPython 구현은 reference counting을 중요한 memory management 기법으로 …

#### 2. lifetime

- **뜻:** File과 lock은 context manager로 명시적으로 닫고 외부 resource lifetime을 garbage collection에 맡기지 않는다.
- **왜 중요한가:** Reference count를 debugging 목적으로 관찰할 수 있어도 observer 자체가 reference를 추가하거나 runtime 내부 temporary reference가 있을 수 있어 절대 숫자에 과도한 의미를 두지 않는다.
- **예시:** File과 lock은 context manager로 명시적으로 닫고 외부 resource …

#### 3. 계약

- **뜻:** 따라서 “함수 끝나자마자 항상 destructor가 실행된다” 같은 implementation timing에 application correctness를 의존하지 않는다.
- **왜 중요한가:** 더 중요한 것은 어떤 path가 object를 유지하는지다.
- **예시:** 따라서 “함수 끝나자마자 항상 destructor가 실행된다” 같은 implementation …

#### 4. 객체

- **뜻:** Performance 측면에서도 reference update는 operation마다 비용을 가질 수 있다.
- **왜 중요한가:** 하지만 일반 code에서 이 비용을 이유로 구조를 복잡하게 만들기 전에 profiler로 실제 bottleneck을 확인한다.
- **예시:** Performance 측면에서도 reference update는 operation마다 비용을 가질 수 …

---

## CHAPTER 03 · reference cycle은 객체들이 서로를 가리켜 count가 0이 되지 않는 구조다

### 시작 전 용어집

#### 1. reference cycle

- **뜻:** Object A가 B를 참조하고 B가 다시 A를 참조하면 외부에서 둘을 더 이상 사용하지 않아도 서로의 reference 때문에 단순 count가 0이 되지 않을 수 있다.
- **왜 중요한가:** Container와 callback, parent-child graph에서 cycle이 자연스럽게 생긴다.
- **예시:** Parent가 child를 강하게 참조하고 child가 parent를 단지 조회만 …

#### 2. 객체

- **뜻:** Ownership이 불명확한 graph는 누가 객체를 살아 있게 해야 하는지부터 다시 설계한다.
- **왜 중요한가:** Cycle을 자동으로 발견해 수집하는 garbage collector가 있지만 모든 cycle을 수동으로 끊어야 한다는 뜻도, cycle이 전혀 문제없다는 뜻도 아니다.
- **예시:** Parent가 child를 강하게 참조하고 child가 parent를 단지 조회만 …

#### 3. count

- **뜻:** Cycle leak을 조사할 때 object type별 live count와 referrer graph를 본다.
- **왜 중요한가:** Parent가 child를 강하게 참조하고 child가 parent를 단지 조회만 하면 되는 구조라면 child→parent를 weak reference로 바꿀 수 있다.
- **예시:** Cycle leak을 조사할 때 object type별 live count와 …

#### 4. resource

- **뜻:** `__del__` finalizer, external resource, large graph가 얽히면 collection timing과 cleanup 의미가 복잡해질 수 있다.
- **왜 중요한가:** 하지만 weak reference는 architecture 문제를 자동 해결하지 않는다.
- **예시:** `__del__` finalizer, external resource, large graph가 얽히면 collection …

---

## CHAPTER 04 · weak reference는 객체를 소유하지 않고 관찰하는 관계를 표현한다

### 시작 전 용어집

#### 1. weak reference

- **뜻:** Cache, observer registry, parent back-reference처럼 객체가 존재하는 동안만 연결을 사용하고 그 연결 때문에 lifetime을 연장하고 싶지 않은 경우 weak reference를 사용할 수 있다.
- **왜 중요한가:** 대상이 다른 strong reference를 모두 잃으면 weak reference만 남아 있어도 수집될 수 있다.
- **예시:** Cache, observer registry, parent back-reference처럼 객체가 존재하는 동안만 …

#### 2. 객체

- **뜻:** 따라서 weak reference를 읽을 때 대상이 이미 사라졌을 가능성을 처리해야 한다.
- **왜 중요한가:** `None`이나 reference error 형태로 absence가 나타날 수 있으며 “조금 전 존재했으니 지금도 존재한다”는 concurrency/lifetime 가정이 깨질 수 있다.
- **예시:** 따라서 weak reference를 읽을 때 대상이 이미 사라졌을 …

#### 3. cache

- **뜻:** Weak-value cache는 값이 다른 곳에서 사용되는 동안만 cache가 유지하게 할 수 있지만 hit rate가 GC timing에 영향을 받을 수 있다.
- **왜 중요한가:** Deterministic cache retention이 필요한 경우 TTL/LRU가 더 적합할 수 있다.
- **예시:** Weak-value cache는 값이 다른 곳에서 사용되는 동안만 cache가 …

#### 4. value

- **뜻:** Weak reference는 “메모리를 줄이는 트릭”보다 non-owning relationship을 표현하는 data model 도구로 본다.
- **왜 중요한가:** Ownership graph가 명확할 때 가장 안전하다.
- **예시:** Weak reference는 “메모리를 줄이는 트릭”보다 non-owning relationship을 표현하는 …

---

## CHAPTER 05 · finalizer는 언제 호출되는지보다 어떤 일을 하면 위험한지 이해한다

### 시작 전 용어집

#### 1. finalizer

- **뜻:** Finalizer는 누락된 cleanup을 보조하는 안전망 정도로 제한하고 business transaction commit 같은 correctness-critical behavior를 두지 않는다.
- **왜 중요한가:** Finalizer에서 exception이 발생했을 때 일반 호출 stack처럼 호출자에게 전달할 수 없는 경우가 있다.
- **예시:** Resource manager object는 `with` protocol을 제공해 정상/예외 path …

#### 2. finalization

- **뜻:** Object finalization hook에서 network request, lock, 중요한 file write 같은 복잡한 작업을 수행하면 실행 시점과 error handling을 통제하기 어렵다.
- **왜 중요한가:** Interpreter shutdown 중에는 module global이 이미 정리되었거나 dependency가 사용할 수 없는 상태일 수 있다.
- **예시:** Resource manager object는 `with` protocol을 제공해 정상/예외 path …

#### 3. module

- **뜻:** External resource는 explicit `close()`와 context manager를 사용한다.
- **왜 중요한가:** Resource manager object는 `with` protocol을 제공해 정상/예외 path 모두에서 정리 순서를 명시한다.
- **예시:** External resource는 explicit `close()`와 context manager를 사용한다.

#### 4. dependency

- **뜻:** 생성자와 finalizer를 대칭으로 생각하는 C++식 RAII 직관을 그대로 Python에 적용하지 않는다.
- **왜 중요한가:** 언어와 implementation의 lifetime contract를 기준으로 설계한다.
- **예시:** 생성자와 finalizer를 대칭으로 생각하는 C++식 RAII 직관을 그대로 …

Debugging도 어려워진다. 

 

---

## CHAPTER 06 · cache는 의도적인 retention이므로 size와 eviction이 contract다

### 시작 전 용어집

#### 1. cache

- **뜻:** Memoization과 cache는 계산 결과를 reference해 나중에 재사용한다.
- **왜 중요한가:** 이는 의도적으로 객체 lifetime을 늘리는 구조다.
- **예시:** Memoization과 cache는 계산 결과를 reference해 나중에 재사용한다.

#### 2. retention

- **뜻:** LRU처럼 최대 entry 수를 두거나 TTL을 적용해 retention budget을 제한한다.
- **왜 중요한가:** Entry 하나의 실제 크기가 크게 다르면 개수 제한만으로 memory 상한을 정확히 제어하지 못할 수 있다.
- **예시:** LRU처럼 최대 entry 수를 두거나 TTL을 적용해 retention …

#### 3. size

- **뜻:** 입력 key가 계속 새로 생기는 unbounded cache는 사실상 leak처럼 memory가 끝없이 증가할 수 있다.
- **왜 중요한가:** Cache key가 큰 object를 strong reference로 보관하면 value뿐 아니라 key graph까지 lifetime이 늘어난다.
- **예시:** 입력 key가 계속 새로 생기는 unbounded cache는 사실상 …

#### 4. eviction

- **뜻:** Eviction 후에도 다른 reference가 value를 보유하면 메모리는 줄지 않는다.
- **왜 중요한가:** Cache metric과 heap snapshot을 함께 보고 실제 ownership을 확인한다.
- **예시:** Eviction 후에도 다른 reference가 value를 보유하면 메모리는 줄지 …

Cache 대상의 평균/최대 size를 측정한다.

 User object 전체 대신 stable small ID를 key로 사용할 수 있는지 본다.

 

---

## CHAPTER 07 · `__slots__`와 객체 layout 최적화는 대량 instance에서 의미가 있다

### 시작 전 용어집

#### 1. slots

- **뜻:** `__slots__`나 compact data representation을 사용하면 memory를 줄일 수 있는 경우가 있다.
- **왜 중요한가:** 하지만 slots는 inheritance, weak reference, dynamic attribute behavior에 제약을 추가하고 code flexibility를 줄인다.
- **예시:** `__slots__`나 compact data representation을 사용하면 memory를 줄일 수 …

#### 2. 객체

- **뜻:** 동일한 작은 객체를 수백만 개 생성하는 workload에서는 per-instance overhead가 상당할 수 있다.
- **왜 중요한가:** Instance 수가 수백 개뿐이라면 복잡성을 정당화하지 못할 수 있다.
- **예시:** 동일한 작은 객체를 수백만 개 생성하는 workload에서는 per-instance …

#### 3. layout

- **뜻:** 일반 Python instance는 dynamic attribute를 지원하기 위해 instance dictionary 같은 구조를 가질 수 있다.
- **왜 중요한가:** Memory profiler로 object count와 size를 먼저 확인한다.
- **예시:** 일반 Python instance는 dynamic attribute를 지원하기 위해 instance …

#### 4. instance

- **뜻:** Named tuple, dataclass slots option, array/numpy 구조처럼 같은 logical data를 더 compact하게 저장하는 선택도 있다.
- **왜 중요한가:** Python object pointer가 많은 nested list보다 contiguous numeric array가 memory locality와 vectorized operation에서 유리할 수 있다.
- **예시:** Named tuple, dataclass slots option, array/numpy 구조처럼 같은 …

Representation 최적화는 public API와 분리하면 나중에 변경하기 쉽다. Caller가 내부 dict를 직접 만지게 하지 않고 accessor/operation contract를 유지한다.

---

## CHAPTER 08 · shallow size와 retained size는 다르다

### 시작 전 용어집

#### 1. shallow size

- **뜻:** Memory leak 조사에서 한 object의 shallow size가 작아 보여도 거대한 graph의 root일 수 있다.
- **왜 중요한가:** Event listener 하나가 screen tree 전체를 붙잡는 경우가 대표적이다.
- **예시:** Memory leak 조사에서 한 object의 shallow size가 작아 …

#### 2. retained size

- **뜻:** Retained size는 특정 object가 사라질 때 함께 해제될 수 있는 전체 graph를 보려는 개념이다.
- **왜 중요한가:** 여러 root가 같은 child를 공유하면 단순 재귀 합산으로 중복 계산할 수 있다.
- **예시:** Retained size는 특정 object가 사라질 때 함께 해제될 …

#### 3. list

- **뜻:** List 1MB의 의미를 이해하려면 list slots와 각 element object, 공유 object를 어떻게 계산할지 정해야 한다.
- **왜 중요한가:** Heap profiler가 dominator tree나 allocation traceback을 제공하는 이유다.
- **예시:** List 1MB의 의미를 이해하려면 list slots와 각 element …

#### 4. slots

- **뜻:** getsizeof` 같은 도구가 한 container object 자체의 크기를 보여 줘도 그 container가 참조하는 모든 child object의 총 memory를 포함하지 않을 수 있다.
- **왜 중요한가:** 따라서 “이 변수는 작다”보다 어떤 graph를 reachability로 유지하는지 본다.
- **예시:** getsizeof` 같은 도구가 한 container object 자체의 크기를 …

`sys. 

  

 

 Data structure 설계와 memory debugging이 연결되는 지점이다.

---

## CHAPTER 09 · allocation churn과 peak memory는 leak과 다른 성능 문제다

### 시작 전 용어집

#### 1. allocation churn

- **뜻:** 많은 short-lived object를 빠르게 만들고 버리면 최종 live memory는 작아도 allocation/deallocation과 garbage collection overhead가 커질 수 있다.
- **왜 중요한가:** Parsing loop에서 substring과 temporary list를 반복 생성하는 경우가 있다.
- **예시:** `read all → parse all → transform all …

#### 2. peak memory

- **뜻:** Peak memory는 특정 단계에서 동시에 많은 intermediate representation을 보관할 때 높아진다.
- **왜 중요한가:** `read all → parse all → transform all → serialize all` 대신 streaming stage를 연결하면 peak를 낮출 수 있다.
- **예시:** Peak memory는 특정 단계에서 동시에 많은 intermediate representation을 …

#### 3. leak

- **뜻:** Object pooling을 직접 구현하는 것은 항상 이득이 아니다.
- **왜 중요한가:** Python allocator가 이미 최적화를 제공할 수 있고 pool이 stale state와 retention을 만들 수 있다.
- **예시:** Object pooling을 직접 구현하는 것은 항상 이득이 아니다.

#### 4. garbage collection

- **뜻:** Profile에서 allocation hotspot이 확인될 때 representation과 algorithm을 먼저 개선한다.
- **왜 중요한가:** Memory performance는 평균 사용량, peak, allocation rate, pause time을 서로 다르게 측정한다.
- **예시:** Profile에서 allocation hotspot이 확인될 때 representation과 algorithm을 먼저 …

하나의 숫자만 보고 원인을 추정하지 않는다.

---

## CHAPTER 10 · memory safety 설계는 ownership·lifetime·budget 세 질문으로 정리한다

### 시작 전 용어집

#### 1. memory safety

- **뜻:** 객체를 누가 소유하고 얼마나 오래 살아 있어야 하며 최대 몇 개/몇 bytes까지 허용하는지를 정하면 cache, callback, background task, stream의 memory behavior를 설명하기 쉬워진다.
- **왜 중요한가:** Ownership이 없는 reference는 weak relation이나 identifier로 표현할 수 있다.
- **예시:** 객체를 누가 소유하고 얼마나 오래 살아 있어야 하며 …

#### 2. ownership

- **뜻:** Resource lifetime은 memory lifetime과 분리한다.
- **왜 중요한가:** Socket object가 아직 memory에 살아 있다고 connection을 계속 열어 두어야 하는 것은 아니고, object가 언젠가 GC될 것이라고 close를 미룰 이유도 없다.
- **예시:** Resource lifetime은 memory lifetime과 분리한다.

#### 3. lifetime

- **뜻:** Memory 관점의 프로그래밍 사고는 **객체 생성보다 reference 관계를 보고, 자동 수집에 기대기보다 필요한 lifetime과 budget을 구조적으로 제한하는 것**이다.
- **왜 중요한가:** External resource는 explicit scope를 갖는다.
- **예시:** Memory 관점의 프로그래밍 사고는 **객체 생성보다 reference 관계를 …

#### 4. budget

- **뜻:** Memory incident에서 GC setting부터 조정하기보다 retention graph와 workload를 확인한다.
- **왜 중요한가:** 누가 객체를 붙잡는지, peak를 만드는 stage가 무엇인지, unbounded queue/cache가 있는지 조사한 뒤 mechanism을 선택한다.
- **예시:** Memory incident에서 GC setting부터 조정하기보다 retention graph와 workload를 …
