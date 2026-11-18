# Mockups for the rest of the app

Mockups for ticket 41, in Quiet paper. They cover Note Type management, the Card Template editor and its previewer, sync setup, the first-run screen, the home-screen widgets and notifications. Each surface is shown in English and Simplified Chinese, and each picture shows Light, Dark and Black side by side. Colours come from `../quiet-paper.values.json`; `node mockups/scripts/check-contrast.mjs` passes, and no new colour pairing was added. The only colours not taken from the values are the ones Android draws around the app: the scrim, the keyboard, the notification shade and the wallpaper.

They were drawn on 2026-11-18, ahead of Milestone 5, at the owner's request.

To see them, run `npm install && npm run dev` in `mockups/`, then open the URL in the last column. `lang` is `en` or `zh`.

## Pictures

| Picture | Shows | URL |
|---|---|---|
| `overview-en.png`, `overview-zh.png` | Every surface below, in Light | `/?rest=overview&lang=en` |
| **Note Types (ticket 42)** | | |
| `note-types-en.png`, `-zh` | Note Types, with notes and Card Templates counted, search, and the add button | `/?rest=note-types&lang=en` |
| `note-type-add-en.png`, `-zh` | Adding a Note Type: one of Anki's standard types, or a copy of one of yours, then a name | `/?rest=note-type-add&lang=en` |
| `note-type-en.png`, `-zh` | A Note Type's page: its fields (drag to reorder; the sort field and keyboard language shown), then its Card Templates | `/?rest=note-type&lang=en` |
| `field-actions-en.png`, `-zh` | A field's actions: rename, move to a position, sort the browser by it, keyboard language, delete | `/?rest=field-actions&lang=en` |
| `full-sync-en.png`, `-zh` | The warning before a change that forces a one-way sync | `/?rest=full-sync&lang=en` |
| **Card Template editor (ticket 43)** | | |
| `template-en.png`, `-zh` | The editor: one tab per Card Template, Front, Back and Styling, the code with its fields picked out, and a row of fields to insert above the keyboard | `/?rest=template&lang=en` |
| `template-more-en.png`, `-zh` | The editor's actions, every one the editor has today | `/?rest=template-more&lang=en` |
| `template-preview-en.png`, `-zh` | The previewer: the Card as the study screen draws it | `/?rest=template-preview&lang=en` |
| **Sync (ticket 44)** | | |
| `sync-server-en.png`, `-zh` | Setup, step 1: the server address, an optional certificate, and the AnkiWeb route | `/?rest=sync-server&lang=en` |
| `sync-sign-in-en.png`, `-zh` | Setup, step 2: sign in to that server | `/?rest=sync-sign-in&lang=en` |
| `sync-account-en.png`, `-zh` | Sync: who is signed in, to which server, sync now, sign out, and how syncing behaves | `/?rest=sync-account&lang=en` |
| `sync-error-en.png`, `-zh` | A sync error in plain words, over the deck list | `/?rest=sync-error&lang=en` |
| `sync-conflict-en.png`, `-zh` | A conflict: keep this phone's collection or the server's | `/?rest=sync-conflict&lang=en` |
| **First run (ticket 45)** | | |
| `welcome-en.png`, `-zh` | The first-run screen: start with no Decks, import, or sync with your own server | `/?rest=welcome&lang=en` |
| `welcome-ankiweb-en.png`, `-zh` | Coming from AnkiWeb: the three steps, and a button to choose the file | `/?rest=welcome-ankiweb&lang=en` |
| **Widgets (ticket 46)** | | |
| `widgets-en.png`, `-zh` | A home screen with every widget: the deck list, one Deck, the small due count, and Add note | `/?rest=widgets&lang=en` |
| `widget-sizes-en.png`, `-zh` | The resizable widgets at their smallest and a larger size, and a widget whose Deck was deleted | `/?rest=widget-sizes&lang=en` |
| `widget-config-en.png`, `-zh` | Choosing the deck list widget's Decks | `/?rest=widget-config&lang=en` |
| **Notifications (ticket 47)** | | |
| `notifications-en.png`, `-zh` | The shade: a review reminder, a Deck's reminder, sync in progress, and a sync that did not finish | `/?rest=notifications&lang=en` |
| `reminders-en.png`, `-zh` | Review reminders: each time, its Deck and its conditions, with a switch | `/?rest=reminders&lang=en` |
| `reminder-edit-en.png`, `-zh` | Adding a review reminder | `/?rest=reminder-edit&lang=en` |
| `reminders-check-en.png`, `-zh` | Checking that reminders can arrive | `/?rest=reminders-check&lang=en` |
| **Decisions** | | |
| `decide-note-types.png` | Decision 1, A beside B | `/?rest=decide-note-types` |
| `decide-sync.png` | Decision 2, A beside B, in English and Chinese | `/?rest=decide-sync` |
| `decide-welcome.png` | Decision 3, A beside B, in English and Chinese | `/?rest=decide-welcome` |

## Decisions for the owner

The surface pictures all show the recommended answer, A. Following the owner gates of 2026-11-17, a choice the owner does not make follows the recommendation.

1. **How fields and Card Templates are reached from Note Types** (`decide-note-types.png`).
   - A, recommended: a Note Type opens its own page, listing its fields and then its Card Templates; Rename and Delete are in the page's menu. Everything about one Note Type is in one place, and each row in the list has one target instead of three.
   - B: as today. Tapping a row opens the fields; each row has a Card Templates button and a menu with Rename and Delete.
2. **Where sync's settings live** (`decide-sync.png`).
   - A, recommended: one Sync page. It says who is signed in and to which server, has Sync now and Sign out, and holds the settings now in Settings › Sync (automatic sync, waiting-changes mark, mobile data, fetch media, network timeout, certificate, one-way sync). Settings › Sync becomes a row that opens this page. Sync setup, account and settings are then the "one redesigned place" ticket 44 asks for.
   - B: the Sync page holds the account only, with a row to Settings › Sync, which keeps the settings (ticket 38 redesigns that page).
3. **The first-run screen's main way in** (`decide-welcome.png`).
   - A, recommended: "Start with no Decks" is the primary button; importing and syncing with your own server are rows below it. One button reads as the default for a Newcomer, and the deck list they land on already offers shared Decks, import and create.
   - B: the three choices as equal rows, with no primary button.
4. **Drop the old notification setting.** Two reminder systems exist today, switched by a developer option. Recommended: keep only review reminders (drawn here) and drop the old "Notify when" setting (never, pending messages, more than N Cards due), its Vibrate and Blink light switches, and its "N Hayaki cards due" notification. A review reminder for all Decks with a threshold does what "more than N Cards due" did, at a time the person chooses; vibration and sound are set per channel in Android, which "Notification settings" opens. On the first start after the change, an old "more than N" choice becomes an all-Decks reminder at 08:00 with that threshold. Alternative: keep both, as today.
5. **Widgets in Black.** Recommended: widgets follow the app's theme: Light by day, and by night Dark or Black, whichever the app uses for dark. The widget pictures show this. Alternative: widgets follow only Android's light and dark, and are never Black.
6. **Drop from sign-in:** the logo and the "Privacy" button. The privacy text is AnkiWeb's, and the Fork never signs in to AnkiWeb; a self-hosted server has its owner's own policy. Recommended: drop both.

Smaller choices drawn without an alternative, which the owner can still change:

- **Field order** is changed by dragging a row by its handle. "Move to position…" stays in the field's actions, for screen readers and hardware keyboards; it replaces today's "Reposition field" dialog.
- **Front, Back and Styling** are a three-part switch at the top of the editor, where they stay visible while the keyboard is open. Today they are a bottom bar that hides behind the keyboard.
- **Insert field** is a row of the Note Type's fields above the keyboard; one tap inserts `{{Field}}` at the caret. "Insert field" stays in the editor's actions and opens today's full list, with the special fields (FrontSide, Deck, Subdeck, Flag, Tags, CardID, Card, Type) and their explanations.
- **Template code** is set in the device's monospace face (IBM Plex Mono stands in for it in the pictures), with `{{fields}}` in cinnabar.
- **The previewer** shows the Card on the card sheet, as the study screen does, with "Show answer" / "Hide answer" below it, and says which Note it uses. On a tablet the editor keeps today's live preview beside the code.
- **Sync setup is two steps**, address then sign-in, with a step mark. The sign-in field is "Username", since a self-hosted server signs in with a user name rather than an AnkiWeb email; "Change server" goes back to step 1.
- **The conflict dialog** names "this phone" and "the server" with when each last changed, instead of today's "Hayaki" and "AnkiWeb" buttons. Each choice says what it replaces.
- **Sync errors** say what happened and what to do. The wording for each is in the table below; only the most common, an unreachable server, is drawn.
- **Notifications** use the Hayaki mark tinted cinnabar as the small icon, the name "Hayaki", cinnabar actions and progress, and the glossary's words. Android draws everything else, in its own face. "Uploading to AnkiWeb…" becomes "Uploading to <server>".
- **The first-run title** keeps today's "Study less. Remember more." Below it a line says Hayaki is an unofficial fork of AnkiDroid, as the spec asks of the about screen.
- **The deck list widget** shows the counts in the deck list's three colours, without a header row, and "N due today" at its top. The smallest size shows the first Deck.
- **A reminder whose Deck was deleted** is struck through and says so in cinnabar; its switch is off.

### Sync errors in plain words

| Today | Proposed (English) |
|---|---|
| "A network error has occurred" | Couldn't reach your sync server. *server* did not answer. Check that the server is running and that this phone can reach it, then try again. |
| "Sign in to your sync server to sync your collection." | You're not signed in to *server*. Sign in to sync. |
| Conflict: "Hayaki" / "AnkiWeb" | Keep which collection? Keep this phone's / Keep the server's (drawn) |
| Sanity check: "Local" / "Remote" | The two collections don't match, so a normal sync isn't safe. Keep this phone's / Keep the server's (same dialog as the conflict) |
| Media error, "Check media" | Some media files couldn't be synced. Check media finds and fixes missing or broken files. |
| Database error, "Check database" | Your collection has a problem the server won't accept. Check database repairs it; then sync again. |
| Corrupt collection | Your collection is damaged and can't be synced until it's repaired. (Help link kept) |
| Clock off | This phone's clock is wrong by more than the server allows. Turn on automatic date and time, then sync again. |
| Collection too large | Your collection is larger than the server accepts. |
| Metered connection | You're on mobile data. Syncing may use some of your data allowance. Continue / Don't ask again / Cancel |

## Not drawn

- Japanese interface text: the ticket asks for English and Simplified Chinese. Japanese Note Type and Deck names are drawn, in Klee One.
- The small dialogs these screens open: rename, delete confirmations, reposition, deck override's Deck picker, Restore to default, Discard changes, the keyboard-language list, the certificate text, the time picker. They follow the dialog patterns of the settings and dialogs Mockups (ticket 32).
- The browser appearance page (two text fields and a note); it is laid out like sync setup's address field.
- Choosing several Note Types to delete (long press, as today), and search results.
- The tablet layouts, 200% font size and right-to-left: checked on the rebuilt surfaces.
- The one-Deck widget's Deck chooser: a Deck picker and Done, like the deck list widget's chooser.
- The notification permission request, which is Android's own dialog.
- The surfaces of ticket 49 (about, permissions, manage space, filtered Deck options, shared Decks, the instant note editor, media) and the Anki Pages of ticket 48: neither ticket asks for a Mockup.

## Kept from today, by surface

Nothing that exists today is left out without being listed above. For checking:

- **Note Types:** list, search, add (standard types or clone), rename, delete one or several, the full-sync warning, notes count; fields: add, rename, reposition, sort by, keyboard language hint, delete, with the last-field and duplicate-name messages.
- **Card Template editor:** a tab per Card Template, front, back, styling, save, preview, browser appearance, add, rename, reposition, delete (with its orphan-Note check), deck override, insert field (and in the text-selection menu), copy as Markdown, restore to default, discard changes, all keyboard shortcuts.
- **Sync:** server address and its switch (an address is either set or empty), custom root certificate, sign-in, signed-in user and server, sign out, sync now, fetch media, automatic sync, sync status mark, metered connections, network timeout, one-way sync, every error dialog, progress and media sync log.
- **First run:** start empty and import, plus sync setup and the AnkiWeb steps.
- **Widgets:** the small due count with minutes, Add note, the deck list widget (up to 5 Decks, reorderable, its chooser) and the one-Deck widget, their empty states and what tapping opens.
- **Notifications:** sync and media sync progress with Cancel / Abort, sync failures, review reminders with snooze, the reminders list, add/edit with time, Deck, threshold, kinds counted and "only if not studied today", delete, troubleshooting with each check and its fix, and the link to Android's notification settings.
