package com.bramestorm.bassanglertracker.training

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.speech.SpeechRecognizer
import android.widget.Toast
import com.bramestorm.bassanglertracker.utils.positionedToast

object VoiceErrorHandler {
    private const val MAX_RETRIES = 2

    /**
     * @param activity   UI context for toasts or TTS
     * @param errorCode  one of SpeechRecognizer.ERROR_*
     * @param retryCount how many attempts so far
     * @param onRetry    invoked to try listening again
     * @param onFallback invoked when retries exhausted
     */
    fun handleError(
        activity: Activity,
        errorCode: Int,
        retryCount: Int,
        onRetry: () -> Unit,
        onFallback: () -> Unit
    ) {
        val message = when (errorCode) {
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_NO_MATCH ->
                "I did not catch that—please speak clearly."
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Network glitch—please try again in a minute."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Microphone permission needed—please grant it."
            else ->
                "Oops—an unexpected error occurred. ($errorCode)"
        }

        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()

        if (retryCount < MAX_RETRIES) {
            Handler(Looper.getMainLooper()).postDelayed(onRetry, 1500)
        } else {
            activity.positionedToast("Voice input failed—using manual entry.")
            onFallback()
        }
    }
}
