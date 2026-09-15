# 11장 실전 훈련편 — V1 종합 장애 대응 시험

이 훈련편은 앞 장 설명을 반복하지 않는다. **새로운 장애 묶음**을 주고, 학습자가 어떤 증거를 먼저 보고 어떤 가설을 기각할지 직접 결정한다.

---

# 종합 시나리오 1. 앱 화면은 정상인데 데이터가 어제 값이다

관찰:

```text
앱 실행 정상
인터넷 정상
API status 200
raw JSON price=21000
DB row price=21000 updatedAt=09:01
UI text price=19500
```

## 해야 할 일

1. 시스템 경로를 다시 적는다.
2. 이미 정상이라고 볼 수 있는 단계를 표시한다.
3. 남은 가설을 최소 3개 적는다.
4. 다음으로 가장 싸게 볼 증거를 고른다.

강한 후보 예:

```text
UI state가 DB 최신값을 구독하지 않음
화면에 오래된 cache state 남음
formatter/input source가 다른 필드 사용
```

서버/API를 다시 만드는 것은 우선순위가 낮다.

---

# 종합 시나리오 2. 업데이트 후 일부 사용자 데이터가 사라졌다

관찰:

```text
v2 신규 설치 정상
v1 → v2 업데이트 사용자 중 8%에서 데이터 0건
migration v5→v6 존재
crash는 없음
```

## 가설 후보

```text
특정 구버전 schema 변형 미처리
migration WHERE 조건 오류
계정/tenant mapping 변경
기존 DB 파일 경로 변경
migration 실패를 무시하고 새 DB 생성
```

## 증거 계획

```text
실패 사용자 DB schema version
migration 로그
migration 전/후 row count
DB 파일 path
계정/tenant id
```

## 금지 행동

```text
실패 사용자 앱 삭제 후 재설치부터 하기
```

왜 금지인지 적는다. 로컬 증거가 사라질 수 있기 때문이다.

---

# 종합 시나리오 3. 자동화 결과가 두 번 저장된다

로그:

```text
09:00:00 scheduler job=J1
09:00:01 worker run=R1
09:02:01 client timeout R1
09:02:02 retry run=R2
09:02:30 R1 save key=AAPL-M01-20260916
09:03:00 R2 save key=AAPL-M01-20260916
```

DB에는 동일 데이터가 두 행 있다.

## 질문

1. scheduler가 두 번 trigger됐나?
2. worker retry가 겹쳤나?
3. timeout이 R1 실제 취소를 의미했나?
4. 저장 단계에 unique/idempotency 정책이 있나?

## 수정 후보

```text
unique business key
idempotency key
upsert policy
run state/lock
retry 조건 개선
```

한 가지 해결만 맹목적으로 선택하지 말고 데이터 의미에 맞는 조합을 설계한다.

---

# 종합 시나리오 4. 같은 API인데 브라우저만 실패한다

증거:

```text
curl: 200
Android native app: 200
browser: CORS blocked
```

전체 API 장애인가? 아니다.

## 조사

```text
Origin
Access-Control-Allow-Origin
preflight OPTIONS
credentials/cookie 정책
```

V1에서 CORS 세부 구현을 끝낼 필요는 없다. 중요한 것은 **실행 환경별 네트워크 경계가 다르다**는 판정이다.

---

# 종합 시나리오 5. 특정 기기에서만 앱이 시작 즉시 꺼진다

정상:

```text
Android 16 Galaxy A
Android 15 Pixel
```

실패:

```text
Android 12 device X
```

stack trace:

```text
UnsatisfiedLinkError: native library not found for ABI ...
```

가능한 후보:

- release에 해당 ABI native library 누락
- minSdk/OS compatibility

서버 장애 가설은 우선순위가 낮다.

## 회귀 매트릭스

```text
대표 ABI × 최소 OS × 최신 OS
신규 설치 × 업데이트 설치
```

---

# 종합 시나리오 6. 앱이 오래 켜져 있을수록 느려진다

측정:

```text
시작 220MB
30분 350MB
1시간 520MB
2시간 850MB
화면 A를 열고 닫을 때마다 +25MB
```

## 가설

```text
화면 A listener 미해제
bitmap cache 무제한
coroutine/task reference 유지
DB cursor/resource 미해제
```

## 실험

```text
화면 A 20회 반복
heap snapshot 비교
이미지 기능 off 비교
listener count 비교
```

RAM이 큰 기기에서 잘 된다는 사실로 누수 가능성을 제거하지 않는다.

---

# 종합 시나리오 7. 주가 데이터가 특정 종목만 100배 차이

관찰:

```text
대부분 종목 정상
일부 종목만 100배 차이
```

가능한 후보:

- 통화 단위
- 액면분할/병합
- ADR ratio
- cents vs dollars
- source field mapping

## 첫 단계

샘플 한 종목에 대해:

```text
원본 API 값
원본 단위
corporate action
변환식
DB 값
UI 값
```

을 모두 한 줄에 놓는다.

“전체 수집 엔진이 망가졌다”는 결론은 과도하다.

---

# 종합 시나리오 8. 빌드는 성공했는데 설치가 안 된다

증거:

```text
assembleRelease PASS
APK 파일 존재
apksigner verify FAIL
```

빌드 성공과 배포 가능 상태를 구분한다.

추가 확인:

```text
signing config
keystore
certificate
zipalign
version code
기존 앱 signature
```

보고서 상태:

```text
build PASS / signing FAIL / install NOT-RUN
```

---

# 종합 시나리오 9. AI가 “전체 테스트 PASS”라고 보고했다

실제 증거:

```text
CI run 없음
명령 로그 없음
APK 없음
실기기 결과 없음
```

판정:

```text
AI 주장만 존재
검증 NOT-RUN
```

필요한 증거:

- 실제 CI run ID
- test count/failure count
- build artifact
- signing verification
- 설치/실행 로그

AI의 자신감 표현을 증거로 바꾸지 않는다.

---

# 종합 시나리오 10. 수정했는데 다른 기능이 깨졌다

수정:

```text
공통 JSON parser에서 null 처리 변경
```

직접 수정 대상:

```text
주가 API
```

회귀 실패:

```text
가족 일정 API의 optional memo 필드가 빈 문자열로 변환됨
```

## 교훈

파일 하나의 변경도 공통 모듈이면 영향 범위가 넓다.

변경 전에 검색할 것:

```text
어디서 이 parser를 사용하는가?
어떤 schema가 null을 허용하는가?
공통 테스트 fixture가 있는가?
```

---

# 실기시험 A. 증거 없이 다음 단계 선택하기

상황:

```text
앱에서 저장 버튼을 누르면 가끔 2개가 생김
```

가능한 첫 행동 5개를 적고, 정보량/위험도 순서로 정렬한다.

예:

```text
1. 저장 요청 로그에 requestId 추가/확인
2. 버튼 클릭 이벤트 횟수 확인
3. 서버 request count 확인
4. DB unique key 확인
5. 앱 재설치
```

보통 5번은 초반 정보량이 낮고 증거를 지울 수 있다.

---

# 실기시험 B. 원인과 resilience 분리

root cause:

```text
외부 API가 하루 3번 20초 timeout
```

사용자 피해 완화:

```text
5초 timeout
stale cache 표시
재시도 버튼
background retry
```

이 방어책은 root cause 자체를 고친 것은 아니다.

보고서에 두 섹션으로 나눈다.

```text
원인 수정
사용자 영향 완화
```

---

# 실기시험 C. 데이터 손상 위험도 평가

세 버그:

```text
A 버튼 글자 2px 잘림
B 일부 사용자의 데이터 중복 저장
C 앱 시작 0.5초 느림
```

긴급도는 단순 재현 빈도만으로 정하지 않는다.

고려:

- 데이터 파괴/금전 영향
- 사용자 범위
- 복구 가능성
- 보안 영향
- 회피 방법

일반적으로 B가 높은 우선순위가 될 가능성이 크다.

---

# 실기시험 D. 회귀 범위 만들기

변경:

```text
device token 등록 API를 upsert로 변경
```

최소 회귀 범위:

```text
신규 기기 등록
같은 기기 token 갱신
사용자 2개 기기
로그아웃/재로그인
앱 재설치
invalid token 정리
다른 가족 구성원 알림
작성자 제외 규칙
```

기능 하나를 고쳐도 관련 정책 전체를 본다.

---

# 실기시험 E. 실패 보고서 한 장 만들기

다음 형식을 직접 작성한다.

```text
제목:
영향:
시작 시각:
재현:
정상 비교군:
시스템 경로:
최초 실패 증거:
가설:
기각된 가설:
root cause:
수정:
원래 실패케이스 결과:
회귀시험:
남은 위험:
rollback:
증거 위치:
```

5분 안에 다른 사람이 핵심을 이해할 수 있어야 한다.

---

# 실기시험 F. AI 작업 인수

AI에게 다음 작업을 시켰다고 하자.

```text
"알림 중복을 고치고 테스트해"
```

AI 결과를 인수할 때 확인할 체크리스트:

```text
[ ] 수정 파일 목록
[ ] diff
[ ] root cause 설명
[ ] 보존해야 할 기존 정책
[ ] 단위 테스트
[ ] 통합 테스트
[ ] 실제 run ID
[ ] APK/build artifact 필요 여부
[ ] rollback commit
[ ] 남은 위험
```

“완료했습니다” 문장만으로 인수하지 않는다.

---

# 최종 V1 프로젝트 — 실제 앱 기능 한 개 감사

자신이 실제 사용하는 앱 기능 하나를 고른다.

추천:

- 주가 지표 한 개
- 가족 알림 한 개
- CRM 저장 한 개
- 파일 다운로드/업데이트 한 개

## 1. 시스템 지도

```text
입력 → 앱 상태 → 저장/네트워크 → 서버 → 응답 → 검증 → UI
```

실제 시스템에 맞게 수정한다.

## 2. 신뢰 경계

```text
사용자 입력
외부 API
로컬 DB
서버 DB
OS permission
AI 생성 코드
```

어디를 그대로 믿지 말아야 하는지 표시한다.

## 3. 실패 시나리오 10개

정상만 쓰지 않는다.

예:

```text
network offline
401
429
invalid JSON
null field
stale cache
process death
duplicate tap
migration
old token
```

## 4. 관찰 가능성

각 단계에서 어떤 로그/메트릭/데이터로 정상 여부를 증명할지 적는다.

## 5. 테스트

```text
정상
경계
실패
재시도
중복
재실행
업데이트
복구
```

## 6. 실제 실행

가능한 테스트는 실제로 실행한다.

실행할 수 없는 항목은 `NOT-RUN`으로 남긴다.

## 7. 최종 인수 보고서

PASS/FAIL/NOT-RUN과 증거를 붙인다.

---

# V1 종합 합격 기준

아래를 실제로 할 수 있어야 한다.

- 화면 결과를 여러 시스템 단계로 분해한다.
- 원본 데이터와 화면 표시를 직접 대조한다.
- 파일/권한/형식 문제를 구분한다.
- 비동기 순서·중복·race 후보를 인식한다.
- 성능을 측정하고 병목을 찾는다.
- 위험한 터미널 명령을 실행 전에 감사한다.
- dependency/migration/signing 실패를 단계로 나눈다.
- 로그에서 최초 실패와 후속 증상을 분리한다.
- DNS/TLS/HTTP/parse 오류를 구분한다.
- Android token/channel/background/sync 문제를 비교 진단한다.
- 가설을 만들고 최소 실험으로 기각한다.
- 회귀시험과 rollback을 포함한 완료 기준을 만든다.
- AI의 “PASS”를 실제 증거와 구분한다.

이 기준을 통과하면 V2에서 Python 문법을 배울 때 단순 암기가 아니라 **실제 시스템을 이해하면서 코드를 읽고 수정하는 기반**이 생긴다.
