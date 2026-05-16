# Study UX Iteration

| Field | Value |
|---|---|
| Status | Closed (10/10 mergeadas en local; validación en device pendiente) |
| Role | Plan atómico para iterar diseño + animación + interacción de la carta en `Study` |
| Source of Truth | No (queda como histórico; ver `STUDY_CURRENT.md` para estado actual) |
| Read this when | Querés trazar el rationale de la iteración o auditar las decisiones |
| Última verificación contra código | 2026-05-16 |

## Resumen de cierre

Las 10 tareas se implementaron en una sola sesión. `:app:compileDebugKotlin`, `detekt`, `:domain:test`, y `testDebugUnitTest` en verde.

**Desviaciones del plan original (intencionales):**

- **S2-T6**: el plan decía mover `StudyStageHeader` como overlay top-start de la carta. Decisión final: eliminarlo por completo, porque (a) `study_answer_guidance` ya se muestra dentro del dock en Check, y (b) `study_prompt_guidance` ("Intenta recordar antes de mirar") se solapa con el `TapToRevealHint` del dock en Recall. Mantener ambos era ruido.
- **S3-T9**: el plan ponía el `HBadge(cardType)` en top-start dentro del Box exterior. Implementado así (sale del `FlashcardFrontContent` y vive en el `Box` de `StudyCanvas` junto a TTS).
- **S1-T2 v1**: implementé el gesto (tap → flip; drag horizontal → grade) con thresholds 25% / 50%. **Falta** el overlay tintado con label del grade durante el drag — la única afordancia visual hoy es la translación de la carta. Es un follow-up barato (~1h) pero queda fuera de esta iteración para no inflar la sesión. Anotado como **F1-Overlay-Swipe** abajo.

## Follow-ups conocidos

- **F1-Overlay-Swipe**: agregar overlay tintado (errorContainer/tertiaryContainer/primaryContainer/secondaryContainer) sobre la back face con label "Hard"/"Easy"/etc. mientras `dragOffset != 0`. Alpha proporcional a `|dragOffset| / widthPx`. Vive dentro del `Box` re-rotado del back content (para no quedar mirrored).
- **F2-Validación-Device**: ninguna de las 10 tareas se probó en device físico. La sensación final del flip 420ms con cameraDistance 30 + scale entrada + swipe coordinado solo se confirma con hardware real.
- **F3-Hard-Color**: el plan mapea HARD a `tertiaryContainer` (suele ser verde/teal en M3). Semánticamente HARD es "struggled" — más cercano a warning (ámbar). Considerar usar `semanticColors.warning` que ya existe en el tema.

## TL;DR

La sesión de estudio funciona, pero la interacción no fluye: el tap en la carta y el botón "Reveal" compiten, las animaciones son seriales (~1s por carta), los 4 botones de grade son densos, el TTS está enterrado en el back content, y no hay feedback informado del intervalo SRS al gradar. Esta iteración rediseña la interacción en **10 tareas atómicas** organizadas en 3 sprints. Cada tarea es independiente; ningún cambio toca `:domain` excepto T8 (helper puro de preview de intervalo).

**Estimación total**: 1.5-2 días efectivos. Cada tarea tiene archivo afectado, criterio de aceptación y estimación.

## Principios de diseño

1. **Una afordancia por intención**: si la carta es tocable para revelar, no hay botón "Reveal" paralelo.
2. **Gestos canónicos antes que botones**: swipe horizontal para grade en back face; botones quedan como fallback accesible.
3. **Una sola superficie viva**: carta + dock + hints viven dentro del mismo `Surface`; cambian con `SizeTransform` spring, no como pantallas separadas.
4. **Animación corta y paralela**: ≤ 420 ms por flip, ≤ 220 ms por transición entre cartas, fade + scale en vez de slide horizontal.
5. **Feedback informado**: cada grade muestra su intervalo SRS resultante antes y después de elegir.
6. **Jerarquía visual proporcional al recall**: la palabra es ~70% del peso visual del front; hints son progresivos (on-demand), no automáticos.

---

## Sprint 1 — Interacción core (objetivo: ½ día)

Quita ruido perceptual y resuelve la fricción más obvia: dónde toco, qué pasa cuando arrastro.

### S1-T1: Eliminar botón "Reveal" redundante en `StudyStage.Recall`

- **Archivo:** `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyScreen.kt:532-547`
- **Por qué:** la carta ya gira al tap (`StudyScreen.kt:255-258`), pero el dock muestra un `HButton "Reveal answer"`. Dos afordancias para la misma acción confunden y rompen la expectativa cuando el usuario toca la back por error.
- **Qué hacer:** en la rama `StudyStage.Recall`, reemplazar el `HButton` por un hint chip sutil con `HBadge(variant = Outline)` + ícono `Icons.Outlined.TouchApp`, texto "Toca la carta para revelar". Mantener el `HButton` "Answer" cuando `needsTypedAnswer == true` (ese caso sí necesita CTA dock-driven porque el siguiente paso es input).
- **Criterio:** en Recall sin typed-answer, no hay botón grande en el dock. Tap en carta → flip. Tap en el chip → no hace nada (es solo affordance visual).
- **Estimación:** 30 min.
- **Estado:** [x]
- **Depende de:** ninguna.

### S1-T2: Swipe horizontal en back face → grade

- **Archivos:** `app/src/main/kotlin/com/emm/hello/newfeatures/study/FlippableCard.kt` (agregar gesture handling), `StudyScreen.kt:429-456` (cablear callbacks).
- **Por qué:** todo el flujo de grade es tap en botones. Swipe es la interacción canónica de flashcards (Anki Pro, Quizlet, RemNote). Acelera sesiones de 50+ cards.
- **Qué hacer:**
    1. Reemplazar `Modifier.clickable` en `FlippableCard` por `Modifier.pointerInput` que combine `detectTapGestures` (para flip) + `detectHorizontalDragGestures` (para grade).
    2. Agregar parámetros nuevos a `FlippableCard`: `gradeEnabled: Boolean` y `onGradeSwipe: (ReviewGrade) -> Unit`. Solo activar gestures de drag cuando `cardFace == Back && gradeEnabled`.
    3. Mientras el usuario arrastra, pintar un overlay tintado encima de la carta (`errorContainer` / `tertiaryContainer` / `primaryContainer` / `secondaryContainer`) cuya opacidad crece con `dragOffset / widthPx`.
    4. Definir umbrales: |offset| < 25% width → snap back (cancelado); 25% ≤ |offset| < 50% → `HARD` (izq) o `GOOD` (der); |offset| ≥ 50% → `AGAIN` (izq) o `EASY` (der).
    5. Mostrar texto centrado en el overlay con el grade pendiente ("Hard", "Easy"...) en `headlineSmall`.
    6. En `StudyScreen.kt`, pasar `gradeEnabled = sessionStage == StudyStage.Grade && enabledGrades` y `onGradeSwipe = callbacks.onReviewAnswer`.
- **Criterio:** en back face durante Grade, swipe izquierda corta marca Hard, larga marca Again; swipe derecha corta marca Good, larga marca Easy; release sin pasar umbral hace snap-back. Los 4 botones del dock siguen disponibles como fallback. Si `enabledGrades` no incluye un grade (caso typed-answer correcto bloquea Again), el swipe en esa dirección queda deshabilitado.
- **Estimación:** 3-4 h (la lógica de gesture + overlay + accesibilidad).
- **Estado:** [x]
- **Depende de:** ninguna estrictamente, pero leer mejor después de S1-T1.

### S1-T3: Mover TTS a `IconButton` flotante en esquina de la carta

- **Archivos:** `StudyScreen.kt:818-824` (eliminar `HButton` del back), `StudyScreen.kt:399-461` (`StudyCanvas` envuelve `FlippableCard` en un `Box` con overlay top-end).
- **Por qué:** hoy el botón "Speak" vive dentro del back content (`FlashcardBackContent`), rompiendo la jerarquía del answer y siendo inalcanzable mientras el usuario está en Front (cuando más útil es: validar pronunciación antes de revelar).
- **Qué hacer:**
    1. Quitar el `HButton` ghost de TTS de `FlashcardBackContent` (`StudyScreen.kt:818-824`).
    2. En `StudyCanvas`, envolver el `AnimatedContent` con un `Box` que tenga un `IconButton` alineado a `TopEnd` con padding 12.dp. Ícono `Icons.AutoMirrored.Filled.VolumeUp` cuando idle, `Icons.Outlined.Stop` cuando `isSpeaking`. El botón **no rota con el flip** (vive afuera del `FlippableCard`).
    3. El callback `onSpeak`/`onStop` lee la palabra del `currentItem` independientemente de la face. Si `!ttsReady`, deshabilitar.
    4. ContentDescription correcto en cada estado.
- **Criterio:** TTS accesible en Front y Back, ubicado fija en esquina superior derecha, no rota con flip. Funciona idéntico que antes (mismo `TextToSpeechManager`).
- **Estimación:** 1 h.
- **Estado:** [x]
- **Depende de:** ninguna.

---

## Sprint 2 — Motion & polish (objetivo: ½ día)

Hace que la interacción se sienta fluida. Cambios pequeños individualmente, sinérgicos en conjunto.

### S2-T4: Acortar y reemplazar la transición entre cartas

- **Archivo:** `StudyScreen.kt:82-87` (constantes), `StudyScreen.kt:412-425` (`transitionSpec`).
- **Por qué:** hoy `CARD_TRANSITION_DURATION_MS = 350` con slide horizontal + `CARD_FLIP_DURATION_MS = 600` flip + `CARD_EXIT_FADE_DURATION_MS = 250` fade out. Encadenados al gradar son ~1s perceptual. El slide horizontal compite además con el swipe de S1-T2.
- **Qué hacer:**
    1. `CARD_TRANSITION_DURATION_MS = 350` → **220**.
    2. `CARD_EXIT_FADE_DURATION_MS = 250` → **160**.
    3. Reemplazar `slideInHorizontally + fadeIn` por `fadeIn + scaleIn(initialScale = 0.96f)`.
    4. Reemplazar `slideOutHorizontally + fadeOut` por `fadeOut + scaleOut(targetScale = 0.92f)`.
    5. Usar `FastOutSlowInEasing` explícito en los `tween`.
- **Criterio:** al gradar (o al swipear si S1-T2 ya está merged), la carta saliente se desvanece encogiendo levemente y la nueva entra creciendo desde 0.96 con fade. No hay slide horizontal (eso queda libre para que el swipe gestural no compita).
- **Estimación:** 45 min.
- **Estado:** [x]
- **Depende de:** ninguna.

### S2-T5: Aumentar `cameraDistance` y suavizar el flip

- **Archivo:** `FlippableCard.kt:26-27, 67`.
- **Por qué:** `CARD_CAMERA_DISTANCE_MULTIPLIER = 12f` da un flip "pegado al lente", caricaturesco. Guideline Compose es ~30. El flip de 600ms también es excesivo combinado con S2-T4.
- **Qué hacer:**
    1. `CARD_CAMERA_DISTANCE_MULTIPLIER = 12f` → **30f**.
    2. `CARD_FLIP_DURATION_MS = 600` → **420**.
    3. Cambiar `tween` por `tween(durationMillis = 420, easing = FastOutSlowInEasing)`.
- **Criterio:** el flip se siente 3D orgánico (no plano-papel), y se completa en ~420ms. Validar en device físico (no solo emulador) porque la percepción de profundidad depende del DPI.
- **Estimación:** 30 min.
- **Estado:** [x]
- **Depende de:** ninguna.

### S2-T6: Unificar Card + Dock + Hint en una sola superficie

- **Archivos:** `StudyScreen.kt:216-315` (la `Column` raíz dentro de `Scaffold`), `StudyScreen.kt:361-382` (eliminar `StudyStageHeader`), `StudyScreen.kt:399-461` (`StudyCanvas`), `StudyScreen.kt:463-611` (`StudyActionDock`).
- **Por qué:** hoy hay 3 bloques visuales separados — `HProgressBar`, `StudyStageHeader` (Text flotante), `StudyCanvas` (Surface), `StudyActionDock` (Surface). Cada uno con sus propios paddings, shapes y backgrounds. Visualmente compiten en vez de sentirse como un solo objeto.
- **Qué hacer:**
    1. Envolver `StudyCanvas` + `StudyActionDock` en un único `Surface` (`shape = extraLarge`, `color = surfaceContainerLowest`) con `Column` interna.
    2. Eliminar el `Surface` interno de `StudyActionDock` (que vive en `StudyScreen.kt:473-476`); ahora es solo una `Column` con padding.
    3. Eliminar `StudyStageHeader` como composable separado. El hint guidance se renderiza dentro de la carta como `Text` overlay top-start (con `bodySmall` + `onSurfaceVariant`), solo en `StudyStage.Recall` y `StudyStage.Check`.
    4. El `AnimatedContent` del dock usa `SizeTransform(clip = false) { initial, target -> spring(stiffness = StiffnessMediumLow) }` para que el cambio de altura entre stages (Recall → Check → Grade) sea spring fluido.
- **Criterio:** se ve un solo cuerpo visual con la carta arriba y los controles abajo, sin "costuras" entre superficies. El cambio entre stages anima la altura con spring, no salta.
- **Estimación:** 2 h.
- **Estado:** [x]
- **Depende de:** facilita S1-T3 (TTS floating ya está dentro del wrapper único); idealmente después de S1-T3.

### S2-T7: Eliminar dual-source `prevStudyItem`

- **Archivos:** `StudyScreen.kt:130, 259, 389, 446` (eliminar `prevStudyItem` y referencias).
- **Por qué:** hoy `frontContent` usa `currentItem` y `backContent` usa `prevStudyItem.value` para evitar parpadeo al avanzar mientras la carta hace flip. Tener dos fuentes de datos en la misma vista es frágil — y deja de tener sentido cuando S2-T4 reemplaza el slide horizontal por fade+scale (la carta saliente se desvanece como unidad, no hay momento en que se necesite congelar el back).
- **Qué hacer:**
    1. Eliminar `prevStudyItem` (`StudyScreen.kt:130`) y `onCardAnimationFinished` callback (`StudyScreen.kt:259`).
    2. En `FlashcardBackContent`, leer `card = currentItem?.flashcard` y `studyCard = currentItem?.studyCard` directamente.
    3. Quitar el parámetro `onFinished` de `FlippableCard` (`FlippableCard.kt:40, 48`).
- **Criterio:** al gradar, la carta saliente hace fade+scale out con sus propios datos consistentes; la nueva carta entra con los suyos. Sin flash de contenido vacío en la back face. Verificar visualmente con 5-10 grades seguidos.
- **Estimación:** 45 min.
- **Estado:** [x]
- **Depende de:** S2-T4 (la nueva transición es la que hace que esto sea seguro).

---

## Sprint 3 — Informed feedback (objetivo: ½ día)

Convierte cada interacción en una decisión informada. Es donde un usuario con 50 cards/día pasa de "tocar Good por inercia" a "evaluar honestamente".

### S3-T8: Preview de intervalo SRS por grade (helper de dominio)

- **Archivos:** `domain/src/main/kotlin/com/emm/domain/study/PreviewNextInterval.kt` (nuevo), `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyUiState.kt`, `StudyViewModel.kt:43-55` (`showNextCard`).
- **Por qué:** hoy el usuario elige Again/Hard/Good/Easy sin saber qué intervalo SRS produce cada uno. Mostrar el preview convierte el grade en una decisión informada y respeta la lógica del scheduler ya implementada (`SpacedRepetitionScheduler.kt`).
- **Qué hacer:**
    1. Crear `domain/.../study/PreviewNextInterval.kt`:
        ```kotlin
        object PreviewNextInterval {
            fun previewAll(review: FlashcardReview, clock: Clock): Map<ReviewGrade, Long> =
                ReviewGrade.entries.associateWith { grade ->
                    SpacedRepetitionScheduler.schedule(review, grade, review.flashcardId, clock).interval
                }
        }
        ```
       Devuelve días por grade. Pure function, JVM-only, sin side effects.
    2. Agregar `intervalPreviews: Map<ReviewGrade, Long> = emptyMap()` a `StudyUiState`.
    3. En `StudyViewModel.showNextCard()`, después de setear `currentItem`, calcular `PreviewNextInterval.previewAll(currentItem.review, clock)` y guardar en state. Inyectar `Clock` en el VM (usar `SystemClock` del domain como default en `NewModule.kt`).
    4. Tests: `PreviewNextIntervalTest.kt` en `domain/src/test/` cubriendo cards nuevas (repetitions=0) y maduras (repetitions=5, interval=14).
- **Criterio:** `StudyUiState.intervalPreviews` contiene los 4 intervalos en días al inicio de cada card. Tests verdes. Sin cambios visibles aún (S3-T10 los renderiza).
- **Estimación:** 1.5 h.
- **Estado:** [x]
- **Depende de:** ninguna.

### S3-T9: Jerarquía visual del front — palabra dominante + hint progresivo

- **Archivo:** `StudyScreen.kt:673-744` (`FlashcardFrontContent`), `StudyScreen.kt:864-911` (`CardTypePromptBlock`).
- **Por qué:** hoy el front apila 5-6 elementos competitivos (badge cardType + frontTitle "Recognition" + prompt + phonetic + frontSupport + separators). `frontTitle` repite lo que dice el badge. El `frontSupport` siempre visible da hints automáticos que rompen el principio de "desirable difficulty" del SRS.
- **Qué hacer:**
    1. Eliminar el `Text(frontTitle)` (`StudyScreen.kt:704-712`) — el `cardType` badge ya comunica esa información.
    2. Mover el `HBadge(cardType)` a la esquina top-start de la carta (alineado top-start dentro del `Box` exterior, junto al TTS de S1-T3 que está en top-end). Padding 12.dp. Quitarlo del flujo central.
    3. Subir el `prompt` a `displayMedium` (de `headlineMedium`) y dejarlo en `FontWeight.Bold`.
    4. Phonetic en `bodySmall`, sin separator antes (el peso visual ya es bajo).
    5. **Hint progresivo**: el `frontSupport` deja de mostrarse por default. Agregar un `IconButton` pequeño debajo del phonetic con `Icons.Outlined.Info`. Al tap, muestra el `frontSupport` en un `HAlert` inline (`AlertVariant.Default`). Tap de nuevo lo oculta.
    6. Para `StudyCardType.Cloze`, mantener el label "study_cloze_prompt_title" porque indica que la respuesta es un completamiento, no la palabra entera (es semánticamente necesario, no es ruido).
- **Criterio:** la palabra ocupa el centro visual y representa ≥ 60% del peso. Hints solo aparecen on-demand. Cloze sigue siendo distinguible visualmente (label "Completa la frase" sobre la palabra).
- **Estimación:** 2 h.
- **Estado:** [x]
- **Depende de:** S1-T3 (porque el cardType badge va al top-start del wrapper que ya tiene TTS en top-end).

### S3-T10: Grade buttons como grid 2x2 con intervalo + color semántico

- **Archivos:** `StudyScreen.kt:1091-1152` (`AnswerButtons`), nuevo composable privado `GradeChip` dentro del mismo archivo.
- **Por qué:** los 4 `HButton` rectangulares con leadingIcon son densos, ocupan poco click-target relativo al ancho, y no muestran el intervalo. El estándar de Anki Pro/RemNote es chips 1:1 con label + intervalo + color semántico.
- **Qué hacer:**
    1. Crear `GradeChip(grade, intervalDays, enabled, onClick)` composable privado en `StudyScreen.kt`. Renderiza un `Surface` con:
        - Aspect ratio 1:1 (o `heightIn(min = 88.dp)`).
        - Background: `errorContainer` (Again), `tertiaryContainer` (Hard), `primaryContainer` (Good), `secondaryContainer` (Easy).
        - Border 1dp del `contentColorFor(...)` con alpha 0.2.
        - Layout: ícono top-start, label `titleMedium` SemiBold center, intervalo `labelSmall` bottom-center con `formatInterval(days)` ("1 día", "6 días", "2 semanas", "1 mes" — helper local).
    2. Reemplazar las dos `Row` con `HButton` por un `Column` de 2 `Row` con `GradeChip` en cada celda, `Modifier.weight(1f)` y `aspectRatio(1f)` o altura fija.
    3. `formatInterval(days: Long): String` local: <1 → "Hoy", 1 → "Mañana", <7 → "$days días", <30 → "$weeks semana(s)", else "$months mes(es)".
    4. Pasar `intervalPreviews: Map<ReviewGrade, Long>` desde state a `AnswerButtons`.
    5. Mantener `enabledGrades` y `guidance` igual que hoy (la policy de S1-T1 sigue válida).
- **Criterio:** dock muestra grid 2x2 de chips coloreados; cada uno con ícono + label + intervalo dinámico calculado para la card actual. Tocar uno graba el grade. Si un grade está en `enabledGrades = false`, el chip se renderiza con alpha 0.4 y no recibe taps. Los intervalos coinciden con lo que `SpacedRepetitionScheduler.schedule` retornaría.
- **Estimación:** 2 h.
- **Estado:** [x]
- **Depende de:** S3-T8 (para tener los previews en state).

---

## Anti-tareas (qué NO se hace en esta iteración)

- **No** se introduce un sistema de gestos verticales (swipe up = skip, swipe down = back). Demasiado ambiguo, y el back ya tiene navigation icon.
- **No** se agrega haptic feedback nuevo más allá del que ya existe (`TextHandleMove` en flip, `LongPress` en grade). Si S1-T2 necesita haptic en el cruce de umbral, se agrega ahí puntualmente.
- **No** se crea un `H*` component nuevo para el chip de hint ni para el grade chip. Se mantienen como composables privados dentro de `StudyScreen.kt`. Si pasan a usarse en otras pantallas, se promueven a `core/ui/` en su PR propio.
- **No** se cambia la lógica de aggregated grade en `StudyViewModel.processReviewAnswer` (`StudyViewModel.kt:68-93`). El preview de S3-T8 muestra el intervalo "si esta fuera la última studyCard del flashcard"; el comportamiento real para flashcards con múltiples studyCards sigue siendo el moreConservativeGrade ya implementado. Esto es deliberado: complicar el preview con lógica de pending studyCards hace el plan inestimable. Se puede revisar después.
- **No** se toca `:data` ni el schema. Todo el cambio vive en `app/` excepto el helper puro de S3-T8 en `:domain`.

---

## Orden recomendado de implementación

1. **S1-T1** (afterReveal limpio) → confirma que el dock se simplifica sin romper Recall.
2. **S1-T3** (TTS floating) → libera espacio en back content.
3. **S2-T5** (cameraDistance + flip duration) → cambio trivial, mejora inmediata.
4. **S2-T4** (transición fade+scale) → habilita S2-T7.
5. **S2-T7** (eliminar prevStudyItem) → cierra deuda de doble-source.
6. **S2-T6** (unificar superficie) → consolida visualmente lo anterior.
7. **S3-T8** (helper preview interval + state) → infraestructura para S3-T10.
8. **S3-T9** (jerarquía front) → con TTS y badge ya posicionados.
9. **S3-T10** (grid 2x2 con intervalo) → cierre visible.
10. **S1-T2** (swipe) → al final porque es la tarea más larga y se beneficia de tener todo lo demás estable para validar la sensación.

## Criterios de cierre del plan

- Sesión de 20 cards en device físico se siente "una sola acción continua", no "20 pantallas separadas".
- Tap para revelar tiene exactamente un destino (la carta misma cuando no hay typed-answer; el dock cuando sí).
- Cada grade muestra su intervalo SRS resultante antes de elegirlo.
- Swipe a izquierda/derecha funciona como alternativa fluida a los chips de grade.
- TTS accesible desde Front y Back sin rotar con flip.
- No hay regresiones en typed-answer (Exact / FlexibleText / ManualSelfCheck siguen funcionando como hoy).
