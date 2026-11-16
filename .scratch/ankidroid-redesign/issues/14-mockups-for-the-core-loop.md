# 14: Mockups for the Core Loop

**What to build:** The owner approves how the bottom bar, deck list, study screen and finished screen will look and behave before they are built.

**Blocked by:** 13 (Milestone 1 sign-off: the Foundation on a device)

**Status:** ready-for-human

- [x] Mockups cover the bottom bar, the deck list (with Decks, empty, and with long Chinese and Japanese names), the study screen (question and answer) and the finished screen.
- [x] Each is shown in English and Simplified Chinese, in Light, Dark and Black.
- [x] The deck list Mockup settles how the new, learning and due counts are shown.
- [x] The study screen Mockup settles whether the deck title and time-remaining estimate are shown, and where the answer buttons sit.
- [ ] Every feature only the old study screen has is listed and marked "build" or "drop" by the owner: saving a whiteboard drawing, reading the whole Card aloud, answer-button position, minimum press time, double-tap timeout, and any other found.
- [x] The finished screen Mockup shows every state: more Cards later today, daily limit reached, Cards buried, nothing left.
- [ ] The owner has approved each Mockup.

## Comments

The Mockups are drawn and captured in `docs/redesign/core-loop/`; its `README.md` lists every picture and the URL that shows it. Japanese interface text is not drawn (Japanese Deck names are). What awaits the owner:

1. Pick A or B for the three things the Mockups settle. Each picture shows the recommendation (A) beside one alternative (B):
   - `decide-counts.png`: counts in three columns under a header (A), or one total with the kinds named under the Deck (B).
   - `decide-studybar.png`: Deck title and time-remaining estimate shown (A), or counts only (B).
   - `decide-buttons.png`: answer buttons at the bottom with no setting (A), or at the top with the old screen's setting kept (B).
2. Mark "build" or "drop" in the "Owner's decision" column of `docs/redesign/core-loop/old-study-screen-features.md`. It lists 32 features only the old study screen has, found in the code, each with a recommendation.
3. Approve each Mockup, or say what to change.
