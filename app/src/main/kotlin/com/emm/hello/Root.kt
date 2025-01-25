package com.emm.hello

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emm.data.WordEntity
import com.emm.hello.page.HomeScreen
import com.emm.hello.page.HomeViewModel
import com.emm.hello.page.InitScreen
import com.emm.hello.route.Home
import com.emm.hello.route.Init
import org.koin.androidx.compose.koinViewModel

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
                modifier = Modifier,
            )
        }
    }
}