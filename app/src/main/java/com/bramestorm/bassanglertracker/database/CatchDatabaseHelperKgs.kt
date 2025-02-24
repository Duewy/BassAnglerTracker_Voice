package com.bramestorm.bassanglertracker.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bramestorm.bassanglertracker.CatchItemKgs

class CatchDatabaseHelperKgs(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "catch_database_kgs.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "catches_kg"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE_TIME = "date_time"
        private const val COLUMN_SPECIES = "species"
        private const val COLUMN_WEIGHT_KG = "weight_kg"
    }

    private lateinit var dbHelper: CatchDatabaseHelperKgs  // ✅ Declare dbHelper here

    override fun onCreate(db: SQLiteDatabase) {

        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE_TIME TEXT NOT NULL,
                $COLUMN_SPECIES TEXT NOT NULL,
                $COLUMN_WEIGHT_KG DOUBLE NOT NULL )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertCatch(dateTime: String, species: String, weightKg: Double) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE_TIME, dateTime)
            put(COLUMN_SPECIES, species)
            put(COLUMN_WEIGHT_KG, weightKg)
        }
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun getAllCatches(): List<CatchItemKgs> {
        val catches = mutableListOf<CatchItemKgs>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_DATE_TIME DESC", null)

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_TIME))
            val species = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIES))
            val weightKg = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT_KG)) // ✅ Use Double

            catches.add(CatchItemKgs(id, dateTime, species, weightKg))
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
