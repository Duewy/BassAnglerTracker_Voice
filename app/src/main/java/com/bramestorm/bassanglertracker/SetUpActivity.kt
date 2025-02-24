package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SetUpActivity : AppCompatActivity() {

    private lateinit var btnWeight: Button
    private lateinit var btnLength: Button
    private lateinit var btnImperial: Button
    private lateinit var btnMetric: Button
    private lateinit var btnFunDay: Button
    private lateinit var btnTournament: Button
    private lateinit var btnSelectFishingEvent: Button

    private var isWeightSelected = true
    private var isLengthSelected = false

    private var isImperialSelected = true
    private var isMetricSelected = false

    private var isFunDaySelected =true
    private var isTournamentSelected = false


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
        btnSelectFishingEvent = findViewById(R.id.btnSelectFishingEvent)

        // Toggle Weight Selection
        btnWeight.setOnClickListener {
            isWeightSelected = true
            isLengthSelected = false
            btnWeight.setBackgroundResource(R.color.white)
            btnLength.setBackgroundResource(R.color.grey)
        }

        btnLength.setOnClickListener {
            isLengthSelected = true
            isWeightSelected = false
            btnLength.setBackgroundResource(R.color.white)
            btnWeight.setBackgroundResource(R.color.grey)
        }

        // Toggle Units Selection
        btnImperial.setOnClickListener {
            isImperialSelected = true
            isMetricSelected = false
            btnImperial.setBackgroundResource(R.color.white)
            btnMetric.setBackgroundResource(R.color.grey)
        }

        btnMetric.setOnClickListener {
            isMetricSelected = true
            isImperialSelected = false
            btnMetric.setBackgroundResource(R.color.white)
            btnImperial.setBackgroundResource(R.color.grey)
        }

        // Toggle Fun Day/Tournament Selection
        btnFunDay.setOnClickListener {
            isFunDaySelected = true
            isTournamentSelected = false
            btnFunDay.setBackgroundResource(R.color.white)
            btnTournament.setBackgroundResource(R.color.grey)
        }

        btnTournament.setOnClickListener {
            isTournamentSelected = true
            isFunDaySelected = false
            btnTournament.setBackgroundResource(R.color.white)
            btnFunDay.setBackgroundResource(R.color.grey)
        }

        // Button Click to Select Fishing Event
        btnSelectFishingEvent.setOnClickListener {
            if (isWeightSelected && isImperialSelected && isFunDaySelected) {
                // ✅ Open CatchEntryLbsOzs Activity
                val intent = Intent(this, CatchEntryLbsOzs::class.java)
                startActivity(intent)
            } else if (isWeightSelected && isMetricSelected && isFunDaySelected) {
                // ✅ Open CatchEntryKgs Activity
                val intent = Intent(this, CatchEntryKgs::class.java)
                startActivity(intent)
            }else if (isLengthSelected && isImperialSelected && isFunDaySelected) {
                // ✅ Open CatchEntryInches Activity
                val intent = Intent(this, CatchEntryInches::class.java)
                startActivity(intent)
            }else if  (isLengthSelected && isMetricSelected&& isFunDaySelected) {
                // ✅ Open CatchEntryMetric Activity
                val intent = Intent(this, CatchEntryMetric::class.java)
                startActivity(intent)
            }
        }
    }
}
