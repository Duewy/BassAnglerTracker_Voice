package com.bramestorm.bassanglertracker

data class CatchItem(

    val id: Int,
    val dateTime: String,
    val longitude:Double? = null,
    val latitude:Double? = null,
    val species: String,

    // ✅ Whole Number Storage Approach

    val totalWeightOz: Int?,            // Store lbs/oz as total ounces
    val totalWeightHundredthKg :Int?,   // Store Kgs 0.00 as hundredths of Kg
    val totalLengthA8th: Int?,          // Store inches & 8ths as total eighths
    val totalLengthTenths: Int?,         // Store cm as tenths (e.g., 45.6cm → stored as 456)

    val catchType: String,              // Sort Catch Log with catchType
    val markerType: String? = null,     // # of Tournament fish to set Culling Limits
    val clipColor: String? = null       // color for Tournament clips
)


fun formatWeightOzToLbsOz(totalOz: Int): String {
    val lbs = totalOz / 16
    val oz = totalOz % 16
    return "${lbs} lbs ${oz} oz"
}

fun formatLengthA8thToInches(lengthA8ths: Int): String {
    val inches = lengthA8ths / 8
    val eighths = lengthA8ths % 8
    return if (eighths == 0) {
        "$inches in"
    } else {
        "$inches ${eighths}/8 in"
    }
}

fun formatWeightKg(hundredthKg: Int): String {
    val kg = hundredthKg / 100.0
    return String.format("%.2f kg", kg)
}

fun formatLengthCm(tenthCm: Int): String {
    val cm = tenthCm / 10.0
    return String.format("%.1f cm", cm)
}
