package com.example.wallpaperchanger

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wallpaperchanger.ui.FolderPicker
import com.example.wallpaperchanger.ui.MainScreen
import com.example.wallpaperchanger.ui.TimerSettings
import com.example.wallpaperchanger.ui.theme.WallpaperChangerTheme
import com.example.wallpaperchanger.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        setContent {
            WallpaperChangerTheme {
                var currentScreen by remember { mutableStateOf("main") }

                val viewModel: MainViewModel = viewModel(
                    factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(application)
                )

                LaunchedEffect(Unit) {
                    viewModel.toastMessage.collectLatest { message ->
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }

                // 初始化通知栏
                LaunchedEffect(Unit) {
                    viewModel.refreshNotification()
                }

                when (currentScreen) {
                    "main" -> MainScreen(
                        viewModel = viewModel,
                        onNavigateToFolderPicker = { currentScreen = "folder" },
                        onNavigateToTimerSettings = { currentScreen = "timer" }
                    )
                    "folder" -> FolderPicker(
                        viewModel = viewModel,
                        onBack = { currentScreen = "main" }
                    )
                    "timer" -> TimerSettings(
                        viewModel = viewModel,
                        onBack = { currentScreen = "main" }
                    )
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
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }
}
