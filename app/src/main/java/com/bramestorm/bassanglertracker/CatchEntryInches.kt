package com.bramestorm.bassanglertracker

import android.app.Activity
import android.app.AlertDialog
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
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper.normalizeSpeciesName
import com.bramestorm.bassanglertracker.utils.getMotivationalMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatchEntryInches : BaseCatchEntryActivity() {


    private lateinit var btnSetUp3Inch: Button
    private lateinit var btnOpenWeightPopupInch: Button
    private lateinit var simpleInchListView: ListView
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper
    private val lengthEntryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                totalLengthQuarters = data.getIntExtra("lengthTotalInches", 0)
                selectedSpecies      = data.getStringExtra("selectedSpecies") ?: selectedSpecies

                if (totalLengthQuarters > 0) {
                    selectedSpecies = normalizeSpeciesName(selectedSpecies)
                    saveCatch()
                    Log.d("DB_DEBUG", "✅ saveCatch() called via launcher")
                } else {
                    Log.e("DB_DEBUG", "⚠️ Invalid length—nothing saved")
                }
            }
        }
    }

    private var selectedSpecies: String = ""
    private var totalLengthQuarters: Int = 0
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
            Toast.makeText(this@CatchEntryInches, "Speech error $error", Toast.LENGTH_SHORT).show()
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
        setContentView(R.layout.activity_catch_entry_inches)

        //******  Initialize speech recognizer ***********************
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(recognitionListener)
        }
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        dbHelper = CatchDatabaseHelper(this)

        btnSetUp3Inch = findViewById(R.id.btnSetUp3Inch)
        btnOpenWeightPopupInch = findViewById(R.id.btnOpenWeightPopupInch)
        simpleInchListView = findViewById(R.id.simpleInchListView)

        updateListViewInch() // Load today's catches into ListView

        btnOpenWeightPopupInch.setOnClickListener {
            openWeightPopupInch()
        }

        btnSetUp3Inch.setOnClickListener {
            val intent2 = Intent(this, SetUpActivity::class.java)
            startActivity(intent2)
        }

        simpleInchListView.setOnItemLongClickListener { parent, view, position, id ->
            if (catchList.isEmpty()) {
                Toast.makeText(this, "😢 No catches available", Toast.LENGTH_SHORT).show()
                return@setOnItemLongClickListener true
            }

            if (position >= catchList.size) {
                return@setOnItemLongClickListener true
            }

            val selectedCatch = catchList[position]
            showEditDeleteDialog(selectedCatch)
            true
        }


    }//`````````` END ON-CREATE `````````````

    override fun onDestroy() {
        recognizer.destroy()
        super.onDestroy()
    }

    private fun openWeightPopupInch() {
        val intent = Intent(this, PopupLengthEntryInches::class.java)
        lengthEntryLauncher.launch(intent)
    }


    // %%%%%%%%%%% SAVE CATCH  %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    private fun saveCatch() {

        val newCatch = CatchItem(
            id = 0,
            latitude = null,
            longitude = null,
            dateTime = getCurrentDateTime(),
            species = selectedSpecies,
            totalWeightOz = null,
            totalLengthQuarters = totalLengthQuarters,
            totalLengthTenths = null,
            totalWeightHundredthKg = null,
            catchType = "inches",
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
            totalLengthQuarters = 0 // ✅ Move this after successful save
        }
        // ✅ MOTIVATIONAL TOAST FOR FUN DAY - Inches
        if (catchList.size >= 2) {
            val lastCatch = catchList.firstOrNull() // Most recent (sorted by dateTime)
            lastCatch?.let {
                val message = getMotivationalMessage(this, it.id, catchList.size, "inches")
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        updateListViewInch()  // ✅ Updates the UI
    }


    //:::::::::::::::: UPDATE LIST VIEW in time_Date Order ::::::::::::::::::::::::::::::::

    private fun updateListViewInch() {

        val todaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val todaysCatches = dbHelper.getCatchesForToday("inches", todaysDate)
            .sortedByDescending { it.dateTime }  // Sort by dateTime (newest first)

        // ✅ CatchList is updated BEFORE updating the ListView
        catchList.clear()
        catchList.addAll(todaysCatches)

        val catchDisplayList = todaysCatches.map {
            val totalLengthQuarters = it.totalLengthQuarters ?: 0
            val inches = (totalLengthQuarters / 4)
            val quarters = (totalLengthQuarters  % 4)
            // Format the time from dateTime
            val timeFormatted = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val parsedDate = inputFormat.parse(it.dateTime ?: "")
                outputFormat.format(parsedDate ?: Date())
            } catch (e: Exception) {
                "N/A"
            }
            when (quarters) {
                0 -> "${it.species} - $inches Inches @ $timeFormatted"
                2 -> "${it.species} - $inches 1/2 Inches @ $timeFormatted"
                else -> "${it.species} - $inches ${quarters}/4 Inches @ $timeFormatted"
            }
        }

        runOnUiThread {
            val adapter = CatchItemAdapter(this, catchList)
            simpleInchListView.adapter = adapter
        }
    }


    //*************** DELETE ENTRY from list View of Catches ********************

    private fun showEditDeleteDialog(catchItem: CatchItem) {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Edit or Delete")
            .setMessage("Do you want to edit or delete this entry?")
            .setPositiveButton("Edit") { _, _ ->
                showEditDialog(catchItem) // Call the new edit function
            }
            .setNegativeButton("Delete") { _, _ ->
                val dbHelper = CatchDatabaseHelper(this)
                dbHelper.deleteCatch(catchItem.id)
                updateListViewInch()
                Toast.makeText(this, "Catch deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    //*************** EDIT list View of Catches ********************

    private fun showEditDialog(catchItem: CatchItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_catch_inches, null)
        val edtLengthInches = dialogView.findViewById<EditText>(R.id.edtLengthInches)
        val edtLengthEntryQuarters = dialogView.findViewById<EditText>(R.id.edtLengthQuarters)
        val spinnerSpeciesEditInches = dialogView.findViewById<Spinner>(R.id.spinnerSpeciesEditInches)

        // --- 1. Load user-selected species list ---
        val speciesList = SharedPreferencesManager.getSelectedSpeciesList(this)
        val normalizedSpeciesList = speciesList.map { normalizeSpeciesName(it) }
        val currentSpeciesNormalized = normalizeSpeciesName(catchItem.species)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpeciesEditInches.adapter = adapter

        // --- 2. Set current values ---
        val newLengthQuarters = catchItem.totalLengthQuarters ?: 0
        edtLengthInches.setText((newLengthQuarters / 4).toString())
        edtLengthEntryQuarters.setText((newLengthQuarters % 4).toString())

        // --- 3. Set spinner selection based on normalized match ---
        val speciesIndex = normalizedSpeciesList.indexOf(currentSpeciesNormalized)
        spinnerSpeciesEditInches.setSelection(if (speciesIndex != -1) speciesIndex else 0)

        // --- 4. Show dialog and handle save ---
        AlertDialog.Builder(this)
            .setTitle("Edit Catch")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newInches = edtLengthInches.text.toString().toIntOrNull() ?: 0
                val new4ths = edtLengthEntryQuarters.text.toString().toIntOrNull() ?: 0
                val totalLengthQuarters = ((newInches * 4) + new4ths)
                val species = spinnerSpeciesEditInches.selectedItem.toString()

                val dbHelper = CatchDatabaseHelper(this)

                dbHelper.updateCatch(
                    catchId = catchItem.id,
                    newWeightOz = null,
                    newWeightKg = null,
                    newLengthQuarters = totalLengthQuarters,
                    newLengthCm = null,
                    species = species
                )

                Log.d("DB_DEBUG", "✅ Updating ID=${catchItem.id}, New Length=$totalLengthQuarters, New Species=$species")

                updateListViewInch()
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
            if (p.totalLengthQuarters> 0) {
                // stash into your existing fields…
                totalLengthQuarters = p.totalLengthQuarters
                selectedSpecies     = normalizeSpeciesName(p.species)
                // then call your no-arg saveCatch()
                saveCatch()
            }
        } ?: Toast.makeText(this, " 👎Could not parse: $transcript", Toast.LENGTH_LONG).show()
    }


    // --- Voice Control: override to start listening on wake event ---
    override fun onVoiceWake() {
        recognizer.startListening(recognizerIntent)
    }

}//+++++++++++++ END  od CATCH ENTRY LBS OZS ++++++++++++++++++++++++++++++++++++++++