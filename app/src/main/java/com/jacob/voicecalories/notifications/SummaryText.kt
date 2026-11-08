package com.jacob.voicecalories.notifications

import com.jacob.voicecalories.data.AppSettings
import com.jacob.voicecalories.data.Totals
import java.text.NumberFormat

object SummaryText {

    private val numberFormat: NumberFormat = NumberFormat.getIntegerInstance()

    fun title(totals: Totals, entryCount: Int): String =
        if (entryCount == 0) "No meals logged today" else "Today: ${kcal(totals.calories)} kcal"

    fun shortText(totals: Totals, settings: AppSettings, entryCount: Int): String {
        if (entryCount == 0) return "Tap to log your first meal by voice."
        val remaining = settings.calorieGoal - totals.calories
        val remainingText = when {
            remaining > 0 -> "${kcal(remaining)} kcal left"
            remaining == 0 -> "right at your goal"
            else -> "${kcal(-remaining)} kcal over goal"
        }
        return "${kcal(totals.calories)} kcal • ${grams(totals.proteinGrams)}g protein • $remainingText"
    }

    fun body(totals: Totals, settings: AppSettings, entryCount: Int): String {
        if (entryCount == 0) {
            return "Nothing logged yet today.\n\nTap this notification, then tap the mic and say " +
                "what you ate — for example, \"an apple, a diet coke, and a turkey sandwich\"."
        }

        val remaining = settings.calorieGoal - totals.calories
        val remainingLine = when {
            remaining > 0 -> "${kcal(remaining)} kcal left of your ${kcal(settings.calorieGoal)} kcal goal."
            remaining == 0 -> "You hit your ${kcal(settings.calorieGoal)} kcal goal."
            else -> "${kcal(-remaining)} kcal over your ${kcal(settings.calorieGoal)} kcal goal."
        }

        val macroLine = "${grams(totals.proteinGrams)}g protein • ${grams(totals.carbsGrams)}g carbs • " +
            "${grams(totals.fatGrams)}g fat"

        val tips = suggestions(totals, settings)
        return buildString {
            append(remainingLine)
            append("\n")
            append(macroLine)
            if (tips.isNotEmpty()) {
                append("\n\n")
                tips.forEach { tip ->
                    append("• ")
                    append(tip)
                    append("\n")
                }
            }
            append("\nTap to review or log another meal.")
        }.trim()
    }

    fun suggestions(totals: Totals, settings: AppSettings): List<String> {
        val tips = mutableListOf<String>()
        val remaining = settings.calorieGoal - totals.calories

        when {
            remaining < 0 -> tips += "A 30-minute walk burns roughly 150 kcal and can help balance the day."
            remaining > 700 -> tips += "You have plenty of room left. A balanced meal with protein and vegetables would fit well."
            remaining in 1..150 -> tips += "You're close to your calorie goal. Keep any late snack light."
            else -> tips += "You're on track. Keep your next meal balanced."
        }

        if (settings.proteinGoal > 0 && totals.proteinGrams < settings.proteinGoal * 0.7) {
            tips += "Protein is low (${grams(totals.proteinGrams)}g). Greek yogurt, eggs, chicken, tofu, or beans would help."
        }
        if (totals.fiberGrams in 0.1..24.0) {
            tips += "Fiber is under 25g. Fruit, beans, or whole grains are easy additions."
        }
        if (totals.sugarGrams > 60) {
            tips += "Sugar is high today. Choose water or unsweetened drinks next."
        }
        if (totals.sodiumMg > 2300) {
            tips += "Sodium is high. Go easy on salty foods for the rest of the day."
        }

        return tips.take(3)
    }

    fun kcal(value: Int): String = numberFormat.format(value)

    fun grams(value: Double): String =
        if (value >= 10 || value == 0.0) value.toInt().toString() else String.format("%.1f", value)
}
