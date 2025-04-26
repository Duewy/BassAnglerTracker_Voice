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
import com.bramestorm.bassanglertracker.base.BaseCatchEntryActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.training.VoiceCatchParse
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper.normalizeSpeciesName
import com.bramestorm.bassanglertracker.utils.getMotivationalMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatchEntryKgs : BaseCatchEntryActivity() {

    private lateinit var btnSetUp3Kgs: Button
    private lateinit var btnOpenWeightPopupKgs: Button
    private lateinit var simpleKgsListView: ListView
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper

    private var selectedSpecies: String = ""
    private var  totalWeightHundredthKg: Int = 0
    private val requestWeightEntry = 1001

    // --- voice-to-text callback handler ---
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            Toast.makeText(this@CatchEntryKgs, "Speech error $error", Toast.LENGTH_SHORT).show()
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
        setContentView(R.layout.activity_catch_entry_kgs)

        //******  Initialize speech recognizer ***********************
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(recognitionListener)
        }
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        dbHelper = CatchDatabaseHelper(this)

        btnSetUp3Kgs = findViewById(R.id.btnSetUp3Kgs)
        btnOpenWeightPopupKgs = findViewById(R.id.btnOpenWeightPopupKgs)
        simpleKgsListView = findViewById(R.id.simpleKgsListView)

        updateListViewKgs() // Load today's catches into ListView

        btnOpenWeightPopupKgs.setOnClickListener {
            openWeightPopupKgs()
        }

        btnSetUp3Kgs.setOnClickListener {
            val intent2 = Intent(this, SetUpActivity::class.java)
            startActivity(intent2)
        }

        simpleKgsListView.setOnItemLongClickListener { parent, view, position, id ->
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
        recognizer.destroy()
        super.onDestroy()
    }

    private fun openWeightPopupKgs() {
        val intent = Intent(this, PopupWeightEntryKgs::class.java)
        startActivityForResult(intent, requestWeightEntry)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestWeightEntry && resultCode == Activity.RESULT_OK) {
            totalWeightHundredthKg= data?.getIntExtra("lengthTotalCms", 0) ?: 0
            selectedSpecies = data?.getStringExtra("selectedSpecies") ?: selectedSpecies


            //  CALL `saveCatch()` IMMEDIATELY AFTER WEIGHT IS RECEIVED
            if (totalWeightHundredthKg > 0) {
                selectedSpecies = normalizeSpeciesName(selectedSpecies)
                saveCatch()
                Log.d("DB_DEBUG", "✅ saveCatch is called")
            } else {
                Log.e("DB_DEBUG", "⚠️ Invalid weight! Catch not saved.")
            }
        }
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
            totalWeightOz = null,
            totalLengthA8th = null,
            totalLengthTenths = null,
            totalWeightHundredthKg = totalWeightHundredthKg,
            catchType = "kgs",
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
            totalWeightHundredthKg = 0 // ✅ Move this after successful save
        }
        // ✅ MOTIVATIONAL TOAST FOR FUN DAY - Kilograms
        if (catchList.size >= 2) {
            val lastCatch = catchList.firstOrNull() // Most recent (sorted by dateTime)
            lastCatch?.let {
                val message = getMotivationalMessage(this, it.id, catchList.size, "kgs")
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        updateListViewKgs()  // ✅ Now only updates the UI, no extra insert
    }


    //:::::::::::::::: UPDATE LIST VIEW in time_Date Order ::::::::::::::::::::::::::::::::

    private fun updateListViewKgs() {
        Log.d("DB_DEBUG", "🔄 updateListViewKgs() is being called.")

        val todaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val todaysCatches = dbHelper.getCatchesForToday("kgs", todaysDate)
            .sortedByDescending { it.dateTime }  // Sort by dateTime (newest first)

        Log.d("DB_DEBUG", "🔍 Catches retrieved from DB: ${todaysCatches.size}")

        // ✅ Make sure catchList is updated BEFORE updating the ListView
        catchList.clear()
        catchList.addAll(todaysCatches)

        val catchDisplayList = todaysCatches.map {
            val totalWeightHundredthKg = it.totalWeightHundredthKg ?: 0
            val kgs = totalWeightHundredthKg / 100
            val grams = totalWeightHundredthKg % 100

            // Format the time from dateTime
            val timeFormatted = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val parsedDate = inputFormat.parse(it.dateTime ?: "")
                outputFormat.format(parsedDate ?: Date())
            } catch (e: Exception) {
                "N/A"
            }

            "${it.species} - $kgs.$grams Kgs @ $timeFormatted"
        }

        runOnUiThread {
            val adapter = CatchItemAdapter(this, catchList)
            simpleKgsListView.adapter = adapter
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
                updateListViewKgs()
                Toast.makeText(this, "Catch deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    //*************** EDIT list View of Catches ********************

    private fun showEditDialog(catchItem: CatchItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_catch_kgs, null)
        val edtWeightKgs = dialogView.findViewById<EditText>(R.id.edtDialogWeightKgs)
        val edtWeightGrams = dialogView.findViewById<EditText>(R.id.edtDialogWeightGrams)
        val spinnerSpecies = dialogView.findViewById<Spinner>(R.id.spinnerSpeciesEditKgs)

        // --- 1. Load user-selected species list ---
        val speciesList = SharedPreferencesManager.getSelectedSpeciesList(this)
        val normalizedSpeciesList = speciesList.map { normalizeSpeciesName(it) }
        val currentSpeciesNormalized = normalizeSpeciesName(catchItem.species)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecies.adapter = adapter

        // --- 2. Set current values ---
        val totalWeightKgs = catchItem.totalWeightHundredthKg ?: 0 // Default to 0 if null
        edtWeightKgs.setText((totalWeightKgs / 100).toString())
        edtWeightGrams.setText((totalWeightKgs % 100).toString())

        // --- 3. Set spinner selection based on normalized match ---
        val speciesIndex = normalizedSpeciesList.indexOf(currentSpeciesNormalized)
        spinnerSpecies.setSelection(if (speciesIndex != -1) speciesIndex else 0)

        // --- 4. Show dialog and handle save ---
        AlertDialog.Builder(this)
            .setTitle("Edit Catch")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newKgs = edtWeightKgs.text.toString().toIntOrNull() ?: 0
                val newGrams = edtWeightGrams.text.toString().toIntOrNull() ?: 0
                val totalWeightHundredthKg = (newKgs * 100) + newGrams
                val species = spinnerSpecies.selectedItem.toString()

                val dbHelper = CatchDatabaseHelper(this)

                dbHelper.updateCatch(
                    catchId = catchItem.id,
                    newWeightOz = null,
                    newWeightKg = totalWeightHundredthKg,
                    newLengthA8ths = null,
                    newLengthCm = null,
                    species = species
                )

                Log.d("DB_DEBUG", "✅ Updating ID=${catchItem.id}, New Weight=$totalWeightHundredthKg, New Species=$species")

                updateListViewKgs()
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
            if (p.totalWeightHundredthKg > 0) {
                // stash into your existing fields…
                totalWeightHundredthKg = p.totalWeightHundredthKg
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


}//+++++++++++++ END  od CATCH ENTRY Kgs ++++++++++++++++++++++++++++++++++++++++