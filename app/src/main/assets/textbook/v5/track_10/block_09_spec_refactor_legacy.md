# TRACK 10 · 오류·테스트·Git·빌드·배포 — 변경을 안전하게 만드는 기술

# BLOCK 09 · 명세·불변식·리팩터링·legacy 안전망

테스트가 있는 것만으로 변경이 쉬워지지는 않는다. 무엇을 지켜야 하는지 명세와 invariant로 만들고, legacy behavior를 고정한 뒤 작은 refactor와 module boundary로 변경 파급을 줄이는 법을 배운다.


```text
개념 설명 → 아주 쉬운 예 → 한 줄씩 해석 → 직접 실행 → 일부 수정 → 작은 문제 → 왜 맞고 틀렸는지 설명
```

---

## CHAPTER 01 · LESSON 01 · acceptance criteria로 “완료”의 관찰 조건을 먼저 정한다

**LESSON ID:** `T10-B09-L01`

### 먼저 쉬운 말로 이해하기

기능을 만들고 나서 “이 정도면 됐지”라고 판단하면 사람마다 완료 기준이 달라진다. acceptance criteria는 사용자가 볼 수 있는 조건으로 성공/실패를 미리 적는다.

### 안에서는 실제로 무엇이 일어나는가

좋은 기준은 구현 방법보다 behavior를 말한다. “Redis를 쓴다”보다 “동일 요청을 5초 안에 재전송해도 중복 결제가 발생하지 않는다”가 검증 가능한 acceptance에 가깝다.

### 아주 쉬운 예

로그인 잠금 요구를 단순 함수로 옮긴다.

```python
def locked(failed_attempts):
    return failed_attempts >= 5

assert locked(4) is False
assert locked(5) is True
```

### 한 줄씩 읽기

- 기준 숫자 5가 명시돼 있다.
- 4/5 경계가 acceptance를 실행 가능한 형태로 만든다.
- 실제 시스템에는 시간 window/해제 정책이 추가로 필요하다.

### 직접 실행

1. test를 실행한다.
2. 기준을 3으로 바꾸려면 요구사항과 test를 함께 어떻게 변경할지 적는다.
3. 문장 acceptance와 자동 test 사이 대응을 표시한다.


### 일부를 바꿔서 다시 확인하기

모호한 요구 “보안을 강화한다”를 3개의 관찰 가능한 기준으로 다시 써 본다.

### 작은 문제

“코드가 깔끔해야 한다”를 acceptance criteria 하나로 둘 수 있는가?

### 왜 맞고 왜 틀리는가

유지보수 품질 기준은 필요하지만 사용자 behavior acceptance와는 성격이 다르다. lint/complexity/review 같은 별도 engineering gate로 구체화한다.

### 자주 만나는 실패와 확인 순서

- 서로 충돌하는 acceptance가 있으면 구현 전에 해결한다.
- 기준이 실제 운영 조건을 빼먹지 않았는지 확인한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** acceptance criteria는 완료를 관찰 가능한 behavior로 정의한다.
- **직접 코딩·실행해서 익힐 것:** 문장 기준을 경계값 test로 옮긴다.
- **AI에게 맡겨도 되는 것:** acceptance/test 초안 생성.
- **사람이 최종 확인할 것:** 요구사항의 권위와 누락된 업무 조건을 확인한다.

### 근거 연결

`DORA-2025` · `NIST-SSDF-12`

---

## CHAPTER 02 · LESSON 02 · invariant는 동작 중 항상 깨지면 안 되는 규칙이다

**LESSON ID:** `T10-B09-L02`

### 먼저 쉬운 말로 이해하기

특정 예시의 기대값보다 더 근본적인 규칙이 있다. 계좌 잔액은 허용 정책 아래 음수가 되면 안 된다, 주문 총액은 line item 합과 같아야 한다처럼 여러 경로에서 항상 지켜야 하는 조건을 invariant라고 한다.

### 안에서는 실제로 무엇이 일어나는가

invariant는 test property, runtime assertion, DB constraint로 표현할 수 있다. 한 곳에서만 검사하면 우회 경로가 생길 수 있으므로 어느 계층에서 강제할지 설계한다.

### 아주 쉬운 예

재고가 음수가 되지 않는 작은 domain 함수를 만든다.

```python
def remove_stock(stock, qty):
    if qty < 0 or qty > stock:
        raise ValueError("invalid qty")
    new_stock = stock - qty
    assert new_stock >= 0
    return new_stock
```

### 한 줄씩 읽기

- 입력 validation이 invalid transition을 막는다.
- assertion은 내부 invariant를 다시 표현한다.
- DB 동시성에서는 이 함수만으로 충분하지 않을 수 있다.

### 직접 실행

1. 정상 10→3 제거를 실행한다.
2. stock보다 큰 qty에서 예외가 나는지 test한다.
3. 동시에 두 주문이 들어올 때 invariant를 DB에서도 지켜야 하는 이유를 적는다.


### 일부를 바꿔서 다시 확인하기

property-based test로 어떤 valid stock/qty에서도 결과가 0 이상임을 검사하도록 확장한다.

### 작은 문제

invariant를 발견했으면 UI validation만 넣어도 되는가?

### 왜 맞고 왜 틀리는가

아니다. 다른 client/API 경로가 우회할 수 있다. 신뢰 경계 안에서 domain/DB 수준 강제가 필요할 수 있다.

### 자주 만나는 실패와 확인 순서

- assert를 사용자 입력 validation 대체로 쓰지 않는다. runtime 설정에서 제거될 수 있다.
- invariant가 잘못 정의되면 유효한 업무를 막을 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** invariant는 시스템이 유효한 상태인 동안 항상 지켜야 하는 규칙이다.
- **직접 코딩·실행해서 익힐 것:** 여러 입력/경로에서 invariant를 검증한다.
- **AI에게 맡겨도 되는 것:** property/constraint 후보 생성.
- **사람이 최종 확인할 것:** 업무 규칙의 실제 권위와 강제 계층을 결정한다.

### 근거 연결

`HYPOTHESIS` · `NIST-SSDF-12`

---

## CHAPTER 03 · LESSON 03 · characterization test로 낡은 코드의 현재 행동을 먼저 고정한다

**LESSON ID:** `T10-B09-L03`

### 먼저 쉬운 말로 이해하기

legacy code를 고치려는데 정확한 사양이 없으면 “정리”가 행동을 바꾸는지 알기 어렵다. characterization test는 현재 시스템이 실제로 하는 행동을 먼저 기록해 리팩터링 안전망을 만든다.

### 안에서는 실제로 무엇이 일어나는가

현재 행동이 이상해 보여도 바로 expected를 바꾸지 않는다. 사용자/의존 시스템이 그 행동에 기대고 있을 수 있다. 먼저 관찰하고, 의도된 변경은 별도 requirement로 진행한다.

### 아주 쉬운 예

이상한 rounding 동작을 현재 상태 그대로 test에 잡아 둔다.

```python
def legacy_price(x):
    return int(x * 1.1)

def test_current_behavior():
    assert legacy_price(19) == 20
```

### 한 줄씩 읽기

- test 이름이 “정답”보다 현재 behavior 기록임을 드러낸다.
- float/int truncation이 숨은 규칙일 수 있다.
- 이후 refactor에서 같은 입력이 바뀌면 즉시 알 수 있다.

### 직접 실행

1. 현재 구현 결과를 실제 실행해 expected를 확인한다.
2. 구현을 Decimal로 바꾸기 전에 characterization set을 넓힌다.
3. 업무 담당자와 새로운 rounding 요구를 확인한 뒤 별도 behavior change test를 만든다.


### 일부를 바꿔서 다시 확인하기

운영 로그에서 대표 입력을 개인정보 제거 후 sampling해 characterization case로 만드는 절차를 설계한다.

### 작은 문제

현재 behavior가 버그처럼 보여도 characterization test에 넣는 게 잘못인가?

### 왜 맞고 왜 틀리는가

아니다. 현재 상태를 보존하는 안전망 역할이 있다. 버그 수정은 그 test를 의도적으로 갱신하는 별도 변경으로 다룬다.

### 자주 만나는 실패와 확인 순서

- 우연한 구현 세부까지 전부 고정하면 개선을 어렵게 한다.
- 운영 data를 test fixture로 가져올 때 민감정보를 제거한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** characterization test는 사양이 불명확한 기존 코드의 현재 관찰 행동을 고정한다.
- **직접 코딩·실행해서 익힐 것:** 현재 결과를 먼저 실행해 기록한 뒤 refactor한다.
- **AI에게 맡겨도 되는 것:** legacy case 추출/fixture 생성.
- **사람이 최종 확인할 것:** 보존할 계약과 고칠 결함을 구분한다.

### 근거 연결

`PYTEST` · `NIST-SSDF-12`

---

## CHAPTER 04 · LESSON 04 · refactoring과 behavior change를 한 commit에 섞지 않는다

**LESSON ID:** `T10-B09-L04`

### 먼저 쉬운 말로 이해하기

refactoring은 외부 behavior를 유지하면서 내부 구조를 바꾸는 작업이다. 기능 변경과 동시에 섞으면 test 실패가 구조 변경 때문인지 요구 변경 때문인지 구분하기 어렵다.

### 안에서는 실제로 무엇이 일어나는가

작은 단계로 rename/extract/move를 하고 기존 tests를 계속 green으로 유지한 뒤 behavior change를 별도 diff로 하면 review와 rollback이 쉬워진다. Git history도 의도를 더 잘 보존한다.

### 아주 쉬운 예

중복 계산을 helper로 추출하되 결과는 유지한다.

```python
def line_total(price, qty):
    return price * qty

def order_total(lines):
    return sum(line_total(p, q) for p, q in lines)

assert order_total([(100,2),(50,1)]) == 250
```

### 한 줄씩 읽기

- helper 추출 전후 public 결과 250은 같아야 한다.
- 이 단계에서는 세금/할인 규칙을 추가하지 않는다.
- behavior change는 별도 test와 commit으로 한다.

### 직접 실행

1. 원래 inline 버전 test를 먼저 실행한다.
2. helper 추출 뒤 같은 tests를 실행한다.
3. 그 다음 할인 기능을 별도 change로 추가한다.


### 일부를 바꿔서 다시 확인하기

한 commit에서 파일 이동+대규모 rename+새 기능을 섞었을 때 code review가 왜 어려운지 diff 관점으로 설명한다.

### 작은 문제

refactor commit에서 test가 하나 바뀌어야 한다면 항상 잘못인가?

### 왜 맞고 왜 틀리는가

test가 implementation detail에 묶여 있었다면 test 구조 변경은 가능하다. 다만 외부 behavior expected가 바뀌는지 면밀히 구분해야 한다.

### 자주 만나는 실패와 확인 순서

- format-only 변경을 기능 diff와 섞으면 review noise가 커진다.
- 큰 rename이 blame/history 추적을 어렵게 할 수 있어 단계 분리를 고려한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** refactoring은 외부 behavior를 유지하는 구조 변경이며 기능 변경과 가능한 분리한다.
- **직접 코딩·실행해서 익힐 것:** refactor 전후 같은 test suite를 실행한다.
- **AI에게 맡겨도 되는 것:** 작은 refactor step 제안.
- **사람이 최종 확인할 것:** 실제 behavior가 유지됐는지와 commit 의도를 확인한다.

### 근거 연결

`GIT-DOC` · `DORA-2025`

---

## CHAPTER 05 · LESSON 05 · seam을 만들어 legacy 코드에 테스트 지점을 만든다

**LESSON ID:** `T10-B09-L05`

### 먼저 쉬운 말로 이해하기

낡은 코드가 global DB, 시스템 시간, static client에 강하게 묶여 test하기 어렵다면 한 번에 전부 다시 쓰는 것은 위험하다. 먼저 의존성을 바꿀 수 있는 작은 **seam(이음새)**을 만든다.

### 안에서는 실제로 무엇이 일어나는가

예를 들어 함수 안의 `requests.get` 직접 호출을 `fetch` 인자로 빼면 기존 behavior를 유지하면서 test double을 넣을 수 있다. 이 seam 주변부터 characterization/regression tests를 늘린다.

### 아주 쉬운 예

고정 global 대신 function parameter에 기본값을 두는 간단한 seam을 만든다.

```python
def real_fetch():
    return {"status": "ok"}

def status(fetch=real_fetch):
    return fetch()["status"]

def fake_fetch():
    return {"status": "test"}

assert status(fake_fetch) == "test"
```

### 한 줄씩 읽기

- 기본 production 동작은 real_fetch다.
- test에서는 fake_fetch를 넣을 수 있다.
- 이 작은 경계가 전면 rewrite 없이 testability를 높인다.

### 직접 실행

1. 원래 직접 호출 버전 behavior를 기록한다.
2. seam을 만든 뒤 production default 결과가 같은지 확인한다.
3. fake로 error response를 만들어 recovery path를 test한다.


### 일부를 바꿔서 다시 확인하기

constructor injection, adapter wrapper 등 더 명확한 구조로 점진적으로 옮길 계획을 적는다.

### 작은 문제

legacy code는 지저분하니 tests 없이 전부 새로 쓰는 것이 더 빠르지 않은가?

### 왜 맞고 왜 틀리는가

작아 보이는 시스템도 숨은 계약이 많다. characterization과 incremental seam 없이 rewrite하면 기존 행동을 놓칠 위험이 크다.

### 자주 만나는 실패와 확인 순서

- temporary seam이 영구적인 나쁜 abstraction이 되지 않게 이름/책임을 정리한다.
- real dependency와 fake의 contract drift를 integration test로 보완한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** seam은 기존 동작을 크게 깨지 않고 의존성을 교체/관찰할 수 있게 만든 지점이다.
- **직접 코딩·실행해서 익힐 것:** global/direct call을 작은 주입 경계로 바꿔 test한다.
- **AI에게 맡겨도 되는 것:** adapter/seam 리팩터링 초안.
- **사람이 최종 확인할 것:** 점진 변경이 실제 risk를 줄이는지 확인한다.

### 근거 연결

`PYTEST` · `DORA-2025`

---

## CHAPTER 06 · LESSON 06 · module boundary는 변경 파급 범위를 줄이는 계약이다

**LESSON ID:** `T10-B09-L06`

### 먼저 쉬운 말로 이해하기

파일을 여러 개로 나누는 것만으로 module 설계가 되지는 않는다. module boundary는 바깥이 무엇에 의존할 수 있는지 공개 계약을 정하고 내부 구현을 숨겨 변경 파급을 제한한다.

### 안에서는 실제로 무엇이 일어나는가

좋은 경계는 높은 응집도와 낮은 결합을 지향한다. DB schema detail이 UI까지 새어 나오면 작은 column 변경이 여러 층을 깨뜨린다. adapter/domain/view model 같은 변환 경계가 변화를 흡수할 수 있다.

### 아주 쉬운 예

dict schema를 바로 쓰지 않고 작은 함수로 변환한다.

```python
def to_user_view(row):
    return {"id": row["user_id"], "name": row["display_name"]}

row = {"user_id":1, "display_name":"Kim", "internal_flag":True}
assert to_user_view(row) == {"id":1,"name":"Kim"}
```

### 한 줄씩 읽기

- 내부 column 이름을 public view와 분리한다.
- internal_flag가 외부로 새지 않는다.
- DB schema 변경 시 adapter 한 곳을 우선 수정할 수 있다.

### 직접 실행

1. 실행해 public 결과를 확인한다.
2. 내부 `display_name`을 `name_text`로 바꾸고 adapter만 수정하는 실험을 한다.
3. public contract test가 그대로 통과하는지 본다.


### 일부를 바꿔서 다시 확인하기

경계마다 data copy/translation 비용이 생긴다. 작은 앱에서 과도한 layer를 만들지 않는 trade-off를 적는다.

### 작은 문제

“레이어가 많을수록 좋은 architecture”인가?

### 왜 맞고 왜 틀리는가

아니다. 변경 이유가 다른 책임을 분리할 만큼만 경계를 둔다. 의미 없는 wrapper는 복잡도만 늘린다.

### 자주 만나는 실패와 확인 순서

- module cycle이 생기면 경계 방향이 흐려질 수 있다.
- 내부 model을 API response로 그대로 노출하면 보안/호환성 문제가 생길 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** module boundary는 공개 계약과 내부 구현을 분리해 변경 파급을 통제한다.
- **직접 코딩·실행해서 익힐 것:** 내부 schema 변경이 adapter 한 곳에 머무는지 test한다.
- **AI에게 맡겨도 되는 것:** dependency graph와 boundary 후보 생성.
- **사람이 최종 확인할 것:** 추상화가 실제 변경 이유를 분리하는지 판단한다.

### 근거 연결

`DORA-2025` · `NIST-SSDF-12`

---

## CHAPTER 07 · LESSON 07 · 기술 부채는 “더러운 코드”가 아니라 미래 변경 비용으로 기록한다

**LESSON ID:** `T10-B09-L07`

### 먼저 쉬운 말로 이해하기

기술 부채를 감정적으로 “코드가 별로다”라고 부르면 우선순위를 잡기 어렵다. 어떤 변경을 할 때 시간이 더 들고, 어떤 실패 위험이 커지는지 구체적으로 기록해야 한다.

### 안에서는 실제로 무엇이 일어나는가

예: tests가 없어 결제 수정 때 수동 회귀 3시간, 공용 global state 때문에 병렬 test 불가, dependency가 EOL이라 보안 patch 불가. 이런 비용과 risk를 backlog에 연결한다.

### 아주 쉬운 예

작은 debt register를 data로 표현한다.

```python
debts = [
    {"item":"global clock", "cost":"flaky date tests", "risk":"medium"},
    {"item":"no migration rollback", "cost":"slow recovery", "risk":"high"},
]
for d in debts:
    print(d["risk"], d["item"], "->", d["cost"])
```

### 한 줄씩 읽기

- item 자체보다 결과 cost/risk를 적는다.
- 모든 debt를 즉시 제거하지 않고 변경 계획과 연결한다.
- 고위험 debt는 release gate와 연결할 수 있다.

### 직접 실행

1. 자기 프로젝트의 debt 3개를 같은 형식으로 적는다.
2. 각 debt에 발생 조건과 증거를 하나 붙인다.
3. “언젠가 rewrite” 대신 작은 상환 step을 만든다.


### 일부를 바꿔서 다시 확인하기

새 기능 하나를 만들면서 동시에 갚을 수 있는 debt와 별도 project가 필요한 debt를 구분한다.

### 작은 문제

코드가 오래됐다는 이유만으로 기술 부채 점수가 높은가?

### 왜 맞고 왜 틀리는가

아니다. 실제 변경 비용·장애 위험·지원 종료 같은 근거가 중요하다. 오래됐어도 안정적이고 잘 격리돼 있으면 우선순위가 낮을 수 있다.

### 자주 만나는 실패와 확인 순서

- debt 목록을 끝없이 늘리고 ownership/기한 없이 방치하지 않는다.
- “새 기술로 바꾸고 싶다”를 debt라는 이름으로 포장하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 기술 부채는 미래 변경/운영 비용과 위험으로 구체화한다.
- **직접 코딩·실행해서 익힐 것:** debt item에 증거·영향·작은 상환 단계를 붙인다.
- **AI에게 맡겨도 되는 것:** debt register 정리.
- **사람이 최종 확인할 것:** 사업 영향과 실제 risk를 기준으로 우선순위를 결정한다.

### 근거 연결

`DORA-2025` · `NIST-SSDF-12`

---
