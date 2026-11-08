package com.jacob.voicecalories.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jacob.voicecalories.MainActivity
import com.jacob.voicecalories.R
import com.jacob.voicecalories.data.AppSettings
import com.jacob.voicecalories.data.FoodEntry
import com.jacob.voicecalories.data.totals

object SummaryNotifier {

    const val CHANNEL_ID = "daily_summary"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily calorie summary",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "A once-a-day summary of calories, macros, and suggestions."
        }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, entries: List<FoodEntry>, settings: AppSettings): Boolean {
        if (!settings.notificationsEnabled) return false
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val totals = entries.totals()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_food)
            .setContentTitle(SummaryText.title(totals, entries.size))
            .setContentText(SummaryText.shortText(totals, settings, entries.size))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(SummaryText.body(totals, settings, entries.size))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .addAction(
                R.drawable.ic_stat_food,
                "Log another meal",
                listenIntent(context)
            )
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun listenIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("voicecalories://listen")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
