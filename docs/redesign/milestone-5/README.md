# Milestone 5 device evidence

Hayaki and the Official App ran side by side on disposable Android emulators. Personal phone verification and the owner’s sign-off remain open at the owner’s request. [Milestone issue](../../../.scratch/ankidroid-redesign/issues/51-milestone-5-sign-off-the-redesign-is-done.md)

## Android walkthrough

The Android 15 walkthrough captured 97 screen states in each of nine profiles. It exercised the Core Loop, Browse, Note editing, Settings, sync, importing and exporting, Note Types, Card Templates, widgets, reminders and the other app-owned destinations. The archives include the full Android frame and the accessibility hierarchy for each capture. [Before and after gallery](gallery/README.md) · [Emulator baselines](baselines/README.md)

The comparison uses genuine Official App frames from version 2.25.3 and a few earlier Upstream walkthrough images. Related screens are labeled where they provide context for a different destination. Android and the Engine used the same language in each profile. Fixture data made the storage permission request, shared Deck download and review reminder observable. The bundled Anki Page checks use the Engine’s WebView routes. Browse and the Note Editor open the Engine’s Change Note Type page. [Original Upstream walkthrough](../before/) · [Anki Page verification](anki-pages/README.md)

The final review repeated the Decks, Browse, study, navigation, account and widget flows on Android 15. The nine profiles cover English, Simplified Chinese and Japanese in Light, Dark and Black. Each archived frame identifies its source APK. [Navigation verification](../../../.scratch/ankidroid-redesign/issues/50-finish-the-single-activity-structure-and-remove-leftover-xml.md) · [APK and frame digests](baselines/provenance.csv)

An external app shared text into Hayaki. Both quick Add and saving from the full editor returned to the sender. The full-editor check verified the saved Cloze text and exactly one new Note and Card. On a fresh install, opening or sharing a Deck package survived Welcome and Android’s storage permission screen. Opening a collection package through the same path restored its Decks. Converting a Note from Basic to Basic (and reversed card) and back through Browse and the Note Editor preserved its fields and the original Card’s ID and scheduling.

The Roborazzi test harness writes its rendered baselines under `AnkiDroid/build/outputs/roborazzi/`. [Milestone 1 baseline decision](../../../.scratch/ankidroid-redesign/issues/13-milestone-1-sign-off-the-foundation-on-a-device.md)

## Package exchange

Both apps imported a reviewed Deck and a collection exported by the other. The four receiver comparisons matched the exported Cards, Notes, scheduling, review history and media. [Captures and verification](round-trip/README.md)

## Decisions

The decision record covers the spec’s open questions and the Mockup choices approved by the owner. [Redesign decisions](decisions.md)
