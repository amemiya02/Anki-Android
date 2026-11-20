# Study across eligible Decks and stop on exit

Status: done
Type: AFK
User stories: 8, 10, 11

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Make Study All the primary daily action when work is available. Review the current eligible top-level Deck and continue to the next eligible Deck after the completion screen closes.

## Acceptance criteria

- [x] With two due top-level Decks and a nested child, Study All covers eligible Cards according to the Engine without scheduling the child twice.
- [x] After a Deck finishes, re-read eligibility before opening the next one. Skip a Deck that became ineligible and end when no work remains.
- [x] An explicit exit from a running review returns to Decks. Launching only the current Deck never advances to another Deck. Check queue changes against the iOS fixture walkthrough.

## Blocked by

- [05 · Read today’s workload and start the current Deck](05-daily-decks.md)

## Verification

Android’s restored reviewer and Finished paths passed, including system Back after rotation, skipping an ineligible Deck, preserving the completion summary if the Deck refills, and choosing another eligible top-level Deck first. On iOS the same eligibility regression failed before the fix and passed afterward. The fixture contains Geography and Languages::Spanish. [Android results](https://github.com/amemiya02/Hayaki/actions/runs/44748772663) · [iOS regression before](https://github.com/amemiya02/Hayaki-iOS/actions/runs/44741765551) · [iOS regression after](https://github.com/amemiya02/Hayaki-iOS/actions/runs/44741885985)

The installed iOS candidate at `cc34221` finished Geography after three Easy ratings on both iPhone and iPad. Done opened Languages and early exit returned to the same 3 new, 2 learning, 1 due and 3 studied seen on Android. The iPad next Card was “adiós”. [Device results and screenshots](https://github.com/amemiya02/Hayaki/blob/redesign/docs/releases/0.2.0.md)
