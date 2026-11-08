package com.jacob.voicecalories.nutrition

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Offline fallback estimator. It is intentionally simple: split the sentence
 * into food phrases, match each phrase against a small built-in table, and
 * scale by a leading number. Gemini gives much better results for mixed meals,
 * but this keeps the app usable without a key or a network connection.
 */
object LocalNutrition {

    private data class Food(
        val keywords: List<String>,
        val name: String,
        val quantity: String,
        val calories: Int,
        val protein: Double,
        val carbs: Double,
        val fat: Double,
        val fiber: Double = 0.0,
        val sugar: Double = 0.0,
        val sodium: Double = 0.0
    )

    private val foods: List<Food> = listOf(
        Food(listOf("diet coke", "diet coca cola", "coke zero", "zero sugar cola", "diet soda"), "Diet soda", "1 can", 0, 0.0, 0.0, 0.0, 0.0, 0.0, 35.0),
        Food(listOf("coca cola", "coke", "cola", "soft drink", "soda"), "Soda", "1 can", 140, 0.0, 39.0, 0.0, 0.0, 39.0, 45.0),
        Food(listOf("water", "sparkling water"), "Water", "1 glass", 0, 0.0, 0.0, 0.0),
        Food(listOf("black coffee", "coffee"), "Coffee", "1 cup", 2, 0.3, 0.0, 0.0),
        Food(listOf("latte", "coffee with milk", "cappuccino"), "Coffee with milk", "1 cup", 120, 6.0, 10.0, 6.0),
        Food(listOf("sweet tea"), "Sweet tea", "1 glass", 120, 0.0, 30.0, 0.0, 0.0, 30.0, 10.0),
        Food(listOf("iced tea", "green tea", "tea"), "Tea", "1 cup", 2, 0.0, 0.5, 0.0),
        Food(listOf("orange juice", "apple juice", "juice"), "Juice", "1 glass", 112, 1.7, 26.0, 0.5, 0.5, 21.0, 2.0),
        Food(listOf("lemonade"), "Lemonade", "1 glass", 120, 0.0, 31.0, 0.0, 0.0, 28.0, 5.0),
        Food(listOf("energy drink"), "Energy drink", "1 can", 110, 0.0, 28.0, 0.0, 0.0, 27.0, 200.0),
        Food(listOf("smoothie"), "Smoothie", "1 medium", 200, 5.0, 38.0, 3.0, 4.0, 28.0, 80.0),

        Food(listOf("apple"), "Apple", "1 medium", 95, 0.5, 25.0, 0.3, 4.4, 19.0, 2.0),
        Food(listOf("banana"), "Banana", "1 medium", 105, 1.3, 27.0, 0.4, 3.1, 14.0, 1.0),
        Food(listOf("orange"), "Orange", "1 medium", 62, 1.2, 15.4, 0.2, 3.1, 12.2, 0.0),
        Food(listOf("grapes"), "Grapes", "1 cup", 104, 1.1, 27.0, 0.2, 1.4, 23.0, 2.0),
        Food(listOf("strawberries"), "Strawberries", "1 cup", 49, 1.0, 11.7, 0.5, 3.0, 7.4, 2.0),
        Food(listOf("blueberries"), "Blueberries", "1 cup", 84, 1.1, 21.4, 0.5, 3.6, 14.7, 1.0),
        Food(listOf("avocado"), "Avocado", "1 medium", 240, 3.0, 12.8, 22.0, 10.0, 0.7, 11.0),
        Food(listOf("apple sauce", "applesauce"), "Applesauce", "1 cup", 100, 0.2, 25.0, 0.2, 2.0, 20.0, 5.0),

        Food(listOf("egg", "scrambled egg", "fried egg"), "Egg", "1 large", 78, 6.3, 0.6, 5.3, 0.0, 0.6, 62.0),
        Food(listOf("toast", "bread"), "Bread", "1 slice", 80, 2.7, 14.0, 1.0, 0.8, 1.4, 150.0),
        Food(listOf("bagel"), "Bagel", "1 medium", 250, 10.0, 49.0, 1.5, 2.0, 6.0, 450.0),
        Food(listOf("oatmeal", "oats"), "Oatmeal", "1 cup cooked", 150, 5.0, 27.0, 3.0, 4.0, 1.0, 0.0),
        Food(listOf("cereal"), "Cereal", "1 cup", 150, 3.0, 33.0, 1.0, 2.0, 12.0, 200.0),
        Food(listOf("greek yogurt", "yogurt"), "Greek yogurt", "1 cup", 100, 10.0, 6.0, 3.0, 0.0, 4.0, 50.0),
        Food(listOf("milk"), "Milk", "1 cup", 103, 8.0, 12.0, 2.4, 0.0, 12.0, 107.0),
        Food(listOf("protein shake", "protein powder"), "Protein shake", "1 scoop", 120, 24.0, 3.0, 1.5, 0.0, 1.0, 150.0),
        Food(listOf("granola bar"), "Granola bar", "1 bar", 190, 4.0, 29.0, 7.0, 2.0, 12.0, 100.0),

        Food(listOf("turkey sandwich"), "Turkey sandwich", "1 sandwich", 380, 24.0, 45.0, 12.0, 3.0, 6.0, 1100.0),
        Food(listOf("chicken sandwich"), "Chicken sandwich", "1 sandwich", 450, 28.0, 45.0, 17.0, 2.0, 6.0, 1000.0),
        Food(listOf("tuna sandwich"), "Tuna sandwich", "1 sandwich", 400, 22.0, 42.0, 16.0, 2.5, 5.0, 900.0),
        Food(listOf("sandwich"), "Sandwich", "1 sandwich", 350, 18.0, 40.0, 12.0, 2.0, 5.0, 900.0),
        Food(listOf("cheeseburger", "hamburger", "burger"), "Burger", "1 burger", 500, 25.0, 40.0, 25.0, 2.0, 8.0, 900.0),
        Food(listOf("hot dog"), "Hot dog", "1 hot dog", 290, 10.0, 24.0, 17.0, 1.0, 5.0, 800.0),
        Food(listOf("fries", "french fries"), "French fries", "1 medium", 365, 4.0, 48.0, 17.0, 4.0, 0.0, 250.0),
        Food(listOf("pizza"), "Pizza", "1 slice", 285, 12.0, 36.0, 10.0, 2.0, 4.0, 640.0),
        Food(listOf("pasta", "spaghetti", "noodles"), "Pasta", "1 cup cooked", 220, 8.0, 43.0, 1.3, 2.5, 1.0, 1.0),
        Food(listOf("rice"), "Rice", "1 cup cooked", 205, 4.3, 45.0, 0.4, 0.6, 0.1, 2.0),
        Food(listOf("salad"), "Salad", "1 bowl", 150, 5.0, 12.0, 9.0, 4.0, 4.0, 300.0),
        Food(listOf("chicken breast", "grilled chicken", "chicken"), "Chicken", "1 breast", 165, 31.0, 0.0, 3.6, 0.0, 0.0, 74.0),
        Food(listOf("salmon"), "Salmon", "1 fillet", 208, 20.0, 0.0, 13.0, 0.0, 0.0, 59.0),
        Food(listOf("steak"), "Steak", "1 serving", 270, 26.0, 0.0, 18.0, 0.0, 0.0, 60.0),
        Food(listOf("tofu"), "Tofu", "1 cup", 144, 16.0, 3.0, 9.0, 2.0, 1.0, 18.0),
        Food(listOf("black beans", "beans"), "Beans", "1 cup", 227, 15.0, 40.0, 0.9, 15.0, 1.0, 2.0),
        Food(listOf("soup"), "Soup", "1 bowl", 150, 6.0, 20.0, 5.0, 3.0, 4.0, 700.0),
        Food(listOf("sushi"), "Sushi", "6 pieces", 300, 12.0, 45.0, 7.0, 2.0, 6.0, 600.0),
        Food(listOf("taco"), "Taco", "1 taco", 170, 8.0, 13.0, 9.0, 2.0, 1.0, 500.0),
        Food(listOf("burrito"), "Burrito", "1 burrito", 500, 20.0, 60.0, 18.0, 5.0, 3.0, 1200.0),
        Food(listOf("macaroni cheese", "macaroni and cheese", "mac and cheese"), "Mac and cheese", "1 cup", 310, 12.0, 38.0, 12.0, 2.0, 4.0, 700.0),
        Food(listOf("peanut butter"), "Peanut butter", "2 tbsp", 190, 8.0, 7.0, 16.0, 2.0, 3.0, 140.0),
        Food(listOf("nuts", "almonds", "peanuts"), "Nuts", "1 oz", 170, 6.0, 6.0, 15.0, 2.0, 1.0, 5.0),
        Food(listOf("cheese"), "Cheese", "1 slice", 110, 7.0, 0.4, 9.0, 0.0, 0.1, 180.0),
        Food(listOf("bacon"), "Bacon", "1 slice", 43, 3.0, 0.1, 3.3, 0.0, 0.0, 137.0),
        Food(listOf("ham"), "Ham", "1 slice", 60, 5.0, 1.0, 3.5, 0.0, 0.5, 250.0),
        Food(listOf("pickle"), "Pickle", "1 spear", 5, 0.3, 1.0, 0.1, 0.3, 0.5, 300.0),
        Food(listOf("popcorn"), "Popcorn", "3 cups", 150, 3.0, 19.0, 7.0, 3.0, 0.0, 2.0),
        Food(listOf("chips"), "Chips", "1 oz", 160, 2.0, 15.0, 10.0, 1.0, 0.0, 170.0),
        Food(listOf("pretzels"), "Pretzels", "1 oz", 108, 2.6, 22.0, 0.8, 1.0, 1.0, 385.0),
        Food(listOf("hummus"), "Hummus", "2 tbsp", 70, 2.0, 6.0, 5.0, 1.5, 0.0, 150.0),
        Food(listOf("butter"), "Butter", "1 tbsp", 102, 0.1, 0.0, 11.5, 0.0, 0.0, 91.0),
        Food(listOf("olive oil"), "Olive oil", "1 tbsp", 119, 0.0, 0.0, 13.5, 0.0, 0.0, 0.0),
        Food(listOf("chocolate"), "Chocolate", "1 bar", 235, 3.0, 26.0, 14.0, 3.0, 22.0, 20.0),
        Food(listOf("cookie"), "Cookie", "1 cookie", 150, 2.0, 20.0, 7.0, 0.5, 11.0, 100.0),
        Food(listOf("ice cream"), "Ice cream", "1 cup", 200, 3.5, 24.0, 11.0, 0.7, 21.0, 80.0),
        Food(listOf("cake"), "Cake", "1 slice", 350, 4.0, 50.0, 15.0, 1.0, 35.0, 300.0),
        Food(listOf("donut", "doughnut"), "Donut", "1 donut", 250, 3.0, 30.0, 14.0, 1.0, 15.0, 250.0),
        Food(listOf("beer"), "Beer", "1 bottle", 153, 1.6, 13.0, 0.0, 0.0, 0.0, 14.0),
        Food(listOf("wine"), "Wine", "1 glass", 125, 0.1, 4.0, 0.0, 0.0, 0.9, 6.0)
    ).sortedByDescending { food -> food.keywords.maxOf { it.length } }

    private val numberWords = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "a" to 1, "an" to 1, "half" to 1
    )

    private val stopWords = setOf(
        "a", "an", "the", "some", "my", "and", "also", "then", "plus",
        "food", "meal", "breakfast", "lunch", "dinner", "snack", "today",
        "to", "in", "on", "of", "for", "i", "we", "it", "that"
    )

    fun estimate(text: String): List<NutritionItem> {
        val cleaned = stripPrefixes(text)
        if (cleaned.isBlank()) return emptyList()

        // "mac and cheese" is one food, but "and" is also how people separate
        // foods. Protect this common phrase before splitting the sentence.
        val protected = cleaned.replace("mac and cheese", "macaroni cheese")

        return protected
            .split(Regex("""\s*(?:,|;|&|\bplus\b|\band\b)\s*"""))
            .mapNotNull { estimatePart(it) }
    }

    private fun stripPrefixes(text: String): String {
        var value = text.lowercase(Locale.US).trim()

        val verbPhrase =
            """(?:ate|had|have\s+eaten|eaten|am\s+going\s+to\s+(?:eat|drink|have)|""" +
                """(?:i['’]?m|we['’]?re)\s+going\s+to\s+(?:eat|drink|have)|""" +
                """will\s+(?:eat|drink|have))"""

        val prefixes = listOf(
            Regex("""^(?:hey\s+google[,\s]+)?(?:in\s+my\s+(?:calorie\s+tracker|food\s+log|diary)[,\s]+)"""),
            Regex("""^(?:please\s+)?(?:log|record|track|add)\s+(?:that\s+)?(?:i\s+)?(?:just\s+)?$verbPhrase(?:\s+|$)"""),
            Regex("""^(?:i|we)\s+(?:just\s+)?(?:ate|had|have\s+eaten|eaten)(?:\s+|$)"""),
            Regex("""^(?:i['’]?m|i\s+am|we['’]?re|we\s+are)\s+(?:going\s+to\s+)?(?:eat|drink|have)(?:\s+|$)"""),
            Regex("""^(?:please\s+)?(?:log|record|track|add)(?:\s+|$)""")
        )

        for (prefix in prefixes) {
            value = value.replaceFirst(prefix, "")
        }

        value = value.replace(
            Regex("""\b(?:in|to|into)\s+my\s+(?:calorie\s+tracker|food\s+log|diary|calorie\s+app)\b"""),
            " "
        )
        value = value.replace(
            Regex("""\b(?:for|at)\s+(?:breakfast|lunch|dinner|snack)\b"""),
            " "
        )
        value = value.replace(Regex("""\btoday\b"""), " ")

        return value.replace(Regex("""\s+"""), " ").trim(' ', '.', ',', '!', '?')
    }

    private fun estimatePart(rawPart: String): NutritionItem? {
        var part = rawPart.trim(' ', '.', ',', '!', '?', '\n')
        if (part.isBlank()) return null
        if (part in stopWords) return null
        if (part.none { it.isLetter() }) return null

        part = part.replace(Regex("""^(?:also|then|and|plus)\s+"""), "")

        var multiplier = 1
        val firstSpace = part.indexOf(' ')
        if (firstSpace > 0) {
            val first = part.substring(0, firstSpace)
            val parsed = first.toIntOrNull() ?: numberWords[first]
            if (parsed != null) {
                multiplier = parsed.coerceAtLeast(1)
                part = part.substring(firstSpace + 1)
            }
        }

        part = part.replace(Regex("""^(?:a|an|the|some|my)\s+"""), "")
        part = part.replace(
            Regex("""^(?:cup|glass|slice|bowl|plate|piece|can|bottle|serving)s?\s+of\s+"""),
            ""
        )
        part = part.trim()
        if (part.isBlank() || part in stopWords) return null

        val match = foods.firstOrNull { food ->
            food.keywords.any { keyword -> containsWord(part, keyword) }
        }

        if (match != null) {
            return NutritionItem(
                name = match.name,
                quantity = if (multiplier == 1) match.quantity else "$multiplier × ${match.quantity}",
                calories = match.calories * multiplier,
                proteinGrams = scale(match.protein, multiplier),
                carbsGrams = scale(match.carbs, multiplier),
                fatGrams = scale(match.fat, multiplier),
                fiberGrams = scale(match.fiber, multiplier),
                sugarGrams = scale(match.sugar, multiplier),
                sodiumMg = scale(match.sodium, multiplier)
            )
        }

        val displayName = part.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
        }

        return NutritionItem(
            name = displayName,
            quantity = if (multiplier == 1) "1 serving (estimated)" else "$multiplier servings (estimated)",
            calories = 200 * multiplier,
            proteinGrams = 8.0 * multiplier,
            carbsGrams = 25.0 * multiplier,
            fatGrams = 8.0 * multiplier,
            fiberGrams = 2.0 * multiplier,
            sugarGrams = 5.0 * multiplier,
            sodiumMg = 300.0 * multiplier
        )
    }

    private fun containsWord(text: String, word: String): Boolean =
        Regex("""\b${Regex.escape(word)}(?:s|es)?\b""").containsMatchIn(text)

    private fun scale(value: Double, multiplier: Int): Double =
        (value * multiplier * 10).roundToInt() / 10.0
}
