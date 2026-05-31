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

    val settings: StateFlow<SettingsStore.WallpaperSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsStore.WallpaperSettings())

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun changeWallpaper() {
        val s = settings.value
        if (s.folderUri.isEmpty()) {
            viewModelScope.launch { _toastMessage.emit("请先选择壁纸来源文件夹") }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val folderUri = Uri.parse(s.folderUri)
            val result = wallpaperRepo.setWallpaperFromFolder(
                folderRepo = folderRepo,
                folderUri = folderUri,
                sequenceMode = s.sequenceMode,
                currentIndex = s.currentIndex,
                wallpaperMode = s.wallpaperMode
            )
            result.onSuccess { (_, newIndex) ->
                if (s.sequenceMode == SettingsStore.SEQUENCE_MODE_SEQUENTIAL) {
                    settingsStore.setCurrentIndex(newIndex)
                }
                val modeLabel = if (s.wallpaperMode == SettingsStore.WALLPAPER_MODE_STATIC) "静态" else "滚动"
                _toastMessage.emit("壁纸已更换 ✨ ($modeLabel)")
            }.onFailure { e ->
                _toastMessage.emit("更换失败: ${e.message}")
            }
            _isLoading.value = false
        }
    }

    fun setFolderUri(uri: Uri, name: String, count: Int) {
        viewModelScope.launch {
            settingsStore.setFolder(uri.toString(), name, count)
            _toastMessage.emit("已选择: $name ($count 张)")
        }
    }

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

    fun setWallpaperMode(mode: String) {
        viewModelScope.launch {
            settingsStore.setWallpaperMode(mode)
            val label = if (mode == SettingsStore.WALLPAPER_MODE_STATIC) "静态模式" else "滚动模式"
            _toastMessage.emit("壁纸呈现: $label")
        }
    }

    fun setSequenceMode(mode: String) {
        viewModelScope.launch {
            settingsStore.setSequenceMode(mode)
            if (mode == SettingsStore.SEQUENCE_MODE_SEQUENTIAL) {
                settingsStore.setCurrentIndex(0)
            }
            val label = if (mode == SettingsStore.SEQUENCE_MODE_SEQUENTIAL) "顺序更换" else "随机更换"
            _toastMessage.emit("换图顺序: $label")
        }
    }

    fun getNextIntervalTime(hours: Int): String {
        return schedulerRepo.getNextIntervalTime(hours)
    }

    fun getNextScheduledTime(hour: Int, minute: Int): String {
        return schedulerRepo.getNextScheduledTime(hour, minute)
    }

    fun refreshNotification() {
        val ctx = getApplication<Application>()
        if (settings.value.folderUri.isNotEmpty()) {
            notification.showQuickAction(ctx)
        }
    }

    fun hideNotification() {
        notification.hideQuickAction(getApplication())
    }
}
