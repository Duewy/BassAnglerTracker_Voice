package com.bramestorm.bassanglertracker

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatDialog


class PopupWeightEntry(
    context: Context,
    private val onWeightEntered: (Int, Int, String) -> Unit
) : AppCompatDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_weight_entry)

        val editWeightLbs = findViewById<EditText>(R.id.editWeightLbs)
        val editWeightOz = findViewById<EditText>(R.id.editWeightOz)
        val radioLargeMouth = findViewById<RadioButton>(R.id.radioLargeMouth)
        val radioSmallMouth = findViewById<RadioButton>(R.id.radioSmallMouth)
        val btnSaveWeight = findViewById<Button>(R.id.btnSaveWeight)

        btnSaveWeight?.setOnClickListener {
            val weightLbs = editWeightLbs?.text.toString().toIntOrNull() ?: 0
            val weightOz = editWeightOz?.text.toString().toIntOrNull() ?: 0

            if (weightOz > 15) {
                Toast.makeText(context, "Ounces cannot exceed 15!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (weightLbs == 0 && weightOz == 0) {
                Toast.makeText(context, "Enter a valid weight!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Get species from radio buttons
            val selectedSpecies = when {
                radioLargeMouth?.isChecked == true -> "L"
                radioSmallMouth?.isChecked == true -> "S"
                else -> "B" // Default to "B" for general bass if neither is selected
            }

            // ✅ Pass individual Lbs and Oz (not totalWeightOz)
            onWeightEntered(weightLbs, weightOz, selectedSpecies)

            dismiss()
        }
    }
}
