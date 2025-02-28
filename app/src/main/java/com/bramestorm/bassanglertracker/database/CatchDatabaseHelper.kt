package com.bramestorm.bassanglertracker.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bramestorm.bassanglertracker.CatchItem

class CatchDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "catch_database.db"
        private const val DATABASE_VERSION = 5
        private const val TABLE_NAME = "catches"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE_TIME = "date_time"
        private const val COLUMN_SPECIES = "species"
        private const val COLUMN_TOTAL_WEIGHT_OZ = "total_weight_oz"
        private const val COLUMN_TOTAL_LENGTH_8THS = "total_length_8ths"
        private const val COLUMN_TOTAL_WEIGHT_HG = "total_weight_hg"
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
                $COLUMN_TOTAL_WEIGHT_HG INTEGER,
                $COLUMN_TOTAL_LENGTH_TENTHS INTEGER,
                $COLUMN_CATCH_TYPE TEXT NOT NULL,
                $COLUMN_MARKER_TYPE TEXT,
                $COLUMN_CLIP_COLOR TEXT
            )
        """.trimIndent()
        db.execSQL(createCatchesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db) // Recreate the table with the updated schema
    }

    fun insertCatch(catch: CatchItem): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE_TIME, catch.dateTime)
            put(COLUMN_SPECIES, catch.species)
            put(COLUMN_TOTAL_WEIGHT_OZ, catch.totalWeightOz ?: 0)
            put(COLUMN_TOTAL_LENGTH_8THS, catch.totalLengthA8th ?: 0)
            put(COLUMN_TOTAL_WEIGHT_HG, catch.weightDecimalTenthKg ?: 0)
            put(COLUMN_TOTAL_LENGTH_TENTHS, catch.lengthDecimalTenthCm ?: 0)
            put(COLUMN_CATCH_TYPE, catch.catchType)
            put(COLUMN_MARKER_TYPE, catch.markerType)
            put(COLUMN_CLIP_COLOR, catch.clipColor ?: "None")
        }
        return db.insert(TABLE_NAME, null, values) != -1L
    }

    fun getAllCatches(): List<CatchItem> {
        val catches = mutableListOf<CatchItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_DATE_TIME DESC", null)

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_TIME))
            val species = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIES))
            val totalWeightOz = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_WEIGHT_OZ))
            val totalLengthA8th = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_LENGTH_8THS))
            val weightDecimalTenthKg = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_WEIGHT_HG))
            val lengthDecimalTenthCm = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_LENGTH_TENTHS))
            val catchType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATCH_TYPE))
            val markerType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MARKER_TYPE)) ?: "Unknown"
            val clipColor = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLIP_COLOR)) ?: "None"

            catches.add(
                CatchItem(
                    id = id,
                    dateTime = dateTime,
                    species = species,
                    totalWeightOz = totalWeightOz,
                    totalLengthA8th = totalLengthA8th,
                    weightDecimalTenthKg = weightDecimalTenthKg,
                    lengthDecimalTenthCm = lengthDecimalTenthCm,
                    catchType = catchType,
                    markerType = markerType,
                    clipColor = clipColor
                )
            )
        }
        cursor.close()
        db.close()
        return catches
    }

    fun deleteCatch(catchId: Int) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID=?", arrayOf(catchId.toString()))
        db.close()
    }
}
