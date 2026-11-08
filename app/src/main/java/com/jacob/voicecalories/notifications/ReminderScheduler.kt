package com.jacob.voicecalories.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jacob.voicecalories.data.AppSettings
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val WORK_NAME = "daily_summary_work"

    fun schedule(context: Context, settings: AppSettings) {
        val workManager = WorkManager.getInstance(context)

        if (!settings.notificationsEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val now = ZonedDateTime.now()
        var next = now
            .withHour(settings.summaryHour)
            .withMinute(settings.summaryMinute)
            .withSecond(0)
            .withNano(0)

        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }

        val delayMillis = (next.toInstant().toEpochMilli() - System.currentTimeMillis())
            .coerceAtLeast(0)

        val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
