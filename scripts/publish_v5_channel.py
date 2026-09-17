#!/usr/bin/env python3
"""Write the V5 remote-content channel marker after textbook content commits are complete.

Usage:
  python scripts/publish_v5_channel.py --revision "$(git rev-parse HEAD)"
  git add app/src/main/assets/textbook/v5/channel.json
  git commit -m "publish(v5): advance content channel"

The revision must be the content commit *before* the channel-marker commit. No network calls are made.
"""
from __future__ import annotations

import argparse
import json
import re
from datetime import datetime, timezone
from pathlib import Path

REVISION_RE = re.compile(r"^[0-9a-f]{40}$")
ROOT = Path(__file__).resolve().parents[1]
CHANNEL = ROOT / "app/src/main/assets/textbook/v5/channel.json"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--revision", required=True)
    args = parser.parse_args()
    revision = args.revision.strip().lower()
    if not REVISION_RE.fullmatch(revision):
        parser.error("--revision must be a full 40-character lowercase Git commit SHA")

    payload = {
        "schemaVersion": 1,
        "revision": revision,
        "publishedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"),
    }
    CHANNEL.parent.mkdir(parents=True, exist_ok=True)
    CHANNEL.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    print(f"prepared {CHANNEL.relative_to(ROOT)} -> {revision}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
