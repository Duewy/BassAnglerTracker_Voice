package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.questionnaire.FirstTimeQuestionnaireActivity
import com.bramestorm.bassanglertracker.questionnaire.FirstTimeQuestionnaireGate

class IntroPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.intro_page)

        DailyAdManager.preload(applicationContext)

        Handler(Looper.getMainLooper()).postDelayed({
            val nextActivity = if (FirstTimeQuestionnaireGate.isCompleted(this)) {
                MainActivity::class.java
            } else {
                FirstTimeQuestionnaireActivity::class.java
            }
            startActivity(Intent(this, nextActivity))
            finish() // closes the splash screen so it can't be returned to
        }, 3000) // wait for 3 seconds then route to questionnaire or main page
    }
}
