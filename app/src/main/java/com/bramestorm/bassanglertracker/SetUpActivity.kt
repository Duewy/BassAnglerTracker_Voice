package com.bramestorm.bassanglertracker

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
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
import com.bramestorm.bassanglertracker.activities.SpeciesSelectionActivity
import com.bramestorm.bassanglertracker.models.SpeciesItem
import com.bramestorm.bassanglertracker.util.positionedToast
import com.bramestorm.bassanglertracker.utils.SharedPreferencesManager
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper
import com.bramestorm.bassanglertracker.utils.SpeciesImageHelper.normalizeSpeciesName
import com.bramestorm.bassanglertracker.voice.VoiceControlService
import com.bramestorm.bassanglertracker.voice.VoiceSetupActivity


class SetUpActivity : AppCompatActivity() {

    private lateinit var btnWeight: Button
    private lateinit var btnLength: Button
    private lateinit var btnImperial: Button
    private lateinit var btnMetric: Button
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
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val REQUEST_VOICE_SETUP = 2001
        private const val BT_REQUEST_CODE = 3003
        private const val AUDIO_PERM_REQ = 4001
    }

    private val sharedPreferences by lazy { getSharedPreferences("AppPrefs", MODE_PRIVATE) }
    private val prefs by lazy { getSharedPreferences("UserPrefs", MODE_PRIVATE) }

    private var isWeightSelected = false
    private var isLengthSelected = false
    private var isImperialSelected = false
    private var isMetricSelected = false
    private var isFunDaySelected = false
    private var isTournamentSelected = false
    private var selectedSpecies: String = ""
    private var audioPermissionCallback: (() -> Unit)? = null

    private var isValUnits = false
    private var isValMeasuring = false



    //--------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SharedPreferencesManager.initializeDefaultSpeciesIfNeeded(this)

        //------------------- Ensures that the GPS must be Enabled Every Day --------------------

        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val today = android.text.format.DateFormat.format("yyyy-MM-dd", java.util.Date()).toString()
        val lastDate = prefs.getString("GPS_LAST_DATE", "")

        if (today != lastDate) {
            // New day → reset GPS to disabled
            prefs.edit()
                .putBoolean("GPS_ENABLED", false)
                .putString("GPS_LAST_DATE", today)
                .apply()
            positionedToast("📍 GPS logging has been reset.\nEnable it manually if needed.")
        }

        setContentView(R.layout.activity_set_up_event)


        // Initialize UI components
        btnWeight = findViewById(R.id.btnWeight)
        btnLength = findViewById(R.id.btnLength)
        btnImperial = findViewById(R.id.btnImperial)
        btnMetric = findViewById(R.id.btnMetric)
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
        btnWeight.setOnClickListener {
            Log.d("DEBUG", "Weight Is Selected ")
            isWeightSelected = true
            isLengthSelected = false
            isValMeasuring = true
            btnImperial.text = "Lbs Ozs"
            btnMetric.text = " Kgs"
            btnWeight.setBackgroundResource(R.color.bright_green)
            btnLength.setBackgroundResource(R.color.lite_grey)
        }

        btnLength.setOnClickListener {
            Log.d("DEBUG", "Length Is Selected ")
            isLengthSelected = true
            isWeightSelected = false
            isValMeasuring = true
            btnImperial.text = "Inches 4ths"
            btnMetric.text = "Cms"
            btnLength.setBackgroundResource(R.color.bright_green)
            btnWeight.setBackgroundResource(R.color.lite_grey)
        }

        // Toggle Units Selection
        btnImperial.setOnClickListener {
            Log.d("DEBUG", "Imperial Is Selected ")
            isImperialSelected = true
            isMetricSelected = false
            isValUnits = true
            btnImperial.setBackgroundResource(R.color.bright_green)
            btnMetric.setBackgroundResource(R.color.lite_grey)
        }

        btnMetric.setOnClickListener {
            Log.d("DEBUG", "Metric Is Selected ")
            isMetricSelected = true
            isImperialSelected = false
            isValUnits = true
            btnMetric.setBackgroundResource(R.color.bright_green)
            btnImperial.setBackgroundResource(R.color.lite_grey)
        }

        // Toggle Fun Day/Tournament Selection
        btnFunDay.setOnClickListener {
            Log.d("DEBUG", "FunDay Is Selected ")
            isFunDaySelected = true
            isTournamentSelected = false
            btnFunDay.setBackgroundResource(R.color.bright_green)
            btnTournament.setBackgroundResource(R.color.lite_grey)
            btnLength.visibility = View.VISIBLE
            btnMetric.visibility = View.VISIBLE
            txtLimitMarker.alpha = 0.3f
            txtSpeciesSelector.alpha = 0.3f
            tglCullingValue.alpha = 0.3f
            tglCullingValue.isEnabled=false
            spinnerTournamentSpecies.alpha = 0.3f
            spinnerTournamentSpecies.isEnabled = false
        }

        btnTournament.setOnClickListener {
            Log.d("DEBUG", "Tournament Is Selected ")
            isTournamentSelected = true
            isFunDaySelected = false
          //  isLengthSelected = false
          //  isWeightSelected = true
            btnTournament.setBackgroundResource(R.color.bright_green)
            btnFunDay.setBackgroundResource(R.color.lite_grey)
            btnLength.visibility = View.VISIBLE
            tglCullingValue.alpha = 1.0f
            txtLimitMarker.alpha = 1.0f
            txtSpeciesSelector.alpha = 1.0f
            tglCullingValue.isEnabled = true
            spinnerTournamentSpecies.alpha = 1.0f
            spinnerTournamentSpecies.isEnabled = true
        }


        // |||||||||||||| Load saved GPS state ||||||||||||||||||||||||||||||||||

        //------ ✅ Check both: saved state AND permission for GPS -----------
        val isGpsEnabledInPrefs = sharedPreferences.getBoolean("GPS_ENABLED", false)
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        tglGPS.isChecked = isGpsEnabledInPrefs && hasLocationPermission

        // Immediately set background based on initial state
        if (tglGPS.isChecked || tglGPS.text == "Enabled") {
            tglGPS.setBackgroundResource(R.drawable.btn_outline_green)
        } else {
            tglGPS.setBackgroundResource(R.drawable.btn_outline_orange)
        }

        tglGPS.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkAndRequestLocationPermission()
                // ✅ Change background to green
                tglGPS.setBackgroundResource(R.drawable.btn_outline_green)
            } else {
                disableGps()
                // 🔄 Revert background to orange
                tglGPS.setBackgroundResource(R.drawable.btn_outline_orange)
            }
        }

        //------ VOICE CONTROL ----------------
             // 1) Set initial state without triggering callbacks
        tglVoice.setOnCheckedChangeListener(null)
        tglVoice.isChecked = prefs.getBoolean("voice_enabled", false)
        tglVoice.background = if (isVoiceModeEnabled()) {
            ContextCompat.getDrawable(this, R.drawable.btn_outline_green)
        } else
            ContextCompat.getDrawable(this, R.drawable.btn_outline_orange)

            // 2) Now install the listener
        tglVoice.setOnCheckedChangeListener { _, isOn ->
            prefs.edit().putBoolean("voice_enabled", isOn).apply()
            if (isOn) {
                if (!checkBluetoothHeadset()) {
                    positionedToast("⚠️ No Bluetooth headset detected.")
                }
                ensureAudioPermissions {
                    startActivityForResult(
                        Intent(this, VoiceSetupActivity::class.java),
                        REQUEST_VOICE_SETUP
                    )
                }
            } else {
                stopVoiceService()
            }
        }



        btnMainSetup.setOnClickListener {
            val intent2 = Intent(this, MainActivity::class.java)
            startActivity(intent2)
        }

        btnCustomizeSpecies.setOnClickListener {
            val intent = Intent(this,SpeciesSelectionActivity::class.java)
            startActivity(intent)
        }


        //----------  Load user-Selected SPECIES LIST with icons --------------
        loadTournamentSpeciesSpinner()


        //---  Fishing Event Selection (Fun Day or Tournament)
        btnStartFishing.setOnClickListener {

            val nextActivity = when {
                isFunDaySelected && isWeightSelected && isImperialSelected -> CatchEntryLbsOzs::class.java
                isFunDaySelected && isWeightSelected && isMetricSelected -> CatchEntryKgs::class.java
                isFunDaySelected && isLengthSelected && isImperialSelected -> CatchEntryInches::class.java
                isFunDaySelected && isLengthSelected && isMetricSelected -> CatchEntryMetric::class.java

                isTournamentSelected && isWeightSelected && isImperialSelected -> CatchEntryTournament::class.java
                isTournamentSelected && isWeightSelected && isMetricSelected-> CatchEntryTournamentKgs::class.java
                isTournamentSelected && isLengthSelected && isMetricSelected-> CatchEntryTournamentCentimeters::class.java
                isTournamentSelected && isLengthSelected && isImperialSelected-> CatchEntryTournamentInches::class.java
                else -> null
            }

            if (nextActivity != null) {
                val intent = Intent(this, nextActivity).apply {
                    if (isTournamentSelected) {
                        putExtra("NUMBER_OF_CATCHES", if (tglCullingValue.isChecked) 5 else 4)
                        putExtra("TOURNAMENT_SPECIES", selectedSpecies)
                        putExtra("unitType", if (isWeightSelected) "weight" else "length")
                        putExtra("CULLING_ENABLED", tglCullingValue.isChecked)
                    }
                }
                startActivity(intent)
            } else {
               positionedToast("⚠️ Please select a Measurement and Unit Type!")
            }
        }
    }
    //=================== END of ON CREATE ================================

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VOICE_SETUP) {
            if (resultCode == Activity.RESULT_OK) {
                // user granted mic + no assistant conflict → start your service
                startVoiceService()
            } else {
                // setup failed or was canceled → roll back the toggle
                prefs.edit().putBoolean("voice_enabled", false).apply()
                tglVoice.isChecked = false
            }
        }
    }

    private fun ensureAudioPermissions(onGranted: () -> Unit) {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            perms += Manifest.permission.RECORD_AUDIO
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE_MICROPHONE
            ) != PackageManager.PERMISSION_GRANTED) {
            perms += Manifest.permission.FOREGROUND_SERVICE_MICROPHONE
        }

        if (perms.isNotEmpty()) {
            audioPermissionCallback = onGranted
            ActivityCompat.requestPermissions(
                this,
                perms.toTypedArray(),
                AUDIO_PERM_REQ
            )
        } else {
            onGranted()
        }
    }



    private fun startVoiceService() {
        val svc = Intent(this, VoiceControlService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }
    }

    private fun stopVoiceService() {
        stopService(Intent(this, VoiceControlService::class.java))
    }

    private fun checkBluetoothHeadset(): Boolean {
        // on Android 12+ we need BLUETOOTH_CONNECT at runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    BT_REQUEST_CODE
                )
                // bail out now; we’ll re–check once the user grants
                return false
            }
        }

        // safe to query SCO support and connection state now
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (!audioManager.isBluetoothScoAvailableOffCall) return false

        val btAdapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        return btAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) ==
                BluetoothAdapter.STATE_CONNECTED
    }




    // ------------ Tournament Species Selector ------------------
    private fun loadTournamentSpeciesSpinner() {
        val spinnerSpecies: Spinner = findViewById(R.id.spinnerTournamentSpecies)

        val savedSpecies = SharedPreferencesManager.getSelectedSpeciesList(this).ifEmpty {
            SharedPreferencesManager.getMasterSpeciesList(this)
        }

        val speciesList = savedSpecies.map { speciesName ->
            val imageRes = SpeciesImageHelper.getSpeciesImageResId(speciesName)
            SpeciesItem(speciesName, imageRes)
        }

        val adapter = SpeciesSpinnerAdapter(this, speciesList)
        spinnerSpecies.adapter = adapter

        // Select first by default (normalized)
        if (speciesList.isNotEmpty()) {
            selectedSpecies = normalizeSpeciesName(speciesList[0].name)
        }

        spinnerSpecies.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSpecies = normalizeSpeciesName(speciesList[position].name)
                Log.d("DB_DEBUG", "Species selected: $selectedSpecies")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedSpecies = ""
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

    //--------------- Request Permissions for GPS and Porcupine ----------------
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {

            BT_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                    // user just granted Bluetooth; re–invoke your toggle logic
                    if (checkBluetoothHeadset()) {
                        positionedToast("✅ Bluetooth headset connected.")
                    } else {
                        positionedToast("⚠️ No headset paired or powered on.")
                    }
                } else {
                    positionedToast("🚫 Bluetooth permission denied; voice won’t work.")
                    tglVoice.isChecked = false
                }
            }

            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    enableGps()
                } else {
                    positionedToast("🚫 GPS permission denied.")
                    tglGPS.isChecked = false
                    disableGps()
                }
            }
        }
    }


    //------------------------ Enable GPS ------------------------
    private fun enableGps() {
        sharedPreferences.edit()
            .putBoolean("GPS_ENABLED", true)
            .apply()
        // one call, always positioned the same
        positionedToast("GPS is Enabled")
    }

    //------------------------ Disable GPS ------------------------
    private fun disableGps() {
        sharedPreferences.edit().putBoolean("GPS_ENABLED", false).apply()
        positionedToast("GPS Logging is disabled.\nThe GPS Logging MUST BE Enable\nif you want to log catch locations.")
    }

//======================= onResume ==========================================
    override fun onResume() {
        super.onResume()
        loadTournamentSpeciesSpinner() // Refreshes list if species were updated
    }

    //!!!!!!!!!!!!!!! For Shared Sessions !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    private fun isVoiceModeEnabled(): Boolean {
        val prefs = getSharedPreferences("BassAnglerTrackerPrefs", MODE_PRIVATE)
        return prefs.getBoolean("VOICE_MODE_ENABLED", false)
    }

}
//================END==========================
