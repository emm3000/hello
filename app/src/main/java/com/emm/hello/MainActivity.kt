package com.emm.hello

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import com.emm.hello.page.HomeScreen
import com.emm.hello.page.HomeViewModel
import com.emm.hello.ui.theme.HelloTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelloTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val vm: HomeViewModel = koinViewModel()
                    HomeScreen(
                        onClick = {},
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}