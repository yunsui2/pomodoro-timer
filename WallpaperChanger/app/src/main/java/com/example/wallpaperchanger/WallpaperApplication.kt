package com.example.wallpaperchanger

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class WallpaperApplication : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "wallpaper_service"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "壁纸服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示换壁纸快捷按钮"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
