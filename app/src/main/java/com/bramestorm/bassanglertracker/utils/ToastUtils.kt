package com.bramestorm.bassanglertracker.util

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.bramestorm.bassanglertracker.R

fun Context.positionedToast(
    message: String,
    gravity: Int = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL,
    xOffset: Int = 0,
    yOffsetDp: Int = 0
) {
    val inflater = LayoutInflater.from(this)
    val layout = inflater.inflate(
        R.layout.custom_toast,
        null
    )
    layout.findViewById<TextView>(R.id.toast_text).text = message

    Toast(this).apply {
        duration = Toast.LENGTH_LONG
        view = layout
        // convert dp to px
        val yOffsetPx = (yOffsetDp * resources.displayMetrics.density).toInt()
        setGravity(gravity, xOffset, yOffsetPx)
    }.show()
}
