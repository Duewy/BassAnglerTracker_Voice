package com.bramestorm.bassanglertracker

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class CatchEntryTournamentKgs : AppCompatActivity() {

    // Buttons
    private lateinit var btnStartFishingKgs: Button
    private lateinit var btnSetUpKgs: Button
    private lateinit var btnMainKgs:Button
    private lateinit var btnAlarmKgs: Button


    // Alarm Variables
    private var alarmHour: Int = -1
    private var alarmMinute: Int = -1
    private var alarmTriggered: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null

    // Weight Display TextViews
    private lateinit var firstRealWeightKgs: TextView
    private lateinit var secondRealWeightKgs: TextView
    private lateinit var thirdRealWeightKgs: TextView
    private lateinit var fourthRealWeightKgs: TextView
    private lateinit var fifthRealWeightKgs: TextView
    private lateinit var sixthRealWeightKgs: TextView

    private lateinit var firstDecWeightKgs: TextView
    private lateinit var secondDecWeightKgs: TextView
    private lateinit var thirdDecWeightKgs: TextView
    private lateinit var fourthDecWeightKgs: TextView
    private lateinit var fifthDecWeightKgs: TextView
    private lateinit var sixthDecWeightKgs: TextView

    private lateinit var decimalTextViews: List<TextView>
    private lateinit var speciesLetters: List<TextView>

    private lateinit var txtTypeLetter1:TextView
    private lateinit var txtTypeLetter2:TextView
    private lateinit var txtTypeLetter3:TextView
    private lateinit var txtTypeLetter4:TextView
    private lateinit var txtTypeLetter5:TextView
    private lateinit var txtTypeLetter6:TextView

    private lateinit var txtKgsColorLetter1:TextView
    private lateinit var txtKgsColorLetter2:TextView
    private lateinit var txtKgsColorLetter3:TextView
    private lateinit var txtKgsColorLetter4:TextView
    private lateinit var txtKgsColorLetter5:TextView
    private lateinit var txtKgsColorLetter6:TextView


    private lateinit var totalRealWeightKgs: TextView
    private lateinit var totalDecWeightKgs: TextView

    private var availableClipColors: List<ClipColor> = emptyList()
    private val flashHandler = Handler(Looper.getMainLooper())


    // Database Helper
    private lateinit var dbHelper: CatchDatabaseHelper

    // Tournament Configuration
    private var tournamentCatchLimit: Int = 4
    private var measurementSystem: String = "weight"
    private var isCullingEnabled: Boolean = false
    private var typeOfMarkers: String = "Color"
    private var tournamentSpecies: String = "Unknown"

    // Request Codes
    private val requestWeightENTRY = 1007
    private val requestAlarmSET = 1008


    // ----------------- wait for POPUP WEIGHT VALUES  ------------------------
    private val weightEntryLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val weightTotalKgs = data?.getIntExtra("weightTotalKgs", 0) ?: 0
            val selectedSpecies = data?.getStringExtra("selectedSpecies") ?: ""
            val clipColor = data?.getStringExtra("clip_color") ?: ""

            Log.d("DB_DEBUG", "✅ Received weightTotalKgs: $weightTotalKgs, selectedSpecies: $selectedSpecies, clip_color: $clipColor")

            if (weightTotalKgs > 0) {
                saveTournamentCatch(weightTotalKgs, selectedSpecies, clipColor)
            }
        }
    }

    //================ ON CREATE =======================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view_kgs)

        dbHelper = CatchDatabaseHelper(this)
        btnStartFishingKgs = findViewById(R.id.btnStartFishingKgs)
        btnSetUpKgs = findViewById(R.id.btnSetUpKgs)
        btnMainKgs = findViewById(R.id.btnMainKgs)
        btnAlarmKgs = findViewById(R.id.btnAlarmKgs)

        // Assign TextViews
        firstRealWeightKgs = findViewById(R.id.firstRealWeightKgs)
        secondRealWeightKgs = findViewById(R.id.secondRealWeightKgs)
        thirdRealWeightKgs = findViewById(R.id.thirdRealWeightKgs)
        fourthRealWeightKgs = findViewById(R.id.fourthRealWeightKgs)
        fifthRealWeightKgs = findViewById(R.id.fifthRealWeightKgs)
        sixthRealWeightKgs = findViewById(R.id.sixthRealWeightKgs)

        firstDecWeightKgs = findViewById(R.id.firstDecWeightKgs)
        secondDecWeightKgs = findViewById(R.id.secondDecWeightKgs)
        thirdDecWeightKgs = findViewById(R.id.thirdDecWeightKgs)
        fourthDecWeightKgs = findViewById(R.id.fourthDecWeightKgs)
        fifthDecWeightKgs = findViewById(R.id.fifthDecWeightKgs)
        sixthDecWeightKgs = findViewById(R.id.sixthDecWeightKgs)

        txtTypeLetter1 = findViewById(R.id.txtTypeLetter1)
        txtTypeLetter2 = findViewById(R.id.txtTypeLetter2)
        txtTypeLetter3 = findViewById(R.id.txtTypeLetter3)
        txtTypeLetter4 = findViewById(R.id.txtTypeLetter4)
        txtTypeLetter5 = findViewById(R.id.txtTypeLetter5)
        txtTypeLetter6 = findViewById(R.id.txtTypeLetter6)

        totalRealWeightKgs = findViewById(R.id.totalRealWeightKgs)
        totalDecWeightKgs = findViewById(R.id.totalDecWeightKgs)

        txtKgsColorLetter1 = findViewById(R.id.txtKgsColorLetter1)
        txtKgsColorLetter2 = findViewById(R.id.txtKgsColorLetter2)
        txtKgsColorLetter3 = findViewById(R.id.txtKgsColorLetter3)
        txtKgsColorLetter4 = findViewById(R.id.txtKgsColorLetter4)
        txtKgsColorLetter5 = findViewById(R.id.txtKgsColorLetter5)
        txtKgsColorLetter6 = findViewById(R.id.txtKgsColorLetter6)



        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)

        btnStartFishingKgs.setOnClickListener { showWeightPopup() }
        btnSetUpKgs.setOnClickListener { startActivity(Intent(this, SetUpActivity::class.java)) }
        btnMainKgs.setOnClickListener { startActivity(Intent(this,MainActivity::class.java)) }
        btnAlarmKgs.setOnClickListener { startActivityForResult(Intent(this, PopUpAlarm::class.java), requestAlarmSET) }
        val dbHelper = CatchDatabaseHelper(this)

        updateTournamentList()
        handler.postDelayed(checkAlarmRunnable, 60000)
    }
// ~~~~~~~~~~~~~~~~~~~~~ END ON CREATE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    override fun onDestroy() {
        super.onDestroy()

        // Clean up all handlers and media players
        handler.removeCallbacksAndMessages(null)
        flashHandler.removeCallbacksAndMessages(null)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    /** ~~~~~~~~~~~~~ Opens the weight entry popup ~~~~~~~~~~~~~~~ */

    private fun showWeightPopup() {
        val intent = Intent(this, PopupWeightEntryTourKgs::class.java)
        intent.putExtra("isTournament", true)

        if (tournamentSpecies.equals("Large Mouth", true) || tournamentSpecies.equals("Largemouth", true))  {
            intent.putExtra("tournamentSpecies", "Large Mouth Bass")
        } else         if (tournamentSpecies.equals("Small Mouth", true) || tournamentSpecies.equals("Smallmouth", true))  {
            intent.putExtra("tournamentSpecies", "Small Mouth Bass")
        } else{
            intent.putExtra("tournamentSpecies", tournamentSpecies)
        }

        // 🔥 Send available clip colors as String array
        val colorNames = availableClipColors.map { it.name }.toTypedArray()
        intent.putExtra("availableClipColors", colorNames)

        weightEntryLauncher.launch(intent)
    }

    // ^^^^^^^^^^^^^ SAVE TOURNAMENT CATCH ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    private fun saveTournamentCatch(weightTotalKgs: Int, bassType: String, clipColor: String) {
        val availableColors = calculateAvailableClipColors(
            dbHelper,
            catchType = "Kgs",
            date = getCurrentDate(),
            tournamentCatchLimit = tournamentCatchLimit,
            isCullingEnabled = isCullingEnabled
        )
        val cleanClipColor = clipColor.uppercase() // This came from the popup

        val speciesInitial = if (bassType == "Large Mouth") "L" else "S"

        Log.d("DB_DEBUG", "✅ Assigned Clip Color: $cleanClipColor")

        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = bassType,
            totalWeightOz = null,
            totalLengthA8th = null,
            totalWeightHundredthKg = weightTotalKgs,
            totalLengthTenths = null,
            catchType = "Kgs",
            markerType = speciesInitial,
            clipColor = cleanClipColor
        )

        val result = dbHelper.insertCatch(catch)
        Log.d("DB_DEBUG", "✅ Catch Insert Result: $result, Stored Clip Color: ${catch.clipColor}")

        Toast.makeText(this, "$bassType Catch Saved!", Toast.LENGTH_SHORT).show()


        updateTournamentList()
    }

    private fun getTopTournamentCatches(): List<CatchItem> {
        val allCatches = dbHelper.getCatchesForToday("Kgs", getCurrentDate())
        val sorted = allCatches.sortedByDescending { it.totalWeightHundredthKg ?: 0 }
        return if (isCullingEnabled) sorted.take(tournamentCatchLimit) else sorted
    }


    // ``````````````` UPDATE TOTAL WEIGHT ``````````````````````
    private fun updateTotalWeight(tournamentCatches: List<CatchItem>) {
        // Always sort and limit to top N
        val catchesToUse = tournamentCatches
            .sortedByDescending { it.totalWeightHundredthKg ?: 0 }
            .take(tournamentCatchLimit)  // ✅ Apply limit always

        val totalWeightKgs = catchesToUse.sumOf { it.totalWeightHundredthKg ?: 0 }
        val totalKgs = totalWeightKgs / 100
        val totalDec = totalWeightKgs % 100

        totalRealWeightKgs.text = totalKgs.toString()
        totalDecWeightKgs.text = totalDec.toString()

        // !!!!!!!!!!!!!!!!!!!! MOTIVATIONAL TOASTS !!!!!!!!!!!!!!!!!!!!!!!!!!!
        val currentCount = dbHelper
            .getCatchesForToday("LbsOzs", getCurrentDate())
            .sortedByDescending { it.totalWeightOz ?: 0 }
            .take(tournamentCatchLimit)
            .size
        if (currentCount >= 2) {
            val message = getMotivationalMessage(currentCount, tournamentCatchLimit)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }



    //################## UPDATE TOURNAMENT LIST   ###################################

    private fun updateTournamentList() {
        val formattedDate = getCurrentDate()
        val realWeightKgs = listOf(
            firstRealWeightKgs, secondRealWeightKgs, thirdRealWeightKgs,
            fourthRealWeightKgs, fifthRealWeightKgs, sixthRealWeightKgs
        )

        val decWeightKgs = listOf(
            firstDecWeightKgs, secondDecWeightKgs, thirdDecWeightKgs,
            fourthDecWeightKgs, fifthDecWeightKgs, sixthDecWeightKgs
        )
        val allCatches = dbHelper.getCatchesForToday(catchType = "Kgs", formattedDate)
        val sortedCatches = allCatches.sortedByDescending { it.totalWeightHundredthKg ?: 0 }
        val tournamentCatches = if (isCullingEnabled) {
            sortedCatches.take(tournamentCatchLimit)
        } else {
            sortedCatches
        }

        // ✨ Clean way to update available clip colors
        availableClipColors = calculateAvailableClipColors(
            dbHelper,
            catchType = "Kgs",
            date = formattedDate,
            tournamentCatchLimit = tournamentCatchLimit,
            isCullingEnabled = isCullingEnabled
        )

        Log.d("CLIP_COLOR", "🎨 Available Colors: $availableClipColors")

        clearTournamentTextViews()

        runOnUiThread {
            for (i in tournamentCatches.indices) {
                if (i >= realWeightKgs.size || i >= decWeightKgs.size) continue

                val catch = tournamentCatches[i]
                val totalWeightKgs = catch.totalWeightHundredthKg ?: 0
                val weightKgs = totalWeightKgs / 100
                val weightDec = totalWeightKgs % 100

                val clipColor = try {
                    ClipColor.valueOf(catch.clipColor?.uppercase() ?: "")
                } catch (_: Exception) {
                    ClipColor.RED
                }

                realWeightKgs[i].text = weightKgs.toString()
                decWeightKgs[i].text = weightDec.toString()

                realWeightKgs[i].setBackgroundResource(clipColor.resId)
                decWeightKgs[i].setBackgroundResource(clipColor.resId)

                val baseColor = ContextCompat.getColor(this, clipColor.resId)
                val layeredDrawable = createLayeredDrawable(baseColor)
                realWeightKgs[i].background = layeredDrawable
                decWeightKgs[i].background = layeredDrawable



                val textColor = if (clipColor == ClipColor.BLUE)
                    resources.getColor(R.color.clip_white, theme)
                else
                    resources.getColor(R.color.black, theme)

                realWeightKgs[i].setTextColor(textColor)
                decWeightKgs[i].setTextColor(textColor)

                realWeightKgs[i].invalidate()
                decWeightKgs[i].invalidate()
            }
            // 👇 ADD THIS FOR COLORBLIND ACCESSIBILITY 👇
            val colorLetters = listOf(
                txtKgsColorLetter1, txtKgsColorLetter2, txtKgsColorLetter3,
                txtKgsColorLetter4, txtKgsColorLetter5, txtKgsColorLetter6
            )

            colorLetters.forEach { it.text = "" } // Clear first

            for (i in tournamentCatches.indices) {
                if (i >= tournamentCatchLimit || i >= colorLetters.size) break

                val catch = tournamentCatches[i]
                val colorInitial = when (catch.clipColor?.uppercase()) {
                    "BLUE" -> "B"
                    "RED" -> "R"
                    "GREEN" -> "G"
                    "YELLOW" -> "Y"
                    "ORANGE" -> "O"
                    "WHITE" -> "W"
                    else -> "?"
                }
                colorLetters[i].text = colorInitial

                val speciesCode = getSpeciesCode(catch.species ?: "")
                when (i) {
                    0 -> txtTypeLetter1.text = speciesCode
                    1 -> txtTypeLetter2.text = speciesCode
                    2 -> txtTypeLetter3.text = speciesCode
                    3 -> txtTypeLetter4.text = speciesCode
                    4 -> txtTypeLetter5.text = speciesCode
                    5 -> txtTypeLetter6.text = speciesCode
                }

            }

        }

        updateTotalWeight(tournamentCatches)
        adjustTextViewVisibility()

        if (tournamentCatches.size >= tournamentCatchLimit) {
            Handler(Looper.getMainLooper()).postDelayed({
                when (tournamentCatchLimit) {
                    4 -> {
                        blinkTextViewTwice(fourthRealWeightKgs)
                        blinkTextViewTwice(fourthDecWeightKgs)
                    }

                    5 -> {
                        blinkTextViewTwice(fifthRealWeightKgs)
                        blinkTextViewTwice(fifthDecWeightKgs)
                    }

                    6 -> {
                        blinkTextViewTwice(sixthRealWeightKgs)
                        blinkTextViewTwice(sixthDecWeightKgs)
                    }
                }
            }, 300) // Wait
        }


    }


    //########### Clear Tournament Text Views  ########################

    private fun clearTournamentTextViews() {
        firstRealWeightKgs.text = ""
        secondRealWeightKgs.text = ""
        thirdRealWeightKgs.text = ""
        fourthRealWeightKgs.text = ""
        fifthRealWeightKgs.text = ""
        sixthRealWeightKgs.text = ""

        firstDecWeightKgs.text = ""
        secondDecWeightKgs.text = ""
        thirdDecWeightKgs.text = ""
        fourthDecWeightKgs.text = ""
        fifthDecWeightKgs.text = ""
        sixthDecWeightKgs.text = ""

        totalRealWeightKgs.text = "0"
        totalDecWeightKgs.text = "0"
    }

    // %%%%%%%%%%%% Clip Color assignment  %%%%%%%%%%%%%%%%%%%%%%%

    enum class ClipColor(val resId: Int) {
        RED(R.color.clip_red),
        BLUE(R.color.clip_blue),
        GREEN(R.color.clip_green),
        YELLOW(R.color.clip_yellow),
        ORANGE(R.color.clip_orange),
        WHITE(R.color.clip_white);
    }

    //????????????? AVAILABLE COLORS   ???????????????????????
    private fun calculateAvailableClipColors(
        dbHelper: CatchDatabaseHelper,
        catchType: String,
        date: String,
        tournamentCatchLimit: Int,
        isCullingEnabled: Boolean
    ): List<ClipColor> {
        val allCatches = dbHelper.getCatchesForToday(catchType, date)
        val sorted = allCatches.sortedByDescending { it.totalWeightHundredthKg ?: 0 }
        val topCatches = sorted.take(tournamentCatchLimit) // ✅ Always limit to top N

        val usedColors = topCatches.mapNotNull { it.clipColor }
            .mapNotNull {
                try { ClipColor.valueOf(it.uppercase()) } catch (_: Exception) { null }
            }
            .toSet()

        return ClipColor.entries.filter { it !in usedColors }
    }



    // ~~~~~~~~~~~~~ ADJUST TEXT VIEW VIABILITY for culling values ~~~~~~~~~~~~~
    private fun adjustTextViewVisibility() {
        when (tournamentCatchLimit) {
            4 -> {
                fifthRealWeightKgs.alpha = 0.3f
                fifthDecWeightKgs.alpha = 0.3f
                fifthRealWeightKgs.isEnabled = false
                fifthDecWeightKgs.isEnabled = false
                sixthRealWeightKgs.visibility = View.INVISIBLE
                sixthDecWeightKgs.visibility = View.INVISIBLE
            }
            5 -> {
                sixthRealWeightKgs.alpha = 0.3f
                sixthDecWeightKgs.alpha = 0.3f
                sixthRealWeightKgs.isEnabled = false
                sixthDecWeightKgs.isEnabled = false
                txtTypeLetter6.isEnabled = false
            }
            else -> {
                fifthRealWeightKgs.alpha = 1.0f
                fifthDecWeightKgs.alpha = 1.0f
                fifthRealWeightKgs.isEnabled = true
                fifthDecWeightKgs.isEnabled = true
                sixthRealWeightKgs.visibility = View.VISIBLE
                sixthDecWeightKgs.visibility = View.VISIBLE
                sixthRealWeightKgs.alpha = 1.0f
                sixthDecWeightKgs.alpha = 1.0f
                sixthRealWeightKgs.isEnabled = true
                sixthDecWeightKgs.isEnabled = true
            }
        }
    }

    //!!!!!!!!!!!!!!!! Get SPECIES Letter !!!!!!!!!!!!!!!!!

    private fun getSpeciesCode(species: String): String {
        return when (species.uppercase()) {
            "LARGE MOUTH" -> "LM"
            "SMALL MOUTH" -> "SM"
            "WALLEYE"     -> "WE"
            "PIKE"        -> "PK"
            "PERCH"       -> "PH"
            "PANFISH"     -> "PF"
            "CATFISH"     -> "CF"
            "CRAPPIE"     -> "CP"
            else          -> "--"
        }
    }

//!!!!!!!!!!!!!!!!! MOTIVATIONAL MESSAGES !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    private fun getMotivationalMessage(currentCount: Int, totalNeeded: Int): String {
        val remaining = totalNeeded - currentCount

        val messages = listOf(
            "🔥 You're on fire!",
            "🎣 Keep casting, you're almost there!",
            "💪 One catch at a time!",
            "🏆 That one’s a game-changer!",
            "👏 Nice pull! You're stacking ‘em!",
            "🚀 You're climbing that leaderboard!",
            "🌊 The lake is yours today!",
            "💯 Crushing it, keep going!",
            "🎉 You’ve got $currentCount so far — only $remaining to go!",
            "💥 That puts you at $currentCount — get that next one!"
        )

        return messages.random()
    }


    // +++++++++++++++++ CHECK ALARM ++++++++++++++++++++++++

    private val checkAlarmRunnable = object : Runnable {
        override fun run() {
            val calendar = Calendar.getInstance()
            val nowHour = calendar.get(Calendar.HOUR_OF_DAY)
            val nowMinute = calendar.get(Calendar.MINUTE)

            Log.d("ALARM_DEBUG", "🕒 Checking alarm... Now: $nowHour:$nowMinute, Set: $alarmHour:$alarmMinute")

            if (!alarmTriggered && nowHour == alarmHour && nowMinute == alarmMinute) {
                alarmTriggered = true
                Log.d("ALARM_DEBUG", "🔔 Alarm triggered!")
                startAlarm()
            }

            if (!alarmTriggered) {
                handler.postDelayed(this, 60000)
            }
        }
    }


    // Start Alarm
    private fun startAlarm() {
        // ✅ Ensure raw file exists
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        mediaPlayer?.start()

        val flashHandler = Handler()
        var isRed = true
        val flashRunnable = object : Runnable {
            override fun run() {
                btnAlarmKgs.setBackgroundColor(if (isRed) Color.RED else Color.WHITE)
                isRed = !isRed
                flashHandler.postDelayed(this, 500)
            }
        }

        flashHandler.post(flashRunnable)

        handler.postDelayed({
            mediaPlayer?.stop()
            mediaPlayer?.release()
            btnAlarmKgs.setBackgroundColor(Color.TRANSPARENT)
            flashHandler.removeCallbacks(flashRunnable)
        }, 4000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("ALARM_DEBUG", "📥 onActivityResult triggered with requestCode=$requestCode")

        if (requestCode == requestAlarmSET && resultCode == Activity.RESULT_OK) {
            alarmHour = data?.getIntExtra("ALARM_HOUR", -1) ?: -1
            alarmMinute = data?.getIntExtra("ALARM_MINUTE", -1) ?: -1
            alarmTriggered = false // ✅ reset so the alarm can trigger again

            Log.d("ALARM_DEBUG", "✅ Alarm Set - hour=$alarmHour, minute=$alarmMinute")

            if (alarmHour != -1 && alarmMinute != -1) {
                val amPm = if (alarmHour >= 12) "PM" else "AM"
                val displayHour = if (alarmHour % 12 == 0) 12 else alarmHour % 12
                val formattedMinute = String.format("%02d", alarmMinute)

                val timeString = "$displayHour:$formattedMinute $amPm"
                btnAlarmKgs.text = "⏰ $timeString"

                Toast.makeText(this, "Alarm Set for $timeString", Toast.LENGTH_SHORT).show()
            }
        }
    }


    //++++++++++++++++ Date and Time  +++++++++++++++++++++++++++++
    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    //************** DATE *****************************
    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // @@@@@@@@@@@@@ BLINK  @@@@@@@@@@@@@@@@@@@@@@@@@
    private fun blinkTextViewTwice(textView: TextView) {
        val blink = AnimationUtils.loadAnimation(this, R.anim.blink)

        // Delay 1 second before first blink
        Handler(Looper.getMainLooper()).postDelayed({
            textView.startAnimation(blink)

            // Delay slightly before doing the second blink
            Handler(Looper.getMainLooper()).postDelayed({
                textView.startAnimation(blink)
            }, 700) // Wait ~1 blink duration
        }, 1000) // Initial 1 second delay
    }

//+++++++++++++++++++++ Create Boarder Around Clip Color  ++++++++++++++++++++

    private fun createLayeredDrawable(baseColor: Int): Drawable {
        val colorDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 4f
            setColor(baseColor)
        }

        val borderDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 4f
            setStroke(4, Color.BLACK) // 4dp border
            setColor(Color.TRANSPARENT) // Don't cover the base
        }

        return LayerDrawable(arrayOf(colorDrawable, borderDrawable))
    }

}//################## END  ################################