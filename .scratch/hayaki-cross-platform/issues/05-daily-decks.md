# Read today’s workload and start the current Deck

Status: done
Type: AFK
User stories: 7, 8, 9, 11, 12, 17

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

Show remaining work, reviews already completed and the three queue counts on Decks. Preserve the direct action for an eligible current Deck and the distinct empty and finished states.

## Acceptance criteria

- [x] The summary shows all three queue categories, including zero. Deck rows show nonzero counts beside their own Deck names.
- [x] Verify an empty collection has a path to get started, and a populated collection with nothing due shows today is clear.
- [x] Start just the current Deck, return and observe fresh counts. Check a narrow layout, deeply nested Chinese and Japanese names, and TalkBack reading order.

## Blocked by

None - can start immediately

## Verification

The 6 DailyDecks and 14 DeckListScreen tests passed. They cover the collapsed current Deck, its direct study action, refreshed counts, an empty collection, a cleared day, nested Chinese and Japanese names, and the large-text layout. The installed shared fixture begins at 5 new, 2 learning and 2 due, then returns with 3 new, 2 learning and 1 due after three Easy ratings. [Regression XML](https://github.com/amemiya02/Hayaki/actions/runs/44748772663) · [Installed phone and wide layouts](https://github.com/amemiya02/Hayaki/actions/runs/44748906084)

On a phone and a wide display at 200% font scale, TalkBack traversed the Today summary, the named new, learning and due queues, then Study All. After the second Deck was left early, Decks reported six Cards left and three studied. [Signed-release TalkBack focus trace](https://github.com/amemiya02/Hayaki/blob/redesign/docs/releases/0.2.0-talkback.md)

A clean API 36 installation started with a seeded Deck and one new Card. A touch on its localized queue count opened Study Options. The isolated release instrumentation reported one executed test with no failures or skips. [Cold-start Deck count test](https://github.com/amemiya02/Hayaki/actions/runs/44760561739)
