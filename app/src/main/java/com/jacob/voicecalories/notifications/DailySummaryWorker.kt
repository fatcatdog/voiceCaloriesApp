package com.jacob.voicecalories.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jacob.voicecalories.data.AppDatabase
import com.jacob.voicecalories.data.SettingsStore
import java.time.LocalDate
import java.time.ZoneId

class DailySummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsStore(applicationContext).current()
        if (!settings.notificationsEnabled) return Result.success()

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val entries = AppDatabase.get(applicationContext)
            .foodDao()
            .getEntriesBetween(start, end)

        SummaryNotifier.show(applicationContext, entries, settings)
        return Result.success()
    }
}
