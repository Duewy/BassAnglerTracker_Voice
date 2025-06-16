package com.bramestorm.bassanglertracker.training

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.MainActivity
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.SetUpActivity
import com.bramestorm.bassanglertracker.utils.positionedToast
import com.bramestorm.bassanglertracker.voice.VoiceSetupActivity

class UserTrainingVoiceCommands : AppCompatActivity() {

    private lateinit var btnSetUpUser   : Button
    private lateinit var btnMenuUser    : Button
    private lateinit var btnWhatIsVCC   : Button
    private lateinit var btnTeachVCC    : Button
    private lateinit var btnVoiceSetup  :Button



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_training_voice_commands)

        btnSetUpUser = findViewById(R.id.btnSetUpUser)
        btnMenuUser = findViewById(R.id.btnMenuUser)
        btnWhatIsVCC = findViewById(R.id.btnWhatIsVCC)
        btnTeachVCC = findViewById(R.id.btnTeachVCC)
        btnVoiceSetup = findViewById(R.id.btnVoiceSetup)




        btnSetUpUser.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
        }

        btnMenuUser.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnTeachVCC.setOnClickListener {
            val intent = Intent(this, TrainingWords::class.java)
            startActivity(intent)
        }

        // GitHub has saved files on...

        btnWhatIsVCC.setOnClickListener {
            val pdfUrl = "https://raw.githubusercontent.com/Duewy/Catch_and_Cull_Help_Files/main/Voice_Control_Guide.pdf"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                positionedToast("No browser found to open link.")
            }
        }

        // Goto the Voice Set Up page for Bixby or STT / TTS issues
        btnVoiceSetup.setOnClickListener {
            startActivity(Intent(this, VoiceSetupActivity::class.java))
        }


    }//_____________ END On Create ____________________

    }
