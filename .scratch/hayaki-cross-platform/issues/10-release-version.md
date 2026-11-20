# Expose 0.2.0 while preserving the install upgrade path

Status: done
Type: AFK
User stories: 21, 22, 23

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Give both editions the joint product version, preserving signed Android update eligibility and independently advancing build numbers on both platforms.

## Acceptance criteria

- [x] Verify Android version name and each shipped ABI’s increasing effective version code. Preserve the application ID and signing identity.
- [x] Verify the iOS host app and extensions agree on 0.2.0 and its iOS build number. Check both editions’ installed version labels against package metadata.
- [x] Prepare release-note text explaining the transition from the inherited Android version sequence and how later single-platform hotfixes will identify their platform.

## Blocked by

None - can start immediately

## Verification

The previous signed build and all five Android 0.2.0 APKs used the same package ID and certificate. Each ABI's effective version code increased by 186, and the installed ARM64 About screen showed 0.2.0 after updating the existing installation. The iOS simulator artifact’s app, Share extension and Widgets extension each report 0.2.0 (7). [Signed Android package set](https://github.com/amemiya02/Hayaki/actions/runs/44746442899) · [Android installed upgrade record](https://github.com/amemiya02/Hayaki/blob/redesign/docs/releases/0.2.0-upgrade.md) · [iOS simulator package](https://github.com/amemiya02/Hayaki-iOS/actions/runs/44755687099/artifacts/14638546672)

The iPhone About screen showed the same 0.2.0 (7) as its package and opened the Changelog. The shipped build number is independent of Android’s per-ABI version code. [Installed About test and XCTest results](https://github.com/amemiya02/Hayaki-iOS/actions/runs/44755687099/artifacts/14640571286)

The final iOS simulator package built from `cc34221` had archive SHA-256 `0097a7a4e670fda82a55b53574fe2fe14d49c546cc433c9f22bccab0ff505f0d`. The joint verifier confirmed version 0.2.0 (7) for the app and both extensions and the upgrade identity and version codes for all five Android packages. [Final iOS package](https://github.com/amemiya02/Hayaki-iOS/actions/runs/44755687099/artifacts/14638546672) · [Final Android packages](https://github.com/amemiya02/Hayaki/actions/runs/44746442899/artifacts/14633647903)
