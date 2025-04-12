package com.bramestorm.bassanglertracker.models

// ---------- FOR MOTIVATIONAL MESSAGES ---------------------
data class MotivationContext(
    val currentCount: Int,
    val totalNeeded: Int,
    val timeSinceLastCatchMillis: Long,
    val comparisonValue: Float,
    val smallestComparisonValue: Float,
    val isNewBiggestOfDay: Boolean
)


