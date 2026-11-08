package com.jacob.voicecalories.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.stringResource
import com.jacob.voicecalories.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import com.jacob.voicecalories.ui.theme.MacroProteinColor
import com.jacob.voicecalories.ui.theme.MacroCarbsColor
import com.jacob.voicecalories.ui.theme.MacroFatColor
import com.jacob.voicecalories.ui.theme.CalorieAccentColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jacob.voicecalories.data.AppSettings
import com.jacob.voicecalories.data.FoodEntry
import com.jacob.voicecalories.data.Totals
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    assistantText: String?,
    autoListen: Boolean,
    onAssistantTextConsumed: () -> Unit,
    onAutoListenConsumed: () -> Unit
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showMacroMetrics by rememberSaveable { mutableStateOf(false) }
    var showTextDialog by rememberSaveable { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<FoodEntry?>(null) }
    var deletingEntry by remember { mutableStateOf<FoodEntry?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.logMeal(spoken)
            }
        }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ready")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "No speech recognizer was found on this device. You can type the meal instead."
                )
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshDay()
    }

    LaunchedEffect(assistantText) {
        val text = assistantText
        if (!text.isNullOrBlank()) {
            viewModel.logMeal(text)
            onAssistantTextConsumed()
        }
    }

    var hasAutoListenedOnLaunch by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(settings.autoListenOnLaunch, assistantText) {
        if (!hasAutoListenedOnLaunch && settings.autoListenOnLaunch && assistantText.isNullOrBlank() && !autoListen) {
            hasAutoListenedOnLaunch = true
            startListening()
        }
    }

    LaunchedEffect(autoListen) {
        if (autoListen) {
            onAutoListenConsumed()
            startListening()
        }
    }

    LaunchedEffect(ui.lastLogged) {
        val logged = ui.lastLogged ?: return@LaunchedEffect
        val itemCount = logged.entries.size
        val result = snackbarHostState.showSnackbar(
            message = "Logged $itemCount item${if (itemCount == 1) "" else "s"} • " +
                "${formatNumber(logged.totals.calories)} kcal",
            actionLabel = "Undo",
            withDismissAction = true,
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoLastLog()
        } else {
            viewModel.clearLastLogged()
        }
        logged.warning?.let { warning ->
            snackbarHostState.showSnackbar(
                message = warning,
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
        }
    }

    LaunchedEffect(ui.error) {
        val error = ui.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = error, withDismissAction = true)
        viewModel.clearError()
    }

    LaunchedEffect(ui.message) {
        val message = ui.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = message)
        viewModel.clearMessage()
    }

    if (showSettings) {
        SettingsScreen(
            initial = settings,
            onBack = { showSettings = false },
            onSave = { updated ->
                viewModel.saveSettings(updated)
                showSettings = false
            },
            onTestNotification = viewModel::sendTestNotification
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = todayLabel(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMacroMetrics = true }) {
                            Icon(Icons.Filled.Insights, contentDescription = "Macro metrics")
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                )
            },
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { showTextDialog = true }) {
                        Text("Type instead")
                    }
                    ExtendedFloatingActionButton(
                        onClick = { startListening() },
                        icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                        text = { Text("Log food") }
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 128.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DailySummaryCard(
                        totals = totals,
                        settings = settings,
                        onOpenMacroMetrics = { showMacroMetrics = true }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's food",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (entries.isNotEmpty()) {
                            Text(
                                text = "${entries.size} item${if (entries.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (entries.isEmpty()) {
                    item {
                        EmptyState(
                            onSpeak = { startListening() },
                            onType = { showTextDialog = true }
                        )
                    }
                } else {
                    items(entries, key = { it.id }) { entry ->
                        FoodRow(
                            entry = entry,
                            onEdit = { editingEntry = entry },
                            onDelete = { deletingEntry = entry }
                        )
                    }
                    item {
                        Text(
                            text = "Tap an item to edit. Nutrition values are estimates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showTextDialog) {
        TextEntryDialog(
            onDismiss = { showTextDialog = false },
            onSubmit = { text ->
                showTextDialog = false
                viewModel.logMeal(text)
            }
        )
    }

    if (showMacroMetrics) {
        MacroMetricsSheet(
            totals = totals,
            settings = settings,
            entryCount = entries.size,
            onDismiss = { showMacroMetrics = false }
        )
    }

    editingEntry?.let { entry ->
        EditEntryDialog(
            entry = entry,
            onDismiss = { editingEntry = null },
            onSave = { updated ->
                viewModel.updateEntry(updated)
                editingEntry = null
            }
        )
    }

    deletingEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { deletingEntry = null },
            title = { Text("Delete ${entry.name}?") },
            text = { Text("This removes it from today's food log.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEntry(entry)
                        deletingEntry = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEntry = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (ui.isLogging) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Estimating nutrition") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Checking calories and macros…")
                }
            }
        )
    }
}

@Composable
private fun DailySummaryCard(
    totals: Totals,
    settings: AppSettings,
    onOpenMacroMetrics: () -> Unit
) {
    val goal = settings.calorieGoal.coerceAtLeast(1)
    val progress = (totals.calories.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val overGoal = totals.calories > goal
    val remaining = goal - totals.calories

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "${formatNumber(totals.calories)} kcal",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = CalorieAccentColor
                    )
                    Text(
                        text = "of ${formatNumber(goal)} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (overGoal) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    } else {
                        CalorieAccentColor.copy(alpha = 0.15f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (overGoal) MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                        else CalorieAccentColor.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = if (remaining >= 0) {
                            "${formatNumber(remaining)} left"
                        } else {
                            "${formatNumber(-remaining)} over"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (overGoal) {
                            MaterialTheme.colorScheme.error
                        } else {
                            CalorieAccentColor
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = if (overGoal) {
                    MaterialTheme.colorScheme.error
                } else {
                    CalorieAccentColor
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    MacroValue(
                        label = "Protein",
                        grams = totals.proteinGrams,
                        goal = settings.proteinGoal,
                        accentColor = MacroProteinColor
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    MacroValue(
                        label = "Carbs",
                        grams = totals.carbsGrams,
                        accentColor = MacroCarbsColor
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    MacroValue(
                        label = "Fat",
                        grams = totals.fatGrams,
                        accentColor = MacroFatColor
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                modifier = Modifier.padding(top = 2.dp)
            )

            FilledTonalButton(
                onClick = onOpenMacroMetrics,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Insights,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = CalorieAccentColor
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "View Today's Macro Metrics",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MacroValue(
    label: String,
    grams: Double,
    goal: Int? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (goal != null && goal > 0) {
                    "${formatGrams(grams)} / ${goal}g"
                } else {
                    "${formatGrams(grams)}g"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FoodRow(entry: FoodEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = CalorieAccentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, CalorieAccentColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${formatNumber(entry.calories)} kcal",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CalorieAccentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = listOfNotNull(
                            entry.quantity.takeIf { it.isNotBlank() },
                            "${formatGrams(entry.proteinGrams)}g P",
                            "${formatGrams(entry.carbsGrams)}g C",
                            "${formatGrams(entry.fatGrams)}g F"
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete ${entry.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onSpeak: () -> Unit, onType: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = CalorieAccentColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, CalorieAccentColor.copy(alpha = 0.3f)),
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = CalorieAccentColor
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Nothing logged yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tell me what you ate and I'll estimate the calories and macros.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSpeak,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Speak a meal")
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onType) {
                Text("Type instead", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TextEntryDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ready") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("an apple, a diet coke, and a turkey sandwich") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Log it")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditEntryDialog(
    entry: FoodEntry,
    onDismiss: () -> Unit,
    onSave: (FoodEntry) -> Unit
) {
    var name by rememberSaveable(entry.id) { mutableStateOf(entry.name) }
    var quantity by rememberSaveable(entry.id) { mutableStateOf(entry.quantity) }
    var calories by rememberSaveable(entry.id) { mutableStateOf(entry.calories.toString()) }
    var protein by rememberSaveable(entry.id) { mutableStateOf(entry.proteinGrams.toString()) }
    var carbs by rememberSaveable(entry.id) { mutableStateOf(entry.carbsGrams.toString()) }
    var fat by rememberSaveable(entry.id) { mutableStateOf(entry.fatGrams.toString()) }
    var fiber by rememberSaveable(entry.id) { mutableStateOf(entry.fiberGrams.toString()) }
    var sugar by rememberSaveable(entry.id) { mutableStateOf(entry.sugarGrams.toString()) }
    var sodium by rememberSaveable(entry.id) { mutableStateOf(entry.sodiumMg.toString()) }

    val canSave = name.isNotBlank() && calories.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit food") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Portion") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { input -> calories = input.filter(Char::isDigit) },
                    label = { Text("Calories") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        label = "Protein",
                        value = protein,
                        onValueChange = { protein = it },
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        label = "Carbs",
                        value = carbs,
                        onValueChange = { carbs = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        label = "Fat",
                        value = fat,
                        onValueChange = { fat = it },
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        label = "Fiber",
                        value = fiber,
                        onValueChange = { fiber = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        label = "Sugar",
                        value = sugar,
                        onValueChange = { sugar = it },
                        modifier = Modifier.weight(1f)
                    )
                    NumberField(
                        label = "Sodium",
                        value = sodium,
                        onValueChange = { sodium = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider()
                Text(
                    text = "Values are in grams, except sodium (mg).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        entry.copy(
                            name = name.trim(),
                            quantity = quantity.trim(),
                            calories = calories.toIntOrNull() ?: 0,
                            proteinGrams = protein.toDoubleOrNull() ?: 0.0,
                            carbsGrams = carbs.toDoubleOrNull() ?: 0.0,
                            fatGrams = fat.toDoubleOrNull() ?: 0.0,
                            fiberGrams = fiber.toDoubleOrNull() ?: 0.0,
                            sugarGrams = sugar.toDoubleOrNull() ?: 0.0,
                            sodiumMg = sodium.toDoubleOrNull() ?: 0.0
                        )
                    )
                },
                enabled = canSave
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            onValueChange(input.filter { it.isDigit() || it == '.' })
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

private val numberFormat: NumberFormat = NumberFormat.getIntegerInstance()

private fun formatNumber(value: Int): String = numberFormat.format(value)

private fun formatGrams(value: Double): String =
    if (value >= 10 || value == 0.0) value.toInt().toString()
    else String.format(Locale.US, "%.1f", value)

private fun todayLabel(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.US))
