# TRACK 10 · 오류·테스트·Git·빌드·배포 — 변경을 안전하게 만드는 기술

# BLOCK 14 · CI 파이프라인과 품질 게이트

CI는 자동 실행 자체보다 “같은 변경을 같은 규칙으로 검증하고 증거를 남기는 시스템”이다. job 분리, cache, matrix, secret/permission, security scan, artifact provenance, failure triage를 실제 검증 범위와 연결한다.


```text
개념 설명 → 아주 쉬운 예 → 한 줄씩 해석 → 직접 실행 → 일부 수정 → 작은 문제 → 왜 맞고 틀렸는지 설명
```

---

## CHAPTER 01 · LESSON 01 · CI는 commit마다 같은 검증 절차를 반복 실행한다

**LESSON ID:** `T10-B14-L01`

### 먼저 쉬운 말로 이해하기

Continuous Integration의 핵심은 “GitHub Actions 화면”이 아니라 변경을 자주 합치고 같은 검증을 자동으로 반복하는 것이다. local에서 되던 검사가 CI에서도 재현되도록 명령과 환경을 코드로 남긴다.

### 안에서는 실제로 무엇이 일어나는가

pipeline은 checkout→setup→build→test→artifact 같은 stage로 구성할 수 있다. 각 job의 실패가 전체 gate를 막아야 하는지 정책을 명시한다. `continue-on-error` 같은 설정은 실패를 숨길 수 있어 의도가 필요하다.

### 아주 쉬운 예

외부 Actions를 실행하지 않고 local shell pipeline으로 exit propagation을 본다.

```bash
set -e
python -m compileall -q .
pytest -q
printf 'pipeline reached end
'
```

### 한 줄씩 읽기

- `set -e`는 단순 shell 예에서 실패 시 중단하게 돕는다.
- compile과 test는 서로 다른 failure class다.
- 실제 CI YAML의 semantics는 shell과 다를 수 있어 platform 문서를 확인한다.

### 직접 실행

1. package test directory에서 local pipeline을 실행한다.
2. 의도적으로 failing test를 임시로 넣어 뒤 단계가 실행되지 않는지 본다.
3. 원복 후 다시 성공을 확인한다.


### 일부를 바꿔서 다시 확인하기

lint를 non-blocking advisory로 둘지 required gate로 둘지 실패 비용과 false positive를 기준으로 결정한다.

### 작은 문제

CI workflow 파일이 repository에 있으니 CI 검증 완료라고 말할 수 있는가?

### 왜 맞고 왜 틀리는가

아니다. workflow 정의 존재는 PREPARED 상태다. 실제 run 결과가 있어야 해당 commit에 대한 CI evidence가 된다.

### 자주 만나는 실패와 확인 순서

- workflow가 test discovery 0개인데 green인지 확인한다.
- 외부 Actions 실행은 이 프로젝트 규칙상 사전 승인 없이 하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** CI는 동일한 검증 절차를 변경마다 자동 반복하는 integration gate다.
- **직접 코딩·실행해서 익힐 것:** local 동등 명령에서 fail/success propagation을 직접 본다.
- **AI에게 맡겨도 되는 것:** workflow 초안/실패 분석.
- **사람이 최종 확인할 것:** 실제 실행 여부와 required gate 범위를 확인한다.

### 근거 연결

`DORA-2025` · `TEXTBOOK-V5-CONTRACT`

---

## CHAPTER 02 · LESSON 02 · job을 실패 유형별로 나누면 어떤 증거가 깨졌는지 빨리 안다

**LESSON ID:** `T10-B14-L02`

### 먼저 쉬운 말로 이해하기

모든 검사를 한 거대한 job에 넣으면 첫 실패 뒤 나머지 정보를 얻지 못할 수 있다. compile, unit, integration, security, packaging처럼 독립 가능한 검사를 분리하면 병렬화와 진단이 쉬워진다.

### 안에서는 실제로 무엇이 일어나는가

반대로 지나치게 잘게 나누면 setup overhead와 복잡도가 커진다. dependency가 있는 단계는 needs/ordering으로 연결하고, 동일 artifact를 공유할 때 identity를 보존한다.

### 아주 쉬운 예

local에서는 각 gate를 함수처럼 분리한 shell script로 정신 모델을 만든다.

```bash
check_json() { python -m json.tool "$1" >/dev/null; }
check_python() { python -m compileall -q "$1"; }
check_json track_10/manifest.json
check_python tests
```

### 한 줄씩 읽기

- 각 함수는 다른 실패 유형을 검사한다.
- 어느 단계가 실패했는지 이름을 붙일 수 있다.
- CI에서는 job별 artifact/report를 모을 수 있다.

### 직접 실행

1. 두 gate를 각각 실행한다.
2. JSON을 깨뜨려 schema 계열 실패만 나는지 본다.
3. 복구 후 Python syntax를 깨뜨려 다른 gate가 실패하는지 본다.


### 일부를 바꿔서 다시 확인하기

같은 setup을 10개 job이 반복해 시간이 커지는 경우 reusable step/cache와 단일 job trade-off를 적는다.

### 작은 문제

job 수가 많으면 CLEAN PASS 수도 늘어난다고 볼 수 있는가?

### 왜 맞고 왜 틀리는가

아니다. 서로 다른 실패 유형을 실제 검증하는지가 핵심이다. 같은 검사를 이름만 바꿔 반복하면 독립 PASS가 아니다.

### 자주 만나는 실패와 확인 순서

- 병렬 job이 같은 mutable test DB를 공유하지 않는다.
- required check 이름 변경이 branch protection에 영향을 줄 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** CI job 분리는 서로 다른 실패 유형과 의존성을 명확히 하는 데 목적이 있다.
- **직접 코딩·실행해서 익힐 것:** 의도적으로 각 gate를 깨뜨려 다른 failure를 검출하는지 본다.
- **AI에게 맡겨도 되는 것:** pipeline decomposition 제안.
- **사람이 최종 확인할 것:** 독립 검증인지 중복 실행인지 판단한다.

### 근거 연결

`TEXTBOOK-V5-CONTRACT` · `DORA-2025`

---

## CHAPTER 03 · LESSON 03 · cache는 속도를 높이지만 검증 입력의 일부를 잘못 재사용할 수 있다

**LESSON ID:** `T10-B14-L03`

### 먼저 쉬운 말로 이해하기

CI cache는 dependency download나 build 중간 결과를 재사용해 시간을 줄인다. 하지만 key가 부정확하면 다른 dependency/환경의 결과를 재사용해 이상한 성공이나 실패를 만들 수 있다.

### 안에서는 실제로 무엇이 일어나는가

cache key에는 lockfile hash, OS/toolchain 등 결과에 영향을 주는 입력을 포함한다. cache miss는 correctness 실패가 아니라 느려짐이어야 한다. artifact와 cache도 구분한다: cache는 가속용, artifact는 결과물/증거 전달용이다.

### 아주 쉬운 예

lockfile hash로 cache key를 만드는 원리를 local hash로 본다.

```bash
printf 'dep=1.0
' > lock-demo.txt
key=$(sha256sum lock-demo.txt | awk '{print $1}')
echo "cache-key=$key"
```

### 한 줄씩 읽기

- lock 내용이 바뀌면 key가 바뀐다.
- OS/toolchain이 결과에 영향 주면 key에 같이 넣어야 한다.
- cache hit 자체를 correctness evidence로 쓰면 안 된다.

### 직접 실행

1. 현재 key를 기록한다.
2. dep=1.1로 바꿔 key가 달라지는지 본다.
3. lock을 되돌려 원래 key가 다시 나오는지 확인한다.


### 일부를 바꿔서 다시 확인하기

cache를 완전히 비운 cold run에서도 pipeline이 성공하는지 주기적으로 확인하는 이유를 설명한다.

### 작은 문제

cache가 있으면 dependency download 검증을 건너뛰어도 되는가?

### 왜 맞고 왜 틀리는가

cache가 source를 신뢰하게 만드는 것은 아니다. cache 내용의 출처/키와 dependency 검증 정책이 필요하다.

### 자주 만나는 실패와 확인 순서

- cache poisoning 위험과 write 권한 범위를 고려한다.
- secret을 cache key/content에 넣지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** cache는 성능 최적화이며 correctness source of truth가 아니다.
- **직접 코딩·실행해서 익힐 것:** 입력 hash에 따라 key가 바뀌는지 직접 본다.
- **AI에게 맡겨도 되는 것:** cache key 설계.
- **사람이 최종 확인할 것:** key가 실제 모든 중요한 입력을 반영하는지 확인한다.

### 근거 연결

`GITHUB-ACTIONS` · `NIST-SSDF-12`

---

## CHAPTER 04 · LESSON 04 · matrix testing으로 중요한 환경 차이를 선택해 검증한다

**LESSON ID:** `T10-B14-L04`

### 먼저 쉬운 말로 이해하기

한 OS·한 runtime 버전에서만 test하면 다른 지원 환경에서 깨질 수 있다. matrix testing은 지원하는 중요한 조합을 자동 반복한다. 하지만 조합을 무한히 늘리면 비용과 시간이 폭증한다.

### 안에서는 실제로 무엇이 일어나는가

사용자 비중, support policy, 위험을 기준으로 대표 조합을 고른다. 예: Python 최소/최신 지원 버전, Android API 주요 범위, DB 버전. 모든 조합 대신 pairwise/risk-based 선택을 고려한다.

### 아주 쉬운 예

local에서는 환경 값을 변수로 바꿔 같은 test가 조건에 따라 다르게 동작하지 않는지 본다.

```bash
for mode in dev test prodlike; do
  echo "checking mode=$mode"
  APP_MODE=$mode python -c 'import os; print(os.environ["APP_MODE"])'
done
```

### 한 줄씩 읽기

- 같은 script를 세 환경 값으로 반복한다.
- 실제 CI matrix는 runner/runtime 버전 자체를 바꿀 수 있다.
- prodlike는 production credential을 쓰라는 뜻이 아니다.

### 직접 실행

1. loop를 실행해 세 값이 실제 주입되는지 본다.
2. 한 mode에서만 실패하도록 임시 조건을 만들고 진단한다.
3. 지원 환경 표에서 중요 조합을 고른다.


### 일부를 바꿔서 다시 확인하기

20개 환경 조합 중 어떤 것을 PR마다, 어떤 것을 nightly/release에서 돌릴지 비용과 risk로 분리한다.

### 작은 문제

matrix case 하나가 실패하니 나머지 성공 case로 전체 PASS라고 해도 되는가?

### 왜 맞고 왜 틀리는가

안 된다. 지원 대상인 실패 환경은 전체 release gate에 영향을 준다. 실패 case를 명시한다.

### 자주 만나는 실패와 확인 순서

- 지원하지 않는 환경 실패를 gate에 넣어 noise를 만들지 않는다.
- 외부 유료 runner matrix 확대는 비용 승인 전 실행하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** matrix는 지원 환경 차이에서 생기는 regression을 대표 조합으로 검사한다.
- **직접 코딩·실행해서 익힐 것:** 환경 변수를 바꾼 반복 실행으로 개념을 확인한다.
- **AI에게 맡겨도 되는 것:** risk-based matrix 설계.
- **사람이 최종 확인할 것:** 지원 policy와 비용을 기준으로 조합을 결정한다.

### 근거 연결

`DORA-2025` · `GITHUB-ACTIONS`

---

## CHAPTER 05 · LESSON 05 · CI secret과 token 권한은 최소화한다

**LESSON ID:** `T10-B14-L05`

### 먼저 쉬운 말로 이해하기

CI는 code를 실행하기 때문에 credential을 다루면 위험이 커진다. build에 필요하지 않은 write token을 주지 않고, job별 최소 permission만 부여해야 한다.

### 안에서는 실제로 무엇이 일어나는가

fork PR/untrusted code가 secret을 읽을 수 있는지, logs에 masking이 되는지, OIDC로 short-lived credential을 쓸 수 있는지 platform 정책을 확인한다. secret 값을 echo하거나 artifact/cache에 저장하지 않는다.

### 아주 쉬운 예

실제 secret을 쓰지 않고 환경변수가 존재하는지만 안전하게 확인한다.

```bash
python - <<'PY2'
import os
print("TOKEN configured:", bool(os.getenv("TOKEN")))
PY2
```

### 한 줄씩 읽기

- 값 자체는 출력하지 않는다.
- configured 여부도 일부 환경에서는 민감할 수 있어 필요할 때만 남긴다.
- 실제 CI permission은 workflow의 `permissions` scope를 확인한다.

### 직접 실행

1. 로컬 dummy TOKEN으로 true/false만 확인한다.
2. script에서 token 값을 출력하는 줄이 없는지 grep한다.
3. write permission이 필요한 job과 read-only job을 분리한다.


### 일부를 바꿔서 다시 확인하기

artifact upload step이 workspace 전체를 압축하면 `.env`가 섞일 수 있는 사례를 만들고 allowlist path 정책을 설계한다.

### 작은 문제

GitHub가 log에서 secret을 mask하니 마음대로 출력해도 안전한가?

### 왜 맞고 왜 틀리는가

아니다. masking 누락·변형·다른 sink가 있을 수 있다. 애초에 secret을 출력하지 않는 것이 원칙이다.

### 자주 만나는 실패와 확인 순서

- pull_request_target 같은 고권한 trigger는 보안 의미를 충분히 이해하지 않고 사용하지 않는다.
- third-party action에 불필요한 token permission을 넘기지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** CI credential은 필요한 job에 필요한 최소 권한으로만 제공하고 출력/보존하지 않는다.
- **직접 코딩·실행해서 익힐 것:** dummy secret로 값 비노출 검사를 한다.
- **AI에게 맡겨도 되는 것:** workflow permission review.
- **사람이 최종 확인할 것:** 외부 action 신뢰와 실제 permission 범위를 확인한다.

### 근거 연결

`NIST-SSDF-12` · `GITHUB-ACTIONS`

---

## CHAPTER 06 · LESSON 06 · security scan은 한 종류가 아니라 서로 다른 위험을 본다

**LESSON ID:** `T10-B14-L06`

### 먼저 쉬운 말로 이해하기

dependency vulnerability scan, secret scan, static analysis, container scan은 이름은 모두 “보안 검사”지만 보는 대상이 다르다. 하나가 green이라고 다른 위험까지 green인 것은 아니다.

### 안에서는 실제로 무엇이 일어나는가

dependency scan은 알려진 package advisory, secret scan은 credential pattern/history, SAST는 source pattern/data flow, artifact scan은 built package를 볼 수 있다. false positive/false negative가 있으므로 triage와 정책이 필요하다.

### 아주 쉬운 예

로컬에는 실제 secret을 만들지 않고 위험 문자열 탐지 원리만 dummy pattern으로 본다.

```bash
printf 'API_TOKEN=DEMO_NOT_A_SECRET
' > scan-demo.txt
grep -n 'API_TOKEN=' scan-demo.txt
```

### 한 줄씩 읽기

- grep는 scanner가 아니라 pattern detection 원리 예다.
- 실제 secret scanner는 다양한 provider pattern과 entropy/history를 본다.
- 발견 후에는 파일 삭제뿐 아니라 credential rotation이 필요할 수 있다.

### 직접 실행

1. dummy pattern을 탐지한다.
2. 파일을 삭제하고 Git history에 commit됐다고 가정했을 때 남는 risk를 적는다.
3. security gate별 탐지 대상 표를 만든다.


### 일부를 바꿔서 다시 확인하기

SAST 경고 하나를 suppress하려면 근거와 scope를 기록해야 하는 이유를 설명한다.

### 작은 문제

보안 scanner 4개를 붙이면 software가 안전하다고 증명되는가?

### 왜 맞고 왜 틀리는가

아니다. 알려진/모델링된 위험 일부를 자동 탐지한다. threat modeling, review, runtime hardening, incident response가 별도다.

### 자주 만나는 실패와 확인 순서

- scanner 경고를 일괄 ignore하지 않는다.
- 검사 도구 자체와 rule version도 provenance/업데이트 대상이다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 보안 gate마다 탐지하는 실패 유형이 다르며 green 범위를 확대 해석하지 않는다.
- **직접 코딩·실행해서 익힐 것:** dummy pattern으로 탐지/triage 흐름을 연습한다.
- **AI에게 맡겨도 되는 것:** 경고 분류/근거 요약.
- **사람이 최종 확인할 것:** 실제 위험과 suppression 타당성을 사람이 판단한다.

### 근거 연결

`NIST-SSDF-12` · `GITHUB-ACTIONS`

---

## CHAPTER 07 · LESSON 07 · CI 결과를 artifact provenance와 연결하고 실패를 빠르게 triage한다

**LESSON ID:** `T10-B14-L07`

### 먼저 쉬운 말로 이해하기

CI green badge 하나보다 “어느 commit에서 어떤 job들이 어떤 artifact를 만들었는가”가 중요하다. build/test 결과와 artifact digest, source SHA, workflow run을 연결하면 배포 후 문제가 생겼을 때 돌아갈 길이 생긴다.

### 안에서는 실제로 무엇이 일어나는가

GitHub artifact attestation 같은 기능은 provenance를 서명된 claim으로 만들 수 있지만, 생성과 verification이 모두 필요하다. CI 실패는 첫 error만 보지 말고 failing job, first meaningful error, environment difference, flaky 여부를 분류한다.

### 아주 쉬운 예

로컬 report에 source/test/artifact identity를 함께 넣는다.

```bash
commit=$(git rev-parse HEAD 2>/dev/null || echo local)
artifact_sha=$(sha256sum artifact.bin 2>/dev/null | awk '{print $1}')
printf 'commit=%s
artifact_sha=%s
' "$commit" "$artifact_sha"
```

### 한 줄씩 읽기

- source revision과 artifact digest가 한 report에 연결된다.
- test report identity도 같은 build에 묶어야 한다.
- 실제 attestation verification은 별도 실행 증거가 필요하다.

### 직접 실행

1. 로컬 report를 생성한다.
2. artifact 한 byte를 바꿔 digest mismatch를 본다.
3. CI failure triage template에 job/env/first error/reproduction 항목을 채운다.


### 일부를 바꿔서 다시 확인하기

AI가 CI log를 요약하게 하되 secret redaction과 잘못된 원인 단정을 사람이 검토하는 절차를 설계한다.

### 작은 문제

AI가 “dependency bug”라고 요약했으니 바로 version downgrade해도 되는가?

### 왜 맞고 왜 틀리는가

아니다. 요약은 가설이다. raw failure와 재현, changelog/known issue 증거를 확인해야 한다.

### 자주 만나는 실패와 확인 순서

- attestation 생성 성공만 보고 verification을 생략하지 않는다.
- CI log의 token/PII를 외부 AI에 그대로 보내지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** CI evidence는 source·test·artifact identity를 연결하고 실패 triage는 사실과 가설을 분리한다.
- **직접 코딩·실행해서 익힐 것:** local report에서 digest mismatch를 검출한다.
- **AI에게 맡겨도 되는 것:** log 요약/triage 초안.
- **사람이 최종 확인할 것:** raw evidence와 provenance verification을 최종 확인한다.

### 근거 연결

`GITHUB-ATTEST` · `SLSA-12` · `DORA-2025`

---
