"""One-time source migration. Never called by the steady-state book compiler."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[1]
def patch(path,old,new):
 p=ROOT/path;s=p.read_text()
 if new in s:return
 assert s.count(old)==1,(path,'source changed; reconcile before activation')
 p.write_text(s.replace(old,new,1))
patch('live/track3-book/reader.js','  window.importProgress=function(raw){',"  // Save book-specific fields before the shared reader input handler.\n  if(document.body&&typeof document.body.addEventListener==='function'){\n    document.body.addEventListener('input',e=>{if(window.bookInput(e))e.stopPropagation()});\n  }\n  window.importProgress=function(raw){")
patch('live/track3-book/reader.js',"+highlightPythonLine(line)+'</span>'","+(line?highlightPythonLine(line):'')+'</span>'")
patch('live/track3-book/reader.css','grid-template-columns:auto minmax(0,1fr) auto auto auto auto;gap:2px','grid-template-columns:auto minmax(0,1fr) auto auto auto auto auto!important;gap:2px')
p=ROOT/'live/track3-book/reader.css';s=p.read_text();rule='\n.book-mode .reader-page-info,.book-mode #page-pos{color:var(--bookMuted)!important}\n'
if rule not in s:p.write_text(s+rule)
(ROOT/'tools/build_content.py').write_text('"""Build the canonical app with the authored continuous Track 3 textbook."""\nfrom compile_track3_book import build\n\nif __name__ == "__main__":\n    build()\n')
old=ROOT/'live/track3.manuscript.md';archive=ROOT/'archive/track3-fragmented-manuscript-20260924.md'
if old.exists():
 archive.parent.mkdir(exist_ok=True)
 assert not archive.exists(),'Archive already exists; reconcile rather than overwrite'
 shutil.move(old,archive)
(ROOT/'CODINGROADMAP_LIVE.md').write_text('''# 코딩로드맵 수정·배포 정본

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
''')
shutil.copyfile(ROOT/'tools/track3_publish_workflow.yml',ROOT/'.github/workflows/publish-coding-roadmap-live.yml')
print('Track 3 source migration prepared; no channel published by this script.')
