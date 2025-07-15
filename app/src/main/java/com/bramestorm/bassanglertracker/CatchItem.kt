package com.bramestorm.bassanglertracker

import android.content.Context

data class CatchItem(

    val id: Int,
    val dateTime: String,
    val longitude:Double? = null,
    val latitude:Double? = null,
    val species: String,

    // ✅ Whole Number Storage Approach to avoid Floats, Doubles or other format issues

    val totalWeightOz: Int?,                // Store lbs/oz as total ounces
    val totalWeightHundredthPounds: Int?,   // Store Pounds 0.00 as total hundredths of Pounds
    val totalWeightHundredthKg :Int?,       // Store Kgs 0.00 as hundredths of Kg
    val totalLengthQuarters: Int?,          // Store inches & 4ths
    val totalLengthTenths: Int?,            // Store cm as tenths (e.g., 45.6cm → stored as 456)

    val catchType: String,              // Sort Catch Log with catchType
    val markerType: String? = null,     // # of Tournament fish to set Culling Limits
    val clipColor: String? = null       // color for Tournament clips
)

    // Modes for the TournamentVoiceFeedback to find which one to use
    enum class MeasurementMode {
        LBS_OZ,
        POUNDS,
        KG,
        INCHES,
        CM
    }

fun CatchItem.getMeasurementMode(): MeasurementMode? {
    return when {
        totalWeightOz != null -> MeasurementMode.LBS_OZ
        totalWeightHundredthPounds !=null -> MeasurementMode.POUNDS
        totalWeightHundredthKg != null -> MeasurementMode.KG
        totalLengthQuarters != null -> MeasurementMode.INCHES
        totalLengthTenths != null -> MeasurementMode.CM
        else -> null
    }
}


//------------- for MOTIVATIONAL MESSAGES ----------------------
fun CatchItem.getComparisonValueByMode(mode: MeasurementMode): Int {
    return when (mode) {
        MeasurementMode.LBS_OZ -> totalWeightOz ?: 0
        MeasurementMode.POUNDS -> totalWeightHundredthPounds ?: 0
        MeasurementMode.KG     -> totalWeightHundredthKg ?: 0
        MeasurementMode.INCHES -> totalLengthQuarters ?: 0
        MeasurementMode.CM     -> totalLengthTenths ?: 0
    }
}


//======= FORMAT THE MEASUREMENT UNITS  ===============================

fun formatWeightOzToLbsOz(totalOz: Int): String {
    val lbs = totalOz / 16
    val oz = totalOz % 16
    return "$lbs lbs $oz oz"
}

fun formatWeightPounds(context: Context, hundredthLbs: Int): String {
    val pounds = hundredthLbs / 100.0
    return context.getString(R.string.weight_format_pounds, pounds)
}

fun formatWeightKg(context: Context, hundredthKg: Int): String {
    val kg = hundredthKg / 100.0
    return context.getString(R.string.weight_format_kg, kg)
}


fun formatLengthQuartersToInches(lengthQuarters: Int): String {
    val inches = lengthQuarters / 4
    val quarters = lengthQuarters % 4
    return if (quarters == 0) {
        "$inches in"
    } else {
        "$inches ${quarters}/4 in"
    }
}

fun formatLengthCm(context: Context, tenthCm: Int): String {
    val cm = tenthCm / 10.0
    return context.getString(R.string.length_format_cm, cm)
}

