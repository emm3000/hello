package com.emm.hello

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.emm.data.word.WordEntity
import com.emm.domain.SourceType
import com.emm.domain.Word
import com.emm.domain.WordRepository
import com.emm.hello.core.AddWord
import com.emm.hello.core.Backup
import com.emm.hello.core.Detail
import com.emm.hello.core.Home
import com.emm.hello.core.Main
import com.emm.hello.features.addword.AddWordDialog
import com.emm.hello.features.backup.BackupScreen
import com.emm.hello.features.backup.domain.LocalStorageRepository
import com.emm.hello.features.detail.DetailScreen
import com.emm.hello.features.detail.DetailUiState
import com.emm.hello.features.detail.DetailViewModel
import com.emm.hello.features.home.HomeScreen
import com.emm.hello.features.main.MainScreen
import com.emm.hello.features.main.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.Instant
import java.util.UUID

@Composable
fun Root(modifier: Modifier = Modifier) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier,
    ) {

        composable<Home> {
            HomeScreen(
                onClick = { navController.navigate(Main) },
                modifier = Modifier,
            )
        }
        composable<Main> {
            val vm: MainViewModel = koinViewModel()

            val words: List<WordEntity> by vm.words.collectAsStateWithLifecycle()

            MainScreen(
                words = words,
                wordSearch = vm.searchState,
                onWordSearchUpdate = vm::updateSearch,
                navigateToDetail = { navController.navigate(Detail(it)) },
                navigateToAddWord = { navController.navigate(AddWord) },
                navigateToBackup = { navController.navigate(Backup) },
                modifier = Modifier,
            )
        }

        composable<Detail> {
            val detail: Detail = it.toRoute<Detail>()
            val repository: WordRepository = koinInject()
            val coroutineScope: CoroutineScope = rememberCoroutineScope()
            val vm: DetailViewModel = koinViewModel()
            val state: DetailUiState = vm.state

            LaunchedEffect(Unit) {
                vm.detail(detail.wordId)
            }

            LaunchedEffect(state.isDeleteSuccess) {
                if (state.isDeleteSuccess) {
                    navController.popBackStack()
                }
            }

            if (state.currentWord != null) {
                DetailScreen(
                    state = state,
                    wordName = state.currentWord.word,
                    generateContent = {
                        vm.contentCreator(state.currentWord, sourceType = SourceType.SCRAPPING)
                    },
                    deleteWord = {
                        vm.delete(state.currentWord.id)
                    },
                    updateWord = { newWordName ->
                        coroutineScope.launch {
                            val newWord: Word = state.currentWord.copy(word = newWordName)
                            repository.upsert(newWord)
                        }
                    }
                )
            }
        }

        dialog<AddWord>(
            dialogProperties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            val repository: WordRepository = koinInject()
            val coroutineScope = rememberCoroutineScope()

            AddWordDialog(
                onAddWord = { word ->
                    addWord(
                        coroutineScope = coroutineScope,
                        wordName = word,
                        repository = repository,
                        navController = navController,
                    )
                },
                onDismiss = { navController.popBackStack() }
            )
        }

        composable<Backup> {
            val repository: LocalStorageRepository = koinInject()
            val scope = rememberCoroutineScope()
            val permissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

            }

            val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
                val uri: Uri = it ?: return@rememberLauncherForActivityResult
                scope.launch {
                    repository.read(uri)
                    navController.popBackStack()
                }
            }

            LaunchedEffect(Unit) {
                permissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    )
                )
            }

            BackupScreen(
                exportAsJson = {
                    scope.launch {
                        repository.save()
                        navController.popBackStack()
                    }
                },
                populateDb = {
                    fileLauncher.launch(arrayOf("*/*"))
                },
            )
        }
    }
}

private fun addWord(
    coroutineScope: CoroutineScope,
    wordName: String,
    repository: WordRepository,
    navController: NavHostController
) {
    val toEpochMilli = Instant.now().toEpochMilli()
    val word = Word(
        id = UUID.randomUUID().toString(),
        word = wordName,
        hasContent = false,
        createdAt = toEpochMilli,
    )
    coroutineScope.launch {
        repository.upsert(word)
        navController.popBackStack()
    }
}