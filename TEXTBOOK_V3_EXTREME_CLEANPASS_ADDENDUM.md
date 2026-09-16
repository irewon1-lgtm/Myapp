# TEXTBOOK V3 · Extreme Simulation / Clean Pass Addendum

대상 브랜치: `textbook-v3-deep-beginner`

코드 기준점:

```text
작업 시작: 2f6375baec7b90c83e4a71b54b2affa7ca793482
Extreme60 실제 asset 보강 완료 코드 HEAD: 44986f5a950288d50fc11d179701ccaef96fc0bc
```

이 문서는 실제 실행한 것과 아직 실행하지 못한 것을 분리한다.

## 1. 극한 시뮬레이션에서 잡은 실패 유형

```text
A. exact 26 LESSON 숫자 맞추기 때문에 과적재되는 실패
B. 60분 이상 LESSON을 UI에서 60분으로 숨기는 실패
C. authored BLOCK count mismatch가 atomic pages로 조용히 fallback하는 실패
D. 긴 LESSON 중간에서 앱 종료 후 읽던 세로 위치를 잃는 실패
E. H4와 본문 크기가 같아 긴 교재가 벽글처럼 보이는 실패
F. 코드 horizontal scroll과 LESSON horizontal swipe가 경쟁하는 실패
G. V2 → V3 통합 과정에서 핵심 개념이 사라지는 실패
H. Extreme60이 실제 asset이 아니라 synthetic data만 검사하는 실패
I. byte/lesson 배수를 실제 교육 깊이 증가로 오해하는 실패
```

## 2. 실제 수정

### 구조/분량

- `정확히 26` gate 제거. 현재 결과는 26이지만 품질 고정 목표가 아니다.
- 전체 learner lesson 허용 범위 `20..50`.
- TRACK별 `1..6`.
- learner lesson 내부 major BLOCK `1..4`.
- 실제 raw reading estimate `10..60분` gate.
- V3 estimatedMinutes의 upper 60 clamp 제거. 60분을 넘으면 숫자가 그대로 드러나 test가 실패한다.
- V2 reader 평균 대비 V3 depth ratio `12x..60x` 양방향 guard.

### grouping 회귀 방지

- V3 authored BLOCK count와 grouping table이 다르면 즉시 `IllegalArgumentException`.
- V3에서는 atomic silent fallback 금지.
- Legacy/non-V3 synthetic caller만 기존 atomic behavior 유지.

### 내용 누락 회귀 방지

`V3CriticalCoverageTest.kt` 추가.

11 TRACK 핵심 개념 spine과 사용자 지적 4개 영역을 검사한다.

```text
node / next / 중간 삽입
callback / Promise / microtask
MIME / Content-Type / boundary
데이터 무결성 / CHECK / 참조 무결성
```

### reader 복원/가독성

- progress namespace → `v3_deep_beginner_progress`.
- `firstVisibleItemIndex + firstVisibleItemScrollOffset` 저장/복원 연결.
- `snapshotFlow` 위치 기록.
- offset 48px bucket 저장으로 write 과다 감소.
- H3 19sp/Bold/top16dp.
- H4 17sp/SemiBold/top10dp.
- 본문 16sp.
- 코드 horizontalScroll 제거.
- code `softWrap=true`.
- swipe threshold 86dp → 110dp.

### Extreme60 실질화

기존 `TextbookExtreme60Test`는 주로 synthetic markdown을 검사했다.

수정 후 60개 case가 11개 실제 V3 TRACK을 순환하며 직접:

```text
real asset read
→ TextbookMarkdownParser.parse
→ TextbookSectioner.split(track.id)
→ real lesson count
→ real reading time 10..60
→ real weightedLength
→ authored BLOCK preservation
→ major BLOCK 1..4
→ old tiny LESSON label leak 없음
```

을 검사한다.

그리고 기존 synthetic/non-V3 behavior 검증도 같이 유지한다.

## 3. 실제 실행 PASS

### PASS · standalone Sectioner resilience

실제 Kotlin compile/run:

```text
V3_SECTIONER_RESILIENCE_PASS sections=2 minutes=[83, 55]
```

83분 synthetic case가 60분으로 숨지 않고 실제로 노출됨을 확인했다.

### PASS · CLEAN 7 서로 다른 실패 유형

실제 Kotlin compile/run:

```text
V3_EXTREME_CLEAN7_PASS
sections=2
overlong=[83, 55]
estimator=[1, 1, 10, 100]
```

검증한 7종:

```text
1 정상 V3 grouping
2 legacy atomic content preservation
3 old LESSON label 제거
4 authored BLOCK mismatch fail-fast
5 60분 초과 raw time 노출
6 모든 TextbookBlock type weight 양수
7 minute estimator monotonic / upper clamp 없음
```

### PASS · 실제 TextbookBlock model shape compile/run

실제 프로젝트의 `TextbookBlock.Code(language, text)` 등 model shape로 다시 컴파일/실행:

```text
REAL_MODEL_SECTIONER_PASS [84, 56]
```

### PASS · Extreme60 새 test source Kotlin syntax compile

실제 GitHub에 쓴 `TextbookExtreme60Test.kt`와 동일 test source를 로컬에서 minimal dependency stubs와 함께 Kotlin compile:

```text
EXTREME60_KOTLIN_SYNTAX_COMPILE_PASS
```

이 PASS는 Kotlin syntax/type-shape 검증이다. JUnit/Android/Gradle 실제 실행 PASS를 뜻하지 않는다.

## 4. 실행하지 못한 검증

현재 container DNS는 다음 host 모두 해석하지 못했다.

```text
github.com
api.github.com
raw.githubusercontent.com
codeload.github.com
services.gradle.org
repo1.maven.org
```

따라서 아래는 아직 PASS가 아니다.

```text
./gradlew test
실제 11 V3 asset의 JUnit/Gradle 전수 실행
Android instrumented tests
Galaxy Tab 실제/에뮬레이터 swipe + scroll-resume
APK build/install
```

새 actual-asset gate가 문제를 발견하면 숫자를 clamp하거나 test를 약하게 바꾸지 않는다. 해당 learner lesson을 교육적으로 더 나눈다.

## 5. 실행하지 않은 변경

```text
main merge: 안 함
GitHub Actions: 안 돌림
APK release/deploy: 안 함
production 변경: 안 함
유료 실행: 안 함
```

전체 PASS라고 보고하지 않는다. 현재 확정 PASS는 위 standalone Kotlin 검증 범위뿐이다.
