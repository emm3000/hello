# 06: core/ui: wheel time picker

Status: ready-for-agent
Device: emulator
Blocked by: None
**Branch:** feat/06-wheel-time-picker

The owner chose to replace the Material3 clock dial inside `HTimePickerDialog` with a custom wheel picker. The new picker has an hour wheel and a minute wheel, and the selected row sits in a highlighted band. Both Settings time rows use the dialog: the study reminder and the daily words.
Gate row 3: a `core/ui` default that restyles every caller. Read `.claude/rules/ui-components.md`, `docs/DESIGN_BRIEF.md`, `core/theme/` (`Color.kt`, `Type.kt`, `Foundation.kt`) and `docs/SETTINGS_CURRENT.md`.

Decisions:
- **Callers stay untouched.** The public signature of `HTimePickerDialog` does not change, so `SettingsScreen` is not edited.
- **Format.** 24-hour, as today. The hour wheel runs 00 to 23, the minute wheel runs 00 to 55 in steps of 5, and both are cyclic.
- **Off-step minutes.** An `initialTime` whose minute is not a multiple of 5 opens on the nearest step, rounding half up and carrying into the hour (`09:58` opens on `10:00`, `23:58` on `00:00`). Saving writes the value shown.
- **Snapping.** Each wheel settles on a row with a fling, and a light haptic tick plays when the selected row changes.
- **Tokens.** No Material3 `TimePicker` remains. Every colour, size and type style comes from the theme tokens; add a token when one is missing. There is one accent at most: the Save `HButton`.
- **Accessibility.** Each wheel has semantics that TalkBack reads, such as "Hour, 9" or "Minuto, 30", and it can be adjusted with the accessibility increment and decrement actions. At a font scale of 1.3 nothing is clipped.

## Acceptance
- [ ] `rg 'material3.TimePicker' app/src/main/kotlin` prints nothing
- [ ] `git diff main -- app/src/main/kotlin/com/emm/hello/newfeatures/settings/` is empty
- [ ] JVM test for the pure logic (step rounding with the carry, cyclic wrap) passes in `:app` or `:domain`
- [ ] Evidence on the emulator, in English and Spanish, for both Settings time rows, saved as `.evidence/06-*.png`:
  - the dialog open on the current value
  - a mid-scroll frame
  - the value after saving, on the row
  - an off-step value that opens rounded
  - the dialog at a font scale of 1.3, with the font scale restored afterwards
- [ ] Changing the time on a row reschedules its work, the same as before the change (see `docs/NOTIFICATIONS_PLAN.md`)
- [ ] The `@PreviewLightDark` preview compiles
- [ ] `scripts/check` green
