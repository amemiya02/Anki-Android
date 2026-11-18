# Milestone 4 device evidence

The 2026-11-19 settings walk and package exchange ran on a disposable Pixel 9 emulator with Android 15 AOSP. Both apps were installed side by side. The owner deferred the personal phone walk and sign-off. [Milestone issue](../../../.scratch/ankidroid-redesign/issues/40-milestone-4-sign-off-settings-and-dialogs-on-a-device.md)

| App | Package and version | APK SHA-256 |
| --- | --- | --- |
| Fork (`51386d6eeb`) | `com.ichi2.anki.redesign.debug` · `2.26.0alpha14-debug` · `22600114` | `43a5d58e2e117fa66082f6ee75f5dc0460634d099ef1cae96e4fc7a21e5333ce` |
| Official App | `com.ichi2.anki` · `2.25.3` · `322503300` | `68de13faf1819008946850aa7c7873f8d60f057ffa96dc2bb5b1ad1bade4df80` |

AnkiDroid 2.25.3 was the current stable release at verification. The installed APK matched the Official App used for Milestone 2. [Official release](https://github.com/ankidroid/Anki-Android/releases/tag/v2.25.3) · [Signing certificate](../milestone-2/README.md)

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
