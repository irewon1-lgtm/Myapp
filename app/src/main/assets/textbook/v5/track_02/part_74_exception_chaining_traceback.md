# PART 74 · Exception chaining과 traceback — 원인 보존·경계 번역·재발생을 구분하기

Exception handling의 목적은 오류를 숨기는 것이 아니라 실패의 의미를 더 정확히 전달하는 것이다. Python은 현재 exception, 이전 context, explicit cause, traceback을 서로 연결할 수 있다. 이 구조를 이해하면 low-level I/O error를 domain error로 번역하면서도 원인을 잃지 않고, 반대로 불필요한 내부 구현 detail을 사용자에게 노출하지 않을 수 있다.

---

## CHAPTER 01 · exception context는 처리 중 새 exception이 발생했음을 연결한다

### 시작 전 용어집

#### 1. exception

- **뜻:** `except` block 안에서 다른 exception이 발생하면 Python은 이전 exception을 context로 연결할 수 있다.
- **왜 중요한가:** 여기서는 `ValueError`를 처리하는 과정에서 `ConfigError`가 발생했고 traceback은 두 실패의 관계를 보여줄 수 있다.
- **예시:** try: / value = int(raw)

#### 2. value

- **뜻:** 이 연결은 “새 오류가 이전 오류와 무관하다”가 아니라 처리 중 발생했다는 사실을 보존한다.
- **왜 중요한가:** Error boundary를 설계할 때 context를 남길지, explicit cause로 더 명확히 표현할지 결정한다.
- **예시:** try: / value = int(raw)

#### 3. traceback

- **뜻:** 단순히 message를 복사하고 원본 exception을 버리면 debugging 정보가 줄어든다.
- **예시:** try: / value = int(raw)

```python
try:
    value = int(raw)
except ValueError:
    raise ConfigError("invalid integer")
```

 

 

---

## CHAPTER 02 · `raise ... from ...`은 명시적 cause를 만들어 번역 관계를 표현한다

### 시작 전 용어집

#### 1. raise

- **뜻:** Low-level exception을 higher-level domain exception으로 바꿀 때 `raise NewError(.
- **왜 중요한가:** ) from exc`를 사용하면 직접적인 원인 관계를 표시할 수 있다.
- **예시:** try: / record = json.loads(text)

#### 2. from

- **뜻:** Caller는 `ConfigError`라는 안정된 API를 받고, 운영자와 debugger는 underlying parse error를 추적할 수 있다.
- **왜 중요한가:** Cause는 무조건 추가하는 것이 아니라 abstraction boundary에서 의미가 바뀔 때 특히 유용하다.
- **예시:** try: / record = json.loads(text)

#### 3. cause

- **뜻:** 같은 exception을 그대로 다시 올릴 수 있다면 불필요하게 wrapper를 늘리지 않는다.
- **예시:** try: / record = json.loads(text)

..

```python
try:
    record = json.loads(text)
except ValueError as exc:
    raise ConfigError("configuration is not valid JSON") from exc
```


 

---

## CHAPTER 03 · `from None`은 context 표시를 억제하지만 원인 자체를 없애는 설계 도구는 아니다

### 시작 전 용어집

#### 1. from None

- **뜻:** from None`으로 context 표시를 억제할 수 있다.
- **왜 중요한가:** 예를 들어 mapping lookup의 `KeyError`를 public attribute API의 `AttributeError`로 바꾸는 경우가 있다.
- **예시:** try: / return data[name]

#### 2. context

- **뜻:** 사용자에게 low-level detail을 보여줄 필요가 없을 때 `raise .
- **왜 중요한가:** 하지만 모든 내부 오류를 `from None`으로 가리면 운영 디버깅이 어려워진다.
- **예시:** try: / return data[name]

#### 3. 오류

- **뜻:** Public message를 단순하게 하는 문제와 내부 telemetry에서 root cause를 보존하는 문제를 분리한다.
- **왜 중요한가:** Suppress는 표현 정책이지 failure 원인을 무시해도 된다는 뜻이 아니다.
- **예시:** try: / return data[name]

..  

```python
try:
    return data[name]
except KeyError:
    raise AttributeError(name) from None
```

 


---

## CHAPTER 04 · traceback object는 실패 당시 frame chain과 실행 위치를 보존한다

### 시작 전 용어집

#### 1. traceback

- **뜻:** Exception의 traceback은 어떤 call path를 따라 실패 지점에 도달했는지 보여준다.
- **왜 중요한가:** 각 frame은 local state와 code object에 연결될 수 있으므로 강력한 디버깅 정보이면서 동시에 memory retention과 민감정보 노출 위험도 가진다.
- **예시:** Exception의 traceback은 어떤 call path를 따라 실패 지점에 …

#### 2. 실패

- **뜻:** Long-lived exception object를 cache하면 traceback이 큰 object graph를 붙잡을 수 있다.
- **왜 중요한가:** Error history를 저장할 때는 필요한 summary만 추출하고 전체 traceback object를 계속 보관해야 하는지 검토한다.
- **예시:** Long-lived exception object를 cache하면 traceback이 큰 object graph를 …

#### 3. frame

- **뜻:** Traceback을 사용자에게 그대로 반환하면 file path, 내부 function 이름, 입력값이 노출될 수 있다.
- **왜 중요한가:** 운영 로그와 external response의 정보 수준을 분리한다.
- **예시:** Traceback을 사용자에게 그대로 반환하면 file path, 내부 function …

---

## CHAPTER 05 · bare `raise`는 현재 exception의 traceback을 보존하는 재발생 경로다

### 시작 전 용어집

#### 1. bare

- **뜻:** `except` 안에서 같은 exception을 다시 올릴 때는 일반적으로 bare `raise`가 현재 traceback 관계를 보존하는 자연스러운 방법이다.
- **왜 중요한가:** 새로 `raise exc`를 쓰는 것과 traceback 표현이 동일하다고 단정하지 않는다.
- **예시:** try: / work()

#### 2. raise

- **뜻:** Exception을 관찰만 하고 그대로 전달하려면 bare `raise`를 우선한다.
- **왜 중요한가:** Logging 후 re-raise 패턴에서는 상위 계층도 같은 exception을 logging해 duplicate log가 생길 수 있다.
- **예시:** try: / work()

#### 3. exception

- **뜻:** 어느 boundary가 최종 log 책임을 가지는지 정한다.
- **예시:** try: / work()

```python
try:
    work()
except TemporaryError:
    metrics.increment("temporary_error")
    raise
```

 

 

---

## CHAPTER 06 · exception note는 원본 타입을 바꾸지 않고 추가 context를 붙이는 수단이 될 수 있다

### 시작 전 용어집

#### 1. exception

- **뜻:** 이미 의미 있는 exception type이 있는데 운영 정보만 조금 더 붙이고 싶다면 원본 exception을 다른 wrapper로 바꾸지 않고 note를 추가하는 방식이 유용할 수 있다.
- **왜 중요한가:** 예를 들어 batch 처리 중 어느 record에서 실패했는지, 어떤 configuration key를 처리 중이었는지를 note로 추가하면 original error semantics를 보존하면서 진단성을 높일 수 있다.
- **예시:** 이미 의미 있는 exception type이 있는데 운영 정보만 …

#### 2. 타입

- **뜻:** 다만 note에 secret이나 전체 payload를 넣지 않는다.
- **왜 중요한가:** Error message와 마찬가지로 로그·traceback을 통해 외부로 이동할 수 있다.
- **예시:** 다만 note에 secret이나 전체 payload를 넣지 않는다.

#### 3. context

- **뜻:** Note는 structured telemetry를 대체하지 않는다.
- **왜 중요한가:** 검색·집계가 필요한 field는 logging/trace system에 별도로 기록한다.
- **예시:** Note는 structured telemetry를 대체하지 않는다.

---

## CHAPTER 07 · boundary translation은 내부 exception vocabulary를 public domain vocabulary로 바꾼다

### 시작 전 용어집

#### 1. boundary translation

- **뜻:** Repository layer의 `OSError`, parser의 `ValueError`, client library의 transport exception을 application service가 그대로 외부로 노출하면 caller가 내부 구현에 결합된다.
- **왜 중요한가:** 이 번역은 원인을 지우는 작업이 아니라 stable abstraction을 만드는 작업이다.
- **예시:** try: / row = repository.load(user_id)

#### 2. exception

- **뜻:** Retry 가능한 오류와 영구 오류를 분리하고, public exception에 caller가 실제로 취할 action을 표현한다.
- **왜 중요한가:** 너무 넓은 `except Exception`으로 모든 실패를 한 타입으로 바꾸면 programming bug까지 domain failure로 위장될 수 있다.
- **예시:** try: / row = repository.load(user_id)

```python
try:
    row = repository.load(user_id)
except StorageTimeout as exc:
    raise UserLookupUnavailable(user_id) from exc
```

 

 번역 범위를 좁힌다.

---

## CHAPTER 08 · traceback contract는 사용자 메시지와 운영 진단을 분리해 설계한다

### 시작 전 용어집

#### 1. traceback

- **뜻:** 좋은 exception API는 caller에게 안정된 type과 message를 제공하면서 운영자에게는 root cause와 trace를 보존한다.
- **왜 중요한가:** 두 독자를 같은 문자열 하나로 만족시키려 하지 않는다.
- **예시:** 좋은 exception API는 caller에게 안정된 type과 message를 제공하면서 …

#### 2. exception

- **뜻:** Exception object를 장기 저장할 때 memory retention이 없는지도 점검한다.
- **왜 중요한가:** 이 PART의 핵심은 **오류를 catch하는 행위보다 실패 관계를 얼마나 정확히 보존하고 어느 abstraction boundary에서 어떤 vocabulary로 번역할지를 설계하는 것**이다.
- **예시:** Exception object를 장기 저장할 때 memory retention이 없는지도 …

#### 3. 오류

- **뜻:** 테스트에서는 cause 연결, context suppression, bare re-raise, boundary translation을 각각 확인한다.
- **예시:** 테스트에서는 cause 연결, context suppression, bare re-raise, boundary …
