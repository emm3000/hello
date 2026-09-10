---
name: h-component
description: Scaffold a shared H* component in core/ui following Input.kt/FieldShell.kt patterns
argument-hint: <ComponentName>
allowed-tools: Read Write Bash(./gradlew:*)
disable-model-invocation: true
---

Create a shared component named **H$ARGUMENTS** in `app/src/main/kotlin/com/emm/hello/core/ui/`.

## Before creating

1. Read `app/src/main/kotlin/com/emm/hello/core/ui/Input.kt` and `FieldShell.kt` — they are the canonical templates.
2. Verify `H$ARGUMENTS` does not already exist in `core/ui/`.
3. Confirm with me the expected usage (is it app-wide or better kept inside a feature?).

## How to create it

Copy `${CLAUDE_SKILL_DIR}/templates/H__Component__.kt.template` to `app/src/main/kotlin/com/emm/hello/core/ui/H$ARGUMENTS.kt`, drop the `.template` suffix and replace every `__Component__` placeholder with **$ARGUMENTS** (PascalCase, no `H` prefix — the template already carries it).

The template is the minimal shape: a bordered, clickable container with a label. Reshape its content for the component being built, but keep what it encodes — `modifier: Modifier = Modifier` as the first optional parameter, theme tokens instead of literal colors, an animated border, the 48dp minimum height, and the `@PreviewLightDark` preview.

Placeholder copy in the preview is a literal string. Real user-facing copy belongs in `values/strings.xml` and `values-es/strings.xml`, passed in by the caller.

## Templates

- `templates/H__Component__.kt.template` — public composable, private constants, `@PreviewLightDark` preview.

## Requirements

- Name with `H` prefix (`HSearchBar`, `HChip`, `HTagInput`, etc.).
- shadcn-style pattern: animated border, transparent background, minimum height 48dp.
- Minimal public API — no escape hatches to raw Material3.
- Pure composable: no ViewModel, no internal side effects.
- Include `@PreviewLightDark` with at least one base state.

## After creating

- Run `./gradlew :app:compileDebugKotlin` to verify.
- If it makes sense, leave a note in `core/ui/` on when to use it vs. existing components.

Do not modify Material3 imports in existing features in this turn — that is a separate scope.
