package com.emm.domain.quote

data class SaveQuoteAsFlashcardCommand(
    val quoteId: String,
    val phrase: String,
    val description: String,
    val translation: String,
    val pronunciation: String,
) {
    companion object {
        fun fromQuote(quote: Quote): SaveQuoteAsFlashcardCommand {
            return SaveQuoteAsFlashcardCommand(
                quoteId = quote.id,
                phrase = quote.phrase,
                description = quote.description,
                translation = quote.translation,
                pronunciation = quote.pronunciation,
            )
        }
    }
}
