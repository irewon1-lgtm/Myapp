# PART 10 · Iterator·Generator·지연 평가 — 데이터를 저장하지 않고 흐르게 하는 실행 모델

많은 프로그램은 데이터를 모두 모은 뒤 처리할 필요가 없다. 파일 줄, 네트워크 message, database row, 계산 sequence처럼 값이 순서대로 도착한다면 하나씩 생산하고 소비하는 구조가 메모리와 응답 지연을 줄일 수 있다. Python의 iterable, iterator, generator는 이런 흐름을 언어 수준에서 표현한다. 대신 단일 소비, 평가 시점, resource lifetime이라는 새로운 계약을 이해해야 한다.

---

## CHAPTER 01 · iterable과 iterator는 “반복 가능한 값”과 “진행 중인 상태”를 구분한다

List는 여러 번 `for`에 사용할 수 있는 iterable이다. `iter(list)`를 호출하면 현재 위치를 기억하는 iterator가 만들어지고 `next()`를 호출할 때마다 다음 값을 반환한다. Iterator는 어느 위치까지 소비했는지 state를 가지며 끝에 도달하면 종료 protocol을 따른다.

이 구분이 중요한 이유는 iterable이라고 해서 항상 데이터를 메모리에 보관하는 collection은 아니기 때문이다. File object, generator, custom stream도 iteration protocol을 제공할 수 있다. Consumer는 “다음 값을 주세요”라는 공통 interface를 사용하고 producer가 값들을 어디에 저장했는지는 몰라도 된다.

Iterator 자체는 보통 한 방향으로 진행한다. 한 번 끝까지 소비한 뒤 다시 처음부터 순회하려면 새로운 iterator가 필요할 수 있다. List는 `iter(list)`를 다시 호출해 새 iterator를 얻을 수 있지만 generator object 자체는 이미 소비된 상태를 되돌리지 않는다. API가 iterable을 받는지 single-pass iterator를 받는지에 따라 호출자가 기대할 수 있는 반복 가능성이 달라진다.

디버깅에서는 `for` 문을 마법으로 보지 않고 `iterator = iter(source)`와 반복적인 `next(iterator)` 호출로 풀어 생각하면 lazy behavior를 추적하기 쉽다. 어떤 시점에 실제 데이터 접근이 일어나는지, 종료가 어디서 전달되는지도 보인다.

---

## CHAPTER 02 · generator function은 호출 시 바로 본문 전체를 실행하지 않는다

함수 body에 `yield`가 있으면 일반 함수와 다른 generator execution model이 만들어진다. 호출 결과는 최종 값을 즉시 계산한 것이 아니라 iteration을 진행할 수 있는 generator object다. 실제 body 실행은 다음 값을 요청할 때 진행되고 `yield`에서 값을 내보낸 뒤 local state를 보존한 채 일시 중단된다.

```python
def count_up(limit):
    value = 0
    while value < limit:
        yield value
        value += 1
```

`count_up(3)` 호출만으로 loop가 끝까지 실행되지 않는다. 소비자가 값을 요청할 때 `0`까지 실행하고 멈추며, 다음 요청에서 이전 local variable 상태로 이어서 실행한다. 이 suspend/resume semantics가 generator의 핵심이다.

Generator는 sequence를 만드는 코드와 소비 시점을 분리한다. 따라서 오류도 generator 생성 시점이 아니라 특정 항목을 요청하는 도중 발생할 수 있다. 함수 호출이 성공했다고 데이터 전체가 유효하다고 결론 내리면 안 된다. Lazy pipeline에서는 오류가 실제 소비 위치까지 늦춰질 수 있다.

Local state가 generator lifetime 동안 유지되기 때문에 큰 객체를 참조하면 예상보다 오래 memory를 잡고 있을 수 있다. 지연 평가는 memory를 무조건 적게 쓴다는 규칙이 아니라 **필요한 상태만 유지하도록 설계할 때** 장점이 생긴다.

---

## CHAPTER 03 · lazy pipeline은 producer와 consumer 사이의 backpressure 형태를 만든다

여러 generator를 연결하면 값 하나가 source에서 filter와 transformation을 통과해 sink로 흐르는 pipeline을 만들 수 있다. 각 단계가 다음 값을 요청할 때만 upstream이 진행되므로 consumer 속도가 전체 처리 속도를 자연스럽게 제한하는 형태가 된다.

```python
lines = read_lines(path)
rows = (parse(line) for line in lines)
valid = (row for row in rows if row.is_valid)
for row in valid:
    save(row)
```

이 구조는 모든 line과 row를 동시에 list로 저장하지 않을 수 있다. 첫 valid row를 얻기 위해 필요한 만큼만 upstream이 실행되고, `save`가 느리면 다음 값을 요청하는 속도도 느려진다. 메모리 buffer가 무제한으로 쌓이는 eager producer보다 안정적인 경우가 있다.

하지만 각 단계가 network를 직접 호출하거나 외부 resource를 오래 보유하면 pipeline의 lifetime이 길어지는 만큼 lock이나 connection도 오래 유지될 수 있다. Lazy 구조에서는 “값을 언제 계산하는가”뿐 아니라 “resource를 언제 획득하고 언제 해제하는가”를 함께 설계해야 한다.

병렬 처리나 async stream에서는 명시적인 queue와 buffer가 들어가며 backpressure가 더 복잡해진다. 기본 generator pipeline에서 얻는 핵심 사고는 producer와 consumer의 속도가 독립적일 수 있고, 중간에 데이터를 얼마나 저장할지가 시스템 안정성에 영향을 준다는 점이다.

---

## CHAPTER 04 · generator expression과 list comprehension은 평가 시점이 다르다

`[transform(x) for x in source]`는 일반적으로 결과 list 전체를 만들고, `(transform(x) for x in source)`는 값을 요청할 때 계산하는 generator expression을 만든다. 괄호 한 종류 차이가 memory와 exception 시점, source lifetime을 바꿀 수 있다.

Source가 작고 결과를 여러 번 사용하거나 random access가 필요하다면 list가 단순하다. Source가 매우 크고 한 번 순차 처리한다면 generator가 적합할 수 있다. “generator가 더 고급이므로 항상 사용”하는 식의 선택은 오히려 single-pass bug와 lifetime 문제를 만든다.

Generator expression이 외부 variable을 참조하면 실제 평가 시점에 그 variable 값이 무엇인지도 중요해질 수 있다. Expression을 만들 때 모든 값을 snapshot했다고 가정하면 late evaluation 때문에 다른 결과를 볼 수 있다. Evaluation time을 계약의 일부로 본다.

성능 측정에서도 전체 작업 시간을 비교해야 한다. Generator는 큰 intermediate list allocation을 피하지만 각 항목마다 iteration overhead가 존재하고 CPU cache 특성이 달라질 수 있다. 데이터 크기와 소비 방식이 실제 workload에서 어떤지 측정한 뒤 선택한다.

---

## CHAPTER 05 · `yield from`은 nested iteration의 값과 종료 의미를 위임한다

Generator가 다른 iterable의 모든 값을 그대로 중계해야 할 때 수동 `for` loop로 하나씩 yield할 수 있다. `yield from`은 이 위임을 언어 수준에서 표현하며 단순 값 전달뿐 아니라 generator protocol의 일부 상호작용을 연결한다.

```python
def flatten(groups):
    for group in groups:
        yield from group
```

이 예제에서는 각 group의 원소를 순차적으로 외부 consumer에게 전달한다. 직접 중첩 loop를 쓰는 것보다 “이 iterable에 iteration을 위임한다”는 의도가 드러난다. Recursive tree traversal에서도 child generator를 연결할 때 사용할 수 있다.

위임된 generator의 종료값과 `send`, `throw`, `close` 같은 더 고급 protocol을 다룰 수 있다는 점은 coroutine-style 사용과 연결되지만, 일반 데이터 pipeline에서는 불필요한 복잡성을 피하는 편이 좋다. 기본 iteration이 목적이면 값 전달 semantics만으로 충분하다.

중요한 것은 nested iterable의 경계가 사라지는지 여부다. `yield from group`은 group 자체를 한 값으로 내보내는 것이 아니라 내부 원소들을 외부 sequence에 펼친다. 데이터 구조에서 group boundary가 의미 있다면 flatten하지 않아야 한다. 문법 편의가 domain structure를 지우지 않게 한다.

---

## CHAPTER 06 · iterator를 두 소비자가 공유하면 진행 상태도 공유한다

Iterator는 현재 위치라는 mutable state를 가지므로 같은 iterator object를 두 코드가 번갈아 소비하면 서로의 진행에 영향을 준다. 한 consumer가 값을 하나 가져가면 다른 consumer에게는 그 값이 다시 제공되지 않는다. Iterator를 immutable sequence처럼 공유하면 데이터 누락처럼 보이는 문제가 생길 수 있다.

```python
it = iter([10, 20, 30])
a = next(it)  # 10
b = next(it)  # 20
```

함수에 iterator를 전달했는데 내부에서 validation을 위해 한 번 모두 순회한 뒤 실제 처리에서 다시 순회하려 하면 두 번째에는 값이 남아 있지 않을 수 있다. “입력을 검사한 것”이 실제로는 소비한 것이다. 여러 번 읽어야 한다면 materialize하거나 재생성 가능한 iterable을 요구해야 한다.

`itertools.tee`처럼 iterator를 논리적으로 복제하는 도구도 내부 buffer 비용이 생길 수 있다. 한 소비자가 매우 느리면 아직 소비되지 않은 항목을 저장해야 하므로 원래 stream의 memory 장점을 잃을 수 있다. 복제는 무료가 아니다.

API 문서에서 parameter가 iterable인지 iterator인지 구분하고, 함수가 입력을 몇 번 순회하는지 명확하게 하면 이런 오류를 줄일 수 있다. Single-pass property는 데이터 타입만큼 중요한 behavior contract다.

---

## CHAPTER 07 · generator와 resource lifetime을 함께 설계하지 않으면 handle이 오래 열린다

파일 generator가 `with open(...)` 내부에서 값을 yield한다면 file은 generator가 끝까지 소비되거나 정리될 때까지 열려 있을 수 있다. 소비자가 첫 몇 줄만 읽고 generator를 보관한다면 예상보다 오래 file descriptor를 점유할 수 있다. Lazy computation은 resource lifetime도 지연시킨다.

API를 설계할 때 resource ownership을 결정한다. Generator 함수가 resource를 열고 닫을 책임을 가진다면 조기 종료 시 cleanup이 확실히 실행되도록 해야 한다. 호출자가 이미 열린 resource를 전달하는 구조라면 lifetime 책임은 호출자에게 있을 수 있다. 두 모델을 섞으면 누가 닫아야 하는지 불명확해진다.

Database cursor나 network response를 generator로 감쌀 때도 같은 문제가 더 크게 나타난다. Connection pool slot을 오래 점유하면 다른 요청이 resource를 얻지 못할 수 있다. 작은 memory 절약이 전체 throughput을 떨어뜨릴 수도 있다.

따라서 lazy API는 maximum consumption time, cancellation, close behavior를 고려한다. 필요한 데이터가 작다면 connection 내부에서 list로 materialize한 뒤 resource를 빠르게 반환하는 것이 전체 시스템에는 더 나을 수 있다. 메모리와 resource occupancy를 함께 최적화한다.

---

## CHAPTER 08 · generator 내부 예외는 소비 시점의 오류 경계에서 처리한다

Generator pipeline에서 parsing function이 잘못된 row를 만나 exception을 던지면 해당 row가 실제로 요청되는 순간 pipeline이 중단된다. Pipeline을 만드는 코드와 소비하는 코드가 멀리 떨어져 있다면 stack과 책임 경계도 달라질 수 있다. 오류 처리를 producer 생성 시점에만 두면 실제 failure를 잡지 못한다.

각 item 실패를 건너뛸지 전체 stream을 실패시킬지도 정책이다. Financial transaction처럼 한 항목 오류가 전체 batch의 정확성을 깨뜨릴 수 있으면 즉시 중단해야 할 수 있다. 로그 ingestion처럼 일부 malformed line을 quarantine하고 계속 처리하는 것이 더 적합할 수도 있다.

항목 오류를 계속 처리하려면 실패 정보도 stream data로 모델링할 수 있다. `(success, value_or_error)` 형태나 명시적인 result object를 yield하면 consumer가 error count와 bad record를 관리할 수 있다. 모든 exception을 generator 내부에서 로그만 남기고 버리면 데이터 손실이 보이지 않는다.

Exception 이후 generator가 어떤 상태에 있는지도 이해해야 한다. 일반적으로 예외가 generator 밖으로 전파되어 종료되면 같은 generator에서 처리를 계속할 수 없을 수 있다. Per-item recovery가 필요하면 예외가 밖으로 나오기 전에 해당 iteration 내부에서 정책적으로 처리해야 한다.

---

## CHAPTER 09 · 무한 sequence는 종료 대신 소비 budget을 계약으로 둔다

`itertools.count()`처럼 끝이 없는 sequence도 iterator로 표현할 수 있다. Event stream, sensor feed, queue consumer 역시 논리적으로 무한할 수 있다. 이런 source에서 `list(source)`처럼 전체 materialization을 시도하면 종료하지 않거나 memory를 고갈시킬 수 있다.

무한 stream은 producer의 종료가 아니라 consumer의 budget으로 제어한다. 첫 N개, deadline까지, 특정 predicate가 참일 때까지, cancellation signal이 올 때까지 같은 조건을 둔다. `islice` 같은 도구로 유한 window를 만들 수 있다.

Windowing은 streaming system의 핵심 개념으로 확장된다. 최근 5분, 100개 단위 batch, key별 session처럼 무한 입력을 유한 상태로 처리한다. Window boundary와 늦게 도착한 데이터 처리 규칙이 결과 semantics를 결정한다.

작은 Python generator에서도 “이 sequence가 유한하다는 보장이 있는가”를 확인하는 습관은 중요하다. 외부 API pagination이 버그로 같은 page token을 계속 반환하면 이론상 유한한 조회가 실제로 무한 loop가 될 수 있다. 중복 token 감지와 page budget 같은 defensive termination condition을 둘 수 있다.

---

## CHAPTER 10 · eager와 lazy의 선택은 계산 위치·메모리·실패·관측성을 함께 결정한다

Eager evaluation은 결과를 즉시 계산해 이후 사용 시점의 behavior를 단순하게 만든다. 초기 비용과 memory가 더 들 수 있지만 오류가 일찍 발생하고 resource를 빠르게 닫을 수 있다. Lazy evaluation은 필요할 때만 계산해 초기 latency와 memory를 줄일 수 있지만 실제 작업이 소비 시점으로 이동한다.

이 차이는 logging과 profiling에도 영향을 준다. Generator를 만드는 함수가 매우 빠르게 끝났다고 해서 실제 데이터 처리가 빨랐다는 뜻이 아니다. 처리 비용은 downstream iteration 위치에서 발생한다. 성능 측정 구간이 실제 consumption을 포함하는지 확인해야 한다.

Caching과도 관계가 있다. Lazy 계산 결과를 한 번 materialize해 cache하면 이후에는 eager snapshot처럼 동작한다. 반대로 매 iteration마다 원본 source를 다시 계산하는 iterable은 데이터가 변경되면 결과도 달라질 수 있다. Snapshot semantics가 필요한지 live view가 필요한지 결정한다.

선택 기준을 하나로 줄이지 않는다. 데이터 크기, 첫 결과가 필요한 시간, 전체 반복 횟수, random access, 오류를 언제 보고 싶은지, resource를 얼마나 오래 보유할 수 있는지, producer와 consumer 속도 차이를 함께 본다. Iterator와 generator의 핵심은 문법 축약이 아니라 **계산의 시간과 상태를 프로그램 구조 안에서 명시적으로 다루는 능력**이다.