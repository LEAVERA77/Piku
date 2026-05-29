package com.piku.app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import com.piku.app.ui.navigation.PikuRootNav
import com.piku.app.ui.theme.PikuTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PikuTheme {
                PikuRootNav()
            }
        }
    }
}
