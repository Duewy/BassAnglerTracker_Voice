package com.bramestorm.bassanglertracker

data class CatchItem(
    val id: Int,
    val dateTime: String,
    val species: String,

    // ✅ Whole Number Storage Approach
    val totalWeightOz: Int?, // Store lbs/oz as total ounces
    val totalLengthA8th: Int?, // Store inches & 8ths as total eighths

    // ✅ Metric Storage
    val weightDecimalTenthKg: Int?, // Store kg as tenths (e.g., 2.34kg → stored as 234)
    val lengthDecimalTenthCm: Int?, // Store cm as tenths (e.g., 45.6cm → stored as 456)

    val catchType: String,
    val markerType: String? = null,

    val clipColor: String = "clip_grey"
)
