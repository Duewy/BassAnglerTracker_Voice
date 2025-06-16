package com.bramestorm.bassanglertracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.training.UserTrainingIndex
import com.bramestorm.bassanglertracker.training.UserTrainingVoiceCommands
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.voice.VoiceSetupActivity


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if this is the First time the Catch and Call app has opened
        checkFirstLaunch()

        //--------------------- Set the Initial List for Species ---------------------
        if (!SharedPreferencesManager.isSpeciesInitialized(this)) {
            val default8 = listOf("Large Mouth", "Small Mouth", "Crappie", "Walleye", "Catfish", "Perch", "Pike", "Bluegill")
            SharedPreferencesManager.saveSelectedSpeciesList(this, default8)
            SharedPreferencesManager.saveAllSpecies(this, SharedPreferencesManager.getAllSpecies(this))
            SharedPreferencesManager.setSpeciesInitialized(this, true)
            Log.d("MainActivity", "Species initialized with default 8: $default8")
        } else {
            val selected = SharedPreferencesManager.getSelectedSpeciesList(this)
            Log.d("MainActivity", "Species already initialized. Loaded selected: $selected")
        }

        // ---------------- Open Set-Up page --------------------------------
        val openSetUpActivity = findViewById<Button>(R.id.btnSetUp11)
        openSetUpActivity.setOnClickListener {
            val intent = Intent(this,SetUpActivity::class.java)
            startActivity(intent)
        }

    //-------------------------- Open the Manual TRAINING INDEX ------------------------
        val btnManualControls = findViewById<Button>(R.id.btnManualControls)
        btnManualControls.setOnClickListener{
            val intent1 = Intent(this, UserTrainingIndex::class.java)
            startActivity(intent1)
        }

        //-------------------------- Open the Voice Control TRAINING INDEX ------------------------
        val btnVCC = findViewById<Button>(R.id.btnVCC)
        btnVCC.setOnClickListener {
           //val intent = Intent(this, UserTrainingVoiceCommands::class.java)
            intent = Intent(this, UserTrainingVoiceCommands::class.java)
            startActivity(intent)
        }

        //-------------------------- Open the Google MAPS ------------------------
        val btnLookUpShareData = findViewById<Button>(R.id.btnLookUpShareData)
        btnLookUpShareData.setOnClickListener {
            val intent = Intent(this, LookUpShareDataActivity::class.java)
            startActivity(intent)
        }

    }// `````````` END On Create  ``````````````````````

    // Check if this is the User's first Time Opening the Catch and Call App
    // if so then they will have to set up the proper STT and TTS as well as
    // turn off back ground apps such as Bixby

    private fun checkFirstLaunch() {
        val prefs = getSharedPreferences("BassAnglerTrackerPrefs", Context.MODE_PRIVATE)
        val firstLaunch = prefs.getBoolean("FIRST_LAUNCH_COMPLETE", false)

        if (!firstLaunch) {
            // Launch voice setup guide
            startActivity(Intent(this, VoiceSetupActivity::class.java))

            // Prevent this from running again
            prefs.edit().putBoolean("FIRST_LAUNCH_COMPLETE", true).apply()
        }
    }


}// !!!!!!!!!!!!!!! END MainActivity !!!!!!!!!!!!!!!!!!!!!!!
