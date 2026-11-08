package com.jacob.voicecalories.nutrition

import androidx.compose.ui.graphics.Color
import com.jacob.voicecalories.data.AppSettings
import com.jacob.voicecalories.data.Totals
import com.jacob.voicecalories.ui.theme.CalorieAccentColor
import com.jacob.voicecalories.ui.theme.MacroCarbsColor
import com.jacob.voicecalories.ui.theme.MacroFatColor
import com.jacob.voicecalories.ui.theme.MacroFiberColor
import com.jacob.voicecalories.ui.theme.MacroProteinColor
import com.jacob.voicecalories.ui.theme.MacroSodiumColor
import com.jacob.voicecalories.ui.theme.MacroSugarColor
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

enum class MetricStatus {
    OPTIMAL,  // Green / check
    GOOD,     // Normal / on track
    WARNING,  // Amber / caution
    ALERT,    // Red / needs attention
    INFO      // Neutral
}

data class MacroInsight(
    val title: String,
    val message: String,
    val status: MetricStatus
)

data class MacroItem(
    val id: String,
    val name: String,
    val amountFormatted: String,
    val targetFormatted: String,
    val progress: Float,
    val isOverLimit: Boolean,
    val status: MetricStatus,
    val statusLabel: String,
    val recommendation: String,
    val accentColor: Color
)

data class MacroRatio(
    val proteinPct: Int,
    val carbsPct: Int,
    val fatPct: Int
)

data class MacroMetricsReport(
    val hasEntries: Boolean,
    val totalCalories: Int,
    val calorieGoal: Int,
    val insights: List<MacroInsight>,
    val items: List<MacroItem>,
    val ratio: MacroRatio
)

object MacroEvaluator {

    private val numberFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)

    fun evaluate(totals: Totals, settings: AppSettings, entryCount: Int): MacroMetricsReport {
        if (entryCount == 0) {
            return MacroMetricsReport(
                hasEntries = false,
                totalCalories = 0,
                calorieGoal = settings.calorieGoal,
                insights = listOf(
                    MacroInsight(
                        title = "No meals logged today",
                        message = "Log meals by voice or typing to see your personalized macro metrics, sodium limits, protein targets, and health insights.",
                        status = MetricStatus.INFO
                    )
                ),
                items = emptyList(),
                ratio = MacroRatio(0, 0, 0)
            )
        }

        val insights = mutableListOf<MacroInsight>()
        val items = mutableListOf<MacroItem>()

        val calorieGoal = settings.calorieGoal.coerceAtLeast(1)
        val remainingCalories = calorieGoal - totals.calories

        // 1. Protein Evaluation
        val proteinGoal = settings.proteinGoal.coerceAtLeast(1)
        val proteinProgress = (totals.proteinGrams / proteinGoal).toFloat()
        val proteinPct = (proteinProgress * 100).roundToInt()

        if (totals.proteinGrams < proteinGoal * 0.7) {
            insights += MacroInsight(
                title = "Not Enough Protein",
                message = "Protein is low (${formatGrams(totals.proteinGrams)}g of ${proteinGoal}g goal, $proteinPct%). High-protein foods like chicken, eggs, Greek yogurt, canned tuna, tofu, or protein shakes will help.",
                status = MetricStatus.ALERT
            )
            items += MacroItem(
                id = "protein",
                name = "Protein",
                amountFormatted = "${formatGrams(totals.proteinGrams)}g",
                targetFormatted = "Goal: ${proteinGoal}g ($proteinPct%)",
                progress = proteinProgress.coerceIn(0f, 1f),
                isOverLimit = false,
                status = MetricStatus.ALERT,
                statusLabel = "Not Enough",
                recommendation = "Low intake. Try adding chicken breast, eggs, Greek yogurt, cottage cheese, or lentils.",
                accentColor = MacroProteinColor
            )
        } else if (totals.proteinGrams >= proteinGoal) {
            insights += MacroInsight(
                title = "Protein Goal Reached!",
                message = "You've met your protein goal with ${formatGrams(totals.proteinGrams)}g of ${proteinGoal}g ($proteinPct%). Great job fueling your muscles!",
                status = MetricStatus.OPTIMAL
            )
            items += MacroItem(
                id = "protein",
                name = "Protein",
                amountFormatted = "${formatGrams(totals.proteinGrams)}g",
                targetFormatted = "Goal: ${proteinGoal}g ($proteinPct%)",
                progress = 1f,
                isOverLimit = false,
                status = MetricStatus.OPTIMAL,
                statusLabel = "Goal Met",
                recommendation = "Daily target achieved! Supports lean muscle and appetite satisfaction.",
                accentColor = MacroProteinColor
            )
        } else {
            items += MacroItem(
                id = "protein",
                name = "Protein",
                amountFormatted = "${formatGrams(totals.proteinGrams)}g",
                targetFormatted = "Goal: ${proteinGoal}g ($proteinPct%)",
                progress = proteinProgress.coerceIn(0f, 1f),
                isOverLimit = false,
                status = MetricStatus.GOOD,
                statusLabel = "On Track",
                recommendation = "Almost at your target. A high-protein snack will help you reach ${proteinGoal}g.",
                accentColor = MacroProteinColor
            )
        }

        // 2. Salt / Sodium Evaluation (Daily guideline limit: 2300 mg)
        val sodiumLimit = 2300.0
        val sodiumMg = totals.sodiumMg
        val sodiumProgress = (sodiumMg / sodiumLimit).toFloat()

        if (sodiumMg > sodiumLimit) {
            val overMg = (sodiumMg - sodiumLimit).roundToInt()
            insights += MacroInsight(
                title = "Too Much Salt (High Sodium)",
                message = "Sodium is high at ${formatNumber(sodiumMg.roundToInt())} mg (+${formatNumber(overMg)} mg over the 2,300 mg limit). Drink plenty of water and favor unsalted, fresh foods.",
                status = MetricStatus.ALERT
            )
            items += MacroItem(
                id = "sodium",
                name = "Sodium / Salt",
                amountFormatted = "${formatNumber(sodiumMg.roundToInt())} mg",
                targetFormatted = "Limit: 2,300 mg",
                progress = sodiumProgress.coerceIn(0f, 1f),
                isOverLimit = true,
                status = MetricStatus.ALERT,
                statusLabel = "Too High",
                recommendation = "Exceeds daily 2,300 mg limit. Drink extra water and choose low-sodium foods.",
                accentColor = MacroSodiumColor
            )
        } else if (sodiumMg in 1500.0..sodiumLimit) {
            items += MacroItem(
                id = "sodium",
                name = "Sodium / Salt",
                amountFormatted = "${formatNumber(sodiumMg.roundToInt())} mg",
                targetFormatted = "Limit: 2,300 mg",
                progress = sodiumProgress.coerceIn(0f, 1f),
                isOverLimit = false,
                status = MetricStatus.GOOD,
                statusLabel = "Within Limit",
                recommendation = "Well within the 2,300 mg maximum recommended by dietary guidelines.",
                accentColor = MacroSodiumColor
            )
        } else {
            items += MacroItem(
                id = "sodium",
                name = "Sodium / Salt",
                amountFormatted = "${formatNumber(sodiumMg.roundToInt())} mg",
                targetFormatted = "Limit: 2,300 mg",
                progress = sodiumProgress.coerceIn(0f, 1f),
                isOverLimit = false,
                status = MetricStatus.OPTIMAL,
                statusLabel = "Optimal",
                recommendation = "Low sodium level. Keep up the clean eating habits.",
                accentColor = MacroSodiumColor
            )
        }

        // 3. Sugar Evaluation (Guideline max: 50g)
        val sugarLimit = 50.0
        val sugarProgress = (totals.sugarGrams / sugarLimit).toFloat()

        if (totals.sugarGrams > sugarLimit) {
            insights += MacroInsight(
                title = "High Sugar",
                message = "Sugar intake is high at ${formatGrams(totals.sugarGrams)}g (recommended max: ~50g). Favor water, unsweetened beverages, or fresh whole fruits.",
                status = MetricStatus.WARNING
            )
            items += MacroItem(
                id = "sugar",
                name = "Sugar",
                amountFormatted = "${formatGrams(totals.sugarGrams)}g",
                targetFormatted = "Recommended Max: 50g",
                progress = sugarProgress.coerceIn(0f, 1f),
                isOverLimit = true,
                status = MetricStatus.ALERT,
                statusLabel = "High",
                recommendation = "Over recommended 50g max. Watch for sweetened drinks, sweets, and flavored sauces.",
                accentColor = MacroSugarColor
            )
        } else if (totals.sugarGrams in 25.0..sugarLimit) {
            items += MacroItem(
                id = "sugar",
                name = "Sugar",
                amountFormatted = "${formatGrams(totals.sugarGrams)}g",
                targetFormatted = "Recommended Max: 50g",
                progress = sugarProgress.coerceIn(0f, 1f),
                isOverLimit = false,
                status = MetricStatus.GOOD,
                statusLabel = "Moderate",
                recommendation = "Moderate sugar intake, keeping safely below the 50g threshold.",
                accentColor = MacroSugarColor
            )
        } else {
            items += MacroItem(
                id = "sugar",
                name = "Sugar",
                amountFormatted = "${formatGrams(totals.sugarGrams)}g",
                targetFormatted = "Recommended Max: 50g",
                progress = sugarProgress.coerceIn(0f, 1f),
                isOverLimit = false,
                status = MetricStatus.OPTIMAL,
                statusLabel = "Low",
                recommendation = "Great job keeping sugar low. Helps prevent energy crashes.",
                accentColor = MacroSugarColor
            )
        }

        // 4. Fiber Evaluation (Target: 25g)
        val fiberGoal = 25.0
        val fiberProgress = (totals.fiberGrams / fiberGoal).toFloat()

        if (totals.fiberGrams < 15.0) {
            insights += MacroInsight(
                title = "Low Fiber",
                message = "Fiber is low at ${formatGrams(totals.fiberGrams)}g (target: 25g+). Oats, berries, chia seeds, broccoli, or beans will boost your digestive health.",
                status = MetricStatus.WARNING
            )
            items += MacroItem(
                id = "fiber",
                name = "Fiber",
                amountFormatted = "${formatGrams(totals.fiberGrams)}g",
                targetFormatted = "Target: 25g",
                progress = fiberProgress.coerceIn(0f, 1f),
                isOverLimit = false,
                status = MetricStatus.WARNING,
                statusLabel = "Low",
                recommendation = "Under 25g daily target. Whole grains, beans, and vegetables support gut health.",
                accentColor = MacroFiberColor
            )
        } else if (totals.fiberGrams >= fiberGoal) {
            insights += MacroInsight(
                title = "Fiber Target Met",
                message = "You've reached ${formatGrams(totals.fiberGrams)}g fiber (target: 25g). Excellent for steady digestion and fullness!",
                status = MetricStatus.OPTIMAL
            )
            items += MacroItem(
                id = "fiber",
                name = "Fiber",
                amountFormatted = "${formatGrams(totals.fiberGrams)}g",
                targetFormatted = "Target: 25g",
                progress = 1f,
                isOverLimit = false,
                status = MetricStatus.OPTIMAL,
                statusLabel = "Target Met",
                recommendation = "Healthy fiber level reached. Helps regulate blood sugar and digestion.",
                accentColor = MacroFiberColor
            )
        } else {
            items += MacroItem(
                id = "fiber",
                name = "Fiber",
                amountFormatted = "${formatGrams(totals.fiberGrams)}g",
                targetFormatted = "Target: 25g",
                progress = fiberProgress.coerceIn(0f, 1f),
                isOverLimit = false,
                status = MetricStatus.GOOD,
                statusLabel = "Approaching",
                recommendation = "Approaching 25g target. A serving of fruit or vegetables will finish it off.",
                accentColor = MacroFiberColor
            )
        }

        // 5. Calories
        val calorieProgress = (totals.calories.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1f)
        if (remainingCalories < 0) {
            insights += MacroInsight(
                title = "Over Calorie Target",
                message = "${formatNumber(-remainingCalories)} kcal over your ${formatNumber(calorieGoal)} kcal goal. A light 30-minute walk burns roughly 150 kcal.",
                status = MetricStatus.WARNING
            )
            items += MacroItem(
                id = "calories",
                name = "Calories",
                amountFormatted = "${formatNumber(totals.calories)} kcal",
                targetFormatted = "${formatNumber(calorieGoal)} kcal goal (${formatNumber(-remainingCalories)} over)",
                progress = 1f,
                isOverLimit = true,
                status = MetricStatus.ALERT,
                statusLabel = "${formatNumber(-remainingCalories)} Over",
                recommendation = "Over calorie budget. Consider a walk or lighter meals tomorrow.",
                accentColor = CalorieAccentColor
            )
        } else {
            items += MacroItem(
                id = "calories",
                name = "Calories",
                amountFormatted = "${formatNumber(totals.calories)} kcal",
                targetFormatted = "${formatNumber(calorieGoal)} kcal goal (${formatNumber(remainingCalories)} left)",
                progress = calorieProgress,
                isOverLimit = false,
                status = if (remainingCalories in 0..200) MetricStatus.OPTIMAL else MetricStatus.GOOD,
                statusLabel = "${formatNumber(remainingCalories)} Left",
                recommendation = "${formatNumber(remainingCalories)} kcal remaining towards your daily target.",
                accentColor = CalorieAccentColor
            )
        }

        // 6. Carbs & Fat
        val carbsKcal = totals.carbsGrams * 4.0
        val fatKcal = totals.fatGrams * 9.0

        items += MacroItem(
            id = "carbs",
            name = "Carbohydrates",
            amountFormatted = "${formatGrams(totals.carbsGrams)}g",
            targetFormatted = "~${formatNumber(carbsKcal.roundToInt())} kcal",
            progress = 1f,
            isOverLimit = false,
            status = MetricStatus.INFO,
            statusLabel = "${formatNumber(carbsKcal.roundToInt())} kcal",
            recommendation = "Primary energy source. Prioritize complex carbs (oats, brown rice, sweet potatoes).",
            accentColor = MacroCarbsColor
        )

        items += MacroItem(
            id = "fat",
            name = "Total Fat",
            amountFormatted = "${formatGrams(totals.fatGrams)}g",
            targetFormatted = "~${formatNumber(fatKcal.roundToInt())} kcal",
            progress = 1f,
            isOverLimit = false,
            status = MetricStatus.INFO,
            statusLabel = "${formatNumber(fatKcal.roundToInt())} kcal",
            recommendation = "Essential for nutrient absorption. Favor unsaturated fats (avocado, nuts, olive oil).",
            accentColor = MacroFatColor
        )

        // 7. Macro Calorie Ratio
        val proteinKcal = totals.proteinGrams * 4.0
        val totalMacroKcal = proteinKcal + carbsKcal + fatKcal
        val ratio = if (totalMacroKcal > 0) {
            val pPct = ((proteinKcal / totalMacroKcal) * 100).roundToInt()
            val cPct = ((carbsKcal / totalMacroKcal) * 100).roundToInt()
            val fPct = (100 - pPct - cPct).coerceAtLeast(0)
            MacroRatio(pPct, cPct, fPct)
        } else {
            MacroRatio(0, 0, 0)
        }

        // If no alerts triggered, add a positive takeaway!
        if (insights.none { it.status == MetricStatus.ALERT }) {
            insights.add(
                0,
                MacroInsight(
                    title = "Macros In Balance",
                    message = "Sodium is within limits and your macronutrients are well distributed today.",
                    status = MetricStatus.OPTIMAL
                )
            )
        }

        return MacroMetricsReport(
            hasEntries = true,
            totalCalories = totals.calories,
            calorieGoal = calorieGoal,
            insights = insights,
            items = items,
            ratio = ratio
        )
    }

    private fun formatNumber(value: Number): String = numberFormat.format(value.toLong())

    private fun formatGrams(value: Double): String =
        if (value >= 10 || value == 0.0) value.roundToInt().toString()
        else String.format(Locale.US, "%.1f", value)
}
