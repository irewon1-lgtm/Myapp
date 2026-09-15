# 10장 실전 훈련편 — 재현·가설·격리·경계값·회귀시험

이 훈련편은 디버깅 방법론을 읽는 데서 끝내지 않는다. **불완전한 증거를 보고 다음 실험을 설계하는 훈련**을 반복한다.

---

## 훈련 A. 모호한 버그 보고를 재현 가능한 문장으로 바꾸기

원문:

```text
가끔 검색이 이상해요.
```

추가 질문을 만든다.

```text
어떤 검색어?
어떤 기기/앱 버전?
몇 번 중 몇 번?
정상 기대 결과?
실제 결과?
네트워크?
로그인 상태?
처음 실행/재실행?
```

개선 예:

```text
Android 16, 앱 1.5.0에서
로그인 후 AAPL 검색을 10회 반복하면 3회에서 이전 MSFT 결과가 표시됨.
Wi-Fi/5G 모두 재현.
기대: 항상 AAPL 결과.
```

---

## 훈련 B. 관찰과 해석 분리

다음을 `관찰`과 `해석`으로 나눈다.

```text
HTTP 500이 기록됨
서버가 완전히 망가졌다
DB row count가 0임
migration이 데이터를 삭제했다
배우자 폰 channel=OFF
알림 미수신 원인은 channel이다
```

관찰:

- HTTP 500
- row count 0
- channel OFF

해석/가설:

- 서버 전체 장애
- migration 데이터 삭제
- channel이 원인

가설은 추가 증거로 검증한다.

---

## 훈련 C. 가설을 최소 3개 만드는 연습

증상:

```text
가격이 화면에 0으로 표시
```

가설 예:

```text
H1 API가 0 반환
H2 parser가 null을 0으로 변환
H3 DB default가 0
H4 UI formatter가 오류 시 0 표시
H5 다른 종목 mapping
```

가설을 많이 만드는 목적은 쓸데없이 상상하는 것이 아니라 **한 원인에 너무 빨리 고정되는 것을 막는 것**이다.

---

## 훈련 D. 가설별 예상 증거 만들기

H1 API 0:

```text
raw response price=0
```

H2 parser null→0:

```text
raw response price=null
parse result=0
```

H3 DB default:

```text
parse result missing
DB row price=0
```

H4 UI formatter:

```text
state price=null/200
UI text=0
```

증거를 미리 적으면 조사 순서가 빨라진다.

---

## 훈련 E. binary isolation

전체 경로:

```text
source → API → parser → DB → API2 → app state → UI
```

먼저 중간 `DB` 값이 정상인지 본다.

- DB 정상 → 앞 절반 후보 낮추고 뒤 절반 조사
- DB 오류 → 앞 절반 조사

그 다음 다시 중간 지점을 확인한다.

정확한 이진탐색 알고리즘이 아니어도 **중간 증거로 후보 절반을 제거하는 사고**가 유용하다.

---

## 훈련 F. 파괴적 실험과 정보량

버그: 로컬 DB 데이터 불일치.

실험 후보:

A. 앱 삭제/재설치

B. DB row와 schema version 읽기

C. cache만 별도 복사 후 비교

첫 행동으로 어떤 것이 정보량 대비 안전한가?

보통 B/C가 낫다. A는 원인 증거를 지울 수 있다.

---

## 훈련 G. 한 변수씩 바꾸기

실패 상태:

```text
API timeout
```

한 번에 다음을 모두 바꾼다.

```text
timeout 5→30초
DNS 변경
HTTP client 교체
retry 추가
server 재시작
```

문제가 사라졌다.

질문:

- 어떤 변경이 효과 있었는가?
- 새 버그를 어떤 변경이 만들었는가?

알 수 없다.

다시 baseline으로 돌아가 한 변경씩 시험한다.

---

## 훈련 H. 경계값 20개 만들기

### 조건 1

```text
합격 = score >= 80
```

대표:

```text
79, 80, 81
```

### 조건 2

```text
수량 1~100
```

대표:

```text
0,1,2,99,100,101, 빈 값, null, 문자열 "100"
```

### 조건 3

```text
문자열 길이 최대 20
```

대표:

```text
길이 0,1,19,20,21
한글 20자
emoji 포함
공백만
```

단순 숫자 경계뿐 아니라 타입과 문자 인코딩도 함께 본다.

---

## 훈련 I. 테스트 oracle 만들기

테스트는 입력만 있어서는 안 된다. **기대 결과(oracle)**가 필요하다.

나쁜 테스트:

```text
AAPL 검색해보기
```

좋은 테스트:

```text
입력: AAPL
fixture: price=200, currency=USD
기대: 화면 "200 USD"
기대하지 않음: MSFT/원화 표시/crash
```

외부 API처럼 값이 바뀌는 시스템은 고정 fixture/mock을 사용해 결정성을 높일 수 있다.

---

## 훈련 J. flaky test와 실제 버그 구분

테스트 100회 중 3회만 실패한다.

가능한 후보:

- race condition
- 실제 네트워크 의존
- 시간대/현재시각 의존
- 랜덤 seed 비고정
- shared global state
- 테스트 간 데이터 정리 실패

`재실행해서 PASS`만 반복하지 말고 실패 패턴을 조사한다.

---

## 훈련 K. regression scope 정하기

수정:

```text
공통 MoneyFormatter에서 percent 처리 수정
```

직접 영향:

- 수익률
- 마진
- 할인율

간접 영향:

- 주가 숫자 formatter를 공유하는지
- 보고서 export가 같은 함수 사용하는지

수정 파일이 하나라도 영향 범위는 여러 화면일 수 있다.

---

## 훈련 L. 회귀시험을 위험 기반으로 우선순위화

시간이 10분뿐이라 전체 E2E를 못 돌린다고 하자.

우선순위 기준:

```text
수정과 직접 연결
사용자 영향 큼
데이터 파괴 가능성
과거 장애 이력
변경된 공통 모듈 사용 빈도
```

모든 테스트가 같은 가치라고 가정하지 않는다.

---

## 훈련 M. 테스트가 PASS인데 운영에서 실패

가능한 이유:

- production config 차이
- 실제 데이터 규모
- 실제 latency
- 권한/OS 차이
- migration fixture 부족
- mock이 실제 API와 다름

자동 테스트 PASS는 강한 증거지만 **운영 환경 동일성까지 보장하는 것은 아니다.**

---

## 훈련 N. rollback 기준 미리 정하기

배포 후 이런 조건이면 rollback한다고 미리 정한다.

```text
crash-free users < 99.5%
데이터 저장 실패 > 1%
결제 중복 1건 이상
migration failure > 0.1%
```

숫자는 서비스 위험에 맞춰 정한다.

장애가 난 뒤 감정적으로 결정하는 것보다 사전 기준이 낫다.

---

## 훈련 O. 원인 수정과 방어코드 분리

원인:

```text
서버가 간헐적으로 30초 timeout
```

클라이언트 방어:

```text
crash 대신 timeout 안내
limited retry
stale cache 표시
```

방어코드는 사용자 피해를 줄이지만 서버 timeout root cause를 고친 것은 아니다.

보고서에서 둘을 분리한다.

---

## 훈련 P. “최소 수정”이 항상 한 줄이라는 뜻은 아니다

원인이 데이터 schema 계약 불일치라면:

- schema validator 추가
- parser 수정
- migration
- 테스트 fixture

등 여러 파일이 필요할 수 있다.

최소 수정은 **원인 해결에 필요한 범위만 바꾼다**는 뜻이지 줄 수 경쟁이 아니다.

---

## AI 답 검증 1

AI 답:

> 코드를 읽어보니 논리상 맞으므로 PASS입니다.

판정: 틀림.

실행/테스트하지 않았다면 `검토상 이상 없음, 실행 NOT-RUN` 정도가 정확하다.

---

## AI 답 검증 2

AI 답:

> 버그가 재현되지 않으니 해결됐습니다.

판정: 근거 부족.

환경/빈도/조건이 바뀌어 재현되지 않을 수도 있다. 수정 전 재현과 수정 후 동일 조건 비교가 필요하다.

---

## 디버깅 미션 1 — 6개월 수익률

앱 값:

```text
-45%
```

원자료 손계산:

```text
+12%
```

조사 장부:

```text
current date:
current price:
target date - 6 months:
actual trading day selected:
start price:
adjusted/unadjusted:
corporate actions:
formula:
stored value:
UI value:
```

값이 처음 달라지는 칸을 찾는다.

---

## 디버깅 미션 2 — PER 누락

100종목 중 40개 PER 없음.

누락 원인 분류:

```text
EPS null
적자 정책
mapping fail
price missing
period mismatch
parser error
```

원인별 개수를 세고 복구 ROI가 큰 그룹부터 수정한다.

---

## 디버깅 미션 3 — 알림

동일 event에서 2/3명 수신.

가설:

```text
server global failure
recipient mapping
stale token
channel
battery restriction
```

정상 2명을 비교군으로 사용해 공통 서버 장애 가능성을 낮춘다.

---

## 독립 프로젝트 — Bug Investigation Pack

실제 작은 버그 하나를 골라 파일 세트를 만든다.

### `01_problem.md`

```text
환경
재현
기대
실제
빈도
사용자 영향
```

### `02_hypotheses.md`

```text
H1 / 예상 증거 / 결과
H2 / 예상 증거 / 결과
H3 / 예상 증거 / 결과
```

### `03_evidence.md`

원본 로그/스크린샷/데이터 위치를 기록한다. secret은 제거한다.

### `04_change.md`

```text
root cause
수정 파일
변경하지 않은 범위
rollback
```

### `05_tests.md`

```text
원래 실패 케이스
경계값
회귀시험
실행 결과
```

AI가 작성해도 각 증거 링크와 실제 실행 결과는 사람이 확인한다.

---

# 10장 훈련 완료 기준

- 모호한 버그 보고를 재현 가능한 문장으로 바꾼다.
- 관찰과 가설을 구분한다.
- 최소 3개의 경쟁 가설과 예상 증거를 만든다.
- 중간 지점 증거로 시스템 범위를 빠르게 줄인다.
- 파괴적 실험보다 값싼 실험을 먼저 한다.
- 숫자/문자/날짜/타입 경계 테스트를 설계한다.
- flaky test를 단순 재실행으로 덮지 않는다.
- 수정 영향에 맞는 회귀 범위를 정한다.
- PASS/NOT-RUN을 실제 실행 증거로 구분한다.
- Bug Investigation Pack을 완성할 수 있다.
