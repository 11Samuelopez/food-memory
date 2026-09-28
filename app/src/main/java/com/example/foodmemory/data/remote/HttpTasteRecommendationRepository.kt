package com.example.foodmemory.data.remote

import com.example.foodmemory.domain.model.AiTasteRecommendation
import com.example.foodmemory.domain.model.RecommendationType
import com.example.foodmemory.domain.model.TasteProfile
import com.example.foodmemory.domain.repository.TasteRecommendationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Android data adapter: sends only aggregated favorite dishes/places and validates the API JSON. */
class HttpTasteRecommendationRepository(private val baseUrl: String) : TasteRecommendationRepository {
    override suspend fun recommend(profile: TasteProfile): List<AiTasteRecommendation> = withContext(Dispatchers.IO) {
        val request = JSONObject()
            .put("favoriteDishes", JSONArray().apply {
                profile.favoriteDishes.forEach { put(JSONObject().put("name", it.name).put("rating", it.rating)) }
            })
            .put("placesToRepeat", JSONArray().apply {
                profile.placesToRepeat.forEach { put(JSONObject().put("name", it.name).put("rating", it.rating)) }
            })
            .toString()
        val connection = URL(baseUrl.trimEnd('/') + "/recommendations").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 55_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { it.write(request.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(responseText).optString("error") }.getOrNull()
                throw IllegalStateException(message?.takeIf(String::isNotBlank) ?: "No se pudieron cargar las sugerencias.")
            }
            val items = JSONObject(responseText).optJSONArray("recommendations") ?: return@withContext emptyList()
            buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val name = item.optString("name").trim()
                    val reason = item.optString("reason").trim()
                    val type = when (item.optString("type")) {
                        "DISH" -> RecommendationType.DISH
                        "RESTAURANT" -> RecommendationType.RESTAURANT
                        else -> continue
                    }
                    if (name.isNotBlank() && reason.isNotBlank()) add(AiTasteRecommendation(type, name, reason))
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
