@file:Suppress("SpellCheckingInspection")

package com.emm.data.wordcontent

object PromptFactory {

    fun dictionaryPrompt(input: String): String {
        return "Eres un profesor experto de inglés dame que significa y cuando usar esta palabra o frase \"${input}\""
    }

    fun ankiPrompt(input: String): String {
        return """
            Soy un profesor de inglés experto en crear tarjetas Anki efectivas. Tu tarea es generar el contenido de una flashcard para la palabra "get". La tarjeta debe tener la siguiente estructura:



            ## Front



            * **Definición**: Una definición clara y concisa de la palabra "$input" en inglés.

            * **Sinónimos**: Una lista de sinónimos relevantes para la palabra "$input".

            * **Ejemplos**: Tres oraciones de ejemplo que demuestren el uso de la palabra "$input" en diferentes contextos.

            * **Ejercicio**: Un ejercicio interactivo para practicar el uso de la palabra "$input". Este ejercicio podría ser:

            * **Completar la oración**: Una oración con un espacio en blanco donde el usuario debe colocar la palabra "$input" o uno de sus sinónimos.

            * **Elegir la respuesta correcta**: Una pregunta de opción múltiple donde el usuario debe elegir la definición o el sinónimo correcto de la palabra "$input".

            * **Traducir la oración**: Una oración en español que el usuario debe traducir al inglés usando la palabra "$input".



            ## Back



            * **Solución al ejercicio**: La respuesta correcta al ejercicio presentado en el front de la tarjeta.

            * **Información adicional**: Cualquier información adicional relevante sobre la palabra "$input", como su etimología, uso en expresiones idiomáticas, o notas sobre su pronunciación.



            ## Instrucciones adicionales



            * El contenido de la tarjeta debe estar en inglés.

            * La definición debe ser clara y concisa, utilizando un lenguaje sencillo y fácil de entender.

            * Los sinónimos deben ser relevantes y de uso común.

            * Los ejemplos deben ser variados y mostrar el uso de la palabra "$input" en diferentes contextos.

            * El ejercicio debe ser interactivo y ayudar al usuario a practicar el uso de la palabra "$input".

            * La solución al ejercicio debe ser clara y concisa.

            * La información adicional debe ser relevante y útil para el usuario.



            ## Ejemplo



            Para la palabra "happy", la tarjeta Anki podría verse así:



            ## Front



            * **Definition**: Feeling or showing pleasure and contentment.

            * **Synonyms**: joyful, pleased, delighted.

            * **Examples**:

            * She was happy to see her friends.

            * The children were happy playing in the park.

            * He felt happy after receiving the good news.

            * **Exercise**: Choose the correct synonym for "happy" in the following sentence: "She was ______ to be home."

            * a) sad

            * b) joyful

            * c) angry



            ## Back



            * **Solution to the exercise**: b) joyful

            * **Additional information**: "Happy" comes from the Old Norse word "happr", meaning "luck". It is often used in the expression "happy birthday".
        """.trimIndent()
    }
}