# Product

<!-- impeccable:product-schema 1 -->

Terms in capitals (Deck, Card, Native Surface, Core Loop, Engine and the rest) are defined in [GLOSSARY.md](GLOSSARY.md). Decisions cite the records in [docs/adr/](docs/adr/).

## Platform

android

## Users

**Primary: the owner.** A Daily Reviewer who studies Anki Decks on an Android phone most days, in Simplified Chinese, Japanese and English. Product decisions serve that daily use first.

**Others are welcome.** They are Daily Reviewers who want the same thing, and Newcomers who open the app with no Decks. Many keep the Official App (AnkiDroid) installed alongside. How these people get Hayaki is undecided (see Capabilities and Constraints), so no surface may assume a store, a download page or a support channel.

**The job.** Open the app, see what is due, study it and finish, every day and often for a long session. Less often: find a Card, add or edit a Note, adjust settings, or move a collection in or out.

## Product Purpose

Hayaki brings an Anki collection to Android. You review due Cards offline, look up and edit Notes mid-session, and see review history and upcoming workload. It is a Fork of AnkiDroid with the whole interface rebuilt: a Reskin, Restructure and Reflow taken together.

It exists because AnkiDroid's interface was incoherent for someone studying daily in three scripts. The study screen showed three unrelated colours at once. Chinese and Japanese text was set in a heavy system font. Destinations used every day sat in a side drawer. Dialogs and settings looked like separate apps. There were five themes and none looked designed. Upstream accepts only small single-purpose changes and has no design direction, so it could not fix this ([ADR-0001](docs/adr/0001-own-fork-no-upstreaming.md)).

Success means:

- the owner chooses Hayaki for daily study;
- every Native Surface reads as one product;
- nothing relied on in AnkiDroid is lost;
- the collection can always return to Anki or AnkiDroid intact.

**Current state.** The Redesign finished with all five Milestones. The owner signed off on 2026-11-19 ([Milestone 5 evidence](docs/redesign/milestone-5/README.md)). Future work refines this app under the same rules.

## Positioning

Three claims, confirmed by the owner, that AnkiDroid cannot truthfully make:

- **A calm, coherent interface.** One considered look covers every Native Surface, built to stay restful through long daily sessions. There are no leftover screens from another era and no second study screen. Since [ADR-0006](docs/adr/0006-direction-material-3-standard.md) that look is the Material 3 standard, finished to the level of Google Calendar, Keep and Tasks.
- **Three scripts read as one.** Interface text in Chinese, Japanese and Latin script uses the system sans-serif in the interface language, so Japanese text gets Japanese glyph forms and Chinese text gets Chinese ones. Hierarchy uses size and weight only, so emphasis survives in scripts that have no italics or capitals.
- **A shorter daily path.** On phones a bottom bar holds Decks, Browse, Stats and More, replacing the drawer. Each step of the Core Loop (choose a Deck, study its Cards, reach the finished screen) is one tap. The finished screen says when to come back.

Hayaki follows the upstream Engine for collection storage, scheduling and sync.

## Operating Context

- **Phone, one thumb.** You answer Cards one-handed. Gestures, tap zones, hardware keys, controller bindings, the stylus whiteboard and voice recording all carry over from AnkiDroid.
- **Day and night.** Sessions run in daylight and in the dark. Light, Dark and Black (true black for OLED) are offered. The app follows the system's light or dark setting by default; Black is an explicit choice.
- **Mixed-language content.** Decks are in Chinese, Japanese and English. Deck names in Chinese and Japanese can be long and must not push the counts out of view.
- **Large collections.** The card browser has to stay smooth at 100,000 Cards.
- **Tablets and foldables.** They keep the drawer and two-pane layouts, reskinned. A bespoke large-screen design is deferred.
- **Beside the Official App.** Hayaki has its own app ID and its own data. Data moves in three ways:
  - A Deck package (`.apkg`) merges into the other app's collection.
  - A collection package (`.colpkg`) replaces the other app's collection.
  - AnkiWeb or a self-hosted Anki sync server.
- **Arriving from AnkiWeb.** Choose Sync on Welcome and sign in with the existing AnkiWeb account. File import is available on the same screen.
- **Capturing from other apps.** Text shared from a browser or dictionary opens quick Add or the full editor. Afterwards the person returns to the app that shared it.
- **Beyond the app.** Home-screen widgets (Glance) and review-reminder notifications.
- **Anki Pages.** Statistics, Deck options, Card info, the importers, Change Note Type and image occlusion are web pages shared with desktop Anki and shown inside the app.

## Capabilities and Constraints

**Hard limits (confirmed binding after sign-off):**

- **The Engine is never modified.** Collection storage, scheduling, sync and the Anki backend stay Upstream's code. Collection format, sync compatibility and scheduling behaviour stay identical to Upstream. Engine updates come in when sync requires it or a few times a year ([ADR-0001](docs/adr/0001-own-fork-no-upstreaming.md), [ADR-0003](docs/adr/0003-interface-rebuilt-natively-in-compose.md)).
- **Anki's words are used verbatim** in every language: Note, Card, Deck, Note Type, Card Template and their established translations. Friendlier substitutes such as "flashcard", "set" or "model" are never used. GLOSSARY.md is the vocabulary for code, tests, tickets and copy.
- **No existing feature or setting is removed.** The only exceptions are explicit "drop" decisions by the owner. Defaults may change.
- **Non-UI work happens only in passing.** Speed fixes, bug fixes, defaults and small features are taken on only when met on a surface already being worked on. They are never sought out.

**Data and sync:**

- **Sync destination.** AnkiWeb is the default for a new profile. Existing self-hosted profiles keep their server. Each profile uses one destination for its collection and media. See [ADR-0001](docs/adr/0001-own-fork-no-upstreaming.md).
- **Data can always leave.** A whole collection or a single Deck, with scheduling, review history and media, must round-trip with the current Official App. The export option that older versions can read stays easy to find. The export screen says whether importing will replace or merge.
- **No crash reports or usage statistics** are sent anywhere.
- **Apps that integrate with the Official App's public API do not reach Hayaki.** This is accepted. Ordinary Android text and file sharing does reach it.

**Interface boundaries:**

- **Card Content is rendered exactly as its Card Template styles it.** The interface never restyles Card Content or the default Card Templates. Bundled typefaces apply to the Card Frame and the rest of the interface only.
- **Anki Pages receive Harmonisation only.** They get the app's colours and typeface; their content and arrangement are untouched. The finished screen is the one Anki Page rebuilt natively.
- **There is one study screen** ([ADR-0004](docs/adr/0004-one-study-screen-port-then-remove.md)). It carries the AnkiDroid JavaScript API, speech, whiteboard saving, the Deck title and the time-remaining estimate.

**Stack:**

- Every Native Surface is Jetpack Compose with Material 3, in a single activity with Compose navigation. Widgets use Glance. There is no dependency-injection framework ([ADR-0003](docs/adr/0003-interface-rebuilt-natively-in-compose.md)).
- Mockups are disposable web pages in `mockups/` (React, Tailwind, shadcn/ui). Only design values carry over from a Mockup to Android.
- Android 7.0+ (minSdk 24).

**Themes, fonts and icons:**

- **Themes:** Light, Dark and Black only, from a fixed palette with no wallpaper-derived dynamic colour. Earlier Plain and E-ink selections migrate to Light.
- **Fonts:** the system sans-serif stack (Roboto and the system's CJK font), chosen by interface language. No interface fonts are bundled since ADR-0006. Any font bundled later must be SIL Open Font License, because the source is public.
- **Icons:** one Material Symbols style throughout. Only the launcher icon is drawn specially.

**Languages:**

- English, Simplified Chinese and Japanese are first-class. Every new or changed string is written by the Fork in all three.
- Other locales (82 exist) keep Upstream's translations where the wording still fits. Strings the Fork adds fall back to English in those locales.
- One right-to-left language is checked on every surface.

**Undecided (deferred, not ruled out):**

- Distribution to other people: store release, release signing and support.
- Translating new strings beyond English, Simplified Chinese and Japanese.
- A bespoke tablet, foldable or desktop design.

## Brand Commitments

- **Name.** "Hayaki" is written the same way in every language. It never contains "AnkiDroid" and must not be mistaken for the Official App or for a product of Anki's makers. "AnkiDroid Redesign" is a development-build working name only and never reaches another person.
- **Launcher icon.** Icon A, at `docs/redesign/quiet-paper/launcher-icon.svg`. It never reuses AnkiDroid's or Anki's logo.
- **Provenance.** Hayaki credits AnkiDroid and its contributors and states its licence, GPL-3.0-or-later. It links to its public source at `github.com/amemiya02/Hayaki`. The licence requires source to be offered with any build given to someone else.
  - *Open:* ADR-0001 and the spec require the About screen to call Hayaki an unofficial fork of AnkiDroid. Since commit `3cb77115cd` it shows a "Credits" heading linking to AnkiDroid and its contributors, with no explicit "unofficial fork" wording. Whether that meets the commitment is the owner's call.
- **Direction.** On 2026-11-19 the owner replaced Quiet paper with the Material 3 category standard, played straight ([ADR-0006](docs/adr/0006-direction-material-3-standard.md)). The craft bar is Google Calendar, Keep and Tasks. The owner wants it refined, clean and fluid, with no quirks smuggled in. DESIGN.md records the visual system; this file does not restate it.
- **Voice** (seen in shipped copy, not separately confirmed). Copy uses short plain sentences in the second person. It says what will happen, especially when data can be replaced ("This phone keeps its collection."). Instructions name exact menu paths. There are no exclamation marks and no cheerleading. The last copy pass, `3cb77115cd`, simplified wording further.

## Evidence on Hand

- **Current screenshots:** `docs/images/` holds the deck list, the study screen with the answer revealed, and the card browser, each in English and Simplified Chinese.
- **Before the Redesign:** `docs/redesign/before/`.
- **Directions and Mockups:**
  - The three Directions compared: `docs/redesign/directions/`.
  - The Mockup project: `mockups/`.
  - Per-area Mockup decisions: `docs/redesign/core-loop/`, `finding-and-adding/`, `settings-and-dialogs/` and `the-rest/`.
- **Device evidence:**
  - Each Milestone's record is in `docs/redesign/milestone-1/` through `milestone-5/`.
  - The final walkthrough captured 97 screen states in each of nine profiles (English, Simplified Chinese and Japanese, in Light, Dark and Black). It also compared Hayaki with the Official App 2.25.3 (`docs/redesign/milestone-5/gallery/`).
- **Data round trip:** both apps imported a Deck and a collection exported by the other. Cards, Notes, scheduling, review history and media matched (`docs/redesign/milestone-5/round-trip/`).
- **Test results:** the final regression run at `b0a0715495` had zero failures. The app suite reported 4,192 passes across 533 test classes.
- **Decisions:** `docs/redesign/milestone-5/decisions.md`, plus the issue files in `.scratch/ankidroid-redesign/`.
- **Absent, and not to be invented:** users other than the owner on record, testimonials, reviews, a store listing, download counts, press, pricing, and a distribution channel.

## Product Principles

1. **Daily study comes first.** The Core Loop gets the most care. Anything that slows or interrupts the owner's daily session is a defect, whatever else it adds.
2. **Card Content leads; the Card Frame recedes.** The interface frames what the person is learning and never competes with it or restyles it.
3. **Three scripts, one standard.** A surface is not finished until it holds up in English, Simplified Chinese and Japanese. Design emphasis must not depend on italics, capitals or letter-spacing.
4. **Nothing relied on is lost.** Engine parity, every feature and setting, and the way back to Anki stay intact. A removal is a deliberate owner decision, never a side effect.
5. **Speak Anki's language plainly.** Use Anki's own words and say what an action will do before it does it, above all when data can be replaced.

## Accessibility & Inclusion

Every Native Surface must pass the Quality Bar:

- text contrast meets WCAG AA in Light, Dark and Black;
- touch targets are at least 48dp;
- the layout mirrors correctly in a right-to-left language;
- the surface stays usable at 200% system font size;
- every control has a screen-reader label;
- the surface has been checked in English, Simplified Chinese and Japanese.

The rendered-screen test harness (Roborazzi, `ScreenshotTest` and `ComposeScreenshotTest`) varies theme, language and font size. Its capture-time accessibility check enforces contrast and touch-target size mechanically.
