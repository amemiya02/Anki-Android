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
