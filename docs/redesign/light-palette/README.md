# Light palette candidates

The owner accepted every Mockup and asked whether the Light theme's colour scheme can change, without saying what they dislike. These pictures show four candidate Light palettes beside the current one, so one can be picked by eye. Each candidate is a complete Light palette: every role of the `light` block in `../quiet-paper.values.json`, plus the Light flag colours of `mockups/src/mock/find/flags.json`. **Dark and Black do not change.** Type, spacing and shape do not change either.

The candidates live in `mockups/src/mock/palette/palettes.json`. `node mockups/scripts/check-palette-contrast.mjs` checks each candidate and the current palette against the Quality Bar and passes for all of them.

Start the Mockups with `npm install && npm run dev` in `mockups/`, then open the URL in the last column. `lang` is `en` or `zh`.

## Pictures

Each row shows the same five Light screens: the deck list, the study screen (answer side), the card browser results, settings home and the destructive confirmation dialog.

| Picture | Shows | URL |
|---|---|---|
| `compare-zh.png`, `compare-en.png` | Every palette, one per row | `/?palette=compare&lang=zh` |
| `current-zh.png` | 现在 Current, for reference | `/?palette=current&lang=zh` |
| `plain-zh.png` | 白纸 Plain paper | `/?palette=plain&lang=zh` |
| `indigo-zh.png` | 藍 Indigo | `/?palette=indigo&lang=zh` |
| `celadon-zh.png` | 青瓷 Celadon | `/?palette=celadon&lang=zh` |
| `ink-zh.png` | 墨 Ink | `/?palette=ink&lang=zh` |

## The candidates

- **现在 Current** (background `#F5F0E6`, ink `#211E19`, primary `#A63D25`): warm cream paper, warm ink and cinnabar, as accepted.
- **白纸 Plain paper** (background `#FAFAF8`, ink `#1F1E1C`, primary `#A63D25`): clean near-white paper with ink text; cinnabar stays the one accent, so it still reads as Quiet paper, without the cream.
- **藍 Indigo** (background `#F2F3F6`, ink `#1A1D26`, primary `#2B3D78`): cool grey paper and deep indigo, like ai-zome cloth; calm and a little formal, the most like a tool.
- **青瓷 Celadon** (background `#EFF3F0`, ink `#1B2420`, primary `#1F5A52`): a very pale grey-green glaze with deep pine-teal; the softest on the eyes, quiet and a little cool.
- **墨 Ink** (background `#FFFFFF`, ink `#141414`, primary `#1C1C1C`): white paper and black ink; buttons are near-black and colour appears only where it means something (counts, answer buttons and marks, with a red-seal emphasis `#B0371F`).

In every candidate the count and answer colours keep their meaning (new blue, learning and Again red, due and Good green, Hard amber, Easy blue), retuned to sit on each background. The card sheet roles stand for how the app draws Card Content today, so they are the same everywhere.

### Every value

| Role | current | plain | indigo | celadon | ink |
|---|---|---|---|---|---|
| background | `#F5F0E6` | `#FAFAF8` | `#F2F3F6` | `#EFF3F0` | `#FFFFFF` |
| onBackground | `#211E19` | `#1F1E1C` | `#1A1D26` | `#1B2420` | `#141414` |
| onBackgroundMuted | `#675F53` | `#625F59` | `#5A606E` | `#55625B` | `#5E5E5E` |
| outline | `#D8CFBE` | `#E2E0DB` | `#DADDE4` | `#D4DDD7` | `#E4E4E4` |
| primary | `#A63D25` | `#A63D25` | `#2B3D78` | `#1F5A52` | `#1C1C1C` |
| onPrimary | `#FFF9F2` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` |
| emphasis | `#A63D25` | `#A63D25` | `#2B3D78` | `#1F5A52` | `#B0371F` |
| countNew | `#2F4A6B` | `#2F4A6B` | `#22598A` | `#2D4E78` | `#1F4E8C` |
| countLearning | `#A63D25` | `#A63D25` | `#A3343A` | `#A13F2A` | `#B0371F` |
| countDue | `#3E5B34` | `#3E5B34` | `#2E6343` | `#3D6128` | `#2D6A36` |
| answerAgain | `#A63D25` | `#A63D25` | `#A3343A` | `#A13F2A` | `#B0371F` |
| answerHard | `#8A5E10` | `#8A5E10` | `#86600F` | `#85600F` | `#8F6100` |
| answerGood | `#3E5B34` | `#3E5B34` | `#2E6343` | `#3D6128` | `#2D6A36` |
| answerEasy | `#2F4A6B` | `#2F4A6B` | `#22598A` | `#2D4E78` | `#1F4E8C` |
| progressTrack | `#DDD4C3` | `#E6E4DF` | `#DCDFE6` | `#D6DED9` | `#E8E8E8` |
| progressFill | `#211E19` | `#1F1E1C` | `#1A1D26` | `#1B2420` | `#141414` |
| cardSheet | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` |
| onCardSheet | `#000000` | `#000000` | `#000000` | `#000000` | `#000000` |
| cardSheetRule | `#8B8B8B` | `#8B8B8B` | `#8B8B8B` | `#8B8B8B` | `#8B8B8B` |
| flag red | `#A63D25` | `#A63D25` | `#A3343A` | `#A13F2A` | `#B0371F` |
| flag orange | `#8A5E10` | `#8A5E10` | `#86600F` | `#85600F` | `#8F6100` |
| flag green | `#3E5B34` | `#3E5B34` | `#2E6343` | `#3D6128` | `#2D6A36` |
| flag blue | `#2F4A6B` | `#2F4A6B` | `#22598A` | `#2D4E78` | `#1F4E8C` |
| flag pink | `#A3436F` | `#A3436F` | `#9E3F6E` | `#9E4270` | `#A8406F` |
| flag turquoise | `#256B66` | `#256B66` | `#216A68` | `#1F6A6A` | `#1F6B66` |
| flag purple | `#6B4A8E` | `#6B4A8E` | `#64479A` | `#6A4A8C` | `#6A45A0` |

### Contrast

Every pairing `scripts/check-contrast.mjs` checks (text 4.5:1, marks 3:1), the two the card browser adds (`scripts/check-find-contrast.mjs`), and each flag colour as a mark (3:1) on the background. Every value passes.

| Pairing | Needs | current | plain | indigo | celadon | ink |
|---|---|---|---|---|---|---|
| onBackground on background | 4.5 | 14.62 | 15.94 | 15.17 | 14.21 | 18.42 |
| onBackgroundMuted on background | 4.5 | 5.54 | 6.09 | 5.68 | 5.71 | 6.48 |
| onPrimary on primary | 4.5 | 6.06 | 6.33 | 10.29 | 7.95 | 17.04 |
| countNew on background | 4.5 | 7.99 | 8.69 | 6.61 | 7.58 | 8.31 |
| countLearning on background | 4.5 | 5.58 | 6.06 | 6.09 | 5.75 | 6.15 |
| countDue on background | 4.5 | 6.72 | 7.30 | 6.35 | 6.38 | 6.51 |
| emphasis on background | 4.5 | 5.58 | 6.06 | 9.27 | 7.09 | 6.15 |
| onCardSheet on cardSheet | 4.5 | 21.00 | 21.00 | 21.00 | 21.00 | 21.00 |
| primary on background | 3 | 5.58 | 6.06 | 9.27 | 7.09 | 17.04 |
| answerAgain on background | 3 | 5.58 | 6.06 | 6.09 | 5.75 | 6.15 |
| answerHard on background | 3 | 5.01 | 5.45 | 5.12 | 5.10 | 5.42 |
| answerGood on background | 3 | 6.72 | 7.30 | 6.35 | 6.38 | 6.51 |
| answerEasy on background | 3 | 7.99 | 8.69 | 6.61 | 7.58 | 8.31 |
| progressFill on progressTrack | 3 | 11.29 | 13.11 | 12.62 | 11.60 | 15.04 |
| onBackground on progressTrack | 4.5 | 11.29 | 13.11 | 12.62 | 11.60 | 15.04 |
| background on emphasis | 3 | 5.58 | 6.06 | 9.27 | 7.09 | 6.15 |
| flag red on background | 3 | 5.58 | 6.06 | 6.09 | 5.75 | 6.15 |
| flag orange on background | 3 | 5.01 | 5.45 | 5.12 | 5.10 | 5.42 |
| flag green on background | 3 | 6.72 | 7.30 | 6.35 | 6.38 | 6.51 |
| flag blue on background | 3 | 7.99 | 8.69 | 6.61 | 7.58 | 8.31 |
| flag pink on background | 3 | 5.15 | 5.60 | 5.58 | 5.41 | 5.79 |
| flag turquoise on background | 3 | 5.48 | 5.96 | 5.69 | 5.63 | 6.26 |
| flag purple on background | 3 | 6.17 | 6.71 | 6.50 | 6.31 | 7.07 |

## Decision for the owner

**Which Light palette?** Recommended: **白纸 Plain paper**.

- It is the smallest change that answers the most likely complaint, that the cream looks yellow or dated. Everything else the owner accepted stays: cinnabar buttons and marks, the count and answer colours, the flags.
- Dark and Black stay warm with a cinnabar primary. Plain paper and Ink keep a red accent, so switching theme still feels like the same app. Indigo and Celadon would give Light a different accent from Dark and Black; picking either should come with matching Dark and Black accents, which were not asked for and are not drawn.
- In Indigo and Celadon the emphasis colour is the primary hue, so the destructive dialog's button ("Replace collection") is indigo or teal rather than red. It is still named for what it does and marked with the warning sign, but if the owner picks either, a separate red `danger` role (the answerAgain colour) is worth adding.
- Alternative: **墨 Ink**, if the owner wants the plainest, most neutral look. Its near-black buttons are the strongest contrast of all, but the screen loses the cinnabar warmth that gives Quiet paper its character.

## Not drawn

- English pictures of single candidates: `compare-en.png` shows every candidate in English; `/?palette=<name>&lang=en` draws one.
- Dark and Black for the candidates: they do not change.
