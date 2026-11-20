# Study across eligible Decks and stop on exit

Status: ready-for-agent
Type: AFK
User stories: 8, 10, 11

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Make Study All the primary daily action when work is available. Review the current eligible top-level Deck and continue to the next eligible Deck after the completion screen closes.

## Acceptance criteria

- [ ] With two due top-level Decks and a nested child, Study All covers eligible Cards according to the Engine without scheduling the child twice.
- [ ] After a Deck finishes, re-read eligibility before opening the next one. Skip a Deck that became ineligible and end when no work remains.
- [ ] An explicit exit from a running review returns to Decks. Launching only the current Deck never advances to another Deck. Check queue changes against the iOS fixture walkthrough.

## Blocked by

- [05 · Read today’s workload and start the current Deck](05-daily-decks.md)
