package com.bramestorm.bassanglertracker.voice

import android.content.Context
import com.bramestorm.bassanglertracker.CatchItem
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.voice.VoiceParser.ParsedCatch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for converting a ParsedCatch into a CatchItem and saving it to the database.
 */
object CatchSaver {
    /**
     * Saves the parsed catch data under the given catchType (e.g., "Fun Day", "Tournament").
     */
    fun save(
        context: Context,
        parsed: ParsedCatch,
        catchType: String
    ) {
        // Convert imperial weight to total ounces
        val totalOz = ((parsed.weightLbs ?: 0) * 16) + (parsed.weightOz ?: 0)

        // Convert Pounds + decimal to hundredth-Pounds integer
        val totalHundredthPounds = ((parsed.weightPounds ?: 0) * 100) + ((parsed.weightDec ?: 0) / 10)

        // Convert kg + grams to hundredth-kg integer
        val totalHundredthKg = ((parsed.weightKgWhole ?: 0) * 100) + ((parsed.weightGrams ?: 0) / 10)

        // Convert inches + quarters to total quarters
        val totalQuarters = ((parsed.lengthInches ?: 0) * 4) + (parsed.lengthQuarters ?: 0)

        // Convert cm + tenths to total tenths
        val totalTenths = ((parsed.lengthCm ?: 0) * 10) + (parsed.lengthTenths ?: 0)

        // Build the CatchItem
        val item = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = parsed.species ?: "Unknown",
            totalWeightOz = totalOz,
            totalWeightHundredthPounds = totalHundredthPounds,
            totalLengthQuarters = totalQuarters,
            totalLengthTenths = totalTenths,
            totalWeightHundredthKg = totalHundredthKg,
            catchType = catchType,
            markerType = parsed.clipColor ?: "",
            clipColor = parsed.clipColor ?: "",
            latitude = 0.0,
            longitude = 0.0
        )

        // Insert into the database
        CatchDatabaseHelper(context).insertCatch(item)
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
