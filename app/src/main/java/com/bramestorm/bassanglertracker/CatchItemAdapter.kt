package com.bramestorm.bassanglertracker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class CatchItemAdapter(context: Context, private val catches: MutableList<CatchItem>) :
    ArrayAdapter<CatchItem>(context, 0, catches) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Inflate view if not already provided.
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        val catchItem = getItem(position)
        // Format the catch item as desired.
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = catchItem?.let {
            when (it.catchType) {
                "weight_imperial" -> "${it.species}: ${it.weightLbs} lbs, ${it.weightOz} oz  ${it.weightDecimal} lbs"
                "weight_metric" -> "${it.species}: ${it.weightDecimal} kg"
                "length_imperial" -> " ${it.species}:  ${it.lengthDecimal} in"
                "length_metric" -> "${it.species}: ${it.lengthDecimal} cm"
                else -> it.toString()
            }
        } ?: ""
        return view
    }
}
