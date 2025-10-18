package com.emm.hello.newfeatures

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.emm.hello.newfeatures.card.cardDetailRoute
import com.emm.hello.newfeatures.card.newCardRoute
import com.emm.hello.newfeatures.dashboard.DashboardRoute
import com.emm.hello.newfeatures.dashboard.dashboard
import com.emm.hello.newfeatures.dashboard.quote
import com.emm.hello.newfeatures.deck.deckDetailRoute
import com.emm.hello.newfeatures.deck.newDeckRoute
import com.emm.hello.newfeatures.study.study

@Composable
fun NewRoot() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {

        dashboard(navController)
        quote(navController)
        study(navController)
        newCardRoute(navController)
        newDeckRoute(navController)
        deckDetailRoute(navController)
        cardDetailRoute(navController)
    }
}