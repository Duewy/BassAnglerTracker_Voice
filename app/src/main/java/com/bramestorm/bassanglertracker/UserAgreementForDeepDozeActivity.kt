package com.bramestorm.bassanglertracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.positionedToast

class UserAgreementForDeepDozeActivity : AppCompatActivity() {

    private lateinit var btnDisagree: Button
    private lateinit var btnAgree: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_agreement_for_deep_doze)

        btnDisagree = findViewById(R.id.btnDisagree)
        btnAgree = findViewById(R.id.btnAgree)

        btnAgree.setOnClickListener {
            // ✅ Save preference that user agreed
            SharedPreferencesManager.setUserAgreedToDeepDoze(this, true)

            val resultIntent = Intent().apply {
                putExtra("USER_AGREED", true)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        btnDisagree.setOnClickListener {
            positionedToast("⚠️ Voice Control may not function while your phone is asleep.")
            val resultIntent = Intent().apply {
                putExtra("USER_AGREED", false)
            }
            setResult(RESULT_CANCELED, resultIntent)
            finish()
        }
    }

    companion object {
        fun hasUserAgreed(context: Context): Boolean {
            return SharedPreferencesManager.hasUserAgreedToDeepDoze(context)
        }
    }
}
