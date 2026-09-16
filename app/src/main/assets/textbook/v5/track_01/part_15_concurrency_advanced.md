# PART 15 · 동시성 심화 — atomic primitive에서 lock-free reclamation까지

mutex를 쓰면 많은 문제를 안전하게 풀 수 있지만 고성능 runtime, scheduler, database, network stack에는 더 정교한 synchronization이 필요하다. 여기서는 `lock-free가 더 고급이니까 무조건 좋다`는 식으로 가지 않는다. **정확성 모델을 먼저 세우고 progress guarantee, memory ordering, reclamation, fairness, cancellation 비용까지 비교**한다.

---

## CHAPTER 01 · read-modify-write가 atomic primitive의 핵심이다

단순 load와 store만으로 shared counter를 안전하게 증가시키기 어렵다.

CPU는 다음과 같은 atomic read-modify-write primitive를 제공할 수 있다.

```text
compare-and-swap / compare_exchange
test-and-set
fetch-add
exchange
load-linked / store-conditional 계열
```

언어/runtime는 이를 더 높은 수준 atomic API로 노출한다.

### atomic은 operation 범위에만 적용된다

`AtomicInteger` 두 개의 각각의 update가 atomic해도 둘 사이 invariant가 자동으로 atomic해지는 것은 아니다.

---

## CHAPTER 02 · CAS를 조건부 상태 전이로 이해한다

Compare-And-Swap 개념:

```text
현재값 == expected ?
    yes → newValue로 바꾸고 성공
    no  → 바꾸지 않고 실패
```

이를 loop와 결합하면 다른 thread가 값을 바꿨을 때 다시 읽고 계산할 수 있다.

```text
loop:
  old = load
  new = f(old)
  if CAS(old, new): success
  else retry
```

### retry가 공짜가 아니다

경쟁이 심하면 여러 thread가 동시에 CAS 실패를 반복해 CPU/cache coherence traffic을 만들 수 있다.

low contention에서 빠른 구조가 high contention에서 나빠질 수 있다.

---

## CHAPTER 03 · LL/SC는 `중간에 누가 바꿨는가`를 감지하는 다른 모델이다

Load-Linked/Store-Conditional 계열에서는 linked load 뒤 관련 memory가 변경되지 않았을 때만 conditional store가 성공한다.

개념:

```text
old = LL(addr)
new = f(old)
if SC(addr, new): success
else retry
```

CAS와 비슷한 high-level algorithm을 구현할 수 있지만 architecture semantics가 다르다.

언어 atomic library를 사용할 때는 architecture 차이를 runtime/compiler가 감싸도록 두는 것이 일반적으로 안전하다.

---

## CHAPTER 04 · lock-free, wait-free, obstruction-free를 같은 말로 쓰지 않는다

### lock-free

전체 system 관점에서 어떤 operation은 계속 progress한다는 guarantee와 관련된다. 특정 thread가 starvation될 수는 있다.

### wait-free

각 operation이 bounded number of own steps 안에 완료되는 더 강한 guarantee를 목표로 한다.

### obstruction-free

혼자 실행되는 충분한 구간이 주어지면 progress할 수 있는 더 약한 성질이다.

구체 정의는 concurrency literature를 따른다.

`mutex 없음 = wait-free`가 아니다.

---

## CHAPTER 05 · lock-free가 자동으로 빠르거나 단순하지 않다

lock-free queue는 blocking lock convoy를 피할 수 있지만:

```text
atomic retry
cache-line bouncing
complex memory reclamation
harder proof/debugging
platform memory-order bugs
```

비용이 있다.

critical section이 짧고 contention이 낮으면 잘 구현된 mutex가 더 빠르고 훨씬 안전할 수 있다.

---

## CHAPTER 06 · ABA 문제는 값이 같아 보여도 history가 달라진 상황이다

CAS algorithm에서:

```text
Thread 1 reads A
Thread 2 changes A → B → A
Thread 1 CAS expects A, sees A, succeeds
```

값만 비교하면 중간 변화가 없었던 것처럼 보인다.

linked structure에서 node가 제거됐다 재사용되면 심각한 corruption을 만들 수 있다.

### mitigation

```text
version/tagged pointer
memory reclamation strategy
LL/SC semantics
algorithm-specific design
```

등을 사용한다.

---

## CHAPTER 07 · node를 list에서 제거했다고 바로 free하면 안 될 수 있다

lock-free list에서 Thread A가 node X pointer를 읽은 직후 Thread B가 X를 unlink하고 free했다고 하자.

A가 pointer를 dereference하면 use-after-free가 된다.

```text
A: p = node X
B: unlink X; free X
A: read p->next  // invalid
```

lock-free algorithm에서는 **logical removal**과 **physical memory reclamation**을 분리해야 한다.

---

## CHAPTER 08 · hazard pointer는 `내가 곧 사용할 node`를 공개한다

thread가 dereference할 pointer를 hazard slot에 등록한다.

reclaimer는 retired node가 어떤 thread hazard set에도 없을 때만 free한다.

```text
reader publishes X as hazard
↓
reclaimer sees X protected
↓
free delayed
```

대가:

```text
hazard publication atomic operations
scan cost
per-thread metadata
```

가 있다.

---

## CHAPTER 09 · epoch-based reclamation은 reader 세대가 지나갈 때까지 기다린다

reader가 active epoch를 표시하고 retired node를 바로 free하지 않는다.

모든 relevant reader가 old epoch를 떠난 뒤 그 epoch의 node를 reclaim한다.

장점은 reader hot path가 단순해질 수 있다는 점이다.

하지만 오래 멈춘 reader가 reclamation을 오래 막을 수 있다.

---

## CHAPTER 10 · RCU는 read-mostly structure의 update/read를 분리한다

Read-Copy-Update 계열 pattern은 reader가 매우 싼 방식으로 old version을 읽게 하고 writer가 새 version을 만든 뒤 publish하고 grace period 뒤 old version을 reclaim한다.

```text
readers → current version
writer creates new
↓ atomic publish
new readers → new version
old readers finish
↓ grace period
old version reclaim
```

Linux kernel의 read-mostly data structure에서 중요한 technique이다.

### RCU가 universal collection이 아니다

update가 매우 빈번하거나 reader semantics가 맞지 않으면 적합하지 않다.

---

## CHAPTER 11 · seqlock은 reader가 retry할 수 있을 때 유리하다

writer는 sequence counter를 update 전/후 바꾸고 reader는 시작/끝 sequence가 일관적인지 확인한다.

```text
reader seq1
copy data
reader seq2
if seq1 != seq2 or writer-active:
    retry
```

reader가 writer를 block하지 않는 대신 write가 자주 발생하면 reader가 반복 retry할 수 있다.

pointer lifetime이 바뀌는 data에는 추가 reclamation 문제가 있다.

---

## CHAPTER 12 · read-write lock은 reader가 많다고 무조건 이득이 아니다

RWLock은 여러 reader 동시 진입을 허용하고 writer는 exclusive access를 얻는다.

하지만:

```text
lock metadata 복잡
reader count atomic contention
writer starvation/fairness
short critical section에서 overhead
```

가 있다.

mutex와 workload를 benchmark한다.

---

## CHAPTER 13 · priority inversion은 낮은 priority task가 높은 priority task를 막는 문제다

```text
Low priority L owns mutex M
High priority H waits M
Medium priority M2 keeps running CPU
```

L이 CPU를 못 얻어 lock을 못 풀면 H가 간접적으로 medium task 때문에 오래 기다린다.

### priority inheritance

lock owner L의 effective priority를 H 수준으로 잠시 올려 빨리 critical section을 끝내게 하는 protocol이 있다.

real-time system에서 중요하다.

---

## CHAPTER 14 · lock convoy는 하나의 느린 owner 뒤에 모두 줄서는 현상이다

많은 thread가 같은 mutex를 경쟁하고 lock owner가 preempt되거나 I/O를 하면 queue 전체가 멈춘다.

```text
T1 owns lock, descheduled
T2 wait
T3 wait
T4 wait
```

critical section을 줄이고 sharding/partitioning을 고려한다.

---

## CHAPTER 15 · sharded lock은 contention domain을 나눈다

하나의 global map lock 대신 key hash로 N개의 shard lock을 둔다.

```text
key → hash % 16 → lock shard
```

서로 다른 shard의 operation은 병렬 진행할 수 있다.

대가:

```text
multiple-key operation에서 여러 lock 필요
deadlock order 관리
hot-key shard imbalance
```

이다.

---

## CHAPTER 16 · optimistic concurrency는 충돌이 드물다는 가정을 이용한다

공유 state를 lock 없이 읽고 version을 확인한 뒤 commit 시 version이 그대로인지 검사한다.

```text
read value + version 7
compute
CAS/version check
if still 7 → commit version 8
else retry
```

충돌이 적으면 lock hold time을 줄일 수 있다.

충돌이 많으면 retry waste가 커진다.

DB optimistic locking과 같은 사고 모델이다.

---

## CHAPTER 17 · linearizability는 concurrent operation이 한 순간에 일어난 것처럼 설명 가능한가를 묻는다

thread-safe collection을 검증할 때 단순히 crash가 없는 것으로 부족하다.

각 operation이 call과 return 사이 어떤 **linearization point**에서 atomic하게 발생한 것처럼 전체 history를 설명할 수 있는지 본다.

```text
A enqueue(1) start
B dequeue start
A enqueue return
B dequeue return 1
```

가능한 sequential history와 real-time order를 만족해야 한다.

---

## CHAPTER 18 · sequential consistency와 linearizability는 같은 말이 아니다

둘 다 concurrent history의 ordering을 다루지만 real-time order requirement가 다르다.

linearizability는 operation이 완료된 뒤 시작한 operation이 그 이전 효과를 보도록 real-time ordering을 포함한다.

정확한 consistency model을 문서화하지 않으면 test가 무엇을 검증해야 하는지 알 수 없다.

---

## CHAPTER 19 · double-checked locking은 memory visibility가 핵심이다

잘못된 lazy singleton:

```text
if instance == null:
  lock
  if instance == null:
     instance = new Object()
```

language memory model에 맞는 volatile/atomic publication 없이 구현하면 다른 thread가 fully initialized object를 보지 못할 수 있다.

현대 언어가 제공하는 lazy initialization primitive를 선호한다.

---

## CHAPTER 20 · safe publication은 object construction과 visibility를 연결한다

object를 생성한 thread의 field write가 다른 thread에 보이려면 synchronization relation이 필요하다.

가능한 방식:

```text
publish under mutex
volatile/atomic reference
thread-safe queue/channel
immutable object + safe publication
```

plain global variable assignment만으로 모든 언어에서 충분하다고 가정하지 않는다.

---

## CHAPTER 21 · work stealing은 불균형한 task를 worker 사이에서 나눈다

각 worker가 local deque를 가지고 자기 task를 처리하다 비면 다른 worker의 task를 steal할 수 있다.

```text
worker A queue: many tasks
worker B queue: empty
↓
B steals from A
```

recursive fork-join workload에서 load balancing에 유리하다.

### 너무 작은 task

task scheduling overhead가 실제 computation보다 커질 수 있다.

grain size가 중요하다.

---

## CHAPTER 22 · structured concurrency는 child task lifetime을 scope에 묶는다

fire-and-forget task를 계속 만들면 caller가 끝난 뒤에도 work가 남고 cancellation/error가 유실될 수 있다.

structured concurrency는 parent scope가 child lifecycle을 소유하게 한다.

```text
scope start
├─ child A
├─ child B
└─ wait/cancel policy
scope ends only when children settled
```

Kotlin coroutine scope도 이 철학을 반영한다.

---

## CHAPTER 23 · cancellation은 synchronization과 race를 만든다

child가 lock 획득 직후 cancel되면 cleanup이 lock을 release해야 한다.

```text
acquire resource
try
  suspend/work
finally
  release resource
```

cancellation-safe API는 partial state를 남기지 않는 protocol이 필요하다.

### cancellation mask/non-cancellable cleanup

일부 runtime은 critical cleanup 동안 cancellation을 잠시 지연하는 mechanism을 제공한다.

과도하게 사용하면 cancellation latency가 길어진다.

---

## CHAPTER 24 · channel은 shared memory 대신 message ownership을 전달한다

actor/channel model에서는 state owner 하나가 message를 순서대로 처리하게 할 수 있다.

```text
producer threads
↓ messages
channel
↓
single state owner
```

mutex를 줄일 수 있지만 queueing/backpressure 문제가 생긴다.

### bounded channel

producer rate가 consumer보다 빠를 때 capacity limit과 suspend/drop/reject policy를 정한다.

---

## CHAPTER 25 · actor도 deadlock과 logical race가 사라지는 것은 아니다

actor A가 B의 reply를 기다리며 자신이 처리해야 하는 message를 막고, B가 A의 message를 기다리면 protocol deadlock이 생길 수 있다.

message ordering도 network/retry와 결합되면 stale update가 가능하다.

shared memory race는 줄지만 distributed state machine correctness가 새 문제다.

---

## CHAPTER 26 · fairness는 throughput과 trade-off가 있다

lock scheduler가 완전 FIFO fairness를 강제하면 starvation은 줄지만 cache-hot owner가 다시 잡는 optimization이 제한될 수 있다.

unfair lock은 throughput이 높을 수 있지만 특정 waiter가 오래 기다릴 위험이 있다.

SLO가 tail latency인지 total throughput인지에 따라 선택이 달라진다.

---

## CHAPTER 27 · spinlock과 mutex의 선택은 wait duration과 execution context에 달려 있다

spinlock:

```text
while locked:
  CPU spins
```

짧고 sleep할 수 없는 kernel context에서는 유용할 수 있다.

user-space에서 lock hold가 길면 CPU 낭비가 크다.

adaptive mutex는 짧은 spin 후 block하는 식의 hybrid strategy를 사용할 수 있다.

---

## CHAPTER 28 · false sharing을 lock-free code에서도 본다

atomic counter를 lock-free로 만들었어도 여러 counter가 같은 cache line에 있으면 coherence traffic이 커질 수 있다.

```text
CPU0 atomic counter0
CPU1 atomic counter1
same cache line
```

algorithmic lock-free와 hardware-level contention은 별개다.

---

## CHAPTER 29 · contention collapse는 retry가 work보다 많아지는 상태다

CAS loop에 100 thread가 동시에 몰리면 한 번 성공할 때 99번 실패가 나고 다시 경쟁할 수 있다.

throughput이 thread 수 증가와 함께 오히려 떨어질 수 있다.

해결 후보:

```text
backoff
combining
sharding
queue lock
reduce concurrency
batching
```

---

## CHAPTER 30 · race detector는 happens-before violation을 찾는다

ThreadSanitizer 같은 도구는 instrumentation으로 shared memory access와 synchronization을 추적해 data race를 보고한다.

하지만:

```text
logical stale response
incorrect lock ordering but no race yet
deadlock depending on timing
atomicity violation across multiple atomic fields
```

를 모두 자동 해결하지 않는다.

---

## CHAPTER 31 · model checking과 deterministic scheduler

동시성 bug는 가능한 interleaving 수가 폭발한다.

작은 state machine에 대해 가능한 schedule을 체계적으로 탐색하거나 deterministic scheduler로 yield point를 통제하면 rare interleaving을 재현할 수 있다.

```text
T1 step1
T2 step1
T2 step2
T1 step2
```

random stress보다 더 강한 coverage를 만들 수 있다.

---

## CHAPTER 32 · linearizability test는 operation history를 기록한다

concurrent queue를 테스트할 때 각 operation의:

```text
start time
end time
input
output
```

을 history로 기록하고 valid sequential ordering이 존재하는지 검사할 수 있다.

복잡하지만 concurrent data structure correctness를 훨씬 직접 검증한다.

---

## CHAPTER 33 · concurrency limit도 correctness boundary가 된다

외부 API가 동시에 최대 10 connection만 허용한다면 semaphore limit은 단순 성능 tuning이 아니라 provider contract를 지키는 correctness rule이다.

limit을 runtime config로 바꿀 때:

```text
current inflight
new lower limit
waiting queue
cancellation
```

transition semantics도 정의해야 한다.

---

## CHAPTER 34 · Android coroutine race를 실제 사례로 본다

검색창:

```text
query A launch
query AB launch
AB returns first → UI=AB
A returns later  → UI=A (stale)
```

data race detector에는 문제가 없을 수 있다.

해결:

```text
previous job cancel
or
request sequence/token compare
or
latest-only stream operator
```

여기서 invariant는 `UI는 가장 최신 query 결과만 표시한다`다.

---

## CHAPTER 35 · 고급 동시성 선택표

```text
문제                         먼저 검토할 도구
single owner mutable state    confinement / actor
short low-contention critical mutex
many readers few writers      RWLock / immutable snapshot / RCU candidate
counter                       atomic fetch-add / sharded counter
lock-free container 필요      proven library first
high contention counter       sharding/combining before CAS storm
async child lifecycle         structured concurrency
external capacity             semaphore/bounded queue
```

직접 lock-free algorithm을 작성하는 것은 마지막 수단에 가깝다.

이미 검증된 standard library/concurrent collection을 우선한다.

---

## PART 15 종료 점검

1. CAS loop가 high contention에서 왜 느려질 수 있는가?
2. lock-free와 wait-free의 progress guarantee가 어떻게 다른가?
3. ABA 문제는 값이 같아도 왜 발생하는가?
4. lock-free structure에서 unlink와 free를 분리해야 하는 이유는 무엇인가?
5. hazard pointer와 epoch reclamation의 trade-off는 무엇인가?
6. RCU가 read-mostly workload에 유리한 이유는 무엇인가?
7. seqlock reader가 retry해야 하는 이유는 무엇인가?
8. priority inversion은 어떤 execution sequence에서 생기는가?
9. linearizability가 단순 thread safety보다 강한 무엇을 요구하는가?
10. safe publication이 memory visibility와 어떻게 연결되는가?
11. work stealing에서 task grain size가 중요한 이유는 무엇인가?
12. structured concurrency가 orphan task를 줄이는 이유는 무엇인가?
13. actor/channel도 protocol deadlock이 생길 수 있는 이유는 무엇인가?
14. lock-free라도 false sharing이 성능을 망칠 수 있는 이유는 무엇인가?
15. deterministic scheduler/model checking이 random stress보다 어떤 종류의 bug를 잘 찾는가?
16. Android 최신 검색 결과 race가 data race가 아닌 logical race인 이유는 무엇인가?

동시성을 `thread를 여러 개 쓴다`로 설명하지 않는다. **progress guarantee, atomic state transition, memory reclamation, ordering, ownership, scheduling, cancellation, history correctness**까지 연결해야 한다.