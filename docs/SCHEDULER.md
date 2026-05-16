# SpacedRepetitionScheduler

| Field | Value |
|---|---|
| Status | Active |
| Role | Documentación del algoritmo de scheduling de reviews |
| Source of Truth | `domain/src/main/kotlin/com/emm/domain/study/SpacedRepetitionScheduler.kt` |
| Tests | `domain/src/test/kotlin/com/emm/domain/study/SpacedRepetitionSchedulerTest.kt` |
| Read this when | Toques el scheduler, expliques a un usuario "por qué volvió tan pronto", o decidas migrar a FSRS |
| Última verificación contra código | 2026-05-15 |

## TL;DR

Variante simplificada de **SM-2** (Anki clásico) con una elección de diseño deliberada:
las cartas calificadas como **HARD pasan** en lugar de reprobar. Solo **AGAIN** reprueba.
Eso hace que el algoritmo sea **más permisivo** que Anki estándar para reducir la
fricción del usuario nuevo, a costo de progresar más despacio.

Si un usuario reclama "esta carta volvió rápido", la respuesta casi siempre es:
- Calificó HARD varias veces → el ease bajó hasta el piso 1.3 → intervalos cortos.
- O calificó AGAIN → reset a `interval = 1`.

## Mapeo Grade → Quality (no canónico)

Internamente cada `ReviewGrade` se traduce a un valor de "quality" SM-2.
Nuestro mapeo está **shifteado hacia arriba** respecto a Anki canónico:

| Grade | Quality (Hello) | Quality canónico Anki | Efecto |
|---|---|---|---|
| `AGAIN` | 1 | 0 | Reprueba (quality < 3) |
| `HARD`  | 3 | 2 | **Pasa** en Hello, reprueba en Anki canónico |
| `GOOD`  | 4 | 3 | Pasa |
| `EASY`  | 5 | 4 | Pasa |

Threshold de reprobación: `quality < QUALITY_THRESHOLD_FOR_RESET (3)`.
Como HARD = 3, **HARD pasa por un punto**.

### Por qué este shift

Decisión de producto: en una app local-first sin presión social ni gamificación,
calificarse honestamente como HARD ya implica humildad. Penalizarlo con un reset
desincentiva la auto-evaluación honesta. El usuario termina marcando GOOD
mecánicamente para no perder el ritmo. Preferimos que HARD pase pero **baje el ease**,
acortando los próximos intervalos sin tirar el progreso a la basura.

## Algoritmo paso a paso

```
input:  review (ease, repetitions, interval, lapses), grade, flashcardId, clock
output: nuevo FlashcardReview

1. quality = mapGradeToQuality(grade)
2. si quality < 3:
     // FAIL path
     newEaseFactor   = review.easeFactor   (sin cambios)
     newRepetitions  = 0                   (reset)
     newInterval     = 1                   (mañana lo ves otra vez)
     newLapses       = review.lapses + 1
   sino:
     // PASS path
     qualityDistance = MAX_QUALITY - quality
     easeAdjustment  = 0.10 - qualityDistance * (0.08 + qualityDistance * 0.02)
     newEaseFactor   = max(1.3, review.easeFactor + easeAdjustment)
     newRepetitions  = review.repetitions + 1
     newInterval     = cuando newRepetitions sea:
                         1L → 1 día
                         2L → 6 días
                         3L+ → round(review.interval * newEaseFactor)
     newLapses       = review.lapses  (sin cambios)

3. lastReviewedAt = clock.now()
4. nextReviewAt   = clock.now() + newInterval días
```

## Constantes (qué significa cada número)

```kotlin
MINIMUM_EASE_FACTOR        = 1.3   // piso del ease — nunca baja de acá
QUALITY_THRESHOLD_FOR_RESET = 3    // quality < esto → fail path
MAX_QUALITY                = 5     // quality de EASY
EASE_DELTA_BASE            = 0.1   // ajuste cuando quality == 5
EASE_DELTA_FACTOR          = 0.08  // pendiente lineal de decay
EASE_DELTA_PENALTY         = 0.02  // término cuadrático que penaliza calidad baja
SECOND_REVIEW_INTERVAL_DAYS = 6L   // intervalo tras la 2ª repetición exitosa
```

Los 4 valores del ease (`MINIMUM_EASE_FACTOR`, `EASE_DELTA_BASE`, `EASE_DELTA_FACTOR`,
`EASE_DELTA_PENALTY`) son los **mismos que Anki**. La fórmula del ease adjustment
es literalmente la de SM-2:

> `EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))`

Lo único que difiere es **cuándo se invoca** (umbral de reset) y la tabla de
intervalos para reps tempranas (Anki usa 10 días para la 2ª; nosotros 6).

## Tabla de easeAdjustment por grade

| Grade | qualityDistance | Ajuste de ease |
|---|---|---|
| `AGAIN` (q=1) | 4 | n/a (no se aplica, va por fail path) |
| `HARD`  (q=3) | 2 | `0.10 - 2*(0.08 + 2*0.02) = -0.14` |
| `GOOD`  (q=4) | 1 | `0.10 - 1*(0.08 + 1*0.02) = 0.00` |
| `EASY`  (q=5) | 0 | `0.10` |

Implicancias:
- **GOOD no cambia el ease**. Es el grade "neutral" — el algoritmo te respeta.
- **HARD baja 0.14**. Llegás al piso 1.3 desde 2.5 en ~9 HARDs seguidos.
- **EASY sube 0.10**. Te premia pero modesto.

## Edge cases cubiertos por tests

`SpacedRepetitionSchedulerTest.kt` cubre:

- Mapeo grade → behavior (ease delta correcto por grade).
- Piso del ease: HARD en 1.3 se queda en 1.3, HARD en 1.4 floorea a 1.3.
- Progresión de intervalos: 1 → 6 → ease * interval.
- Brand-new card con AGAIN: `lapses = 1, repetitions = 0, interval = 1`.
- Brand-new card con EASY: `ease = 2.6, repetitions = 1, interval = 1`.
- Múltiples AGAINs acumulan lapses correctamente.
- Lapse mid-stream resetea repetitions pero **conserva el ease**.
- Timestamps: `lastReviewedAt = clock.now()`, `nextReviewAt` exactamente N días después.
- `flashcardId` es **reemplazado** por el argumento, no leído del review (importante
  para evitar arrastrar el id placeholder de `FlashcardReview.empty()`).

## Invariantes garantizados por `FlashcardReview`

El constructor de `FlashcardReview` (no el scheduler) hace `require()` de:

- `easeFactor >= 1.3`
- `nextReviewAt >= lastReviewedAt`
- `interval >= 0`
- `repetitions >= 0`
- `lapses >= 0`

Por eso el scheduler nunca chequea estas condiciones a la salida: si las viola,
`copy()` tira en el `init` block.

## Limitaciones conocidas

1. **No hay modelo de "leech"**: cartas con muchísimos lapses no se marcan ni se
   sacan del deck. Anki las suspende a las 8. Decidir cuándo agregarlo cuando
   tengamos data real de retención.
2. **Clock asumido monotónico**: si el reloj del device retrocede entre un review
   y el siguiente, `nextReviewAt < lastReviewedAt` rompería el constructor. No
   defendemos contra esto explícitamente — confiamos en el sistema.
3. **No hay diferenciación de "learning" vs "review" cards**: Anki tiene dos
   colas separadas. Nosotros simplificamos a una sola.
4. **Intervalos en días enteros**: `(interval * ease).roundToLong()`. Pierde
   precisión sub-día. OK para una app de estudio diario; no OK si alguna vez
   hacemos repaso por minutos/horas.

## Si alguna vez decidimos migrar a FSRS

FSRS (Free Spaced Repetition Scheduler) es el sucesor moderno de SM-2 que usa
ML para predecir retention. Migrar requeriría:

- Tabla de parámetros por usuario (FSRS los aprende del review history).
- Cambio de schema en `FlashcardReview` (FSRS usa "stability" y "difficulty",
  no `easeFactor`).
- Lógica de scheduling completamente distinta.
- Tests del scheduler reescritos.

**Cuándo evaluarlo**: cuando tengamos ≥3 meses de review data de usuarios beta
y podamos medir retention real vs predicha. Antes es prematuro.

## Documentos relacionados

- `LAUNCH_READINESS_AUDIT.md` — `S2-T4` originalmente pedía esta doc.
- `ARCHITECTURE.md` — ubicación del scheduler en el domain.
- `AGENTS.md` — reglas de modificación del domain.
