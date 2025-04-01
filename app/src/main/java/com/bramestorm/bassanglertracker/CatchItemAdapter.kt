package com.bramestorm.bassanglertracker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatchItemAdapter(
    context: Context,
    private val catches: MutableList<CatchItem>,
) : ArrayAdapter<CatchItem>(context, 0, catches) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_catch, parent, false)

        val catchItem = getItem(position)
        val txtCatchInfo = view.findViewById<TextView>(R.id.txtCatchInfo)
        val imgSpecies = view.findViewById<ImageView>(R.id.imgSpecies)

        // Format time from dateTime string
        val timeFormatted = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val parsedDate = inputFormat.parse(catchItem?.dateTime ?: "")
            outputFormat.format(parsedDate ?: Date())
        } catch (e: Exception) {
            "N/A"
        }

        catchItem?.let {
            val speciesName = it.species ?: "Unknown"
            val infoText = when (it.catchType) {
                "lbsOzs" -> {
                    val totalOz = it.totalWeightOz ?: 0
                    "$speciesName: ${totalOz / 16}Lbs ${totalOz % 16}oz, @ $timeFormatted"
                }
                "kgs" -> "$speciesName: ${it.totalWeightHundredthKg?.div(100.0) ?: 0.0} Kg, @ $timeFormatted"
                "inches" -> {
                    val totalA8th = it.totalLengthA8th ?: 0
                    val inches = totalA8th / 8
                    val a8ths = totalA8th % 8
                    val lengthFormatted = when (a8ths) {
                        0 -> "$inches in"
                        2 -> "$inches 1/4 in"
                        4 -> "$inches 1/2 in"
                        6 -> "$inches 3/4 in"
                        else -> "$inches ${a8ths}/8 in"
                    }
                    "$speciesName: $lengthFormatted, @ $timeFormatted"
                }
                "metric" -> "$speciesName: ${it.totalLengthTenths?.div(10.0) ?: 0.0} cm, @ $timeFormatted"
                else -> it.toString()
            }

            txtCatchInfo.text = infoText
            imgSpecies.setImageResource(SpeciesImageHelper.getSpeciesImageResId(speciesName))
        }

        return view
    }
}
