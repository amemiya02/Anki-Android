# 49: Remaining small surfaces

**What to build:** Every Native Surface not covered elsewhere is rebuilt, so that nothing in the app still shows the old look.

**Blocked by:** 41 (Mockups for the rest)

**Status:** implemented, rendered verification in progress

- [x] The about screen, permission screens, manage-space screens, filtered Deck options, the shared-Decks download screen, the instant note editor and the media screens are rebuilt.
- [ ] A walk through every screen the app can show finds no old-look surface.
- [ ] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [x] Screen-logic tests and Engine tests still pass unchanged.

The Compose screens cover About, storage and permission usage, filtered Deck options, shared Deck downloads, the Instant Note Editor, Manage Space, and attachment editing. On Android the tested paths included recording and saving audio to a Note, drawing and saving an image, rotating and cropping an image, editing a shared text intent, and returning after save. [Additional screens](../../../docs/redesign/milestone-5/additional-screens.png) and [media attachments](../../../docs/redesign/milestone-5/media-attachments.png).

The final focused logic run passed all 270 cases in 30 classes and completed the Engine suite. Earlier rendered checks passed all 96 Media Check and pronunciation cases, with four additional gesture regressions. The comprehensive appearance matrix and whole-app walk are still being verified.
