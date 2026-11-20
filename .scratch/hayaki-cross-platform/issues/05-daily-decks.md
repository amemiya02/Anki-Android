# Read today’s workload and start the current Deck

Status: ready-for-agent
Type: AFK
User stories: 7, 8, 9, 11, 12, 17

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Show remaining work, reviews already completed and the three queue counts on Decks. Preserve the direct action for an eligible current Deck and the distinct empty and finished states.

## Acceptance criteria

- [ ] The summary shows all three queue categories, including zero. Deck rows show nonzero counts beside their own Deck names.
- [ ] Verify an empty collection has a path to get started, and a populated collection with nothing due shows today is clear.
- [ ] Start just the current Deck, return and observe fresh counts. Check a narrow layout, deeply nested Chinese and Japanese names, and TalkBack reading order.

## Blocked by

None - can start immediately
