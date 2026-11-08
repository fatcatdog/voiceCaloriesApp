package com.jacob.voicecalories

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.jacob.voicecalories.ui.MainScreen
import com.jacob.voicecalories.ui.MainViewModel
import com.jacob.voicecalories.ui.theme.VoiceCaloriesTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var assistantText by mutableStateOf<String?>(null)
    private var autoListen by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            handleIntent(intent)
        }

        setContent {
            VoiceCaloriesTheme {
                MainScreen(
                    viewModel = viewModel,
                    assistantText = assistantText,
                    autoListen = autoListen,
                    onAssistantTextConsumed = { assistantText = null },
                    onAutoListenConsumed = { autoListen = false }
                )
            }
        }

        window.decorView.post { requestNotificationPermissionIfNeeded() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val data = intent.data
        val meal = intent.getStringExtra("meal")
            ?: data
                ?.takeIf { it.scheme == "voicecalories" && it.host == "log" }
                ?.getQueryParameter("meal")

        if (!meal.isNullOrBlank()) {
            assistantText = meal
        }

        if (data?.scheme == "voicecalories" && data.host == "listen") {
            autoListen = true
        }
        if (intent.getBooleanExtra("autoListen", false)) {
            autoListen = true
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
