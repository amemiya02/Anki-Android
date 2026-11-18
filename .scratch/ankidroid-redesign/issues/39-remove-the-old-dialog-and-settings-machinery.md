# 39: Remove the old dialog and settings machinery

**What to build:** Nothing in the app can show an old-style dialog or settings screen, because the code that drew them is gone.

**Blocked by:** 34 (Dialogs: study screen and card browser); 35 (Dialogs: note editor, importing, errors and the rest); 37 (Settings: studying and controls); 38 (Settings: everything else)

**Status:** implemented, final integration verification pending

- [x] The old dialog and settings building blocks are deleted.
- [ ] The app builds and every test passes without them.
- [x] No screenshot test still captures an old-style dialog or settings screen.

## Implementation and verification

The Settings pages use Compose, backed by the existing keys, types and defaults. Deck and tag pickers, Manage Space, binding editors, Custom Study, Find and Replace, and Set Due Date use the native shared components. The old AndroidX preference presenters, custom preference Views, adapter-driven tag and note-type dialogs and dormant search and sort layouts have been removed. XML preference metadata still supplies the persisted settings definitions.

The integrated run executed 270 focused cases, with ten existing skips and two failures in Study Options. Its first refresh had opened the same recovery dialog twice. After the fix, all 23 Study Options and recovery tests passed, including the two previously failing cases. Rendered verification covered 240 tag and note-type mapping cases plus 144 Custom Study cases. The full-app run and the final About, sync and media presenter audits are tracked with tickets 44, 49 and 50.

The final presenter audit confirmed 26 retired files remain absent and inspected the 61 test files that capture screens or dialogs. The Settings pages and dialog capture routes use the Compose surfaces. Two Foundation layout tests passed on the single-activity integration.
