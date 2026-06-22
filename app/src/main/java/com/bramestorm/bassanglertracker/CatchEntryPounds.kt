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

class CatchEntryPounds : BaseCatchEntryActivity() {

    private lateinit var btnSetUp3: Button
    private lateinit var btnOpenWeightPopupPounds: Button
    private lateinit var simplePoundsListView: ListView
    private val catchList = mutableListOf<CatchItem>()
    private lateinit var dbHelper: CatchDatabaseHelper
    private lateinit var tts: TextToSpeech

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

    // Push bottom-constrained views (like the AdView) above the system navigation bar
    val root = findViewById<View>(android.R.id.content)
    ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
        insets
    }

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
                android.util.Log.e("AdMob", "Banner failed in ${this@CatchEntryPounds::class.java.simpleName}: ${error.message}")
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

    private val voiceCatchReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            Log.d("VCC_FLOW", "📥 Received VOICE_CATCH_SAVED broadcast → updating list")
            updateListViewPounds()
        }
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
    //=================================================================================
    override fun onSpeechResult(transcript: String) {
        // Voice catches are handled upstream by VoiceControlService
        // and delivered via VOICE_CATCH_SAVED broadcast — no direct parsing needed here
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
            catchType = "fun_pounds",
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
        val todaysCatches = dbHelper.getCatchesForToday("fun_pounds", todaysDate).sortedByDescending { it.dateTime }

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
        val spinnerSpeciesLbs = dialogView.findViewById<Spinner>(R.id.spinnerSpeciesEditPounds)

        SharedPreferencesManager.loadSpeciesList(this)
        val speciesList = SharedPreferencesManager.loadSpeciesList(this)
        val normalizedSpeciesList = speciesList.map { normalizeSpeciesName(it) }
        val currentSpeciesNormalized = normalizeSpeciesName(catchItem.species)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpeciesLbs.adapter = adapter


        val totalWeightHundredthPounds = catchItem.totalWeightHundredthPounds ?: 0
        edtWeightLbs.setText((totalWeightHundredthPounds / 100).toString())
        edtWeightDec.setText((totalWeightHundredthPounds % 100).toString())

        clearOnceOnFocus(edtWeightLbs)
        clearOnceOnFocus(edtWeightDec)

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

    //============================================================================
    private fun getCurrentDateTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    //============================================================================
    override fun onManualWake() {
        openWeightPopupPounds()
    }

}//================= END ================================