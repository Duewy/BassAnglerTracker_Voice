package com.bramestorm.bassanglertracker

data class CatchItemKgs(
    val id: Int,
    val dateTime: String,
    val species: String,
    val weightKgs: Double
) {
    override fun toString(): String {
        return "$species - $weightKgs Kgs - $dateTime"
    }
}
