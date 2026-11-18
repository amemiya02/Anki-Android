# 42: Note Types and fields

**What to build:** The owner manages Note Types and their fields in the redesigned look.

**Blocked by:** 41 (Mockups for the rest)

**Status:** done

- [x] Note Types can be listed, added, renamed and deleted.
- [x] A Note Type's fields can be added, renamed, reordered and deleted.
- [x] Changes that force a full sync warn as they do today.
- [x] A Note Type opens its own page listing its fields, then its Card Templates; a Card Template there opens the Card Template editor (per the Mockups, decision 1).
- [x] Fields are reordered by dragging, and "Move to position…" stays for screen readers and hardware keyboards (per the Mockups).
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Implementation

- The Manage note types entry opens a Compose list with note and Card Template counts, search and bulk selection. The detail page shows fields above Card Templates. Tapping a Card Template opens its tab in the existing editor.
- Field dragging and Move to position share the same collection operation. Field changes validate against the current Note Type before applying a confirmed schema change. The full-sync warning retains the pending field identity across activity recreation.

## Verification

- The baseline rendered regression failed at the list row interaction because the XML screen had no Compose hierarchy. The replacement tests drive the Compose route and verify the collection state after the actions.
- The rendered matrix drives the Note Types list, add sheet, detail page, field actions and full-sync warning on the running activity. The matrix covers Light, Dark and Black in English, Simplified Chinese, Japanese and Arabic at normal and 200% font size.

All 43 focused cases passed across the initial run and targeted reruns. The activity tests cover opening the selected template, restoring a pending field change, and confirming the full-sync warning before writing. All 24 rendered configurations passed, producing 120 captures. The Engine suite passed 143 tests and skipped seven existing cases.
