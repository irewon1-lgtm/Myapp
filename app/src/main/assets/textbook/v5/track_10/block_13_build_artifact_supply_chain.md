# TRACK 10 · 오류·테스트·Git·빌드·배포 — 변경을 안전하게 만드는 기술

# BLOCK 13 · 빌드·dependency·artifact·공급망

코드를 고친 뒤에는 어떤 source와 dependency로 어떤 artifact를 만들었는지 추적할 수 있어야 한다. build, lockfile, reproducibility, digest, SBOM, provenance/attestation, dependency update를 하나의 공급망 흐름으로 묶는다.


```text
개념 설명 → 아주 쉬운 예 → 한 줄씩 해석 → 직접 실행 → 일부 수정 → 작은 문제 → 왜 맞고 틀렸는지 설명
```

---

## CHAPTER 01 · LESSON 01 · build는 source를 실행 가능한 artifact로 바꾸는 재현 가능한 과정이다

**LESSON ID:** `T10-B13-L01`

### 먼저 쉬운 말로 이해하기

소스 파일이 있다고 사용자가 바로 실행할 수 있는 것은 아니다. compile, resource 처리, dependency 결합, packaging 같은 단계를 거쳐 APK·binary·container image 같은 **artifact**가 만들어진다. build는 이 변환 과정을 뜻한다.

### 안에서는 실제로 무엇이 일어나는가

중요한 것은 “내 PC에서 한 번 만들어졌다”보다 입력과 단계가 추적 가능하다는 점이다. source revision, toolchain version, dependency, build flags가 입력이고 artifact가 출력이다. 단계 하나가 달라지면 결과도 달라질 수 있다.

### 아주 쉬운 예

Python에서는 compile이 단순하지만 source→bytecode 변환을 작은 build 예로 본다.

```bash
mkdir -p build-demo
printf 'print("hello")
' > build-demo/app.py
python -m compileall -q build-demo
echo "exit=$?"
find build-demo -type f -maxdepth 3 -print
```

### 한 줄씩 읽기

- source app.py가 build 입력이다.
- `compileall`은 syntax/bytecode 생성 단계의 작은 예다.
- exit 0은 이 단계 성공 증거이지 전체 앱 runtime 성공은 아니다.

### 직접 실행

1. 명령을 로컬 temp 폴더에서 실행한다.
2. 문법 오류를 넣어 non-zero/오류를 확인한다.
3. 오류를 복구하고 source SHA와 artifact 목록을 기록한다.


### 일부를 바꿔서 다시 확인하기

Android에서는 compile/resource merge/dex/package/signing처럼 더 많은 단계가 있다는 build graph를 그린다. 각 단계 실패가 서로 다른 class임을 표시한다.

### 작은 문제

compile 단계가 성공했으니 설치·실행도 PASS라고 써도 되는가?

### 왜 맞고 왜 틀리는가

안 된다. build 성공과 runtime/install 검증은 별도다. 각 gate의 실제 실행 범위를 이름에 붙인다.

### 자주 만나는 실패와 확인 순서

- generated artifact를 source와 섞어 commit하지 않는 정책을 확인한다.
- build script가 network에서 mutable “latest”를 가져오면 재현성이 떨어질 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** build는 source/toolchain/dependency/config 입력을 artifact로 변환하는 과정이다.
- **직접 코딩·실행해서 익힐 것:** 작은 build 실패와 성공 exit를 직접 본다.
- **AI에게 맡겨도 되는 것:** build graph 설명과 오류 분류.
- **사람이 최종 확인할 것:** 어떤 단계까지 실제 검증됐는지 범위를 확인한다.

### 근거 연결

`SLSA-12` · `TEXTBOOK-V5-CONTRACT`

---

## CHAPTER 02 · LESSON 02 · dependency lockfile은 “무엇을 설치했는가”를 고정하는 핵심 입력이다

**LESSON ID:** `T10-B13-L02`

### 먼저 쉬운 말로 이해하기

`package.json`이나 requirements에 넓은 버전 범위만 있으면 같은 source라도 설치 날짜에 따라 다른 dependency가 들어올 수 있다. lockfile은 해석된 구체 버전과 dependency graph를 고정하는 데 도움을 준다.

### 안에서는 실제로 무엇이 일어나는가

lockfile도 source control과 review 대상이다. dependency를 올릴 때 application code와 마찬가지로 diff, changelog, tests, security 정보를 본다. lockfile을 매 build마다 임의 재생성하면 재현성이 약해진다.

### 아주 쉬운 예

Python의 간단한 버전 고정 파일을 개념 예로 본다.

```bash
cat > requirements-demo.txt <<'EOF'
pytest==9.0.2
EOF
cat requirements-demo.txt
```

### 한 줄씩 읽기

- `==`는 교육용 exact version 예다.
- 실제 Python 프로젝트는 uv/poetry/pip-tools 등 resolver/lock 형식이 다를 수 있다.
- 핵심은 resolved dependency identity를 build 입력으로 추적하는 것이다.

### 직접 실행

1. 현재 환경의 `pytest --version`과 파일 값을 비교한다.
2. 버전 값을 의도적으로 다른 값으로 바꿔 “문서와 실제 환경 불일치”를 만든다.
3. 실제 install은 이 패키지 검증에서 network 비용 없이 수행하지 않는다.


### 일부를 바꿔서 다시 확인하기

transitive dependency도 lock에 기록되는 이유와 직접 dependency 버전만 적을 때 남는 불확실성을 설명한다.

### 작은 문제

lockfile이 있으면 supply-chain 위험이 사라지는가?

### 왜 맞고 왜 틀리는가

아니다. 무엇을 설치할지 고정할 뿐 그 dependency 자체가 안전하다는 보장은 아니다. provenance, vulnerability, review, update 정책이 별도로 필요하다.

### 자주 만나는 실패와 확인 순서

- lockfile conflict를 무조건 한쪽 선택으로 해결하지 않는다.
- dependency update가 generated diff라 해도 test 없이 자동 merge하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** lockfile은 resolved dependency를 재현 가능한 build 입력으로 고정하는 역할을 한다.
- **직접 코딩·실행해서 익힐 것:** 실제 runtime version과 선언/lock identity를 비교한다.
- **AI에게 맡겨도 되는 것:** dependency diff/changelog 요약.
- **사람이 최종 확인할 것:** 업데이트 위험과 실제 resolved graph를 확인한다.

### 근거 연결

`NIST-SSDF-12` · `SLSA-12`

---

## CHAPTER 03 · LESSON 03 · reproducible build는 같은 입력에서 같은 결과를 재생할 수 있게 한다

**LESSON ID:** `T10-B13-L03`

### 먼저 쉬운 말로 이해하기

같은 commit을 오늘과 다음 달에 build했는데 결과가 설명 없이 달라지면 문제가 생겼을 때 추적하기 어렵다. reproducible build는 build 입력과 환경을 고정해 동일하거나 검증 가능한 동등 artifact를 다시 만들 수 있게 하는 목표다.

### 안에서는 실제로 무엇이 일어나는가

timestamp, random build ID, file ordering, absolute path, toolchain version이 artifact bytes를 바꿀 수 있다. 완전 byte-for-byte reproducibility가 어려운 플랫폼도 있지만 무엇이 결과를 바꾸는지 식별하고 provenance에 기록해야 한다.

### 아주 쉬운 예

같은 text artifact의 hash를 두 번 계산해 identity를 비교한다.

```bash
printf 'artifact-content
' > artifact.bin
sha256sum artifact.bin
sha256sum artifact.bin
```

### 한 줄씩 읽기

- 같은 bytes는 같은 SHA-256 digest를 만든다.
- 한 byte가 바뀌면 digest가 달라진다.
- hash 일치는 출처/안전성 전체를 증명하지 않고 content identity 비교에 쓰인다.

### 직접 실행

1. 두 hash가 같은지 확인한다.
2. 파일 끝에 공백 한 글자를 추가해 hash 변화를 본다.
3. source revision과 toolchain 정보를 별도 metadata에 기록한다.


### 일부를 바꿔서 다시 확인하기

build timestamp를 artifact 본문에 넣는 script를 만들어 매 실행 hash가 달라지는 것을 보고, timestamp를 외부 metadata로 옮기는 대안을 생각한다.

### 작은 문제

hash가 같으면 그 artifact가 악성 코드가 아님을 증명하는가?

### 왜 맞고 왜 틀리는가

아니다. 같은 content인지 확인할 뿐이다. 신뢰하는 source/build 과정과 연결하는 provenance/signature가 별도다.

### 자주 만나는 실패와 확인 순서

- 비결정적 build 요소를 “원래 그래”라고 무시하지 말고 원인을 목록화한다.
- hash algorithm은 보안 목적에 적합한 현재 권장 방식을 사용한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** reproducibility는 build 입력과 과정의 통제를 통해 결과를 다시 설명/검증할 수 있게 한다.
- **직접 코딩·실행해서 익힐 것:** 같은/다른 bytes의 digest를 직접 비교한다.
- **AI에게 맡겨도 되는 것:** 비결정 build 원인 탐색.
- **사람이 최종 확인할 것:** 재현 수준과 남은 비결정 요소를 명시한다.

### 근거 연결

`SLSA-12` · `GOOGLE-SRE`

---

## CHAPTER 04 · LESSON 04 · artifact는 한번 만든 뒤 source revision과 함께 식별한다

**LESSON ID:** `T10-B13-L04`

### 먼저 쉬운 말로 이해하기

“v1.2 APK”라는 파일명만으로는 어느 source에서 어떤 build가 만든 것인지 부족하다. artifact에는 digest, source SHA, build job identity, version을 연결해 **무엇을 배포했는지** 추적 가능하게 한다.

### 안에서는 실제로 무엇이 일어나는가

좋은 release flow는 같은 검증된 artifact를 환경마다 다시 build하지 않고 promote하는 방식을 고려한다. 매 환경에서 rebuild하면 source는 같아도 artifact가 달라질 수 있다.

### 아주 쉬운 예

artifact metadata JSON을 로컬에서 만든다.

```bash
sha=$(sha256sum artifact.bin | awk '{print $1}')
commit=$(git rev-parse HEAD 2>/dev/null || echo no-git)
printf '{"sha256":"%s","source":"%s"}
' "$sha" "$commit" > artifact-meta.json
cat artifact-meta.json
```

### 한 줄씩 읽기

- digest는 artifact bytes identity다.
- source SHA는 source revision과 연결한다.
- 실제 CI에서는 workflow/run/toolchain identity도 provenance에 들어갈 수 있다.

### 직접 실행

1. 로컬 artifact와 metadata를 만든다.
2. artifact를 수정한 뒤 metadata를 재생성하지 않고 mismatch를 검출하는 script를 만든다.
3. 검증된 artifact를 이름만 바꾸는 것과 rebuild하는 차이를 적는다.


### 일부를 바꿔서 다시 확인하기

release note에 source SHA와 artifact digest를 함께 기록하는 format을 설계한다.

### 작은 문제

같은 version 문자열이면 같은 artifact라고 볼 수 있는가?

### 왜 맞고 왜 틀리는가

아니다. version 이름은 사람이 정한 label이고 bytes identity는 digest 등으로 확인해야 한다.

### 자주 만나는 실패와 확인 순서

- artifact를 덮어써 같은 URL/version이 다른 bytes를 가리키는 운영을 피한다.
- 서명 key/credential을 artifact 옆 plaintext로 저장하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** artifact는 source와 별개로 digest/build identity를 가진 배포 대상이다.
- **직접 코딩·실행해서 익힐 것:** metadata와 digest mismatch를 직접 검출한다.
- **AI에게 맡겨도 되는 것:** release metadata 생성.
- **사람이 최종 확인할 것:** 검증한 artifact와 실제 배포 artifact가 같은지 확인한다.

### 근거 연결

`SLSA-12` · `GITHUB-ATTEST`

---

## CHAPTER 05 · LESSON 05 · SBOM은 artifact 안의 software 구성요소 목록이다

**LESSON ID:** `T10-B13-L05`

### 먼저 쉬운 말로 이해하기

앱이 직접 작성한 코드만으로 만들어지지 않는다. library와 transitive dependency가 들어간다. SBOM(Software Bill of Materials)은 어떤 component가 포함됐는지 기계가 읽을 수 있는 목록으로 표현한다.

### 안에서는 실제로 무엇이 일어나는가

SBOM은 vulnerability가 없다는 인증서가 아니다. 특정 library 취약점이 발표됐을 때 “우리 artifact에 그 component가 들어갔는가”를 조사하는 inventory 근거가 된다. SPDX, CycloneDX 같은 표준 형식이 있다.

### 아주 쉬운 예

교육용 최소 component 목록을 JSON으로 표현한다.

```bash
cat > sbom-demo.json <<'EOF'
{
  "components": [
    {"name":"demo-app","version":"1.0.0"},
    {"name":"example-lib","version":"2.3.1"}
  ]
}
EOF
python -m json.tool sbom-demo.json
```

### 한 줄씩 읽기

- component 이름과 version이 최소 식별 단서다.
- 실제 SBOM에는 package URL, license, hashes, relationships 등 더 많은 metadata가 있다.
- manual 목록보다 build tool이 실제 resolved graph에서 생성하는 편이 정확하다.

### 직접 실행

1. JSON 문법을 실제 검증한다.
2. dependency 하나를 추가해 SBOM diff를 본다.
3. artifact digest와 SBOM을 같은 release metadata에 연결한다.


### 일부를 바꿔서 다시 확인하기

직접 dependency에는 없지만 transitive로 포함된 취약 component가 있다고 가정하고 SBOM이 왜 유용한지 설명한다.

### 작은 문제

SBOM을 만들었으니 dependency review가 끝난 것인가?

### 왜 맞고 왜 틀리는가

아니다. inventory가 생긴 것이다. vulnerability/license/provenance/policy 평가는 별도다.

### 자주 만나는 실패와 확인 순서

- SBOM을 stale하게 수동 유지하지 않는다.
- private package name 자체가 민감한 정보일 수 있어 공개 범위를 정책에 맞춘다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** SBOM은 artifact를 구성하는 software component inventory다.
- **직접 코딩·실행해서 익힐 것:** 간단한 SBOM을 parse하고 dependency diff를 본다.
- **AI에게 맡겨도 되는 것:** SBOM 요약/취약 component lookup 보조.
- **사람이 최종 확인할 것:** SBOM이 실제 build artifact와 일치하는지와 공개 범위를 확인한다.

### 근거 연결

`GITHUB-ATTEST` · `NIST-SSDF-12`

---

## CHAPTER 06 · LESSON 06 · provenance와 attestation은 artifact가 어디서 어떻게 만들어졌는지 연결한다

**LESSON ID:** `T10-B13-L06`

### 먼저 쉬운 말로 이해하기

artifact hash만 있으면 bytes는 식별할 수 있지만 “누가 어떤 source와 workflow로 만들었는가”는 알 수 없다. provenance는 artifact가 나온 source와 build 과정을 추적할 수 있는 정보를 말한다.

### 안에서는 실제로 무엇이 일어나는가

SLSA는 provenance를 software artifact의 출처와 build를 추적하는 검증 가능한 정보로 설명한다. GitHub artifact attestation도 workflow, repository, commit SHA 등과 artifact를 암호학적으로 연결할 수 있다. 단 attestation이 있다는 것 자체가 software가 안전하다는 뜻은 아니다.

### 아주 쉬운 예

실제 attestation 생성은 외부 GitHub Actions write/실행이 필요할 수 있으므로 여기서는 local metadata 구조만 만든다.

```bash
cat artifact-meta.json
# 실제 GitHub attestation 생성/Actions 실행은 사전 승인 없이는 수행하지 않는다.
```

### 한 줄씩 읽기

- provenance는 source/build 관계를 설명한다.
- attestation은 그 주장에 검증 가능한 서명을 붙이는 방식이다.
- 검증 단계가 있어야 provenance를 소비하는 의미가 생긴다.

### 직접 실행

1. 현재 package의 artifact metadata를 읽는다.
2. source SHA/digest가 빠지면 어떤 추적 질문에 답할 수 없는지 적는다.
3. 외부 attestation 명령은 실행하지 않는다.


### 일부를 바꿔서 다시 확인하기

“attestation verified”와 “artifact vulnerability 0”은 서로 다른 주장임을 두 문장으로 구분한다.

### 작은 문제

서명된 artifact면 무조건 안전한가?

### 왜 맞고 왜 틀리는가

아니다. 서명은 identity/integrity/provenance 정책을 돕지만 source 자체가 악성·취약할 수 있다. 무엇을 신뢰할지 정책이 필요하다.

### 자주 만나는 실패와 확인 순서

- attestation 검증 없이 파일만 생성해 두고 공급망 보호 완료라고 보고하지 않는다.
- signing credential 권한을 최소화한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** provenance는 artifact와 source/build 과정을 연결하고 attestation은 그 연결의 검증 가능성을 높인다.
- **직접 코딩·실행해서 익힐 것:** local metadata의 source/digest 연결을 확인한다.
- **AI에게 맡겨도 되는 것:** provenance/attestation workflow 초안.
- **사람이 최종 확인할 것:** 신뢰 정책과 실제 verification 결과를 확인한다.

### 근거 연결

`SLSA-12` · `GITHUB-ATTEST`

---

## CHAPTER 07 · LESSON 07 · dependency update는 자동화와 사람 검토를 함께 쓴다

**LESSON ID:** `T10-B13-L07`

### 먼저 쉬운 말로 이해하기

dependency update bot은 새 버전을 빠르게 알려주지만 “최신이니 merge”가 정답은 아니다. breaking change, vulnerability fix, license, build tool 요구, transitive diff를 확인하고 tests를 실행해야 한다.

### 안에서는 실제로 무엇이 일어나는가

작은 patch update도 supply-chain compromise 가능성이 0은 아니다. 반대로 update를 영원히 미루면 알려진 취약점과 지원 종료 위험이 쌓인다. 자동 PR + policy + test + review를 조합한다.

### 아주 쉬운 예

업데이트 review checklist를 text로 만든다.

```bash
cat <<'EOF'
1. 왜 업데이트하는가: security/bug/support
2. direct/transitive diff는 무엇인가
3. changelog/breaking change가 있는가
4. lockfile diff가 예상 범위인가
5. 어떤 tests를 실제 실행했는가
6. rollback 가능한가
EOF
```

### 한 줄씩 읽기

- 업데이트 목적을 먼저 적는다.
- lockfile에는 예상보다 많은 transitive 변화가 생길 수 있다.
- tests와 rollback 범위를 evidence로 남긴다.

### 직접 실행

1. 가상의 dependency 1개를 선택해 checklist를 채운다.
2. breaking change가 있는 major update와 security patch update의 우선순위를 비교한다.
3. AI 요약을 쓰더라도 원문 release/security advisory를 확인한다.


### 일부를 바꿔서 다시 확인하기

dependency 자동 merge를 허용할 수 있는 낮은 위험 조건을 정의하고, 높은 위험 package는 human review를 강제하는 정책을 만든다.

### 작은 문제

test가 green이면 dependency 공급망 위험 검토가 불필요한가?

### 왜 맞고 왜 틀리는가

아니다. tests는 behavior 일부를 확인할 뿐 package 출처·악성 변경·license·새 취약점까지 보장하지 않는다.

### 자주 만나는 실패와 확인 순서

- 자동 update가 동시에 너무 많이 쌓이면 원인 추적이 어려워진다.
- maintainer 계정 compromise 같은 위험은 version number만으로 판단할 수 없다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** dependency update는 freshness와 안정성·공급망 위험 사이의 관리된 변경이다.
- **직접 코딩·실행해서 익힐 것:** update diff/checklist/test evidence를 연결한다.
- **AI에게 맡겨도 되는 것:** changelog/advisory 요약.
- **사람이 최종 확인할 것:** merge와 위험 수용은 사람이 판단한다.

### 근거 연결

`NIST-SSDF-12` · `SLSA-12`

---
