package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Button
import android.widget.Toast
import android.widget.EditText
import android.widget.TextView
import android.text.InputFilter
import android.text.Spanned
import android.os.Handler
import android.os.Looper
import android.os.Bundle

class PopupLengthEntryInches : Activity() {

    private lateinit var edtWeightInches: EditText
    private lateinit var edtWeight8ths: EditText
    private lateinit var btnSaveWeight: Button
    private lateinit var btnCancel: Button
    private var selectedSpecies: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_weight_entry_inches) // NEW XML Layout

        edtWeightInches = findViewById(R.id.edtWeightInches)
        edtWeight8ths = findViewById(R.id.edtWeight8ths)
        btnSaveWeight = findViewById(R.id.btnSaveWeight)
        btnCancel = findViewById(R.id.btnCancel)


        selectedSpecies = intent.getStringExtra("selectedSpecies") ?: ""

        // Apply InputFilters to enforce min and max values
        edtWeightInches.filters = arrayOf(MinMaxInputFilter(this, 1,99)) // Inches: 1-99
        edtWeight8ths.filters = arrayOf(MinMaxInputFilter(this, 0,7))    // 8ths: 0-7

        btnSaveWeight.setOnClickListener {
            val resultIntent = Intent()

            val weightInches = edtWeightInches.text.toString().toIntOrNull() ?: 0
            val weight8ths = edtWeight8ths.text.toString().toIntOrNull() ?: 0

            val totalLength8ths = (weightInches * 8) + weight8ths // Convert to total 8ths

            Log.d("PopupWeightEntryInches", "Length Entered: $weightInches Inches, $weight8ths 8ths")
            Log.d("PopupWeightEntryInches", "Total Length in 8ths: $totalLength8ths") // Debugging Log

            resultIntent.putExtra("lengthTotal8ths", totalLength8ths)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    class MinMaxInputFilter(
        private val context: Context,
        private val min: Int,
        private val max: Int
    ) : InputFilter {

        private val handler = Handler(Looper.getMainLooper()) // Ensure Toast runs on the UI thread

        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            try {
                val input = (dest.toString() + source.toString()).toInt()
                if (input in min..max) {
                    return null // Accept input
                } else {
                    showToast("Value must be between $min and $max")
                }
            } catch (e: NumberFormatException) {
                showToast("Invalid input! Enter a number between $min and $max")
            }
            return "" // Reject input
        }

        private fun showToast(message: String) {
            handler.post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
