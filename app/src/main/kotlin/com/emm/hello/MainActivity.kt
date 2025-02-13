package com.emm.hello

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.features.backup.DataModeler
import com.emm.hello.features.backup.domain.LocalStorageRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val dataModeler: DataModeler by inject()
    private val localStorageRepository: LocalStorageRepository by inject()

    private val launcherForExternalPermissionSettings = registerForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        callback = ::checkBackupPermissions,
    )

    private val launcherForLegacyExternalPermissions: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        callback = ::isAllTrue
    )

    @Suppress("UNUSED_PARAMETER")
    private fun checkBackupPermissions(result: ActivityResult) {
        checkPermissionForBackup()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermissionForBackup()
        setContent {
            HelloTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Root(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun checkPermissionForBackup() {
        if (hasPermissions()) {
            readBackupThenActivateConstantBackup()
        } else {
            isSdk30OrNewer(
                truly = ::requestManageAllFilesAccess,
                falsely = ::callLegacyPermissions
            )
        }
    }

    private fun callLegacyPermissions() {
        val permissionsArray: Array<String> = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
        launcherForLegacyExternalPermissions.launch(permissionsArray)
    }

    @SuppressLint("InlinedApi")
    private fun requestManageAllFilesAccess() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = Uri.parse("package:${packageName}")
        launcherForExternalPermissionSettings.launch(intent)
    }

    private fun isAllTrue(conditions: Map<String, Boolean>) {
        val allTrue = conditions.values.all { it }
        if (allTrue) readBackupThenActivateConstantBackup() else checkPermissionForBackup()
    }

    private fun readBackupThenActivateConstantBackup() = lifecycleScope.launch {
        localStorageRepository.readPower()
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            dataModeler.observeAll().collectLatest { localStorageRepository.save() }
        }
    }
}