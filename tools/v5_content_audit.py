from __future__ import annotations

import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

FORBIDDEN = [
    "이 PART의 목표",
    "이 PART에서는",
    "이 PART에서 반드시",
    "다음 PART에서는",
    "다음 PART에서",
    "지금은 외우지",
    "외울 필요",
    "외울 단어",
    "초보자",
    "쉽게 생각",
    "간단히 말",
    "책을 덮고",
    "자가점검",
    "비유로",
    "비유하면",
    "여기서 중요한 것은",
    "기억해야 할 것은",
]
MIN_CHAPTER_PROSE = 420
MAX_CODE_RATIO = 0.28
MAX_SHINGLE_REPEAT_RATIO = 0.08
SHINGLE_WIDTH = 12
LONG_PARAGRAPH_MIN = 100


def repo_root() -> Path:
    here = Path(__file__).resolve()
    for candidate in [here.parent.parent, Path.cwd(), Path.cwd().parent]:
        if (candidate / "app/src/main/assets/textbook/v5").is_dir():
            return candidate
    raise SystemExit("Cannot locate repository root containing app/src/main/assets/textbook/v5")


def normalize_space(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def strip_code(text: str) -> str:
    return re.sub(r"```[\s\S]*?```", "", text)


def prose_chars(text: str) -> int:
    text = strip_code(text)
    text = re.sub(r"(?m)^#+.*$", "", text)
    text = re.sub(r"[#>*_`|\-]+", "", text)
    return len(re.sub(r"\s+", "", text))


def semantic_chars(text: str) -> int:
    text = re.sub(r"[#>*_`|\-]+", "", text)
    return len(re.sub(r"\s+", "", text))


def code_ratio(text: str) -> float:
    code = sum(len(m.group(0)) for m in re.finditer(r"```[\s\S]*?```", text))
    prose = prose_chars(text)
    return code / max(1, code + prose)


def h2_chapters(text: str) -> list[str]:
    matches = list(re.finditer(r"(?m)^## CHAPTER\s+", text))
    if not matches:
        return []
    chunks = []
    for i, match in enumerate(matches):
        start = match.start()
        end = matches[i + 1].start() if i + 1 < len(matches) else len(text)
        chunks.append(text[start:end])
    return chunks


def paragraph_blocks(text: str) -> list[str]:
    # Remove code, headings, list/table lines, then split on blank lines.
    text = strip_code(text)
    blocks = []
    for raw in re.split(r"\n\s*\n", text):
        lines = [line.strip() for line in raw.splitlines()]
        if not lines or any(line.startswith("#") for line in lines):
            continue
        if all((not line) or line.startswith(("- ", "* ", ">", "|")) or re.match(r"^\d+[.)]\s", line) for line in lines):
            continue
        paragraph = normalize_space(" ".join(line for line in lines if line))
        if paragraph:
            blocks.append(paragraph)
    return blocks


def tokens_without_markup(text: str) -> list[str]:
    text = strip_code(text)
    text = re.sub(r"[#>*_`|\-]+", " ", text)
    return [t for t in re.split(r"\s+", text.strip()) if t]


def registry_ids(root: Path) -> set[str]:
    files = [
        root / "app/src/main/java/com/futuretech/poweruser/textbook/V5BookSourceRegistry.kt",
        root / "app/src/main/java/com/futuretech/poweruser/textbook/V5BookSupplementalSources.kt",
    ]
    ids: set[str] = set()
    for file in files:
        if not file.is_file():
            continue
        text = file.read_text(encoding="utf-8")
        ids.update(re.findall(r'\bs\(\s*"([A-Z0-9._-]+)"\s*,', text))
    return ids


def main() -> int:
    root = repo_root()
    v5 = root / "app/src/main/assets/textbook/v5"
    known_sources = registry_ids(root)
    errors: list[str] = []
    active_texts: list[tuple[str, str]] = []
    track_semantic: dict[str, int] = {}
    long_paragraph_owners: dict[str, list[str]] = defaultdict(list)

    track_dirs = sorted(p for p in v5.iterdir() if p.is_dir() and re.fullmatch(r"track_\d{2}", p.name))
    expected_tracks = [f"track_{n:02d}" for n in range(1, 12)]
    actual_tracks = [p.name for p in track_dirs]
    if actual_tracks != expected_tracks:
        errors.append(f"TRACK_DIRS expected={expected_tracks} actual={actual_tracks}")

    for track_dir in track_dirs:
        manifest_file = track_dir / "manifest.json"
        if not manifest_file.is_file():
            errors.append(f"{track_dir.name}: missing manifest.json")
            continue
        try:
            manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
        except Exception as exc:
            errors.append(f"{track_dir.name}: invalid manifest JSON: {exc}")
            continue

        track_num = int(track_dir.name[-2:])
        expected_track_id = f"T{track_num:02d}"
        if manifest.get("trackId") != expected_track_id:
            errors.append(f"{track_dir.name}: trackId={manifest.get('trackId')} expected={expected_track_id}")
        parts = manifest.get("parts") or []
        orders = [p.get("order") for p in parts]
        if orders != list(range(1, len(parts) + 1)):
            errors.append(f"{expected_track_id}: non-contiguous part orders {orders}")
        if len({p.get('id') for p in parts}) != len(parts):
            errors.append(f"{expected_track_id}: duplicate part ids")

        semantic_total = 0
        for idx, part in enumerate(parts, 1):
            part_id = part.get("id")
            expected_part_id = f"{expected_track_id}-P{idx:02d}"
            if part_id != expected_part_id:
                errors.append(f"{expected_track_id}: part {idx} id={part_id} expected={expected_part_id}")

            asset_rel = part.get("assetPath", "")
            source_rel = part.get("sourceMapPath", "")
            asset = root / "app/src/main/assets" / asset_rel
            source_map_file = root / "app/src/main/assets" / source_rel
            if not asset.is_file():
                errors.append(f"{part_id}: missing asset {asset_rel}")
                continue
            if not source_map_file.is_file():
                errors.append(f"{part_id}: missing source map {source_rel}")
                continue

            text = asset.read_text(encoding="utf-8")
            active_texts.append((f"{expected_track_id}/{asset.name}", text))
            semantic_total += semantic_chars(text)

            for phrase in FORBIDDEN:
                if phrase.lower() in text.lower():
                    errors.append(f"{part_id}: forbidden phrase {phrase!r}")

            ratio = code_ratio(text)
            if ratio > MAX_CODE_RATIO:
                errors.append(f"{part_id}: code ratio {ratio:.4f} > {MAX_CODE_RATIO}")

            chapters = h2_chapters(text)
            if not chapters:
                errors.append(f"{part_id}: no H2 CHAPTER")
            for chapter_index, chapter in enumerate(chapters, 1):
                n = prose_chars(chapter)
                if n < MIN_CHAPTER_PROSE:
                    errors.append(f"{part_id} CHAPTER {chapter_index:02d}: prose {n} < {MIN_CHAPTER_PROSE}")

            for para in paragraph_blocks(text):
                norm = normalize_space(para)
                if len(norm) >= LONG_PARAGRAPH_MIN:
                    long_paragraph_owners[norm].append(f"{part_id}:{asset.name}")

            try:
                source_map = json.loads(source_map_file.read_text(encoding="utf-8"))
            except Exception as exc:
                errors.append(f"{part_id}: invalid source-map JSON: {exc}")
                continue
            if source_map.get("partId") != part_id:
                errors.append(f"{part_id}: source-map partId={source_map.get('partId')}")
            sections = source_map.get("sections") or []
            if len(sections) != len(chapters):
                errors.append(f"{part_id}: chapters={len(chapters)} evidence={len(sections)}")
            for chapter_index, evidence in enumerate(sections, 1):
                prefix = f"{part_id}-S{chapter_index:02d}-"
                sid = evidence.get("sectionId", "")
                if not sid.startswith(prefix):
                    errors.append(f"{part_id}: evidence order/id mismatch {sid}, expected prefix {prefix}")
                source_ids = evidence.get("sourceIds") or []
                if not source_ids:
                    errors.append(f"{sid}: empty sourceIds")
                if len(source_ids) != len(set(source_ids)):
                    errors.append(f"{sid}: duplicate sourceIds")
                unknown = [s for s in source_ids if s not in known_sources]
                if unknown:
                    errors.append(f"{sid}: unknown source ids {unknown}")

        track_semantic[expected_track_id] = semantic_total

    duplicate_paras = {p: owners for p, owners in long_paragraph_owners.items() if len(owners) > 1}
    for para, owners in list(duplicate_paras.items())[:100]:
        errors.append(f"DUPLICATE_LONG_PARAGRAPH owners={owners}: {para[:120]!r}")

    shingle_counts: Counter[str] = Counter()
    shingle_total = 0
    for _, text in active_texts:
        tokens = tokens_without_markup(text)
        for i in range(max(0, len(tokens) - SHINGLE_WIDTH + 1)):
            shingle = " ".join(tokens[i : i + SHINGLE_WIDTH])
            shingle_counts[shingle] += 1
            shingle_total += 1
    duplicate_occurrences = sum(max(0, count - 1) for count in shingle_counts.values())
    shingle_ratio = duplicate_occurrences / shingle_total if shingle_total else 1.0
    if shingle_ratio > MAX_SHINGLE_REPEAT_RATIO:
        errors.append(
            f"SHINGLE_REPEAT ratio={shingle_ratio:.6f} > {MAX_SHINGLE_REPEAT_RATIO} "
            f"duplicates={duplicate_occurrences} total={shingle_total}"
        )

    report = {
        "track_dirs": actual_tracks,
        "active_part_count": len(active_texts),
        "track_semantic_chars": track_semantic,
        "long_duplicate_groups": len(duplicate_paras),
        "twelve_token_duplicate_ratio": round(shingle_ratio, 8),
        "error_count": len(errors),
        "errors": errors[:500],
        "note": "Literal 5x corpus target remains enforced by V5BookScaleCorpusGateTest; this Python audit covers manifest/evidence/editorial/duplication contracts.",
    }
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
