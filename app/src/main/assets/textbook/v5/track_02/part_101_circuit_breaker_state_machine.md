# PART 101 · Circuit breaker state machine — 실패를 계속 재시도하지 않고 dependency를 격리하기

외부 API나 database가 이미 심하게 느리거나 실패 중일 때 모든 요청이 끝까지 timeout과 retry를 반복하면 장애가 caller까지 증폭된다. Circuit breaker는 최근 실패를 관찰해 일정 조건에서 새 호출을 빠르게 차단하고, 회복 가능성을 제한된 probe로 확인하는 state machine이다. 핵심은 **실패를 숨기는 것이 아니라 실패한 dependency에 추가 부하를 보내지 않으면서 caller에게 빠른 실패를 제공하는 것**이다.

---

## CHAPTER 01 · breaker는 최근 failure window를 기반으로 dependency health를 추정한다

### 시작 전 용어집

#### 1. breaker

- **뜻:** Breaker가 보는 failure는 application 전체 오류가 아니라 특정 dependency operation의 health 신호여야 한다.
- **왜 중요한가:** Validation error처럼 dependency health와 무관한 실패를 failure rate에 섞지 않는다.
- **예시:** Breaker가 보는 failure는 application 전체 오류가 아니라 특정 …

#### 2. failure window

- **뜻:** 한 번의 timeout만으로 circuit을 열면 정상적인 transient error에도 과민하게 반응한다.
- **왜 중요한가:** 반대로 최근 1시간 전체 평균만 보면 지금 발생한 급격한 장애를 늦게 감지한다.
- **예시:** 한 번의 timeout만으로 circuit을 열면 정상적인 transient error에도 …

#### 3. dependency health

- **뜻:** Sliding count/time window 안에서 호출 수, 실패 수, timeout, latency를 계산해 판단할 수 있다.
- **왜 중요한가:** Sample 수가 너무 적을 때는 failure ratio가 불안정하므로 minimum request count를 둘 수 있다.
- **예시:** Sliding count/time window 안에서 호출 수, 실패 수, …

---

## CHAPTER 02 · CLOSED·OPEN·HALF_OPEN은 호출 허용 범위를 바꾸는 명시적 상태다

### 시작 전 용어집

#### 1. CLOSED

- **뜻:** CLOSED에서는 호출을 정상 허용하며 결과를 관찰한다.
- **왜 중요한가:** Threshold를 넘으면 OPEN으로 전환해 실제 dependency 호출 없이 빠르게 실패시킨다.
- **예시:** CLOSED --failure threshold--> OPEN / OPEN --cooldown--> HALF_OPEN

#### 2. OPEN

- **뜻:** 일정 recovery delay 뒤에는 HALF_OPEN으로 넘어가 제한된 probe를 허용한다.
- **왜 중요한가:** 이 상태 전이를 코드 곳곳의 boolean으로 흩뜨리지 않고 하나의 객체/모듈에서 관리하면 transition reasoning이 쉽다.
- **예시:** CLOSED --failure threshold--> OPEN / OPEN --cooldown--> HALF_OPEN

#### 3. 상태

- **뜻:** Dependency가 회복됐는지 확인할 controlled path가 필요하다.
- **예시:** CLOSED --failure threshold--> OPEN / OPEN --cooldown--> HALF_OPEN

```text
CLOSED --failure threshold--> OPEN
OPEN --cooldown--> HALF_OPEN
HALF_OPEN --success--> CLOSED
HALF_OPEN --failure--> OPEN
```


OPEN 상태도 영구 차단이 아니다. 

---

## CHAPTER 03 · trip threshold는 오류율뿐 아니라 최소 표본과 latency를 함께 고려할 수 있다

### 시작 전 용어집

#### 1. trip threshold

- **뜻:** `5회 실패하면 open` 같은 고정 count는 traffic volume에 따라 의미가 달라진다.
- **왜 중요한가:** 초당 수천 요청인 서비스와 하루 10회 호출하는 배치 job은 같은 threshold를 사용할 수 없다.
- **예시:** `5회 실패하면 open` 같은 고정 count는 traffic volume에 …

#### 2. 오류

- **뜻:** Failure ratio, consecutive failure, slow-call ratio를 조합할 수 있지만 조건을 너무 복잡하게 만들면 운영자가 왜 circuit이 열렸는지 이해하기 어렵다.
- **왜 중요한가:** Threshold 근거와 측정 window를 metric으로 노출한다.
- **예시:** Failure ratio, consecutive failure, slow-call ratio를 조합할 수 …

#### 3. latency

- **뜻:** Breaker tuning은 장애를 만들며 실험하지 말고 replay/load simulation에서 false-open과 late-open trade-off를 검증한다.
- **예시:** Breaker tuning은 장애를 만들며 실험하지 말고 replay/load simulation에서 …

---

## CHAPTER 04 · timeout·retry·breaker는 서로 다른 층이지만 함께 계산해야 한다

### 시작 전 용어집

#### 1. timeout

- **뜻:** Timeout은 한 attempt가 너무 오래 걸리지 않게 하고, retry는 일부 transient failure를 복구하며, breaker는 dependency가 지속적으로 실패할 때 새 attempt 자체를 제한한다.
- **왜 중요한가:** 순서를 잘못 잡으면 OPEN breaker 안에서 retry loop가 빠른 실패를 여러 번 반복해 의미 없는 attempt count를 소비할 수 있다.
- **예시:** Timeout은 한 attempt가 너무 오래 걸리지 않게 하고, …

#### 2. retry

- **뜻:** 보통 breaker rejection은 즉시 retry할 대상이 아니다.
- **왜 중요한가:** P97의 deadline과 P98의 retry budget도 연결한다.
- **예시:** 보통 breaker rejection은 즉시 retry할 대상이 아니다.

#### 3. breaker

- **뜻:** Probe 호출 하나가 caller deadline을 넘지 않도록 remaining budget을 적용한다.
- **예시:** Probe 호출 하나가 caller deadline을 넘지 않도록 remaining …

---

## CHAPTER 05 · breaker는 dependency와 operation 단위로 적절히 격리한다

### 시작 전 용어집

#### 1. breaker

- **뜻:** 너무 큰 breaker key는 unrelated operation을 함께 차단하고, 너무 작은 key는 상태 수와 관리 비용이 커진다.
- **왜 중요한가:** Tenant별 failure가 서로 독립적이라면 global breaker 하나가 건강한 tenant까지 막을 수 있다.
- **예시:** 너무 큰 breaker key는 unrelated operation을 함께 차단하고, …

#### 2. dependency

- **뜻:** 한 외부 서비스의 `search` endpoint 장애가 `health`나 `write`까지 동일하게 막아야 하는지는 별도 판단이다.
- **왜 중요한가:** 반대로 endpoint마다 tenant마다 모두 분리하면 낮은 traffic으로 threshold가 의미 없어질 수 있다.
- **예시:** 한 외부 서비스의 `search` endpoint 장애가 `health`나 `write`까지 …

#### 3. operation

- **뜻:** 실제 shared bottleneck이 어디인지 기준으로 isolation grain을 정한다.
- **예시:** 실제 shared bottleneck이 어디인지 기준으로 isolation grain을 정한다.

---

## CHAPTER 06 · HALF_OPEN probe는 recovery를 확인하되 다시 overload를 만들지 않아야 한다

### 시작 전 용어집

#### 1. HALF_OPEN probe

- **뜻:** Cooldown이 끝났다고 모든 대기 요청을 한꺼번에 보내면 회복 중인 dependency에 새로운 thundering herd를 만든다.
- **왜 중요한가:** HALF_OPEN에서는 소수의 probe만 허용하고 나머지는 계속 reject하거나 fallback한다.
- **예시:** Cooldown이 끝났다고 모든 대기 요청을 한꺼번에 보내면 회복 …

#### 2. recovery

- **뜻:** Recovery 단계에서 admission을 점진적으로 늘리는 방식은 rate limiter와 함께 설계할 수 있다.
- **왜 중요한가:** Probe 성공 기준도 단일 200 response보다 latency와 여러 연속 성공을 볼 수 있다.
- **예시:** Recovery 단계에서 admission을 점진적으로 늘리는 방식은 rate limiter와 …

#### 3. overload

- **뜻:** 반대로 너무 많은 성공을 요구하면 정상 traffic 복귀가 늦어진다.
- **예시:** 반대로 너무 많은 성공을 요구하면 정상 traffic 복귀가 …

---

## CHAPTER 07 · breaker observability는 현재 상태와 transition 원인을 보여줘야 한다

### 시작 전 용어집

#### 1. breaker observability

- **뜻:** 필수 metric에는 current state, open count, rejection count, probe result, observed failure ratio가 있다.
- **왜 중요한가:** 상태 전환 log에는 dependency key와 threshold 근거를 남긴다.
- **예시:** 필수 metric에는 current state, open count, rejection count, …

#### 2. 상태

- **뜻:** Caller error에는 “dependency unavailable” 같은 안정된 의미를 제공하되 내부 circuit 구현 detail을 public API에 과도하게 노출하지 않는다.
- **왜 중요한가:** Circuit이 계속 열리고 닫히는 flapping은 threshold가 불안정하거나 dependency가 부분 회복 중이라는 신호다.
- **예시:** Caller error에는 “dependency unavailable” 같은 안정된 의미를 제공하되 …

#### 3. transition

- **뜻:** Transition frequency 자체를 alert 대상으로 볼 수 있다.
- **예시:** Transition frequency 자체를 alert 대상으로 볼 수 있다.

---

## CHAPTER 08 · breaker contract는 실패 격리와 회복 탐지를 동시에 보장해야 한다

### 시작 전 용어집

#### 1. breaker contract

- **뜻:** Circuit breaker는 timeout/retry를 대체하지 않는다.
- **왜 중요한가:** 각 attempt의 시간 제한, transient 복구, 지속 장애 격리라는 서로 다른 책임을 조합한다.
- **예시:** Circuit breaker는 timeout/retry를 대체하지 않는다.

#### 2. 실패

- **뜻:** 테스트에서는 최소 표본 미달, 연속 failure, OPEN fast-fail, cooldown, HALF_OPEN probe concurrency, 회복 성공, 다시 실패하는 경우를 deterministic clock으로 검증한다.
- **왜 중요한가:** 이 PART의 핵심은 **circuit breaker를 오류를 막는 스위치로 보지 않고, 최근 실패를 근거로 dependency 호출 admission을 동적으로 제어하고 제한된 probe로 회복을 확인하는 상태 기계로 설계하는 것**이다.
- **예시:** 테스트에서는 최소 표본 미달, 연속 failure, OPEN fast-fail, …
