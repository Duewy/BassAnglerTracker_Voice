package com.bramestorm.bassanglertracker.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.bramestorm.bassanglertracker.PopupVccTournLbs

class VoiceWakeReceiver : BroadcastReceiver() {
    companion object {
        const val VOICE_WAKE_ACTION = "com.bramestorm.bassanglertracker.VOICE_WAKE"
        const val VOICE_WAKE_PERMISSION = "com.bramestorm.bassanglertracker.permission.VOICE_WAKE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == VOICE_WAKE_ACTION) {
            Log.d("VCC", "🔥 VoiceWakeReceiver triggered")
            Toast.makeText(context, "🎙️ Voice Wake!", Toast.LENGTH_SHORT).show()
            // launch the popup activity on top →
            context.startActivity(
                Intent(context, PopupVccTournLbs::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

