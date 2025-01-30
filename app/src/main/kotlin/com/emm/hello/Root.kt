package com.emm.hello

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.emm.data.WordDao
import com.emm.data.WordEntity
import com.emm.hello.page.AddWordDialog
import com.emm.hello.page.DetailScreen
import com.emm.hello.page.HomeScreen
import com.emm.hello.page.HomeViewModel
import com.emm.hello.page.InitScreen
import com.emm.hello.route.AddWord
import com.emm.hello.route.Detail
import com.emm.hello.route.Home
import com.emm.hello.route.Init
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.UUID

@Composable
fun Root(modifier: Modifier = Modifier) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Init,
        modifier = modifier,
    ) {

        composable<Init> {
            InitScreen(
                onClick = { navController.navigate(Home) },
                modifier = Modifier,
            )
        }
        composable<Home> {
            val vm: HomeViewModel = koinViewModel()

            val words: List<WordEntity> by vm.words.collectAsStateWithLifecycle()

            HomeScreen(
                words = words,
                wordSearch = vm.searchState,
                onWordSearchUpdate = vm::updateSearch,
                navigateToDetail = { navController.navigate(Detail(it)) },
                navigateToAddWord = { navController.navigate(AddWord) },
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

        composable<AddWord> {
            val wordDao: WordDao = koinInject()
            val coroutineScope = rememberCoroutineScope()

            AddWordDialog(
                onAddWord = { word ->
                    addWord(
                        coroutineScope = coroutineScope,
                        word = word,
                        wordDao = wordDao,
                        navController = navController,
                    )

                },
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}

private fun addWord(
    coroutineScope: CoroutineScope,
    word: String,
    wordDao: WordDao,
    navController: NavHostController
) {
    val wordEntity = WordEntity(
        id = UUID.randomUUID().toString(),
        word = word,
    )
    coroutineScope.launch {
        withContext(Dispatchers.IO) {
            wordDao.insert(wordEntity)
        }
        navController.popBackStack()
    }
}