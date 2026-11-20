# Measure the daily study path on a repeatable baseline

Status: done
Type: AFK
User stories: 10, 13, 14, 20

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Provide repeatable release-build measurements for the existing app and the redesigned Core Loop so joint acceptance can detect regressions. Use the recorded pre-change Android revision for the baseline.

## Acceptance criteria

- [x] On a documented Android device, record cold start, reveal latency, consecutive grading and scrolling the 100,000-Card browser.
- [x] Record device, refresh rate, build revision, collection, run conditions, timing and missed-frame distributions.
- [x] The same procedure can be replayed against a candidate. Any repeatable slowdown can be traced to the corresponding interaction.

## Blocked by

None - can start immediately

## Implementation evidence

The replay tool builds a disposable 100,000-Card fixture and captures Android release cold starts, Card reveals, consecutive ratings and Browser swipes. The recorded release `5336449995` ran on a Pixel 9 Android 15 AOSP emulator. The accepted baseline includes three cold starts, one five-Card Study pass and ten measured Browse swipes. The archive includes per-action timings, deadline distributions and raw Android output. A preliminary candidate replay used the same emulator and fixture. The signed final candidate and exact baseline ran on the same hosted emulator. In the candidate-first follow-up, the cold-start direction reversed and the p95 rating observation gap shrank from 254 to 11 ms. Independent cold-start and Study traces are linked from the comparison. [Replay instructions](../../../tools/core-loop-benchmark/README.md) · [Baseline evidence](../../../tools/core-loop-benchmark/baselines/android-5336449995.md) · [Local candidate replay](../../../tools/core-loop-benchmark/baselines/android-fb65dea3e.md) · [Final paired comparison](../../../tools/core-loop-benchmark/baselines/android-hosted-0ca35438.md)

## Verification

Both measurements used the same pair of release APKs and the same 100,000-card fixture. Median cold launch moved from 1,854 to 2,003 ms in baseline-first order and from 1,938 to 1,788 ms in candidate-first order. Browse frame p95 changed from 103.86 to 103.85 ms. In reverse order, the rating capture upper-bound p95 changed from 778.90 to 789.85 ms and rating frame p95 from 150.46 to 151.19 ms. The report distinguishes capture intervals from display latency and records missed deadlines, renderer waits and independent traces. [Measurements, raw reports and analysis](https://github.com/amemiya02/Hayaki/blob/redesign/tools/core-loop-benchmark/baselines/android-hosted-0ca35438.md) · [Baseline first](https://github.com/amemiya02/Hayaki/actions/runs/44748691056) · [Candidate first](https://github.com/amemiya02/Hayaki/actions/runs/44752886490)
