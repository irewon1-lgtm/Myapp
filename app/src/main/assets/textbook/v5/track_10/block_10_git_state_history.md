# TRACK 10 · 오류·테스트·Git·빌드·배포 — 변경을 안전하게 만드는 기술

# BLOCK 10 · Git의 상태 모델과 변경 기록

Git을 명령어 암기로 배우지 않고 working tree→index→commit→history라는 상태 이동으로 이해한다. 실제 diff와 SHA를 검증 증거에 연결해 이후 CI·artifact·rollback의 기반을 만든다.


```text
개념 설명 → 아주 쉬운 예 → 한 줄씩 해석 → 직접 실행 → 일부 수정 → 작은 문제 → 왜 맞고 틀렸는지 설명
```

---

## CHAPTER 01 · LESSON 01 · Git을 파일 백업이 아니라 변경 이력 그래프로 이해한다

**LESSON ID:** `T10-B10-L01`

### 먼저 쉬운 말로 이해하기

Git은 “파일을 복사해 두는 프로그램”보다 훨씬 구조적이다. 각 commit은 특정 시점의 프로젝트 상태와 부모 commit, 작성 정보, message를 연결한 기록이다. branch 이름은 이 기록 그래프의 한 commit을 가리키는 움직이는 표지라고 생각하면 된다.

### 안에서는 실제로 무엇이 일어나는가

Git object는 content-addressed 방식으로 식별되고 commit은 tree와 parent를 참조한다. 초급에서 내부 object 형식을 암기할 필요는 없지만, commit을 “파일 하나의 버전”이 아니라 프로젝트 snapshot과 history 연결로 이해해야 merge/rebase/reset이 덜 혼란스럽다.

### 아주 쉬운 예

빈 임시 repository를 만들고 세 번 commit해 history가 늘어나는 것을 본다.

```bash
mkdir git-demo && cd git-demo
git init
git config user.name "Learner"
git config user.email "learner@example.invalid"
echo one > note.txt
git add note.txt
git commit -m "add first note"
echo two >> note.txt
git commit -am "append second note"
git log --oneline --decorate --graph
```

### 한 줄씩 읽기

- `git init`은 현재 폴더를 repository로 만든다.
- 첫 commit은 note.txt가 있는 snapshot을 기록한다.
- 둘째 commit은 첫 commit을 parent로 연결한다.
- `log --graph`는 현재 history를 눈으로 보여 준다.

### 직접 실행

1. 이 패키지의 별도 temp directory에서 위 명령을 실행한다.
2. `git log --oneline --graph`에서 commit 두 개를 확인한다.
3. 각 commit SHA 앞부분이 서로 다른지 기록한다.


### 일부를 바꿔서 다시 확인하기

세 번째 commit을 추가하고 branch pointer가 최신 commit으로 이동하는지 `git log --decorate`로 본다. 실제 프로젝트가 아니라 연습용 repository에서 한다.

### 작은 문제

Git commit을 “파일 차이 한 묶음”이라고만 이해하면 어떤 점이 부족한가?

### 왜 맞고 왜 틀리는가

commit은 변경 내용뿐 아니라 전체 tree 상태와 parent 관계를 가진 history node다. 이 관계 때문에 ancestor, merge base, branch 이동 같은 개념이 가능하다.

### 자주 만나는 실패와 확인 순서

- 실제 업무 repository에서 연습한다고 history를 임의 reset하지 않는다.
- 초보 단계에서는 `.git` 내부 파일을 직접 수정하지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** commit은 프로젝트 상태와 부모 이력을 잇는 기록이고 branch는 commit을 가리키는 이름이다.
- **직접 코딩·실행해서 익힐 것:** 임시 repo에서 commit을 만들고 graph를 직접 본다.
- **AI에게 맡겨도 되는 것:** Git 명령 설명과 history 그림 생성.
- **사람이 최종 확인할 것:** 어떤 작업이 history를 바꾸는지 이해하고 실제 repo에 적용할지 판단한다.

### 근거 연결

`GIT-BOOK` · `GIT-DOC`

---

## CHAPTER 02 · LESSON 02 · working tree·index·HEAD 세 영역을 구분한다

**LESSON ID:** `T10-B10-L02`

### 먼저 쉬운 말로 이해하기

Git이 헷갈리는 가장 큰 이유는 “현재 파일”이 하나가 아니기 때문이다. working tree는 지금 편집 중인 파일, index(staging area)는 다음 commit에 넣기로 고른 상태, HEAD는 현재 checkout한 commit을 가리킨다.

### 안에서는 실제로 무엇이 일어나는가

`git add`는 서버에 업로드하는 명령이 아니라 working tree의 선택한 내용을 index에 올린다. `git commit`은 index를 기준으로 새 commit을 만든다. 그래서 파일 일부만 stage하거나 stage 뒤 다시 수정하면 두 종류 diff가 생긴다.

### 아주 쉬운 예

한 파일을 stage한 뒤 다시 수정해 두 diff를 만든다.

```bash
echo alpha > demo.txt
git add demo.txt
echo beta >> demo.txt
git status --short
git diff
git diff --staged
```

### 한 줄씩 읽기

- 첫 add 시점의 `alpha`가 index에 있다.
- 그 뒤 `beta`는 working tree에만 추가된다.
- `git diff`는 보통 working tree↔index 차이를 보여 준다.
- `git diff --staged`는 index↔HEAD 차이를 보여 준다.

### 직접 실행

1. 임시 repo에서 실행한다.
2. status에 staged/unstaged 상태가 동시에 표현되는지 본다.
3. 두 diff가 서로 다른 줄을 보여 주는지 비교한다.


### 일부를 바꿔서 다시 확인하기

`git add demo.txt`를 다시 실행해 index를 최신 working tree와 맞추고 두 diff가 어떻게 변하는지 본다.

### 작은 문제

파일을 저장했으니 다음 commit에 자동으로 포함되는가?

### 왜 맞고 왜 틀리는가

아니다. index에 stage된 내용만 commit 대상이다. 저장과 stage는 다른 상태 전이다.

### 자주 만나는 실패와 확인 순서

- `git add .` 전에 status/diff를 보지 않으면 비밀 파일이나 unrelated change가 함께 stage될 수 있다.
- IDE의 “Commit” 버튼도 내부적으로 어떤 파일이 stage되는지 확인한다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** working tree, index, HEAD는 서로 다른 상태다.
- **직접 코딩·실행해서 익힐 것:** stage 전후 diff를 직접 비교한다.
- **AI에게 맡겨도 되는 것:** status/diff 해석 보조.
- **사람이 최종 확인할 것:** commit에 실제로 어떤 내용이 들어가는지 diff로 확인한다.

### 근거 연결

`GIT-BOOK` · `GIT-DOC`

---

## CHAPTER 03 · LESSON 03 · git status와 diff를 변경 전후 기본 계기판으로 쓴다

**LESSON ID:** `T10-B10-L03`

### 먼저 쉬운 말로 이해하기

코드를 고치기 전후에 `git status`와 `git diff`를 보는 습관은 사고를 크게 줄인다. 무엇을 건드렸는지 모르면 테스트 결과와 commit 범위를 신뢰하기 어렵다.

### 안에서는 실제로 무엇이 일어나는가

status는 untracked/modified/staged 같은 상태를, diff는 실제 줄 변화를 보여 준다. 명령 결과를 읽고 예상하지 못한 generated file, secret, 대규모 formatting을 발견하면 commit 전에 분리한다.

### 아주 쉬운 예

두 파일 중 하나만 의도적으로 수정하고 diff 범위를 확인한다.

```bash
printf "A\n" > a.txt
printf "B\n" > b.txt
git add a.txt b.txt
git commit -m "add two files"
printf "A changed\n" > a.txt
git status --short
git diff -- a.txt
```

### 한 줄씩 읽기

- baseline commit이 있어야 변경 전후가 비교된다.
- status는 a.txt만 modified라고 보여야 한다.
- path를 붙인 diff는 특정 파일 변경을 집중해서 본다.

### 직접 실행

1. 임시 repo에서 실행한다.
2. b.txt도 수정한 뒤 전체 `git diff`와 path diff를 비교한다.
3. 의도한 변경 파일 목록을 commit 전 체크리스트에 적는다.


### 일부를 바꿔서 다시 확인하기

공백/formatting만 바뀐 파일과 실제 logic 변경을 분리해 별도 commit로 만드는 실습을 한다.

### 작은 문제

test가 통과했는데 diff에 내가 모르는 설정 파일 변경이 있다. 그대로 commit해도 되는가?

### 왜 맞고 왜 틀리는가

안 된다. test 통과가 그 변경의 의도를 설명하지 않는다. 설정 변화 원인을 확인하거나 변경에서 제외해야 한다.

### 자주 만나는 실패와 확인 순서

- binary/generated file diff는 일반 text diff로 충분하지 않을 수 있다.
- secret이 diff에 보이면 commit 전에 제거하고 이미 commit됐다면 history/credential 대응이 필요하다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** status는 변경 상태, diff는 실제 내용 차이를 보여 주는 기본 증거다.
- **직접 코딩·실행해서 익힐 것:** 수정 전후 status/diff를 확인한다.
- **AI에게 맡겨도 되는 것:** diff 요약과 suspicious file 탐지.
- **사람이 최종 확인할 것:** 모든 diff가 의도된 변경인지 사람이 승인한다.

### 근거 연결

`GIT-DOC` · `NIST-SSDF-12`

---

## CHAPTER 04 · LESSON 04 · 작고 의미 있는 commit으로 변경 이유를 보존한다

**LESSON ID:** `T10-B10-L04`

### 먼저 쉬운 말로 이해하기

commit이 너무 크면 reviewer가 무엇이 핵심인지 알기 어렵고 rollback도 거칠어진다. 반대로 의미 없는 한 줄 commit 수백 개도 history를 읽기 어렵게 한다. 하나의 설명 가능한 의도 단위로 묶는 것이 좋다.

### 안에서는 실제로 무엇이 일어나는가

좋은 commit은 build/test 가능한 상태를 유지하는 것을 목표로 하고 message는 “무엇을”보다 “왜”가 드러나게 쓴다. refactor와 behavior change를 분리하면 bisect와 review가 쉬워진다.

### 아주 쉬운 예

서로 unrelated한 두 변경을 stage 선택으로 분리한다.

```bash
git status --short
git add src/calculator.py
git diff --staged
git commit -m "fix tax boundary at 10 percent"
# 문서 변경은 다음 commit에서 별도로 처리
```

### 한 줄씩 읽기

- status로 전체 변경을 본다.
- 특정 파일/patch만 stage해 commit 범위를 만든다.
- staged diff를 마지막으로 검토한다.

### 직접 실행

1. 연습 repo에서 두 파일을 서로 다른 의도로 수정한다.
2. 첫 의도 파일만 stage/commit한다.
3. 나머지 working tree 변경이 그대로 남는지 확인한다.


### 일부를 바꿔서 다시 확인하기

`git add -p`를 사용해 같은 파일 안의 서로 다른 hunk를 선택 stage하는 연습을 한다. 실수로 hunk를 놓쳤다면 commit 전에 staged diff로 잡는다.

### 작은 문제

commit이 작으면 무조건 하나의 줄씩 commit하는 것이 좋은가?

### 왜 맞고 왜 틀리는가

아니다. 독립적으로 설명·검증 가능한 의도 단위가 기준이다. 함께 있어야 build가 되는 변경은 한 단위가 될 수 있다.

### 자주 만나는 실패와 확인 순서

- 중간 commit이 항상 깨진 상태면 bisect가 어려워질 수 있다.
- commit message에 secret/customer data를 넣지 않는다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** commit은 가능한 한 하나의 설명 가능한 변경 의도를 담는다.
- **직접 코딩·실행해서 익힐 것:** 선택 stage와 staged diff를 사용한다.
- **AI에게 맡겨도 되는 것:** commit 분리/메시지 후보 생성.
- **사람이 최종 확인할 것:** 분리된 각 commit이 의미 있고 검증 가능한지 확인한다.

### 근거 연결

`GIT-BOOK` · `DORA-2025`

---

## CHAPTER 05 · LESSON 05 · log·show·blame로 “언제 왜 바뀌었나”를 추적한다

**LESSON ID:** `T10-B10-L05`

### 먼저 쉬운 말로 이해하기

버그 원인을 찾을 때 현재 코드만 보면 왜 이런 이상한 조건이 들어왔는지 모를 수 있다. `git log`, `git show`, `git blame`는 과거 변경과 context를 찾는 도구다.

### 안에서는 실제로 무엇이 일어나는가

`log -- path`는 특정 파일 history, `show <sha>`는 commit diff와 metadata, `blame`은 현재 각 줄이 마지막으로 바뀐 commit을 찾는다. blame은 “누구 탓” 도구가 아니라 관련 change와 의사결정을 찾는 출발점이다.

### 아주 쉬운 예

연습 repo의 특정 파일 history를 조사한다.

```bash
git log --oneline -- note.txt
git show --stat HEAD
git blame note.txt
```

### 한 줄씩 읽기

- path log는 관련 commit 범위를 줄인다.
- show는 한 commit에서 실제 무엇이 바뀌었는지 본다.
- blame의 SHA를 다시 show/log로 따라가면 당시 context를 찾을 수 있다.

### 직접 실행

1. 임시 repo에서 명령을 실행한다.
2. 첫 줄과 둘째 줄이 어느 commit에서 왔는지 본다.
3. 해당 SHA를 `git show`로 열어 message와 diff를 연결한다.


### 일부를 바꿔서 다시 확인하기

파일 이름이 바뀐 history에서 `git log --follow`를 시도하고 한계를 조사한다.

### 작은 문제

blame 결과에 특정 개발자 이름이 있으니 그 사람이 bug 원인이라고 결론내려도 되는가?

### 왜 맞고 왜 틀리는가

안 된다. 마지막 줄 변경자일 뿐이고 요구사항/후속 상호작용/다른 commit이 원인일 수 있다. blame은 책임 추궁이 아니라 history 탐색 도구로 쓴다.

### 자주 만나는 실패와 확인 순서

- 대규모 formatting commit이 blame history를 가릴 수 있다.
- 오래된 commit의 dependency/환경을 그대로 재실행하기 어려울 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** log/show/blame은 현재 코드가 만들어진 변경 history와 context를 찾는다.
- **직접 코딩·실행해서 익힐 것:** 한 줄에서 commit까지 실제로 역추적한다.
- **AI에게 맡겨도 되는 것:** history 요약.
- **사람이 최종 확인할 것:** 과거 의도를 현재 요구와 구분하고 사람 탓으로 단정하지 않는다.

### 근거 연결

`GIT-BOOK` · `GIT-DOC`

---

## CHAPTER 06 · LESSON 06 · .gitignore는 비밀 삭제 도구가 아니라 추적 대상 규칙이다

**LESSON ID:** `T10-B10-L06`

### 먼저 쉬운 말로 이해하기

build output, cache, 개인 IDE 설정처럼 repository에 넣지 않을 파일은 `.gitignore`로 관리할 수 있다. 하지만 secret을 이미 commit한 뒤 ignore에 추가한다고 history에서 secret이 사라지지는 않는다.

### 안에서는 실제로 무엇이 일어나는가

ignore rule은 untracked file의 추적 제안을 막는 데 가깝다. 이미 tracked인 파일은 별도 index/history 처리가 필요하다. credential이 노출됐다면 key rotation 같은 실제 보안 대응이 우선이다.

### 아주 쉬운 예

간단한 ignore 규칙을 만든다.

```bash
printf ".env\n__pycache__/\n*.log\n" > .gitignore
touch .env debug.log
git status --short
```

### 한 줄씩 읽기

- `.env`와 `.log`는 untracked 목록에서 숨겨질 수 있다.
- `.gitignore` 자체는 repository에 commit할 수 있다.
- 무엇이 ignore됐는지 `git check-ignore -v`로 확인할 수 있다.

### 직접 실행

1. 임시 repo에서 실행한다.
2. `git check-ignore -v .env debug.log`로 적용 규칙을 본다.
3. 이미 tracked file을 ignore에 추가했을 때 status가 어떻게 다른지 별도 실험한다.


### 일부를 바꿔서 다시 확인하기

secret scanner가 있다고 가정해도 commit 전 diff 검사와 credential 관리가 필요한 이유를 적는다.

### 작은 문제

API key를 commit한 뒤 1분 안에 지우고 `.gitignore`에 넣었다. 이제 안전한가?

### 왜 맞고 왜 틀리는가

안전하다고 볼 수 없다. remote/history/cache에 남았을 수 있으므로 key를 폐기/회전하고 노출 범위를 조사해야 한다.

### 자주 만나는 실패와 확인 순서

- `.env.example`에는 실제 secret 대신 변수 이름/가짜값만 둔다.
- ignore가 과도하면 필요한 migration/config 파일까지 누락될 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** .gitignore는 어떤 untracked 파일을 Git 추적 후보에서 제외할지 정하는 규칙이지 secret 삭제 기능이 아니다.
- **직접 코딩·실행해서 익힐 것:** ignore 적용 여부와 tracked 여부를 직접 확인한다.
- **AI에게 맡겨도 되는 것:** ignore pattern 생성/검토.
- **사람이 최종 확인할 것:** 민감정보 노출 시 credential rotation과 history 대응을 결정한다.

### 근거 연결

`GIT-DOC` · `NIST-SSDF-12`

---

## CHAPTER 07 · LESSON 07 · commit SHA를 실행 결과와 연결해 재현성을 높인다

**LESSON ID:** `T10-B10-L07`

### 먼저 쉬운 말로 이해하기

“테스트 통과한 코드”가 나중에 어떤 code인지 알 수 없으면 증거 가치가 떨어진다. test report, build artifact, deployment에는 commit SHA 같은 source identity를 연결해야 한다.

### 안에서는 실제로 무엇이 일어나는가

동일 branch 이름도 시간이 지나면 다른 commit을 가리킨다. 반면 full commit SHA는 특정 Git object를 식별한다. release tag도 유용하지만 tag가 어떤 commit을 가리키는지 기록해야 한다.

### 아주 쉬운 예

현재 SHA를 파일이나 report에 남기는 가장 단순한 예다.

```bash
git rev-parse HEAD
git status --porcelain
```

### 한 줄씩 읽기

- `rev-parse HEAD`는 현재 commit identity를 얻는다.
- status가 비어 있어야 “그 commit 그대로” 실행했다는 설명이 단순해진다.
- dirty working tree에서 실행했다면 SHA만으로 실제 코드 전체를 설명하지 못한다.

### 직접 실행

1. 임시 repo에서 HEAD SHA를 기록한다.
2. 파일을 수정만 하고 commit하지 않은 뒤 SHA가 같은지 확인한다.
3. 동일 SHA인데 working tree가 달라질 수 있음을 report에 표시한다.


### 일부를 바꿔서 다시 확인하기

테스트 harness가 `commit_sha`와 `dirty=true/false`를 자동 기록하도록 만들어 본다.

### 작은 문제

branch `main`에서 테스트했다고 기록하면 나중에도 정확한 source를 재현할 수 있는가?

### 왜 맞고 왜 틀리는가

main pointer가 이동할 수 있으므로 부족하다. commit SHA와 dependency/build identity를 함께 남기는 것이 더 정확하다.

### 자주 만나는 실패와 확인 순서

- dirty build artifact를 공식 release로 착각하지 않게 한다.
- SHA만 있고 submodule/dependency lock state가 다르면 완전 재현이 아닐 수 있다.

### 이번 LESSON에서 구분할 것

- **암기 최소선:** 검증 결과는 branch 이름보다 정확한 source revision과 연결해야 추적성이 높다.
- **직접 코딩·실행해서 익힐 것:** SHA와 dirty 상태를 함께 확인한다.
- **AI에게 맡겨도 되는 것:** build metadata 삽입 코드 생성.
- **사람이 최종 확인할 것:** 어떤 source/dependency 상태로 실행됐는지 최종 확인한다.

### 근거 연결

`SLSA-12` · `GIT-DOC`

---
