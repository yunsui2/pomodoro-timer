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
        private const val SCHEDULED_REQUEST_CODE = 1001
    }

    fun scheduleInterval(hours: Int) {
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

    fun cancelInterval() {
        WorkManager.getInstance(context).cancelUniqueWork(INTERVAL_WORK_NAME)
    }

    fun scheduleAtTime(hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, WallpaperActionReceiver::class.java).apply {
            action = WallpaperActionReceiver.ACTION_CHANGE_WALLPAPER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, SCHEDULED_REQUEST_CODE, intent,
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

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelScheduledTime() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WallpaperActionReceiver::class.java).apply {
            action = WallpaperActionReceiver.ACTION_CHANGE_WALLPAPER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, SCHEDULED_REQUEST_CODE, intent,
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

/**
 * WorkManager Worker: 在后台自动换壁纸。
 */
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
                val result = wallpaperRepo.setRandomWallpaperFromFolder(folderRepo, folderUri)
                if (result.isSuccess) {
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
