package com.example.aaaaketahuan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
            AaaaKetahuanTheme {
                AppNavGraph(viewModel = viewModel)
            }
        }
    }
}
