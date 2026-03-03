package com.bramestorm.bassanglertracker.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log

object BluetoothUtils {
    private const val TAG = "BluetoothUtils"

    fun isHeadsetConnected(context: Context): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter ?: return false
        return try {
            adapter.getProfileConnectionState(BluetoothProfile.HEADSET) ==
                    BluetoothAdapter.STATE_CONNECTED
        } catch (e: SecurityException) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission missing", e)
            false
        }
    }
}