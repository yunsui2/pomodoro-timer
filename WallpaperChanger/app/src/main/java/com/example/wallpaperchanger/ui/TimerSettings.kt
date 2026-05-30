package com.example.wallpaperchanger.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallpaperchanger.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSettings(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    var intervalEnabled by remember(settings.intervalEnabled) {
        mutableStateOf(settings.intervalEnabled)
    }
    var intervalHours by remember(settings.intervalHours) {
        mutableIntStateOf(settings.intervalHours)
    }
    var scheduledEnabled by remember(settings.scheduledEnabled) {
        mutableStateOf(settings.scheduledEnabled)
    }
    var scheduledHour by remember(settings.scheduledHour) {
        mutableIntStateOf(settings.scheduledHour)
    }
    var scheduledMinute by remember(settings.scheduledMinute) {
        mutableIntStateOf(settings.scheduledMinute)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("定时设置") },
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
                .verticalScroll(rememberScrollState())
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(vertical = 12.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("⏱️ 间隔模式") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🕐 定时模式") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> IntervalSettingsCard(
                    enabled = intervalEnabled,
                    hours = intervalHours,
                    nextTime = viewModel.getNextIntervalTime(intervalHours),
                    onEnabledChange = { intervalEnabled = it },
                    onHoursChange = { intervalHours = it },
                    onSave = {
                        viewModel.updateIntervalSettings(intervalEnabled, intervalHours)
                    }
                )
                1 -> ScheduledSettingsCard(
                    enabled = scheduledEnabled,
                    hour = scheduledHour,
                    minute = scheduledMinute,
                    nextTime = viewModel.getNextScheduledTime(scheduledHour, scheduledMinute),
                    onEnabledChange = { scheduledEnabled = it },
                    onHourChange = { scheduledHour = it },
                    onMinuteChange = { scheduledMinute = it },
                    onSave = {
                        viewModel.updateScheduledSettings(scheduledEnabled, scheduledHour, scheduledMinute)
                    }
                )
            }
        }
    }
}

@Composable
private fun IntervalSettingsCard(
    enabled: Boolean,
    hours: Int,
    nextTime: String,
    onEnabledChange: (Boolean) -> Unit,
    onHoursChange: (Int) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("间隔换壁纸", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("每", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))

                    val hourOptions = listOf(1, 2, 3, 4, 6, 8, 12, 24)
                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text("$hours 小时")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            hourOptions.forEach { h ->
                                DropdownMenuItem(
                                    text = { Text("$h 小时") },
                                    onClick = {
                                        onHoursChange(h)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text("自动换一次", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "下次更换: $nextTime",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存设置")
                }
            }
        }
    }
}

@Composable
private fun ScheduledSettingsCard(
    enabled: Boolean,
    hour: Int,
    minute: Int,
    nextTime: String,
    onEnabledChange: (Boolean) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("定时换壁纸", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("每天", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(12.dp))

                    var hourExpanded by remember { mutableStateOf(false) }
                    var minuteExpanded by remember { mutableStateOf(false) }

                    Box {
                        OutlinedButton(onClick = { hourExpanded = true }) {
                            Text(String.format("%02d", hour))
                        }
                        DropdownMenu(
                            expanded = hourExpanded,
                            onDismissRequest = { hourExpanded = false }
                        ) {
                            (0..23).forEach { h ->
                                DropdownMenuItem(
                                    text = { Text(String.format("%02d", h)) },
                                    onClick = {
                                        onHourChange(h)
                                        hourExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text(":", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))

                    Box {
                        OutlinedButton(onClick = { minuteExpanded = true }) {
                            Text(String.format("%02d", minute))
                        }
                        DropdownMenu(
                            expanded = minuteExpanded,
                            onDismissRequest = { minuteExpanded = false }
                        ) {
                            (0..59 step 5).forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(String.format("%02d", m)) },
                                    onClick = {
                                        onMinuteChange(m)
                                        minuteExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text("换一次", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "下次触发: $nextTime",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存设置")
                }
            }
        }
    }
}
