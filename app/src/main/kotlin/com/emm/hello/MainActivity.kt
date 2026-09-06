package com.emm.hello

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.navigation.LaunchDestination
import com.emm.hello.newfeatures.NewRoot

class MainActivity : ComponentActivity() {

    private val launchDestination: MutableState<LaunchDestination?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (savedInstanceState == null) {
            launchDestination.value = intent.launchDestination()
        }
        setContent {
            HelloTheme {
                NewRoot(
                    launchDestination = launchDestination.value,
                    onLaunchDestinationConsumed = { launchDestination.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchDestination.value = intent.launchDestination()
    }

    private fun Intent.launchDestination(): LaunchDestination? =
        LaunchDestination.fromExtraValue(getStringExtra(LaunchDestination.EXTRA_NAME))
}
