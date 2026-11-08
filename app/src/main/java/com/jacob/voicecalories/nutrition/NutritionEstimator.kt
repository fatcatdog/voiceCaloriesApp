package com.jacob.voicecalories.nutrition

import com.jacob.voicecalories.data.AppSettings

object NutritionEstimator {

    suspend fun estimate(settings: AppSettings, text: String): NutritionResult {
        val trimmed = text.trim()
        require(trimmed.isNotBlank()) { "Tell me what you ate." }

        if (settings.apiKey.isNotBlank()) {
            return try {
                NutritionResult(
                    items = GeminiClient.estimate(settings.apiKey, settings.model, trimmed),
                    source = "Gemini"
                )
            } catch (error: Exception) {
                val local = LocalNutrition.estimate(trimmed)
                if (local.isEmpty()) throw error
                NutritionResult(
                    items = local,
                    source = "Built-in estimate",
                    warning = "Gemini wasn't available (${error.message?.take(120)}). " +
                        "Used the built-in food database instead."
                )
            }
        }

        val local = LocalNutrition.estimate(trimmed)
        require(local.isNotEmpty()) {
            "I couldn't find any food in that. Try \"an apple and a turkey sandwich\"."
        }
        return NutritionResult(
            items = local,
            source = "Built-in estimate",
            warning = "Add a Gemini API key in Settings for better estimates of mixed meals."
        )
    }
}
