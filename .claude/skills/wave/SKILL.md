---
name: wave
description: Boot one peer Claude session per ready ticket in its own worktree and dispatch each ticket. Use on /wave, "levantar wave", "abrir peers", or when the orchestrator starts the next wave after the previous one is landed.
argument-hint: <ticket numbers>
allowed-tools: Bash(scripts/wave:*) Bash(scripts/read-ticket:*) Bash(git worktree:*) Bash(git status:*) Bash(fd:*) Bash(rg:*) ListAgents SendMessage Read
---

Each ticket becomes one peer session and one dispatch. Read `docs/agents/multi-session.md` first: its isolation, device and review rules bind every step here.

## Preconditions

Tickets are committed on `main` before the wave, since each worktree is cut from it. Stop and report when a ticket is missing, lacks a `**Branch:**` or `Device:` line, is not `ready-for-agent`, or has a `Blocked by` ticket that is not `Status: done`. Stop when a branch from the previous wave is still unlanded, or when `git status --short` in the owner's checkout shows changes under a path a ticket touches.

## Hard rules

- One ticket per peer, at most 3 peers at a time (one per device slot).
- No wave holds two tickets touching the same feature package, the same `core/ui` component or the same DI module. A SQLDelight migration is a wave of one.
- One explicit model and one explicit effort per ticket, stated to the owner before booting.
- Peer names are one lowercase word, taken from the slice (`capture`, `deck`).

## Decision Gates

Rows are ordered by blast radius: how much a mistake breaks and whether `scripts/check` catches it. A ticket matching several rows takes the highest. This table is the only home of the model choices; `docs/agents/dispatch-log.md` records how each row performs. Only the owner changes it, in one `chore(agents):` commit that also updates the `model`/`effort` frontmatter of `.claude/agents/*.md`.

| Row | Work | Model:effort | Extra instruction |
| --- | --- | --- | --- |
| 1 | Markdown, copy in `strings.xml` (both locales), renames, a curated deck's data, applying an already designed diff; every criterion is a command with fixed output | sonnet:low | none |
| 2 | Code the gate catches, in one feature: one screen, one ViewModel rule, one use case, one repository function with its test | opus:medium | `mattpocock-skills:tdd` for behavior; evidence for a screen |
| 3 | Traps where the gate stays green on a wrong answer: a `core/ui` `H*` default or theme token that restyles N screens, `navigation/`, `startup/`, a Koin module in `di/`, analytics events (the Play Data safety form depends on them), Gradle files, `libs.versions.toml`, `build-logic/`, detekt config, `AndroidManifest.xml`, a new dependency, `.github/`, `.claude/`, `scripts/`, `.githooks/` | opus:high | evidence of every affected screen; a new dependency names the build |
| 4 | A SQLDelight migration, backup/export, account deletion, App Check or auth, `supabase/functions/` or `supabase/migrations/`, anything that can lose user data | opus:high | wave of one for a migration; `.claude/rules/sqldelight.md` read before the first edit; a test per failure branch |
| 5 | Design: identity, icons, tokens or a screen mockup as an artifact | fable:high | no branch: the peer publishes the artifact, appends its URL under the ticket's `## Comments`, and the orchestrator sets `Status: done` on `main` |

| Role | Model:effort |
| --- | --- |
| `reviewer` agent | opus:high |
| `reviewer-light` agent: only docs, copy, renames or a restyle with no `core/ui` default or token change | sonnet:medium |
| `ticket-writer` agent | opus:medium |
| Orchestrator (set with `/model`) | opus:medium |

Every row is a bet until the dispatch log says otherwise. A row with two first-review FIX FIRST verdicts caused by `judgment` moves up one step; note the date and branches below.

- Inherited bet (pichanga, cabildo, myGame): row 2 starts at opus:medium, because a Sonnet row-2 run elsewhere shipped code the compiler accepted and the rules rejected.
- Inherited bet: a sweep that needs judgment per line (which duplicate is real, which comment survives) is row 3.

## Steps

1. Read each ticket to the end. Confirm status, blockers, `Device:` and `**Branch:**`; pick a peer name.
2. Classify each ticket with the table; a deviation carries a one-line reason. Tell the owner the plan, one line per ticket: `@<name> <NN>-<slug> <model>:<effort>`. The first wave of a session waits for the owner's go; later waves start on their own.
3. Run `scripts/wave <name>:<model>:<effort>:<NN> ...` once with every ticket. Slots follow argument order (first ticket slot 1). Pass `--terminal manual` when the owner is not in herdr, and give them the printed lines.
4. Poll `ListAgents` until every peer name appears, at most 90 seconds.
5. Send each peer one dispatch: the ticket path, its row and the row's extra instruction, the docs to read, "the ticket's acceptance criteria are the contract and win over any file list here", and "report the `scripts/ready` line to the session that sent this message, with SendMessage". The rest of the peer's duties are in `docs/agents/multi-session.md` §Peer, which its system prompt already points at.
6. Report to the owner in one or two lines: peers booted, tickets dispatched.

## Output

The list `@<name> <NN>-<slug> <model>:<effort>`, then nothing until a peer reports back.
