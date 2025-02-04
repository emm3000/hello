package com.emm.hello

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.emm.data.WordDao
import com.emm.data.WordEntity
import com.emm.hello.features.addword.AddWordDialog
import com.emm.hello.features.backup.BackupScreen
import com.emm.hello.features.detail.DetailScreen
import com.emm.hello.features.main.MainScreen
import com.emm.hello.features.main.MainViewModel
import com.emm.hello.features.home.HomeScreen
import com.emm.hello.features.backup.JustFiles
import com.emm.hello.core.AddWord
import com.emm.hello.core.Backup
import com.emm.hello.core.Detail
import com.emm.hello.core.Home
import com.emm.hello.core.Main
import com.emm.hello.features.addword.domain.Word
import com.emm.hello.features.addword.domain.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
            val wordDao: WordDao = koinInject()

            val (word, setWord) = remember {
                mutableStateOf<WordEntity?>(null)
            }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    val wordEntity: WordEntity = wordDao.selectBy(detail.wordId) ?: return@withContext
                    setWord(wordEntity)
                }
            }

            if (word != null) {
                DetailScreen(word)
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
            val justFiles: JustFiles = koinInject()
            val scope = rememberCoroutineScope()
            val ga = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

            }

            LaunchedEffect(Unit) {
                ga.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    )
                )
            }

            BackupScreen(
                internalOnSave = {
                    scope.launch {
                        justFiles.saveInternal()
                    }
                },
                internalOnUpdate = {
                    scope.launch {
                        justFiles.saveInternal()
                    }
                },
                internalOnDelete = {
                    justFiles.deleteInternal()
                },
                externalOnSave = {
                    scope.launch {
                        justFiles.saveExternal()
                    }
                },
                externalOnUpdate = {
                    scope.launch {
                        justFiles.saveExternal()
                    }
                },
                externalOnDelete = {
                    justFiles.deleteExternal()
                },
                sharedOnSave = {
                    scope.launch {
                        justFiles.saveJsonToDownloads()
                    }
                },
                sharedOnUpdate = {},
                sharedOnDelete = {},
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
    val now: LocalDateTime = LocalDateTime.now()
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a")
    val formattedDate = now.format(formatter)
    val word = Word(
        id = UUID.randomUUID().toString(),
        word = wordName,
        createdAt = formattedDate,
    )
    coroutineScope.launch {
        repository.upsert(word)
        navController.popBackStack()
    }
}