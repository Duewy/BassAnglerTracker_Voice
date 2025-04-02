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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_length_entry_inches)

        val edtLengthInches: EditText = findViewById(R.id.edtLengthInches)
        val edtLength8ths: EditText = findViewById(R.id.edtLength8ths)
        val btnSaveCatch: Button = findViewById(R.id.btnSaveCatch)
        val btnCancel: Button = findViewById(R.id.btnCancel)
        val spinnerSpecies: Spinner = findViewById(R.id.spinnerInchesSpeciesPopUp)

        // Load species from SharedPreferences and map to SpeciesItem with correct image
        val savedSpecies = SharedPreferencesManager.getSelectedSpeciesList(this)

        val speciesList = savedSpecies.map { speciesName ->
            val imageRes = SpeciesImageHelper.getSpeciesImageResId(speciesName)
            SpeciesItem(speciesName, imageRes)
        }

        val adapter = SpeciesSpinnerAdapter(this, speciesList)
        spinnerSpecies.adapter = adapter

        spinnerSpecies.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSpecies = speciesList[position].name
                Log.d("DB_DEBUG", "Species selected: $selectedSpecies")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedSpecies = ""
            }
        }

        edtLengthInches.filters = arrayOf(MinMaxInputFilter(1, 99))
        edtLength8ths.filters = arrayOf(MinMaxInputFilter(0, 7))

        btnSaveCatch.setOnClickListener {
            val resultIntent = Intent()

            val lengthInches = edtLengthInches.text.toString().toIntOrNull() ?: 0
            val length8ths = edtLength8ths.text.toString().toIntOrNull() ?: 0
            val lengthTotalInches = (lengthInches * 8) + length8ths

            if (lengthTotalInches == 0) {
                Toast.makeText(this, "Length cannot be 0 Inches 0/8ths!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resultIntent.putExtra("lengthTotalInches", lengthTotalInches)
            resultIntent.putExtra("selectedSpecies", selectedSpecies)

            Log.d("DB_DEBUG", "🚀 Returning length from Pop Up: $lengthTotalInches (in 1/8ths), Species: $selectedSpecies")

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

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
