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
    catches: MutableList<CatchItem>,
) : ArrayAdapter<CatchItem>(context, 0, catches)
{

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_catch, parent, false)

        val catchItem = getItem(position)
        val txtCatchInfo = view.findViewById<TextView>(R.id.txtSpeciesNameListItem)
        val imgSpecies = view.findViewById<ImageView>(R.id.imgSpeciesListItem)
        val imgGpsPin = view.findViewById<ImageView>(R.id.imgGpsPin)

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
                    "$speciesName: ${formatWeightOzToLbsOz(totalOz)}, @ $timeFormatted"
                }

                "pounds" -> {
                    val hundredthLbs = it.totalWeightHundredthPounds ?: 0
                    "$speciesName: ${formatWeightPounds(context, hundredthLbs)}, @ $timeFormatted"
                }

                "kgs" -> {
                    val hundredthKg = it.totalWeightHundredthKg ?: 0
                    "$speciesName: ${formatWeightKg(context, hundredthKg)}, @ $timeFormatted"
                }

                "inches" -> {
                    val quarters = it.totalLengthQuarters ?: 0
                    "$speciesName: ${formatLengthQuartersToInches(quarters)}, @ $timeFormatted"
                }

                "metric" -> {
                    val tenthCm = it.totalLengthTenths ?: 0
                    "$speciesName: ${formatLengthCm(context, tenthCm)}, @ $timeFormatted"
                }

                else -> it.toString()
            }


            txtCatchInfo.text = infoText
            imgSpecies.setImageResource(SpeciesImageHelper.getSpeciesImageResId(speciesName))

            // 🧭 Show pin icon if lat/lng is available
            if (it.latitude != 0.0 && it.longitude != 0.0) {
                imgGpsPin.setImageResource(R.drawable.icon_pin)
                imgGpsPin.visibility = View.VISIBLE
            } else {
                imgGpsPin.visibility = View.GONE
            }
        }

        return view
    }//------- END of getView -------------------

}//----------- END ---------------------
