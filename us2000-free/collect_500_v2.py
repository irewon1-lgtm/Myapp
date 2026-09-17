#!/usr/bin/env python3
from __future__ import annotations
import importlib.util
import json
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("us2000_collect_base", HERE / "collect_500.py")
if SPEC is None or SPEC.loader is None:
    raise RuntimeError("US2000_BASE_COLLECTOR_IMPORT_FAILED")
base = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(base)


def parse_drawdown_raw(raw):
    text = str(raw or "").replace("−", "-")
    matches = re.findall(r"([-+]?\d+(?:\.\d+)?)\s*%", text)
    if not matches:
        return None
    pct = float(matches[-1])
    return abs(pct) if pct < 0 else 0.0


# Regression for the actual Finviz composite format: price + distance from 52W high.
assert parse_drawdown_raw("25.97 -16.10%") == 16.10
assert parse_drawdown_raw("100.00 -0.00%") == 0.0
assert parse_drawdown_raw("-42.32%") == 42.32

_original_parse_finviz = base.parse_finviz


def fixed_parse_finviz(page):
    out = _original_parse_finviz(page)
    row = out.get("Drawdown_52W_Pct", {})
    fixed = parse_drawdown_raw(row.get("raw"))
    if fixed is not None:
        row["value"] = fixed
        out["Drawdown_52W_Pct"] = row
    return out


base.parse_finviz = fixed_parse_finviz
base.main()

# Add explicit post-collection evidence for CLEAN review without changing record/chunk hashes.
root = HERE / "output"
manifest_path = root / "manifest.json"
manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
records = []
for chunk in manifest["chunks"]:
    records.extend(json.loads((root / chunk["file"]).read_text(encoding="utf-8")))

hold_details = []
drawdown_positive = 0
drawdown_samples = []
for record in records:
    for metric, cell in record["metrics"].items():
        if cell.get("status") == "HOLD":
            hold_details.append({
                "ticker": record["ticker"],
                "metric": metric,
                "reason": cell.get("reason"),
                "collectionErrors": record.get("collectionErrors", []),
            })
    dd = record["metrics"]["Drawdown_52W_Pct"]
    if dd.get("status") == "NUMERIC" and isinstance(dd.get("value"), (int, float)):
        if dd["value"] > 0:
            drawdown_positive += 1
            if len(drawdown_samples) < 5:
                drawdown_samples.append({"ticker": record["ticker"], "value": dd["value"], "raw": dd.get("raw")})

if len(hold_details) != manifest["totalHold"]:
    raise RuntimeError(f"HOLD_DETAIL_COUNT_MISMATCH:{len(hold_details)}:{manifest['totalHold']}")
if drawdown_positive == 0:
    raise RuntimeError("DRAWDOWN_PARSER_ZERO_POSITIVE_VALUES")

manifest["holdDetails"] = hold_details
manifest["parserAudit"] = {
    "drawdownCompositeRegression": parse_drawdown_raw("25.97 -16.10%"),
    "drawdownPositiveCount": drawdown_positive,
    "drawdownSamples": drawdown_samples,
}
manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")
print("US2000_FREE_V2_AUDIT", json.dumps({
    "holds": len(hold_details),
    "drawdownPositiveCount": drawdown_positive,
    "drawdownSamples": drawdown_samples,
}, ensure_ascii=False), flush=True)
