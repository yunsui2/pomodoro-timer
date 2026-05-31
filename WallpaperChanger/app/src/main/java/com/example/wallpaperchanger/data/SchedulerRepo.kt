package com.example.wallpaperchanger.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.*
import com.example.wallpaperchanger.notification.WallpaperNotification
import com.example.wallpaperchanger.receiver.WallpaperActionReceiver
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SchedulerRepo(private val context: Context) {

    companion object {
        private const val INTERVAL_WORK_NAME = "wallpaper_interval_change"
        private const val REQ_SCHEDULED = 1001
        private const val REQ_INTERVAL_ALARM = 2001
    }

    /** 间隔定时 — 根据 useAlarmManager 选择底层实现 */
    fun scheduleInterval(hours: Int, useAlarmManager: Boolean) {
        if (useAlarmManager) {
            scheduleIntervalAlarm(hours)
        } else {
            scheduleIntervalWorkManager(hours)
        }
    }

    fun cancelInterval(useAlarmManager: Boolean) {
        if (useAlarmManager) {
            cancelIntervalAlarm()
        } else {
            cancelIntervalWorkManager()
        }
    }

    // ---- WorkManager 模式（标准，可能被杀） ----

    private fun scheduleIntervalWorkManager(hours: Int) {
        val workRequest = PeriodicWorkRequestBuilder<WallpaperChangeWorker>(
            hours.toLong(), TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            INTERVAL_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun cancelIntervalWorkManager() {
        WorkManager.getInstance(context).cancelUniqueWork(INTERVAL_WORK_NAME)
    }

    // ---- AlarmManager 模式（持久，划掉 App 也不丢） ----

    fun scheduleIntervalAlarm(hours: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, WallpaperActionReceiver::class.java).apply {
            action = WallpaperActionReceiver.ACTION_INTERVAL_CHANGE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, REQ_INTERVAL_ALARM, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, hours)
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    fun cancelIntervalAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WallpaperActionReceiver::class.java).apply {
            action = WallpaperActionReceiver.ACTION_INTERVAL_CHANGE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQ_INTERVAL_ALARM, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // ---- 指定时间定时（每日） ----

    fun scheduleAtTime(hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, WallpaperActionReceiver::class.java).apply {
            action = WallpaperActionReceiver.ACTION_CHANGE_WALLPAPER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, REQ_SCHEDULED, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    fun cancelScheduledTime() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WallpaperActionReceiver::class.java).apply {
            action = WallpaperActionReceiver.ACTION_CHANGE_WALLPAPER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQ_SCHEDULED, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun getNextScheduledTime(hour: Int, minute: Int): String {
        return String.format("%02d:%02d", hour, minute)
    }

    fun getNextIntervalTime(hours: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, hours)
        return String.format("%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE))
    }
}

class WallpaperChangeWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val settingsStore = SettingsStore(applicationContext)
        val folderRepo = FolderRepo(applicationContext)
        val wallpaperRepo = WallpaperRepo(applicationContext)

        return try {
            kotlinx.coroutines.runBlocking {
                val settings = settingsStore.settingsFlow.first()
                if (settings.folderUri.isEmpty()) {
                    return@runBlocking Result.failure()
                }
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
                    WallpaperNotification.showChangedNotification(applicationContext)
                    Result.success()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
