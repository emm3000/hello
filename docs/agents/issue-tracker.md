# Issue tracker: local markdown

Wave tickets live as markdown files in `.scratch/`, tracked in git, so every worktree and branch carries its own copy and a status change rides in the commit that earns it. GitHub issues stay for owner-facing work outside a wave (store assets, naming research); a wave ticket may cite one as `#N`.

## Conventions

- One feature per directory: `.scratch/<feature-slug>/`. Its spec, when there is one, is `spec.md`; the wave plan is `waves.md` beside it.
- One file per ticket: `.scratch/<feature-slug>/issues/<NN>-<slug>.md`. `NN` is unique across the repo, the next free number: `fd --no-ignore '^[0-9]+-' .scratch | rg -o '/([0-9]+)-' -r '$1' | sort -n | tail -1`.
- Near the top, plain lines: `Status:`, `Device: emulator|none`, `Blocked by: NN, NN | None`, `**Branch:** <type>/<NN>-<slug>`. `scripts/read-ticket` reads `Device:` and `**Branch:**`.
- `Device: emulator` when the work needs a screen or an install to verify; `none` otherwise.
- A finished ticket's last commit sets `Status: done`, the status `Blocked by` waits on.
- Comments append under a `## Comments` heading at the bottom.

## Format

```
# NN: <Area>: <slice>

Status: ready-for-agent
Device: emulator | none
Blocked by: None
**Branch:** <type>/NN-<slug>

<at most 3 lines of context, then pointers: docs/*_CURRENT.md, rule, files>

## Acceptance
- [ ] ...
- [ ] `scripts/check` green
```

## The board

```bash
rg -l '^Status: ready-for-agent' .scratch/*/issues
```

## When a skill says "publish to the issue tracker"

Create a file under `.scratch/<feature-slug>/issues/` as above.

## When a skill says "fetch the relevant ticket"

`scripts/read-ticket <NN> path`, then read that file.
