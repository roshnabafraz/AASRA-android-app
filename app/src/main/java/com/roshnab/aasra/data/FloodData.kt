package com.roshnab.aasra.data

import org.osmdroid.util.GeoPoint
import org.json.JSONArray

object FloodData {
    // Keep your existing raw flood string here...
    private const val FLOOD_RAW = "[[77.56214783,35.47998346], ... ]" // Paste your full array

    fun getFloodBoundary(): List<GeoPoint> {
        val list = mutableListOf<GeoPoint>()
        try {
            val jsonArray = JSONArray(FLOOD_RAW)
            for (i in 0 until jsonArray.length()) {
                val point = jsonArray.getJSONArray(i)
                // Government data is [Long, Lat] -> Convert to GeoPoint(Lat, Long)
                list.add(GeoPoint(point.getDouble(1), point.getDouble(0)))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    // --- NEW: Approximate Indus River Path ---
    fun getIndusRiver(): List<GeoPoint> {
        return listOf(
            GeoPoint(35.3, 75.6), // Skardu (North)
            GeoPoint(34.1, 72.9), // Tarbela
            GeoPoint(32.9, 71.5), // Kalabagh
            GeoPoint(30.8, 70.8), // Taunsa
            GeoPoint(28.4, 70.3), // Rahim Yar Khan
            GeoPoint(27.5, 68.8), // Sukkur
            GeoPoint(25.3, 68.3), // Hyderabad
            GeoPoint(24.0, 67.5)  // Karachi (Sea)
        )
    }
}