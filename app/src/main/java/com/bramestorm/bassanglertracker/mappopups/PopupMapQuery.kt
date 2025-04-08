package com.bramestorm.bassanglertracker.mappopups

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.models.MapQueryFilters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PopupMapQuery(
    private val context: Context,
    private val onFiltersSelected: (MapQueryFilters) -> Unit) {

    private lateinit var dialog: AlertDialog

    fun showPopup() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.popup_map_query, null)

        // Get all buttons and textviews
        val btnDateMapQuery = view.findViewById<Button>(R.id.btnDateMapQuery)
        val txtDatesMapQuery = view.findViewById<TextView>(R.id.txtDatesMapQuery)

        val btnSpeciesMapQuery = view.findViewById<Button>(R.id.btnSpeciesMapQuery)
        val txtSpeciesMapQuery = view.findViewById<TextView>(R.id.txtSpeciesMapQuery)

        val btnTypeOfDayMapQuery = view.findViewById<Button>(R.id.btnTypeOfDayMapQuery)
        val txtTypeofDayMapQuery = view.findViewById<TextView>(R.id.txtTypeofDayMapQuery)


        val btnSizesMapQuery = view.findViewById<Button>(R.id.btnSizesMapQuery)
        val txtSizesMapQuery = view.findViewById<TextView>(R.id.txtSizesOfMapQuery)

        val btnMeasurementMapQuery = view.findViewById<Button>(R.id.btnMeasurementMapQuery)
        val txtMeasurementTypeMapQuery = view.findViewById<TextView>(R.id.txtMeasurementTypeMapQuery)

        val btnGetMap = view.findViewById<Button>(R.id.btnGetQueryMap)
        val btnResetFilters= view.findViewById<Button>(R.id.btnResetFilters)
        val btnMainMenu = view.findViewById<Button>(R.id.btnMainPgMapQuery)

        // Show dialog
        dialog = AlertDialog.Builder(context).setView(view).create()
        dialog.show()


        btnDateMapQuery.setOnClickListener {
            PopupQueryDate(context) { selectedDateRange ->
                txtDatesMapQuery.text = selectedDateRange
            }.showPopup()
        }

        btnSpeciesMapQuery.setOnClickListener {
            PopupQuerySpecies(context) { selectedSpecies ->
                txtSpeciesMapQuery.text = selectedSpecies
            }.showPopup()
        }

        btnTypeOfDayMapQuery.setOnClickListener {
            PopupQueryEventType(context) { selectedEvent ->
                txtTypeofDayMapQuery.text = selectedEvent
            }.showPopup()
        }


        btnSizesMapQuery.setOnClickListener {
            PopupQuerySize(context) { type, range ->
                txtSizesMapQuery.text = "$type: $range"
            }.showPopup()
        }

        btnMeasurementMapQuery.setOnClickListener {
            PopupQueryMeasurement(context) { selectedMeasurement ->
                txtMeasurementTypeMapQuery.text = selectedMeasurement
            }.showPopup()
        }

        btnGetMap.setOnClickListener {
            val dateRange = txtDatesMapQuery.text.toString().ifBlank {
                "${getFirstDayOfMonth()} to ${getTodayAsString()}"
            }

            val species = txtSpeciesMapQuery.text.toString().ifBlank { "All" }
            val eventType = txtTypeofDayMapQuery.text.toString().ifBlank { "Both" }
            val sizeRangeText = txtSizesMapQuery.text.toString().ifBlank { "0 - 9999" }

            val sizeType = if (sizeRangeText.contains("Length", true)) "Length" else "Weight"
            val sizeRange = sizeRangeText.substringAfter(":").trim().ifBlank { "0 - 9999" }

            val measurementType = txtMeasurementTypeMapQuery.text.toString().ifBlank {
                "Imperial (lbs/oz, inches)"
            }

            val filters = MapQueryFilters(
                dateRange = dateRange,
                species = species,
                eventType = eventType,
                sizeType = sizeType,
                sizeRange = sizeRange,
                measurementType = measurementType
            )

            dialog.dismiss()
            onFiltersSelected(filters)
        }

        btnResetFilters.setOnClickListener {
            txtDatesMapQuery.text = "${getFirstDayOfMonth()} to ${getTodayAsString()}"
            txtSpeciesMapQuery.text = "All"
            txtTypeofDayMapQuery.text = "Both"
            txtSizesMapQuery.text = "Weight: 0 - 9999"
            txtMeasurementTypeMapQuery.text = "Imperial (lbs/oz, inches)"
        }

        btnMainMenu.setOnClickListener {
            dialog.dismiss()
        }
    }//--------------- END PopupMapQuery  -----------------------------

    private fun getFirstDayOfMonth(): String {
        val sdf = SimpleDateFormat("yyyy-MM-01", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getTodayAsString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

}
