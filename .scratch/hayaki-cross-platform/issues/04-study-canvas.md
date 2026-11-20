# Apply Paper and Midnight to the study canvas

Status: done
Type: AFK
User stories: 4, 6, 15

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Carry a chosen Theme from Appearance settings into an actual review. Give ordinary Cards the appropriate study canvas and respect backgrounds supplied by a Card Template.

## Acceptance criteria

- [x] Paper and Midnight use the pinned light and dark study colours for Cards with no background of their own. Black uses a pure black default canvas.
- [x] A styled Card keeps its authored background. A following unstyled Card uses the selected Theme again.
- [x] Review and reveal Cards containing media and formulae after changing Theme and reopening the reviewer. Verify content and contrast.

## Blocked by

- [02 · Choose and keep a Hayaki Theme across screens](02-themes.md)

## Verification

On the API 36 emulator, AndroidJUnitRunner reported success for the installed reviewer’s template and plain-card sequence, including relaunch, local image decoding and MathJax. The Chromium comparison also covered Paper and Midnight in Light, Dark and Black. The authored background failed before the cascade fix and passed after it. [Instrumented results, phone-webview profile](https://github.com/amemiya02/Hayaki/actions/runs/44736135391) · [Cascade fix](https://github.com/amemiya02/Hayaki/commit/7c0f1b9c2c)
