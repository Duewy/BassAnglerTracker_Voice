package com.bramestorm.bassanglertracker.voice

import android.speech.RecognitionListener
import androidx.appcompat.app.AppCompatActivity


class VoiceControlHelper(
    private val activity: AppCompatActivity,
    private val listener: RecognitionListener
) {
 //   fun ensureAudioPermissions(onGranted: ()→Unit) { … }
 //   fun ensureBluetoothConnected(onOk: ()→Unit, onFail: ()→Unit) { }
      fun startListening() { }
      fun stopListening() { }
    // parseSpeech(results: Bundle): VoiceCommand { … }
}
