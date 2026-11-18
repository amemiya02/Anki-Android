# Settings parity

[The before-and-after inventory](settings-parity.tsv) lists all 208 rows from the pre-Settings-rebuild XML inventory. It includes route headers, section headings and explanatory rows alongside editable settings. Each row records its original source, storage key where applicable, title, summary, dependency and default, followed by its destination in the Redesign. The baseline follows the prior removal of the old study screen and its settings and the decision to offer Light, Dark and Black as the three named themes.

| Redesign group | Settings and behavior |
|---|---|
| General | Language, the full-screen drawer gesture on tablets, clipboard image conversion, the default Deck for new Cards, double-Back exit, both system context menus, and showing media filenames in the Card Browser. Clipboard format and the default Deck are stored in the selected collection. |
| Appearance | Follow system, Light, Dark, and Black use the saved theme values. The night theme and Deck list background controls remain here. The Card Browser filename switch moved to General. |
| Studying | Scheduling, the study screen, answer buttons and toolbar actions. Timebox has its own subpage. |
| Controls | Corner taps, swipe sensitivity, keyboard shortcuts when a hardware keyboard is available, and every existing binding under Study screen or Previewer. |
| Accessibility | Card zoom, image zoom, answer button size, Card Browser font size, long-press duration, double-tap interval and the animation override. |
| Sync | Account, fetch media, automatic sync, sync badge, metered networks, timeout, one-way sync, and custom server URL, enable switch and certificate. Server setup offers sign-in once the URL is configured. Ticket 44 owns the unified Sync setup. |
| Notifications and Review reminders | Notifications keeps the due-Card threshold, vibration and LED blink under the existing device rules. The developer option still switches to Review reminders, which keeps the existing schedule, editor, and per-Deck reminders. Ticket 47 owns the Review reminders rebuild. |
| Backups | Backup retention help, minimum interval, daily, weekly and monthly limits. The limits still read from and write to the selected collection. |
| Advanced | Collection directory scanner and custom path, workarounds, audio permission, API access, third-party apps, JavaScript and external launch permissions. |
| About | Version, build date, Engine and FSRS versions, contributors, licence, source, changelog and debug details. Developer options are enabled through About in release builds. |
| Developer options and Switch profile | Developer tools and both feature flags retain their prior visibility. Switch profile keeps the existing list, profile editing and switching behavior when its flag is enabled. |

Review reminders, Switch profile, About, Timebox and Toolbar actions include controls generated outside the baseline XML. Their entry points and settings are described in the table above. The old study screen removals are listed separately in [Old study screen features](../core-loop/old-study-screen-features.md).
