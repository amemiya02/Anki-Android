# 08: Foundation: bundled typefaces and the type scale

**What to build:** Interface text everywhere uses the chosen typefaces: Chinese text in the bundled Chinese face, Japanese in the bundled Japanese face, Latin in the bundled Latin face, and any other script in the system font.

**Blocked by:** 06 (Complete the chosen Direction: Dark, Black, name and launcher icon); 07 (Foundation: colours, shape and the three themes)

**Status:** done

- [x] Full openly licensed typefaces for Simplified Chinese, Japanese and Latin are bundled with the app, with their licences included.
- [x] With the interface in Japanese, shared Han characters show Japanese glyph forms; in Chinese, Chinese forms.
- [x] The type scale is defined once and read by Compose surfaces and the remaining XML screens.
- [x] Line height suits Chinese and Japanese text as well as Latin.
- [x] No style in the type scale relies on italics, capitals or letter-spacing.
- [x] Card Content is unaffected and still uses the font its Card Template sets.
- [x] The deck list is captured in English, Simplified Chinese, Japanese and one right-to-left language as proof.

## Comments

- Faces: Newsreader (variable, Google Fonts), LXGW WenKai Regular and Medium (v1.530 release), Klee
  One Regular and SemiBold (Google Fonts), whole and unmodified in `common/android/src/main/res/font/`.
  Licences ship in `assets/fonts/<face>/OFL.txt`; `LICENSES/OFL-1.1.txt` and `REUSE.toml` record them.
  The fonts are stored uncompressed so Android maps them instead of inflating about 26 MB per Chinese
  face into memory; that makes the APK about 70 MB larger, above the spec's rough 20 to 40 MB.
- The typeface is a fallback chain (`InterfaceTypeface.kt`): Newsreader, then Klee One and LXGW WenKai
  in Japanese or LXGW WenKai in any other language, then the system serif. XML text gets it through
  the themes' `viewInflaterClass` (`FoundationViewInflater`), Compose through `MaterialTheme.foundation.type`
  and `MaterialTheme.typography`. CJK weights follow the Mockups: 400 and 500 take the regular face,
  600 the heavier one.
- Android 7 to 9 cannot build such a chain: there the interface uses the language's own face alone
  (`res/font-zh`, `res/font-ja`), so Latin letters in a Chinese or Japanese interface are drawn in
  that face's Latin, and weights 500 and 600 need Android 9.
- The type scale is `TextAppearance.Foundation.*` in `foundation.xml`; `docs/redesign/quiet-paper.values.json`
  gained a `lineHeight` per role (from the Mockups' CSS) and `figures: tabular` on counts.
  `FoundationTest` checks both XML and Compose against it.
- Proof: `InterfaceTypefaceScreenshotTest` (deck list, en / zh-cn / ja / ar, Light, Dark, Black) and
  `TypeScaleScreenshotTest` (every Compose style with a wrapped paragraph in each language);
  `InterfaceTypefaceTest` and `InterfaceTypefaceDeckListTest` compare drawn pixels with each face
  alone, including the Japanese and Chinese forms of 骨 and 直; `CardContentTypefaceTest` covers
  Card Content.
- Left for ticket 10: layouts with fixed `textSize`/`textStyle` (the deck list's names and counts
  still use them) and about a dozen layouts that name `TextAppearance.Material3.*` directly. Their
  text is already drawn in the interface typeface; only their sizes are off the scale.
