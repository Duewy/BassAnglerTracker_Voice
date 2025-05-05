// File: VoiceAudioUtils.kt (inside a `utils` or `voice` folder)

package com.bramestorm.bassanglertracker.voice

import android.content.Context
import android.media.MediaPlayer
import com.bramestorm.bassanglertracker.R

object VoiceAudioUtils {
    private var mediaPlayer: MediaPlayer? = null

    fun playSilentAudio(context: Context) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, R.raw.silence_0_1s)
            mediaPlayer?.setOnCompletionListener {
                it.release()
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
