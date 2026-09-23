package com.emm.domain.study

enum class DailyNewCardLimit(val cards: Int) {
    TEN(10),
    TWENTY(20),
    THIRTY(30);

    companion object {
        val DEFAULT: DailyNewCardLimit = TEN

        fun fromCards(cards: Int): DailyNewCardLimit? = entries.firstOrNull { it.cards == cards }
    }
}
