# Multi-session orchestration

How several Claude Code sessions work this repo in parallel: one orchestrator, one peer per ticket, one fresh reviewer per branch. Read this before dispatching a ticket. Model choices live only in `.claude/skills/wave/SKILL.md` §Decision Gates.

This is the wave mode. A change too small to ticket still runs in solo mode (`CLAUDE.md` §Work protocol): one session with its `writer`, `explorer` and `device-check` subagents on the owner's checkout.

## The unit

One ticket with `Status: ready-for-agent`: an acceptance list of falsifiable checks plus at most 3 lines of context. Where tickets live and how to query the board: `docs/agents/issue-tracker.md`; status strings: `docs/agents/triage-labels.md`. The board is always a query, never a list kept in a doc.

Tickets come from the `ticket-writer` agent once the owner and the orchestrator have agreed what the feature is. Read a ticket to the end: corrections to a stale body live in its comments.

## The loop

```
1. Orchestrator scopes the tickets and picks a row for each
2. /wave boots one peer per ticket: own worktree, branch <type>/<NN>-<slug>, commits, never pushes
3. Peer commits atomic changes, runs scripts/ready, reports the branch and short SHA it prints
4. Orchestrator launches reviewer (or reviewer-light) on the branch: MERGE or FIX FIRST
5. Orchestrator runs scripts/land, closes the cycle, logs the outcome
6. When the wave is landed: one push of main, then the next /wave
```

## Roles

- **Owner** approves the first wave of a session, checks `/model` in each peer pane (a session cannot see its own effort), and runs `gcloud auth login` once on the machine, so `scripts/appcheck-register` can register App Check debug tokens (§Devices).
- **Orchestrator** routes, reviews through agents and lands. It stays thin: investigation and anything that produces an artifact (tickets, specs, docs) goes to a subagent; inline work is routing state (`git status`, `git worktree list`, the board query, `ListAgents`) and at most 1-3 files. It reports to the owner in 1-2 lines and only decisions that are the owner's; landing a clean branch is routine. A peer's reply is a claim: verify it before acting on it.
- **Peer** carries one ticket in its own worktree (§Peer).
- **Reviewer** is the `reviewer` agent, or `reviewer-light`, fresh per branch and read-only.
- Refer to sessions with an `@` prefix in chat (`@capture`), so a session is never confused with a feature or ticket of the same name.

## Launching peers

- `/wave` is the only entry point (`.claude/skills/wave/SKILL.md`), typed by the owner or invoked by the orchestrator for the next wave.
- `scripts/wave [--terminal herdr|manual] <name>:<model>:<effort>:<NN> ...` validates every ticket and opens one herdr pane per peer in a tab labelled `wave`, each running `scripts/session`. Outside herdr it prints the `scripts/session` lines for the owner to run.
- `scripts/session <name> <model> <effort> <NN> <slot>` creates the worktree `../Hello-<name>` from local `main` when missing, checks out the ticket's `**Branch:**`, copies the four gitignored build files (`app/google-services.json`, `keystore.properties`, `local.properties`, `key/`) from the owner's checkout, points git at `.githooks`, and starts `claude -n <name>` in bypass mode with the ticket path and its device slot in the system prompt. `ListAgents` finds the peer by that name.

## Isolation

- **Worktree**: `../Hello-<name>`, cut from local `main`, never from `origin/main`. A peer never touches the owner's checkout. Review and verification run in throwaway worktrees under a scratchpad.
- **Host**: one Gradle build or emulator boot at a time on the machine, enforced by `scripts/host`, a lock in the shared git directory that waits rather than fails. `scripts/check` already takes it for its Gradle step. Wrap every other heavy command a peer runs: `scripts/host ./gradlew ...`, `scripts/host scripts/emulator <slot>`. _Why: in pichanga, overlapping native builds wedged an emulator three times; there the orchestrator handed out the host by message, which made it a bottleneck, so here a lock does it._
- A peer stops only processes it started, by PID. Never `pkill` or `killall` a shared name (`emulator`, `qemu-system`, `java`, `gradle`): it reaches other peers and other repos.
- A peer changes nothing outside its worktree: no global installs, no `brew`, no system config, no `git config --global`.

## Devices

- A pool of three AVDs, `hello-1`, `hello-2`, `hello-3`, one per wave slot, with fixed serials `emulator-5620`, `emulator-5622`, `emulator-5624`. `scripts/emulator <slot>` creates the AVD on first use (GPU on, 4 GB RAM) and boots it.
- The pool is kept between waves, and so is the app on it: install with `ANDROID_SERIAL=<serial> ./gradlew :app:installDebug` or `adb -s <serial> install -r`, never `pm clear` or uninstall. _Why: the debug App Check token lives in the app's storage; a wiped app mints a new one and every Gemini call fails with 403 until the owner registers it in the Firebase console._
- After every debug install, run `scripts/appcheck-register <serial>`. It reads the app's debug secret with `run-as`, and when the AVD's secret is new it registers it through the App Check REST API (replacing that AVD's previous token), so no Gemini call fails with 403. It needs `gcloud auth login` done once on this machine by the owner; when gcloud is not authenticated, report it to the orchestrator and continue with work that needs no AI call.
- `Medium_Phone*` AVDs and any other running emulator belong to the owner or other repos. Never install on them.
- Local Supabase: `adb -s <serial> reverse tcp:54321 tcp:54321` after every boot; `10.0.2.2` does not reach Docker on this Mac.
- `scripts/wave-close` shuts the slot's emulator down; the AVD stays.

## Peer

What every peer does, from boot to report:

1. Read the ticket to the end, then the docs it cites and the `.claude/rules/` that own the paths it touches.
2. The ticket's acceptance criteria are the contract and win over any file list; run every criterion check before reporting.
3. Behavior (a use case, a ViewModel rule, a repository function, a derived `UiState` field): load `mattpocock-skills:tdd`, failing test first. Pure layout or copy skips it.
4. A screen change: the evidence in §Evidence.
5. Atomic conventional commits on the ticket branch. `scripts/check` runs in the pre-commit hook and only runs what the staged paths need. A correction to an earlier commit of the branch is `git commit --fixup=<sha>`. Never push, never touch `main`, never merge.
6. Keep the slice small. Stop and report instead of widening scope. Past about 60% context, commit only finished changes, leave a half-done piece uncommitted, and report both.
7. Long commands: a commit, `scripts/ready` or a Gradle task can wait on the host lock and then build, past the Bash tool's 2-minute default. Run them with `timeout: 600000`, or `run_in_background`. If `scripts/ready` stops mid-rebase, fix and `git rebase --continue`, or `git rebase --abort` and rerun it.
8. Close-out: the last commit sets the ticket's `Status: done` and lists the evidence under `## Comments`. Then `scripts/ready` (fixups folded, rebased on `main`, the gate run per commit, a receipt written for the head). Message the orchestrator the line it prints, in 1-2 lines, with `SendMessage`.
9. On FIX FIRST: fix each blocking item as a `--fixup` of the commit it belongs to, rerun `scripts/ready`, report the new SHA.

## Evidence

A screen change ships with evidence the reviewer can open: for every changed screen and state, a screenshot from the peer's own emulator, `adb -s <serial> exec-out screencap -p > .evidence/<NN>-<state>-<shortsha>.png` in the worktree (gitignored), with the short SHA of the commit it was taken on. It stays valid while `git diff <shortsha> <branch> -- app/` is empty, so a later rebase or a fixup outside `app/` needs no new capture. Compare against `docs/DESIGN_BRIEF.md` and the feature's `docs/*_CURRENT.md`. List the absolute paths in the ticket comment. Check font scale 1.3 when the ticket names text layout.

## Review cycle

- A peer gets nothing new until its branch is reviewed, fixed and landed.
- The orchestrator launches `reviewer` with the branch name. A branch of only docs, copy, renames or a restyle that changes no `core/ui` default or theme token gets `reviewer-light`; its `ESCALATE` reply means rerun with `reviewer`.
- The reviewer does not rerun the gate: `scripts/ready` ran it per commit and left a receipt that `scripts/land` checks. It reads the diff, the ticket and the evidence.
- FIX FIRST goes back to the same peer as one message with the blocking items; the review runs again on the new SHA.
- Landing is fast-forward only, in the owner's checkout on `main`: `scripts/land <branch> <reviewed-sha>`. It refuses a head that is not the reviewed SHA, a head without a `scripts/ready` receipt, or a branch not on top of `main`; the peer then reruns `scripts/ready`. When only the base moved, `git range-diff main <reviewed-sha> <branch>` showing no change in the commits is enough to land the new head without a new review.
- Push: once per wave, after its last branch lands, `git push` from the owner's checkout. The owner's go on the first wave of a session authorizes these pushes for that session; a tag never. Every push to `main` triggers the release build workflow; a Play upload needs a `v*` tag, which stays the owner's call.

## Between tickets

A cycle is closed when all of this is done:

1. The branch is landed.
2. `scripts/wave-close <name>`: refuses a dirty worktree or an unlanded branch, shuts down the slot's emulator, removes the worktree, deletes the branch, prunes, and closes the herdr pane labelled `<name>` (an idle `claude` holds memory). Outside herdr the owner closes the pane.
3. One row per branch in `docs/agents/dispatch-log.md`; the wave's rows ship as one `docs(agents):` commit on `main`.
4. The orchestrator asks whether the merged work changed a feature's behavior or a decision; if so, the matching `docs/*_CURRENT.md` or `ARCHITECTURE.md` is fixed, by the peer in its branch or by a row-1 docs ticket.

When every branch of the wave is landed and pushed, the orchestrator starts the next `/wave` itself.
