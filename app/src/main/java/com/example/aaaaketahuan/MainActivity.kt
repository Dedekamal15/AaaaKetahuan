package com.example.aaaaketahuan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.aaaaketahuan.ui.navigation.AppNavGraph
import com.example.aaaaketahuan.ui.theme.AaaaKetahuanTheme
import com.example.aaaaketahuan.viewmodel.TransaksiViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: TransaksiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            AaaaKetahuanTheme(darkTheme = isDarkTheme) {
                AppNavGraph(viewModel = viewModel)
            }
        }
    }
}
