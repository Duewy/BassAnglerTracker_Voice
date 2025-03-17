package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner

class PopupWeightEntryKgs : Activity() {

    private var selectedSpecies: String = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_weight_entry_kgs)

        val edtWeightKgs: EditText = findViewById(R.id.edtWeightKgs)
        val edtWeightGrams: EditText = findViewById(R.id.edtWeightGrams)
        val btnSaveWeight: Button = findViewById(R.id.btnSaveWeight)
        val btnCancel: Button = findViewById(R.id.btnCancel)
        val spinnerSpecies: Spinner = findViewById(R.id.spinnerKgsSpeciesPopUp)

        // Load species list from strings.xml
        val speciesArray = resources.getStringArray(R.array.species_list)

        // Set up the spinner with species list
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecies.adapter = adapter

        // Update selectedSpecies when user picks an item
        spinnerSpecies.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedSpecies = speciesArray[position] // Update selectedSpecies
                Log.d("DB_DEBUG", "Species selected: $selectedSpecies") // Debugging log
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedSpecies = "" // Reset if nothing is selected
            }
        }

        // `````````````` Apply InputFilters to limit values  ````````````````````
       // edtWeightKgs.filters = arrayOf(MinMaxInputFilter(0, 99)) // Kgs: 0-99
       // edtWeightGrams.filters = arrayOf(MinMaxInputFilter(0, 99)) // Kgs/1000 (0 to 99)

        // ````````````````````` SAVE CATCH ENTRY  ````````````````````

        btnSaveWeight.setOnClickListener {
            val resultIntent = Intent()

            val weightKgs = edtWeightKgs.text.toString().toIntOrNull() ?: 0
            val weightGrams = edtWeightGrams.text.toString().toIntOrNull() ?: 0
            val totalWeightHundredthKg  = ((weightKgs * 100) + weightGrams)

            resultIntent.putExtra("weightTotalKg", totalWeightHundredthKg )
            resultIntent.putExtra("selectedSpecies", selectedSpecies)

            Log.d("DB_DEBUG", "🚀 Returning weight from Pop Up: $totalWeightHundredthKg  GRAMS, Species: $selectedSpecies")

                setResult(Activity.RESULT_OK, resultIntent)
                finish()
        }

        // ~~~~~~~~~~~~~~~~ Cancel button functionality  ~~~~~~~~~~~~~~~~~~~~~
        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    } // ###################### END ON CREATE ##############################################

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
            try {
                val input = (dest.toString() + source.toString()).toInt()
                if (input in min..max) {
                    return null // Accept input
                }
            } catch (e: NumberFormatException) {
                // Ignore invalid input
            }
            return "" // Reject input if out of range
        }
    }

}//################ END POP UP WEIGHT ENTRY KGS ######################
