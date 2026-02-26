package com.bramestorm.bassanglertracker

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.activities.SpeciesOrganizeActivity
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper
import com.bramestorm.bassanglertracker.utils.positionedToast
import com.bramestorm.bassanglertracker.voice.VoiceControlService
import java.util.Date


class SetUpActivity : AppCompatActivity() {

    private lateinit var btnLbsOzs: Button
    private lateinit var btnPounds: Button
    private lateinit var btnKilograms: Button
    private lateinit var btnInches: Button
    private lateinit var btnCentimeters: Button
    private lateinit var txtLimitMarker :TextView
    private lateinit var txtSpeciesSelector :TextView
    private lateinit var btnFunDay: Button
    private lateinit var btnTournament: Button
    private lateinit var btnStartFishing: Button
    private lateinit var spinnerTournamentSpecies: Spinner
    private lateinit var tglCullingValue: ToggleButton
    private lateinit var tglGPS: ToggleButton
    private lateinit var tglVoice:ToggleButton
    private lateinit var btnMainSetup:Button
    private lateinit var btnCustomizeSpecies :Button

    // --------------- Permission Codes for GPS and Porcupine ----------------
    companion object {
        private const val REQUEST_RECORD_AUDIO              = 100
        private const val REQUEST_BLUETOOTH_CONNECT         = 101
        private const val LOCATION_PERMISSION_REQUEST_CODE  = 1001
        private const val REQUEST_PHONE_STATE               = 1003
        private const val REQUEST_VOICE_SETUP               = 2001
        private const val REQUEST_DEEP_DOZE_AGREEMENT       = 2002

        // values for the VoiceHandlers to identify which MeasurementMode in use
            const val TYPE_FUN_LBS      = 1
            const val TYPE_FUN_POUNDS   = 2
            const val TYPE_FUN_KGS      = 3
            const val TYPE_FUN_INCH     = 4
            const val TYPE_FUN_CM       = 5
            const val TYPE_TOURN_LBS    = 6
            const val TYPE_TOURN_POUNDS = 7
            const val TYPE_TOURN_KGS    = 8
            const val TYPE_TOURN_CM     = 9
            const val TYPE_TOURN_INCH   = 10
            const val TYPE_DEFAULT      = 0


        const val EXTRA_SPECIES       = "selectedSpecies"
        const val EXTRA_CATCH_TYPE    = "catchType"
        const val EXTRA_IS_TOURNAMENT = "isTournament"
        const val EXTRA_TOURNAMENT_SPECIES = "tournamentSpecies"
        const val EXTRA_NUMBER_OF_CATCHES = "NUMBER_OF_CATCHES"

        private const val PREFS_NAME                        = "BassAnglerTrackerPrefs"
        private const val KEY_VOICE_CONTROL                 = "VOICE_CONTROL_ENABLED"
        private const val KEY_LAST_VOICE_DATE               = "VOICE_LAST_TOGGLE_DATE"
        private const val KEY_USE_BLUETOOTH_MODE            = "VOICE_USE_BLUETOOTH"
    }

    private val sharedPreferences by lazy { getSharedPreferences("BassAnglerTrackerPrefs", MODE_PRIVATE) }

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }


    private var isFunDaySelected = false
    private var isTournamentSelected = false
    private var selectedSpecies: String = ""
    private var isGPSInitializingToggle = true
    private var isVoiceInitializingToggle = false


    private var isLbsOzsSelected = false
    private var isPoundsDecimalSelected = false
    private var isKilogramsSelected = false
    private var isInchesSelected = false
    private var isCentimetersSelected = false


    // ----------- Set the BackGround of Unselected to lt Grey
    private fun resetUnitSelectionHighlights() {
        btnLbsOzs.setBackgroundResource(R.color.lite_grey)
        btnPounds.setBackgroundResource(R.color.lite_grey)
        btnKilograms.setBackgroundResource(R.color.lite_grey)
        btnInches.setBackgroundResource(R.color.lite_grey)
        btnCentimeters.setBackgroundResource(R.color.lite_grey)
        btnKilograms.setTextColor(ContextCompat.getColor(this, R.color.black))
    }//--------------------------------------------------------


    //--------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPhoneStatePermissionIfNeeded()

        //------------------- Ensures that the GPS must be Re-Enabled Every Day --------------------

        val today = DateFormat.format("yyyy-MM-dd", Date()).toString()
        val lastVoiceDate = prefs.getString(KEY_LAST_VOICE_DATE, "")
        if (lastVoiceDate != today) {
            prefs.edit()
                .putBoolean(KEY_VOICE_CONTROL, false)
                .putString(KEY_LAST_VOICE_DATE, today)
                .apply()
            SharedPreferencesManager.setVccEnabled(this, false) // daily reset also resets VCC service state
        }

        setContentView(R.layout.activity_set_up_event)

        // Initialize UI components
        btnLbsOzs = findViewById(R.id.btnLbsOzs)
        btnPounds = findViewById(R.id.btnPounds)
        btnKilograms = findViewById(R.id.btnKilograms)
        btnInches = findViewById(R.id.btnInches)
        btnCentimeters = findViewById(R.id.btnCentimeters)
        btnFunDay = findViewById(R.id.btnFunDay)
        btnTournament = findViewById(R.id.btnTournament)
        btnStartFishing = findViewById(R.id.btnStartFishing)
        txtLimitMarker = findViewById(R.id.txtLimitMarker)
        tglCullingValue = findViewById(R.id.tglCullingValue)
        txtSpeciesSelector = findViewById(R.id.txtSpeciesSelector)
        spinnerTournamentSpecies = findViewById(R.id.spinnerTournamentSpecies)
        tglGPS = findViewById(R.id.tglGPS)
        tglVoice = findViewById(R.id.tglVoice)
        btnMainSetup = findViewById(R.id.btnMainSetup)
        btnCustomizeSpecies = findViewById(R.id.btnCustomizeSpecies)

        tglCullingValue.alpha = 0.3f
        tglCullingValue.isEnabled=false
        txtSpeciesSelector.alpha = 0.3f
        txtLimitMarker.alpha = 0.3f
        tglCullingValue.alpha = 0.3f
        spinnerTournamentSpecies.alpha = 0.3f
        spinnerTournamentSpecies.isEnabled = false

        // Toggle Weight Selection
        btnLbsOzs.setOnClickListener {
            isLbsOzsSelected = true
            isPoundsDecimalSelected = false
            isKilogramsSelected = false
            isInchesSelected = false
            isCentimetersSelected = false
            resetUnitSelectionHighlights()
            btnLbsOzs.setBackgroundResource(R.color.main_compliment)
        }

        btnPounds.setOnClickListener {
            isLbsOzsSelected = false
            isPoundsDecimalSelected = true
            isKilogramsSelected = false
            isInchesSelected = false
            isCentimetersSelected = false
            resetUnitSelectionHighlights()
            btnPounds.setBackgroundResource(R.color.main_compliment)
        }


        btnKilograms.setOnClickListener {
            isLbsOzsSelected = false
            isPoundsDecimalSelected = false
            isKilogramsSelected = true
            isInchesSelected = false
            isCentimetersSelected = false
            resetUnitSelectionHighlights()
            btnKilograms.setBackgroundResource(R.color.main_compliment)
            btnKilograms.setTextColor(ContextCompat.getColor(this, R.color.black))

        }

        // Toggle Length Selection
        btnInches.setOnClickListener {
            isLbsOzsSelected = false
            isPoundsDecimalSelected = false
            isKilogramsSelected = false
            isInchesSelected = true
            isCentimetersSelected = false
            resetUnitSelectionHighlights()
            btnInches.setBackgroundResource(R.color.main_compliment)
        }

        btnCentimeters.setOnClickListener {
            isLbsOzsSelected = false
            isPoundsDecimalSelected = false
            isKilogramsSelected = false
            isInchesSelected = false
            isCentimetersSelected = true
            resetUnitSelectionHighlights()
            btnCentimeters.setBackgroundResource(R.color.main_compliment)
        }

        // Toggle Fun Day/Tournament Selection
        btnFunDay.setOnClickListener {
            Log.d("DEBUG", "FunDay Is Selected ")
            isFunDaySelected = true
            isTournamentSelected = false
            btnFunDay.setBackgroundResource(R.color.clip_bright_green)
            btnTournament.setBackgroundResource(R.color.lite_grey)
            txtLimitMarker.alpha = 0.3f
           // txtLimitMarker.setBackgroundResource(R.color.nothing)
            txtSpeciesSelector.alpha = 0.3f
          //  txtSpeciesSelector.setBackgroundResource(R.color.nothing)
            tglCullingValue.alpha = 0.3f
           // tglCullingValue.setBackgroundResource(R.color.lite_grey)
            tglCullingValue.isEnabled=false
            spinnerTournamentSpecies.alpha = 0.3f
            spinnerTournamentSpecies.isEnabled = false
        }

        btnTournament.setOnClickListener {
            Log.d("DEBUG", "Tournament Is Selected ")
            isTournamentSelected = true
            isFunDaySelected = false
            btnTournament.setBackgroundResource(R.color.clip_bright_green)
            btnFunDay.setBackgroundResource(R.color.lite_grey)
            tglCullingValue.alpha = 1.0f
           // tglCullingValue.setBackgroundResource(R.drawable.btn_outline_off_white)
            txtLimitMarker.alpha = 1.0f
            //txtLimitMarker.setBackgroundResource(R.drawable.btn_outline_off_white)
            txtSpeciesSelector.alpha = 1.0f
            //txtSpeciesSelector.setBackgroundResource(R.drawable.btn_outline_off_white)
            tglCullingValue.isEnabled = true
            spinnerTournamentSpecies.alpha = 1.0f
            spinnerTournamentSpecies.isEnabled = true
        }


// |||||||||||||| Load saved GPS state ||||||||||||||||||||||||||||||||||

// If edition doesn't support GPS, force OFF and orange, no matter prefs
        if (!BuildConfig.FEATURE_GPS_LOGGING) {
            isGPSInitializingToggle = true
            tglGPS.isChecked = false
            tglGPS.setBackgroundResource(R.drawable.btn_outline_orange)
            isGPSInitializingToggle = false
        } else {
            //------ ✅ Check both: saved state AND permission for GPS -----------
            val isGpsEnabledInPrefs = sharedPreferences.getBoolean("GPS_ENABLED", false)
            val hasLocationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            isGPSInitializingToggle = true
            tglGPS.isChecked = isGpsEnabledInPrefs && hasLocationPermission
            tglGPS.setBackgroundResource(
                if (tglGPS.isChecked) R.drawable.btn_outline_green else R.drawable.btn_outline_orange
            )
            isGPSInitializingToggle = false
        }

        tglGPS.setOnCheckedChangeListener { _, isChecked ->
            if (isGPSInitializingToggle) return@setOnCheckedChangeListener

            if (isChecked && !BuildConfig.FEATURE_GPS_LOGGING) {
                positionedToast("GPS logging is available in the Tracker or Pro VC editions only.")
                isGPSInitializingToggle = true
                tglGPS.isChecked = false
                tglGPS.setBackgroundResource(R.drawable.btn_outline_orange)
                isGPSInitializingToggle = false
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                checkAndRequestLocationPermission()
                tglGPS.setBackgroundResource(R.drawable.btn_outline_green)
            } else {
                disableGps()
                tglGPS.setBackgroundResource(R.drawable.btn_outline_orange)
            }
        }

// ------ VOICE CONTROL ENABLE ----------------
        tglVoice.setOnCheckedChangeListener { _, isChecked ->

            if (isVoiceInitializingToggle) return@setOnCheckedChangeListener

            // 🔒 ProVC only
            if (isChecked && !BuildConfig.FEATURE_VOICE_COMMANDS) {
                positionedToast(
                    "Voice Control is available in the Pro VC edition only.\n" +
                            "Upgrade to enable hands‑free catch logging."
                )
                isVoiceInitializingToggle = true
                tglVoice.isChecked = false
                tglVoice.setBackgroundResource(R.drawable.btn_outline_orange)
                isVoiceInitializingToggle = false
                return@setOnCheckedChangeListener
            }

            if (isChecked) {

                // a) BT device must be connected
                if (!isBluetoothConnectedSafe()) {
                    positionedToast("⚠️ Please connect a Bluetooth headset/mic for voice control.")
                    isVoiceInitializingToggle = true
                    tglVoice.isChecked = false
                    tglVoice.setBackgroundResource(R.drawable.btn_outline_orange)
                    isVoiceInitializingToggle = false
                    return@setOnCheckedChangeListener
                }

                // b) Deep‐doze agreement
                if (!SharedPreferencesManager.hasUserAgreedToDeepDoze(this)) {
                    startActivityForResult(
                        Intent(this, UserAgreementForDeepDozeActivity::class.java),
                        REQUEST_DEEP_DOZE_AGREEMENT
                    )
                    isVoiceInitializingToggle = true
                    tglVoice.isChecked = false
                    tglVoice.setBackgroundResource(R.drawable.btn_outline_orange)
                    isVoiceInitializingToggle = false
                    return@setOnCheckedChangeListener
                }

                // c) RECORD_AUDIO permission
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        REQUEST_RECORD_AUDIO
                    )
                    isVoiceInitializingToggle = true
                    tglVoice.isChecked = false
                    tglVoice.setBackgroundResource(R.drawable.btn_outline_orange)
                    isVoiceInitializingToggle = false
                    return@setOnCheckedChangeListener
                }

                // d) BLUETOOTH_CONNECT permission (Android S+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(
                        this, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        REQUEST_BLUETOOTH_CONNECT
                    )
                    isVoiceInitializingToggle = true
                    tglVoice.isChecked = false
                    tglVoice.setBackgroundResource(R.drawable.btn_outline_orange)
                    isVoiceInitializingToggle = false
                    return@setOnCheckedChangeListener
                }

                // e) All checks passed → save prefs & start the service
                prefs.edit()
                    .putBoolean(KEY_USE_BLUETOOTH_MODE, true)
                    .putBoolean(KEY_VOICE_CONTROL, true)
                    .apply()

                // ✅ This is what startVoiceService() checks (single source of truth for service)
                SharedPreferencesManager.setVccEnabled(this, true)

                positionedToast("🤙 Voice Control Enabled (Bluetooth mic).")
                startVoiceService()

                tglVoice.setBackgroundResource(R.drawable.btn_outline_green)

            } else {
                // Turning OFF
                prefs.edit()
                    .putBoolean(KEY_VOICE_CONTROL, false)
                    .apply()

                // ✅ Keep service state consistent with SharedPreferencesManager.isVccEnabled(...)
                SharedPreferencesManager.setVccEnabled(this, false)

                stopVoiceService()
                positionedToast("Voice control disabled.")
                tglVoice.setBackgroundResource(R.drawable.btn_outline_orange)
            }
        }
// ---------------- END tglVoice ----------------


        btnMainSetup.setOnClickListener {
            val intent2 = Intent(this, MainActivity::class.java)
            startActivity(intent2)
        }

        btnCustomizeSpecies.setOnClickListener {
            val intent = Intent(this,SpeciesOrganizeActivity::class.java)
            startActivity(intent)
        }


        //----------  Load user-Selected SPECIES LIST with icons --------------
        loadTournamentSpeciesSpinner()


        //---  Fishing Event Selection (Fun Day or Tournament)
        btnStartFishing.setOnClickListener {
            var catchEntryType = TYPE_DEFAULT
            val nextActivity: Class<*>? = when {
                // — Fun Day branches —
                isFunDaySelected && isLbsOzsSelected-> {
                    catchEntryType = TYPE_FUN_LBS
                    CatchEntryLbsOzs::class.java
                }
                isFunDaySelected && isPoundsDecimalSelected-> {
                    catchEntryType = TYPE_FUN_POUNDS
                    CatchEntryPounds::class.java
                }
                isFunDaySelected && isKilogramsSelected -> {
                    catchEntryType = TYPE_FUN_KGS
                    CatchEntryKgs::class.java
                }
                isFunDaySelected && isCentimetersSelected -> {
                    catchEntryType = TYPE_FUN_CM
                    CatchEntryCentimeters::class.java
                }
                isFunDaySelected && isInchesSelected -> {
                    catchEntryType = TYPE_FUN_INCH
                    CatchEntryInches::class.java
                }


                // — Tournament branches —
                isTournamentSelected && isLbsOzsSelected-> {
                    catchEntryType = TYPE_TOURN_LBS
                    CatchEntryTournament::class.java
                }
                isTournamentSelected && isPoundsDecimalSelected-> {
                    catchEntryType = TYPE_TOURN_POUNDS
                    CatchEntryTournamentPounds::class.java
                }
                isTournamentSelected && isKilogramsSelected-> {
                    catchEntryType = TYPE_TOURN_KGS
                    CatchEntryTournamentKgs::class.java
                }
                isTournamentSelected && isCentimetersSelected -> {
                    catchEntryType = TYPE_TOURN_CM
                    CatchEntryTournamentCentimeters::class.java
                }
                isTournamentSelected && isInchesSelected -> {
                    catchEntryType = TYPE_TOURN_INCH
                    CatchEntryTournamentInches::class.java
                }

                else -> null
            }
            SharedPreferencesManager.saveCatchEntryType(this@SetUpActivity, catchEntryType)

            Log.d(TAG,"🗃️G, $catchEntryType")

            if (nextActivity != null) {
                Intent(this@SetUpActivity, nextActivity).apply {
                    putExtra("VCC_ENABLED", tglVoice.isChecked)
                    // tournament‐only extras
                    if (isTournamentSelected) {
                        putExtra("NUMBER_OF_CATCHES", if (tglCullingValue.isChecked) 5 else 4)
                        putExtra("TOURNAMENT_SPECIES", selectedSpecies)
                        putExtra("CULLING_ENABLED", tglCullingValue.isChecked)
                        // instead of SharedPreferencesManager.getInstance(this).apply { … }
                        val ctx = this@SetUpActivity

                            // persist tournament‐mode settings into prefs:
                        SharedPreferencesManager.setVccEnabled(ctx, tglVoice.isChecked)
                        SharedPreferencesManager.setTournamentSpecies(ctx, selectedSpecies)
                        SharedPreferencesManager.setNumberOfCatches(ctx, if (tglCullingValue.isChecked) 5 else 4)
                        SharedPreferencesManager.setCullingEnabled(ctx, tglCullingValue.isChecked)
                    }
                }.also { startActivity(it) }
            } else {
                positionedToast("⚠️ Please select a Measurement and Unit Type!")
                }
        }

    }  //=================== END of ON CREATE ================================

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VOICE_SETUP) {
            if (resultCode == Activity.RESULT_OK) {
                // user granted mic + no assistant conflict → start your service
                startVoiceService()
            } else {
                // setup failed or was canceled → roll back the toggle
                prefs.edit().putBoolean("voice_enabled", false).apply()
                tglVoice.isChecked = false      //todo Should we add tglVoice.text = "Disabled" and tglVoice.background = orange ?????
            }
        }
        if (requestCode == REQUEST_DEEP_DOZE_AGREEMENT) {
            if (resultCode == RESULT_OK) {
                // User agreed → re-trigger toggle to continue setup
                tglVoice.isChecked = true
            } else {
                // User declined → keep it off
                tglVoice.isChecked = false
                positionedToast("Voice Control requires agreement to run in Deep Sleep mode.")
            }
        }
    }


    // ~~~~ Voice Services for Vcc ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private fun startVoiceService() {
        if (SharedPreferencesManager.isVccEnabled(this)) {
            val svc = Intent(this, VoiceControlService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svc)
            } else {
                startService(svc)
            }
        } else {
            stopService(Intent(this, VoiceControlService::class.java))
        }
    }


    private fun stopVoiceService() {
        stopService(Intent(this, VoiceControlService::class.java))
    }


    // ------------ Tournament Species Selector ------------------

    private fun loadTournamentSpeciesSpinner() {
        val spinnerSpecies: Spinner = findViewById(R.id.spinnerTournamentSpecies)

        // ✅ Single source of truth
        val speciesList = SharedPreferencesManager
            .loadSpeciesList(this)
            .map { speciesName ->
                SpeciesItem(
                    speciesName,
                    SpeciesImageHelper.getSpeciesImageResId(speciesName)
                )
            }

        val adapter = SpeciesSpinnerAdapter(this, speciesList)
        spinnerSpecies.adapter = adapter

        // Default selection
        if (speciesList.isNotEmpty()) {
            selectedSpecies = speciesList[0].name
        }

        spinnerSpecies.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedSpecies = speciesList[position].name
                    Log.d("DB_DEBUG", "Tournament species selected: $selectedSpecies")
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    if (speciesList.isNotEmpty()) {
                        selectedSpecies = speciesList[0].name
                    }
                }
            }
    }


    //------------------------------ GPS Permissions --------------------------------------------------
    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableGps()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    //------------------------ Enable GPS ------------------------
    private fun enableGps() {
        sharedPreferences.edit()
            .putBoolean("GPS_ENABLED", true)
            .apply()
        // one call, always positioned the same
        positionedToast("GPS is Enabled👍")
    }

    //------------------------ Disable GPS ------------------------
    private fun disableGps() {
        sharedPreferences.edit().putBoolean("GPS_ENABLED", false).apply()
        positionedToast("GPS Logging is Disabled.\nyou can not log catch locations.")
    }

    //--------------- Request Permissions for Bluetooth ----------------

    private fun requestPhoneStatePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                REQUEST_PHONE_STATE
            )
        }
    }


    // ---------- Permission Callbacks ------------
        override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

            when (requestCode) {
                REQUEST_RECORD_AUDIO -> {
                    if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                        // user granted mic → re‐toggle on
                        tglVoice.isChecked = true
                    } else {
                        positionedToast("🚫 Audio permission denied.")
                    }
                }

                REQUEST_BLUETOOTH_CONNECT -> {
                    if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                        // user granted BT_CONNECT → re‐toggle on
                        tglVoice.isChecked = true
                    } else {
                        positionedToast("🚫 Bluetooth permission denied.")
                    }
                }

                LOCATION_PERMISSION_REQUEST_CODE -> {
                    if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                        enableGps()
                    } else {
                        positionedToast("🚫 GPS permission denied.")
                        tglGPS.isChecked = false
                        disableGps()
                    }
                }

                REQUEST_PHONE_STATE -> {
                    if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "✅ Phone State permission granted")
                    } else {
                        positionedToast("🚫 Phone permission denied.")
                    }
                }
            }
        }//================= END onRequestPermissionResult ==============================

        // ---------- Safe Bluetooth check ---------
        private fun isBluetoothConnectedSafe(): Boolean {
            return com.bramestorm.bassanglertracker.utils.BluetoothUtils.isHeadsetConnected()
        }

    //======================= onResume ==========================================
        override fun onResume() {
            super.onResume()
            loadTournamentSpeciesSpinner() // Refreshes list if species were updated
        }

        //!!!!!!!!!!!!!!! For Shared Sessions !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        private fun isVoiceModeEnabled(): Boolean {
            return SharedPreferencesManager.isVccEnabled(this)
        }

    }

//================END==========================
