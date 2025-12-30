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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bramestorm.bassanglertracker.CatchEntryTournamentPounds.ClipColor
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


class CatchEntryTournamentKgs : BaseCatchEntryActivity() {


    // Buttons
    private lateinit var btnStartFishingKgs: Button
    private lateinit var btnSetUpKgs: Button
    private lateinit var btnMainKgs:Button
    private lateinit var btnAlarmKgs: Button
    private lateinit var dialogInstance: AlertDialog

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
    private lateinit var txtVCCTourKgs: TextView

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
        // ← outputs from this popup
        const val EXTRA_WEIGHT_KGS             = "totalWeightHundredthKg"        // Send & receive this
        const val EXTRA_SPECIES                = "selectedSpecies"      // Send this
        const val EXTRA_CLIP_COLOR             = "clip_color"           // Send this
        const val EXTRA_MEASURING_TYPE         = "measuringType"
        const val EXTRA_IS_TOURNAMENT          = "isTournament"
        const val EXTRA_CULLING_NUMBERS        = "Culling_Numbers"

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
            val weightTotalKgs = data?.getIntExtra(EXTRA_WEIGHT_KGS, 0) ?: 0
            val selectedSpecies = data?.getStringExtra(EXTRA_SPECIES) ?: ""
            val clipColor = data?.getStringExtra(EXTRA_CLIP_COLOR) ?: ""

            if (weightTotalKgs > 0) {
                saveTournamentCatch(weightTotalKgs, selectedSpecies, clipColor)
            }
        }
    }

 //================ ON CREATE =======================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_view_kgs)

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
             measurementUnit = VoiceInteractionHelper.MeasurementUnit.KG_G,
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
        btnStartFishingKgs = findViewById(R.id.btnStartFishingKgs)
        btnSetUpKgs = findViewById(R.id.btnSetUpKgs)
        btnMainKgs = findViewById(R.id.btnMainKgs)
        txtGPSNotice = findViewById(R.id.txtGPSNotice)
        txtVCCTourKgs = findViewById(R.id.txtVCCTourKgs)

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

        updateVccLabel()            // just shows user if VCC is Enabled or not...
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
        showWeightPopup()
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
        val weightTotalKgs    = intent.getIntExtra(EXTRA_WEIGHT_KGS, 0)
        val sp   = intent.getStringExtra(CatchEntryTournament.EXTRA_SPECIES).orEmpty()
        val clip = intent.getStringExtra(CatchEntryTournament.EXTRA_CLIP_COLOR).orEmpty()
        saveTournamentCatch(weightTotalKgs , sp, clip)
    }

    /** ~~~~~~~~~~~~~ Opens the weight entry popup ~~~~~~~~~~~~~~~ */

    private fun showWeightPopup() {

        val intent = Intent(this, PopupWeightEntryTourKgs::class.java).apply {

            putExtra(com.bramestorm.bassanglertracker.CatchEntryTournament.EXTRA_IS_TOURNAMENT, true)

            putExtra(com.bramestorm.bassanglertracker.CatchEntryTournament.EXTRA_TOURNAMENT_SPECIES,tournamentSpecies)

            // Send as an ArrayList so you can retrieve with getStringArrayListExtra
            val colorArray = availableClipColors.map { it.name }.toTypedArray()
            putExtra(CatchEntryTournament.EXTRA_AVAILABLE_CLIP_COLORS, colorArray)
        }
        weightEntryLauncher.launch(intent)
    }

    // ^^^^^^^^^^^^^ SAVE TOURNAMENT CATCH ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    private fun saveTournamentCatch(weightTotalKgs: Int, species: String, clipColor: String) {

        val cleanClipColor = clipColor.uppercase() // This came from the popup

        val normalized =
            SharedPreferencesManager.normalizeSpeciesName(species)

        val speciesInitial =
            SharedPreferencesManager.getSpeciesInitial(normalized)

        Log.d("DB_DEBUG", "✅ Assigned Clip Color: $cleanClipColor")

        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = species,
            totalWeightOz = null,
            totalWeightHundredthPounds = null,
            totalLengthQuarters = null,
            totalWeightHundredthKg = weightTotalKgs,
            totalLengthTenths = null,
            catchType = "tournament_kgs",
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

        val allCatches = dbHelper.getCatchesForToday(catchType = "tournament_kgs", formattedDate)
        val sortedCatches = allCatches.sortedByDescending { it.totalWeightHundredthKg ?: 0 }

        val tournamentCatches = if (isCullingEnabled) {
            sortedCatches.take(tournamentCatchLimit)
        } else {
            sortedCatches
        }

        availableClipColors = calculateAvailableClipColors(
            dbHelper,
            catchType = "tournament_kgs",
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

                // fill in the TextViews
                realWeightKgs[i].text = weightKgs.toString()  // ensure there is a "0" in 01 - 09
                decWeightKgs[i].text = weightDec.toString().padStart(2, '0')

                val clipColor = try {
                    ClipColor.valueOf(catch.clipColor?.uppercase() ?: "")
                } catch (_: Exception) {
                    ClipColor.RED
                }

                val baseColor = ContextCompat.getColor(this, clipColor.resId)
                val layeredDrawable = createLayeredDrawable(baseColor)
                realWeightKgs[i].background = layeredDrawable
                decWeightKgs[i].background = layeredDrawable

                //------------- If Clip is Blue then Text is White
                val textColor = if (clipColor == ClipColor.BLUE)
                    resources.getColor(R.color.clip_white, theme)
                else
                    resources.getColor(R.color.black, theme)

                realWeightKgs[i].setTextColor(textColor)
                decWeightKgs[i].setTextColor(textColor)

                realWeightKgs[i].invalidate()
                decWeightKgs[i].invalidate()

                colorLetters[i].text = when (clipColor.name) {
                    "BLUE"      -> "B"
                    "RED"       -> "R"
                    "GREEN"     -> "G"
                    "YELLOW"    -> "Y"
                    "ORANGE"    -> "O"
                    "WHITE"     -> "W"
                    else        -> "?"
                }

                typeLetters[i].text =
                    SharedPreferencesManager.getSpeciesInitial(catch.species)

                // **long-press to 📝 EDIT or DELETE 🚫 this exact item**
                realWeightKgs[i].setOnLongClickListener {
                    showTournamentEditDialog(sortedCatches[i])
                    true
                }
                decWeightKgs[i].setOnLongClickListener {
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
    }//$$$$$$$$$$$$$$$ END Update Tournament List  $$$$$$$$$$$$$$$$$$$$$$$$$$

    //########### Clear Tournament Text Views  ########################

    private fun clearTournamentTextViews() {

        val realWeights = listOf(
            firstRealWeightKgs, secondRealWeightKgs, thirdRealWeightKgs,
            fourthRealWeightKgs, fifthRealWeightKgs, sixthRealWeightKgs
        )

        val decWeights = listOf(
            firstDecWeightKgs, secondDecWeightKgs, thirdDecWeightKgs,
            fourthDecWeightKgs, fifthDecWeightKgs, sixthDecWeightKgs
        )

        val typeLetters = listOf(
            txtTypeLetter1, txtTypeLetter2, txtTypeLetter3,
            txtTypeLetter4, txtTypeLetter5, txtTypeLetter6
        )

        val colorLetters = listOf(
            txtKgsColorLetter1, txtKgsColorLetter2, txtKgsColorLetter3,
            txtKgsColorLetter4, txtKgsColorLetter5, txtKgsColorLetter6
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

        totalRealWeightKgs.text = "0"
        totalDecWeightKgs.text = "0"
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
    } //---------------- END Adjust the Text View Visibility ----------------

    //!!!!!!!!!!!!!!!! Get SPECIES Letter !!!!!!!!!!!!!!!!!
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
        // 1) inflate your custom layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_tournament_catch_kgs,null)

        // 2) find in-layout views
        val spnClipColor = dialogView.findViewById<Spinner>(R.id.spnClipColorKgs)
        val edtKgs       = dialogView.findViewById<EditText>(R.id.edtTourWeightKgs)
        val edtGrams     = dialogView.findViewById<EditText>(R.id.edtTourWeightGrams)
        val btnSave      = dialogView.findViewById<Button>(R.id.btnSaveEdtTourKgs)
        val btnCancel    = dialogView.findViewById<Button>(R.id.btnCancelEdtTourKgs)
        val btnDelete    = dialogView.findViewById<Button>(R.id.btnDeleteEdtTourKgs)

        // 3) prefill the fields from the CatchItem
        val totalHundredth = c.totalWeightHundredthKg ?: 0
        edtKgs.setText((totalHundredth / 100).toString())
        edtGrams.setText((totalHundredth % 100).toString())

        // 4) color box
        // Find available clip colors
        val availableColors = calculateAvailableClipColorsForEdit(
            dbHelper = dbHelper,
            catchType = "tournament_kgs", // or cms / inches / kgs / pounds
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
            val newKgs   = edtKgs.text.toString().toIntOrNull() ?: 0
            val newGrams = edtGrams.text.toString().toIntOrNull() ?: 0
            val newTotalKg = (newKgs * 100 + newGrams)
            val selectedClipColor = spnClipColor.selectedItem.toString()

            if(newTotalKg == 0){
                edtKgs.setText("0")
                edtGrams.setText("0")
                edtKgs.requestFocus()
                edtKgs.setSelection(edtKgs.text.length)

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(edtKgs, InputMethodManager.SHOW_IMPLICIT)

                positionedToast("🚫 Weight can not be 0.00 Kgs")
                return@setOnClickListener
            }

            dbHelper.updateCatch(
                catchId           = c.id,
                newWeightOz       = null,
                newWeightKg       = newTotalKg,
                newLengthQuarters = null,
                newLengthCm       = null,
                species           = c.species,
                clipColor = selectedClipColor
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
            updateTournamentList()
            dialogInstance.dismiss()
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
            txtVCCTourKgs.text = getString(R.string.vcc_on)
            txtVCCTourKgs.setBackgroundColor(ContextCompat.getColor(this, R.color.clip_yellow))
            txtVCCTourKgs.setTextColor(ContextCompat.getColor(this, R.color.clip_orange))// Orange
        } else {
            txtVCCTourKgs.text = getString(R.string.manual_mode)
            txtVCCTourKgs.setTextColor(ContextCompat.getColor(this, R.color.clip_blue))// blue
            txtVCCTourKgs.background = null
        }
    }

    // ------------ VCC Enabled Set Up Voice Control to Keep the BaseCatchEntryActivity connected ----------------
    override fun onSpeechResult(transcript: String) {
        // No-op: VCC now handled entirely in VoiceControlService
    }


}//################## END  ################################