# Measure the daily study path on a repeatable baseline

Status: ready-for-agent
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
