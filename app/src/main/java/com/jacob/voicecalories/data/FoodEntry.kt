package com.jacob.voicecalories.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_entries")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: String = "",
    val calories: Int,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val fiberGrams: Double = 0.0,
    val sugarGrams: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val rawText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "GEMINI"
)
