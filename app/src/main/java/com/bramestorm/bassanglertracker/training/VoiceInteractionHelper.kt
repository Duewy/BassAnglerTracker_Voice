package com.bramestorm.bassanglertracker.training

import android.app.Activity
import android.util.Log
import android.widget.Toast

class VoiceInteractionHelper(
    private val activity: Activity,
    private val onCommandAction: (String) -> Unit // Callbacks for actual action (save catch, etc.)
) {
    private val commandManager: VoiceCommandManager
    private val responseManager: VoiceResponseManager

    init {
        responseManager = VoiceResponseManager(activity)

        commandManager = VoiceCommandManager(
            activity,
            onCommandReceived = { spokenText -> onCommandAction(spokenText) },
            onError = { error -> speak("Sorry, I didn't catch that.") },
            onAlreadyListening = {
                Toast.makeText(activity, "Voice already listening. Please wait...", Toast.LENGTH_SHORT).show()
            }
        )

    }

    fun startListening() {
        commandManager.startListening()
    }

    fun stopListening() {
       commandManager.stopListening()
    }


    fun speak(message: String, onDone: (() -> Unit)? = null) {
        responseManager.speak(message, onDone)
    }


    fun shutdown() {
        Log.d("Voice", "🔻 VoiceInteractionHelper shutting down...")
        commandManager.stopListening() // Stop any active session
        commandManager.shutdown()      // Destroy recognizer
        responseManager.shutdown()     // Destroy TTS
    }


}
