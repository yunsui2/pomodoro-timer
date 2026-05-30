package com.example.wallpaperchanger.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.wallpaperchanger.MainActivity
import com.example.wallpaperchanger.WallpaperApplication
import com.example.wallpaperchanger.data.SettingsStore
import com.example.wallpaperchanger.receiver.WallpaperActionReceiver
import kotlinx.coroutines.flow.first

class WallpaperNotification(private val context: Context) {

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val NOTIFICATION_CHANGED_ID = 2002

        /**
         * 显示"壁纸已更换"的短暂通知。
         */
        fun showChangedNotification(context: Context) {
            val notification = NotificationCompat.Builder(
                context, WallpaperApplication.NOTIFICATION_CHANNEL_ID
            )
                .setSmallIcon(android.R.drawable.ic_menu_gallery)
                .setContentTitle("壁纸已更换")
                .setContentText("已随机更换壁纸 ✨")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_CHANGED_ID, notification)
        }
    }

    /**
     * 显示持续存在的通知栏快捷按钮。
     */
    fun showQuickAction(context: Context) {
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val changeIntent = Intent(context, WallpaperActionReceiver::class.java).apply {
            action = WallpaperActionReceiver.ACTION_CHANGE_WALLPAPER
        }
        val changePendingIntent = PendingIntent.getBroadcast(
            context, 1, changeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtitle = try {
            kotlinx.coroutines.runBlocking {
                val settings = SettingsStore(context).settingsFlow.first()
                if (settings.folderUri.isEmpty()) "未设置壁纸来源" else "${settings.folderName} · ${settings.imageCount}张"
            }
        } catch (e: Exception) {
            "点击打开应用"
        }

        val notification = NotificationCompat.Builder(
            context, WallpaperApplication.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("壁纸助手")
            .setContentText(subtitle)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_rotate, "换壁纸", changePendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 隐藏通知栏快捷按钮。
     */
    fun hideQuickAction(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}
