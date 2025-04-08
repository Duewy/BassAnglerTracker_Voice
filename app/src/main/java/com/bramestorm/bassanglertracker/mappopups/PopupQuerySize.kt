package com.bramestorm.bassanglertracker.mappopups

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bramestorm.bassanglertracker.R

class PopupQuerySize(
    private val context: Context,
    private val onSizeRangeSelected: (String, String) -> Unit  // measurementType, sizeRange
) {
    fun showPopup() {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_query_size, null)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupMeasurement)
        val radioWeight = view.findViewById<RadioButton>(R.id.radioWeight)
        val radioLength = view.findViewById<RadioButton>(R.id.radioLength)
        val edtMin = view.findViewById<EditText>(R.id.edtMinSize)
        val edtMax = view.findViewById<EditText>(R.id.edtMaxSize)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmSizeQuery)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelSizeQuery)

        val dialog = AlertDialog.Builder(context).setView(view).create()

        // Default hint
        edtMin.hint = "Min Size"
        edtMax.hint = "Max Size"

        // Update hints on toggle
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioWeight -> {
                    edtMin.hint = "Min Weight (lbs or kg)"
                    edtMax.hint = "Max Weight (lbs or kg)"
                }
                R.id.radioLength -> {
                    edtMin.hint = "Min Length (in or cm)"
                    edtMax.hint = "Max Length (in or cm)"
                }
            }
        }

        btnConfirm.setOnClickListener {
            val min = edtMin.text.toString().trim()
            val max = edtMax.text.toString().trim()
            val selectedType = when (radioGroup.checkedRadioButtonId) {
                R.id.radioWeight -> "Weight"
                R.id.radioLength -> "Length"
                else -> ""
            }

            if (selectedType.isEmpty()) {
                Toast.makeText(context, "Please select a measurement type.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (min.isEmpty() || max.isEmpty()) {
                Toast.makeText(context, "Please enter both min and max.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            onSizeRangeSelected(selectedType, "$min - $max")
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
