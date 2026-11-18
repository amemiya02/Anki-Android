# 36: Settings: home, search and appearance

**What to build:** The owner opens redesigned settings, finds a setting by browsing its group or by searching, and changes the theme there.

**Blocked by:** 32 (Mockups for settings and dialogs)

**Status:** done

- [x] Settings home shows the groups from the Mockup.
- [x] Search finds any setting by name and opens it.
- [x] The appearance group is complete: Light, Dark, Black and follow-system, plus every other appearance setting that exists today.
- [x] Changing a setting takes effect as it does today.
- [x] Settings is reachable from More.
- [x] On a tablet-sized screen the surface still works in its existing layout.
- [x] It matches the agreed Mockup.
- [x] It passes the Quality Bar at the rendered-screen seam: Light, Dark and Black; English, Simplified Chinese, Japanese and one right-to-left language; normal and 200% font size.
- [x] Screen-logic tests and Engine tests still pass unchanged.

## Verification

All 96 rendered cases passed on phone and tablet in Light, Dark and Black across English, Simplified Chinese, Japanese and Arabic at normal and 200% text. The tests produced 192 captures covering home, search, Appearance and the split layout. Settings search navigation passed in the integrated Studying implementation, including the Timebox destination. The Engine passed 143 cases with seven existing skips.
