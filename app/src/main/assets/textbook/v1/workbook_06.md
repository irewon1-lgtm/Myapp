# 6장 실전 훈련편 — 버전·의존성·빌드·migration·rollback

이 훈련편은 버전 숫자를 외우는 대신 **업데이트가 실제로 어떤 단계에서 깨지는지 재현하고 증거로 좁히는 연습**을 한다.

---

## 훈련 A. 다운로드·설치·실행·기능 정상 분리

다음 상황을 단계별로 판정한다.

### A-1

```text
APK 다운로드 완료
설치 버튼 → "앱을 설치할 수 없습니다"
```

판정:

```text
다운로드 PASS
설치 FAIL
실행 NOT-RUN
기능 NOT-RUN
```

### A-2

```text
설치 성공
앱 아이콘 탭
즉시 crash
```

판정:

```text
다운로드 PASS
설치 PASS
프로세스 시작 후 실행 FAIL
기능 NOT-RUN
```

### A-3

```text
앱 실행 성공
홈 화면 정상
저장 버튼만 crash
```

판정:

```text
설치/시작 PASS
저장 기능 FAIL
```

한 단계 성공을 전체 PASS로 쓰지 않는다.

---

## 훈련 B. 버전 조합 표 만들기

환경:

```text
App 2.0
Runtime 17
Library A 4.1
Library B 2.5
API schema v3
```

정상 조합을 표로 고정한다.

| 구성요소 | 정상 버전 | 변경 후보 |
|---|---:|---:|
| App | 2.0 | 2.1 |
| Runtime | 17 | 21 |
| Library A | 4.1 | 5.0 |
| Library B | 2.5 | 2.6 |
| API schema | v3 | v3 |

AI에게 전부 최신화해달라고 하기 전에 **어느 변경이 필요한지** 결정한다.

---

## 훈련 C. 한 번에 세 dependency 업데이트했을 때

변경:

```text
A 1.0 → 2.0
B 3.0 → 4.0
C 5.1 → 6.0
```

결과:

```text
빌드 실패
```

원인 후보는 최소 세 개다.

더 좋은 복구 방식:

```text
마지막 정상 상태 복구
A만 업데이트 → 테스트
B만 업데이트 → 테스트
C만 업데이트 → 테스트
필요하면 조합 테스트
```

변경량을 줄이면 원인 정보를 얻는다.

---

## 훈련 D. lock file diff 읽기

업데이트 PR에서 코드 5줄만 바꿨는데 lock file에서 패키지 80개 버전이 변했다.

질문:

- 직접 의존성 하나가 많은 transitive dependency를 바꿨나?
- 패키지 관리자 전체 재해석이 일어났나?
- 의도한 변경인가?

lock file의 큰 diff를 “자동 파일이니까 무시”하지 않는다.

---

## 훈련 E. 빌드 성공과 테스트 성공 구분

로그:

```text
compile PASS
assembleRelease PASS
unit test NOT-RUN
install NOT-RUN
launch NOT-RUN
```

보고서 문장:

```text
Release APK 빌드 성공. 기능 검증은 아직 미실행.
```

잘못된 문장:

```text
앱 완벽하게 정상.
```

---

## 훈련 F. 신규 설치 정상 / 업데이트 실패

관찰:

```text
v2 신규 설치 10/10 PASS
v1 → v2 업데이트 0/10 PASS
```

업데이트 경로에만 존재하는 것들을 적는다.

```text
기존 DB
기존 SharedPreferences/설정
기존 cache
기존 파일
migration
이전 계정/session 상태
```

migration과 persisted-state 호환성이 우선 후보가 된다.

---

## 훈련 G. schema migration 손계산

v1 DB:

```text
users(id INTEGER, name TEXT)
```

v2 DB:

```text
users(id INTEGER, name TEXT, email TEXT NULL)
```

기존 1,000행을 보존하면서 nullable email을 추가하려 한다.

검증 항목:

```text
migration 전 row count = 1000
migration 후 row count = 1000
기존 id/name 보존
email = null 허용
schema version 증가
```

단순히 앱이 켜진다고 데이터 보존이 증명되는 것은 아니다.

---

## 훈련 H. destructive migration 위험

가장 쉬운 개발용 해결:

```text
DB 삭제 후 새 schema 생성
```

테스트 데이터에서는 편할 수 있지만 운영 사용자 데이터가 모두 사라질 수 있다.

운영 migration에서 이런 옵션을 사용하기 전:

- 사용자 데이터 가치
- backup
- 복구 정책
- 명시적 동의
- migration 테스트

를 확인한다.

---

## 훈련 I. 구버전 fixture 만들기

업데이트 테스트를 자동화하려면 구버전 상태가 필요하다.

fixture 예:

```text
v1 schema DB
사용자 10명
일정 50개
설정 6개
cache 일부
```

테스트:

```text
fixture 로드
→ v2 migration 실행
→ row/schema 검증
→ 앱 기능 test
```

신규 DB로 migration 함수를 호출하는 것보다 실제 구버전 구조에서 시험하는 편이 강하다.

---

## 훈련 J. 서명 불일치

상황:

```text
package name 동일
version code 더 높음
APK 정상 다운로드
기존 앱 위 설치 실패
```

후보:

- signing certificate 다름

개발 debug key와 release key를 혼동하거나 다른 release key로 서명하면 업데이트가 거절될 수 있다.

APK 파일명만 같다고 같은 배포 신원은 아니다.

---

## 훈련 K. minSdk/ABI 문제

새 버전에서 최소 Android 버전을 올렸다.

```text
v1 minSdk 26
v2 minSdk 31
```

Android 29 기기는 v1을 쓸 수 있었지만 v2 설치가 불가능해질 수 있다.

또 native library가 특정 ABI만 포함하면 일부 CPU 기기에서 문제가 날 수 있다.

업데이트 대상 기기 범위를 release 전에 확인한다.

---

## 훈련 L. API schema 호환성

서버 응답 v1:

```json
{"price":100}
```

서버 응답 v2:

```json
{"quote":{"price":100}}
```

앱이 옛 경로 `price`만 찾으면 깨진다.

가능한 대응:

- API versioning
- 하위 호환 기간
- 앱 업데이트 선행
- parser가 두 형태를 일시 지원

제품 상황에 맞는 migration 전략이 필요하다.

---

## 훈련 M. config drift

코드는 동일하지만 환경별 설정이 달라 실패한다.

```text
dev: API_URL=https://dev.example
prod: API_URL=https://api.example
```

운영 빌드에서 dev URL이 들어갔다.

검증:

```text
빌드 산출물의 effective config
CI environment
runtime environment
secret store
```

“내 PC에서는 됨”은 운영 설정 검증이 아니다.

---

## 훈련 N. rollback이 안 되는 migration

v2 migration에서 문자열 날짜를 epoch 숫자로 변환하면서 원본 문자열 열을 삭제했다고 하자.

v1로 rollback하면 v1 코드가 기대한 원본 열이 없다.

질문:

- APK rollback만으로 충분한가?
- reverse migration이 가능한가?
- 배포 전에 DB backup이 필요한가?

**코드 rollback과 데이터 rollback은 별개**다.

---

## 훈련 O. staged rollout 사고실험

전체 사용자에게 한 번에 v2를 배포하는 대신:

```text
내부 테스트
→ 1%
→ 10%
→ 50%
→ 100%
```

처럼 점진 배포한다고 하자.

각 단계에서 볼 지표:

- crash rate
- ANR
- migration failure
- 핵심 기능 성공률
- support tickets
- 데이터 오류

문제가 생기면 범위를 제한하고 rollback/중단할 수 있다.

---

## AI 답 검증 1

AI 답:

> `assembleRelease`가 성공했으므로 APK 설치와 기능까지 검증됐습니다.

판정: 틀림.

빌드·서명·설치·실행·기능 테스트는 별도 단계다.

---

## AI 답 검증 2

AI 답:

> 신규 설치에서 정상이라 migration도 정상입니다.

판정: 틀림.

신규 설치는 migration 경로를 아예 통과하지 않을 수 있다.

---

## 디버깅 미션 1 — dependency update 후 런타임 crash

빌드는 성공하지만 실행 시:

```text
NoSuchMethodError
```

가능한 후보:

- compile 시 사용한 API와 runtime 실제 라이브러리 버전 불일치
- transitive dependency 충돌

확인:

```text
dependency tree
lock file
실제 packaged version
stack trace
```

---

## 디버깅 미션 2 — 업데이트 후 특정 사용자만 crash

특징:

```text
신규 사용자 정상
오래 사용한 계정만 crash
```

강한 후보:

- 과거 버전에서 남은 특이 데이터
- null 허용 규칙 변화
- 오래된 cache/schema

샘플 사용자 데이터를 익명화해 재현 fixture로 만든다.

---

## 독립 프로젝트 — Release Gate 설계

앱 v2를 배포한다고 가정한다. 다음 gate를 직접 채운다.

```text
[코드]
unit tests:
integration tests:

[데이터]
migration fixture:
row-count invariant:
rollback backup:

[빌드]
debug APK:
release APK:
signing verify:

[기기]
최소 OS:
대표 OS:
업데이트 install:
신규 install:

[서비스]
API schema compatibility:
feature flag:

[운영]
crash monitoring:
rollback trigger:
```

각 항목에 PASS 증거가 없으면 `NOT-RUN` 또는 `FAIL`로 표시한다.

---

# 6장 훈련 완료 기준

- 다운로드/설치/실행/기능 단계를 따로 판정한다.
- dependency 여러 개를 한 번에 바꾸지 않고 원인을 분리한다.
- lock file의 의도치 않은 대규모 변화를 확인한다.
- 신규 설치와 업데이트 migration 테스트를 구분한다.
- 구버전 fixture로 데이터 보존을 검증한다.
- signing/minSdk/ABI 때문에 설치가 실패할 수 있음을 안다.
- API schema/config drift도 버전 호환 문제로 본다.
- 코드 rollback과 데이터 rollback을 별도로 설계한다.
- release gate를 실제 증거 기반으로 작성한다.
