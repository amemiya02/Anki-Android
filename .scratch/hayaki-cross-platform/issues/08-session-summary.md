# Finish a session with its results and next action

Status: ready-for-agent
Type: AFK
User stories: 16, 17, 18, 19, 20

## Parent

[Hayaki 0.2.0 specification](https://github.com/amemiya02/Hayaki/issues/1)

## What to build

At Finished, present the completed session’s review count, elapsed time and rating distribution, followed by the reason study stopped and actions available for that Deck.

## Acceptance criteria

- [ ] Compare the distribution and count with a known sequence of ratings and the duration with observed session time. Entering an already-finished Deck shows its state without invented session figures.
- [ ] Exercise a cleared queue, a later learning Card, a daily limit and buried Cards. Verify the next time and applicable Unbury, Custom Study and Deck Options routes.
- [ ] Done remains reachable with TalkBack and 200% text on a phone and a wide display. Returning to Decks refreshes the counts.

## Blocked by

None - can start immediately
