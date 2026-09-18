# PART 90 · Memory diagnostics — tracemalloc·GC·retention graph으로 leak와 정상 cache를 구분하기

Python memory 문제를 보면 먼저 “GC가 안 돈다”거나 “메모리 leak이다”라고 단정하기 쉽다. 실제로는 live cache가 커지는 경우, traceback/frame이 객체를 붙잡는 경우, cycle이 늦게 정리되는 경우, native allocation이 Python-level 추적에 안 잡히는 경우가 서로 다르다. 이 절에서는 **증가를 측정하고, allocation과 retention을 분리하고, 재현 가능한 window에서 비교하는 방법**을 다룬다.

---

## CHAPTER 01 · tracemalloc snapshot은 Python allocation이 어디서 늘었는지 비교할 출발점을 만든다

### 시작 전 용어집

#### 1. tracemalloc snapshot

- **뜻:** `tracemalloc`은 Python memory allocation의 traceback 정보를 추적해 snapshot을 만들 수 있다.
- **왜 중요한가:** 한 snapshot의 top line만 보면 오래 살아 있는 정상 cache와 일시적 burst를 구분하기 어렵다.
- **예시:** import tracemalloc / tracemalloc.start()

#### 2. Python allocation

- **뜻:** 같은 workload 전후를 비교하는 방식이 더 유용하다.
- **왜 중요한가:** 또 모든 process RSS 증가가 tracemalloc에 동일하게 잡히는 것은 아니다.
- **예시:** import tracemalloc / tracemalloc.start()

#### 3. traceback

- **뜻:** Native library, mmap, allocator fragmentation 같은 영역은 별도 도구가 필요할 수 있다.
- **예시:** import tracemalloc / tracemalloc.start()

```python
import tracemalloc

tracemalloc.start()
before = tracemalloc.take_snapshot()
run_workload()
after = tracemalloc.take_snapshot()
```

 

 

---

**검증 시나리오 P90-C1 — CHAPTER 01 · tracemalloc snapshot은 Python allocation이 어디서 늘었는지 비교할 출발점을 만든다**
`CHAPTER 01 · tracemalloc snapshot은 Python allocation이 어디서 늘었는지 비교할 출발점을 만든다` 검증은 정상 경로를 먼저 재현하는 데서 시작한다. P90-C1에서는 `CHAPTER 01 · tracemalloc snapshot은 Python allocation이 어디서 늘었는지 비교할 출발점을 만든다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 01 · tracemalloc snapshot은 Python allocation이 어디서 늘었는지 비교할 출발점을 만든다`에 대해 오류 경로 하나를 의도적으로 만든다고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 01 · tracemalloc snapshot은 Python allocation이 어디서 늘었는지 비교할 출발점을 만든다`의 예외 종류와 직전 상태를 함께 남긴다하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 01 · tracemalloc snapshot은 Python allocation이 어디서 늘었는지 비교할 출발점을 만든다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P90-C1의 마무리는 복구 후 같은 오류가 다시 재현되지 않는지 검사하는 것이다. 통과 기준은 `CHAPTER 01 · tracemalloc snapshot은 Python allocation이 어디서 늘었는지 비교할 출발점을 만든다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 02 · snapshot diff는 allocation 증가 위치를 보여주지만 retention 원인을 자동 설명하지 않는다

### 시작 전 용어집

#### 1. snapshot diff

- **뜻:** 특정 line에서 allocation size가 증가했다고 해서 그 line 자체가 leak 원인인 것은 아니다.
- **왜 중요한가:** 그곳에서 만든 object를 다른 global cache나 callback registry가 오래 참조할 수 있다.
- **예시:** stats = after.compare_to(before, "lineno") / for stat in …

#### 2. allocation

- **뜻:** 따라서 allocation site와 retention owner를 분리한다.
- **왜 중요한가:** Snapshot은 “어디서 생겼나”를 알려주고 reference graph investigation은 “왜 아직 살아 있나”를 찾는다.
- **예시:** stats = after.compare_to(before, "lineno") / for stat in …

```python
stats = after.compare_to(before, "lineno")
for stat in stats[:10]:
    print(stat)
```

 

 

---

**검증 시나리오 P90-C2 — CHAPTER 02 · snapshot diff는 allocation 증가 위치를 보여주지만 retention 원인을 자동 설명하지 않는다**
`CHAPTER 02 · snapshot diff는 allocation 증가 위치를 보여주지만 retention 원인을 자동 설명하지 않는다` 검증은 호출 순서를 단순화하는 데서 시작한다. P90-C2에서는 `CHAPTER 02 · snapshot diff는 allocation 증가 위치를 보여주지만 retention 원인을 자동 설명하지 않는다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 02 · snapshot diff는 allocation 증가 위치를 보여주지만 retention 원인을 자동 설명하지 않는다`에 대해 순서 하나만 뒤집어 차이를 본다고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 02 · snapshot diff는 allocation 증가 위치를 보여주지만 retention 원인을 자동 설명하지 않는다`의 호출 전후의 상태 전이를 번호로 남긴다하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 02 · snapshot diff는 allocation 증가 위치를 보여주지만 retention 원인을 자동 설명하지 않는다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P90-C2의 마무리는 다른 순서에서도 계약이 유지되는지 확인하는 것이다. 통과 기준은 `CHAPTER 02 · snapshot diff는 allocation 증가 위치를 보여주지만 retention 원인을 자동 설명하지 않는다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 03 · GC debug는 cycle과 unreachable object를 조사하는 도구다

### 시작 전 용어집

#### 1. debug

- **뜻:** Reference cycle이 의심되면 `gc` module로 collector 상태와 object를 조사할 수 있다.
- **왜 중요한가:** 그러나 production request마다 전체 object graph를 훑으면 큰 overhead가 생길 수 있으므로 diagnostic window에서 제한적으로 사용한다.
- **예시:** Reference cycle이 의심되면 `gc` module로 collector 상태와 object를 …

#### 2. cycle

- **뜻:** 문제는 cycle이 long-lived root와 연결되어 reachable 상태로 남는 경우다.
- **왜 중요한가:** Finalizer와 resurrection 같은 특수 lifetime도 P73과 연결해 본다.
- **예시:** 문제는 cycle이 long-lived root와 연결되어 reachable 상태로 남는 …

#### 3. unreachable object

- **뜻:** Collector가 정상적으로 회수한다면 leak이 아닐 수 있다.
- **왜 중요한가:** GC count만 보고 root cause를 결정하지 않는다.
- **예시:** Collector가 정상적으로 회수한다면 leak이 아닐 수 있다.

Cycle 자체가 무조건 문제는 아니다.  

 

---

## CHAPTER 04 · object retention은 누가 strong reference를 유지하는지 찾아야 설명된다

### 시작 전 용어집

#### 1. object retention

- **뜻:** 큰 object가 살아 있다면 parent reference를 따라간다.
- **왜 중요한가:** Common root에는 module global, LRU cache, task registry, traceback, closure, queue, observer list가 있다.
- **예시:** 예를 들어 completed Task를 history list에 계속 저장하면 …

#### 2. strong reference

- **뜻:** 예를 들어 completed Task를 history list에 계속 저장하면 task가 exception traceback과 local object를 함께 붙잡을 수 있다.
- **왜 중요한가:** Memory 사용량이 task 수와 선형 증가한다면 registry cleanup을 확인한다.
- **예시:** 예를 들어 completed Task를 history list에 계속 저장하면 …

#### 3. module

- **뜻:** Reference graph 도구의 결과에는 debugger 자신이 만든 temporary reference도 포함될 수 있다.
- **왜 중요한가:** 관찰 행위가 graph를 바꿀 수 있음을 염두에 둔다.
- **예시:** Reference graph 도구의 결과에는 debugger 자신이 만든 temporary …

---

## CHAPTER 05 · cache growth와 leak은 둘 다 memory 증가지만 정상성 판단 기준이 다르다

### 시작 전 용어집

#### 1. cache

- **뜻:** 중요한 것은 cache가 bound를 가지는지, hit rate가 가치 있는지, eviction 후 memory가 안정화되는지다.
- **왜 중요한가:** 반면 request가 끝나도 unique key가 계속 쌓여 plateau가 없으면 unbounded cache가 사실상 leak처럼 동작한다.
- **예시:** working set 증가 -> cache 증가 -> 일정 …

#### 2. leak

- **뜻:** 그래서 memory가 증가한다고 바로 leak은 아니다.
- **왜 중요한가:** Cache key cardinality, TTL, max size를 metric으로 관찰한다.
- **예시:** working set 증가 -> cache 증가 -> 일정 …

#### 3. memory

- **뜻:** “성능을 위해 cache한다”는 말만으로 무제한 retention을 정당화하지 않는다.
- **예시:** working set 증가 -> cache 증가 -> 일정 …

Cache는 의도적으로 object를 유지한다.  

```text
working set 증가 -> cache 증가 -> 일정 크기에서 plateau
```

이 패턴은 합리적일 수 있다. 

 

---

**검증 시나리오 P90-C5 — CHAPTER 05 · cache growth와 leak은 둘 다 memory 증가지만 정상성 판단 기준이 다르다**
`CHAPTER 05 · cache growth와 leak은 둘 다 memory 증가지만 정상성 판단 기준이 다르다` 검증은 반례부터 하나 만든다하는 데서 시작한다. P90-C5에서는 `CHAPTER 05 · cache growth와 leak은 둘 다 memory 증가지만 정상성 판단 기준이 다르다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 05 · cache growth와 leak은 둘 다 memory 증가지만 정상성 판단 기준이 다르다`에 대해 반례와 정상례의 차이만 좁힌다고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 05 · cache growth와 leak은 둘 다 memory 증가지만 정상성 판단 기준이 다르다`의 중간 값을 직접 출력해 추측을 줄인다하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 05 · cache growth와 leak은 둘 다 memory 증가지만 정상성 판단 기준이 다르다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P90-C5의 마무리는 수정 후 두 사례를 같은 순서로 재검증하는 것이다. 통과 기준은 `CHAPTER 05 · cache growth와 leak은 둘 다 memory 증가지만 정상성 판단 기준이 다르다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 06 · memory measurement 자체도 overhead와 noise를 만든다

### 시작 전 용어집

#### 1. memory measurement

- **뜻:** Tracemalloc frame depth를 크게 하거나 자주 snapshot을 찍으면 CPU와 memory overhead가 커진다.
- **왜 중요한가:** Debug build, profiler, test instrumentation도 allocation pattern을 바꿀 수 있다.
- **예시:** Tracemalloc frame depth를 크게 하거나 자주 snapshot을 찍으면 …

#### 2. overhead

- **뜻:** 따라서 baseline과 instrumented run을 구분하고 overhead를 기록한다.
- **왜 중요한가:** Production에서는 sampling이나 짧은 diagnostic window를 사용할 수 있다.
- **예시:** 따라서 baseline과 instrumented run을 구분하고 overhead를 기록한다.

#### 3. noise

- **뜻:** RSS는 allocator가 OS에 memory를 즉시 반환하지 않아 object 회수 뒤에도 바로 줄지 않을 수 있다.
- **왜 중요한가:** Python object count 감소와 process memory 감소가 항상 동기화된다고 기대하지 않는다.
- **예시:** RSS는 allocator가 OS에 memory를 즉시 반환하지 않아 object …

---

## CHAPTER 07 · reproduction window는 동일 workload와 warm-up을 맞춰야 비교가 의미 있다

### 시작 전 용어집

#### 1. reproduction window

- **뜻:** Cold start에서 module import와 cache warm-up이 일어나면 처음 몇 요청 동안 memory가 자연스럽게 늘어난다.
- **왜 중요한가:** 이를 leak으로 오판하지 않으려면 warm-up 후 동일 request batch를 반복해 trend를 본다.
- **예시:** warm-up -> snapshot A -> workload 1000회 -> …

#### 2. workload

- **뜻:** A→B와 B→C 증가량이 비슷하게 계속 누적되는지, B 이후 plateau인지 확인한다.
- **왜 중요한가:** 재현 script에는 input cardinality와 concurrency도 고정한다.
- **예시:** warm-up -> snapshot A -> workload 1000회 -> …

#### 3. warm-up

- **뜻:** 매번 새로운 random key를 생성하면 cache growth와 leak을 구분하기 어렵다.
- **예시:** warm-up -> snapshot A -> workload 1000회 -> …

```text
warm-up -> snapshot A -> workload 1000회 -> snapshot B -> workload 1000회 -> snapshot C
```


 

---

**검증 시나리오 P90-C7 — CHAPTER 07 · reproduction window는 동일 workload와 warm-up을 맞춰야 비교가 의미 있다**
`CHAPTER 07 · reproduction window는 동일 workload와 warm-up을 맞춰야 비교가 의미 있다` 검증은 가장 작은 객체 상태로 시작하는 데서 시작한다. P90-C7에서는 `CHAPTER 07 · reproduction window는 동일 workload와 warm-up을 맞춰야 비교가 의미 있다` 실행 직전 상태를 먼저 적고 실행 뒤 값과 비교해 실제 규칙을 확인한다. 두 번째 단계에서는 `CHAPTER 07 · reproduction window는 동일 workload와 warm-up을 맞춰야 비교가 의미 있다`에 대해 속성 하나만 바꿔 재실행고 다른 코드는 그대로 두어 원인 후보를 하나로 제한한다. 이때 `CHAPTER 07 · reproduction window는 동일 workload와 warm-up을 맞춰야 비교가 의미 있다`의 반환값과 부수효과를 따로 기록하여 우연한 통과를 배제한다. 예상과 다르면 `CHAPTER 07 · reproduction window는 동일 workload와 warm-up을 맞춰야 비교가 의미 있다`의 타입, 값, 호출 순서, 예외 경계 가운데 최초로 달라진 항목부터 확인하고 최소 수정 뒤 다시 실행한다. P90-C7의 마무리는 수정 뒤 원래 조건을 다시 회귀 확인하는 것이다. 통과 기준은 `CHAPTER 07 · reproduction window는 동일 workload와 warm-up을 맞춰야 비교가 의미 있다`의 정상 사례와 변형 사례가 모두 설명 가능한 결과를 내고 같은 절차를 반복해도 동일한 상태 계약을 유지하는 것이다.
## CHAPTER 08 · memory diagnostic contract는 측정 결과와 해석을 분리한다

### 시작 전 용어집

#### 1. memory diagnostic

- **뜻:** Memory 조사 보고서는 RSS 증가, tracemalloc diff, object count, retained owner를 별도 사실로 기록한다.
- **왜 중요한가:** “leak 원인”은 reference path나 반복 실험으로 입증한 뒤 결론낸다.
- **예시:** Memory 조사 보고서는 RSS 증가, tracemalloc diff, object …

#### 2. contract

- **뜻:** 테스트에서는 cleanup 이후 object count가 돌아오는지, cache max가 지켜지는지, task/traceback registry가 제거되는지 확인한다.
- **왜 중요한가:** Native memory가 의심되면 Python-level 도구만으로 PASS라고 하지 않는다.
- **예시:** 테스트에서는 cleanup 이후 object count가 돌아오는지, cache max가 …

#### 3. leak

- **뜻:** 이 PART의 핵심은 **메모리 증가를 곧바로 GC 실패로 해석하지 않고, allocation site·strong reference retention·cache policy·allocator behavior를 서로 다른 실패 유형으로 분리해 실제 측정으로 좁히는 것**이다.
- **예시:** 이 PART의 핵심은 **메모리 증가를 곧바로 GC 실패로 …

---

## 실전 학습 루프 · memory diagnostics

### 1. 쉬운 예

메모리가 계속 증가한다고 곧바로 leak이라고 단정할 수 없다. allocator cache, object retention, 큰 temporary, fragmentation, native memory 등 원인이 다르다. 먼저 object 수와 allocation trace, RSS 같은 서로 다른 지표를 구분한다.

### 2. 한 줄 해석

메모리 진단은 “크다”가 아니라 어떤 메모리 층에서 무엇이 계속 남는지 증거를 모으는 과정이다.

### 3. 직접 실행

실행 전에 결과를 먼저 예상하고, 실행 후에는 **어느 경계에서 상태나 의미가 바뀌었는지** 표시한다.

```python
import tracemalloc
tracemalloc.start()
items = [bytearray(1024) for _ in range(100)]
snap = tracemalloc.take_snapshot()
for stat in snap.statistics('lineno')[:3]:
    print(stat)
```

### 4. 수정 실습

1. 동일 동작 전후 snapshot을 비교해 증가 위치를 찾는다.
2. Python object trace에 보이지 않는 native allocation 가능성도 분리한다.

수정 전후를 비교할 때는 정상 경로만 보지 않고 실패 입력과 자원 한도도 함께 확인한다.

### 5. 확인 문제

RSS가 줄지 않으면 Python 객체가 반드시 leak된 것일까?

### 6. 정답과 오답 설명

**정답:** 아니다. allocator와 OS 반환 정책 때문에 객체 해제와 RSS 감소가 즉시 일치하지 않을 수 있다.

**자주 나오는 오답:** 하나의 숫자만 보고 leak 결론을 내리는 것이 가장 흔한 진단 오류다.

마지막에는 이 주제를 **입력/신뢰 수준 → 변환 또는 대기 → 검증 → 결과/실패** 순서로 다시 설명한다. 이 순서가 보이면 실제 장애에서도 원인 경계를 빠르게 좁힐 수 있다.

## 현장 디버깅 체크 · memory diagnostics

### 증상에서 시작한다

장시간 실행 후 RSS가 증가하지만 snapshot에서는 뚜렷한 Python object 증가가 보이지 않는다. 재현 시점의 입력과 작업 식별자를 먼저 고정하고, 결과를 보고 추측하기보다 상태 전이를 시간순으로 적는다.

### 먼저 볼 증거

RSS, Python heap snapshot, object count, allocator/native buffer, cache size를 시간축으로 나눠 본다. 한 숫자만 보지 말고 **대기/실행/완료/실패**를 분리하면 병목과 논리 오류를 구분하기 쉽다.

### 일부러 실패시켜 보기

의도적 Python object retention과 native buffer 증가를 각각 만들어 tracemalloc에 보이는 차이를 비교한다. 정상 경로는 원래 잘 되는 경우가 많다. 강제 실패에서 cleanup·retry·재시작 의미가 유지되는지가 운영 품질을 결정한다.

### 통과 기준

어느 메모리 층이 증가하는지 증거로 분리한 뒤 그 층에 맞는 도구와 수정만 적용해야 한다. 이 기준을 regression test와 운영 metric 두 곳에 동시에 연결하면 배포 뒤 같은 문제가 돌아왔을 때 빠르게 탐지할 수 있다.

