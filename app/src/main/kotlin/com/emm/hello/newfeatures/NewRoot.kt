package com.emm.hello.newfeatures

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun NewRoot() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NewRoutes.Dashboard,
    ) {

        composable<NewRoutes.Dashboard> {

        }
    }

}