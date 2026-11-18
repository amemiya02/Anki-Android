# Milestone 4 device evidence

The 2026-11-19 settings walk and package exchange ran on a disposable Pixel 9 emulator with Android 15 AOSP. Both apps were installed side by side. The owner signed off on 2026-11-19. [Milestone issue](../../../.scratch/ankidroid-redesign/issues/40-milestone-4-sign-off-settings-and-dialogs-on-a-device.md)

| App | Package and version | APK SHA-256 |
| --- | --- | --- |
| Fork (`51386d6eeb`) | `com.ichi2.anki.redesign.debug` · `2.26.0alpha14-debug` · `22600114` | `43a5d58e2e117fa66082f6ee75f5dc0460634d099ef1cae96e4fc7a21e5333ce` |
| Official App | `com.ichi2.anki` · `2.25.3` · `322503300` | `68de13faf1819008946850aa7c7873f8d60f057ffa96dc2bb5b1ad1bade4df80` |

AnkiDroid 2.25.3 was the current stable release at verification. The installed APK matched the Official App used for Milestone 2. [Official release](https://github.com/ankidroid/Anki-Android/releases/tag/v2.25.3) · [Signing certificate](../milestone-2/README.md)

## Settings and dialogs

| Locale | Settings search | Light dialogs | Dark dialogs | Black dialogs |
| --- | --- | --- | --- | --- |
| English (`en-US`) | [Settings](en-settings.png) | [Light](en-light.png) | [Dark](en-dark.png) | [Black](en-black.png) |
| Simplified Chinese (`zh-CN`) | [Settings](zh-settings.png) | [Light](zh-light.png) | [Dark](zh-dark.png) | [Black](zh-black.png) |

1. Open Settings from More. Search for Theme, choose Dark, Black and Light, and check the selected tile in every palette.
2. Search for Fetch media on sync and open the result. Open the single-choice dialog, choose Never, check the setting's value and restore the starting value.
3. Create a temporary Deck with the input dialog and remove it after checking that it appears. Long-press English and open Export deck to see the multiple-choice options.
4. Open the collection package with the Fork to see the destructive replacement action, then cancel. Open the `.pdf` fixture with the Fork to verify the unsupported-file error and dismiss it.
5. Open More options, then Check and Check database. Capture the confirmation and accept it. The progress panels are frames from the real database check on a disposable 100,000-Card collection.

## Add button after changing theme

The initial build let the bottom bar cover part of the Add button after changing the theme from More. The regression replay searched for Theme, selected Black and Light, returned to More and opened Decks. On the original build the button ended at pixel 1483 while the bar started at 1420. With the fix, the button ended at pixel 1374 and kept the same 46-pixel gap as fresh Decks. Tapping Add and Create deck opened the input dialog. The patched APK SHA-256 was `c831c9be44f239af562e850e9651df481a59d9d569191063bb2b748b0d84d076`. The focused Android instrumentation regression passed as well. [Before and after](add-button.png)

## Rendered baselines

The Settings and dialog screenshot matrix generated baselines for 2,136 cases across 21 suites. The first record found 36 selector failures in localized Add profile tests. After the selectors took their labels from the active locale, all 96 cases in that suite passed recording. The broad comparison matched 2,112 cases. The remaining 24 Advanced cases rendered a changing Robolectric directory name. All 24 matched after the capture fixture supplied a fixed directory summary. The Milestone 1 decision keeps Roborazzi baselines under `AnkiDroid/build/outputs/roborazzi/`. [Milestone 1 decision](../../../.scratch/ankidroid-redesign/issues/13-milestone-1-sign-off-the-foundation-on-a-device.md)

## Round trip with the Official App

Both apps began with the same disposable collection. The Fork graded two English Cards before export. The Official App merged the English Deck and then replaced its collection. After grading two more English Cards in the Official App, the same merge and replacement ran in the Fork. Both Deck exports included scheduling information, Deck presets and media. Both collection exports included media. All four exports used the current package format.

| Direction | Export | Cards | Notes | Review entries | Media files |
| --- | --- | ---: | ---: | ---: | ---: |
| Fork → Official App | English Deck | 13 | 13 | 4 | 1 |
| Fork → Official App | Collection | 42 | 42 | 13 | 3 |
| Official App → Fork | English Deck | 13 | 13 | 6 | 1 |
| Official App → Fork | Collection | 42 | 42 | 15 | 3 |

Both apps used their normal import and export screens. English was removed from the receiving collection before the Deck merge. Learning progress and Deck presets were enabled at import. For each import, the saved database had zero missing or extra Cards, Notes or review entries. Card scheduling fields matched the exported package. Every packaged media file matched the received file byte for byte. The image attached to an English Note was present in all four exchanges. The collection packages also carried a 3GP audio file and a WebP image. [Export and import captures](round-trip.png) · [Database and media comparison procedure](../milestone-3/README.md#repeat-the-comparison)

| Export | Package SHA-256 |
| --- | --- |
| Fork English Deck | `6701f5a347e51b0604bc1cf653257696573667422ad63afd04693c5babe59c75` |
| Fork collection | `eb4f428c7dabfa1674d6cdcf4a4d270d16f6e22ea423dd594abf515eb5d6b4d2` |
| Official App English Deck | `6c3928de5842606974f0dc41ce04ad73b6de52ef6962c1a823a9d9b574842f76` |
| Official App collection | `d54a01a483ff1e2e05a6a84fb87d2b6935533a6fbb29f7cc87e53287c8a5e27d` |
