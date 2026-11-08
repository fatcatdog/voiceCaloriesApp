package com.jacob.voicecalories.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jacob.voicecalories.data.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initial: AppSettings,
    onBack: () -> Unit,
    onSave: (AppSettings) -> Unit,
    onTestNotification: () -> Unit
) {
    val context = LocalContext.current

    var apiKey by rememberSaveable(initial.apiKey) { mutableStateOf(initial.apiKey) }
    var model by rememberSaveable(initial.model) { mutableStateOf(initial.model) }
    var apiVisible by rememberSaveable { mutableStateOf(false) }
    var calorieGoal by rememberSaveable(initial.calorieGoal) {
        mutableStateOf(initial.calorieGoal.toString())
    }
    var proteinGoal by rememberSaveable(initial.proteinGoal) {
        mutableStateOf(initial.proteinGoal.toString())
    }
    var hour by rememberSaveable(initial.summaryHour) {
        mutableStateOf(initial.summaryHour.toString())
    }
    var minute by rememberSaveable(initial.summaryMinute) {
        mutableStateOf(initial.summaryMinute.toString())
    }
    var notificationsEnabled by rememberSaveable(initial.notificationsEnabled) {
        mutableStateOf(initial.notificationsEnabled)
    }
    var autoListenOnLaunch by rememberSaveable(initial.autoListenOnLaunch) {
        mutableStateOf(initial.autoListenOnLaunch)
    }

    var permissionGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    val hourValue = hour.toIntOrNull()
    val minuteValue = minute.toIntOrNull()
    val calorieValue = calorieGoal.toIntOrNull()
    val proteinValue = proteinGoal.toIntOrNull()

    val valid = hourValue != null && hourValue in 0..23 &&
        minuteValue != null && minuteValue in 0..59 &&
        calorieValue != null && calorieValue in 500..10000 &&
        proteinValue != null && proteinValue in 0..500

    fun save() {
        if (!valid) return
        onSave(
            initial.copy(
                apiKey = apiKey.trim(),
                model = model.trim().ifBlank { AppSettings.DEFAULT_MODEL },
                calorieGoal = calorieValue ?: initial.calorieGoal,
                proteinGoal = proteinValue ?: initial.proteinGoal,
                summaryHour = hourValue ?: initial.summaryHour,
                summaryMinute = minuteValue ?: initial.summaryMinute,
                notificationsEnabled = notificationsEnabled,
                autoListenOnLaunch = autoListenOnLaunch
            )
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { save() }, enabled = valid) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("Nutrition estimates")

            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Gemini API key") },
                        singleLine = true,
                        visualTransformation = if (apiVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { apiVisible = !apiVisible }) {
                                Icon(
                                    imageVector = if (apiVisible) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = if (apiVisible) {
                                        "Hide API key"
                                    } else {
                                        "Show API key"
                                    }
                                )
                            }
                        },
                        supportingText = {
                            Text("Stored only on this phone. It is sent directly to Google's Gemini API.")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Gemini model") },
                        singleLine = true,
                        supportingText = {
                            Text("Default: ${AppSettings.DEFAULT_MODEL}")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = { openGeminiKeyPage(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Get a free Gemini API key")
                    }

                    Text(
                        text = "Without a key, the app uses a small built-in food database. " +
                            "That works offline, but mixed meals such as a turkey sandwich are " +
                            "much more accurate with Gemini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionHeader("Daily goals")

            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = calorieGoal,
                            onValueChange = { input ->
                                calorieGoal = input.filter(Char::isDigit)
                            },
                            label = { Text("Calories") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = proteinGoal,
                            onValueChange = { input ->
                                proteinGoal = input.filter(Char::isDigit)
                            },
                            label = { Text("Protein (g)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (!valid) {
                        Text(
                            text = "Calories must be 500–10,000, protein 0–500, " +
                                "hour 0–23, and minute 0–59.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            SectionHeader("Daily summary")

            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "End-of-day notification",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "A summary of calories, macros, and suggestions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = hour,
                            onValueChange = { input -> hour = input.filter(Char::isDigit) },
                            label = { Text("Hour (0–23)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minute,
                            onValueChange = { input -> minute = input.filter(Char::isDigit) },
                            label = { Text("Minute") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Android may deliver the summary a few minutes late. " +
                            "It is scheduled once per day and survives reboots.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!permissionGranted) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Allow notifications")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (permissionGranted) {
                                onTestNotification()
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send test notification")
                    }
                }
            }

            SectionHeader("Voice logging")

            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-listen on launch",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Immediately listen when opened. Just say \"Hey Google, open Logger\" and speak hands-free.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoListenOnLaunch,
                            onCheckedChange = { autoListenOnLaunch = it }
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Voice command:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "\"Hey Google, open Logger\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "App Actions only work after the app is published on Google Play " +
                            "and the action is approved. Until then, use the launcher shortcut " +
                            "or open the app and tap the mic.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Your food log stays in a local database on this phone. " +
                    "Nothing is synced to a server. Gemini requests contain only the " +
                    "meal description.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

private fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun openGeminiKeyPage(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
        )
    }
}
