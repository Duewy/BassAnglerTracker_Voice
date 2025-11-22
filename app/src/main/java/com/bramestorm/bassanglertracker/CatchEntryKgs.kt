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

class CatchEntryKgs : BaseCatchEntryActivity() {

    private lateinit var btnSetUp3Kgs: Button
    private lateinit var btnOpenWeightPopupKgs: Button
    private lateinit var simpleKgsListView: ListView
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper

    private var voiceControlEnabled = false
    private lateinit var voiceHelper: VoiceInteractionHelper

    private var selectedSpecies: String = ""
    private var totalWeightHundredthKg: Int = 0
    private lateinit var dialogInstance: AlertDialog
    override val dialog: Any get() = dialogInstance

    companion object {
        const val EXTRA_WEIGHT_KGS = "totalWeightHundredthKg"
        const val EXTRA_SPECIES = "selectedSpecies"
    }

    private val weightEntryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                totalWeightHundredthKg = data.getIntExtra(EXTRA_WEIGHT_KGS, 0)
                selectedSpecies = data.getStringExtra(EXTRA_SPECIES) ?: selectedSpecies

                if (totalWeightHundredthKg > 0) {
                    selectedSpecies = normalizeSpeciesName(selectedSpecies)
                    saveCatch()
                } else {
                    Log.e("DB_DEBUG", "⚠️ Invalid weight—nothing saved")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_kgs)

        voiceControlEnabled = intent.getBooleanExtra("VCC_ENABLED", false)
        Log.d("VCC_FLOW", "Voice control enabled: $voiceControlEnabled")

        if (voiceControlEnabled) {
            startService(Intent(this, VoiceControlService::class.java).apply {
                action = VoiceControlService.ACTION_START_VOICE
            })

            voiceHelper = VoiceInteractionHelper(
                activity = this,
                measurementUnit = VoiceInteractionHelper.MeasurementUnit.KG_G,
                isTournament = false,
                onCommandAction = { transcript -> onSpeechResult(transcript) }
            )
        }

        dbHelper = CatchDatabaseHelper(this)

        btnSetUp3Kgs = findViewById(R.id.btnSetUp3Kgs)
        btnOpenWeightPopupKgs = findViewById(R.id.btnOpenWeightPopupKgs)
        simpleKgsListView = findViewById(R.id.simpleKgsListView)

        updateListViewKgs()

        btnOpenWeightPopupKgs.setOnClickListener {
            openWeightPopupKgs()
        }

        btnSetUp3Kgs.setOnClickListener {
            startActivity(Intent(this, SetUpActivity::class.java))
        }

        simpleKgsListView.setOnItemLongClickListener { _, _, position, _ ->
            if (position >= catchList.size) return@setOnItemLongClickListener true
            showEditDeleteDialog(catchList[position])
            true
        }
    }

    override fun onDestroy() {
        stopService(Intent(this, VoiceControlService::class.java))
        if (::voiceHelper.isInitialized) voiceHelper.shutdown()
        super.onDestroy()
    }

    override fun onSpeechResult(transcript: String) {
        Log.d("VCC_TRANSCRIPT", "Received: $transcript")
        // TODO: implement actual parser
    }

    private fun openWeightPopupKgs() {
        val popupIntent = Intent(this, PopupWeightEntryKgs::class.java)
        weightEntryLauncher.launch(popupIntent)
    }

    private fun saveCatch() {
        val newCatch = CatchItem(
            id = 0,
            latitude = null,
            longitude = null,
            dateTime = getCurrentDateTime(),
            species = selectedSpecies,
            totalWeightOz = null,
            totalWeightHundredthPounds = null,
            totalLengthQuarters = null,
            totalLengthTenths = null,
            totalWeightHundredthKg = totalWeightHundredthKg,
            catchType = "kgs",
            markerType = selectedSpecies,
            clipColor = null
        )

        val success = dbHelper.insertCatch(newCatch)
        if (success) {
            Toast.makeText(this, "$selectedSpecies Catch Saved!", Toast.LENGTH_SHORT).show()
            totalWeightHundredthKg = 0
        } else {
            Toast.makeText(this, "⚠️ Failed to save catch!", Toast.LENGTH_SHORT).show()
        }
        updateListViewKgs()
    }

    private fun updateListViewKgs() {
        val todaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todaysCatches = dbHelper.getCatchesForToday("kgs", todaysDate).sortedByDescending { it.dateTime }

        catchList.clear()
        catchList.addAll(todaysCatches)

        if (catchList.size >= 2) {
            catchList.firstOrNull()?.let {
                val message = getMotivationalMessage(this, it.id, catchList.size, "Kgs")
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        simpleKgsListView.adapter = CatchItemAdapter(this, catchList)
    }

    private fun showEditDeleteDialog(catchItem: CatchItem) {
        AlertDialog.Builder(this)
            .setTitle("Edit or Delete")
            .setMessage("Do you want to edit or delete this entry?")
            .setPositiveButton("Edit") { _, _ -> showEditDialog(catchItem) }
            .setNegativeButton("Delete") { _, _ ->
                dbHelper.deleteCatch(catchItem.id)
                updateListViewKgs()
                Toast.makeText(this, "Catch deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(catchItem: CatchItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_catch_kgs, null)
        val edtWeightKgs = dialogView.findViewById<EditText>(R.id.edtDialogWeightKgs)
        val edtWeightGrams = dialogView.findViewById<EditText>(R.id.edtDialogWeightGrams)
        val spinnerSpecies = dialogView.findViewById<Spinner>(R.id.spinnerSpeciesEditKgs)

        val speciesList = SharedPreferencesManager.getSelectedSpeciesList(this)
        val normalizedSpeciesList = speciesList.map { normalizeSpeciesName(it) }
        val currentSpeciesNormalized = normalizeSpeciesName(catchItem.species)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecies.adapter = adapter

        val totalWeightKgs = catchItem.totalWeightHundredthKg ?: 0
        edtWeightKgs.setText((totalWeightKgs / 100).toString())
        edtWeightGrams.setText((totalWeightKgs % 100).toString())

        val speciesIndex = normalizedSpeciesList.indexOf(currentSpeciesNormalized)
        spinnerSpecies.setSelection(if (speciesIndex != -1) speciesIndex else 0)

        AlertDialog.Builder(this)
            .setTitle("Edit Catch")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newKgs = edtWeightKgs.text.toString().toIntOrNull() ?: 0
                val newGrams = edtWeightGrams.text.toString().toIntOrNull() ?: 0
                val totalWeightHundredthKg = (newKgs * 100) + newGrams
                val species = spinnerSpecies.selectedItem.toString()

                dbHelper.updateCatch(
                    catchId = catchItem.id,
                    newWeightOz = null,
                    newWeightKg = totalWeightHundredthKg,
                    newLengthQuarters = null,
                    newLengthCm = null,
                    species = species
                )

                updateListViewKgs()
                Toast.makeText(this, "Catch updated!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getCurrentDateTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }
}
