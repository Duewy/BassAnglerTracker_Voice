package com.bramestorm.bassanglertracker

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
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
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.training.ParsedCatch
import com.bramestorm.bassanglertracker.training.VoiceCatchParse
import com.bramestorm.bassanglertracker.training.VoiceCommandHandler
import com.bramestorm.bassanglertracker.training.VoiceInteractionHelper
import com.bramestorm.bassanglertracker.training.VoiceResponseManager
import com.bramestorm.bassanglertracker.utils.GpsUtils
import com.bramestorm.bassanglertracker.utils.getMotivationalMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Locale.getDefault


class CatchEntryTournament : AppCompatActivity() {

    // Buttons
    private lateinit var btnTournamentCatch: Button
    private lateinit var btnMenu: Button
    private lateinit var btnMainPg: Button
    private lateinit var btnAlarm: Button
    private lateinit var tglVoiceOnLbs: ToggleButton

    private lateinit var txtGPSNotice: TextView

    // Alarm Variables
    private var alarmHour: Int = -1
    private var alarmMinute: Int = -1
    private var alarmTriggered: Boolean = false

    // Audio Variables
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent

    private lateinit var voiceResponseManager: VoiceResponseManager
    private lateinit var voiceHelper: VoiceInteractionHelper
    private lateinit var voiceCommandHandler: VoiceCommandHandler
    private var pendingParsedCatch: ParsedCatch? = null
    private var awaitingConfirmation = false

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

    private lateinit var txtTypeLetter1: TextView
    private lateinit var txtTypeLetter2: TextView
    private lateinit var txtTypeLetter3: TextView
    private lateinit var txtTypeLetter4: TextView
    private lateinit var txtTypeLetter5: TextView
    private lateinit var txtTypeLetter6: TextView

    private lateinit var txtColorLetter1: TextView
    private lateinit var txtColorLetter2: TextView
    private lateinit var txtColorLetter3: TextView
    private lateinit var txtColorLetter4: TextView
    private lateinit var txtColorLetter5: TextView
    private lateinit var txtColorLetter6: TextView


    private lateinit var totalRealWeight: TextView
    private lateinit var totalDecWeight: TextView


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
    private var lastTournamentCatch: CatchItem? = null

    // Request Codes
    private val requestAlarmSET = 1006
    private val recordAudioRequestCode = 101


    // ----------------- wait for POPUP WEIGHT VALUES  ------------------------
    private val weightEntryLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val weightTotalOz = data?.getIntExtra("weightTotalOz", 0) ?: 0
            val selectedSpecies = data?.getStringExtra("selectedSpecies") ?: ""
            val clipColor = data?.getStringExtra("clip_color") ?: ""

            Log.d(
                "DB_DEBUG",
                "✅ Received weightTotalOz: $weightTotalOz, selectedSpecies: $selectedSpecies, clip_color: $clipColor"
            )

            if (weightTotalOz > 0) {

                saveTournamentCatch(weightTotalOz, selectedSpecies, clipColor)
            }
        }
    }

    private fun checkAndRequestAudioPermission() {
        val permission = android.Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), recordAudioRequestCode)
        }
    }

    //================ ON CREATE =======================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view)

        dbHelper = CatchDatabaseHelper(this)
        btnTournamentCatch = findViewById(R.id.btnStartFishing)
        btnMenu = findViewById(R.id.btnMenu)
        btnMainPg = findViewById(R.id.btnMainPg)
        btnAlarm = findViewById(R.id.btnAlarm)
        txtGPSNotice = findViewById(R.id.txtGPSNotice)
        tglVoiceOnLbs = findViewById(R.id.tglVoiceOnLbs)

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



        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)


        //--------------- SETTING VOICE CONTROL ---------------------------
        checkAndRequestAudioPermission()
        //
        //    voiceHelper = VoiceInteractionHelper(this) { command ->
        //        handleVoiceCommand(command)  // Your command parser
        //    }
        //   voiceCommandHandler = VoiceCommandHandler(this) { message ->
        //       voiceHelper.speak(message)
        //   }


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        //---------------------------------------------------------------

        btnTournamentCatch.setOnClickListener { showWeightPopup() }

        btnMenu.setOnClickListener { startActivity(Intent(this, SetUpActivity::class.java)) }

        btnMainPg.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }

        //_______VOICE CONTROL FOR NOW ________________________________  REMOVE WHEN THE REAL VOICE CONTROL BUTTON IS IN SETUP PAGE

        tglVoiceOnLbs.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                try {
                    speechRecognizer.destroy()
                } catch (_: Exception) {}

                // ✅ Recreate recognizer
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                setupRecognizerListener()

                // ✅ Wait before calling startListening()
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d("VOICE", "▶️ Now calling startListening()")
                    speechRecognizer.startListening(speechIntent)
                }, 500) // 500ms seems to avoid busy error

            } else {
                try {
                    speechRecognizer.stopListening()
                    speechRecognizer.cancel()
                    speechRecognizer.destroy()
                } catch (_: Exception) {}
            }
        }








        //tglVoiceOnLbs.setOnCheckedChangeListener { _, isChecked ->
        //     if (isChecked) {
        //         voiceHelper.speak("I am ready to log your catch.") {
        //             Log.d("Voice", "🟢 TTS confirmed done. Starting recognizer.")
        //             voiceHelper.startListening()
        //         }
        //    } else {
        //        voiceHelper.speak("Voice mode turned off.")
        //        voiceHelper.stopListening()
        //     }
        // }

        // @@@@@@@@@@@@@ SET ALARM BTN @@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        btnAlarm.setOnClickListener {
            startActivityForResult(
                Intent(this, PopUpAlarm::class.java),
                requestAlarmSET
            )
        }

        val dbHelper = CatchDatabaseHelper(this)

        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)

        updateTournamentList()

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
        flashHandler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        speechRecognizer.destroy()
        // voiceHelper.shutdown()
    }


    /** ~~~~~~~~~~~~~ Opens the weight entry popup ~~~~~~~~~~~~~~~ */

    private fun showWeightPopup() {
        val intent = Intent(this, PopupWeightEntryTourLbs::class.java)
        intent.putExtra("isTournament", true)

        if (tournamentSpecies.equals("Large Mouth", true) || tournamentSpecies.equals(
                "Largemouth",
                true
            )
        ) {
            intent.putExtra("tournamentSpecies", "Large Mouth Bass")
        } else if (tournamentSpecies.equals(
                "Small Mouth",
                true
            ) || tournamentSpecies.equals("Smallmouth", true)
        ) {
            intent.putExtra("tournamentSpecies", "Small Mouth Bass")
        } else {
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
            totalLengthA8th = null,
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
        val totalLbs = totalWeightOz / 16
        val totalOz = totalWeightOz % 16

        totalRealWeight.text = totalLbs.toString()
        totalDecWeight.text = totalOz.toString()

        // !!!!!!!!!!!!!!!!!!!! MOTIVATIONAL TOASTS !!!!!!!!!!!!!!!!!!!!!!!!!!!
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

        Log.d("CLIP_COLOR", "🎨 Available Colors: $availableClipColors")

        clearTournamentTextViews()

        runOnUiThread {
            // Show 1 extra row (6th) for culled fish preview
            val loopLimit = minOf(sortedCatches.size, 6)

            for (i in 0 until loopLimit) {
                if (i >= realWeights.size) continue

                val catch = sortedCatches[i]
                val totalWeightOz = catch.totalWeightOz ?: 0
                val weightLbs = totalWeightOz / 16
                val weightOz = totalWeightOz % 16

                val clipColor = try {
                    ClipColor.valueOf(catch.clipColor?.uppercase() ?: "")
                } catch (_: Exception) {
                    ClipColor.RED
                }

                realWeights[i].text = weightLbs.toString()
                decWeights[i].text = weightOz.toString()

                val baseColor = ContextCompat.getColor(this, clipColor.resId)
                val layeredDrawable = createLayeredDrawable(baseColor)
                realWeights[i].background = layeredDrawable
                decWeights[i].background = layeredDrawable

                val textColor = if (clipColor == ClipColor.BLUE)
                    resources.getColor(R.color.clip_white, theme)
                else
                    resources.getColor(R.color.black, theme)

                realWeights[i].setTextColor(textColor)
                decWeights[i].setTextColor(textColor)

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

                typeLetters[i].text = getSpeciesCode(catch.species ?: "")
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
        isCullingEnabled: Boolean
    ): List<ClipColor> {
        val allCatches = dbHelper.getCatchesForToday(catchType, date)
        val sorted = allCatches.sortedByDescending { it.totalWeightOz ?: 0 }
        val topCatches = sorted.take(tournamentCatchLimit) // ✅ Always limit to top N

        val usedColors = topCatches.mapNotNull { it.clipColor }
            .mapNotNull {
                try {
                    ClipColor.valueOf(it.uppercase())
                } catch (_: Exception) {
                    null
                }
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

    //!!!!!!!!!!!!!!!! Get SPECIES Letter !!!!!!!!!!!!!!!!!

    private fun getSpeciesCode(species: String): String {
        return when (species.uppercase()) {
            "LARGE MOUTH" -> "LM"
            "SMALL MOUTH" -> "SM"
            "WALLEYE" -> "WE"
            "PIKE" -> "PK"
            "PERCH" -> "PH"
            "PANFISH" -> "PF"
            "CATFISH" -> "CF"
            "CRAPPIE" -> "CP"
            else -> "--"
        }
    }


    // +++++++++++++++++ CHECK ALARM ++++++++++++++++++++++++


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == requestAlarmSET && resultCode == Activity.RESULT_OK) {
            alarmHour = data?.getIntExtra("ALARM_HOUR", -1) ?: -1
            alarmMinute = data?.getIntExtra("ALARM_MINUTE", -1) ?: -1
            alarmTriggered = false

            if (alarmHour != -1 && alarmMinute != -1) {
                val amPm = if (alarmHour >= 12) "PM" else "AM"
                val displayHour = if (alarmHour % 12 == 0) 12 else alarmHour % 12
                val formattedMinute = String.format(getDefault(), "%02d", alarmMinute)
                val timeString = "$displayHour:$formattedMinute $amPm"

                btnAlarm.text = getString(R.string.alarm_set_to, timeString)
                val toastMessage = getString(R.string.alarm_toast_message, timeString)
                Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, alarmHour)
                    set(Calendar.MINUTE, alarmMinute)
                    set(Calendar.SECOND, 0)
                }

                val alarmIntent =
                    Intent(this, com.bramestorm.bassanglertracker.alarm.AlarmReceiver::class.java)
                val pendingIntent =
                    PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE)

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(
                        this,
                        "⚠️ Please enable exact alarm permission in settings.",
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                    return  // Exit early — no point in scheduling
                }
                Log.d("ALARM_DEBUG", "⏱ Attempting to schedule alarm at: ${calendar.time}")
                Log.d("ALARM_DEBUG", "🔍 System time now is: ${System.currentTimeMillis()}")

                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d("ALARM_DEBUG", "⏱ Attempting to schedule alarm at: ${calendar.time}")
                    Log.d("ALARM_DEBUG", "🔍 System time now is: ${System.currentTimeMillis()}")

                } catch (e: SecurityException) {
                    Log.e("ALARM_DEBUG", "❌ Cannot schedule exact alarm. SecurityException.", e)
                    Toast.makeText(
                        this,
                        "Exact alarm could not be scheduled (permissions)",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }
    }

//----------- END onActivityResult   (alarm) -------------------------------


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

    //%%%%%%%%%%%%%%%% Script for Voice Commands & InterActions  is inside the training.VoiceCommandHandler  %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


    // Recreate a fresh recognizer
// Attach the recognition listener to the existing recognizer
    private fun setupRecognizerListener() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("VOICE", "✅ Ready for speech")
                Toast.makeText(applicationContext, "🎧 Listening now...", Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {
                Log.d("VOICE", "🎙️ Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                Log.d("VOICE", "🛑 End of speech")
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission error"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown error"
                }

                Log.e("VOICE_ERROR", "❌ SpeechRecognizer error: $message ($error)")
                Toast.makeText(applicationContext, "Speech Error: $message", Toast.LENGTH_LONG).show()
                tglVoiceOnLbs.isChecked = false
            }

            override fun onResults(results: Bundle?) {
                val spokenText =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0) ?: ""
                Log.d("VOICE", "🗣️ Recognized: $spokenText")
                Toast.makeText(applicationContext, "You said: \"$spokenText\"", Toast.LENGTH_LONG).show()

                when {
                    spokenText.contains("hello", true) -> {
                        Toast.makeText(applicationContext, "Hey there, angler!", Toast.LENGTH_SHORT).show()
                    }
                    spokenText.contains("ready", true) -> {
                        Toast.makeText(applicationContext, "Ready to log your next catch!", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(applicationContext, "I'm listening, say a command!", Toast.LENGTH_SHORT).show()
                    }
                }

                tglVoiceOnLbs.isChecked = false
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }



        private fun handleVoiceCommand(command: String) {
            val parser = VoiceCatchParse()

            if (awaitingConfirmation) {
                val yesWords = listOf("yes", "that is correct", "correct", "that's right", "right")
                val noWords = listOf("no", "that's wrong", "incorrect")

                val confirmation = command.lowercase(Locale.ROOT)

                when {
                    yesWords.any { confirmation.contains(it) } -> {
                        pendingParsedCatch?.let { parsed ->
                            // Convert weight to total ounces
                            val totalWeightOz = (parsed.weightLbs * 16) + parsed.weightOz
                            val species = parsed.species
                            val clipColor = parsed.clipColor

                            saveTournamentCatch(totalWeightOz, species, clipColor)

                            voiceHelper.speak("Catch saved. We'll tally your weight next.")
                            pendingParsedCatch = null
                            awaitingConfirmation = false
                        }

                    }

                    noWords.any { confirmation.contains(it) } -> {
                        voiceHelper.speak("Okay, please try again.")
                        pendingParsedCatch = null
                        awaitingConfirmation = false
                    }

                    else -> {
                        voiceHelper.speak("Please confirm your catch by saying 'yes' or 'no'.")
                    }
                }

                return
            }

            val parsed = parser.parseVoiceCommand(command)

            if (parsed != null) {
                pendingParsedCatch = parsed
                awaitingConfirmation = true

                val response =
                    "OK, you caught a ${parsed.species}, weighing ${parsed.weightLbs} pounds and ${parsed.weightOz} ounces, on the ${parsed.clipColor} clip. Is this correct? Over."
                voiceHelper.speak(response)

            } else {
                voiceHelper.speak("Sorry, I couldn't understand your catch details. Please try again.")
            }

            if (command.lowercase().contains("over and out")) {
                voiceHelper.speak("Out.")
                voiceHelper.stopListening()
                return
            }

        }

}
//################## END  ################################