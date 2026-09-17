# PART 90 · Memory diagnostics — tracemalloc·GC·retention graph으로 leak와 정상 cache를 구분하기

Python memory 문제를 보면 먼저 “GC가 안 돈다”거나 “메모리 leak이다”라고 단정하기 쉽다. 실제로는 live cache가 커지는 경우, traceback/frame이 객체를 붙잡는 경우, cycle이 늦게 정리되는 경우, native allocation이 Python-level 추적에 안 잡히는 경우가 서로 다르다. 이 PART에서는 **증가를 측정하고, allocation과 retention을 분리하고, 재현 가능한 window에서 비교하는 방법**을 다룬다.

---

## CHAPTER 01 · tracemalloc snapshot은 Python allocation이 어디서 늘었는지 비교할 출발점을 만든다

`tracemalloc`은 Python memory allocation의 traceback 정보를 추적해 snapshot을 만들 수 있다.

```python
import tracemalloc

tracemalloc.start()
before = tracemalloc.take_snapshot()
run_workload()
after = tracemalloc.take_snapshot()
```

한 snapshot의 top line만 보면 오래 살아 있는 정상 cache와 일시적 burst를 구분하기 어렵다. 같은 workload 전후를 비교하는 방식이 더 유용하다.

또 모든 process RSS 증가가 tracemalloc에 동일하게 잡히는 것은 아니다. Native library, mmap, allocator fragmentation 같은 영역은 별도 도구가 필요할 수 있다.

---

## CHAPTER 02 · snapshot diff는 allocation 증가 위치를 보여주지만 retention 원인을 자동 설명하지 않는다

```python
stats = after.compare_to(before, "lineno")
for stat in stats[:10]:
    print(stat)
```

특정 line에서 allocation size가 증가했다고 해서 그 line 자체가 leak 원인인 것은 아니다. 그곳에서 만든 object를 다른 global cache나 callback registry가 오래 참조할 수 있다.

따라서 allocation site와 retention owner를 분리한다. Snapshot은 “어디서 생겼나”를 알려주고 reference graph investigation은 “왜 아직 살아 있나”를 찾는다.

---

## CHAPTER 03 · GC debug는 cycle과 unreachable object를 조사하는 도구다

Reference cycle이 의심되면 `gc` module로 collector 상태와 object를 조사할 수 있다. 그러나 production request마다 전체 object graph를 훑으면 큰 overhead가 생길 수 있으므로 diagnostic window에서 제한적으로 사용한다.

Cycle 자체가 무조건 문제는 아니다. Collector가 정상적으로 회수한다면 leak이 아닐 수 있다. 문제는 cycle이 long-lived root와 연결되어 reachable 상태로 남는 경우다.

Finalizer와 resurrection 같은 특수 lifetime도 P73과 연결해 본다. GC count만 보고 root cause를 결정하지 않는다.

---

## CHAPTER 04 · object retention은 누가 strong reference를 유지하는지 찾아야 설명된다

큰 object가 살아 있다면 parent reference를 따라간다. Common root에는 module global, LRU cache, task registry, traceback, closure, queue, observer list가 있다.

예를 들어 completed Task를 history list에 계속 저장하면 task가 exception traceback과 local object를 함께 붙잡을 수 있다. Memory 사용량이 task 수와 선형 증가한다면 registry cleanup을 확인한다.

Reference graph 도구의 결과에는 debugger 자신이 만든 temporary reference도 포함될 수 있다. 관찰 행위가 graph를 바꿀 수 있음을 염두에 둔다.

---

## CHAPTER 05 · cache growth와 leak은 둘 다 memory 증가지만 정상성 판단 기준이 다르다

Cache는 의도적으로 object를 유지한다. 그래서 memory가 증가한다고 바로 leak은 아니다. 중요한 것은 cache가 bound를 가지는지, hit rate가 가치 있는지, eviction 후 memory가 안정화되는지다.

```text
working set 증가 -> cache 증가 -> 일정 크기에서 plateau
```

이 패턴은 합리적일 수 있다. 반면 request가 끝나도 unique key가 계속 쌓여 plateau가 없으면 unbounded cache가 사실상 leak처럼 동작한다.

Cache key cardinality, TTL, max size를 metric으로 관찰한다. “성능을 위해 cache한다”는 말만으로 무제한 retention을 정당화하지 않는다.

---

## CHAPTER 06 · memory measurement 자체도 overhead와 noise를 만든다

Tracemalloc frame depth를 크게 하거나 자주 snapshot을 찍으면 CPU와 memory overhead가 커진다. Debug build, profiler, test instrumentation도 allocation pattern을 바꿀 수 있다.

따라서 baseline과 instrumented run을 구분하고 overhead를 기록한다. Production에서는 sampling이나 짧은 diagnostic window를 사용할 수 있다.

RSS는 allocator가 OS에 memory를 즉시 반환하지 않아 object 회수 뒤에도 바로 줄지 않을 수 있다. Python object count 감소와 process memory 감소가 항상 동기화된다고 기대하지 않는다.

---

## CHAPTER 07 · reproduction window는 동일 workload와 warm-up을 맞춰야 비교가 의미 있다

Cold start에서 module import와 cache warm-up이 일어나면 처음 몇 요청 동안 memory가 자연스럽게 늘어난다. 이를 leak으로 오판하지 않으려면 warm-up 후 동일 request batch를 반복해 trend를 본다.

```text
warm-up -> snapshot A -> workload 1000회 -> snapshot B -> workload 1000회 -> snapshot C
```

A→B와 B→C 증가량이 비슷하게 계속 누적되는지, B 이후 plateau인지 확인한다.

재현 script에는 input cardinality와 concurrency도 고정한다. 매번 새로운 random key를 생성하면 cache growth와 leak을 구분하기 어렵다.

---

## CHAPTER 08 · memory diagnostic contract는 측정 결과와 해석을 분리한다

Memory 조사 보고서는 RSS 증가, tracemalloc diff, object count, retained owner를 별도 사실로 기록한다. “leak 원인”은 reference path나 반복 실험으로 입증한 뒤 결론낸다.

테스트에서는 cleanup 이후 object count가 돌아오는지, cache max가 지켜지는지, task/traceback registry가 제거되는지 확인한다. Native memory가 의심되면 Python-level 도구만으로 PASS라고 하지 않는다.

이 PART의 핵심은 **메모리 증가를 곧바로 GC 실패로 해석하지 않고, allocation site·strong reference retention·cache policy·allocator behavior를 서로 다른 실패 유형으로 분리해 실제 측정으로 좁히는 것**이다.
