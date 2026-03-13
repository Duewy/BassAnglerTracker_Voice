package com.bramestorm.bassanglertracker.training

import android.content.Context
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

   fun speak(text: String, utteranceId: String) {
           tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
         }

    fun speak(message: String, onDone: (() -> String)? = null) {
        val utteranceId = "VoiceFeedback" + System.currentTimeMillis()

        // ✅ Set listener BEFORE speaking
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
                        onDone()
                    }
                }
            })
        }

        // ✅ Now speak — listener is already waiting
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

    }//==== END = Speak =========================



    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}//========= END = Voice Response Manager ===============
