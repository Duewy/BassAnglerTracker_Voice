
package com.bramestorm.bassanglertracker.mappopups


import android.app.DatePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bramestorm.bassanglertracker.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PopupQueryDate(
    private val context: Context,
    private val onDateRangeSelected: (String) -> Unit)
 {
    private var fromDate: String = ""
    private var toDate: String = ""

    fun showPopup() {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_query_date, null)
        val btnFrom = view.findViewById<Button>(R.id.btnFromDate)
        val btnTo = view.findViewById<Button>(R.id.btnToDate)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelDateQuery)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmDateQuery)

        val dialog = AlertDialog.Builder(context).setView(view).create()

        btnFrom.setOnClickListener {
            showDatePicker { date ->
                fromDate = date
                btnFrom.text = "From: $date"
            }
        }

        btnTo.setOnClickListener {
            showDatePicker { date ->
                toDate = date
                btnTo.text = "To: $date"
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            // Default to current month if dates are empty
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(Date())
            val firstOfMonth = "${today.substring(0, 8)}01"

            val finalFrom = if (fromDate.isNotEmpty()) fromDate else today
            val finalTo = if (toDate.isNotEmpty()) toDate else today

            // Validate range
            if (finalFrom > finalTo) {
                Toast.makeText(context, "⚠️ 'From Date' must be before 'To Date'", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = "$finalFrom to $finalTo"
            onDateRangeSelected(result)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                onDateSelected(selectedDate)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
