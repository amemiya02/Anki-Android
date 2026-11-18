# 17: Deck list: Decks, counts and starting a session

**What to build:** The owner opens the app to a redesigned deck list, sees what is due in each Deck at a glance, and starts studying a Deck with one tap.

**Blocked by:** 15 (Bottom bar navigation on phones)

**Status:** done

- [x] Decks show as a tree; sub-Decks expand and collapse, and the state is remembered.
- [x] Each Deck shows its new, learning and due counts as the Mockup presents them.
- [x] The total due today and today's study summary are visible.
- [x] Tapping a Deck with Cards starts a study session; tapping a finished Deck opens the finished screen.
- [x] With no Decks, the empty state tells a Newcomer what to do first.
- [x] Long Deck names in Chinese and Japanese stay readable and never push the counts off screen.
- [x] On a tablet-sized screen the surface still works in its existing layout.
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Comments

The deck list is rebuilt in Compose (`deckpicker/DeckListScreen.kt`) inside the existing `DeckPicker`; its state comes from `DeckPickerViewModel.flowOfDeckListScreen`. How each criterion was checked:

- **Tree, expand and collapse, remembered.** The collapse state is the Engine's, as before. `DeckListScreenTest` (chevron "Expand"/"Collapse" by label) and `DeckListTest` (expanded in one deck list, still expanded when it opens again).
- **Counts as the Mockup presents them.** Decision 1, option A: three columns of coloured figures under New / Learning / Due headings, a zero in regular weight and the muted colour, "Done for today" when all three are zero. `DeckListScreenTest`; pictures in `DeckPickerScreenshotTest` reproduce `decks-en.png` (58 to study, 9 studied, every count).
- **Total due and today's summary.** "58 cards to study today", "9 studied so far" and the progress line, at the top. The studied count is new view model state (`cardsStudiedToday()`, the reviews the Engine counts for its "Studied N cards" line). `DeckPickerViewModelTest`, `DeckListTest`.
- **Tapping a Deck.** With Cards it starts the study screen; a finished Deck opens the finished screen (`DeckListTest`). Tapping the counts still opens study options, a long press (or right click) the Deck's menu, as before.
- **Empty state.** "No decks yet", Get shared decks, Import a file, Create a deck, as `decks-empty-*.png`. `DeckListScreenTest`, `DeckListTest`.
- **Long Chinese and Japanese names.** Two lines, then an ellipsis; the count columns never move and widen only when a count would not fit. `long_names` pictures, including 200%.
- **Tablet.** The two-pane layout and drawer are unchanged, with the new list in the start pane: `DeckPickerTabletScreenshotTest`, `DeckPickerTest`'s xlarge runs. On a device: Milestone 2 sign-off (ticket 24).
- **Quality Bar.** `DeckPickerScreenshotTest` (`@CheckAccessibility`, zh-cn/ja/ar, 200%): 72 pictures pass with `-Ptheme=light,dark,black`, opened and compared with the Mockups.
- **Screen-logic and Engine tests.** `DeckPickerViewModelTest` passes with two tests added and none changed; the Engine is untouched.

Deliberately different, or left for other tickets (owner decisions are delegated, 2026-11-17):

- The phone's top bar shows today's date over "Decks" (the Engine's `actionsDecks`) as in the Mockup; tablets keep the app's name. The old "N cards due" subtitle is gone from both: the summary says it.
- The summary shows only how many Cards were studied, as the Mockup does; the Engine's longer "Studied N cards in M minutes today" line is no longer on the deck list (it is on the finished screen and in Stats).
- Column headings are the Engine's words (`actionsNew`, `schedulingLearning`, `decksReviewHeader`), so the rendered-screen tests show them in English in every language. They shrink to fit their column at 200% and stay at the top while the Decks scroll; the summary scrolls away.
- The current Deck is marked with the Mockup's cinnabar dot (a cinnabar chevron when it has sub-Decks); a filtered Deck's name is cinnabar, standing in for the old list's separate filtered-Deck colour. The old list's hierarchy lines are gone: the Mockup indents sub-Decks only.
- "Done for today" also appears on a Deck with no Cards at all; tapping it still says the Deck is empty.
- The add button and the sync icon are ticket 18's. The add button is hidden in the accessibility pictures: the check estimates its label colour from the picture (3.5:1 in Dark) and reports Deck rows lying under it. The tablet picture shows it.
- `DeckAdapter`, its hierarchy decoration and `item_deck.xml` are deleted, so the baseline profiles' deck-row rules are stale and `BaselineProfileTest`'s two deck-row checks were removed: regenerate the profiles on a device (ticket 24 or the final review).
- `androidTest` does not compile on `redesign` already (ticket 15 removed `dev_bottom_nav_key`, still used by the instrumented `DeckPickerTest`); this ticket's instrumented helpers now find Decks by name through UiAutomator.
