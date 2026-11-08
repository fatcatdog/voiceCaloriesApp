package com.jacob.voicecalories.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jacob.voicecalories.data.AppDatabase
import com.jacob.voicecalories.data.AppSettings
import com.jacob.voicecalories.data.FoodEntry
import com.jacob.voicecalories.data.SettingsStore
import com.jacob.voicecalories.data.Totals
import com.jacob.voicecalories.data.totals
import com.jacob.voicecalories.notifications.ReminderScheduler
import com.jacob.voicecalories.notifications.SummaryNotifier
import com.jacob.voicecalories.nutrition.NutritionEstimator
import com.jacob.voicecalories.nutrition.toFoodEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class LoggedSummary(
    val entries: List<FoodEntry>,
    val source: String,
    val warning: String?
) {
    val totals: Totals
        get() = entries.totals()
}

data class UiState(
    val isLogging: Boolean = false,
    val lastLogged: LoggedSummary? = null,
    val error: String? = null,
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.get(application).foodDao()
    private val settingsStore = SettingsStore(application)

    val settings: StateFlow<AppSettings> = settingsStore.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings()
    )

    private val selectedDate = MutableStateFlow(LocalDate.now(ZoneId.systemDefault()))
    val currentDate: StateFlow<LocalDate> = selectedDate.asStateFlow()

    val entries: StateFlow<List<FoodEntry>> = selectedDate
        .flatMapLatest { date ->
            val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            dao.entriesBetween(start, end)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val totals: StateFlow<Totals> = entries
        .map { it.totals() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            Totals()
        )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val current = settingsStore.current()
            ReminderScheduler.schedule(getApplication(), current)
        }
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun selectPreviousDay() {
        selectedDate.value = selectedDate.value.minusDays(1)
    }

    fun selectNextDay() {
        val today = LocalDate.now(ZoneId.systemDefault())
        if (selectedDate.value.isBefore(today)) {
            selectedDate.value = selectedDate.value.plusDays(1)
        }
    }

    fun selectToday() {
        selectedDate.value = LocalDate.now(ZoneId.systemDefault())
    }

    fun refreshDay() {
        // Keep selected date or ensure refreshed
        selectedDate.value = selectedDate.value
    }

    fun logMeal(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _ui.value.isLogging) return

        viewModelScope.launch {
            _ui.update { it.copy(isLogging = true, error = null) }
            try {
                val currentSettings = settingsStore.current()
                val result = NutritionEstimator.estimate(currentSettings, trimmed)
                val timestamp = System.currentTimeMillis()
                val entities = result.items.map { item ->
                    item.toFoodEntry(
                        rawText = trimmed,
                        timestamp = timestamp,
                        source = result.source
                    )
                }

                val ids = dao.insertAll(entities)
                val saved = entities.mapIndexedNotNull { index, entry ->
                    val id = ids.getOrNull(index) ?: return@mapIndexedNotNull null
                    entry.copy(id = id)
                }

                refreshDay()
                _ui.update {
                    it.copy(
                        isLogging = false,
                        lastLogged = LoggedSummary(
                            entries = saved,
                            source = result.source,
                            warning = result.warning
                        )
                    )
                }
            } catch (error: Exception) {
                _ui.update {
                    it.copy(
                        isLogging = false,
                        error = error.message ?: "Could not log that meal."
                    )
                }
            }
        }
    }

    fun undoLastLog() {
        val logged = _ui.value.lastLogged ?: return
        viewModelScope.launch {
            dao.deleteByIds(logged.entries.map { it.id })
            _ui.update { it.copy(lastLogged = null, message = "Removed ${logged.entries.size} item(s)") }
        }
    }

    fun clearLastLogged() {
        _ui.update { it.copy(lastLogged = null) }
    }

    fun deleteEntry(entry: FoodEntry) {
        viewModelScope.launch {
            dao.delete(entry)
            _ui.update { it.copy(message = "Deleted ${entry.name}") }
        }
    }

    fun updateEntry(entry: FoodEntry) {
        viewModelScope.launch {
            dao.update(entry)
            _ui.update { it.copy(message = "Updated ${entry.name}") }
        }
    }

    fun saveSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            settingsStore.save(newSettings)
            ReminderScheduler.schedule(getApplication(), newSettings)
            _ui.update { it.copy(message = "Settings saved") }
        }
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            val currentSettings = settingsStore.current()
            val start = startOfToday()
            val todayEntries = dao.getEntriesBetween(start, endOfDay(start))
            val posted = SummaryNotifier.show(getApplication(), todayEntries, currentSettings)
            _ui.update {
                it.copy(
                    message = when {
                        posted -> "Test notification sent. If you don't see it, check " +
                            "notification permission in Android settings."
                        !currentSettings.notificationsEnabled ->
                            "Notifications are off. Turn on the daily summary, then tap Save."
                        else -> "Notification permission is missing. Allow notifications, then try again."
                    }
                )
            }
        }
    }

    fun clearMessage() {
        _ui.update { it.copy(message = null) }
    }

    fun clearError() {
        _ui.update { it.copy(error = null) }
    }

    private fun startOfToday(): Long =
        LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun endOfDay(startMillis: Long): Long =
        Instant.ofEpochMilli(startMillis)
            .atZone(ZoneId.systemDefault())
            .plusDays(1)
            .toInstant()
            .toEpochMilli()
}
