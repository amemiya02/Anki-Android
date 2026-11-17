# 15: Bottom bar navigation on phones

**What to build:** On a phone, Decks, Browse, Stats and More are one tap away in a bottom bar, and the side drawer is gone. More leads to settings, help and support.

**Blocked by:** 14 (Mockups for the Core Loop)

**Status:** done

- [x] The bottom bar shows Decks, Browse, Stats and More, and each tab opens its existing screen.
- [x] More is a new surface listing settings, help and support, and each entry opens what the drawer's entry opened.
- [x] The side drawer no longer exists on phones.
- [x] The system back gesture returns to Decks from another tab, and leaves the app from Decks.
- [x] On a tablet-sized screen the drawer and two-pane layouts still work.
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.

## Comments

Built on Upstream's bottom navigation. What changed and how each criterion was checked:

- **Bar on phones, drawer gone.** The developer option `devBottomNav` is removed: the bar is the phone's only navigation (`bottom_navigation_available`, true below `sw600dp`). Phones' `activity_navigation_drawer*.xml` hold no `NavigationView`; the drawer layouts are in `layout-sw600dp/`. `NavigationDrawerActivity.hasNavigationDrawer` guards every drawer call; screens that had a drawer button (browser, old study screen) show a back button. The full-screen drawer gesture setting is hidden on phones. Checked by `BottomBarNavigationTest` (tabs open Browse/Statistics/More, no drawer view, back returns to Decks, back on Decks leaves).
- **More.** Rebuilt in Compose (`MoreScreen`): Settings, Help, Support AnkiDroid, each through the same `NavigationDrawerActivity.openSettings/openHelp/openSupport` calls the drawer uses (the help and support menus, settings with the drawer's recreate-on-return). Upstream's longer list of help and support links is reached through those menus, as on the drawer. Checked by `BottomBarNavigationTest`.
- **Tablets.** `DeckPickerTest` (xlarge), `ReviewerTest` (sw600dp), `NavigationDrawerInsetsTest` and `NavigationDrawerScreenshotTest` (now on the tablet configuration) cover the drawer and split pane. On a device: Milestone 2 sign-off (ticket 24).
- **Mockup and Quality Bar.** `BottomBarScreenshotTest` (Decks and More, `@CheckAccessibility`, zh-cn/ja/ar, 200%) passes 48 pictures with `-Ptheme=light,dark,black`; compared with `docs/redesign/core-loop/more-*.png` and the bar in `decks-*.png`.

Deliberately different or left for other tickets:

- The More footer says "Based on AnkiDroid" (the existing `about_fork_title`, translated) rather than the Mockup's "Built on AnkiDroid", to reuse a string instead of adding a near-duplicate.
- Help and Support carry the Mockup's "opens outside" mark although they open the help and support menus first (as the drawer did); every entry in those menus opens outside the app.
- In the rendered-screen tests the Decks tab label is English in every language: it is the Engine's `actionsDecks` string, which Robolectric does not translate. On a device it follows the app's language.
- The Decks picture hides the add button: in Dark and Black the accessibility check reports its label colour (#1C1A17 on cinnabar, 3.5:1). The add button and the deck list belong to tickets 17 and 18. At 200% the "Studied ..." summary is cut at the top of the bar; also the deck list's (ticket 17).
- The bar is still Material's `BottomNavigationView` restyled (hide-on-scroll, saved tab and tests depend on it); a Compose bar can come with the single-activity work (ticket 50).
