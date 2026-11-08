package com.jacob.voicecalories.nutrition

import com.jacob.voicecalories.data.FoodEntry
import kotlin.math.roundToInt

data class NutritionItem(
    val name: String,
    val quantity: String,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val fiberGrams: Double = 0.0,
    val sugarGrams: Double = 0.0,
    val sodiumMg: Double = 0.0
)

data class NutritionResult(
    val items: List<NutritionItem>,
    val source: String,
    val warning: String? = null
)

fun NutritionItem.toFoodEntry(
    rawText: String,
    timestamp: Long = System.currentTimeMillis(),
    source: String
): FoodEntry = FoodEntry(
    name = name,
    quantity = quantity,
    calories = calories,
    proteinGrams = proteinGrams,
    carbsGrams = carbsGrams,
    fatGrams = fatGrams,
    fiberGrams = fiberGrams,
    sugarGrams = sugarGrams,
    sodiumMg = sodiumMg,
    rawText = rawText,
    timestamp = timestamp,
    source = source
)

internal fun Double.roundGrams(): Double = (this * 10).roundToInt() / 10.0
