# Choose and keep a Hayaki Theme across screens

Status: ready-for-agent
Type: AFK
User stories: 3, 4, 7, 23

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Let a learner choose any of the ten iOS Themes on Android, independently of Appearance. Resolve accent and surface colours from the same pinned iOS brand definitions across the Android Foundation.

## Acceptance criteria

- [ ] All ten named choices appear. Hayaki defaults to #2C776E in light and #7ED6CA in dark. Compare theme IDs and colours against the recorded iOS revision.
- [ ] Choose a Theme, alternate System, Light, Dark and Black, and relaunch. The selected Theme persists and Black has a pure black default background.
- [ ] Walk from a Compose screen to a legacy Android surface and an Anki Page. The selection applies throughout. Statistics retains deck selection and PDF export.
- [ ] New, learning and due counts and Again, Hard, Good and Easy retain semantic colours and readable labels across Themes.

## Blocked by

None - can start immediately
