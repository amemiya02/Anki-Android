# Reveal and rate Cards from the same thumb area

Status: done
Type: AFK
User stories: 7, 13, 14, 15, 16, 17, 20

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Rework the Android Card Frame so reveal and grading remain within a stable thumb area. Preserve the existing reviewer inputs and make the same path usable on phones and wide displays.

## Acceptance criteria

- [x] Exercise two- and four-rating configurations and interval visibility. One tap, gesture or hardware grade produces exactly one review.
- [x] At 200% text the ratings can wrap to two columns and intervals remain readable. TalkBack identifies each action in order, targets meet 48 dp and reduced motion is respected.
- [x] Verify a readable centred width on a wide display. Review media and formulae and exercise the whiteboard, then return to grading.

## Blocked by

None - can start immediately

## Verification

All 13 AnswerArea, 7 StudyBar and 7 whiteboard gesture tests passed, along with the wide reviewer and 25 reviewer fragment tests. The accessibility click reveals the answer with a saved minimum hold, while a physical short tap produces its hint and a hold reveals only once. The installed shared fixture exercises four ratings and math. The signed upgrade walkthrough exercises the saved K binding with Again and Good enabled. [Controls and reviewer tests](https://github.com/amemiya02/Hayaki/actions/runs/44748772663) · [Installed upgrade](https://github.com/amemiya02/Hayaki/blob/redesign/docs/releases/0.2.0-upgrade.md)

On the signed release, TalkBack focus traversed Show Answer and the four ratings in order, each with its interval. Three Easy double-taps produced exactly three completed reviews. Both the phone and wide display used 200% text and disabled animation. [Signed-release TalkBack focus trace](https://github.com/amemiya02/Hayaki/blob/redesign/docs/releases/0.2.0-talkback.md)
