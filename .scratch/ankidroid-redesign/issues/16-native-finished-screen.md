# 16: Native finished screen

**What to build:** When a Deck has nothing left to study today, the owner sees a designed finished screen that says what happens next and offers the ways to continue.

**Blocked by:** 14 (Mockups for the Core Loop)

**Status:** done

- [x] It opens when a study session runs out of Cards and when a finished Deck is tapped on the deck list.
- [x] It says when more Cards become available, and whether a daily limit is why the session ended.
- [x] When Cards are buried, it offers to unbury them, and doing so returns to studying.
- [x] Custom study and Deck options are reachable from it.
- [x] One tap returns to the deck list.
- [x] It shows everything the Anki Page it replaces showed, for a normal Deck and for a filtered Deck.
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Comments

Built as `FinishedFragment` / `FinishedScreen` (Compose); the `congrats` Anki Page (`CongratsPage`) is gone. Mockups approved in advance by the owner on 2026-11-17 (ticket 14).

- **Opens**: on every study session that runs out of Cards (the developer option `new_congrats_screen` that gated it is removed), when a finished Deck is tapped, and from the newer study screen itself when no caller awaits its result (a shortcut, or studying started from the finished screen). Tablets' study options panel keeps its own finished state.
- **Says**: leads with one reason (daily limit > learning later today > buried > done) and lists every paragraph the page showed, in the Engine's words: learning Cards later today and when the next is ready, the new and review limit messages, the buried-Cards message, "Congratulations! ...", the filtered-Deck explanation, and the Deck description. Added: when the next day's Cards arrive (from the Engine's day cutoff) and today's study summary (Engine's `studiedToday`).
- **Deliberately different from the Mockup**: body text is the Engine's own sentences (already translated) rather than the Mockup's shorter wording, e.g. "One or more cards were buried, and will be shown tomorrow..." for "Some related or buried cards were held back...". Today's summary is the Engine's "Studied N cards in M minutes today (Xs/card)". A filtered Deck shows no Custom study row (the page offered none) and its Deck options row opens the filtered Deck's options.
- **Unbury** now opens the study screen straight away (the page went to study options). Custom study results and a refilled filtered Deck still open study options, as before.
- **Dropped**: the page's once-a-minute refresh of "the next card will be ready in N minutes"; the screen re-reads on every return to it and on queue changes. A timer loop also hung the tests (virtual time never settles).
- **Quality Bar**: `FinishedScreenScreenshotTest` (later, limit, buried, done, filtered) passes `@CheckAccessibility` in Light, Dark, Black x en, zh-CN, ja, ar x 100%, 200% (120 pictures), compared with `docs/redesign/core-loop/finished-*.png`. Under Robolectric the Engine's strings are English in every language. On-device check of both entry points and of a real Chinese/Japanese Engine translation is left to the Milestone sign-off (ticket 24).
- **Tests**: `CongratsViewModelTest` unchanged and passing; new `CongratsViewModelFinishedStateTest`; `CongratsPageTest` became `FinishedFragmentTest` (web-page polling tests dropped with the page); `ReviewCompletionTest` now always expects the finished screen.
