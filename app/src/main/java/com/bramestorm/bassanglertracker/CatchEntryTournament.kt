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
import com.bramestorm.bassanglertracker.alarm.AlarmReceiver
import com.bramestorm.bassanglertracker.base.BaseCatchEntryActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.training.CatchMode
import com.bramestorm.bassanglertracker.training.VoiceInputMapper.loadUserVoiceMap
import com.bramestorm.bassanglertracker.training.VoiceInteractionHelper
import com.bramestorm.bassanglertracker.ui.MyWeightEntryDialogFragment
import com.bramestorm.bassanglertracker.utils.GpsUtils
import com.bramestorm.bassanglertracker.utils.getMotivationalMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class CatchEntryTournament : BaseCatchEntryActivity() {

    // Buttons
    private lateinit var btnTournamentCatch: Button
    private lateinit var btnMenu: Button
    private lateinit var btnMainPg:Button
    private lateinit var btnAlarm: Button
    private lateinit var editDialog: AlertDialog

    // Alarm Variables
    private var alarmHour: Int = -1
    private var alarmMinute: Int = -1
    private var alarmTriggered: Boolean = false


    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null

    // Weight Display TextViews
    private lateinit var firstRealWeight: TextView
    private lateinit var secondRealWeight: TextView
    private lateinit var thirdRealWeight: TextView
    private lateinit var fourthRealWeight: TextView
    private lateinit var fifthRealWeight: TextView
    private lateinit var sixthRealWeight: TextView

    private lateinit var firstDecWeight: TextView
    private lateinit var secondDecWeight: TextView
    private lateinit var thirdDecWeight: TextView
    private lateinit var fourthDecWeight: TextView
    private lateinit var fifthDecWeight: TextView
    private lateinit var sixthDecWeight: TextView

    private lateinit var txtTypeLetter1:TextView
    private lateinit var txtTypeLetter2:TextView
    private lateinit var txtTypeLetter3:TextView
    private lateinit var txtTypeLetter4:TextView
    private lateinit var txtTypeLetter5:TextView
    private lateinit var txtTypeLetter6:TextView

    private lateinit var txtColorLetter1:TextView
    private lateinit var txtColorLetter2:TextView
    private lateinit var txtColorLetter3:TextView
    private lateinit var txtColorLetter4:TextView
    private lateinit var txtColorLetter5:TextView
    private lateinit var txtColorLetter6:TextView


    private lateinit var totalRealWeight: TextView
    private lateinit var totalDecWeight: TextView

    private lateinit var txtGPSNotice:  TextView
    private lateinit var txtVCCTourLbs: TextView

    private var availableClipColors: List<ClipColor> = emptyList()
    private val flashHandler = Handler(Looper.getMainLooper())

    // Database Helper
    private lateinit var dbHelper: CatchDatabaseHelper

    // Voice Helper
    private var voiceControlEnabled = false
    private lateinit var voiceHelper: VoiceInteractionHelper
    lateinit var userVoiceMap: MutableMap<String, String>       // Correct with Mispronunciations ReWrite the Word/Phrase DataBase



    // Tournament Configuration
    private var tournamentCatchLimit: Int = 4
    private var measurementSystem: String = "weight"
    private var isCullingEnabled: Boolean = false
    private var typeOfMarkers: String = "Color"
    private var tournamentSpecies: String = "Unknown"
    private var lastTournamentCatch: CatchItem? = null

    // Request Codes
    private val requestAlarmSET = 1006


    // ----------------- wait for POPUP WEIGHT VALUES  ------------------------
    private val weightEntryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val totalWeightOz = data.getIntExtra("weightTotalOz", 0)
                val species       = data.getStringExtra("selectedSpecies") ?: ""
                val clipColor     = data.getStringExtra("clip_color") ?: ""

                if (totalWeightOz > 0) {
                    saveTournamentCatch(totalWeightOz, species, clipColor)
                }
            }
        }
    }

    override val dialog: DialogFragment
        get() {
            // build the species list
            val speciesList = if (tournamentSpecies.equals("Bass", true)) {
                listOf("Large Mouth", "Small Mouth")
            } else {
                listOf(tournamentSpecies)
            }

            // compute the _current_ available clip colors
            val clipColorList = calculateAvailableClipColors(
                dbHelper,
                catchType            = "LbsOzs",
                date                 = getCurrentDate(),
                tournamentCatchLimit = tournamentCatchLimit,
                isCullingEnabled     = isCullingEnabled
            ).map { it.name }  // convert from your enum to String list

            // return a brand‐new DialogFragment each time, wiring in the real save call
            return MyWeightEntryDialogFragment(
                speciesList     = speciesList,
                clipColorList   = clipColorList
            ) { lbs, oz, species, clipColor ->
                // lbs & oz come from the three spinners
                val totalWeightOz = lbs * 16 + oz
                saveTournamentCatch(totalWeightOz, species, clipColor)
            }
        }


    //================START - ON CREATE =======================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view)


        dbHelper = CatchDatabaseHelper(this)
        btnTournamentCatch = findViewById(R.id.btnStartFishing)
        btnMenu = findViewById(R.id.btnMenu)
        btnMainPg = findViewById(R.id.btnMainPg)
        btnAlarm = findViewById(R.id.btnAlarm)
        txtGPSNotice = findViewById(R.id.txtGPSNotice)
        txtVCCTourLbs = findViewById(R.id.txtVCCTourLbs)

        // Assign TextViews
        firstRealWeight = findViewById(R.id.firstRealWeight)
        secondRealWeight = findViewById(R.id.secondRealWeight)
        thirdRealWeight = findViewById(R.id.thirdRealWeight)
        fourthRealWeight = findViewById(R.id.fourthRealWeight)
        fifthRealWeight = findViewById(R.id.fifthRealWeight)
        sixthRealWeight = findViewById(R.id.sixthRealWeight)

        firstDecWeight = findViewById(R.id.firstDecWeight)
        secondDecWeight = findViewById(R.id.secondDecWeight)
        thirdDecWeight = findViewById(R.id.thirdDecWeight)
        fourthDecWeight = findViewById(R.id.fourthDecWeight)
        fifthDecWeight = findViewById(R.id.fifthDecWeight)
        sixthDecWeight = findViewById(R.id.sixthDecWeight)

        txtTypeLetter1 = findViewById(R.id.txtTypeLetter1)
        txtTypeLetter2 = findViewById(R.id.txtTypeLetter2)
        txtTypeLetter3 = findViewById(R.id.txtTypeLetter3)
        txtTypeLetter4 = findViewById(R.id.txtTypeLetter4)
        txtTypeLetter5 = findViewById(R.id.txtTypeLetter5)
        txtTypeLetter6 = findViewById(R.id.txtTypeLetter6)

        totalRealWeight = findViewById(R.id.totalRealWeight)
        totalDecWeight = findViewById(R.id.totalDecWeight)

        txtColorLetter1 = findViewById(R.id.txtColorLetter1)
        txtColorLetter2 = findViewById(R.id.txtColorLetter2)
        txtColorLetter3 = findViewById(R.id.txtColorLetter3)
        txtColorLetter4 = findViewById(R.id.txtColorLetter4)
        txtColorLetter5 = findViewById(R.id.txtColorLetter5)
        txtColorLetter6 = findViewById(R.id.txtColorLetter6)

    // Set Up the Voice Helper interaction with VoiceInteractionHelper ------
        voiceHelper = VoiceInteractionHelper(
            this,
            CatchMode.TOURNAMENT_LBS_OZS,
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {}
                override fun onResults(results: Bundle?) {}
                override fun onPartialResults(partial: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
        ).apply {
            userVoiceMap = loadUserVoiceMap(this@CatchEntryTournament).toMutableMap()
            onCommandRecognized = { command: String -> handleCommand(command) }
        }


        // GET VAlUES from SetUp page -----------
        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)       // Culling Values
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"                 // Typical Markers colors
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"      // Tournament Species
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"                 // Type of Measuring
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)     // Is Culling / Tournament mode
        voiceControlEnabled  = intent.getBooleanExtra("VCC_ENABLED", false)     // Is the app in VCC mode?
        val voiceOn = intent.getBooleanExtra(Constants.EXTRA_VOICE_CONTROL_ENABLED, false)

        //----ADD a CATCH button is clicked -----------
        btnTournamentCatch.setOnClickListener {
            if (voiceControlEnabled) {
                voiceHelper.startCatchSequence()
            } else {
                showWeightPopup()  // your manual entry
            }
        }

        btnMenu.setOnClickListener { startActivity(Intent(this, SetUpActivity::class.java)) }
        btnMainPg.setOnClickListener { startActivity(Intent(this,MainActivity::class.java)) }
        btnAlarm.setOnClickListener { startActivityForResult(Intent(this, PopUpAlarm::class.java), requestAlarmSET) }

        updateVccLabel()
        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)

        updateTournamentList()
        handler.postDelayed(checkAlarmRunnable, 60000)
    }
// ~~~~~~~~~~~~~~~~~~~~~ END ON CREATE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // ------------- On RESUME --------- Check GPS  Statues --------------
    override fun onResume() {
        super.onResume()
        updateVccLabel()
        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)
    }

    //----------- On Manual Wake ------------------------
    override fun onManualWake() {
        showWeightPopup()   // launches PopupWeightEntry… Activity
    }

    //------------- ON DESTROY ----- Disarm the ALARM -----------------
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        flashHandler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
    }


    /** ~~~~~~~~~~~~~ Opens the weight entry popup ~~~~~~~~~~~~~~~ */

    private fun showWeightPopup() {
        val intent = Intent(this, PopupWeightEntryTourLbs::class.java)
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
    private fun saveTournamentCatch(weightTotalOz: Int, bassType: String, clipColor: String) {
        val availableColors = calculateAvailableClipColors(
            dbHelper,
            catchType = "LbsOzs",
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
            totalWeightOz = weightTotalOz,
            totalLengthQuarters = null,
            totalWeightHundredthKg = null,
            totalLengthTenths = null,
            catchType = "LbsOzs",
            markerType = speciesInitial,
            clipColor = cleanClipColor
        )

        val result = dbHelper.insertCatch(catch)
        Log.d("DB_DEBUG", "✅ Catch Insert Result: $result, Stored Clip Color: ${catch.clipColor}")

        Toast.makeText(this, "$bassType Catch Saved!", Toast.LENGTH_SHORT).show()
        if (result) {
            lastTournamentCatch = catch
        }
        updateTournamentList()
    }


    // ``````````````` UPDATE TOTAL WEIGHT ``````````````````````
    private fun updateTotalWeight(tournamentCatches: List<CatchItem>) {
        // Always sort and limit to top N
        val catchesToUse = tournamentCatches
            .sortedByDescending { it.totalWeightOz ?: 0 }
            .take(tournamentCatchLimit)  // ✅ Apply limit always

        val totalWeightOz = catchesToUse.sumOf { it.totalWeightOz ?: 0 }
        val totalLbs = (totalWeightOz / 16)
        val totalOz = (totalWeightOz % 16)

        totalRealWeight.text = totalLbs.toString()
        totalDecWeight.text = totalOz.toString()


    // !!!!!!!!!!!!!!!!!!!! 👍 MOTIVATIONAL TOASTS 👍 !!!!!!!!!!!!!!!!!!!!!!!!!!!

        val currentCount = dbHelper
            .getCatchesForToday("LbsOzs", getCurrentDate())
            .sortedByDescending { it.totalWeightOz ?: 0 }
            .take(tournamentCatchLimit)
            .size

        if (currentCount >= 2) {
            lastTournamentCatch?.let {
                val message = getMotivationalMessage(this, it.id, tournamentCatchLimit, "lbs")
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //################## UPDATE TOURNAMENT LIST   ###################################

    private fun updateTournamentList() {
        val formattedDate = getCurrentDate()

        val realWeights = listOf(
            firstRealWeight, secondRealWeight, thirdRealWeight,
            fourthRealWeight, fifthRealWeight, sixthRealWeight
        )

        val decWeights = listOf(
            firstDecWeight, secondDecWeight, thirdDecWeight,
            fourthDecWeight, fifthDecWeight, sixthDecWeight
        )

        val colorLetters = listOf(
            txtColorLetter1, txtColorLetter2, txtColorLetter3,
            txtColorLetter4, txtColorLetter5, txtColorLetter6
        )

        val typeLetters = listOf(
            txtTypeLetter1, txtTypeLetter2, txtTypeLetter3,
            txtTypeLetter4, txtTypeLetter5, txtTypeLetter6
        )

        val allCatches = dbHelper.getCatchesForToday(catchType = "LbsOzs", formattedDate)
        val sortedCatches = allCatches.sortedByDescending { it.totalWeightOz ?: 0 }

        // These are the ones used for scoring and totals
        val tournamentCatches = if (isCullingEnabled) {
            sortedCatches.take(tournamentCatchLimit)
        } else {
            sortedCatches
        }

        availableClipColors = calculateAvailableClipColors(
            dbHelper,
            catchType = "LbsOzs",
            date = formattedDate,
            tournamentCatchLimit = tournamentCatchLimit,
            isCullingEnabled = isCullingEnabled
        )

        clearTournamentTextViews()

        runOnUiThread {
            val loopLimit = minOf(sortedCatches.size, 6)
            for (i in 0 until loopLimit) {
                if (i >= realWeights.size) break

                val item = sortedCatches[i]
                val oz = item.totalWeightOz ?: 0
                val lbs = oz / 16
                val remOz = oz % 16

                // fill in the TextViews
                realWeights[i].text = lbs.toString()
                decWeights[i].text  = remOz.toString()

                val clipColor = try {
                    ClipColor.valueOf(item.clipColor!!.uppercase())
                } catch (_: Exception) {
                    ClipColor.RED
                }

                val baseColor = ContextCompat.getColor(this, clipColor.resId)
                val drawable  = createLayeredDrawable(baseColor)
                realWeights[i].background = drawable
                decWeights[i].background  = drawable

                // Text overlays
                colorLetters[i].text = when (clipColor.name) {
                    "BLUE" -> "B"
                    "RED" -> "R"
                    "GREEN" -> "G"
                    "YELLOW" -> "Y"
                    "ORANGE" -> "O"
                    "WHITE" -> "W"
                    else -> "?"
                }
                typeLetters[i].text  = getSpeciesCode(item.species ?: "")
                // **long-press to edit/delete this exact item**
                realWeights[i].setOnLongClickListener {
                    showTournamentEditDialog(item)
                    true
                }
                decWeights[i].setOnLongClickListener {
                    showTournamentEditDialog(item)
                    true
                }
            }

            updateTotalWeight(tournamentCatches)
            adjustTextViewVisibility()

            // Blink the weight of the last qualifying fish
            if (tournamentCatches.size >= tournamentCatchLimit) {
                Handler(Looper.getMainLooper()).postDelayed({
                    when (tournamentCatchLimit) {
                        4 -> {
                            blinkTextViewTwice(fourthRealWeight)
                            blinkTextViewTwice(fourthDecWeight)
                        }
                        5 -> {
                            blinkTextViewTwice(fifthRealWeight)
                            blinkTextViewTwice(fifthDecWeight)
                        }
                        6 -> {
                            blinkTextViewTwice(sixthRealWeight)
                            blinkTextViewTwice(sixthDecWeight)
                        }
                    }
                }, 300)
            }
        }
    }



    //########### Clear Tournament Text Views  ########################

    private fun clearTournamentTextViews() {
        firstRealWeight.text = ""
        secondRealWeight.text = ""
        thirdRealWeight.text = ""
        fourthRealWeight.text = ""
        fifthRealWeight.text = ""
        sixthRealWeight.text = ""

        firstDecWeight.text = ""
        secondDecWeight.text = ""
        thirdDecWeight.text = ""
        fourthDecWeight.text = ""
        fifthDecWeight.text = ""
        sixthDecWeight.text = ""

        totalRealWeight.text = "0"
        totalDecWeight.text = "0"
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
        isCullingEnabled: Boolean,
    ): List<ClipColor> {
        val allCatches = dbHelper.getCatchesForToday(catchType, date)
        val sorted = allCatches.sortedByDescending { it.totalWeightOz ?: 0 }
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
                fifthRealWeight.alpha = 0.3f
                fifthDecWeight.alpha = 0.3f
                fifthRealWeight.isEnabled = false
                fifthDecWeight.isEnabled = false
                sixthRealWeight.visibility = View.INVISIBLE
                sixthDecWeight.visibility = View.INVISIBLE
            }
            5 -> {
                sixthRealWeight.alpha = 0.3f
                sixthDecWeight.alpha = 0.3f
                sixthRealWeight.isEnabled = false
                sixthDecWeight.isEnabled = false
                txtTypeLetter6.isEnabled = false
            }
            else -> {
                fifthRealWeight.alpha = 1.0f
                fifthDecWeight.alpha = 1.0f
                fifthRealWeight.isEnabled = true
                fifthDecWeight.isEnabled = true
                sixthRealWeight.visibility = View.VISIBLE
                sixthDecWeight.visibility = View.VISIBLE
                sixthRealWeight.alpha = 1.0f
                sixthDecWeight.alpha = 1.0f
                sixthRealWeight.isEnabled = true
                sixthDecWeight.isEnabled = true
            }
        }
    }

    //!!!!!!!!!!!!!!!! Get SPECIES Letters !!!!!!!!!!!!!!!!!

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


        //******************* EDIT Weights ********************************
        private fun showTournamentEditDialog(c: CatchItem) {
            // 1) inflate the custom layout
            val dialogView = layoutInflater.inflate(R.layout.dialog_edit_tournament_catch_lbs, null)

            // 2) pull out your in-layout buttons & fields
            val txtClipColor = dialogView.findViewById<TextView>(R.id.txtClipColor)
            val edtLbs       = dialogView.findViewById<EditText>(R.id.edtTourWeightLbs)
            val edtOzs       = dialogView.findViewById<EditText>(R.id.edtTourWeightOzs)
            val btnSave      = dialogView.findViewById<Button>(R.id.btnSaveEdtTourLbs)
            val btnCancel    = dialogView.findViewById<Button>(R.id.btnCancelEdtTourLbs)

            // 3) pre-fill the fields
            val weightOz = c.totalWeightOz ?: 0
            edtLbs.setText((weightOz / 16).toString())
            edtOzs.setText((weightOz % 16).toString())

            // 4) color box
            val clipColor = try {
                ClipColor.valueOf(c.clipColor!!.uppercase())
            } catch (_: Exception) {
                ClipColor.RED
            }
            txtClipColor.background =
                createLayeredDrawable(ContextCompat.getColor(this, clipColor.resId))

            // 5) build & show **one** dialog
            val dialog = AlertDialog.Builder(this)
                .setTitle("Edit or Delete Catch")
                .setView(dialogView)
                .create()
            dialog.show()

            // 6) wire your in-layout Save
            btnSave.setOnClickListener {
                val newLbs     = edtLbs.text.toString().toIntOrNull() ?: 0
                val newOzs     = edtOzs.text.toString().toIntOrNull() ?: 0
                val newWeightOz = (newLbs * 16) + newOzs

                dbHelper.updateCatch(
                    catchId            = c.id,
                    newWeightOz        = newWeightOz,
                    newWeightKg        = null,
                    newLengthQuarters  = null,
                    newLengthCm        = null,
                    species            = c.species
                )
                updateTournamentList()
                dialog.dismiss()
            }

            // 7) …and Cancel
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


    //!!!!!!!  Start Alarm  !!!!!!!!
    private fun startAlarm() {
        // ✅ Ensure raw file exists
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        mediaPlayer?.start()

        val flashHandler = Handler()
        var isRed = true
        val flashRunnable = object : Runnable {
            override fun run() {
                btnAlarm.setBackgroundColor(if (isRed) Color.RED else Color.WHITE)
                isRed = !isRed
                flashHandler.postDelayed(this, 500)
            }
        }

        flashHandler.post(flashRunnable)

        handler.postDelayed({
            mediaPlayer?.stop()
            mediaPlayer?.release()
            btnAlarm.setBackgroundColor(Color.TRANSPARENT)
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
                btnAlarm.text = getString(R.string.alarm_set_to, timeString)
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

    //+++++++ Create Boarder Around Clip Color ++++++++++++++++++++

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

    // ----------- Check if VCC is Enabled ------------------
    private fun updateVccLabel() {
        if (voiceControlEnabled) {
            txtVCCTourLbs.text = "Voice Control ON"
            txtVCCTourLbs.setTextColor(Color.parseColor("#00800D")) // Green
        } else {
            txtVCCTourLbs.text = "Manual Mode"
            txtVCCTourLbs.setTextColor(Color.parseColor("#FF000000")) // Black
        }
    }


    private fun handleCommand(command: String) {
        when (command) {
            "add a catch", "save fish", "save that", "tag fish", "record fish", "log catch" -> {
                showWeightPopup()
            }
            else -> {
                Toast.makeText(this, "Command recognized but no action assigned.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // ------------ VCC Enabled Set Up Voice Control ----------------
    override fun onSpeechResult(transcript: String) {
        voiceHelper.handleTranscript(transcript)
    }


    /**
     * Also required by the base.  You could use this
     * if you wanted the old “wake” event to kick off VCC,
     * but we’re using a double-tap listener instead.
     */
        // ------------ Double Tap Wakes App Up for VCC --------------
    override fun onVoiceWake() {
        // no-op
    }

}//################## END  ################################