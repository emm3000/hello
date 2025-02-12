package com.emm.hello

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.features.DataStore
import com.emm.hello.features.backup.DataModeler
import com.emm.hello.features.backup.domain.LocalStorageRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val dataModeler: DataModeler by inject()
    private val localStorageRepository: LocalStorageRepository by inject()
    private val dataStore: DataStore by lazy { DataStore(applicationContext) }

    private val activityResultLauncher = registerForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        callback = ::onFilePicked,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initBackupListener()
        isFirstTimeOpeningTheApp()
        setContent {
            HelloTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Root(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun onFilePicked(uri: Uri?) = lifecycleScope.launch {
        uri ?: return@launch
        localStorageRepository.read(uri)
    }

    private fun isFirstTimeOpeningTheApp() {
        if (dataStore.isFirstLaunch) {
            dataStore.setFirstLaunchCompleted()
            activityResultLauncher.launch(arrayOf("*/*"))
        }
    }

    private fun initBackupListener() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataModeler.observeAll().collectLatest { localStorageRepository.save() }
            }
        }
    }
}