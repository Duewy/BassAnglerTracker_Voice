package com.bramestorm.bassanglertracker

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
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
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.CatchEntryTournament.Companion.EXTRA_AVAILABLE_CLIP_COLORS
import com.bramestorm.bassanglertracker.alarm.AlarmReceiver
import com.bramestorm.bassanglertracker.base.BaseCatchEntryActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.training.ParsedCatch
import com.bramestorm.bassanglertracker.training.VoiceCatchParse
import com.bramestorm.bassanglertracker.training.VoiceInteractionHelper
import com.bramestorm.bassanglertracker.utils.GpsUtils
import com.bramestorm.bassanglertracker.utils.getMotivationalMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class CatchEntryTournamentKgs : BaseCatchEntryActivity() {


    // Buttons
    private lateinit var btnStartFishingKgs: Button
    private lateinit var btnSetUpKgs: Button
    private lateinit var btnMainKgs:Button
    private lateinit var btnAlarmKgs: Button
    private lateinit var dialogInstance: AlertDialog
    override val dialog: AlertDialog
        get() = dialogInstance

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

    private lateinit var txtGPSNotice: TextView

    private var availableClipColors: List<ClipColor> = emptyList()
    private val flashHandler = Handler(Looper.getMainLooper())

    // Database Helper
    private lateinit var dbHelper: CatchDatabaseHelper

    // Voice Helper
    private lateinit var tts: TextToSpeech
    private var toastTts: TextToSpeech? = null
    private var voiceControlEnabled = false
    private lateinit var voiceHelper: VoiceInteractionHelper
    lateinit var userVoiceMap: MutableMap<String, String>       //todo Correct with Mispronunciations ReWrite the Word/Phrase DataBase
    private var awaitingResult = false

    // --- voice-to-text callback handler ---
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            Toast.makeText(this@CatchEntryTournamentKgs, "Speech error $error", Toast.LENGTH_SHORT).show()
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

            if (weightTotalKgs > 0) {
                saveTournamentCatch(weightTotalKgs, selectedSpecies, clipColor)
            }
        }
    }

 //================ ON CREATE =======================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view_kgs)

    //******  Initialize speech recognizer ***********************
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(recognitionListener)
        }
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        dbHelper = CatchDatabaseHelper(this)
        btnStartFishingKgs = findViewById(R.id.btnStartFishingKgs)
        btnSetUpKgs = findViewById(R.id.btnSetUpKgs)
        btnMainKgs = findViewById(R.id.btnMainKgs)
        btnAlarmKgs = findViewById(R.id.btnAlarmKgs)
        txtGPSNotice = findViewById(R.id.txtGPSNotice)

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

        //>>>>  Get Values from Set-Up Page <<<<<<<<<
        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)
        voiceControlEnabled  = intent.getBooleanExtra("VCC_ENABLED", false)

     //************ onClickListener **************************
        btnStartFishingKgs.setOnClickListener { showWeightPopup() }
        btnSetUpKgs.setOnClickListener { startActivity(Intent(this, SetUpActivity::class.java)) }
        btnMainKgs.setOnClickListener { startActivity(Intent(this,MainActivity::class.java)) }
        btnAlarmKgs.setOnClickListener { startActivityForResult(Intent(this, PopUpAlarm::class.java), requestAlarmSET) }

        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)

        updateTournamentList()
        handler.postDelayed(checkAlarmRunnable, 60000)  // check every minute (60 sec)
    }
// ~~~~~~~~~~~~~~~~~~~~~ END ON CREATE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // ------------- On RESUME --------- Check GPS  Statues --------------
    override fun onResume() {
        super.onResume()
        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)
    }

    //------------- ON DESTROY ----- Disarm the ALARM -----------------
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

        // Send as an ArrayList so you can retrieve with getStringArrayListExtra
        val colorArray = availableClipColors.map { it.name }.toTypedArray()
        intent.putExtra(EXTRA_AVAILABLE_CLIP_COLORS, colorArray)

        weightEntryLauncher.launch(intent)
    }

    // ^^^^^^^^^^^^^ SAVE TOURNAMENT CATCH ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    private fun saveTournamentCatch(weightTotalKgs: Int, bassType: String, clipColor: String) {

        val cleanClipColor = clipColor.uppercase() // This came from the popup

        val speciesInitial = if (bassType == "Large Mouth") "L" else "S"

        Log.d("DB_DEBUG", "✅ Assigned Clip Color: $cleanClipColor")

        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = bassType,
            totalWeightOz = null,
            totalLengthQuarters = null,
            totalWeightHundredthKg = weightTotalKgs,
            totalLengthTenths = null,
            catchType = "kgs",
            markerType = speciesInitial,
            clipColor = cleanClipColor
        )

        val result = dbHelper.insertCatch(catch)
        Log.d("DB_DEBUG", "✅ Catch Insert Result: $result, Stored Clip Color: ${catch.clipColor}")

        Toast.makeText(this, "$bassType Catch Saved!", Toast.LENGTH_SHORT).show()
        // ✅ Save the most recent catch for motivational messaging
        if (result) {
            lastTournamentCatch = catch
        }

        updateTournamentList()
    }

// ``````````````` UPDATE TOTAL WEIGHT ```````````````````````
    private fun updateTotalWeight(tournamentCatches: List<CatchItem>) {
        // Always sort and limit to top N
        val catchesToUse = tournamentCatches
            .sortedByDescending { it.totalWeightHundredthKg ?: 0 }
            .take(tournamentCatchLimit)  // ✅ Apply limit always

        val totalWeightKgs = catchesToUse.sumOf { it.totalWeightHundredthKg ?: 0 }
        val totalKgs = totalWeightKgs / 100
        val totalDec = totalWeightKgs % 100

        totalRealWeightKgs.text = totalKgs.toString()
        totalDecWeightKgs.text = totalDec.toString().padStart(2, '0')

        // !!!!!!!!!!!!!!!!!!!! MOTIVATIONAL TOASTS !!!!!!!!!!!!!!!!!!!!!!!!!!!
        val currentCount = dbHelper
            .getCatchesForToday("kgs", getCurrentDate())
            .sortedByDescending { it.totalWeightHundredthKg ?: 0 }
            .take(tournamentCatchLimit)
            .size

        if (currentCount >= 2) {
            lastTournamentCatch?.let {
                val message = getMotivationalMessage(this, it.id, tournamentCatchLimit, "kgs")
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
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

        val colorLetters = listOf(
            txtKgsColorLetter1, txtKgsColorLetter2, txtKgsColorLetter3,
            txtKgsColorLetter4, txtKgsColorLetter5, txtKgsColorLetter6
        )

        val typeLetters = listOf(
            txtTypeLetter1, txtTypeLetter2, txtTypeLetter3,
            txtTypeLetter4, txtTypeLetter5, txtTypeLetter6
        )

        val allCatches = dbHelper.getCatchesForToday(catchType = "kgs", formattedDate)
        val sortedCatches = allCatches.sortedByDescending { it.totalWeightHundredthKg ?: 0 }

        val tournamentCatches = if (isCullingEnabled) {
            sortedCatches.take(tournamentCatchLimit)
        } else {
            sortedCatches
        }

        availableClipColors = calculateAvailableClipColors(
            dbHelper,
            catchType = "kgs",
            date = formattedDate,
            tournamentCatchLimit = tournamentCatchLimit,
            isCullingEnabled = isCullingEnabled
        )

        Log.d("CLIP_COLOR", "🎨 Available Colors KGS: $availableClipColors")

        clearTournamentTextViews()

        runOnUiThread {
            val loopLimit = minOf(sortedCatches.size, 6)

            for (i in 0 until loopLimit) {
                if (i >= realWeightKgs.size) continue

                val catch = sortedCatches[i]
                val totalWeightKgs = catch.totalWeightHundredthKg ?: 0
                val weightKgs = totalWeightKgs / 100
                val weightDec = totalWeightKgs % 100

                val clipColor = try {
                    ClipColor.valueOf(catch.clipColor?.uppercase() ?: "")
                } catch (_: Exception) {
                    ClipColor.RED
                }

                realWeightKgs[i].text = weightKgs.toString()  // ensure there is a "0" in 01 - 09
                decWeightKgs[i].text = weightDec.toString().padStart(2, '0')


                realWeightKgs[i].setOnLongClickListener {
                    showTournamentEditDialog(sortedCatches[i])
                    true
                }
                decWeightKgs[i].setOnLongClickListener {
                    showTournamentEditDialog(sortedCatches[i])
                    true
                }


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
                }, 300)
            }
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

    private fun showTournamentEditDialog(c: CatchItem) {
        // 1) inflate your custom layout
        val dialogView = layoutInflater.inflate(
            R.layout.dialog_edit_tournament_catch_kgs,
            null
        )

        // 2) find in-layout views
        val txtClipColor = dialogView.findViewById<TextView>(R.id.txtClipColor)
        val edtKgs       = dialogView.findViewById<EditText>(R.id.edtTourWeightKgs)
        val edtGrams     = dialogView.findViewById<EditText>(R.id.edtTourWeightGrams)
        val btnSave      = dialogView.findViewById<Button>(R.id.btnSaveEdtTourKgss)
        val btnCancel    = dialogView.findViewById<Button>(R.id.btnCancelEdtTourKgs)

        // 3) prefill the fields from the CatchItem
        val totalHundredth = c.totalWeightHundredthKg ?: 0
        edtKgs.setText((totalHundredth / 100).toString())
        edtGrams.setText((totalHundredth % 100).toString())

        // 4) set the clip-color box
        val clip = try {
            ClipColor.valueOf(c.clipColor!!.uppercase())
        } catch (_: Exception) {
            ClipColor.RED
        }
        txtClipColor.background = createLayeredDrawable(
            ContextCompat.getColor(this, clip.resId)
        )

        // 5) build & show the AlertDialog (rename to 'dlg' to avoid collision)
        dialogInstance = AlertDialog.Builder(this)
            .setTitle("Edit or Delete Catch")
            .setView(dialogView)
            .create()
        dialogInstance.show()

        // 6) Save button
        btnSave.setOnClickListener {
            val newKgs   = edtKgs.text.toString().toIntOrNull() ?: 0
            val newGrams = edtGrams.text.toString().toIntOrNull() ?: 0
            val newTotalKg = (newKgs * 100 + newGrams)

            dbHelper.updateCatch(
                catchId           = c.id,
                newWeightOz       = null,
                newWeightKg       = newTotalKg,
                newLengthQuarters = null,
                newLengthCm       = null,
                species           = c.species
            )
            updateTournamentList()
            dialogInstance.dismiss()
        }

        // 7) Cancel button
        btnCancel.setOnClickListener {
            dialogInstance.dismiss()
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

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
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
                btnAlarmKgs.text = getString(R.string.alarm_set_to, timeString)
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

    // --- Voice Control: override to receive speech transcripts ---

    override fun onSpeechResult(transcript: String) {

        VoiceCatchParse().parseVoiceCommand(transcript)?.let { p: ParsedCatch ->
            if (p.totalWeightHundredthKg > 0) saveTournamentCatch(p.totalWeightHundredthKg, p.species, p.clipColor)
        } ?: Toast.makeText(this, "Could not parse: $transcript", Toast.LENGTH_LONG).show()
    }

    // --- Voice Control: override to start listening on wake event ---
    override fun onVoiceWake() {
        recognizer.startListening(recognizerIntent)
    }

}//################## END  ################################