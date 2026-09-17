# TRACK 10 · 오류·테스트·Git·빌드·배포 — 변경을 안전하게 만드는 기술

# BLOCK 05 · 성능·메모리·동시성 문제를 증거로 좁힌다

기능이 맞아도 느리거나, 메모리가 쌓이거나, 동시에 실행될 때만 깨질 수 있다. 이 BLOCK은 latency/profiling/leak/race/deadlock/resource lifetime을 초보가 직접 관찰 가능한 형태로 바꾼다.


```text
개념 설명 → 아주 쉬운 예 → 한 줄씩 해석 → 직접 실행 → 일부 수정 → 작은 문제 → 왜 맞고 틀렸는지 설명
```

---

## CHAPTER 01 · LESSON 01 · 느림을 “느낌”이 아니라 latency 분포로 측정한다

**LESSON ID:** `T10-B05-L01`

### 먼저 쉬운 말로 이해하기

성능 문제를 “앱이 답답하다”로만 표현하면 수정 목표가 없다. 어느 작업이 얼마나 자주, 얼마나 느린지 숫자로 정의해야 한다. 평균 하나만 보면 소수의 매우 느린 요청이 숨을 수 있다.

### 안에서는 실제로 무엇이 일어나는가

latency는 분포다. p50은 중앙 사용 경험, p95/p99는 느린 꼬리 구간을 보는 데 자주 쓰인다. 표본 수, 측정 지점, warm-up, device/network 조건을 함께 기록해야 비교가 의미 있다.

### 아주 쉬운 예

간단한 latency 배열에서 percentile 근사값을 계산한다.

```python
values = sorted([12, 14, 15, 16, 18, 20, 25, 40, 120, 500])

def percentile(data, p):
    idx = round((len(data) - 1) * p)
    return data[idx]

print("p50", percentile(values, 0.50))
print("p90", percentile(values, 0.90))
```

### 한 줄씩 읽기

- 정렬된 표본에서 위치를 잡는 단순 교육용 근사다.
- 실제 통계 library는 percentile 정의가 여러 가지일 수 있다.
- 500ms 같은 tail 값은 평균보다 percentile/최댓값에서 더 잘 보인다.

### 직접 실행

1. 실행해 p50/p90 차이를 본다.
2. 500을 50으로 바꿔 tail 변화가 p50에 거의 안 보이는지 확인한다.
3. 실제 기능의 시작/종료 측정 지점을 문장으로 정의한다.


### 일부를 바꿔서 다시 확인하기

같은 기능을 30회 측정하는 harness를 만들고 median과 p95를 기록한다. 성능 변경 전후는 같은 환경에서 비교한다.

### 작은 문제

평균 latency가 100ms에서 90ms로 줄었다. 모든 사용자가 빨라졌다고 말할 수 있는가?

### 왜 맞고 왜 틀리는가

아니다. tail이 악화됐을 수 있고 표본/환경이 달라졌을 수도 있다. 분포와 조건을 함께 비교해야 한다.

### 자주 만나는 실패와 확인 순서

- 한 번의 benchmark로 결론을 내리지 않는다.
- 로그 자체가 hot path 성능을 왜곡하는지 점검한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 성능은 측정 지점과 분포로 말한다.
- **직접 코딩·실행해서 익힐 것:** 반복 측정에서 median/tail을 비교한다.
- **AI에게 맡겨도 되는 것:** benchmark harness 생성과 결과 표 요약.
- **사람이 최종 확인할 것:** 측정 조건이 동등한지와 개선이 사용자 경로에 해당하는지 판단한다.

### 근거 연결

`GOOGLE-SRE` · `DORA-2025`

---

## CHAPTER 02 · LESSON 02 · profiler로 CPU 시간을 많이 쓰는 구간을 찾는다

**LESSON ID:** `T10-B05-L02`

### 먼저 쉬운 말로 이해하기

코드를 눈으로 보고 “이 함수가 느릴 것 같다”라고 추측하는 대신 profiler를 사용하면 실행 중 어디에 시간이 쓰였는지 볼 수 있다. profiler는 최적화 대상을 고르는 증거 도구다.

### 안에서는 실제로 무엇이 일어나는가

CPU profiler는 함수 호출 횟수와 누적 시간을 보여 준다. wall-clock 지연이 I/O 대기 때문이라면 CPU profiler만으로 충분하지 않을 수 있으므로 성능 문제 종류를 먼저 구분한다.

### 아주 쉬운 예

Python `cProfile`로 불필요하게 반복되는 계산을 측정한다.

```python
def work():
    total = 0
    for _ in range(2000):
        total += sum(i * i for i in range(1000))
    return total

work()
```

### 한 줄씩 읽기

- `sum`과 generator가 반복해서 호출된다.
- 정확히 어느 함수가 많은 cumulative time을 쓰는지는 profiler 결과로 확인한다.
- 최적화 전 결과 correctness 기준도 보존해야 한다.

### 직접 실행

1. 파일을 저장한다.
2. `python -m cProfile -s cumulative perf_demo.py`를 실행한다.
3. 상위 함수와 call count를 기록한다.


### 일부를 바꿔서 다시 확인하기

같은 계산값을 미리 한 번 계산해 재사용하는 버전을 만들고 다시 profile한다. 결과값이 동일한지도 assertion으로 확인한다.

### 작은 문제

profiler에서 가장 느린 함수가 library 함수다. library를 무조건 바꿔야 하는가?

### 왜 맞고 왜 틀리는가

아니다. 내 코드가 그 함수를 불필요하게 많이 호출하는지, 입력 크기가 잘못됐는지 먼저 본다. hot spot은 위치를 알려주지 설계 결론을 대신하지 않는다.

### 자주 만나는 실패와 확인 순서

- debug build/profiler overhead와 production 성능 차이를 고려한다.
- 최적화로 코드 복잡도가 커졌는데 실제 병목이 아니면 유지보수 비용만 늘 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** profiler는 실제 실행 시간을 바탕으로 hot spot을 찾는다.
- **직접 코딩·실행해서 익힐 것:** cProfile을 직접 실행하고 call count/cumulative time을 읽는다.
- **AI에게 맡겨도 되는 것:** profile 결과 요약과 최적화 후보 생성.
- **사람이 최종 확인할 것:** 성능 개선이 correctness와 유지보수성을 해치지 않는지 확인한다.

### 근거 연결

`PYTHON-PROFILE` · `GOOGLE-SRE`

---

## CHAPTER 03 · LESSON 03 · 메모리 누수는 “많이 쓴다”와 “반환되지 않는다”를 구분한다

**LESSON ID:** `T10-B05-L03`

### 먼저 쉬운 말로 이해하기

메모리를 많이 쓰는 작업이 항상 leak은 아니다. 큰 파일을 처리해 잠깐 사용량이 늘었다가 참조가 사라지고 회수되면 정상일 수 있다. leak은 더 이상 필요 없는 객체가 계속 도달 가능한 상태로 남아 사용량이 누적되는 문제다.

### 안에서는 실제로 무엇이 일어나는가

garbage-collected 언어에서도 global collection, cache, listener, closure가 객체 참조를 붙잡으면 회수되지 않는다. retained path를 따라 누가 참조를 유지하는지 찾는 사고가 중요하다.

### 아주 쉬운 예

global list가 데이터를 계속 보관하는 작은 예다.

```python
cache = []

def handle(payload):
    cache.append(payload)
    return len(payload)

for i in range(5):
    handle("x" * 1000)
    print(i, len(cache))
```

### 한 줄씩 읽기

- 각 요청 후 cache 길이가 계속 증가한다.
- cache가 global이라 함수가 끝나도 payload 참조가 남는다.
- 이것이 실제 leak인지 의도한 cache인지는 eviction 정책에 달려 있다.

### 직접 실행

1. 실행해 cache 길이 증가를 확인한다.
2. 최대 2개만 유지하도록 수정한다.
3. 장시간 반복 시 collection 크기가 bounded한지 확인한다.


### 일부를 바꿔서 다시 확인하기

`collections.deque(maxlen=2)`로 바꾼 뒤 상태가 자동 제한되는지 실행한다. memory profiler가 있다면 객체 수와 크기도 별도로 측정한다.

### 작은 문제

메모리 사용량이 1GB다. leak이라고 확정해도 되는가?

### 왜 맞고 왜 틀리는가

아니다. workload 규모와 GC 주기, cache 정책, 처리 중인 데이터가 필요하다. 시간에 따라 불필요한 retained object가 증가하는지 봐야 한다.

### 자주 만나는 실패와 확인 순서

- cache를 무조건 비우면 성능/기능 요구를 깨뜨릴 수 있다.
- heap dump에는 민감 데이터가 포함될 수 있어 취급 권한이 필요하다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** leak은 불필요한 객체가 계속 참조되어 회수되지 않는 문제다.
- **직접 코딩·실행해서 익힐 것:** bounded/unbounded collection을 반복 실행해 차이를 본다.
- **AI에게 맡겨도 되는 것:** heap 분석 절차와 retained-path 후보 제안.
- **사람이 최종 확인할 것:** 보관이 의도된 cache인지, dump 취급이 안전한지 판단한다.

### 근거 연결

`GOOGLE-SRE` · `NIST-SSDF-12`

---

## CHAPTER 04 · LESSON 04 · race condition은 결과가 실행 순서에 의존하는 문제다

**LESSON ID:** `T10-B05-L04`

### 먼저 쉬운 말로 이해하기

두 작업이 같은 상태를 읽고 바꾸는데 순서를 보장하지 않으면 특정 interleaving에서 잘못된 결과가 생길 수 있다. race는 “빠르게 실행해서 생기는 버그”가 아니라 필요한 ordering/atomicity가 없는 문제다.

### 안에서는 실제로 무엇이 일어나는가

`read → compute → write`가 하나의 원자적 연산이 아니면 두 worker가 같은 old value를 읽고 서로의 update를 덮어쓸 수 있다. lock, transaction, atomic operation, immutable message 같은 설계로 필요한 계약을 만든다.

### 아주 쉬운 예

실제 thread timing에 의존하지 않고 lost update 순서를 손으로 재현한다.

```python
balance = 100

a_read = balance
b_read = balance
a_new = a_read + 10
b_new = b_read + 20
balance = a_new
balance = b_new
print(balance)
```

### 한 줄씩 읽기

- A와 B 모두 같은 100을 읽는다.
- 각자 110과 120을 계산한다.
- 마지막 write가 120이라 기대한 130이 사라진다.

### 직접 실행

1. 실행해 120을 확인한다.
2. A write 뒤 B가 최신 110을 읽는 순서로 바꿔 130을 만든다.
3. 어떤 ordering 보장이 필요한지 문장으로 적는다.


### 일부를 바꿔서 다시 확인하기

DB라면 atomic increment나 transaction을 사용하는 대안을 조사한다. 단순 Python lock 예가 실제 분산 시스템의 동시성까지 해결한다고 확대 해석하지 않는다.

### 작은 문제

race를 재현하려고 `sleep(0.1)`을 넣어 실패가 잘 나게 만들었다. 이것이 fix인가?

### 왜 맞고 왜 틀리는가

아니다. sleep은 scheduling 확률을 바꿀 뿐 필요한 상호배제/원자성 계약을 만들지 않는다.

### 자주 만나는 실패와 확인 순서

- lock 범위를 너무 넓히면 성능/교착 위험이 커진다.
- process가 여러 개인데 thread lock 하나만으로 보호된다고 착각할 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** race는 공유 상태의 결과가 허용되지 않은 interleaving에 따라 달라지는 문제다.
- **직접 코딩·실행해서 익힐 것:** lost update 순서를 명시적으로 재현한다.
- **AI에게 맡겨도 되는 것:** 동기화 대안 목록과 stress test 생성.
- **사람이 최종 확인할 것:** 시스템 범위(thread/process/distributed)에 맞는 동기화 도구인지 확인한다.

### 근거 연결

`GOOGLE-SRE` · `PYTEST`

---

## CHAPTER 05 · LESSON 05 · deadlock은 서로가 가진 자원을 기다리는 순환이다

**LESSON ID:** `T10-B05-L05`

### 먼저 쉬운 말로 이해하기

두 작업이 각각 자원 하나를 잡고 상대 자원을 기다리면 둘 다 영원히 진행하지 못할 수 있다. 느림과 deadlock은 겉으로 “멈춤”처럼 보여도 진단이 다르다.

### 안에서는 실제로 무엇이 일어나는가

대표 조건은 상호배제, hold-and-wait, 비선점, 순환 대기다. 실무에서는 lock ordering을 통일하거나, 필요한 자원을 한 순서로 획득하거나, timeout/try-lock과 상위 복구 전략을 둔다.

### 아주 쉬운 예

실제로 영원히 멈추는 코드를 실행하지 않고 lock 순서를 데이터로 표현한다.

```python
orders = {
    "worker_a": ["account", "ledger"],
    "worker_b": ["ledger", "account"],
}
for worker, locks in orders.items():
    print(worker, "->".join(locks))
```

### 한 줄씩 읽기

- A와 B의 lock 순서가 반대다.
- A가 account, B가 ledger를 먼저 잡으면 순환 대기가 가능하다.
- 둘 다 account→ledger로 통일하면 이 특정 순환을 제거할 수 있다.

### 직접 실행

1. 실행해 두 순서를 눈으로 비교한다.
2. worker_b 순서를 account→ledger로 바꾼다.
3. 코드 리뷰 체크에 lock ordering rule을 적는다.


### 일부를 바꿔서 다시 확인하기

timeout을 추가했을 때 단순히 예외만 던지면 작업 데이터가 중간 상태로 남는지 생각한다. transaction rollback/compensation과 함께 설계한다.

### 작은 문제

deadlock을 발견했으니 lock을 모두 제거하면 되는가?

### 왜 맞고 왜 틀리는가

아니다. 원래 lock이 보호하던 invariant가 깨질 수 있다. 순서·범위·data ownership을 재설계해야 한다.

### 자주 만나는 실패와 확인 순서

- production에서 강제로 process를 kill하면 데이터 일관성 복구가 필요한지 확인한다.
- timeout을 짧게 설정해 정상적인 긴 작업까지 실패시키지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** deadlock은 대기 의존성이 순환을 만들어 진행이 멈추는 상태다.
- **직접 코딩·실행해서 익힐 것:** lock 획득 순서를 그래프로 표현해 순환 가능성을 찾는다.
- **AI에게 맡겨도 되는 것:** thread dump/lock graph 해석 보조.
- **사람이 최종 확인할 것:** lock 제거가 아니라 invariant를 보존하는 해결인지 확인한다.

### 근거 연결

`GOOGLE-SRE` · `NIST-SSDF-12`

---

## CHAPTER 06 · LESSON 06 · resource leak은 close가 필요한 자원의 수명 문제다

**LESSON ID:** `T10-B05-L06`

### 먼저 쉬운 말로 이해하기

메모리 외에도 file descriptor, socket, DB connection처럼 명시적으로 반납해야 하는 자원이 있다. 예외 경로에서 close가 빠지면 정상 테스트 몇 번은 통과해도 장시간 운영에서 고갈될 수 있다.

### 안에서는 실제로 무엇이 일어나는가

context manager/RAII 같은 구조는 “작업이 성공하든 실패하든 정리한다”는 수명 규칙을 코드 구조로 만든다. cleanup을 각 return 앞에 복사하는 것보다 안전하다.

### 아주 쉬운 예

Python 파일을 `with`로 열면 block 종료 시 닫힌다.

```python
from pathlib import Path

Path("demo.txt").write_text("hello", encoding="utf-8")
with open("demo.txt", "r", encoding="utf-8") as f:
    print(f.read())
print("closed", f.closed)
```

### 한 줄씩 읽기

- `with` 진입에서 resource를 얻는다.
- block 안에서 예외가 나도 `__exit__` 정리 경로가 호출된다.
- block 밖에서 `f.closed`가 True인지 확인한다.

### 직접 실행

1. 스크립트를 실행해 closed 상태를 확인한다.
2. with 내부에서 일부러 예외를 발생시키고 finally 후 closed 여부를 검사하는 실험을 만든다.
3. DB connection pool에서도 같은 acquire/release 사고를 적용한다.


### 일부를 바꿔서 다시 확인하기

직접 `open()` 후 여러 return branch가 있는 함수를 만들고 어느 branch에서 close가 빠질 수 있는지 찾아본다.

### 작은 문제

GC가 있으니 파일을 닫지 않아도 결국 정리되므로 괜찮은가?

### 왜 맞고 왜 틀리는가

명시적 resource 수명은 GC 시점에 맡기지 않는 것이 안전하다. descriptor 고갈·flush 지연·lock 유지가 생길 수 있다.

### 자주 만나는 실패와 확인 순서

- cleanup에서 또 예외가 나면 원래 오류가 가려질 수 있다.
- pool leak은 단순 메모리 profiler보다 active/idle connection metric이 더 직접적일 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** close가 필요한 resource는 정상/예외 모든 경로에서 수명이 끝나야 한다.
- **직접 코딩·실행해서 익힐 것:** context manager의 cleanup을 실제 예외 경로에서 확인한다.
- **AI에게 맡겨도 되는 것:** resource wrapper 코드 생성.
- **사람이 최종 확인할 것:** 도구의 실제 lifecycle 계약과 failure cleanup을 확인한다.

### 근거 연결

`PYTHON-CONTEXT` · `GOOGLE-SRE`

---

## CHAPTER 07 · LESSON 07 · 성능 수정도 correctness regression을 막아야 한다

**LESSON ID:** `T10-B05-L07`

### 먼저 쉬운 말로 이해하기

빠르게 만들었다가 결과가 틀리면 최적화가 아니다. 성능 작업에는 기준 correctness test와 benchmark가 둘 다 필요하다. 먼저 동일 결과를 보장하고, 그다음 같은 조건에서 성능 차이를 측정한다.

### 안에서는 실제로 무엇이 일어나는가

optimization은 cache, batching, algorithm 변경처럼 동작 의미에 영향을 줄 수 있다. 따라서 before/after에 같은 input/output contract를 적용하고, 성능 기준도 수치로 둔다.

### 아주 쉬운 예

제곱합을 loop와 공식 두 방식으로 계산해 결과와 시간을 비교한다.

```python
from time import perf_counter

def slow(n): return sum(i*i for i in range(1, n+1))
def fast(n): return n*(n+1)*(2*n+1)//6

n=100000
assert slow(n) == fast(n)
for fn in (slow, fast):
    t=perf_counter(); fn(n); print(fn.__name__, perf_counter()-t)
```

### 한 줄씩 읽기

- assertion이 두 구현의 결과 일치를 먼저 확인한다.
- 그 뒤 시간을 잰다.
- 한 번의 timing은 교육용 비교일 뿐 정식 benchmark 결론은 반복 측정이 필요하다.

### 직접 실행

1. 실행해 assertion 통과와 대략적인 시간 차이를 본다.
2. n을 0,1,10으로 바꿔 경계 correctness도 본다.
3. 반복 횟수와 median을 추가한다.


### 일부를 바꿔서 다시 확인하기

음수 n에 대한 계약을 정의하고 두 구현이 같은 의미를 가지는지 확인한다. 빠른 공식이 입력 domain을 다르게 처리하면 명세가 필요하다.

### 작은 문제

새 구현이 30% 빨라졌지만 한 경계 입력에서 다른 결과를 낸다. 채택해도 되는가?

### 왜 맞고 왜 틀리는가

요구사항상 그 입력이 유효하다면 안 된다. 먼저 correctness를 회복한 뒤 성능을 평가한다. 성능은 기능 정확성을 대체하지 않는다.

### 자주 만나는 실패와 확인 순서

- microbenchmark 결과를 전체 앱 체감 성능으로 확대 해석하지 않는다.
- cache 최적화가 stale data를 만들 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 성능 개선은 같은 correctness 계약을 지키면서 측정 가능한 비용을 줄이는 것이다.
- **직접 코딩·실행해서 익힐 것:** 동일 결과 assertion과 benchmark를 함께 실행한다.
- **AI에게 맡겨도 되는 것:** benchmark scaffolding과 후보 최적화 생성.
- **사람이 최종 확인할 것:** 실제 병목과 correctness/trade-off를 최종 판단한다.

### 근거 연결

`GOOGLE-SRE` · `DORA-2025`

---
