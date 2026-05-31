# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 当前项目

**Android 一键换壁纸 App**（`WallpaperChanger/`），另有一个历史项目番茄钟（`pomodoro.html` / `main.js`）已在 working tree 中删除。

## 构建命令

构建 APK 需要在 PowerShell 中设置环境变量后运行 Gradle：

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
$env:ANDROID_HOME = "C:\Android\Sdk"
$env:ANDROID_SDK_ROOT = "C:\Android\Sdk"
$gradleHome = "$env:TEMP\gradle-8.5\gradle-8.5"
$classpath = (Get-ChildItem "$gradleHome\lib" -Filter "*.jar" | ForEach-Object { $_.FullName }) -join ";"
Set-Location "D:\aimakeapp\WallpaperChanger"
java -cp $classpath org.gradle.launcher.GradleMain assembleDebug --no-daemon
```

## Architecture

Android 原生 App，Kotlin + Jetpack Compose，MVVM 架构。

```
WallpaperChanger/app/src/main/java/com/example/wallpaperchanger/
├── MainActivity.kt              # 入口 Activity，页面路由，权限请求
├── WallpaperApplication.kt      # Application，通知渠道初始化
├── data/
│   ├── SettingsStore.kt         # DataStore 持久化 (11 个配置项)
│   ├── FolderRepo.kt            # MediaStore 扫描 + SAF DocumentFile 遍历
│   ├── WallpaperRepo.kt         # WallpaperManager 设置壁纸 (滚动/静态)
│   └── SchedulerRepo.kt         # WorkManager 间隔 + AlarmManager 定时
├── receiver/
│   └── WallpaperActionReceiver.kt  # 小组件/通知栏/定时触发的广播接收器
├── notification/
│   └── WallpaperNotification.kt    # 通知栏常驻快捷按钮
├── viewmodel/
│   └── MainViewModel.kt         # MVVM 状态管理中枢
├── ui/
│   ├── theme/Theme.kt           # Material3 主题 (紫蓝色)
│   ├── MainScreen.kt            # 主界面 + 设置弹窗
│   ├── FolderPicker.kt          # 文件夹选择 (自动扫描 + 手动浏览)
│   └── TimerSettings.kt         # 定时设置 (间隔/指定时间双 Tab)
└── widget/
    └── WallpaperWidget.kt       # 1×1 桌面小组件
```

### 核心数据流

| 触发方式 | 路径 |
|----------|------|
| App 内按钮 | MainScreen → MainViewModel → WallpaperRepo.setWallpaperFromFolder() |
| 桌面小组件 | AppWidget onClick → WallpaperActionReceiver → 同上 |
| 通知栏按钮 | Notification Action → WallpaperActionReceiver → 同上 |
| 定时触发 | WorkManager/AlarmManager → WallpaperChangeWorker → 同上 |

### 网络注意事项

当前开发环境在中国大陆，以下服务被墙，已做镜像处理：
- **Google Maven** (`dl.google.com`) → 使用阿里云镜像 `maven.aliyun.com/repository/google`
- **Gradle 分发** (`services.gradle.org`) → 使用腾讯云镜像 `mirrors.cloud.tencent.com/gradle`
- **Android SDK** → 通过 `redirector.gvt1.com` CDN 手动下载组件

Gradle 配置文件 `settings.gradle.kts` 中已配置镜像仓库。
