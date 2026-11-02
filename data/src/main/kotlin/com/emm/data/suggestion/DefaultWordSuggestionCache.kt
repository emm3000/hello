package com.emm.data.suggestion

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.emm.data.HelloDb
import com.emm.data.SelectBatchWithCandidates
import com.emm.data.SuggestionQueries
import com.emm.data.localfirst.LocalFirstWrite
import com.emm.domain.suggestion.SuggestedWord
import com.emm.domain.suggestion.WordSuggestionCache
import com.emm.domain.suggestion.WordSuggestions
import com.emm.domain.time.Clock
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@LocalFirstWrite
class DefaultWordSuggestionCache(
    private val db: HelloDb,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher,
) : WordSuggestionCache {

    private val dao: SuggestionQueries = db.suggestionQueries

    override fun observe(): Flow<WordSuggestions?> = dao.selectBatchWithCandidates()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { rows ->
            rows.firstOrNull()?.let { batchRow ->
                WordSuggestions(
                    situation = batchRow.situation,
                    words = rows.mapNotNull(SelectBatchWithCandidates::toSuggestedWord),
                    firstSeenAt = batchRow.firstSeenAt?.let(Instant::ofEpochMilli),
                )
            }
        }

    override suspend fun replace(suggestions: WordSuggestions) = withContext(ioDispatcher) {
        val createdAt: Long = clock.now().toEpochMilli()
        db.transaction {
            dao.clearBatch()
            dao.insertBatch(situation = suggestions.situation, createdAt = createdAt)
            suggestions.words.forEachIndexed { index, suggestedWord ->
                dao.insertCandidate(
                    position = index.toLong(),
                    word = suggestedWord.word,
                    translation = suggestedWord.translation,
                )
            }
            recordHistory(suggestions, shownAt = createdAt)
        }
    }

    override suspend fun markSeen() {
        withContext(ioDispatcher) {
            dao.markBatchSeen(firstSeenAt = clock.now().toEpochMilli())
        }
    }

    private fun recordHistory(suggestions: WordSuggestions, shownAt: Long) {
        dao.insertHistory(kind = HISTORY_KIND_SITUATION, text = suggestions.situation, shownAt = shownAt)
        suggestions.words.forEach { suggestedWord ->
            dao.insertHistory(kind = HISTORY_KIND_ITEM, text = suggestedWord.word, shownAt = shownAt)
        }
    }
}

private fun SelectBatchWithCandidates.toSuggestedWord(): SuggestedWord? {
    val candidateWord: String = word ?: return null
    val candidateTranslation: String = translation ?: return null
    return SuggestedWord(candidateWord, candidateTranslation)
}
