package com.emm.hello

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.emm.data.WordDao
import com.emm.data.WordEntity
import com.emm.hello.page.DetailScreen
import com.emm.hello.page.HomeScreen
import com.emm.hello.page.HomeViewModel
import com.emm.hello.page.InitScreen
import com.emm.hello.route.Detail
import com.emm.hello.route.Home
import com.emm.hello.route.Init
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

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

            val words: List<WordEntity> by vm.wordListFlow.collectAsStateWithLifecycle()

            HomeScreen(
                words = words,
                onSave = vm::insert,
                navigateToDetail = { navController.navigate(Detail(it)) },
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
    }
}