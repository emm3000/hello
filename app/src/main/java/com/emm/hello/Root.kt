package com.emm.hello

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emm.hello.page.InitScreen
import com.emm.hello.route.Home
import com.emm.hello.route.Init

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
            Text("Home Screen")
        }
    }
}