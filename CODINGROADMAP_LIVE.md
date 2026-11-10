# 코딩로드맵 수정·배포 정본

## 운영 경로
- 저장소: irewon1-lgtm/Myapp
- 브랜치: coding-roadmap-live
- 앱 데이터: live/content.json
- 공통 화면: live/reader.template.html
- 생성 통합본: live/reader.html
- 운영 채널: live/channel.json (CI가 검증한 불변 커밋과 SHA-256을 게시)
- 네이티브 쉘: android-live/com/codingroadmap/live/LiveActivity.java

## Track 3 연속 교재
- 원고 정본: live/track3-book/ch01.md ~ ch09.md
- 교재 화면: live/track3-book/reader.js, reader.css
- 변환: tools/compile_track3_book.py (tools/build_content.py 진입점)
- 설명 문단 → 실제 예제 → 연습문제 → 풀이 → 다음 개념이 장 전체에서 연속된다.
- 571개 쪽지형 페이지 고정, 첫 코드만 추출, 표의 평문 변환은 폐기했다.
- 장의 HTML과 표·코드·문단 순서를 보존한다. 화면 높이에 따른 물리적 페이지 수만 달라진다.
- 이전 원고는 archive/track3-fragmented-manuscript-20260924.md에 보관하며 제작 입력으로 쓰지 않는다.
- 기존 메모·북마크는 legacyTrack3에 원문을 보존하고 새 본문에 임의로 재귀속하지 않는다.
- 개정판 기록은 문단/코드 줄 앵커에 저장한다. 기존 다른 트랙 기록을 삭제하지 않는다.
- 장별 5종은 두 공개 교재와 세 대학 강좌 자료의 관련 절이다. 45권 완독을 주장하지 않는다.
- Think Python 3판은 2024년 판이며 2025년 이후 출간 도서로 표시하지 않는다.

## 검증과 게시
1. 원고와 화면을 최신 정본에서 수정한다. 다른 트랙을 재생성하지 않는다.
2. python3 tools/build_content.py
3. node tools/verify_reader.cjs
4. python3 tools/test_track3_book.py
5. python3 tools/test_track3_browser.py
6. python3 tools/test_track3_worker.py
7. Publish CodingRoadmap lessons and screen의 성공과 channel.json의 커밋·해시를 확인한다.

브라우저 검사는 실제 Chromium에 native bridge를 모사한 검사다. 실제 Galaxy 설치·자동업데이트 수신 검증으로 과장하지 않는다. Python worker 검사는 공개 CDN에서 실제 엔진을 로드한다. 네트워크 로드 실패·중지·실행 오류를 정상 출력으로 위장하지 않는다.

일반 교재·WebView 화면 변경에는 APK 재설치가 필요 없다. 검증된 새 통합본을 받은 다음 진입 또는 설정의 받은 변경 적용에서 적용한다. 오프라인에서는 기존 저장본을 사용한다. 네이티브 쉘의 HTML 상한은 3 MiB이므로 배포 전에 확인한다.

## Android 본체와 서명
Application ID는 com.codingroadmap.app이다. 네이티브 권한·WebView 외부 동작 수정은 별도 서명 APK 갱신이 필요하다. 개인키를 만들거나 공개 저장소에 올리지 않는다. 기존 인증서 SHA-256은 3a51cc8b57302cb4b64a941f079352ed2c312c1a987696ee22495a0491a2ae5a 이다. 기존 classes.dex/classes2.dex 및 이전 내장 교재는 이 콘텐츠 개편에서 변경하지 않는다.

## Track 4 연속 교재
- 원고 정본: live/track4-book/ch01.md ~ ch12.md
- 주제: 웹의 기본 — 화면·동작·데이터 연결하기
- 변환: tools/compile_track4_book.py; build_content.py가 Track 3 이후 호출한다.
- 참조: 장별 주된 자료 5종, 총 60건의 연결이다. 60권 완독을 주장하지 않는다. 2024년 책은 보조 자료이다.
- 표·문단·코드·연습·해설을 원고 순서대로 보존하고 장마다 하나의 연속 본문을 제공한다.
- 기존 Track 1~3 원문과 기록 ID를 보존한다. Track 4의 문제 ID에는 t4 접두사를 둔다.
- reader.js의 책 페이지 계산은 현재 트랙을 기준으로 한다. Track 4 코드는 Python 실행기로 보내지 않는다.
- 짧은 JavaScript는 격리된 Worker, 완전한 HTML 예제는 출처가 분리된 sandbox iframe에서 실행한다. 네트워크·저장소·다운로드가 필요한 전체 프로젝트와 혼동하지 않는다.
- 전체 프로젝트: live/track4-book/project/index.html, style.css, app.js, topics.json.
- 테스트: tools/test_track4_book.py, tools/test_track4_browser.py. 기존 Track 3 검사도 유지한다.
- 전송본에서는 book.html/text의 중복 집계 문자열만 제거하고 원본 블록으로 복원한다. 내용을 요약하거나 삭제하지 않는다. 3 MiB 네이티브 다운로드 상한을 검사한다.

## Track 12 바이브 코딩 필수 용어집 (추가형)
- 사용자가 승인한 **전체 용어 200개** 가운데 **1~50번(5단원 × 10개)**만 우선 공개한다.
- 기존 Track 5는 TypeScript 예약 트랙이다. Track 1~11의 ID·제목·기존 기록을 변경하지 않고 신규 Track 12로 추가한다.
- 원고 정본: `live/vibe-terms/terms-001-005.json`부터 `terms-046-050.json`까지 총 10개 파일.
- 한 용어의 다섯 요소: ① 쉬운 뜻 ② 실제 앱 개발 예시 ③ 헷갈리는 용어와 비교 ④ AI 오판 판별 문제+정답 해설 ⑤ 복사 가능한 Codex 작업 지시문.
- 컴파일: `python tools/build_content.py`에서 기존 Track 3 → 신규 Track 12 → Track 4 순으로 실행. Track 4 빌더가 합성본 `live/reader.html`을 생성한다.
- 격리된 새 트랙 표시: `live/vibe-terms/reader.js`, `reader.css`. 기존 독서 화면·북마크·메모·완료 기록 구조를 재사용한다.
- 테스트: `python tools/test_track12_glossary.py` 및 `python tools/test_track12_browser.py`. 기존 3·4 트랙 검증과 전체 뷰어 검증은 유지한다.
- 배포: 기존 `Publish CodingRoadmap lessons and screen` 성공 + `live/channel.json` 최신 커밋/해시 확인이 필요하다. 네이티브 셸은 미변경이므로 일반 교재 업데이트는 기존 수신 절차를 따른다.
- 51~200번 원고는 아직 미구현이다. 1~50번 게시를 200개 전체 완료로 표기하지 않는다.
- 실제 Galaxy 기기 설치·수신 검증을 실행하지 않았다면 `NOT_RUN`으로 보고한다.
- Track 12 화면 디자인 승인: **책형 목업 4번 + 다크모드**. 전용 `vibe-book-shell`을 사용하여 기존 Track 1~4 UI에는 영향이 없다.
- **용어 1개 = 논리적 책 페이지 1개**. 설명이 길면 같은 페이지 안에서 세로로 스크롤하며 읽는다. 종전의 자동 가로 분할(용어당 2~3장)은 Track 12에서 사용하지 않는다.
- 1~5번 내용은 모든 항목을 본문에서 그대로 표시한다. 항목 간 여백을 줄이고, 각 항목을 둘러싸는 카드·테두리를 없앤다. Codex 지시문만 인용문처럼 약하게 음영 표시한다.
- 하단 이전/다음 버튼으로 1→50번 용어를 1개씩 넘긴다. 장 경계(10→11 등)를 넘길 때에도 용어가 생략되지 않아야 한다. 50번 뒤에는 미공개 51번으로 넘어가지 않는다.
- 기존 북마크·용어별 메모·읽던 위치는 같은 ID를 사용한다. 추가 `vibeBookScroll` 필드가 같은 용어 내 세로 스크롤 위치를 보존한다. 글자 크기 `Aa` 조정은 이 트랙에서만 작용한다.
- 필수 검수: 360×800 휴대폰과 800×1280 태블릿 실제 Chromium에서 **50개 전수**의 5개 본문·하단 내비게이션·가독성·복사·북마크·메모·세로 스크롤을 확인한다. 실제 Android 수신 검증을 실행하지 않았으면 NOT_RUN.


## Track 12 좌우 터치 넘김 복구 (2026-11-10)
- 사용자 지적: 4번 다크 책형 적용 시 기존 슬라이드 넘김(좌우 손가락 스와이프)이 사라진 오류.
- Track 12 전용 터치·마우스 드래그 탐지로 오른쪽에서 왼쪽 밀기 → 다음 용어, 왼쪽에서 오른쪽 밀기 → 이전 용어를 복원한다.
- 위아래 터치는 동일한 용어의 본문만 세로 스크롤하고 넘김하지 않는다. 짧은 드래그·대각 이동·복사 버튼 누름으로는 용어를 변경하지 않는다.
- 손가락으로 넘길 때 책장 이동 애니메이션을 적용하고, 상단·하단 버튼과 북마크·메모·개별 용어 스크롤 기록을 보존한다.
- 연속된 50개 전체 용어의 실제 Chromium CDP 터치 이벤트로 1→50 및 50→1 전수를 자동 검증한다. 이 CI는 안드로이드 실제 기기 시험이 아니다.

## Track 12 좌우 화면 끝 탭 복원 (2026-11-10)
- 사용자가 지적한 누락: 다크 책형 화면에서 **왼쪽·오른쪽 끝 탭**이 이전·다음 용어로 넘어가지 않음.
- 본문 화면의 좌측 12% (최소 38px, 최대 56px) 탭은 이전 용어, 우측 동등한 영역 탭은 다음 용어로 이동한다. 별도의 불투명 버튼이나 영역을 덮지 않고 이벤트 위임으로 처리한다.
- 책형 다크모드, 용어당 하나의 세로 스크롤 페이지, 좌우 스와이프, 하단 이전/다음 버튼은 그대로 유지한다. 중앙 탭, 버튼·링크, 세로 드래그, 짧은 터치나 대각 스크롤은 잘못 넘김하지 않는다. 제스처 후 합성 클릭으로 두 장 넘어가는 현상도 막는다.
- 실제 Chromium의 CDP 터치 이벤트로 우측 끝 1→50(49회), 좌측 끝 50→1(49회), 경계 1·50·본문 중앙·스크롤 위치 변화·마우스 탭을 검증한다. Android 물리기기 시험은 별도 미실행(NOT_RUN).
