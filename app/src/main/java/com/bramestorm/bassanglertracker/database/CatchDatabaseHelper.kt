package com.bramestorm.bassanglertracker.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bramestorm.bassanglertracker.CatchItem

class CatchDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "catch_database.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_NAME = "catches"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE_TIME = "date_time"
        private const val COLUMN_SPECIES = "species"
        private const val COLUMN_WEIGHT_LBS = "weight_lbs"
        private const val COLUMN_WEIGHT_OZ = "weight_oz"
        // Use REAL to store the decimal weight properly
        private const val COLUMN_WEIGHT_DECIMAL = "weight"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE_TIME TEXT NOT NULL,
                $COLUMN_SPECIES TEXT NOT NULL,
                $COLUMN_WEIGHT_LBS INTEGER NOT NULL,
                $COLUMN_WEIGHT_OZ INTEGER NOT NULL,
                $COLUMN_WEIGHT_DECIMAL REAL NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertCatch(dateTime: String, species: String, weightLbs: Int, weightOz: Int, weightDecimal: Float) {
        val values = ContentValues().apply {
            put(COLUMN_DATE_TIME, dateTime)
            put(COLUMN_SPECIES, species)
            put(COLUMN_WEIGHT_LBS, weightLbs)
            put(COLUMN_WEIGHT_OZ, weightOz)
            put(COLUMN_WEIGHT_DECIMAL, weightDecimal)
        }
        writableDatabase.insert(TABLE_NAME, null, values)
    }

    fun getAllCatches(): List<CatchItem> {
        val catches = mutableListOf<CatchItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_DATE_TIME DESC", null)

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_TIME))
            val species = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIES))
            val weightLbs = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT_LBS))
            val weightOz = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT_OZ))
            val weightDecimal = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT_DECIMAL))
            catches.add(CatchItem(id, dateTime, species, weightLbs, weightOz, weightDecimal))
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
