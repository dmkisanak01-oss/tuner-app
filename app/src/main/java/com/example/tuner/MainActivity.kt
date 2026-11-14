package com.example.tuner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.example.tuner.ui.AppNav
import com.example.tuner.ui.LocalContainer
import com.example.tuner.ui.theme.TunerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as TunerApp).container
        setContent {
            CompositionLocalProvider(LocalContainer provides container) {
                TunerTheme { AppNav() }
            }
        }
    }
}
