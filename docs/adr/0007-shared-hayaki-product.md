---
status: accepted
---

# Share Hayaki's brand, daily study experience and public release version

Android and iOS ship one product from separate repositories. The iOS edition supplies the app icon, ten Themes and interaction reference for the Core Loop. Android retains its existing capability inventory and implements these interactions in Jetpack Compose and Material 3, so a learner can change devices and still find the same actions while using the accessibility, navigation and input conventions of each platform.

## Consequences

- This replaces the fixed purple palette in [ADR-0006](0006-direction-material-3-standard.md). Appearance and Theme are independent. Card counts and ratings retain their meanings across Themes. Card Templates control their own Card Content.
- Android presents its existing Anki Pages through Harmonisation and aligns their navigation with the rest of the product. Statistics retains its deck filter and PDF export. [ADR-0003](0003-interface-rebuilt-natively-in-compose.md) continues to govern the Engine and UI boundary.
- Joint releases share a public product version and release notes. Each platform advances its build number according to its upgrade and packaging requirements. A platform-specific hotfix may ship on its own and its release notes identify the affected platform.
- The [iOS repository](https://github.com/amemiya02/Hayaki-iOS) owns the brand manifest at `docs/brand/manifest.json`. Android vendors its resources and records the source revision. The two repositories can build independently. A joint release identifies the exact pair of revisions so maintainers can reproduce what learners received on each platform.

### Updating the shared brand

Change the artwork or palette in Hayaki-iOS first. In Android, set the revision in `docs/brand/themes.json` to the iOS commit containing `docs/brand/manifest.json`. The icon importer reads that revision too. Copy the named accents and study canvases into the Android palette file, then run the icon importer with the iOS checkout and run `python3 tools/foundation/generate_colors.py`. Commit the generated resources with the updated pin. The generator checks text contrast for all ten themes in Light, Dark and Black.

For each joint release, the Android release notes identify the shared public version. Verify the previous and candidate APK sets and the iOS app and extensions with `python3 tools/joint-release/verify.py --help`. The release record links the two tested source revisions, their platform build numbers and the device results.
