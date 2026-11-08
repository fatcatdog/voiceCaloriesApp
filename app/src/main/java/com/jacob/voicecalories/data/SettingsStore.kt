package com.jacob.voicecalories.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val apiKey: String = "",
    val model: String = DEFAULT_MODEL,
    val calorieGoal: Int = 2000,
    val proteinGoal: Int = 120,
    val summaryHour: Int = 21,
    val summaryMinute: Int = 0,
    val notificationsEnabled: Boolean = true,
    val autoListenOnLaunch: Boolean = true
) {
    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }
}

class SettingsStore(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("gemini_api_key")
        val MODEL = stringPreferencesKey("gemini_model")
        val CALORIE_GOAL = intPreferencesKey("calorie_goal")
        val PROTEIN_GOAL = intPreferencesKey("protein_goal")
        val SUMMARY_HOUR = intPreferencesKey("summary_hour")
        val SUMMARY_MINUTE = intPreferencesKey("summary_minute")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val AUTO_LISTEN_ON_LAUNCH = booleanPreferencesKey("auto_listen_on_launch")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            AppSettings(
                apiKey = prefs[Keys.API_KEY].orEmpty(),
                model = prefs[Keys.MODEL] ?: AppSettings.DEFAULT_MODEL,
                calorieGoal = prefs[Keys.CALORIE_GOAL] ?: 2000,
                proteinGoal = prefs[Keys.PROTEIN_GOAL] ?: 120,
                summaryHour = prefs[Keys.SUMMARY_HOUR] ?: 21,
                summaryMinute = prefs[Keys.SUMMARY_MINUTE] ?: 0,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                autoListenOnLaunch = prefs[Keys.AUTO_LISTEN_ON_LAUNCH] ?: true
            )
        }

    suspend fun current(): AppSettings = settings.first()

    suspend fun save(value: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.API_KEY] = value.apiKey.trim()
            prefs[Keys.MODEL] = value.model.trim().ifBlank { AppSettings.DEFAULT_MODEL }
            prefs[Keys.CALORIE_GOAL] = value.calorieGoal.coerceIn(500, 10000)
            prefs[Keys.PROTEIN_GOAL] = value.proteinGoal.coerceIn(0, 500)
            prefs[Keys.SUMMARY_HOUR] = value.summaryHour.coerceIn(0, 23)
            prefs[Keys.SUMMARY_MINUTE] = value.summaryMinute.coerceIn(0, 59)
            prefs[Keys.NOTIFICATIONS_ENABLED] = value.notificationsEnabled
            prefs[Keys.AUTO_LISTEN_ON_LAUNCH] = value.autoListenOnLaunch
        }
    }
}
