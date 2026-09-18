# PART 27 · Python cost model — 같은 결과를 더 적은 작업과 더 적은 객체로 만들기

성능 문제는 “Python이 느리다”라는 문장으로 설명할 수 없다. 실제 비용은 알고리즘의 성장률, 객체 생성 수, interpreter dispatch, memory access, I/O wait, library가 native code를 사용하는지에 따라 달라진다. 최적화는 문법 트릭을 외우는 일이 아니라 **어디서 시간이 쓰이고 어떤 비용이 입력 크기와 함께 커지는지**를 측정하고 구조를 바꾸는 과정이다.

---

## CHAPTER 01 · 먼저 `n`이 커질 때 작업량이 어떻게 증가하는지 본다

### 시작 전 용어집

#### 1. 입력

- **뜻:** 입력 100개에서는 모든 구현이 충분히 빠를 수 있다.
- **왜 중요한가:** 성능 차이는 1만, 100만으로 커질 때 드러난다.
- **예시:** 입력 100개에서는 모든 구현이 충분히 빠를 수 있다.

#### 2. list

- **뜻:** Membership 검사를 loop 안에서 list로 반복하면 전체 비용이 `n×m`으로 커질 수 있다.
- **왜 중요한가:** 같은 의미를 set으로 바꿔 lookup 구조를 사용하면 workload 전체가 달라진다.
- **예시:** Membership 검사를 loop 안에서 list로 반복하면 전체 비용이 …

#### 3. 반복

- **뜻:** 한 번의 순회는 대체로 입력 크기에 비례하지만 모든 항목 쌍을 비교하는 nested loop는 입력이 두 배가 될 때 작업량이 약 네 배가 될 수 있다.
- **왜 중요한가:** 자료구조 선택은 micro-optimization이 아니라 algorithmic optimization이다.
- **예시:** 한 번의 순회는 대체로 입력 크기에 비례하지만 모든 …

#### 4. set

- **뜻:** Big-O는 상수 비용과 실제 hardware를 숨기므로 최종 판단은 benchmark가 필요하다.
- **왜 중요한가:** 하지만 구조적으로 나쁜 성장률을 빠른 machine이나 작은 syntax 개선으로 해결하려 하지 않는다.
- **예시:** Big-O는 상수 비용과 실제 hardware를 숨기므로 최종 판단은 …

미세한 문법 차이보다 이런 성장률이 먼저다.

  

 

---

## CHAPTER 02 · Python loop 비용과 native library loop 비용은 실행 층이 다르다

### 시작 전 용어집

#### 1. Python loop

- **뜻:** Python-level loop는 각 iteration마다 object lookup과 bytecode/interpreter dispatch 비용이 발생할 수 있다.
- **왜 중요한가:** 같은 계산을 C로 구현된 built-in이나 vectorized library가 내부 loop에서 처리하면 Python frame을 매 요소마다 오가지 않아 훨씬 빠를 수 있다.
- **예시:** Python-level loop는 각 iteration마다 object lookup과 bytecode/interpreter dispatch …

#### 2. native library

- **뜻:** 그렇다고 모든 코드를 one-liner built-in으로 바꾸는 것이 목표는 아니다.
- **왜 중요한가:** 먼저 profile에서 실제 hot loop인지 확인한다.
- **예시:** 그렇다고 모든 코드를 one-liner built-in으로 바꾸는 것이 목표는 …

#### 3. frame

- **뜻:** 작은 collection에서는 readability가 더 중요하고, 대규모 numeric processing에서는 NumPy처럼 contiguous array와 native kernel을 사용하는 모델이 구조적으로 유리할 수 있다.
- **왜 중요한가:** Library call 하나가 내부에서 O(n²)라면 Python 호출 횟수가 적어도 느리다.
- **예시:** 작은 collection에서는 readability가 더 중요하고, 대규모 numeric processing에서는 …

실행 층과 알고리즘 복잡도를 함께 본다.

---

## CHAPTER 03 · attribute lookup과 function call은 반복 횟수가 매우 클 때 비용이 누적된다

### 시작 전 용어집

#### 1. attribute lookup

- **뜻:** Function call과 dynamic attribute lookup은 의미 있는 abstraction을 제공하지만 zero-cost는 아니다.
- **왜 중요한가:** 수백 번 호출하는 business code에서는 거의 문제되지 않지만 수천만 번 도는 inner loop에서는 local binding, data layout, call boundary가 성능에 영향을 줄 수 있다.
- **예시:** Function call과 dynamic attribute lookup은 의미 있는 abstraction을 …

#### 2. function call

- **뜻:** 이 때문에 hot loop를 찾기 전까지 함수를 합치거나 object abstraction을 깨뜨리는 것은 premature optimization이 될 수 있다.
- **왜 중요한가:** Profiler가 특정 small function의 call overhead를 실제 병목으로 보여 줄 때만 refactor한다.
- **예시:** 이 때문에 hot loop를 찾기 전까지 함수를 합치거나 …

#### 3. 반복

- **뜻:** Optimized path와 clear path가 달라질 경우 benchmark와 test로 behavior equivalence를 보호한다.
- **왜 중요한가:** 읽기 어려운 코드는 이후 bug cost를 늘리므로 speedup이 실제로 필요한지 수치로 판단한다.
- **예시:** Optimized path와 clear path가 달라질 경우 benchmark와 test로 …

---

## CHAPTER 04 · object allocation은 CPU와 memory bandwidth를 함께 사용한다

### 시작 전 용어집

#### 1. object allocation

- **뜻:** Loop 안에서 temporary dict/list/string을 계속 만들면 allocator와 garbage collection, memory traffic이 늘어난다.
- **왜 중요한가:** 최종 결과에 필요하지 않은 intermediate collection을 generator나 direct accumulation으로 제거하면 peak memory와 allocation rate를 낮출 수 있다.
- **예시:** Loop 안에서 temporary dict/list/string을 계속 만들면 allocator와 garbage …

#### 2. CPU

- **뜻:** 반대로 generator가 항상 빠른 것은 아니다.
- **왜 중요한가:** Lazy iterator overhead가 있고 결과를 여러 번 읽어야 하면 결국 materialize해야 할 수 있다.
- **예시:** 반대로 generator가 항상 빠른 것은 아니다.

#### 3. memory bandwidth

- **뜻:** Allocation을 줄이는 대신 code path가 복잡해지지 않는지 측정한다.
- **왜 중요한가:** String concatenation도 작은 경우 단순하지만 대량 조각을 반복적으로 새 문자열로 만드는 pattern은 copy 비용이 누적될 수 있다.
- **예시:** Allocation을 줄이는 대신 code path가 복잡해지지 않는지 측정한다.

#### 4. dict

- **뜻:** 여러 조각을 모아 join하는 방식처럼 data structure에 맞는 operation을 선택한다.
- **예시:** 여러 조각을 모아 join하는 방식처럼 data structure에 맞는 …

---

## CHAPTER 05 · cache는 계산을 줄이지만 memory와 invalidation 비용을 산다

### 시작 전 용어집

#### 1. cache

- **뜻:** 하지만 input 종류가 무한히 늘어나면 cache가 memory를 계속 점유한다.
- **왜 중요한가:** Max size와 eviction을 두고 hit ratio를 관찰한다.
- **예시:** 하지만 input 종류가 무한히 늘어나면 cache가 memory를 계속 …

#### 2. memory

- **뜻:** 같은 pure calculation이 반복된다면 memoization이 큰 speedup을 줄 수 있다.
- **왜 중요한가:** 외부 data에 의존하는 함수는 cache invalidation이 correctness 문제다.
- **예시:** 같은 pure calculation이 반복된다면 memoization이 큰 speedup을 줄 …

#### 3. validation

- **뜻:** 가격, 권한, 설정처럼 source가 변경될 수 있는 값은 TTL이나 version key가 필요하다.
- **왜 중요한가:** 오래된 값을 빠르게 반환하는 것은 성능 개선이 아니라 잘못된 결과일 수 있다.
- **예시:** 가격, 권한, 설정처럼 source가 변경될 수 있는 값은 …

#### 4. 반복

- **뜻:** Cache key가 mutable object나 불안정한 representation이면 같은 logical request가 다른 entry로 분리되거나 잘못된 hit가 생길 수 있다.
- **왜 중요한가:** Canonical key와 freshness contract를 함께 설계한다.
- **예시:** Cache key가 mutable object나 불안정한 representation이면 같은 logical …

---

## CHAPTER 06 · batch는 fixed overhead를 여러 항목에 나눠 가진다

### 시작 전 용어집

#### 1. batch

- **뜻:** 항목 1000개를 하나씩 호출하는 대신 batch로 묶으면 왕복 횟수를 줄일 수 있다.
- **왜 중요한가:** 하지만 batch가 너무 크면 latency와 memory, 실패 재처리 범위가 커진다.
- **예시:** 항목 1000개를 하나씩 호출하는 대신 batch로 묶으면 왕복 …

#### 2. fixed overhead

- **뜻:** Database나 network 요청마다 connection, serialization, syscall 같은 fixed overhead가 있다.
- **왜 중요한가:** Batch size는 throughput과 tail latency의 trade-off다.
- **예시:** Database나 network 요청마다 connection, serialization, syscall 같은 fixed …

#### 3. serialization

- **뜻:** 작은 real-time request와 대량 background import는 다른 값을 가질 수 있다.
- **왜 중요한가:** 외부 서비스가 제공하는 최대 payload와 rate limit도 고려한다.
- **예시:** 작은 real-time request와 대량 background import는 다른 값을 …

#### 4. 실패

- **뜻:** Batch 내부 일부만 실패할 수 있다면 per-item result를 제공할지 전체 실패로 볼지 contract를 정한다.
- **왜 중요한가:** 성능을 위해 합친 operation이 오류 모델까지 바꾼다는 점을 기억한다.
- **예시:** Batch 내부 일부만 실패할 수 있다면 per-item result를 …

---

## CHAPTER 07 · benchmark는 workload와 warm-up, variance를 통제해야 한다

### 시작 전 용어집

#### 1. benchmark

- **뜻:** Benchmark 자체가 실제 production workload를 대표하지 않으면 빠른 코드를 잘못 선택할 수 있다.
- **왜 중요한가:** Data size, hit ratio, error rate, concurrency를 현실적인 범위로 구성한다.
- **예시:** Benchmark 자체가 실제 production workload를 대표하지 않으면 빠른 …

#### 2. workload

- **뜻:** 코드 한 번 실행 시간을 눈으로 재는 방식은 OS scheduling, cache, background process에 크게 흔들린다.
- **왜 중요한가:** 여러 번 반복하고 분포를 보고, 비교 대상이 같은 input과 environment를 사용하는지 확인한다.
- **예시:** 코드 한 번 실행 시간을 눈으로 재는 방식은 …

#### 3. warm-up

- **뜻:** 아주 짧은 code는 timer resolution보다 짧을 수 있어 충분한 반복이 필요하다.
- **왜 중요한가:** JIT이 없는 일반 CPython에서도 filesystem cache, DNS cache, connection reuse처럼 첫 실행과 이후 실행이 다를 수 있다.
- **예시:** 아주 짧은 code는 timer resolution보다 짧을 수 있어 …

#### 4. variance

- **뜻:** Cold latency와 steady-state latency 중 무엇을 측정하는지 구분한다.
- **예시:** Cold latency와 steady-state latency 중 무엇을 측정하는지 구분한다.

---

## CHAPTER 08 · profiler는 hot path를 찾고 benchmark는 변경 효과를 비교한다

### 시작 전 용어집

#### 1. profiler

- **뜻:** Profiler는 전체 프로그램에서 시간이 어디에 쓰이는지 범위를 좁히는 데 강하고 benchmark는 특정 변경 전후를 안정적으로 비교하는 데 강하다.
- **왜 중요한가:** 두 도구를 섞어 쓰면 “왜 느린가”와 “이 수정이 실제로 빨라졌는가”를 별도로 답할 수 있다.
- **예시:** Profiler는 전체 프로그램에서 시간이 어디에 쓰이는지 범위를 좁히는 …

#### 2. path

- **뜻:** Profile 결과에서 function total time과 self time을 구분한다.
- **왜 중요한가:** 느린 함수가 실제로는 하위 network call을 기다리는 wrapper일 수 있다.
- **예시:** Profile 결과에서 function total time과 self time을 구분한다.

#### 3. benchmark

- **뜻:** 호출 횟수도 같이 봐서 작은 비용이 수백만 번 반복되는지 확인한다.
- **왜 중요한가:** 최적화 후 bottleneck이 다른 곳으로 이동할 수 있다.
- **예시:** 호출 횟수도 같이 봐서 작은 비용이 수백만 번 …

#### 4. 함수

- **뜻:** 한 번의 profile로 끝내지 않고 end-to-end 목표를 만족할 때까지 다시 측정하되, 목표 이상으로 최적화하지 않는다.
- **예시:** 한 번의 profile로 끝내지 않고 end-to-end 목표를 만족할 …

---

## CHAPTER 09 · latency 최적화와 throughput 최적화는 다른 설계를 요구할 수 있다

### 시작 전 용어집

#### 1. latency

- **뜻:** Batching은 throughput을 높이지만 첫 항목이 batch가 찰 때까지 기다려 latency가 늘 수 있다.
- **왜 중요한가:** Cache는 평균 latency를 낮추지만 miss path tail latency는 그대로일 수 있다.
- **예시:** Batching은 throughput을 높이지만 첫 항목이 batch가 찰 때까지 …

#### 2. throughput

- **뜻:** 성능 tuning은 CPU 사용률 하나가 아니라 latency distribution, throughput, memory, queue depth, error rate를 함께 본다.
- **왜 중요한가:** 한 요청을 가장 빨리 끝내는 구조와 전체 요청을 많이 처리하는 구조는 같지 않을 수 있다.
- **예시:** 성능 tuning은 CPU 사용률 하나가 아니라 latency distribution, …

#### 3. thread

- **뜻:** 요청마다 thread를 많이 만들면 개별 I/O가 겹치지만 전체 system에서는 connection contention과 context switching이 늘 수 있다.
- **왜 중요한가:** Service 목표가 p95인지 total jobs/hour인지 먼저 정한다.
- **예시:** 요청마다 thread를 많이 만들면 개별 I/O가 겹치지만 전체 …

빠르지만 실패가 늘어난 최적화는 성공이 아니다.

---

## CHAPTER 10 · 최적화 우선순위는 `알고리즘 → I/O → 표현 → 미세 비용` 순으로 좁힌다

### 시작 전 용어집

#### 1. 객체

- **뜻:** Python 성능을 배우는 목적은 빠른 문법 목록을 암기하는 것이 아니라 **작업량, 객체 수, 데이터 이동, 기다림 시간을 비용 모델로 설명하고 측정된 병목에만 구조적 개선을 적용하는 것**이다.
- **왜 중요한가:** 실무에서 가장 큰 개선은 불필요한 O(n²)을 제거하거나 round trip을 줄이고, 데이터를 더 적합한 구조로 바꾸는 데서 나오는 경우가 많다.
- **예시:** Python 성능을 배우는 목적은 빠른 문법 목록을 암기하는 …

그 뒤 memory representation과 batching, native library 활용을 본다. 마지막에야 call overhead나 작은 expression 차이를 고려한다.

Optimization은 behavior change 위험을 가진다. Before/after benchmark와 correctness regression test를 함께 유지한다. 숫자로 이득을 설명할 수 없는 복잡한 최적화는 되돌릴 준비를 한다.

**검증 메모 — CHAPTER 10 · 최적화 우선순위는 `알고리즘 → I/O → 표현 → 미세 비용` 순으로 좁힌다**
CHAPTER 10 · 최적화 우선순위는 `알고리즘 → I/O → 표현 → 미세 비용` 순으로 좁힌다을 성능 관점에서 확인할 때는 실행 시간을 한 번 재는 것으로 결론 내리지 않는다. 입력 크기와 반복 횟수를 고정하고, 준비 단계와 실제 측정 구간을 분리한 뒤 여러 번 실행해 분포를 본다. 같은 기능을 하는 대안 코드가 있다면 한 요소만 바꿔 비교하고, 속도 차이가 자료구조·할당·호출 횟수 중 어디에서 생기는지 설명한다. 결과가 예상과 다르면 측정 코드 자체의 비용과 캐시·워밍업 영향을 먼저 의심하고 다시 측정한다.
