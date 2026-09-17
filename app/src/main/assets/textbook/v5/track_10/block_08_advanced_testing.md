# TRACK 10 · 오류·테스트·Git·빌드·배포 — 변경을 안전하게 만드는 기술

# BLOCK 08 · 강한 테스트 도구와 검출력

기본 예시 몇 개를 넘어서 test 자체의 힘을 높이는 방법을 배운다. property-based testing, shrinking, fuzzing, mutation, contract, snapshot, coverage와 risk의 한계를 각각 실제 실패 유형과 연결한다.


```text
개념 설명 → 아주 쉬운 예 → 한 줄씩 해석 → 직접 실행 → 일부 수정 → 작은 문제 → 왜 맞고 틀렸는지 설명
```

---

## CHAPTER 01 · LESSON 01 · property-based test는 예시 몇 개 대신 항상 지켜야 할 성질을 검사한다

**LESSON ID:** `T10-B08-L01`

### 먼저 쉬운 말로 이해하기

예시 기반 test는 1, 2, 10 같은 몇 값을 직접 고른다. property-based test는 입력 범위를 생성하고 그 모든 사례에서 지켜야 할 **성질(property)**을 검사한다.

### 안에서는 실제로 무엇이 일어나는가

좋은 property는 구현 복사가 아니라 수학적/업무적 불변식을 표현한다. 정렬이라면 “결과가 비내림차순이고 입력 원소를 잃지 않는다” 같은 성질이 있다. 도구는 edge case를 탐색하고 실패 입력을 더 작게 줄여 줄 수 있다.

### 아주 쉬운 예

Hypothesis로 list 정렬 성질을 검사한다.

```python
from hypothesis import given, strategies as st

@given(st.lists(st.integers()))
def test_sort_is_ordered(xs):
    ys = sorted(xs)
    assert all(a <= b for a, b in zip(ys, ys[1:]))
    assert sorted(xs) == ys
```

### 한 줄씩 읽기

- strategy가 다양한 integer list를 만든다.
- 첫 assertion은 순서 property를 검사한다.
- 두 번째는 교육용으로 다소 중복이므로 실제 test에서는 multiset 보존 같은 더 독립적인 성질이 좋다.

### 직접 실행

1. Hypothesis가 설치된 환경에서 test를 실행한다.
2. 정렬 결과를 일부러 reverse로 바꿔 어떤 최소 반례가 나오는지 본다.
3. 실패 예시가 shrink되는 과정을 기록한다.


### 일부를 바꿔서 다시 확인하기

`sum(xs) == sum(ys)`만으로 원소 보존을 증명할 수 없는 반례를 찾아 더 강한 property를 설계한다.

### 작은 문제

property-based test가 많은 입력을 생성하면 E2E test가 필요 없어지는가?

### 왜 맞고 왜 틀리는가

아니다. 생성 범위 내 함수 성질에 강하지만 실제 component 연결·배포 환경은 별도 범위다.

### 자주 만나는 실패와 확인 순서

- 잘못된 property를 쓰면 많은 test가 모두 green이어도 의미가 약하다.
- 무한/너무 큰 입력 생성은 실행 시간을 폭증시킬 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** property-based test는 입력 예시보다 범용 불변식을 정의하고 자동 생성 입력으로 검사한다.
- **직접 코딩·실행해서 익힐 것:** property를 깨뜨리고 최소 반례를 직접 본다.
- **AI에게 맡겨도 되는 것:** strategy/property 후보 생성.
- **사람이 최종 확인할 것:** property가 실제 요구사항인지와 입력 domain을 확인한다.

### 근거 연결

`HYPOTHESIS` · `PYTEST`

---

## CHAPTER 02 · LESSON 02 · shrinking은 실패 입력을 가장 이해하기 쉬운 반례로 줄인다

**LESSON ID:** `T10-B08-L02`

### 먼저 쉬운 말로 이해하기

자동 생성 test가 길이 10,000인 복잡한 입력에서 실패하면 원인을 읽기 어렵다. property testing 도구의 shrinking은 실패를 유지하면서 입력을 더 작은 형태로 줄여 사람이 이해하기 쉬운 반례를 찾는다.

### 안에서는 실제로 무엇이 일어나는가

shrinking은 “원래 운영 입력과 똑같다”는 뜻이 아니다. 같은 property를 깨뜨리는 더 단순한 입력을 찾는 과정이다. 최소 반례는 regression test와 원인 설명에 유용하다.

### 아주 쉬운 예

의도적으로 0에서 실패하는 reciprocal 함수를 검사한다.

```python
from hypothesis import given, strategies as st

@given(st.integers())
def test_reciprocal_round_trip(x):
    y = 1 / x
    assert (1 / y) == x
```

### 한 줄씩 읽기

- integer strategy에는 0이 포함될 수 있다.
- 0은 첫 연산에서 division error를 낸다.
- 도구는 복잡한 실패보다 작은 0을 쉽게 찾을 수 있다.

### 직접 실행

1. test를 실행해 실패 입력을 확인한다.
2. strategy를 `integers().filter(lambda x: x != 0)`로 제한해 domain 의미를 바꾼다.
3. float 오차 때문에 exact equality가 또 어떤 문제를 만드는지 본다.


### 일부를 바꿔서 다시 확인하기

실패를 숨기려고 무조건 `assume(x != 0)`를 넣기 전에 0이 제품 domain에서 유효한지 요구사항을 확인한다.

### 작은 문제

shrunk input이 실제 고객 입력과 다르니 쓸모없다고 볼 수 있는가?

### 왜 맞고 왜 틀리는가

아니다. 동일한 invariant를 깨는 핵심 조건을 드러낸다. 실제 사건과의 연결은 별도로 확인한다.

### 자주 만나는 실패와 확인 순서

- filter/assume를 과도하게 쓰면 유효한 입력 공간을 실수로 제외할 수 있다.
- floating-point property는 수치 오차 허용을 설계해야 한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** shrinking은 실패를 유지하는 더 작은 반례를 찾아 진단을 쉽게 한다.
- **직접 코딩·실행해서 익힐 것:** 실패 property에서 최소 반례를 관찰한다.
- **AI에게 맡겨도 되는 것:** property 최소화 설명.
- **사람이 최종 확인할 것:** 반례가 실제 input domain에 포함되는지 확인한다.

### 근거 연결

`HYPOTHESIS`

---

## CHAPTER 03 · LESSON 03 · fuzzing은 예상하지 못한 입력으로 parser와 경계를 두드린다

**LESSON ID:** `T10-B08-L03`

### 먼저 쉬운 말로 이해하기

parser, decoder, file/network input처럼 외부에서 복잡한 byte/string이 들어오는 경계에는 사람이 생각하지 못한 조합이 많다. fuzzing은 대량의 변형 입력을 넣어 crash, hang, invariant 위반을 찾는다.

### 안에서는 실제로 무엇이 일어나는가

fuzzer는 단순 random과 다르게 coverage feedback이나 corpus mutation을 이용할 수 있다. 초급 단계에서는 “외부 입력은 항상 이상할 수 있고, 실패 입력을 보존해 재현한다”는 원리를 익힌다.

### 아주 쉬운 예

작은 parser에 여러 문자열을 넣는다.

```python
def parse_pair(text):
    left, right = text.split(":")
    return int(left), int(right)

cases = ["1:2", "", ":", "1:2:3", "a:2", "999999:1"]
for case in cases:
    try:
        print(case, parse_pair(case))
    except Exception as e:
        print(case, type(e).__name__)
```

### 한 줄씩 읽기

- 정상 한 건과 여러 비정상 shape를 함께 넣는다.
- 예외 종류가 입력별로 다를 수 있다.
- 진짜 fuzzer는 훨씬 많은 변형을 자동 생성한다.

### 직접 실행

1. 스크립트를 실행해 각 입력 결과를 본다.
2. 문자열 길이 제한을 추가하고 초대형 input 정책을 정의한다.
3. 발견된 crash input을 regression test로 고정한다.


### 일부를 바꿔서 다시 확인하기

무한 loop나 과도한 memory를 유발할 수 있는 입력을 안전한 timeout/resource limit 환경에서만 fuzz해야 하는 이유를 적는다.

### 작은 문제

fuzzer가 crash를 찾지 못했으니 parser가 안전하다고 증명된 것인가?

### 왜 맞고 왜 틀리는가

아니다. 탐색한 입력·시간·coverage 범위에서 발견하지 못한 것이다. fuzzing은 증거를 늘리지만 완전한 증명은 아니다.

### 자주 만나는 실패와 확인 순서

- production endpoint를 무제한 fuzz해 서비스 장애를 만들면 안 된다.
- 민감 corpus를 외부 fuzz service에 업로드하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** fuzzing은 자동 생성/변형 입력으로 경계의 예상 밖 실패를 탐색한다.
- **직접 코딩·실행해서 익힐 것:** 작은 malformed corpus를 실행하고 crash input을 보존한다.
- **AI에게 맡겨도 되는 것:** fuzz harness/corpus 생성.
- **사람이 최종 확인할 것:** 안전한 격리 환경과 탐색 범위를 확인한다.

### 근거 연결

`NIST-SSDF-12` · `OWASP-TESTING`

---

## CHAPTER 04 · LESSON 04 · mutation testing은 테스트가 일부러 넣은 결함을 잡는지 본다

**LESSON ID:** `T10-B08-L04`

### 먼저 쉬운 말로 이해하기

coverage가 높아도 assertion이 약하면 잘못된 코드를 통과시킬 수 있다. mutation testing은 `>=`를 `>`로 바꾸거나 연산자를 뒤집는 작은 결함을 자동으로 넣고 test가 그것을 실패시키는지 검사한다.

### 안에서는 실제로 무엇이 일어나는가

test가 mutant를 죽인다는 것은 그 변화가 test에 의해 감지됐다는 뜻이다. 살아남은 mutant는 빠진 test, 동등한 변화, 중요하지 않은 code 등 여러 의미가 있으므로 사람이 해석한다.

### 아주 쉬운 예

경계 조건 mutant를 손으로 만들어 원리를 본다.

```python
def allowed(age):
    return age >= 19

def test_allowed():
    assert allowed(18) is False
    assert allowed(19) is True
```

### 한 줄씩 읽기

- 원래 `>=`는 test를 통과한다.
- mutant로 `> 19`를 만들면 19 assertion이 실패해야 한다.
- 18 case만 있었다면 이 mutant가 살아남을 수 있다.

### 직접 실행

1. 원본 test를 실행한다.
2. 조건을 `> 19`로 임시 변경해 test가 red 되는지 확인한다.
3. 다시 원복하고 실제 mutation tool을 조사한다.


### 일부를 바꿔서 다시 확인하기

test suite가 오래 걸릴 때 전체 mutation 비용을 줄이기 위한 changed-code/risk-based 전략을 설계한다.

### 작은 문제

mutation score 100%가 품질 100점을 의미하는가?

### 왜 맞고 왜 틀리는가

아니다. 생성 mutation 종류와 코드 범위에 대한 검출력 지표다. 요구사항 누락, 통합 오류, 보안·운영 위험을 모두 대변하지 않는다.

### 자주 만나는 실패와 확인 순서

- 동등 mutant를 억지로 죽이려고 무의미한 test를 쓰지 않는다.
- mutation run은 비용이 클 수 있어 CI cadence를 조정한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** mutation testing은 의도적 작은 결함을 test가 검출하는지 확인한다.
- **직접 코딩·실행해서 익힐 것:** 조건 mutant를 직접 넣어 regression test 검출력을 확인한다.
- **AI에게 맡겨도 되는 것:** mutation 결과 분류.
- **사람이 최종 확인할 것:** 살아남은 mutant의 실제 위험도를 판단한다.

### 근거 연결

`PYTEST` · `DORA-2025`

---

## CHAPTER 05 · LESSON 05 · contract test는 서비스 사이의 약속이 맞는지 검사한다

**LESSON ID:** `T10-B08-L05`

### 먼저 쉬운 말로 이해하기

서비스 A가 B의 API를 호출할 때 A의 fake가 오래된 schema를 흉내 내면 unit test는 green인데 production은 깨질 수 있다. contract test는 consumer와 provider가 합의한 요청/응답 약속을 검증한다.

### 안에서는 실제로 무엇이 일어나는가

계약에는 field 이름/type, required/optional, status code, semantic constraint가 들어갈 수 있다. consumer-driven contract에서는 소비자가 실제 필요 조건을 표현하고 provider가 그 계약을 충족하는지 검증한다.

### 아주 쉬운 예

아주 작은 JSON contract를 함수로 확인한다.

```python
def valid_user_response(data):
    return (
        isinstance(data, dict)
        and isinstance(data.get("id"), int)
        and isinstance(data.get("name"), str)
    )

assert valid_user_response({"id":1, "name":"Kim"})
assert not valid_user_response({"id":"1", "name":"Kim"})
```

### 한 줄씩 읽기

- id type이 contract 일부다.
- provider가 문자열로 바꾸면 consumer parsing이 깨질 수 있다.
- 실무 contract tool은 versioned interaction과 provider verification을 자동화할 수 있다.

### 직접 실행

1. 스크립트를 실행한다.
2. optional `nickname`을 추가해 backward compatibility를 생각한다.
3. 기존 required field를 삭제했을 때 어떤 consumer가 깨지는지 적는다.


### 일부를 바꿔서 다시 확인하기

API versioning 없이 field type을 바꾸는 변경과 새 optional field를 추가하는 변경의 위험 차이를 설명한다.

### 작은 문제

provider unit test가 모두 통과하면 contract test가 불필요한가?

### 왜 맞고 왜 틀리는가

아니다. provider 내부 correctness와 consumer가 기대하는 외부 계약 일치는 다른 범위다.

### 자주 만나는 실패와 확인 순서

- contract가 실제 production traffic 요구를 과도하게 축소하면 놓치는 상호작용이 생긴다.
- 민감한 실제 응답을 contract fixture로 그대로 저장하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** contract test는 component 사이의 외부 약속이 양쪽에서 유지되는지 검사한다.
- **직접 코딩·실행해서 익힐 것:** schema 변경을 만들어 consumer 기대가 깨지는지 본다.
- **AI에게 맡겨도 되는 것:** contract schema/fixture 초안.
- **사람이 최종 확인할 것:** 호환성 정책과 실제 consumer 필요를 확인한다.

### 근거 연결

`NIST-SSDF-12` · `DORA-2025`

---

## CHAPTER 06 · LESSON 06 · snapshot·golden test는 큰 출력 변화를 diff로 검토한다

**LESSON ID:** `T10-B08-L06`

### 먼저 쉬운 말로 이해하기

복잡한 JSON, 렌더링 결과, compiler output처럼 field가 많으면 expected를 일일이 assertion하기 어렵다. snapshot/golden test는 승인된 기준 출력을 파일로 저장하고 새 결과와 비교한다.

### 안에서는 실제로 무엇이 일어나는가

강점은 큰 구조 변화가 한눈에 보인다는 것이고, 약점은 개발자가 이유 없이 “snapshot update”를 눌러 결함까지 승인할 수 있다는 것이다. 안정적인 출력에 쓰고 timestamp/random field는 정규화한다.

### 아주 쉬운 예

문자열 report를 golden 값과 비교한다.

```python
def render(user):
    return f"ID={user['id']}\nNAME={user['name']}\n"

golden = "ID=1\nNAME=Kim\n"
assert render({"id":1,"name":"Kim"}) == golden
```

### 한 줄씩 읽기

- golden은 승인된 기준 출력이다.
- 렌더 결과가 바뀌면 전체 문자열 diff가 생긴다.
- 무작위 timestamp가 들어가면 매번 snapshot이 깨질 수 있다.

### 직접 실행

1. 원본을 실행한다.
2. render에 `ROLE=user` 줄을 추가해 test를 실패시킨다.
3. 요구사항 변경이라면 golden diff를 사람이 검토한 뒤 갱신한다.


### 일부를 바꿔서 다시 확인하기

출력에 현재 시각이 포함되는 버전에서 시각을 주입하거나 제거해 snapshot을 안정화한다.

### 작은 문제

snapshot 100개가 한꺼번에 바뀌었다. 전부 자동 accept해도 되는가?

### 왜 맞고 왜 틀리는가

안 된다. 왜 바뀌었는지 diff를 검토해야 한다. 의도치 않은 광범위 변경일 수 있다.

### 자주 만나는 실패와 확인 순서

- binary/대형 snapshot은 review가 사실상 불가능해질 수 있다.
- 개인정보가 snapshot repository에 영구 저장되지 않게 한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** snapshot/golden test는 기준 출력과 새 출력을 diff로 검토하는 방식이다.
- **직접 코딩·실행해서 익힐 것:** 의도된/의도치 않은 출력 변경을 만들어 review한다.
- **AI에게 맡겨도 되는 것:** snapshot normalization 제안.
- **사람이 최종 확인할 것:** 기준 갱신이 요구사항 변경과 일치하는지 검토한다.

### 근거 연결

`PYTEST` · `NIST-SSDF-12`

---

## CHAPTER 07 · LESSON 07 · coverage 숫자보다 위험 기반 테스트 포트폴리오를 만든다

**LESSON ID:** `T10-B08-L07`

### 먼저 쉬운 말로 이해하기

line coverage 90%는 많은 줄이 실행됐다는 뜻이지 assertion이 옳거나 중요한 위험이 보호된다는 뜻은 아니다. 테스트 자원은 결제·권한·데이터 손실·migration처럼 실패 비용이 큰 경로에 더 배분해야 한다.

### 안에서는 실제로 무엇이 일어나는가

risk는 대략 영향도×발생 가능성×탐지 어려움으로 생각할 수 있다. 숫자를 절대 점수로 믿기보다 어떤 실패를 어떤 test 층이 잡는지 matrix로 관리한다.

### 아주 쉬운 예

작은 위험 표를 코드 대신 text로 만든다.

```python
risks = [
    ("duplicate_charge", "high", "integration"),
    ("wrong_button_color", "low", "ui_snapshot"),
    ("migration_data_loss", "critical", "migration+rollback"),
]
for risk in risks:
    print(risk)
```

### 한 줄씩 읽기

- 모든 risk에 같은 수의 test를 배치하지 않는다.
- critical data loss는 rollback rehearsal까지 필요할 수 있다.
- coverage report는 빈 영역을 찾는 보조 신호로 사용한다.

### 직접 실행

1. 스크립트를 실행한다.
2. 자신의 앱 기능 5개를 위험 표로 분류한다.
3. 각 risk를 unit/integration/E2E/operational check 중 어디서 잡을지 연결한다.


### 일부를 바꿔서 다시 확인하기

coverage가 낮지만 위험도가 낮은 generated code와 coverage는 높지만 돈을 다루는 핵심 logic을 비교해 우선순위를 정한다.

### 작은 문제

coverage 100%를 달성하면 mutation/contract/E2E를 빼도 되는가?

### 왜 맞고 왜 틀리는가

안 된다. coverage는 실행 여부 중심 지표라 검출력·연결 계약·운영 환경을 대체하지 못한다.

### 자주 만나는 실패와 확인 순서

- coverage 목표 때문에 의미 없는 assertion 없는 test를 추가하지 않는다.
- 위험 평가는 시간이 지나며 제품 변화에 맞춰 갱신한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** coverage는 보조 신호이고 테스트 투자는 실패 영향과 가능성에 맞춘다.
- **직접 코딩·실행해서 익힐 것:** risk-to-test matrix를 직접 만든다.
- **AI에게 맡겨도 되는 것:** coverage gap/risk matrix 생성.
- **사람이 최종 확인할 것:** 사업 영향과 사용자 피해를 최종 평가한다.

### 근거 연결

`DORA-2025` · `NIST-SSDF-12`

---
