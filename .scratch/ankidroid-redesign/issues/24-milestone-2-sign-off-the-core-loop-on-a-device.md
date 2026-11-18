# 24: Milestone 2 sign-off: the Core Loop on a device

**What to build:** The owner studies a real session in the Fork, from the deck list through the study screen to the finished screen, and agrees the Core Loop is done.

**Blocked by:** 16 (Native finished screen); 18 (Deck list: adding, Deck actions and sync); 23 (Remove the old study screen)

**Status:** ready-for-agent

- [x] On the emulator, a full session is studied in English and in Simplified Chinese, ending on the finished screen.
- [x] A shared Deck whose Cards use the AnkiDroid JavaScript API is studied without a warning.
- [x] The walk is captured in all three themes and set beside the "before" pictures.
- [ ] Baseline pictures are regenerated under `AnkiDroid/build/` and verified, following the decision in ticket 13.
- [ ] The owner has studied a session on their own phone and signed off the Milestone.
- [ ] Round trip with the Official App: a collection and a Deck exported from the Fork import into the current release of the Official App with Cards, scheduling, review history and media intact, and the same works in the other direction.

## Comments

On 2026-11-18 the owner asked development to continue across milestones while personal phone sign-off stays pending. Emulator checks and the package round trip still apply.

Six emulator sessions finished on the Pixel 9. The current API test studied all three Cards in the published AnkiVoice v38 Deck. The Upstream wiki test Deck displayed the API version-update notice with its 0.0.2 template. [Device evidence](../../../docs/redesign/milestone-2/README.md)

The six Core Loop Roborazzi classes recorded 696 phone captures with no failures across the requested themes, languages and font sizes. The verification run is pending.

With AnkiDroid 2.25.3, the Fork-to-Official Deck and collection compared equal. The Official-to-Fork Deck also compared equal. The Official collection export is ready for another import attempt. The emulator displayed the correct Replace confirmation, then reported repeated input focus and system timeouts. [Package comparison](../../../docs/redesign/milestone-2/README.md#round-trip-with-the-official-app)
