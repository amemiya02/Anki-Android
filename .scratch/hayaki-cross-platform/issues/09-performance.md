# Measure the daily study path on a repeatable baseline

Status: ready-for-agent
Type: AFK
User stories: 10, 13, 14, 20

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Provide repeatable release-build measurements for the existing app and the redesigned Core Loop so joint acceptance can detect regressions. Use the recorded pre-change Android revision for the baseline.

## Acceptance criteria

- [ ] On a documented Android device, record cold start, reveal latency, consecutive grading and scrolling the 100,000-Card browser.
- [ ] Record device, refresh rate, build revision, collection, run conditions, timing and missed-frame distributions.
- [ ] The same procedure can be replayed against a candidate. Any repeatable slowdown can be traced to the corresponding interaction.

## Blocked by

None - can start immediately
