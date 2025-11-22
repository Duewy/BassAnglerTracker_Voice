package com.bramestorm.bassanglertracker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
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
import com.bramestorm.bassanglertracker.training.VoiceInteractionHelper
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager.normalizeSpeciesName
import com.bramestorm.bassanglertracker.utils.getMotivationalMessage
import com.bramestorm.bassanglertracker.voice.VoiceControlService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatchEntryPounds : BaseCatchEntryActivity() {

    private lateinit var btnSetUp3: Button
    private lateinit var btnOpenWeightPopupPounds: Button
    private lateinit var simplePoundsListView: ListView
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper

    private var voiceControlEnabled = false
    private lateinit var voiceHelper: VoiceInteractionHelper

    private var selectedSpecies: String = ""
    private var totalWeightPounds: Int = 0
    private lateinit var dialogInstance: AlertDialog
    override val dialog: Any get() = dialogInstance

    companion object {
        const val EXTRA_WEIGHT_POUNDS = "weightTotalPounds"
        const val EXTRA_SPECIES = "selectedSpecies"
    }

    private val weightEntryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                totalWeightPounds = data.getIntExtra(EXTRA_WEIGHT_POUNDS, 0)
                selectedSpecies = data.getStringExtra(EXTRA_SPECIES) ?: selectedSpecies

                if (totalWeightPounds> 0) {
                    selectedSpecies = normalizeSpeciesName(selectedSpecies)
                    saveCatch()
                } else {
                    Log.e("DB_DEBUG", "⚠️ Invalid weight—nothing saved")
                }
            }
        }
    }

//=============== On Create ====================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_pounds)

        voiceControlEnabled = intent.getBooleanExtra("VCC_ENABLED", false)
        Log.d("VCC_FLOW", "Voice control enabled: $voiceControlEnabled")

        if (voiceControlEnabled) {
            startService(Intent(this, VoiceControlService::class.java).apply {
                action = VoiceControlService.ACTION_START_VOICE
            })

            voiceHelper = VoiceInteractionHelper(
                activity = this,
                measurementUnit = VoiceInteractionHelper.MeasurementUnit.POUNDS,
                isTournament = false,
                onCommandAction = { transcript -> onSpeechResult(transcript) }
            )
        }

        dbHelper = CatchDatabaseHelper(this)

        btnSetUp3 = findViewById(R.id.btnSetUp3)
        btnOpenWeightPopupPounds = findViewById(R.id.btnOpenWeightPopupPounds)
        simplePoundsListView = findViewById(R.id.simplePoundsListView)

        updateListViewPounds()

        btnOpenWeightPopupPounds.setOnClickListener {
            openWeightPopupPounds()
        }

        btnSetUp3.setOnClickListener {
            startActivity(Intent(this, SetUpActivity::class.java))
        }

        simplePoundsListView.setOnItemLongClickListener { _, _, position, _ ->
            if (position >= catchList.size) return@setOnItemLongClickListener true
            showEditDeleteDialog(catchList[position])
            true
        }
    }//=============== END on Create ==============================

    override fun onDestroy() {
        stopService(Intent(this, VoiceControlService::class.java))
        if (::voiceHelper.isInitialized) voiceHelper.shutdown()
        super.onDestroy()
    }
    //=================================================================================
    override fun onSpeechResult(transcript: String) {
        Log.d("VCC_TRANSCRIPT", "Received: $transcript")
        // TODO: implement actual parser or use broadcast response
    }
    //============================================================================
    private fun openWeightPopupPounds() {
        val popupIntent = Intent(this, PopupWeightEntryPounds::class.java)
        weightEntryLauncher.launch(popupIntent)
    }
    //============================================================================
    private fun saveCatch() {
        val newCatch = CatchItem(
            id = 0,
            latitude = null,
            longitude = null,
            dateTime = getCurrentDateTime(),
            species = selectedSpecies,
            totalWeightOz = null,
            totalWeightHundredthPounds = totalWeightPounds,
            totalLengthQuarters = null,
            totalLengthTenths = null,
            totalWeightHundredthKg = null,
            catchType = "pounds",
            markerType = selectedSpecies,
            clipColor = null
        )

        val success = dbHelper.insertCatch(newCatch)
        if (success) {
            Toast.makeText(this, "$selectedSpecies Catch Saved!", Toast.LENGTH_SHORT).show()
            totalWeightPounds = 0
        } else {
            Toast.makeText(this, "⚠️ Failed to save catch!", Toast.LENGTH_SHORT).show()
        }

        updateListViewPounds()
    }

    //============================================================================
    private fun updateListViewPounds() {
        val todaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todaysCatches = dbHelper.getCatchesForToday("pounds", todaysDate).sortedByDescending { it.dateTime } //todo check the correct catchType

        catchList.clear()
        catchList.addAll(todaysCatches)

        if (catchList.size >= 2) {
            catchList.firstOrNull()?.let {
                val message = getMotivationalMessage(this, it.id, catchList.size, "lbs")
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        simplePoundsListView.adapter = CatchItemAdapter(this, catchList)
    }

    //============================================================================
    private fun showEditDeleteDialog(catchItem: CatchItem) {
        AlertDialog.Builder(this)
            .setTitle("Edit or Delete")
            .setMessage("Do you want to edit or delete this entry?")
            .setPositiveButton("Edit") { _, _ -> showEditDialog(catchItem) }
            .setNegativeButton("Delete") { _, _ ->
                dbHelper.deleteCatch(catchItem.id)
                updateListViewPounds()
                Toast.makeText(this, "Catch deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    //============================================================================
    private fun showEditDialog(catchItem: CatchItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_catch_pounds, null)
        val edtWeightLbs = dialogView.findViewById<EditText>(R.id.edtWeightLbs)
        val edtWeightDec = dialogView.findViewById<EditText>(R.id.edtWeightDec)
        val spinnerSpeciesLbs = dialogView.findViewById<Spinner>(R.id.spinnerSpeciesEditLbs)

        val speciesList = SharedPreferencesManager.getSelectedSpeciesList(this)
        val normalizedSpeciesList = speciesList.map { normalizeSpeciesName(it) }
        val currentSpeciesNormalized = normalizeSpeciesName(catchItem.species)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpeciesLbs.adapter = adapter

        val totalWeightHundredthPounds = catchItem.totalWeightHundredthPounds ?: 0
        edtWeightLbs.setText((totalWeightHundredthPounds / 100).toString())
        edtWeightDec.setText((totalWeightHundredthPounds % 100).toString())

        val speciesIndex = normalizedSpeciesList.indexOf(currentSpeciesNormalized)
        spinnerSpeciesLbs.setSelection(if (speciesIndex != -1) speciesIndex else 0)

        AlertDialog.Builder(this)
            .setTitle("Edit Catch")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newLbs = edtWeightLbs.text.toString().toIntOrNull() ?: 0
                val newOzs = edtWeightDec.text.toString().toIntOrNull() ?: 0
                val newTotalWeightHundredthPounds = (newLbs * 100) + newOzs
                val species = spinnerSpeciesLbs.selectedItem.toString()

                dbHelper.updateCatch(
                    catchId = catchItem.id,
                    newWeightOz = null,
                    newWeightPounds = newTotalWeightHundredthPounds,
                    newWeightKg = null,
                    newLengthQuarters = null,
                    newLengthCm = null,
                    species = species
                )

                updateListViewPounds()
                Toast.makeText(this, "Catch updated!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //============================================================================
    private fun getCurrentDateTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    //============================================================================
    override fun onManualWake() {
        openWeightPopupPounds()
    }

}//================= END ================================