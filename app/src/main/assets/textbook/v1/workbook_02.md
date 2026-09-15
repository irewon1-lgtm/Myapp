# 2장 실전 훈련편 — 파일·경로·형식·인코딩·무결성

이 훈련편은 파일·폴더 정의를 반복하지 않는다. 실제 파일 문제를 **존재 → 경로 → 권한 → 형식 → 인코딩 → 무결성** 순서로 진단하는 연습에 집중한다.

---

## 훈련 A. 같은 파일명, 다른 파일 찾기

프로젝트 구조:

```text
MyApp/
  config.json
  app/
    config.json
  backup/
    config.json
```

다음 작업지시가 왜 위험한지 적는다.

```text
"config.json 수정해줘"
```

더 안전한 지시로 바꾼다.

```text
"MyApp/app/config.json만 수정하고 나머지 config.json은 변경하지 마"
```

### 추가 문제

AI가 `backup/config.json`을 수정했다고 보고했다. 실제 앱이 읽는 파일은 `app/config.json`이다.

어떤 증상이 생길 수 있는가?

- AI는 “수정 완료”라고 말함
- 앱 동작은 전혀 바뀌지 않음

이때 코드를 다시 고치기 전에 **실제 실행 경로가 어떤 파일을 읽는지** 확인한다.

---

## 훈련 B. 상대경로를 실제 위치로 계산하기

현재 작업 폴더:

```text
/home/user/MyApp/tools
```

코드:

```python
path = "../data/report.csv"
```

실제 최종 위치를 적는다.

```text
/home/user/MyApp/data/report.csv
```

### 변형

현재 작업 폴더가 다음으로 바뀌면?

```text
/home/user/MyApp
```

같은 `../data/report.csv`는 이제 다른 곳을 가리킨다.

상대경로 버그를 잡을 때 왜 `pwd` 또는 working directory 로그가 유용한지 적는다.

---

## 훈련 C. 경로 문자열 합치기 실수

나쁜 코드:

```python
folder = "downloads"
name = "report.csv"
path = folder + name
print(path)
```

결과:

```text
downloadsreport.csv
```

최소 수정:

```python
path = folder + "/" + name
```

하지만 운영체제별 경로 차이를 고려하면 전용 경로 도구를 쓰는 편이 더 안전할 수 있다.

Python 예:

```python
from pathlib import Path
path = Path("downloads") / "report.csv"
print(path)
```

직접 폴더와 파일명을 바꿔 실행한다.

---

## 훈련 D. 확장자만 바꾸는 실수

상황:

```text
원본 파일: photo.png
사용자 행동: 이름을 photo.jpg로 변경
```

질문:

1. 내부 바이트가 JPEG로 바뀌는가?
2. 이미지 뷰어가 열 수도 있는 이유는 무엇인가?
3. 업로드 서버가 거절할 수도 있는 이유는 무엇인가?

정답의 핵심:

```text
확장자는 이름의 힌트
실제 포맷은 내부 구조
MIME은 전달 시의 종류 정보
```

셋이 어긋날 수 있다.

---

## 훈련 E. CSV 한글 깨짐 수사

파일 원문은 한글이 정상이라고 가정한다.

```csv
병원명,매출
프라임내과,600000
서울의원,420000
```

Excel에서 열었더니 한글이 깨진다.

가능한 가설:

```text
H1 파일이 실제로 손상됨
H2 UTF-8 파일을 다른 인코딩으로 해석함
H3 CSV 가져오기 과정에서 인코딩 자동추정 실패
H4 중간 저장 프로그램이 다시 인코딩함
```

### 증거 수집 순서

1. 원본 바이트/텍스트를 다른 UTF-8 지원 편집기로 열어본다.
2. 파일 인코딩을 확인한다.
3. Excel의 데이터 가져오기에서 인코딩을 명시한다.
4. 같은 원본을 다시 저장하기 전/후 비교한다.

파일명을 `.xlsx`로 바꾸는 행동이 왜 해결이 아닌지 적는다.

---

## 훈련 F. CSV 열 깨짐 찾기

데이터:

```csv
company,revenue,note
A,1000,"good, stable"
B,900,"watch"
```

단순히 문자열을 쉼표로 나누면 첫 행 note가 두 칸으로 잘릴 수 있다.

Python의 CSV 라이브러리를 사용해 확인한다.

```python
import csv
from io import StringIO

text = '''company,revenue,note
A,1000,"good, stable"
B,900,"watch"'''

for row in csv.reader(StringIO(text)):
    print(row)
```

직접 실행하고 수동 `split(',')`와 차이를 확인한다.

---

## 훈련 G. 텍스트/바이너리 모드 차이

다음 파일 종류를 텍스트/바이너리로 분류한다.

```text
report.csv
config.json
photo.jpg
archive.zip
app.apk
notes.txt
```

그 다음 질문에 답한다.

> APK를 텍스트 편집기로 열었는데 글자가 깨져 보인다. 파일 손상 증거인가?

아니다. 바이너리 포맷을 텍스트로 해석했기 때문일 수 있다.

---

## 훈련 H. Base64를 보안으로 착각하지 않기

Python으로 실행한다.

```python
import base64

secret = b"my-password"
encoded = base64.b64encode(secret)
print(encoded)
print(base64.b64decode(encoded))
```

Base64는 원문으로 쉽게 되돌릴 수 있다.

질문:

1. 이것을 암호화라고 부르면 왜 위험한가?
2. API key를 Base64로 바꿔 공개 저장소에 올려도 되는가?

정답: 안 된다.

---

## 훈련 I. 해시로 동일성 비교

Python으로 문자열 해시를 확인한다.

```python
import hashlib

for text in [b"hello", b"hello!", b"Hello"]:
    print(hashlib.sha256(text).hexdigest())
```

아주 작은 변경에도 해시가 크게 달라지는 것을 확인한다.

### 파일 해시

안전한 테스트 파일을 하나 만든다.

```python
from pathlib import Path
import hashlib

path = Path("sample.txt")
path.write_text("hello", encoding="utf-8")
data = path.read_bytes()
print(hashlib.sha256(data).hexdigest())
```

파일 내용을 한 글자 바꿔 다시 계산한다.

---

## 훈련 J. 같은 해시가 의미하는 것과 의미하지 않는 것

판정 문제:

1. 두 파일 SHA-256이 같으면 바이트 내용이 같은 강한 근거다. → 맞음
2. 해시가 같으면 파일은 반드시 안전한 소프트웨어다. → 틀림
3. 공식 배포 해시와 일치하면 다운로드 중 변형 여부 확인에 도움이 된다. → 맞음
4. 악성 파일은 해시를 가질 수 없다. → 틀림

`무결성`과 `신뢰`를 한 문장으로 각각 정의한다.

---

## 훈련 K. 권한 문제와 존재 문제 분리

다음 오류를 분류한다.

```text
FileNotFoundError
PermissionError
UnicodeDecodeError
BadZipFile
checksum mismatch
```

예상 범주:

```text
존재/경로
권한
인코딩
형식/손상
무결성
```

같은 “파일 안 열림”이어도 수정 방법이 전혀 다르다는 점을 확인한다.

---

## 훈련 L. Android 파일 접근 상황

상황:

```text
사용자가 파일 선택기로 CSV를 선택함
앱에서 당장은 읽힘
기기 재부팅/앱 재시작 후 다시 읽으려니 권한 오류
```

가능한 후보:

- URI 권한을 지속적으로 보존하지 않음
- 앱이 임시 접근 권한만 가지고 있었음
- 파일이 이동/삭제됨

PC의 절대경로 문제와 똑같이 취급하면 안 되는 이유를 적는다.

---

## 훈련 M. 대량 파일 변경 dry run

목표:

```text
reports/ 안의 .txt 100개를 날짜_원래이름 형식으로 변경
```

바로 rename하지 않고 먼저 미리보기만 만든다.

```python
from pathlib import Path

root = Path("reports")
for path in root.glob("*.txt"):
    new_name = "2026-09-16_" + path.name
    print(path.name, "->", new_name)
```

실제 rename은 하지 않는다.

다음 검사를 추가해본다.

- 새 이름 중복 여부
- 대상 개수
- 예상하지 않은 확장자 포함 여부

---

## AI 답 검증 1

AI 답:

> `photo.png`를 `photo.jpg`로 바꾸면 JPEG 변환이 완료됩니다.

판정: 틀림.

최소 두 문장으로 반박한다.

---

## AI 답 검증 2

AI 답:

> 다운로드한 APK 파일명이 이전과 같으므로 동일한 APK입니다.

판정: 근거 부족.

더 강한 증거:

- SHA-256
- 서명 정보
- 파일 크기/버전 정보
- 신뢰 가능한 배포 출처

---

## 디버깅 미션 1 — 상대경로

코드:

```python
from pathlib import Path

path = Path("data/input.csv")
print(path.exists())
```

CI에서는 `False`, 내 PC에서는 `True`다.

조사할 증거를 순서대로 적는다.

```text
CI working directory
repository checkout path
실제 data/input.csv 존재 여부
대소문자 차이
.gitignore/배포 포함 여부
```

---

## 디버깅 미션 2 — 잘못된 인코딩 가정

코드:

```python
text = open("data.csv", encoding="utf-8").read()
```

어떤 외부 시스템이 CP949 파일을 제공해 오류가 난다.

잘못된 해결:

```text
errors="ignore"로 모든 오류 무시
```

왜 위험한가?

문자가 사라져도 프로그램이 계속 진행해 데이터 손상을 숨길 수 있다.

더 나은 방향:

- 공급 형식 확인
- 예상 인코딩 명시
- 실패를 명확히 기록
- 변환 후 검증

---

## 독립 프로젝트 — 배포 파일 증거 패키지

안전한 테스트 파일 또는 앱 산출물을 대상으로 다음 증거 패키지를 만든다.

```text
파일명
전체 경로
파일 크기
확장자
실제 형식
SHA-256
생성 시각
출처/빌드 commit
서명 정보(가능한 경우)
```

그 다음 같은 파일을 다른 폴더에 복사하고 해시를 비교한다.

한 바이트라도 바꾼 복사본을 만들 수 있다면 해시가 달라지는지도 확인한다.

---

# 2장 훈련 완료 기준

- 상대경로를 working directory 기준으로 계산한다.
- 같은 이름의 파일이 여러 개일 때 실제 사용 파일을 경로로 구분한다.
- 확장자 변경과 포맷 변환을 구분한다.
- CSV 인코딩/구분자 오류를 원인별로 나눈다.
- Base64를 암호화라고 부르지 않는다.
- 해시가 보장하는 것과 보장하지 않는 것을 구분한다.
- 파일 오류를 존재/경로/권한/형식/인코딩/무결성으로 분류한다.
- 대량 파일 변경 전에 dry run과 복구 계획을 세운다.
