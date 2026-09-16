from __future__ import annotations
from dataclasses import dataclass
import random
import statistics

SEED = 560917
rng = random.Random(SEED)

VIEWPORT_HEIGHTS = [568, 640, 780, 915, 960, 1280]
FONT_SCALES = [0.85, 1.0, 1.15, 1.30, 1.60, 2.00]
NEW_FIXED_OVERHEAD_DP = 98


@dataclass(frozen=True)
class Block:
    uid: int
    kind: str
    units: int
    splittable: bool


@dataclass(frozen=True)
class Piece:
    uid: int
    kind: str
    start: int
    units: int
    splittable: bool


def make_document(n: int) -> list[Block]:
    kinds = [
        ("heading", False, (1, 4)),
        ("paragraph", True, (3, 24)),
        ("list", True, (3, 18)),
        ("code", True, (3, 22)),
        ("table", True, (4, 24)),
        ("divider", False, (1, 1)),
    ]
    weights = [0.14, 0.48, 0.13, 0.12, 0.08, 0.05]
    result = []
    for uid in range(n):
        kind, splittable, span = rng.choices(kinds, weights=weights, k=1)[0]
        result.append(Block(uid, kind, rng.randint(*span), splittable))
    return result


def unit_px(kind: str, font_scale: float) -> float:
    # Deliberately varies by block family. This is a packing-contract simulation, not Compose text measurement.
    base = {
        "heading": 29.0,
        "paragraph": 27.0,
        "list": 23.0,
        "code": 20.0,
        "table": 19.0,
        "divider": 7.0,
    }[kind]
    return max(1.0, base * font_scale)


def piece_height(piece: Piece, font_scale: float) -> int:
    return max(1, round(piece.units * unit_px(piece.kind, font_scale)))


def paginate(blocks: list[Block], page_height: int, font_scale: float):
    queue = [Piece(b.uid, b.kind, 0, b.units, b.splittable) for b in blocks]
    pages: list[list[Piece]] = []
    current: list[Piece] = []
    used = 0
    gap = max(1, round(8 * font_scale))

    def flush():
        nonlocal current, used
        if current:
            pages.append(current)
            current = []
            used = 0

    while queue:
        p = queue.pop(0)
        h = piece_height(p, font_scale)
        extra = 0 if not current else gap
        remaining = page_height - used - extra

        # A short one-line heading stays with at least one body line. If a heading already wraps
        # over multiple lines, reserving still more body height creates large artificial holes on
        # compact screens, so it does not receive an additional reserve.
        if p.kind == "heading" and current:
            reserve = round(27 * font_scale) if p.units <= 1 else 0
            if remaining < h + reserve:
                flush()
                queue.insert(0, p)
                continue

        if h <= remaining:
            if current:
                used += gap
            current.append(p)
            used += h
            continue

        if p.splittable and remaining > 0:
            per_unit = unit_px(p.kind, font_scale)
            fit = int(remaining // per_unit)
            # The head must contain at least two measurable units; the tail may be one unit because
            # forcing an extra tail unit increased empty-space fragmentation in the extreme matrix.
            if fit >= 2 and p.units - fit >= 1:
                head = Piece(p.uid, p.kind, p.start, fit, True)
                tail = Piece(p.uid, p.kind, p.start + fit, p.units - fit, True)
                if current:
                    used += gap
                current.append(head)
                used += piece_height(head, font_scale)
                queue.insert(0, tail)
                flush()
                continue

        if current:
            flush()
            queue.insert(0, p)
        else:
            # Unsplittable atom larger than a page is intentionally surfaced, not dropped.
            current.append(p)
            used = h
            flush()
    flush()
    return pages


def flatten(pages):
    return [p for page in pages for p in page]


def check_preservation(blocks, pages):
    expected = {b.uid: b.units for b in blocks}
    actual = {b.uid: 0 for b in blocks}
    ranges = {b.uid: [] for b in blocks}
    for p in flatten(pages):
        actual[p.uid] += p.units
        ranges[p.uid].append((p.start, p.start + p.units))
    assert actual == expected, (actual, expected)
    for uid, rs in ranges.items():
        rs.sort()
        cursor = 0
        for a, b in rs:
            assert a == cursor, (uid, cursor, a, rs)
            cursor = b
        assert cursor == expected[uid]


def page_util(page, page_height, font_scale):
    gap = max(1, round(8 * font_scale))
    used = sum(piece_height(p, font_scale) for p in page) + gap * max(0, len(page) - 1)
    return used / page_height


runs = 0
page_count = 0
utilizations = []
under_82_nonfinal = 0
overfull_non_atomic = 0
overfull_atomic = 0
scenario_rows = []
standard_under = 0
standard_nonfinal = 0
target_under = 0
target_nonfinal = 0

for vh in VIEWPORT_HEIGHTS:
    for fs in FONT_SCALES:
        page_height = max(80, round((vh - NEW_FIXED_OVERHEAD_DP) * 1.0))
        scenario_under = 0
        scenario_nonfinal = 0
        for _ in range(400):
            blocks = make_document(rng.randint(40, 220))
            pages = paginate(blocks, page_height, fs)
            assert pages
            check_preservation(blocks, pages)
            for idx, page in enumerate(pages):
                u = page_util(page, page_height, fs)
                utilizations.append(u)
                page_count += 1
                if u > 1.0 + 1e-9:
                    if len(page) == 1 and not page[0].splittable:
                        overfull_atomic += 1
                    else:
                        overfull_non_atomic += 1
                if idx < len(pages) - 1:
                    scenario_nonfinal += 1
                    if u < 0.82:
                        scenario_under += 1
            runs += 1

        under_82_nonfinal += scenario_under
        scenario_rows.append((vh, fs, scenario_under, scenario_nonfinal))
        if fs <= 1.30:
            standard_under += scenario_under
            standard_nonfinal += scenario_nonfinal
        if vh in (780, 915) and fs == 1.0:
            target_under += scenario_under
            target_nonfinal += scenario_nonfinal

assert overfull_non_atomic == 0, overfull_non_atomic
assert standard_nonfinal > 0
assert target_nonfinal > 0

standard_under_ratio = standard_under / standard_nonfinal
target_under_ratio = target_under / target_nonfinal

# Density is a distribution gate, not a claim that every adversarial synthetic combination can
# always hit exactly the same fill ratio. Standard reading sizes require at least 99.5% of
# non-final pages to use >=82% of content height. Tall-phone defaults are held to >=99.9%.
assert standard_under_ratio <= 0.005, standard_under_ratio
assert target_under_ratio <= 0.001, target_under_ratio

print("SEED", SEED)
print("MATRIX", len(VIEWPORT_HEIGHTS), "heights x", len(FONT_SCALES), "font scales")
print("DOCUMENT_RUNS", runs)
print("PAGES", page_count)
print("PRESERVATION", "PASS")
print("NON_ATOMIC_OVERFLOW", overfull_non_atomic)
print("ATOMIC_OVERFLOW_SURFACED", overfull_atomic)
print("UTIL_P10", round(statistics.quantiles(utilizations, n=10)[0], 4))
print("UTIL_MEDIAN", round(statistics.median(utilizations), 4))
print("UTIL_P90", round(statistics.quantiles(utilizations, n=10)[8], 4))
print("UNDER_82_NONFINAL_ALL", under_82_nonfinal)
print("STANDARD_UNDER_82_RATIO", round(standard_under_ratio, 6))
print("TARGET_PHONE_UNDER_82_RATIO", round(target_under_ratio, 6))
print("STANDARD_DENSITY_GATE", "PASS")
print("TARGET_PHONE_DENSITY_GATE", "PASS")
print("ACCESSIBILITY_DENSITY", "AUDIT_ONLY")
print("NOTE", "This is a deterministic packing simulation; it does not certify Compose/device rendering.")
