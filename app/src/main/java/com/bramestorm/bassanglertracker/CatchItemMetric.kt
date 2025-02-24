package com.bramestorm.bassanglertracker

data class CatchItemMetric(
    val id: Int,
    val dateTime: String,
    val species: String,
    val lengthDecimal: Float
    ) {
    override fun toString(): String {
        return "$species - $lengthDecimal cm - $dateTime"
    }
}

