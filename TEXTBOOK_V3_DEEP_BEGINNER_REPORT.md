# TEXTBOOK V3 · Deep Beginner Rewrite Report

기준 브랜치: `main`

작업 브랜치: `textbook-v3-deep-beginner`

목표:

- 기존 `TRACK → BLOCK → LESSON` 학습 구조와 reader UX는 유지한다.
- 서로 직접 연결되는 짧은 LESSON을 하나의 큰 learner-facing LESSON으로 묶는다.
- 정의 몇 줄짜리 단어집을 없애고 완전 초보 기준의 단계별 설명으로 바꾼다.
- 새 learner-facing LESSON 평균 분량은 기존 authored LESSON 평균의 최소 20배를 정적 기준으로 삼는다.
- main 병합, Actions, APK 배포는 사용자 검토/승인 전 실행하지 않는다.

## 1. 실제 원고 교체

V3 source assets 11개를 별도 경로에 추가했다.

```text
app/src/main/assets/textbook/v3/track_01.md
...
app/src/main/assets/textbook/v3/track_11.md
```

기존 V2 source는 삭제하거나 덮어쓰지 않았다.

| TRACK | V2 bytes | V2 authored LESSON | V3 bytes | V3 authored BLOCK | 최종 learner LESSON | 평균 분량 배수(byte 기준) |
|---:|---:|---:|---:|---:|---:|---:|
| 01 | 19,903 | 31 | 43,404 | 5 | 2 | 33.80x |
| 02 | 26,967 | 70 | 46,637 | 8 | 4 | 30.26x |
| 03 | 29,161 | 51 | 42,231 | 7 | 2 | 36.93x |
| 04 | 25,817 | 43 | 50,455 | 6 | 3 | 28.01x |
| 05 | 33,848 | 66 | 59,042 | 8 | 4 | 28.78x |
| 06 | 34,945 | 49 | 52,792 | 7 | 2 | 37.01x |
| 07 | 30,999 | 48 | 40,571 | 6 | 2 | 31.41x |
| 08 | 34,962 | 63 | 48,829 | 8 | 2 | 43.99x |
| 09 | 35,853 | 39 | 34,484 | 3 | 1 | 37.51x |
| 10 | 34,461 | 53 | 32,670 | 4 | 2 | 25.12x |
| 11 | 46,537 | 77 | 40,611 | 5 | 2 | 33.60x |

합계:

```text
기존 authored LESSON: 590
V3 authored BLOCK: 67
최종 learner-facing LESSON: 26
감소율: 95.6%
최저 TRACK 평균 분량 배수: 25.12x
최고 TRACK 평균 분량 배수: 43.99x
```

주의: 위 `분량 배수`는 파일 byte / visible lesson count를 이용한 정적 평균 지표다. 실제 parser weightedLength 기준으로 각 개별 learner LESSON이 20x 이상인지 확인하는 JVM test를 추가했지만 전체 Gradle suite는 아래 환경 제약 때문에 아직 실행하지 못했다. 따라서 이 보고서에서는 그 test를 PASS라고 표시하지 않는다.

## 2. learner-facing LESSON grouping

최종 reader page 수:

```text
TRACK 01: 2
TRACK 02: 4
TRACK 03: 2
TRACK 04: 3
TRACK 05: 4
TRACK 06: 2
TRACK 07: 2
TRACK 08: 2
TRACK 09: 1
TRACK 10: 2
TRACK 11: 2
합계: 26
```

`TextbookSectioner`는 V3에서 adjacent/prerequisite-related BLOCK만 묶는다.

reader 동작:

```text
TRACK
↓
큰 LESSON 1개 선택
↓
LESSON 내부에서 BLOCK이 책의 소단원처럼 이어짐
↓
각 BLOCK 안에서 단계별 H4 소제목
↓
짧은 recall
↓
다음 큰 LESSON
```

기존 source의 `### LESSON 01 · ...` 라벨은 learner reader에서 별도 페이지로 쓰지 않고 H4 subsection으로 내리며 `LESSON 01 ·` 문자열은 제거한다.

## 3. 내용 편집 규칙

각 BLOCK은 가능한 한 다음 흐름을 가진다.

```text
실제 상황/문제
→ 아주 쉬운 뜻
→ 비유/그림
→ 동작 순서
→ 작은 코드
→ 코드 해석
→ 실패/오류 사례
→ 언제 쓰는가
→ 다른 개념과 연결
→ 책을 덮고 확인하는 질문
```

초급 원칙:

- 영어 용어를 먼저 던지지 않는다.
- 아직 설명하지 않은 전문용어로 다른 전문용어를 설명하지 않는다.
- 코드가 나오면 앞의 쉬운 개념과 연결한다.
- `실행됨 = 정답`으로 가르치지 않는다.
- 용어 사전은 본문 설명을 대체하지 않는다.

대표 개선:

- TRACK 03 node/linked list: 상자 연결 → node/value/next → 순회 → 삽입 → 연결 유실 → 배열 비교 → singly/doubly까지 연결.
- TRACK 05 async: 기다림 → blocking/sync/async → callback → Promise → async/await → call stack/event loop/microtask → race/cancel/timeout으로 연결.
- TRACK 06 MIME: 받은 byte의 종류 문제 → MIME → JSON → Content-Type/Accept → form → multipart → boundary → 실제 upload 문제로 연결.
- TRACK 08 integrity: 말이 안 되는 age/order → PRIMARY KEY/UNIQUE/NULL/CHECK → FOREIGN KEY → 참조 무결성 → 관계/JOIN → transaction/ACID로 연결.

## 4. 품질 테스트 교체

옛 gate의 문제:

- TRACK 전체 글자 수만 길면 PASS 가능.
- BLOCK 수가 많아야 PASS.
- H3 `LESSON` 하나를 reader page 하나로 강제.
- 실제 LESSON 교육 밀도를 검사하지 않음.

V3 gate:

- V3 asset 경로 확인.
- authored BLOCK 수를 작은 범위로 제한.
- source truncation byte floor.
- BLOCK별 최소 설명량/step subsection 확인.
- 반복 padding/TODO 차단.
- 최종 learner-facing lesson count = 26 고정.
- authored BLOCK가 merged lesson 내부에 모두 남는지 확인.
- nested old `LESSON 01` 라벨이 learner page에 재등장하지 않는지 확인.
- V2 실제 section 평균 weightedLength 대비 **각 V3 learner-facing LESSON weightedLength ≥ 20x** gate 추가.

수정한 test files:

```text
TextbookClean25Test.kt
TextbookExtreme60Test.kt
TextbookRealAssetSectionTest.kt
V1EditorialQualityTest.kt
V1TextbookCatalogTest.kt
```

## 5. 실제 실행한 검증

### PASS · core Kotlin compile/run

외부 dependency 없이 `TextbookBlock + TextbookSectioner + synthetic V3 blocks`를 Kotlin 1.9.0으로 실제 컴파일하고 실행했다.

실제 결과:

```text
CORE_GROUPING_PASS total=26
```

검증 내용:

- 11 TRACK grouping table이 실제 Kotlin에서 26 learner lessons를 생성.
- grouping count mismatch가 없을 때 planned group 수 사용.
- merged lesson에서 TRACK H1 제거.
- source H2 BLOCK heading을 내부 subchapter로 보존.
- source `LESSON 01 ·` label을 H4 subsection으로 변환.
- synthetic/non-V3 chapter id는 기존 atomic split/content preservation 유지.

## 6. 실행하지 못한 검증

### NOT RUN · full Gradle/JVM/Android suite

현재 작업 container에서 GitHub clone을 실제 시도했으나 DNS가 차단되어 실패했다.

실제 오류:

```text
fatal: unable to access 'https://github.com/irewon1-lgtm/Myapp.git/':
Could not resolve host: github.com
```

따라서 아래 항목을 PASS라고 주장하지 않는다.

```text
./gradlew test
Android instrumented tests
Galaxy Tab emulator/device UI test
APK build/install
GitHub Actions
main merge
production deployment
```

Actions/deploy는 사용자 승인 전 실행하지 않는다.

## 7. branch isolation

확인 시점:

```text
main: cdab71485e48bb16f1e146a774156c5d74ae0077
textbook-v3-deep-beginner: 별도 ahead branch
```

V3 변경은 main에 병합하지 않았다.

사용자가 원고/구조를 먼저 검토한 뒤 다음 단계로 갈 수 있다.
