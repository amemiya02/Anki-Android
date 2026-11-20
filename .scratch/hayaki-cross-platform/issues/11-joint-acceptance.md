# Verify the two release candidates together

Status: done
Type: HITL
User stories: 1–23

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Use the same study collection on both installed release candidates and record evidence for the pair. Use the prepared release notes and versions to identify the builds that pass.

## Acceptance criteria

- [x] Align the study day and starting queues, perform the same ratings, and compare the complete Decks → Study All → reveal → rate → Finished path, counts and applicable actions.
- [x] Inspect the launcher and theme choices and repeat the Android Core Loop with TalkBack, 200% text and reduced motion. Check phone and wide layouts, English, Simplified Chinese, Japanese and RTL mirroring.
- [x] Upgrade over the previous signed Android build and verify collection, history and preferences. Repeat the performance measurements on the baseline device and investigate repeatable slowdowns.
- [x] Record evidence, tested revisions and build numbers for both editions. Verify release notes and joint release tags identify the tested 0.2.0 pair before marking release acceptance complete.

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

## Verification record

Both device tests import the same collection package (SHA-256 `63ba3eba14a180e13bcb50300009e0e9d2961e8cee93a193af40facf95bf1612`). They begin at Decks, choose Study All, grade Geography Easy until Finished, use Done to continue to the next eligible Deck, and exit that Deck with work remaining. The fixture contains 8 notes, 9 cards and 4 earlier reviews. [Android scenario and fixture provenance](../../../AnkiDroid/src/androidTest/assets/hayaki-release/README.md) · [iOS scenario](https://github.com/amemiya02/Hayaki-iOS/blob/cc342214d5a1a60ae25363be1d4599228e97cc89/UITests/StudyUITests.swift)

The initial ARM64 performance capture used a separate 100,000-card fixture with SHA-256 `98521cda65c48d402271722a92a45f2ffdc610a7bc4d45237922e46b7ee2db4f`. Its pre-change ARM64 release at `5336449995` has APK SHA-256 `7c0ba4df1453fbe2b7ba32393aa71e65ab89e064a28f68210eb6d6fe38f39a38`. The final hosted baseline and signed release used the same 100,000-card package on that runner, SHA-256 `10148ad4f78ccedcb08a4054646935999bf94a2068b7b128c348f472b7a7b935`. [Measurement procedure](../../../tools/core-loop-benchmark/README.md) · [Paired results and traces](https://github.com/amemiya02/Hayaki/actions/runs/44748691056/artifacts/14635237868)

The Android skin imports ten theme IDs, twenty accent colours and four Paper/Midnight canvas colours from the pinned iOS sources. All twenty-four values agree with iOS. Regenerating the Android resources produced a clean diff, and the Foundation generator passed its text contrast checks. [Pinned iOS palette](https://github.com/amemiya02/Hayaki-iOS/blob/6c04596ceb4942eb21a540c16130a6bbe0290772/Packages/HayakiAppearance/Sources/HayakiAppearance/Theme.swift)

On 2026-11-20 the maintainer approved the Android launcher artwork, Welcome screen, English phone study flow and Simplified Chinese wide-screen flow at 200% text after viewing the installed-device captures. [Launcher captures](https://github.com/amemiya02/Hayaki/actions/runs/44741108170) · [Study captures](https://github.com/amemiya02/Hayaki/actions/runs/44738659025/artifacts/14631070983)

The signed Android ARM64 build completed the fixture on phone and wide displays with the Google TalkBack service, 200% text and all animation scales set to zero. Physical gestures revealed and rated Cards, reached Done, continued to the next Deck and returned to a refreshed daily summary. [Actual focus trace and replication steps](https://github.com/amemiya02/Hayaki/blob/redesign/docs/releases/0.2.0-talkback.md)

## Verification

The accepted pair is Android `0ca354386eea699c91a8c34d86bb46084081217d` (universal build 22600300) and iOS `cc342214d5a1a60ae25363be1d4599228e97cc89` (build 7). Both identify as 0.2.0. On Android phone and wide displays, iPhone and iPad, the shared fixture started at 5 new, 2 learning, 2 due and 0 studied. Three Easy ratings finished Geography. Done opened Languages and early exit returned to 3 new, 2 learning, 1 due and 3 studied. [Release notes and installed iOS screenshots](https://github.com/amemiya02/Hayaki/blob/redesign/docs/releases/0.2.0.md) · [Installed Android results](https://github.com/amemiya02/Hayaki/actions/runs/44748906084) · [Packaged iOS candidate](https://github.com/amemiya02/Hayaki-iOS/actions/runs/44755687099/artifacts/14638546672)

The maintainer approved the Android visuals. The signed release also completed the study path with actual TalkBack physical gestures at 200% on phone and wide displays. The in-place upgrade preserved the collection, review history, input bindings and appearance policy. The 100,000-card benchmark ran in both orders and recorded timing, frame deadlines and separate traces. [TalkBack observations](https://github.com/amemiya02/Hayaki/blob/redesign/docs/releases/0.2.0-talkback.md) · [Signed upgrade](https://github.com/amemiya02/Hayaki/blob/redesign/docs/releases/0.2.0-upgrade.md) · [Performance comparison](https://github.com/amemiya02/Hayaki/blob/redesign/tools/core-loop-benchmark/baselines/android-hosted-0ca35438.md)

The final iOS source passed the hosted phone and landscape iPad checks. A separate portrait iPad replay installed the archived app and verified all three Easy grades, Done, and the early exit. [iPhone XCTest](https://github.com/amemiya02/Hayaki-iOS/actions/runs/44755687099/artifacts/14640571286) · [iPad XCTest](https://github.com/amemiya02/Hayaki-iOS/actions/runs/44755687099/artifacts/14639617848)

All 210 cases in the final expanded Android suite passed on Linux with zero skips, including Info in Light, Dark and Black. A cold-start release installation also passed the localized Deck-count action. [JUnit XML](https://github.com/amemiya02/Hayaki/actions/runs/44761458950/artifacts/14642007113) · [Cold-start instrumented test](https://github.com/amemiya02/Hayaki/actions/runs/44760561739)

Both annotated v0.2.0 tags were created and read back from the personal repositories. Each annotation records both tested commits and links to the same immutable release notes. [Android tag](https://github.com/amemiya02/Hayaki/tree/v0.2.0) · [iOS tag](https://github.com/amemiya02/Hayaki-iOS/tree/v0.2.0) · [Tagged pair's release notes](https://github.com/amemiya02/Hayaki/blob/441c523ed1dd273750a6c82a957c89e242314448/docs/releases/0.2.0.md)
