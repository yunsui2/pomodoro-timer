package com.example.wallpaperchanger.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallpaperchanger.data.ImageFolder
import com.example.wallpaperchanger.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPicker(
    viewModel: MainViewModel,
    refreshKey: Int = 0,
    onRequestStoragePermission: () -> Unit,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    // 每次进入页面时重新扫描（由 refreshKey 触发）
    val folders = remember(refreshKey) { viewModel.folderRepo.scanImageFolders() }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // 获取持久化权限，确保定时任务也能访问
            viewModel.folderRepo.takePersistablePermission(it)

            val (name, count) = viewModel.folderRepo.resolveFolderInfo(it)
            viewModel.setFolderUri(it, name, count)
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择壁纸来源") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← 返回") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // 手动浏览按钮（始终可用，不依赖权限）
            OutlinedButton(
                onClick = { folderPickerLauncher.launch(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("📂 手动浏览文件夹...", fontSize = 14.sp)
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "——— 或选择下方文件夹 ———",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )

            if (folders.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "未找到包含图片的文件夹",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "请授予存储权限以自动扫描，\n或使用上方「手动浏览」",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onRequestStoragePermission,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🔐 授予存储权限")
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(folders) { folder ->
                        FolderItem(
                            folder = folder,
                            isSelected = settings.folderUri.contains(
                                folder.uri.getQueryParameter("bucketId") ?: ""
                            ),
                            onClick = {
                                viewModel.setFolderUri(folder.uri, folder.name, folder.count)
                                onBack()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderItem(
    folder: ImageFolder,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🖼️", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${folder.count} 张照片",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        "✓ 当前",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
