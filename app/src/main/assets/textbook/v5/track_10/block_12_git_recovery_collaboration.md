# TRACK 10 · 오류·테스트·Git·빌드·배포 — 변경을 안전하게 만드는 기술

# BLOCK 12 · Git 복구·remote 협업·review 안전장치

실수 복구와 협업은 같은 “되돌리기” 명령으로 해결되지 않는다. restore/revert/reset/reflog의 범위를 나누고 fetch/push/PR/review/보호 정책을 외부 write 위험과 연결한다.


```text
개념 설명 → 아주 쉬운 예 → 한 줄씩 해석 → 직접 실행 → 일부 수정 → 작은 문제 → 왜 맞고 틀렸는지 설명
```

---

## CHAPTER 01 · LESSON 01 · restore는 파일 상태를 되돌리고 commit history와는 구분한다

**LESSON ID:** `T10-B12-L01`

### 먼저 쉬운 말로 이해하기

작업 중 파일을 잘못 수정했을 때 “되돌리기”에도 여러 층이 있다. `git restore`는 working tree나 index의 파일 상태를 지정한 source에 맞추는 도구다. 이미 공유된 commit을 취소하는 `revert`와는 목적이 다르다.

### 안에서는 실제로 무엇이 일어나는가

`git restore file`은 기본적으로 working tree 변경을 버릴 수 있으므로 실행 전 diff 확인이 중요하다. `--staged`는 index에서 unstage하는 용도로 사용할 수 있다.

### 아주 쉬운 예

연습 repo에서 수정→diff→restore를 한다.

```bash
echo accidental >> note.txt
git diff -- note.txt
git restore note.txt
git status --short
```

### 한 줄씩 읽기

- restore 전 diff로 버릴 내용을 확인한다.
- restore 뒤 working tree가 HEAD/index 상태로 돌아간다.
- uncommitted change는 복구가 어려울 수 있으므로 실제 파일에서 무심코 실행하지 않는다.

### 직접 실행

1. 임시 repo에서만 실행한다.
2. staged 변경을 `git restore --staged`로 unstage한다.
3. working tree 내용이 유지되는지 확인한다.


### 일부를 바꿔서 다시 확인하기

IDE local history가 있는 경우와 Git tracked history의 차이를 구분한다.

### 작은 문제

이미 remote에 push한 잘못된 commit을 `git restore`로 해결할 수 있는가?

### 왜 맞고 왜 틀리는가

파일을 local에서 바꾸는 것은 가능하지만 공유 history의 취소 기록은 별도다. 보통 새 revert/fix commit 전략을 검토한다.

### 자주 만나는 실패와 확인 순서

- restore 전 항상 status/diff를 본다.
- untracked file은 restore 대상과 다르므로 별도 삭제 명령을 함부로 쓰지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** restore는 주로 working tree/index 파일 상태를 되돌리는 도구이며 공유 commit 취소와 다르다.
- **직접 코딩·실행해서 익힐 것:** 임시 repo에서 unstaged/staged 복구를 직접 본다.
- **AI에게 맡겨도 되는 것:** 안전한 복구 명령 후보 제안.
- **사람이 최종 확인할 것:** 버려질 uncommitted data가 없는지 확인한다.

### 근거 연결

`GIT-RESTORE` · `GIT-DOC`

---

## CHAPTER 02 · LESSON 02 · revert는 과거 commit을 지우지 않고 반대 변경을 새 commit으로 남긴다

**LESSON ID:** `T10-B12-L02`

### 먼저 쉬운 말로 이해하기

이미 공유된 history에서 잘못된 변경을 취소할 때 과거 commit을 없애기보다 **반대 변경을 새 commit으로 추가**하는 것이 협업에 안전한 경우가 많다. `git revert`가 이 역할을 한다.

### 안에서는 실제로 무엇이 일어나는가

revert는 target commit이 만든 patch의 반대를 현재 history에 적용한다. history는 그대로 남아 “추가→취소” 흐름이 추적된다. 이후 다시 필요하면 revert의 revert 같은 전략도 가능하다.

### 아주 쉬운 예

연습 repo에서 마지막 commit을 revert한다.

```bash
git log --oneline -3
git revert --no-edit HEAD
git log --oneline -3
git show --stat HEAD
```

### 한 줄씩 읽기

- 원래 commit은 history에서 사라지지 않는다.
- 새 revert commit이 추가된다.
- 현재 file content는 target 변경을 취소한 상태가 된다.

### 직접 실행

1. 임시 repo에서 feature commit을 만든다.
2. 그 commit을 revert한다.
3. log에 두 commit이 모두 존재하는지 확인한다.


### 일부를 바꿔서 다시 확인하기

merge commit revert는 parent 선택 의미가 추가되어 더 복잡하므로 처음 배우는 사람이 실제 shared repo에서 임의 실행하지 않고 별도 연습에서 개념만 확인한다.

### 작은 문제

revert하면 database migration 같은 외부 side effect도 자동으로 원상복구되는가?

### 왜 맞고 왜 틀리는가

아니다. Git은 source change를 다룬다. 배포된 DB/data/external action은 별도 rollback/compensation이 필요하다.

### 자주 만나는 실패와 확인 순서

- revert 대상이 이후 commit과 충돌할 수 있다.
- security incident에서는 코드 revert만으로 leaked credential을 회수할 수 없다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** revert는 commit history를 보존하면서 반대 변경을 새 commit으로 만든다.
- **직접 코딩·실행해서 익힐 것:** commit→revert history를 실제 확인한다.
- **AI에게 맡겨도 되는 것:** revert impact 분석.
- **사람이 최종 확인할 것:** source 외 side effect rollback을 별도로 확인한다.

### 근거 연결

`GIT-REVERT` · `GOOGLE-SRE`

---

## CHAPTER 03 · LESSON 03 · reset은 reference와 index/working tree를 움직이는 강력한 도구라 범위를 이해하고 쓴다

**LESSON ID:** `T10-B12-L03`

### 먼저 쉬운 말로 이해하기

`git reset`은 옵션에 따라 branch pointer, index, working tree를 함께 움직일 수 있어 강력하다. 특히 `--hard`는 uncommitted 작업을 잃게 할 수 있으므로 “되돌리기 만능 명령”처럼 쓰면 위험하다.

### 안에서는 실제로 무엇이 일어나는가

soft/mixed/hard는 대략 어디까지 상태를 맞추는지가 다르다. 초급 실전에서는 shared history 수정보다 연습 repo에서 상태 변화를 먼저 이해하고, 협업 commit 취소에는 revert를 우선 검토한다.

### 아주 쉬운 예

세 단계의 차이는 실제 데이터 손실 위험 때문에 명령 실행을 temp repo로 제한한다.

```bash
# 연습 repo에서만
before=$(git rev-parse HEAD)
git reset --soft HEAD~1
git status --short
# 상태를 관찰한 뒤 원래 위치 복구
```

### 한 줄씩 읽기

- soft는 branch pointer를 움직이되 index/working tree를 남긴다.
- 기본 mixed는 index도 조정한다.
- hard는 working tree까지 바꾸므로 가장 파괴적일 수 있다.

### 직접 실행

1. 전용 temp repo와 백업 branch를 만든다.
2. soft/mixed를 각각 실행해 status 차이를 기록한다.
3. hard는 별도 복제 temp repo에서만 실행하고 잃는 내용을 사전 diff로 확인한다.


### 일부를 바꿔서 다시 확인하기

같은 상황을 restore/revert/reset 각각으로 처리할 때 무엇이 달라지는지 표로 만든다.

### 작은 문제

remote main의 잘못된 commit을 없애려고 바로 `reset --hard` 후 force push해도 되는가?

### 왜 맞고 왜 틀리는가

공유 history를 파괴적으로 rewrite할 수 있으므로 승인/정책 없이 하면 안 된다. revert 등 비파괴 취소를 먼저 검토한다.

### 자주 만나는 실패와 확인 순서

- 실제 사용자 작업 폴더에서 실험하지 않는다.
- untracked 파일은 reset 동작과 별도이므로 clean 명령까지 결합하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** reset은 ref/index/working tree 상태를 option에 따라 움직이며 파괴 가능성이 있다.
- **직접 코딩·실행해서 익힐 것:** temp repo에서 옵션별 status 차이를 본다.
- **AI에게 맡겨도 되는 것:** 상황별 restore/revert/reset 비교.
- **사람이 최종 확인할 것:** 공유 history/미commit data 위험을 최종 확인한다.

### 근거 연결

`GIT-RESET` · `GIT-DOC`

---

## CHAPTER 04 · LESSON 04 · reflog는 로컬 reference 이동 기록으로 실수 복구 단서를 준다

**LESSON ID:** `T10-B12-L04`

### 먼저 쉬운 말로 이해하기

branch를 잘못 reset하거나 commit을 잃어버린 것처럼 보여도 Git object가 즉시 사라지는 것은 아니다. reflog는 로컬에서 HEAD/branch가 어디를 가리켰는지 이동 기록을 보여 복구 단서를 줄 수 있다.

### 안에서는 실제로 무엇이 일어나는가

reflog는 영구 백업이나 remote history가 아니다. 보존 기간과 GC가 있고 다른 개발자 컴퓨터에는 같은 reflog가 없다. 그래서 “언제든 복구 가능”을 믿고 위험 명령을 써서는 안 된다.

### 아주 쉬운 예

연습 repo에서 commit 후 pointer 이동을 reflog로 본다.

```bash
git reflog --oneline -10
```

### 한 줄씩 읽기

- checkout/reset/commit 같은 reference 이동이 보일 수 있다.
- 이전 SHA를 찾아 temporary branch를 만들어 복구할 수 있다.
- reflog는 로컬 repository별 기록이다.

### 직접 실행

1. temp repo에서 commit SHA를 기록한다.
2. soft reset 등 안전한 pointer 이동 뒤 reflog에서 이전 SHA를 찾는다.
3. `git branch rescue <sha>`로 복구 pointer를 만든다.


### 일부를 바꿔서 다시 확인하기

dangling commit과 garbage collection 개념은 고급 세부로 깊게 가지 않고 “즉시 영구 삭제는 아닐 수 있지만 보장된 백업도 아니다”까지만 기억한다.

### 작은 문제

reflog가 있으니 backup이 필요 없는가?

### 왜 맞고 왜 틀리는가

아니다. reflog는 로컬 Git reference 이동 기록일 뿐 장기·재해 복구 backup을 대체하지 않는다.

### 자주 만나는 실패와 확인 순서

- 복구하려는 SHA를 찾기 전에 추가 파괴 작업을 최소화한다.
- remote repository 장애/삭제는 별도 backup 정책이 필요하다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** reflog는 로컬 ref 이동 history로 최근 실수 복구 단서를 제공할 수 있다.
- **직접 코딩·실행해서 익힐 것:** pointer 이동 후 이전 SHA를 찾아 rescue branch를 만든다.
- **AI에게 맡겨도 되는 것:** recovery 명령 안내.
- **사람이 최종 확인할 것:** 복구 가능성을 과신하지 않고 backup/협업 범위를 확인한다.

### 근거 연결

`GIT-DOC`

---

## CHAPTER 05 · LESSON 05 · fetch·pull·push를 remote 상태 이동으로 구분한다

**LESSON ID:** `T10-B12-L05`

### 먼저 쉬운 말로 이해하기

remote 협업에서 `pull`만 외우면 실제로 어떤 단계가 일어났는지 놓치기 쉽다. `fetch`는 remote 정보를 local remote-tracking refs로 가져오고, `pull`은 보통 fetch 뒤 merge/rebase 같은 통합을 수행한다. `push`는 local commit을 remote ref에 반영하려 한다.

### 안에서는 실제로 무엇이 일어나는가

network 명령은 external write 여부가 달라진다. fetch는 읽기 성격이지만 push는 외부 repository를 변경한다. 이 프로젝트 규칙처럼 외부 write는 사전 승인이 필요한 환경에서는 명령 의미를 구분해야 한다.

### 아주 쉬운 예

로컬 bare repository를 remote처럼 만들어 network 없이 fetch/push 원리를 연습한다.

```bash
git remote -v
git fetch origin
git log --oneline --decorate --all
# 실제 외부 push는 승인 없이 실행하지 않는다.
```

### 한 줄씩 읽기

- remote-tracking branch는 마지막 fetch 때 본 remote 상태다.
- fetch 자체로 현재 working branch가 자동 merge되는 것은 아니다.
- push는 remote state를 바꾸므로 외부 write다.

### 직접 실행

1. 패키지 test harness의 local bare remote에서 fetch/push를 연습한다.
2. fetch 전후 remote-tracking ref 변화를 확인한다.
3. 실제 GitHub에는 write하지 않는다.


### 일부를 바꿔서 다시 확인하기

`git pull --rebase`와 기본 pull 정책이 team history에 어떤 영향을 주는지 설정을 확인한다.

### 작은 문제

`origin/main`이 최신이라고 생각했는데 며칠 fetch하지 않았다. 실제 GitHub main과 같다고 볼 수 있는가?

### 왜 맞고 왜 틀리는가

아니다. remote-tracking ref는 마지막 fetch 시점 snapshot일 수 있다. 최신 여부를 확인하려면 fetch/read가 필요하다.

### 자주 만나는 실패와 확인 순서

- push --force는 공유 history를 덮을 수 있어 정책 확인이 필요하다.
- remote URL과 credential을 로그에 노출하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** fetch는 remote 정보를 가져오고 push는 remote ref를 변경하며 pull은 fetch+통합의 조합이다.
- **직접 코딩·실행해서 익힐 것:** local bare remote로 상태 변화를 안전하게 실습한다.
- **AI에게 맡겨도 되는 것:** remote divergence 요약.
- **사람이 최종 확인할 것:** 외부 write 승인과 협업 정책을 확인한다.

### 근거 연결

`GIT-BOOK` · `GIT-DOC`

---

## CHAPTER 06 · LESSON 06 · PR과 code review는 diff를 사람의 의도와 위험에 연결한다

**LESSON ID:** `T10-B12-L06`

### 먼저 쉬운 말로 이해하기

Pull Request는 단순히 “merge 버튼을 누르는 화면”이 아니다. 변경 이유, diff, test evidence, 위험, rollout/rollback 정보를 리뷰 가능한 단위로 모으는 협업 경계다.

### 안에서는 실제로 무엇이 일어나는가

reviewer는 style만 보는 것이 아니라 요구사항, edge case, security, migration, observability, test adequacy를 본다. 자동 CI는 반복 검사를 맡고 사람은 맥락과 의도·trade-off를 판단한다.

### 아주 쉬운 예

로컬에서 PR 제출 전 체크리스트를 text로 만든다.

```bash
cat <<'EOF'
WHY: 어떤 문제를 해결하는가
DIFF: 예상한 파일만 바뀌었는가
TEST: 실제 실행한 명령과 결과는 무엇인가
RISK: 실패하면 무엇이 깨지는가
ROLLBACK: 어떻게 안전하게 되돌리는가
EOF
```

### 한 줄씩 읽기

- WHY는 변경 목적이다.
- DIFF는 scope 검증이다.
- TEST는 실제 evidence다.
- RISK/ROLLBACK은 운영 변경에서 중요하다.

### 직접 실행

1. 자기 로컬 change를 가정해 다섯 항목을 채운다.
2. test를 실행하지 않았다면 “미실행”이라고 적는다.
3. AI가 작성한 diff라면 이해하지 못한 부분을 표시하고 직접 검토한다.


### 일부를 바꿔서 다시 확인하기

migration이 있는 PR에는 forward/backward compatibility와 rollback 가능성을 추가한다.

### 작은 문제

CI가 green이면 reviewer가 diff를 읽지 않아도 되는가?

### 왜 맞고 왜 틀리는가

안 된다. 자동 test가 포착하지 못한 requirement·security·운영 의미가 있고, 잘못된 test 자체가 green일 수 있다.

### 자주 만나는 실패와 확인 순서

- 거대한 PR은 review 품질을 떨어뜨릴 수 있다.
- “LGTM”만 남기고 실제 evidence/위험을 보지 않는 형식적 review를 피한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** PR/review는 change의 의도·증거·위험을 사람과 자동화가 함께 검토하는 경계다.
- **직접 코딩·실행해서 익힐 것:** 로컬 diff와 test evidence로 review checklist를 작성한다.
- **AI에게 맡겨도 되는 것:** PR summary/review questions 생성.
- **사람이 최종 확인할 것:** merge 여부와 위험 수용은 사람이 판단한다.

### 근거 연결

`DORA-2025` · `NIST-SSDF-12`

---

## CHAPTER 07 · LESSON 07 · force push·branch protection·승인 규칙으로 공유 history를 보호한다

**LESSON ID:** `T10-B12-L07`

### 먼저 쉬운 말로 이해하기

혼자 쓰는 연습 branch와 여러 사람이 의존하는 main은 위험도가 다르다. shared branch에는 force push 제한, required review/checks 같은 보호 정책을 둘 수 있다.

### 안에서는 실제로 무엇이 일어나는가

`--force-with-lease`는 단순 force보다 remote가 내가 예상한 위치인지 확인하는 안전장치를 제공하지만, 그래도 history rewrite라는 본질은 같다. 정책상 허용된 개인 branch가 아니라면 무단 사용하지 않는다.

### 아주 쉬운 예

실제 GitHub 설정을 바꾸지 않고 local refs에서 “expected old SHA” 개념을 연습한다.

```bash
old=$(git rev-parse refs/remotes/origin/main 2>/dev/null || true)
echo "expected_remote=$old"
# 외부 force push는 실행하지 않는다.
```

### 한 줄씩 읽기

- lease 사고는 “remote가 내가 마지막으로 본 상태와 같은가”를 확인한다.
- branch protection은 server-side policy로 위험 작업을 막을 수 있다.
- 정책 설정 자체도 외부 시스템 변경이므로 승인 대상일 수 있다.

### 직접 실행

1. local bare remote에서 non-fast-forward push가 거부되는 상황을 만든다.
2. 강제 옵션 없이 실패를 관찰한다.
3. 실제 외부 repository에는 push하지 않는다.


### 일부를 바꿔서 다시 확인하기

required checks가 어떤 SHA에 적용되는지와 admin bypass가 있는지 policy 문서를 읽는 연습을 한다.

### 작은 문제

`--force-with-lease`를 쓰면 main force push도 항상 안전한가?

### 왜 맞고 왜 틀리는가

아니다. 예상 ref check를 추가할 뿐 다른 사람 history를 rewrite할 수 있다. repository 정책과 승인이 우선이다.

### 자주 만나는 실패와 확인 순서

- branch protection를 임시로 끄는 운영 변경은 별도 승인·감사가 필요하다.
- 자동 bot credential에 과도한 write 권한을 주지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 공유 branch는 기술 명령뿐 아니라 server policy·review·required checks로 보호한다.
- **직접 코딩·실행해서 익힐 것:** local remote에서 fast-forward/non-fast-forward 차이를 실습한다.
- **AI에게 맡겨도 되는 것:** policy 설명과 안전 옵션 안내.
- **사람이 최종 확인할 것:** 외부 write와 history rewrite 승인 여부를 확인한다.

### 근거 연결

`GIT-DOC` · `NIST-SSDF-12`

---
