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

- `TODAY_CURRENT.md`: current state of the `Today` flow (session-first home)
- `LIBRARY_CURRENT.md`: current state of the `Library` flow (all cards, content search)
- `CAPTURE_CURRENT.md`: current state of the `Capture` flow (add a word)
- `CARD_DETAIL_CURRENT.md`: current state of the `Card Detail` flow (view and delete)
- `EDIT_FLASHCARD_CURRENT.md`: current state of the `Edit Flashcard` flow
- `DECK_CURRENT.md`: current state of the `Decks` and `New/Edit Deck` flows
- `STUDY_CURRENT.md`: current state of the `Study` flow
- `SETTINGS_CURRENT.md`: current state of the `Settings` flow (export/import backup, daily study reminder)
- `ONBOARDING_CURRENT.md`: current state of the first-run onboarding flow (welcome gate + seeded starter deck)
- `SUGGEST_CURRENT.md`: current state of the `Suggest` flow (a situation and new words for a zero-due day)

## Plans & history (not source of truth)

These documents capture planning context, decisions, and delivery history. They are kept for traceability but never override `*_CURRENT.md` files or the code.

- `RESTRUCTURE_PLAN.md` — Status: **Active**. Session-first product restructure (Hoy / Capturar / Biblioteca) in 5 phases; Phases 1, 2, 4 and 5 done, Phase 3 open only on its zero-due AI suggestion.
- `REDESIGN_PLAN.md` — Status: **Active**. The hybrid redesign (light page, ink controls, color-block cards, English chrome) in 10 stages of atomic units, each with its falsifier; source of truth for stage order and scope. Stages 0–8 and 9.1 are shipped; only 9.2 (this docs pass) remains.
- `DESIGN_BRIEF.md` — Status: **Active**. Rationale for the learning mechanics (grade rules, two-beat card, direction by maturity) stays active; its visual sections were resynced to the hybrid design on 2026-08-28.
- `FEATURE_ROADMAP.md` — Status: **Active**. Prioritized list of pending features; ordering now deferred to `RESTRUCTURE_PLAN.md`.
- `LAUNCH_READINESS_AUDIT.md` — Status: **Active**. Pre-launch hardening audit; Sprint 2 items still open.
- `NOTIFICATIONS_PLAN.md` — Status: **Active**. Sprint 1 and Sprint 2 both complete (toggle, time picker, deep link); only the runtime notification-permission request (`F-Onboarding-Consent`) remains open.
- `privacy-policy.md` — Status: **Draft**. Minimal privacy policy covering local data, Firebase AI/Gemini, Crashlytics and Analytics. Pending public URL and Play Console Data Safety form.

## Rule

`*_CURRENT.md` files describe current code and are the source of truth for feature behavior. Plan and history docs are explicitly marked and kept for traceability — they never override current-behavior docs or the code.
