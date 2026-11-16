- Follow the repository's [AI policy](AI_POLICY.md).
- Keep changes focused on the requested task. Avoid unrelated refactors, formatting, and settings changes.
- Keep branch history linear. Do not create merge commits.
- For bug fixes, write a regression test that fails for the expected reason before the fix, then confirm it passes with the fix.

## Agent skills

### Issue tracker

Issues and specs live as markdown files under `.scratch/` in this repo. See `docs/agents/issue-tracker.md`.

### Triage labels

The five default triage roles, recorded as a `Status:` line in each issue file. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
