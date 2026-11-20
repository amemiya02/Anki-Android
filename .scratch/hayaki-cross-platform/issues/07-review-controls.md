# Reveal and rate Cards from the same thumb area

Status: ready-for-agent
Type: AFK
User stories: 7, 13, 14, 15, 16, 17, 20

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Rework the Android Card Frame so reveal and grading remain within a stable thumb area. Preserve the existing reviewer inputs and make the same path usable on phones and wide displays.

## Acceptance criteria

- [ ] Exercise two- and four-rating configurations and interval visibility. One tap, gesture or hardware grade produces exactly one review.
- [ ] At 200% text the ratings can wrap to two columns and intervals remain readable. TalkBack identifies each action in order, targets meet 48 dp and reduced motion is respected.
- [ ] Verify a readable centred width on a wide display. Review media and formulae and exercise the whiteboard, then return to grading.

## Blocked by

None - can start immediately
