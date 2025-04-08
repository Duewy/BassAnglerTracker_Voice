package com.bramestorm.bassanglertracker.models

data class MapQueryFilters(
    val dateRange: String,
    val species: String,
    val eventType: String,
    val sizeType: String,         // "Weight" or "Length"
    val sizeRange: String,        // e.g., "1.0 - 5.5"
    val measurementType: String   // "Imperial..." or "Metric..."
)
