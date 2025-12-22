package com.bramestorm.bassanglertracker

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bramestorm.bassanglertracker.base.BaseCatchEntryActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.training.VoiceInteractionHelper
import com.bramestorm.bassanglertracker.utils.GpsUtils
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.getMotivationalMessage
import com.bramestorm.bassanglertracker.utils.positionedToast
import com.bramestorm.bassanglertracker.voice.VoiceControlService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class CatchEntryTournamentInches : BaseCatchEntryActivity()  {


    // Buttons
    private lateinit var btnTournamentCatch:Button
    private lateinit var btnMainInches: Button
    private lateinit var btnSetUpInches: Button

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
    private lateinit var tts: TextToSpeech
    private var toastTts: TextToSpeech? = null
    private var voiceControlEnabled = false
    private lateinit var voiceHelper: VoiceInteractionHelper
    lateinit var userVoiceMap: MutableMap<String, String>       //todo Correct with Mispronunciations ReWrite the Word/Phrase DataBase


    // Tournament Configuration
    private var tournamentCatchLimit: Int = 4
    private var measurementSystem: String = "weight"
    private var isCullingEnabled: Boolean = false
    private var typeOfMarkers: String = "Color"
    private var tournamentSpecies: String = "Unknown"
    private var lastTournamentCatch: CatchItem? = null

    companion object {
        const val EXTRA_LENGTH_INCHES          = "totalLengthQuarters"    // Send & receive this from this popup
        const val EXTRA_SPECIES                = "selectedSpecies"      // Send this
        const val EXTRA_CLIP_COLOR             = "clip_color"           // Send this
        const val EXTRA_MEASURING_TYPE         = "unitType"
        const val EXTRA_IS_TOURNAMENT          = "isTournament"
        const val EXTRA_CULLING_NUMBERS        = "Culling_Numbers"

        // → inputs into this popup
        const val EXTRA_AVAILABLE_CLIP_COLORS  = "availableClipColors"  // Receive this list
        const val EXTRA_TOURNAMENT_SPECIES     = "tournamentSpecies"    // Receive this
    }


    // ````````````` Retrieves data from the Manual Mode POPUP ````````````````````````
    private val entryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val totalLengthQuarters = result.data!!.getIntExtra(EXTRA_LENGTH_INCHES, 0)
            val sp = result.data!!.getStringExtra(EXTRA_SPECIES).orEmpty()
            val clip = result.data!!.getStringExtra(EXTRA_CLIP_COLOR).orEmpty()

           if(totalLengthQuarters > 0 ) {
               saveTournamentCatch(totalLengthQuarters, sp, clip)
           }
        }
    }

    //================ ON CREATE =======================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view_inches)

        // 1️⃣ Read the VCC flag first
        voiceControlEnabled = intent.getBooleanExtra("VCC_ENABLED", false)
        Log.d("VCC_FLOW", "Voice control enabled: $voiceControlEnabled")

        // 2️⃣ Launch your VoiceControlService *only* if VCC is on
        if (voiceControlEnabled) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, VoiceControlService::class.java)
            )
            LocalBroadcastManager.getInstance(this)
                .registerReceiver(
                    voiceCatchReceiver,
                    IntentFilter("com.bramestorm.VOICE_CATCH_SAVED")
                )

            // 3️⃣ And only then wire up your helper
            voiceHelper = VoiceInteractionHelper(
                activity        = this,
                measurementUnit = VoiceInteractionHelper.MeasurementUnit.LBS_OZ,
                isTournament    = true,
                onCommandAction = { transcript -> onSpeechResult(transcript) }
            )
        }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
            }
        }

        dbHelper = CatchDatabaseHelper(this)
        btnTournamentCatch = findViewById(R.id.btnStartFishingInches)
        btnMainInches = findViewById(R.id.btnMainInches)
        btnSetUpInches = findViewById(R.id.btnSetUpInches)
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

        //--- Retrieve the values from the Set Up page -------
        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)
        voiceControlEnabled  = intent.getBooleanExtra("VCC_ENABLED", false)     // Is the app in VCC mode?

        //----ADD a CATCH button is clicked -----------
        btnTournamentCatch.setOnClickListener {showLengthInchesPopup()}
        btnSetUpInches.setOnClickListener { startActivity(Intent(this, SetUpActivity::class.java)) }
        btnMainInches.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }

        updateVccLabel()         // just shows user if VCC is Enabled or not...
        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)

        updateTournamentList()
         }
// ~~~~~~~~~~~~~~~~~~~~~ END ON CREATE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

override val dialog: Any
    get() = throw UnsupportedOperationException("BaseCatchEntryActivity.dialog is unused in this subclass")


    // ------------- On RESUME --------- Check GPS  Statues --------------
    override fun onResume() {
        super.onResume()
        updateVccLabel()            // just shows user if VCC is Enabled or not...
        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)
        updateTournamentList()
    }

    //----------- On Manual Wake ------------------------
    override fun onManualWake() {
        showLengthInchesPopup()
    }

    //------------- ON DESTROY ------------------
    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
        if (::voiceHelper.isInitialized) voiceHelper.shutdown()
        toastTts?.shutdown()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(voiceCatchReceiver)
    }


    private val voiceCatchReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("VCC_FLOW", "🧠 Voice catch saved — refreshing UI")
            updateTournamentList()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val totalLengthQuarters = intent.getIntExtra(EXTRA_LENGTH_INCHES, 0)
        val sp   = intent.getStringExtra(CatchEntryTournament.EXTRA_SPECIES).orEmpty()
        val clip = intent.getStringExtra(CatchEntryTournament.EXTRA_CLIP_COLOR).orEmpty()
        saveTournamentCatch(totalLengthQuarters, sp, clip)
    }

    /** ~~~~~~~~~~~~~ Opens the weight entry popup ~~~~~~~~~~~~~~~ */

    private fun showLengthInchesPopup() {

        val intent = Intent(this,PopupLengthEntryTourInches::class.java).apply {

            putExtra(EXTRA_IS_TOURNAMENT, true)  // Tell the Popup this is a Tournament

            // Send the Species from the Set Up page on to the Popup
            putExtra(EXTRA_TOURNAMENT_SPECIES, tournamentSpecies)

            // Send as an ArrayList so you can retrieve with getStringArrayListExtra
            val colorArray = availableClipColors.map { it.name }.toTypedArray()
            putExtra(CatchEntryTournament.EXTRA_AVAILABLE_CLIP_COLORS,colorArray)
        }
        entryLauncher.launch(intent)
    }


    // ^^^^^^^^^^^^^ SAVE TOURNAMENT CATCH ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    private fun saveTournamentCatch(totalLengthQuarters: Int, species: String, clipColor: String) {

        val cleanClipColor = clipColor.uppercase() // This came from the popup

        val normalized =
            SharedPreferencesManager.normalizeSpeciesName(species)

        val speciesInitial =
            SharedPreferencesManager.getSpeciesInitial(normalized)

        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = species,
            totalWeightOz = null,
            totalWeightHundredthPounds = null,
            totalLengthTenths = null,
            totalWeightHundredthKg = null,
            totalLengthQuarters = totalLengthQuarters,
            catchType = "tournament_inches",
            markerType = speciesInitial,
            clipColor = cleanClipColor
        )
        val result = dbHelper.insertCatch(catch)

        Toast.makeText(this, "$species Catch Saved!", Toast.LENGTH_SHORT).show()

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
        totalDecLengthInches.text = totalDec.toString()

        // !!!!!!!!!!!!!!!!!!!! 👍 MOTIVATIONAL TOASTS 👍 !!!!!!!!!!!!!!!!!!!!!!!!!!!
        // todo Set up Better Scenarios
        val currentCount = dbHelper
            .getCatchesForToday("inches", getCurrentDate())
            .sortedByDescending { it.totalLengthQuarters ?: 0 }
            .take(tournamentCatchLimit)
            .size

        if (currentCount >= 2) {
            lastTournamentCatch?.let {
                val message = getMotivationalMessage(this, it.id, tournamentCatchLimit, "lbs")
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                    if (voiceControlEnabled) {
                        toastTts = TextToSpeech(this) { status ->
                            if (status == TextToSpeech.SUCCESS) {
                                toastTts?.language = Locale.getDefault()
                                toastTts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "TTS_MOTIVATION")

                                // Optional: shut down after 4 seconds to free memory
                                Handler(Looper.getMainLooper()).postDelayed({
                                    toastTts?.shutdown()
                                    toastTts = null
                                }, 4000)
                            }
                        }
                    }
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

        val allCatches = dbHelper.getCatchesForToday(catchType = "tournament_inches", formattedDate)
        val sortedCatches = allCatches.sortedByDescending { it.totalLengthQuarters ?: 0 }

        val tournamentCatches = if (isCullingEnabled) {
            sortedCatches.take(tournamentCatchLimit)
        } else {
            sortedCatches
        }

        availableClipColors = calculateAvailableClipColors(
            dbHelper,
            catchType = "tournament_inches",
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

                typeLetters[i].text =
                    SharedPreferencesManager.getSpeciesInitial(catch.species)

                // **long-press to 📝 EDIT or DELETE 🚫 this exact item**
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

        val realLengths = listOf(
            firstRealLengthInches, secondRealLengthInches, thirdRealLengthInches,
            fourthRealLengthInches, fifthRealLengthInches, sixthRealLengthInches
        )

        val decLengths = listOf(
            firstDecLengthInches, secondDecLengthInches, thirdDecLengthInches,
            fourthDecLengthInches, fifthDecLengthInches, sixthDecLengthInches
        )

        val typeLetters = listOf(
            txtTypeLetterInches1, txtTypeLetterInches2, txtTypeLetterInches3,
            txtTypeLetterInches4, txtTypeLetterInches5, txtTypeLetterInches6
        )

        val colorLetters = listOf(
            txtInchesColorLetter1, txtInchesColorLetter2, txtInchesColorLetter3,
            txtInchesColorLetter4, txtInchesColorLetter5, txtInchesColorLetter6
        )

        realLengths.forEach {
            it.text = ""
            it.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
            it.setTextColor(ContextCompat.getColor(this, R.color.black))
            it.setOnLongClickListener(null)
        }

        decLengths.forEach {
            it.text = ""
            it.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
            it.setTextColor(ContextCompat.getColor(this, R.color.black))
            it.setOnLongClickListener(null)
        }

        typeLetters.forEach { it.text = "" }
        colorLetters.forEach { it.text = "" }

        totalRealLengthInches.text = "0"
        totalDecLengthInches.text = "0"
    }


    // %%%%%%%%%%%% Clip Color assignment  %%%%%%%%%%%%%%%%%%%%%%%

    enum class ClipColor(val resId: Int) {
        BLUE(R.color.clip_blue),
        YELLOW(R.color.clip_yellow),
        GREEN(R.color.clip_green),
        ORANGE(R.color.clip_orange),
        WHITE(R.color.clip_white),
        RED(R.color.clip_red);
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
    } //---------------- END Adjust the Text View Visibility ----------------

    //!!!!!!!!!!!!!!!! Get SPECIES Letters for Side Text !!!!!!!!!!!!!!!!!
    private fun getSpeciesCode(species: String): String {
        val u = species.uppercase(Locale.US)
        return when {
            u.startsWith("LARGE MOUTH")  -> "LM"
            u.startsWith("LARGEMOUTH")  -> "LM"
            u.startsWith("SMALL MOUTH")  -> "SM"
            u.startsWith("SPOTTEDBASS")  -> "SB"
            u == "SPOTTED BASS"   -> "SB"
            u == "WALLEYE"        -> "WE"
            u == "PIKE"           -> "PK"
            u =="PERCH"           -> "PH"
            u == "PANFISH"        -> "PF"
            u =="CATFISH"         -> "CF"
            u == "CRAPPIE"       -> "CP"
            else          -> "--"
        }
    } //------------ END Get Species Codes ----------------

    //******************* FOR 🥸 User 📝 EDIT Logged Lengths ********************************

    private fun showTournamentEditDialog(c: CatchItem) {
        // 1) inflate your 4ths layout
        val dialogView = layoutInflater.inflate(
            R.layout.dialog_edit_tournament_catch_inches,
            null
        )

        // 2) find your views
        val spnClipColor = dialogView.findViewById<Spinner>(R.id.spnClipColorInches)
        val edtInches             = dialogView.findViewById<EditText>(R.id.edtTourLengthInches)            // hint="inches"
        val edtQuartersOfInch     = dialogView.findViewById<EditText>(R.id.edtTourLengthQuarters)          // hint="⁄4ths"
        val btnSave               = dialogView.findViewById<Button>(R.id.btnSaveEdtTourInches)
        val btnCancel             = dialogView.findViewById<Button>(R.id.btnCancelEdtTourInches)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnDeleteEdtTourInches)

        // 3) prefill from CatchItem.totalLengthQuarters (which stores quarters)
        val totalQuarters = c.totalLengthQuarters ?: 0
        edtInches.setText((totalQuarters / 4).toString())
        edtQuartersOfInch.setText((totalQuarters % 4).toString())

        // 4) show clip-color box
        val allClipColors = CatchEntryTournamentInches.ClipColor.entries.map { it.name }.toMutableList()
        val currentColor = c.clipColor ?: "RED"
        val availableColors = allClipColors.toMutableList().apply {
            remove(currentColor)
            add(0, currentColor)
        }

        val colorAdapter = ClipColorSpinnerAdapter(this, availableColors)
        spnClipColor.adapter = colorAdapter

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
            val selectedClipColor = spnClipColor.selectedItem.toString()

            if (newTotalInches == 0) {
                edtInches.setText("0")
                edtQuartersOfInch.setText("0")
                edtInches.requestFocus()
                edtQuartersOfInch.setSelection(edtInches.text.length)

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(edtInches, InputMethodManager.SHOW_IMPLICIT)

                positionedToast("🚫 Length cannot be 0 Inches 0/4 !")
                return@setOnClickListener
            }

            dbHelper.updateCatch(
                catchId           = c.id,
                newWeightOz       = null,
                newWeightKg       = null,
                newLengthQuarters = newTotalInches,
                newLengthCm       = null,
                species           = c.species,
                clipColor         = selectedClipColor
            )

            updateTournamentList()
            dialog.dismiss()
        }

        // 7)  Cancel button
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // 8) Delete button
        btnDelete.setOnClickListener {
            dbHelper.deleteCatch(c.id) // ensure `catch.id` is in scope
            dialog.dismiss()
            updateTournamentList()
        }
    }//========== END of User Editing Logged Length ==============================

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


    override fun onSpeechResult(transcript: String) {
        // VCC handled elsewhere; no-op or forward to your VoiceControlService
    }



}//################## END  ################################
