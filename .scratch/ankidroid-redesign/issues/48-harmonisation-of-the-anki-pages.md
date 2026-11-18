# 48: Harmonisation of the Anki Pages

**What to build:** Opening statistics, Deck options, Card info or an importer no longer feels like leaving the app: each Anki Page uses the Fork's colours and typeface, with its content and arrangement untouched.

**Blocked by:** 40 (Milestone 4 sign-off: settings and dialogs on a device)

**Status:** done

- [x] Statistics, Deck options, Card info, the importers, change Note Type and image occlusion each take the Fork's colours in Light, Dark and Black.
- [x] Each uses the Fork's typefaces for Latin, Chinese and Japanese text.
- [x] No Anki Page's content, arrangement or behaviour changes.
- [x] The web styling follows the modern-web-guidance skill.
- [x] Each page is captured in all three themes as proof.

## Verification

Android 15 rendered all seven Anki Pages in Light, Dark and Black. All seven also loaded the bundled Newsreader face in English, LXGW WenKai for Simplified Chinese and Klee One for Japanese. The same seeded routes and visible page text matched before and after. [Android capture gallery](../../../docs/redesign/milestone-5/anki-pages/README.md)

The Statistics palette regression failed on the previous APK and passed in all three themes on the updated APK. The full focused runs passed 16 Android tests and 20 JVM tests.
