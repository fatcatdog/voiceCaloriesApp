package com.jacob.voicecalories.nutrition

import com.jacob.voicecalories.data.AppSettings
import com.jacob.voicecalories.data.Totals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MacroEvaluatorTest {

    @Test
    fun returnsEmptyReportWhenNoEntries() {
        val report = MacroEvaluator.evaluate(
            totals = Totals(),
            settings = AppSettings(),
            entryCount = 0
        )

        assertFalse(report.hasEntries)
        assertEquals(0, report.items.size)
        assertEquals(1, report.insights.size)
        assertEquals(MetricStatus.INFO, report.insights[0].status)
    }

    @Test
    fun flagsTooMuchSaltWhenSodiumExceedsLimit() {
        val totals = Totals(
            calories = 1500,
            proteinGrams = 100.0,
            carbsGrams = 150.0,
            fatGrams = 50.0,
            fiberGrams = 25.0,
            sugarGrams = 30.0,
            sodiumMg = 2800.0 // > 2300 mg limit
        )

        val report = MacroEvaluator.evaluate(
            totals = totals,
            settings = AppSettings(calorieGoal = 2000, proteinGoal = 120),
            entryCount = 3
        )

        assertTrue(report.hasEntries)
        val saltInsight = report.insights.find { it.title.contains("Salt", ignoreCase = true) }
        assertTrue("Expected high salt insight", saltInsight != null)
        assertEquals(MetricStatus.ALERT, saltInsight?.status)

        val sodiumItem = report.items.find { it.id == "sodium" }
        assertEquals(MetricStatus.ALERT, sodiumItem?.status)
        assertEquals("Too High", sodiumItem?.statusLabel)
        assertTrue(sodiumItem?.isOverLimit == true)
    }

    @Test
    fun flagsNotEnoughProteinWhenProteinIsLow() {
        val totals = Totals(
            calories = 1400,
            proteinGrams = 35.0, // << 120 * 0.7 = 84
            carbsGrams = 200.0,
            fatGrams = 50.0,
            fiberGrams = 25.0,
            sugarGrams = 25.0,
            sodiumMg = 1600.0
        )

        val report = MacroEvaluator.evaluate(
            totals = totals,
            settings = AppSettings(calorieGoal = 2000, proteinGoal = 120),
            entryCount = 2
        )

        val proteinInsight = report.insights.find { it.title.contains("Protein", ignoreCase = true) }
        assertTrue("Expected low protein insight", proteinInsight != null)
        assertEquals("Not Enough Protein", proteinInsight?.title)
        assertEquals(MetricStatus.ALERT, proteinInsight?.status)

        val proteinItem = report.items.find { it.id == "protein" }
        assertEquals(MetricStatus.ALERT, proteinItem?.status)
        assertEquals("Not Enough", proteinItem?.statusLabel)
    }

    @Test
    fun celebratesProteinGoalHit() {
        val totals = Totals(
            calories = 1900,
            proteinGrams = 135.0, // >= 120
            carbsGrams = 180.0,
            fatGrams = 60.0,
            fiberGrams = 26.0,
            sugarGrams = 30.0,
            sodiumMg = 1800.0
        )

        val report = MacroEvaluator.evaluate(
            totals = totals,
            settings = AppSettings(calorieGoal = 2000, proteinGoal = 120),
            entryCount = 4
        )

        val proteinInsight = report.insights.find { it.title.contains("Goal Reached", ignoreCase = true) }
        assertTrue("Expected protein goal reached insight", proteinInsight != null)
        assertEquals(MetricStatus.OPTIMAL, proteinInsight?.status)

        val proteinItem = report.items.find { it.id == "protein" }
        assertEquals(MetricStatus.OPTIMAL, proteinItem?.status)
        assertEquals("Goal Met", proteinItem?.statusLabel)
    }

    @Test
    fun calculatesMacroCalorieRatiosCorrectly() {
        // Protein: 100g * 4 = 400 kcal (40%)
        // Carbs: 100g * 4 = 400 kcal (40%)
        // Fat: 22.2g * 9 ~= 200 kcal (20%)
        // Total macro kcal = 1000 kcal
        val totals = Totals(
            calories = 1000,
            proteinGrams = 100.0,
            carbsGrams = 100.0,
            fatGrams = 22.22,
            fiberGrams = 25.0,
            sugarGrams = 20.0,
            sodiumMg = 1500.0
        )

        val report = MacroEvaluator.evaluate(
            totals = totals,
            settings = AppSettings(),
            entryCount = 2
        )

        assertEquals(40, report.ratio.proteinPct)
        assertEquals(40, report.ratio.carbsPct)
        assertEquals(20, report.ratio.fatPct)
    }
}
