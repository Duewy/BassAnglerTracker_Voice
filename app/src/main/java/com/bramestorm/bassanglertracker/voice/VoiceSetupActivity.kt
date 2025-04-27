package com.bramestorm.bassanglertracker.voice

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.R
import com.bramestorm.bassanglertracker.util.positionedToast

class VoiceSetupActivity : AppCompatActivity() {

    companion object {
        private const val REQ_BT_CONNECT = 101
        private const val TAG = "VoiceSetup"
        private val ASSIST_KEYS = listOf("assistant", "voice_interaction_service")
    }

    private lateinit var txtDefaultAssist: TextView
    private lateinit var txtDefaultRecognizer: TextView
    private lateinit var btnAssistantSettings: Button
    private lateinit var btnVoiceInputSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_setup)

        // Bind UI
        txtDefaultAssist      = findViewById(R.id.txtDefaultAssist)
        txtDefaultRecognizer  = findViewById(R.id.txtDefaultRecognizer)
        btnAssistantSettings  = findViewById(R.id.btnAssistantSettings)
        btnVoiceInputSettings = findViewById(R.id.btnVoiceInputSettings)

        // Bluetooth permission / check (existing)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQ_BT_CONNECT
            )
        } else {
            checkBluetoothDevices()
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
            openAppInfo(getDefaultAssistantPackage())
        }
        btnVoiceInputSettings.setOnClickListener {
            openAppInfo(getDefaultVoiceRecognizerPackage())
        }
    }

    private fun updateVoiceSetupUI() {
        // Assistant check
        val assistPkg = getDefaultAssistantPackage()
        val assistOk  = isApprovedAssistant(assistPkg)
        txtDefaultAssist.text = if (assistOk) {
            "✓ Assistant: ${getAppLabel(assistPkg)}"
        } else {
            "✗ Assistant: ${getAppLabel(assistPkg)}"
        }
        btnAssistantSettings.apply {
            isEnabled = !assistOk && !assistPkg.isNullOrBlank()
            alpha     = if (isEnabled) 1f else 0.5f
        }

        // Recognizer check
        val recogPkg = getDefaultVoiceRecognizerPackage()
        val recogOk  = isApprovedRecognizer(recogPkg)
        txtDefaultRecognizer.text = if (recogOk) {
            "✓ Recognizer: ${getAppLabel(recogPkg)}"
        } else {
            "✗ Recognizer: ${getAppLabel(recogPkg)}"
        }
        btnVoiceInputSettings.apply {
            isEnabled = !recogOk && !recogPkg.isNullOrBlank()
            alpha     = if (isEnabled) 1f else 0.5f
        }

        // If both checks pass, close with success
        if (assistOk && recogOk) {
            positionedToast("👍 Voice setup OK")
            setResult(Activity.RESULT_OK)
            finish()
        }
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
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val paired   = btAdapter?.bondedDevices
        if (paired.isNullOrEmpty()) {
            positionedToast("No Bluetooth device paired;\nVoice Control won’t work.")
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
