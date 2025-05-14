package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ListCatchLogView : AppCompatActivity() {

    private lateinit var btnShareData: Button
    private lateinit var btnSetUpCLV : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_catch_log_view)

        btnShareData = findViewById(R.id.btnShareData)
        btnSetUpCLV= findViewById(R.id.btnSetUpCLV)

            // --- Go Back to Share Fishing Logs Activity ---
        btnShareData.setOnClickListener {
            val intent = Intent(this, ShareFishingLogsActivity::class.java)
            startActivity(intent)
        }

        // --- Go To Set-Up Page ---
        btnSetUpCLV.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
        }

    }
}