# Triage labels

The skills speak in five canonical triage roles. In the local markdown tracker the label is the value of the ticket's `Status:` line.

| Role in mattpocock/skills | `Status:` value | Meaning |
| --- | --- | --- |
| `needs-triage` | `needs-triage` | The orchestrator or the owner still has to evaluate it |
| `needs-info` | `needs-info` | A named fact is missing before it can be falsified |
| `ready-for-agent` | `ready-for-agent` | Fully specified; a peer can take it |
| `ready-for-human` | `ready-for-human` | Needs the owner (a console, a store, a device in hand) |
| `wontfix` | `wontfix` | Will not be done |

`done` closes a ticket and unblocks the tickets that list it under `Blocked by`.
