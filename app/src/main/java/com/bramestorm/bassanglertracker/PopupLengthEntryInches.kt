package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper

class PopupLengthEntryInches : Activity() {

    private var selectedSpecies: String = ""

    companion object {
        const val EXTRA_LENGTH_INCHES = "totalLengthQuarters"
        const val EXTRA_SPECIES       = "selectedSpecies"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_length_entry_inches)

        val edtLengthInches: EditText = findViewById(R.id.edtLengthInches)
        val edtLengthQuarters: EditText = findViewById(R.id.edtLengthQuarters)
        val btnSaveCatch: Button = findViewById(R.id.btnSaveCatch)
        val btnCancel: Button = findViewById(R.id.btnCancel)

        loadSpeciesSpinner()

        edtLengthInches.filters = arrayOf(MinMaxInputFilter(1, 299))    //Limit to 299 Inches
        edtLengthQuarters.filters = arrayOf(MinMaxInputFilter(0, 3))    //Limit 0 to 3 quarters

        btnSaveCatch.setOnClickListener {
            val resultIntent = Intent()

            val lengthInches = edtLengthInches.text.toString().toIntOrNull() ?: 0
            val lengthQuarters = edtLengthQuarters.text.toString().toIntOrNull() ?: 0
            val lengthTotalInches = (lengthInches * 4) + lengthQuarters

            if (lengthTotalInches == 0) {
                Toast.makeText(this, "Length cannot be 0 Inches 0/4ths!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resultIntent.putExtra(EXTRA_LENGTH_INCHES, lengthTotalInches)
            resultIntent.putExtra(EXTRA_SPECIES, selectedSpecies)

            Log.d("DB_DEBUG", "🚀 Returning length from Pop Up: $lengthTotalInches (in 1/4ths), Species: $selectedSpecies")

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun loadSpeciesSpinner() {
        val spinnerSpecies: Spinner = findViewById(R.id.spinnerInchesSpeciesPopUp)

        val speciesList = SharedPreferencesManager
            .loadSpeciesList(this)
            .map { speciesName ->
                SpeciesItem(
                    speciesName,
                    SpeciesImageHelper.getSpeciesImageResId(speciesName)
                )
            }

        val adapter = SpeciesSpinnerAdapter(this, speciesList)
        spinnerSpecies.adapter = adapter

        if (speciesList.isNotEmpty()) {
            selectedSpecies = speciesList[0].name
        }

        spinnerSpecies.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedSpecies = speciesList[position].name
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedSpecies = ""
                }
            }
    }

    // -------------- Min & Max Input Filter to enforce value limits --------------------------
    class MinMaxInputFilter(private val min: Int, private val max: Int) : InputFilter {
        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            return try {
                val input = (dest.toString() + source.toString()).toInt()
                if (input in min..max) null else ""
            } catch (e: NumberFormatException) {
                ""
            }
        }
    }
}
