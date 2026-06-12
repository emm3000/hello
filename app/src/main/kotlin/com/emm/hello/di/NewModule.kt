package com.emm.hello.di

import android.content.Context
import android.content.SharedPreferences
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.deck.DefaultDeckRepository
import com.emm.data.deck.DefaultDeckSelectionPreferencesRepository
import com.emm.data.deck.DefaultTagRepository
import com.emm.data.flashcard.DefaultFlashcardDuplicateRepository
import com.emm.data.flashcard.DefaultFlashcardRepository
import com.emm.data.flashcard.DefaultFlashcardReviewRepository
import com.emm.data.study.DefaultStudyStatsRepository
import com.emm.data.localfirst.DefaultLocalIdentityInitializer
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.localfirst.LocalIdentityInitializer
import com.emm.data.remote.DataStore
import com.emm.data.remote.provideSharedPreferences
import com.emm.domain.authoring.CreateFlashcardUseCase
import com.emm.domain.authoring.EnsureUniqueFlashcardInDeckUseCase
import com.emm.domain.authoring.GeneratedLearningNoteMapper
import com.emm.domain.authoring.IsExactDuplicateGeneratedNoteUseCase
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDeckDetailUseCase
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.GetFilteredDecksUseCase
import com.emm.domain.deck.RestoreDeckUseCase
import com.emm.domain.deck.SoftDeleteDeckUseCase
import com.emm.domain.deck.UpdateDeckUseCase
import com.emm.domain.flashcard.FlashcardDuplicateRepository
import com.emm.domain.flashcard.FlashcardGenerationRepository
import com.emm.domain.flashcard.FlashcardGenerationContextSentencePolicy
import com.emm.domain.flashcard.FlashcardGenerationDisambiguationPolicy
import com.emm.domain.flashcard.FlashcardGenerationInputTypeRulesPolicy
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.flashcard.GenerateLearningNotePreviewUseCase
import com.emm.domain.flashcard.CountDueFlashcardsUseCase
import com.emm.domain.flashcard.RestoreFlashcardUseCase
import com.emm.domain.flashcard.SoftDeleteFlashcardUseCase
import com.emm.domain.flashcard.UpdateFlashcardUseCase
import com.emm.domain.study.ObserveFlashcardsWithReviewUseCase
import com.emm.domain.study.GetDashboardStatsUseCase
import com.emm.domain.study.StudyStatsRepository
import com.emm.domain.flashcard.RegenerateLearningNoteClozeUseCase
import com.emm.domain.flashcard.RegenerateLearningNoteExampleUseCase
import com.emm.domain.flashcard.RegenerateLearningNoteFieldUseCase
import com.emm.domain.flashcard.RegenerateStudyCardUseCase
import com.emm.domain.flashcard.ValidateFlashcardGenerationInputUseCase
import com.emm.domain.generation.GeneratedLearningNoteCardsPolicy
import com.emm.domain.generation.GeneratedLearningNoteCoreFieldsPolicy
import com.emm.domain.generation.GeneratedLearningNoteQualityChecksPolicy
import com.emm.domain.generation.GeneratedLearningNoteTypeRequirementsPolicy
import com.emm.domain.generation.ValidateGeneratedLearningNoteUseCase
import com.emm.domain.study.ScheduleFlashcardReviewUseCase
import com.emm.domain.study.StudySessionRepository
import com.emm.domain.time.Clock
import com.emm.domain.time.SystemClock
import com.emm.hello.BuildConfig
import com.emm.data.export.BackupExporter
import com.emm.data.export.BackupImporter
import com.emm.data.export.ExportBackupDataSource
import com.emm.data.export.ImportBackupDataSource
import com.emm.data.onboarding.DataStoreOnboardingStateRepository
import com.emm.domain.onboarding.OnboardingStateRepository
import com.emm.hello.newfeatures.card.EditFlashcardViewModel
import com.emm.hello.newfeatures.onboarding.OnboardingViewModel
import com.emm.hello.newfeatures.shared.UndoEventHolder
import com.emm.hello.newfeatures.card.FlashcardDetailViewModel
import com.emm.hello.newfeatures.card.NewCardGenerationDependencies
import com.emm.hello.newfeatures.card.NewCardViewModel
import com.emm.hello.newfeatures.dashboard.DashboardViewModel
import com.emm.hello.newfeatures.deck.DeckDetailViewModel
import com.emm.hello.newfeatures.deck.NewDeckViewModel
import com.emm.hello.newfeatures.settings.SettingsViewModel
import com.emm.hello.newfeatures.study.StudyViewModel
import com.emm.hello.startup.AppStartupCoordinator
import com.emm.hello.startup.AppStartupViewModel
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val newModule = module {
    single { provideSqlDriver(androidContext()) }
    single<HelloDb> { provideDb(get()) }
    single { UndoEventHolder() }

    repository()
    useCases()
    viewModels()
}

fun Module.repository() {
    single { androidContext().contentResolver }
    single {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }
    single<SharedPreferences> { provideSharedPreferences(androidContext()) }

    factory<com.emm.domain.deck.DeckRepository> {
        DefaultDeckRepository(get(), get())
    } bind DeckRepository::class
    factoryOf(::DefaultTagRepository) bind com.emm.domain.deck.TagRepository::class
    factoryOf(::DefaultDeckSelectionPreferencesRepository) bind DefaultDeckSelectionRepository::class
    factoryOf(::DefaultFlashcardRepository)
    factory<FlashcardRepository> { get<DefaultFlashcardRepository>() }
    factory<StudySessionRepository> { get<DefaultFlashcardRepository>() }
    factory<FlashcardGenerationRepository> { get<DefaultFlashcardRepository>() }
    factoryOf(::DefaultFlashcardDuplicateRepository) bind FlashcardDuplicateRepository::class
    factoryOf(::DefaultFlashcardReviewRepository) bind FlashcardReviewRepository::class
    factoryOf(::DefaultStudyStatsRepository) bind StudyStatsRepository::class
    factoryOf(::DefaultLocalIdentityInitializer) bind LocalIdentityInitializer::class
    factoryOf(::LocalDeviceIdentityProvider)
    factoryOf(::DataStore)
    factoryOf(::ExportBackupDataSource) bind BackupExporter::class
    factoryOf(::ImportBackupDataSource) bind BackupImporter::class
    factoryOf(::DataStoreOnboardingStateRepository) bind OnboardingStateRepository::class
    single { AppStartupCoordinator(get(), get()) }
}

fun Module.useCases() {
    single<Clock> { SystemClock }
    factoryOf(::FlashcardGenerationDisambiguationPolicy)
    factoryOf(::FlashcardGenerationInputTypeRulesPolicy)
    factoryOf(::FlashcardGenerationContextSentencePolicy)
    factoryOf(::GeneratedLearningNoteCoreFieldsPolicy)
    factoryOf(::GeneratedLearningNoteTypeRequirementsPolicy)
    factoryOf(::GeneratedLearningNoteCardsPolicy)
    factoryOf(::GeneratedLearningNoteQualityChecksPolicy)
    factoryOf(::GeneratedLearningNoteMapper)
    factoryOf(::GetDecksUseCase)
    factoryOf(::GetFilteredDecksUseCase)
    factoryOf(::CreateFlashcardUseCase)
    factoryOf(::EnsureUniqueFlashcardInDeckUseCase)
    factoryOf(::IsExactDuplicateGeneratedNoteUseCase)
    factoryOf(::ValidateFlashcardGenerationInputUseCase)
    factoryOf(::ValidateGeneratedLearningNoteUseCase)
    factoryOf(::GenerateLearningNotePreviewUseCase)
    factoryOf(::RegenerateLearningNoteExampleUseCase)
    factoryOf(::RegenerateLearningNoteClozeUseCase)
    factoryOf(::RegenerateLearningNoteFieldUseCase)
    factoryOf(::RegenerateStudyCardUseCase)
    factory {
        NewCardGenerationDependencies(
            createFlashcardUseCase = get(),
            generateLearningNotePreviewUseCase = get(),
            regenerateLearningNoteExampleUseCase = get(),
            regenerateLearningNoteClozeUseCase = get(),
            regenerateLearningNoteFieldUseCase = get(),
            regenerateStudyCardUseCase = get(),
            validateInputUseCase = get(),
            validateGeneratedLearningNoteUseCase = get(),
        )
    }
    factoryOf(::GetDeckDetailUseCase)
    factoryOf(::ObserveFlashcardsWithReviewUseCase)
    factoryOf(::ScheduleFlashcardReviewUseCase)
    factoryOf(::GetDashboardStatsUseCase)
    factoryOf(::UpdateDeckUseCase)
    factoryOf(::SoftDeleteDeckUseCase)
    factoryOf(::RestoreDeckUseCase)
    factoryOf(::UpdateFlashcardUseCase)
    factoryOf(::SoftDeleteFlashcardUseCase)
    factoryOf(::RestoreFlashcardUseCase)
    factoryOf(::CountDueFlashcardsUseCase)
}

fun Module.viewModels() {
    viewModel { AppStartupViewModel(get()) }
    viewModel { params ->
        NewDeckViewModel(
            deckRepository = get(),
            updateDeckUseCase = get(),
            formMode = params.get(),
        )
    }
    viewModel { DashboardViewModel(get(), get(), get(), get(), get()) }
    viewModel {
        NewCardViewModel(
            getDecksUseCase = get(),
            generationDependencies = get(),
            defaultDeckSelectionRepository = get(),
            generationQuota = get(),
        )
    }
    viewModel {
        StudyViewModel(
            deckId = it.get(),
            studySessionRepository = get(),
            scheduleFlashcardReviewUseCase = get(),
            flashcardReviewRepository = get(),
            clock = get(),
            onboardingState = get(),
        )
    }
    viewModel {
        DeckDetailViewModel(
            deckId = it.get(),
            getDeckDetailUseCase = get(),
            observeFlashcardsWithReviewUseCase = get(),
            softDeleteDeckUseCase = get(),
            restoreFlashcardUseCase = get(),
            undoEventHolder = get(),
        )
    }
    viewModel {
        FlashcardDetailViewModel(
            flashcardId = it.get(),
            flashcardRepository = get(),
            softDeleteFlashcardUseCase = get(),
            undoEventHolder = get(),
        )
    }
    viewModel {
        EditFlashcardViewModel(
            flashcardId = it.get(),
            deckId = it.get(),
            flashcardRepository = get(),
            updateFlashcardUseCase = get(),
            softDeleteFlashcardUseCase = get(),
        )
    }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { OnboardingViewModel(get()) }
}

fun provideSqlDriver(context: Context): SqlDriver {
    return AndroidSqliteDriver(
        schema = HelloDb.Schema,
        context = context,
        name = "${BuildConfig.APPLICATION_ID}.db",
        callback = csm()
    )
}

fun csm() = object : AndroidSqliteDriver.Callback(schema = HelloDb.Schema) {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }
}

fun provideDb(sqlDriver: SqlDriver): HelloDb = HelloDb(sqlDriver)
