# Recognise Hayaki from launcher to Welcome

Status: done
Type: AFK
User stories: 1, 2, 23

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Carry the iOS smiling folded-card mark into the Android launcher and Welcome screen. Record the iOS brand revision so maintainers can repeat the import.

## Acceptance criteria

- [x] On an installed Android build, the adaptive and legacy launcher entries and Welcome show the same mark as iOS.
- [x] Inspect the eyes, smile and folded corner at launcher size under round and squircle masks and in monochrome mode.
- [x] The recorded source identifies the icon artwork. A normal Android build uses checked-in assets.

## Blocked by

None - can start immediately

## Verification

The signed 0.2.0 APK appears in the stock launcher on API 24 as a legacy bitmap and on API 36 as an adaptive icon. The installed app’s Welcome screen on API 36 uses the same eyes, smile and folded corner. The PackageManager probe rendered the installed adaptive and monochrome artwork with both round and squircle masks. On API 24 the stock WebView displayed the app’s unsupported-WebView warning after launch. [API 24 launcher screenshots](https://github.com/amemiya02/Hayaki/actions/runs/44741108170/artifacts/14630507826) · [API 36 Welcome and mask captures](https://github.com/amemiya02/Hayaki/actions/runs/44739067873) · [Pinned artwork](https://github.com/amemiya02/Hayaki-iOS/blob/6c04596ceb4942eb21a540c16130a6bbe0290772/docs/brand/manifest.json)
