package com.example.wallpaperchanger.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallpaperchanger.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToFolderPicker: () -> Unit,
    onNavigateToTimerSettings: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("换壁纸") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // 当前来源信息
            Text(
                text = if (settings.folderName.isNotEmpty())
                    "当前来源: ${settings.folderName} (${settings.imageCount}张)"
                else "请选择壁纸来源",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 中间大圆形按钮
            Button(
                onClick = { viewModel.changeWallpaper() },
                enabled = !isLoading && settings.folderName.isNotEmpty(),
                modifier = Modifier
                    .size(150.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isLoading) "..." else "🔄",
                        fontSize = 36.sp
                    )
                    Text(
                        text = "换一张",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 定时状态
            val statusParts = mutableListOf<String>()
            if (settings.intervalEnabled) {
                statusParts.add("每${settings.intervalHours}小时自动换")
            }
            if (settings.scheduledEnabled) {
                val time = String.format("%02d:%02d", settings.scheduledHour, settings.scheduledMinute)
                statusParts.add("每天${time}换")
            }
            if (statusParts.isEmpty()) {
                statusParts.add("未设置定时")
            }

            Text(
                text = statusParts.joinToString(" · "),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.weight(1f))

            // 底部操作栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onNavigateToFolderPicker) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📁", fontSize = 20.sp)
                        Text("选择文件夹", fontSize = 12.sp)
                    }
                }

                TextButton(onClick = onNavigateToTimerSettings) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⏰", fontSize = 20.sp)
                        Text("定时设置", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
