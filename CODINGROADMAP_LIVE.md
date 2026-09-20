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

## 읽기 화면 고정 계약 — Galaxy Tab S10 FE

- 한 논리 페이지는 한 화면(viewport) 안에 모두 보여야 한다. 읽기 본문에 세로 스크롤을 만들지 않는다.
- 내용이 화면 높이를 넘으면 여백·간격을 먼저 compact 처리하고, 그래도 넘치면 페이지 전체를 비율 축소해 같은 화면 안에 맞춘다.
- 이전/다음 페이지는 좌우 스와이프로 이동하며, 손가락 이동을 따라가는 horizontal slide animation을 유지한다.
- 세로/가로 회전 모두 이 계약을 유지한다. Galaxy Tab S10 FE 태블릿 프로필을 우선 검증한다.
- 이 UI 수정 때문에 기존 page.id, 교재 본문, 메모·북마크·읽던 위치·자동 업데이트 계약을 바꾸지 않는다.

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

트랙 1·2만 제공한다. 3~11은 준비 중이다. Python 실행기는 없으며 출력 설명을 실제 실행 결과처럼 표시하지 않는다. 추가 보완 우선순위는 Android 실행환경 안내, 개념별 작은 예제·줄별 추적, 미정의 용어, 반복 문장 정리, 틀린 이유를 파악하는 연습이다. 전체 커리큘럼 완성을 주장하지 않는다.
