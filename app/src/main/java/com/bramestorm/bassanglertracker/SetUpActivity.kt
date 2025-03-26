package com.bramestorm.bassanglertracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices


class SetUpActivity : AppCompatActivity() {

    private lateinit var btnWeight: Button
    private lateinit var btnLength: Button
    private lateinit var btnImperial: Button
    private lateinit var btnMetric: Button
    private lateinit var btnFunDay: Button
    private lateinit var btnTournament: Button
    private lateinit var btnStartFishing: Button
    private lateinit var spinnerTournamentSpecies: Spinner
    private lateinit var tglCullingValue: ToggleButton
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tglGPS: ToggleButton
    private lateinit var btnMainSetup:Button
    private val sharedPreferences by lazy { getSharedPreferences("AppPrefs", MODE_PRIVATE) }
    private val prefs by lazy { getSharedPreferences("BassAnglerTrackerPrefs", MODE_PRIVATE) }

    private var isWeightSelected = false
    private var isLengthSelected = false
    private var isImperialSelected = false
    private var isMetricSelected = false
    private var isFunDaySelected = false
    private var isTournamentSelected = false
    private var selectedSpecies: String = ""

    private var isValUnits = false
    private var isValMeasuring = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_up_event)

        // Initialize UI components
        btnWeight = findViewById(R.id.btnWeight)
        btnLength = findViewById(R.id.btnLength)
        btnImperial = findViewById(R.id.btnImperial)
        btnMetric = findViewById(R.id.btnMetric)
        btnFunDay = findViewById(R.id.btnFunDay)
        btnTournament = findViewById(R.id.btnTournament)
        btnStartFishing = findViewById(R.id.btnStartFishing)
        tglCullingValue = findViewById(R.id.tglCullingValue)
        spinnerTournamentSpecies = findViewById(R.id.spinnerTournamentSpecies)
        tglGPS = findViewById(R.id.tglGPS)
        btnMainSetup = findViewById(R.id.btnMainSetup)

        tglCullingValue.visibility = View.INVISIBLE
        spinnerTournamentSpecies.visibility = View.INVISIBLE

        // Toggle Weight Selection
        btnWeight.setOnClickListener {
            Log.d("DEBUG", "Weight Is Selected ")
            isWeightSelected = true
            isLengthSelected = false
            isValMeasuring = true
            btnImperial.text = "Lbs Ozs"
            btnMetric.text = " Kgs"
            btnWeight.setBackgroundResource(R.color.white)
            btnLength.setBackgroundResource(R.color.grey)
        }

        btnLength.setOnClickListener {
            Log.d("DEBUG", "Length Is Selected ")
            isLengthSelected = true
            isWeightSelected = false
            isValMeasuring = true
            btnImperial.text = "Inches 8ths"
            btnMetric.text = "Cms"
            btnLength.setBackgroundResource(R.color.white)
            btnWeight.setBackgroundResource(R.color.grey)
        }

        // Toggle Units Selection
        btnImperial.setOnClickListener {
            Log.d("DEBUG", "Imperial Is Selected ")
            isImperialSelected = true
            isMetricSelected = false
            isValUnits = true
            btnImperial.setBackgroundResource(R.color.white)
            btnMetric.setBackgroundResource(R.color.grey)
        }

        btnMetric.setOnClickListener {
            Log.d("DEBUG", "Metric Is Selected ")
            isMetricSelected = true
            isImperialSelected = false
            isValUnits = true
            btnMetric.setBackgroundResource(R.color.white)
            btnImperial.setBackgroundResource(R.color.grey)
        }

        // Toggle Fun Day/Tournament Selection
        btnFunDay.setOnClickListener {
            Log.d("DEBUG", "FunDay Is Selected ")
            isFunDaySelected = true
            isTournamentSelected = false
            btnFunDay.setBackgroundResource(R.color.white)
            btnTournament.setBackgroundResource(R.color.grey)
            btnLength.visibility = View.VISIBLE
            btnMetric.visibility = View.VISIBLE
            tglCullingValue.visibility = View.INVISIBLE
            spinnerTournamentSpecies.visibility = View.INVISIBLE
        }

        btnTournament.setOnClickListener {
            Log.d("DEBUG", "Tournament Is Selected ")
            isTournamentSelected = true
            isFunDaySelected = false
          //  isLengthSelected = false
          //  isWeightSelected = true
            btnTournament.setBackgroundResource(R.color.white)
            btnFunDay.setBackgroundResource(R.color.grey)
            btnLength.visibility = View.VISIBLE
            tglCullingValue.visibility = View.VISIBLE
            spinnerTournamentSpecies.visibility = View.VISIBLE
        }

        // Initialize GPS location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // |||||||||||||| Load saved GPS state ||||||||||||||||||||||||||||||||||

        val isGpsEnabled = sharedPreferences.getBoolean("GPS_ENABLED", false)
        tglGPS.isChecked = isGpsEnabled

        tglGPS.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkAndRequestLocationPermission()
            } else {
                disableGps()
            }
        }

        btnMainSetup.setOnClickListener {
            val intent2 = Intent(this, MainActivity::class.java)
            startActivity(intent2)
        }

        // Load species list from strings.xml
        val speciesList = listOf(
            SpeciesItem("Large Mouth", R.drawable.fish_large_mouth),
            SpeciesItem("Small Mouth", R.drawable.fish_small_mouth),
            SpeciesItem("Crappie", R.drawable.fish_crappie),
            SpeciesItem("Walleye", R.drawable.fish_walleye),
            SpeciesItem("Catfish", R.drawable.fish_catfish),
            SpeciesItem("Perch", R.drawable.fish_perch),
            SpeciesItem("Pike", R.drawable.fish_northern_pike),
            SpeciesItem("Bluegill", R.drawable.fish_bluegill)
        )

        val adapter = SpeciesSpinnerAdapter(this, speciesList)
        spinnerTournamentSpecies.adapter = adapter

// Update selectedSpecies when user picks an item
        spinnerTournamentSpecies.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSpecies = speciesList[position].name

                Log.d("DB_DEBUG", "Species selected: $selectedSpecies")

            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // You can leave this empty if you don’t need to handle it
            }
        }

        // Fishing Event Selection (Fun Day or Tournament)
        btnStartFishing.setOnClickListener {

            val nextActivity = when {
                isFunDaySelected && isWeightSelected && isImperialSelected -> CatchEntryLbsOzs::class.java
                isFunDaySelected && isWeightSelected && isMetricSelected -> CatchEntryKgs::class.java
                isFunDaySelected && isLengthSelected && isImperialSelected -> CatchEntryInches::class.java
                isFunDaySelected && isLengthSelected && isMetricSelected -> CatchEntryMetric::class.java

                isTournamentSelected && isWeightSelected && isImperialSelected -> CatchEntryTournament::class.java
                isTournamentSelected && isWeightSelected && isMetricSelected-> CatchEntryTournamentKgs::class.java
                isTournamentSelected && isLengthSelected && isMetricSelected-> CatchEntryTournamentCentimeters::class.java
                isTournamentSelected && isLengthSelected && isImperialSelected-> CatchEntryTournamentInches::class.java
                else -> null
            }

            if (nextActivity != null) {
                val intent = Intent(this, nextActivity).apply {
                    if (isTournamentSelected) {
                        putExtra("NUMBER_OF_CATCHES", if (tglCullingValue.isChecked) 5 else 4)
                        putExtra("TOURNAMENT_SPECIES", selectedSpecies)
                        putExtra("unitType", if (isWeightSelected) "weight" else "length")
                        putExtra("CULLING_ENABLED", tglCullingValue.isChecked)
                    }
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "⚠️ Please select a valid option!", Toast.LENGTH_SHORT).show()
            }
        }
    }//-------------- END of ON CREATE  _______________________


    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableGps()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableGps()
            } else {
                tglGPS.isChecked = false
                disableGps()  // Ensure GPS is disabled if permissions are denied.
                Toast.makeText(this, "GPS permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun enableGps() {
            sharedPreferences.edit().putBoolean("GPS_ENABLED", true).apply()
            Toast.makeText(this, "GPS enabled", Toast.LENGTH_SHORT).show()
        }

        private fun disableGps() {
            sharedPreferences.edit().putBoolean("GPS_ENABLED", false).apply()
            Toast.makeText(this, "GPS disabled", Toast.LENGTH_SHORT).show()
        }

        companion object {
            private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        }
}
