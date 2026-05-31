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
        private val KEY_WALLPAPER_MODE = stringPreferencesKey("wallpaper_mode")
        private val KEY_SEQUENCE_MODE = stringPreferencesKey("sequence_mode")
        private val KEY_CURRENT_INDEX = intPreferencesKey("current_index")

        const val WALLPAPER_MODE_SCROLL = "scroll"
        const val WALLPAPER_MODE_STATIC = "static"
        const val SEQUENCE_MODE_RANDOM = "random"
        const val SEQUENCE_MODE_SEQUENTIAL = "sequential"
    }

    data class WallpaperSettings(
        val folderUri: String = "",
        val folderName: String = "",
        val imageCount: Int = 0,
        val intervalEnabled: Boolean = false,
        val intervalHours: Int = 3,
        val scheduledEnabled: Boolean = false,
        val scheduledHour: Int = 8,
        val scheduledMinute: Int = 0,
        val wallpaperMode: String = WALLPAPER_MODE_SCROLL,
        val sequenceMode: String = SEQUENCE_MODE_RANDOM,
        val currentIndex: Int = 0
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
            scheduledMinute = prefs[KEY_SCHEDULED_MINUTE] ?: 0,
            wallpaperMode = prefs[KEY_WALLPAPER_MODE] ?: WALLPAPER_MODE_SCROLL,
            sequenceMode = prefs[KEY_SEQUENCE_MODE] ?: SEQUENCE_MODE_RANDOM,
            currentIndex = prefs[KEY_CURRENT_INDEX] ?: 0
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

    suspend fun setWallpaperMode(mode: String) {
        context.dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                set(KEY_WALLPAPER_MODE, mode)
            }
        }
    }

    suspend fun setSequenceMode(mode: String) {
        context.dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                set(KEY_SEQUENCE_MODE, mode)
            }
        }
    }

    suspend fun setCurrentIndex(index: Int) {
        context.dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                set(KEY_CURRENT_INDEX, index)
            }
        }
    }
}
