package com.bramestorm.bassanglertracker.share

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.SetUpActivity
import java.io.File

class ShareFishingLogsActivity : AppCompatActivity() {

    private lateinit var chkIncludeDate: CheckBox
    private lateinit var chkIncludeSpecies: CheckBox
    private lateinit var chkIncludeWeight: CheckBox
    private lateinit var chkIncludeLength: CheckBox
    private lateinit var chkIncludeGPS: CheckBox
    private lateinit var chkIncludeCatchType: CheckBox
    private lateinit var btnGenerateCSV: Button
    private lateinit var btnShareCSV: Button
    private lateinit var btnSetUpSFLogs :Button

    private var generatedCsvFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_fishing_logs)

        chkIncludeDate = findViewById(R.id.chkIncludeDate)
        chkIncludeSpecies = findViewById(R.id.chkIncludeSpecies)
        chkIncludeWeight = findViewById(R.id.chkIncludeWeight)
        chkIncludeLength = findViewById(R.id.chkIncludeLength)
        chkIncludeGPS = findViewById(R.id.chkIncludeGPS)
        chkIncludeCatchType = findViewById(R.id.chkIncludeCatchType)

        btnGenerateCSV = findViewById(R.id.btnGenerateCSV)
        btnShareCSV = findViewById(R.id.btnShareCSV)
        btnSetUpSFLogs= findViewById(R.id.btnSetUpSFLogs)

        //------------- Create Data into CSV -------------------
        btnGenerateCSV.setOnClickListener {
            generatedCsvFile = generateDummyCatchLogCsv()

            if (generatedCsvFile != null) {
                Toast.makeText(this, "CSV generated to cache!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "⚠️ Warning: Failed to generate CSV", Toast.LENGTH_SHORT).show()
            }
        }

        //--------------- Share the Data with CSV ----------------------
        btnShareCSV.setOnClickListener {
            generatedCsvFile?.let { file ->
                val uri = FileProvider.getUriForFile(
                    this,
                    "com.bramestorm.bassanglertracker.fileprovider", // match manifest
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share fishing log via:"))
            } ?: run {
                Toast.makeText(this,  "⚠️ Warning: Please generate the CSV first", Toast.LENGTH_SHORT).show()
            }
        }

        //-------------- Goto SetUp page ---------------------
        btnSetUpSFLogs.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
        }


    }//----------------- END OnCreate -------------------------------


    override fun onResume() {
        super.onResume()
        //------------ Clear all the Checked Values to Start Fresh ---------------------
        chkIncludeDate.isChecked = false
        chkIncludeSpecies.isChecked = false
        chkIncludeWeight.isChecked = false
        chkIncludeLength.isChecked = false
        chkIncludeGPS.isChecked = false
        chkIncludeCatchType.isChecked = false

    }//-------------- END onResume --------------------


    private fun generateDummyCatchLogCsv(): File? {
        return try {
            val file = File(cacheDir, "catch_log.csv")
            file.printWriter().use { writer ->
                val headers = mutableListOf<String>()
                if (chkIncludeDate.isChecked) headers.add("Date")
                if (chkIncludeSpecies.isChecked) headers.add("Species")
                if (chkIncludeWeight.isChecked) headers.add("Weight")
                if (chkIncludeLength.isChecked) headers.add("Length")
                if (chkIncludeGPS.isChecked) headers.add("GPS")
                if (chkIncludeCatchType.isChecked) headers.add("Catch Type")
                writer.println(headers.joinToString(","))

                // Dummy data row
                for (i in 1..5) {
                    val row = mutableListOf<String>()
                    if (chkIncludeDate.isChecked) row.add("2025-04-0$i 07:00")
                    if (chkIncludeSpecies.isChecked) row.add("Bass")
                    if (chkIncludeWeight.isChecked) row.add("${4 + i} lbs ${i} oz")
                    if (chkIncludeLength.isChecked) row.add("${15 + i}\"")
                    if (chkIncludeGPS.isChecked) row.add("Lat: 43.12$i, Lng: -79.32$i")
                    if (chkIncludeCatchType.isChecked) row.add(if (i % 2 == 0) "Fun Day" else "Tournament")
                    writer.println(row.joinToString(","))
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
