# 25: Mockups for finding and adding

**What to build:** The owner approves how the card browser, the note editor and the previewer will look and behave before they are built.

**Blocked by:** 24 (Milestone 2 sign-off: the Core Loop on a device)

**Status:** done

- [x] Mockups cover the card browser (results, search, selection), the note editor (adding and editing) and the previewer.
- [x] Each is shown in English and Simplified Chinese, in Light, Dark and Black.
- [x] The card browser Mockup settles whether Upstream's unfinished search view and search tools are adopted, finished or dropped.
- [x] The remaining tickets in this Milestone are re-sliced if the Mockups change their shape.
- [x] The owner has approved each Mockup.

## Comments

Drawn on 2026-11-18 ahead of the Milestone at the owner's request; awaiting the owner's look.

The Mockups are captured in `docs/redesign/finding-and-adding/`; its `README.md` lists every picture and its URL, the decisions (each drawn A beside B where it matters), the smaller choices and what is not drawn. Japanese interface text is not drawn (Japanese Card and Deck names are). What awaits the owner:

1. Pick A or B: result rows (`decide-rows.png`), Upstream's search tools (`decide-search.png`; both options adopt and finish Upstream's search view), and where Add and Save sit in the note editor (`decide-save.png`). A is recommended each time.
2. Agree the proposed flag colours (`mockups/src/mock/find/flags.json`); the Foundation has none.
3. Approve each Mockup, or say what to change.

Tickets 26, 27, 28, 29 and 30 were re-sliced to the Mockups (criteria added under each; nothing removed).

On 2026-11-18 the owner chose the recommended option for every decision in this ticket (and in tickets 25, 32 and 41 alike), and asked for a different Light colour scheme. The layouts and decisions are settled; approval waits only on the new Light palette, which is drawn separately and then applied to the Foundation.

The owner confirmed on 2026-11-18 that the Mockups, all twelve choices and the new Light palette are settled and requested implementation. The chosen Light palette is Plain paper (Foundation commit `ee9ab30a4d`).
