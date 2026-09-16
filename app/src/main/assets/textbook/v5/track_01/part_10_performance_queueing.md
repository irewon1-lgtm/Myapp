# PART 10 · 성능을 감으로 말하지 않는다 — latency, throughput, queueing, saturation

`느리다`는 기술 용어가 아니다. 사용자 한 요청이 오래 걸리는지, 초당 처리 가능한 양이 부족한지, CPU가 포화됐는지, queue가 쌓이는지, tail latency만 폭발하는지에 따라 해결책이 달라진다.

이 PART의 목표는 공식 암기가 아니라 **시스템의 일을 단위와 시간으로 표현하고, 어디서 대기가 생기며, 어느 자원을 늘리거나 일을 줄여야 효과가 있는지 계산하는 습관**을 만드는 것이다.

---

## CHAPTER 01 · latency와 throughput은 서로 다른 질문이다

### latency

하나의 작업이 시작해서 끝날 때까지 걸리는 시간이다.

```text
request start 10:00:00.000
response end  10:00:00.240
latency = 240ms
```

### throughput

단위 시간에 완료하는 작업 수다.

```text
600 requests / minute
= 10 requests / second
```

### 둘은 독립적으로 나빠질 수 있다

한 요청 latency가 10ms여도 worker가 하나뿐이라 초당 100건 이상 못 처리할 수 있다.

반대로 1000개의 request를 병렬 처리해 throughput은 높지만 각각 2초 걸릴 수 있다.

따라서 성능 목표는 최소 두 축으로 쓴다.

```text
p95 latency < 200ms
AND
sustained throughput >= 500 req/s
```

---

## CHAPTER 02 · service time과 waiting time을 분리한다

전체 latency는 실제 자원을 사용한 시간과 기다린 시간을 합친 결과다.

```text
latency
= queue waiting
+ CPU service
+ I/O wait
+ downstream wait
+ scheduling delay
+ serialization/etc
```

예:

```text
queue wait       300ms
CPU work          20ms
DB                80ms
network          100ms
----------------------
total            500ms
```

CPU 20ms를 2배 빠르게 만들어도 total은 490ms다.

가장 큰 waiting component를 먼저 본다.

---

## CHAPTER 03 · utilization이 100%에 가까워질수록 queue가 민감해진다

server가 평균 초당 100건 처리할 수 있고 arrival도 평균 100건이면 `딱 맞는다`고 생각하기 쉽다.

실제 traffic과 service time은 매 순간 일정하지 않다.

```text
평균 arrival = capacity
```

상태에서는 작은 burst만 와도 queue가 생기고 비울 spare capacity가 없다.

### headroom

production capacity를 평균 load와 정확히 맞추지 않는 이유다.

```text
normal load 50%
peak 70%
incident/retry 고려
```

같이 headroom을 둔다.

정확한 목표 utilization은 workload와 SLO에 따라 달라진다.

---

## CHAPTER 04 · Little’s Law로 concurrency를 연결한다

안정 상태에서 장기 평균을 생각하면 다음 관계가 매우 강력하다.

```text
L = λW
```

- L: 시스템 안에 평균 몇 개의 일이 있는가
- λ: 단위 시간당 완료/도착 rate
- W: 평균 머무는 시간

예:

```text
throughput = 100 req/s
average latency = 0.2s
```

그러면 평균 in-flight request 수는:

```text
L = 100 * 0.2 = 20
```

### 왜 유용한가

관측값 두 개로 세 번째가 말이 되는지 검산할 수 있다.

metrics에 `inflight=200`, throughput 100/s, latency 0.2s라고 나오면 measurement window나 metric 정의가 서로 다른지 의심할 수 있다.

### 평균 관계를 percentile에 그대로 쓰지 않는다

Little’s Law는 평균 관계다.

`p99 concurrency = p99 throughput × p99 latency`처럼 임의로 percentile끼리 곱하면 안 된다.

---

## CHAPTER 05 · queue는 일을 없애지 않고 시간을 뒤로 민다

queue가 있으면 producer와 consumer 속도 차이를 잠시 흡수할 수 있다.

```text
burst arrival 200/s
worker capacity 100/s
```

1초 burst면 약 100개가 queue에 남는다.

그 후 traffic이 50/s로 떨어지면 spare 50/s로 2초 동안 queue를 비울 수 있다.

### sustained overload

arrival이 계속 120/s이고 service capacity가 100/s이면:

```text
queue growth = 20/s
```

1분이면 1200개가 추가된다.

queue가 큰 것은 capacity를 만든 것이 아니다.

실패를 늦춘 것이다.

---

## CHAPTER 06 · bounded queue는 실패 방식을 선택하게 한다

unbounded queue:

```text
장점: 순간 burst 흡수
문제: 지속 overload에서 memory/latency 무한 증가
```

bounded queue:

```text
queue full
→ reject
→ block producer
→ degrade feature
→ shed load
```

중 하나를 정책으로 정할 수 있다.

`일부를 빨리 거절`하는 것이 `전부를 30초 뒤 timeout`시키는 것보다 시스템과 사용자에게 나을 수 있다.

---

## CHAPTER 07 · tail latency는 여러 단계가 연결될수록 확대된다

한 request가 20개 shard 중 모두의 결과를 기다린다고 하자.

각 shard의 대부분은 빠르지만 1%가 느리다면 전체 request가 느린 shard 하나를 만날 확률은 크게 올라간다.

단순한 독립 가정 예:

```text
각 shard가 빠를 확률 = 0.99
20개 모두 빠를 확률 = 0.99^20 ≈ 0.818
```

즉 약 18% request가 최소 한 개의 느린 shard를 만날 수 있다.

실제 시스템에서는 독립 가정이 깨질 수 있지만 **fan-out이 tail risk를 증폭한다**는 직관은 중요하다.

---

## CHAPTER 08 · timeout은 latency budget의 일부다

상위 API SLO가 1초인데 downstream timeout을 각각 2초로 설정하면 budget이 맞지 않는다.

```text
client budget 1000ms
↓
service A
  DB timeout 800ms
  remote B timeout 2000ms
```

remote B가 2초 기다리면 client는 이미 포기했다.

work가 결과를 전달할 대상 없이 계속 남는다.

### deadline propagation

상위 request가 남은 deadline을 하위 call에 전달하면 불필요한 work를 줄일 수 있다.

```text
original deadline = 1000ms
A에서 300ms 소비
B에게 remaining 700ms 전달
```

---

## CHAPTER 09 · retry는 load를 추가한다

실패율이 올라가면 retry가 늘어난다.

```text
original traffic 1000/s
20% retry once
→ additional 200/s
→ total 1200/s
```

이미 capacity 문제로 실패한 상황이면 retry가 더 큰 overload를 만들어 failure rate를 높일 수 있다.

### retry budget

무제한 retry 대신 최대 attempt, total deadline, retryable error class를 제한한다.

### idempotency

side-effect operation을 retry할 때 duplicate가 안전한지 확인한다.

성능 정책과 correctness가 연결된다.

---

## CHAPTER 10 · exponential backoff는 동시 재시도를 흩트린다

모든 client가 1초 뒤 정확히 동시에 retry하면 새로운 burst가 생긴다.

```text
failure at t=0
10000 clients
all retry t=1s
```

backoff와 jitter를 사용해 retry 시점을 분산한다.

개념:

```text
attempt 1: around 100ms
attempt 2: around 200ms
attempt 3: around 400ms
+
random jitter
```

정확한 공식은 system 특성에 맞게 정한다.

---

## CHAPTER 11 · Amdahl’s Law는 부분 최적화의 상한을 보여 준다

전체 실행시간 중 20%인 부분을 10배 빠르게 해도 전체가 10배 빨라지지 않는다.

기존 normalized time:

```text
slow part   = 0.2
other       = 0.8
```

slow part를 10배:

```text
new = 0.2/10 + 0.8
    = 0.82
```

전체 speedup:

```text
1 / 0.82 ≈ 1.22x
```

### 의미

micro-optimization 전 profiler로 그 부분이 전체에서 차지하는 비율을 확인해야 한다.

1%짜리 code를 무한히 빠르게 해도 전체 개선은 약 1%를 넘기 어렵다.

---

## CHAPTER 12 · 병렬화도 serial fraction에 막힌다

전체 중 10%가 반드시 single-thread serial section이라면 나머지 90%에 core를 무한히 투입해도 total speedup에는 상한이 있다.

```text
limit = 1 / 0.1 = 10x
```

실제론 communication/synchronization overhead 때문에 더 낮다.

`core 32개 = 32배`가 아닌 이유다.

---

## CHAPTER 13 · throughput scaling에는 coordination cost가 있다

worker 수를 1,2,4,8로 늘려 보자.

이상적:

```text
1 → 100/s
2 → 200/s
4 → 400/s
8 → 800/s
```

실제:

```text
1 → 100/s
2 → 195/s
4 → 360/s
8 → 500/s
16 → 480/s
```

8 이후 lock contention/cache pressure/DB pool competition이 커져 감소할 수 있다.

scaling curve 자체를 측정한다.

---

## CHAPTER 14 · batching은 fixed overhead를 나눠 낸다

request 하나마다 fixed 1ms setup + item당 0.1ms가 든다고 하자.

1개씩 100번:

```text
100 * (1 + 0.1) = 110ms
```

100개 batch 한 번:

```text
1 + 100*0.1 = 11ms
```

큰 차이다.

### batching의 대가

batch를 채우려고 기다리면 latency가 증가한다.

한 item failure가 batch 전체 retry를 만들 수 있다.

memory peak도 커진다.

그래서 throughput-optimized batch size와 latency-sensitive batch size가 다를 수 있다.

---

## CHAPTER 15 · cache hit ratio 하나로 성능을 설명하지 않는다

cache hit 99%가 좋아 보이지만 miss가 매우 비싸면 1%가 total latency 대부분을 만들 수 있다.

```text
hit: 1ms
miss: 1000ms
```

100 request 중:

```text
99*1 + 1*1000 = 1099ms aggregate service
```

miss 1개가 전체 비용의 대부분이다.

### weighted cost

metric은 발생 확률 × 비용으로 본다.

```text
expected cost = Σ probability_i * cost_i
```

---

## CHAPTER 16 · hit ratio를 높여도 stale correctness가 깨질 수 있다

cache TTL을 1시간으로 늘리면 hit ratio가 좋아질 수 있다.

하지만 가격/재고 데이터가 1시간 stale하면 business correctness가 깨진다.

성능 metric을 correctness constraint 안에서 최적화해야 한다.

---

## CHAPTER 17 · GC throughput과 pause latency를 구분한다

GC algorithm이 application 전체 throughput을 높여도 긴 stop-the-world pause가 있으면 UI/latency SLO가 깨질 수 있다.

반대로 pause를 극도로 짧게 줄이기 위해 background GC CPU를 많이 쓰면 throughput/배터리가 나빠질 수 있다.

성능 목표에 따라 collector/runtime tuning 기준이 달라진다.

---

## CHAPTER 18 · memory가 성능 cache가 될 수 있지만 pressure를 만든다

DB page cache, application cache, decoded image cache는 I/O/CPU를 줄일 수 있다.

하지만 memory를 많이 쓰면:

```text
GC pressure
process reclaim
page cache eviction
swap/zRAM
OOM/process kill
```

가능성이 커진다.

`RAM을 더 써서 빠르게` 전략에도 system-wide trade-off가 있다.

---

## CHAPTER 19 · compression은 network/storage와 CPU 사이의 교환이다

10MB data를 2MB로 압축할 수 있지만 압축 CPU 200ms가 든다고 하자.

느린 network에서는 이득일 수 있다.

빠른 local link에서는 압축이 오히려 늦을 수 있다.

판단:

```text
saved transfer time
>
compression + decompression CPU time ?
```

그리고 battery/thermal 비용도 본다.

---

## CHAPTER 20 · prefetch는 latency를 숨기지만 낭비를 만들 수 있다

사용자가 다음 화면을 열 확률이 높을 때 data를 미리 가져오면 체감 latency를 줄일 수 있다.

하지만 prediction이 틀리면:

```text
network bandwidth 낭비
battery 사용
cache pollution
server load
```

가 생긴다.

prefetch hit usefulness를 metric으로 본다.

---

## CHAPTER 21 · concurrency limit은 downstream을 보호한다

client가 10000개의 request를 한꺼번에 DB에 보내면 client thread는 빠르게 submit할 수 있어도 DB가 무너질 수 있다.

semaphore/pool로 in-flight를 제한한다.

```text
max 50 concurrent DB ops
remaining callers wait/reject
```

이 limit은 `더 느리게 만들기`가 아니라 **downstream saturation 이전에 queue 위치를 통제**하는 것이다.

---

## CHAPTER 22 · connection pool도 queue다

DB connection pool size 20에서 100 request가 동시에 query를 요청하면 80개는 connection을 기다릴 수 있다.

전체 DB latency metric에:

```text
pool wait 300ms
query 20ms
```

가 섞일 수 있다.

query optimizer만 튜닝해도 320→305ms 정도다.

pool wait를 별도 측정한다.

---

## CHAPTER 23 · lock contention도 queueing 문제다

mutex 하나를 100 thread가 경쟁하면 lock 앞에 논리적 queue가 생긴다.

critical section service time이 1ms이면 단일 lock은 이상적으로도 초당 대략 1000번 정도보다 훨씬 높은 serial throughput을 기대할 수 없다(실제 overhead 포함 시 더 낮다).

arrival이 이 capacity를 넘으면 wait가 증가한다.

동기화 문제도 queue model로 볼 수 있다.

---

## CHAPTER 24 · coordinated omission이 latency 측정을 속일 수 있다

load generator가 이전 request가 끝날 때까지 다음 request를 보내지 않는 closed-loop 구조라면 server가 멈춘 동안 새로운 request가 생성되지 않는다.

실제 사용자 traffic은 멈추지 않을 수 있다.

따라서 stall 동안 기다렸어야 할 request가 측정에서 빠져 tail latency가 실제보다 좋아 보이는 현상이 생긴다.

load test generator의 arrival model을 확인해야 한다.

---

## CHAPTER 25 · open-loop와 closed-loop load test를 구분한다

### closed-loop

```text
send request
wait response
send next
```

server가 느려지면 offered load도 자연히 줄어든다.

### open-loop

외부 arrival rate에 맞춰 request를 생성한다.

server가 느려져도 traffic이 계속 들어와 queueing을 더 현실적으로 드러낼 수 있다.

둘은 서로 다른 질문에 답한다.

---

## CHAPTER 26 · benchmark는 steady-state와 ramp를 분리한다

테스트 시작 직후에는:

```text
JIT warmup
cache cold
connection creation
DNS/TLS handshake
DB buffer cold
```

가 섞인다.

warm-up 구간과 steady measurement 구간을 분리한다.

반대로 startup 성능을 측정하는 benchmark라면 warm-up을 제거하면 안 된다.

질문에 맞게 실험을 설계한다.

---

## CHAPTER 27 · 성능 회귀 gate를 absolute number 하나로만 두지 않는다

CI machine load가 변하면 benchmark noise가 있다.

전략:

```text
multiple iterations
median/distribution
baseline comparison
noise budget
large regressions gate
small changes review
```

mobile device benchmark는 battery/thermal 상태까지 통제한다.

---

## CHAPTER 28 · 비용 최적화와 성능 최적화는 같지 않다

server 2배를 추가하면 latency/throughput은 개선될 수 있지만 비용도 2배 가까이 늘 수 있다.

더 효율적인 algorithm으로 같은 SLO를 절반의 자원으로 달성하면 성능과 비용이 함께 좋아진다.

성능 목표에:

```text
latency
throughput
availability
resource/cost
```

를 함께 둔다.

---

## CHAPTER 29 · energy는 mobile 성능의 또 다른 자원이다

CPU를 최고 frequency로 오래 사용하면 작업은 빨리 끝날 수 있지만 thermal throttling과 배터리 소모가 커진다.

background poll을 1초마다 하는 app은 latency는 좋을 수 있어도 radio wakeup과 battery를 낭비한다.

mobile에서는:

```text
latency
battery
thermal
network radio state
```

를 같이 최적화한다.

---

## CHAPTER 30 · 종합 capacity 계산

API worker가 평균 40ms CPU와 60ms downstream wait를 사용한다고 하자.

한 request wall latency는 약 100ms지만 worker가 async wait 동안 다른 request를 처리할 수 있는 구조라면 CPU capacity와 downstream capacity를 따로 계산해야 한다.

8 CPU core에서 core당 유효 CPU budget을 단순화해 초당 1000ms라고 하면 total 8000ms CPU/s다.

요청당 CPU 40ms면 CPU-only upper bound는:

```text
8000 / 40 ≈ 200 req/s
```

실제는 scheduler/runtime/other work/headroom 때문에 낮아진다.

DB가 150 req/s에서 saturation된다면 system capacity는 CPU 200이 아니라 DB 150 근처에서 먼저 막힐 수 있다.

capacity는 **가장 먼저 포화되는 shared resource**가 결정한다.

---

## CHAPTER 31 · 최적화 우선순위 공식

실무에서 다음 표를 만든다.

```text
component        share   improvable?  expected gain  risk
DB wait          45%     yes          high           medium
network          30%     limited      low            low
CPU parse        10%     yes          medium         low
UI               5%      yes          low            medium
other            10%
```

Amdahl 관점의 share, 수정 가능성, correctness risk를 함께 본다.

가장 눈에 띄는 code가 아니라 **전체 결과를 가장 크게 바꿀 지점**을 고른다.

---

## PART 10 종료 점검

1. latency와 throughput은 어떻게 다른가?
2. service time과 queue wait를 왜 분리해야 하는가?
3. utilization이 saturation에 가까울수록 latency가 민감해지는 이유는 무엇인가?
4. Little’s Law `L=λW`로 어떤 값들을 검산할 수 있는가?
5. unbounded queue가 sustained overload를 해결하지 못하는 이유는 무엇인가?
6. fan-out이 tail latency를 증폭할 수 있는 이유는 무엇인가?
7. retry가 overload를 악화하는 feedback loop를 설명할 수 있는가?
8. Amdahl’s Law가 micro-optimization 상한을 어떻게 보여 주는가?
9. batching이 throughput을 늘리면서 latency를 늘릴 수 있는 이유는 무엇인가?
10. connection pool wait와 query execution time을 왜 따로 측정해야 하는가?
11. coordinated omission이 latency benchmark를 어떻게 속이는가?
12. open-loop와 closed-loop load test가 무엇이 다른가?
13. mobile에서 performance와 battery/thermal을 왜 같이 봐야 하는가?
14. system capacity가 가장 먼저 포화되는 resource에 의해 제한되는 이유는 무엇인가?

이제 `빠른 코드`를 느낌으로 평가하지 않는다. **시간, rate, concurrency, queue, utilization, percentile, capacity**라는 수량으로 말할 수 있어야 한다.