"""One-time, reviewable migration of the existing continuous reader to multiple tracks."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]


def replace_once(text, old, new):
    assert text.count(old)==1, ('Expected exactly one migration target',old[:100],text.count(old))
    return text.replace(old,new,1)


def apply_compatibility():
    patches=json.loads((ROOT/'tools/track4-compatibility-patches.json').read_text())
    allowed={'tools/verify_reader.cjs','tools/test_track4_browser.py','live/track4-book/reader.js'}
    for patch in patches:
        assert patch['path'] in allowed
        path=ROOT/patch['path'];text=path.read_text()
        if patch['new'] in text:continue
        path.write_text(replace_once(text,patch['old'],patch['new']))


def main():
    apply_compatibility()
    path=ROOT/'live/track4-book/ch02.md'
    text=path.read_text()
    text=text.replace('Jennifer Niederstetter Robbins가 아닌 Jennifer Niederst Robbins, 2025.', 'Jennifer Niederst Robbins, 2025.')
    text=text.replace(' 저자의 이름은 Jennifer Niederst Robbins입니다.', '')
    path.write_text(text)
    path=ROOT/'live/track4-book/ch12.md'
    text=path.read_text().replace('"内容を確認".replace("内容を確認", "内容")','"공부한 내용을 1~80자 범위로 적어 주세요."')
    path.write_text(text)

    path=ROOT/'live/track3-book/reader.js'
    text=path.read_text()
    if 'CONTINUOUS_MULTI_TRACK_V1' not in text:
        text=replace_once(text,'function sampleMarkup(sample){','function sampleMarkup(sample){\n    if(sample.language && sample.language!=="python")return window.Track4Web.sampleMarkup(sample);')
        text=replace_once(text,'9장 목차로','트랙 목차로')
        text=replace_once(text,"let value=original.trackPage();if(selectedTrack!==3)return value;","let value=original.trackPage();if(!chapters.some(c=>c.track===selectedTrack&&c.pages[0]?.book))return value;")
        text=replace_once(text,"'<small>9장 · 마지막으로 읽은 위치 기준</small>'","'<small>'+chapters.filter(c=>c.track===selectedTrack&&c.pages[0]?.book).length+'장 · 마지막으로 읽은 위치 기준</small>'")
        text=replace_once(text,'if(id!==3)return original.progressForTrack(id);','if(!chapters.some(c=>c.track===id&&c.pages[0]?.book))return original.progressForTrack(id);')
        text=replace_once(text,'const books=chapters.filter(c=>c.track===3);','const books=chapters.filter(c=>c.track===id&&c.pages[0]?.book);')
        text=replace_once(text,"['book-v1',COURSE.version,width,height,state.readerFont]","['book-v1',COURSE.version,chapters[current.c].track,width,height,state.readerFont]")
        text=replace_once(text,'for(const c of chapters.filter(x=>x.track===3)){','for(const c of chapters.filter(x=>x.track===chapters[current.c].track&&x.pages[0]?.book)){')
        text=replace_once(text,'host.remove();trackSlideMap={track:3,key,ready:true,entries,byId,total};','host.remove();trackSlideMap={track:chapters[current.c].track,key,ready:true,entries,byId,total};')
        text='/* CONTINUOUS_MULTI_TRACK_V1 — preserve Track 3; use the selected book track. */\n'+text
        path.write_text(text)
    path=ROOT/'tools/test_track3_book.py'
    text=path.read_text()
    old="assert all(p.get('kind')!='book' for c in course['chapters'] if c.get('track')!=3 for p in c['pages'])"
    new="assert all(p.get('kind')!='book' for c in course['chapters'] if c.get('track') not in (3,4) for p in c['pages'])"
    if old in text:text=replace_once(text,old,new)
    assert new in text
    path.write_text(text)
    (ROOT/'tools/build_content.py').write_text('''"""Build the two authored continuous textbooks without regenerating other tracks."""
from compile_track3_book import build
from compile_track4_book import build as build_track4

if __name__ == "__main__":
    build()
    build_track4()
''')
    path=ROOT/'CODINGROADMAP_LIVE.md';text=path.read_text()
    if '## Track 4 연속 교재' not in text:
        text+='''
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
'''
    path.write_text(text)
    print('Prepared Track 4 integration; prior Track 3 content not rewritten.')

if __name__=='__main__':main()
