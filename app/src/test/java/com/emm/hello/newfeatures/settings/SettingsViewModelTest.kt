package com.emm.hello.newfeatures.settings

import android.net.Uri
import com.emm.data.export.BackupExporter
import com.emm.data.export.BackupImporter
import com.emm.hello.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setup() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)
    }

    @Test
    fun `ExportUriReceived success emits ShowSuccess effect`() = runTest {
        val exportDataSource = FakeBackupExporter(Result.success(Unit))
        val importDataSource = FakeBackupImporter()
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

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
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

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
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/import.json")
        viewModel.onIntent(SettingsUiIntent.ImportUriReceived(uri))

        assertThat(viewModel.state.value.isConfirmDialogVisible).isTrue()
        assertThat(viewModel.state.value.pendingImportUri).isEqualTo(uri)
    }

    @Test
    fun `confirmImport success emits ShowSuccess and clears dialog`() = runTest {
        val exportDataSource = FakeBackupExporter()
        val importDataSource = FakeBackupImporter(Result.success(Unit))
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

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
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

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
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

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
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/export.json")

        viewModel.onIntent(SettingsUiIntent.ExportUriReceived(uri))
        assertThat(viewModel.state.value.isExporting).isTrue()
    }

    @Test
    fun `isImporting is true during import operation`() = runTest {
        val exportDataSource = FakeBackupExporter()
        val importDataSource = FakeBackupImporter(suspendDuring = 100L)
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/import.json")
        viewModel.onIntent(SettingsUiIntent.ImportUriReceived(uri))

        viewModel.onIntent(SettingsUiIntent.ConfirmImport)
        assertThat(viewModel.state.value.isImporting).isTrue()
    }

    @Test
    fun `ExportData intent emits LaunchExportPicker effect`() = runTest {
        val viewModel = SettingsViewModel(FakeBackupExporter(), FakeBackupImporter())

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(SettingsUiIntent.ExportData)

        assertThat(effectDeferred.await()).isEqualTo(SettingsUiEffect.LaunchExportPicker)
        assertThat(viewModel.state.value.isExporting).isFalse()
    }

    @Test
    fun `ImportData intent emits LaunchImportPicker effect`() = runTest {
        val viewModel = SettingsViewModel(FakeBackupExporter(), FakeBackupImporter())

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(SettingsUiIntent.ImportData)

        assertThat(effectDeferred.await()).isEqualTo(SettingsUiEffect.LaunchImportPicker)
        assertThat(viewModel.state.value.isConfirmDialogVisible).isFalse()
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
