# 5장 실전 훈련편 — 터미널·권한·파이프·환경변수·PATH

이 훈련편은 명령어 암기장이 아니다. 실제 명령을 **프로그램 / 옵션 / 대상 / 권한 / 출력 / 복구 가능성**으로 해부하고, 위험한 명령을 실행 전에 걸러내는 연습을 한다.

---

## 훈련 A. 명령 해부 10문제

각 명령에서 **실행 프로그램 / 옵션 / 대상**을 나눈다.

```bash
ls -la data
```

```bash
python script.py input.csv
```

```bash
cp report.csv backup/report.csv
```

```bash
rm -r temp
```

```bash
grep ERROR app.log
```

```bash
curl -I https://example.com
```

```bash
git status
```

```bash
git diff -- app/src/main/MainActivity.kt
```

```bash
sha256sum app-release.apk
```

```bash
find data -name "*.csv"
```

모르는 옵션은 실행하지 말고 도움말/공식 문서를 확인한다.

---

## 훈련 B. 현재 위치가 다른 문제

터미널 A:

```text
/home/user/MyApp
```

터미널 B:

```text
/home/user/MyApp/tools
```

둘 다 실행:

```bash
python scripts/check.py
```

A에서는 성공, B에서는 파일을 못 찾는다.

질문:

- Python 파일 경로가 상대경로인가?
- 스크립트가 현재 작업 폴더를 기준으로 파일을 찾는가?
- `pwd`와 실제 최종 경로를 출력하면 무엇을 알 수 있는가?

---

## 훈련 C. `sudo`를 붙이면 왜 위험한가

상황:

```text
Permission denied
```

AI가 답한다.

```bash
sudo some-command
```

실행 전 질문:

1. 어떤 파일/장치를 수정하는 명령인가?
2. 관리자 권한이 실제 필요한가?
3. 현재 사용자 권한/소유권이 잘못된 건 아닌가?
4. `sudo` 때문에 생성 파일 소유자가 root가 되면 후속 문제가 생기지 않는가?
5. 되돌릴 방법이 있는가?

`Permission denied` → `sudo` 자동반사는 금지한다.

---

## 훈련 D. 출력 덮어쓰기 사고

명령:

```bash
some-command > important.log
```

환경에 따라 기존 `important.log`를 덮어쓸 수 있다.

안전한 첫 시험:

```bash
some-command > test-output.log
```

그 다음 결과를 확인한 뒤 실제 파일 정책을 정한다.

### 추가 문제

`>>`는 보통 append에 쓰인다. 그렇다고 모든 상황에서 무조건 안전한가?

아니다. 로그가 무한히 커질 수 있고 중복 데이터가 쌓일 수 있다. **파일 크기/rotation 정책**도 필요할 수 있다.

---

## 훈련 E. 파이프 데이터 흐름 추적

명령:

```bash
cat app.log | grep ERROR | head -n 20
```

흐름을 글로 적는다.

```text
app.log 내용
→ ERROR 포함 줄만 선택
→ 앞 20줄만 출력
```

그 다음 다음 명령의 흐름을 설명한다.

```bash
find . -name "*.kt" | wc -l
```

한 명령의 stdout이 다음 명령의 stdin으로 전달되는 구조를 찾는다.

---

## 훈련 F. stderr와 stdout 분리

프로그램이 다음을 만든다고 하자.

```text
정상 결과 100줄
오류 메시지 3줄
```

자동화에서 정상 결과만 다음 단계로 넘기고 오류는 별도로 기록하고 싶다.

운영체제/셸에 따라 구체 문법은 다를 수 있지만, 핵심 설계는:

```text
stdout → 데이터 파이프라인
stderr → 오류 로그/감시
```

이다.

왜 정상 데이터와 오류 문자열을 섞으면 parser가 깨질 수 있는지 적는다.

---

## 훈련 G. 환경변수 존재 여부 확인

Python 예:

```python
import os

api_url = os.getenv("API_URL")
print("API_URL exists:", api_url is not None)
```

비밀값을 직접 출력하지 않고 존재 여부만 확인하는 패턴이다.

다음 코드를 왜 피해야 하는가?

```python
print("API_KEY=", os.getenv("API_KEY"))
```

로그/스크린샷에 secret이 남을 수 있다.

---

## 훈련 H. 개발/운영 설정 혼동

환경변수:

```text
APP_ENV=dev
API_URL=https://dev-api.example.com
```

운영 배포인데 dev 주소를 보고 있다.

증상:

- 운영 데이터가 안 보임
- 테스트 계정만 보임

조사 순서:

```text
실행 환경변수
빌드 config
base URL 로그(비밀값 제외)
배포 환경 설정
```

UI 코드부터 고치는 것은 우선순위가 아니다.

---

## 훈련 I. PATH에 같은 실행 파일 두 개

설치 상태:

```text
/usr/bin/python → Python 3.10
/home/user/.local/bin/python → Python 3.13
```

PATH 순서:

```text
/usr/bin
/home/user/.local/bin
```

`python`을 입력하면 앞쪽 후보가 선택될 수 있다.

질문:

- 왜 설치 자체는 성공했는데 옛 버전이 실행될 수 있는가?
- 어떤 실행 파일을 실제로 쓰는지 확인해야 하는가?

---

## 훈련 J. command not found 진단

가능한 원인:

```text
프로그램 미설치
PATH 미등록
명령 이름 오타
현재 shell 환경 차이
가상환경 비활성화
권한 문제
```

“재설치” 하나만 반복하지 않고 후보를 나눈다.

---

## 훈련 K. secret 사고 대응

상황:

```text
API key를 실수로 공개 GitHub commit에 올림
```

잘못된 대응:

```text
파일에서 key 문자열만 삭제하고 끝
```

필요한 대응 후보:

```text
키 폐기/revoke
새 키 발급
사용 로그 확인
저장소 히스토리 노출 여부 확인
secret scanning
코드에서 secure loading 방식으로 변경
```

공개된 secret은 이미 복사됐다고 가정하고 대응하는 편이 안전하다.

---

## 훈련 L. 위험 명령 사전 감사

아래 명령은 실제로 실행하지 않는다.

```bash
rm -rf "$TARGET"
```

실행 전 검증 코드를 먼저 만든다고 가정한다.

```bash
printf 'TARGET=%s\n' "$TARGET"
```

그 다음 확인할 것:

```text
TARGET이 빈 값인가?
루트/홈 같은 상위 폴더인가?
삭제 예상 개수는 얼마인가?
backup이 있는가?
dry run이 가능한가?
```

변수 기반 삭제는 특히 빈 변수/잘못된 변수 값에 주의한다.

---

## 훈련 M. 명령의 출력이 증거가 되려면

좋지 않은 기록:

```text
명령 실행했음. 됨.
```

더 좋은 기록:

```text
command: sha256sum app.apk
exit code: 0
stdout: <hash> app.apk
timestamp: ...
target path: ...
```

어떤 명령을, 어느 대상에, 어떤 결과로 실행했는지 남긴다.

---

## AI 답 검증 1

AI 답:

> 권한 오류가 나면 `sudo`를 붙이면 해결됩니다.

판정: 위험.

`sudo`가 원인을 고치는 것이 아니라 더 높은 권한으로 같은 작업을 강제할 뿐일 수 있다.

---

## AI 답 검증 2

AI 답:

> API key는 환경변수에 넣었으므로 `printenv` 결과를 이슈에 붙여도 안전합니다.

판정: 틀림.

환경변수에 저장해도 출력하면 비밀값이 노출된다.

---

## 디버깅 미션 1 — CI에서만 command not found

내 PC:

```text
mytool --version → 정상
```

CI:

```text
mytool: command not found
```

조사:

```text
CI 설치 step 존재?
PATH?
실행 파일 경로?
캐시가 잘못된 성공을 숨기고 있었나?
OS 차이?
```

설치 명령을 무작정 반복하기 전에 환경 차이를 기록한다.

---

## 디버깅 미션 2 — wrong environment

앱 로그:

```text
APP_ENV=prod
API_HOST=dev-api.example.com
```

이 조합 자체가 모순 신호다.

설정 source가 여러 개인지 찾는다.

```text
.env
CI secret
build config
runtime environment
command-line option
```

우선순위 규칙이 겹치면 한 설정이 다른 설정을 덮을 수 있다.

---

## 독립 프로젝트 — 안전한 명령 실행 체크러

가상의 명령 문자열을 입력받아 사람이 체크할 항목을 출력하는 작은 Python 프로그램을 만든다.

입력 예:

```text
rm -rf build
```

출력 예:

```text
[ ] 현재 위치 확인
[ ] 대상 경로 확인
[ ] 삭제/덮어쓰기 여부 확인
[ ] 관리자 권한 여부 확인
[ ] 복구 가능성 확인
```

이 프로그램이 명령을 자동으로 “안전하다”고 판정하도록 만들 필요는 없다. **사람이 놓치지 않게 질문을 만드는 도구**면 충분하다.

---

# 5장 훈련 완료 기준

- 명령을 프로그램/옵션/대상으로 나눠 읽는다.
- 상대경로 명령 전 현재 작업 위치를 확인한다.
- Permission denied에 자동으로 sudo를 붙이지 않는다.
- stdout/stderr와 pipe 흐름을 설명한다.
- 리다이렉션의 덮어쓰기 위험을 안다.
- 환경변수와 PATH 때문에 환경별 동작이 달라질 수 있음을 진단한다.
- secret을 로그에 출력하지 않는다.
- AI가 만든 위험 명령을 실행 전 체크리스트로 감사한다.
