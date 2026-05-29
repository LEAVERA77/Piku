package com.piku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.piku.app.ui.navigation.PikuNavGraph
import com.piku.app.ui.theme.PikuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PikuTheme {
                PikuNavGraph()
            }
        }
    }
}
