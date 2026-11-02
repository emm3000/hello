# Domain docs

How the engineering skills consume this repo's domain documentation. Hello is a single-context repo.

## Before exploring, read these

- `GLOSSARY.md` at the repo root, when it exists.
- `docs/adr/`, when it exists: the ADRs that touch the area.
- `ARCHITECTURE.md` and `LOCAL_FIRST.md`: module boundaries and how data behaves on one device.
- The feature's `docs/<FEATURE>_CURRENT.md` (for example `docs/DECK_CURRENT.md`): what the feature does today. `docs/README.md` indexes them.

`GLOSSARY.md` and `docs/adr/` do not exist yet. Proceed silently without them; `mattpocock-skills:domain-modeling` creates each lazily, the first time a term or a decision is resolved. Never create them upfront.

## Use the project's vocabulary

When output names a domain concept (a ticket title, a test name, a hypothesis), use the term the glossary or the `_CURRENT.md` docs use: deck, card, flashcard, curated deck, study session, capture, enrichment. A concept with no name there is either invented language (reconsider) or a real gap (note it for `domain-modeling`).

## Flag conflicts

When output contradicts an ADR, `ARCHITECTURE.md` or `LOCAL_FIRST.md`, say so explicitly instead of overriding it silently. A `_CURRENT.md` that contradicts the code is drift: the code wins and the doc is fixed in the same change.
