package com.example.wallpaperchanger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.wallpaperchanger.data.FolderRepo
import com.example.wallpaperchanger.data.SchedulerRepo
import com.example.wallpaperchanger.data.SettingsStore
import com.example.wallpaperchanger.data.WallpaperRepo
import com.example.wallpaperchanger.notification.WallpaperNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WallpaperActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CHANGE_WALLPAPER = "com.example.wallpaperchanger.CHANGE_WALLPAPER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CHANGE_WALLPAPER) {
            CoroutineScope(Dispatchers.IO).launch {
                changeWallpaper(context)
            }
        }
    }

    private suspend fun changeWallpaper(context: Context) {
        val settingsStore = SettingsStore(context)
        val folderRepo = FolderRepo(context)
        val wallpaperRepo = WallpaperRepo(context)

        val settings = settingsStore.settingsFlow.first()
        if (settings.folderUri.isEmpty()) return

        val folderUri = Uri.parse(settings.folderUri)
        val result = wallpaperRepo.setWallpaperFromFolder(
            folderRepo = folderRepo,
            folderUri = folderUri,
            sequenceMode = settings.sequenceMode,
            currentIndex = settings.currentIndex,
            wallpaperMode = settings.wallpaperMode
        )
        if (result.isSuccess) {
            if (settings.sequenceMode == SettingsStore.SEQUENCE_MODE_SEQUENTIAL) {
                settingsStore.setCurrentIndex(result.getOrNull()?.second ?: 0)
            }
            WallpaperNotification.showChangedNotification(context)
        }
    }
}

/**
 * 开机启动接收器，恢复定时任务。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val settingsStore = SettingsStore(context)
                val schedulerRepo = SchedulerRepo(context)
                val settings = settingsStore.settingsFlow.first()
                if (settings.intervalEnabled) {
                    schedulerRepo.scheduleInterval(settings.intervalHours)
                }
                if (settings.scheduledEnabled) {
                    schedulerRepo.scheduleAtTime(settings.scheduledHour, settings.scheduledMinute)
                }
            }
        }
    }
}
