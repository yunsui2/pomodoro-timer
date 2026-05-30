# Android 一键换壁纸 App — 设计文档

> 日期：2026-05-31 | 状态：已确认

## 1. 概述

一款 Android 原生 App，让用户从手机相册选择一个文件夹作为壁纸来源，支持一键随机换壁纸、桌面小组件快捷更换、通知栏按钮更换，以及定时自动换壁纸。

## 2. 需求回顾

| 需求 | 决策 |
|------|------|
| 平台 | Android |
| 壁纸来源 | 从相册选一个文件夹，从中随机抽取照片 |
| 换壁纸方式 | App 内按钮 + 桌面小组件 + 通知栏按钮 + 定时自动 |
| 定时模式 | 固定间隔（如每3小时）+ 指定时间（如每天8点），两种可同时开启 |
| 文件夹选择 | 自动扫描图片文件夹列表 + 系统文件夹选择器备选 |

## 3. UI 设计决策

| 页面/组件 | 选择 | 说明 |
|-----------|------|------|
| 主界面 | 简洁居中布局 | 大按钮居中，下方显示来源文件夹和定时状态 |
| 文件夹选择 | 扫描列表 + 手动浏览 | 自动列出所有含图片的文件夹，顶部可手动浏览 |
| 定时设置 | 模式选择器 + 动态内容 | 顶部切换间隔/定时模式，两种各有独立开关 |
| 桌面小组件 | 1×1 极简按钮 | 小方块，只有一个换壁纸图标 |
| 通知栏 | 单按钮 | 下拉通知栏显示，一键换壁纸 |

## 4. 技术架构

### 4.1 技术栈

| 层面 | 选型 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose（声明式 UI） |
| 壁纸设置 | Android `WallpaperManager` API |
| 定时任务 | WorkManager（间隔模式）+ AlarmManager（指定时间） |
| 桌面小组件 | Glance（Compose for AppWidgets） |
| 文件夹访问 | MediaStore（扫描图片文件夹）+ SAF（系统文件选择器） |
| 数据持久化 | DataStore (Preferences) |

### 4.2 分层架构

```
UI 层 (Jetpack Compose)
├── MainScreen        — 主界面（一键换壁纸 + 状态信息）
├── FolderPicker      — 文件夹选择页面
├── TimerSettings     — 定时设置页面
├── AppWidget         — 1×1 桌面小组件
└── Notification      — 通知栏快捷按钮
        │
        ▼
ViewModel 层
└── MainViewModel     — 状态管理、换壁纸逻辑、UI 事件处理
        │
        ▼
数据/系统服务层
├── WallpaperRepo     — 调用 WallpaperManager 设置系统壁纸
├── FolderRepo        — 通过 MediaStore 扫描含图片的文件夹
├── SchedulerRepo     — 管理 WorkManager 定时任务
└── SettingsStore     — DataStore 持久化存储所有设置
```

### 4.3 核心数据流

| 触发方式 | 处理流程 |
|----------|----------|
| App 内按钮点击 | 从选中文件夹随机取图 → `WallpaperManager.setBitmap()` → 更新 UI 状态 |
| 桌面小组件点击 | AppWidget onClick → BroadcastReceiver → 后台换壁纸（无需打开 App） |
| 通知栏按钮点击 | Notification Action → BroadcastReceiver → 后台换壁纸 |
| 定时触发 | WorkManager/AlarmManager 到时间 → Worker 取随机图片 → 设置壁纸 → 可选通知 |

## 5. 功能模块

### 必须实现

1. **选择壁纸来源文件夹** — 扫描展示 + 手动浏览两种方式
2. **一键随机换壁纸** — App 主界面大按钮
3. **桌面小组件 1×1** — 点击即换，无需打开 App
4. **通知栏快捷按钮** — 下拉通知栏一键换壁纸
5. **间隔模式定时** — 基于 WorkManager，可设置间隔时长
6. **指定时间定时** — 基于 AlarmManager，可设置每天固定时间

### 可选增强（后续迭代）

- 换壁纸过渡动画（淡入淡出）
- 壁纸历史记录
- 切换随机/顺序模式
- 选择仅设置主屏幕 / 仅锁屏 / 两者

## 6. 数据存储设计

使用 DataStore (Preferences) 存储以下键值：

| Key | 类型 | 说明 |
|-----|------|------|
| `folder_uri` | String | 选中文件夹的 URI |
| `folder_name` | String | 文件夹名称（用于显示） |
| `image_count` | Int | 文件夹内图片数量 |
| `interval_enabled` | Boolean | 间隔模式是否开启 |
| `interval_hours` | Int | 间隔小时数 |
| `scheduled_enabled` | Boolean | 定时模式是否开启 |
| `scheduled_time` | String | 定时时间（如 "08:00"） |

## 7. 项目结构

```
WallpaperChanger/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/wallpaperchanger/
│   │   │   ├── MainActivity.kt          — 入口 Activity
│   │   │   ├── ui/
│   │   │   │   ├── MainScreen.kt        — 主界面 Composable
│   │   │   │   ├── FolderPicker.kt      — 文件夹选择
│   │   │   │   └── TimerSettings.kt     — 定时设置
│   │   │   ├── viewmodel/
│   │   │   │   └── MainViewModel.kt     — 状态管理
│   │   │   ├── data/
│   │   │   │   ├── WallpaperRepo.kt     — 壁纸操作
│   │   │   │   ├── FolderRepo.kt        — 文件夹扫描
│   │   │   │   ├── SchedulerRepo.kt     — 定时任务
│   │   │   │   └── SettingsStore.kt     — 持久化存储
│   │   │   ├── widget/
│   │   │   │   └── WallpaperWidget.kt   — 桌面小组件
│   │   │   └── notification/
│   │   │       └── WallpaperNotification.kt — 通知栏
│   │   ├── res/                         — 资源文件
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

## 8. 权限需求

| 权限 | 用途 | 备注 |
|------|------|------|
| `SET_WALLPAPER` | 设置系统壁纸 | 无需运行时授权 |
| `READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` | 读取相册图片 | Android 13+ 用细粒度权限 |
| `RECEIVE_BOOT_COMPLETED` | 重启后恢复定时任务 | 可选 |
| `POST_NOTIFICATIONS` | 通知栏按钮 | Android 13+ 需要 |
| `SCHEDULE_EXACT_ALARM` | 指定时间精确触发 | 定时模式需要 |
