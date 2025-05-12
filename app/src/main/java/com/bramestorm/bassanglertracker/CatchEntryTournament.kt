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
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bramestorm.bassanglertracker.PopupWeightEntryLbs.MinMaxInputFilter
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
    private val requestAlarmSET = 1008

    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null

    // For VCC to Wake UP
    private var launchFromWake = false

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
    lateinit var userVoiceMap: MutableMap<String, String>       //todo Correct with Mispronunciations ReWrite the Word/Phrase DataBase
    private var awaitingResult = false


    // Tournament Configuration
    private var tournamentCatchLimit: Int = 4
    private var measurementSystem: String = "weight"
    private var isCullingEnabled: Boolean = false
    private var typeOfMarkers: String = "Color"
    private var tournamentSpecies: String = "Unknown"
    private var lastTournamentCatch: CatchItem? = null

    companion object {
        const val EXTRA_WEIGHT_OZ     = "weightTotalOz"
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
            val sp   = intent.getStringExtra(PopupVccTournLbs.EXTRA_SPECIES).orEmpty()
            val clip = intent.getStringExtra(PopupVccTournLbs.EXTRA_CLIP_COLOR).orEmpty()
            Log.d("CET-DEBUG", "Broadcast received: $oz, $sp, $clip")
            saveTournamentCatch(oz, sp, clip)
        }
    }

    // ````````````` Retrieves data from the POPUPS ````````````````````````
    private val entryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("CET-DEBUG", "⌚ entryLauncher fired: resultCode=${result.resultCode}, data=${result.data}")

        awaitingResult = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val oz = result.data!!.getIntExtra(EXTRA_WEIGHT_OZ, 0)
            val sp = result.data!!.getStringExtra(EXTRA_SPECIES).orEmpty()
            val clip = result.data!!.getStringExtra(EXTRA_CLIP_COLOR).orEmpty()
            Log.d("TournRecv", "🏁 Got result → $oz oz, $sp, $clip")
            saveTournamentCatch(oz, sp, clip)
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
                catchType            = "LbsOzs",
                date                 = getCurrentDate(),
                tournamentCatchLimit = tournamentCatchLimit,
                isCullingEnabled     = isCullingEnabled
            ).map { it.name }  // convert from the enum to String list

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

     LocalBroadcastManager.getInstance(this)
         .registerReceiver(catchReceiver, IntentFilter("com.bramestorm.CATCH_TOURNAMENT"))

     // Set Up the Voice Helper interaction with VoiceInteractionHelper ------
     voiceHelper = VoiceInteractionHelper(
         activity = this, //
         measurementUnit = VoiceInteractionHelper.MeasurementUnit.LBS_OZ,
         isTournament = true,
         onCommandAction = { transcript -> onSpeechResult(transcript) }
     )

     voiceControlEnabled = intent.getBooleanExtra("VCC_ENABLED", false)

     Log.d("VCC_FLOW", "Voice control enabled: $voiceControlEnabled")

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


        // GET VAlUES from SetUp page -----------
        tournamentCatchLimit = intent.getIntExtra("NUMBER_OF_CATCHES", 4)       // Culling Values
        typeOfMarkers = intent.getStringExtra("Color_Numbers") ?: "Color"                 // Typical Markers colors
        tournamentSpecies = intent.getStringExtra("TOURNAMENT_SPECIES") ?: "Unknown"      // Tournament Species
        measurementSystem = intent.getStringExtra("unitType") ?: "weight"                 // Type of Measuring
        isCullingEnabled = intent.getBooleanExtra("CULLING_ENABLED", false)     // Is Culling / Tournament mode
        voiceControlEnabled  = intent.getBooleanExtra("VCC_ENABLED", false)     // Is the app in VCC mode?

        //----ADD a CATCH button is clicked -----------
     btnTournamentCatch.setOnClickListener {
             showWeightPopup()
     }

     btnMenu.setOnClickListener { startActivity(Intent(this, SetUpActivity::class.java)) }
     btnMainPg.setOnClickListener { startActivity(Intent(this,MainActivity::class.java)) }
     btnAlarm.setOnClickListener { startActivityForResult(Intent(this, PopUpAlarm::class.java), requestAlarmSET) }

        updateVccLabel()
        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)

        updateTournamentList()
     handler.postDelayed(checkAlarmRunnable, 60000) // check every minute (60 sec)

     Handler(Looper.getMainLooper()).post {
         if (launchFromWake) {
             Log.d("VCC_DEBUG", "🔊 Wake trigger → launching popup with VCC=$voiceControlEnabled")
             showWeightPopup()
             launchFromWake = false
         }
     }

 } // ~~~~~~~~~~~~~~~~~~~~~ END ON CREATE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
        val oz   = intent.getIntExtra(EXTRA_WEIGHT_OZ, 0)
        val sp   = intent.getStringExtra(EXTRA_SPECIES).orEmpty()
        val clip = intent.getStringExtra(EXTRA_CLIP_COLOR).orEmpty()
        saveTournamentCatch(oz, sp, clip)
    }  /** ~~~~~~~~~~~~~ Opens the weight entry popup ~~~~~~~~~~~~~~~ */
    /** Launches the appropriate popup (VCC vs manual) */
    private fun showWeightPopup() {
        awaitingResult = true


        // build the Intent with your fresh list
        val intent = Intent(
            this,
            if (voiceControlEnabled) PopupVccTournLbs::class.java
            else PopupWeightEntryTourLbs::class.java
        ).apply {
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
        }

        Log.d("CatchEntryTournament","▶️ Launching ${if (voiceControlEnabled) "VCC" else "Manual"} popup with colors=$availableClipColors")

        entryLauncher.launch(intent)
    }



    // ^^^^^^^^^^^^^ SAVE TOURNAMENT CATCH ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    private fun saveTournamentCatch(weightTotalOz: Int, species: String, clipColor: String) {
        val availableColors = calculateAvailableClipColors(
            dbHelper,
            catchType = "LbsOzs",
            date = getCurrentDate(),
            tournamentCatchLimit = tournamentCatchLimit,
            isCullingEnabled = isCullingEnabled
        )
        val cleanClipColor = clipColor.uppercase() // This came from the popup

        val speciesInitial = if (species == "Large Mouth") "L" else "S"

        Log.d("DB_DEBUG", "✅ Assigned Clip Color: $cleanClipColor")

        val catch = CatchItem(
            id = 0,
            dateTime = getCurrentDateTime(),
            species = species,
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
                typeLetters[i].text  = getSpeciesCode(item.species ?: "")

                // **long-press to EDIT or DELETE this exact item**
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


    // ~~~~~~~~~~~~~ ADJUST TEXT VIEW VIABILITY for Culling Values ~~~~~~~~~~~~~
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
    } //---------------- END Adjust the Text View Visibility ----------------

    //!!!!!!!!!!!!!!!! Get SPECIES Letters for Side Text !!!!!!!!!!!!!!!!!
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
    } //------------ END Get Species Codes ----------------


        //******************* User EDIT Logged Weights ********************************
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
            edtLbs.filters = arrayOf(MinMaxInputFilter(0, 99)) // Lbs: 0-99
            edtOzs.filters = arrayOf(MinMaxInputFilter(0, 15)) // Ozs 0 - 15


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
                    species            = c.species
                )
                updateTournamentList()
                dialog.dismiss()
            }

            // 7) …and Cancel
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }
        }//========== END of User Editing Logged Weights ==============================

    // ^^^^^^^^^^^^^^^  CHECK ALARM ++++++++++++++++++++++++

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

    // ^^^^^^^^^^^^^^^ Start Alarm ^^^^^^^^^^^^^^^^^^^^^
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

    @Deprecated("This method has been deprecated in favor of using the Activity Result API" +
            "which brings increased type safety via an {@link ActivityResultContract} and the prebuilt " +
            "contracts for common intents available in {@link androidx.activity.result.contract.ActivityResultContracts}, " +
            "provides hooks for testing, and allow receiving results in separate, testable classes independent from the " +
            "activity. Use {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}with the appropriate " +
            "{@link ActivityResultContract} and handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestAlarmSET && resultCode == Activity.RESULT_OK) {
            alarmHour = data?.getIntExtra("ALARM_HOUR", -1) ?: -1
            alarmMinute = data?.getIntExtra("ALARM_MINUTE", -1) ?: -1
            alarmTriggered = false // ✅ reset so the alarm can trigger again

            if (alarmHour != -1 && alarmMinute != -1) {
                // Format time string for display
                val amPm = if (alarmHour >= 12) "PM" else "AM"
                val displayHour = if (alarmHour % 12 == 0) 12 else alarmHour % 12
                val formattedMinute = String.format(Locale.getDefault(), "%02d", alarmMinute)
                val timeString = "$displayHour:$formattedMinute $amPm"

                // Update button and show toast
                btnAlarm.text = getString(R.string.alarm_set_to, timeString)
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
            }
        }
    }//============ END of ALARM Components ================

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

    // ------------ VCC Enabled Set Up Voice Control ----------------
    override fun onSpeechResult(transcript: String) {       //todo not sure what to do with this, is it for voice wakeup?? can we use for other voice commands???
        Log.d("VCC", "Speech Result Received: $transcript")

        // 👇 Replace with your actual phrase recognition or command parsing
        if (transcript.contains("add fish", ignoreCase = true)) {
            showWeightPopup() // 🔥 Launches PopupWeightEntryTourLbs.kt
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