# 45: First run for a Newcomer

**What to build:** Someone opening the Fork for the first time sees a designed welcome that offers clear ways to begin.

**Blocked by:** 41 (Mockups for the rest)

**Status:** ready-for-agent

- [x] The first-run screen offers to start with an empty collection, to import a collection or Deck, or to set up sync with a server.
- [x] Each choice leads to a working next step.
- [x] It does not appear again once the owner has begun.
- [x] It matches the agreed Mockup.
- [ ] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [x] A collection or a Deck exported from the Official App or desktop Anki can be imported from the first-run screen, keeping scheduling, review history and media.
- [x] A person arriving from AnkiWeb is shown the steps to bring their Decks in through the Official App or desktop Anki.

## Implementation

- The Compose welcome uses the approved first-run layout. Start with no Decks opens Decks, Import opens Android’s package picker, and Sync with your own server opens the same address and sign-in flow used from Decks.
- A choice to start, import or set up sync finishes the welcome. AnkiWeb owners can read how to sync in the Official App or desktop Anki, export a collection package with media and choose that file. The guide keeps its place when the activity is recreated.
- Existing installs retain the original introduction preference. Installs with a recorded app version also go straight to Decks.

## Verification

The previous first-launch screen failed the self-hosted sync choice check on Android 15. The rebuilt Play debug APK passed the same check and opened the self-hosted setup. Start with no Decks showed the empty collection. The AnkiWeb guide displayed all three steps and opened Android’s package picker. The tested choices led to Decks on the next launch. [Android 15 screenshots](../evidence/ticket45-first-run.jpg)

Two fresh launches imported packages exported by Official AnkiDroid 2.25.3 through the welcome’s Import action and Android’s Downloads picker. The Deck import with learning progress preserved 13 Cards and Notes and two review rows. The collection import preserved 42 Cards and Notes and 11 review rows. Calendar due dates and the SHA-256 of `m3-field.png` matched for both imports. For the Deck comparison, day-based due values were compared relative to each collection’s creation day.

The focused JVM tests and rendered accessibility matrix are queued. The issue remains open for those results.
