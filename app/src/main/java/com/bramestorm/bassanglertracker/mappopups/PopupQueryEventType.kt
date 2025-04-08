package com.bramestorm.bassanglertracker.mappopups

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.bramestorm.bassanglertracker.R

class PopupQueryEventType(
    private val context: Context,
    private val onEventTypeSelected: (String) -> Unit
) {
    fun showPopup() {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_query_event_type, null)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupEventType)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmEventType)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelEventType)

        val dialog = AlertDialog.Builder(context).setView(view).create()

        btnConfirm.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            val selectedText = when (selectedId) {
                R.id.radioFunDay -> "Fun Day"
                R.id.radioTournament -> "Tournament"
                R.id.radioBoth -> "Both"
                else -> "Unknown"
            }
            onEventTypeSelected(selectedText)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
