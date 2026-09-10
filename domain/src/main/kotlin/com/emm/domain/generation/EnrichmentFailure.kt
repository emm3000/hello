package com.emm.domain.generation

data class EnrichmentFailure(
    val code: GenerationRefusalCode?,
    val reason: String?,
) {

    companion object {

        fun of(code: GenerationRefusalCode?, reason: String?): EnrichmentFailure? {
            if (code == null && reason == null) return null
            return EnrichmentFailure(code = code, reason = reason)
        }
    }
}
