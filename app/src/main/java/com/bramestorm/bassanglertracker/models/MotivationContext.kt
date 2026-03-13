package com.bramestorm.bassanglertracker.models

data class MotivationContext(
    val currentCount: Int,
    val totalNeeded: Int,
    val timeSinceLastCatchMillis: Long,
    val comparisonValue: Int,
    val smallestComparisonValue: Int,
    val isNewBiggestOfDay: Boolean,
    val isCullUpgrade: Boolean = false,
    val isTooSmall: Boolean = false,
    val catchesInLast15Min: Int = 0
)


