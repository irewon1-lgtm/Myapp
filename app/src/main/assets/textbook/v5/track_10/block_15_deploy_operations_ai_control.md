# TRACK 10 · 오류·테스트·Git·빌드·배포 — 변경을 안전하게 만드는 기술

# BLOCK 15 · 배포·rollback·운영 검증·AI 변경 통제

마지막 BLOCK은 코드를 만든 뒤 사용자가 실제로 쓰는 환경까지 안전하게 보내는 흐름을 완성한다. deployment/release 분리, canary/blue-green, migration, rollback, smoke/observability, incident, AI 시대의 변경 통제를 하나의 capstone으로 연결한다.


```text
개념 설명 → 아주 쉬운 예 → 한 줄씩 해석 → 직접 실행 → 일부 수정 → 작은 문제 → 왜 맞고 틀렸는지 설명
```

---

## CHAPTER 01 · LESSON 01 · deployment와 release를 분리하면 사용자 노출을 별도로 통제할 수 있다

**LESSON ID:** `T10-B15-L01`

### 먼저 쉬운 말로 이해하기

코드를 production 환경에 배치하는 deployment와 사용자가 새 기능을 쓰게 만드는 release는 같은 순간일 수도 있지만 분리할 수도 있다. feature flag를 사용하면 새 code를 먼저 배포하고 기능 노출은 나중에 조절할 수 있다.

### 안에서는 실제로 무엇이 일어나는가

이 분리는 위험을 줄일 수 있지만 flag가 오래 쌓이면 복잡도가 증가한다. flag default, 대상 사용자, 만료/삭제 계획, fail-safe behavior를 관리해야 한다.

### 아주 쉬운 예

간단한 flag로 새 계산 경로를 선택한다.

```python
def checkout(total, new_pricing=False):
    if new_pricing:
        return round(total * 0.95)
    return total

assert checkout(10000, False) == 10000
assert checkout(10000, True) == 9500
```

### 한 줄씩 읽기

- 같은 binary/code에 두 경로가 존재한다.
- deployment 후 flag=false면 새 경로는 아직 사용자에게 노출되지 않을 수 있다.
- release는 flag audience 변경으로 별도 제어할 수 있다.

### 직접 실행

1. 두 flag 상태 test를 실행한다.
2. default를 잘못 true로 바꿨을 때 영향 범위를 생각한다.
3. flag 제거 시 old path와 tests를 정리하는 계획을 만든다.


### 일부를 바꿔서 다시 확인하기

payment/security feature처럼 flag로 끄면 안 되는 invariant와 실험 가능한 feature를 구분한다.

### 작은 문제

feature flag가 있으니 rollback이 필요 없어지는가?

### 왜 맞고 왜 틀리는가

아니다. flag로 끌 수 없는 migration/config/runtime bug가 있고 flag system 자체도 실패할 수 있다. binary/config rollback 전략이 별도로 필요하다.

### 자주 만나는 실패와 확인 순서

- flag 조합 폭발을 막기 위해 수명/owner를 둔다.
- client-side flag에 비밀 entitlement 판단을 맡기지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** deployment는 code 배치, release는 사용자 노출이며 feature flag로 분리할 수 있다.
- **직접 코딩·실행해서 익힐 것:** 두 flag 경로를 실제 test한다.
- **AI에게 맡겨도 되는 것:** flag plan/cleanup 후보 생성.
- **사람이 최종 확인할 것:** 어떤 기능을 flag로 안전하게 제어할 수 있는지 결정한다.

### 근거 연결

`GOOGLE-SRE` · `DORA-2025`

---

## CHAPTER 02 · LESSON 02 · canary와 blue-green은 변경 영향 범위를 제한하는 rollout 전략이다

**LESSON ID:** `T10-B15-L02`

### 먼저 쉬운 말로 이해하기

새 version을 한 번에 100% 사용자에게 보내면 결함도 한 번에 퍼진다. canary는 작은 traffic/population에 먼저 노출해 신호를 보고 점진 확대한다. blue-green은 두 환경을 준비하고 traffic을 전환하는 방식이다.

### 안에서는 실제로 무엇이 일어나는가

canary는 control과 candidate를 같은 시간대에 비교하면 시간 변화 noise를 줄일 수 있다. success metric과 자동/수동 중단 기준을 배포 전에 정해야 한다. blue-green은 빠른 traffic reversal 장점이 있지만 두 환경 비용과 data schema compatibility 문제가 있다.

### 아주 쉬운 예

실제 production 배포 대신 1000명의 가상 traffic에서 5% canary 영향 크기를 계산한다.

```python
users = 1000
canary_ratio = 0.05
candidate_users = int(users * canary_ratio)
print(candidate_users)
```

### 한 줄씩 읽기

- 5%면 50명이 candidate에 노출되는 단순 모델이다.
- candidate error 20%라면 전체 error impact는 대략 1%가 될 수 있다.
- 실제 rollout은 사용자 단위/요청 단위 sticky routing 등 세부가 필요하다.

### 직접 실행

1. 계산 script를 실행한다.
2. canary ratio를 1%,10%,50%로 바꿔 blast radius를 비교한다.
3. 확대/중단 기준 metric을 세 개 적는다.


### 일부를 바꿔서 다시 확인하기

control과 canary를 서로 다른 요일에 비교하는 방식이 왜 noise가 큰지 설명한다.

### 작은 문제

canary가 5분간 정상이라 100% 확대했다. 충분한가?

### 왜 맞고 왜 틀리는가

기능 특성에 따라 느린 실패·batch·cache·메모리 누수가 늦게 나타날 수 있다. 관찰 duration과 metric은 위험에 맞춰 정해야 한다.

### 자주 만나는 실패와 확인 순서

- canary population이 대표성이 없으면 특정 사용자군 문제를 놓칠 수 있다.
- blue-green에서도 DB migration이 backward compatible하지 않으면 즉시 rollback이 어려울 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** canary/blue-green은 새 변경의 blast radius와 rollback 시간을 줄이는 rollout 전략이다.
- **직접 코딩·실행해서 익힐 것:** 가상 traffic으로 노출 비율과 영향도를 계산한다.
- **AI에게 맡겨도 되는 것:** rollout stage/metric 제안.
- **사람이 최종 확인할 것:** 대표성·비용·data compatibility를 기준으로 전략을 선택한다.

### 근거 연결

`GOOGLE-SRE-CANARY` · `GOOGLE-SRE`

---

## CHAPTER 03 · LESSON 03 · DB migration은 code rollback과 별도로 forward/backward compatibility를 설계한다

**LESSON ID:** `T10-B15-L03`

### 먼저 쉬운 말로 이해하기

앱 binary를 이전 version으로 되돌려도 DB schema가 이미 바뀌었다면 예전 code가 동작하지 않을 수 있다. 그래서 database migration은 deploy와 함께 가장 조심해야 할 변경 중 하나다.

### 안에서는 실제로 무엇이 일어나는가

안전한 패턴 중 하나가 expand-and-contract다. 먼저 새 column/table을 추가해 old/new code가 함께 동작하게 만들고, data backfill과 code 전환이 끝난 뒤 오래된 schema를 제거한다. destructive migration을 첫 단계에 두지 않는다.

### 아주 쉬운 예

schema 변화 순서를 text state로 표현한다.

```python
steps = [
  "1 expand: add new nullable column",
  "2 deploy code: write old+new / read compatible",
  "3 backfill and verify",
  "4 switch reads",
  "5 contract: remove old column later",
]
print("\n".join(steps))
```

### 한 줄씩 읽기

- expand 단계는 backward compatibility를 만든다.
- backfill은 실제 data change라 별도 검증이 필요하다.
- contract는 old version이 더 이상 필요 없음을 확인한 뒤 수행한다.

### 직접 실행

1. 단계를 출력한다.
2. 2단계에서 새 code 실패로 rollback할 때 old code가 schema와 호환되는지 설명한다.
3. old column을 1단계에서 바로 삭제하면 어떤 rollback 문제가 생기는지 적는다.


### 일부를 바꿔서 다시 확인하기

large table migration에서 lock/time/resource 영향과 batch/backfill observability를 추가로 설계한다.

### 작은 문제

migration SQL이 성공 exit로 끝났으니 data가 모두 올바르다고 말해도 되는가?

### 왜 맞고 왜 틀리는가

아니다. row count, invariant, null/duplicate, sample/aggregate verification 등 data correctness 검사가 별도다.

### 자주 만나는 실패와 확인 순서

- backup이 있다고 rollback이 즉시 가능한 것은 아니다. restore 시간/RPO/RTO를 고려한다.
- irreversible data deletion은 사전 승인과 복구 계획 없이 실행하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** DB migration은 code와 다른 수명의 상태 변경이며 compatibility와 data verification이 필요하다.
- **직접 코딩·실행해서 익힐 것:** expand→backfill→switch→contract 순서를 시뮬레이션한다.
- **AI에게 맡겨도 되는 것:** migration/rollback plan 초안.
- **사람이 최종 확인할 것:** 실제 data loss risk와 rollback 가능성을 확인한다.

### 근거 연결

`GOOGLE-SRE` · `NIST-SSDF-12`

---

## CHAPTER 04 · LESSON 04 · rollback은 “이전 commit으로 돌아가기”보다 넓은 복구 계획이다

**LESSON ID:** `T10-B15-L04`

### 먼저 쉬운 말로 이해하기

production 문제에서 빠른 완화가 root cause 수정보다 먼저일 수 있다. rollback은 이전 binary, config, feature flag, routing을 복구하는 행동을 포함하지만 DB/data/external side effect는 자동으로 되돌아가지 않을 수 있다.

### 안에서는 실제로 무엇이 일어나는가

좋은 rollback plan에는 trigger, 실행 방법, 예상 시간, data compatibility, verification이 있다. rollback 자체도 실패할 수 있으므로 평소 rehearsal이 필요하다.

### 아주 쉬운 예

rollback checklist를 실행 가능한 local text gate로 만든다.

```bash
cat <<'EOF'
TRIGGER: error rate / critical function failure
TARGET: known-good artifact digest
DATA: backward-compatible schema confirmed?
ACTION: traffic/config/binary rollback
VERIFY: smoke + key metrics + user path
EOF
```

### 한 줄씩 읽기

- known-good artifact는 source 이름보다 digest/provenance로 식별한다.
- data compatibility 질문이 code rollback과 별도다.
- rollback 후 verification까지 해야 복구를 확인할 수 있다.

### 직접 실행

1. 가상 release에 checklist를 채운다.
2. rollback target artifact digest를 local metadata에서 찾는다.
3. verification 없이 “rollback command 성공”만으로 종료하지 않는 절차를 적는다.


### 일부를 바꿔서 다시 확인하기

forward-fix가 rollback보다 안전한 경우와 반대 경우를 예로 하나씩 든다.

### 작은 문제

원인을 아직 모르면 rollback하면 안 되는가?

### 왜 맞고 왜 틀리는가

사용자 피해를 줄이는 것이 우선인 incident에서는 known-good 상태로 복귀할 수 있다면 원인 분석 전에 rollback이 합리적일 수 있다. 상황과 data compatibility를 본다.

### 자주 만나는 실패와 확인 순서

- 잘못된 target version으로 rollback하지 않도록 artifact identity를 확인한다.
- rollback 후 queue/retry가 과거 요청을 다시 실행하는 부작용을 확인한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** rollback은 known-good 운영 상태로 복구하고 그 결과를 다시 검증하는 절차다.
- **직접 코딩·실행해서 익힐 것:** 가상 release의 trigger/target/action/verify를 작성한다.
- **AI에게 맡겨도 되는 것:** rollback runbook 초안.
- **사람이 최종 확인할 것:** data/external side effect와 실제 복구 여부를 확인한다.

### 근거 연결

`GOOGLE-SRE` · `SLSA-12`

---

## CHAPTER 05 · LESSON 05 · 배포 후 smoke test와 observability로 “실제로 살아 있는가”를 확인한다

**LESSON ID:** `T10-B15-L05`

### 먼저 쉬운 말로 이해하기

deployment command가 성공해도 앱이 사용자 요청을 정상 처리한다는 보장은 없다. 배포 뒤에는 핵심 경로를 작게 실행하는 smoke test와 error/latency/saturation 같은 운영 signal을 확인한다.

### 안에서는 실제로 무엇이 일어나는가

health endpoint 하나만 200이라고 business function이 정상이라는 뜻은 아니다. 읽기/쓰기/인증 등 가장 중요한 user path를 위험 없이 검사하고, canary/control metric을 비교한다.

### 아주 쉬운 예

로컬 service 대신 작은 함수 health check를 묶어 판정 범위를 연습한다.

```python
checks = {
  "process": True,
  "db_read": True,
  "core_calculation": True,
}
print(all(checks.values()), checks)
```

### 한 줄씩 읽기

- all=True는 적힌 세 check 범위만 통과했다는 뜻이다.
- payment 실제 청구 같은 위험한 smoke는 별도 sandbox/안전 설계가 필요하다.
- 운영 metric은 시간이 지나며 변화하므로 배포 직후만 보지 않는다.

### 직접 실행

1. 세 check를 실행한다.
2. 하나를 False로 바꿔 overall이 fail 되는지 본다.
3. 자기 앱 핵심 user journey 3개를 destructive하지 않은 smoke로 설계한다.


### 일부를 바꿔서 다시 확인하기

error rate는 정상인데 latency만 급증하는 경우, process health만으로 놓치는 이유를 설명한다.

### 작은 문제

배포 후 200 health check 하나 성공했다. “운영 PASS”라고 써도 되는가?

### 왜 맞고 왜 틀리는가

안 된다. 그 endpoint availability 범위의 evidence다. 핵심 기능·data·latency·사용자 경로는 별도다.

### 자주 만나는 실패와 확인 순서

- smoke test가 production data를 생성/삭제하지 않도록 격리한다.
- alert가 너무 늦거나 noise가 많으면 rollback 판단이 지연된다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 배포 성공과 release 건강성은 다르며 smoke/metrics로 실제 user path를 확인한다.
- **직접 코딩·실행해서 익힐 것:** 여러 health signal을 묶어 부분 실패를 만든다.
- **AI에게 맡겨도 되는 것:** smoke/metric dashboard 초안.
- **사람이 최종 확인할 것:** 무엇을 검증했는지와 사용자 영향 신호를 확인한다.

### 근거 연결

`GOOGLE-SRE-CANARY` · `DORA-2025`

---

## CHAPTER 06 · LESSON 06 · incident에서는 완벽한 원인보다 피해 완화와 증거 보존을 먼저 정렬한다

**LESSON ID:** `T10-B15-L06`

### 먼저 쉬운 말로 이해하기

운영 장애가 진행 중일 때 개발자가 모두 root cause를 찾는 데 몰두하면 사용자 피해가 계속될 수 있다. incident response는 탐지→영향 범위→완화→복구→원인 분석 순서를 분리한다.

### 안에서는 실제로 무엇이 일어나는가

역할을 나누고 timeline을 남기며, 변경을 통제한다. rollback/feature disable/traffic shift로 피해를 줄인 뒤 forensic evidence를 보존하고 원인을 분석한다. postmortem은 사람 비난보다 시스템 조건과 재발 방지 action을 찾는다.

### 아주 쉬운 예

간단한 incident timeline 구조를 만든다.

```python
timeline = [
 ("10:02", "alert: error rate high"),
 ("10:05", "impact confirmed"),
 ("10:08", "rollback started"),
 ("10:12", "error rate recovered"),
]
for t, e in timeline: print(t, e)
```

### 한 줄씩 읽기

- 시간순 기록은 나중에 detection/mitigation delay를 계산할 수 있다.
- 완화 성공과 root cause 확정은 다른 상태다.
- postmortem action은 owner와 검증 가능한 완료 조건이 필요하다.

### 직접 실행

1. timeline을 실행한다.
2. 10:04에 unrelated deploy가 하나 있었다는 정보를 추가하고 FACT/HYPOTHESIS로 분리한다.
3. “누가 실수했나” 대신 어떤 guardrail이 없었는지 질문을 바꾼다.


### 일부를 바꿔서 다시 확인하기

MTTD/MTTR 같은 지표가 사람 평가 점수로 오용될 때 생기는 문제와 시스템 개선 지표로 쓸 때의 차이를 적는다.

### 작은 문제

rollback으로 복구됐으니 incident는 끝났고 더 할 일이 없는가?

### 왜 맞고 왜 틀리는가

사용자 영향은 끝났을 수 있지만 root cause, hidden data damage, recurrence prevention, action tracking이 남는다.

### 자주 만나는 실패와 확인 순서

- incident 중 여러 사람이 동시에 production 변경을 하지 않도록 change coordination을 둔다.
- postmortem에 민감 사용자 정보를 필요 이상 기록하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** incident에서는 사용자 영향 완화, 복구 확인, 이후 원인/재발 방지를 분리한다.
- **직접 코딩·실행해서 익힐 것:** timeline과 FACT/HYPOTHESIS를 작성한다.
- **AI에게 맡겨도 되는 것:** timeline/postmortem 초안.
- **사람이 최종 확인할 것:** 위험한 운영 행동 승인과 root cause/action을 최종 판단한다.

### 근거 연결

`GOOGLE-SRE` · `DORA-2025`

---

## CHAPTER 07 · LESSON 07 · AI가 만든 변경도 같은 검증 게이트를 통과시킨다

**LESSON ID:** `T10-B15-L07`

### 먼저 쉬운 말로 이해하기

AI가 코드를 빨리 만들 수 있어도 AI가 “테스트했습니다”라고 말한 문장은 실제 실행 증거가 아니다. 사람이 직접 모든 boilerplate를 쓸 필요는 없지만, 변경 의도·diff·test scope·security·release/rollback은 증거로 확인해야 한다.

### 안에서는 실제로 무엇이 일어나는가

2025 DORA 연구는 AI가 조직의 기존 강점과 약점을 증폭하는 성격을 보고한다. 검증 체계가 좋은 팀에서는 속도를 활용할 수 있지만, gate가 약하면 더 많은 변경이 더 빨리 위험하게 들어갈 수 있다. 따라서 AI output도 동일한 source review, tests, CI, provenance, rollout 원칙을 따른다.

### 아주 쉬운 예

Track10 전체를 묶는 local capstone checklist를 만든다.

```bash
cat <<'EOF'
1 reproduce: 실패를 실제 재현했는가
2 test: regression test가 수정 전 fail / 수정 후 pass인가
3 diff: 의도한 변경만 있는가
4 git: source revision이 식별되는가
5 build: artifact를 실제 만들었는가
6 evidence: test/build 결과가 artifact와 연결되는가
7 rollout: 실패 시 rollback/verification 계획이 있는가
EOF
```

### 한 줄씩 읽기

- AI는 1~7의 초안을 빠르게 만들 수 있다.
- 실제 실행 여부는 tool output/exit/report로 확인한다.
- 외부 deploy/write는 비용·위험 규칙에 따라 별도 승인한다.

### 직접 실행

1. 이 패키지의 local validation script를 실제 실행한다.
2. 실패 하나를 의도적으로 만들어 gate가 red 되는지 확인한 뒤 원복한다.
3. 미검증 항목은 PASS 대신 UNVERIFIED/PREPARED로 남긴다.


### 일부를 바꿔서 다시 확인하기

AI에게 root cause 후보를 세 개 제시하게 한 뒤 각각 어떤 evidence로 반증할지 사람이 실험 계획을 만드는 연습을 한다.

### 작은 문제

AI가 생성한 test 200개가 green이면 사람이 코드를 이해하지 않아도 merge해도 되는가?

### 왜 맞고 왜 틀리는가

안 된다. test가 요구사항을 제대로 표현하는지, 빠진 위험이 없는지, code가 안전한지, 운영에서 되돌릴 수 있는지는 사람의 책임 있는 판단이 필요하다.

### 자주 만나는 실패와 확인 순서

- AI가 존재하지 않는 command/API를 제안할 수 있으므로 공식 문서/실행으로 확인한다.
- AI에게 secret, private production log, 개인정보를 무심코 보내지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** AI는 작성·탐색·요약을 가속할 수 있지만 검증 증거와 변경 책임을 대신하지 않는다.
- **직접 코딩·실행해서 익힐 것:** Track10 전체 local gate를 실제 실행하고 미검증 범위를 남긴다.
- **AI에게 맡겨도 되는 것:** 코드/test/runbook 초안과 log 요약.
- **사람이 최종 확인할 것:** 요구사항·증거·위험 수용·외부 write 승인·최종 PASS 판정을 사람이 한다.

### 근거 연결

`DORA-2025` · `NIST-SSDF-12` · `TEXTBOOK-V5-CONTRACT`

---
