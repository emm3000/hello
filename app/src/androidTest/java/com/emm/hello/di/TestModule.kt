package com.emm.hello.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.export.BackupExporter
import com.emm.data.export.BackupImporter
import com.emm.domain.flashcard.FlashcardGenerationInput
import com.emm.domain.flashcard.FlashcardGenerationRepository
import com.emm.domain.generation.AmbiguousGenerationInputException
import com.emm.domain.generation.GeneratedLearningNote
import com.emm.domain.localfirst.LocalIdentityInitializer
import com.emm.domain.suggestion.WordSuggestionRepository
import com.emm.domain.suggestion.WordSuggestions
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module that overrides production bindings with deterministic fakes
 * for instrumented tests.
 *
 * Load it **after** the production modules so these bindings win:
 *
 * ```
 * KoinComposeTestRule(newModule, repositoryModule, testModule)
 * ```
 */
val testModule = module {
    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = HelloDb.Schema,
            context = androidContext(),
            name = ":memory:",
        )
    }
    single<HelloDb> { HelloDb(get()) }
    single<LocalIdentityInitializer> { FakeLocalIdentityInitializer() }
    single<FlashcardGenerationRepository> { FakeFlashcardGenerationRepository() }
    single<WordSuggestionRepository> { FakeWordSuggestionRepository() }
    single<BackupExporter> { FakeBackupExporter() }
    single<BackupImporter> { FakeBackupImporter() }
}

private class FakeFlashcardGenerationRepository : FlashcardGenerationRepository {

    override suspend fun generateLearningNote(input: FlashcardGenerationInput): GeneratedLearningNote {
        throw AmbiguousGenerationInputException(reason = "Generation is disabled in instrumented tests.")
    }
}

private class FakeWordSuggestionRepository : WordSuggestionRepository {

    override suspend fun suggest(recentWords: List<String>): WordSuggestions {
        return WordSuggestions(situation = "", words = emptyList())
    }
}
