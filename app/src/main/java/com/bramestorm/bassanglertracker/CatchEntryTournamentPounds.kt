package com.bramestorm.bassanglertracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.bramestorm.bassanglertracker.base.BaseCatchEntryActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.utils.GpsUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatchEntryTournamentLBS : BaseCatchEntryActivity() {

    private lateinit var btnStartFishingLBS: Button
    private lateinit var btnSetUpLBS: Button
    private lateinit var btnMainLBS: Button
    private lateinit var txtVCCTourLBS: TextView
    private lateinit var txtGPSNotice: TextView

    private lateinit var firstRealWeightLBS: TextView
    private lateinit var firstDecWeightLBS: TextView
    private lateinit var secondRealWeightLBS: TextView
    private lateinit var secondDecWeightLBS: TextView
    private lateinit var thirdRealWeightLBS: TextView
    private lateinit var thirdDecWeightLBS: TextView
    private lateinit var fourthRealWeightLBS: TextView
    private lateinit var fourthDecWeightLBS: TextView
    private lateinit var fifthRealWeightLBS: TextView
    private lateinit var fifthDecWeightLBS: TextView
    private lateinit var sixthRealWeightLBS: TextView
    private lateinit var sixthDecWeightLBS: TextView

    private lateinit var totalRealWeightLBS: TextView
    private lateinit var totalDecWeightLBS: TextView

    private lateinit var dbHelper: CatchDatabaseHelper
    private var tournamentCatchLimit: Int = 4
    private var lastTournamentCatch: CatchItem? = null

    override val dialog: Any
        get() = throw UnsupportedOperationException("BaseCatchEntryActivity.dialog is unused in this subclass")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view_pounds_decimal)

        dbHelper = CatchDatabaseHelper(this)

        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)

        btnStartFishingLBS = findViewById(R.id.btnStartFishingLBS)
        btnSetUpLBS = findViewById(R.id.btnMainPg)
        btnMainLBS = findViewById(R.id.btnMenu)
        txtVCCTourLBS = findViewById(R.id.txtVCCTourLbs)
        txtGPSNotice = findViewById(R.id.txtGPSNotice)

        firstRealWeightLBS = findViewById(R.id.firstRealWeightLBS)
        firstDecWeightLBS = findViewById(R.id.firstDecWeightLBS)
        secondRealWeightLBS = findViewById(R.id.secondRealWeightLBS)
        secondDecWeightLBS = findViewById(R.id.secondDecWeightLBS)
        thirdRealWeightLBS = findViewById(R.id.thirdRealWeightLBS)
        thirdDecWeightLBS = findViewById(R.id.thirdDecWeightLBS)
        fourthRealWeightLBS = findViewById(R.id.fourthRealWeightLBS)
        fourthDecWeightLBS = findViewById(R.id.fourthDecWeightLBS)
        fifthRealWeightLBS = findViewById(R.id.fifthRealWeightLBS)
        fifthDecWeightLBS = findViewById(R.id.fifthDecWeightLBS)
        sixthRealWeightLBS = findViewById(R.id.sixthRealWeightLBS)
        sixthDecWeightLBS = findViewById(R.id.sixthDecWeightLBS)

        totalRealWeightLBS = findViewById(R.id.totalRealWeightLBS)
        totalDecWeightLBS = findViewById(R.id.totalDecWeightLBS)

        btnStartFishingLBS.setOnClickListener {
            Toast.makeText(this, "TODO: Open Decimal Pound Popup", Toast.LENGTH_SHORT).show()
        }

        btnSetUpLBS.setOnClickListener {
            startActivity(Intent(this, SetUpActivity::class.java))
        }

        btnMainLBS.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        GpsUtils.updateGpsStatusLabel(txtGPSNotice, this)
        updateTournamentList()
    }

    override fun onSpeechResult(transcript: String) {
        TODO("Not yet implemented")
    }

    private fun updateTournamentList() {
        val todaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val sorted = dbHelper.getCatchesForToday("decimalLBS", todaysDate)
            .sortedByDescending { it.totalWeightPounds ?: 0 }
            .take(tournamentCatchLimit)

        val realViews = listOf(firstRealWeightLBS, secondRealWeightLBS, thirdRealWeightLBS, fourthRealWeightLBS, fifthRealWeightLBS, sixthRealWeightLBS)
        val decViews = listOf(firstDecWeightLBS, secondDecWeightLBS, thirdDecWeightLBS, fourthDecWeightLBS, fifthDecWeightLBS, sixthDecWeightLBS)

        val total = sorted.sumOf { it.totalWeightPounds ?: 0 }
        val lbs = total / 100
        val dec = total % 100
        totalRealWeightLBS.text = lbs.toString()
        totalDecWeightLBS.text = String.format("%02d", dec)

        for (i in sorted.indices) {
            val valLbs = (sorted[i].totalWeightPounds ?: 0) / 100
            val valDec = (sorted[i].totalWeightPounds ?: 0) % 100
            realViews[i].text = valLbs.toString()
            decViews[i].text = String.format("%02d", valDec)
        }
    }

    private fun getCurrentDateTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }
}
