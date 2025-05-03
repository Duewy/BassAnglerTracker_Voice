package com.bramestorm.bassanglertracker.training

data class PracticePhrase(
                    // Sets up the Variables for the VCC to list and log failures and correct the interactions
    val text: String,
    var isMastered: Boolean = false,
    var successCount: Int = 0,
    var failureCount: Int = 0,
    var recentFailures: Int = 0,
    var lastMisheardInput: String? = null,
    val skipSuggestionsFor: MutableSet<String> = mutableSetOf()
)
