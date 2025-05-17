package com.bramestorm.bassanglertracker

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bramestorm.bassanglertracker.base.BaseCatchEntryActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.training.VoiceCatchParse
import com.bramestorm.bassanglertracker.training.VoiceInteractionHelper
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper.normalizeSpeciesName
import com.bramestorm.bassanglertracker.utils.getMotivationalMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatchEntryLbsOzs : BaseCatchEntryActivity() {

    override val catchReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            // no-op for now – we don’t use tournament broadcasts here
        }
    }

    private lateinit var btnSetUp3: Button
    private lateinit var btnOpenWeightPopup: Button
    private lateinit var simpleLbsListView: ListView
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper

    // Voice Helper
    private var voiceControlEnabled = false
    private lateinit var voiceHelper: VoiceInteractionHelper
    lateinit var userVoiceMap: MutableMap<String, String>       //todo Correct with Mispronunciations ReWrite the Word/Phrase DataBase
    private var awaitingResult = false


    companion object {
        const val EXTRA_WEIGHT_OZ     = "weightTotalOz"
        const val EXTRA_SPECIES       = "selectedSpecies"
    }

    private val weightEntryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                totalWeightOz = data.getIntExtra(EXTRA_WEIGHT_OZ , 0)
                selectedSpecies = data.getStringExtra(EXTRA_SPECIES) ?: selectedSpecies

                if (totalWeightOz > 0) {
                    selectedSpecies = normalizeSpeciesName(selectedSpecies)
                    saveCatch()
                    Log.d("DB_DEBUG", "✅ saveCatch() called via launcher")
                } else {
                    Log.e("DB_DEBUG", "⚠️ Invalid weight—nothing saved")
                }
            }
        }
    }

    private var selectedSpecies: String = ""
    private var totalWeightOz: Int = 0
    private lateinit var dialogInstance: AlertDialog
    override val dialog: Any
        get() = dialogInstance


    // --- voice-to-text callback handler ---
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            Toast.makeText(this@CatchEntryLbsOzs, "Speech error $error", Toast.LENGTH_SHORT).show()
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

    //========= onCreate =============================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_lbs_ozs)

        //-- Set Up the Voice Helper interaction with VoiceInteractionHelper ------
        voiceHelper = VoiceInteractionHelper(
            activity = this, //
            measurementUnit = VoiceInteractionHelper.MeasurementUnit.LBS_OZ,
            isTournament = false,
            onCommandAction = { transcript -> onSpeechResult(transcript) }
        )
        voiceControlEnabled = intent.getBooleanExtra("VCC_ENABLED", false)
        dbHelper = CatchDatabaseHelper(this)

        //******  Initialize speech recognizer ***********************
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(recognitionListener)
        }
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        // ******** Set Up Values ****************
        btnSetUp3 = findViewById(R.id.btnSetUp3)
        btnOpenWeightPopup = findViewById(R.id.btnOpenWeightPopup)
        simpleLbsListView = findViewById(R.id.simpleLbsListView)

        updateListViewLb() // Load today's catches into ListView

        // $$$$$$$$ ADD a Catch  $$$$$$$$$$$$$$$$$$$$$$$
        btnOpenWeightPopup.setOnClickListener {
            openWeightPopup()
        }
        // GOTO SET-UP PAGE
        btnSetUp3.setOnClickListener {
            val intent2 = Intent(this, SetUpActivity::class.java)
            startActivity(intent2)
        }

        //+++++++++ EDIT OR DELETE ENTRIES ++++++++++++++
        simpleLbsListView.setOnItemLongClickListener { _, _, position, _ ->
            if (catchList.isEmpty()) {
                Toast.makeText(this, "No catches available", Toast.LENGTH_SHORT).show()
                return@setOnItemLongClickListener true
            }
            if (position >= catchList.size) {
                Log.e("DB_DEBUG", "⚠️ Invalid position: $position, Catch List Size: ${catchList.size}")
                return@setOnItemLongClickListener true
            }
            val selectedCatch = catchList[position]
            showEditDeleteDialog(selectedCatch)
            true
        }

    }//`````````` END ON-CREATE `````````````


    override fun onDestroy() {
        super.onDestroy()
        voiceHelper.shutdown()
        recognizer.destroy()
    }

    private fun openWeightPopup() {
        val intent = if (voiceControlEnabled) {
            Intent(this, PopupVccWeightEntryLbs::class.java)

        } else {
            Intent(this, PopupWeightEntryLbs::class.java)
        }
        weightEntryLauncher.launch(intent)
    }


    // %%%%%%%%%%% SAVE CATCH  %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    private fun saveCatch() {
        Log.d("DB_DEBUG", "🔍 We are in saveCatch().")
        val newCatch = CatchItem(
            id = 0,
            latitude = null,
            longitude = null,
            dateTime = getCurrentDateTime(),
            species = selectedSpecies,
            totalWeightOz = totalWeightOz,
            totalLengthQuarters = null,
            totalLengthTenths = null,
            totalWeightHundredthKg = null,
            catchType = "lbsOzs",
            markerType = selectedSpecies,
            clipColor = null
        )

        val success = dbHelper.insertCatch(newCatch)

        if (success) {
            Toast.makeText(this, "$selectedSpecies Catch Saved!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "⚠️ Failed to save catch!", Toast.LENGTH_SHORT).show()
        }

        if (success) {
            totalWeightOz = 0 // ✅ Move this after successful save
        }

        updateListViewLb()  // ✅ Now only updates the UI, no extra insert
    }


    //:::::::::::::::: UPDATE LIST VIEW in time_Date Order ::::::::::::::::::::::::::::::::

    private fun updateListViewLb() {
        val todaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todaysCatches = dbHelper.getCatchesForToday("lbsOzs", todaysDate)
            .sortedByDescending { it.dateTime }

        Log.d("DB_DEBUG", "🔍 Catches retrieved from DB: ${todaysCatches.size}")

        // ✅ Make sure catchList is updated BEFORE updating the ListView
        catchList.clear()
        catchList.addAll(todaysCatches)

        // ✅ MOTIVATIONAL TOAST FOR FUN DAY (when 2+ catches exist)
        if (catchList.size >= 2) {
            val lastCatch = catchList.firstOrNull() // Most recent (sorted by dateTime)
            lastCatch?.let {
                val message = getMotivationalMessage(this, it.id, catchList.size, "lbs")
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        runOnUiThread {
            val adapter = CatchItemAdapter(this, catchList)
            simpleLbsListView.adapter = adapter
        }
    }




    //*************** DELETE ENTRY from list View of Catches ********************

    private fun showEditDeleteDialog(catchItem: CatchItem) {
        AlertDialog.Builder(this)
            .setTitle("Edit or Delete")
            .setMessage("Do you want to edit or delete this entry?")
            .setPositiveButton("Edit") { _, _ ->
                showEditDialog(catchItem) // Call the new edit function
            }
            .setNegativeButton("Delete") { _, _ ->
                val dbHelper = CatchDatabaseHelper(this)
                dbHelper.deleteCatch(catchItem.id)
                updateListViewLb()
                Toast.makeText(this, "Catch deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    //*************** EDIT list View of Catches ********************

    private fun showEditDialog(catchItem: CatchItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_catch_lbs, null)
        val edtWeightLbs = dialogView.findViewById<EditText>(R.id.edtWeightLbs)
        val edtWeightOzs = dialogView.findViewById<EditText>(R.id.edtWeightOzs)
        val spinnerSpeciesLbs = dialogView.findViewById<Spinner>(R.id.spinnerSpeciesEditLbs)

        // --- 1. Load user-selected species list ---
        val speciesList = SharedPreferencesManager.getSelectedSpeciesList(this)
        val normalizedSpeciesList = speciesList.map { normalizeSpeciesName(it) }
        val currentSpeciesNormalized = normalizeSpeciesName(catchItem.species)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpeciesLbs.adapter = adapter

        // --- 2. Set current values ---
        val totalWeightOz = catchItem.totalWeightOz ?: 0 // Default to 0 if null
        edtWeightLbs.setText((totalWeightOz / 16).toString())
        edtWeightOzs.setText((totalWeightOz % 16).toString())

        // --- 3. Set spinner selection based on normalized species match ---
        val speciesIndex = normalizedSpeciesList.indexOf(currentSpeciesNormalized)
        spinnerSpeciesLbs.setSelection(if (speciesIndex != -1) speciesIndex else 0)

        // --- 4. Show dialog and save on confirm ---
        AlertDialog.Builder(this)
            .setTitle("Edit Catch")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newLbs = edtWeightLbs.text.toString().toIntOrNull() ?: 0
                val newOzs = edtWeightOzs.text.toString().toIntOrNull() ?: 0
                val totalWeightOz = (newLbs * 16) + newOzs
                val species = spinnerSpeciesLbs.selectedItem.toString()

                val dbHelper = CatchDatabaseHelper(this)

                dbHelper.updateCatch(
                    catchId = catchItem.id,
                    newWeightOz = totalWeightOz,
                    newWeightKg = null,      // Lbs/Oz mode only
                    newLengthQuarters = null,   // No length change
                    newLengthCm = null,
                    species = species
                )

                Log.d("DB_DEBUG", "✅ Updating ID=${catchItem.id}, New Weight=$totalWeightOz, New Species=$species")

                updateListViewLb()
                Toast.makeText(this, "Catch updated!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    // ############## GET DATE and TIME  ############################

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    // --- Voice Control: override to receive speech transcripts ---

    override fun onSpeechResult(transcript: String) {
        VoiceCatchParse().parseVoiceCommand(transcript)?.let { p ->
            if (p.totalLengthTenths > 0) {
                // stash into your existing fields…
                totalWeightOz = p.totalWeightOzs
                selectedSpecies     = normalizeSpeciesName(p.species)
                // then call your no-arg saveCatch()
                saveCatch()
            }
        } ?: Toast.makeText(this, "Could not parse: $transcript", Toast.LENGTH_LONG).show()
    }


    // --- Voice Control: override to start listening on wake event ---
    override fun onVoiceWake() {
        recognizer.startListening(recognizerIntent)
    }

    override fun onManualWake() {
        // (this is the tap handler)
        openWeightPopup()
    }


}//+++++++++++++ END of CATCH ENTRY LBS OZS ++++++++++++++++++++++++++++++++++++++++