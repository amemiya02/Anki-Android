# Hayaki 0.2.0 — shared brand and daily study across Android and iOS

Status: done

## Problem Statement

A Daily Reviewer moving from iPhone to Android encounters a different launcher icon, a purple palette and different actions on the Decks and Finished screens. On iPhone they can start all due Decks from the daily summary. On Android the summary starts the current Deck. Matching the colours alone would still leave the learner guessing how to do the same day's work. Existing Android learners also have collections, input preferences and night settings they rely on when upgrading.

The editions have used different public version sequences. A learner reporting a problem and a maintainer reproducing it need to know which builds belong to the same Hayaki release.

## Solution

Release Hayaki 0.2.0 with the smiling folded-card icon, the same ten Themes and a common Core Loop on both platforms. Open Decks, see what is due, choose Study All or the current Deck, reveal and rate Cards, and finish with a useful account of the session. Android expresses these actions with native Material 3 controls. Each edition builds independently and identifies the pair of tested revisions in the joint release notes.

## User Stories

1. As a Newcomer, I want to recognise the same smiling Card on the launcher and Welcome screen, so that I can identify Hayaki on either device.
2. As an Android learner, I want the face and folded corner to remain recognisable under my launcher's mask and monochrome setting, so that I can find the app at icon size.
3. As a Daily Reviewer, I want the same ten named Themes on both devices, so that I can select the look I recognise.
4. As a Daily Reviewer, I want to change Appearance independently of Theme, so that following the system or switching to Black preserves my chosen colours after relaunch.
5. As a night-time learner upgrading Android, I want my existing System setting to use Light by day and Black at night, so that I can continue my usual study routine.
6. As a learner using Paper or Midnight, I want its study background on ordinary Cards and the author's background on styled Cards, so that my Theme and my Card Templates both work.
7. As a Daily Reviewer, I want new, learning, due and rating labels to keep their meanings across Themes, so that I can read the queue and choose a rating confidently.
8. As a Daily Reviewer, I want Decks to show today's remaining work, reviews already done and the queue breakdown, so that I can decide whether to start a session.
9. As a Newcomer, I want the empty Deck list to offer a way to get started, so that I can distinguish an empty collection from a day with nothing due.
10. As a Daily Reviewer, I want Study All to take me through eligible top-level Decks, including their children, so that I can finish today's work from one entry point.
11. As a Daily Reviewer, I want to resume only my current Deck or exit Study All early, so that I choose when my session ends.
12. As a learner with nested Decks and long Chinese or Japanese names, I want to see which counts belong to each Deck, so that I can choose the right one on a narrow phone.
13. As a one-handed reviewer, I want Show Answer and the ratings in the same thumb area, so that I can review consecutive Cards without repeatedly shifting my grip.
14. As a Daily Reviewer, I want my enabled ratings, interval display and existing review inputs to work together, so that a button, gesture or hardware key records my intended answer once.
15. As a learner using media, formulae or a stylus, I want my Card Content and whiteboard to work in the new Card Frame, so that I can keep studying my existing Decks.
16. As a learner using large text, I want labels, intervals and actions to reflow, so that I can rate a Card and reach Done at 200% text size.
17. As a TalkBack user, I want the Deck, counts and available actions announced in reading order, so that I can complete a session independently.
18. As a Daily Reviewer, I want Finished to show how many reviews I completed, how long I spent and how I rated them, so that I can understand this session.
19. As a Daily Reviewer, I want Finished to distinguish a cleared queue, a later learning Card, a daily limit and buried Cards, so that I know when to return or what I can do now.
20. As a learner on a tablet or foldable, I want the Card and controls to remain within a readable width, so that reviewing is comfortable on a wide display.
21. As an existing Android learner, I want to install 0.2.0 over my signed Hayaki build and reopen my collection, so that the new version preserves my study history and preferences.
22. As a learner reporting a problem, I want both editions to display the joint release version and identify platform-specific fixes, so that I can tell which release notes apply.
23. As a maintainer, I want one source for brand assets and an exact revision for each released app, so that I can reproduce a mismatch and update either edition independently.

## Implementation Decisions

- **Brand ownership.** The iOS repository owns the brand manifest, icon master, transparent foreground and Theme definitions. Import Android resources from one recorded iOS commit. Generate adaptive foreground and background layers, a monochrome silhouette and legacy launcher assets from the selected artwork. Use checked-in resources for normal builds.
- **Theme and Appearance.** Offer Hayaki, Sakura, Matcha, Ocean, Indigo, Wisteria, Amber, Sumi, Paper and Midnight. Default to Hayaki, with light accent `#2C776E` and dark accent `#7ED6CA`. Pair each Theme with System, Light, Dark or Black. Paper and Midnight supply their light and dark default study canvases. Black uses a pure black default canvas. A Card Template's own styling takes precedence for its Card Content.
- **Android Foundation.** Resolve the selected Theme across Compose, existing Android views and Anki Pages. Keep new/learning/due blue/red/green and Again/Hard/Good/Easy red/amber/green/blue, accompanied by labels or accessible names. Use Kotlin, Jetpack Compose and stable Material 3 components for Native Surfaces. Retain platform navigation, system fonts, touch feedback and existing Engine behaviour.
- **Preference upgrade.** Preserve the day/night policy and saved night palette independently, including an inactive night choice while Light is selected. Run the existing historical preference conversion before interpreting reused preference values. Existing learners receive the Hayaki Theme alongside their preserved Appearance.
- **Deck selection.** Keep Decks, Browse, Stats and More as the main destinations. Give Study All priority when work is available and keep a direct route to the eligible current Deck. Show nonzero queue counts on Deck rows and all three queue categories in the daily summary. After a Deck finishes and its completion screen closes, refresh eligibility before advancing to the next top-level Deck. Honour an explicit early exit. The Engine determines eligibility, counts and scheduling.
- **Review and Finished.** Centre the Card Frame at a readable width on wide layouts. Keep reveal and rating controls in the same bottom area, preserve two- or four-rating configurations and interval visibility, and allow two-column ratings at large text sizes. Show session figures when reviews occurred. For an already-finished Deck, show the reason and applicable actions directly. Preserve next learning time, Unbury Cards, Custom Study and Deck Options where applicable.
- **Public release.** Use 0.2.0 in joint release notes, tags, installed version labels and package metadata. Build numbers advance independently. Keep the Android application ID and signing identity and advance the effective version code for each shipped ABI. Align the iOS app and extensions. Joint release notes record both tested commits and explain Android's transition from its inherited version sequence. Single-platform hotfixes may ship separately with their platform identified.

## Testing Decisions

The primary acceptance test starts at the installed apps' normal entry points. Import a fresh copy of the same study collection on each platform, align the study day and starting queue, then perform the same ratings. Compare the Deck selected, available actions, queue changes and Finished summary. Assert what the learner sees, hears and can do. Use platform-specific screenshots to inspect layout and check session duration against the time spent on each device.

| Scenario | Observable pass condition | Existing test support |
| --- | --- | --- |
| Decks through Finished | With two eligible top-level Decks and a nested child, Study All advances on completion, refreshes changed eligibility and covers each due Card according to the Engine. Early exit returns to Decks. Starting only the current Deck ends there. Returning to Decks shows updated counts. | Shared iOS study fixture and deterministic queue seeds, iOS navigation and study UI tests, Android Deck list and reviewer tests. |
| Review and queue boundaries | Reveal shows the configured ratings and intervals. A single grade produces one review. Check media, formulae, styled backgrounds, whiteboard and hardware input. Exercise done, learning later, daily limit and buried cases, including opening Finished with zero reviews. | Existing iOS study-session, answer-bar and Finished tests, Android reviewer instrumentation and Card Frame and Finished screenshot suites. |
| Brand and preferences | Compare all ten IDs, accents and default canvases with the pinned iOS source. Inspect adaptive, legacy and monochrome icons at launcher size. Change Theme and Appearance, relaunch, and check Compose, a legacy surface and an Anki Page. Verify System with Black overnight, forced Light with a saved night palette, and legacy Plain and E-ink preferences. | iOS appearance tests, Android theme resolution, preference-upgrade and settings navigation tests, rendered-screen harness. |
| Accessibility and layout | Complete the Core Loop with TalkBack, 200% text and reduced motion. Check labels, focus order, contrast, 48 dp targets, RTL mirroring and long Deck names in English, Simplified Chinese and Japanese. Exercise phone and wide layouts in light, dark and Black. | Android Roborazzi captures and accessibility checks, device walkthrough, iOS large-type and iPad reference captures. |
| Responsiveness | Capture a baseline before changing the Core Loop. Repeat cold start, reveal, consecutive ratings and scrolling a 100,000-Card browser on the same Android device under the same conditions. Compare timings and missed-frame distributions. Investigate repeatable slowdowns before sign-off. | Existing Android Macrobenchmark and Baseline Profile tooling and the prior large-collection scrolling fixture/procedure. |
| Installation and release | Upgrade over a previously signed Android build. Verify collection, review history, preferences and effective ABI version codes. Inspect installed versions and iOS extension metadata, then match the joint notes to the two tested revisions. | Android release metadata, signed upgrade walkthrough, iOS project-generated app and extension metadata. |

Record device, refresh rate, locale, Appearance, fixture and app revisions with the evidence. For a bug fix, first reproduce the failure at the highest applicable existing test entry point, then verify the same test passes with the fix.

## Out of Scope

Later Milestones cover the Browse and editor flows, wide-screen browsing, sync progress and retry, import, remaining settings, widgets and notifications. Their current entry points remain available. Android Statistics and Deck Options receive Harmonisation through the shared Foundation. Rebuilding Statistics as native charts, changing its deck filter or PDF export, changing the Engine, and redesigning template-authored Card Content are outside 0.2.0.

## Further Notes

The reference pair used for design review on 2026-11-20 was Android `5336449995` and iOS `fbb380a`. Release notes will identify the revisions that actually pass acceptance. The 0.2.0 release gate covers the icon, all ten Themes, preference migration, the complete Core Loop, accessibility, consecutive-rating performance and installation checks above. [Shared product decision](../../docs/adr/0007-shared-hayaki-product.md) · [Android Quality Bar](../../PRODUCT.md) · [iOS study fixture at the design baseline](https://github.com/amemiya02/Hayaki-iOS/blob/fbb380a/Packages/CollectionStore/Sources/CollectionTesting/Fixtures/collection.colpkg)

## Release acceptance

All eleven implementation and acceptance tickets ([#2](https://github.com/amemiya02/Hayaki/issues/2) through [#12](https://github.com/amemiya02/Hayaki/issues/12)) are complete. Android and iOS share the folded-card brand, ten Themes, public version 0.2.0 and the verified daily study flow. The tagged pair is Android `0ca354386eea699c91a8c34d86bb46084081217d` (universal version code 22600300) and iOS `cc342214d5a1a60ae25363be1d4599228e97cc89` (build 7). [Tested releases and evidence](https://github.com/amemiya02/Hayaki/blob/redesign/docs/releases/0.2.0.md) · [Android tag](https://github.com/amemiya02/Hayaki/tree/v0.2.0) · [iOS tag](https://github.com/amemiya02/Hayaki-iOS/tree/v0.2.0)

The implementation is available for review in [Hayaki Android #13](https://github.com/amemiya02/Hayaki/pull/13) and [Hayaki iOS #61](https://github.com/amemiya02/Hayaki-iOS/pull/61).
