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

class PopupWeightEntryTourLbs : Activity() {

    // Flags and extras
    private var isTournament: Boolean = false
    private var catchType: String = ""
    private var selectedSpecies: String = ""

    // UI Components
    private lateinit var spinnerSpecies: Spinner
    private lateinit var spinnerClipColor: Spinner
    private lateinit var edtWeightLbs: EditText
    private lateinit var edtWeightOz: EditText
    private lateinit var btnSaveWeight: Button
    private lateinit var btnCancel: Button

    companion object {
        const val EXTRA_WEIGHT_OZ     = "weightTotalOz"
        const val EXTRA_SPECIES       = "selectedSpecies"
        const val EXTRA_CLIP_COLOR    = "clip_color"
        const val EXTRA_CATCH_TYPE    = "catchType"
        const val EXTRA_IS_TOURNAMENT = "isTournament"
        const val EXTRA_AVAILABLE_CLIP_COLORS = "availableClipColors"
        const val EXTRA_TOURNAMENT_SPECIES = "tournamentSpecies"
    }

    //============== ON CREATE ===============================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_weight_entry_tour_lbs)


        //------  Retrieve intent extras from CATCH ENTRY TOURNAMENT  --------------------------
        isTournament = intent.getBooleanExtra("isTournament", false)
        catchType    = intent.getStringExtra("catchType") ?: ""

        val colorNames  = intent.getStringArrayExtra("availableClipColors")
            ?: arrayOf( "BLUE","YELLOW", "GREEN",  "ORANGE", "WHITE","RED")


        // UI Components
        spinnerSpecies = findViewById(R.id.spinnerTournyLbsSpeciesPopUp)
        spinnerClipColor = findViewById(R.id.clipColorSpinner)
        edtWeightLbs = findViewById(R.id.edtWeightTourLbs)
        edtWeightOz = findViewById(R.id.edtWeightTourOz)
        btnSaveWeight = findViewById(R.id.btnSaveWeight)
        btnCancel = findViewById(R.id.btnCancel)

        // ----- Clear out the values in the Edit Text Boxes ----
        clearOnFocus(edtWeightLbs)
        clearOnFocus(edtWeightOz)

        // Defer spinner setup until window is attached (prevents ANR)
        spinnerSpecies.post {
            setupTournamentSpeciesSpinner()
        }

        // ****************  Setup Clip Color Spinner ****************
        val availableColorNames = intent.getStringArrayExtra("availableClipColors") ?: emptyArray()
        val adapter = ClipColorSpinnerAdapter(this, availableColorNames.toList())
        spinnerClipColor.adapter = adapter

        // `````````````` Apply InputFilters to limit values  ````````````````````
        edtWeightLbs.filters = arrayOf(MinMaxInputFilter(0, 99)) // Lbs: 0-99
        edtWeightOz.filters = arrayOf(MinMaxInputFilter(0, 15)) // Ozs 0 - 15

        // `````````` SAVE btn ````````````````
        btnSaveWeight.setOnClickListener {
            val selectedSpeciesValue = spinnerSpecies.selectedItem.toString()
            val selectedClipColor = spinnerClipColor.selectedItem?.toString()?.uppercase() ?: "RED"
            Log.d("CLIPS", "🎨 Selected Clip Color: $selectedClipColor")

            val weightLbs = edtWeightLbs.text.toString().toIntOrNull() ?: 0
            val weightOz = edtWeightOz.text.toString().toIntOrNull() ?: 0
            val totalWeightOz = ((weightLbs * 16) + weightOz)

            if (totalWeightOz == 0) {
                Toast.makeText(this, "🚫 Weight cannot be 0 lbs 0 oz!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Intent().also {
                it.putExtra(EXTRA_WEIGHT_OZ, totalWeightOz)
                it.putExtra(EXTRA_SPECIES, selectedSpeciesValue)
                it.putExtra(EXTRA_CLIP_COLOR, selectedClipColor)
                it.putExtra(EXTRA_CATCH_TYPE, catchType)
                it.putExtra(EXTRA_IS_TOURNAMENT, isTournament)
            }.let { resultIntent ->
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
      // ````````` CANCEL btn ```````````````````
        btnCancel.setOnClickListener {
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
