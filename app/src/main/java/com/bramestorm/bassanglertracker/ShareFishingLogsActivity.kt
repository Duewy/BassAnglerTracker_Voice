package com.bramestorm.bassanglertracker

import android.content.Intent
import android.content.Intent.createChooser
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bramestorm.bassanglertracker.utils.positionedToast
import java.io.File

class ShareFishingLogsActivity : AppCompatActivity() {

    private lateinit var chkIncludeDate: CheckBox
    private lateinit var chkIncludeSpecies: CheckBox
    private lateinit var chkIncludeWeight: CheckBox
    private lateinit var chkIncludeLength: CheckBox
    private lateinit var chkIncludeGPS: CheckBox
    private lateinit var chkIncludeCatchType: CheckBox
    private lateinit var btnGenerateCSV: Button
    private lateinit var btnViewFile : Button
    private lateinit var btnShareCSV: Button
    private lateinit var btnSetUpSFLogs :Button
    private lateinit var btnMainSFL :Button


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
        btnViewFile = findViewById(R.id.btnViewFile)
        btnShareCSV = findViewById(R.id.btnShareCSV)
        btnSetUpSFLogs= findViewById(R.id.btnSetUpSFLogs)
        btnMainSFL  = findViewById(R.id.btnMainSFL)

        //  start with View/Share disabled
        btnViewFile.isEnabled = false
        btnShareCSV.isEnabled = false


        //------------- Create Data into CSV ️🖋️-------------------
        //  generate CSV
        btnGenerateCSV.setOnClickListener {
            generatedCsvFile = generateDummyCatchLogCsv()
            if (generatedCsvFile != null) {
                positionedToast("CSV generated to cache!")
                // now enable the other two buttons
                btnViewFile.isEnabled = true
                btnShareCSV.isEnabled = true
            } else {
                positionedToast("⚠️ Warning: Failed to generate CSV 📄")
            }
        }


        //--------------- Share the Data with CSV 📝----------------------
        // share via other text email apps
        btnShareCSV.setOnClickListener {
            generatedCsvFile?.let { file ->
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
                Intent(Intent.ACTION_SEND).run {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(createChooser(this, "Share fishing log via:"))
                }
            }
        }

        // ----------🥽 See the Files Yourself 😍----------------------------
        //  view the CSV in your ListCatchLogView
        btnViewFile.setOnClickListener {
            generatedCsvFile?.let { file ->
                Intent(this, ListCatchLogView::class.java).apply {
                    putExtra("CSV_FILE_PATH", file.absolutePath)
                }.also { startActivity(it) }
            } ?: positionedToast("⚠️ Please generate the CSV first")
        }


        //-------------- Goto SetUp page ---------------------
        btnSetUpSFLogs.setOnClickListener {
            val intent = Intent(this, SetUpActivity::class.java)
            startActivity(intent)
        }

        // ---------- Goto Main Page ------------------------
        btnMainSFL.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
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

            //todo Remove for APP Release 🚨
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
                    if (chkIncludeWeight.isChecked) row.add("${4 + i} lbs $i oz")
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
