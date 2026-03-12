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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bramestorm.bassanglertracker.PopupWeightEntryLbs.MinMaxInputFilter
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


class CatchEntryTournament : BaseCatchEntryActivity() {

    // Buttons
    private lateinit var btnTournamentCatch: Button
    private lateinit var btnMenu: Button
    private lateinit var btnMainPg:Button

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
        const val EXTRA_WEIGHT_OZ              = "weightTotalOz"        // Send & receive this
        const val EXTRA_SPECIES                = "selectedSpecies"      // Send this
        const val EXTRA_CLIP_COLOR             = "clip_color"           // Send this
        const val EXTRA_IS_TOURNAMENT          = "isTournament"

        // → inputs into this popup
        const val EXTRA_AVAILABLE_CLIP_COLORS  = "availableClipColors"  // Receive this list
        const val EXTRA_TOURNAMENT_SPECIES     = "tournamentSpecies"    // Receive this
    }


    // ````````````` Retrieves data from the Manual Mode POPUP ````````````````````````
    private val entryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val oz = result.data!!.getIntExtra(EXTRA_WEIGHT_OZ, 0)
            val sp = result.data!!.getStringExtra(EXTRA_SPECIES).orEmpty()
            val clip = result.data!!.getStringExtra(EXTRA_CLIP_COLOR).orEmpty()

            if (oz > 0) {
                saveTournamentCatch(oz, sp, clip)
            }
        }
    }

    //================START - ON CREATE =======================================
        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
               setContentView(R.layout.activity_tournament_view)

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
                            measurementUnit = VoiceInteractionHelper.MeasurementUnit.LBS_OZ,
                            isTournament    = true,
                            onCommandAction = { transcript -> onSpeechResult(transcript) }
                                )

                tts = TextToSpeech(this) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        tts?.language = Locale.getDefault()
                    }
            }

        }

        dbHelper = CatchDatabaseHelper(this)
        btnTournamentCatch = findViewById(R.id.btnStartFishing)
        btnMenu = findViewById(R.id.btnMenu)
        btnMainPg = findViewById(R.id.btnMainPg)
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


        //>>>>  Get Values from Set-Up Page <<<<<<<<<
        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)
        voiceControlEnabled  = intent.getBooleanExtra("VCC_ENABLED", false)

        //----ADD a CATCH button is clicked -----------
        btnTournamentCatch.setOnClickListener { showWeightPopup() }
        btnMenu.setOnClickListener { startActivity(Intent(this, SetUpActivity::class.java)) }
        btnMainPg.setOnClickListener { startActivity(Intent(this,MainActivity::class.java)) }

        updateVccLabel()         // just shows user if VCC is Enabled or not...
        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)

        updateTournamentList()      //todo ask if we need to put this in the onResume to update the list when we wake up the app???

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
        unregisterReceiver(voiceCatchReceiver)
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
        val oz   = intent.getIntExtra(EXTRA_WEIGHT_OZ, 0)
        val sp   = intent.getStringExtra(EXTRA_SPECIES).orEmpty()
        val clip = intent.getStringExtra(EXTRA_CLIP_COLOR).orEmpty()
        saveTournamentCatch(oz, sp, clip)
    }

    /** ~~~~~~~~~~~~~ Opens the weight entry popup ~~~~~~~~~~~~~~~ */

    private fun showWeightPopup() {

        val intent = Intent(this, PopupWeightEntryTourLbs::class.java).apply {

            putExtra(EXTRA_IS_TOURNAMENT, true)

            putExtra(EXTRA_TOURNAMENT_SPECIES,tournamentSpecies)

            // Send as an ArrayList so you can retrieve with getStringArrayListExtra
            val colorArray = availableClipColors.map { it.name }.toTypedArray()
            putExtra(EXTRA_AVAILABLE_CLIP_COLORS, colorArray)
        }
        entryLauncher.launch(intent)
    }


    // ^^^^^^^^^^^^^ SAVE TOURNAMENT CATCH ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    private fun saveTournamentCatch(weightTotalOz: Int, species: String, clipColor: String) {

        val cleanClipColor = clipColor.uppercase() // This came from the popup

        val speciesInitial =
            SharedPreferencesManager.getSpeciesInitial(this, species)


        Log.d("DB_DEBUG", "✅ Assigned Clip Color: $cleanClipColor")

        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = species,
            totalWeightOz = weightTotalOz,
            totalWeightHundredthPounds = null,
            totalLengthQuarters = null,
            totalWeightHundredthKg = null,
            totalLengthTenths = null,
            catchType = "tournament_lbs_ozs",
            markerType = speciesInitial,
            clipColor = cleanClipColor
        )

        val result = dbHelper.insertCatch(catch)
        Log.d("DB_DEBUG", "✅ Catch Insert Result: $result, Stored Clip Color: ${catch.clipColor}")
        // ✅ Save the most recent catch for motivational messaging
        Toast.makeText(this, "$species Catch Saved!", Toast.LENGTH_SHORT).show()
        if (result) {
            lastTournamentCatch = catch
        }
        updateTournamentList()
    }// -------------- END Save Tournament Catch  -----------------------------


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
        // todo Set up Better Scenarios
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

        val allCatches = dbHelper.getCatchesForToday(catchType = "tournament_lbs_ozs", formattedDate)
        val sortedCatches = allCatches.sortedByDescending { it.totalWeightOz ?: 0 }

        // These are the ones used for scoring and totals
        val tournamentCatches = if (isCullingEnabled) {
            sortedCatches.take(tournamentCatchLimit)
        } else {
            sortedCatches
        }

        availableClipColors = calculateAvailableClipColors(
            dbHelper,
            catchType = "tournament_lbs_ozs",
            date = formattedDate,
            tournamentCatchLimit = tournamentCatchLimit
        )
        Log.d("CLIP_COLOR", "🎨 Available Colors LBS: $availableClipColors")
        clearTournamentTextViews()

        runOnUiThread {
            val loopLimit = minOf(sortedCatches.size, tournamentCatchLimit + 1, 6)

            for (i in 0 until loopLimit) {
                if (i >= realWeights.size) break

                val item = sortedCatches[i]
                val oz = item.totalWeightOz ?: 0
                val lbs = oz / 16
                val remOz = oz % 16

                // fill in the TextViews
                realWeights[i].text = lbs.toString()
                decWeights[i].text = remOz.toString()

                val clipColor = try {
                    ClipColor.valueOf(item.clipColor!!.uppercase())
                } catch (_: Exception) {
                    ClipColor.RED
                }

                val baseColor = ContextCompat.getColor(this, clipColor.resId)
                val drawable  = createLayeredDrawable(baseColor)
                realWeights[i].background = drawable
                decWeights[i].background  = drawable

                //------------- If Clip is Blue then Text is White
                val textColor = if (clipColor == ClipColor.BLUE)
                    resources.getColor(R.color.clip_white, theme)
                else
                    resources.getColor(R.color.black, theme)

                realWeights[i].setTextColor(textColor)
                decWeights[i].setTextColor(textColor)

                // Text overlays
                colorLetters[i].text = when (clipColor.name) {
                    "BLUE"   -> "B"
                    "RED"    -> "R"
                    "GREEN"  -> "G"
                    "YELLOW" -> "Y"
                    "ORANGE" -> "O"
                    "WHITE"  -> "W"
                    else     -> "?"
                }

                typeLetters[i].text =
                    SharedPreferencesManager.getSpeciesInitial(this, item.species)


                // **long-press to 📝 EDIT or DELETE 🚫 this exact item**
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
    }//$$$$$$$$$$$$$$$ END Update Tournament List  $$$$$$$$$$$$$$$$$$$$$$$$$$

    //########### Clear Tournament Text Views  ########################

    private fun clearTournamentTextViews() {

        val realWeights = listOf(
            firstRealWeight, secondRealWeight, thirdRealWeight,
            fourthRealWeight, fifthRealWeight, sixthRealWeight
        )

        val decWeights = listOf(
            firstDecWeight, secondDecWeight, thirdDecWeight,
            fourthDecWeight, fifthDecWeight, sixthDecWeight
        )

        val typeLetters = listOf(
            txtTypeLetter1, txtTypeLetter2, txtTypeLetter3,
            txtTypeLetter4, txtTypeLetter5, txtTypeLetter6
        )

        val colorLetters = listOf(
            txtColorLetter1, txtColorLetter2, txtColorLetter3,
            txtColorLetter4, txtColorLetter5, txtColorLetter6
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

        totalRealWeight.text = "0"
        totalDecWeight.text = "0"
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
        val sorted = allCatches.sortedByDescending { it.totalWeightOz ?: 0 }
        val topCatches = sorted.take(tournamentCatchLimit) // ✅ Always limit to top N
        val usedColors = topCatches.mapNotNull { it.clipColor }
            .mapNotNull {
                try { ClipColor.valueOf(it.uppercase()) } catch (_: Exception) { null }
            }
            .toSet()

        return ClipColor.entries.filter { it !in usedColors }
    }


    // ~~~~~~~~~~~~~ ADJUST TEXT VIEW VIABILITY for Culling Values ~~~~~~~~~~~~~
    private fun adjustTextViewVisibility() {
        when (tournamentCatchLimit) {
            4 -> {
                // Row 5 = culling candidate (dimmed)
                fifthRealWeight.alpha = 0.3f
                fifthDecWeight.alpha = 0.3f
                fifthRealWeight.isEnabled = false
                fifthDecWeight.isEnabled = false
                txtTypeLetter5.alpha = 0.3f
                txtColorLetter5.alpha = 0.3f

                // Row 6 = not visible at all
                sixthRealWeight.visibility = View.INVISIBLE
                sixthDecWeight.visibility = View.INVISIBLE
                txtTypeLetter6.visibility = View.INVISIBLE
                txtColorLetter6.visibility = View.INVISIBLE
            }
            5 -> {
                // Row 6 = culling candidate (dimmed)
                sixthRealWeight.alpha = 0.3f
                sixthDecWeight.alpha = 0.3f
                sixthRealWeight.isEnabled = false
                sixthDecWeight.isEnabled = false
                txtTypeLetter6.alpha = 0.3f
                txtColorLetter6.alpha = 0.3f
            }
            else -> {
                // Limit = 6, all rows fully visible
                fifthRealWeight.alpha = 1.0f
                fifthDecWeight.alpha = 1.0f
                fifthRealWeight.isEnabled = true
                fifthDecWeight.isEnabled = true
                txtTypeLetter5.alpha = 1.0f
                txtColorLetter5.alpha = 1.0f
                txtTypeLetter5.visibility = View.VISIBLE
                txtColorLetter5.visibility = View.VISIBLE

                sixthRealWeight.visibility = View.VISIBLE
                sixthDecWeight.visibility = View.VISIBLE
                sixthRealWeight.alpha = 1.0f
                sixthDecWeight.alpha = 1.0f
                sixthRealWeight.isEnabled = true
                sixthDecWeight.isEnabled = true
                txtTypeLetter6.alpha = 1.0f
                txtColorLetter6.alpha = 1.0f
                txtTypeLetter6.visibility = View.VISIBLE
                txtColorLetter6.visibility = View.VISIBLE
            }
        }
    } //---------------- END Adjust the Text View Visibility ----------------


    //******************* FOR 🥸 User 📝 EDIT Logged Weights ********************************

    private fun showTournamentEditDialog(c: CatchItem) {
        // 1) inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_tournament_catch_lbs, null)

        // 2) pull out your in-layout buttons & fields
        val spnClipColor = dialogView.findViewById<Spinner>(R.id.spnClipColor)
        val edtLbs       = dialogView.findViewById<EditText>(R.id.edtTourWeightLbs)
        val edtOzs       = dialogView.findViewById<EditText>(R.id.edtTourWeightOzs)
        val btnSave      = dialogView.findViewById<Button>(R.id.btnSaveEdtTourLbs)
        val btnCancel    = dialogView.findViewById<Button>(R.id.btnCancelEdtTourLbs)
        val btnDelete    = dialogView.findViewById<Button>(R.id.btnDeleteEdtTourLbs)

        // 3) pre-fill the fields
        edtLbs.filters = arrayOf(MinMaxInputFilter(0, 99)) // Lbs: 0-99
        edtOzs.filters = arrayOf(MinMaxInputFilter(0, 15)) // Ozs 0 - 15

        val weightOz = c.totalWeightOz ?: 0
        edtLbs.setText((weightOz / 16).toString())
        edtOzs.setText((weightOz % 16).toString())

        clearOnceOnFocus(edtLbs)
        clearOnceOnFocus(edtOzs)

        // 4) color box
            // Find available clip colors
        val availableColors = calculateAvailableClipColorsForEdit(
            dbHelper = dbHelper,
            catchType = "tournament_lbs_ozs",
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


        // 5) build & show **one** dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit or Delete Catch")
            .setView(dialogView)
            .create()
        dialog.show()

        // 6) wire your in-layout Save
        btnSave.setOnClickListener {
            val selectedClipColor = spnClipColor.selectedItem.toString().uppercase()
            val newLbs     = edtLbs.text.toString().toIntOrNull() ?: 0
            val newOzs     = edtOzs.text.toString().toIntOrNull() ?: 0
            val newWeightOz = (newLbs * 16) + newOzs

            if (newWeightOz == 0) {
                edtLbs.setText("0")
                edtOzs.setText("0")
                edtLbs.requestFocus()
                edtLbs.setSelection(edtLbs.text.length)

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(edtLbs, InputMethodManager.SHOW_IMPLICIT)

                positionedToast("🚫 Weight cannot be 0 lbs 0 oz!")
                return@setOnClickListener
            }

            dbHelper.updateCatch(
                catchId            = c.id,
                newWeightOz        = newWeightOz,
                newWeightKg        = null,
                newLengthQuarters  = null,
                newLengthCm        = null,
                species            = c.species,
                clipColor          = selectedClipColor
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
            txtVCCTourLbs.text = getString(R.string.vcc_on)
            txtVCCTourLbs.setBackgroundColor(ContextCompat.getColor(this, R.color.clip_yellow))
            txtVCCTourLbs.setTextColor(ContextCompat.getColor(this, R.color.clip_orange))// Orange
        } else {
            txtVCCTourLbs.text = getString(R.string.manual_mode)
            txtVCCTourLbs.setTextColor(ContextCompat.getColor(this, R.color.clip_blue))// blue
            txtVCCTourLbs.background = null
        }
    }

    // ------------ VCC Enabled Set Up Voice Control to Keep the BaseCatchEntryActivity connected ----------------
    override fun onSpeechResult(transcript: String) {
        // No-op: VCC now handled entirely in VoiceControlService
    }


}//################## END  ################################