"""Extend the approved dark-book Track 12 from 150 to 200 words without rewriting 1-150.

The source-of-truth is the forty sets of five terms in live/vibe-terms.
Tracks 1-11 and the first fifteen Track 12 chapters (page IDs, text and edition)
are byte-for-byte equivalent after JSON serialization. Any unexpected baseline
or source change is a hard failure rather than an implicit migration.
"""
from __future__ import annotations

import hashlib
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
TRACK_ID = 12
LEGACY_EDITION = "vibe-terms-v1-001-050"
SECOND_EDITION = "vibe-terms-v1-001-100"
PREVIOUS_EDITION = "vibe-terms-v1-001-150"
EDITION = "vibe-terms-v1-001-200"
LEGACY_TERMS_SHA256 = "38ced95b17f009bb912ac64ec2e797436e946aacb6ae5af94639945a5a5c57a2"
SECOND_TERMS_SHA256 = "8c8139b882a6350655ea7b937c668905c007fb5679ede8b5b3ba27b2f1070d5e"
UNIT_TITLES = (
    "AI에게 앱 개발을 지시할 때 쓰는 말",
    "ChatGPT·Codex·AI 이해하기",
    "AI가 알아서 개발하게 만드는 용어",
    "AI가 작성한 코드 이해하기",
    "GitHub 코드 저장과 복구",
    "앱을 실제로 실행하고 배포하는 용어",
    "AI가 만든 앱이 정상인지 검수하기",
    "오류가 발생했을 때 꼭 알아야 할 말",
    "UI와 앱 화면을 이해하는 용어",
    "Android 앱 완성과 보안",
    "PRD와 개발 요구사항 심화",
    "AI에게 정확하게 지시하는 기술",
    "AI 연속 개발과 자동 복구",
    "앱의 내부 구조 이해",
    "인터넷·서버·데이터 통신",
    "UI·사용성·화면 품질 검수",
    "GitHub 협업과 버전 추적",
    "테스트 데이터와 고급 품질 검증",
    "Android 배포·데이터 보존·호환성",
    "보안·비용·최종 검수",
)
FIELDS = ("number", "term", "english", "explain", "example",
          "contrast", "wrong", "answer", "prompt")


def digest(value):
    raw = json.dumps(value, ensure_ascii=False, sort_keys=True).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()


def load_terms(root=ROOT):
    paths = sorted((root / "live/vibe-terms").glob("terms-*.json"))
    expected = [
        f"terms-{n:03d}-{n+4:03d}.json" for n in range(1, 201, 5)
    ]
    assert [p.name for p in paths] == expected, "Exactly 40 ordered five-term sources required"
    terms = []
    for path in paths:
        payload = json.loads(path.read_text(encoding="utf-8"))
        assert list(payload) == ["terms"], f"Unexpected source schema: {path.name}"
        assert len(payload["terms"]) == 5, f"Five complete entries required: {path.name}"
        terms.extend(payload["terms"])

    assert [t["number"] for t in terms] == list(range(1, 201))
    assert len(set(t["term"] for t in terms)) == 200, "Duplicate glossary titles"
    assert digest(terms[:50]) == LEGACY_TERMS_SHA256, "Approved terms 1-50 changed"
    assert digest(terms[:100]) == SECOND_TERMS_SHA256, "Approved terms 1-100 changed"
    for term in terms:
        assert set(term) == set(FIELDS), f"Incorrect field schema on word {term.get('number')}"
        for field in FIELDS[1:]:
            assert isinstance(term[field], str) and term[field].strip(), (
                term["number"], field
            )
        for field in FIELDS[3:]:
            assert len(term[field].strip()) >= 40, (
                term["number"], field, "Insufficient explanation"
            )
        assert len(term["prompt"]) < 1400, (term["number"], "Long Codex prompt")
    return terms


def build(root=ROOT):
    terms = load_terms(root)
    path = root / "live/content.json"
    course = json.loads(path.read_text(encoding="utf-8"))
    others = [c for c in course["chapters"] if c.get("track") != TRACK_ID]
    other_tracks = [t for t in course["tracks"] if t.get("id") != TRACK_ID]
    other_chapters_hash, other_tracks_hash = digest(others), digest(other_tracks)

    old_track = next((t for t in course["tracks"] if t["id"] == TRACK_ID), None)
    assert old_track, "Track 12 must already exist; no blank rebuild allowed"
    assert old_track.get("edition") in (LEGACY_EDITION, SECOND_EDITION, PREVIOUS_EDITION, EDITION), (
        "Unknown Track 12 edition; stop rather than overwrite existing course"
    )
    existing = sorted(
        [c for c in course["chapters"] if c.get("track") == TRACK_ID],
        key=lambda c: c["chapter"]
    )
    assert len(existing) in (15, 20), "Unexpected Track 12 chapter count"
    assert [c["chapter"] for c in existing] == list(range(1, len(existing)+1))
    first_fifteen = existing[:15]
    prior_audit = course.get("vibeGlossaryAudit", {})
    approved_150_sha = prior_audit.get(
        "preservedFirst150SourceSha256", prior_audit.get("sourceSha256")
    )
    assert approved_150_sha == digest(terms[:150]), (
        "Approved source terms 1-150 changed; stop, do not reauthor"
    )
    for idx, chapter in enumerate(first_fifteen, 1):
        assert chapter["id"] == f"t12-vibe-ch{idx:02d}"
        expected_edition = (LEGACY_EDITION if idx <= 5 else
                            SECOND_EDITION if idx <= 10 else PREVIOUS_EDITION)
        assert chapter["edition"] == expected_edition, (
            f"Previously approved chapter {idx} edition changed"
        )
        assert chapter["title"] == UNIT_TITLES[idx-1]
        assert len(chapter["pages"]) == 10
        for j, page in enumerate(chapter["pages"]):
            n = (idx-1)*10+j+1
            assert page["id"] == f"t12-vibe-{n:03d}"
            assert page["kind"] == "vibe-term"
            assert page["vibe"] == terms[n-1], f"Original term {n} modified"

    preserved_first_five_hash = digest(first_fifteen[:5])
    preserved_first_ten_hash = digest(first_fifteen[:10])
    preserved_first_fifteen_hash = digest(first_fifteen)
    new_chapters = []
    for unit_idx in range(16, 21):
        title = UNIT_TITLES[unit_idx-1]
        group = terms[(unit_idx-1)*10:unit_idx*10]
        pages = []
        for t in group:
            n = t["number"]
            pages.append({
                "id": f"t12-vibe-{n:03d}",
                "kind": "vibe-term",
                "eyebrow": f"바이브 코딩 용어 {n}/200 · 공개 1~200",
                "title": f"{n}. {t['term']} ({t['english']})",
                "paragraphs": [t["explain"]],
                "vibe": {key: t[key] for key in FIELDS},
            })
        new_chapters.append({
            "id": f"t12-vibe-ch{unit_idx:02d}",
            "track": TRACK_ID,
            "chapter": unit_idx,
            "title": title,
            "subtitle": f"{group[0]['number']}~{group[-1]['number']}번 · 실전 용어 10개",
            "desc": f"용어 10개를 실제 앱 개발 상황에 연결해 배웁니다: {title}",
            "pages": pages,
            "edition": EDITION,
            "remember": "AI의 개발 결과를 이해하고 확인하며 필요할 때 정확하게 지시하는 능력을 익힙니다.",
        })
    course["chapters"] = others + first_fifteen + new_chapters
    old_track.update(
        title="바이브 코딩 필수 용어 200",
        subtitle="1~200번 공개 · 20단원 · 책형 다크모드 · 뜻·실전·비교·판별·지시문",
        available=True,
        chapters=list(UNIT_TITLES),
        edition=EDITION,
    )

    assert preserved_first_fifteen_hash == digest([
        c for c in course["chapters"] if c.get("track") == TRACK_ID and c["chapter"] <= 15
    ]), "Original fifteen chapters changed"
    assert other_chapters_hash == digest([
        c for c in course["chapters"] if c.get("track") != TRACK_ID
    ]), "Other course chapters changed"
    assert other_tracks_hash == digest([
        t for t in course["tracks"] if t.get("id") != TRACK_ID
    ]), "Other track metadata changed"

    ids = [p["id"] for c in course["chapters"] for p in c["pages"]]
    chapter_ids = [c["id"] for c in course["chapters"]]
    assert len(ids) == len(set(ids)), "Page ID collision"
    assert len(chapter_ids) == len(set(chapter_ids)), "Chapter ID collision"

    course["vibeGlossaryAudit"] = {
        "schema": EDITION,
        "termCount": 200,
        "unitCount": 20,
        "firstTerm": 1,
        "lastTerm": 200,
        "futureTerms": 0,
        "fieldsPerTerm": ["explain", "example", "contrast", "wrong", "answer", "prompt"],
        "sourceSha256": digest(terms),
        "preservedFirst50SourceSha256": LEGACY_TERMS_SHA256,
        "preservedFirst100SourceSha256": SECOND_TERMS_SHA256,
        "preservedFirst150SourceSha256": approved_150_sha,
        "preservedFirst5ChaptersSha256": preserved_first_five_hash,
        "preservedFirst10ChaptersSha256": preserved_first_ten_hash,
        "preservedFirst15ChaptersSha256": preserved_first_fifteen_hash,
        "otherChaptersSha256": other_chapters_hash,
        "otherTracksSha256": other_tracks_hash,
        "physicalDeviceTest": "NOT_RUN",
    }
    path.write_text(json.dumps(course, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print(json.dumps(course["vibeGlossaryAudit"], ensure_ascii=False))
    return course


if __name__ == "__main__":
    build()
