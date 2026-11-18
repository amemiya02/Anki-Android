# Anki Pages on Android

Each sheet shows the bundled Anki Page in Light, Dark and Black on the Android 15 Pixel 9 emulator. The collection contains six example Cards, three reviews and a Deck name with Latin, Chinese and Japanese text.

| Page | Visible evidence | Captures |
| --- | --- | --- |
| Statistics | Review totals, Future Due and Calendar | [Light, Dark and Black](statistics.png) |
| Deck options | Daily limits, intervals and Save | [Light, Dark and Black](deck-options.png) |
| Card info | Card ID, Deck, Note Type and review log | [Light, Dark and Black](card-info.png) |
| CSV importer | File preview with all three scripts, mapping choices and Import | [Light, Dark and Black](csv-import.png) |
| Anki package importer | Package name, import options and Import | [Light, Dark and Black](package-import.png) |
| Change Note Type | Two Note Types, field and Card Template mapping, and Save | [Light, Dark and Black](change-note-type.png) |
| Image occlusion | Image, mask tools and note tab | [Light, Dark and Black](image-occlusion.png) |

The emulator used the production WebView and POST bridge. The before and after captures produced identical routes and rendered text for all seven pages. The original Change Note Type palette captures used the instrumented page host. The final review exercised the page through Browse and the Note Editor on Android. Converting a Basic Note to Basic (and reversed card) added its second Card. Converting it back removed that Card. The Note ID, both fields, the first Card ID and its scheduling values survived both saves. Each save returned to the screen that opened the page.

The WebView loaded Newsreader on all 35 page and profile checks. All seven pages loaded LXGW WenKai in Simplified Chinese and Klee One in Japanese, with Newsreader first for Latin text. [Simplified Chinese and Japanese captures](languages.png)

| Theme | Page background | Text | Text contrast | Primary button contrast |
| --- | --- | --- | --- | --- |
| Light | `#FAFAF8` | `#1F1E1C` | 15.94:1 | 6.33:1 |
| Dark | `#1C1A17` | `#ECE5D8` | 13.86:1 | 4.68:1 |
| Black | `#000000` | `#ECE5D8` | 16.77:1 | 4.68:1 |

The Statistics regression failed on the prior APK on the palette and typeface assertion. The updated APK passed all three palette tests and 13 other Android tests covering the POST bridge, Card info, CSV error dismissal and Deck options. Twenty focused JVM checks passed for bundled assets, navigation and the POST bridge.
