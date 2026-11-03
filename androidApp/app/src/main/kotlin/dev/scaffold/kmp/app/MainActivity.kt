package dev.scaffold.kmp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.scaffold.kmp.android.feature.reference.ReferenceNavHost
import dev.scaffold.kmp.designsystem.ScaffoldTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScaffoldTheme {
                ReferenceNavHost()
            }
        }
    }
}
