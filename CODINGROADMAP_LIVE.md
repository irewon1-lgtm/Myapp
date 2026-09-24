# 코딩 로드맵 — 수정·배포 정본

사용자의 요구: 완전 초보자가 용어의 뜻·사용법·예시부터 배우고, 쉬운 이론과 직접 실행할 코드를 연결한다. 실습·확인문제 바로 다음 장에 정답·이유·풀이를 둔다. 기억할 것과 직접 해볼 것을 나눈다. 갤럭시 폰·태블릿의 다크 모드, 읽던 위치, 좌우 이동, 메모·북마크를 유지한다. 본문과 UI 수정 때 APK 재설치를 반복하지 않는다.

## 반드시 사용할 경로

- Repository: `irewon1-lgtm/Myapp`
- Branch: `coding-roadmap-live`
- Course: `live/content.json`
- Track 3 authored source: `live/track3.manuscript.md`
- Screen: `live/reader.template.html`
- Generated self-contained reader: `live/reader.html`
- Channel: `live/channel.json`
- Native shell: `android-live/com/codingroadmap/live/LiveActivity.java`
- Stable endpoint: `https://raw.githubusercontent.com/irewon1-lgtm/Myapp/coding-roadmap-live/live/channel.json`

다른 채팅방에서도 위 정본을 최신으로 읽고 수정한다. 채팅 답변·다른 브랜치·로컬 파일만 고쳐서는 사용자 앱에 반영되지 않는다.

## UI / 리더 기준 — CLEAN REBUILD 2026-09-20

- UI는 사용자가 제공한 reference screens의 구조를 기준으로 한다: 아이보리 서재형 홈/트랙/북마크 + 짙은 브라운 계열 독서 뷰어.
- 밀리의서재의 서재 중심 정보구조와 독서 뷰어 사용감을 참고하되, 외부 브랜드 로고·전용 이미지·저작물을 복제하지 않고 코딩 로드맵 고유 텍스트·CSS 자산을 사용한다.
- `live/reader.template.html`은 CLEAN REBUILD 단일 스타일 레이어를 유지한다. 과거 `PREMIUM_MINIMAL_V3`, `UNIFORM_READER_GRID_V6`, `NO_GAP_FULL_PAGE_V7` 등 누적 override 레이어를 되살리지 않는다.
- 홈: 이어 학습 카드, 학습 트랙 책표지 카드, 하단 4탭(홈/서재/북마크/설정).
- 트랙: 책 표지형 소개 + 실제 진행률 + 실제 챕터 목록. 존재하지 않는 영상 수/학습시간 같은 가짜 메타데이터를 만들지 않는다.
- 북마크: 북마크/메모를 실제 state에서만 렌더한다.
- 읽기: dark reader, serif 중심 제목/본문, gold accent, 상단 목차·북마크·메모·Aa, 하단 이전/다음·진도·집중모드.
- Android 홈/제스처 바에 UI가 가리지 않도록 native WebView inset과 CSS `--navSafe`를 함께 사용한다.
- 읽기 화면 양끝 투명 hit zone과 좌우 swipe로 이전/다음 화면을 이동한다.

## 읽기 페이지 계약 — 고정 typography + 자연 분할

- 본문 기본 크기는 18px, line-height는 1.68로 고정한다. 특정 페이지 때문에 글자 크기나 line-height를 확대/축소하지 않는다.
- page별 `transform:scale`, scale-to-fit, compact mode, sparse-page stretching을 금지한다.
- 교재의 논리 page.id는 메모/북마크/검색/복원 identity로 보존하되, 화면에서는 현재 챕터 내용을 연속 흐름으로 합쳐 실제 viewport 높이에 맞는 horizontal physical pages로 다시 분할한다.
- physical pages는 동일 폭·동일 높이를 사용하고 CSS multi-column `column-fill: balance`로 분량을 배분한다. 문단에는 `orphans:3 / widows:3`을 적용한다.
- 사용자가 요구한 연속 읽기 밀도를 위해 paragraph-to-paragraph 추가 빈 줄을 만들지 않는다. 다만 제목·코드·설명 박스 자체의 최소 내부 padding은 가독성을 위해 고정값으로 유지하며 page별로 달라지지 않는다.
- 내용이 많으면 다음 physical page로 자연스럽게 이어진다. 내용 자체를 삭제·요약·복제하거나 글자를 작게 만들어 끼워 넣지 않는다.
- 세로 스크롤은 reader에서 사용하지 않는다. physical page 이동은 좌우 swipe 또는 양끝 tap으로 수행한다.
- 화면 회전 후 같은 typography contract로 다시 pagination한다.
- `page.id`, 교재 본문, 메모·북마크·완료·읽던 위치, 자동 업데이트 계약은 보존한다.

## 렌더 QA 계약

- 정적/DOM stub 검증만으로 완료를 주장하지 않는다.
- publish workflow에서 self-contained reader 생성 후 실제 Chromium으로 Galaxy Tab S10 FE에 가까운 16:10 landscape/portrait 렌더 smoke test를 수행한다.
- 렌더 QA는 Korean font가 있는 환경에서 reader의 18px typography, vertical overflow hidden, physical pagination 완료, footer visible 여부를 확인한다.
- landscape/portrait reader screenshot과 landscape home screenshot을 workflow artifact로 남긴다.
- 실제 Galaxy Tab S10 FE 물리기기에서의 터치감·One UI 조합은 별도 실기기 검증 전까지 PARTIAL이다.

## 보통의 교재·화면 수정

1. 정본 브랜치 최신 상태를 읽는다. 일반 교재는 content.json, Track 3 본문은 `live/track3.manuscript.md`, 화면은 template을 정본으로 수정한다.
2. 기존 page.id를 바꾸지 않는다. 추가 페이지는 새로운 고유 ID를 부여한다. 기존 메모·북마크는 이 ID에 연결된다.
3. `python3 tools/build_content.py`, `node tools/verify_reader.cjs`로 기본 구조와 학습 기록 로직을 확인한다. 이 검사는 렌더링/실기기 검사가 아니다.
4. 정본 브랜치에 커밋한다. `Publish CodingRoadmap lessons and screen` workflow가 교재·화면 통합본을 생성하고 커밋 고정 주소 + SHA-256을 채널에 게시한다.
5. workflow 성공과 channel.json의 커밋/해시를 확인한 뒤 배포 완료라고 보고한다. 실패 시 기존 채널을 유지하며 완료를 주장하지 않는다.

일반 콘텐츠 변경은 APK가 필요 없다. 검증된 새 교재를 받은 뒤 앱을 다시 열면 반영된다. 읽는 도중 변경이 도착하면 현재 페이지를 강제로 바꾸지 않고 다음 진입 또는 설정의 `받은 변경 적용`에서 적용한다. 네트워크가 없으면 마지막 저장본, 없으면 APK 기본 교재를 쓴다.

## Android 본체 변경

네이티브 권한·기기 연동·WebView 바깥 기능은 APK 업데이트가 필요하다. 모든 기능을 영원히 APK 없이 바꿀 수 있다는 약속은 하지 않는다.

- Application ID: `com.codingroadmap.app`
- New native activity: `com.codingroadmap.live.LiveActivity`
- Native release: 0.9.0, versionCode 100
- Source baseline filename: CodingRoadmapFresh_v0.8.8_BOOKSHELF_ALWAYS_ON.apk
- Baseline APK SHA256: `92a673a4c99d752a32f200d6774251d865fe4ee7239496eb04f316efd22f6f24`
- Baseline installed manifest: versionName 0.8.4 / versionCode 15
- Required signer certificate SHA256: `3a51cc8b57302cb4b64a941f079352ed2c312c1a987696ee22495a0491a2ae5a`

기존 서명 보관본은 사용자의 비공개 파일 `CodingRoadmapFresh_v0.8.5_SIGNING_KEY_KEEP.zip`이다. 절대로 개인키를 저장소에 올리지 않는다. 새 키를 만들어 업데이트 충돌을 반복하지 않는다.

`Compile CodingRoadmap live shell` workflow는 Android Java를 컴파일하고 classes3.dex를 제공한다. `tools/patch_apk.py BASE_APK CLASSES3_DEX READER_HTML OUTPUT_UNSIGNED_APK`는 원래 APK를 보존하면서 새 Activity·인터넷 권한·버전을 추가한다. zipalign 후 기존 키로 서명하고, 앱 ID·versionCode·서명·ZIP 무결성을 확인한다. 이후 네이티브 릴리스 때는 baseline/versionCode 가정을 업데이트하고 현재 설치본보다 높은 값을 사용한다.

## 데이터·기존 화면 보존

기존 classes.dex, classes2.dex, 16개 본문 파일은 수정하지 않는다. 이전 책장과 화면 꺼짐 방지 기능도 남긴다. 새로운 학습 기록은 별도 SharedPreferences에 보관한다. 이전 AndroidX DataStore는 읽기만 한다. 이전 앱 메모/북마크는 트랙 ID가 없어 자동으로 특정 트랙에 귀속시키지 않고 이전 기록으로 보존한다. 원래 화면은 설정에서 열 수 있으며 그 화면은 내장본이라 원격 수정이 반영되지 않는다.

## 현재 검증 범위와 남은 과제

기준 APK 정적 분석, Java 빌드, APK 서명·버전, 교재 구조, Python 풀이 실행, JavaScript 상태 로직을 검증한다. 실제 Galaxy 기기 설치/업데이트, 이전 기록 마이그레이션, 화면 크기별 렌더링, 네트워크에서 실제 채널 갱신은 실기기 검증 전까지 미검증으로 남긴다.

트랙 1·2 전체와 트랙 3의 9개 챕터를 제공한다. 트랙 4~11은 준비 중이다. 실습 화면은 Pyodide 기반 실제 Python 실행을 지원하며, 교재의 검증 결과와 기기 전체 동작 범위는 구분해서 표시한다. 전체 커리큘럼 완성을 주장하지 않는다.
