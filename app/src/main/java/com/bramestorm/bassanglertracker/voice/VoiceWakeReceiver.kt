package com.bramestorm.bassanglertracker.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.bramestorm.bassanglertracker.PopupVccTournLbs



class VoiceWakeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        Log.d("VCC", "🚨 VoiceWakeReceiver: onReceive() called with action = ${intent.action}")

        if (intent.action == "VOICE_WAKE") {
            Log.d("VCC", "🔥 VoiceWakeReceiver triggered")
            Toast.makeText(context, "🎙️ Voice Wake Triggered!", Toast.LENGTH_SHORT).show()

            val popupIntent = Intent(context, PopupVccTournLbs::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(popupIntent)
        }
    }
}
