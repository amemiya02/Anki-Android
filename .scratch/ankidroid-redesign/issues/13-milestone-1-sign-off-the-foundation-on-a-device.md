# 13: Milestone 1 sign-off: the Foundation on a device

**What to build:** The owner sees the Foundation running on a device and agrees it is done before any Core Loop work starts.

**Blocked by:** 01 (Give the Fork its own identity); 02 (Switch off crash reports and usage statistics); 03 (Drop AnkiWeb sign-in and sync); 08 (Foundation: bundled typefaces and the type scale); 09 (Apply the Fork's name and launcher icon); 10 (Sweep hard-coded colours and text sizes out of layouts); 11 (Move interface icons to one Material Symbols style); 12 (Sweep hard-coded colours out of illustrations and graphics)

**Status:** ready-for-human

- [x] The Fork is installed on the emulator beside the Official App, with a copy of a collection brought in by import.
- [x] The "before" walk (deck list, drawer, both study screens, finished screen, a dialog, one Anki Page) is repeated and captured in English and Simplified Chinese, in all three themes. Both study screens still exist at this point.
- [ ] No capture shows two looks at once.
- [ ] Baseline pictures for the rendered-screen tests are regenerated and committed.
- [ ] The owner has signed off the Milestone.
- [ ] Round trip with the Official App: a collection and a Deck exported from the Fork import into the current release of the Official App with Cards, scheduling, review history and media intact, and the same works in the other direction.

## Comments

Device part done on `ticket/13` (Pixel 9 emulator, Android 16, debug build `com.ichi2.anki.redesign.debug`). Pictures: `docs/redesign/milestone-1/` (`<lang>-<theme>-<surface>.png`, one `overview-en.png` and `overview-zh.png` per language: rows Light, Dark, Black; columns deck list, drawer, backup dialog, finished screen, old study screen question and answer, new study screen question and answer, import Anki Page).

**Verified on the device**

- Installed beside Upstream's debug build `com.ichi2.anki.debug` (standing in for the Official App, which was not installed). Upstream's app kept its install time and still opens with its own decks. The launcher shows "AnkiDroid Redesign" with the Hayaki icon next to Upstream's icon (`launcher.png`); the app switcher shows the Hayaki icon (`app-switcher.png`).
- The collection came in by import. A `.colpkg` was exported from Upstream's app through its own export screen, with media and scheduling. Opening it as a file offered both apps (`open-package-chooser.png`). The Fork replaced its empty collection, and the decks and counts match Upstream's (30 cards due). The sample collection has no media files, so media was not tested.
- The walk was repeated in English and Simplified Chinese, in Light, Dark and Black, with both study screens (switched with `newReviewerOptions`).
- The checks deferred from tickets 01, 02, 03 and 09 are recorded under each ticket's Comments. Confirmed on the device: the sync button leads to sync setup; there are no sign-up or password links; the first-run screen offers import; a forced crash sends nothing; shared text and package files resolve to the Fork. Still open there: self-hosted sync end to end (no server was set up), a release build on the device (to see the name Hayaki), a real notification, the themed launcher icon, and one unexplained burst of network traffic (ticket 02).

**Problems seen in the captures (why "No capture shows two looks at once" stays open)**

- Old study screen, every theme: the area behind the gesture bar is a cinnabar band, while the "Show answer" bar above it is paper. In Light the "Show answer" label is paper-coloured on paper and cannot be read (`*-light-study-old-question.png`).
- Anki Pages keep Upstream's web look inside the Foundation's app bar. On the finished screen this means Roboto and a blue "custom study" link. On the import page it means a blue Import button and Roboto. Harmonisation is ticket 48 and the native finished screen is ticket 16. The owner should decide whether this counts as "two looks at once" for Milestone 1.
- First-run screen (Light): the status bar icons are white on paper and cannot be read (`en-light-first-run.png`).
- Card Content is not restyled, as intended: white in Light on the old study screen and grey in Dark.
- Drawer: Hayaki's mark in the header sits under the status bar clock.

**Baseline pictures:** the rendered-screen test baselines are not kept in git. Roborazzi writes them to `AnkiDroid/build/outputs/roborazzi/`, which `AnkiDroid/.gitignore` (`/build`) ignores, and the harness notes say baselines are build outputs. So none were regenerated or committed. That criterion needs the owner to decide whether baselines should be committed, or to reword it.

**Awaiting the owner**

- Look at the two overview sheets and decide on the problems above, then sign off.
- Round trip with the Official App: the Official App was not installed, so this was not done.

**Fixes after the walk** (branch `fix/m1-device`, one commit each, each with a regression test that failed first):

- Old study screen: the strip under the answer buttons (`showAnswerColor`) was the Foundation's primary (the cinnabar band behind the gesture bar) and the "Show answer" label was onPrimary (paper on paper in Light). Both now follow the page: the strip is paper like the buttons, the label is ink, and the navigation bar icons follow the theme instead of always being light. No restyle otherwise (ADR-0004). Test: `ReviewerLegibilityTest` (label contrast on its rendered button, strip colour under the navigation bar, navigation bar icons; Light, Dark, Black).
- Light status bar: the first-run screen and seven more screens still asked for light status bar icons in every theme, a leftover of Upstream's coloured app bars: study options, card browser (without the search view), Note Type fields, card template editor, browser appearance, about and shared decks. All now follow the theme. Test: `StatusBarFollowsThemeTest` starts every screen in `ActivityList` in Light, Dark and Black and checks the status bar icon appearance (69 cases).
- Drawer header: the header was deliberately not inset, so Upstream's banner drew under the status bar; Hayaki's mark then sat under the clock. The header now adds the status bar's height to its top padding. Test: `NavigationDrawerInsetsTest`.
- Checked with the harness in Light, Dark and Black (`ReviewerScreenshotTest` question and answer, `NavigationDrawerScreenshotTest`, `IntroductionScreenshotTest`). The harness does not draw the system's status bar icons, so the status bar fix rests on the test above and still wants a look on a device.
- The unexplained network traffic is investigated under ticket 02's Comments.
