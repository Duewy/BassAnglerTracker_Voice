package com.bramestorm.bassanglertracker

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatDialog

class PopupWeightEntry(context: Context, private val onWeightEntered: (Int, Int, String) -> Unit) :

    AppCompatDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_weight_entry)

        val editWeightLbs = findViewById<EditText>(R.id.editWeightLbs)
        val editWeightOz = findViewById<EditText>(R.id.editWeightOz)
        val radioGroupBassType = findViewById<RadioGroup>(R.id.radioGroupBassType)
        val btnSaveWeight = findViewById<Button>(R.id.btnSaveWeight)

        btnSaveWeight?.setOnClickListener {
            val weightLbs = editWeightLbs?.text.toString().toIntOrNull() ?: 0
            val weightOz = editWeightOz?.text.toString().toIntOrNull() ?: 0

            // ✅ Ensure ounces do not exceed 15
            if (weightOz > 15) {
                Toast.makeText(context, "Ounces cannot exceed 15!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Convert to total ounces for storage
            val totalWeightOz = (weightLbs * 16) + weightOz


            if (weightLbs == 0 && weightOz == 0) {
                Toast.makeText(context, "Enter a valid weight!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedBassType = when (radioGroupBassType?.checkedRadioButtonId) {
                R.id.radioLargeMouth -> "Large Mouth Bass"
                R.id.radioSmallMouth -> "Small Mouth Bass"
                else -> "Bass"
            }

            // ✅ Send weightLbs and weightOz separately
            onWeightEntered(weightLbs, weightOz, selectedBassType)


            dismiss()
        }
    }
}
