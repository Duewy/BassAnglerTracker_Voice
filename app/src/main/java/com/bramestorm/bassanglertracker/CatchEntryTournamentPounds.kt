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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bramestorm.bassanglertracker.base.BaseCatchEntryActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.training.VoiceInteractionHelper
import com.bramestorm.bassanglertracker.utils.GpsUtils
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.getMotivationalMessage
import com.bramestorm.bassanglertracker.utils.positionedToast
import com.bramestorm.bassanglertracker.voice.VoiceControlService
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class CatchEntryTournamentPounds : BaseCatchEntryActivity() {


    // Buttons
    private lateinit var btnStartFishingPounds: Button
    private lateinit var btnSetUpPounds: Button
    private lateinit var btnMainPounds:Button
    private lateinit var dialogInstance: AlertDialog


    // Weight Display TextViews
    private lateinit var firstRealWeightPounds: TextView
    private lateinit var secondRealWeightPounds: TextView
    private lateinit var thirdRealWeightPounds: TextView
    private lateinit var fourthRealWeightPounds: TextView
    private lateinit var fifthRealWeightPounds: TextView
    private lateinit var sixthRealWeightPounds: TextView

    private lateinit var firstDecWeightPounds: TextView
    private lateinit var secondDecWeightPounds: TextView
    private lateinit var thirdDecWeightPounds: TextView
    private lateinit var fourthDecWeightPounds: TextView
    private lateinit var fifthDecWeightPounds: TextView
    private lateinit var sixthDecWeightPounds: TextView

    private lateinit var txtTypeLetter1:TextView
    private lateinit var txtTypeLetter2:TextView
    private lateinit var txtTypeLetter3:TextView
    private lateinit var txtTypeLetter4:TextView
    private lateinit var txtTypeLetter5:TextView
    private lateinit var txtTypeLetter6:TextView

    private lateinit var txtPoundsColorLetter1:TextView
    private lateinit var txtPoundsColorLetter2:TextView
    private lateinit var txtPoundsColorLetter3:TextView
    private lateinit var txtPoundsColorLetter4:TextView
    private lateinit var txtPoundsColorLetter5:TextView
    private lateinit var txtPoundsColorLetter6:TextView


    private lateinit var totalRealWeightPounds: TextView
    private lateinit var totalDecWeightPounds: TextView

    private lateinit var txtGPSNotice: TextView
    private lateinit var txtVCCTourPounds: TextView

    private var availableClipColors: List<ClipColor> = emptyList()

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
        // ← outputs from this popup
        const val EXTRA_WEIGHT_POUNDS          = "totalWeightHundredthPounds"        // Send & receive this
        const val EXTRA_SPECIES                = "selectedSpecies"                  // Send this
        const val EXTRA_CLIP_COLOR             = "clip_color"                       // Send this
        const val EXTRA_IS_TOURNAMENT          = "isTournament"

        // → inputs into this popup
        const val EXTRA_AVAILABLE_CLIP_COLORS  = "availableClipColors"  // Receive this list
        const val EXTRA_TOURNAMENT_SPECIES     = "tournamentSpecies"    // Receive this
    }


    // ----------------- Retrieves data from the Manual Mode POPUP   ------------------------
    private val weightEntryLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val weightTotalPounds = data?.getIntExtra(EXTRA_WEIGHT_POUNDS, 0) ?: 0
            val selectedSpecies = data?.getStringExtra(EXTRA_SPECIES) ?: ""
            val clipColor = data?.getStringExtra(EXTRA_CLIP_COLOR) ?: ""

            if (weightTotalPounds > 0) {
                saveTournamentCatch(weightTotalPounds, selectedSpecies, clipColor)
            }
        }
    }

    //================ ON CREATE =======================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view_pounds_decimal)

        // Push bottom-constrained views (like the AdView) above the system navigation bar
        val root = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }

        // 1️⃣ Read the VCC flag first
        voiceControlEnabled = intent.getBooleanExtra("VCC_ENABLED", false)
        Log.d("VCC_FLOW", "Voice control enabled: $voiceControlEnabled")

        // 2️⃣ Launch your VoiceControlService *only* if VCC is on
        if (voiceControlEnabled) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, VoiceControlService::class.java)
            )

            ContextCompat.registerReceiver(
                this,
                voiceCatchReceiver,
                IntentFilter("com.bramestorm.VOICE_CATCH_SAVED"),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

            // 3️⃣ And only then wire up your helper
            voiceHelper = VoiceInteractionHelper(
                activity        = this,
                onCommandAction = { transcript -> onSpeechResult(transcript) }
            )
        }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
            }
        }

        dbHelper = CatchDatabaseHelper(this)
        btnStartFishingPounds = findViewById(R.id.btnStartFishingPounds)
        btnSetUpPounds = findViewById(R.id.btnSetUpPounds)
        btnMainPounds = findViewById(R.id.btnMainPounds)
        txtGPSNotice = findViewById(R.id.txtGPSNotice)
        txtVCCTourPounds = findViewById(R.id.txtVCCTourPounds)

        // Assign TextViews
        firstRealWeightPounds = findViewById(R.id.firstRealWeightPounds)
        secondRealWeightPounds = findViewById(R.id.secondRealWeightPounds)
        thirdRealWeightPounds = findViewById(R.id.thirdRealWeightPounds)
        fourthRealWeightPounds = findViewById(R.id.fourthRealWeightPounds)
        fifthRealWeightPounds = findViewById(R.id.fifthRealWeightPounds)
        sixthRealWeightPounds = findViewById(R.id.sixthRealWeightPounds)

        firstDecWeightPounds = findViewById(R.id.firstDecWeightPounds)
        secondDecWeightPounds = findViewById(R.id.secondDecWeightPounds)
        thirdDecWeightPounds = findViewById(R.id.thirdDecWeightPounds)
        fourthDecWeightPounds = findViewById(R.id.fourthDecWeightPounds)
        fifthDecWeightPounds = findViewById(R.id.fifthDecWeightPounds)
        sixthDecWeightPounds = findViewById(R.id.sixthDecWeightPounds)

        txtTypeLetter1 = findViewById(R.id.txtTypeLetter1)
        txtTypeLetter2 = findViewById(R.id.txtTypeLetter2)
        txtTypeLetter3 = findViewById(R.id.txtTypeLetter3)
        txtTypeLetter4 = findViewById(R.id.txtTypeLetter4)
        txtTypeLetter5 = findViewById(R.id.txtTypeLetter5)
        txtTypeLetter6 = findViewById(R.id.txtTypeLetter6)

        totalRealWeightPounds = findViewById(R.id.totalRealWeightPounds)
        totalDecWeightPounds = findViewById(R.id.totalDecWeightPounds)

        txtPoundsColorLetter1 = findViewById(R.id.txtColorLetter1)
        txtPoundsColorLetter2 = findViewById(R.id.txtColorLetter2)
        txtPoundsColorLetter3 = findViewById(R.id.txtColorLetter3)
        txtPoundsColorLetter4 = findViewById(R.id.txtColorLetter4)
        txtPoundsColorLetter5 = findViewById(R.id.txtColorLetter5)
        txtPoundsColorLetter6 = findViewById(R.id.txtColorLetter6)

        //>>>>  Get Values from Set-Up Page <<<<<<<<<
        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)
        voiceControlEnabled  = intent.getBooleanExtra("VCC_ENABLED", false)

        //************ onClickListener **************************
        btnStartFishingPounds.setOnClickListener { showWeightPopup() }
        btnSetUpPounds.setOnClickListener { startActivity(Intent(this, SetUpActivity::class.java)) }
        btnMainPounds.setOnClickListener { startActivity(Intent(this,MainActivity::class.java)) }

        updateVccLabel()            // just shows user if VCC is Enabled or not...
        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)

        updateTournamentList()

            //------------------- AdMob for FREE Edition Only --------------------------
        val adView = findViewById<com.google.android.gms.ads.AdView?>(R.id.adViewCatchEntry)

        if (!BuildConfig.FEATURE_CATCHENTRY_BANNER_ADS || adView == null) {
            adView?.visibility = View.GONE
        } else {
            // Start collapsed so user never sees an empty banner strip
            adView.visibility = View.GONE

            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    adView.visibility = View.VISIBLE
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    adView.visibility = View.GONE
                }
            }

            adView.loadAd(AdRequest.Builder().build())
        }

    }//=============== END on Create ==============================

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
        showWeightPopup()
    }

    //------------- ON DESTROY --------------------
    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        if (::voiceHelper.isInitialized) voiceHelper.shutdown()
        toastTts?.shutdown()
        if (voiceControlEnabled) { unregisterReceiver(voiceCatchReceiver) }
        super.onDestroy()
    }


    private val voiceCatchReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("VCC_FLOW", "🧠 Voice catch saved — refreshing UI")
            updateTournamentList()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val weightTotalPounds    = intent.getIntExtra(EXTRA_WEIGHT_POUNDS, 0)
        val sp   = intent.getStringExtra(CatchEntryTournament.EXTRA_SPECIES).orEmpty()
        val clip = intent.getStringExtra(CatchEntryTournament.EXTRA_CLIP_COLOR).orEmpty()
        saveTournamentCatch(weightTotalPounds , sp, clip)
    }

    /** ~~~~~~~~~~~~~ Opens the weight entry popup ~~~~~~~~~~~~~~~ */

    private fun showWeightPopup() {

        val intent = Intent(this, PopupWeightEntryTourPounds::class.java).apply {

            putExtra(CatchEntryTournament.EXTRA_IS_TOURNAMENT, true)

            putExtra(CatchEntryTournament.EXTRA_TOURNAMENT_SPECIES,tournamentSpecies)

            // Send as an ArrayList so you can retrieve with getStringArrayListExtra
            val colorArray = availableClipColors.map { it.name }.toTypedArray()
            putExtra(CatchEntryTournament.EXTRA_AVAILABLE_CLIP_COLORS, colorArray)
        }
        weightEntryLauncher.launch(intent)
    }

    // ^^^^^^^^^^^^^ SAVE TOURNAMENT CATCH ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    private fun saveTournamentCatch(weightTotalPounds: Int, species: String, clipColor: String) {

        val cleanClipColor = clipColor.uppercase() // This came from the popup

        val speciesInitial =
            SharedPreferencesManager.getSpeciesInitial(this, species)

        Log.d("DB_DEBUG", "✅ Assigned Clip Color: $cleanClipColor")

        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = species,
            totalWeightOz = null,
            totalWeightHundredthPounds = weightTotalPounds,
            totalLengthQuarters = null,
            totalWeightHundredthKg =null,
            totalLengthTenths = null,
            catchType = "tournament_pounds",
            markerType = speciesInitial,
            clipColor = cleanClipColor
        )

        val result = dbHelper.insertCatch(catch)
        Log.d("DB_DEBUG", "✅ Catch Insert Result: $result, Stored Clip Color: ${catch.clipColor}")

        Toast.makeText(this, "$species Catch Saved!", Toast.LENGTH_SHORT).show()
        // ✅ Save the most recent catch for motivational messaging
        if (result) {
            lastTournamentCatch = catch
        }
        updateTournamentList()
    }// -------------- END Save Tournament Catch  -----------------------------


    // ``````````````` UPDATE TOTAL WEIGHT ```````````````````````
    private fun updateTotalWeight(tournamentCatches: List<CatchItem>) {
        // Always sort and limit to top N
        val catchesToUse = tournamentCatches
            .sortedByDescending { it.totalWeightHundredthPounds ?: 0 }
            .take(tournamentCatchLimit)  // ✅ Apply limit always

        val totalWeightPounds = catchesToUse.sumOf { it.totalWeightHundredthPounds ?: 0 }
        val totalPounds = totalWeightPounds / 100
        val totalDec = totalWeightPounds % 100

        totalRealWeightPounds.text = totalPounds.toString()
        totalDecWeightPounds.text = totalDec.toString().padStart(2, '0')

        // !!!!!!!!!!!!!!!!!!!! 👍 MOTIVATIONAL TOASTS 👍 !!!!!!!!!!!!!!!!!!!!!!!!!!!
        val currentCount = dbHelper
            .getCatchesForToday("tournament_pounds", getCurrentDate())
            .sortedByDescending { it.totalWeightHundredthPounds ?: 0 }
            .take(tournamentCatchLimit)
            .size

        if (currentCount >= 2) {
            lastTournamentCatch?.let {
                val message = getMotivationalMessage(this, it.id, tournamentCatchLimit, "pounds")
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

        val realWeightPounds = listOf(
            firstRealWeightPounds, secondRealWeightPounds, thirdRealWeightPounds,
            fourthRealWeightPounds, fifthRealWeightPounds, sixthRealWeightPounds
        )

        val decWeightPounds = listOf(
            firstDecWeightPounds, secondDecWeightPounds, thirdDecWeightPounds,
            fourthDecWeightPounds, fifthDecWeightPounds, sixthDecWeightPounds
        )

        val colorLetters = listOf(
            txtPoundsColorLetter1, txtPoundsColorLetter2, txtPoundsColorLetter3,
            txtPoundsColorLetter4, txtPoundsColorLetter5, txtPoundsColorLetter6
        )

        val typeLetters = listOf(
            txtTypeLetter1, txtTypeLetter2, txtTypeLetter3,
            txtTypeLetter4, txtTypeLetter5, txtTypeLetter6
        )

        val allCatches = dbHelper.getCatchesForToday(catchType = "tournament_pounds", formattedDate)
        val sortedCatches = allCatches.sortedByDescending { it.totalWeightHundredthPounds ?: 0 }

        val tournamentCatches = if (isCullingEnabled) {
            sortedCatches.take(tournamentCatchLimit)
        } else {
            sortedCatches
        }

        availableClipColors = calculateAvailableClipColors(
            dbHelper,
            catchType = "tournament_pounds",
            date = formattedDate,
            tournamentCatchLimit = tournamentCatchLimit
        )

        clearTournamentTextViews()

        runOnUiThread {
            val loopLimit = minOf(sortedCatches.size, tournamentCatchLimit + 1, 6)

            for (i in 0 until loopLimit) {
                if (i >= realWeightPounds.size) continue

                val catch = sortedCatches[i]
                val totalWeightPounds = catch.totalWeightHundredthPounds ?: 0
                val weightPounds = totalWeightPounds / 100
                val weightDec = totalWeightPounds % 100

                // fill in the TextViews
                realWeightPounds[i].text = weightPounds.toString()  // ensure there is a "0" in 01 - 09
                decWeightPounds[i].text = weightDec.toString().padStart(2, '0')

                val clipColor = try {
                    ClipColor.valueOf(catch.clipColor?.uppercase() ?: "")
                } catch (_: Exception) {
                    ClipColor.RED
                }

                val baseColor = ContextCompat.getColor(this, clipColor.resId)
                val layeredDrawable = createLayeredDrawable(baseColor)
                realWeightPounds[i].background = layeredDrawable
                decWeightPounds[i].background = layeredDrawable

                //------------- If Clip is Blue then Text is White
                val textColor = if (clipColor == ClipColor.BLUE)
                    resources.getColor(R.color.clip_white, theme)
                else
                    resources.getColor(R.color.black, theme)

                realWeightPounds[i].setTextColor(textColor)
                decWeightPounds[i].setTextColor(textColor)

                realWeightPounds[i].invalidate()
                decWeightPounds[i].invalidate()

                colorLetters[i].text = when (clipColor.name) {
                    "BLUE"      -> "B"
                    "YELLOW"    -> "Y"
                    "GREEN"     -> "G"
                    "ORANGE"    -> "O"
                    "WHITE"     -> "W"
                    "RED"       -> "R"
                    else        -> "?"
                }

                typeLetters[i].text =
                    SharedPreferencesManager.getSpeciesInitial(this, catch.species)

                // **long-press to 📝 EDIT or DELETE 🚫 this exact item**
                realWeightPounds[i].setOnLongClickListener {
                    showTournamentEditDialog(sortedCatches[i])
                    true
                }
                decWeightPounds[i].setOnLongClickListener {
                    showTournamentEditDialog(sortedCatches[i])
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
                            blinkTextViewTwice(fourthRealWeightPounds)
                            blinkTextViewTwice(fourthDecWeightPounds)
                        }
                        5 -> {
                            blinkTextViewTwice(fifthRealWeightPounds)
                            blinkTextViewTwice(fifthDecWeightPounds)
                        }
                        6 -> {
                            blinkTextViewTwice(sixthRealWeightPounds)
                            blinkTextViewTwice(sixthDecWeightPounds)
                        }
                    }
                }, 300)
            }
        }
    }//$$$$$$$$$$$$$$$ END Update Tournament List  $$$$$$$$$$$$$$$$$$$$$$$$$$

    //########### Clear Tournament Text Views  ########################

    private fun clearTournamentTextViews() {

        val realWeights = listOf(
            firstRealWeightPounds, secondRealWeightPounds, thirdRealWeightPounds,
            fourthRealWeightPounds, fifthRealWeightPounds, sixthRealWeightPounds
        )

        val decWeights = listOf(
            firstDecWeightPounds, secondDecWeightPounds, thirdDecWeightPounds,
            fourthDecWeightPounds, fifthDecWeightPounds, sixthDecWeightPounds
        )

        val typeLetters = listOf(
            txtTypeLetter1, txtTypeLetter2, txtTypeLetter3,
            txtTypeLetter4, txtTypeLetter5, txtTypeLetter6
        )

        val colorLetters = listOf(
            txtPoundsColorLetter1, txtPoundsColorLetter2, txtPoundsColorLetter3,
            txtPoundsColorLetter4, txtPoundsColorLetter5, txtPoundsColorLetter6
        )

        realWeights.forEach {
            it.text = ""
            it.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
            it.setTextColor(ContextCompat.getColor(this, R.color.black))
            it.setOnLongClickListener(null)
        }

        decWeights.forEach {
            it.text = ""
            it.setBackgroundColor(ContextCompat.getColor(this, R.color.grey))
            it.setTextColor(ContextCompat.getColor(this, R.color.black))
            it.setOnLongClickListener(null)
        }

        typeLetters.forEach { it.text = "" }
        colorLetters.forEach { it.text = "" }

        totalRealWeightPounds.text = "0"
        totalDecWeightPounds.text = "0"
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
        tournamentCatchLimit: Int
    ): List<ClipColor> {
        val allCatches = dbHelper.getCatchesForToday(catchType, date)
        val sorted = allCatches.sortedByDescending { it.totalWeightHundredthPounds ?: 0 }
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
                // Row 5 = culling candidate (dimmed)
                fifthRealWeightPounds.alpha = 0.3f
                fifthDecWeightPounds.alpha = 0.3f
                fifthRealWeightPounds.isEnabled = false
                fifthDecWeightPounds.isEnabled = false
                txtTypeLetter5.alpha = 0.3f
                txtPoundsColorLetter5.alpha = 0.3f

                // Row 6 = not visible at all
                sixthRealWeightPounds.visibility = View.INVISIBLE
                sixthDecWeightPounds.visibility = View.INVISIBLE
                txtTypeLetter6.visibility = View.INVISIBLE
                txtPoundsColorLetter6.visibility = View.INVISIBLE
            }
            5 -> {
                // Row 6 = culling candidate (dimmed)
                sixthRealWeightPounds.alpha = 0.3f
                sixthDecWeightPounds.alpha = 0.3f
                sixthRealWeightPounds.isEnabled = false
                sixthDecWeightPounds.isEnabled = false
                txtTypeLetter6.alpha = 0.3f
                txtPoundsColorLetter6.alpha = 0.3f
            }
            else -> {
                // Limit = 6, all rows fully visible
                fifthRealWeightPounds.alpha = 1.0f
                fifthDecWeightPounds.alpha = 1.0f
                fifthRealWeightPounds.isEnabled = true
                fifthDecWeightPounds.isEnabled = true
                txtTypeLetter5.alpha = 1.0f
                txtPoundsColorLetter5.alpha = 1.0f
                txtTypeLetter5.visibility = View.VISIBLE
                txtPoundsColorLetter5.visibility = View.VISIBLE

                sixthRealWeightPounds.visibility = View.VISIBLE
                sixthDecWeightPounds.visibility = View.VISIBLE
                sixthRealWeightPounds.alpha = 1.0f
                sixthDecWeightPounds.alpha = 1.0f
                sixthRealWeightPounds.isEnabled = true
                sixthDecWeightPounds.isEnabled = true
                txtTypeLetter6.alpha = 1.0f
                txtPoundsColorLetter6.alpha = 1.0f
                txtTypeLetter6.visibility = View.VISIBLE
                txtPoundsColorLetter6.visibility = View.VISIBLE
            }
        }
    } //---------------- END Adjust the Text View Visibility ----------------

    //******************* FOR 🥸 User 📝 EDIT Logged Weights ********************************

    private fun showTournamentEditDialog(c: CatchItem) {
        // 1) inflate your custom layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_tournament_catch_pounds,null)

        // 2) find in-layout views
        val spnClipColor = dialogView.findViewById<Spinner>(R.id.spnClipColorPounds)
        val edtPounds    = dialogView.findViewById<EditText>(R.id.edtTourWeightPounds)
        val edtLbsDecimal     = dialogView.findViewById<EditText>(R.id.edtTourWeightPoundsDecimal)
        val btnSave      = dialogView.findViewById<Button>(R.id.btnSaveEdtTourPounds)
        val btnCancel    = dialogView.findViewById<Button>(R.id.btnCancelEdtTourPounds)
        val btnDelete    = dialogView.findViewById<Button>(R.id.btnDeleteEdtTourPounds)

        // 3) prefill the fields from the CatchItem
        val totalHundredth = c.totalWeightHundredthPounds ?: 0
        edtPounds.setText((totalHundredth / 100).toString())
        edtLbsDecimal.setText((totalHundredth % 100).toString())

        clearOnceOnFocus(edtPounds)
        clearOnceOnFocus(edtLbsDecimal)

        // 4) color box
        // Find available clip colors
        val availableColors = calculateAvailableClipColorsForEdit(
            dbHelper = dbHelper,
            catchType = "tournament_pounds",
            date = getCurrentDate(),
            tournamentCatchLimit = tournamentCatchLimit,
            editingCatchId = c.id
        ).toMutableList().apply {
            if (!contains(c.clipColor)) {
                add(0, c.clipColor!!)
            }
        }

        // Load spinner with clip color of Catch Id
        val colorAdapter = ClipColorSpinnerAdapter(this, availableColors)
        spnClipColor.adapter = colorAdapter


        // 5) build & show the AlertDialog (rename to 'dlg' to avoid collision)
        dialogInstance = AlertDialog.Builder(this)
            .setTitle("Edit or Delete Catch")
            .setView(dialogView)
            .create()
        dialogInstance.show()

        // 6) Save button
        btnSave.setOnClickListener {
            val newPounds   = edtPounds.text.toString().toIntOrNull() ?: 0
            val newDecimal = edtLbsDecimal.text.toString().toIntOrNull() ?: 0
            val newTotalHundredthPounds = (newPounds * 100 + newDecimal)
            val selectedClipColor = spnClipColor.selectedItem.toString()

            if(newTotalHundredthPounds  == 0){
                edtPounds.setText("0")
                edtLbsDecimal.setText("0")
                edtPounds.requestFocus()
                edtPounds.setSelection(edtPounds.text.length)

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(edtPounds, InputMethodManager.SHOW_IMPLICIT)

                positionedToast("🚫 Weight can not be 0.00 Pounds")
                return@setOnClickListener
            }

            dbHelper.updateCatch(
                catchId           = c.id,
                newWeightOz       = null,
                newWeightPounds   = newTotalHundredthPounds ,
                newWeightKg       = null,
                newLengthQuarters = null,
                newLengthCm       = null,
                species           = c.species,
                clipColor         = selectedClipColor
            )
            updateTournamentList()
            dialogInstance.dismiss()
        }

        // 7) Cancel button
        btnCancel.setOnClickListener {
            dialogInstance.dismiss()
        }

        // 8) Delete button
        btnDelete.setOnClickListener {
            dbHelper.deleteCatch(c.id)
            dialogInstance.dismiss()
            updateTournamentList()
            Toast.makeText(this, "Catch Deleted", Toast.LENGTH_SHORT).show()
        }
    }//========== END of User Editing Logged Weights ==============================

    //----- Calculate Available Clips for EDIT Mode  --------------------------------


    private fun calculateAvailableClipColorsForEdit(
        dbHelper: CatchDatabaseHelper,
        catchType: String,
        date: String,
        tournamentCatchLimit: Int,
        editingCatchId: Int
    ): List<String> {

        val allCatches = dbHelper.getCatchesForToday(catchType, date)
            .sortedByDescending { it.totalWeightHundredthPounds ?: 0 }
            .take(tournamentCatchLimit)

        val usedColors = allCatches
            .filter { it.id != editingCatchId }   // 👈 exclude the one being edited
            .mapNotNull { it.clipColor }
            .map { it.uppercase() }
            .toSet()

        return ClipColor.entries
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

    // ----------- Show if VCC is Enabled ------------------
    private fun updateVccLabel() {
        if (voiceControlEnabled) {
            txtVCCTourPounds.text = getString(R.string.vcc_on)
            txtVCCTourPounds.setBackgroundColor(ContextCompat.getColor(this, R.color.clip_yellow))
            txtVCCTourPounds.setTextColor(ContextCompat.getColor(this, R.color.clip_orange))// Orange
        } else {
            txtVCCTourPounds.text = getString(R.string.manual_mode)
            txtVCCTourPounds.setTextColor(ContextCompat.getColor(this, R.color.clip_blue))// blue
            txtVCCTourPounds.background = null
        }
    }

    // ------------ VCC Enabled Set Up Voice Control to Keep the BaseCatchEntryActivity connected ----------------
    override fun onSpeechResult(transcript: String) {
        // No-op: VCC now handled entirely in VoiceControlService
    }


}//################## END  ################################