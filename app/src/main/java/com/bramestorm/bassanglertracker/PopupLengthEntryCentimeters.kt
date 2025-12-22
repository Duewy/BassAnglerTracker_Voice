package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper

class PopupLengthEntryCentimeters : Activity() {

    private var selectedSpecies: String = ""

    companion object {
        const val EXTRA_LENGTH_CMS            = "totalLengthTenths"
        const val EXTRA_SPECIES               = "selectedSpecies"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_length_entry_centimeters)


        val edtLengthCm: EditText = findViewById(R.id.edtLengthCms)
        val edtLengthDecimal: EditText = findViewById(R.id.edtLengthDecimal)
        val btnSaveCatch: Button = findViewById(R.id.btnSaveLength)
        val btnCancel: Button = findViewById(R.id.btnCancel)

        loadSpeciesSpinner()


        edtLengthCm.filters = arrayOf(MinMaxInputFilter(0, 399))      // Cms: 1-399
        edtLengthDecimal.filters = arrayOf(MinMaxInputFilter(0, 9))  // millimeters: 0-9

        btnSaveCatch.setOnClickListener {
            val resultIntent = Intent()

            val lengthCm = edtLengthCm.text.toString().toIntOrNull() ?: 0
            val lengthDecimal = edtLengthDecimal.text.toString().toIntOrNull() ?: 0
            val totalLengthCms = (lengthCm * 10) + lengthDecimal

            if (totalLengthCms == 0) {
                Toast.makeText(this, "Length cannot be 0.0 cms!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resultIntent.putExtra(EXTRA_LENGTH_CMS, totalLengthCms)
            resultIntent.putExtra(EXTRA_SPECIES, selectedSpecies)

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
        val spinnerSpecies: Spinner = findViewById(R.id.spinnerCmsSpeciesPopUp)

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
