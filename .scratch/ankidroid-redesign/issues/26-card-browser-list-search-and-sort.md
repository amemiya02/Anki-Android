# 26: Card browser: list, search and sort

**What to build:** The owner can find any Card in a redesigned card browser that stays smooth however large the collection is.

**Blocked by:** 25 (Mockups for finding and adding)

**Status:** browser implemented, verification in progress

- [x] Search returns the same Cards and Notes as today for the same query.
- [ ] Results scroll smoothly on a collection of 100,000 Cards; rows are drawn lazily.
- [x] Sort order and the Cards-or-Notes mode can be changed.
- [x] Tapping a result opens it for editing.
- [x] Browse in the bottom bar opens it, and tapping Browse again starts a search.
- [x] On a tablet-sized screen the surface still works in its existing layout.
- [x] Upstream's search view is the only search, finished: the filter chips (decks, card state, flags, tags) take several choices, are filled in from a typed or saved query, and every label is translated; the toolbar search box and the menu items the chips replace are gone.
- [x] Upstream's search tools are dropped or finished, as the owner decides in the Mockup (recommended: dropped, with a link to the manual's search syntax).
- [x] Rows show flag, marked, suspended and buried as marks and words, not as a row tint; the flag colours are added to the Foundation (values file and `foundation.xml`).
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [ ] Screen-logic tests and Engine tests still pass unchanged.

## Implementation evidence

The production Browse tab and standalone browser now render the same Compose list and search page. The existing ViewModel supplies result IDs, and the list requests Card or Note content as rows enter the viewport. The tablet host keeps its resizable editor pane. The options dialog retains truncation, accent handling and default search text. Column editing and bulk operations keep their existing callbacks for ticket 27.

The first seeded rendered test failed against the XML browser because its Card could not be found in the Compose accessibility tree. The first focused Compose run passed 190 tests with one skip, including the Browser, its ViewModel, search and sort logic. The Engine suite completed with 143 passed and seven skipped. The Mockup contrast check passed for all seven flag colours in Light, Dark and Black.

### 100,000 Cards on a Pixel 9 emulator

The ARM64 debug APK with SHA-256 `c32f3c2196a574f2863dab923e07a026adfa600e0e321293f0b28f745d6654af` opened a disposable 100,000-Card collection and scrolled to fixture 00149. All nine Deck choices appeared on opening the sheet. Tapping Browse again opened search, and the query `Browser fixture 98765` showed one matching Card.

The hardware-GPU run used the M2 Metal renderer and ART speed compilation. Before the row optimization, ten swipes rendered 359 frames with a 17 ms median, 25 ms p95 and 3.62% jank. With the optimization, the first pass recorded a 200 ms p95. A subsequent pass over new rows rendered 351 frames with a 22 ms median, 38 ms p95 and 24 janky frames (6.84%). Host load varied. The SwiftShader pass measured 25 janky frames out of 26. A controlled idle-host run with a release or profileable build is pending.

The row loader reuses the ordinary Card colour result to avoid extra Card and Note lookups. If a higher-priority mark can mask another, it reads the additional state from the Engine. A bounded cache holds recent visible rows. [Anki 26.09.4 browser row colours](https://github.com/ankitects/anki/blob/26.09.4/rslib/src/browser_table.rs)

### Tablet verification

On an Android 16 emulator at 1280 × 800 dp, standalone Browse opened alongside the Note Editor. Tapping Search opened Gboard with the editor still visible. Submitting `is:new` showed 99,992 Cards and the New chip in the disposable fixture. Tapping fixture 00001 changed the editor to that Card, and Sort opened in a centered sheet.

The ARM64 debug APK had SHA-256 `05b2b5974db53d3ae8bdf4fe3cb0044fc7ee9481c35e12849d00b43b57458770`. The initial launch under host load reported a startup ANR. A force-stop and fresh launch succeeded, and the emulator's original display settings were restored. Captures are in `/tmp/anki-redesign-qa/browser-tablet/`.

The full-window rendered results check first detected a 40 dp overflow target in the tablet toolbar. It passed with the 48 dp target and adjacent editor visible. Robolectric stalls while showing the IME with the tablet editor present. The rendered phone tests cover search, results and sort, and the tablet tests capture results and sort with the editor visible. The Android run above covers the tablet keyboard transition.

The final rendered matrix passed all 48 device, language, theme and font-scale combinations. It produced 120 captures covering phone results, search and sort, and tablet results and sort. The separate last-row and native text-layout regressions passed after waiting for the loaded row to scroll into view and measuring its actual line count.
