"""Actual Chromium acceptance for dark mockup 4: 50 words, one scrollable page each.

The test runs in a native-bridge emulator, NOT on a physical Android device.
"""
from __future__ import annotations
import json
import os
import re
import shutil
from pathlib import Path
from urllib.parse import unquote

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / "live/reader.html").read_text(encoding="utf-8")
COURSE = json.loads((ROOT / "live/content.json").read_text(encoding="utf-8"))
OUT = ROOT / "review"
OUT.mkdir(exist_ok=True)
TERMS = [p for c in COURSE["chapters"] if c["track"] == 12 for p in c["pages"]]
ERRORS = []
CHECKS = []


def normalize(s):
    return re.sub(r"\s+", " ", str(s)).strip()


def fixture(browser, width, height):
    page = browser.new_page(viewport={"width": width, "height": height},
                            has_touch=True, is_mobile=True)
    page.on("pageerror", lambda err: ERRORS.append(str(err)))
    bridge = """window.testSaved='{}';
window.RoadmapNative={
 getState:()=>window.testSaved,
 saveState:s=>{window.testSaved=s},
 getLegacy:()=> '{}',
 getStatus:()=> '검증용 브리지',
 ready:()=>{}
};"""
    page.set_content(SOURCE.replace("<script>", "<script>" + bridge, 1),
                     wait_until="domcontentloaded")
    return page


def open_term(page, n):
    page.evaluate("n => openPage('t12-vibe-'+String(n).padStart(3,'0'))", n)
    page.wait_for_function(
        "document.body.dataset.vibeReady==='1' && "
        "document.querySelector('.vibe-book-page')?.dataset.pageId===state.location && "
        "document.querySelectorAll('.vibe-book-section').length===5",
        timeout=12000,
    )


def check_single_page(page, n, width, height):
    term = TERMS[n - 1]["vibe"]
    actual = page.evaluate("""() => {
      const shell=document.querySelector('.vibe-book-shell');
      const viewport=document.getElementById('vibe-book-viewport');
      const article=document.querySelector('.vibe-book-page');
      const sections=[...document.querySelectorAll('.vibe-book-section')];
      const footer=document.querySelector('.vibe-book-footer');
      const q=(x)=>getComputedStyle(x);
      return {
        id:article?.dataset.pageId, term:Number(shell?.dataset.vibeTerm),
        sections:sections.map(x=>x.innerText),
        textSizes:sections.map(x=>parseFloat(q(x.querySelector('p')).fontSize)),
        edgeBorders:sections.map(x=>q(x).borderLeftStyle),
        bodyBackground:q(shell).backgroundColor,
        articleCount:document.querySelectorAll('.vibe-book-page').length,
        legacyPager:!!document.getElementById('paper'),
        verticalScrollable:viewport.scrollHeight>viewport.clientHeight,
        horizontalOverflow:document.documentElement.scrollWidth>innerWidth+2,
        scrollLeft:viewport.scrollLeft,
        footerVisible:footer.getBoundingClientRect().top>=0
          && footer.getBoundingClientRect().bottom<=innerHeight+2,
        topVisible:document.querySelector('.vibe-book-top').getBoundingClientRect().top>=0,
        copyText:decodeURIComponent(document.querySelector('[data-action="vibe-copy"]')?.dataset.code||''),
        questionLabel:sections[3].querySelector('h2').innerText,
        noteButton:!!document.querySelector('[data-action="note"]'),
        nextDisabled:document.querySelector('[data-action="vibe-next"]').disabled,
        previousDisabled:document.querySelector('[data-action="vibe-prev"]').disabled,
        fontButton:!!document.querySelector('[data-action="vibe-font"]'),
        theme:q(shell).colorScheme
      };
    }""")
    assert actual["id"] == f"t12-vibe-{n:03d}", (n, "wrong page", actual["id"])
    assert actual["term"] == n and actual["articleCount"] == 1
    assert actual["legacyPager"] is False, (n, "old physical page splitter returned")
    assert actual["bodyBackground"] == "rgb(23, 21, 18)", (n, actual["bodyBackground"])
    assert actual["theme"] == "dark", (n, actual["theme"])
    assert actual["footerVisible"] and actual["topVisible"], (n, "navigation clipped")
    assert actual["horizontalOverflow"] is False, (n, "horizontal scroll")
    assert all(s == "none" for s in actual["edgeBorders"]), (n, "boxed paragraphs")
    assert all(s >= 17 for s in actual["textSizes"]), (n, "unreadably small text")
    assert actual["copyText"] == term["prompt"]
    assert actual["noteButton"] and actual["fontButton"]
    assert actual["questionLabel"].startswith("4.")
    assert actual["previousDisabled"] == (n == 1)
    assert actual["nextDisabled"] == (n == 50)
    for i, content in enumerate((term["explain"], term["example"],
                                 term["contrast"], term["wrong"], term["prompt"])):
        assert normalize(content) in normalize(actual["sections"][i]), (
            n, i, "authored lesson text missing"
        )
    assert normalize(term["answer"]) in normalize(actual["sections"][3]), (
        n, "AI error question explanation lost"
    )
    CHECKS.append({"term": n, "viewport": [width, height], "status": "PASS"})


def navigation_and_persistence(page):
    open_term(page, 1)
    assert page.evaluate("document.getElementById('vibe-book-viewport').scrollHeight > document.getElementById('vibe-book-viewport').clientHeight")
    page.evaluate("""() => {
       const e=document.getElementById('vibe-book-viewport');
       e.scrollTop=140;
       e.dispatchEvent(new Event('scroll'));
    }""")
    page.wait_for_timeout(370)
    scrolled = page.evaluate("document.getElementById('vibe-book-viewport').scrollTop")
    assert scrolled >= 80, "Long page must scroll vertically rather than split into 2 slides"
    assert page.evaluate("state.location") == "t12-vibe-001", "Scrolling switched the term"
    assert page.evaluate("state.vibeBookScroll['t12-vibe-001']") >= 80, "Scroll position not saved"
    page.locator('[data-action="vibe-next"]').click()
    assert page.evaluate("state.location") == "t12-vibe-002"
    page.locator('[data-action="vibe-prev"]').click()
    assert page.evaluate("state.location") == "t12-vibe-001"
    assert page.evaluate("document.getElementById('vibe-book-viewport').scrollTop") >= 80
    CHECKS.append({"test": "one term = one vertical page, next/prev navigation, resume", "status": "PASS"})

    page.locator('[data-action="vibe-font"]').click()
    sz=page.evaluate("parseFloat(getComputedStyle(document.querySelector('.vibe-book-section p')).fontSize)")
    assert sz == 20, ("Font size control", sz)
    page.locator('[data-action="bookmark"]').click()
    stored = json.loads(page.evaluate("window.testSaved"))
    assert "t12-vibe-001" in stored["bookmarks"]
    assert stored["vibeBookFont"] == 20
    CHECKS.append({"test": "bookmarks, font sizing persist without replacing other state", "status": "PASS"})

    page.locator('[data-action="note"]').click()
    page.locator("#note").fill("바이브 용어 첫 번째 메모 보존")
    page.locator('[data-action="close"]').click()
    assert page.evaluate("state.notes['t12-vibe-001']") == "바이브 용어 첫 번째 메모 보존"
    CHECKS.append({"test": "legacy per-page notes remain available", "status": "PASS"})

    for n in (10, 20, 30, 40):
        open_term(page, n)
        page.locator('[data-action="vibe-next"]').click()
        assert page.evaluate("state.location") == f"t12-vibe-{n+1:03d}", (
            n, "chapter boundary skipped a term"
        )
    open_term(page, 50)
    assert page.locator('[data-action="vibe-next"]').is_disabled()
    CHECKS.append({"test": "five chapters are continuous 1-50 and stop at the last term", "status": "PASS"})



def drag_touch(page, session, *, start_x, start_y, end_x, end_y, steps=7):
    """Dispatch genuine Chrome touch events; not artificial DOM event handlers."""
    session.send("Input.dispatchTouchEvent", {
        "type": "touchStart",
        "touchPoints": [{"x": start_x, "y": start_y, "id": 1}],
    })
    for k in range(1, steps + 1):
        f = k / steps
        session.send("Input.dispatchTouchEvent", {
            "type": "touchMove",
            "touchPoints": [{"x": start_x + (end_x-start_x)*f,
                             "y": start_y + (end_y-start_y)*f, "id": 1}],
        })
        page.wait_for_timeout(9)
    session.send("Input.dispatchTouchEvent", {"type": "touchEnd", "touchPoints": []})
    page.wait_for_timeout(65)


def swipe_term(page, session, direction):
    box=page.locator("#vibe-book-viewport").bounding_box()
    assert box and box["width"]>250 and box["height"]>240
    y=box["y"]+min(175,box["height"]*.30)
    x0=box["x"]+box["width"]*(.78 if direction=="left" else .22)
    x1=box["x"]+box["width"]*(.22 if direction=="left" else .78)
    drag_touch(page,session,start_x=x0,start_y=y,end_x=x1,end_y=y+3)


def touch_gesture_tests(page):
    open_term(page,1)
    session=page.context.new_cdp_session(page)
    try:
        assert page.evaluate("matchMedia('(pointer: coarse)').matches"), "Touch emulation inactive"

        # It must be possible to read tall text vertically without changing terms.
        rect=page.locator("#vibe-book-viewport").bounding_box()
        start=rect["y"]+min(320,rect["height"]*.60)
        end=rect["y"]+min(105,rect["height"]*.18)
        x=rect["x"]+rect["width"]*.48
        drag_touch(page,session,start_x=x,start_y=start,end_x=x+4,end_y=end)
        assert page.evaluate("state.location")=="t12-vibe-001", "Vertical scroll switched terms"
        assert page.evaluate("document.getElementById('vibe-book-viewport').scrollTop")>15, (
            "Touch vertical scrolling must remain enabled"
        )
        CHECKS.append({"test":"real vertical touch scroll stays within one term","status":"PASS"})

        # A very short or diagonal touch must NOT turn the page.
        rect=page.locator("#vibe-book-viewport").bounding_box()
        x=rect["x"]+rect["width"]*.6
        y=rect["y"]+min(150,rect["height"]*.28)
        drag_touch(page,session,start_x=x,start_y=y,end_x=x-18,end_y=y+2)
        assert page.evaluate("state.location")=="t12-vibe-001", "Accidental short swipe"
        drag_touch(page,session,start_x=x,start_y=y,end_x=x-64,end_y=y-120)
        assert page.evaluate("state.location")=="t12-vibe-001", "Diagonal scroll turned a page"
        CHECKS.append({"test":"short and diagonal gestures do not switch terms","status":"PASS"})

        # Swipe left across every vocabulary item; including chapter boundaries.
        for number in range(1,50):
            assert page.evaluate("state.location")==f"t12-vibe-{number:03d}",number
            swipe_term(page,session,"left")
            assert page.evaluate("state.location")==f"t12-vibe-{number+1:03d}",(
                number,"left swipe must open the NEXT term"
            )
            assert page.evaluate("document.querySelectorAll('.vibe-book-page').length")==1
        swipe_term(page,session,"left")
        assert page.evaluate("state.location")=="t12-vibe-050", "51st term is not published"
        CHECKS.append({"test":"49 native touch swipes forward (01 through 50) and right endpoint","status":"PASS"})

        # Swipe right across every term, including chapter boundaries.
        for number in range(50,1,-1):
            swipe_term(page,session,"right")
            assert page.evaluate("state.location")==f"t12-vibe-{number-1:03d}",(
                number,"right swipe must open the PREVIOUS term"
            )
        swipe_term(page,session,"right")
        assert page.evaluate("state.location")=="t12-vibe-001", "Cannot swipe before term 1"
        CHECKS.append({"test":"49 native touch swipes backward (50 through 01) and left endpoint","status":"PASS"})

        # Swipes on a control must not activate the page-navigation gesture.
        button=page.locator(".vibe-book-copy")
        page.evaluate("document.querySelector('.vibe-book-copy').scrollIntoView({block:'center'})")
        box=button.bounding_box()
        assert box, "Codex copy control absent"
        x=box["x"]+box["width"]*.55
        y=box["y"]+box["height"]*.50
        drag_touch(page,session,start_x=x,start_y=y,end_x=max(5,x-100),end_y=y)
        assert page.evaluate("state.location")=="t12-vibe-001", "Touching a button changed pages"
        CHECKS.append({"test":"copy button is excluded from swipe navigation","status":"PASS"})
        edge_tap_tests(page,session)
    finally:
        session.detach()



def tap_touch(page, session, x, y):
    """Send a real mobile tap through Chrome DevTools touch input."""
    session.send("Input.dispatchTouchEvent", {
        "type": "touchStart",
        "touchPoints": [{"x": x, "y": y, "id": 1}],
    })
    page.wait_for_timeout(32)
    session.send("Input.dispatchTouchEvent", {
        "type": "touchEnd", "touchPoints": [],
    })
    page.wait_for_timeout(75)


def tap_edge(page, session, side):
    bounds=page.locator("#vibe-book-viewport").bounding_box()
    assert bounds and bounds["width"]>=300 and bounds["height"]>=250
    x=bounds["x"]+(12 if side=="left" else bounds["width"]-12)
    y=bounds["y"]+min(145,bounds["height"]*.30)
    tap_touch(page,session,x,y)


def edge_tap_tests(page, session):
    # A drag may suppress synthetic clicks briefly. Wait before intentional taps.
    page.wait_for_timeout(660)
    open_term(page,1)
    tap_edge(page,session,"left")
    assert page.evaluate("state.location")=="t12-vibe-001", "Term 0 must not be opened"

    # All right-edge taps: 1→50, including 10→11 etc.
    for number in range(1,50):
        tap_edge(page,session,"right")
        assert page.evaluate("state.location")==f"t12-vibe-{number+1:03d}", (
            number,"right edge must open next term"
        )
    tap_edge(page,session,"right")
    assert page.evaluate("state.location")=="t12-vibe-050", "Term 51 is unavailable"
    CHECKS.append({"test":"49 genuine right-edge taps from 1 to 50; last term clamped","status":"PASS"})

    # All left-edge taps: 50→1, including all chapter boundaries.
    for number in range(50,1,-1):
        tap_edge(page,session,"left")
        assert page.evaluate("state.location")==f"t12-vibe-{number-1:03d}", (
            number,"left edge must open previous term"
        )
    tap_edge(page,session,"left")
    assert page.evaluate("state.location")=="t12-vibe-001"
    CHECKS.append({"test":"49 genuine left-edge taps from 50 to 1; first term clamped","status":"PASS"})

    # A normal tap in the middle of the page is not a turn.
    rect=page.locator("#vibe-book-viewport").bounding_box()
    tap_touch(page,session,rect["x"]+rect["width"]*.50,
              rect["y"]+rect["height"]*.25)
    assert page.evaluate("state.location")=="t12-vibe-001","Center tap changed terms"
    CHECKS.append({"test":"center tap does not trigger a page turn","status":"PASS"})

    # At any vertical reading position, the edge taps should still work.
    page.evaluate("""() => {
       const vp=document.getElementById('vibe-book-viewport');
       vp.scrollTop=Math.min(220,vp.scrollHeight-vp.clientHeight);
    }""")
    tap_edge(page,session,"right")
    assert page.evaluate("state.location")=="t12-vibe-002", (
        "Right edge must work after scrolling the text"
    )
    tap_edge(page,session,"left")
    assert page.evaluate("state.location")=="t12-vibe-001"
    CHECKS.append({"test":"edge taps work while reading long scrolled text","status":"PASS"})


def mouse_edge_tests(page):
    """Also check desktop/tablet pointer clicks, not only touch simulation."""
    open_term(page,1)
    vp=page.locator("#vibe-book-viewport").bounding_box()
    xleft=vp["x"]+14
    xright=vp["x"]+vp["width"]-14
    y=vp["y"]+min(155,vp["height"]*.27)
    page.mouse.click(xleft,y)
    assert page.evaluate("state.location")=="t12-vibe-001"
    page.mouse.click(xright,y)
    assert page.evaluate("state.location")=="t12-vibe-002"
    page.mouse.click(xleft,y)
    assert page.evaluate("state.location")=="t12-vibe-001"
    CHECKS.append({"test":"left/right tap zones also work with mouse/tablet pointer","status":"PASS"})


def main():
    assert len(TERMS) == 50
    with sync_playwright() as pw:
        executable = (os.environ.get("CHROME") or shutil.which("chromium")
                      or shutil.which("google-chrome") or shutil.which("google-chrome-stable"))
        assert executable, "A real installed Chromium browser is required"
        browser = pw.chromium.launch(executable_path=executable, headless=True,
                                     args=["--no-sandbox", "--disable-dev-shm-usage"])
        for width, height in ((360, 800), (800, 1280)):
            page = fixture(browser, width, height)
            assert page.evaluate("COURSE.tracks.some(t=>t.id===12&&t.available)")
            page.evaluate("doAction('track',{dataset:{track:'12'}})")
            assert page.locator(".editorial-chapter-row").count() == 5
            for n in range(1, 51):
                open_term(page, n)
                check_single_page(page, n, width, height)
                if n in (1, 25, 50) and width == 360:
                    page.screenshot(path=str(OUT / f"track12-darkbook-top-{n:03d}.png"))
            if width == 360:
                navigation_and_persistence(page)
                touch_gesture_tests(page)
                open_term(page, 1)
                page.evaluate("""() => {
                   const v=document.getElementById('vibe-book-viewport');
                   v.scrollTop=v.scrollHeight;
                }""")
                page.screenshot(path=str(OUT / "track12-darkbook-bottom-001.png"))
            if width == 800:
                mouse_edge_tests(page)
            page.close()
        browser.close()
    assert not ERRORS, ERRORS
    report = {
        "status": "PASS",
        "scope": "real Chromium; approved dark book mockup 4; not a physical Android test",
        "terms": len(TERMS), "viewports": [[360,800],[800,1280]],
        "singleTermVerticalPages": 50,
        "genuineAndroidStyleTouchSwipes": 98,
        "genuineLeftRightEdgeTaps": 98,
        "edgeTapNavigation": True,
        "verticalTouchScrollPreserved": True,
        "fiveSectionsPerTerm": True, "paragraphBoxesRemoved": True,
        "tests": len(CHECKS), "checks": CHECKS,
        "physicalDevice": "NOT_RUN", "apkRebuild": "NOT_REQUIRED",
        "errors": ERRORS,
    }
    (OUT / "track12-vibe-browser-tests.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps({k: v for k, v in report.items() if k != "checks"}, ensure_ascii=False))


if __name__ == "__main__":
    main()
