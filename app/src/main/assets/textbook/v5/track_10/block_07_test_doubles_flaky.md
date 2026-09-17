# TRACK 10 · 오류·테스트·Git·빌드·배포 — 변경을 안전하게 만드는 기술

# BLOCK 07 · 결정적 테스트·대역·flaky 제어

좋은 테스트는 같은 조건에서 같은 판단을 내리고, 외부 시스템 때문에 흔들리지 않아야 한다. 이 BLOCK은 test double, dependency injection, clock/random 통제, fixture isolation, flaky 진단, 순서·병렬성까지 다룬다.


```text
개념 설명 → 아주 쉬운 예 → 한 줄씩 해석 → 직접 실행 → 일부 수정 → 작은 문제 → 왜 맞고 틀렸는지 설명
```

---

## CHAPTER 01 · LESSON 01 · test double은 진짜 의존성을 통제된 대역으로 바꾼다

**LESSON ID:** `T10-B07-L01`

### 먼저 쉬운 말로 이해하기

테스트에서 매번 실제 결제 서버·이메일·시계를 호출하면 느리고 위험하며 결과가 흔들릴 수 있다. 그래서 검사 대상 밖의 의존성을 **통제 가능한 대역**으로 바꾼다. 이 대역을 넓게 test double이라고 부른다.

### 안에서는 실제로 무엇이 일어나는가

fake, stub, mock이라는 이름보다 먼저 목적을 본다. “정해진 값을 돌려주기”, “간단한 메모리 구현으로 동작하기”, “특정 호출이 있었는지 검증하기”처럼 필요한 통제 수준이 다르다. test double은 실제 시스템을 완전히 대신하는 증거가 아니라 특정 단위를 고립하는 도구다.

### 아주 쉬운 예

실제 환율 API 대신 정해진 환율을 반환하는 함수를 넣는다.

```python
def total_krw(usd, rate_provider):
    return usd * rate_provider()

def fixed_rate():
    return 1300

def test_total_krw():
    assert total_krw(10, fixed_rate) == 13000
```

### 한 줄씩 읽기

- `rate_provider` 경계를 함수 인자로 받는다.
- `fixed_rate`는 network 없이 1300을 반환한다.
- test는 환율 계산 logic에 집중한다.

### 직접 실행

1. pytest로 test를 실행한다.
2. fixed_rate를 1400으로 바꿔 expected도 요구사항에 맞게 조정한다.
3. provider가 예외를 던지는 실패 path test를 별도로 만든다.


### 일부를 바꿔서 다시 확인하기

실제 API 응답 shape가 바뀌었을 때 fake만으로는 잡지 못한다. 별도의 integration/contract test가 왜 필요한지 한 줄로 설명한다.

### 작은 문제

fake test가 모두 green이면 실제 결제 서버 연결도 정상이라고 말해도 되는가?

### 왜 맞고 왜 틀리는가

안 된다. fake는 내부 logic 범위의 증거다. 실제 endpoint, auth, schema, timeout은 integration evidence가 필요하다.

### 자주 만나는 실패와 확인 순서

- fake가 실제 계약과 오래 떨어져 drift할 수 있다.
- 실제 부작용을 막기 위해 production credential을 unit test에 넣지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** test double은 외부 의존성을 통제해 특정 범위를 고립한다.
- **직접 코딩·실행해서 익힐 것:** 정상/예외를 반환하는 대역을 직접 만들어 test한다.
- **AI에게 맡겨도 되는 것:** fake/stub/mock 초안 생성.
- **사람이 최종 확인할 것:** 대역이 무엇을 증명하지 못하는지와 실제 계약 검증 범위를 확인한다.

### 근거 연결

`PYTEST` · `NIST-SSDF-12`

---

## CHAPTER 02 · LESSON 02 · stub과 mock을 “값 제공”과 “상호작용 검증”으로 구분한다

**LESSON ID:** `T10-B07-L02`

### 먼저 쉬운 말로 이해하기

초보 단계에서는 이름을 외우기보다 질문을 구분하면 된다. **stub 성격**의 대역은 test에 필요한 값을 공급하고, **mock 성격**의 대역은 어떤 호출이 일어났는지를 검사한다.

### 안에서는 실제로 무엇이 일어나는가

상호작용 검증은 이메일이 실제로 전송됐는지보다 “메일 전송 경계가 한 번 호출됐는지” 확인할 때 유용하다. 하지만 내부 함수 호출 횟수까지 모두 mock하면 구현 변화에 지나치게 민감해질 수 있다.

### 아주 쉬운 예

간단한 callable 객체로 호출 수를 기록한다.

```python
class MailerSpy:
    def __init__(self): self.sent = []
    def send(self, address, text): self.sent.append((address, text))

def welcome(address, mailer):
    mailer.send(address, "welcome")

mailer = MailerSpy()
welcome("a@example.com", mailer)
assert mailer.sent == [("a@example.com", "welcome")]
```

### 한 줄씩 읽기

- MailerSpy는 실제 이메일을 보내지 않는다.
- `sent` list에 호출 argument를 기록한다.
- assertion은 중요한 side-effect boundary를 검사한다.

### 직접 실행

1. 스크립트 또는 pytest로 실행한다.
2. welcome을 두 번 호출해 기록이 두 개 되는지 본다.
3. 요구사항이 “중복 발송 금지”라면 idempotency test로 확장한다.


### 일부를 바꿔서 다시 확인하기

내부 formatter 함수 호출 횟수까지 assert하는 test를 하나 작성한 뒤 formatter를 inline하면 왜 불필요하게 깨지는지 설명한다.

### 작은 문제

mock 호출 assertion이 많을수록 test가 정밀해지는가?

### 왜 맞고 왜 틀리는가

반드시 그렇지 않다. 소비자가 의존하는 상호작용만 검사해야 한다. 내부 구현 순서까지 고정하면 refactoring을 방해한다.

### 자주 만나는 실패와 확인 순서

- mock framework 설정 오류가 실제 코드보다 test를 복잡하게 만들 수 있다.
- 메일·결제 같은 외부 부작용은 test 계정/환경을 분리한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** stub은 통제된 값을 주고 mock/spy는 중요한 호출 상호작용을 관찰하는 데 쓸 수 있다.
- **직접 코딩·실행해서 익힐 것:** side-effect 경계를 대역으로 바꾸고 argument를 검사한다.
- **AI에게 맡겨도 되는 것:** mock 설정과 verification 코드 생성.
- **사람이 최종 확인할 것:** 어떤 상호작용이 실제 제품 계약인지 결정한다.

### 근거 연결

`PYTEST`

---

## CHAPTER 03 · LESSON 03 · dependency injection으로 대역을 끼울 자리를 만든다

**LESSON ID:** `T10-B07-L03`

### 먼저 쉬운 말로 이해하기

함수 안에서 직접 `new Client()`를 만들거나 global 객체를 꺼내면 test가 그 의존성을 바꾸기 어렵다. dependency injection은 필요한 객체를 바깥에서 받아 **교체 가능한 경계**를 만든다.

### 안에서는 실제로 무엇이 일어나는가

주입 방식은 생성자, 함수 인자, interface 등 여러 형태가 있다. 목적은 framework를 쓰는 것이 아니라 business logic과 I/O 구현을 결합하지 않는 것이다.

### 아주 쉬운 예

저장소 함수를 인자로 받는 작은 서비스 함수를 만든다.

```python
def create_user(name, save_user):
    user = {"name": name.strip()}
    return save_user(user)

def fake_save(user):
    return {**user, "id": 1}

print(create_user(" Kim ", fake_save))
```

### 한 줄씩 읽기

- service는 이름 정규화와 save 호출을 담당한다.
- 실제 DB 함수 대신 fake_save를 주입할 수 있다.
- production에서는 같은 자리에 real repository 함수를 넣는다.

### 직접 실행

1. 실행해 id=1 결과를 확인한다.
2. fake_save가 duplicate error를 내도록 바꿔 service의 error 정책을 test한다.
3. service가 SQL 세부사항을 직접 알지 않게 유지한다.


### 일부를 바꿔서 다시 확인하기

global DB client를 직접 사용하는 버전과 비교해 어떤 test 준비가 더 어려운지 적는다.

### 작은 문제

모든 작은 함수에 dependency를 인자로 넣으면 무조건 좋은가?

### 왜 맞고 왜 틀리는가

아니다. 외부 변경 가능성/side effect가 있는 경계에서 가치가 크다. 단순 순수 계산까지 과도하게 추상화하면 복잡도만 늘 수 있다.

### 자주 만나는 실패와 확인 순서

- service locator/global patching에만 의존하면 test 순서 오염이 생길 수 있다.
- dependency lifetime(singleton/request scope)을 잘못 잡으면 상태가 공유될 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** DI는 의존성을 바깥에서 받아 실제 구현과 대역을 교체 가능하게 만든다.
- **직접 코딩·실행해서 익힐 것:** real/fake implementation을 같은 경계에 넣어 본다.
- **AI에게 맡겨도 되는 것:** interface/constructor 초안 생성.
- **사람이 최종 확인할 것:** 추상화 비용과 실제 교체 필요성을 판단한다.

### 근거 연결

`DORA-2025` · `PYTEST`

---

## CHAPTER 04 · LESSON 04 · 시간·난수·UUID를 테스트에서 고정한다

**LESSON ID:** `T10-B07-L04`

### 먼저 쉬운 말로 이해하기

현재 시각, random 값, UUID는 실행마다 달라질 수 있다. 이런 값이 test expected에 직접 들어가면 같은 코드도 매번 다른 결과가 되어 flaky해진다.

### 안에서는 실제로 무엇이 일어나는가

clock/random/id generator를 함수나 객체로 감싸 주입하면 test에서 deterministic 값을 제공할 수 있다. production에서는 진짜 generator를 사용한다.

### 아주 쉬운 예

주문 번호 생성기를 주입한다.

```python
def make_order(item, id_gen):
    return {"id": id_gen(), "item": item}

def fixed_id():
    return "order-001"

def test_make_order():
    assert make_order("book", fixed_id) == {"id":"order-001", "item":"book"}
```

### 한 줄씩 읽기

- ID 생성이 business function 바깥 경계로 이동했다.
- test는 항상 같은 ID를 받는다.
- production UUID의 uniqueness는 별도 test/라이브러리 계약 문제다.

### 직접 실행

1. pytest로 실행한다.
2. fixed_id가 같은 값만 내므로 두 주문의 id 중복 문제를 일부러 만든다.
3. 중복 금지가 service 책임인지 generator 계약인지 구분한다.


### 일부를 바꿔서 다시 확인하기

sequence generator를 만들어 `order-001`, `order-002`를 순서대로 반환하게 하고 두 호출 expected를 검사한다.

### 작은 문제

test에서 production random generator를 그대로 쓰고 실패하면 retry 3회하도록 만들었다. 좋은 해결인가?

### 왜 맞고 왜 틀리는가

아니다. 확률적 실패를 숨긴다. test 입력을 고정하거나 property-based 방식으로 seed/reproduction을 보존해야 한다.

### 자주 만나는 실패와 확인 순서

- 보안 token 생성기를 deterministic fake로 바꾼 코드를 production에 실수로 주입하지 않는다.
- 시간 mocking이 timezone 의미를 잃지 않게 한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 비결정 입력은 test에서 통제 가능한 경계로 만든다.
- **직접 코딩·실행해서 익힐 것:** 고정/순차 generator를 넣어 반복 가능한 결과를 만든다.
- **AI에게 맡겨도 되는 것:** fake clock/random/id generator 생성.
- **사람이 최종 확인할 것:** production과 test wiring이 분리돼 있는지 확인한다.

### 근거 연결

`PYTEST` · `HYPOTHESIS`

---

## CHAPTER 05 · LESSON 05 · fixture와 teardown으로 테스트 간 상태를 격리한다

**LESSON ID:** `T10-B07-L05`

### 먼저 쉬운 말로 이해하기

한 test가 만든 파일·DB row·global 값을 다음 test가 그대로 보면 실행 순서에 따라 결과가 바뀐다. 각 test가 필요한 상태를 만들고 끝나면 정리해 **독립 실행 가능**하게 해야 한다.

### 안에서는 실제로 무엇이 일어나는가

fixture는 반복되는 준비를 명시적으로 제공한다. pytest fixture의 scope를 넓히면 속도는 좋아질 수 있지만 상태 공유 위험이 커진다. teardown/finalizer는 실패하더라도 정리가 수행되게 설계한다.

### 아주 쉬운 예

`tmp_path` fixture를 사용하면 test마다 임시 디렉터리를 받을 수 있다.

```python
def test_write_file(tmp_path):
    p = tmp_path / "note.txt"
    p.write_text("hello", encoding="utf-8")
    assert p.read_text(encoding="utf-8") == "hello"
```

### 한 줄씩 읽기

- pytest가 test별 임시 경로를 제공한다.
- 실제 사용자 파일을 건드리지 않는다.
- test 종료 뒤 fixture lifecycle에 따라 정리된다.

### 직접 실행

1. pytest로 실행한다.
2. 같은 파일명을 쓰는 test를 하나 더 추가한다.
3. 두 test가 서로 파일 내용을 덮지 않는지 확인한다.


### 일부를 바꿔서 다시 확인하기

global list를 공유하는 두 test를 일부러 만들고 순서를 바꾸면 깨지는 사례를 만든 뒤 fixture로 독립화한다.

### 작은 문제

test suite를 전체로 돌릴 때만 실패하고 단독 실행은 성공한다. 가장 먼저 무엇을 의심할까?

### 왜 맞고 왜 틀리는가

공유 상태, 실행 순서 의존, 전역 patch 복원 실패, 공용 DB/file fixture를 우선 조사한다.

### 자주 만나는 실패와 확인 순서

- session-scope fixture가 mutable state를 공유할 수 있다.
- teardown에서 production data를 삭제하지 않도록 환경 격리를 보장한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** test는 가능한 한 순서와 다른 test 상태에 독립적이어야 한다.
- **직접 코딩·실행해서 익힐 것:** 같은 resource 이름을 쓰는 test를 격리해 실행한다.
- **AI에게 맡겨도 되는 것:** fixture/cleanup 코드 생성.
- **사람이 최종 확인할 것:** scope와 정리 대상이 안전한지 확인한다.

### 근거 연결

`PYTEST`

---

## CHAPTER 06 · LESSON 06 · flaky test를 재시도로 숨기지 말고 원인을 분류한다

**LESSON ID:** `T10-B07-L06`

### 먼저 쉬운 말로 이해하기

같은 commit에서 pass와 fail이 오가는 test를 flaky test라고 한다. 재실행해서 green이 되었다고 해결된 것은 아니다. flaky는 CI 신뢰를 깎아 진짜 regression도 “또 흔들렸겠지”라고 무시하게 만든다.

### 안에서는 실제로 무엇이 일어나는가

원인은 timing, async 완료 대기 부족, random, test order, shared resource, network, clock, resource exhaustion 등으로 분류할 수 있다. 실패 시 seed·duration·worker·환경을 남기고 재현율을 측정한다.

### 아주 쉬운 예

sleep 대신 조건을 기다리는 사고를 단순한 함수로 표현한다.

```python
def ready_after(attempt):
    return attempt >= 3

for attempt in range(1, 6):
    if ready_after(attempt):
        print("ready", attempt)
        break
```

### 한 줄씩 읽기

- 고정 1초 sleep 대신 실제 완료 조건을 확인하는 방향을 보여 준다.
- 최대 시도/timeout이 있어 무한 대기를 피해야 한다.
- 실제 async test에서는 framework의 await/event primitive를 사용한다.

### 직접 실행

1. 실행해 3번째에 ready가 되는지 본다.
2. 조건을 절대 true가 안 되게 바꾸고 timeout/failure 경로를 추가한다.
3. 실제 flaky test 하나를 원인 category로 분류하는 연습을 한다.


### 일부를 바꿔서 다시 확인하기

`rerun=3` 설정은 진단 중 임시 완화가 될 수 있지만 실패를 gate에서 완전히 숨기지 않도록 최초 실패율을 별도 metric으로 남기는 설계를 적는다.

### 작은 문제

10번 중 1번 실패하는 test를 CI에서 자동 5회 재시도해 항상 green으로 만들면 품질이 좋아진 것인가?

### 왜 맞고 왜 틀리는가

아니다. 신호를 숨긴 것이다. 원인 제거와 flaky rate 추적이 필요하다.

### 자주 만나는 실패와 확인 순서

- 불안정한 외부 sandbox API에 직접 의존하면 suite 신뢰가 떨어진다.
- parallel test에서 동일 port/file/db row를 공유하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** flaky test는 같은 조건에서 결과가 불안정한 test이며 재시도는 원인 해결이 아니다.
- **직접 코딩·실행해서 익힐 것:** 반복 실행과 상태 기록으로 재현율을 측정한다.
- **AI에게 맡겨도 되는 것:** flaky classification과 반복 harness 생성.
- **사람이 최종 확인할 것:** retry가 신호를 숨기는지, 원인 제거 계획이 있는지 확인한다.

### 근거 연결

`PYTEST` · `DORA-2025`

---

## CHAPTER 07 · LESSON 07 · 테스트 순서와 병렬 실행을 바꿔 숨은 결합을 찾는다

**LESSON ID:** `T10-B07-L07`

### 먼저 쉬운 말로 이해하기

test A가 먼저 실행되어야만 test B가 성공한다면 B는 독립적이지 않다. 순서 randomization이나 병렬 실행은 이런 숨은 결합을 드러내는 진단 도구가 될 수 있다.

### 안에서는 실제로 무엇이 일어나는가

공유 DB row, global cache, environment variable, fixed port는 대표적인 충돌 지점이다. 병렬화는 suite를 빠르게 할 수 있지만 isolation이 먼저다.

### 아주 쉬운 예

전역 list를 공유하는 잘못된 예를 본다.

```python
state = []

def test_a():
    state.append("ready")
    assert "ready" in state

def test_b():
    assert "ready" in state  # A가 먼저라는 숨은 가정
```

### 한 줄씩 읽기

- test_b는 자기 Arrange 단계가 없다.
- 단독 실행하면 실패할 수 있다.
- suite order가 우연히 A→B일 때만 green이 될 수 있다.

### 직접 실행

1. test_b만 실행해 실패를 확인한다.
2. 각 test가 자기 state를 만들도록 수정한다.
3. 둘을 어떤 순서로 실행해도 통과하는지 확인한다.


### 일부를 바꿔서 다시 확인하기

파일 이름, port, database schema에 worker ID를 붙이는 방식 등 parallel isolation 전략을 설계한다.

### 작은 문제

병렬 실행에서만 실패하면 production 동시성 bug라고 바로 볼 수 있는가?

### 왜 맞고 왜 틀리는가

아니다. test resource 충돌일 수도 있다. production code race와 test harness interference를 구분해야 한다.

### 자주 만나는 실패와 확인 순서

- 환경변수 monkeypatch 복원이 누락되면 뒤 test가 오염된다.
- parallel worker가 같은 external rate limit을 공유할 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** test는 실행 순서와 다른 test의 잔여 상태에 의존하지 않는 것이 원칙이다.
- **직접 코딩·실행해서 익힐 것:** 단독/순서 변경 실행으로 숨은 결합을 드러낸다.
- **AI에게 맡겨도 되는 것:** isolation strategy와 unique resource name 생성.
- **사람이 최종 확인할 것:** 병렬화가 실제로 안전한지와 shared dependency 제한을 확인한다.

### 근거 연결

`PYTEST`

---
