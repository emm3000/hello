package com.emm.hello.newfeatures.deck

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.emm.hello.navigation.Navigator
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
object NewDeckRoute : NavKey

@Composable
fun NewDeckDestination(navigator: Navigator) {
    val vm: NewDeckViewModel = koinViewModel()
    val uiState = vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.effect.collect { effect ->
            when (effect) {
                NewDeckUiEffect.NavigateBack -> navigator.goBack()
                is NewDeckUiEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    NewDeckScreen(
        onNavigateBack = { navigator.goBack() },
        state = uiState.value,
        onIntent = vm::onIntent,
    )
}
