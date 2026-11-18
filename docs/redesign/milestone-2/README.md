# Milestone 2 device evidence

The 2026-11-18 walk ran on a disposable Pixel 9 emulator with Android 16 and hardware graphics. The Fork was built from `5c4db1562078b878811ab8cec7ca8025de527cc1`, the commit that retired the old study screen. The Official App was installed alongside it. Phone sign-off remains pending at the owner's request. [Milestone issue](../../../.scratch/ankidroid-redesign/issues/24-milestone-2-sign-off-the-core-loop-on-a-device.md)

| App | Package and version | APK SHA-256 |
| --- | --- | --- |
| Fork | `com.ichi2.anki.redesign.debug` · `2.26.0alpha14-debug` · `22600114` | `a2359af708cf7ed0a725f429897daa2f4e314caa28023b0a1169dc835590faa8` |
| Official App | `com.ichi2.anki` · `2.25.3` · `322503300` | `68de13faf1819008946850aa7c7873f8d60f057ffa96dc2bb5b1ad1bade4df80` |
| Fork used for reverse import (`c63f115e3c`) | `com.ichi2.anki.redesign.debug` · `2.26.0alpha14-debug` · `22600114` | `5cb56c254a71894eccc0df0e4922c08f1b109714350667d66d23976e18f0e2c4` |

The installed Official App's signing certificate was `CN=Nicolas Raoul`, SHA-256 `2071534f0f4b5e54ae952dd275d70da6e3459ee69909d2ab1b4843c4c5b21a45`. GitHub's latest stable release resolved to 2.25.3 on the test date. [Official release](https://github.com/ankidroid/Anki-Android/releases/tag/v2.25.3)

## Core Loop

Each walk started from a fresh import of the test collection. One tap on 中文 opened the first of eight Cards in HSK 4. All eight were revealed and graded Easy before the native finished screen appeared. Its primary action returned to the deck list, where the completed Deck remained visible.

| Locale | Light | Dark | Black |
| --- | --- | --- | --- |
| English (`en-US`) | [Light walk](en-light.png) | [Dark walk](en-dark.png) | [Black walk](en-black.png) |
| Simplified Chinese (`zh-CN`) | [Light walk](zh-light.png) | [Dark walk](zh-dark.png) | [Black walk](zh-black.png) |

Each sheet shows the deck list, question, answer and finished screen in that order. Light pairs the walk with the original Upstream captures. Dark and Black pair it with Milestone 1 captures in the same theme and locale. The question and answer show the Card 熟悉. The collections in the historical captures have different counts. [Before the Redesign](../before/) · [Milestone 1](../milestone-1/)

## Shared Decks using the JavaScript API

Imported the unmodified AnkiVoice v38 test `.apkg` (SHA-256 `1bdf801dce366422f900386ebff8ad237ee90bc5ae1ca2fb161c58e476d9cca8`). Its Card Content initialized API 0.0.3 and displayed its reading controls. Revealed and graded all three Cards using the native buttons and reached the finished screen without an API warning. [Published AnkiVoice test package](https://github.com/tylerecouture/Ankivoice/releases/tag/v38)

The test Deck linked by Upstream's wiki ships API 0.0.2 (SHA-256 `48b104e97c699e920ced44a40ca105623315c5a45de68113835f333cf6238fe5`). Its Mark Card button displayed the API version-update notice on the unmodified download. The screenshots show that notice and the completed session with the current API contract. [Upstream JavaScript API guide](https://github.com/ankidroid/Anki-Android/wiki/AnkiDroid-Javascript-API) · [JavaScript API captures](javascript.png)

## Rendered baselines

`recordRoborazziPlayDebug` passed all 696 captures from `BottomBarScreenshotTest`, `DeckPickerScreenshotTest`, `FinishedScreenScreenshotTest`, `StudyScreenCardFrameScreenshotTest`, `StudyScreenScreenshotTest` and `StudyScreenToolsScreenshotTest`. The phone matrix covers Light, Dark and Black, English, Simplified Chinese, Japanese and Arabic, and normal and 200% font sizes. Accessibility checks ran during capture. The baselines live in `AnkiDroid/build/outputs/roborazzi/`, as agreed for Milestone 1. `verifyRoborazziPlayDebug` is pending on the same matrix. [Milestone 1 decision](../../../.scratch/ankidroid-redesign/issues/13-milestone-1-sign-off-the-foundation-on-a-device.md)

## Round trip with the Official App

Both apps exported fresh packages through their Export menus after two new grades, Good and Easy. The whole collections included media. The Deck exports included scheduling, Deck presets and media. All packages used the current format. Each recipient started without the Deck being merged and imported it with learning progress and presets enabled. The Official App also imported the Fork's whole collection with Replace. [Round-trip captures](round-trip.png)

The comparisons checked Cards and all stored scheduling fields, Note GUIDs, fields and tags, review log entries, and media filenames, byte counts and SHA-256 digests. The Official App re-exported both imports. After the reverse Deck import, the Fork's stored database, write-ahead log and media file were read directly from the disposable device. All four comparisons matched.

| Direction | Export | Cards | Notes | Review entries | Media files | Outcome |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| Fork → Official App | 中文 Deck | 8 | 8 | 5 | 1 | Matched |
| Fork → Official App | Collection | 36 | 36 | 14 | 3 | Matched |
| Official App → Fork | English Deck | 7 | 7 | 5 | 1 | Matched |
| Official App → Fork | Collection | 36 | 36 | 17 | 3 | Matched via production importer. |

Android reported input focus timeouts on a cold file launch and on returning to the Fork. The initial collection import succeeded after restarting the Fork and opening the file with the app running. A fresh instance of the AVD also reported system and storage startup timeouts. The reverse comparison moved to the existing disposable Pixel 9 session, running the Fork at `c63f115e3c` with the same Engine. It reached the Replace confirmation for the Official App's collection, where repeated Android app and system warnings blocked confirmation. An instrumentation runner then invoked the Fork’s production collection importer on an Android 14 AOSP emulator using the same Official export. The recipient database and media directory matched all 36 Cards and Notes, 17 review entries and three media digests.

| File | SHA-256 |
| --- | --- |
| Fork Deck | `839616df1f6d474c34938ee4d4749492914dad3ac94b25f3f294a562171b74a6` |
| Fork collection | `651b24b0598c1a91180be8ad454425f3cd4e022a431c9bbe1abbbbf149b3abec` |
| Fork Deck re-exported by the Official App | `38933b286c7bd00519c194d43264da999197ebc8d68aaef6b7e2dc3c4aa96249` |
| Fork collection re-exported by the Official App | `ff8d1b480a7f40887c06f32f69cdcac54ed4c4ee684d0583108468a62e776a72` |
| Official App Deck | `6ecd081ad40a4f9f83b377c7cd330dbebaf38393d4ce4f1297b2b2a61a15728a` |
| Official App collection | `c9324c13c003cd1b86b59a46410e2801aa4947fa08e8f9662529edb2c54ffa70` |
