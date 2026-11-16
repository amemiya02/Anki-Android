# 09: Apply the Fork's name and launcher icon

**What to build:** The Fork appears as Hayaki, with its launcher icon (`docs/redesign/quiet-paper/launcher-icon.svg`), everywhere a person sees it.

**Blocked by:** 01 (Give the Fork its own identity); 06 (Complete the chosen Direction: Dark, Black, name and launcher icon)

**Status:** done (the launcher, app switcher and a real notification are left to the device check in ticket 13)

- [ ] The launcher, the app switcher, the about screen and notifications show the approved name. (About screen verified; the rest deferred to ticket 13, see Comments.)
- [x] The launcher icon is the approved one, in adaptive and themed forms.
- [x] No interface text still calls the app AnkiDroid, except where it refers to Upstream or the Official App.
- [x] New or changed text is provided in English, Simplified Chinese and Japanese.
- [x] The about screen says the app is an unofficial fork of AnkiDroid, credits Upstream, states the licence, and links to the public source of the Fork.

## Comments

- Release builds are named Hayaki (`ForkIdentity.NAME`); debug builds keep the working name
  "AnkiDroid Redesign". Checked in the generated resource values of both variants and by
  `ForkIdentityTest`. The about screen shows Hayaki in every build.
- Interface text says Hayaki in every language (`AppNameInStringsTest` scans every translation,
  including those that spell AnkiDroid in another script). Text that means Upstream or the Official
  App keeps AnkiDroid: the Help menu's manual, FAQ, privacy policy and "Support AnkiDroid" section,
  "Rate AnkiDroid" (opens the Official App's store page), the AnkiDroid API and JavaScript API, and the
  sync setup's route through the Official App. Strings from the Engine and Anki Pages are not changed.
- Pre-existing on `redesign`, not changed here: `PreferenceTitleLengthTest` fails because `menu_my_account`
  (now a sync setup title, ticket 03) has no `maxLength`, and `lintPlayRelease` reports 30 NewApi errors in
  `foundation.xml` (`android:lineHeight`, `android:textFontWeight`, ticket 08).
- Pictures: `AboutScreenshotTest` (English, Simplified Chinese, Japanese) and
  `LauncherIconScreenshotTest` (circle and rounded-square masks, themed form on a light and a dark
  ground), recorded with `-Pscreenshot -Ptheme=light` and looked at.
- Deferred to the device check (ticket 13): the name in the launcher, the app switcher and a real
  notification, and the icon on a real launcher (adaptive masks, themed icons on Android 13+).
- Left to later tickets: the notification small icon (`ic_star_notify`, a star from the Official
  App's logo) for ticket 47, the widgets' logos for ticket 46, and the AnkiWeb sign-in illustrations
  (`login_logo`, `logged_in_logo`) for ticket 44. The launch screen and first-run screen now show the
  Hayaki icon; their colours are left to ticket 12.

**Device check (ticket 13):** On the Pixel 9 emulator (Android 16), the debug build's launcher entry and app switcher show the Hayaki icon (adaptive, circle mask) and the working name "AnkiDroid Redesign", as debug builds should. Still open: Hayaki as the name on a device, which needs a release build installed; a real notification; and the themed (monochrome) icon on the launcher. The launcher setting for themed icons was not changed.
