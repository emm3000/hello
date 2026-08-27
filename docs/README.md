# Docs

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual documentation index by feature |
| Source of Truth | No |
| Read this when | You want to understand current features without opening code at random |

This directory describes only currently implemented behavior.

## Read first

Global sources of truth still live at the root:

1. `README.md`
2. `CLAUDE.md`
3. `ARCHITECTURE.md`
4. `LOCAL_FIRST.md`

## Active documents

- `DASHBOARD_CURRENT.md`: current state of the `Dashboard` flow (list, search and filters)
- `CARD_CREATION_CURRENT.md`: current state of the `New Card` flow
- `CARD_DETAIL_CURRENT.md`: current state of the `Card Detail` flow (view and delete)
- `EDIT_FLASHCARD_CURRENT.md`: current state of the `Edit Flashcard` flow
- `DECK_CURRENT.md`: current state of the `Deck Detail` and `New/Edit Deck` flows
- `STUDY_CURRENT.md`: current state of the `Study` flow
- `SETTINGS_CURRENT.md`: current state of the `Settings` flow (export/import backup)
- `ONBOARDING_CURRENT.md`: current state of the first-run onboarding flow (welcome gate + seeded starter deck)

## Plans & history (not source of truth)

These documents capture planning context, decisions, and delivery history. They are kept for traceability but never override `*_CURRENT.md` files or the code.

- `RESTRUCTURE_PLAN.md` — Status: **Active**. Session-first product restructure (Hoy / Capturar / Biblioteca) in 5 phases; Phase 1 (Study session) done, Phase 2 (Capture) next.
- `DESIGN_BRIEF.md` — Status: **Active**. Visual direction and design rationale for the English-learning redesign; source of truth for visual decisions, input for the design canvas.
- `FEATURE_ROADMAP.md` — Status: **Active**. Prioritized list of pending features; ordering now deferred to `RESTRUCTURE_PLAN.md`.
- `LAUNCH_READINESS_AUDIT.md` — Status: **Active**. Pre-launch hardening audit; Sprint 2 items still open.
- `NOTIFICATIONS_PLAN.md` — Status: **Active**. Sprint 1 complete; Sprint 2 (settings toggle, i18n) pending.
- `privacy-policy.md` — Status: **Draft**. Minimal privacy policy covering local data, Firebase AI/Gemini, Crashlytics and Analytics. Pending public URL and Play Console Data Safety form.

## Rule

`*_CURRENT.md` files describe current code and are the source of truth for feature behavior. Plan and history docs are explicitly marked and kept for traceability — they never override current-behavior docs or the code.
