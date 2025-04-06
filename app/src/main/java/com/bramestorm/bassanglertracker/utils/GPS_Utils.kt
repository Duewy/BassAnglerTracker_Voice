package com.bramestorm.bassanglertracker.utils

import android.content.Context
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.R

object GpsUtils {
    fun updateGpsStatusLabel(textView: TextView, context: Context) {
        val isEnabled = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getBoolean("GPS_ENABLED", false)

        textView.text = context.getString(if (isEnabled) R.string.gps_on else R.string.gps_off)
        textView.setTextColor(
            ContextCompat.getColor(
                context,
                if (isEnabled) R.color.clip_blue else R.color.clip_red
            )
        )
    }
}
