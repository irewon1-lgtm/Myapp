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

**직접 확인하기 — CHAPTER 01 · exception context는 처리 중 새 exception이 발생했음을 연결한다**
CHAPTER 01 · exception context는 처리 중 새 exception이 발생했음을 연결한다은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 규칙을 확인해야 오래 남는다. 먼저 입력이나 객체 하나만 두고 기대 결과를 적은 뒤 실행한다. 다음에는 값 하나, 호출 순서 하나, 경계 조건 하나만 바꿔 실제 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 01 · exception context는 처리 중 새 exception이 발생했음을 연결한다의 규칙이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 좁히고, 수정 뒤 원래 예제와 반대 조건 예제를 모두 다시 실행한다. 마지막에는 왜 그런 결과가 나왔는지 자기 문장으로 설명해 본다.
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

**직접 확인하기 — CHAPTER 02 · `raise ... from ...`은 명시적 cause를 만들어 번역 관계를 표현한다**
CHAPTER 02 · `raise ... from ...`은 명시적 cause를 만들어 번역 관계를 표현한다은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 규칙을 확인해야 오래 남는다. 먼저 입력이나 객체 하나만 두고 기대 결과를 적은 뒤 실행한다. 다음에는 값 하나, 호출 순서 하나, 경계 조건 하나만 바꿔 실제 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 02 · `raise ... from ...`은 명시적 cause를 만들어 번역 관계를 표현한다의 규칙이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 좁히고, 수정 뒤 원래 예제와 반대 조건 예제를 모두 다시 실행한다. 마지막에는 왜 그런 결과가 나왔는지 자기 문장으로 설명해 본다.
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

**직접 확인하기 — CHAPTER 03 · `from None`은 context 표시를 억제하지만 원인 자체를 없애는 설계 도구는 아니다**
CHAPTER 03 · `from None`은 context 표시를 억제하지만 원인 자체를 없애는 설계 도구는 아니다은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 규칙을 확인해야 오래 남는다. 먼저 입력이나 객체 하나만 두고 기대 결과를 적은 뒤 실행한다. 다음에는 값 하나, 호출 순서 하나, 경계 조건 하나만 바꿔 실제 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 03 · `from None`은 context 표시를 억제하지만 원인 자체를 없애는 설계 도구는 아니다의 규칙이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 좁히고, 수정 뒤 원래 예제와 반대 조건 예제를 모두 다시 실행한다. 마지막에는 왜 그런 결과가 나왔는지 자기 문장으로 설명해 본다.
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

**직접 확인하기 — CHAPTER 05 · bare `raise`는 현재 exception의 traceback을 보존하는 재발생 경로다**
CHAPTER 05 · bare `raise`는 현재 exception의 traceback을 보존하는 재발생 경로다은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 규칙을 확인해야 오래 남는다. 먼저 입력이나 객체 하나만 두고 기대 결과를 적은 뒤 실행한다. 다음에는 값 하나, 호출 순서 하나, 경계 조건 하나만 바꿔 실제 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 05 · bare `raise`는 현재 exception의 traceback을 보존하는 재발생 경로다의 규칙이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 좁히고, 수정 뒤 원래 예제와 반대 조건 예제를 모두 다시 실행한다. 마지막에는 왜 그런 결과가 나왔는지 자기 문장으로 설명해 본다.
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

**직접 확인하기 — CHAPTER 07 · boundary translation은 내부 exception vocabulary를 public domain vocabulary로 바꾼다**
CHAPTER 07 · boundary translation은 내부 exception vocabulary를 public domain vocabulary로 바꾼다은 설명만 읽고 넘기기보다 가장 작은 실행 예제로 규칙을 확인해야 오래 남는다. 먼저 입력이나 객체 하나만 두고 기대 결과를 적은 뒤 실행한다. 다음에는 값 하나, 호출 순서 하나, 경계 조건 하나만 바꿔 실제 결과가 어떻게 달라지는지 비교한다. 한 줄 해석은 “CHAPTER 07 · boundary translation은 내부 exception vocabulary를 public domain vocabulary로 바꾼다의 규칙이 값의 의미와 프로그램 상태 변화에 어떤 제약을 주는지 확인한다”이다. 예상과 다르면 타입·정체성·호출 순서·예외 경계를 차례로 좁히고, 수정 뒤 원래 예제와 반대 조건 예제를 모두 다시 실행한다. 마지막에는 왜 그런 결과가 나왔는지 자기 문장으로 설명해 본다.
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

---

## 실전 학습 루프 · exception chaining과 traceback

### 1. 쉬운 예

낮은 계층의 JSON decode 오류를 상위에서 `ConfigError`로 바꾸더라도 원래 원인을 잃으면 진단이 어려워진다. exception chaining은 사용자에게는 도메인 의미를 주면서 개발자에게는 원인 경로를 보존한다.

### 2. 한 줄 해석

좋은 예외 변환은 오류의 의미를 높이되 원래 cause와 traceback evidence를 버리지 않는다.

### 3. 직접 실행

아래 코드는 개념을 작게 격리한 예다. 실행 전에 출력이나 상태 변화를 먼저 예상한 뒤 실제 결과와 비교한다.

```python
class ConfigError(Exception): pass

def load_config(text):
    try:
        import json
        return json.loads(text)
    except ValueError as exc:
        raise ConfigError('설정 형식 오류') from exc

load_config('{bad')
```

결과가 예상과 다르면 문법부터 고치지 말고, **어떤 protocol·상태·계약이 호출됐는지**를 한 단계씩 확인한다. 이렇게 해야 “우연히 동작하는 코드”와 “이유를 설명할 수 있는 코드”를 구분할 수 있다.

### 4. 수정 실습

1. `from None`을 사용해 context 표시가 어떻게 달라지는지 확인한다.
2. catch-all로 예외를 빈 dict로 바꿨을 때 어떤 증거가 사라지는지 비교한다.

수정 후에는 정상 입력 하나만 보지 말고 빈 값, 경계값, 반복 호출, 예외 경로 중 해당되는 반례를 최소 하나 추가한다.

### 5. 확인 문제

상위 계층의 새 예외만 기록하고 원래 예외는 버리는 것이 더 깔끔할까?

### 6. 정답과 오답 설명

**정답:** 대개 아니다. 복구·사용자 메시지용 의미와 root-cause 진단용 cause는 함께 보존하는 편이 낫다.

**자주 나오는 오답:** stack trace가 길다는 이유만으로 원인을 삭제하면 운영 장애에서 가장 중요한 증거를 없앨 수 있다.

마지막으로 코드를 다시 읽으면서 **입력 → 호출되는 규칙 → 상태 변화 → 결과/예외** 네 칸으로 요약한다. 이 네 칸을 설명할 수 있으면 단순 암기가 아니라 실행 모델을 이해한 것이다.

## 현장 디버깅 체크 · exception chaining·traceback

### 증상에서 시작한다

사용자에게는 ConfigError 하나만 보이는데 로그만으로는 원래 JSON/IO 오류 위치를 찾을 수 없다. 이때 문법을 먼저 고치면 원인이 가려질 수 있다. 재현 입력과 실제 상태를 보존한 뒤 **어느 경계에서 처음 기대와 달라졌는지**를 찾는다.

### 먼저 볼 증거

`__cause__`, `__context__`, traceback의 application frame과 변환된 예외 타입을 함께 본다. 최종 출력 하나만 보지 말고 호출 전 값, 호출 뒤 값, 예외 또는 resource 상태를 나란히 두면 원인 후보가 급격히 줄어든다.

### 일부러 실패시켜 보기

낮은 계층 예외를 `raise NewError(...) from exc`와 원인 없이 다시 던지는 두 버전으로 비교한다. 정상 예제만 통과시키는 것은 검증이 아니다. 경계 조건을 강제로 만들고 같은 증상이 반복되는지 확인해야 수정 전후를 비교할 수 있다.

### 통과 기준

상위 오류 의미는 유지하면서 root cause와 호출 경로를 재현할 증거가 사라지지 않아야 한다. 이 기준을 테스트 이름과 assertion으로 옮기면 이후 refactoring에서도 같은 오류가 돌아오는지 자동으로 잡을 수 있다.

