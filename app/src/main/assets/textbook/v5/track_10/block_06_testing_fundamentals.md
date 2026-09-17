# TRACK 10 · 오류·테스트·Git·빌드·배포 — 변경을 안전하게 만드는 기술

# BLOCK 06 · 테스트의 기본기와 검증 범위

디버깅으로 찾은 사실을 미래에도 지키려면 테스트가 필요하다. 이 BLOCK은 assertion, AAA, unit/integration/E2E, 경계값, 계약 중심 test, 실패 진단, 실행 증거를 실제 명령과 연결한다.


```text
개념 설명 → 아주 쉬운 예 → 한 줄씩 해석 → 직접 실행 → 일부 수정 → 작은 문제 → 왜 맞고 틀렸는지 설명
```

---

## CHAPTER 01 · LESSON 01 · 테스트는 입력과 기대 결과를 자동 비교한다

**LESSON ID:** `T10-B06-L01`

### 먼저 쉬운 말로 이해하기

테스트는 “코드가 좋아 보이는지” 평가하는 글이 아니라 특정 조건에서 실제 결과를 기대 결과와 비교하는 실행 가능한 검사다. 사람이 매번 버튼을 눌러 확인하던 절차를 코드로 옮기면 반복 가능해진다.

### 안에서는 실제로 무엇이 일어나는가

가장 작은 test에는 setup/input, action, assertion이 있다. assertion이 실패하면 test runner는 non-zero exit와 실패 위치를 제공한다. 테스트 파일이 존재하는 것과 실제 실행되어 통과한 것은 구분한다.

### 아주 쉬운 예

pytest로 가장 작은 덧셈 test를 만든다.

```python
def add(a, b):
    return a + b

def test_add_two_numbers():
    assert add(2, 3) == 5
```

### 한 줄씩 읽기

- 함수 정의는 production logic 예다.
- test 함수가 실제 action을 호출한다.
- assertion은 expected 5와 actual 결과를 비교한다.

### 직접 실행

1. 파일을 `test_add.py`로 저장한다.
2. `pytest -q test_add.py`를 실행한다.
3. exit code와 `1 passed`를 실제 확인한다.


### 일부를 바꿔서 다시 확인하기

기대값을 일부러 6으로 바꿔 fail report를 읽은 뒤 다시 5로 복구한다. 성공 출력만 보는 것보다 실패 메시지를 읽는 능력이 중요하다.

### 작은 문제

test 파일을 만들었지만 한 번도 실행하지 않았다. 상태를 PASS라고 써도 되는가?

### 왜 맞고 왜 틀리는가

안 된다. `PREPARED` 또는 “테스트 작성됨, 미실행”이다. PASS는 runner가 실제 실행되어 성공 증거가 있을 때만 쓴다.

### 자주 만나는 실패와 확인 순서

- assertion 없는 test는 실행되더라도 아무것도 검증하지 않을 수 있다.
- 실패 test를 skip 처리해 숫자만 green으로 만드는 것을 경계한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 테스트는 실제 결과와 명시된 기대를 자동 비교하는 실행 가능한 검사다.
- **직접 코딩·실행해서 익힐 것:** pytest에서 pass와 fail을 모두 직접 실행한다.
- **AI에게 맡겨도 되는 것:** test skeleton과 edge case 생성.
- **사람이 최종 확인할 것:** 기대값 근거와 test가 실제 실행됐는지 확인한다.

### 근거 연결

`PYTEST` · `TEXTBOOK-V5-CONTRACT`

---

## CHAPTER 02 · LESSON 02 · Arrange–Act–Assert로 테스트 의도를 읽기 쉽게 나눈다

**LESSON ID:** `T10-B06-L02`

### 먼저 쉬운 말로 이해하기

테스트가 길어지면 준비·실행·검증이 섞여 무엇을 확인하는지 모호해진다. Arrange–Act–Assert는 문법 규칙이 아니라 사고 순서를 분리하는 방식이다.

### 안에서는 실제로 무엇이 일어나는가

Arrange에서는 입력과 상태를 만든다. Act에서는 검사 대상 동작을 한 번 수행한다. Assert에서는 관찰 가능한 결과를 검증한다. 여러 동작이 한 test에 섞이면 실패 원인이 넓어질 수 있다.

### 아주 쉬운 예

할인 계산 test를 세 단계로 적는다.

```python
def discounted(price, rate):
    return price * (1-rate)

def test_discount():
    # Arrange
    price, rate = 10000, 0.2
    # Act
    actual = discounted(price, rate)
    # Assert
    assert actual == 8000
```

### 한 줄씩 읽기

- 입력 준비가 첫 구역에 있다.
- action은 한 번의 business operation이다.
- assertion이 최종 계약을 확인한다.

### 직접 실행

1. pytest로 실행한다.
2. rate를 1.2로 바꾸어 현재 함수가 어떤 결과를 내는지 본다.
3. 유효 rate 범위 validation 요구가 있다면 별도 test를 만든다.


### 일부를 바꿔서 다시 확인하기

두 개의 서로 다른 기능 assertion을 한 test에 넣었다면 각각 독립 test로 나누어 실패 메시지가 더 분명해지는지 비교한다.

### 작은 문제

AAA를 지키기 위해 comment 세 줄만 넣으면 좋은 test가 되는가?

### 왜 맞고 왜 틀리는가

아니다. 실제로 setup/action/assertion 책임이 분리되어야 한다. label만 붙인다고 test scope가 작아지거나 기대값이 올바르게 되지는 않는다.

### 자주 만나는 실패와 확인 순서

- Arrange에서 network/DB setup이 너무 커지면 unit test가 사실상 integration test가 될 수 있다.
- Assert가 implementation detail만 검사하면 리팩터링에 취약하다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** AAA는 준비·동작·검증의 책임을 분리하는 읽기 구조다.
- **직접 코딩·실행해서 익힐 것:** 한 test의 action과 assertion 범위를 줄여 본다.
- **AI에게 맡겨도 되는 것:** test 리팩터링 제안.
- **사람이 최종 확인할 것:** 사용자 관점 계약을 검사하는지 확인한다.

### 근거 연결

`PYTEST`

---

## CHAPTER 03 · LESSON 03 · unit·integration·E2E의 검증 범위를 구분한다

**LESSON ID:** `T10-B06-L03`

### 먼저 쉬운 말로 이해하기

테스트 종류는 우열 순위가 아니라 **어디까지 실제로 연결해 검증하는가**가 다르다. unit은 작은 단위를 빠르게 고립하고, integration은 여러 구성요소의 계약을 확인하며, E2E는 사용자 경로를 넓게 확인한다.

### 안에서는 실제로 무엇이 일어나는가

unit만 있으면 DB schema/API wiring 같은 연결 오류를 놓칠 수 있고, E2E만 있으면 느리고 실패 원인 파악이 어렵다. 위험과 비용에 맞춰 층을 조합한다.

### 아주 쉬운 예

가격 계산 함수를 unit으로 검사하고, 파일 저장까지 연결하면 integration 범위가 넓어진다는 그림을 본다.

```python
def subtotal(price, qty):
    return price * qty

def test_subtotal_unit():
    assert subtotal(1200, 3) == 3600
```

### 한 줄씩 읽기

- 외부 I/O가 없어서 계산 logic만 빠르게 검사한다.
- 실제 DB repository를 붙이면 integration 성격이 커진다.
- 브라우저/앱 화면부터 서버·DB까지 실제 연결하면 E2E에 가깝다.

### 직접 실행

1. unit test를 실행한다.
2. 같은 기능의 integration/E2E에서 추가로 잡을 실패를 각각 2개씩 적는다.
3. 모든 test를 E2E로 만들었을 때 비용을 추정한다.


### 일부를 바꿔서 다시 확인하기

예: DB column 이름 불일치, JSON serialization, auth middleware, navigation 같은 연결 위험을 test 층에 배치한다.

### 작은 문제

unit test 100% 통과면 production 기능이 연결돼 있다고 볼 수 있는가?

### 왜 맞고 왜 틀리는가

아니다. unit 범위 밖의 wiring, configuration, external contract, device/runtime는 별도 증거가 필요하다.

### 자주 만나는 실패와 확인 순서

- test 이름에 unit/integration을 쓰는 것만으로 실제 범위가 결정되지 않는다.
- E2E가 third-party에 의존하면 불안정성과 비용이 커질 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** unit/integration/E2E는 실제 연결 범위와 실패 검출 종류가 다르다.
- **직접 코딩·실행해서 익힐 것:** 한 기능을 세 층으로 나눠 어떤 위험을 검사할지 설계한다.
- **AI에게 맡겨도 되는 것:** test pyramid/portfolio 초안.
- **사람이 최종 확인할 것:** 제품 위험에 맞는 범위와 실행 비용을 결정한다.

### 근거 연결

`PYTEST` · `DORA-2025`

---

## CHAPTER 04 · LESSON 04 · 정상값보다 경계값과 실패 경로를 의도적으로 검사한다

**LESSON ID:** `T10-B06-L04`

### 먼저 쉬운 말로 이해하기

사용자는 평균적인 입력만 보내지 않는다. 빈 문자열, 0, 최댓값, null, 중복 요청, timeout처럼 경계에서 결함이 자주 나온다. 테스트는 happy path뿐 아니라 실패 계약을 명시한다.

### 안에서는 실제로 무엇이 일어나는가

경계는 요구사항에서 찾는다. `age >= 19`, 최대 길이 100, retry 최대 3회처럼 비교 연산자의 양쪽 값을 검사하면 off-by-one을 잡기 쉽다.

### 아주 쉬운 예

나이 조건에서 18·19·20을 검사한다.

```python
def adult(age):
    return age >= 19

def test_boundary():
    assert adult(18) is False
    assert adult(19) is True
    assert adult(20) is True
```

### 한 줄씩 읽기

- 18은 경계 바로 아래다.
- 19는 정확한 경계다.
- 20은 경계 위에서 규칙이 유지되는지 본다.

### 직접 실행

1. pytest를 실행한다.
2. 조건을 `> 19`로 바꿔 어떤 assertion이 실패하는지 본다.
3. 음수/비정상 type은 요구사항에 맞춰 validation test로 분리한다.


### 일부를 바꿔서 다시 확인하기

문자열 최대 길이 100이라는 규칙에 99,100,101 case를 만들어 같은 패턴을 적용한다.

### 작은 문제

테스트 case를 많이 만들수록 무조건 좋은가?

### 왜 맞고 왜 틀리는가

아니다. 위험과 경계를 대표하는 case가 중요하다. 의미가 같은 값 수천 개는 유지비만 늘릴 수 있다.

### 자주 만나는 실패와 확인 순서

- 예외가 난다는 것만 검사하고 정확한 예외 의미를 놓칠 수 있다.
- production 장애 case를 regression으로 남기되 개인정보는 제거한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 경계값은 규칙이 바뀌는 바로 아래·정확한 값·바로 위를 우선 검사한다.
- **직접 코딩·실행해서 익힐 것:** off-by-one 결함을 일부러 만들고 test가 잡는지 본다.
- **AI에게 맡겨도 되는 것:** boundary case 목록 생성.
- **사람이 최종 확인할 것:** 요구사항에서 진짜 경계를 확인한다.

### 근거 연결

`PYTEST` · `HYPOTHESIS`

---

## CHAPTER 05 · LESSON 05 · assertion은 관찰 가능한 계약을 검사한다

**LESSON ID:** `T10-B06-L05`

### 먼저 쉬운 말로 이해하기

좋은 테스트는 내부 지역변수 이름보다 외부에서 의미 있는 결과를 검사한다. 구현 세부에 붙으면 리팩터링만 해도 깨지는 brittle test가 된다.

### 안에서는 실제로 무엇이 일어나는가

관찰 가능한 계약에는 return value, 저장된 record, emitted event, HTTP status/body처럼 소비자가 의존하는 결과가 있다. private helper가 몇 번 호출됐는지 같은 것은 그 자체가 계약일 때만 중요하다.

### 아주 쉬운 예

정렬 함수의 내부 algorithm이 아니라 결과 속성을 검사한다.

```python
def normalize(names):
    return sorted(n.strip().lower() for n in names)

def test_normalize_contract():
    assert normalize([" Bob ", "alice"]) == ["alice", "bob"]
```

### 한 줄씩 읽기

- test는 어떤 정렬 algorithm을 썼는지 모른다.
- trim/lower/sort라는 사용자 관점 결과를 검증한다.
- 구현을 바꿔도 같은 계약이면 test는 유지된다.

### 직접 실행

1. test를 실행한다.
2. generator를 list comprehension으로 바꿔도 test가 그대로 통과하는지 본다.
3. 내부 helper 호출 count를 억지로 assert했을 때 리팩터링 취약성을 비교한다.


### 일부를 바꿔서 다시 확인하기

성능상 “외부 API는 최대 한 번 호출”이 진짜 계약이라면 call count assertion이 정당할 수 있다. implementation detail과 비기능 요구를 구분한다.

### 작은 문제

private 함수 단위 test가 많으면 나쁜가?

### 왜 맞고 왜 틀리는가

항상 그렇지는 않지만 public behavior가 충분히 보호되는지, private test가 리팩터링을 과도하게 방해하는지 균형을 봐야 한다.

### 자주 만나는 실패와 확인 순서

- snapshot 전체를 무분별하게 assertion으로 쓰면 작은 변경에도 큰 diff가 난다.
- assertion message 없이 복잡한 조건 하나로 합치면 실패 원인이 불명확해질 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** assertion은 소비자가 의존하는 계약을 우선 검사한다.
- **직접 코딩·실행해서 익힐 것:** 구현을 바꿔도 public behavior test가 유지되는지 실험한다.
- **AI에게 맡겨도 되는 것:** assertion 후보 생성.
- **사람이 최종 확인할 것:** 무엇이 진짜 계약인지 결정한다.

### 근거 연결

`PYTEST` · `DORA-2025`

---

## CHAPTER 06 · LESSON 06 · test 실패를 제품 bug와 test bug로 나눠 진단한다

**LESSON ID:** `T10-B06-L06`

### 먼저 쉬운 말로 이해하기

test가 red라고 해서 production code가 항상 틀린 것은 아니다. 기대값이 오래됐거나 fixture가 잘못됐거나 환경이 깨졌을 수 있다. 반대로 “test가 이상하다”며 삭제하는 것도 위험하다.

### 안에서는 실제로 무엇이 일어나는가

먼저 실패 메시지와 actual/expected를 확인하고, 요구사항 출처를 검증한다. 그다음 production code와 test setup을 각각 최소 재현한다. test 자체도 code이므로 bug가 생길 수 있다.

### 아주 쉬운 예

잘못된 기대값을 일부러 넣어 차이를 본다.

```python
def tax(amount):
    return amount * 0.1

def test_tax():
    assert tax(1000) == 90  # 오래된 잘못된 기대값이라고 가정
```

### 한 줄씩 읽기

- 함수 결과는 100이다.
- test expected는 90이다.
- 어느 쪽을 바꿀지는 현재 세율 요구사항을 확인해야 결정된다.

### 직접 실행

1. pytest 실패를 본다.
2. 요구사항이 10%임을 확인했다고 가정하고 expected를 100으로 고친다.
3. 반대로 요구사항이 9%였다면 production logic을 고쳐야 함을 적는다.


### 일부를 바꿔서 다시 확인하기

fixture가 1000이 아니라 문자열 `"1000"`을 넣도록 바꾸고 실패 종류가 어떻게 달라지는지 본다.

### 작은 문제

test를 green으로 만드는 가장 빠른 수정이 항상 올바른가?

### 왜 맞고 왜 틀리는가

아니다. green은 기대와 실제가 일치한다는 신호일 뿐, 기대 자체가 요구사항과 틀리면 잘못된 green이 가능하다.

### 자주 만나는 실패와 확인 순서

- 실패 test를 xfail/skip 처리할 때 이유와 만료 조건 없이 방치하지 않는다.
- 시간/순서 의존 test는 production code와 별개로 flaky할 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** test 실패는 product code, test code, 환경 중 어디서든 원인이 생길 수 있다.
- **직접 코딩·실행해서 익힐 것:** actual/expected와 요구사항을 분리해 진단한다.
- **AI에게 맡겨도 되는 것:** failure triage 요약.
- **사람이 최종 확인할 것:** 기대값의 권위 있는 근거와 skip 정당성을 확인한다.

### 근거 연결

`PYTEST` · `DORA-2025`

---

## CHAPTER 07 · LESSON 07 · 테스트 실행 결과를 재현 가능한 증거로 남긴다

**LESSON ID:** `T10-B06-L07`

### 먼저 쉬운 말로 이해하기

“테스트했음”보다 어떤 명령, 어떤 commit, 몇 개 test, 어떤 결과였는지가 필요하다. 자동화 runner의 exit code와 report는 검증 범위를 나중에 추적할 수 있게 한다.

### 안에서는 실제로 무엇이 일어나는가

CI에서도 동일하다. green badge는 특정 workflow와 commit에 대한 증거다. 다른 branch, 다른 환경, 배포된 artifact까지 자동으로 보장하지 않는다. test evidence를 build/release identity와 연결하는 습관이 중요하다.

### 아주 쉬운 예

로컬에서는 명령과 exit code를 가장 작은 증거로 남길 수 있다.

```bash
# 예시 셸 흐름
pytest -q
code=$?
echo "pytest_exit=$code"
exit $code
```

### 한 줄씩 읽기

- pytest가 성공하면 일반적으로 exit code 0이다.
- shell이 code를 보존한다.
- 실패 code를 0으로 덮어쓰면 CI가 거짓 green이 될 수 있다.

### 직접 실행

1. 이 패키지의 executable tests를 실행한다.
2. 명령·test count·exit code를 report에 기록한다.
3. 의도적으로 한 assertion을 깨뜨린 임시 test에서 non-zero를 확인한 뒤 원복한다.


### 일부를 바꿔서 다시 확인하기

JUnit/XML 같은 machine-readable report가 왜 필요한지 조사한다. 사람이 보는 console과 자동 시스템이 읽는 결과를 분리할 수 있다.

### 작은 문제

CI job이 `pytest || true`로 되어 있다. 화면에 실패가 보여도 workflow는 성공할 수 있는가?

### 왜 맞고 왜 틀리는가

그렇다. `|| true`가 실패 exit를 성공으로 바꾸면 gate가 무력화된다. 의도된 non-blocking check인지 명확히 구분해야 한다.

### 자주 만나는 실패와 확인 순서

- log가 잘렸거나 test discovery가 0개인데 green인 경우를 확인한다.
- cached result를 새 commit 실행 결과로 오해하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 검증 증거에는 실행 명령·대상 revision·결과·범위가 필요하다.
- **직접 코딩·실행해서 익힐 것:** exit code를 보존하고 test discovery count를 확인한다.
- **AI에게 맡겨도 되는 것:** report 포맷과 CI 요약 생성.
- **사람이 최종 확인할 것:** 실패를 성공으로 삼키는 설정과 미실행 범위를 확인한다.

### 근거 연결

`DORA-2025` · `TEXTBOOK-V5-CONTRACT`

---
