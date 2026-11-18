# 38: Settings: everything else

**What to build:** Every remaining setting is in the redesigned settings.

**Blocked by:** 36 (Settings: home, search and appearance)

**Status:** done

- [x] Every remaining group is present and works: general, sync, notifications and reminders, accessibility, backups, advanced, and developer options.
- [x] No setting that exists today is missing; a list of settings before and after shows this.
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Verification

The [parity inventory](../../../docs/redesign/settings-and-dialogs/settings-parity.tsv) maps 208 existing entries. The eight migrated pages keep the original settings keys and dynamic availability. The filename setting failed on the pre-migration General screen and passed on the new screen. All 22 focused settings tests passed, including Advanced, profiles, search and sync setup. The Engine suite had 143 passes and seven skips.

All 288 rendered cases passed across three themes, four languages and both font sizes. The initial pass flagged a partially clipped numeric value on the Arabic developer page at 200%. The page fixture now dispatches the 24 dp gesture-navigation inset of an edge-to-edge phone. All 192 first/last-row cases then passed with accessibility scanning, alongside the 96 passing choice, sync and separately varied inset cases.
