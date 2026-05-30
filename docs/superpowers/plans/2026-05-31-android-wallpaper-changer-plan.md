# Android 一键换壁纸 App — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一款 Android 原生换壁纸 App，支持从相册文件夹随机选取照片设置为壁纸，提供桌面小组件、通知栏按钮和定时自动更换功能。

**Architecture:** MVVM 三层架构 — Jetpack Compose UI 层 → ViewModel 状态管理层 → Repository 数据/系统服务层。使用 WorkManager 处理定时任务，Glance 实现桌面小组件，DataStore 持久化设置。

**Tech Stack:** Kotlin, Jetpack Compose, WorkManager, Glance (AppWidget), DataStore, MediaStore, SAF, WallpaperManager, AlarmManager

---

## 文件结构总览

```
WallpaperChanger/
├── build.gradle.kts                          # 根构建文件
├── settings.gradle.kts                       # 项目设置
├── gradle.properties                         # Gradle 配置
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties         # Gradle Wrapper
├── app/
│   ├── build.gradle.kts                      # App 模块构建文件
│   └── src/main/
│       ├── AndroidManifest.xml               # 清单文件
│       ├── java/com/example/wallpaperchanger/
│       │   ├── MainActivity.kt               # 入口 Activity
│       │   ├── WallpaperApplication.kt       # Application 类（初始化通知渠道）
│       │   ├── ui/
│       │   │   ├── theme/
│       │   │   │   └── Theme.kt              # Material3 主题
│       │   │   ├── MainScreen.kt             # 主界面
│       │   │   ├── FolderPicker.kt           # 文件夹选择页
│       │   │   └── TimerSettings.kt          # 定时设置页
│       │   ├── viewmodel/
│       │   │   └── MainViewModel.kt          # 状态管理
│       │   ├── data/
│       │   │   ├── SettingsStore.kt          # DataStore 持久化
│       │   │   ├── FolderRepo.kt             # 文件夹扫描
│       │   │   ├── WallpaperRepo.kt          # 壁纸设置
│       │   │   └── SchedulerRepo.kt          # 定时任务
│       │   ├── widget/
│       │   │   └── WallpaperWidget.kt        # 1×1 桌面小组件
│       │   ├── notification/
│       │   │   └── WallpaperNotification.kt  # 通知栏
│       │   └── receiver/
│       │       └── WallpaperActionReceiver.kt # 广播接收器
│       └── res/
│           ├── values/
│           │   ├── strings.xml
│           │   └── themes.xml
│           ├── drawable/
│           │   └── ic_widget_refresh.xml     # 小组件图标
│           └── xml/
│               └── wallpaper_widget_info.xml  # 小组件配置
```

---

### Task 1: 项目脚手架

**Files:**
- Create: `WallpaperChanger/settings.gradle.kts`
- Create: `WallpaperChanger/build.gradle.kts`
- Create: `WallpaperChanger/gradle.properties`
- Create: `WallpaperChanger/gradle/wrapper/gradle-wrapper.properties`
- Create: `WallpaperChanger/app/build.gradle.kts`
- Create: `WallpaperChanger/app/src/main/AndroidManifest.xml`
- Create: `WallpaperChanger/app/src/main/res/values/strings.xml`
- Create: `WallpaperChanger/app/src/main/res/values/themes.xml`

- [ ] **Step 1: 创建项目目录结构**

```powershell
New-Item -ItemType Directory -Force -Path "WallpaperChanger\app\src\main\java\com\example\wallpaperchanger"
New-Item -ItemType Directory -Force -Path "WallpaperChanger\app\src\main\res\values"
New-Item -ItemType Directory -Force -Path "WallpaperChanger\app\src\main\res\drawable"
New-Item -ItemType Directory -Force -Path "WallpaperChanger\app\src\main\res\xml"
New-Item -ItemType Directory -Force -Path "WallpaperChanger\gradle\wrapper"
```

- [ ] **Step 2: 编写 settings.gradle.kts**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WallpaperChanger"
include(":app")
```

- [ ] **Step 3: 编写根 build.gradle.kts**

```kotlin
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
```

- [ ] **Step 4: 编写 gradle.properties**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 5: 编写 gradle-wrapper.properties**

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 6: 编写 app/build.gradle.kts**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.wallpaperchanger"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.wallpaperchanger"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Glance (AppWidget)
    implementation("androidx.glance:glance-appwidget:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

- [ ] **Step 7: 编写 AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.SET_WALLPAPER" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <application
        android:name=".WallpaperApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.WallpaperChanger">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.WallpaperChanger">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".receiver.WallpaperActionReceiver"
            android:exported="false" />

        <receiver
            android:name=".widget.WallpaperWidget"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/wallpaper_widget_info" />
        </receiver>

        <receiver
            android:name=".receiver.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

    </application>
</manifest>
```

- [ ] **Step 8: 编写 strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">换壁纸</string>
    <string name="widget_name">一键换壁纸</string>
    <string name="notification_channel_name">壁纸服务</string>
    <string name="notification_title">壁纸助手</string>
    <string name="change_wallpaper">换壁纸</string>
</resources>
```

- [ ] **Step 9: 编写 themes.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.WallpaperChanger" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 10: Commit**

```bash
git add WallpaperChanger/
git commit -m "feat: 初始化 Android 项目脚手架"
```

---

### Task 2: 数据持久化层 — SettingsStore

**Files:**
- Create: `WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/data/SettingsStore.kt`

- [ ] **Step 1: 编写 SettingsStore**

```kotlin
package com.example.wallpaperchanger.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wallpaper_settings")

class SettingsStore(private val context: Context) {

    companion object {
        private val KEY_FOLDER_URI = stringPreferencesKey("folder_uri")
        private val KEY_FOLDER_NAME = stringPreferencesKey("folder_name")
        private val KEY_IMAGE_COUNT = intPreferencesKey("image_count")
        private val KEY_INTERVAL_ENABLED = booleanPreferencesKey("interval_enabled")
        private val KEY_INTERVAL_HOURS = intPreferencesKey("interval_hours")
        private val KEY_SCHEDULED_ENABLED = booleanPreferencesKey("scheduled_enabled")
        private val KEY_SCHEDULED_HOUR = intPreferencesKey("scheduled_hour")
        private val KEY_SCHEDULED_MINUTE = intPreferencesKey("scheduled_minute")
    }

    data class WallpaperSettings(
        val folderUri: String = "",
        val folderName: String = "",
        val imageCount: Int = 0,
        val intervalEnabled: Boolean = false,
        val intervalHours: Int = 3,
        val scheduledEnabled: Boolean = false,
        val scheduledHour: Int = 8,
        val scheduledMinute: Int = 0
    )

    val settingsFlow: Flow<WallpaperSettings> = context.dataStore.data.map { prefs ->
        WallpaperSettings(
            folderUri = prefs[KEY_FOLDER_URI] ?: "",
            folderName = prefs[KEY_FOLDER_NAME] ?: "",
            imageCount = prefs[KEY_IMAGE_COUNT] ?: 0,
            intervalEnabled = prefs[KEY_INTERVAL_ENABLED] ?: false,
            intervalHours = prefs[KEY_INTERVAL_HOURS] ?: 3,
            scheduledEnabled = prefs[KEY_SCHEDULED_ENABLED] ?: false,
            scheduledHour = prefs[KEY_SCHEDULED_HOUR] ?: 8,
            scheduledMinute = prefs[KEY_SCHEDULED_MINUTE] ?: 0
        )
    }

    suspend fun setFolder(uri: String, name: String, count: Int) {
        context.dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                set(KEY_FOLDER_URI, uri)
                set(KEY_FOLDER_NAME, name)
                set(KEY_IMAGE_COUNT, count)
            }
        }
    }

    suspend fun setIntervalEnabled(enabled: Boolean) {
        context.dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                set(KEY_INTERVAL_ENABLED, enabled)
            }
        }
    }

    suspend fun setIntervalHours(hours: Int) {
        context.dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                set(KEY_INTERVAL_HOURS, hours)
            }
        }
    }

    suspend fun setScheduledEnabled(enabled: Boolean) {
        context.dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                set(KEY_SCHEDULED_ENABLED, enabled)
            }
        }
    }

    suspend fun setScheduledTime(hour: Int, minute: Int) {
        context.dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                set(KEY_SCHEDULED_HOUR, hour)
                set(KEY_SCHEDULED_MINUTE, minute)
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/data/SettingsStore.kt
git commit -m "feat: 添加 SettingsStore 数据持久化层"
```

---

### Task 3: 文件夹扫描 — FolderRepo

**Files:**
- Create: `WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/data/FolderRepo.kt`

- [ ] **Step 1: 编写 FolderRepo**

```kotlin
package com.example.wallpaperchanger.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns

data class ImageFolder(
    val name: String,
    val path: String,
    val uri: Uri,
    val count: Int
)

class FolderRepo(private val context: Context) {

    /**
     * 通过 MediaStore 扫描所有包含图片的文件夹。
     * 返回按图片数量降序排列的文件夹列表。
     */
    fun scanImageFolders(): List<ImageFolder> {
        val folders = mutableMapOf<String, MutableList<String>>()

        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.BUCKET_ID
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor: Cursor? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null, null, sortOrder
            )
        } else {
            @Suppress("DEPRECATION")
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null, null, sortOrder
            )
        }

        cursor?.use {
            val bucketNameIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dataIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val bucketIdIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)

            while (it.moveToNext()) {
                val bucketName = it.getString(bucketNameIndex) ?: "Unknown"
                val data = it.getString(dataIndex)
                val bucketId = it.getString(bucketIdIndex)
                val key = "$bucketName||$bucketId"
                folders.getOrPut(key) { mutableListOf() }.add(data)
            }
        }

        return folders.map { (key, images) ->
            val (name, bucketId) = key.split("||")
            val firstImage = images.first()
            val contentUri = Uri.parse("${MediaStore.Images.Media.EXTERNAL_CONTENT_URI}?bucketId=$bucketId")
            ImageFolder(
                name = name,
                path = firstImage.substringBeforeLast("/"),
                uri = contentUri,
                count = images.size
            )
        }.sortedByDescending { it.count }
    }

    /**
     * 获取指定文件夹 Uri 下的所有图片 Uri 列表。
     * 用于随机选图时构建池。
     */
    fun getImagesInFolder(folderUri: Uri): List<Uri> {
        val images = mutableListOf<Uri>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA
        )

        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val bucketId = folderUri.getQueryParameter("bucketId")
        val selectionArgs = arrayOf(bucketId ?: return emptyList())

        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                images.add(uri)
            }
        }

        return images
    }

    /**
     * 使用 SAF (Storage Access Framework) 让用户手动浏览，
     * 返回用户选择的文件夹 URI 和显示名称。
     */
    fun resolveFolderInfo(uri: Uri): Pair<String, Int> {
        var name = "已选文件夹"
        var count = 0

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex >= 0) {
                    name = cursor.getString(displayNameIndex) ?: name
                }
            }
            count = cursor.count
        }

        return Pair(name, count)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/data/FolderRepo.kt
git commit -m "feat: 添加 FolderRepo 文件夹扫描功能"
```

---

### Task 4: 壁纸设置 — WallpaperRepo

**Files:**
- Create: `WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/data/WallpaperRepo.kt`

- [ ] **Step 1: 编写 WallpaperRepo**

```kotlin
package com.example.wallpaperchanger.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WallpaperRepo(private val context: Context) {

    private val wallpaperManager = WallpaperManager.getInstance(context)

    /**
     * 将指定 Uri 的图片设置为系统壁纸（仅主屏幕）。
     * 在 IO 线程执行，避免阻塞 UI。
     * 返回是否设置成功。
     */
    suspend fun setWallpaper(imageUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("无法打开图片文件"))

            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                return@withContext Result.failure(Exception("无法解码图片"))
            }

            // 仅设置主屏幕壁纸（Android 7.0+）
            wallpaperManager.setBitmap(
                bitmap,
                null,  // 不设置锁屏壁纸
                false  // 不跨屏幕
            )

            bitmap.recycle()
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(Exception("没有权限设置壁纸: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("设置壁纸失败: ${e.message}"))
        }
    }

    /**
     * 从文件夹随机选取一张图片并设为壁纸。
     * 返回文件数量为 0 时失败。
     */
    suspend fun setRandomWallpaperFromFolder(folderRepo: FolderRepo, folderUri: Uri): Result<Uri> {
        val images = folderRepo.getImagesInFolder(folderUri)
        if (images.isEmpty()) {
            return Result.failure(Exception("文件夹中没有图片"))
        }

        val randomImage = images.random()
        return setWallpaper(randomImage).map { randomImage }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/data/WallpaperRepo.kt
git commit -m "feat: 添加 WallpaperRepo 壁纸设置功能"
```

---

### Task 5: 定时任务 — SchedulerRepo

**Files:**
- Create: `WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/data/SchedulerRepo.kt`

- [ ] **Step 1: 编写 SchedulerRepo**

```kotlin
package com.example.wallpaperchanger.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.example.wallpaperchanger.receiver.WallpaperActionReceiver
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SchedulerRepo(private val context: Context) {

    companion object {
        private const val INTERVAL_WORK_NAME = "wallpaper_interval_change"
        private const val SCHEDULED_REQUEST_CODE = 1001
    }

    /**
     * 设置固定间隔定时换壁纸（基于 WorkManager）。
     * WorkManager 保证至少间隔指定时间，但不保证精确时刻。
     */
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

    /**
     * 取消固定间隔定时。
     */
    fun cancelInterval() {
        WorkManager.getInstance(context).cancelUniqueWork(INTERVAL_WORK_NAME)
    }

    /**
     * 设置指定时间定时换壁纸（基于 AlarmManager）。
     * 精确到分钟。
     */
    fun scheduleAtTime(hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, WallpaperActionReceiver::class.java).apply {
            action = WallpaperActionReceiver.ACTION_CHANGE_WALLPAPER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SCHEDULED_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 计算今天的目标时间
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // 如果今天的时间已过，设置为明天
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

    /**
     * 取消指定时间定时。
     */
    fun cancelScheduledTime() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WallpaperActionReceiver::class.java).apply {
            action = WallpaperActionReceiver.ACTION_CHANGE_WALLPAPER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SCHEDULED_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * 获取下次定时触发的时间（估算）。
     */
    fun getNextScheduledTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return String.format("%02d:%02d", hour, minute)
    }
}

/**
 * WorkManager Worker：在后台执行换壁纸操作。
 */
class WallpaperChangeWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val settingsStore = SettingsStore(applicationContext)
        val folderRepo = FolderRepo(applicationContext)
        val wallpaperRepo = WallpaperRepo(applicationContext)

        // 需要同步获取当前设置... 这里用 runBlocking 简化
        // 实际生产环境建议使用 DataStore 同步读取或缓存
        return try {
            kotlinx.coroutines.runBlocking {
                var folderUri: android.net.Uri? = null
                settingsStore.settingsFlow.collect { settings ->
                    if (settings.folderUri.isNotEmpty()) {
                        folderUri = android.net.Uri.parse(settings.folderUri)
                    }
                    throw kotlinx.coroutines.CancellationException("stop") // 只取第一个值
                }
            }
            Result.failure()
        } catch (_: kotlinx.coroutines.CancellationException) {
            // 上面故意抛的异常，不能用
            Result.failure()
        }

        // 简化实现：在新的协程中完成换壁纸
        val settingsStore = SettingsStore(applicationContext)
        val folderRepo = FolderRepo(applicationContext)
        val wallpaperRepo = WallpaperRepo(applicationContext)

        return try {
            kotlinx.coroutines.runBlocking {
                val settings = kotlinx.coroutines.flow.first(settingsStore.settingsFlow)
                if (settings.folderUri.isEmpty()) {
                    return@runBlocking Result.failure()
                }
                val folderUri = android.net.Uri.parse(settings.folderUri)
                val result = wallpaperRepo.setRandomWallpaperFromFolder(folderRepo, folderUri)
                if (result.isSuccess) {
                    WallpaperNotification(applicationContext).showChangedNotification()
                    Result.success()
                } else {
                    Result.failure()
                }
            }
        }
    }
}
```

等一下，上面的 Worker 实现有严重问题。让我重新写一个正确的版本：

- [ ] **Step 1 (修正): 编写 SchedulerRepo**

```kotlin
package com.example.wallpaperchanger.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.*
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
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
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
```

- [ ] **Step 2: Commit**

```bash
git add WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/data/SchedulerRepo.kt
git commit -m "feat: 添加 SchedulerRepo 定时任务功能"
```

---

### Task 6: 广播接收器 & Application 类

**Files:**
- Create: `WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/receiver/WallpaperActionReceiver.kt`
- Create: `WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/WallpaperApplication.kt`

- [ ] **Step 1: 编写 WallpaperActionReceiver**

```kotlin
package com.example.wallpaperchanger.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.wallpaperchanger.data.FolderRepo
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
        if (intent.action == ACTION_CHANGE_WALLPAPER ||
            intent.action == Intent.ACTION_BOOT_COMPLETED
        ) {
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
        val result = wallpaperRepo.setRandomWallpaperFromFolder(folderRepo, folderUri)
        if (result.isSuccess) {
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
                val schedulerRepo = com.example.wallpaperchanger.data.SchedulerRepo(context)
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
```

- [ ] **Step 2: 编写 WallpaperApplication**

```kotlin
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
```

- [ ] **Step 3: Commit**

```bash
git add WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/receiver/WallpaperActionReceiver.kt
git add WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/WallpaperApplication.kt
git commit -m "feat: 添加广播接收器和 Application 初始化"
```

---

### Task 7: 通知栏 — WallpaperNotification

**Files:**
- Create: `WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/notification/WallpaperNotification.kt`

- [ ] **Step 1: 编写 WallpaperNotification**

```kotlin
package com.example.wallpaperchanger.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.wallpaperchanger.MainActivity
import com.example.wallpaperchanger.R
import com.example.wallpaperchanger.WallpaperApplication
import com.example.wallpaperchanger.receiver.WallpaperActionReceiver
import kotlinx.coroutines.flow.first
import com.example.wallpaperchanger.data.SettingsStore

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
     * 点击通知主体打开 App，按钮触发换壁纸。
     */
    fun showQuickAction(context: Context) {
        // 打开 App 的 Intent
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 换壁纸的 Intent
        val changeIntent = Intent(context, WallpaperActionReceiver::class.java).apply {
            action = WallpaperActionReceiver.ACTION_CHANGE_WALLPAPER
        }
        val changePendingIntent = PendingIntent.getBroadcast(
            context, 1, changeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context, WallpaperApplication.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("壁纸助手")
            .setContentText(getSubtitleText(context))
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

    private fun getSubtitleText(context: Context): String {
        return try {
            kotlinx.coroutines.runBlocking {
                val settings = SettingsStore(context).settingsFlow.first()
                if (settings.folderUri.isEmpty()) "未设置壁纸来源" else "${settings.folderName} · ${settings.imageCount}张"
            }
        } catch (e: Exception) {
            "点击打开应用"
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/notification/WallpaperNotification.kt
git commit -m "feat: 添加通知栏快捷按钮功能"
```

---

### Task 8: ViewModel — MainViewModel

**Files:**
- Create: `WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/viewmodel/MainViewModel.kt`

- [ ] **Step 1: 编写 MainViewModel**

```kotlin
package com.example.wallpaperchanger.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wallpaperchanger.data.*
import com.example.wallpaperchanger.notification.WallpaperNotification
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = SettingsStore(application)
    val folderRepo = FolderRepo(application)
    private val wallpaperRepo = WallpaperRepo(application)
    private val schedulerRepo = SchedulerRepo(application)
    private val notification = WallpaperNotification(application)

    // -- 从 DataStore 读取的设置状态 --
    val settings: StateFlow<SettingsStore.WallpaperSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsStore.WallpaperSettings())

    // -- UI 事件 --
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 立即换一张壁纸。
     */
    fun changeWallpaper() {
        val folderUriStr = settings.value.folderUri
        if (folderUriStr.isEmpty()) {
            viewModelScope.launch { _toastMessage.emit("请先选择壁纸来源文件夹") }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val folderUri = Uri.parse(folderUriStr)
            val result = wallpaperRepo.setRandomWallpaperFromFolder(folderRepo, folderUri)
            result.onSuccess {
                _toastMessage.emit("壁纸已更换 ✨")
            }.onFailure { e ->
                _toastMessage.emit("更换失败: ${e.message}")
            }
            _isLoading.value = false
        }
    }

    /**
     * 设置壁纸来源文件夹。
     */
    fun setFolderUri(uri: Uri, name: String, count: Int) {
        viewModelScope.launch {
            settingsStore.setFolder(uri.toString(), name, count)
            _toastMessage.emit("已选择: $name ($count 张)")
        }
    }

    /**
     * 更新间隔定时设置。
     */
    fun updateIntervalSettings(enabled: Boolean, hours: Int) {
        viewModelScope.launch {
            settingsStore.setIntervalEnabled(enabled)
            settingsStore.setIntervalHours(hours)

            if (enabled) {
                schedulerRepo.scheduleInterval(hours)
            } else {
                schedulerRepo.cancelInterval()
            }
        }
    }

    /**
     * 更新指定时间定时设置。
     */
    fun updateScheduledSettings(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsStore.setScheduledEnabled(enabled)
            settingsStore.setScheduledTime(hour, minute)

            if (enabled) {
                schedulerRepo.scheduleAtTime(hour, minute)
            } else {
                schedulerRepo.cancelScheduledTime()
            }
        }
    }

    /**
     * 获取下次间隔触发的预估时间。
     */
    fun getNextIntervalTime(hours: Int): String {
        return schedulerRepo.getNextIntervalTime(hours)
    }

    /**
     * 获取下次定时触发的预估时间。
     */
    fun getNextScheduledTime(hour: Int, minute: Int): String {
        return schedulerRepo.getNextScheduledTime(hour, minute)
    }

    /**
     * 显示/刷新通知栏快捷按钮。
     */
    fun refreshNotification() {
        val ctx = getApplication<android.app.Application>()
        if (settings.value.folderUri.isNotEmpty()) {
            notification.showQuickAction(ctx)
        }
    }

    /**
     * 隐藏通知栏按钮。
     */
    fun hideNotification() {
        notification.hideQuickAction(getApplication())
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/viewmodel/MainViewModel.kt
git commit -m "feat: 添加 MainViewModel 状态管理"
```

---

### Task 9: UI 主题 — Theme.kt

**Files:**
- Create: `WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/ui/theme/Theme.kt`

- [ ] **Step 1: 编写 Theme.kt**

```kotlin
package com.example.wallpaperchanger.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6366F1),       // 紫蓝色主色调
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF8B5CF6),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF334155)
)

@Composable
fun WallpaperChangerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/ui/theme/Theme.kt
git commit -m "feat: 添加 Material3 主题"
```

---

### Task 10: 主界面 — MainScreen

**Files:**
- Create: `WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/ui/MainScreen.kt`

- [ ] **Step 1: 编写 MainScreen**

```kotlin
package com.example.wallpaperchanger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

            // 中间大按钮
            Button(
                onClick = { viewModel.changeWallpaper() },
                enabled = !isLoading && settings.folderName.isNotEmpty(),
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape),
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
```

- [ ] **Step 2: Commit**

```bash
git add WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/ui/MainScreen.kt
git commit -m "feat: 添加主界面 MainScreen"
```

---

### Task 11: 文件夹选择页 — FolderPicker

**Files:**
- Create: `WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/ui/FolderPicker.kt`

- [ ] **Step 1: 编写 FolderPicker**

```kotlin
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallpaperchanger.data.ImageFolder
import com.example.wallpaperchanger.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPicker(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val folders = remember { viewModel.folderRepo.scanImageFolders() }

    // SAF 文件夹选择器
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val (name, count) = viewModel.folderRepo.resolveFolderInfo(it)
            viewModel.setFolderUri(it, name, count)
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
            // 手动浏览按钮
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (folders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "未找到包含图片的文件夹",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(folders) { folder ->
                        FolderItem(
                            folder = folder,
                            isSelected = settings.folderUri.contains(folder.uri.toString().takeLast(20)),
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
```

- [ ] **Step 2: Commit**

```bash
git add WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/ui/FolderPicker.kt
git commit -m "feat: 添加文件夹选择页面 FolderPicker"
```

---

### Task 12: 定时设置页 — TimerSettings

**Files:**
- Create: `WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/ui/TimerSettings.kt`

- [ ] **Step 1: 编写 TimerSettings**

```kotlin
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

    var selectedTab by remember { mutableIntStateOf(0) } // 0=间隔, 1=定时

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
            // 模式切换 Tab
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

                    // 小时选择器
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

                    // 时间选择器（简化版：小时和分钟分开选）
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
```

- [ ] **Step 2: Commit**

```bash
git add WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/ui/TimerSettings.kt
git commit -m "feat: 添加定时设置页面 TimerSettings"
```

---

### Task 13: 桌面小组件 — WallpaperWidget

**Files:**
- Create: `WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/widget/WallpaperWidget.kt`
- Create: `WallpaperChanger/app/src/main/res/xml/wallpaper_widget_info.xml`
- Create: `WallpaperChanger/app/src/main/res/drawable/ic_widget_refresh.xml`

- [ ] **Step 1: 编写 widget info XML**

```xml
<!-- wallpaper_widget_info.xml -->
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="40dp"
    android:minHeight="40dp"
    android:updatePeriodMillis="0"
    android:initialLayout="@layout/widget_layout"
    android:resizeMode="none"
    android:widgetCategory="home_screen"
    android:description="@string/widget_name" />
```

- [ ] **Step 2: 编写 widget 布局 XML**

创建 `WallpaperChanger/app/src/main/res/layout/widget_layout.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/widget_background"
    android:padding="4dp">

    <ImageView
        android:id="@+id/widget_icon"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:src="@drawable/ic_widget_refresh"
        android:scaleType="centerInside"
        android:contentDescription="换壁纸" />

</FrameLayout>
```

创建 `WallpaperChanger/app/src/main/res/drawable/widget_background.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <corners android:radius="16dp" />
    <gradient
        android:startColor="#6366F1"
        android:endColor="#8B5CF6"
        android:angle="135" />
</shape>
```

- [ ] **Step 3: 编写 WallpaperWidget.kt**

```kotlin
package com.example.wallpaperchanger.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.wallpaperchanger.R
import com.example.wallpaperchanger.receiver.WallpaperActionReceiver

class WallpaperWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        /**
         * 更新指定小组件的视图。
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // 整个小组件点击 → 触发换壁纸
            val intent = Intent(context, WallpaperActionReceiver::class.java).apply {
                action = WallpaperActionReceiver.ACTION_CHANGE_WALLPAPER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
```

- [ ] **Step 4: 编写小组件图标 ic_widget_refresh.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="#FFFFFF">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -7.99,8s3.57,8 7.99,8c3.73,0 6.84,-2.55 7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z" />
</vector>
```

- [ ] **Step 5: Commit**

```bash
git add WallpaperChanger/app/src/main/res/xml/wallpaper_widget_info.xml
git add WallpaperChanger/app/src/main/res/layout/widget_layout.xml
git add WallpaperChanger/app/src/main/res/drawable/widget_background.xml
git add WallpaperChanger/app/src/main/res/drawable/ic_widget_refresh.xml
git add WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/widget/WallpaperWidget.kt
git commit -m "feat: 添加 1×1 桌面小组件"
```

---

### Task 14: MainActivity 串联全部功能

**Files:**
- Create: `WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/MainActivity.kt`

- [ ] **Step 1: 编写 MainActivity**

```kotlin
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

    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainViewModel = MainViewModel(application)

        // 请求通知权限 (Android 13+)
        requestNotificationPermission()

        // 恢复通知栏按钮
        mainViewModel.refreshNotification()

        setContent {
            WallpaperChangerTheme {
                var currentScreen by remember { mutableStateOf("main") }

                val viewModel: MainViewModel = viewModel(
                    factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(application)
                )

                // Toast 消息监听
                LaunchedEffect(Unit) {
                    viewModel.toastMessage.collectLatest { message ->
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }

                when (currentScreen) {
                    "main" -> MainScreen(
                        viewModel = viewModel,
                        onNavigateToFolderPicker = { currentScreen = "folder" },
                        onNavigateToTimerSettings = { currentScreen = "timer" }
                    )
                    "folder" -> FolderPicker(
                        viewModel = viewModel,
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
}
```

- [ ] **Step 2: Commit**

```bash
git add WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/MainActivity.kt
git commit -m "feat: 实现 MainActivity 串联全部功能"
```

---

### Task 15: 构建和安装验证

- [ ] **Step 1: 在 Android Studio 中打开项目**

打开 `WallpaperChanger/` 目录作为 Android Studio 项目，等待 Gradle 同步完成。

- [ ] **Step 2: 构建 APK**

在 Android Studio 中：`Build → Build Bundle(s) / APK(s) → Build APK(s)`

或命令行（需要 Android SDK）：
```bash
cd WallpaperChanger
./gradlew assembleDebug
```

- [ ] **Step 3: 安装到手机**

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 4: 验证功能清单**

| 验证项 | 预期行为 |
|--------|----------|
| 打开 App | 显示简洁居中主界面 |
| 点击底部「选择文件夹」 | 进入文件夹列表，显示扫描到的文件夹 |
| 选中一个文件夹 | 返回主界面，显示来源信息 |
| 点击中间大按钮 | 壁纸更换，显示 Toast 提示 |
| 进入「定时设置」→ 间隔模式 | 可开关，可选间隔小时，保存后生效 |
| 进入「定时设置」→ 定时模式 | 可开关，可选时间，保存后生效 |
| 通知栏下拉 | 看到「壁纸助手」通知，点击「换壁纸」按钮 |
| 长按桌面 → 添加小组件 | 找到「一键换壁纸」1×1 小组件 |
| 点击桌面小组件 | 壁纸立即更换 |

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore: 验证构建通过，功能测试完毕"
```

---

## 依赖关系

```
Task 1 (脚手架)
 └→ Task 2 (SettingsStore)
     └→ Task 3 (FolderRepo)
         └→ Task 4 (WallpaperRepo)
             └→ Task 5 (SchedulerRepo)
                 └→ Task 6 (Receiver + Application)
                     └→ Task 7 (Notification)
                         └→ Task 8 (ViewModel)
                             ├→ Task 9 (Theme)
                             ├→ Task 10 (MainScreen)
                             ├→ Task 11 (FolderPicker)
                             ├→ Task 12 (TimerSettings)
                             ├→ Task 13 (Widget)
                             └→ Task 14 (MainActivity)
                                 └→ Task 15 (构建验证)
```

Task 9-13 之间无依赖关系，可以并行开发。
