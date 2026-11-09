"""Track 12 Vibe Coding glossary: additive 50-term compiler.

Source of truth: live/vibe-terms/terms-*.json. Does not edit Tracks 1-11,
existing stable page IDs, bookmarks, notes, or Android signing identity.
"""
from __future__ import annotations

import hashlib
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
TRACK_ID = 12
EDITION = "vibe-terms-v1-001-050"
UNIT_TITLES = (
    "AI에게 앱 개발을 지시할 때 쓰는 말",
    "ChatGPT·Codex·AI 이해하기",
    "AI가 알아서 개발하게 만드는 용어",
    "AI가 작성한 코드 이해하기",
    "GitHub 코드 저장과 복구",
)
FIELDS = ("number", "term", "english", "explain", "example",
          "contrast", "wrong", "answer", "prompt")


def digest(value):
    raw = json.dumps(value, ensure_ascii=False, sort_keys=True).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()


def load_terms(root=ROOT):
    paths = sorted((root / "live/vibe-terms").glob("terms-*.json"))
    assert len(paths) == 10, "Exactly ten approved source files are required for terms 001-050"
    terms = []
    for path in paths:
        payload = json.loads(path.read_text(encoding="utf-8"))
        assert list(payload) == ["terms"], f"Unexpected source schema: {path.name}"
        assert len(payload["terms"]) == 5, f"Each source file contains five complete terms: {path.name}"
        terms.extend(payload["terms"])
    assert [t["number"] for t in terms] == list(range(1, 51)), "Terms 1-50 must be continuous and ordered"
    assert len(set(t["term"] for t in terms)) == 50, "Duplicate glossary terms"
    for term in terms:
        assert set(term) == set(FIELDS), f"Missing or unexpected field for term {term.get('number')}"
        for field in FIELDS[1:]:
            value = term[field]
            assert isinstance(value, str) and value.strip(), (term["number"], field)
        for field in FIELDS[3:]:
            assert len(term[field].strip()) >= 40, (term["number"], field, "Content too short")
        assert len(term["prompt"]) < 1400, (term["number"], "Prompt should fit on a phone")
    return terms


def build(root=ROOT):
    terms = load_terms(root)
    path = root / "live/content.json"
    course = json.loads(path.read_text(encoding="utf-8"))
    other_chapters = [c for c in course["chapters"] if c.get("track") != TRACK_ID]
    other_tracks = [t for t in course["tracks"] if t.get("id") != TRACK_ID]
    before_chapters = digest(other_chapters)
    before_tracks = digest(other_tracks)
    existing = [c for c in course["chapters"] if c.get("track") == TRACK_ID]
    if existing:
        assert all(c.get("edition") == EDITION for c in existing), (
            "An existing Track 12 has a different edition; stop instead of overwriting it"
        )
    existing_track = next((t for t in course["tracks"] if t["id"] == TRACK_ID), None)
    if existing_track:
        assert existing_track.get("edition") == EDITION, "Track 12 ID already belongs to another track"
    else:
        course["tracks"].append({
            "id": TRACK_ID,
            "title": "바이브 코딩 필수 용어 200",
            "subtitle": "우선 공개 1~50번 · 5단원 · 뜻·실전·비교·판별·지시문",
            "available": True,
            "chapters": list(UNIT_TITLES),
            "edition": EDITION,
        })

    new_chapters = []
    for unit_idx, unit_title in enumerate(UNIT_TITLES, 1):
        group = terms[(unit_idx - 1) * 10:unit_idx * 10]
        pages = []
        for t in group:
            n = t["number"]
            page = {
                "id": f"t12-vibe-{n:03d}",
                "kind": "vibe-term",
                "eyebrow": f"바이브 코딩 용어 {n}/200 · 공개 1~50",
                "title": f"{n}. {t['term']} ({t['english']})",
                "paragraphs": [t["explain"]],
                "vibe": {key: t[key] for key in FIELDS},
            }
            pages.append(page)
        new_chapters.append({
            "id": f"t12-vibe-ch{unit_idx:02d}",
            "track": TRACK_ID,
            "chapter": unit_idx,
            "title": unit_title,
            "subtitle": f"{group[0]['number']}~{group[-1]['number']}번 · 실전 용어 10개",
            "desc": f"용어 10개를 실제 앱 개발 상황에 연결해 배웁니다: {unit_title}",
            "pages": pages,
            "edition": EDITION,
            "remember": "단어를 외우기보다 AI가 한 말이 맞는지 검수하고 필요한 지시를 내리는 방법을 익힙니다.",
        })
    course["chapters"] = other_chapters + new_chapters

    assert before_chapters == digest([c for c in course["chapters"] if c.get("track") != TRACK_ID])
    assert before_tracks == digest([t for t in course["tracks"] if t.get("id") != TRACK_ID])
    all_page_ids = [p["id"] for c in course["chapters"] for p in c["pages"]]
    assert len(all_page_ids) == len(set(all_page_ids)), "Colliding page ID"
    all_chapter_ids = [c["id"] for c in course["chapters"]]
    assert len(all_chapter_ids) == len(set(all_chapter_ids)), "Colliding chapter ID"

    course["vibeGlossaryAudit"] = {
        "schema": EDITION,
        "termCount": len(terms),
        "unitCount": len(UNIT_TITLES),
        "firstTerm": 1,
        "lastTerm": 50,
        "futureTerms": 150,
        "fieldsPerTerm": ["explain", "example", "contrast", "wrong", "answer", "prompt"],
        "sourceSha256": digest(terms),
        "otherChaptersSha256": before_chapters,
        "otherTracksSha256": before_tracks,
        "physicalDeviceTest": "NOT_RUN",
    }
    path.write_text(json.dumps(course, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(course["vibeGlossaryAudit"], ensure_ascii=False))
    return course


if __name__ == "__main__":
    build()
