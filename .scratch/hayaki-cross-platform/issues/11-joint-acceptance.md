# Verify the two release candidates together

Status: ready-for-human
Type: HITL
User stories: 1–23

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Use the same study collection on both installed release candidates and record evidence for the pair. Use the prepared release notes and versions to identify the builds that pass.

## Acceptance criteria

- [ ] Align the study day and starting queues, perform the same ratings, and compare the complete Decks → Study All → reveal → rate → Finished path, counts and applicable actions.
- [ ] Inspect the launcher and theme choices and repeat the Android Core Loop with TalkBack, 200% text and reduced motion. Check phone and wide layouts, English, Simplified Chinese, Japanese and RTL mirroring.
- [ ] Upgrade over the previous signed Android build and verify collection, history and preferences. Repeat the performance measurements on the baseline device and investigate repeatable slowdowns.
- [ ] Record evidence, tested revisions and build numbers for both editions. Verify release notes and joint release tags identify the tested 0.2.0 pair before marking release acceptance complete.

## Blocked by

- [01 · Recognise Hayaki from launcher to Welcome](01-launcher.md)
- [02 · Choose and keep a Hayaki Theme across screens](02-themes.md)
- [03 · Keep existing night preferences after upgrading](03-upgrade-appearance.md)
- [04 · Apply Paper and Midnight to the study canvas](04-study-canvas.md)
- [05 · Read today’s workload and start the current Deck](05-daily-decks.md)
- [06 · Study across eligible Decks and stop on exit](06-study-all.md)
- [07 · Reveal and rate Cards from the same thumb area](07-review-controls.md)
- [08 · Finish a session with its results and next action](08-session-summary.md)
- [09 · Measure the daily study path on a repeatable baseline](09-performance.md)
- [10 · Expose 0.2.0 while preserving the install upgrade path](10-release-version.md)
