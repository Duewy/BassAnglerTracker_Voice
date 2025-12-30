package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.bramestorm.bassanglertracker.PopupWeightEntryLbs.MinMaxInputFilter

class PopupLengthEntryTourInches : Activity() {

    // Flags and extras
    private var isTournament: Boolean = false
    private var catchType: String = ""
    private var selectedSpecies: String = ""

    // UI Components
    private lateinit var spinnerSpecies: Spinner
    private lateinit var spinnerClipColor: Spinner
    private lateinit var edtLengthInches: EditText
    private lateinit var edtLengthQuarters: EditText
    private lateinit var btnSaveLengthInches: Button
    private lateinit var btnCancelInches: Button

    companion object {
        const val EXTRA_LENGTH_INCHES          = "totalLengthQuarters"
        const val EXTRA_SPECIES                = "selectedSpecies"
        const val EXTRA_CLIP_COLOR             = "clip_color"           // Send this
        const val EXTRA_CATCH_TYPE             = "catchType"
        const val EXTRA_IS_TOURNAMENT          = "isTournament"
        const val EXTRA_CULLING_NUMBERS        = "Culling_Numbers"

        // → inputs into this popup
        const val EXTRA_AVAILABLE_CLIP_COLORS  = "availableClipColors"  // Receive this list
        const val EXTRA_TOURNAMENT_SPECIES     = "tournamentSpecies"    // Receive this
    }

    //============== ON CREATE ===============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_length_entry_tour_inches)

        //------  Retrieve intent extras from CATCH ENTRY TOURNAMENT  --------------------------
        isTournament = intent.getBooleanExtra("isTournament", false)
        catchType = intent.getStringExtra("catchType") ?: ""
      //  selectedSpecies = intent.getStringExtra("selectedSpecies") ?: ""

        val colorNames = intent.getStringArrayExtra("availableClipColors")
            ?: arrayOf( "BLUE","YELLOW", "GREEN",  "ORANGE", "WHITE","RED")

        // UI Components
        spinnerSpecies = findViewById(R.id.spinnerInchesTournySpeciesPopUp)
        spinnerClipColor = findViewById(R.id.clipColorSpinner)
        edtLengthInches = findViewById(R.id.edtLengthTourInches)
        edtLengthQuarters = findViewById(R.id.edtLengthQuarters)
        btnSaveLengthInches = findViewById(R.id.btnSaveLengthInches)
        btnCancelInches = findViewById(R.id.btnCancelInches)

    // Defer spinner setup until window is attached (prevents ANR)
        spinnerSpecies.post {
            setupTournamentSpeciesSpinner()
        }

        // ****************  Setup Clip Color Spinner ****************
        val availableColorNames = intent.getStringArrayExtra("availableClipColors") ?: emptyArray()
        val adapter = ClipColorSpinnerAdapter(this, availableColorNames.toList())
        spinnerClipColor.adapter = adapter

        // `````````````` Apply InputFilters to limit values  ````````````````````

        clearOnFocus(edtLengthInches)
        clearOnFocus(edtLengthQuarters)

        edtLengthInches.filters = arrayOf(MinMaxInputFilter(0, 99)) // Inches: 0-99
        edtLengthQuarters.filters = arrayOf(MinMaxInputFilter(0, 3)) //  0 - 3 / 4ths

        // `````````` btn SAVE  ````````````````
        btnSaveLengthInches.setOnClickListener {
            val selectedSpeciesValue = spinnerSpecies.selectedItem.toString()
            val selectedClipColor = spinnerClipColor.selectedItem?.toString()?.uppercase() ?: "RED"
            Log.d("CLIPS", "🎨 Selected Clip Color: $selectedClipColor")

            val lengthInches = edtLengthInches.text.toString().toIntOrNull() ?: 0
            val lengthQuarters = edtLengthQuarters.text.toString().toIntOrNull() ?: 0

            val totalLengthQuarters= ((lengthInches * 4) + lengthQuarters)

            if ( totalLengthQuarters == 0) {
                Toast.makeText(this, "Length cannot be 0 Inches!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("CLIPS", "✅ Sending Result - weightLengthInches: $totalLengthQuarters, selectedSpecies: $selectedSpeciesValue, clipColor: $selectedClipColor")

            val resultIntent = Intent().apply {
                putExtra(EXTRA_LENGTH_INCHES, totalLengthQuarters)
                putExtra(EXTRA_SPECIES, selectedSpeciesValue)
                putExtra(EXTRA_CLIP_COLOR, selectedClipColor)
                putExtra(EXTRA_CATCH_TYPE, catchType)
                putExtra(EXTRA_IS_TOURNAMENT, isTournament)
            }

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        // ````````` btn CANCEL  ```````````````````
        btnCancelInches.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }//```````````` END ON CREATE ```````````````````````````
    private fun setupTournamentSpeciesSpinner() {

        val tournamentSpecies = intent
            .getStringExtra(EXTRA_TOURNAMENT_SPECIES)
            ?.trim()

        if (tournamentSpecies.isNullOrEmpty()) {
            Log.e("POPUP", "Tournament species missing — closing popup safely")
            finish()
            return
        }

        val speciesList = when (tournamentSpecies.lowercase()) {

            "large mouth" -> listOf(
                "Large Mouth",
                "Small Mouth"
            )

            "small mouth" -> listOf(
                "Small Mouth",
                "Large Mouth"
            )

            "spotted bass" -> listOf(
                "Spotted Bass",
                "Small Mouth",
                "Large Mouth"
            )

            else -> listOf(
                tournamentSpecies
            )
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            speciesList
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerSpecies.adapter = adapter
    }

    //------ Clear the Edit Text Box onClick ----------
    private fun clearOnFocus(editText: EditText) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                editText.text.clear()
                editText.setSelection(0)
            }
        }
    } //---------------------------------------------


}//================== END  ==========================
