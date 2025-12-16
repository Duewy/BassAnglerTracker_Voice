package com.bramestorm.bassanglertracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.training.UserTrainingIndex
import com.bramestorm.bassanglertracker.training.UserTrainingVoiceCommands
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.voice.VoiceSetupActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if this is the First time the Catch and Call app has opened
        checkFirstLaunch()



        //--------------------- Set the Initial List for Species ---------------------
        SharedPreferencesManager.initializeDefaultSpeciesIfNeeded(this)

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


        MobileAds.initialize(this) {}


    }// `````````` END On Create  ``````````````````````

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus && shouldShowAdToday()) {
            showAdPopup("main")
        }
    }

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


    private fun showAdPopup(s: String) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val parent = findViewById<View>(android.R.id.content)
        val popupView = inflater.inflate(R.layout.popup_advertisement, parent as ViewGroup, false)

        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true

        val popupWindow = PopupWindow(popupView, width, height, focusable)
        popupWindow.elevation = 10f

        // Show popup at center of screen
        if (!isFinishing && !isDestroyed) {
            popupWindow.showAtLocation(parent, Gravity.CENTER, 0, 0)
        } else {
            Log.w("MapAd", "Activity not in valid state to show popup.")
        }

        // Load the Ad
        val adViewPopup = popupView.findViewById<AdView>(R.id.adViewPopup)
        val adRequest = AdRequest.Builder().build()
        adViewPopup.loadAd(adRequest)

        // Close button
        val closeBtn = popupView.findViewById<Button>(R.id.btnCloseAd)
        closeBtn.setOnClickListener {
            popupWindow.dismiss()
        }
    }

    // only show advertisement once a day...
    private fun shouldShowAdToday(): Boolean {
        val prefs = getSharedPreferences("BassAnglerTrackerPrefs", MODE_PRIVATE)
        val lastShownDate = prefs.getString("LAST_AD_DATE", "")
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        return if (lastShownDate != today) {
            prefs.edit().putString("LAST_AD_DATE", today).apply()
            true
        } else {
            false
        }
    }

}// !!!!!!!!!!!!!!! END MainActivity !!!!!!!!!!!!!!!!!!!!!!!
