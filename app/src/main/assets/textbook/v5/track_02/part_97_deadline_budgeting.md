# PART 97 · Deadline budgeting — timeout을 계층마다 새로 시작하지 않고 남은 시간으로 전달하기

네트워크 요청이 여러 단계의 서비스를 거치면 각 단계에서 `timeout=5`를 새로 주는 방식은 전체 요청 시간이 5초라는 뜻이 아니다. DNS, connect, retry, DB query가 각각 독립 5초를 가지면 caller가 기대한 latency budget을 훨씬 넘길 수 있다. 안정적인 시스템은 상대 timeout과 절대 deadline을 구분하고 **남은 시간 budget을 하위 operation으로 전달**한다.

---

## CHAPTER 01 · timeout은 한 operation의 최대 대기이고 deadline은 전체 작업의 종료 시점이다

### 시작 전 용어집

#### 1. timeout

- **뜻:** Timeout은 보통 “지금부터 최대 N초”라는 상대 시간이다.
- **왜 중요한가:** Deadline은 “이 시각 이후에는 결과가 더 이상 유효하지 않다”는 전체 작업 경계다.
- **예시:** request deadline = T / remaining = T …

#### 2. operation

- **뜻:** 하위 작업은 자신이 원하는 기본 timeout과 remaining 중 더 작은 값을 사용해야 전체 budget을 지킬 수 있다.
- **왜 중요한가:** 모든 layer가 자기 timeout을 새로 시작하면 nesting 깊이에 따라 worst-case latency가 커진다.
- **예시:** request deadline = T / remaining = T …

#### 3. deadline

- **뜻:** Request scope에는 하나의 deadline을 두는 편이 reasoning이 쉽다.
- **예시:** request deadline = T / remaining = T …

```text
request deadline = T
remaining = T - now
```


 

---

## CHAPTER 02 · elapsed-time 계산에는 wall clock보다 monotonic clock이 적합하다

### 시작 전 용어집

#### 1. elapsed-time

- **뜻:** System wall clock은 NTP 보정이나 관리자 변경으로 앞으로/뒤로 움직일 수 있다.
- **왜 중요한가:** Duration과 timeout 계산에는 monotonic clock을 사용해야 한다.
- **예시:** start = time.monotonic() / # work

#### 2. wall clock

- **뜻:** 반대로 사용자에게 “2026-09-18 20:00에 만료”처럼 calendar timestamp를 보여줄 때는 wall-clock datetime이 필요하다.
- **왜 중요한가:** Duration source와 human time representation을 분리한다.
- **예시:** start = time.monotonic() / # work

#### 3. monotonic clock

- **뜻:** Deadline object가 내부적으로 monotonic target을 보유하면 clock adjustment가 operation timeout을 늘리거나 줄이는 문제를 피할 수 있다.
- **예시:** start = time.monotonic() / # work

```python
start = time.monotonic()
# work
elapsed = time.monotonic() - start
```

 


---

## CHAPTER 03 · budget propagation은 하위 호출이 상위 SLA를 초과하지 않게 한다

### 시작 전 용어집

#### 1. budget propagation

- **뜻:** 상위 request에 2초가 남았는데 DB client에 기본 10초 timeout을 전달하면 하위 operation이 caller가 이미 포기한 뒤까지 resource를 점유할 수 있다.
- **왜 중요한가:** Network call뿐 아니라 retry backoff, queue wait, lock acquisition에도 같은 budget을 적용한다.
- **예시:** remaining = deadline.remaining() / await db.query(sql, timeout=min(remaining, DB_MAX_TIMEOUT))

#### 2. SLA

- **뜻:** Timeout이 operation 실행 시간만 제한하고 queue에서 기다린 시간은 제외한다면 실제 latency가 예상보다 길어질 수 있다.
- **왜 중요한가:** Budget은 처리 pipeline 전체에서 소비되는 자원이다.
- **예시:** remaining = deadline.remaining() / await db.query(sql, timeout=min(remaining, DB_MAX_TIMEOUT))

```python
remaining = deadline.remaining()
await db.query(sql, timeout=min(remaining, DB_MAX_TIMEOUT))
```

 


---

## CHAPTER 04 · nested timeout이 겹치면 어떤 layer가 실패를 발생시켰는지 구분해야 한다

### 시작 전 용어집

#### 1. nested timeout

- **뜻:** Outer request timeout 3초, inner HTTP read timeout 1초가 동시에 존재할 수 있다.
- **왜 중요한가:** Inner가 먼저 실패하면 retry 가능한 read timeout일 수 있고 outer deadline이 끝났다면 더 이상 retry할 budget이 없다.
- **예시:** Outer request timeout 3초, inner HTTP read timeout …

#### 2. layer

- **뜻:** Exception type/message만 보고 판단하기보다 deadline state와 operation stage를 함께 기록한다.
- **왜 중요한가:** Timeout wrapper를 여러 겹 쌓을 때 cleanup과 cancellation propagation이 어떻게 동작하는지도 검증한다.
- **예시:** Exception type/message만 보고 판단하기보다 deadline state와 operation stage를 …

#### 3. 실패

- **뜻:** Inner timeout을 catch한 뒤 outer cancellation까지 삼키지 않는다.
- **예시:** Inner timeout을 catch한 뒤 outer cancellation까지 삼키지 않는다.

---

## CHAPTER 05 · async timeout은 underlying work의 cancellation semantics와 연결된다

### 시작 전 용어집

#### 1. async timeout

- **뜻:** Async timeout이 발생하면 await 중인 task에 cancellation을 전달하는 방식으로 구현될 수 있다.
- **왜 중요한가:** 하지만 thread executor나 외부 server operation이 즉시 중단된다는 보장은 없다.
- **예시:** Async timeout이 발생하면 await 중인 task에 cancellation을 전달하는 …

#### 2. underlying work

- **뜻:** 따라서 timeout 뒤에도 worker가 side effect를 완료할 수 있는지 확인한다.
- **왜 중요한가:** Payment 같은 operation은 request ID/idempotency key로 늦게 도착한 완료와 retry 중복을 처리해야 한다.
- **예시:** 따라서 timeout 뒤에도 worker가 side effect를 완료할 수 …

#### 3. cancellation

- **뜻:** “Caller가 기다리기를 포기했다”와 “작업이 실제로 중단됐다”는 다른 상태다.
- **예시:** “Caller가 기다리기를 포기했다”와 “작업이 실제로 중단됐다”는 다른 상태다.

---

## CHAPTER 06 · connect·read·write timeout은 서로 다른 failure stage를 의미한다

### 시작 전 용어집

#### 1. connect

- **뜻:** HTTP client의 하나의 `timeout=5`가 내부적으로 connect, pool wait, write, read에 어떻게 배분되는지는 library마다 다를 수 있다.
- **왜 중요한가:** Connection establishment가 느린 것과 response body가 느린 것은 운영 원인도 다르다.
- **예시:** 예를 들어 connect에 전체 budget을 다 써버리면 response를 …

#### 2. read

- **뜻:** Stage별 timeout을 설정할 수 있다면 전체 deadline 안에서 합리적 upper bound를 둔다.
- **왜 중요한가:** 예를 들어 connect에 전체 budget을 다 써버리면 response를 읽을 시간이 없다.
- **예시:** Stage별 timeout을 설정할 수 있다면 전체 deadline 안에서 …

#### 3. write timeout

- **뜻:** 로그와 metric에서도 `timeout` 하나가 아니라 stage를 구분하면 network, server, pool saturation을 더 빨리 좁힐 수 있다.
- **예시:** 로그와 metric에서도 `timeout` 하나가 아니라 stage를 구분하면 network, …

---

## CHAPTER 07 · timeout observability는 설정값보다 실제 남은 budget과 소비 시간을 기록한다

### 시작 전 용어집

#### 1. timeout

- **뜻:** 장애 분석에서 “timeout=2초였다”만 알면 부족하다.
- **왜 중요한가:** Operation 시작 시 remaining budget, queue wait, attempt count, 실제 elapsed를 기록하면 어디서 시간이 소비됐는지 알 수 있다.
- **예시:** 장애 분석에서 “timeout=2초였다”만 알면 부족하다.

#### 2. observability

- **뜻:** Trace span에 deadline remaining을 남기면 downstream이 이미 50ms밖에 남지 않은 request를 받았다는 사실도 보인다.
- **왜 중요한가:** 이런 request는 expensive work를 시작하기보다 빠르게 실패시키는 편이 전체 resource에 유리할 수 있다.
- **예시:** Trace span에 deadline remaining을 남기면 downstream이 이미 50ms밖에 …

#### 3. budget

- **뜻:** Timeout metric은 정상 latency percentile과 함께 본다.
- **왜 중요한가:** 임계값 바로 아래에서 지속적으로 느린 요청도 문제다.
- **예시:** Timeout metric은 정상 latency percentile과 함께 본다.

---

## CHAPTER 08 · deadline contract는 전체 latency budget의 소유자를 명확히 한다

### 시작 전 용어집

#### 1. deadline

- **뜻:** Request lifecycle에서 누가 deadline을 만들고, 어떤 하위 operation에 전달하며, retry/backoff가 얼마를 소비할 수 있는지 정한다.
- **왜 중요한가:** Deadline이 끝난 뒤 새 side effect를 시작하지 않는 rule도 중요하다.
- **예시:** Request lifecycle에서 누가 deadline을 만들고, 어떤 하위 operation에 …

#### 2. latency budget

- **뜻:** 테스트에서는 monotonic clock을 제어해 queue wait, nested timeout, retry 직전 budget 소진, cancellation 후 cleanup을 검증한다.
- **왜 중요한가:** 실제 `sleep()`에 의존하는 느린 테스트보다 fake clock/dependency injection이 더 결정적이다.
- **예시:** 테스트에서는 monotonic clock을 제어해 queue wait, nested timeout, …

#### 3. retry

- **뜻:** 이 PART의 핵심은 **timeout을 함수마다 붙이는 숫자로 보지 않고, request 전체가 공유하고 소비하는 유한한 deadline budget으로 관리하는 것**이다.
- **예시:** 이 PART의 핵심은 **timeout을 함수마다 붙이는 숫자로 보지 …
