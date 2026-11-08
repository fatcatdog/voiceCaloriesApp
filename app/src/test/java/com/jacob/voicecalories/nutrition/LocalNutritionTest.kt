package com.jacob.voicecalories.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNutritionTest {

    @Test
    fun parsesTheExampleMeal() {
        val items = LocalNutrition.estimate(
            "I am going to eat an apple, a diet coke, and a turkey sandwich"
        )

        assertEquals(3, items.size)
        assertEquals("Apple", items[0].name)
        assertEquals(95, items[0].calories)
        assertEquals("Diet soda", items[1].name)
        assertEquals(0, items[1].calories)
        assertEquals("Turkey sandwich", items[2].name)
        assertEquals(380, items[2].calories)
    }

    @Test
    fun handlesTheLogPrefixAndContractions() {
        val items = LocalNutrition.estimate(
            "log that I'm going to eat a banana and a coffee with milk"
        )

        assertEquals(2, items.size)
        assertEquals("Banana", items[0].name)
        assertEquals("Coffee with milk", items[1].name)
    }

    @Test
    fun scalesCountedFoods() {
        val items = LocalNutrition.estimate("2 apples")

        assertEquals(1, items.size)
        assertEquals("Apple", items[0].name)
        assertEquals(190, items[0].calories)
        assertTrue(items[0].quantity.contains("2"))
    }

    @Test
    fun keepsMacAndCheeseTogether() {
        val items = LocalNutrition.estimate("mac and cheese")

        assertEquals(1, items.size)
        assertEquals("Mac and cheese", items[0].name)
        assertEquals(310, items[0].calories)
    }

    @Test
    fun returnsEmptyForNonFoodText() {
        val items = LocalNutrition.estimate("log that I ate")
        assertTrue(items.isEmpty())
    }
}
