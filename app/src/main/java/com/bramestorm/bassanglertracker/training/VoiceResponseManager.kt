package com.bramestorm.bassanglertracker.training

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceResponseManager(context: Context) {
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("VoiceResponse", "Language not supported")
                }
            } else {
                Log.e("VoiceResponse", "Initialization failed")
            }
        }
    }

    fun speak(message: String, onDone: (() -> Unit)? = null) {
        val params = Bundle()
        val utteranceId = "VoiceFeedback" + System.currentTimeMillis()

        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, params, utteranceId)

        if (onDone != null) {
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d("Voice", "🗣️ TTS started")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d("Voice", "✅ TTS finished")
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d("Voice", "🕒 Delay after TTS complete... triggering onDone() now.")
                        onDone()
                    }, 2000)
                }

                override fun onError(utteranceId: String?) {
                    Log.e("Voice", "❌ TTS error")
                    Handler(Looper.getMainLooper()).post {
                        onDone() // still trigger continuation if needed
                    }
                }
            })
        }
    }



    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
