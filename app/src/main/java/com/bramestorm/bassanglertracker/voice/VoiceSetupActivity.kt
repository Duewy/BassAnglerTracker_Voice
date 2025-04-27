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
        private const val BIXBY_PKG      = "com.samsung.android.bixby.agent"
    }

    // UI references
    private lateinit var txtDefaultAssist: TextView
    private lateinit var txtDefaultRecognizer: TextView
    private lateinit var btnAssistantSettings: Button
    private lateinit var btnVoiceInputSettings: Button
    private lateinit var btnBixbySettings: Button
    private lateinit var txtDefaultBixby: TextView
    private var hasBixby = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_setup)

        // 1) bind views
        txtDefaultAssist       = findViewById(R.id.txtDefaultAssist)
        txtDefaultRecognizer   = findViewById(R.id.txtDefaultRecognizer)
        btnAssistantSettings   = findViewById(R.id.btnAssistantSettings)
        btnVoiceInputSettings  = findViewById(R.id.btnVoiceInputSettings)
        btnBixbySettings       = findViewById(R.id.btnBixbySettings)
        txtDefaultBixby     = findViewById(R.id.txtDefaultBixby)

        // 2) request BLUETOOTH_CONNECT if API 31+
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

        val bixbyIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:com.samsung.android.bixby.agent")
        }
        hasBixby = packageManager.resolveActivity(bixbyIntent, 0) != null

        // initial state for the button
        btnBixbySettings.isEnabled = hasBixby
        btnBixbySettings.alpha     = if (hasBixby) 1f else 0.5f
        btnBixbySettings.setOnClickListener { startActivity(bixbyIntent) }


        setupSettingsButtons()    // wire all three buttons here
        updateVoiceSetupUI()      // now that listeners exist, refresh ✓/✗ UI

    } //========= END onCreate ========================

    override fun onResume() {
        super.onResume()
        updateVoiceSetupUI()
    }

    private fun openAppInfo(pkg: String?) {
        pkg?.takeIf { it.isNotBlank() }?.let {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$it")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }.also(::startActivity)
        } ?: positionedToast("No package to open")
    }


    private fun setupSettingsButtons() {
        // Assistant → App-Info for whatever package Android is using
        btnAssistantSettings.setOnClickListener {
            openAppInfo(getDefaultAssistantPackage())
        }

        // Recognizer → App-Info for whatever package Android is using
        btnVoiceInputSettings.setOnClickListener {
            openAppInfo(getDefaultVoiceRecognizerPackage())
        }

        // Bixby → App-Info for Samsung’s Bixby package
        btnBixbySettings.setOnClickListener {
            openAppInfo(BIXBY_PKG)
        }
    }


    private fun updateVoiceSetupUI() {
        // — Default Assistant —
        val assistPkg = getDefaultAssistantPackage()
        val assistOk  = isApprovedAssistant(assistPkg)
        txtDefaultAssist.text = if (assistOk) {
            "✓ Assistant: ${getAppLabel(assistPkg)}"
        } else {
            "✗ Assistant: ${getAppLabel(assistPkg)}"
        }
        btnAssistantSettings.apply {
            // only allow tapping if it’s non-Google and we actually have a pkg
            isEnabled = !assistOk && !assistPkg.isNullOrBlank()
            alpha     = if (isEnabled) 1f else 0.5f
            setOnClickListener {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$assistPkg")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }.also(::startActivity)
            }
        }

        // — Default Recognizer —
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
            setOnClickListener {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$recogPkg")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }.also(::startActivity)
            }
        }
        val defaultAssist = getDefaultAssistantPackage()
        val bixbyDefault  = defaultAssist == "com.samsung.android.bixby.agent"
        txtDefaultBixby.text = when {
            !hasBixby           -> "✓ Bixby not installed"
            bixbyDefault        -> "✗ Bixby is default assistant"
            else                -> "✓ Bixby installed but not default"
        }
        btnBixbySettings.apply {
            isEnabled = hasBixby && bixbyDefault
            alpha     = if (isEnabled) 1f else 0.5f
        }


        // — Success: both Google? show toast & close —
        positionedToast("👍 All Voice Assistant & Recognizer settings are correct 👍")
              // tell the caller it all went well
              setResult(Activity.RESULT_OK)
              finish()
    }


    @SuppressLint("MissingPermission")
    private fun checkBluetoothDevices() {
        // Bail out if we don’t have BLUETOOTH_CONNECT on API31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val paired   = btAdapter?.bondedDevices
        if (paired.isNullOrEmpty()) {
            positionedToast("No Bluetooth device paired;\nVoice Control won’t work.")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_BT_CONNECT) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                checkBluetoothDevices()
            } else {
                positionedToast("Bluetooth permission denied;\nVoice Control disabled.")
            }
        }
    }

    // — helper getters & approval checks —

    private fun String?.toPackageName(): String? {
        if (this.isNullOrBlank()) return null
        // if it looks like “pkgName/className”…
        if (this.contains("/")) {
            return ComponentName.unflattenFromString(this)?.packageName
        }
        // otherwise assume it’s already just the pkg
        return this
    }

    private fun getDefaultAssistantPackage(): String? {
        for (key in ASSIST_KEYS) {
            val flat = Settings.Secure.getString(contentResolver, key)
            Log.d(TAG, "Settings.Secure[\"$key\"] -> \"$flat\"")
            val pkg = flat?.let { parseFlattenedPkg(it) }
            Log.d(TAG, "  parsed to pkg = \"$pkg\"")
            if (!pkg.isNullOrBlank()) return pkg
        }
        return null
    }


    /** Check the “voice_recognition_service” key first, then fallback to resolveActivity. */
    private fun getDefaultVoiceRecognizerPackage(): String? {
        val flat = Settings.Secure.getString(contentResolver, "voice_recognition_service")
        Log.d(TAG, "Settings.Secure[\"voice_recognition_service\"] -> \"$flat\"")
        val pkg = flat?.let { parseFlattenedPkg(it) }
        if (!pkg.isNullOrBlank()) {
            Log.d(TAG, "  parsed to pkg = \"$pkg\"")
            return pkg
        }
        // fallback
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        val info   = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val fallback = info?.activityInfo?.packageName
        Log.d(TAG, "resolveActivity recognizer pkg = \"$fallback\"")
        return fallback
    }
    private fun isApprovedAssistant(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        val approved = setOf(
            "com.google.android.googlequicksearchbox",   // Google App
            "com.google.android.apps.googleassistant"    // Google Assistant (alt)
        )
        return approved.contains(pkg)
    }

    /** Splits “com.foo/.BarService” into “com.foo” or returns as-is if already a pkg. */
    private fun parseFlattenedPkg(flat: String): String? {
        return ComponentName.unflattenFromString(flat)?.packageName
            ?: flat.takeIf { it.isNotBlank() }
    }

    private fun isApprovedRecognizer(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        val approved = setOf(
            "com.google.android.googlequicksearchbox",   // Google App (some OEMs bundle it)
            "com.google.android.voicesearch",            // legacy
            "com.google.android.asr",                     // “Speech Recognizer & Synthesis from Google”
            "com.google.android.tts"                     // TTS engine that also does recognition
        )
        return approved.contains(pkg)
    }

    private fun getAppLabel(pkg: String?): String {
        if (pkg.isNullOrBlank()) return "Unknown"
        return try {
            val ai = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            // As a fallback, show the last part of the package name
            pkg.substringAfterLast('.')
        }
    }

}//======= END ================
