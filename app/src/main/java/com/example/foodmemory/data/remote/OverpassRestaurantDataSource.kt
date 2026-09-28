package com.example.foodmemory.data.remote

import android.location.Location
import com.example.foodmemory.domain.model.NearbyRestaurant
import com.example.foodmemory.domain.repository.NearbyPlacesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

class OverpassRestaurantDataSource : NearbyPlacesRepository {
    override suspend fun findNearby(latitude: Double, longitude: Double): List<NearbyRestaurant> =
        withContext(Dispatchers.IO) {
            val cacheKey = "${(latitude * 1000).toInt()}:${(longitude * 1000).toInt()}"
            val now = System.currentTimeMillis()
            placeCache[cacheKey]?.takeIf { now - it.savedAt < CACHE_TTL_MILLIS }?.let { return@withContext it.places }

            coroutineScope {
                val attempts = ENDPOINTS.mapTo(mutableListOf()) { endpoint ->
                    async { runCatching { runInterruptible { fetchNearby(latitude, longitude, endpoint) } } }
                }
                var lastFailure: Throwable? = null
                while (attempts.isNotEmpty()) {
                    val (finishedIndex, result) = select<Pair<Int, Result<List<NearbyRestaurant>>>> {
                        attempts.forEachIndexed { index, attempt -> attempt.onAwait { index to it } }
                    }
                    attempts.removeAt(finishedIndex)
                    result.onSuccess { places ->
                        placeCache[cacheKey] = CachedPlaces(now, places)
                        attempts.forEach { it.cancel() }
                        return@coroutineScope places
                    }.onFailure { failure ->
                        if (failure is CancellationException) throw failure
                        lastFailure = failure
                    }
                }
                throw lastFailure ?: IOException("Nearby search failed")
            }
        }

    private fun fetchNearby(latitude: Double, longitude: Double, endpoint: String): List<NearbyRestaurant> {
        val query = "[out:json][timeout:8];(nwr[\"amenity\"~\"bar|pub|restaurant|cafe|fast_food\"][\"name\"](around:$SEARCH_RADIUS_METERS,$latitude,$longitude););out center tags;"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 3_500
            readTimeout = 9_000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            setRequestProperty("User-Agent", "FoodMemoryDemo/1.0 (Android)")
        }
        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
                it.write("data=${URLEncoder.encode(query, "UTF-8")}")
            }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) throw OverpassResponseException(responseCode)

            val elements = JSONObject(body).optJSONArray("elements") ?: return emptyList()
            return buildList {
                for (index in 0 until elements.length()) {
                    val item = elements.optJSONObject(index) ?: continue
                    val tags = item.optJSONObject("tags") ?: continue
                    val name = tags.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val center = item.optJSONObject("center")
                    val lat = if (item.has("lat")) item.optDouble("lat") else center?.optDouble("lat") ?: continue
                    val lon = if (item.has("lon")) item.optDouble("lon") else center?.optDouble("lon") ?: continue
                    val address = listOfNotNull(
                        tags.optString("addr:street").takeIf { it.isNotBlank() },
                        tags.optString("addr:housenumber").takeIf { it.isNotBlank() }
                    ).joinToString(" ").ifBlank { null }
                    val distance = FloatArray(1)
                    Location.distanceBetween(latitude, longitude, lat, lon, distance)
                    add(
                        NearbyRestaurant(
                            item.optLong("id"), name, tags.optString("amenity"), lat, lon,
                            address, distance[0].roundToInt()
                        )
                    )
                }
            }.distinctBy { it.id }.sortedBy { it.distanceMeters }
        } finally {
            connection.disconnect()
        }
    }

    private class OverpassResponseException(val responseCode: Int) : IOException("Overpass returned HTTP $responseCode")

    private data class CachedPlaces(val savedAt: Long, val places: List<NearbyRestaurant>)

    private companion object {
        val placeCache = ConcurrentHashMap<String, CachedPlaces>()
        const val CACHE_TTL_MILLIS = 5 * 60 * 1000L
        val ENDPOINTS = listOf(
            "https://overpass.private.coffee/api/interpreter",
            "https://overpass-api.de/api/interpreter"
        )
        const val SEARCH_RADIUS_METERS = 800
    }
}
