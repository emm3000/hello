package com.emm.domain.flashcard

data class StaticCategories(
    val id: Int,
    val name: String,
)

val staticCategories = listOf(
    StaticCategories(1, "Vocabulario Básico"),
    StaticCategories(2, "Saludos y Presentaciones"),
    StaticCategories(3, "Alfabeto y Pronunciación"),
    StaticCategories(4, "Números y Fechas"),
    StaticCategories(5, "Colores y Objetos"),
    StaticCategories(6, "Verbo To Be"),
    StaticCategories(7, "Artículos y Sustantivos"),
    StaticCategories(8, "Pronombres Personales"),
    StaticCategories(9, "Adjetivos y Comparaciones"),
    StaticCategories(10, "Presente Simple"),
    StaticCategories(11, "Presente Continuo"),
    StaticCategories(12, "Pasado Simple"),
    StaticCategories(13, "Pasado Continuo"),
    StaticCategories(14, "Futuro (will, going to)"),
    StaticCategories(15, "Verbos Modales"),
    StaticCategories(16, "Preguntas y Respuestas"),
    StaticCategories(17, "Preposiciones"),
    StaticCategories(18, "Conectores y Frases Comunes"),
    StaticCategories(19, "Tiempos Perfectos"),
    StaticCategories(20, "Condicionales"),
    StaticCategories(21, "Phrasal Verbs"),
    StaticCategories(22, "Inglés Conversacional"),
    StaticCategories(23, "Inglés para Viajes"),
    StaticCategories(24, "Inglés de Negocios"),
    StaticCategories(25, "Errores Comunes"),
    StaticCategories(26, "Expresiones Idiomáticas"),
    StaticCategories(27, "Comprensión Auditiva"),
    StaticCategories(28, "Comprensión Lectora"),
    StaticCategories(29, "Escritura"),
    StaticCategories(30, "Habla y Pronunciación")
)

val difficult = listOf("básico", "intermedio", "avanzado")
