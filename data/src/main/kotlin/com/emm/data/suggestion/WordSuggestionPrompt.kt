package com.emm.data.suggestion

object WordSuggestionPrompt {

    fun build(recentWords: List<String>): String {
        val levelContext: String = buildLevelContext(recentWords)
        return """
            You are an English tutor for a Spanish-speaking learner.
            $levelContext
            Pick ONE everyday situation that is one step above the learner's inferred level.
            Return exactly 6 English words or short expressions that are useful in that situation and that are NOT already in the learner's recent words list above.
            Give each word or expression a natural Spanish translation.
            Respond with strict JSON only, no markdown, no explanation, in exactly this shape:
            {"situation": "<one short English sentence>", "words": [{"word": "...", "translation": "..."}]}
        """.trimIndent()
    }

    private fun buildLevelContext(recentWords: List<String>): String {
        if (recentWords.isEmpty()) {
            return "The learner is a total beginner with no recorded vocabulary yet. " +
                "Pick an A2-level everyday situation."
        }
        val recentWordsList: String = recentWords.joinToString(", ")
        return "The learner has recently studied these English words or expressions: $recentWordsList. " +
            "Infer the learner's approximate level from this list."
    }
}
