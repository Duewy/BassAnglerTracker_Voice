package com.bramestorm.bassanglertracker
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView


class ClipColorSpinnerAdapter(
    private val context: Context,
    private val colorList: List<String> // Color names
) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, colorList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val colorName = colorList[position]

        textView.text = colorName
        textView.setTextColor(getColorFromName(colorName)) // ✅ Set text color

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val colorName = colorList[position]

        textView.text = colorName
        textView.setTextColor(getColorFromName(colorName)) // ✅ Set text color

        return view
    }

    private fun getColorFromName(colorName: String): Int {
        return when (colorName.uppercase()) {
            "RED" -> context.getColor(R.color.clip_red)
            "YELLOW" -> context.getColor(R.color.clip_yellow)
            "GREEN" -> context.getColor(R.color.clip_green)
            "BLUE" -> context.getColor(R.color.clip_blue)
            "WHITE" -> context.getColor(R.color.clip_white)
            "ORANGE" -> context.getColor(R.color.clip_orange)
            else -> context.getColor(R.color.black) // Default
        }
    }
}
