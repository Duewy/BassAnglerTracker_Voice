package com.bramestorm.bassanglertracker

import android.content.Intent
import android.content.Intent.createChooser
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.utils.positionedToast
import java.io.File
import java.util.Locale

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
            generatedCsvFile = generateCatchLogCsv()

            if (generatedCsvFile != null) {
                positionedToast("CSV generated to cache!")
                // now enable the other two buttons
                btnViewFile.isEnabled = true
                btnShareCSV.isEnabled = true
            } else {
                positionedToast("⚠️ Warning: Failed to generate CSV 📄")
                btnViewFile.isEnabled = false
                btnShareCSV.isEnabled = false
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

    // Real CSV export using data from the SQLite database
// -----  Generate Catch Log to CSV ------
    private fun generateCatchLogCsv(): File? {
        return try {
            // 1) Get all real catches except "practice"
            val dbHelper = CatchDatabaseHelper(this)
            val catches = dbHelper.getAllCatchesExcludingPractice()

            if (catches.isEmpty()) {
                Toast.makeText(this, "No catches found to export.", Toast.LENGTH_SHORT).show()
                return null
            }

            // 2) Create CSV file in cache
            val file = File(cacheDir, "catch_log.csv")
            file.printWriter().use { writer ->
                // ----- Header row based on checkboxes -----
                val headers = mutableListOf<String>()
                if (chkIncludeDate.isChecked)      headers.add("DateTime")
                if (chkIncludeSpecies.isChecked)   headers.add("Species")
                if (chkIncludeWeight.isChecked)    headers.add("Weight")
                if (chkIncludeLength.isChecked)    headers.add("Length")
                if (chkIncludeGPS.isChecked)       headers.add("GPS")
                if (chkIncludeCatchType.isChecked) headers.add("CatchType")

                writer.println(headers.joinToString(","))

                // ----- Data rows -----
                for (catch in catches) {
                    val row = mutableListOf<String>()

                    // Date / Time (already a string like "yyyy-MM-dd HH:mm:ss")
                    if (chkIncludeDate.isChecked) {
                        row.add(catch.dateTime ?: "")
                    }

                    // Species
                    if (chkIncludeSpecies.isChecked) {
                        row.add(catch.species ?: "")
                    }

                    // Weight: choose best available representation
                    if (chkIncludeWeight.isChecked) {
                        val totalOz              = catch.totalWeightOz ?: 0
                        val hundredthPounds      = catch.totalWeightHundredthPounds ?: 0
                        val hundredthKg          = catch.totalWeightHundredthKg ?: 0

                        val weightStr = when {
                            // 1) Stored as total ounces (lbs/oz mode)
                            totalOz > 0 -> {
                                val lbs = totalOz / 16
                                val oz  = totalOz % 16
                                "${lbs}lb ${oz}oz"
                            }

                            // 2) Stored as hundredths of pounds (decimal lbs mode)
                            hundredthPounds > 0 -> {
                                val decLbs = hundredthPounds / 100.0
                                String.format(Locale.getDefault(), "%.2flb", decLbs)
                            }

                            // 3) Stored as hundredths of kg (kg mode)
                            hundredthKg > 0 -> {
                                val kg = hundredthKg / 100.0
                                String.format(Locale.getDefault(), "%.2fkg", kg)
                            }

                            else -> ""
                        }

                        row.add(weightStr)
                    }

                    // Length: choose best available representation
                    if (chkIncludeLength.isChecked) {
                        val quartersTotal = catch.totalLengthQuarters ?: 0
                        val tenthsTotal   = catch.totalLengthTenths ?: 0

                        val lengthStr = when {
                            // 1) Stored as quarters of an inch
                            quartersTotal > 0 -> {
                                val inches   = quartersTotal / 4
                                val quarters = quartersTotal % 4
                                if (quarters == 0) {
                                    "${inches}\""
                                } else {
                                    "${inches} , ${quarters}/4\""
                                }
                            }

                            // 2) Stored as tenths of cm
                            tenthsTotal > 0 -> {
                                val cm = tenthsTotal / 10.0
                                String.format(Locale.getDefault(), "%.1fcm", cm)
                            }

                            else -> ""
                        }

                        row.add(lengthStr)
                    }

                    // GPS (from latitude / longitude)
                    if (chkIncludeGPS.isChecked) {
                        val gpsStr =
                            if ((catch.latitude != 0.0) || (catch.longitude != 0.0)) {
                                "${catch.latitude};${catch.longitude}"
                            } else {
                                ""
                            }
                        row.add(gpsStr)
                    }

                    // Catch type (Fun Day / Tournament / etc.)
                    if (chkIncludeCatchType.isChecked) {
                        row.add(catch.catchType ?: "")
                    }

                    writer.println(row.joinToString(","))
                }
            }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    } //--- END --- Generate Catch Log to CSV -------



}//=== END =======
