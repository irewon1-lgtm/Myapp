"""Real Chromium smoke for the 50-term mobile/tablet glossary.

No claim about a physical Android installation or automatic app update.
"""
from __future__ import annotations
import json
import os
import shutil
from pathlib import Path
from urllib.parse import unquote

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / "live/reader.html").read_text(encoding="utf-8")
COURSE = json.loads((ROOT / "live/content.json").read_text(encoding="utf-8"))
OUT = ROOT / "review"
OUT.mkdir(exist_ok=True)
ERRORS = []
CHECKS = []


def fixture(browser, width, height):
    page = browser.new_page(viewport={"width": width, "height": height})
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
        "document.querySelectorAll('.vibe-card').length===5 && "
        "chapters[current.c].track===12 && "
        "document.body.dataset.qaReady==='1'", timeout=30000
    )


def main():
    with sync_playwright() as pw:
        executable = (os.environ.get("CHROME") or shutil.which("chromium")
                      or shutil.which("google-chrome") or shutil.which("google-chrome-stable"))
        assert executable, "System Chromium must be available"
        browser = pw.chromium.launch(executable_path=executable, headless=True,
                                     args=["--no-sandbox", "--disable-dev-shm-usage"])
        for width, height in ((360, 800), (800, 1280)):
            page = fixture(browser, width, height)
            assert page.evaluate("COURSE.tracks.some(t=>t.id===12&&t.available)")
            page.evaluate("doAction('track',{dataset:{track:'12'}})")
            assert page.locator(".editorial-chapter-row").count() == 5
            CHECKS.append(f"track navigation {width}x{height}")
            for n in (1, 25, 50):
                open_term(page, n)
                chunks = page.locator(".vibe-card")
                assert chunks.count() == 5, n
                assert page.locator(".vibe-copy-button").count() == 1
                assert page.locator(".vibe-answer").count() == 1
                t = next(p["vibe"] for c in COURSE["chapters"] if c["track"] == 12
                         for p in c["pages"] if p["id"] == f"t12-vibe-{n:03d}")
                for i, field in enumerate(("explain", "example", "contrast",
                                           "wrong", "prompt")):
                    assert t[field] in chunks.nth(i).inner_text(), (n, field)
                page.locator(".vibe-answer summary").click()
                assert t["answer"] in page.locator(".vibe-answer").inner_text()
                button = page.locator(".vibe-copy-button")
                assert unquote(button.get_attribute("data-code")) == t["prompt"]
                assert page.locator('[data-action="vibe-copy"]').count() == 1
                assert page.locator('[data-action="practice"]').count() == 0
                assert page.evaluate("document.documentElement.scrollWidth") <= width + 2
                CHECKS.append(f"term {n} full five components and copy {width}x{height}")
                if n in (1, 50) and width == 360:
                    page.screenshot(path=str(OUT / f"track12-term-{n:03d}-phone.png"))
            open_term(page, 1)
            page.locator('[data-action="bookmark"]').click()
            stored = json.loads(page.evaluate("window.testSaved"))
            assert "t12-vibe-001" in stored["bookmarks"]
            assert page.evaluate("state.bookmarks.includes('t12-vibe-001')")
            CHECKS.append(f"bookmark persistence {width}x{height}")
            page.close()
        browser.close()
    assert not ERRORS, ERRORS
    result = {"status": "PASS", "scope": "real Chromium phone/tablet viewport",
              "tests": len(CHECKS), "checks": CHECKS,
              "deviceInstall": "NOT_RUN", "errors": ERRORS}
    (OUT / "track12-vibe-browser-tests.json").write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
