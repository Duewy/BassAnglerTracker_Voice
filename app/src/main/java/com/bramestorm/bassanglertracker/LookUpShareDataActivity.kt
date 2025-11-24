package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LookUpShareDataActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_look_up_share_data)

        //---------------  GoTo Share Logs Page button ---------------------
        val btnShareLogs = findViewById<Button>(R.id.btnShareLogs)
        btnShareLogs.setOnClickListener {
            val intent = Intent(this, ShareFishingLogsActivity::class.java)
            startActivity(intent)
        }

        //---------------- GoTo Google Map Lookup button ----------------
        val btnLookUpMap = findViewById<Button>(R.id.btnLookUpMap)
        btnLookUpMap.setOnClickListener {
            val intent = Intent(this, MapCatchLocationsActivity::class.java)
            startActivity(intent)
        }
        // ------------ GoTo Top 5 Page button -------------------
        val btnGoToTop5Page= findViewById<Button>(R.id.btnGoToTop5Page)
        btnGoToTop5Page.setOnClickListener {
            val intent = Intent(this, TopFiveCatchesActivity::class.java)
            startActivity(intent)
        }

       //-------------- GoTo SetUp Page button ----------------------
        val btnSetUpSharedLookUp= findViewById<Button>(R.id.btnSetUpSharedLookUp)
        btnSetUpSharedLookUp.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
        }


    }//----------------- END OnCreate ------------------

}//----------------------- END  ----------------------------
