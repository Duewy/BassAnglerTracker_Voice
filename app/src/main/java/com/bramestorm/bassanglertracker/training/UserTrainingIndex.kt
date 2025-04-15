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
    private lateinit var btnFunDayFishing:Button
    private lateinit var btnTournamentFishing:Button
    private lateinit var btnWhatIsGPS:Button
    private lateinit var btnMappingGPS:Button
    private lateinit var btnShareTop5:Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_training_index)

        btnSetUpUser = findViewById(R.id.btnSetUpUser)
        btnMenuUser = findViewById(R.id.btnMenuUser)
        btnFunDayFishing = findViewById(R.id.btnFunDayFishing)
        btnTournamentFishing = findViewById(R.id.btnTournamentFishing)
        btnWhatIsGPS = findViewById(R.id.btnWhatIsGPS)
        btnMappingGPS = findViewById(R.id.btnMappingGPS)
        btnShareTop5 = findViewById(R.id.btnShareTop5)



        btnSetUpUser.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
        }

        btnMenuUser.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        // GitHub Repository has all saved files on...

        btnFunDayFishing.setOnClickListener {
            val pdfUrl = "https://github.com/Duewy/Catch_and_Cull_Help_Files/raw/main/funday_how_to.pdf"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        }

        btnTournamentFishing.setOnClickListener{
            val pdfUrl = "https://github.com/Duewy/Catch_and_Cull_Help_Files/raw/main/Tournament_HowTo.pdf"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        }

        btnShareTop5.setOnClickListener {
            val pdfUrl = "https://github.com/Duewy/Catch_and_Cull_Help_Files/raw/main/Find%20Top%205%20Catches.pdf"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        }

        btnWhatIsGPS.setOnClickListener {
            val pdfUrl = "https://github.com/Duewy/Catch_and_Cull_Help_Files/raw/main/Setup%20GPS.pdf"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        }

        btnMappingGPS.setOnClickListener {
            val pdfUrl = "https://github.com/Duewy/Catch_and_Cull_Help_Files/raw/main/Share%20Catches%20As%20CSV%20File-1.pdf"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        }

    }
}