package com.roshnab.aasra.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.util.GeoPoint
import java.net.URL

object FloodRepository {

    private const val DATA_URL = "https://ffd.pmd.gov.pk/js/kmz_values.js"

    // Suspend function to fetch data without blocking the UI
    suspend fun fetchFloodData(): List<GeoPoint> {
        return withContext(Dispatchers.IO) {
            val points = mutableListOf<GeoPoint>()
            try {
                // 1. Download the raw text from the URL
                val rawJsFile = URL(DATA_URL).readText()

                // 2. Clean the data
                // The file looks like: "var kmz_pakistan=[[77.1, 35.2], ...];"
                // We need to find the array start "[" and end "]"
                val startIndex = rawJsFile.indexOf("[")
                val endIndex = rawJsFile.lastIndexOf("]") + 1

                if (startIndex != -1 && endIndex != -1) {
                    val cleanJsonString = rawJsFile.substring(startIndex, endIndex)

                    // 3. Parse JSON
                    val jsonArray = JSONArray(cleanJsonString)
                    for (i in 0 until jsonArray.length()) {
                        val coordinate = jsonArray.getJSONArray(i)
                        val lon = coordinate.getDouble(0)
                        val lat = coordinate.getDouble(1)
                        points.add(GeoPoint(lat, lon))
                    }
                    Log.d("AASRA_DATA", "Successfully loaded ${points.size} flood points.")
                }
            } catch (e: Exception) {
                Log.e("AASRA_DATA", "Failed to fetch flood data", e)
                // Return empty list on failure so app doesn't crash
            }
            return@withContext points
        }
    }
}