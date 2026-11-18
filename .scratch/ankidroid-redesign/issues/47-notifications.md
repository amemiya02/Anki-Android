# 47: Notifications

**What to build:** Review reminders and other notifications match the app in look and wording.

**Blocked by:** 41 (Mockups for the rest)

**Status:** done

- [x] Every notification the app sends uses the Fork's name, icon and accent colour.
- [x] Wording follows the glossary and is provided in English, Simplified Chinese and Japanese.
- [x] Tapping a notification opens the same place as today.
- [x] The reminder setup screens are rebuilt in the new look.
- [x] Review reminders are the only reminders: the old "Notify when" setting, its Vibrate and Blink light switches and its notification are removed, and an old "more than N Cards due" choice becomes an all-Decks reminder with that threshold (per the Mockups, decision 4).
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.

On Android 15 the pending legacy threshold migrated on launch to one enabled all-Decks reminder at 08:00 and retained its ID and time after a second launch. With the previous delivery timestamp set before the next scheduled occurrence, the production receiver posted a “Time to study” notification with colour `#A63D25` and both snooze actions. Tapping the system notification opened the Deck list. The focused suite passed all 100 Android tests and 143 Engine tests, with seven existing Engine skips. [Emulator captures](../../../docs/redesign/milestone-5/review-reminders.png)

All 192 rendered checks passed for the reminder list, editor, time picker, deletion confirmation and troubleshooting screens in Light, Dark and Black, in English, Simplified Chinese, Japanese and Arabic, at normal and 200% text size.
