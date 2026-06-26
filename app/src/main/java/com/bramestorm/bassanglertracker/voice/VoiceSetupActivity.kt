package com.bramestorm.bassanglertracker.voice

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.BuildConfig
import com.bramestorm.bassanglertracker.MainActivity
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.utils.BluetoothUtils
import com.bramestorm.bassanglertracker.utils.positionedToast

class VoiceSetupActivity : AppCompatActivity() {

    companion object {
        private const val REQ_BT_CONNECT = 101
        private const val TAG = "VoiceSetup"
        private val ASSIST_KEYS = listOf("assistant", "voice_interaction_service")


        fun isVoiceAssistantReady(context: Context): Boolean {
            val hasMicPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasMicPermission) {
                Log.e("VCC_PERMISSION", "🚫 RECORD_AUDIO permission not granted!")
            }

            val sttIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            val sttResolved = sttIntent.resolveActivity(context.packageManager) != null

            // You can enhance this if you later check for TTS readiness
            val ttsInstalled = true

            return hasMicPermission && sttResolved && ttsInstalled
        }

    }

    private lateinit var btnMainVSU : Button
    private lateinit var btnPDF : Button
    private lateinit var txtDefaultAssist: TextView
    private lateinit var txtDefaultRecognizer: TextView
    private lateinit var txtDefaultBixby: TextView          // the Bixby status text
    private lateinit var btnAssistantSettings: Button
    private lateinit var btnVoiceInputSettings: Button
    private lateinit var btnBixbySettings: Button           // the Bixby settings button
    private var hasLeftForSettings = false                   // tracks if user left to fix settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_setup)

        btnMainVSU            = findViewById(R.id.btnMainVSU)
        btnPDF                = findViewById(R.id.btnPDF)
        // Bind UI
        txtDefaultAssist      = findViewById(R.id.txtDefaultAssist)
        txtDefaultRecognizer  = findViewById(R.id.txtDefaultRecognizer)
        txtDefaultBixby       = findViewById(R.id.txtDefaultBixby)
        btnAssistantSettings  = findViewById(R.id.btnAssistantSettings)
        btnVoiceInputSettings = findViewById(R.id.btnVoiceInputSettings)
        btnBixbySettings      = findViewById(R.id.btnBixbySettings)


// Only ProVC needs the full Bluetooth/hands-free setup flow
        val isProVc = BuildConfig.FEATURE_VOICE_COMMANDS

        if (isProVc) {
            // Bluetooth permission / check (ProVC only)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQ_BT_CONNECT
                )
            } else {
                checkBluetoothDevices()
            }
        } else {
            // Basic edition: keep setup focused on enabling voice for Practice
            // (No BT permission prompts here)
        }


        btnMainVSU .setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }


        btnPDF.setOnClickListener {
            // Assuming PDF is in assets or served via GitHub
            val url = "https://raw.githubusercontent.com/Duewy/Catch_and_Call_Help_Files/main/Voice_Assistant_Setup_Guide_CatchAndCall.pdf"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        val infoAssist = findViewById<ImageView>(R.id.infoAssist)
        infoAssist.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Default Assistant")
                .setMessage("Set your assistant to Google for best voice control compatibility. Tap 'Assistant Settings' to open the right page.")
                .setPositiveButton("OK", null)
                .show()
        }

        //infoManufacturesRecognizer
        val infoManufacturesRecognizer = findViewById<ImageView>(R.id.infoManufacturesRecognizer)
        infoManufacturesRecognizer.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Manufacturer Voice Service")
                .setMessage("Your device may come with a manufacturer voice assistant (like Bixby or others). "
                        + "These may interfere with the Catch and Call voice system.")
                .setPositiveButton("OK", null)
                .show()
        }

        //infoVoiceRecognizer
        val infoRecognizer = findViewById<ImageView>(R.id.infoVoiceRecognizer)
        infoRecognizer.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Voice Recognizer")
                .setMessage("Make sure the default speech recognizer is set to Google. Tap 'Voice Input Settings' to configure it.")
                .setPositiveButton("OK", null)
                .show()
        }

        //infoBixbySettings
        val infoBixbySettings = findViewById<ImageView>(R.id.infoBixbySettings)
        infoBixbySettings.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Disable Bixby")
                .setMessage("If you're on a Samsung device, Bixby might be the default assistant. "
                        + "Use this button to force-stop Bixby or turn it off in App Info.")
                .setPositiveButton("OK", null)
                .show()
        }


        // Discover all assistants & recognizers
        val assistants  = getAllVoiceInteractionServices()
        val recognizers = getAllSpeechRecognizers()
        Log.d(TAG, "Installed assistants:  $assistants")
        Log.d(TAG, "Installed recognizers: $recognizers")

        setupSettingsButtons()
        updateVoiceSetupUI()
    }//============ END onCreate ============================

    override fun onResume() {
        super.onResume()
        updateVoiceSetupUI()
    }

    private fun setupSettingsButtons() {
        btnAssistantSettings.setOnClickListener {
            hasLeftForSettings = true
            openAppInfo(getDefaultAssistantPackage())
        }
        btnVoiceInputSettings.setOnClickListener {
            hasLeftForSettings = true
            openAppInfo(getDefaultVoiceRecognizerPackage())
        }
        // Opens Bixby/manufacturer voice service App Info so user can force-stop it
        btnBixbySettings.setOnClickListener {
            val bixbyPkg = getManufacturerVoicePkg()
            if (bixbyPkg != null) {
                hasLeftForSettings = true
                positionedToast("⚠️ Catch and Call Pro VC requires Bixby to be disabled.\nForce-stop or disable it here.")
                openAppInfo(bixbyPkg)
            } else {
                positionedToast("No manufacturer voice service found on this device.")
            }
        }
    }


    private fun updateVoiceSetupUI() {
        // Assistant check
        val assistPkg = getDefaultAssistantPackage()
        val assistOk = isApprovedAssistant(assistPkg)
        txtDefaultAssist.text = if (assistOk) {
            "✓ Assistant: ${getAppLabel(assistPkg)}"
        } else {
            "✗ Assistant: ${getAppLabel(assistPkg)}"
        }
        btnAssistantSettings.apply {
            isEnabled = !assistOk && !assistPkg.isNullOrBlank()
            alpha = if (isEnabled) 1f else 0.5f
        }

        // Recognizer check
        val recogPkg = getDefaultVoiceRecognizerPackage()
        val recogOk = isApprovedRecognizer(recogPkg)
        txtDefaultRecognizer.text = if (recogOk) {
            "✓ Recognizer: ${getAppLabel(recogPkg)}"
        } else {
            "✗ Recognizer: ${getAppLabel(recogPkg)}"
        }
        btnVoiceInputSettings.apply {
            isEnabled = !recogOk && !recogPkg.isNullOrBlank()
            alpha = if (isEnabled) 1f else 0.5f
        }


        // Manufacturer voice service check (Bixby, etc.)
        val bixbyPkg = getManufacturerVoicePkg()
        if (bixbyPkg != null) {
            txtDefaultBixby.text = "⚠️ Found: ${getAppLabel(bixbyPkg)}"
            btnBixbySettings.apply {
                isEnabled = true
                alpha = 1f
            }
        } else {
            txtDefaultBixby.text = "✓ No manufacturer voice service found"
            btnBixbySettings.apply {
                isEnabled = false
                alpha = 0.5f
            }
        }

        // If both checks pass — only auto-close if user is returning from settings
        if (assistOk && recogOk) {
            if (hasLeftForSettings) {
                positionedToast("👍 Voice setup OK")
                setResult(Activity.RESULT_OK)
                finish()                                    // auto-close: user just fixed something
            } else {
                positionedToast("👍 Voice setup OK \n tap MAIN MENU button to continue.")
                setResult(Activity.RESULT_OK)
            }
        }
    }

    // — checks if a known manufacturer voice service (Bixby, etc.) is installed
    private fun getManufacturerVoicePkg(): String? {
        val knownManufacturerPkgs = listOf(
            "com.samsung.android.bixby.agent",         // Bixby main agent
            "com.samsung.android.visionintelligence",   // Bixby Vision
            "com.samsung.android.bixby.service",        // Bixby background service
            "com.samsung.android.svoiceime",            // Samsung S-Voice (older)
            "com.samsung.android.svoice"                // Samsung S-Voice legacy
        )
        for (pkg in knownManufacturerPkgs) {
            try {
                packageManager.getApplicationInfo(pkg, 0)
                return pkg  // found it — it's installed
            } catch (_: PackageManager.NameNotFoundException) {
                // not installed, try next
            }
        }
        return null  // none found
    }

    private fun openAppInfo(pkg: String?) {
        pkg?.takeIf { it.isNotBlank() }?.let {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$it")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }.also(::startActivity)
        } ?: positionedToast("No package to open")
    }

    @SuppressLint("MissingPermission")
    private fun checkBluetoothDevices() {
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val btAdapter = manager?.adapter
        if (btAdapter == null) {
            positionedToast("This device has no Bluetooth adapter; Voice Control won't work.")
            return
        }

        val paired = btAdapter.bondedDevices
        if (paired.isNullOrEmpty()) {
            positionedToast("No Bluetooth device paired;\nVoice Control won't work.")
            return
        }

        if (BluetoothUtils.isHeadsetConnected(this)) {
            positionedToast("✅ Bluetooth headset connected — Voice Control should work.")
        } else {
            positionedToast("⚠️ Bluetooth is paired but not connected.\nConnect your headset before enabling Voice Control.")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQ_BT_CONNECT) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted → do the check now
                checkBluetoothDevices()
            } else {
                // User denied → let them know voice won’t work
                positionedToast("🚫 Bluetooth permission denied; voice won’t work.")
            }
        }
    }

    // Enumerate assistants
    private fun getAllVoiceInteractionServices(): List<String> {
        val pm = packageManager
        val intent = Intent("android.service.voice.VoiceInteractionService")
        return pm.queryIntentServices(intent, 0)
            .map { it.serviceInfo.packageName }
    }

    // Enumerate recognizers
    private fun getAllSpeechRecognizers(): List<String> {
        val pm = packageManager
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
    }

    // Rest of your existing helpers...
    // — returns the OS’s “assistant” setting (e.g. Google Assistant, Bixby, etc.)
    private fun getDefaultAssistantPackage(): String? {
        for (key in ASSIST_KEYS) {
            val flat = Settings.Secure.getString(contentResolver, key)
            Log.d(TAG, "Settings.Secure[\"$key\"] → \"$flat\"")
            val pkg = flat?.let { parseFlattenedPkg(it) }
            Log.d(TAG, "  parsed to pkg=\"$pkg\"")
            if (!pkg.isNullOrBlank()) return pkg
        }
        return null
    }

    // — is this package one of the approved Google assistants?
    private fun isApprovedAssistant(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        val approved = setOf(
            "com.google.android.googlequicksearchbox",    // Google App
            "com.google.android.apps.googleassistant"     // Assistant standalone
        )
        return approved.contains(pkg)
    }

    // — returns the OS’s speech‐to‐text service
    private fun getDefaultVoiceRecognizerPackage(): String? {
        val flat = Settings.Secure.getString(contentResolver, "voice_recognition_service")
        Log.d(TAG, "Settings.Secure[\"voice_recognition_service\"] → \"$flat\"")
        val pkg = flat?.let { parseFlattenedPkg(it) }
        if (!pkg.isNullOrBlank()) {
            Log.d(TAG, "  parsed to pkg=\"$pkg\"")
            return pkg
        }
        // fallback to whatever will handle ACTION_RECOGNIZE_SPEECH
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        val info   = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val fallback = info?.activityInfo?.packageName
        Log.d(TAG, "resolveActivity recognizer pkg=\"$fallback\"")
        return fallback
    }

    // — is this package one of the approved Google recognizers?
    private fun isApprovedRecognizer(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        val approved = setOf(
            "com.google.android.googlequicksearchbox",   // Google App
            "com.google.android.voicesearch",            // legacy
            "com.google.android.asr",                    // modern ASR
            "com.google.android.tts"                     // TTS (some OEMs bundle recognition)
        )
        return approved.contains(pkg)
    }

    // — turn “com.foo/.BarService” into “com.foo”
    private fun parseFlattenedPkg(flat: String): String? {
        return ComponentName.unflattenFromString(flat)?.packageName
            ?: flat.takeIf { it.isNotBlank() }
    }

    // — human‐readable label for a package name
    private fun getAppLabel(pkg: String?): String {
        if (pkg.isNullOrBlank()) return "Unknown"
        return try {
            val ai = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            pkg.substringAfterLast('.')
        }
    }

}//============END ===================================
