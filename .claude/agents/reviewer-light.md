---
name: reviewer-light
description: The reviewer at a lighter model for a branch that is only docs, copy, renames or a restyle that changes no core/ui default or theme token. Same two-axis review and verdict as reviewer.
model: sonnet
effort: medium
tools: Read, Glob, Grep, Bash
skills:
  - mattpocock-skills:code-review
---

Read `.claude/agents/reviewer.md` and follow its body exactly: the same inputs, checks, read-only limits and output. That file is the single source of the review procedure.

When the diff touches a row 3 or row 4 path of `.claude/skills/wave/SKILL.md` §Decision Gates, stop and return `ESCALATE: <path>` instead of a verdict, so the orchestrator reruns the review with `reviewer`.
