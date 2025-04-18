package com.bramestorm.bassanglertracker.training

import android.app.Activity
import android.widget.Button
import java.util.Locale

class VoiceCommandHandler(
    private val activity: Activity,
    private val speak: (String) -> Unit
) {
    fun handle(command: String, logCatchButton: Button?) {
        val lower = command.lowercase(Locale.ROOT)

            //^^^^^^^^^^^^^^^^ Tournament Voice Catch Flow ^^^^^^^^^^^^^^^^^^^^^^^^^^
        when {
            lower.contains("log my catch") || lower.contains("new catch") -> {
                speak("Opening the catch entry.")
                logCatchButton?.performClick()
            }

            lower.contains("cancel") || lower.contains("over") || lower.contains("stop") -> {
                speak("Voice mode canceled.")
            }

            else -> {
                speak("Sorry, I didn't understand that command.")
            }
        }
    }
}
