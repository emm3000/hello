# 06: core/ui: wheel time picker

Status: done
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
- [x] `rg 'material3.TimePicker' app/src/main/kotlin` prints nothing
- [x] `git diff main -- app/src/main/kotlin/com/emm/hello/newfeatures/settings/` is empty
- [x] JVM test for the pure logic (step rounding with the carry, cyclic wrap) passes in `:app` or `:domain`
- [x] Evidence on the emulator, in English and Spanish, for both Settings time rows, saved as `.evidence/06-*.png`:
  - the dialog open on the current value
  - a mid-scroll frame
  - the value after saving, on the row
  - an off-step value that opens rounded
  - the dialog at a font scale of 1.3, with the font scale restored afterwards
- [x] Changing the time on a row reschedules its work, the same as before the change (see `docs/NOTIFICATIONS_PLAN.md`)
- [x] The `@PreviewLightDark` preview compiles
- [x] `scripts/check` green

## Comments

Done on `7772db49` (`feat(ui): replace the Material clock in HTimePickerDialog with hour and minute wheels`), emulator `emulator-5620` (hello-1).

- `rg 'material3.TimePicker' app/src/main/kotlin` prints nothing; `git diff main -- app/src/main/kotlin/com/emm/hello/newfeatures/settings/` is empty.
- `WheelTimeTest` (11 tests: rounding half up with the carry, `23:58` to `00:00`, seconds ignored, cyclic wrap at 23/55 and negative indices, shortest path across the wrap) failed red against stubs, then passed green. The pre-commit `scripts/check` was green on the commit.
- Evidence, 20 captures in `/Users/emm/AndroidStudioProjects/Hello-wheel/.evidence/`, named `06-<en|es>-<reminder|words>-<state>-7772db49.png` with state `open`, `midscroll`, `saved`, `offstep`, `font13`:
  - open: reminder 19:00 and words 09:00 open on the current value in the band.
  - saved: en reminder 23:05, en words 10:20, es reminder 14:05, es words 01:20 shown on the rows after Save.
  - offstep: seeded 09:58 opens on 10:00, 23:58 on 00:00, 07:42 on 07:40, 18:03 on 18:05; Save then writes the rounded value.
  - font13: title, five rows per wheel and both buttons visible, nothing clipped; font scale restored to 1.0. The 32sp wheel digits do not grow at 1.3 (wheel bounds identical to 1.0) because of Android 14 non-linear font scaling; title and buttons do grow.
- Reschedule: `next_schedule_time_override` in `androidx.work.workdb` moved from 19:00 to 23:05 for `DueCardsReminderWorker` after saving the reminder, and from 09:00 to 10:20 for `DailyWordsWorker` after saving the words time; the other worker stayed unchanged each time.
- Accessibility: each wheel is an `android.widget.SeekBar` node with content-desc "Hour"/"Minute" ("Hora"/"Minuto"), its value as state description, and `setProgress` for increment and decrement. TalkBack itself was not enabled on the device.
- `WheelTimePickerPreview` is `@PreviewLightDark` and compiles in `:app:assembleDebug`.
