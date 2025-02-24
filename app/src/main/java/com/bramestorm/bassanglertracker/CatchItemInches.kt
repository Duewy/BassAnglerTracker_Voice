package com.bramestorm.bassanglertracker

data class CatchItemInches(
    val id: Int,
    val dateTime: String,
    val species: String,
    val length8th: Int,
    val lengthInches: Int,
    val lengthInDec: Float  // Ensure it's Float, not Int!
) {
    override fun toString(): String {
        return "$species - ${String.format("%.3f", lengthInches + (length8th / 8.0))} inches | Date: $dateTime"
    }
}
