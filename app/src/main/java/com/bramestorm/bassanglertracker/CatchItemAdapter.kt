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
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        val catchItem = getItem(position)
        val textView = view.findViewById<TextView>(android.R.id.text1)

        textView.text = catchItem?.let {
            when (it.catchType) {
                "weight_imperial" -> {
                    val totalOz = it.totalWeightOz ?: 0
                    "${it.species}: ${totalOz / 16} lbs, ${totalOz % 16} oz"
                }
                "weight_metric" -> "${it.species}: ${it.weightDecimalTenthKg?.div(10.0) ?: 0.0} kg"
                "length_imperial" -> {
                    val totalA8th = it.totalLengthA8th ?: 0
                    "${it.species}: ${totalA8th / 8} in ${totalA8th % 8}/8"
                }
                "length_metric" -> "${it.species}: ${it.lengthDecimalTenthCm?.div(10.0) ?: 0.0} cm"
                else -> it.toString()
            }
        } ?: ""
        return view
    }
}
