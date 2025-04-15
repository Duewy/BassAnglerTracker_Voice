package com.bramestorm.bassanglertracker.training

data class PracticePhrase(

    val text: String,
    var isMastered: Boolean = false,
    var successCount: Int = 0,
    var failureCount: Int = 0
)
