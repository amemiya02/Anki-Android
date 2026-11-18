# 26: Card browser: list, search and sort

**What to build:** The owner can find any Card in a redesigned card browser that stays smooth however large the collection is.

**Blocked by:** 25 (Mockups for finding and adding)

**Status:** ready-for-agent

- [ ] Search returns the same Cards and Notes as today for the same query.
- [ ] Results scroll smoothly on a collection of 100,000 Cards; rows are drawn lazily.
- [ ] Sort order and the Cards-or-Notes mode can be changed.
- [ ] Tapping a result opens it for editing.
- [ ] Browse in the bottom bar opens it, and tapping Browse again starts a search.
- [ ] On a tablet-sized screen the surface still works in its existing layout.
- [ ] Upstream's search view is the only search, finished: the filter chips (decks, card state, flags, tags) take several choices, are filled in from a typed or saved query, and every label is translated; the toolbar search box and the menu items the chips replace are gone.
- [ ] Upstream's search tools are dropped or finished, as the owner decides in the Mockup (recommended: dropped, with a link to the manual's search syntax).
- [ ] Rows show flag, marked, suspended and buried as marks and words, not as a row tint; the flag colours are added to the Foundation (values file and `foundation.xml`).
- [ ] It matches the agreed Mockup.
- [ ] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [ ] Screen-logic tests and Engine tests still pass unchanged.
