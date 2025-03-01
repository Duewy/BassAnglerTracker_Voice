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
            tglCullingValue.visibility = View.INVISIBLE
            tglColorLetter.visibility = View.INVISIBLE
            tglGPS.visibility = View.INVISIBLE
        }

        btnTournament.setOnClickListener {
            isTournamentSelected = true
            isFunDaySelected = false
            btnTournament.setBackgroundResource(R.color.white)
            btnFunDay.setBackgroundResource(R.color.grey)
            tglCullingValue.visibility = View.VISIBLE
            tglColorLetter.visibility = View.VISIBLE
            tglGPS.visibility = View.VISIBLE
        }

        // ✅ Select Fishing Event (Fun Day or Tournament)
        btnStartFishing.setOnClickListener {

            // Determine next activity based on selections
            val nextActivity = when {
                isWeightSelected && isImperialSelected && isFunDaySelected -> CatchEntryLbsOzs::class.java
                isWeightSelected && !isImperialSelected && isFunDaySelected -> CatchEntryKgs::class.java
                !isWeightSelected && isImperialSelected && isFunDaySelected -> CatchEntryInches::class.java
                !isWeightSelected && !isImperialSelected && isFunDaySelected -> CatchEntryMetric::class.java
                else -> CatchEntryTournament::class.java // Default to Tournament Mode
            }

            // If it's Tournament Mode, attach extra data
            if (!isFunDaySelected) {
                val numberOfCatches = if (tglCullingValue.isChecked) 5 else 4
                val typeOfMarkers = if (tglColorLetter.isChecked) "Color" else "Number"
                val selectedSpecies = spinnerTournamentSpecies.selectedItem?.toString() ?: "Unknown"

                val intent = Intent(this, CatchEntryTournament::class.java).apply {
                    putExtra("NUMBER_OF_CATCHES", numberOfCatches)
                    putExtra("Color_Numbers", typeOfMarkers)
                    putExtra("TOURNAMENT_SPECIES", selectedSpecies)
                    putExtra("unitType", if (isWeightSelected) "weight" else "length")
                    putExtra("CULLING_ENABLED", tglCullingValue.isChecked)
                }
                startActivity(intent)
            } else {
                // Regular Fun Day Mode
                startActivity(Intent(this, nextActivity))
            }
        }


    }


}