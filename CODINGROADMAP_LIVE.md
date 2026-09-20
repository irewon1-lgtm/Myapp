# 코딩 로드맵 — 수정·배포 정본

사용자의 요구: 완전 초보자가 용어의 뜻·사용법·예시부터 배우고, 쉬운 이론과 직접 실행할 코드를 연결한다. 실습·확인문제 바로 다음 장에 정답·이유·풀이를 둔다. 기억할 것과 직접 해볼 것을 나눈다. 갤럭시 폰·태블릿의 다크 모드, 읽던 위치, 좌우 이동, 메모·북마크를 유지한다. 본문과 UI 수정 때 APK 재설치를 반복하지 않는다.

## 반드시 사용할 경로

- Repository: `irewon1-lgtm/Myapp`
- Branch: `coding-roadmap-live`
- Course: `live/content.json`
- Screen: `live/reader.template.html`
- Generated self-contained reader: `live/reader.html`
- Channel: `live/channel.json`
- Native shell: `android-live/com/codingroadmap/live/LiveActivity.java`
- Stable endpoint: `https://raw.githubusercontent.com/irewon1-lgtm/Myapp/coding-roadmap-live/live/channel.json`

다른 채팅방에서도 위 정본을 최신으로 읽고 수정한다. 채팅 답변·다른 브랜치·로컬 파일만 고쳐서는 사용자 앱에 반영되지 않는다.

## UI 기준 — 2026-09-20 reference screens

- 홈/서재/북마크/트랙 목록은 사용자가 제공한 reference screen처럼 따뜻한 아이보리·베이지 계열의 서재형 UI를 사용한다.
- 읽기 화면은 별도 다크 리더 UI를 사용하며, 큰 명조계열 제목 + 차분한 본문 + 금빛 포인트를 유지한다.
- 홈에는 이어 학습 카드, 학습 트랙 책표지형 카드, 하단 4개 탭(홈/서재/북마크/설정)을 유지한다.
- 트랙 화면은 책 표지형 소개 + 진행률 + 목차 리스트 구조를 사용한다.
- 북마크 화면은 큰 제목 + 필터 칩 + 카드형 저장 항목 구조를 사용한다.
- Android 제스처/홈 내비게이션 바에 하단 글자가 가리지 않도록 fixed bottom navigation과 reader footer에 최소 28~30dp 상당의 bottom safe padding을 둔다.
- 읽기 화면 좌/우 끝에는 투명 hit zone을 두어 탭만으로도 이전/다음 페이지 이동이 가능해야 한다.
- 기존 좌우 swipe, 한 화면 1페이지, 자동 fit, page.id, 메모·북마크·읽던 위치, 자동 업데이트 계약은 유지한다.

## 읽기 화면 고정 계약 — Galaxy Tab S10 FE

- 읽기 본문은 모든 페이지에서 동일한 typography grid를 사용한다. 본문 기본 크기 18px, line-height 1.68, 문단 간격 14px을 페이지별로 바꾸지 않는다.
- 제목·본문·코드·카드의 각 계층은 고정 규격을 사용하며, 특정 페이지의 내용량 때문에 글자 크기·줄간격·문단 여백을 줄이거나 늘리지 않는다.
- 페이지별 scale-to-fit, transform scale, compact 모드, sparse-page space-evenly 확장을 금지한다.
- 한 논리 페이지를 억지로 한 화면에 끼워 넣지 않는다. 챕터 내용을 연속 흐름으로 배치하고 고정 높이 화면 페이지로 자동 분할한다.
- 화면 페이지들은 CSS balanced multi-column 방식으로 균등 분배한다. 목표 편차는 최대 줄 수 대비 3줄 이내이며, orphans/widows 3 규칙으로 문단 끝·시작이 과도하게 짧아지지 않게 한다.
- 내용이 많은 경우 자동으로 다음 화면 페이지로 이어지고, 내용 자체를 삭제·요약·늘리거나 페이지별로 글자를 축소하지 않는다.
- 읽기 본문에는 세로 스크롤을 만들지 않는다. 좌우 스와이프 및 화면 양끝 탭으로 페이지를 이동한다.
- 세로/가로 회전 모두 같은 typography/spacing 계약을 유지한다. Galaxy Tab S10 FE 태블릿 프로필을 우선 검증한다.
- 기존 page.id, 교재 본문, 메모·북마크·읽던 위치·자동 업데이트 계약은 보존한다.

## 보통의 교재·화면 수정

1. 정본 브랜치 최신 상태를 읽는다. 원본 content.json과 template만 수정한다.
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

트랙 1·2 전체와 트랙 3 Chapter 1만 제공한다. 트랙 3의 나머지 챕터 및 4~11은 준비 중이다. Python 실행기는 없으며 출력 설명을 실제 실행 결과처럼 표시하지 않는다. 추가 보완 우선순위는 Android 실행환경 안내, 개념별 작은 예제·줄별 추적, 미정의 용어, 반복 문장 정리, 틀린 이유를 파악하는 연습이다. 전체 커리큘럼 완성을 주장하지 않는다.
