# PART 99 · Rate limiting과 admission control — 요청 속도·동시성·burst를 서로 다른 budget으로 관리하기

Rate limit은 “초당 몇 번” 숫자 하나로 끝나지 않는다. 초당 요청 수가 낮아도 각 요청이 오래 걸리면 동시 실행 수가 폭증할 수 있고, 평균 rate가 같아도 짧은 burst가 downstream을 무너뜨릴 수 있다. 또한 limit 초과 요청을 queue에 쌓을지 즉시 거절할지에 따라 latency와 memory가 달라진다. 이 PART에서는 **rate·concurrency·burst·queue를 별도 자원으로 분리**한다.

---

## CHAPTER 01 · rate와 concurrency는 서로 다른 부하 차원이다

초당 100 request가 들어와도 각 요청이 1ms면 동시 실행은 작을 수 있다. 반대로 초당 10 request인데 각 요청이 30초 걸리면 수백 개가 동시에 남을 수 있다.

```text
concurrency ≈ arrival rate × service time
```

따라서 API 보호에는 request rate limit과 in-flight concurrency limit을 함께 사용할 수 있다. 하나는 도착 속도를, 다른 하나는 현재 점유 resource를 제한한다.

Database connection, worker slot처럼 실제 bottleneck이 동시성에 묶여 있다면 semaphore/admission limit이 더 직접적인 control이다.

---

## CHAPTER 02 · token bucket은 평균 rate와 제한된 burst를 함께 표현한다

Token bucket에서는 일정 속도로 token이 보충되고 요청마다 token을 소비한다. Bucket capacity만큼 token이 쌓일 수 있어 잠깐의 burst를 허용한다.

```text
refill rate = 10 tokens/sec
capacity = 20
request cost = 1 token
```

Idle 후 최대 20개 요청이 즉시 통과할 수 있고 이후 평균 10/sec로 제한된다. 이 모델은 고정 window 경계에서 요청이 몰리는 문제를 줄일 수 있다.

Operation cost가 크게 다르면 모든 request를 token 1개로 취급하지 않고 weighted cost를 고려할 수 있다. 다만 cost model이 복잡해지면 예측 가능성이 떨어지므로 실제 bottleneck과 연결한다.

---

## CHAPTER 03 · burst capacity는 정상 traffic spike와 overload 사이의 완충 장치다

Burst를 0에 가깝게 제한하면 정상적인 batch refresh나 mobile reconnect가 불필요하게 throttled될 수 있다. 너무 큰 burst는 downstream queue와 DB pool을 순간적으로 포화시킨다.

Capacity는 평균 rate와 별개로 정한다. Downstream이 짧게 감당할 수 있는 headroom, request cost, queue latency를 측정한다.

Burst 사용 후 bucket이 회복되는 동안 client가 어떤 latency/거절을 보는지도 UX와 protocol에 포함한다. Average-only dashboard는 순간 overload를 숨길 수 있다.

---

## CHAPTER 04 · server가 retry timing을 알려줄 수 있으면 client는 그 신호를 존중한다

HTTP 응답에는 일시적 overload나 rate limit 상황에서 retry timing을 표현하는 표준 header/status semantics가 있을 수 있다. Client는 이를 무시하고 즉시 exponential retry를 시작하기보다 server hint와 자신의 deadline을 함께 고려한다.

Retry hint가 30초인데 caller deadline이 2초 남았다면 기다리지 않고 실패하는 편이 맞다. P97의 budget과 연결한다.

Header를 절대 신뢰할지, maximum wait를 둘지도 client policy다. 잘못 구성된 server가 지나치게 긴 delay를 보내도 application이 무한히 block되지 않게 한다.

---

## CHAPTER 05 · client-side limiter는 downstream을 보호하면서 자기 queue도 bounded하게 해야 한다

한 application instance가 외부 API를 초당 50회만 호출해야 한다면 client 내부에 limiter를 둘 수 있다. 하지만 limit을 기다리는 task를 무제한 queue에 쌓으면 memory와 latency가 증가한다.

Queue wait도 request deadline에 포함하고, queue capacity 초과 시 즉시 reject하거나 load shedding한다. 오래 기다린 request가 token을 얻었을 때 이미 의미 없는 작업이 되지 않았는지 확인한다.

Limiter는 downstream 보호와 자신의 resource 보호를 동시에 해야 한다.

---

## CHAPTER 06 · distributed rate limit은 여러 instance 사이 상태 일관성과 clock 문제를 만든다

Service instance가 10개인데 각 instance가 independently 100/sec를 허용하면 전체는 1000/sec가 될 수 있다. Global limit이 필요하면 shared counter/store나 traffic gateway가 필요하다.

분산 counter는 network latency와 failure를 추가한다. Strongly consistent exact rate보다 약간의 초과를 허용하는 approximate limiter가 더 실용적일 수 있다.

Clock-skew에 민감한 fixed-window algorithm을 쓴다면 node time alignment도 고려한다. Global policy가 필요한지 per-instance protection만 필요한지 먼저 구분한다.

---

## CHAPTER 07 · overload에서 queue와 reject 중 무엇을 선택할지 latency budget으로 판단한다

요청을 queue에 넣으면 당장 거절은 줄지만 대기 시간이 늘고 memory를 점유한다. 이미 처리 용량보다 유입이 오래 높다면 queue는 문제를 미래로 미루기만 한다.

Interactive request는 짧은 queue와 빠른 reject가 더 나을 수 있고 offline batch는 bounded queue에서 기다릴 수 있다. Priority가 있다면 low-priority job을 먼저 shed하는 policy도 가능하다.

Little's Law 관점에서 backlog가 계속 늘면 시스템이 안정 상태가 아니다. Queue length와 age를 함께 모니터링한다.

---

## CHAPTER 08 · rate contract는 허용량보다 overload 시 behavior를 더 명확히 해야 한다

Rate policy는 refill rate, burst capacity, concurrency limit, queue capacity, reject response, retry hint, client identity 기준을 정의한다. User별·tenant별·IP별 limit은 fairness와 abuse threat model이 다르다.

테스트에서는 exact average rate뿐 아니라 burst, queue full, slow downstream, multiple instance, retry-after behavior를 검증한다. Load test에서 p95/p99 queue delay와 rejection ratio를 함께 본다.

이 PART의 핵심은 **rate limiting을 숫자 하나의 throttle로 보지 않고, 도착 속도·동시 점유·burst·대기열을 분리해 overload 시 어떤 일을 받아들이고 버릴지 결정하는 admission control로 설계하는 것**이다.
