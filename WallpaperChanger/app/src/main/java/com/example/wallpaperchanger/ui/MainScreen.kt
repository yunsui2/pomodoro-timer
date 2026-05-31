package com.example.wallpaperchanger.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallpaperchanger.data.SettingsStore
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
    var showSettings by remember { mutableStateOf(false) }

    // 设置弹窗
    if (showSettings) {
        SettingsDialog(
            wallpaperMode = settings.wallpaperMode,
            sequenceMode = settings.sequenceMode,
            onWallpaperModeChange = { viewModel.setWallpaperMode(it) },
            onSequenceModeChange = { viewModel.setSequenceMode(it) },
            onDismiss = { showSettings = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("换壁纸") },
                actions = {
                    TextButton(onClick = { showSettings = true }) {
                        Text("⚙️", fontSize = 20.sp)
                    }
                },
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
                modifier = Modifier.size(150.dp),
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

            // 状态行
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
            // 当前模式摘要
            val modeLabel = if (settings.wallpaperMode == SettingsStore.WALLPAPER_MODE_STATIC) "静态" else "滚动"
            val seqLabel = if (settings.sequenceMode == SettingsStore.SEQUENCE_MODE_SEQUENTIAL) "顺序" else "随机"
            statusParts.add("${modeLabel}·${seqLabel}")

            Text(
                text = statusParts.joinToString(" | "),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(
    wallpaperMode: String,
    sequenceMode: String,
    onWallpaperModeChange: (String) -> Unit,
    onSequenceModeChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚙️ 壁纸设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {

                // 壁纸呈现方式
                Column {
                    Text(
                        "壁纸呈现方式",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = wallpaperMode == SettingsStore.WALLPAPER_MODE_SCROLL,
                            onClick = { onWallpaperModeChange(SettingsStore.WALLPAPER_MODE_SCROLL) },
                            label = { Text("滚动") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = wallpaperMode == SettingsStore.WALLPAPER_MODE_STATIC,
                            onClick = { onWallpaperModeChange(SettingsStore.WALLPAPER_MODE_STATIC) },
                            label = { Text("静态") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = if (wallpaperMode == SettingsStore.WALLPAPER_MODE_STATIC)
                            "壁纸完全呈现，不随桌面翻页"
                        else
                            "壁纸随桌面翻页横向滚动",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }

                // 换图顺序
                Column {
                    Text(
                        "换图顺序",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = sequenceMode == SettingsStore.SEQUENCE_MODE_RANDOM,
                            onClick = { onSequenceModeChange(SettingsStore.SEQUENCE_MODE_RANDOM) },
                            label = { Text("随机") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = sequenceMode == SettingsStore.SEQUENCE_MODE_SEQUENTIAL,
                            onClick = { onSequenceModeChange(SettingsStore.SEQUENCE_MODE_SEQUENTIAL) },
                            label = { Text("顺序") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = if (sequenceMode == SettingsStore.SEQUENCE_MODE_SEQUENTIAL)
                            "按列表顺序一张一张换"
                        else
                            "每次从文件夹中随机抽取",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}
