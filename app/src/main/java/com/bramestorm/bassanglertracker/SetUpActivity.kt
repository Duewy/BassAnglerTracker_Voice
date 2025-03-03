package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity


class SetUpActivity : AppCompatActivity() {

    private lateinit var btnWeight: Button
    private lateinit var btnLength: Button
    private lateinit var btnImperial: Button
    private lateinit var btnMetric: Button
    private lateinit var btnFunDay: Button
    private lateinit var btnTournament: Button
    private lateinit var btnStartFishing: Button
    private lateinit var spinnerTournamentSpecies: Spinner
    private lateinit var tglColorLetter: ToggleButton
    private lateinit var tglCullingValue: ToggleButton
    private lateinit var tglGPS: ToggleButton

    private var isWeightSelected = true
    private var isLengthSelected = false
    private var isImperialSelected = true
    private var isMetricSelected = false
    private var isFunDaySelected = true
    private var isTournamentSelected = false

    private var isValUnits = false
    private var isValMeasuring = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_up_event)

        // Initialize Buttons
        btnWeight = findViewById(R.id.btnWeight)
        btnLength = findViewById(R.id.btnLength)
        btnImperial = findViewById(R.id.btnImperial)
        btnMetric = findViewById(R.id.btnMetric)
        btnFunDay = findViewById(R.id.btnFunDay)
        btnTournament = findViewById(R.id.btnTournament)
        btnStartFishing = findViewById(R.id.btnStartFishing)
        tglCullingValue = findViewById(R.id.tglCullingValue)
        tglColorLetter = findViewById(R.id.tglColorLetter)
        spinnerTournamentSpecies = findViewById(R.id.spinnerTournamentSpecies)
        tglGPS = findViewById(R.id.tglGPS)

        tglCullingValue.visibility = View.INVISIBLE
        tglColorLetter.visibility = View.INVISIBLE
        spinnerTournamentSpecies.visibility =View.INVISIBLE

        // Toggle Weight Selection
        btnWeight.setOnClickListener {
            isWeightSelected = true
            isLengthSelected = false
            isValMeasuring = true
            btnWeight.setBackgroundResource(R.color.white)
            btnLength.setBackgroundResource(R.color.grey)
        }

        btnLength.setOnClickListener {
            isLengthSelected = true
            isWeightSelected = false
            isValMeasuring = true
            btnLength.setBackgroundResource(R.color.white)
            btnWeight.setBackgroundResource(R.color.grey)
        }

        // Toggle Units Selection
        btnImperial.setOnClickListener {
            isImperialSelected = true
            isMetricSelected = false
            isValUnits = true
            btnImperial.setBackgroundResource(R.color.white)
            btnMetric.setBackgroundResource(R.color.grey)
        }

        btnMetric.setOnClickListener {
            isMetricSelected = true
            isImperialSelected = false
            isValUnits = true
            btnMetric.setBackgroundResource(R.color.white)
            btnImperial.setBackgroundResource(R.color.grey)
        }

        // Toggle Fun Day/Tournament Selection
        btnFunDay.setOnClickListener {
            isFunDaySelected = true
            isTournamentSelected = false
            btnFunDay.setBackgroundResource(R.color.white)
            btnTournament.setBackgroundResource(R.color.grey)
            btnLength.visibility = View.VISIBLE
            btnMetric.visibility=View.VISIBLE
            tglCullingValue.visibility = View.INVISIBLE
            tglColorLetter.visibility = View.INVISIBLE
            spinnerTournamentSpecies.visibility =View.INVISIBLE
        }

        btnTournament.setOnClickListener {
            isTournamentSelected = true
            isFunDaySelected = false
            isLengthSelected = false
            isWeightSelected = true
            btnTournament.setBackgroundResource(R.color.white)
            btnFunDay.setBackgroundResource(R.color.grey)
            btnLength.visibility = View.INVISIBLE       //take away measuring options for Tournament mode
            tglCullingValue.visibility = View.VISIBLE
            tglColorLetter.visibility = View.VISIBLE
            spinnerTournamentSpecies.visibility =View.VISIBLE
        }

        /// ✅ Select Fishing Event (Fun Day or Tournament)
        btnStartFishing.setOnClickListener {

            // Determine next activity for Fun Day mode
            val nextActivity = when {
                isWeightSelected && isImperialSelected && isFunDaySelected -> CatchEntryLbsOzs::class.java
                isWeightSelected && isMetricSelected && isFunDaySelected -> CatchEntryKgs::class.java
                isLengthSelected && isImperialSelected && isFunDaySelected -> CatchEntryInches::class.java
                isLengthSelected && isMetricSelected && isFunDaySelected -> CatchEntryMetric::class.java
                else -> null
            }

            nextActivity?.let { startActivity(Intent(this, it)) }

            // Tournament Mode (Imperial & Metric)
            if (isTournamentSelected) {
                val numberOfCatches = if (tglCullingValue.isChecked) 5 else 4
                val typeOfMarkers = if (tglColorLetter.isChecked) "Color" else "Number"
                val selectedSpecies = spinnerTournamentSpecies.selectedItem?.toString() ?: "Unknown"
                val tournamentActivity = if (isImperialSelected) CatchEntryTournament::class.java else CatchEntryTournamentKgs::class.java

                val intent = Intent(this, tournamentActivity).apply {
                    putExtra("NUMBER_OF_CATCHES", numberOfCatches)
                    putExtra("Color_Numbers", typeOfMarkers)
                    putExtra("TOURNAMENT_SPECIES", selectedSpecies)
                    putExtra("unitType", if (isWeightSelected) "weight" else "length")
                    putExtra("CULLING_ENABLED", tglCullingValue.isChecked)
                }

                startActivity(intent)
            }
        }



    }


}