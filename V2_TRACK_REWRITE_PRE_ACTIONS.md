# V2 코딩 완전과정 — Actions 실행 전 상태

기준 브랜치: `rewrite/v2-track-bookscale-prep`

이 문서는 **실행 완료 보고서가 아니다.** Actions/Android build/emulator를 돌리기 전에 무료·비파괴 범위에서 준비한 변경과 아직 실행 검증되지 않은 경계를 기록한다.

## 실제 반영된 구조

- 사용자 메인 과정: 11 TRACK
- 계층: TRACK → BLOCK → LESSON → 저부담 회상 → TRACK 실전
- V2 본문: `app/src/main/assets/textbook/v2/track_01.md` ~ `track_11.md`
- 본문 파일 11개 존재 확인
- GitHub contents metadata 기준 V2 본문 파일 크기 합계: 353,453 bytes
- 최소 파일: 19,903 bytes
- 최대 파일: 46,537 bytes
- 학습 화면: dark reader, TRACK/LESSON 표기, 이전/다음 LESSON, 좌우 swipe 유지
- 구형 9권/134장 목차는 주 학습 화면에서 제거
- 구형 90 micro-lesson 진입 버튼은 V2 메인 목차에서 제거
- V2 읽기 진행 저장소는 `v2_track_progress`로 분리; 기존 V1 preference는 삭제하지 않음

## 실제 반영된 11 TRACK

1. 컴퓨터와 프로그래밍의 언어
2. 프로그래밍 사고와 문법
3. 자료구조와 알고리즘
4. 웹 화면과 브라우저
5. JavaScript와 TypeScript 깊게
6. 인터넷·네트워크·API
7. 서버와 백엔드
8. 데이터베이스
9. 보안과 데이터 보호
10. 오류·테스트·Git·빌드·배포
11. 소프트웨어 설계와 종합 프로젝트

## 전문용어 선행개념 순서 보강

- 비동기: 기다림 → blocking → synchronous → asynchronous → callback → Promise → async/await → call stack/task/event loop → microtask → race condition
- HTTP 데이터 형식: 데이터 종류 필요성 → MIME → JSON → Content-Type → Accept → file upload → multipart/form-data → boundary
- DB 무결성/transaction: ID → PRIMARY KEY/UNIQUE/NULL/NOT NULL/CHECK → 관계 → FOREIGN KEY → referential integrity → data integrity → transaction → Atomicity → Consistency → Isolation → Durability
- 보안: asset/threat/trust boundary → authentication/authorization → encoding → hash → password hashing/salt → encryption → key/signature/certificate/TLS → injection/XSS/CSRF/SSRF/file upload/secrets

## V2 실습 연결

- `V2-T01` ~ `V2-T11` 전용 practice lesson 추가
- 지원 런타임: Python / HTML+JS / TypeScript / SQL
- 기존 `TB1-Cxx` 문제는 V2 TRACK의 현재 practice 연결에서 제거
- 각 LESSON 끝 inline activity는 현재 읽은 LESSON 제목에 근거한 저부담 회상으로 변경
- 실제 실행·hidden tests·debugging·AI answer audit는 기존 FocusedPractice 실행 엔진을 재사용

## V2 검증 코드 준비

- `V1TextbookCatalogTest` → V2 TRACK/practice mapping 기준
- `TextbookLearningFlowTest` → 현재 LESSON 기반 recall 확인
- `TextbookClean25Test` → 25개 V2 구조 gate
- `TextbookExtreme60Test` → 60개 V2 구조 scenario
- `V1EditorialQualityTest` → BLOCK/LESSON, anti-padding, prerequisite ordering 검사
- `TextbookRealAssetSectionTest` → authored H3 LESSON 1개당 reader page 1개 확인
- `V2TrackPracticeDataTest` → 11 practice ID/runtime/broken-vs-fix/AI answer metadata 검사
- Android UI instrumentation 2개 → 11 TRACK/LESSON UI 기준으로 수정
- `tools/textbook_quality_report.py` → V2 asset/structure/prerequisite/UI/practice gate로 교체
- `tools/tablet_capture_smoke.sh` → V2 tablet evidence tag로 정렬

## 현재 실제 확인된 것

- 준비 브랜치가 존재하고 최신 변경이 GitHub에 commit되어 있음
- 준비 브랜치의 GitHub Actions run 수: 0
- V2 asset 11개가 GitHub에 존재함
- V2 asset 파일 크기 metadata 확인
- 대표 TRACK→BLOCK→LESSON section boundary 로직을 별도 시뮬레이션해 첫 소개가 독립 빈 LESSON으로 떨어지지 않는 흐름 확인

## 아직 PASS/완료라고 부르면 안 되는 것

아래는 아직 실제 Actions/Android 실행 전이므로 PASS가 아니다.

- Kotlin 전체 compile
- `testDebugUnitTest` 전체
- Extreme60 실제 60/60
- CLEAN25 실제 25/25
- 새 V2 editorial/runtime quality report 최종 결과
- Debug APK build
- Release APK build
- APK signing/verification
- Android API 34 connected tests
- Galaxy Tab 1280dp simulation screenshot
- release APK install/launch smoke
- production/main merge
- production release/deployment

## 승인 후 실행할 단 한 번의 검증

기존 `.github/workflows/verify-textbook-v1.yml`을 이 준비 브랜치에서 수동 실행하면 다음을 한 번에 수행하도록 준비되어 있다.

1. 전체 JVM unit tests
2. lecture/feedback/review regression
3. V2 textbook Extreme60 + CLEAN25 + static quality report
4. Debug APK build
5. Release APK build
6. test signing + signature/hash verification
7. Android API 34 connected instrumentation
8. Galaxy Tab-class 1280dp reader simulation + screenshot evidence
9. signed Release APK install/launch smoke
10. evidence artifact upload

준비 브랜치는 `main`이 아니므로 production publish 조건과 분리한다. 실제 production merge/release는 별도 승인 없이는 실행하지 않는다.
