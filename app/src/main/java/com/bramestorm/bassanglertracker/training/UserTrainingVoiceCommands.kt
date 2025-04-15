package com.bramestorm.bassanglertracker.training

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.MainActivity
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.SetUpActivity

class UserTrainingVoiceCommands : AppCompatActivity() {

    private lateinit var btnSetUpUser: Button
    private lateinit var btnMenuUser: Button
    private lateinit var btnWhatIsVCC: Button
    private lateinit var btnEnableVCC : Button
    private lateinit var btnTeachVCC : Button



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_training_voice_commands)

        btnSetUpUser = findViewById(R.id.btnSetUpUser)
        btnMenuUser = findViewById(R.id.btnMenuUser)
        btnWhatIsVCC = findViewById(R.id.btnWhatIsVCC)
        btnEnableVCC = findViewById(R.id.btnEnableVCC)
        btnTeachVCC = findViewById(R.id.btnTeachVCC)




        btnSetUpUser.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
        }

        btnMenuUser.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        // GOOGLE DRIVE to save files on...

        btnWhatIsVCC.setOnClickListener {
            val toast = Toast.makeText(this, "Not available on this version\n Check out the Catch and Call with Voice Control", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }

        btnEnableVCC.setOnClickListener {
            val toast = Toast.makeText(this, "Not available on this version\n Check out the Catch and Call with Voice Control", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }

        btnTeachVCC.setOnClickListener {
            val intent = Intent(this, TrainingWords::class.java)
            startActivity(intent)
        }

    }//_____________ END On Create ____________________



}