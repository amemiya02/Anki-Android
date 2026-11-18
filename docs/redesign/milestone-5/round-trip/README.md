# Round trip with the Official App

On 2026-11-19, both apps exchanged an English Deck and a collection on a disposable Pixel 9 emulator. The sender reviewed Cards before exporting. The recipient removed the English Deck before importing the Deck package with learning progress and Deck presets enabled. Each collection package went through the recipient’s Replace action. All four exports used the current package format and included media.

| App | Package and version | APK SHA-256 |
| --- | --- | --- |
| Hayaki debug | `com.ichi2.anki.redesign.debug` · `2.26.0alpha14-debug` · `22600114` | `0e723909d89329e2382b0f5dd1c785260599acf314fc70f954243efe36fcdd09` |
| Official App | `com.ichi2.anki` · `2.25.3` · `322503300` | `68de13faf1819008946850aa7c7873f8d60f057ffa96dc2bb5b1ad1bade4df80` |

The Official App’s APK digest matches the arm64 release asset. GitHub marked 2.25.3 as the latest stable release at verification. [Official release](https://github.com/ankidroid/Anki-Android/releases/tag/v2.25.3)

| Direction | Package | Cards | Notes | Review entries | Media files | Android captures |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| Hayaki → Official App | Deck | 13 | 13 | 8 | 1 | [Export](m5-fork-deck.apkg-export.png) · [Merge options](official-deck-merge.png) · [Import result](official-deck-added.png) |
| Hayaki → Official App | Collection | 42 | 42 | 17 | 4 | [Export](m5-fork-collection.colpkg-export.png) · [Replace confirmation](official-collection-replace.png) · [Decks](official-collection-list.png) |
| Official App → Hayaki | Deck | 13 | 13 | 10 | 1 | [Export](m5-official-deck.apkg-export.png) · [Merge options](fork-deck-merge.png) · [Import result](fork-deck-added.png) |
| Official App → Hayaki | Collection | 42 | 42 | 19 | 4 | [Export](m5-official-collection.colpkg-export.png) · [Replace confirmation](fork-collection-replace.png) · [Decks](fork-collection-list.png) |

The receiver’s SQLite database matched the exported package in every direction. The comparison found zero missing or extra Cards, Notes or review entries. Card scheduling fields, Note GUIDs, fields and tags, and review history matched. Every received media file matched its packaged byte count and SHA-256. The Deck included a PNG. The collection also carried two WebP images and a 3GP recording. [Comparison procedure](../../milestone-3/README.md#repeat-the-comparison)

| Export | Package SHA-256 |
| --- | --- |
| Hayaki Deck | `e219969e251e09a0e98275c1306a97c13e8ad04c58670a2c7aba9400f27a7a77` |
| Hayaki collection | `c47c23a0409f23aaa0806dd6a9f68ab4b6ccfbb754704add512dfd456e60ec36` |
| Official App Deck | `21d31d2a2d489cc432eb6a1720fde4352da4a833799bdd5e0739e60e56973d1c` |
| Official App collection | `412ab8f451a3fae27d990dae02c756ff6c872fa7fdd81c087e39293283e2a705` |
