package com.bramestorm.bassanglertracker.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.util.Log

object BluetoothUtils {
    private const val TAG = "BluetoothUtils"

    fun isHeadsetConnected(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        return try {
            adapter.getProfileConnectionState(BluetoothProfile.HEADSET) ==
                    BluetoothAdapter.STATE_CONNECTED
        } catch (e: SecurityException) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission missing", e)
            false
        }
    }
}