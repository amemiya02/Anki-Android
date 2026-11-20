# Expose 0.2.0 while preserving the install upgrade path

Status: ready-for-agent
Type: AFK
User stories: 21, 22, 23

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Give both editions the joint product version, preserving signed Android update eligibility and independently advancing build numbers on both platforms.

## Acceptance criteria

- [ ] Verify Android version name and each shipped ABI’s increasing effective version code. Preserve the application ID and signing identity.
- [ ] Verify the iOS host app and extensions agree on 0.2.0 and its iOS build number. Check both editions’ installed version labels against package metadata.
- [ ] Prepare release-note text explaining the transition from the inherited Android version sequence and how later single-platform hotfixes will identify their platform.

## Blocked by

None - can start immediately

## Verification

The previous signed build and all five Android 0.2.0 APKs used the same package ID and certificate. Each ABI's effective version code increased by 186, and the installed ARM64 About screen showed 0.2.0 after updating the existing installation. The iOS simulator artifact’s app, Share extension and Widgets extension each report 0.2.0 (7). [Signed Android package set](https://github.com/amemiya02/Hayaki/actions/runs/44738659742) · [Android installed upgrade record](https://github.com/amemiya02/Hayaki/blob/redesign/docs/releases/0.2.0-upgrade.md) · [iOS simulator package](https://github.com/amemiya02/Hayaki-iOS/actions/runs/44739923183/artifacts/14629944496)
