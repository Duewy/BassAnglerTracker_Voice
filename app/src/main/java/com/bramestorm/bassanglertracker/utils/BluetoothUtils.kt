package com.bramestorm.bassanglertracker.utils

import android.annotation.SuppressLint
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

    /**
     * Returns the name of the first connected Bluetooth headset device,
     * or null if none is connected or permission is missing.
     */
    @SuppressLint("MissingPermission")
    fun getConnectedHeadsetName(context: Context): String? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter ?: return null

        return try {
            if (adapter.getProfileConnectionState(BluetoothProfile.HEADSET) !=
                BluetoothAdapter.STATE_CONNECTED) {
                return null
            }

            // Get bonded devices and find one that's a headset type
            adapter.bondedDevices?.firstOrNull { device ->
                val majorClass = device.bluetoothClass?.majorDeviceClass
                majorClass == 0x0400  // Audio/Video major class
            }?.name ?: adapter.bondedDevices?.firstOrNull()?.name
        } catch (e: SecurityException) {
            Log.w(TAG, "BLUETOOTH_CONNECT permission missing", e)
            null
        }
    }
}