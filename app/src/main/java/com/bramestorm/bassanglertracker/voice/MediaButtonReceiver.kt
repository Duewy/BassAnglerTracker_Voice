package com.bramestorm.bassanglertracker.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

class MediaButtonReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("VoiceCtrlSvc", "📥 MediaButtonReceiver received: ${intent.action}")

        // Forward the intent to VoiceControlService
        val serviceIntent = Intent(context, VoiceControlService::class.java).apply {
            action = Intent.ACTION_MEDIA_BUTTON
            putExtras(intent)
        }
        context.startForegroundService(serviceIntent)
    }
}
