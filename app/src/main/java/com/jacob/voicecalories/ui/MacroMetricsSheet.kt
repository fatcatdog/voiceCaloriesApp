package com.jacob.voicecalories.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jacob.voicecalories.data.AppSettings
import com.jacob.voicecalories.data.Totals
import com.jacob.voicecalories.nutrition.MacroEvaluator
import com.jacob.voicecalories.nutrition.MacroInsight
import com.jacob.voicecalories.nutrition.MacroItem
import com.jacob.voicecalories.nutrition.MetricStatus
import com.jacob.voicecalories.ui.theme.CalorieAccentColor
import com.jacob.voicecalories.ui.theme.MacroCarbsColor
import com.jacob.voicecalories.ui.theme.MacroFatColor
import com.jacob.voicecalories.ui.theme.MacroProteinColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroMetricsSheet(
    totals: Totals,
    settings: AppSettings,
    entryCount: Int,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val report = remember(totals, settings, entryCount) {
        MacroEvaluator.evaluate(totals, settings, entryCount)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Macro Metrics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Macronutrients, sodium, & health insights",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            if (!report.hasEntries) {
                EmptyMacroState(onDismiss = onDismiss)
            } else {
                // 1. Health Insights & Alerts (e.g. Too much salt, Not enough protein)
                if (report.insights.isNotEmpty()) {
                    Text(
                        text = "Key Health Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        report.insights.forEach { insight ->
                            InsightCard(insight = insight)
                        }
                    }
                }

                // 2. Macro Calorie Ratio Bar (Protein vs Carbs vs Fat)
                if (report.ratio.proteinPct > 0 || report.ratio.carbsPct > 0 || report.ratio.fatPct > 0) {
                    Text(
                        text = "Macro Calorie Split",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    MacroRatioCard(
                        proteinPct = report.ratio.proteinPct,
                        carbsPct = report.ratio.carbsPct,
                        fatPct = report.ratio.fatPct
                    )
                }

                // 3. Detailed Metric Breakdown Cards (Protein, Sodium, Fiber, Sugar, Calories, etc.)
                Text(
                    text = "Detailed Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    report.items.forEach { item ->
                        MacroItemCard(item = item)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InsightCard(insight: MacroInsight) {
    val (bgColor, borderColor, iconColor, icon) = when (insight.status) {
        MetricStatus.ALERT -> Quad(
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.error,
            Icons.Filled.Warning
        )
        MetricStatus.WARNING -> Quad(
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.tertiary,
            Icons.Filled.Warning
        )
        MetricStatus.OPTIMAL -> Quad(
            CalorieAccentColor.copy(alpha = 0.12f),
            CalorieAccentColor.copy(alpha = 0.35f),
            CalorieAccentColor,
            Icons.Filled.CheckCircle
        )
        MetricStatus.GOOD, MetricStatus.INFO -> Quad(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.primary,
            Icons.Filled.Info
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(22.dp)
                    .padding(top = 2.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MacroRatioCard(proteinPct: Int, carbsPct: Int, fatPct: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Segmented Distribution Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                if (proteinPct > 0) {
                    Box(
                        modifier = Modifier
                            .weight(proteinPct.toFloat())
                            .background(MacroProteinColor)
                    )
                }
                if (carbsPct > 0) {
                    Box(
                        modifier = Modifier
                            .weight(carbsPct.toFloat())
                            .background(MacroCarbsColor)
                    )
                }
                if (fatPct > 0) {
                    Box(
                        modifier = Modifier
                            .weight(fatPct.toFloat())
                            .background(MacroFatColor)
                    )
                }
            }

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RatioLegendItem(label = "Protein", pct = proteinPct, color = MacroProteinColor)
                RatioLegendItem(label = "Carbs", pct = carbsPct, color = MacroCarbsColor)
                RatioLegendItem(label = "Fat", pct = fatPct, color = MacroFatColor)
            }
        }
    }
}

@Composable
private fun RatioLegendItem(label: String, pct: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$label: $pct%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MacroItemCard(item: MacroItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top: Name + Indicator Dot + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(item.accentColor)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                StatusBadge(status = item.status, label = item.statusLabel)
            }

            // Middle: Amount & Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = item.amountFormatted,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isOverLimit) MaterialTheme.colorScheme.error else item.accentColor
                )
                Text(
                    text = item.targetFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress Bar (for trackable metrics)
            if (item.id in listOf("protein", "sodium", "sugar", "fiber", "calories")) {
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (item.isOverLimit) MaterialTheme.colorScheme.error else item.accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Bottom: Actionable tip / recommendation
            Text(
                text = item.recommendation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusBadge(status: MetricStatus, label: String) {
    val (bgColor, textColor) = when (status) {
        MetricStatus.ALERT -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f) to MaterialTheme.colorScheme.error
        MetricStatus.WARNING -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) to MaterialTheme.colorScheme.tertiary
        MetricStatus.OPTIMAL -> CalorieAccentColor.copy(alpha = 0.15f) to CalorieAccentColor
        MetricStatus.GOOD, MetricStatus.INFO -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyMacroState(onDismiss: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = CalorieAccentColor,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "No Meals Logged Today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Log your meals by voice or typing to see your personalized macro metrics, salt levels, protein intake, fiber, sugar, and actionable health insights.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
