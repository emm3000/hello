package com.emm.hello.newfeatures.settings

import android.net.Uri
import com.emm.data.export.ExportBackupDataSource
import com.emm.data.export.ExportException
import com.emm.data.export.ImportBackupDataSource
import com.emm.data.export.ImportException
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
    fun `onExportUri success emits ShowSuccess effect`() = runTest {
        val exportDataSource = FakeExportDataSource(Result.success(Unit))
        val importDataSource = FakeImportDataSource()
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/export.json")

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onExportUri(uri)

        val effect = effectDeferred.await()
        assertThat(effect).isEqualTo(SettingsUiEffect.ShowSuccess("Backup exported successfully"))
        assertThat(viewModel.uiState.value.isExporting).isFalse()
    }

    @Test
    fun `onExportUri failure emits ShowError effect`() = runTest {
        val exportDataSource = FakeExportDataSource(Result.failure(ExportException("Export failed")))
        val importDataSource = FakeImportDataSource()
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/export.json")

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onExportUri(uri)

        val effect = effectDeferred.await()
        assertThat(effect).isInstanceOf(SettingsUiEffect.ShowError::class.java)
        assertThat((effect as SettingsUiEffect.ShowError).message).isEqualTo("Export failed")
        assertThat(viewModel.uiState.value.isExporting).isFalse()
    }

    @Test
    fun `onImportUri shows confirmation dialog`() = runTest {
        val exportDataSource = FakeExportDataSource()
        val importDataSource = FakeImportDataSource()
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/import.json")
        viewModel.onImportUri(uri)

        assertThat(viewModel.uiState.value.showConfirmDialog).isTrue()
    }

    @Test
    fun `confirmImport success emits ShowSuccess and clears dialog`() = runTest {
        val exportDataSource = FakeExportDataSource()
        val importDataSource = FakeImportDataSource(Result.success(Unit))
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/import.json")
        viewModel.onImportUri(uri)
        assertThat(viewModel.uiState.value.showConfirmDialog).isTrue()

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(SettingsUiIntent.ConfirmImport)

        // Wait for effect
        val effect = effectDeferred.await()
        assertThat(effect).isEqualTo(SettingsUiEffect.ShowSuccess("Backup restored successfully"))
        assertThat(viewModel.uiState.value.showConfirmDialog).isFalse()
        assertThat(viewModel.uiState.value.isImporting).isFalse()
    }

    @Test
    fun `confirmImport failure emits ShowError and clears dialog`() = runTest {
        val exportDataSource = FakeExportDataSource()
        val importDataSource = FakeImportDataSource(Result.failure(ImportException("Import failed")))
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/import.json")
        viewModel.onImportUri(uri)

        val effectDeferred = backgroundScope.async { viewModel.effect.first() }
        viewModel.onIntent(SettingsUiIntent.ConfirmImport)

        val effect = effectDeferred.await()
        assertThat(effect).isInstanceOf(SettingsUiEffect.ShowError::class.java)
        assertThat((effect as SettingsUiEffect.ShowError).message).isEqualTo("Import failed")
        assertThat(viewModel.uiState.value.showConfirmDialog).isFalse()
    }

    @Test
    fun `cancelImport dismisses dialog without importing`() = runTest {
        val exportDataSource = FakeExportDataSource()
        val importDataSource = FakeImportDataSource()
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/import.json")
        viewModel.onImportUri(uri)
        assertThat(viewModel.uiState.value.showConfirmDialog).isTrue()

        viewModel.onIntent(SettingsUiIntent.CancelImport)

        assertThat(viewModel.uiState.value.showConfirmDialog).isFalse()
        assertThat(importDataSource.importCalled).isFalse()
    }

    @Test
    fun `isExporting is true during export operation`() = runTest {
        val exportDataSource = FakeExportDataSource(suspendDuring = 100L)
        val importDataSource = FakeImportDataSource()
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/export.json")

        // Start export and check state immediately
        viewModel.onExportUri(uri)
        assertThat(viewModel.uiState.value.isExporting).isTrue()
    }

    @Test
    fun `isImporting is true during import operation`() = runTest {
        val exportDataSource = FakeExportDataSource()
        val importDataSource = FakeImportDataSource(suspendDuring = 100L)
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

        val uri = Uri.parse("content://test/import.json")
        viewModel.onImportUri(uri)

        viewModel.onIntent(SettingsUiIntent.ConfirmImport)
        // State should be importing right after ConfirmImport
        assertThat(viewModel.uiState.value.isImporting).isTrue()
    }

    @Test
    fun `ExportData intent does nothing (SAF picker handled by Route)`() = runTest {
        val exportDataSource = FakeExportDataSource()
        val importDataSource = FakeImportDataSource()
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

        viewModel.onIntent(SettingsUiIntent.ExportData)

        assertThat(viewModel.uiState.value.isExporting).isFalse()
        assertThat(exportDataSource.exportCalled).isFalse()
    }

    @Test
    fun `ImportData intent does nothing (SAF picker handled by Route)`() = runTest {
        val exportDataSource = FakeExportDataSource()
        val importDataSource = FakeImportDataSource()
        val viewModel = SettingsViewModel(exportDataSource, importDataSource)

        viewModel.onIntent(SettingsUiIntent.ImportData)

        assertThat(viewModel.uiState.value.showConfirmDialog).isFalse()
    }
}

private class FakeExportDataSource(
    private val result: Result<Unit> = Result.success(Unit),
    private val suspendDuring: Long = 0L,
) : ExportBackupDataSource(mockk(relaxed = true), mockk(relaxed = true)) {

    var exportCalled = false

    override suspend fun export(outputUri: Uri): Result<Unit> {
        exportCalled = true
        if (suspendDuring > 0) kotlinx.coroutines.delay(suspendDuring)
        return result
    }
}

private class FakeImportDataSource(
    private val result: Result<Unit> = Result.success(Unit),
    private val suspendDuring: Long = 0L,
) : ImportBackupDataSource(mockk(relaxed = true), mockk(relaxed = true)) {

    var importCalled = false

    override suspend fun import(inputUri: Uri): Result<Unit> {
        importCalled = true
        if (suspendDuring > 0) kotlinx.coroutines.delay(suspendDuring)
        return result
    }
}
