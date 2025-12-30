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
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bramestorm.bassanglertracker.CatchEntryTournamentPounds.ClipColor
import com.bramestorm.bassanglertracker.base.BaseCatchEntryActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.training.VoiceInteractionHelper
import com.bramestorm.bassanglertracker.utils.GpsUtils
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.getMotivationalMessage
import com.bramestorm.bassanglertracker.voice.VoiceControlService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class CatchEntryTournamentCentimeters :  BaseCatchEntryActivity() {


    // Buttons
    private lateinit var btnTournamentCatch:Button
    private lateinit var btnMainCms: Button
    private lateinit var btnSetUpCms: Button

    // Length Cms Display TextViews
    private lateinit var firstRealLengthCms: TextView
    private lateinit var secondRealLengthCms: TextView
    private lateinit var thirdRealLengthCms: TextView
    private lateinit var fourthRealLengthCms: TextView
    private lateinit var fifthRealLengthCms: TextView
    private lateinit var sixthRealLengthCms: TextView

    private lateinit var firstDecLengthCms: TextView
    private lateinit var secondDecLengthCms: TextView
    private lateinit var thirdDecLengthCms: TextView
    private lateinit var fourthDecLengthCms: TextView
    private lateinit var fifthDecLengthCms: TextView
    private lateinit var sixthDecLengthCms: TextView


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


    private lateinit var totalRealLengthCms: TextView
    private lateinit var totalDecLengthCms: TextView

    private lateinit var txtGPSNotice: TextView
    private lateinit var txtVCCTourCms: TextView

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

                                // ← outputs this to the popup
        const val EXTRA_LENGTH_CMS             = "totalLengthTenths"    // Send & receive this
        const val EXTRA_SPECIES                = "selectedSpecies"      // Send this
        const val EXTRA_CLIP_COLOR             = "clip_color"           // Send this
        const val EXTRA_MEASURING_TYPE         = "unitType"
        const val EXTRA_IS_TOURNAMENT          = "isTournament"
        const val EXTRA_CULLING_NUMBERS        = "Culling_Numbers"

                               // → inputs this from the popup
        const val EXTRA_AVAILABLE_CLIP_COLORS  = "availableClipColors"  // Receive this list
        const val EXTRA_TOURNAMENT_SPECIES     = "tournamentSpecies"    // Receive this
    }


    // ````````````` Retrieves data from the Manual Mode POPUP ````````````````````````
    private val lengthEntryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val totalLengthTenths = data.getIntExtra(EXTRA_LENGTH_CMS, 0)
                val species = data.getStringExtra(EXTRA_SPECIES) ?: ""
                val clipColor = data.getStringExtra(EXTRA_CLIP_COLOR) ?: ""

                if (totalLengthTenths > 0) {
                    saveTournamentCatch(totalLengthTenths, species, clipColor)
                }
            }
        }
    }

    //================ ON CREATE =======================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view_centimeters)

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
                measurementUnit = VoiceInteractionHelper.MeasurementUnit.CM,
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
        btnTournamentCatch = findViewById(R.id.btnStartFishingCms)
        btnMainCms = findViewById(R.id.btnMainCms)
        btnSetUpCms = findViewById(R.id.btnSetUpCms)
        txtGPSNotice = findViewById(R.id.txtGPSNotice)
        txtVCCTourCms = findViewById(R.id.txtVCCTourCms)

        // Assign TextViews
        firstRealLengthCms = findViewById(R.id.firstRealLengthCms)
        secondRealLengthCms = findViewById(R.id.secondRealLengthCms)
        thirdRealLengthCms = findViewById(R.id.thirdRealLengthCms)
        fourthRealLengthCms = findViewById(R.id.fourthRealLengthCms)
        fifthRealLengthCms = findViewById(R.id.fifthRealLengthCms)
        sixthRealLengthCms = findViewById(R.id.sixthRealLengthCms)

        firstDecLengthCms = findViewById(R.id.firstDecLengthCms)
        secondDecLengthCms = findViewById(R.id.secondDecLengthCms)
        thirdDecLengthCms = findViewById(R.id.thirdDecLengthCms)
        fourthDecLengthCms = findViewById(R.id.fourthDecLengthCms)
        fifthDecLengthCms = findViewById(R.id.fifthDecLengthCms)
        sixthDecLengthCms = findViewById(R.id.sixthDecLengthCms)

        txtTypeLetter1 = findViewById(R.id.txtTypeLetterCms1)
        txtTypeLetter2 = findViewById(R.id.txtTypeLetterCms2)
        txtTypeLetter3 = findViewById(R.id.txtTypeLetterCms3)
        txtTypeLetter4 = findViewById(R.id.txtTypeLetterCms4)
        txtTypeLetter5 = findViewById(R.id.txtTypeLetterCms5)
        txtTypeLetter6 = findViewById(R.id.txtTypeLetterCms6)

        totalRealLengthCms = findViewById(R.id.totalRealLengthCms)
        totalDecLengthCms = findViewById(R.id.totalDecLengthCms)

        txtTypeLetter1 = findViewById(R.id.txtTypeLetterCms1)
        txtTypeLetter2 =findViewById(R.id.txtTypeLetterCms2)
        txtTypeLetter3 = findViewById(R.id.txtTypeLetterCms3)
        txtTypeLetter4 = findViewById(R.id.txtTypeLetterCms4)
        txtTypeLetter5 = findViewById(R.id.txtTypeLetterCms5)
        txtTypeLetter6 = findViewById(R.id.txtTypeLetterCms6)

        txtColorLetter1 = findViewById(R.id.txtCmsColorLetter1)
        txtColorLetter2 = findViewById(R.id.txtCmsColorLetter2)
        txtColorLetter3 = findViewById(R.id.txtCmsColorLetter3)
        txtColorLetter4 = findViewById(R.id.txtCmsColorLetter4)
        txtColorLetter5 = findViewById(R.id.txtCmsColorLetter5)
        txtColorLetter6 = findViewById(R.id.txtCmsColorLetter6)


        //--- Retrieve the values from the Set Up page -------
        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)
        voiceControlEnabled  = intent.getBooleanExtra("VCC_ENABLED", false)

        btnTournamentCatch.setOnClickListener { showLengthPopup() }
        btnSetUpCms.setOnClickListener { startActivity(Intent(this, SetUpActivity::class.java)) }
        btnMainCms.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }

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
        showLengthPopup()
    }

    //------------- ON DESTROY ---------------------
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
        val cms   = intent.getIntExtra(EXTRA_LENGTH_CMS, 0)
        val sp   = intent.getStringExtra(EXTRA_TOURNAMENT_SPECIES).orEmpty()
        val clip = intent.getStringExtra(EXTRA_CLIP_COLOR).orEmpty()
        saveTournamentCatch(cms, sp, clip)
    }

    /** ~~~~~~~~~~~~~ Opens the weight entry popup ~~~~~~~~~~~~~~~ */

    private fun showLengthPopup() {

        val intent = Intent(this, PopupLengthEntryTourCms::class.java).apply {

            putExtra(EXTRA_IS_TOURNAMENT, true)

            putExtra(EXTRA_TOURNAMENT_SPECIES, tournamentSpecies)

            // Send as an ArrayList so you can retrieve with getStringArrayListExtra
            val colorArray = availableClipColors.map { it.name }.toTypedArray()
            putExtra(CatchEntryTournament.EXTRA_AVAILABLE_CLIP_COLORS,colorArray)
        }
        lengthEntryLauncher.launch(intent)
    }

    // ^^^^^^^^^^^^^ SAVE TOURNAMENT CATCH ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    private fun saveTournamentCatch(totalLengthTenths: Int, species: String, clipColor: String) {

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
            totalLengthQuarters = null,
            totalWeightHundredthKg = null,
            totalLengthTenths = totalLengthTenths,
            catchType = "tournament_cms",
            markerType = speciesInitial,
            clipColor = cleanClipColor
        )
        val result = dbHelper.insertCatch(catch)
        Toast.makeText(this, "$species Catch Saved!", Toast.LENGTH_SHORT).show()
        // ✅ Save the most recent catch for motivational messaging
        if (result) {
            lastTournamentCatch = catch
        }
        updateTournamentList()
    }// -------------- END Save Tournament Catch  -----------------------------


    // ``````````````` UPDATE TOTAL LENGTH ``````````````````````

    private fun updateTotalLength(tournamentCatches: List<CatchItem>) {
        // Always sort and limit to top N
        val catchesToUse = tournamentCatches
            .sortedByDescending { it.totalLengthTenths ?: 0 }
            .take(tournamentCatchLimit)  // ✅ Apply limit always

        val totalLengthCms = catchesToUse.sumOf { it.totalLengthTenths ?: 0 }
        val totalCms = totalLengthCms / 10
        val totalDec = totalLengthCms % 10

        totalRealLengthCms.text = totalCms.toString()
        totalDecLengthCms.text = totalDec.toString()

        // !!!!!!!!!!!!!!!!!!!! 👍 MOTIVATIONAL TOASTS 👍 !!!!!!!!!!!!!!!!!!!!!!!!!!!
        // todo Set up Better Scenarios
        val currentCount = dbHelper
            .getCatchesForToday("tournament_cms", getCurrentDate())
            .sortedByDescending { it.totalLengthTenths ?: 0 }
            .take(tournamentCatchLimit)
            .size

        if (currentCount >= 2) {
            lastTournamentCatch?.let {
                val message = getMotivationalMessage(this, it.id, tournamentCatchLimit, "tournament_cms")
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

        val realLengthCms = listOf(
            firstRealLengthCms, secondRealLengthCms, thirdRealLengthCms,
            fourthRealLengthCms, fifthRealLengthCms, sixthRealLengthCms
        )

        val decLengthCms = listOf(
            firstDecLengthCms, secondDecLengthCms, thirdDecLengthCms,
            fourthDecLengthCms, fifthDecLengthCms, sixthDecLengthCms
        )

        val colorLetters = listOf(
            txtColorLetter1, txtColorLetter2, txtColorLetter3,
            txtColorLetter4, txtColorLetter5, txtColorLetter6
        )

        val typeLetters = listOf(
            txtTypeLetter1, txtTypeLetter2, txtTypeLetter3,
            txtTypeLetter4, txtTypeLetter5, txtTypeLetter6
        )

        val allCatches = dbHelper.getCatchesForToday(catchType = "tournament_cms", formattedDate)
        val sortedCatches = allCatches.sortedByDescending { it.totalLengthTenths ?: 0 }

        val tournamentCatches = if (isCullingEnabled) {
            sortedCatches.take(tournamentCatchLimit)
        } else {
            sortedCatches
        }

        availableClipColors = calculateAvailableClipColors(
            dbHelper,
            catchType = "tournament_cms",
            date = formattedDate,
            tournamentCatchLimit = tournamentCatchLimit,
            isCullingEnabled = isCullingEnabled
        )
        Log.d("CLIP_COLOR", "🎨 Available Colors: $availableClipColors")
        clearTournamentTextViews()

        runOnUiThread {
            val loopLimit = minOf(sortedCatches.size, 6) // up to 6 total slots

            for (i in 0 until loopLimit) {
                if (i >= realLengthCms.size) continue

                val catch = sortedCatches[i]
                val totalLengthCms = catch.totalLengthTenths ?: 0
                val lengthCms = totalLengthCms / 10
                val lengthDec = totalLengthCms % 10

                val clipColor = try {
                    ClipColor.valueOf(catch.clipColor?.uppercase() ?: "")
                } catch (_: Exception) {
                    ClipColor.RED
                }

                realLengthCms[i].text = lengthCms.toString()
                decLengthCms[i].text = lengthDec.toString()

                val baseColor = ContextCompat.getColor(this, clipColor.resId)
                val layeredDrawable = createLayeredDrawable(baseColor)
                realLengthCms[i].background = layeredDrawable
                decLengthCms[i].background = layeredDrawable

                val textColor = if (clipColor == ClipColor.BLUE)
                    resources.getColor(R.color.clip_white, theme)
                else
                    resources.getColor(R.color.black, theme)

                realLengthCms[i].setTextColor(textColor)
                decLengthCms[i].setTextColor(textColor)

                realLengthCms[i].invalidate()
                decLengthCms[i].invalidate()

                // Clip color label
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
                realLengthCms[i].setOnLongClickListener {
                    showTournamentEditDialog(catch)
                    true
                }
                decLengthCms[i].setOnLongClickListener {
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
                            blinkTextViewTwice(fourthRealLengthCms)
                            blinkTextViewTwice(fourthDecLengthCms)
                        }
                        5 -> {
                            blinkTextViewTwice(fifthRealLengthCms)
                            blinkTextViewTwice(fifthDecLengthCms)
                        }
                        6 -> {
                            blinkTextViewTwice(sixthRealLengthCms)
                            blinkTextViewTwice(sixthDecLengthCms)
                        }
                    }
                }, 300)
            }
        }
    }



    //########### Clear Tournament Text Views  ########################

    private fun clearTournamentTextViews() {

        val realLengths = listOf(
            firstRealLengthCms, secondRealLengthCms, thirdRealLengthCms,
            fourthRealLengthCms, fifthRealLengthCms, sixthRealLengthCms
        )

        val decLengths = listOf(
            firstDecLengthCms, secondDecLengthCms, thirdDecLengthCms,
            fourthDecLengthCms, fifthDecLengthCms, sixthDecLengthCms
        )

        val typeLetters = listOf(
            txtTypeLetter1, txtTypeLetter2, txtTypeLetter3,
            txtTypeLetter4, txtTypeLetter5, txtTypeLetter6
        )

        val colorLetters = listOf(
            txtColorLetter1, txtColorLetter2, txtColorLetter3,
            txtColorLetter4, txtColorLetter5, txtColorLetter6
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

        totalRealLengthCms.text = "0"
        totalDecLengthCms.text = "0"
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
        val sorted = allCatches.sortedByDescending { it.totalLengthTenths ?: 0 }
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
                fifthRealLengthCms.alpha = 0.3f
                fifthDecLengthCms.alpha = 0.3f
                fifthRealLengthCms.isEnabled = false
                fifthDecLengthCms.isEnabled = false
                sixthRealLengthCms.visibility = View.INVISIBLE
                sixthDecLengthCms.visibility = View.INVISIBLE
            }
            5 -> {
                sixthRealLengthCms.alpha = 0.3f
                sixthDecLengthCms.alpha = 0.3f
                sixthRealLengthCms.isEnabled = false
                sixthDecLengthCms.isEnabled = false
                txtTypeLetter6.isEnabled = false
            }
            else -> {
                fifthRealLengthCms.alpha = 1.0f
                fifthDecLengthCms.alpha = 1.0f
                fifthRealLengthCms.isEnabled = true
                fifthDecLengthCms.isEnabled = true
                sixthRealLengthCms.visibility = View.VISIBLE
                sixthDecLengthCms.visibility = View.VISIBLE
                sixthRealLengthCms.alpha = 1.0f
                sixthDecLengthCms.alpha = 1.0f
                sixthRealLengthCms.isEnabled = true
                sixthDecLengthCms.isEnabled = true
            }
        }
    }

    //!!!!!!!!!!!!!!!! Get SPECIES Letters for Side Text !!!!!!!!!!!!!!!!!
    //todo Follow the APPLE Xcode method !!!!!
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


    //******************* FOR 🥸 User 📝 EDIT Logged Weights ********************************

    private fun showTournamentEditDialog(c: CatchItem) {
        // 1) inflate your layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_tournament_catch_cms, null)

        // 2) find your views
        val spnClipColor = dialogView.findViewById<Spinner>(R.id.spnClipColorCms)
        val edtCms      = dialogView.findViewById<EditText>(R.id.edtTourLengthCms)
        val edtDec      = dialogView.findViewById<EditText>(R.id.edtTourLengthDec)
        val btnSave     = dialogView.findViewById<Button>(R.id.btnSaveEdtTourCms)
        val btnCancel   = dialogView.findViewById<Button>(R.id.btnCancelEdtTourCms)
        val btnDelete   = dialogView.findViewById<Button>(R.id.btnDeleteEdtTourCms)

        // 3) prefill from CatchItem.totalLengthTenths (which stores millimeters)
        val totalTenths = c.totalLengthTenths  ?: 0
        edtCms.setText(( totalTenths / 10).toString())
        edtDec.setText(( totalTenths % 10).toString())

        clearOnceOnFocus(edtCms)
        clearOnceOnFocus(edtDec)

        // 4) show clip-color box
        val availableColors = calculateAvailableClipColorsForEdit(
            dbHelper = dbHelper,
            catchType = "tournament_cms", // or cms / inches / kgs / pounds
            date = getCurrentDate(),
            tournamentCatchLimit = tournamentCatchLimit,
            editingCatchId = c.id
        ).toMutableList().apply {
            if (!contains(c.clipColor)) {
                add(0, c.clipColor!!)
            }
        }


        val colorAdapter = ClipColorSpinnerAdapter(this, availableColors)
        spnClipColor.adapter = colorAdapter


        // 5) build & show **one** dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit or Delete Catch")
            .setView(dialogView)
            .create()
        dialog.show()

        // 6) Save → write into the tenths column
        btnSave.setOnClickListener {
            val newCms  = edtCms.text.toString().toIntOrNull() ?: 0
            val newDec = edtDec.text.toString().toIntOrNull() ?: 0
            val newLengthCm  = (newCms* 10 + newDec)
            val selectedClipColor = spnClipColor.selectedItem.toString()

            dbHelper.updateCatch(
                catchId = c.id,
                newWeightOz = null,
                newWeightKg = null,
                newLengthQuarters = null,
                newLengthCm = newLengthCm,
                species = c.species,
                clipColor = selectedClipColor
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

    //----- Calculate Available Clips for EDIT Mode  --------------------------------


    private fun calculateAvailableClipColorsForEdit(
        dbHelper: CatchDatabaseHelper,
        catchType: String,
        date: String,
        tournamentCatchLimit: Int,
        editingCatchId: Int
    ): List<String> {

        val allCatches = dbHelper.getCatchesForToday(catchType, date)
            .sortedByDescending {
                it.totalWeightOz
                    ?: it.totalWeightHundredthKg
                    ?: it.totalLengthQuarters
                    ?: it.totalLengthTenths
                    ?: 0
            }
            .take(tournamentCatchLimit)

        val usedColors = allCatches
            .filter { it.id != editingCatchId }   // 👈 exclude the one being edited
            .mapNotNull { it.clipColor }
            .map { it.uppercase() }
            .toSet()

        return com.bramestorm.bassanglertracker.CatchEntryTournamentPounds.ClipColor.entries
            .map { it.name }
            .filter { it !in usedColors }
    }

    //----- END Calculate Available Clips for EDIT Mode  --------------------------------

    private fun clearOnceOnFocus(editText: EditText) {
        editText.onFocusChangeListener = object : View.OnFocusChangeListener {
            private var cleared = false
            override fun onFocusChange(v: View?, hasFocus: Boolean) {
                if (hasFocus && !cleared) {
                    editText.text.clear()
                    cleared = true
                }
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

    // @@@@@@@@@@@@@ BLINK for the Smallest Fish on Full Culling List @@@@@@@@@@@@@@@@@@@@@@@@@

    private fun blinkTextViewTwice(textView: TextView) {
        val blink = AnimationUtils.loadAnimation(this, R.anim.blink)

        // Delay 1 second before first blink
        Handler(Looper.getMainLooper()).postDelayed({
            textView.startAnimation(blink)

            // Delay slightly before doing the second blink
            Handler(Looper.getMainLooper()).postDelayed({
                textView.startAnimation(blink)
            }, 700) // Wait ~0.7 sec blink duration
        }, 1000) // Initial 1.0 second delay
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
            txtVCCTourCms.text = getString(R.string.vcc_on)
            txtVCCTourCms.setBackgroundColor(ContextCompat.getColor(this, R.color.clip_yellow))
            txtVCCTourCms.setTextColor(ContextCompat.getColor(this, R.color.clip_orange))// Orange
        } else {
            txtVCCTourCms.text = getString(R.string.manual_mode)
            txtVCCTourCms.setTextColor(ContextCompat.getColor(this, R.color.clip_blue))// blue
            txtVCCTourCms.background = null
        }
    }

    // ------------ VCC Enabled Set Up Voice Control to Keep the BaseCatchEntryActivity connected ----------------
    override fun onSpeechResult(transcript: String) {
        // No-op: VCC now handled entirely in VoiceControlService
    }


}//################## END  ################################