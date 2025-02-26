package com.bramestorm.bassanglertracker

data class CatchItem(
    val id: Int,
    val dateTime: String,
    val species: String,
    val weightLbs: Int?,
    val weightOz: Int?,
    val weightDecimal: Float?,
    val lengthA8th: Int?,
    val lengthInches: Int?,
    val lengthDecimal: Float?,
    val catchType: String,
    val markerType: String? = null
)

