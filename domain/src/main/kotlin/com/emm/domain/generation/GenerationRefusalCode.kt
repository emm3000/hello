package com.emm.domain.generation

enum class GenerationRefusalCode(val wire: String) {
    EmptyInput("empty_input"),
    Unintelligible("unintelligible"),
    Contradictory("contradictory"),
    Unmappable("unmappable"),
    CreditsExhausted("credits_exhausted"),
    ;

    companion object {

        fun fromWire(raw: String?): GenerationRefusalCode? = entries.firstOrNull { code -> code.wire == raw }
    }
}
