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

class PopupWeightEntryTourPounds : Activity() {

    // Flags and extras
    private var isTournament: Boolean = false
    private var catchType: String = ""
    private var selectedSpecies: String = ""

    // UI Components
    private lateinit var spinnerSpecies: Spinner
    private lateinit var spinnerClipColor: Spinner
    private lateinit var edtWeightTourPounds: EditText
    private lateinit var edtWeightTourPoundsDecimal: EditText
    private lateinit var btnSaveWeightPounds: Button
    private lateinit var btnCancelPounds: Button

    companion object {
        // ← outputs from this popup
        const val EXTRA_WEIGHT_POUNDS             = "totalWeightHundredthPounds"    // Send & receive this
        const val EXTRA_SPECIES                = "selectedSpecies"                  // Send this
        const val EXTRA_CLIP_COLOR             = "clip_color"                       // Send this
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
        setContentView(R.layout.popup_weight_entry_tour_pounds)

        //------  Retrieve intent extras from CATCH ENTRY TOURNAMENT  --------------------------
        isTournament = intent.getBooleanExtra("isTournament", false)
        catchType = intent.getStringExtra("catchType") ?: ""

        val colorNames = intent.getStringArrayExtra("availableClipColors")
            ?: arrayOf( "BLUE","YELLOW", "GREEN",  "ORANGE", "WHITE","RED")

        Log.d("PopupWeightEntry", "isTournament: $isTournament | catchType: $catchType | selectedSpecies: $selectedSpecies")
        Log.d("PopupWeightEntry", "Available clip colors: ${colorNames.joinToString()}")

        // UI Components
        spinnerSpecies = findViewById(R.id.spinnerTournyPoundsSpeciesPopUp)
        spinnerClipColor = findViewById(R.id.clipColorSpinner)
        edtWeightTourPounds = findViewById(R.id.edtWeightTourPounds)
        edtWeightTourPoundsDecimal = findViewById(R.id.edtWeightTourPoundsDecimal)
        btnSaveWeightPounds = findViewById(R.id.btnSaveWeightPounds)
        btnCancelPounds = findViewById(R.id.btnCancelPounds)

// Defer spinner setup until window is attached (prevents ANR)
        spinnerSpecies.post {
            setupTournamentSpeciesSpinner()
        }


        // ****************  Setup Clip Color Spinner ****************
        val availableColorNames = intent.getStringArrayExtra("availableClipColors") ?: emptyArray()
        val adapter = ClipColorSpinnerAdapter(this, availableColorNames.toList())
        spinnerClipColor.adapter = adapter

        // `````````````` Apply InputFilters to limit values  ````````````````````

        clearOnFocus(edtWeightTourPounds)
        clearOnFocus(edtWeightTourPoundsDecimal)

        edtWeightTourPounds.filters = arrayOf(MinMaxInputFilter(0, 99)) // Pounds: 0-99
        edtWeightTourPoundsDecimal.filters = arrayOf(MinMaxInputFilter(0, 99)) // Decimal 0 - 99

        // `````````` SAVE btn ````````````````
        btnSaveWeightPounds.setOnClickListener {
            val selectedSpeciesValue = spinnerSpecies.selectedItem.toString()
            val selectedClipColor = spinnerClipColor.selectedItem?.toString()?.uppercase() ?: "RED"
            Log.d("CLIPS", "🎨 Selected Clip Color: $selectedClipColor")

            val weightPounds = edtWeightTourPounds.text.toString().toIntOrNull() ?: 0
            val weightDec = edtWeightTourPoundsDecimal.text.toString().toIntOrNull() ?: 0
            val totalWeightHundredthPounds = ((weightPounds * 100) + weightDec)

            if (totalWeightHundredthPounds == 0) {
                Toast.makeText(this, "Weight cannot be 0.00 Lbs!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(
                "CLIPS",
                "✅ Sending Result - weightTotalHundredthPounds: $totalWeightHundredthPounds, selectedSpecies: $selectedSpeciesValue, clipColor: $selectedClipColor"
            )

            Intent().apply {
                putExtra(EXTRA_WEIGHT_POUNDS, totalWeightHundredthPounds)
                putExtra(EXTRA_SPECIES, selectedSpeciesValue)
                putExtra(EXTRA_CLIP_COLOR, selectedClipColor)
                putExtra(EXTRA_CATCH_TYPE, catchType)
                putExtra(EXTRA_IS_TOURNAMENT, isTournament)
            }.let { resultIntent ->
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }

        // ````````` CANCEL btn ```````````````````
        btnCancelPounds.setOnClickListener {
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
