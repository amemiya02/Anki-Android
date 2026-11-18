# 34: Dialogs: study screen and card browser

**What to build:** Every dialog reachable from the study screen and the card browser uses the shared dialog patterns.

**Blocked by:** 33 (Dialog patterns and the deck list's dialogs)

**Status:** done

- [x] Every dialog reachable from the study screen and the card browser uses the shared patterns.
- [x] Each dialog does what it did before.
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Verification

Browser Options and Reset progress each passed 24 rendered configurations with accessibility checks. Browser Options saves truncation and restores the choice on reopen. Cancel in Reset progress returns to the active Card. The focused app run passed 92 checks covering tags, scheduling, Find and Replace, Change Note Type, help, voice and whiteboard colors. The Engine passed 143 cases with seven existing skips. The integrated saved-search shortcut regression also passed using the new picker.
