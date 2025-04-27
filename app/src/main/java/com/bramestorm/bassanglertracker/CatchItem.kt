package com.bramestorm.bassanglertracker

import android.content.Context

data class CatchItem(

    val id: Int,
    val dateTime: String,
    val longitude:Double? = null,
    val latitude:Double? = null,
    val species: String,

    // ✅ Whole Number Storage Approach

    val totalWeightOz: Int?,            // Store lbs/oz as total ounces
    val totalWeightHundredthKg :Int?,   // Store Kgs 0.00 as hundredths of Kg
    val totalLengthQuarters: Int?,          // Store inches & 4ths
    val totalLengthTenths: Int?,         // Store cm as tenths (e.g., 45.6cm → stored as 456)

    val catchType: String,              // Sort Catch Log with catchType
    val markerType: String? = null,     // # of Tournament fish to set Culling Limits
    val clipColor: String? = null       // color for Tournament clips
)

//------------- for MOTIVATIONAL MESSAGES ----------------------
fun CatchItem.getComparisonValueByMode(mode: String): Float {
    return when (mode.lowercase()) {
        "lbs" -> (this.totalWeightOz ?: 0).toFloat()
        "kgs" -> (this.totalWeightHundredthKg ?: 0) / 100f
        "inches" -> (this.totalLengthQuarters ?: 0) / 8f
        "cms" -> (this.totalLengthTenths ?: 0) / 10f
        else -> 0f
    }
}



fun formatWeightOzToLbsOz(totalOz: Int): String {
    val lbs = totalOz / 16
    val oz = totalOz % 16
    return "$lbs lbs $oz oz"
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

fun formatWeightKg(context: Context, hundredthKg: Int): String {
    val kg = hundredthKg / 100.0
    return context.getString(R.string.weight_format_kg, kg)
}

fun formatLengthCm(context: Context, tenthCm: Int): String {
    val cm = tenthCm / 10.0
    return context.getString(R.string.length_format_cm, cm)
}

