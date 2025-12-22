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

class PopupWeightEntryTourKgs : Activity() {

    // Flags and extras
    private var isTournament: Boolean = false
    private var catchType: String = ""
    private var selectedSpecies: String = ""

    // UI Components
    private lateinit var spinnerSpecies: Spinner
    private lateinit var spinnerClipColor: Spinner
    private lateinit var edtWeightTourKgs: EditText
    private lateinit var edtWeightTourGrams: EditText
    private lateinit var btnSaveWeightKgs: Button
    private lateinit var btnCancelKgs: Button

    companion object {
        // ← outputs from this popup
        const val EXTRA_WEIGHT_KGS             = "totalWeightHundredthKg"        // Send & receive this
        const val EXTRA_SPECIES                = "selectedSpecies"               // Send this
        const val EXTRA_CLIP_COLOR             = "clip_color"                    // Send this
        const val EXTRA_CATCH_TYPE             = "catchType"
        const val EXTRA_IS_TOURNAMENT          = "isTournament"


        // → inputs into this popup
        const val EXTRA_AVAILABLE_CLIP_COLORS  = "availableClipColors"  // Receive this list
        const val EXTRA_TOURNAMENT_SPECIES     = "tournamentSpecies"    // Receive this
    }

    //============== ON CREATE ===============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_weight_entry_tour_kgs)

        //------  Retrieve intent extras from CATCH ENTRY TOURNAMENT  --------------------------

        catchType = intent.getStringExtra("catchType") ?: ""

        val colorNames = intent.getStringArrayExtra("availableClipColors")
            ?: arrayOf( "BLUE","YELLOW", "GREEN",  "ORANGE", "WHITE","RED")


        // UI Components
        spinnerSpecies = findViewById(R.id.spinnerTournyKgsSpeciesPopUp)
        spinnerClipColor = findViewById(R.id.clipColorSpinner)
        edtWeightTourKgs = findViewById(R.id.edtWeightTourKgs)
        edtWeightTourGrams = findViewById(R.id.edtWeightTourGrams)
        btnSaveWeightKgs = findViewById(R.id.btnSaveWeightKgs)
        btnCancelKgs = findViewById(R.id.btnCancelKgs)

// Defer spinner setup until window is attached (prevents ANR)
        spinnerSpecies.post {
            setupTournamentSpeciesSpinner()
        }

        // ****************  Setup Clip Color Spinner ****************
        val availableColorNames = intent.getStringArrayExtra("availableClipColors") ?: emptyArray()
        val adapter = ClipColorSpinnerAdapter(this, availableColorNames.toList())
        spinnerClipColor.adapter = adapter

        // `````````````` Apply InputFilters to limit values  ````````````````````
        edtWeightTourKgs.filters = arrayOf(MinMaxInputFilter(0, 99)) // Kgs: 0-99
        edtWeightTourGrams.filters = arrayOf(MinMaxInputFilter(0, 99)) // Grams 0 - 99

        // `````````` SAVE btn ````````````````
        btnSaveWeightKgs.setOnClickListener {
            val selectedSpeciesValue = spinnerSpecies.selectedItem.toString()
            val selectedClipColor = spinnerClipColor.selectedItem?.toString()?.uppercase() ?: "RED"
            Log.d("CLIPS", "🎨 Selected Clip Color: $selectedClipColor")

            val weightKgs = edtWeightTourKgs.text.toString().toIntOrNull() ?: 0
            val weightGrams = edtWeightTourGrams.text.toString().toIntOrNull() ?: 0
            val totalWeightHundredthKg = (weightKgs * 100) + weightGrams

            if (totalWeightHundredthKg == 0) {
                Toast.makeText(this, "Weight cannot be 0.00 Kgs!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(
                "CLIPS",
                "✅ Sending Result - weightTotalKgs: $totalWeightHundredthKg, selectedSpecies: $selectedSpeciesValue, clipColor: $selectedClipColor"
            )

            Intent().apply {
                putExtra(EXTRA_WEIGHT_KGS, totalWeightHundredthKg)
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
        btnCancelKgs.setOnClickListener {
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

}//================== END  ==========================
