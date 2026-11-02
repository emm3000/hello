# Dispatch log

Outcomes of every reviewed branch, used to tune `.claude/skills/wave/SKILL.md` §Decision Gates from evidence instead of opinion. The orchestrator adds one row to Recent per branch at cycle close. When Recent passes 20 rows, the oldest fold into Summary.

Cause, for a first-review FIX FIRST: `checklist` (a rule the peer had and missed), `judgment` (no rule covered it and the call was wrong), `spec` (the ticket was thin or wrong).

## Summary

One line per row and model:effort pair; a row that changes model gets a new line.

| Row | Model:effort | Branches | First-review MERGE | FIX FIRST: checklist | FIX FIRST: judgment | FIX FIRST: spec |
| --- | --- | --- | --- | --- | --- | --- |

## Recent

| Date | Branch | Ticket | Row | Model:effort | First review | Cause |
| --- | --- | --- | --- | --- | --- | --- |
