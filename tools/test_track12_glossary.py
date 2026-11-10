"""Contract tests for all 200 terms and lossless preservation of approved first 150."""
from __future__ import annotations
import hashlib
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from compile_track12_glossary import EDITION, LEGACY_EDITION, SECOND_EDITION, PREVIOUS_EDITION, LEGACY_TERMS_SHA256, SECOND_TERMS_SHA256, UNIT_TITLES, digest, load_terms


def main():
    terms = load_terms()
    course = json.loads((ROOT / "live/content.json").read_text(encoding="utf-8"))
    new_track = [t for t in course["tracks"] if t["id"] == 12]
    assert len(new_track) == 1
    assert new_track[0]["available"] is True and new_track[0]["edition"] == EDITION
    chapters = [c for c in course["chapters"] if c["track"] == 12]
    assert len(chapters) == 20 and [c["title"] for c in chapters] == list(UNIT_TITLES)
    assert [len(c["pages"]) for c in chapters] == [10] * 20
    pages = [p for c in chapters for p in c["pages"]]
    assert [p["id"] for p in pages] == [f"t12-vibe-{n:03d}" for n in range(1, 201)]
    assert [c["id"] for c in chapters] == [f"t12-vibe-ch{n:02d}" for n in range(1, 21)]
    for i, (p, term) in enumerate(zip(pages, terms), start=1):
        assert p["kind"] == "vibe-term" and p["vibe"] == term, (i, "source mismatch")
        assert p["paragraphs"] == [term["explain"]]
        assert all(term[field] for field in ("explain", "example", "contrast", "wrong", "answer", "prompt"))
        assert sum(len(term[field]) for field in ("explain", "example", "contrast", "wrong", "answer", "prompt")) >= 390
    ids = [p["id"] for c in course["chapters"] for p in c["pages"]]
    assert len(ids) == len(set(ids)), "Preserved track page identity collision"
    assert all(t["id"] in list(range(1, 13)) for t in course["tracks"])

    audit = course["vibeGlossaryAudit"]
    assert audit["schema"] == EDITION and audit["termCount"] == 200 and audit["unitCount"] == 20
    assert audit["sourceSha256"] == digest(terms)
    assert audit["preservedFirst50SourceSha256"] == LEGACY_TERMS_SHA256
    assert audit["preservedFirst100SourceSha256"] == SECOND_TERMS_SHA256
    assert digest(terms[:50]) == LEGACY_TERMS_SHA256
    assert digest(terms[:100]) == SECOND_TERMS_SHA256
    assert audit["preservedFirst150SourceSha256"] == digest(terms[:150])
    assert audit["physicalDeviceTest"] == "NOT_RUN"

    html = (ROOT / "live/reader.html").read_text(encoding="utf-8")
    assert "VIBE_BOOK_DARK_V2" in html and "vibe-book-copy" in html, "Approved dark book design missing"
    assert "VIBE_BOOK_SWIPE_V3" in html, "Approved left/right touch swipe missing"
    assert 'VIBE_EDGE_TAP_V4' in html, 'Left/right edge tap navigation missing'
    assert "vibe-book-viewport" in html and "vibe-book-section" in html, "Continuous term page missing"
    assert "vibe-book-meaning" in html and "vibe-book-judgement" in html, "Five learning sections missing"
    assert len(html.encode("utf-8")) <= 3 * 1024 * 1024, "Existing Android shell 3 MiB cap"
    m = re.search(r'<script[^>]*id="course"[^>]*>([\s\S]*?)</script>', html)
    assert m, "Embedded runtime course missing"
    embedded = json.loads(m.group(1))
    runtime_pages = [p for c in embedded["chapters"] if c["track"] == 12 for p in c["pages"]]
    # New HTML transport omits the redundant paragraph; the runtime hydrates it
    # from the approved term source before any viewer can read it.
    assert all("paragraphs" not in p for p in runtime_pages)
    for page in runtime_pages:
        page["paragraphs"] = [page["vibe"]["explain"]]
    assert runtime_pages == pages, "Term content lost or corrupted in runtime transport"
    assert "TRACK12_LOSSLESS_TRANSPORT_V1" in html

    baseline = ROOT / "review/track4-before.json"
    prior_still_intact = False
    if baseline.exists():
        old = json.loads(baseline.read_text(encoding="utf-8"))
        for track in (1, 2):
            before = [c for c in old["chapters"] if c["track"] == track]
            after = [c for c in course["chapters"] if c["track"] == track]
            assert digest(before) == digest(after), f"Track {track} source changed"
        assert [t for t in old["tracks"] if t["id"] <= 11] == [
            t for t in course["tracks"] if t["id"] <= 11
        ], "Existing track metadata changed"
        previously_published = [c for c in old["chapters"] if c.get("track") == 12]
        # A first deployment starts with 15 chapters; all subsequent rebuilds
        # start with the already-published 20. Both must preserve 1-150 exactly.
        assert len(previously_published) in (15, 20), (
            "Unexpected baseline Track 12 chapter count"
        )
        original_fifteen = previously_published[:15]
        new_first_fifteen = chapters[:15]
        assert digest(original_fifteen) == digest(new_first_fifteen), (
            "Previously approved 1-150 chapters, page IDs or text changed"
        )
        if len(previously_published) == 20:
            assert digest(previously_published) == digest(chapters), (
                "Rebuilding the published 200-term course altered chapter text or IDs"
            )
        assert all(c["edition"] == LEGACY_EDITION for c in chapters[:5])
        assert all(c["edition"] == SECOND_EDITION for c in chapters[5:10])
        assert all(c["edition"] == PREVIOUS_EDITION for c in chapters[10:15])
        assert all(c["edition"] == EDITION for c in chapters[15:])
        assert audit["preservedFirst5ChaptersSha256"] == digest(original_fifteen[:5])
        assert audit["preservedFirst10ChaptersSha256"] == digest(original_fifteen[:10])
        assert audit["preservedFirst15ChaptersSha256"] == digest(original_fifteen)
        assert old.get("legacyTrack3") == course.get("legacyTrack3")
        assert old.get("bookAliases") == course.get("bookAliases")
        prior_still_intact = True

    report = {
        "status": "PASS", "scope": "content, source and embedded HTML verification",
        "terms": len(terms), "chapters": len(chapters), "sectionsPerTerm": 5,
        "oldTrackContentPreserved": prior_still_intact,
        "readerBytes": len(html.encode("utf-8")), "apkBuild": "NOT_RUN",
        "physicalDevice": "NOT_RUN",
        "sha256": hashlib.sha256(html.encode()).hexdigest(),
    }
    review = ROOT / "review"
    review.mkdir(exist_ok=True)
    (review / "track12-vibe-source-tests.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(report, ensure_ascii=False))


if __name__ == "__main__":
    main()
