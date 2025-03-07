package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class PopupLengthEntryMetric : Activity() {

        private lateinit var edtWeightKgs: EditText
        private lateinit var edtWeightGrams: EditText
        private lateinit var btnSaveWeight: Button
        private lateinit var btnCancel: Button
        private lateinit var txtPopupTitle: TextView
        private var selectedSpecies: String = ""

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.popup_weight_entry_metric) // NEW XML Layout

            edtWeightKgs = findViewById(R.id.edtWeightCms)
            edtWeightGrams = findViewById(R.id.edtWeightDec)
            btnSaveWeight = findViewById(R.id.btnSaveWeight)
            btnCancel = findViewById(R.id.btnCancel)

            selectedSpecies = intent.getStringExtra("selectedSpecies") ?: ""

            btnSaveWeight.setOnClickListener {
                val resultIntent = Intent()

                val weightKgs = edtWeightKgs.text.toString().toIntOrNull() ?: 0
                val weightGrams = edtWeightGrams.text.toString().toIntOrNull() ?: 0
                val totalWeightGrams = (weightKgs * 1000) + weightGrams // Convert to total grams

                Log.d("PopupWeightEntryKgs", "Weight Entered: $weightKgs kgs, $weightGrams grams")
                Log.d("PopupWeightEntryKgs", "Total Weight in Grams: $totalWeightGrams") // Debugging Log

                resultIntent.putExtra("weightTotalGrams", totalWeightGrams)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }


            btnCancel.setOnClickListener {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }