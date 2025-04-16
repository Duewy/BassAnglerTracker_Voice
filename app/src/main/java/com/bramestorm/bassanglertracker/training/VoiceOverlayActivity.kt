package com.bramestorm.bassanglertracker.training

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bramestorm.bassanglertracker.R

class VoiceOverlayActivity : AppCompatActivity() {

    private lateinit var wakeLock: PowerManager.WakeLock
    private val requestCodeAudio = 101


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                requestCodeAudio
            )
        } else {
            startVoiceListening() // 🚀 only call this if permission is already granted
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // Acquire temporary wake lock
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "CatchCaddy::VoiceWakeLock"
        )
        wakeLock.acquire(10_000L)

        setContentView(R.layout.activity_voice_overlay) // Simple "Listening..." UI

        // Start speech recognition
        Handler(Looper.getMainLooper()).postDelayed({
            startSpeechRecognition()
        }, 1000)
    }

    private fun startSpeechRecognition() {
        // Your speech logic here
        Toast.makeText(this, "Speak now...", Toast.LENGTH_SHORT).show()

        // Dismiss overlay after delay
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 5000)
    }

    override fun onPause() {
        super.onPause()
        if (wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == requestCodeAudio) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceListening()
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
                finish() // 🔒 optional: close overlay if denied
            }
        }
    }

    private fun startVoiceListening() {
        // 🔊 TODO: Start voice recognition or wake listener
        Log.d("VoiceOverlay", "Permission granted. Ready to listen.")
    }


}
