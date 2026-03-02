package com.emm.hello

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.emm.data.deck.RemoteDataSource
import com.emm.hello.core.theme.HelloTheme
import com.emm.hello.newfeatures.NewRoot
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val remote by inject<RemoteDataSource>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            remote.export()
        }
        setContent {
            HelloTheme {
                NewRoot()
            }
        }
    }
}
