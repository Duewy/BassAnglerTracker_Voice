package com.bramestorm.bassanglertracker.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bramestorm.bassanglertracker.CatchItem
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



class CatchDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "catch_database.db", null, 6) {

    companion object {
        private const val DATABASE_NAME = "catch_database.db"
        private const val DATABASE_VERSION = 6
        private const val TABLE_NAME = "catches"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE_TIME = "date_time"
        private const val COLUMN_SPECIES = "species"
        private const val COLUMN_TOTAL_WEIGHT_OZ = "total_weight_oz"
        private const val COLUMN_TOTAL_LENGTH_8THS = "total_length_8ths"
        private const val COLUMN_TOTAL_WEIGHT_KG = "total_weight_hundredth_kg"
        private const val COLUMN_TOTAL_LENGTH_TENTHS = "total_length_tenths"
        private const val COLUMN_CATCH_TYPE = "catch_type"
        private const val COLUMN_MARKER_TYPE = "marker_type"
        private const val COLUMN_CLIP_COLOR = "clip_color"
    }


    override fun onCreate(db: SQLiteDatabase) {
        val createCatchesTable = """
        CREATE TABLE $TABLE_NAME (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_DATE_TIME TEXT NOT NULL,
            $COLUMN_SPECIES TEXT NOT NULL,
            $COLUMN_TOTAL_WEIGHT_OZ INTEGER,
            $COLUMN_TOTAL_LENGTH_8THS INTEGER,
            $COLUMN_TOTAL_WEIGHT_KG INTEGER DEFAULT 0,
            $COLUMN_TOTAL_LENGTH_TENTHS INTEGER,
            $COLUMN_CATCH_TYPE TEXT NOT NULL,
            $COLUMN_MARKER_TYPE TEXT,
            $COLUMN_CLIP_COLOR TEXT
        )
    """.trimIndent()
        db.execSQL(createCatchesTable)
    }// ````````````` END ON CREATE ``````````````````````````````

    // ^^^^ just in case we need to upgrade SQL later.... ^^^^^^^^^^^^^^^^^^

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("DB_DEBUG", "⚠️ Upgrading database from version $oldVersion to $newVersion...")

        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME") // Drop old table

        onCreate(db) // Recreate the table
    }


// **************** - INSERT CATCH -  *****************************************

    fun insertCatch(catch: CatchItem): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE_TIME, catch.dateTime)
            put(COLUMN_SPECIES, catch.species)
            put(COLUMN_TOTAL_WEIGHT_OZ, catch.totalWeightOz ?: 0)
            put(COLUMN_TOTAL_LENGTH_8THS, catch.totalLengthA8th ?: 0)
            put(COLUMN_TOTAL_LENGTH_TENTHS, catch.lengthDecimalTenthCm ?: 0)
            put(COLUMN_TOTAL_WEIGHT_KG, catch.totalWeightHundredthKg ?: 0)
            put(COLUMN_CATCH_TYPE, catch.catchType)
            put(COLUMN_MARKER_TYPE, catch.markerType)
            put(COLUMN_CLIP_COLOR, catch.clipColor)
        }

        val result = db.insert(TABLE_NAME, null, values)  // ✅ Correct insert

        return if (result == -1L) {
            Log.e("DB_DEBUG", "⚠️ Insert FAILED for catch: $values")
            false
        } else {
            Log.d("DB_DEBUG", "✅ Insert SUCCESS for catch: $values")
            true
        }
    }


//************ - GET ALL CATCHES FOR LIST VIEWS  *******************************

    fun getCatchesForToday(catchType: String, todayDate: String): List<CatchItem> {
        val db = readableDatabase
        val catchList = mutableListOf<CatchItem>()
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        Log.d("DB_DEBUG", "Fetching catches for today: $todayDate, catchType: $catchType")


        val cursor = db.rawQuery(
            "SELECT * FROM catches WHERE SUBSTR(date_time, 1, 10) = ? AND catch_type = ? ORDER BY total_weight_oz DESC",
            arrayOf(todayDate, catchType)
        )

        if (cursor.moveToFirst()) {
            do {
                val catch = CatchItem(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    dateTime = cursor.getString(cursor.getColumnIndexOrThrow("date_time")),
                    species = cursor.getString(cursor.getColumnIndexOrThrow("species")),
                    totalWeightOz = cursor.getInt(cursor.getColumnIndexOrThrow("total_weight_oz")),
                    totalLengthA8th = cursor.getInt(cursor.getColumnIndexOrThrow("total_length_8ths")),
                    lengthDecimalTenthCm = cursor.getInt(cursor.getColumnIndexOrThrow("total_length_tenths")),
                    totalWeightHundredthKg = cursor.getInt(cursor.getColumnIndexOrThrow("total_weight_hundredth_kg")),
                    catchType = cursor.getString(cursor.getColumnIndexOrThrow("catch_type")),
                    markerType = cursor.getString(cursor.getColumnIndexOrThrow("marker_type")),
                    clipColor = cursor.getString(cursor.getColumnIndexOrThrow("clip_color"))
                )
                catchList.add(catch)
            } while (cursor.moveToNext())
        } else {
            Log.d("DB_DEBUG", "No data found for today: $todayDate")
        }
        cursor.close()
        return catchList
    }
    // ______________ UPDATE CATCH _______________________________

    fun updateCatch(catchId: Int, totalWeightOz: Int, species: String) {
        val db = writableDatabase
        val values = ContentValues()
        values.put("total_weight_oz", totalWeightOz)
        values.put("species", species) // Update species

        Log.d("DB_DEBUG", "🔄 Updating ID=$catchId, New Weight=$totalWeightOz, New Species=$species")

        db.update(TABLE_NAME, values, "$COLUMN_ID=?", arrayOf(catchId.toString()))
        Log.d("DB_DEBUG", "✅ Rows updated: $catchId")
        db.close()
    }


    //_______________ DELETE CATCH FROM DATA BASE  _______________________

    fun deleteCatch(catchId: Int) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID=?", arrayOf(catchId.toString()))
        db.close()
    }

}
 //!!!!!!!!!!!!!!!!!!! - END - !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
