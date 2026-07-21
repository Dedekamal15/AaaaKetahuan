package com.example.aaaaketahuan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.example.aaaaketahuan.ui.auth.AuthScreen
import com.example.aaaaketahuan.ui.navigation.AppNavGraph
import com.example.aaaaketahuan.ui.onboarding.OnboardingScreen
import com.example.aaaaketahuan.ui.theme.AaaaKetahuanTheme
import com.example.aaaaketahuan.viewmodel.TransaksiViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: TransaksiViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — user has chosen */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS permission on Android 13+ so daily reminders work
        requestNotificationPermission()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val userEmail by viewModel.userEmail.collectAsState()
            val userDisplayName by viewModel.userDisplayName.collectAsState()
            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            AaaaKetahuanTheme(darkTheme = isDarkTheme) {
                when {
                    userEmail == null -> AuthScreen(viewModel = viewModel)
                    userDisplayName == null -> OnboardingScreen(viewModel = viewModel)
                    else -> AppNavGraph(viewModel = viewModel)
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
