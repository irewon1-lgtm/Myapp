# TRACK 10 · 오류·테스트·Git·빌드·배포 — 변경을 안전하게 만드는 기술

# BLOCK 03 · 로그·stack trace·debugger로 실행을 관찰한다

재현된 실패를 실제 실행 안에서 추적하려면 관측 도구가 필요하다. 이 BLOCK은 구조화 로그, 심각도, correlation ID, stack trace, debugger, exception chaining, log/metric/trace의 역할을 연결한다.


```text
개념 설명 → 아주 쉬운 예 → 한 줄씩 해석 → 직접 실행 → 일부 수정 → 작은 문제 → 왜 맞고 틀렸는지 설명
```

---

## CHAPTER 01 · LESSON 01 · print보다 구조화된 로그로 사건을 남긴다

**LESSON ID:** `T10-B03-L01`

### 먼저 쉬운 말로 이해하기

`print("여기 왔다")`는 첫 실험에는 유용하지만 운영 디버깅에는 부족하다. 언제, 어떤 요청, 어느 단계, 어떤 결과였는지 field로 남겨야 여러 사건을 구분하고 검색할 수 있다.

### 안에서는 실제로 무엇이 일어나는가

구조화 로그는 사람이 읽는 문장 안에 모든 값을 섞지 않고 `event`, `request_id`, `duration_ms`, `status` 같은 key-value로 분리한다. 그러면 필터·집계·상관관계 분석이 쉬워지고, 로그 형식 변경에도 parser가 덜 깨진다.

### 아주 쉬운 예

Python 표준 logging에 `extra`를 쓰는 것보다 여기서는 JSON 한 줄을 직접 만들어 원리를 본다.

```python
import json, time

event = {
    "event": "order_loaded",
    "order_id": 42,
    "duration_ms": 18,
    "ok": True,
}
print(json.dumps(event, ensure_ascii=False))
```

### 한 줄씩 읽기

- 한 줄 JSON은 event 경계를 명확히 한다.
- `duration_ms`는 성능 가설을 검사할 수 있는 숫자다.
- 민감정보는 아무 field나 넣지 않고 schema를 정한다.

### 직접 실행

1. 실행해 JSON 한 줄이 나오는지 확인한다.
2. `ok`를 false로 바꾸고 검색하기 쉬운 차이를 본다.
3. `phone`이나 access token을 넣지 않는 로그 schema를 적는다.


### 일부를 바꿔서 다시 확인하기

`step` field를 추가해 `request_received`, `db_loaded`, `response_sent` 세 사건을 출력하도록 바꾼다. 같은 요청을 묶을 `request_id`도 넣는다.

### 작은 문제

로그 문장 “실패함”만 10만 줄 있다. 원인 조사에 왜 약한가?

### 왜 맞고 왜 틀리는가

어떤 요청·단계·버전·오류 종류인지 구조적 정보가 없어 사건을 묶거나 비교하기 어렵기 때문이다. 필요한 진단 field를 설계해야 한다.

### 자주 만나는 실패와 확인 순서

- 로그가 너무 많으면 중요한 신호가 묻히고 비용·성능 문제가 생긴다.
- secret/개인정보를 로그에 넣으면 장기간 복제될 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 로그는 사건과 진단에 필요한 field를 남기되 민감정보는 제외한다.
- **직접 코딩·실행해서 익힐 것:** 작은 실행에 request_id/step/duration을 넣어 흐름을 추적한다.
- **AI에게 맡겨도 되는 것:** logging boilerplate와 query 예시 생성.
- **사람이 최종 확인할 것:** 어떤 정보가 필요한 증거인지, privacy와 비용 경계를 결정한다.

### 근거 연결

`GOOGLE-SRE` · `NIST-SSDF-12`

---

## CHAPTER 02 · LESSON 02 · log level은 심각도와 운영 행동을 연결한다

**LESSON ID:** `T10-B03-L02`

### 먼저 쉬운 말로 이해하기

DEBUG, INFO, WARNING, ERROR 같은 level은 글씨 색깔이 아니라 **운영자가 어떻게 반응할지** 구분하는 신호다. 모든 것을 ERROR로 찍으면 진짜 장애가 묻히고, 중요한 실패를 DEBUG로만 남기면 production에서 보이지 않을 수 있다.

### 안에서는 실제로 무엇이 일어나는가

level 정책은 시스템마다 다르지만 핵심은 일관성이다. 정상적인 상태 전이는 INFO, 예상 가능한 일시적 문제는 WARNING, 사용자 요청 실패나 중요한 작업 실패는 ERROR처럼 팀 규칙을 정한다. 예외가 있어도 자동 복구됐다면 무조건 최고 심각도일 필요는 없다.

### 아주 쉬운 예

Python logging으로 level filter가 출력에 미치는 영향을 본다.

```python
import logging
logging.basicConfig(level=logging.INFO)

logging.debug("cache lookup detail")
logging.info("service started")
logging.warning("retrying request")
logging.error("payment failed")
```

### 한 줄씩 읽기

- root level이 INFO라 DEBUG는 기본 출력에서 제외된다.
- WARNING은 복구 가능 상황에서도 운영 관심이 필요한 신호가 될 수 있다.
- ERROR는 실패 사실을 남기지만 원인 단정 문구는 증거가 있어야 한다.

### 직접 실행

1. 실행해서 어떤 네 줄 중 무엇이 보이는지 확인한다.
2. level을 DEBUG로 바꾸고 출력 차이를 본다.
3. 같은 event를 중복으로 여러 level에 찍지 않도록 설계한다.


### 일부를 바꿔서 다시 확인하기

사용자 입력 validation 실패를 매번 ERROR로 남기는 경우와 시스템 내부 예외를 비교해 적절한 level을 결정해 본다. traffic이 큰 서비스에서는 잘못된 level이 alert 폭주로 이어질 수 있다.

### 작은 문제

ERROR 로그가 하나 있으니 시스템 전체 장애라고 말해도 되는가?

### 왜 맞고 왜 틀리는가

안 된다. 한 요청 실패일 수 있다. error rate, affected users, duration, dependency 상태 등 범위를 측정해야 severity를 판단할 수 있다.

### 자주 만나는 실패와 확인 순서

- 예외 stack trace를 매 retry마다 ERROR로 찍으면 한 장애가 여러 건처럼 보일 수 있다.
- DEBUG에 민감한 payload 전체를 남겨 production 설정 변경 시 노출되는 경우가 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** level은 메시지 장식이 아니라 신호의 의미와 대응 우선순위를 표현한다.
- **직접 코딩·실행해서 익힐 것:** logging level을 바꿔 실제 필터 동작을 본다.
- **AI에게 맡겨도 되는 것:** 초기 level 정책 제안.
- **사람이 최종 확인할 것:** 제품의 위험도·alert 기준에 맞게 level과 중복 기록 정책을 결정한다.

### 근거 연결

`GOOGLE-SRE` · `NIST-SSDF-12`

---

## CHAPTER 03 · LESSON 03 · request ID로 여러 로그를 한 사건으로 묶는다

**LESSON ID:** `T10-B03-L03`

### 먼저 쉬운 말로 이해하기

동시에 여러 사용자가 요청하면 로그 순서가 섞인다. `request_id`나 correlation ID를 모든 주요 단계에 전달하면 같은 요청의 로그만 골라 흐름을 재구성할 수 있다.

### 안에서는 실제로 무엇이 일어나는가

ID는 원인을 해결하지 않지만 **상관관계**를 만든다. gateway→API→worker처럼 여러 component를 지날 때 같은 trace context를 전달하면 어느 구간에서 시간이 늘거나 오류가 시작됐는지 좁힐 수 있다.

### 아주 쉬운 예

두 요청의 단계가 섞여도 ID로 분리되는 예를 만든다.

```python
events = [
    ("r1", "start"),
    ("r2", "start"),
    ("r2", "db_done"),
    ("r1", "db_done"),
]

for rid, step in events:
    print(f"request_id={rid} step={step}")
```

### 한 줄씩 읽기

- 출력 순서는 요청별로 연속되지 않는다.
- `request_id`가 있어 r1만 filter할 수 있다.
- 실제 시스템에서는 외부 입력 ID를 그대로 신뢰하기보다 생성/검증 규칙을 둔다.

### 직접 실행

1. 실행하고 r1 줄만 눈으로 모아 본다.
2. Python에서 r1만 필터링하도록 코드를 수정한다.
3. 각 요청의 step 순서가 기대한 lifecycle과 맞는지 확인한다.


### 일부를 바꿔서 다시 확인하기

`duration_ms`를 각 event에 추가하고 같은 request_id 내부의 구간별 시간을 계산해 본다. 이것이 distributed tracing의 아주 작은 정신 모형이다.

### 작은 문제

서로 다른 서비스 로그에 같은 사용자 이메일이 있으니 이메일을 correlation ID로 써도 되는가?

### 왜 맞고 왜 틀리는가

권장되지 않는다. 개인정보이고 여러 시스템에서 장기 추적 식별자가 될 수 있다. 요청 단위의 비민감 임의 ID나 표준 trace context를 사용한다.

### 자주 만나는 실패와 확인 순서

- ID를 새로 만들지만 downstream에 전달하지 않아 구간이 끊길 수 있다.
- 사용자가 보낸 임의 header를 검증 없이 로그 field에 넣으면 log injection이나 과도한 cardinality 문제가 생길 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** correlation ID는 같은 사건의 여러 기록을 묶는 식별자다.
- **직접 코딩·실행해서 익힐 것:** 섞인 event를 ID로 필터링하고 순서를 복원한다.
- **AI에게 맡겨도 되는 것:** trace propagation 코드 초안.
- **사람이 최종 확인할 것:** ID의 생성·전달·privacy·cardinality 정책을 확인한다.

### 근거 연결

`GOOGLE-SRE` · `NIST-SSDF-12`

---

## CHAPTER 04 · LESSON 04 · stack trace를 아래에서 위로 읽는 이유를 익힌다

**LESSON ID:** `T10-B03-L04`

### 먼저 쉬운 말로 이해하기

stack trace는 “무서운 긴 영어”가 아니라 함수 호출 경로와 실패 지점을 기록한 지도다. 처음에는 마지막 예외 종류와 내 코드 파일의 가장 가까운 frame을 찾는 것부터 시작한다.

### 안에서는 실제로 무엇이 일어나는가

함수 A가 B를 부르고 B가 C를 부르면 실행 stack에는 호출 관계가 쌓인다. C에서 처리되지 않은 예외가 올라오면 runtime은 이 경로를 출력한다. library 내부 frame이 많아도 내 코드에서 어떤 값으로 library를 호출했는지가 핵심일 수 있다.

### 아주 쉬운 예

세 함수 깊이에서 0으로 나누어 stack trace를 만든다.

```python
def load_ratio(total, count):
    return total / count

def summarize(values):
    return load_ratio(sum(values), len(values))

def report():
    return summarize([])

print(report())
```

### 한 줄씩 읽기

- `report`가 빈 list를 넘긴다.
- `summarize`는 길이 0을 `count`로 전달한다.
- 실제 예외는 `load_ratio`의 나눗셈 줄에서 발생한다.

### 직접 실행

1. 실행해 traceback 전체를 본다.
2. 마지막 줄에서 예외 type과 message를 찾는다.
3. 그 위 frame을 따라 `[] → len=0 → division` 경로를 적는다.


### 일부를 바꿔서 다시 확인하기

`summarize`에서 빈 list를 검증하는 방식과 `load_ratio`에서 count=0을 처리하는 방식을 각각 구현해 보고 어느 계층이 계약을 책임지는지 비교한다.

### 작은 문제

stack trace의 마지막 줄만 보고 그 줄을 무조건 고치면 왜 위험한가?

### 왜 맞고 왜 틀리는가

그 줄은 잘못된 입력을 받은 결과일 수 있다. 실제 결함은 더 위 caller가 계약을 어긴 데 있을 수 있으므로 호출 경로와 값의 흐름을 함께 본다.

### 자주 만나는 실패와 확인 순서

- caught exception을 message만 바꿔 다시 던지면서 원래 cause를 잃을 수 있다.
- production trace에 파일 path나 내부 정보가 사용자에게 그대로 노출되지 않게 한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** stack trace는 예외 종류·실패 줄·호출 경로를 함께 보여 준다.
- **직접 코딩·실행해서 익힐 것:** 의도적으로 예외를 만들고 frame을 따라 입력 흐름을 역추적한다.
- **AI에게 맡겨도 되는 것:** trace 요약과 관련 frame 후보 표시.
- **사람이 최종 확인할 것:** 실제 root cause가 어느 계약 위반인지 판단한다.

### 근거 연결

`PYTHON-EXCEPTIONS` · `GOOGLE-SRE`

---

## CHAPTER 05 · LESSON 05 · debugger의 breakpoint와 step으로 실행 흐름을 멈춘다

**LESSON ID:** `T10-B03-L05`

### 먼저 쉬운 말로 이해하기

로그를 계속 추가하는 대신 debugger로 특정 줄에서 실행을 멈추고 변수 값을 직접 볼 수 있다. breakpoint는 “여기에서 잠깐 멈춰 상태를 보자”는 관찰 지점이다.

### 안에서는 실제로 무엇이 일어나는가

step over는 현재 줄을 실행하고 다음 줄로, step into는 호출 함수 안으로 들어가고, step out은 현재 함수가 끝날 때까지 진행한다. 무작정 한 줄씩 따라가기보다 가설과 관련된 지점에 breakpoint를 둔다.

### 아주 쉬운 예

Python 내장 `breakpoint()`로 가장 단순한 debugger 경험을 만든다.

```python
def discount(price, rate):
    result = price * (1 - rate)
    breakpoint()
    return result

print(discount(10000, 0.2))
```

### 한 줄씩 읽기

- `result` 계산 뒤 debugger가 멈춘다.
- 프롬프트에서 `p price`, `p rate`, `p result`로 값을 볼 수 있다.
- `c`로 계속 실행하면 return과 print까지 진행한다.

### 직접 실행

1. 터미널에서 스크립트를 실행한다.
2. 멈춘 뒤 `p result`를 입력해 실제 값 8000.0을 확인한다.
3. `c`로 종료하고 breakpoint를 제거한다.


### 일부를 바꿔서 다시 확인하기

rate를 `20`으로 잘못 넘겨 result가 왜 음수가 되는지 debugger에서 확인한다. 입력 validation을 어디에 둘지 결정한다.

### 작은 문제

debugger에서 값이 정상으로 보여서 bug가 없다고 결론내려도 되는가?

### 왜 맞고 왜 틀리는가

아니다. 관찰한 그 실행 경로의 그 시점이 정상이라는 증거일 뿐이다. 다른 branch나 race/timing bug는 debugger 때문에 현상이 달라질 수도 있다.

### 자주 만나는 실패와 확인 순서

- debugger에서 값을 임의 수정하면 원래 실행과 다른 실험이 된다.
- production process에 무단 attach하거나 민감값을 보는 것은 보안/운영 위험이 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** breakpoint는 실행을 멈춰 상태와 control flow를 관찰하는 지점이다.
- **직접 코딩·실행해서 익힐 것:** breakpoint에서 변수 값을 직접 읽고 step/continue를 사용한다.
- **AI에게 맡겨도 되는 것:** IDE debugger 설정 안내.
- **사람이 최종 확인할 것:** 어디를 멈출지 가설을 정하고 관찰이 시스템 행동을 바꾸는지 판단한다.

### 근거 연결

`PYTHON-PDB` · `NIST-SSDF-12`

---

## CHAPTER 06 · LESSON 06 · 예외를 감쌀 때 원래 원인을 보존한다

**LESSON ID:** `T10-B03-L06`

### 먼저 쉬운 말로 이해하기

낮은 수준의 예외를 사용자에게 그대로 보여주지 않기 위해 상위 의미로 바꿔 던질 때가 있다. 이때 원래 원인을 완전히 버리면 디버깅 정보가 사라진다.

### 안에서는 실제로 무엇이 일어나는가

Python의 `raise ... from ...`처럼 exception chaining을 사용하면 “업무 의미의 오류”와 “실제 low-level cause”를 함께 보존할 수 있다. 반대로 모든 예외를 `except Exception: return None`으로 삼켜 버리면 실패가 정상값처럼 보일 수 있다.

### 아주 쉬운 예

파일 읽기 실패를 설정 로드 실패로 감싸 본다.

```python
class ConfigError(RuntimeError):
    pass

def load_config(path):
    try:
        with open(path, "r", encoding="utf-8") as f:
            return f.read()
    except OSError as exc:
        raise ConfigError(f"cannot load config: {path}") from exc

load_config("missing.txt")
```

### 한 줄씩 읽기

- `OSError`는 실제 파일 I/O 실패다.
- `ConfigError`는 상위 계층에서 이해할 업무 의미를 준다.
- `from exc`가 원래 cause 연결을 보존한다.

### 직접 실행

1. 실행해 두 예외가 연결되어 출력되는지 본다.
2. `from exc`를 제거하고 trace가 어떻게 달라지는지 비교한다.
3. 존재하는 파일로 바꿔 정상 경로도 실행한다.


### 일부를 바꿔서 다시 확인하기

`except Exception: return ""`로 바꾼 버전을 실행하고, 실패가 빈 설정이라는 정상처럼 보이는 문제를 설명한다.

### 작은 문제

사용자에게 내부 stack trace를 보여주지 않으려면 서버 로그에서도 cause를 지워야 하는가?

### 왜 맞고 왜 틀리는가

아니다. 외부 응답은 안전하게 추상화하면서 내부의 권한 있는 진단 기록에는 원인 chain을 보존할 수 있다. 공개 범위와 내부 진단 범위를 분리한다.

### 자주 만나는 실패와 확인 순서

- 너무 넓은 exception catch가 programming error까지 숨길 수 있다.
- exception message에 secret path/token을 넣지 않도록 logging 정책을 확인한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 오류 의미를 변환해도 원래 cause를 진단용으로 보존할 수 있다.
- **직접 코딩·실행해서 익힐 것:** chained exception과 swallowed exception을 실행해 차이를 본다.
- **AI에게 맡겨도 되는 것:** error mapping 코드 초안.
- **사람이 최종 확인할 것:** 어떤 오류를 복구할지, 어떤 정보는 사용자에게 숨기고 내부에 남길지 결정한다.

### 근거 연결

`PYTHON-EXCEPTIONS` · `NIST-SSDF-12`

---

## CHAPTER 07 · LESSON 07 · 로그·metric·trace가 서로 다른 질문에 답한다

**LESSON ID:** `T10-B03-L07`

### 먼저 쉬운 말로 이해하기

관측 가능성 도구를 모두 “로그”라고 부르면 선택이 흐려진다. log는 개별 사건의 자세한 기록, metric은 시간에 따른 숫자 집계, trace는 한 요청이 여러 구간을 지난 경로와 시간을 보는 데 강하다.

### 안에서는 실제로 무엇이 일어나는가

한 요청의 정확한 예외를 찾는 데는 log가 유리하고, 전체 오류율이 1%에서 8%로 올랐는지는 metric이 빠르다. 어느 dependency 구간이 느려졌는지는 distributed trace가 힌트를 준다. 셋은 대체재가 아니라 서로 보완한다.

### 아주 쉬운 예

같은 요청에서 latency라는 숫자와 사건 로그를 동시에 만든다.

```python
events = [120, 130, 900, 140]
print("count", len(events))
print("max_ms", max(events))
for i, ms in enumerate(events, 1):
    if ms > 500:
        print("slow_request", i, ms)
```

### 한 줄씩 읽기

- count/max는 metric 사고방식의 작은 예다.
- slow_request 한 건은 구체 사건을 찾는 log 사고방식이다.
- 실제 trace라면 그 900ms 요청의 DB/API 구간을 더 쪼갠다.

### 직접 실행

1. 실행해 max와 slow request가 같은 이상치를 가리키는지 본다.
2. 900을 400으로 바꿔 alert 조건이 사라지는지 확인한다.
3. 평균만 계산했을 때 이상치가 얼마나 가려지는지 비교한다.


### 일부를 바꿔서 다시 확인하기

p50/p95 같은 percentile 개념을 추가로 조사하고, 평균 latency 하나만으로 tail 문제를 놓칠 수 있는 사례를 적는다.

### 작은 문제

오류율 metric이 정상이라면 사용자 불만 로그는 무시해도 되는가?

### 왜 맞고 왜 틀리는가

안 된다. 전체 비율이 정상이어도 특정 기능·지역·계정군에 집중된 오류가 있을 수 있다. metric의 집계 범위와 개별 evidence를 함께 본다.

### 자주 만나는 실패와 확인 순서

- high-cardinality field를 무분별하게 metric label로 쓰면 비용과 성능이 폭증할 수 있다.
- trace sampling 때문에 모든 요청이 trace에 남는다고 가정하면 안 된다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** log·metric·trace는 서로 다른 관측 질문에 강점이 있다.
- **직접 코딩·실행해서 익힐 것:** 같은 현상을 사건/집계/구간 관점으로 나눠 본다.
- **AI에게 맡겨도 되는 것:** dashboard/query 초안.
- **사람이 최종 확인할 것:** 관측 데이터의 범위·sampling·privacy·비용을 확인한다.

### 근거 연결

`GOOGLE-SRE` · `DORA-2025`

---
