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
            // Route to the first-time questionnaire if the user hasn't completed it yet,
            // otherwise go straight to MainActivity. Mirrors iOS RootLaunchView logic.
            val destination = if (!FirstTimeQuestionnaireGate.isCompleted(this)) {
                FirstTimeQuestionnaireActivity::class.java
            } else {
                MainActivity::class.java
            }
            startActivity(Intent(this, destination))
            finish() // closes the splash screen so it can't be returned to
        }, 3000)
    }
}
