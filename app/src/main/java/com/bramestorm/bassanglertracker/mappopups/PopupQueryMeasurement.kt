package com.bramestorm.bassanglertracker.mappopups

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bramestorm.bassanglertracker.R

class PopupQueryMeasurement(
    private val context: Context,
    private val onMeasurementSelected: (String) -> Unit
) {
    fun showPopup() {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_query_measurement, null)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupMeasurementType)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmMeasurement)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelMeasurement)

        val dialog = AlertDialog.Builder(context).setView(view).create()

        btnConfirm.setOnClickListener {
            val selected = when (radioGroup.checkedRadioButtonId) {
                R.id.radioImperialLbsOz -> "Imperial (lbs/oz, inches)"
                R.id.radioMetric -> "Metric (kg, cm)"
                else -> "Not selected"
            }

            if (selected == "Not selected") {
                Toast.makeText(context, "Please select a measurement type.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            onMeasurementSelected(selected)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
