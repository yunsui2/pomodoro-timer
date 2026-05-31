package com.example.wallpaperchanger

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            Toast.makeText(this, "存储权限已授予，请重新进入文件夹选择", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "需要存储权限才能自动扫描文件夹", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        setContent {
            WallpaperChangerTheme {
                var currentScreen by remember { mutableStateOf("main") }
                // 用于触发 FolderPicker 刷新
                var folderRefreshKey by remember { mutableIntStateOf(0) }

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
                        onNavigateToFolderPicker = {
                            folderRefreshKey++
                            currentScreen = "folder"
                        },
                        onNavigateToTimerSettings = { currentScreen = "timer" }
                    )
                    "folder" -> FolderPicker(
                        viewModel = viewModel,
                        refreshKey = folderRefreshKey,
                        onRequestStoragePermission = { requestStoragePermission() },
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

    private fun requestStoragePermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // 过滤出还未授权的权限
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (notGranted.isNotEmpty()) {
            storagePermissionLauncher.launch(notGranted)
        }
    }
}
