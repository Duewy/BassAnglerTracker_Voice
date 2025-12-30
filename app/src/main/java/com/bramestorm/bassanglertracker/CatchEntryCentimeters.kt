package com.bramestorm.bassanglertracker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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

class CatchEntryCentimeters : BaseCatchEntryActivity() {

    private lateinit var btnSetUp3Cm: Button
    private lateinit var btnOpenLengthCmPopup: Button
    private lateinit var simpleCmListView: ListView
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper

    private var selectedSpecies: String = ""
    private var totalLengthTenths: Int = 0
    private var voiceControlEnabled = false
    private lateinit var voiceHelper: VoiceInteractionHelper

    private lateinit var dialogInstance: AlertDialog
    override val dialog: Any get() = dialogInstance

    companion object {
        const val EXTRA_LENGTH_CMS            = "totalLengthTenths"
        const val EXTRA_SPECIES               = "selectedSpecies"
    }

    private val lengthEntryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val totalLengthTenths = data.getIntExtra(EXTRA_LENGTH_CMS, 0)
                selectedSpecies = data.getStringExtra(EXTRA_SPECIES) ?: selectedSpecies

                if (totalLengthTenths > 0) {
                    selectedSpecies = normalizeSpeciesName(selectedSpecies)
                    saveCatch()
                } else {
                    Log.e("DB_DEBUG", "⚠️ Invalid length—nothing saved")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_centimeters)

        voiceControlEnabled = intent.getBooleanExtra("VCC_ENABLED", false)
        Log.d("VCC_FLOW", "Voice control enabled: $voiceControlEnabled")

        if (voiceControlEnabled) {
            startService(Intent(this, VoiceControlService::class.java).apply {
                action = VoiceControlService.ACTION_START_VOICE
            })

            voiceHelper = VoiceInteractionHelper(
                activity = this,
                measurementUnit = VoiceInteractionHelper.MeasurementUnit.CM,
                isTournament = false,
                onCommandAction = { transcript -> onSpeechResult(transcript) }
            )
        }

        dbHelper = CatchDatabaseHelper(this)

        btnSetUp3Cm = findViewById(R.id.btnSetUp3Cm)
        btnOpenLengthCmPopup = findViewById(R.id.btnOpenLengthCmPopup)
        simpleCmListView = findViewById(R.id.simpleCmListView)

        updateListViewCm()

        btnOpenLengthCmPopup.setOnClickListener {
            openLengthCmPopup()
        }

        btnSetUp3Cm.setOnClickListener {
            startActivity(Intent(this, SetUpActivity::class.java))
        }

        simpleCmListView.setOnItemLongClickListener { _, _, position, _ ->
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
        // TODO: implement parser or interaction
    }

    private fun openLengthCmPopup() {
        val intent = Intent(this, PopupLengthEntryCentimeters::class.java)
        lengthEntryLauncher.launch(intent)
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
            totalLengthTenths = totalLengthTenths,
            totalWeightHundredthKg = null,
            catchType = "fun_cm",
            markerType = selectedSpecies,
            clipColor = null
        )

        val success = dbHelper.insertCatch(newCatch)
        if (success) {
            Toast.makeText(this, "$selectedSpecies Catch Saved!", Toast.LENGTH_SHORT).show()
            totalLengthTenths = 0
        } else {
            Toast.makeText(this, "⚠️ Failed to save catch!", Toast.LENGTH_SHORT).show()
        }

        if (catchList.size >= 2) {
            catchList.firstOrNull()?.let {
                val message = getMotivationalMessage(this, it.id, catchList.size, "cms")
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        updateListViewCm()
    }

    private fun updateListViewCm() {
        val todaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todaysCatches = dbHelper.getCatchesForToday("fun_cm", todaysDate).sortedByDescending { it.dateTime }

        catchList.clear()
        catchList.addAll(todaysCatches)

        simpleCmListView.adapter = CatchItemAdapter(this, catchList)
    }

    private fun showEditDeleteDialog(catchItem: CatchItem) {
        AlertDialog.Builder(this)
            .setTitle("Edit or Delete")
            .setMessage("Do you want to edit or delete this entry?")
            .setPositiveButton("Edit") { _, _ -> showEditDialog(catchItem) }
            .setNegativeButton("Delete") { _, _ ->
                dbHelper.deleteCatch(catchItem.id)
                updateListViewCm()
                Toast.makeText(this, "Catch deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(catchItem: CatchItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_catch_cms, null)
        val edtLengthCms = dialogView.findViewById<EditText>(R.id.edtLengthCms)
        val edtLengthDecimal = dialogView.findViewById<EditText>(R.id.edtLengthDecimal)
        val spinnerSpeciesLbs = dialogView.findViewById<Spinner>(R.id.spinnerSpeciesEditCms)

SharedPreferencesManager.loadSpeciesList(this)
        val speciesList = SharedPreferencesManager.loadSpeciesList(this)
        val normalizedSpeciesList = speciesList.map { normalizeSpeciesName(it) }
        val currentSpeciesNormalized = normalizeSpeciesName(catchItem.species)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpeciesLbs.adapter = adapter


        val totalLengthTenths = catchItem.totalLengthTenths ?: 0
        edtLengthCms.setText((totalLengthTenths / 10).toString())
        edtLengthDecimal.setText((totalLengthTenths % 10).toString())

        clearOnceOnFocus(edtLengthCms)
        clearOnceOnFocus(edtLengthDecimal)

        val speciesIndex = normalizedSpeciesList.indexOf(currentSpeciesNormalized)
        spinnerSpeciesLbs.setSelection(if (speciesIndex != -1) speciesIndex else 0)

        AlertDialog.Builder(this)
            .setTitle("Edit Catch")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newCm = edtLengthCms.text.toString().toIntOrNull() ?: 0
                val newDecimal = edtLengthDecimal.text.toString().toIntOrNull() ?: 0
                val totalLengthTenths = ((newCm * 10) + newDecimal)
                val species = spinnerSpeciesLbs.selectedItem.toString()

                dbHelper.updateCatch(
                    catchId = catchItem.id,
                    newWeightOz = null,
                    newWeightKg = null,
                    newLengthQuarters = null,
                    newLengthCm = totalLengthTenths,
                    species = species
                )

                updateListViewCm()
                Toast.makeText(this, "Catch updated!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

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

    private fun getCurrentDateTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }
}
