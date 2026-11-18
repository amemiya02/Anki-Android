# 42: Note Types and fields

**What to build:** The owner manages Note Types and their fields in the redesigned look.

**Blocked by:** 41 (Mockups for the rest)

**Status:** ready-for-agent

- [ ] Note Types can be listed, added, renamed and deleted.
- [ ] A Note Type's fields can be added, renamed, reordered and deleted.
- [ ] Changes that force a full sync warn as they do today.
- [ ] A Note Type opens its own page listing its fields, then its Card Templates; a Card Template there opens the Card Template editor (per the Mockups, decision 1).
- [ ] Fields are reordered by dragging, and "Move to position…" stays for screen readers and hardware keyboards (per the Mockups).
- [ ] It matches the agreed Mockup.
- [ ] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [ ] Screen-logic tests and Engine tests still pass unchanged.

## Implementation

- The Manage note types entry opens a Compose list with note and Card Template counts, search and bulk selection. The detail page shows fields above Card Templates. Tapping a Card Template opens its tab in the existing editor.
- Field dragging and Move to position share the same collection operation. Field changes validate against the current Note Type before applying a confirmed schema change. The full-sync warning retains the pending field identity across activity recreation.

## Verification

- The baseline rendered regression failed at the list row interaction because the XML screen had no Compose hierarchy. The replacement tests drive the Compose route and verify the collection state after the actions.
- The rendered matrix drives the Note Types list, add sheet, detail page, field actions and full-sync warning on the running activity. The matrix covers Light, Dark and Black in English, Simplified Chinese, Japanese and Arabic at normal and 200% font size.
