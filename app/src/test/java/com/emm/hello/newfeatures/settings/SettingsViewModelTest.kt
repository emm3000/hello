package com.emm.hello.newfeatures.settings

import android.net.Uri
import com.emm.data.export.BackupExporter
import com.emm.data.export.BackupImporter
import com.emm.domain.reminder.GetStudyReminderSettingsUseCase
import com.emm.domain.reminder.SetStudyReminderEnabledUseCase
import com.emm.domain.reminder.SetStudyReminderTimeUseCase
import com.emm.domain.reminder.StudyReminderScheduler
import com.emm.domain.reminder.StudyReminderSettings
import com.emm.domain.reminder.StudyReminderSettingsRepository
import com.emm.domain.reminder.SyncStudyReminderUseCase
import com.emm.hello.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import java.time.LocalTime
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reminderRepository = FakeStudyReminderSettingsRepository(
        StudyReminderSettings(isEnabled = false, time = StudyReminderSettings.DEFAULT_TIME),
    )
    private val reminderScheduler = RecordingStudyReminderScheduler()
    private val getStudyReminderSettings = GetStudyReminderSettingsUseCase(reminderRepository)
    private val setStudyReminderEnabled = SetStudyReminderEnabledUseCase(
        reminderRepository,
        SyncStudyReminderUseCase(reminderRepository, reminderScheduler),
    )
    private val setStudyReminderTime = SetStudyReminderTimeUseCase(
        reminderRepository,
        SyncStudyReminderUseCase(reminderRepository, reminderScheduler),
    )

    @Before
    fun setup() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)
    }

    private fun buildViewModel(
        exportDataSource: BackupExporter = FakeBackupExporter(),
        importDataSource: BackupImporter = FakeBackupImporter(),
    ): SettingsViewModel = SettingsViewModel(
        exportDataSource,
        importDataSource,
        getStudyReminderSettings,
        setStudyReminderEnabled,
        setStudyReminderTime,
    )

    @Test
    fun `ExportUriReceived success emits ShowSuccess effect`() = runTest {
        val exportDataSource = FakeBackupExporter(Result.success(Unit))
        val importDataSource = FakeBackupImporter()
        val viewModel = buildViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/export.json")

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(SettingsUiIntent.ExportUriReceived(uri))

        val effect = effectDeferred.await()
        assertThat(effect).isEqualTo(SettingsUiEffect.ShowSuccess("Backup exported successfully"))
        assertThat(viewModel.state.value.isExporting).isFalse()
    }

    @Test
    fun `ExportUriReceived failure emits ShowError effect`() = runTest {
        val exportDataSource = FakeBackupExporter(Result.failure(Exception("Export failed")))
        val importDataSource = FakeBackupImporter()
        val viewModel = buildViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/export.json")

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(SettingsUiIntent.ExportUriReceived(uri))

        val effect = effectDeferred.await()
        assertThat(effect).isInstanceOf(SettingsUiEffect.ShowError::class.java)
        assertThat((effect as SettingsUiEffect.ShowError).message).isEqualTo("Couldn't export the backup")
        assertThat(viewModel.state.value.isExporting).isFalse()
    }

    @Test
    fun `ImportUriReceived shows confirmation dialog and stores uri`() = runTest {
        val exportDataSource = FakeBackupExporter()
        val importDataSource = FakeBackupImporter()
        val viewModel = buildViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/import.json")
        viewModel.onIntent(SettingsUiIntent.ImportUriReceived(uri))

        assertThat(viewModel.state.value.isConfirmDialogVisible).isTrue()
        assertThat(viewModel.state.value.pendingImportUri).isEqualTo(uri)
    }

    @Test
    fun `confirmImport success emits ShowSuccess and clears dialog`() = runTest {
        val exportDataSource = FakeBackupExporter()
        val importDataSource = FakeBackupImporter(Result.success(Unit))
        val viewModel = buildViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/import.json")
        viewModel.onIntent(SettingsUiIntent.ImportUriReceived(uri))
        assertThat(viewModel.state.value.isConfirmDialogVisible).isTrue()

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(SettingsUiIntent.ConfirmImport)

        val effect = effectDeferred.await()
        assertThat(effect).isEqualTo(SettingsUiEffect.ShowSuccess("Backup restored"))
        assertThat(viewModel.state.value.isConfirmDialogVisible).isFalse()
        assertThat(viewModel.state.value.isImporting).isFalse()
        assertThat(viewModel.state.value.pendingImportUri).isNull()
    }

    @Test
    fun `confirmImport failure emits ShowError and clears dialog`() = runTest {
        val exportDataSource = FakeBackupExporter()
        val importDataSource = FakeBackupImporter(Result.failure(Exception("Import failed")))
        val viewModel = buildViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/import.json")
        viewModel.onIntent(SettingsUiIntent.ImportUriReceived(uri))

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(SettingsUiIntent.ConfirmImport)

        val effect = effectDeferred.await()
        assertThat(effect).isInstanceOf(SettingsUiEffect.ShowError::class.java)
        assertThat((effect as SettingsUiEffect.ShowError).message).isEqualTo("Couldn't restore the backup.")
        assertThat(viewModel.state.value.isConfirmDialogVisible).isFalse()
    }

    @Test
    fun `cancelImport dismisses dialog without importing`() = runTest {
        val exportDataSource = FakeBackupExporter()
        val importDataSource = FakeBackupImporter()
        val viewModel = buildViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/import.json")
        viewModel.onIntent(SettingsUiIntent.ImportUriReceived(uri))
        assertThat(viewModel.state.value.isConfirmDialogVisible).isTrue()

        viewModel.onIntent(SettingsUiIntent.CancelImport)

        assertThat(viewModel.state.value.isConfirmDialogVisible).isFalse()
        assertThat(viewModel.state.value.pendingImportUri).isNull()
        assertThat(importDataSource.importCalled).isFalse()
    }

    @Test
    fun `isExporting is true during export operation`() = runTest {
        val exportDataSource = FakeBackupExporter(suspendDuring = 100L)
        val importDataSource = FakeBackupImporter()
        val viewModel = buildViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/export.json")

        viewModel.onIntent(SettingsUiIntent.ExportUriReceived(uri))
        assertThat(viewModel.state.value.isExporting).isTrue()
    }

    @Test
    fun `isImporting is true during import operation`() = runTest {
        val exportDataSource = FakeBackupExporter()
        val importDataSource = FakeBackupImporter(suspendDuring = 100L)
        val viewModel = buildViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/import.json")
        viewModel.onIntent(SettingsUiIntent.ImportUriReceived(uri))

        viewModel.onIntent(SettingsUiIntent.ConfirmImport)
        assertThat(viewModel.state.value.isImporting).isTrue()
    }

    @Test
    fun `ExportData intent emits LaunchExportPicker effect`() = runTest {
        val viewModel = buildViewModel()

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(SettingsUiIntent.ExportData)

        assertThat(effectDeferred.await()).isEqualTo(SettingsUiEffect.LaunchExportPicker)
        assertThat(viewModel.state.value.isExporting).isFalse()
    }

    @Test
    fun `ImportData intent emits LaunchImportPicker effect`() = runTest {
        val viewModel = buildViewModel()

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(SettingsUiIntent.ImportData)

        assertThat(effectDeferred.await()).isEqualTo(SettingsUiEffect.LaunchImportPicker)
        assertThat(viewModel.state.value.isConfirmDialogVisible).isFalse()
    }

    @Test
    fun `initial state reflects a disabled reminder repository`() = runTest {
        val viewModel = buildViewModel()

        assertThat(viewModel.state.value.isReminderEnabled).isFalse()
        assertThat(viewModel.state.value.reminderTime).isEqualTo(LocalTime.of(19, 0))
    }

    @Test
    fun `SetReminderEnabled false persists false and cancels`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onIntent(SettingsUiIntent.SetReminderEnabled(false))

        assertThat(reminderRepository.setEnabledCalls).containsExactly(false)
        assertThat(reminderScheduler.cancelCount).isEqualTo(1)
        assertThat(viewModel.state.value.isReminderEnabled).isFalse()
    }

    @Test
    fun `SetReminderEnabled true persists true and schedules with stored time`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onIntent(SettingsUiIntent.SetReminderEnabled(true))

        assertThat(reminderRepository.setEnabledCalls).containsExactly(true)
        assertThat(reminderScheduler.scheduledTimes).containsExactly(StudyReminderSettings.DEFAULT_TIME)
        assertThat(viewModel.state.value.isReminderEnabled).isTrue()
    }

    @Test
    fun `EditReminderTime shows the picker`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onIntent(SettingsUiIntent.EditReminderTime)

        assertThat(viewModel.state.value.isReminderTimePickerVisible).isTrue()
    }

    @Test
    fun `DismissReminderTimePicker hides the picker`() = runTest {
        val viewModel = buildViewModel()
        viewModel.onIntent(SettingsUiIntent.EditReminderTime)

        viewModel.onIntent(SettingsUiIntent.DismissReminderTimePicker)

        assertThat(viewModel.state.value.isReminderTimePickerVisible).isFalse()
    }

    @Test
    fun `SetReminderTime persists the time, schedules once when enabled, and hides the picker`() = runTest {
        val time: LocalTime = LocalTime.of(7, 30)
        val repository = FakeStudyReminderSettingsRepository(
            StudyReminderSettings(isEnabled = true, time = StudyReminderSettings.DEFAULT_TIME),
        )
        val scheduler = RecordingStudyReminderScheduler()
        val syncStudyReminder = SyncStudyReminderUseCase(repository, scheduler)
        val viewModel = SettingsViewModel(
            FakeBackupExporter(),
            FakeBackupImporter(),
            GetStudyReminderSettingsUseCase(repository),
            SetStudyReminderEnabledUseCase(repository, syncStudyReminder),
            SetStudyReminderTimeUseCase(repository, syncStudyReminder),
        )
        viewModel.onIntent(SettingsUiIntent.EditReminderTime)

        viewModel.onIntent(SettingsUiIntent.SetReminderTime(time))

        assertThat(repository.setTimeCalls).containsExactly(time)
        assertThat(scheduler.scheduledTimes).containsExactly(time)
        assertThat(viewModel.state.value.reminderTime).isEqualTo(time)
        assertThat(viewModel.state.value.isReminderTimePickerVisible).isFalse()
    }
}

private class FakeBackupExporter(
    private val result: Result<Unit> = Result.success(Unit),
    private val suspendDuring: Long = 0L,
) : BackupExporter {

    var exportCalled = false

    override suspend fun export(outputUri: Uri): Result<Unit> {
        exportCalled = true
        if (suspendDuring > 0) kotlinx.coroutines.delay(suspendDuring)
        return result
    }
}

private class FakeBackupImporter(
    private val result: Result<Unit> = Result.success(Unit),
    private val suspendDuring: Long = 0L,
) : BackupImporter {

    var importCalled = false

    override suspend fun import(inputUri: Uri): Result<Unit> {
        importCalled = true
        if (suspendDuring > 0) kotlinx.coroutines.delay(suspendDuring)
        return result
    }
}

private class FakeStudyReminderSettingsRepository(
    private var settings: StudyReminderSettings,
) : StudyReminderSettingsRepository {

    var setEnabledCalls: List<Boolean> = emptyList()
        private set

    var setTimeCalls: List<LocalTime> = emptyList()
        private set

    override fun get(): StudyReminderSettings = settings

    override fun setEnabled(isEnabled: Boolean) {
        settings = settings.copy(isEnabled = isEnabled)
        setEnabledCalls = setEnabledCalls + isEnabled
    }

    override fun setTime(time: LocalTime) {
        settings = settings.copy(time = time)
        setTimeCalls = setTimeCalls + time
    }
}

private class RecordingStudyReminderScheduler : StudyReminderScheduler {

    var scheduledTimes: List<LocalTime> = emptyList()
        private set

    var cancelCount: Int = 0
        private set

    override fun schedule(time: LocalTime) {
        scheduledTimes = scheduledTimes + time
    }

    override fun cancel() {
        cancelCount += 1
    }
}
