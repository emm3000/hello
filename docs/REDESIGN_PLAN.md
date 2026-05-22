# Redesign Plan — Ember Dark

| Field | Value |
|---|---|
| Status | Active |
| Role | Source of truth for the visual redesign work |
| Source | Designer handoff bundle (`/tmp/hello-design/hello/`) + on-screen analysis |
| Read this when | Implementing the new visual system |
| Last verified | 2026-05-21 |

## Progress

| Phase | Status | Commits |
|---|---|---|
| 0 — Tokens, fonts, theme | ✅ Done | `89a3ee6`, `d5ea716` |
| 1 — H* components | ✅ Done | `c76265a` |
| 2.1 — Dashboard | ✅ Done (empty-state polished against designer mock) | `9bbfea1`, `416bef5`, `e09f196`, `4e6fb75` |
| 2.2 — Study | ⏳ Pending | — |
| 2.3 — New Card wizard | 🟡 In progress (sub-1 Mode + sub-2 Input done; sub-3 Review + sub-4 Loading/Error pending) | `31b09bc`, `de47991` |
| 2.4 — Card Detail | ✅ Done (dict-style scroll, new HDictSense) | `f295744` |
| 2.5 — Deck Detail | ⏳ Pending | — |
| 2.6 — New/Edit Deck | ⏳ Pending | — |
| 2.7 — Edit Flashcard | ⏳ Pending | — |
| 2.8 — Settings | ⏳ Pending | — |
| 3 — Missing screens/states | ⏳ Pending | — |
| 4 — Microcopy pass | ⏳ Pending | — |
| 5 — Cleanup | ⏳ Pending | — |

**Resume hint — Phase 2.3 (next session):**
The wizard is being shipped as 4 sub-commits. Two are done:
1. ✅ `31b09bc` Mode screen + new `HWizTop` in `core/ui` (back · 3-segment progress · `N/total` mono · uppercase subtitle).
2. ✅ `de47991` Input screen (per-mode serif headline, BigSerifTextField 28sp italic, round mic, deck picker row, difficulty `HChip` row, sticky Continuar with `imePadding`).

Still pending:
3. ⏳ **Sub-3 Review screen** — restyle `NewCardReviewScreen.kt` + `NewCardPreviewShared.kt` + `NewCardPreviewComponents.kt` + `NewCardPreviewCards.kt` (~1300 lines total). Spec § 2.3 step 3: mono `adjective · B2` row + refresh icon, 54sp serif word, mono phonetic, italic serif translation, per-section `ReviewBlock` with edit + regen icons (block-scoped), `HSectionLabel "Tarjetas de estudio · N"` with section-level regen, `StudyCardRow` per card with toggle/prompt/italic answer/per-row regen, sticky `Guardar tarjeta` accent CTA with gradient mask.
4. ⏳ **Sub-4 Loading + Error (quota) states** — restyle the loading skeleton (3 accent pulse dots + italic serif `"Pensando en cuándo se suele usar X…"` + mono ETA + skeleton lines) and the quota error (`!` glyph in `emberBadSoft` circle + serif headline + "Tu palabra" preserve-input surface + `Crear a mano` + `Avisarme mañana` + mono reset hint).

When resuming, start by `Read`-ing `NewCardReviewScreen.kt` and the three Preview* files to understand the regen/edit intent dispatch (the data wiring stays intact, only chrome changes). `HWizTop` and `HDictSense` are already reusable from `core/ui`. The `NewCardBottomBar` is already on Ember surface from sub-1.

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
