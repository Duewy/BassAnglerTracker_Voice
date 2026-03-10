package com.bramestorm.bassanglertracker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bramestorm.bassanglertracker.base.BaseCatchEntryActivity
import com.bramestorm.bassanglertracker.database.CatchDatabaseHelper
import com.bramestorm.bassanglertracker.training.VoiceInteractionHelper
import com.bramestorm.bassanglertracker.utils.GpsUtils
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager.normalizeSpeciesName
import com.bramestorm.bassanglertracker.utils.getMotivationalMessage
import com.bramestorm.bassanglertracker.voice.VoiceControlService
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CatchEntryInches : BaseCatchEntryActivity() {

    private lateinit var btnSetUp3Inch: Button
    private lateinit var btnOpenWeightPopupInch: Button
    private lateinit var simpleInchListView: ListView
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper
    private lateinit var voiceHelper: VoiceInteractionHelper
    private var voiceControlEnabled = false
    private lateinit var tts: TextToSpeech

    private var selectedSpecies: String = ""
    private var totalLengthQuarters: Int = 0
    private lateinit var dialogInstance: AlertDialog
    override val dialog: Any get() = dialogInstance

    companion object {
        const val EXTRA_LENGTH_INCHES = "totalLengthQuarters"
        const val EXTRA_SPECIES       = "selectedSpecies"
    }

    private val lengthEntryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                totalLengthQuarters = data.getIntExtra(EXTRA_LENGTH_INCHES, 0)
                selectedSpecies = data.getStringExtra(EXTRA_SPECIES) ?: selectedSpecies

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catch_entry_inches)

        // Push bottom-constrained views (like the AdView) above the system navigation bar
        val root = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }

        // VCC Initialization
        voiceControlEnabled = intent.getBooleanExtra("VCC_ENABLED", false)
        Log.d("VCC_FLOW", "Voice control enabled: $voiceControlEnabled")

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
        }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
            }
        }

        dbHelper = CatchDatabaseHelper(this)

        btnSetUp3Inch = findViewById(R.id.btnSetUp3Inch)
        btnOpenWeightPopupInch = findViewById(R.id.btnOpenWeightPopupInch)
        simpleInchListView = findViewById(R.id.simpleInchListView)

        updateListViewInch()

        btnOpenWeightPopupInch.setOnClickListener {
            val intent = Intent(this, PopupLengthEntryInches::class.java)
            lengthEntryLauncher.launch(intent)
        }

        btnSetUp3Inch.setOnClickListener {
            val intent2 = Intent(this, SetUpActivity::class.java)
            startActivity(intent2)
        }

        simpleInchListView.setOnItemLongClickListener { _, _, position, _ ->
            if (catchList.isEmpty() || position >= catchList.size) {
                Toast.makeText(this, "😢 No catches available", Toast.LENGTH_SHORT).show()
                return@setOnItemLongClickListener true
            }
            showEditDeleteDialog(catchList[position])
            true
        }

        updateVccLabel()
        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)

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

    override fun onDestroy() {
        stopService(Intent(this, VoiceControlService::class.java))
        if (::voiceHelper.isInitialized) voiceHelper.shutdown()
        if (voiceControlEnabled) {
            unregisterReceiver(voiceCatchReceiver)
        }
        super.onDestroy()
    }

    // ── Add onResume() to refresh status when app wakes: ──
    override fun onResume() {
        super.onResume()
        updateVccLabel()
        GpsUtils.updateGpsStatusLabel(findViewById(R.id.txtGPSNotice), this)
    }

    // ── Add the updateVccLabel() function: ──
    private fun updateVccLabel() {
        val txtVCC = findViewById<TextView>(R.id.txtVCCFunDay)
        if (voiceControlEnabled) {
            txtVCC.text = getString(R.string.vcc_on)
            txtVCC.setBackgroundColor(ContextCompat.getColor(this, R.color.clip_yellow))
            txtVCC.setTextColor(ContextCompat.getColor(this, R.color.clip_orange))
        } else {
            txtVCC.text = getString(R.string.manual_mode)
            txtVCC.setTextColor(ContextCompat.getColor(this, R.color.clip_blue))
            txtVCC.background = null
        }
    }

    private val voiceCatchReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            Log.d("VCC_FLOW", "📥 Received VOICE_CATCH_SAVED broadcast → updating list")
            updateListViewInch()
        }
    }


    override fun onSpeechResult(transcript: String) {
        Log.d("VCC_TRANSCRIPT", "Received: $transcript")
        // You can handle voice input parsing here if needed
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
            totalLengthQuarters = totalLengthQuarters,
            totalLengthTenths = null,
            totalWeightHundredthKg = null,
            catchType = "fun_inches",
            markerType = selectedSpecies,
            clipColor = null
        )

        val success = dbHelper.insertCatch(newCatch)

        if (success) {
            Toast.makeText(this, "$selectedSpecies Catch Saved!", Toast.LENGTH_SHORT).show()
            totalLengthQuarters = 0
        } else {
            Toast.makeText(this, "⚠️ Failed to save catch!", Toast.LENGTH_SHORT).show()
        }

        if (catchList.size >= 2) {
            catchList.firstOrNull()?.let {
                val message = getMotivationalMessage(this, it.id, catchList.size, "inches")
                if (message != null) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        updateListViewInch()
    }

    private fun updateListViewInch() {
        val todaysDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todaysCatches = dbHelper.getCatchesForToday("fun_inches", todaysDate)
            .sortedByDescending { it.dateTime }

        catchList.clear()
        catchList.addAll(todaysCatches)

        runOnUiThread {
            val adapter = CatchItemAdapter(this, catchList)
            simpleInchListView.adapter = adapter
        }
    }

    private fun showEditDeleteDialog(catchItem: CatchItem) {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Edit or Delete")
            .setMessage("Do you want to edit or delete this entry?")
            .setPositiveButton("Edit") { _, _ -> showEditDialog(catchItem) }
            .setNegativeButton("Delete") { _, _ ->
                dbHelper.deleteCatch(catchItem.id)
                updateListViewInch()
                Toast.makeText(this, "Catch deleted!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(catchItem: CatchItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_catch_inches, null)
        val edtLengthInches = dialogView.findViewById<EditText>(R.id.edtLengthInches)
        val edtLengthEntryQuarters = dialogView.findViewById<EditText>(R.id.edtLengthQuarters)
        val spinnerSpeciesEditInches = dialogView.findViewById<Spinner>(R.id.spinnerSpeciesEditInches)

SharedPreferencesManager.loadSpeciesList(this)
        val speciesList = SharedPreferencesManager.loadSpeciesList(this)
        val normalizedSpeciesList = speciesList.map { normalizeSpeciesName(it) }
        val currentSpeciesNormalized = normalizeSpeciesName(catchItem.species)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpeciesEditInches.adapter = adapter


        val newLengthQuarters = catchItem.totalLengthQuarters ?: 0
        edtLengthInches.setText((newLengthQuarters / 4).toString())
        edtLengthEntryQuarters.setText((newLengthQuarters % 4).toString())

        clearOnceOnFocus(edtLengthInches)
        clearOnceOnFocus(edtLengthEntryQuarters)

        val speciesIndex = normalizedSpeciesList.indexOf(currentSpeciesNormalized)
        spinnerSpeciesEditInches.setSelection(if (speciesIndex != -1) speciesIndex else 0)

        AlertDialog.Builder(this)
            .setTitle("Edit Catch")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newInches = edtLengthInches.text.toString().toIntOrNull() ?: 0
                val new4ths = edtLengthEntryQuarters.text.toString().toIntOrNull() ?: 0
                val totalLengthQuarters = ((newInches * 4) + new4ths)
                val species = spinnerSpeciesEditInches.selectedItem.toString()

                dbHelper.updateCatch(
                    catchId = catchItem.id,
                    newWeightOz = null,
                    newWeightKg = null,
                    newLengthQuarters = totalLengthQuarters,
                    newLengthCm = null,
                    species = species
                )

                updateListViewInch()
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
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
