# Hello

| Field | Value |
|---|---|
| Status | Active |
| Role | Main entry point for the repo |
| Source of Truth | Partial |
| Read this when | You're entering the project for the first time |

Android app in Kotlin + Jetpack Compose for studying with decks and flashcards.

## Current state

- local-first single-device product
- `HelloDb` is the source of truth for reads and writes
- startup ensures local install identity, seeds starter data and reads the onboarding flag (`hasSeenWelcome`)
- no active runtime for remote sync, pairing or remote bootstrap

## Modules

- `app`: UI, navigation, DI and startup
- `data`: repositories, SQLDelight, local identity and Firebase AI
- `domain`: JVM-only models and use cases

## Reading order

1. `CLAUDE.md`
2. `ARCHITECTURE.md`
3. `LOCAL_FIRST.md`
4. `docs/README.md`
5. `docs/DESIGN_BRIEF.md`

## Active docs

- `CLAUDE.md`: operating manifest; detailed code rules live in `.claude/rules/`
- `ARCHITECTURE.md`: current technical structure
- `LOCAL_FIRST.md`: current runtime contract
- `docs/CARD_CREATION_CURRENT.md`: current card creation flow
- `docs/STUDY_CURRENT.md`: current study flow
- `docs/DESIGN_BRIEF.md`: visual direction and design rationale

## Don't assume

- multi-device
- active remote sync
- that legacy config references imply active behavior
