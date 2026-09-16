# TEXTBOOK V3 · 완전초급 심화 교재 품질 계약

## 1. 목적

기존 V2의 가장 큰 실패는 `TRACK → BLOCK → 짧은 LESSON 수백 개` 구조가 실제 학습에서는 단어장처럼 보였다는 점이다.

V3의 목적은 LESSON 개수를 특정 숫자로 맞추는 것이 아니다.

```text
짧은 정의 조각 제거
+
관련 내용을 책의 한 절처럼 연결
+
완전 초보가 상황 → 원리 → 예제 → 코드 → 실수 → 연결 순서로 읽게 함
+
긴 글이 과적재되지 않게 상한을 둠
+
앱을 닫았다 다시 열어도 읽던 위치로 복귀
```

기존 V2 `### LESSON` 총수는 590개다.

현재 V3 원고/묶음에서 learner-facing LESSON은 26개다. 이 숫자는 **현재 구현 결과**이지 품질을 위해 영구 고정하는 목표가 아니다.

앞으로 교육적으로 필요하면 27개, 30개, 35개로 바뀔 수 있다. 대신 아래 품질 상·하한을 지켜야 한다.

## 2. 현재 TRACK 구조

| TRACK | V2 authored LESSON | V3 source BLOCK | 현재 learner LESSON |
|---|---:|---:|---:|
| 01 컴퓨터와 프로그래밍의 언어 | 31 | 5 | 2 |
| 02 프로그래밍 사고와 문법 | 70 | 8 | 4 |
| 03 자료구조와 알고리즘 | 51 | 7 | 2 |
| 04 웹 화면과 브라우저 | 43 | 6 | 3 |
| 05 JavaScript와 TypeScript | 66 | 8 | 4 |
| 06 인터넷·네트워크·API | 49 | 7 | 2 |
| 07 서버와 백엔드 | 48 | 6 | 2 |
| 08 데이터베이스 | 63 | 8 | 2 |
| 09 보안과 데이터 보호 | 39 | 3 | 1 |
| 10 오류·테스트·Git·빌드·배포 | 53 | 4 | 2 |
| 11 소프트웨어 설계와 종합 프로젝트 | 77 | 5 | 2 |
| **합계** | **590** | **67** | **26 현재값** |

현재 26개는 590개 대비 95.6% 감소다. 감소율 자체는 참고 지표다.

## 3. learner-facing 구조

```text
TRACK
↓
큰 LESSON 선택 / 좌우 스와이프
↓
LESSON 안에서 관련 BLOCK이 책의 소단원처럼 이어짐
↓
BLOCK 내부 H4 단계 소제목
↓
본문 / 그림 / 코드 / 실패 사례
↓
짧은 회상
↓
다음 LESSON
```

V3에서는 source의 `### LESSON 01 · ...`가 learner-facing 새 페이지가 되지 않는다.

- H2 BLOCK → reader 내부 H3
- H3 legacy LESSON → reader 내부 H4
- TRACK H1 → 중복 제거

## 4. 편집 계약

각 BLOCK은 가능한 한 다음 흐름을 가진다.

1. **실제 상황/문제** — 왜 필요한지 먼저 본다.
2. **아주 쉬운 뜻** — 영어 전문용어를 먼저 던지지 않는다.
3. **비유/그림** — 상태나 데이터 흐름을 눈으로 본다.
4. **동작 순서** — 한 단계씩 따라간다.
5. **작은 코드** — 최소 예제로 시작한다.
6. **코드 해석** — 아직 배우지 않은 문법을 건너뛰지 않는다.
7. **실패/오류 사례** — 틀린 결과가 왜 생기는지 본다.
8. **언제 쓰는가** — 실제 앱 위치와 연결한다.
9. **다른 개념과 연결** — 다음 개념이 왜 필요한지 이어 준다.
10. **책을 덮고 확인** — 자기 말로 회상한다.

금지:

- 정의 한두 줄만 있는 용어 카드
- 전문용어로 다른 전문용어를 설명하는 순환 설명
- 실행됨 = 정답으로 가르치기
- 반복 문장으로 길이만 늘리기
- TODO/TBD/준비중

## 5. 분량 품질 계약

### 폐기한 기준

아래는 참고 통계로만 남기고 PASS 근거로 쓰지 않는다.

```text
V3 파일 byte / visible lesson count
-----------------------------------
V2 파일 byte / authored lesson count
```

한국어 UTF-8 byte 수와 LESSON 합치기 효과가 크게 섞이므로 `25x~44x` 같은 숫자를 실제 교육 깊이 증가로 해석하지 않는다.

### 실제 gate

실제 `TextbookMarkdownParser + TextbookSectioner` 결과를 기준으로 검사한다.

- 전체 learner-facing LESSON 수: 현재 구조에서 20~50 범위
- TRACK별 learner-facing LESSON: 1~6
- 한 learner-facing LESSON의 내부 major BLOCK: 1~4
- 실제 추정 읽기시간: 10~60분
- V2 reader page 평균 weightedLength 대비 각 V3 LESSON depth ratio: 12x~60x
- estimatedMinutes는 60분에서 강제로 잘라 숨기지 않는다. 60분 초과면 실제 숫자가 노출되어 test가 실패해야 한다.

현재 26개를 유지하려고 70~100분짜리 LESSON을 만드는 것은 금지한다.

## 6. grouping 안전 계약

V3 grouping table은 best-effort 힌트가 아니라 **강제 계약**이다.

원고 BLOCK 수가 바뀌었는데 grouping table을 갱신하지 않으면:

```text
예전 atomic split으로 조용히 fallback
```

하지 않는다.

반드시 즉시 실패한다.

```text
authored BLOCK count mismatch
→ IllegalArgumentException
→ grouping 계약 수정 필요
```

이 규칙은 V3가 나중에 다시 수백 개의 짧은 페이지로 퇴행하는 것을 막는다.

Legacy/non-V3 synthetic caller만 기존 atomic split을 유지한다.

## 7. 내용 coverage 계약

V3 통합 과정에서 핵심 개념이 사라지는 것을 막기 위해 `V3CriticalCoverageTest`를 둔다.

대표 spine:

- TRACK 03: array / node / stack / hash / tree / Big-O
- TRACK 05: closure / Promise / async-await / event loop / TypeScript
- TRACK 06: DNS / TCP / TLS / Content-Type / multipart / CORS
- TRACK 08: PRIMARY KEY / FOREIGN KEY / JOIN / index / transaction / ACID
- TRACK 09: threat / authentication / hash / encryption / SQL injection / XSS / CSRF / secret
- TRACK 10: reproduction / unit-integration / Git / CI / artifact / rollback
- TRACK 11: requirement / acceptance criteria / cohesion / coupling / DI / cache / circuit breaker / observability

사용자가 직접 문제 삼았던 아래 4개는 별도 회귀 gate를 둔다.

```text
node + next + 중간 삽입
callback + Promise + microtask
MIME + Content-Type + boundary
데이터 무결성 + CHECK + 참조 무결성
```

## 8. 긴 글 reader UX 계약

V3는 LESSON이 길기 때문에 `어느 LESSON인가`만 기억해서는 부족하다.

반드시 다음을 저장한다.

```text
TRACK
LESSON index
세로 firstVisibleItemIndex
세로 firstVisibleItemScrollOffset
```

V3는 별도 preference namespace를 사용한다.

```text
v3_deep_beginner_progress
```

이전 V1/V2 읽기 완료/위치가 V3 완료로 잘못 이어지면 안 된다.

세로 위치는 `snapshotFlow`로 기록하고 재진입 시 `rememberLazyListState` 초기 위치에 복원한다.

## 9. reader 가독성 / swipe 계약

- 검은 계열 배경 + 밝은 본문 유지
- 본문 최대 폭 780dp
- H3 major BLOCK은 본문보다 명확히 큼
- H4 단계 제목도 본문 16sp보다 크게 표시
- BLOCK/H4 위 여백을 늘려 긴 페이지에서 위치를 눈으로 찾을 수 있게 함
- LESSON 좌우 swipe 유지
- 코드 블록은 좁은 폰에서 `softWrap=true`
- 코드 내부 horizontalScroll은 제거해 LESSON swipe와 gesture 경쟁하지 않게 함
- swipe threshold는 110dp로 올려 작은 가로 움직임 오작동을 줄임

## 10. 검증 단계

### 무료·비파괴 로컬 검증

핵심 `TextbookSectioner`는 외부 dependency 없이 Kotlin compiler로 실제 컴파일/실행한다.

CLEAN 유형은 서로 달라야 한다.

1. 정상 grouping 수
2. legacy atomic content preservation
3. old LESSON label 제거
4. V3 BLOCK mismatch fail-fast
5. 60분 초과 시간 은폐 금지
6. block weight 양수
7. minute estimator monotonic

### 전체 저장소 검증

아래는 실제 실행 전 PASS라고 하지 않는다.

```text
./gradlew test
Android instrumented tests
Galaxy Tab 실제/에뮬레이터 UI 검증
APK build/install
```

## 11. 승인 경계

허용된 범위:

```text
textbook-v3-deep-beginner 브랜치 수정
무료·비파괴 정적검증
로컬 Kotlin standalone compile/run
```

별도 승인 없이 실행하지 않음:

```text
main merge
GitHub Actions
APK release/deploy
운영환경 변경
유료 실행
```
