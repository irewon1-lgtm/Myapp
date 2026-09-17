# V5 Page Packing Extreme Simulation Report

Date: 2026-09-17
Branch: `textbook-v5-deep-book-engine`
Seed: `560917`

This report records a **deterministic synthetic packing simulation**, not a claim of Android/Compose device rendering success.

## Current V5 reader geometry used

- top bar: 26dp
- part strip: 18dp
- footer: 14dp
- outer vertical padding: 0dp
- page vertical padding: 3dp top + 3dp bottom
- fixed vertical overhead used by simulator: 64dp
- ordinary block gap: 5dp

## Matrix

- viewport heights: 568, 640, 780, 915, 960, 1280dp
- font scales: 0.85, 1.0, 1.15, 1.30, 1.60, 2.00
- synthetic documents per matrix cell: 400
- total documents: 14,400
- total generated pages: 892,182

## Actual execution result

- content piece preservation/order: PASS
- non-atomic/splittable overflow: 0
- atomic overflow exposed: 0
- utilization p10: 0.9308
- utilization median: 0.9757
- utilization p90: 0.9955
- all-matrix non-final pages below 82% utilization: 6,319
- standard reading sizes (fontScale <= 1.30) below-82% ratio: 0.001740 (0.1740%)
- 780/915dp at fontScale 1.0 below-82% ratio: 0.000000 (0%)
- standard-density gate <= 0.5%: PASS
- target-phone density gate <= 0.1%: PASS

## What this proves

The deterministic packing algorithm, under the current dense V5 geometry model, preserves authored pieces and fills almost all ordinary non-final pages densely across the stress matrix. It also surfaces rather than silently drops oversized atomic units.

## What this does NOT prove

It does **not** prove real Compose text measurement, OEM font metrics, IME/inset behavior, Android device rendering, actual Galaxy screen screenshots, or instrumentation tests. Those remain unverified until the Android/device test suite is actually executed. No such execution is claimed here.
