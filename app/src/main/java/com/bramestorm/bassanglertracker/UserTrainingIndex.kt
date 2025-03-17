package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class UserTrainingIndex : AppCompatActivity() {

    private lateinit var btnSetUpUser:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_training_index)

        btnSetUpUser = findViewById<Button>(R.id.btnSetUpUser)

        btnSetUpUser.setOnClickListener {
            val intent = Intent(this,SetUpActivity::class.java)
            startActivity(intent)
        }


    }
}