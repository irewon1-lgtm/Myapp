# PART 99 · Rate limiting과 admission control — 요청 속도·동시성·burst를 서로 다른 budget으로 관리하기

Rate limit은 “초당 몇 번” 숫자 하나로 끝나지 않는다. 초당 요청 수가 낮아도 각 요청이 오래 걸리면 동시 실행 수가 폭증할 수 있고, 평균 rate가 같아도 짧은 burst가 downstream을 무너뜨릴 수 있다. 또한 limit 초과 요청을 queue에 쌓을지 즉시 거절할지에 따라 latency와 memory가 달라진다. 이 PART에서는 **rate·concurrency·burst·queue를 별도 자원으로 분리**한다.

---

## CHAPTER 01 · rate와 concurrency는 서로 다른 부하 차원이다

### 시작 전 용어집

#### 1. rate

- **뜻:** 따라서 API 보호에는 request rate limit과 in-flight concurrency limit을 함께 사용할 수 있다.
- **왜 중요한가:** 하나는 도착 속도를, 다른 하나는 현재 점유 resource를 제한한다.
- **예시:** concurrency ≈ arrival rate × service time

#### 2. concurrency

- **뜻:** 초당 100 request가 들어와도 각 요청이 1ms면 동시 실행은 작을 수 있다.
- **왜 중요한가:** 반대로 초당 10 request인데 각 요청이 30초 걸리면 수백 개가 동시에 남을 수 있다.
- **예시:** concurrency ≈ arrival rate × service time

#### 3. semaphore

- **뜻:** Database connection, worker slot처럼 실제 bottleneck이 동시성에 묶여 있다면 semaphore/admission limit이 더 직접적인 control이다.
- **예시:** concurrency ≈ arrival rate × service time

```text
concurrency ≈ arrival rate × service time
```

 


---

## CHAPTER 02 · token bucket은 평균 rate와 제한된 burst를 함께 표현한다

### 시작 전 용어집

#### 1. token bucket

- **뜻:** Token bucket에서는 일정 속도로 token이 보충되고 요청마다 token을 소비한다.
- **왜 중요한가:** Bucket capacity만큼 token이 쌓일 수 있어 잠깐의 burst를 허용한다.
- **예시:** refill rate = 10 tokens/sec / capacity = …

#### 2. rate

- **뜻:** Idle 후 최대 20개 요청이 즉시 통과할 수 있고 이후 평균 10/sec로 제한된다.
- **왜 중요한가:** 이 모델은 고정 window 경계에서 요청이 몰리는 문제를 줄일 수 있다.
- **예시:** refill rate = 10 tokens/sec / capacity = …

#### 3. burst

- **뜻:** Operation cost가 크게 다르면 모든 request를 token 1개로 취급하지 않고 weighted cost를 고려할 수 있다.
- **왜 중요한가:** 다만 cost model이 복잡해지면 예측 가능성이 떨어지므로 실제 bottleneck과 연결한다.
- **예시:** refill rate = 10 tokens/sec / capacity = …

```text
refill rate = 10 tokens/sec
capacity = 20
request cost = 1 token
```

 

 

---

## CHAPTER 03 · burst capacity는 정상 traffic spike와 overload 사이의 완충 장치다

### 시작 전 용어집

#### 1. burst capacity

- **뜻:** Burst를 0에 가깝게 제한하면 정상적인 batch refresh나 mobile reconnect가 불필요하게 throttled될 수 있다.
- **왜 중요한가:** 너무 큰 burst는 downstream queue와 DB pool을 순간적으로 포화시킨다.
- **예시:** Burst를 0에 가깝게 제한하면 정상적인 batch refresh나 mobile …

#### 2. traffic spike

- **뜻:** Downstream이 짧게 감당할 수 있는 headroom, request cost, queue latency를 측정한다.
- **왜 중요한가:** Burst 사용 후 bucket이 회복되는 동안 client가 어떤 latency/거절을 보는지도 UX와 protocol에 포함한다.
- **예시:** Downstream이 짧게 감당할 수 있는 headroom, request cost, …

#### 3. overload

- **뜻:** Average-only dashboard는 순간 overload를 숨길 수 있다.
- **예시:** Average-only dashboard는 순간 overload를 숨길 수 있다.

Capacity는 평균 rate와 별개로 정한다. 

 

---

## CHAPTER 04 · server가 retry timing을 알려줄 수 있으면 client는 그 신호를 존중한다

### 시작 전 용어집

#### 1. server

- **뜻:** Client는 이를 무시하고 즉시 exponential retry를 시작하기보다 server hint와 자신의 deadline을 함께 고려한다.
- **왜 중요한가:** Retry hint가 30초인데 caller deadline이 2초 남았다면 기다리지 않고 실패하는 편이 맞다.
- **예시:** Client는 이를 무시하고 즉시 exponential retry를 시작하기보다 server …

#### 2. retry

- **뜻:** HTTP 응답에는 일시적 overload나 rate limit 상황에서 retry timing을 표현하는 표준 header/status semantics가 있을 수 있다.
- **왜 중요한가:** Header를 절대 신뢰할지, maximum wait를 둘지도 client policy다.
- **예시:** HTTP 응답에는 일시적 overload나 rate limit 상황에서 retry …

#### 3. client

- **뜻:** 잘못 구성된 server가 지나치게 긴 delay를 보내도 application이 무한히 block되지 않게 한다.
- **예시:** 잘못 구성된 server가 지나치게 긴 delay를 보내도 application이 …

P97의 budget과 연결한다.

 

---

## CHAPTER 05 · client-side limiter는 downstream을 보호하면서 자기 queue도 bounded하게 해야 한다

### 시작 전 용어집

#### 1. client-side limiter

- **뜻:** 한 application instance가 외부 API를 초당 50회만 호출해야 한다면 client 내부에 limiter를 둘 수 있다.
- **왜 중요한가:** 하지만 limit을 기다리는 task를 무제한 queue에 쌓으면 memory와 latency가 증가한다.
- **예시:** 한 application instance가 외부 API를 초당 50회만 호출해야 …

#### 2. stream

- **뜻:** Limiter는 downstream 보호와 자신의 resource 보호를 동시에 해야 한다.
- **왜 중요한가:** Queue wait도 request deadline에 포함하고, queue capacity 초과 시 즉시 reject하거나 load shedding한다.
- **예시:** Limiter는 downstream 보호와 자신의 resource 보호를 동시에 해야 …

#### 3. queue

- **뜻:** 오래 기다린 request가 token을 얻었을 때 이미 의미 없는 작업이 되지 않았는지 확인한다.
- **예시:** 오래 기다린 request가 token을 얻었을 때 이미 의미 …

---

## CHAPTER 06 · distributed rate limit은 여러 instance 사이 상태 일관성과 clock 문제를 만든다

### 시작 전 용어집

#### 1. distributed rate

- **뜻:** Service instance가 10개인데 각 instance가 independently 100/sec를 허용하면 전체는 1000/sec가 될 수 있다.
- **왜 중요한가:** Global limit이 필요하면 shared counter/store나 traffic gateway가 필요하다.
- **예시:** Service instance가 10개인데 각 instance가 independently 100/sec를 허용하면 …

#### 2. rate limit

- **뜻:** 분산 counter는 network latency와 failure를 추가한다.
- **왜 중요한가:** Strongly consistent exact rate보다 약간의 초과를 허용하는 approximate limiter가 더 실용적일 수 있다.
- **예시:** 분산 counter는 network latency와 failure를 추가한다.

#### 3. instance

- **뜻:** Global policy가 필요한지 per-instance protection만 필요한지 먼저 구분한다.
- **왜 중요한가:** Clock-skew에 민감한 fixed-window algorithm을 쓴다면 node time alignment도 고려한다.
- **예시:** Global policy가 필요한지 per-instance protection만 필요한지 먼저 구분한다.

---

## CHAPTER 07 · overload에서 queue와 reject 중 무엇을 선택할지 latency budget으로 판단한다

### 시작 전 용어집

#### 1. overload

- **뜻:** 요청을 queue에 넣으면 당장 거절은 줄지만 대기 시간이 늘고 memory를 점유한다.
- **왜 중요한가:** 이미 처리 용량보다 유입이 오래 높다면 queue는 문제를 미래로 미루기만 한다.
- **예시:** 요청을 queue에 넣으면 당장 거절은 줄지만 대기 시간이 …

#### 2. queue

- **뜻:** Interactive request는 짧은 queue와 빠른 reject가 더 나을 수 있고 offline batch는 bounded queue에서 기다릴 수 있다.
- **왜 중요한가:** Priority가 있다면 low-priority job을 먼저 shed하는 policy도 가능하다.
- **예시:** Interactive request는 짧은 queue와 빠른 reject가 더 나을 …

#### 3. reject

- **뜻:** Little's Law 관점에서 backlog가 계속 늘면 시스템이 안정 상태가 아니다.
- **왜 중요한가:** Queue length와 age를 함께 모니터링한다.
- **예시:** Little's Law 관점에서 backlog가 계속 늘면 시스템이 안정 …

---

## CHAPTER 08 · rate contract는 허용량보다 overload 시 behavior를 더 명확히 해야 한다

### 시작 전 용어집

#### 1. rate contract

- **뜻:** Rate policy는 refill rate, burst capacity, concurrency limit, queue capacity, reject response, retry hint, client identity 기준을 정의한다.
- **왜 중요한가:** User별·tenant별·IP별 limit은 fairness와 abuse threat model이 다르다.
- **예시:** Rate policy는 refill rate, burst capacity, concurrency limit, …

#### 2. overload

- **뜻:** 이 PART의 핵심은 **rate limiting을 숫자 하나의 throttle로 보지 않고, 도착 속도·동시 점유·burst·대기열을 분리해 overload 시 어떤 일을 받아들이고 버릴지 결정하는 admission control로 설계하는 것**이다.
- **왜 중요한가:** 테스트에서는 exact average rate뿐 아니라 burst, queue full, slow downstream, multiple instance, retry-after behavior를 검증한다.
- **예시:** 이 PART의 핵심은 **rate limiting을 숫자 하나의 throttle로 …

#### 3. behavior

- **뜻:** Load test에서 p95/p99 queue delay와 rejection ratio를 함께 본다.
- **예시:** Load test에서 p95/p99 queue delay와 rejection ratio를 함께 …

---

## 실전 학습 루프 · rate limit과 backpressure

### 1. 쉬운 예

생산자가 초당 1,000건을 보내는데 소비자가 100건만 처리할 수 있으면 queue는 계속 커진다. rate limit은 입구를 제한하고 backpressure는 downstream 처리 능력에 맞춰 upstream 속도를 줄이거나 거부하게 한다.

### 2. 한 줄 해석

처리량 제어의 목표는 모든 요청을 무조건 받는 것이 아니라 bounded resource 안에서 안정적으로 서비스를 유지하는 것이다.

### 3. 직접 실행

아래 최소 예제를 실행하기 전에 **성공 경로와 실패 경로를 각각 한 줄로 예측**한다.

```python
from collections import deque

queue = deque(maxlen=3)
for x in range(5):
    if len(queue) == queue.maxlen:
        print('reject/backpressure', x)
    else:
        queue.append(x)
```

### 4. 수정 실습

1. 고정 queue 크기에서 reject 대신 오래 대기시키면 latency가 어떻게 변하는지 생각한다.
2. client별 rate limit과 전체 시스템 concurrency limit을 분리한다.

수정 뒤에는 같은 입력을 여러 번 실행하거나 중간 crash를 가정해 결과가 중복·누락·무한 대기로 바뀌지 않는지 확인한다.

### 5. 확인 문제

queue를 아주 크게 만들면 backpressure 문제가 해결될까?

### 6. 정답과 오답 설명

**정답:** 아니다. 메모리와 지연에 문제를 옮길 뿐이며 지속적인 입력 초과는 결국 제어해야 한다.

**자주 나오는 오답:** buffer 크기 증가를 capacity 증가와 동일시하면 overload가 늦게 드러날 뿐이다.

운영형 문제에서는 함수 한 번의 정상 출력보다 **재시도, 중복, timeout, crash, 재시작** 뒤의 상태가 더 중요하다. 마지막으로 이 기능이 어떤 상태를 영구 저장하고 어떤 상태를 다시 계산할 수 있는지 구분해 적는다.

