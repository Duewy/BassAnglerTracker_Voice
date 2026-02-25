package com.bramestorm.bassanglertracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.training.UserManualModeTrainingIndex
import com.bramestorm.bassanglertracker.training.UserTrainingVoiceCommands
import com.bramestorm.bassanglertracker.utils.positionedToast
import com.bramestorm.bassanglertracker.voice.VoiceSetupActivity
import com.google.android.gms.ads.interstitial.InterstitialAd
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private var dailyInterstitial: InterstitialAd? = null
    private var hasTriedToShowDailyAdThisResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if this is the First time the Catch and Call app has opened
        checkFirstLaunch()

        // ✅ Initialize AdMob once (daily popup uses popup_advertisement AdView)
        if (BuildConfig.FEATURE_DAILY_AD) {
            DailyAdManager.preload(applicationContext)
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


    }// `````````` END On Create  ``````````````````````

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || !BuildConfig.FEATURE_DAILY_AD) return
        if (hasTriedToShowDailyAdThisResume) return
        hasTriedToShowDailyAdThisResume = true

        // ✅ Don’t show an interstitial immediately after questionnaire completion
        if (consumeSkipDailyAdFlag()) return

        if (shouldShowAdToday()) {
            val shown = DailyAdManager.showIfReady(this)
            if (!shown) {
                DailyAdManager.preload(applicationContext)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasTriedToShowDailyAdThisResume = false
    }

    private fun consumeSkipDailyAdFlag(): Boolean {
        val prefs = getSharedPreferences("BassAnglerTrackerPrefs", MODE_PRIVATE)
        val skip = prefs.getBoolean("SKIP_DAILY_AD_ON_NEXT_MAIN", false)
        if (skip) prefs.edit().putBoolean("SKIP_DAILY_AD_ON_NEXT_MAIN", false).apply()
        return skip
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
