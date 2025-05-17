package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.bramestorm.bassanglertracker.util.positionedToast
import java.io.File
import java.io.IOException

class ListCatchLogView : AppCompatActivity() {

    private lateinit var btnLookUpCLV: Button
    private lateinit var btnSetUpCLV : Button
    private lateinit var btnMainCLV : Button
    private lateinit var listView : ListView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_catch_log_view)

        btnLookUpCLV = findViewById(R.id.btnLookUpCLV)
        btnMainCLV = findViewById(R.id.btnMainCLV)
        btnSetUpCLV= findViewById(R.id.btnSetUpCLV)

            // --- Go Back to Share Fishing Logs Activity ---
        btnLookUpCLV.setOnClickListener {
            val intent = Intent(this, ShareFishingLogsActivity::class.java)
            startActivity(intent)
        }

        // --- Go To Main Page ---
        btnMainCLV.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // --- Go To Set-Up Page ---
        btnSetUpCLV.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
        }

        // 2) Grab the CSV path from the Intent
        val csvPath = intent.getStringExtra("CSV_FILE_PATH")
        if (csvPath.isNullOrBlank()) {
            positionedToast("No CSV file specified")
            finish()
            return
        }

        // 3) Read all lines from the file
        val rows = mutableListOf<String>()
        try {
            File(csvPath).bufferedReader().useLines { lines ->
                lines.forEach { rows.add(it) }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            positionedToast("Error reading CSV: ${e.localizedMessage}")
            finish()
            return
        }

        // 4) Hook up an ArrayAdapter to display each row as a single list item
        listView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            rows
        )

    }//=== END on Create =========

}// ======= END ========================