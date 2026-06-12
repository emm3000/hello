# Redesign Plan — Ember Dark

| Field | Value |
|---|---|
| Status | Closed |
| Role | History of the Ember dark visual redesign (all phases complete) |
| Source | Designer handoff bundle (`/tmp/hello-design/hello/`) + on-screen analysis |
| Read this when | Tracing the rationale of a visual decision or auditing a shipped phase |
| Last verified | 2026-06-11 |

## Progress

| Phase | Status | Commits |
|---|---|---|
| 0 — Tokens, fonts, theme | ✅ Done | `dd81c91`, `0ed4180` |
| 1 — H* components | ✅ Done | `efd4db3` |
| 2.1 — Dashboard | ✅ Done (empty-state polished against designer mock) | `a54cdf4` |
| 2.2 — Study | ✅ Done (sub-1 StudyTop + sub-2 Start/Empty + sub-3 Recall/Check + sub-4 Grade/Mismatch) | `4ba50d2`, `83c2376`, `e6efe73`, `f6b9c2d` |
| 2.3 — New Card wizard | ✅ Done (sub-1 Mode + sub-2 Input + sub-3 Review + sub-4 Loading/Error) | `a88ffd7`, `56ace4c`, `5d83b4c`, `a9351da` |
| 2.4 — Card Detail | ✅ Done (dict-style scroll, new HDictSense) | `e7e2aa6` |
| 2.5 — Deck Detail | ✅ Done (sub-1 chrome + Ember header + sub-2 grouped card list) | `113a96d`, `7a0ec3d` |
| 2.6 — New/Edit Deck | ✅ Done (1 commit) | `67e7bbf` |
| 2.7 — Edit Flashcard | ✅ Done (1 commit) | `3ab5de1` |
| 2.8 — Settings | ✅ Done (1 commit) | `207d384` |
| 3 — Missing screens/states | ✅ Done | `29c4443` |
| 4 — Microcopy pass | ✅ Done | `7ae81bb` |
| 5 — Cleanup | ✅ Done (5a dead code + 5b detekt clean + 5c docs resync) | `4d98203`, `0fd7c4f`, `fe221b3` |

**Phase 2.8 shipped as a single commit:**
- ✅ `207d384` (2026-05-22) — Settings. `HTopBar` (back only) replaces the M3 `TopAppBar`. New `SettingsHeader`: mono 11sp `AJUSTES` eyebrow + 44sp serif `Cómo quieres que funcione.` + 18sp italic serif `emberMuted` subtitle `Local y a tu ritmo.`. New `DataSection` `Tus datos` via `HSectionLabel` + a single `emberSurface` container (16dp radius) with two `SettingsRow`s separated by an `HSeparator`: Exportar datos (Download icon + muted sub `Crea un archivo .hello para guardar donde quieras.`) and Restaurar desde backup (Upload icon + emberBad sub `Reemplaza todo lo que hay ahora.`). Each row shows an inline 18dp accent `CircularProgressIndicator` while busy, otherwise a muted chevron. New `SettingsRow` primitive (28dp icon column + title + optional sub with Muted/Danger tone + trailing slot) ready to host `HToggle`/badge in future sections. Footer: italic serif `hecho con tinta y café` + Geist Mono `hello · 2026` in faint tone, centered. `HAlertDialog` reused for the restore confirmation with the trash icon. New strings: `settings_eyebrow`, `settings_headline`, `settings_subtitle`, `settings_section_data`, `settings_export_subtitle`, `settings_import_subtitle`, `settings_footer_tagline`, `settings_footer_meta`.

**All phases complete.** The Ember dark redesign roadmap is closed. The time picker (Settings → reminder time) remains deferred as a follow-up tracked in `docs/FEATURE_ROADMAP.md` (Notifications Sprint 2 → F-Time-Picker).

**Phase 2.7 shipped as a single commit:**
- ✅ `3ab5de1` (2026-05-22) — Edit Flashcard. Inline top bar (close × + `EDITAR TARJETA` Geist Mono + accent `Guardar` text action, muted when invalid/loading/submitting). Stack of `HInput`s: word · translation · `Row` of phonetic + part-of-speech weight 1f side-by-side · meaning multiline (4 rows). `HSectionLabel "Ejemplos · N"` with trailing accent inline `+ agregar` action (replaces the old outlined "Añadir ejemplo" button). New `ExampleEditRow` card: `emberSurface` (14dp radius, 1dp `emberDivider`) holding a 17sp Instrument Serif italic field for the English text, hairline divider, Geist 14sp field for the Spanish translation, plus a close × IconButton anchored top-end. New `DangerRow` at the bottom: `emberBadSoft` container, 1dp 35% `emberBad` border, trash icon + Geist Medium 15sp `Borrar tarjeta` in `emberBad`. Delete wiring: new `DeleteFlashcard` / `ConfirmDeleteFlashcard` / `DismissDeleteFlashcard` intents, `isDeleteConfirmationVisible` in `UiState`, `FlashcardDeleted` effect, `softDeleteFlashcardUseCase` injected through `NewModule` (reuses the existing use case already used by Card Detail). Route handles `FlashcardDeleted` by `navigator.goBack()`. Reused `HAlertDialog` with the existing `delete_flashcard_*` strings.

**Phase 2.6 shipped as a single commit:**
- ✅ `67e7bbf` (2026-05-22) — New/Edit Deck. Inline top bar (close × + uppercase Geist Mono title `MAZO NUEVO` / `EDITAR MAZO` + Geist Medium 15sp accent text action `Crear` / `Guardar`, ghost when invalid). 44sp serif headline `Un mazo nuevo.` / `Editar mazo.`. `DeckNameField`: `emberSurface` card with 1.5dp `emberAccent` border, 14dp radius, 22sp Instrument Serif italic value and same-style placeholder, accent caret; auto-focused on first composition in Create mode via `FocusRequester` + `LaunchedEffect` (Edit mode skips auto-focus to avoid stealing focus from a pre-filled value). Description keeps `HInput` (3-line min). New `EmberTagsField`: mono `ETIQUETAS` label, a single `emberSurface` container (1dp `emberDivider`, 14dp radius) holding a `FlowRow` of accent removable `HChip`s and an inline mini text field whose placeholder is `+ agregar…`; Enter or comma commits (lowercased, trimmed, dedup). Supporting text moved outside the container.

**Phase 2.5 shipped as 2 sub-commits:**
1. ✅ `113a96d` (2026-05-22) — Chrome + Ember header. HTopBar (back + MoreVert dropdown) wraps the screen in `emberBg` with `statusBarsPadding`. New `EmberDeckHeader`: mono 11sp `MAZO` eyebrow, 44sp serif deck name, Geist 14sp muted description, FlowRow of `HChip` tags. Hairline-bordered stats row in Geist Mono: `N tarjetas · N para hoy (accent) · X.X d. promedio` (promedio segment hidden until at least one card has been studied). Accent Lg `Estudiar N ahora` CTA full-width, only when `dueCount > 0`. New strings: `deck_detail_meta_label`, plurals `deck_detail_stats_cards`, `deck_detail_stats_due_today`, `deck_detail_stats_avg_interval`, `deck_detail_study_now_cta`.
2. ✅ `7a0ec3d` (2026-05-22) — Card list. `HSectionLabel "Tarjetas · N"` (count becomes `filtered / total` while searching) with a trailing `IconButton` that toggles between a muted search glyph and an accent close glyph. Tapping expands an `HSearchBar` below the label; closing also clears the query. The list lives in a single `emberSurface` column (16dp radius) with `HSeparator` between rows. New `CardListItem`: 15.5sp Geist Medium word + 14sp italic serif translation (falls back to phonetic) + 11sp Geist Mono `d MMM` (es-ES) next review date. Empty state restyled (italic serif headline + Geist body + Accent `Añadir tarjeta`); no-search-results restyled with serif `"Nada con \"X\"."` + Geist body. M3 FAB replaced by accent circular `HFab`. New strings: `deck_detail_search_toggle_open` / `_close`, `deck_detail_search_no_results`.

**Deferrals from Phase 2.5** (need data plumbing or polish):
- The "italic muted suffix" beside the deck name mentioned in the 2.5 spec is currently not rendered (Deck model has no structured sub-name field — only `name` and `description`).
- The hairline stats row only computes `X.X d. promedio` from cards with `repetitions > 0 && interval > 0`; it stays hidden for fresh decks.
- Search auto-focus on tap: the toggle expands the search bar but the text field still requires a second tap to receive focus (no auto-focus / `FocusRequester` plumbed yet).

**Phase 2.2 shipped as 4 sub-commits:**
1. ✅ `4ba50d2` (2026-05-22) — StudyTop chrome: close icon + thin HProgressBar + N/total mono counter + uppercase mono state label (RECORDAR / ESCRIBIR / RESPUESTA), wrapped in emberBg Surface with statusBarsPadding. Counter hidden on Start/Empty.
2. ✅ `83c2376` (2026-05-22) — Start (mono REPASO DE HOY eyebrow + 44sp serif `"N tarjetas para hoy."` via plurals + mono ≈Nmin + Accent Lg Comenzar + mono supportive line) and Empty (HEmptyState `"Hoy no toca repasar."`). StartMeta data class added to keep StudyCanvas under LongParameterList.
3. ✅ `e6efe73` (2026-05-22) — Recall (mono INGLÉS → ESPAÑOL/ESPAÑOL → INGLÉS/COMPLETA LA FRASE/FORMA DEL VERBO direction overlay + 48sp serif prompt + mono phonetic + mono "Mostrar pista" pill + Accent CTA "Responder escribiendo" or "Ver respuesta"). Check (new private StudyAnswerInput: emberSurface row with 1.5dp emberAccent border, 26sp italic serif placeholder, accent SolidColor cursor, mono ES/EN trailing indicator + Accent Comprobar + underlined "Revelar igual" ghost). FlashcardBackContent now takes the prompt so it stays visible during Check.
4. ✅ `f6b9c2d` (2026-05-22) — Grade reveal (mono context label + 44sp serif answer + mono phonetic + italic serif supporting + mono emberGood confirm for correct typed answers) and Mismatch (two stacked emberSurface cards: ESCRIBISTE 22sp serif strikethrough emberBad + SE ESPERABA 22sp serif emberAccent, plus italic explanation). GradeChip rebuilt on Ember tokens: emberSurface chip with 1dp emberDivider border, Geist Medium 15sp label tinted per grade (emberBad/Warn/OnBg/Accent — Good intentionally neutral for bias fix), mono interval below, dashed locked-border (drawBehind + dashPathEffect) for disabled chips with emberFaint label. Dock gets mono "Desliza para calificar rápido" hint and contextual italic lock footnote.

**Deferrals from Phase 2.2** (need new data plumbing or polish):
- Direction label uses cardType as stand-in for true part-of-speech (StudyFlashcard/StudySessionItem don't carry partOfSpeech/levelBand today).
- Start state stats row only shows estimated minutes (no `N nuevas · N repaso` split — VM doesn't expose it).
- Empty state's "Próxima tarjeta" hint card needs a next-due-time field that doesn't exist yet.
- FlippableCard still uses Material gradient tokens; restyling the gradient itself to pure Ember is its own follow-up.
- Dashed locked-chip border is subtle; could use a softer ring or higher dash contrast.
- Linguistic explanation in Mismatch is the existing `study_typed_answer_no_*_match` string, not a diff-aware explanation.

**Phase 2.3 shipped as 4 sub-commits:**
1. ✅ `a88ffd7` Mode screen + new `HWizTop` in `core/ui` (back · 3-segment progress · `N/total` mono · uppercase subtitle).
2. ✅ `56ace4c` Input screen (per-mode serif headline, BigSerifTextField 28sp italic, round mic, deck picker row, difficulty `HChip` row, sticky Continuar with `imePadding`).
3. ✅ `5d83b4c` Review screen — `WordHeader` (mono `pos · band`, 54sp serif word, mono IPA, italic serif translation), per-section `ReviewBlock` (label + edit/regen icon-buttons + read↔edit toggle italic-serif ↔ `HInput`) for Significado / Ejemplo / Patrón de uso / Error común (warn tone) / Cloze, `TARJETAS DE ESTUDIO · N` mono label with `N activas` trailing, `StudyCardRow` (HToggle + serif prompt + italic serif answer + per-row regen icon, 0.5 alpha when inactive) in a single grouped `emberSurface` with hairline dividers, sticky `Guardar tarjeta` accent CTA via `NewCardBottomBar`. Data flow untouched.
4. ✅ `a9351da` Loading + quota Error states — `LoadingPreviewSkeleton(word)` shows 3 accent pulse dots (alpha-staggered 0.25↔1.0, 900ms reverse), italic-serif `"Pensando en cuándo se suele usar {word}…"` (generic fallback when word blank), mono `SUELE TARDAR 8–12 S`, then existing shimmer skeleton. New `QuotaExceededState` composable: `!` glyph in 52dp `emberBadSoft` circle, 32sp serif headline `"Llegamos al límite diario de IA."`, geist muted body, `TU PALABRA` surface card (16dp radius, 18×16 padding, 28sp serif word), Accent `Crear a mano` + Ghost `Avisarme mañana`, mono footer `SE REINICIA EN N min` / `A LAS HH:MM` / `MAÑANA` (threshold 2h). Quota path discriminated via new `NewCardErrorUi.quotaResetAt: Instant?` propagated from `ClassifiedError`. Both error buttons wired to `onNavigateBack` until domain support exists. Verified visually on `medium_phone` (loading caught mid-API call; quota forced by writing `gen_quota_count=50` to `com.emm.data.preferences.xml`).

**Verified deferrals from Phase 2.3** (need domain intent before they can ship):
- Refresh icon on the `WordHeader` (regenerate-whole-note) — no global note-regen intent exists.
- Section-level regen on `TARJETAS DE ESTUDIO · N` (regenerate-all-cards) — same root cause; per-card regen via `RegenerateCardClicked` covers the practical case.
- `Crear a mano` action on the quota error → currently navigates back to Input. A true "manual mode" would need a non-AI creation path (not in any of the 3 `TypeView`s today).
- `Avisarme mañana` → currently navigates back. Needs notification scheduling infra.

When resuming, the data wiring (UiState/Intent/ViewModel/repository) is untouched and must stay that way — Phase 2 is visual-only. `HWizTop` and `HDictSense` are already reusable from `core/ui`. The `NewCardBottomBar` is already on Ember surface from sub-1.

**Verified deferrals from Phase 2.1** (need domain change before they can ship):
- Per-deck due count on `DeckRow` (`Deck` model has no `dueCount` field).
- `HSectionLabel` trailing action "N con repaso ↗" (same root cause).

**Confirmed visual notes from manual review on `medium_phone` and `small_phone`:**
- Instrument Serif Regular + Italic are bundled as local TTFs in `res/font/` (sourced from `google/fonts/ofl/instrumentserif`, OFL license preserved in `res/raw/instrument_serif_ofl.txt`). Geist + Geist Mono still load via the Google Fonts provider.
- The earlier visual delta against the designer's mock (flatter italic strokes, less contrast, looser horizontal metrics) was **not** render-engine inherent: it was caused by the GMS Google Fonts provider serving a stripped, mobile-optimised cut of Instrument Serif Italic (subsetted glyph set, dropped OpenType `liga`/`calt`/stylistic-alternate tables, rebuilt hinting). Pinning the upstream TTF restores the full chancery italic forms and tighter metrics — the empty-state hero `"Empieza por una palabra."` now also fits on one line for free. `FontSynthesis.None` on the accent span is kept as a defence-in-depth: any future font-load failure surfaces as upright text instead of fake italic.
- Rule of thumb for future fonts: **display / editorial faces → bundle the TTF locally. UI sans / mono → Google Fonts provider is fine.**

## Overview

Full visual rework of the app to the **"Ember dark"** identity proposed by the designer. Replaces the current generic Material 3 look with an editorial, warm-dark, typography-driven system. Scope is **dark-only** for now (light mode deferred). Architecture, navigation, and features stay the same — this is a visual + microcopy pass, not a feature rebuild.

The designer's bundle ships HTML/JSX prototypes with tokens that map 1:1 to Compose `Color()` values. Components map onto the existing `H*` family.

## Constraints

- No new features. No screens added.
- Navigation graph stays as documented in `ARCHITECTURE.md`.
- `strings.xml` stays in Spanish but moves to **neutral Spanish** (no rioplatense). Designer mocks use voseo (`decime`, `te toco`, `pisa todo`) — those are rewritten to neutral forms (`dinos`, `te avisamos`, `reemplaza todo`) during Phase 4.
- Dark-only. Light mode deferred entirely.
- All UI keeps going through `H*` components — no raw Material 3.
- Tests stay green (string asserts may need adjustment when copy changes).
- Empty-state `·` dot glyph kept as-is.
- Grade buttons rendered visually equal — no pre-highlight on `Bien`.

## Out of scope (raised by designer review, deferred)

- Light mode.
- Six missing screens/states (notification push body, time picker, delete dialogs, import-confirm dialog, category bottom sheet, mic-listening state, session-finished dialog). Each will fall back to a Phase 2 implementation that visually matches Ember dark but does not need a designer mock to ship.
- Replacing the empty-state "·" glyph with a richer mark (waiting on designer alt).

---

## Phase 0 — Foundation (tokens, fonts, theme)

**Goal:** every later phase consumes these. No screen changes yet.

### 0.1 Color tokens
Add an `Ember` color palette and wire it into `MaterialTheme` so legacy code keeps working while H* migrates.

| Token | Hex / RGBA | Compose name |
|---|---|---|
| bg | `#0F0E0C` | `EmberBg` |
| surface | `#1A1815` | `EmberSurface` |
| surface2 | `#26231E` | `EmberSurface2` |
| elev | `#3A352D` | `EmberElev` |
| divider | rgba(244,239,230,0.08) | `EmberDivider` |
| onBg | `#E8E1D0` | `EmberOnBg` |
| primary | `#F4EFE6` | `EmberPrimary` |
| muted | `#9C9079` | `EmberMuted` |
| hint | `#CEBD9A` | `EmberHint` |
| faint | rgba(244,239,230,**0.55**) | `EmberFaint` |
| accent | `#CC7A4A` | `EmberAccent` |
| accentSoft | rgba(204,122,74,0.14) | `EmberAccentSoft` |
| good | `#7BAE8B` | `EmberGood` |
| warn | `#D49765` | `EmberWarn` |
| bad | `#C16B5E` | `EmberBad` |
| goodSoft / warnSoft / badSoft | rgba(*,0.12) | `EmberGoodSoft` etc. |

> **Deviation from designer spec:** `faint` bumped from 0.4 → 0.55 for WCAG AA. If a place really needs 0.4 for purely decorative reasons, use it inline.

Files:
- `app/src/main/kotlin/com/emm/hello/core/theme/Color.kt` — add palette.
- `app/src/main/kotlin/com/emm/hello/core/theme/Theme.kt` — point dark scheme at Ember tokens. Light scheme stays as-is (deferred), but dark is forced.

### 0.2 Typography
Three families: **Instrument Serif** (display / editorial, italic-heavy) is bundled as local TTFs because the GMS Google Fonts provider serves a stripped cut that loses its chancery italic forms. **Geist** + **Geist Mono** load via the Google Fonts provider (the GMS cut is fine for UI sans / mono).

Files:
- `app/src/main/res/values/font_certs.xml` — Google Fonts cert (still required for Geist + Geist Mono).
- `app/src/main/res/font/geist.xml`, `geist_mono.xml` — downloadable font providers.
- `app/src/main/res/font/instrument_serif_regular.ttf`, `instrument_serif_italic.ttf` — bundled TTFs (sourced from `google/fonts/ofl/instrumentserif`).
- `app/src/main/res/raw/instrument_serif_ofl.txt` — SIL OFL license, packaged for attribution.
- `app/src/main/kotlin/com/emm/hello/core/theme/Type.kt` — rebuild `Typography` with the new families and rebuild the existing tipography table mapping (now in English thanks to the prior pass).

Type scale mapping:
| Compose role | Family | Size | Usage |
|---|---|---|---|
| displayLarge | Instrument Serif italic | 56 | Study card front, Start screen hero |
| displayMedium | Instrument Serif | 44–48 | Deck Detail header, Settings hero |
| displaySmall | Instrument Serif | 36–42 | Dashboard hero, empty states |
| headlineMedium | Instrument Serif | 30 | Step titles |
| headlineSmall | Instrument Serif | 22–26 | Deck row title, big stats number |
| titleLarge | Geist 500 | 16 | TopAppBar |
| titleMedium | Geist 500 | 14.5 | Settings row title, deck row metadata |
| bodyLarge | Geist | 15.5 | Form inputs |
| bodyMedium | Geist | 14 | Body copy |
| bodySmall | Geist | 13 | Microcopy |
| labelLarge | Geist 500 | 14.5 | Buttons |
| labelMedium | Geist Mono | 11 | Section labels (uppercase, +0.12em) |
| labelSmall | Geist Mono | 10 | Faint metadata (intervals, "hoy") |

### 0.3 Shapes & spacing
- Radii: 12 (inputs/blocks), 14 (review blocks), 16 (cards), 22 (search/chip pill), 30 (FAB), full (chip).
- Spacing scale: 4 / 6 / 8 / 10 / 12 / 14 / 18 / 22 / 26 / 32. Keep `dp` literals — no separate spacing token (designer uses literals too).

### 0.4 Acceptance for Phase 0
- App compiles, runs in dark, looks **broken but warm** (old layouts on new tokens).
- Detekt + tests pass.
- No screen-level changes yet.

---

## Phase 1 — `core/ui` H* component refresh

Refresh existing components and add new ones. **No feature screen edits yet** — all changes verified in `@Preview` composables.

### 1.1 Refresh existing
- `HButton` — pill shape (`h/2` radius), variants `primary` / `accent` / `secondary` / `ghost`, sizes `sm/md/lg`, danger flag.
- `HInput` — surface bg, accent border on focus, uppercase mono label above, multiline support, optional trailing icon.
- `HCard` — surface bg, 16 radius, hairline border, accent left-border variant for "due" state.
- `HSearchBar` — 44–48 height, 22 radius, search icon left, accent border on focus.
- `HBadge` (formerly some chip variant) — 22 height, 11 radius, tones `accent/good/warn/muted`.
- `HSeparator` — uses `EmberDivider`.
- `HTagChip` → renamed/refactored as `HChip` with `active` / `accent` / `removable` props. **Min hit area 48dp via inner padding** even though visual height is 30 (a11y fix).
- `HAlert`, `HDialog`, `HSelect`, `HSearchBar`, `HTagInput` — restyle to new tokens. Behavior unchanged.

### 1.2 New components to add
| New component | Purpose |
|---|---|
| `HTopBar` | Back arrow + optional title + actions row (used everywhere). |
| `HSectionLabel` | Uppercase mono label with optional trailing action. |
| `HStat` | Big serif number + small label inline. |
| `HFab` | Accent FAB with optional label. |
| `HProgressBar` | 2dp accent bar. |
| `HToggle` | iOS-style switch in accent (used in Settings). |
| `HEmptyState` | Reusable glyph + serif headline + body + CTA stack — abstracted out of the 4 empty-state designs. |

### 1.3 Acceptance for Phase 1
- All `H*` components have `@Preview` in Ember dark.
- Old feature screens still work (using updated components automatically).
- New components have isolated previews.
- Detekt + tests pass.

---

## Phase 2 — Feature screen redesigns

Each feature gets its own focused PR. Order = visibility/impact.

### 2.1 Dashboard
**Files:** `newfeatures/dashboard/DashboardScreen.kt` and helpers.

Changes:
- New top row: `Hello.` wordmark (italic serif, accent period) + settings icon button.
- Editorial hero: big serif headline `"N para repasar, el resto puede esperar."` with accent emphasis. CTAs below: `Estudiar ahora` (accent) + `N mazos` (secondary).
- `HSearchBar` directly below (no padding-heavy section).
- Tag filter row — **horizontal scroll** (designer mock truncated; fix here).
- `HSectionLabel "Tus mazos"` with trailing accent action `"N con repaso ↗"`.
- New `DeckRow` component: serif title, italic serif description, mono metadata footer (tarjetas count + tags), accent left border + big serif number when due.
- `HFab label="Nueva tarjeta"`.
- Empty state via `HEmptyState`.
- No-results state with accent-bordered search + serif headline `"Nada con \"X\"."` + CTA to create from query.

### 2.2 Study (5 states)
**Files:** `newfeatures/study/StudyScreen.kt` and helpers.

Changes:
- New `StudyTop` minimal bar: close icon + thin progress bar + `current/total` mono + uppercase state label centered.
- **Start state:** mazo label, big serif headline `"N tarjetas para hoy."`, stats row (`≈Nmin · N nuevas · N repaso`), accent `Comenzar` CTA, supportive line below.
- **Empty state:** `HEmptyState` + "Próxima tarjeta" hint card.
- **Recall state:** mono part-of-speech label, **64sp serif word centered**, mono phonetic, optional "Mostrar pista" pill, accent `Ver respuesta` + ghost `Responder escribiendo`. **Remove the swipe hint here** (bug fix — only show in Grade).
- **Check state:** mono "español → inglés" label, big serif prompt, accent-bordered serif input with cursor, mono `EN` indicator, `Comprobar` accent CTA, dashed-underline `Ver respuesta sin responder`.
- **Grade state:** mono context label, 60sp serif answer, mono phonetic, italic serif example with accent word highlight, 4-button grid `Otra vez / Difícil / Bien / Fácil` with intervals in mono below each, swipe hint mono below. **Remove pre-highlighting of `Bien`** (bias fix).
- **Grade-Mismatch state:** "Escribiste X" (strikethrough red) + "Se esperaba Y" (accent) cards + linguistic explanation. `Bien`/`Fácil` rendered as `GradeLocked` dashed-border disabled. Footnote explains lock.

### 2.3 New Card wizard (3 steps + 2 states)
**Files:** `newfeatures/card/NewCardModeScreen.kt`, `NewCardInputStepScreen.kt`, `NewCardReviewScreen.kt`, plus new `WizTop` shared component.

Changes:
- Shared `WizTop`: back arrow + 3-segment progress + `step/total` mono + optional uppercase subtitle.
- **Step 1 Mode:** serif headline `"¿Qué querés aprender?"` + 3 `ModeCard`s (`A / B / C` letters in italic serif, title, description, italic example, selected = accent border + check icon). Sticky `Continuar` CTA. Copy aligned with designer.
- **Step 2 Input:** keyboard visible, big serif headline, accent-bordered serif input (28sp, italic) with mic round button. Optional context `HInput`. `HSectionLabel "Guardar en"` + deck row with deck icon + name + mono metadata + chevron. `HSectionLabel "Dificultad"` + chip row `A2 / B1 / B2 / C1 / auto`. **Sticky Continuar above keyboard** (designer omitted; fix here).
- **Step 3 Review:**
  - Word header: mono `adjective · B2` row + refresh icon, 54sp serif word, mono phonetic, italic serif translation.
  - `ReviewBlock` per section (`Significado`, `Ejemplo`, `Patrón de uso`, `Error común` with warn tone), each with edit + regen icons. **Single regen icon scope: this block only.**
  - `HSectionLabel "Tarjetas de estudio · N"` with trailing **section-level regen** (clearly scoped: regenerates all cards).
  - `StudyCardRow` per card: toggle (on/off → 0.5 opacity), prompt, italic serif answer, **per-row regen** (scoped to that card).
  - Sticky `Guardar tarjeta` accent CTA at bottom with gradient mask.
- **Step 3 Loading:** 3 pulse dots in accent, italic serif `"Pensando en cuándo se suele usar X…"`, mono ETA, skeleton lines.
- **Step 3 Error (quota):** `!` glyph in bad-soft circle, serif headline `"Llegamos al límite diario de IA."`, body, "Tu palabra" surface card preserving input, `Crear a mano` + `Avisarme mañana`, mono reset hint.

### 2.4 Card Detail (biggest UX win)
**File:** `newfeatures/card/FlashcardDetailScreen.kt`.

Changes:
- **Drop the tabs**, replace with a single scrolling dictionary-style layout.
- Top bar: back + edit + more icons.
- Header: mono `Tarjeta · Mazo`, 64sp serif word, baseline row of phonetic + italic serif `adjective` (accent) + mono level.
- Italic serif 22sp translation line.
- Hairline.
- `DictSense` blocks: italic serif number (1–4) + mono label + body. Tones: `default` and `warn` (for "No confundir con").
- Mono footer: study card counts + next review.

### 2.5 Deck Detail
**File:** `newfeatures/deck/DeckDetailScreen.kt`.

Changes:
- Drop redundant header card.
- Top bar with back + more.
- Mono `Mazo` label + 44sp serif name (two-line: title + italic muted suffix).
- Body sans description.
- Tag chips row.
- Stats row (`38 tarjetas · 7 para hoy (accent) · 6.2 d. promedio`) inside hairline-bordered area.
- Accent `Estudiar N ahora` CTA full width.
- `HSectionLabel "Tarjetas · N"` + inline search icon button.
- `CardListItem` rows in a single grouped surface (1dp dividers between).
- `HFab` plus icon for new card.

### 2.6 New/Edit Deck
**File:** `newfeatures/deck/NewDeckScreen.kt`.

Changes:
- Top bar: close icon + uppercase mono title + accent "Crear"/"Guardar" text button.
- Serif headline `"Un mazo nuevo."` or `"Editar mazo."`.
- Name input — focused state by default, accent border, italic serif 22sp value.
- Description `HInput`.
- Tags: surface container with removable chips + "+ agregar…" placeholder.
- Keyboard visible.

### 2.7 Edit Flashcard
**File:** `newfeatures/card/EditFlashcardScreen.kt`.

Changes:
- Top bar: close + mono title + accent `Guardar` text button.
- Stack of `HInput`s (word, translation, phonetic + type side by side, meaning multiline).
- `HSectionLabel "Ejemplos · N"` with trailing `+ agregar` accent inline action.
- `ExampleEditRow`: italic serif EN + sans ES + close icon top-right.
- Danger row at the bottom: `Borrar tarjeta` in bad color.

### 2.8 Settings
**File:** `newfeatures/settings/SettingsScreen.kt`.

Changes:
- Top bar: back only.
- Mono `Ajustes` label + 48sp serif `"Cómo querés que funcione."`.
- Italic serif muted subtitle.
- `SettingsSection` groups (`Estudio`, `Tus datos`, `Sobre Hello`).
- `SettingsRow`: 28dp icon + title + sub + trailing (`HToggle` or chevron or badge or nothing).
- Microcopy pass: `"Recordatorio nocturno · Te toco a las 21:00, en silencio."`, `"Traer un backup · pisa todo lo que hay ahora"` (danger).
- Footer: italic serif `"hecho con tinta y café"` + mono `"buenos aires · 2026"`.

---

## Phase 3 — Missing screens / states (designer didn't ship)

Implement with Ember dark tokens but without bespoke mocks. Match the system.

1. **Notification push** — small icon (already in spec) + accent unread dot, sans body in muted.
2. **Settings → time picker** — Material 3 `TimePickerDialog` wrapped to use Ember tokens (override colors via `MaterialTheme`).
3. **Delete confirmation dialogs** (deck, card) — `HDialog` with serif headline, body, `Cancelar` ghost + `Borrar` danger.
4. **Import-confirm dialog** — `HDialog` with warn-tone explainer ("pisa todo lo que hay ahora").
5. **Category bottom sheet** (NewCard mode B) — `ModalBottomSheet` in surface, mono label row + serif category rows + accent check on selection.
6. **Mic-listening state** — input grows pulsing accent ring, mono "Escuchando…" label.
7. **Session-finished dialog** — `HDialog` with serif `"Listo."` + italic serif `"Repasaste N tarjetas."` + stats row + accent `Volver` CTA.

---

## Phase 4 — Microcopy pass

Pass over `app/src/main/res/values/strings.xml` (~341 strings) aligning tone to the designer's voice (calm, premium, **neutral Spanish — not rioplatense**).

Translation rules:
- Imperative `tú` form, not `vos`: `prueba` not `probá`, `quita` not `sacá`, `escribe` not `escribí`, `puedes` not `podés`.
- Avoid colloquialisms like `pisa todo`. Use `reemplaza`, `sobrescribe`.
- Keep the warmth and brevity. Don't make it corporate — just remove the river-plate flavor.

Examples (translated from designer mocks):
- Designer: `"Probá otra palabra, sacá la etiqueta {tag}, o creá una tarjeta con esta."`
  → Neutral: `"Prueba otra palabra, quita la etiqueta {tag}, o crea una tarjeta con esta."`
- Designer: `"Podés salir cuando quieras — guarda el progreso."`
  → Neutral: `"Puedes salir cuando quieras — guarda el progreso."`
- Designer: `"Hoy no toca repasar."`
  → Neutral: `"Hoy no toca repasar."` (this one is already neutral, keep)
- Designer: `"un archivo .hello a guardar donde quieras"`
  → Neutral: `"un archivo .hello para guardar donde quieras"`
- Designer: `"pisa todo lo que hay ahora"`
  → Neutral: `"reemplaza todo lo que hay ahora"`
- Designer: `"Te toco a las 21:00, en silencio."`
  → Neutral: `"Te avisamos a las 21:00, en silencio."`
- Designer: `"Decime la palabra"`
  → Neutral: `"Dime la palabra"`

When implementing screens in Phase 2, **inline copy must already be in neutral Spanish** — don't ship rioplatense and fix later.

Update test expectations (`NewCardErrorClassifierTest`, `StudyAnswerPolicyTest`, etc.) for any changed strings.

---

## Phase 5 — Cleanup

- Remove the Material 3 light scheme if dark-only is final, or wire it as a fallback that maps to Ember tokens.
- Delete any orphaned styles, drawables, or themes from before the refresh.
- Update `docs/*_CURRENT.md` files to match the new layout where they describe components ("uses tabs" → "uses dictionary sections", etc.).
- Final `./gradlew detekt testDebugUnitTest` + manual smoke pass over the 8 screens.

---

## Execution order

| Order | Phase | Effort | Independent? |
|---|---|---|---|
| 1 | Phase 0 (tokens + fonts + theme) | S (~1.5h) | Yes |
| 2 | Phase 1 (H* refresh) | M (~3h) | Yes, after 0 |
| 3 | Phase 2.1 Dashboard | M (~2h) | Yes, after 1 |
| 4 | Phase 2.4 Card Detail | M (~2h) | Yes, after 1 |
| 5 | Phase 2.2 Study | L (~3h) | Yes, after 1 |
| 6 | Phase 2.3 New Card wizard | L (~3h) | Yes, after 1 |
| 7 | Phase 2.5/2.6/2.7/2.8 Deck/NewDeck/Edit/Settings | M (~2h) | Yes, after 1 |
| 8 | Phase 3 Missing screens | M (~2h) | Yes, after 2 |
| 9 | Phase 4 Microcopy | S (~1h) | Yes |
| 10 | Phase 5 Cleanup | S (~1h) | Yes, last |

Each phase ships as one PR. Detekt + tests gate every PR.

## Decisions locked (2026-05-21)

1. **Dark-only.** Light deferred entirely.
2. **Neutral Spanish.** No rioplatense (`tú` imperative, no colloquialisms).
3. **Keep `·` empty-state glyph** for now.
4. **Remove `Bien` pre-highlight** in Grade.
5. **Sticky Continuar** added to NewCard Step 2 above keyboard.

## Reference

- Designer bundle: `/tmp/hello-design/hello/` (HTML + JSX prototypes, tokens, components).
- This plan supersedes ad-hoc visual decisions in feature `_CURRENT.md` docs once each phase ships.
