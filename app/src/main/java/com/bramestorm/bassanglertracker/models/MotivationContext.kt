package com.bramestorm.bassanglertracker.models

// ---------- FOR MOTIVATIONAL MESSAGES ---------------------
data class MotivationContext(
    val currentCount: Int,
    val totalNeeded: Int,
    val timeSinceLastCatchMillis: Long,
    val comparisonValue: Int,
    val smallestComparisonValue: Int,
    val isNewBiggestOfDay: Boolean
)


