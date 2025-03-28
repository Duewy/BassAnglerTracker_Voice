package com.bramestorm.bassanglertracker.training

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.MainActivity
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.SetUpActivity

class UserTrainingIndex : AppCompatActivity() {

    private lateinit var btnSetUpUser:Button
    private lateinit var btnMenuUser:Button
    private lateinit var btnFunDayWeight:Button
    private lateinit var btnFunDayLength:Button
    private lateinit var btnTournamentWeight:Button
    private lateinit var btnTournamentLength:Button
    private lateinit var btnWhatIsGPS:Button
    private lateinit var btnMappingGPS:Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_training_index)

        btnSetUpUser = findViewById(R.id.btnSetUpUser)
        btnMenuUser = findViewById(R.id.btnMenuUser)
        btnFunDayWeight = findViewById(R.id.btnFundDayWeight)
        btnFunDayLength = findViewById(R.id.btnFundDayLength)
        btnTournamentWeight = findViewById(R.id.btnTournamentWeight)
        btnTournamentLength = findViewById(R.id.btnTournamentLength)
        btnWhatIsGPS = findViewById(R.id.btnWhatIsGPS)
        btnMappingGPS = findViewById(R.id.btnMappingGPS)



        btnSetUpUser.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
        }

        btnMenuUser.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        // GOOGLE DRIVE to save files on...

        btnFunDayWeight.setOnClickListener {
            val pdfUrl = "https://drive.google.com/file/d/1vya_3-wf4B0FXad4jZ8N0SvXWQLBwKww/view?usp=sharing"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            startActivity(intent)

        }

        btnFunDayLength.setOnClickListener{
            val pdfUrl = "https://drive.google.com/file/d/1nMVrC_QPpsHdZiOIF3RXUyc6-8QtDVZ0/view?usp=sharing"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            startActivity(intent)
        }

        btnTournamentWeight.setOnClickListener{
            val pdfUrl = "https://drive.google.com/file/d/1qmCiOOKdNhDi3-siebhOze3CUvBWGqvM/view?usp=sharing"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            startActivity(intent)
        }

        btnTournamentLength.setOnClickListener {
            val pdfUrl = "https://drive.google.com/file/d/1NyX9nR2BIc5kqcSQ467QkYDvfcyZHt9k/view?usp=sharing"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            startActivity(intent)
        }

        btnWhatIsGPS.setOnClickListener {  }        //FILL in with Google Drive Docs

        btnMappingGPS.setOnClickListener {  }

    }
}