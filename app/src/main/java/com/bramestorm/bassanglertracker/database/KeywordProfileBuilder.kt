package com.bramestorm.bassanglertracker.database

import android.content.Context
import android.util.Log
import java.util.Locale

object KeywordProfileBuilder {

    private const val PREF_KEYWORDS = "AD_KEYWORDS"
    private const val PREF_KEYWORDS_DATE = "AD_KEYWORDS_DATE"
    private const val REFRESH_DAYS = 7

    fun getCachedOrFreshKeywords(context: Context): List<String> {
        val prefs = context.getSharedPreferences("BassAnglerTrackerPrefs", Context.MODE_PRIVATE)

        val lastUpdated = prefs.getLong(PREF_KEYWORDS_DATE, 0L)
        val now = System.currentTimeMillis()

        val oneWeekMillis = 1000L * 60 * 60 * 24 * REFRESH_DAYS

        if (now - lastUpdated < oneWeekMillis) {
            val cached = prefs.getStringSet(PREF_KEYWORDS, null)
            if (!cached.isNullOrEmpty()) {
                Log.d("KeywordProfile", "🔁 Using cached Ad Keywords: $cached")
                return cached.toList()
            }
        }

        val fresh = buildKeywords(context)
        prefs.edit()
            .putStringSet(PREF_KEYWORDS, fresh.toSet())
            .putLong(PREF_KEYWORDS_DATE, now)
            .apply()

        Log.d("KeywordProfile", "🌱 Built new Ad Keywords: $fresh")
        return fresh
    }

    private fun buildKeywords(context: Context): List<String> {
        val dbHelper = CatchDatabaseHelper(context)
        val catches = dbHelper.getAllCatchesExcludingPractice()
        if (catches.isEmpty()) return emptyList()

        val keywordSet = mutableSetOf<String>()

        // 1. Most frequent species
        val topSpecies = catches
            .groupingBy { it.species.lowercase(Locale.getDefault()) }
            .eachCount()
            .maxByOrNull { it.value }?.key

        topSpecies?.let {
            keywordSet.add(it)
            keywordSet.add("$it fishing")
        }

        // 2. Mode preference (Fun Day or Tournament)
        val modeCounts = catches.groupingBy { it.catchType.lowercase() }.eachCount()
        val dominantMode = modeCounts.maxByOrNull { it.value }?.key ?: "fun"
        keywordSet.add(dominantMode)
        if (dominantMode.contains("tourn")) keywordSet.add("tournament fishing")

        // 3. Unit type
        val unitKeyword = when {
            catches.any { it.totalWeightOz != null && it.totalWeightOz > 0 } -> "lbs oz"
            catches.any { it.totalWeightHundredthKg != null && it.totalWeightHundredthKg > 0 } -> "kg"
            catches.any { it.totalLengthQuarters != null && it.totalLengthQuarters > 0 } -> "inches"
            catches.any { it.totalLengthTenths != null && it.totalLengthTenths > 0 } -> "centimeters"
            else -> null
        }
        unitKeyword?.let { keywordSet.add(it) }

        // 4. Time-of-year / Season
        val monthCounts = catches.mapNotNull {
            it.dateTime.takeIf { dt -> dt.length >= 7 }?.substring(5, 7)
        }
        val mostCommonMonth = monthCounts.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        val season = getSeasonForMonth(mostCommonMonth)
        keywordSet.add(season)

        // 5. Region (integrate RegionClassifier)
        val latestCatchWithLocation = catches.lastOrNull { it.latitude != 0.0 && it.longitude != 0.0 }
        latestCatchWithLocation?.let { catch ->
            val region = RegionClassifier.getRegionFromLocation(
                country = "Canada", // ⬅ Replace with real country lookup if you geocode
                admin = "Ontario",  // ⬅ Replace with Geocoder or saved mapping later
                subAdmin = "Leeds"  // ⬅ Replace with Geocoder or region tag
            )
            keywordSet.add(region)
        }

        Log.d("KeywordProfile", "Generated Keywords: $keywordSet")
        return keywordSet.toList()
    }

    private fun getSeasonForMonth(monthStr: String?): String {
        return when (monthStr?.toIntOrNull()) {
            in 3..5 -> "spring"
            in 6..8 -> "summer"
            in 9..11 -> "fall"
            12, 1, 2 -> "winter"
            else -> "fishing season"
        }
    }
}
