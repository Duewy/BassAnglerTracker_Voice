package com.bramestorm.bassanglertracker.training

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.MainActivity
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.SetUpActivity

class UserManualModeTrainingIndex : AppCompatActivity() {

    private lateinit var btnSetUpUser:Button
    private lateinit var btnMenuUser:Button
    private lateinit var btnFunDayFishing:Button
    private lateinit var btnTournamentFishing:Button
    private lateinit var btnWhatIsGPS:Button
    private lateinit var btnMappingGPS:Button
    private lateinit var btnShareTop5:Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_manual_mode_training_index)

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
            val pdfUrl = "https://github.com/Duewy/Catch_and_Call_Help_Files/raw/main/Android_iOS_Fun_Day.pdf"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        }

        btnTournamentFishing.setOnClickListener{
            val pdfUrl = "https://github.com/Duewy/Catch_and_Call_Help_Files/raw/main/Android_iOS_Tournament.pdf"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        }

        btnShareTop5.setOnClickListener {
            val pdfUrl = "https://github.com/Duewy/Catch_and_Call_Help_Files/raw/main/Android_Find_Top_5_Catches.pdf"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        }

        btnWhatIsGPS.setOnClickListener {
            val pdfUrl = "https://github.com/Duewy/Catch_and_Call_Help_Files/raw/main/Android_iOS_Setup_GPS.pdf"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        }

        btnMappingGPS.setOnClickListener {
            val pdfUrl = "https://github.com/Duewy/Catch_and_Call_Help_Files/raw/main/Android_Save_CSV_or_KML_Files.pdf"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        }

    }//========================= END onCreate =============================

}//=====================END =============================================