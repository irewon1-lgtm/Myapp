# TRACK 10 · 오류·테스트·Git·빌드·배포 — 변경을 안전하게 만드는 기술

# BLOCK 11 · branch·merge·rebase·conflict

여러 변경이 동시에 진행될 때 Git은 history를 결합하지만 제품 의도까지 자동으로 결정하지는 못한다. branch pointer, merge graph, conflict, rebase, cherry-pick을 실제 history 변화로 익힌다.


```text
개념 설명 → 아주 쉬운 예 → 한 줄씩 해석 → 직접 실행 → 일부 수정 → 작은 문제 → 왜 맞고 틀렸는지 설명
```

---

## CHAPTER 01 · LESSON 01 · branch는 파일 복사본이 아니라 commit을 가리키는 이름이다

**LESSON ID:** `T10-B11-L01`

### 먼저 쉬운 말로 이해하기

새 branch를 만들면 프로젝트 폴더 전체를 별도 복사하는 것이 아니다. branch는 특정 commit을 가리키고, 새 commit을 만들면 현재 branch pointer가 앞으로 이동한다.

### 안에서는 실제로 무엇이 일어나는가

HEAD는 보통 현재 branch를 가리키고 branch는 commit을 가리킨다. 이 mental model을 가지면 “branch를 바꿨더니 파일이 왜 바뀌지?”를 history 이동으로 이해할 수 있다.

### 아주 쉬운 예

임시 repo에서 feature branch를 만든다.

```bash
git switch -c feature/message
echo feature >> note.txt
git add note.txt
git commit -m "add feature message"
git log --oneline --decorate --graph --all
```

### 한 줄씩 읽기

- `switch -c`는 branch를 만들고 그 branch로 이동한다.
- 새 commit은 feature pointer만 앞으로 이동시킨다.
- `--all` graph에서 다른 branch pointer와 분기를 볼 수 있다.

### 직접 실행

1. main에서 baseline commit을 만든 뒤 feature branch를 만든다.
2. feature commit 후 main으로 돌아가 note.txt 차이를 본다.
3. graph에서 두 branch가 가리키는 commit을 확인한다.


### 일부를 바꿔서 다시 확인하기

main에도 다른 commit을 추가해 두 branch가 갈라지는 모습을 만든다.

### 작은 문제

branch를 삭제하면 그 commit 내용이 즉시 영원히 사라지는가?

### 왜 맞고 왜 틀리는가

다른 ref가 commit을 가리키는지와 reflog/GC 상태에 따라 다르다. branch는 pointer이므로 삭제 의미와 data 보존을 구분한다.

### 자주 만나는 실패와 확인 순서

- uncommitted 변경이 branch switch를 방해하거나 따라갈 수 있으므로 status를 먼저 본다.
- 실제 공유 branch 삭제는 협업 영향이 있으니 승인 없이 하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** branch는 독립 파일 복사본이 아니라 history의 commit pointer다.
- **직접 코딩·실행해서 익힐 것:** 분기 graph를 실제 만들고 pointer 이동을 본다.
- **AI에게 맡겨도 되는 것:** branch graph 설명.
- **사람이 최종 확인할 것:** 공유 branch 수명과 변경 범위를 확인한다.

### 근거 연결

`GIT-BOOK` · `GIT-DOC`

---

## CHAPTER 02 · LESSON 02 · fast-forward와 merge commit의 차이를 history 그림으로 본다

**LESSON ID:** `T10-B11-L02`

### 먼저 쉬운 말로 이해하기

두 branch를 합칠 때 history 모양에 따라 단순히 pointer만 앞으로 움직일 수도 있고, 두 parent를 가진 merge commit이 생길 수도 있다. 둘을 모두 “merge 됐다”라고만 보면 history가 왜 다른지 이해하기 어렵다.

### 안에서는 실제로 무엇이 일어나는가

target branch 이후 다른 commit이 없고 feature가 그 앞에 직선으로 이어지면 fast-forward가 가능하다. 양쪽이 모두 진행됐다면 공통 ancestor를 기준으로 세 방향 변경을 합치고 merge commit을 만들 수 있다.

### 아주 쉬운 예

연습 repo에서 직선 history를 fast-forward한다.

```bash
git switch main
git merge feature/message
git log --oneline --graph --decorate --all
```

### 한 줄씩 읽기

- main이 feature ancestor라면 pointer가 feature commit까지 이동할 수 있다.
- 새 merge commit이 반드시 생기는 것은 아니다.
- `--no-ff` 정책을 쓰면 의도적으로 merge commit을 만들 수도 있다.

### 직접 실행

1. fast-forward 사례를 실제 실행한다.
2. 새 branch 두 개를 갈라 각자 commit한 뒤 merge commit 사례를 만든다.
3. 두 graph를 비교한다.


### 일부를 바꿔서 다시 확인하기

team이 merge commit/linear history 중 어떤 정책을 원하는지 이유와 함께 적는다. 도구 default를 절대 법칙으로 외우지 않는다.

### 작은 문제

merge commit이 생겼으니 conflict가 있었다는 뜻인가?

### 왜 맞고 왜 틀리는가

아니다. 서로 다른 파일을 바꿔 자동 merge돼도 merge commit은 생길 수 있다. conflict는 자동 결합이 불가능한 변경 충돌이다.

### 자주 만나는 실패와 확인 순서

- merge 전 tests/status를 확인한다.
- 자동 merge 성공이 semantic correctness를 보장하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** fast-forward는 pointer 이동으로 합칠 수 있는 직선 관계이고, divergent history는 merge commit이 생길 수 있다.
- **직접 코딩·실행해서 익힐 것:** 두 history 모양을 직접 만든다.
- **AI에게 맡겨도 되는 것:** graph 해석.
- **사람이 최종 확인할 것:** 팀 history 정책과 semantic 검증을 판단한다.

### 근거 연결

`GIT-BOOK`

---

## CHAPTER 03 · LESSON 03 · merge conflict는 Git이 사람의 의도를 결정할 수 없다는 신호다

**LESSON ID:** `T10-B11-L03`

### 먼저 쉬운 말로 이해하기

같은 줄이나 관련 영역이 서로 다르게 바뀌면 Git은 어느 쪽이 정답인지 모른다. conflict 해결은 marker를 지우는 작업이 아니라 두 변경의 **의도**를 이해해 최종 behavior를 결정하는 일이다.

### 안에서는 실제로 무엇이 일어나는가

ours/theirs를 기계적으로 고르는 대신 base, 양쪽 diff, tests, 요구사항을 본다. conflict 파일이 compile되더라도 semantic conflict가 남을 수 있다.

### 아주 쉬운 예

같은 한 줄을 두 branch에서 다르게 바꾼 뒤 merge해 conflict marker를 관찰한다.

```bash
# branch A: greeting = "hello"
# branch B: greeting = "hi"
# merge 후 예시 marker
# <<<<<<< HEAD
# greeting = "hello"
# =======
# greeting = "hi"
# >>>>>>> feature
```

### 한 줄씩 읽기

- HEAD 쪽과 merge 대상 쪽 변경을 구분한다.
- marker는 최종 코드가 아니므로 남기면 안 된다.
- 정답은 “둘 중 하나”일 수도, 두 의도를 결합한 제3의 코드일 수도 있다.

### 직접 실행

1. 임시 repo에서 실제 conflict를 만든다.
2. `git status`로 unmerged path를 확인한다.
3. 요구 behavior를 정해 파일을 수정하고 add→commit 후 tests를 실행한다.


### 일부를 바꿔서 다시 확인하기

text conflict는 없지만 한 branch가 함수 이름을 바꾸고 다른 branch가 그 호출 semantics를 바꾼 “semantic conflict” 사례를 적는다.

### 작은 문제

Git이 auto-merge에 성공했으면 사람 검토가 불필요한가?

### 왜 맞고 왜 틀리는가

아니다. 줄 단위 충돌이 없다는 뜻일 뿐 두 기능 조합이 올바른지는 test/review가 필요하다.

### 자주 만나는 실패와 확인 순서

- conflict 해결 중 다른 unrelated change를 실수로 넣지 않도록 diff를 본다.
- generated lockfile conflict는 도구별 올바른 재생성 절차를 따른다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** conflict는 Git이 변경 의도를 자동 결정할 수 없는 상태이며 최종 의미는 사람이 결정한다.
- **직접 코딩·실행해서 익힐 것:** 실제 conflict를 만들고 status/diff/test까지 수행한다.
- **AI에게 맡겨도 되는 것:** 양쪽 diff 요약.
- **사람이 최종 확인할 것:** 요구사항과 tests로 최종 semantic resolution을 판단한다.

### 근거 연결

`GIT-BOOK` · `DORA-2025`

---

## CHAPTER 04 · LESSON 04 · rebase는 commit을 새 base 위에 다시 적용해 history를 다시 쓴다

**LESSON ID:** `T10-B11-L04`

### 먼저 쉬운 말로 이해하기

rebase를 “깔끔하게 만드는 명령”으로만 외우면 위험하다. 기존 commit의 patch를 새 base 뒤에 다시 적용해 **새 commit identity**를 만드는 history rewrite다.

### 안에서는 실제로 무엇이 일어나는가

공유되지 않은 local feature history를 최신 main 위에 정리할 때 유용할 수 있다. 이미 여러 사람이 기반으로 삼은 public commit을 rebase하고 force push하면 다른 사람 history와 충돌할 수 있다.

### 아주 쉬운 예

연습 branch에서 base를 바꾸고 SHA가 바뀌는지 본다.

```bash
git switch feature/demo
git rebase main
git log --oneline --graph --decorate --all
```

### 한 줄씩 읽기

- rebase 전 feature commit의 SHA를 기록한다.
- 같은 논리 변경이라도 새 parent 위에 적용되어 SHA가 달라질 수 있다.
- conflict가 나면 resolve→`git rebase --continue` 또는 중단은 `--abort`로 처리한다.

### 직접 실행

1. 임시 repo에서 rebase 전 SHA를 저장한다.
2. rebase 후 SHA를 비교한다.
3. public branch라 가정했을 때 왜 coordination이 필요한지 설명한다.


### 일부를 바꿔서 다시 확인하기

merge 방식과 rebase 방식으로 같은 최종 file content를 만들고 history graph 차이를 비교한다.

### 작은 문제

rebase 후 최종 파일이 같으니 commit SHA도 같아야 하는가?

### 왜 맞고 왜 틀리는가

아니다. parent와 metadata가 달라지므로 commit object identity가 달라질 수 있다.

### 자주 만나는 실패와 확인 순서

- 공유 branch를 승인 없이 force push하지 않는다.
- rebase 중 conflict를 자동으로 한쪽만 선택해 semantic change를 잃지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** rebase는 commit을 새 base에 다시 적용해 새 history를 만드는 작업이다.
- **직접 코딩·실행해서 익힐 것:** rebase 전후 graph와 SHA를 직접 비교한다.
- **AI에게 맡겨도 되는 것:** rebase plan/충돌 요약.
- **사람이 최종 확인할 것:** 공유 history rewrite가 안전한지 팀 정책을 확인한다.

### 근거 연결

`GIT-BOOK`

---

## CHAPTER 05 · LESSON 05 · cherry-pick은 특정 commit의 변경을 현재 branch에 새 commit으로 적용한다

**LESSON ID:** `T10-B11-L05`

### 먼저 쉬운 말로 이해하기

전체 branch를 merge하지 않고 bug fix commit 하나만 다른 release branch에 가져와야 할 때가 있다. `cherry-pick`은 선택한 commit이 만든 변화를 현재 위치에 다시 적용한다.

### 안에서는 실제로 무엇이 일어나는가

원본 commit과 새 commit은 보통 SHA가 다르다. context가 다르면 conflict가 날 수 있고, dependency commit을 빼먹으면 단독으로 동작하지 않을 수 있다.

### 아주 쉬운 예

연습 repo에서 한 commit만 다른 branch에 적용한다.

```bash
git switch release
git cherry-pick "$FIX_COMMIT_SHA"
git log --oneline --decorate --graph --all
```

### 한 줄씩 읽기

- 현재 release branch에 새 commit이 생긴다.
- 원본 fix의 patch를 현재 context에 적용한다.
- 전체 feature history는 자동으로 따라오지 않는다.

### 직접 실행

1. 독립 fix commit을 만든다.
2. release branch로 이동해 cherry-pick한다.
3. 새 commit diff와 tests를 확인한다.


### 일부를 바꿔서 다시 확인하기

fix가 이전 refactor commit에 의존하도록 만든 뒤 fix만 cherry-pick하면 어떤 실패가 생기는지 조사한다.

### 작은 문제

cherry-pick이 성공 exit로 끝났으니 fix가 release에서 동작한다고 볼 수 있는가?

### 왜 맞고 왜 틀리는가

아니다. patch 적용 성공일 뿐 runtime/contract는 별도 tests가 필요하다.

### 자주 만나는 실패와 확인 순서

- 같은 fix를 두 번 cherry-pick해 duplicate behavior를 만들지 않는다.
- release 정책상 backport 승인/추적 ID를 남긴다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** cherry-pick은 특정 commit의 변경을 다른 history 위치에 새 commit으로 적용한다.
- **직접 코딩·실행해서 익힐 것:** 독립 fix를 실제 backport해 diff/test를 본다.
- **AI에게 맡겨도 되는 것:** dependency 분석.
- **사람이 최종 확인할 것:** 선택 commit이 독립적인지와 release 위험을 판단한다.

### 근거 연결

`GIT-BOOK`

---

## CHAPTER 06 · LESSON 06 · merge와 rebase는 “좋고 나쁨”이 아니라 history 정책 선택이다

**LESSON ID:** `T10-B11-L06`

### 먼저 쉬운 말로 이해하기

merge는 기존 branch history를 보존하면서 결합하고, rebase는 commit을 새 base에 다시 적용해 선형 history를 만들 수 있다. 하나가 항상 정답은 아니다.

### 안에서는 실제로 무엇이 일어나는가

review 방식, release 추적, bisect, shared branch, team familiarity에 따라 trade-off가 달라진다. 중요한 것은 팀이 예상 가능한 정책을 쓰고 위험한 history rewrite를 통제하는 것이다.

### 아주 쉬운 예

같은 두 branch를 복제한 임시 repo에서 merge/rebase 결과 graph를 나란히 본다.

```bash
git log --graph --oneline --decorate --all
# graph를 그린 뒤 각 commit parent 관계를 비교한다.
```

### 한 줄씩 읽기

- merge는 두 parent를 가진 commit을 남길 수 있다.
- rebase는 feature commit SHA와 parent를 바꾼다.
- 최종 working tree가 같아도 history 의미가 다를 수 있다.

### 직접 실행

1. merge 사례 graph를 저장한다.
2. 별도 branch에서 rebase 사례 graph를 만든다.
3. review/rollback/bisect 관점 장단점을 표로 적는다.


### 일부를 바꿔서 다시 확인하기

팀이 squash merge를 쓴다면 PR 내부 세부 commit과 main history가 어떻게 달라지는지 조사한다.

### 작은 문제

개인 취향으로 모든 shared branch를 rebase/force push해도 되는가?

### 왜 맞고 왜 틀리는가

안 된다. 협업 history 계약을 깨뜨릴 수 있다. repository 정책과 합의를 따라야 한다.

### 자주 만나는 실패와 확인 순서

- tool UI가 제공한다고 안전한 operation이라는 뜻은 아니다.
- history 정리보다 source/test correctness가 우선이다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** merge는 history를 결합하고 rebase는 commit을 새 base 위에 다시 적용한다.
- **직접 코딩·실행해서 익힐 것:** 두 방식을 실제 graph로 비교한다.
- **AI에게 맡겨도 되는 것:** 정책 비교표 생성.
- **사람이 최종 확인할 것:** 협업·release 요구에 맞는 정책을 선택한다.

### 근거 연결

`GIT-BOOK` · `DORA-2025`

---

## CHAPTER 07 · LESSON 07 · conflict 해결 뒤에는 반드시 diff와 테스트로 의미를 재검증한다

**LESSON ID:** `T10-B11-L07`

### 먼저 쉬운 말로 이해하기

conflict marker를 모두 지워 commit이 가능해졌다는 것은 문법적 절차가 끝났다는 뜻일 뿐이다. 두 변경의 조합이 올바른지는 새 상태에서 다시 검증해야 한다.

### 안에서는 실제로 무엇이 일어나는가

conflict resolution은 새로운 code change로 취급한다. staged diff를 읽고 unit/integration tests를 실행하며, 영향이 큰 경우 해당 feature의 acceptance를 다시 확인한다.

### 아주 쉬운 예

merge 후 staged diff와 test를 순서대로 실행하는 체크를 만든다.

```bash
git status
git diff --staged
pytest -q
```

### 한 줄씩 읽기

- status로 unresolved marker/path가 없는지 본다.
- staged diff가 최종 resolution 내용을 보여 준다.
- test는 합쳐진 behavior를 실제 실행한다.

### 직접 실행

1. 실제 연습 conflict를 해결한다.
2. `git diff --check`로 남은 whitespace/conflict marker 문제를 보조 확인한다.
3. pytest 또는 해당 프로젝트 test를 실행한다.


### 일부를 바꿔서 다시 확인하기

두 branch에서 각각 통과하던 tests를 합친 뒤 새로운 조합 test가 필요한 사례를 만든다.

### 작은 문제

양쪽 branch의 CI가 merge 전 모두 green이었다. merge 결과도 자동으로 green인가?

### 왜 맞고 왜 틀리는가

아니다. 조합에서만 생기는 semantic bug가 있다. merge commit/최종 SHA에 대해 다시 검증해야 한다.

### 자주 만나는 실패와 확인 순서

- merge queue/required checks가 있다면 final integration SHA에 적용되는지 확인한다.
- conflict 해결을 AI에 맡겨도 최종 의도와 tests는 사람이 확인한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** conflict resolution은 새로운 결합 상태이므로 최종 diff와 test가 필요하다.
- **직접 코딩·실행해서 익힐 것:** 해결 후 staged diff와 tests를 직접 실행한다.
- **AI에게 맡겨도 되는 것:** resolution candidate 제안.
- **사람이 최종 확인할 것:** 양쪽 요구가 모두 보존되는지 최종 결정한다.

### 근거 연결

`GIT-DOC` · `DORA-2025`

---
