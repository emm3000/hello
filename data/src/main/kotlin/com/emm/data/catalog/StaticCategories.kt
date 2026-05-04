package com.emm.data.catalog

data class StaticCategories(
    val id: Int,
    val name: String,
)

val staticCategories: List<StaticCategories> = listOf(
    StaticCategories(1, "Vocabulario Basico"),
    StaticCategories(2, "Saludos y Presentaciones"),
    StaticCategories(3, "Alfabeto y Pronunciacion"),
    StaticCategories(4, "Numeros y Fechas"),
    StaticCategories(5, "Colores y Objetos"),
    StaticCategories(6, "Verbo To Be"),
    StaticCategories(7, "Articulos y Sustantivos"),
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
    StaticCategories(22, "Ingles Conversacional"),
    StaticCategories(23, "Ingles para Viajes"),
    StaticCategories(24, "Ingles de Negocios"),
    StaticCategories(25, "Errores Comunes"),
    StaticCategories(26, "Expresiones Idiomaticas"),
    StaticCategories(27, "Comprension Auditiva"),
    StaticCategories(28, "Comprension Lectora"),
    StaticCategories(29, "Escritura"),
    StaticCategories(30, "Habla y Pronunciacion")
)

val difficult: List<String> = listOf("basico", "intermedio", "avanzado")
