package com.emm.hello

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.features.DataStore
import com.emm.hello.features.backup.DataModeler
import com.emm.hello.features.backup.domain.LocalStorageRepository
import com.emm.hello.newfeatures.NewRoot
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val dataModeler: DataModeler by inject()
    private val localStorageRepository: LocalStorageRepository by inject()
    private val dataStore: DataStore by lazy { DataStore(applicationContext) }

    private val launcherForLegacyExternalPermissions: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        callback = ::isAllTrue
    )

    private val launcherForExternalFile = registerForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        callback = ::onFilePicked,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }
        setContent {
            HelloTheme {
                NewRoot()
            }
        }
    }

    private fun checkPermissionForBackup() = isAtLeastApi30(
        truly = ::isFirstTimeOpeningTheApp,
        falsely = ::callLegacyPermissions
    )

    private fun onFilePicked(uri: Uri?) = lifecycleScope.launch {
        uri ?: return@launch run { readBackupThenActivateConstantBackup() }
        dataStore.uri = uri.toString()
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, takeFlags)
        localStorageRepository.read(uri)
        readBackupThenActivateConstantBackup()
    }

    private fun isFirstTimeOpeningTheApp() {
        if (dataStore.isFirstLaunch) {
            dataStore.setFirstLaunchCompleted()
            launcherForExternalFile.launch(arrayOf("*/*"))
        } else {
            readBackupThenActivateConstantBackup()
        }
    }

    private fun callLegacyPermissions() {
        val permissionsArray: Array<String> = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
        launcherForLegacyExternalPermissions.launch(permissionsArray)
    }

    private fun isAllTrue(conditions: Map<String, Boolean>) = lifecycleScope.launch {
        val allTrue: Boolean = conditions.values.all { it }
        if (allTrue) {
            localStorageRepository.readPower()
            readBackupThenActivateConstantBackup()
        } else {
            checkPermissionForBackup()
        }
    }

    private fun readBackupThenActivateConstantBackup() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            dataModeler.observeAll().collectLatest { localStorageRepository.save() }
        }
    }
}