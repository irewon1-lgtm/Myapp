# TRACK 10 · 오류·테스트·Git·빌드·배포 — 변경을 안전하게 만드는 기술

# BLOCK 04 · 가설 기반 디버깅과 변화점 추적

증거가 모이면 원인 후보를 효율적으로 줄여야 한다. 이 BLOCK은 가설 트리, binary isolation, 정상/실패 차이, git bisect, instrumentation, Heisenbug, regression test까지 이어진다.


```text
개념 설명 → 아주 쉬운 예 → 한 줄씩 해석 → 직접 실행 → 일부 수정 → 작은 문제 → 왜 맞고 틀렸는지 설명
```

---

## CHAPTER 01 · LESSON 01 · 가설 트리로 원인 후보를 계층화한다

**LESSON ID:** `T10-B04-L01`

### 먼저 쉬운 말로 이해하기

복잡한 장애에서는 원인 후보가 수십 개가 될 수 있다. 머릿속으로만 생각하면 이미 본 후보를 반복하거나, 눈에 띄는 한 가지에 집착한다. 가설 트리는 “어느 층에서 실패할 수 있는가”를 나눠 조사 순서를 만든다.

### 안에서는 실제로 무엇이 일어나는가

예를 들어 API 500 오류는 client 입력, routing, validation, business logic, DB, 외부 API, serialization 같은 층으로 나눌 수 있다. 각 branch에는 그 branch를 빠르게 배제하거나 지지하는 증거를 붙인다. 좋은 첫 실험은 한 번에 큰 가지를 제거한다.

### 아주 쉬운 예

파일 처리 실패를 입력·변환·출력 세 단계로 쪼개 본다.

```python
def transform(text):
    return text.strip().upper()

def pipeline(raw):
    parsed = raw.decode("utf-8")
    changed = transform(parsed)
    return changed.encode("utf-8")

print(pipeline(b" hello "))
```

### 한 줄씩 읽기

- decode가 입력 해석 단계다.
- transform은 내부 business 변환 단계다.
- encode는 출력 직렬화 단계다.

### 직접 실행

1. 정상 입력을 실행한다.
2. `b"\xff"`를 넣어 입력 해석 branch에서 실패를 만든다.
3. 가설 트리에 `decode / transform / encode` 세 가지를 적고 현재 증거가 어느 branch를 가리키는지 표시한다.


### 일부를 바꿔서 다시 확인하기

`transform`이 숫자만 거부하도록 바꿔 다른 branch 실패를 만든다. 같은 “pipeline 실패” 증상이 서로 다른 단계에서 나올 수 있음을 확인한다.

### 작은 문제

원인 후보가 20개다. 무조건 하나씩 순서대로 시험하는 것보다 좋은 전략은 무엇인가?

### 왜 맞고 왜 틀리는가

한 실험으로 여러 후보를 나눌 수 있는 경계부터 확인한다. 예를 들어 DB 호출 전까지 정상인지 확인하면 client/routing/business 일부와 DB 이후 후보를 크게 분리할 수 있다.

### 자주 만나는 실패와 확인 순서

- 가설 트리를 원인 목록으로만 쓰고 각 branch에 판별 실험을 붙이지 않으면 진전이 없다.
- 가능성이 낮아 보인다는 이유만으로 증거 없이 branch를 삭제하면 안 된다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 가설 트리는 원인 후보를 계층으로 나누고 판별 실험을 붙이는 도구다.
- **직접 코딩·실행해서 익힐 것:** 한 관측 지점으로 큰 branch를 나누는 실험을 설계한다.
- **AI에게 맡겨도 되는 것:** 원인 후보 분류와 질문 초안.
- **사람이 최종 확인할 것:** 실제 시스템 경계와 비용을 고려해 조사 순서를 결정한다.

### 근거 연결

`GOOGLE-SRE` · `DORA-2025`

---

## CHAPTER 02 · LESSON 02 · binary isolation으로 실패 구간을 절반씩 줄인다

**LESSON ID:** `T10-B04-L02`

### 먼저 쉬운 말로 이해하기

긴 처리 경로를 처음부터 한 줄씩 따라가면 느리다. 중간 지점에서 상태가 정상인지 검사해 실패가 앞쪽인지 뒤쪽인지 나누면 조사 범위를 빠르게 줄일 수 있다.

### 안에서는 실제로 무엇이 일어나는가

binary search와 같은 사고방식이지만 코드 줄 수를 기계적으로 반으로 나눈다는 뜻은 아니다. 의미 있는 단계 경계를 골라 입력/출력을 검사하고, 이상이 있는 쪽을 다시 나눈다.

### 아주 쉬운 예

여러 변환이 이어진 문자열 pipeline에서 중간값을 본다.

```python
def step1(s): return s.strip()
def step2(s): return s.lower()
def step3(s): return s.replace("-", "_")
def step4(s): return s[:5]

v1 = step1("  ABC-DEF  ")
v2 = step2(v1)
print("mid", v2)
v3 = step3(v2)
v4 = step4(v3)
print("final", v4)
```

### 한 줄씩 읽기

- `v2`는 네 단계 중간의 관찰 지점이다.
- mid가 이미 틀리면 step1~2를 본다.
- mid가 맞고 final이 틀리면 step3~4로 범위가 줄어든다.

### 직접 실행

1. 원래 결과를 실행한다.
2. step3를 일부러 잘못 바꿔 final만 틀리게 만든다.
3. mid 출력이 정상인 것을 확인해 앞 절반을 우선 제외한다.


### 일부를 바꿔서 다시 확인하기

중간 지점을 v1 또는 v3로 옮겨 남은 범위를 더 줄인다. 로그를 영구적으로 남길 필요가 없다면 임시 instrumentation은 수정 후 제거한다.

### 작은 문제

중간값이 정상이라고 앞쪽 코드가 완전히 무결하다고 말해도 되는가?

### 왜 맞고 왜 틀리는가

그 특정 입력에 대해 그 중간 계약을 만족했다는 뜻이다. 다른 입력/side effect/성능 문제까지 무결하다는 의미는 아니다.

### 자주 만나는 실패와 확인 순서

- 중간 로그가 민감 데이터를 출력하지 않게 한다.
- 관찰 코드 자체가 timing을 바꾸는 동시성 bug에서는 주의한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** binary isolation은 의미 있는 중간 경계로 원인 공간을 빠르게 줄인다.
- **직접 코딩·실행해서 익힐 것:** pipeline 중간값을 직접 검사해 실패 구간을 좁힌다.
- **AI에게 맡겨도 되는 것:** 관찰 지점 후보 제안.
- **사람이 최종 확인할 것:** 관찰이 실제 계약을 대표하는지와 부작용을 확인한다.

### 근거 연결

`GIT-BISECT` · `GOOGLE-SRE`

---

## CHAPTER 03 · LESSON 03 · 정상 사례와 실패 사례의 차이를 비교한다

**LESSON ID:** `T10-B04-L03`

### 먼저 쉬운 말로 이해하기

한 사용자는 되고 다른 사용자는 안 될 때, 두 사례를 따로 보는 것보다 **차이**를 표로 만들면 강력하다. 이것을 differential debugging의 한 형태로 볼 수 있다.

### 안에서는 실제로 무엇이 일어나는가

정상/실패 사례에서 버전, 입력 길이, 권한, locale, feature flag, dependency 응답 같은 차이를 나열한다. 그중 하나만 바꾸어 실패가 따라 움직이는지 확인하면 인과 후보가 강해진다.

### 아주 쉬운 예

정상 문자열과 실패 문자열의 길이·문자 종류를 비교한다.

```python
good = "alice"
bad = "alice😀"

for name, value in [("good", good), ("bad", bad)]:
    print(name, len(value), value.isascii(), value.encode("utf-8"))
```

### 한 줄씩 읽기

- 문자 개수와 byte 길이는 다를 수 있다.
- `isascii()`는 두 입력의 구조적 차이를 드러낸다.
- 실패를 “emoji 때문”이라고 단정하기 전에 실제 저장/전송 경계에서 실험해야 한다.

### 직접 실행

1. 두 입력의 출력 차이를 확인한다.
2. bad의 emoji를 한글로 바꿔 ASCII 여부와 byte 길이를 다시 본다.
3. 어떤 차이가 실패와 함께 움직이는지 표를 만든다.


### 일부를 바꿔서 다시 확인하기

길이는 같고 encoding만 다른 입력, encoding은 같고 길이만 다른 입력을 만들어 두 가설을 분리한다.

### 작은 문제

정상 계정과 실패 계정의 유일한 차이가 premium flag다. 이것으로 원인이 확정되는가?

### 왜 맞고 왜 틀리는가

상관관계일 뿐이다. flag를 통제한 실험이나 해당 분기 로그로 실제로 실행 경로가 달라지는지 확인해야 한다.

### 자주 만나는 실패와 확인 순서

- 비교 대상이 너무 많은 차이를 동시에 가지면 false lead가 많아진다.
- 실패 사용자의 실제 개인정보를 비교표에 그대로 넣지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 정상과 실패 사례의 차이를 후보로 만들고 하나씩 통제한다.
- **직접 코딩·실행해서 익힐 것:** 입력 특성을 표로 비교하고 한 변수씩 바꾼다.
- **AI에게 맡겨도 되는 것:** diff 표 자동 생성.
- **사람이 최종 확인할 것:** 차이가 인과인지 단순 상관인지 실험으로 확인한다.

### 근거 연결

`GOOGLE-SRE` · `NIST-SSDF-12`

---

## CHAPTER 04 · LESSON 04 · git bisect로 버그를 넣은 commit 범위를 찾는다

**LESSON ID:** `T10-B04-L04`

### 먼저 쉬운 말로 이해하기

어제는 정상, 오늘은 실패하고 그 사이 commit이 많다면 모든 commit을 눈으로 읽을 필요가 없다. `git bisect`는 알려진 good commit과 bad commit 사이를 이진 탐색해 변화가 생긴 commit을 좁힌다.

### 안에서는 실제로 무엇이 일어나는가

Git은 중간 commit을 checkout하고 사용자가 good/bad를 표시하도록 한다. 반복하면 후보 범위가 절반씩 줄어든다. 자동 테스트가 있으면 `git bisect run`으로 판정을 자동화할 수도 있다.

### 아주 쉬운 예

실제 연습은 별도 임시 repository에서 세 commit을 만들고 두 번째 변화가 실패를 도입하게 구성한다.

```bash
# 개념 명령
# git bisect start
# git bisect bad HEAD
# git bisect good <known-good-sha>
# ... 테스트 후 git bisect good 또는 bad
# git bisect reset
```

### 한 줄씩 읽기

- `start`는 bisect session을 연다.
- 현재 실패 commit을 bad로 표시한다.
- 과거 정상 commit을 good으로 표시하면 Git이 중간 후보를 선택한다.
- 끝나면 `reset`으로 원래 branch 위치로 돌아온다.

### 직접 실행

1. 이 패키지의 `tests/git_bisect_demo.sh`를 실행한다.
2. 스크립트가 임시 repo를 만들고 실제 `git bisect run`을 수행하는지 확인한다.
3. 출력에서 first bad commit과 의도한 commit message를 비교한다.


### 일부를 바꿔서 다시 확인하기

테스트 판정 명령을 바꿔 exit code 0=good, 1~127(125 제외)=bad라는 자동화 의미를 확인한다. build 불가처럼 판정 불능 commit은 skip 전략이 필요할 수 있다.

### 작은 문제

bisect가 지목한 commit 하나를 바로 “root cause”라고 불러도 되는가?

### 왜 맞고 왜 틀리는가

그 commit에서 관찰한 property가 처음 바뀌었다는 강한 증거다. 하지만 그 commit의 어떤 줄/환경 변화가 실제 원인인지 추가 조사해야 한다.

### 자주 만나는 실패와 확인 순서

- flaky test를 bisect 판정기로 쓰면 잘못된 commit을 지목할 수 있다.
- public history에서 과거 commit을 직접 수정하는 대신 bisect는 읽기 중심 조사로 사용한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** git bisect는 good/bad 경계 사이에서 변화가 처음 나타난 commit을 이진 탐색한다.
- **직접 코딩·실행해서 익힐 것:** 임시 repo에서 bisect run을 실제 실행한다.
- **AI에게 맡겨도 되는 것:** bisect 명령 구성과 후보 diff 요약.
- **사람이 최종 확인할 것:** 판정 테스트가 deterministic한지와 지목 commit의 실제 원인을 확인한다.

### 근거 연결

`GIT-BISECT` · `GIT-DOC`

---

## CHAPTER 05 · LESSON 05 · instrumentation은 가설을 측정 가능한 값으로 바꾼다

**LESSON ID:** `T10-B04-L05`

### 먼저 쉬운 말로 이해하기

“아마 DB가 느리다”는 가설만으로는 부족하다. DB 호출 전후 시간을 재면 latency라는 숫자가 생긴다. instrumentation은 실행에 관측 코드를 넣어 가설을 검증 가능한 evidence로 바꾸는 행위다.

### 안에서는 실제로 무엇이 일어나는가

좋은 instrumentation은 필요한 신호를 최소 오버헤드로 모은다. 시작/종료 시각, count, error code, queue depth 등이 예다. 측정 자체가 성능을 바꿀 수 있으므로 overhead와 sampling을 고려한다.

### 아주 쉬운 예

함수 실행 시간을 `perf_counter`로 잰다.

```python
from time import perf_counter, sleep

start = perf_counter()
sleep(0.02)
elapsed_ms = (perf_counter() - start) * 1000
print(round(elapsed_ms, 1))
```

### 한 줄씩 읽기

- `perf_counter`는 경과 시간 측정에 적합한 monotonic 고해상도 clock이다.
- sleep 20ms는 관찰 가능한 지연을 의도적으로 만든다.
- 한 번의 측정만으로 성능 결론을 내리면 안 된다.

### 직접 실행

1. 스크립트를 여러 번 실행해 값 분산을 본다.
2. sleep을 0.01로 바꿔 중앙 경향이 줄어드는지 본다.
3. 측정 코드를 실제 느린 구간 앞뒤에만 두는 설계를 적는다.


### 일부를 바꿔서 다시 확인하기

10번 반복해 최소/중앙/최대값을 출력하도록 수정한다. 측정 단위와 warm-up, 시스템 부하가 결과에 미치는 영향도 기록한다.

### 작은 문제

로그를 더 많이 넣었더니 버그가 사라졌다. 무엇을 의심해야 하는가?

### 왜 맞고 왜 틀리는가

timing-sensitive race나 buffering 변화처럼 관찰 코드가 실행 순서를 바꾼 Heisenbug 가능성을 고려한다. 더 낮은 오버헤드 측정과 반복 실험이 필요하다.

### 자주 만나는 실패와 확인 순서

- 고빈도 loop에 무거운 logging을 넣으면 원래 성능 특성이 달라진다.
- 측정값의 단위(ms/s)를 섞으면 잘못 해석한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** instrumentation은 가설과 관련된 상태를 실제 측정값으로 만든다.
- **직접 코딩·실행해서 익힐 것:** 경과 시간을 측정하고 반복 분포를 본다.
- **AI에게 맡겨도 되는 것:** 측정 코드/metric 이름 제안.
- **사람이 최종 확인할 것:** 측정 오버헤드와 통계적 해석 범위를 확인한다.

### 근거 연결

`GOOGLE-SRE` · `DORA-2025`

---

## CHAPTER 06 · LESSON 06 · Heisenbug에서 관찰 행위가 현상을 바꿀 수 있음을 안다

**LESSON ID:** `T10-B04-L06`

### 먼저 쉬운 말로 이해하기

debugger로 멈추거나 print를 추가했더니 오류가 사라지는 현상이 있다. 관찰이 timing, thread scheduling, buffering을 바꾸면 원래 조건이 깨질 수 있다. 이런 종류를 흔히 Heisenbug라고 부른다.

### 안에서는 실제로 무엇이 일어나는가

동시성 문제는 특정 interleaving에서만 나타날 수 있다. breakpoint는 스레드를 오래 멈추게 하고 logging lock은 실행 순서를 바꿀 수 있다. 따라서 low-overhead trace, 반복 stress, deterministic scheduler가 가능한 테스트 구조를 고려한다.

### 아주 쉬운 예

두 thread가 같은 counter를 바꾸는 예는 runtime에 따라 재현성이 다를 수 있으므로 “항상 실패” 예로 쓰지 않는다. 대신 사건 순서가 결과를 바꿀 수 있는 모델을 명시한다.

```python
def apply(balance, operation):
    kind, amount = operation
    if kind == "deposit":
        return balance + amount
    return balance - amount

ops_a = [("deposit", 100), ("withdraw", 50)]
print(apply(apply(1000, ops_a[0]), ops_a[1]))
```

### 한 줄씩 읽기

- 순차 예는 order를 명시해 deterministic하다.
- 실제 concurrent system에서는 read-modify-write가 겹칠 수 있다.
- 문제 핵심은 “동시에”라는 말보다 가능한 interleaving과 atomicity다.

### 직접 실행

1. 순차 결과를 확인한다.
2. 연산 순서가 바뀌어도 결과가 같은지 본다.
3. 잔액 부족 검사 같은 조건을 추가해 순서가 결과 의미를 바꾸는 사례를 만든다.


### 일부를 바꿔서 다시 확인하기

실제 race를 조사할 때 timestamped event, thread/task ID, lock 획득/해제 정보를 최소한으로 기록하는 계획을 작성한다.

### 작은 문제

print를 넣으면 실패가 사라졌으니 print가 수정책인가?

### 왜 맞고 왜 틀리는가

아니다. timing을 우연히 바꿔 증상을 숨긴 것일 수 있다. 공유 상태·atomicity·lock/ordering 계약을 찾아야 한다.

### 자주 만나는 실패와 확인 순서

- 관찰 도구로 재현율이 바뀌는지 기록하지 않으면 단서를 놓친다.
- sleep을 임시로 넣어 race를 숨기고 production fix로 제출하면 안 된다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 동시성 bug에서는 관찰 자체가 timing을 바꿀 수 있다.
- **직접 코딩·실행해서 익힐 것:** 순서 의존 상태 전이를 모델링하고 관찰 오버헤드를 의식한다.
- **AI에게 맡겨도 되는 것:** stress harness와 trace point 제안.
- **사람이 최종 확인할 것:** 진짜 동기화 규칙이 해결책인지 확인한다.

### 근거 연결

`GOOGLE-SRE` · `PYTEST`

---

## CHAPTER 07 · LESSON 07 · 수정 전에 실패를 고정하는 regression test를 만든다

**LESSON ID:** `T10-B04-L07`

### 먼저 쉬운 말로 이해하기

원인을 찾았다고 느낀 순간 바로 코드를 고치면, 나중에 같은 bug가 돌아왔는지 자동으로 알 수 없다. 가능하면 먼저 현재 실패를 재현하는 작은 regression test를 만든다.

### 안에서는 실제로 무엇이 일어나는가

좋은 regression test는 bug의 본질적인 입력과 기대값을 보존한다. implementation detail에 과도하게 묶이면 리팩터링 때 불필요하게 깨진다. 수정 전 실패, 수정 후 통과를 확인하면 bug와 fix의 연결 증거가 강해진다.

### 아주 쉬운 예

앞선 배송비 경계 bug를 test로 고정한다.

```python
def shipping_fee(total):
    if total > 50000:
        return 0
    return 3000

def test_free_shipping_boundary():
    assert shipping_fee(50000) == 0
```

### 한 줄씩 읽기

- test 이름이 과거 bug의 경계를 설명한다.
- 50,000이라는 입력은 요구사항 “이상”의 정확한 경계다.
- 수정 전에는 assertion이 실패해야 bug를 실제로 잡는 test임을 확인할 수 있다.

### 직접 실행

1. 파일을 pytest로 실행해 수정 전 실패를 확인한다.
2. 조건을 `>=`로 바꾼 뒤 같은 test를 다시 실행한다.
3. 49,999원 paid case도 추가해 과도한 fix를 막는다.


### 일부를 바꿔서 다시 확인하기

bug report ID나 설명을 comment 대신 test 이름/문서에 연결하고, 왜 이 경계가 중요한지 남긴다.

### 작은 문제

처음부터 통과하는 regression test를 추가했다. 이 test가 bug를 잡는다는 증거가 충분한가?

### 왜 맞고 왜 틀리는가

아니다. 가능한 경우 수정 전 버전에서 실제로 실패하는지 확인해야 test의 검출력을 입증할 수 있다. mutation이나 임시 rollback로도 검사할 수 있다.

### 자주 만나는 실패와 확인 순서

- fix 구현을 그대로 test에 복사하면 같은 오류를 두 번 쓸 수 있다.
- 너무 넓은 E2E만 regression으로 두면 느리고 원인 국소화가 어렵다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** regression test는 과거 실패를 재현하고 미래 재발을 감지한다.
- **직접 코딩·실행해서 익힐 것:** 수정 전 fail → 수정 후 pass 순서를 실제 확인한다.
- **AI에게 맡겨도 되는 것:** test skeleton과 경계 case 생성.
- **사람이 최종 확인할 것:** test가 bug를 실제 잡는지와 요구사항을 올바르게 표현하는지 판단한다.

### 근거 연결

`PYTEST` · `DORA-2025`

---
