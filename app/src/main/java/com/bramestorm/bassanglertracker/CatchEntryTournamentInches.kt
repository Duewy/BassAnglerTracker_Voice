package com.bramestorm.bassanglertracker

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bramestorm.bassanglertracker.alarm.AlarmReceiver
import com.bramestorm.bassanglertracker.base.BaseCatchEntryActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.training.VoiceInteractionHelper
import com.bramestorm.bassanglertracker.ui.MyWeightEntryDialogFragment
import com.bramestorm.bassanglertracker.util.positionedToast
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
    private lateinit var editDialog: AlertDialog

    // Alarm Variables
    private var alarmHour: Int = -1
    private var alarmMinute: Int = -1
    private var alarmTriggered: Boolean = false
    private val requestAlarmSET = 1012

    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null

    // For VCC to Wake UP
    private var launchFromWake = false

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
    private lateinit var txtVCCTourInches: TextView

    private var availableClipColors: List<ClipColor> = emptyList()
    private val flashHandler = Handler(Looper.getMainLooper())

    // Database Helper
    private lateinit var dbHelper: CatchDatabaseHelper

    // Voice Helper
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

    companion object {
        const val EXTRA_LENGTH_INCHES     = "lengthTotalInches"
        const val EXTRA_SPECIES       = "selectedSpecies"
        const val EXTRA_CLIP_COLOR    = "clip_color"
        const val EXTRA_CATCH_TYPE    = "catchType"
        const val EXTRA_IS_TOURNAMENT = "isTournament"
        const val EXTRA_AVAILABLE_CLIP_COLORS = "availableClipColors"
        const val EXTRA_TOURNAMENT_SPECIES = "tournamentSpecies"
    }

    //!!!!!!!!!!!!!!!!!! Forces Android to Receive data from PopupVcc that is already using Bluetooth
    override val catchReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val oz   = intent.getIntExtra(PopupVccTournLbs.EXTRA_WEIGHT_OZ, 0)
            val sp   = intent.getStringExtra(PopupVccTournLbs.EXTRA_TOURNAMENT_SPECIES).orEmpty()
            val clip = intent.getStringExtra(PopupVccTournLbs.EXTRA_AVAILABLE_CLIP_COLORS).orEmpty()
            saveTournamentCatch(oz, sp, clip)
        }
    }

    // ----------------- wait for POPUP WEIGHT VALUES  ------------------------
    private val lengthEntryLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->
                    val totalLengthQuarters = data.getIntExtra(EXTRA_LENGTH_INCHES, 0)
                    val species = data.getStringExtra(EXTRA_SPECIES)  ?: ""
                    val clipColor = data.getStringExtra(EXTRA_CLIP_COLOR) ?: ""

                    if (totalLengthQuarters > 0) {
                        saveTournamentCatch(totalLengthQuarters, species, clipColor)
                    }
                }
            }
    }
    // ````````````` Retrieves data from the POPUPS ````````````````````````
    private val entryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        awaitingResult = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val totalLengthQuarters = result.data!!.getIntExtra(EXTRA_LENGTH_INCHES, 0)
            val sp = result.data!!.getStringExtra(EXTRA_SPECIES).orEmpty()
            val clip = result.data!!.getStringExtra(EXTRA_CLIP_COLOR).orEmpty()
            saveTournamentCatch(totalLengthQuarters, sp, clip)
        }
    }

    override val dialog: DialogFragment
        get() {
            // build the species list  > if Large or Small Mouth both will be on list
            val speciesList = if (tournamentSpecies.equals("Large Mouth", true) || tournamentSpecies.equals("Largemouth", true))  {
                listOf("Large Mouth", "Small Mouth")
            } else         if (tournamentSpecies.equals("Small Mouth", true) || tournamentSpecies.equals("Smallmouth", true))  {
                listOf("Small Mouth", "Large Mouth")
            } else{
                listOf(tournamentSpecies)
            }

            // compute the _current_ available clip colors
            val clipColorList = calculateAvailableClipColors(
                dbHelper,
                catchType            = "Inches",
                date                 = getCurrentDate(),
                tournamentCatchLimit = tournamentCatchLimit,
                isCullingEnabled     = isCullingEnabled
            ).map { it.name }  // convert from the enum to String list

            // return a brand‐new DialogFragment each time, wiring in the real save call
            return MyWeightEntryDialogFragment(
                speciesList     = speciesList,
                clipColorList   = clipColorList
            ) { inches, quarters, species, clipColor ->
                // Inches and Quarters come from the three spinners
                val totalLengthQuarters = inches * 4 + quarters
                saveTournamentCatch(totalLengthQuarters, species, clipColor)
            }
        }

    //================ ON CREATE =======================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view_inches)

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(catchReceiver, IntentFilter("com.bramestorm.CATCH_TOURNAMENT"))   //todo does this need to be renamed??

        // Set Up the Voice Helper interaction with VoiceInteractionHelper ------
        voiceHelper = VoiceInteractionHelper(
            activity = this, //
            measurementUnit = VoiceInteractionHelper.MeasurementUnit.INCHES,
            isTournament = true,
            onCommandAction = { transcript -> onSpeechResult(transcript) }
        )

        voiceControlEnabled = intent.getBooleanExtra("VCC_ENABLED", false)

        dbHelper = CatchDatabaseHelper(this)
        btnTournamentCatch = findViewById(R.id.btnStartFishingInches)
        btnMainInches = findViewById(R.id.btnMainInches)
        btnSetUpInches = findViewById(R.id.btnSetUpInches)
        btnAlarmInches = findViewById(R.id.btnAlarmInches)
        txtGPSNotice = findViewById(R.id.txtGPSNotice)
        txtVCCTourInches = findViewById(R.id.txtVCCTourInches)

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

        // GET VAlUES from SetUp page -----------
        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra(EXTRA_TOURNAMENT_SPECIES) ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)
        voiceControlEnabled  = intent.getBooleanExtra("VCC_ENABLED", false)     // Is the app in VCC mode?

        //----ADD a CATCH button is clicked -----------
        btnTournamentCatch.setOnClickListener {
            showWeightPopup()
        }

        btnSetUpInches.setOnClickListener { startActivity(Intent(this, SetUpActivity::class.java)) }
        btnMainInches.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        btnAlarmInches.setOnClickListener { startActivityForResult(Intent(this, PopUpAlarm::class.java), requestAlarmSET) }

        updateVccLabel()
        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)

        updateTournamentList()
        handler.postDelayed(checkAlarmRunnable, 60000) // check every minute (60 sec)

        Handler(Looper.getMainLooper()).post {
            if (launchFromWake) {
                showWeightPopup()
                launchFromWake = false
            }
        }
    }
// ~~~~~~~~~~~~~~~~~~~~~ END ON CREATE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


    // ------------- On RESUME --------- Check GPS  Statues --------------
    override fun onResume() {
        super.onResume()
        updateVccLabel()            // just shows user if VCC is Enabled or not...
        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)
    }

    //----------- On Manual Wake ------------------------
    override fun onManualWake() {
        showWeightPopup()
    }

    //------------- ON DESTROY ----- Disarm the ALARM -----------------
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(catchReceiver)
        voiceHelper.shutdown()
        handler.removeCallbacksAndMessages(null)
        flashHandler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val totalLengthQuarters = intent.getIntExtra(EXTRA_LENGTH_INCHES, 0)
        val sp   = intent.getStringExtra(CatchEntryTournament.EXTRA_SPECIES).orEmpty()
        val clip = intent.getStringExtra(CatchEntryTournament.EXTRA_CLIP_COLOR).orEmpty()
        saveTournamentCatch(totalLengthQuarters, sp, clip)
    }

    /** ~~~~~~~~~~~~~ Opens the weight entry popup ~~~~~~~~~~~~~~~ */
    /** Launches the appropriate popup (VCC vs manual) */
    private fun showWeightPopup() {
        awaitingResult = true

        // build the Intent with your fresh list
        val intent = Intent(
            this,
            if (voiceControlEnabled) PopupVccTournInches::class.java
            else PopupLengthEntryTourInches::class.java
        ).apply {
            intent.putExtra(EXTRA_IS_TOURNAMENT, true)

            if (tournamentSpecies.equals("Large Mouth", true) || tournamentSpecies.equals("Largemouth", true))  {
                intent.putExtra(EXTRA_TOURNAMENT_SPECIES, "Large Mouth Bass")
            } else         if (tournamentSpecies.equals("Small Mouth", true) || tournamentSpecies.equals("Smallmouth", true))  {
                intent.putExtra(EXTRA_TOURNAMENT_SPECIES, "Small Mouth Bass")
            } else{
                intent.putExtra(EXTRA_TOURNAMENT_SPECIES, tournamentSpecies)
            }

            // 🔥 Send available clip colors as String array
            val colorNames = availableClipColors.map { it.name }.toTypedArray()
            intent.putExtra(EXTRA_AVAILABLE_CLIP_COLORS, colorNames)
        }

        entryLauncher.launch(intent)
    }


    // ^^^^^^^^^^^^^ SAVE TOURNAMENT CATCH ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    fun saveTournamentCatch(totalLengthQuarters: Int, bassType: String, clipColor: String) {
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
        val totalInches = (totalLengthInches / 4)
        val totalDec = (totalLengthInches % 4)

        totalRealLengthInches.text = totalInches.toString()
        totalDecLengthInches.text = "$totalDec /8"

// !!!!!!!!!!!!!!!!!!!! MOTIVATIONAL TOASTS !!!!!!!!!!!!!!!!!!!!!!!!!!!
        val currentCount = dbHelper             //todo create better set points for motivational toasts
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
                val lengthInches = totalLengthInches / 4
                val lengthDec = totalLengthInches % 4

                val clipColor = try {
                    ClipColor.valueOf(catch.clipColor?.uppercase() ?: "")
                } catch (_: Exception) {
                    ClipColor.RED
                }

                realLengthInches[i].text = lengthInches.toString()
                decLengthInches[i].text = "$lengthDec /4"

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

                // **long-press to EDIT or DELETE this exact item**
                realLengthInches[i].setOnLongClickListener {
                    showTournamentEditDialog(catch)
                    true
                }
                decLengthInches[i].setOnLongClickListener {
                    showTournamentEditDialog(catch)
                    true
                }
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

    private fun showTournamentEditDialog(c: CatchItem) {
        // 1) inflate your 4ths layout
        val dialogView = layoutInflater.inflate(
            R.layout.dialog_edit_tournament_catch_inches,
            null
        )

        // 2) find your views
        val txtClipColor          = dialogView.findViewById<TextView>(R.id.txtClipColor)
        val edtInches             = dialogView.findViewById<EditText>(R.id.edtTourWeightKgs)            // hint="inches"
        val edtQuartersOfInch     = dialogView.findViewById<EditText>(R.id.edtTourWeightGrams)          // hint="⁄4ths"
        val btnSave               = dialogView.findViewById<Button>(R.id.btnSaveEdtTourInches)
        val btnCancel             = dialogView.findViewById<Button>(R.id.btnCancelEdtTourInches)

        // 3) prefill from CatchItem.totalLengthQuarters (which stores quarters)
        val totalQuarters = c.totalLengthQuarters ?: 0
        edtInches.setText((totalQuarters / 4).toString())
        edtQuartersOfInch.setText((totalQuarters % 4).toString())

        // 4) show clip-color box
        val clip = try {
            ClipColor.valueOf(c.clipColor!!.uppercase())
        } catch (_: Exception) {
            ClipColor.RED
        }
        txtClipColor.background = createLayeredDrawable(
            ContextCompat.getColor(this, clip.resId)
        )

        // 5) build & show **one** dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit or Delete Catch")
            .setView(dialogView)
            .create()
        dialog.show()

        // 6) Save → recombine inches*4 + quarters, update DB
        btnSave.setOnClickListener {
            val newInches   = edtInches.text.toString().toIntOrNull() ?: 0
            val newQuarters = edtQuartersOfInch.text.toString().toIntOrNull() ?: 0
            val newTotalInches    = (newInches * 4 + newQuarters)

            dbHelper.updateCatch(
                catchId           = c.id,
                newWeightOz       = null,
                newWeightKg       = null,
                newLengthQuarters = newTotalInches,
                newLengthCm       = null,
                species           = c.species
            )
            updateTournamentList()
            dialog.dismiss()
        }

        // 7) Cancel just dismisses
        btnCancel.setOnClickListener {
            dialog.dismiss()
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
                btnAlarmInches.text = getString(R.string.alarm_set_to, timeString)
                positionedToast(getString(R.string.alarm_toast_message, timeString))
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

    //+++++++ Create Boarder Around Clip Color to have Show Up on Backgrounds ++++++++++++++++++++

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

    // ----------- Show if VCC is Enabled ------------------
    private fun updateVccLabel() {
        if (voiceControlEnabled) {
            txtVCCTourInches.text = getString(R.string.vcc_on)
            txtVCCTourInches.setBackgroundColor(ContextCompat.getColor(this, R.color.clip_yellow))
            txtVCCTourInches.setTextColor(ContextCompat.getColor(this, R.color.clip_orange))// Orange
        } else {
            txtVCCTourInches.text = getString(R.string.manual_mode)
            txtVCCTourInches.setTextColor(ContextCompat.getColor(this, R.color.clip_blue))// blue
            txtVCCTourInches.background = null
        }
    }

    // ------------ VCC Enabled Set Up Voice Control ----------------
    override fun onSpeechResult(transcript: String) {       //todo not sure what to do with this, is it for voice wakeup?? can we use for other voice commands???
        Log.d("VCC", "Speech Result Received: $transcript")

        // 👇 Replace with your actual phrase recognition or command parsing  todo find out what we will do with this...
        if (transcript.contains("add fish", ignoreCase = true)) {
            showWeightPopup() // 🔥 Launches PopupLengthInches.kt
        } else {
            Toast.makeText(this, "Unrecognized command: $transcript", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Also required by the base.  You could use this
     * if you wanted the old “wake” event to kick off VCC,
     * but we’re using a double-tap listener instead.
     */
    // ------------ Double Tap Wakes App Up for VCC --------------
    override fun onVoiceWake() {
        launchFromWake = true
    }


}//################## END  ################################
