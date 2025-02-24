package com.bramestorm.bassanglertracker.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bramestorm.bassanglertracker.CatchItem

class CatchDatabaseHelperMetric(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "catch_database.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_NAME = "catches"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE_TIME = "date_time"
        private const val COLUMN_SPECIES = "species"
        private const val COLUMN_LENGTH_CENTIMETERS = "lengthCentimeters"
        private const val COLUMN_LENGTH_CMDEC = "lengthCmDec"
        private const val COLUMN_LENGTH_INCM = "lengthTotalCm"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE_TIME TEXT NOT NULL,
                $COLUMN_SPECIES TEXT NOT NULL,
                $COLUMN_LENGTH_CENTIMETERS INTEGER NOT NULL,
                $COLUMN_LENGTH_CMDEC INTEGER NOT NULL,
                $COLUMN_LENGTH_INCM REAL NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertCatch(dateTime: String, species: String, lengthCentimeters: Int, lengthCmDec: Int, lengthTotalCm: Float) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE_TIME, dateTime)
            put(COLUMN_SPECIES, species)
            put(COLUMN_LENGTH_CENTIMETERS, lengthCentimeters)
            put(COLUMN_LENGTH_CMDEC,lengthCmDec)
            put(COLUMN_LENGTH_INCM ,lengthTotalCm)
        }
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun getAllCatches(): List<CatchItem> {
        val catches = mutableListOf<CatchItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_DATE_TIME DESC", null)

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_TIME))
            val species = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIES))
            val  lengthCentimeters = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LENGTH_CENTIMETERS))
            val lengthCmDec = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LENGTH_CMDEC))
            val lengthTotalCm = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_LENGTH_CMDEC))

            catches.add(CatchItem(id, dateTime, species, lengthCentimeters, lengthCmDec, lengthTotalCm))
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
