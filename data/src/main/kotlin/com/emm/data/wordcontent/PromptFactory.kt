package com.emm.data.wordcontent

object PromptFactory {

    fun dictionaryPrompt(input: String): String {
        return "Eres un profesor experto de inglés dame que significa y cuando usar esta palabra o frase \"${input}\""
    }

    fun ankiPrompt(input: String): String {
        return "Eres un experto profesor de ingles y conocer en cards anki, necesito que me generes el contenido de una flash card recibiendo como input la siguiente palabra: \"${input}\", el contenido debe ser estructurado, si es posible agrega ejercicios en el front antes de revelar la carta"
    }
}