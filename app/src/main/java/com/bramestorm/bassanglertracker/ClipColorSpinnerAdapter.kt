package com.bramestorm.bassanglertracker
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView


class ClipColorSpinnerAdapter(
    private val context: Context,
    private val colorList: List<String>
) : ArrayAdapter<String>(context, R.layout.spinner_color_item, colorList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createColorView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createColorView(position, convertView, parent)
    }

    private fun createColorView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as android.view.LayoutInflater
        val view = inflater.inflate(R.layout.spinner_color_item, parent, false)

        val colorNameView = view.findViewById<TextView>(R.id.spinnerColorName)
        val colorPatchView = view.findViewById<View>(R.id.spinnerColorBox)

        val colorName = colorList[position]
        colorNameView.text = colorName
        colorPatchView.setBackgroundColor(getColorFromName(colorName))

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
            else -> context.getColor(R.color.black)
        }
    }
}
