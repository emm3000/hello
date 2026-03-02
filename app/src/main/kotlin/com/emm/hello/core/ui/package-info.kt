/**
 * # Hello UI — Sistema de componentes (shadcn/ui style)
 *
 * Este paquete expone todos los componentes reutilizables de la app.
 * Están inspirados descaradamente en shadcn/ui y mapeados sobre Material 3.
 *
 * ## Componentes disponibles
 *
 * | Componente     | Archivo          | shadcn equivalente  | Uso en la app                                      |
 * |----------------|------------------|---------------------|----------------------------------------------------|
 * | HButton        | Button.kt        | <Button />          | Acciones principales, estudio, generación          |
 * | HBadge         | Badge.kt         | <Badge />           | Conteo de cards, dificultad, estado de revisión    |
 * | HCard          | Card.kt          | <Card />            | DeckItem, CardPreview, FlashcardDetail             |
 * | HInput         | Input.kt         | <Input />           | Formularios de Deck y Flashcard                    |
 * | HAlert         | Alert.kt         | <Alert />           | Errores, confirmaciones, info en NewCard           |
 * | HSeparator     | Separator.kt     | <Separator />       | Divisores de sección                               |
 * | HSkeleton      | Skeleton.kt      | <Skeleton />        | Loading states en Dashboard y listas               |
 * | HAlertDialog   | Dialog.kt        | <AlertDialog />     | Diálogo fin de sesión en Study                     |
 *
 * ## Uso rápido
 *
 * ```kotlin
 * // Botón primario
 * HButton(text = "Generar", onClick = { ... })
 *
 * // Botón con variante
 * HButton(text = "Eliminar", onClick = { ... }, variant = ButtonVariant.Destructive)
 *
 * // Con estado de carga
 * HButton(text = "Guardar", onClick = { ... }, isLoading = viewModel.isLoading)
 *
 * // Badge
 * HBadge("24 tarjetas", variant = BadgeVariant.Secondary)
 *
 * // Card con slots
 * HCard(variant = CardVariant.Outlined) {
 *     HCardHeader(title = "Serendipity", description = "/ˌserənˈdɪpɪti/")
 *     HCardContent { Text("The occurrence of events by chance in a happy way") }
 *     HCardFooter { HButton(text = "Ver detalle", ...) }
 * }
 *
 * // Input con label externo
 * HInput(
 *     value = name,
 *     onValueChange = { name = it },
 *     label = "Nombre del mazo",
 *     placeholder = "Vocabulario B2",
 *     errorMessage = if (name.isBlank()) "Requerido" else null,
 * )
 *
 * // Alert de error
 * HAlert(
 *     title = "No se pudo generar",
 *     description = error,
 *     variant = AlertVariant.Destructive,
 * )
 *
 * // Skeleton mientras carga
 * if (isLoading) DashboardSkeleton() else DeckList(decks)
 *
 * // Dialog
 * HAlertDialog(
 *     title = "Sesión finalizada",
 *     description = "Has terminado todas las tarjetas de hoy.",
 *     confirmText = "Volver",
 *     cancelText = null,
 *     onConfirm = onNavigateBack,
 *     onDismiss = onNavigateBack,
 * )
 * ```
 *
 * ## Tipografía
 *
 * La tipografía usa **Geist** (shadcn's font). Si no tienes los .ttf en
 * `res/font/`, el sistema hace fallback a la fuente del sistema.
 *
 * Archivos font esperados:
 *   - `res/font/geist_regular.ttf`
 *   - `res/font/geist_medium.ttf`
 *   - `res/font/geist_semibold.ttf`
 *   - `res/font/geist_bold.ttf`
 *
 * Descarga: https://vercel.com/font/geist
 */
package com.emm.hello.core.ui
