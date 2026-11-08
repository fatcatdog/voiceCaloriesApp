package com.jacob.voicecalories.nutrition

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Small, dependency-light Gemini REST client.
 *
 * The app asks Gemini for structured JSON so the response can be stored
 * directly in the local database.
 */
object GeminiClient {

    private const val TAG = "GeminiClient"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_PROMPT = """
You are a nutrition estimator inside a calorie tracking app.

The user gives a free-form description of food and drinks they ate or are about to eat.
Break it into distinct items and estimate the nutrition for each item.

Rules:
- Use realistic standard portion sizes when the user does not state an amount.
- Calories, protein, carbs, and fat are for that individual item, not the whole meal.
- Diet soda, black coffee, water, and unsweetened tea are essentially 0 calories.
- Do not invent foods that were not mentioned.
- Combine modifiers into one item (for example "turkey sandwich" stays one item).
- If a described item is clearly not food or drink, omit it.
- Values are estimates; be reasonable rather than overly precise.
- Return only the JSON requested by the response schema.
"""

    suspend fun estimate(
        apiKey: String,
        model: String,
        mealText: String
    ): List<NutritionItem> = withContext(Dispatchers.IO) {
        val safeModel = model.trim().ifBlank { "gemini-2.5-flash" }
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/$safeModel:generateContent" +
                "?key=${apiKey.trim()}"

        val request = Request.Builder()
            .url(url)
            .post(buildRequestBody(mealText).toString().toRequestBody(JSON))
            .build()

        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val apiMessage = parseApiError(responseText)
                Log.w(TAG, "Gemini HTTP ${response.code}: $apiMessage")
                throw IOException(apiMessage ?: "Gemini request failed (HTTP ${response.code})")
            }
            parseItems(responseText)
        }
    }

    private fun buildRequestBody(mealText: String): JSONObject = JSONObject().apply {
        put(
            "systemInstruction",
            JSONObject().put(
                "parts",
                JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))
            )
        )
        put(
            "contents",
            JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", "Meal: $mealText")))
                }
            )
        )
        put(
            "generationConfig",
            JSONObject().apply {
                put("temperature", 0.2)
                put("responseMimeType", "application/json")
                put("responseSchema", responseSchema())
            }
        )
    }

    private fun responseSchema(): JSONObject {
        fun property(type: String, description: String) = JSONObject()
            .put("type", type)
            .put("description", description)

        val itemProperties = JSONObject().apply {
            put("name", property("STRING", "Short, natural name of the food or drink."))
            put("quantity", property("STRING", "Portion description, e.g. '1 medium' or '1 can'."))
            put("calories", property("INTEGER", "Estimated calories for this item."))
            put("proteinGrams", property("NUMBER", "Protein in grams."))
            put("carbsGrams", property("NUMBER", "Carbohydrates in grams."))
            put("fatGrams", property("NUMBER", "Fat in grams."))
            put("fiberGrams", property("NUMBER", "Fiber in grams. Use 0 if unknown."))
            put("sugarGrams", property("NUMBER", "Sugar in grams. Use 0 if unknown."))
            put("sodiumMg", property("NUMBER", "Sodium in milligrams. Use 0 if unknown."))
        }

        val itemSchema = JSONObject().apply {
            put("type", "OBJECT")
            put("properties", itemProperties)
            put(
                "required",
                JSONArray(
                    listOf(
                        "name",
                        "quantity",
                        "calories",
                        "proteinGrams",
                        "carbsGrams",
                        "fatGrams"
                    )
                )
            )
        }

        return JSONObject().apply {
            put("type", "OBJECT")
            put(
                "properties",
                JSONObject().put(
                    "items",
                    JSONObject().apply {
                        put("type", "ARRAY")
                        put("description", "One entry per distinct food or drink.")
                        put("items", itemSchema)
                    }
                )
            )
            put("required", JSONArray(listOf("items")))
        }
    }

    private fun parseItems(responseText: String): List<NutritionItem> {
        val root = JSONObject(responseText)
        val candidates = root.optJSONArray("candidates")
            ?: throw IOException("Gemini returned no response candidates.")
        if (candidates.length() == 0) {
            throw IOException("Gemini returned an empty response.")
        }

        val first = candidates.optJSONObject(0)
            ?: throw IOException("Gemini returned an unreadable response.")
        val parts = first.optJSONObject("content")?.optJSONArray("parts")
            ?: throw IOException("Gemini returned no content.")

        val raw = buildString {
            for (index in 0 until parts.length()) {
                append(parts.optJSONObject(index)?.optString("text").orEmpty())
            }
        }.trim()

        val jsonText = raw
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val itemsArray = JSONObject(jsonText).optJSONArray("items")
            ?: throw IOException("Gemini did not return a food list.")

        val items = ArrayList<NutritionItem>(itemsArray.length())
        for (index in 0 until itemsArray.length()) {
            val item = itemsArray.optJSONObject(index) ?: continue
            val name = item.optString("name").trim()
            if (name.isBlank()) continue

            items += NutritionItem(
                name = name,
                quantity = item.optString("quantity").trim().ifBlank { "1 serving" },
                calories = item.optDouble("calories", 0.0).roundToInt().coerceAtLeast(0),
                proteinGrams = item.optDouble("proteinGrams", 0.0).coerceAtLeast(0.0).roundGrams(),
                carbsGrams = item.optDouble("carbsGrams", 0.0).coerceAtLeast(0.0).roundGrams(),
                fatGrams = item.optDouble("fatGrams", 0.0).coerceAtLeast(0.0).roundGrams(),
                fiberGrams = item.optDouble("fiberGrams", 0.0).coerceAtLeast(0.0).roundGrams(),
                sugarGrams = item.optDouble("sugarGrams", 0.0).coerceAtLeast(0.0).roundGrams(),
                sodiumMg = item.optDouble("sodiumMg", 0.0).coerceAtLeast(0.0).roundToInt().toDouble()
            )
        }

        if (items.isEmpty()) {
            throw IOException("I couldn't identify any food in that description.")
        }
        return items
    }

    private fun parseApiError(body: String): String? = runCatching {
        JSONObject(body)
            .optJSONObject("error")
            ?.optString("message")
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
