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
