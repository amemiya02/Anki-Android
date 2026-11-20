# Keep existing night preferences after upgrading

Status: ready-for-agent
Type: AFK
User stories: 4, 5, 21

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Upgrade an existing Android installation into the new Theme picker, preserving both its active Appearance and any saved night palette.

## Acceptance criteria

- [ ] System with Black at night still follows Light by day and Black at night after upgrading and relaunching.
- [ ] Forced Light keeps the inactive night preference. Forced Dark and forced Black resolve as before. Upgraded learners start with the Hayaki Theme.
- [ ] Exercise older Plain and E-ink preferences through the historical conversion before interpreting reused values. Verify the resulting settings through the normal settings UI.

## Blocked by

- [02 · Choose and keep a Hayaki Theme across screens](02-themes.md)
