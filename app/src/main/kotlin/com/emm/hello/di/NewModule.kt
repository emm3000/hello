package com.emm.hello.di

import android.content.Context
import android.content.SharedPreferences
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.connectivity.AndroidConnectivityRepository
import com.emm.data.curated.BundledCuratedDeckCatalog
import com.emm.data.deck.DefaultDeckRepository
import com.emm.data.deck.DefaultDeckSelectionPreferencesRepository
import com.emm.data.deck.DefaultTagRepository
import com.emm.data.flashcard.DefaultFlashcardDuplicateRepository
import com.emm.data.flashcard.DefaultFlashcardEnrichmentRepository
import com.emm.data.library.DefaultLibraryRepository
import com.emm.data.flashcard.DefaultFlashcardRepository
import com.emm.data.flashcard.DefaultFlashcardReviewRepository
import com.emm.data.flashcard.DefaultStudySessionRepository
import com.emm.data.flashcard.RemoteFlashcardGenerationRepository
import com.emm.data.suggestion.CannedWordSuggestionRepository
import com.emm.data.suggestion.RemoteWordSuggestionRepository
import com.emm.data.study.DefaultStudyStatsRepository
import com.emm.data.localfirst.DefaultLocalIdentityInitializer
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.seed.DefaultSeedDataInitializer
import com.emm.domain.localfirst.LocalIdentityInitializer
import com.emm.domain.seed.SeedDataInitializer
import com.emm.data.remote.DataStore
import com.emm.data.remote.SupabaseAccountRepository
import com.emm.data.remote.provideSharedPreferences
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.GetAccountUseCase
import com.emm.domain.account.LinkGoogleAccountUseCase
import com.emm.domain.authoring.CaptureFlashcardUseCase
import com.emm.domain.authoring.CreateFlashcardUseCase
import com.emm.domain.authoring.CreateManualFlashcardUseCase
import com.emm.domain.authoring.EnrichCapturedFlashcardUseCase
import com.emm.domain.authoring.FindPendingEnrichmentsUseCase
import com.emm.domain.authoring.MarkEnrichmentFailedUseCase
import com.emm.domain.authoring.RetryFailedEnrichmentsUseCase
import com.emm.domain.authoring.EnsureUniqueFlashcardInDeckUseCase
import com.emm.domain.authoring.GeneratedLearningNoteMapper
import com.emm.domain.authoring.IsExactDuplicateGeneratedNoteUseCase
import com.emm.domain.connectivity.ConnectivityRepository
import com.emm.domain.curated.CuratedDeckCatalog
import com.emm.domain.curated.GetCuratedDecksUseCase
import com.emm.domain.curated.InstallCuratedDeckUseCase
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.RestoreDeckUseCase
import com.emm.domain.deck.SoftDeleteDeckUseCase
import com.emm.domain.deck.UpdateDeckUseCase
import com.emm.domain.flashcard.FlashcardDuplicateRepository
import com.emm.domain.flashcard.FlashcardGenerationRepository
import com.emm.domain.flashcard.FlashcardEnrichmentRepository
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.flashcard.CountDueFlashcardsUseCase
import com.emm.domain.flashcard.RestoreFlashcardUseCase
import com.emm.domain.flashcard.UpdateFlashcardUseCase
import com.emm.domain.library.LibraryRepository
import com.emm.domain.library.SearchLibraryUseCase
import com.emm.domain.study.ObserveFlashcardsWithReviewUseCase
import com.emm.domain.study.GetDashboardStatsUseCase
import com.emm.domain.study.GetStudySessionUseCase
import com.emm.domain.study.StudyStatsRepository
import com.emm.domain.suggestion.SuggestWordsUseCase
import com.emm.domain.suggestion.WordSuggestionRepository
import com.emm.hello.newfeatures.suggest.SuggestViewModel
import com.emm.domain.flashcard.FsrsParameters
import com.emm.domain.generation.GeneratedLearningNoteCardsPolicy
import com.emm.domain.generation.GeneratedLearningNoteCoreFieldsPolicy
import com.emm.domain.generation.GeneratedLearningNoteExamplePolicy
import com.emm.domain.generation.GeneratedLearningNoteQualityChecksPolicy
import com.emm.domain.generation.GeneratedLearningNoteTypeRequirementsPolicy
import com.emm.domain.generation.ValidateGeneratedLearningNoteUseCase
import com.emm.domain.study.ScheduleFlashcardReviewUseCase
import com.emm.domain.study.StudySessionRepository
import com.emm.domain.time.Clock
import com.emm.domain.time.SystemClock
import com.emm.hello.BuildConfig
import com.emm.hello.R
import com.emm.data.export.BackupExporter
import com.emm.data.export.BackupImporter
import com.emm.data.export.ExportBackupDataSource
import com.emm.data.export.ImportBackupDataSource
import com.emm.data.onboarding.DataStoreOnboardingStateRepository
import com.emm.data.reminder.DataStoreStudyReminderSettingsRepository
import com.emm.domain.onboarding.OnboardingStateRepository
import com.emm.domain.reminder.GetStudyReminderSettingsUseCase
import com.emm.domain.reminder.SetStudyReminderEnabledUseCase
import com.emm.domain.reminder.SetStudyReminderTimeUseCase
import com.emm.domain.reminder.StudyReminderSettingsRepository
import com.emm.domain.reminder.SyncStudyReminderUseCase
import com.emm.domain.reminder.StudyReminderScheduler
import com.emm.hello.notifications.NotificationPermission
import com.emm.hello.notifications.SystemNotificationPermission
import com.emm.hello.notifications.WorkManagerStudyReminderScheduler
import com.emm.hello.newfeatures.card.EditFlashcardViewModel
import com.emm.hello.newfeatures.onboarding.OnboardingViewModel
import com.emm.hello.newfeatures.shared.UndoEventHolder
import com.emm.hello.newfeatures.card.FlashcardDetailViewModel
import com.emm.hello.newfeatures.capture.CaptureViewModel
import com.emm.hello.newfeatures.today.TodayViewModel
import com.emm.hello.newfeatures.deck.DecksViewModel
import com.emm.hello.newfeatures.library.LibraryViewModel
import com.emm.hello.newfeatures.deck.NewDeckViewModel
import com.emm.hello.newfeatures.settings.BuildInfo
import com.emm.hello.newfeatures.settings.SettingsViewModel
import com.emm.hello.newfeatures.store.StoreViewModel
import com.emm.hello.newfeatures.study.StudyViewModel
import com.emm.hello.core.activity.CurrentActivityHolder
import com.emm.hello.core.audio.TextToSpeechManager
import com.emm.hello.core.auth.ActivityGoogleSignInLauncher
import com.emm.hello.core.auth.GoogleCredentialClient
import com.emm.hello.core.auth.GoogleSignInLauncher
import com.emm.hello.enrichment.FlashcardEnrichmentScheduler
import com.emm.hello.startup.AppStartupCoordinator
import com.emm.hello.startup.AppStartupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
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
    single<ConnectivityRepository> { AndroidConnectivityRepository(androidContext()) }
    single { TextToSpeechManager(androidContext()) }
    single { GoogleCredentialClient() }
    single { CurrentActivityHolder() }
    single<GoogleSignInLauncher> { ActivityGoogleSignInLauncher(activityHolder = get(), client = get()) }
    single<AccountRepository> { SupabaseAccountRepository(auth = get(), sessionInitializer = get()) }

    factory<com.emm.domain.deck.DeckRepository> {
        DefaultDeckRepository(get(), get())
    } bind DeckRepository::class
    factoryOf(::DefaultTagRepository) bind com.emm.domain.deck.TagRepository::class
    factoryOf(::DefaultDeckSelectionPreferencesRepository) bind DefaultDeckSelectionRepository::class
    single<FlashcardRepository> {
        DefaultFlashcardRepository(db = get(), json = get(), ioDispatcher = Dispatchers.IO)
    }
    single<FlashcardEnrichmentRepository> {
        DefaultFlashcardEnrichmentRepository(db = get(), ioDispatcher = Dispatchers.IO)
    }
    single<LibraryRepository> {
        DefaultLibraryRepository(db = get(), ioDispatcher = Dispatchers.IO)
    }
    single<StudySessionRepository> {
        DefaultStudySessionRepository(db = get(), json = get(), ioDispatcher = Dispatchers.IO)
    }
    single<FlashcardGenerationRepository> {
        RemoteFlashcardGenerationRepository(
            transport = get(),
            session = get(),
            appCheck = get(),
            telemetry = get(),
            json = get(),
        )
    }
    single<WordSuggestionRepository> {
        if (BuildConfig.USE_CANNED_AI) {
            CannedWordSuggestionRepository()
        } else {
            RemoteWordSuggestionRepository(
                transport = get(),
                session = get(),
                appCheck = get(),
                telemetry = get(),
                json = get(),
            )
        }
    }
    factoryOf(::DefaultFlashcardDuplicateRepository) bind FlashcardDuplicateRepository::class
    factoryOf(::DefaultFlashcardReviewRepository) bind FlashcardReviewRepository::class
    factoryOf(::DefaultStudyStatsRepository) bind StudyStatsRepository::class
    factoryOf(::DefaultLocalIdentityInitializer) bind LocalIdentityInitializer::class
    factoryOf(::LocalDeviceIdentityProvider)
    singleOf(::DataStore)
    factoryOf(::ExportBackupDataSource) bind BackupExporter::class
    factoryOf(::ImportBackupDataSource) bind BackupImporter::class
    factoryOf(::DataStoreOnboardingStateRepository) bind OnboardingStateRepository::class
    factoryOf(::DataStoreStudyReminderSettingsRepository) bind StudyReminderSettingsRepository::class
    single<CuratedDeckCatalog> { BundledCuratedDeckCatalog() }
    single<StudyReminderScheduler> { WorkManagerStudyReminderScheduler(androidContext(), get()) }
    single<NotificationPermission> { SystemNotificationPermission(androidContext()) }
    single<SeedDataInitializer> {
        DefaultSeedDataInitializer(
            deckRepository = get(),
            flashcardRepository = get(),
            dataStore = get(),
            deckName = androidContext().getString(R.string.onboarding_seed_deck_name),
        )
    }
    single {
        AppStartupCoordinator(
            localIdentityInitializer = get(),
            seedDataInitializer = get(),
            onboardingStateRepository = get(),
            findPendingEnrichments = get(),
            requeueEnrichments = { ids -> ids.forEach { FlashcardEnrichmentScheduler.enqueue(androidContext(), it) } },
        )
    }
}

fun Module.useCases() {
    single<Clock> { SystemClock }
    single { FsrsParameters.DEFAULT }
    factoryOf(::GeneratedLearningNoteCoreFieldsPolicy)
    factoryOf(::GeneratedLearningNoteTypeRequirementsPolicy)
    factoryOf(::GeneratedLearningNoteCardsPolicy)
    factoryOf(::GeneratedLearningNoteExamplePolicy)
    factoryOf(::GeneratedLearningNoteQualityChecksPolicy)
    factoryOf(::GeneratedLearningNoteMapper)
    factoryOf(::GetDecksUseCase)
    factoryOf(::CreateFlashcardUseCase)
    factoryOf(::CaptureFlashcardUseCase)
    factoryOf(::CreateManualFlashcardUseCase)
    factoryOf(::EnrichCapturedFlashcardUseCase)
    factoryOf(::RetryFailedEnrichmentsUseCase)
    factoryOf(::FindPendingEnrichmentsUseCase)
    factoryOf(::MarkEnrichmentFailedUseCase)
    factoryOf(::EnsureUniqueFlashcardInDeckUseCase)
    factoryOf(::IsExactDuplicateGeneratedNoteUseCase)
    factoryOf(::ValidateGeneratedLearningNoteUseCase)
    factoryOf(::ObserveFlashcardsWithReviewUseCase)
    factoryOf(::ScheduleFlashcardReviewUseCase)
    factory { GetDashboardStatsUseCase(get(), get()) }
    factory { GetStudySessionUseCase(get(), get(), get()) }
    factoryOf(::UpdateDeckUseCase)
    factoryOf(::SoftDeleteDeckUseCase)
    factoryOf(::RestoreDeckUseCase)
    factoryOf(::UpdateFlashcardUseCase)
    factoryOf(::RestoreFlashcardUseCase)
    factoryOf(::CountDueFlashcardsUseCase)
    factoryOf(::SearchLibraryUseCase)
    factoryOf(::SuggestWordsUseCase)
    factoryOf(::SyncStudyReminderUseCase)
    factoryOf(::SetStudyReminderEnabledUseCase)
    factoryOf(::SetStudyReminderTimeUseCase)
    factoryOf(::GetStudyReminderSettingsUseCase)
    factoryOf(::GetAccountUseCase)
    factoryOf(::LinkGoogleAccountUseCase)
    factoryOf(::GetCuratedDecksUseCase)
    factoryOf(::InstallCuratedDeckUseCase)
}

fun Module.viewModels() {
    viewModel { AppStartupViewModel(get()) }
    viewModel { params ->
        NewDeckViewModel(
            deckRepository = get(),
            updateDeckUseCase = get(),
            softDeleteDeckUseCase = get(),
            undoEventHolder = get(),
            formMode = params.get(),
        )
    }
    viewModel { TodayViewModel(get()) }
    viewModel { LibraryViewModel(get(), get(), get(), get(), get()) }
    viewModel { DecksViewModel(get(), get(), get()) }
    viewModel { StoreViewModel(get(), get()) }
    viewModel {
        StudyViewModel(
            deckId = it.get(),
            getStudySessionUseCase = get(),
            scheduleFlashcardReviewUseCase = get(),
            flashcardReviewRepository = get(),
        )
    }
    viewModel {
        FlashcardDetailViewModel(
            flashcardId = it.get(),
            flashcardRepository = get(),
            undoEventHolder = get(),
        )
    }
    viewModel {
        EditFlashcardViewModel(
            flashcardId = it.get(),
            flashcardRepository = get(),
            updateFlashcardUseCase = get(),
        )
    }
    viewModel { CaptureViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SuggestViewModel(get(), get(), get(), get(), get()) }
    single {
        BuildInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            commit = BuildConfig.GIT_COMMIT,
        )
    }
    viewModel {
        SettingsViewModel(
            exportDataSource = get(),
            importDataSource = get(),
            getStudyReminderSettings = get(),
            setStudyReminderEnabled = get(),
            setStudyReminderTime = get(),
            notificationPermission = get(),
            getAccount = get(),
            linkGoogleAccountUseCase = get(),
            googleSignInLauncher = get(),
            googleServerClientId = androidContext().getString(R.string.default_web_client_id),
            noGoogleAccountMessage = androidContext().getString(R.string.settings_google_no_credentials),
            googleLinkFailedMessage = androidContext().getString(R.string.settings_google_link_failed),
            buildInfo = get(),
        )
    }
    viewModel { OnboardingViewModel(get(), get()) }
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
