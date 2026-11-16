# Features only the old study screen has

ADR-0004 says the old study screen (`Reviewer` / `AbstractFlashcardViewer`) is removed once the newer one (`ReviewerFragment`) can replace it. Before that, each feature only the old screen has is marked "build" or "drop" by the owner. This is that list, found by reading the code in this repository at `redesign` (upstream `2f6b7a649e`), not from memory.

How it was found:

- Four settings screens and the settings search hide the settings only the old screen uses. Each fragment keeps the list in `legacyStudyScreenSettings`: `AccessibilitySettingsFragment`, `AppearanceSettingsFragment`, `AdvancedSettingsFragment` and `ControlsSettingsFragment` in `AnkiDroid/src/main/java/com/ichi2/anki/preferences/`.
- The old screen's settings reads (`restorePreferences` in both classes), its menu (`res/menu/reviewer.xml`), its commands (`ViewerCommand`) and its card rendering (`cardviewer/CardAppearance`) were compared with the newer screen (`ui/windows/reviewer/`, `previewer/CardViewerFragment`, `res/xml/preferences_reviewer.xml`).

Paths below are relative to `AnkiDroid/src/main/java/com/ichi2/anki/`. "Newer" says what `ReviewerFragment` has today.

A "build" means ticket 20 (or 21/22 for the JavaScript API) makes it work in the study screen. A "drop" is a deliberate exception to "no existing feature is removed", and the setting goes when the old screen goes (ticket 23).

## Named in ADR-0004

| # | Feature | What it does today | Code | Newer | Recommendation | Owner's decision |
|---|---|---|---|---|---|---|
| 1 | Save a whiteboard drawing | Saves the drawing as an image file and shows where it went. | `Reviewer` menu `action_save_whiteboard` → `Whiteboard.saveWhiteboard` | No | **Build.** A small action in the whiteboard toolbar; a drawing is otherwise lost when the Card changes. | |
| 2 | Read the whole Card aloud | With the `tts` setting on, reads Card text with the phone's speech engine when the Card has no audio; a menu item picks the voice language per Deck. | `tts` in `AbstractFlashcardViewer.restorePreferences`, `readCardTts`; `action_select_tts` | No. Only `{{tts}}` and `[anki:tts]` in the Card Template speak | **Drop.** Anki's own `{{tts}}` field in the Card Template does this, per field and per language, and works the same on desktop. | |
| 3 | Answer-button position | Puts the answer buttons at the top or the bottom, or hides them. | `answerButtonPosition` | Partial: always at the bottom; a switch hides them | **Drop.** The Mockup puts them at the bottom where the thumb is; hiding them stays. If the owner picks B in `decide-buttons.png`, this becomes "build". | |
| 4 | Minimum press time | Ignores answer-button presses shorter than a set time, so a brush of the thumb does not answer. | `showCardAnswerButtonTime` → `minimalClickSpeed` | No | **Build.** An accessibility setting for tremor; small. | |
| 5 | Double-tap timeout | Sets how quickly two taps count as a double tap. | `doubleTapTimeout` = `Prefs.doubleTapInterval` | No: uses the system value in `GestureParser`; upstream marks it "TODO implement it in the new study screen" | **Build.** An accessibility setting; one value passed to `GestureParser`. | |
| 6 | AnkiDroid JavaScript API | Card Templates call AnkiDroid from JavaScript (counts, answer, flag, speech and more). | `AnkiDroidJsAPI`; `AbstractFlashcardViewer.jsApi` | No: every call shows "not supported" (`ReviewerFragment`) | **Build.** Already decided by ADR-0004; tickets 21 and 22. | |
| 7 | Time-remaining estimate | Shows roughly how long the rest of the session will take, under the title. Off by default. | `showETA` → `sched.eta` in `Reviewer` | No | **Build,** shown quietly after the counts, as in Mockup A of `decide-studybar.png`. | |
| 8 | Deck title | Shows the current Card's Deck as the title. Off by default. | `showDeckTitle` → `AbstractFlashcardViewer.updateDeckName` | No | **Build,** shown as in Mockup A of `decide-studybar.png`; it says which sub-Deck a Card came from. | |

## Found in the code

| # | Feature | What it does today | Code | Newer | Recommendation | Owner's decision |
|---|---|---|---|---|---|---|
| 9 | Full-screen modes | Three modes; the bars hide again after a delay, and the top bar and answer buttons fade with them. | `fullscreenMode` / `FullScreenMode` in `Reviewer` | Partial: hides the system bars, the toolbar or the answer buttons, but nothing re-hides on its own | **Drop.** The newer screen's three switches cover the same need without timers. | |
| 10 | Show top bar | Hides the bar with the counts, timer and mark and flag icons. | `showTopbar` | Partial: the toolbar can be hidden whole; counts follow the collection's "show remaining count" | **Drop.** Covered by the toolbar position "none" and the collection setting. | |
| 11 | Centre vertically | Centres short Card Content vertically. | `centerVertically` → `CardAppearance` class `vertically_centered` | No | **Build.** One CSS class; people who set it would otherwise see their Cards move. | |
| 12 | Image zoom | Scales images on the Card separately from text. | `imageZoom` → `CardAppearance` | No (card zoom exists) | **Build.** An accessibility setting; one CSS rule. | |
| 13 | Theme class on the Card | Adds `ankidroid_dark_mode` (and `ankidroid_plain_mode`) to the Card so Card Templates can style for them. | `CardAppearance.getCssClasses` | No: only `nightMode night_mode` | **Build** `ankidroid_dark_mode`, so shared Card Templates that use it keep working; drop `ankidroid_plain_mode` with the Plain theme. | |
| 14 | Default Card font | Sets OpenSans on the Card body when the Card Template sets no font. | `reviewer/ReviewerCustomFonts` | No | **Drop.** The newer screen renders as desktop Anki does; the default Card Template sets its own font. | |
| 15 | Large answer buttons | Makes the answer buttons taller. | `showLargeAnswerButtons` | No | **Drop.** The Mockup's buttons are 66dp already, and the newer screen's button-size setting covers the rest. | |
| 16 | Answer-button size | Sets the answer buttons' size. | `answerButtonSize` (`Prefs.answerButtonsSize`) | Partial: a different key, `answerBtnSize`, so the old value is not carried over | **Build** a one-time copy of the old value into the new setting. | |
| 17 | Gestures on or off | Gestures do nothing unless switched on. | `gestures` (`GestureProcessor.PREF_KEY`) | Different: always on, each gesture bound or unbound | **Drop** the switch, but clear the gesture bindings once for people who had gestures off, so no swipe starts acting unexpectedly. | |
| 18 | Double scrolling | Page Up and Page Down scroll two pages. | `double_scrolling` → `processHardwareButtonScroll` | No: one page | **Drop.** Rarely used; the keys still scroll. | |
| 19 | Reset speech languages | Forgets the voice language chosen for each Deck. | `resetLanguages` | No | **Drop,** with #2. | |
| 20 | Hide the whiteboard but keep it on | Hides the drawing to see the Card beneath, without turning the whiteboard off. | `action_hide_whiteboard` | Partial: only the whiteboard toolbar hides | **Build.** One toggle in the whiteboard toolbar. | |
| 21 | Pen colour command | A bindable command and a palette for the pen colour. | `ViewerCommand.CHANGE_WHITEBOARD_PEN_COLOR` | Partial: brushes and a colour picker in the whiteboard toolbar, no binding | **Drop.** The whiteboard toolbar's brushes replace it. | |
| 22 | Whiteboard state per Deck | Remembers per Deck whether the whiteboard is on and shown, the stylus mode and the pen colour. | `MetaDB.getWhiteboardState` and siblings, used by `Reviewer` | Partial: one setting for every Deck | **Build.** The whiteboard suits a kanji Deck and not an English one; switching it each time is friction. | |
| 23 | Voice recorder per Deck | Remembers per Deck whether the voice recorder is shown. | `MetaDB.getMicToolbarState` | Partial: one setting for every Deck | **Build,** with #22: the same mechanism. | |
| 24 | Save voice command | A bindable command that stops and keeps a recording. | `ViewerCommand.SAVE_VOICE` | No (record and replay only) | **Drop.** Stopping in the recorder keeps the recording. | |
| 25 | Choose a speech voice | Menu item to choose the speech language for the Deck. | `action_select_tts` | No | **Drop,** with #2. | |
| 26 | Answer functions in Card HTML | `buttonAnswerEase1`–`4()`, `taFocus()` and `reloadPage()` work from Card Templates. | `AbstractFlashcardViewer.filterUrl` | No: they show "not supported" (`showAnswer()` works) | **Build,** with the JavaScript API (tickets 21–22). | |
| 27 | Answer key on the question side | An answer key on the question first shows the answer, with a visible press. | `flipOrAnswerCard` | Different: answer keys act on the answer side only | **Drop.** Answering a Card before its answer is shown is more often a slip than a wish. | |
| 28 | Side drawer from the study screen | Swipe from the edge to reach Decks, the card browser, statistics and settings. | `AbstractFlashcardViewer` extends `NavigationDrawerActivity` | No | **Drop.** The bottom bar replaces the drawer on phones (ticket 15); back returns to the deck list. | |
| 29 | Old app-bar button settings | Choose for about 35 actions whether each shows in the bar, in the menu, or not at all. | `preferences_custom_buttons.xml`, `ActionButtons` | Partial: the newer screen's toolbar-actions settings; whiteboard and speech items are not there | **Drop.** The newer settings replace it; whiteboard actions move to the whiteboard toolbar (#1, #20). | |
| 30 | Recovery when the Card view crashes | Reloads when the web view that draws the Card crashes, instead of leaving a blank screen. | `onRenderProcessGone` → `OnRenderProcessGoneDelegate` | No | **Build.** Not visible until it is needed, then the screen is blank. | |
| 31 | Hide the due count | A hidden setting with no entry in settings. | `hideDueCount` in `Reviewer` | No | **Drop.** No one can set it from the app. | |
| 32 | Keep the screen on during auto-advance | The auto-advance setting also keeps the screen on. | `timeoutAnswer` in `AbstractFlashcardViewer.restorePreferences` | Partial: only `keepScreenOn` does | **Build.** Without it the screen can sleep in the middle of an auto-advancing session. | |

## Not differences

Both study screens already have: flags, mark, edit, add a Note, tags, bury and suspend, delete, set due date, reset, Card info, Deck options, undo and redo, auto-advance, replaying audio and voice, hints, user actions 1–9, the timebox message, leech messages, the answer timer, shake, controllers and other input devices, card zoom, type-in-the-answer options, keep screen on, swipe sensitivity, corner taps, the estimates on the answer buttons, the progress bar and the audio play buttons. Nothing in either screen adds a dictionary lookup, an E-ink mode for Cards or a colour-inversion option.
