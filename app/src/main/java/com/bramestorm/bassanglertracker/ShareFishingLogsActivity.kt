package com.bramestorm.bassanglertracker

import android.content.Intent
import android.content.Intent.createChooser
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
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
    private lateinit var btnGenerateKML: Button
    private lateinit var btnViewFile : Button
    private lateinit var btnShareCSV: Button
    private lateinit var btnShareKLM: Button
    private lateinit var btnSetUpSFLogs :Button
    private lateinit var btnMainSFL :Button

    private var generatedCsvFile: File? = null
    private var generatedKmlFile: File? = null


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
        btnGenerateKML = findViewById(R.id.btnGenerateKML)

        btnViewFile = findViewById(R.id.btnViewFile)

        btnShareCSV = findViewById(R.id.btnShareCSV)
        btnShareKLM = findViewById(R.id.btnShareKLM)

        btnSetUpSFLogs= findViewById(R.id.btnSetUpSFLogs)
        btnMainSFL  = findViewById(R.id.btnMainSFL)

        //  start with View/Share disabled
        btnViewFile.isEnabled = false
        btnShareCSV.isEnabled = false
        btnShareKLM.isEnabled = false


        //------------- Create Data into CSV ️ & KLM (for Google Earth or other map software) 🖋️-------------------

        //  generate CSV Files
        btnGenerateCSV.setOnClickListener {

            // 🔒 Require at least one checkbox
            if (!hasAnyFieldSelected()) {
                positionedToast("Please select at least one field to include.")
                return@setOnClickListener
            }

            val csvFile = generateCatchLogCsv()
            generatedCsvFile = csvFile

            if (csvFile != null) {
                positionedToast("CSV generated!")
                btnViewFile.isEnabled = true
                btnShareCSV.isEnabled = true
            } else {
                positionedToast("⚠️ Failed to generate CSV")
                btnViewFile.isEnabled = false
                btnShareCSV.isEnabled = false
            }
        }

        //  generate KLM Files
        btnGenerateKML.setOnClickListener {

            // 🔒 Require at least one checkbox
            if (!hasAnyFieldSelected()) {
                positionedToast("Please select at least one field to include.")
                return@setOnClickListener
            }

            val kmlFile = generateCatchLogKml()
            generatedKmlFile = kmlFile

            if (kmlFile != null) {
                positionedToast("KML generated for Google Earth!")
                btnShareKLM.isEnabled = true
            } else {
                positionedToast("⚠️ Failed to generate KML")
                btnShareKLM.isEnabled = false
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

        btnShareKLM.setOnClickListener {
            generatedKmlFile?.let { file ->
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
                Intent(Intent.ACTION_SEND).run {
                    type = "application/vnd.google-earth.kml+xml"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(createChooser(this, "Share KML via:"))
                }
            } ?: positionedToast("⚠️ Please generate the KML first")
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

        btnShareCSV.isEnabled = false
        btnShareKLM.isEnabled = false
        btnViewFile.isEnabled = false
        generatedCsvFile = null
        generatedKmlFile = null

    }//-------------- END onResume --------------------

    // ---------------------------------------
        // HELPER: Did the user select any fields?
    // ---------------------------------------
    private fun hasAnyFieldSelected(): Boolean {
        return chkIncludeDate.isChecked ||
                chkIncludeSpecies.isChecked ||
                chkIncludeWeight.isChecked ||
                chkIncludeLength.isChecked ||
                chkIncludeGPS.isChecked ||
                chkIncludeCatchType.isChecked
    }



    // Real CSV export using data from the SQLite database
        // -----  Generate Catch Log to CSV ------
    // ---------------------------------------
        // CSV FIELD ESCAPER  (prevents broken rows)
    // ---------------------------------------
    private fun toCsvField(raw: String?): String {
        val s = raw ?: ""
        return if (s.contains(',') || s.contains('"') || s.contains('\n')) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else {
            s
        }
    }

    // ---------------------------------------
        // FIXED INCHES FORMATTER  (NO COMMAS!)
    // ---------------------------------------
    private fun formatInchesLength(quartersTotal: Int): String {
        if (quartersTotal <= 0) return ""
        val inches = quartersTotal / 4
        val q = quartersTotal % 4

        return if (q == 0) {
            "${inches}\""
        } else {
            "$inches ${q}/4\""
        }
    }

    // ---------------------------------------
        // ADD DATE TO FILE NAME
    // ---------------------------------------
    private fun buildCsvFileName(): String {
        val sdf = java.text.SimpleDateFormat("dd_MMM_yyyy", Locale.getDefault())
        val today = sdf.format(java.util.Date())
        return "catch_log_${today}.csv"
    }

    // ---------------------------------------
        // MAIN CSV GENERATOR (FULLY FIXED)
    // ---------------------------------------
    private fun generateCatchLogCsv(): File? {
        return try {
            val dbHelper = CatchDatabaseHelper(this)
            val catches = dbHelper.getAllCatchesExcludingPractice()

            if (catches.isEmpty()) {
                positionedToast(" No catches found to export.")
               return null
            }

            // ------- USE DATED FILE NAME -------
            val fileName = buildCsvFileName()
            val file = File(cacheDir, fileName)

            file.printWriter().use { writer ->

                // ----- HEADER -----
                val headers = mutableListOf<String>()
                if (chkIncludeDate.isChecked)      headers.add("Date/Time")
                if (chkIncludeSpecies.isChecked)   headers.add("Species")
                if (chkIncludeWeight.isChecked)    headers.add("Weight")
                if (chkIncludeLength.isChecked)    headers.add("Length")
                if (chkIncludeCatchType.isChecked) headers.add("CatchType")
                if (chkIncludeGPS.isChecked)       headers.add("GPS")

                writer.println(headers.joinToString(","))

                // ----- DATA ROWS -----
                for (catch in catches) {
                    val row = mutableListOf<String>()

                    // Date/time
                    if (chkIncludeDate.isChecked)
                        row.add(catch.dateTime ?: "")

                    // Species
                    if (chkIncludeSpecies.isChecked)
                        row.add(catch.species ?: "")

                    // Weight
                    if (chkIncludeWeight.isChecked) {
                        val totalOz = catch.totalWeightOz ?: 0
                        val hundredthPounds = catch.totalWeightHundredthPounds ?: 0
                        val hundredthKg = catch.totalWeightHundredthKg ?: 0

                        val weightStr = when {
                            totalOz > 0 -> {
                                val lbs = totalOz / 16
                                val oz = totalOz % 16
                                "${lbs}lb ${oz}oz"
                            }
                            hundredthPounds > 0 -> {
                                String.format(Locale.getDefault(),
                                    "%.2flb", hundredthPounds / 100.0)
                            }
                            hundredthKg > 0 -> {
                                String.format(Locale.getDefault(),
                                    "%.2fkg", hundredthKg / 100.0)
                            }
                            else -> ""
                        }

                        row.add(weightStr)
                    }

                    // Length
                    if (chkIncludeLength.isChecked) {
                        val quarters = catch.totalLengthQuarters ?: 0
                        val tenths  = catch.totalLengthTenths ?: 0

                        val lengthStr = when {
                            quarters > 0 -> formatInchesLength(quarters)
                            tenths > 0 -> String.format(Locale.getDefault(),
                                "%.1fcm", tenths / 10.0)
                            else -> ""
                        }

                        row.add(lengthStr)
                    }

                    // Catch type
                    if (chkIncludeCatchType.isChecked)
                        row.add(catch.catchType ?: "")

                    // GPS
                    if (chkIncludeGPS.isChecked) {
                        val gps = if (catch.latitude != null && catch.longitude != null) {
                            "${catch.latitude},${catch.longitude}"
                        } else {
                            ""
                        }
                        row.add(gps)
                    }

                    // WRITE CSV SAFELY (must be after all row fields are added)
                writer.println(row.joinToString(",") { toCsvField(it) })

                }
            }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    //--- END --- Generate Catch Log to CSV -------

    // ------------------------------------------------------------
        // GOOGLE EARTH: Pick correct icon for FunDay vs Tournament
    // ------------------------------------------------------------
    private fun getKmlIconForCatch(catchType: String?, clipColor: String?): String {

        // ---- Fun Day → fishing icon ----
        if (catchType.equals("Fun Day", ignoreCase = true) ||
            catchType.equals("fun", ignoreCase = true)) {
            return "http://maps.google.com/mapfiles/kml/shapes/fishing.png"
        }

        // ---- Tournament → use clip colors ----
        return when (clipColor?.lowercase(Locale.getDefault())) {
            "yellow" -> "http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png"
            "blue"   -> "http://maps.google.com/mapfiles/kml/pushpin/blue-pushpin.png"
            "green"  -> "http://maps.google.com/mapfiles/kml/pushpin/grn-pushpin.png"
            "red"    -> "http://maps.google.com/mapfiles/kml/pushpin/red-pushpin.png"
            "white"  -> "http://maps.google.com/mapfiles/kml/pushpin/wht-pushpin.png"

            // No orange → substitute with pink
            "orange" -> "http://maps.google.com/mapfiles/kml/pushpin/pink-pushpin.png"

            else     -> "http://maps.google.com/mapfiles/kml/pushpin/wht-pushpin.png"
        }
    }

    // ------------------------------------------------------------
        // GOOGLE EARTH: Generate a full KML file of all catches
        // ------------------------------------------------------------
    private fun generateCatchLogKml(): File? {
        return try {
            val dbHelper = CatchDatabaseHelper(this)
            val catches = dbHelper.getAllCatchesExcludingPractice()

            if (catches.isEmpty()) {
                positionedToast("No catches found for KML export.")
                return null
            }

            // ----- Create KML filename -----
            val sdf = java.text.SimpleDateFormat("dd_MMM_yyyy", Locale.getDefault())
            val today = sdf.format(java.util.Date())
            val fileName = "catch_log_$today.kml"

            val file = File(cacheDir, fileName)


            file.printWriter().use { writer ->
                // ---- KML HEADER ----
                writer.println("""<?xml version="1.0" encoding="UTF-8"?>""")
                writer.println("""<kml xmlns="http://www.opengis.net/kml/2.2">""")
                writer.println("<Document>")
                writer.println("<name>$fileName</name>")

                // ---- LOOP THROUGH CATCHES ----
                for (catch in catches) {

                    // ignore empty GPS
                    if (catch.latitude == null || catch.longitude == null) continue

                    val iconUrl = getKmlIconForCatch(catch.catchType, catch.clipColor)

                    // Build readable description text
                    val weightStr = when {
                        (catch.totalWeightOz ?: 0) > 0 -> {
                            val oz = catch.totalWeightOz!!
                            val lbs = oz / 16
                            val remOz = oz % 16
                            "${lbs}lb ${remOz}oz"
                        }
                        (catch.totalWeightHundredthPounds ?: 0) > 0 -> {
                            String.format("%.2flb", (catch.totalWeightHundredthPounds!! / 100.0))
                        }
                        (catch.totalWeightHundredthKg ?: 0) > 0 -> {
                            String.format("%.2fkg", (catch.totalWeightHundredthKg!! / 100.0))
                        }
                        else -> ""
                    }

                    val lengthStr = when {
                        (catch.totalLengthQuarters ?: 0) > 0 -> {
                            val q = catch.totalLengthQuarters!!
                            val i = q / 4
                            val rem = q % 4
                            if (rem == 0) "${i}\"" else "$i ${rem}/4\""
                        }
                        (catch.totalLengthTenths ?: 0) > 0 -> {
                            String.format("%.1fcm", catch.totalLengthTenths!! / 10.0)
                        }
                        else -> ""
                    }

                    // ---- GOOGLE EARTH PLACEMARK ----
                    writer.println("<Placemark>")
                    writer.println("<name>${catch.species}</name>")

                    // CDATA allows HTML in popup
                    writer.println("<description><![CDATA[")
                    writer.println("<b>Date:</b> ${catch.dateTime}<br/>")
                    if (weightStr.isNotEmpty()) writer.println("<b>Weight:</b> $weightStr<br/>")
                    if (lengthStr.isNotEmpty()) writer.println("<b>Length:</b> $lengthStr<br/>")
                    writer.println("<b>Type:</b> ${catch.catchType}<br/>")
                    if (!catch.clipColor.isNullOrEmpty())
                        writer.println("<b>Clip Color:</b> ${catch.clipColor}<br/>")
                    writer.println("]]></description>")

                    writer.println("<Style><IconStyle><Icon><href>$iconUrl</href></Icon></IconStyle></Style>")

                    // KML uses lon,lat,altitude (altitude = 0)
                    writer.println("<Point>")
                    writer.println("<coordinates>${catch.longitude},${catch.latitude},0</coordinates>")
                    writer.println("</Point>")

                    writer.println("</Placemark>")
                }

                // ---- KML FOOTER ----
                writer.println("</Document>")
                writer.println("</kml>")
            }

            file

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }// === END == Generate KLM File ========



}//=== END =======
