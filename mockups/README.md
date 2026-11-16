# Mockups

Disposable web Mockups for the Redesign, built with React, Tailwind and shadcn/ui.
They are pictures of screens to choose from, not app code. Only design values
(colour, type, spacing, shape) carry over to Android.

```bash
npm install
npm run dev
```

- `/` shows all three Directions.
- `/?d=paper`, `/?d=crisp` or `/?d=soft` shows one Direction.
- `/?compare=decks&lang=zh` shows one screen across the three Directions
  (`decks`, `question` or `answer`; `en` or `zh`).

- `/?d=paper&theme=dark` (or `black`) shows the chosen Direction in another theme.
- `/?themes=decks&lang=zh` shows one screen in Light, Dark and Black.
- `/?identity` shows the name and launcher icon candidates.
- `/?core=overview&lang=en` shows every Core Loop surface; `/?core=<surface>&lang=zh`
  shows one in Light, Dark and Black, and `/?decide=counts` (or `studybar`,
  `buttons`) shows a decision the owner makes. The surfaces are listed in
  `docs/redesign/core-loop/README.md`.

The chosen Direction, Quiet paper, takes its colours from
`docs/redesign/quiet-paper.values.json`. Check every colour pairing in that
file against the Quality Bar with:

```bash
node scripts/check-contrast.mjs
```

Captured pictures are in `docs/redesign/directions/`,
`docs/redesign/quiet-paper/` and `docs/redesign/core-loop/`.
