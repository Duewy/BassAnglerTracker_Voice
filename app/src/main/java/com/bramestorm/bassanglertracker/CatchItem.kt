package com.bramestorm.bassanglertracker

data class CatchItem(
    val id: Int,
    val dateTime: String,
    val species: String,

    // Weight (for different units)
    val weightLbs: Int? = null,
    val weightOz: Int? = null,
    val weightDecimal: Float? = null, // Lbs (decimal) or Kgs
    val weightKgs: Double? = null,
    val weightTotal: Double? = null,

    // Length (for different units)
    val lengthInches: Int? = null,
    val length8th: Int? = null,
    val lengthInDec: Float?= null, // Inches (decimal) or cm


    val lengthCentimeters: Int? = null,
    val lengthCmDec: Int? = null,
    val lengthTotalCm: Float? = null
) {
    override fun toString(): String {
        // Smart weight formatting
        val weightString = when {
            weightLbs != null && weightOz != null -> "$weightLbs lbs $weightOz oz"
            weightDecimal != null -> "${String.format("%.2f", weightDecimal)} lbs"
            weightKgs != null -> "${String.format("%.2f", weightKgs)} kg"
            else -> "N/A"
        }

        // Smart length formatting
        val lengthString = when {
            lengthInches != null && length8th != null -> "${
                String.format(
                    "%.3f",
                    lengthInches + (length8th / 8.0)
                )
            } inches"

            lengthTotalCm != null -> "${String.format("%.2f", lengthTotalCm)} cm"
            else -> "N/A"
        }

        return "$species - Weight: $weightString | Length: $lengthString | Date: $dateTime"
    }
}

