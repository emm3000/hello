---
paths:
  - "**/*.kt"
---

# Kotlin style rules

Readability is the goal these rules serve. When a rule and readability disagree, say so instead of silently picking one.

## Explicit types

Declare the type on every property and every local `val` / `var`.

| Case | Write |
|---|---|
| Local value | `val remaining: Int = quota.remainingToday()` |
| Property | `private val decks: StateFlow<List<Deck>> = ...` |
| Function return | `fun loadDeck(): Deck` — always, including single-expression functions |
| Abstraction matters | `val decks: List<Deck> = mutableListOf()` — declare the supertype, not `MutableList` |

Declare the **supertype** whenever the caller should depend on the abstraction rather than the concrete implementation: `List` over `MutableList`, `Flow` over `MutableStateFlow`, the interface over the implementation.

Omit the explicit type only when it would be pure noise:

- The right-hand side is a constructor call that already names the type: `val request = CreateDeckRequest(...)`. The IDE greys out the annotation because the type is already written on the line.
- Delegated properties where the delegate makes the annotation unwieldy: `val viewModel: StudyViewModel by viewModel()` is fine, but do not contort a `by remember` chain to satisfy this rule.
- Lambda parameters whose type is fixed by the receiver and obvious in context.

Primary constructor parameters always carry their type — Kotlin requires it.

## Comments

**Write none.** No KDoc, no `//`, no `/* */`, no section-header banners, no commented-out code.

The code is the explanation. If a line needs a comment to be understood, the fix is a better name or an extracted function with a name that says it — see `naming.md`.

Three exceptions survive, and only these:

1. **Why a non-obvious constraint exists.** The code can show *what* the value is, never *why* it was chosen. Existing example in `Color.kt`: `// NOTE: emberFaint uses 0.55 opacity (bumped from designer's 0.40 for WCAG AA).` Without it, someone "fixes" the value back and breaks accessibility.
2. **A warning of consequences.** A workaround, an ordering requirement, a known platform bug.
3. **An external reference.** A spec, a paper, a migration note the code cannot carry — for example why an FSRS constant has the value it has.

Never acceptable: restating what the code does, `// region`-style headers, `// maps the state`, dead code left commented, or a `TODO` committed without an owner and a reason.

When you delete code, delete it. Git has the history.

## Kotlin idioms

- Prefer `val`. Reach for `var` only when reassignment is the point.
- Prefer extension functions over utility classes and `object` holders.
- Prefer expression bodies for functions that are genuinely one expression, with the return type still declared.
- Prefer `sealed interface` over `enum` when the variants carry data.
- Use `require` / `check` in `init` to reject invalid state at construction — see `principles.md`, fail fast.

## detekt

Config lives in `config/detekt/detekt.yml`. `./gradlew detekt` must be green before every commit.

### Complexity

```yaml
CyclomaticComplexMethod:
  active: true
  threshold: 10
  ignoreSingleWhenExpression: true
  ignoreSimpleWhenEntries: true
  nestingFunctions:
    - 'also'
    - 'apply'
    - 'run'
    - 'let'
    - 'use'
    - 'with'
```

Those scope functions are what produces callback hell. Chains like `also { apply { run { ... } } }` get refactored into named intermediate functions or an early return.

### Returns

```yaml
ReturnCount:
  active: true
  max: 5
  excludeLabeled: true
  excludedFunctions:
    - 'equals'
  ignoreAnnotated:
    - 'Composable'
```

Early returns in guard clauses are encouraged. Labeled returns inside lambdas (`return@mapNotNull null`) do not count. More than five real returns means the function should be split.

### Check before committing

1. More than 3 levels of nesting? Extract a function.
2. A chain of `else if`? Use `when`, or extract functions.
3. A function doing several things? Split it — see `principles.md`, SLAP.
4. Nested `also` / `apply` / `run` / `let`? Refactor into named steps.
