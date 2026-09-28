package com.example.foodmemory.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.foodmemory.domain.model.DishAnalysis
import com.example.foodmemory.domain.model.DishIngredient
import com.example.foodmemory.domain.model.IngredientEvidence
import com.example.foodmemory.domain.repository.DishAnalysisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class HttpDishAnalysisRepository(private val baseUrl: String) : DishAnalysisRepository {
    override suspend fun analyze(photoPath: String): DishAnalysis = withContext(Dispatchers.IO) {
        val photo = File(photoPath)
        val bitmap = BitmapFactory.decodeFile(photo.absolutePath)
            ?: throw IllegalArgumentException("No se pudo leer la fotografía.")
        val resized = resize(bitmap, maxDimension = 1280)
        val imageBytes = ByteArrayOutputStream().use { output ->
            resized.compress(Bitmap.CompressFormat.JPEG, 82, output)
            output.toByteArray()
        }
        if (resized !== bitmap) resized.recycle()
        bitmap.recycle()

        val requestJson = JSONObject()
            .put("mimeType", "image/jpeg")
            .put("imageBase64", Base64.encodeToString(imageBytes, Base64.NO_WRAP))
            .toString()
        val connection = (URL(baseUrl.trimEnd('/') + "/analyze-dish").openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 55_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { it.write(requestJson.toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            val responseText = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (responseCode !in 200..299) {
                val message = runCatching { JSONObject(responseText).optString("error") }.getOrNull()
                throw IllegalStateException(message?.takeIf(String::isNotBlank) ?: "No se pudo analizar el plato ($responseCode).")
            }
            parseAnalysis(JSONObject(responseText))
        } finally {
            connection.disconnect()
        }
    }

    private fun resize(source: Bitmap, maxDimension: Int): Bitmap {
        val largestSide = maxOf(source.width, source.height)
        if (largestSide <= maxDimension) return source
        val scale = maxDimension.toFloat() / largestSide
        return Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun parseAnalysis(json: JSONObject): DishAnalysis {
        val ingredientsJson = json.optJSONArray("ingredients")
        val ingredients = buildList {
            if (ingredientsJson != null) {
                for (index in 0 until ingredientsJson.length()) {
                    val ingredient = ingredientsJson.optJSONObject(index) ?: continue
                    val name = ingredient.optString("name").trim()
                    if (name.isNotBlank()) {
                        add(
                            DishIngredient(
                                name = name,
                                evidence = if (ingredient.optString("evidence") == "VISIBLE") {
                                    IngredientEvidence.VISIBLE
                                } else IngredientEvidence.TYPICAL
                            )
                        )
                    }
                }
            }
        }
        return DishAnalysis(
            isFood = json.optBoolean("isFood"),
            dishName = json.optString("dishName").trim(),
            description = json.optString("description").trim(),
            ingredients = ingredients,
            uncertainty = json.optString("uncertainty").trim()
        )
    }
}
