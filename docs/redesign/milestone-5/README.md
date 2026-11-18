# Milestone 5 device evidence

Hayaki and the Official App ran side by side on disposable Android emulators. Personal phone verification and the owner’s sign-off remain open at the owner’s request. [Milestone issue](../../../.scratch/ankidroid-redesign/issues/51-milestone-5-sign-off-the-redesign-is-done.md)

## Android walkthrough

The Android 15 walkthrough captured 97 screen states in each of nine profiles. It exercised the Core Loop, Browse, Note editing, Settings, sync, importing and exporting, Note Types, Card Templates, widgets, reminders and the other app-owned destinations. The archives include the full Android frame and the accessibility hierarchy for each capture. [Before and after gallery](gallery/README.md) · [Emulator baselines](baselines/README.md)

The comparison uses genuine Official App frames from version 2.25.3 and a few earlier Upstream walkthrough images. Related screens are labeled where they provide context for a different destination. Android and the Engine used the same language in each profile. Fixture data made the storage permission request, shared Deck download and review reminder observable. The bundled Anki Page checks use the Engine’s WebView routes. The Browse change flow covers the native Change Note Type editor. [Original Upstream walkthrough](../before/) · [Anki Page verification](anki-pages/README.md)

The captured builds span the single-activity migration. The final Decks, Add menu and overflow captures use the APK with the corrected loading indicator. In all nine profiles the indicator cleared after a theme change, and the Add button opened its menu above the navigation bar. The baseline index records the APK digest for each capture. [Navigation verification](../../../.scratch/ankidroid-redesign/issues/50-finish-the-single-activity-structure-and-remove-leftover-xml.md) · [APK and frame digests](baselines/provenance.csv)

The Roborazzi test harness writes its rendered baselines under `AnkiDroid/build/outputs/roborazzi/`. [Milestone 1 baseline decision](../../../.scratch/ankidroid-redesign/issues/13-milestone-1-sign-off-the-foundation-on-a-device.md)

## Package exchange

Both apps imported a reviewed Deck and a collection exported by the other. The four receiver comparisons matched the exported Cards, Notes, scheduling, review history and media. [Captures and verification](round-trip/README.md)

## Decisions

The decision record covers the spec’s open questions and the Mockup choices approved by the owner. [Redesign decisions](decisions.md)
