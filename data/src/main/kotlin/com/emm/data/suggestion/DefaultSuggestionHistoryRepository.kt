package com.emm.data.suggestion

import com.emm.data.HelloDb
import com.emm.data.SuggestionQueries
import com.emm.data.localfirst.LocalFirstWrite
import com.emm.domain.suggestion.SuggestionHistory
import com.emm.domain.suggestion.SuggestionHistoryRepository
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.emm.data.SuggestionHistory as SuggestionHistoryRow

@LocalFirstWrite
class DefaultSuggestionHistoryRepository(
    db: HelloDb,
    private val ioDispatcher: CoroutineDispatcher,
) : SuggestionHistoryRepository {

    private val dao: SuggestionQueries = db.suggestionQueries

    override suspend fun fetch(): SuggestionHistory = withContext(ioDispatcher) {
        val rows: List<SuggestionHistoryRow> = dao.selectHistory().executeAsList()
        SuggestionHistory(
            situations = rows.textsOf(HISTORY_KIND_SITUATION),
            words = rows.textsOf(HISTORY_KIND_ITEM),
        )
    }

    override suspend fun deleteShownBefore(cutoff: Instant) {
        withContext(ioDispatcher) {
            dao.deleteHistoryShownBefore(cutoff.toEpochMilli())
        }
    }
}

private fun List<SuggestionHistoryRow>.textsOf(kind: String): List<String> =
    filter { row -> row.kind == kind }.map { row -> row.text }
