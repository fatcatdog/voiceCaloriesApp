package com.jacob.voicecalories

import android.app.Application
import com.jacob.voicecalories.notifications.SummaryNotifier

class VoiceCaloriesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SummaryNotifier.createChannel(this)
    }
}
