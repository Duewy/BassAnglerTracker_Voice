package com.bramestorm.bassanglertracker.voice

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.R



class VoiceSetupActivity : AppCompatActivity() {

    companion object { private const val REQ_BT_CONNECT = 101  }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_setup)

        //  CHECK BLUETOOTH_CONNECT PERMISSION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Ask the user
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQ_BT_CONNECT
            )
        } else {
            // Already granted (or not needed on older Android), go check devices
            checkBluetoothDevices()
        }


        //  GET REFERENCES TO YOUR BUTTONS and TextView
        val btnBixby       = findViewById<Button>(R.id.btnBixbySettings)
        val btnVoiceInput  = findViewById<Button>(R.id.btnVoiceInputSettings)
        val btnAssistant   = findViewById<Button>(R.id.btnAssistantSettings)
        val txtDefaultRecognizer = findViewById<TextView>(R.id.txtDefaultRecognizer)
        val txtDefaultAssist = findViewById<TextView>(R.id.txtDefaultAssist)

        //  DETECT BIXBY AVAILABILITY
        val bixbyIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .apply { data = Uri.parse("package:com.samsung.android.bixby.agent") }
        val hasBixby = packageManager.resolveActivity(bixbyIntent, 0) != null
        btnBixby.isEnabled = hasBixby
        btnBixby.alpha     = if (hasBixby) 1f else 0.5f

        // DETECT VOICE-INPUT SETTINGS AVAILABILITY
        val voiceInputIntent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
        val hasVoiceInput = packageManager.resolveActivity(voiceInputIntent, 0) != null
        btnVoiceInput.isEnabled = hasVoiceInput
        btnVoiceInput.alpha     = if (hasVoiceInput) 1f else 0.5f

        //  DETECT ASSISTANT SETTINGS (Google Assistant) AVAILABILITY
        val assistantIntent = Intent("com.google.android.apps.gsa.settings.ASSISTANT")
        val hasAssistant = packageManager.resolveActivity(assistantIntent, 0) != null
        btnAssistant.isEnabled = hasAssistant
        btnAssistant.alpha     = if (hasAssistant) 1f else 0.5f

        // 1) Resolve and display the assistant label
        val rawAssist  = Settings.Secure.getString(contentResolver, "voice_interaction_service")
        val assistName = getServiceLabel(rawAssist)
        txtDefaultAssist.text = "Default assistant: $assistName  ✔"

        // 2) Resolve and display the recognizer label
        val rawRecog   = Settings.Secure.getString(contentResolver, "voice_recognition_service")
        val recogName  = getServiceLabel(rawRecog)
        txtDefaultRecognizer.text = "Default recognizer: $recogName  ✔"

        // 3) Disable the Voice-Input button if it’s already Google
        val isDefaultGoogle = rawRecog.contains("google", ignoreCase = true)
        btnVoiceInput.isEnabled = hasVoiceInput && !isDefaultGoogle
        btnVoiceInput.alpha     = if (btnVoiceInput.isEnabled) 1f else 0.5f

        // 4) Wire up the clicks
        btnBixby.setOnClickListener { startActivity(bixbyIntent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) }
        btnVoiceInput.setOnClickListener { startActivity(voiceInputIntent) }
        btnAssistant.setOnClickListener { startActivity(assistantIntent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) }

    }//=========== END onCreate ================================

    // HANDLE THE USER’S RESPONSE TO THE PERMISSION DIALOG
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_BT_CONNECT) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                // Now we can safely check bondedDevices
                checkBluetoothDevices()
            } else {
                // User denied — treat as “no devices”
                val toast = Toast.makeText(
                    this,
                    "Bluetooth permission denied; \n" +
                            "Voice Control will not function.",
                    Toast.LENGTH_LONG
                )
                toast.setGravity(
                    Gravity.CENTER,   // or Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL
                    0,                // xOffset
                    0                 // yOffset
                )
                toast.show()
            }
        }
    }

    private fun checkBluetoothDevices() {
        try {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            val paired = btAdapter?.bondedDevices
            if (paired.isNullOrEmpty()) {
                val toast = Toast.makeText(
                    this,
                    "There is no Bluetooth device connected;\nVoice Control will not function.",
                    Toast.LENGTH_LONG
                )
                toast.setGravity(
                    Gravity.CENTER,   // or Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL
                    0,                // xOffset
                    0                 // yOffset
                )
                toast.show()
            }
        } catch (sec: SecurityException) {
            // Should not happen if we requested correctly, but just in case:
            val toast = Toast.makeText(
                this,
                "Unable to check Bluetooth devices (permission denied).",
                Toast.LENGTH_LONG
            )
            toast.setGravity(
                Gravity.CENTER,   // or Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL
                0,                // xOffset
                0                 // yOffset
            )
            toast.show()
        }
    }


    private fun getServiceLabel(flattened: String?): String {
        // if it's null or empty, bail out early
        if (flattened.isNullOrBlank()) return "Unknown"

        return try {
            // parse the flat component name into package/class
            val comp = ComponentName.unflattenFromString(flattened)
                ?: return "Unknown"
            // look up the app’s human‐readable label
            val ai = packageManager.getApplicationInfo(comp.packageName, 0)
            packageManager.getApplicationLabel(ai).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

}
