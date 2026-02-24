package com.bramestorm.bassanglertracker

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.bramestorm.bassanglertracker.training.UserManualModeTrainingIndex
import com.bramestorm.bassanglertracker.training.UserTrainingVoiceCommands
import com.bramestorm.bassanglertracker.utils.positionedToast
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


        // ---------------- Open Set-Up page --------------------------------
        val openSetUpActivity = findViewById<Button>(R.id.btnSetUp11)
        openSetUpActivity.setOnClickListener {
            val intent = Intent(this,SetUpActivity::class.java)
            startActivity(intent)
        }

    //-------------------------- Open the Manual TRAINING INDEX ------------------------
        val btnManualControls = findViewById<Button>(R.id.btnManualControls)
        btnManualControls.setOnClickListener{
            val intent1 = Intent(this, UserManualModeTrainingIndex::class.java)
            startActivity(intent1)
        }

        //-------------------------- Open the Voice Control TRAINING INDEX ------------------------
        val btnVCC = findViewById<Button>(R.id.btnVCC)
        btnVCC.setOnClickListener {
            if (BuildConfig.FEATURE_VOICE_COMMANDS) {
                startActivity(Intent(this, UserTrainingVoiceCommands::class.java))
            } else {
                positionedToast(
                    "Voice Controls are available in the Pro VC edition only.\n" +
                            "Upgrade to enable hands‑free catch logging."
                )
            }
        }

        //-------------------------- Open the See & Share / Google MAPS ------------------------
        val btnLookUpShareData = findViewById<Button>(R.id.btnLookUpShareData)
        btnLookUpShareData.setOnClickListener {
            if (BuildConfig.FEATURE_GPS_LOGGING) {
                startActivity(Intent(this, LookUpShareDataActivity::class.java))
            } else {
                positionedToast(
                    "See & Share is available in the Tracker or Pro VC editions.\n" +
                            "Upgrade to unlock catch mapping and sharing."
                )
            }
        }

        //-------------------------- Open the Privacy Policy pdf 📝 --------------------
        val btnPrivacyPolicy = findViewById<Button>(R.id.btnPrivacyPolicy)
        btnPrivacyPolicy.setOnClickListener {
            val pdfUrl = "https://github.com/Duewy/Catch_and_Cull_Help_Files/raw/main/Privacy_Policy.pdf"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        }

        if (BuildConfig.FEATURE_DAILY_AD && hasFocus && shouldShowAdToday()) {
            showAdPopup("main")
        }



    }// `````````` END On Create  ``````````````````````

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (BuildConfig.FEATURE_DAILY_AD && hasFocus && shouldShowAdToday()) {
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

            // ✅ Only Pro VC needs the full voice setup flow
            if (BuildConfig.FEATURE_VOICE_COMMANDS) {
                positionedToast(
                    "One-time setup: enable your phone’s voice system for hands‑free logging.\n" +
                            "Bluetooth headset setup is included."
                )
                startActivity(Intent(this, VoiceSetupActivity::class.java))
            }

            // Prevent this from running again (all editions)
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
