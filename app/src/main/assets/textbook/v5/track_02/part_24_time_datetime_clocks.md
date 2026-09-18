# PART 24 · Time·datetime·clock — 시각, 기간, 시간대를 서로 다른 값으로 다루기

시간 관련 버그는 문법보다 개념을 섞을 때 발생한다. “지금”이라는 시각, 두 사건 사이의 기간, 벽시계가 보여 주는 local time, timeout을 재는 monotonic clock은 서로 다른 의미를 가진다. 날짜 문자열 하나를 `datetime`으로 바꾸는 것보다 중요한 것은 **어떤 시간축의 어떤 순간을 표현하고 어떤 연산을 허용할지**를 고정하는 것이다.

---

## CHAPTER 01 · instant와 local date-time은 같은 객체처럼 보여도 의미가 다르다

### 시작 전 용어집

#### 1. instant

- **뜻:** 전 세계 시간축의 한 순간을 나타내는 instant와 “서울에서 2026-09-17 09:00” 같은 local date-time은 다르다.
- **왜 중요한가:** Local clock 표기만으로는 timezone이 없으면 정확한 instant를 결정할 수 없다.
- **예시:** 전 세계 시간축의 한 순간을 나타내는 instant와 “서울에서 …

#### 2. local date-time

- **뜻:** 반대로 instant를 사용자에게 표시하려면 timezone 규칙을 적용해 local representation으로 변환해야 한다.
- **왜 중요한가:** 데이터베이스에 appointment를 저장할 때 “매주 월요일 오전 9시 서울”처럼 지역 일정 규칙이 중요한 값과 “결제가 발생한 절대 시각”처럼 instant가 중요한 값을 구분한다.
- **예시:** 반대로 instant를 사용자에게 표시하려면 timezone 규칙을 적용해 local …

#### 3. 객체

- **뜻:** 모두 UTC timestamp 하나로 저장하면 반복 일정의 local 의미가 깨질 수 있고, 모두 local string으로 저장하면 사건 순서를 전 세계에서 비교하기 어렵다.
- **왜 중요한가:** Naive datetime과 timezone-aware datetime을 섞으면 비교와 변환에서 오류가 생길 수 있다.
- **예시:** 모두 UTC timestamp 하나로 저장하면 반복 일정의 local …

#### 4. clock

- **뜻:** Application policy를 정해 내부 event timestamp는 aware UTC instant, 사용자 입력 일정은 zone ID를 함께 저장하는 식으로 모델링한다.
- **왜 중요한가:** 이 값은 세계 시간축의 한 점인가, 달력상의 local 약속인가, 단순 날짜인가, elapsed duration인가.
- **예시:** Application policy를 정해 내부 event timestamp는 aware UTC …

시간 타입을 선택할 때 먼저 질문한다.  타입이 이 질문에 답하게 만든다.

---

## CHAPTER 02 · timezone은 UTC offset 하나가 아니라 지역별 변화 규칙이다

### 시작 전 용어집

#### 1. timezone

- **뜻:** `+09:00` 같은 offset은 특정 순간의 UTC와 local time 차이를 말하지만 `Asia/Seoul` 같은 timezone ID는 역사와 미래의 offset 규칙을 나타낸다.
- **왜 중요한가:** Daylight saving을 사용하는 지역에서는 같은 zone의 offset이 계절에 따라 달라질 수 있다.
- **예시:** `+09:00` 같은 offset은 특정 순간의 UTC와 local time …

#### 2. UTC offset

- **뜻:** 따라서 미래 일정에는 현재 offset만 저장하면 규칙 변경이나 DST를 반영하기 어렵다.
- **왜 중요한가:** Zone identifier와 local schedule 의미를 함께 보존할 수 있다.
- **예시:** 따라서 미래 일정에는 현재 offset만 저장하면 규칙 변경이나 …

반면 과거 event timestamp는 이미 확정된 instant와 당시 display zone을 필요에 따라 기록한다.

Timezone database도 software data이므로 국가 정책 변경에 따라 업데이트될 수 있다. 먼 미래 일정이 많은 system에서는 tzdata version과 recalculation policy를 고려한다. “한 번 계산한 UTC가 영원히 현지 9시”라는 가정이 깨질 수 있다.

사용자 profile timezone과 device timezone도 다를 수 있다. 서버가 자신의 local timezone으로 데이터를 해석하지 않도록 boundary에서 zone을 명시한다.

---

## CHAPTER 03 · DST transition은 존재하지 않는 시각과 두 번 존재하는 시각을 만든다

### 시작 전 용어집

#### 1. DST transition

- **뜻:** Daylight saving time을 사용하는 지역에서는 clock을 앞으로 이동할 때 local time 일부가 존재하지 않을 수 있고, 뒤로 이동할 때 동일한 local clock time이 두 번 나타날 수 있다.
- **왜 중요한가:** `2026-11-01 01:30` 같은 표기만으로 어느 instant인지 모호할 수 있다.
- **예시:** 이 문제는 특정 나라만의 예외가 아니라 시간 모델에서 …

#### 2. clock

- **뜻:** Parser가 이런 ambiguous/nonexistent local time을 자동으로 한쪽으로 선택하면 사용자 의도와 다를 수 있다.
- **왜 중요한가:** Calendar application은 명시적으로 offset/zone rule을 적용하고 ambiguity를 사용자에게 물을 수 있다.
- **예시:** 이 문제는 특정 나라만의 예외가 아니라 시간 모델에서 …

#### 3. set

- **뜻:** Timezone-aware type과 library를 사용해 직접 offset rule을 구현하지 않는다.
- **왜 중요한가:** Batch system은 정책적으로 first/second occurrence를 선택할 수 있지만 문서화해야 한다.
- **예시:** 이 문제는 특정 나라만의 예외가 아니라 시간 모델에서 …

#### 4. 경계

- **뜻:** Duration 계산에서도 “내일 같은 시각”과 “24시간 후”는 DST 경계에서 다를 수 있다.
- **왜 중요한가:** Calendar arithmetic과 elapsed-time arithmetic을 구분한다.
- **예시:** 이 문제는 특정 나라만의 예외가 아니라 시간 모델에서 …

하루 뒤 local 9시는 실제 elapsed seconds가 23시간 또는 25시간일 수 있다.

이 문제는 특정 나라만의 예외가 아니라 시간 모델에서 local representation과 instant가 다르다는 증거다. 

---

## CHAPTER 04 · wall clock은 timeout 측정에 적합하지 않을 수 있다

### 시작 전 용어집

#### 1. wall clock

- **뜻:** System wall clock은 NTP synchronization, 관리자 변경, suspend/resume 때문에 앞으로 또는 뒤로 조정될 수 있다.
- **왜 중요한가:** 요청 시작 시각과 끝 시각을 wall-clock timestamp로 빼서 timeout을 판단하면 clock jump 때문에 음수 duration이나 과도한 duration이 나올 수 있다.
- **예시:** System wall clock은 NTP synchronization, 관리자 변경, suspend/resume …

#### 2. timeout

- **뜻:** Elapsed time과 deadline에는 monotonic clock을 사용한다.
- **왜 중요한가:** Monotonic clock은 시스템이 살아 있는 동안 한 방향으로 진행되는 성질을 제공해 duration 측정에 적합하다.
- **예시:** Elapsed time과 deadline에는 monotonic clock을 사용한다.

#### 3. 타입

- **뜻:** 같은 숫자 타입이라도 clock domain이 다르므로 섞지 않는다.
- **왜 중요한가:** Test에서는 실제 sleep으로 시간을 보내기보다 fake clock을 주입하면 deadline과 expiry 경계를 빠르고 deterministic하게 검증할 수 있다.
- **예시:** 같은 숫자 타입이라도 clock domain이 다르므로 섞지 않는다.

#### 4. 경계

- **뜻:** 반면 사람이 읽는 calendar timestamp로 변환할 수 있는 epoch 의미는 없을 수 있다.
- **왜 중요한가:** Log event에는 wall-clock instant를 남기고 performance duration은 monotonic timestamp 차이로 계산한다.
- **예시:** 반면 사람이 읽는 calendar timestamp로 변환할 수 있는 …

따라서 두 clock을 동시에 사용할 수 있다.  


---

## CHAPTER 05 · duration과 calendar period는 서로 다른 산술을 가진다

### 시작 전 용어집

#### 1. duration

- **뜻:** `timedelta` 같은 duration은 일정한 elapsed time을 표현하는 데 적합하다.
- **왜 중요한가:** 그러나 “한 달 후”는 30일이나 31일처럼 고정 seconds로 정의되지 않는다.
- **예시:** Business day 계산에는 주말뿐 아니라 국가별 holiday calendar와 …

#### 2. calendar period

- **뜻:** Calendar month, business day, end-of-month rule은 별도의 calendar arithmetic이다.
- **왜 중요한가:** 1월 31일에서 한 달 뒤를 2월 28일/29일로 clamp할지 3월로 넘길지 domain policy가 필요하다.
- **예시:** Business day 계산에는 주말뿐 아니라 국가별 holiday calendar와 …

#### 3. dependency

- **뜻:** 이런 calendar data도 version과 source를 가진 외부 dependency다.
- **왜 중요한가:** 시간 산술 함수 이름에 `add_days`, `next_billing_date`, `after_duration`처럼 의미를 드러내면 같은 `+` 연산으로 서로 다른 policy를 섞는 것을 줄일 수 있다.
- **예시:** Business day 계산에는 주말뿐 아니라 국가별 holiday calendar와 …

#### 4. 함수

- **뜻:** 정기 결제, 만기, 보고서 일정은 서로 다른 rule을 가질 수 있다.
- **왜 중요한가:** 범용 date library가 자동으로 업무 정답을 결정해 주지 않는다.
- **예시:** Business day 계산에는 주말뿐 아니라 국가별 holiday calendar와 …

Business day 계산에는 주말뿐 아니라 국가별 holiday calendar와 예외 영업일이 필요할 수 있다. 


---

## CHAPTER 06 · timestamp serialization은 precision과 zone 정보를 명시한다

### 시작 전 용어집

#### 1. timestamp serialization

- **뜻:** ISO 8601 계열 문자열은 human-readable interchange에 널리 쓰이지만 fractional second precision, `Z`와 offset 표기, zone ID 포함 여부를 정해야 한다.
- **왜 중요한가:** 두 system이 같은 instant를 서로 다른 textual form으로 나타낼 수 있으므로 raw string equality를 instant equality로 사용하지 않는다.
- **예시:** ISO 8601 계열 문자열은 human-readable interchange에 널리 쓰이지만 …

#### 2. precision

- **뜻:** Precision을 줄이면 ordering이 같아 보이는 여러 event가 동일 timestamp가 될 수 있다.
- **왜 중요한가:** Audit log가 nanosecond ordering을 실제로 보장할 수 있는지도 별도 문제다.
- **예시:** Precision을 줄이면 ordering이 같아 보이는 여러 event가 동일 …

#### 3. zone

- **뜻:** Wire format에서 timezone-aware instant를 받아 내부 canonical instant로 변환한 뒤 display 단계에서 zone을 적용하는 흐름이 일반적으로 안전하다.
- **왜 중요한가:** Unix timestamp도 seconds인지 milliseconds인지 unit이 없으면 1000배 오류가 생긴다.
- **예시:** Wire format에서 timezone-aware instant를 받아 내부 canonical instant로 …

#### 4. set

- **뜻:** API field 이름이나 schema에 unit을 명시하고 numeric range를 검증한다.
- **왜 중요한가:** 10자리와 13자리 길이를 추측해 자동 변환하는 코드는 미래 날짜나 malformed input에서 위험하다.
- **예시:** API field 이름이나 schema에 unit을 명시하고 numeric range를 …

Clock resolution과 storage precision, application ordering requirement를 구분한다.


---

## CHAPTER 07 · 날짜 범위는 inclusive/exclusive convention을 고정한다

### 시작 전 용어집

#### 1. inclusive

- **뜻:** API가 inclusive end를 요구한다면 내부 representation과 변환 boundary를 하나로 모아 `+epsilon` 같은 임의 보정을 여러 곳에서 하지 않는다.
- **왜 중요한가:** 보고서 기간이 “9월 1일부터 9월 30일까지”라는 자연어는 timestamp query에서 경계를 어떻게 표현할지 결정해야 한다.
- **예시:** `start <= t < next_month_start` 같은 반열린 구간은 …

#### 2. exclusive convention

- **뜻:** `start <= t < next_month_start` 같은 반열린 구간은 마지막 날의 `23:59:59.
- **왜 중요한가:** `를 인위적으로 만들 필요가 없어 precision 변화에 강하다.
- **예시:** `start <= t < next_month_start` 같은 반열린 구간은 …

#### 3. 경계

- **뜻:** Date-only field와 datetime field를 비교할 때도 사용자의 timezone에서 day boundary를 instant range로 변환한다.
- **왜 중요한가:** UTC 자정 기준으로 잘라 버리면 한국 사용자의 local day 일부가 이전/다음 날짜로 들어갈 수 있다.
- **예시:** Date-only field와 datetime field를 비교할 때도 사용자의 timezone에서 …

#### 4. precision

- **뜻:** Adjacent range를 `[a,b)`, `[b,c)`로 통일하면 중복과 누락 없이 partition하기 쉽다.
- **왜 중요한가:** Pagination과 time-window aggregation에서도 같은 convention을 사용할 수 있다.
- **예시:** Adjacent range를 `[a,b)`, `[b,c)`로 통일하면 중복과 누락 없이 …

999...

 

 


---

## CHAPTER 08 · scheduling은 “몇 초 후”와 “달력의 특정 시각”을 구분한다

### 시작 전 용어집

#### 1. scheduling

- **뜻:** Scheduling rule과 concurrency rule은 별개다.
- **왜 중요한가:** Timer는 현재부터 10분 후처럼 elapsed delay를 기준으로 실행할 수 있고 scheduler는 매일 local 09:00처럼 calendar rule을 기준으로 실행할 수 있다.
- **예시:** Scheduling rule과 concurrency rule은 별개다.

#### 2. queue

- **뜻:** Long-running job이 다음 schedule과 겹칠 때 skip, queue, parallel run 중 정책을 정한다.
- **왜 중요한가:** Machine sleep이나 restart 후 missed execution을 어떻게 처리할지도 policy다.
- **예시:** Long-running job이 다음 schedule과 겹칠 때 skip, queue, …

Recurring schedule을 다음 실행 instant 하나로만 계속 덧셈해 계산하면 DST와 calendar change 때문에 local 시각이 drift할 수 있다. 원래 recurrence rule과 timezone에서 다음 occurrence를 다시 계산하는 편이 의미를 보존한다.

Scheduler가 중복 실행될 수 있는 distributed environment에서는 job operation이 idempotent해야 할 수 있다. “정확히 한 번 실행”을 scheduler flag 하나로 보장하기보다 duplicate trigger와 retry를 견디는 job design이 중요하다.

 

---

## CHAPTER 09 · time-based cache는 expiration clock과 freshness 의미를 분리한다

### 시작 전 용어집

#### 1. time-based cache

- **뜻:** Cache entry에 TTL 60초를 적용할 때 elapsed duration을 재는 monotonic clock이 적합할 수 있다.
- **왜 중요한가:** 하지만 “데이터가 2026-09-17T12:00Z 기준으로 최신” 같은 freshness metadata는 wall-clock instant다.
- **예시:** Cache entry에 TTL 60초를 적용할 때 elapsed duration을 …

#### 2. expiration clock

- **뜻:** 둘을 같은 timestamp field로 사용하지 않는다.
- **왜 중요한가:** Sliding expiration은 access할 때마다 deadline을 연장하고 absolute expiration은 최초 기준으로 끝난다.
- **예시:** 둘을 같은 timestamp field로 사용하지 않는다.

#### 3. freshness

- **뜻:** User session, DNS cache, API response마다 적합한 policy가 다르다.
- **왜 중요한가:** Cache library default를 업무 의미로 착각하지 않는다.
- **예시:** User session, DNS cache, API response마다 적합한 policy가 …

#### 4. API

- **뜻:** Stale-while-revalidate처럼 expiry 이후 잠시 오래된 값을 제공하면서 background refresh하는 정책은 latency를 줄일 수 있지만 어떤 data에 stale 허용이 가능한지 판단해야 한다.
- **왜 중요한가:** Test에서는 fake clock을 전진시켜 expiry 직전·정확히 deadline·직후를 확인한다.
- **예시:** Stale-while-revalidate처럼 expiry 이후 잠시 오래된 값을 제공하면서 background …

권한과 잔액처럼 민감한 값에는 위험할 수 있다.

 Real sleep에 의존하면 suite가 느리고 flaky해진다.

---

## CHAPTER 10 · 시간 코드는 clock source와 timezone rule을 dependency로 다룬다

### 시작 전 용어집

#### 1. clock

- **뜻:** Operation 시작에 clock에서 한 번 instant를 받아 필요한 계산에 전달하거나 explicit clock dependency를 사용한다.
- **왜 중요한가:** Timezone도 global machine setting에 의존하지 않고 사용자/업무 context에서 전달한다.
- **예시:** Operation 시작에 clock에서 한 번 instant를 받아 필요한 …

#### 2. timezone rule

- **뜻:** 함수 안에서 아무 곳에서나 `now()`를 호출하면 같은 use case 내부에서도 서로 다른 시각을 읽을 수 있고 test에서 특정 경계를 만들기 어렵다.
- **왜 중요한가:** Calendar calculation function이 zone을 parameter로 받아 same input에서 deterministic하게 결과를 만들 수 있어야 한다.
- **예시:** 함수 안에서 아무 곳에서나 `now()`를 호출하면 같은 use …

#### 3. dependency

- **뜻:** Persistence에는 original local intent를 보존해야 하는 값과 canonical instant만 필요한 값을 구분한다.
- **왜 중요한가:** Scheduling, audit, timeout, TTL이 모두 “시간”이라는 이유로 하나의 helper에 모이지 않게 한다.
- **예시:** Persistence에는 original local intent를 보존해야 하는 값과 canonical …

#### 4. 함수

- **뜻:** 시간 프로그래밍의 핵심은 날짜 API를 많이 아는 것이 아니라 **instant·local time·timezone·duration·calendar period·monotonic clock을 다른 domain으로 모델링해 잘못된 산술을 구조적으로 줄이는 것**이다.
- **예시:** 시간 프로그래밍의 핵심은 날짜 API를 많이 아는 것이 …
