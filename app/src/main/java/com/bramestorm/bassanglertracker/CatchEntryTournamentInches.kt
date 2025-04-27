package com.bramestorm.bassanglertracker

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.alarm.AlarmReceiver
import com.bramestorm.bassanglertracker.base.BaseCatchEntryActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.training.ParsedCatch
import com.bramestorm.bassanglertracker.training.VoiceCatchParse
import com.bramestorm.bassanglertracker.utils.GpsUtils
import com.bramestorm.bassanglertracker.utils.getMotivationalMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class CatchEntryTournamentInches : BaseCatchEntryActivity()  {


    // Buttons
    private lateinit var btnTournamentCatch:Button
    private lateinit var btnMainInches: Button
    private lateinit var btnAlarmInches: Button
    private lateinit var btnSetUpInches: Button


    // Alarm Variables
    private var alarmHour: Int = -1
    private var alarmMinute: Int = -1
    private var alarmTriggered: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null

    // Weight Display TextViews
    private lateinit var firstRealLengthInches: TextView
    private lateinit var secondRealLengthInches: TextView
    private lateinit var thirdRealLengthInches: TextView
    private lateinit var fourthRealLengthInches: TextView
    private lateinit var fifthRealLengthInches: TextView
    private lateinit var sixthRealLengthInches: TextView

    private lateinit var firstDecLengthInches: TextView
    private lateinit var secondDecLengthInches: TextView
    private lateinit var thirdDecLengthInches: TextView
    private lateinit var fourthDecLengthInches: TextView
    private lateinit var fifthDecLengthInches: TextView
    private lateinit var sixthDecLengthInches: TextView

    private lateinit var txtTypeLetterInches1:TextView
    private lateinit var txtTypeLetterInches2:TextView
    private lateinit var txtTypeLetterInches3:TextView
    private lateinit var txtTypeLetterInches4:TextView
    private lateinit var txtTypeLetterInches5:TextView
    private lateinit var txtTypeLetterInches6:TextView

    private lateinit var txtInchesColorLetter1:TextView
    private lateinit var txtInchesColorLetter2:TextView
    private lateinit var txtInchesColorLetter3:TextView
    private lateinit var txtInchesColorLetter4:TextView
    private lateinit var txtInchesColorLetter5:TextView
    private lateinit var txtInchesColorLetter6:TextView


    private lateinit var totalRealLengthInches: TextView
    private lateinit var totalDecLengthInches: TextView

    private lateinit var txtGPSNotice: TextView


    private var availableClipColors: List<ClipColor> = emptyList()
    private val flashHandler = Handler(Looper.getMainLooper())


    // Database Helper
    private lateinit var dbHelper: CatchDatabaseHelper


    // --- voice-to-text callback handler ---
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            Toast.makeText(this@CatchEntryTournamentInches, "Speech error $error", Toast.LENGTH_SHORT).show()
        }
        override fun onResults(results: Bundle) {
            results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.let { onSpeechResult(it) }
        }
        override fun onPartialResults(partial: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }


    // Tournament Configuration
    private var tournamentCatchLimit: Int = 4
    private var measurementSystem: String = "weight"
    private var isCullingEnabled: Boolean = false
    private var typeOfMarkers: String = "Color"
    private var tournamentSpecies: String = "Unknown"
    private var lastTournamentCatch: CatchItem? = null

    // Request Codes
    private val requestAlarmSET = 1012


    // ----------------- wait for POPUP WEIGHT VALUES  ------------------------
    private val lengthEntryLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val totalLengthQuarters = data?.getIntExtra("lengthTotalInches", 0) ?: 0
            val selectedSpecies = data?.getStringExtra("selectedSpecies") ?: ""
            val clipColor = data?.getStringExtra("clip_color") ?: ""

            Log.d("DB_DEBUG", "✅ Received totalLengthQuarters: $totalLengthQuarters, selectedSpecies: $selectedSpecies, clip_color: $clipColor")

            if (totalLengthQuarters > 0) {
                saveTournamentCatch(totalLengthQuarters, selectedSpecies, clipColor)
            }
        }
    }

    //================ ON CREATE =======================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view_inches)

        //******  Initialize speech recognizer ***********************
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(recognitionListener)
        }
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        dbHelper = CatchDatabaseHelper(this)
        btnTournamentCatch = findViewById(R.id.btnStartFishingInches)
        btnMainInches = findViewById(R.id.btnMainInches)
        btnSetUpInches = findViewById(R.id.btnSetUpInches)
        btnAlarmInches = findViewById(R.id.btnAlarmInches)
        txtGPSNotice = findViewById(R.id.txtGPSNotice)

        // Assign TextViews
        firstRealLengthInches = findViewById(R.id.firstRealLengthInches)
        secondRealLengthInches = findViewById(R.id.secondRealLengthInches)
        thirdRealLengthInches = findViewById(R.id.thirdRealLengthInches)
        fourthRealLengthInches = findViewById(R.id.fourthRealLengthInches)
        fifthRealLengthInches = findViewById(R.id.fifthRealLengthInches)
        sixthRealLengthInches = findViewById(R.id.sixthRealLengthInches)

        firstDecLengthInches = findViewById(R.id.firstDecLengthInches)
        secondDecLengthInches = findViewById(R.id.secondDecLengthInches)
        thirdDecLengthInches = findViewById(R.id.thirdDecLengthInches)
        fourthDecLengthInches = findViewById(R.id.fourthDecLengthInches)
        fifthDecLengthInches = findViewById(R.id.fifthDecLengthInches)
        sixthDecLengthInches = findViewById(R.id.sixthDecLengthInches)

        txtTypeLetterInches1 = findViewById(R.id.txtTypeLetterInches1)
        txtTypeLetterInches2 = findViewById(R.id.txtTypeLetterInches2)
        txtTypeLetterInches3 = findViewById(R.id.txtTypeLetterInches3)
        txtTypeLetterInches4 = findViewById(R.id.txtTypeLetterInches4)
        txtTypeLetterInches5 = findViewById(R.id.txtTypeLetterInches5)
        txtTypeLetterInches6 = findViewById(R.id.txtTypeLetterInches6)

        totalRealLengthInches = findViewById(R.id.totalRealLengthInches)
        totalDecLengthInches = findViewById(R.id.totalDecLengthInches)

        txtTypeLetterInches1 = findViewById(R.id.txtTypeLetterInches1)
        txtTypeLetterInches2 =findViewById(R.id.txtTypeLetterInches2)
        txtTypeLetterInches3 = findViewById(R.id.txtTypeLetterInches3)
        txtTypeLetterInches4= findViewById(R.id.txtTypeLetterInches4)
        txtTypeLetterInches5= findViewById(R.id.txtTypeLetterInches5)
        txtTypeLetterInches6= findViewById(R.id.txtTypeLetterInches6)

        txtInchesColorLetter1 = findViewById(R.id.txtInchesColorLetter1)
        txtInchesColorLetter2 = findViewById(R.id.txtInchesColorLetter2)
        txtInchesColorLetter3 = findViewById(R.id.txtInchesColorLetter3)
        txtInchesColorLetter4 = findViewById(R.id.txtInchesColorLetter4)
        txtInchesColorLetter5 = findViewById(R.id.txtInchesColorLetter5)
        txtInchesColorLetter6 = findViewById(R.id.txtInchesColorLetter6)



        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)

        btnTournamentCatch.setOnClickListener { showWeightPopup() }
        btnSetUpInches.setOnClickListener { startActivity(Intent(this, SetUpActivity::class.java)) }
        btnMainInches.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        btnAlarmInches.setOnClickListener { startActivityForResult(Intent(this, PopUpAlarm::class.java), requestAlarmSET) }
        val dbHelper = CatchDatabaseHelper(this)

        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)

        updateTournamentList()
        handler.postDelayed(checkAlarmRunnable, 60000)
    }
// ~~~~~~~~~~~~~~~~~~~~~ END ON CREATE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // ------------- On RESUME --------- Check GPS  Statues --------------
    override fun onResume() {
        super.onResume()
        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)
    }

    // +++++++++++++ On-Destroy +++++++++++++++++++

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
        val intent = Intent(this, PopupLengthEntryTourInches::class.java)
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

        lengthEntryLauncher.launch(intent)
    }

    // ^^^^^^^^^^^^^ SAVE TOURNAMENT CATCH ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    private fun saveTournamentCatch(totalLengthQuarters: Int, bassType: String, clipColor: String) {
        val availableColors = calculateAvailableClipColors(
            dbHelper,
            catchType = "inches",
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
            totalLengthTenths = null,
            totalWeightHundredthKg = null,
            totalLengthQuarters = totalLengthQuarters,
            catchType = "inches",
            markerType = speciesInitial,
            clipColor = cleanClipColor
        )
        val result = dbHelper.insertCatch(catch)

        Toast.makeText(this, "$bassType Catch Saved!", Toast.LENGTH_SHORT).show()

        if (result) {
            lastTournamentCatch = catch
        }
        updateTournamentList()
    }

    // ``````````````` UPDATE TOTAL LENGTH ``````````````````````

    private fun updateTotalLength(tournamentCatches: List<CatchItem>) {
        // Always sort and limit to top N
        val catchesToUse = tournamentCatches
            .sortedByDescending { it.totalLengthQuarters ?: 0 }
            .take(tournamentCatchLimit)  // ✅ Apply limit always

        val totalLengthInches = catchesToUse.sumOf { it.totalLengthQuarters ?: 0 }
        val totalInches = (totalLengthInches / 8)
        val totalDec = (totalLengthInches % 8)

        totalRealLengthInches.text = totalInches.toString()
        totalDecLengthInches.text = "$totalDec /8"

// !!!!!!!!!!!!!!!!!!!! MOTIVATIONAL TOASTS !!!!!!!!!!!!!!!!!!!!!!!!!!!
        val currentCount = dbHelper
            .getCatchesForToday("inches", getCurrentDate())
            .sortedByDescending { it.totalLengthQuarters ?: 0 }
            .take(tournamentCatchLimit)
            .size

        if (currentCount >= 2) {
            lastTournamentCatch?.let {
                val message = getMotivationalMessage(this, it.id, tournamentCatchLimit, "inches")
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //################## UPDATE TOURNAMENT LIST   ###################################
    private fun updateTournamentList() {
        val formattedDate = getCurrentDate()

        val realLengthInches = listOf(
            firstRealLengthInches, secondRealLengthInches, thirdRealLengthInches,
            fourthRealLengthInches, fifthRealLengthInches, sixthRealLengthInches
        )

        val decLengthInches = listOf(
            firstDecLengthInches, secondDecLengthInches, thirdDecLengthInches,
            fourthDecLengthInches, fifthDecLengthInches, sixthDecLengthInches
        )

        val colorLetters = listOf(
            txtInchesColorLetter1, txtInchesColorLetter2, txtInchesColorLetter3,
            txtInchesColorLetter4, txtInchesColorLetter5, txtInchesColorLetter6
        )

        val typeLetters = listOf(
            txtTypeLetterInches1, txtTypeLetterInches2, txtTypeLetterInches3,
            txtTypeLetterInches4, txtTypeLetterInches5, txtTypeLetterInches6
        )

        val allCatches = dbHelper.getCatchesForToday(catchType = "inches", formattedDate)
        val sortedCatches = allCatches.sortedByDescending { it.totalLengthQuarters ?: 0 }

        val tournamentCatches = if (isCullingEnabled) {
            sortedCatches.take(tournamentCatchLimit)
        } else {
            sortedCatches
        }

        availableClipColors = calculateAvailableClipColors(
            dbHelper,
            catchType = "inches",
            date = formattedDate,
            tournamentCatchLimit = tournamentCatchLimit,
            isCullingEnabled = isCullingEnabled
        )

        Log.d("CLIP_COLOR", "🎨 Available Colors: $availableClipColors")

        clearTournamentTextViews()

        runOnUiThread {
            val loopLimit = minOf(sortedCatches.size, 6)

            for (i in 0 until loopLimit) {
                if (i >= realLengthInches.size) continue

                val catch = sortedCatches[i]
                val totalLengthInches = catch.totalLengthQuarters ?: 0
                val lengthInches = totalLengthInches / 8
                val lengthDec = totalLengthInches % 8

                val clipColor = try {
                    ClipColor.valueOf(catch.clipColor?.uppercase() ?: "")
                } catch (_: Exception) {
                    ClipColor.RED
                }

                realLengthInches[i].text = lengthInches.toString()
                decLengthInches[i].text = "$lengthDec /8"

                val baseColor = ContextCompat.getColor(this, clipColor.resId)
                val layeredDrawable = createLayeredDrawable(baseColor)
                realLengthInches[i].background = layeredDrawable
                decLengthInches[i].background = layeredDrawable

                val textColor = if (clipColor == ClipColor.BLUE)
                    resources.getColor(R.color.clip_white, theme)
                else
                    resources.getColor(R.color.black, theme)

                realLengthInches[i].setTextColor(textColor)
                decLengthInches[i].setTextColor(textColor)

                realLengthInches[i].invalidate()
                decLengthInches[i].invalidate()

                colorLetters[i].text = when (clipColor.name) {
                    "BLUE" -> "B"
                    "RED" -> "R"
                    "GREEN" -> "G"
                    "YELLOW" -> "Y"
                    "ORANGE" -> "O"
                    "WHITE" -> "W"
                    else -> "?"
                }

                typeLetters[i].text = getSpeciesCode(catch.species ?: "")
            }

            updateTotalLength(tournamentCatches)
            adjustTextViewVisibility()

            if (tournamentCatches.size >= tournamentCatchLimit) {
                Handler(Looper.getMainLooper()).postDelayed({
                    when (tournamentCatchLimit) {
                        4 -> {
                            blinkTextViewTwice(fourthRealLengthInches)
                            blinkTextViewTwice(fourthDecLengthInches)
                        }
                        5 -> {
                            blinkTextViewTwice(fifthRealLengthInches)
                            blinkTextViewTwice(fifthDecLengthInches)
                        }
                        6 -> {
                            blinkTextViewTwice(sixthRealLengthInches)
                            blinkTextViewTwice(sixthDecLengthInches)
                        }
                    }
                }, 300)
            }
        }
    }

    //########### Clear Tournament Text Views  ########################

    private fun clearTournamentTextViews() {
        firstRealLengthInches.text = ""
        secondRealLengthInches.text = ""
        thirdRealLengthInches.text = ""
        fourthRealLengthInches.text = ""
        fifthRealLengthInches.text = ""
        sixthRealLengthInches.text = ""

        firstDecLengthInches.text = ""
        secondDecLengthInches.text = ""
        thirdDecLengthInches.text = ""
        fourthDecLengthInches.text = ""
        fifthDecLengthInches.text = ""
        sixthDecLengthInches.text = ""

        totalRealLengthInches.text = "0"
        totalDecLengthInches.text = "0"
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
        val sorted = allCatches.sortedByDescending { it.totalLengthQuarters ?: 0 }
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
                fifthRealLengthInches.alpha = 0.3f
                fifthDecLengthInches.alpha = 0.3f
                fifthRealLengthInches.isEnabled = false
                fifthDecLengthInches.isEnabled = false
                sixthRealLengthInches.visibility = View.INVISIBLE
                sixthDecLengthInches.visibility = View.INVISIBLE
            }
            5 -> {
                sixthRealLengthInches.alpha = 0.3f
                sixthDecLengthInches.alpha = 0.3f
                sixthRealLengthInches.isEnabled = false
                sixthDecLengthInches.isEnabled = false
                txtTypeLetterInches6.isEnabled = false
            }
            else -> {
                fifthRealLengthInches.alpha = 1.0f
                fifthDecLengthInches.alpha = 1.0f
                fifthRealLengthInches.isEnabled = true
                fifthDecLengthInches.isEnabled = true
                sixthRealLengthInches.visibility = View.VISIBLE
                sixthDecLengthInches.visibility = View.VISIBLE
                sixthRealLengthInches.alpha = 1.0f
                sixthDecLengthInches.alpha = 1.0f
                sixthRealLengthInches.isEnabled = true
                sixthDecLengthInches.isEnabled = true
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

    // --------- Start Alarm ------------------------
    private fun startAlarm() {
        // ✅ Ensure raw file exists
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        mediaPlayer?.start()

        val flashHandler = Handler()
        var isRed = true
        val flashRunnable = object : Runnable {
            override fun run() {
                btnAlarmInches.setBackgroundColor(if (isRed) Color.RED else Color.WHITE)
                isRed = !isRed
                flashHandler.postDelayed(this, 500)
            }
        }

        flashHandler.post(flashRunnable)

        handler.postDelayed({
            mediaPlayer?.stop()
            mediaPlayer?.release()
            btnAlarmInches.setBackgroundColor(Color.TRANSPARENT)
            flashHandler.removeCallbacks(flashRunnable)
        }, 4000)
    }

    //@@@@@@@@@@@@ Alarm Triggering @@@@@@@@@@@@@@@

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("ALARM_DEBUG", "📥 onActivityResult triggered with requestCode=$requestCode")

        if (requestCode == requestAlarmSET && resultCode == Activity.RESULT_OK) {
            alarmHour = data?.getIntExtra("ALARM_HOUR", -1) ?: -1
            alarmMinute = data?.getIntExtra("ALARM_MINUTE", -1) ?: -1
            alarmTriggered = false // ✅ reset so the alarm can trigger again

            Log.d("ALARM_DEBUG", "✅ Alarm Set - hour=$alarmHour, minute=$alarmMinute")

            if (alarmHour != -1 && alarmMinute != -1) {
                // Format time string for display
                val amPm = if (alarmHour >= 12) "PM" else "AM"
                val displayHour = if (alarmHour % 12 == 0) 12 else alarmHour % 12
                val formattedMinute = String.format(Locale.getDefault(), "%02d", alarmMinute)
                val timeString = "$displayHour:$formattedMinute $amPm"

                // Update button and show toast
                btnAlarmInches.text = getString(R.string.alarm_set_to, timeString)
                val toastMessage = getString(R.string.alarm_toast_message, timeString)
                Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()

                // Schedule alarm
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, alarmHour)
                    set(Calendar.MINUTE, alarmMinute)
                    set(Calendar.SECOND, 0)
                }

                val alarmIntent = Intent(this, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    this, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE
                )

                val mgr = getSystemService(ALARM_SERVICE) as AlarmManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    !mgr.canScheduleExactAlarms()
                ) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                } else {
                    mgr.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                Log.d("ALARM_DEBUG", "⏰ Alarm scheduled for ${calendar.time}")
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

    // @@@@@@@@@@@@@ BLINK Smallest Catch  @@@@@@@@@@@@@@@@@@@@@@@@@

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

    // --- Voice Control: override to receive speech transcripts ---

    override fun onSpeechResult(transcript: String) {

        VoiceCatchParse().parseVoiceCommand(transcript)?.let { p: ParsedCatch ->
            if (p.totalLengthQuarters > 0) saveTournamentCatch(p.totalLengthQuarters, p.species, p.clipColor)
        } ?: Toast.makeText(this, "Could not parse: $transcript", Toast.LENGTH_LONG).show()
    }

    // --- Voice Control: override to start listening on wake event ---
    override fun onVoiceWake() {
        recognizer.startListening(recognizerIntent)
    }

}//################## END  ################################
