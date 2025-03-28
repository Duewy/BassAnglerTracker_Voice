package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.training.UserTrainingIndex
import com.bramestorm.bassanglertracker.training.UserTrainingVoiceCommands

class MainActivity : AppCompatActivity() {

    private lateinit var btnManualControls:Button
    private lateinit var btnSetUp11:Button
    private lateinit var btnVCC:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnManualControls = findViewById<Button>(R.id.btnManualControls)
        btnManualControls.setOnClickListener{
            val intent1 = Intent(this, UserTrainingIndex::class.java)
            startActivity(intent1)
        }

        val btnVCC = findViewById<Button>(R.id.btnVCC)
        btnVCC.setOnClickListener {
            val intent = Intent(this, UserTrainingVoiceCommands::class.java)
            startActivity(intent)
        }

        val openSetUpActivity = findViewById<Button>(R.id.btnSetUp11)
        openSetUpActivity.setOnClickListener {
            val intent = Intent(this,SetUpActivity::class.java)
            startActivity(intent)
        }


    }// `````````` END On Create  ``````````````````````


}// !!!!!!!!!!!!!!! END MainActivity !!!!!!!!!!!!!!!!!!!!!!!
