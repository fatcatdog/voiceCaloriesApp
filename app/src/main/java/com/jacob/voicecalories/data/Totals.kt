package com.jacob.voicecalories.data

data class Totals(
    val calories: Int = 0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val fiberGrams: Double = 0.0,
    val sugarGrams: Double = 0.0,
    val sodiumMg: Double = 0.0
)

fun List<FoodEntry>.totals(): Totals = Totals(
    calories = sumOf { it.calories },
    proteinGrams = sumOf { it.proteinGrams },
    carbsGrams = sumOf { it.carbsGrams },
    fatGrams = sumOf { it.fatGrams },
    fiberGrams = sumOf { it.fiberGrams },
    sugarGrams = sumOf { it.sugarGrams },
    sodiumMg = sumOf { it.sodiumMg }
)
