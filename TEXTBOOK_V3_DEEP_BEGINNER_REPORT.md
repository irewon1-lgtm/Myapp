# TEXTBOOK V3 · Deep Beginner Extreme Fix Report

작업 브랜치: `textbook-v3-deep-beginner`

기준 작업 시작점: `2f6375baec7b90c83e4a71b54b2affa7ca793482`

이 보고서는 `완료/PASS`를 실제 실행 여부에 따라 분리한다.

## 1. 이번 수정 목표

V3 원고 자체는 이전 단어장형 실패를 상당 부분 해결했지만, 극한 시뮬레이션에서 새 위험이 확인됐다.

```text
1. 정확히 26 LESSON이라는 숫자가 품질 목표로 굳어질 위험
2. 60분을 넘는 LESSON도 UI에서 60분으로 잘려 과적재가 숨는 문제
3. V3 BLOCK 수가 바뀌면 grouping mismatch가 atomic split으로 조용히 fallback하는 문제
4. 긴 LESSON을 중간까지 읽고 앱을 닫으면 세로 위치가 복원되지 않는 문제
5. H4 제목과 본문이 같은 16sp라 긴 페이지 스캔성이 약한 문제
6. 코드 horizontalScroll과 LESSON horizontal swipe의 제스처 경쟁 가능성
7. V2 → V3 통합 과정에서 핵심 개념 누락을 직접 막는 coverage gate 부족
8. byte 기반 25x~44x 수치를 실제 교육 깊이 증가처럼 오해할 위험
```

이번 작업은 위 8개를 수정했다.

## 2. 실제 코드 변경

### 2.1 `TextbookSectioner` fail-fast

기존:

```text
grouping.sizes.sum() != atomic.size
→ atomic split 반환
```

문제:

```text
원고 BLOCK 하나 추가
↓ grouping table 갱신 누락
↓ 테스트/화면이 조용히 수십 개 조각 LESSON으로 후퇴 가능
```

수정:

```text
V3 grouping mismatch
→ IllegalArgumentException
→ 즉시 실패
```

Legacy/non-V3 synthetic caller는 기존 atomic split을 유지한다.

### 2.2 읽기시간 upper clamp 제거

기존 V3:

```kotlin
estimatedMinutes = raw.coerceIn(10, 60)
```

문제:

```text
실제 83분
→ UI 60분
→ 품질 테스트도 60분으로 보아 과적재 은폐 가능
```

수정:

```text
raw estimate를 그대로 유지
최소 10분만 보정
60분 초과면 실제 숫자가 드러남
새 실제 asset test가 실패해야 함
```

정식 V3 상한:

```text
10..60분
```

### 2.3 exact 26 gate 제거

현재 실제 grouping 결과는 여전히 26 LESSON이다.

하지만 테스트는 더 이상 `정확히 26`을 품질 기준으로 삼지 않는다.

새 기준:

```text
전체 learner LESSON: 20..50
TRACK별: 1..6
한 LESSON 내부 major BLOCK: 1..4
```

교육적으로 더 좋은 구조가 27/30/35 LESSON이면 바꿀 수 있다.

### 2.4 depth 양방향 gate

기존:

```text
V2 평균보다 20x 이상
```

만 검사해서 너무 긴 LESSON은 통과할 수 있었다.

새 실제 asset test:

```text
V2 reader page 평균 대비
V3 LESSON depth ratio 12x..60x
```

즉 너무 짧은 단어장과 너무 긴 벽글을 모두 막는다.

### 2.5 V3 critical coverage test

새 파일:

```text
V3CriticalCoverageTest.kt
```

11 TRACK의 핵심 spine을 검사한다.

대표:

```text
TRACK 03: array / node / stack / hash / tree / Big-O
TRACK 05: const / closure / Promise / async-await / event loop / TypeScript
TRACK 06: DNS / TCP / TLS / Content-Type / multipart / CORS
TRACK 08: PRIMARY KEY / FOREIGN KEY / JOIN / index / transaction / ACID
TRACK 09: threat / authentication / hash / encryption / SQL injection / XSS / CSRF / secret
TRACK 10: reproduction / unit / integration / Git / CI / artifact / rollback
TRACK 11: requirement / acceptance criteria / cohesion / coupling / DI / cache / circuit breaker / observability
```

사용자가 직접 문제 삼았던 4개는 별도 회귀 조건으로 추가했다.

```text
node + next + 중간 삽입
callback + Promise + microtask
MIME + Content-Type + boundary
데이터 무결성 + CHECK + 참조 무결성
```

주의: 이 test file은 작성/커밋했지만 Full Gradle suite를 아직 실행하지 않았으므로 `PASS`라고 표시하지 않는다.

## 3. reader UX 수정

### 3.1 V3 전용 progress namespace

기존 namespace:

```text
v2_track_progress
```

새 namespace:

```text
v3_deep_beginner_progress
```

이전 V1/V2 완료/위치가 V3 완료로 잘못 이어지지 않는다.

### 3.2 LESSON 세로 위치 저장/복원

기존 `TextbookProgressStore`에 함수는 있었지만 reader가 사용하지 않았다.

이번에 실제 연결했다.

저장:

```text
currentConcept.id
firstVisibleItemIndex
firstVisibleItemScrollOffset
```

reader에서는 `snapshotFlow`로 위치를 기록한다.

재진입:

```text
rememberLazyListState(
  initialFirstVisibleItemIndex = 저장 index,
  initialFirstVisibleItemScrollOffset = 저장 offset
)
```

offset은 48px bucket으로 기록해 스크롤 매 pixel마다 SharedPreferences write가 발생하는 것을 줄였다.

### 3.3 긴 글 제목 계층 강화

기존:

```text
H3 18sp
H4 16sp
본문 16sp
```

수정:

```text
H3 19sp / Bold / top 16dp
H4 17sp / SemiBold / top 10dp
본문 16sp
```

형광색을 추가하지 않고 크기·굵기·여백으로 계층을 만든다.

### 3.4 코드 horizontalScroll 제거

기존:

```text
코드 horizontalScroll
+
페이지 horizontal swipe
```

이 둘이 좁은 폰에서 gesture 경쟁할 가능성이 있었다.

수정:

```text
코드 softWrap = true
horizontalScroll 제거
LESSON swipe 유지
swipe threshold 86dp → 110dp
```

### 3.5 reader regression contract

새 파일:

```text
V3ReaderResilienceContractTest.kt
```

정적 계약:

- saved scroll을 `rememberLazyListState`에 넣는지
- `snapshotFlow`로 실제 저장하는지
- V3 progress namespace인지
- code horizontalScroll이 다시 들어오지 않는지
- `softWrap=true`인지
- H3/H4 시각 계층이 다시 무너지지 않는지

이 test도 Full Gradle 미실행이므로 아직 PASS라고 부르지 않는다.

## 4. byte 25x~44x 해석 수정

기존 보고서의:

```text
TRACK별 25.12x ~ 43.99x
```

는 `파일 byte / visible lesson count`와 V2 authored lesson 평균의 비교였다.

이 수치는:

```text
590 조각 → 26 큰 페이지로 합친 효과
UTF-8 한국어 byte 크기
실제 원고 증가
```

가 섞여 있다.

따라서 이제 **품질 PASS 근거로 사용하지 않는다.**

현재 파일 byte 통계는 참고 통계로만 유지한다.

실제 품질 gate는 parser 결과의 `weightedLength`, raw `estimatedMinutes`, internal BLOCK count를 사용한다.

## 5. 실제 실행한 검증

### PASS 1 · standalone Kotlin resilience compile/run

로컬 환경:

```text
kotlinc available
JDK 21
```

`TextbookBlock stub + 실제 수정 TextbookSectioner + harness`를 실제 컴파일/실행했다.

결과:

```text
V3_SECTIONER_RESILIENCE_PASS sections=2 minutes=[83, 55]
```

검증:

- V1-C01 grouping = 2
- legacy `LESSON 01` label 제거
- overlong synthetic content가 60으로 clamp되지 않고 83분으로 노출
- BLOCK mismatch가 예외로 실패

### PASS 2 · 서로 다른 실패 유형 CLEAN 7

실제 Kotlin compile/run 결과:

```text
V3_EXTREME_CLEAN7_PASS
sections=2
overlong=[83, 55]
estimator=[1, 1, 10, 100]
```

실제 검증한 7종:

1. 정상 V3 grouping 수
2. legacy/non-V3 atomic content preservation
3. old tiny LESSON label 제거
4. V3 authored BLOCK mismatch fail-fast
5. overlong lesson raw minute 노출
6. Heading/Paragraph/Bullet/Code/Table/Divider weight 양수
7. minute estimator monotonic + upper clamp 없음

### PASS 3 · 실제 `TextbookBlock` data shape 호환 compile/run

stub의 Code constructor 순서와 실제 프로젝트 model 차이까지 제거하기 위해 실제 `TextbookBlock` shape로 다시 컴파일했다.

결과:

```text
REAL_MODEL_SECTIONER_PASS [84, 56]
```

즉 수정한 `TextbookSectioner` 자체는 실제 model signature와 호환되는 상태로 standalone Kotlin compile/run PASS다.

## 6. 실제 repository 반영 증거

작업 시작점:

```text
2f6375baec7b90c83e4a71b54b2affa7ca793482
```

이후 별도 branch에 다음 변경을 실제 커밋했다.

```text
Harden V3 lesson grouping and expose true reading time
Isolate V3 textbook progress namespace
Replace fixed 26-page gate with depth and overload guards
Add V3 critical concept coverage regression gate
Restore V3 reading position and improve long-form reader hierarchy
Add V3 long-reader resilience regression contract
Replace exact-26 target with V3 quality bounds and resilience contract
```

`main`에는 병합하지 않았다.

## 7. 아직 실행하지 못한 검증

현재 실행 container에서 GitHub DNS 실패는 다시 재현됐다.

```text
fatal: unable to access 'https://github.com/irewon1-lgtm/Myapp.git/':
Could not resolve host: github.com
```

따라서 아래는 아직 PASS가 아니다.

```text
전체 repository clone
./gradlew test
TextbookRealAssetSectionTest 실제 11 TRACK 전수 실행
V3CriticalCoverageTest JVM 실행
V3ReaderResilienceContractTest JVM 실행
Android instrumented tests
Galaxy Tab 실제/에뮬레이터 swipe/scroll-resume 확인
APK build/install
```

특히 **새 10~60분 actual asset gate가 현재 26개 LESSON을 전부 통과하는지는 아직 실제 JVM 실행 증거가 없다.**

이 gate가 실패하면 `26 유지`를 위해 시간을 다시 clamp하지 않고, 해당 TRACK grouping을 더 교육적으로 나누는 것이 다음 수정 원칙이다.

## 8. 승인 경계

실행하지 않음:

```text
main merge
GitHub Actions
APK release/deploy
production 변경
유료 실행
```

현재 작업은 `textbook-v3-deep-beginner` branch 안의 코드/test/document 수정과 무료 로컬 Kotlin standalone 검증까지만 수행했다.

## 9. 현재 판정

### 실제 PASS라고 말할 수 있는 것

```text
TextbookSectioner 수정본 standalone Kotlin compile/run
CLEAN 7 서로 다른 실패 유형
실제 TextbookBlock model shape 호환 compile/run
GitHub branch 코드 반영
```

### 코드상 수정 완료했지만 Full suite PASS는 아닌 것

```text
scroll resume wiring
V3 progress isolation
reader H3/H4 hierarchy
code soft-wrap / horizontal-scroll 제거
actual asset depth/time gate
critical content coverage gate
reader resilience source contract
```

### 아직 미검증

```text
Full Gradle/JVM
Android UI
Galaxy Tab 실화면
APK
```

따라서 **전체 프로젝트 PASS라고 보고하지 않는다.**
